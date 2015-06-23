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

package com.alachisoft.tayzgrid.caching.autoexpiration;

import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.ISizable;
import com.alachisoft.tayzgrid.common.ISizableIndex;
import com.alachisoft.tayzgrid.common.ServiceConfiguration;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.lang.Object;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Summary description for ExpirationManager.
 */
public class ExpirationManager implements IDisposable, ISizableIndex
{
    /**
     * The Task that takes care of auto-expiration of items.
     */
    private static class AutoExpirationTask implements TimeScheduler.Task
    {

        /**
         * Reference to the parent.
         */
        private ExpirationManager _parent = null;
        /**
         * Periodic interval
         */
        private long _interval = 1000;

        /**
         * Constructor.
         *
         * @param parent
         * @param interval
         */
        public AutoExpirationTask(ExpirationManager parent, long interval)
        {
            _parent = parent;
            _interval = interval;
        }

        public final long getInterval()
        {
            synchronized (this)
            {
                return _interval;
            }
        }

        public final void setInterval(long value)
        {
            synchronized (this)
            {
                _interval = value;
            }
        }

        /**
         * Sets the cancel flag.
         */
        public final void Cancel()
        {
            synchronized (this)
            {
                _parent = null;
            }
        }

        /**
         * True if task is cancelled, false otherwise
         */
        public final boolean getIsCancelled()
        {
            return this._parent == null;
        }

        /**
         * returns true if the task has completed.
         *
         * @return bool
         */
        public boolean IsCancelled()
        {
            synchronized (this)
            {
                return _parent == null;
            }
        }

        /**
         * tells the scheduler about next interval.
         *
         * @return
         */
        public long GetNextInterval()
        {
            synchronized (this)
            {
                return _interval;
            }
        }

        /**
         * This is the main method that runs as a thread. CacheManager does all sorts of house keeping tasks in that method.
         */
        public void Run()
        {
            if (_parent == null)
            {
                return;
            }
            try
            {
                boolean expired = _parent.Expire();
            }
            catch (Exception e)
            {
            }
        }
    }

    private static class ExpiryIndexEntry implements ISizable
    {

        private ExpirationHint _hint;
       
        public ExpiryIndexEntry(ExpirationHint hint, boolean hasDependentKeys)
        {
            _hint = hint;
          
        }

        public ExpiryIndexEntry(ExpirationHint hint)
        {
            this(hint, false);
        }

        public final boolean IsExpired(CacheRuntimeContext context)
        {
            if (_hint != null)
            {
                return _hint.DetermineExpiration(context);
            }
            return false;
        }

     

        public final ExpirationHint getHint()
        {
            return _hint;
        }

        public final void setHint(ExpirationHint value)
        {
            _hint = value;
        }
        
        public int getSize()
        {
            return getExpiryIndexEntrySize();
        }
        public int getInMemorySize()
        {
            int inMemorySize = this.getSize();
            inMemorySize += inMemorySize <= 24 ? 0 : MemoryUtil.NetOverHead;
            
            return inMemorySize;
        }
        
        private int getExpiryIndexEntrySize()
        {
            int temp = 0;
            temp += com.alachisoft.tayzgrid.common.util.MemoryUtil.NetByteSize; // for _hasDependentKeys
            temp += _hint.getInMemorySize();
            return temp;
        }
    }
    
    /**
     * The top level Cache. esentially to remove the items on the whole cluster for the cascaded dependencies.
     */
    private Cache _topLevelCache;
    /**
     * The runtime context associated with the current cache.
     */
    private CacheRuntimeContext _context;
    /**
     * The periodic auto-expiration task.
     */
    private AutoExpirationTask _taskExpiry;
        
    /**
     * clean interval for expiration in milliseconds.
     */
    private int _cleanInterval = 30000;
    /**
     * maximum ratio of items that can be removed on each clean interval.
     */
    private float _cleanRatio = 1;
    
    /**
     * to determine the last slot so expiration can be round robin.
     */
    private boolean _allowClusteredExpiry = true;
  
    /**
     * A value used for place holder
     */
    private static Object _DATA = new Object();
    /**
     * A counter which tells how many time expiration occured
     */
    private long _runCount;
    private java.util.HashMap _mainIndex = new java.util.HashMap(25000, 0.7f);
    
    /**
     * locked for the selection of expired items.
     */
    private HashMap _transitoryIndex = new java.util.HashMap(25000, 0.7f);//
    /**
     * A flag used to indicate that index is cleared while expiration was in progress.
     */
    private boolean _indexCleared;
    /**
     * flag that indicates that the cache has been cleared after we have selected the keys for expiration. If set we dont continue with the expiration coz cache has already been
     * cleared.
     */
    private boolean _cacheCleared = false;
    private Object _status_mutex = new Object();
    /**
     * It is the interval between two consecutive removal of items from the cluster so that user operation is not affected
     */
    private long _sleepInterval = 0; //milliseconds
    /**
     * No of items which can be removed in a single clustered operation.
     */
    private int _removeThreshhold = 10;
    /**
     * Flag which indicates whether to explicitly call GC.Collect or not
     */
    private boolean _allowExplicitGCCollection = true;
    private boolean _inProgress;


    /**
     * Is this node the coordinator node. useful to synchronize database dependent items.
     */
    private boolean _isCoordinator = true;
    /**
     * Is this node the sub-coordinator node in partitione-of-replica topology. for all other tolpologies its false.
     */
    private boolean _isSubCoordinator = false;
    
    /**
     * Accumulated size of Expiration Manager
     */
    private long _expirationManagerSize = 0;
    
    /**
     * MaxCount of Index Hashtable
     */
    private long _mainIndexMaxCount = 0;
    
    /**
     * MaxCount of Transitory Index Hashtable
     */
    private long _transitoryIndexMaxCount = 0;
    
    private ILogger _ncacheLog;

    private ILogger getCacheLog()
    {
        return _ncacheLog;
    }
    private int _cacheLastAccessLoggingInterval = 20;
    private int _cacheLastAccessLoggingIntervalPassed;
    private int _cacheLastAccessInterval;
    private boolean _cacheLastAccessCountEnabled;
    private boolean _cacheLastAccessCountLoggingEnabled;

    /**
     * True if this node is a "cordinator" or a "subcordinator" in case of partition-of-replica.
     */
    public final boolean getIsCoordinatorNode()
    {
        return _isCoordinator;
    }

    public final void setIsCoordinatorNode(boolean value)
    {
        _isCoordinator = value;
    }

    /**
     * A flag which indicates whether expiration is in progress
     */
    public final boolean getIsInProgress()
    {
        synchronized (_status_mutex)
        {
            return _inProgress;
        }
    }

    public final void setIsInProgress(boolean value)
    {
        synchronized (_status_mutex)
        {
            _inProgress = value;
        }
    }

    /**
     * Top Level Cache only to remove the cascaded dependencies on clean interval. which is started from top level cache.
     */
    public final Cache getTopLevelCache()
    {
        return _topLevelCache;
    }

    public final void setTopLevelCache(Cache value)
    {
        _topLevelCache = value;
    }

    private boolean getIsCacheLastAccessCountEnabled()
    {
        boolean isCachelastAccessEnabled = false;
        try
        {

            String str = ServicePropValues.CacheServer_EnableCacheLastAccessCount;

            if (str != null && !str.equals(""))
            {
                isCachelastAccessEnabled = Boolean.parseBoolean(str);
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("ExpirationManager.IsCacheLastAccessCountEnabled", "invalid value provided for CacheServer.EnableCacheLastAccessCount");
        }
        return isCachelastAccessEnabled;
    }

    private boolean getIsCacheLastAccessLoggingEnabled()
    {
        boolean isCachelastAccessLogEnabled = false;
        try
        {
            String str = ServicePropValues.CacheServer_EnableCacheLastAccessCountLogging;

            if (str != null && !str.equals(""))
            {
                isCachelastAccessLogEnabled = Boolean.parseBoolean(str);
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("ExpirationManager.IsCacheLastAccessLoggingEnabled", "invalid value provided for CacheServer.EnableCacheLastAccessCount");
        }

        if (getIsCacheLastAccessCountEnabled() && isCachelastAccessLogEnabled)
        {
            //: Cmbine file paths
            String path = Common.combinePath(AppUtil.getInstallDir(), ServicePropValues.LOGS_FOLDER);
            getCacheLog().Info(_context.getSerializationContext() + (_context.getIsStartedAsMirror() ? "-replica" : "") + "." + "cache-last-acc-log " + path);
        }

        return isCachelastAccessLogEnabled;
    }

    private int getCacheLastAccessCountInterval()
    {
        int isCachelastAccessInterval = _cacheLastAccessInterval;
        try
        {
            String str = ServicePropValues.CacheServer_CacheLastAccessCountInterval;

            if (str != null && !str.equals(""))
            {
                isCachelastAccessInterval = Integer.parseInt(str);
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("ExpirationManager.CacheLastAccessCountInterval", "invalid value provided for CacheServer.CacheLastAccessCountInterval");
        }
        return isCachelastAccessInterval;
    }

    private int getCacheLastAccessLoggingInterval()
    {
        int isCachelastAccessLogingInterval = _cacheLastAccessLoggingInterval;
        try
        {
            String str = ServicePropValues.CacheServer_CacheLastAccessLogInterval;

            if (str != null && !str.equals(""))
            {
                isCachelastAccessLogingInterval = Integer.parseInt(str);
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("ExpirationManager.CacheLastAccessLogInterval", "invalid value provided for CacheServer.CacheLastAccessLogInterval");
        }
        return isCachelastAccessLogingInterval;
    }

    /**
     * Overloaded Constructor
     *
     * @param timeSched
     */
    public ExpirationManager(java.util.Map properties, CacheRuntimeContext context)
    {
        _context = context;
        _ncacheLog = context.getCacheLog();

        Initialize(properties);
        if (ServicePropValues.CacheServer_ExpirationBulkRemoveDelay != null)
        {
            _sleepInterval = (Integer.decode(ServicePropValues.CacheServer_ExpirationBulkRemoveDelay)) * 1000;
        }
        if (ServicePropValues.CacheServer_ExpirationBulkRemoveSize != null)
        {
            _removeThreshhold = (Integer.decode(ServicePropValues.CacheServer_ExpirationBulkRemoveSize));
        }
        //new way to do this...
        _sleepInterval = Integer.parseInt(ServiceConfiguration.getExpirationBulkRemoveDelay());
        _removeThreshhold = Integer.parseInt(ServiceConfiguration.getExpirationBulkRemoveSize());

        if (ServicePropValues.CacheServer_EnableGCCollection != null)
        {
            _allowExplicitGCCollection = (Boolean.parseBoolean(ServicePropValues.CacheServer_EnableGCCollection));
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     */
    public void dispose()
    {
        if (_taskExpiry != null)
        {
            _taskExpiry.Cancel();
            _taskExpiry = null;
        }

        synchronized (_status_mutex)
        {
            _mainIndex.clear();
            _mainIndex = null;

            _transitoryIndex.clear();
            _transitoryIndex = null;
            
            _expirationManagerSize = 0;
        }

    }

    public final long getCleanInterval()
    {
        return _taskExpiry.getInterval();
    }

    public final void setCleanInterval(long value)
    {
        _taskExpiry.setInterval(value);
    }

    /**
     * True if expiry task is disposed, flase otherwise.
     */
    public final boolean getIsDisposed()
    {
        return !(_taskExpiry != null && !_taskExpiry.getIsCancelled());
    }



    /**
     * keys on which key dependency exists.
     */
    public final boolean getAllowClusteredExpiry()
    {
        synchronized (this)
        {
            return _allowClusteredExpiry;
        }
    }

    public final void setAllowClusteredExpiry(boolean value)
    {
        synchronized (this)
        {
            _allowClusteredExpiry = value;
        }
    }

 /**
     * Initialize expiration manager based on Configuration
     *
     * @param properties
     */
    private void Initialize(java.util.Map properties)
    {
        if (properties == null)
        {
            throw new IllegalArgumentException("properties");
        }

        if (properties.containsKey("clean-interval"))
        {
            _cleanInterval = (Integer.decode(properties.get("clean-interval").toString())) * 1000;
        }
        _cacheLastAccessCountEnabled = getIsCacheLastAccessCountEnabled();
        _cacheLastAccessCountLoggingEnabled = getIsCacheLastAccessLoggingEnabled();
        _cacheLastAccessInterval = getCacheLastAccessCountInterval();
        _cacheLastAccessLoggingInterval = getCacheLastAccessLoggingInterval();
    }

    /**
     * Start the auto-expiration task
     */
    public final void Start()
    {
        if (_taskExpiry == null)
        {
            _taskExpiry = new AutoExpirationTask(this, _cleanInterval);
            _context.TimeSched.AddTask(_taskExpiry);
        }
    }

    /**
     * Stop the auto-expiration task
     */
    public final void Stop()
    {
        if (_taskExpiry != null)
        {
            _taskExpiry.Cancel();
        }
    }

    /**
     * Initialize the new hint. if no new hint is specified then dispose the old hint.
     *
     * @param oldHint
     * @param newHint
     */
    public final void ResetHint(ExpirationHint oldHint, ExpirationHint newHint) throws OperationFailedException
    {
        synchronized (this)
        {
            if (newHint != null)
            {
                if (oldHint != null)
                { 
                    //dispose only if newHint is not null
                    ((IDisposable) oldHint).dispose();
                }
                newHint.Reset(_context);
            }
        }
    }

    public final void ResetVariant(ExpirationHint hint) throws OperationFailedException
    {
        synchronized (this)
        {
            hint.ResetVariant(_context);
        }
    }

    /**
     * Clear the expiration index
     */
    public final void Clear()
    {
        synchronized (this)
        {
            _cacheCleared = true;
        }
        synchronized (_status_mutex)
        {
            if (!getIsInProgress())
            {
                _mainIndex = new HashMap(25000, 0.7f);
                _transitoryIndex = new HashMap(25000, 0.7f);

                _transitoryIndexMaxCount = 0;
                _mainIndexMaxCount = 0;
            }
            else
            {
                _transitoryIndex = new HashMap(25000, 0.7f);
                _transitoryIndexMaxCount = 0;
                _indexCleared = true;
            }
            _expirationManagerSize = 0;
        }
    }

    /**
     * Called by the scheduler to remove the items that has expired
     */
    public final boolean Expire() throws InterruptedException, ArgumentException
    {
        //indicates whether some items expired during this interval or not...
        boolean expired = false;

        long currentRun = 0;
        synchronized (this)
        {
            currentRun = _runCount++;
        }

        _sleepInterval = Integer.parseInt(ServiceConfiguration.getExpirationBulkRemoveDelay());
        _removeThreshhold = Integer.parseInt(ServiceConfiguration.getExpirationBulkRemoveSize());

        //notification is sent for a max of 100k data if multiple items...
        //otherwise if a single item is greater than 100k then notification is sent for
        //that item only...
        int notifThreshold = 30 * 1024;

        CacheBase cacheInst = _context.getCacheImpl();
        CacheBase cache = _context.getCacheInternal();
        Cache rootCache = _context.getCacheRoot();
        Object[] keys = null;
        Object[] values = null;

        if (cache == null)
        {
            throw new UnsupportedOperationException("No cache instance defined");
        }

        boolean allowExpire = getAllowClusteredExpiry();

        //in case of replication and por, only the coordinator/sub-coordinator is responsible to expire the items.
        if (!allowExpire)
        {
            return false;
        }
        java.util.ArrayList selectedKeys = new java.util.ArrayList();
        java.util.ArrayList cascadedKeys = new java.util.ArrayList();
        int oldItemsCount = 0;
        java.util.HashMap oldeItems = null;

        try
        {
            StartLogging();
            int currentTime = AppUtil.DiffSeconds(Calendar.getInstance().getTime());

            int cleanSize = (int) Math.ceil(cache.getCount() * _cleanRatio);

            //set the flag that we are going to expire the items.

            if (_cacheLastAccessLoggingIntervalPassed >= _cacheLastAccessLoggingInterval)
            {
                _cacheLastAccessLoggingInterval = getCacheLastAccessLoggingInterval();
                _cacheLastAccessCountEnabled = getIsCacheLastAccessCountEnabled();
                _cacheLastAccessCountLoggingEnabled = getIsCacheLastAccessLoggingEnabled();
                _cacheLastAccessInterval = getCacheLastAccessCountInterval();
            }
            else
            {
                _cacheLastAccessLoggingIntervalPassed++;
            }


            if (_cacheLastAccessCountEnabled && _cacheLastAccessCountLoggingEnabled)
            {
                if (_cacheLastAccessLoggingIntervalPassed >= _cacheLastAccessLoggingInterval)
                {
                    _cacheLastAccessLoggingIntervalPassed = 0;
                    oldeItems = new java.util.HashMap();
                }
            }

            synchronized (_mainIndex)
            {
                Iterator em = _mainIndex.entrySet().iterator();

                if (em != null)
                {
                    while (em.hasNext())
                    {
                        Map.Entry pair = (Map.Entry) em.next();
                        ExpiryIndexEntry expirtyEntry = (ExpiryIndexEntry) ((pair.getValue() instanceof ExpiryIndexEntry) ? pair.getValue() : null);
                        ExpirationHint hint = expirtyEntry.getHint();
                        if (hint != null && _cacheLastAccessCountEnabled && hint instanceof IdleExpiration)
                        {
                            IdleExpiration slidingExpHint = (IdleExpiration) ((hint instanceof IdleExpiration) ? hint : null);
                            TimeSpan diff = TimeSpan.Subtract(AppUtil.GetDateTime(AppUtil.DiffSeconds(Calendar.getInstance().getTime())), AppUtil.GetDateTime(slidingExpHint.getLastAccessTime()));
                            if (diff.getTotalMinutes() >= _cacheLastAccessInterval)
                            {
                                oldItemsCount++;
                                if (oldeItems != null)
                                {
                                    oldeItems.put(pair.getKey(), null);
                                }
                            }
                        }
                        if (hint == null || (new Integer(hint.getSortKey())).compareTo(currentTime) >= 0)
                        {
                            continue;
                        }

                        if (!allowExpire && hint.getIsRoutable())
                        {
                            continue;
                        }

                        if (hint.DetermineExpiration(_context))
                        {
                            if (hint.getNeedsReSync() && _context.getDsMgr() != null)
                            {
                                //get old entry to know existing groupinfo and queryinfo for tag purposes.
                                CacheEntry oldEntry = cache.Get(pair.getKey(), new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));


                                if (oldEntry != null)
                                {
                                    _context.getDsMgr().ResyncCacheItemAsync(pair.getKey(), hint, null, oldEntry.getGroupInfo(), oldEntry.getQueryInfo(), oldEntry.getResyncProviderName());
                                }
                                else
                                {
                                    _context.getDsMgr().ResyncCacheItemAsync(pair.getKey(), hint, null, null, null, oldEntry != null ? oldEntry.getResyncProviderName() : null);
                                }
                            }
                            else
                            {
                                selectedKeys.add(pair.getKey());
                            }
                            if (cleanSize > 0 && selectedKeys.size() == cleanSize)
                            {
                                break;
                            }
                        }

                    }
                }
            }

            if (getCacheLog().getIsInfoEnabled())
            { 
                getCacheLog().Info("ExpirationManager.Expire()", String.format("Expiry time for {0}/{1} Items: " + (TimeSpan.Subtract(NCDateTime.getUTCNow(), new Date())), selectedKeys.size(), cache.getCount()));
            }
        }
        catch (Exception e)
        {
            getCacheLog().Error("ExpirationManager.Expire(bool)", "LocalCache(Expire): " + e.toString());
        }
        finally
        {
            _context.PerfStatsColl.incrementCacheLastAccessCountStats(oldItemsCount);
            ApplyLoggs();
            java.util.ArrayList dependentItems = new java.util.ArrayList();
            java.util.ArrayList removedItems = null;
            java.util.Date startTime = new java.util.Date();
            try
            {
                if (selectedKeys.size() > 0)
                {
                    //new architectural changes begins from here.
                    java.util.ArrayList keysTobeRemoved = new java.util.ArrayList();

                    for (int i = 0; i < selectedKeys.size() && !_cacheCleared; i++)
                    {
                        keysTobeRemoved.add(selectedKeys.get(i));
                        if (keysTobeRemoved.size() % _removeThreshhold == 0)
                        {
                            try
                            {
                                if (this.getIsDisposed())
                                {
                                    break;
                                }
                                OperationContext operationContext = new OperationContext();
                                operationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                                
                                Object tempVar = cache.RemoveSync(keysTobeRemoved.toArray(new Object[0]), ItemRemoveReason.Expired, false, operationContext);
                                removedItems = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
                                //set the flag that item has expired from cache...
                                expired = true;
                                if (_context.PerfStatsColl != null)
                                {
                                    _context.PerfStatsColl.incrementExpiryPerSecStatsBy(keysTobeRemoved.size());
                                }
                            }
                            catch (Exception e)
                            {
                                getCacheLog().Error("ExpiryManager.Expire", "an error occured while removing expired items. Error " + e.toString());
                            }
                            keysTobeRemoved.clear();
                            if (removedItems != null && removedItems.size() > 0)
                            {
                                dependentItems.addAll(removedItems);
                            }
                            //we stop the activity of the current thread so that normal user operation is not affected.
                            Thread.sleep(_sleepInterval * 1000L);
                        }
                    }

                    if (!this.getIsDisposed() && keysTobeRemoved.size() > 0)
                    {
                        try
                        {
                            OperationContext operationContext = new OperationContext();
                            operationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                            
                            Object tempVar2 = cache.RemoveSync(keysTobeRemoved.toArray(new Object[0]), ItemRemoveReason.Expired, false, operationContext);
                            removedItems = (java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null);

                            //set the flag that item has expired from cache...
                            expired = true;
                            if (_context.PerfStatsColl != null)
                            {
                                _context.PerfStatsColl.incrementExpiryPerSecStatsBy(keysTobeRemoved.size());
                            }
                            if (removedItems != null && removedItems.size() > 0)
                            {
                                dependentItems.addAll(removedItems);
                            }
                        }
                        catch (Exception e)
                        {
                            getCacheLog().Error("ExpiryManager.Expire", "an error occured while removing expired items. Error " + e.toString());
                        }
                    }
                }

                if (!this.getIsDisposed() && dependentItems.size() > 0)
                {
                    java.util.ArrayList removableList = new java.util.ArrayList();
                    if (rootCache != null)
                    {
                        for (Object depenentItme : dependentItems)
                        {
                            if (depenentItme == null)
                            {
                                continue;
                            }
                            removableList.add(depenentItme);
                            if (removableList.size() % 100 == 0)
                            {
                                try
                                {
                                    if (this.getIsDisposed())
                                    {
                                        break;
                                    }

                                    OperationContext operationContext = new OperationContext();
                                    operationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                                    
                                    rootCache.CascadedRemove(removableList.toArray(new Object[0]), ItemRemoveReason.Expired, true, operationContext);
                                    if (_context.PerfStatsColl != null)
                                    {
                                        _context.PerfStatsColl.incrementExpiryPerSecStatsBy(removableList.size());
                                    }
                                }
                                catch (Exception exc)
                                {
                                    getCacheLog().Error("ExpiryManager.Expire", "an error occured while removing dependent items. Error " + exc.toString());
                                }
                                removableList.clear();
                            }
                        }
                        if (!this.getIsDisposed() && removableList.size() > 0)
                        {
                            try
                            {
                                OperationContext operationContext = new OperationContext();
                                operationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                                
                                rootCache.CascadedRemove(removableList.toArray(new Object[0]), ItemRemoveReason.Expired, true, operationContext);

                                if (_context.PerfStatsColl != null)
                                {
                                    _context.PerfStatsColl.incrementExpiryPerSecStatsBy(removableList.size());
                                }
                            }
                            catch (Exception exc)
                            {
                                getCacheLog().Error("ExpiryManager.Expire", "an error occured while removing dependent items. Error " + exc.toString());
                            }
                            removableList.clear();
                        }
                    }
                }
            }
            finally
            {
                _transitoryIndex.clear();
                synchronized (this)
                {
                    _cacheCleared = false;
                }

                if (oldeItems != null)
                {
                    StringBuilder sb = new StringBuilder();
                    Iterator ide = oldeItems.entrySet().iterator();
                    int count = 1;
                    while (ide.hasNext())
                    {
                        Map.Entry pair = (Map.Entry) ide.next();
                        sb.append(pair.getKey() + ", ");

                        if (count % 10 == 0)
                        {
                            sb.append("\r\n");
                            count = 1;
                        }
                        else
                        {
                            count++;
                        }
                    }
                    getCacheLog().Info(sb.toString().trim());
                }
            }
        }
        return expired;
    }

    public final void UpdateIndex(Object key, CacheEntry entry)
    {
        if (entry == null || entry.getExpirationHint() == null)
        {
            return;
        }
       
        UpdateIndex(key, entry.getExpirationHint());
    }

    public final void UpdateIndex(Object key, ExpirationHint hint)
    {
        if (key == null || hint == null)
        {
            return;
        }

        synchronized (_status_mutex)
        {
            int addSize = 0;
            int removeSize = 0;
            
            if (!getIsInProgress())
            {
                if (!_mainIndex.containsKey(key))
                {
                    ExpiryIndexEntry entry = new ExpiryIndexEntry(hint);
                    _mainIndex.put(key, entry);
                    
                    addSize = entry.getInMemorySize();
                    
                    if(_mainIndex.size() > _mainIndexMaxCount)
                        _mainIndexMaxCount = _mainIndex.size();
                }
                else
                {
                    ExpiryIndexEntry expEntry = (ExpiryIndexEntry) ((_mainIndex.get(key) instanceof ExpiryIndexEntry) ? _mainIndex.get(key) : null);
                    if (expEntry != null)
                    {
                        removeSize = expEntry.getInMemorySize();
                        
                        expEntry.setHint(hint);
                       
                        
                        addSize = expEntry.getInMemorySize();
                    }
                }
            }
            else
            {
                if (_transitoryIndex.get(key) == null)
                {
                    ExpiryIndexEntry entry = new ExpiryIndexEntry(hint);
                    _transitoryIndex.put(key, entry);
                    
                    addSize = entry.getInMemorySize();
                    
                    if(_transitoryIndex.size() > _transitoryIndexMaxCount)
                        _transitoryIndexMaxCount = _transitoryIndex.size();
                }
                else
                {
                    ExpiryIndexEntry expEntry = (ExpiryIndexEntry) ((_transitoryIndex.get(key) instanceof ExpiryIndexEntry) ? _transitoryIndex.get(key) : null);
                    if (expEntry != null)
                    {
                        removeSize = expEntry.getInMemorySize();
                        
                        expEntry.setHint(hint);
                      
                        
                        addSize = expEntry.getInMemorySize();
                    }
                }
            }
            _expirationManagerSize -= removeSize;
            _expirationManagerSize += addSize;
        }
    }

    public final void RemoveFromIndex(Object key)
    {
        synchronized (_status_mutex)
        {
            int removeSize = 0;
            if (!getIsInProgress())
            {
                ExpiryIndexEntry expEntry = (ExpiryIndexEntry) ((_mainIndex.get(key) instanceof ExpiryIndexEntry) ? _mainIndex.get(key) : null);
                if(expEntry != null)
                {
                    removeSize = expEntry.getInMemorySize();
                }
                _mainIndex.remove(key);
            }
            else
            {
                //Adding a with null value indicates that this key has been
                //removed so we should remove it from the main index.
                ExpiryIndexEntry expEntry = (ExpiryIndexEntry) ((_transitoryIndex.get(key) instanceof ExpiryIndexEntry) ? _transitoryIndex.get(key) : null);
                if(expEntry != null)
                {
                    removeSize = expEntry.getInMemorySize();
                }
                _transitoryIndex.put(key, null);
            }
            _expirationManagerSize -= removeSize;
        }
    }

    /**
     * We log all the operations in a transitory index when we are iterating on the main index to determine the expired items. StartLogging causes all the the subsequent operation
     * to be directed to the transitory index.
     */
    private void StartLogging()
    {
        setIsInProgress(true);
    }

    /**
     * We log all the operations in a transitory index when we are iterating on the main index to determine the expired items. StopLogging should be called after selection of item
     * is completd. We apply all the logs from transitory index to the main index. A null value in transitory index against a key indicates that this item is removed during
     * logging, so we should remove it from the main log as well.
     */
    private void ApplyLoggs()
    {
        synchronized (_status_mutex)
        {
            setIsInProgress(false);
            if (_indexCleared)
            {
                //_mainIndex.clear();
                _mainIndex = new HashMap(25000, 0.7f);
                _mainIndexMaxCount = 0;
                _indexCleared = false;
            }

            Iterator ide = _transitoryIndex.entrySet().iterator();

            Object key;
            ExpiryIndexEntry expEntry;
            while (ide.hasNext())
            {
                Map.Entry pair = (Map.Entry) ide.next();
                key = pair.getKey();
                expEntry = (ExpiryIndexEntry) ((pair.getValue() instanceof ExpiryIndexEntry) ? pair.getValue() : null);

                ExpiryIndexEntry oldEntry = (ExpiryIndexEntry) _mainIndex.get(key);
                
                if (expEntry != null)
                {
                    _mainIndex.put(key, expEntry);
                }
                else
                {
                    //it means this item has been removed;
                    _mainIndex.remove(key);
                }
                
                if(oldEntry != null)
                    _expirationManagerSize -= oldEntry.getInMemorySize();
            }
        }
    }
    
    @Override
    public long getIndexInMemorySize()
    {
        return _expirationManagerSize + 
                        ((_mainIndexMaxCount + _transitoryIndexMaxCount) * 
                                            MemoryUtil.NetHashtableOverHead);
    }
}
