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
import com.alachisoft.tayzgrid.tools.common.LogErrors;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import java.net.UnknownHostException;
import java.util.Iterator;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.utility.jarscanner.Fields;
import com.alachisoft.tayzgrid.utility.jarscanner.JARReflector;
import java.io.File;
import java.util.HashMap;
import java.util.List;

public final class ConfigureQueryIndexTool {

    private static ConfigureQueryIndexParam cParam = new ConfigureQueryIndexParam();
    private static CacheRPCService NCache;
    private static ICacheServer cacheServer;
    private static LogErrors logErr;

    /**
     * Validate all parameters in property string.
     */
    private static boolean ValidateParameters() {
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

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getAttributes())) {
            System.err.println("Error: Attributes not specified");
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
    public static void Run(String[] args) throws UnknownHostException, Exception {
        JARReflector jarRef = null;
        String[] strProviderClasses = null;
        com.alachisoft.tayzgrid.config.newdom.Class[] queryClasses = null;
        String failedNodes = "";
        boolean successFull=true;
        
        try {
            AssemblyUsage.PrintLogo(cParam.getIsLogo());
            NCache = ToolsRPCService.GetRPCService();

            Object param = new ConfigureQueryIndexParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (ConfigureQueryIndexParam) param;
            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                successFull=false;
                return;
                
            }
            
            if (!ValidateParameters()) {
                 
                successFull=false;
                return;
            }
            if (cParam.getServer() != null && !cParam.getServer().equals("")) {
                NCache.setServerName(cParam.getServer());
            }
            if (cParam.getPort() != -1) {
                NCache.setPort(cParam.getPort());
            }

            cacheServer = NCache.GetCacheServer(new TimeSpan(0, 0, 30));

            String extension = ".jar";
            if (cacheServer != null) {
                if (cacheServer.IsRunning(cParam.getCacheId())) {
                    successFull=false;
                    throw new RuntimeException(cParam.getCacheId() + " is Running on " + NCache.getServerName() + "Stop the cache first...");
                }

                com.alachisoft.tayzgrid.config.newdom.CacheServerConfig serverConfig = cacheServer.GetNewConfiguration(cParam.getCacheId());

                if (serverConfig == null) {
                    successFull=false;
                    throw new RuntimeException("Specified cache is not registered on given server.");
                }

                try {
                    jarRef = new JARReflector();
                    java.io.File temp = new File(cParam.getAsmPath());
                    if (temp.isFile()) {
                        extension = temp.getPath().substring(temp.getPath().lastIndexOf("."));
                    }

                } catch (Exception e) {
                    successFull=false;
                    String message = String.format("Could not load assembly \"" + cParam.getAsmPath() + "\". %1$s", e.getMessage());
                    System.err.println("Error : " + message);
                    LogEvent(e.getMessage());
                    return;
                }

                if (serverConfig.getCacheSettings().getQueryIndices() == null) {
                    serverConfig.getCacheSettings().setQueryIndices(new com.alachisoft.tayzgrid.config.newdom.QueryIndex());
                    serverConfig.getCacheSettings().getQueryIndices().setClasses(queryClasses);
                }

                queryClasses = serverConfig.getCacheSettings().getQueryIndices().getClasses();

                serverConfig.getCacheSettings().getQueryIndices().setClasses(GetSourceClass(GetClass(queryClasses)));
                
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
                            System.out.println("Query Index is configured successfully On: " + NCache.getServerName() + " For " + cParam.getCacheId());

                        } catch (Exception ex) {
                            successFull=false;
                            System.err.println("Failed to Add Query Index on " + NCache.getServerName());
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
                        System.out.println("Query Index is configured successfully On: " + NCache.getServerName() + " For " + cParam.getCacheId());

                    } catch (Exception ex) {
                        successFull=false;
                        System.err.println("Failed to Add Query Index on " + NCache.getServerName());
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
            successFull=false;
            
            
            System.err.println("Error : " + e.getMessage());
            LogEvent(e.getMessage());
        } finally {
            if(successFull && !cParam.getIsUsage())
            {
                System.out.println("Query Index added Successfully ");          
            }
            
            NCache.dispose();
        }
        }
    

    public static java.util.Hashtable GetAttributes(java.util.Hashtable attrib) {
        String[] str = cParam.getAttributes().split("[$]", -1);
        java.util.Hashtable hash = new java.util.Hashtable();
        for (String st : str) {
            hash.put(st, st);
        }
        //for merging attributes
        if (!attrib.isEmpty() && attrib != null) {
            for (Object key : attrib.keySet()) {
                hash.put(key, attrib.get(key));
            }
        }
        return hash;
    }

    public static com.alachisoft.tayzgrid.config.newdom.Attrib[] GetClassAttributes(java.util.Hashtable attrib, java.lang.Class type) throws Exception {
        com.alachisoft.tayzgrid.config.newdom.Attrib[] a = new com.alachisoft.tayzgrid.config.newdom.Attrib[attrib.size()];
        JARReflector jarRef = new JARReflector();
     
        HashMap<String, List<Fields>> AllFields = jarRef.getFieldsFullyQualify(cParam.getAsmPath());
        List<Fields> fi = AllFields.get(cParam.GetClass());
        
         int index = 0;
        
                for (Object key : attrib.keySet()) {
                    Fields temp = GetField(key, fi);
                    if (temp != null) {
                        a[index] = new com.alachisoft.tayzgrid.config.newdom.Attrib();
                        a[index].setName((String) key);
                        a[index].setID((String) attrib.get(key));
                        a[index].setType(temp.getType());
                        if(temp.getType().contains("java.math.BigInteger") || temp.getType().contains("java.math.BigDecimal") || temp.getType().equals("byte[]") || temp.getType().equals("short[]")  )
                        {
                            throw new Exception("Invalid Class attribute(s) specified");
                        }
                        index++;
                    }
                }
        return a;
    }

    public static java.util.Hashtable GetClass(com.alachisoft.tayzgrid.config.newdom.Class[] cl) throws Exception {
        java.util.Hashtable hash = new java.util.Hashtable();
        java.util.Hashtable att = new java.util.Hashtable();
        com.alachisoft.tayzgrid.config.newdom.Class c = new com.alachisoft.tayzgrid.config.newdom.Class();
        JARReflector jarRef = new JARReflector();

        c.setName(cParam.GetClass());

        java.lang.Class type;
        type = jarRef.getClassForName(cParam.getAsmPath(), cParam.GetClass());

        String fullVersion = "";
        c.setID(cParam.GetClass());
        if (cl != null) {
            hash = ClassToHashtable(cl);
        }

        if (hash.containsKey(c.getName())) {
            com.alachisoft.tayzgrid.config.newdom.Class existingClass = (com.alachisoft.tayzgrid.config.newdom.Class) hash.get(c.getName());
            att = AttribToHashtable(existingClass.getAttributes());
        }

        if (cParam.getAttributes() != null || !cParam.getAttributes().equals("")) {
            c.setAttributes(GetClassAttributes(GetAttributes(att), type));
        }


        hash.put(c.getName(), c);
        return hash;
    }

    public static com.alachisoft.tayzgrid.config.newdom.Class[] GetSourceClass(java.util.Hashtable pParams) {
        com.alachisoft.tayzgrid.config.newdom.Class[] param = new com.alachisoft.tayzgrid.config.newdom.Class[pParams.size()];
        int index = 0;
        for (Object key : pParams.keySet()) {
            param[index] = new com.alachisoft.tayzgrid.config.newdom.Class();
            param[index].setName((String) key);
            param[index] = (com.alachisoft.tayzgrid.config.newdom.Class) pParams.get(key);
            index++;
        }
        return param;
    }

    public static boolean ValidateClass(String cl, java.util.ArrayList cc) {
        for (Iterator it = cc.iterator(); it.hasNext();) {
            com.alachisoft.tayzgrid.config.newdom.Class c = (com.alachisoft.tayzgrid.config.newdom.Class) it.next();
            if (c.getName().equals(cl)) {
                return false;
            }
        }
        return true;
    }

    public static java.util.Hashtable ClassToHashtable(com.alachisoft.tayzgrid.config.newdom.Class[] cl) {
        java.util.Hashtable hash = new java.util.Hashtable();
        for (int i = 0; i < cl.length; i++) {
            hash.put(cl[i].getName(), cl[i]);
        }
        return hash;
    }

    public static java.util.Hashtable AttribToHashtable(com.alachisoft.tayzgrid.config.newdom.Attrib[] cl) {
        java.util.Hashtable hash = new java.util.Hashtable();
        for (int i = 0; i < cl.length; i++) {
            hash.put(cl[i].getID(), cl[i].getName());
        }
        return hash;
    }

    public static Fields GetField(Object key, List<Fields> fields) throws Exception {
        
        boolean invalidAttribute=false;
        
        try{
        for (Fields temp : fields) {

            if (temp.getName().equals(key)) {
                return temp;
            }
            else
            {
                invalidAttribute=true;
            }
        }
        }
        catch(Exception ex)
        {
            throw new Exception("Class not found");
        }
        if(invalidAttribute)
        {
            throw new Exception("Invalid Class attribute(s) specified '" +key+"'");
        }
        return null;
    }
}
