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

package com.alachisoft.tayzgrid.caching.datasourceproviders;

import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.IGRShutDown;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.cacheloader.CacheLoaderUtil;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.evictionpolicies.PriorityEvictionHint;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.util.Log;

import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.caching.expiration.ExpirationContract;
import com.alachisoft.tayzgrid.common.caching.expiration.ExpirationType;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor.IAsyncTask;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.LanguageContext;
import com.alachisoft.tayzgrid.runtime.caching.ProviderCacheItem;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.OperationResult;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteOperation;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import java.io.Serializable;
import java.lang.Object;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manager class for read-trhough and write-through operations
 */
public class DatasourceMgr  implements IGRShutDown//implements IDisposable
{   
    /**
     * Base class for datasource specific tasks.
     */
    private static class CacheResyncTask implements IAsyncTask {

        /**
         * The parent on this task.
         */
        private DatasourceMgr _parent;
        /**
         * Key for the item.
         */
        private Object _key;
        /**
         * item.
         */
        private Object _val;
        /**
         * item.
         */
        private ExpirationHint _exh;
        /**
         * item.
         */
        private EvictionHint _evh;
        /**
         */
        private BitSet _flag;
        /**
         */
        private GroupInfo _groupInfo;
        /**
         */
        private java.util.HashMap _queryInfo;
        private String _resyncProviderName;
        
        /**
         * Constructor.
         *
         * @param parent
         */
        public CacheResyncTask(DatasourceMgr parent, Object key, ExpirationHint exh, EvictionHint evh, GroupInfo groupInfo, java.util.HashMap queryInfo, String resyncProviderName) {
            _parent = parent;
            _key = key;
            _exh = exh;
            _evh = evh;
            _groupInfo = groupInfo;
            _queryInfo = queryInfo;
            _resyncProviderName = resyncProviderName;
        }

        public final Object getValue() {
            return _val;
        }

        public final ExpirationHint getExpirationHint() {
            return _exh;
        }

        public final EvictionHint getEvictionHint() {
            return _evh;
        }

        public final BitSet getFlag() {
            return this._flag;
        }

        public final GroupInfo getGroupInfo() {
            return this._groupInfo;
        }

        public final java.util.HashMap getQueryInfo() {
            return this._queryInfo;
        }

        /**
         * Do write-thru now.
         */
        public final void Process() throws GeneralFailureException, OperationFailedException, OperationFailedException, CacheException {
            synchronized (this) {
                try {
                    if (_val == null) {
                        ProviderCacheItem item = null;
                        LanguageContext languageContext = LanguageContext.NONE;
                        CacheEntry entry = null;
                        UserBinaryObject userBrinaryObject = null;
                        try {
                            tangible.RefObject<ProviderCacheItem> tempRef_item = new tangible.RefObject<ProviderCacheItem>(item);
                            tangible.RefObject<LanguageContext> tempRef_languageContext = new tangible.RefObject<LanguageContext>(languageContext);
                            _parent.ReadThru(_key, tempRef_item, _resyncProviderName, tempRef_languageContext);
                            item = tempRef_item.argvalue;
                            languageContext = tempRef_languageContext.argvalue;
                            tangible.RefObject<BitSet> tempRef__flag = new tangible.RefObject<BitSet>(this._flag);
                            tangible.RefObject<CacheEntry> tempRef_entry = new tangible.RefObject<CacheEntry>(entry);
                            userBrinaryObject = _parent.GetCacheEntry(_key, item, tempRef__flag, _groupInfo != null ? _groupInfo.getGroup() : null, _groupInfo != null ? _groupInfo.getSubGroup() : null, tempRef_entry, languageContext);
                            this._flag = tempRef__flag.argvalue;
                            entry = tempRef_entry.argvalue;

                        } catch (Exception ex) {
                            _val = ex;
                            _parent._context.getCacheLog().Error("DatasourceMgr.ResyncCacheItem", ex.getMessage() + " " + ex.getStackTrace());
                        }

                        OperationContext operationContext = new OperationContext();
                        operationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                        
                        if (!(_val instanceof Exception) && userBrinaryObject != null) {
                            CacheInsResultWithEntry result= _parent._context.getCacheImpl().Insert(_key, entry, true, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                            if (result != null && result.getResult() == CacheInsResult.IncompatibleGroup) _parent._context.getCacheImpl().Remove(_key, ItemRemoveReason.Removed, true, null, 0, LockAccessType.IGNORE_LOCK, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
                        } else {
                            _parent._context.getCacheImpl().Remove(_key, ItemRemoveReason.Expired, true, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                        }
                    }
                } catch (Exception e) {
                    _val = e;
                    _parent._context.getCacheLog().Error("DatasourceMgr.ResyncCacheItem", e.getMessage() + " " + e.getStackTrace());
                } finally {
                    _parent._context.PerfStatsColl.setCountStats(_parent._context.getCacheInternal().getCount());
                    _parent._context.PerfStatsColl.setCacheSizeStats(_parent._context.getCacheInternal().getSize());
                    _parent._queue.remove(_key);
                }
            }
        }

        private java.util.HashMap GetQueryInfo(Object value) {
            java.util.HashMap queryInfo = null;

            if (_parent._context.getCacheImpl().getTypeInfoMap() == null) {
                return null;
            }

            try {
                String typeName = value.getClass().getName();
                typeName = typeName.replace("+", ".");

                int handleId = _parent._context.getCacheImpl().getTypeInfoMap().getHandleId(typeName);
                if (handleId != -1) {
                    queryInfo = new java.util.HashMap();
                    java.lang.Class valType = null;
                    java.util.ArrayList attribValues = new java.util.ArrayList();
                    java.util.ArrayList attributes = _parent._context.getCacheImpl().getTypeInfoMap().getAttribList(handleId);
                    for (int i = 0; i < attributes.size(); i++) {
                        Object propertyAttrib = null;
                        if (propertyAttrib != null) {
                        } else {
                            java.lang.reflect.Field fieldAttrib = value.getClass().getDeclaredField((String) attributes.get(i));
                            if (fieldAttrib != null) {
                                Object attribValue = fieldAttrib.get(value);

                                if (attribValue instanceof String) {
                                    attribValue = (Object) (attribValue.toString()).toLowerCase();
                                }
                                attribValues.add(attribValue);
                            } else {
                                throw new Exception("Unable extracting query information from user object.");
                            }
                        }
                    }
                    queryInfo.put(handleId, attribValues);
                }
            } catch (Exception e) {
            }
            return queryInfo;
        }
    }
    /**
     * The runtime context associated with the current cache.
     */
    private CacheRuntimeContext _context;
    private CacheBase _cacheImpl;
    private String _cacheName;
    /**
     * The external datasource writer
     */
    private java.util.Map<String, ReadThruProviderMgr> _readerProivder = new java.util.HashMap<String, ReadThruProviderMgr>();
    /**
     * The external datasource writer
     */
    private java.util.HashMap<String, WriteThruProviderMgr> _writerProivder = new java.util.HashMap<String, WriteThruProviderMgr>();
     /**
     *  keep language context against each provider
     */
    private java.util.HashMap<Object, LanguageContext> _updateOpProviderMgr = new java.util.HashMap<Object, LanguageContext>();
    /**
     * Asynchronous event processor.
     */
    public AsyncProcessor _asyncProc;
    /**
     * Asynchronous write behind task processor
     */
    public WriteBehindAsyncProcessor _writeBehindAsyncProcess;
    public DSAsyncUpdatesProcessor _dsUpdateProcessor;
    /**
     * The external datasource reader
     */
    private java.util.HashMap _queue;
    private String _defaultReadThruProvider;
    private String _defaultWriteThruProvider;
    private boolean anyWriteBehindEnabled = false;
    private boolean anyWriteThruEnabled = false;
    private java.lang.Class _type = ICompactSerializable.class;
    

    /**
     * Initializes the object based on the properties specified in configuration
     *
     * @param properties properties collection for this cache.
     */
    public DatasourceMgr(String cacheName, java.util.Map properties, CacheRuntimeContext context, long timeout) throws ConfigurationException {
        _cacheName = cacheName;
        _context = context;
        _queue = new java.util.HashMap();
        Initialize(properties, timeout);
    }

    public final String getCacheName() {
        return _cacheName;
    }

    public final boolean getLoadCompactTypes() {
        return getIsReadThruEnabled() || getIsWriteThruEnabled();
    }

    public CacheBase getCacheImpl() {
        return _context.getCacheImpl();
    }

    public void setCacheImpl(CacheBase cacheImpl) {
        if (_writeBehindAsyncProcess != null) {
            _writeBehindAsyncProcess.setCacheImpl(_context.getCacheImpl());
        }
        _cacheImpl = cacheImpl;
    }

    /**
     * Check if ReadThru is enabled
     */
    public final boolean getIsReadThruEnabled() {
        return (_readerProivder.size() > 0);
    }

    /**
     * Check if WriteThru is enabled
     */
    public final boolean getIsWriteThruEnabled() {
        return (anyWriteThruEnabled);
    }

    public final String getDefaultReadThruProvider() {
        if (_defaultReadThruProvider != null) {
            return _defaultReadThruProvider.toLowerCase();
        }
        return null;
    }

    public final void setDefaultReadThruProvider(String value) {
        _defaultReadThruProvider = value;
    }

    public final WriteThruProviderMgr GetProvider(String providerName) {
        WriteThruProviderMgr writeThruManager = null;
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(providerName)) {
            providerName = getDefaultWriteThruProvider();
        }
        if (_writerProivder != null && _writerProivder.containsKey(providerName.toLowerCase())) {
            writeThruManager = _writerProivder.get(providerName.toLowerCase());
        }
        return writeThruManager;
    }

    public final String getDefaultWriteThruProvider() {
        if (_defaultWriteThruProvider != null) {
            return _defaultWriteThruProvider.toLowerCase();
        }
        return null;
    }

    public final void setDefaultWriteThruProvider(String value) {
        _defaultWriteThruProvider = value;
    }

    /**
     * Check if WriteBehind is enabled
     */
    public final boolean getIsWriteBehindEnabled() {
        return (anyWriteBehindEnabled);
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    public final void dispose() {
        if (_readerProivder != null) {
            java.util.Iterator enu = _readerProivder.values().iterator();
            while (enu.hasNext()) {

                IDisposable disposable = (IDisposable) enu.next();
                if (disposable != null) {

                    disposable.dispose();
                }
            }
            _readerProivder = null;
        }
        if (_writeBehindAsyncProcess != null) {
            _writeBehindAsyncProcess.Stop();
            _writeBehindAsyncProcess = null;
        }
        if (_writerProivder != null) {
            java.util.Iterator enu = _writerProivder.values().iterator();
            while (enu.hasNext()) {
                IDisposable disposable = (IDisposable) enu.next();
                if (disposable != null) {

                    disposable.dispose();
                }
            }
            _writerProivder = null;
        }
        if (_asyncProc != null) {
            _asyncProc.Stop();
            _asyncProc = null;
        }
    }

    /**
     * Method that allows the object to initialize itself. Passes the property
     * map down the object hierarchy so that other objects may configure
     * themselves as well.. 
     *
     * @param properties properties collection for this cache.
     */
    private void Initialize(java.util.Map properties, long timeout) throws ConfigurationException {
        String mode = "";
        ILogger log;
        int throttlingRate = 0, requeueLimit = 0, evictionRate = 0;
        long batchInterval = 0, operationDelay = 0;
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            if(_context.getCacheLog().getIsInfoEnabled())
                _context.getCacheLog().Info("Initializing data source providers.");
            if (properties.containsKey("read-thru")) {
                java.util.Map readThruProps = (java.util.Map) properties.get("read-thru");
                String enabled = (String) readThruProps.get("enabled");
                if (enabled.toLowerCase().equals("true")) {
                    _context.PerfStatsColl.setIsReadThrough(true);
                    java.util.Map providers = (java.util.Map) readThruProps.get("read-thru-providers");
                    if (providers != null) {
                        Iterator ide = providers.entrySet().iterator();
                        while (ide.hasNext()) {
                            Map.Entry pair = (Map.Entry) ide.next();
                            if (!_readerProivder.containsKey(pair.getKey().toString().toLowerCase())) {
                                _readerProivder.put(pair.getKey().toString().toLowerCase(), new ReadThruProviderMgr(_cacheName, (java.util.HashMap) ((providers.get(pair.getKey()) instanceof java.util.HashMap) ? providers.get(pair.getKey()) : null), _context));
                            }
                        }
                    }
                    
                    //configure factory-- in case of jcache only
                    if(_context.getCacheRoot().getConfiguration().getCacheLoaderFactory() != null)
                    {
                        String defaultreader = _context.getCacheRoot().getConfiguration().getCacheLoaderFactory().toString().toLowerCase();
                       if (!_readerProivder.containsKey(defaultreader)) {
                                _readerProivder.put(defaultreader, new ReadThruProviderMgr(_cacheName,_context.getCacheRoot().getConfiguration().getCacheLoaderFactory().create() , _context));
                        } 
                    }
                }
            }
            if (properties.containsKey("write-thru")) {
                java.util.Map writeThruProps = (java.util.Map) properties.get("write-thru");
                String enabled = (String) writeThruProps.get("enabled");
                if (enabled.toLowerCase().equals("true")) {
                    _context.PerfStatsColl.setIsWriteThrough(true);
                    anyWriteThruEnabled = true;//previous async mode config flag is removed now
                    if (writeThruProps.containsKey("write-behind")) {
                        anyWriteBehindEnabled = true;
                        java.util.Map writeBehindProps = (java.util.Map) writeThruProps.get("write-behind");
                        mode = writeBehindProps.get("mode").toString();
                        throttlingRate = Integer.parseInt(writeBehindProps.get("throttling-rate-per-sec").toString());
                        requeueLimit = Integer.parseInt(writeBehindProps.get("failed-operations-queue-limit").toString());
                        evictionRate = Integer.parseInt(writeBehindProps.get("failed-operations-eviction-ratio").toString());
                        if (mode.toLowerCase().equals("batch")) {
                            java.util.Map batchConfig = (java.util.Map) writeBehindProps.get("batch-mode-config");
                            batchInterval = Integer.parseInt(batchConfig.get("batch-interval").toString());
                            operationDelay = Integer.parseInt(batchConfig.get("operation-delay").toString());
                        }
                    }
                    java.util.Map providers = (java.util.Map) writeThruProps.get("write-thru-providers");
                    if (providers != null) {
                        Iterator ide = providers.entrySet().iterator();
                        while (ide.hasNext()) {
                            Map.Entry pair = (Map.Entry) ide.next();
                            if (!_writerProivder.containsKey(pair.getKey().toString().toLowerCase())) {
                                _writerProivder.put(pair.getKey().toString().toLowerCase(), new WriteThruProviderMgr(_cacheName, (java.util.HashMap) ((providers.get(pair.getKey()) instanceof java.util.HashMap) ? providers.get(pair.getKey()) : null), _context, (int) timeout, operationDelay, pair.getKey().toString()));

                            }
                        }
                    }
                    
                    //configure factory-- in case of jcache only
                    if(_context.getCacheRoot().getConfiguration().getCacheWriterFactory()!= null)
                    {
                        String defaultwriter = _context.getCacheRoot().getConfiguration().getCacheWriterFactory().toString().toLowerCase();
                       if (!_writerProivder.containsKey(defaultwriter)) {
                                _writerProivder.put(defaultwriter, new WriteThruProviderMgr(_cacheName, _context.getCacheRoot().getConfiguration().getCacheWriterFactory().create(), _context, (int) timeout, operationDelay, defaultwriter));
                        } 
                    }
                }
            }
            if (_writerProivder != null && anyWriteThruEnabled) {
                _dsUpdateProcessor = new DSAsyncUpdatesProcessor(this, _context.getCacheLog());
            }
            if (_writerProivder != null && anyWriteBehindEnabled) {
                _writeBehindAsyncProcess = new WriteBehindAsyncProcessor(this, throttlingRate, mode, batchInterval, operationDelay, requeueLimit, evictionRate, timeout, _writerProivder, _context.getCacheImpl(), _context);
            }
            if (_readerProivder != null && !this._context.getIsStartedAsMirror()) {
                _asyncProc = new AsyncProcessor(_context.getCacheLog());
                _asyncProc.Start();
            }
            if(_context.getCacheLog().getIsInfoEnabled())
                _context.getCacheLog().Info("Data source providers' initialization complete.");
        } catch (ConfigurationException e) {
            throw e;
            
        } catch (Exception e) {
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    private CacheResyncTask GetQueuedReadRequest(Object key) {
        synchronized (_queue) {
            return (CacheResyncTask) _queue.get(key);
        }
    }

    /**
     * Start the async processor thread
     */
    public final void StartWriteBehindProcessor() {
        if (_writeBehindAsyncProcess != null) {
            _writeBehindAsyncProcess.Start();
        }
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param key
     * @param e
     * @return
     */
    public final Object ResyncCacheItemAsync(Object key, ExpirationHint exh, EvictionHint evh, GroupInfo groupInfo, java.util.HashMap queryInfo, String resyncProviderName) {
        synchronized (_queue) {
            if (_asyncProc != null && GetQueuedReadRequest(key) == null) {
                AsyncProcessor.IAsyncTask task = new CacheResyncTask(this, (String) ((key instanceof String) ? key : null), exh, evh, groupInfo, queryInfo, resyncProviderName);
                _queue.put(key, task);
                _asyncProc.Enqueue(task);
            }
            return null;
        }
    }

    public final UserBinaryObject GetCacheEntry(Object key, ProviderCacheItem item, tangible.RefObject<BitSet> flag, String group, String subGroup, tangible.RefObject<CacheEntry> cacheEntry, LanguageContext langContext) throws OperationFailedException, ArgumentException, java.io.IOException {
        UserBinaryObject userObject = null;
        cacheEntry.argvalue = null;

        switch (langContext) {
            case DOTNET:
                userObject = GetCacheEntryDotNet(key, item, flag, group, subGroup, cacheEntry);
                break;
            case JAVA:
                userObject = GetCacheEntryDotNet(key, item, flag, group, subGroup, cacheEntry);
                break;
        }

        return userObject;
    }

    private UserBinaryObject GetCacheEntryJava(Object key, ProviderCacheItem item, tangible.RefObject<BitSet> flag, String group, String subGroup, tangible.RefObject<CacheEntry> cacheEntry) throws OperationFailedException {
        UserBinaryObject userObject = null;

        return userObject;
    }

    private UserBinaryObject GetCacheEntryDotNet(Object key, ProviderCacheItem item, tangible.RefObject<BitSet> flag, String group, String subGroup, tangible.RefObject<CacheEntry> cacheEntry) throws OperationFailedException, ArgumentException, java.io.IOException {
        UserBinaryObject userObject = null;
        cacheEntry.argvalue = null;
        Object val = null;

        if (item != null && item.getValue() != null) {
            if (item.getGroup() == null && item.getSubGroup() != null) {
                throw new OperationFailedException("Error occurred while synchronization with data source; group must be specified for sub group");
            }

            if (flag.argvalue == null) {
                flag.argvalue = new BitSet();
            }
            val = item.getValue();
            java.util.HashMap queryInfo = new java.util.HashMap();

            TypeInfoMap typeMap = _context.getCacheRoot().GetTypeInfoMap();
            queryInfo.put("query-info", com.alachisoft.tayzgrid.caching.cacheloader.CacheLoaderUtil.GetQueryInfo(item.getValue(), typeMap));
            if (item.getTags() != null) {
                java.util.HashMap tagInfo = CacheLoaderUtil.GetTagInfo(item.getValue(), item.getTags());
                if (tagInfo != null) {
                    queryInfo.put("tag-info", tagInfo);
                }
            }
            if (item.getNamedTags() != null) {
                try {
                    java.util.HashMap namedTagInfo = CacheLoaderUtil.GetNamedTagsInfo(item.getValue(), item.getNamedTags(), typeMap);
                    if (namedTagInfo != null) {
                        queryInfo.put("named-tag-info", namedTagInfo);
                    }
                } catch (Exception ex) {
                    throw new OperationFailedException("Error occurred while synchronization with data source; " + ex.getMessage());
                }
            }
//            if (!(item.getValue() instanceof Serializable) && !_type.isAssignableFrom(item.getValue().getClass())) {
//                throw new OperationFailedException("Read through provider returned an object that is not serializable.");
//            }
            
            com.alachisoft.tayzgrid.serialization.util.SerializationBitSet tempflag = new com.alachisoft.tayzgrid.serialization.util.SerializationBitSet(flag.argvalue.getData());
            val = SerializationUtil.safeSerialize(val, _context.getSerializationContext(), tempflag);
            flag.argvalue.setData(tempflag.getData());
            
            userObject = UserBinaryObject.CreateUserBinaryObject((byte[]) ((val instanceof byte[]) ? val : null));

            EvictionHint evh = new PriorityEvictionHint(item.getItemPriority());
            if(_context.ExpirationContract!=null){
            java.util.HashMap resolvedExpirations = _context.ExpirationContract.resolveClientExpirations(item.getAbsoluteExpiration(), item.getSlidingExpiration());
            item.setAbsoluteExpiration((Date)resolvedExpirations.get(ExpirationType.FixedExpiration));
            item.setSlidingExpiration((TimeSpan)resolvedExpirations.get(ExpirationType.SlidingExpiration));
            }
            
            ExpirationHint exh = com.alachisoft.tayzgrid.caching.autoexpiration.DependencyHelper.GetExpirationHint( item.getAbsoluteExpiration(), item.getSlidingExpiration());

            if (exh != null) {
                exh.setCacheKey(key);
                if (item.isResyncItemOnExpiration()) {
                    exh.SetBit(ExpirationHint.NEEDS_RESYNC);
                }
            }

            cacheEntry.argvalue = new CacheEntry(userObject, exh, evh);
            cacheEntry.argvalue.setFlag(flag.argvalue);

            if(!Common.isNullorEmpty(group))
               cacheEntry.argvalue.setGroupInfo(new GroupInfo(item.getGroup(), item.getSubGroup()));

            cacheEntry.argvalue.setQueryInfo(queryInfo);
            cacheEntry.argvalue.setResyncProviderName(item.getResyncProviderName() == null ? null : item.getResyncProviderName().toLowerCase());
            cacheEntry.argvalue.setKeySize(CacheKeyUtil.getKeySize(key, _cacheName));
        }

        return userObject;
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param key
     * @param e
     * @param group
     * @param subGroup
     * @return
     */
    public final Object ResyncCacheItem(Object key, tangible.RefObject<CacheEntry> entry, tangible.RefObject<BitSet> flag, String group, String subGroup, String providerName, OperationContext operationContext) throws OperationFailedException, Exception {
        ProviderCacheItem item = null;
        LanguageContext langContext = LanguageContext.forValue(0);

        ExpirationHint exh = null;
        EvictionHint evh = null;


        tangible.RefObject<ProviderCacheItem> tempRef_item = new tangible.RefObject<ProviderCacheItem>(item);
        tangible.RefObject<LanguageContext> tempRef_langContext = new tangible.RefObject<LanguageContext>(langContext);
        ReadThru(key, tempRef_item, providerName, tempRef_langContext);
        item = tempRef_item.argvalue;
        langContext = tempRef_langContext.argvalue;
        UserBinaryObject userObject = null;

        try {
            userObject = GetCacheEntry(key, item, flag, group, subGroup, entry, langContext);

            if (userObject == null) {
                return userObject;
            }

//            CacheInsResultWithEntry result = _context.getCacheImpl().Insert(key, entry.argvalue, false, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
            CacheInsResultWithEntry result = _context.getCacheImpl().Insert(key, (CacheEntry)entry.argvalue.clone(), true, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
            if (result != null && result.getResult() == CacheInsResult.IncompatibleGroup) throw new OperationFailedException("Data group of the inserted item does not match the existing item's data group");
            if (result.getResult() == CacheInsResult.Failure) {
                throw new OperationFailedException("Operation failed to synchronize with data source");
            } else if (result.getResult() == CacheInsResult.NeedsEviction) {
                throw new OperationFailedException("The cache is full and not enough items could be evicted.");
            }
        } catch (Exception ex) {
            throw new OperationFailedException("Error occurred while synchronization with data source. Error: " + ex.getMessage());
        }

        return userObject;
    }
    
    
    /**
     *
     *
     * @param keys
     * @param e
     * @param flag
     * @param group
     * @param subGroup
     */
    public final void ResyncCacheItem(java.util.HashMap orginalTable, Object[] keys, CacheEntry[] e, BitSet[] flag, String providerName, OperationContext operationContext, int updateCounter) throws OperationFailedException {
        java.util.HashMap<Object, ProviderCacheItem> cacheItems;
        LanguageContext langContext = LanguageContext.forValue(0);
        
        tangible.RefObject<LanguageContext> tempRef_langContext = new tangible.RefObject<LanguageContext>(langContext);
        cacheItems = ReadThru(keys, providerName, tempRef_langContext);
        langContext = tempRef_langContext.argvalue;

        if (cacheItems == null || (cacheItems != null && cacheItems.isEmpty())) {
            return;
        }

        Object[] refinedKeys = new Object[cacheItems.size()];
        CacheEntry[] refinedEnteries = new CacheEntry[cacheItems.size()];

        int counter = 0;
        for (int i = 0; i < keys.length; i++) {

            ProviderCacheItem cacheItem = null;

            if (!((cacheItem = cacheItems.get(keys[i])) != null) || cacheItem == null) {
                continue;
            }


            try {
                CacheEntry entry = null;
                tangible.RefObject<BitSet> tempRef_Object = new tangible.RefObject<BitSet>(flag[i]);
                tangible.RefObject<CacheEntry> tempRef_entry = new tangible.RefObject<CacheEntry>(entry);
                UserBinaryObject userBinaryObject = GetCacheEntry(keys[i], cacheItem, tempRef_Object, null, null, tempRef_entry, langContext);
                flag[i] = tempRef_Object.argvalue;
                entry = tempRef_entry.argvalue;

                if (userBinaryObject == null) {
                    continue;
                }

                refinedKeys[counter] = keys[i];
                refinedEnteries[counter++] = entry;
            } catch (Exception exception) {
                _context.getCacheLog().Error("DatasourceMgr.ResyncCacheItem", "Error occurred while synchronization with data source; " + exception.getMessage());
                continue;
            }

        }

        if (counter == 0) {
            return;
        }

        tangible.RefObject<Object[]> tempRef_refinedKeys = new tangible.RefObject<Object[]>(refinedKeys);
        Cache.Resize(tempRef_refinedKeys, counter);
        refinedKeys = tempRef_refinedKeys.argvalue;
        tangible.RefObject<Object[]> tempRef_refinedEnteries = new tangible.RefObject<Object[]>(refinedEnteries);
        Cache.Resize(tempRef_refinedEnteries, counter);
        refinedEnteries = (CacheEntry[]) tempRef_refinedEnteries.argvalue;

        java.util.HashMap insertedValues = null;
        boolean replaceExistingValues = false;

        try {
            CacheEntry[] clonedEntries = new CacheEntry[refinedEnteries.length];
            for(int i=0;i<refinedEnteries.length;i++) {
                clonedEntries[i] = (CacheEntry)refinedEnteries[i].clone();
            }
            
            _context.PerfStatsColl.mSecPerAddBeginSample();
            _context.PerfStatsColl.mSecPerUpdBeginSample();
            insertedValues = _context.getCacheImpl().Insert(refinedKeys, clonedEntries, true, operationContext);
            _context.PerfStatsColl.mSecPerUpdEndSample();
            _context.PerfStatsColl.mSecPerAddEndSample();
            replaceExistingValues = (Boolean)operationContext.GetValueByField(OperationContextFieldName.ReplaceExistingValues);
                    if(!replaceExistingValues) {
                        _context.PerfStatsColl.incrementAddPerSecStats(refinedKeys.length);    
                    }
                    else {
                        _context.PerfStatsColl.incrementUpdPerSecStats(updateCounter);
                        _context.PerfStatsColl.incrementAddPerSecStats(refinedKeys.length-updateCounter);
                        
                    }
        } catch (Exception ex) {
            throw new OperationFailedException("error while trying to synchronize the cache with data source. Error: " + ex.getMessage(), ex);
        }
        boolean isJcacheLoader = false;
        if(operationContext.Contains(OperationContextFieldName.JCacheLoader) ) {
                isJcacheLoader = (Boolean)operationContext.GetValueByField(OperationContextFieldName.JCacheLoader);
            }
        for (int i = 0; i < refinedKeys.length; i++) {
            
            if(isJcacheLoader) {
                orginalTable.put(refinedKeys[i], null);
            }
            else {
                if (insertedValues.containsKey(refinedKeys[i])) {
                    CacheInsResultWithEntry insResult = (CacheInsResultWithEntry) ((insertedValues.get(refinedKeys[i]) instanceof CacheInsResultWithEntry) ? insertedValues.get(refinedKeys[i]) : null);
                    if (insResult != null && (insResult.getResult() == CacheInsResult.Success || insResult.getResult() == CacheInsResult.SuccessOverwrite)) {
                        Object value = refinedEnteries[i].getValue();
                        if (value instanceof CallbackEntry) {
                            value = ((CallbackEntry) value).getValue();
                        }
                        orginalTable.put(refinedKeys[i], new CompressedValueEntry(value, refinedEnteries[i].getFlag()));
                    }
                } else {
                    Object value = refinedEnteries[i].getValue();
                    if (value instanceof CallbackEntry) {
                        value = ((CallbackEntry) value).getValue();
                    }
                    orginalTable.put(refinedKeys[i], new CompressedValueEntry(value, refinedEnteries[i].getFlag()));
                }
            }
            
        }
    }

    /**
     * Responsible for loading the object from the external data source. Key is
     * passed as parameter.
     *
     * @param key
     * @return
     */
    public final void ReadThru(Object key, tangible.RefObject<ProviderCacheItem> item, String providerName, tangible.RefObject<LanguageContext> langContext) throws Exception {
        item.argvalue = null;
        langContext.argvalue = LanguageContext.NONE;

        if (_readerProivder == null) {
            return;
        }

        ReadThruProviderMgr readThruManager = null;

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(providerName)) {
            providerName = getDefaultReadThruProvider();
        }

        if (_readerProivder.containsKey(providerName.toLowerCase())) {
            readThruManager = _readerProivder.get(providerName.toLowerCase());

            try {
                langContext.argvalue = readThruManager.getProviderType();

                if (readThruManager != null) {
                    readThruManager.ReadThru(key, item);
                }
            } catch (Exception e) {
                throw e;
            }
        }
    }

    /**
     *
     * @param keys
     * @return
     */
    public final java.util.HashMap<Object, ProviderCacheItem> ReadThru(Object[] keys, String providerName, tangible.RefObject<LanguageContext> langContext) throws OperationFailedException {
        langContext.argvalue = LanguageContext.NONE;
        if (_readerProivder == null) {
            return null;
        }

        ReadThruProviderMgr readThruManager = null;

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(providerName)) {
            providerName = getDefaultReadThruProvider();
        }

        if (_readerProivder.containsKey(providerName.toLowerCase())) {
            readThruManager = _readerProivder.get(providerName.toLowerCase());
            if (readThruManager != null) {
                langContext.argvalue = readThruManager.getProviderType();
                return readThruManager.ReadThru(keys);
            }
        }
        return null;
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param key
     */
    public final void WriteBehind(CacheBase internalCache, Object key, com.alachisoft.tayzgrid.caching.CacheEntry entry, String source, String taskId, String providerName, com.alachisoft.tayzgrid.caching.OpCode operationCode)throws Exception {
        if (_writerProivder == null) {
            return;
        }
        WriteThruProviderMgr writeThruManager = GetProvider(providerName);
        if (writeThruManager != null) {
            writeThruManager.WriteBehind(internalCache, key, entry, source, taskId, operationCode);
        }
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param key
     * @param val
     * @return
     */
    public final void WriteBehind(CacheBase internalCache, Object key, CacheEntry entry, String source, String taskId, String providerName, OpCode operationCode, WriteBehindAsyncProcessor.TaskState state)throws Exception {
        if (_writerProivder == null) {
            return;
        }
        WriteThruProviderMgr writeThruManager = GetProvider(providerName);
        if (writeThruManager != null) {
            writeThruManager.WriteBehind(internalCache, key, entry, source, taskId, operationCode, state);
        }
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param table
     * @return
     */
    public final void WriteBehind(CacheBase internalCache, Object[] keys, Object[] values, CacheEntry[] entries, String source, String taskId, String providerName, OpCode operationCode)throws Exception {
        if (_writerProivder == null) {
            return;
        }
        WriteThruProviderMgr writeThruManager = GetProvider(providerName);
        if (writeThruManager != null) {
            if (values == null) {
                values = new Object[keys.length];
            }
            writeThruManager.WriteBehind(internalCache, keys, values, entries, source, taskId, operationCode);
        }
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param table
     * @return
     */
    public final void WriteBehind(CacheBase internalCache, Object[] keys, Object[] values, CacheEntry[] entries, String source, String taskId, String providerName, OpCode operationCode, WriteBehindAsyncProcessor.TaskState state)throws Exception {
        if (_writerProivder == null) {
            return;
        }
        WriteThruProviderMgr writeThruManager = GetProvider(providerName);
        if (writeThruManager != null) {
            if (values == null) {
                values = new Object[keys.length];
            }
            writeThruManager.WriteBehind(internalCache, keys, values, entries, source, taskId, operationCode, state);
        }
    }

    public void WriteBehind(DSWriteBehindOperation operation)throws Exception {
        if (_writerProivder == null) {
            return;
        }
        WriteThruProviderMgr writeThruManager = GetProvider(operation.getProviderName());
        if (writeThruManager != null) {
            writeThruManager.WriteBehind(operation);
        }
    }

   public void WriteBehind(ArrayList operations)throws Exception {
        if (operations == null || _writerProivder == null) {
            return;
        }
        DSWriteBehindOperation operation = operations.get(0) instanceof DSWriteBehindOperation ? (DSWriteBehindOperation) operations.get(0) : null;
        WriteThruProviderMgr writeThruManager = (operation != null) ? GetProvider(operation.getProviderName()) : null;//bulk write thru call have same provider
        if (writeThruManager != null) {
            writeThruManager.WriteBehind(operations);
        }
    }

    public final void SetState(String taskId, String providerName, com.alachisoft.tayzgrid.caching.OpCode opCode, WriteBehindAsyncProcessor.TaskState state)throws Exception {
        for (WriteThruProviderMgr provider : GetWriteThruMgr(providerName, this._writerProivder, opCode)) {
            if (provider != null) {
                provider.SetState(taskId, state);
            }
        }
    }

    public final void SetState(String taskId, String providerName, com.alachisoft.tayzgrid.caching.OpCode opCode, WriteBehindAsyncProcessor.TaskState state, java.util.HashMap table)throws Exception {
        for (WriteThruProviderMgr provider : GetWriteThruMgr(providerName, this._writerProivder, opCode)) {
            if (provider != null) {
                provider.SetState(taskId, state, table);
            }
        }
    }

    private Iterator<WriteThruProviderMgr> GetWriteThruMgr(String providerName, java.util.Map<String, WriteThruProviderMgr> providers) {
        if (providerName == null) {
            providerName = _defaultWriteThruProvider;
        }
        if (providerName == null) {
            return (new ArrayList<WriteThruProviderMgr>()).iterator();
        }

        Map.Entry pair;
        ArrayList<WriteThruProviderMgr> arr = new ArrayList<WriteThruProviderMgr>();
        while (providers.entrySet().iterator().hasNext()) {
            pair = (Map.Entry) providers.entrySet().iterator().next();
            arr.add((WriteThruProviderMgr) pair.getValue());
        }
        return arr.iterator();
    }

    private Iterable<WriteThruProviderMgr> GetWriteThruMgr(String providerName, java.util.Map<String, WriteThruProviderMgr> providers, OpCode operationCode) {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(providerName)) {
            providerName = _defaultWriteThruProvider;
        }
        if (providerName == null) {

            return (Iterable<WriteThruProviderMgr>) new ArrayList<WriteThruProviderMgr>();
        }

        if (operationCode != OpCode.Clear) {
            WriteThruProviderMgr selected = null;
            selected = providers.get(providerName.toLowerCase());
            ArrayList<WriteThruProviderMgr> arr = new ArrayList<WriteThruProviderMgr>();
            arr.add(selected);
            return (Iterable<WriteThruProviderMgr>) arr;
        }


        Map.Entry pair;
        ArrayList<WriteThruProviderMgr> arr = new ArrayList<WriteThruProviderMgr>();
        Iterator providerIe = providers.entrySet().iterator();
        while (providerIe.hasNext()) {
            pair = (Map.Entry) providerIe.next();
            arr.add((WriteThruProviderMgr) pair.getValue());
        }
        return (Iterable<WriteThruProviderMgr>) arr;
    }
    
    
    public final void WindUpTask()
    {
    }

    public final void WaitForShutDown(long interval)
    {
            _context.getCacheLog().CriticalInfo("DatasourceMgr", "Waiting for  Write Behind queue shutdown task completion.");

            java.util.Date startShutDown = new java.util.Date();

            if (_writeBehindAsyncProcess != null)
            {
                    _writeBehindAsyncProcess.WaitForShutDown(interval);
            }

            _context.getCacheLog().CriticalInfo("DatasourceMgr", "Waiting for  Write Behind Update queue shutdown task completion.");

            if (_dsUpdateProcessor != null)
            {
                    long startTime = (Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l;
                    long timeout = (int)(interval * 1000) - (int)(((Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l - startTime) * 1000);
                    timeout = timeout / 1000;
                    if (timeout > 0)
                    {
                            _dsUpdateProcessor.WaitForShutDown(timeout);
                    }
            }
            _context.getCacheLog().CriticalInfo("DatasourceMgr", "Shutdown task completed.");
    }

    /**
     * Deqeueu a task mathcing taskId
     *
     * @param taskId taskId
     */
    public final void DequeueWriteBehindTask(String[] taskId, String providerName)throws Exception{
        WriteThruProviderMgr provider = null;
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(providerName)) {
            providerName = getDefaultWriteThruProvider();
        }
        provider = this._writerProivder.get(providerName.toLowerCase());

        if (provider != null) {
            provider.DequeueWriteBehindTask(taskId);
        }
    }

    /**
     * Responsible for updating/inserting an object to the data source. The Key
     * and the object are passed as parameter.
     *
     * @param key
     * @param val
     * @return
     */
    public final OperationResult WriteThru(Object key, CacheEntry val, OpCode opCode, String providerName, OperationContext operationContext) throws OperationFailedException {
        OperationResult result = null;
        if (_writerProivder == null) {
            return null;
        }
        WriteThruProviderMgr writeThruManager = GetProvider(providerName);
        if (writeThruManager != null) {
            DSWriteOperation operation = new DSWriteOperation(_context.getSerializationContext(), key, null, val, opCode, providerName);
            result = writeThruManager.WriteThru(_context.getCacheImpl(), operation, false, operationContext);
           
            if (result != null && result.getUpdateInCache()) {
                if (result.getDSOperationStatus() == OperationResult.Status.Success) {
                    DSUpdateInCache(result.getOperation(), writeThruManager.getLanguageContext());
                }
            }
        }
        return result;
    }

    public final void DSUpdateInCache(WriteOperation operation, LanguageContext languageContext) {
        UserBinaryObject userBrinaryObject = null;
        Exception exc = null;
        boolean rollback = ValidateWriteOperation(operation);
        try {
            if (!rollback) {
                switch (operation.getOperationType()) {
                    case Add:
                    case Update:
                        ProviderCacheItem item = operation.getProviderCacheItem();
                        CacheEntry entry = null;
                        BitSet flag = new BitSet();

                        tangible.RefObject<BitSet> tempRef_flag = new tangible.RefObject<BitSet>(flag);
                        tangible.RefObject<CacheEntry> tempRef_entry = new tangible.RefObject<CacheEntry>(entry);
                        userBrinaryObject = GetCacheEntry(operation.getKey(), item, tempRef_flag, item.getGroup() != null ? item.getGroup() : null, item.getSubGroup() != null ? item.getSubGroup() : null, tempRef_entry, languageContext);
                        flag = tempRef_flag.argvalue;
                        entry = tempRef_entry.argvalue;
                        if (userBrinaryObject != null) {
                            _context.PerfStatsColl.mSecPerDSUpdBeginSample();
                           CacheInsResultWithEntry result=_context.getCacheImpl().Insert(operation.getKey(), entry, true, null, 0, LockAccessType.IGNORE_LOCK, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
                            if (result != null && result.getResult() == CacheInsResult.IncompatibleGroup)
                                    {
                                        rollback = true;
                                        _context.getCacheLog().Error("DatasourceMgr.UpdateInCache", "Data group of the inserted item does not match the existing item's data group");
                                    }
                            _context.PerfStatsColl.mSecPerDSUpdEndSample();
                            _context.PerfStatsColl.incrementDSUpdatePerSec();
                        }
                        break;
                }
            }
        } catch (Exception e) {
            exc = e;
            _context.getCacheLog().Error("DatasourceMgr.UpdateInCache", "Error:" + e.getMessage() + " " + e.getStackTrace());
        } finally {
            if (exc != null || rollback) {
                try {
                    //rollback, removing key from cache
                    _context.getCacheLog().Error("Data source Update in cache failed, removing key:" + operation.getKey());
                    _context.getCacheImpl().Remove(operation.getKey(), ItemRemoveReason.Removed, true, null, 0, LockAccessType.IGNORE_LOCK, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));

                } catch (Exception ex) {
                    _context.getCacheLog().Error("DatasourceMgr.UpdateInCache", "Error in removing key: " + operation.getKey() + "Error: " + ex.getMessage() + " " + ex.getStackTrace());
                }
            }
            _context.PerfStatsColl.incrementCountStatsBy(_context.getCacheInternal().getSize());
        }
    }

    private boolean ValidateWriteOperation(WriteOperation operation) {
        if (operation == null) {
            _context.getCacheLog().Error("DatasourceMgr.UpdateInCache", "Write operation is not provided");
            return true;
        } else if (operation.getProviderCacheItem() == null) {
            _context.getCacheLog().Error("DatasourceMgr.UpdateInCache", "Provider cache item is not provided for key: " + operation.getKey());
            return true;
        } else if (operation.getProviderCacheItem().getValue() == null) {
            _context.getCacheLog().Error("DatasourceMgr.UpdateInCache", "Provider cache item value is not provided for key: " + operation.getKey());
            return true;
        } else {
            return false;
        }
    }

    /**
     *
     *
     * @param opCode
     * @return
     */
    public OperationResult[] WriteThru(Object[] keys, Object[] values, CacheEntry[] entries, java.util.HashMap returnSet, OpCode opCode, String providerName, OperationContext operationContext) throws OperationFailedException {
        OperationResult[] resultSet = null;
        if (_writerProivder == null) {
            return null;
        }
        WriteThruProviderMgr writeThruManager = GetProvider(providerName);
        if (writeThruManager != null) {
            DSWriteOperation[] dsOperations = new DSWriteOperation[keys.length];
            if (values == null) {
                values = new Object[keys.length];
            }
            
//            if(entries == null) {
//                entries = new CacheEntry[keys.length];
//            }
            if(opCode == OpCode.Remove) {
                for (int i = 0; i < keys.length; i++) {
                    dsOperations[i] = new DSWriteOperation(_context.getSerializationContext(), keys[i], null, null, opCode, providerName);
                }
            }
            else {
                for (int i = 0; i < keys.length; i++) {
                    dsOperations[i] = new DSWriteOperation(_context.getSerializationContext(), keys[i], values[i], entries[i], opCode, providerName);
                }
            }

            
            resultSet = writeThruManager.WriteThru(_context.getCacheImpl(), dsOperations, returnSet, false, operationContext);
            if (resultSet == null) {
                return null;
            }
            //synchronously applying update to cache store in case of write thru
            DSUpdateInCache(resultSet, writeThruManager.getLanguageContext());
        }
        return resultSet;
    }

    public final OperationResult WriteThru(DSWriteBehindOperation operation, OperationContext context)throws OperationFailedException{
        OperationResult result = null;
        if (_writerProivder == null) {
            return null;
        }
        WriteThruProviderMgr writeThruManager = GetProvider(operation.getProviderName());
        if (writeThruManager != null) {
            result = writeThruManager.WriteThru(_context.getCacheImpl(), operation, true, context);
            if (result != null && result.getUpdateInCache()) {
                if (result.getDSOperationStatus() == OperationResult.Status.Success && result.getOperation() != null) {
                    switch (result.getOperation().getOperationType()) {
                        case Add:
                        case Update:
                            _updateOpProviderMgr.put(result.getOperation().getKey(), writeThruManager.getLanguageContext());
                            this._dsUpdateProcessor.Enqueue(result.getOperation());
                            break;
                    }
                }
            }
        }
        return result;
    }

    public final OperationResult[] WriteThru(DSWriteBehindOperation[] operations, String provider, java.util.HashMap returnSet, OperationContext context)throws OperationFailedException{
        OperationResult[] result = null;
        if (_writerProivder == null) {
            return null;
        }
        WriteThruProviderMgr writeThruManager = GetProvider(provider);
        if (writeThruManager != null) {
            result = writeThruManager.WriteThru(_context.getCacheImpl(), operations, returnSet, true, context);
            if (result == null) {
                return null;
            }
            //enqueue operations in update queue
            for (int i = 0; i < result.length; i++) {
                if (result[i] != null && result[i].getUpdateInCache()) {
                    if (result[i].getDSOperationStatus() == OperationResult.Status.Success && result[i].getOperation() != null) {
                        switch (result[i].getOperation().getOperationType()) {
                            case Add:
                            case Update:
                                _updateOpProviderMgr.put(result[i].getOperation().getKey(), writeThruManager.getLanguageContext());
                                this._dsUpdateProcessor.Enqueue(result[i].getOperation());
                                break;
                        }
                    }
                }
            }
        }
        return result;
    }

    public final void DSUpdateInCache(OperationResult[] resultSet, LanguageContext languageContext) {
        if (resultSet == null) {
            return;
        }
        BitSet[] flags = new BitSet[resultSet.length];
        Object[] keys = new Object[resultSet.length];
        CacheEntry[] enteries = new CacheEntry[resultSet.length];
        java.util.ArrayList keysToRemove = new java.util.ArrayList();
        int counter = 0;
        OperationResult.Status status;
        for (int i = 0; i < resultSet.length; i++) {
            if (!(resultSet[i].getUpdateInCache())) {
                continue;
            }

            WriteOperation operation = resultSet[i].getOperation();
            boolean rollback = ValidateWriteOperation(operation);

            if (rollback) {
                keysToRemove.add(operation);
            } else if (resultSet[i].getDSOperationStatus() == OperationResult.Status.Success) {
                ProviderCacheItem cacheItem = resultSet[i].getOperation().getProviderCacheItem();
                if (cacheItem == null) {
                    continue;
                }

                try {
                    CacheEntry entry = null;
                    flags[i] = new BitSet();
                    tangible.RefObject<BitSet> tempRef_Object = new tangible.RefObject<BitSet>(flags[i]);
                    tangible.RefObject<CacheEntry> tempRef_entry = new tangible.RefObject<CacheEntry>(entry);
                    UserBinaryObject userBinaryObject = GetCacheEntry(resultSet[i].getOperation().getKey(), cacheItem, tempRef_Object, cacheItem.getGroup() != null ? cacheItem.getGroup() : null, cacheItem.getSubGroup() != null ? cacheItem.getSubGroup() : null, tempRef_entry, languageContext);
                    flags[i] = tempRef_Object.argvalue;
                    entry = tempRef_entry.argvalue;

                    if (userBinaryObject == null) {
                        continue;
                    }

                    keys[counter] = resultSet[i].getOperation().getKey();
                    enteries[counter++] = entry;
                } catch (Exception exception) {
                    _context.getCacheLog().Error("DSWrite Operation", "Error occurred while updating key: " + resultSet[i].getOperation().getKey() + " after write operations; " + exception.getMessage());
                    continue;
                }
            }
        }
        if (counter == 0) {
            return;
        }

        tangible.RefObject<Object[]> tempRef_keys = new tangible.RefObject<Object[]>(keys);
        Cache.Resize(tempRef_keys, counter);
        keys = tempRef_keys.argvalue;
        tangible.RefObject<Object[]> tempRef_enteries = new tangible.RefObject<Object[]>(enteries);
        Cache.Resize(tempRef_enteries, counter);
        enteries = tempRef_enteries.argvalue instanceof CacheEntry[]?(CacheEntry[])tempRef_enteries.argvalue:null;
        java.util.HashMap keysInserted = null;
        try {
            _context.PerfStatsColl.mSecPerDSUpdBeginSample();
            keysInserted = _context.getCacheImpl().Insert(keys, enteries, true, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
            _context.PerfStatsColl.mSecPerDSUpdEndSample(keys.length);
            _context.PerfStatsColl.incrementDSUpdatePerSecBy(keys.length);
        } catch (Exception ex) {
            try {
                _context.getCacheLog().Error("DSWrite Operation:UpdateInCache", "Data source Update in cache failed, Error: " + ex.getMessage());
                _context.getCacheImpl().Remove(keys, ItemRemoveReason.Removed, true, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
            } catch (Exception exp) {
                _context.getCacheLog().Error("DSWrite Operation:UpdateInCache", "Error occurred while removing keys ; Error: " + exp.getMessage());
            }
        } finally {
            //removing failed keys
            if (keysInserted != null && keysInserted.size() > 0) {
                for (int i = 0; i < keys.length; i++) {
                    if (keysInserted.containsKey(keys[i])) {
                        if (!(keysInserted.get(keys[i]) instanceof Exception)) {
                            CacheInsResultWithEntry insResult = (CacheInsResultWithEntry) ((keysInserted.get(keys[i]) instanceof CacheInsResultWithEntry) ? keysInserted.get(keys[i]) : null);
                            if (insResult != null && (insResult.getResult() != CacheInsResult.Success || insResult.getResult() != CacheInsResult.SuccessOverwrite)) {
                                keysToRemove.add(keys[i]);
                            }
                        } else {
                            keysToRemove.add(keys[i]);
                        }
                    }
                }
            }
            try {
                if (keysToRemove.size() > 0) {
                    Object[] selectedKeys = new Object[keysToRemove.size()];
                    System.arraycopy(keysToRemove.toArray(), 0, selectedKeys, 0, keysToRemove.size());
                    _context.getCacheImpl().Remove(selectedKeys, ItemRemoveReason.Removed, true, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
                }
            } catch (Exception exp) {
                _context.getCacheLog().Error("DSWrite Operation:UpdateInCache", "Error occurred while removing keys ; Error: " + exp.getMessage());
            }
        }
    }

    public final void DSAsyncUpdateInCache(WriteOperation operation) {
        if (_updateOpProviderMgr == null) 
        {
            return;
        }
        LanguageContext languageContext = LanguageContext.NONE;
        if (_updateOpProviderMgr.containsKey(operation.getKey())) {
            languageContext = _updateOpProviderMgr.get(operation.getKey());
            if (languageContext != LanguageContext.NONE) {
                DSUpdateInCache(operation, languageContext);
            }
        }
    }

    public final void HotApplyWriteBehind(String mode, int throttlingrate, int requeueLimit, int requeueEvictionRatio, int batchInterval, int operationDelay) {
        if (_writeBehindAsyncProcess != null) {
            _writeBehindAsyncProcess.SetConfigDefaults(mode, throttlingrate, batchInterval, operationDelay, requeueLimit, requeueEvictionRatio);
        }

    }
}
