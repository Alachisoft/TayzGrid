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

import com.alachisoft.tayzgrid.config.newdom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.newdom.CacheTopology;
import com.alachisoft.tayzgrid.config.newdom.Cleanup;
import com.alachisoft.tayzgrid.config.newdom.EvictionPolicy;
import com.alachisoft.tayzgrid.config.newdom.Storage;
import com.alachisoft.tayzgrid.web.caching.CacheInitParams;
import com.alachisoft.tayzgrid.web.caching.CacheMode;
import java.math.BigDecimal;


public class JSR107InitParams extends CacheInitParams{
    private CacheServerConfig serverConfig;

    public JSR107InitParams() {
        serverConfig = new CacheServerConfig();
        Storage storage = new Storage();
        storage.setSize(1024);
        storage.setType("heap");
        serverConfig.getCacheSettings().setStorage(storage);  
        serverConfig.getCacheSettings().setInProc(true);
        
        //All notifications enabled by default
        serverConfig.getCacheSettings().getNotifications().setCacheClear(true);
        serverConfig.getCacheSettings().getNotifications().setItemAdd(true);
        serverConfig.getCacheSettings().getNotifications().setItemRemove(true);
        serverConfig.getCacheSettings().getNotifications().setItemUpdate(true);
        
        //Default eviction policy
        EvictionPolicy evictionPolicy = new EvictionPolicy();
        evictionPolicy.setDefaultPriority("normal");
        evictionPolicy.setEnabled(true);
        evictionPolicy.setEvictionRatio(new BigDecimal(5));
        evictionPolicy.setPolicy("priority");
        serverConfig.getCacheSettings().setEvictionPolicy(evictionPolicy);
        
        //Default cleanup settings
        Cleanup cleanup = new Cleanup();
        cleanup.setInterval(15);
        serverConfig.getCacheSettings().setCleanup(cleanup);
        
        //Default cache-topology
        CacheTopology topology = new CacheTopology();
        topology.setTopology("local");
        serverConfig.getCacheSettings().setCacheTopology(topology);
    }
    
    
    
    @Override
    public void Initialize(String cacheName)
    {
        //No need to load configurations
        serverConfig.getCacheSettings().setName(cacheName);
    }
    
    @Override
    public CacheMode getMode()
    {
        //Onlu inproc cache can be created using JCache API
        return CacheMode.InProc;
    }
    
    public CacheServerConfig getCacheServerConfig()
    {
        return serverConfig;
    }

}
