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
import com.alachisoft.tayzgrid.common.net.*;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import java.net.UnknownHostException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;

public final class RemoveCacheTool {

    /**
     * NCache service controller.
     */
    private static CacheRPCService NCache;
    /**
     * Cache ids specified at the command line.
     */
    private static String _userId = "";
    private static String _password = "";
    private static ICacheServer cacheServer;
    private static String clusterId = "";
    private static RemoveCacheParam cParam = new RemoveCacheParam();

    /**
     * Log an event in event viewer.
     */
    private static void LogEvent(String msg) {
    }

    private static boolean ValidateParameters() {
        // Validating CacheId

        AssemblyUsage.PrintLogo(cParam.getIsLogo());

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getCacheId())) {
            System.err.println("Error: Cache Name not specified");
            return false;
        }

        return true;
    }


    public static void Run(String[] args) throws UnknownHostException, Exception {
        try {
            NCache = ToolsRPCService.GetRPCService();

            Object param = new RemoveCacheParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (RemoveCacheParam) param;
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

            if (cacheServer != null) {
                try {
                    com.alachisoft.tayzgrid.config.newdom.CacheServerConfig serverConfig = cacheServer.GetNewConfiguration(cParam.getCacheId());

                    if (serverConfig == null) {
                        throw new RuntimeException("Specified cache does not exist");
                    }

                    byte[] userId = null;
                    byte[] paswd = null;    
                
                    if (serverConfig.getCacheSettings().getCacheType().equals("clustered-cache")) {
                        
                        for (Address node : serverConfig.getCacheDeployment().getServers().GetAllConfiguredNodes()) {
                            try {
                                NCache.setServerName(node.getIpAddress().getHostAddress().toString());
                                cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                                cacheServer.UnregisterCache(cParam.getCacheId(), null, userId, paswd, false, false);
                                System.out.println(cParam.getCacheId() + " is removed Successfully From " + NCache.getServerName());
                            } catch (RuntimeException ex) {
                                System.err.println("Failed to Rmove Cache on " + NCache.getServerName());
                                if (ex.getMessage() != null) {
                                    System.err.println("Error Detail: " + ex.getMessage());
                                }

                                LogEvent(ex.getMessage());
                            } finally {
                                cacheServer.dispose();
                            }
                        }
                    } else {
                        try {
                            cacheServer.UnregisterCache(cParam.getCacheId(), null, userId, paswd, false, false);
                            System.out.println(cParam.getCacheId() + " is removed Successfully From " + NCache.getServerName());
                        } catch (RuntimeException ex) {
                            System.err.println("Failed to Rmove Cache on " + NCache.getServerName());
                            if (ex.getMessage() != null) {
                                System.err.println("Error Detail: " + ex.getMessage());
                            }
                        }
                    }
                } 
                catch (com.alachisoft.tayzgrid.runtime.exceptions.SecurityException se) {
                    System.err.println("Failed to Remove " + cParam.getCacheId() + ". Security Exception: One or more given parameters/user rights are not valid.");
                    LogEvent(se.getMessage());
                } 
                catch (RuntimeException ex) {
                    System.err.println("Failed to Remove " + cParam.getCacheId());
                    if (ex.getMessage() != null) {
                        System.err.println(ex.getMessage());
                    }
                    LogEvent(ex.getMessage());
                }
            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e) {
            if (e.getMessage() != null) {
                System.err.println("Error :" + e.getMessage());
            }
        } finally {
            NCache.dispose();
        }
    }
}
