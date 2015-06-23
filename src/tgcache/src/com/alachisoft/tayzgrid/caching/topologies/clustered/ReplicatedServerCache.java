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
import com.alachisoft.tayzgrid.caching.util.CacheHelper;
import com.alachisoft.tayzgrid.caching.util.LazyKeysetEnumerator;
import com.alachisoft.tayzgrid.caching.util.ClusterHelper;
import com.alachisoft.tayzgrid.caching.topologies.local.IndexedOverflowCache;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.topologies.IClusterEventsListener;
import com.alachisoft.tayzgrid.caching.statistics.NodeStatus;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics;
import com.alachisoft.tayzgrid.caching.queries.DeleteQueryResultSet;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DataSourceReplicationManager;
import com.alachisoft.tayzgrid.caching.datasourceproviders.WriteBehindAsyncProcessor;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.NodeExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.EventContext;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.AllowedOperationType;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.EventId;
import com.alachisoft.tayzgrid.caching.DataSourceUpdateOptions;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.OpCode;
import com.alachisoft.tayzgrid.common.threading.Promise;
import com.alachisoft.tayzgrid.common.threading.Latch;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.datastructures.NewHashmap;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;
import com.alachisoft.tayzgrid.common.datastructures.ReplicatedStateTransferStatus;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.ResetableIterator;
import com.alachisoft.tayzgrid.common.EventCategories;
import com.alachisoft.tayzgrid.common.LockOptions;
import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.RemoteException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.caching.datagrouping.GroupInfo;
import com.alachisoft.tayzgrid.caching.topologies.local.IndexedLocalCache;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;

import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatus;
import com.alachisoft.tayzgrid.persistence.EventType;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class provides the partitioned cluster cache primitives.
 */
public class ReplicatedServerCache extends ReplicatedCacheBase implements IPresenceAnnouncement, ICacheEventsListener {

    /**
     * Call balancing helper object.
     */
    private IActivityDistributor _callBalancer;
    /**
     * The periodic update task.
     */
    private PeriodicPresenceAnnouncer _taskUpdate;
    private PeriodicStatsUpdater _localStatsUpdater;
    private StateTransferTask _stateTransferTask = null;
    /**
     * keeps track of all server members excluding itself
     */
    protected java.util.List _otherServers = Collections.synchronizedList(new java.util.ArrayList(11));
    private java.util.ArrayList _nodesInStateTransfer = new java.util.ArrayList();
    private boolean _allowEventRaiseLocally;
    private Latch _stateTransferLatch = new Latch((byte) ReplicatedStateTransferStatus.UNDER_STATE_TRANSFER);
    private AsyncItemReplicator _asyncReplicator = null;
    private boolean threadRunning = true;
    private int confirmClusterStartUP = 3;
    int eventCounter = 0;
    protected InetAddress _srvrJustLeft = null;

    /**
     * Constructor.
     *
     * @param cacheSchemes collection of cache schemes (config properties).
     * @param properties properties collection for this cache.
     * @param listener cache events listener
     */
    public ReplicatedServerCache(java.util.Map cacheClasses, java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context) throws ConfigurationException {
        super(properties, listener, context);
        _stats.setClassName("replicated-server");
        Initialize(cacheClasses, properties);
    }

    /**
     *
     *
     * @param cacheClasses
     * @param properties
     * @param listener
     * @param context
     * @param clusterListener
     * @param userId
     * @param password
     */
    public ReplicatedServerCache(java.util.Map cacheClasses, java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context, IClusterEventsListener clusterListener, String userId, String password) {
        super(properties, listener, context, clusterListener);
        _stats.setClassName("replicated-server");

    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    @Override
    public void dispose() {
        if (_stateTransferTask != null) {
            _stateTransferTask.StopProcessing();
        }
        if (_taskUpdate != null) {
            _taskUpdate.Cancel();
            _taskUpdate = null;
        }

        if (_localStatsUpdater != null) {
            _localStatsUpdater.Cancel();
            _localStatsUpdater = null;
        }

        if (_internalCache != null) {
            _internalCache.dispose();
            _internalCache = null;
        }

        threadRunning = false;
        super.dispose();
    }

    @Override
    public void WindUpReplicatorTask() {
        if (_asyncReplicator != null) {
            _asyncReplicator.WindUpTask();
        }
    }

    @Override
    public void WaitForReplicatorTask(long interval) {
        if (_asyncReplicator != null) {
            _asyncReplicator.WaitForShutDown(interval);
        }
    }

    @Override
    public boolean IsOperationAllowed(Object key, AllowedOperationType opType) {
        if (super._shutdownServers != null && super._shutdownServers.size() > 0) {
            if (opType == AllowedOperationType.AtomicWrite) {
                return false;
            }

            // reads will be allowed on shut down node too?
            if (IsInStateTransfer()) {
                if (super.IsShutdownServer(getCluster().getCoordinator())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean IsOperationAllowed(Object[] keys, AllowedOperationType opType, OperationContext operationContext) {
        if (super._shutdownServers != null && super._shutdownServers.size() > 0) {
            if (opType == AllowedOperationType.BulkWrite) {
                return false;
            }

            if (IsInStateTransfer()) {
                if (super.IsShutdownServer(getCluster().getCoordinator())) {
                    return false;
                }
            }

        }

        return true;
    }

    @Override
    public boolean IsOperationAllowed(AllowedOperationType opType, OperationContext operationContext) {
        if (super._shutdownServers != null && super._shutdownServers.size() > 0) {
            if (opType == AllowedOperationType.BulkWrite) {
                return false;
            }

            if (opType == AllowedOperationType.BulkRead) {
                if (IsInStateTransfer()) {
                    if (super.IsShutdownServer(getCluster().getCoordinator())) {
                        return false;
                    }
                }
            }
        }
        return true;
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
    protected void Initialize(java.util.Map cacheClasses, java.util.Map properties) throws ConfigurationException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        try {
            super.Initialize(cacheClasses, properties);

            java.util.Map frontCacheProps = ConfigHelper.GetCacheScheme(cacheClasses, properties, "internal-cache");
            String cacheType = String.valueOf(frontCacheProps.get("type")).toLowerCase();
            if (cacheType.compareTo("local-cache") == 0) {
                _internalCache = CacheBase.Synchronized(new IndexedLocalCache(cacheClasses, this, frontCacheProps, this, _context));
            } else if (cacheType.compareTo("overflow-cache") == 0) {
                _internalCache = CacheBase.Synchronized(new IndexedOverflowCache(cacheClasses, this, frontCacheProps, this, _context));
            } else {
                throw new ConfigurationException("invalid or non-local class specified in partitioned cache");
            }

            _stats.setNodes(Collections.synchronizedList(new java.util.ArrayList()));
            _callBalancer = new CallBalancer();

            InitializeCluster(properties, getName(), MCAST_DOMAIN, 
                    new Identity(true, (_context.getRender() != null ? _context.getRender().getPort() : 0), 
                    (_context.getRender()!= null ? _context.getRender().getIPAddress() : null)));
            _stats.setGroupName(getCluster().getClusterName());

            postInstrumentatedData(_stats, getName());
            HasStarted();
        } catch (ConfigurationException e) {
            dispose();
            throw e;
        } catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.getMessage(), e);
        }

    }

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
                _internalCache = CacheBase.Synchronized(new IndexedLocalCache(cacheClasses, this, frontCacheProps, this, _context));
            } else {
                throw new ConfigurationException("invalid or non-local class specified in partitioned cache");
            }

            _stats.setNodes(Collections.synchronizedList(new java.util.ArrayList()));
            _callBalancer = new CallBalancer();

            InitializeCluster(properties, getName(), MCAST_DOMAIN, new Identity(true, (_context.getRender() != null ? _context.getRender().getPort() : 0), (_context.getRender()
                    != null ? _context.getRender().getIPAddress() : null)), userId, password, twoPhaseInitialization, false);
            _stats.setGroupName(getCluster().getClusterName());

            postInstrumentatedData(_stats, getName());

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
     * Return the next node in call balacing order.
     *
     * @return
     */
    private Address GetNextNode() {
        NodeInfo node = _callBalancer.SelectNode(_stats, null);
        return node == null ? null : node.getAddress();
    }

    /**
     * Called after the membership has been changed. Lets the members do some
     * member oriented tasks.
     */
    @Override
    public void OnAfterMembershipChange() throws InterruptedException, OperationFailedException {
        super.OnAfterMembershipChange();
        _context.ExpiryMgr.setAllowClusteredExpiry(getCluster().getIsCoordinator());

        if (_taskUpdate == null) {
            _taskUpdate = new PeriodicPresenceAnnouncer(this, _statsReplInterval);
            _context.TimeSched.AddTask(_taskUpdate);

            StartStateTransfer();
        }

        if (_localStatsUpdater == null) {
            _localStatsUpdater = new PeriodicStatsUpdater(this);
            _context.TimeSched.AddTask(_localStatsUpdater);
        }

        if (getCluster().getIsCoordinator()) {

            if(getCluster().getIsCoordinator())
            {
                if(_context.ClientDeathDetection != null) _context.ClientDeathDetection.StartMonitoringClients();
            }

          
            if (_context.getDsMgr() != null && _context.getDsMgr().getIsWriteBehindEnabled() ) {
                _context.getDsMgr().StartWriteBehindProcessor();
            }
        } 


       

        //async replicator is used to replicate the update index operations to other replica nodes.
        if (getCluster().getServers().size() > 1) {
            if (_asyncReplicator == null) {
                _asyncReplicator = new AsyncItemReplicator(getContext(), new TimeSpan(0, 0, 2));
            }
            _asyncReplicator.Start();
            getContext().getCacheLog().CriticalInfo("OnAfterMembershipChange", "async-replicator started.");
        } else {
            if (_asyncReplicator != null) {
                _asyncReplicator.Stop(false);
                _asyncReplicator = null;
                getContext().getCacheLog().CriticalInfo("OnAfterMembershipChange", "async-replicator stopped.");
            }
        }
        UpdateCacheStatistics();
    }

    /**
     * Called when a new member joins the group.
     *
     * ///
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

        if (getLocalAddress().compareTo(address) == 0) {
            _stats.setLocalNode(info);
        } else {
            synchronized (_nodesInStateTransfer) {
                if (!_nodesInStateTransfer.contains(address)) {
                    _nodesInStateTransfer.add(address);
                }
            }
            //add into the list of other servers.
            if (!_otherServers.contains(address)) {
                _otherServers.add(address);
            }
        }
        if (!info.getIsInproc()) {
            AddServerInformation(address, identity.getRendererPort(), info.getConnectedClients().size());

        }

        if (getContext().getCacheLog().getIsInfoEnabled()) {
            getContext().getCacheLog().Info("ReplicatedCache.OnMemberJoined()", "Replication increased: " + address);
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

        //remove into the list of other servers.
        _otherServers.remove(address);

        if (!info.getIsInproc()) {
            RemoveServerInformation(address, identity.getRendererPort());

        }

        if (getContext().getCacheLog().getIsInfoEnabled()) {
            getContext().getCacheLog().Info("ReplicatedCache.OnMemberLeft()", "Replica Removed: " + address);
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
    public Object HandleClusterMessage(Address src, Function func) throws OperationFailedException,  GeneralFailureException, StateTransferException, LockingException, CacheException, SuspectedException, Exception {
        if (!getCluster().getValidMembers().contains(src)) {
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("ReplicatedCache.HandleClusterMessage()", src + " is not a valid member so its message is discarded");
            }
            return null;
        }
        OpCodes opCode = OpCodes.forValue((int) func.getOpcode());
        switch (opCode) {
            case PeriodicUpdate:
                return handlePresenceAnnouncement(src, func.getOperand());

            case ReqStatus:
                return this.handleReqStatus();

            case GetCount:
                return handleCount();

            case Contains:
                return handleContains(func.getOperand());

            case Get:
                return handleGet(func.getOperand());

            case Insert:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleInsert(src, func.getOperand(), func.getUserPayload());

            case Add:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleAdd(src, func.getOperand(), func.getUserPayload());

            case AddHint:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleAddHint(src, func.getOperand());

           
            case Remove:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleRemove(src, func.getOperand());

            case RemoveRange:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleRemoveRange(func.getOperand());

            case Clear:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleClear(src, func.getOperand());

            case GetKeys:
                return handleGetKeys(func.getOperand());

            case KeyList:
                return handleKeyList();

            case NotifyAdd:
                return handleNotifyAdd(func.getOperand());

            case NotifyUpdate:
                return handleNotifyUpdate(func.getOperand());

            case NotifyRemoval:
                return handleNotifyRemoval(func.getOperand());
            case Search:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleSearch(func.getOperand());

            case SearchEntries:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleSearchEntries(func.getOperand());
            case GetDataGroupInfo:
                return handleGetGroupInfo(func.getOperand());

            case GetGroup:
                return handleGetGroup(func.getOperand());

            case UpdateIndice:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleUpdateIndice(func.getOperand());

            case LockKey:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleLock(func.getOperand());

            case UnLockKey:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                handleUnLockKey(func.getOperand());
                break;

            case IsLocked:
                return handleIsLocked(func.getOperand());

            case GetTag:
                return handleGetTag(func.getOperand());

            case ReplicateOperations:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleReplicateOperations(src, func.getOperand(), func.getUserPayload());

            case GetNextChunk:
                _stateTransferLatch.WaitForAny((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED);
                return handleGetNextChunk(src, func.getOperand());
            
            case DeleteQuery:
                return handleDeleteQuery(func.getOperand());
         
            case GetKeysByTag:
                return handleGetKeysByTag(func.getOperand());

            case SignalEndOfStateTxfr:
                handleSignalEndOfStateTxfr(src);
                break;

            case NotifyCustomRemoveCallback:
                return handleNotifyRemoveCallback(func.getOperand());

            case NotifyCustomUpdateCallback:
                return handleNotifyUpdateCallback(func.getOperand());

        }
        return super.HandleClusterMessage(src, func);
    }

    private void handleSignalEndOfStateTxfr(Address requestingNode) throws Exception {
        if (getCluster().getIsCoordinator()) {
            byte b1 = 0, b2 = 1, b3 = 2;
        }
        synchronized (_nodesInStateTransfer) {
            _nodesInStateTransfer.remove(requestingNode);
            _allowEventRaiseLocally = true;
        }

    }

    /**
     * Periodic update (PULL model), i.e. on demand fetch of information from
     * every node.
     */
    public boolean DetermineClusterStatus() {
        try {
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("ReplicatedServerCache.DetermineClusterStatus", " determine cluster status");
            }
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
            //System.err.println(e.toString() + " DetermineClusterStatus");
        } catch (Exception e) {
            getContext().getCacheLog().Error("ReplicatedCache.DetermineClusterStatus()", e.toString());
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
     * Periodic update (PUSH model), i.e., Publish cache statisitcs so that
     * every node in the cluster gets an idea of the state of every other node.
     */
    public final boolean AnnouncePresence(boolean urgent) {
        try {
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("ReplicatedCache.AnnouncePresence()", " announcing presence ;urget " + urgent);
            }

            if (this.getValidMembers().size() > 1) {
                Function func = new Function(OpCodes.PeriodicUpdate.getValue(), handleReqStatus());
                if (!urgent) {
                    getCluster().SendNoReplyMessage(func);
                } else {
                    getCluster().Broadcast(func, GroupRequest.GET_NONE, false, Priority.Normal);
                }
            }
            return true;
        } catch (Exception e) {
            getContext().getCacheLog().Error("ReplicatedCache.AnnouncePresence()", e.toString());
        }
        return false;
    }

    /**
     * Handler for Periodic update (PUSH model).
     *
     * @param sender
     * @param obj
     * @return
     */
    private Object handlePresenceAnnouncement(Address sender, Object obj) {
        synchronized (getServers()) {
            NodeInfo other = (NodeInfo) ((obj instanceof NodeInfo) ? obj : null);
            NodeInfo info = _stats.GetNode((Address) ((sender instanceof Address) ? sender : null));
            if (other != null && info != null) {
                getContext().getCacheLog().Debug("Replicated.handlePresenceAnnouncement()", "sender = " + sender + " stats = " + other.getStatistics());
                info.setStatistics(other.getStatistics());
                info.setConnectedClients(other.getConnectedClients());
                info.setStatus(other.getStatus());
            }

        }
        UpdateCacheStatistics();
        return null;
    }

    @Override
    public CacheStatistics CombineClusterStatistics(ClusterCacheStatistics s) {
        CacheStatistics c = ClusterHelper.CombineReplicatedStatistics(s);
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
        AnnouncePresence(false);
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
        AnnouncePresence(false);
    }

    /**
     * Asynchronous state tranfer job.
     */
    protected static class StateTransferTask implements AsyncProcessor.IAsyncTask {

        /**
         * The partition base class
         */
        private ReplicatedServerCache _parent = null;
        /**
         * A promise object to wait on.
         */
        private Promise _promise = null;
        private ILogger _ncacheLog;

        private ILogger getCacheLog() {
            return _ncacheLog;
        }
        private boolean _stopProcessing = false;

        /**
         * Constructor
         *
         * @param parent
         */
        public StateTransferTask(ReplicatedServerCache parent) {
            _parent = parent;
            _ncacheLog = parent.getContext().getCacheLog();

            _promise = new Promise();
        }

        public final void StopProcessing() {
            _stopProcessing = true;
        }

        /**
         * Wait until a result is available for the state transfer task.
         *
         * @param timeout
         * @return
         */
        public final Object WaitUntilCompletion(long timeout) {
            return _promise.WaitResult(timeout);
        }

        /**
         * Signal the end of state transfer.
         *
         * @param result transfer result
         * @return
         */
        private void SignalEndOfTransfer(Object result) throws IOException, GeneralFailureException, OperationFailedException, CacheException, Exception {
            _parent.EndStateTransfer(_parent.Local_Count());
            _promise.SetResult(result);
        }

        /**
         * Do the state transfer now.
         */
        @Override
        public void Process() {

            Object result = null;
            boolean logEvent = false;
            try {
                _parent._statusLatch.SetStatusBit(NodeStatus.Initializing, NodeStatus.Running);
                _parent.DetermineClusterStatus();

                DataSourceReplicationManager dsRepMgr = null;
                try {
                    dsRepMgr = new DataSourceReplicationManager(_parent, _parent.getContext().getDsMgr(), _parent.getContext().getCacheLog());
                    dsRepMgr.ReplicateWriteBehindQueue();

                } catch (Exception exc) {
                    getCacheLog().Error("StateTransfer.Process", "Transfering queue: " + exc.toString());
                } finally {
                    if (dsRepMgr != null) {
                        dsRepMgr.dispose();
                    }
                }

                // Fetch the list of keys from coordinator and open an enumerator
                Iterator ie = _parent.Clustered_GetEnumerator(_parent.getCluster().getCoordinator());
                if (ie == null) {
                    // there are no keys, the cache is empty
                    _parent._stateTransferLatch.SetStatusBit((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED, (byte) ReplicatedStateTransferStatus.UNDER_STATE_TRANSFER);
                    return;
                }

                java.util.HashMap keysTable = new java.util.HashMap();
                while (ie.hasNext()) {
                    Map.Entry pair = (Map.Entry) ie.next();
                    Object key = pair.getKey();
                    keysTable.put(key, null);
                }

                getCacheLog().CriticalInfo("ReplicatedServerCache.StateTransfer", "Transfered keys list" + keysTable.size());
                _parent._internalCache.SetStateTransferKeyList(keysTable);
                _parent._stateTransferLatch.SetStatusBit((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED, (byte) ReplicatedStateTransferStatus.UNDER_STATE_TRANSFER);

                ie = _parent.Clustered_GetEnumerator(_parent.getCluster().getCoordinator());

                java.util.HashMap itemsHavingKeyDependency = new java.util.HashMap();
                boolean loggedonce = false;
                while (ie.hasNext()) {
                    Map.Entry pair = (Map.Entry) ie.next();
                    if (!loggedonce) {
                        getCacheLog().CriticalInfo("ReplicatedServerCache.StateTransfer", "State transfer has started");
                        if (_parent.alertPropagator != null) {
                            _parent.alertPropagator.RaiseAlert(EventID.StateTransferStart, "NCache", "\"" + _parent._context.getSerializationContext() + "\""
                                    + " has started state transfer.");
                        }
                        //-
                        EventLogger.LogEvent("TayzGrid", "\"" + _parent._context.getSerializationContext() + "\"" + " has started state transfer.", com.alachisoft.tayzgrid.common.enums.EventType.INFORMATION, EventCategories.Information, EventID.StateTransferStart);
                        loggedonce = logEvent = true;
                    }

                    if (!_stopProcessing) {
                        Object key = pair.getKey();
                        // if the object is already there, skip a network call
                        if (_parent.Local_Contains(key, new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation))) {
                            continue;
                        }
                        // fetches the object remotely
                        CacheEntry val = (CacheEntry) ((pair.getValue() instanceof CacheEntry) ? pair.getValue() : null);
                        if (val != null) {
                           
                            try {
                                // doing an Add ensures that the object is not updated
                                // if it had already been added while we were fetching it.
                                if (!_stopProcessing) {
                                    OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);

                                    _parent.Local_Add(key, val, null, null, false, operationContext);
                                    _parent.getContext().PerfStatsColl.incrementStateTxfrPerSecStats();
                                } else {
                                    result = _parent.Local_Count();
                                    if (getCacheLog().getIsInfoEnabled()) {
                                        getCacheLog().Info("ReplicatedServerCache.StateTransfer", "  state transfer was stopped by the parent.");
                                    }

                                    return;
                                }
                            } catch (Exception e) {
                                // object already there so skip it.
                            }
                        }
                    } else {
                        result = _parent.Local_Count();
                        if (getCacheLog().getIsInfoEnabled()) {
                            getCacheLog().Info("ReplicatedServerCache.StateTransfer", "  state transfer was stopped by the parent.");
                        }
                        return;
                    }
                }

                if (!_stopProcessing) {
                    
                } else {
                    result = _parent.Local_Count();
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ReplicatedServerCache.StateTransfer", "  state transfer was stopped by the parent.");
                    }
                    return;
                }
                result = _parent.Local_Count();

            } catch (Exception e) {
                result = e;
            } finally {
                _parent._stateTransferLatch.SetStatusBit((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED, (byte) ReplicatedStateTransferStatus.UNDER_STATE_TRANSFER);
                _parent._internalCache.UnSetStateTransferKeyList();
                if (_parent != null && _parent._context != null && _parent._context.PerfStatsColl != null) {
                    _parent._context.PerfStatsColl.nodeStatus(CacheNodeStatus.Running.getValue());
                }
                try {
                    Thread.sleep(2000);
                    SignalEndOfTransfer(result);
                } catch (Exception e) {

                }
                if (logEvent) {
                    getCacheLog().CriticalInfo("ReplicatedServerCache.StateTransfer", "State transfer has ended");

                    if (result instanceof Exception) {
                        EventLogger.LogEvent("TayzGrid", "\"" + _parent._context.getSerializationContext() + "\"" + " has ended state transfer prematurely.", com.alachisoft.tayzgrid.common.enums.EventType.ERROR, EventCategories.Error, EventID.StateTransferError);
                        if (_parent.alertPropagator != null) {
                            _parent.alertPropagator.RaiseAlert(EventID.StateTransferError, "NCache", "\"" + _parent._context.getSerializationContext() + "\""
                                    + " has ended state transfer prematurely.");
                        }
                    } else {
                        if (_parent.alertPropagator != null) {
                            _parent.alertPropagator.RaiseAlert(EventID.StateTransferStop, "NCache", "\"" + _parent._context.getSerializationContext() + "\""
                                    + " has completed state transfer.");
                        }
                        EventLogger.LogEvent("TayzGrid", "\"" + _parent._context.getSerializationContext() + "\"" + " has completed state transfer.", com.alachisoft.tayzgrid.common.enums.EventType.INFORMATION, EventCategories.Information, EventID.StateTransferStop);
                    }
                }

            }
        }
    }

    /**
     * Fetch state from a cluster member. If the node is the coordinator there
     * is no need to do the state transfer.
     */
    protected final void StartStateTransfer() {
        if (!getCluster().getIsCoordinator()) {
            /**
             * Tell everyone that we are not fully-functional, i.e.,
             * initilizing.
             */
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("ReplicatedCache.StartStateTransfer()", "Requesting state transfer " + getLocalAddress());
            }
            if (_context != null && _context.PerfStatsColl != null) {
                _context.PerfStatsColl.nodeStatus(CacheNodeStatus.InStateTransfer.getValue());
            }
            /**
             * Start the initialization(state trasfer) task.
             */
            if (_stateTransferTask == null) {
                _stateTransferTask = new StateTransferTask(this);
            }
            _context.AsyncProc.Enqueue(_stateTransferTask);

            /**
             * Un-comment the following line to do it synchronously. object v =
             * stateTransferTask.WaitUntilCompletion(-1);
             */
        } else {
            _stateTransferLatch.SetStatusBit((byte) ReplicatedStateTransferStatus.STATE_TRANSFER_COMPLETED, (byte) ReplicatedStateTransferStatus.UNDER_STATE_TRANSFER);
            _allowEventRaiseLocally = true;
            _statusLatch.SetStatusBit(NodeStatus.Running, NodeStatus.Initializing);
            UpdateCacheStatistics();
            AnnouncePresence(true);
        }
    }

    /**
     * Fetch state from a cluster member. If the node is the coordinator there
     * is no need to do the state transfer.
     */
    public final void EndStateTransfer(Object result) throws IOException, ClassNotFoundException {
        if (getContext().getCacheLog().getIsInfoEnabled()) {
            getContext().getCacheLog().Info("ReplicatedCache.EndStateTransfer()", "State Txfr ended: " + result);
        }
        if (result instanceof Exception) {
            /**
             * What to do? if we failed the state transfer?. Proabably we'll
             * keep servicing in degraded mode? For the time being we don't!
             */
        }

        /**
         * Set the status to fully-functional (Running) and tell everyone about
         * it.
         */
        _statusLatch.SetStatusBit(NodeStatus.Running, NodeStatus.Initializing);

        Function fun = new Function(OpCodes.SignalEndOfStateTxfr.getValue(), "", false);
        if (getCluster() != null) {
            getCluster().BroadcastToServers(fun, GroupRequest.GET_ALL, true);

        }

        UpdateCacheStatistics();
        AnnouncePresence(true);
    }

    /**
     * returns the number of objects contained in the cache.
     */
    @Override
    public long getCount() throws GeneralFailureException, OperationFailedException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }
        long count = 0;
        if (IsInStateTransfer()) {
            count = Clustered_Count(GetDestInStateTransfer());
        } else {
            count = Local_Count();
        }
        return count;
    }

    @Override
    public long getSessionCount() {
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }
        /**
         * If we are in state transfer, we return the count from some other
         * functional node.
         */
        if (_statusLatch.IsAnyBitsSet(NodeStatus.Initializing)) {
            try {
                return Clustered_SessionCount();
            } catch (CacheException cacheException) {
            }
        }
        return Local_SessionCount();
    }

    @Override
    public InetAddress getServerJustLeft() {
        return _srvrJustLeft;
    }

    @Override
    public void setServerJustLeft(InetAddress value) {
        _srvrJustLeft = value;
    }

    @Override
    public int getServersCount() {
        return getCluster().getValidMembers().size();
    }

    @Override
    public boolean IsServerNodeIp(Address clientAddress) {
        for (Iterator it = getCluster().getServers().iterator(); it.hasNext();) {
            Address addr = (Address) it.next();
            if (addr.getIpAddress().getHostName().equals(clientAddress.getIpAddress().getHostName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the count of local cache items only.
     *
     * @return count of items.
     */
    private long Local_SessionCount() {
        if (_internalCache != null) {
            return _internalCache.getSessionCount();
        }
        return 0;
    }

    private long Clustered_SessionCount() throws CacheException {
        Address targetNode = GetNextNode();
        return Clustered_SessionCount(targetNode);
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

    /**
     * Hanlde cluster-wide Get(key) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleSessionCount() throws OperationFailedException {
        try {
            return Local_SessionCount();
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return 0;
    }

    /**
     * Determines whether the cache contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     */
    @Override
    public boolean Contains(Object key, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("RepCache.Cont", "");
        }
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        boolean contains = false;

        if (IsInStateTransfer()) {
            try {
                contains = Safe_Clustered_Contains(key, operationContext) != null;
            } catch (TimeoutException timeoutException) {
                throw timeoutException;
            } catch (SuspectedException suspectedException) {
                throw new OperationFailedException(suspectedException);
            }
        } else {
            contains = Local_Contains(key, operationContext);
        }

        return contains;
    }

    @Override
    public DeleteQueryResultSet DeleteQuery(String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason ir, OperationContext operationContext)
            throws Exception {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }
        DeleteQueryResultSet result;

        if (getCluster().getServers().size() > 1) {
            java.util.ArrayList servers = new java.util.ArrayList();
            servers.addAll(this.getServers());
            result = Clustered_DeleteQuery(servers, query, values, notify, isUserOperation, ir, operationContext);
        } else {
            result = Local_DeleteQuery(query, values, notify, isUserOperation, ir, operationContext);
        }

        return result;
    }

    private DeleteQueryResultSet Local_DeleteQuery(String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason ir, OperationContext operationContext)
            throws Exception {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        return _internalCache.DeleteQuery(query, values, notify, isUserOperation, ir, operationContext);
    }

    @Override
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
     * Determines whether the cache contains the specified keys.
     *
     * @param keys The keys to locate in the cache.
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     */
    @Override
    public java.util.HashMap Contains(Object[] keys, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("RepCache.ContBlk", "");
        }
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap result = new java.util.HashMap();
        java.util.HashMap tbl = Local_Contains(keys, operationContext);
        java.util.ArrayList list = new ArrayList();

        if (tbl != null && tbl.size() > 0) {
            list = (java.util.ArrayList) tbl.get("items-found");
        }

        /**
         * If we failed and during state transfer, we check from some other
         * functional node as well.
         */
        if (list != null && list.size() < keys.length && (_statusLatch.IsAnyBitsSet(NodeStatus.Initializing))) {
            Object[] rKeys = new Object[keys.length - list.size()];
            int i = 0;
            java.util.Iterator ir = Arrays.asList(keys).iterator();
            while (ir.hasNext()) {
                Object key = ir.next();
                if (list.contains(key) == false) {
                    rKeys[i] = key;
                    i++;
                }
            }

            java.util.HashMap clusterTbl = null;
            try {
                clusterTbl = Clustered_Contains(rKeys, operationContext);
            } catch (SuspectedException suspectedException) {
                throw new OperationFailedException(suspectedException);
            }

            java.util.ArrayList clusterList = null;

            if (clusterTbl != null && clusterTbl.size() > 0) {
                clusterList = (java.util.ArrayList) clusterTbl.get("items-found");
            }

            java.util.Iterator ie = clusterList.iterator();
            while (ie.hasNext()) {
                list.add(ie.next());
            }
        }
        result.put("items-found", list);
        return result;
    }

    /**
     * Determines whether the local cache contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @return true if the cache contains an element with the specified key;
     * otherwise, false.
     */
    private boolean Local_Contains(Object key, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        boolean retVal = false;
        if (_internalCache != null) {
            retVal = _internalCache.Contains(key, operationContext);
        }
        return retVal;
    }

    /**
     * Determines whether the local cache contains the specified keys.
     *
     * @param keys The keys to locate in the cache.
     * @return List of keys available in cache
     */
    private java.util.HashMap Local_Contains(Object[] keys, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap tbl = new java.util.HashMap();
        if (_internalCache != null) {
            tbl = _internalCache.Contains(keys, operationContext);
        }
        return tbl;
    }

    /**
     * Determines whether the cluster contains a specific key.
     *
     * @param key The key to locate in the cache.
     * @return address of the node that contains the specified key; otherwise,
     * null.
     */
    private Address Clustered_Contains(Object key, OperationContext operationContext) throws TimeoutException, SuspectedException, GeneralFailureException, CacheException {
        Address targetNode = GetNextNode();
        if (targetNode == null) {
            return null;
        }
        return Clustered_Contains(targetNode, key, operationContext);
    }

    /**
     *
     *
     * @param key
     * @return
     */
    private Address Safe_Clustered_Contains(Object key, OperationContext operationContext) throws TimeoutException, SuspectedException, GeneralFailureException, CacheException {
        try {
            return Clustered_Contains(key, operationContext);
        } catch (SuspectedException e) {
            return Clustered_Contains(key, operationContext);
        } catch (TimeoutException e) {
            return Clustered_Contains(key, operationContext);
        }
    }

    /**
     * Determines whether the cluster contains the specified keys.
     *
     * @param key The keys to locate in the cache.
     * @return list of keys and their addresses
     */
    private java.util.HashMap Clustered_Contains(Object[] keys, OperationContext operationContext) throws TimeoutException, SuspectedException, GeneralFailureException, CacheException {
        Address targetNode = GetNextNode();
        return Clustered_Contains(targetNode, keys, operationContext);
    }

    /**
     * Hanlde cluster-wide Contain(key(s)) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleContains(Object info) throws OperationFailedException {
        try {
            OperationContext operationContext = null;
            if (info instanceof Object[]) {
                Object[] objs = (Object[]) ((info instanceof Object[]) ? info : null);
                if (objs[0] instanceof Object[] && objs.length > 1) {
                    operationContext = (OperationContext) ((objs[1] instanceof OperationContext) ? objs[1] : null);
                }

                if (objs[0] instanceof Object[]) {
                    return Local_Contains((Object[]) objs[0], operationContext);
                } else {
                    if (Local_Contains(objs[0], operationContext)) {
                        return true;
                    }
                }

            } else {
                if (Local_Contains(info, operationContext)) {
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

    @Override
    protected QueryResultSet Local_Search(String queryText, java.util.Map values, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        QueryResultSet resultSet = null;

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        resultSet = _internalCache.Search(queryText, values, operationContext);

        return resultSet;
    }

    @Override
    protected QueryResultSet Local_SearchEntries(String queryText, java.util.Map values, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        QueryResultSet resultSet = null;

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        resultSet = _internalCache.SearchEntries(queryText, values, operationContext);

        return resultSet;
    }

    /**
     *
     * @param info
     * @return
     */
    public final QueryResultSet handleSearch(Object info) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache != null) {
            java.util.ArrayList keyList = new java.util.ArrayList();
            Object[] data = (Object[]) info;
            return _internalCache.Search((String) ((data[0] instanceof String) ? data[0] : null), (java.util.Map) ((data[1] instanceof java.util.Map) ? data[1] : null), (OperationContext) ((data[2] instanceof OperationContext) ? data[2] : null));
        }
        return null;
    }

    /**
     *
     * @param info
     * @return
     */
    public final QueryResultSet handleSearchEntries(Object info) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache != null) {
            Object[] data = (Object[]) info;
            OperationContext operationContext = null;
            if (data.length > 2) {
                operationContext = (OperationContext) ((data[2] instanceof OperationContext) ? data[2] : null);
            }

            return _internalCache.SearchEntries((String) ((data[0] instanceof String) ? data[0] : null), (java.util.Map) ((data[1] instanceof java.util.Map) ? data[1] : null), operationContext);
        }
        return null;
    }

    public final QueryResultSet Clustered_SearchEntries(String queryText, java.util.Map values, OperationContext operationContext) throws CacheException {
        return Clustered_SearchEntries(getCluster().getCoordinator(), queryText, values, true, operationContext);
    }

    /**
     * Removes all entries from the store.
     *
     *
     * This method invokes <see cref="handleClear"/> on every node in the
     * cluster, which then fires OnCacheCleared locally. The <see
     * cref="handleClear"/> method on the coordinator will also trigger a
     * cluster-wide notification to the clients.
     *
     */
    @Override
    public void Clear(CallbackEntry cbEntry, DataSourceUpdateOptions updateOptions, OperationContext operationContext) throws OperationFailedException, GeneralFailureException, CacheException {
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_statusLatch.IsAnyBitsSet(NodeStatus.Initializing)) {
            if (_stateTransferTask != null) {
                _stateTransferTask.StopProcessing();
            }
            _statusLatch.WaitForAny(NodeStatus.Running);
        }

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        String taskId = null;
        if (updateOptions == DataSourceUpdateOptions.WriteBehind) {
            taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }

        if (getCluster().getServers().size() > 1) {
            Clustered_Clear(cbEntry, taskId, false, operationContext);
        } else {
            handleClear(getCluster().getLocalAddress(), new Object[]{
                cbEntry, taskId, operationContext
            });
        }

        if (taskId != null) {
            SignalTaskState(getCluster().getServers(), taskId, null, OpCode.Clear, WriteBehindAsyncProcessor.TaskState.Execute, operationContext);
        }
    }

    /**
     * Clears the local cache only.
     */
    private void Local_Clear(Address src, CallbackEntry cbEntry, String taskId, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache != null) {
            _internalCache.Clear(null, DataSourceUpdateOptions.None, operationContext);
            if (taskId != null) {
                CacheEntry entry = new CacheEntry(cbEntry, null, null);
                if (operationContext.Contains(OperationContextFieldName.ReadThruProviderName)) {
                    entry.setProviderName((String) operationContext.GetValueByField(OperationContextFieldName.ReadThruProviderName));
                }
                super.AddWriteBehindTask(src, null, entry, taskId, OpCode.Clear, operationContext);
            }

            UpdateCacheStatistics();
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
            RaiseCacheClearNotifier();
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return null;
    }

    @Override
    public CacheEntry GetGroup(Object key, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        /**
         * If we are in state transfer, we check locally first and then to make
         * sure we do a clustered call and fetch from some other functional
         * node.
         */
        CacheEntry e = Local_GetGroup(key, group, subGroup, version, lockId, lockDate, lockExpiration, accessType, operationContext);
        if ((e == null) && (_statusLatch.IsAnyBitsSet(NodeStatus.Initializing))) {
            e = Clustered_GetGroup(key, group, subGroup, lockId, lockDate, accessType);
        }

        if (e == null) {
            _stats.BumpMissCount();
        } else {
            _stats.BumpHitCount();
            // update the indexes on other nodes in the cluster
            if ((e.getExpirationHint() != null && e.getExpirationHint().getIsVariant())) { //|| (e.EvictionHint !=null && e.EvictionHint.IsVariant)
                UpdateIndices(key, true, operationContext);
            }
        }
        return e;
    }

    private CacheEntry Local_GetGroup(Object key, String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache != null) {
            return _internalCache.GetGroup(key, group, subGroup, operationContext);
        }

        return null;

    }

    /**
     * Retrieve the object from the cache for the given group or sub group.
     */
    private CacheEntry Local_GetGroup(Object key, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache != null) {
            return _internalCache.GetGroup(key, group, subGroup, version, lockId, lockDate, lockExpiration, accessType, operationContext);
        }

        return null;
    }

    /**
     * Retrieve the objects from the cluster. Used during state trasfer, when
     * the cache is loading state from other members of the cluster.
     *
     * @param keys keys of the entries.
     */
    private CacheEntry Clustered_GetGroup(Object key, String group, String subGroup, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockAccessType accessType) {
        /**
         * Fetch address of a fully functional node. There should always be one
         * fully functional node in the cluster (coordinator is alway
         * fully-functional).
         */
        Address targetNode = GetNextNode();
        if (targetNode != null) {
            return Clustered_GetGroup(targetNode, group, subGroup, lockId, lockDate, accessType);
        }
        return null;
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

    @Override
    protected java.util.HashMap Local_GetTagData(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        java.util.HashMap retVal = null;

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        retVal = _internalCache.GetTagData(tags, comparisonType, operationContext);
        return retVal;
    }

    @Override
    protected java.util.ArrayList Local_GetTagKeys(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        java.util.ArrayList retVal = null;

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        retVal = _internalCache.GetTagKeys(tags, comparisonType, operationContext);
        return retVal;
    }

    /**
     *
     * @param info
     * @return
     */
    public final java.util.Map handleGetTag(Object info) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache != null) {
            Object[] data = (Object[]) info;

            OperationContext operationContext = null;
            if (data.length > 2) {
                operationContext = (OperationContext) ((data[2] instanceof OperationContext) ? data[2] : null);
            }

            return _internalCache.GetTagData((String[]) ((data[0] instanceof String[]) ? data[0] : null), (TagComparisonType) data[1], operationContext);
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
    public CacheEntry Get(Object key, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType access, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException, TimeoutException {
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCache.Get", "");
            }
            /**
             * Wait until the object enters any running status
             */
            _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

            if (_internalCache == null) {
                throw new UnsupportedOperationException();
            }
            CacheEntry e = null;

            if (access == LockAccessType.COMPARE_VERSION || access == LockAccessType.MATCH_VERSION) {
                if (getCluster().getServers().size() > 1) {
                    e = Local_Get(key, false, operationContext);
                    if (e != null) {
                        //if the item does not exist at the speicified version.
                        if (access == LockAccessType.MATCH_VERSION) {
                            if (!e.CompareVersion(version.argvalue)) {
                                e = null;
                            }
                        } //if the item in the cache is not newer than the specified version.
                        else if (access == LockAccessType.COMPARE_VERSION) {
                            if (!e.IsNewer(version.argvalue)) {
                                e = null;
                                version.argvalue = 0L;
                            } else {
                                //set the latest version.
                                version.argvalue = e.getVersion();
                            }
                        }
                    }
                } else {
                    e = Local_Get(key, version, lockId, lockDate, lockExpiration, access, operationContext);
                }
            } else if (access == LockAccessType.ACQUIRE || access == LockAccessType.DONT_ACQUIRE) {
                if (getCluster().getServers().size() > 1) {
                    e = Local_Get(key, false, operationContext);
                    if (e != null) {
                        if (access == LockAccessType.DONT_ACQUIRE) {
                            if (e.IsItemLocked() && !e.CompareLock(lockId.argvalue)) {
                                lockId.argvalue = e.getLockId();
                                lockDate.argvalue = e.getLockDate();
                                e = null;
                            } else {
                                lockDate.argvalue = e.getLockDate(); //compare lock does not set the lockdate internally.
                            }
                        } else if (!e.IsLocked(lockId, lockDate)) {
                            if (Clustered_Lock(key, lockExpiration, lockId, lockDate, operationContext)) {
                                e = Local_Get(key, version, lockId, lockDate, lockExpiration, LockAccessType.IGNORE_LOCK, operationContext);
                            }
                        } else {
                            //dont send the entry back if it is locked.
                            e = null;
                        }
                    } else if (_statusLatch.IsAnyBitsSet(NodeStatus.Initializing)) {
                        try {
                            if (access == LockAccessType.ACQUIRE) {
                                if (Clustered_Lock(key, lockExpiration, lockId, lockDate, operationContext)) {
                                    e = Clustered_Get(key, lockId, lockDate, access, operationContext);
                                }
                            } else {
                                e = Clustered_Get(key, lockId, lockDate, access, operationContext);
                            }
                        } catch (SuspectedException suspectedException) {
                            throw new OperationFailedException(suspectedException.getMessage(), suspectedException);
                        }
                    }
                } else {
                    e = Local_Get(key, version, lockId, lockDate, lockExpiration, access, operationContext);
                }
            } else {
                if (access == LockAccessType.GET_VERSION) {
                    e = Local_Get(key, version, lockId, lockDate, lockExpiration, access, operationContext);
                } else {
                    e = Local_Get(key, operationContext);
                }

                if (e == null && _statusLatch.IsAnyBitsSet(NodeStatus.Initializing)) {
                    try {
                        e = Clustered_Get(key, lockId, lockDate, LockAccessType.IGNORE_LOCK, operationContext);
                    } catch (SuspectedException suspectedException) {
                        throw new OperationFailedException(suspectedException.getMessage(), suspectedException);
                    }
                }
            }

            if (e == null) {
                _stats.BumpMissCount();
            } else {
                _stats.BumpHitCount();
                // update the indexes on other nodes in the cluster
                if ((e.getExpirationHint() != null && e.getExpirationHint().getIsVariant())) { //|| (e.EvictionHint !=null && e.EvictionHint.IsVariant)
                    UpdateIndices(key, true, operationContext);
                    Local_Get(key, operationContext); //to update the index locally.
                }
            }
            return e;
        } catch (CacheException cacheException) {
            throw new OperationFailedException(cacheException);
        }
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
            ServerMonitor.LogClientActivity("RepCache.GetBlk", "");
        }

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap table = null;

        if (IsInStateTransfer()) {
            java.util.ArrayList dests = GetDestInStateTransfer();
            try {
                table = Clustered_Get((Address) ((dests.get(0) instanceof Address) ? dests.get(0) : null), keys, operationContext);

            } catch (SuspectedException sus) {
                throw new OperationFailedException(sus);
            }
        } else {
            table = Local_Get(keys, operationContext);
        }

        if (table != null) {

            java.util.ArrayList updateIndiceKeyList = null;
            Iterator ine = table.entrySet().iterator();
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
                    // update the indexes on other nodes in the cluster
                    if ((e.getExpirationHint() != null && e.getExpirationHint().getIsVariant())) {
                        updateIndiceKeyList.add(pair.getKey());
                    }
                }
            }
            if (updateIndiceKeyList != null && updateIndiceKeyList.size() > 0) {
                UpdateIndices(updateIndiceKeyList.toArray(new Object[0]), true, operationContext);
            }
        }

        return table;
    }

    /**
     * Retrieve the list of keys fron the cache for the given group or sub
     * group.
     */
    @Override
    protected java.util.ArrayList Local_GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
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
    protected java.util.HashMap Local_GetGroupData(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache != null) {
            return _internalCache.GetGroupData(group, subGroup, operationContext);
        }

        return null;
    }

    /**
     * Perform a dummy get operation on cluster that triggers index updates on
     * all the nodes in the cluster, has no other particular purpose.
     *
     * @param key key of the entry.
     */
    @Override
    public void UpdateIndices(Object key, OperationContext operationContext) {
        try {
            if (getCluster().getServers() != null && getCluster().getServers().size() > 1) {
                Function func = new Function(OpCodes.UpdateIndice.getValue(), new Object[]{
                    key, operationContext
                });
                getCluster().Multicast(_otherServers, func, GroupRequest.GET_ALL, false);
            }
        } catch (Exception e) {
            getContext().getCacheLog().Error("ReplicatedCache.UpdateIndices()", e.toString());
        }
    }

    @Override
    protected void UpdateIndices(Object key, boolean async, OperationContext operationContext) {
        if (_asyncReplicator != null && getCluster().getServers().size() > 1) {
            try {
                _asyncReplicator.AddUpdateIndexKey(key);
            } catch (Exception e) {
            }
        }
    }

    protected void UpdateIndices(Object[] keys, boolean async, OperationContext operationContext) {
        if (_asyncReplicator != null && getCluster().getServers().size() > 1) {
            try {
                for (Object key : keys) {
                    UpdateIndices(key, async, operationContext);
                }
            } catch (Exception e) {
            }
        }
    }

    private void RemoveUpdateIndexOperation(Object key) {
        if (key != null) {
            try {
                if (_asyncReplicator != null) {
                    _asyncReplicator.RemoveUpdateIndexKey(key);
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void ReplicateOperations(Object[] opCodes, Object[] info, Object[] userPayLoads, java.util.ArrayList compilationInfo, long seqId, long viewId) throws GeneralFailureException {
        try {
            if (getCluster().getServers() != null && getCluster().getServers().size() > 1) {
                Function func = new Function(OpCodes.ReplicateOperations.getValue(), new Object[]{
                    opCodes, info, compilationInfo
                }, false);
                func.setUserPayload(userPayLoads);
                getCluster().Multicast(_otherServers, func, GroupRequest.GET_ALL, false);
            }
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    public final Object handleReplicateOperations(Address src, Object info, Object[] userPayLoad) throws OperationFailedException {
        try {
            Object[] objs = (Object[]) info;
            Object[] opCodes = (Object[]) objs[0];
            Object[] keys = (Object[]) objs[1];
            OperationContext operationContext = null;

            for (int i = 0; i < opCodes.length; i++) {
                OpCodes opc = OpCodes.forValue(((java.lang.Byte) opCodes[i]).intValue());
                switch (opc) {
                    case UpdateIndice:
                        Object[] data = (Object[]) info;
                        if (data != null && data.length > 3) {
                            operationContext = (OperationContext) ((data[3] instanceof OperationContext) ? data[3] : null);
                        }
                        return handleUpdateIndice(new Object[]{
                            keys[i], operationContext
                        });
                }
            }
        } catch (Exception e) {
            throw new OperationFailedException(e);
        }
        return null;
    }

    /**
     * Retrieve the object from the cluster. Used during state trasfer, when the
     * cache is loading state from other members of the cluster.
     *
     * @param key key of the entry.
     */
    private CacheEntry Clustered_Get(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockAccessType accessType, OperationContext operationContext) throws CacheException, SuspectedException, TimeoutException {
        /**
         * Fetch address of a fully functional node. There should always be one
         * fully functional node in the cluster (coordinator is alway
         * fully-functional).
         */
        Address targetNode = GetNextNode();
        if (targetNode != null) {
            return Clustered_Get(targetNode, key, lockId, lockDate, accessType, operationContext);
        }
        return null;
    }

    private CacheEntry Safe_Clustered_Get(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockAccessType accessType, OperationContext operationContext) throws CacheException, SuspectedException, TimeoutException {
        try {
            return Clustered_Get(key, lockId, lockDate, accessType, operationContext);
        } catch (SuspectedException e) {
            return Clustered_Get(key, lockId, lockDate, accessType, operationContext);
        } catch (TimeoutException e) {
            return Clustered_Get(key, lockId, lockDate, accessType, operationContext);
        }

    }

    /**
     * Retrieve the object from the local cache only.
     *
     * @param key key of the entry.
     * @return cache entry.
     */
    public CacheEntry Local_Get(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, CacheException, LockingException {
        CacheEntry retVal = null;
        if (_internalCache != null) {
            retVal = _internalCache.Get(key, operationContext);
        }
        return retVal;
    }

    private CacheEntry Local_Get(Object key, boolean isUserOperation, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        Object lockId = null;
        java.util.Date lockDate = new java.util.Date();
        long version = 0;

        CacheEntry retVal = null;
        if (_internalCache != null) {
            tangible.RefObject<Long> tempRef_version = new tangible.RefObject<Long>(version);
            tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
            tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
            retVal = _internalCache.Get(key, isUserOperation, tempRef_version, tempRef_lockId, tempRef_lockDate, null, LockAccessType.IGNORE_LOCK, operationContext);
            version = tempRef_version.argvalue;
            lockId = tempRef_lockId.argvalue;
            lockDate = tempRef_lockDate.argvalue;
        }

        return retVal;
    }

    /**
     * Retrieve the object from the local cache only.
     *
     * @param key key of the entry.
     * @return cache entry.
     */
    private CacheEntry Local_Get(Object key, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockExpiration lockExpiration, LockAccessType access, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        CacheEntry retVal = null;
        if (_internalCache != null) {
            retVal = _internalCache.Get(key, version, lockId, lockDate, lockExpiration, access, operationContext);
        }
        return retVal;
    }

    /**
     * Retrieve the objects from the local cache only.
     *
     * @param keys keys of the entries.
     * @return cache entries.
     */
    private java.util.HashMap Local_Get(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        java.util.HashMap retVal = null;
        if (_internalCache != null) {
            retVal = _internalCache.Get(keys, operationContext);
        }
        return retVal;
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
            Object key = package_Renamed[0];
            String group = (String) package_Renamed[1];
            String subGroup = (String) package_Renamed[2];
            OperationContext operationContext = null;

            if (package_Renamed.length > 3) {
                operationContext = (OperationContext) ((package_Renamed[3] instanceof OperationContext) ? package_Renamed[3] : null);
            }

            return Local_GetGroup(key, group, subGroup, operationContext);
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
    private Object handleGet(Object info) throws OperationFailedException {
        try {
            Object[] objs = (Object[]) ((info instanceof Object[]) ? info : null);
            OperationContext operationContext = null;
            if (objs.length > 1) {
                operationContext = (OperationContext) ((objs[1] instanceof OperationContext) ? objs[1] : null);
            }

            if (objs[0] instanceof Object[]) {
                return Local_Get((Object[]) objs[0], operationContext);
            } else {
                CacheEntry entry = Local_Get(objs[0], operationContext);
                /*
                 * send value and entry seperaty
                 */
                OperationResponse opRes = new OperationResponse();
                if (entry != null) {
                    UserBinaryObject ubObject = (UserBinaryObject) (entry.getValue() instanceof CallbackEntry ? ((CallbackEntry) entry.getValue()).getValue() : entry.getValue());
                    opRes.UserPayload = ubObject.getData();
                    opRes.SerializablePayload = entry.CloneWithoutValue();
                }

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
     * Gets the data group info the item.
     *
     * @param key Key of the item
     * @return Data group info of the item
     */
    @Override
    public GroupInfo GetGroupInfo(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        _statusLatch.WaitForAny((byte) (NodeStatus.Running | NodeStatus.Running));

        GroupInfo info;
        info = Local_GetGroupInfo(key, operationContext);
        if (info == null && _statusLatch.getStatus().IsAnyBitSet(NodeStatus.Initializing)) {
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
    public java.util.HashMap GetGroupInfoBulk(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        _statusLatch.WaitForAny(NodeStatus.Running);

        java.util.HashMap infoTable;
        infoTable = Local_GetGroupInfoBulk(keys, operationContext);
        if ((infoTable == null || infoTable.size() < keys.length) && _statusLatch.getStatus().IsAnyBitSet(NodeStatus.Initializing)) {
            java.util.Collection result = Clustered_GetGroupInfoBulk(keys, operationContext);
            ClusteredOperationResult opRes;
            java.util.HashMap infos;
            java.util.HashMap max = null;
            if (result != null) {
                java.util.Iterator ie = result.iterator();
                while (ie.hasNext()) {
                    opRes = (ClusteredOperationResult) ie.next();
                    if (opRes != null) {
                        infos = (java.util.HashMap) opRes.getResult();
                        if (max == null) {
                            max = infos;
                        } else if (infos.size() > max.size()) {
                            max = infos;
                        }

                    }
                }
            }
            infoTable = max;
        }
        return infoTable;
    }

    /**
     * Gets the data group info the item.
     *
     * @param key Key of the item
     * @return Data group info of the item
     */
    private GroupInfo Local_GetGroupInfo(Object key, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
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
    private java.util.HashMap Local_GetGroupInfoBulk(Object[] keys, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        if (_internalCache != null) {
            return _internalCache.GetGroupInfoBulk(keys, operationContext);
        }
        return null;
    }

    /**
     * Handles the request for data group of the item. Only the coordinator of a
     * subcluter will reply.
     *
     * @param info Key(s) of the item(s)
     * @return Data group info of the item(s)
     */
    private Object handleGetGroupInfo(Object info) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        Object result = null;
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
     * Adds a pair of key and value to the cache. Throws an exception or reports
     * error if the specified key already exists in the cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     *
     * This method invokes <see cref="handleAdd"/> on every server-node in the
     * cluster. If the operation fails on any one node the whole operation is
     * considered to have failed and is rolled-back. Moreover the node
     * initiating this request (this method) also triggers a cluster-wide
     * item-add notificaion.
     *
     */
    @Override
    public CacheAddResult Add(Object key, CacheEntry cacheEntry, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("RepCache.Add_1", "");
        }

        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }
        if (getContext().getCacheLog().getIsInfoEnabled()) {
            getContext().getCacheLog().Info("Replicated.Add()", "Key = " + key);
        }

        if (Local_Contains(key, operationContext)) {
            return CacheAddResult.KeyExists;
        }
        CacheAddResult result = CacheAddResult.Success;
        Exception thrown = null;

        String taskId = null;
        if (cacheEntry.getFlag() != null && cacheEntry.getFlag().IsBitSet((byte) BitSetConstants.WriteBehind)) {
            taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }

        try {
            if (getCluster().getServers().size() > 1) {
                // Try to add to the local node and the cluster.
                result = Clustered_Add(key, cacheEntry, taskId, operationContext);
                if (result == CacheAddResult.KeyExists) {
                    return result;
                }
            } else {
                result = Local_Add(key, cacheEntry, getCluster().getLocalAddress(), taskId, true, operationContext);
            }
        } catch (Exception e) {
            thrown = e;
        }

        if (result != CacheAddResult.Success || thrown != null) {
            boolean timeout = false;
            boolean rollback = true;
            try {
                if (result == CacheAddResult.FullTimeout) {
                    timeout = true;
                    rollback = false;
                }
                if (result == CacheAddResult.PartialTimeout) {
                    timeout = true;
                }
                if (rollback) {
                    if (getCluster().getServers().size() > 1) {
                        Clustered_Remove(key, ItemRemoveReason.Removed, null, null, null, false, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                    } else {
                        Local_Remove(key, ItemRemoveReason.Removed, null, null, null, null, true, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                    }
                }
            } catch (Exception e) {
            }

            if (thrown != null) {
                throw new OperationFailedException(thrown);
            }
            if (timeout) {
                throw new com.alachisoft.tayzgrid.common.exceptions.TimeoutException("Operation timeout.");
            }
        } else {
            if (taskId != null) {
                if (result == CacheAddResult.Success) {
                    SignalTaskState(getCluster().getServers(), taskId, cacheEntry.getProviderName(), OpCode.Add, WriteBehindAsyncProcessor.TaskState.Execute, operationContext);
                } else {
                    SignalTaskState(getCluster().getServers(), taskId, cacheEntry.getProviderName(), OpCode.Add, WriteBehindAsyncProcessor.TaskState.Remove, operationContext);
                }
            }
        }

        return result;
    }

    /**
     * Add ExpirationHint against the given key
     *
     * @param key
     * @param eh
     * @return
     */
    @Override
    public boolean Add(Object key, ExpirationHint eh, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        if (getContext().getCacheLog().getIsInfoEnabled()) {
            getContext().getCacheLog().Info("Replicated.Add()", "Key = " + key);
        }

        if (Local_Contains(key, operationContext) == false) {
            return false;
        }
        boolean result = false;
        Exception thrown = null;
        try {
            if (getCluster().getServers().size() > 1) {
                // Try to add to the local node and the cluster.
                result = Clustered_Add(key, eh, operationContext);
                if (result == false) {
                    return result;
                }
            } else {
                result = Local_Add(key, eh, operationContext);
            }
        } catch (Exception e) {
            thrown = e;
        }

        return result;
    }



    /**
     * Adds a pair of key and value to the cache. Throws an exception or reports
     * error if the specified key already exists in the cache.
     *
     * @param key key of the entry.
     * @param cacheEntry the cache entry.
     * @return returns the result of operation.
     *
     * This method invokes <see cref="handleAdd"/> on every server-node in the
     * cluster. If the operation fails on any one node the whole operation is
     * considered to have failed and is rolled-back. Moreover the node
     * initiating this request (this method) also triggers a cluster-wide
     * item-add notificaion.
     *
     */
    @Override
    public java.util.HashMap Add(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("RepCache.AddBlk", "");
        }
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        Object[] failedKeys = null;
        java.util.HashMap addResult = new java.util.HashMap();
        java.util.HashMap tmp = new java.util.HashMap();
        String providerName = null;

        java.util.HashMap existingKeys = Local_Contains(keys, operationContext);
        java.util.ArrayList list = new java.util.ArrayList();
        if (existingKeys != null && existingKeys.size() > 0) {
            list = (java.util.ArrayList) ((existingKeys.get("items-found") instanceof java.util.ArrayList) ? existingKeys.get("items-found") : null);
        }

        int failCount = list.size();
        if (failCount > 0) {
            java.util.Iterator ie = list.iterator();
            while (ie.hasNext()) {
                addResult.put(ie.next(), new OperationFailedException("The specified key already exists."));
            }

            // all keys failed, so return.
            if (failCount == keys.length) {
                return addResult;
            }

            Object[] newKeys = new Object[keys.length - failCount];
            CacheEntry[] entries = new CacheEntry[keys.length - failCount];

            int i = 0;
            int j = 0;

            java.util.Iterator im = Arrays.asList(keys).iterator();
            while (im.hasNext()) {
                Object key = im.next();
                if (!list.contains(key)) {
                    newKeys[j] = key;
                    entries[j] = cacheEntries[i];
                    j++;
                }
                i++;
            }

            keys = newKeys;
            cacheEntries = entries;
        }

        String taskId = null;
        if (cacheEntries[0].getFlag() != null && cacheEntries[0].getFlag().IsBitSet((byte) BitSetConstants.WriteBehind)) {
            taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }

        Exception thrown = null;
        try {
            if (getCluster().getServers().size() > 1) {
                // Try to add to the local node and the cluster.
                tmp = Clustered_Add(keys, cacheEntries, taskId, operationContext);
            } else {
                tmp = Local_Add(keys, cacheEntries, getCluster().getLocalAddress(), taskId, true, operationContext);
            }
        } catch (Exception inner) {
            getContext().getCacheLog().Error("Replicated.Clustered_Add()", inner.toString());
            for (int i = 0; i < keys.length; i++) {
                tmp.put(keys[i], new OperationFailedException(inner.getMessage(), inner));
            }
            thrown = inner;
        }

        if (thrown != null) {
            if (getCluster().getServers().size() > 1) {
                Clustered_Remove(keys, ItemRemoveReason.Removed, operationContext);
            } else {
                Local_Remove(keys, ItemRemoveReason.Removed, null, null, null, null, false, operationContext);
            }
        } else {
            failCount = 0;
            java.util.ArrayList failKeys = new java.util.ArrayList();
            Iterator ide = tmp.entrySet().iterator();
            java.util.HashMap writeBehindTable = new java.util.HashMap();

            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                if (pair.getValue() instanceof CacheAddResult) {
                    CacheAddResult res = (CacheAddResult) pair.getValue();
                    switch (res) {
                        case Failure:
                        case KeyExists:
                        case NeedsEviction:
                            failCount++;
                            failKeys.add(pair.getKey());
                            addResult.put(pair.getKey(), pair.getValue());
                            break;

                        case Success:
                            addResult.put(pair.getKey(), pair.getValue());
                            writeBehindTable.put(pair.getKey(), null);
                            break;
                    }
                } else { //it means value is exception
                    failCount++;
                    failKeys.add(pair.getKey());
                    addResult.put(pair.getKey(), pair.getValue());
                }
            }

            if (failCount > 0) {
                Object[] keysToRemove = new Object[failCount];
                System.arraycopy(failKeys.toArray(), 0, keysToRemove, 0, failKeys.size());

                if (getCluster().getServers().size() > 1) {
                    Clustered_Remove(keysToRemove, ItemRemoveReason.Removed, null, null, null, false, operationContext);
                } else {
                    Local_Remove(keysToRemove, ItemRemoveReason.Removed, null, null, null, null, false, operationContext);
                }
            }

            if (taskId != null) {
                if (writeBehindTable.size() > 0) {
                    SignalBulkTaskState(getCluster().getServers(), taskId, cacheEntries[0].getProviderName(), addResult, OpCode.Add, WriteBehindAsyncProcessor.TaskState.Execute);
                } else {
                    SignalTaskState(getCluster().getServers(), taskId, cacheEntries[0].getProviderName(), OpCode.Add, WriteBehindAsyncProcessor.TaskState.Remove, operationContext);
                }
            }
        }
        return addResult;
    }

    /**
     * Add the object to the local cache.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * Does an Add locally, however the generated notification is discarded,
     * since it is specially handled in <see cref="Add"/>.
     *
     */
    private CacheAddResult Local_Add(Object key, CacheEntry cacheEntry, Address src, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException {
        CacheAddResult retVal = CacheAddResult.Failure;

        CacheEntry clone = null;
        if (taskId != null && cacheEntry.getHasQueryInfo()) {
            clone = (CacheEntry) cacheEntry.clone();
        } else {
            clone = cacheEntry;
        }

        if (_internalCache != null) {
            Object[] keys = null;
            try {
                
                retVal = _internalCache.Add(key, cacheEntry, notify, operationContext);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
           
        }

        if (retVal == CacheAddResult.Success && taskId != null) {
            AddWriteBehindTask(src,  key, clone, taskId, OpCode.Add, operationContext);
        }

        return retVal;
    }

    /**
     * Add the ExpirationHint against the given key
     *
     * @param key
     * @param hint
     * @return
     */
    private boolean Local_Add(Object key, ExpirationHint hint, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        boolean retVal = false;
        if (_internalCache != null) {
            CacheEntry cacheEntry = new CacheEntry();
            cacheEntry.setExpirationHint(hint);
         

            try {
                retVal = _internalCache.Add(key, hint, operationContext);
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }

            
        }
        return retVal;
    }



    /**
     * Add the object to the local cache.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * Does an Add locally, however the generated notification is discarded,
     * since it is specially handled in <see cref="Add"/>.
     *
     */
    private java.util.HashMap Local_Add(Object[] keys, CacheEntry[] cacheEntries, Address src, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException, OperationFailedException, LockingException, GeneralFailureException, CacheException {
        java.util.HashMap table = new java.util.HashMap();

        java.util.ArrayList goodKeysList = new java.util.ArrayList();
        java.util.ArrayList goodEntriesList = new java.util.ArrayList();

        java.util.ArrayList badKeysList = new java.util.ArrayList();

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
            table = _internalCache.Add(keys, cacheEntries, notify, operationContext);
        }

        if (taskId != null && table != null) {
            java.util.HashMap successfullEntries = new java.util.HashMap();
            for (int i = 0; i < keys.length; i++) {
                Object value = table.get(keys[i]);
                if (value instanceof CacheAddResult && (CacheAddResult) value == CacheAddResult.Success) {
                    successfullEntries.put(keys[i], clone[i]);
                }
            }
            if (successfullEntries.size() > 0) {
                AddWriteBehindTask(src, successfullEntries, null, taskId, OpCode.Add);
            }
        }

        return table;
    }

    /**
     * Add the object to the cluster.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method invokes <see cref="handleAdd"/> on every server-node in the
     * cluster. If the operation fails on any one node the whole operation is
     * considered to have failed and is rolled-back.
     *
     */
    private CacheAddResult Clustered_Add(Object key, CacheEntry cacheEntry, String taskId, OperationContext operationContext) throws CacheException {
        return Clustered_Add(getCluster().getServers(), key, cacheEntry, taskId, operationContext);
    }

    /**
     * Add the ExpirationHint to the cluster.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method invokes <see cref="handleAddHint"/> on every server-node in
     * the cluster. If the operation fails on any one node the whole operation
     * is considered to have failed and is rolled-back.
     *
     */
    private boolean Clustered_Add(Object key, ExpirationHint eh, OperationContext operationContext) throws CacheException {
        return Clustered_Add(getCluster().getServers(), key, eh, operationContext);
    }



    /**
     * Add the object to the cluster.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method invokes <see cref="handleAdd"/> on every server-node in the
     * cluster. If the operation fails on any one node the whole operation is
     * considered to have failed and is rolled-back.
     *
     */
    private java.util.HashMap Clustered_Add(Object[] keys, CacheEntry[] cacheEntries, String taskId, OperationContext operationContext) throws CacheException {
        return Clustered_Add(getCluster().getServers(), keys, cacheEntries, taskId, operationContext);
    }

    private Object handleUpdateIndice(Object key) throws OperationFailedException {
        //we do a get operation on the item so that its relevent index in epxiration/eviction
        //is updated.
        handleGet(key);
        return null;
    }

    /**
     * Hanlde cluster-wide Add(key) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleAdd(Address src, Object info, Object[] userPayload) throws OperationFailedException {
        try {
            Object[] objs = (Object[]) info;

            String taskId = null;
            OperationContext oc = null;

            if (objs.length > 2) {
                taskId = objs[2] != null ? (String) ((objs[2] instanceof String) ? objs[2] : null) : null;
            }

            if (objs.length > 3) {
                oc = (OperationContext) ((objs[3] instanceof OperationContext) ? objs[3] : null);
            }

            if (objs[0] instanceof Object[]) {
                Object[] keys = (Object[]) objs[0];
                CacheEntry[] entries = (CacheEntry[]) ((objs[1] instanceof CacheEntry[]) ? objs[1] : null);
                if (objs.length > 2) {
                    taskId = objs[2] != null ? (String) ((objs[2] instanceof String) ? objs[2] : null) : null;
                }

                java.util.HashMap results = Local_Add(keys, entries, src, taskId, true, oc);

                return results;
            } else {
                CacheAddResult result = CacheAddResult.Failure;
                Object key = objs[0];
                CacheEntry e = (CacheEntry) ((objs[1] instanceof CacheEntry) ? objs[1] : null);
                e.setValue(userPayload);

                {
                    result = Local_Add(key, e, src, taskId, true, oc);
                }

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
     * Hanlde cluster-wide AddHint(key) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleAddHint(Address src, Object info) throws OperationFailedException {
        try {
            Object[] objs = (Object[]) info;
            Object key = objs[0];
            ExpirationHint eh = (ExpirationHint) ((objs[1] instanceof ExpirationHint) ? objs[1] : null);
            OperationContext oc = (OperationContext) ((objs[2] instanceof OperationContext) ? objs[2] : null);

            if (src.compareTo(getLocalAddress()) != 0) {
                if (eh != null && !eh.getIsRoutable()) {
                    NodeExpiration expiry = new NodeExpiration(src);
                    return Local_Add(key, expiry, oc);
                }
            }
            return Local_Add(key, eh, oc);
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return false;
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
     * This method invokes <see cref="handleInsert"/> on every server-node in
     * the cluster. If the operation fails on any one node the whole operation
     * is considered to have failed and is rolled-back. Moreover the node
     * initiating this request (this method) also triggers a cluster-wide
     * item-update notificaion.
     *
     */
    @Override
    public CacheInsResultWithEntry Insert(Object key, CacheEntry cacheEntry, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("RepCache.Insert", "");
        }
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        if (getContext().getCacheLog().getIsInfoEnabled()) {
            getContext().getCacheLog().Info("Replicated.Insert()", "Key = " + key);
        }

        CacheEntry pEntry = null;
        CacheInsResultWithEntry retVal = new CacheInsResultWithEntry();
        Exception thrown = null;

        String taskId = null;
        if (cacheEntry.getFlag() != null && cacheEntry.getFlag().IsBitSet((byte) BitSetConstants.WriteBehind)) {
            taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }

        try {
            // We get the actual item to raise custom call back with the item.
            //Get internally catters for the state-transfer scenarios.
            pEntry = Get(key, operationContext);
            retVal.setEntry(pEntry);

            if (pEntry != null) {
                if (accessType != LockAccessType.IGNORE_LOCK) {
                    if (accessType == LockAccessType.COMPARE_VERSION) {
                        if (!pEntry.CompareVersion(version)) {
                            retVal.setEntry(null);
                            retVal.setResult(CacheInsResult.VersionMismatch);
                            return retVal;
                        }
                        accessType = LockAccessType.IGNORE_LOCK;
                    } else {
                        if (pEntry.IsItemLocked() && !pEntry.CompareLock(lockId)) {
                            retVal.setEntry(null);
                            retVal.setResult(CacheInsResult.ItemLocked);
                            return retVal;
                        }
                    }
                }

                GroupInfo oldInfo = pEntry.getGroupInfo();
                GroupInfo newInfo = cacheEntry.getGroupInfo();

                if (!CacheHelper.CheckDataGroupsCompatibility(newInfo, oldInfo)) {
                    throw new Exception("Data group of the inserted item does not match the existing item's data group.");
                }
            }
            if (getCluster().getServers().size() > 1) {
                // Try to add to the local node and the cluster.
                retVal = Clustered_Insert(key, cacheEntry, taskId, lockId, accessType, operationContext);

                //if coordinator has sent the previous entry, use that one...
                //otherwise send back the localy got previous entry...
                if (retVal.getEntry() != null) {
                    pEntry = retVal.getEntry();
                } else {
                    retVal.setEntry(pEntry);
                }
            } else {
                retVal = Local_Insert(key, cacheEntry, getCluster().getLocalAddress(), taskId, true, lockId, version, accessType, operationContext);
            }
        } catch (Exception e) {
            thrown = e;
        }

        // Try to insert to the local node and the cluster.
        if ((retVal.getResult() == CacheInsResult.NeedsEviction || retVal.getResult() == CacheInsResult.Failure || retVal.getResult() == CacheInsResult.FullTimeout
                || retVal.getResult() == CacheInsResult.PartialTimeout) || thrown != null) {
            getContext().getCacheLog().Warn("Replicated.Insert()", "rolling back, since result was " + retVal.getResult());
            boolean rollback = true;
            boolean timeout = false;
            if (retVal.getResult() == CacheInsResult.PartialTimeout) {
                timeout = true;
            } else if (retVal.getResult() == CacheInsResult.FullTimeout) {
                timeout = true;
                rollback = false;
            }

            if (rollback) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException interruptedException) {
                }
                /**
                 * failed on the cluster, so remove locally as well.
                 */
                if (getCluster().getServers().size() > 1) {
                    Clustered_Remove(key, ItemRemoveReason.Removed, null, null, null, false, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                }
            }
            if (timeout) {

                throw new OperationFailedException("operation failed because the group member was suspected");

            }
            if (thrown != null) {
                throw new OperationFailedException(thrown);
            }
        }

        if (notify && retVal.getResult() == CacheInsResult.SuccessOverwrite) {

            RemoveUpdateIndexOperation(key);
        }

        if (taskId != null) {
            if (retVal.getResult() == CacheInsResult.Success || retVal.getResult() == CacheInsResult.SuccessOverwrite) {
                SignalTaskState(getCluster().getServers(), taskId, cacheEntry.getProviderName(), OpCode.Update, WriteBehindAsyncProcessor.TaskState.Execute, operationContext);
            } else {
                SignalTaskState(getCluster().getServers(), taskId, cacheEntry.getProviderName(), OpCode.Update, WriteBehindAsyncProcessor.TaskState.Remove, operationContext);
            }
        }
        return retVal;
    }

    /**
     * Adds key and value pairs to the cache. If any of the specified key
     * already exists in the cache; it is updated, otherwise a new item is added
     * to the cache.
     *
     * @param keys keys of the entries.
     * @param cacheEntries the cache entries.
     * @return the list of keys that failed to add.
     *
     * This method invokes <see cref="handleInsert"/> on every server-node in
     * the cluster. If the operation fails on any one node the whole operation
     * is considered to have failed and is rolled-back. Moreover the node
     * initiating this request (this method) also triggers a cluster-wide
     * item-update notificaion.
     *
     */
    @Override
    public java.util.HashMap Insert(Object[] keys, CacheEntry[] cacheEntries, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("RepCache.InsertBlk", "");
        }
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap insertResults = null;

        String taskId = null;
        if (cacheEntries[0].getFlag() != null && cacheEntries[0].getFlag().IsBitSet((byte) BitSetConstants.WriteBehind)) {
            taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }

        if (getCluster().getServers().size() > 1) {
            insertResults = Clustered_Insert(keys, cacheEntries, taskId, notify, operationContext);
        } else {
            java.util.HashMap pEntries = null;

            pEntries = Get(keys, operationContext); //dont remove

            java.util.HashMap existingItems;
            java.util.HashMap jointTable = new java.util.HashMap();
            java.util.HashMap failedTable = new java.util.HashMap();
            java.util.HashMap insertable = new java.util.HashMap();
            java.util.ArrayList inserted = new java.util.ArrayList();
            java.util.ArrayList added = new java.util.ArrayList();

            ClusteredOperationResult opResult;
            Object[] validKeys;
            Object[] failedKeys;
            CacheEntry[] validEnteries;
            int index = 0;
            Object key;
            Address node = null;

            for (int i = 0; i < keys.length; i++) {
                jointTable.put(keys[i], cacheEntries[i]);
            }

            existingItems = Local_GetGroupInfoBulk(keys, operationContext);
            if (existingItems != null && existingItems.size() > 0) {
                insertable = CacheHelper.GetInsertableItems(existingItems, jointTable);
                Iterator ide;
                if (insertable != null) {
                    index = 0;
                    validKeys = new Object[insertable.size()];
                    validEnteries = new CacheEntry[insertable.size()];
                    ide = insertable.entrySet().iterator();
                    while (ide.hasNext()) {
                        Map.Entry pair = (Map.Entry) ide.next();
                        key = pair.getKey();
                        validKeys[index] = key;
                        validEnteries[index] = (CacheEntry) pair.getValue();
                        jointTable.remove(key);
                        inserted.add(key);
                        index += 1;
                    }

                    if (validKeys.length > 0) {
                        try {
                            insertResults = Local_Insert(validKeys, validEnteries, getCluster().getLocalAddress(), taskId, true, operationContext);
                        } catch (Exception e) {
                            getContext().getCacheLog().Error("PartitionedServerCache.Insert(Keys)", e.toString());
                            for (int i = 0; i < validKeys.length; i++) {
                                failedTable.put(validKeys[i], e);
                                inserted.remove(validKeys[i]);
                            }
                            Clustered_Remove(validKeys, ItemRemoveReason.Removed, null, null, null, false, operationContext);
                        }

                        if (insertResults != null) {
                            Iterator ie = insertResults.entrySet().iterator();
                            while (ie.hasNext()) {
                                Map.Entry pair = (Map.Entry) ie.next();
                                if (pair.getValue() instanceof CacheInsResultWithEntry) {
                                    CacheInsResultWithEntry res = (CacheInsResultWithEntry) ((pair.getValue() instanceof CacheInsResultWithEntry) ? pair.getValue() : null);
                                    switch (res.getResult()) {
                                        case Failure:
                                            failedTable.put(pair.getKey(), new OperationFailedException("Generic operation failure; not enough information is available."));
                                            break;
                                        case NeedsEviction:
                                            failedTable.put(pair.getKey(), new OperationFailedException("The cache is full and not enough items could be evicted."));
                                            break;
                                        case IncompatibleGroup:
                                            failedTable.put(pair.getKey(), new OperationFailedException("Data group of the inserted item does not match the existing item's data group"));
                                            break;
                                        case DependencyKeyNotExist:
                                            failedTable.put(pair.getKey(), new OperationFailedException("One of the dependency keys does not exist."));
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
                ide = existingItems.entrySet().iterator();
                while (ide.hasNext()) {
                    Map.Entry pair = (Map.Entry) ide.next();
                    key = pair.getKey();
                    if (jointTable.containsKey(key)) {
                        failedTable.put(key, new OperationFailedException("Data group of the inserted item does not match the existing item's data group"));
                        jointTable.remove(key);
                    }
                }
            }

            java.util.HashMap localInsertResult = null;
            if (jointTable.size() > 0) {
                index = 0;
                validKeys = new Object[jointTable.size()];
                validEnteries = new CacheEntry[jointTable.size()];
                added = new java.util.ArrayList();
                Iterator ide = jointTable.entrySet().iterator();
                while (ide.hasNext()) {
                    Map.Entry pair = (Map.Entry) ide.next();
                    key = pair.getKey();
                    validKeys[index] = key;
                    validEnteries[index] = (CacheEntry) pair.getValue();
                    added.add(key);
                    index += 1;
                }
                localInsertResult = Local_Insert(validKeys, validEnteries, getCluster().getLocalAddress(), taskId, notify, operationContext);
            }

            if (localInsertResult != null) {
                Iterator ide = localInsertResult.entrySet().iterator();
                CacheInsResultWithEntry result = null;
                while (ide.hasNext()) {
                    Map.Entry pair = (Map.Entry) ide.next();
                    result = (CacheInsResultWithEntry) pair.getValue();

                    if (result.getResult() == CacheInsResult.NeedsEviction) {
                        failedTable.put(pair.getKey(), new OperationFailedException("The cache is full and not enough items could be evicted."));
                        added.remove(pair.getKey());
                    } else if (result.getResult() == CacheInsResult.DependencyKeyNotExist) {
                        failedTable.put(pair.getKey(), new OperationFailedException("One of the dependency keys does not exist."));
                        added.remove(pair.getKey());
                    } else if (result.getResult() == CacheInsResult.DependencyKeyNotExist) {
                        failedTable.put(pair.getKey(), new OperationFailedException("Error setting up key dependency."));
                        added.remove(pair.getKey());
                    }
                }
            }

            if (notify) {
                java.util.Iterator ideInsterted = inserted.iterator();
                while (ideInsterted.hasNext()) {
                    key = ideInsterted.next();

                    RemoveUpdateIndexOperation(key);
                }
            }


            insertResults = failedTable;
        }

        if (taskId != null && insertResults != null) {
            java.util.HashMap writeBehindTable = new java.util.HashMap();
            for (int i = 0; i < keys.length; i++) {
                if (!insertResults.containsKey(keys[i])) {
                    writeBehindTable.put(keys[i], cacheEntries[i]);
                }
            }

            if (writeBehindTable.size() > 0) {
                super.SignalBulkTaskState(getCluster().getServers(), taskId, cacheEntries[0].getProviderName(), writeBehindTable, OpCode.Update, WriteBehindAsyncProcessor.TaskState.Execute);
            } else {
                super.SignalTaskState(getCluster().getServers(), taskId, cacheEntries[0].getProviderName(), OpCode.Update, WriteBehindAsyncProcessor.TaskState.Remove, operationContext);
            }
        }

        return insertResults;
    }

    /**
     * Adds a pair of key and value to the cache. If the specified key already
     * exists in the cache; it is updated, otherwise a new item is added to the
     * cache.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * Does an Insert locally, however the generated notification is discarded,
     * since it is specially handled in <see cref="Insert"/>.
     *
     */
    private CacheInsResultWithEntry Local_Insert(Object key, CacheEntry cacheEntry, Address src, String taskId, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException {
        CacheInsResultWithEntry retVal = new CacheInsResultWithEntry();

        CacheEntry clone = null;
        if (taskId != null && cacheEntry.getHasQueryInfo()) {
            clone = (CacheEntry) cacheEntry.clone();
        } else {
            clone = cacheEntry;
        }

        try {
            if (_internalCache != null) {

                retVal = _internalCache.Insert(key, cacheEntry, notify, lockId, version, accessType, operationContext);
            }
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }

        if (retVal != null && (retVal.getResult() == CacheInsResult.Success || retVal.getResult() == CacheInsResult.SuccessOverwrite) && taskId != null) {
            AddWriteBehindTask(src, key, clone, taskId, OpCode.Update, operationContext);
        }

        return retVal;
    }

    /**
     * Adds key and value pairs to the cache. If any of the specified key
     * already exists in the cache; it is updated, otherwise a new item is added
     * to the cache.
     *
     * @param keys keys of the entries.
     * @return cache entries.
     *
     * Does an Insert locally, however the generated notification is discarded,
     * since it is specially handled in <see cref="Insert"/>.
     *
     */
    private java.util.HashMap Local_Insert(Object[] keys, CacheEntry[] cacheEntries, Address src, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException {
        java.util.HashMap retVal = null;
        Exception thrown = null;

        java.util.HashMap badEntriesTable = new java.util.HashMap();
        java.util.ArrayList goodKeysList = new java.util.ArrayList();
        java.util.ArrayList goodEntriesList = new java.util.ArrayList();
        java.util.ArrayList badKeysList = new java.util.ArrayList();

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

        try {
            if (_internalCache != null) {            
                retVal = _internalCache.Insert(keys, cacheEntries, notify, operationContext);
            }
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }

        if (taskId != null && retVal != null) {
            java.util.HashMap successfullEntries = new java.util.HashMap();
            for (int i = 0; i < keys.length; i++) {
                Object value = retVal.get(keys[i]);
                if (value instanceof CacheInsResultWithEntry && (((CacheInsResultWithEntry) value).getResult() == CacheInsResult.Success
                        || ((CacheInsResultWithEntry) value).getResult() == CacheInsResult.SuccessOverwrite)) {
                    successfullEntries.put(keys[i], clone[i]);
                }
            }

            if (successfullEntries.size() > 0) {
                AddWriteBehindTask(src, successfullEntries, null, taskId, OpCode.Update);
            }
        }
        return retVal;
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
    private CacheInsResultWithEntry Clustered_Insert(Object key, CacheEntry cacheEntry, String taskId, Object lockId, LockAccessType accesssType, OperationContext operationContext) throws CacheException {
        return Clustered_Insert(getCluster().getServers(), key, cacheEntry, taskId, lockId, accesssType, operationContext);
    }

    /**
     * Add the objects to the cluster.
     *
     * @param keys
     * @param cacheEntries
     * @return
     *
     * This method invokes <see cref="handleInsert"/> on every server-node in
     * the cluster. If the operation fails on any one node the whole operation
     * is considered to have failed and is rolled-back.
     *
     */
    @Override
    protected java.util.HashMap Clustered_Insert(Object[] keys, CacheEntry[] cacheEntries, OperationContext operationContext) throws CacheException {
        return Clustered_Insert(getCluster().getServers(), keys, cacheEntries, null, operationContext);
    }

    /**
     * Adds a pair of key and value to the cache. If the specified key already
     * exists in the cache; it is updated, otherwise a new item is added to the
     * cache.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     */
    private Object handleInsert(Address src, Object info, Object[] userPayload) throws OperationFailedException {
        try {
            Object[] objs = (Object[]) info;
            boolean returnEntry = false;
            String taskId = null;
            OperationContext operationContext = null;

            if (objs.length > 2) {
                taskId = objs[2] != null ? (String) ((objs[2] instanceof String) ? objs[2] : null) : null;
            }

            if (objs.length == 3) {
                operationContext = (OperationContext) ((objs[2] instanceof OperationContext) ? objs[2] : null);
            } else if (objs.length == 4) {
                operationContext = (OperationContext) ((objs[3] instanceof OperationContext) ? objs[3] : null);
            }

            //if client node is requesting for the previous cache entry
            //then cluster coordinator must send it back...
            if (objs.length == 7) {
                returnEntry = (Boolean) objs[3] && getCluster().getIsCoordinator();
                operationContext = (OperationContext) ((objs[6] instanceof OperationContext) ? objs[6] : null);
            }

            if (objs[0] instanceof Object[]) {
                Object[] keys = (Object[]) objs[0];
                CacheEntry[] entries = (CacheEntry[]) ((objs[1] instanceof CacheEntry[]) ? objs[1] : null);
                return Local_Insert(keys, entries, src, taskId, true, operationContext);
            } else {
                Object key = objs[0];
                CacheEntry e = (CacheEntry) ((objs[1] instanceof CacheEntry) ? objs[1] : null);
                e.setValue(userPayload);
                Object lockId = null;
                LockAccessType accessType = LockAccessType.IGNORE_LOCK;
                long version = 0;
                if (objs.length == 7) {
                    lockId = objs[4];
                    accessType = (LockAccessType) objs[5];
                }
                CacheInsResultWithEntry resultWithEntry = Local_Insert(key, e, src, taskId, true, lockId, version, accessType, operationContext);

                /*
                 * send value and entry seperately
                 */
                OperationResponse opRes = new OperationResponse();
                if (resultWithEntry.getEntry() != null) {
                    if (resultWithEntry.getEntry().getValue() instanceof CallbackEntry) {
                        opRes.UserPayload = null;
                    } else {
                        //we need not to send this entry back... it is needed only for custom notifications and/or key dependencies...
                        
                        opRes.UserPayload = null;
                    }

                    if (returnEntry) {
                        CacheEntry tempVar = resultWithEntry.getEntry().CloneWithoutValue();
                        {
                            resultWithEntry.setEntry((CacheEntry) ((tempVar instanceof CacheEntry) ? tempVar : null));
                        }
                    } else {
                        resultWithEntry.setEntry(null);
                    }
                }

                opRes.SerializablePayload = resultWithEntry;
                return opRes;
            }
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e);
            }
        }
        return CacheInsResult.Failure;
    }

    private Object handleGetKeysByTag(Object info) throws GeneralFailureException, OperationFailedException, CacheException, LockingException, LockingException {
        if (_internalCache != null) {
            Object[] data = (Object[]) info;
            java.util.ArrayList keys = _internalCache.GetTagKeys((String[]) ((data[0] instanceof String[]) ? data[0] : null), (TagComparisonType) data[1], (OperationContext) ((data[2] instanceof OperationContext) ? data[2] : null));
            return keys;
        }

        return null;
    }

    @Override
    public Object RemoveSync(Object[] keys, ItemRemoveReason reason, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, TimeoutException {
        Object result = null;
        try {
            if (getCluster().getServers().size() > 1) {
                result = Clustered_Remove(keys, reason, operationContext);
            } else {
                result = handleRemoveRange(new Object[]{
                    keys, reason, operationContext
                });
            }
        } catch (TimeoutException e) {
            //we retry the operation.
            try {
                Thread.sleep(2000L);
            } catch (Exception exc) {
            }
            if (getCluster().getServers().size() > 1) {
                result = Clustered_Remove(keys, reason, operationContext);
            } else {
                result = handleRemoveRange(new Object[]{
                    keys, reason, operationContext
                });
            }
        } catch (Exception e) {
            getContext().getCacheLog().Error("ReplicatedCache.RemoveSync", e.toString());
            throw new OperationFailedException(e);
        }
        return result;
    }

    /**
     * Removes the object and key pair from the cache. The key is specified as
     * parameter.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * Remove notifications in a repicated cluster are handled differently. If
     * there is an explicit request for Remove, the node initiating the request
     * triggers the notifications. Expirations and Evictions are replicated and
     * again the node initiating the replication triggers the cluster-wide
     * notification.
     *
     */
    @Override
    public CacheEntry Remove(Object key, ItemRemoveReason ir, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("RepCache.Remove", "");
        }
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

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

        CacheEntry e = null;

        if (accessType == LockAccessType.COMPARE_VERSION) {
            e = Get(key, operationContext);
            if (e != null) {
                if (!e.CompareVersion(version)) {
                    throw new LockingException("Item in the cache does not exist at the specified version.");
                }
            }
        } else if (accessType != LockAccessType.IGNORE_LOCK) {
            //Get internally catters for the state-transfer scenarios.
            e = Get(key, operationContext);
            if (e != null) {
                if (e.IsItemLocked() && !e.CompareLock(lockId)) {
                    //this exception directly goes to user.
                    throw new LockingException("Item is locked.");
                }
            }
        }

        String taskId = null;
        if (updateOptions == DataSourceUpdateOptions.WriteBehind) {
            taskId = getCluster().getLocalAddress().toString() + ":" + (new Integer(NextSequence())).toString();
        }
        try {
            if (getCluster().getServers().size() > 1) {
                e = Clustered_Remove(actualKey, ir, cbEntry, taskId, providerName, false, lockId, version, accessType, operationContext);
            } else {
                e = Local_Remove(actualKey, ir, getCluster().getLocalAddress(), cbEntry, taskId, providerName, true, lockId, version, accessType, operationContext);
            }
        } catch (TimeoutException ex) {
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException interruptedException) {
            }

            if (getCluster().getServers().size() > 1) {
                e = Clustered_Remove(actualKey, ir, cbEntry, taskId, providerName, false, lockId, version, accessType, operationContext);
            } else {
                e = Local_Remove(actualKey, ir, getCluster().getLocalAddress(), cbEntry, taskId, providerName, true, lockId, version, accessType, operationContext);
            }
        }

        if (e != null && notify) {
            RemoveUpdateIndexOperation(key);
        }

        if (taskId != null) {
            if (e != null) {
                SignalTaskState(getCluster().getServers(), taskId, providerName, OpCode.Remove, WriteBehindAsyncProcessor.TaskState.Execute, operationContext);
            }
        }

        return e;
    }

    /**
     * Removes the objects and key pairs from the cache. The keys are specified
     * as parameter.
     *
     * @param keys keys of the entries.
     * @return keys that actually removed from the cache
     *
     * Remove notifications in a repicated cluster are handled differently. If
     * there is an explicit request for Remove, the node initiating the request
     * triggers the notifications. Expirations and Evictions are replicated and
     * again the node initiating the replication triggers the cluster-wide
     * notification.
     *
     */
    @Override
    public java.util.HashMap Remove(Object[] keys, ItemRemoveReason ir, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("RepCache.RemoveBlk", "");
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
        java.util.HashMap writeBehindTable = new java.util.HashMap();
        String providerName = null;

        if (keys[0] instanceof Object[]) {
            Object[] package_Renamed = (Object[]) ((keys[0] instanceof Object[]) ? keys[0] : null);
            keys[0] = package_Renamed[0];
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

        java.util.HashMap removed = null;
        if (getCluster().getServers().size() > 1) {
            removed = Clustered_Remove(keys, ir, cbEntry, taskId, providerName, false, operationContext);
        } else {
            removed = Local_Remove(keys, ir, getCluster().getLocalAddress(), cbEntry, taskId, providerName, true, operationContext);
        }

        if (removed.size() > 0) {
            Iterator ide = removed.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                Object key = pair.getKey();
                CacheEntry e = (CacheEntry) pair.getValue();
                if (e != null) {
                    RemoveUpdateIndexOperation(pair.getKey());
                    writeBehindTable.put(pair.getKey(), pair.getValue());
                }
            }
        }

        if (taskId != null && writeBehindTable.size() > 0) {
            SignalBulkTaskState(getCluster().getServers(), taskId, null, writeBehindTable, OpCode.Remove, WriteBehindAsyncProcessor.TaskState.Execute);
        } else if (taskId != null && writeBehindTable.isEmpty()) {
            SignalBulkTaskState(getCluster().getServers(), taskId, null, writeBehindTable, OpCode.Remove, WriteBehindAsyncProcessor.TaskState.Remove);
        }

        return removed;
    }

    /**
     * Remove the object from the local cache only.
     *
     * @param key key of the entry.
     * @param ir
     * @param notify
     * @return cache entry.
     */
    private CacheEntry Local_Remove(Object key, ItemRemoveReason ir, Address src, CallbackEntry cbEntry, String taskId, String providerName, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        CacheEntry retVal = null;
        if (_internalCache != null) {
            retVal = _internalCache.Remove(key, ir, notify, lockId, version, accessType, operationContext);
         
        }

        if (retVal != null && taskId != null) {
            CacheEntry cloned = retVal;
            cloned.setProviderName(providerName);
            if (cbEntry != null) {
                Object tempVar = retVal.clone();
                cloned = (CacheEntry) ((tempVar instanceof CacheEntry) ? tempVar : null);
                cloned.setProviderName(providerName);
                if (cloned.getValue() instanceof CallbackEntry) {
                    ((CallbackEntry) cloned.getValue()).setWriteBehindOperationCompletedCallback(cbEntry.getWriteBehindOperationCompletedCallback());
                } else {
                    cbEntry.setValue(cloned.getValue());
                    cloned.setValue(cbEntry);
                }
            }
            super.AddWriteBehindTask(src, key, cloned, taskId, OpCode.Remove, operationContext);
        }

        return retVal;
    }

    

    /**
     * Remove the objects from the local cache only.
     *
     * @param keys keys of the entries.
     * @param ir
     * @param notify
     * @return keys and values that actualy removed from the cache
     */
    private java.util.HashMap Local_Remove(Object[] keys, ItemRemoveReason ir, Address src, CallbackEntry cbEntry, String taskId, String providerName, boolean notify, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException {
        java.util.HashMap retVal = null;
        java.util.HashMap writeBehindTable = new java.util.HashMap();
        if (_internalCache != null) {
            retVal = _internalCache.Remove(keys, ir, notify, operationContext);

            for (int i = 0; i < keys.length; i++) {
                if (retVal.get(keys[i]) instanceof CacheEntry) {
                    CacheEntry entry = (CacheEntry) retVal.get(keys[i]);
                    entry.setProviderName(providerName);
                   
                    if (taskId != null) {
                        writeBehindTable.put(keys[i], entry);
                    }
                }
            }
        }

        if (writeBehindTable.size() > 0 && taskId != null) {
            AddWriteBehindTask(src, writeBehindTable, cbEntry, taskId, OpCode.Remove);
        }

        return retVal;
    }

    @Override
    protected java.util.HashMap Local_RemoveTag(String[] tags, TagComparisonType tagComparisonType, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
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
     */
    @Override
    protected java.util.HashMap Local_RemoveGroup(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
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

    /**
     * Hanlde cluster-wide Remove(key) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     *
     * Removes an item locally; however without generating notifications.
     * <p>
     * <b>Note:</b> When a client invokes <see cref="handleRemove"/>; it is the
     * clients reponsibility to actaully initiate the notification. </p>
     *
     */
    private Object handleRemove(Address src, Object info) throws OperationFailedException {
        try {
            Object result = null;

            if (info instanceof Object[]) {
                Object[] args = (Object[]) ((info instanceof Object[]) ? info : null);
                String taskId = null;
                CallbackEntry cbEntry = null;
                String providerName = null;
                OperationContext operationContext = null;

                if (args.length > 3) {
                    cbEntry = (CallbackEntry) ((args[3] instanceof CallbackEntry) ? args[3] : null);
                }
                if (args.length > 4) {
                    taskId = (String) ((args[4] instanceof String) ? args[4] : null);
                }
                if (args.length > 8) {
                    providerName = (String) ((args[8] instanceof String) ? args[8] : null);
                }

                if (args.length > 9) {
                    operationContext = (OperationContext) ((args[9] instanceof OperationContext) ? args[9] : null);
                } else if (args.length > 6) {
                    operationContext = (OperationContext) ((args[6] instanceof OperationContext) ? args[6] : null);
                } else if (args.length > 2) {
                    operationContext = (OperationContext) ((args[2] instanceof OperationContext) ? args[2] : null);
                }

                if (args != null && args.length > 0) {
                    Object tmp = args[0];
                    if (tmp instanceof Object[]) {
                        if (args.length > 5) {
                            providerName = (String) ((args[5] instanceof String) ? args[5] : null);
                        }
                        result = Local_Remove((Object[]) tmp, ItemRemoveReason.Removed, src, cbEntry, taskId, providerName, true, operationContext);

                    } else {
                        Object lockId = args[5];
                        LockAccessType accessType = (LockAccessType) args[6];
                        long version = (Long) args[7];

                        CacheEntry entry = Local_Remove(tmp, ItemRemoveReason.Removed, src, cbEntry, taskId, providerName, true, lockId, version, accessType, operationContext);
                        /*
                         * send value and entry seperaty
                         */
                        OperationResponse opRes = new OperationResponse();
                        if (entry != null) {
                            opRes.UserPayload = (entry.getValue() instanceof CallbackEntry ? ((CallbackEntry) entry.getValue()).getUserData() : entry.getUserData());
                            opRes.SerializablePayload = entry.CloneWithoutValue();
                        }
                        result = opRes;
                    }
                }
            }

            /**
             * Only the coordinator returns the object, this saves a lot of
             * traffic because only one reply actually contains the object other
             * replies are simply dummy objects.
             */
            if (getCluster().getIsCoordinator()) {
                return result;
            }
        } catch (Exception e) {
            if (_clusteredExceptions) {
                throw new OperationFailedException(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Hanlde cluster-wide Remove(key[]) requests.
     *
     * @param info the object containing parameters for this operation.
     * @return object to be sent back to the requestor.
     *
     * Removes a range of items locally; however without generating
     * notifications.
     * <p>
     * <b>Note:</b> When a client invokes <see cref="handleRemoveRange"/>; it is
     * the clients responsibility to actaully initiate the notification. </p>
     *
     */
    private Object handleRemoveRange(Object info) throws OperationFailedException {
        try {
            Object[] objs = (Object[]) info;
            OperationContext operationContext = null;
            if (objs.length > 2) {
                operationContext = (OperationContext) ((objs[2] instanceof OperationContext) ? objs[2] : null);
            }

            if (objs[0] instanceof Object[]) {
                Object[] keys = (Object[]) objs[0];
                ItemRemoveReason ir = (ItemRemoveReason) objs[1];

                java.util.HashMap totalRemovedItems = new java.util.HashMap();
                CacheEntry entry = null;
                Iterator ide = null;

                if (getContext().getCacheLog().getIsInfoEnabled()) {
                    getContext().getCacheLog().Info("Replicated.handleRemoveRange()", "Keys = " + (Integer) keys.length);
                }

                java.util.HashMap removedItems = Local_Remove(keys, ir, null, null, null, null, true, operationContext);

                if (removedItems != null) {
                    totalRemovedItems = removedItems;
                 
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
     * Returns a .NET IEnumerator interface so that a client should be able to
     * iterate over the elements of the cache store.
     *
     * @return IDictionaryEnumerator enumerator.
     */
    @Override
    public ResetableIterator GetEnumerator() throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        if ((_statusLatch.IsAnyBitsSet(NodeStatus.Initializing))) {
            Object tempVar = getCluster().getCoordinator().clone();
            {
                return Clustered_GetEnumerator((Address) ((tempVar instanceof Address) ? tempVar : null));
            }
        }

        return new LazyKeysetEnumerator(this, (Object[]) handleKeyList(), false);
    }

    @Override
    public EnumerationDataChunk GetNextChunk(EnumerationPointer pointer, OperationContext operationContext) throws OperationFailedException, GeneralFailureException, TimeoutException, SuspectedException, CacheException {
        /*
         * [KS]: All clustered operation have been removed as they use to return duplicate keys because the snapshots created on all the nodes of replicated for a particular
         * enumerator were not sorted and in case of node up and down we might get duplicate keys when a request is routed to another client. As per discussion with iqbal sahab it
         * has been decided that whenever a node leaves the cluster we will throw Enumeration has been modified exception to the client.
         */

        /**
         * Wait until the object enters any running status
         */
        _statusLatch.WaitForAny((byte) (NodeStatus.Initializing | NodeStatus.Running));

        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        EnumerationDataChunk nextChunk = null;

        if (getCluster().getServers().size() > 1) {

            //Enumeration Pointer came to this node on rebalance of clients and node is currently in state transfer or pointer came to this node and node is intitialized but doesnt have snapshot
            if ((_statusLatch.IsAnyBitsSet(NodeStatus.Initializing) && (pointer.getChunkId() > 0 || !getInternalCache().HasEnumerationPointer(pointer)))
                    || (!_statusLatch.IsAnyBitsSet(NodeStatus.Initializing) && pointer.getChunkId() > 0 && !getInternalCache().HasEnumerationPointer(pointer))) {
                throw new OperationFailedException("Enumeration Has been Modified");
            } else { //Node is initialized with data and has snapshot should return the next chunk locally
                nextChunk = getInternalCache().GetNextChunk(pointer, operationContext);
            }

            //Dispose the pointer on all nodes as this is the last chunk for this particular enumeration pointer
            if (pointer.isSocketServerDispose() && nextChunk == null) {
                pointer.setDisposable(true);
                getInternalCache().GetNextChunk(pointer, operationContext);
            } else if (nextChunk.isLastChunk()) {
                pointer = nextChunk.getPointer();
                pointer.setDisposable(true);
                getInternalCache().GetNextChunk(pointer, operationContext);
            }
        } else if (pointer.getChunkId() > 0 && !getInternalCache().HasEnumerationPointer(pointer)) {
            throw new OperationFailedException("Enumeration Has been Modified");
        } else {
            nextChunk = getInternalCache().GetNextChunk(pointer, operationContext);
        }

        return nextChunk;
    }

    private EnumerationDataChunk Clustered_GetNextChunk(java.util.ArrayList dests, EnumerationPointer pointer, OperationContext operationContext) throws CacheException {
        try {
            Function func = new Function(OpCodes.GetNextChunk.getValue(), new Object[]{
                pointer, operationContext
            }, false);

            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL, false);

            ClusterHelper.ValidateResponses(results, EnumerationDataChunk.class, getName());

            /**
             * Get a single resulting enumeration data chunk from a request
             */
            EnumerationDataChunk tempVar = ClusterHelper.FindAtomicEnumerationDataChunkReplicated(results);
            EnumerationDataChunk nextChunk = (EnumerationDataChunk) ((tempVar instanceof EnumerationDataChunk) ? tempVar : null);

            return nextChunk;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
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
                throw new OperationFailedException(e.getMessage(), e);
            }
        }
        return null;
    }

    private EnumerationDataChunk handleGetNextChunk(Address src, Object info) throws OperationFailedException, GeneralFailureException, TimeoutException, SuspectedException, CacheException {
        Object[] package_Renamed = (Object[]) ((info instanceof Object[]) ? info : null);
        EnumerationPointer pointer = (EnumerationPointer) ((package_Renamed[0] instanceof EnumerationPointer) ? package_Renamed[0] : null);
        OperationContext operationContext = (OperationContext) ((package_Renamed[1] instanceof OperationContext) ? package_Renamed[1] : null);

        //if a request for a an enumerator whos in middle of its enumeration comes to coordinator
        //and we cannot find it we will throw enumeration modified exception.
        if (this.getCluster().getIsCoordinator() && pointer.getChunkId() > 0 && !pointer.isSocketServerDispose() && !getInternalCache().HasEnumerationPointer(pointer)) {
            throw new OperationFailedException("Enumeration Has been Modified");
        }

        EnumerationDataChunk nextChunk = getInternalCache().GetNextChunk(pointer, operationContext);
        return nextChunk;
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
        OperationContext operationContext = null;
        Object[] operands = (Object[]) ((operand instanceof Object[]) ? operand : null);
        if (operands != null) {
            Object Keys = operands[0];
            CallbackInfo updateCallback = (CallbackInfo) ((operands[1] instanceof CallbackInfo) ? operands[1] : null);
            CallbackInfo removeCallback = (CallbackInfo) ((operands[2] instanceof CallbackInfo) ? operands[2] : null);
            if (operands.length > 2) {
                operationContext = (OperationContext) ((operands[3] instanceof OperationContext) ? operands[3] : null);
            }

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
    public boolean AcceptClient(InetAddress clientAddress) {
        if (getCluster().getIsCoordinator()) {
            return Local_AcceptClient(clientAddress);
        }
        return Clustered_AcceptClient(clientAddress);
    }

    private boolean Clustered_AcceptClient(InetAddress clientAddress) {
        try {
            Function func = new Function(OpCodes.AcceptClient.getValue(), new Object[]{
                clientAddress
            }, true);
            Object result = getCluster().SendMessage(getCluster().getCoordinator(), func, GroupRequest.GET_FIRST, false);
            if (result == null) {
                return false;
            }
            return (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean Local_AcceptClient(InetAddress clientAddress) {
        if (_internalCache != null) {
            if (_internalCache.AcceptClient(clientAddress)) {
                try {
                    UpdateCacheStatistics();
                    AnnouncePresence(true);
                } catch (Exception e) {
                }
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean handleAcceptClient(Object operand) {
        if (operand != null && operand instanceof Object[]) {
            Object[] package_Renamed = (Object[]) ((operand instanceof Object[]) ? operand : null);
            InetAddress address = (InetAddress) package_Renamed[0];
            return Local_AcceptClient(address);
        }
        return false;
    }

    @Override
    public void DisconnectClient(InetAddress clientAddress) {
        if (getCluster().getIsCoordinator()) {
            Local_DisconnectClient(clientAddress);
        }
        Clustered_DisconnectClient(clientAddress);
    }

    private void Clustered_DisconnectClient(InetAddress clientAddress) {
        try {
            Function func = new Function(OpCodes.DisconnectClient.getValue(), new Object[]{
                clientAddress
            }, true);
            //request for response just to make a synchronous call.
            Object result = getCluster().SendMessage(getCluster().getCoordinator(), func, GroupRequest.GET_FIRST, false);
        } catch (Exception e) {
        }
    }

    private void Local_DisconnectClient(InetAddress clientAddress) {
        if (_internalCache != null) {
            _internalCache.DisconnectClient(clientAddress);
            UpdateCacheStatistics();
            AnnouncePresence(true);
        }
    }

    private void handleDisconnectClient(Object operand) {
        if (operand != null && operand instanceof Object[]) {
            Object[] package_Renamed = (Object[]) ((operand instanceof Object[]) ? operand : null);
            InetAddress address = (InetAddress) package_Renamed[0];
            Local_DisconnectClient(address);
        }
    }

    @Override
    public boolean getIsEvictionAllowed() {
        return getCluster().getIsCoordinator();
    }

    @Override
    public void setIsEvictionAllowed(boolean value) {
        super.setIsEvictionAllowed(value);
    }

    /**
     * Fire when the cache is cleared.
     */
    public void OnCacheCleared(OperationContext operationContext, EventContext eventContext) {
        // do local notifications only, every node does that, so we get a replicated notification.
        if (_context.PersistenceMgr != null) {
            com.alachisoft.tayzgrid.persistence.Event perEvent = new com.alachisoft.tayzgrid.persistence.Event();
            perEvent.setPersistedEventId(eventContext.getEventID());
            perEvent.getPersistedEventId().setEventType(EventType.CACHE_CLEARED_EVENT);
            _context.PersistenceMgr.AddToPersistedEvent(perEvent);
        }
        UpdateCacheStatistics();
        handleNotifyCacheCleared(operationContext, eventContext);
    }

    /**
     * Hanlder for clustered cache clear notification.
     *
     * @param info packaged information
     * @return null
     */
    private Object handleNotifyCacheCleared(OperationContext operationContext, EventContext eventContext) {
        NotifyCacheCleared(true, operationContext, eventContext);
        return null;
    }

    /**
     * Fired when an item is added to the cache.
     */
    public void OnItemAdded(Object key, OperationContext operationContext, EventContext eventContext) {
        // specially handled in Add. 
        try {
            FilterEventContextForGeneralDataEvents(com.alachisoft.tayzgrid.runtime.events.EventType.ItemAdded, eventContext);
            //persist events
            if (_context.PersistenceMgr != null) {
                com.alachisoft.tayzgrid.persistence.Event perEvent = new com.alachisoft.tayzgrid.persistence.Event();
                com.alachisoft.tayzgrid.persistence.EventInfo eventInfo = new com.alachisoft.tayzgrid.persistence.EventInfo();
                eventInfo.setKey(key);
                perEvent.setPersistedEventInfo(eventInfo);
                perEvent.setPersistedEventId(eventContext.getEventID());
                perEvent.getPersistedEventId().setEventType(EventType.ITEM_ADDED_EVENT);
                _context.PersistenceMgr.AddToPersistedEvent(perEvent);
            }

            NotifyItemAdded(key, true, operationContext, eventContext);
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("ReplicatedCache.OnItemAdded()", "key: " + key.toString());
            }
        } catch (RuntimeException e) {
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("ReplicatedCache.OnItemAdded()", e.toString());
            }
        }
    }

    /**
     * Hanlder for clustered item added notification.
     *
     * @param info packaged information
     * @return null
     */
    @Override
    public Object handleNotifyAdd(Object info) {
        Object[] args = (Object[]) ((info instanceof Object[]) ? info : null);
        NotifyItemAdded(args[0], true, (OperationContext) args[1], (EventContext) args[2]);
        return null;
    }

    public void OnCustomEvent(Object notifId, Object data, OperationContext operationContext, EventContext eventContext) {
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

    /**
     * handler for item updated event.
     */
    public void OnItemUpdated(Object key, OperationContext operationContext, EventContext eventContext) {
        // specially handled in update.
        try {
            FilterEventContextForGeneralDataEvents(com.alachisoft.tayzgrid.runtime.events.EventType.ItemUpdated, eventContext);
            if (_context.PersistenceMgr != null) {
                com.alachisoft.tayzgrid.persistence.Event perEvent = new com.alachisoft.tayzgrid.persistence.Event();
                com.alachisoft.tayzgrid.persistence.EventInfo eventInfo = new com.alachisoft.tayzgrid.persistence.EventInfo();
                eventInfo.setKey(key);
                perEvent.setPersistedEventInfo(eventInfo);
                perEvent.setPersistedEventId(eventContext.getEventID());
                perEvent.getPersistedEventId().setEventType(EventType.ITEM_UPDATED_EVENT);
                _context.PersistenceMgr.AddToPersistedEvent(perEvent);
            }

            NotifyItemUpdated(key, true, operationContext, eventContext);
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("ReplicatedCache.OnItemUpdated()", "key: " + key.toString());
            }
        } catch (Exception e) {
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("ReplicatedCache.OnItemUpdated()", "Key: " + key.toString() + " Error: " + e.toString());
            }
        }
    }

    /**
     * Hanlder for clustered item updated notification.
     *
     * @param info packaged information
     * @return null
     */
    public Object handleNotifyUpdate(Object info) {
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
     * If the items are removed due to evictions or expiration, this method
     * replicates the removals, i.e., makes sure those items get removed from
     * every node in the cluster. It then also triggers the cluster-wide item
     * remove notification.
     *
     */
    public void OnItemsRemoved(Object[] keys, Object[] values, ItemRemoveReason reason, OperationContext operationContext, EventContext[] eventContexts) {
        try {

            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("Replicated.OnItemsRemoved()", "items evicted/expired, now replicating");
            } 

            if ((getIsItemRemoveNotifier() )) {
                CacheEntry entry;
                for (int i = 0; i < keys.length; i++) {
                    entry = (CacheEntry) values[i];
                    Object value = entry.getValue();

                    FilterEventContextForGeneralDataEvents(com.alachisoft.tayzgrid.runtime.events.EventType.ItemRemoved, eventContexts[i]);
                    //value is contained inside eventContext
                    Object data = new Object[]{keys[i], reason, operationContext, eventContexts[i]};

                    if (_context.PersistenceMgr != null) {
                        com.alachisoft.tayzgrid.persistence.Event perEvent = new com.alachisoft.tayzgrid.persistence.Event();
                        com.alachisoft.tayzgrid.persistence.EventInfo eventInfo = new com.alachisoft.tayzgrid.persistence.EventInfo();
                        eventInfo.setKey(keys[i]);
                        perEvent.setPersistedEventInfo(eventInfo);
                        perEvent.setPersistedEventId(eventContexts[i].getEventID());
                        perEvent.getPersistedEventId().setEventType(EventType.ITEM_REMOVED_EVENT);
                        _context.PersistenceMgr.AddToPersistedEvent(perEvent);
                    }

                    if (getCluster().getIsCoordinator() && _nodesInStateTransfer.size() > 0) {
                        RaiseItemRemoveNotifier((ArrayList) _nodesInStateTransfer.clone(), data);
                    }

                    if (_allowEventRaiseLocally) {
                        NotifyItemRemoved(keys[i], null, reason, true, operationContext, eventContexts[i]);
                    }
                }
            }
        } catch (Exception e) {
            getContext().getCacheLog().Warn("Replicated.OnItemsRemoved", "failed: " + e.toString());
        } finally {
            UpdateCacheStatistics();
        }
    }

    private void RaiseItemRemoveNotifier(java.util.ArrayList servers, Object data) throws Exception {
        try {
            Function func = new Function(OpCodes.NotifyRemoval.getValue(), data);
            getCluster().Multicast(servers, func, GroupRequest.GET_NONE, false, getCluster().getTimeout());
        } catch (RuntimeException e) {
            getContext().getCacheLog().Error("ReplicatedCache.ItemRemoveNotifier()", e.toString());
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
        if (objs.length > 2) {
            operationContext = (OperationContext) ((objs[2] instanceof OperationContext) ? objs[2] : null);
        }

        NotifyItemRemoved(objs[0], null, (ItemRemoveReason) objs[1], true, operationContext, (EventContext) objs[3]);
        return null;
    }

    /**
     * handler for item update callback event.
     */
    public void OnCustomUpdateCallback(Object key, Object value, OperationContext operationContext, EventContext eventContext) {
        // specially handled in update.
        try {
            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("Replicated.OnCustomUpdateCallback()", "");
            }

            if (value != null) {
                java.util.List itemUpdateCallbackListener = (java.util.List) value;
                if (_context.PersistenceMgr != null) {
                    if (itemUpdateCallbackListener != null && itemUpdateCallbackListener.size() > 0) {
                        com.alachisoft.tayzgrid.persistence.Event perEvent = new com.alachisoft.tayzgrid.persistence.Event();
                        com.alachisoft.tayzgrid.persistence.EventInfo eventInfo = new com.alachisoft.tayzgrid.persistence.EventInfo();
                        eventInfo.setKey(key);
                        eventInfo.setCallBackInfoList((ArrayList) itemUpdateCallbackListener);
                        perEvent.setPersistedEventInfo(eventInfo);
                        perEvent.setPersistedEventId(eventContext.getEventID());
                        perEvent.getPersistedEventId().setEventType(EventType.ITEM_UPDATED_CALLBACK);
                        _context.PersistenceMgr.AddToPersistedEvent(perEvent);
                    }
                }

                if (getCluster().getIsCoordinator() && _nodesInStateTransfer.size() > 0) {
                    Object data = new Object[]{key, itemUpdateCallbackListener, operationContext, eventContext};
                    RaiseUpdateCallbackNotifier((ArrayList) _nodesInStateTransfer.clone(), data);
                }

                if (_allowEventRaiseLocally) {
                    NotifyCustomUpdateCallback(key, itemUpdateCallbackListener, true, operationContext, eventContext);
                }
            }
        } catch (ClassNotFoundException c) {
            getContext().getCacheLog().Warn("Replicated.OnCustomUpdated", "failed: " + c.toString());
        } catch (IOException io) {
            getContext().getCacheLog().Warn("Replicated.OnCustomUpdated", "failed: " + io.toString());
        } catch (RuntimeException e) {
            getContext().getCacheLog().Warn("Replicated.OnCustomUpdated", "failed: " + e.toString());
        } finally {
            UpdateCacheStatistics();
        }
    }

    private void RaiseUpdateCallbackNotifier(java.util.ArrayList servers, Object data) throws ClassNotFoundException, IOException {
        try {
            if (getCluster().getServers() != null && getCluster().getServers().size() > 1) {
                Function func = new Function(OpCodes.NotifyCustomUpdateCallback.getValue(), data);
                getCluster().Multicast(servers, func, GroupRequest.GET_NONE, false, getCluster().getTimeout());
            }
        } catch (RuntimeException e) {
            getContext().getCacheLog().Error("ReplicatedCache.CustomUpdateCallback()", e.toString());
        }
    }

    private Object handleNotifyUpdateCallback(Object info) {
        Object[] objs = (Object[]) info;
        java.util.ArrayList callbackListeners = (java.util.ArrayList) ((objs[1] instanceof java.util.ArrayList) ? objs[1] : null);
        NotifyCustomUpdateCallback(objs[0], objs[1], true, (OperationContext) objs[2], (EventContext) objs[3]);
        return null;
    }

    /**
     * handler for item remove callback event.
     */
    public void OnCustomRemoveCallback(Object key, Object value, ItemRemoveReason removalReason, OperationContext operationContext, EventContext eventContext) {
        try {

            if (getContext().getCacheLog().getIsInfoEnabled()) {
                getContext().getCacheLog().Info("Replicated.OnCustomRemoveCallback()", "items evicted/expired, now replicating");
            }

            CacheEntry entry = (CacheEntry) ((value instanceof CacheEntry) ? value : null);
            Object tempVar = entry.getValue();
            CallbackEntry cbEntry = entry != null ? (CallbackEntry) ((tempVar instanceof CallbackEntry) ? tempVar : null) : null;

            if (cbEntry != null) {
                if (_context.PersistenceMgr != null) {
                    com.alachisoft.tayzgrid.persistence.Event perEvent = new com.alachisoft.tayzgrid.persistence.Event();
                    perEvent.setPersistedEventId(eventContext.getEventID());

                    com.alachisoft.tayzgrid.persistence.EventInfo eventInfo = new com.alachisoft.tayzgrid.persistence.EventInfo();
                    eventInfo.setKey(key);
                    eventInfo.setFlag(cbEntry.getFlag());
                    eventInfo.setReason(removalReason);
                    eventInfo.setCallBackInfoList((ArrayList) cbEntry.getItemRemoveCallbackListener());
                    UserBinaryObject itemValue = (UserBinaryObject) ((cbEntry.getValue() instanceof UserBinaryObject) ? cbEntry.getValue() : null);
                    eventInfo.setValue(itemValue.getDataList());
                    perEvent.setPersistedEventInfo(eventInfo);

                    _context.PersistenceMgr.AddToPersistedEvent(perEvent);

                }

                if (getCluster().getIsCoordinator() && _nodesInStateTransfer.size() > 0) {
                    Object data = new Object[]{key, removalReason, operationContext, eventContext};
                    RaiseRemoveCallbackNotifier((ArrayList) _nodesInStateTransfer.clone(), data);
                }
                if (_allowEventRaiseLocally) {
                    NotifyCustomRemoveCallback(key, entry, removalReason, true, operationContext, eventContext);
                }
            }
        } catch (Exception e) {
            getContext().getCacheLog().Warn("Replicated.OnItemsRemoved", "failed: " + e.toString());
        } finally {
            UpdateCacheStatistics();
        }
    }

    private void RaiseRemoveCallbackNotifier(java.util.ArrayList servers, Object data) throws Exception {
        try {
            if (getCluster().getServers() != null && getCluster().getServers().size() > 1) {
                Function func = new Function(OpCodes.NotifyCustomRemoveCallback.getValue(), data);
                getCluster().Multicast(servers, func, GroupRequest.GET_NONE, false, getCluster().getTimeout());
            }
        } catch (RuntimeException e) {
            getContext().getCacheLog().Error("ReplicatedCache.CustomRemoveCallback()", e.toString());
        }
    }

    private Object handleNotifyRemoveCallback(Object info) throws Exception {
        Object[] objs = (Object[]) info;
        NotifyCustomRemoveCallback(objs[0], null, (ItemRemoveReason) objs[1], true, (OperationContext) objs[2], (EventContext) objs[3]);
        return null;
    }

   

    private boolean Local_CanLock(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        CacheEntry e = Get(key, operationContext);
        if (e != null) {
            return !e.IsLocked(lockId, lockDate);
        } else {
            lockId.argvalue = null;
        }
        return false;
    }

    @Override
    public LockOptions Lock(Object key, LockExpiration lockExpiration, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("RepCache.Lock", "");
        }
        if (getCluster().getServers().size() > 1) {
            Clustered_Lock(key, lockExpiration, lockId, lockDate, operationContext);
            return new LockOptions(lockId.argvalue, lockDate.argvalue);
        } else {
            return Local_Lock(key, lockExpiration, lockId.argvalue, lockDate.argvalue, operationContext);
        }
    }

    private LockOptions handleLock(Object info) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        Object[] param = (Object[]) info;
        OperationContext operationContext = null;
        if (param.length > 4) {
            operationContext = (OperationContext) ((param[4] instanceof OperationContext) ? param[4] : null);
        }

        return Local_Lock(param[0], (LockExpiration) param[3], param[1], (java.util.Date) param[2], operationContext);
    }

    private LockOptions Local_Lock(Object key, LockExpiration lockExpiration, Object lockId, java.util.Date lockDate, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        if (_internalCache != null) {
            tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
            tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
            LockOptions tempVar = _internalCache.Lock(key, lockExpiration, tempRef_lockId, tempRef_lockDate, operationContext);
            lockId = tempRef_lockId.argvalue;
            lockDate = tempRef_lockDate.argvalue;
            return tempVar;
        }
        return null;
    }

    @Override
    public void UnLock(Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("RepCache.Unlock", "");
        }
        if (getCluster().getServers().size() > 1) {
            Clustered_UnLock(key, lockId, isPreemptive, operationContext);
        } else {
            Local_UnLock(key, lockId, isPreemptive, operationContext);
        }
    }

    private void Local_UnLock(Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        if (_internalCache != null) {
            _internalCache.UnLock(key, lockId, isPreemptive, operationContext);
        }
    }

    private void handleUnLockKey(Object info) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        Object[] package_Renamed = (Object[]) ((info instanceof Object[]) ? info : null);
        if (package_Renamed != null) {
            Object key = package_Renamed[0];
            Object lockId = package_Renamed[1];
            boolean isPreemptive = (Boolean) package_Renamed[2];
            OperationContext operationContext = null;
            if (package_Renamed.length > 3) {
                operationContext = (OperationContext) ((package_Renamed[3] instanceof OperationContext) ? package_Renamed[3] : null);
            }

            Local_UnLock(key, lockId, isPreemptive, operationContext);
        }
    }

    @Override
    public LockOptions IsLocked(Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        if (getCluster().getServers().size() > 1) {
            return Clustered_IsLocked(key, lockId, lockDate, operationContext);
        } else {
            return Local_IsLocked(key, lockId.argvalue, lockDate.argvalue, operationContext);
        }
    }

    private LockOptions handleIsLocked(Object info) throws OperationFailedException, CacheException, LockingException {
        Object[] param = (Object[]) info;
        OperationContext operationContext = null;
        if (param.length > 3) {
            operationContext = (OperationContext) ((param[3] instanceof OperationContext) ? param[3] : null);
        }

        return Local_IsLocked(param[0], param[1], (java.util.Date) param[2], operationContext);
    }

    private LockOptions Local_IsLocked(Object key, Object lockId, java.util.Date lockDate, OperationContext operationContext) throws OperationFailedException, CacheException, LockingException, GeneralFailureException {
        if (_internalCache != null) {
            tangible.RefObject<Object> tempRef_lockId = new tangible.RefObject<Object>(lockId);
            tangible.RefObject<java.util.Date> tempRef_lockDate = new tangible.RefObject<java.util.Date>(lockDate);
            LockOptions tempVar = _internalCache.IsLocked(key, tempRef_lockId, tempRef_lockDate, operationContext);
            lockId = tempRef_lockId.argvalue;
            lockDate = tempRef_lockDate.argvalue;
            return tempVar;
        }
        return null;
    }

    @Override
    public void StopServices() throws InterruptedException {
        _statusLatch.SetStatusBit((byte) 0, (byte) (NodeStatus.Initializing | NodeStatus.Running));
        if (_asyncReplicator != null) {
            _asyncReplicator.dispose();
        }
        super.StopServices();
    }



 

    /**
     * Retrieve the list of keys from the cache for the given group or sub
     * group.
     */
    @Override
    public java.util.ArrayList GetGroupKeys(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.ArrayList list = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        if (IsInStateTransfer()) {
            list = Clustered_GetGroupKeys(GetDestInStateTransfer(), group, subGroup, operationContext);
        } else {
            list = Local_GetGroupKeys(group, subGroup, operationContext);
        }

        return list;
    }

    /**
     * Retrieve the list of key and value pairs from the cache for the given
     * group or sub group.
     */
    @Override
    public java.util.HashMap GetGroupData(String group, String subGroup, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap list = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        if (IsInStateTransfer()) {
            list = Clustered_GetGroupData(GetDestInStateTransfer(), group, subGroup, operationContext);
        } else {
            list = Local_GetGroupData(group, subGroup, operationContext);
        }
        if (getCluster().getServers().size() > 1) {

            if (list != null) {

                ArrayList updateIndiceKeyList = null;
                Iterator ine = list.entrySet().iterator();
                Map.Entry entry;
                while (ine.hasNext()) {
                    entry = (Map.Entry) ine.next();
                    CacheEntry e = (CacheEntry) entry.getValue();
                    if (e == null) {
                        _stats.BumpMissCount();
                    } else {
                        if (updateIndiceKeyList == null) {
                            updateIndiceKeyList = new ArrayList();
                        }
                        _stats.BumpHitCount();
                        // update the indexes on other nodes in the cluster
                        if ((e.getExpirationHint() != null && e.getExpirationHint().getIsVariant())) {
                            updateIndiceKeyList.add(entry.getKey());
                        }
                    }
                }
                if (updateIndiceKeyList != null && updateIndiceKeyList.size() > 0) {
                    UpdateIndices(updateIndiceKeyList.toArray(), true, operationContext);
                }
            }

        }
        return list;
    }

    /**
     * Retrieve the list of keys from the cache for the given tags.
     */
    @Override
    public java.util.ArrayList GetTagKeys(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.ArrayList list = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        if (IsInStateTransfer()) {
            list = Clustered_GetTagKeys(GetDestInStateTransfer(), tags, comparisonType, operationContext);
        } else {
            list = Local_GetTagKeys(tags, comparisonType, operationContext);
        }

        return list;
    }

    /**
     * Retrieve the list of key and value pairs from the cache for the given
     * tags.
     */
    @Override
    public java.util.HashMap GetTagData(String[] tags, TagComparisonType comparisonType, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap list = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        if (IsInStateTransfer()) {
            list = Clustered_GetTagData(GetDestInStateTransfer(), tags, comparisonType, operationContext);
        } else {
            list = Local_GetTagData(tags, comparisonType, operationContext);
        }
        if (getCluster().getServers().size() > 1) {

            if (list != null) {

                ArrayList updateIndiceKeyList = null;
                Iterator ine = list.entrySet().iterator();
                Map.Entry entry;
                while (ine.hasNext()) {
                    entry = (Map.Entry) ine.next();
                    CacheEntry e = (CacheEntry) entry.getValue();
                    if (e == null) {
                        _stats.BumpMissCount();
                    } else {
                        if (updateIndiceKeyList == null) {
                            updateIndiceKeyList = new ArrayList();
                        }
                        _stats.BumpHitCount();
                        // update the indexes on other nodes in the cluster
                        if ((e.getExpirationHint() != null && e.getExpirationHint().getIsVariant())) {
                            updateIndiceKeyList.add(entry.getKey());
                        }
                    }
                }
                if (updateIndiceKeyList != null && updateIndiceKeyList.size() > 0) {
                    UpdateIndices(updateIndiceKeyList.toArray(), true, operationContext);
                }
            }

        }
        return list;
    }

    /**
     * Remove the list of key from the cache for the given tags.
     */
    @Override
    public java.util.HashMap Remove(String[] tags, TagComparisonType comparisonType, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap result = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        if (IsInStateTransfer()) {
            result = Clustered_RemoveByTag(GetDestInStateTransfer(), tags, comparisonType, notify, operationContext);
        } else {
            result = Local_RemoveTag(tags, comparisonType, notify, operationContext);
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
    public java.util.HashMap Remove(String group, String subGroup, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        java.util.HashMap result = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        if (IsInStateTransfer()) {
            result = Clustered_RemoveGroup(GetDestInStateTransfer(), group, subGroup, notify, operationContext);
        } else {
            result = Local_RemoveGroup(group, subGroup, notify, operationContext);
        }

        return result;
    }

    /**
     * Retrieve the list of keys from the cache based on the specified query.
     */
    @Override
    public QueryResultSet Search(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        QueryResultSet result = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        if (IsInStateTransfer()) {
            result = Clustered_Search(GetDestInStateTransfer(), query, values, operationContext);
        } else {
            result = Local_Search(query, values, operationContext);
        }

        return result;
    }

    /**
     * Retrieve the list of keys and values from the cache based on the
     * specified query.
     */
    @Override
    public QueryResultSet SearchEntries(String query, java.util.Map values, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, StateTransferException, CacheException {
        if (_internalCache == null) {
            throw new UnsupportedOperationException();
        }

        QueryResultSet result = null;
        java.util.ArrayList dests = new java.util.ArrayList();

        if (IsInStateTransfer()) {
            result = Clustered_SearchEntries(GetDestInStateTransfer(), query, values, operationContext);
        } else {
            result = Local_SearchEntries(query, values, operationContext);
        }
        if (getCluster().getServers().size() > 1) {

            if (result != null) {
                if (result.getUpdateIndicesKeys() != null) {
                    UpdateIndices(result.getUpdateIndicesKeys().toArray(), true, operationContext);
                }
            }

        }
        return result;
    }

    @Override
    public boolean IsInStateTransfer() {
        return _statusLatch.IsAnyBitsSet(NodeStatus.Initializing);
    }

    @Override
    protected boolean VerifyClientViewId(long clientLastViewId) {
        return true;
    }

    @Override
    protected java.util.ArrayList GetDestInStateTransfer() {
        java.util.ArrayList list = new java.util.ArrayList();
        list.add(this.getCluster().getCoordinator());
        return list;
    }
   
        
    @Override
    public void OnTaskCallback(Object taskID, Object value, OperationContext operationContext, EventContext eventContext){
    
        if(value!=null)
            RaiseTaskCalbackNotifier(taskID, (List) value, eventContext);
    }
    
    @Override
    public java.util.ArrayList determineClientConnectivity(java.util.ArrayList clients) {
        java.util.ArrayList result = null;
        if (clients == null) {
            return null;
        }
        if (_context.getCacheLog().getIsInfoEnabled()) {
            _context.getCacheLog().Info("Client-Death-Detection.DetermineClientConnectivity()", "going to determine client connectivity in cluster");
        }
        try {
            DetermineClusterStatus(); //updating stats
            result = clients;
            for (Iterator it = clients.iterator(); it.hasNext();) {
                String client = (String) it.next();
                for (Iterator itc = _stats.getNodes().iterator(); itc.hasNext();) {
                    NodeInfo node = (NodeInfo) itc.next();
                    if (node.getConnectedClients().contains(client)) {
                        if (result.contains(client)) {
                            result.remove(client);
                        }
                        break;
                    }
                }
            }
        } catch (RuntimeException e) {
            _context.getCacheLog().Error("Client-Death-Detection.DetermineClientConnectivity()", e.toString());
        } finally {
            if (_context.getCacheLog().getIsInfoEnabled()) {

                _context.getCacheLog().Info("Client-Death-Detection.DetermineClientConnectivity()", "determining client connectivity in cluster completed");
            }
        }
        return result;
    }
}
