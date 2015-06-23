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

package com.alachisoft.tayzgrid.config.newdom;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.common.StatusInfo;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class ServersNodes implements Cloneable, InternalCompactSerializable {

    private String _activeMirrorNode;
    private java.util.ArrayList nodesList;
    private java.util.HashMap<NodeIdentity, StatusInfo> nodes;

    public ServersNodes() {
        nodes = new java.util.HashMap<NodeIdentity, StatusInfo>();
        nodesList = new java.util.ArrayList();
        _activeMirrorNode = "";

    }

    @ConfigurationSectionAnnotation(value = "server-node")
    public final ServerNode[] getServerNodeList() {
        ServerNode[] serverNode = new ServerNode[nodesList.size()];
        for (int index = 0; index < nodesList.size(); index++) {
            serverNode[index] = (ServerNode) nodesList.get(index);
        }

        return serverNode;
    }

    @ConfigurationSectionAnnotation(value = "server-node")
    public final void setServerNodeList(Object[] value) {
        nodesList.clear();
        nodesList.addAll(Arrays.asList(value));
    }
    
    public final java.util.ArrayList getNodesList() {
        return nodesList;
    }

    public final void setNodesList(java.util.ArrayList value) {
        nodesList = value;
    }
    
    public final String getActiveMirrorNode() {
        if (getServerNodeList() != null) {
            for (int index = 0; index < nodesList.size(); index++) 
            {
                ServerNode server = (ServerNode) nodesList.get(index);
                if (server.getIsActiveMirrorNode()) {
                    return server.getIP();
                }
            }
        }
        return _activeMirrorNode;
    }

    public final void setActiveMirrorNode(String value) {
        _activeMirrorNode = value;
        if(value != null && !value.isEmpty())
        if (getServerNodeList() != null) {
            for (int index = 0; index < nodesList.size(); index++) {
                ServerNode server;
                server = (ServerNode) nodesList.get(index);
                if (server.getIP().equals(value)) {
                    server.setIsActiveMirrorNode(true);
                }
            }
        }
    }

    public final java.util.HashMap<NodeIdentity, StatusInfo> getNodes() {
        return nodes;
    }

    public final void setNodes(java.util.HashMap<NodeIdentity, StatusInfo> value) {
        nodes = value;
    }

    public final NodeIdentity[] getNodeIdentities() 
    {
        NodeIdentity[] nodeIdentities = new NodeIdentity[nodes.size()];
        nodes.keySet().toArray(nodeIdentities);
        return nodeIdentities;
    }
    
    public final boolean FindNode(String ip) {
        for (int index = 0; index < nodesList.size(); index++) {
            ServerNode server;
            server = (ServerNode) nodesList.get(index);

            if (server.getIP().equals(ip)) {
                return true;
            }
        }
        return false;
    }
    
    public final int getNewNodePriority() {
        int priority = 0;
        if (getNodes() == null) {
            return 1;
        }
        
        for (NodeIdentity node : getNodeIdentities()) {
            if (node.getNodePriority() > priority) {
                priority = node.getNodePriority();
            }
        }
        
        return priority + 1;
    }
    
    public final void ReAssignPriority(NodeIdentity leavingNode) {
        for (NodeIdentity node : getNodeIdentities()) {
            if (leavingNode.getNodeName() == node.getNodeName()) {
                leavingNode.setNodePriority(node.getNodePriority());
            }
        }
        
        for (NodeIdentity oldNode : getNodeIdentities()) {
            if (oldNode.getNodePriority() > leavingNode.getNodePriority()) {
                oldNode.setNodePriority(oldNode.getNodePriority() - 1);
            }
        }
    }

    /**
     * Gets the list of all configured servers in the cache based on
     * initial-host list.
     *
     * @return
     */
    public final java.util.ArrayList<Address> GetAllConfiguredNodes() throws UnknownHostException {
        java.util.ArrayList<Address> nodes = new java.util.ArrayList<Address>();
        if (getServerNodeList() != null) {
            for (int index = 0; index < nodesList.size(); index++) {
                ServerNode sn = (ServerNode) nodesList.get(index);
                
                nodes.add(new Address(sn.getIP(), 0));
            }
        }
        
        return nodes;
    }

    public final Object clone() {
        ServersNodes serverNode = new ServersNodes();
        Object tempVar = getServerNodeList().clone();
        serverNode.setServerNodeList((ServerNode[]) ((tempVar instanceof ServerNode[]) ? tempVar : null));
        if (nodes != null) {
            serverNode.nodes = new java.util.HashMap<NodeIdentity, StatusInfo>();
            for (java.util.Map.Entry<NodeIdentity, StatusInfo> pair : nodes.entrySet()) {
                serverNode.nodes.put(pair.getKey(), pair.getValue());
            }
        }
        return serverNode;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        if (this.nodesList == null) {
            this.nodesList = new ArrayList();
        }
        
        this.nodesList = (java.util.ArrayList) Common.readAs(reader.ReadObject(), java.util.ArrayList.class);
        
        boolean nodeExists = reader.ReadBoolean();
        if (nodeExists) {
            this.nodes = new HashMap<NodeIdentity, StatusInfo>();
            
            int count = reader.ReadInt32();
            for (int index = 0; index < count; index++) {
                nodes.put(Common.as(reader.ReadObject(), NodeIdentity.class), Common.as(reader.ReadObject(), StatusInfo.class));
            }
        }
    }
    
    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(this.nodesList);
        if (nodes != null && nodes.size() > 0) {
            writer.Write(true);
            writer.Write(nodes.size());
            for (NodeIdentity key : nodes.keySet()) {
                writer.WriteObject(key);
                writer.WriteObject(nodes.get(key));
            }
        } else {
            writer.Write(false);
        }
    }
}