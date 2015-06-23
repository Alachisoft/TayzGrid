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
import com.alachisoft.tayzgrid.common.datastructures.DistributionMaps;
import com.alachisoft.tayzgrid.common.datastructures.DistributionInfoData;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.io.IOException;

public interface IDistributionPolicyMember {

    /**
     * Called on the coordinator node when the view changes. It returns the new
     * hashmap.
     *
     * @param member
     * @param isNew
     * @return
     */
    DistributionMaps GetDistributionMaps(DistributionInfoData distInfo);

    java.util.ArrayList getHashMap();

    void setHashMap(java.util.ArrayList value);

    java.util.HashMap getBucketsOwnershipMap();

    void setBucketsOwnershipMap(java.util.HashMap value);

    void EmptyBucket(int bucketId) throws LockingException, StateTransferException, OperationFailedException, CacheException, IOException, ClassNotFoundException;

    void InstallHashMap(DistributionMaps distributionMaps, java.util.List leftMbrs);

    void InstallMirrorMap(CacheNode[] nodes);

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
}