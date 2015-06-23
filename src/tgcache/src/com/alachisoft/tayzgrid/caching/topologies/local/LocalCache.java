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

package com.alachisoft.tayzgrid.caching.topologies.local;

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.autoexpiration.AggregateExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.FixedExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.IdleExpiration;
import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionPolicyFactory;
import com.alachisoft.tayzgrid.caching.evictionpolicies.IEvictionPolicy;
import com.alachisoft.tayzgrid.caching.evictionpolicies.PriorityEvictionHint;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.common.DeleteParams;
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.common.ResetableIterator;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.storage.CacheStorageFactory;
import com.alachisoft.tayzgrid.storage.ICacheStorage;
import com.alachisoft.tayzgrid.storage.StoreAddResult;
import com.alachisoft.tayzgrid.storage.StoreInsResult;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;

import java.net.*;
import java.util.Iterator;

public class LocalCache extends LocalCacheBase implements Runnable {

    protected ICacheStorage _cacheStore;
    protected IEvictionPolicy _evictionPolicy;
    private Thread _evictionThread;
    private Object _eviction_sync_mutex = new Object();
    private boolean _allowExplicitGCCollection = true;
    private boolean _notifyCacheFull = false;

    public LocalCache(java.util.Map cacheClasses, CacheBase parentCache, java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context) throws ConfigurationException {
        super(properties, parentCache, listener, context);

        _stats.setClassName("local-cache");
        Initialize(cacheClasses, properties);
    }

    @Override
    public void dispose() {
        if (_cacheStore != null) {
            _cacheStore.dispose();
            _cacheStore = null;
        }
        super.dispose();
    }

    @Override
    public void run() {
        EvictAysnc();
    }
    
    @Override
    public long getCount() {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("LocalCache.Count", "");
        }

        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }

        return _cacheStore.getCount();
    }

    @Override
    public long getSessionCount() {
        return _stats.getSessionCount();
    }

    @Override
    public InetAddress getServerJustLeft() {
        return null;
    }

    @Override
    public void setServerJustLeft(InetAddress value) {
        ;
    }

    @Override
    public int getServersCount() {
        return 1;
    }

    @Override
    public boolean IsServerNodeIp(Address clientAddress) {
        return false;
    }
    
    @Override
    public long getSize() {
        if (_cacheStore != null) {
            return _cacheStore.getSize();
        }
        return 0;
    }

    @Override
    public float getEvictRatio() {
        if (_evictionPolicy != null) {
            return _evictionPolicy.getEvictRatio();
        }
        return 0;
    }

    @Override
    public void setEvictRatio(float value) {
        if (_evictionPolicy != null) {
            _evictionPolicy.setEvictRatio(value);
        }
    }

    @Override
    public long getMaxSize() {
        if (_cacheStore != null) {
            return _cacheStore.getMaxSize();
        }
        return 0;
    }

    @Override
    public void setMaxSize(long value) throws CacheException {
        if (_cacheStore != null) {
            //if the cache has less data than the new maximum size.
            //we can not apply the new size to the cache if the cache has already more data.
            if (_cacheStore.getSize() <= value) {
                _cacheStore.setMaxSize(value);
                _stats.setMaxSize(value);
                _context.PerfStatsColl.setCacheMaxSizeStats(value);
            } else {
                throw new CacheException("You need to remove some data from cache before applying the new size");
            }
        }
    }
    
    @Override
     public boolean getVirtualUnlimitedSpace()
    {
        return _cacheStore.getVirtualUnlimitedSpace();
    }
    
    @Override
    public void setVirtualUnlimitedSpace(boolean isVirtualUnlimitedSpace)
    {
        _cacheStore.setVirtualUnlimitedSpace(isVirtualUnlimitedSpace);
    }

    @Override
    public boolean CanChangeCacheSize(long size) {
        return (_cacheStore.getSize() <= size);
    }

    @Override
    protected void Initialize(java.util.Map cacheClasses, java.util.Map properties) throws ConfigurationException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            super.Initialize(cacheClasses, properties);

            if (ServicePropValues.Cache_EnableGCCollection != null) {
                _allowExplicitGCCollection = Boolean.parseBoolean(ServicePropValues.Cache_EnableGCCollection);
            }
            if (!properties.containsKey("storage")) {
                throw new ConfigurationException("Missing configuration option 'storage'");
            }

            if (properties.containsKey("scavenging-policy")) {
                java.util.Map evictionProps = (java.util.Map) ((properties.get("scavenging-policy") instanceof java.util.Map) ? properties.get("scavenging-policy") : null);
                if (evictionProps.containsKey("eviction-enabled")) {
                    String evictionEnabled = evictionProps.get("eviction-enabled").toString();
                    String evictRatio = evictionProps.get("evict-ratio").toString();
                    Boolean createEvictionPolicy = false;
                    if (evictionEnabled != null) {
                        createEvictionPolicy = Boolean.parseBoolean(evictionEnabled);
                    }
                    if (evictRatio != null && !evictRatio.toLowerCase().equals("null")) {
                        createEvictionPolicy &= Double.parseDouble(evictRatio) > 0;
                    } else {
                        createEvictionPolicy = false;
                    }
                    if (createEvictionPolicy) {
                        _evictionPolicy = EvictionPolicyFactory.CreateEvictionPolicy(evictionProps);
                    }
                }

            } else {
                _evictionPolicy = EvictionPolicyFactory.CreateDefaultEvictionPolicy();
            }
            java.util.Map storageProps = (java.util.Map) ((properties.get("storage") instanceof java.util.Map) ? properties.get("storage") : null);
            _cacheStore = CacheStorageFactory.CreateStorageProvider(storageProps, this._context.getSerializationContext(), _evictionPolicy != null, _context.getCacheLog(), _context.getEmailAlertNotifier());

            _stats.setMaxCount(_cacheStore.getMaxCount());
            _stats.setMaxSize(_cacheStore.getMaxSize());
        } catch (ConfigurationException e) {
            if (_context != null) {
                _context.getCacheLog().Error("LocalCache.Initialize()", e.toString());
            }
            dispose();
            throw e;
        } catch (Exception e) {
            if (_context != null) {
                _context.getCacheLog().Error("LocalCache.Initialize()", e.toString());
            }
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    @Override
    public void ClearInternal() {
        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }

        _cacheStore.Clear();

        _context.PerfStatsColl.setCacheSizeStats(0); // on clear cache, cachesize set to zero
        
        if (_evictionThread != null) {
            getCacheLog().Flush();
            _evictionThread.stop();
        }

        if (_evictionPolicy != null) {
            _evictionPolicy.Clear();
            
            if(_context.PerfStatsColl != null)
            {
                _context.PerfStatsColl.SetEvictionIndexSize(_evictionPolicy.getIndexInMemorySize());
            }
        }
    }

    @Override
    public boolean ContainsInternal(Object key) throws StateTransferException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("LocalCache.Cont", "");
        }

        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }
        return _cacheStore.Contains(key);
    }

    @Override
    public CacheEntry GetInternal(Object key, boolean isUserOperation, OperationContext operationContext) throws StateTransferException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("LocalCache.Get", "");
        }

        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }

        CacheEntry e = (CacheEntry) _cacheStore.Get(key);
        if (e != null) {
            EvictionHint evh = e.getEvictionHint();
            if (isUserOperation && _evictionPolicy != null && evh != null && evh.getIsVariant()) {
                _evictionPolicy.Notify(key, evh, null);
            }
        }
        return e;
    }

    @Override
    public CacheEntry GetEntryInternal(Object key, boolean isUserOperation) {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("LocalCache.GetInternal", "");
        }

        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }

        CacheEntry e = (CacheEntry) _cacheStore.Get(key);
        if (e == null) {
            return e;
        }

        EvictionHint evh = e.getEvictionHint();
        if (isUserOperation && _evictionPolicy != null && evh != null && evh.getIsVariant()) {
            _evictionPolicy.Notify(key, evh, null);
        }

        return e;
    }

    @Override
    public int GetItemSize(Object key) {
        if (_cacheStore == null) {
            return 0;
        }
        return _cacheStore.GetItemSize(key);
    }

    @Override
    public CacheAddResult AddInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation) throws StateTransferException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("LocalCache.Add_1", "");
        }
        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }

        if (_evictionPolicy != null) {
            if (cacheEntry.getEvictionHint() instanceof PriorityEvictionHint) {
                cacheEntry.setPriority(((PriorityEvictionHint) cacheEntry.getEvictionHint()).getPriority());
            }

            cacheEntry.setEvictionHint(_evictionPolicy.CompatibleHint(cacheEntry.getEvictionHint()));
        }

        if(_evictionPolicy == null)
            cacheEntry.setEvictionHint(null);
            
        StoreAddResult result = _cacheStore.Add(key, cacheEntry,!isUserOperation);
        // Operation completed!
        if (result == StoreAddResult.Success || result == StoreAddResult.SuccessNearEviction) {
            if (_evictionPolicy != null) {
                _evictionPolicy.Notify(key, null, cacheEntry.getEvictionHint());
            }
        }
        if (result == StoreAddResult.NotEnoughSpace && !_notifyCacheFull) {
            _notifyCacheFull = true;
            _context.getCacheLog().Error("LocalCache.AddInternal", "The cache is full and not enough items could be evicted.");
        }
        
        if(_context.PerfStatsColl != null)
        {
            if(_evictionPolicy != null)
            {
                _context.PerfStatsColl.SetEvictionIndexSize(_evictionPolicy.getIndexInMemorySize());
            }
            if(_context.ExpiryMgr != null)
            {
                _context.PerfStatsColl.SetExpirationIndexSize(_context.ExpiryMgr.getIndexInMemorySize());
            }
        }
        
        switch (result) {
            case Success:
                return CacheAddResult.Success;
            case KeyExists:
                return CacheAddResult.KeyExists;
            case NotEnoughSpace:
                return CacheAddResult.NeedsEviction;
            case SuccessNearEviction:
                return CacheAddResult.SuccessNearEviction;
        }
        return CacheAddResult.Failure;
    }

    @Override
    public boolean AddInternal(Object key, ExpirationHint eh, OperationContext operationContext) throws StateTransferException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("LocalCache.Add_2", "");
        }

        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }

        CacheEntry e = (CacheEntry) _cacheStore.Get(key);
        if (e == null) {
            return false;
        }

        //We only allow either idle expiration or Fixed expiration both cannot be set at the same time
        if ((e.getExpirationHint() instanceof IdleExpiration && eh instanceof FixedExpiration) || (e.getExpirationHint() instanceof FixedExpiration && eh instanceof IdleExpiration)) {
            return false;
        }

        if (e.getExpirationHint() == null) {
            e.setExpirationHint(eh);
        } else {
            if (e.getExpirationHint() instanceof AggregateExpirationHint) {
                ((AggregateExpirationHint) e.getExpirationHint()).Add(eh);
            } else {
                AggregateExpirationHint aeh = new AggregateExpirationHint();
                aeh.Add(e.getExpirationHint());
                aeh.Add(eh);
                e.setExpirationHint(aeh);
            }
        }
        _cacheStore.Insert(key, e,true);
        e.setLastModifiedTime(new java.util.Date());
        
        if(_context.PerfStatsColl != null)
        {
            if(_evictionPolicy != null)
            {
                _context.PerfStatsColl.SetEvictionIndexSize(_evictionPolicy.getIndexInMemorySize());
            }
            if(_context.ExpiryMgr != null)
            {
                _context.PerfStatsColl.SetExpirationIndexSize(_context.ExpiryMgr.getIndexInMemorySize());
            }
        }
        
        return true;
    }

    @Override
    public boolean RemoveInternal(Object key, ExpirationHint eh) throws StateTransferException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("LocalCache.Remove", "");
        }

        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }

        CacheEntry e = (CacheEntry) _cacheStore.Get(key);
        if (e == null || e.getExpirationHint() == null) {
            return false;
        } else {
            if (e.getExpirationHint() instanceof AggregateExpirationHint) {

                AggregateExpirationHint AggHint = new AggregateExpirationHint();
                AggregateExpirationHint entryExpHint = (AggregateExpirationHint) e.getExpirationHint();
                for (Iterator it = entryExpHint.iterator(); it.hasNext();) {
                    ExpirationHint exp = (ExpirationHint) it.next();
                    if (!exp.equals(eh)) {
                        AggHint.Add(exp);
                    }
                }
                e.setExpirationHint(AggHint);
            } else if (e.getExpirationHint().equals(eh)) {
                e.setExpirationHint(null);
            }
        }

        if (_notifyCacheFull) {
            _notifyCacheFull = false;
        }
        _cacheStore.Insert(key, e,true);
        e.setLastModifiedTime(new java.util.Date());
        
        if(_context.PerfStatsColl != null)
        {
            if(_evictionPolicy != null)
                _context.PerfStatsColl.SetEvictionIndexSize((long)_evictionPolicy.getIndexInMemorySize());
            if(_context.ExpiryMgr != null)
                _context.PerfStatsColl.SetExpirationIndexSize((long)_context.ExpiryMgr.getIndexInMemorySize());
        }
        
        return true;
    }



    @Override
    public CacheInsResult InsertInternal(Object key, CacheEntry cacheEntry, boolean isUserOperation, CacheEntry oldEntry, OperationContext operationContext) throws StateTransferException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("LocalCache.Insert", "");
        }

        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }

        if (cacheEntry.getEvictionHint() instanceof PriorityEvictionHint) {
            cacheEntry.setPriority(((PriorityEvictionHint) cacheEntry.getEvictionHint()).getPriority());
        }

        if (_evictionPolicy != null) {
            cacheEntry.setEvictionHint(_evictionPolicy.CompatibleHint(cacheEntry.getEvictionHint()));
        }

        EvictionHint peEvh = oldEntry == null ? null : oldEntry.getEvictionHint();

        if(_evictionPolicy == null)
            cacheEntry.setEvictionHint(null);
            
        StoreInsResult result = StoreInsResult.Failure;
        boolean doInsert = false;
        InsertParams options = null;
        Object obj = operationContext.GetValueByField(OperationContextFieldName.InsertParams);
        if(obj != null)
        {
            options = (InsertParams) obj;
        }
        if(options == null)
            doInsert = true;
        else
        {
            if(options.IsReplaceOperation)
            {
                if(options.CompareOldValue)
                {
                    //--TODO-- Compare Old value, doInsert if match
                    if(oldEntry == null && options.OldValue == null)
                        doInsert = true;
                    if(oldEntry!= null && oldEntry.valueEquals(options.OldValue))
                        doInsert = true;
                    if(oldEntry!=null && !oldEntry.valueEquals(options.OldValue))
                        operationContext.Add(OperationContextFieldName.ExtendExpiry, true);
                }
                else if(oldEntry == null)
                    result = StoreInsResult.Failure;
                else
                    doInsert = true;                            
            }
            else
            {
                if(options.CompareOldValue)
                {
                    //--TODO-- Compare Old value, doInsert if match
                     if(oldEntry == null && options.OldValue == null)
                        doInsert = true;
                    if(oldEntry!= null && oldEntry.valueEquals(options.OldValue))
                        doInsert = true;
                }
                else
                    doInsert = true;
            }
               
        }
        
        if(doInsert)
            result = _cacheStore.Insert(key, cacheEntry,!isUserOperation);
        // Operation completed!
        if (result == StoreInsResult.Success || result == StoreInsResult.SuccessNearEviction) {
            if (_evictionPolicy != null) {
                _evictionPolicy.Notify(key, null, cacheEntry.getEvictionHint());
            }
        } else if (result == StoreInsResult.SuccessOverwrite || result == StoreInsResult.SuccessOverwriteNearEviction) {
            //update the cache item version...
            if (isUserOperation) {
                cacheEntry.UpdateVersion(oldEntry);
            }
            //update the cache item last modifeid time...
            cacheEntry.UpdateLastModifiedTime(oldEntry);

            if (_evictionPolicy != null) {
                _evictionPolicy.Notify(key, peEvh, cacheEntry.getEvictionHint());
            }
        }
        if (result == StoreInsResult.NotEnoughSpace && !_notifyCacheFull) {
            _notifyCacheFull = true;
            _context.getCacheLog().Error("LocalCache.InsertInternal", "The cache is full and not enough items could be evicted.");
        }

        if(_context.PerfStatsColl != null)
        {
            if(_evictionPolicy != null)
            {
                _context.PerfStatsColl.SetEvictionIndexSize(_evictionPolicy.getIndexInMemorySize());
            }
            if(_context.ExpiryMgr != null)
            {
                _context.PerfStatsColl.SetExpirationIndexSize(_context.ExpiryMgr.getIndexInMemorySize());
            }
        }
        
        switch (result) {
            case Success:
                return CacheInsResult.Success;
            case SuccessOverwrite:
                return CacheInsResult.SuccessOverwrite;
            case NotEnoughSpace:
                return CacheInsResult.NeedsEviction;
            case SuccessNearEviction:
                return CacheInsResult.SuccessNearEvicition;
            case SuccessOverwriteNearEviction:
                return CacheInsResult.SuccessOverwriteNearEviction;
        }

        return CacheInsResult.Failure;
    }

    @Override
    public CacheEntry RemoveInternal(Object key, ItemRemoveReason removalReason, boolean isUserOperation, OperationContext operationContext) throws StateTransferException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("LocalCache.Remove", "");
        }

        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }
        
        if(operationContext.Contains(OperationContextFieldName.DeleteParams))
        {
            DeleteParams deleteParams = (DeleteParams)operationContext.GetValueByField(OperationContextFieldName.DeleteParams);
            if(deleteParams.CompareOldValue)
            {
                //Compare current value with params value
                //if not match return null
                CacheEntry pe = GetInternal(key, false, operationContext);
                if(pe!= null && !pe.valueEquals(deleteParams.OldValue)) {
                    operationContext.Add(OperationContextFieldName.ExtendExpiry, pe);
                    return null;
                }                   
            }
        }
            

        CacheEntry e = (CacheEntry) _cacheStore.Remove(key);
        if (e != null) {
            if (_evictionPolicy != null && e.getEvictionHint() != null) {
                _evictionPolicy.Remove(key, e.getEvictionHint());
            }

            if (_notifyCacheFull) {
                _notifyCacheFull = false;
            }
        }
        
        if(_context.PerfStatsColl != null)
        {
            if(_evictionPolicy != null)
                _context.PerfStatsColl.SetEvictionIndexSize((long)_evictionPolicy.getIndexInMemorySize());
            if(_context.ExpiryMgr != null)
                _context.PerfStatsColl.SetExpirationIndexSize((long)_context.ExpiryMgr.getIndexInMemorySize());
        }
        
        return e;
    }

    @Override
    public ResetableIterator GetEnumerator() throws OperationFailedException, LockingException, GeneralFailureException {
        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }
        return (ResetableIterator) new ResetableCacheStoreIterator(_cacheStore);
    }

    class ResetableCacheStoreIterator implements ResetableIterator {

        public ICacheStorage cacheStore;

        Iterator iter;

        public ResetableCacheStoreIterator(ICacheStorage cacheStore) {
            this.cacheStore = cacheStore;
            iter = cacheStore.GetEnumerator();
        }

        @Override
        public void reset() {
            iter = cacheStore.GetEnumerator();
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Object next() {
            return iter.next();
        }

        @Override
        public void remove() {
            iter.remove();
        }

    }

    @Override
    //: Array -Object[]
    public Object[] getKeys() {
        if (_cacheStore == null) {
            throw new UnsupportedOperationException();
        }
        return _cacheStore.getKeys();
    }

    @Override
    public void Evict() {
        if (_evictionPolicy == null) {
            return;
        }
        synchronized (_eviction_sync_mutex) {
            if (_parentCache.getIsEvictionAllowed()) {
                if (_allowAsyncEviction) {
                    if (_evictionThread == null) {
                        _evictionThread = new Thread(this);
                        _evictionThread.setDaemon(true);
                        _evictionThread.start();
                    }
                } else {
                    DoEvict(this);
                }
            }
        }
    }

    private void DoEvict(CacheBase cache) {
        try {
            if (_evictionPolicy != null) {
                _evictionPolicy.Execute(cache, _context, getSize());
                if (_allowExplicitGCCollection) {
                }
            }
        } finally {
        }
    }

    private void EvictAysnc() {
        try {
            if (!getIsSelfInternal()) {
                DoEvict(_context.getCacheImpl());
            } else {
                DoEvict(_context.getCacheInternal());
            }
        }
        catch (Exception e) {

            if (_context != null) {
                _context.getCacheLog().Error("LocalCache._evictionRun", e.toString());
            }

        } finally {
            synchronized (_eviction_sync_mutex) {
                _evictionThread = null;
            }
        }
    }
}