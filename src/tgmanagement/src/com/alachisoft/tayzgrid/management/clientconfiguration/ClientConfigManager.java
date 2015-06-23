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
package com.alachisoft.tayzgrid.management.clientconfiguration;

import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.common.AppUtil;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.management.clientconfiguration.dom.NodeConfiguration;
import com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration;
import com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheConfiguration;
import com.alachisoft.tayzgrid.common.enums.ClientNodeStatus;
import com.alachisoft.tayzgrid.common.enums.RtContextValue;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

public class ClientConfigManager {

    private static java.util.ArrayList ipAddresses = new java.util.ArrayList(1);
    private static String DIRNAME = "config";
    private static String FILENAME = "client.conf";
    private static String c_configDir = DIRNAME;
    private static String c_configFileName = FILENAME;
    public static String ENDSTRING = "\r\n";
    private static String bindIp;
    private static String _localCacheGivenId = "";
    private static java.util.HashMap _caches = new java.util.HashMap();
    private static ClientConfiguration _configuration;

    public static void AvailableNIC(java.util.HashMap nic) {
        if (nic != null) {
            ipAddresses.addAll(nic.keySet());
        }
    }

    public static void LoadConfiguration() throws ManagementException {
        try {
            CombinePath();

            if (!(new java.io.File(c_configFileName)).isFile() || (new java.io.File(c_configFileName)).length() == 0) {
                /**
                 * Save a dummy configuration.
                 */
                SaveConfiguration();
                return;
            }

            _caches.clear();
            try {
                LoadXml();
            } catch (Exception parserConfigurationException) {
                throw new ManagementException(parserConfigurationException.getMessage(), parserConfigurationException);
            }
        } catch (ManagementException e) {
            c_configFileName = "";
            throw e;
        } catch (RuntimeException e) {
            c_configFileName = "";
            throw new ManagementException(e.getMessage(), e);
        }
    }

    public static String getBindIP() {
        return bindIp;
    }

    public static void setBindIP(String value) {
        bindIp = value;
    }

    public static String getLocalCacheId() {
        return _localCacheGivenId;
    }

    public static void setLocalCacheId(String value) {
        _localCacheGivenId = value;
    }

    private static void CombinePath() throws ManagementException {
        c_configDir = AppUtil.getInstallDir();

        if (c_configDir == null || c_configDir.length() == 0) {
            throw new ManagementException("Missing installation folder information");
        }

        c_configDir = new File(c_configDir, DIRNAME).getPath();
        if (!(new java.io.File(c_configDir)).isDirectory()) {
            (new java.io.File(c_configDir)).mkdir();
        }

        c_configFileName = new File(c_configDir, FILENAME).getPath();
    }

    private static void LoadXml() throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(c_configFileName)) {
            CombinePath();
        }

        ConfigurationBuilder configBuilder = new ConfigurationBuilder(c_configFileName);
        try {
            configBuilder.RegisterRootConfigurationObject(com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration.class);
            configBuilder.ReadConfiguration();
        } catch (Exception exception) {
            throw new ManagementException(exception.getMessage(), exception);
        }

        ClientConfiguration clientConfiguration = null;
        Object[] configuration = configBuilder.getConfiguration();

        if (configuration != null && configuration.length > 0) {
            for (int i = 0; i < configuration.length; i++) {
                clientConfiguration = (ClientConfiguration) ((configuration[i] instanceof ClientConfiguration) ? configuration[i] : null);
                break;
            }
        }

        _configuration = clientConfiguration;

        if (_configuration == null) {
            _configuration = new com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration();
        }

        if (_configuration.getNodeConfiguration() == null) {
            _configuration.setNodeConfiguration(new NodeConfiguration());
        }

        _configuration.setBindIp(getBindIP());
    }

    public static void UpdateCacheConfiguration(String cacheId, ClientConfiguration configuration) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        LoadXml();

        if (_configuration == null) {
            _configuration = new com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration();
        }

        if (_configuration.getCacheConfigurationsMap() == null) {
            _configuration.setCacheConfigurationsMap(new java.util.HashMap<String, CacheConfiguration>());
        }

        cacheId = cacheId.toLowerCase();
        Object tempVar = configuration.getNodeConfiguration().clone();
        _configuration.setNodeConfiguration((NodeConfiguration) ((tempVar instanceof NodeConfiguration) ? tempVar : null));
        CacheConfiguration cacheConfiguration = null;
        if ((cacheConfiguration = configuration.getCacheConfigurationsMap().get(cacheId)) != null) {
            cacheConfiguration.setBindIp(configuration.getBindIp());
            CacheConfiguration tempVar2 = (CacheConfiguration) cacheConfiguration.clone();
            //tempVar2.setSocketPort(port);

            _configuration.getCacheConfigurationsMap().put(cacheId, (CacheConfiguration) ((tempVar2 instanceof CacheConfiguration) ? tempVar2 : null));
        }

        SaveConfiguration();
    }

    public static void RemoveCacheServer(String cacheId, String server) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        LoadXml();

        if (_configuration != null && _configuration.getCacheConfigurationsMap() != null) {
            cacheId = cacheId.toLowerCase();
            if (_configuration.getCacheConfigurationsMap().containsKey(cacheId)) {
                CacheConfiguration cacheConfig = _configuration.getCacheConfigurationsMap().get(cacheId);
                cacheConfig.RemoveServer(server);
            }
        }

        SaveConfiguration();
    }

    public static void RemoveCache(String cacheId) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {

        LoadXml();

        if (_configuration != null && _configuration.getCacheConfigurationsMap() != null) {
            cacheId = cacheId.toLowerCase();
            if (_configuration.getCacheConfigurationsMap().containsKey(cacheId)) {
                _configuration.getCacheConfigurationsMap().remove(cacheId);
            }
        }

        SaveConfiguration();
    }

    public static void AddCache(String cacheId, int socketPort, RtContextValue serverRuntimeContext) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        LoadXml();

        if (_configuration != null) {
            if (_configuration.getCacheConfigurationsMap() == null) {
                _configuration.setCacheConfigurationsMap(new java.util.HashMap<String, CacheConfiguration>());
            }

            if (!_configuration.getCacheConfigurationsMap().containsKey(cacheId.toLowerCase())) {
                CacheConfiguration cacheConfiguration = new CacheConfiguration();
                cacheConfiguration.setClientPort(socketPort);
                cacheConfiguration.setCacheId(cacheId);
                cacheConfiguration.setServerRuntimeContext(serverRuntimeContext);
                cacheConfiguration.AddLocalServer(_configuration.getBindIp());

                _configuration.getCacheConfigurationsMap().put(cacheId.toLowerCase(), cacheConfiguration);
            }
        }

        SaveConfiguration();
    }

    public static void AddCache(String cacheId, CacheServerConfig config) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        LoadXml();

        if (_configuration != null) {
            if (_configuration.getCacheConfigurationsMap() == null) {
                _configuration.setCacheConfigurationsMap(new java.util.HashMap<String, CacheConfiguration>());
            }

            if (_configuration.getCacheConfigurationsMap().containsKey(cacheId.toLowerCase())) {
                CacheConfiguration cacheConfiguration = new CacheConfiguration();
                cacheConfiguration.setCacheId(cacheId);
                cacheConfiguration.setClientPort(config.getClientPort());
                cacheConfiguration.setDefaultReadThruProvider((String) ((config.getCacheLoaderFactory()== null) ? "" : config.getCacheLoaderFactory()));
                cacheConfiguration.setDefaultWriteThruProvider((String) ((config.getCacheWriterFactory() == null) ? "" : config.getCacheWriterFactory()));
                cacheConfiguration.setLoadBalance(config.getAutoLoadBalancing() == null ? true : (config.getAutoLoadBalancing().getEnabled()));
                
                if (config.getCluster() != null && config.getCluster().getNodes() != null) {
                    if (config.getCluster().getNodes().size() != 1) {
                        // Extract server names from config
                        String[] serverList = new String[config.getCluster().getNodeIdentities().size()];
                        for (int i = 0; i < serverList.length; i++) {
                            serverList[i] = config.getCluster().getNodeIdentities().get(i).getNodeName();
                        }

                        // Sort priority list i.e. local node at top
                        String[] copyServerList = new String[serverList.length];
                        for (int i = 0; i < serverList.length; i++) {
                            if (cacheConfiguration.getBindIp().equals(serverList[i])) {
                                copyServerList[0] = serverList[i];
                                cacheConfiguration.AddServer(cacheConfiguration.getBindIp(), 0);
                            } else if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(copyServerList[0])) {
                                copyServerList[i + 1] = serverList[i];
                                cacheConfiguration.AddServer(serverList[i], i + 1);
                            } else {
                                copyServerList[i] = serverList[i];
                                cacheConfiguration.AddServer(serverList[i], i);
                            }
                        }
                    } else {
                        cacheConfiguration.AddServer(config.getCluster().getNodeIdentities().get(0).getNodeName(), 0);
                    }
                } else {
                    cacheConfiguration.AddLocalServer(_configuration.getBindIp());
                }
                _configuration.getCacheConfigurationsMap().put(cacheId.toLowerCase(), cacheConfiguration);
            }
        }

        SaveConfiguration();
    }
    
    public static void SaveConfiguration() throws ManagementException {
        if (c_configFileName == null || c_configFileName.equals("")) {
            CombinePath();
        }

        FileWriter fs = null;
        BufferedWriter sw = null;

        try {
            fs = new FileWriter(c_configFileName);
            sw = new BufferedWriter(fs);
            sw.write(ToXml());
            sw.flush();
        } catch (Exception e) {
            throw new ManagementException(e.getMessage(), e);
        } finally {
            if (sw != null) {
                try {
                    sw.close();
                } catch (IOException ex) {
                    Logger.getLogger(ClientConfigManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException ex) {
                    Logger.getLogger(ClientConfigManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private static String ToXml() throws IllegalArgumentException, IllegalAccessException, Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append(ENDSTRING + "<!-- Client configuration file is used by client to connect to out-proc caches. " + ENDSTRING
                + "Light weight client also uses this configuration file to connect to the remote caches. " + ENDSTRING
                + "This file is automatically generated each time a new cache/cluster is created or " + ENDSTRING
                + "cache/cluster configuration settings are applied. Additionally security information " + ENDSTRING + "can be provided for each cache in the following format. "
                + ENDSTRING + "<security>" + ENDSTRING + "\t<primary user-id=\"domain\\pri-user\" password=\"pri-pass\"/>" + ENDSTRING
                + "\t<secondary user-id=\"domain\\sec-user\" password=\"sec-pass\"/>" + ENDSTRING + "</security>" + ENDSTRING + "-->");
        sb.append(ENDSTRING + "<!-- Client configuration file is used by client to connect to out-proc caches. " + ENDSTRING
                + "This file is automatically generated each time a new cache/cluster is created or " + ENDSTRING + "cache/cluster configuration settings are applied." + ENDSTRING
                + "-->");

        sb.append("\n");
        if (_configuration != null) {
            Object[] configuration = new Object[1];
            configuration[0] = _configuration;
            ConfigurationBuilder cfgBuilder = new ConfigurationBuilder(configuration);
            try {
                cfgBuilder.RegisterRootConfigurationObject(ClientConfiguration.class);
            } catch (Exception exception) {
                throw new IllegalArgumentException(exception.getMessage(), exception);
            }
            sb.append(cfgBuilder.GetXmlString());
        } else {
            sb.append("<configuration>\r\n");

            sb.append("\r\n</configuration>");
        }
        return sb.toString();
    }

    public static void UpdateServerNodes(String cacheId, CacheServerList serversPriorityList, RtContextValue serverRuntimeContext, int port) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        LoadXml();

        if (_configuration != null) {
            if (_configuration.getCacheConfigurationsMap() == null) {
                _configuration.setCacheConfigurationsMap(new java.util.HashMap<String, CacheConfiguration>());
            }

            cacheId = cacheId.toLowerCase();
            CacheConfiguration cacheConfiguration = null;

            if (!((cacheConfiguration = _configuration.getCacheConfigurationsMap().get(cacheId)) != null)) {
                cacheConfiguration = new CacheConfiguration();
                cacheConfiguration.setCacheId(cacheId);
                cacheConfiguration.setBindIp(bindIp);
                cacheConfiguration.setServerRuntimeContext(serverRuntimeContext);
                cacheConfiguration.setClientPort(port);
                _configuration.getCacheConfigurationsMap().put(cacheId, cacheConfiguration);
            }
            cacheConfiguration.setClientPort(port);
            cacheConfiguration.setServersPriorityList(serversPriorityList);
        }

        SaveConfiguration();
    }

    public static void UpdateServerNodes(String cacheId, String[] servers, tangible.RefObject<String> xml, boolean loadBalance, int port) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        LoadXml();

        cacheId = cacheId.toLowerCase();
        CacheConfiguration cacheConfiguration = null;

        if (_configuration != null && _configuration.getCacheConfigurationsMap() != null) {
            if (!((cacheConfiguration = _configuration.getCacheConfigurationsMap().get(cacheId)) != null)) {
                cacheConfiguration = new CacheConfiguration();
                cacheConfiguration.setCacheId(cacheId);
                cacheConfiguration.setClientPort(port);
                _configuration.getCacheConfigurationsMap().put(cacheId, cacheConfiguration);
            }
        }

        cacheConfiguration.getServersPriorityList().getServersList().clear();

        for (int i = 0; i < servers.length; i++) {
            com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer server = new com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer();
            server.setServerName(servers[i]);
            server.setPriority(i);
            cacheConfiguration.getServersPriorityList().getServersList().put(i, server);
        }

        cacheConfiguration.setClientPort(port);
        cacheConfiguration.setLoadBalance(loadBalance);

        SaveConfiguration();
        if (xml != null) {
            xml.argvalue = "";
        }
    }

    public static int GetConfigurationId() throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        int configurationId = 0;

        LoadXml();
        if (_configuration != null && _configuration.getNodeConfiguration() != null) {
            configurationId = _configuration.getNodeConfiguration().getConfigurationId();
        }

        return configurationId;
    }

    public static ClientNodeStatus GetClientNodeStatus(String cacheId) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        ClientNodeStatus status = ClientNodeStatus.Unavailable;

        LoadXml();

        if (_configuration != null && _configuration.getCacheConfigurationsMap() != null) {
            CacheConfiguration config = null;
            if ((config = _configuration.getCacheConfigurationsMap().get(cacheId.toLowerCase())) != null) {
                status = ClientNodeStatus.ClientCacheUnavailable;
            }
        }

        return status;
    }

    public static ClientConfiguration GetClientConfiguration(String cacheId) throws ManagementException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException {
        LoadXml();
        return _configuration;
    }

    public static ClientConfiguration loadConfiguration(String filePath) throws ManagementException {
        ClientConfiguration clientConfiguration = null;
        if (!Common.isNullorEmpty(filePath)) {
            ConfigurationBuilder configBuilder = new ConfigurationBuilder(filePath);
            try {
                configBuilder.RegisterRootConfigurationObject(com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration.class);
                configBuilder.ReadConfiguration();
            } catch (Exception exception) {
                throw new ManagementException(exception.getMessage(), exception);
            }

            Object[] configuration = configBuilder.getConfiguration();

            if (configuration != null && configuration.length > 0) {
                for (int i = 0; i < configuration.length; i++) {
                    clientConfiguration = (ClientConfiguration) ((configuration[i] instanceof ClientConfiguration) ? configuration[i] : null);
                    break;
                }
            }
        }
        return clientConfiguration;
    }

}
