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
import com.alachisoft.tayzgrid.management.clientconfiguration.dom.*;
import com.alachisoft.tayzgrid.common.net.*;
import com.alachisoft.tayzgrid.common.*;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.UnknownHostException;
import java.util.Iterator;

public final class RemoveClientNodeTool {

    private static CacheRPCService NCache;
    
    private static java.util.ArrayList currentServerNodes = new java.util.ArrayList();
    private static java.util.ArrayList currentClientNodes = new java.util.ArrayList();
    private static com.alachisoft.tayzgrid.config.newdom.CacheServerConfig config;
    private static com.alachisoft.tayzgrid.management.ICacheServer cacheServer;
    private static RemoveClientNodeParam cParam = new RemoveClientNodeParam();

    private static boolean ValidateParameters() {
        // Validating CacheId

        AssemblyUsage.PrintLogo(cParam.getIsLogo());

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getCacheId())) {
            System.err.println("Error: Cache Name not specified");
            return false;
        }

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getClientNode())) {
            System.err.println("Error: Client Node not specified");
            return false;
        }

        return true;
    }

    private static void LogEvent(String msg) {
    }

    public static void Run(String[] args) throws Exception {

        try {
            NCache = ToolsRPCService.GetRPCService();

            Object param = new RemoveClientNodeParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (RemoveClientNodeParam) param;
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
            }

            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
            
            if(cacheServer == null)
            {throw new Exception("TayzGrid service can not be contacted");}
            config = cacheServer.GetNewConfiguration(cParam.getCacheId());
            if (config == null) {
                System.err.println("Error : The cache doesnot exist");
                return;
            }
            if (config.getCacheSettings().getCacheType().equals("clustered-cache")) {
                for (Address node : config.getCacheDeployment().getServers().GetAllConfiguredNodes()) {
                    currentServerNodes.add(node.getIpAddress().getHostName().toString());
                }
            } else {
                System.err.println("Error: Client nodes cannot be added to local caches");
                return;
            }

            if (UpdateConfigs()) {
                System.out.println("Info: Client node(s) successfully removed...");
            }
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        } finally {
            if (cacheServer != null) {
                cacheServer.dispose();
            }
        }

    }

    private static boolean UpdateConfigs() throws UnknownHostException, Exception {
        try {
            boolean successfullremove = false;
            if (config.getCacheDeployment().getClientNodes() == null) {
                System.out.println("Error : Client node(s) not found.");
                return false;
            }

            if (config.getCacheDeployment().getClientNodes().getNodesList() == null) {
                System.out.println("Error : Client node(s) not found.");
                return false;
            }

            com.alachisoft.tayzgrid.config.newdom.ClientNode[] existingClientNodes = config.getCacheDeployment().getClientNodes().getNodes();
            for (com.alachisoft.tayzgrid.config.newdom.ClientNode cNode : existingClientNodes) {
                if (cParam.getClientNode().toString().equals(cNode.getName())) {
                    config.getCacheDeployment().getClientNodes().getNodesList().remove(cNode);
                    successfullremove = true;
                }
            }

            if (!successfullremove) {
                System.out.println("Error : Client node(s) not found.");
                return false;
            }

            byte[] userId = null;
            byte[] paswd = null;

            for (Iterator it = currentServerNodes.iterator(); it.hasNext();) {
                String node = (String) it.next();
                NCache.setServerName(node);
                cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                cacheServer.RegisterCache(cParam.getCacheId(), config, "", true, userId, paswd, true);
            }

            NCache.setServerName(cParam.getClientNode());
            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
            ClientConfiguration clientConfiguration = cacheServer.GetClientConfiguration(cParam.getCacheId());
            CacheConfiguration cacheConfiguration = clientConfiguration.getCacheConfigurationsMap().get(cParam.getCacheId());
            cacheServer.RemoveCacheFromClientConfig(cParam.getCacheId());

            return true;

        } catch (RuntimeException ex) {
            System.out.println("Error: " + ex.getMessage());
            return false;
        }
    }
}
