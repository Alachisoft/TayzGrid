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
import com.alachisoft.tayzgrid.config.newdom.Parameter;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.tools.common.LogErrors;

import com.alachisoft.tayzgrid.management.ToolsRPCService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.utility.jarscanner.JARReflector;

public final class ConfigureStartupLoaderTool {

    private static ConfigureStartupLoaderParam cParam = new ConfigureStartupLoaderParam();
    private static CacheRPCService NCache;
  
    private static LogErrors logErr; 

    /**
     * Validate all parameters in property string.
     */
    private static boolean ValidateParameters() {
        AssemblyUsage.PrintLogo(cParam.getIsLogo());
        // Validating CacheId
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getCacheId())) {
            System.err.println("Error: Cache Name not specified");
            return false;
        }
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.GetClass())) {
            System.err.println("Error: Class Name not specified");
            return false;
        }
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getAsmPath())) {
            System.err.println("Error: Assembly Path not specified");
            return false;
        }
        return true;
    }

    private static void LogEvent(String msg) {
    }

    private static void PrintMessage(String msg) {
        System.out.println(msg);
    }

    /**
     * The main entry point for the tool.
     */
    public static void Run(String[] args) throws Exception {
        JARReflector jarRef = null;
        String[] strProviderClasses = null;
//	
        com.alachisoft.tayzgrid.config.newdom.Parameter[] pr = new com.alachisoft.tayzgrid.config.newdom.Parameter[20];
        String failedNodes = "";
        try {
            NCache = ToolsRPCService.GetRPCService();

            Object param = new ConfigureStartupLoaderParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (ConfigureStartupLoaderParam) param;
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
                    throw new RuntimeException(cParam.getCacheId() + " is Running on " + NCache.getServerName() + "Stop the cache first...");
                }

                com.alachisoft.tayzgrid.config.newdom.CacheServerConfig serverConfig = cacheServer.GetNewConfiguration(cParam.getCacheId());
                if (serverConfig == null) {
                    throw new RuntimeException("Specified cache is not registered on given server.");
                }

                try {
                    jarRef = new JARReflector();
                } catch (Exception e) {
                    String message = String.format("Could not load Jar \"" + cParam.getAsmPath() + "\". %1$s", e.getMessage());
                    if(e.getMessage()!=null)
                        System.err.println("Error : " + message);
                    LogEvent(e.getMessage());
                    return;
                }

                try {
                    strProviderClasses = jarRef.getClassNames(cParam.getAsmPath(), "com.alachisoft.tayzgrid.runtime.cacheloader.CacheLoader");
                    if (!(strProviderClasses.length > 0 && IsValidProviderClass(strProviderClasses, cParam.GetClass()))) {
                        System.err.println("Error: Specified class does not implement ICacheStartupProvider/ICacheLoader.");
                        return;

                    }
                } catch (Exception e) {
                    String message = String.format("Could not load assembly \"" + cParam.getAsmPath() + "\". %1$s", e.getMessage());
                    if(e.getMessage()!=null)
                    System.err.println("Error : " + message);
                    LogEvent(e.getMessage());
                    return;
                }

                if (serverConfig.getCacheSettings().getCacheLoader() != null) {
                    if (cParam.GetClass().equals(serverConfig.getCacheSettings().getCacheLoader().getProvider().getClassName())) {
                        throw new RuntimeException("Startup Loader with same class is already registered.");
                    }

                }

                if (serverConfig.getCacheSettings().getCacheLoader() == null) {
                    serverConfig.getCacheSettings().setCacheLoader(new com.alachisoft.tayzgrid.config.newdom.CacheLoader());
                    serverConfig.getCacheSettings().getCacheLoader().setProvider(new com.alachisoft.tayzgrid.config.newdom.ProviderAssembly());
                }


                serverConfig.getCacheSettings().getCacheLoader().setEnabled(true);
                serverConfig.getCacheSettings().getCacheLoader().setRetries(cParam.getRetries());
                serverConfig.getCacheSettings().getCacheLoader().setRetryInterval(cParam.getRetryInterval());
                String asmName = cParam.getAsmPath().split("\\.")[cParam.getAsmPath().split("\\.").length - 1];
                serverConfig.getCacheSettings().getCacheLoader().getProvider().setAssemblyName(cParam.getAsmPath());
                serverConfig.getCacheSettings().getCacheLoader().getProvider().setClassName(cParam.GetClass());
                serverConfig.getCacheSettings().getCacheLoader().getProvider().setFullProviderName(cParam.getAsmPath());
                if (cParam.getParameters() != null && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getParameters())) {
                    serverConfig.getCacheSettings().getCacheLoader().setParameters(GetProviderParams(GetParams(cParam.getParameters())));
                }
                byte[] userId = null;
                byte[] paswd = null;
                if (serverConfig.getCacheSettings().getCacheType().equals("clustered-cache")) {
                    for (Address node : serverConfig.getCacheDeployment().getServers().GetAllConfiguredNodes()) {
                        NCache.setServerName(node.getIpAddress().getHostName().toString());
                        try {
                            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));

                            if (cacheServer.IsRunning(cParam.getCacheId())) {
                                throw new RuntimeException(cParam.getCacheId() + " is Running on " + NCache.getServerName() + "Stop the cache first...");
                            }

                            cacheServer.RegisterCache(cParam.getCacheId(), serverConfig, "", true, userId, paswd, cParam.getIsHotApply());
                            System.out.println("Startup Loader is configured Successfully on " + NCache.getServerName() + " For " + cParam.getCacheId());

                        } catch (Exception ex) {
                            System.err.println("Failed to Add Startup Loader on " + NCache.getServerName());
                            if(ex.getMessage()!=null)
                                System.err.println("Error Detail: " + ex.getMessage());
                            failedNodes = failedNodes + "/n" + node.getIpAddress().toString();
                            LogEvent(ex.getMessage());
                        } finally {
                            cacheServer.dispose();
                        }
                    }
                } else {
                    try {
                        cacheServer.RegisterCache(cParam.getCacheId(), serverConfig, "", true, userId, paswd, cParam.getIsHotApply());
                        System.out.println("Startup Loader is configured Successfully on " + NCache.getServerName() + " For " + cParam.getCacheId());

                    } catch (Exception ex) {
                        System.err.println("Failed to Add Startup Loader on " + NCache.getServerName());
                        if(ex.getMessage()!=null)
                        System.err.println("Error Detail: " + ex.getMessage());
                        LogEvent(ex.getMessage());
                    } finally {
                        cacheServer.dispose();
                    }
                }

                if (!cParam.getNoDeploy()) {
                    ToolsRPCService.DeployAssembly(cParam.getServer(), cParam.getPort(), cParam.getAsmPath(), cParam.getCacheId(), cParam.getDepAsmPath(), "", "", logErr);
                }

            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e) {
            if(e.getMessage()!=null)
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

    public static Parameter[] GetProviderParams(java.util.Hashtable pParams) {
        Parameter[] param = new Parameter[pParams.size()];
        int index = 0;

        for (Object key : pParams.keySet()) {
            param[index] = new Parameter();
            param[index].setName((String) key);
            param[index].setParamValue((String) pParams.get(key));
            index++;
        }
        return param;
    }

    public static boolean IsValidProviderClass(String[] Classes, String ProviderClass) {
        for (int index = 0; index < Classes.length; index++) {
            if (Classes[index].equals(ProviderClass)) {
                return true;
            }
        }
        return false;
    }
}
