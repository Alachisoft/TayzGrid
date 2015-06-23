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

import com.alachisoft.tayzgrid.cluster.util.RspList;
import com.alachisoft.tayzgrid.cluster.util.Rsp;
import com.alachisoft.tayzgrid.cluster.blocks.GroupRequest;
import com.alachisoft.tayzgrid.cluster.OperationResponse;
import com.alachisoft.tayzgrid.caching.util.MiscUtil;
import com.alachisoft.tayzgrid.config.ConfigHelper;
import com.alachisoft.tayzgrid.caching.util.ClusterHelper;
import com.alachisoft.tayzgrid.caching.util.CacheHelper;
import com.alachisoft.tayzgrid.caching.topologies.local.HashedLocalCache;
import com.alachisoft.tayzgrid.caching.topologies.local.HashedOverflowCache;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.IClusterEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.caching.statistics.NodeStatus;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.evictionpolicies.EvictionHint;
import com.alachisoft.tayzgrid.caching.datasourceproviders.WriteBehindAsyncProcessor;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.EventContextFieldName;
import com.alachisoft.tayzgrid.caching.AllowedOperationType;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.EventId;
import com.alachisoft.tayzgrid.caching.EventCacheEntry;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.caching.DataSourceUpdateOptions;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OperationID;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.common.threading.Latch;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatus;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;

import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.datastructures.DistributionMode;
import com.alachisoft.tayzgrid.common.datastructures.HashMapBucket;
import com.alachisoft.tayzgrid.common.datastructures.ClusterActivity;
import com.alachisoft.tayzgrid.common.datastructures.BalancingResult;
import com.alachisoft.tayzgrid.common.datastructures.NewHashmap;
import com.alachisoft.tayzgrid.common.datastructures.DistributionMaps;
import com.alachisoft.tayzgrid.common.datastructures.PartNodeInfo;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;
import com.alachisoft.tayzgrid.common.datastructures.DistributionInfoData;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.ResetableIterator;
import com.alachisoft.tayzgrid.common.LockOptions;
import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.caching.datagrouping.DataAffinity;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.common.exceptions.BucketTransferredException;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.mapreduce.MapReduceOpCodes;
import com.alachisoft.tayzgrid.mapreduce.MapReduceOperation;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class provides the partitioned cluster cache primitives.
 */
public class PartitionedServerCache extends PartitionedCacheBase implements ICacheEventsListener {

    /**
     * The periodic update task.
     */
    private PeriodicPresenceAnnouncer _taskUpdate;
    /**
     * The data groups allowed for this node
     */
    private java.util.Map _dataAffinity;
    private DistributionManager _distributionMgr;
    private AutomaticDataLoadBalancer _autoBalancingTask;
    private StateTransferTask _stateTransferTask;
    private Object _txfrTaskMutex = new Object();
    private java.util.HashMap _corresponders;
    private long _clusterItemCount;
    private boolean threadRunning = true;
    private int confirmClusterStartUP = 3;
    protected InetAddress _srvrJustLeft = null;
    private ClientsManager _clientsMgr;

    /**
     * Constructor.
     *
     * @param cacheSchemes collection of cache schemes (config properties).
     * @param properties properties collection for this cache.
     * @param listener cache events listener
     */
    public PartitionedServerCache(java.util.Map cacheClasses, java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context) throws ConfigurationException {
        super(properties, listener, context);
        _stats.setClassName("partitioned-server");
        Initialize(cacheClasses, properties);
    }

    /**
     * Constructor.
     *
     * @param cacheSchemes collection of cache schemes (config properties).
     * @param properties properties collection for this cache.
     * @param listener cache events listener
     */
    public PartitionedServerCache(java.util.Map cacheClasses, java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context, IClusterEventsListener clusterListener, String userId, String password) {
        super(properties, listener, context, clusterListener);
        _stats.setClassName("partitioned-server");
    }

    @Override
    public boolean IsOperationAllowed(Object key, AllowedOperationType opType) {
        if (super._shutdownServers != null && super._shutdownServers.size() > 0) {
            Address targetNode = GetNextNode(key, "");

            if (opType == AllowedOperationType.AtomicRead || opType == AllowedOperationType.AtomicWrite) {
                if (super.IsShutdownServer(targetNode)) {
                    return false;
                } else if (super.IsShutdownServer(getLocalAddress())) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean IsOperationAllowed(Object[] keys, AllowedOperationType opType, OperationContext operationContext) {
        if (super._shutdownServers != null && super._shutdownServers.size() > 0) {

            long clientLastViewId = GetClientLastViewId(operationContext);

            if (clientLastViewId == getCluster().getLastViewID() && !IsInStateTransfer()) {
                if (super.IsShutdownServer(getLocalAddress())) {
                    return false;
                }
            } else {
                java.util.ArrayList totalKeys = new java.util.ArrayList(Arrays.asList(keys));
                java.util.HashMap targetNodes = GetTargetNodes(totalKeys, "");
                if (targetNodes != null) {
                    Iterator ide = targetNodes.entrySet().iterator();

                    while (ide.hasNext()) {
                        Map.Entry pair = (Map.Entry) ide.next();
                        Address targetNode = (Address) ((pair.getKey() instanceof Address) ? pair.getKey() : null);
                        if (super.IsShutdownServer(targetNode)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean IsOperationAllowed(AllowedOperationType opType, OperationContext operationContext) {
        if (super._shutdownServers != null && super._shutdownServers.size() > 0) {
            if (opType == AllowedOperationType.ClusterRead) {
                return false;
            }

            if (opType == AllowedOperationType.BulkWrite) {
                return false;
            }

            if (opType == AllowedOperationType.BulkRead) {
                long clientLastViewId = GetClientLastViewId(operationContext);
                if (clientLastViewId == forcedViewId) //Client wants only me to collect data from cluster and return
                {
                    return false;
                } else if (clientLastViewId == getCluster().getLastViewID()) {
                    if (super.IsShutdownServer(getLocalAddress())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    @Override
    public void dispose() {
        setHasDisposed(true);

        if (_taskUpdate != null) {
            _taskUpdate.Cancel();
            _taskUpdate = null;
        }
        if (_autoBalancingTask != null) {
            _autoBalancingTask.Cancel();
            _autoBalancingTask = null;
        }
        if (_internalCache != null) {
            _internalCache.dispose();
            _internalCache = null;
        }
        if (_stateTransferTask != null) {
            _stateTransferTask.Stop();
            _stateTransferTask = null;
        }

        threadRunning = false;
        super.dispose();
    }

    /**
     * Returns the cache local to the node, i.e., internal cache.
     */
    @Override
    public CacheBase getInternalCache() {
        return _internalCache;
    }

    public final DistributionManager getDistributionMgr() {
        return _distributionMgr;
    }

    public final void setDistributionMgr(DistributionManager value) {
        _distributionMgr = value;
    }

    /**
     * Method that allows the object to initialize itself. Passes the property
     * map down the object hierarchy so that other objects may configure
     * themselves as well..
     *
     * @param cacheSchemes collection of cache schemes (config properties).
     * @param properties properties collection for this cache.
     */
    @Override
    public void Initialize(java.util.Map cacheClasses, java.util.Map properties, String userId, String password, boolean twoPhaseInitialization) throws ConfigurationException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            super.Initialize(cacheClasses, properties);

            java.util.Map frontCacheProps = ConfigHelper.GetCacheScheme(cacheClasses, properties, "internal-cache");
            String cacheType = String.valueOf(frontCacheProps.get("type")).toLowerCase();
            if (cacheType.compareTo("local-cache") == 0) {
                _internalCache = CacheBase.Synchronized(new HashedLocalCache(cacheClasses, this, frontCacheProps, this, _context, false));
            } else if (cacheType.compareTo("overflow-cache") == 0) {
                _internalCache = CacheBase.Synchronized(new HashedOverflowCache(cacheClasses, this, frontCacheProps, this, _context, false));
            } else {
                throw new ConfigurationException("invalid or non-local class specified in partitioned cache");
            }

            if (properties.containsKey("data-affinity")) {
                _dataAffinity = (java.util.Map) properties.get("data-affinity");
            }

            _distributionMgr = new DistributionManager(_autoBalancingThreshold, _internalCache.getMaxSize());
            _distributionMgr.setNCacheLog(getContext().getCacheLog());

            //+Numan Hanif: Fix for 7419
            //getInternalCache().setBucketSize(_distributionMgr.getBucketSize());
            getInternalCache().setBucketSize(AppUtil.TotalBuckets);
            //+Numan Hanif


            _stats.setNodes(Collections.synchronizedList(new java.util.ArrayList()));

            _initialJoiningStatusLatch = new Latch();

            InitializeCluster(properties, getName(), MCAST_DOMAIN, new Identity(true, (_context.getRender() != null ? _context.getRender().getPort() : 0), (_context.getRender()
                    != null ? _context.getRender().getIPAddress() : null)), userId, password, twoPhaseInitialization, false);
            _stats.setGroupName(getCluster().getClusterName());
            _distributionMgr.setLocalAddress(getLocalAddress());

            postInstrumentatedData(_stats, getName());

            //Wait for the completion of installation of new view
            if (!twoPhaseInitialization) {
                _initialJoiningStatusLatch.WaitForAny(NodeStatus.Running);
                DetermineClusterStatus();
                _statusLatch.SetStatusBit(NodeStatus.Running, NodeStatus.Initializing);
                AnnouncePresence(true);
            }

            for (int i = 0; i < confirmClusterStartUP; i++) {
                ConfirmClusterStartUP(false, i + 1);
            }

            HasStarted();
        } catch (ConfigurationException e) {
            dispose();
            throw e;
        } catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch state from a cluster member. If the node is the coordinator there
     * is no need to do the state transfer.
     */
    @Override
    protected void StartStateTransfer(boolean isBalanceDataLoad) {
        /**
         * Tell everyone that we are not fully-functional, i.e., initilizing.
         */
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("PartitionedCache.StartStateTransfer()", "Requesting state transfer " + getLocalAddress());
        }
        if (_stateTransferTask == null) {
            _stateTransferTask = new StateTransferTask(this, getCluster().getLocalAddress());
        }
        if (_context != null && _context.PerfStatsColl != null) {
            _context.PerfStatsColl.nodeStatus(CacheNodeStatus.InStateTransfer.getValue());
        }
        _stateTransferTask.setIsBalanceDataLoad(isBalanceDataLoad);
        _stateTransferTask.DoStateTransfer(_distributionMgr.GetBucketsList(getCluster().getLocalAddress()), false);
        if (_context != null && _context.PerfStatsColl != null) {
            _context.PerfStatsColl.nodeStatus(CacheNodeStatus.Running.getValue());
        }
    }

    private int GetBucketId(Object key) {
        int hashCode = AppUtil.hashCode(key);
       
        //+Numan Hanif: Fix for 7419
        //int bucketId = hashCode / _distributionMgr.getBucketSize();
        int bucketId = hashCode % AppUtil.TotalBuckets;
        //+Numan Hanif

        if (bucketId < 0) {
            bucketId *= -1;
        }
        return bucketId;
    }

    /**
     * Called after the membership has been changed. Lets the members do some
     * member oriented tasks.
     */
    @Override
    public void OnAfterMembershipChange() throws InterruptedException, OperationFailedException {
        try {
            ArrayList serverList = new ArrayList(getCluster().getServers());
            NotifyHashmapChanged(getCluster().getLastViewID(), _distributionMgr.GetOwnerHashMapTable(getCluster().getRenderers()), serverList, true, true);
        } catch (Exception exc) {
            this.getCacheLog().Error("OnAfterMembershipChange", exc.getMessage());
        }

        MapReduceOperation operation = new MapReduceOperation();
        operation.setOpCode(MapReduceOpCodes.CancelAllTasks);
        handleMapReduceOperation(operation);
        
        super.OnAfterMembershipChange();

        if (_taskUpdate == null) {
            _taskUpdate = new PeriodicPresenceAnnouncer(this, _statsReplInterval);
            _context.TimeSched.AddTask(_taskUpdate);
            _statusLatch.SetStatusBit(NodeStatus.Running, NodeStatus.Initializing);

            //Set joining completion status.
            _initialJoiningStatusLatch.SetStatusBit(NodeStatus.Running, NodeStatus.Initializing);
        }

        //coordinator is responsible for carrying out the automatic load
        //balancing...
        if (getCluster().getIsCoordinator()) {
            if (_isAutoBalancingEnabled) {
                if (_autoBalancingTask == null) {
                    _autoBalancingTask = new AutomaticDataLoadBalancer(this, _autoBalancingInterval);
                    _context.TimeSched.AddTask(_autoBalancingTask);
                }
            }
            if(_context.ClientDeathDetection != null) _context.ClientDeathDetection.StartMonitoringClients();
        }

        if (_context.getDsMgr() != null && _context.getDsMgr().getIsWriteBehindEnabled() ) {
            _context.getDsMgr().StartWriteBehindProcessor();
        }

        StartStateTransfer(false);

        UpdateCacheStatistics();
        //We announces our status aa uninitalized
    }

    /**
     * Called when a new member joins the group.
     *
     * @param address address of the joining member
     * @param identity additional identity information
     * @return true if the node joined successfuly
     */
    @Override
    public boolean OnMemberJoined(Address address, NodeIdentity identity) throws UnknownHostException {
        if (!super.OnMemberJoined(address, identity) || !((Identity) identity).getHasStorage()) {
            return false;
        }

        NodeInfo info = new NodeInfo((Address) ((address instanceof Address) ? address : null));
        if (identity.getRendererAddress() != null) {
            info.setRendererAddress(new Address(identity.getRendererAddress(), identity.getRendererPort()));
        }
        info.setIsInproc(identity.getRendererPort() == 0);
        info.setSubgroupName(identity.getSubGroupName());
        _stats.getNodes().add(info);
        _distributionMgr.OnMemberJoined(address, identity);

        if (getLocalAddress().compareTo(address) == 0) {
            //muds:
            UpdateLocalBuckets();

            _stats.setLocalNode(info);
            if (_dataAffinity != null) {
                DataAffinity da = new DataAffinity(_dataAffinity);
                _stats.getLocalNode().setDataAffinity(da);
                if (da.getAllBindedGroups() != null) {
                    java.util.Iterator ie = da.getAllBindedGroups().iterator();
                    while (ie.hasNext()) {
                        if (!_stats.getClusterDataAffinity().contains(ie.next())) {
                            _stats.getClusterDataAffinity().add(ie.next());
                        }

                        if (_stats.getPartitionsHavingDatagroup().containsKey(ie.next())) {
                            java.util.ArrayList nodeList = (java.util.ArrayList) _stats.getPartitionsHavingDatagroup().get(ie.next());
                            if (!nodeList.contains(address)) {
                                nodeList.add(address);
                            }
                        } else {
                            java.util.ArrayList nodeList = new java.util.ArrayList();
                            nodeList.add(address);
                            _stats.getPartitionsHavingDatagroup().put(ie.next(), nodeList);
                        }
                    }

                    if (!_stats.getDatagroupsAtPartition().containsKey(address)) {
                        _stats.getDatagroupsAtPartition().put(address, da.getGroups());
                    }
                }
                _dataAffinity = null;
            }

        }

        if (!info.getIsInproc()) {
            AddServerInformation(address, identity.getRendererPort(), info.getConnectedClients().size());
        }

        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("PartitionedCache.OnMemberJoined()", "Partition extended: " + address);
        }
        return true;
    }

    /**
     * Called when an existing member leaves the group.
     *
     * @param address address of the joining member
     * @return true if the node left successfuly
     */
    @Override
    public boolean OnMemberLeft(Address address, NodeIdentity identity) {
        if (!super.OnMemberLeft(address, identity)) {
            return false;
        }

        NodeInfo info = _stats.GetNode((Address) ((address instanceof Address) ? address : null));
        _stats.getNodes().remove(info);
        _distributionMgr.OnMemberLeft(address, identity);

        //muds:
        if (_stats.getDatagroupsAtPartition().containsKey(address)) {
            java.util.ArrayList datagroups = (java.util.ArrayList) _stats.getDatagroupsAtPartition().get(address);
            if (datagroups != null && datagroups.size() > 0) {
                java.util.Iterator ie = datagroups.iterator();
                while (ie.hasNext()) {
                    if (_stats.getPartitionsHavingDatagroup().containsKey(ie.next())) {
                        java.util.ArrayList nodeList = (java.util.ArrayList) _stats.getPartitionsHavingDatagroup().get(ie.next());
                        if (nodeList != null) {
                            if (nodeList.contains(address)) {
                                nodeList.remove(address);
                            }
                        }
                    }
                }
            }
            _stats.getDatagroupsAtPartition().remove(address);
        }

        //muds:
        UpdateLocalBuckets();

        if (_corresponders != null) {
            StateTxfrCorresponder cor = (StateTxfrCorresponder) ((_corresponders.get(address) instanceof StateTxfrCorresponder) ? _corresponders.get(address) : null);
            if (cor != null) {
                cor.dispose();
                _corresponders.remove(address);
            }
        }
        if (!info.getIsInproc()) {
            RemoveServerInformation(address, identity.getRendererPort());
        }

        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("PartitionedCache.OnMemberLeft()", "Partition shrunk: " + address);
        }
        return true;
    }

    /**
     * Handles the function requests.
     *
     * @param func
     * @return
     */
    @Override
    public Object HandleClusterMessage(Address src, Function func) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, TimeoutException, SuspectedException, Exception {
        OpCodes opCode = OpCodes.forValue((int) func.getOpcode());
        switch (opCode) {
            case PeriodicUpdate:
                return handlePresenceAnnouncement(src, func.getOperand());

            case ReqStatus:
                return this.handleReqStatus();

            case GetCount:
                return handleCount();

            case Get:
                return handleGet(func.getOperand());

            case Insert:
                return handleInsert(src, func.getOperand(), func.getUserPayload());

            case Contains:
                return handleContains(func.getOperand());

            case Add:
                return handleAdd(src, func.getOperand(), func.getUserPayload());

            case AddHint:
                return handleAddHint(func.getOperand());

          
            case Remove:
                return handleRemove(src, func.getOperand());

            case Clear:
                return handleClear(src, func.getOperand());

            case KeyList:
                return handleKeyList();

            case NotifyAdd:
                return handleNotifyAdd(func.getOperand());

            case NotifyUpdate:
                return handleNotifyUpdate(func.getOperand());

            case NotifyRemoval:
                return handleNotifyRemoval(func.getOperand());

            case NotifyBulkRemoval:
                return handleNotifyBulkRemoval(func.getOperand());

            case GetKeys:
                return handleGetKeys(func.getOperand());

            case GetData:
                return handleGetData(func.getOperand());

            case RemoveGroup:
                return handleRemoveGroup(func.getOperand());
           
            case Search:
                return handleSearch(func.getOperand());

            case SearchEntries:
                return handleSearchEntries(func.getOperand());

            case VerifyDataIntegrity:
                return handleVerifyDataIntegrity(func.getOperand());

            case GetDataGroupInfo:
                return handleGetGroupInfo(func.getOperand());

            case GetGroup:
                return handleGetGroup(func.getOperand());

            case LockBuckets:
                return handleLockBuckets(func.getOperand());

            case ReleaseBuckets:
                handleReleaseBuckets(func.getOperand(), src);
                break;

            case TransferBucket:
                return handleTransferBucket(src, func.getOperand());

            case AckStateTxfr:
                handleAckStateTxfr(func.getOperand(), src);
                break;

            case AnnounceStateTransfer:
                handleAnnounceStateTransfer(func.getOperand(), src);
                break;

            case SignalEndOfStateTxfr:
                handleSignalEndOfStateTxfr(src);
                break;

            case LockKey:
                return handleLock(func.getOperand());

            case UnLockKey:
                handleUnLock(func.getOperand());
                break;

            case IsLocked:
                return handleIsLocked(func.getOperand());

            case GetTag:
                return handleGetTag(func.getOperand());

            case GetKeysByTag:
                return handleGetKeysByTag(func.getOperand());

            case DeleteQuery:
                return handleDeleteQuery(func.getOperand());

            case RemoveByTag:
                return handleRemoveByTag(func.getOperand());


            case GetNextChunk:
                return handleGetNextChunk(src, func.getOperand());
        }
        return super.HandleClusterMessage(src, func);

    }

    private Object handleRemoveByTag(Object info) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            Object[] data = (Object[]) info;
            java.util.HashMap removed = Local_RemoveTag((String[]) ((data[0] instanceof String[]) ? data[0] : null), (TagComparisonType) data[1], (Boolean) data[2], (OperationContext) ((data[3] instanceof OperationContext) ? data[3] : null));
            return removed;
        }

        return null;
    }

    private Object handleGetKeysByTag(Object info) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            Object[] data = (Object[]) info;
            java.util.ArrayList keys = _internalCache.GetTagKeys((String[]) ((data[0] instanceof String[]) ? data[0] : null), (TagComparisonType) data[1], (OperationContext) ((data[2] instanceof OperationContext) ? data[2] : null));
            return keys;
        }

        return null;
    }

    /**
     * Periodic update (PULL model), i.e. on demand fetch of information from
     * every node.
     */
    @Override
    public boolean DetermineClusterStatus() {
        try {
            Function func = new Function(OpCodes.ReqStatus.getValue(), null);
            RspList results = getCluster().BroadcastToServers(func, GroupRequest.GET_ALL, false);

            for (int i = 0; i < results.size(); i++) {
                Rsp rsp = (Rsp) results.elementAt(i);
                Object tempVar = rsp.getValue();
                NodeInfo nodeInfo = (NodeInfo) ((tempVar instanceof NodeInfo) ? tempVar : null);
                if (nodeInfo != null) {
                    Object tempVar2 = rsp.getSender();
                    handlePresenceAnnouncement((Address) ((tempVar2 instanceof Address) ? tempVar2 : null), nodeInfo);
                }
            }
        } /**
         * This is Madness.. No .. this was previously maintained as it is i.e.
         * using exception as flow control Null can be rooted down to
         * MirrorManager.GetGroupInfo where null is intentionally thrown and
         * here instead of providing a check ... This flow can be found in every
         * DetermineCluster function of every topology
         */
        catch (NullPointerException e) {
        } catch (Exception e) {
            getCacheLog().Error("ParitionedCache.DetermineClusterStatus()", e.toString());
        }
        return false;
    }

    /**
     * Handler for Periodic update (PULL model), i.e. on demand fetch of
     * information from every node.
     */
    private Object handleReqStatus() {
        return _stats.getLocalNode() != null ? _stats.getLocalNode().clone() : null;
    }

    /**
     * Handler for Periodic update (PUSH model).
     *
     * @param sender
     * @param obj
     * @return
     */
    private Object handlePresenceAnnouncement(Address sender, Object obj) {
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("PartitionedServer.handlePresenceAnnouncement", "sender :" + sender);
        }
        NodeInfo other = null;
        NodeInfo info = null;
        synchronized (getServers()) {
            other = (NodeInfo) ((obj instanceof NodeInfo) ? obj : null);
            info = _stats.GetNode((Address) ((sender instanceof Address) ? sender : null));
            if (other != null && info != null) {
                info.setStatistics(other.getStatistics());
                info.setStatus(other.getStatus());
                info.setConnectedClients(other.getConnectedClients());
                info.setDataAffinity(other.getDataAffinity());

                if (other.getDataAffinity() != null && other.getDataAffinity().getGroups() != null) {
                    java.util.Iterator ie = other.getDataAffinity().getGroups().iterator();
                    while (ie.hasNext()) {
                        if (!_stats.getClusterDataAffinity().contains(ie.next())) {
                            _stats.getClusterDataAffinity().add(ie.next());
                        }

                        //muds:
                        if (_stats.getPartitionsHavingDatagroup().containsKey(ie.next())) {
                            java.util.ArrayList nodeList = (java.util.ArrayList) _stats.getPartitionsHavingDatagroup().get(ie.next());
                            if (!nodeList.contains(sender)) {
                                nodeList.add(sender);
                            }
                        } else {
                            java.util.ArrayList nodeList = new java.util.ArrayList();
                            nodeList.add(sender);
                            _stats.getPartitionsHavingDatagroup().put(ie.next(), nodeList);
                        }
                    }
                    if (!_stats.getDatagroupsAtPartition().containsKey(sender)) {
                        _stats.getDatagroupsAtPartition().put(sender, other.getDataAffinity().getGroups());
                    }
                }
            }

        }
        if (other != null && info != null) {
            _distributionMgr.UpdateBucketStats(other);
            if (!info.getIsInproc()) {
                UpdateClientsCount(sender, info.getConnectedClients().size());
            }
        }
        UpdateCacheStatistics(false);
        return null;
    }

    /**
     * Updates the statistics for the cache scheme.
     */
    @Override
    protected void UpdateCacheStatistics() {
        UpdateCacheStatistics(true);
    }

    /**
     * Updates the statistics for the cache scheme.
     */
    protected final void UpdateCacheStatistics(boolean updateBucketstats) {
        super.UpdateCacheStatistics();
        if (updateBucketstats) {
            _distributionMgr.UpdateBucketStats(_stats.getLocalNode());
        }
    }

    @Override
    public CacheStatistics CombineClusterStatistics(ClusterCacheStatistics s) {
        CacheStatistics c = ClusterHelper.CombinePartitionStatistics(s);
        return c;
    }

    /**
     * Update the list of the clients connected to this node and replicate it
     * over the entire cluster.
     *
     * @param client
     */
    @Override
    public void ClientConnected(String client, boolean isInproc) throws OperationFailedException {
        super.ClientConnected(client, isInproc);
        PublishStats(false);
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));
    }

    /**
     * Update the list of the clients connected to this node and replicate it
     * over the entire cluster.
     *
     * @param client
     */
    @Override
    public void ClientDisconnected(String client, boolean isInproc) throws OperationFailedException {
        super.ClientDisconnected(client, isInproc);
        PublishStats(false);
    }

    @Override
    public CacheStatistics getStatistics() {
        Object tempVar = _stats.clone();
        CacheStatistics stats = (CacheStatistics) ((tempVar instanceof CacheStatistics) ? tempVar : null);
        long maxSize = 0;
        for (Iterator it = _stats.getNodes().iterator(); it.hasNext();) {
            NodeInfo nodeInfo = (NodeInfo) it.next();
            if (nodeInfo.getStatistics() != null) {
                maxSize += nodeInfo.getStatistics().getMaxSize();
            }
        }
        stats.setMaxSize(maxSize);
        return stats;

    }

    public boolean PublishStats(boolean urgent) {
        try {
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("ClusteredCacheBase.AnnouncePresence()", " announcing presence ;urget " + urgent);
            }
            if (this.getValidMembers().size() > 1) {
                Function func = new Function(OpCodes.PeriodicUpdate.getValue(), _stats.getLocalNode().clone());
                if (!urgent) {
                    getCluster().SendNoReplyMessage(func);
                } else {
                    getCluster().Broadcast(func, GroupRequest.GET_NONE, false, Priority.Normal);
                }
            }
            return true;
        } catch (Exception e) {
            getContext().getCacheLog().Error("ClusteredCacheBase.AnnouncePresence()", e.toString());
        }
        return false;
    }

    @Override
    public void BalanceDataLoad() throws SuspectedException, TimeoutException, GeneralFailureException {
        Clustered_BalanceDataLoad(getCluster().getCoordinator(), getCluster().getLocalAddress());
    }

    /**
     * Return the next node in load balancing order.
     *
     * @param data
     * @return
     */
    private Address GetNextNode(Object key, String group) {
        return _distributionMgr.SelectNode(key, group);
    }

    private java.util.HashMap GetTargetNodes(java.util.ArrayList keys, String group) {
        java.util.HashMap targetNodes = new java.util.HashMap();
        Address targetNode = null;

        if (keys != null) {
            for (Object key : keys) {
                targetNode = GetNextNode(key, group);
                if (targetNode != null) {
                    if (targetNodes.containsKey(targetNode)) {
                        java.util.HashMap keyList = (java.util.HashMap) targetNodes.get(targetNode);
                        keyList.put(key, null);
                    } else {
                        java.util.HashMap keyList = new java.util.HashMap();
                        keyList.put(key, null);
                        targetNodes.put(targetNode, keyList);
                    }
                }
            }
        }
        return targetNodes;
    }

    @Override
    public java.util.HashMap LockBuckets(java.util.ArrayList bucketIds) throws OperationFailedException {
        return Clustered_LockBuckets(bucketIds, getLocalAddress(), getCluster().getCoordinator());
    }

    /**
     * Locks the buckets which are under the process of state transfer. A locked
     * bucket can not be assigned to a node while loadbalancing. Only a
     * coordinator node can lock the buckets.
     *
     * @param info
     * @return
     */
    private java.util.HashMap handleLockBuckets(Object info) {
        java.util.HashMap result = null;
        try {
            Object[] package_Renamed = (Object[]) ((info instanceof Object[]) ? info : null);
            java.util.ArrayList bucketIds = (java.util.ArrayList) ((package_Renamed[0] instanceof java.util.ArrayList) ? package_Renamed[0] : null);
            Address owner = (Address) ((package_Renamed[1] instanceof Address) ? package_Renamed[1] : null);

            if (getCluster().getIsCoordinator()) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("received lock bucket request from " + owner);
                }
                result = _distributionMgr.LockBuckets(bucketIds, owner);
            } else {
                getCacheLog().Error("PartitionedServerCache.handleLockBuckets", "i am not coordinator but i have received bucket lock request.");
            }
        } catch (Exception e) {
            return result;
        } finally {
        }
        return result;
    }

    private void handleAnnounceStateTransfer(Object info, Address src) {
        java.util.ArrayList bucketIds = (java.util.ArrayList) ((info instanceof java.util.ArrayList) ? info : null);
        _distributionMgr.ChangeBucketStatusToStateTransfer(bucketIds, src);
    }

    private OperationResponse handleTransferBucket(Address sender, Object info) throws StateTransferException, OperationFailedException, CacheException, LockingException {
        Object[] pack = (Object[]) ((info instanceof Object[]) ? info : null);

        java.util.ArrayList bucketIds = (java.util.ArrayList) ((pack[0] instanceof java.util.ArrayList) ? pack[0] : null);
        byte transferType = (Byte) pack[1];
        boolean sparsedBuckets = (Boolean) pack[2];
        int expectedTxfrId = (Integer) pack[3];
        boolean isBalanceDataLoad = (Boolean) pack[4];

        if (_corresponders == null) {
            _corresponders = new java.util.HashMap();
        }

        StateTxfrCorresponder corresponder = null;
        synchronized (_corresponders) {
            corresponder = (StateTxfrCorresponder) ((_corresponders.get(sender) instanceof StateTxfrCorresponder) ? _corresponders.get(sender) : null);

            if (corresponder == null) {
                corresponder = new StateTxfrCorresponder(this, _distributionMgr, sender, transferType);
                _corresponders.put(sender, corresponder);
            }
        }
        corresponder.setIsBalanceDataLoad(isBalanceDataLoad);
        //ask the corresponder to transfer data for the bucket(s).

        com.alachisoft.tayzgrid.caching.topologies.clustered.StateTxfrInfo transferInfo = corresponder.TransferBucket(bucketIds, sparsedBuckets, expectedTxfrId);
        OperationResponse rsp = new OperationResponse();
        rsp.SerializablePayload = transferInfo;
        if (transferInfo.getPayLoad() != null) {
            rsp.UserPayload = transferInfo.getPayLoad().toArray(new Object[0]);
        }
        return rsp;
    }

    private void handleReleaseBuckets(Object info, Address src) {
        try {
            java.util.ArrayList bucketIds = (java.util.ArrayList) ((info instanceof java.util.ArrayList) ? info : null);
            _distributionMgr.ReleaseBuckets(bucketIds, src);
        } catch (Exception e) {
            getCacheLog().Error("PartitionedServerCache.handleReleaseBuckets", e.toString());
        }
    }

    @Override
    public void AckStateTxfrCompleted(Address owner, java.util.ArrayList bucketIds) throws OperationFailedException, GeneralFailureException {
        Clustered_AckStateTxfrCompleted(owner, bucketIds);
    }

    private void handleAckStateTxfr(Object info, Address client) {
        Object[] keys = null;
        try {
            java.util.ArrayList bucketIds = (java.util.ArrayList) info;
            java.util.Iterator ie = bucketIds.iterator();
            while (ie.hasNext()) {
                //remove this bucket from the local buckets.
                //this bucket has been transfered to some other node.
                getInternalCache().RemoveBucket((Integer) ie.next());
            }

        } catch (Exception e) {
            getCacheLog().DevTrace("handleAckStateTxfr", "handleAckStateTxfr " + e.toString());
        }
    }

    private void handleSignalEndOfStateTxfr(Address requestingNode) {
        if (_corresponders != null) {
            StateTxfrCorresponder cor = (StateTxfrCorresponder) ((_corresponders.get(requestingNode) instanceof StateTxfrCorresponder) ? _corresponders.get(requestingNode) : null);
            if (cor != null) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.handleSignalEndOfTxfr", requestingNode.toString() + " Corresponder removed.");
                }
                cor.dispose();
                _corresponders.remove(requestingNode);
            }
        }
    }

    @Override
    public void UpdateLocalBuckets() {
        if (getBucketsOwnershipMap() != null) {
            if (getBucketsOwnershipMap().containsKey(getLocalAddress())) {
                Object tempVar = getBucketsOwnershipMap().get(getLocalAddress());
                java.util.ArrayList myBuckets = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
                if (myBuckets != null) {
                    java.util.ArrayList<Integer> bucketIds = new java.util.ArrayList<Integer>();
                    for (int i = 0; i < myBuckets.size(); i++) {
                        HashMapBucket bucket = (HashMapBucket) myBuckets.get(i);
                        bucketIds.add(bucket.getBucketId());
                    }
                    _internalCache.UpdateLocalBuckets(bucketIds);
                }
            }
        }
    }

    @Override
    public void ReplicateConnectionString(String connString, boolean isSql) throws GeneralFailureException, CacheException {
        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        try {
            if (_internalCache == null) {
                throw new UnsupportedOperationException();
            }
            if (getServers().size() >= 1) {
                Clustered_ReplicateConnectionString(getCluster().getServers(), connString, isSql, true);
            }
        } finally {
        }
    }

    

    /**
     * returns the number of objects contained in the cache.
     */
    @Override
    public long getCount() throws GeneralFailureException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }
        long count = 0;
        if (IsInStateTransfer()) {
            count = Clustered_Count(GetDestInStateTransfer());
        } else {
            count = Clustered_Count(this.getServers());
        }
        return count;
    }

    /**
     * Hanlde cluster-wide Get(key) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleCount() throws OperationFailedException {
        try {
            return Local_Count();
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return 0;
    }

    @Override
    public int getServersCount() {
        return getCluster().getValidMembers().size();
    }

    /**
     *
     * @return
     */
    @Override
    public java.util.List getActiveServers()
    {
        return getServers();
    }
    
    @Override
    public boolean IsServerNodeIp(Address clientAddress) {
        for (Iterator it = getCluster().getServers().iterator(); it.hasNext();) {
            Address addr = (Address) it.next();
            if (addr.getIpAddress().equals(clientAddress.getIpAddress())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether the cache contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     */
    @Override
    public boolean Contains(Object key, String group, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PrtCache.Cont", "");
        }

        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);
        boolean suspectedErrorOccured = false;
        Address node = null;

        while (true) {
            try {
                if (_internalCache == null) {
                    throw new UnsupportedOperationException();
                }

                node = GetNextNode(key, group);
                if (node != null) {
                    if (node.compareTo(getCluster().getLocalAddress()) == 0) {
                        return Local_Contains(key, operationContext);
                    } else {
                        return Clustered_Contains(node, key, operationContext) != null;

                    }
                }
                return false;
            } catch (SuspectedException se) {
                suspectedErrorOccured = true;
                //we redo the operation
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.Contains", node + " left while Contains. Error: " + se.toString());
                }
                continue;
            } catch (TimeoutException te) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.Contains", node + " operation timed out. Error: " + te.toString());
                }
                if (suspectedErrorOccured) {
                    suspectedErrorOccured = false;
                    continue;
                } else {
                    throw new GeneralFailureException(te.getMessage(), te);
                }
            } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
                _distributionMgr.Wait(key, group);
            } finally {
            }
        }
    }

    /**
     * Determines whether the cache contains the given keys.
     *
     * @param keys The keys to locate in the cache.
     * @return list of available keys from the given key list
     */
    @Override
    public java.util.HashMap Contains(Object[] keys, String group, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap targetNodes = null;
        java.util.HashMap result = new java.util.HashMap();
        java.util.HashMap tmpKeyTbl = null;
        Address targetNode = null;

        java.util.ArrayList totalFoundKeys = new java.util.ArrayList();
        java.util.ArrayList totalRremainingKeys = new java.util.ArrayList();
        java.util.ArrayList totalKeys = new java.util.ArrayList(Arrays.asList(keys));

        do {
            targetNodes = GetTargetNodes(totalKeys, group);
            Iterator ide = targetNodes.entrySet().iterator();
            java.util.HashMap keyList = null;
            //We select one node at a time for contain operation.
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                targetNode = (Address) ((pair.getKey() instanceof Address) ? pair.getKey() : null);
                keyList = (java.util.HashMap) pair.getValue();

                if (targetNode != null) {
                    Object[] currentKeys = MiscUtil.GetArrayFromCollection(keyList.keySet());

                    try {
                        if (targetNode != null) {
                            if (targetNode.equals(getCluster().getLocalAddress())) {
                                tmpKeyTbl = Local_Contains(currentKeys, operationContext);
                            } else {
                                tmpKeyTbl = Clustered_Contains(targetNode, currentKeys, operationContext);
                            }
                        }
                    } catch (SuspectedException se) {
                        totalRremainingKeys.addAll(Arrays.asList(currentKeys));

                        //we redo the operation
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PoRServerCache.Contains", targetNode + " left while addition");
                        }
                        continue;
                    } catch (com.alachisoft.tayzgrid.common.exceptions.TimeoutException te) {
                        totalRremainingKeys.addAll(Arrays.asList(currentKeys));

                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PoRServerCache.Contains", targetNode + " operation timed out");
                        }
                        continue;
                    }

                    if (tmpKeyTbl != null) {
                        //list of items which have been transfered to some other node.
                        //so we need to revisit them.
                        java.util.ArrayList transferredKeys = (java.util.ArrayList) ((tmpKeyTbl.get("items-transfered") instanceof java.util.ArrayList) ? tmpKeyTbl.get("items-transfered") : null);
                        if (transferredKeys != null && transferredKeys.size() > 0) {
                            totalRremainingKeys.addAll(transferredKeys);
                        }

                        java.util.ArrayList foundKeys = (java.util.ArrayList) ((tmpKeyTbl.get("items-found") instanceof java.util.ArrayList) ? tmpKeyTbl.get("items-found") : null);
                        if (foundKeys != null && foundKeys.size() > 0) {
                            totalFoundKeys.addAll(foundKeys);
                        }
                    }
                }
            }

            totalKeys = new java.util.ArrayList(totalRremainingKeys);
            totalRremainingKeys.clear();

        } while (totalKeys.size() > 0);

        result.put("items-found", totalFoundKeys);

        return result;
    }

    /**
     * Determines whether the local cache contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     */
    private boolean Local_Contains(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        boolean retVal = false;
        if (_internalCache != null) {
            retVal = _internalCache.Contains(key, operationContext);
        }
        return retVal;
    }

    /**
     * Determines whether the local cache contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     */
    private java.util.HashMap Local_Contains(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        java.util.HashMap retVal = new java.util.HashMap();
        if (_internalCache != null) {
            retVal = _internalCache.Contains(keys, operationContext);
        }
        return retVal;
    }

    /**
     * Hanlde cluster-wide Get(key) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleContains(Object info) throws OperationFailedException {
        try {
            OperationContext operationContext = null;
            Object[] args = (Object[]) info;
            if (args.length > 1) {
                operationContext = (OperationContext) ((args[1] instanceof OperationContext) ? args[1] : null);
            }
            if (args[0] instanceof Object[]) {
                return Local_Contains((Object[]) args[0], operationContext);
            } else {
                if (Local_Contains(args[0], operationContext)) {
                    return true;
                }
            }
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return null;
    }

    /**
     * Removes all entries from the store.
     *
     *
     * This method invokes <see cref="handleClear"/> on every node in the
     * partition, which then fires OnCacheCleared locally. The <see
     * cref="handleClear"/> method on the coordinator will also trigger a
     * cluster-wide notification to the clients.
     *
     */
    @Override
    public void Clear(CallbackEntry cbEntry, DataSourceUpdateOptions updateOptions, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException {
        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        String taskId = null;
        if (updateOptions == DataSourceUpdateOptions.WriteBehind) {
            taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }

        if (getServers().size() > 1) {
            Clustered_Clear(cbEntry, taskId, false, operationContext);
        } else {
            handleClear(getCluster().getLocalAddress(), new Object[]{
                cbEntry, taskId, operationContext
            });
        }

    }

    /**
     * Removes all entries from the local cache only.
     */
    private void Local_Clear(Address src, CallbackEntry cbEntry, String taskId, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        //AbortStateTransfer();
        if (_internalCache != null) {
            _internalCache.Clear(null, DataSourceUpdateOptions.None, operationContext);
//            if (taskId != null && getCluster().getIsCoordinator()) {
//                CacheEntry entry = new CacheEntry(cbEntry, null, null);
//                if (operationContext.Contains(OperationContextFieldName.ReadThruProviderName)) {
//                    entry.setProviderName((String) operationContext.GetValueByField(OperationContextFieldName.ReadThruProviderName));
//                }
//            }
        }
    }

    private void AbortStateTransfer() {
        //Check if this node is involved in any kind of state transfer.
        if (_stateTransferTask != null && _stateTransferTask.getIsRunning()) {
            _stateTransferTask.Stop();
        }
        if (_corresponders != null && _corresponders.size() > 0) {
            _corresponders.clear();
        }
    }

    /**
     * Removes all entries from the store.
     *
     *
     * <see cref="handleClear"/> is called on every server node in a cluster.
     * Therefore in order to ensure that only one notification is generated for
     * the cluster, only the coordinator node replicates the notification to the
     * clients.
     * <p>
     * <b>Note: </b> The notification to the servers is handled in their <see
     * cref="OnCacheCleared"/> callback. Therefore the servers generate the
     * notifications locally. Only the clients need to be notified. </p>
     *
     */
    private Object handleClear(Address src, Object operand) throws OperationFailedException {
        try {
            Object[] args = (Object[]) ((operand instanceof Object[]) ? operand : null);
            CallbackEntry cbEntry = null;
            String taskId = null;
            OperationContext operationContext = null;

            if (args.length > 0) {
                cbEntry = (CallbackEntry) ((args[0] instanceof CallbackEntry) ? args[0] : null);
            }
            if (args.length > 1) {
                taskId = (String) ((args[1] instanceof String) ? args[1] : null);
            }
            if (args.length > 2) {
                operationContext = (OperationContext) ((args[2] instanceof OperationContext) ? args[2] : null);
            }

            Local_Clear(src, cbEntry, taskId, operationContext);

            /**
             * Only the coordinator replicates notification.
             */
            if (getIsCacheClearNotifier()) {
                if (getValidMembers().size() > 1) {
                    if (getCluster().getIsCoordinator()) {
                        getCluster().SendNoReplyMessageAsync(new Function(OpCodes.NotifyClear.getValue(), null, false));
                    }
                } else {
                    handleNotifyCacheCleared();
                }
            }
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return null;
    }

    protected QueryResultSet Local_Search(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return _internalCache.Search(query, values, operationContext);
    }

    @Override
    protected QueryResultSet Local_SearchEntries(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return _internalCache.SearchEntries(query, values, operationContext);
    }

    public final QueryResultSet handleSearch(Object info) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            java.util.ArrayList keyList = new java.util.ArrayList();
            Object[] data = (Object[]) info;
            return _internalCache.Search((String) ((data[0] instanceof String) ? data[0] : null), (java.util.Map) ((data[1] instanceof java.util.Map) ? data[1] : null), (OperationContext) ((data[2] instanceof OperationContext) ? data[2] : null));
        }

        return null;
    }

    public final QueryResultSet handleSearchEntries(Object info) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            java.util.HashMap keyValues = new java.util.HashMap();
            Object[] data = (Object[]) info;
            return _internalCache.SearchEntries((String) ((data[0] instanceof String) ? data[0] : null), (java.util.Map) ((data[1] instanceof java.util.Map) ? data[1] : null), (OperationContext) ((data[2] instanceof OperationContext) ? data[2] : null));
        }

        return null;
    }

    /**
     * Retrieve the object from the cache if found in a specified group and
     * subgroup.
     *
     * @param key key of the entry.
     * @param group group name.
     * @param subGroup subgroup name.
     * @return cache entry.
     */
    @Override
    public CacheEntry GetGroup(Object key, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        CacheEntry e = null;
        boolean suspectedErrorOccured = false;
        Address address = null;

        while (true) {
            address = GetNextNode(key, group);

            if (address == null) {
                getCacheLog().Error("PartitionedServerCache.GetGroup(): ", "specified key does not map to any node. return.");
                return null;
            }

            try {
                if (address.compareTo(getCluster().getLocalAddress()) == 0) {
                    e = Local_GetGroup(key, group, subGroup, version, lockId, lockDate, lockExpiration, accessType, operationContext);
                } else {
                    e = Clustered_GetGroup(address, key, group, subGroup, version, lockId, lockDate, lockExpiration, accessType, operationContext);
                }

                if (e == null) {
                    _stats.BumpMissCount();
                } else {
                    _stats.BumpHitCount();
                }
                return e;
            } catch (SuspectedException se) {
                suspectedErrorOccured = true;
                //we redo the operation
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.GetGroup", address + " left while addition: " + se.toString());
                }
                continue;
            } catch (TimeoutException te) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.GetGroup", address + " operation timed out: " + te.toString());
                }
                if (suspectedErrorOccured) {
                    suspectedErrorOccured = false;
                    continue;
                } else {
                    throw new GeneralFailureException(te.getMessage(), te);
                }
            } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
                _distributionMgr.Wait(key, group);
            }
        }
    }

    @Override
    protected java.util.HashMap Local_GetTagData(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        return _internalCache.GetTagData(tags, comparisonType, operationContext);
    }

    public final java.util.HashMap handleGetTag(Object info) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            Object[] data = (Object[]) info;
            java.util.HashMap keyValues = _internalCache.GetTagData((String[]) ((data[0] instanceof String[]) ? data[0] : null), (TagComparisonType) data[1], (OperationContext) ((data[2] instanceof OperationContext) ? data[2] : null));
            return keyValues;
        }

        return null;
    }

    /**
     * Retrieve the object from the cache. A string key is passed as parameter.
     *
     * @param key key of the entry.
     * @return cache entry.
     */
    @Override
    public CacheEntry Get(Object key, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType access, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PrtCache.Get", "");
        }

        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        Address address = null;
        CacheEntry e = null;
        boolean suspectedErrorOccured = false;

        while (true) {
            address = GetNextNode(key, null);

            if (address == null) {
                getCacheLog().Error("PartitionedServerCache.Get()", "specified key does not map to any node. return.");
                return null;
            }

            try {
                if (address.compareTo(getCluster().getLocalAddress()) == 0) {
                    e = Local_Get(key, version, lockId, lockDate, lockExpiration, access, operationContext);
                } else {
                    e = Clustered_Get(address, key, version, lockId, lockDate, lockExpiration, access, operationContext);

                }

                if (e == null) {
                    _stats.BumpMissCount();
                } else {
                    _stats.BumpHitCount();
                }
                break;
            } catch (SuspectedException se) {
                suspectedErrorOccured = true;
                //we redo the operation
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.Get", address + " left while Get. Error: " + se.toString());
                }
                continue;
            } catch (TimeoutException te) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.Get", address + " operation timed out. Error: " + te.toString());
                }
                if (suspectedErrorOccured) {
                    suspectedErrorOccured = false;
                    continue;
                } else {
                    throw new GeneralFailureException(te.getMessage(), te);
                }
            } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
                _distributionMgr.Wait(key, null);
            }
        }
        return e;

    }

    private java.util.HashMap OptimizedGet(Object[] keys, OperationContext operationContext) throws GeneralFailureException, OperationFailedException, CacheException, LockingException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PoRCache.GetBlk", "");
        }

        java.util.HashMap result = new java.util.HashMap();

        java.util.ArrayList remainingKeys = new java.util.ArrayList();

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        result = Local_Get(keys, operationContext);

        if (result != null && result.size() > 0) {
            Iterator ie = result.entrySet().iterator();
            while (ie.hasNext()) {
                Map.Entry pair = (Map.Entry) ie.next();
                if (pair.getValue() instanceof StateTransferException) {
                    remainingKeys.add(pair.getKey());
                }
            }
        }
        Iterator ine = result.entrySet().iterator();
        java.util.ArrayList updateIndiceKeyList = null;
        while (ine.hasNext()) {
            Map.Entry pair = (Map.Entry) ine.next();
            CacheEntry e = (CacheEntry) ((pair.getValue() instanceof CacheEntry) ? pair.getValue() : null);
            if (e == null) {
                _stats.BumpMissCount();
            } else {
                if (updateIndiceKeyList == null) {
                    updateIndiceKeyList = new java.util.ArrayList();
                }

                _stats.BumpHitCount();

                if ((e.getExpirationHint() != null && e.getExpirationHint().getIsVariant())) {
                    updateIndiceKeyList.add(pair.getKey());
                }
            }
        }

        if (updateIndiceKeyList != null && updateIndiceKeyList.size() > 0) {
            UpdateIndices(updateIndiceKeyList.toArray(new Object[0]), true, operationContext);
        }

        if (remainingKeys.size() > 0) {
            java.util.HashMap tmpResult = ClusteredGet(remainingKeys.toArray(new Object[0]), operationContext);
            Iterator ide = tmpResult.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry entry = (Map.Entry) ide.next();
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    private java.util.HashMap ClusteredGet(Object[] keys, OperationContext operationContext) throws GeneralFailureException, OperationFailedException, CacheException, LockingException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PoRCache.GetBlk", "");
        }

        java.util.HashMap result = new java.util.HashMap();
        java.util.HashMap targetNodes = null;
        java.util.HashMap tmpData = null;

        java.util.ArrayList totalKeys = new java.util.ArrayList(Arrays.asList(keys));
        java.util.ArrayList totalRemainingKeys = new java.util.ArrayList();

        Address targetNode = null;

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        do {
            targetNodes = GetTargetNodes(totalKeys, null);
            Iterator ide = targetNodes.entrySet().iterator();
            java.util.HashMap keyList = null;
            //We select one node at a time for operation.
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                targetNode = (Address) ((pair.getKey() instanceof Address) ? pair.getKey() : null);
                keyList = (java.util.HashMap) pair.getValue();

                if (targetNode != null) {
                    Object[] currentKeys = MiscUtil.GetArrayFromCollection(keyList.keySet());
                    try {
                        if (targetNode.equals(getCluster().getLocalAddress())) {
                            tmpData = Local_Get(currentKeys, operationContext);
                        } else {
                            tmpData = Clustered_Get(targetNode, currentKeys, operationContext);
                        }
                    } catch (SuspectedException se) {
                        //we redo the operation
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PoRServerCache.Contains", targetNode + " left while addition");
                        }
                        totalRemainingKeys.add(currentKeys);
                        continue;
                    } catch (TimeoutException te) {
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PoRServerCache.Contains", targetNode + " operation timed out");
                        }
                        totalRemainingKeys.add(currentKeys);
                        continue;
                    }

                    if (tmpData != null && tmpData.size() > 0) {
                        Iterator ie = tmpData.entrySet().iterator();
                        while (ie.hasNext()) {
                            Map.Entry iePair = (Map.Entry) ie.next();
                            if (iePair.getValue() instanceof StateTransferException) {
                                totalRemainingKeys.add(iePair.getKey());
                            } else {
                                result.put(iePair.getKey(), iePair.getValue());
                            }
                        }
                    }
                }
            }

            totalKeys = new java.util.ArrayList(totalRemainingKeys);
            totalRemainingKeys.clear();
        } while (totalKeys.size() > 0);
        Iterator ine = result.entrySet().iterator();
        java.util.ArrayList updateIndiceKeyList = null;
        while (ine.hasNext()) {
            Map.Entry pair = (Map.Entry) ine.next();
            CacheEntry e = (CacheEntry) pair.getValue();
            if (e == null) {
                _stats.BumpMissCount();
            } else {
                if (updateIndiceKeyList == null) {
                    updateIndiceKeyList = new java.util.ArrayList();
                }
                _stats.BumpHitCount();
                if ((e.getExpirationHint() != null && e.getExpirationHint().getIsVariant())) {
                    updateIndiceKeyList.add(pair.getKey());
                }

            }
        }
        if (updateIndiceKeyList != null && updateIndiceKeyList.size() > 0) {
            UpdateIndices(updateIndiceKeyList.toArray(new Object[0]), true, operationContext);
        }

        return result;
    }

    /**
     * Retrieve the objects from the cache. An array of keys is passed as
     * parameter.
     *
     * @param keys keys of the entries.
     * @return cache entries.
     */
    @Override
    public java.util.HashMap Get(Object[] keys, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PoRCache.GetBlk", "");
        }

        java.util.HashMap result = null;

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        long clientLastViewId = GetClientLastViewId(operationContext);

        if (clientLastViewId == getCluster().getLastViewID() && !IsInStateTransfer()) {
            result = OptimizedGet(keys, operationContext);
        } else {
            result = ClusteredGet(keys, operationContext);
        }
        return result;
    }

    /**
     * Retrieve the objects from the cache. An array of keys is passed as
     * parameter.
     *
     * @param keys keys of the entries.
     * @return cache entries.
     */
    @Override
    public java.util.HashMap GetGroup(Object[] keys, String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        java.util.HashMap targetNodes = null;
        java.util.ArrayList contains = new java.util.ArrayList();
        java.util.HashMap result = new java.util.HashMap();
        java.util.HashMap tmpData = null;
        java.util.HashMap totalKeys = null;
        Address targetNode = null;
        boolean suspectedErrorOccured = false;

        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        try {
            if (_internalCache == null) {
                throw new UnsupportedOperationException();
            }

            totalKeys = new java.util.HashMap();
            for (int i = 0; i < keys.length; i++) {
                totalKeys.put(keys[i], null);
            }

            while (totalKeys.size() > 0) {
                java.util.ArrayList _keys = new java.util.ArrayList(totalKeys.keySet());

                targetNodes = GetTargetNodes(_keys, group);
                Iterator ide = targetNodes.entrySet().iterator();
                java.util.HashMap keyList = null;
                //We select one node at a time for contain operation.
                while (ide.hasNext()) {
                    Map.Entry pair = (Map.Entry) ide.next();
                    targetNode = (Address) ((pair.getKey() instanceof Address) ? pair.getKey() : null);
                    keyList = (java.util.HashMap) pair.getValue();
                    if (targetNode != null) {
                        break;
                    }
                }

                if (targetNode != null) {
                    Object[] currentKeys = new Object[keyList.size()];
                    int j = 0;
                    for (Object key : keyList.keySet()) {
                        currentKeys[j] = key;
                        j++;
                    }

                    try {
                        if (targetNode.compareTo(getLocalAddress()) == 0) {
                            tmpData = Local_GetGroup(currentKeys, group, subGroup, operationContext);
                        } else {
                            tmpData = Clustered_GetGroup(targetNode, currentKeys, group, subGroup, operationContext);
                        }
                    } catch (SuspectedException se) {
                        suspectedErrorOccured = true;
                        //we redo the operation
                        if (getCacheLog().getIsInfoEnabled()) {
                            getCacheLog().Info("PartitionedServerCache.GetGroup", targetNode + " left while GetGroup. Error: " + se.toString());
                        }
                        continue;
                    } catch (TimeoutException te) {
                        if (getCacheLog().getIsInfoEnabled()) {
                            getCacheLog().Info("PartitionedServerCache.GetGroup", targetNode + " operation timed out. Error: " + te.toString());
                        }
                        if (suspectedErrorOccured) {
                            suspectedErrorOccured = false;
                        } else {
                            for (int i = 0; i < currentKeys.length; i++) {
                                totalKeys.remove(currentKeys[i]);
                            }
                        }
                        continue;
                    }

                    //list of items which have been transfered to some other node.
                    //so we need to revisit them.
                    java.util.ArrayList remainingKeys = null;

                    if (tmpData != null && tmpData.size() > 0) {
                        remainingKeys = new java.util.ArrayList();
                        Iterator ie = tmpData.entrySet().iterator();
                        while (ide.hasNext()) {
                            Map.Entry pair = (Map.Entry) ide.next();
                            if (pair.getValue() instanceof StateTransferException) {
                                remainingKeys.add(pair.getKey());
                            } else {
                                totalKeys.remove(pair.getKey());
                                result.put(pair.getKey(), pair.getValue());
                            }
                        }
                    }

                    if (remainingKeys != null && remainingKeys.size() > 0) {
                        _distributionMgr.Wait(remainingKeys.get(0), group);
                    }
                }
            }
            return result;
        } finally {
        }
    }

    /**
     * Retrieve the object from the local cache only.
     *
     * @param key key of the entry.
     * @return cache entry.
     */
    public final CacheEntry Local_Get(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        CacheEntry retVal = null;
        if (_internalCache != null) {
            retVal = _internalCache.Get(key, operationContext);
        }
        return retVal;
    }

    /**
     * Retrieve the object from the local cache only.
     *
     * @param key key of the entry.
     * @return cache entry.
     */
    private CacheEntry Local_Get(Object key, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType access, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        CacheEntry retVal = null;
        if (_internalCache != null) {
            retVal = _internalCache.Get(key, version, lockId, lockDate, lockExpiration, access, operationContext);
        }
        return retVal;
    }

    /**
     * Retrieve the objects from the local cache only.
     *
     * @param keys keys of the entry.
     * @return cache entries.
     */
    private java.util.HashMap Local_Get(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        if (_internalCache != null) {
            return _internalCache.Get(keys, operationContext);
        }

        return null;
    }

    /**
     * Retrieve the list of keys fron the cache for the given group or sub
     * group.
     */
    private CacheEntry Local_GetGroup(Object key, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            return _internalCache.GetGroup(key, group, subGroup, version, lockId, lockDate, lockExpiration, accessType, operationContext);
        }

        return null;
    }

    @Override
    protected java.util.ArrayList Local_GetTagKeys(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            return _internalCache.GetTagKeys(tags, comparisonType, operationContext);
        }

        return null;
    }

    /**
     * Retrieve the list of keys fron the cache for the given group or sub
     * group.
     */
    private java.util.HashMap Local_GetGroup(Object[] keys, String group, String subGroup, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            return _internalCache.GetGroup(keys, group, subGroup, operationContext);
        }

        return null;
    }

    /**
     * Retrieve the list of keys fron the cache for the given group or sub
     * group.
     */
    @Override
    protected java.util.ArrayList Local_GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            return _internalCache.GetGroupKeys(group, subGroup, operationContext);
        }

        return null;
    }

    /**
     * Retrieve the list of keys fron the cache for the given group or sub
     * group.
     */
    @Override
    protected java.util.HashMap Local_GetGroupData(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            return _internalCache.GetGroupData(group, subGroup, operationContext);
        }

        return null;
    }

    /**
     * Hanlde cluster-wide Get(key) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleGet(Object info) throws OperationFailedException {
        try {
            Object[] args = (Object[]) ((info instanceof Object[]) ? info : null);
            if (args[0] instanceof Object[]) {
                java.util.HashMap table = Local_Get((Object[]) args[0], (OperationContext) ((args[1] instanceof OperationContext) ? args[1] : null));
                return table;
            } else {
                Object key = args[0];
                Object lockId = args[1];
                java.util.Date lockDate = (java.util.Date) args[2];
                LockAccessType accessType = (LockAccessType) args[3];
                long version = (Long) args[4];
                LockExpiration lockExpiration = (LockExpiration) args[5];
                OperationContext operationContext = (OperationContext) ((args[6] instanceof OperationContext) ? args[6] : null);

                tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
                tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
                tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
                CacheEntry entry = Local_Get(key, tempRef_version, tempRef_lockId, tempRef_lockDate, lockExpiration, accessType, operationContext);
                version = tempRef_version.argvalue;
                lockId = tempRef_lockId.argvalue;
                lockDate = tempRef_lockDate.argvalue;
                OperationResponse opRes = new OperationResponse();
                Object[] response = new Object[4];
                if (entry != null) {
                    UserBinaryObject ubObject = (UserBinaryObject) (entry.getValue() instanceof CallbackEntry ? ((CallbackEntry) entry.getValue()).getValue() : entry.getValue());
                    opRes.UserPayload = ubObject.getData();
                    response[0] = entry.CloneWithoutValue();
                }
                response[1] = lockId;
                response[2] = lockDate;
                response[3] = version;
                opRes.SerializablePayload = response;

                return opRes;
            }
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return null;
    }

    /**
     * Hanlde cluster-wide Get(key) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleGetGroup(Object info) throws OperationFailedException {
        try {
            Object[] package_Renamed = (Object[]) info;
            String group = (String) package_Renamed[1];
            String subGroup = (String) package_Renamed[2];
            Object lockId = package_Renamed[3];
            java.util.Date lockDate = (java.util.Date) package_Renamed[4];
            LockAccessType accessType = (LockAccessType) package_Renamed[5];
            long version = (Long) package_Renamed[6];
            LockExpiration lockExpiration = (LockExpiration) package_Renamed[7];
            OperationContext operationContext = null;

            if (package_Renamed.length > 4) {
                operationContext = (OperationContext) ((package_Renamed[8] instanceof OperationContext) ? package_Renamed[8] : null);
            } else {
                operationContext = (OperationContext) ((package_Renamed[3] instanceof OperationContext) ? package_Renamed[3] : null);
            }

            if (package_Renamed[0] instanceof Object[]) {
                Object[] keys = (Object[]) package_Renamed[0];

                return Local_GetGroup(keys, group, subGroup, operationContext);
            } else {
                Object key = package_Renamed[0];
                tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
                tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
                tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
                Object tempVar = Local_GetGroup(key, group, subGroup, tempRef_version, tempRef_lockId, tempRef_lockDate, lockExpiration, accessType, operationContext);
                version = tempRef_version.argvalue;
                lockId = tempRef_lockId.argvalue;
                lockDate = tempRef_lockDate.argvalue;
                return tempVar;
            }
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return null;
    }

    /**
     * Adds a pair of key and value to the cache. Throws an exception or reports
     * error if the specified key already exists in the cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     *
     * This method either invokes <see cref="handleAdd"/> on every server-node
     * in the cluster, or invokes <see cref="Local_Add"/> locally. <see
     * cref="Local_Add"/> can only be called on one node in the cluster,
     * therefore it triggers a cluster-wide item added notification.
     *
     */
    @Override
    public CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException, SuspectedException, TimeoutException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PrtCache.Add_1", "");
        }

        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);
        Address targetNode = null;
        String taskId = null;
        CacheAddResult result = CacheAddResult.Failure;
        try {
            if (_internalCache == null) {
                throw new UnsupportedOperationException();
            }

            if (cacheEntry.getFlag() != null && cacheEntry.getFlag().IsBitSet((byte) BitSetConstants.WriteBehind)) {
                taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
            }

           

            tangible.RefObject<Address> tempRef_targetNode = new tangible.RefObject<Address>(targetNode);
            result = Safe_Clustered_Add(key, cacheEntry, tempRef_targetNode, taskId, operationContext);
            targetNode = tempRef_targetNode.argvalue;
         
            return result;
        } finally {
        }
    }

    /**
     *
     *
     * @param key
     * @param eh
     * @return
     */
    @Override
    public boolean Add(Object key, String group, ExpirationHint eh, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        boolean result = false;
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PrtCache.Add_3", "");
        }

        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        CacheEntry cacheEntry = new CacheEntry();
        cacheEntry.setExpirationHint(eh);

       

        boolean suspectedErrorOccured = false;
        Address targetNode = null;

        while (true) {
            try {
                targetNode = GetNextNode(key, group);
                if (targetNode != null) {
                    if (targetNode.compare(getLocalAddress()) == 0) {
                        result = Local_Add(key, eh, operationContext);
                        break;
                    } else {
                        result = Clustered_Add(targetNode, key, eh, operationContext);
                        break;
                    }
                }
                result = false;
                break;
            } catch (SuspectedException se) {
                suspectedErrorOccured = true;
                //we redo the operation
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.Add", targetNode + " left while addition. Error: " + se.toString());
                }
                continue;
            } catch (TimeoutException te) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.Add", targetNode + " operation timed out. Error: " + te.toString());
                }
                if (suspectedErrorOccured) {
                    suspectedErrorOccured = false;
                    continue;
                } else {
                    throw new GeneralFailureException(te.getMessage(), te);
                }
            } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
                _distributionMgr.Wait(key, group);
            }

        }

   
        return result;
    }

   
    /**
     * Add the object to the local cache.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method can only be called on one node in the cluster. It triggers
     * <see cref="OnItemAdded"/>, which initiates a cluster-wide item added
     * notification.
     *
     */
    private CacheAddResult Local_Add(Object key, CacheEntry cacheEntry, Address src, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, SuspectedException, TimeoutException {
        CacheAddResult retVal = CacheAddResult.Failure;

        CacheEntry clone = null;
        if (taskId != null && cacheEntry.getHasQueryInfo()) {
            clone = (CacheEntry) cacheEntry.clone();
        } else {
            clone = cacheEntry;
        }

        if (_internalCache != null) {
            retVal = _internalCache.Add(key, cacheEntry, notify, operationContext);

            if (taskId != null && retVal == CacheAddResult.Success) {
                super.AddWriteBehindTask(src, key, clone, taskId, OpCode.Add, WriteBehindAsyncProcessor.TaskState.Execute, operationContext);
            }
        }

        return retVal;
    }

    /**
     *
     * @param key
     * @param eh
     * @return
     */
    private boolean Local_Add(Object key, ExpirationHint eh, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        boolean retVal = false;
        if (_internalCache != null) {
            retVal = _internalCache.Add(key, eh, operationContext);
        }
        return retVal;
    }



    /**
     * A wrapper method that reperform the operations that fail because of the
     * members suspected during operations.
     *
     * @param key
     * @param cacheEntry
     * @return
     */
    private CacheAddResult Safe_Clustered_Add(Object key, CacheEntry cacheEntry, tangible.RefObject<Address> targetNode, String taskId, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, SuspectedException, TimeoutException {
        boolean suspectedErrorOccured = false;
        int maxTries = _stats.getNodes().size() > 3 ? 3 : _stats.getNodes().size() - 1;
        CacheAddResult result = CacheAddResult.Failure;
        targetNode.argvalue = null;
        do {
            String group = cacheEntry.getGroupInfo() == null ? null : cacheEntry.getGroupInfo().getGroup();

            try {
                targetNode.argvalue = GetNextNode(key, group);

                //possible in case of strict affinity...
                if (targetNode.argvalue == null) {
                    throw new OperationFailedException("No target node available to accommodate the data.");
                }

                if (targetNode.argvalue.compareTo(getLocalAddress()) == 0) {
                    result = Local_Add(key, cacheEntry, getCluster().getLocalAddress(), taskId, true, operationContext);
                } else {
                    result = Clustered_Add(targetNode.argvalue, key, cacheEntry, taskId, operationContext);
                }
                return result;
            } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
                _distributionMgr.Wait(key, group);
            } catch (TimeoutException te) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedCache.Clustered_Add()", te.toString());
                }
                if (suspectedErrorOccured) {
                    suspectedErrorOccured = false;
                    continue;
                } else {
                    throw te;
                }
            } catch (SuspectedException e) {
                suspectedErrorOccured = true;
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedCache.Clustered_Add()", e.toString());
                }
                if (maxTries == 0) {
                    throw e;
                }
                maxTries--;
            }
        } while (maxTries > 0);
        return result;
    }

    /**
     * Hanlde cluster-wide Add(key) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     *
     * This method can only be called on one node in the cluster. It triggers
     * <see cref="OnItemAdded"/>, which initiates a cluster-wide item added
     * notification.
     *
     */
    private Object handleAdd(Address src, Object info, Object[] userPayload) throws OperationFailedException {
        try {
            Object[] objs = (Object[]) info;
            OperationContext operationContext = null;
            String taskId = null;
            if (objs.length > 2) {
                taskId = (String) ((objs[2] instanceof String) ? objs[2] : null);
            }

            if (objs.length > 3) {
                operationContext = (OperationContext) ((objs[3] instanceof OperationContext) ? objs[3] : null);
            }

            if (objs[0] instanceof Object[]) {
                Object[] keys = (Object[]) objs[0];
                CacheEntry[] entries = (CacheEntry[]) ((objs[1] instanceof CacheEntry[]) ? objs[1] : null);
                return Local_Add(keys, entries, src, taskId, true, operationContext);
            } else {
                Object key = objs[0];
                CacheEntry e = (CacheEntry) ((objs[1] instanceof CacheEntry) ? objs[1] : null);
                e.setValue(userPayload);
                CacheAddResult result = Local_Add(key, e, src, taskId, true, operationContext);
                return result;
            }
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return CacheAddResult.Failure;
    }

    /**
     * Hanlde cluster-wide GetKeys(group, subGroup) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleGetKeys(Object info) throws OperationFailedException {
        try {
            Object[] objs = (Object[]) info;
            String group = (String) ((objs[0] instanceof String) ? objs[0] : null);
            String subGroup = (String) ((objs[1] instanceof String) ? objs[1] : null);
            OperationContext operationContext = null;
            if (objs.length > 2) {
                operationContext = (OperationContext) ((objs[2] instanceof OperationContext) ? objs[2] : null);
            }

            return Local_GetGroupKeys(group, subGroup, operationContext);
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return null;
    }

    /**
     * Hanlde cluster-wide GetData(group, subGroup) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleGetData(Object info) throws OperationFailedException {
        try {
            Object[] objs = (Object[]) info;
            String group = (String) ((objs[0] instanceof String) ? objs[0] : null);
            String subGroup = (String) ((objs[1] instanceof String) ? objs[1] : null);
            OperationContext operationContext = (OperationContext) ((objs[2] instanceof OperationContext) ? objs[2] : null);
            return Local_GetGroupData(group, subGroup, operationContext);
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return null;
    }

    /**
     * Add the expiration hint against the given key
     *
     * @param info
     * @return
     */
    private Object handleAddHint(Object info) throws OperationFailedException {
        try {
            Object[] objs = (Object[]) info;
            Object key = objs[0];
            ExpirationHint eh = (ExpirationHint) ((objs[1] instanceof ExpirationHint) ? objs[1] : null);
            OperationContext oc = (OperationContext) ((objs[2] instanceof OperationContext) ? objs[2] : null);

            return Local_Add(key, eh, oc);
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return false;
    }

   
    /**
     * Gets the data group info the item.
     *
     * @param key Key of the item
     * @return Data group info of the item
     */
    @Override
    public GroupInfo GetGroupInfo(Object key, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        _statusLatch.WaitForAny(NodeStatus.Running);

        GroupInfo info;
        info = Local_GetGroupInfo(key, operationContext);
        if (info == null) {
            ClusteredOperationResult result = Clustered_GetGroupInfo(key, operationContext);
            if (result != null) {
                Object tempVar = result.getResult();
                info = (GroupInfo) ((tempVar instanceof GroupInfo) ? tempVar : null);
            }
        }
        return info;
    }

    /**
     * Gets data group info the items
     *
     * @param keys Keys of the items
     * @return IDictionary of the data grup info the items
     */
    @Override
    public java.util.HashMap GetGroupInfoBulk(Object[] keys, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        _statusLatch.WaitForAny(NodeStatus.Running);

        java.util.HashMap infoTable;
        infoTable = Local_GetGroupInfoBulk(keys, operationContext);
        if (infoTable == null) {
            infoTable = (java.util.HashMap) Clustered_GetGroupInfoBulk(keys, operationContext);
        }

        return infoTable;
    }

    /**
     * Gets the data group info the item.
     *
     * @param key Key of the item
     * @return Data group info of the item
     */
    private GroupInfo Local_GetGroupInfo(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            return _internalCache.GetGroupInfo(key, operationContext);
        }
        return null;
    }

    /**
     * Gets data group info the items
     *
     * @param keys Keys of the items
     * @return IDictionary of the data grup info the items
     */
    private java.util.HashMap Local_GetGroupInfoBulk(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            return _internalCache.GetGroupInfoBulk(keys, operationContext);
        }
        return null;
    }

    /**
     * Handles the request for data group of the item
     *
     * @param info Key(s) of the item(s)
     * @return Data group info of the item(s)
     */
    private Object handleGetGroupInfo(Object info) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        Object result;
        OperationContext operationContext = null;
        Object[] args = (Object[]) info;
        if (args.length > 1) {
            operationContext = (OperationContext) ((args[1] instanceof OperationContext) ? args[1] : null);
        }
        if (args[0] instanceof Object[]) {
            result = Local_GetGroupInfoBulk((Object[]) args[0], operationContext);
        } else {
            result = Local_GetGroupInfo(args[0], operationContext);
        }

        return result;
    }

    /**
     * Handles the data integrity vication from a new joining node.
     *
     * @param info Data groups of the joining node.
     * @return True, if conflict found, otherwise false Vies whether the
     * data groups of the joining node exist on this node or not. We get the
     * list of all the groups contained by the cache. Remove the own data
     * affinity groups. From remaining groups, if the joining node groups exist,
     * we return true.
     */
    public final Object handleVerifyDataIntegrity(Object info) {
        java.util.ArrayList allGroups = null;
        java.util.ArrayList otherGroups = (java.util.ArrayList) info;
        java.util.ArrayList myBindedGroups = null;
        if (_statusLatch.getStatus().IsBitSet(NodeStatus.Running)) {
            try {
                allGroups = _internalCache.getDataGroupList();
                if (_stats != null && _stats.getLocalNode() != null && _stats.getLocalNode().getDataAffinity() != null) {
                    myBindedGroups = _stats.getLocalNode().getDataAffinity().getGroups();
                }

                if (allGroups != null) {
                    java.util.Iterator ie;
                    if (myBindedGroups != null) {
                        ie = myBindedGroups.iterator();
                        while (ie.hasNext()) {
                            allGroups.remove(ie.next());
                        }
                    }

                    ie = allGroups.iterator();
                    while (ie.hasNext()) {
                        Object obj = ie.next();
                        if (otherGroups.contains(obj)) {
                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("PartitionedServerCache.handleVerifyDataIntegrity", "data integrity not vied : group " + obj.toString()
                                        + "alread exists.");
                            }
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                getCacheLog().Error("PartitionedServerCache.handleVerifyDataIntegrity", e.toString());
            }
        }
        return false;
    }

    /**
     * Adds pairs keys and values to the cache. Returns the list of keys that
     * failed to Add.
     *
     * @param keys keys of the entries.
     * @param cacheEntries cache entries.
     * @return List of keys that are added or that alredy exists in the cache
     * and their status.
     *
     * This method either invokes <see cref="handleAdd"/> on every server-node
     * in the cluster, or invokes <see cref="Local_Add"/> locally. <see
     * cref="Local_Add"/> can only be called on one node in the cluster,
     * therefore it triggers a cluster-wide item added notification.
     *
     */
    private boolean IsClusterNodeExist(String InetAddress) {
        java.util.ArrayList serverIPList = new java.util.ArrayList();
        String onlyIP = "";
        for (Object ipPort : getCluster().getServers()) {
            onlyIP = ipPort.toString();
            onlyIP = onlyIP.substring(0, onlyIP.indexOf(':'));
            serverIPList.add(onlyIP);
        }
        if (serverIPList.contains(InetAddress)) {
            return true;
        } else {
            return false;
        }
    }

    private java.util.HashMap OptimizedAdd(Object[] keys, CacheEntry[] cacheEntries, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PoRCache.AddBlk", "");
        }

        java.util.HashMap result = new java.util.HashMap();
        java.util.ArrayList goodKeysList = new java.util.ArrayList();
        java.util.HashMap totalDepKeys = new java.util.HashMap();
        java.util.HashMap tmpResult = new java.util.HashMap();

        java.util.ArrayList totalKeys = new java.util.ArrayList(Arrays.asList(keys));
        java.util.ArrayList totalEntries = new java.util.ArrayList(Arrays.asList(cacheEntries));

        java.util.HashMap depResult = new java.util.HashMap();

        java.util.ArrayList successfulKeys = new java.util.ArrayList();
        java.util.ArrayList remainingKeys = new java.util.ArrayList();

        java.util.ArrayList goodEntriesList = new java.util.ArrayList();

       

       
        try {
            tmpResult = Local_Add(keys, cacheEntries, getCluster().getLocalAddress(), taskId, notify, operationContext);
        } catch (Exception ex) {
            if (ex instanceof BucketTransferredException) {
                tmpResult = new java.util.HashMap();
                for (int i = 0; i < keys.length; i++) {
                    tmpResult.put(keys[i], new OperationFailedException(ex.getMessage(), ex));
                }
            }

        }

        if (tmpResult != null && tmpResult.size() > 0) {
            Iterator ie = tmpResult.entrySet().iterator();
            while (ie.hasNext()) {
                Map.Entry pair = (Map.Entry) ie.next();
                if (pair.getValue() instanceof StateTransferException) {
                    remainingKeys.add(pair.getKey());
                } else {
                    if (pair.getValue() instanceof Exception) {
                        result.put(pair.getKey(), pair.getValue());
                    } else if (pair.getValue() instanceof CacheAddResult) {
                        CacheAddResult res = (CacheAddResult) pair.getValue();
                        switch (res) {
                            case Failure:
                                result.put(pair.getKey(), new OperationFailedException("Generic operation failure; not enough information is available."));
                                break;
                            case NeedsEviction:
                                result.put(pair.getKey(), new OperationFailedException("The cache is full and not enough items could be evicted."));
                                break;
                            case KeyExists:
                                result.put(pair.getKey(), new OperationFailedException("The specified key already exists."));
                                break;
                            case Success:
                                successfulKeys.add(pair.getKey());
                                int index = totalKeys.indexOf(pair.getKey());
                                if (index != -1) {
                                    depResult.put(pair.getKey(), totalEntries.get(index));
                                }
                                break;
                        }
                    }
                }
            }
        }

        if (remainingKeys.size() > 0) {
            Object[] currentKeys = new Object[remainingKeys.size()];
            CacheEntry[] currentValues = new CacheEntry[remainingKeys.size()];

            int j = 0;
            for (Iterator it = remainingKeys.iterator(); it.hasNext();) {
                Object key = it.next();
                int index = totalKeys.indexOf(key);
                if (index != -1) {
                    currentKeys[j] = totalKeys.get(index);
                    currentValues[j] = (CacheEntry) ((totalEntries.get(index) instanceof CacheEntry) ? totalEntries.get(index) : null);
                    j++;
                }
            }

            tmpResult = ClusteredAdd(currentKeys, currentValues, taskId, notify, operationContext);

            Iterator ide = tmpResult.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                result.put(pair.getKey(), pair.getValue());
            }

        }

        
        return result;
    }

    private java.util.HashMap ClusteredAdd(Object[] keys, CacheEntry[] cacheEntries, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        java.util.HashMap targetNodes = null;
        java.util.HashMap result = new java.util.HashMap();
        java.util.HashMap tmpResult = null;
        java.util.ArrayList totalKeys = new java.util.ArrayList(Arrays.asList(keys));
        java.util.ArrayList keysToAdd = new java.util.ArrayList(Arrays.asList(keys));

        java.util.HashMap failedTbl = new java.util.HashMap();
        java.util.ArrayList totalEntries = new java.util.ArrayList(Arrays.asList(cacheEntries));
        Address targetNode = null;
        Object[] currentKeys = null;
        CacheEntry[] currentValues = null;

        java.util.ArrayList goodKeysList = new java.util.ArrayList();
        java.util.ArrayList goodEntriesList = new java.util.ArrayList();
        java.util.HashMap<Object, CacheEntry> fullEntrySet = new java.util.HashMap<Object, CacheEntry>();
        java.util.HashMap depResult = new java.util.HashMap();
        java.util.HashMap totalDepKeys = new java.util.HashMap();

        java.util.HashMap totalSuccessfullKeys = new java.util.HashMap();
        java.util.HashMap totalRemainingKeys = new java.util.HashMap();

        String group = cacheEntries[0].getGroupInfo() == null ? null : cacheEntries[0].getGroupInfo().getGroup();

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        do {
            targetNodes = GetTargetNodes(keysToAdd, group);

            if (targetNodes != null && targetNodes.isEmpty()) {
                for (Object key : keysToAdd) {
                    result.put(key, new OperationFailedException("No target node available to accommodate the data."));
                }
                return result;
            }
            Iterator ide = targetNodes.entrySet().iterator();
            java.util.HashMap keyList = null;

            //We select one node at a time for Add operation.
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                targetNode = (Address) ((pair.getKey() instanceof Address) ? pair.getKey() : null);
                keyList = (java.util.HashMap) pair.getValue();

                if (targetNode != null && keyList != null) {
                    currentKeys = new Object[keyList.size()];
                    currentValues = new CacheEntry[keyList.size()];

                    int j = 0;
                    for (Object key : keyList.keySet()) {
                        int index = totalKeys.indexOf(key);
                        if (index != -1) {
                            currentKeys[j] = totalKeys.get(index);
                            currentValues[j] = (CacheEntry) ((totalEntries.get(index) instanceof CacheEntry) ? totalEntries.get(index) : null);
                            if (!fullEntrySet.containsKey(totalKeys.get(index))) {
                                fullEntrySet.put(totalKeys.get(index), (CacheEntry) totalEntries.get(index));
                            }
                            j++;
                        }
                    }

                    goodKeysList.clear();
                    goodEntriesList.clear();


                    try {
                        if (targetNode.equals(getCluster().getLocalAddress())) {
                            tmpResult = Local_Add(keys, cacheEntries, getCluster().getLocalAddress(), taskId, notify, operationContext);
                        } else {
                            tmpResult = Clustered_Add(targetNode, keys, cacheEntries, taskId, operationContext);
                        }
                    } catch (SuspectedException se) {
                        //we redo the operation
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PartitionedServerCache.SafeAdd", targetNode + " left while addition");
                        }

                        tmpResult = new java.util.HashMap();
                        for (int i = 0; i < keys.length; i++) {
                            tmpResult.put(keys[i], new GeneralFailureException(se.getMessage(), se));
                        }
                    } catch (TimeoutException te) {
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PartitionedServerCache.SafeAdd", targetNode + " operation timed out");
                        }

                        tmpResult = new java.util.HashMap();
                        for (int i = 0; i < keys.length; i++) {
                            tmpResult.put(keys[i], new GeneralFailureException(te.getMessage(), te));
                        }
                    } catch (BucketTransferredException ex) {
                        tmpResult = new java.util.HashMap();
                        for (int i = 0; i < keys.length; i++) {
                            tmpResult.put(keys[i], new OperationFailedException(ex.getMessage(), ex));
                        }
                    }

                    if (tmpResult != null && tmpResult.size() > 0) {
                        Iterator ie = tmpResult.entrySet().iterator();
                        while (ie.hasNext()) {
                            Map.Entry iePair = (Map.Entry) ie.next();
                            if (iePair.getValue() instanceof StateTransferException) {
                                totalRemainingKeys.put(iePair.getKey(), null);
                            } else {
                                if (iePair.getValue() instanceof Exception) {
                                    result.put(iePair.getKey(), iePair.getValue());
                                } else if (iePair.getValue() instanceof CacheAddResult) {
                                    CacheAddResult res = (CacheAddResult) iePair.getValue();
                                    switch (res) {
                                        case Failure:
                                            result.put(iePair.getKey(), new OperationFailedException("Generic operation failure; not enough information is available."));
                                            break;
                                        case NeedsEviction:
                                            result.put(iePair.getKey(), new OperationFailedException("The cache is full and not enough items could be evicted."));
                                            break;
                                        case KeyExists:
                                            result.put(iePair.getKey(), new OperationFailedException("The specified key already exists."));
                                            break;
                                        case Success:
                                            totalSuccessfullKeys.put(iePair.getKey(), null);
                                            int index = totalKeys.indexOf(iePair.getKey());
                                            
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            keysToAdd = new java.util.ArrayList(totalRemainingKeys.keySet());
            totalRemainingKeys.clear();
        } while (keysToAdd.size() > 0);

        if (totalSuccessfullKeys.size() > 0) {
            java.util.Iterator ie = totalSuccessfullKeys.keySet().iterator();
            while (ie.hasNext()) {
                Object key = ie.next();
                if (notify) {
                    // If everything went ok!, initiate local and cluster-wide notifications.
                    EventContext eventContext = CreateEventContextForGeneralDataEvent(com.alachisoft.tayzgrid.persistence.EventType.ITEM_ADDED_EVENT, operationContext, fullEntrySet.get(key), null);
                    RaiseItemAddNotifier(key, fullEntrySet.get(key), operationContext, eventContext);
                    handleNotifyAdd(new Object[]{key, operationContext, eventContext});
                }
              
            }

          
        }

        return result;
    }

    @Override
    public java.util.HashMap Add(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PoRCache.AddBlk", "");
        }
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        java.util.HashMap result = null;

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        String taskId = null;
        if (cacheEntries[0].getFlag() != null && cacheEntries[0].getFlag().IsBitSet((byte) BitSetConstants.WriteBehind)) {
            taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }

        long clientLastViewId = GetClientLastViewId(operationContext);

        if (clientLastViewId == getCluster().getLastViewID() && !IsInStateTransfer()) {
            result = OptimizedAdd(keys, cacheEntries, taskId, notify, operationContext);
        } else {
            result = ClusteredAdd(keys, cacheEntries, taskId, notify, operationContext);
        }
        return result;
    }

    /**
     * Add the objects to the local cache.
     *
     * @param keys key of the entry.
     * @return list of added keys.
     *
     * This method can only be called on one node in the cluster. It triggers
     * <see cref="OnItemAdded"/>, which initiates a cluster-wide item added
     * notification.
     *
     */
    private java.util.HashMap Local_Add(Object[] keys, CacheEntry[] cacheEntries, Address src, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        java.util.HashMap added = new java.util.HashMap();

        CacheEntry[] clone = null;
        if (taskId != null) {
            clone = new CacheEntry[cacheEntries.length];
            for (int i = 0; i < cacheEntries.length; i++) {
                if (cacheEntries[i].getHasQueryInfo()) {
                    clone[i] = (CacheEntry) cacheEntries[i].clone();
                } else {
                    clone[i] = cacheEntries[i];
                }
            }
        }

        if (_internalCache != null) {
            added = _internalCache.Add(keys, cacheEntries, notify, operationContext);

            if (taskId != null && added.size() > 0) {
                java.util.HashMap writeBehindTable = new java.util.HashMap();
                for (int i = 0; i < keys.length; i++) {
                    Object value = added.get(keys[i]);
                    if (value instanceof CacheAddResult && ((CacheAddResult) value) == CacheAddResult.Success) {
                        writeBehindTable.put(keys[i], clone[i]);
                    }
                }
                if (writeBehindTable.size() > 0) {
                    super.AddWriteBehindTask(src, writeBehindTable, null, taskId, OpCode.Add, WriteBehindAsyncProcessor.TaskState.Execute);
                }
            }
        }
        return added;
    }

    /**
     * Adds a pair of key and value to the cache. If the specified key already
     * exists in the cache; it is updated, otherwise a new item is added to the
     * cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     *
     * This method either invokes <see cref="handleInsert"/> on any cluster node
     * or invokes <see cref="Local_Insert"/> locally. The choice of the server
     * node is determined by the
     * <see cref="LoadBalancer"/>. <see cref="Local_Insert"/> triggers either
     * <see cref="OnItemAdded"/> or <see cref="OnItemUpdated"/>, which in turn
     * trigger either an item-added or item-updated cluster-wide notification.
     *
     */
    @Override
    public CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PrtCache.Insert", "");
        }

        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);
        Address targetNode = null;

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        String taskId = null;
        if (cacheEntry.getFlag() != null && cacheEntry.getFlag().IsBitSet((byte) BitSetConstants.WriteBehind)) {
            taskId = taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }

        CacheInsResultWithEntry result = new CacheInsResultWithEntry();

 
        tangible.RefObject<Address> tempRef_targetNode = new tangible.RefObject<Address>(targetNode);
        result = Safe_Clustered_Insert(key, cacheEntry, tempRef_targetNode, taskId, lockId, version, accessType, operationContext);
        targetNode = tempRef_targetNode.argvalue;

        return result;
    }

    private java.util.HashMap OptimizedInsert(Object[] keys, CacheEntry[] cacheEntries, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PoRCache.InsertBlk", "");
        }

        java.util.HashMap result = new java.util.HashMap();

        java.util.HashMap addedKeys = new java.util.HashMap();
        java.util.HashMap insertedKeys = new java.util.HashMap();
        java.util.ArrayList remainingKeys = new java.util.ArrayList();

        java.util.ArrayList totalKeys = new java.util.ArrayList(Arrays.asList(keys));
        java.util.ArrayList totalEntries = new java.util.ArrayList(Arrays.asList(cacheEntries));

        java.util.HashMap depResult = new java.util.HashMap();

        java.util.HashMap totalDepKeys = new java.util.HashMap();
        java.util.HashMap oldDepKeys = new java.util.HashMap();
        java.util.HashMap tmpResult = new java.util.HashMap();

        java.util.ArrayList goodKeysList = new java.util.ArrayList();
        java.util.ArrayList goodEntriesList = new java.util.ArrayList();
     
        try {
            tmpResult = Local_Insert(keys, cacheEntries, getCluster().getLocalAddress(), taskId, notify, operationContext);
        } catch (Exception ex) {
            tmpResult = new java.util.HashMap();
            for (int i = 0; i < keys.length; i++) {
                tmpResult.put(keys[i], new OperationFailedException(ex.getMessage(), ex));
            }
        }

        if (tmpResult != null && tmpResult.size() > 0) {
            Iterator ie = tmpResult.entrySet().iterator();
            while (ie.hasNext()) {
                Map.Entry pair = (Map.Entry) ie.next();
                if (pair.getValue() instanceof StateTransferException) {
                    remainingKeys.add(pair.getKey());
                } else {
                    if (pair.getValue() instanceof Exception) {
                        result.put(pair.getKey(), pair.getValue());
                    } else if (pair.getValue() instanceof CacheInsResultWithEntry) {
                        CacheInsResultWithEntry res = (CacheInsResultWithEntry) ((pair.getValue() instanceof CacheInsResultWithEntry) ? pair.getValue() : null);
                        switch (res.getResult()) {
                            case Failure:
                                result.put(pair.getKey(), new OperationFailedException("Generic operation failure; not enough information is available."));
                                break;
                            case NeedsEviction:
                                result.put(pair.getKey(), new OperationFailedException("The cache is full and not enough items could be evicted."));
                                break;
                            case IncompatibleGroup:
                                result.put(pair.getKey(), new OperationFailedException("Data group of the inserted item does not match the existing item's data group"));
                                break;
                            case Success:
                                addedKeys.put(pair.getKey(), null);
                                int index = totalKeys.indexOf(pair.getKey());
                                
                                break;
                            case SuccessOverwrite:
                                insertedKeys.put(pair.getKey(), pair.getValue());
                                index = totalKeys.indexOf(pair.getKey());
                                if (index != -1) {
                                   
                                    result.put(pair.getKey(), pair.getValue());
                                }
                                break;
                        }
                    }
                }
            }
        }

        if (remainingKeys.size() > 0) {
            Object[] currentKeys = new Object[remainingKeys.size()];
            CacheEntry[] currentValues = new CacheEntry[remainingKeys.size()];

            int j = 0;
            for (Iterator it = remainingKeys.iterator(); it.hasNext();) {
                Object key = it.next();
                int index = totalKeys.indexOf(key);
                if (index != -1) {
                    currentKeys[j] = totalKeys.get(index);
                    currentValues[j] = (CacheEntry) ((totalEntries.get(index) instanceof CacheEntry) ? totalEntries.get(index) : null);
                    j++;
                }
            }

            tmpResult = ClusteredInsert(currentKeys, currentValues, taskId, notify, operationContext);
            Iterator ide = tmpResult.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                result.put(pair.getKey(), pair.getValue());
            }
        }

  


        return result;
    }

    private java.util.HashMap ClusteredInsert(Object[] keys, CacheEntry[] cacheEntries, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        java.util.HashMap targetNodes = null;
        java.util.HashMap result = new java.util.HashMap();
        java.util.HashMap tmpResult = null;

        java.util.ArrayList totalKeys = new java.util.ArrayList(Arrays.asList(keys));
        java.util.ArrayList keysToInsert = new java.util.ArrayList(Arrays.asList(keys));
        java.util.ArrayList totalEntries = new java.util.ArrayList(Arrays.asList(cacheEntries));

        java.util.HashMap failedTbl = new java.util.HashMap();

        Address targetNode = null;
        Object[] currentKeys = null;
        CacheEntry[] currentValues = null;

        java.util.ArrayList goodKeysList = new java.util.ArrayList();
        java.util.ArrayList goodEntriesList = new java.util.ArrayList();
        java.util.HashMap<Object, CacheEntry> fullEntrySet = new java.util.HashMap<Object, CacheEntry>();
        java.util.HashMap depResult = new java.util.HashMap();
        java.util.HashMap totalDepKeys = new java.util.HashMap();
        java.util.HashMap oldDepKeys = new java.util.HashMap();

        java.util.HashMap totalAddedKeys = new java.util.HashMap();
        java.util.HashMap totalInsertedKeys = new java.util.HashMap();
        java.util.HashMap totalRemainingKeys = new java.util.HashMap();

        String group = cacheEntries[0].getGroupInfo() == null ? null : cacheEntries[0].getGroupInfo().getGroup();

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        do {
            targetNodes = GetTargetNodes(keysToInsert, group);

            if (targetNodes != null && targetNodes.isEmpty()) {
                for (Object key : keysToInsert) {
                    result.put(key, new OperationFailedException("No target node available to accommodate the data."));
                }
                return result;
            }
            Iterator ide = targetNodes.entrySet().iterator();
            java.util.HashMap keyList = null;

            //We select one node at a time for Add operation.
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                targetNode = (Address) ((pair.getKey() instanceof Address) ? pair.getKey() : null);
                keyList = (java.util.HashMap) pair.getValue();

                if (targetNode != null && keyList != null) {
                    currentKeys = new Object[keyList.size()];
                    currentValues = new CacheEntry[keyList.size()];

                    int j = 0;
                    for (Object key : keyList.keySet()) {
                        int index = totalKeys.indexOf(key);
                        if (index != -1) {
                            currentKeys[j] = totalKeys.get(index);
                            currentValues[j] = (CacheEntry) ((totalEntries.get(index) instanceof CacheEntry) ? totalEntries.get(index) : null);
                            if (!fullEntrySet.containsKey((String) totalKeys.get(index))) {
                                fullEntrySet.put((String) totalKeys.get(index), (CacheEntry) totalEntries.get(index));
                            }

                            j++;
                        }
                    }

                    goodKeysList.clear();
                    goodEntriesList.clear();

                    

                   
                    try {
                        if (targetNode.equals(getCluster().getLocalAddress())) {
                            tmpResult = Local_Insert(keys, cacheEntries, getCluster().getLocalAddress(), taskId, notify, operationContext);
                        } else {
                            tmpResult = Clustered_Insert(targetNode, keys, cacheEntries, taskId, operationContext);
                        }
                    } catch (SuspectedException se) {
                        //we redo the operation
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PartitionedServerCache.SafeAdd", targetNode + " left while addition");
                        }

                        tmpResult = new java.util.HashMap();
                        for (int i = 0; i < keys.length; i++) {
                            tmpResult.put(keys[i], new GeneralFailureException(se.getMessage(), se));
                        }
                    } catch (TimeoutException te) {
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PartitionedServerCache.SafeAdd", targetNode + " operation timed out");
                        }

                        tmpResult = new java.util.HashMap();
                        for (int i = 0; i < keys.length; i++) {
                            tmpResult.put(keys[i], new GeneralFailureException(te.getMessage(), te));
                        }
                    } catch (BucketTransferredException ex) {
                        tmpResult = new java.util.HashMap();
                        for (int i = 0; i < keys.length; i++) {
                            tmpResult.put(keys[i], new OperationFailedException(ex.getMessage(), ex));
                        }
                    }

                    if (tmpResult != null && tmpResult.size() > 0) {
                        Iterator ie = tmpResult.entrySet().iterator();
                        while (ie.hasNext()) {
                            Map.Entry iePair = (Map.Entry) ie.next();
                            if (iePair.getValue() instanceof StateTransferException) {
                                totalRemainingKeys.put(iePair.getKey(), null);
                            } else {
                                if (iePair.getValue() instanceof Exception) {
                                    result.put(iePair.getKey(), iePair.getValue());
                                } else if (iePair.getValue() instanceof CacheInsResultWithEntry) {
                                    CacheInsResultWithEntry res = (CacheInsResultWithEntry) ((iePair.getValue() instanceof CacheInsResultWithEntry) ? iePair.getValue() : null);
                                    switch (res.getResult()) {
                                        case Failure:
                                            result.put(iePair.getKey(), new OperationFailedException("Generic operation failure; not enough information is available."));
                                            break;
                                        case NeedsEviction:
                                            result.put(iePair.getKey(), new OperationFailedException("The cache is full and not enough items could be evicted."));
                                            break;
                                        case IncompatibleGroup:
                                            result.put(iePair.getKey(), new OperationFailedException("Data group of the inserted item does not match the existing item's data group"));
                                            break;
                                        case Success:
                                            totalAddedKeys.put(iePair.getKey(), null);
                                            int index = totalKeys.indexOf(iePair.getKey());
                                            
                                            break;
                                        case SuccessOverwrite:
                                            totalInsertedKeys.put(iePair.getKey(), iePair.getValue());
                                            index = totalKeys.indexOf(iePair.getKey());
                                            if (index != -1) {                                               
                                                result.put(iePair.getKey(), iePair.getValue());
                                            }
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            keysToInsert = new java.util.ArrayList(totalRemainingKeys.keySet());
            totalRemainingKeys.clear();
        } while (keysToInsert.size() > 0);

        if (totalAddedKeys.size() > 0) {
            java.util.Iterator ie = totalAddedKeys.keySet().iterator();
            while (ie.hasNext()) {
                Object key = ie.next();
                if (notify) {
                    // If everything went ok!, initiate local and cluster-wide notifications.
                    EventContext eventContext = CreateEventContextForGeneralDataEvent(com.alachisoft.tayzgrid.persistence.EventType.ITEM_ADDED_EVENT, operationContext, fullEntrySet.get(key), null);
                    RaiseItemAddNotifier(key, fullEntrySet.get(key), operationContext, eventContext);
                    handleNotifyAdd(new Object[]{key, operationContext, eventContext});
                }
                        
            }
        }

        if (totalInsertedKeys.size() > 0) {
            Iterator ide = totalInsertedKeys.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                Object key = pair.getKey();
                CacheInsResultWithEntry insResult = (CacheInsResultWithEntry) ((pair.getValue() instanceof CacheInsResultWithEntry) ? pair.getValue() : null);
                if (notify) {
                    CacheEntry currentEntry = fullEntrySet.get(key);
                    Object value = insResult.getEntry().getValue();
                    if (value instanceof CallbackEntry) {

                        RaiseCustomUpdateCalbackNotifier(key, currentEntry, insResult.getEntry(), operationContext);
                    }
                    EventContext eventContext = CreateEventContextForGeneralDataEvent(com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_EVENT, operationContext, currentEntry, insResult.getEntry());
                    RaiseItemUpdateNotifier(key, operationContext, eventContext);
                    handleNotifyUpdate(new Object[]{key, operationContext, eventContext});

                }
                
               
            }

           
        }

   

        return result;
    }

    /**
     * Adds key and value pairs to the cache. If any of the specified key
     * already exists in the cache; it is updated, otherwise a new item is added
     * to the cache.
     *
     * @param keys keys of the entry.
     * @param cacheEntries the cache entries.
     * @return IDictionary of failed items. Keys are the keys of items and
     * values are failure reasons usually exceptions
     *
     * This method either invokes <see cref="handleInsert"/> on any cluster node
     * or invokes <see cref="Local_Insert"/> locally. The choice of the server
     * node is determined by the
     * <see cref="LoadBalancer"/>. <see cref="Local_Insert"/> triggers either
     * <see cref="OnItemAdded"/> or <see cref="OnItemUpdated"/>, which in turn
     * trigger either an item-added or item-updated cluster-wide notification.
     *
     */
    @Override
    public java.util.HashMap Insert(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PoRCache.InsertBlk", "");
        }
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap result = null;
        String taskId = null;

        if (cacheEntries[0].getFlag() != null && cacheEntries[0].getFlag().IsBitSet((byte) BitSetConstants.WriteBehind)) {
            taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }

        long clientLastViewId = GetClientLastViewId(operationContext);

        if (clientLastViewId == getCluster().getLastViewID() && !IsInStateTransfer()) {
            result = OptimizedInsert(keys, cacheEntries, taskId, notify, operationContext);
        } else {
            result = ClusteredInsert(keys, cacheEntries, taskId, notify, operationContext);
        }

        return result;
    }

    private CacheInsResultWithEntry Local_Insert(Object key, CacheEntry cacheEntry, Address src, String taskId, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        CacheInsResultWithEntry retVal = new CacheInsResultWithEntry();

        CacheEntry clone = null;
        if (taskId != null && cacheEntry.getHasQueryInfo()) {
            clone = (CacheEntry) cacheEntry.clone();
        } else {
            clone = cacheEntry;
        }

        if (_internalCache != null) {
            retVal = _internalCache.Insert(key, cacheEntry, notify, lockId, version, accessType, operationContext);

            if (taskId != null && (retVal.getResult() == CacheInsResult.Success || retVal.getResult() == CacheInsResult.SuccessOverwrite)) {
                super.AddWriteBehindTask(src, key, clone, taskId, OpCode.Update, WriteBehindAsyncProcessor.TaskState.Execute, operationContext);
            }
        }
        return retVal;
    }

    /**
     * Insert the objects to the local cache.
     *
     * @param keys keys of the entries.
     * @return cache entries.
     */
    private java.util.HashMap Local_Insert(Object[] keys, CacheEntry[] cacheEntries, Address src, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        java.util.HashMap retVal = new java.util.HashMap();

        CacheEntry[] clone = null;
        if (taskId != null) {
            clone = new CacheEntry[cacheEntries.length];
            for (int i = 0; i < cacheEntries.length; i++) {
                if (cacheEntries[i].getHasQueryInfo()) {
                    clone[i] = (CacheEntry) cacheEntries[i].clone();
                } else {
                    clone[i] = cacheEntries[i];
                }
            }
        }

        if (_internalCache != null) {
            retVal = _internalCache.Insert(keys, cacheEntries, notify, operationContext);

            if (taskId != null && retVal.size() > 0) {
                java.util.HashMap writeBehindTable = new java.util.HashMap();
                for (int i = 0; i < keys.length; i++) {
                    CacheInsResultWithEntry value = (CacheInsResultWithEntry) ((retVal.get(keys[i]) instanceof CacheInsResultWithEntry) ? retVal.get(keys[i]) : null);
                    if (value != null && (value.getResult() == CacheInsResult.Success || value.getResult() == CacheInsResult.SuccessOverwrite)) {
                        writeBehindTable.put(keys[i], clone[i]);
                    }
                }
                if (writeBehindTable.size() > 0) {
                    super.AddWriteBehindTask(src, writeBehindTable, null, taskId, OpCode.Update, WriteBehindAsyncProcessor.TaskState.Execute);
                }
            }
        }
        return retVal;
    }

    private CacheInsResultWithEntry Safe_Clustered_Insert(Object key, CacheEntry cacheEntry, tangible.RefObject<Address> targetNode, String taskId, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        boolean suspectedErrorOccured = false;
        int maxTries = _stats.getNodes().size() > 3 ? 3 : _stats.getNodes().size() - 1;
        CacheInsResultWithEntry retVal = new CacheInsResultWithEntry();

        String group = cacheEntry.getGroupInfo() == null ? null : cacheEntry.getGroupInfo().getGroup();
        targetNode.argvalue = null;
        do {
            try {
                targetNode.argvalue = GetNextNode(key, group);

                if (targetNode.argvalue == null) {
                    throw new OperationFailedException("No target node available to accommodate the data.");
                }

                if (targetNode.argvalue.compareTo(getLocalAddress()) == 0) {
                    retVal = Local_Insert(key, cacheEntry, getCluster().getLocalAddress(), taskId, true, lockId, version, accessType, operationContext);
                } else {
                    retVal = Clustered_Insert(targetNode.argvalue, key, cacheEntry, taskId, lockId, version, accessType, operationContext);
                }

                break;
            } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
                _distributionMgr.Wait(key, group);
            } catch (TimeoutException te) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedCache.Safe_Clustered_Insert()", te.toString());
                }
                if (suspectedErrorOccured) {
                    suspectedErrorOccured = false;
                    continue;
                } else {
                    throw te;
                }
            } catch (SuspectedException e) {
                suspectedErrorOccured = true;
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedCache.Safe_Clustered_Insert()", e.toString());
                }
                if (maxTries == 0) {
                    throw e;
                }
                maxTries--;
            }
        } while (maxTries > 0);
        return retVal;
    }

    /**
     * Adds a pair of key and value to the cache. If the specified key already
     * exists in the cache; it is updated, otherwise a new item is added to the
     * cache.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     *
     * This method triggers either <see cref="OnItemAdded"/> or <see
     * cref="OnItemUpdated"/>, which in turn trigger either an item-added or
     * item-updated cluster-wide notification.
     *
     */
    private Object handleInsert(Address src, Object info, Object[] userPayload) throws OperationFailedException, CacheException {
        try {
            OperationContext operationContext = null;
            Object[] objs = (Object[]) info;

            String taskId = null;
            if (objs.length > 2) {
                taskId = (String) ((objs[2] instanceof String) ? objs[2] : null);
            }

            if (objs.length > 4) {
                operationContext = (OperationContext) ((objs[6] instanceof OperationContext) ? objs[6] : null);
            } else {
                operationContext = (OperationContext) ((objs[3] instanceof OperationContext) ? objs[3] : null);
            }

            if (objs[0] instanceof Object[]) {
                Object[] keyArr = (Object[]) objs[0];
                CacheEntry[] valArr = (CacheEntry[]) objs[1];
                return Local_Insert(keyArr, valArr, src, taskId, true, operationContext);
            } else {
                Object key = objs[0];
                CacheEntry e = (CacheEntry) ((objs[1] instanceof CacheEntry) ? objs[1] : null);
                e.setValue(userPayload);
                Object lockId = objs[3];
                LockAccessType accessType = (LockAccessType) objs[4];
                long version = (Long) objs[5];
                CacheInsResultWithEntry retVal = Local_Insert(key, e, src, taskId, true, lockId, version, accessType, operationContext);
                /*
                 * send value and entry seperaty
                 */
                OperationResponse opRes = new OperationResponse();
                if (retVal.getEntry() != null) {
                    opRes.UserPayload = null; //ubObject.Data;

                   
                    CacheEntry tempVar = retVal.getEntry().CloneWithoutValue();
                    {
                        retVal.setEntry((CacheEntry) ((tempVar instanceof CacheEntry) ? tempVar : null));
                    }
                    

                }
                opRes.SerializablePayload = retVal;
                return opRes;
                //return retVal;
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }

        return null;
    }

    @Override
    public Object RemoveSync(Object[] keys, ItemRemoveReason reason, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException {
        java.util.ArrayList depenedentItemList = new java.util.ArrayList();
        try {

            java.util.HashMap totalRemovedItems = new java.util.HashMap();

            CacheEntry entry = null;
            Iterator ide = null;

            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("PartitionedCache.RemoveSync", "Keys = " + (Integer) keys.length);
            }

            for (int i = 0; i < keys.length; i++) {
                try {
                    if (keys[i] != null) {
                        entry = Local_Remove(keys[i], reason, null, null, null, null, false, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                    }

                    if (entry != null) {
                        totalRemovedItems.put(keys[i], entry);
                        
                    }
                } catch (Exception ex) {
                    throw ex;
                }
            }

            java.util.ArrayList keysOfRemoveNotification = new java.util.ArrayList();
            java.util.ArrayList entriesOfRemoveNotification = new java.util.ArrayList();
            java.util.ArrayList<EventContext> eventContexts = new java.util.ArrayList<EventContext>();
            int sizeThreshhold = 30 * 1024;
            int countThreshhold = 50;
            int size = 0;

            ide = totalRemovedItems.entrySet().iterator();

            while (ide.hasNext()) {
                try {
                    Map.Entry pair = (Map.Entry) ide.next();
                    entry = (CacheEntry) ((pair.getValue() instanceof CacheEntry) ? pair.getValue() : null);
                    if (entry != null) {
                        if (getIsItemRemoveNotifier()) {
                            EventId eventId = null;
                            OperationID opId = operationContext.getOperatoinID();
                            EventContext eventContext = null;

                            //generate event id
                            if (!operationContext.Contains(OperationContextFieldName.EventContext)) //for atomic operations
                            {
                                eventId = EventId.CreateEventId(opId);
                            } else //for bulk
                            {
                                eventId = ((EventContext) operationContext.GetValueByField(OperationContextFieldName.EventContext)).getEventID();
                            }

                            eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_EVENT);
                            eventContext = new EventContext();
                            eventContext.Add(EventContextFieldName.EventID, eventId);
                            eventContext.setItem(CacheHelper.CreateCacheEventEntry(com.alachisoft.tayzgrid.runtime.events.EventDataFilter.DataWithMetaData, entry));

                            size += entry.getSize();
                            keysOfRemoveNotification.add(pair.getKey());
                            eventContexts.add(eventContext);

                            if (size > sizeThreshhold || keysOfRemoveNotification.size() > countThreshhold) {
                                RaiseAsyncItemRemoveNotifier(keysOfRemoveNotification.toArray(new Object[0]), null, reason, operationContext, eventContexts.toArray(new EventContext[0]));
                                keysOfRemoveNotification.clear();
                                entriesOfRemoveNotification.clear();
                                eventContexts.clear();
                                size = 0;
                            }
                            NotifyItemRemoved(pair.getKey(), entry, reason, true, operationContext, eventContext);
                        }
                        if (entry.getValue() instanceof CallbackEntry) {
                            EventId eventId = null;
                            OperationID opId = operationContext.getOperatoinID();
                            CallbackEntry cbEtnry = (CallbackEntry) entry.getValue(); // e.DeflattedValue(_context.SerializationContext);
                            EventContext eventContext = null;

                            if (cbEtnry != null && cbEtnry.getItemRemoveCallbackListener() != null && cbEtnry.getItemRemoveCallbackListener().size() > 0) {
                                //generate event id
                                if (!operationContext.Contains(OperationContextFieldName.EventContext)) //for atomic operations
                                {
                                    eventId = EventId.CreateEventId(opId);
                                } else //for bulk
                                {
                                    eventId = ((EventContext) operationContext.GetValueByField(OperationContextFieldName.EventContext)).getEventID();
                                }

                                eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_CALLBACK);
                                eventContext = new EventContext();
                                eventContext.Add(EventContextFieldName.EventID, eventId);
                                EventCacheEntry eventCacheEntry = CacheHelper.CreateCacheEventEntry(cbEtnry.getItemRemoveCallbackListener(), entry);
                                eventContext.setItem(eventCacheEntry);
                                eventContext.Add(EventContextFieldName.ItemRemoveCallbackList, new ArrayList(cbEtnry.getItemRemoveCallbackListener()));

                                RaiseAsyncCustomRemoveCalbackNotifier(pair.getKey(), entry, reason, operationContext, eventContext);
                            }
                        }
                    }
                } catch (Exception ex) {
                    getCacheLog().Error("PartitionedCache.RemoveSync", "an error occured while raising events. Error :" + ex.toString());
                }
            }

            if (keysOfRemoveNotification.size() > 0) {
                RaiseAsyncItemRemoveNotifier(keysOfRemoveNotification.toArray(new Object[0]), null, reason, operationContext, eventContexts.toArray(new EventContext[0]));
            }
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }

        return depenedentItemList;
    }

    /**
     * Removes the object and key pair from the cache. The key is specified as
     * parameter.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method invokes <see cref="handleRemove"/> on every server node in
     * the cluster. In a partition only one node can remove an item (due to
     * partitioning of data). Therefore the <see cref="OnItemsRemoved"/> handler
     * of the node actually removing the item is responsible for triggering a
     * cluster-wide Item removed notification.
     * <p>
     * <b>Note:</b>
     * Evictions and Expirations are also handled through the <see
     * cref="OnItemsRemoved"/> handler. </p>
     *
     */
    @Override
    public CacheEntry Remove(Object key, String group, ItemRemoveReason ir, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PrtCache.Remove", "");
        }

        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);
        boolean suspectedErrorOccured = false;
        Address targetNode = null;
        CacheEntry entry = null;

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        Object actualKey = key;
        DataSourceUpdateOptions updateOptions = DataSourceUpdateOptions.None;
        CallbackEntry cbEntry = null;
        String providerName = null;

        if (key instanceof Object[]) {
            Object[] package_Renamed = (Object[]) ((key instanceof Object[]) ? key : null);
            actualKey = package_Renamed[0];
            updateOptions = (DataSourceUpdateOptions) package_Renamed[1];
            cbEntry = (CallbackEntry) ((package_Renamed[2] instanceof CallbackEntry) ? package_Renamed[2] : null);
            if (package_Renamed.length > 3) {
                providerName = (String) ((package_Renamed[3] instanceof String) ? package_Renamed[3] : null);
            }
        }

        String taskId = null;
        if (updateOptions == DataSourceUpdateOptions.WriteBehind) {
            taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }

        while (true) {
            try {
                targetNode = GetNextNode(actualKey, group);
                if (targetNode != null) {
                    if (targetNode.compareTo(getLocalAddress()) == 0) {
                        entry = Local_Remove(actualKey, ir, getCluster().getLocalAddress(), cbEntry, taskId, providerName, notify, lockId, version, accessType, operationContext);
                    } else {
                        entry = Clustered_Remove(targetNode, actualKey, ir, cbEntry, taskId, providerName, notify, lockId, version, accessType, operationContext);
                    }

                }
                break;

            } catch (SuspectedException se) {
                suspectedErrorOccured = true;
                //we redo the operation
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.Remove", targetNode + " left while addition. Error: " + se.toString());
                }
                continue;
            } catch (TimeoutException te) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.Remove", targetNode + " operation timed out. Error: " + te.toString());
                }
                if (suspectedErrorOccured) {
                    suspectedErrorOccured = false;
                    continue;
                } else {
                    throw new GeneralFailureException(te.getMessage(), te);
                }
            } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
                _distributionMgr.Wait(actualKey, group);
            }
        }
        if (notify && entry != null) {
            Object value = entry.getValue();

            if (value instanceof CallbackEntry) {
                RaiseCustomRemoveCalbackNotifier(actualKey, entry, ir);
            }
        }

     

        return entry;
    }

    private java.util.HashMap OptimizedRemove(Object[] keys, String group, ItemRemoveReason ir, String taskId, String providerName, CallbackEntry cbEntry, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PoRCache.RemoveBlk", "");
        }

        java.util.HashMap result = new java.util.HashMap();
        java.util.HashMap totalDepKeys = new java.util.HashMap();

        java.util.ArrayList remainingKeys = new java.util.ArrayList();

        try {
            result = Local_Remove(keys, ir, getCluster().getLocalAddress(), cbEntry, taskId, providerName, notify, operationContext);
        } catch (Exception ex) {
            for (int i = 0; i < keys.length; i++) {
                result.put(keys[i], new OperationFailedException(ex.getMessage(), ex));
            }
        }

        if (result != null) {
            Iterator ie = result.entrySet().iterator();
            while (ie.hasNext()) {
                Map.Entry pair = (Map.Entry) ie.next();
                if (pair.getValue() instanceof StateTransferException) {
                    remainingKeys.add(pair.getKey());
                }
            }
        }

        if (result.size() > 0) {
            Iterator ide = result.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                Object key = pair.getKey();
                CacheEntry entry = (CacheEntry) ((pair.getValue() instanceof CacheEntry) ? pair.getValue() : null);
                if (entry != null) {
                    if (notify) {

                        Object value = entry.getValue();
                        if (value instanceof CallbackEntry) {
                            RaiseCustomRemoveCalbackNotifier(key, entry, ir);
                        }

                        if (!notify) {
                            EventContext eventContext = CreateEventContextForGeneralDataEvent(com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_EVENT, operationContext, entry, null);
                            Object data = new Object[]{key, ir, operationContext, eventContext};
                            RaiseItemRemoveNotifier(data);
                            handleNotifyRemoval(data);
                        }

                    }
                 
                }
            }

           
        }

        if (remainingKeys.size() > 0) {
            java.util.HashMap tmpResult = ClusteredRemove(remainingKeys.toArray(new Object[0]), group, ir, taskId, providerName, cbEntry, notify, operationContext);
            Iterator ide = tmpResult.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry entry = (Map.Entry) ide.next();
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    private java.util.HashMap ClusteredRemove(Object[] keys, String group, ItemRemoveReason ir, String taskId, String providerName, CallbackEntry cbEntry, boolean notify, OperationContext operationContext) throws GeneralFailureException, CacheException, OperationFailedException, LockingException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PoRCache.RemoveBlk", "");
        }

        java.util.HashMap targetNodes = null;
        java.util.HashMap result = new java.util.HashMap();
        java.util.HashMap tmpResult = null;

        java.util.ArrayList totalKeys = new java.util.ArrayList(Arrays.asList(keys));
        java.util.ArrayList totalRemainingKeys = new java.util.ArrayList();

        java.util.HashMap totalDepKeys = new java.util.HashMap();

        Address targetNode = null;

        do {
            targetNodes = GetTargetNodes(totalKeys, group);
            if (targetNodes != null && targetNodes.isEmpty()) {
                for (Object key : totalKeys) {
                    result.put(key, new OperationFailedException("No target node available to accommodate the data."));
                }
                return result;
            }

            Iterator ide = targetNodes.entrySet().iterator();
            java.util.HashMap keyList = null;

            //We select one node at a time for Add operation.
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                targetNode = (Address) ((pair.getKey() instanceof Address) ? pair.getKey() : null);
                keyList = (java.util.HashMap) pair.getValue();

                if (targetNode != null && keyList != null) {
                    Object[] currentKeys = MiscUtil.GetArrayFromCollection(keyList.keySet());
                    try {
                        if (targetNode.equals(getCluster().getLocalAddress())) {
                            tmpResult = Local_Remove(currentKeys, ir, getCluster().getLocalAddress(), cbEntry, taskId, providerName, notify, operationContext);
                        } else {
                            tmpResult = Clustered_Remove(targetNode, currentKeys, ir, cbEntry, taskId, providerName, notify, operationContext);
                        }
                    } catch (SuspectedException se) {
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PartitionedServerCache.Remove()", targetNode + " left while addition");
                        }
                        totalRemainingKeys.addAll(Arrays.asList(currentKeys));
                        continue;
                    } catch (TimeoutException te) {
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PartitionedServerCache.Remove()", targetNode + " operation timed out");
                        }
                        totalRemainingKeys.addAll(Arrays.asList(currentKeys));
                        continue;
                    }

                    if (tmpResult != null) {
                        Iterator ie = tmpResult.entrySet().iterator();
                        while (ie.hasNext()) {
                            Map.Entry iePair = (Map.Entry) ie.next();
                            if (iePair.getValue() instanceof StateTransferException) {
                                totalRemainingKeys.add(iePair.getKey());
                            } else {
                                if (iePair.getValue() instanceof CacheEntry) {
                                    result.put(iePair.getKey(), iePair.getValue());
                                }
                            }
                        }
                    }
                }
            }

            totalKeys = new java.util.ArrayList(totalRemainingKeys);
            totalRemainingKeys.clear();
        } while (totalKeys.size() > 0);

        if (result.size() > 0) {
            Iterator id = result.entrySet().iterator();
            while (id.hasNext()) {
                Map.Entry pair = (Map.Entry) id.next();
                Object key = pair.getKey();
                CacheEntry entry = (CacheEntry) pair.getValue();
                if (notify) {
                    Object value = entry.getValue();
                    if (value instanceof CallbackEntry) {
                        RaiseCustomRemoveCalbackNotifier(key, entry, ir);
                    }
                }
              
            }

            
        }

        return result;
    }

    /**
     * Removes the key and value pairs from the cache. The keys are specified as
     * parameter.
     *
     * @param keys keys of the entries.
     * @return list of removed keys
     *
     * This method invokes <see cref="handleRemove"/> on every server node in
     * the cluster. In a partition only one node can remove an item (due to
     * partitioning of data). Therefore the <see cref="OnItemsRemoved"/> handler
     * of the node actually removing the item is responsible for triggering a
     * cluster-wide Item removed notification.
     * <p>
     * <b>Note:</b>
     * Evictions and Expirations are also handled through the <see
     * cref="OnItemsRemoved"/> handler. </p>
     *
     */
    @Override
    public java.util.HashMap Remove(Object[] keys, String group, ItemRemoveReason ir, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PoRCache.Remove", "");
        }
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        DataSourceUpdateOptions updateOptions = DataSourceUpdateOptions.None;
        CallbackEntry cbEntry = null;
        String providerName = null;
        HashMap result = new HashMap();

        if (keys != null && keys.length > 0) {
            if (keys[0] instanceof Object[]) {
                Object[] package_Renamed = (Object[]) ((keys[0] instanceof Object[]) ? keys[0] : null);
                updateOptions = (DataSourceUpdateOptions) package_Renamed[1];
                cbEntry = (CallbackEntry) ((package_Renamed[2] instanceof CallbackEntry) ? package_Renamed[2] : null);
                if (package_Renamed.length > 3) {
                    providerName = (String) ((package_Renamed[3] instanceof String) ? package_Renamed[3] : null);
                }
                keys[0] = package_Renamed[0];
            }

            if (_internalCache == null) {
                throw new UnsupportedOperationException();
            }

            String taskId = null;
            if (updateOptions == DataSourceUpdateOptions.WriteBehind) {
                taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
            }

            long clientLastViewId = GetClientLastViewId(operationContext);

            if (clientLastViewId == getCluster().getLastViewID() && !IsInStateTransfer()) {
                result = OptimizedRemove(keys, group, ir, taskId, providerName, cbEntry, notify, operationContext);
            } else {
                result = ClusteredRemove(keys, group, ir, taskId, providerName, cbEntry, notify, operationContext);
            }
        }
        return result;
    }

    /**
     * Remove the object from the local cache only.
     *
     * @param key key of the entry.
     * @return cache entry.
     */
    private CacheEntry Local_Remove(Object key, ItemRemoveReason ir, Address src, CallbackEntry cbEntry, String taskId, String providerName, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, OperationFailedException {
        CacheEntry retVal = null;
        if (_internalCache != null) {
            retVal = _internalCache.Remove(key, ir, notify, lockId, version, accessType, operationContext);
            if (taskId != null && retVal != null) {
                CacheEntry cloned = retVal;
                Object tempVar = retVal.clone();
                cloned = (CacheEntry) ((tempVar instanceof CacheEntry) ? tempVar : null);
                cloned.setProviderName(providerName);
                if (cbEntry != null) {
                    if (cloned.getValue() instanceof CallbackEntry) {
                        ((CallbackEntry) cloned.getValue()).setWriteBehindOperationCompletedCallback(cbEntry.getWriteBehindOperationCompletedCallback());
                    } else {
                        cbEntry.setValue(cloned.getValue());
                        cloned.setValue(cbEntry);
                    }
                }
                super.AddWriteBehindTask(src, key, cloned, taskId, OpCode.Remove, WriteBehindAsyncProcessor.TaskState.Execute, operationContext);
            }
        }
        return retVal;
    }

    /**
     * Remove the objects from the local cache only.
     *
     * @param keys keys of the entries.
     * @return list of removed keys.
     */
    private java.util.HashMap Local_Remove(Object[] keys, ItemRemoveReason ir, Address src, CallbackEntry cbEntry, String taskId, String providerName, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        java.util.HashMap removedKeys = null;

        if (_internalCache != null) {
            removedKeys = _internalCache.Remove(keys, ir, notify, operationContext);

            if (taskId != null && removedKeys != null && removedKeys.size() > 0) {
                java.util.HashMap writeBehindTable = new java.util.HashMap();
                for (int i = 0; i < keys.length; i++) {
                    CacheEntry entry = (CacheEntry) ((removedKeys.get(keys[i]) instanceof CacheEntry) ? removedKeys.get(keys[i]) : null);
                    if (entry != null) {
                        entry.setProviderName(providerName);
                        writeBehindTable.put(keys[i], entry);
                    }
                }
                if (writeBehindTable.size() > 0) {
                    super.AddWriteBehindTask(src, writeBehindTable, cbEntry, taskId, OpCode.Remove, WriteBehindAsyncProcessor.TaskState.Execute);
                }
            }

        }
        return removedKeys;
    }

    /**
     * Remove the group from cache.
     *
     * @param group group to be removed.
     * @param subGroup subGroup to be removed.
     */
    @Override
    public java.util.HashMap Remove(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        // Wait until the object enters the running status
        _statusLatch.WaitForAny(NodeStatus.Running);

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        long clientLastViewId = GetClientLastViewId(operationContext);

        if (clientLastViewId != getCluster().getLastViewID()) {
            if (this.getCluster().getIsCoordinator() || _context.getCacheRoot().getIsInProc()) {
                java.util.ArrayList list = GetGroupKeys(group, subGroup, operationContext);
                if (list != null && list.size() > 0) {
                    Object[] grpKeys = MiscUtil.GetArrayFromCollection(list);
                    return Remove(grpKeys, ItemRemoveReason.Removed, notify, operationContext);
                }
            }

            return new java.util.HashMap();
        } else {
            return Local_RemoveGroup(group, subGroup, notify, operationContext);
        }
    }

    @Override
    protected java.util.HashMap Local_RemoveGroup(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }
        java.util.ArrayList list = Local_GetGroupKeys(group, subGroup, operationContext);
        if (list != null && list.size() > 0) {
            Object[] grpKeys = MiscUtil.GetArrayFromCollection(list);
            return Remove(grpKeys, ItemRemoveReason.Removed, notify, operationContext);
        }
        return null;
    }

    @Override
    public java.util.HashMap Remove(String[] tags, TagComparisonType tagComparisonType, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        // Wait until the object enters the running status
        _statusLatch.WaitForAny(NodeStatus.Running);

        long ClientLastViewId = GetClientLastViewId(operationContext);
        if (ClientLastViewId != getCluster().getLastViewID()) {
            if (this.getCluster().getIsCoordinator() || _context.getCacheRoot().getIsInProc()) {
                java.util.ArrayList list = GetTagKeys(tags, tagComparisonType, operationContext);
                if (list != null && list.size() > 0) {
                    Object[] grpKeys = MiscUtil.GetArrayFromCollection(list);
                    return Remove(grpKeys, ItemRemoveReason.Removed, notify, operationContext);
                }
            }

            return new java.util.HashMap();
        } else {
            return Local_RemoveTag(tags, tagComparisonType, notify, operationContext);
        }
    }

    @Override
    protected java.util.HashMap Local_RemoveTag(String[] tags, TagComparisonType tagComparisonType, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }
        java.util.ArrayList list = Local_GetTagKeys(tags, tagComparisonType, operationContext);
        if (list != null && list.size() > 0) {
            Object[] grpKeys = MiscUtil.GetArrayFromCollection(list);
            return Remove(grpKeys, ItemRemoveReason.Removed, notify, operationContext);
        }
        return null;
    }

    /**
     * Remove the group from cache.
     *
     * @param group group to be removed.
     * @param subGroup subGroup to be removed.
     */
    private java.util.HashMap Local_Remove(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, StateTransferException {
        if (_internalCache != null) {
            return _internalCache.Remove(group, subGroup, notify, operationContext);
        }
        return null;
    }

    /**
     * Hanlde cluster-wide Remove(key) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     *
     * This method triggers <see cref="OnItemsRemoved"/>, which then triggers a
     * cluster-wide Item removed notification.
     *
     */
    private Object handleRemove(Address src, Object info) throws OperationFailedException {
        try {
            Object[] param = (Object[]) info;
            CallbackEntry cbEntry = null;
            String taskId = null;
            String providerName = null;
            OperationContext oc = null;

            if (param.length > 3) {
                cbEntry = (CallbackEntry) ((param[3] instanceof CallbackEntry) ? param[3] : null);
            }
            if (param.length > 4) {
                taskId = (String) ((param[4] instanceof String) ? param[4] : null);
            }
            if (param.length > 8) {
                providerName = (String) ((param[8] instanceof String) ? param[8] : null);
            }

            if (param.length == 3) {
                oc = (OperationContext) ((param[2] instanceof OperationContext) ? param[2] : null);
            }

            if (param.length == 10) {
                oc = (OperationContext) ((param[9] instanceof OperationContext) ? param[9] : null);
            }

            if (param.length == 7) {
                oc = (OperationContext) ((param[6] instanceof OperationContext) ? param[6] : null);
            }

            if (param[0] instanceof Object[]) {
                if (param.length > 5) {
                    providerName = (String) ((param[5] instanceof String) ? param[5] : null);
                }
                java.util.HashMap table = Local_Remove((Object[]) param[0], (ItemRemoveReason) param[1], src, cbEntry, taskId, providerName, (Boolean) param[2], oc);

                return table;
            } else {
                Object lockId = param[5];
                LockAccessType accessType = (LockAccessType) param[6];
                long version = (Long) param[7];
                CacheEntry e = Local_Remove(param[0], (ItemRemoveReason) param[1], src, cbEntry, taskId, providerName, (Boolean) param[2], lockId, version, accessType, oc);
                OperationResponse opRes = new OperationResponse();
                if (e != null) {
                    UserBinaryObject ubObject = (UserBinaryObject) (e.getValue() instanceof CallbackEntry ? ((CallbackEntry) e.getValue()).getValue() : e.getValue());
                    opRes.UserPayload = ubObject.getData();

                    opRes.SerializablePayload = e.CloneWithoutValue();
                }
                return opRes;
            }

            /**
             * Unlike replicated server the following is not needed here, since
             * in a partition only one node actually has the object.
             * if(IsCoordinator) return e;
             *
             */
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return null;
    }

    /**
     * Hanlde cluster-wide RemoveGroup(group, subGroup) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     *
     * This method triggers <see cref="OnItemsRemoved"/>, which then triggers a
     * cluster-wide Item removed notification.
     *
     */
    private Object handleRemoveGroup(Object info) throws OperationFailedException {
        try {
            Object[] param = (Object[]) info;
            String group = (String) ((param[0] instanceof String) ? param[0] : null);
            String subGroup = (String) ((param[1] instanceof String) ? param[1] : null);
            boolean notify = (Boolean) param[2];

            OperationContext operationContext = null;

            if (param.length > 3) {
                operationContext = (OperationContext) ((param[3] instanceof OperationContext) ? param[3] : null);
            }

            return Local_RemoveGroup(group, subGroup, notify, operationContext);
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }

        return null;
    }

    /**
     * Returns a .NET IEnumerator interface so that a client should be able to
     * iterate over the elements of the cache store.
     *
     * @return Iterator enumerator.
     */
    @Override
    public ResetableIterator GetEnumerator() throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        ResetableIterator localEnumerator = new LazyPartitionedKeysetEnumerator(this, (Object[]) handleKeyList(), getCluster().getLocalAddress(), true);

        if (getCluster().getServers().size() == 1) {
            return localEnumerator;
        }

        return Clustered_GetEnumerator(getCluster().getServers(), localEnumerator);
    }

    @Override
    public EnumerationDataChunk GetNextChunk(EnumerationPointer pointer, OperationContext operationContext) throws GeneralFailureException, TimeoutException, SuspectedException, CacheException {
        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        EnumerationDataChunk nextChunk = null;

        long clientLastViewId = GetClientLastViewId(operationContext);
        String intenededRecepient = GetIntendedRecipient(operationContext);
        Address[] servers = new Address[getCluster().getServers().size()];
        System.arraycopy(getCluster().getServers().toArray(), 0, servers, 0, getCluster().getServers().size());
        Address targetNode = null;

        if (clientLastViewId == -1 && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(intenededRecepient)) {
            for (int i = 0; i < servers.length; i++) {
                Object tempVar = servers[i];
                Address server = (Address) ((tempVar instanceof Address) ? tempVar : null);
                if (server.getIpAddress().getHostAddress().equals(intenededRecepient)) {
                    targetNode = server;
                    break;
                }
            }
            if (targetNode != null) {
                if (targetNode.equals(getCluster().getLocalAddress())) {
                    nextChunk = getInternalCache().GetNextChunk(pointer, operationContext);
                } else {
                    nextChunk = Clustered_GetNextChunk(targetNode, pointer, operationContext);
                }
            } else {
                nextChunk = new EnumerationDataChunk();
                nextChunk.setPointer(pointer);
            }
        } else {
            nextChunk = getInternalCache().GetNextChunk(pointer, operationContext);
        }

        return nextChunk;
    }

    /**
     * Hanlde cluster-wide KeyList requests.
     *
     * @return object to be sent back to the requestor.
     */
    private Object handleKeyList() throws OperationFailedException {
        try {
            return MiscUtil.GetKeyset(_internalCache, (int) getCluster().getTimeout());
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return null;
    }

    /**
     * Fire when the cache is cleared.
     */
    public void OnCacheCleared(OperationContext operationContext, EventContext eventContext) {
        // do local notifications only, every node does that, so we get a replicated notification.
        UpdateCacheStatistics();
        handleNotifyCacheCleared();
    }

    /**
     *
     *
     * @param notifId
     * @param data
     */
    public void OnCustomEvent(Object notifId, Object data, OperationContext operationContext, EventContext eventContext) {
    }

    /**
     * Hanlder for clustered cache clear notification.
     *
     * @param info packaged information
     * @return null
     */
    private Object handleNotifyCacheCleared() {
        NotifyCacheCleared(true, null, null);
        return null;
    }

    /**
     * Fired when an item is added to the cache.
     *
     *
     * Triggers a cluster-wide item added notification.
     *
     */
    public void OnItemAdded(Object key, OperationContext operationContext, EventContext eventContext) {
        // Handle all exceptions, do not let the effect seep thru
        try {
            FilterEventContextForGeneralDataEvents(com.alachisoft.tayzgrid.runtime.events.EventType.ItemAdded, eventContext);
            // do not broad cast if there is only one node.
            if (getIsItemAddNotifier() && getValidMembers().size() > 1) {
                Object notification = new Function(OpCodes.NotifyAdd.getValue(), new Object[]{key, operationContext, eventContext});
                RaiseGeneric(notification);
                handleNotifyAdd(new Object[]{key, operationContext, eventContext});
            } else {
                handleNotifyAdd(new Object[]{key, operationContext, eventContext});
            }
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("PartitionedCache.OnItemAdded()", "key: " + key.toString());
            }
        } catch (Exception e) {
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("PartitionedCache.OnItemAdded()", e.toString());
            }
        }
    }

    /**
     * Hanlder for clustered item added notification.
     *
     * @param info packaged information
     * @return null
     */
    private Object handleNotifyAdd(Object info) {
        Object[] args = (Object[]) ((info instanceof Object[]) ? info : null);
        NotifyItemAdded(args[0], true, (OperationContext) args[1], (EventContext) args[2]);
        return null;
    }

    /**
     * handler for item updated event.
     */
    public void OnItemUpdated(Object key, OperationContext operationContext, EventContext eventContext) {
        // Handle all exceptions, do not let the effect seep thru
        try {
            FilterEventContextForGeneralDataEvents(com.alachisoft.tayzgrid.runtime.events.EventType.ItemRemoved, eventContext);
            Object[] packedData = new Object[]{key, operationContext, eventContext};
            // do not broad cast if there is only one node.
            if (getIsItemUpdateNotifier() && getValidMembers().size() > 1) {
                Object notification = new Function(OpCodes.NotifyUpdate.getValue(), packedData);
                RaiseGeneric(notification);
                handleNotifyUpdate(packedData);
            } else {
                handleNotifyUpdate(packedData);
            }
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("PartitionedCache.OnItemUpdated()", "key: " + key.toString());
            }
        } catch (Exception e) {
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("PartitionedCache.OnItemUpdated()", "Key: " + key.toString() + " Error: " + e.toString());
            }
        }
    }

    /**
     * Hanlder for clustered item updated notification.
     *
     * @param info packaged information
     * @return null
     */
    private Object handleNotifyUpdate(Object info) {
        Object[] args = (Object[]) ((info instanceof Object[]) ? info : null);
        if (args != null) {
            OperationContext opContext = null;
            EventContext evContext = null;
            if (args.length > 1) {
                opContext = (OperationContext) ((args[1] instanceof OperationContext) ? args[1] : null);
            }
            if (args.length > 2) {
                evContext = (EventContext) ((args[2] instanceof EventContext) ? args[2] : null);
            }

            NotifyItemUpdated(args[0], true, opContext, evContext);
        } else {
            NotifyItemUpdated(info, true, null, null);
        }
        return null;
    }

    /**
     * Fired when an item is removed from the cache.
     */
    public void OnItemRemoved(Object key, Object val, ItemRemoveReason reason, OperationContext operationContext, EventContext eventContext) throws OperationFailedException, CacheException, LockingException {
        ((ICacheEventsListener) this).OnItemsRemoved(new Object[]{
            key
        }, new Object[]{
            val
        }, reason, operationContext, new EventContext[]{eventContext});
    }

    /**
     * Fired when multiple items are removed from the cache.
     *
     *
     * In a partition only one node can remove an item (due to partitioning of
     * data). Therefore this handler triggers a cluster-wide Item removed
     * notification.
     *
     */
    public void OnItemsRemoved(Object[] keys, Object[] values, ItemRemoveReason reason, OperationContext operationContext, EventContext[] eventContext) {
        // Handle all exceptions, do not let the effect seep thru
        try {
            if (getIsItemRemoveNotifier() ) {
                Object notification = null;

                CacheEntry entry;
                for (int i = 0; i < keys.length; i++) {
                    FilterEventContextForGeneralDataEvents(com.alachisoft.tayzgrid.runtime.events.EventType.ItemRemoved, eventContext[i]);

                    Object data = new Object[]{
                        keys[i], reason, operationContext, eventContext[i]
                    };

                    notification = new Function(OpCodes.NotifyRemoval.getValue(), data);
                    RaiseGeneric(notification);

                }
                NotifyItemsRemoved(keys, null, reason, true, operationContext, eventContext);
            } else {
                NotifyItemsRemoved(keys, null, reason, true, operationContext, eventContext);
            }
        } catch (Exception e) {
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("ParitionedCache.OnItemsRemoved()", e.toString());
            }
        }
    }

    /**
     * Hanlder for clustered item removal notification.
     *
     * @param info packaged information
     * @return null
     */
    private Object handleNotifyRemoval(Object info) throws OperationFailedException, CacheException, StateTransferException, LockingException {
        Object[] objs = (Object[]) info;
        OperationContext operationContext = null;
        EventContext evContext = null;
        if (objs.length > 2) {
            operationContext = (OperationContext) ((objs[2] instanceof OperationContext) ? objs[2] : null);
        }
        if (objs.length > 3) {
            evContext = (EventContext) ((objs[3] instanceof EventContext) ? objs[3] : null);
        }

        NotifyItemRemoved(objs[0], null, (ItemRemoveReason) objs[1], true, operationContext, evContext);

        return null;
    }

    /**
     * Hanlder for clustered item removal notification.
     *
     * @param info packaged information
     * @return null
     */
    private Object handleNotifyBulkRemoval(Object info) throws OperationFailedException, CacheException, StateTransferException, LockingException {
        OperationContext operationContext = null;
        Object[] objs = (Object[]) info;
        Object[] keys = (Object[]) ((objs[0] instanceof Object[]) ? objs[0] : null);
        Object[] values = (Object[]) ((objs[1] instanceof Object[]) ? objs[1] : null);
        ItemRemoveReason reason = (ItemRemoveReason) objs[2];
        EventContext[] eventContexts = null;
        if (objs.length > 3) {
            operationContext = (OperationContext) ((objs[3] instanceof OperationContext) ? objs[3] : null);
            eventContexts = (EventContext[]) ((objs[4] instanceof EventContext[]) ? objs[4] : null);
        }

        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                NotifyItemRemoved(keys[i], null, reason, true, operationContext, eventContexts[i]);
            }
        }

        return null;
    }

    /**
     * handler for item update callback event.
     */
    public void OnCustomUpdateCallback(Object key, Object value, OperationContext operationContext, EventContext eventContext) {
        if (value != null) {
            RaiseCustomUpdateCalbackNotifier(key, (java.util.List) value, eventContext);
        }
    }

    /**
     * handler for item remove callback event.
     */
    public void OnCustomRemoveCallback(Object key, Object entry, ItemRemoveReason removalReason, OperationContext operationContext, EventContext eventContext) throws OperationFailedException {
        boolean notifyRemove = false;
        Object notifyRemoval = operationContext.GetValueByField(OperationContextFieldName.NotifyRemove);
        if (notifyRemoval != null) {
            notifyRemove = (Boolean) notifyRemoval;
        }
        if ((removalReason == ItemRemoveReason.Removed)  && !notifyRemove) {
            return;
        }

        if (entry != null) {
            RaiseCustomRemoveCalbackNotifier(key, (CacheEntry) entry, removalReason, operationContext, eventContext);
        }
    }

    /**
     * Fire when hasmap changes when - new node joins - node leaves -
     * manual/automatic load balance
     *
     * @param newHashmap new hashmap
     */
    public void OnHashmapChanged(NewHashmap newHashmap, boolean updateClientMap) {
    }

    /**
     *
     *
     * @param operationCode
     * @param result
     * @param cbEntry
     */
    public void OnWriteBehindOperationCompletedCallback(OpCode operationCode, Object result, CallbackEntry cbEntry) {
    }

    @Override
    public Object handleRegisterKeyNotification(Object operand) throws OperationFailedException, CacheException, LockingException, GeneralFailureException, SuspectedException, TimeoutException {
        Object[] operands = (Object[]) ((operand instanceof Object[]) ? operand : null);
        if (operands != null) {
            Object Keys = operands[0];
            CallbackInfo updateCallback = (CallbackInfo) ((operands[1] instanceof CallbackInfo) ? operands[1] : null);
            CallbackInfo removeCallback = (CallbackInfo) ((operands[2] instanceof CallbackInfo) ? operands[2] : null);
            OperationContext operationContext = (OperationContext) ((operands[3] instanceof OperationContext) ? operands[3] : null);
            if (_internalCache != null) {
                if (Keys instanceof Object[]) {
                    _internalCache.RegisterKeyNotification((Object[]) Keys, updateCallback, removeCallback, operationContext);
                } else {
                    _internalCache.RegisterKeyNotification((Object) Keys, updateCallback, removeCallback, operationContext);
                }
            }
        }
        return null;
    }

    @Override
    public Object handleUnregisterKeyNotification(Object operand) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        Object[] operands = (Object[]) ((operand instanceof Object[]) ? operand : null);
        if (operands != null) {
            Object Keys = operands[0];
            CallbackInfo updateCallback = (CallbackInfo) ((operands[1] instanceof CallbackInfo) ? operands[1] : null);
            CallbackInfo removeCallback = (CallbackInfo) ((operands[2] instanceof CallbackInfo) ? operands[2] : null);
            OperationContext operationContext = (OperationContext) ((operands[3] instanceof OperationContext) ? operands[3] : null);
            if (_internalCache != null) {
                if (Keys instanceof Object[]) {
                    _internalCache.UnregisterKeyNotification((Object[]) Keys, updateCallback, removeCallback, operationContext);
                } else {
                    _internalCache.UnregisterKeyNotification((Object) Keys, updateCallback, removeCallback, operationContext);
                }
            }
        }
        return null;
    }

    @Override
    public void RegisterKeyNotification(Object key, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, SuspectedException, TimeoutException {
        Object[] obj = new Object[]{
            key, updateCallback, removeCallback, operationContext
        };
        Address targetNode = null;
        try {
            targetNode = GetNextNode(key, null);
            if (targetNode.equals(getCluster().getLocalAddress())) {
                handleRegisterKeyNotification(obj);
            } else {
                Function fun = new Function(OpCodes.RegisterKeyNotification.getValue(), obj, false);
                Object results = getCluster().SendMessage(targetNode, fun, getGetFirstResponse(), false);

            }

        } catch (com.alachisoft.tayzgrid.common.exceptions.SuspectedException se) {
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("PartServerCache.RegisterKeyNotification", targetNode + " left while Registering notification");
            }
            throw se;
        } catch (com.alachisoft.tayzgrid.common.exceptions.TimeoutException te) {
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("PartServerCache.RegisterKeyNotification", targetNode + " operation timed out");
            }
            throw te;
        } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
            throw se;
        } catch (Exception e) {
        }
    }

    @Override
    public void RegisterKeyNotification(Object[] keys, CallbackInfo updateCallback, CallbackInfo removeCallback, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, SuspectedException, TimeoutException {
        Object[] obj = null;
        HashMap targetNodes = null;
        HashMap result = new HashMap();

        ArrayList totalKeys = new ArrayList(Arrays.asList(keys));
        Address targetNode = null;
        Object[] currentKeys = null;
        targetNodes = GetTargetNodes(totalKeys, null);

        if (targetNodes != null && targetNodes.size() != 0) {
            Iterator ide = targetNodes.entrySet().iterator();
            HashMap keyList = null;
            Map.Entry entry;

            while (ide.hasNext()) {
                entry = (Map.Entry) ide.next();
                targetNode = entry.getKey() instanceof Address ? (Address) entry.getKey() : null;
                keyList = (HashMap) entry.getValue();

                if (targetNode != null && keyList != null) {
                    currentKeys = new Object[keyList.size()];
                    int j = 0;
                    for (Object key : keyList.keySet()) {
                        int index = totalKeys.indexOf(key);
                        if (index != -1) {
                            currentKeys[j] = totalKeys.get(index);
                            j++;
                        }
                    }

                    try {
                        obj = new Object[]{
                            currentKeys, updateCallback, removeCallback, operationContext
                        };
                        if (targetNode.equals(getCluster().getLocalAddress())) {
                            handleRegisterKeyNotification(obj);
                        } else {
                            Function fun = new Function(OpCodes.RegisterKeyNotification.getValue(), obj, false);
                            Object rsp = getCluster().SendMessage(targetNode, fun, getGetFirstResponse(), false);
                        }
                    } catch (com.alachisoft.tayzgrid.common.exceptions.SuspectedException se) {
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PartServerCache.RegisterKeyNotification", targetNode + " left while Registering notification");
                        }
                        throw se;
                    } catch (com.alachisoft.tayzgrid.common.exceptions.TimeoutException te) {
                        if (getContext().getCacheLog().getIsInfoEnabled()) {
                            getContext().getCacheLog().Info("PartServerCache.RegisterKeyNotification", targetNode + " operation timed out");
                        }
                        throw te;
                    } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
                        throw se;
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    @Override
    public void EmptyBucket(int bucketId) throws LockingException, StateTransferException, OperationFailedException, CacheException, IOException, ClassNotFoundException {
        getInternalCache().RemoveBucketData(bucketId);

        //Announce all the members that i am the new owner of this bucket....
        java.util.ArrayList bucketIds = new java.util.ArrayList(1);
        bucketIds.add(bucketId);
        ReleaseBuckets(bucketIds);
    }

    @Override
    public DistributionMaps GetDistributionMaps(DistributionInfoData distInfo) {
        getCacheLog().Debug("PartitionedCache.GetHashMap()", "here comes the request for hashmap");
        distInfo.setGroup(null);
        return this._distributionMgr.GetMaps(distInfo);
    }

    @Override
    public java.util.ArrayList getHashMap() {
        return _distributionMgr.getInstalledHashMap();
    }

    @Override
    public void setHashMap(java.util.ArrayList value) {
        _distributionMgr.setInstalledHashMap(value);
    }

    @Override
    public java.util.HashMap getBucketsOwnershipMap() {
        return _distributionMgr.getBucketsOwnershipMap();
    }

    @Override
    public void setBucketsOwnershipMap(java.util.HashMap value) {
        _distributionMgr.setBucketsOwnershipMap(value);
    }

    @Override
    public NewHashmap GetOwnerHashMapTable(tangible.RefObject<Integer> bucketSize) {
        java.util.ArrayList membersList = new java.util.ArrayList();
        for (Iterator it = getCluster().getRenderers().values().iterator(); it.hasNext();) {
            Address address = (Address) it.next();
            if (address.getIpAddress() != null) {
                membersList.add(address);
            }
        }

        return new NewHashmap(getCluster().getLastViewID(), _distributionMgr.GetOwnerHashMapTable(getCluster().getRenderers(), bucketSize), membersList);
    }

    @Override
    public void InstallHashMap(DistributionMaps distributionMaps, java.util.List leftMbrs) {
        _distributionMgr.InstallHashMap(distributionMaps, leftMbrs);
    }

    @Override
    protected DistributionMaps GetMaps(DistributionInfoData info) {
        return _distributionMgr.GetMaps(info);
    }

  

    @Override
    public void AutoLoadBalance() throws GeneralFailureException {
        if (_distributionMgr.getCandidateNodesForBalance().size() > 0) {
            DetermineClusterStatus();
            java.util.ArrayList candidateNodes = _distributionMgr.getCandidateNodesForBalance();
            if (candidateNodes != null && candidateNodes.size() > 0) {
                DistributionMaps maps = null;
                DistributionManager.CandidateNodeForLoadBalance candidateNode = (DistributionManager.CandidateNodeForLoadBalance) ((candidateNodes.get(0) instanceof DistributionManager.CandidateNodeForLoadBalance) ? candidateNodes.get(0) : null);
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedCache.AutoLoadBalance", "candidate node count: " + candidateNodes.size() + " candidate node :" + candidateNode.getNode()
                            + " above avg(%) :" + candidateNode.getPercentageAboveAverage());
                }
                Address tempVar = candidateNode.getNode();
                PartNodeInfo nodeInfo = new PartNodeInfo((Address) ((tempVar instanceof Address) ? tempVar : null), null, false);
                DistributionInfoData distInfo = new DistributionInfoData(DistributionMode.Manual, ClusterActivity.None, nodeInfo);
                maps = _distributionMgr.GetMaps(distInfo);

                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedCache.AutoLoadBalance", "result :" + maps.getBalancingResult());
                }

                if (maps.getBalancingResult() == BalancingResult.Default) {
                    PublishMaps(maps);
                }
            } else {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedCache.AutoLoadBalance", "No need to load balance");
                }
            }
        }
    }

    private LockOptions Local_Lock(Object key, LockExpiration lockExpiration, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        if (_internalCache != null) {
            return _internalCache.Lock(key, lockExpiration, lockId, lockDate, operationContext);
        }
        return null;
    }

    private LockOptions handleLock(Object info) throws OperationFailedException, CacheException, LockingException {
        Object[] package_Renamed = (Object[]) ((info instanceof Object[]) ? info : null);
        Object key = package_Renamed[0];
        Object lockId = package_Renamed[1];
        java.util.Date lockDate = (java.util.Date) package_Renamed[2];
        LockExpiration lockExpiration = (LockExpiration) package_Renamed[3];
        OperationContext operationContext = (OperationContext) ((package_Renamed[4] instanceof OperationContext) ? package_Renamed[4] : null);

        tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
        tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
        LockOptions tempVar = Local_Lock(key, lockExpiration, tempRef_lockId, tempRef_lockDate, operationContext);
        lockId = tempRef_lockId.argvalue;
        lockDate = tempRef_lockDate.argvalue;
        return tempVar;
    }

    @Override
    public LockOptions Lock(Object key, LockExpiration lockExpiration, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PartCache.lock", "lock_id :" + lockId.argvalue);
        }

        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        LockOptions lockInfo = null;
        Address address = null;
        boolean suspectedErrorOccured = false;

        while (true) {
            address = GetNextNode(key, null);

            if (address == null) {
                getCacheLog().Error("PartitionedServerCache.lock()", "specified key does not map to any node. return.");
                return null;
            }

            try {
                if (address.compareTo(getCluster().getLocalAddress()) == 0) {
                    lockInfo = Local_Lock(key, lockExpiration, lockId, lockDate, operationContext);
                } else {
                    lockInfo = Clustered_Lock(address, key, lockExpiration, lockId, lockDate, operationContext);
                }
                return lockInfo;
            } catch (SuspectedException se) {
                suspectedErrorOccured = true;
                //we redo the operation
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.lock", address + " left while trying to lock the key. Error: " + se.toString());
                }
                continue;
            } catch (TimeoutException te) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.lock", address + " operation timed out. Error: " + te.toString());
                }
                if (suspectedErrorOccured) {
                    suspectedErrorOccured = false;
                    continue;
                } else {
                    throw new GeneralFailureException(te.getMessage(), te);
                }
            } catch (com.alachisoft.tayzgrid.caching.exceptions.StateTransferException se) {
                _distributionMgr.Wait(key, null);
            }
        }
    }

    @Override
    public void UnLock(Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PartCache.Unlock", "lock_id :" + lockId);
        }
        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        Address address = null;
        boolean suspectedErrorOccured = false;

        while (true) {
            address = GetNextNode(key, null);

            if (address == null) {
                getCacheLog().Error("PartitionedServerCache.unlock()", "specified key does not map to any node. return.");
            }

            try {
                if (address.compareTo(getCluster().getLocalAddress()) == 0) {
                    Local_UnLock(key, lockId, isPreemptive, operationContext);
                    break;
                } else {
                    Clustered_UnLock(address, key, lockId, isPreemptive, operationContext);
                    break;
                }
            } catch (SuspectedException se) {
                suspectedErrorOccured = true;
                //we redo the operation
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.unlock", address + " left while trying to lock the key. Error: " + se.toString());
                }
                continue;
            } catch (TimeoutException te) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.unlock", address + " operation timed out. Error: " + te.toString());
                }
                if (suspectedErrorOccured) {
                    suspectedErrorOccured = false;
                    continue;
                } else {
                    throw new GeneralFailureException(te.getMessage(), te);
                }
            }
        }
    }

    private void Local_UnLock(Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        if (_internalCache != null) {
            _internalCache.UnLock(key, lockId, isPreemptive, operationContext);
        }
    }

    private void handleUnLock(Object info) throws OperationFailedException, CacheException, LockingException {
        Object[] package_Renamed = (Object[]) ((info instanceof Object[]) ? info : null);
        Object key = package_Renamed[0];
        Object lockId = package_Renamed[1];
        boolean isPreemptive = (Boolean) package_Renamed[2];
        OperationContext operationContext = (OperationContext) ((package_Renamed[3] instanceof OperationContext) ? package_Renamed[3] : null);

        Local_UnLock(key, lockId, isPreemptive, operationContext);
    }

    private LockOptions Local_IsLocked(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        if (_internalCache != null) {
            return _internalCache.IsLocked(key, lockId, lockDate, operationContext);
        }
        return null;
    }

    private LockOptions handleIsLocked(Object info) throws OperationFailedException, CacheException, LockingException {
        Object[] package_Renamed = (Object[]) ((info instanceof Object[]) ? info : null);
        Object key = package_Renamed[0];
        Object lockId = package_Renamed[1];
        java.util.Date lockDate = (java.util.Date) package_Renamed[2];
        OperationContext operationContext = null;
        if (package_Renamed.length > 3) {
            operationContext = (OperationContext) ((package_Renamed[3] instanceof OperationContext) ? package_Renamed[3] : null);
        }

        tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
        tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
        LockOptions tempVar = Local_IsLocked(key, tempRef_lockId, tempRef_lockDate, operationContext);
        lockId = tempRef_lockId.argvalue;
        lockDate = tempRef_lockDate.argvalue;
        return tempVar;
    }

    @Override
    public LockOptions IsLocked(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        /**
         * Wait until the object enters the running status
         */
        _statusLatch.WaitForAny(NodeStatus.Running);

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        LockOptions lockInfo = null;
        Address address = null;
        boolean suspectedErrorOccured = false;

        while (true) {
            address = GetNextNode(key, null);

            if (address == null) {
                getCacheLog().Error("PartitionedServerCache.lock()", "specified key does not map to any node. return.");
                return null;
            }

            try {
                if (address.compareTo(getCluster().getLocalAddress()) == 0) {
                    lockInfo = Local_IsLocked(key, lockId, lockDate, operationContext);
                } else {
                    lockInfo = Clustered_IsLocked(address, key, lockId, lockDate, operationContext);
                }
                return lockInfo;
            } catch (SuspectedException se) {
                suspectedErrorOccured = true;
                //we redo the operation
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.lock", address + " left while trying to lock the key. Error: " + se.toString());
                }
                continue;
            } catch (TimeoutException te) {
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("PartitionedServerCache.lock", address + " operation timed out. Error: " + te.toString());
                }
                if (suspectedErrorOccured) {
                    suspectedErrorOccured = false;
                    continue;
                } else {
                    throw new GeneralFailureException(te.getMessage(), te);
                }
            }
        }
    }

    /**
     * Gets the node status.
     *
     * @return
     */
    @Override
    protected CacheNodeStatus GetNodeStatus() {
        CacheNodeStatus status = CacheNodeStatus.Running;

        //Check for state transfer.
        if (_stateTransferTask != null && _stateTransferTask.getIsRunning()) {
            status = CacheNodeStatus.InStateTransfer;
        }

        if (_corresponders != null && _corresponders.size() > 0) {
            status = CacheNodeStatus.InStateTransfer;
        }

        return status;
    }


    private EnumerationDataChunk Clustered_GetNextChunk(Address address, EnumerationPointer pointer, OperationContext operationContext) throws GeneralFailureException, CacheException {
        try {
            Function func = new Function(OpCodes.GetNextChunk.getValue(), new Object[]{
                pointer, operationContext
            });
            Object result = getCluster().SendMessage(address, func, GroupRequest.GET_FIRST, getCluster().getTimeout());

            EnumerationDataChunk nextChunk = (EnumerationDataChunk) ((result instanceof EnumerationDataChunk) ? result : null);

            return nextChunk;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    private EnumerationDataChunk handleGetNextChunk(Address src, Object info) throws GeneralFailureException, TimeoutException, SuspectedException, CacheException {
        Object[] package_Renamed = (Object[]) ((info instanceof Object[]) ? info : null);
        EnumerationPointer pointer = (EnumerationPointer) ((package_Renamed[0] instanceof EnumerationPointer) ? package_Renamed[0] : null);
        OperationContext operationContext = (OperationContext) ((package_Renamed[1] instanceof OperationContext) ? package_Renamed[1] : null);

        EnumerationDataChunk nextChunk = getInternalCache().GetNextChunk(pointer, operationContext);
        return nextChunk;
    }


    @Override
    public boolean IsInStateTransfer() {
        boolean inTransfer = false;
        if (GetNodeStatus() == CacheNodeStatus.InStateTransfer) {
            inTransfer = true;
        } else {
            inTransfer = _distributionMgr.InStateTransfer();
        }
        return inTransfer;
    }

    @Override
    protected boolean VerifyClientViewId(long clientLastViewId) {
        return clientLastViewId == getCluster().getLastViewID();
    }

    @Override
    protected java.util.ArrayList GetDestInStateTransfer() {
        java.util.ArrayList list = _distributionMgr.GetPermanentAddress(this.getServers());
        return list;
    }
    
        
    @Override
    public void OnTaskCallback(Object taskID, Object value, OperationContext operationContext, EventContext eventContext){
    
        if(value!=null)
            RaiseTaskCalbackNotifier(taskID, (List) value, eventContext);
    }
}
