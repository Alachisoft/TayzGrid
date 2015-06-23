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

package com.alachisoft.tayzgrid.jsr107.configuration;

import com.alachisoft.tayzgrid.config.newdom.ServerNode;
import java.net.Inet4Address;


public class ClusteredCacheConfiguration<K, V> extends CacheConfiguration<K, V> {
    
    public ClusteredCacheConfiguration()
    {
        this.initParams.getCacheServerConfig().getCacheSettings().getCacheTopology().setTopology(ClusterTopology.Partitioned.getValue());
        this.initParams.getCacheServerConfig().getCacheSettings().getCacheTopology().getClusterSettings().getChannel().setTcpPort(8701);
    }
    
    public ClusteredCacheConfiguration<K, V> setClusterTopology(ClusterTopology topology)
    {
        this.initParams.getCacheServerConfig().getCacheSettings().getCacheTopology().setTopology(topology.getValue());
        
        return this;
    }
    
    public ClusterTopology getClusterTopology()
    {
        return ClusterTopology.forValue(this.initParams.getCacheServerConfig().getCacheSettings().getCacheTopology().getTopology());
    }
    
    public ClusteredCacheConfiguration<K, V> addServer(Inet4Address ip)
    {
        if(!this.initParams.getCacheServerConfig().getCacheDeployment().getServers().FindNode(ip.getHostAddress()))
        {
            ServerNode clusterNode = new ServerNode(ip.getHostAddress(), false);
            this.initParams.getCacheServerConfig().getCacheDeployment().getServers().getNodesList().add(clusterNode);
        }
        return this;
    }
    
    public ClusteredCacheConfiguration<K, V> removeServer(Inet4Address ip)
    {
        if(!this.initParams.getCacheServerConfig().getCacheDeployment().getServers().FindNode(ip.getHostAddress()))
        {
            ServerNode clusterNode = new ServerNode(ip.getHostAddress(), false);
            this.initParams.getCacheServerConfig().getCacheDeployment().getServers().getNodesList().remove(clusterNode);
        }
        return this;
    }
    
    public ClusteredCacheConfiguration<K, V> setClusterPort(int port)
    {
        this.initParams.getCacheServerConfig().getCacheSettings().getCacheTopology().getClusterSettings().getChannel().setTcpPort(port);
        return this;
    }

}
