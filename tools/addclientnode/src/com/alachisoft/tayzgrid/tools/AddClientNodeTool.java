
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
import com.alachisoft.tayzgrid.common.enums.*;
import com.alachisoft.tayzgrid.common.exceptions.ManagementException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.monitoring.EventLogEntryType;
import com.alachisoft.tayzgrid.config.newdom.*;
import com.alachisoft.tayzgrid.management.clientconfiguration.dom.*;
import com.alachisoft.tayzgrid.common.net.*;
import com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.InetAddress;

public final class AddClientNodeTool {

    private static CacheRPCService NCache;
    private static java.util.ArrayList<String> newClientNodes = new java.util.ArrayList<String>();
    private static java.util.ArrayList<String> currentServerNodes = new java.util.ArrayList<String>();
    private static java.util.ArrayList<String> currentClientNodes = new java.util.ArrayList<String>();
    private static com.alachisoft.tayzgrid.config.newdom.CacheServerConfig config;
    private static ClientConfiguration clientConfig;
    private static com.alachisoft.tayzgrid.management.ICacheServer cacheServer;
    private static AddClientNodeParam cParam = new AddClientNodeParam();
    

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

    private static boolean ValidateParameters() {
        // Validating CacheId
        AssemblyUsage.PrintLogo(cParam.getIsLogo());
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getCacheId())) {
            System.err.println("Error: Cache Name not specified");
            return false;
        }

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getServer())) {
            System.err.println("Error: Server not specified");
            return false;
        }
        if (!IsValidIP(cParam.getServer()))
        {
           System.err.println("Error: Invalid Server IP");
           return false;
        }

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getClientNode())) {
            System.err.println("Error: Client Node not specified");
            return false;
        }
        
        if (!IsValidIP(cParam.getClientNode()))
        {
           System.err.println("Error: ClientNode(IP) is invalid");
           return false;
        }
        

        return true;
    }

    private static void LogEvent(String msg) {
        EventLogEntryType type = EventLogEntryType.Error;
        try {
        } finally {
        }
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
    /**
     * The main entry point for the tool.
     */
    public static void Run(String[] args) throws UnknownHostException, ManagementException, IllegalArgumentException, SecurityException, IllegalAccessException, ParserConfigurationException, InstantiationException, InstantiationException, IOException, IOException, SAXException, Exception {
        
        try {
            NCache = ToolsRPCService.GetRPCService();
            AddClientNodeParam param = new AddClientNodeParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = (AddClientNodeParam) tempRef_param.argvalue;
            cParam = (AddClientNodeParam) param;
            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }

            if (!ValidateParameters()) {
                return;
            }
            

            
            
            try{
                if (IsValidAddress(cParam.getClientNode())) {
                    NCache.setServerName(cParam.getClientNode());
                    //ICacheServer temp = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                }
            }
            catch(RuntimeException exp)
            {
                System.err.println("Failed to Connect to IP " + cParam.getClientNode() + ".");
                System.err.println("Error Detail: " + exp.getMessage());
                return;
            }
            
            if (cParam.getPort() != -1) {
                NCache.setPort(cParam.getPort());
            }
            if (cParam.getServer() != null && !cParam.getServer().equals("")) {
                NCache.setServerName(cParam.getServer());
            }
            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
            
            config = cacheServer.GetNewConfiguration(cParam.getCacheId());
            if (config == null) {
                System.err.println("Error : The cache doesnot exist");
                return;
            }
             if (config.getCacheSettings().getCacheType().equals("clustered-cache") && !config.getCacheSettings().getInProc()) {
                for (Address node : config.getCacheDeployment().getServers().GetAllConfiguredNodes()) {
                    currentServerNodes.add(node.getIpAddress().getHostAddress().toString());
                }
            } else {
                System.err.println("Error: Client nodes cannot be added to local/Inproc caches");
                return;
            }
            if (config.getCacheDeployment().getClientNodes() != null) {

                for (ClientNode clientNode : config.getCacheDeployment().getClientNodes().getNodesList()) {
                    if (cParam.getClientNode().equals(clientNode.getName())) {

                        System.err.println("Error: " + clientNode.getName() + " already part of \"" + cParam.getCacheId() + "\"");
                        return;
                    }
                    currentClientNodes.add(clientNode.getName());
                }
            }

            UpdateConfigs();


        } catch (Exception ex) {
            if(ex.getMessage()!=null)
            System.err.println("Error: " + ex.getMessage());
        }

    }

    private static void UpdateConfigs() throws ManagementException, IllegalArgumentException, IllegalAccessException, SecurityException, ParserConfigurationException, InstantiationException, IOException, SAXException, TimeoutException, Exception {
        try {
            boolean isSuccessFull=false;
            if (config.getCacheDeployment().getClientNodes() == null) {
                config.getCacheDeployment().setClientNodes(new ClientNodes());
            }

            if (config.getCacheDeployment().getClientNodes().getNodesList() == null) {
                config.getCacheDeployment().getClientNodes().setNodesList(new java.util.ArrayList<ClientNode>());
            }

            String runtimeContext = "0";
            //No client cache will be registered in case of adding a client node.[by Sir Iqbal].

            ClientNode clientNod = new ClientNode();
            clientNod.setName(cParam.getClientNode());
            clientNod.setClientRuntimeContext(RtContextValue.JVCACHE);
            config.getCacheDeployment().getClientNodes().getNodesList().add(clientNod);
            //}

            for (String node : currentServerNodes) {
                NCache.setServerName(node);
                cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                
                cacheServer.ApplyCacheConfiguration(cParam.getCacheId(), config, null, null, false);
                }

            String oldClientNode = null;
            ClientConfiguration clientConfig = null;
            if (currentClientNodes.size() > 0) {
                oldClientNode = currentClientNodes.get(0);
                NCache.setServerName(oldClientNode);
                cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                clientConfig = cacheServer.GetClientConfiguration(cParam.getCacheId());
            }

            NCache.setServerName(cParam.getClientNode()); 
            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
            String xml = "";
            cacheServer.UpdateClientServersList(cParam.getCacheId(), GetSeversPriorityList(),runtimeContext, config.getCacheSettings().getClientPort());
            ClientConfiguration clientConfiguration = cacheServer.GetClientConfiguration(cParam.getCacheId());
            CacheConfiguration cacheConfig = new CacheConfiguration();
            cacheConfig = clientConfiguration.getCacheConfigurationsMap().get(cParam.getCacheId());
            cacheConfig.setCacheId(cParam.getCacheId());
            if (cParam.getClientBindingIp() != null && !cParam.getClientBindingIp().equals("")) {
                cacheConfig.setBindIp(cParam.getClientBindingIp());
            } else {
                cacheConfig.setBindIp(cParam.getClientNode());
            }

            cacheConfig.setClientPort(config.getCacheSettings().getClientPort());
            
            clientConfiguration.getCacheConfigurationsMap().remove(cParam.getCacheId());
            clientConfiguration.getCacheConfigurationsMap().put(cParam.getCacheId(), cacheConfig);

            if (config.getCacheDeployment().getServers().getNodeIdentities() != null && config.getCacheDeployment().getServers().getNodeIdentities().length != 0) {
                cacheConfig.setServersPriorityList(new CacheServerList());

                for (NodeIdentity identity : config.getCacheDeployment().getServers().getNodeIdentities()) {
                    com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer server = new com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer();
                    server.setServerName(identity.getNodeName());
                    server.setPriority(identity.getNodePriority());
                    cacheConfig.getServersPriorityList().setServersList(identity.getNodePriority() - 1, server);
                }
            }

            cacheServer.UpdateClientConfiguration(cParam.getCacheId(), clientConfiguration);
            isSuccessFull=true;
            if(isSuccessFull)
            {
                System.out.println("SuccessFully added client node");
            }

        } catch (Exception ex) {
            if(ex.getMessage()!=null)
            System.err.println("Error: " + ex.getMessage());
        }
    }

    private static com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList GetSeversPriorityList() throws UnknownHostException {

        java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer> serversPriorityList = new java.util.HashMap<Integer, com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer>();

        java.util.ArrayList<Address> hosts = config.getCacheDeployment().getServers().GetAllConfiguredNodes();


        int priority = 0;
        for (Address addr : hosts) {
            com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer server = new com.alachisoft.tayzgrid.management.clientconfiguration.dom.CacheServer();
            server.setServerName(addr.getIpAddress().getHostAddress());
            server.setPriority(priority);

            serversPriorityList.put(priority, server);
            priority++;
        }
        com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList csList = new com.alachisoft.tayzgrid.management.clientconfiguration.CacheServerList();
        csList.setServersList(serversPriorityList);
        return csList;

    }
}
