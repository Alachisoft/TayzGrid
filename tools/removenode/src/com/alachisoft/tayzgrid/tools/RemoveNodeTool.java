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
import com.alachisoft.tayzgrid.config.newdom.ClientNode;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.InetAddress;
import java.util.Iterator;

/**
 * Summary description for RemoveNodeTool.
 */
public final class RemoveNodeTool {

    /**
     * NCache service controller.
     */
    private static CacheRPCService NCache;
    private static String _partId = "";
    private static RemoveNodeParam cParam = new RemoveNodeParam();

    private static boolean ValidateParameters() {
        // Validating CacheId
        
        AssemblyUsage.PrintLogo(cParam.getIsLogo());

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getCacheId())) {
            System.err.println("Error: Cache Name not specified");
            return false;
        }


        return true;
    }

    private static void LogEvent(String msg) {
    }

    public static void Run(String[] args) throws Exception {
        try {
            NCache = ToolsRPCService.GetRPCService();

            Object param = new RemoveNodeParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (RemoveNodeParam) param;
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
            if (cParam.getServer() != null && !cParam.getServer().equals("")) {
                NCache.setServerName(cParam.getServer());
            } else {
                cParam.setServer(NCache.getServerName());
            }

            ICacheServer m = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
            if (m != null) {
                try {
                    InetAddress address = null;
                    String clusterIp;
                    try {
                        address = InetAddress.getByName(NCache.getServerName());
                        clusterIp = m.GetClusterIP();
                        if (clusterIp != null && !clusterIp.equals("")) {
                            NCache.setServerName(clusterIp);
                        }
                    } catch (Exception e) {
                    }

                    CacheStatusOnServerContainer result = m.IsClusteredCache(cParam.getCacheId());
                    if (result.cacheStatus == CacheStatusOnServer.Unregistered) {
                        throw new RuntimeException("The requested cache is not registered on the specified server");
                    } else if (result.cacheStatus == CacheStatusOnServer.LocalCache) {
                        throw new RuntimeException("removenode can be used with clustered caches only");
                    }
                    byte[] userId = null;
                    byte[] paswd = null;    
                
                    System.out.println("Removing " + NCache.getServerName().toLowerCase() + " from the cache " + cParam.getCacheId());
                    NewCacheRegisterationInfo info = m.GetNewUpdatedCacheConfiguration(cParam.getCacheId(), null, NCache.getServerName(), false);
                    
                    m.UnregisterCache(cParam.getCacheId(), _partId, userId, paswd,false,true);
                    
                    for (Iterator it = info.getAffectedNodes().iterator(); it.hasNext();) {
                        String serverName = (String) it.next();
                        NCache.setServerName(serverName);
                        m = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                        m.RegisterCache(cParam.getCacheId(), info.getUpdatedCacheConfig(), _partId, true, userId, paswd, false);
                    }
                    
                    java.util.HashMap<String, java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer>> serversPriorityList = new java.util.HashMap<String, java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer>>();
                    try{
                        java.util.ArrayList clusterNodes = info.getUpdatedCacheConfig().getCacheDeployment().getServers().getNodesList();
                        for (Object temp : clusterNodes)
                        {
                            com.alachisoft.tayzgrid.config.newdom.ServerNode nodei=(com.alachisoft.tayzgrid.config.newdom.ServerNode) temp;
                            serversPriorityList.put(nodei.getIP(), ToolsRPCService.GetPrioritizedServerListForClient(nodei.getIP(), cParam.getCacheId(), clusterNodes));
                        }
                        for ( Object temp : clusterNodes){
                            com.alachisoft.tayzgrid.config.newdom.ServerNode node = (com.alachisoft.tayzgrid.config.newdom.ServerNode) temp;
                            NCache.setServerName( node.getIP());
                            ICacheServer _cacheServer = NCache.GetCacheServer(new TimeSpan( 0, 0, 30));
                            com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList _cacheServerList =  new com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList(serversPriorityList.get(node.getIP()));
                            //CacheInfo _info = _cacheServer.GetCacheInfo(cParam.getCacheId());
                            _cacheServer.UpdateClientServersList(cParam.getCacheId(), _cacheServerList, RtContextValue.NCACHE.toString(),info.getUpdatedCacheConfig().getCacheSettings().getClientPort());
                        }
                        
                        serversPriorityList.clear();
                        //Remove from ClientNodes
                        clusterNodes = info.getUpdatedCacheConfig().getCacheDeployment().getServers().getNodesList();
                        java.util.ArrayList<ClientNode> clusterClientNodes = info.getUpdatedCacheConfig().getCacheDeployment().getClientNodes().getNodesList();                  
                        for ( Object temp : clusterClientNodes)
                        {
                            ClientNode nodei=(ClientNode)temp;
                            serversPriorityList.put(nodei.getName(), ToolsRPCService.GetPrioritizedServerListForClient(nodei.getName(), cParam.getCacheId(), clusterNodes));
                        }
                        for (Object t : clusterClientNodes)
                        {
                            ClientNode node=(ClientNode) t;
                            NCache.setServerName(node.getName());
                            ICacheServer _cacheServer = NCache.GetCacheServer(new TimeSpan( 0, 0, 30));
                            com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList _cacheServerList = new com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList(serversPriorityList.get(node.getName()));
                            CacheInfo _info = _cacheServer.GetCacheInfo(cParam.getCacheId());
                            _cacheServer.UpdateClientServersList(cParam.getCacheId(), _cacheServerList, node.getClientRuntimeContextString(),_info.getSocketServerPort());
                        }
                    }catch(Exception exp)
                    {
                    }
                    System.out.println(" Node removed successfully from cache " + cParam.getCacheId());
                } 
                
                catch (com.alachisoft.tayzgrid.runtime.exceptions.SecurityException e) {
                    System.err.println("Failed to remove " + NCache.getServerName().toLowerCase() + " from " + cParam.getCacheId() + " Error: " + e.getMessage());
                } 
                catch (RuntimeException e) {
                    System.err.println("Failed to remove " + NCache.getServerName().toLowerCase() + " from " + cParam.getCacheId() + " Error: " + e.getMessage());
                }
            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
        } finally {
            NCache.dispose();
        }
    }

    public static java.util.ArrayList SetMirrorNode(java.util.ArrayList servers) {
        if (!servers.isEmpty()) {
            com.alachisoft.tayzgrid.config.newdom.ServerNode sn = (com.alachisoft.tayzgrid.config.newdom.ServerNode) servers.get(0);
            if (sn.getIsActiveMirrorNode()) {
                sn.setIsActiveMirrorNode(false);
            }

            servers.set(0, sn);
        }
        return servers;
    }
}