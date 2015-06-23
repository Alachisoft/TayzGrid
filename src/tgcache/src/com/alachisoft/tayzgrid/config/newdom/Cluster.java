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

import com.alachisoft.tayzgrid.common.StatusInfo;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.Arrays;

public class Cluster implements Cloneable, InternalCompactSerializable {

    boolean _isNew;
    private String topology;//Changes for Dom
    private String activeMirrorNode = null;//Changes for Dom
    private int opTimeout = 60;
    private int statsRepInterval;
    private boolean useHeartBeat;
    private Channel channel;
    private java.util.HashMap<NodeIdentity, StatusInfo> nodes;//Changes for Dom (old managers)

    public Cluster() {
        channel = new Channel();
        nodes = new java.util.HashMap<NodeIdentity, StatusInfo>();//Changes for Dom Code Insert
        topology = new String();
        _isNew = true;
    }

    public final String getTopology() {
        String value = this.topology;
        if (value != null) {
            value = value.toLowerCase();
            if (value.equals("replicated")) {
                return "replicated-server";
            } 
            else if (value.equals("partitioned")) {
                return "partitioned-server";
            } 
        }
        return value;
    }

    public final void setTopology(String value) {
        this.topology = value;
    }

    public final boolean getIsNew() {
        return _isNew;
    }

    public final void setIsNew(boolean value) {
        _isNew = value;
    }

    @ConfigurationAttributeAnnotation(value = "operation-timeout", appendText = "sec")//Change for dom op-timeout
    public final int getOpTimeout() {
        return opTimeout;
    }

    @ConfigurationAttributeAnnotation(value = "operation-timeout", appendText = "sec")//Change for dom op-timeout
    public final void setOpTimeout(int value) {
        opTimeout = value;
    }

    @ConfigurationAttributeAnnotation(value = "stats-repl-interval", appendText = "sec")
    public final int getStatsRepInterval() {
        return statsRepInterval;
    }

    @ConfigurationAttributeAnnotation(value = "stats-repl-interval", appendText = "sec")
    public final void setStatsRepInterval(int value) {
        statsRepInterval = value;
    }

    @ConfigurationAttributeAnnotation(value = "use-heart-beat", appendText = "")
    public final boolean getUseHeartbeat() {
        return useHeartBeat;
    }

    @ConfigurationAttributeAnnotation(value = "use-heart-beat", appendText = "")
    public final void setUseHeartbeat(boolean value) {
        useHeartBeat = value;
    }

    @ConfigurationSectionAnnotation(value = "cluster-connection-settings")//change for dom  channel
    public final Channel getChannel() {
        return channel;
    }

    @ConfigurationSectionAnnotation(value = "cluster-connection-settings")//change for dom channel
    public final void setChannel(Channel value) {
        channel = value;
    }

    //Change for dom Comment code
    public final java.util.HashMap<NodeIdentity, StatusInfo> getNodes() {
        return nodes;
    }

    public final void setNodes(java.util.HashMap<NodeIdentity, StatusInfo> value) {
        nodes = value;
    }

    public final java.util.ArrayList<NodeIdentity> getNodeIdentities() {
        NodeIdentity[] nodeIdentities = new NodeIdentity[nodes.size()];
        nodes.keySet().toArray(nodeIdentities);

        return new java.util.ArrayList<NodeIdentity>(Arrays.asList(nodeIdentities));
    }

    @ConfigurationAttributeAnnotation(value = "active-mirror-node", appendText = "")
    public final String getActiveMirrorNode() {
        return activeMirrorNode;
    }

    @ConfigurationAttributeAnnotation(value = "active-mirror-node", appendText = "")
    public final void setActiveMirrorNode(String value) {
        activeMirrorNode = value;
    }

    @Override
    public final Object clone() throws CloneNotSupportedException {
        Cluster cluster = new Cluster();
        cluster.setOpTimeout(getOpTimeout());
        cluster.setStatsRepInterval(getStatsRepInterval());
        cluster.setUseHeartbeat(getUseHeartbeat());
        cluster.setIsNew(_isNew);

        cluster.setChannel(getChannel() != null ? (Channel) getChannel().clone() : null);
        return cluster;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        opTimeout = reader.ReadInt32();
        statsRepInterval = reader.ReadInt32();
        useHeartBeat = reader.ReadBoolean();
        channel = (Channel) Common.readAs(reader.ReadObject(), Channel.class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {

        writer.Write(opTimeout);
        writer.Write(statsRepInterval);
        writer.Write(useHeartBeat);
        writer.WriteObject(channel);
    }
}
