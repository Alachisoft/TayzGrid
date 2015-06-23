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

package com.alachisoft.tayzgrid.config.newdom;

import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.common.net.Address;

public class DomHelper {

    public static com.alachisoft.tayzgrid.config.dom.CacheServerConfig convertToOldDom(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig newDom) throws Exception {
        com.alachisoft.tayzgrid.config.dom.CacheServerConfig oldDom = null;
        try {
            if (newDom != null) {
                oldDom = new com.alachisoft.tayzgrid.config.dom.CacheServerConfig();
                oldDom.setCacheLoaderFactory(newDom.getCacheLoaderFactory());
                oldDom.setCacheWriterFactory(newDom.getCacheWriterFactory());
                oldDom.setIsLoaderOnly(newDom.getIsLoaderOnly());
                if (newDom.getCacheSettings() != null) {
                    oldDom.setAutoStartCacheOnServiceStartup(newDom.getCacheSettings().getAutoStartCacheOnServiceStartup());
                    oldDom.setName(newDom.getCacheSettings().getName());
                    oldDom.setInProc(newDom.getCacheSettings().getInProc());
                    oldDom.setDataFormat(newDom.getCacheSettings().getDataFormat());
                    oldDom.setConfigID(newDom.getConfigID());
                    oldDom.setLastModified(newDom.getCacheSettings().getLastModified());
                    oldDom.setManagementPort(newDom.getCacheSettings().getManagementPort());
                    oldDom.setClientPort(newDom.getCacheSettings().getClientPort());
                    if (newDom.getCacheSettings().getLog() != null) {
                        oldDom.setLog(newDom.getCacheSettings().getLog());
                    } else {
                        oldDom.setLog(new com.alachisoft.tayzgrid.config.newdom.Log());
                    }

                    if (newDom.getCacheSettings().getPerfCounters() != null) {
                        oldDom.setPerfCounters(newDom.getCacheSettings().getPerfCounters());
                    } else {
                        oldDom.setPerfCounters(new PerfCounters());
                    }

                  

                    if (newDom.getCacheSettings().getQueryIndices() != null) {
                        oldDom.setQueryIndices(newDom.getCacheSettings().getQueryIndices());
                    }

                    if (newDom.getCacheSettings().getBackingSource() != null) {
                        oldDom.setBackingSource(newDom.getCacheSettings().getBackingSource());
                    }

                    if (newDom.getCacheSettings().getCacheLoader() != null) {
                        oldDom.setCacheLoader(newDom.getCacheSettings().getCacheLoader());
                    }

                    if (newDom.getCacheSettings().getNotifications() != null) {
                        oldDom.setNotifications(newDom.getCacheSettings().getNotifications());
                    } else {
                        oldDom.setNotifications(new Notifications());
                    }

                 
                    if (newDom.getCacheSettings().getCleanup() != null) {
                        oldDom.setCleanup(newDom.getCacheSettings().getCleanup());
                    } else {
                        oldDom.setCleanup(new Cleanup());
                    }

                    if (newDom.getCacheSettings().getStorage() != null) {
                        oldDom.setStorage(newDom.getCacheSettings().getStorage());
                    } else {
                        oldDom.setStorage(new Storage());
                    }

                    if (newDom.getCacheSettings().getEvictionPolicy() != null) {
                        oldDom.setEvictionPolicy(newDom.getCacheSettings().getEvictionPolicy());
                    } else {
                        oldDom.setEvictionPolicy(new EvictionPolicy());
                    }
                    
                    if(newDom.getCacheSettings().getExpirationPolicy()!=null){
                        oldDom.setExpirationPolicy(newDom.getCacheSettings().getExpirationPolicy());
                    }else
                    {
                        oldDom.setExpirationPolicy(new ExpirationPolicy());
                    }

                    if(newDom.getCacheSettings().getTaskConfiguration() != null) {
                        oldDom.setTaskConfiguration(newDom.getCacheSettings().getTaskConfiguration());
                    } else {
                        oldDom.setTaskConfiguration(new TaskConfiguration());
                    }
                    
                    if (newDom.getCacheSettings().getCacheTopology() != null) {

                        oldDom.setCacheType(newDom.getCacheSettings().getCacheType());
                    }

                    if (oldDom.getCacheType().equals("clustered-cache")) {

                        if (newDom.getCacheDeployment() != null) {

                            if (oldDom.getCluster() == null) {
                                oldDom.setCluster(new com.alachisoft.tayzgrid.config.dom.Cluster());
                            }

                            String topology = newDom.getCacheSettings().getCacheTopology().getTopology();
                            if (topology != null) {
                                topology = topology.toLowerCase();

                                if (topology.equals("replicated")) {
                                    topology = "replicated-server";
                                } else if (topology.equals("partitioned")) {
                                    topology = "partitioned-server";
                                } else if (topology.equals("local")) {
                                    topology = "local-cache";
                                } 
                            }

                            oldDom.getCluster().setTopology(topology);
                            oldDom.getCluster().setOpTimeout(newDom.getCacheSettings().getCacheTopology().getClusterSettings().getOpTimeout());
                            oldDom.getCluster().setStatsRepInterval(newDom.getCacheSettings().getCacheTopology().getClusterSettings().getStatsRepInterval());
                            oldDom.getCluster().setUseHeartbeat(newDom.getCacheSettings().getCacheTopology().getClusterSettings().getUseHeartbeat());

                            if (oldDom.getCluster().getChannel() == null) {
                                oldDom.getCluster().setChannel(new com.alachisoft.tayzgrid.config.dom.Channel());
                            }

                            oldDom.getCluster().getChannel().setTcpPort(newDom.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().getTcpPort());
                            oldDom.getCluster().getChannel().setPortRange(newDom.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().getPortRange());
                            oldDom.getCluster().getChannel().setConnectionRetries(newDom.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().getConnectionRetries());
                            oldDom.getCluster().getChannel().setConnectionRetryInterval(newDom.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().getConnectionRetryInterval());
                            
                            oldDom.getCluster().getChannel().setInitialHosts(createInitialHosts(newDom.getCacheDeployment().getServers().getNodesList(), newDom.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().getTcpPort()));
                            oldDom.getCluster().getChannel().setNumInitHosts(newDom.getCacheDeployment().getServers().getNodesList().size());

                            oldDom.getCluster().setActiveMirrorNode(newDom.getCacheDeployment().getServers().getActiveMirrorNode());

                            if (newDom.getCacheDeployment().getClientNodes() != null) {
                                if (oldDom.getClientNodes() == null) {
                                    oldDom.setClientNodes(new ClientNodes());
                                }

                                oldDom.setClientNodes(newDom.getCacheDeployment().getClientNodes());
                            }
                        }

                    }

                    if (newDom.getCacheSettings().getAutoLoadBalancing() != null) {
                        oldDom.setAutoLoadBalancing(newDom.getCacheSettings().getAutoLoadBalancing());
                    }

                    if (newDom.getCacheSettings().getAlertsNotifications() != null) {
                        oldDom.setAlertsNotifications(newDom.getCacheSettings().getAlertsNotifications());
                    }

                    if (newDom.getCacheSettings().getSQLDependencyConfig() != null) {
                        oldDom.setSQLDependencyConfig(newDom.getCacheSettings().getSQLDependencyConfig());
                    }


                    oldDom.setIsRunning(newDom.getIsRunning());
                    oldDom.setIsRegistered(newDom.getIsRegistered());
                    oldDom.setIsExpired(newDom.getIsExpired());
                    oldDom.setManagementPort(newDom.getCacheSettings().getManagementPort());
                    oldDom.setClientPort(newDom.getCacheSettings().getClientPort());
                }
            }
        } catch (Exception ex) {
            throw new Exception("DomHelper.convertToOldDom" + ex.getMessage());
        }
        return oldDom;

    }

    public static com.alachisoft.tayzgrid.config.newdom.CacheServerConfig convertToNewDom(com.alachisoft.tayzgrid.config.dom.CacheServerConfig oldDom) throws Exception {
        com.alachisoft.tayzgrid.config.newdom.CacheServerConfig newDom = null;
        try {
            if (oldDom != null) {
                newDom = new CacheServerConfig();
                newDom.setCacheLoaderFactory(oldDom.getCacheLoaderFactory());
                newDom.setCacheWriterFactory(oldDom.getCacheWriterFactory());
                if (newDom.getCacheSettings() == null) {
                    newDom.setCacheSettings(new CacheServerConfigSetting());
                }
                newDom.getCacheSettings().setAutoStartCacheOnServiceStartup(oldDom.getAutoStartCacheOnServiceStartup());
                newDom.getCacheSettings().setManagementPort(oldDom.getManagementPort());
                newDom.getCacheSettings().setClientPort(oldDom.getClientPort());
                newDom.getCacheSettings().setName(oldDom.getName());
                newDom.getCacheSettings().setDataFormat(oldDom.getDataFormat());
                newDom.getCacheSettings().setInProc(oldDom.getInProc());
                newDom.setConfigID(oldDom.getConfigID());
                newDom.getCacheSettings().setLastModified(oldDom.getLastModified());
               
                if (oldDom.getLog() != null) {
                    newDom.getCacheSettings().setLog(oldDom.getLog());
                } else {
                    newDom.getCacheSettings().setLog(new Log());
                }

                if (oldDom.getPerfCounters() != null) {
                    newDom.getCacheSettings().setPerfCounters(oldDom.getPerfCounters());
                } else {
                    newDom.getCacheSettings().setPerfCounters(new PerfCounters());
                }

              

                if (oldDom.getQueryIndices() != null) {
                    newDom.getCacheSettings().setQueryIndices(oldDom.getQueryIndices());
                }
                
                if (oldDom.getBackingSource() != null) {
                    newDom.getCacheSettings().setBackingSource(oldDom.getBackingSource());
                }

                if (oldDom.getCacheLoader() != null) {
                    newDom.getCacheSettings().setCacheLoader(oldDom.getCacheLoader());
                }

                if (oldDom.getNotifications() != null) {
                    newDom.getCacheSettings().setNotifications(oldDom.getNotifications());
                } else {
                    newDom.getCacheSettings().setNotifications(new Notifications());
                }

             

                if (oldDom.getCleanup() != null) {
                    newDom.getCacheSettings().setCleanup(oldDom.getCleanup());
                } else {
                    newDom.getCacheSettings().setCleanup(new Cleanup());
                }

                if (oldDom.getStorage() != null) {
                    newDom.getCacheSettings().setStorage(oldDom.getStorage());
                } else {
                    newDom.getCacheSettings().setStorage(new Storage());
                }

                if (oldDom.getEvictionPolicy() != null) {
                    newDom.getCacheSettings().setEvictionPolicy(oldDom.getEvictionPolicy());
                } else {
                    newDom.getCacheSettings().setEvictionPolicy(oldDom.getEvictionPolicy());
                }
                
                if(oldDom.getExpirationPolicy()!=null)
                {
                    newDom.getCacheSettings().setExpirationPolicy(oldDom.getExpirationPolicy());
                }
                else
                    newDom.getCacheSettings().setExpirationPolicy(new ExpirationPolicy());
                

                if(oldDom.getTaskConfiguration() != null) {
                    newDom.getCacheSettings().setTaskConfiguration(oldDom.getTaskConfiguration());
                } else {
                    newDom.getCacheSettings().setTaskConfiguration(oldDom.getTaskConfiguration());
                }
                
                if (newDom.getCacheSettings().getCacheTopology() == null) {

                    newDom.getCacheSettings().setCacheTopology(new CacheTopology());
                }

                newDom.getCacheSettings().setCacheType(oldDom.getCacheType());
                if (oldDom.getCluster() != null) {
                    String topology = oldDom.getCluster().getTopology();
                    if (topology != null) {
                        topology = topology.toLowerCase();

                        if (topology.equals("replicated-server")) {
                            topology = "replicated";
                        } else if (topology.equals("partitioned-server")) {
                            topology = "partitioned";
                        } else if (topology.equals("local-cache")) {
                            topology = "local-cache";
                        } 
                    }

                    newDom.getCacheSettings().getCacheTopology().setTopology(topology);
                    if (oldDom.getCacheType().equals("clustered-cache")) {
                        if (newDom.getCacheDeployment() == null) {
                            newDom.setCacheDeployment(new CacheDeployment());
                        }

                        if (newDom.getCacheSettings().getCacheTopology().getClusterSettings() == null) {
                            newDom.getCacheSettings().getCacheTopology().setClusterSettings(new Cluster());
                        }
                        newDom.getCacheSettings().getCacheTopology().getClusterSettings().setOpTimeout(oldDom.getCluster().getOpTimeout());
                        newDom.getCacheSettings().getCacheTopology().getClusterSettings().setStatsRepInterval(oldDom.getCluster().getStatsRepInterval());
                        newDom.getCacheSettings().getCacheTopology().getClusterSettings().setUseHeartbeat(oldDom.getCluster().getUseHeartbeat());

                        if (newDom.getCacheSettings().getCacheTopology().getClusterSettings().getChannel() == null) {
                            newDom.getCacheSettings().getCacheTopology().getClusterSettings().setChannel(new Channel());
                        }

                        if (oldDom.getCluster().getChannel() != null) {
                            newDom.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().setTcpPort(oldDom.getCluster().getChannel().getTcpPort());
                            newDom.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().setPortRange(oldDom.getCluster().getChannel().getPortRange());
                            newDom.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().setConnectionRetries(oldDom.getCluster().getChannel().getConnectionRetries());
                            newDom.getCacheSettings().getCacheTopology().getClusterSettings().getChannel().setConnectionRetryInterval(oldDom.getCluster().getChannel().getConnectionRetryInterval());
                        }

                        if (newDom.getCacheDeployment().getServers() == null) {
                            newDom.getCacheDeployment().setServers(new ServersNodes());
                        }
                        newDom.getCacheDeployment().getServers().setNodesList(createServers(oldDom.getCluster().getChannel().getInitialHosts(), oldDom.getCluster().getActiveMirrorNode()));

                        if (oldDom.getClientNodes() != null) {
                            if (newDom.getCacheDeployment().getClientNodes() == null) {
                                newDom.getCacheDeployment().setClientNodes(new ClientNodes());
                            }

                            newDom.getCacheDeployment().setClientNodes(oldDom.getClientNodes());
                        }

                    }
                } else {
                    if (oldDom.getCacheType() != null) {
                        if (oldDom.getCacheType().equals("client-cache")) {
                            newDom.getCacheSettings().getCacheTopology().setTopology(oldDom.getCacheType());
                        } else if (oldDom.getCacheType().equals("local-cache")) {
                            newDom.getCacheSettings().getCacheTopology().setTopology(oldDom.getCacheType());
                        }

                        newDom.getCacheSettings().getCacheTopology().setClusterSettings(null);
                    }
                }

                if (oldDom.getAutoLoadBalancing() != null) {
                    newDom.getCacheSettings().setAutoLoadBalancing(oldDom.getAutoLoadBalancing());
                }
                if (oldDom.getAlertsNotifications() != null) {
                    newDom.getCacheSettings().setAlertsNotifications(oldDom.getAlertsNotifications());
                }
                if (oldDom.getSQLDependencyConfig() != null) {
                    newDom.getCacheSettings().setSQLDependencyConfig(oldDom.getSQLDependencyConfig());
                }
                newDom.setIsRunning(oldDom.getIsRunning());
                newDom.setIsRegistered(oldDom.getIsRegistered());
                newDom.setIsExpired(oldDom.getIsExpired());
            }
        } catch (Exception ex) {
            throw new Exception("DomHelper.convertToNewDom" + ex.getMessage());
        }
        return newDom;
    }

    private static java.util.ArrayList createServers(String l, String an) throws Exception {
        Global.Tokenizer tok = new Global.Tokenizer(l, ",");
        String t;
        Address addr;
        int port;
        java.util.ArrayList retval = new java.util.ArrayList();
        java.util.HashMap hosts = new java.util.HashMap();
        ServerNode node;
        int j = 0;
        while (tok.hasNext()) {
            try {
                t = tok.next();
                String host = t.substring(0, (t.indexOf((char) '[')) - (0));
                host = host.trim();
                port = Integer.parseInt(t.substring(t.indexOf((char) '[') + 1, t.indexOf((char) '[') + 1 + (t.indexOf((char) ']')) - (t.indexOf((char) '[') + 1)));
                if (an != null && !an.isEmpty()) {
                    if (an.equals(host)) {
                        node = new ServerNode(host, true);
                    } else {
                        node = new ServerNode(host, false);
                    }
                } else {
                    node = new ServerNode(host, false);
                }

                retval.add(node);
                j++;

            } catch (NumberFormatException e) {
            } catch (Exception e) {
            }
        }

        return retval;

    }

    private static String createInitialHosts(java.util.ArrayList nodes, int port) throws Exception {
        String initialhost = "";
        try {
            for (int index = 0; index < nodes.size(); index++) {
                ServerNode node = (ServerNode) nodes.get(index);
                initialhost = initialhost + node.getIP().toString() + "[" + port + "]";
                if (nodes.size() > 1 && index != nodes.size() - 1) {
                    initialhost = initialhost + ",";
                }
            }
        } catch (Exception ex) {
        }

        return initialhost;
    }
}
