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

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class Channel implements Cloneable, InternalCompactSerializable {

    private int tcpPort, numInitHosts, connectionRetries = 2, connectionRetryInterval = 2;
    private int portRange = 1; // default port-range is '1'
    private String initialHosts;
    private int joinRetryInterval = 5;
    private int joinRetries = 24;

    public Channel() {
    }

    public Channel(int defaultPortRange) {
        portRange = defaultPortRange;
    }

    @ConfigurationAttributeAnnotation(value = "tcp-port", appendText = "")
    public final int getTcpPort() {
        return tcpPort;
    }

    @ConfigurationAttributeAnnotation(value = "tcp-port", appendText = "")
    public final void setTcpPort(int value) {
        tcpPort = value;
    }

    @ConfigurationAttributeAnnotation(value = "port-range", appendText = "")
    public final int getPortRange() {
        return portRange;
    }

    @ConfigurationAttributeAnnotation(value = "port-range", appendText = "")
    public final void setPortRange(int value) {
        portRange = value;
    }

    @ConfigurationAttributeAnnotation(value = "connection-retries", appendText = "")
    public final int getConnectionRetries() {
        return connectionRetries;
    }

    @ConfigurationAttributeAnnotation(value = "connection-retries", appendText = "")
    public final void setConnectionRetries(int value) {
        connectionRetries = value;
    }

    @ConfigurationAttributeAnnotation(value = "connection-retry-interval", appendText = "secs")
    public final int getConnectionRetryInterval() {
        return connectionRetryInterval;
    }

    @ConfigurationAttributeAnnotation(value = "connection-retry-interval", appendText = "secs")
    public final void setConnectionRetryInterval(int value) {
        connectionRetryInterval = value;
    }

    @ConfigurationAttributeAnnotation(value = "initial-hosts", appendText = "")
    public final String getInitialHosts() {
        return initialHosts;
    }

    @ConfigurationAttributeAnnotation(value = "initial-hosts", appendText = "")
    public final void setInitialHosts(String value) {
        initialHosts = value;
    }

    @ConfigurationAttributeAnnotation(value = "num-initial-hosts", appendText = "")
    public final int getNumInitHosts() {
        return numInitHosts;
    }

    @ConfigurationAttributeAnnotation(value = "num-initial-hosts", appendText = "")
    public final void setNumInitHosts(int value) {
        numInitHosts = value;
    }

    @ConfigurationAttributeAnnotation(value = "join_retry_count", appendText = "")
    public final int getJoinRetries() {
        return joinRetries;
    }

    @ConfigurationAttributeAnnotation(value = "join_retry_count", appendText = "")
    public final void setJoinRetries(int value) {
        joinRetries = value;
    }

    @ConfigurationAttributeAnnotation(value = "join_retry_timeout", appendText = "")
    public final int getJoinRetryInterval() {
        return joinRetryInterval;
    }

    @ConfigurationAttributeAnnotation(value = "join_retry_timeout", appendText = "")
    public final void setJoinRetryInterval(int value) {
        joinRetryInterval = value;
    }

    @Override
    public final Object clone() throws CloneNotSupportedException {
        Channel channel = new Channel();
        channel.setTcpPort(getTcpPort());
        channel.setPortRange(getPortRange());
        channel.setConnectionRetries(getConnectionRetries());
        channel.setConnectionRetryInterval(getConnectionRetryInterval());
        channel.setInitialHosts(getInitialHosts() != null ? (String) new String(getInitialHosts()) : null);
        channel.setNumInitHosts(getNumInitHosts());
        return channel;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        tcpPort = reader.ReadInt32();
        numInitHosts = reader.ReadInt32();
        connectionRetries = reader.ReadInt32();
        connectionRetryInterval = reader.ReadInt32();
        portRange = reader.ReadInt32();
        initialHosts = (String) Common.readAs(reader.ReadObject(), String.class);
        joinRetryInterval = reader.ReadInt32();
        joinRetries = reader.ReadInt32();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.Write(tcpPort);
        writer.Write(numInitHosts);
        writer.Write(connectionRetries);
        writer.Write(connectionRetryInterval);
        writer.Write(portRange);
        writer.WriteObject(initialHosts);
        writer.Write(joinRetryInterval);
        writer.Write(joinRetries);
    }
}
