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


import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.config.newdom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.newdom.ServerNode;
import com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.servicecontrol.CacheService;
import com.alachisoft.tayzgrid.tools.common.LogErrors;
import java.io.File;
import java.io.FileInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import tangible.DotNetToJavaStringHelper;
import tangible.RefObject;

public class ToolsRPCService {
    
    private static String _server;
    private static int _port;
    private static ICacheServer cacheServer;
    private static CacheService NCache;
    
    public static CacheRPCService GetRPCService() throws UnknownHostException
    {
        boolean loaded = ServicePropValues.loadServiceProp();
        if (loaded && (_server == null || _server.trim().isEmpty()) && ServicePropValues.BIND_ToCLUSTER_IP != null && !ServicePropValues.BIND_ToCLUSTER_IP.trim().isEmpty())
        {
            _server = ServicePropValues.BIND_ToCLUSTER_IP;
        }

        if (loaded && _port == 0 && ServicePropValues.CACHE_MANAGEMENT_PORT != null && !ServicePropValues.CACHE_MANAGEMENT_PORT.trim().isEmpty())
        {
            _port = Integer.parseInt(ServicePropValues.CACHE_MANAGEMENT_PORT);
        }
             return  new CacheRPCService(_server, _port);
    }
    
    public static String GetClientBindIP() throws UnknownHostException
    {
        boolean loaded = ServicePropValues.loadServiceProp();
        if (loaded && ServicePropValues.BIND_toClient_IP != null && !ServicePropValues.BIND_toClient_IP.trim().isEmpty())
        {           
            return ServicePropValues.BIND_toClient_IP;
        }
        else
        {
            return null;
        }
    }
      
    public static boolean DeployAssembly(String server, int port, String path, String cacheId, String depAsmPath, String userId, String password, LogErrors logError) throws ManagementException, Exception {
        ArrayList<File> files = new ArrayList<File>();
        String fileName = null;
        byte[] asmData;
        String failedNodes = "";
        CacheServerConfig serverConfig = null;
        NCache = ToolsRPCService.GetRPCService();
        try {
            if (port != -1) {
                NCache.setPort(port);
            }
            if (port == -1) {
                NCache.setPort(NCache.getUseTcp() ? CacheConfigManager.getTcpPort() : CacheConfigManager.getHttpPort());
            }
            if (server != null || !server.equals("")) {
                NCache.setServerName(server);
            }
            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
            if (cacheServer != null) {
                serverConfig = cacheServer.GetNewConfiguration(cacheId);
                if (path != null && !path.equals("")) {
                    String[] extension = path.split("\\.(?=[^\\.]+$)");
                    if (extension[extension.length - 1].equals("jar")) {
                        File fi = new File(path);
                        files.add(fi);
                    } else {
                        File di = new File(path);
                        try {
                            for (File f : di.listFiles()) {
                                String[] extension1 = f.getPath().split("\\.(?=[^\\.]+$)");
                                if (extension1[extension1.length - 1].equals("")) {
                                    files.add(f);
                                }
                            }
                        } catch (RuntimeException ex) {
                            logError.invoke("Directory " + di.getPath() + "could not be accessed!!!!");
                            return false; // We already got an error trying to access dir so dont try to access it again
                        }
                    }
                }
                if (depAsmPath != null && !depAsmPath.equals("")) {
                    String[] extension = depAsmPath.split("\\.(?=[^\\.]+$)");
                    if (extension[extension.length - 1].equals("jar")) {
                        File fi = new File(depAsmPath);
                        files.add(fi);
                    } else {
                        File di = new File(depAsmPath);
                        try {
                            for (File f : di.listFiles()) {
                                String[] extension1 = f.getPath().split("\\.(?=[^\\.]+$)");
                                if (extension1[extension1.length - 1].equals("jar")) {
                                    files.add(f);
                                }
                            }
                        } catch (RuntimeException ex) {
                            logError.invoke("Directory " + di.getPath() + "could not be accessed!!!!");
                            return false; // We already got an error trying to access dir so dont try to access it again
                        }
                    }
                }
                for (File f : files) {
                    try {
                        FileInputStream fs = new FileInputStream(f.getPath());
                        int size = (int) new File(f.getPath()).length();
                        asmData = new byte[size];
                        fs.read(asmData, 0, asmData.length);
                        fs.close();
                        fileName = (new File(f.getPath())).getName();
                        
                        if (serverConfig.getCacheSettings().getCacheType().equals("clustered-cache")) {
                            for (Address node : serverConfig.getCacheDeployment().getServers().GetAllConfiguredNodes()) {
                                NCache.setServerName(node.getIpAddress().getHostName().toString());
                                try {
                                    cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                                    cacheServer.CopyAssemblies(cacheId, fileName, asmData);
                                } catch (RuntimeException ex) {
                                    logError.invoke("Failed to Deploy Assembly on " + NCache.getServerName());
                                    logError.invoke("Error Detail: " + ex.getMessage());
                                }
                            }
                        } else {
                            cacheServer.CopyAssemblies(cacheId, fileName, asmData);
                        }
                    } catch (RuntimeException e) {
                        String message = String.format("Could not deploy assembly \"" + fileName + "\". %1$s", e.getMessage());
                        logError.invoke("Error : " + message);
                        return false;
                    }
                }
            }
        } catch (RuntimeException e) {
            logError.invoke("Error : {0}" + e.getMessage());
        } finally {
            NCache.dispose();
        }
        return true;
    }

    private static boolean IsValidAddress(String iPaddress) {
        try {
            InetAddress.getByName(iPaddress);
        } catch (UnknownHostException e) {
            return false;
        }
        return true;
    }

    private static HashMap BringLocalServerToFirstPriority(String localNode, java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer> serversPriorityList) throws Exception {
        NCache = ToolsRPCService.GetRPCService();
        HashMap<Integer, CacheServer> tempList = new HashMap<Integer, CacheServer>();
        int localServerPriority = 0;
        boolean localServerFound = false;
        NCache.setServerName(localNode);
        ICacheServer sw = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
        String nodeName = localNode;
        HashMap temp = sw.GetNodeInfo().getMap();
        String server = (String) ((temp.get(com.alachisoft.tayzgrid.management.CacheServer.Channel.SocketServer) instanceof String) ? temp.get(com.alachisoft.tayzgrid.management.CacheServer.Channel.SocketServer) : null);
        Inet4Address serverAddress = null;
        RefObject<Inet4Address> tempRef_serverAddress = new RefObject<Inet4Address>(serverAddress);
        boolean tempVar = IsValidAddress(server);
        serverAddress = tempRef_serverAddress.argvalue;
        if (tempVar) {
            nodeName = server;
        }
        Map<Integer, CacheServer> pair = (Map<Integer, CacheServer>) serversPriorityList;
        int i = 0;
        while (i < pair.size()) {
            String serverName = pair.get(i).getServerName().toLowerCase();
            if (serverName.compareTo(nodeName.toLowerCase()) == 0) {
                localServerFound = true;
                localServerPriority = i;
                break;
            }
            i++;
        }
        if (localServerFound) {
            tempList.put(0, serversPriorityList.get(localServerPriority));
            int priority = 1;
            Map<Integer, CacheServer> pair1 = serversPriorityList;
            for (int key : serversPriorityList.keySet()) {
                if (key != localServerPriority) {
                    tempList.put(priority++, pair1.get(key));
                }
            }
            serversPriorityList = tempList;
        }
        return serversPriorityList;
    }

    public static HashMap GetPrioritizedServerListForClient(String clientNode, String clusterId, java.util.ArrayList _nodeList) throws Exception {
        NCache = ToolsRPCService.GetRPCService();
        int priority = 0;
        String ClientServerName = "";
        HashMap<Integer, CacheServer> serversPriorityList = new HashMap<Integer, CacheServer>();
        for (Object temp : _nodeList) {
            ServerNode serverNode = (ServerNode) temp;
            CacheServer server = new CacheServer();
            try {
                NCache.setServerName(serverNode.getIP());
                ICacheServer _cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                HashMap bindedIps = _cacheServer.BindedIp().getMap();
                if (bindedIps.containsKey(com.alachisoft.tayzgrid.management.CacheServer.Channel.SocketServer)) {
                    ClientServerName = bindedIps.get(com.alachisoft.tayzgrid.management.CacheServer.Channel.SocketServer).toString();
                }
                if (!DotNetToJavaStringHelper.isNullOrEmpty(ClientServerName)) {
                    server.setServerName(ClientServerName);
                } else {
                    server.setServerName(serverNode.getIP());
                }
            } catch (RuntimeException ex) {
                ClientServerName = serverNode.getIP();
                server.setServerName(serverNode.getIP());
            }
            server.setPriority(priority);
            serversPriorityList.put(priority++, server);
        }
        serversPriorityList = (HashMap<Integer, CacheServer>) BringLocalServerToFirstPriority(clientNode, serversPriorityList);
        return serversPriorityList;
    }
}
