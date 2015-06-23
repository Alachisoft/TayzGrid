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
import com.alachisoft.tayzgrid.common.caching.statistics.operations.CacheOperations;
import org.weakref.jmx.Managed;

public class ClusteredCacheMonitor extends CacheMonitor implements ClusteredCacheMonitorMBean
{
    public ClusteredCacheMonitor(String nodeName)
    {
        super(nodeName);
    }

    public ClusteredCacheMonitor(String nodeName, ILogger logger, int port)
    {
        super(nodeName, logger, port);
    }

    @Managed(description="Number of clustered operations performed per second.", name ="Cluster ops/sec")
    @Override
    public double getClusterOperationsPerSec()
    {
        return getCounter(CacheOperations.ClusterOperationsPerSec);
    }
  }
