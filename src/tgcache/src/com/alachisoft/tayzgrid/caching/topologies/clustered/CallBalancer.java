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

/**
 * A class that helps balancing calls across cluster members.
 */
public class CallBalancer implements IActivityDistributor {

    /**
     * Call balancing variable, index of the server last contacted.
     */
    private int _lastServ = 0;

    /**
     * Return the next node in call balacing order that is fully functional.
     *
     * @return
     */
    public NodeInfo SelectNode(ClusterCacheStatistics clusterStats, Object hint) {
        java.util.List memberInfos = clusterStats.getNodes();
        synchronized (memberInfos) {
            int maxtries = memberInfos.size();
            NodeInfo info = null;
            do {
                info = (NodeInfo) memberInfos.get(_lastServ % memberInfos.size());
                _lastServ = ++_lastServ % memberInfos.size();
                if (info.getStatus().IsAnyBitSet(NodeStatus.Running)) {
                    return info;
                }
                maxtries--;
            } while (maxtries > 0);
        }
        return null;
    }
}
