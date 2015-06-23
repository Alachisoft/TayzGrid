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

public final class RemovedStartupLoaderTool {

    private static RemovedStartupLoaderParam cParam = new RemovedStartupLoaderParam();
    private static CacheRPCService NCache;

    /**
     * Validate all parameters in property string.
     */
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

    /**
     * The main entry point for the tool.
     */
    public static void Run(String[] args) throws UnknownHostException, Exception {

        com.alachisoft.tayzgrid.config.newdom.Parameter[] pr = new com.alachisoft.tayzgrid.config.newdom.Parameter[20];
        String failedNodes = "";
        try {
            NCache = ToolsRPCService.GetRPCService();

            Object param = new RemovedStartupLoaderParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (RemovedStartupLoaderParam) param;
            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }
            if (!ValidateParameters()) {
                return;
            }
            if (cParam.getServer() != null && !cParam.getServer().equals("")) {
                NCache.setServerName(cParam.getServer());
            }
            if (cParam.getPort() != -1) {
                NCache.setPort(cParam.getPort());
            }

            ICacheServer cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
            if (cacheServer != null) {
                if (cacheServer.IsRunning(cParam.getCacheId())) {
                    throw new RuntimeException(cParam.getCacheId() + " is Running on " + NCache.getServerName() + " Stop the cache first...");
                }

                com.alachisoft.tayzgrid.config.newdom.CacheServerConfig serverConfig = cacheServer.GetNewConfiguration(cParam.getCacheId());
                if (serverConfig == null) {
                    throw new RuntimeException("Specified cache is not registered on given server.");
                }
                if (serverConfig.getCacheSettings().getCacheLoader() != null) {

                    if (serverConfig.getCacheSettings().getCacheLoader().getProvider() != null) {
                        serverConfig.getCacheSettings().setCacheLoader(null);
                    } else {
                        return;
                    }
                } else {
                    throw new RuntimeException("No Startup Loader was registered.");
                }

                byte[] userId = null;
                byte[] paswd = null;
                    
                if (serverConfig.getCacheSettings().getCacheType().equals("clustered-cache")) {
                    for (Address node : serverConfig.getCacheDeployment().getServers().GetAllConfiguredNodes()) {
                        NCache.setServerName(node.getIpAddress().getHostName().toString());
                        try {
                            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));

                            if (cacheServer.IsRunning(cParam.getCacheId())) {
                                throw new RuntimeException(cParam.getCacheId() + " is Running on " + NCache.getServerName() + " Stop the cache first...");
                            }

                            cacheServer.RegisterCache(cParam.getCacheId(), serverConfig, "", true, userId, paswd, cParam.getIsHotApply());
                            System.out.println("Startup Loader on " + NCache.getServerName() + " For " + cParam.getCacheId() + " Successfully Removed");
                        } catch (RuntimeException ex) {
                            System.err.println("Failed to Remove Startup Loader on " + NCache.getServerName());
                            
                            if(ex.getMessage()!= null)
                                System.err.println("Error Detail: " + ex.getMessage());
                            failedNodes = failedNodes + "/n" + node.getIpAddress().toString();
                            LogEvent(ex.getMessage());
                        } finally {
                            cacheServer.dispose();
                        }
                    }
                } else {
                    try {
                        cacheServer.RegisterCache(cParam.getCacheId(), serverConfig, "", true, userId, paswd, false);
                    } catch (RuntimeException ex) {
                        System.err.println("Failed to Remove Startup Loader on " + NCache.getServerName());
                        if(ex.getMessage()!= null)
                            System.err.println("Error Detail: " + ex.getMessage());
                        LogEvent(ex.getMessage());
                    } finally {
                        cacheServer.dispose();
                    }
                }
            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e) {
            if(e.getMessage()!= null)
                System.err.println("Error : " + e.getMessage());
            LogEvent(e.getMessage());
        } finally {

            NCache.dispose();
        }
    }

    public static java.util.Hashtable GetParams(String param) {
        java.util.Hashtable hash = new java.util.Hashtable();
        String[] st = param.split("[$]", -1);
        for (int i = 0; i < st.length; i++) {
            String[] str = st[i].split("=");
            hash.put(str[0], str[1]);
        }
        return hash;
    }

    public static com.alachisoft.tayzgrid.config.newdom.Parameter[] GetProviderParams(java.util.Hashtable pParams) {
        com.alachisoft.tayzgrid.config.newdom.Parameter[] param = new com.alachisoft.tayzgrid.config.newdom.Parameter[pParams.size()];
        int index = 0;
        for (Object key : pParams.keySet()) {
            param[index] = new com.alachisoft.tayzgrid.config.newdom.Parameter();
            param[index].setName((String) key);
            param[index] = (com.alachisoft.tayzgrid.config.newdom.Parameter) pParams.get(key);
            index++;
        }
        return param;
    }
}
