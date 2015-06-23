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
import com.alachisoft.tayzgrid.common.datastructures.DistributionMaps;
import com.alachisoft.tayzgrid.common.datastructures.BalancingResult;
import com.alachisoft.tayzgrid.common.datastructures.HashMapBucket;
import com.alachisoft.tayzgrid.common.datastructures.BucketStatus;
import com.alachisoft.tayzgrid.common.datastructures.DistributionInfoData;
import java.util.Collections;
import java.util.Iterator;

public class BalanceNodeMgr {

    private NodeBalanceData _nodeBalData;
    private long _weightToMove = 0;
    private BalanceDataForNode _primaryNode;
    private java.util.ArrayList _hashMap;
    private ClusterCacheBase _parent;

    public BalanceNodeMgr(ClusterCacheBase parent) {
        _parent = parent;
    }

    public final DistributionMaps BalanceNodes(DistributionInfoData distInfo, java.util.ArrayList hashMap, java.util.HashMap bucketStats, java.util.ArrayList members) {
        _hashMap = hashMap;
        _nodeBalData = new NodeBalanceData(hashMap, bucketStats, members);

        //Check if any other state transfer is not in progress...
        boolean bAllFunctional = this.SanityCheckForAllFunctional(hashMap);
        //Add some status saying that node balancing is not possible at the moment.
        if (!bAllFunctional) {
            DistributionMaps result = new DistributionMaps(BalancingResult.AlreadyInBalancing);
            return result;
        }

        //Check if really the node needs some balancing or not.
        boolean bShouldBalance = this.SanityCheckForCandidateNode((Address) distInfo.getAffectedNode().getNodeAddress());

        if (!bShouldBalance) {
            DistributionMaps result = new DistributionMaps(BalancingResult.NotRequired);
            return result;
        }

        java.util.ArrayList dataListForNodes = _nodeBalData.getBalanceDataListForNodes();
        java.util.ArrayList candidates = FilterCandidateNodes();
        for (Iterator it = candidates.iterator(); it.hasNext();) {
            AddressWeightPair awPair = (AddressWeightPair) it.next();
            BalanceDataForNode secNode = GetBalDataForNode(awPair.getNodeAddress());
            BalanceTwoNodes(_primaryNode, secNode, awPair.getWeightShare());
            ApplyChangesInHashMap(secNode);
        }
        ApplyChangesInHashMap(_primaryNode);
        return new DistributionMaps(_hashMap, null);

    }

    //data to be moved from Primary node to the secondary node.
    //As a result priNode and secNode got updated WeightIdPairLists.
    private static void BalanceTwoNodes(BalanceDataForNode priNode, BalanceDataForNode secNode, long dataToMove) {
        int priBucketCount = priNode.getItemsCount();
        int secBucketCount = secNode.getItemsCount();
        java.util.ArrayList priWIPairList = priNode.getWeightIdList();
        java.util.ArrayList secWIPairList = secNode.getWeightIdList();
        int cushionFactor = 10; // 10% cushion for balancing...   +- 10%
        long swapWeightGain = 0; // weight gain for this swap
        long cushionWeight = (long) ((double) (dataToMove * cushionFactor) / (double) 100);
        boolean bTargetAchieved = false; //loop-invariant, in case we need to exit the loop in middle.
        long movedSoFar = 0;
        java.util.ArrayList usedIndex = new java.util.ArrayList(); //this list would keep all those indicies related to Inner loop that are consumed/used in swap.

        //Making pivot node to be the secondary one, the one that needs to gain weight.
        //swapping or try to swap each element of secNode to all elements of priNode.
        //primary is traversed in Descending order, and secondary is traversed in ascending order.
        for (int i = 0; i < secBucketCount && !bTargetAchieved; i++) {
            WeightIdPair secWIPair = (WeightIdPair) secWIPairList.get(i);

            for (int j = priBucketCount - 1; j >= 0; j--) {
                WeightIdPair priWIPair = (WeightIdPair) priWIPairList.get(j);

                //only move when there is a gain.
                if (priWIPair.getWeight() > secWIPair.getWeight() && !usedIndex.contains(j)) {
                    swapWeightGain = priWIPair.getWeight() - secWIPair.getWeight();
                    movedSoFar += swapWeightGain;

                    if (movedSoFar <= dataToMove) {
                        if (dataToMove - movedSoFar <= cushionWeight) {
                            //swap the buckets and exit
                            secWIPairList.set(i, priWIPair);
                            priWIPairList.set(j, secWIPair);
                            bTargetAchieved = true;
                            break;
                        } else {
                            secWIPairList.set(i, priWIPair);
                            priWIPairList.set(j, secWIPair);
                            usedIndex.add(j);
                            break; //i need to move fwd now
                        }
                    } else { //end if
                        if (movedSoFar - dataToMove <= cushionWeight) {
                            //swap the buckets an exit
                            secWIPairList.set(i, priWIPair);
                            priWIPairList.set(j, secWIPair);
                            bTargetAchieved = true;
                            break;
                        } else {
                            movedSoFar -= swapWeightGain;
                        }

                    } //end else
                } //end if for priWeight > seWeight
            } //end inner for loop
        } //end outer for loop
        //re-assign the WeightIdPairList to respective BalanceDataForNode
        priNode.setWeightIdList(priWIPairList);
        Collections.sort(priNode.getWeightIdList());

        secNode.setWeightIdList(secWIPairList);
        Collections.sort(secNode.getWeightIdList());
    }

    private boolean SanityCheckForAllFunctional(java.util.ArrayList hashMap) {
        boolean bAllFunctional = true;
        for (Iterator it = hashMap.iterator(); it.hasNext();) {
            HashMapBucket hmBuck = (HashMapBucket) it.next();
            if (!hmBuck.getPermanentAddress().equals(hmBuck.getTempAddress())) {
                bAllFunctional = false;
                break;
            }
        }
        return bAllFunctional;
    }

    //Need to check if the source node really needs any balancing?. If the weight is more then the Avg weight/Node then its true else false.
    private boolean SanityCheckForCandidateNode(Address sourceNode) {
        java.util.ArrayList dataListForNodes = _nodeBalData.getBalanceDataListForNodes();
        for (Iterator it = dataListForNodes.iterator(); it.hasNext();) {
            BalanceDataForNode balData = (BalanceDataForNode) it.next();
            if (balData.getNodeAddress().equals(sourceNode)) {
                if (balData.getPercentData() > _nodeBalData.getPercentWeightPerNode()) {
                    this._weightToMove = balData.getTotalWeight() - this._nodeBalData.getWeightPerNode(); //Weight to move is the one that is above the Avg. weight the node Should bear.
                    _primaryNode = balData;
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false; //nothing found.
    }

    //returns list of those nodes that need to be participated in sharing load from Source node.
    private java.util.ArrayList FilterCandidateNodes() {
        java.util.ArrayList dataListForNodes = _nodeBalData.getBalanceDataListForNodes();
        java.util.ArrayList filteredNodes = new java.util.ArrayList();

        int totalPercentMissing = 0;
        int percentMissing = 0;
        int percentShareToGain = 0;
        for (Iterator it = dataListForNodes.iterator(); it.hasNext();) {
            BalanceDataForNode balData = (BalanceDataForNode) it.next();
            if (balData.getTotalWeight() < _nodeBalData.getWeightPerNode()) {
                totalPercentMissing += _nodeBalData.getPercentWeightPerNode() - balData.getPercentData();
            }
        }
        for (Iterator it = dataListForNodes.iterator(); it.hasNext();) {
            BalanceDataForNode balData = (BalanceDataForNode) it.next();
            if (balData.getTotalWeight() < _nodeBalData.getWeightPerNode()) {
                long weightToGain = 0;

                percentMissing = _nodeBalData.getPercentWeightPerNode() - balData.getPercentData();

                try {
                    percentShareToGain = (int) ((double) ((double) percentMissing / (double) totalPercentMissing) * 100);
                    weightToGain = (long) ((double) (percentShareToGain * this._weightToMove) / (double) 100);

                } catch (Exception e) {
                }

                AddressWeightPair awPair = new AddressWeightPair(balData.getNodeAddress(), weightToGain);
                filteredNodes.add(awPair);
            }
        }

        return filteredNodes;
    }

    //Returns BalancDataForNode instance for demanded node. from the list
    private BalanceDataForNode GetBalDataForNode(Address addr) {
        java.util.ArrayList dataListForNodes = _nodeBalData.getBalanceDataListForNodes();
        for (Iterator it = dataListForNodes.iterator(); it.hasNext();) {
            BalanceDataForNode balData = (BalanceDataForNode) it.next();
            if (balData.getNodeAddress().equals(addr)) {
                return balData;
            }
        }
        return null;
    }

    private void ApplyChangesInHashMap(BalanceDataForNode secNode) {
        java.util.ArrayList weightIdPair = secNode.getWeightIdList();
        Address newAddr = secNode.getNodeAddress();
        HashMapBucket bucket = null;
        for (Iterator it = weightIdPair.iterator(); it.hasNext();) {
            WeightIdPair widPair = (WeightIdPair) it.next();
            bucket = (HashMapBucket) _hashMap.get(widPair.getBucketId());
            if (!newAddr.equals(bucket.getTempAddress())) {
                bucket.setStatus(BucketStatus.NeedTransfer);
            }
            bucket.setTempAddress(newAddr);
        }
    }

    public static class AddressWeightPair {

        private Address _nodeAddr;
        private long _weightShare = 0;

        public AddressWeightPair(Address address, long weightShare) {
            _nodeAddr = address;
            _weightShare = weightShare;
        }

        public final Address getNodeAddress() {
            return _nodeAddr;
        }

        public final long getWeightShare() {
            return _weightShare;
        }

        public final void setWeightShare(long value) {
            _weightShare = value;
        }
    }
}