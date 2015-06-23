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

package com.alachisoft.tayzgrid.tools;

import com.alachisoft.tayzgrid.management.*;
import com.alachisoft.tayzgrid.common.*;
import com.alachisoft.tayzgrid.common.configuration.*;
import com.alachisoft.tayzgrid.config.newdom.ServerNode;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.Iterator;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ConfigureCacheTool {

    private static ConfigureCacheParam ccParam = new ConfigureCacheParam();
    private static CacheRPCService NCache;
    private static com.alachisoft.tayzgrid.config.newdom.CacheServerConfig _SimpleCacheConfig = new com.alachisoft.tayzgrid.config.newdom.CacheServerConfig();

    /**
     * Validate all parameters in property string.
     */
    private static boolean ValidateParameters() {
        AssemblyUsage.PrintLogo(ccParam.getIsLogo());
        // Validating CacheId
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(ccParam.getCacheId())) {
            System.err.println("Error: Cache Name not specified");
            return false;
        }
        
         if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(ccParam.getPath())) {
            if (ccParam.getTopology() != null || !ccParam.getTopology().equals("")) {
                if (ccParam.getCacheSize() == -1) {
                    System.err.println("Error: Cache Size not Specified...");
                    return false;
                }
            } else {
                System.err.println("Error: Config Path not specified. (For simple case specify Topology)");
                return false;
            }
        }
         
        if(!ccParam.getTopology().contains("local"))
        {
            if(ccParam.getRange() <=0 )
            {
                System.err.println("Error: port range should be a positive integer");
                 return false;
            }
        }

        if (ccParam.getServer() == null || ccParam.getServer().equals("")) {
            System.err.println("Error: Server ip not specified");
            return false;
        } else {
            
            java.util.ArrayList servers = GetServers(ccParam.getServer());
            for (Object server : servers) 
            {
                String ip=((ServerNode)server).getIP();
                if (!IsValidIP(ip))
                {
                    System.err.println("Error: Server IP "+ip+" is Invalid");
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean IsValidIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        ip = ip.trim();
        if ((ip.length() < 6) & (ip.length() > 15)) {
            return false;
        }
        try {
            Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    private static void LogEvent(String msg) {
    }

    /**
     * The main entry point for the tool.
     */
    public static void Run(String[] args) throws UnknownHostException, Exception {

        String failedNodes = "";
        String runtimeContext = "1";
        com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[] caches = null;
        ICacheServer cacheServer = null;
        com.alachisoft.tayzgrid.config.newdom.CacheServerConfig _cacheConfig = null;
        boolean isSuccessfull = true;
        try {
            NCache = ToolsRPCService.GetRPCService();


            Object param = new ConfigureCacheParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            ccParam = (ConfigureCacheParam) param;

            if (ccParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }

            if (!ValidateParameters()) {
                return;
            }

            if (ccParam.getPort() != -1) {
                NCache.setPort(ccParam.getPort());
            }

            if (ccParam.getPath() != null && !ccParam.getPath().equals("")) {
                java.io.File temp = new File(ccParam.getPath());
                if (temp.isFile()) {
                    String extension = temp.getPath().substring(temp.getPath().lastIndexOf("."));

                    if (!extension.equals(".conf") && !extension.equals(".xml")) {
                        throw new RuntimeException("Wrong file format. Only .conf and .xml is supported.");
                    }

                } else {
                    throw new RuntimeException("Wrong Path Specified for Configuration file.");
                }

                ConfigurationBuilder builder = new ConfigurationBuilder(ccParam.getPath());
                builder.RegisterRootConfigurationObject(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig.class);
                builder.ReadConfiguration();

                if (builder.getConfiguration() != null) {
                    caches = new com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[builder.getConfiguration().length];
                    System.arraycopy(builder.getConfiguration(), 0, caches, 0, builder.getConfiguration().length);
                } else {
                    throw new RuntimeException("Configuration cannot be loaded.");
                }
                _cacheConfig = caches[0];

                if (_cacheConfig.getCacheSettings().getName() == null) {
                    _cacheConfig.getCacheSettings().setName(ccParam.getCacheId());
                }

                if (_cacheConfig.getCacheSettings().getStorage() == null) {
                    throw new RuntimeException("Cache Size is not specified.");
                }

                if (_cacheConfig.getCacheSettings().getStorage().getSize() == -1) {
                    throw new RuntimeException("Cache Size is not specified.");
                }

                if (_cacheConfig.getCacheSettings().getEvictionPolicy() == null) {
                    _cacheConfig.getCacheSettings().setEvictionPolicy(new com.alachisoft.tayzgrid.config.newdom.EvictionPolicy());
                    _cacheConfig.getCacheSettings().getEvictionPolicy().setPolicy("lru");
                    _cacheConfig.getCacheSettings().getEvictionPolicy().setDefaultPriority("normal");
                    BigDecimal temp1 = BigDecimal.valueOf(5);
                    _cacheConfig.getCacheSettings().getEvictionPolicy().setEvictionRatio(temp1);
                    _cacheConfig.getCacheSettings().getEvictionPolicy().setEnabled(true);

                }

                if (_cacheConfig.getCacheSettings().getCleanup() == null) {
                    _cacheConfig.getCacheSettings().setCleanup(new com.alachisoft.tayzgrid.config.newdom.Cleanup());
                    _cacheConfig.getCacheSettings().getCleanup().setInterval(15);
                }

                if (_cacheConfig.getCacheSettings().getNotifications() == null) {
                    _cacheConfig.getCacheSettings().setNotifications(new com.alachisoft.tayzgrid.config.newdom.Notifications());
                }

                if (_cacheConfig.getCacheSettings().getLog() == null) {
                    _cacheConfig.getCacheSettings().setLog(new com.alachisoft.tayzgrid.config.newdom.Log());
                }

                if (_cacheConfig.getCacheSettings().getPerfCounters() == null) {
                    _cacheConfig.getCacheSettings().setPerfCounters(new com.alachisoft.tayzgrid.config.newdom.PerfCounters());
                    _cacheConfig.getCacheSettings().getPerfCounters().setEnabled(true);
                }
                
                if(ccParam.getSocketPort() != -1){
                    _cacheConfig.getCacheSettings().setClientPort(ccParam.getSocketPort());
                }

                if (_cacheConfig.getCacheSettings().getCacheType().equals("clustered-cache")) {
                    if (_cacheConfig.getCacheSettings().getCacheTopology().getClusterSettings() == null) {
                        throw new RuntimeException("Cluster settings is not specified for cluster cache.");
                    }

                    if (_cacheConfig.getCacheSettings().getCacheTopology().getClusterSettings().getChannel() == null) {
                        throw new RuntimeException("Cluster Channel related settings is not specified for cluster cache.");
                    }

//                    if (_cacheConfig.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().getTcpPort() == -1) {
//                        throw new RuntimeException("Cluster Port is not specified for cluster cache.");
//                    }
                }

            } else {
                _SimpleCacheConfig.setCacheSettings(new com.alachisoft.tayzgrid.config.newdom.CacheServerConfigSetting());
                _SimpleCacheConfig.getCacheSettings().setName(ccParam.getCacheId());
                _SimpleCacheConfig.getCacheSettings().setStorage(new com.alachisoft.tayzgrid.config.newdom.Storage());
                _SimpleCacheConfig.getCacheSettings().setEvictionPolicy(new com.alachisoft.tayzgrid.config.newdom.EvictionPolicy());
                _SimpleCacheConfig.getCacheSettings().setCleanup(new com.alachisoft.tayzgrid.config.newdom.Cleanup());
                _SimpleCacheConfig.getCacheSettings().setNotifications(new com.alachisoft.tayzgrid.config.newdom.Notifications());
                _SimpleCacheConfig.getCacheSettings().setLog(new com.alachisoft.tayzgrid.config.newdom.Log());
                _SimpleCacheConfig.getCacheSettings().setPerfCounters(new com.alachisoft.tayzgrid.config.newdom.PerfCounters());
                _SimpleCacheConfig.getCacheSettings().getPerfCounters().setEnabled(true);
                _SimpleCacheConfig.getCacheSettings().getStorage().setType("heap");
                _SimpleCacheConfig.getCacheSettings().getStorage().setSize(ccParam.getCacheSize());
                _SimpleCacheConfig.getCacheSettings().getEvictionPolicy().setPolicy("lru");
                _SimpleCacheConfig.getCacheSettings().getEvictionPolicy().setDefaultPriority("normal");
                BigDecimal temp = BigDecimal.valueOf(5);
                _SimpleCacheConfig.getCacheSettings().getEvictionPolicy().setEvictionRatio(temp);
                _SimpleCacheConfig.getCacheSettings().getEvictionPolicy().setEnabled(false);
                _SimpleCacheConfig.getCacheSettings().getCleanup().setInterval(15);
                _SimpleCacheConfig.getCacheSettings().setCacheTopology(new com.alachisoft.tayzgrid.config.newdom.CacheTopology());
                _SimpleCacheConfig.getCacheSettings().getCacheTopology().setTopology(ccParam.getTopology());

                if(ccParam.getIsInProc())
                    if(ccParam.getTopology().contains("mirror") || ccParam.getTopology().contains("partition") || ccParam.getTopology().contains("replicated")){
                        System.err.println("Cluster Topology cannot be created as Inproc");
                        return;
                    }
                    else
                        _SimpleCacheConfig.getCacheSettings().setInProc(ccParam.getIsInProc());
                            
                if(ccParam.getSocketPort() != -1){
                    _SimpleCacheConfig.getCacheSettings().setClientPort(ccParam.getSocketPort());
                }
                
                if (_SimpleCacheConfig.getCacheSettings().getCacheType().equals("clustered-cache")) {
                    _SimpleCacheConfig.getCacheSettings().getCacheTopology().setClusterSettings(new com.alachisoft.tayzgrid.config.newdom.Cluster());
                    _SimpleCacheConfig.getCacheSettings().getCacheTopology().getClusterSettings().setChannel(new com.alachisoft.tayzgrid.config.newdom.Channel());
                }

                if (ccParam.getEvictionPolicy() != null && !ccParam.getEvictionPolicy().equals("")) {
                    _SimpleCacheConfig.getCacheSettings().getEvictionPolicy().setPolicy(ccParam.getEvictionPolicy());
                    _SimpleCacheConfig.getCacheSettings().getEvictionPolicy().setEnabled(true);
                }
                BigDecimal temp1 = BigDecimal.valueOf(-1);

                if (ccParam.getRatio().compareTo(temp1) != 0) {
                    _SimpleCacheConfig.getCacheSettings().getEvictionPolicy().setEvictionRatio(ccParam.getRatio());
                }

                if (ccParam.getCleanupInterval() != -1) {
                    _SimpleCacheConfig.getCacheSettings().getCleanup().setInterval(ccParam.getCleanupInterval());
                }

                if (ccParam.getDefaultPriority() != null && !ccParam.getDefaultPriority().equals("")) {
                    _SimpleCacheConfig.getCacheSettings().getEvictionPolicy().setDefaultPriority(ccParam.getDefaultPriority());
                    _SimpleCacheConfig.getCacheSettings().getEvictionPolicy().setEnabled(true);
                }


                _cacheConfig = _SimpleCacheConfig;
            }

            try {
                if (!ValidateParameterValues(_cacheConfig)) {
                    return;
                }
                _cacheConfig.getCacheSettings().setName(ccParam.getCacheId());

                byte[] userId = null;
                byte[] paswd = null;
                
                if (_cacheConfig.getCacheSettings().getCacheType().equals("clustered-cache")) {
                    if (_cacheConfig.getCacheDeployment() == null) {
                        _cacheConfig.setCacheDeployment(new com.alachisoft.tayzgrid.config.newdom.CacheDeployment());
                        _cacheConfig.getCacheDeployment().setServers(new com.alachisoft.tayzgrid.config.newdom.ServersNodes());

                    }
                    _cacheConfig.getCacheDeployment().getServers().setNodesList(GetServers(ccParam.getServer()));

                }
                //code for updating client list!!
                java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer> serverList = new java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer>();
                int serverCount = 0;
                for (Iterator it = GetServers(ccParam.getServer()).iterator(); it.hasNext();) {
                    com.alachisoft.tayzgrid.config.newdom.ServerNode node = (com.alachisoft.tayzgrid.config.newdom.ServerNode) it.next();
                    com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer tempServer = new com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer();
                    tempServer.setServerName(node.getIP());
                    serverList.put(serverCount, tempServer);
                    serverCount++;
                }
                
                int clientPort = GetMaximumClientPort(GetServers(ccParam.getServer()).iterator());
                if(clientPort == 0){
                    System.err.println("Unable to get Client Port from servers");
                    return;
                }
                com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList servers = new com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList(serverList);
                for (Iterator it = GetServers(ccParam.getServer()).iterator(); it.hasNext();) {
                    com.alachisoft.tayzgrid.config.newdom.ServerNode node = (com.alachisoft.tayzgrid.config.newdom.ServerNode) it.next();
                    NCache.setServerName(node.getIP());
                    cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                    
                    if(_cacheConfig.getCacheSettings().getClientPort() < clientPort && _cacheConfig.getCacheSettings().getClientPort() != 0 && cacheServer.IsClientPortAvailable(_cacheConfig.getCacheSettings().getClientPort()) == false){
                        System.err.println("Client Port "+_cacheConfig.getCacheSettings().getClientPort()+" is not available on either server\n");
                            return;
                    }
                   
                    if(_cacheConfig.getCacheSettings().getClientPort() == 0)
                        _cacheConfig.getCacheSettings().setClientPort(clientPort);
                    
                    System.out.println("Configure Cache " + _cacheConfig.getCacheSettings().getName() + " on Server " + NCache.getServerName()+" with Client Port "+_cacheConfig.getCacheSettings().getClientPort());
                    try {
                        
                        if (cacheServer != null) {
                            com.alachisoft.tayzgrid.config.newdom.CacheServerConfig serverConfig = cacheServer.GetNewConfiguration(_cacheConfig.getCacheSettings().getName());

                            if (serverConfig != null) {
                                throw new RuntimeException("Specified cache already exists");
                                //       return;
                            } else if (serverConfig != null && ccParam.getIsOverWrite()) {
                                if (serverConfig.getCacheDeployment() != null) {
                                    if (serverConfig.getCacheDeployment().getClientNodes() != null) {
                                        _cacheConfig.getCacheDeployment().setClientNodes(serverConfig.getCacheDeployment().getClientNodes());
                                    }
                                }
                            }
                            
                            
//                            if(!cacheServer.IsManagementPortAvailable(_cacheConfig.getCacheSettings().getManagementPort()))
//                            {throw new Exception("Management Port not available.");}
                            cacheServer.RegisterCache(_cacheConfig.getCacheSettings().getName(), _cacheConfig, "", ccParam.getIsOverWrite(), userId, paswd, ccParam.getIsHotApply());
                            cacheServer.UpdateClientServersList(_cacheConfig.getCacheSettings().getName(), servers, runtimeContext,_cacheConfig.getCacheSettings().getClientPort());
                            if (isSuccessfull == true) {
                                System.out.println("SuccessFully created Cache " + _cacheConfig.getCacheSettings().getName());
                            }
                        }
                        else
                        {throw new Exception("TayzGrid service can not be contacted");}
                    } catch (RuntimeException ex) {
                        System.err.println("Failed to Create Cache on " + NCache.getServerName());
                        System.err.println("Error Detail: " + ex.getMessage());
                        failedNodes = failedNodes + "/n" + node.getIP().toString();
                        LogEvent(ex.getMessage());
                        isSuccessfull = false;
                    } finally {
                        if (cacheServer != null) {
                            cacheServer.dispose();
                        }
                    }
                }

            } 
            catch (com.alachisoft.tayzgrid.runtime.exceptions.SecurityException se) {
                System.err.println("Failed to Create " + _cacheConfig.getCacheSettings().getName() + ". Security Exception: One or more given parameters/user rights are not valid.");
                LogEvent(se.getMessage());
                isSuccessfull = false;
            } 
            catch (RuntimeException ex) {
                System.err.println("Failed to Configure " + _cacheConfig.getCacheSettings().getName());
                if (!ex.getMessage().equals(null)) {
                    System.err.println(ex.getMessage());
                }
                LogEvent(ex.getMessage());
                isSuccessfull = false;
            }

        } catch (Exception e) {
            if (!e.getMessage().equals(null)) {
                System.err.println(e.getMessage());
            }
            isSuccessfull = false;
        } finally {
            NCache.dispose();

        }
    }

    public static java.util.ArrayList GetServers(String servers) {
        java.util.ArrayList serverList = new java.util.ArrayList();
        String[] st = servers.split("[,]", -1);
        for (int i = 0; i < st.length; i++) {
            serverList.add(new com.alachisoft.tayzgrid.config.newdom.ServerNode(st[i], false));
        }

        return serverList;
    }

    public static java.util.ArrayList SetMirrorNode(java.util.ArrayList servers) {
        int i = 0;
        if (servers.size() > 1) {
            for (Iterator it = servers.iterator(); it.hasNext();) {
                com.alachisoft.tayzgrid.config.newdom.ServerNode sn = (com.alachisoft.tayzgrid.config.newdom.ServerNode) it.next();
                if (i == servers.size() - 1) {
                    sn.setIsActiveMirrorNode(true);
                }
                i++;
            }
        }

        return servers;
    }

    private static boolean ValidateParameterValues(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig _cacheconfig) {

        if (_cacheconfig.getCacheSettings().getEvictionPolicy().getPolicy().equals("priority") || _cacheconfig.getCacheSettings().getEvictionPolicy().getPolicy().equals("lfu") || _cacheconfig.getCacheSettings().getEvictionPolicy().getPolicy().equals("lru")) {
            if (_cacheconfig.getCacheSettings().getEvictionPolicy().getPolicy().equals("priority")) {
                if (!(_cacheconfig.getCacheSettings().getEvictionPolicy().getDefaultPriority().equals("high") || _cacheconfig.getCacheSettings().getEvictionPolicy().getDefaultPriority().equals("above-normal")
                        || _cacheconfig.getCacheSettings().getEvictionPolicy().getDefaultPriority().equals("normal") || _cacheconfig.getCacheSettings().getEvictionPolicy().getDefaultPriority().equals("below-normal")
                        || _cacheconfig.getCacheSettings().getEvictionPolicy().getDefaultPriority().equals("low"))) {
                    System.err.println("Error: Default Priority is not Valid");
                    return false;
                }
            }
        } else {
            System.err.println("Error: Eviction-policy is not Valid");
            return false;
        }
        BigDecimal max = BigDecimal.valueOf(100);

        if (_cacheconfig.getCacheSettings().getEvictionPolicy().getEvictionRatio().compareTo(max) > 0) {
            _cacheconfig.getCacheSettings().getEvictionPolicy().setEvictionRatio(max);
        }
        // Validating Topology
        String topology = _cacheconfig.getCacheSettings().getCacheTopology().getTopology();

        if (_cacheconfig.getCacheSettings().getCacheTopology().getClusterSettings() != null && _cacheconfig.getCacheSettings().getCacheTopology().getClusterSettings().getChannel() != null && _cacheconfig.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().getTcpPort() > 0 && "local-cache".equals(topology)) {
            System.err.println("Error: Specify cluster topology");
            return false;
        }

        if (!topology.equals("local-cache")) {
            if (_cacheconfig.getCacheSettings().getCacheTopology().getClusterSettings() != null) {
                topology = _cacheconfig.getCacheSettings().getCacheTopology().getTopology();
            }
        }

        if ("local-cache".equals(topology) || "replicated".equals(topology) || "partitioned".equals(topology)) {
        } else if("mirrored".equals(topology)) {
            System.err.println("Error: Mirror Topology is only available in Enterprise edition.");
            return false;
        } else if("partitioned-replica".equals(topology)) {
            System.err.println("Error: Partition of Replica Topology is only available in Enterprise edition.");
            return false;
        } else {
            System.err.println("Error: Topology is not Valid");
            return false;
        }

        return true;

    }
    
    public static int GetMaximumClientPort(Iterator it){
    
        int maxPort = 0;
        while(it.hasNext())
        {
           try{
               com.alachisoft.tayzgrid.config.newdom.ServerNode node = (com.alachisoft.tayzgrid.config.newdom.ServerNode) it.next();
           
                NCache.setServerName(node.getIP());
                ICacheServer cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                int tempPort = cacheServer.GetMaxSocketPort();
                if(maxPort < tempPort)
                    maxPort = tempPort;
            }
           catch(Exception ex)
           {
               maxPort = 0;
               break;
           }
        }
        return maxPort;
    }
}
