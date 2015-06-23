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
import com.alachisoft.tayzgrid.config.dom.*;
import com.alachisoft.tayzgrid.common.net.*;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Iterator;

public final class RemoveQueryIndexTool {

    private static RemoveQueryIndexParam cParam = new RemoveQueryIndexParam();
    private static CacheRPCService NCache;
    private static ICacheServer cacheServer;

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
        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.GetClass())) {
            System.err.println("Error: Class Name not specified");
            return false;
        }
        return true;
    }

    private static void LogEvent(String msg) {
    }

    /**
     * The main entry point for the tool. ju
     */
    public static void Run(String[] args) throws Exception {
        com.alachisoft.tayzgrid.config.newdom.Class[] queryClasses = null;
        String failedNodes = "";

        try {
            NCache = ToolsRPCService.GetRPCService();

            Object param = new RemoveQueryIndexParam();
            tangible.RefObject<Object> tempRef_param = new tangible.RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (RemoveQueryIndexParam) param;
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

                if (serverConfig.getCacheSettings().getQueryIndices() != null) {

                    if (serverConfig.getCacheSettings().getQueryIndices().getClasses() != null) {
                        queryClasses = serverConfig.getCacheSettings().getQueryIndices().getClasses();
                    } else {
                        return;
                    }

                    if (queryClasses != null) {
                        serverConfig.getCacheSettings().getQueryIndices().setClasses(GetSourceClass(GetClass(queryClasses)));
                        if (serverConfig.getCacheSettings().getQueryIndices().getClasses().length == 0) {
                            serverConfig.getCacheSettings().setQueryIndices(null);
                        }
                    }
                } else {
                    return;
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
                            System.out.println("Query Index on " + NCache.getServerName() + " for " + cParam.getCacheId() + " Successfully Removed");
                        } catch (RuntimeException ex) {
                            System.err.println("Failed to Remove Query Index on " + NCache.getServerName());
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
                        System.out.println("Query Index on " + NCache.getServerName() + " for " + cParam.getCacheId() + " Successfully Removed");


                    } catch (Exception ex) {
                        System.err.println("Failed to Remove Query Index on " + NCache.getServerName());
                        if(ex.getMessage()!=null)
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
            if(e.getMessage()!=null)
                System.err.println("Error : " + e.getMessage());
            LogEvent(e.getMessage());
        } finally {
            NCache.dispose();
        }
    }

    public static java.util.Hashtable GetAttributes(java.util.Hashtable attrib) {
        String[] str = cParam.getAttributes().split("[$]", -1);

        if (attrib.size() != 0 && attrib != null) {
            for (String st : str) {
                if (attrib.keySet().contains(st)) {
                    attrib.remove(st);
                }
            }
        }
        return attrib;
    }

    public static com.alachisoft.tayzgrid.config.newdom.Attrib[] GetClassAttributes(java.util.Hashtable attrib) {
        com.alachisoft.tayzgrid.config.newdom.Attrib[] a = new com.alachisoft.tayzgrid.config.newdom.Attrib[attrib.size()];
        int index = 0;
        com.alachisoft.tayzgrid.config.newdom.Attrib attribValue = new com.alachisoft.tayzgrid.config.newdom.Attrib();
        for (Object key : attrib.keySet()) {
            a[index] = new com.alachisoft.tayzgrid.config.newdom.Attrib();
            attribValue = (com.alachisoft.tayzgrid.config.newdom.Attrib) attrib.get(key);
            a[index].setName((String) attribValue.getName());
            a[index].setID((String) attribValue.getID());
            a[index].setType((String) attribValue.getType());
            index++;

        }
        return a;
    }

    public static java.util.Hashtable GetClass(com.alachisoft.tayzgrid.config.newdom.Class[] cl) {
        java.util.Hashtable hash = new java.util.Hashtable();
        java.util.Hashtable att = new java.util.Hashtable();
        com.alachisoft.tayzgrid.config.newdom.Class c = new com.alachisoft.tayzgrid.config.newdom.Class();
        if (cl != null) {
            hash = ClassToHashtable(cl);

        }

        com.alachisoft.tayzgrid.config.newdom.Class existingClass = null;

        if (cParam.getAttributes() == null || cParam.getAttributes().equals("")) {
            if (hash.keySet().contains(cParam.GetClass())) {
                hash.remove(cParam.GetClass());
            }
        } else if (cParam.getAttributes() != null && !cParam.getAttributes().equals("")) {

            if (hash.keySet().contains(cParam.GetClass())) {
                existingClass = (com.alachisoft.tayzgrid.config.newdom.Class) hash.get(cParam.GetClass());
                att = AttribToHashtable(existingClass.getAttributes());
            }
            existingClass.setAttributes(GetClassAttributes(GetAttributes(att)));
            if (existingClass.getAttributes().length == 0) {
                hash.remove(existingClass.getName());
            } else {
                hash.put(existingClass.getName(), existingClass);
            }
        }

        return hash;
    }

    public static com.alachisoft.tayzgrid.config.newdom.Class[] GetSourceClass(java.util.Hashtable pParams) {
        com.alachisoft.tayzgrid.config.newdom.Class[] param = new com.alachisoft.tayzgrid.config.newdom.Class[pParams.size()];

        int index = 0;
        for (Object key : pParams.keySet()) {
            com.alachisoft.tayzgrid.config.newdom.Class c = (com.alachisoft.tayzgrid.config.newdom.Class) pParams.get(key);
            if (c.getAttributes().length > 0) {
                param[index] = new com.alachisoft.tayzgrid.config.newdom.Class();
                param[index].setName((String) key);
                param[index] = (com.alachisoft.tayzgrid.config.newdom.Class) pParams.get(key);
            }
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
            hash.put(cl[i].getName(), cl[i]);
        }
        return hash;
    }
}
