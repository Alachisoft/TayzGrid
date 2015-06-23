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
package com.alachisoft.tayzgrid.common.caching.statistics.monitoring;

import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.net.NetworkData;
import com.alachisoft.tayzgrid.common.PortPool;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.Operations;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.ServerOperations;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.PerformanceCounter;
import com.alachisoft.tayzgrid.common.caching.statistics.customcounters.StringCounter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hyperic.sigar.SigarException;
import org.weakref.jmx.Managed;

public abstract class ServerMonitor extends Monitor implements ServerMonitorMBean
{
    protected static EnumMap serverCounterStore;

    private String nodeName;
    static
    {
        serverCounterStore = new EnumMap<ServerOperations, PerformanceCounter>(ServerOperations.class);
    }


    @Override
    public void setPort()
    {
        super.setSnmpPort(PortPool.getInstance().getNextSNMPPort(nodeName, ServicePropValues.getStatsServerIp()));
    }
    
    public void setPort(int port)
    {
        super.setSnmpPort(port);
    }

    public static EnumMap getServerCounterStore()
    {
        return serverCounterStore;
    }

    public ServerMonitor(String nodeName)
    {
        super(nodeName);
        this.nodeName= nodeName;
        setPort();
    }



    public ServerMonitor(String nodeName, ILogger logger)
    {
        super(nodeName, logger);
         this.nodeName= nodeName;
        setPort();
    }
    
    public ServerMonitor(String nodeName, ILogger logger, int port)
    {
        super(nodeName, logger);
         this.nodeName= nodeName;
        setPort(port);
    }

    public final double getServerCounter(Operations operation)
    {
        return ((PerformanceCounter) getServerCounterStore().get(operation)).getValue();
    }
    
    public String getSnmpPort(Operations operation)
    {
        return ((StringCounter)getServerCounterStore().get(operation)).getStringValue();
    }
    

    @Managed(description="Last event occured during operation performed on TayzGrid", name="Logged Event" )
    @Override
    public String getLoggedEvent()
    {
        return EventLogger.getLastLoggedEvent();
    }

    @Managed(description="Number of requests received (meaning cache commands like add, get, insert, remove etc.) from all clients by this cache server.",name="Requests/sec")
    @Override
    public double getRequestsPerSec()
    {
        return getServerCounter(ServerOperations.RequestsPerSec);
    }

    @Managed(description="Bytes being received by cache server from all its clients.",name="Client Bytes Received/sec")
    @Override
    public double getClientBytesRecievedPerSecStats()
    {
        return getServerCounter(ServerOperations.ClientBytesRecievedPerSec);

    }

    @Managed(description="Number of responses received by all clients from the cache server.",name="Responses/sec")
    @Override
    public double getResponsesPerSec()
    {
        return getServerCounter(ServerOperations.ResponsesPerSec);
    }

    @Managed(description="Bytes being sent from cache server to all its clients.",name="Client Bytes Sent/sec")
    @Override
    public double getClientBytesSentPerSecStats()
    {
        return getServerCounter(ServerOperations.ClientBytesSentPerSec);
    }

    @Managed(description="Average time, in microseconds, taken to complete one cache-operation.",name="Average µs/cache operations")
    @Override
    public double getmSecPerCacheOperation()
    {
        return getServerCounter(ServerOperations.MSecPerCacheOperation);
    }

    @Managed(description="Total network usage consumed by TayzGrid process. ",name="TayzGrid Network Usage")
    @Override
    public double getVMNetworkUsage()
    {
       return getServerCounter(ServerOperations.VMNetworkUsage);
    }

    @Managed(description="Port used by SNMP manager to published server, cache and  client counter.",name="Cache Ports"   )
    @Override
    public final String getCachePorts()
    {
        Map map = PortPool.getInstance().getSNMPMap();
        
        Map newMap = new HashMap();
        Iterator ite = map.entrySet().iterator();
        while (ite.hasNext())
        {
            Map.Entry pair = (Map.Entry) ite.next();
            
            String key = pair.getKey().toString() ;
            
            if(!newMap.containsKey(key)){
                newMap.put(key,pair.getValue());
            }else{
                ArrayList list = (ArrayList)pair.getValue();
                ArrayList existingList = (ArrayList)newMap.get(key);
                existingList.addAll(list);
            }
        }
        
        
        StringBuilder strb = new StringBuilder("");
        ite = newMap.entrySet().iterator();
        while (ite.hasNext())
        {
            Map.Entry pair = (Map.Entry) ite.next();
            strb.append(pair.getKey());
            strb.append("[");
            strb.append(getSNMPPortString(pair.getValue()));
            strb.append("];");
        }
        return strb.toString();
    }
    
    private Object getSNMPPortString(Object value)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("snmp(");
        if(value instanceof ArrayList){
            ArrayList<Integer> portList = (ArrayList<Integer>) value; 
            int listLen = portList.size();
            int index = 0;
            for(Integer val : portList){
                sb.append(val);
                index++;
                if(index < listLen)
                    sb.append(",");
            }
            sb.append(")");
        }
        else
            return value;
        
        return sb.toString();
    }
    
    
    @Managed(description="Client port with ip address", name="Client Port" )
    @Override
    public String getClientPort()
    {
        return  getSnmpPort(ServerOperations.ClientPort);
    }
    
    
  
    //<editor-fold defaultstate="collapsed" desc="System Counters">
    @Managed(description="Total CPU consumed by TayzGrid Server process. ",name="Total CPU Usage")
    @Override
    public final double getSystemCpuUsage()
    {
        return getServerCounter(ServerOperations.SystemCpuUsage);
    }

    @Managed(description="Available free memory for TayzGrid Server process. ",name="Total Free Physical Memory")
    @Override
    public final double getSystemFreeMemory()
    {
        return getServerCounter(ServerOperations.SystemFreeMemory);
    }

    @Managed(description="Total memory used by TayzGrid Server process. ",name="Total Memory Usage")
    @Override
    public final double getSystemMemoryUsage()
    {
        return getServerCounter(ServerOperations.SystemMemoryUsage);
    }

    @Managed(description="CPU consumed by TayzGrid process",name="TayzGrid CPU Usage")
    @Override
    public final double getVMCpuUsage()
    {
        return getServerCounter(ServerOperations.VMCpuUsage);
    }

    @Managed(description="Memory available to TayzGrid process. ",name="TayzGrid Available Memory")
    @Override
    public final double getVMCommittedMemory()
    {
        return getServerCounter(ServerOperations.VMCommittedMemory);
    }

    @Managed(description="Maximum memory occupied by TayzGrid process.",name="TayzGrid Max Memory")
    @Override
    public final double getVMMaxMemory()
    {
        return getServerCounter(ServerOperations.VMMaxMemory);
    }

   @Managed(description="Total memory used by TayzGrid process. ",name="TayzGrid Memory Usage")
    @Override
    public final double getVMMemroyUsage()
    {
        return getServerCounter(ServerOperations.VMMemoryUsage);
    }
    //</editor-fold>
}
