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

package com.alachisoft.tayzgrid.config.dom;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.StatusInfo;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Cluster implements Cloneable, InternalCompactSerializable
{

    private String topology;
    private String activeMirrorNode = null;
    private int opTimeout = 60;
    private int statsRepInterval;
    private boolean useHeartBeat;
    private Channel channel;
    private java.util.HashMap<com.alachisoft.tayzgrid.config.newdom.NodeIdentity, StatusInfo> nodes;

    public Cluster()
    {
        channel = new Channel();
        nodes = new java.util.HashMap<com.alachisoft.tayzgrid.config.newdom.NodeIdentity, StatusInfo>();
    }

    @ConfigurationAttributeAnnotation(value = "topology", appendText = "")
    public final String getCacheType()
    {
        return this.topology;
    }

    @ConfigurationAttributeAnnotation(value = "topology", appendText = "")
    public final void setCacheType(String value)
    {
        this.topology = value;
    }

    /**
     * Get the topology type
     */
    public final String getTopology()
    {
        String value = this.topology;
        if (value != null)
        {
            value = value.toLowerCase();
            if (value.equals("replicated"))
            {
                return "replicated-server";
            }
            else if (value.equals("partitioned"))
            {
                return "partitioned-server";
            }
        }
        return value;
    }

    public final void setTopology(String value)
    {
        this.topology = value;
    }

    @ConfigurationAttributeAnnotation(value = "op-timeout", appendText = "sec")
    public final int getOpTimeout()
    {
        return opTimeout;
    }

    @ConfigurationAttributeAnnotation(value = "op-timeout", appendText = "sec")
    public final void setOpTimeout(int value)
    {
        opTimeout = value;
    }

    @ConfigurationAttributeAnnotation(value = "stats-repl-interval", appendText = "sec")
    public final int getStatsRepInterval()
    {
        return statsRepInterval;
    }

    @ConfigurationAttributeAnnotation(value = "stats-repl-interval", appendText = "sec")
    public final void setStatsRepInterval(int value)
    {
        statsRepInterval = value;
    }

    @ConfigurationAttributeAnnotation(value = "use-heart-beat", appendText = "")
    public final boolean getUseHeartbeat()
    {
        return useHeartBeat;
    }

    @ConfigurationAttributeAnnotation(value = "use-heart-beat", appendText = "")
    public final void setUseHeartbeat(boolean value)
    {
        useHeartBeat = value;
    }

    @ConfigurationSectionAnnotation(value = "channel")
    public final Channel getChannel()
    {
        return channel;
    }

    @ConfigurationSectionAnnotation(value = "channel")
    public final void setChannel(Channel value)
    {
        channel = value;
    }

    public final java.util.HashMap<com.alachisoft.tayzgrid.config.newdom.NodeIdentity, StatusInfo> getNodes()
    {
        return nodes;
    }

    public final void setNodes(java.util.HashMap<com.alachisoft.tayzgrid.config.newdom.NodeIdentity, StatusInfo> value)
    {
        nodes = value;
    }

    public final java.util.ArrayList<com.alachisoft.tayzgrid.config.newdom.NodeIdentity> getNodeIdentities()
    {
        com.alachisoft.tayzgrid.config.newdom.NodeIdentity[] nodeIdentities = new com.alachisoft.tayzgrid.config.newdom.NodeIdentity[nodes.size()];
        nodes.keySet().toArray(nodeIdentities);
        return new java.util.ArrayList<com.alachisoft.tayzgrid.config.newdom.NodeIdentity>(Arrays.asList(nodeIdentities));
    }

    @ConfigurationAttributeAnnotation(value = "active-mirror-node", appendText = "")
    public final String getActiveMirrorNode()
    {
        return activeMirrorNode;
    }

    @ConfigurationAttributeAnnotation(value = "active-mirror-node", appendText = "")
    public final void setActiveMirrorNode(String value)
    {
        activeMirrorNode = value;
    }

    public final int getNewNodePriority()
    {
        int priority = 0;
        if (getNodes() == null)
        {
            return 1;
        }

        for (com.alachisoft.tayzgrid.config.newdom.NodeIdentity node : getNodeIdentities())
        {
            if (node.getNodePriority() > priority)
            {
                priority = node.getNodePriority();
            }
        }

        return priority + 1;
    }

    public final void ReAssignPriority(com.alachisoft.tayzgrid.config.newdom.NodeIdentity leavingNode)
    {
        for (com.alachisoft.tayzgrid.config.newdom.NodeIdentity node : getNodeIdentities())
        {
            if (leavingNode.getNodeName().equals(node.getNodeName()))
            {
                leavingNode.setNodePriority(node.getNodePriority());
            }
        }

        for (com.alachisoft.tayzgrid.config.newdom.NodeIdentity oldNode : getNodeIdentities())
        {
            if (oldNode.getNodePriority() > leavingNode.getNodePriority())
            {
                oldNode.setNodePriority(oldNode.getNodePriority() - 1);
            }
        }
    }

    /**
     * Gets the list of all configured servers in the cache based on initial-host list.
     *
     * @return
     */
    public final java.util.ArrayList<Address> GetAllConfiguredNodes() throws UnknownHostException
    {
        java.util.ArrayList<Address> nodes = new java.util.ArrayList<Address>();

        if (getChannel() != null)
        {
            String[] splitted = getChannel().getInitialHosts().split("[,]", -1);

            String nameOrIP = null;
            int port;

            for (String hostString : splitted)
            {
                int firstBrace = hostString.indexOf("[");
                int lastBrace = hostString.indexOf("]");
                if (firstBrace > 0 && lastBrace > firstBrace)
                {
                    nameOrIP = hostString.substring(0, firstBrace);
                    port = Integer.parseInt(hostString.substring(firstBrace + 1, firstBrace + 1 + lastBrace - firstBrace - 1));
                    nodes.add(new Address(nameOrIP, port));
                }
            }
        }

        return nodes;
    }

    public final Object clone() throws CloneNotSupportedException
    {
        Cluster cluster = new Cluster();
        cluster.topology = this.topology != null ? new String(this.topology) : null;
        cluster.setOpTimeout(getOpTimeout());
        cluster.setStatsRepInterval(getStatsRepInterval());
        cluster.setUseHeartbeat(getUseHeartbeat());
        cluster.activeMirrorNode = activeMirrorNode;
        if (nodes != null)
        {
            cluster.nodes = new java.util.HashMap<com.alachisoft.tayzgrid.config.newdom.NodeIdentity, StatusInfo>();
            for (java.util.Map.Entry<com.alachisoft.tayzgrid.config.newdom.NodeIdentity, StatusInfo> pair : nodes.entrySet())
            {
                cluster.nodes.put(pair.getKey(), pair.getValue());
            }
        }

        cluster.setChannel(getChannel() != null ? (Channel) getChannel().clone() : null);
        return cluster;
    }

    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        topology = (String) Common.readAs(reader.ReadObject(), String.class);
        activeMirrorNode = (String) Common.readAs(reader.ReadObject(), String.class);
        opTimeout = reader.ReadInt32();
        statsRepInterval = reader.ReadInt32();
        useHeartBeat = reader.ReadBoolean();
        channel = (Channel) Common.readAs(reader.ReadObject(), Channel.class);

        boolean nodeExists = reader.ReadBoolean();
        if (nodeExists)
        {
            nodes = new HashMap<com.alachisoft.tayzgrid.config.newdom.NodeIdentity, StatusInfo>();
            int count = reader.ReadInt32();

            for (int i = 0; i < count; i++)
            {
                nodes.put((com.alachisoft.tayzgrid.config.newdom.NodeIdentity)Common.readAs(reader.ReadObject(),com.alachisoft.tayzgrid.config.newdom.NodeIdentity.class) , (StatusInfo) Common.readAs(reader.ReadObject() ,StatusInfo.class));
            }
        }
    }

    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(topology);
        writer.WriteObject(activeMirrorNode);
        writer.Write(opTimeout);
        writer.Write(statsRepInterval);
        writer.Write(useHeartBeat);
        writer.WriteObject(channel);

        if (nodes != null)
        {
            writer.Write(true);
            writer.Write(nodes.size());
            Iterator ide = nodes.entrySet().iterator();

            while (ide.hasNext())
            {
                Map.Entry current = (Map.Entry) ide.next();
                writer.WriteObject(current.getKey());
                writer.WriteObject(current.getValue());
            }
        }
        else
            writer.Write(false);
    }
}
