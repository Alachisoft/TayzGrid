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

package com.alachisoft.tayzgrid.caching.topologies.clustered.mirroring;

import com.alachisoft.tayzgrid.common.mirroring.GroupInfo;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.mirroring.CacheNode;
import java.util.LinkedHashMap;

/**
 * This class manages the linked list of cache nodes and their mirrors.
 * <p>
 * We just add and remove the node identifiers of nodes and the manager
 * automatically generates the group id for the nodes and associates mirrors
 * with each node.
 *
 * GroupInfo of any cache node can be obtained from the GetGroupInfo method.
 *
 * MirrorManager on the Coordinator node is active and handles all the
 * managment. Other nodes in the cluster have passive copies of the same mirror
 * manager and is activated incase coordinator goes down. </p>
 */
public class MirrorManager implements Iterable {

    private LinkedHashMap nodesList;
    private CacheNode lastNode;
    private CacheNode firstNode;
    private ILogger _ncacheLog;

    private ILogger getCacheLog() {
        return _ncacheLog;
    }

    private int _sequenceSeed = 1;

    public MirrorManager(ILogger NCacheLog) {
        nodesList = new LinkedHashMap();
        firstNode = null;
        lastNode = null;
        _ncacheLog = NCacheLog;
    }

    public final void Update(CacheNode[] nodes) {
        if (nodes != null) {
            getCacheLog().CriticalInfo("MirrorManager.Update()", "Number of nodes " + Integer.toString(nodes.length));
        }
        int maxSeq = 0;
        nodesList.clear();
        firstNode = nodes[0];
        lastNode = (nodes.length > 1) ? nodes[nodes.length - 1] : firstNode;
        for (CacheNode node : nodes) {
            getCacheLog().CriticalInfo("MirrorManager.Update()", "Node: " + node.toString());
            if (node.getSequence() > maxSeq) {
                maxSeq = node.getSequence();
            }
            nodesList.put(node.getNodeId(), node);
        }
        if (maxSeq + 1 > this.getSequenceSeed()) {
            this.setSequenceSeed(maxSeq + 1);
        }
    }

    private int GetNextSequence() {
        return _sequenceSeed++;
    }

    /**
     * This sequence seed generates a unique sequence number for each cache
     * node. It helps in associating group with a cache node. The group of a
     * node-mirror pair is basically the sequence of the active node.
     * <p>
     * When some other node in the cluster becomes co-ordinator then it might
     * not have the seed for the cachenode class in such a case it should be
     * initialized to the 1 + maximum group id in the cluster </p>
     */
    public final int getSequenceSeed() {
        return _sequenceSeed;
    }

    public final void setSequenceSeed(int value) {
        _sequenceSeed = value;
    }

    public final CacheNode getItem(String nodeId) {
        return (CacheNode) nodesList.get(nodeId);
    }

    public final CacheNode getFirstNode() {
        return firstNode;
    }

    public final CacheNode getLastNode() {
        return lastNode;
    }

    /**
     * Adds a new node to the list and re-adjusts the mirrors
     *
     * @param nodeId The identifier for the new CacheNode. IP Adrress or Machine
     * Name
     * @return The newly added node
     */
    public final void AddNode(String nodeId) {
        getCacheLog().CriticalInfo("MirrorManager.AddNode", nodeId);
        if (nodesList.containsKey(nodeId)) {
            CacheNode node = (CacheNode) ((nodesList.get(nodeId) instanceof CacheNode) ? nodesList.get(nodeId) : null);

            try {
                // it is observed that sometimes a previous node with same id but different sequence
                //remains in the list may be because of ungraceful node left. remove that node now and add new
                //node.
                RemoveNode(nodeId);
            } catch (Exception ex) {
                getCacheLog().Error("MirrorManager.AddNode", ex.toString());
                return;
            }
        }
        if (firstNode == null) {

            lastNode = firstNode = new CacheNode(nodeId, GetNextSequence());
            firstNode.setPreviousNodeId(firstNode.getNodeId());
            firstNode.setBackupNodeId(firstNode.getNodeId());
            nodesList.put(nodeId, firstNode);
            getCacheLog().Debug("MirrorManager.AddNode", "After : First Node null" + firstNode.toString());
        } else {
            getCacheLog().Debug("MirrorManager.AddNode", "Before: First Node not null-:affectedNode" + getLastNode().toString());
            CacheNode newNode = new CacheNode(nodeId, GetNextSequence());
            newNode.setBackupNodeId(firstNode.getNodeId());
            String lastBackup = lastNode.getBackupNodeId();
            lastNode.setBackupNodeId(newNode.getNodeId());
            CacheNode affectedNode = (CacheNode) getLastNode().clone();
            newNode.setPreviousNodeId(lastNode.getNodeId());
            nodesList.put(nodeId, newNode);
            lastNode = newNode;
            firstNode.setPreviousNodeId(lastNode.getNodeId());

            getCacheLog().Debug("MirrorManager.AddNode", "After : First Node not null-:affectedNode" + affectedNode.toString());
        }
    }

    /**
     * Returns the GroupInfo object for a specific cache node that is part of
     * the cluster.
     *
     * @param nodeId The identifier of the cache node
     * @return GroupInfo structure for this nodeId.
     */
    public final GroupInfo GetGroupInfo(String nodeId) {
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("MirrorManager.GetGroupInfo", nodeId);
            getCacheLog().Info("MirrorManager.GetGroupInfo", "firstNode->" + (firstNode == null ? "null" : firstNode.getNodeId()));
        }
        if (nodeId != null && nodeId.equals(firstNode.getNodeId()) && nodesList.size() == 1) {
            int sequence = this.getItem(nodeId).getSequence();
            GroupInfo grpInfo = new GroupInfo("group_" + sequence, "group_" + (sequence + 1));
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("MirrorManager.GetGroupInfo", "Count == 1" + grpInfo.toString());
            }
            return grpInfo;
        } else {
            CacheNode resultNode = this.getItem(nodeId);
            if (resultNode != null) {
                GroupInfo grpInfo = new GroupInfo("group_" + (new Integer(resultNode.getSequence())).toString(), "group_"
                        + (new Integer(this.getItem(resultNode.getPreviousNodeId()).getSequence())).toString());
                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("MirrorManager.GetGroupInfo", "resultNode->" + grpInfo.toString());
                }
                return grpInfo;
            }
        }
        getCacheLog().Error("MirrorManager.GetGroupInfo", "return null:" + nodeId);
        return null;
    }

    /**
     * Removes a cache node from the list and automaticalluy re-adjusts the
     * mirros.
     *
     * @param nodeId the identifier of the cache node to be removed/param>
     */
    public final void RemoveNode(String nodeId) {
        getCacheLog().CriticalInfo("MirrorManager.RemoveNode", nodeId);
        CacheNode node = (CacheNode) nodesList.get(nodeId);
        if (node != null) {
            getCacheLog().CriticalInfo("MirrorManager.RemoveNode", "Before: Node not null");
            CacheNode prevNode = (CacheNode) nodesList.get(node.getPreviousNodeId());
            CacheNode nextNode = (CacheNode) nodesList.get(node.getBackupNodeId());
            String lastBackup = prevNode.getBackupNodeId();
            prevNode.setBackupNodeId(nextNode.getNodeId());
            CacheNode affectedNode = (CacheNode) prevNode.clone();
            nextNode.setPreviousNodeId(prevNode.getNodeId());
            nodesList.remove(nodeId);
            if (nodeId.equals(lastNode.getNodeId())) {
                lastNode = prevNode;
            } else if (nodeId.equals(firstNode.getNodeId())) {
                firstNode = nextNode;
            }

            getCacheLog().CriticalInfo("MirrorManager.RemoveNode", "After: Node not null -> " + affectedNode.toString());
        }
    }

    /**
     * Returns the complete list of CacheNodes
     */
    public final CacheNode[] getCacheNodes() {
        CacheNode[] nodes = new CacheNode[this.nodesList.size()];
        System.arraycopy(this.nodesList.values().toArray(), 0, nodes, 0, this.nodesList.values().size());
        return nodes;
    }

    public final java.util.Iterator iterator() {
        return nodesList.values().iterator();
    }
}
