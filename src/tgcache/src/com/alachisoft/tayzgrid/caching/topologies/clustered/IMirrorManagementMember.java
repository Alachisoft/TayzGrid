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

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.mirroring.CacheNode;

/**
 * Mirror Management Interface exposes the mirror management functionaly to the
 * cluster services layer. The Mirror Manager returns the map of cluster with
 * respect to the joining opr leaving of new node.
 */
public interface IMirrorManagementMember {

    /**
     * Updates the MirroMap with this new list of cacheNodes. The sequence Idis
     * automatically adjusted.
     *
     * @param cacheNodes List of CacheNodes
     */
    void UpdateMirrorMap(CacheNode[] cacheNodes);

    /**
     * Called on the coordinator node when the view changes. It returns the
     * mirror map. be sure to add the new node before retreiving the Map.
     *
     * @return Returns the object containg the array of CacheNodes
     * (<cref>CacheNode</cref>[]).
     *
     */
    CacheNode[] GetMirrorMap();

    /**
     * Gets the groupId for this node from the mirrorManager. If nodeIdentity is
     * a mirror cache instance then groupId is the ip/name of the group it
     * belongs to. Otherwise retruns Empty String. This only fetches the groupId
     * if there is no GroupId for this node then Empty String is returned.
     *
     * @param affectedNode The Address of the AffectedNode.
     * @param isStartedAsMirror Whether this node joined as Mirror.
     * @return GroupId of the node. If there is
     */
    String GetGroupId(Address affectedNode, boolean isStartedAsMirror);

    /**
     * Adds the new joinging node in the mirror manager. So mirror manager can
     * rearrange the mirrors. If this node is mirror then it only returns the
     * group_id of this mirror.
     *
     * @param affectedNode The new joining node.
     * @param isStartedAsMirror Whether this node joined as Mirror.
     * @return The groupId this node belongs to. If this is the new joinging
     * node and is not mirror then the GroupId would be its name or Ip.
     */
    String AddNode(Address affectedNode, boolean isStartedAsMirror);

    /**
     * Removes the node from the Mirror mapping. If it is a mirror instance then
     * the mirror mapping is not affected.
     *
     * @param affectedNode The affectedNode leaving the cluster.
     * @param isStartedAsMirror Whether this node joined as Mirror.
     * @return The groupId this node belond to. Empty String or null if this
     * node was not in teh mirror mapping.
     */
    String RemoveNode(Address affectedNode, boolean isStartedAsMirror);
}