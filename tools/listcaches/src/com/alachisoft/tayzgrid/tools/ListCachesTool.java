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

import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.monitoring.ConfiguredCacheInfo;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.config.ConfigReader;
import com.alachisoft.tayzgrid.config.PropsConfigReader;
import com.alachisoft.tayzgrid.management.ICacheServer;
import com.alachisoft.tayzgrid.management.CacheRPCService;
import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import com.alachisoft.tayzgrid.management.ToolsRPCService;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.UnknownHostException;
import java.util.Iterator;
import tangible.RefObject;

public class ListCachesTool {

    private static CacheRPCService NCache;
    private static ListCachesParam cParam = new ListCachesParam();

    private static String format(String string) {
        return ListCachesTool.format(-17, string);
    }

    private static String formatId(String string) {
        if (string.length() > 30) {
            string = string.substring(0, 26);
            string = string + "...";
        }
        return ListCachesTool.format(-29, string);
    }

    private static String formatPartitionId(String string) {
        if (string.length() > 15) {
            string = string.substring(0, 11);
            string = string + "...";
        }
        return ListCachesTool.format(-15, string);
    }

    private static String formatScheme(String string) {
        return ListCachesTool.format(-28, string);
    }

    private static String formatStatus(String string) {
        return ListCachesTool.format(-7, string);
    }
    
    private static String formatPID(String string) 
    {
        if(Common.isNullorEmpty(string))
        return "";
        return ListCachesTool.format(-5, "("+string+")");
    }

    private static String format(int spaces, String string) {
        return String.format(String.format("%" + spaces + "s", string));
    }

    private static void PrintCacheInfo(CacheStatistics statistics,String topology ,String cacheName, boolean isRunning, String pid) {
        CacheStatistics s = statistics;
        String schemeName = s == null ? topology :s.getClassName().toLowerCase();
        String running = isRunning ? "Running" : "Stopped";
        String pidvalue = pid;
        System.out.println(ListCachesTool.formatId(cacheName) + ListCachesTool.formatScheme(schemeName) + ListCachesTool.formatStatus(running) + ListCachesTool.formatPID(pid));
    }

    private static void PrintDetailedCacheInfo(CacheStatistics statistics,String topology, String partId, boolean isRunning, String cacheName, String configString, String pid) //boolean printConf, boolean xmlSyntax,
    {
        CacheStatistics s = statistics;
        long MaxSize = 0;
        String schemeName = topology;
        boolean running = isRunning;
        System.out.println("Cache-Name:     " + cacheName);
        if (partId != null && !partId.equals("")) {
            System.out.println("Partition-ID:   " + partId);
        }
        System.out.println("Scheme:         " + schemeName);
        System.out.println("Status:         " + (isRunning ? "Running" : "Stopped"));
        if (running) 
        {
            System.out.println("Process-Id:     " + pid);
            if(s!=null)
            {
                if (s instanceof ClusterCacheStatistics) {
                    StringBuilder nodes = new StringBuilder();

                    ClusterCacheStatistics cs = (ClusterCacheStatistics) ((s instanceof ClusterCacheStatistics) ? s : null);
                    System.out.println("Cluster size:   " + cs.getNodes().size());

                    MaxSize = (cs.getLocalNode().getStatistics().getMaxSize() / 1024) / 1024;
                    for (Iterator it = cs.getNodes().iterator(); it.hasNext();) {
                        NodeInfo n = (NodeInfo) it.next();
                        nodes.append("                ").append(n.getAddress()).append("\n");
                    }
                    System.out.printf("%1$s", nodes.toString());

                    if (partId != null && !partId.equals("")) {
                        if (cs.getSubgroupNodes() != null && cs.getSubgroupNodes().containsKey(partId.toLowerCase())) {
                            nodes = new StringBuilder();
                            java.util.ArrayList groupNodes = (java.util.ArrayList) ((cs.getSubgroupNodes().get(partId.toLowerCase()) instanceof java.util.ArrayList) ? cs.getSubgroupNodes().get(partId.toLowerCase()) : null);
                            System.out.println("Partition size: " + groupNodes.size());
                            for (Iterator it = groupNodes.iterator(); it.hasNext();) {
                                Address address = (Address) it.next();
                                nodes.append("                ").append(address).append("\n");
                            }
                        }
                        System.out.printf("%1$s", nodes.toString());
                    }
                }
                System.out.println("UpTime:         " + s.getUpTime());

                if (s.getMaxSize() != 0) {
                    System.out.println("Capacity:       " + ((s.getMaxSize() / 1024) / 1024) + "MB");
                } else {
                    System.out.println("Capacity:        " + MaxSize + "MB");
                }

                System.out.println("Count:          " + s.getCount());
            }
        }
        System.out.println("");
    }

    /**
     *
     * @param args
     * @throws UnknownHostException
     * @throws Exception
     */
    public static void Run(String[] args) throws UnknownHostException, Exception {

        try {
            NCache = ToolsRPCService.GetRPCService();
            Object param = new ListCachesParam();
            tangible.RefObject<Object> tempRef_param = new RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (ListCachesParam) param;

            AssemblyUsage.PrintLogo(cParam.getIsLogo());

            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }
            if(!cParam.getDetail() && args.length != 0 && cParam.getServer().length() == 0 && cParam.getPort() == -1) //&& ! cParam.getXmlSyntax() 
            {
                AssemblyUsage.PrintUsage();
                return;
            }
            if (cParam.getPort() != -1) {
                NCache.setPort(cParam.getPort());
            }
            if (cParam.getServer() != null && !cParam.getServer().equals("")) {
                NCache.setServerName(cParam.getServer());
            }

             ICacheServer m = NCache.GetCacheServer(new TimeSpan(0, 0, 30));

            System.out.println("Listing registered caches on " + NCache.getServerName() + ": " + NCache.getPort() + " \n");

            if (m != null) {
                ConfiguredCacheInfo[] configuredCaches = new ConfiguredCacheInfo[]{}; 
                
                configuredCaches = m.GetAllConfiguredCaches();

                ConfiguredCacheInfo[] configuredPartitionOfReplicaCaches = null; // = m.GetConfiguredPartitionedReplicaCaches();
               
               if(configuredCaches.length !=0)
               {
                if (configuredCaches.length > 0 || configuredPartitionOfReplicaCaches.length > 0) {
                    if (!cParam.getDetail()) {
                        System.out.println("Cache-Name                   Scheme                      Status(PID)");
                        System.out.println("----------                   ------                      -----------");
                    }

                    for (int i = 0; i < configuredCaches.length; i++) {
                        ConfiguredCacheInfo cacheInfo = configuredCaches[i];

                        if (!cParam.getDetail()) {
                            PrintCacheInfo(null,cacheInfo.getTopology().name(), cacheInfo.getCacheId(), cacheInfo.getIsRunning(), cacheInfo.getPID());
                        } else {
                            PrintDetailedCacheInfo(m.GetCacheStatistics2(cacheInfo.getCacheId()) ,cacheInfo.getTopology().name() ,null, cacheInfo.getIsRunning(), cacheInfo.getCacheId(), cacheInfo.getCachePropString(), cacheInfo.getPID()); //cParam.getPrintConf(), cParam.getCacheId()
                        }
                    }

                } else {
                    System.out.println("There are no registered caches on " + NCache.getServerName());
                }
               }
               else {
                    System.out.println("There are no registered caches on " + NCache.getServerName());
                }
               
            }
            else
            {throw new Exception("TayzGrid service can not be contacted");}
        } catch (Exception e) {
            System.err.println("Error : " + e.getMessage());
        } 
        finally {
            NCache.dispose();
        }
    }
}
