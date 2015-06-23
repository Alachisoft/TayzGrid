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
import com.alachisoft.tayzgrid.common.enums.*;
import com.alachisoft.tayzgrid.config.newdom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.newdom.ServerNode;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;

import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import java.net.InetAddress;
import java.util.Iterator;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.UnknownHostException;

/**
 * Summary description for AddNodeTool.
 */
public final class AddNodeTool {

    /**
     * NCache service controller.
     */
    private static CacheRPCService NCache;
    /**
     * Re-register flag specified at the command line.
     */
    private static boolean reregister = true;
    /**
     * Configuration ids specified at the command line.
     */
    private static java.util.ArrayList s_configId = new java.util.ArrayList();
    
    /**
     * Partition name specified at the command line.
     */
    private static String _partId = "";
    private static AddNodeParam cParam = new AddNodeParam();

    private static boolean ValidateParameters() {
        AssemblyUsage.PrintLogo(cParam.getIsLogo());
        // Validating CacheId
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getCacheId())) {
            System.err.println("Error: Cache Name not specified");
            return false;
        }

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getNewServer())) {
            System.err.println("Error: New Server is not specified");
            return false;
        }

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getExistingServer())) {
            System.err.println("Error: Existing Server is not specified");
            return false;
        }
        

        return true;
    }

    private static void LogEvent(String msg) {
    }

    /**
     * The main entry point for the tool.
     */
    public static void Run(String[] args) throws Exception {

        String runtimeContext="1";
        try {
            NCache = ToolsRPCService.GetRPCService();
            boolean isSuccessFull=false;
            Object param = new AddNodeParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (AddNodeParam) param;
            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }

            if (!ValidateParameters()) {
                return;
            }
            if (cParam.getPort() != -1) {
                NCache.setPort(cParam.getPort());
            }
            try{
                if (IsValidAddress(cParam.getNewServer())) {
                    NCache.setServerName(cParam.getNewServer());
                    ICacheServer temp = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                    if(temp == null)
                    {throw new Exception("TayzGrid service can not be contacted");}
                }
            }
            catch(RuntimeException exp)
            {
                System.err.println("Failed to Connect to IP " + cParam.getNewServer() + ".");
                System.err.println("Error Detail: " + exp.getMessage());
                return;
            }
            
            try {
                if (cParam.getExistingServer() != null && !cParam.getExistingServer().equals("")) {
                    NCache.setServerName(cParam.getExistingServer());
                }
                InetAddress address = null;
                String clusterIp;

                ICacheServer m = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                if (!IsValidAddress(cParam.getNewServer())) {
                    clusterIp = m.GetClusterIP();
                    if (clusterIp != null && !clusterIp.equals("")) {
                        cParam.setNewServer(clusterIp);
                    }
                }
                
                if (!IsValidAddress(cParam.getExistingServer())) {

                    clusterIp = m.GetClusterIP();
                    if (clusterIp != null && !clusterIp.equals("")) {
                        cParam.setExistingServer(clusterIp);
                    }
                }

                CacheStatusOnServerContainer result;
                result = m.IsClusteredCache(cParam.getCacheId());

                if (result.cacheStatus == CacheStatusOnServer.Unregistered) {
                    throw new RuntimeException("The requested cache is not registered on the source node");
                } else if (result.cacheStatus == CacheStatusOnServer.LocalCache) {
                    throw new RuntimeException("addnode can be used with clustered caches only");
                }

                System.out.println("Adding " + cParam.getNewServer() + " to cache " + cParam.getCacheId());
                com.alachisoft.tayzgrid.common.monitoring.Node[] nodes = m.GetCacheServers(cParam.getCacheId());

                for (com.alachisoft.tayzgrid.common.monitoring.Node node : nodes) {
                    if (node.getAddress().getIpAddress().getHostName().toString().equals(cParam.getNewServer())) {
                        System.err.println("Error: destination node already exists");
                        return;
                    }
                    String[] split = node.getAddress().getIpAddress().toString().split("/");
                    if(split[1].equals(cParam.getNewServer()))
                    {
                        System.err.println("Error: destination node already exists");
                        return;
                    }
                }

                NewCacheRegisterationInfo info = m.GetNewUpdatedCacheConfiguration(cParam.getCacheId(), _partId, cParam.getNewServer(), true);

                byte[] userId = null;
                byte[] paswd = null;
                //first of all try to register the cache on the destination server...
                try {
                    NCache.setServerName(cParam.getNewServer());
                    m = NCache.GetCacheServer(new TimeSpan(0, 0, 30));

                    address = InetAddress.getByName(cParam.getNewServer());
                    clusterIp = m.GetClusterIP();
                    if (clusterIp != null && !clusterIp.equals("")) {
                        cParam.setNewServer(clusterIp);
                    }

                    m.RegisterCache(cParam.getCacheId(), info.getUpdatedCacheConfig(), _partId, true, userId, paswd, false);
                    isSuccessFull=true;
                    
                    
                } catch (RuntimeException ex) {
                    System.err.println("Failed to Create Cache on " + NCache.getServerName() + ".");
                    System.err.println("Error Detail: " + ex.getMessage() + ".");
                    isSuccessFull=false;
                    LogEvent(ex.getMessage());
                } finally {
                }
                // Now update the cache configurations on all the servers where the cache
                //is registered...
                for (Iterator ite = info.getAffectedNodes().iterator(); ite.hasNext();) {
                    String serverName = (String) ite.next();

                    if (info.getAffectedPartitions().size() > 0) {
                        for (Iterator iterator = info.getAffectedPartitions().iterator(); iterator.hasNext();) {
                            String partId = (String) iterator.next();
                            try {
                                NCache.setServerName(serverName);
                                address = InetAddress.getByName(NCache.getServerName());
                                clusterIp = m.GetClusterIP();
                                if (clusterIp != null && !clusterIp.equals("")) {
                                    NCache.setServerName(clusterIp);
                                }
                                reregister = true;
                                m = NCache.GetCacheServer(new TimeSpan(0, 0, 30));

                                m.RegisterCache(cParam.getCacheId(), info.getUpdatedCacheConfig(), _partId, true, EncryptionUtil.Encrypt(""), EncryptionUtil.Encrypt(""), false);
                                isSuccessFull=true;
                                
                            } catch (RuntimeException ex) {
                                System.err.println("Failed to Updated Cache Configuration on " + NCache.getServerName());
                                System.err.println("Error Detail: " + ex.getMessage());
                                isSuccessFull=false;
                                LogEvent(ex.getMessage());
                            } finally {
                            }
                        }
                    } else {
                        try {
                            NCache.setServerName(serverName);
                            m=NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                            address = InetAddress.getByName(NCache.getServerName());
                            clusterIp = m.GetClusterIP();
                            if (clusterIp != null && !clusterIp.equals("")) {
                                NCache.setServerName(clusterIp);
                            }
                            reregister = true;
                            m = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                            m.RegisterCache(cParam.getCacheId(), info.getUpdatedCacheConfig(), _partId, true, EncryptionUtil.Encrypt(""), EncryptionUtil.Encrypt(""), false);
                            isSuccessFull=true;
                        } catch (RuntimeException ex) {
                            System.err.println("Failed to Create Cache on " + NCache.getServerName());
                            System.err.println("Error Detail: " + ex.getMessage());
                            isSuccessFull=false;
                            LogEvent(ex.getMessage());
                        } finally {
                        }
                    }
                }
                java.util.HashMap<String, java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer>> serversPriorityList = new java.util.HashMap<String, java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer>>();
                    
                    try
                    {

                        java.util.ArrayList clusterNodes = info.getUpdatedCacheConfig().getCacheDeployment().getServers().getNodesList();

                        for (Object temp : clusterNodes)
                        {
                            com.alachisoft.tayzgrid.config.newdom.ServerNode nodei= (com.alachisoft.tayzgrid.config.newdom.ServerNode)temp;
                            serversPriorityList.put(nodei.getIP(), ToolsRPCService.GetPrioritizedServerListForClient(nodei.getIP(), cParam.getCacheId(), clusterNodes));
                        }

                        com.alachisoft.tayzgrid.config.newdom.ServerNode nodeForClientList = new com.alachisoft.tayzgrid.config.newdom.ServerNode(); //Hack: priority list requires a serverIP and client Ip cant be used hence!!
                        for (Object temp2: clusterNodes)
                        {
                            com.alachisoft.tayzgrid.config.newdom.ServerNode node =(com.alachisoft.tayzgrid.config.newdom.ServerNode) temp2;
                            NCache.setServerName(node.getIP());
                            ICacheServer _cacheServer = NCache.GetCacheServer(new TimeSpan( 0, 0, 30));
                            com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList _cacheServerList = new com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList(serversPriorityList.get(node.getIP()));
                            _cacheServer.UpdateClientServersList(cParam.getCacheId(), _cacheServerList,runtimeContext,info.getUpdatedCacheConfig().getCacheSettings().getClientPort());
                            isSuccessFull=true;
                            
                            nodeForClientList = node;
                        }

                        java.util.ArrayList<com.alachisoft.tayzgrid.config.newdom.ClientNode> clientNodeList = info.getUpdatedCacheConfig().getCacheDeployment().getClientNodes().getNodesList();
                        for (Object temp3 : clientNodeList)
                        {
                                com.alachisoft.tayzgrid.config.newdom.ClientNode node=(com.alachisoft.tayzgrid.config.newdom.ClientNode) temp3;
                                NCache.setServerName(node.getName());
                                ICacheServer _cacheServer = NCache.GetCacheServer(new TimeSpan( 0, 0, 30));
                                com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList _cacheServerList = new  com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList(serversPriorityList.get(nodeForClientList.getIP()));
                                _cacheServer.UpdateClientServersList(cParam.getCacheId(), _cacheServerList,runtimeContext,info.getUpdatedCacheConfig().getCacheSettings().getClientPort());
                                isSuccessFull=true;
                                
                        }
                }
                catch (RuntimeException e)
                {
                    isSuccessFull=false;
                }
                if(isSuccessFull)
                {
                    System.out.println("SuccessFully Added Node "+ cParam.getNewServer()+" in cache "+cParam.getCacheId());
                }
            }
            catch (java.lang.SecurityException e) {
                System.err.println("Failed to add " + NCache.getServerName().toLowerCase() + " to " + cParam.getCacheId() + ". Error: " + e.getMessage());
            } 
            catch (ConfigurationException e) {
                System.err.println("Failed to add " + NCache.getServerName().toLowerCase() + " to " + cParam.getCacheId() + ". Error: " + e.getMessage());
            } catch (RuntimeException e) {
                System.err.println("Failed to add " + NCache.getServerName().toLowerCase() + " to " + cParam.getCacheId() + ". Error: " + e.getMessage());
            }
            //}
        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
        } finally {
            NCache.dispose();
        }
    }

    public static java.util.ArrayList SetMirrorNode(java.util.ArrayList servers) {

        if (servers.size() > 1) {
            for (Iterator it = servers.iterator(); it.hasNext();) {
                ServerNode sn = (ServerNode) it.next();
                if (sn.getIP().toString().equals(cParam.getNewServer())) {
                    sn.setIsActiveMirrorNode(true);
                }
            }
        }

        return servers;
    }

    private static boolean IsValidAddress(String iPaddress) {

        InetAddress address = null;

        try {
            address = InetAddress.getByName(iPaddress);
        } catch (UnknownHostException e) {
            return false;
        }

        return true;
    }
}