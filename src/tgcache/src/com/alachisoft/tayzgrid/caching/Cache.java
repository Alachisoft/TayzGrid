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
package com.alachisoft.tayzgrid.caching;




import static com.alachisoft.tayzgrid.caching.OperationContextFieldName.DeleteParams;

import com.alachisoft.tayzgrid.caching.alertspropagators.EmailNotifierArgs;
import com.alachisoft.tayzgrid.caching.autoexpiration.AggregateExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationManager;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;

import com.alachisoft.tayzgrid.caching.cacheloader.CacheStartupLoader;
import com.alachisoft.tayzgrid.caching.cacheloader.LoadCacheTask;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DSWriteBehindOperation;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DatasourceMgr;
import com.alachisoft.tayzgrid.caching.datasourceproviders.WriteBehindAsyncProcessor;
import com.alachisoft.tayzgrid.caching.emailalertpropagator.EmailAlertNotifier;
import com.alachisoft.tayzgrid.caching.enumeration.CacheSnapshotPool;
import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.evictionpolicies.PriorityEvictionHint;
import com.alachisoft.tayzgrid.caching.queries.DeleteQueryResultSet;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.caching.statistics.PerfStatsCollector;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import static com.alachisoft.tayzgrid.caching.topologies.CacheAddResult.Success;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.topologies.CacheSyncWrapper;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.IClusterEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.clustered.ClusterCacheBase;
import com.alachisoft.tayzgrid.caching.topologies.clustered.PartitionedServerCache;
import com.alachisoft.tayzgrid.caching.topologies.clustered.ReplicatedServerCache;
import com.alachisoft.tayzgrid.caching.topologies.local.IndexedLocalCache;
import com.alachisoft.tayzgrid.caching.topologies.local.IndexedOverflowCache;
import com.alachisoft.tayzgrid.caching.topologies.local.LocalCache;
import com.alachisoft.tayzgrid.caching.topologies.local.LocalCacheImpl;
import com.alachisoft.tayzgrid.caching.util.AsyncAdd;
import com.alachisoft.tayzgrid.caching.util.AsyncBulkGet;
import com.alachisoft.tayzgrid.caching.util.AsyncClear;
import com.alachisoft.tayzgrid.caching.util.AsyncInsert;
import com.alachisoft.tayzgrid.caching.util.AsyncRemove;
import com.alachisoft.tayzgrid.caching.util.CacheHelper;
import com.alachisoft.tayzgrid.caching.util.CacheInfo;
import com.alachisoft.tayzgrid.caching.util.ConvHelper;
import com.alachisoft.tayzgrid.caching.util.HotConfig;
import com.alachisoft.tayzgrid.caching.util.MiscUtil;
import com.alachisoft.tayzgrid.cluster.ChannelClosedException;
import com.alachisoft.tayzgrid.cluster.ChannelException;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.CacheConfigParams;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.DeleteParams;
import com.alachisoft.tayzgrid.common.EventCategories;
import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.common.InsertResult;
import com.alachisoft.tayzgrid.common.LockOptions;
import com.alachisoft.tayzgrid.common.PortPool;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.caching.expiration.ExpirationContract;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.Monitor;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.NewHashmap;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;
import com.alachisoft.tayzgrid.common.enums.DataFormat;
import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.event.NEvent;
import com.alachisoft.tayzgrid.common.event.NEventEnd;
import com.alachisoft.tayzgrid.common.event.NEventStart;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.logger.JLogger;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatistics;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatus;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.propagator.AlertNotificationTypes;
import com.alachisoft.tayzgrid.common.propagator.IAlertPropagator;
import com.alachisoft.tayzgrid.common.remoting.RemotingChannels;
import com.alachisoft.tayzgrid.common.stats.HPTime;
import com.alachisoft.tayzgrid.common.stats.HPTimeStats;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.threading.Latch;
import com.alachisoft.tayzgrid.common.threading.TimeScheduler;
import com.alachisoft.tayzgrid.common.util.AuthenticateFeature;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.PortCalculator;
import com.alachisoft.tayzgrid.config.ConfigHelper;
import com.alachisoft.tayzgrid.config.ConfigReader;
import com.alachisoft.tayzgrid.config.PropsConfigReader;
import com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy;
import com.alachisoft.tayzgrid.config.newdom.Provider;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.parser.AttributeIndexNotDefined;
import com.alachisoft.tayzgrid.parser.TypeIndexNotDefined;
import com.alachisoft.tayzgrid.persistence.PersistenceManager;
import com.alachisoft.tayzgrid.processor.EntryProcessorManager;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.OperationResult;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteOperation;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.NotSupportedException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationNotSupportedException;
import com.alachisoft.tayzgrid.runtime.exceptions.StateTransferInProgressException;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessor;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessorResult;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.standard.FormatterServices;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import tangible.RefObject;

/**
 * The main class that is the interface of the system with the outside world.
 * This class is remotable (MarshalByRefObject).
 */
public class Cache /*extends MarshalByRefObject*/ implements Iterable, ICacheEventsListener, IClusterEventsListener, IDisposable {
    private boolean _deathDetectionEnabled;
    private int _graceTime;

    @Override
    public Iterator iterator() {
        try {
            return this.GetEnumerator();
        } catch (OperationFailedException operationFailedException) {
            throw new RuntimeException(operationFailedException.getMessage(), operationFailedException.getCause());
        }
    }

    /**
     * The name of the cache instance.
     */
    private CacheInfo _cacheInfo = new CacheInfo();
    /**
     * The runtime context associated with the current cache.
     */
    private CacheRuntimeContext _context = new CacheRuntimeContext();

    /**
     * The runtime context associated with the current cache.
     */
    private RemotingChannels _channels;
    /**
     * delegate for item addition notifications.
     */
    private NEvent _itemAdded;
    /**
     * delegate for item updation notifications.
     */
    private NEvent _itemUpdated;
    /**
     * delegate for item removal notifications.
     */
    private NEvent _itemRemoved;
    /**
     * delegate for cache clear notifications.
     */
    private NEvent _cacheCleared;
    /**
     * delegate for custom notification.
     */
    private NEvent _cusotmNotif;
    /**
     * delegate for custom remove callback notifications.
     */
    private NEvent _customRemoveNotif;
    /**
     * delegate for custom update callback notifications.
     */
    private NEvent _customUpdateNotif;
    
    /**
     * delegate for async operations.
     */
    private NEvent _asyncOperationCompleted;
    private NEvent _dataSourceUpdated;
    private NEvent _cacheStopped;
    private NEvent _cacheBecomeActive;
    private NEvent _memberJoined;
    private NEvent _memberLeft;
    private NEvent _blockClientActivity;
    private NEvent _unblockClientActivity;
    private NEvent _hashmapChanged;
    private NEvent _taskNotifListeners;

    public static boolean isCloudEdition;

    /**
     * @return the _deathDetectionEnabled
     */
    public boolean isDeathDetectionEnabled() {
        return _deathDetectionEnabled;
    }

    /**
     * @param deathDetectionEnabled the _deathDetectionEnabled to set
     */
    public void setDeathDetectionEnabled(boolean deathDetectionEnabled) {
        this._deathDetectionEnabled = deathDetectionEnabled;
    }

    /**
     * @return the _graceTime
     */
    public int getGraceTime() {
        return _graceTime;
    }

    /**
     * @param graceTime the _graceTime to set
     */
    public void setGraceTime(int graceTime) {
        this._graceTime = graceTime;
    }

    public boolean isClustered() {
        return _isClustered;
    }

    public static interface CacheStoppedEvent {

        void invoke(String cacheName);
    }

    public static interface CacheStartedEvent {

        void invoke(String cacheName);
    }
    public static CacheStoppedEvent OnCacheStopped;
    public static CacheStartedEvent OnCacheStarted;
    private NEvent _configurationModified;


    private java.util.ArrayList _connectedClients = new java.util.ArrayList();
    /**
     * Indicates wtherher a cache is InProc or not.
     */
    private boolean _inProc;
    public static int ServerPort;
    private static float s_clientsRequests = 0;
    private static float s_clientsBytesRecieved = 0;
    private static float s_clientsBytesSent = 0;
    private long _lockIdTicker = 0;

    public String _logFile;
    /**
     * Holds the cach type name.
     */
    private String _cacheType;
    private boolean _isClustered;
  
    private boolean _isPersistEnabled = false;
    private int _persistenceInterval = 5;
    private Object _shutdownMutex = new Object();
    private String _uniqueId;
    private String _blockserverIP;
    private long _blockinterval = 180;
    private java.util.Date _startShutDown = new java.util.Date(0);
    long startTime;
    private Latch _shutDownStatusLatch = new Latch(ShutDownStatus.NONE);
    public AsyncProcessor _asyncProcessor; // created seperately for async clear, add, insert and remove operations from client for graceful shutdown.

    public final long getBlockInterval() {
        long timeout = (_blockinterval * 1000) - (((Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l - startTime) * 1000);
        return (timeout / 1000);
    }

    public final void setBlockInterval(long value) {
        _blockinterval = value;
    }

    public final boolean getIsInProc() {
        return _inProc;
    }

    public final CacheRuntimeContext getContext() {
        return _context;
    }

    public boolean getIsPersistEnabled() {
        return _isPersistEnabled;
    }

    public void setIsPersistEnabled(boolean value) {
        _isPersistEnabled = value;
    }

    public int getPersistenceInterval() {
        return _persistenceInterval;
    }

    public void setPersistenceInterval(int value) {
        _persistenceInterval = value;
    }

  

    public IAlertPropagator EmailAlertPropagator;


    /**
     * Thread to reset Instantaneous Stats after every one second.
     */
    private Thread _instantStatsUpdateTimer;
    private boolean threadRunning = true;
    private TimeSpan InstantStatsUpdateInterval = new TimeSpan(0, 0, 1);

    private String _bridgeId;
    private static boolean s_logClientEvents;

    /**
     * Default constructor.
     */
    static {
        try {
            MiscUtil.RegisterCompactTypes();
        } catch (CacheArgumentException cacheArgumentException) {
        }
        String tmpStr = ServicePropValues.CacheServer_LogClientEvents;
        if (tmpStr != null && !tmpStr.equals("")) {
            s_logClientEvents = Boolean.parseBoolean(tmpStr);
        }
    }

    /**
     * Default constructor.
     */
    public Cache() {
        _context.setCacheRoot(this);
    }

    public final ILogger getCacheLog() {
        return _context.getCacheLog();
    }

    /**
     * Overlaoded constructor, used internally by Cache Manager.
     *
     * @param configString property string used to create cache.
     */
    protected Cache(String configString) throws ConfigurationException {
        _context.setCacheRoot(this);
        _cacheInfo = ConfigHelper.GetCacheInfo(configString);
    }

    /**
     * Overlaoded constructor, used internally by Cache Manager.
     *
     * @param configString property string used to create cache.
     */
    protected Cache(String configString, CacheRenderer renderer) throws ConfigurationException {
        _context.setCacheRoot(this);
        _cacheInfo = ConfigHelper.GetCacheInfo(configString);
        _context.setRender(renderer);
    }

    /**
     * Finalizer for the cache.
     */
    protected void finalize() throws Throwable {
        dispose(false);
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     * @param disposing
     */
    private void dispose(boolean disposing) {
        synchronized (this) {


            if (_context.getCacheImpl() != null) {
                try {
                    _context.getCacheImpl().StopServices();
                } catch (InterruptedException interruptedException) {
                    getCacheLog().Error("Cache.Dispose", "StopService " + interruptedException.getMessage());
                }
            }

            if (_context.getDsMgr() != null) {
                _context.getDsMgr().dispose();
                _context.setDsMgr(null);
            }
             if (_context.getEntryProcessorManager() != null) {
                _context.getEntryProcessorManager().dispose();
                _context.setEntryProcessorManager(null);
            }
            

            if (_context.getCSLMgr() != null) {
                _context.getCSLMgr().dispose();
                _context.setCSLMgr(null);
            }

       

            if (_asyncProcessor != null) {
                _asyncProcessor.Stop();
                _asyncProcessor = null;
            }

            if (_connectedClients != null) {
                synchronized (_connectedClients) {
                    _connectedClients.clear();
                    _context.PerfStatsColl.setClientConnectedStats(_connectedClients.size());

                }
            }

            ClearCallbacks();

            _cacheStopped = null;
            _cacheCleared = null;
            _taskNotifListeners = null;
            
            _itemAdded = null;
            _itemUpdated = null;
            _itemRemoved = null;
            _cusotmNotif = null;

            if (getCacheLog() != null) {
                getCacheLog().CriticalInfo("Cache.Dispose", "Cache stopped successfully");
                getCacheLog().Flush();
                getCacheLog().Close();
            }

            try {
                if (_channels != null) {
                }
            } catch (Exception e2) {
            }
            _channels = null;

            System.gc();

            if (disposing) {
            }

            if (_context != null) {
                _context.dispose();
            }
            //Dispose snaphot pool for this cache.
            if (disposing) {
                CacheSnapshotPool.getInstance().DisposePool(_context.getCacheRoot().getName());
            }
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    public void dispose() {
        try {
            dispose(true);

            if (EmailAlertPropagator != null) {
                EmailAlertPropagator.RaiseAlert(EventID.CacheStop, "TayzGrid", "\"" + getName() + "\"" + " stopped successfully.");
            }

        } catch (Exception exp) {
            if (EmailAlertPropagator != null) {
                EmailAlertPropagator.RaiseAlert(EventID.CacheStop, "TayzGrid", "\"" + getName() + "\"" + " cannot be stopped.");
            }
        }
    }

    /**
     * Name of the cache.
     */
    public final String getName() {
        return _cacheInfo.getName();
    }

    /**
     * Property string used to create cache.
     */
    public final String getConfigString() {
        return _cacheInfo.getConfigString();
    }

    public final void setConfigString(String value) {
        _cacheInfo.setConfigString(value);
    }


    public final void setBridgeId(String value) {
        _bridgeId = value;
    }

    public final String getBridgeId() {
        return _bridgeId;
    }

    /**
     * Returns true if the cache is running, false otherwise.
     */
    public final boolean getIsRunning() {
        return _context.getCacheImpl() != null;
    }

    public final boolean getIsCoordinator() {
        if (_context.getIsClusteredImpl()) {
            return ((ClusterCacheBase) _context.getCacheImpl()).getCluster().getIsCoordinator();
        } else {
            return false;
        }
    }

    /**
     * Get the running cache type name.
     */
    public final String getCacheType() {
        return _cacheType;
    }
    
    private static ExpirationContract getExpirationContract(com.alachisoft.tayzgrid.config.dom.CacheServerConfig config) throws ArgumentException
    {
        ExpirationContract contract = new ExpirationContract();
        if(config!=null)
        {
            ExpirationPolicy expiration = config.getExpirationPolicy();
            if(expiration != null)
            {
                contract = new ExpirationContract(expiration.getPolicyType(), expiration.getDuration(), expiration.getUnit());
            }
        }
        
        return contract;
    }

    public final boolean getSerializationEnabled()
    {
        if (!_inProc)
        {
            return true;
        }
        else
        {
            if (_context.getInMemoryDataFormat() == DataFormat.Binary)
            {
                return true;
            }
            else
            {
                if(isClustered())
                    return true;
                else
                    return false;
            }
        }

    }

    /**
     * returns the number of objects contained in the cache.
     */
    public final long getCount() throws GeneralFailureException, OperationFailedException, CacheException {
        // Cache has possibly expired so return default.
        if (!getIsRunning()) {
            return 0;
        }
        return _context.getCacheImpl().getCount();
    }

    public final String getTargetCacheUniqueID() {
        if (this._context.getCacheImpl() instanceof ClusterCacheBase) {
            return ((ClusterCacheBase) this._context.getCacheImpl()).getBridgeSourceCacheId();
        } else {
            return "";
        }
    }

    /**
     * returns the statistics of the Clustered Cache.
     *
     * @return
     */
    public final CacheStatistics getStatistics() {
        // Cache has possibly expired so return default.
        if (!getIsRunning()) {
            return new CacheStatistics("", _cacheInfo.getClassName());
        }
        return _context.getCacheImpl().getStatistics();
    }

    public final java.util.ArrayList<CacheNodeStatistics> GetCacheNodeStatistics() throws GeneralFailureException, OperationFailedException, CacheException {
        java.util.ArrayList<CacheNodeStatistics> statistics = null;
        if (!getIsRunning()) {
            statistics = new java.util.ArrayList<CacheNodeStatistics>();
            CacheNodeStatistics nodeStats = new CacheNodeStatistics(null);
            nodeStats.setStatus(CacheNodeStatus.Stopped);
            statistics.add(nodeStats);
        } else {
            statistics = _context.getCacheImpl().GetCacheNodeStatistics();
            if (statistics != null && statistics.size() > 0) {
                statistics.get(0).setClientCount((short) _connectedClients.size());
            }
        }
        return statistics;
    }

    public final void GetLeastLoadedServer(tangible.RefObject<String> ipAddress, tangible.RefObject<Integer> serverPort) {
        String connectedIpAddress = ipAddress.argvalue;
        int connectedPort = serverPort.argvalue;

        if (!this._context.getIsClusteredImpl()) {
            return;
        }
        if (_inProc) {
            return;
        }

        java.util.List nodes = ((ClusterCacheBase) this._context.getCacheImpl())._stats.getNodes();

        NodeInfo localNodeInfo = null;
        int min = Integer.MAX_VALUE;
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            NodeInfo i = (NodeInfo) it.next();
            if (i.getIsStartedAsMirror()) {
                continue;
            }

            if (!_context.getCacheImpl().IsShutdownServer(i.getAddress())) {
                if (i.getConnectedClients().size() < min) {
                    if (i.getRendererAddress() != null) {
                        ipAddress.argvalue = (String) i.getRendererAddress().getIpAddress().getHostAddress();
                        serverPort.argvalue = i.getRendererAddress().getPort();
                    } else {
                        ipAddress.argvalue = (String) i.getAddress().getIpAddress().getHostAddress();
                    }

                    min = i.getConnectedClients().size();
                }
            }
            if (i.getAddress().getIpAddress().getHostAddress().equals(connectedIpAddress)) {
                localNodeInfo = i;
            }
        }

        /**
         * we don't need to reconnect the the selected server if the selected
         * server has same clients as the server with which the client is
         * currently connected.
         */
        if (localNodeInfo != null && localNodeInfo.getConnectedClients().size() == min) {
            ipAddress.argvalue = connectedIpAddress;
            serverPort.argvalue = connectedPort;
        }
    }

    public java.util.HashMap<String, Integer> GetRunningServers(String ipAddress, Integer serverPort) {
        java.util.HashMap<String, Integer> runningServers = new java.util.HashMap<String, Integer>();
        if (this.getCacheType().equals("replicated-server")) {
            String connectedIpAddress = ipAddress;
            int connectedPort = serverPort;
          
                java.util.List nodes = ((ClusterCacheBase) this._context.getCacheImpl())._stats.getNodes();

                for (Iterator it = nodes.iterator(); it.hasNext();) {
                    NodeInfo i = (NodeInfo) it.next();
                    if (!_context.getCacheImpl().IsShutdownServer(i.getAddress())) {
                        if (i.getRendererAddress() != null) {
                            ipAddress = (String) i.getRendererAddress().getIpAddress().getHostAddress().toString();
                            serverPort = i.getRendererAddress().getPort();
                            runningServers.put(ipAddress, serverPort);
                        }
                    }
                }
            
        }
        return runningServers;
    }



    /**
     * Gets or sets the cache item at the specified key.
     */
    public final Object getItem(Object key) throws OperationFailedException {
        Object lockId = null;
        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(utc);
        java.util.Date lockDate = cal.getTime();
        long version = 0;
        tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
        tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
        tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
        Object tempVar = GetGroup(key, new BitSet(), null, null, tempRef_version, tempRef_lockId, tempRef_lockDate, TimeSpan.ZERO, LockAccessType.IGNORE_LOCK, "", new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation)).Value;
        version = tempRef_version.argvalue;
        lockId = tempRef_lockId.argvalue;
        lockDate = tempRef_lockDate.argvalue;
        return tempVar;
    }

    /**
     * delegate for item addition notifications.
     */
    public void addItemAddedListner(NEventStart start, NEventEnd end) {
        _itemAdded.addNEventListnersSync(start, end);
    }

    public void removeItemAddListner(NEventStart start) {
        _itemAdded.removeNEventListnersSync(start);
    }

    public void addAsyncOperationCallbackListner(NEventStart start, NEventEnd end) {
        _asyncOperationCompleted.addNEventListners(start, end);
    }

    public void removeAsyncOperationCallbackListner(NEventStart start) {
        _asyncOperationCompleted.removeNEventListners(start);
    }

    public void addItemUpdatedListner(NEventStart start, NEventEnd end) {
        _itemUpdated.addNEventListnersSync(start, end);
    }

    public void removeItemUpdatedListner(NEventStart start) {
        _itemUpdated.removeNEventListnersSync(start);
    }

    public void addItemRemovedListner(NEventStart start, NEventEnd end) {
        _itemRemoved.addNEventListnersSync(start, end);
    }

    public void removeItemRemovedListner(NEventStart start) {
        _itemRemoved.removeNEventListnersSync(start);
    }

    public void addCacheClearedListner(NEventStart start, NEventEnd end) {
        _cacheCleared.addNEventListnersSync(start, end);
    }

    public void removeCacheClearedListner(NEventStart start) {
        _cacheCleared.removeNEventListnersSync(start);
    }

    public void addCustomNotifListner(NEventStart start, NEventEnd end) {
        _cusotmNotif.addNEventListners(start, end);
    }

    public void removeCustomNotifListner(NEventStart start) {
        _cusotmNotif.removeNEventListners(start);
    }

    public void addDsUpdatedListner(NEventStart start, NEventEnd end) {
        _dataSourceUpdated.addNEventListners(start, end);
    }

    public void removeDsUpdatedListner(NEventStart start) {
        _dataSourceUpdated.removeNEventListners(start);
    }

    public void addNodeJoinedListner(NEventStart start, NEventEnd end) {
        _memberJoined.addNEventListners(start, end);
    }

    public void removeNodeJoinedListner(NEventStart start) {
        _memberJoined.removeNEventListners(start);
    }

    public void addNodeLeftListner(NEventStart start, NEventEnd end) {
        _memberLeft.addNEventListners(start, end);
    }

    public void removeNodeLeftListner(NEventStart start) {
        _memberLeft.removeNEventListners(start);
    }

    public void addCacheStoppedListner(NEventStart start, NEventEnd end) {
        if (_cacheStopped != null) {
            _cacheStopped.addNEventListners(start, end);
        }
    }

    public void removeCacheStoppedListner(NEventStart start) {
        _cacheStopped.removeNEventListners(start);
    }

    public void addHashmapChangedListner(NEventStart start, NEventEnd end) {
        _hashmapChanged.addNEventListners(start, end);
    }

    public void removeHashmapChangedListner(NEventStart start) {
        _hashmapChanged.removeNEventListners(start);
    }

    public void addBlockActivity(NEventStart start, NEventEnd end) {
        _blockClientActivity.addNEventListners(start, end);
    }

    public void removeBlockActivity(NEventStart start) {
        _blockClientActivity.removeNEventListners(start);
    }

    public void addUnBlockActivity(NEventStart start, NEventEnd end) {
        _unblockClientActivity.addNEventListners(start, end);
    }

    public void removeUnBlockActivity(NEventStart start) {
        _unblockClientActivity.removeNEventListners(start);
    }

    public void addTaskNotifListener(NEventStart start, NEventEnd end)
    {
        _taskNotifListeners.addNEventListners(start, end);
    }
    
    public void removeTaskNotifListener(NEventStart start)
    {
        _taskNotifListeners.removeNEventListners(start);
    }
    
    public void addCustomUpdateNotifListner(NEventStart start, NEventEnd end) {
        _customUpdateNotif.addNEventListners(start, end);
    }

    public void removeCustomUpdateNotifListner(NEventStart start) {
        _customUpdateNotif.removeNEventListners(start);
    }

    public void addCustomRemoveNotifListner(NEventStart start, NEventEnd end) {
        _customRemoveNotif.addNEventListners(start, end);
    }

    public void removeCustomRemoveNotifListner(NEventStart start) {
        _customRemoveNotif.removeNEventListners(start);
    }

    /**
     * Starts the cache functionality.
     *
     * @param renderer It provides the events like onCientConnected etc from the
     * SocketServer. Also the portSocketServer is listenting for client
     * connections.
     * @param userId userId
     * @param password password
     */
    protected void Start(CacheRenderer renderer, String userId, String password, boolean twoPhaseInitialization) throws Exception {
        Start(renderer, userId, password, false, twoPhaseInitialization);
    }

    public void StartPhase2() throws ChannelClosedException, ChannelException {
        if (_context.getCacheImpl() instanceof ClusterCacheBase) {
            ((ClusterCacheBase) _context.getCacheImpl()).InitializePhase2();
        }
    }

    /**
     * Start the cache functionality.
     */
    protected void Start(CacheRenderer renderer, String userId, String password, boolean isStartingAsMirror, boolean twoPhaseInitialization) throws Exception {
        try {
            if (getIsRunning()) {
                Stop(false);
            }
            ConfigReader propReader = new PropsConfigReader(getConfigString());
            _context.setRender(renderer);
            if (renderer != null) {

                renderer.addClientConnectedListner(new NEventStart() {
                    @Override
                    public Object hanleEvent(Object... obj) throws SocketException, OperationFailedException {
                        OnClientConnected((String) obj[0], (String) obj[0]);
                        return null;
                    }
                }, null);

                renderer.addClientDisconnectedListner(new NEventStart() {
                    @Override
                    public Object hanleEvent(Object... obj) throws SocketException, OperationFailedException {
                        OnClientDisconnected((String) obj[0], (String) obj[0]);
                        return null;
                    }
                }, null);

            }

            Initialize(propReader.getProperties(), false, userId, password, isStartingAsMirror, twoPhaseInitialization);
            if (_context != null && _context.PerfStatsColl != null) {
                _context.PerfStatsColl.nodeStatus(CacheNodeStatus.Running.getValue());
            }
            if (EmailAlertPropagator != null) {
                EmailAlertPropagator.RaiseAlert(EventID.CacheStart, "TayzGrid", "\"" + getName() + "\"" + " started successfully.");
            }
        } catch (Exception exception) {
            if (EmailAlertPropagator != null) {
                EmailAlertPropagator.RaiseAlert(EventID.CacheStart, "TayzGrid", "\"" + getName() + "\"" + " cannot be started.");
            }
            throw exception;
        }
    }

    /**
     * Fired when a client is connected with the socket server.
     *
     * @param client
     * @param cacheId
     */
    public final void OnClientDisconnected(String client, String cacheId) throws OperationFailedException {
        if (_context.getCacheImpl() != null) {
           synchronized (_connectedClients) {
                _connectedClients.remove(client);
                _context.PerfStatsColl.setClientConnectedStats(_connectedClients.size());
            }
            _context.getCacheImpl().ClientDisconnected(client, _inProc);

            if (s_logClientEvents) {
                EventLogger.LogEvent("TayzGrid", "Client \"" + client + "\" has disconnected from " + _cacheInfo.getName(), EventType.INFORMATION, EventCategories.Information, EventID.ClientDisconnected);
            }
            _context.getCacheLog().CriticalInfo("Cache.OnClientDisconnected", "Client \"" + client + "\" has disconnected from cache");
        }
    }

    public final void OnClientConnected(String client, String cacheId) throws OperationFailedException {
        if (_context.getCacheImpl() != null) {
            if (!_connectedClients.contains(client)) {
                synchronized (_connectedClients) {
                    _connectedClients.add(client);
                    _context.PerfStatsColl.setClientConnectedStats(_connectedClients.size());
                }
            }

            _context.getCacheImpl().ClientConnected(client, _inProc);

            EventLogger.LogEvent("TayzGrid", "Client \"" + client + "\" has connected to " + _cacheInfo.getName(), EventType.INFORMATION, EventCategories.Information, EventID.ClientConnected);
            _context.getCacheLog().CriticalInfo("Cache.OnClientConnected", "Client \"" + client + "\" has connected to cache");
        }
    }

    /**
     * Stop the internal working of the cache.
     */
    public void Stop(boolean isGracefullShutdown) throws Exception {
        _startShutDown = new java.util.Date();
        startTime = (Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l;

        if (isGracefullShutdown) {
            _shutDownStatusLatch.SetStatusBit(ShutDownStatus.SHUTDOWN_INPROGRESS, ShutDownStatus.NONE);

            int shutdownTimeout = 180;
            int blockTimeout = 3;

            tangible.RefObject<Integer> tempRef_shutdownTimeout = new tangible.RefObject<Integer>(shutdownTimeout);
            tangible.RefObject<Integer> tempRef_blockTimeout = new tangible.RefObject<Integer>(blockTimeout);
            String expMsg = GracefulTimeout.GetGracefulShutDownTimeout(tempRef_shutdownTimeout, tempRef_blockTimeout);
            shutdownTimeout = tempRef_shutdownTimeout.argvalue;
            blockTimeout = tempRef_blockTimeout.argvalue;
            if (expMsg != null) {
                _context.getCacheLog().CriticalInfo("Cache.GracefulShutDown", expMsg);
            }

            _context.getCacheLog().CriticalInfo("Cache.Stop", "Graceful Shutdown Timeout: " + shutdownTimeout);

            _context.getCacheLog().CriticalInfo("Cache.Stop", "Starting Graceful Shutdown Processor...");

            synchronized (_shutdownMutex) {
                Thread _gracefulStopThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ShutDownGraceful();
                    }
                });
                _gracefulStopThread.setName(_cacheInfo.getName() + ".GraceFullShutDownProcessor");
                _gracefulStopThread.setDaemon(true);
                _gracefulStopThread.start();
                try {
                    com.alachisoft.tayzgrid.common.threading.Monitor.wait(_shutdownMutex, (shutdownTimeout + 1) * 1000);
                } catch (Exception ex) {
                    _context.getCacheLog().Error("Cache.Stop", "Graceful Shutdown Process has intruppted. " + ex.getMessage());
                }
            }
            _context.getCacheLog().CriticalInfo("Cache.Stop", "Graceful Shutdown Process has ended.");
        }
        dispose();
    }

    public boolean VerifyNodeShutDown(boolean isGraceful) {
        if (isGraceful) {
            java.util.ArrayList<ShutDownServerInfo> servers = GetShutDownServers();

            if (servers != null) {
                return false;
            }
        }

        return true;
    }

    public final java.util.ArrayList<ShutDownServerInfo> GetShutDownServers() {
        return _context.getCacheImpl().GetShutDownServers();
    }

    public final void NotifyBlockActivityToClients(String uniqueId, String server, long interval, int port) {
        _uniqueId = uniqueId;
        _blockserverIP = server;
        _blockinterval = interval;

        if (_shutDownStatusLatch.IsAnyBitsSet((byte) (ShutDownStatus.NONE | ShutDownStatus.SHUTDOWN_COMPLETED))) {
            _shutDownStatusLatch.SetStatusBit(ShutDownStatus.SHUTDOWN_INPROGRESS, (byte) (ShutDownStatus.NONE | ShutDownStatus.SHUTDOWN_COMPLETED));
        }

        if (this._blockClientActivity == null) {
            return;
        }

        _context.getCacheLog().CriticalInfo("Cache.NotifyBlockActivityToClients", "Notifying " + _blockClientActivity.getCount() + " clients to block activity.");

        _blockClientActivity.fireEvents(true, new Object[]{
            uniqueId,
            server,
            interval,
            port
        });
    }

    public final void NotifyUnBlockActivityToClients(String uniqueId, String server, int port) {
        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            _shutDownStatusLatch.SetStatusBit(ShutDownStatus.SHUTDOWN_COMPLETED, ShutDownStatus.SHUTDOWN_INPROGRESS);
        }

        if (this._unblockClientActivity == null) {
            return;
        }

        _context.getCacheLog().CriticalInfo("Cache.NotifyUnBlockActivityToClients", "Notifying " + _unblockClientActivity.getCount() + " clients to unblock activity.");

        _unblockClientActivity.fireEvents(true, new Object[]{
            uniqueId,
            server,
            port
        });

        _uniqueId = null;
        _blockserverIP = null;
        _blockinterval = 0;
    }

    /**
     * shutdown function.
     */
    public final void ShutDownGraceful() {
        int shutdownTimeout = 180;
        int blockTimeout = 3;

        tangible.RefObject<Integer> tempRef_shutdownTimeout = new tangible.RefObject<Integer>(shutdownTimeout);
        tangible.RefObject<Integer> tempRef_blockTimeout = new tangible.RefObject<Integer>(blockTimeout);
        String expMsg = GracefulTimeout.GetGracefulShutDownTimeout(tempRef_shutdownTimeout, tempRef_blockTimeout);
        shutdownTimeout = tempRef_shutdownTimeout.argvalue;
        blockTimeout = tempRef_blockTimeout.argvalue;
        if (expMsg != null) {
            _context.getCacheLog().CriticalInfo("Cache.GracefulShutDown", expMsg);
        }

        setBlockInterval(shutdownTimeout);

        try {
            _context.getCacheLog().CriticalInfo("Cache.ShutDownGraceful", "Waiting for " + blockTimeout + "seconds...");
            //Wait for activity to get completed...

            String uniqueID = new com.alachisoft.tayzgrid.caching.util.GUID().toString();//Guid.NewGuid().toString();

            _context.getCacheLog().CriticalInfo("Cache.ShutDownGraceful", "Notifying Cluster and Clients to block activity.");
            _context.getCacheImpl().NotifyBlockActivity(uniqueID, getBlockInterval());

            _context.getCacheLog().CriticalInfo("Cache.ShutDownGraceful", "Waiting for Background process completion.");
            Thread.sleep(blockTimeout * 1000); //wait before activity blocking.

            _context.getCacheLog().CriticalInfo("Cache.ShutDownGraceful", "Starting Windup Tasks.");

            if (_asyncProcessor != null) {
                _asyncProcessor.WindUpTask();
            }

          

            if (_context.getDsMgr() != null && _context.getDsMgr().getIsWriteBehindEnabled() && _context.getDsMgr()._writeBehindAsyncProcess != null) {
                _context.getDsMgr().WindUpTask();
            }

            _context.getCacheImpl().WindUpReplicatorTask();

            _context.getCacheLog().CriticalInfo("Cache.ShutDownGraceful", "Windup Tasks Ended.");

            if (_context.getCSLMgr() != null && !_context.getCSLMgr().getIsCacheLoaderTaskCompleted() && _context.getCSLMgr().getIsCacheloaderEnabled()) {
                _context.getCacheImpl().NotifyCacheLoaderExecution();
            }

            if (_asyncProcessor != null) {
                if (getBlockInterval() > 0) {
                    _asyncProcessor.WaitForShutDown(getBlockInterval());
                }
            }

          

            if (_context.getDsMgr() != null && _context.getDsMgr().getIsWriteBehindEnabled() && _context.getDsMgr()._writeBehindAsyncProcess != null) {
                if (getBlockInterval() > 0) {
                    _context.getDsMgr().WaitForShutDown(getBlockInterval());
                }
            }

            if (getBlockInterval() > 0) {
                _context.getCacheImpl().WaitForReplicatorTask(getBlockInterval());
            }

            _context.getCacheImpl().NotifyUnBlockActivity(uniqueID);

        } catch (InterruptedException ti) {
            _context.getCacheLog().Error("Cache.ShutdownGraceful", "Graceful Shutdown have stopped. " + ti.toString());
        } catch (Exception e) {
            _context.getCacheLog().Error("Cache.GracefulShutDown", "Graceful Shutdown have stopped. " + e.toString());
        } finally {
            synchronized (_shutdownMutex) {
                com.alachisoft.tayzgrid.common.threading.Monitor.pulse(_shutdownMutex);
            }
        }
    }

    public final boolean AcceptClient(InetAddress clientAddress) {
        if (_context.getCacheImpl() != null) {
            return _context.getCacheImpl().AcceptClient(clientAddress);
        }
        return false;
    }

    public final void DisconnectClient(InetAddress clientAddress) {
        if (_context.getCacheImpl() != null) {
            _context.getCacheImpl().DisconnectClient(clientAddress);
        }
    }

    public final String GetDefaultReadThruProvider() {
        String defaultProvider = null;
        if(_cacheInfo.getConfiguration().getCacheLoaderFactory()!=null)
            return _cacheInfo.getConfiguration().getCacheLoaderFactory().toString().toLowerCase();
        com.alachisoft.tayzgrid.config.newdom.Provider[] providers = _cacheInfo.getConfiguration().getBackingSource().getReadthru().getProviders();
        for (com.alachisoft.tayzgrid.config.newdom.Provider pro : providers) {
            if (pro.getIsDefaultProvider()) {
                defaultProvider = pro.getProviderName();
            }
        }
        return defaultProvider;
    }

    public final String GetDefaultWriteThruProvider() {
        String defaultProvider = null;
        if(_cacheInfo.getConfiguration().getCacheWriterFactory()!=null)
            return _cacheInfo.getConfiguration().getCacheWriterFactory().toString().toLowerCase();
        com.alachisoft.tayzgrid.config.newdom.Provider[] providers = _cacheInfo.getConfiguration().getBackingSource().getWritethru().getProviders();
        for (com.alachisoft.tayzgrid.config.newdom.Provider pro : providers) {
            if (pro.getIsDefaultProvider()) {
                defaultProvider = pro.getProviderName();
            }
        }
        return defaultProvider;
    }

    public final void Initialize(java.util.Map properties, boolean inProc, String userId, String password) throws ConfigurationException {
        Initialize(properties, inProc, userId, password, false, false);
    }

    public final void Initialize(java.util.Map properties, boolean inProc, String userId, String password, boolean isStartingAsMirror, boolean twoPhaseInitialization) throws
            ConfigurationException {
        _itemAdded = new NEvent("Cache.OnItemAdded()", _context.getCacheLog());
        _itemUpdated = new NEvent("Cache.OnItemUpdated()", _context.getCacheLog());
        _itemRemoved = new NEvent("Cache.OnItemUpdated()", _context.getCacheLog());
        _cacheCleared = new NEvent("Cache.OnCacheCleared()", _context.getCacheLog());
        _cusotmNotif = new NEvent("Cache.OnCustomEvent()", _context.getCacheLog());

        _asyncOperationCompleted = new NEvent("Cache.OnAsyncOperationCompletedCallback()", _context.getCacheLog());
        _dataSourceUpdated = new NEvent("Cache.OnWriteBehindOperationCompletedCallback", _context.getCacheLog());
        _cacheStopped = new NEvent("Cache.Dispose", _context.getCacheLog());
        //Bridge not in POC
        _cacheBecomeActive = new NEvent("Cache.CacheBecomeActive", _context.getCacheLog());
        _memberJoined = new NEvent("Cache.OnMemberJoined()", _context.getCacheLog());
        _memberLeft = new NEvent("Cache.OnMemberLeft()", _context.getCacheLog());
        _configurationModified = new NEvent("configurationModified", _context.getCacheLog());
        _hashmapChanged = new NEvent("Cache.OnHashmapChanged", _context.getCacheLog());
        _taskNotifListeners = new NEvent("Cache.OnTaskNotificationCallback()", _context.getCacheLog());
        _customRemoveNotif = new NEvent("Cache.OnCustomRemoveCallback()", _context.getCacheLog());
        _customUpdateNotif = new NEvent("Cache.OnCustomUpdateCallback()", _context.getCacheLog());
        _blockClientActivity = new NEvent("Cache.BlockClientActivity", _context.getCacheLog());
        _unblockClientActivity = new NEvent("Cache.UnBlockClientActivity", _context.getCacheLog());

        //Just to initialze the HP time
        HPTime time = HPTime.getNow();
        for (int i = 1; i < 1000; i++) {
            time = HPTime.getNow();
        }
        _inProc = inProc;
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            synchronized (this) {
                if (!properties.containsKey("cache")) {
                    throw new ConfigurationException("Missing configuration attribute 'cache'");
                }

                java.util.Map cacheConfig = (java.util.Map) properties.get("cache");
                if(cacheConfig.get("data-format")!= null)
                    _context.setInMemoryDataFormat(((String)cacheConfig.get("data-format")).equalsIgnoreCase("binary")?DataFormat.Binary:DataFormat.Object);
              

                    if (cacheConfig.containsKey("alerts")) {
                        java.util.Map alertConfig = (java.util.Map) cacheConfig.get("alerts");
                        if (alertConfig.containsKey("alerts-types")) {

                            _context.setCacheAlertTypes(com.alachisoft.tayzgrid.caching.util.AlertTypeHelper.Initialize((java.util.Map) ((alertConfig.get("alerts-types") instanceof java.util.Map) ? alertConfig.get("alerts-types") : null)));
                        } else {
                            _context.setCacheAlertTypes(new AlertNotificationTypes());
                        }

                        if (alertConfig.containsKey("email-notification")) {

                            try {
                                _context.setEmailAlertNotifier(new EmailAlertNotifier());
                            } catch (Exception x) {

                            }
                            EmailNotifierArgs emailNotifierArgs = new EmailNotifierArgs((java.util.Map) ((alertConfig.get("email-notification") instanceof java.util.Map) ? alertConfig.get("email-notification") : null), _context);
                            _context.getEmailAlertNotifier().Initialize(emailNotifierArgs, _context.getCacheAlertTypes());
                        }
                    }
                    EmailAlertPropagator = _context.getEmailAlertNotifier();



                
              
                if (cacheConfig.containsKey("name")) {
                    _cacheInfo.setName(String.valueOf(cacheConfig.get("name")).trim());
                }

                _cacheInfo.setCurrentPartitionId(GetCurrentPartitionId(_cacheInfo.getName(), cacheConfig));

                if (cacheConfig.containsKey("log")) {
                    _context.setCacheLog(new JLogger());
                    _context.getCacheLog().Initialize((java.util.Map) ((cacheConfig.get("log") instanceof java.util.Map) ? cacheConfig.get("log") : null), _cacheInfo.getCurrentPartitionId(), _cacheInfo.getName(), isStartingAsMirror, inProc);

                } else {
                    _context.setCacheLog(new JLogger());
                    _context.getCacheLog().Initialize(null, _cacheInfo.getCurrentPartitionId(), _cacheInfo.getName());
                }

                
                //ClientDeath Dectection
                if (Boolean.parseBoolean(ServicePropValues.Enable_Client_Death_Detection))
                    {
                        setDeathDetectionEnabled(Boolean.parseBoolean(ServicePropValues.Enable_Client_Death_Detection));
                        setGraceTime(Integer.parseInt(ServicePropValues.Client_Death_Detection_Grace_Period));
                        if (isDeathDetectionEnabled())
                            _context.ClientDeathDetection = new ClientDeathDetectionMgr(getGraceTime());
                    }
                

                _context.setSerializationContext(_cacheInfo.getName());
                _context.TimeSched = new TimeScheduler();

                _context.AsyncProc = new AsyncProcessor(_context.getCacheLog());
                _asyncProcessor = new AsyncProcessor(_context.getCacheLog());
            
                _context.ExpirationContract = getExpirationContract(_cacheInfo.getConfiguration());//getExpirationContract(cacheConfig);
//                if (!inProc) {
//                    if (!_cacheInfo.getCurrentPartitionId().equals("")) {
//                        _context.PerfStatsColl = new PerfStatsCollector(getName() + "-" + _cacheInfo.getCurrentPartitionId(), inProc);
//                    } else {
//                        _context.PerfStatsColl = new PerfStatsCollector(getName(), inProc);
//                    }
//                } else 
                {
                    if (isStartingAsMirror) { // this is a special case for Partitioned Mirror Topology.
                        _context.PerfStatsColl = new PerfStatsCollector(getName() + "-" + "replica", false);
                    } else {
                        _context.PerfStatsColl = new PerfStatsCollector(getName(), inProc);
                    }
                    _context.PerfStatsColl.setCacheLog(_context.getCacheLog());
                }

                _context.setIsStartedAsMirror(isStartingAsMirror);
                _context.PerfStatsColl.setIsStoreByValue(_context.getInMemoryDataFormat()==DataFormat.Binary?false:true);
                
                    if (cacheConfig.containsKey("backing-source")) {
                        try {
                            long reminderTimeout = 10000;
                            java.util.Map cacheClass = (java.util.Map) cacheConfig.get("cache-classes");
                            if (cacheClass != null) {
                                cacheClass = (java.util.Map) cacheClass.get(_cacheInfo.getName().toLowerCase());
                                if (cacheClass != null) {
                                    if (cacheClass.containsKey("op-timeout")) {
                                        reminderTimeout = Long.parseLong(cacheClass.get("op-timeout").toString());
                                        if (reminderTimeout < 100) {
                                            reminderTimeout = 100;
                                        }
                                        if (reminderTimeout > 30000) {
                                            reminderTimeout = 30000;
                                        }
                                    }
                                }
                            }

                            java.util.Map dsConfig = (java.util.Map) cacheConfig.get("backing-source");

                            _context.setDsMgr(new DatasourceMgr(this.getName(), dsConfig, _context, reminderTimeout));
                            if (_context.getDsMgr()._dsUpdateProcessor != null) {
                                _context.getDsMgr()._dsUpdateProcessor.Start();
                            }
                            if (_context.getDsMgr().getIsReadThruEnabled()) {
                                _context.getDsMgr().setDefaultReadThruProvider(GetDefaultReadThruProvider());
                            }
                            if (_context.getDsMgr().getIsWriteThruEnabled()) {
                                _context.getDsMgr().setDefaultWriteThruProvider(GetDefaultWriteThruProvider());
                            }
                        } catch (Exception e) {
                            if (e instanceof ConfigurationException) {
                                _context.getCacheLog().Error("Cache.Initialize()", e.toString());
                                String msg = String.format("Datasource provider (ReadThru/WriteThru) could not be initialized because of the following error: %1$s", e.getMessage());

                                EventLogger.LogEvent(msg, EventType.WARNING);
                                throw new Exception(msg);
                            } else {
                                _context.getCacheLog().Error("Cache.Initialize()", e.toString());
                                String msg = String.format("Failed to initialize datasource sync. read-through/write-through will not be available, Error %1$s", e.getMessage());

                                EventLogger.LogEvent(msg, EventType.WARNING);
                                throw new Exception(msg, e);
                            }
                        }
                    }

                  

                    if (cacheConfig.containsKey("cache-loader")) {
                        try {
                            java.util.Map cslConfig = (java.util.Map) cacheConfig.get("cache-loader");
                            _context.setCSLMgr(new CacheStartupLoader(cslConfig, this, _context.getCacheLog()));
                        } catch (Exception e) {
                            _context.getCacheLog().Error("Cache.Initialize()", e.toString());
                            String msg = String.format("Failed to initialize cache startup loader, Error %1$s", e.toString());

                            EventLogger.LogEvent(msg, EventType.WARNING);
                            //throw e;
                        }


                    }
             



                _cacheInfo.setConfigString(ConfigHelper.CreatePropertyString(properties));

                CreateInternalCache(cacheConfig, userId, password, isStartingAsMirror, twoPhaseInitialization);
               
                //setting cache Impl instance
                if (_context.getCacheImpl() != null) {
                    if (_context.getDsMgr() != null) {
                        _context.getDsMgr().setCacheImpl(_context.getCacheImpl());
                    }
                    if (_context.ClientDeathDetection != null) {
                        _context.ClientDeathDetection.setCacheImpl(_context.getCacheImpl());
                    }
                }

                if (!_context.getIsClusteredImpl())
                {
                    if(_context.getDsMgr() != null)
                    {
                        _context.getDsMgr().StartWriteBehindProcessor();
                    }
                    if (_context.ClientDeathDetection != null)
                    {
                        _context.ClientDeathDetection.StartMonitoringClients();
                    }
                }

                // we bother about perf stats only if the user has read/write rights over counters.
                _context.PerfStatsColl.setCountStats(CacheHelper.GetLocalCount(_context.getCacheImpl()));
                _context.PerfStatsColl.setCacheSizeStats(CacheHelper.getCacheSize(_context.getCacheImpl()));

                
                if (!(_context.getCacheImpl() instanceof ClusterCacheBase)) {

                    if (_context.getCSLMgr() != null && _context.getCSLMgr().getIsCacheloaderEnabled()) {
                        try {
                            LoadCacheTask loadCacheThread = new LoadCacheTask(_context.getCSLMgr());
                            _context.getCSLMgr().setTask(loadCacheThread);
                            Thread t = new Thread(loadCacheThread);
                            t.start();
                        } catch (Exception e) {
                        }
                    }
                }
                
                _context.setEntryProcessorManager(new EntryProcessorManager(this.getCacheType(),_context,this));
            }
             _context.getCacheLog().CriticalInfo("TCPPorts", "jmxPort: "+Monitor.getJmxPort());
            _context.getCacheLog().CriticalInfo("Cache '" + _context.getCacheRoot().getName() + "' started successfully.");
        
            this._context.PerfStatsColl.setCacheMaxSizeStats(this._context.getCacheImpl().getMaxSize());
            
        } catch (ConfigurationException e) {
            dispose();
            throw e;
        } catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.getMessage(), e);
        }
    }

    public final void Initialize2(java.util.Map properties, boolean inProc, String userId, String password) throws ConfigurationException {
        //Just to initialze the HP time
        HPTime time = HPTime.getNow();
        for (int i = 1; i < 1000; i++) {
            time = HPTime.getNow();
        }
        _inProc = inProc;
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            synchronized (this) {
                if (properties.containsKey("name")) {
                    _cacheInfo.setName(String.valueOf(properties.get("name")).trim());
                }

                if (properties.containsKey("log")) {
                    _context.setCacheLog(new JLogger());
                    _context.getCacheLog().Initialize((java.util.Map) ((properties.get("log") instanceof java.util.Map) ? properties.get("log") : null), _cacheInfo.getCurrentPartitionId(), _cacheInfo.getName());

                } else {
                    _context.setCacheLog(new JLogger());
                    _context.getCacheLog().Initialize(null, _cacheInfo.getCurrentPartitionId(), _cacheInfo.getName());

                }
                _context.setSerializationContext(_cacheInfo.getName());
                _context.TimeSched = new TimeScheduler();

                _context.AsyncProc = new AsyncProcessor(_context.getCacheLog());
                _asyncProcessor = new AsyncProcessor(_context.getCacheLog());

                CreateInternalCache2(properties, userId, password);

                // we bother about perf stats only if the user has read/write rights over counters.
                _context.PerfStatsColl.setCountStats(CacheHelper.GetLocalCount(_context.getCacheImpl()));
                _context.PerfStatsColl.setCacheSizeStats(CacheHelper.getCacheSize(_context.getCacheImpl()));
            
 
                //_context.CacheStatsColl = new CacheStatsCollector(_context);
                _cacheInfo.setConfigString(ConfigHelper.CreatePropertyString(properties));
            }
        } catch (ConfigurationException e) {
            dispose();
            throw e;
        } catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.getMessage(), e);
        }
    }

    private String GetCurrentPartitionId(String cacheId, java.util.Map config) {
        cacheId = cacheId.toLowerCase();
        if (config.containsKey("cache-classes")) {
            config = (HashMap) ((config.get("cache-classes") instanceof HashMap) ? config.get("cache-classes") : null);
            if (config.containsKey(cacheId)) {
                config = (HashMap) ((config.get(cacheId) instanceof HashMap) ? config.get(cacheId) : null);
                if (config.containsKey("type")) {
                    String type = (String) ((config.get("type") instanceof String) ? config.get("type") : null);
                    if (config.containsKey("cluster")) {
                        config = (HashMap) ((config.get("cluster") instanceof HashMap) ? config.get("cluster") : null);
                        if (config.containsKey("sub-group-id")) {
                            return (String) ((config.get("sub-group-id") instanceof String) ? config.get("sub-group-id") : null);
                        }
                    }
                }
            }
        }
        return "";
    }

    private void CreateInternalCache(java.util.Map properties, String userId, String password, boolean isStartingAsMirror, boolean twoPhaseInitialization) throws
            ConfigurationException, Exception {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            if (!properties.containsKey("class")) {
                throw new ConfigurationException("Missing configuration attribute 'class'");
            }
            String cacheScheme = String.valueOf(properties.get("class"));
            
            if (!properties.containsKey("client-port")) {
                throw new ConfigurationException("Missing configuration attribute 'client-port'");
            }
            int cPort = (Integer.parseInt(properties.get("client-port").toString()));
            
            if (!properties.containsKey("cache-classes")) {
                throw new ConfigurationException("Missing configuration section 'cache-classes'");
            }
            java.util.Map cacheClasses = (java.util.Map) properties.get("cache-classes");

            if (!cacheClasses.containsKey(cacheScheme.toLowerCase())) {
                throw new ConfigurationException("Can not find cache class '" + cacheScheme + "'");
            }
            java.util.Map schemeProps = (java.util.Map) cacheClasses.get(cacheScheme.toLowerCase());
            
            if (!schemeProps.containsKey("type")) {
                throw new ConfigurationException("Can not find the type of cache, invalid configuration for cache class '" + cacheScheme + "'");
            }

            if (getName().length() < 1) {
                _cacheInfo.setName(cacheScheme);
            }

            //Initialize the performance counters, if enabled.
            boolean bEnableCounter = true;

            if (properties.containsKey("perf-counters")) {
                bEnableCounter = (Boolean.parseBoolean(properties.get("perf-counters").toString()));
            }

            // Check the type of license the application
            boolean inProc = false;
            boolean isClusterable = true;

            // we wants to provide replicated cluster.
           
                if (properties.containsKey("inproc")) {
                    Object obj = properties.get("inproc");
                    inProc = (Boolean) obj;
                }

             
                

                if (inProc) {

                    try
					{
                        MiscUtil.RegisterCompact();
                    } catch (CacheArgumentException nCacheArgumentException) {
                    }


                }

            

            _context.ExpiryMgr = new ExpirationManager(schemeProps, _context);

            _context.ExpiryMgr.setTopLevelCache(this);

            _cacheInfo.setClassName(String.valueOf(schemeProps.get("type")).toLowerCase());
            _context.AsyncProc.Start();
            _asyncProcessor.Start();


            if(schemeProps.containsKey("isclientcache") && schemeProps.get("isclientcache").equals("true"))
            {
                if(inProc)
                {
                    _context.setInMemoryDataFormat(DataFormat.Object);
                }else{
                    _context.setInMemoryDataFormat(DataFormat.Binary);
                }
            }

            if (_cacheInfo.getClassName().compareTo("replicated-server") == 0) {
                if(inProc){
                    throw new OperationNotSupportedException("Cluster Topology cannot be started as Inproc Cluster");
                }
                if (ServicePropValues.CacheServer_EventsPersistence != null) {
                        _isPersistEnabled = Boolean.parseBoolean(ServicePropValues.CacheServer_EventsPersistence);
                    }
                    if (_isPersistEnabled) {
                        if (ServicePropValues.CacheServer_EventsPersistenceInterval != null) {
                            _persistenceInterval = Integer.parseInt(ServicePropValues.CacheServer_EventsPersistenceInterval);
                        }

                        _context.PersistenceMgr = new PersistenceManager(_persistenceInterval);
                    }
                    if (isClusterable) {
                        _context.setCacheImpl(new ReplicatedServerCache(cacheClasses, schemeProps, this, _context, this, userId, password));
                        _context.getCacheImpl().Initialize(cacheClasses, schemeProps, userId, password, twoPhaseInitialization);
                    }
                
            }
            
            if (_cacheInfo.getClassName().compareTo("partitioned-server") == 0) {
                if(inProc){
                    throw new OperationNotSupportedException("Cluster Topology cannot be started as Inproc Cluster");
                }
                if (isClusterable) {
                    _context.setCacheImpl(new PartitionedServerCache(cacheClasses, schemeProps, this, _context, this, userId, password));
                    _context.getCacheImpl().Initialize(cacheClasses, schemeProps, userId, password, twoPhaseInitialization);
                }
            }

            if (_context.getCacheImpl() == null) {

                if (_cacheInfo.getClassName().compareTo("overflow-cache") == 0) {
                    
                        LocalCacheImpl cache = new LocalCacheImpl();
                        cache.setInternal(CacheBase.Synchronized(new IndexedOverflowCache(cacheClasses, cache, schemeProps, this, _context)));
                        _context.setCacheImpl(cache);
                   
                } else {
                    if (_cacheInfo.getClassName().compareTo("local-cache") == 0) {
                        LocalCacheImpl cache = new LocalCacheImpl();
                        cache.setInternal(CacheBase.Synchronized(new IndexedLocalCache(cacheClasses, cache, schemeProps, this, _context)));
                        _context.setCacheImpl(cache);
                    } else {
                        throw new ConfigurationException("Specified cache class '" + _cacheInfo.getClassName() + "' is not available in this edition of TayzGrid.");
                    }
                }
            }
            
            _cacheType = _cacheInfo.getClassName();
            _isClustered = _cacheInfo.getIsClusteredCache();
            // Start the expiration manager if the cache was created sucessfully!
            if (_context.getCacheImpl() != null) {
                /**
                 * there is no need to do expirations on the Async replica's;
                 * Expired items are removed fromreplica by the respective
                 * active partition.
                 */
                if (!isStartingAsMirror) {
                    _context.ExpiryMgr.Start();
                }

                if (bEnableCounter) {
                    
                        int snmpPort = PortCalculator.getSNMPPort(cPort);
                    if (_context.getCacheImpl() instanceof ClusterCacheBase) {

                        
                        if(isStartingAsMirror)
                            snmpPort = PortCalculator.getSNMPPortReplica(cPort);
                        
                        _context.PerfStatsColl.initializePerfCounters(this._inProc, true, snmpPort);
                        ((ClusterCacheBase) _context.getCacheImpl()).InitializeClusterPerformanceCounter(_context.PerfStatsColl);

                    } else {
                        _context.PerfStatsColl.initializePerfCounters(this._inProc, false, snmpPort);
                        //((ClusterCacheBase) _context.getCacheImpl()).InitializeClusterPerformanceCounter(_context.PerfStatsColl);

                    } 
                    PortPool.getInstance().assignPort(_context.getCacheRoot().getName(), _context.PerfStatsColl.getSNMPPort());
                    getServerInitialeNodes(schemeProps);
                }
                
                if(_context.PerfStatsColl!=null)
                {
                    _context.PerfStatsColl.isProc(Boolean.toString(inProc));
                }
            } else {
                _context.ExpiryMgr.dispose();
               
            }
        } catch (ConfigurationException e) {
            _context.getCacheLog().Error("Cache.CreateInternalCache()", e.toString());
            _context.setCacheImpl(null);
            dispose();
            throw e;
        } catch (Exception e) {
            _context.getCacheLog().Error("Cache.CreateInternalCache()", e.toString());
            _context.setCacheImpl(null);
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    private void getServerInitialeNodes(java.util.Map schemeProps) {

        if (_context.PerfStatsColl != null) {
            java.util.Map clusterProps = (java.util.Map) ((schemeProps.get("cluster") instanceof java.util.Map) ? schemeProps.get("cluster") : null);
            HashMap nodeList = new HashMap();
            if (clusterProps != null) {
                nodeList = (HashMap) ((clusterProps.get("channel") instanceof HashMap) ? clusterProps.get("channel") : null);
                if (nodeList != null) {
                    nodeList = (HashMap) ((nodeList.get("tcpping") instanceof HashMap) ? nodeList.get("tcpping") : null);
                    if (nodeList != null) {
                        if (nodeList.containsKey("initial_hosts")) {

                           _context.PerfStatsColl.setCacheServers(String.valueOf(nodeList.get("initial_hosts")));
                        }
                    }
                }
            }
        }
    }

    private void CreateInternalCache2(java.util.Map properties, String userId, String password) throws ConfigurationException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            String cacheScheme = String.valueOf(properties.get("name")).trim();

            if (getName().length() < 1) {
                _cacheInfo.setName(cacheScheme);
            }

            //Initialize the performance counters, if enabled.
            boolean bEnableCounter = true;

            if (properties.containsKey("perf-counters")) {
                java.util.HashMap perfCountersProps = (java.util.HashMap) ((properties.get("perf-counters") instanceof java.util.HashMap) ? properties.get("perf-counters") : null);
                if (perfCountersProps != null) {
                    if (perfCountersProps.containsKey("enabled")) {
                        bEnableCounter = (Boolean) (perfCountersProps.get("enabled"));
                    }
                }
            }

            //we wants to provide replicated cluster.
            boolean isClusterable = true;

            _context.ExpiryMgr = new ExpirationManager(properties, _context);
            //For the removal of Cascaded Dependencies on Clean Interval.
            _context.ExpiryMgr.setTopLevelCache(this);

            java.util.Map clusterProps = null;

            if (properties.containsKey("cluster")) {
                clusterProps = (java.util.Map) ((properties.get("cluster") instanceof java.util.Map) ? properties.get("cluster") : null);
                if (clusterProps.containsKey("topology")) {
                    _cacheInfo.setClassName(String.valueOf(clusterProps.get("topology")).trim());
                }
            } else {
                _cacheInfo.setClassName("local");
            }

            _context.AsyncProc.Start();
            _asyncProcessor.Start();

            //We should use an InternalCacheFactory for the code below
            if (_cacheInfo.getClassName().compareTo("replicated") == 0) {
                if (isClusterable) //_context.CacheImpl = new ReplicatedServerCache(cacheClasses, schemeProps, this, _context);
                {
                    _context.setCacheImpl(new ReplicatedServerCache(properties, clusterProps, this, _context, this, userId, password));
                    _context.getCacheImpl().Initialize(properties, clusterProps, userId, password, false);
                }
            } else {
                if (_cacheInfo.getClassName().compareTo("local") == 0) {
                    LocalCacheImpl cache = new LocalCacheImpl();
                    cache.setInternal(CacheBase.Synchronized(new LocalCache(properties, cache, properties, this, _context)));
                    _context.setCacheImpl(cache);
                } else {
                    throw new ConfigurationException("Specified cache class '" + _cacheInfo.getClassName() + "' is not available in this edition of TayzGrid.");
                }
            }

            _cacheType = _cacheInfo.getClassName();
            _isClustered = _cacheInfo.getIsClusteredCache();
            // Start the expiration manager if the cache was created sucessfully!
            if (_context.getCacheImpl() != null) {
                _context.ExpiryMgr.Start();
                if (bEnableCounter) {
                    if (_context.getCacheImpl() instanceof ClusterCacheBase) {
                        _context.PerfStatsColl.initializePerfCounters(this._inProc, true);
                        ((ClusterCacheBase) _context.getCacheImpl()).InitializeClusterPerformanceCounter(_context.PerfStatsColl);

                    } else {
                        _context.PerfStatsColl.initializePerfCounters(this._inProc);
                    }
                }
            } else {
                _context.ExpiryMgr.dispose();
                
            }
        } catch (ConfigurationException e) {
            _context.getCacheLog().Error("Cache.CreateInternalCache()", e.toString());
            _context.setCacheImpl(null);
            dispose();
            throw e;
        } catch (Exception e) {
            _context.getCacheLog().Error("Cache.CreateInternalCache()", e.toString());
            _context.setCacheImpl(null);
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    private void CheckDataSourceAvailabilityAndOptions(DataSourceUpdateOptions updateOpts) throws OperationFailedException {
        if (updateOpts != DataSourceUpdateOptions.None) {
//            if (_context.getDsMgr() != null && ((_context.getDsMgr().getIsWriteThruEnabled() && updateOpts == DataSourceUpdateOptions.WriteThru)
//                    || ((updateOpts == DataSourceUpdateOptions.WriteBehind || updateOpts
//                    == DataSourceUpdateOptions.WriteThru)))) {
//                return;
//            }
            
            if (_context.getDsMgr() != null && ((_context.getDsMgr().getIsWriteThruEnabled() && 
                    (updateOpts == DataSourceUpdateOptions.WriteThru || updateOpts == DataSourceUpdateOptions.WriteBehind)))) {
                return;
            }

            throw new OperationFailedException("Backing source not available. Verify backing source settings");
        }
    }
    
    private boolean CheckDataSourceAvailabilityAndOptions(DataSourceReadOptions readOptions, String providerName) throws OperationFailedException {
        if(readOptions != DataSourceReadOptions.None) {
            
            if(_context.getDsMgr() != null && (_context.getDsMgr().getIsReadThruEnabled() && 
                   ((readOptions == DataSourceReadOptions.ReadThru || readOptions
                    == DataSourceReadOptions.OptionalReadThru) ))) {
                if(providerName == null || providerName.isEmpty())
                    providerName = _context.getDsMgr().getDefaultReadThruProvider();
                if(_cacheInfo.getConfiguration().getBackingSource() != null) {
                    Provider[] providers = _cacheInfo.getConfiguration().getBackingSource().getReadthru().getProviders();
                    if(providers != null) {
                    for(int i=0;i<providers.length;i++) {
                        if(providers[i].getProviderName().equalsIgnoreCase(providerName)) {
                            if(providers[i].getIsLoaderOnly()) {
                                if(readOptions == DataSourceReadOptions.ReadThru)
                                    throw new OperationFailedException("Backing source is configured as a loader only. Cannot perform a read through operation.");
                                else
                                    return true;
                            }
                            else
                                return false;
                        }
                    }
                    throw new OperationFailedException("Invalid provider name.");
                }
                    else if(( providers == null) && _context.getCacheRoot().getIsInProc()) {
                        if(_cacheInfo.getConfiguration().getIsLoaderOnly() && (_cacheInfo.getConfiguration().getCacheLoaderFactory()!= null &&_cacheInfo.getConfiguration().getCacheLoaderFactory().toString().toLowerCase().equals(providerName))) {
                            return true;
                        }
                    return false;
                } 
            }
        }
             throw new OperationFailedException("Backing source not available. Verify backing source settings");
        }
        return false;
    }
    
    /**
     *
     * @param array
     * @param newLength
     */
    public static void Resize(tangible.RefObject<Object[]> array, int newLength) {
        if (array.argvalue == null) {
            return;
        }
        if (array.argvalue.length == newLength) {
            return;
        }

        if (array.argvalue instanceof String[]) {
            String[] copyArray = new String[newLength];
            for (int i = 0; i < newLength; i++) {
                if (i < array.argvalue.length) {
                    copyArray[i] = (String) array.argvalue[i];
                } else {
                    break;
                }
            }
            array.argvalue = copyArray;
        } else if (array.argvalue instanceof CacheEntry[]) {
            CacheEntry[] copyArray = new CacheEntry[newLength];
            for (int i = 0; i < newLength; i++) {
                if (i < array.argvalue.length) {
                    copyArray[i] = (CacheEntry) array.argvalue[i];
                } else {
                    break;
                }
            }
            array.argvalue = copyArray;
        } else {

            Object[] copyArray = new Object[newLength];
            for (int i = 0; i < newLength; i++) {
                if (i < array.argvalue.length) {
                    copyArray[i] = array.argvalue[i];
                } else {
                    break;
                }
            }
            array.argvalue = copyArray;
        }
    }

    /**
     * Clear all the contents of cache
     *
     * @return
     */
    public final void Clear() throws OperationFailedException {
        Clear(new BitSet(), null, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
    }

    /**
     * Clear all the contents of cache
     *
     * @return
     */
    public final void Clear(OperationContext operationContext) throws OperationFailedException {
        Clear(new BitSet(), null, operationContext);
    }

    /**
     * Clear all the contents of cache
     *
     * @return
     */
    public final void Clear(BitSet flag, CallbackEntry cbEntry, OperationContext operationContext) throws OperationFailedException {
        // Cache has possibly expired so do default.
        if (!getIsRunning()) { //throw new InvalidOperationException();
            return;
        }

        Object block = null;
        boolean isNoBlock = false;
        block = operationContext.GetValueByField(OperationContextFieldName.NoGracefulBlock);
        if (block != null) {
            isNoBlock = (Boolean) block;
        }

        if (!isNoBlock) {
            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkWrite, operationContext)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }
        }

        DataSourceUpdateOptions updateOpts = this.UpdateOption(flag);
        this.CheckDataSourceAvailabilityAndOptions(updateOpts);

        try {

            String providerName = null;
            if (operationContext.Contains(OperationContextFieldName.ReadThruProviderName)) {
                providerName = (String) operationContext.GetValueByField(OperationContextFieldName.ReadThruProviderName);
            }

            _context.getCacheImpl().Clear(cbEntry, updateOpts, operationContext);

            if (updateOpts == DataSourceUpdateOptions.WriteThru) {
                this._context.getDsMgr().WriteThru(null, null, OpCode.Clear, providerName, operationContext);
            } else if (!_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind ) {
                CacheEntry entry = null;
                if (cbEntry != null) {
                    entry = new CacheEntry(cbEntry, null, null);
                }
                this._context.getDsMgr().WriteBehind(_context.getCacheImpl(), null, entry, null, null, providerName, OpCode.Clear, WriteBehindAsyncProcessor.TaskState.Execute);
            }

        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Clear()", inner.toString());
            throw new OperationFailedException("Clear operation failed. Error: " + inner.getMessage(), inner);
        }
    }

    /**
     *
     * @param flagMap
     * @param cbEntry
     * @param operationContext
     */
    public final void ClearAsync(BitSet flagMap, CallbackEntry cbEntry, OperationContext operationContext) {
        // Cache has possibly expired so do default.

        if (!getIsRunning()) {
            return;
        }
        _asyncProcessor.Enqueue(new AsyncClear(this, cbEntry, flagMap, operationContext));

    }
    
    public CacheConfigParams getCacheConfiguration() throws OperationFailedException {
        CacheConfigParams cacheConfigParams = new CacheConfigParams();
        
        if (ServerMonitor.getMonitorActivity()) 
            ServerMonitor.LogClientActivity("Cache.GetCacheConfig", "");
        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }
        try {
            if(this._context.getDsMgr() != null) {
                if(_context.getDsMgr().getIsReadThruEnabled()) {
                    String providerName = _context.getDsMgr().getDefaultReadThruProvider();
                    boolean result = this.CheckDataSourceAvailabilityAndOptions(DataSourceReadOptions.OptionalReadThru, providerName);
                    if(result)
                        cacheConfigParams.setIsReadThru(false);
                    else
                       cacheConfigParams.setIsReadThru(this._context.getDsMgr().getIsReadThruEnabled());  
                }
                else
                    cacheConfigParams.setIsReadThru(this._context.getDsMgr().getIsReadThruEnabled()); 
                

                cacheConfigParams.setIsWriteThru(this._context.getDsMgr().getIsWriteThruEnabled());
            }
            if(this._cacheInfo.getConfiguration() != null) {
                cacheConfigParams.setIsStatisticsEnabled((this._cacheInfo.getConfiguration().getPerfCounters().getEnabled()));
            }
        }
        catch (Exception inner) {
            _context.getCacheLog().Error("Cache.GetCacheConfiguration()", inner.toString());
            throw new OperationFailedException("GetCacheConfiguration operation failed. Error : " + inner.getMessage(), inner);
        }
        return cacheConfigParams;
    }

    /**
     * Determines whether the cache contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @param operationContext
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     * @throws OperationFailedException
     */
    public final boolean Contains(Object key, OperationContext operationContext) throws OperationFailedException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("Cache.Contains", "");
        }
        if (key == null) {
            throw new IllegalArgumentException("key");
        }


        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return false;
        }

        try {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(key, AllowedOperationType.AtomicRead)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }

            return _context.getCacheImpl().Contains(key, operationContext);
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Contains()", inner.toString());
            throw new OperationFailedException("Contains operation failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     *
     * @param key
     * @param group
     * @param subGroup
     * @param lockId
     * @param lockDate
     * @param lockTimeout
     * @param accessType
     * @param operationContext
     * @return
     * @throws OperationFailedException
     */
    public final Object GetCacheEntry(Object key, String group, String subGroup, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, TimeSpan lockTimeout, tangible.RefObject<Long> version, LockAccessType accessType, OperationContext operationContext) throws
            OperationFailedException {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(key, AllowedOperationType.AtomicRead)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        try {
            _context.PerfStatsColl.mSecPerGetBeginSample();
            _context.PerfStatsColl.incrementGetPerSecStats();
            CacheEntry entry = null;

            LockExpiration lockExpiration = null;

            //Alam: if lockId will be empty if item is not already lock provided by user
            if ((lockId.argvalue != null && lockId.argvalue.toString().equals("")) || lockId.argvalue == null) {
                if (accessType == LockAccessType.ACQUIRE) {
                    lockId.argvalue = GetLockId(key);
                    lockDate.argvalue = new java.util.Date();

                    if (!TimeSpan.ZERO.equals(lockTimeout)) {
                        lockExpiration = new LockExpiration(lockTimeout);
                    }
                }
            }

            Object generatedLockId = lockId.argvalue;
            //if only key is provided by user
            if (group == null && subGroup == null && (accessType == LockAccessType.IGNORE_LOCK || accessType == LockAccessType.DONT_ACQUIRE)) {
                entry = _context.getCacheImpl().Get(key, operationContext);
            } //if key , group and sub-group are provided by user
            else if (group != null) {
                entry = _context.getCacheImpl().GetGroup(key, group, subGroup, version, lockId, lockDate, null, LockAccessType.IGNORE_LOCK, operationContext);
            } //Alam: if key and locking information is provided by user
            else {
                entry = _context.getCacheImpl().Get(key, version, lockId, lockDate, lockExpiration, accessType, operationContext);
            }

            if (entry == null && accessType == LockAccessType.ACQUIRE) {
                if (lockId.argvalue == null || generatedLockId.equals(lockId.argvalue)) {
                    lockId.argvalue = null;
                    lockDate.argvalue = new java.util.Date(0);
                }
            }
            boolean isLoaderOnly = false;
            boolean doReadThru = false;
            if (operationContext.Contains(OperationContextFieldName.ReaderBitsetEnum)) {
              //  boolean isReadThruFlagSet = this.CheckDataSourceAvailabilityAndOptions(DataSourceReadOptions.ReadThru, group);
                BitSet flagMap  = (BitSet) operationContext.GetValueByField(OperationContextFieldName.ReaderBitsetEnum);
                DataSourceReadOptions readOptions = this.ReadOption(flagMap);
                String providerName = null;
                if (operationContext.Contains(OperationContextFieldName.ReadThruProviderName)) {
                    providerName = String.valueOf(operationContext.GetValueByField(OperationContextFieldName.ReadThruProviderName));
                }
                isLoaderOnly  = this.CheckDataSourceAvailabilityAndOptions(readOptions, providerName);
              if(!isLoaderOnly && readOptions != DataSourceReadOptions.None) {
                  doReadThru = true;
              }

                boolean itemIsLocked = false;
                if (accessType == LockAccessType.DONT_ACQUIRE) {
                    if (generatedLockId == null && lockId.argvalue != null && ((String) lockId.argvalue).compareTo("") != 0) {
                        itemIsLocked = true;
                    }
                } else if (accessType == LockAccessType.ACQUIRE) {
                    if (generatedLockId != null && lockId.argvalue != null && ((String) lockId.argvalue).compareTo("") != 0 && !generatedLockId.equals(lockId.argvalue)) {
                        itemIsLocked = true;
                    }
                }

//                if (entry == null && !itemIsLocked && (isReadThruFlagSet && _context.getDsMgr() != null && _context.getDsMgr().getIsReadThruEnabled())) {
                  if (entry == null && !itemIsLocked && doReadThru) {  
                    BitSet bitset = new BitSet();
                    tangible.RefObject<CacheEntry> tempRef_entry = new tangible.RefObject<CacheEntry>(entry);
                    tangible.RefObject<BitSet> tempRef_bitset = new tangible.RefObject<BitSet>(bitset);
                    _context.getDsMgr().ResyncCacheItem(key, tempRef_entry, tempRef_bitset, group, subGroup, providerName, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
                    entry = tempRef_entry.argvalue;
                    bitset = tempRef_bitset.argvalue;
                }
            }

            _context.PerfStatsColl.mSecPerGetEndSample();

            if (entry != null) {
                _context.PerfStatsColl.incrementHitsPerSecStats();
            } else {
                _context.PerfStatsColl.incrementMissPerSecStats();
            }

            return entry;
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Get()", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Get()", inner.toString());
            throw new OperationFailedException("Get operation failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Retrieve the object from the cache. A key is passed as parameter.
     *
     * @param key
     * @return
     * @throws OperationFailedException
     */
    public final CompressedValueEntry Get(Object key) throws OperationFailedException {
        return GetGroup(key, new BitSet(), null, null, null, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
    }

    public final java.util.HashMap GetByTag(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException {
        if (!getIsRunning()) {
            return null;
        }

        java.util.HashMap table = null;

        try {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkRead, operationContext)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }

            table = _context.getCacheImpl().GetTagData(tags, comparisonType, operationContext);

            if (table != null) {
                Iterator ide = ((java.util.HashMap) table.clone()).entrySet().iterator();
                while (ide.hasNext()) {
                    Map.Entry pair = (Map.Entry) ide.next();
                    Object key = pair.getKey();
                    CacheEntry entry = (CacheEntry) ((pair.getValue() instanceof CacheEntry) ? pair.getValue() : null);
                    CompressedValueEntry val = new CompressedValueEntry();
                    val.Value = entry.getValue() instanceof CallbackEntry ? ((CallbackEntry) entry.getValue()).getValue() : entry.getValue();
                    val.Flag = entry.getFlag();
                    table.put(key, val);
                }
            }
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.GetByTag()", inner.toString());
            }
            throw inner;
        } catch (StateTransferInProgressException inner) {
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.GetByTag()", inner.toString());
            throw new OperationFailedException("GetByTag operation failed. Error : " + inner.getMessage(), inner);
        }

        return table;
    }

    public final java.util.ArrayList GetKeysByTag(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException {
        if (!getIsRunning()) {
            return null;
        }

        try {
            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkRead, operationContext)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }

            return _context.getCacheImpl().GetTagKeys(tags, comparisonType, operationContext);
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.GetKeysByTag()", inner.toString());
            }
            throw inner;
        } catch (StateTransferInProgressException inner) {
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.GetKeysByTag()", inner.toString());
            throw new OperationFailedException("GetKeysByTag operation failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Remove items from cache on the basis of specified Tags
     *
     * @param sTags Tag names
     * @param comparisonType
     * @param operationContext
     * @throws OperationFailedException
     */
    public final void RemoveByTag(String[] sTags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException {
        if (!getIsRunning()) {
            return;
        }

        try {
            CascadedRemove(sTags, comparisonType, true, operationContext);
        } catch (StateTransferInProgressException se) {
            throw se;
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.RemoveByTag()", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.RemoveByTag()", inner.toString());
            throw new OperationFailedException("RemoveByTag operation failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Retrieve the object from the cache.
     *
     * @param key
     * @param lockId
     * @param isPreemptive
     * @param operationContext
     * @throws OperationFailedException
     * @throws LockingException
     */
    public final void Unlock(Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws OperationFailedException, LockingException,
            GeneralFailureException, CacheException {
        if (getIsRunning()) {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(key, AllowedOperationType.AtomicWrite)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }
            _context.getCacheImpl().UnLock(key, lockId, isPreemptive, operationContext);
        }
    }

    public final boolean IsLocked(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws
            OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (getIsRunning()) {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(key, AllowedOperationType.AtomicRead)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }

            Object passedLockId = lockId.argvalue;
            LockOptions lockInfo = _context.getCacheImpl().IsLocked(key, lockId, lockDate, operationContext);
            if (lockInfo != null) {
                if (lockInfo.getLockId() == null) {
                    return false;
                }
                lockId.argvalue = lockInfo.getLockId();
                lockDate.argvalue = lockInfo.getLockDate();

                return !lockInfo.getLockId().equals(passedLockId);

            } else {
                return false;
            }
        }
        return false;
    }

    public final boolean Lock(Object key, TimeSpan lockTimeout, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws
            OperationFailedException, LockingException, GeneralFailureException, CacheException {
        lockId.argvalue = null;
        lockDate.argvalue = NCDateTime.getUTCNow();
        LockExpiration lockExpiration = null;
        lockTimeout = lockTimeout != null ? lockTimeout : TimeSpan.ZERO;
        if (!TimeSpan.ZERO.equals(lockTimeout)) {
            lockExpiration = new LockExpiration(lockTimeout);
        }

        if (getIsRunning()) {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(key, AllowedOperationType.AtomicWrite)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }

            Object generatedLockId = lockId.argvalue = GetLockId(key);
            LockOptions lockInfo = _context.getCacheImpl().Lock(key, lockExpiration, lockId, lockDate, operationContext);
            if (lockInfo != null) {
                lockId.argvalue = lockInfo.getLockId();
                lockDate.argvalue = lockInfo.getLockDate();
                if (generatedLockId.equals(lockInfo.getLockId())) {
                    return true;
                }
                return false;
            } else {
                lockId.argvalue = null;
                return false;
            }
        }
        return false;
    }

    /**
     *
     * @param key
     * @param flagMap
     * @param group
     * @param subGroup
     * @param providerName
     * @param operationContext
     * @return
     * @throws OperationFailedException
     */
    public final CompressedValueEntry GetGroup(Object key, BitSet flagMap, String group, String subGroup, String providerName, OperationContext operationContext) throws
            OperationFailedException {
        Object lockId = null;
        java.util.Date lockDate = NCDateTime.getUTCNow();

        long version = 0;
        tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
        tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
        tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
        CompressedValueEntry tempVar = GetGroup(key, flagMap, group, subGroup, tempRef_version, tempRef_lockId, tempRef_lockDate, TimeSpan.ZERO, LockAccessType.IGNORE_LOCK, providerName, operationContext);
        version = tempRef_version.argvalue;
        lockId = tempRef_lockId.argvalue;
        lockDate = tempRef_lockDate.argvalue;
        return tempVar;
    }

    private Object GetLockId(Object key) {
        long nextId = 0;
        synchronized (this) {
            nextId = _lockIdTicker++;
        }
        return ManagementFactory.getRuntimeMXBean().getName() + "-" + System.getProperty("user.name") + "-" + key.toString() + "-" + nextId;
    }

    /**
     * Retrieve the object from the cache.
     *
     * @param key
     * @param group
     * @param subGroup
     * @return
     */
    public final CompressedValueEntry GetGroup(Object key, BitSet flagMap, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, TimeSpan lockTimeout, LockAccessType accessType, String providerName, OperationContext operationContext) throws
            OperationFailedException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("Cache.GetGrp", "");
        }
        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(key, AllowedOperationType.AtomicRead)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        boolean groupSpecified = false;
        CompressedValueEntry result = new CompressedValueEntry();
        CacheEntry e = null;
        try {
            _context.PerfStatsColl.mSecPerGetBeginSample();
            _context.PerfStatsColl.incrementGetPerSecStats();
            _context.PerfStatsColl.incrementHitsRatioPerSecBaseStats();

            HPTimeStats getTime = new HPTimeStats();
            getTime.BeginSample();

            LockExpiration lockExpiration = null;
            if (accessType == LockAccessType.ACQUIRE) {
                lockId.argvalue = GetLockId(key);
                lockDate.argvalue = NCDateTime.getUTCNow();
                lockTimeout = lockTimeout != null ? lockTimeout : TimeSpan.ZERO;
                if (!TimeSpan.ZERO.equals(lockTimeout)) {
                    lockExpiration = new LockExpiration(lockTimeout);
                }
            }
            boolean doReadThru = false;
            boolean isLoaderOnly = false;
            if(flagMap != null) {
                DataSourceReadOptions readOptions = this.ReadOption(flagMap);
                isLoaderOnly = this.CheckDataSourceAvailabilityAndOptions(readOptions, providerName);
                if(!isLoaderOnly && readOptions != DataSourceReadOptions.None)
                    doReadThru = true;
            }
            
            Object generatedLockId = lockId.argvalue;
            if ((group == null || group.isEmpty()) && (subGroup == null || subGroup.isEmpty())) {
                e = _context.getCacheImpl().Get(key, version, lockId, lockDate, lockExpiration, accessType, operationContext);
            } else {
                groupSpecified = true;
                e = _context.getCacheImpl().GetGroup(key, group, subGroup, version, lockId, lockDate, lockExpiration, accessType, operationContext);
            }

            if (e == null && accessType == LockAccessType.ACQUIRE) {
                if (lockId.argvalue == null || generatedLockId.equals(lockId.argvalue)) {
                    lockId.argvalue = null;
                    lockDate.argvalue = new NCDateTime(1970, 1, 1, 0, 0, 0, 0).getDate();
                }
            }
        //    if (flagMap != null) {
            //    boolean isReadThruFlagSet = flagMap.IsBitSet((byte) BitSetConstants.ReadThru);
         //       boolean readThru = false;

                if (e != null) {
                    _context.PerfStatsColl.mSecPerGetEndSample();

                    result.Value = e.getValue();
                    result.Flag = e.getFlag();
                }

                boolean itemIsLocked = false;
                if (accessType == LockAccessType.DONT_ACQUIRE) {
                    if (generatedLockId == null && lockId.argvalue != null && ((String) lockId.argvalue).compareTo("") != 0) {
                        itemIsLocked = true;
                    }
                } else if (accessType == LockAccessType.ACQUIRE) {
                    if (generatedLockId != null && lockId.argvalue != null && ((String) lockId.argvalue).compareTo("") != 0 && !generatedLockId.equals(lockId.argvalue)) {
                        itemIsLocked = true;
                    }
                }
                
             //   if (e == null && !itemIsLocked && (isReadThruFlagSet && _context.getDsMgr() != null && _context.getDsMgr().getIsReadThruEnabled())) {
                if (e == null && !itemIsLocked && doReadThru) {
                    result.Flag = new BitSet();
                    tangible.RefObject<CacheEntry> tempRef_e = new tangible.RefObject<CacheEntry>(e);
                    tangible.RefObject<BitSet> tempRef_Flag = new tangible.RefObject<BitSet>(result.Flag);
                    result.Value = _context.getDsMgr().ResyncCacheItem(key, tempRef_e, tempRef_Flag, group, subGroup, providerName, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
                    e = tempRef_e.argvalue;
                    result.Flag = tempRef_Flag.argvalue;
                }
       //     }
            _context.PerfStatsColl.mSecPerGetEndSample();

            getTime.EndSample();

            if (result.Value != null) {
                _context.PerfStatsColl.incrementHitsRatioPerSecStats();
                _context.PerfStatsColl.incrementHitsPerSecStats();
            } else {
                _context.PerfStatsColl.incrementMissPerSecStats();
            }
            if (result.Value instanceof CallbackEntry) {
                result.Value = ((CallbackEntry) result.Value).getValue();
            }
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Get()", "Get operation failed. Error : " + inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Get()", "Get operation failed. Error : " + inner.toString());
            throw new OperationFailedException("Get operation failed. Error :" + inner.getMessage(), inner);
        }

        return result;
    }

    private String GetDefaultProvider() {
        return "";
    }

    /**
     * Retrieve the list of keys fron the cache for the given group or sub
     * group.
     */
    public final java.util.ArrayList GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException {
        if (group == null) {
            throw new IllegalArgumentException("group");
        }
        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkRead, operationContext)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        try {
            HPTimeStats getTime = new HPTimeStats();

            getTime.BeginSample();
            java.util.ArrayList result = _context.getCacheImpl().GetGroupKeys(group, subGroup, operationContext);
            getTime.EndSample();

            if (result != null) {
                _context.PerfStatsColl.incrementGetPerSecStatsBy(result.size());
            }
            return result;

        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Get()", "Get operation failed. Error :" + inner.toString());
            }
            throw inner;
        } catch (StateTransferInProgressException inner) {
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Get()", "Get operation failed. Error :" + inner.toString());
            throw new OperationFailedException("Get operation failed. Error :" + inner.getMessage(), inner);
        }
    }

    /**
     * Retrieve the list of key and value pairs from the cache for the given
     * group or sub group.
     */
    public final java.util.HashMap GetGroupData(String group, String subGroup, OperationContext operationContext) throws OperationFailedException {
        if (group == null) {
            throw new IllegalArgumentException("group");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkRead, operationContext)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        try {
            HPTimeStats getTime = new HPTimeStats();
            getTime.BeginSample();

            java.util.HashMap table = _context.getCacheImpl().GetGroupData(group, subGroup, operationContext);

            if (table != null) {
                Object[] keyArr = new Object[table.size()];
                System.arraycopy(table.keySet().toArray(), 0, keyArr, 0, table.keySet().size());
                java.util.Iterator ie = Arrays.asList(keyArr).iterator();

                CompressedValueEntry val = null;
                while (ie.hasNext()) {
                    Object obj = ie.next();
                    val = new CompressedValueEntry();
                    CacheEntry entry = (CacheEntry) table.get(obj);
                    val.Value = entry.getValue();
                    if (val.Value instanceof CallbackEntry) {
                        val.Value = ((CallbackEntry) val.Value).getValue();
                    }
                    val.Flag = entry.getFlag();
                    table.put(obj, val);
                }

                getTime.EndSample();
                _context.PerfStatsColl.incrementGetPerSecStatsBy(table.size());
            }

            return table;
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Get()", "Get operation failed. Error : " + inner.toString());
            }
            throw inner;
        } catch (StateTransferInProgressException inner) {
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Get()", "Get operation failed. Error : " + inner.toString());
            throw new OperationFailedException("Get operation failed. Error : " + inner.getMessage(), inner);
        }
    }
    
    public void AsyncGetBulk(Object[] keys, BitSet flagMap, String providerName, OperationContext operationContext) {
        if(keys == null) {
            throw new IllegalArgumentException("keys");
        }
        
        if (!getIsRunning()) { 
            return;
        }
        _asyncProcessor.Enqueue(new AsyncBulkGet(this,keys, flagMap, providerName, operationContext));
        
    }

    /**
     * Retrieve the array of objects from the cache. An array of keys is passed
     * as parameter.
     */
    public final java.util.Map GetBulk(Object[] keys, BitSet flagMap, String providerName, OperationContext operationContext) throws OperationFailedException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("Cache.GetBlk", "");
        }

        if (keys == null) {
            throw new IllegalArgumentException("keys");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(keys, AllowedOperationType.BulkRead, operationContext)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        java.util.HashMap table = null;
        boolean replaceExistingValues = false;
        boolean isJCacheLoader = false;

        int updateCounter = 0;
        try {
            HPTimeStats getTime = new HPTimeStats();
            getTime.BeginSample();
 
            if (operationContext.Contains(OperationContextFieldName.ReplaceExistingValues))
                replaceExistingValues =(Boolean) operationContext.GetValueByField(OperationContextFieldName.ReplaceExistingValues);
            
            boolean readThruEnabled = false;
            boolean isLoaderOnly = false;
            
            if(operationContext.Contains(OperationContextFieldName.JCacheLoader)) 
                   isJCacheLoader = (Boolean) operationContext.GetValueByField(OperationContextFieldName.JCacheLoader);
           if(flagMap != null) {
                DataSourceReadOptions readOptions = this.ReadOption(flagMap);
                isLoaderOnly = this.CheckDataSourceAvailabilityAndOptions(readOptions, providerName);
           
              //  readThruEnabled = _context.getDsMgr() != null && _context.getDsMgr().getIsReadThruEnabled();
            if((isLoaderOnly && isJCacheLoader ) || (!isLoaderOnly && readOptions != DataSourceReadOptions.None) )
                readThruEnabled = true;
           }
            
            
            if(replaceExistingValues) { 
                
                table = new HashMap();
            }
            else {
                table = _context.getCacheImpl().Get(keys, operationContext);
            }
            
            if (table != null ) {
                
                /**
                 * We maintain indexes of keys that needs resync or are not
                 * fetched in this array This saves us from instantiating 3
                 * separate arrays and then resizing it; 3 arrays to hold keys,
                 * enteries, and flags
                 */
                int[] resyncIndexes = null;
                int counter = 0;

                if (readThruEnabled) {
                    resyncIndexes = new int[keys.length];
                }


                for (int i = 0; i < keys.length; i++) {
                    if(_context.getCacheImpl().Contains(keys[i], null))
                        updateCounter++;
                    if (table.containsKey(keys[i])) {
                        if (table.get(keys[i]) != null) {
                            CacheEntry entry = (CacheEntry) ((table.get(keys[i]) instanceof CacheEntry) ? table.get(keys[i]) : null);
                            CompressedValueEntry val = new CompressedValueEntry();
                            val.Value = entry.getValue() instanceof CallbackEntry ? ((CallbackEntry) entry.getValue()).getValue() : entry.getValue();
                            val.Flag = entry.getFlag();
                            table.put(keys[i], val);
                        }
                    } 
//                    else if (readThruEnabled && flagMap.IsBitSet(((byte) BitSetConstants.ReadThru))) {
                    else if (readThruEnabled) {
                        resyncIndexes[counter++] = i;
                    }
                }

                /**
                 * start resync operation only if there are some keys that
                 * failed to get and readthru is enabled
                 */
                if (readThruEnabled && counter > 0) {
                    if (providerName == null || providerName.equals("")) {
                        providerName = _context.getDsMgr().getDefaultReadThruProvider();
                    }
                    Object[] failedKeys = new Object[counter];
                    CacheEntry[] enteries = new CacheEntry[counter];
                    BitSet[] flags = new BitSet[counter];

                    for (int i = 0; i < counter; i++) {
                        int index = resyncIndexes[i];

                        failedKeys[i] = keys[index];
                        enteries[i] = (CacheEntry) ((table.get(keys[index]) instanceof CacheEntry) ? table.get(keys[index]) : null);
                        flags[i] = enteries[i] == null ? new BitSet() : enteries[i].getFlag();
                    }
                    OperationContext opContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                    if(isJCacheLoader)
                        opContext.Add(OperationContextFieldName.JCacheLoader, isJCacheLoader);
                    opContext.Add(OperationContextFieldName.ReplaceExistingValues, replaceExistingValues);                   
//                    _context.getDsMgr().ResyncCacheItem(table, failedKeys, enteries, flags, providerName, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation), isJCacheLoader);

                        _context.getDsMgr().ResyncCacheItem(table, failedKeys, enteries, flags, providerName, opContext, updateCounter);                   
                }
                
                getTime.EndSample();
            }
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Get()", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Get()", inner.toString());
            throw new OperationFailedException("Get operation failed. Error : " + inner.getMessage(), inner);
        }
        return table;
    }

    /**
     * Convert CompactCacheEntry to CacheEntry, CompactCacheEntry may be
     * serialized
     *
     * @param data
     * @return
     */
    private CacheEntry MakeCacheEntry(CompactCacheEntry cce, String serializationContext) {
        boolean isAbsolute = false;
        boolean isResync = false;
        int priority = CacheItemPriority.Normal.value();

        int opt = (int) cce.getOptions();

        if (opt != 255) {
            isAbsolute = (opt & 0x1) != 0 ? Boolean.TRUE : Boolean.FALSE;
            opt = (opt >> 1);
            isResync = (opt & 0x1) != 0 ? Boolean.TRUE : Boolean.FALSE;
            opt = (opt >> 1);
            priority = opt - 2;
        }

        ExpirationHint eh = ConvHelper.MakeExpirationHint(cce.getExpiration(), isAbsolute);

        if (eh != null && cce.getDependency() != null) {
            eh = new AggregateExpirationHint(cce.getDependency(), eh);
        }

        if (eh == null) {
            eh = cce.getDependency();
        }

        if (eh != null) {
            if (isResync) {
                eh.SetBit(ExpirationHint.NEEDS_RESYNC);
            }
        }

        CacheEntry e = new CacheEntry(cce.getValue(), eh, new PriorityEvictionHint(CacheItemPriority.forValue(priority)));
        if (cce.getGroup() != null && !cce.getGroup().isEmpty()) {
            e.setGroupInfo(new GroupInfo(cce.getGroup(), cce.getSubGroup()));
        }

        e.setQueryInfo(cce.getQueryInfo());
        e.setFlag(cce.getFlag());

        e.setLockId(cce.getLockId());
        e.setLockAccessType(cce.getLockAccessType());
        e.setVersion((long) cce.getVersion());
        e.setResyncProviderName(cce.getResyncProviderName());
        e.setProviderName(cce.getProviderName());
        e.setKeySize(CacheKeyUtil.getKeySize(cce.getKey(), serializationContext));
        return e;
    }

    /**
     * Add an ExpirationHint against given key
     *
     * @param key
     * @param hint
     * @return
     */
    public final boolean AddExpirationHint(Object key, ExpirationHint hint, OperationContext operationContext) throws Exception {
        if (!getIsRunning()) {
            return false;
        }

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(key, AllowedOperationType.AtomicWrite)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        try {
            return _context.getCacheImpl().Add(key, hint, operationContext);
        } catch (Exception ex) {
            _context.getCacheLog().Error("Add operation failed. Error: " + ex.toString());
            throw ex;
        }
    }

 

    /**
     * Add a CompactCacheEntry, it may be serialized
     *
     * @param entry
     */
    public final void AddEntry(Object entry, OperationContext operationContext) throws OperationFailedException {
        // check if cache is running.
        if (!getIsRunning()) {
            return;
        }
        CompactCacheEntry cce = null;

        cce = (CompactCacheEntry) entry;

        CacheEntry e = MakeCacheEntry(cce, _context.getSerializationContext());

       
        String group = null, subgroup = null;
        if (e.getGroupInfo() != null && e.getGroupInfo().getGroup() != null) {
            group = e.getGroupInfo().getGroup();
            subgroup = e.getGroupInfo().getSubGroup();
        }
        Add(cce.getKey(), e.getValue(), e.getExpirationHint(),   e.getEvictionHint(), group, subgroup, e.getQueryInfo(), e.getFlag(), cce.getProviderName(), e.getResyncProviderName(), operationContext, null);
    }

    /**
     * Basic Add operation, takes only the key and object as parameter.
     */
    public final void Add(Object key, Object value) throws OperationFailedException {
        Add(key, value, null, null, null, null, null, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
    }

    /**
     * Overload of Add operation. Uses an additional ExpirationHint parameter to
     * be used for Item Expiration Feature.
     */
    public final void Add(Object key, Object value, ExpirationHint expiryHint, OperationContext operationContext) throws OperationFailedException {
        Add(key, value, expiryHint, null, null, null, null, operationContext);
    }

    /**
     * Overload of Add operation. Uses an additional EvictionHint parameter to
     * be used for Item auto eviction policy.
     */
    public final void Add(Object key, Object value, EvictionHint evictionHint) throws OperationFailedException //#else
    {
        Add(key, value, null,  evictionHint, null, null, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
    }

    /**
     * Overload of Add operation. Uses additional EvictionHint and
     * ExpirationHint parameters.
     */
    public final void Add(Object key, Object value, ExpirationHint expiryHint,  EvictionHint evictionHint, String group, String subGroup, OperationContext operationContext) throws
            OperationFailedException {
        Add(key, value, expiryHint,  evictionHint, group, subGroup, null, operationContext);
    }

    /**
     * Overload of Add operation. Uses additional EvictionHint and
     * ExpirationHint parameters.
     */
    public final void Add(Object key, Object value, ExpirationHint expiryHint,  EvictionHint evictionHint, String group, String subGroup, java.util.HashMap queryInfo, OperationContext operationContext) throws
            OperationFailedException {
        Add(key, value, expiryHint,  evictionHint, group, subGroup, queryInfo, new BitSet(), null, null, operationContext, null);
    }

    /**
     * Overload of Add operation. uses additional paramer of Flag for checking
     * if compressed or not
     */
    public final void Add(Object key, Object value, ExpirationHint expiryHint,  EvictionHint evictionHint, String group, String subGroup, java.util.HashMap queryInfo, BitSet flag, String providerName, String resyncProviderName, OperationContext operationContext, HPTime bridgeOperationTime) throws
            OperationFailedException {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        if (value == null) {
            throw new IllegalArgumentException("value");
        }

        if ((expiryHint != null) && !(expiryHint instanceof Serializable)) {
            throw new IllegalArgumentException("expiryHint is not serializable");
        }
        if ((evictionHint != null) && !(evictionHint instanceof Serializable)) {
            throw new IllegalArgumentException("evictionHint is not serializable");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return;
        }
        DataSourceUpdateOptions updateOpts = this.UpdateOption(flag);
        this.CheckDataSourceAvailabilityAndOptions(updateOpts);

        GroupInfo grpInfo = null;
        if (!Common.isNullorEmpty(group)) {
            grpInfo = new GroupInfo(group, subGroup);
        }

        CacheEntry e = new CacheEntry(value, expiryHint, evictionHint);
        e.setGroupInfo(grpInfo);
        //Object size of Inproc
        Object dataSize = operationContext.GetValueByField(OperationContextFieldName.ValueDataSize);

        if (dataSize != null) {
            int tempSize = Integer.parseInt(dataSize.toString());
            e.setDataSize(tempSize);
        }

        e.setResyncProviderName(resyncProviderName);
        e.setProviderName(providerName);
        e.setQueryInfo(queryInfo);
//        if(_inProc)                             //jugaar
//        {e.getFlag().setData(flag.getData());}
//        else
        {e.getFlag().setData((byte) (e.getFlag().getData() | flag.getData()));}
      ;
        
        Object keySize = operationContext.GetValueByField(OperationContextFieldName.KeySize);
        if(keySize!=null)
        {
            e.setKeySize((Long)keySize);
            operationContext.RemoveValueByField(OperationContextFieldName.KeySize);
        }
        else
        {
            e.setKeySize(CacheKeyUtil.getKeySize(key, this.getName()));
        }

        try {
            HPTimeStats addTime = new HPTimeStats();
            _context.PerfStatsColl.mSecPerAddBeginSample();

            CacheEntry clone;
            if ((updateOpts == DataSourceUpdateOptions.WriteThru || updateOpts == DataSourceUpdateOptions.WriteBehind) && e.getHasQueryInfo()) {
                clone = (CacheEntry) e.clone();
            } else {
                clone = e;
            }
            
           
            addTime.BeginSample();
            Add(key, e, operationContext);
            addTime.EndSample();
            _context.PerfStatsColl.mSecPerAddEndSample();
            
             OperationResult dsResult = null;
            String taskId = new com.alachisoft.tayzgrid.caching.util.GUID().toString();
            if (updateOpts == DataSourceUpdateOptions.WriteThru) {
                dsResult = this._context.getDsMgr().WriteThru(key, clone, OpCode.Add, providerName, operationContext);
                if (dsResult != null && dsResult.getDSOperationStatus() == OperationResult.Status.FailureRetry) {
                    WriteOperation operation = dsResult.getOperation();
                    if (operation != null) {
                        _context.getCacheLog().Info("Retrying Write Operation" + operation.getOperationType() + " operation for key:" + operation.getKey());
                        //creating ds operation with previous entry
                        DSWriteBehindOperation dsOperation = new DSWriteBehindOperation(this.getName(), operation.getKey(), null, clone, OpCode.Add, providerName, 0, taskId, null, WriteBehindAsyncProcessor.TaskState.Execute);
                        _context.getCacheImpl().EnqueueDSOperation(dsOperation);
                    }
                }
                
            }
            
            else if (!_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind ) {
                this._context.getDsMgr().WriteBehind(_context.getCacheImpl(), key, clone, null, taskId, providerName, OpCode.Add, WriteBehindAsyncProcessor.TaskState.Execute);
            }
        } catch (Exception inner) {
            throw new OperationFailedException("Add operation failed" + ":" + inner.getMessage(), inner);
        }
    }

    /**
     * called from web cache to initiate the custom notifications.
     *
     * @param notifId
     * @param data
     * @param async
     */
    public final void SendNotification(Object notifId, Object data) throws Exception {
        // cache may have expired or not initialized.
        if (!getIsRunning()) {
            return;
        }

        if (notifId != null && !(notifId instanceof java.io.Serializable)) {
            throw new IllegalArgumentException("notifId is not serializable");
        }
        if (data != null && !(data instanceof java.io.Serializable)) {
            throw new IllegalArgumentException("data is not serializable");
        }

        try {
            _context.getCacheImpl().SendNotification(notifId, data);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     *
     *
     * @param entry
     */
    public final void AddAsyncEntry(Object entry, OperationContext operationContext) throws IOException, ClassNotFoundException {

        if (!getIsRunning()) {
            return;
        }

        CompactCacheEntry cce = (CompactCacheEntry) SerializationUtil.safeDeserialize(entry, _context.getSerializationContext(), null);

        boolean isAbsolute = false;
        boolean isResync = false;
        int priority = CacheItemPriority.Normal.value();

        int opt = (int) cce.getOptions();

        if (opt != 255) {
            isAbsolute = (opt & 1) != 0 ? Boolean.TRUE : Boolean.FALSE;
            opt = (opt >> 1);
            isResync = (opt & 1) != 0 ? Boolean.TRUE : Boolean.FALSE;
            opt = (opt >> 1);
            priority = opt - 2;
        }

        ExpirationHint eh = ConvHelper.MakeExpirationHint(cce.getExpiration(), isAbsolute);

        if (eh != null && cce.getDependency() != null) {
            eh = new AggregateExpirationHint(cce.getDependency(), eh);
        }

        if (eh == null) {
            eh = cce.getDependency();
        }

        if (eh != null) {
            if (isResync) {
                eh.SetBit(ExpirationHint.NEEDS_RESYNC);
            }
        }
        AddAsync(cce.getKey(), cce.getValue(), eh,  new PriorityEvictionHint(CacheItemPriority.forValue(priority)), cce.getGroup(), cce.getSubGroup(), cce.getFlag(), cce.getQueryInfo(), cce.getProviderName(), cce.getResyncProviderName(), operationContext);
    }

    public final void AddAsync(Object key, Object value, ExpirationHint expiryHint,  EvictionHint evictionHint, String group, String subGroup, OperationContext operationContext) throws
            IOException, ClassNotFoundException {
        AddAsync(key, value, expiryHint, evictionHint, group, subGroup, new BitSet(), null, null, null, operationContext);
    }

    /**
     * Overload of Add operation. Uses additional EvictionHint and
     * ExpirationHint parameters.
     */
    public final void AddAsync(Object key, Object value, ExpirationHint expiryHint,  EvictionHint evictionHint, String group, String subGroup, BitSet Flag, java.util.HashMap queryInfo, String providerName, String resyncProviderName, OperationContext operationContext) throws
            IOException, ClassNotFoundException {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        if (value == null) {
            throw new IllegalArgumentException("value");
        }

        if (!_inProc) {
            if (key instanceof byte[]) {
                key = SerializationUtil.safeDeserialize((byte[]) key, _context.getSerializationContext(), null);
            }

            if (value instanceof byte[]) {
                value = SerializationUtil.safeDeserialize((byte[]) value, _context.getSerializationContext(), null);
            }
        }

        if ((expiryHint != null) && !(expiryHint instanceof Serializable)) {
            throw new IllegalArgumentException("expiryHint is not not serializable");
        }

        if ((evictionHint != null) && !(evictionHint instanceof Serializable)) {
            throw new IllegalArgumentException("evictionHint is not serializable");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return;
        }
        _asyncProcessor.Enqueue(new AsyncAdd(this, key, value, expiryHint,  evictionHint, group, subGroup, Flag, queryInfo, providerName, resyncProviderName, operationContext));

    }

    /**
     * Internal Add operation. Does write-through as well.
     */
    private void Add(Object key, CacheEntry e, OperationContext operationContext) throws OperationFailedException {
        Object value = e.getValue();
        try {
            CacheAddResult result = CacheAddResult.Failure;

            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("Cache.Add", (String) ((key instanceof String) ? key : null));
            }

            Object block = null;
            Boolean isNoBlock = false;
            block = operationContext.GetValueByField(OperationContextFieldName.NoGracefulBlock);
            if (block != null) {
                isNoBlock = (Boolean) block;
            }

            if (!isNoBlock) {
                if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                    if (!_context.getCacheImpl().IsOperationAllowed(key, AllowedOperationType.AtomicWrite)) {
                        _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                    }
                }
            }

            result = _context.getCacheImpl().Add(key, e, true, operationContext);

            switch (result) {
                case Failure:
                    break;

                case NeedsEviction:
                    throw new OperationFailedException("The cache is full and not enough items could be evicted.", false);

                case KeyExists:
                    throw new OperationFailedException("The specified key already exists.", false);

                case Success:
                    _context.PerfStatsColl.incrementAddPerSecStats();
                    break;
            }
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Add():", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Add():", inner.toString());
            throw new OperationFailedException("Add operation failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Add array of CompactCacheEntry to cache, these may be serialized
     *
     * @param entries
     */
    public final java.util.Map AddEntries(Object[] entries, OperationContext operationContext) throws OperationFailedException, Exception {
        // check if cache is running.
        if (!getIsRunning()) {
            return null;
        }

        Object[] keys = new Object[entries.length];
        Object[] values = new Object[entries.length];
        CallbackEntry[] callbackEnteries = new CallbackEntry[entries.length];
        ExpirationHint[] exp = new ExpirationHint[entries.length];
        EvictionHint[] evc = new EvictionHint[entries.length];      
        BitSet[] flags = new BitSet[entries.length];
        java.util.HashMap[] queryInfo = new java.util.HashMap[entries.length];
        GroupInfo[] groupInfo = new GroupInfo[entries.length];

        CallbackEntry cbEntry = null;

        for (int i = 0; i < entries.length; i++) {
            CompactCacheEntry cce = (CompactCacheEntry) SerializationUtil.safeDeserialize(entries[i], _context.getSerializationContext(), null);
            Object tempVar = cce.getKey();
            keys[i] = tempVar;
            CacheEntry ce = MakeCacheEntry(cce, _context.getSerializationContext());
            if (ce != null) {
                if (ce.getValue() instanceof CallbackEntry) {
                    Object tempVar2 = ce.getValue();
                    {
                        cbEntry = (CallbackEntry) ((tempVar2 instanceof CallbackEntry) ? tempVar2 : null);
                    }
                }
            } else {
                cbEntry = null;
            }

            callbackEnteries[i] = cbEntry;

            Object tempVar3 = ce.getValue();
            Object value = (CallbackEntry) ((tempVar3 instanceof CallbackEntry) ? tempVar3 : null);
            values[i] = value == null ? ce.getValue() : ((CallbackEntry) ce.getValue()).getValue();

            exp[i] = ce.getExpirationHint();
            evc[i] = ce.getEvictionHint();
            queryInfo[i] = ce.getQueryInfo();
            flags[i] = ce.getFlag();
            groupInfo[i] = new GroupInfo(cce.getGroup(), cce.getSubGroup());
            GroupInfo gInfo = new GroupInfo(cce.getGroup(), cce.getSubGroup());

            groupInfo[i] = gInfo;
        }

        java.util.Map items = Add(keys, values, callbackEnteries, exp,  evc, groupInfo, queryInfo, flags, null, null, operationContext);
        if (items != null) {
            CompileReturnSet((HashMap) items);
        }
        return items;
    }

    private void CompileReturnSet(java.util.HashMap returnSet) {
        if (returnSet != null && returnSet.size() > 0) {
            java.util.HashMap tmp = (java.util.HashMap) returnSet.clone();
            Iterator ie = tmp.entrySet().iterator();
            Map.Entry keyPair;
            while (ie.hasNext()) {
                keyPair = (Map.Entry) ie.next();
                if (keyPair.getValue() instanceof OperationResult.Status) {
                    OperationResult.Status status = (OperationResult.Status) keyPair.getValue();
                    if (status == OperationResult.Status.Success || status == OperationResult.Status.FailureRetry) {
                        returnSet.remove(keyPair.getKey());
                    }
                } else if (keyPair.getValue() instanceof Exception) {
                    returnSet.put(keyPair.getKey(), new OperationFailedException(((Exception) keyPair.getValue()).getMessage()));
                }
            }

        }
    }

    /**
     * Overload of Add operation for bulk additions. Uses additional
     * EvictionHint and ExpirationHint parameters.
     *
     * @param keys
     * @param values
     * @param expiryHint
     * @param evictionHint
     * @param group
     * @param subGroup
     * @param operationContext
     * @return
     * @throws java.lang.Exception
     */
    public final java.util.Map Add(Object[] keys, Object[] values, ExpirationHint expiryHint, EvictionHint evictionHint, String group, String subGroup, OperationContext operationContext) throws
            Exception {
        if (keys == null) {
            throw new IllegalArgumentException("keys");
        }
        if (values == null) {
            throw new IllegalArgumentException("items");
        }
        if (keys.length != values.length) {
            throw new IllegalArgumentException("keys count is not equal to values count");
        }
        
        long[] keySizes = null;
        Object ks = operationContext.GetValueByField(OperationContextFieldName.KeySize);
        if(ks!=null)
        {
            keySizes =(long[])ks; 
            operationContext.RemoveValueByField(OperationContextFieldName.KeySize);
        }
        
                
        CacheEntry[] ce = new CacheEntry[values.length];

        for (int i = 0; i < values.length; i++) {
            Object key = keys[i];
            Object value = values[i];

            if (key == null) {
                throw new IllegalArgumentException("key");
            }
            if (value == null) {
                throw new IllegalArgumentException("value");
            }

            if ((expiryHint != null) && !(expiryHint instanceof Serializable)) {
                throw new IllegalArgumentException("expiryHint is not serializable");
            }
            if ((evictionHint != null) && !(evictionHint instanceof Serializable)) {
                throw new IllegalArgumentException("evictionHint is not serializable");
            }

            // Cache has possibly expired so do default.
            if (!getIsRunning()) {
                return null;
            }
            ce[i] = new CacheEntry(value, expiryHint, evictionHint);
            GroupInfo grpInfo = null;
            if (!Common.isNullorEmpty(group)) {
                grpInfo = new GroupInfo(group, subGroup);
            }

            ce[i].setGroupInfo(grpInfo);
            if (keySizes != null)
            {
                ce[i].setKeySize(keySizes[i]);
            }
            else
            {
                ce[i].setKeySize(CacheKeyUtil.getKeySize(keys, this.getName()));
            }
        }

        try {
            return Add(keys, ce, operationContext);
        } catch (Exception inner) {
            throw inner;
        }

    }

    /**
     * Overload of Add operation for bulk additions. Uses EvictionHint and
     * ExpirationHint arrays.
     *
     * @param keys
     * @param values
     * @param callbackEnteries
     * @param queryInfos
     * @param expirations
     * @param flags
     * @param evictions
     * @param syncDependencies
     * @param groupInfos
     * @param providerName
     * @param resyncProviderName
     * @param operationContext
     * @return
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     * @throws java.lang.Exception
     */
    public final java.util.Map Add(Object[] keys, Object[] values, CallbackEntry[] callbackEnteries, ExpirationHint[] expirations,  EvictionHint[] evictions, GroupInfo[] groupInfos, java.util.HashMap[] queryInfos, BitSet[] flags, String providerName, String resyncProviderName, OperationContext operationContext) throws
            OperationFailedException, Exception {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("Cache.InsertBlk", "");
        }

        if (keys == null) {
            throw new IllegalArgumentException("keys");
        }
        if (values == null) {
            throw new IllegalArgumentException("items");
        }
        if (keys.length != values.length) {
            throw new IllegalArgumentException("keys count is not equal to values count");
        }
        DataSourceUpdateOptions updateOpts = this.UpdateOption(flags[0]);
        this.CheckDataSourceAvailabilityAndOptions(updateOpts);

        CacheEntry[] enteries = new CacheEntry[values.length];
        //Object size of inproc
        Object dataSize = operationContext.GetValueByField(OperationContextFieldName.ValueDataSize);

        long[] sizes = null;
        if (dataSize != null) {
            sizes = new long[keys.length];
            int count = 0;
            for (long sizeValue : (long[]) dataSize) {

                sizes[count] = sizeValue;
                count++;
            }
        }
        
        long[] keySizes = null;
        Object ks = operationContext.GetValueByField(OperationContextFieldName.KeySize);
        if(ks!=null)
        {
            keySizes =(long[])ks; 
            operationContext.RemoveValueByField(OperationContextFieldName.KeySize);
        }

        for (int i = 0; i < values.length; i++) {
            if (keys[i] == null) {
                throw new IllegalArgumentException("key");
            }
            if (values[i] == null) {
                throw new IllegalArgumentException("value");
            }
            
            if ((expirations[i] != null) && !(expirations[i] instanceof Serializable)) {
                throw new IllegalArgumentException("expiryHint is not serializable");
            }
            if ((evictions[i] != null) && !(evictions[i] instanceof Serializable)) {
                throw new IllegalArgumentException("evictionHint is not serializable");
            }

            // Cache has possibly expired so do default.
            if (!getIsRunning()) {
                return null;
            }

            enteries[i] = new CacheEntry(values[i], expirations[i], evictions[i]);
         

            if(groupInfos[i] != null && groupInfos[i].getGroup()!=null && !groupInfos[i].getGroup().isEmpty()){

                enteries[i].setGroupInfo(groupInfos[i]);
            }

            enteries[i].setQueryInfo(queryInfos[i]);
            enteries[i].getFlag().setData((byte) (enteries[i].getFlag().getData() | flags[i].getData()));
            enteries[i].setResyncProviderName(resyncProviderName);
            enteries[i].setProviderName(providerName);
            if (sizes != null) {
                enteries[i].setDataSize(sizes[i]);
            }
            if (keySizes != null)
            {
                enteries[i].setKeySize(keySizes[i]);
            }
            else
            {                
                enteries[i].setKeySize(CacheKeyUtil.getKeySize(keys, this.getName()));
            }
            
            if (callbackEnteries[i] != null) {
                Object tempVar = callbackEnteries[i].clone();
                CallbackEntry cloned = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null);
                cloned.setValue(values[i]);
                cloned.setFlag(enteries[i].getFlag());
                enteries[i].setValue(cloned);
            }
        }

        try {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(keys, AllowedOperationType.BulkWrite, operationContext)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }

            java.util.Map result;
            HPTimeStats addTime = new HPTimeStats();

            CacheEntry[] clone = null;
            if (updateOpts == DataSourceUpdateOptions.WriteBehind || updateOpts == DataSourceUpdateOptions.WriteThru) {
                clone = new CacheEntry[enteries.length];
                for (int i = 0; i < enteries.length; i++) {
                    if (enteries[i].getHasQueryInfo()) {
                        clone[i] = (CacheEntry) enteries[i].clone();
                    } else {
                        clone[i] = enteries[i];
                    }
                }
            }

            addTime.BeginSample();
            result = Add(keys, enteries, operationContext);
            addTime.EndSample();

            Object[] filteredKeys = null;
            Object[] filteredValues = null;
            if (updateOpts != DataSourceUpdateOptions.None && keys.length > result.size()) {
                filteredKeys = new Object[keys.length - result.size()];
                filteredValues = new Object[keys.length - result.size()];

                for (int i = 0, j = 0; i < keys.length; i++) {
                    if (!result.containsKey(keys[i])) {
                        filteredKeys[j] = keys[i];
                        if (!_inProc) {
                            UserBinaryObject ubObject = (UserBinaryObject) ((values[i] instanceof UserBinaryObject) ? values[i] : null);
                            }
                        j++;
                    }
                }

                OperationResult[] dsResults = null;
                String taskId = (new com.alachisoft.tayzgrid.caching.util.GUID()).toString();
                if (updateOpts == DataSourceUpdateOptions.WriteThru ) {
                    dsResults = this._context.getDsMgr().WriteThru(filteredKeys, filteredValues, clone, (java.util.HashMap) ((result instanceof java.util.HashMap) ? result : null), OpCode.Add, providerName, operationContext);
                    if (dsResults != null) {
                        EnqueueRetryOperations(filteredKeys, filteredValues, clone, (java.util.HashMap) ((result instanceof java.util.HashMap) ? result : null), OpCode.Add, providerName, taskId, operationContext);
                    }
                } else if (!_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind) {
                    this._context.getDsMgr().WriteBehind(_context.getCacheImpl(), filteredKeys, filteredValues, clone, null, taskId, providerName, OpCode.Add, WriteBehindAsyncProcessor.TaskState.Execute);
                }
            }
            return result;
        } catch (Exception inner) {
            throw inner;
        }
    }

    /**
     * For operations,which needs to be retried.
     */
    private void EnqueueRetryOperations(Object[] keys, Object[] values, CacheEntry[] entries, java.util.HashMap returnSet, OpCode opCode, String providerName, String taskId, OperationContext operationContext) throws Exception {
        java.util.ArrayList operations = new java.util.ArrayList();
        DSWriteBehindOperation dsOperation = null;
        if (values == null) {
            values = new Object[keys.length];
        }
        
        if(entries == null) {
            entries = new CacheEntry[keys.length];
        }
        for (int i = 0; i < keys.length; i++) {
            if (returnSet.containsKey(keys[i]) && returnSet.get(keys[i]) instanceof OperationResult.Status) {
                OperationResult.Status status = (OperationResult.Status) returnSet.get(keys[i]);
                if (status == OperationResult.Status.FailureRetry) {
                    _context.getCacheLog().Info("Retrying Write Operation: " + opCode + " for key:" + keys[i]);
                    dsOperation = new DSWriteBehindOperation(this.getName(), keys[i], values[i], entries[i], opCode, providerName, 0, taskId, null, WriteBehindAsyncProcessor.TaskState.Execute);
                    operations.add(dsOperation);
                }
            }
        }
        if (operations.size() > 0) {
            _context.getCacheImpl().EnqueueDSOperation(operations);
        }
    }

    /**
     * Internal Add operation for bulk additions. Does write-through as well.
     */
    private java.util.HashMap Add(Object[] keys, CacheEntry[] entries, OperationContext operationContext) throws OperationFailedException {
        try {
            java.util.HashMap result = new java.util.HashMap();
            result = _context.getCacheImpl().Add(keys, entries, true, operationContext);
            if (result != null) {
                //there is a chance that all the keys could not be added to the
                //cache successfully. so remove dependency for failed keys.

                java.util.HashMap tmp = (java.util.HashMap) result.clone();
                Iterator ide = tmp.entrySet().iterator();
                while (ide.hasNext()) {
                    Map.Entry entry = (Map.Entry) ide.next();
                    CacheAddResult addResult = CacheAddResult.Failure;
                    if (entry.getValue() instanceof CacheAddResult) {
                        addResult = (CacheAddResult) entry.getValue();
                        switch (addResult) {
                            case Failure:
                                break;
                            case KeyExists:
                                result.put(entry.getKey(), new OperationFailedException("The specified key already exists."));
                                break;
                            case NeedsEviction:
                                result.put(entry.getKey(), new OperationFailedException("The cache is full and not enough items could be evicted."));
                                break;
                            case Success:
                                result.remove(entry.getKey());
                                break;
                        }
                    }
                }
            }
            return result;
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Add():", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Add():", inner.toString());
            throw new OperationFailedException("Add operation failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Insert a CompactCacheEntry, it may be serialized
     *
     * @param entry
     */
    public final InsertResult InsertEntry(Object entry, OperationContext operationContext) throws OperationFailedException, Exception {
        if (!getIsRunning()) {
            return null;
        }

        CompactCacheEntry cce = null;

        cce = (CompactCacheEntry) entry;

        CacheEntry e = MakeCacheEntry(cce, _context.getSerializationContext());

        String group = null, subgroup = null;
        if (e.getGroupInfo() != null && e.getGroupInfo().getGroup() != null) {
            group = e.getGroupInfo().getGroup();
            subgroup = e.getGroupInfo().getSubGroup();
        }
        return Insert(cce.getKey(), e.getValue(), e.getExpirationHint(),  e.getEvictionHint(), group, subgroup, e.getQueryInfo(), e.getFlag(), e.getLockId(), e.getVersion(), e.getLockAccessType(), e.getProviderName(), e.getResyncProviderName(), operationContext);
    }

    /**
     * Basic Insert operation, takes only the key and object as parameter.
     */
    public final long Insert(Object key, Object value) throws OperationFailedException {
        try {
            InsertResult result = Insert(key, value, null, null, null, null, null,  new BitSet(), null, 0, LockAccessType.IGNORE_LOCK, null, null, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation));
            if(result != null)
                return result.Version;
            return 0;

        } catch (Exception inner) {
            throw new OperationFailedException(inner);
        }
    }

    /**
     * Overload of Insert operation. Uses additional EvictionHint and
     * ExpirationHint parameters.
     */

    public final InsertResult Insert(Object key, Object value, ExpirationHint expiryHint,  EvictionHint evictionHint, String group, String subGroup, java.util.HashMap queryInfo, BitSet flag, Object lockId, long version, LockAccessType accessType, String providerName, String resyncProviderName, OperationContext operationContext) throws

            OperationFailedException, Exception {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("Cache.Insert", "");
        }
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        if (value == null) {
            throw new IllegalArgumentException("value");
        }

        if ((expiryHint != null) && !(expiryHint instanceof Serializable)) {
            throw new IllegalArgumentException("expiryHint is not not serializable");
        }
        if ((evictionHint != null) && !(evictionHint instanceof Serializable)) {
            throw new IllegalArgumentException("evictionHint is not serializable");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }
        GroupInfo grpInfo = null;
        DataSourceUpdateOptions updateOpts = this.UpdateOption(flag);

        this.CheckDataSourceAvailabilityAndOptions(updateOpts);
        if (!Common.isNullorEmpty(group)) {
            grpInfo = new GroupInfo(group, subGroup);
        }

        CacheEntry e = new CacheEntry(value, expiryHint, evictionHint);
        e.setGroupInfo(grpInfo);
        e.setQueryInfo(queryInfo);
//        if(_inProc)                             //jugaar
//        {e.getFlag().setData(flag.getData());}
//        else
        {e.getFlag().setData((byte) (e.getFlag().getData() | flag.getData()));}
        e.setResyncProviderName(resyncProviderName);
        e.setProviderName(providerName);
       
        Object dataSize = operationContext.GetValueByField(OperationContextFieldName.ValueDataSize);

        if (dataSize != null) {
            int tempSize = Integer.parseInt(dataSize.toString());
            e.setDataSize(tempSize);
        }
        
        Object keySize = operationContext.GetValueByField(OperationContextFieldName.KeySize);
        if(keySize!=null)
        {
            e.setKeySize((Long)keySize);
            operationContext.RemoveValueByField(OperationContextFieldName.KeySize);
        }
        else
        {
            e.setKeySize(CacheKeyUtil.getKeySize(key, this.getName()));
        }
        /**
         * update the counters for various statistics
         */
        InsertResult result;
        try {
            CacheEntry clone;
            if ((updateOpts == DataSourceUpdateOptions.WriteThru || updateOpts == DataSourceUpdateOptions.WriteBehind) && e.getHasQueryInfo()) {
                clone = (CacheEntry) e.clone();
            } else {
                clone = e;
            }

            _context.PerfStatsColl.mSecPerUpdBeginSample();
            result = Insert(key, e, lockId, version, accessType, operationContext);
            _context.PerfStatsColl.mSecPerUpdEndSample();
            
            boolean doWriteThru = true;
            if(operationContext.Contains(OperationContextFieldName.InsertParams)) {
                InsertParams insertParams = (InsertParams)operationContext.GetValueByField(OperationContextFieldName.InsertParams);
                if(insertParams.IsReplaceOperation && !result.Success)
                    doWriteThru = false;
            }
            
            OperationResult dsResult = null;
            String taskId = new com.alachisoft.tayzgrid.caching.util.GUID().toString();
            if (updateOpts == DataSourceUpdateOptions.WriteThru && doWriteThru) {
                dsResult = this._context.getDsMgr().WriteThru(key, clone, OpCode.Update, providerName, operationContext);
                if (dsResult != null && dsResult.getDSOperationStatus() == OperationResult.Status.FailureRetry) {
                    WriteOperation operation = dsResult.getOperation();
                    if (operation != null) {
                        _context.getCacheLog().Info("Retrying Write Operation" + operation.getOperationType() + " operation for key:" + operation.getKey());
                        //creating ds operation with with previous entry
                        DSWriteBehindOperation dsOperation = new DSWriteBehindOperation(this.getName(), operation.getKey(), null, clone, OpCode.Update, providerName, 0, taskId, null, WriteBehindAsyncProcessor.TaskState.Execute);
                        _context.getCacheImpl().EnqueueDSOperation(dsOperation);
                    }
                }
            } else if (!_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind ) {
                this._context.getDsMgr().WriteBehind(_context.getCacheImpl(), key, clone, null, taskId, providerName, OpCode.Update, WriteBehindAsyncProcessor.TaskState.Execute);
            }

        } catch (Exception inner) {
            throw inner;
        }

        return result;
    }

    /**
     * Return update option type set in the flag
     *
     */
    private DataSourceUpdateOptions UpdateOption(BitSet flag) {
        if (flag.IsBitSet((byte) BitSetConstants.WriteThru)) {
            if(flag.IsBitSet((byte) BitSetConstants.OptionalDSOperation) && (this._context.getDsMgr() == null || !_context.getDsMgr().getIsWriteThruEnabled()))
                return DataSourceUpdateOptions.None;
                
            return DataSourceUpdateOptions.WriteThru;
        } else if (flag.IsBitSet((byte) BitSetConstants.WriteBehind)) {
            return DataSourceUpdateOptions.WriteBehind;
        } else {
            return DataSourceUpdateOptions.None;
        }
    }
    // Resolving client-provided Read Options for ReadThru
    private DataSourceReadOptions ReadOption(BitSet flag) {
        if(flag.IsBitSet((byte)BitSetConstants.ReadThru)) {
            if(flag.IsBitSet((byte)BitSetConstants.OptionalDSOperation) && 
                    (this._context.getDsMgr() == null || !this._context.getDsMgr().getIsReadThruEnabled()))
                return DataSourceReadOptions.None;
            if(flag.IsBitSet((byte)BitSetConstants.OptionalDSOperation) && 
                    (this._context.getDsMgr() != null && this._context.getDsMgr().getIsReadThruEnabled()))
                return DataSourceReadOptions.OptionalReadThru;
            return DataSourceReadOptions.ReadThru; 
        }
        else
            return DataSourceReadOptions.None;     
    }

    /**
     *
     *
     * @param entry
     * @param operationContext
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final void InsertAsyncEntry(Object entry, OperationContext operationContext) throws OperationFailedException {
        if (!getIsRunning()) {
            return;
        }

        CompactCacheEntry cce = null;
        try {
            cce = (CompactCacheEntry) SerializationUtil.safeDeserialize(entry, _context.getSerializationContext(), null);
        } catch (IOException iOException) {
            throw new OperationFailedException(iOException);
        } catch (ClassNotFoundException classNotFoundException) {
            throw new OperationFailedException(classNotFoundException);
        }

        boolean isAbsolute = false;
        boolean isResync = false;
        int priority = CacheItemPriority.Normal.value();

        int opt = (int) cce.getOptions();

        if (opt != 255) {
            isAbsolute = (opt & 1) != 0 ? Boolean.TRUE : Boolean.FALSE;
            opt = (opt >> 1);
            isResync = (opt & 1) != 0 ? Boolean.TRUE : Boolean.FALSE;
            opt = (opt >> 1);
            priority = opt - 2;
        }

        ExpirationHint eh = ConvHelper.MakeExpirationHint(cce.getExpiration(), isAbsolute);

        if (eh != null && cce.getDependency() != null) {
            eh = new AggregateExpirationHint(cce.getDependency(), eh);
        }

        if (eh == null) {
            eh = cce.getDependency();
        }

        if (eh != null) {
            if (isResync) {
                eh.SetBit(ExpirationHint.NEEDS_RESYNC);
            }
        }

        InsertAsync(cce.getKey(), cce.getValue(), eh, new PriorityEvictionHint(CacheItemPriority.forValue(priority)), cce.getGroup(), cce.getSubGroup(), cce.getFlag(), cce.getQueryInfo(), cce.getProviderName(), cce.getResyncProviderName(), operationContext);
    }

    public final void InsertAsync(Object key, Object value, ExpirationHint expiryHint, EvictionHint evictionHint, String group, String subGroup, OperationContext operationContext) {
        InsertAsync(key, value, expiryHint,  evictionHint, group, subGroup, new BitSet(), null, null, null, operationContext);
    }

    /**
     * Overload of Insert operation. Uses additional EvictionHint and
     * ExpirationHint parameters.
     *
     * @param key
     * @param resyncProviderName
     * @param value
     * @param subGroup
     * @param expiryHint
     * @param evictionHint
     * @param queryInfo
     * @param group
     * @param Flag
     * @param providerName
     * @param operationContext
     */
    public final void InsertAsync(Object key, Object value, ExpirationHint expiryHint,  EvictionHint evictionHint, String group, String subGroup, BitSet Flag, java.util.HashMap queryInfo, String providerName, String resyncProviderName, OperationContext operationContext) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        if (value == null) {
            throw new IllegalArgumentException("value");
        }

        if ((expiryHint != null) && !(expiryHint instanceof Serializable)) {
            throw new IllegalArgumentException("expiryHint is not not serializable");
        }
        if ((evictionHint != null) && !(evictionHint instanceof Serializable)) {
            throw new IllegalArgumentException("evictionHint is not serializable");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return;
        }
        _asyncProcessor.Enqueue(new AsyncInsert(this, key, value, expiryHint,  evictionHint, group, subGroup, Flag, queryInfo, providerName, resyncProviderName, operationContext));
    }

    /**
     * Internal Insert operation. Does a write thru as well.
     */
    private InsertResult Insert(Object key, CacheEntry e, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException {

        HPTimeStats insertTime = new HPTimeStats();
        insertTime.BeginSample();

        InsertResult result = new InsertResult();
        Object value = e.getValue();
        try {;
            CacheInsResultWithEntry retVal = CascadedInsert(key, e, true, lockId, version, accessType, operationContext);
            insertTime.EndSample();

            switch (retVal.getResult()) {
                case Failure:
                    break;

                case NeedsEviction:
                case NeedsEvictionNotRemove:
                    throw new OperationFailedException("The cache is full and not enough items could be evicted.", false);

                case SuccessOverwrite:
                    _context.PerfStatsColl.incrementUpdPerSecStats();
                    long itemVersion = retVal.getEntry() == null ? 1 : retVal.getEntry().getVersion() + 1;
                    if(retVal.getEntry()!= null)
                    {
                        CompressedValueEntry cve = new CompressedValueEntry();
                        //cve.Value = e.getValue();
                    //    cve.Flag = e.getFlag();
                        cve.Value = retVal.getEntry().getValue();
                        cve.Flag = retVal.getEntry().getFlag();
                        result.ExistingValue = cve;
                        
                    }
                    result.Version = itemVersion;
                    result.Success = true;                        
                    break;

                case Success:
                    _context.PerfStatsColl.incrementAddPerSecStats();
                    itemVersion = retVal.getEntry() == null ? 1 : retVal.getEntry().getVersion();
                    if(retVal.getEntry()!= null)
                    {
                        CompressedValueEntry cve = new CompressedValueEntry();
                        cve.Value = e.getValue();
                        cve.Flag = e.getFlag();
                        result.ExistingValue = cve;
                        
                    }
                    result.Version = itemVersion;
                    result.Success = true;
                    break;

                case IncompatibleGroup:
                    throw new OperationFailedException("Data group of the inserted item does not match the existing item's data group.");

                case ItemLocked:
                    throw new LockingException("Item is locked.");

                case VersionMismatch:
                    throw new LockingException("Item does not exist at the specified version.");
            }
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Insert():", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Insert():", inner.toString());
            throw new OperationFailedException("Insert operation failed. Error : " + inner.getMessage(), inner);
        }
        return result;
    }

    /**
     * Insert array of CompactCacheEntry to cache, these may be serialized
     *
     * @param entries
     */
    public final java.util.Map InsertEntries(Object[] entries, OperationContext operationContext) throws OperationFailedException, Exception {
        // check if cache is running.
        if (!getIsRunning()) {
            return null;
        }

        Object[] keys = new Object[entries.length];
        Object[] values = new Object[entries.length];
        CallbackEntry[] callbackEnteries = new CallbackEntry[entries.length]; //Asif Imam
        ExpirationHint[] exp = new ExpirationHint[entries.length];
        EvictionHint[] evc = new EvictionHint[entries.length];
        BitSet[] flags = new BitSet[entries.length];
        java.util.HashMap[] queryInfo = new java.util.HashMap[entries.length];
        GroupInfo[] groupInfos = new GroupInfo[entries.length];
        CallbackEntry cbEntry = null;

        for (int i = 0; i < entries.length; i++) {
            CompactCacheEntry cce = (CompactCacheEntry) SerializationUtil.safeDeserialize(entries[i], _context.getSerializationContext(), null);
            Object tempVar = cce.getKey();
            keys[i] = tempVar;
            CacheEntry ce = MakeCacheEntry(cce, _context.getSerializationContext());
            if (ce != null) {
                if (ce.getValue() instanceof CallbackEntry) {
                    Object tempVar2 = ce.getValue();
                    {
                        cbEntry = (CallbackEntry) ((tempVar2 instanceof CallbackEntry) ? tempVar2 : null);
                    }
                }
            } else {
                cbEntry = null;
            }

            callbackEnteries[i] = cbEntry;

            Object tempVar3 = ce.getValue();
            Object value = (CallbackEntry) ((tempVar3 instanceof CallbackEntry) ? tempVar3 : null);
            values[i] = value == null ? ce.getValue() : ((CallbackEntry) ce.getValue()).getValue();

            exp[i] = ce.getExpirationHint();
            evc[i] = ce.getEvictionHint();
            queryInfo[i] = ce.getQueryInfo();
            groupInfos[i] = ce.getGroupInfo();
            flags[i] = ce.getFlag();
        }
        Map items = Insert(keys, values, callbackEnteries, exp, evc, groupInfos, queryInfo, flags, null, null, operationContext);
        if (items != null) {
            CompileReturnSet((HashMap) items);
        }
        return items;
    }

    /**
     * Overload of Insert operation for bulk inserts. Uses an additional
     * ExpirationHint parameter to be used for Item Expiration Feature.
     *
     * @param keys
     * @param values
     * @param expiryHint
     * @param operationContext
     * @return
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final java.util.Map Insert(Object[] keys, Object[] values, ExpirationHint expiryHint, OperationContext operationContext) throws OperationFailedException {
        return Insert(keys, values, expiryHint, null, null, null, operationContext);
    }

    /**
     * Overload of Insert operation for bulk inserts. Uses an additional
     * EvictionHint parameter to be used for Item auto eviction policy.
     *
     * @param keys
     * @param values
     * @param evictionHint
     * @param operationContext
     * @return
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final java.util.Map Insert(Object[] keys, Object[] values, EvictionHint evictionHint, OperationContext operationContext) throws OperationFailedException {
        return Insert(keys, values, null, evictionHint, null, null, operationContext);
    }

    /**
     * Overload of Insert operation for bulk inserts. Uses additional
     * EvictionHint and ExpirationHint parameters.
     *
     * @param keys
     * @param values
     * @param expiryHint
     * @param evictionHint
     * @param group
     * @param subGroup
     * @param operationContext
     * @return
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final java.util.Map Insert(Object[] keys, Object[] values, ExpirationHint expiryHint, EvictionHint evictionHint, String group, String subGroup, OperationContext operationContext) throws
            OperationFailedException {

        if (keys == null) {
            throw new IllegalArgumentException("keys");
        }
        if (values == null) {
            throw new IllegalArgumentException("items");
        }
        if (keys.length != values.length) {
            throw new IllegalArgumentException("keys count is not equal to values count");
        }

        long[] keySizes = null;
        Object ks = operationContext.GetValueByField(OperationContextFieldName.KeySize);
        if(ks!=null)
        {
            keySizes =(long[])ks; 
            operationContext.RemoveValueByField(OperationContextFieldName.KeySize);
        }
        
        CacheEntry[] ce = new CacheEntry[values.length];

        for (int i = 0; i < values.length; i++) {
            Object key = keys[i];
            Object value = values[i];

            if (key == null) {
                throw new IllegalArgumentException("key");
            }
            if (value == null) {
                throw new IllegalArgumentException("value");
            }

            if ((expiryHint != null) && !(expiryHint instanceof Serializable)) {
                throw new IllegalArgumentException("expiryHint is not not serializable");
            }
            if ((evictionHint != null) && !(evictionHint instanceof Serializable)) {
                throw new IllegalArgumentException("evictionHint is not serializable");
            }

            // Cache has possibly expired so do default.
            if (!getIsRunning()) {
                return null;
            }

            ce[i] = new CacheEntry(value, expiryHint, evictionHint);
            GroupInfo grpInfo = null;
            if (!Common.isNullorEmpty(group)) {
                grpInfo = new GroupInfo(group, subGroup);
            }

            ce[i].setGroupInfo(grpInfo);
            if (keySizes != null)
            {
                ce[i].setKeySize(keySizes[i]);
            }
            else
            {                
                ce[i].setKeySize(CacheKeyUtil.getKeySize(keys, this.getName()));
            }
        }
        /**
         * update the counters for various statistics
         */
        try {
            return Insert(keys, ce, operationContext);
        } catch (Exception inner) {
            throw new OperationFailedException(inner);
        }
    }

    /**
     * Overload of Insert operation for bulk inserts. Uses EvictionHint and
     * ExpirationHint arrays.
     *
     * @param keys
     * @param values
     * @param callbackEnteries
     * @param expirations
     * @param syncDependencies
     * @param evictions
     * @param groupInfos
     * @param queryInfos
     * @param flags
     * @param providername
     * @param resyncProviderName
     * @param operationContext
     * @return
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final java.util.Map Insert(Object[] keys, Object[] values, CallbackEntry[] callbackEnteries, ExpirationHint[] expirations, EvictionHint[] evictions, GroupInfo[] groupInfos, java.util.HashMap[] queryInfos, BitSet[] flags, String providername, String resyncProviderName, OperationContext operationContext) throws
            OperationFailedException, Exception {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("Cache.InsertBlk", "");
        }

        if (keys == null) {
            throw new IllegalArgumentException("keys");
        }
        if (values == null) {
            throw new IllegalArgumentException("items");
        }
        if (keys.length != values.length) {
            throw new IllegalArgumentException("keys count is not equal to values count");
        }

        DataSourceUpdateOptions updateOpts = this.UpdateOption(flags[0]);
        this.CheckDataSourceAvailabilityAndOptions(updateOpts);
        CacheEntry[] ce = new CacheEntry[values.length];

        //for object size in inproc
        Object dataSize = operationContext.GetValueByField(OperationContextFieldName.ValueDataSize);
        long[] sizes = null;
        if (dataSize != null) {
            sizes = new long[keys.length];
            int count = 0;
            for (long sizeValue : (long[]) dataSize) {

                sizes[count] = sizeValue;
                count++;
            }
        }
        
        long[] keySizes = null;
        Object ks = operationContext.GetValueByField(OperationContextFieldName.KeySize);
        if(ks!=null)
        {
            keySizes =(long[])ks; 
            operationContext.RemoveValueByField(OperationContextFieldName.KeySize);
        }

        for (int i = 0; i < values.length; i++) {

            if (keys[i] == null) {
                throw new IllegalArgumentException("key");
            }
            if (values[i] == null) {
                throw new IllegalArgumentException("value");
            }

            if ((expirations[i] != null) && !(expirations[i] instanceof Serializable)) {
                throw new IllegalArgumentException("expiryHint is not not serializable");
            }
            if ((evictions[i] != null) && !(evictions[i] instanceof Serializable)) {
                throw new IllegalArgumentException("evictionHint is not serializable");
            }

            // Cache has possibly expired so do default.
            if (!getIsRunning()) {
                return null;
            }

            ce[i] = new CacheEntry(values[i], expirations[i], evictions[i]);
          
            if(groupInfos[i] != null && groupInfos[i].getGroup()!=null && !groupInfos[i].getGroup().isEmpty())
              ce[i].setGroupInfo(groupInfos[i]);
            

            ce[i].setQueryInfo(queryInfos[i]);
            ce[i].getFlag().setData((byte) (ce[i].getFlag().getData() | flags[i].getData()));
            ce[i].setProviderName(providername);
            if (sizes != null) {
                ce[i].setDataSize(sizes[i]);
            }
            if (keySizes != null)
            {
                ce[i].setKeySize(keySizes[i]);
            }
            else
            {
                ce[i].setKeySize(CacheKeyUtil.getKeySize(keys, this.getName()));
            }
            
            if (callbackEnteries[i] != null) {
                Object tempVar = callbackEnteries[i].clone();
                CallbackEntry cloned = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null);
                cloned.setValue(values[i]);
                cloned.setFlag(ce[i].getFlag());
                ce[i].setValue(cloned);
            }
        }

        try {
            CacheEntry[] clone = null;
            if (updateOpts == DataSourceUpdateOptions.WriteBehind || updateOpts == DataSourceUpdateOptions.WriteThru) {
                clone = new CacheEntry[ce.length];
                for (int i = 0; i < ce.length; i++) {
                    if (ce[i].getHasQueryInfo()) {
                        clone[i] = (CacheEntry) ce[i].clone();
                    } else {
                        clone[i] = ce[i];
                    }
                }
            }

            HPTimeStats insertTime = new HPTimeStats();
            insertTime.BeginSample();

            java.util.Map result = Insert(keys, ce, operationContext);

            insertTime.EndSample();

            Object[] filteredKeys = null;
            Object[] filteredValues = null;

            if (updateOpts != DataSourceUpdateOptions.None && keys.length > result.size()) {
                filteredKeys = new Object[keys.length - result.size()];
                filteredValues = new Object[keys.length - result.size()];

                for (int i = 0, j = 0; i < keys.length; i++) {
                    if (!result.containsKey(keys[i])) {
                        filteredKeys[j] = keys[i];
                        if (!_inProc) {
                            UserBinaryObject ubObject = (UserBinaryObject) ((values[i] instanceof UserBinaryObject) ? values[i] : null);
                            
                            }
                        j++;
                    }
                }

                OperationResult[] dsResults = null;
                String taskId = new com.alachisoft.tayzgrid.caching.util.GUID().toString();
                if (updateOpts == DataSourceUpdateOptions.WriteThru ) {
                    dsResults = this._context.getDsMgr().WriteThru(filteredKeys, filteredValues, clone, (java.util.HashMap) ((result instanceof java.util.HashMap) ? result : null), OpCode.Update, providername, operationContext);
                    if (dsResults != null) {
                        EnqueueRetryOperations(filteredKeys, filteredValues, clone, (java.util.HashMap) ((result instanceof java.util.HashMap) ? result : null), OpCode.Update, providername, taskId, operationContext);
                    }
                } else if (!_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind) {
                    this._context.getDsMgr().WriteBehind(_context.getCacheImpl(), filteredKeys, filteredValues, clone, null, taskId, providername, OpCode.Update, WriteBehindAsyncProcessor.TaskState.Execute);
                }
            }
            return result;
        } catch (Exception inner) {
            throw inner;
        }
    }

    /**
     * Internal Insert operation. Does a write thru as well.
     */
    private java.util.HashMap Insert(Object[] keys, CacheEntry[] entries, OperationContext operationContext) throws OperationFailedException {
        try {
            java.util.HashMap result;
            result = CascadedInsert(keys, entries, true, operationContext);
            if (result != null) {

                java.util.HashMap tmp = (java.util.HashMap) result.clone();
                Iterator ide = tmp.entrySet().iterator();
                while (ide.hasNext()) {
                    Map.Entry pair = (Map.Entry) ide.next();
                    CacheInsResultWithEntry insResult = null;
                    if (pair.getValue() instanceof CacheInsResultWithEntry) {
                        insResult = (CacheInsResultWithEntry) pair.getValue();
                        switch (insResult.getResult()) {
                            case Failure:
                                break;

                            case NeedsEviction:
                                result.put(pair.getKey(), new OperationFailedException("The cache is full and not enough items could be evicted."));
                                break;

                            case Success:
                                result.remove(pair.getKey());
                                break;

                            case SuccessOverwrite:
                                result.remove(pair.getKey());
                                break;

                            case IncompatibleGroup:
                                result.put(pair.getKey(), new OperationFailedException("Data group of the inserted item does not match the existing item's data group"));
                                break;
                        }
                    }
                }
            }
            return result;
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Insert()", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Insert()", inner.toString());
            throw new OperationFailedException("Insert operation failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Removes the object and key pair from the cache. The key is specified as
     * parameter.
     *
     * @param key
     * @param operationContext
     * @return
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final CompressedValueEntry Remove(Object key, OperationContext operationContext) throws OperationFailedException {
        return Remove(key, new BitSet(), null, null, 0, LockAccessType.IGNORE_LOCK, null, operationContext);
    }

    /**
     * Remove the group from cache.
     *
     * @param group group to be removed.
     * @param subGroup subGroup to be removed.
     * @param operationContext
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final void Remove(String group, String subGroup, OperationContext operationContext) throws OperationFailedException {
        if (group == null) {
            throw new IllegalArgumentException("group");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return;
        }

        try {
            HPTimeStats removeTime = new HPTimeStats();
            removeTime.BeginSample();

            java.util.HashMap removed = CascadedRemove(group, subGroup, true, operationContext);

            removeTime.EndSample();

        } catch (StateTransferInProgressException se) {
            throw se;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Remove()", inner.toString());
            throw new OperationFailedException("Remove operation failed. Error : " + inner.getMessage(), inner);
        }
        return;
    }

    /**
     * Removes the object and key pair from the cache. The key is specified as
     * parameter.
     *
     * @param key
     * @param flag
     * @param cbEntry
     * @param lockId
     * @param version
     * @param accessType
     * @param providerName
     * @param operationContext
     * @return
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final CompressedValueEntry Remove(Object key, BitSet flag, CallbackEntry cbEntry, Object lockId, long version, LockAccessType accessType, String providerName, OperationContext operationContext) throws
            OperationFailedException {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }


        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }

        DataSourceUpdateOptions updateOpts = this.UpdateOption(flag);

        this.CheckDataSourceAvailabilityAndOptions(updateOpts);
        try {
            HPTimeStats removeTime = new HPTimeStats();
            removeTime.BeginSample();
            _context.PerfStatsColl.mSecPerDelBeginSample();

            Object packedKey = key;
            if (_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind) {
                packedKey = new Object[]{
                    key,
                    updateOpts,
                    cbEntry,
                    providerName
                };
            }
            CacheEntry e = CascadedRemove(key, packedKey, ItemRemoveReason.Removed, true, lockId, version, accessType, operationContext);

            _context.PerfStatsColl.mSecPerDelEndSample();
            _context.PerfStatsColl.incrementDelPerSecStats();
            removeTime.EndSample();

            OperationResult dsResult = null;
            String taskId = new com.alachisoft.tayzgrid.caching.util.GUID().toString();
            if (updateOpts == DataSourceUpdateOptions.WriteThru ) {
                dsResult = this._context.getDsMgr().WriteThru(key, null, OpCode.Remove, providerName, operationContext);
                if (dsResult != null && dsResult.getDSOperationStatus() == OperationResult.Status.FailureRetry) {
                    WriteOperation operation = dsResult.getOperation();
                    if (operation != null) {
                        _context.getCacheLog().Info("Retrying Write Operation" + operation.getOperationType() + " operation for key:" + operation.getKey());
                        //creating ds operation with previous entry
                        DSWriteBehindOperation dsOperation = new DSWriteBehindOperation(this.getName(), operation.getKey(), null, e, OpCode.Remove, providerName, 0, taskId, null, WriteBehindAsyncProcessor.TaskState.Execute);
                        _context.getCacheImpl().EnqueueDSOperation(dsOperation);
                    }
                }
            } else if (!_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind) {
                CacheEntry ce = new CacheEntry();
                if (cbEntry != null) {
                        if(e == null) 
                            ce.setValue(cbEntry);
                        else
                            ce = e;
                    if (ce.getValue() instanceof CallbackEntry) {
                        ((CallbackEntry) ce.getValue()).setWriteBehindOperationCompletedCallback(cbEntry.getWriteBehindOperationCompletedCallback());
                    } else {
                        cbEntry.setValue(ce.getValue());
                        ce.setValue(cbEntry); 
                    }
                }

                this._context.getDsMgr().WriteBehind(_context.getCacheImpl(), key, ce, null, taskId, providerName, OpCode.Remove, WriteBehindAsyncProcessor.TaskState.Execute);
            }
            if (e != null) {
                CompressedValueEntry obj = new CompressedValueEntry();
                obj.Value = e.getValue();
                obj.Flag = e.getFlag();
                if (obj.Value instanceof CallbackEntry) {
                    obj.Value = ((CallbackEntry) obj.Value).getValue();
                }
                return obj;
            }
        } catch (OperationFailedException ex) {
            if (ex.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Remove()", ex.toString());
            }
            throw ex;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Remove()", inner.toString());
            throw new OperationFailedException("Remove operation failed. Error : " + inner.getMessage(), inner);
        }
        return null;
    }

    /**
     * Removes the object and key pair from the cache. The key is specified as
     * parameter.
     *
     * @param key
     * @param flag
     * @param cbEntry
     * @param lockId
     * @param version
     * @param accessType
     * @param providerName
     * @param operationContext
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final boolean Delete(Object key, BitSet flag, CallbackEntry cbEntry, Object lockId, long version, LockAccessType accessType, String providerName, OperationContext operationContext) throws
            OperationFailedException {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return false;
        }
        DataSourceUpdateOptions updateOpts = this.UpdateOption(flag);
        this.CheckDataSourceAvailabilityAndOptions(updateOpts);

        try {
            HPTimeStats removeTime = new HPTimeStats();
            removeTime.BeginSample();
            _context.PerfStatsColl.mSecPerDelBeginSample();
            Object packedKey = key;
            if (_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind) {
                packedKey = new Object[]{
                    key,
                    updateOpts,
                    cbEntry,
                    providerName
                };
            }

            CacheEntry e = CascadedRemove(key, packedKey, ItemRemoveReason.Removed, true, lockId, version, accessType, operationContext);

            _context.PerfStatsColl.mSecPerDelEndSample();
            _context.PerfStatsColl.incrementDelPerSecStats();
            removeTime.EndSample();
 
            DeleteParams deleteParams = null;
            boolean isWriteThrough = false;
            if(operationContext.Contains(OperationContextFieldName.DeleteParams)) {
                deleteParams = (DeleteParams)operationContext.GetValueByField(OperationContextFieldName.DeleteParams);
                if((deleteParams.CompareOldValue && e != null) || (!deleteParams.CompareOldValue))
                    isWriteThrough = true;
            }
            else if(updateOpts == DataSourceUpdateOptions.WriteBehind || updateOpts == DataSourceUpdateOptions.WriteThru)
                isWriteThrough = true;

            if(isWriteThrough) {
                OperationResult dsResult = null;
                String taskId = new com.alachisoft.tayzgrid.caching.util.GUID().toString();
                if (updateOpts == DataSourceUpdateOptions.WriteThru ) {
                    dsResult = this._context.getDsMgr().WriteThru(key, /*e*/null, OpCode.Remove, providerName, operationContext);
                    if (dsResult != null && dsResult.getDSOperationStatus() == OperationResult.Status.FailureRetry) {
                        WriteOperation operation = dsResult.getOperation();
                        if (operation != null) {
                            _context.getCacheLog().Info("Retrying Write Operation" + operation.getOperationType() + " operation for key:" + operation.getKey());
                            //creating ds operation with with previous entry

                            DSWriteBehindOperation dsOperation = new DSWriteBehindOperation(this.getName(), operation.getKey(), null, e, OpCode.Remove, providerName, 0, taskId, null, WriteBehindAsyncProcessor.TaskState.Execute);
                            _context.getCacheImpl().EnqueueDSOperation(dsOperation);
                        }
                    }

                 } else if (!_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind) {
                     CacheEntry ce = new CacheEntry();
                    if (cbEntry != null) {
                        if(e == null) 
                            ce.setValue(cbEntry);
                        else
                            ce = e;

                        if (ce.getValue() instanceof CallbackEntry) {
                            ((CallbackEntry) ce.getValue()).setWriteBehindOperationCompletedCallback(cbEntry.getWriteBehindOperationCompletedCallback());
                        } else {
                            cbEntry.setValue(ce.getValue());
                            ce.setValue(cbEntry);
                        }
                    }
                    this._context.getDsMgr().WriteBehind(_context.getCacheImpl(), key, ce, null, taskId, providerName, OpCode.Remove, WriteBehindAsyncProcessor.TaskState.Execute); 
                }
            }
            if(e!=null)
                return true;
            else
                return false;


        } catch (OperationFailedException ex) {
            if (ex.getIsTracable()) {
                _context.getCacheLog().Error("Cache.Delete()", ex.toString());
            }
            throw ex;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Delete()", inner.toString());
            throw new OperationFailedException("Delete operation failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Removes the object and key pair from the cache. The key is specified as
     * parameter.
     */
    public final void RemoveAsync(Object key, OperationContext operationContext) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return;
        }
        _asyncProcessor.Enqueue(new AsyncRemove(this, key, operationContext));

    }

    /**
     * Removes the objects for the given keys from the cache. The keys are
     * specified as parameter.
     *
     * @param keys array of keys to be removed
     * @param flagMap
     * @param cbEntry
     * @param providerName
     * @param operationContext
     * @return keys that failed to be removed
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final java.util.Map Remove(Object[] keys, BitSet flagMap, CallbackEntry cbEntry, String providerName, OperationContext operationContext) throws OperationFailedException {
        if (keys == null) {
            throw new IllegalArgumentException("keys");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(keys, AllowedOperationType.BulkWrite, operationContext)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        DataSourceUpdateOptions updateOpts = UpdateOption(flagMap);

        this.CheckDataSourceAvailabilityAndOptions(updateOpts);

        try {
            HPTimeStats removeTime = new HPTimeStats();
            removeTime.BeginSample();

            if (_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind) {
                Object pack = new Object[]{
                    keys[0],
                    updateOpts,
                    cbEntry,
                    providerName
                };
                keys[0] = pack;
            }

            java.util.Map removed = CascadedRemove(keys, ItemRemoveReason.Removed, true, operationContext);

            removeTime.EndSample();

            if (updateOpts != DataSourceUpdateOptions.None /*&& removed != null && removed.size() > 0 */&& !(_context.getIsClusteredImpl() && updateOpts
                    == DataSourceUpdateOptions.WriteBehind)) {
                

                OperationResult[] dsResults = null;
                String taskId = new com.alachisoft.tayzgrid.caching.util.GUID().toString();
                if (updateOpts == DataSourceUpdateOptions.WriteThru ) {
                    HashMap returnSet = new HashMap();
//                    dsResults = this._context.getDsMgr().WriteThru(filteredKeys, null, filteredEntries, returnSet, OpCode.Remove, providerName, operationContext);
                      dsResults = this._context.getDsMgr().WriteThru(keys, null, null, returnSet, OpCode.Remove, providerName, operationContext);
                    if (dsResults != null) {
//                        EnqueueRetryOperations(filteredKeys, null, filteredEntries, returnSet, OpCode.Remove, providerName, taskId, operationContext);
                        EnqueueRetryOperations(keys, null, null, returnSet, OpCode.Remove, providerName, taskId, operationContext);
                    }
                } else if (!_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind) {
                    
                    CacheEntry [] entries = new CacheEntry[keys.length];
                    if(cbEntry != null) {
                        for(int i=0;i<keys.length;i++) {
                            if(removed != null && removed.containsKey(keys[i])) {
                                entries[i] = (CacheEntry) ((removed.get(keys[i]) instanceof CacheEntry) ? removed.get(keys[i]) : null);
                            }
                            else{
                                entries[i] = new CacheEntry();
                                entries[i].setValue(cbEntry);
                            }
                        }
                        

                        for (int i = 0; i < entries.length; i++) {
                            if (entries[i].getValue() instanceof CallbackEntry) {
                                ((CallbackEntry) entries[i].getValue()).setWriteBehindOperationCompletedCallback(cbEntry.getWriteBehindOperationCompletedCallback());
                            } else {
                                cbEntry.setValue(entries[i].getValue());
                                entries[i].setValue(cbEntry);
                            }
                        }
                    }
                    
                    this._context.getDsMgr().WriteBehind(_context.getCacheImpl(), keys, null, entries, null, taskId, providerName, OpCode.Remove, WriteBehindAsyncProcessor.TaskState.Execute);
               }
            }

            CompressedValueEntry val = null;
            if (removed != null) {
                Object[] keysCollection = new Object[removed.size()];
                System.arraycopy(removed.keySet().toArray(), 0, keysCollection, 0, removed.keySet().size());
                java.util.Iterator ie = Arrays.asList(keysCollection).iterator();
                while (ie.hasNext()) {
                    Object curVal = ie.next();
                    CacheEntry entry = (CacheEntry) ((removed.get(curVal) instanceof CacheEntry) ? removed.get(curVal) : null);
                    if (entry != null) {
                        val = new CompressedValueEntry();
                        val.Value = entry.getValue();
                        if (val.Value instanceof CallbackEntry) {
                            val.Value = ((CallbackEntry) val.Value).getValue();
                        }
                        val.Flag = entry.getFlag();
                        removed.put(curVal, val);
                    }
                }
            }

            return removed;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Remove()", inner.toString());
            throw new OperationFailedException("Remove operation failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Removes the objects for the given keys from the cache. The keys are
     * specified as parameter.
     *
     * @param keys array of keys to be removed
     * @param flagMap
     * @param cbEntry
     * @param providerName
     * @param operationContext
     * @throws OperationFailedException
     */
    public final void Delete(Object[] keys, BitSet flagMap, CallbackEntry cbEntry, String providerName, OperationContext operationContext) throws OperationFailedException {
        if (keys == null) {
            throw new IllegalArgumentException("keys");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return;
        }

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(keys, AllowedOperationType.BulkWrite, operationContext)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        DataSourceUpdateOptions updateOpts = UpdateOption(flagMap);

        this.CheckDataSourceAvailabilityAndOptions(updateOpts);

        try {
            HPTimeStats removeTime = new HPTimeStats();
            removeTime.BeginSample();

            if (_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind) {
                Object pack = new Object[]{
                    keys[0],
                    updateOpts,
                    cbEntry,
                    providerName
                };
                keys[0] = pack;
            }

            java.util.Map removed = CascadedRemove(keys, ItemRemoveReason.Removed, true, operationContext);

            removeTime.EndSample();

            if (updateOpts != DataSourceUpdateOptions.None /*&& removed != null && removed.size() > 0*/ && !(_context.getIsClusteredImpl() && updateOpts
                    == DataSourceUpdateOptions.WriteBehind)) {


                OperationResult[] dsResults = null;
                String taskId = new com.alachisoft.tayzgrid.caching.util.GUID().toString();
                if (updateOpts == DataSourceUpdateOptions.WriteThru ) {
                    HashMap returnSet = new HashMap();
                  //  dsResults = this._context.getDsMgr().WriteThru(filteredKeys, null, filteredEntries, returnSet, OpCode.Remove, providerName, operationContext);
                    dsResults = this._context.getDsMgr().WriteThru(keys, null, null, returnSet, OpCode.Remove, providerName, operationContext);
                    if (dsResults != null) {
                        //EnqueueRetryOperations(filteredKeys, null, filteredEntries, returnSet, OpCode.Remove, providerName, taskId, operationContext);
                        EnqueueRetryOperations(keys, null, null, returnSet, OpCode.Remove, providerName, taskId, operationContext);
                    }
                } else if (!_context.getIsClusteredImpl() && updateOpts == DataSourceUpdateOptions.WriteBehind) {
                    CacheEntry [] entries = new CacheEntry[keys.length];
                    if(cbEntry != null) {
                        for(int i=0;i<keys.length;i++) {
                            if(removed != null && removed.containsKey(keys[i])) {
                                entries[i] = (CacheEntry) ((removed.get(keys[i]) instanceof CacheEntry) ? removed.get(keys[i]) : null);
                            }
                            else{
                                entries[i] = new CacheEntry();
                                entries[i].setValue(cbEntry);
                            }
                            
                        }
                        for (int i = 0; i < entries.length; i++) {
                            if (entries[i].getValue() instanceof CallbackEntry) {
                                ((CallbackEntry) entries[i].getValue()).setWriteBehindOperationCompletedCallback(cbEntry.getWriteBehindOperationCompletedCallback());
                            } else {
                                cbEntry.setValue(entries[i].getValue());
                                entries[i].setValue(cbEntry);
                            }
                        }
                    }

                    this._context.getDsMgr().WriteBehind(_context.getCacheImpl(), keys, null, entries, null, taskId, providerName, OpCode.Remove, WriteBehindAsyncProcessor.TaskState.Execute);
                }
            }

        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.Delete()", inner.toString());
            throw new OperationFailedException("Delete operation failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Retrieves a dictionary enumerator used to iterate through the key
     * settings and their values contained in the cache
     *
     * @return
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    public final java.util.Iterator GetEnumerator() throws OperationFailedException {
        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }
        try {
            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.ClusterRead, new OperationContext())) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }

            return (Iterator) new com.alachisoft.tayzgrid.caching.util.CacheEnumerator(_context.getSerializationContext(), _context.getCacheImpl().GetEnumerator());
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.GetEnumerator()", inner.toString());
            throw new OperationFailedException("GetEnumerator failed. Error : " + inner.getMessage(), inner);
        }
    }

    public final EnumerationDataChunk GetNextChunk(EnumerationPointer pointer, OperationContext operationContext) throws OperationFailedException {
        EnumerationDataChunk chunk = null;

        if (!getIsRunning()) {
            return null;
        }

        try {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkWrite, operationContext)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }

            chunk = _context.getCacheImpl().GetNextChunk(pointer, operationContext);
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.GetNextChunk()", inner.toString());
            throw new OperationFailedException("GetNextChunk failed. Error : " + inner.getMessage(), inner);
        }

        return chunk;
    }

    public java.util.ArrayList<com.alachisoft.tayzgrid.persistence.Event> GetFilteredEvents(String clientID, HashMap events, EventStatus registeredEventStatus) throws Exception {
        if (_context.PersistenceMgr != null) {
            if (_context.PersistenceMgr.HasCompleteData()) {
                return _context.PersistenceMgr.GetFilteredEventsList(clientID, events, registeredEventStatus);
            } else {
                return _context.getCacheImpl().GetFilteredEvents(clientID, events, registeredEventStatus);
            }

        }
        return null;
    }

    /**
     * Fired when an item is added to the cache.
     *
     * @param key key of the cache item
     * @param operationContext
     * @param eventContext
     */
    @Override
    public void OnItemAdded(Object key, OperationContext operationContext, EventContext eventContext) {

        if (_itemAdded != null) {
            try {
                key = SerializationUtil.compactSerialize(key, _context.getSerializationContext());
            } catch (IOException iOException) {
                _context.getCacheLog().Error("Cache.OnItemAdded()", iOException.toString());
                return;
            }
            _itemAdded.fireEventsSynchronous(true, key, eventContext);
        }
    }

    /**
     * handler for item updated event.
     *
     * @param key key of the Item to be added
     * @param operationContext
     * @param eventContext
     */
    @Override
    public void OnItemUpdated(Object key, OperationContext operationContext, EventContext eventContext) {

        if (_itemUpdated != null) {
            try {
                key = SerializationUtil.compactSerialize(key, _context.getSerializationContext());
            } catch (IOException iOException) {
                _context.getCacheLog().Error("Cache.OnItemUpdated()", iOException.toString());
                //return;
            }
            _itemUpdated.fireEventsSynchronous(true, key, eventContext);
        }
    }

    /**
     * Fired when an item is removed from the cache.
     *
     * @param key key of the cache item
     * @param value item itself
     * @param reason reason the item was removed
     * @param operationContext
     * @param eventContext
     */
    @Override
    public final void OnItemRemoved(Object key, Object value, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) {

        Object data = null;
        if (value != null) {
            data = ((CacheEntry) value).getValue();
        }
        try {
            key = SerializationUtil.compactSerialize(key, _context.getSerializationContext());
        } catch (IOException iOException) {
            _context.getCacheLog().Error("Cache.OnItemRemoved()", iOException.toString());
            return;
        }

        if (_itemRemoved != null) {

            BitSet flag = null;
            if (eventContext != null && eventContext.getItem() != null && eventContext.getItem().getFlags() != null) {
                flag = eventContext.getItem().getFlags();
            }
            _itemRemoved.fireEventsSynchronous(true, new Object[]{
                key,
                data,
                reason,
                flag,
                eventContext
            });
        }
    }

    /**
     * Fired when multiple items are removed from the cache.
     *
     * @param keys
     * @param value
     * @param operationContext
     * @param eventContext
     */
    @Override
    public final void OnItemsRemoved(Object[] keys, Object[] value, ItemRemoveReason reason, OperationContext operationContext, EventContext[] eventContext) {

        try {
            if (_itemRemoved != null) {
                for (int i = 0; i < keys.length; i++) {
                    OnItemRemoved(keys[i], null, reason, operationContext, eventContext[i]);
                }
            }
        } catch (Exception e) {
            _context.getCacheLog().Error("Cache.OnItemsRemoved()", e.toString());
        }

    }

    /**
     * Fire when the cache is cleared.
     *
     * @param operationContext
     * @param eventContext
     */
    @Override
    public void OnCacheCleared(OperationContext operationContext, EventContext eventContext) {

        if (_cacheCleared != null) {
            _cacheCleared.fireEventsSynchronous(s_logClientEvents, null, eventContext);
        }

    }

    public void OnCustomEvent(Object notifId, Object data, OperationContext operationContext, EventContext eventContext) {

        if (_cusotmNotif != null) {
            _cusotmNotif.fireEvents(true, new Object[]{
                notifId,
                data,
                eventContext
            });
        }
    }

    /**
     * Fired when an item is removed from the cache having
     * CacheItemRemoveCallback.
     *
     * @param key key of the cache item
     * @param value CallbackEntry containing the callback and actual item
     * @param reason reason the item was removed
     * @param operationContext
     * @param eventContext
     */
    @Override
    public void OnCustomRemoveCallback(Object key, Object value, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) {
        CallbackEntry cbEntry = (CallbackEntry) ((value instanceof CallbackEntry) ? value : null);

        Object tempVar = eventContext.GetValueByField(EventContextFieldName.ItemRemoveCallbackList);
        java.util.List removeCallbacklist = (java.util.List) ((tempVar instanceof java.util.List) ? tempVar : null);

        if (removeCallbacklist != null && removeCallbacklist.size() > 0) {
            for (Iterator it = removeCallbacklist.iterator(); it.hasNext();) {
                CallbackInfo cbInfo = (CallbackInfo) it.next();
                if (_connectedClients != null && _connectedClients.contains(cbInfo.getClient())) {
                    if (_customRemoveNotif != null) {
                        _customRemoveNotif.fireEvents(true, new Object[]{
                            key,
                            new Object[]{
                                null,
                                cbInfo
                            },
                            reason,
                            null,
                            eventContext
                        });
                    }
                }
            }
        }
    }

    /**
     * Fired when an item is updated and it has CacheItemUpdate callback
     * attached with it.
     *
     * @param key key of the cache item
     * @param value CallbackEntry containing the callback and actual item
     * @param operationContext
     * @param eventContext
     */
    @Override
    public void OnCustomUpdateCallback(Object key, Object value, OperationContext operationContext, EventContext eventContext) {
        java.util.List updateListeners = (java.util.List) ((value instanceof java.util.List) ? value : null);

        if (updateListeners != null && updateListeners.size() > 0) {
            Object tempVar = updateListeners;
            updateListeners = (java.util.List) ((tempVar instanceof java.util.List) ? tempVar : null);
            for (Iterator it = updateListeners.iterator(); it.hasNext();) {
                CallbackInfo cbInfo = (CallbackInfo) it.next();
                if (_connectedClients != null && _connectedClients.contains(cbInfo.getClient())) {
                    if (_customUpdateNotif != null) {
                        _customUpdateNotif.fireEvents(true, new Object[]{
                            key,
                            cbInfo,
                            eventContext
                        });
                    }
                }
            }
        }
    }

    /**
     * Fire when hasmap changes when - new node joins - node leaves -
     * manual/automatic load balance
     *
     * @param newHashmap new hashmap
     * @param updateClientMap
     */
    @Override
    public void OnHashmapChanged(NewHashmap newHashmap, boolean updateClientMap) {

      
            if (this._hashmapChanged == null) {
                return;
            }
            _hashmapChanged.fireEvents(true, newHashmap, null);

    }

    /**
     *
     *
     * @param operationCode
     * @param result
     * @param cbEntry
     */
    @Override
    public void OnWriteBehindOperationCompletedCallback(OpCode operationCode, Object result, CallbackEntry cbEntry) {

        if (cbEntry.getWriteBehindOperationCompletedCallback() == null) {
            return;
        }
        if (_dataSourceUpdated == null) {
            return;
        }

        _dataSourceUpdated.fireEvents(true, new Object[]{
            result,
            cbEntry,
            operationCode
        });

    }

    /**
     * Fired when an asynchronous opertaion is performed on cache.
     *
     * @param opCode
     * @param result
     */
    public final void OnAsyncOperationCompleted(AsyncOpCode opCode, Object result) {

        if (_asyncOperationCompleted != null) {
            _asyncOperationCompleted.fireEvents(true, new Object[]{
                opCode,
                result,
                null
            });
        }
    }

    /**
     * Clears the list of all callback listeners when disposing.
     */
    private void ClearCallbacks() {
        if (_asyncOperationCompleted != null) {
            _asyncOperationCompleted.unsubscribeAllListners();
        }
        if (_cacheStopped != null) {
            _cacheStopped.unsubscribeAllListners();
        }
        if (_cacheCleared != null) {
            _cacheCleared.unsubscribeAllListners();
        }
        if (_itemUpdated != null) {
            _itemUpdated.unsubscribeAllListners();
        }
        if (_itemRemoved != null) {
            _itemRemoved.unsubscribeAllListners();
        }
        if (_itemAdded != null) {
            _itemAdded.unsubscribeAllListners();
        }
        
        if(_taskNotifListeners!=null){
            _taskNotifListeners.unsubscribeAllListners();
        }
        
        if (_customUpdateNotif != null) {
            _customUpdateNotif.unsubscribeAllListners();
        }
        if (_customRemoveNotif != null) {
            _customRemoveNotif.unsubscribeAllListners();
        }
        if (_cusotmNotif != null) {
            _cusotmNotif.unsubscribeAllListners();
        }
    }

    public final QueryResultSet Search(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, TypeIndexNotDefined {
        if (!getIsRunning()) {
            return null;
        }

        if (query == null || query.equals("")) {
            throw new IllegalArgumentException("query");
        }

        try {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkRead, operationContext)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }

            return _context.getCacheImpl().Search(query, values, operationContext);
        } catch (OperationFailedException ex) {
            if (ex.getIsTracable()) {
                if (_context.getCacheLog().getIsErrorEnabled()) {
                    _context.getCacheLog().Error("search operation failed. Error: " + ex.toString());
                }
            }
            if (ex.getMessage().contains("Index is not defined for")) {
                throw new TypeIndexNotDefined(ex.getMessage());
            }
            throw ex;
        } catch (StateTransferInProgressException inner) {
            throw inner;
        } catch (com.alachisoft.tayzgrid.parser.TypeIndexNotDefined inner) {
            throw new com.alachisoft.tayzgrid.parser.TypeIndexNotDefined(inner.getMessage());
        } catch (Exception ex) {
            if (_context.getCacheLog().getIsErrorEnabled()) {
                _context.getCacheLog().Error("search operation failed. Error: " + ex.toString());
            }

            if (ex.getMessage().contains("Index is not defined for")) {
                throw new TypeIndexNotDefined(ex.getMessage());
            }
            throw new OperationFailedException("search operation failed. Error: " + ex.getMessage(), ex);
        }
    }

    public final QueryResultSet SearchEntries(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, TypeIndexNotDefined {
        if (!getIsRunning()) {
            return null;
        }

        if (query == null || query.equals("")) {
            throw new IllegalArgumentException("query");
        }
        try {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkRead, operationContext)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }

            return _context.getCacheImpl().SearchEntries(query, values, operationContext);
        } catch (OperationFailedException ex) {
            if (ex.getIsTracable()) {
                if (_context.getCacheLog().getIsErrorEnabled()) {
                    _context.getCacheLog().Error("search operation failed. Error: " + ex.toString());
                }
            }
            if (ex.getMessage().contains("Index is not defined for")) {
                throw new TypeIndexNotDefined(ex.getMessage());
            }
            throw ex;
        } catch (StateTransferInProgressException inner) {
            throw inner;
        } catch (Exception ex) {
            if (_context.getCacheLog().getIsErrorEnabled()) {
                _context.getCacheLog().Error("search operation failed. Error: " + ex.toString());
            }

            if (ex instanceof com.alachisoft.tayzgrid.parser.TypeIndexNotDefined) {
                throw new com.alachisoft.tayzgrid.parser.TypeIndexNotDefined("search operation failed. Error: " + ex.getMessage());
            }
            if (ex instanceof com.alachisoft.tayzgrid.parser.AttributeIndexNotDefined) {
                throw new com.alachisoft.tayzgrid.parser.AttributeIndexNotDefined("search operation failed. Error: " + ex.getMessage());
            }

            throw new OperationFailedException("search operation failed. Error: " + ex.getMessage(), ex);
        }
    }

    public void DeleteQuery(String query, java.util.Map values, OperationContext operationContext) throws
            OperationFailedException, Exception {
        if (!getIsRunning()) {
            return;
        }
        if (query == null || query.equals("")) {
            throw new IllegalArgumentException("query");
        }
        try {
            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkRead, operationContext)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }
            operationContext.Add(OperationContextFieldName.NotifyRemove, true);
            DeleteQueryResultSet result = _context.getCacheImpl().DeleteQuery(query, values, true, true, ItemRemoveReason.Removed, operationContext);
            operationContext.Add(OperationContextFieldName.NotifyRemove, false);
           
        } catch (StateTransferInProgressException ex) {
            throw ex;
        } catch (Exception ex) {
            if (_context.getCacheLog().getIsErrorEnabled()) {
                _context.getCacheLog().Error("delete query operation failed. Error: " + ex.toString());
            }
            throw new OperationFailedException("search operation failed. Error: " + ex.getMessage(), ex);
        }
    }

    public int RemoveQuery(String query, java.util.Map values, OperationContext operationContext) throws
            OperationFailedException, Exception {
        if (!getIsRunning()) {
            return 0;
        }

        if (query == null || query.equals("")) {
            throw new IllegalArgumentException("query");
        }
        try {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkRead, operationContext)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }
            operationContext.Add(OperationContextFieldName.NotifyRemove, true);
            DeleteQueryResultSet result = _context.getCacheImpl().DeleteQuery(query, values, true, true, ItemRemoveReason.Removed, operationContext);
            operationContext.Add(OperationContextFieldName.NotifyRemove, false);
          

            if (_context.PerfStatsColl != null) {
                _context.PerfStatsColl.incrementDelPerSecStatsBy(result.getKeysEffectedCount());
            }

            if (result != null) {
                return result.getKeysEffectedCount();
            } else {
                return 0;
            }

        } catch (OperationFailedException ex) {
            if (ex.getIsTracable()) {
                if (_context.getCacheLog().getIsErrorEnabled()) {
                    _context.getCacheLog().Error("search operation failed. Error: " + ex.toString());
                }
            }
            
            throw ex;
        } catch (StateTransferInProgressException ex) {
            throw ex;
        } catch (Exception ex) {
            if (_context.getCacheLog().getIsErrorEnabled()) {
                _context.getCacheLog().Error("Remove operation failed. Error: " + ex.toString());
            }
            if (ex.getMessage().contains("Index is not defined for attribute")) {
                throw new AttributeIndexNotDefined(ex.getMessage());
            } else if (ex.getMessage().contains("Index is not defined for")) {
                throw new TypeIndexNotDefined(ex.getMessage());
            }
            throw new OperationFailedException("Remove operation failed. Error: " + ex.getMessage(), ex);
        }
    }

    public final CacheInsResultWithEntry CascadedInsert(Object key, CacheEntry entry, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws
            OperationFailedException, GeneralFailureException, LockingException, CacheException {

        Object block = null;
        Boolean isNoBlock = false;
        block = operationContext.GetValueByField(OperationContextFieldName.NoGracefulBlock);
        if (block != null) {
            isNoBlock = (Boolean) block;
        }

        if (!isNoBlock) {

            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(key, AllowedOperationType.AtomicWrite)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }
        }

        CacheInsResultWithEntry result = _context.getCacheImpl().Insert(key, entry, notify, lockId, version, accessType, operationContext);
      
        return result;
    }

    public final java.util.HashMap CascadedInsert(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException,
            GeneralFailureException,
            LockingException,
            CacheException {

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(keys, AllowedOperationType.BulkWrite, operationContext)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        java.util.HashMap table = _context.getCacheImpl().Insert(keys, cacheEntries, notify, operationContext);
      
        return table;
    }

    public final CacheEntry CascadedRemove(Object key, Object pack, ItemRemoveReason reason, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws
            OperationFailedException, GeneralFailureException, LockingException, CacheException {

        Object block = null;
        Boolean isNoBlock = false;
        block = operationContext.GetValueByField(OperationContextFieldName.NoGracefulBlock);
        if (block != null) {
            isNoBlock = (Boolean) block;
        }

        if (!isNoBlock) {
            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
                if (!_context.getCacheImpl().IsOperationAllowed(key, AllowedOperationType.AtomicWrite)) {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }
        }

        CacheEntry oldEntry = _context.getCacheImpl().Remove(pack, reason, notify, lockId, version, accessType, operationContext);

      
        return oldEntry;
    }

    public final java.util.HashMap CascadedRemove(Object[] keys, ItemRemoveReason reason, boolean notify, OperationContext operationContext) throws OperationFailedException,
            GeneralFailureException,
            LockingException,
            CacheException {

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(keys, AllowedOperationType.BulkWrite, operationContext)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        java.util.HashMap table = _context.getCacheImpl().Remove(keys, reason, notify, operationContext);
       
        return table;
    }

    public final java.util.HashMap CascadedRemove(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException,
            GeneralFailureException,
            LockingException, CacheException {

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkWrite, operationContext)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        java.util.HashMap table = _context.getCacheImpl().Remove(group, subGroup, notify, operationContext);
        
        return table;
    }

    public final java.util.HashMap CascadedRemove(String[] tags, TagComparisonType comaprisonType, boolean notify, OperationContext operationContext) throws
            OperationFailedException, GeneralFailureException, LockingException, CacheException {

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkWrite, operationContext)) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        java.util.HashMap table = _context.getCacheImpl().Remove(tags, comaprisonType, notify, operationContext);
   
        return table;
    }



    public final int GetNumberOfClientsToDisconect() {
        CacheBase tempVar = this._context.getCacheImpl();
        ReplicatedServerCache impl = (ReplicatedServerCache) ((tempVar instanceof ReplicatedServerCache) ? tempVar : null);
        if (impl == null) {
            return 0;
        }

        if (_inProc) {
            return 0;
        }

        java.util.List nodes = ((ClusterCacheBase) this._context.getCacheImpl())._stats.getNodes();

        int clientsToDisconnect = 0;
        int totalClients = 0;
        int currNodeClientCount = 0;
        int maxClientsPerNode;
        try {
            for (Iterator it = nodes.iterator(); it.hasNext();) {
                NodeInfo i = (NodeInfo) it.next();
                if (((ClusterCacheBase) this._context.getCacheImpl()).getCluster().getLocalAddress().compareTo(i.getAddress()) == 0) {
                    currNodeClientCount = i.getConnectedClients().size();
                }
                totalClients = totalClients + i.getConnectedClients().size();
            }
        } catch (Exception e) {
        }

        maxClientsPerNode = (int) Math.ceil(((double) totalClients / nodes.size()));

        if (currNodeClientCount > maxClientsPerNode) {
            clientsToDisconnect = currNodeClientCount - maxClientsPerNode;
            return clientsToDisconnect;
        }

        return 0;

    }

    public final void OnMemberJoined(com.alachisoft.tayzgrid.common.net.Address clusterAddress, com.alachisoft.tayzgrid.common.net.Address serverAddress) {
        int clientsToDisconnect = 0;
        try {
            
                clientsToDisconnect = this.GetNumberOfClientsToDisconect();
            
        } catch (Exception e) {
            clientsToDisconnect = 0;
        }
        try {
            if (_memberJoined != null) {
                HashMap hd = _memberJoined.getInvocationList();

                Iterator ide = hd.entrySet().iterator();

                for (int i = hd.size() - 1; i >= 0; i--) {
                    Map.Entry<NEventStart, NEventEnd> pair = (Map.Entry<NEventStart, NEventEnd>) ide.next();

                    if (i > (clientsToDisconnect - 1)) {
                        this.MemberJoinedAsyncCallbackHandler(pair.getKey(), new Object[]{
                            clusterAddress,
                            serverAddress,
                            false,
                            null
                        });

                    } else {
                        this.MemberJoinedAsyncCallbackHandler(pair.getKey(), new Object[]{
                            clusterAddress,
                            serverAddress,
                            true
                        });
                    }
                }
            }
        } catch (Exception e) {
            _context.getCacheLog().Error("Cache.MemberJoinedAsyncCallbackHandler", e.toString());
        }
    }

    /**
     * This callback is called by .Net framework after asynchronous call for
     * OnItemAdded has ended.
     *
     * @param ar
     * @param value
     */
    public final void MemberJoinedAsyncCallbackHandler(NEventStart ar, Object... value) {
//        NodeJoinedCallback subscribber = (NodeJoinedCallback) ar.AsyncState;

        try {
            ar.hanleEvent(value);
        } catch (SocketException ex) {
            // Client is dead, so remove from the event handler list
            _context.getCacheLog().Error("Cache.MemberJoinedAsyncCallbackHandler", ex.toString());
            synchronized (_memberJoined) {
                _memberJoined.removeNEventListners(ar);
            }
        } catch (Exception e) {
            _context.getCacheLog().Error("Cache.MemberJoinedAsyncCallbackHandler", e.toString());
        }
    }

    public final void OnMemberLeft(com.alachisoft.tayzgrid.common.net.Address clusterAddress, com.alachisoft.tayzgrid.common.net.Address serverAddress) {
        if (_memberLeft != null) {

            _memberLeft.fireEvents(true, new Object[]{
                clusterAddress,
                serverAddress,
                null
            });
        }
    }

    /**
     * Registers the item update/remove or both callbacks with the specified
     * key. Keys should exist before the registration.
     *
     * @param key
     * @param updateCallback
     * @param removeCallback
     * @param operationContext
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */

    public final void RegisterKeyNotificationCallback(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws
            OperationFailedException
    {


        if (!getIsRunning()) {
            return;
        }
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        if (updateCallback == null && removeCallback == null) {
            throw new IllegalArgumentException();
        }

        try 
        {
            _context.getCacheImpl().RegisterKeyNotification(key, updateCallback, removeCallback, operationContext);
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.RegisterKeyNotificationCallback() ", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.RegisterKeyNotificationCallback() ", inner.toString());
            throw new OperationFailedException("RegisterKeyNotification failed. Error : " + inner.getMessage(), inner);
        }
    }

    public final void RegisterKeyNotificationCallback(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws
            OperationFailedException {

        if (!getIsRunning()) {
            return;
        }
        if (keys == null) {
            throw new IllegalArgumentException("keys");
        }
        if (keys.length == 0) {
            throw new IllegalArgumentException("Keys count can not be zero");
        }
        if (updateCallback == null && removeCallback == null) {
            throw new IllegalArgumentException();
        }

        try {
            _context.getCacheImpl().RegisterKeyNotification(keys, updateCallback, removeCallback, operationContext);
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.RegisterKeyNotificationCallback() ", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.RegisterKeyNotificationCallback() ", inner.toString());
            throw new OperationFailedException("RegisterKeyNotification failed. Error : " + inner.getMessage(), inner);
        }

    }

    /**
     * Unregisters the item update/remove or both call backs with the specified
     * key.
     *
     * @param key
     * @param updateCallback
     * @param removeCallback
     * @param operationContext
     * @throws OperationFailedException
     */

    public final void UnregisterKeyNotificationCallback(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws
            OperationFailedException 
    {


        if (!getIsRunning()) {
            return;
        }
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        if (updateCallback == null && removeCallback == null) {
            throw new IllegalArgumentException();
        }

        try {
            _context.getCacheImpl().UnregisterKeyNotification(key, updateCallback, removeCallback, operationContext);
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.UnregisterKeyNotificationCallback() ", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.UnregisterKeyNotificationCallback()", inner.toString());
            throw new OperationFailedException("UnregisterKeyNotification failed. Error : " + inner.getMessage(), inner);
        }
    }


    public final void UnregisterKeyNotificationCallback(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws
            OperationFailedException 
    {

        if (!getIsRunning()) {
            return;
        }
        if (keys == null) {
            throw new IllegalArgumentException("keys");
        }
        if (keys.length == 0) {
            throw new IllegalArgumentException("Keys count can not be zero");
        }
        if (updateCallback == null && removeCallback == null) {
            throw new IllegalArgumentException();
        }

        try {
            _context.getCacheImpl().UnregisterKeyNotification(keys, updateCallback, removeCallback, operationContext);
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.UnregisterKeyNotificationCallback() ", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.UnregisterKeyNotificationCallback()", inner.toString());
            throw new OperationFailedException("UnregisterKeyNotification failed. Error : " + inner.getMessage(), inner);
        }

    }

    /**
     * Apply runtime configuration settings.
     *
     * @param hotConfig
     * @return
     */
    public final Exception CanApplyHotConfig(long size) {
        if (this._context.getCacheImpl() != null && !this._context.getCacheImpl().CanChangeCacheSize(size)) {
            return new Exception("You need to remove some data from cache before applying the new size");
        }
        return null;
    }

    public final void ApplyHotConfiguration(HotConfig hotConfig) throws CacheArgumentException, Exception {
        if (hotConfig != null) {
            //backing source hot apply config
            if (hotConfig.getBackingSource() != null && this._context.getDsMgr() != null) {
                if (hotConfig.getBackingSource().containsKey("backing-source")) {
                    HashMap backingSource = (HashMap) hotConfig.getBackingSource().get("backing-source");
                    if (backingSource.containsKey("write-thru")) {
                        HashMap writeThru = (HashMap) backingSource.get("write-thru");

                        if (Boolean.parseBoolean(writeThru.get("enabled").toString()) && writeThru.containsKey("write-behind")) {
                            String mode = "non-batch";
                            int throttlingrate = -1;
                            int failedOpsQueue = -1;
                            int failedOpsEvictionRatio = -1;
                            int batchInterval = -1;
                            int operationDelay = -1;

                            HashMap writeBehind = (HashMap) writeThru.get("write-behind");

                            if (writeBehind.containsKey("mode")) {
                                mode = writeBehind.get("mode").toString();
                            }
                            if (writeBehind.containsKey("throttling-rate-per-sec")) {
                                throttlingrate = Integer.parseInt(writeBehind.get("throttling-rate-per-sec").toString());
                            }
                            if (writeBehind.containsKey("failed-operations-queue-limit")) {
                                failedOpsQueue = Integer.parseInt(writeBehind.get("failed-operations-queue-limit").toString());
                            }
                            if (writeBehind.containsKey("failed-operations-eviction-ratio")) {
                                failedOpsEvictionRatio = Integer.parseInt(writeBehind.get("failed-operations-eviction-ratio").toString());
                            }
                            if (mode.equals("batch") && writeBehind.containsKey("batch-mode-config")) {
                                HashMap batchConfig = (HashMap) writeBehind.get("batch-mode-config");

                                if (batchConfig.containsKey("batch-interval")) {
                                    batchInterval = Integer.parseInt(batchConfig.get("batch-interval").toString());
                                }
                                if (batchConfig.containsKey("operation-delay")) {
                                    operationDelay = Integer.parseInt(batchConfig.get("operation-delay").toString());
                                }
                            }
                            this._context.getDsMgr().HotApplyWriteBehind(mode, throttlingrate, failedOpsQueue, failedOpsEvictionRatio, batchInterval, operationDelay);
                        }
                    }
                }
            }

            if (hotConfig.getIsErrorLogsEnabled() && _cacheInfo != null) {
                if (!_context.getCacheLog().getIsErrorEnabled()) {

                    String cache_name = _cacheInfo.getName();
                    if (_cacheInfo.getCurrentPartitionId() != null && !_cacheInfo.getCurrentPartitionId().equals("")) {
                        cache_name += "-" + _cacheInfo.getCurrentPartitionId();
                    }
                    _context.getCacheLog().SetLevel("ERROR");
                }

                if (hotConfig.getIsDetailedLogsEnabled()) {
                    _context.getCacheLog().SetLevel("ALL");
                }
                else if (_context.getCacheLog().getIsInfoEnabled() && !hotConfig.getIsDetailedLogsEnabled())
                {
                    _context.getCacheLog().SetLevel("Error");
                }
            } else if (!hotConfig.getIsErrorLogsEnabled()) {
                _context.getCacheLog().SetLevel("OFF");
            }

            if (hotConfig.getAlertNotifier() != null) {
                if (hotConfig.getAlertNotifier().containsKey("alerts-types")) {
                    Object tempVar = hotConfig.getAlertNotifier().get("alerts-types");
                    _context.setCacheAlertTypes(com.alachisoft.tayzgrid.caching.util.AlertTypeHelper.Initialize((java.util.Map) ((tempVar instanceof java.util.Map) ? tempVar : null)));
                } else {
                    _context.setCacheAlertTypes(new AlertNotificationTypes());
                }

                if (hotConfig.getAlertNotifier().containsKey("email-notification")) {
                    if (_context.getEmailAlertNotifier() == null) {
                        _context.setEmailAlertNotifier(new EmailAlertNotifier());
                    } else {
                        _context.getEmailAlertNotifier().Unintialize();
                    }
                    Object tempVar2 = hotConfig.getAlertNotifier().get("email-notification");
                    EmailNotifierArgs emailNotifierArgs = new EmailNotifierArgs((java.util.Map) ((tempVar2 instanceof java.util.Map) ? tempVar2 : null), _context);
                    _context.getEmailAlertNotifier().Initialize(emailNotifierArgs, _context.getCacheAlertTypes());
                } else {
                    _context.setEmailAlertNotifier(new EmailAlertNotifier());
                }

                EmailAlertPropagator = _context.getEmailAlertNotifier();
                //-
            }

            this._context.getCacheImpl().getActualStats().setMaxSize(hotConfig.getCacheMaxSize());
            this._context.getCacheImpl().getInternalCache().setMaxSize(this._context.getCacheImpl().getActualStats().getMaxSize());
            
            this._context.ExpiryMgr.setCleanInterval(hotConfig.getCleanInterval());
            this._context.getCacheImpl().getInternalCache().setEvictRatio(hotConfig.getEvictRatio() / 100);


            if (this._configurationModified != null) {
                HashMap has = _configurationModified.getInvocationList();
                for (Iterator it = has.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<NEventStart, NEventEnd> object = (Map.Entry<NEventStart, NEventEnd>) it.next();
                    try {
                        object.getKey().hanleEvent(hotConfig);
                    } catch (SocketException socketException) {
                        getCacheLog().Error("Cache.ApplyHotConfig", socketException.getMessage());
                    }
                }
            }
        }
    }

    public final void BalanceDataLoad() throws SuspectedException, TimeoutException, GeneralFailureException {

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.ClusterRead, new OperationContext())) {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }

        _context.getCacheImpl().BalanceDataLoad();
    }

    public final NewHashmap GetOwnerHashMap(tangible.RefObject<Integer> bucketSize) {
        NewHashmap hashMap = _context.getCacheImpl().GetOwnerHashMapTable(bucketSize);
        return hashMap;
    }

    public static float getClientsRequests() {
        tangible.RefObject<Float> tempRef_s_clientsRequests = new tangible.RefObject<Float>(s_clientsRequests);
        java.util.concurrent.atomic.AtomicReference<RefObject<Float>> atom3 = new AtomicReference<RefObject<Float>>(tempRef_s_clientsRequests);
        float tempVar = (atom3.getAndSet(new RefObject<Float>(0F))).argvalue;
        s_clientsRequests = tempRef_s_clientsRequests.argvalue;
        return tempVar;
    }

    public static float getClientsBytesSent() {
        tangible.RefObject<Float> tempRef_s_clientsBytesSent = new tangible.RefObject<Float>(s_clientsBytesSent);
        java.util.concurrent.atomic.AtomicReference<RefObject<Float>> atom = new AtomicReference<RefObject<Float>>(tempRef_s_clientsBytesSent);
        float tempVar = (atom.getAndSet(new RefObject<Float>(0F))).argvalue;
        s_clientsBytesSent = tempRef_s_clientsBytesSent.argvalue;
        return tempVar;
    }

    public static float getClientsBytesRecieved() {
        tangible.RefObject<Float> tempRef_s_clientsBytesRecieved = new tangible.RefObject<Float>(s_clientsBytesRecieved);
        java.util.concurrent.atomic.AtomicReference<RefObject<Float>> atom3 = new AtomicReference<RefObject<Float>>(tempRef_s_clientsBytesRecieved);
        float tempVar = (atom3.getAndSet(new RefObject<Float>(0F))).argvalue;
        s_clientsBytesRecieved = tempRef_s_clientsBytesRecieved.argvalue;
        return tempVar;
    }

    public final com.alachisoft.tayzgrid.config.dom.CacheServerConfig getConfiguration() {
        return _cacheInfo.getConfiguration();
    }

    public final void setConfiguration(com.alachisoft.tayzgrid.config.dom.CacheServerConfig value) {
        _cacheInfo.setConfiguration(value);
    }

    /**
     * Update socket server statistics
     *
     * @param stats
     */
    public final void UpdateSocketServerStats(SocketServerStats stats) {

        tangible.RefObject<Float> tempRef_s_clientsRequests = new tangible.RefObject<Float>(s_clientsRequests);
        tangible.RefObject<Float> tempRef_s_clientsBytesSent = new tangible.RefObject<Float>(s_clientsBytesSent);
        tangible.RefObject<Float> tempRef_s_clientsBytesRecieved = new tangible.RefObject<Float>(s_clientsBytesRecieved);

        java.util.concurrent.atomic.AtomicReference<RefObject<Float>> atom1 = new AtomicReference<RefObject<Float>>(tempRef_s_clientsRequests);
        java.util.concurrent.atomic.AtomicReference<RefObject<Float>> atom2 = new AtomicReference<RefObject<Float>>(tempRef_s_clientsBytesSent);
        java.util.concurrent.atomic.AtomicReference<RefObject<Float>> atom3 = new AtomicReference<RefObject<Float>>(tempRef_s_clientsBytesRecieved);

        atom1.set(new RefObject<Float>((Float) (s_clientsRequests + stats.getRequests())));
        s_clientsRequests = tempRef_s_clientsRequests.argvalue;

        atom2.set(new RefObject<Float>((Float) (s_clientsBytesSent + stats.getBytesSent())));
        s_clientsBytesSent = tempRef_s_clientsBytesSent.argvalue;

        atom3.set(new RefObject<Float>((Float) (s_clientsBytesRecieved + stats.getBytesRecieved())));
        s_clientsBytesRecieved = tempRef_s_clientsBytesRecieved.argvalue;
    }

    /**
     * To Get the string for the TypeInfoMap used in Queries.
     *
     * @return String representation of the TypeInfoMap for this cache.
     */
    public final TypeInfoMap GetTypeInfoMap() {
        if (!getIsRunning()) {
            return null;
        } else {
            return _context.getCacheImpl().getTypeInfoMap();
        }

    }

    public final boolean IsServerNodeIp(Address clientAddress) {
        return _context.getCacheImpl().IsServerNodeIp(clientAddress);
    }

    public final InetAddress getServerJustLeft() {
        return _context.getCacheImpl().getServerJustLeft();
    }

    public final void submitMapReduceTask(MapReduceTask task, String taskId, TaskCallbackInfo callbackInfo,OperationContext operationContext) throws NotSupportedException, GeneralFailureException, OperationFailedException
    {
         // Cache has possibly expired so return default.
        if (!getIsRunning()) {
            return;
        }
        
        try
        {
            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) 
            {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.ClusterRead.getValue(),null))
                {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }
            
            _context.getCacheLog().CriticalInfo("Cache.SubmitMapReduceTask", "MapReduce task with task ID '" + taskId.toUpperCase() + "' is submitted.");
            if(getCacheType().equals("partitioned-server"))
                _context.getCacheImpl().submitMapReduceTask(task, taskId, callbackInfo,operationContext);

        else
            throw new NotSupportedException("this feature is only supported in Partition and Partition of Replica Topologies");
        }catch(GeneralFailureException ex)
        {
            _context.getCacheLog().Error("Cache.submitMapReduceTask()", ex.toString());
            throw ex;
        }
        catch(NotSupportedException ex)
        {
            _context.getCacheLog().Error("Cache.submitMapReduceTask()", ex.toString());
            throw ex;
        }
        catch(OperationFailedException ex)
        {
            _context.getCacheLog().Error("Cache.submitMapReduceTask()", ex.toString());
            throw ex;
        }

    }
    
        
    public final void cancelMapReduceTask(String taskId, boolean cancellAll) throws OperationFailedException
    {
         // Cache has possibly expired so return default.
        if (!getIsRunning()) {
            return;
        }        
        _context.getCacheImpl().cancelMapReduceTask(taskId, cancellAll);

    }
    
    public final java.util.ArrayList getRunningTasks() throws GeneralFailureException
    {
         // Cache has possibly expired so return default.
        if (!getIsRunning()) {
            return null;
        }
        return _context.getCacheImpl().getRunningTasks();
    }
    
    public final TaskStatus getTaskProgress(String taskId) throws GeneralFailureException
    {
        // Cache has possibly expired so return default.
        if (!getIsRunning()) {
            return null;
        }
        return _context.getCacheImpl().getTaskProgress(taskId);
    }
    
    /**
     * Registers the item update/remove or both callbacks with the specified
     * key. Keys should exist before the registration.
     *
     * @param taskID            
     * @param callbackInfo            
     * @param operationContext
     * @throws
     * OperationFailedException
     */
    public final void RegisterTaskNotificationCallback(String taskID, TaskCallbackInfo callbackInfo, OperationContext operationContext) throws
            OperationFailedException {

        if (!getIsRunning()) {
            return;
        }
        if (taskID == null) {
            throw new IllegalArgumentException("TaskID");
        }
        if (callbackInfo == null) {
            throw new IllegalArgumentException();
        }

        try {
            _context.getCacheImpl().RegisterTaskNotification(taskID, callbackInfo, operationContext);
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.RegisterTaskNotificationCallback() ", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.RegisterTaskNotificationCallback() ", inner.toString());
            throw new OperationFailedException("RegisterTaskNotificationCallback failed. Error : " + inner.getMessage(), inner);
        }
    }

    /**
     * Unregisters the item update/remove or both call backs with the specified
     * key.
     *
     * @param taskID
     * @param operationContext
     * @param callbackInfo
     * @throws OperationFailedException
     */
    public final void UnregisterTaskNotificationCallback(String taskID, TaskCallbackInfo callbackInfo, OperationContext operationContext) throws
            OperationFailedException {

        if (!getIsRunning()) {
            return;
        }
        if (taskID == null) {
            throw new IllegalArgumentException("key");
        }
        if (callbackInfo == null) {
            throw new IllegalArgumentException();
        }

        try {
            _context.getCacheImpl().UnregisterTaskNotification(taskID, callbackInfo, operationContext);
        } catch (OperationFailedException inner) {
            if (inner.getIsTracable()) {
                _context.getCacheLog().Error("Cache.UnregisterKeyNotificationCallback() ", inner.toString());
            }
            throw inner;
        } catch (Exception inner) {
            _context.getCacheLog().Error("Cache.UnregisterKeyNotificationCallback()", inner.toString());
            throw new OperationFailedException("UnregisterKeyNotification failed. Error : " + inner.getMessage(), inner);
        }
    }
    
     /**
     * Fired when an item is updated and it has CacheItemUpdate callback
     * attached with it.
     *
     * @param key key of the cache item
     * @param value CallbackEntry containing the callback and actual item
     * @param operationContext
     * @param eventContext
     */
    @Override
    public void OnTaskCallback(Object taskID,Object value,OperationContext operationContext,EventContext eventContext){
        java.util.List taskListeners = (java.util.List) ((value instanceof java.util.List) ? value : null);

        if (taskListeners != null && taskListeners.size() > 0) {
            Object tempVar = taskListeners;
            taskListeners = (java.util.List) ((tempVar instanceof java.util.List) ? tempVar : null);
            for (Iterator it = taskListeners.iterator(); it.hasNext();) {
                TaskCallbackInfo cbInfo = (TaskCallbackInfo) it.next();
                if (_connectedClients != null && _connectedClients.contains(cbInfo.getClient())) {
                    if (_taskNotifListeners != null) {
                        _taskNotifListeners.fireEvents(true, new Object[]{
                            taskID,
                            cbInfo,
                            eventContext
                        });
                    }
                }
            }
        }
    }
    
     public final List<TaskEnumeratorResult> getTaskEnumerator(TaskEnumeratorPointer pointer,OperationContext operationContext) throws Exception
    {      
        if (!getIsRunning()) {
            return null;
        }   
        
        try
        {
            if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) 
            {
                if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.ClusterRead.getValue(),null))
                {
                    _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
            }
            
            if(getCacheType().equals("partitioned-server"))
                return _context.getCacheImpl().getTaskEnumerator(pointer,operationContext);
            else
                throw new NotSupportedException("this feature is only supported in Partition and Partition of Replica Topologies");
        }
        catch(NotSupportedException ex)
        {
            _context.getCacheLog().Error("Cache.getTaskEnumerator()", ex.toString());
            throw ex;
        }

    }
     
      public TaskEnumeratorResult getTaskNextRecord(TaskEnumeratorPointer pointer,OperationContext operationContext) throws Exception
      {
            if (!getIsRunning()) return null;

            try
            {
                if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS))
                {
                    if (!_context.getCacheImpl().IsOperationAllowed(AllowedOperationType.BulkRead, operationContext))
                        _shutDownStatusLatch.WaitForAny((byte)(ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
                }
                return _context.getCacheImpl().getTaskNextRecord(pointer, operationContext);
            }
            catch (Exception ex)
            {
                _context.getCacheLog().Error("getTaskNextRecord operation failed. Error: " + ex.getMessage());
                throw new OperationFailedException("getTaskNextRecord operation failed. Error: " + ex.getMessage(), ex);            }
        }
    
      
      
      public java.util.Map<Object,EntryProcessorResult> invokeEntryProcessor(Object[] keys,EntryProcessor entryProcessor, Object[] arguments,String defaultReadThru,String defaultWriteThru,OperationContext operationContext)
      {
           if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("Cache.invokeEntryProcessor", "");
        }

        if (keys == null) {
            throw new IllegalArgumentException("keys");
        }

        // Cache has possibly expired so do default.
        if (!getIsRunning()) {
            return null;
        }

        if (_shutDownStatusLatch.IsAnyBitsSet(ShutDownStatus.SHUTDOWN_INPROGRESS)) {
            if (!_context.getCacheImpl().IsOperationAllowed(keys, AllowedOperationType.AtomicRead, operationContext)||!_context.getCacheImpl().IsOperationAllowed(keys, AllowedOperationType.AtomicWrite, operationContext)) 
            {
                _shutDownStatusLatch.WaitForAny((byte) (ShutDownStatus.SHUTDOWN_COMPLETED | ShutDownStatus.NONE), getBlockInterval() * 1000);
            }
        }
        if(_context.getEntryProcessorManager()!=null)
        {
            return _context.getEntryProcessorManager().processEntries(keys,entryProcessor,arguments,defaultReadThru,defaultWriteThru,operationContext);
        }
        
        return null;
      }      
      
      
}
