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

import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.caching.statistics.operations.ServerOperations;
import java.util.EnumMap;
import org.weakref.jmx.Managed;

public class CacheServerMonitor extends ServerMonitor implements CacheServerMBean
{
    public CacheServerMonitor(String nodeName)
    {
        super(nodeName);
    }

    public CacheServerMonitor(String nodeName, ILogger logger)
    {
        super(nodeName, logger);
    }

    @Override
    public EnumMap getCounterStore()
    {
        return getServerCounterStore();
    }

    @Override
    public String getExportString()
    {
        return getExportStringPrefix() + "server";
    }
    
    @Managed(description="Number of bytes sent per second to other nodes of the cluster.",name="Bytes sent/sec")
    @Override
    public double getByteSentPerSec()
    {
        return getCounter(ServerOperations.byteSentPerSec);
    }
    
    
    @Managed(description="Number of bytes received per second from other nodes of the cluster.",name="Bytes received/sec")
    @Override
    public double getByteReceivePerSec()
    {
        return getCounter(ServerOperations.bytereceivePerSec);
    }
    
    @Managed(description="Number of items in event queue.",name="Event Queue Count")
    @Override
    public double getEventQueueCount()
    {
        return getCounter(ServerOperations.eventQueueCount);
    }
    
}
