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
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;

public final class DeployAssemblyTool {

    private static DeployAssemblyParam cParam = new DeployAssemblyParam();
    private static CacheRPCService NCache;
    private static ICacheServer cacheServer;
    private static com.alachisoft.tayzgrid.config.newdom.CacheServerConfig serverConfig = null;

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
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getPath())) {
            System.err.println("Error: Assembly Path not specified");
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
        java.util.ArrayList<java.io.File> files = new java.util.ArrayList<java.io.File>(); // List that will hold the files and subfiles in path
        String fileName = null;
        byte[] asmData;
        String failedNodes = "";

        try {
            boolean succsfull = true;
            NCache = ToolsRPCService.GetRPCService();

            Object param = new DeployAssemblyParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (DeployAssemblyParam) param;
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

            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));

            if (cacheServer != null) {
                serverConfig = cacheServer.GetNewConfiguration(cParam.getCacheId());

                if (serverConfig == null) {
                    throw new RuntimeException("Specified cache is not registered on given server.");
                }
                java.io.File temp = new java.io.File(cParam.getPath());

                if (temp.isFile()) {
                    java.io.File fi = new java.io.File(cParam.getPath());
                    files.add(fi);
                } else {
                    java.io.File di = new java.io.File(cParam.getPath());

                    try {
                        for (java.io.File f : di.listFiles()) {

                            if (new java.io.File(f.getPath()).isFile()) {
                                files.add(f);
                            }
                        }
                    } catch (RuntimeException ex) {
                        System.err.println("Directory " + di.getPath() + "  \n could not be accessed!!!!");
                        LogEvent(ex.getMessage());
                        return; // We already got an error trying to access dir so dont try to access it again
                    }

                }

                for (java.io.File f : files) {
                    try {
                        java.io.FileInputStream fs = new java.io.FileInputStream(f.getPath());// FileMode.Open, FileAccess.Read);
                        asmData = new byte[fs.available()];
                        fs.read(asmData);
                        fs.close();
                        fileName = (new java.io.File(f.getPath())).getName();
                        
                        if (serverConfig.getCacheSettings().getCacheType().equals("clustered-cache")) {
                            for (Address node : serverConfig.getCacheDeployment().getServers().GetAllConfiguredNodes()) {
                                NCache.setServerName(node.getIpAddress().getHostName().toString());
                                try {
                                    cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
                                    cacheServer.CopyAssemblies(cParam.getCacheId(), fileName, asmData);
                                    System.out.println("Jar Successfully Deployed on " + NCache.getServerName());

                                } catch (RuntimeException ex) {
                                    System.err.println("Failed to Deploy Jar on " + NCache.getServerName());
                                    System.err.println("Error Detail: " + ex.getMessage());
                                    LogEvent(ex.getMessage());
                                }
                            }
                        } else {
                            cacheServer.CopyAssemblies(cParam.getCacheId(), fileName, asmData);
                            System.out.println("Jar Successfully Deployed..." );
                        }
                    } catch (RuntimeException e) {
                        String message = String.format("Could not deploy Jar \"" + fileName + "\". %1$s", e.getMessage());
                        
                        if(e.getMessage()!=null)
                            System.err.println("Error : " + message);
                        LogEvent(e.getMessage());
                        return;
                    }
                }
            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e) {
            if(e.getMessage()!=null)
            System.err.println("Error :" + e.getMessage());
            LogEvent(e.getMessage());
        } finally {
            NCache.dispose();
        }

    }
}
