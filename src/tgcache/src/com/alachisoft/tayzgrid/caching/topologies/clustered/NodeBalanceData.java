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
import com.alachisoft.tayzgrid.common.datastructures.BucketStatus;
import com.alachisoft.tayzgrid.common.datastructures.HashMapBucket;
import java.util.Iterator;

public class NodeBalanceData {

    private java.util.ArrayList _hashMapData;
    private java.util.ArrayList _balanceDataListForNodes; 
    private java.util.ArrayList _members;
    private long _cacheDataSum;
    private long _weightPerNode;
    private int _percentWeightPerNode;

    public NodeBalanceData(java.util.ArrayList hashMap, java.util.HashMap bucketStatistics, java.util.ArrayList members) {
        _percentWeightPerNode = 100 / members.size();
        _members = members;
        int memberCount = _members.size();
        _hashMapData = new java.util.ArrayList(memberCount);
        _cacheDataSum = 1;

        java.util.ArrayList _weightIdList = new java.util.ArrayList();
        for (int i = 0; i < DistributionManager.TotalBuckets; i++) {
            HashMapBucket hmapBuck = (HashMapBucket) hashMap.get(i);
            BucketStatistics buckStats = (BucketStatistics) bucketStatistics.get(i);
            if (hmapBuck.getStatus() != BucketStatus.UnderStateTxfr) { //include only those buckets that are Functional
                WeightIdPair listItem = new WeightIdPair(hmapBuck.getBucketId(), buckStats.getDataSize(), hmapBuck.getPermanentAddress());
                _weightIdList.add(listItem);
            }
            _cacheDataSum += buckStats.getDataSize(); //Lets get the TOTAL weight of the cluster.
        }

        _weightPerNode = _cacheDataSum / memberCount;

        _balanceDataListForNodes = new java.util.ArrayList(memberCount);
        for (Iterator it = _members.iterator(); it.hasNext();) {
            Address mbr = (Address) it.next();
            BalanceDataForNode balanceData = new BalanceDataForNode(_weightIdList, mbr, _cacheDataSum);
            _balanceDataListForNodes.add(balanceData);
        }
    }

    public final java.util.ArrayList getBalanceDataListForNodes() {
        return _balanceDataListForNodes;
    }

    public final long getCacheDataSum() {
        return _cacheDataSum;
    }

    public final long getWeightPerNode() {
        return _weightPerNode;
    }

    public final int getPercentWeightPerNode() {
        return _percentWeightPerNode;
    }
}
