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

import com.alachisoft.tayzgrid.common.threading.TimeScheduler;

/**
 * The periodic update task. Replicates cache stats to all nodes. _stats are
 * used for load balancing as well as statistics reporting.
 */
public class AutomaticDataLoadBalancer implements TimeScheduler.Task {

    /**
     * The parent on this task.
     */
    private ClusterCacheBase _parent = null;

    /**
     * The periodic interval for stat replications.
     */
    private long _period = 5000;

    /**
     * Constructor.
     *
     * @param parent
     */
    public AutomaticDataLoadBalancer(ClusterCacheBase parent) {
        _parent = parent;
    }

    /**
     * Overloaded Constructor.
     *
     * @param parent
     * @param period
     */
    public AutomaticDataLoadBalancer(ClusterCacheBase parent, long period) {
        _parent = parent;
        _period = period;
    }

    /**
     * Sets the cancel flag.
     */
    public final void Cancel() {
        synchronized (this) {
            _parent = null;
        }
    }

    /**
     * The task is cancelled or not.
     *
     * @return
     */
    @Override
    public boolean IsCancelled() {
        synchronized (this) {
            return _parent == null;
        }
    }

    /**
     *
     *
     * @return
     */
    @Override
    public long GetNextInterval() {
        return _period;
    }

    /**
     *
     */
    @Override
    public void Run() {
        if (_parent != null) {
            try {
                _parent.AutoLoadBalance();
            } catch (Exception e) {
                _parent.getContext().getCacheLog().Error("AutomaticLoadBalancer.Run", e.toString());
            }
        }
    }
}
