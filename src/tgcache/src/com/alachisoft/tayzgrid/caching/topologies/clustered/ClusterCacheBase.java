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
package com.alachisoft.tayzgrid.caching.topologies.clustered;


import com.alachisoft.tayzgrid.caching.AllowedOperationType;
import com.alachisoft.tayzgrid.caching.AsyncCallbackInfo;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.DataSourceUpdateOptions;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.EventContextFieldName;
import com.alachisoft.tayzgrid.caching.EventContextOperationType;
import com.alachisoft.tayzgrid.caching.EventId;
import com.alachisoft.tayzgrid.caching.EventStatus;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OpCode;
import static com.alachisoft.tayzgrid.caching.OpCode.Add;
import static com.alachisoft.tayzgrid.caching.OpCode.Update;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationID;
import com.alachisoft.tayzgrid.caching.ShutDownServerInfo;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.cacheloader.LoadCacheTask;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DSWriteBehindOperation;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DataSourceCorresponder;
import com.alachisoft.tayzgrid.caching.datasourceproviders.WriteBehindAsyncProcessor;
import com.alachisoft.tayzgrid.caching.datasourceproviders.WriteBehindQueueRequest;
import com.alachisoft.tayzgrid.caching.datasourceproviders.WriteBehindQueueResponse;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.queries.DeleteQueryResultSet;
import com.alachisoft.tayzgrid.caching.queries.QueryDataFilters;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.caching.statistics.NodeStatus;
import com.alachisoft.tayzgrid.caching.statistics.StatisticCounter;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.IClusterEventsListener;
import com.alachisoft.tayzgrid.caching.util.CacheHelper;
import com.alachisoft.tayzgrid.caching.util.ClusterHelper;
import com.alachisoft.tayzgrid.cluster.ChannelClosedException;
import com.alachisoft.tayzgrid.cluster.ChannelException;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.OperationResponse;
import com.alachisoft.tayzgrid.cluster.blocks.GroupRequest;
import com.alachisoft.tayzgrid.cluster.util.Rsp;
import com.alachisoft.tayzgrid.cluster.util.RspList;
import com.alachisoft.tayzgrid.common.GenericCopier;
import com.alachisoft.tayzgrid.common.LockOptions;

import com.alachisoft.tayzgrid.common.datastructures.BalancingResult;

import com.alachisoft.tayzgrid.common.datastructures.BucketStatus;
import com.alachisoft.tayzgrid.common.datastructures.ClusterActivity;
import com.alachisoft.tayzgrid.common.datastructures.DistributionInfoData;
import com.alachisoft.tayzgrid.common.datastructures.DistributionMaps;
import com.alachisoft.tayzgrid.common.datastructures.DistributionMode;
import com.alachisoft.tayzgrid.common.datastructures.HashMapBucket;
import com.alachisoft.tayzgrid.common.datastructures.PartNodeInfo;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.mirroring.CacheNode;
import com.alachisoft.tayzgrid.common.mirroring.GroupInfo;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatistics;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatus;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.monitoring.ServerNode;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.threading.Latch;
import com.alachisoft.tayzgrid.mapreduce.MapReduceOpCodes;
import com.alachisoft.tayzgrid.mapreduce.MapReduceOperation;
import com.alachisoft.tayzgrid.mapreduce.TaskExecutionStatus;
import com.alachisoft.tayzgrid.mapreduce.notification.TaskCallbackInfo;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.InvalidTaskEnumeratorException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.ParserException;
import com.alachisoft.tayzgrid.runtime.exceptions.SecurityException;
import com.alachisoft.tayzgrid.runtime.exceptions.StateTransferInProgressException;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * A class to serve as the base for all clustered cache implementations.
 */
public class ClusterCacheBase extends CacheBase implements IClusterParticipant, IPresenceAnnouncement, IDistributionPolicyMember { //, IMirrorManagementMember

    /**
     * An enumeration that defines the various opcodes to be used by this cache
     * scheme.
     */
    public enum OpCodes {

        /**
         * Periodic update sent to all the servers in the group.
         */
        PeriodicUpdate,
        /**
         * On demand request of current status, similar to periodic req.
         */
        ReqStatus,
        /**
         * Clusterwide Contains(key) request
         */
        Contains,
        /**
         * Clusterwide GetCount() request
         */
        GetCount,
        /**
         * Clusterwide Get(key) request
         */
        Get,
        /**
         * Clusterwide Add(key, obj) request
         */
        Add,
        /**
         * Clusterwide Add(key, obj) request
         */
        Insert,
        /**
         * Clusterwide Remove(key) request
         */
        Remove,
        /**
         * Clusterwide Remove(key[]) request
         */
        RemoveRange,
        /**
         * Clusterwide Clear() request
         */
        Clear,
        /**
         * Clusterwide KeyList() request
         */
        KeyList,
        /**
         * Clusterwide addition notification
         */
        NotifyAdd,
        /**
         * Clusterwide updation notification
         */
        NotifyUpdate,
        /**
         * Clusterwide removal notification
         */
        NotifyRemoval,
        /**
         * Clusterwide cache clear notification
         */
        NotifyClear,
        NotifyCustomNotif,
        /**
         * Clusterwide GetKeys(group, subGroup) request
         */
        GetKeys,
        /**
         * Clusterwide GetData(group, subGroup) request
         */
        GetData,
        /**
         * Clusterwide Remove(group, subGroup) request
         */
        RemoveGroup,
       
        /**
         * Clusterwide Add(key, expirationHint) request
         */
        AddHint,
        
        /**
         * Clusterwide Search(querytext) request
         */
        Search,
        /**
         * Clusterwide SearchEntries(querytext) request
         */
        SearchEntries,
        DeleteQuery,
        /**
         * Custom item update callback request
         */
        NotifyCustomUpdateCallback,
        /**
         * Custom item remove callback request
         */
        NotifyCustomRemoveCallback,
        /**
         * Verify data integrity request
         */
        VerifyDataIntegrity,
        /**
         * Get Data group info request
         */
        GetDataGroupInfo,
        /**
         * Clusterwide GetGroup(key, group, subgroup)
         */
        GetGroup,
        /**
         * Registers callback with an existing item.
         */
        RegisterKeyNotification,
        /**
         * UnRegisters callback with an existing item.
         */
        UnregisterKeyNotification,
        /**
         * Replicates connection string to all nodes.
         */
        ReplicatedConnectionString,
        /**
         * Locks the hashmap buckets.
         */
        LockBuckets,
        /**
         * Release the hashmap buckets.
         */
        ReleaseBuckets,
        AnnounceStateTransfer,
        /**
         * Transfer a bucket from the source to destination.
         */
        TransferBucket,
        /**
         * Sends an acknowledgment for the completion of state transfer.
         */
        AckStateTxfr,
        EnquireTransferableBuckets,
        //<summary>signals end of state txfr</summary>
        SignalEndOfStateTxfr,
        EmptyBucket,
        BalanceNode,
        PublishMap,
        /**
         * Signal write behind task status
         */
        SignalWBTState,
        /**
         * Write behind task is completed
         */
        WBTCompleted,
        /**
         * Notifies to the target node when write behind task is completed
         */
        NotifyWBTResult,
        /**
         * Enqueue write behind operations
         */
        EnqueueWBOp,
        /**
         * Queue needs to be copied from coordinator to new node
         */
        TransferQueue,
       
        UpdateIndice,
        /**
         * Clusterwide bulk removal notification
         */
        NotifyBulkRemoval,
        /**
         * Represents the async replication of invalidated items
         */
        ReplicateOperations,
        LockKey,
        UnLockKey,
        UpdateLockInfo,
        IsLocked,
        GetTag,
        /**
         * Operation for getting keys with specified tags.
         */
        GetKeysByTag,
        
        InStateTransfer,

        AcceptClient,
        DisconnectClient,
        GetSessionCount,
        GetNextChunk,
        RemoveByTag,
        GetFilteredPersistentEvents,
        BlockActivity,
        ExecuteCacheLoader,
        MapReduceOperation,
        NotifyTaskCallback,
        DeadClients;

        public byte getValue() {
            return (byte) this.ordinal();
        }

        public static OpCodes forValue(int value) {
            return values()[value];
        }
    }
    /**
     * The default interval for statistics replication.
     */
    protected long _statsReplInterval = 5000;
    protected static final long forcedViewId = -5; //This id is sent by client to a single node, to direct node to  perform cluster operation possibly on replica as well.
    /**
     * The cluster service.
     */
    private ClusterService _cluster;
    /**
     * The listener of the cluster events like member joined etc. etc.
     */
    private IClusterEventsListener _clusterListener;

    /**
     * The statistics for this cache scheme.
     */
    public ClusterCacheStatistics _stats;
    /**
     * The runtime status of this node.
     */
    public Latch _statusLatch = new Latch();
    /**
     * The initialization status status of this node.
     */
    protected Latch _initialJoiningStatusLatch;
    /**
     * keeps track of all server members
     */
    protected boolean _clusteredExceptions = true;
    protected boolean _asyncOperation = true;
    protected FunctionObjectProvider _functionProvider = new FunctionObjectProvider(10);
    /**
     * The physical storage for this cache
     */
    public CacheBase _internalCache;
    /**
     * Contains CacheNodeInformation
     */
    protected java.util.HashMap _nodeInformationTable;
    protected long _autoBalancingInterval = 180 * 1000; //3 minutes
    /**
     * The threshold that drives when to start auto load balancing on a node.
     * Default value 10 means that auto load balancing will start when the node
     * will have 10% more data than the current average data per node.
     */
    protected int _autoBalancingThreshold = 60; //60% of the average data size per node
    protected boolean _isAutoBalancingEnabled = false;
    protected java.util.ArrayList _hashmap;
    protected java.util.HashMap _bucketsOwnershipMap;
    protected Object _txfrTaskMutex;
    private String _nodeName;
    private int _taskSequenceNumber = 0;
    private java.util.HashMap _wbQueueTransferCorresponders = new java.util.HashMap();
    private boolean _hasDisposed = false;
    public java.util.HashMap _bucketStateTxfrStatus = new java.util.HashMap();
    public java.util.HashMap<Address, ShutDownServerInfo> _shutdownServers = new java.util.HashMap<Address, ShutDownServerInfo>();

    public com.alachisoft.tayzgrid.caching.statistics.StatisticCounter PerfStatsColl;
    protected java.util.HashMap _nodeIdnetities = new java.util.HashMap();

    protected final ReplicationOperation GetClearReplicationOperation(int opCode, Object info) {
        return GetReplicationOperation(opCode, info, 2, null, 0);
    }

    protected final ReplicationOperation GetReplicationOperation(int opCode, Object info, int operationSize, Object[] userPayLoad, long payLoadSize) {

        Map.Entry<Integer, Object> entry = new AbstractMap.SimpleEntry<Integer, Object>(opCode, info);
        ReplicationOperation operation = new ReplicationOperation(entry, operationSize, userPayLoad, payLoadSize);
        return operation;
    }

    protected final long GetClientLastViewId(OperationContext operationContext) {
        long ClientLastViewId = -1;
        Object clientLastViewId = operationContext.GetValueByField(OperationContextFieldName.ClientLastViewId);
        if (clientLastViewId != null) {
            ClientLastViewId = Long.parseLong(clientLastViewId.toString());
        }
        return ClientLastViewId;
    }

    protected final String GetIntendedRecipient(OperationContext operationContext) {
        String IntendedRecipient = "";
        Object intendedRecipient = operationContext.GetValueByField(OperationContextFieldName.IntendedRecipient);
        if (intendedRecipient != null) {
            IntendedRecipient = intendedRecipient.toString();
        }
        return IntendedRecipient;
    }

    /**
     * Returns the cache local to the node, i.e., internal cache.
     */
    @Override
    public CacheBase getInternalCache() {
        return _internalCache;
    }

    /**
     *
     * @return
     */
    @Override
    public TypeInfoMap getTypeInfoMap() {
        return getInternalCache().getTypeInfoMap();
    }

    public final boolean getHasDisposed() {
        return _hasDisposed;
    }

    public final void setHasDisposed(boolean value) {
        _hasDisposed = value;
    }

    /**
     * Get next task's sequence nuumber
     *
     * @return
     */
    protected final int NextSequence() {
        return ++_taskSequenceNumber;
    }

    /**
     * Overloaded constructor. Takes the listener as parameter.
     *
     * @param listener listener of Cache events.
     */
    public ClusterCacheBase(java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context) {
        super(properties, listener, context);
        this._nodeInformationTable = new java.util.HashMap(10);

        _stats = new ClusterCacheStatistics();

        _stats.setInstanceName("");

        _nodeName = System.getProperty("user.name");

    }

    /**
     * Overloaded constructor. Takes the listener as parameter.
     *
     * @param listener listener of Cache events.
     */
    public ClusterCacheBase(java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context, IClusterEventsListener clusterListener) {
        super(properties, listener, context);
        this._nodeInformationTable = new java.util.HashMap(10);

        _stats = new ClusterCacheStatistics();
        _stats.setInstanceName("");

        _clusterListener = clusterListener;

        _nodeName = System.getProperty("user.name");

    }

    /**
     * Perform a dummy get operation on cluster that triggers index updates on
     * all the nodes in the cluster, has no other particular purpose.
     *
     * @param key key of the entry.
     */
    protected void UpdateIndices(Object key, boolean async, OperationContext operationContext) throws OperationFailedException {
        if (getCluster().getServers() != null && getCluster().getServers().size() > 1) {
            if (async) {
                if (_context.AsyncProc != null) {
                    _context.AsyncProc.Enqueue(new UpdateIndicesTask(this, key));
                }
            } else {
                UpdateIndices(key, operationContext);
            }
        }

    }

    public void UpdateLocalBuckets() {
    }

    public void UpdateIndices(Object key, OperationContext operationContext) throws OperationFailedException {
    }

    @Override
    public void NotifyBlockActivity(String uniqueId, long interval) {
        try {
            Function func = new Function(OpCodes.BlockActivity.getValue(), new Object[]{uniqueId, _cluster.getLocalAddress(), interval}, false);
            RspList results = getCluster().Broadcast(func, GroupRequest.GET_ALL, false, Priority.Critical);
        } catch (RuntimeException e) {
            //throw new GeneralFailureException(e.Message, e);
            _context.getCacheLog().Error("ClusterCacheBase.NotifyBlockActivity", e.toString());
        } catch (IOException ioex) {
            _context.getCacheLog().Error("ClusterCacheBase.NotifyBlockActivity", ioex.toString());
        } catch (ClassNotFoundException c) {
            _context.getCacheLog().Error("ClusterCacheBase.NotifyBlockActivity", c.toString());
        }

    }

    @Override
    public void NotifyUnBlockActivity(String uniqueId) {
        Address server = (Address) _cluster.getRenderers().get(getCluster().getLocalAddress());

        if (server != null) {
            if (server.getIpAddress() != null) {
                _context.getCacheRoot().NotifyUnBlockActivityToClients(uniqueId, server.getIpAddress().getHostAddress(), server.getPort());
            }
            _shutdownServers.remove(getCluster().getLocalAddress());
        }
    }

    @Override
    public void NotifyCacheLoaderExecution() {

        if (_cluster.getServers().size() > 1) {
            try {
                Function func = new Function(OpCodes.ExecuteCacheLoader.getValue(), new Object[]{}, true);
                Address server = null;
                if (getCluster().getIsCoordinator()) {
                    server = _cluster.NextCoordinator();
                }

                Object result;
                if (server != null) {
                    _context.getCacheLog().CriticalInfo("ClusterCacheBase.NotifyCacheLoaderExecution", "Notifying next coordinator for cache loader execution.");
                    result = getCluster().SendMessage(server, func, getGetFirstResponse());
                    _context.getCacheLog().CriticalInfo("ClusterCacheBase.NotifyCacheLoaderExecution", "Next coordinator " + server.getIpAddress() + " is notified.");
                }

            } catch (RuntimeException e) {
                _context.getCacheLog().Error("ClusterCacheBase.NotifyCacheLoaderExecution", e.toString());
            } catch (Exception e) {
                _context.getCacheLog().Error("ClusterCacheBase.NotifyCacheLoaderExecution", e.toString());
            }
        }

    }



    @Override
    public void WindUpReplicatorTask() {

    }

    @Override
    public void WaitForReplicatorTask(long interval) {
    }

    @Override
    public java.util.ArrayList<ShutDownServerInfo> GetShutDownServers() {
        java.util.ArrayList<ShutDownServerInfo> ssServers = null;
        if (_shutdownServers != null && _shutdownServers.size() > 0) {
            for (Address adrs : _shutdownServers.keySet()) {
                ssServers.add(_shutdownServers.get(adrs));
            }
        }
        return ssServers;
    }

    @Override
    public boolean IsShutdownServer(Address server) {
        if (_shutdownServers != null) {
            if (_shutdownServers.containsKey(server)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean IsOperationAllowed(Object key, AllowedOperationType opType) {
        return true;
    }

    @Override
    public boolean IsOperationAllowed(Object[] key, AllowedOperationType opType, OperationContext operationContext) {
        return true;
    }

    @Override
    public boolean IsOperationAllowed(AllowedOperationType opType, OperationContext operationContext) {
        return true;
    }

    public void InitializePhase2() throws ChannelClosedException, ChannelException {
        if (getCluster() != null) {
            getCluster().InitializePhase2();
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    @Override
    public void dispose() {
        if (_nodeInformationTable != null) {
            synchronized (_nodeInformationTable) {
                _nodeInformationTable.clear();
            }
        }

        _statusLatch.Clear();
        if (_cluster != null) {
            _cluster.dispose();
            _cluster = null;
        }

        _stats = null;
        super.dispose();
    }

    @Override
    public void StopServices() throws java.lang.InterruptedException {
        if (_cluster != null) {
            _cluster.StopServices();
        }
    }

    public java.util.List getActiveServers() {
        return this.getMembers();
    }

    public final ClusterService getCluster() {
        return _cluster;
    }

    /**
     * The hashtable that contains members and their info.
     */
    protected final java.util.List getMembers() {
        return _cluster.getMembers();
    }

    protected final java.util.List getValidMembers() {
        return _cluster.getValidMembers();
    }

    protected final java.util.List getServers() {
        return _cluster.getServers();
    }
    
    public java.util.List getAllServers()
    {
        return _cluster.getServers();
    }

    /**
     * The local address of this instance.
     */
    protected final Address getLocalAddress() {
        return _cluster.getLocalAddress();
    }

    /**
     * returns the statistics of the Clustered Cache.
     *
     * @return
     */
    @Override
    public CacheStatistics getStatistics() {

        Object tempVar = _stats.clone();
        return (CacheStatistics) ((tempVar instanceof CacheStatistics) ? tempVar : null);

    }

    @Override
    public java.util.ArrayList<CacheNodeStatistics> GetCacheNodeStatistics() throws GeneralFailureException, OperationFailedException, CacheException {
        java.util.ArrayList<CacheNodeStatistics> statistics = new java.util.ArrayList<CacheNodeStatistics>();

        CacheNodeStatistics nodeStats = new CacheNodeStatistics(new ServerNode(null, getCluster().getLocalAddress()));
        nodeStats.setDataSize(getInternalCache().getSize());
        nodeStats.setItemCount(getInternalCache().getCount());
        nodeStats.setTotalCacheSize(getInternalCache().getMaxSize());
        nodeStats.setStatus(GetNodeStatus());
        statistics.add(nodeStats);

        return statistics;
    }

    protected CacheNodeStatus GetNodeStatus() {
        return CacheNodeStatus.Running;
    }

    @Override
    public CacheStatistics getActualStats() {
        return _stats;
    }

    public final String getBridgeSourceCacheId() {
        return _cluster.getBridgeSourceCacheId();
    }

    protected byte getGetFirstResponse() {
        return GroupRequest.GET_FIRST;
    }

    protected byte getGetAllResponses() {
        return GroupRequest.GET_ALL;
    }

    /**
     * Method that allows the object to initialize itself. Passes the property
     * map down the object hierarchy so that other objects may configure
     * themselves as well..
     *
     * @param properties properties collection for this cache.
     */
    @Override
    protected void Initialize(java.util.Map cacheClasses, java.util.Map properties) throws ConfigurationException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            super.Initialize(cacheClasses, properties);
            if (properties.containsKey("stats-repl-interval")) {
                long val = Long.parseLong(tangible.DotNetToJavaStringHelper.trimEnd(String.valueOf(properties.get("stats-repl-interval")), 's', 'e', 'c'));
                if (val < 1) {
                    val = 1;
                }
                if (val > 300) {
                    val = 300;
                }
                val = val * 1000;
                _statsReplInterval = val;
            }

            if (properties.containsKey("data-load-balancing")) {
                java.util.HashMap autoBalancingProps = (java.util.HashMap) properties.get("data-load-balancing");

                if (autoBalancingProps != null && autoBalancingProps.size() > 0) {
                    Iterator ide = autoBalancingProps.entrySet().iterator();
                    Map.Entry KeyValue;
                    while (ide.hasNext()) {
                        KeyValue = (Map.Entry) ide.next();
                        String tempVar = (String) ((KeyValue.getKey() instanceof String) ? KeyValue.getKey() : null);
                        if (tempVar.equals("enabled")) {
                            this._isAutoBalancingEnabled = (Boolean) KeyValue.getValue();

                        } else if (tempVar.equals("auto-balancing-threshold")) {
                            this._autoBalancingThreshold = (Integer) KeyValue.getValue();

                        } else if (tempVar.equals("auto-balancing-interval")) {
                            this._autoBalancingInterval = (Long) KeyValue.getValue();
                            this._autoBalancingInterval *= 1000; //convert into miliseconds
                        }
                    }
                }
            }
            if (properties.containsKey("async-operation")) {
                _asyncOperation = (Boolean) (properties.get("async-operation"));
            }
        } catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    /**
     * Method that allows the object to initialize itself. Passes the property
     * map down the object hierarchy so that other objects may configure
     * themselves as well..
     *
     * @param properties properties collection for this cache.
     */
    protected void InitializeCluster(java.util.Map properties, String channelName, String domain, NodeIdentity identity) throws ConfigurationException, ChannelException,
            InterruptedException, IOException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            _cluster = new ClusterService(_context, this, this); //, this);
            _cluster.setClusterEventsListener(_clusterListener);
            _cluster.Initialize(properties, channelName, domain, identity, _context.getCacheRoot().getIsInProc());
            
            if(_cluster.getLocalAddress() != null)
                        PerfStatsColl.setLocalAddress(_cluster.getLocalAddress().toString() + ":" + _context.getIsStartedAsMirror());
            
        } catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    protected void InitializeCluster(java.util.Map properties, String channelName, String domain, NodeIdentity identity, String userId, String password, boolean twoPhaseInitialization, boolean isReplica) throws
            ConfigurationException, ChannelException, InterruptedException, IOException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            _cluster = new ClusterService(_context, this, this);
            _cluster.setClusterEventsListener(_clusterListener);
            _cluster.Initialize(properties, channelName, domain, identity, userId, password, twoPhaseInitialization, isReplica, _context.getCacheRoot().getIsInProc());
            
            if(_cluster.getLocalAddress() != null)
                  _context.PerfStatsColl.setLocalAddress(_cluster.getLocalAddress().toString() + ":" + _context.getIsStartedAsMirror());

        } catch (ConfigurationException cEx) {
            dispose();
            throw cEx;
        } catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }

    protected final void ConfirmClusterStartUP(boolean isPOR, int retryNumber) {
        _cluster.ConfirmClusterStartUP(isPOR, retryNumber);
    }

    protected final void HasStarted() {
        _cluster.HasStarted();
    }

    public final void InitializeClusterPerformanceCounter(StatisticCounter counterInstance) {
        PerfStatsColl = counterInstance;
        _cluster.InitializeClusterPerformanceCounters(PerfStatsColl.getInstanceName(), PerfStatsColl.getMonitor());
    }

    /**
     * Authenticate the client and see if it is allowed to join the list of
     * valid members.
     *
     * @param address
     * @param identity
     * @return true if the node is valid and belongs to the scheme's cluster
     */
    public boolean AuthenticateNode(Address address, NodeIdentity identity) {
        return true;
    }

    /**
     * Called when a new member joins the group.
     *
     * @param address address of the joining member
     * @param identity additional identity information
     * @return true if the node joined successfully
     */
    public boolean OnMemberJoined(Address address, NodeIdentity identity) throws java.net.UnknownHostException {
        //The first node that joins the cluster is going to be the coordinater,
        //and should execute the cache loader only once.
       
            if (_cluster.getServers().isEmpty() && _context.getCSLMgr() != null) {
                _context.getCSLMgr().setExecuteCacheLoader(true);
            }

        
        if(!_nodeIdnetities.containsKey(address)){
            _nodeIdnetities.put(address,identity);
        }
        
        return true;
    }

    /**
     * Called when an existing member leaves the group.
     *
     * @param address address of the joining member
     * @return true if the node left successfuly
     */
    public boolean OnMemberLeft(Address address, NodeIdentity identity) {
        if (getContext().getCacheLog().getIsInfoEnabled()) {
            getContext().getCacheLog().Info("ClusterCacheBase.OnMemberLeft()", "Member left: " + address);
        }
        if(_nodeIdnetities.containsKey(address)){
            _nodeIdnetities.remove(address);
        }
        if (_context.ExpiryMgr != null) {
            if (_cluster.getIsCoordinator()) {
                _context.ExpiryMgr.setIsCoordinatorNode(true);
            }

           

        }
        if (address != null && _context.getDsMgr() != null && _context.getDsMgr().getIsWriteBehindEnabled()) {
            _context.getDsMgr()._writeBehindAsyncProcess.NodeLeft(address.toString());
        }

        synchronized (_wbQueueTransferCorresponders) {
            if (_wbQueueTransferCorresponders.containsKey(address)) {
                _wbQueueTransferCorresponders.remove(address);
            }
        }

        return true;
    }

    /**
     * Called after the membership has been changed. Lets the members do some
     * member oriented tasks.
     */
    @Override
    public void OnAfterMembershipChange() throws InterruptedException, OperationFailedException {
        if (_cluster.getIsCoordinator() && !_context.getIsStartedAsMirror()) {
            _statusLatch.SetStatusBit(NodeStatus.Coordinator, (byte) 0);
            if (_context.getCSLMgr() != null && _context.getCSLMgr().getIsCacheloaderEnabled() && _context.getCSLMgr().getExecuteCacheLoader()) {
                try {
                    _context.getCSLMgr().setExecuteCacheLoader(false);
                    LoadCacheTask loadCacheThread = new LoadCacheTask(_context.getCSLMgr());
                    _context.getCSLMgr().setTask(loadCacheThread);
                    Thread t = new Thread(loadCacheThread);
                    t.start();

                } catch (Exception e) {
                }

            }
        } else {
            if (_context.getCSLMgr() != null) {
                _context.getCSLMgr().setExecuteCacheLoader(false);
            }
        }

        if (_context.ExpiryMgr != null) {
            _context.ExpiryMgr.setIsCoordinatorNode(_cluster.getIsCoordinator());
        }

        if (getContext().getCacheLog().getIsInfoEnabled()) {
            getContext().getCacheLog().Info("ClusterCacheBase.OnAfterMembershipChange()", "New Coordinator is: " + _cluster.getCoordinator());
        }

        if (_shutdownServers.size() > 0) {
            java.util.ArrayList removedServers = new java.util.ArrayList();
            for (Address addrs : _shutdownServers.keySet()) {
                if (!_cluster.getServers().contains(addrs)) {
                    ShutDownServerInfo info = (ShutDownServerInfo) _shutdownServers.get(addrs);
                    _context.getCacheRoot().NotifyUnBlockActivityToClients(info.getUniqueBlockingId(), info.getRenderedAddress().getIpAddress().getHostAddress(), info.getRenderedAddress().getPort());
                    removedServers.add(addrs);
                }
            }
            for (int i = 0; i < removedServers.size(); i++) {
                _shutdownServers.remove(removedServers.get(i));
            }
        }
        
        Iterator ite = _nodeIdnetities.entrySet().iterator();
        
        StringBuilder sb = new StringBuilder();
        sb.append("");
        while(ite.hasNext()){
            Map.Entry entry = (Map.Entry)ite.next();
            
            if(entry != null){
                Address node = (Address)entry.getKey();
                NodeIdentity identity = (NodeIdentity)entry.getValue();
                sb.append(entry.getKey() + ":" + identity.getIsStartedAsMirror() + ",");
            }
        }
        
        if(_cluster.getLocalAddress().toString().contains("9635")){
            String str = sb.toString();
        }
        
         if(_cluster.getLocalAddress().toString().contains("9636")){
            String str = sb.toString();
        }
        
        _context.PerfStatsColl.setLocalAddress(_cluster.getLocalAddress().toString() +":" + getIsStartedAsMirror());
        _context.PerfStatsColl.setRunningCacheServers(sb.toString().substring(0, sb.length() - 1));
        
        try {
            _context.PerfStatsColl.setPID(getPID());
        } catch (Exception ex) {
            
        } 
    }

    @Override
    public Object HandleClusterMessage(Address src, Function func, tangible.RefObject<Address> destination, tangible.RefObject<Message> replicationMsg) throws
            OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException, Exception, SuspectedException, TimeoutException {
        destination.argvalue = null;
        replicationMsg.argvalue = null;
        return null;
    }

    /**
     * Handles the function requests.
     *
     * @param func
     * @return
     */
    @Override
    public Object HandleClusterMessage(Address src, Function func) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException,
            CacheException, SuspectedException, TimeoutException, Exception {
        OpCodes op = OpCodes.forValue((int) func.getOpcode());

        switch (op) {
            case NotifyCustomNotif:
                return handleCustomNotification(func.getOperand());

            case NotifyCustomRemoveCallback:
                return handleNotifyRemoveCallback(func.getOperand());

            case NotifyCustomUpdateCallback:
                return handleNotifyUpdateCallback(func.getOperand());

            case RegisterKeyNotification:
                return handleRegisterKeyNotification(func.getOperand());

            case UnregisterKeyNotification:
                return handleUnregisterKeyNotification(func.getOperand());

            case ReplicatedConnectionString:
                return handleReplicateConnectionString(func.getOperand());

            case BalanceNode:
                handleBalanceDataLoad(func.getOperand());
                break;
            case PublishMap:
                handlePublishMap(func.getOperand());
                break;
            case SignalWBTState:
                handleSignalTaskState(func.getOperand());
                break;
            case WBTCompleted:
                handleWriteThruTaskCompleted(func.getOperand());
                break;
            case NotifyWBTResult:
                handleNotifyWriteBehindOperationComplete(func.getOperand());
                break;
            case TransferQueue:
                return handleTransferQueue(func.getOperand(), src);
            case EnqueueWBOp:
                handleEnqueueDSOperation(func.getOperand());
                break;

            case GetFilteredPersistentEvents:
                return handleGetFileteredEvents(func.getOperand());

            case BlockActivity:
                return handleBlockActivity(func.getOperand());

            case ExecuteCacheLoader:
                return handleCacheLoaderStartup(func.getOperand());
            case MapReduceOperation:
                return handleMapReduceOperation(func.getOperand());
            case NotifyTaskCallback:
                return handleNotifyTaskCallback(func.getOperand());
            case DeadClients:
                handleDeadClients(func.getOperand());
                break;
        }
        return null;
    }

    public void AutoLoadBalance() throws GeneralFailureException {
    }

    protected final Object handleBlockActivity(Object arguments) {
        Object[] args = (Object[]) arguments;

        ShutDownServerInfo ssInfo = new ShutDownServerInfo();
        ssInfo.setUniqueBlockingId((String) args[0]);
        ssInfo.setBlockServerAddress((Address) args[1]);
        ssInfo.setBlockInterval((Long) args[2]);
        ssInfo.setRenderedAddress((Address) _cluster.getRenderers().get(ssInfo.getBlockServerAddress()));
        _shutdownServers.put(ssInfo.getBlockServerAddress(), ssInfo);

        _context.getCacheRoot().NotifyBlockActivityToClients(ssInfo.getUniqueBlockingId(), ssInfo.getRenderedAddress().getIpAddress().getHostAddress(), ssInfo.getBlockInterval(), ssInfo.getRenderedAddress().getPort());

        return true;
    }

    protected final Object handleCacheLoaderStartup(Object arguments) {
        if (_context.getCSLMgr() != null) {
            _context.getCSLMgr().setIsCacheLoaderTaskIntruppted(true);
        }
        _context.getCacheLog().CriticalInfo("ClusterCacheBase.handleCacheLoaderStartup", "Start Cache Loader notification is recieved on " + getLocalAddress().getIpAddress().toString());

        return null;
    }

      public boolean DetermineClusterStatus() throws OperationFailedException {
        return false;
    }

    /**
     * Fetch state from a cluster member. If the node is the coordinator there
     * is no need to do the state transfer.
     */
    public void EndStateTransfer(Object result) throws OperationFailedException, Exception {
        if (result instanceof Exception) {
            getCacheLog().Error("ClusterCacheBase.EndStateTransfer", " State transfer ended with Exception " + result.toString());
        }

        /**
         * Set the status to fully-functional (Running) and tell everyone about
         * it.
         */
        _statusLatch.SetStatusBit(NodeStatus.Running, NodeStatus.Initializing);
        UpdateCacheStatistics();
        AnnouncePresence(true);
    }

    public final void SignalEndOfStateTxfr(Address dest) throws ChannelClosedException, ChannelException, IOException {
        //Instead of sending in an Object, sending a Serailzable object

        Function fun = new Function(OpCodes.SignalEndOfStateTxfr.getValue(), "no Value");
        if (_cluster != null) {
            _cluster.SendNoReplyMessage(dest, fun);
        }
    }

    public java.util.HashMap LockBuckets(java.util.ArrayList bucketIds) throws SuspectedException, TimeoutException, OperationFailedException {
        return null;
    }

    /**
     * Announces that given buckets are under state transfer and every body in
     * the cluster should know about their statetransfer.
     *
     * @param bucketIds
     */
    public void AnnounceStateTransfer(java.util.ArrayList bucketIds) throws CacheException {
        Clustered_AnnounceStateTransfer(bucketIds);
    }

    protected final void Clustered_AnnounceStateTransfer(java.util.ArrayList bucketIds) throws CacheException {
        Function function = new Function(OpCodes.AnnounceStateTransfer.getValue(), bucketIds, false);
        try {
            getCluster().Broadcast(function, GroupRequest.GET_NONE, false, Priority.Critical);
        } catch (IOException iOException) {
            throw new CacheException(iOException);
        } catch (ClassNotFoundException classNotFoundException) {
            throw new CacheException(classNotFoundException);
        }
    }

    public StateTxfrInfo TransferBucket(java.util.ArrayList bucketIds, Address targetNode, byte transferType, boolean sparsedBuckets, int expectedTxfrId, boolean isBalanceDataLoad) throws
            SuspectedException, TimeoutException, OperationFailedException {
        return Clustered_TransferBucket(targetNode, bucketIds, transferType, sparsedBuckets, expectedTxfrId, isBalanceDataLoad);
    }

    /**
     * Retrieve the list of keys from the cache for the given group or sub
     * group.
     */
    protected final StateTxfrInfo Clustered_TransferBucket(Address targetNode, java.util.ArrayList bucketIds, byte transferType, boolean sparsedBuckets, int expectedTxfrId, boolean isBalanceDataLoad) throws
            SuspectedException, TimeoutException, OperationFailedException {
        try {
            Function func = new Function(OpCodes.TransferBucket.getValue(), new Object[]{
                bucketIds,
                transferType,
                sparsedBuckets,
                expectedTxfrId,
                isBalanceDataLoad
            }, true);
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("ClusteredCacheBase.Clustered_TransferBucket", " Sending request for bucket transfer to " + targetNode);
            }
            Object result = getCluster().SendMessage(targetNode, func, GroupRequest.GET_FIRST, false);
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("ClusteredCacheBase.Clustered_TransferBucket", " Response recieved from " + targetNode);
            }

            OperationResponse opResponse = (OperationResponse) ((result instanceof OperationResponse) ? result : null);
            StateTxfrInfo transferInfo = null;
            if (opResponse != null) {
                transferInfo = (StateTxfrInfo) ((opResponse.SerializablePayload instanceof StateTxfrInfo) ? opResponse.SerializablePayload : null);
                if (transferInfo != null) {
                    if (opResponse.UserPayload != null && opResponse.UserPayload.length > 0) {
                        java.util.HashMap payloadTable = GetAllPayLoads(Arrays.asList(opResponse.UserPayload), transferInfo.getPayLoadCompilationInfo());

                        if (payloadTable != null && transferInfo.data != null) {
                            Object[] keys = new Object[transferInfo.data.keySet().size()];
                            Object[] tempObjArray = transferInfo.data.keySet().toArray();
                            keys = Arrays.copyOf(tempObjArray, tempObjArray.length, Object[].class);
                            java.util.HashMap data = transferInfo.data;
                            for (Object key : keys) {
                                PayloadInfo payloadInfo = (PayloadInfo) ((data.get(key) instanceof PayloadInfo) ? data.get(key) : null);
                                if (payloadInfo != null) {
                                    //Instance of rrayList
                                    java.util.List userPayload = (java.util.List) ((payloadTable.get(payloadInfo.getPayloadIndex()) instanceof java.util.List) ? payloadTable.get(payloadInfo.getPayloadIndex()) : null);
                                    CacheEntry e = payloadInfo.getEntry();
                                    if (e.getValue() == null) {
                                        e.setValue(userPayload.toArray());
                                    } else if (e.getValue() instanceof CallbackEntry) {
                                        Object tempVar = e.getValue();
                                        CallbackEntry cbEntry = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null);
                                        if (cbEntry.getValue() == null) {
                                            e.setValue(userPayload.toArray());
                                        }
                                    }
                                    data.put(key, e);
                                }
                            }
                        }
                    }
                }
            }
            return transferInfo;
        } catch (Exception e) {
            throw new OperationFailedException(e);
        }
    }

    protected static java.util.HashMap GetAllPayLoads(java.util.List userPayLoad, java.util.ArrayList compilationInfo) {
        java.util.HashMap result = new java.util.HashMap();
        int arrayIndex = 0;
        int readIndex = 0;

        VirtualArray payLoadArray = new VirtualArray(userPayLoad);
        com.alachisoft.tayzgrid.common.datastructures.VirtualIndex virtualIndex = new com.alachisoft.tayzgrid.common.datastructures.VirtualIndex();

        for (int i = 0; i < compilationInfo.size(); i++) {
            if ((Long) compilationInfo.get(i) == 0) {
                result.put(i, null);
            } else {
                long l = (Long) compilationInfo.get(i);
                VirtualArray atomicPayLoadArray = new VirtualArray((int) l);
                com.alachisoft.tayzgrid.common.datastructures.VirtualIndex atomicVirtualIndex = new com.alachisoft.tayzgrid.common.datastructures.VirtualIndex();

                VirtualArray.CopyData(payLoadArray, virtualIndex, atomicPayLoadArray, atomicVirtualIndex, (int) atomicPayLoadArray.getSize());
                virtualIndex.IncrementBy((int) atomicPayLoadArray.getSize());
                result.put(i, atomicPayLoadArray.getBaseArray());
            }
        }
        return result;
    }

    public void AckStateTxfrCompleted(Address owner, java.util.ArrayList bucketIds) throws OperationFailedException, GeneralFailureException {
    }

    public void ReleaseBuckets(java.util.ArrayList bucketIds) throws CacheException {
        Clustered_ReleaseBuckets(bucketIds);
    }

    protected final void Clustered_ReleaseBuckets(java.util.ArrayList bucketIds) throws CacheException {
        Function function = new Function(OpCodes.ReleaseBuckets.getValue(), bucketIds, false);
        try {
            getCluster().Broadcast(function, GroupRequest.GET_NONE, false, Priority.Critical);
        } catch (IOException iOException) {
            throw new CacheException(iOException);
        } catch (ClassNotFoundException classNotFoundException) {
            throw new CacheException(classNotFoundException);
        }
    }

    public void StartLogging(java.util.ArrayList bucketIds) {
    }

    public boolean IsBucketsTransferable(java.util.ArrayList bucketIds, Address owner) {
        return true;
    }

    /**
     * Returns the count of local cache items only.
     *
     * @return count of items.
     */
    public long Local_Count() throws GeneralFailureException, OperationFailedException, CacheException {
        if (_internalCache != null) {
            return _internalCache.getCount();
        }

        return 0;
    }

    /**
     * Periodic update (PUSH model), i.e., Publish cache statisitcs so that
     * every node in the cluster gets an idea of the state of every other node.
     */
    @Override
    public boolean AnnouncePresence(boolean urgent) throws OperationFailedException {
        try {
            UpdateCacheStatistics();
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("ClusteredCacheBase.AnnouncePresence()", " announcing presence ;urget " + urgent);
            }
            if (this.getValidMembers().size() > 1) {
                Function func = new Function(OpCodes.PeriodicUpdate.getValue(), _stats.getLocalNode().clone());
                if (!urgent) {
                    getCluster().SendNoReplyMessage(func);
                } else {
                    try {
                        getCluster().Broadcast(func, GroupRequest.GET_NONE, false, Priority.Normal);
                    } catch (IOException iOException) {
                        throw new OperationFailedException(iOException);
                    } catch (ClassNotFoundException classNotFoundException) {
                        throw new OperationFailedException(classNotFoundException);
                    }
                }
            }
            return true;
        } catch (com.alachisoft.tayzgrid.cluster.ChannelClosedException cce) {
            throw new OperationFailedException(cce);
        } catch (com.alachisoft.tayzgrid.cluster.ChannelException ce) {
            throw new OperationFailedException(ce);
        } catch (java.io.IOException io) {
            throw new OperationFailedException(io);
        } catch (Exception ex) {
            throw new OperationFailedException(ex);
        }
    }

    /**
     * Retrieve the object from the cluster.
     *
     * @param key key of the entry.
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return cache entry.
     */
    protected final CacheEntry Clustered_Get(Address address, Object key, OperationContext operationContext) throws TimeoutException, SuspectedException, CacheException {
        CacheEntry retVal = null;
        try {
            Function func = new Function(OpCodes.Get.getValue(), new Object[]{
                key,
                operationContext
            });
            Object result = getCluster().SendMessage(address, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return retVal;
            }
            retVal = (CacheEntry) ((OperationResponse) result).SerializablePayload;
            if (retVal != null) {
                retVal.setValue(((OperationResponse) result).UserPayload);
            }
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    /**
     * Retrieve the object from the cluster.
     *
     * @param key key of the entry.
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return cache entry.
     * @throws SuspectedException
     */
    protected final CacheEntry Clustered_Get(Address address, Object key, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType access, OperationContext operationContext) throws
            TimeoutException, CacheException, SuspectedException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("ClustCacheBase.Get", "");
        }

        CacheEntry retVal = null;
        try {
            Function func = new Function(OpCodes.Get.getValue(), new Object[]{
                key,
                lockId.argvalue,
                lockDate.argvalue,
                access,
                version.argvalue,
                lockExpiration,
                operationContext
            });
            Object result = getCluster().SendMessage(address, func, getGetFirstResponse());
            if (result == null) {
                return retVal;
            }

            Object[] objArr = (Object[]) ((OperationResponse) result).SerializablePayload;
            retVal = (CacheEntry) ((objArr[0] instanceof CacheEntry) ? objArr[0] : null);
            if (retVal != null) {
                retVal.setValue(((OperationResponse) result).UserPayload);
            }
            lockId.argvalue = objArr[1];
            lockDate.argvalue = (java.util.Date) objArr[2];
            version.argvalue = (Long) objArr[3];
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            if (e.getMessage().contains("I am no more the owner of this bucket")) {
                throw new StateTransferException(e.getMessage(), e);
            }
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    protected final LockOptions Clustered_Lock(Address address, Object key, LockExpiration lockExpiration, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws
            SuspectedException, TimeoutException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("ClustCacheBase.Lock", "");
        }
        LockOptions retVal = null;
        try {
            Function func = new Function(OpCodes.LockKey.getValue(), new Object[]{
                key,
                lockId.argvalue,
                lockDate.argvalue,
                lockExpiration,
                operationContext
            });
            Object result = getCluster().SendMessage(address, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return retVal;
            }

            retVal = (LockOptions) ((result instanceof LockOptions) ? result : null);
            if (retVal != null) {
                lockId.argvalue = retVal.getLockId();
                lockDate.argvalue = retVal.getLockDate();
            }
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }

        return retVal;
    }

    protected final LockOptions Clustered_IsLocked(Address address, Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws
            TimeoutException, SuspectedException, GeneralFailureException, CacheException {
        LockOptions retVal = null;
        try {
            Function func = new Function(OpCodes.IsLocked.getValue(), new Object[]{
                key,
                lockId.argvalue,
                lockDate.argvalue,
                operationContext
            });
            Object result = getCluster().SendMessage(address, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return retVal;
            }

            retVal = (LockOptions) ((result instanceof LockOptions) ? result : null);
            if (retVal != null) {
                lockId.argvalue = retVal.getLockId();
                lockDate.argvalue = retVal.getLockDate();
            }
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    protected final void Clustered_UnLock(Address address, Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws TimeoutException,
            SuspectedException,
            GeneralFailureException,
            CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("ClustCacheBase.Unlock", "");
        }
        LockOptions retVal = null;
        try {
            Function func = new Function(OpCodes.UnLockKey.getValue(), new Object[]{
                key,
                lockId,
                isPreemptive,
                operationContext
            });
            getCluster().SendMessage(address, func, GroupRequest.GET_NONE);
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the objects from the cluster.
     *
     * @param keys keys of the entries.
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return cache entry.
     */
    protected final java.util.HashMap Clustered_Get(Address dest, Object[] keys, OperationContext operationContext) throws TimeoutException, SuspectedException,
            GeneralFailureException, OperationFailedException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("ClustCacheBase.GetBlk", "");
        }

        try {
            Function func = new Function(OpCodes.Get.getValue(), new Object[]{
                keys,
                operationContext
            });
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return null;
            }
            return (java.util.HashMap) ((result instanceof java.util.HashMap) ? result : null);
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the objects from the cluster.
     *
     * @param keys keys of the entries.
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return cache entry.
     */
    protected final java.util.HashMap Clustered_Add(Address dest, Object[] keys, OperationContext operationContext) throws SuspectedException, GeneralFailureException,
            TimeoutException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("ClustCacheBase.GetBlk", "");
        }

        try {
            Function func = new Function(OpCodes.Get.getValue(), new Object[]{
                keys,
                operationContext
            });
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return null;
            }
            return (java.util.HashMap) ((result instanceof java.util.HashMap) ? result : null);
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the object from the cluster.
     *
     * @param key key of the entry.
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return cache entry.
     */
    protected CacheEntry Clustered_GetGroup(Address dest, Object key, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws
            TimeoutException, SuspectedException, GeneralFailureException, StateTransferException {
        CacheEntry retVal = null;

        try {
            Function func = new Function(OpCodes.GetGroup.getValue(), new Object[]{
                key,
                group,
                subGroup,
                lockId.argvalue,
                lockDate.argvalue,
                accessType,
                version.argvalue,
                lockExpiration,
                operationContext
            });
            Object result = getCluster().SendMessage(dest, func, getGetFirstResponse());

            if (result == null) {
                return retVal;
            }

            Object[] objArr = (Object[]) ((OperationResponse) result).SerializablePayload;
            retVal = (CacheEntry) ((objArr[0] instanceof CacheEntry) ? objArr[0] : null);
            if (retVal != null) {
                retVal.setValue(((OperationResponse) result).UserPayload);
            }
            lockId.argvalue = objArr[1];
            lockDate.argvalue = (java.util.Date) objArr[2];
            version.argvalue = (Long) objArr[3];
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    /**
     * Retrieve the object from the cluster.
     *
     * @return cache entry.
     */
    protected final java.util.HashMap Clustered_GetGroup(Address dest, Object[] keys, String group, String subGroup, OperationContext operationContext) throws TimeoutException,
            SuspectedException,
            GeneralFailureException,
            CacheException {
        try {
            Function func = new Function(OpCodes.GetGroup.getValue(), new Object[]{
                keys,
                group,
                subGroup,
                operationContext
            });
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST);

            if (result == null) {
                return null;
            }
            return (java.util.HashMap) ((result instanceof java.util.HashMap) ? result : null);
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the list of keys from the cache for the given group or sub
     * group.
     */
    @Override
    public java.util.ArrayList GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException,
            GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.ArrayList list = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        long clientLastViewId = GetClientLastViewId(operationContext);
        if (clientLastViewId == forcedViewId) { //Client wants only me to collect data from cluster and return
            java.util.ArrayList servers = new java.util.ArrayList();
            if (IsInStateTransfer()) { //I have the updated map I can locate the replica
                servers = GetDestInStateTransfer();
            } else {
                servers.addAll(this.getAllServers());
            }
            list = Clustered_GetGroupKeys(servers, group, subGroup, operationContext);
        } else if (!VerifyClientViewId(clientLastViewId)) {
            throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
        } else {
            list = Local_GetGroupKeys(group, subGroup, operationContext);
            if (IsInStateTransfer()) {
                throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
            }
        }

        return list;
    }

    /**
     * Retrieve the list of key and value pairs from the cache for the given
     * group or sub group.
     */
    @Override
    public java.util.HashMap GetGroupData(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException,
            GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap list = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        long clientLastViewId = GetClientLastViewId(operationContext);
        if (clientLastViewId == forcedViewId) { //Client wants only me to collect data from cluster and return
            java.util.ArrayList servers = new java.util.ArrayList();
            if (IsInStateTransfer()) { //I have the updated map I can locate the replica
                servers = GetDestInStateTransfer();
            } else {
                servers.addAll(this.getAllServers());
            }
            list = Clustered_GetGroupData(servers, group, subGroup, operationContext);
        } else if (!VerifyClientViewId(clientLastViewId)) {
            throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
        } else {
            list = Local_GetGroupData(group, subGroup, operationContext);
            if (IsInStateTransfer()) {
                throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
            }
        }

        return list;
    }

    /**
     * Retrieve the list of keys from the cache for the given tags.
     */
    @Override
    public java.util.ArrayList GetTagKeys(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, LockingException,
            GeneralFailureException, StateTransferException,
            CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.ArrayList list = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        long clientLastViewId = GetClientLastViewId(operationContext);
        if (clientLastViewId == forcedViewId) { //Client wants only me to collect data from cluster and return
            java.util.ArrayList servers = new java.util.ArrayList();
            if (IsInStateTransfer()) { //I have the updated map I can locate the replica
                servers = GetDestInStateTransfer();
            } else {
               servers.addAll(this.getAllServers());
            }
            list = Clustered_GetTagKeys(servers, tags, comparisonType, operationContext);
        } else if (!VerifyClientViewId(clientLastViewId)) {
            throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
        } else {
            list = Local_GetTagKeys(tags, comparisonType, operationContext);
            if (IsInStateTransfer()) {
                throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
            }
        }

        return list;
    }

    /**
     * Retrieve the list of key and value pairs from the cache for the given
     * tags.
     */
    @Override
    public java.util.HashMap GetTagData(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, LockingException,
            GeneralFailureException, StateTransferException,
            CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap list = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        long clientLastViewId = GetClientLastViewId(operationContext);
        if (clientLastViewId == forcedViewId) { //Client wants only me to collect data from cluster and return
            java.util.ArrayList servers = new java.util.ArrayList();
            if (IsInStateTransfer()) { //I have the updated map I can locate the replica
                servers = GetDestInStateTransfer();
            } else {
                servers.addAll(this.getAllServers());
            }
            list = Clustered_GetTagData(servers, tags, comparisonType, operationContext);
        } else if (!VerifyClientViewId(clientLastViewId)) {
            throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
        } else {
            list = Local_GetTagData(tags, comparisonType, operationContext);
            if (IsInStateTransfer()) {
                throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
            }
        }

        return list;
    }

    /**
     * Remove the list of key from the cache for the given tags.
     */
    @Override
    public java.util.HashMap Remove(String[] tags, TagComparisonType comparisonType, boolean notify, OperationContext operationContext) throws OperationFailedException,
            LockingException,
            GeneralFailureException,
            CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap result = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        long clientLastViewId = GetClientLastViewId(operationContext);
        if (clientLastViewId == forcedViewId) { //Client wants only me to collect data from cluster and return
            try {
                java.util.ArrayList servers = new java.util.ArrayList();
                if (IsInStateTransfer()) { //I have the updated map I can locate the replica
                    servers = GetDestInStateTransfer();
                } else {
                    servers.addAll(this.getAllServers());
                }

                result = Clustered_RemoveByTag(servers, tags, comparisonType, notify, operationContext);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }

        } else if (!VerifyClientViewId(clientLastViewId)) {
            throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
        } else {
            result = Local_RemoveTag(tags, comparisonType, notify, operationContext);
            if (IsInStateTransfer()) {
                throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
            }
        }

        return result;
    }

    /**
     * Remove the group from cache.
     *
     * @param group group to be removed.
     * @param subGroup subGroup to be removed.
     */
    @Override
    public java.util.HashMap Remove(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException,
            GeneralFailureException, StateTransferException,
            CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap result = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        long clientLastViewId = GetClientLastViewId(operationContext);
        if (clientLastViewId == forcedViewId) { //Client wants only me to collect data from cluster and return
            try {
                java.util.ArrayList servers = new java.util.ArrayList();
                if (IsInStateTransfer()) { //I have the updated map I can locate the replica
                    servers = GetDestInStateTransfer();
                } else {
                    servers.addAll(this.getAllServers());
                }
                result = Clustered_RemoveGroup(servers, group, subGroup, notify, operationContext);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        } else if (!VerifyClientViewId(clientLastViewId)) {
            throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
        } else {
            result = Local_RemoveGroup(group, subGroup, notify, operationContext);
            if (IsInStateTransfer()) {
                throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
            }
        }

        return result;
    }

    /**
     * Retrieve the list of keys from the cache based on the specified query.
     */
    @Override
    public QueryResultSet Search(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException,
            StateTransferException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        QueryResultSet result = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        long clientLastViewId = GetClientLastViewId(operationContext);
        if (clientLastViewId == forcedViewId) { //Client wants only me to collect data from cluster and return
            java.util.ArrayList servers = new java.util.ArrayList();
            if (IsInStateTransfer()) { //I have the updated map I can locate the replica
                servers = GetDestInStateTransfer();
            } else {
                servers.addAll(this.getAllServers());
            }
            result = Clustered_Search(servers, query, values, operationContext);
        } else if (!VerifyClientViewId(clientLastViewId)) {
            throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
        } else {
            try {
                result = Local_Search(query, values, operationContext);
                if (IsInStateTransfer()) {
                    throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
                }
            } catch (StateTransferInProgressException e) {
                throw e;
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        }

        return result;
    }

    /**
     * Retrieve the list of keys and values from the cache based on the
     * specified query.
     */
    @Override
    public QueryResultSet SearchEntries(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, LockingException,
            GeneralFailureException, StateTransferException,
            CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        QueryResultSet result = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        long clientLastViewId = GetClientLastViewId(operationContext);
        if (clientLastViewId == forcedViewId) { //Client wants only me to collect data from cluster and return
            java.util.ArrayList servers = new java.util.ArrayList();
            if (IsInStateTransfer()) { //I have the updated map I can locate the replica
                servers = GetDestInStateTransfer();
            } else {
               servers.addAll(this.getAllServers());
            }
            result = Clustered_SearchEntries(servers, query, values, operationContext);
        } else if (!VerifyClientViewId(clientLastViewId)) {
            throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
        } else {
            try {
                result = Local_SearchEntries(query, values, operationContext);

                if (IsInStateTransfer()) {
                    throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
                }
            } catch (java.lang.Exception le) {
                throw new OperationFailedException(le);
            }
        }

        return result;
    }

    protected QueryResultSet Local_SearchEntriesCQ(String query, java.util.Map values, String clientUniqueId, String clientId, boolean notifyAdd, boolean notifyUpdate, boolean notifyRemove, OperationContext operationContext, QueryDataFilters datafilters) throws
            OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        throw new UnsupportedOperationException();
    }

    protected QueryResultSet Local_SearchCQ(String query, java.util.Map values, String clientUniqueId, String clientId, boolean notifyAdd, boolean notifyUpdate, boolean notifyRemove, OperationContext operationContext, QueryDataFilters datafilters) throws
            OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        throw new UnsupportedOperationException();
    }

    protected QueryResultSet Local_SearchEntries(String query, java.util.Map values, OperationContext operationContext) throws StateTransferException, ParserException,
            java.lang.Exception {
        throw new UnsupportedOperationException();
    }

    protected QueryResultSet Local_Search(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, java.lang.Exception {
        throw new UnsupportedOperationException();
    }

    protected java.util.ArrayList Local_GetTagKeys(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException,
            LockingException,
            GeneralFailureException,
            StateTransferException, CacheException {
        throw new UnsupportedOperationException();
    }

    protected java.util.ArrayList Local_GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException,
            GeneralFailureException, StateTransferException,
            CacheException {
        throw new UnsupportedOperationException();
    }

    protected java.util.HashMap Local_GetGroupData(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException,
            GeneralFailureException, StateTransferException,
            CacheException {
        throw new UnsupportedOperationException();
    }

    protected java.util.HashMap Local_GetTagData(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException,
            LockingException,
            GeneralFailureException,
            StateTransferException, CacheException {
        throw new UnsupportedOperationException();
    }

    protected java.util.HashMap Local_RemoveTag(String[] tags, TagComparisonType tagComparisonType, boolean notify, OperationContext operationContext) throws
            OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        throw new UnsupportedOperationException();
    }

    protected java.util.HashMap Local_RemoveGroup(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException,
            LockingException,
            GeneralFailureException,
            StateTransferException, CacheException {
        throw new UnsupportedOperationException();
    }

    protected boolean VerifyClientViewId(long clientLastViewId) {
        throw new UnsupportedOperationException();
    }

    protected java.util.ArrayList GetDestInStateTransfer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean IsInStateTransfer() {
        return false;
    }

    /**
     * Retrieve the list of keys fron the cache for the given group or sub
     * group.
     */
    protected final java.util.ArrayList Clustered_GetGroupKeys(java.util.List dests, String group, String subGroup, OperationContext operationContext) throws
            GeneralFailureException, CacheException {
        java.util.ArrayList list = null;
        try {
            Function func = new Function(OpCodes.GetKeys.getValue(), new Object[]{
                group,
                subGroup,
                operationContext
            }, false);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return null;
            }
            ClusterHelper.ValidateResponses(results, java.util.ArrayList.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.ArrayList.class);
            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.HashMap tbl = new java.util.HashMap();
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    java.util.ArrayList cList = (java.util.ArrayList) rsp.getValue();
                    if (cList != null) {
                        for (Object key : cList) {
                            tbl.put(key, null);
                        }
                    }
                }
                list = new java.util.ArrayList(tbl.keySet());
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieve the list of keys from the cache for the given group or sub
     * group.
     */
    protected final java.util.HashMap Clustered_GetGroupData(java.util.List dests, String group, String subGroup, OperationContext operationContext) throws
            GeneralFailureException {
        java.util.HashMap table = new java.util.HashMap();
        try {
            Function func = new Function(OpCodes.GetData.getValue(), new Object[]{
                group,
                subGroup,
                operationContext
            }, false);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return null;
            }
            ClusterHelper.ValidateResponses(results, java.util.HashMap.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.HashMap.class);
            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    java.util.Map entries = (java.util.Map) rsp.getValue();
                    if (entries != null) {
                        Iterator ide = entries.entrySet().iterator();
                        Map.Entry KeyValue;
                        while (ide.hasNext()) {
                            KeyValue = (Map.Entry) ide.next();
                            try {
                                table.put(KeyValue.getKey(), KeyValue.getValue());
                            } catch (IllegalArgumentException ex) //Overwrite entry with an updated one
                            {
                                CacheEntry entry = (CacheEntry) ((KeyValue.getValue() instanceof CacheEntry) ? KeyValue.getValue() : null);
                                CacheEntry existingEntry = (CacheEntry) ((table.get(KeyValue.getKey()) instanceof CacheEntry) ? table.get(KeyValue.getKey()) : null);
                                if (entry != null && existingEntry != null) {
                                    if (entry.getVersion() > existingEntry.getVersion()) {
                                        table.put(KeyValue.getKey(), entry);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }

        return table;
    }

    /**
     * Retrieve the list of keys fron the cache for the given tags.
     */
    protected final java.util.ArrayList Clustered_GetTagKeys(java.util.ArrayList dests, String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws
            GeneralFailureException, CacheException {
        java.util.ArrayList keys = null;

        try {
            Function func = new Function(OpCodes.GetKeysByTag.getValue(), new Object[]{
                tags,
                comparisonType,
                operationContext
            }, false);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false, getCluster().getTimeout() * 10);
            if (results == null) {
                return null;
            }
            ClusterHelper.ValidateResponses(results, java.util.ArrayList.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.ArrayList.class);
            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.HashMap tbl = new java.util.HashMap();
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    java.util.Collection cList = (java.util.Collection) rsp.getValue();
                    if (cList != null) {
                        for (Object key : cList) {
                            tbl.put(key, null);
                        }
                    }
                }
                keys = new java.util.ArrayList(tbl.keySet());
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return keys;
    }

    /**
     * Retrieve the list of keys from the cache for the given tags.
     */
    protected final java.util.HashMap Clustered_GetTagData(java.util.ArrayList dests, String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws
            GeneralFailureException, CacheException {
        java.util.HashMap table = new java.util.HashMap();

        try {
            Function func = new Function(OpCodes.GetTag.getValue(), new Object[]{
                tags,
                comparisonType,
                operationContext
            }, false);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false, getCluster().getTimeout() * 10);
            if (results == null) {
                return null;
            }
            ClusterHelper.ValidateResponses(results, java.util.HashMap.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.HashMap.class);
            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    java.util.Map entries = (java.util.Map) rsp.getValue();
                    if (entries != null) {
                        Iterator ide = entries.entrySet().iterator();
                        Map.Entry KeyValue;
                        while (ide.hasNext()) {
                            KeyValue = (Map.Entry) ide.next();
                            Object Key = KeyValue.getKey();
                            Object Value = KeyValue.getValue();
                            try {
                                table.put(Key, Value);
                            } catch (IllegalArgumentException ex) //Overwrite entry with an updated one
                            {
                                CacheEntry entry = (CacheEntry) ((Value instanceof CacheEntry) ? Value : null);
                                CacheEntry existingEntry = (CacheEntry) ((table.get(Key) instanceof CacheEntry) ? table.get(Key) : null);
                                if (entry != null && existingEntry != null) {
                                    if (entry.getVersion() > existingEntry.getVersion()) {
                                        table.put(Key, entry);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return table;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the list of keys from the cache based on the specified query.
     */
    protected final QueryResultSet Clustered_Search(java.util.ArrayList dests, String queryText, java.util.Map values, OperationContext operationContext) throws CacheException {
        QueryResultSet resultSet = new QueryResultSet();

        try {
            Function func = new Function(OpCodes.Search.getValue(), new Object[]{
                queryText, values, operationContext
            }, false);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false, getCluster().getTimeout() * 10);

            if (results == null) {
                return null;
            }
            ClusterHelper.ValidateResponses(results, QueryResultSet.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, QueryResultSet.class);
            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    QueryResultSet cResultSet = (QueryResultSet) rsp.getValue();
                    resultSet.Compile(cResultSet);
                }
                //remove duplicates
                if (resultSet.getSearchEntriesResult()!=null && resultSet.getSearchKeysResult().size() > 0) {
                    java.util.HashMap tbl = new java.util.HashMap();
                    for (Object key : resultSet.getSearchKeysResult()) {
                        tbl.put(key, "");
                    }
                    resultSet.setSearchKeysResult(new java.util.ArrayList(tbl.keySet()));
                }
            }

            return resultSet;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the list of keys and values from the cache based on the
     * specified query.
     */
    protected final QueryResultSet Clustered_SearchEntries(java.util.ArrayList dests, String queryText, java.util.Map values, OperationContext operationContext) throws CacheException {
        QueryResultSet resultSet = new QueryResultSet();

        try {
            Function func = new Function(OpCodes.SearchEntries.getValue(), new Object[]{
                queryText, values, operationContext
            }, false);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false, getCluster().getTimeout() * 10);

            if (results == null) {
                return null;
            }
            ClusterHelper.ValidateResponses(results, QueryResultSet.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, QueryResultSet.class);
            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    QueryResultSet cResultSet = (QueryResultSet) rsp.getValue();
                    resultSet.Compile(cResultSet);
                }
            }

            return resultSet;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the list of keys and values from the cache based on the
     * specified tag and remove from cache.
     */
    protected final java.util.HashMap Clustered_RemoveByTag(java.util.ArrayList dests, String[] tags, TagComparisonType comparisonType, boolean notify, OperationContext operationContext) throws
            GeneralFailureException, LockingException, OperationFailedException, CacheException {
        java.util.ArrayList list = Clustered_GetTagKeys(dests, tags, comparisonType, operationContext);
        return Remove(list.toArray(new Object[0]), ItemRemoveReason.Removed, notify, operationContext);
    }

    /**
     * Retrieve the list of keys and values from the cache based on the
     * specified groups and remove from cache.
     */
    protected final java.util.HashMap Clustered_RemoveGroup(java.util.ArrayList dests, String group, String subGroup, boolean notify, OperationContext operationContext) throws
            GeneralFailureException, LockingException, OperationFailedException, CacheException {
        java.util.ArrayList list = Clustered_GetGroupKeys(dests, group, subGroup, operationContext);
        return Remove(list.toArray(new Object[0]), ItemRemoveReason.Removed, notify, operationContext);
    }

    /**
     * Returns the count of clustered cache items.
     *
     * @return Count of nodes in cluster.
     */
    protected final long Clustered_Count(java.util.List dests) throws GeneralFailureException, CacheException {
        long retVal = 0;
        try {
            Function func = new Function(OpCodes.GetCount.getValue(), null, false);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false);

            ClusterHelper.ValidateResponses(results, Long.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, Long.class);

            java.util.Iterator ia = rspList.iterator();
            while (ia.hasNext()) {
                Rsp rsp = (Rsp) ia.next();
                if (rsp.getValue() != null) {
                    retVal += (Long) rsp.getValue();
                }
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    protected DeleteQueryResultSet Clustered_Delete(java.util.ArrayList dests, String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason ir, OperationContext operationContext)
            throws Exception {
        return Clustered_DeleteQuery(dests, query, values, notify, isUserOperation, ir, operationContext);
    }

    protected DeleteQueryResultSet Clustered_DeleteQuery(java.util.ArrayList dests, String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason ir, OperationContext operationContext)
            throws Exception {
        DeleteQueryResultSet res = new DeleteQueryResultSet();
        try {
            Function func = new Function(OpCodes.DeleteQuery.getValue(), new Object[]{query, values, notify, isUserOperation, ir, operationContext}, false);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return res;
            }
            ClusterHelper.ValidateResponses(results, DeleteQueryResultSet.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, DeleteQueryResultSet.class);
            if (rspList.size() <= 0) {
                return res;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    DeleteQueryResultSet result = (DeleteQueryResultSet) rsp.getValue();

                    if (result != null) {
                        res.setKeysEffectedCount(result.getKeysEffectedCount());                      
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }
        return res;
    }

    @Override
    public DeleteQueryResultSet DeleteQuery(String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason ir, OperationContext operationContext)
            throws Exception {
        DeleteQueryResultSet result = null;

        long clientLastViewId;
        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        try {
            if (_internalCache == null) {
                throw new UnsupportedOperationException();
            }
            clientLastViewId = GetClientLastViewId(operationContext);
            if (clientLastViewId == forcedViewId) //Client wants only me to collect data from cluster and return
            {
                java.util.ArrayList servers = new java.util.ArrayList();
                if (IsInStateTransfer()) //I have the updated map I can locate the replica
                {
                    servers = GetDestInStateTransfer();
                } else {
                    servers.addAll(this.getAllServers());
                }
                result = Clustered_Delete(servers, query, values, notify, isUserOperation, ir, operationContext);
            } else if (!VerifyClientViewId(clientLastViewId)) {
                throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
            } else {
                if (IsInStateTransfer()) {
                    throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
                }
                result = Local_DeleteQuery(query, values, notify, isUserOperation, ir, operationContext);
            }

            return result;
        } finally {
        }
    }

    private DeleteQueryResultSet Local_DeleteQuery(String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason ir, OperationContext operationContext)
            throws Exception {
        return _internalCache.DeleteQuery(query, values, notify, isUserOperation, ir, operationContext);
    }

    public DeleteQueryResultSet handleDeleteQuery(Object info)
            throws Exception {
        DeleteQueryResultSet result = new DeleteQueryResultSet();
        if (_internalCache != null) {
            Object[] data = (Object[]) info;
            return Local_DeleteQuery((String) ((data[0] instanceof String) ? data[0] : null), (java.util.Map) ((data[1] instanceof java.util.Map) ? data[1] : null), (Boolean) data[2], (Boolean) data[3], (ItemRemoveReason) data[4], (OperationContext) ((data[5] instanceof OperationContext) ? data[5] : null));
        }
        return result;
    }

    /**
     * Gets the data group info of the item. Node containing the item will
     * return the data group information.
     *
     * @param key
     * @return Result of the operation On the other ndoe handleGetGroupInfo is
     * called
     */
    public final ClusteredOperationResult Clustered_GetGroupInfo(java.util.List dest, Object key, boolean excludeSelf, OperationContext operationContext) throws
            GeneralFailureException, CacheException {
        ClusteredOperationResult retVal = null;
        try {
            Function func = new Function(OpCodes.GetDataGroupInfo.getValue(), new Object[]{
                key,
                operationContext
            }, excludeSelf);
            RspList results = getCluster().Multicast(dest, func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return retVal;
            }

            ClusterHelper.ValidateResponses(results, GroupInfo.class, getName());

            Rsp rsp = ClusterHelper.GetFirstNonNullRsp(results, GroupInfo.class);
            if (rsp == null) {
                return retVal;
            }
            retVal = new ClusteredOperationResult((Address) rsp.getSender(), rsp.getValue());
        } catch (CacheException e) {
            getContext().getCacheLog().Error("PartitionedServerCacheBase.Clustered_GetGroupInfo()", e.toString());
            throw e;
        } catch (Exception e) {
            getContext().getCacheLog().Error("PartitionedServerCacheBase.Clustered_GetGroupInfo()", e.toString());
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    /**
     * Gets the data group info the items. Node containing items will return a
     * table of Data grop information.
     *
     * @param keys
     * @return /// On the other ndoe handleGetGroupInfo is called
     */
    public final java.util.Collection Clustered_GetGroupInfoBulk(java.util.List dest, Object[] keys, boolean excludeSelf, OperationContext operationContext) throws
            GeneralFailureException, CacheException {
        java.util.ArrayList resultList = null;
        try {
            Function func = new Function(OpCodes.GetDataGroupInfo.getValue(), new Object[]{
                keys,
                operationContext
            }, excludeSelf);
            RspList results = getCluster().Multicast(dest, func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return resultList;
            }

            ClusterHelper.ValidateResponses(results, java.util.HashMap.class, getName());

            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.HashMap.class);

            if (rspList.size() <= 0) {
                return null;
            } else {
                resultList = new java.util.ArrayList();
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    resultList.add(new ClusteredOperationResult((Address) rsp.getSender(), rsp.getValue()));
                }
            }

        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return resultList;
    }

    /**
     * Determines whether the cluster contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return address of the node that contains the specified key; otherwise,
     * null.
     *
     * Determines whether the cluster contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return address of the node that contains the specified key; otherwise,
     * null.
     */
    protected final Address Clustered_Contains(Address dest, Object key, OperationContext operationContext) throws TimeoutException, SuspectedException, GeneralFailureException,
            CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("ClustCacheBase.Cont", "");
        }

        try {
            Function func = new Function(OpCodes.Contains.getValue(), new Object[]{
                key,
                operationContext
            });
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST);

            if (result != null && (Boolean) result) {
                return dest;
            }
            return null;
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Determines whether the cluster contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return address of the node that contains the specified key; otherwise,
     * null.
     */
    protected final java.util.HashMap Clustered_Contains(Address dest, Object[] keys, OperationContext operationContext) throws TimeoutException, SuspectedException,
            GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("ClustCacheBase.ContBlk", "");
        }

        try {
            Function func = new Function(OpCodes.Contains.getValue(), new Object[]{
                keys,
                operationContext
            });
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST);

            if (result != null) {
                return (java.util.HashMap) ((result instanceof java.util.HashMap) ? result : null);
            }
            return null;
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Add the object to the cluster.
     *
     * @param key
     * @param cacheEntry
     * @return
     *
     * This method invokes <see cref="handleInsert"/> on every server-node in
     * the cluster. If the operation fails on any one node the whole operation
     * is considered to have failed and is rolled-back.
     *
     */
    protected CacheInsResultWithEntry Clustered_Insert(Object key, CacheEntry cacheEntry) throws OperationFailedException, TimeoutException {
        return new CacheInsResultWithEntry();
    }

    public final void UpdateStatistics() {
        UpdateCacheStatistics();
    }

    /**
     * Updates the statistics for the cache scheme.
     */
    protected void UpdateCacheStatistics() {
        try {
            if (PerfStatsColl != null) {
//                StringBuilder sb = new StringBuilder();
//                PerfStatsColl.startedAsMirror(Boolean.toString(_stats.getLocalNode().getIsStartedAsMirror()));
//               // PerfStatsColl.isProc(Boolean.toString(_stats.getLocalNode().getIsInproc()));
//                PerfStatsColl.nodeName(_stats.getLocalNode().getAddress().toString());
//                if (_stats.getNodes() != null) {
//                    for (Iterator it = _stats.getNodes().iterator(); it.hasNext();) {
//                        NodeInfo node = (NodeInfo) it.next();
//                        sb.append(node.getAddress()).append(':').append(node.getIsStartedAsMirror()).append(',');
//                    }
//
//                   
//                    PerfStatsColl.setRunningCacheServers(sb.toString().substring(0, sb.length() - 1));
//                }
//                
//                if(_cluster.getLocalAddress() != null)
//                        PerfStatsColl.setLocalAddress(_cluster.getLocalAddress().toString() + ":" + _context.getIsStartedAsMirror());
                    
//                PerfStatsColl.setPID(getPID());
            }
            
            
            
            _stats.getLocalNode().setStatistics(_internalCache.getStatistics());
            _stats.getLocalNode().getStatus().setData(_statusLatch.getStatus().getData());

            _stats.SetServerCounts((int) getServers().size(), (int) getValidMembers().size(), (int) (getMembers().size() - getValidMembers().size()));
            CacheStatistics c = CombineClusterStatistics(_stats);
            _stats.UpdateCount(c.getCount());
            _stats.setHitCount(c.getHitCount());
            _stats.setMissCount(c.getMissCount());
            _stats.setMaxCount(c.getMaxCount());
            _stats.setMaxSize(c.getMaxSize());
            _stats.setSessionCount(c.getSessionCount());
        } catch (Exception e) {
        }
    }

    
    public String getPID() throws ManagementException, TimeoutException, UnsupportedEncodingException, InterruptedException {
        try{
             return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        }
        catch(Exception ex){
            if(getContext().getCacheLog() != null)
                getContext().getCacheLog().Error(ex.toString());
        }
        return "456";
    }
    
    public CacheStatistics CombineClusterStatistics(ClusterCacheStatistics s) {
        return null;
    }

    /**
     * Updates or Adds the objects to the cluster.
     *
     * @param keys
     * @param cacheEntries
     * @return
     *
     * This method either invokes <see cref="handleInsert"/> on any cluster node
     * or invokes <see cref="Local_Insert"/> locally. The choice of the server
     * node is determined by the <see cref="LoadBalancer"/>.
     *
     */
    protected java.util.HashMap Clustered_Insert(Object[] keys, CacheEntry[] cacheEntries, OperationContext operationContext) throws OperationFailedException, TimeoutException,
            SuspectedException, CacheException {
        return null;
    }

    /**
     * Broadcasts a user-defined event across the cluster.
     *
     * @param notifId
     * @param data
     * @param async
     */
    @Override
    public final void SendNotification(Object notifId, Object data) {
        if (getValidMembers().size() > 1) {
            Object info = new Object[]{
                notifId,
                data
            };
            Function func = new Function(OpCodes.NotifyCustomNotif.getValue(), info);
            getCluster().SendNoReplyMessageAsync(func);

            handleCustomNotification(new Object[]{
                notifId,
                data
            });
        } else {
            super.NotifyCustomEvent(notifId, data, false, null, null);
        }
    }

    public final java.util.ArrayList Clustered_ReplicateConnectionString(java.util.List dest, String connString, boolean isSql, boolean excludeSelf) throws GeneralFailureException,
            CacheException {
        try {
            Function func = new Function(OpCodes.ReplicatedConnectionString.getValue(), new Object[]{
                connString,
                isSql
            }, excludeSelf);
            RspList results = getCluster().Multicast(dest, func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return null;
            }

            ClusterHelper.ValidateResponses(results, Boolean.class, getName());

            return ClusterHelper.GetAllNonNullRsp(results, Boolean.class);
        } catch (CacheException e) {
            getContext().getCacheLog().Error("ClusteredCacheBase.Clustered_ReplicateConnectionString()", e.toString());
            throw e;
        } catch (Exception e) {
            getContext().getCacheLog().Error("ClusteredCacheBase.Clustered_ReplicateConnectionString()", e.toString());
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    protected Object handleReplicateConnectionString(Object info) {
        Object[] objs = (Object[]) info;
        String connString = (String) objs[0];
        boolean isSql = (Boolean) objs[1];
        return true;
    }

    protected void handleEnqueueDSOperation(Object info) throws Exception {
        Object[] objs = (Object[]) info;
        if (objs != null && objs.length > 0) {
            if (objs[0] instanceof DSWriteBehindOperation) {
                DSWriteBehindOperation operation = (DSWriteBehindOperation) objs[0];
                _context.getDsMgr().WriteBehind(operation);
            } else if (objs[0] instanceof ArrayList) {
                _context.getDsMgr().WriteBehind((ArrayList) objs[0]);
            }

        }
    }

    /**
     * Hanlder for clustered user-defined notification.
     *
     * @param info
     * @return
     */
    private Object handleCustomNotification(Object info) {
        Object[] objs = (Object[]) info;
        super.NotifyCustomEvent(objs[0], objs[1], false, null, null);
        return null;
    }

    /**
     * Hanlder for clustered item update callback notification.
     *
     * @param info packaged information ///
     * @param entry callback entry
     * @return null
     */
    private Object handleNotifyUpdateCallback(Object info) {
        Object[] objs = (Object[]) info;
        EventContext eventContext = null;
        java.util.ArrayList callbackListeners = (java.util.ArrayList) ((objs[1] instanceof java.util.ArrayList) ? objs[1] : null);
        java.util.HashMap intendedNotifiers = (java.util.HashMap) ((objs[2] instanceof java.util.HashMap) ? objs[2] : null);
        if (objs.length > 3) {
            eventContext = (EventContext) objs[3];
        }

        Iterator ide = intendedNotifiers.entrySet().iterator();
        Map.Entry KeyValue;
        while (ide.hasNext()) {
            KeyValue = (Map.Entry) ide.next();
            Object Key = KeyValue.getKey();
            Object Value = KeyValue.getValue();
            CallbackInfo cbinfo = (CallbackInfo) ((Key instanceof CallbackInfo) ? Key : null);
            Address node = (Address) ((Value instanceof Address) ? Value : null);

            if (node != null && !node.equals(getCluster().getLocalAddress())) {
                callbackListeners.remove(cbinfo);
            }
        }

        NotifyCustomUpdateCallback(objs[0], objs[1], true, null, eventContext);
        return null;
    }

    /**
     * Hanlder for clustered item remove callback notification.
     *
     * @param info packaged information ///
     * @param entry callback entry
     * @return null
     */
    private final Object handleNotifyRemoveCallback(Object info) throws OperationFailedException {
        Object[] objs = (Object[]) info;
        java.util.HashMap intendedNotifiers = (java.util.HashMap) ((objs[2] instanceof java.util.HashMap) ? objs[2] : null);
        OperationContext operationContext = (OperationContext) ((objs[3] instanceof OperationContext) ? objs[3] : null);
        EventContext eventContext = (EventContext) ((objs[4] instanceof EventContext) ? objs[4] : null);
        // a deep clone is required here as callback list is going to be modified while async cluster
        //notification is being sent to the other nodes.
        Object tempVar = eventContext.clone();
        eventContext = (EventContext) ((tempVar instanceof EventContext) ? tempVar : null);

        Object tempVar2 = eventContext.GetValueByField(EventContextFieldName.ItemRemoveCallbackList);
        java.util.ArrayList callbackList = (java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null);

        Iterator ide = intendedNotifiers.entrySet().iterator();
        Map.Entry KeyValue;
        while (ide.hasNext()) {
            KeyValue = (Map.Entry) ide.next();
            Object Key = KeyValue.getKey();
            Object Value = KeyValue.getValue();
            CallbackInfo cbinfo = (CallbackInfo) ((Key instanceof CallbackInfo) ? Key : null);
            Address node = (Address) ((Value instanceof Address) ? Value : null);

            if (node != null && !node.equals(getCluster().getLocalAddress())) {
                callbackList.remove(cbinfo);
            }
        }
        try {
            NotifyCustomRemoveCallback(objs[0], null, (ItemRemoveReason) objs[1], true, operationContext, eventContext);
        } catch (Exception classNotFoundException) {
            throw new OperationFailedException(classNotFoundException);
        }
        return null;
    }

    /**
     * Initializing the cluster_stats object for WMI
     *
     * @param stats
     * @param Name
     */
    public final void postInstrumentatedData(ClusterCacheStatistics stats, String Name) {
        if (Name.indexOf("_BK_NODE") != -1) {
            Name = Name.substring(0, Name.indexOf("_BK_")) + Name.substring(Name.indexOf("_BK_") + Name.length() - Name.indexOf("_BK_"));
        }
    }

    /**
     * Broadcasts an itemadd notifier across the cluster excluding self
     *
     * @param key
     */
    public final void RaiseGeneric(Object data) {
        getCluster().SendNoReplyMessageAsync(data);
    }

    protected Address GetDestinationForFilteredEvents() {
        return getCluster().getCoordinator();
    }

    @Override
    public java.util.ArrayList<com.alachisoft.tayzgrid.persistence.Event> GetFilteredEvents(String clientID, java.util.HashMap events, EventStatus registeredEventStatus) throws SuspectedException, TimeoutException, java.io.IOException, ClassNotFoundException,
            OperationFailedException, Exception, GeneralFailureException {
        try {
            Object[] arguments = new Object[]{clientID, events, registeredEventStatus};
            Address destination = GetDestinationForFilteredEvents();

            if (destination.equals(getCluster().getLocalAddress())) {
                return (java.util.ArrayList<com.alachisoft.tayzgrid.persistence.Event>) handleGetFileteredEvents(arguments);
            } else {
                Function func = new Function(OpCodes.GetFilteredPersistentEvents.getValue(), arguments, false);
                java.util.ArrayList<com.alachisoft.tayzgrid.persistence.Event> filteredEvents = (java.util.ArrayList<com.alachisoft.tayzgrid.persistence.Event>) getCluster().SendMessage(GetDestinationForFilteredEvents(), func, GroupRequest.GET_FIRST);
                return filteredEvents;
            }
        } catch (RuntimeException e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    protected Object handleGetFileteredEvents(Object arguments) {
        Object[] args = (Object[]) arguments;

        String clientID = (String) args[0];
        java.util.HashMap events = (java.util.HashMap) args[1];
        EventStatus registeredEventStatus = (EventStatus) args[2];

        if (_context.PersistenceMgr != null) {
            return _context.PersistenceMgr.GetFilteredEventsList(clientID, events, registeredEventStatus);
        }

        return null;
    }

    /**
     * Broadcasts an itemadd notifier across the cluster
     *
     * @param key
     */
    protected final void RaiseGeneric(Address dest, Object data) {
        getCluster().SendNoReplyMessageAsync(dest, data);
    }

    /**
     * Broadcasts an itemadd notifier across the cluster
     *
     * @param key
     */
    protected final void RaiseItemAddNotifier(Object key, CacheEntry entry, OperationContext context, EventContext eventContext) {
        // If everything went ok!, initiate local and cluster-wide notifications.
        if (getIsItemAddNotifier() && getValidMembers().size() > 1) {

            if (eventContext == null) {
                eventContext = CreateEventContextForGeneralDataEvent(com.alachisoft.tayzgrid.persistence.EventType.ITEM_ADDED_EVENT, null, entry, null);
            }

            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("ReplicatedBase.RaiseItemAddNotifier()", "onitemadded " + key);
            }
            RaiseGeneric(new Function(OpCodes.NotifyAdd.getValue(), new Object[]{key, context, eventContext}));
        }
    }

    protected final EventContext CreateEventContextForGeneralDataEvent(com.alachisoft.tayzgrid.persistence.EventType eventType, OperationContext context, CacheEntry entry, CacheEntry oldEntry) {
        EventContext eventContext = CreateEventContext(context, eventType);
        com.alachisoft.tayzgrid.runtime.events.EventType generalEventType = com.alachisoft.tayzgrid.runtime.events.EventType.ItemAdded;

        switch (eventType) {
            case ITEM_ADDED_EVENT:
                generalEventType = com.alachisoft.tayzgrid.runtime.events.EventType.ItemAdded;
                break;

            case ITEM_UPDATED_EVENT:
                generalEventType = com.alachisoft.tayzgrid.runtime.events.EventType.ItemUpdated;
                break;

            case ITEM_REMOVED_EVENT:
                generalEventType = com.alachisoft.tayzgrid.runtime.events.EventType.ItemRemoved;
                break;
        }

        eventContext.setItem(CacheHelper.CreateCacheEventEntry(GetGeneralDataEventFilter(generalEventType), entry));
        if (oldEntry != null) {
            eventContext.setOldItem(CacheHelper.CreateCacheEventEntry(GetGeneralDataEventFilter(generalEventType), oldEntry));
        }

        return eventContext;
    }

    protected final void FilterEventContextForGeneralDataEvents(com.alachisoft.tayzgrid.runtime.events.EventType eventType, EventContext eventContext) {
        if (eventContext != null && eventContext.getItem() != null) {
            EventDataFilter filter = GetGeneralDataEventFilter(eventType);

            switch (filter) {
                case Metadata:
                    eventContext.getItem().setValue(null);
                    if (eventContext.getOldItem() != null) {
                        eventContext.getOldItem().setValue(null);
                    }
                    break;

                case None:
                    eventContext.setItem(null);
                    eventContext.setOldItem(null);
                    break;
            }
        }
    }

    /**
     * Broadcasts an itemaupdate notifier across the cluster
     *
     * @param key
     */
    protected final void RaiseItemUpdateNotifier(Object key, OperationContext operationContext, EventContext eventcontext) {
        // If everything went ok!, initiate local and cluster-wide notifications.
        if (getIsItemUpdateNotifier() && getValidMembers().size() > 1) {
            RaiseGeneric(new Function(OpCodes.NotifyUpdate.getValue(), new Object[]{
                key,
                operationContext, eventcontext
            }));
        }
    }

    /**
     * Broadcasts an itemremove notifier across the cluster
     *
     * @param packed key or a list of keys to notify
     */
    protected final void RaiseItemRemoveNotifier(Object packed) {
        // If everything went ok!, initiate local and cluster-wide notifications.
        if (getIsItemRemoveNotifier() && getValidMembers().size() > 1) {
            RaiseGeneric(new Function(OpCodes.NotifyRemoval.getValue(), packed));
        }
    }

    /**
     * Broadcasts an itemremove notifier across the cluster
     *
     * @param packed key or a list of keys to notify
     */
    protected final void RaiseAsyncItemRemoveNotifier(Object[] keys, Object[] values, ItemRemoveReason reason, OperationContext operationContext, EventContext[] eventContexts) {
        // If everything went ok!, initiate local and cluster-wide notifications.
        if (getIsItemRemoveNotifier() && getValidMembers().size() > 1) {
            _context.AsyncProc.Enqueue(new AsyncBroadcastNotifyRemoval(this, keys, values, reason, operationContext, eventContexts));
        }
    }

    /**
     * Broadcasts cache clear notifier across the cluster
     *
     * @param key
     */
    protected final void RaiseCacheClearNotifier() {
        // If everything went ok!, initiate local and cluster-wide notifications.
        if (getIsCacheClearNotifier() && getCluster().getIsCoordinator() && (getValidMembers().size() - getServers().size()) > 1) {
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("ReplicatedBase.RaiseCacheClearNotifier()");
            }
            RaiseGeneric(new Function(OpCodes.NotifyClear.getValue(), null));
        }
    }

    /**
     * sends a custom item remove callback to the node from which callback was
     * added.
     *
     * @param dest Addess of the callback node
     * @param packed key,item and actual callback
     */
    private void RaiseCustomRemoveCalbackNotifier(java.util.ArrayList dests, Object[] packed, boolean async) throws OperationFailedException {
        boolean sendLocal = false;
        Object[] objs = (Object[]) ((packed instanceof Object[]) ? packed : null);
        CallbackEntry cbEntry = (CallbackEntry) ((objs[1] instanceof CallbackEntry) ? objs[1] : null);

        if (dests.contains(getCluster().getLocalAddress())) {
            dests.remove(_cluster.getLocalAddress());
            sendLocal = true;
        }

        if (dests.size() > 0 && getValidMembers().size() > 1) {
            if (async) {
                _cluster.SendNoReplyMcastMessageAsync(dests, new Function(OpCodes.NotifyCustomRemoveCallback.getValue(), packed));
            } else {
                try {
                    _cluster.Multicast(dests, new Function(OpCodes.NotifyCustomRemoveCallback.getValue(), packed), GroupRequest.GET_ALL, false);
                } catch (Exception e) {
                    throw new OperationFailedException(e);
                }
            }
        }

        if (sendLocal) {
            handleNotifyRemoveCallback(packed);
        }
    }

    protected final void RaiseAsyncCustomRemoveCalbackNotifier(Object key, CacheEntry entry, ItemRemoveReason reason, OperationContext opContext, EventContext eventContext) {
        try {
            _context.AsyncProc.Enqueue(new AsyncBroadcastCustomNotifyRemoval(this, key, entry, reason, opContext, eventContext));
        } catch (Exception e) {
        }
    }

    /**
     * Reaises the custom item remove call baack.
     *
     * @param key
     * @param cbEntry
     */
    public final void RaiseCustomRemoveCalbackNotifier(Object key, CacheEntry cacheEntry, ItemRemoveReason reason, boolean async, OperationContext operationContext, EventContext eventContext) throws OperationFailedException {
        java.util.ArrayList destinations = null;
        java.util.List nodes = null;
        CallbackEntry cbEntry = (CallbackEntry) cacheEntry.getValue();
        java.util.HashMap intendedNotifiers = new java.util.HashMap();
        if (cbEntry != null && cbEntry.getItemRemoveCallbackListener().size() > 0) {
            if (_stats.getNodes() != null) {
                nodes = _stats.getNodes();
                destinations = new java.util.ArrayList();
                for (Iterator it = cbEntry.getItemRemoveCallbackListener().iterator(); it.hasNext();) {
                    CallbackInfo cbInfo = (CallbackInfo) it.next();
                    int index = nodes.indexOf(new NodeInfo(getCluster().getLocalAddress()));
                    if (index != -1 && ((NodeInfo) nodes.get(index)).getConnectedClients().contains(cbInfo.getClient())) {
                        if (!destinations.contains(getCluster().getLocalAddress())) {
                            destinations.add(getCluster().getLocalAddress());
                        }
                        intendedNotifiers.put(cbInfo, getCluster().getLocalAddress());
                        continue;
                    } else {
                        for (Iterator ite = nodes.iterator(); ite.hasNext();) {
                            NodeInfo nInfo = (NodeInfo) ite.next();
                            if (nInfo.getConnectedClients() != null && nInfo.getConnectedClients().contains(cbInfo.getClient())) {
                                if (!destinations.contains(nInfo.getAddress())) {
                                    destinations.add(nInfo.getAddress());
                                    intendedNotifiers.put(cbInfo, nInfo.getAddress());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (destinations != null && destinations.size() > 0) {

            if (operationContext == null) {
                operationContext = new OperationContext();
            }

            if (eventContext == null || !eventContext.HasEventID(EventContextOperationType.CacheOperation)) {
                eventContext = CreateEventContext(operationContext, com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_CALLBACK);
                eventContext.setItem(CacheHelper.CreateCacheEventEntry(cbEntry.getItemRemoveCallbackListener(), cacheEntry));
                eventContext.Add(EventContextFieldName.ItemRemoveCallbackList, new ArrayList(cbEntry.getItemRemoveCallbackListener()));
            }

            Object[] packed = new Object[]{key, reason, intendedNotifiers, operationContext, eventContext};

            java.util.ArrayList selectedServer = new java.util.ArrayList(1);
            /**
             * [Ata]Incase of partition and partition of replica, there can be
             * same clients connected to multiple server. therefore the
             * destinations list will contain more then one servers. so the
             * callback will be sent to the same client through different server
             * to avoid this, we will check the list for local server. if client
             * is connected with local node, then there is no need to send
             * callback to all other nodes if there is no local node, then we
             * select the first node in the list.
             */
            //if (destinations.Contains(Cluster.LocalAddress)) selectedServer.Add(Cluster.LocalAddress);
            //else selectedServer.Add(destinations[0]);
            RaiseCustomRemoveCalbackNotifier(destinations, packed, async);
        }

    }

    /**
     * Reaises the custom item remove call baack.
     *
     * @param key
     * @param cbEntry
     */
    public final void RaiseCustomRemoveCalbackNotifier(Object key, CacheEntry cacheEntry, ItemRemoveReason reason) throws OperationFailedException {
        RaiseCustomRemoveCalbackNotifier(key, cacheEntry, reason, true, null, null);
    }

    public void RaiseCustomRemoveCalbackNotifier(Object key, CacheEntry cacheEntry, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) throws OperationFailedException {
        RaiseCustomRemoveCalbackNotifier(key, cacheEntry, reason, true, operationContext, eventContext);
    }

    /**
     * sends a custom item update callback to the node from which callback was
     * added.
     *
     * @param dest Addess of the callback node
     * @param packed key,item and actual callback
     */
    private void RaiseCustomUpdateCalbackNotifier(java.util.ArrayList dests, Object packed, EventContext eventContext) {
        // If everything went ok!, initiate local and cluster-wide notifications.
        boolean sendLocal = false;
        Object[] objs = (Object[]) ((packed instanceof Object[]) ? packed : null);
        java.util.List callbackListeners = (java.util.List) ((objs[1] instanceof java.util.List) ? objs[1] : null);

        if (dests.contains(getCluster().getLocalAddress())) {
            dests.remove(getCluster().getLocalAddress());
            sendLocal = true;
        }

        if (getValidMembers().size() > 1) {
            _cluster.SendNoReplyMcastMessageAsync(dests, new Function(OpCodes.NotifyCustomUpdateCallback.getValue(), new Object[]{
                objs[0],
                callbackListeners,
                objs[2], eventContext
            }));
        }

        if (sendLocal) {
            handleNotifyUpdateCallback(new Object[]{
                objs[0],
                callbackListeners,
                objs[2], eventContext
            });
        }
    }

    private void RaiseCustomUpdateCalbackNotifier(java.util.ArrayList dests, Object packed) {
        RaiseCustomUpdateCalbackNotifier(dests, packed, null);
    }

    protected final void RaiseCustomUpdateCalbackNotifier(Object key, java.util.ArrayList itemUpdateCallbackListener) {
        RaiseCustomUpdateCalbackNotifier(key, itemUpdateCallbackListener, null);
    }

    protected final EventContext CreateEventContext(OperationContext operationContext, com.alachisoft.tayzgrid.persistence.EventType eventType) {
        EventContext eventContext = new EventContext();
        OperationID opId = operationContext != null ? operationContext.getOperatoinID() : null;
        //generate event id
        if (operationContext == null || !operationContext.Contains(OperationContextFieldName.EventContext)) //for atomic operations
        {
            eventContext.setEventID(EventId.CreateEventId(opId));
        } else //for bulk
        {
            eventContext.setEventID(((EventContext) operationContext.GetValueByField(OperationContextFieldName.EventContext)).getEventID());
        }

        eventContext.getEventID().setEventType(eventType);
        return eventContext;

    }

    protected EventDataFilter GetGeneralDataEventFilter(com.alachisoft.tayzgrid.runtime.events.EventType eventType) {
        return EventDataFilter.DataWithMetaData;
    }

    protected final void RaiseCustomUpdateCalbackNotifier(Object key, CacheEntry entry, CacheEntry oldEntry, OperationContext operationContext) {
        Object tempVar = oldEntry.getValue();
        CallbackEntry value = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null);
        EventContext eventContext = null;

        if (value != null && value.getItemUpdateCallbackListener() != null && value.getItemUpdateCallbackListener().size() > 0) {
            eventContext = CreateEventContext(operationContext, com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_CALLBACK);

            if (value != null) {
                eventContext.setItem(CacheHelper.CreateCacheEventEntry(value.getItemUpdateCallbackListener(), entry));
                eventContext.setOldItem(CacheHelper.CreateCacheEventEntry(value.getItemUpdateCallbackListener(), oldEntry));

                RaiseCustomUpdateCalbackNotifier(key, value.getItemUpdateCallbackListener(), eventContext);
            }
        }
    }

    /**
     * sends a custom item update callback to the node from which callback was
     * added.
     *
     * @param key key
     * @param cbEntry callback entry
     */
    protected final void RaiseCustomUpdateCalbackNotifier(Object key, java.util.List itemUpdateCallbackListener, EventContext eventContext) {
        java.util.ArrayList destinations = null;
        java.util.List nodes = null;
        java.util.HashMap intendedNotifiers = new java.util.HashMap();
        if (itemUpdateCallbackListener != null && itemUpdateCallbackListener.size() > 0) {
            if (_stats.getNodes() != null) {
                nodes = _stats.getNodes();
                destinations = new java.util.ArrayList();
                for (Iterator it = itemUpdateCallbackListener.iterator(); it.hasNext();) {
                    CallbackInfo cbInfo = (CallbackInfo) it.next();
                    int index = nodes.indexOf(new NodeInfo(getCluster().getLocalAddress()));
                    if (index != -1 && ((NodeInfo) nodes.get(index)).getConnectedClients().contains(cbInfo.getClient())) {
                        if (!destinations.contains(getCluster().getLocalAddress())) {
                            destinations.add(getCluster().getLocalAddress());
                        }
                        intendedNotifiers.put(cbInfo, getCluster().getLocalAddress());
                        continue;
                    } else {
                        for (Iterator ite = nodes.iterator(); ite.hasNext();) {
                            NodeInfo nInfo = (NodeInfo) ite.next();
                            if (nInfo.getConnectedClients() != null && nInfo.getConnectedClients().contains(cbInfo.getClient())) {
                                if (!destinations.contains(nInfo.getAddress())) {
                                    destinations.add(nInfo.getAddress());
                                    intendedNotifiers.put(cbInfo, nInfo.getAddress());
                                }
                            }
                        }
                    }
                }
            }
        }
        if (destinations != null && destinations.size() > 0) {
            Object[] packed = new Object[]{
                key,
                itemUpdateCallbackListener,
                intendedNotifiers
            };
            java.util.ArrayList selectedServer = new java.util.ArrayList(1);
            /**
             * [Ata]Incase of partition and partition of replica, there can be
             * same clients connected to multiple server. therefore the
             * destinations list will contain more then one servers. so the
             * callback will be sent to the same client through different server
             * to avoid this, we will check the list for local server. if client
             * is connected with local node, then there is no need to send
             * callback to all other nodes if there is no local node, then we
             * select the first node in the list.
             */
            //if (destinations.Contains(Cluster.LocalAddress)) selectedServer.Add(Cluster.LocalAddress);
            //else selectedServer.Add(destinations[0]);
            RaiseCustomUpdateCalbackNotifier(destinations, packed, eventContext);
        }
    }

    /**
     *
     *
     * @param result
     * @param writeBehindOperationCompletedCallback
     */
    protected final void RaiseWriteBehindTaskCompleted(OpCode operationCode, Object result, CallbackEntry cbEntry, OperationContext operationContext) throws
            OperationFailedException {
        Address dest = null;
        java.util.List nodes = null;

        if (cbEntry != null && cbEntry.getWriteBehindOperationCompletedCallback() != null) {
            if (_stats.getNodes() != null) {
                Object tempVar = GenericCopier.DeepCopy(_stats.getNodes());
                nodes = (java.util.List) tempVar;
                for (Iterator it = nodes.iterator(); it.hasNext();) {
                    NodeInfo nInfo = (NodeInfo) it.next();
                    Object tempVar2 = cbEntry.getWriteBehindOperationCompletedCallback();
                    AsyncCallbackInfo asyncInfo = (AsyncCallbackInfo) ((tempVar2 instanceof AsyncCallbackInfo) ? tempVar2 : null);
                    if (nInfo.getConnectedClients() != null && nInfo.getConnectedClients().contains(asyncInfo.getClient())) {
                        dest = nInfo.getAddress();
                    }
                }
            }
        }
        if (dest != null) {
            String destinS = "[" + dest.toString() + "]";

            if (dest.equals(getCluster().getLocalAddress())) {
                DoWrite("ClusterCacheBase.RaiseWriteBehindTaskCompleted", "local notify, destinations=" + destinS, operationContext);
                NotifyWriteBehindTaskCompleted(operationCode, ((java.util.HashMap) ((result instanceof java.util.HashMap) ? result : null)), cbEntry, operationContext);
            } else {
                DoWrite("ClusterCacheBase.RaiseWriteBehindTaskCompleted", "clustered notify, destinations=" + destinS, operationContext);

                Function func = new Function(OpCodes.NotifyWBTResult.getValue(), new Object[]{
                    operationCode,
                    result,
                    cbEntry,
                    operationContext
                }, true);
                try {
                    getCluster().SendNoReplyMessage(dest, func);
                } catch (ChannelException channelException) {
                    throw new OperationFailedException(channelException);
                } catch (IOException iOException) {
                    throw new OperationFailedException(iOException);
                }
            }
        }
    }

    @Override
    public void ClientConnected(String client, boolean isInproc) throws OperationFailedException {
        if (_stats != null && _stats.getLocalNode() != null) {
            NodeInfo localNode = (NodeInfo) _stats.getLocalNode();
            if (localNode.getConnectedClients() != null) {
                synchronized (localNode.getConnectedClients()) {
                    if (!localNode.getConnectedClients().contains(client)) {
                        localNode.getConnectedClients().add(client);
                    }
                }

                if (!isInproc) {
                    UpdateClientsCount(localNode.getAddress(), localNode.getConnectedClients().size());
                }
            }
        }
    }

    @Override
    public void ClientDisconnected(String client, boolean isInproc) throws OperationFailedException {
        if (_stats != null && _stats.getLocalNode() != null) {
            NodeInfo localNode = (NodeInfo) _stats.getLocalNode();
            if (localNode.getConnectedClients() != null) {
                synchronized (localNode.getConnectedClients()) {
                    if (localNode.getConnectedClients().contains(client)) {
                        localNode.getConnectedClients().remove(client);
                    }
                }

                if (!isInproc) {
                    UpdateClientsCount(localNode.getAddress(), localNode.getConnectedClients().size());
                }
            }
        }
    }

    @Override
    public void EnqueueDSOperation(DSWriteBehindOperation operation) throws Exception {
        this.CheckDataSourceAvailabilityAndOptions(DataSourceUpdateOptions.WriteBehind);
        EnqueueWriteBehindOperation(operation);
        if (operation.getTaskId() == null) {
            operation.setTaskId(new com.alachisoft.tayzgrid.caching.util.GUID().toString());
        }
        operation.setSource(getCluster().getLocalAddress().toString());
        _context.getDsMgr().WriteBehind(operation);
    }

    @Override
    public void EnqueueDSOperation(ArrayList operationList) throws Exception {
        this.CheckDataSourceAvailabilityAndOptions(DataSourceUpdateOptions.WriteBehind);
        if (operationList == null) {
            return;
        }
        DSWriteBehindOperation operation = null;
        ArrayList operations = new ArrayList();
        for (int i = 0; i < operations.size(); i++)//update taskid and source
        {
            operation = operationList.get(i) instanceof DSWriteBehindOperation ? (DSWriteBehindOperation) operationList.get(i) : null;
            if (operation.getTaskId() == null) {
                operation.setTaskId(new com.alachisoft.tayzgrid.caching.util.GUID().toString());
            }
            operation.setSource(getCluster().getLocalAddress().toString());
            operations.add(operation);
        }
        if (operations.size() > 0) {
            EnqueueWriteBehindOperation(operations);
            _context.getDsMgr().WriteBehind(operations);
        }
    }

    protected void EnqueueWriteBehindOperation(ArrayList operations) {
        //to be implemented by derived classes.
    }

    protected void EnqueueWriteBehindOperation(DSWriteBehindOperation operation) {
        //to be implemented by derived classes.
    }

    /**
     * Must be override to provide the registration of key notifications.
     *
     * @param operand
     */
    public Object handleRegisterKeyNotification(Object operand) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, SuspectedException,
            TimeoutException {
        return null;
    }

    /**
     * Must be override to provide the unregistration of key notifications.
     *
     * @param operand
     */
    public Object handleUnregisterKeyNotification(Object operand) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        return null;
    }

    /**
     * Sends a cluster wide request to resgister the key based notifications.
     *
     * @param key key agains which notificaiton is to be registered.
     * @param updateCallback
     * @param removeCallback
     */
    @Override
    public void RegisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException,
            CacheException,
            LockingException,
            GeneralFailureException,
            SuspectedException,
            TimeoutException {
        Object[] obj = new Object[]{
            key,
            updateCallback,
            removeCallback,
            operationContext
        };
        if (_cluster.getServers().size() > 1) {
            Function fun = new Function(OpCodes.RegisterKeyNotification.getValue(), obj, false);
            try {
                _cluster.BroadcastToMultiple(_cluster.getServers(), fun, GroupRequest.GET_ALL, true);
            } catch (IOException iOException) {
                throw new OperationFailedException(iOException);
            } catch (ClassNotFoundException classNotFoundException) {
                throw new OperationFailedException(classNotFoundException);
            }

        } else {
            handleRegisterKeyNotification(obj);
        }
    }

    @Override
    public void RegisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException,
            CacheException,
            LockingException,
            GeneralFailureException,
            SuspectedException,
            TimeoutException {
        Object[] obj = new Object[]{
            keys,
            updateCallback,
            removeCallback,
            operationContext
        };
        if (_cluster.getServers().size() > 1) {
            Function fun = new Function(OpCodes.RegisterKeyNotification.getValue(), obj, false);
            try {
                _cluster.BroadcastToMultiple(_cluster.getServers(), fun, GroupRequest.GET_ALL, true);
            } catch (IOException iOException) {
                throw new OperationFailedException(iOException);
            } catch (ClassNotFoundException classNotFoundException) {
                throw new OperationFailedException(classNotFoundException);
            }

        } else {
            handleRegisterKeyNotification(obj);

        }
    }

    /**
     * Sends a cluster wide request to unresgister the key based notifications.
     *
     * @param key key agains which notificaiton is to be uregistered.
     * @param updateCallback
     * @param removeCallback
     */
    @Override
    public void UnregisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException,
            LockingException,
            GeneralFailureException,
            CacheException {
        Object[] obj = new Object[]{
            key,
            updateCallback,
            removeCallback,
            operationContext
        };
        if (_cluster.getServers().size() > 1) {
            Function fun = new Function(OpCodes.UnregisterKeyNotification.getValue(), obj, false);
            try {
                _cluster.BroadcastToMultiple(_cluster.getServers(), fun, GroupRequest.GET_ALL, true);
            } catch (IOException iOException) {
                throw new OperationFailedException(iOException);
            } catch (ClassNotFoundException classNotFoundException) {
                throw new OperationFailedException(classNotFoundException);
            }

        } else {
            handleUnregisterKeyNotification(obj);
        }
    }

    @Override
    public void UnregisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws
            OperationFailedException, LockingException, GeneralFailureException, CacheException {
        Object[] obj = new Object[]{
            keys,
            updateCallback,
            removeCallback,
            operationContext
        };
        if (_cluster.getServers().size() > 1) {
            Function fun = new Function(OpCodes.UnregisterKeyNotification.getValue(), obj, false);
            try {
                _cluster.BroadcastToMultiple(_cluster.getServers(), fun, GroupRequest.GET_ALL, true);
            } catch (IOException iOException) {
                throw new OperationFailedException(iOException);
            } catch (ClassNotFoundException classNotFoundException) {
                throw new OperationFailedException(classNotFoundException);
            }

        } else {
            handleUnregisterKeyNotification(obj);

        }
    }

    public CacheNode[] GetMirrorMap() {
        return null;
    }

    public void InstallMirrorMap(CacheNode[] nodes) {
    }

    public DistributionMaps GetDistributionMaps(DistributionInfoData distInfo) {
        return null;
    }

    public java.util.ArrayList getHashMap() {
        return _hashmap;
    }

    public void setHashMap(java.util.ArrayList value) {
        _hashmap = value;
    }

    @Override
    public java.util.HashMap getBucketsOwnershipMap() {
        return _bucketsOwnershipMap;
    }

    public void setBucketsOwnershipMap(java.util.HashMap value) {
        _bucketsOwnershipMap = value;
    }

    public void EmptyBucket(int bucketId) throws LockingException, StateTransferException, OperationFailedException, CacheException, IOException, ClassNotFoundException {
    }

    public void InstallHashMap(DistributionMaps distributionMaps, java.util.List leftMbrs) {
    }

    public final void Clustered_BalanceDataLoad(Address targetNode, Address requestingNode) throws SuspectedException, TimeoutException, GeneralFailureException {
        try {
            Function func = new Function(OpCodes.BalanceNode.getValue(), requestingNode, false);
            getCluster().SendMessage(targetNode, func, GroupRequest.GET_NONE);
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    protected final void handleUpdateLockInfo(Object info) throws StateTransferException, OperationFailedException, LockingException, GeneralFailureException, CacheException {
        Object[] args = (Object[]) ((info instanceof Object[]) ? info : null);
        boolean acquireLock = (Boolean) args[0];
        Object key = args[1];
        Object lockId = args[2];
        java.util.Date lockDate = (java.util.Date) args[3];
        LockExpiration lockExpiration = (LockExpiration) args[4];
        OperationContext operationContext = null;

        if (args.length > 6) {
            operationContext = (OperationContext) ((args[6] instanceof OperationContext) ? args[6] : null);
        } else {
            operationContext = (OperationContext) ((args[5] instanceof OperationContext) ? args[5] : null);
        }

        if (getInternalCache() != null) {
            if (acquireLock) {
                tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
                tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
                getInternalCache().Lock(key, lockExpiration, tempRef_lockId, tempRef_lockDate, operationContext);
                lockId = tempRef_lockId.argvalue;
                lockDate = tempRef_lockDate.argvalue;
            } else {
                boolean isPreemptive = (Boolean) args[5];
                getInternalCache().UnLock(key, lockId, isPreemptive, operationContext);
            }
        }
    }

    private void handleBalanceDataLoad(Object info) throws GeneralFailureException, OperationFailedException {
        Address requestingNode = (Address) ((info instanceof Address) ? info : null);
        PartNodeInfo partNode = new PartNodeInfo(requestingNode, null, false);
        DistributionInfoData distData = new DistributionInfoData(DistributionMode.Manual, ClusterActivity.None, partNode);
        DistributionMaps maps = GetMaps(distData);

        if (maps.getBalancingResult() == BalancingResult.Default) {
            PublishMaps(maps);

        }
    }

    public final void PrintHashMap(java.util.ArrayList HashMap, java.util.HashMap BucketsOwnershipMap, String ModuleName) {
        java.util.ArrayList newMap = HashMap;
        java.util.HashMap newBucketsOwnershipMap = BucketsOwnershipMap;

        String moduleName = ModuleName;

        try {
            //print hashmap
            if (newMap != null) {
                StringBuilder sb = new StringBuilder();
                getCacheLog().CriticalInfo("DisMgr.Install", "---------------- HashMap (Begin)---------------------");
                for (int i = 0; i < newMap.size(); i++) {
                    sb.append(" " + newMap.get(i).toString());
                    if ((i + 1) % 100 == 0) {
                        getCacheLog().CriticalInfo("DisMgr.newMap", sb.toString());
                        sb.delete(0, sb.length());
                    }
                }
                getCacheLog().CriticalInfo("DisMgr.Install", "---------------- HashMap (End)---------------------");
            }

            HashMapBucket bkt;
            if (newBucketsOwnershipMap != null) {
                getCacheLog().CriticalInfo("DisMgr.Install", "---------------- BucketOwnerShipMap (Begin)---------------------");
                Iterator ide = newBucketsOwnershipMap.entrySet().iterator();
                StringBuilder sb = new StringBuilder();
                Map.Entry KeyValue;
                while (ide.hasNext()) {
                    KeyValue = (Map.Entry) ide.next();
                    Object Key = KeyValue.getKey();
                    Object Value = KeyValue.getValue();
                    Address owner = (Address) ((Key instanceof Address) ? Key : null);
                    getCacheLog().CriticalInfo("DisMgr.Install", "--- owner : " + owner + " ----");
                    java.util.ArrayList myMap = (java.util.ArrayList) ((Value instanceof java.util.ArrayList) ? Value : null);
                    int functionBkts = 0, bktsUnderTxfr = 0, bktsNeedTxfr = 0;

                    for (int i = 0; i < myMap.size(); i++) {
                        bkt = (HashMapBucket) ((myMap.get(i) instanceof HashMapBucket) ? myMap.get(i) : null);
                        switch (bkt.getStatus()) {
                            case BucketStatus.Functional:
                                functionBkts++;
                                break;

                            case BucketStatus.UnderStateTxfr:
                                bktsUnderTxfr++;
                                break;

                            case BucketStatus.NeedTransfer:
                                bktsNeedTxfr++;
                                break;
                        }
                        sb.append("  ").append(bkt.toString());
                        if ((i + 1) % 100 == 0) {
                            getCacheLog().CriticalInfo("DisMgr.newBucketsOwnershipMap", sb.toString());
                            sb.delete(0, sb.length());
                        }
                    }
                    if(sb.length()>0)
                    {
                        getCacheLog().CriticalInfo("DisMgr.newBucketsOwnershipMap", sb.toString());
                        sb.delete(0, sb.length());
                    }
                    getCacheLog().CriticalInfo("DisMgr.Install", "[" + owner + "]->" + " buckets owned :" + myMap.size() + "[ functional : " + functionBkts + " ; underStateTxfr : " + bktsUnderTxfr + " ; needTxfr :" + bktsNeedTxfr + " ]");
                }
                getCacheLog().CriticalInfo("DisMgr.Install", "---------------- BucketOwnerShipMap (End)---------------------");
            }
        } catch (Exception e) {
        }
    }

    public final void PublishMaps(DistributionMaps distributionMaps) throws GeneralFailureException {
        Clustered_PublishMaps(distributionMaps);
    }

    public final void Clustered_PublishMaps(DistributionMaps distributionMaps) throws GeneralFailureException {
        try {
            Function func = new Function(OpCodes.PublishMap.getValue(), new Object[]{
                distributionMaps
            }, false);
            getCluster().Broadcast(func, GroupRequest.GET_NONE, false, Priority.Critical);
        } catch (Exception e) {
            getContext().getCacheLog().Error("PartitionedCache.Clustered_PublishMaps()", e.toString());
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    private void handlePublishMap(Object info) {
        try {
            Object[] package_Renamed = (Object[]) ((info instanceof Object[]) ? info : null);

            DistributionMaps distributionMaps = (DistributionMaps) ((package_Renamed[0] instanceof DistributionMaps) ? package_Renamed[0] : null);
            InstallHashMap(distributionMaps, null);
            UpdateLocalBuckets();
            StartStateTransfer(true);
        } catch (Exception e) {
            if (getContext().getCacheLog().getIsErrorEnabled()) {
                getContext().getCacheLog().Error("PartitionedCache.handlePublishMap()", e.toString());
            }
        }
    }

    protected DistributionMaps GetMaps(DistributionInfoData info) {
        return null;
    }

    protected void StartStateTransfer(boolean isBalanceDataLoad) {
    }

    public String GetGroupId(Address affectedNode, boolean isMirror) {
        return "";
    }

    public final WriteBehindQueueResponse TransferQueue(Address coordinator, WriteBehindQueueRequest req) throws SuspectedException, TimeoutException, OperationFailedException {

        Function func = new Function(OpCodes.TransferQueue.getValue(), req);
        Object result = null;
        try {
            result = getCluster().SendMessage(coordinator, func, GroupRequest.GET_FIRST);
        } catch (Exception exception) {
            throw new OperationFailedException(exception);
        }

        return (WriteBehindQueueResponse) ((result instanceof WriteBehindQueueResponse) ? result : null);
    }

    public final Object handleTransferQueue(Object req, Address src) throws Exception {
        if (_context.getDsMgr() != null && _context.getDsMgr().getIsWriteBehindEnabled()) {
            DataSourceCorresponder corr = null;
            synchronized (_wbQueueTransferCorresponders) {

                if (_wbQueueTransferCorresponders.containsKey(src)) {
                    corr = (DataSourceCorresponder) ((_wbQueueTransferCorresponders.get(src) instanceof DataSourceCorresponder) ? _wbQueueTransferCorresponders.get(src) : null);
                } else {
                    corr = new DataSourceCorresponder(_context.getDsMgr(), getContext().getCacheLog());
                    _wbQueueTransferCorresponders.put(src, corr);
                }
            }
            WriteBehindQueueResponse rsp = corr.GetWriteBehindQueue((WriteBehindQueueRequest) ((req instanceof WriteBehindQueueRequest) ? req : null));
            if (rsp != null && rsp.getNextChunkId() == null) {
                _wbQueueTransferCorresponders.remove(src);
            }
            return rsp;
        } else {
            return null;
        }
    }

    /**
     *
     * @param opCode
     * @param result
     * @param taskId
     */
    @Override
    public void NotifyWriteBehindTaskStatus(OpCode opCode, java.util.HashMap result, CallbackEntry cbEntry, String taskId, String providerName, OperationContext operationContext) throws
            OperationFailedException {
        DequeueWriteBehindTask(new String[]{taskId}, providerName, operationContext);

        if (cbEntry != null && cbEntry.getWriteBehindOperationCompletedCallback() != null) {
            RaiseWriteBehindTaskCompleted(opCode, result, cbEntry, operationContext);

        }
    }

    @Override
    public void NotifyWriteBehindTaskStatus(HashMap opResult, String[] taskIds, String provider, OperationContext context) throws OperationFailedException {
        DequeueWriteBehindTask(taskIds, provider, context);
        CallbackEntry cbEntry = null;
        Hashtable status = new Hashtable();
        int i = 0;
        if (opResult != null) {
            for (Object keyValue : opResult.entrySet()) {
                Map.Entry entry = (Map.Entry) keyValue;
                DSWriteBehindOperation dsOperation = entry.getValue() instanceof DSWriteBehindOperation ? (DSWriteBehindOperation) entry.getValue() : null;
                if (dsOperation == null) {
                    continue;
                }
                cbEntry = dsOperation.getEntry().getValue() instanceof CallbackEntry ? (CallbackEntry) dsOperation.getEntry().getValue() : null;
                if (cbEntry != null && cbEntry.getWriteBehindOperationCompletedCallback() != null) {
                    if (dsOperation.getException() != null) {
                        status.put(dsOperation.getKey(), dsOperation.getException());
                    } else {
                        status.put(dsOperation.getKey(), dsOperation.getDSOpState());
                    }
                    try {
                        RaiseWriteBehindTaskCompleted(dsOperation.getOperationCode(), status, cbEntry, context);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    /**
     *
     *
     * @param taskId
     */
    protected void DequeueWriteBehindTask(String[] taskId, String providerName, OperationContext operationContext) throws OperationFailedException {
        //to be implemented by derived classes.
    }

    /**
     *
     * @param operand
     */
    public final void handleWriteThruTaskCompleted(Object operand) throws Exception {
        Object[] data = operand instanceof Object[] ? (Object[]) operand : null;
        String providerName = null;
        String[] taskIds = data[0] instanceof String[] ? (String[]) data[0] : null;
        if (data != null && data.length > 1) {
            providerName = data[1] instanceof String ? (String) data[1] : null;
        }
        if (taskIds != null) {
            _context.getDsMgr().DequeueWriteBehindTask(taskIds, providerName);
        }
    }

    /**
     *
     * @param operand
     */
    public final void handleNotifyWriteBehindOperationComplete(Object operand) {
        Object[] data = (Object[]) ((operand instanceof Object[]) ? operand : null);
        OperationContext operationContext = null;

        if (data.length > 3) {
            operationContext = (OperationContext) ((data[3] instanceof OperationContext) ? data[3] : null);
        }

        super.NotifyWriteBehindTaskCompleted((OpCode) data[0], (java.util.HashMap) ((data[1] instanceof java.util.HashMap) ? data[1] : null), (CallbackEntry) ((data[2] instanceof CallbackEntry) ? data[2] : null), operationContext);
    }

    protected final void ConfigureClusterForRejoiningAsync(java.util.ArrayList nodes) {

        final Object[] args = new Object[1];
        args[0] = nodes;

        //: ThreadPool Conversion
        Thread workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                OnConfigureCluster(args);
            }
        });
        workerThread.start();
    }

    private void OnConfigureCluster(Object arg) {
        try {
            ConfigureClusterForRejoining((java.util.ArrayList) ((arg instanceof java.util.ArrayList) ? arg : null));
        } catch (Exception e) {
        }
    }

    protected final void ConfigureClusterForRejoining(java.util.ArrayList nodes) {
        Event evt = new Event(Event.CONFIGURE_NODE_REJOINING, nodes, Priority.Critical);
        _cluster.ConfigureLocalCluster(evt);
    }

    protected final void SignalTaskState(java.util.List destinations, String taskId, String providerName, OpCode opCode, WriteBehindAsyncProcessor.TaskState state, OperationContext operationContext) throws
            CacheException {
        String destinS = "";
        for (int i = 0; i < destinations.size(); i++) {
            destinS += "[" + ((Address) destinations.get(i)).toString() + "]";
        }
        DoWrite("ClusterCacheBase.SignalTaskState", "taskId=" + taskId + ", state=" + state.toString() + ", destinations=" + destinS, operationContext);

        Object[] operand = new Object[]{
            taskId,
            state,
            providerName,
            opCode
        };
        java.util.ArrayList copyDest = new java.util.ArrayList();//.(java.util.ArrayList)((destinations instanceof java.util.ArrayList) ? destinations : null);
        copyDest.addAll(destinations);

        if (copyDest.contains(getCluster().getLocalAddress())) {
            copyDest.remove(getCluster().getLocalAddress());
            try {
                handleSignalTaskState(operand);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        }

        if (copyDest.size() > 0) {
            Function func = new Function(OpCodes.SignalWBTState.getValue(), new Object[]{
                taskId,
                state,
                providerName,
                opCode
            }, true);
            try {
                getCluster().SendNoReplyMulticastMessage(copyDest, func, false);
            } catch (ChannelException channelException) {
                throw new OperationFailedException(channelException);
            } catch (IOException iOException) {
                throw new OperationFailedException(iOException);
            }
        }
    }

    protected final void SignalBulkTaskState(java.util.List destinations, String taskId, String providerName, java.util.HashMap table, OpCode opCode, WriteBehindAsyncProcessor.TaskState state) throws
            CacheException {
        Object[] operand = new Object[]{
            taskId,
            state,
            table,
            providerName,
            opCode
        };
        java.util.ArrayList copyDest = new java.util.ArrayList(destinations);//(java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);

        if (copyDest.contains(getCluster().getLocalAddress())) {
            copyDest.remove(getCluster().getLocalAddress());
            try {
                handleSignalTaskState(operand);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        }

        if (copyDest.size() > 0) {
            Function func = new Function(OpCodes.SignalWBTState.getValue(), operand, true);
            try {
                getCluster().SendNoReplyMulticastMessage(copyDest, func, false);
            } catch (ChannelException channelException) {
                throw new OperationFailedException(channelException);
            } catch (IOException iOException) {
                throw new OperationFailedException(iOException);
            }
        }
    }

    protected final void handleSignalTaskState(Object operand) throws Exception {
        if (_context.getDsMgr() != null && _context.getDsMgr().getIsWriteBehindEnabled()) {
            Object[] data = (Object[]) ((operand instanceof Object[]) ? operand : null);
            String taskId = null;
            String providerName = null;
            OpCode code;
            WriteBehindAsyncProcessor.TaskState state;
            java.util.HashMap table = null;

            if (data.length == 4) {
                taskId = (String) data[0];
                state = (WriteBehindAsyncProcessor.TaskState) data[1];
                providerName = (String) data[2];
                code = (OpCode) data[3];
                _context.getDsMgr().SetState(taskId, providerName, code, state);
            } else if (data.length == 5) {
                taskId = (String) data[0];
                state = (WriteBehindAsyncProcessor.TaskState) data[1];
                table = (java.util.HashMap) data[2];
                providerName = (String) data[3];
                code = (OpCode) data[4];

                _context.getDsMgr().SetState(taskId, providerName, code, state, table);
            }
        }
    }

    /**
     *
     * @param source
     * @param key
     * @param cacheEntry
     * @param taskId
     * @param operationCode
     */
    protected final void AddWriteBehindTask(Address source, Object key, CacheEntry cacheEntry, String taskId, OpCode operationCode, OperationContext operationContext) throws
            OperationFailedException {
        AddWriteBehindTask(source, key, cacheEntry, taskId, operationCode, WriteBehindAsyncProcessor.TaskState.Waite, operationContext);
    }

    protected final void AddWriteBehindTask(Address source, Object key, CacheEntry cacheEntry, String taskId, OpCode operationCode, WriteBehindAsyncProcessor.TaskState state, OperationContext operationContext) throws
            OperationFailedException {
        this.CheckDataSourceAvailabilityAndOptions(DataSourceUpdateOptions.WriteBehind);

        String coord = source != null ? source.toString() : null;

        DoWrite("ClusterCacheBase.AddWriteBehindTask", "taskId=" + taskId + ", source=" + coord, operationContext);
        try {
            _context.getDsMgr().WriteBehind(_context.getCacheImpl(), key, cacheEntry, coord, taskId, cacheEntry.getProviderName(), operationCode, state);
        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage());
        }
    }

    /**
     *
     * @param source
     * @param table
     * @param cbEntry
     * @param taskId
     * @param operationCode
     */
    protected final void AddWriteBehindTask(Address source, java.util.HashMap table, CallbackEntry cbEntry, String taskId, OpCode operationCode) throws OperationFailedException {
        AddWriteBehindTask(source, table, cbEntry, taskId, operationCode, WriteBehindAsyncProcessor.TaskState.Waite);
    }

    protected final void AddWriteBehindTask(Address source, java.util.HashMap table, CallbackEntry cbEntry, String taskId, OpCode operationCode, WriteBehindAsyncProcessor.TaskState state) throws
            OperationFailedException {
        this.CheckDataSourceAvailabilityAndOptions(DataSourceUpdateOptions.WriteBehind);

        Object[] keys = new Object[table.size()];
        Object[] values = null;
        CacheEntry[] entries = null;
        String providerName = null;
        try {
            int i = 0;
            switch (operationCode) {
                case Add:
                case Update:
                    values = new Object[table.size()];
                    entries = new CacheEntry[table.size()];
                    for (Iterator it = table.entrySet().iterator(); it.hasNext();) {
                        Map.Entry current = (Map.Entry) it.next();
                        keys[i] = current.getKey();
                        Object entry = ((CacheEntry) current.getValue()).getValue();
                        providerName = ((CacheEntry) current.getValue()).getProviderName();
                        entries[i] = (CacheEntry) current.getValue();
                        if (entry instanceof CallbackEntry) {
                            entry = ((CallbackEntry) entry).getValue();
                        }
                        
                        Object tempVar = ((CacheEntry) table.get(keys[i])).getValue();
                        CallbackEntry callback = (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null);
                        if (callback != null) {
                            entries[i] = new CacheEntry(callback, null, null);
                        }
                        i++;
                    }

                    break;
                case Remove:
                    entries = new CacheEntry[table.size()];
                    for (Iterator it = table.entrySet().iterator(); it.hasNext();) {
                        Map.Entry current = (Map.Entry) it.next();
                        if (current.getValue() instanceof CacheEntry) {
                            keys[i] = current.getKey();
                            entries[i] = (CacheEntry) ((current.getValue() instanceof CacheEntry) ? current.getValue() : null);
                            providerName = entries[i].getProviderName();

                            i++;
                        }
                    }

                    if (entries.length > 0 && cbEntry != null) {
                        for (int j = 0; j < entries.length; j++) {
                            if (entries[j].getValue() instanceof CallbackEntry) {
                                ((CallbackEntry) entries[j].getValue()).setWriteBehindOperationCompletedCallback(cbEntry.getWriteBehindOperationCompletedCallback());
                            } else {
                                cbEntry.setValue(entries[j].getValue());
                                entries[j].setValue(cbEntry);
                            }
                        }
                    }
                    break;
            }
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        String coord = source != null ? source.toString() : null;
        try {
            _context.getDsMgr().WriteBehind(_context.getCacheImpl(), keys, values, entries, source.toString(), taskId, providerName, operationCode, state);
        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage());
        }
    }

    private void CheckDataSourceAvailabilityAndOptions(DataSourceUpdateOptions updateOpts) throws OperationFailedException {
        if (updateOpts != DataSourceUpdateOptions.None) {
            if (_context.getDsMgr() != null && ((_context.getDsMgr().getIsWriteThruEnabled() && updateOpts == DataSourceUpdateOptions.WriteThru)
                    || (updateOpts == DataSourceUpdateOptions.WriteBehind))) {
                return;
            }

            throw new OperationFailedException("Backing source not available. Verify backing source settings");
        }
    }

    /**
     * Get a hashtable containg information off all nodes connected
     */
    public final java.util.HashMap getNodesInformation() {
        synchronized (_nodeInformationTable) {
            return this._nodeInformationTable;
        }
    }

    /**
     * Add server node information to table only if port is not -1 (inproc
     * cache) and there is no previous entry
     *
     * @param ipAddress ip address of server node
     * @param rendrerPort socket server port
     * @param connectedClients numbers of client connected to that server
     */
    protected final void AddServerInformation(Address ipAddress, int rendrerPort, int connectedClients) {
        if (rendrerPort == 0) {
            return;
        }
        ClusterNodeInformation nodeInfo;
        synchronized (_nodeInformationTable) {
            String address = ipAddress.getIpAddress().getHostAddress();
            if (!_nodeInformationTable.containsKey(address)) {
                nodeInfo = new ClusterNodeInformation(rendrerPort, connectedClients);
                _nodeInformationTable.put(address, nodeInfo);
            } else {
                nodeInfo = (ClusterNodeInformation) ((_nodeInformationTable.get(address) instanceof ClusterNodeInformation) ? _nodeInformationTable.get(address) : null);
                nodeInfo.AddRef();
            }
        }
    }

    /**
     * Update the number of clients connected to server node
     *
     * @param ipAddress ip address of server node
     * @param clientsConnected new clients connected count
     */
    protected final void UpdateClientsCount(Address ipAddress, int clientsConnected) {
        synchronized (_nodeInformationTable) {
            String address = ipAddress.getIpAddress().getHostAddress();
            if (_nodeInformationTable.containsKey(address)) {
                ClusterNodeInformation nodeInfo = (ClusterNodeInformation) ((_nodeInformationTable.get(address) instanceof ClusterNodeInformation) ? _nodeInformationTable.get(address) : null);
                if (clientsConnected > 0) {
                    //the partition to which clients connected is an active partition.
                    nodeInfo.setActivePartition(ipAddress);
                    ((ClusterNodeInformation) _nodeInformationTable.get(address)).setConnectedClients(clientsConnected);
                }
            }
        }
    }

    /**
     * Removes server node information from table
     *
     * @param ipAddress ip address of server node
     */
    protected final void RemoveServerInformation(Address ipAddress, int rendererPort) {
        if (rendererPort == 0) {
            return;
        }
        String address = ipAddress.getIpAddress().getHostAddress();
        synchronized (_nodeInformationTable) {
            if (_nodeInformationTable.containsKey(address)) {
                ClusterNodeInformation nodeInfo = (ClusterNodeInformation) ((_nodeInformationTable.get(address) instanceof ClusterNodeInformation) ? _nodeInformationTable.get(address) : null);
                //we reinitialize the client count.
                if (nodeInfo.getActivePartition() != null && nodeInfo.getActivePartition().equals(ipAddress)) {
                    nodeInfo.setConnectedClients(0);
                }

                if (nodeInfo.RemoveRef()) {
                    getNodesInformation().remove(address);
                }
            }
        }
    }



    /**
     *
     * @param task
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException
     */
    @Override
    public void submitMapReduceTask(MapReduceTask task, String taskId, TaskCallbackInfo callbackInfo, OperationContext operationContext) throws GeneralFailureException {
        try {

            if (IsInStateTransfer()) {
                throw new GeneralFailureException("Cluster Is In StateTransfer");
            }

            //=============================Get Sequence ID For Task==========
            MapReduceOperation operation = new MapReduceOperation();
            operation.setOpCode(MapReduceOpCodes.GetTaskSequence);

            Function sequenceFunc = new Function(OpCodes.MapReduceOperation.getValue(), operation, false);
            Object result = getCluster().SendMessage(getCluster().getCoordinator(), sequenceFunc, GroupRequest.GET_FIRST);
            Long sequenceID;
            if (result == null) {
                throw new GeneralFailureException("Task Submission Failed");
            }
            sequenceID = (Long) result;

            //========================== Submit Task ======================
            MapReduceOperation op = new MapReduceOperation();
            op.setData(task);
            op.setCallbackInfo(callbackInfo);
            op.setOpCode(MapReduceOpCodes.SubmitMapReduceTask);
            op.setTaskID(taskId);

            Function func = new Function(OpCodes.MapReduceOperation.getValue(), op, false);
            RspList results = getCluster().Multicast(getActiveServers(), func, GroupRequest.GET_ALL, false);

            ClusterHelper.ValidateResponses(results, TaskExecutionStatus.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, TaskExecutionStatus.class);

            java.util.Iterator ia = rspList.iterator();
            while (ia.hasNext()) {
                Rsp rsp = (Rsp) ia.next();
                if ((TaskExecutionStatus)rsp.getValue()==TaskExecutionStatus.Failure) {
                    MapReduceOperation stopOperation = new MapReduceOperation();
                    stopOperation.setOpCode(MapReduceOpCodes.RemoveFromSubmittedList);
                    stopOperation.setTaskID(taskId);
                    
                    Function stopFunction = new Function(OpCodes.MapReduceOperation.getValue(), stopOperation, false);
                    getCluster().Multicast(getActiveServers(), stopFunction, GroupRequest.GET_ALL, false);
                    throw new GeneralFailureException("Task failed during submition on Node : " + rsp.getSender().toString());
                }
            }
            
            //task submitted successfully on all nodes, so now start the task
            MapReduceOperation startingOperation = new MapReduceOperation();
            startingOperation.setOpCode(MapReduceOpCodes.StartTask);
            startingOperation.setTaskID(taskId);
            startingOperation.setSequenceID(sequenceID);
            
            Function taskStartingFunction = new Function(OpCodes.MapReduceOperation.getValue(), startingOperation, false);
            RspList runTaskCommandRsps = getCluster().Multicast(getActiveServers(), taskStartingFunction, GroupRequest.GET_ALL, false);
            
            ClusterHelper.ValidateResponses(runTaskCommandRsps, TaskExecutionStatus.class, getName());
            java.util.ArrayList runTaskRspList = ClusterHelper.GetAllNonNullRsp(runTaskCommandRsps, TaskExecutionStatus.class);
            
            ia=null;
            ia = runTaskRspList.iterator();
            while (ia.hasNext()) {
                Rsp rsp = (Rsp) ia.next();
                if ((TaskExecutionStatus)rsp.getValue()==TaskExecutionStatus.Failure) {
                    MapReduceOperation stopRunningOperation = new MapReduceOperation();
                    stopRunningOperation.setOpCode(MapReduceOpCodes.RemoveFromRunningList);
                    stopRunningOperation.setTaskID(taskId);
                    
                    Function stopRunningFunction = new Function(OpCodes.MapReduceOperation.getValue(), stopRunningOperation, false);
                    getCluster().Multicast(getActiveServers(), stopRunningFunction, GroupRequest.GET_ALL, false);
                    throw new GeneralFailureException("Task failed while starting on Node : " + rsp.getSender().toString());
                }
            }
            
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return;

        //Send Cluster wide call for submission
    }

    protected final Object handleMapReduceOperation(Object argument) throws OperationFailedException {
        return Local_MapReduceOperation((MapReduceOperation) argument);
    }

    protected Object Local_MapReduceOperation(MapReduceOperation operation) throws OperationFailedException {
        return getInternalCache().TaskOperationRecieved(operation);
    }

    public final void Clustered_SendMapReduceOperation(java.util.ArrayList dests, MapReduceOperation operation) throws GeneralFailureException {
        try {
            operation.setSource(getCluster().getLocalAddress());
            Function func = new Function(OpCodes.MapReduceOperation.getValue(), operation, true);
            getCluster().Multicast(dests, func, getGetAllResponses());
            //handleMapReduceOperation(operation);
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    public final void Clustered_SendMapReduceOperation(Address target, MapReduceOperation operation) throws GeneralFailureException {
        try {
            operation.setSource(getCluster().getLocalAddress());
            Function func = new Function(OpCodes.MapReduceOperation.getValue(), operation, false);
            getCluster().SendMessage(target, func, GroupRequest.GET_FIRST);
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Registers the item update/remove or both callbacks with the specified
     * key. Keys should exist before the registration.
     *
     * @param taskID
     * @param callbackInfo
     * @param operationContext
     * @throws OperationFailedException
     */
    @Override
    public final void RegisterTaskNotification(String taskID, TaskCallbackInfo callbackInfo, OperationContext operationContext) throws
            OperationFailedException {

        try {
            MapReduceOperation op = new MapReduceOperation();
            op.setCallbackInfo(callbackInfo);
            op.setOpCode(MapReduceOpCodes.RegisterTaskNotification);
            op.setTaskID(taskID);

            Function func = new Function(OpCodes.MapReduceOperation.getValue(), op, false);
            RspList results = getCluster().Multicast(getActiveServers(), func, GroupRequest.GET_ALL, false);

            ClusterHelper.ValidateResponses(results, Boolean.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, Boolean.class);

            java.util.Iterator ia = rspList.iterator();
            while (ia.hasNext()) {
                Rsp rsp = (Rsp) ia.next();
                if (rsp.getValue() != null) {
                    //check if all responses are same
                }
            }

        } catch (Exception inner) {
            _context.getCacheLog().Error("ClusterCacheBase.RegisterTaskNotification() ", inner.toString());
            throw new OperationFailedException("RegisterTaskNotification failed. Error : " + inner.getMessage(), inner);
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
    @Override
    public final void UnregisterTaskNotification(String taskID, TaskCallbackInfo callbackInfo, OperationContext operationContext) throws
            OperationFailedException {

        try {
            MapReduceOperation op = new MapReduceOperation();
            op.setData(callbackInfo);
            op.setOpCode(MapReduceOpCodes.UnregisterTaskNotification);
            op.setTaskID(taskID);

            Function func = new Function(OpCodes.MapReduceOperation.getValue(), op, false);
            RspList results = getCluster().Multicast(getCluster().getSubCoordinators(), func, GroupRequest.GET_ALL, false);

            ClusterHelper.ValidateResponses(results, Boolean.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, Boolean.class);

            java.util.Iterator ia = rspList.iterator();
            while (ia.hasNext()) {
                Rsp rsp = (Rsp) ia.next();
                if (rsp.getValue() != null) {
                    //check if all responses are same
                }
            }

        } catch (Exception inner) {
            _context.getCacheLog().Error("ClusterCacheBase.UnregisterTaskNotification() ", inner.toString());
            throw new OperationFailedException("UnregisterTaskNotification failed. Error : " + inner.getMessage(), inner);
        }
    }

    @Override
    public List<TaskEnumeratorResult> getTaskEnumerator(TaskEnumeratorPointer pointer, OperationContext operationContext) throws Exception {
        List<TaskEnumeratorResult> resultSets = new ArrayList<TaskEnumeratorResult>();

        MapReduceOperation operation = new MapReduceOperation();
        operation.setData(pointer);
        operation.setOpCode(MapReduceOpCodes.GetTaskEnumerator);
        operation.setOperationContext(operationContext);

        long clientLastViewId = GetClientLastViewId(operationContext);
        if (clientLastViewId == forcedViewId) // for dedicated request
        {
            ArrayList servers = new ArrayList();
            if (IsInStateTransfer()) {
                servers = GetDestInStateTransfer();
            } else {
                servers.addAll(this.getActiveServers());
            }
            resultSets = Clustered_GetTaskEnumerator(servers, operation);
        } else if (!VerifyClientViewId(clientLastViewId)) {
            throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
        } else {
            resultSets.add((TaskEnumeratorResult) this.Local_MapReduceOperation(operation));
            if (IsInStateTransfer()) {
                throw new StateTransferInProgressException("Operation could not be completed due to state transfer");
            }
        }
        return resultSets;
    }

    /// <summary>
    /// Retrieve the reader result set from the cache based on the specified query.
    /// </summary>
    protected List<TaskEnumeratorResult> Clustered_GetTaskEnumerator(ArrayList dests, MapReduceOperation operation) throws CacheException {
        List<TaskEnumeratorResult> resultSet = new ArrayList<TaskEnumeratorResult>();

        try {

            Function func = new Function((byte) OpCodes.MapReduceOperation.getValue(), operation, false);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false);

            if (results == null) {
                return null;
            }
            ClusterHelper.ValidateResponses(results, TaskEnumeratorResult.class, this.getName());
            ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, TaskEnumeratorResult.class);
            if (rspList.size() <= 0) {
                return null;
            } else {
                Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    TaskEnumeratorResult rResultSet = (TaskEnumeratorResult) rsp.getValue();
                    resultSet.add(rResultSet);
                }
            }

            return resultSet;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    @Override
    public TaskEnumeratorResult getTaskNextRecord(TaskEnumeratorPointer pointer, OperationContext operationContext) throws Exception {
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));
        if (_internalCache == null) {
            throw new OperationFailedException();
        }

        TaskEnumeratorResult reader = null;

        Address intenededRecepient = pointer.getClusterAddress();
        List servers = new ArrayList(getCluster().getServers());

        MapReduceOperation operation = new MapReduceOperation();
        operation.setData(pointer);
        operation.setOpCode(MapReduceOpCodes.GetNextRecord);
        operation.setOperationContext(operationContext);

        Address targetNode = null;

        if (intenededRecepient != null) {
            for (int i = 0; i < servers.size(); i++) {
                Address server = (Address) servers.get(i);
                if (server.getIpAddress().toString() == null ? intenededRecepient == null : server.getIpAddress().toString().equals("/"+intenededRecepient.getIpAddress().getHostAddress())) {
                    targetNode = server;
                    break;
                }
            }
            if (targetNode != null) {
                if (targetNode.equals(getCluster().getLocalAddress())) {
                    reader = (TaskEnumeratorResult) Local_MapReduceOperation(operation);
                } else {
                    reader = Clustered_GetTaskNextRecord(targetNode, operation);
                }
            } else {
                throw new InvalidTaskEnumeratorException("Server "+intenededRecepient+" is not part of cluster.");
            }
        }
        return reader;
    }

    /// <summary>
    /// Retrieve the reader result set from the cache based on the specified query.
    /// </summary>
    protected TaskEnumeratorResult Clustered_GetTaskNextRecord(Address target, MapReduceOperation operation) throws CacheException {
        try {

            Function func = new Function((byte) OpCodes.MapReduceOperation.getValue(), operation, false);
            Object result = getCluster().SendMessage(target, func, GroupRequest.GET_FIRST, getCluster().getTimeout());

            return (TaskEnumeratorResult) result;
        } catch (SuspectedException e) {
            throw new InvalidTaskEnumeratorException("Task Enumerator is Invalid");
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    protected final void RaiseTaskCalbackNotifier(Object taskID, java.util.List taskCallbackListener, EventContext eventContext) {
        if (this.getCluster().getIsCoordinator()) {
            java.util.ArrayList destinations = null;
            java.util.List nodes = null;
            java.util.HashMap intendedNotifiers = new java.util.HashMap();
            if (taskCallbackListener != null && taskCallbackListener.size() > 0) {
                if (_stats.getNodes() != null) {
                    nodes = _stats.getNodes();
                    destinations = new java.util.ArrayList();
                    for (Iterator it = taskCallbackListener.iterator(); it.hasNext();) {
                        TaskCallbackInfo cbInfo = (TaskCallbackInfo) it.next();
                        int index = nodes.indexOf(new NodeInfo(getCluster().getLocalAddress()));
                        if (index != -1 && ((NodeInfo) nodes.get(index)).getConnectedClients().contains(cbInfo.getClient())) {
                            if (!destinations.contains(getCluster().getLocalAddress())) {
                                destinations.add(getCluster().getLocalAddress());
                            }
                            intendedNotifiers.put(cbInfo, getCluster().getLocalAddress());
                            continue;
                        } else {
                            for (Iterator ite = nodes.iterator(); ite.hasNext();) {
                                NodeInfo nInfo = (NodeInfo) ite.next();
                                if (nInfo.getConnectedClients() != null && nInfo.getConnectedClients().contains(cbInfo.getClient())) {
                                    if (!destinations.contains(nInfo.getAddress())) {
                                        destinations.add(nInfo.getAddress());
                                        intendedNotifiers.put(cbInfo, nInfo.getAddress());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (destinations != null && destinations.size() > 0) {
                Object[] packed = new Object[]{
                    taskID,
                    taskCallbackListener,
                    intendedNotifiers
                };

                // If everything went ok!, initiate local and cluster-wide notifications.
                boolean sendLocal = false;
                Object[] objs = (Object[]) ((packed instanceof Object[]) ? packed : null);
                java.util.List callbackListeners = (java.util.List) ((objs[1] instanceof java.util.List) ? objs[1] : null);

                if (destinations.contains(getCluster().getLocalAddress())) {
                    destinations.remove(getCluster().getLocalAddress());
                    sendLocal = true;
                }

                if (getValidMembers().size() > 1) {
                    _cluster.SendNoReplyMcastMessageAsync(destinations, new Function(OpCodes.NotifyTaskCallback.getValue(), new Object[]{
                        objs[0],
                        callbackListeners,
                        objs[2], eventContext
                    }));
                }

                if (sendLocal) {
                    handleNotifyTaskCallback(new Object[]{
                        objs[0],
                        callbackListeners,
                        objs[2], eventContext
                    });
                }

            }
        }
    }

    /**
     * Hanlder for clustered item update callback notification.
     *
     * @param info packaged information ///
     * @param entry callback entry
     * @return null
     */
    private Object handleNotifyTaskCallback(Object info) {
        Object[] objs = (Object[]) info;
        EventContext eventContext = null;
        java.util.ArrayList callbackListeners = (java.util.ArrayList) ((objs[1] instanceof java.util.ArrayList) ? objs[1] : null);
        java.util.HashMap intendedNotifiers = (java.util.HashMap) ((objs[2] instanceof java.util.HashMap) ? objs[2] : null);
        if (objs.length > 3) {
            eventContext = (EventContext) objs[3];
        }

        Iterator ide = intendedNotifiers.entrySet().iterator();
        Map.Entry KeyValue;
        while (ide.hasNext()) {
            KeyValue = (Map.Entry) ide.next();
            Object Key = KeyValue.getKey();
            Object Value = KeyValue.getValue();
            CallbackInfo cbinfo = (CallbackInfo) ((Key instanceof CallbackInfo) ? Key : null);
            Address node = (Address) ((Value instanceof Address) ? Value : null);

            if (node != null && !node.equals(getCluster().getLocalAddress())) {
                callbackListeners.remove(cbinfo);
            }
        }

        NotifyTaskCallback(objs[0], (List) objs[1], false, null, eventContext);
        return null;
    }

    /**
     *
     * @param taskId
     * @param cancelAll
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     */
    @Override
    public void cancelMapReduceTask(String taskId, boolean cancelAll) throws OperationFailedException {
        try {
            MapReduceOperation operation = new MapReduceOperation();
            if (cancelAll) {
                operation.setOpCode(MapReduceOpCodes.CancelAllTasks);
            } else {
                operation.setOpCode(MapReduceOpCodes.CancelTask);
                if (taskId == null || taskId.equals("")) {
                    throw new ArgumentNullException("taskId");
                }
                operation.setTaskID(taskId);
            }

            Function func = new Function(OpCodes.MapReduceOperation.getValue(), operation, false);
            RspList results = getCluster().Multicast(getActiveServers(), func, GroupRequest.GET_ALL, false);

            ClusterHelper.ValidateResponses(results, Boolean.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, Boolean.class);

            java.util.Iterator ia = rspList.iterator();
            while (ia.hasNext()) {
                Rsp rsp = (Rsp) ia.next();
                if (rsp.getValue() != null) {
                    //check if all responses are same
                }
            }
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage(), ex);
        }

    }

    /**
     * Returns the list of taskIDs of the running tasks.
     *
     * @return
     */
    @Override
    public java.util.ArrayList getRunningTasks() throws GeneralFailureException {
        java.util.ArrayList runningTasks = new ArrayList();
        try {
            MapReduceOperation operation = new MapReduceOperation();
            operation.setOpCode(MapReduceOpCodes.GetRunningTasks);

            Function func = new Function(OpCodes.MapReduceOperation.getValue(), operation, false);
            Object result = getCluster().SendMessage(getCluster().getCoordinator(), func, GroupRequest.GET_FIRST);
            runningTasks = (ArrayList) result;

        } catch (Exception ex) {
            throw new GeneralFailureException(ex.getMessage(), ex);
        }
        return runningTasks;
    }

    /**
     * Gets the Progress instance of the task
     *
     * @throws GeneralFailureException
     */
    @Override
    public TaskStatus getTaskProgress(String taskId) throws GeneralFailureException {
        TaskStatus status = new TaskStatus(TaskStatus.Status.Failed, 0);
        try {
            MapReduceOperation operation = new MapReduceOperation();
            operation.setOpCode(MapReduceOpCodes.GetTaskStats);
            operation.setTaskID(taskId);

            Function func = new Function(OpCodes.MapReduceOperation.getValue(), operation, false);
            Object result = getCluster().SendMessage(getCluster().getCoordinator(), func, GroupRequest.GET_FIRST);

            if (result == null) {
                throw new Exception("Task with specified key does not exist.");
            }

            status = (TaskStatus) result;

        } catch (Exception ex) {
            throw new GeneralFailureException(ex.getMessage(), ex);
        }
        return status;
    }

    @Override
    public void declaredDeadClients(ArrayList clients) {
        try {
            if (_context.getCacheLog().getIsInfoEnabled()) {
                _context.getCacheLog().Info("ClusteredCacheBase.DeclaredDeadClients()", " DeclaredDeadClients Status accross the cluster");
            }
            if (this.getValidMembers().size() > 1) {
                Function func = new Function((byte) OpCodes.DeadClients.getValue(), clients);
                getCluster().Broadcast(func, GroupRequest.GET_NONE, false, Priority.Normal);
            } else {
                handleDeadClients(clients);
            }
        } catch (Exception e) {
            _context.getCacheLog().Error("ClusteredCacheBase.AnnouncePresence()", e.toString());
        }
    }

    @Override
    public ArrayList determineClientConnectivity(ArrayList clientList) {
        if (clientList == null) {
            return null;
        }
        if (_context.getCacheLog().getIsInfoEnabled()) {
            _context.getCacheLog().Info("Client-Death-Detection.DetermineClientConnectivity()", "going to determine client connectivity in cluster");
        }

        try {
            DetermineClusterStatus();//updating stats
            ArrayList result = clientList;
            for (Iterator it = _stats.getNodes().iterator(); it.hasNext();) {
                NodeInfo node = (NodeInfo) it.next();
                ArrayList clients = new ArrayList();
                for (Iterator itc = clientList.iterator(); itc.hasNext();) {
                    String client = (String) itc.next();
                    if (!node.getConnectedClients().contains(client)) {
                        clients.add(client);
                    } else if (result.contains(client)) {
                        result.remove(client);
                    }
                }
                String[] tempResult = (String[]) result.toArray();
                String[] tempClient = (String[]) clients.toArray();
                String[] intersection = intersection(Arrays.asList(tempResult), Arrays.asList(tempClient));
                for (int i = 0; i < intersection.length; i++) {
                    if (!result.contains(intersection[i])) {
                        result.add(intersection[i]);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            _context.getCacheLog().Error("Client-Death-Detection.DetermineClientConnectivity()", e.toString());
        } finally {
            if (_context.getCacheLog().getIsInfoEnabled()) {
                _context.getCacheLog().Info("Client-Death-Detection.DetermineClientConnectivity()", "determining client connectivity in cluster completed");
            }
        }
        return null;
    }

    private void handleDeadClients(Object Obj) {
        ArrayList clients = (ArrayList) Obj;
        getInternalCache().declaredDeadClients(clients);
    }

    public String[] intersection(List<String> list1, List<String> list2) {
        List list = new ArrayList();

        for (String t : list1) {
            if (list2.contains(t)) {
                list.add(t);
            }
        }

        return (String[]) list.toArray();
    }

}
