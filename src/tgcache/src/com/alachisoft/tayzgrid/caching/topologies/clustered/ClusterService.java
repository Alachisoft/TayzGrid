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
import com.alachisoft.tayzgrid.cluster.stack.MessageObjectProvider;
import com.alachisoft.tayzgrid.cluster.blocks.RequestHandler;
import com.alachisoft.tayzgrid.cluster.blocks.MsgDispatcher;
import com.alachisoft.tayzgrid.cluster.ChannelException;
import com.alachisoft.tayzgrid.cluster.MembershipListener;
import com.alachisoft.tayzgrid.cluster.Channel;
import com.alachisoft.tayzgrid.cluster.MessageResponder;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.GroupChannel;
import com.alachisoft.tayzgrid.cluster.ChannelClosedException;
import com.alachisoft.tayzgrid.cluster.MessageListener;
import com.alachisoft.tayzgrid.cluster.OperationResponse;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.config.OnClusterConfigUpdate;
import com.alachisoft.tayzgrid.config.ConfigHelper;
import com.alachisoft.tayzgrid.caching.topologies.history.NodeActivities;
import com.alachisoft.tayzgrid.caching.topologies.IClusterEventsListener;
import com.alachisoft.tayzgrid.caching.Cache;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.mirroring.CacheNode;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.enums.Priority;
import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.datastructures.DistributionMode;
import com.alachisoft.tayzgrid.common.datastructures.ClusterActivity;
import com.alachisoft.tayzgrid.common.datastructures.DistributionMaps;
import com.alachisoft.tayzgrid.common.datastructures.PartNodeInfo;
import com.alachisoft.tayzgrid.common.datastructures.DistributionInfoData;
import com.alachisoft.tayzgrid.common.EventCategories;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.common.GenericCopier;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.common.caching.statistics.monitoring.Monitor;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import java.util.ArrayList;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.util.PortCalculator;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

public class ClusterService implements RequestHandler, MembershipListener, MessageListener, MessageResponder, IDisposable {
    
    public static class AsyncBroadCast implements AsyncProcessor.IAsyncTask {
        
        private ClusterService _disp = null;
        private Object _data = null;
        
        public AsyncBroadCast(ClusterService disp, Object data) {
            _disp = disp;
            _data = data;
        }
        
        @Override
        public void Process() throws OperationFailedException, IOException {
            try {
                _disp.SendNoReplyMessage(_data);
            } catch (ChannelException channelException) {
                throw new OperationFailedException(channelException);
            }
            
        }
    }
    
    public static class AsyncUnicasCast implements AsyncProcessor.IAsyncTask {
        
        private ClusterService _disp = null;
        private Object _data = null;
        private Address _dest = null;
        
        public AsyncUnicasCast(Address dest, ClusterService disp, Object data) {
            _disp = disp;
            _data = data;
            _dest = dest;
        }
        
        public void Process() throws IOException, OperationFailedException {
            try {
                _disp.SendNoReplyMessage(_dest, _data);
            } catch (ChannelException channelException) {
                throw new OperationFailedException(channelException);
            }
        }
    }
    
    public static class AsyncMulticastCast implements AsyncProcessor.IAsyncTask {
        
        private ClusterService _disp = null;
        private Object _data = null;
        private java.util.ArrayList _dests = null;
        
        public AsyncMulticastCast(java.util.ArrayList dest, ClusterService disp, Object data) {
            _disp = disp;
            _data = data;
            _dests = dest;
        }
        
        @Override
        public void Process() throws IOException, OperationFailedException {
            try {
                _disp.SendNoReplyMulticastMessage(_dests, _data);
            } catch (ChannelException channelException) {
                throw new OperationFailedException(channelException);
            }
        }
    }
    private OnClusterConfigUpdate _onClusterConfigUpdate;
    private IClusterEventsListener _listener;
    protected String _subgroupid;
    protected CacheRuntimeContext _context;
    protected Channel _channel;
    protected MsgDispatcher _msgDisp;
    protected IClusterParticipant _participant;
    protected IDistributionPolicyMember _distributionPolicyMbr;
    private java.util.Map _subgroups = new java.util.HashMap();
    protected java.util.List _members = Collections.synchronizedList(new java.util.ArrayList(11));
    protected java.util.List _groupCoords = Collections.synchronizedList(new java.util.ArrayList(11));
    protected java.util.List _validMembers = Collections.synchronizedList(new java.util.ArrayList(11));
    protected java.util.List _servers = Collections.synchronizedList(new java.util.ArrayList(11));
    protected java.util.List _otherServers = null;
    private long _defOpTimeout = 60000;
    protected Priority _eventPriority = Priority.Normal;
    private MessageObjectProvider _msgProvider = new MessageObjectProvider(50);
    private ClusterOperationSynchronizer _asynHandler;
    private java.util.HashMap _membersRenders = new java.util.HashMap();
    private long _lastViewId;
    private Object viewMutex = new Object();
    private static final int MAX_CLUSTER_MBRS = 2;
    private static java.util.ArrayList _runningClusters;
    public String localIp = "";
    private Cache.CacheStoppedEvent _cacheStopped = null;
    private Cache.CacheStartedEvent _cacheStarted = null;
    private boolean _stopServices;
    private ILogger _ncacheLog;
    
    public final ILogger getCacheLog() {
        return _ncacheLog;
    }
    
    private String _bridgeSourceCacheId;
    public NodeActivities _history;
    
    public ClusterService(CacheRuntimeContext context, IClusterParticipant part, IDistributionPolicyMember distributionMbr) //, IMirrorManagementMember mirrorManagementMbr)
    {
        _context = context;
        _ncacheLog = context.getCacheLog();
        
        _participant = part;
        _distributionPolicyMbr = distributionMbr;
        
        _asynHandler = new ClusterOperationSynchronizer(this);

       
        if (_runningClusters == null) {
            _runningClusters = new java.util.ArrayList();
        }
       
        _history = new NodeActivities();
    }
    
    private boolean ClusterExist(String CacheName) {

       
            if (_runningClusters.contains(CacheName)) {
                return true;
            } else {
                _runningClusters.add(CacheName);
                return false;
            }
       
       
    }
    
    public final void InitializeClusterPerformanceCounters(String instancename, Monitor monitor) {
        if (_channel != null) {
            try {
                ((GroupChannel) _channel).InitializePerformanceCounter(instancename, monitor);
            } catch (Exception e) {
                getCacheLog().Error("ClusterService.InitializeCLusterCounters", e.toString());
            }
        }
    }
    
    public ClusterService(CacheRuntimeContext context, IClusterParticipant part, IDistributionPolicyMember distributionMbr, IClusterEventsListener listener) {
        _context = context;
        _participant = part;
        _distributionPolicyMbr = distributionMbr;
        _asynHandler = new ClusterOperationSynchronizer(this);
        _listener = listener;

        
        if (_runningClusters == null) {
            _runningClusters = new java.util.ArrayList();
        }
       
        
        _history = new NodeActivities();
    }
    
    public final void dispose() {
        if (_msgDisp != null) {
            _msgDisp.stop();
            _msgDisp = null;
        }
        if (_channel != null) {
            try {
                _channel.close();
            } catch (InterruptedException interruptedException) {
            }
            _channel = null;
        }
        if (_asynHandler != null) {
            _asynHandler.dispose();
        }
    }
    
    public final String getClusterName() {
        return _channel.getChannelName();
    }
    
    public final String getSubClusterName() {
        return _subgroupid;
    }
    
    public final String getBridgeSourceCacheId() {
        return _bridgeSourceCacheId;
    }
    
    public final SubCluster getCurrentSubCluster() {
        return GetSubCluster(_subgroupid);
    }
    
    public final long getTimeout() {
        return _defOpTimeout;
    }
    
    public final void setTimeout(long value) {
        _defOpTimeout = value;
    }
    
    public final IClusterEventsListener getClusterEventsListener() {
        return _listener;
    }
    
    public final void setClusterEventsListener(IClusterEventsListener value) {
        _listener = value;
    }
    
    public final long getLastViewID() {
        return this._lastViewId;
    }
    
    public final SubCluster GetSubCluster(String name) {
        if (name == null) {
            return null;
        }
        return (SubCluster) _subgroups.get(name);
    }
    
    public final SubCluster GetSubCluster(Address address) {
        if (address == null) {
            return null;
        }
        synchronized (_subgroups) {
            for (java.util.Iterator i = _subgroups.values().iterator(); i.hasNext();) {
                SubCluster group = (SubCluster) i.next();
                if (group.IsMember(address)) {
                    return group;
                }
            }
        }
        return null;
    }
    
    public final java.util.List getMembers() {
        return _members;
    }
    
    public final java.util.List getValidMembers() {
        return _validMembers;
    }
    
    public final java.util.List getServers() {
        return _servers;
    }
    
    public final java.util.List getSubCoordinators() {
        return _groupCoords;
    }
    
    public final java.util.List getOtherServers() {
        return _otherServers;
    }
    
    public final java.util.HashMap getRenderers() {
        return this._membersRenders;
    }
    
    public final boolean getIsCoordinator() {
        Address address = getCoordinator();
        if (address != null && getLocalAddress().compareTo(address) == 0) {
            return true;
        }
        return false;
    }
    
    public final boolean IsMember(Address node) {
        return _members.contains(node);
    }
    
    public final Address getCoordinator() {
        synchronized (_servers) {
            if (_servers.size() > 0) {
                return (Address) ((_servers.get(0) instanceof Address) ? _servers.get(0) : null);
            }
        }
        return null;
    }

    /**
     * returns the next coordinator in the cluster.
     */
    public final Address NextCoordinator() {
        
        synchronized (_servers) {
            if (_servers.size() > 1) {
                return (Address) ((_servers.get(1) instanceof Address) ? _servers.get(1) : null);
            }
        }
        return null;
        
    }
    
    public final Address getLocalAddress() {
        return _channel.getLocalAddress();
    }
    
    public final void Initialize(java.util.Map properties, String channelName, String domain, NodeIdentity identity, boolean isInproc) throws ChannelException, InterruptedException,
            ConfigurationException, IOException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }
        
        try {
            if (properties.containsKey("op-timeout")) {
                long val = (Long) (properties.get("op-timeout"));
                if (val < 60) {
                    val = 60;
                }
                val = val * 1000;
                setTimeout(val);
            }
            if (properties.containsKey("notification-priority")) {
                String priority = String.valueOf(properties.get("notification-priority"));
                if (priority.toLowerCase().equals("normal")) {
                    _eventPriority = Priority.Normal;
                }
            }
            
            java.util.Map clusterProps = (java.util.Map) ((properties.get("cluster") instanceof java.util.Map) ? properties.get("cluster") : null);
            if (clusterProps.containsKey("channel")) {
                java.util.Map tempMap1 = (java.util.Map)clusterProps.get("channel");
                java.util.Map tempMap2 = (java.util.Map)tempMap1.get("tcp");
                if(identity.getIsStartedAsMirror()){
                    int port = Integer.parseInt(tempMap2.get("start_port").toString());
                    port = PortCalculator.getClusterPortReplica(port);
                    tempMap2.put("start_port", port);
                }
                tempMap2.put("is_inproc", isInproc);
                tempMap1.put("tcp", tempMap2);
                clusterProps.put("channel", tempMap1);
            }
            String channelProps = ConfigHelper.GetClusterPropertyString(clusterProps, getTimeout());
            
            String name = channelName != null ? channelName.toLowerCase() : null;
            if (clusterProps.containsKey("group-id")) {
                name = String.valueOf(clusterProps.get("group-id"));
            }
            if (clusterProps.containsKey("sub-group-id")) {
                _subgroupid = String.valueOf(clusterProps.get("sub-group-id"));
                if (_subgroupid != null) {
                    _subgroupid = _subgroupid.toLowerCase();
                }
                identity.setSubGroupName(_subgroupid);
            } else {
                _subgroupid = name;
            }
            if (name != null) {
                name = name.toLowerCase();
            }
            if (_subgroupid != null) {
                _subgroupid = _subgroupid.toLowerCase();
            }

           
            this.PopulateClusterNodes(new java.util.HashMap(clusterProps));
            

            //A property or indexer may not be passed as an out or ref parameter.
            _channel = new GroupChannel(channelProps, _context.getCacheLog());
            
            java.util.HashMap config = new java.util.HashMap();
            config.put("additional_data", CompactBinaryFormatter.toByteBuffer(identity, _context.getSerializationContext()));
            _channel.down(new Event(Event.CONFIG, config));
            
            _msgDisp = new MsgDispatcher(_channel, this, this, this, this, false, true);
            _channel.connect(name + domain, _subgroupid, identity.getIsStartedAsMirror(), false);
            
            localIp = getLocalAddress().getIpAddress().getHostAddress();
            _msgDisp.start();
        } catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.toString(), e);
        }
    }
    
    public final void Initialize(java.util.Map properties, String channelName, String domain, NodeIdentity identity, String userId, String password, boolean twoPhaseInitialization, boolean isReplica, boolean isInproc) throws
            ChannelException, ConfigurationException, InterruptedException, IOException {
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }
        
        try {
            if (properties.containsKey("op-timeout")) {
                long val = Long.parseLong((String) properties.get("op-timeout"));
                if (val < 60) {
                    val = 60;
                }
                val = val * 1000;
                setTimeout(val);
            }
            if (properties.containsKey("notification-priority")) {
                String priority = String.valueOf(properties.get("notification-priority"));
                if (priority.toLowerCase().equals("normal")) {
                    _eventPriority = Priority.Normal;
                }
            }
            
            java.util.Map clusterProps = (java.util.Map) ((properties.get("cluster") instanceof java.util.Map) ? properties.get("cluster") : null);
            int port =0;
            if (clusterProps.containsKey("channel")) {
                java.util.Map tempMap1 = (java.util.Map)clusterProps.get("channel");
                java.util.Map tempMap2 = (java.util.Map)tempMap1.get("tcp");
                port = Integer.parseInt(tempMap2.get("start_port").toString());
                if(identity.getIsStartedAsMirror()){
                    port = PortCalculator.getClusterPortReplica(port);
                    tempMap2.put("start_port", port);
                }
                
                java.util.Map tcpPingProp = (java.util.Map)tempMap1.get("tcpping");
                if(tcpPingProp != null){
                    
                    if(identity.getIsStartedAsMirror())
                        tcpPingProp.put("start_port", port -1);
                    else
                        tcpPingProp.put("start_port", port);
                    
                    tcpPingProp.put("is_por", isReplica);
                }
                tempMap2.put("is_inproc", isInproc);
                tempMap1.put("tcp", tempMap2);
                clusterProps.put("channel", tempMap1);
            }
            String channelProps = ConfigHelper.GetClusterPropertyString(clusterProps, userId, password, getTimeout(), isReplica);
            
            String name = channelName != null ? channelName.toLowerCase() : null;
            if (clusterProps.containsKey("group-id")) {
                name = String.valueOf(clusterProps.get("group-id"));
            }
            if (clusterProps.containsKey("sub-group-id")) {
                _subgroupid = String.valueOf(clusterProps.get("sub-group-id"));
                if (_subgroupid != null) {
                    _subgroupid = _subgroupid.toLowerCase();
                }
                identity.setSubGroupName(_subgroupid);
            } else {
                _subgroupid = name;
            }
            if (name != null) {
                name = name.toLowerCase();
            }
            if (_subgroupid != null) {
                _subgroupid = _subgroupid.toLowerCase();
            }

           
            PopulateClusterNodes(new java.util.HashMap(clusterProps));
            

            //A property or indexer may not be passed as an out or ref parameter.
            _channel = new GroupChannel(channelProps, _context.getCacheLog());
            
            java.util.HashMap config = new java.util.HashMap();
            config.put("additional_data", CompactBinaryFormatter.toByteBuffer(identity, _context.getSerializationContext()));
            _channel.down(new Event(Event.CONFIG, config));
            
            _msgDisp = new MsgDispatcher(_channel, this, this, this, this, false, true);
            _channel.connect(name + domain, _subgroupid, identity.getIsStartedAsMirror(), twoPhaseInitialization);
            localIp = getLocalAddress().getIpAddress().getHostAddress();
            _msgDisp.start();
        } catch (Exception e) {
            dispose();
            throw new ConfigurationException("Configuration Error: " + e.getMessage(), e);
        }
    }
    
    public final void ConfirmClusterStartUP(boolean isPOR, int retryNumber) {
        Object[] arg = new Object[2];
        arg[0] = isPOR;
        arg[1] = retryNumber;
        _channel.down(new Event(Event.CONFIRM_CLUSTER_STARTUP, arg));
        
    }
    
    public final void HasStarted() {
        _channel.down(new Event(Event.HAS_STARTED));
    }
    
    public final void InitializePhase2() throws ChannelClosedException, ChannelException {
        if (_channel != null) {
            _channel.connectPhase2();
        }
    }
    
    public final RspList Broadcast(Object msg, byte mode) throws IOException, ClassNotFoundException {
        return BroadcastToMultiple((java.util.List) null, msg, mode, _defOpTimeout);
    }
    
    public final RspList Broadcast(Object msg, byte mode, boolean isSeqRequired, Priority msgPriority) throws IOException, ClassNotFoundException {
        return BroadcastToMultiple((java.util.List) null, msg, mode, _defOpTimeout, isSeqRequired, "", msgPriority);
    }
    
    public final RspList Multicast(java.util.List dests, Object msg, byte mode) throws ClassNotFoundException, IOException {
        return Multicast(dests, msg, mode, true);
    }
    
    public final RspList Multicast(java.util.List dests, Object msg, byte mode, boolean isSeqRequired) throws ClassNotFoundException, IOException {
        return Multicast(dests, msg, mode, isSeqRequired, _defOpTimeout);
    }
    
    public final RspList BroadcastToCoordinators(Object msg, byte mode) throws IOException, ClassNotFoundException {
        return BroadcastToMultiple(_groupCoords, msg, mode, _defOpTimeout);
    }
    
    public final RspList BroadcastToServers(Object msg, byte mode) throws IOException, ClassNotFoundException {
        return BroadcastToMultiple(_servers, msg, mode, _defOpTimeout);
    }
    
    public final RspList BroadcastToServers(Object msg, byte mode, boolean isSeqRequired) throws IOException, ClassNotFoundException {
        return BroadcastToMultiple(_servers, msg, mode, _defOpTimeout, isSeqRequired);
    }
    
    public final RspList BroadcastToMultiple(java.util.List dests, Object msg, byte mode) throws IOException, ClassNotFoundException {
        return BroadcastToMultiple(dests, msg, mode, _defOpTimeout);
    }
    
    public final RspList BroadcastToMultiple(java.util.List dests, Object msg, byte mode, boolean isSeqRequired) throws IOException, ClassNotFoundException {
        return BroadcastToMultiple(dests, msg, mode, _defOpTimeout, isSeqRequired, "", Priority.Normal);
    }
    
    public final RspList BroadcastToMultiple(java.util.List dests, Object msg, byte mode, long timeout, boolean isSeqRequired) throws IOException, ClassNotFoundException {
        return BroadcastToMultiple(dests, msg, mode, timeout, isSeqRequired, "", Priority.Normal);
    }
    
    public final RspList BroadcastToMultiple(java.util.List dests, Object msg, byte mode, long timeout) throws IOException, ClassNotFoundException {
        return BroadcastToMultiple(dests, msg, mode, timeout, true, "", Priority.Normal);
    }
    
    private RspList BroadcastToMultiple(java.util.List dests, Object msg, byte mode, long timeout, boolean isSeqRequired, String traceMsg, Priority priority) throws IOException,
            ClassNotFoundException {
        
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("ClustService.BcastToMultiple", "");
            }
      
        
        byte[] serializedMsg = SerializeMessage(msg);
        Message m = new Message(null, null, serializedMsg);
        if (msg instanceof Function) {
            m.setPayload(((Function) msg).getUserPayload());
            m.responseExpected = ((Function) msg).getResponseExpected();
        }
        
        m.setBuffer(serializedMsg);
        m.setIsSeqRequired(isSeqRequired);
        m.setPriority(priority);
        RspList rspList = null;
        
        try {
            rspList = _msgDisp.castMessage(dests, m, mode, timeout);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("ClustService.BcastToMultiple", "completed");
            }
        }
        if (rspList.size() == 0) {
            return null;
        }
        Rsp rsp;
        for (int i = 0; i < rspList.size(); i++) {
            rsp = (Rsp) rspList.elementAt(i);
            rsp.Deflate(_context.getSerializationContext());
        }
        return rspList;
    }
    
    public final RspList BroadcastToMultiple(java.util.ArrayList dests, Object msg, byte mode, long timeout, String traceMsg, boolean handleAsync) throws IOException,
            ClassNotFoundException {
        
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("ClustService.BcastToMultiple", "");
        }
        
        byte[] serializedMsg = SerializeMessage(msg);
        Message m = new Message(null, null, serializedMsg);
        m.setHandledAysnc(handleAsync);
        m.setBuffer(serializedMsg);
        m.setIsUserMsg(true);
        if (!traceMsg.equals("")) {
            m.setTraceMsg(traceMsg);
            m.setIsProfilable(false);
        }
        RspList rspList = null;
        
        try {
            rspList = _msgDisp.castMessage(dests, m, mode, timeout);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("ClustService.BcastToMultiple", "completed");
            }
        }
        if (rspList.size() == 0) {
            return null;
        }
        Rsp rsp;
        for (int i = 0; i < rspList.size(); i++) {
            rsp = (Rsp) rspList.elementAt(i);
            rsp.Deflate(_context.getSerializationContext());
        }
        return rspList;
    }
    
    public final RspList Multicast(java.util.List dests, Object msg, byte mode, boolean isSeqRequired, long timeout) throws ClassNotFoundException, IOException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("ClustService.Mcast", "");
        }
        
        byte[] serializedMsg = SerializeMessage(msg);
        Message m = new Message(null, null, serializedMsg);
        if (msg instanceof Function) {
            m.setPayload(((Function) msg).getUserPayload());
        }
        m.setBuffer(serializedMsg);
        m.setDests(dests);
        m.setIsSeqRequired(isSeqRequired);
        
        RspList rspList = null;
        
        try {
            rspList = _msgDisp.castMessage(dests, m, mode, timeout);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("ClustService.Mcast", "completed");
            }
            
        }
        if (rspList.size() == 0) {
            return null;
        }
        Rsp rsp;
        for (int i = 0; i < rspList.size(); i++) {
            rsp = (Rsp) rspList.elementAt(i);
            rsp.Deflate(_context.getSerializationContext());
        }
        
        return rspList;
    }
    
    public final void SendResponse(Address dest, Object result, long reqId) throws Exception {
        
        try {
            
            byte[] serializedMsg = SerializeMessage(result);
            Message response = new Message(dest, null, serializedMsg);
            _msgDisp.SendResponse(reqId, response);
        } catch (Exception e) {
            throw e;
        } finally {
        }
    }
    
    protected final Object SendMessage(Address dest, Object msg, byte mode) throws SuspectedException, TimeoutException, java.io.IOException, ClassNotFoundException,
            OperationFailedException, Exception {
        return SendMessage(dest, msg, mode, _defOpTimeout);
    }
    
    protected final Object SendMessage(Address dest, Object msg, byte mode, boolean isSeqRequired) throws SuspectedException, TimeoutException, java.io.IOException,
            ClassNotFoundException, OperationFailedException, Exception {
        return SendMessage(dest, msg, mode, isSeqRequired, _defOpTimeout);
    }
    
    private byte[] SerializeMessage(Object msg) throws IOException {
        byte[] serializedMsg = null;
        serializedMsg = CompactBinaryFormatter.toByteBuffer(msg, _context.getSerializationContext());
        return serializedMsg;
    }
    
    protected final Object SendMessage(Address dest, Object msg, byte mode, long timeout) throws TimeoutException, SuspectedException, IOException, ClassNotFoundException,
            OperationFailedException, Exception {
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("ClustService.SendMsg", "dest_addr :" + dest);
            }
            
            byte[] serializedMsg = SerializeMessage(msg);
            Message m = new Message(dest, null, serializedMsg);
            if (msg instanceof Function) {
                m.setPayload(((Function) msg).getUserPayload());
                m.responseExpected = ((Function) msg).getResponseExpected();
            }
            Object result = _msgDisp.sendMessage(m, mode, timeout);
            if (result instanceof OperationResponse) {
                ((OperationResponse) result).SerializablePayload = CompactBinaryFormatter.fromByteBuffer((byte[]) ((OperationResponse) result).SerializablePayload, _context.getSerializationContext());
            } else if (result instanceof byte[]) {
                result = CompactBinaryFormatter.fromByteBuffer((byte[]) result, _context.getSerializationContext());
            }
            
            if (result != null && result instanceof Exception) {
                throw (Exception) result;
            }
            return result;
        } catch (com.alachisoft.tayzgrid.common.exceptions.SuspectedException e) {
            throw e;
        } catch (com.alachisoft.tayzgrid.common.exceptions.TimeoutException e) {
            throw e;
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("ClustService.SendMsg", "completed");
            }
        }
    }
    
    protected final Object SendMessage(Address dest, Object msg, byte mode, boolean isSeqRequired, long timeout) throws com.alachisoft.tayzgrid.common.exceptions.SuspectedException,
            TimeoutException, IOException, ClassNotFoundException,
            OperationFailedException, Exception {
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("ClustService.SendMsg", "dest_addr :" + dest);
            }
            
            byte[] serializedMsg = SerializeMessage(msg);
            Message m = new Message(dest, null, serializedMsg);
            if (msg instanceof Function) {
                m.setPayload(((Function) msg).getUserPayload());
            }
            m.setIsSeqRequired(isSeqRequired);
            
            Object result = _msgDisp.sendMessage(m, mode, timeout);
            if (result instanceof OperationResponse) {
                ((OperationResponse) result).SerializablePayload = CompactBinaryFormatter.fromByteBuffer((byte[]) ((OperationResponse) result).SerializablePayload, _context.getSerializationContext());
            } else if (result instanceof byte[]) {
                result = CompactBinaryFormatter.fromByteBuffer((byte[]) result, _context.getSerializationContext());
            }
            
            if (result != null && result instanceof Exception) {
                throw (Exception) result;
            }
            return result;
        } catch (com.alachisoft.tayzgrid.common.exceptions.SuspectedException e) {
            throw e;
        } catch (com.alachisoft.tayzgrid.common.exceptions.TimeoutException e) {
            throw e;
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("ClustService.SendMsg", "completed");
            }
        }
    }
    
    protected final Object SendMessage(Address dest, Object msg, byte mode, long timeout, boolean handleAsync) throws SuspectedException, TimeoutException, IOException,
            ClassNotFoundException, Exception {
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("ClustService.SendMsg", "dest_addr :" + dest);
            }
            
            byte[] serializedMsg = SerializeMessage(msg);
            Message m = new Message(dest, null, serializedMsg);
            m.setHandledAysnc(handleAsync);
            m.setIsUserMsg(true);
            Object result = _msgDisp.sendMessage(m, mode, timeout);
            if (result instanceof byte[]) {
                result = CompactBinaryFormatter.fromByteBuffer((byte[]) result, _context.getSerializationContext());
            }
            return result;
        } catch (com.alachisoft.tayzgrid.common.exceptions.SuspectedException e) {
            throw new com.alachisoft.tayzgrid.common.exceptions.SuspectedException();
        } catch (com.alachisoft.tayzgrid.common.exceptions.TimeoutException e) {
            throw e;
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("ClustService.SendMsg", "completed");
            }
        }
    }
    
    protected final void SendNoReplyMessage(Object msg) throws ChannelClosedException, ChannelException, IOException {
        SendNoReplyMessage(msg, _eventPriority);
    }
    
    protected final void SendNoReplyMessage(Address dest, Object msg) throws ChannelClosedException, ChannelException, IOException {
        SendNoReplyMessage(dest, msg, _eventPriority, false);
    }
    
    protected final void SendNoReplyMessage(Object msg, Priority priority) throws ChannelClosedException, ChannelException, IOException {
        SendNoReplyMessage(null, msg, priority, false);
    }
    
    protected final void SendNoReplyMessage(Object msg, Priority priority, boolean isSeqRequired) throws ChannelClosedException, ChannelException, IOException {
        SendNoReplyMessage(null, msg, priority, isSeqRequired);
    }
    
    protected final void SendNoReplyMessage(Address dest, Object msg, Priority priority, boolean isSeqRequired) throws ChannelClosedException, ChannelException, IOException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("ClustService.SendNoRepMsg", "dest_addr :" + dest);
        }
        
        byte[] serializedMsg = SerializeMessage(msg);
        Message m = new Message(dest, null, serializedMsg);
        m.setIsSeqRequired(isSeqRequired);
        m.setPriority(priority);
        _channel.send(m);
    }
    
    public final void ConfigureLocalCluster(Event configurationEvent) {
        if (_channel != null) {
            _channel.down(configurationEvent);
        }
    }
    
    public final void SendNoReplyMulticastMessage(java.util.ArrayList dest, Object msg) throws ChannelClosedException, ChannelException, IOException {
        SendNoReplyMulticastMessage(dest, msg, _eventPriority, false);
        
    }
    
    public final void SendNoReplyMulticastMessage(java.util.ArrayList dest, Object msg, boolean isSeqRequired) throws ChannelClosedException, ChannelException, IOException {
        SendNoReplyMulticastMessage(dest, msg, _eventPriority, isSeqRequired);
        
    }
    
    protected final void SendNoReplyMulticastMessage(java.util.ArrayList dest, Object msg, Priority priority, boolean isSeqRequired) throws ChannelClosedException, ChannelException,
            IOException {
        byte[] serializedMsg = SerializeMessage(msg);
        Message m = new Message(null, null, serializedMsg);
        m.setIsSeqRequired(isSeqRequired);
        m.setPriority(priority);
        m.setDests(dest);
        _channel.send(m);
    }
    
    protected void SendNoReplyMcastMessageAsync(java.util.ArrayList dests, Object msg) {
        _context.AsyncProc.Enqueue(new AsyncMulticastCast(dests, this, msg));
    }
    
    protected void SendNoReplyMessageAsync(Object msg) {
        _context.AsyncProc.Enqueue(new AsyncBroadCast(this, msg));
    }
    
    protected void SendNoReplyMessageAsync(Address dest, Object msg) {
        _context.AsyncProc.Enqueue(new AsyncUnicasCast(dest, this, msg));
    }
    
    private boolean AuthenticateNode(Address address, NodeIdentity identity) {
        return _participant.AuthenticateNode(address, identity);
    }
    
    public void notifyLeaving() {
        if (_channel != null) {
            _channel.down(new Event(Event.NOTIFY_LEAVING));
        }
    }
    
    private void OnAfterMembershipChange() throws InterruptedException, OperationFailedException {
        _groupCoords = new java.util.ArrayList();
        synchronized (_subgroups) {
            for (java.util.Iterator i = _subgroups.values().iterator(); i.hasNext();) {
                SubCluster group = (SubCluster) i.next();
                if (group.getCoordinator() != null) {
                    _groupCoords.add(group.getCoordinator());
                }
            }
        }
        _otherServers = (java.util.List) GenericCopier.DeepCopy(_servers);
        _otherServers.remove(getLocalAddress());
        
        _participant.OnAfterMembershipChange();
    }
    
    private boolean OnMemberJoined(Address address, NodeIdentity identity, java.util.ArrayList joiningNowList) {
        try {
            if (!AuthenticateNode(address, identity)) {
                getCacheLog().Warn("ClusterService.OnMemberJoined()", "A non-server attempted to join cluster -> " + address);
                _validMembers.remove(address);
                _servers.remove(address);
                return false;
            }
            
            SubCluster group = null;
            if (identity.getHasStorage() && identity.getSubGroupName() != null) {
                synchronized (_subgroups) {
                    group = GetSubCluster(identity.getSubGroupName());
                    if (group == null) {
                        if (getCacheLog().getIsInfoEnabled()) {
                            getCacheLog().Info("ClusterService.OnMemberJoined()", "Formed new sub-cluster -> " + identity.getSubGroupName());
                        }
                        group = new SubCluster(identity.getSubGroupName(), this);
                        _subgroups.put(identity.getSubGroupName(), group);
                    }
                    group.OnMemberJoined(address, identity);
                }
            }
            boolean joined = _participant.OnMemberJoined(address, identity);
            if (!joined && group != null) {
                group.OnMemberLeft(address, _distributionPolicyMbr.getBucketsOwnershipMap());
            }
            
            if (joined) {
                getCacheLog().CriticalInfo("ClusterService.OnMemberJoined()", "Member joined: " + address);
                
                Address renderer = new Address(identity.getRendererAddress(), identity.getRendererPort());
                
                String mirrorExplaination = identity.getIsStartedAsMirror() ? " (replica)" : "";
                
                if (joiningNowList.contains(address) && !_context.getIsStartedAsMirror() && !address.equals(getLocalAddress())) {
                    if (_context.getCacheRoot().EmailAlertPropagator != null) {
                        _context.getCacheRoot().EmailAlertPropagator.RaiseAlert(EventID.NodeJoined, "NCache", "Node \"" + address + mirrorExplaination + "\" has joined to \""
                                + _context.getCacheRoot().getName() + "\".");
                    }
                    EventLogger.LogEvent("TayzGrid", "Node \"" + address + mirrorExplaination + "\" has joined to \"" + _context.getCacheRoot().getName() + "\".", EventType.INFORMATION, EventCategories.Information, EventID.NodeJoined);
                }
                
                if (!_membersRenders.containsKey(address)) {
                    _membersRenders.put(address, renderer);
                    
                    if (_listener != null && !identity.getIsStartedAsMirror()) {
                        _listener.OnMemberJoined(address, renderer);
                    }
                }
            }
            return joined;
        } catch (Exception exception) {
            getCacheLog().Error("ClusterService.OnMemberJoined", exception.toString());
        }
        return false;
    }
    
    private boolean OnMemberLeft(Address address, NodeIdentity identity) {
        if (_validMembers.contains(address)) {
            getCacheLog().CriticalInfo("ClusterService.OnMemberLeft()", "Member left: " + address);
            if (identity.getHasStorage() && identity.getSubGroupName() != null) {
                synchronized (_subgroups) {
                    SubCluster group = GetSubCluster(identity.getSubGroupName());
                    if (group != null) {
                        if (group.OnMemberLeft(address, _distributionPolicyMbr.getBucketsOwnershipMap()) < 1) {
                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("ClusterService.OnMemberLeft()", "Destroyed sub-cluster -> " + identity.getSubGroupName());
                            }
                            _subgroups.remove(identity.getSubGroupName());
                        }
                    }
                }
            }
            if (_membersRenders.containsKey(address)) {
                Address renderer = (Address) _membersRenders.get(address);
                _membersRenders.remove(address);
                
                if (_listener != null && !identity.getIsStartedAsMirror()) // invisible replica's don't raise events.
                {
                    _listener.OnMemberLeft(address, renderer);
                }
            }
            
            String mirrorExplaination = identity.getIsStartedAsMirror() ? " (replica)" : "";
            
            if (!_context.getIsStartedAsMirror()) {
                if (_context.getCacheRoot().EmailAlertPropagator != null) {
                    _context.getCacheRoot().EmailAlertPropagator.RaiseAlert(EventID.NodeLeft, "NCache", "Node \"" + address + mirrorExplaination + "\" has left \""
                            + _context.getCacheRoot().getName() + "\".");
                }
                EventLogger.LogEvent("TayzGrid", "Node \"" + address + mirrorExplaination + "\" has left \"" + _context.getCacheRoot().getName() + "\".", EventType.WARNING, EventCategories.Warning, EventID.NodeLeft);
            }
            
            return _participant.OnMemberLeft(address, identity);
        }
        return false;
    }
    
    @Override
    public void viewAccepted(View newView) throws InterruptedException, Exception {
        try {
            java.util.List joined_mbrs, left_mbrs, tmp;
            java.util.ArrayList joining_mbrs = new java.util.ArrayList();
            
            synchronized (viewMutex) {
                Object tmp_mbr;
                
                if (newView == null) {
                    return;
                }
                
                getCacheLog().CriticalInfo("ClusterService.ViewAccepted", newView.toString());
                tmp = newView.getMembers();
                
                if (newView.getVid() != null) {
                    this._lastViewId = newView.getVid().getId();
                }

                // get new members
                joined_mbrs = Collections.synchronizedList(new java.util.ArrayList(10));
                
                for (int i = 0; i < tmp.size(); i++) {
                    tmp_mbr = tmp.get(i);
                    if (!_members.contains(tmp_mbr)) {
                        joined_mbrs.add(tmp_mbr);
                    }
                }
                int localIndex = 0;
                
                if (joined_mbrs.contains(getLocalAddress())) {
                    localIndex = joined_mbrs.indexOf(getLocalAddress());
                }
                
                for (int i = localIndex; i < joined_mbrs.size(); i++) {
                    joining_mbrs.add(joined_mbrs.get(i));
                }

                // get members that left
                left_mbrs = Collections.synchronizedList(new java.util.ArrayList(10));
                for (int i = 0; i < _members.size(); i++) {
                    tmp_mbr = _members.get(i);
                    if (!tmp.contains(tmp_mbr)) {
                        left_mbrs.add(tmp_mbr);
                    }
                }

                // adjust our own membership
                _members.clear();
                _members.addAll(tmp);

                //pick the map from the view and send it to cache.
                //if i am the only member, i can build the map locally.
                if (newView.getDistributionMaps() == null && newView.getMembers().size() == 1) {
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ClusterService.viewAccepted()", "I am the only member in the view so, building map myself");
                    }
                    PartNodeInfo affectedNode = new PartNodeInfo(getLocalAddress(), _subgroupid, true);
                    affectedNode.setPartitionId(newView.getPartition(getLocalAddress()));
                    DistributionInfoData info = new DistributionInfoData(DistributionMode.OptimalWeight, ClusterActivity.NodeJoin, affectedNode);
                    DistributionMaps maps = _distributionPolicyMbr.GetDistributionMaps(info);
                    if (maps != null) {
                        _distributionPolicyMbr.setHashMap(maps.getHashmap());
                        _distributionPolicyMbr.setBucketsOwnershipMap(maps.getBucketsOwnershipMap());
                    }
                } else {
                    if (newView.getMirrorMapping() != null) {
                        _distributionPolicyMbr.InstallMirrorMap(newView.getMirrorMapping());
                        getCacheLog().Info("ClusterService.viewAccepted()", "New MirrorMap installed.");
                    }
                    if (newView.getDistributionMaps() != null) {
                        _distributionPolicyMbr.InstallHashMap(newView.getDistributionMaps(), left_mbrs);
                        if (getCacheLog().getIsInfoEnabled()) {
                            getCacheLog().Info("ClusterService.viewAccepted()", "New hashmap installed");
                        }
                    }
                }
                
                synchronized (_servers) {
                    if (left_mbrs.size() > 0) {
                        for (int i = left_mbrs.size() - 1; i >= 0; i--) {
                            Address ipAddr = (Address) ((Address) left_mbrs.get(i));
                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("ClusterService.viewAccepted", Integer.toString(ipAddr.getAdditionalData().length));
                            }
                            ipAddr = (Address) ipAddr.clone();
                            if (_servers.contains(ipAddr)) {
                                _servers.remove(ipAddr);
                            }
                            
                            Object tempVar3 = null;
                            try {
                                tempVar3 = CompactBinaryFormatter.fromByteBuffer(ipAddr.getAdditionalData(), _context.getSerializationContext());
                            } catch (IOException iOException) {
                            } catch (ClassNotFoundException classNotFoundException) {
                            }
                            OnMemberLeft(ipAddr, (NodeIdentity) ((tempVar3 instanceof NodeIdentity) ? tempVar3 : null));
                            ipAddr.setAdditionalData(null);
                        }
                    }
                    
                    _validMembers = (java.util.List) GenericCopier.DeepCopy(_members);
                    if (getCacheLog().getIsInfoEnabled()) {
                        getCacheLog().Info("ClusterService.viewAccepted", Integer.toString(joining_mbrs.size()));
                    }
                    
                    if (joined_mbrs.size() > 0) {
                        for (int i = 0; i < joined_mbrs.size(); i++) {
                            Address ipAddr = (Address) joined_mbrs.get(i);
                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("ClusterService.viewAccepted", Integer.toString(ipAddr.getAdditionalData().length));
                            }
                            ipAddr = (Address) ipAddr.clone();
                            
                            Object tempVar4 = null;
                            try {
                                tempVar4 = CompactBinaryFormatter.fromByteBuffer(ipAddr.getAdditionalData(), _context.getSerializationContext());
                            } catch (IOException iOException) {
                                getCacheLog().Error("ClusterServices.ViewAccepted", ipAddr.toString() + " " + iOException.getMessage());
                            } catch (ClassNotFoundException classNotFoundException) {
                                getCacheLog().Error("ClusterServices.ViewAccepted", ipAddr.toString() + " " + classNotFoundException.getMessage());
                            }
                            if (!OnMemberJoined(ipAddr, (NodeIdentity) ((tempVar4 instanceof NodeIdentity) ? tempVar4 : null), joining_mbrs)) {
                                
                                    if (!getLocalAddress().equals(ipAddr)) {
                                        _servers.remove(ipAddr);
                                    }
                               
                            } else {
                                if (getCacheLog().getIsInfoEnabled()) {
                                    getCacheLog().Info("ClusterServices.ViewAccepted", ipAddr.toString() + " is added to _servers list.");
                                }
                                _servers.add(ipAddr);
                            }
                            ipAddr.setAdditionalData(null);
                        }
                    }
                }
                if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(_bridgeSourceCacheId)) {
                    _bridgeSourceCacheId = newView.getBridgeSourceCacheId();
                }
                OnAfterMembershipChange();
            }
        } catch (Exception e) {
            throw e;
        }
    }
    
    @Override
    public void suspect(Address suspected_mbr) {
    }
    
    @Override
    public void block() {
    }
    
    @Override
    public boolean AllowJoin() {
        return !_participant.IsInStateTransfer();
    }
    
    @Override
    public void receive(Message msg) {
        ((RequestHandler) this).handle(msg);
    }
    
    public final Object handleFunction(Address src, Function func) throws OperationFailedException, ClassNotFoundException, GeneralFailureException,
            IOException, StateTransferException,  
            LockingException, CacheException, java.lang.Exception {
        return _participant.HandleClusterMessage(src, func);
    }
    
    public final Object handleFunction(Address src, Function func, tangible.RefObject<Address> destination, tangible.RefObject<Message> replicationMsg) throws
            OperationFailedException, GeneralFailureException, StateTransferException, LockingException, Exception, CacheException, SuspectedException {
        return _participant.HandleClusterMessage(src, func, destination, replicationMsg);
    }
    
    @Override
    public Object handle(Message req) {
        if (req == null || req.getLength() == 0) {
            return null;
        }
        try {
            boolean isLocalReq = getLocalAddress().compareTo(req.getSrc()) == 0;
            Object body = req.getFlatObject();
            try {
                if (body instanceof byte[]) {
                    body = CompactBinaryFormatter.fromByteBuffer((byte[]) body, _context.getSerializationContext());
                }
            } catch (java.io.IOException io) {
                throw new Exception(io.getMessage());
            } catch (Exception e) {
                return e;
            }
            Object result = null;
            if (body instanceof Function) {
                Function func = (Function) body;
                func.setUserPayload(req.getPayload());
                if (isLocalReq && func.getExcludeSelf()) {
                    if (req.getHandledAysnc() && req.getRequestId() > 0) {
                        SendResponse(req.getSrc(), null, req.getRequestId());
                    }
                    return null;
                }
                if (req.getHandledAysnc()) {
                    AsyncRequest asyncReq = new AsyncRequest(func, func.getSyncKey());
                    asyncReq.setSrc(req.getSrc());
                    asyncReq.setRequsetId(req.getRequestId());
                    _asynHandler.HandleRequest(asyncReq);
                    return null;
                } else {
                    result = handleFunction(req.getSrc(), func);
                }
            } else if (body instanceof AggregateFunction) {
                AggregateFunction funcs = (AggregateFunction) body;
                Object[] results = new Object[funcs.getFunctions().length];
                for (int i = 0; i < results.length; i++) {
                    Function func = (Function) funcs.getFunctions()[i];
                    if (isLocalReq && func.getExcludeSelf()) {
                        if (req.getHandledAysnc() && req.getRequestId() > 0) {
                            SendResponse(req.getSrc(), null, req.getRequestId());
                            continue;
                        }
                        results[i] = null;
                    } else {
                        if (req.getHandledAysnc()) {
                            AsyncRequest asyncReq = new AsyncRequest(func, func.getSyncKey());
                            asyncReq.setSrc(req.getSrc());
                            asyncReq.setRequsetId(req.getRequestId());
                            _asynHandler.HandleRequest(asyncReq);
                            continue;
                        }
                        results[i] = handleFunction(req.getSrc(), func);
                    }
                }
                result = results;
            }
            
            if (result instanceof OperationResponse) {
                ((OperationResponse) result).SerializablePayload = CompactBinaryFormatter.toByteBuffer(((OperationResponse) result).SerializablePayload, _context.getSerializationContext());
            } else {
                result = CompactBinaryFormatter.toByteBuffer(result, _context.getSerializationContext());
            }
            return result;
        } catch (Exception e) {
            return e;
        }
    }
    
    @Override
    public Object handleNHopRequest(Message req, tangible.RefObject<Address> destination, tangible.RefObject<Message> replicationMsg) {
        destination.argvalue = null;
        replicationMsg.argvalue = null;
        
        if (req == null || req.getLength() == 0) {
            return null;
        }
        
        try {
            boolean isLocalReq = getLocalAddress().compareTo(req.getSrc()) == 0;
            Object body = req.getFlatObject();
            try {
                if (body instanceof byte[]) {
                    body = CompactBinaryFormatter.fromByteBuffer((byte[]) body, _context.getSerializationContext());
                }
            } catch (java.io.IOException io) {
            } catch (Exception e) {
                return e;
            }
            
            Object result = null;
            if (body instanceof Function) {
                Function func = (Function) body;
                func.setUserPayload(req.getPayload());
                if (isLocalReq && func.getExcludeSelf()) {
                    if (req.getHandledAysnc() && req.getRequestId() > 0) {
                        SendResponse(req.getSrc(), null, req.getRequestId());
                    }
                    return null;
                }
                if (req.getHandledAysnc()) {
                    AsyncRequest asyncReq = new AsyncRequest(func, func.getSyncKey());
                    asyncReq.setSrc(req.getSrc());
                    asyncReq.setRequsetId(req.getRequestId());
                    _asynHandler.HandleRequest(asyncReq);
                    return null;
                } else {
                    result = handleFunction(req.getSrc(), func, destination, replicationMsg);
                }
            } else if (body instanceof AggregateFunction) {
                AggregateFunction funcs = (AggregateFunction) body;
                Object[] results = new Object[funcs.getFunctions().length];
                for (int i = 0; i < results.length; i++) {
                    Function func = (Function) funcs.getFunctions()[i];
                    if (isLocalReq && func.getExcludeSelf()) {
                        if (req.getHandledAysnc() && req.getRequestId() > 0) {
                            SendResponse(req.getSrc(), null, req.getRequestId());
                            continue;
                        }
                        results[i] = null;
                    } else {
                        if (req.getHandledAysnc()) {
                            AsyncRequest asyncReq = new AsyncRequest(func, func.getSyncKey());
                            asyncReq.setSrc(req.getSrc());
                            asyncReq.setRequsetId(req.getRequestId());
                            _asynHandler.HandleRequest(asyncReq);
                            continue;
                        }
                        results[i] = handleFunction(req.getSrc(), func);
                    }
                }
                result = results;
            }
            
            if (result instanceof OperationResponse) {
                ((OperationResponse) result).SerializablePayload = CompactBinaryFormatter.toByteBuffer(((OperationResponse) result).SerializablePayload, _context.getSerializationContext());
            } else {
                result = CompactBinaryFormatter.toByteBuffer(result, _context.getSerializationContext());
            }
            
            return result;
        } catch (java.io.IOError io) {
            return io;
        } catch (Exception e) {
            return e;
        }
    }
    
    public final void StopServices() {
        if (_msgDisp != null) {
            _msgDisp.StopReplying();
        }
    }
    
    public final void MarkClusterInStateTransfer() {
        _channel.down(new Event(Event.MARK_CLUSTER_IN_STATETRANSFER));
    }
    
    public final void MarkClusterStateTransferCompleted() {
        _channel.down(new Event(Event.MARK_CLUSTER_STATETRANSFER_COMPLETED));
    }
    
    @Override
    public final Object GetDistributionAndMirrorMaps(Object data) {
        getCacheLog().Debug("MessageResponder.GetDistributionAndMirrorMaps()", "here comes the request for hashmap");
        
        Object[] package_Renamed = (Object[]) ((data instanceof Object[]) ? data : null);
        java.util.ArrayList members = (java.util.ArrayList) ((package_Renamed[0] instanceof java.util.ArrayList) ? package_Renamed[0] : null);
        boolean isJoining = (Boolean) package_Renamed[1];
        String subGroup = (String) package_Renamed[2];
        boolean isStartedAsMirror = (Boolean) package_Renamed[3];
        String partitionId = (String)package_Renamed[4];
        
        ClusterActivity activity = ClusterActivity.None;
        activity = isJoining ? ClusterActivity.NodeJoin : ClusterActivity.NodeLeave;
        
        DistributionMaps maps = null;
        {
            PartNodeInfo affectedNode = new PartNodeInfo((Address) members.get(0), subGroup, !isStartedAsMirror);
             affectedNode.setPartitionId(partitionId);
            DistributionInfoData info = new DistributionInfoData(DistributionMode.OptimalWeight, activity, affectedNode);
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("ClusterService.GetDistributionMaps", "NodeAddress: " + info.getAffectedNode().getNodeAddress().toString() + " subGroup: " + subGroup
                        + " isMirror: " + (new Boolean(isStartedAsMirror)).toString() + " " + (isJoining ? "joining" : "leaving"));
            }
            maps = _distributionPolicyMbr.GetDistributionMaps(info);
        }
        
        CacheNode[] mirrors = _distributionPolicyMbr.GetMirrorMap();
        getCacheLog().Debug("MessageResponder.GetDistributionAndMirrorMaps()", "sending hashmap response back...");
        
        return new Object[]{
            maps,
            mirrors
        };
    }
    
    public final void PopulateClusterNodes(java.util.HashMap clusterProps) {
        
            HashMap nodeList = new HashMap();
            try {
                String x = null;
                
                nodeList = (HashMap) ((clusterProps.get("channel") instanceof HashMap) ? clusterProps.get("channel") : null);
                nodeList = (HashMap) ((nodeList.get("tcpping") instanceof HashMap) ? nodeList.get("tcpping") : null);
                if (nodeList.containsKey("initial_hosts")) {
                    x = String.valueOf(nodeList.get("initial_hosts"));
                }

                // For Partition Of Replicas
                nodeList = (HashMap) ((clusterProps.get("channel") instanceof HashMap) ? clusterProps.get("channel") : null);
                if (nodeList.containsKey("partitions")) {
                    nodeList = (HashMap) ((nodeList.get("partitions") instanceof HashMap) ? nodeList.get("partitions") : null);
                }
                
            } catch (Exception e) {
                getCacheLog().Error(e.getMessage());
            }
        
    }
    
    private String ExtractCacheName(String cacheName) {
       
            if (cacheName.toUpperCase().indexOf("_BK_") != -1) {
                return cacheName.substring(0, cacheName.toUpperCase().indexOf("_BK")) + cacheName.substring(cacheName.toUpperCase().indexOf("_BK") + cacheName.length()
                        - cacheName.toUpperCase().indexOf("_BK"));
            } else {
                return cacheName;
            }
        
    }
}
