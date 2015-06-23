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

public final class RemoveBackingSourceTool {

    private static RemoveBackingSourceParam cParam = new RemoveBackingSourceParam();
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
        
        if ((!cParam.getIsReadThru()) && (!cParam.getIsWriteThru())) {
            System.err.println("Error: ReadThru/WriteThru not specified");
            return false;
        }
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getProviderName())) {
            System.err.println("Error: Providor Name not specified");
            return false;
        }
        
       

        return true;
    }

    ////<summary>
    ////Log an event in event viewer.
    ////</summary>
    private static void LogEvent(String msg) {
//		EventLogEntryType type = EventLogEntryType.Error;
////		using (EventLog ncLog = new EventLog("Application"))
//		EventLog ncLog = new EventLog("Application");
//		try
//		{
//			ncLog.Source = "NCache:RemoveBackingSource Tool";
//			ncLog.WriteEntry(msg, type);
//		}
//		finally
//		{
//			ncLog.dispose();
//		}
    }

    /**
     * The main entry point for the tool.
     */
    public static void Run(String[] args) throws Exception {
        //System.Reflection.Assembly asm = null;
        com.alachisoft.tayzgrid.config.newdom.Provider[] prov = null;
        String failedNodes = "";
        ICacheServer cacheServer;
        //NCache = new CacheRPCService("");
        //Alachisoft.NCache.Config.Dom.Parameter[] parameters=null;
        try {
            NCache = ToolsRPCService.GetRPCService();
            Object param = new RemoveBackingSourceParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (RemoveBackingSourceParam) param;
            boolean succsfull = true;
            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }
            if (!ValidateParameters()) {
                return;
            }
//            if (cParam.getPort() == -1) {
//                NCache.setPort(NCache.getUseTcp() ? CacheConfigManager.getTcpPort() : CacheConfigManager.getHttpPort());
//            }
            if (cParam.getServer() != null && !cParam.getServer().equals("")) {
                NCache.setServerName(cParam.getServer());
            }
            if (cParam.getPort() != -1) {
                NCache.setPort(cParam.getPort());
            }
            //IPAddress address;

            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));
            //if (!IPAddress.TryParse(cParam.Server, out address))
            //{
            //    if (m.GetBindIP() != null && m.GetBindIP() != string.Empty)
            //        cParam.Server = m.GetBindIP();
            //}
            //m = (ICacheServer)NCache.ConnectCacheServer();
            if (cacheServer != null) {
                if (cacheServer.IsRunning(cParam.getCacheId())) {
                    throw new RuntimeException(cParam.getCacheId() + " is Running on " + NCache.getServerName() + "Stop the cache first...");
                }

                com.alachisoft.tayzgrid.config.newdom.CacheServerConfig serverConfig = cacheServer.GetNewConfiguration(cParam.getCacheId());
                if (serverConfig == null) {
                    throw new RuntimeException("Specified cache is not registered on given server.");
                }

                //if (!cParam.Unregister)
                //{
                //try
                //{
                //    asm = System.Reflection.Assembly.LoadFrom(cParam.AsmPath);
                //}
                //catch (Exception e)
                //{
                //    string message = string.Format("Could not load assembly \"" + cParam.AsmPath + "\". {0}", e.Message);
                //    System.out.println("Error : {0}", message);
                //    return;
                //}

                //if (asm == null)
                //    throw new Exception("Could not load specified Assembly");


                //if (serverConfig.CacheSettings.BackingSource == null)
                //    serverConfig.CacheSettings.BackingSource = new Alachisoft.NCache.Config.Dom.BackingSource();

                ////System.Type type = asm.GetType(cParam.Class, true);

                //if (cParam.IsReadThru)
                //{
                //    System.Type typeProvider = type.GetInterface("IReadThruProvider");

                //    if (typeProvider == null)
                //    {
                //        System.out.println("Error: Specified class does not implement IReadThruProvider.");
                //        return;
                //    }
                //    else
                //    {
                //        if (serverConfig.CacheSettings.BackingSource.Readthru == null)
                //        {
                //            serverConfig.CacheSettings.BackingSource.Readthru = new Alachisoft.NCache.Config.Dom.Readthru();
                //            serverConfig.CacheSettings.BackingSource.Readthru.Providers = prov;
                //        }

                //        serverConfig.CacheSettings.BackingSource.Readthru.Enabled = true;
                //        prov = serverConfig.CacheSettings.BackingSource.Readthru.Providers;
                //        serverConfig.CacheSettings.BackingSource.Readthru.Providers = GetSourceProvider(GetProvider(prov, asm));
                //    }
                //}
                //else if (cParam.IsWriteThru)
                //{
                //    System.Type typeProvider = type.GetInterface("IWriteThruProvider");

                //    if (typeProvider != null)
                //    {
                //        System.out.println("Error: Specified class does not implement IWriteThruProvider.");
                //        return;
                //    }
                //    else
                //    {
                //        if (serverConfig.CacheSettings.BackingSource.Writethru == null)
                //        {
                //            serverConfig.CacheSettings.BackingSource.Writethru = new Alachisoft.NCache.Config.Dom.Writethru();
                //            serverConfig.CacheSettings.BackingSource.Writethru.Providers = prov;
                //        }

                //        serverConfig.CacheSettings.BackingSource.Writethru.Enabled = true;
                //        prov = serverConfig.CacheSettings.BackingSource.Writethru.Providers;
                //        serverConfig.CacheSettings.BackingSource.Writethru.Providers = GetSourceProvider(GetProvider(prov, asm));
                //    }
                //}
                // }
                //else
                //{
                if (serverConfig.getCacheSettings().getBackingSource() != null) {
                    if (cParam.getIsReadThru()) {
                        if (serverConfig.getCacheSettings().getBackingSource().getReadthru() != null) {
                            prov = serverConfig.getCacheSettings().getBackingSource().getReadthru().getProviders();
                        } else {
                            return;
                        }
                    } else if (cParam.getIsWriteThru()) {
                        if (serverConfig.getCacheSettings().getBackingSource().getWritethru() != null) {
                            prov = serverConfig.getCacheSettings().getBackingSource().getWritethru().getProviders();
                        } else {
                            return;
                        }
                    }

                    java.util.Hashtable hash = new java.util.Hashtable();

                    if (prov != null) {
                        hash = ProviderToHashtable(prov);
                        if (hash.containsKey(cParam.getProviderName())) {
                            hash.remove(cParam.getProviderName());
                        }
                    }

                    if (cParam.getIsReadThru()) {
                        if (hash.isEmpty()) {
                            serverConfig.getCacheSettings().getBackingSource().setReadthru(null);
                        } else {
                            serverConfig.getCacheSettings().getBackingSource().getReadthru().setProviders(GetSourceProvider(hash));
                        }

                    } else if (cParam.getIsWriteThru()) {
                        if (hash.isEmpty()) {
                            serverConfig.getCacheSettings().getBackingSource().setWritethru(null);
                        } else {
                            serverConfig.getCacheSettings().getBackingSource().getWritethru().setProviders(GetSourceProvider(hash));
                        }
                    }

                } else {
                    return;
                }
                //}

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
                            System.out.println("Backing Source Successfully Removed From " + NCache.getServerName() + " For " + cParam.getCacheId());

                        } catch (RuntimeException ex) {
                            System.err.println("Failed to Remove Backing Source on " + NCache.getServerName());
                            System.err.println("Error Detail: " + ex.getMessage());
                            failedNodes = failedNodes + "/n" + node.getIpAddress().toString();
                            LogEvent(ex.getMessage());
                            succsfull = false;
                        } finally {
                            cacheServer.dispose();
                        }
                    }
                } else {
                    try {
                        cacheServer.RegisterCache(cParam.getCacheId(), serverConfig, "", true, userId, paswd, true);
                        System.out.println("Backing Source Successfully Removed From " + NCache.getServerName() + " For " + cParam.getCacheId());

                    } catch (RuntimeException ex) {
                        System.err.println("Failed to Remove Backing Source on " + NCache.getServerName());
                        System.err.println("Error Detail: " + ex.getMessage());
                        LogEvent(ex.getMessage());
                        succsfull = false;
                    } finally {
                        cacheServer.dispose();
                    }
                }
            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e) {
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
            String[] str = st[i].split("=");//split(new char[]{'='}, 2);
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
            param[index].setParamValue((String) pParams.get(key));
            index++;
        }
        return param;
    }

    public static com.alachisoft.tayzgrid.config.newdom.Provider[] GetSourceProvider(java.util.Hashtable pParams) {
        com.alachisoft.tayzgrid.config.newdom.Provider[] param = new com.alachisoft.tayzgrid.config.newdom.Provider[pParams.size()];
        int index = 0;
        for (Object key : pParams.keySet()) {
            param[index] = new com.alachisoft.tayzgrid.config.newdom.Provider();
            param[index].setProviderName((String) key);
            param[index] = (com.alachisoft.tayzgrid.config.newdom.Provider) pParams.get(key);
            index++;
        }
        return param;
    }

    public static java.util.Hashtable ProviderToHashtable(com.alachisoft.tayzgrid.config.newdom.Provider[] prov) {
        java.util.Hashtable hash = new java.util.Hashtable();
        for (int i = 0; i < prov.length; i++) {
            hash.put(prov[i].getProviderName(), prov[i]);
        }
        return hash;
    }
}
