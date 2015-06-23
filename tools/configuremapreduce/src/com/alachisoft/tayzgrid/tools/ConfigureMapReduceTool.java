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
import com.alachisoft.tayzgrid.config.newdom.TaskConfiguration;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;

public final class ConfigureMapReduceTool {

    private static ConfigureMapReduceParam cParam = new ConfigureMapReduceParam();
    private static CacheRPCService NCache;
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
        if(cParam.getChunkSize() < 0)
        {
             System.err.println("Error: Chunk Size should be a positive value.");
             return false;
        }
        if(cParam.getMaxExceptions() < 0)
        {
             System.err.println("Error: Max Avoidable Exceptions should be a positive value.");
             return false;
        }
        if(cParam.getMaxTasks() < 0)
        {
            System.err.println("Error: Max Tasks should be a positive value.");
             return false;
        }
        if(cParam.getQueueSize() < 0)
        {
            System.err.println("Error: Queue Size should be a positive value.");
             return false;
        }
        return true;
    }



    /**
     * The main entry point for the tool.
     */
    public static void Run(String[] args) throws Exception {
       
        com.alachisoft.tayzgrid.config.newdom.Provider[] prov = null;
        String failedNodes = "";
        ICacheServer cacheServer;
       
        try {
            NCache = ToolsRPCService.GetRPCService();
            Object param = new ConfigureMapReduceParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (ConfigureMapReduceParam) param;
            boolean successfull = true;
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
                if (cacheServer.IsRunning(cParam.getCacheId())) {
                    throw new RuntimeException(cParam.getCacheId() + " is Running on " + NCache.getServerName() + "Stop the cache first...");
                }

                com.alachisoft.tayzgrid.config.newdom.CacheServerConfig serverConfig = cacheServer.GetNewConfiguration(cParam.getCacheId());
                if (serverConfig == null) {
                    throw new RuntimeException("Specified cache is not registered on given server.");
                }
                
                if(serverConfig.getCacheSettings().getCacheTopology().getTopology().equalsIgnoreCase("mirrored"))
                {throw new RuntimeException("MapReduce is not configurable for mirrored-cache topology.");}
                else if(serverConfig.getCacheSettings().getCacheTopology().getTopology().equalsIgnoreCase("replicated"))
                {throw new RuntimeException("MapReduce is not configurable for replicated-cache topology.");}
                else if(serverConfig.getCacheSettings().getCacheTopology().getTopology().equalsIgnoreCase("local-cache"))
                {throw new RuntimeException("MapReduce is not configurable for local-cache topology.");}
                else if(serverConfig.getCacheSettings().getCacheTopology().getTopology().equalsIgnoreCase("client-cache"))
                {throw new RuntimeException("MapReduce is not configurable for client-cache topology.");}
                
                TaskConfiguration ServerTaskConf = null;
                if (serverConfig.getCacheSettings().getTaskConfiguration() != null) 
                {
                    ServerTaskConf = serverConfig.getCacheSettings().getTaskConfiguration();
                    if(cParam.getChunkSize()!=0)
                    {ServerTaskConf.setChunkSize(cParam.getChunkSize());}
                    if(cParam.getMaxExceptions()!= 0)
                    {ServerTaskConf.setMaxExceptions(cParam.getMaxExceptions());}
                    if(cParam.getMaxTasks()!= 0)
                    {ServerTaskConf.setMaxTasks(cParam.getMaxTasks());}
                    if(cParam.getQueueSize()!=0)
                    {ServerTaskConf.setQueueSize(cParam.getQueueSize());}
                } 
                else 
                {
                    ServerTaskConf = new TaskConfiguration();
                    if(cParam.getChunkSize()!=0)
                    {ServerTaskConf.setChunkSize(cParam.getChunkSize());}
                    else
                    {ServerTaskConf.setChunkSize(100);}
                    if(cParam.getMaxExceptions()!= 0)
                    {ServerTaskConf.setMaxExceptions(cParam.getMaxExceptions());}
                    else
                    {ServerTaskConf.setMaxExceptions(10);}
                    if(cParam.getMaxTasks()!= 0)
                    {ServerTaskConf.setMaxTasks(cParam.getMaxTasks());}
                    else
                    {ServerTaskConf.setMaxTasks(10);}
                    if(cParam.getQueueSize()!=0)
                    {ServerTaskConf.setQueueSize(cParam.getQueueSize());}
                    else
                    {ServerTaskConf.setQueueSize(10);}
                }
                serverConfig.getCacheSettings().setTaskConfiguration(ServerTaskConf);

                byte[] userId = null;

                byte[] paswd = null;

               

                if (serverConfig.getCacheSettings().getCacheType().equals("clustered-cache")) 
                {
                    for (Address node : serverConfig.getCacheDeployment().getServers().GetAllConfiguredNodes()) {
                        NCache.setServerName(node.getIpAddress().getHostName().toString());
                        try {

                            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));

                            if (cacheServer.IsRunning(cParam.getCacheId())) {
                                throw new RuntimeException(cParam.getCacheId() + " is Running on " + NCache.getServerName() + "Stop the cache first...");
                            }

                            cacheServer.RegisterCache(cParam.getCacheId(), serverConfig, "", true, userId, paswd, cParam.getIsHotApply());
                            System.out.println("MapReduce Successfully Configured on " + NCache.getServerName() + " For " + cParam.getCacheId());

                        } catch (RuntimeException ex) {
                            System.err.println("Failed to Configure MapReduce on " + NCache.getServerName());
                            System.err.println("Error Detail: " + ex.getMessage());
                            failedNodes = failedNodes + "/n" + node.getIpAddress().toString();
                           
                            successfull = false;
                        } finally {
                            cacheServer.dispose();
                        }
                    }
                } 
                else 
                {
                    try 
                    {
                        cacheServer.RegisterCache(cParam.getCacheId(), serverConfig, "", true, userId, paswd, true);
                        System.out.println("MapReduce Successfully Configured on " + NCache.getServerName() + " For " + cParam.getCacheId());

                    } catch (RuntimeException ex) {
                        System.err.println("Failed to Configure MapReduce on " + NCache.getServerName());
                        System.err.println("Error Detail: " + ex.getMessage());
                       
                        successfull = false;
                    } finally {
                        cacheServer.dispose();
                    }
                }
            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } 
        catch (Exception e) 
        {
            System.err.println("Error : " + e.getMessage());
            
        } 
        finally 
        {
            NCache.dispose();
        }
    }
}
