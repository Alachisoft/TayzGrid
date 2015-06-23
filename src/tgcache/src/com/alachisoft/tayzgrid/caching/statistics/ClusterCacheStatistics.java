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
package com.alachisoft.tayzgrid.caching.statistics;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.Iterator;

/**
 * Info class that holds statistics related to cluster.
 */
public class ClusterCacheStatistics extends CacheStatistics implements Cloneable, ICompactSerializable {

    /**
     * The name of the group participating in the cluster.
     */
    private String _groupName;
    /**
     * The name of the group participating in the cluster.
     */
    private String _channelType;
    /**
     * The number of nodes in the cluster that are providers or consumers, i.e.,
     * group members.
     */
    private int _memberCount;
    /**
     * The number of nodes in the cluster that are storage enabled.
     */
    private int _serverCount;
    /**
     * The number of nodes in the cluster that are unknown, i.e., different
     * cache scheme.
     */
    private int _otherCount;
    /**
     * The statistics of the local node.
     */
    private NodeInfo _localNode;
    /**
     * The statistics of individual nodes.
     */
    private java.util.List _nodeInfos;
    /**
     * Data affinity of the entire cluster.
     */
    private java.util.ArrayList _clusterDataAffinity = new java.util.ArrayList();
    /**
     * A map that gives the list of all data groups mapped at the node.
     */
    private java.util.HashMap _datagroupsAtPartition = new java.util.HashMap();
    /**
     * A map that gives the list of all nodes that have mapping for a group.
     */
    private java.util.HashMap _partitionsHavingDatagroup = new java.util.HashMap();
    /**
     * A map that gives the list of all nodes belonging to the same subgroup
     * i.e. partition.
     */
    private java.util.HashMap _subgroupNodes = new java.util.HashMap();

    /**
     * Constructor.
     */
    public ClusterCacheStatistics() {
    }

    /**
     * Copy constructor.
     *
     * @param stat
     */
    protected ClusterCacheStatistics(ClusterCacheStatistics stat) throws CloneNotSupportedException {
        super(stat);
        synchronized (stat) {
            this._groupName = stat._groupName;
            this._channelType = stat._channelType;
            this._memberCount = stat._memberCount;
            this._serverCount = stat._serverCount;
            this._otherCount = stat._otherCount;
            Object tempVar = stat._localNode == null ? null : stat._localNode.clone();
            this._localNode = stat._localNode == null ? null : (NodeInfo) ((tempVar instanceof NodeInfo) ? tempVar : null);
            if (stat._nodeInfos != null) {
                this._nodeInfos = new java.util.ArrayList(stat._nodeInfos.size());
                for (int i = 0; i < stat._nodeInfos.size(); i++) {
                    Object tempVar2 = ((NodeInfo) stat._nodeInfos.get(i)).clone();
                    this._nodeInfos.add((NodeInfo) ((tempVar2 instanceof NodeInfo) ? tempVar2 : null));
                }
            }
            if (stat.getClusterDataAffinity() != null) {
                this.setClusterDataAffinity((java.util.ArrayList) stat.getClusterDataAffinity().clone());
            }

            if (stat.getPartitionsHavingDatagroup() != null) {
                this.setPartitionsHavingDatagroup((java.util.HashMap) stat.getPartitionsHavingDatagroup().clone());
            }

            if (stat.getDatagroupsAtPartition() != null) {
                this.setDatagroupsAtPartition((java.util.HashMap) stat.getDatagroupsAtPartition().clone());
            }

            if (stat.getSubgroupNodes() != null) {
                this.setSubgroupNodes((java.util.HashMap) stat.getSubgroupNodes().clone());
            }
        }
    }

    @Override
    public long getMaxSize() {
        return this.getLocalNode().getStatistics().getMaxSize();
    }

    @Override
    public void setMaxSize(long value) {
        if (this.getLocalNode() != null) {
            this.getLocalNode().getStatistics().setMaxSize(value);
        }
    }

    /**
     * The name of the group participating in the cluster.
     */
    public final String getGroupName() {
        return _groupName;
    }

    public final void setGroupName(String value) {
        _groupName = value;
    }

    /**
     * The clustering scheme.
     */
    public final String getChannelType() {
        return _channelType;
    }

    public final void setChannelType(String value) {
        _channelType = value;
    }

    /**
     * The total number of nodes in the cluster.
     */
    public final int getClusterSize() {
        return getMemberCount() + getOtherCount();
    }

    /**
     * The number of nodes in the cluster that are providers or consumers, i.e.,
     * group members.
     */
    public final int getMemberCount() {
        return _memberCount;
    }

    public final void setMemberCount(int value) {
        _memberCount = value;
    }

    /**
     * The number of nodes in the cluster that are storage enabled.
     */
    public final int getServerCount() {
        return _serverCount;
    }

    public final void setServerCount(int value) {
        _serverCount = value;
    }

    /**
     * The number of nodes in the cluster that are unknown, i.e., different
     * cache scheme.
     */
    public final int getOtherCount() {
        return _otherCount;
    }

    public final void setOtherCount(int value) {
        _otherCount = value;
    }

    /**
     * The statistics of the local node.
     */
    public final NodeInfo getLocalNode() {
        return _localNode;
    }

    public final void setLocalNode(NodeInfo value) {
        _localNode = value;
    }

    /**
     * The statistics of the local node.
     */
    public final java.util.List getNodes() {
        return _nodeInfos;
    }

    public final void setNodes(java.util.List value) {
        _nodeInfos = value;
    }

    /**
     * Gets/Sets teh data affinity of the cluster.
     *
     * @return
     */
    public final java.util.ArrayList getClusterDataAffinity() {
        return _clusterDataAffinity;
    }

    public final void setClusterDataAffinity(java.util.ArrayList value) {
        _clusterDataAffinity = value;
    }

    public final java.util.HashMap getDatagroupsAtPartition() {
        return _datagroupsAtPartition;
    }

    public final void setDatagroupsAtPartition(java.util.HashMap value) {
        _datagroupsAtPartition = value;
    }

    public final java.util.HashMap getPartitionsHavingDatagroup() {
        return _partitionsHavingDatagroup;
    }

    public final void setPartitionsHavingDatagroup(java.util.HashMap value) {
        _partitionsHavingDatagroup = value;
    }

    public final java.util.HashMap getSubgroupNodes() {
        return _subgroupNodes;
    }

    public final void setSubgroupNodes(java.util.HashMap value) {
        _subgroupNodes = value;
    }

    /**
     * Creates a new object that is a copy of the current instance.
     *
     * @return A new object that is a copy of this instance.
     */
    @Override
    public Object clone() {
        try {
            return new ClusterCacheStatistics(this);
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            return null;
        }
    }

    /**
     * Adds a new node to the node list
     *
     * @param info
     */
    public final NodeInfo GetNode(Address address) {
        synchronized (this) {
            if (_nodeInfos != null) {
                for (int i = 0; i < _nodeInfos.size(); i++) {
                    if (((NodeInfo) _nodeInfos.get(i)).getAddress().compareTo(address) == 0) {
                        return (NodeInfo) ((_nodeInfos.get(i) instanceof NodeInfo) ? _nodeInfos.get(i) : null);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Sets the values of the server/member/other counts
     *
     * @param server
     * @param member
     * @param other
     */
    public final void SetServerCounts(int servCnt, int memCnt, int otherCnt) {
        synchronized (this) {
            _serverCount = servCnt;
            _memberCount = memCnt;
            _otherCount = otherCnt;
        }
    }

    /**
     * returns the string representation of the statistics.
     *
     * @return
     */
    @Override
    public String toString() {
        synchronized (this) {
            StringBuilder ret = new StringBuilder();
            ret.append("Cluster[" + super.toString() + ", Nm:" + getGroupName() + ", ");
            ret.append("S:" + (new Integer(getServerCount())).toString() + ", ");
            ret.append("M:" + (new Integer(getMemberCount())).toString() + ", ");
            ret.append("O:" + (new Integer(getOtherCount())).toString() + ", ");
            if (_localNode != null) {
                ret.append("Local" + _localNode.toString());
            }
            for (Iterator it = _nodeInfos.iterator(); it.hasNext();) {
                NodeInfo i = (NodeInfo) it.next();
                ret.append(", " + i.toString());
            }
            ret.append("]");
            return ret.toString();
        }
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
        super.deserialize(reader);
        _groupName = (String) reader.readObject();
        _channelType = (String) reader.readObject();
        _memberCount = reader.readInt();
        _serverCount = reader.readInt();
        _otherCount = reader.readInt();

        _localNode = NodeInfo.ReadNodeInfo(reader);

        _nodeInfos = (java.util.ArrayList) reader.readObject();
    }

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException {
        super.serialize(writer);
        writer.writeObject(_groupName);
        writer.writeObject(_channelType);
        writer.writeInt(_memberCount);
        writer.writeInt(_serverCount);
        writer.writeInt(_otherCount);  
        if (_localNode != null) {
            writer.writeBoolean(false);
            _localNode.serialize(writer);
        } else {
            writer.writeBoolean(true);
        }
        writer.writeObject(_nodeInfos);
    }
}
