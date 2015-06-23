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

import com.alachisoft.tayzgrid.common.EncryptionUtil;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.config.newdom.BatchConfig;
import com.alachisoft.tayzgrid.config.newdom.Parameter;
import com.alachisoft.tayzgrid.config.newdom.Provider;
import com.alachisoft.tayzgrid.management.ICacheServer;
import com.alachisoft.tayzgrid.management.CacheRPCService;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.tools.common.LogErrors;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.utility.jarscanner.JARReflector;
import java.io.File;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class ConfigureBackingSourceTool {

    private static ConfigureBackingSourceParam cParam = new ConfigureBackingSourceParam();
    private static CacheRPCService NCache;
    
    private static LogErrors logErr;

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

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getAsmPath())) {
            System.err.println("Error: Assembly Path not specified");
            return false;
        }
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.GetClass().toString())) {

            System.err.println("Error: Class Name not specified");
            return false;
        }

        if ((!cParam.getIsReadThru()) && (!cParam.getIsWriteThru())) {
            System.err.println("Error: ReadThru/WriteThru not specified");
            return false;
        }
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getProviderName())) {
            System.err.println("Error: Provider Name not specified");
            return false;
        }

        return true;
    }

    ////<summary>
    ////Log an event in event viewer.
    ////</summary>
    private static void LogEvent(String msg) {
    }

    private static void PrintMessage(String msg) {
        System.out.println(msg);
    }

    /**
     * The main entry point for the tool.
     */
    public static void Run(String[] args) throws UnknownHostException, Exception {
        JARReflector jarRef = null;
        String[] strProviderClasses = null;
        com.alachisoft.tayzgrid.config.newdom.Provider[] prov = null;
        String failedNodes = "";
        ICacheServer cacheServer;
        boolean successFull = true;
        boolean isValid = false;
        try {
            NCache = ToolsRPCService.GetRPCService();
            Object param = new ConfigureBackingSourceParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (ConfigureBackingSourceParam) param;

            if (cParam.getIsUsage()) {
                successFull = false;
                AssemblyUsage.PrintUsage();
                return;
            }
            if (!ValidateParameters()) {
                successFull = false;
                AssemblyUsage.PrintLogo(cParam.getIsLogo());
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
                if (cacheServer.IsRunning(cParam.getCacheId())) {
                    successFull = false;
                    throw new RuntimeException(cParam.getCacheId() + " is Running on " + NCache.getServerName() + "Stop the cache first...");
                }

                com.alachisoft.tayzgrid.config.newdom.CacheServerConfig serverConfig = cacheServer.GetNewConfiguration(cParam.getCacheId());
                if (serverConfig == null) {
                    successFull = false;
                    throw new RuntimeException("Specified cache is not registered on given server.");
                }

                try {
                    jarRef = new JARReflector();
                } catch (Exception e) {
                    successFull = false;
                    String message = String.format("Could not load assembly \"" + cParam.getAsmPath() + "\". %1$s", e.getMessage());
                    System.err.println("Error  : " + message);
                    return;
                }

                if (serverConfig.getCacheSettings().getBackingSource() == null) {
                    serverConfig.getCacheSettings().setBackingSource(new com.alachisoft.tayzgrid.config.newdom.BackingSource());
                }

                if (cParam.getIsReadThru()) { 
                strProviderClasses = jarRef.getClassNames(cParam.getAsmPath(), "com.alachisoft.tayzgrid.runtime.datasourceprovider.ReadThruProvider");
                 if(strProviderClasses.length == 0 || (!IsValidProviderClass(strProviderClasses, cParam.GetClass()))) {
                     strProviderClasses = jarRef.getClassNames(cParam.getAsmPath(), "javax.cache.integration.CacheLoader");
                     isValid = false;
                 }
                 else
                     isValid = true;
                 
                    if (!isValid && !(strProviderClasses.length > 0 && IsValidProviderClass(strProviderClasses, cParam.GetClass()))) {
                        successFull = false;
                        System.err.println("Error: Specified class does not implement ReadThruProvider.");
                        return;
                    } else {
                        if (serverConfig.getCacheSettings().getBackingSource().getReadthru() == null) {
                            serverConfig.getCacheSettings().getBackingSource().setReadthru(new com.alachisoft.tayzgrid.config.newdom.Readthru());
                            serverConfig.getCacheSettings().getBackingSource().getReadthru().setProviders(prov);

                        }

                        serverConfig.getCacheSettings().getBackingSource().getReadthru().setEnabled(true);
                        
                        prov = serverConfig.getCacheSettings().getBackingSource().getReadthru().getProviders();
                        serverConfig.getCacheSettings().getBackingSource().getReadthru().setProviders(GetSourceProvider(GetProvider(prov, cParam.getDefaultProvider())));
                    }
                }
                if (cParam.getIsWriteThru()) {
                    strProviderClasses = jarRef.getClassNames(cParam.getAsmPath(), "com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteThruProvider");
                    if(strProviderClasses.length == 0 || (!IsValidProviderClass(strProviderClasses,cParam.GetClass()))) {
                        strProviderClasses = jarRef.getClassNames(cParam.getAsmPath(), "javax.cache.integration.CacheWriter");
                        isValid = false;
                    }
                    else
                        isValid = true;
                    
                    if (!isValid && !(strProviderClasses.length > 0 && IsValidProviderClass(strProviderClasses, cParam.GetClass()))) {
                        successFull = false;
                        System.err.println("Error: Specified class does not implement WriteThruProvider.");
                        return;
                    } else {

                        if (serverConfig.getCacheSettings().getBackingSource().getWritethru() == null) {
                            serverConfig.getCacheSettings().getBackingSource().setWritethru(new com.alachisoft.tayzgrid.config.newdom.Writethru());
                            serverConfig.getCacheSettings().getBackingSource().getWritethru().setProviders(prov);
                            serverConfig.getCacheSettings().getBackingSource().getWritethru().setWriteBehind(new com.alachisoft.tayzgrid.config.newdom.WriteBehind());

                        }

                        if (serverConfig.getCacheSettings().getBackingSource().getWritethru().getWriteBehind() == null) {
                            serverConfig.getCacheSettings().getBackingSource().getWritethru().setWriteBehind(new com.alachisoft.tayzgrid.config.newdom.WriteBehind());
                        }

                        serverConfig.getCacheSettings().getBackingSource().getWritethru().setEnabled(true);
                        prov = serverConfig.getCacheSettings().getBackingSource().getWritethru().getProviders();
                        serverConfig.getCacheSettings().getBackingSource().getWritethru().setProviders(GetSourceProvider(GetProvider(prov, cParam.getDefaultProvider())));

                        if (cParam.isIsBatching()) {
                            serverConfig.getCacheSettings().getBackingSource().getWritethru().getWriteBehind().setBatchConfig(new BatchConfig());
                            serverConfig.getCacheSettings().getBackingSource().getWritethru().getWriteBehind().getBatchConfig().setBatchInterval("" + cParam.getBatchInterval());
                            serverConfig.getCacheSettings().getBackingSource().getWritethru().getWriteBehind().getBatchConfig().setBatchInterval("" + cParam.getOperationDelay());
                            serverConfig.getCacheSettings().getBackingSource().getWritethru().getWriteBehind().setMode("batch");
                        } else {
                            serverConfig.getCacheSettings().getBackingSource().getWritethru().getWriteBehind().setMode("non-batch");
                        }

                        serverConfig.getCacheSettings().getBackingSource().getWritethru().getWriteBehind().setThrottling("" + cParam.getOperationPerSecond());
                        serverConfig.getCacheSettings().getBackingSource().getWritethru().getWriteBehind().setRequeueLimit("" + cParam.getOperationQueueLimit());
                        serverConfig.getCacheSettings().getBackingSource().getWritethru().getWriteBehind().setEviction("" + cParam.getOperationEvictionRatio());

                    }
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
                        } catch (Exception ex) {
                            successFull = false;
                            System.err.println("Failed to Add Backing Source on " + NCache.getServerName());
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

                    } catch (Exception ex) {
                        successFull = false;
                        System.err.println("Failed to Add Backing Source on " + NCache.getServerName());
                        System.err.println("Error Detail: " + ex.getMessage());
                        LogEvent(ex.getMessage());
                    } finally {
                        cacheServer.dispose();
                    }
                }

                if (!cParam.getNoDeploy()) {

                    if (cParam.getServer() != null && !cParam.getServer().equals("")) {
                        ToolsRPCService.DeployAssembly(cParam.getServer(), cParam.getPort(), cParam.getAsmPath(), cParam.getCacheId(), cParam.getDepAsmPath(), "", "", logErr);
                    } else {
                        ToolsRPCService.DeployAssembly(NCache.getServerName(), cParam.getPort(), cParam.getAsmPath(), cParam.getCacheId(), cParam.getDepAsmPath(), "", "", logErr);
                    }

                }

            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e) {
            successFull = false;
            System.err.println("Error : " + e.getMessage() + "");
            e.printStackTrace();
            LogEvent(e.getMessage());
        } finally {
            if (successFull && !cParam.getIsUsage()) {
                System.out.println("Backing Source successfully configure");
            }
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

    public static java.util.Hashtable GetProvider(com.alachisoft.tayzgrid.config.newdom.Provider[] prov, boolean isDefault)//, System.Reflection.Assembly asm) 
    {
        java.util.Hashtable hash = new java.util.Hashtable();
        com.alachisoft.tayzgrid.config.newdom.Provider p = new com.alachisoft.tayzgrid.config.newdom.Provider();

        File assembly = new File(cParam.getAsmPath());

        p.setAssemblyName(assembly.getName());
        p.setClassName(cParam.GetClass());
        p.setFullProviderName(assembly.getName());
        p.setIsDefaultProvider(isDefault);
        p.setProviderName(cParam.getProviderName());
        p.setIsLoaderOnly(cParam.getIsLoaderOnly());
        if (cParam.getParameters() != null) {
            p.setParameters(GetProviderParams(GetParams(cParam.getParameters())));
        }

        p.setAsyncMode(cParam.getAsync());
        if (prov != null) {
            hash = ProviderToHashtable(prov);
        }

        if (hash.size() > 0) {

            boolean defaultProvider = false;
            Set set = hash.entrySet();
            Iterator it = set.iterator();

            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();

                com.alachisoft.tayzgrid.config.newdom.Provider provider = (com.alachisoft.tayzgrid.config.newdom.Provider) entry.getValue();
                if (provider.getIsDefaultProvider()) {
                    defaultProvider = true;
                    break;
                }

            }
            if (defaultProvider) {
                p.setIsDefaultProvider(false);
            } else {
                p.setIsDefaultProvider(cParam.getDefaultProvider());
            }
        }

        if (hash.containsKey(p.getProviderName())) {
            if (cParam.getIsReadThru()) {
                throw new RuntimeException("Readthru with the same name is already registered");
            } else {
                throw new RuntimeException("Writethru with the same name is already registered");
            }
        }
        hash.put(p.getProviderName(), p);
        return hash;
    }

    public static Provider[] GetSourceProvider(java.util.Hashtable pParams) {
        Provider[] param = new Provider[pParams.size()];
        int index = 0;

        for (Object key : pParams.keySet()) {
            param[index] = new Provider();
            param[index].setProviderName((String) key);
            param[index] = (Provider) pParams.get(key);
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

    public static boolean IsValidProviderClass(String[] Classes, String ProviderClass) {
        for (int index = 0; index < Classes.length; index++) {

            if (Classes[index].equals(ProviderClass)) {
                return true;
            }
        }
        return false;
    }
}
