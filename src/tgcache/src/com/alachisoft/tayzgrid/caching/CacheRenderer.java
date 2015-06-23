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

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.common.event.NEvent;
import com.alachisoft.tayzgrid.common.event.NEventEnd;
import com.alachisoft.tayzgrid.common.event.NEventStart;
import com.alachisoft.tayzgrid.common.LoggingInfo;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Renders cache to its client.
 */
public abstract class CacheRenderer {

    public static interface ClientConnected {

        void invoke(String client, String cacheId);
    }

    public static interface ClientDisconnected {

        void invoke(String client, String cacheId);
    }
    private NEvent _clientConnected = new NEvent("CacheRenderer.ClientConnected", null);
    private NEvent _clientDisconnected = new NEvent("CacheRenderer.ClientDisconnected", null);

    public void addClientConnectedListner(NEventStart start, NEventEnd end) {
        _clientConnected.addNEventListners(start, end);
    }

    public void removeClientConnectedListner(NEventStart start) {
        _clientConnected.removeNEventListners(start);
    }

    public void addClientDisconnectedListner(NEventStart start, NEventEnd end) {
        _clientDisconnected.addNEventListners(start, end);
    }

    public void removeClientDisconnectedListner(NEventStart start) {
        _clientDisconnected.removeNEventListners(start);
    }

    public abstract int getPort();

    public abstract InetAddress getIPAddress();

    /**
     * Get logging status for logging type
     *
     * @param subsystem
     * @param type Type of logging
     * @return Current status of logging
     */
    public abstract LoggingInfo.LogsStatus GetLoggingStatus(LoggingInfo.LoggingSubsystem subsystem, LoggingInfo.LoggingType type);

    /**
     * Set and apply logging status for a logging type
     *
     * @param subsystem
     * @param type Type of logging
     * @param status Logging status to set
     */
    public abstract void SetLoggingStatus(LoggingInfo.LoggingSubsystem subsystem, LoggingInfo.LoggingType type, LoggingInfo.LogsStatus status) throws Exception;

    public java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientNode> GetClientList(String cacheId) throws UnknownHostException {
        return null;
    }

    public java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats> GetClientProcessStats(String cacheId) throws UnknownHostException {
        return null;
    }
}
