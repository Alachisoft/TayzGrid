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

package com.alachisoft.tayzgrid.event;

import java.util.EventListener;

/**
 * Cluster Listener listens for the events related to the Clsuter. like memberJoined, memberLeft events.
 */
public interface CacheStatusEventListener extends EventListener {

    /**
     * Defines a callback method for notifying applications when a node leaves
     * the cluster.
     * @param clusterEvent The details of the event.
     */
    public void memberJoined(ClusterEvent clusterEvent);

    /**
     * Defines a callback method for notifying applications when a node joins
     * the cluster.
     * @param clusterEvent The details of the event.
     */
    public void memberLeft(ClusterEvent clusterEvent);

    /**
     * Defines a callback method for notifying applications when cache on a
     * node is stopped.
     * @param clusterEvent The details of the event.
     */
    public void cacheStopped(ClusterEvent clusterEvent);
}
