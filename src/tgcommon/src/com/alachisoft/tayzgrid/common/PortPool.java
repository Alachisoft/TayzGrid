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
package com.alachisoft.tayzgrid.common;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortPool
{

    int jmx = -1;
    int snmp = -1;
    int snmpPortPool = -1;
    int jmxPortPool = -1;
    int snmpClient = -1;
    private static PortPool defaultPool = null;
    Map<String, ArrayList<Integer>> snmpPool = Collections.synchronizedMap(new HashMap());
    Map jmxPool = Collections.synchronizedMap(new HashMap());
    private static String serverType="";


    public static void setServerType(String serverType) {
        PortPool.serverType = serverType;
    }
    
   

    PortPool()
    {
       if(serverType.equals("CacheServer"))
       {
        if (ServicePropValues.SNMP_PORT != null)
        {
            snmp = Integer.parseInt(ServicePropValues.SNMP_PORT);
        }

        if (ServicePropValues.JMXServer_PORT != null)
        {
            jmx = Integer.parseInt(ServicePropValues.JMXServer_PORT);
        }

        if (ServicePropValues.SNMP_POOL != null)
        {
            snmpPortPool = Integer.parseInt(ServicePropValues.SNMP_POOL);
        }
        else
        {
            snmpPortPool = 10100;
        }

        if (ServicePropValues.JMX__POOL != null)
        {
            jmxPortPool = Integer.parseInt(ServicePropValues.JMX__POOL);
        }
        else
        {
            jmxPortPool = snmpPortPool + 5000;
        }
       }
     
    }

    public static PortPool getInstance()
    {
        if (defaultPool == null)
        {
            defaultPool = new PortPool();
        }

        return defaultPool;
    }

    public int getNextSNMPPort(String cacheName, InetAddress addr)
    {
        InetAddress address = addr == null ?  ServicePropValues.getStatsServerIp() :  addr;
        if (snmpPool.containsKey(cacheName))
        {
            List portList = (ArrayList)snmpPool.get(cacheName);
            return (Integer)portList.get((portList.size()-1));
        }
        else
        {
            while (snmpPortPool < jmxPortPool)
            {
                if(com.alachisoft.tayzgrid.common.net.Helper.isPortFree(snmpPortPool, address))
                {
                    if (!getValue(snmpPortPool))
                    {
                        List portList = new ArrayList<Integer>();
                        portList.add(snmpPortPool);
                        snmpPool.put(cacheName, (ArrayList<Integer>) portList);
                        snmpPortPool++;
                        return snmpPortPool - 1;
                    }
                    else
                    {
                        snmpPortPool++;
                    }
                }
                else
                {
                    snmpPortPool++;
                }
            }
            //should not happen
            return 0;
        }
    }

    /**
     * Selects available port, returns the value if cache registered then returns the same already assigned port SnmpdPort will be less than jmx port.
     *
     * @param cacheName CacheName
     * @return port for snmp, will reuse ports.
     */
    public int getNextSNMPPort(String cacheName)
    {
        return getNextSNMPPort(cacheName, null);
    }

    /**
     * Will update pool
     *
     * @param cacheName
     * @param port
     * @throws IllegalArgumentException Throws it when either cache is already present or port is already in use
     */
    public void assignPort(String cacheName, int port)
    {
        if(!getValue(port)){
            if (!snmpPool.containsKey(cacheName))
            {
                List portList = new ArrayList<Integer>();
                portList.add(port);
                snmpPool.put(cacheName, (ArrayList<Integer>) portList);
            }
            else
            {
                List portList = (ArrayList)snmpPool.get(cacheName);
                portList.add(port);
                snmpPool.put(cacheName, (ArrayList<Integer>) portList);
            }
        }
    }

    /**
     * Frees the port number to be reusable
     *
     * @param cacheName
     */
    public void disposeSNMPPort(String cacheName)
    {
        if (snmpPool.containsKey(cacheName))
        {
            ArrayList portList = (ArrayList)snmpPool.get(cacheName);
            if(portList.size()<=1)
            {
                snmpPortPool = (Integer)portList.get((portList.size()-1));
                snmpPool.remove(cacheName);
            }
        }
    }

    public Map<String, ArrayList<Integer>> getSNMPMap()
    {
        return Collections.unmodifiableMap(snmpPool);
    }

    public Integer getSNMPPort(String cacheName)
    {
        ArrayList<Integer> portList = (ArrayList<Integer>)snmpPool.get(cacheName);
        return portList.get(portList.size()-1);
    }

    public boolean isPortFound(String cacheName)
    {
        if(snmpPool.containsKey(cacheName))
        {
            if(((ArrayList)snmpPool.get(cacheName)).size()>0)
                return true;
        }
        return false;
    }
    public int getJMXPort()
    {
        return getJMXPort(null);
    }

    public int getJMXPort(InetAddress addr)
    {
        InetAddress address = addr == null ?  ServicePropValues.getStatsServerIp() :  addr;
        if (jmx != -1 && com.alachisoft.tayzgrid.common.net.Helper.isPortFree(jmx, address))
        {
            return jmx;
        }
        return com.alachisoft.tayzgrid.common.net.Helper.getFreePort(address);
    }
    
    public boolean getValue(int value)
    {
        for (Object entryObject : snmpPool.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject;
            ArrayList<Integer> portList = (ArrayList<Integer>) entry.getValue();
            for(int i=0; i<portList.size(); i++)
                if(portList.get(i) ==  value)
                    return true;
        }
        return false;
    }

    void disposeSNMPPort(String cacheId, Integer port)        
    {
        if (snmpPool.containsKey(cacheId))
        {
            ArrayList portList = (ArrayList)snmpPool.get(cacheId);
            if(portList.size()<=1)
            {
                snmpPortPool = (Integer)portList.get((portList.size()-1));
                snmpPool.remove(cacheId);
            }
            else
            {
                portList.remove(port);
                snmpPortPool = port;
                snmpPool.put(cacheId,portList);
            }
        }
    }
}
