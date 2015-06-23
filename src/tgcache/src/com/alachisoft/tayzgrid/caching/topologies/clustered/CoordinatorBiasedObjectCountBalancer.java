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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.caching.statistics.NodeStatus;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.logger.ILogger;

/**
 * Does the load balancing based upon the number of objects contained at each
 * node.
 */
public class CoordinatorBiasedObjectCountBalancer implements IActivityDistributor {

    private String loggerName;
    private ILogger _ncacheLog;

    private ILogger getCacheLog() {
        return _ncacheLog;
    }

    public CoordinatorBiasedObjectCountBalancer(CacheRuntimeContext context) {
        _ncacheLog = context.getCacheLog();
    }

    /**
     * Returns an ordered list of nodes, based upon the preferred order of load
     * balancing algorithm.
     *
     * @return ordered list of server nodes
     */
    public NodeInfo SelectNode(ClusterCacheStatistics clusterStats, Object data) {
        java.util.List memberInfos = clusterStats.getNodes();
        String group = (String) ((data instanceof String) ? data : null);
        boolean gpAfStrict = false;
        NodeInfo min = null;
        NodeInfo gMin = null;
        NodeInfo sMin = null;

        synchronized (memberInfos) {
            if (group != null) {
                gpAfStrict = clusterStats.getClusterDataAffinity() != null ? clusterStats.getClusterDataAffinity().contains(group) : false;
            }

            for (int i = 0; i < memberInfos.size(); i++) {
                NodeInfo curr = (NodeInfo) memberInfos.get(i);
                if (curr.getStatus().IsAnyBitSet((byte) (NodeStatus.Coordinator | NodeStatus.SubCoordinator))) {
                    if (curr.getStatistics() == null) {
                        continue;
                    }
                    if (min == null || (curr.getStatistics().getCount() < min.getStatistics().getCount())) {
                        min = curr;
                    }
                    if (curr.getDataAffinity() != null) {
                        if (curr.getDataAffinity().IsExists(group)) {
                            if (gMin == null || (curr.getStatistics().getCount() < gMin.getStatistics().getCount())) {
                                gMin = (NodeInfo) memberInfos.get(i);
                            }
                        } else if (curr.getDataAffinity().getStrict() == false) {
                            sMin = min;
                        } else {
                            min = sMin;
                        }
                    } else {
                        sMin = min;
                    }
                }
            }
        }
        if (gpAfStrict && gMin == null) {
            if (getCacheLog().getIsInfoEnabled()) {
                getCacheLog().Info("CoordinatorBiasedObjectCountBalancer.SelectNode", "strict group affinity, no node found to accomodate " + group + " data");
            }
            return null;
        }
        return (gMin == null) ? sMin : gMin;
    }
}