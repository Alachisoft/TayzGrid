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

import com.alachisoft.tayzgrid.caching.statistics.BucketStatistics;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.datastructures.HashMapBucket;
import com.alachisoft.tayzgrid.common.datastructures.BucketStatus;
import java.util.Iterator;

public class DistributionData {

    private java.util.ArrayList _hashMapData;
    private java.util.ArrayList _distMatrixForNodes;
    private java.util.ArrayList _members;
    private long _cacheDataSum;
    private int _bucketsPerNode;
    private long _weightPerNode;

    public DistributionData(java.util.ArrayList hashMap, java.util.HashMap bucketStatistics, java.util.ArrayList members, ILogger NCacheLog, long cacheSizePerNode) {
        _members = members;
        int memberCount = _members.size();
        _hashMapData = new java.util.ArrayList(memberCount);
        _cacheDataSum = 1;

        java.util.ArrayList _weightIdList = new java.util.ArrayList();
        for (int i = 0; i < DistributionManager.TotalBuckets; i++) {
            HashMapBucket hmapBuck = (HashMapBucket) hashMap.get(i);
            BucketStatistics buckStats = (BucketStatistics) bucketStatistics.get(i);

            //Catering for situations when two nodes are balancing and a new node joins in OR
            // two nodes joins one after the other, first one started state transfer while second jumped in.
            if (hmapBuck.getStatus() != BucketStatus.UnderStateTxfr) { //include only those buckets that are Functional/NeedStateTr
                //We are selecting buckets based on temp address; although it is possible that these buckets
                //might have not been transfered to TEMP owner but algorithm consider these are owned by TEMP owner.
                WeightIdPair listItem = new WeightIdPair(hmapBuck.getBucketId(), buckStats.getDataSize(), hmapBuck.getTempAddress());
                _weightIdList.add(listItem);
            }

            _cacheDataSum += buckStats.getDataSize(); //Lets get the TOTAL weight of the cluster.
        }

        if (NCacheLog.getIsInfoEnabled()) {
            NCacheLog.Info("DistributionData()", "cacheDataSum = " + (new Long(_cacheDataSum)).toString());
        }

        //Initialize the two very important data pieces. All distribution is based on this.
        _bucketsPerNode = DistributionManager.TotalBuckets / (memberCount + 1);
        _weightPerNode = _cacheDataSum / (memberCount + 1);

        //
        _distMatrixForNodes = new java.util.ArrayList(memberCount);
        long maxCacheSize = cacheSizePerNode * memberCount; //in bytes..CacheSize/node is the one user has entered while creating the cluster
        for (Iterator it = _members.iterator(); it.hasNext();) {
            Address mbr = (Address) it.next();
            DistributionMatrix distMatrix = new DistributionMatrix(_weightIdList, mbr, this, NCacheLog);
            distMatrix.setMaxCacheSize(maxCacheSize);
            _distMatrixForNodes.add(distMatrix);
        }
    }

    public final java.util.ArrayList getDistributionMatrixForNodes() {
        return _distMatrixForNodes;
    }

    public final long getCacheDataSum() {
        return _cacheDataSum;
    }

    public final void setCacheDataSum(long value) {
        _cacheDataSum = value;
    }

    public final int getBucketsPerNode() {
        return _bucketsPerNode;
    }

    public final void setBucketsPerNode(int value) {
        _bucketsPerNode = value;
    }

    public final long getWeightPerNode() {
        return _weightPerNode;
    }

    public final void setWeightPerNode(long value) {
        _weightPerNode = value;
    }
}