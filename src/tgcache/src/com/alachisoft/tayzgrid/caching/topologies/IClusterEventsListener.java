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

package com.alachisoft.tayzgrid.caching.topologies;

/**
 * Events callback interface used by the listeners of Cluster events.
 */
public interface IClusterEventsListener {

    /**
     * Fired when a new node joins the cluster.
     *
     * @param clusterAddress The cluster IP and Port of the newly joined node
     * @param serverAddress The server IP and Port of the newly joined node
     */
    void OnMemberJoined(com.alachisoft.tayzgrid.common.net.Address clusterAddress, com.alachisoft.tayzgrid.common.net.Address serverAddress);

    /**
     * Fired when a node leaves the cluster.
     *
     * @param clusterAddress The cluster IP and Port of the leaving node
     * @param serverAddress The server IP and Port of the leaving node
     */
    void OnMemberLeft(com.alachisoft.tayzgrid.common.net.Address clusterAddress, com.alachisoft.tayzgrid.common.net.Address serverAddress);
}