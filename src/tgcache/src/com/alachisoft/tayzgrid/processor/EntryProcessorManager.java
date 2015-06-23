/*
 * Copyright (c) 2015, Alachisoft. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alachisoft.tayzgrid.processor;

import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.DataSourceReadOptions;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.enums.DataFormat;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.EntryProcessorException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessor;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessorResult;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author 
 */
public class EntryProcessorManager {

    public static TimeSpan DefaultLockTimeOut = new TimeSpan(0, 60);

    /**
     * The runtime context associated with the current cache.
     */
    private CacheRuntimeContext _context;
    private Cache _cacheRoot;
    private String _cacheName;

    public EntryProcessorManager(String cacheName, CacheRuntimeContext context, Cache cacheRoot) {
        _cacheName = cacheName;
        _context = context;
        _cacheRoot = cacheRoot;
    }

    public java.util.Map<Object, EntryProcessorResult> processEntries(Object[] keys, EntryProcessor entryProcessor, Object[] arguments, String defaultReadThru, String defaultWriteThru, OperationContext operationContext) {

        java.util.Map<Object, EntryProcessorResult> resultMap = new HashMap<Object, EntryProcessorResult>();
        for (Object key : keys) {
            try {
                EntryProcessorResult result = this.processEntry(key, entryProcessor, arguments, defaultReadThru, defaultWriteThru);
                if (result != null) {
                    resultMap.put(key, result);
                }
            } catch (Exception ex) {
                _context.getCacheLog().Error("Cache.InvokeEntryProcessor", "exception is thrown while processing key: " + key.toString() + ex.getMessage());
            }
        }
        return resultMap;
    }

    public EntryProcessorResult processEntry(Object key, EntryProcessor entryProcessor, Object[] arguments, String defaultReadThru, String defaultWriteThru) 
    {
        EntryProcessorResult result = null;
        EPCacheEntry epEntry = null;
        try {
            epEntry = getEPCacheEntry(key, defaultReadThru, entryProcessor.ignoreLock());

            Object value = (epEntry != null && epEntry.getCacheEntry() != null) ? epEntry.getCacheEntry().getValue() : null;

            TayzGridMutableEntry mutableEntry = new TayzGridMutableEntry(key, value);

            result = new TayzGridEntryProcessorResult(entryProcessor.processEntry(mutableEntry, arguments));

            if (mutableEntry.isUpdated()) {
                epEntry.setCacheEntry((makeCacheEntry(epEntry.getCacheEntry(), mutableEntry.getValue())));

                updateEPCacheEntry(key, epEntry, defaultWriteThru);

                //result = new TayzGridEntryProcessorResult(mutableEntry.getValue());
            }
            else if (mutableEntry.isRemoved()) 
            {
                //epEntry.getCacheEntry().setValue(mutableEntry.getValue());
                removeEPCacheEntry(key, epEntry, defaultWriteThru);
            }

        } catch (EntryProcessorException ex) {
            return new TayzGridEntryProcessorResult(ex);
        } catch (Exception ex) {
            return new TayzGridEntryProcessorResult(new EntryProcessorException(ex));
        }
        finally
        {
            if(epEntry!=null && epEntry.getLockHandle()!=null)
            {
                try {
                    _cacheRoot.Unlock(key,epEntry.getLockHandle().getLockId(),false,new OperationContext());
                } catch(Exception ex)
                {                   
                    _context.getCacheLog().Error("EntryProcessorManager.ProcesssEntry", "exception is thrown while unlocking key: " + key.toString() + ex.getMessage());  
                }
            }
        }     
        return result;
    }

    private EPCacheEntry getEPCacheEntry(Object key, String readthruProvider, Boolean ignoreLock) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        Object lockId = null;
        Long version = null;
        NCDateTime time = new NCDateTime(1970, 1, 1, 0, 0, 0, 0);
        java.util.Date lockDate = time.getDate();
        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);

       // operationContext.Add(OperationContextFieldName.ReadThru, true);
        BitSet flagMap = new BitSet();
        flagMap.SetBit((byte) BitSetConstants.ReadThru);
        flagMap.SetBit((byte) BitSetConstants.OptionalDSOperation);
        operationContext.Add(OperationContextFieldName.ReaderBitsetEnum, flagMap);
        operationContext.Add(OperationContextFieldName.DataFormat, DataFormat.Object);
        if (readthruProvider != null && !readthruProvider.isEmpty()) 
        {
            operationContext.Add(OperationContextFieldName.ReadThruProviderName, readthruProvider);
        }

        tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
        tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
        tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
        LockAccessType lockAccessType = LockAccessType.IGNORE_LOCK;

        if (!ignoreLock) {
            lockAccessType = LockAccessType.ACQUIRE;
        }

        CacheEntry entry = (CacheEntry) _cacheRoot.GetCacheEntry(key, null, null, tempRef_lockId, tempRef_lockDate, EntryProcessorManager.DefaultLockTimeOut, tempRef_version, lockAccessType, operationContext);
        lockId = tempRef_lockId.argvalue;
        lockDate = tempRef_lockDate.argvalue;

        LockHandle handle = null;
        if (lockId != null) {
            handle = new LockHandle(lockId.toString(), lockDate);
        }

        if (entry != null) {
            entry=(CacheEntry) (_cacheRoot.getContext().getInMemoryDataFormat()==DataFormat.Object? entry:entry.clone());
            com.alachisoft.tayzgrid.caching.util.SerializationUtil.Deserialize(entry, _cacheRoot.getName());
        }

        return new EPCacheEntry(entry, handle);
    }

    private void updateEPCacheEntry(Object key, EPCacheEntry epCacheEntry, String writethruProvider) throws Exception {
        CacheEntry entry = epCacheEntry.getCacheEntry();
        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);

        com.alachisoft.tayzgrid.caching.util.SerializationUtil.Serialize(entry, _cacheRoot.getName());

        _cacheRoot.Insert(key, entry.getValue(), entry.getExpirationHint(), entry.getEvictionHint(), entry.getGroupInfo() != null ? entry.getGroupInfo().getGroup() : null, entry.getGroupInfo() != null ? entry.getGroupInfo().getSubGroup() : null, entry.getQueryInfo(), getDSFlag(entry.getFlag(), DSWriteOption.OptionalWriteThru), epCacheEntry.getLockHandle() != null ? epCacheEntry.getLockHandle().getLockId() : null, entry.getVersion(), epCacheEntry.getLockHandle() != null ? LockAccessType.DEFAULT : LockAccessType.IGNORE_LOCK, writethruProvider, entry.getResyncProviderName(), operationContext);

    }

    private void removeEPCacheEntry(Object key, EPCacheEntry epCacheEntry, String writethruProvider) throws OperationFailedException 
    {
        //BitSet flag=epCacheEntry.getCacheEntry()!=null?epCacheEntry.getCacheEntry().getFlag():new BitSet();

        CacheEntry entry = epCacheEntry.getCacheEntry();
        String lockID = null;
        LockAccessType lockAccess = LockAccessType.IGNORE_LOCK;
        if (epCacheEntry.getLockHandle() != null) {
            lockAccess = LockAccessType.DEFAULT;
            lockID = epCacheEntry.getLockHandle().getLockId();
        }
        
        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
        
        _cacheRoot.Remove(key, getDSFlag(entry!=null?entry.getFlag():null, DSWriteOption.OptionalWriteThru), null, lockID, 0, lockAccess, writethruProvider, operationContext);
    }

    private BitSet getDSFlag(BitSet flagMap, DSWriteOption dsWriteOption) {
        if (flagMap == null) {
            flagMap = new BitSet();
        }

        switch (dsWriteOption) {
            case WriteBehind: {
                flagMap.SetBit((byte) BitSetConstants.WriteBehind);
            }
            break;
            case WriteThru: {
                flagMap.SetBit((byte) BitSetConstants.WriteThru);
            }
            break;
            case OptionalWriteThru: {
                flagMap.SetBit((byte) BitSetConstants.WriteThru);
                flagMap.SetBit((byte) BitSetConstants.OptionalDSOperation);
            }
            break;
        }

        return flagMap;
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    public final void dispose() {

    }

    private CacheEntry makeCacheEntry(CacheEntry entry, Object value) 
    {        
        if (entry == null) {
            entry = new CacheEntry(value, null, null);
        }
        else
        {
            if (entry.getValue() instanceof CallbackEntry)
            {
                ((CallbackEntry) entry.getValue()).setValue(value);                
            }
            else
            {                
                entry.setValue(value);                
            } 
        }
        
        HashMap queryInfo=com.alachisoft.tayzgrid.caching.cacheloader.CacheLoaderUtil.GetQueryInfo(value, _cacheRoot.GetTypeInfoMap());
        if(entry.getQueryInfo()==null && queryInfo!=null)
            entry.setQueryInfo(new HashMap());
        
        if(entry.getQueryInfo()!=null)
        {
            entry.getQueryInfo().put("query-info", queryInfo);
        }        

        return entry;
    }

    public class LockHandle {

        private String _lockId;
        private Date _lockDate = new Date(0);

        public LockHandle() {
            try {
                _lockDate = new NCDateTime(1970, 1, 1, 0, 0, 0, 0).getDate();
            } catch (Exception e) {
            }
        }

        /**
         * Create a new LockHandle
         *
         * @param lockId Lock id
         * @param lockDate Lock date
         */
        public LockHandle(String lockId, Date lockDate) {
            this._lockId = lockId;
            this._lockDate = lockDate;
        }

        /**
         * Get lock id
         *
         * @return Lock id
         */
        public String getLockId() {
            return this._lockId;
        }

        /**
         * Set lock id
         *
         * @param lockId New lock id
         */
        public void setLockId(String lockId) {

            this._lockId = lockId;
        }

        /**
         * Get lock date
         *
         * @return Lock date
         */
        public Date getLockDate() {
            return this._lockDate;
        }

        /**
         * Set lock date
         *
         * @param lockDate New lock date
         */
        public void setLockDate(Date lockDate) {
            this._lockDate = lockDate;
        }
    }

    public class EPCacheEntry {

        private LockHandle lockHandle;
        private CacheEntry cacheEntry;

        public EPCacheEntry(CacheEntry entry, LockHandle handle) {
            cacheEntry = entry;
            lockHandle = handle;
        }

        /**
         * @return the lockHandle
         */
        public LockHandle getLockHandle() {
            return lockHandle;
        }

        /**
         * @return the cacheEntry
         */
        public CacheEntry getCacheEntry() {
            return cacheEntry;
        }

        /**
         * @return the cacheEntry
         */
        public void setCacheEntry(CacheEntry entry) {
            cacheEntry = entry;
        }
    }

    public enum DSWriteOption {

        /**
         * Do not update data source
         */
        /**
         * Do not update data source
         */
        None,
        /**
         * Update data source synchronously
         */
        WriteThru,
        /**
         * Update data source asynchronously
         */
        WriteBehind,
        /**
         * Update data source synchronously if default provider is configured
         */
        OptionalWriteThru

    }

}
