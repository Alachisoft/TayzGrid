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

package com.alachisoft.tayzgrid.management;

import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.common.EncryptionUtil;
import java.net.UnknownHostException;
import java.net.InetAddress;

/**
 * Provides the public interface for the CacheServer.
 *
 *
 */
public class CacheServerModerator
{

    private static CacheRPCService _ncacheService;
    private static final int _port = 8270;
    private static final int JvCacheManagementPort=8270;
    private static final int NCacheManagementPort=8250;
    
    private static final String LOCALCACHE = "local-cache";



    public CacheServerModerator() throws UnknownHostException
    {
    }

    public static void StartCache(String cacheId, String userId, String password) throws ManagementException
    {
        try
        {
            if (_ncacheService == null)
            {
                _ncacheService = new CacheRPCService(java.net.InetAddress.getLocalHost().getHostName());
            }
            _ncacheService.setServerName(java.net.InetAddress.getLocalHost().getHostName());
            _ncacheService.setPort(_port);
            ICacheServer cs = _ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
            if (cs != null)
            {
                cs.StartCache(cacheId, EncryptionUtil.Encrypt(userId), EncryptionUtil.Encrypt(password));
            }
        }
        catch (Exception ex)
        {
            throw new ManagementException(ex.getMessage());
        }
    }
    public static void StartCache(String cacheId, String serverName, String userId, String password) throws ManagementException
    {
        try
        {
            if (_ncacheService == null)
            {
                _ncacheService = new CacheRPCService(serverName);
            }
            _ncacheService.setServerName(serverName);
            _ncacheService.setPort(_port);
            ICacheServer cs = _ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
            if (cs != null)
            {
                cs.StartCache(cacheId, EncryptionUtil.Encrypt(userId), EncryptionUtil.Encrypt(password));
            }
        }
        catch (Exception ex)
        {
            throw new ManagementException(ex.getMessage());
        }
    }

    public static void StopCache(String cacheId, String userId, String password, boolean isGracefulShutdown) throws ManagementException
    {
        try
        {
            if (_ncacheService == null)
            {
                _ncacheService = new CacheRPCService(java.net.InetAddress.getLocalHost().getHostName());
            }
            _ncacheService.setServerName(java.net.InetAddress.getLocalHost().getHostName());
            _ncacheService.setPort(_port);
            ICacheServer cs = _ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
            if (cs != null)
            {
                cs.StopCache(cacheId, EncryptionUtil.Encrypt(userId), EncryptionUtil.Encrypt(password), isGracefulShutdown);
            }
        }
        catch (Exception ex)
        {
            throw new ManagementException(ex.getMessage());
        }
    }
    public static void StopCache(String cacheId, String serverName, String userId, String password, boolean isGracefulShutdown) throws ManagementException
    {
        try
        {
            if (_ncacheService == null)
            {
                _ncacheService = new CacheRPCService(serverName);
            }
            _ncacheService.setServerName(serverName);
            _ncacheService.setPort(_port);
            ICacheServer cs = _ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
            if (cs != null)
            {


                cs.StopCache(cacheId, EncryptionUtil.Encrypt(userId), EncryptionUtil.Encrypt(password), isGracefulShutdown);

            }
        }
        catch (Exception ex)
        {
            throw new ManagementException(ex.getMessage());
        }
    }
    
    public static java.util.HashMap<com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode, java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient>> getCacheClients(String cacheName, String initialNodeName,com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext context, int port) throws ManagementException
    {
        _ncacheService = GetCacheService(context);
        
        if (port != 0) 
        {
            _ncacheService.setPort(port);
        }
        
        String startingNode = initialNodeName;
        com.alachisoft.tayzgrid.config.dom.CacheServerConfig cacheServerConfig = null;
        com.alachisoft.tayzgrid.management.ICacheServer cacheServer = null;
        java.util.HashMap<com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode, java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient>> clientList = new java.util.HashMap<com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode, java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient>>();

        try {
            if (initialNodeName.equals("")) 
            {
                cacheServerConfig = getCacheConfigThroughClientConfig(cacheName);

                if (cacheServerConfig == null) 
                {
                    throw new ManagementException("cache with name " + cacheName + " not found in client.conf");
                }
            } 
            else 
            {
                //if initial node not up then ???
                _ncacheService.setServerName(initialNodeName);
                cacheServer = _ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
                
                if (cacheServer == null) 
                {
                    throw new ManagementException("provided initial node not available");
                }

                cacheServerConfig = cacheServer.GetCacheConfiguration(cacheName);
                
                if (cacheServerConfig == null) 
                {
                    throw new ManagementException("cache with name " + cacheName + " not registered on specified node");
                }
            }

            //Copied Code from NCManager                        

            //For Local Cache
            if (cacheServerConfig.getCacheType().equalsIgnoreCase(LOCALCACHE))
            {
                if (cacheServerConfig.getInProc()) 
                {
                    throw new IllegalArgumentException("API is not supported for Local Inproc Cache");
                }

                _ncacheService.setServerName(InetAddress.getLocalHost().getHostName());
                cacheServer =_ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));

                if (cacheServer != null) 
                {

                    if (cacheServer.IsRunning(cacheName)) 
                    {
                        com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode serverNode = new com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode();
                        serverNode.setServerIP(InetAddress.getLocalHost().getHostName());

                        java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats> clients = cacheServer.GetClientProcessStats(cacheServerConfig.getName());
                        java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient> list = new java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient>();
                        
                        for (com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats clientNode : clients) 
                        {
                            com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient cacheClient = new com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient();
                            
                            cacheClient.setClientIP(clientNode.getAddress().getIpAddress().toString());
                            cacheClient.setPort(clientNode.getAddress().getPort());
                            cacheClient.setProcessID(clientNode.getProcessID());
                        
                            list.add(cacheClient);
                        }
                        
                        clientList.put(serverNode, list);
                    }

                }
                
                return clientList;

            } 
            //For Clustered Cache
            else 
            {
                java.util.ArrayList initialHost = InitialHostList(cacheServerConfig.getCluster().getChannel().getInitialHosts());

                for (Object host : initialHost) 
                {
                    try 
                    {
                        _ncacheService.setServerName((String) host);
                        cacheServer =_ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
                    
                        if (cacheServer.IsRunning(cacheName)) 
                        {                        
                            com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode serverNode = new com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode();
                         
                            serverNode.setServerIP((String) ((host instanceof String) ? host : null));
                            serverNode.setPort(cacheServerConfig.getCluster().getChannel().getTcpPort());

                            java.util.ArrayList<com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats> clients = cacheServer.GetClientProcessStats(cacheServerConfig.getName());
                            java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient> list = new java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient>();
                            
                            for (com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats clientNode : clients) 
                            {
                                com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient cacheClient = new com.alachisoft.tayzgrid.runtime.cachemanagement.CacheClient();
                            
                                cacheClient.setClientIP(clientNode.getAddress().getIpAddress().toString());
                                cacheClient.setPort(clientNode.getAddress().getPort());
                                cacheClient.setProcessID(clientNode.getProcessID());
                              
                                list.add(cacheClient);
                            }
                            
                            clientList.put(serverNode, list);
                        }

                    }
                    catch (RuntimeException e) 
                    {
                    }
                }

                return clientList;
            }
        } catch (Exception ex) {
            throw new ManagementException(ex.getMessage());
        } finally {
            _ncacheService.dispose();
            _ncacheService=null;
        }                
    }

    public static com.alachisoft.tayzgrid.runtime.cachemanagement.CacheHealth getCacheHealth(String cacheName, String initialNodeName, com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext context, int port) throws ManagementException
    {
        _ncacheService = GetCacheService(context);

        if (port != 0) 
        {
            _ncacheService.setPort(port);
        }

        String startingNode = initialNodeName;
        int _runningNodes = 0;
        com.alachisoft.tayzgrid.config.dom.CacheServerConfig cacheServerConfig = null;
        com.alachisoft.tayzgrid.management.ICacheServer cacheServer = null;
        com.alachisoft.tayzgrid.runtime.cachemanagement.CacheHealth cacheHealth = new com.alachisoft.tayzgrid.runtime.cachemanagement.CacheHealth();

        try 
        {
            if (initialNodeName.equals("")) 
            {
                cacheServerConfig = getCacheConfigThroughClientConfig(cacheName);

                if (cacheServerConfig == null) 
                {
                    return cacheHealth;
                }
            }
            else 
            {
                //if initial node not up then ???
                _ncacheService.setServerName(initialNodeName);
                cacheServer = _ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
            
                if (cacheServer == null) 
                {
                    throw new ManagementException("provided initial node not available");
                }

                cacheServerConfig = cacheServer.GetCacheConfiguration(cacheName);
                
                if (cacheServerConfig == null) 
                {
                    throw new ManagementException("cache with name " + cacheName + " not registered on specified node");
                }
            }

            //For Local Cache
            if (cacheServerConfig.getCacheType().equalsIgnoreCase(LOCALCACHE)) 
            {
                if (cacheServerConfig.getInProc()) 
                {
                    throw new IllegalArgumentException("API is not supported for Local Inproc Cache");
                }

                _ncacheService.setServerName(InetAddress.getLocalHost().getHostName());
                cacheServer = _ncacheService.GetCacheServer(new TimeSpan( 0, 0, 30));

                if (cacheServer != null) 
                {
                    cacheHealth.setCacheName(cacheServerConfig.getName());
                    cacheHealth.setTopology(com.alachisoft.tayzgrid.runtime.cachemanagement.CacheTopology.LocalOutproc);
                    cacheHealth.setStatus(com.alachisoft.tayzgrid.runtime.cachemanagement.CacheStatus.Stopped);
                
                    if (cacheServer.IsRunning(cacheHealth.getCacheName())) 
                    {
                        cacheHealth.setStatus(com.alachisoft.tayzgrid.runtime.cachemanagement.CacheStatus.Running);
                    }
                }
                
                return cacheHealth;
            } //For Clustered Cache
            else
            {
                cacheHealth.setCacheName(cacheServerConfig.getName());
                cacheHealth.setTopology(GetCacheTopology(cacheServerConfig.getCluster().getCacheType()));
                cacheHealth.setStatus(com.alachisoft.tayzgrid.runtime.cachemanagement.CacheStatus.Stopped);

                java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.NodeStatus> serverNodesList = new java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.NodeStatus>();

                java.util.ArrayList initialHost = InitialHostList(cacheServerConfig.getCluster().getChannel().getInitialHosts());
                
                for (Object host : initialHost) 
                {
                    com.alachisoft.tayzgrid.runtime.cachemanagement.NodeStatus nodeStats = new com.alachisoft.tayzgrid.runtime.cachemanagement.NodeStatus();
                    try 
                    {
                        nodeStats.setNodeInfo(new com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode());
                        nodeStats.getNodeInfo().setServerIP((String) host);
                        nodeStats.getNodeInfo().setPort(cacheServerConfig.getCluster().getChannel().getTcpPort());
                        nodeStats.setConnectivityStatus(com.alachisoft.tayzgrid.runtime.cachemanagement.ConnectivityStatus.CacheStoped);

                        _ncacheService.setServerName((String) host);
                        
                        cacheServer = _ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
                        
                        if (cacheServer.IsRunning(cacheName)) 
                        {
                            cacheHealth.setStatus(com.alachisoft.tayzgrid.runtime.cachemanagement.CacheStatus.Running);
                            
                            nodeStats.setConnectivityStatus(com.alachisoft.tayzgrid.runtime.cachemanagement.ConnectivityStatus.Running);
                            
                            com.alachisoft.tayzgrid.caching.statistics.CacheStatistics cacheStats = cacheServer.GetStatistics(cacheName);
                            
                            com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics clusterCacheStats = (com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics) ((cacheStats instanceof com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics) ? cacheStats : null);
                            
                            java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode> connectedNodesList = new java.util.ArrayList<com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode>();

                            nodeStats.getNodeInfo().setServerIP(clusterCacheStats.getLocalNode().getAddress().getIpAddress().toString());
                            nodeStats.getNodeInfo().setPort(clusterCacheStats.getLocalNode().getAddress().getPort());
                            
                            if (clusterCacheStats.getLocalNode().getIsStartedAsMirror()) 
                            {
                                nodeStats.getNodeInfo().setIsReplica(true);
                            }

                            for (Object node : clusterCacheStats.getNodes()) 
                            {
                                com.alachisoft.tayzgrid.caching.statistics.NodeInfo connectedNode=(com.alachisoft.tayzgrid.caching.statistics.NodeInfo) node;
                                com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode tempNode = new com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode();
                                
                                if (!clusterCacheStats.getLocalNode().getAddress().equals(connectedNode.getAddress())) 
                                {
                                    tempNode.setServerIP(connectedNode.getAddress().getIpAddress().toString());
                                    tempNode.setPort(connectedNode.getAddress().getPort());

                                    if (connectedNode.getIsStartedAsMirror()) 
                                    {
                                        tempNode.setIsReplica(true);
                                    }

                                    connectedNodesList.add(tempNode);
                                }
                            }

                            nodeStats.setConnectedNodes(connectedNodesList.toArray(new com.alachisoft.tayzgrid.runtime.cachemanagement.ServerNode[0]));
                            
                            _runningNodes++;
                        }
                        else 
                        {
                            nodeStats.setConnectedNodes(null);
                        }

                    } catch (RuntimeException e) {
                    }

                    serverNodesList.add(nodeStats);
                }

                for (int i = 0; i < serverNodesList.size(); i++) 
                {
                    com.alachisoft.tayzgrid.runtime.cachemanagement.NodeStatus node = (com.alachisoft.tayzgrid.runtime.cachemanagement.NodeStatus) serverNodesList.get(i);
                    
                    if (node.getConnectedNodes() != null) 
                    {
                        if (node.getConnectedNodes().length == (_runningNodes - 1)) 
                        {
                            node.setConnectivityStatus(com.alachisoft.tayzgrid.runtime.cachemanagement.ConnectivityStatus.FullyConnected);
                        }
                        else if (node.getConnectedNodes().length < (_runningNodes - 1)) 
                        {
                            node.setConnectivityStatus(com.alachisoft.tayzgrid.runtime.cachemanagement.ConnectivityStatus.PartialConnected);
                        }
                    }
                }
                cacheHealth.setServerNodesStatus(serverNodesList.toArray(new com.alachisoft.tayzgrid.runtime.cachemanagement.NodeStatus[0]));

            }
        }
        catch (Exception ex) 
        {
            throw new ManagementException(ex.getMessage());
        }
        finally 
        {
            _ncacheService.dispose();
            _ncacheService=null;
        }
        
        return cacheHealth;
    }

    private static CacheRPCService GetCacheService(com.alachisoft.tayzgrid.runtime.cachemanagement.CacheContext context) {
        try {
            switch (context) {
                case JvCache:
                    return new CacheRPCService("",JvCacheManagementPort);
                case NCache:
                    return new CacheRPCService("",NCacheManagementPort);
            }
        } catch (Exception ex) {
        }

        return null;
    }

    private static java.util.ArrayList InitialHostList(String initialHostsColl) {
        java.util.ArrayList list = new java.util.ArrayList(5);
        String[] commaSplit = initialHostsColl.split("[,]", -1);
        for (String initialHost : commaSplit) {
            String[] split = initialHost.split("\\[");
            list.add(split[0]);
        }
        return list;
    }

    private static com.alachisoft.tayzgrid.runtime.cachemanagement.CacheTopology GetCacheTopology(String topologyType) {
        
        if (topologyType.equals("replicated") || topologyType.equals("replicated-server")) {
            return com.alachisoft.tayzgrid.runtime.cachemanagement.CacheTopology.Replicated;
        } else if (topologyType.equals("partitioned") || topologyType.equals("partitioned-server")) {
            return com.alachisoft.tayzgrid.runtime.cachemanagement.CacheTopology.Partitioned;
        } else {
            return com.alachisoft.tayzgrid.runtime.cachemanagement.CacheTopology.None;
        }
    }

    private static com.alachisoft.tayzgrid.config.dom.CacheServerConfig getCacheConfigThroughClientConfig(String cacheName) throws ManagementException,Exception {
        com.alachisoft.tayzgrid.config.dom.CacheServerConfig cacheServerConfig = null;
        com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer[] serverNodes = null;
        com.alachisoft.tayzgrid.management.ICacheServer cacheServer = null;

        //Get Server Info from Client.nconf for specified cacheName
        com.alachisoft.tayzgrid.management.clientconfiguration.ClientConfigManager.LoadConfiguration();
        com.alachisoft.tayzgrid.management.clientconfiguration.dom.ClientConfiguration clientConfiguration = com.alachisoft.tayzgrid.management.clientconfiguration.ClientConfigManager.GetClientConfiguration(cacheName);
        if (clientConfiguration != null) {
            java.util.HashMap<String, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheConfiguration> cacheConfigurationMap = clientConfiguration.getCacheConfigurationsMap();
            com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheConfiguration cacheClientConfiguration = null;
            
            cacheClientConfiguration = cacheConfigurationMap.get(cacheName);

            if (cacheClientConfiguration == null) {
                throw new ManagementException("cache not found in client.ncconf");
            }

            serverNodes = cacheClientConfiguration.getServers();

            for (com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer node : serverNodes) {
                try {
                    _ncacheService.setServerName(node.getServerName());
                    cacheServer = _ncacheService.GetCacheServer(new TimeSpan(0, 0, 30));
                    if (cacheServer != null) {
                        cacheServerConfig = cacheServer.GetCacheConfiguration(cacheName);
                        if (cacheServerConfig != null) {
                            break;
                        }
                    }

                } catch (Exception ex) {
                }
            }

        } else {
            throw new ManagementException("error while fetching info from client.ncconf");
        }
        return cacheServerConfig;
    }
    
    
}
