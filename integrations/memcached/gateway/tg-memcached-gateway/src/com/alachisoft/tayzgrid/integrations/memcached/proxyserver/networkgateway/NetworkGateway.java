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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway;

import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.LogManager;

public abstract class NetworkGateway {

    public abstract void StartListenForClients();

    public abstract void StopListenForClients();
    protected static final java.util.HashSet<MemTcpClient> _clients = new java.util.HashSet<MemTcpClient>();

    public static void DisposeClient(MemTcpClient client) {
        if (client == null) {
            return;
        }
        try {
            boolean exists = false;
            synchronized (_clients) {
                exists = _clients.remove(client);
            }
            if (exists) {
                LogManager.getLogger().Info("NetworkGateway", "\tDisposing " + client.getProtocol() + "client.");
                client.dispose();
            }
        } catch (RuntimeException e) {
            LogManager.getLogger().Fatal("NetworkGateway", "\tFailed to dispose MemTcpClient. " + e.getMessage());
        }
    }
}