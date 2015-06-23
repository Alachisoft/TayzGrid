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
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.datastructures.HashMapBucket;
import com.alachisoft.tayzgrid.common.datastructures.BucketStatus;
import com.alachisoft.tayzgrid.common.datastructures.DistributionMode;
import com.alachisoft.tayzgrid.common.datastructures.DistributionInfoData;
import java.util.Iterator;

public class DistributeHashMap {

    public static java.util.ArrayList DistributeOrphanBuckets(java.util.ArrayList hashMap, Address leavingNode, java.util.ArrayList members) {
        HashMapBucket tempBuck;
        java.util.ArrayList orphanBuckets = new java.util.ArrayList();
        hashMap = ReAllocBucketsInTransfer(hashMap, leavingNode);
        int[] bucketsInEachNode = NodeBucketsCount(hashMap, members); //node vs bucket count.
        boolean bAssigned = false;

        int memberCount = members.size();
        if (memberCount == 0) {
            return null;
        }

        int bucketsPerNode = hashMap.size() / members.size();

        for (int i = 0, j = 0; i < hashMap.size(); i++) {
            j = (j == memberCount) ? 0 : j;
            tempBuck = (HashMapBucket) hashMap.get(i);

            if (tempBuck.getPermanentAddress().compareTo(leavingNode) == 0) {
                bAssigned = false;
                for (int k = 0; k < memberCount; k++) {
                    if (bucketsInEachNode[j] < bucketsPerNode) {
                        Address mbr = (Address) ((members.get(j) instanceof Address) ? members.get(j) : null);
                        bucketsInEachNode[j] = (bucketsInEachNode[j])++; //increment bucket count as next j is incremented.
                        tempBuck.setPermanentAddress(mbr);
                        tempBuck.setTempAddress(mbr);
                        tempBuck.setStatus(BucketStatus.Functional);
                        j++;
                        bAssigned = true;
                        break;
                    } else {
                        j++;
                        j = (j == memberCount) ? 0 : j;
                    }

                }
                //exceptional case when last node gets few more buckets. Assign those leftover buckets to ANY node.
                if (bAssigned == false) {
                    tempBuck.setPermanentAddress((Address) members.get(j++));
                }

            }
        }

        return hashMap;
    }

    //While a node leaves, all those buckets that were in transfer of buckets to the leaving node, should sieze transfer and would
    // clear up tempAdd to be the same as the perm. addr.
    private static java.util.ArrayList ReAllocBucketsInTransfer(java.util.ArrayList hashMap, Address leavingNode) {
        for (int i = 0; i < hashMap.size(); i++) {
            if (((HashMapBucket) hashMap.get(i)).getTempAddress().compareTo(leavingNode) == 0) {
                ((HashMapBucket) hashMap.get(i)).setTempAddress(((HashMapBucket) hashMap.get(i)).getPermanentAddress());
            }
        }
        return hashMap;
    }

    //Returns int array of bucket-count against each member. This is to deal with cases when node leaves while transfer.
    //resulting very un-even bucket distribution over the cluster.
    private static int[] NodeBucketsCount(java.util.ArrayList hashMap, java.util.ArrayList members) {
        int[] _bucketsCount = new int[members.size()];

        for (int i = 0; i < members.size(); i++) {
            Address addr = (Address) members.get(i);
            int buckCount = 0;
            for (int j = 0; j < hashMap.size(); j++) {
                if (((HashMapBucket) hashMap.get(j)).getPermanentAddress().compareTo(addr) == 0) {
                    buckCount++;
                }
            }
            _bucketsCount[i] = buckCount;
        }
        return _bucketsCount;
    }

    public static java.util.ArrayList BalanceBuckets(DistributionInfoData distInfo, java.util.ArrayList hashMap, java.util.HashMap bucketStats, java.util.ArrayList members, long cacheSizePerNode, ILogger NCacheLog) {
        DistributionData distData = new DistributionData(hashMap, bucketStats, members, NCacheLog, cacheSizePerNode);
        boolean bShouldBalanceWeight = false;

        if (distInfo.getDistribMode() == DistributionMode.AvgWeightTime) { //If weight and time to move has to be avg. Cut the weight to half.
            if (NCacheLog.getIsInfoEnabled()) {
                NCacheLog.Info("DistributionImpl.BalanceBuckets()", "Request comes with DistributionMode.AvgWeightTime");
            }
            distData.setWeightPerNode(distData.getWeightPerNode() / 2);
        }

        java.util.ArrayList distMatrix = distData.getDistributionMatrixForNodes();
        java.util.ArrayList finalBuckets = new java.util.ArrayList();
        for (Iterator it = distMatrix.iterator(); it.hasNext();) {
            DistributionMatrix dMatrix = (DistributionMatrix) it.next();
            if (dMatrix.getDoWeightBalance() == true) {
                bShouldBalanceWeight = true;
                break;
            }
        }

        //If cluster is not loaded only shuffled disribution is required. No need to balance any weight.
        if (bShouldBalanceWeight == false) {
            if (NCacheLog.getIsInfoEnabled()) {
                NCacheLog.Info("DistributionImpl.BalanceBuckets()", "Cluster is not loaded only shuffled disribution is required. No need to balance any weight.");
            }
            distInfo.setDistribMode(DistributionMode.ShuffleBuckets);
        }

        //For cases below we also need to calculate Weight to be balanced along with buckets sacrifices.
        switch (distInfo.getDistribMode()) {
            case OptimalTime:
                for (Iterator it = distMatrix.iterator(); it.hasNext();) {
                    DistributionMatrix dMatrix = (DistributionMatrix) it.next();
                    int[][] IdMatrix = dMatrix.getIdMatrix();
                    for (int i = 0; i < dMatrix.getMatrixDimension().getCols(); i++) {
                        finalBuckets.add(IdMatrix[0][i]); //Always first row of the matrix to be given
                    }
                }
                if (NCacheLog.getIsInfoEnabled()) {
                    NCacheLog.Info("DistributionImpl.BalanceBuckets()", "Request is DistributionMode.OptimalTime");
                    NCacheLog.Info("Selected Buckets are: -");
                    for (int i = 0; i < finalBuckets.size(); i++) {
                        NCacheLog.Info(finalBuckets.get(i).toString());
                    }
                }
                return finalBuckets;

            case ShuffleBuckets:
                for (Iterator it = distMatrix.iterator(); it.hasNext();) {
                    DistributionMatrix dMatrix = (DistributionMatrix) it.next();
                    int[][] IdMatrix = dMatrix.getIdMatrix();
                    int[] resultIndices;
                    RowsBalanceResult rbResult = DistributionCore.ShuffleSelect(dMatrix);
                    resultIndices = rbResult.getResultIndicies();
                    for (int i = 0, j = 0; i < resultIndices.length; i++) {
                        int index = resultIndices[i]; //Index would never be zero, rather the value corresponding in the Matrix be zero.

                        //Get row and col on the basis of matrix index (index of one-D array).
                        int row = index / dMatrix.getMatrixDimension().getCols();
                        int col = index % dMatrix.getMatrixDimension().getCols();

                        if (IdMatrix[row][col] == -1) { //dealing with exceptional case when last row is selected and it got few non-indices.So replace those with lowest most indices in the matrix.
                            finalBuckets.add(IdMatrix[0][j]);
                            j++;
                        } else {
                            finalBuckets.add(IdMatrix[row][col]);
                        }
                    }
                }
                if (NCacheLog.getIsInfoEnabled()) {
                    NCacheLog.Info("DistributionImpl.BalanceBuckets()", "Request is DistributionMode.ShuffleBuckets");
                    NCacheLog.Info("Selected Buckets are: -");
                    for (int i = 0; i < finalBuckets.size(); i++) {
                        NCacheLog.Info(finalBuckets.get(i).toString());
                    }
                }
                return finalBuckets;

            case OptimalWeight: //For both same code works. Change is only in weight that is modified above . it is called FallThrough in switch statements.
            case AvgWeightTime:
                for (Iterator it = distMatrix.iterator(); it.hasNext();) {
                    DistributionMatrix dMatrix = (DistributionMatrix) it.next();
                    int[][] IdMatrix = dMatrix.getIdMatrix();
                    int[] resultIndices;
                    RowsBalanceResult rbResult = DistributionCore.CompareAndSelect(dMatrix);
                    resultIndices = rbResult.getResultIndicies();
                    for (int i = 0, j = 0; i < resultIndices.length; i++) {
                        int index = resultIndices[i]; //Index would never be zero, rather the value corresponding in the Matrix be zero.

                        //Get row and col on the basis of matrix index (index of one-D array).
                        int row = index / dMatrix.getMatrixDimension().getCols();
                        int col = index % dMatrix.getMatrixDimension().getCols();

                        if (IdMatrix[row][col] == -1) { //dealing with exceptional case when last row is selected and it got few non-indices.So replace those with lowest most indices in the matrix.
                            finalBuckets.add(IdMatrix[0][j]);
                            j++;
                        } else {
                            finalBuckets.add(IdMatrix[row][col]);
                        }
                    }
                }
                if (NCacheLog.getIsInfoEnabled()) {
                    NCacheLog.Info("DistributionImpl.BalanceBuckets()", "Request is DistributionMode.AvgWeightTime/ DistributionMode.OptimalWeight");
                    NCacheLog.Info("Selected Buckets are: -");
                    for (int i = 0; i < finalBuckets.size(); i++) {
                        NCacheLog.Info(finalBuckets.get(i).toString());
                    }
                }
                return finalBuckets;
            default:
                break;
        }
        return null;
    }
} 
