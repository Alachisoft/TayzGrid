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
package com.alachisoft.tayzgrid.management.clientconfiguration.dom;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.common.enums.RtContextValue;
import com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheConfiguration implements Cloneable, InternalCompactSerializable {

    private String _cacheId;
    private String _defaultReadThruProvider = "";
    private String _defaultWriteThruProvider = "";
    private boolean _loadBalance = true;
    private boolean _isRegisteredLocal;
    private CacheServerList _serversPriorityList = new CacheServerList();
    private static String _serverName;
    private static String _bindIp;
    private RtContextValue _serverRuntimeContext;
    private int clientPort = 0;

    static {
        try {
            try {
                _serverName = java.net.InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ex) {
                Logger.getLogger(CacheConfiguration.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (RuntimeException e) {
        }
    }

    public final boolean getIsRegisteredLocal() {
        return _isRegisteredLocal;
    }

    public final void setIsRegisteredLocal(boolean value) {
        _isRegisteredLocal = value;
    }

    /**
     *
     * @return
     */
    @ConfigurationAttributeAnnotation(value = "id", appendText = "", order = 0)
    public final String getCacheId() {
        return _cacheId;
    }

    /**
     *
     * @param value
     */
    @ConfigurationAttributeAnnotation(value = "id", appendText = "", order = 0)
    public final void setCacheId(String value) {
        _cacheId = value;
    }

    @ConfigurationAttributeAnnotation(value = "default-readthru-provider", appendText = "")
    public final String getDefaultReadThruProvider() {
        return _defaultReadThruProvider;
    }

    @ConfigurationAttributeAnnotation(value = "default-readthru-provider", appendText = "")
    public final void setDefaultReadThruProvider(String value) {
        _defaultReadThruProvider = value;
    }

    @ConfigurationAttributeAnnotation(value = "default-writethru-provider", appendText = "")
    public final String getDefaultWriteThruProvider() {
        return _defaultWriteThruProvider;
    }

    @ConfigurationAttributeAnnotation(value = "default-writethru-provider", appendText = "")
    public final void setDefaultWriteThruProvider(String value) {
        _defaultWriteThruProvider = value;
    }

    @ConfigurationAttributeAnnotation(value = "load-balance", appendText = "")
    public final boolean getLoadBalance() {
        return _loadBalance;
    }

    @ConfigurationAttributeAnnotation(value = "load-balance", appendText = "")
    public final void setLoadBalance(boolean value) {
        _loadBalance = value;
    }

    public final RtContextValue getServerRuntimeContext() {
        return _serverRuntimeContext;
    }

    public final void setServerRuntimeContext(RtContextValue serverRtContext) {
        _serverRuntimeContext = serverRtContext;
    }

    public final String getServerRuntimeContextString() {
        if (_serverRuntimeContext == RtContextValue.JVCACHE) {
            return "JVCACHE";
        } else {
            return "NCACHE";
        }
    }

    public final void setServerRuntimeContextString(String value) {
        if ("JVCACHE".equals(value)) {
            _serverRuntimeContext = RtContextValue.JVCACHE;
        } else if ("NCACHE".equals(value)) {
            _serverRuntimeContext = RtContextValue.NCACHE;
        }
    }

    @ConfigurationSectionAnnotation(value = "server")
    public final CacheServer[] getServers() {
        CacheServer[] servers = new CacheServer[_serversPriorityList.getServersList().size()];
        for (java.util.Map.Entry<Integer, CacheServer> pair : _serversPriorityList.getServersList().entrySet()) {
            int priority = pair.getKey();
            CacheServer server = pair.getValue();
            server.setPriority(priority);
            servers[priority] = server;
        }

        return servers;
    }

    @ConfigurationSectionAnnotation(value = "server")
    public final void setServers(Object[] value) {
        for (int i = 0; i < value.length; i++) {
            _serversPriorityList.getServersList().put(i, (CacheServer) value[i]);
        }
    }


    public final CacheServerList getServersPriorityList() {
        return _serversPriorityList;
    }

    public final void setServersPriorityList(CacheServerList value) {
        _serversPriorityList = value;
    }

    public final String getBindIp() {
        return _bindIp;
    }

    public final void setBindIp(String value) {
        _bindIp = value;
    }

    public final boolean RemoveServer(String serverName) {
        int serverPriority = 0;
        boolean found = false;

        if (_serversPriorityList != null) {
            for (CacheServer server : _serversPriorityList.getServersList().values()) {
                if (server.getServerName().toLowerCase().equals(serverName.toLowerCase())) {
                    serverPriority = server.getPriority();
                    found = true;
                    break;
                }
            }

            if (found) {
                _serversPriorityList.getServersList().remove(serverPriority);
                return true;
            }
        }

        return false;
    }

    public final boolean AddServer(String serverName, int priority) {
        CacheServer server = new CacheServer();
        server.setServerName(serverName);
        server.setPriority(priority);
        _serversPriorityList.getServersList().put(priority, server);
        return true;
    }

    public final boolean AddLocalServer(String bindedIp) {
        return AddServer(!tangible.DotNetToJavaStringHelper.isNullOrEmpty(getBindIp()) ? getBindIp() : bindedIp, 0);
    }

    private void BringLocalServerToFirstPriority() {
        java.util.HashMap<Integer, CacheServer> tempList = new java.util.HashMap<Integer, CacheServer>();
        int localServerPriority = 0;
        boolean localServerFound = false;

        for (java.util.Map.Entry<Integer, CacheServer> pair : _serversPriorityList.getServersList().entrySet()) {
            String serverName = pair.getValue().getServerName().toLowerCase();
            if ((serverName.compareTo(_serverName.toLowerCase()) == 0) || (serverName.compareTo(_bindIp.toLowerCase()) == 0)) {
                localServerFound = true;
                localServerPriority = pair.getKey();
                break;
            }
        }

        if (localServerFound) {
            tempList.put(0, _serversPriorityList.getServersList().get(localServerPriority));
            int priority = 1;
            for (java.util.Map.Entry<Integer, CacheServer> pair : _serversPriorityList.getServersList().entrySet()) {
                if (pair.getKey() != localServerPriority) {
                    tempList.put(priority++, pair.getValue());
                }
            }
            _serversPriorityList.setServersList(tempList);
        }
    }

    @ConfigurationAttributeAnnotation(value = "client-port", appendText = "")
    public final int getClientPort() {
        return clientPort;
    }

    @ConfigurationAttributeAnnotation(value = "client-port", appendText = "")
    public final void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public final Object clone() {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setBindIp(_bindIp);
        configuration._cacheId = _cacheId;
        
        configuration._defaultReadThruProvider = _defaultReadThruProvider;
        configuration._defaultWriteThruProvider = _defaultWriteThruProvider;
        configuration._loadBalance = _loadBalance;

        configuration.setServers(getServers() != null ? (CacheServer[]) ((getServers().clone() instanceof CacheServer[]) ? getServers().clone() : null) : null);
        configuration._serverRuntimeContext = _serverRuntimeContext;
        configuration.setClientPort(getClientPort());

        return configuration;
    }

    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        _cacheId = (String) Common.readAs(reader.ReadObject(), String.class);
        clientPort = reader.ReadInt32();
        _defaultReadThruProvider = (String) Common.readAs(reader.ReadObject(), String.class);
        _defaultWriteThruProvider = (String) Common.readAs(reader.ReadObject(), String.class);
        _loadBalance = reader.ReadBoolean();
        _isRegisteredLocal = reader.ReadBoolean();
        int length = reader.ReadInt32();
        for (int i = 0; i < length; i++) {
            _serversPriorityList.setServersList(reader.ReadInt32(), (CacheServer) Common.readAs(reader.ReadObject(), CacheServer.class));
        }
        _serverName = (String) Common.readAs(reader.ReadObject(), String.class);
        _bindIp = (String) Common.readAs(reader.ReadObject(), String.class);
        _serverRuntimeContext = "1".equals(Common.as(reader.ReadObject(), String.class)) ? RtContextValue.JVCACHE : RtContextValue.NCACHE;

    }

    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(_cacheId);
        writer.Write(clientPort);
        writer.WriteObject(_defaultReadThruProvider);
        writer.WriteObject(_defaultWriteThruProvider);
        writer.Write(_loadBalance);
        writer.Write(_isRegisteredLocal);
        writer.Write(this._serversPriorityList.getServersList().size());
        Map.Entry current = null;
        Iterator ite = this._serversPriorityList.getServersList().entrySet().iterator();
        while (ite.hasNext()) {
            current = (Map.Entry) ite.next();
            writer.Write(((Integer) current.getKey()).intValue());
            writer.WriteObject(current.getValue());
        }
        writer.WriteObject(_serverName);
        writer.WriteObject(_bindIp);
        writer.WriteObject(_serverRuntimeContext == RtContextValue.JVCACHE ? "1" : "0");

    }

}
