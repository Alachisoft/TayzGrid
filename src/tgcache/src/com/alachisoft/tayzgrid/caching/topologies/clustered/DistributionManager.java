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

import com.alachisoft.tayzgrid.caching.util.MiscUtil;
import com.alachisoft.tayzgrid.caching.statistics.BucketStatistics;
import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.common.util.ReaderWriterLock;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.datastructures.DistributionMaps;
import com.alachisoft.tayzgrid.common.datastructures.HashMapBucket;
import com.alachisoft.tayzgrid.common.datastructures.BucketStatus;
import com.alachisoft.tayzgrid.common.datastructures.DistributionInfoData;
import com.alachisoft.tayzgrid.common.AppUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class DistributionManager {

    public static class CandidateNodeForLoadBalance implements java.lang.Comparable {

        private Address _node;
        private int _percentageAboveAvg;

        public CandidateNodeForLoadBalance(Address node, int percentageAboveAvg) {
            _node = node;
            _percentageAboveAvg = percentageAboveAvg;
        }

        public final Address getNode() {
            return _node;
        }

        public final int getPercentageAboveAverage() {
            return _percentageAboveAvg;
        }

        public final int compareTo(Object obj) {
            if (obj != null && obj instanceof CandidateNodeForLoadBalance) {
                CandidateNodeForLoadBalance other = (CandidateNodeForLoadBalance) ((obj instanceof CandidateNodeForLoadBalance) ? obj : null);
                if (other._percentageAboveAvg > this._percentageAboveAvg) {
                    return 1;
                } else if (other._percentageAboveAvg < _percentageAboveAvg) {
                    return -1;
                } else {
                    return 0;
                }

            }
            return 0;
        }
    }
    protected java.util.ArrayList _installedHashMap;
    protected java.util.ArrayList _lastCreatedHashMap;
    protected java.util.HashMap _bucketsOwnershipMap;
    protected java.util.HashMap _bucketsStats;
    protected java.util.ArrayList _existingMembers;
    protected Address _newMember;
    protected int _bucketSize;
    protected int _memberCount = 0;
    protected ReaderWriterLock _sync = new ReaderWriterLock();
    protected Address _localAddrss;
    protected int _autoBalancingThreshold;
    public static int TotalBuckets = 1000;
    private ILogger _ncacheLog;

    public final ILogger getCacheLog() {
        return _ncacheLog;
    }

    public final void setNCacheLog(ILogger value) {
        _ncacheLog = value;
    }
    protected long _cacheSizePerNode;
    private Object _status_wait_mutex = new Object();

    public DistributionManager(int autoBalancingThreshold, long cacheSizePerNode) {
        Initialize();
        _existingMembers = new java.util.ArrayList();
        _autoBalancingThreshold = autoBalancingThreshold;
        _cacheSizePerNode = cacheSizePerNode;
    }

    private void Initialize() {
        long intRange = Integer.MIN_VALUE;
        _bucketSize = (int) Math.ceil((double) (intRange * -1) / (double) 1000);
    }

    /**
     * Gets/sets the local address.
     */
    public final Address getLocalAddress() {
        return _localAddrss;
    }

    public final void setLocalAddress(Address value) {
        _localAddrss = value;
    }

    public final java.util.ArrayList getMembers() {
        return _existingMembers;
    }

    public final int getBucketSize() {
        return _bucketSize;
    }

    public final void setBucketSize(int value) {
        _bucketSize = value;
    }

    public final java.util.ArrayList getInstalledHashMap() {
        return _installedHashMap;
    }

    public final void setInstalledHashMap(java.util.ArrayList value) {
        _installedHashMap = value;
    }

    public final java.util.ArrayList getLastCreatedHashMap() {
        return _lastCreatedHashMap;
    }

    public final void setLastCreatedHashMap(java.util.ArrayList value) {
        _lastCreatedHashMap = value;
    }

    public final java.util.HashMap getBucketStats() {
        return _bucketsStats;
    }

    public final void setBucketStats(java.util.HashMap value) {
        _bucketsStats = value;
    }

    /**
     * returns the Total data size contained by the cluster
     */
    public final long getTotalDataSize() {
        long size = 0;
        try {
            getSync().AcquireReaderLock();
            if (getBucketStats() != null && getBucketStats().size() > 0) {
                Iterator ide = getBucketStats().entrySet().iterator();
                Map.Entry KeyValue;
                while (ide.hasNext()) {
                    KeyValue = (Map.Entry) ide.next();
                    Object Key = KeyValue.getKey();
                    Object Value = KeyValue.getValue();
                    BucketStatistics stats = (BucketStatistics) Value;
                    size += stats.getDataSize();
                }
            }
            return size;
        } finally {
            getSync().ReleaseReaderLock();
        }
    }

    /**
     * returns the total data size / number of nodes
     */
    public final long getAvgDataSize() {
        try {
            getSync().AcquireReaderLock();
            if (_existingMembers != null && _existingMembers.size() > 0) {
                return getTotalDataSize() / _existingMembers.size();
            }
            return 0;
        } finally {
            getSync().ReleaseReaderLock();
        }
    }

    public final java.util.HashMap getDataSizePerNode() {
        java.util.HashMap tmp = new java.util.HashMap();
        try {
            getSync().AcquireReaderLock();

            java.util.Iterator ie = null;
            if (_lastCreatedHashMap != null) {
                ie = _lastCreatedHashMap.iterator();
            } else if (_installedHashMap != null) {
                ie = getInstalledHashMap().iterator();
            }

            if (ie != null) {
                while (ie.hasNext()) {
                    HashMapBucket bucket = (HashMapBucket) ie.next();
                    if (tmp.containsKey(bucket.getTempAddress())) {
                        long size = (Long) tmp.get(bucket.getTempAddress());
                        size += ((BucketStatistics) getBucketStats().get(bucket.getBucketId())).getDataSize();
                        tmp.put(bucket.getTempAddress(), size);
                    } else {
                        long size = ((BucketStatistics) getBucketStats().get(bucket.getBucketId())).getDataSize();
                        tmp.put(bucket.getTempAddress(), size);
                    }
                }
            }
            return tmp;
        } finally {
            getSync().ReleaseReaderLock();
        }
    }

    /**
     * returns the list of nodes that are getting more data than the average
     */
    public final java.util.ArrayList getCandidateNodesForBalance() {
        ArrayList candidateNodes = new ArrayList();
        Iterator ide = getDataSizePerNode().entrySet().iterator();
        StringBuilder sb = new StringBuilder();
        sb.append("loadbalancing condition : AvgDataSize (MB): " + MiscUtil.ConvertToMegaBytes(getAvgDataSize()));
        Map.Entry KeyValue;
        while (ide.hasNext()) {
            KeyValue = (Map.Entry) ide.next();
            Object Key = KeyValue.getKey();
            Object Value = KeyValue.getValue();
            Address owner = (Address) ((Key instanceof Address) ? Key : null);
            long size = (Long) Value;
            sb.append(" [" + owner + " = " + MiscUtil.ConvertToMegaBytes(size) + " (MB) ]");

            if (size > getAvgDataSize()) {
                long difference = size - getAvgDataSize();
                int percentageAboveAvg = GetPercentAboveAvg(difference);
                if (percentageAboveAvg > _autoBalancingThreshold) {
                    candidateNodes.add(new CandidateNodeForLoadBalance(owner, percentageAboveAvg));
                }
            }
        }
        Collections.sort(candidateNodes);
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("DistributionMgr.CandidateNodesForBalance", sb.toString());
        }
        return candidateNodes;

    }

    public final ReaderWriterLock getSync() {
        return _sync;
    }

    public final java.util.HashMap getBucketsOwnershipMap() {
        return _bucketsOwnershipMap;
    }

    public final void setBucketsOwnershipMap(java.util.HashMap value) {
        _bucketsOwnershipMap = value;
    }

    /**
     * This methods calculates in percentage how much data size on this node is
     * above the average data size...
     *
     * @param difference DataSizeOnThisNode - AverageDataSize
     * @return
     */
    private int GetPercentAboveAvg(long difference) {
        long cent = difference * 100;
        long percent = cent / getAvgDataSize();
        return (int) percent;
    }

    public final boolean IsBucketFunctional(Address owner, Object key) {
        int bucketId = GetBucketId(key);
        getSync().AcquireReaderLock();
        try {
            if (getBucketsOwnershipMap() != null && getBucketsOwnershipMap().containsKey(owner)) {

                Object tempVar = getBucketsOwnershipMap().get(owner);
                java.util.ArrayList buckets = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
                if (buckets != null) {
                    int index = buckets.indexOf(new HashMapBucket(null, bucketId));
                    if (index != -1) {
                        HashMapBucket bucket = (HashMapBucket) ((buckets.get(index) instanceof HashMapBucket) ? buckets.get(index) : null);
                        if (bucket.getStatus() == BucketStatus.Functional) {
                            return true;
                        }
                    }
                }
            }
        } finally {
            getSync().ReleaseReaderLock();
        }
        return false;
    }

    public final Address SelectNode(Object key) {
        int hashCode = AppUtil.hashCode(key);
        
        //+Numan Hanif: Fix for 7419
        //int index = hashCode / this.getBucketSize();
        int index = hashCode % AppUtil.TotalBuckets;
        //+Numan Hanif

        if (index < 0) {
            index = index * -1;
        }

        _sync.AcquireReaderLock();

        try {
            if (_installedHashMap != null) {
                HashMapBucket bucket = (HashMapBucket) ((_installedHashMap.get(index) instanceof HashMapBucket) ? _installedHashMap.get(index) : null);
                if (bucket != null) {
                    {
                        /**
                         * This is special case that handles operations during
                         * stateTransfer. If a bucket is not yet transfered to
                         * the new coordinator from the replica. then the
                         * replica's address is returned.
                         */
                        Address coordinatorNodeAddress = bucket.getTempAddress(); // this should be the sub-coordinator addres
                        java.util.ArrayList ownershipMap = (java.util.ArrayList) ((_bucketsOwnershipMap.get(coordinatorNodeAddress) instanceof java.util.ArrayList) ? _bucketsOwnershipMap.get(coordinatorNodeAddress) : null);
                        if (ownershipMap != null) {
                            int indexOfOwnedBucket = ownershipMap.indexOf(bucket);

                            if (indexOfOwnedBucket != -1) {
                                HashMapBucket ownedBucket = (HashMapBucket) ((ownershipMap.get(indexOfOwnedBucket) instanceof HashMapBucket) ? ownershipMap.get(indexOfOwnedBucket) : null);
                                return ownedBucket.getPermanentAddress();
                            }
                        } else {
                            return bucket.getTempAddress();
                        }
                    }
                    return bucket.getPermanentAddress();
                }
            }
            return null;
        } finally {
            _sync.ReleaseReaderLock();
        }

    }

    public final Address SelectNode(Object key, String group) {
        int hashCode = AppUtil.hashCode(key);
        
        //+Numan Hanif: Fix for 7419
        //int index = hashCode / this.getBucketSize();
        int index = hashCode % AppUtil.TotalBuckets;
        //+Numan Hanif
        
        if (index < 0) {
            index = index * -1;
        }

        _sync.AcquireReaderLock();

        try {
            if (_installedHashMap != null) {
                HashMapBucket bucket = (HashMapBucket) ((_installedHashMap.get(index) instanceof HashMapBucket) ? _installedHashMap.get(index) : null);
                if (bucket != null) {
                    {
                        /**
                         * This is special case that handles operations during
                         * stateTransfer. If a bucket is not yet transfered to
                         * the new coordinator from the replica. then the
                         * replica's address is returned.
                         */
                        Address coordinatorNodeAddress = bucket.getTempAddress(); // this should be the sub-coordinator addres
                        java.util.ArrayList ownershipMap = (java.util.ArrayList) ((_bucketsOwnershipMap.get(coordinatorNodeAddress) instanceof java.util.ArrayList) ? _bucketsOwnershipMap.get(coordinatorNodeAddress) : null);
                        if (ownershipMap != null) {
                            int indexOfOwnedBucket = ownershipMap.indexOf(bucket);

                            if (indexOfOwnedBucket != -1) {
                                HashMapBucket ownedBucket = (HashMapBucket) ((ownershipMap.get(indexOfOwnedBucket) instanceof HashMapBucket) ? ownershipMap.get(indexOfOwnedBucket) : null);
                                return ownedBucket.getPermanentAddress();
                            }
                        } else {
                            return bucket.getTempAddress();
                        }
                    }
                    return bucket.getPermanentAddress();
                }
            }
            return null;
        } finally {
            _sync.ReleaseReaderLock();
        }
    }

    public final int GetBucketId(Object key) {
        int hashCode = AppUtil.hashCode(key);
        
        //+Numan Hanif: Fix for 7419
        //int index = hashCode / this.getBucketSize();
        int index = hashCode % AppUtil.TotalBuckets;
        //+Numan Hanif

        if (index < 0) {
            index = index * -1;
        }

        HashMapBucket bucket = (HashMapBucket) ((_installedHashMap.get(index) instanceof HashMapBucket) ? _installedHashMap.get(index) : null);
        return bucket.getBucketId();
    }

    /**
     * A new map is required when a member leaves or joins the cluster. This
     * method returns a new map based on the input paramameters.
     *
     * @param member Address of the member that has either left or joined the
     * cluster
     * @param isNew A flag. True if the member has joined otherwise false.
     * @return A new hashmap instance
     */
    public DistributionMaps GetMaps(DistributionInfoData distInfoData) {
        java.util.ArrayList tmpMap = null;
        java.util.HashMap bucketsOwnershipMap = null;
        java.util.ArrayList partitionNodes = new java.util.ArrayList();

        _sync.AcquireWriterLock();
        try {
            if (_installedHashMap == null) {
                tmpMap = new java.util.ArrayList(TotalBuckets);
                for (int i = 0; i < TotalBuckets; i++) {
                    HashMapBucket bucket = new HashMapBucket(distInfoData.getAffectedNode().getNodeAddress(), i, BucketStatus.Functional);
                    tmpMap.add(bucket);

                }

                _existingMembers.add(distInfoData.getAffectedNode().getNodeAddress());

                Object tempVar = tmpMap.clone();
                _lastCreatedHashMap = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);

                bucketsOwnershipMap = GetBucketsOwnershipMap(_lastCreatedHashMap);
                return new DistributionMaps(_lastCreatedHashMap, bucketsOwnershipMap);
            } else if (_lastCreatedHashMap == null) {
                Object tempVar2 = _installedHashMap.clone();
                _lastCreatedHashMap = (java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null);
            }

            switch (distInfoData.getClustActivity()) {
                case NodeJoin:
                    try {
                        return GetMapsOnNodeJoining(distInfoData);
                    } catch (Exception e) {
                        if (getCacheLog().getIsErrorEnabled()) {
                            getCacheLog().Error("DistributionMgr.GetMaps()", e.toString());
                        }
                        break;
                    }

                case NodeLeave:

                    try {
                        return GetMapsOnNodeLeaving(distInfoData);
                    } catch (Exception e) {
                        if (getCacheLog().getIsErrorEnabled()) {
                            getCacheLog().Error("DistributionMgr.GetMaps()", e.toString());
                        }
                        break;
                    }

                case None:
                    BalanceNodeMgr bnMgr = new BalanceNodeMgr(null);
                    DistributionMaps result = bnMgr.BalanceNodes(distInfoData, _lastCreatedHashMap, _bucketsStats, _existingMembers);
                    if (result.getHashmap() != null) {
                        Object tempVar3 = result.getHashmap().clone();
                        _lastCreatedHashMap = (java.util.ArrayList) ((tempVar3 instanceof java.util.ArrayList) ? tempVar3 : null);
                        result.setBucketsOwnershipMap(GetBucketsOwnershipMap(_lastCreatedHashMap));
                    }
                    return result;

                default:
                    break;
            }
        } finally {
            _sync.ReleaseWriterLock();
        }
        return null;
    }

    protected final DistributionMaps GetMapsOnNodeJoining(DistributionInfoData distInfoData) {
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("DistributionMgr.GetMapsOnNodeJoining()", "Total Data Size : " + (new Long(getTotalDataSize())).toString());
        }
        java.util.ArrayList tmpMap = null;
        java.util.HashMap bucketsOwnershipMap = null;
        java.util.ArrayList partitionNodes = new java.util.ArrayList();

        java.util.ArrayList newHashMap = DistributeHashMap.BalanceBuckets(distInfoData, _lastCreatedHashMap, _bucketsStats, _existingMembers, _cacheSizePerNode, getCacheLog());
        _existingMembers.add(distInfoData.getAffectedNode().getNodeAddress());

        tmpMap = ChangeOwnerShip(newHashMap, distInfoData.getAffectedNode().getNodeAddress());

        Object tempVar = tmpMap.clone();
        _lastCreatedHashMap = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);

        bucketsOwnershipMap = GetBucketsOwnershipMap(_lastCreatedHashMap);
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("DistributionMgr.GetMaps()", "Sending new map as a new node joined the cluster");
        }
        return new DistributionMaps(_lastCreatedHashMap, bucketsOwnershipMap);
    }

    protected final DistributionMaps GetMapsOnNodeLeaving(DistributionInfoData distInfoData) {
        java.util.ArrayList tmpMap = null;
        java.util.HashMap bucketsOwnershipMap = null;
        _existingMembers.remove(distInfoData.getAffectedNode().getNodeAddress());
        if (getCacheLog().getIsInfoEnabled()) {
            getCacheLog().Info("DistributionMgr.GetMapsOnNodeLeaving()", "Before Calling DistributeOrphanBuckets() ---- Leaving Node:"
                    + distInfoData.getAffectedNode().getNodeAddress().toString() + " Existing Members Count:0" + _existingMembers.size());
        }
        tmpMap = DistributeHashMap.DistributeOrphanBuckets(_lastCreatedHashMap, distInfoData.getAffectedNode().getNodeAddress(), _existingMembers);

        if (tmpMap == null) {
            return null;
        }
        Object tempVar = tmpMap.clone();
        _lastCreatedHashMap = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);

        bucketsOwnershipMap = GetBucketsOwnershipMap(_lastCreatedHashMap);
        return new DistributionMaps(_lastCreatedHashMap, bucketsOwnershipMap);
    }

    protected final java.util.ArrayList ChangeOwnerShip(java.util.ArrayList affectedBuckets, Address newMember) {
        Object tempVar = _lastCreatedHashMap.clone();
        java.util.ArrayList tmpMap = _lastCreatedHashMap == null ? new java.util.ArrayList() : (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
        HashMapBucket bucket;
        for (int i = 0; i < affectedBuckets.size(); i++) {
            int buckId = (Integer) affectedBuckets.get(i);
            bucket = (HashMapBucket) tmpMap.get(buckId);
            bucket.setTempAddress(newMember);
            bucket.setStatus(BucketStatus.NeedTransfer);
        }

        return tmpMap;
    }

    public void OnMemberJoined(Address mbr, NodeIdentity identity) {
        if (_existingMembers != null && !_existingMembers.contains(mbr)) {
            _existingMembers.add(mbr);
        }
    }

    public void OnMemberLeft(Address mbr, NodeIdentity identity) {
        if (_existingMembers != null && _existingMembers.contains(mbr)) {
            _existingMembers.remove(mbr);
        }
    }

    /**
     *
     * @param hashMapList
     * @return
     */
    public java.util.HashMap GetBucketsOwnershipMap(java.util.ArrayList hashMapList) {
        java.util.HashMap bucketsOwnershipMap = new java.util.HashMap();

        _sync.AcquireReaderLock();

        try {
            if (hashMapList != null) {
                for (int i = 0; i < hashMapList.size(); i++) {
                    HashMapBucket bucket = (HashMapBucket) ((hashMapList.get(i) instanceof HashMapBucket) ? hashMapList.get(i) : null);

                    Address owner = bucket.getTempAddress() != null ? bucket.getTempAddress() : bucket.getPermanentAddress();

                    if (bucketsOwnershipMap.containsKey(owner)) {
                        java.util.ArrayList buckets = (java.util.ArrayList) ((bucketsOwnershipMap.get(owner) instanceof java.util.ArrayList) ? bucketsOwnershipMap.get(owner) : null);
                        if (!buckets.contains(bucket)) {
                            buckets.add(bucket.clone());
                        }
                    } else {
                        java.util.ArrayList buckets = new java.util.ArrayList();
                        buckets.add(bucket.clone());
                        bucketsOwnershipMap.put(owner, buckets);
                    }
                }
            }
        } finally {
            _sync.ReleaseReaderLock();
        }
        return bucketsOwnershipMap;
    }

    /**
     * Returns a table that has buckets owners and there respective hashcode
     * ranges
     *
     * @return a hashtable, with bucket id's as keys and owners address as value
     */
    public final java.util.HashMap GetOwnerHashMapTable(java.util.HashMap renderers) {
        java.util.HashMap ownerHashCodeTable = new java.util.HashMap(TotalBuckets);
        try {
            _sync.AcquireReaderLock();
            if (_installedHashMap != null) {
                for (int i = 0; i < _installedHashMap.size(); i++) {
                    HashMapBucket bucket = (HashMapBucket) ((_installedHashMap.get(i) instanceof HashMapBucket) ? _installedHashMap.get(i) : null);
                    if (bucket != null) {
                        switch (bucket.getStatus()) {
                            case BucketStatus.Functional:
                                ownerHashCodeTable.put(i, GetServerAddress(renderers, bucket.getPermanentAddress()));
                                break;
                            case BucketStatus.NeedTransfer:
                            case BucketStatus.UnderStateTxfr:
                                ownerHashCodeTable.put(i, GetServerAddress(renderers, bucket.getTempAddress()));
                                break;
                        }
                    }
                }
            }
        } finally {
            _sync.ReleaseReaderLock();
        }
        return ownerHashCodeTable;
    }

    private String GetServerAddress(java.util.HashMap renderers, Address clusterAddress) {
        String serverAddress = "";
        if (renderers != null) {
            if (renderers.containsKey(clusterAddress)) {
                if (((Address) renderers.get(clusterAddress)).getIpAddress() != null) {
                    serverAddress = ((Address) renderers.get(clusterAddress)).getIpAddress().getHostAddress();
                } else {
                    serverAddress = clusterAddress.getIpAddress().getHostAddress();
                }
            }
        }
        return serverAddress;
    }

    /**
     * Returns a table that has buckets owners and there respective hashcode
     * ranges
     *
     * @param bucketSize out parameter, holds the individual bucket size
     * @return a hashtable, with bucket id's as keys and owners address as value
     */
    public final java.util.HashMap GetOwnerHashMapTable(java.util.HashMap renderers, tangible.RefObject<Integer> bucketSize) 
    {
        //+Numan Hanif: Fix for 7419
        //bucketSize.argvalue = this.getBucketSize();
        bucketSize.argvalue = AppUtil.TotalBuckets;
        //+Numan Hanif


        return GetOwnerHashMapTable(renderers);
    }

    public final void UpdateBucketStats(NodeInfo localNode) {
        try {
            getSync().AcquireWriterLock();

            if (localNode == null) {
                return;
            }

            if (_bucketsStats == null) {
                _bucketsStats = new java.util.HashMap();
            }

            if (localNode.getStatistics() != null && localNode.getStatistics().getLocalBuckets() != null) {
                Object tempVar = localNode.getStatistics().getLocalBuckets().clone();
                java.util.HashMap bucketStats = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
                if (bucketStats != null) {
                    Iterator ide = bucketStats.entrySet().iterator();
                    Map.Entry KeyValue;
                    while (ide.hasNext()) {
                        KeyValue = (Map.Entry) ide.next();
                        Object Key = KeyValue.getKey();
                        Object Value = KeyValue.getValue();
                        //see if this node is the permanent owner of the bucket
                        //otherwise its quite possible that we override the
                        //stats of the bucket from the temporary owner.
                        HashMapBucket bucket = (HashMapBucket) _installedHashMap.get((Integer) Key);
                        if (bucket.getPermanentAddress().equals(localNode.getAddress())) {
                            BucketStatistics stats = (BucketStatistics) ((Value instanceof BucketStatistics) ? Value : null);
                            _bucketsStats.put(Key, Value);
                        } else {
                        }
                    }
                }

                if (getCacheLog().getIsInfoEnabled()) {
                    getCacheLog().Info("DistributionMgr.UpdateBucketStats()", "bucketStats = " + _bucketsStats == null ? "null" : "" + _bucketsStats.size());
                }
            }
        } catch (Exception e) {
            if (getCacheLog().getIsErrorEnabled()) {
                getCacheLog().Error("DistributionMgr.UpdateBucketStats()", e.toString());
            }
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    public final void WaitForBucketToBeFunctional(int bucket) {
        WaitForBucketStatus(bucket, BucketStatus.Functional);
    }

    protected void WaitForBucketStatus(int bucket, byte status) {
        HashMapBucket bkt = (HashMapBucket) ((_installedHashMap.get(bucket) instanceof HashMapBucket) ? _installedHashMap.get(bucket) : null);
        if (bkt != null) {
            bkt.getStateTxfrLatch().WaitForAny(status);
        }
    }

    public void WaitForBucketStatus(int bucket, byte status, Address owner) throws InterruptedException {
        if (owner != null) {
            while (true) {
                HashMapBucket hashBucket = GetBucketForWait(bucket, owner);

                if (hashBucket == null) {
                    return;
                }
                if (hashBucket.getStateTxfrLatch().IsAnyBitsSet(status) || !owner.equals(hashBucket.getTempAddress())) {
                    return;
                }
                synchronized (_status_wait_mutex) {
                    if (!owner.equals(hashBucket.getTempAddress()) || hashBucket.getStateTxfrLatch().IsAnyBitsSet(status)) {
                        return;
                    }
                    Monitor.wait(_status_wait_mutex);
                }
            }
        }
    }

    public void NotifyBucketUpdate() {
        synchronized (_status_wait_mutex) {
            Monitor.pulse(_status_wait_mutex);
        }
    }

    public HashMapBucket GetBucketForWait(int bucket, Address owner) {
        HashMapBucket bkt = (HashMapBucket) ((_installedHashMap.get(bucket) instanceof HashMapBucket) ? _installedHashMap.get(bucket) : null);
        return bkt;
    }

    public final java.util.ArrayList GetBucketIdOwnedbyMe() {
        java.util.ArrayList myBuckets = new java.util.ArrayList();
        _sync.AcquireReaderLock();
        try {
            Iterator ide = _bucketsOwnershipMap.entrySet().iterator();
            Map.Entry KeyValue;
            while (ide.hasNext()) {
                KeyValue = (Map.Entry) ide.next();
                Object Key = KeyValue.getKey();
                Object Value = KeyValue.getValue();
                java.util.ArrayList buckets = (java.util.ArrayList) ((Value instanceof java.util.ArrayList) ? Value : null);
                if (buckets != null) {
                    for (Iterator it = buckets.iterator(); it.hasNext();) {
                        HashMapBucket bucket = (HashMapBucket) it.next();
                        if (bucket.getTempAddress().equals(_localAddrss) || bucket.getPermanentAddress().equals(_localAddrss)) {
                            if (!myBuckets.contains(bucket.getBucketId())) {
                                myBuckets.add(bucket.getBucketId());
                            }
                        }
                    }
                }
            }

        } finally {
            _sync.ReleaseReaderLock();
        }
        return myBuckets;
    }

    public final java.util.ArrayList GetBucketsList(Address ofNode) {
        _sync.AcquireReaderLock();
        java.util.ArrayList myBuckets = null;
        try {

            if (ofNode != null) {
                myBuckets = (java.util.ArrayList) ((_bucketsOwnershipMap.get(ofNode) instanceof java.util.ArrayList) ? _bucketsOwnershipMap.get(ofNode) : null);
            }
            if (myBuckets != null) {
                Object tempVar = myBuckets.clone();
                myBuckets = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
            }
        } catch (NullPointerException e) {
        } finally {
            _sync.ReleaseReaderLock();
        }

        return myBuckets;
    }

    /**
     * Set the status of the bucket to state transfer and in this way this
     * bucket becomes locked. A locked bucket can not be assigned during
     * loadbalancing.
     *
     * @param buckets
     * @param node
     */
    public final void ChangeBucketStatusToStateTransfer(java.util.ArrayList buckets, Address node) {
        getSync().AcquireWriterLock();
        try {
            if (buckets != null) {
                java.util.Iterator ie = buckets.iterator();
                while (ie.hasNext()) {
                    synchronized (getInstalledHashMap()) {
                        HashMapBucket bucket = (HashMapBucket) getInstalledHashMap().get((Integer) ie.next());
                        if (node.equals(bucket.getTempAddress())) {
                            bucket.setStatus(BucketStatus.UnderStateTxfr);
                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("DistributionMgr.ChangeBucketStatus", bucket.toString());
                            }
                        }
                    }
                }

                if (_bucketsOwnershipMap != null) {
                    java.util.ArrayList nodeBuckets = (java.util.ArrayList) ((_bucketsOwnershipMap.get(node) instanceof java.util.ArrayList) ? _bucketsOwnershipMap.get(node) : null);
                    if (nodeBuckets != null) {
                        for (Iterator it = buckets.iterator(); it.hasNext();) {
                            int bucketId = (Integer) it.next();
                            int indexOfBucket = nodeBuckets.indexOf(new HashMapBucket(null, bucketId));
                            if (indexOfBucket != -1) {
                                HashMapBucket bucket = (HashMapBucket) ((nodeBuckets.get(indexOfBucket) instanceof HashMapBucket) ? nodeBuckets.get(indexOfBucket) : null);
                                if (node.equals(bucket.getTempAddress())) {
                                    bucket.setStatus(BucketStatus.UnderStateTxfr);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            getSync().ReleaseWriterLock();
        }
    }

    /**
     * Locks the buckets which are under the process of state transfer. A locked
     * bucket can not be assigned to a node while loadbalancing. Only a
     * coordinator node can lock the buckets.
     *
     * @param buckets
     * @param requestingNode
     * @return
     */
    public java.util.HashMap LockBuckets(java.util.ArrayList buckets, Address requestingNode) {
        java.util.ArrayList lockAcquired = new java.util.ArrayList();
        java.util.ArrayList ownerChanged = new java.util.ArrayList();

        java.util.HashMap result = new java.util.HashMap();
        getSync().AcquireWriterLock();
        try {
            if (buckets != null) {
                java.util.Iterator ie = buckets.iterator();
                while (ie.hasNext()) {
                    synchronized (getInstalledHashMap()) {
                        int next = (Integer) ie.next();
                        HashMapBucket bucket = (HashMapBucket) getInstalledHashMap().get(next);

                        if (requestingNode.equals(bucket.getTempAddress())) {
                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("DistributionMgr.lockbuckets", "acquired locked on bucket [" + bucket.getBucketId() + "] by " + requestingNode);
                            }

                            bucket.setStatus(BucketStatus.UnderStateTxfr);
                            if (!lockAcquired.contains(next)) {
                                lockAcquired.add(next);
                            }
                        } else if (!ownerChanged.contains(next)) {
                            if (getCacheLog().getIsInfoEnabled()) {
                                getCacheLog().Info("DistributionMgr.lockbuckets", "bucket [" + bucket.getBucketId() + "] owner ship is changed; new owner is "
                                        + bucket.getTempAddress());
                            }
                            ownerChanged.add(next);
                        }
                    }
                }
            }
        } catch (Exception e) {
            getCacheLog().Error("DistributionMgr.lockbuckets", e.toString());
            return result;
        } finally {
            getSync().ReleaseWriterLock();
        }
        result.put(BucketLockResult.OwnerChanged, ownerChanged);
        result.put(BucketLockResult.LockAcquired, lockAcquired);

        return result;
    }

    /**
     * Releases a bucket by setting its status again to functional. Only node
     * who has set its status to state trxfr can change its status.
     *
     * @param buckets
     * @param node
     */
    public final void ReleaseBuckets(java.util.ArrayList buckets, Address requestingNode) {
        try {
            getSync().AcquireWriterLock();
            try {
                if (buckets != null) {
                    java.util.Iterator ie = buckets.iterator();
                    while (ie.hasNext()) {
                        synchronized (getInstalledHashMap()) {
                            Object obj = ie.next();
                            HashMapBucket bucket = (HashMapBucket) getInstalledHashMap().get((Integer) obj);
                            if (requestingNode.equals(bucket.getTempAddress())) {
                                bucket.setStatus(BucketStatus.Functional);
                                //Change permnant address only when node who locked the bucket
                                //has sent request to release after he has transfered the bucket completely.
                                bucket.setPermanentAddress(bucket.getTempAddress());
                            }
                        }
                    }
                }

                if (_bucketsOwnershipMap != null) {
                    java.util.ArrayList nodeBuckets = (java.util.ArrayList) ((_bucketsOwnershipMap.get(requestingNode) instanceof java.util.ArrayList) ? _bucketsOwnershipMap.get(requestingNode) : null);
                    if (nodeBuckets != null) {
                        for (Iterator it = buckets.iterator(); it.hasNext();) {
                            int bucketId = (Integer) it.next();
                            int indexOfBucket = -1;
                            int startIndex = 0;

                            do {
                                //TempFix: Updates status for multipile occurances of the same bucket in ownership map for replica.
                                indexOfBucket = getIndex(nodeBuckets, new HashMapBucket(null, bucketId), startIndex, nodeBuckets.size());

                                if (indexOfBucket != -1) {
                                    HashMapBucket bucket = (HashMapBucket) ((nodeBuckets.get(indexOfBucket) instanceof HashMapBucket) ? nodeBuckets.get(indexOfBucket) : null);
                                    if (requestingNode.equals(bucket.getTempAddress())) {
                                        bucket.setStatus(BucketStatus.Functional);
                                        //Change permnant address only when node who locked the bucket
                                        //has sent request to release after he has transfered the bucket completely.
                                        bucket.setPermanentAddress(requestingNode);
                                    }
                                    startIndex = indexOfBucket + 1;
                                }
                            } while (indexOfBucket >= 0);
                        }
                    }
                }
            } finally {
                getSync().ReleaseWriterLock();
                NotifyBucketUpdate();
            }
        } catch (NullPointerException e) {
        } catch (Exception e) {
            getCacheLog().Error("DistributionMgr.ReleaseBuckets", e.toString());
        }
    }

    private int getIndex(java.util.ArrayList bucket, Object obj, int startIndex, int size) {
        for (int i = startIndex; i < size; i++) {
            if (obj.equals(bucket.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Verifies whether the given node is temorary owner of the bucket or not.
     *
     * @param bucketId
     * @param temOwner
     * @return
     */
    public boolean VerifyTemporaryOwnership(int bucketId, Address temOwner) {
        try {
            if (temOwner != null) {
                getSync().AcquireReaderLock();
                try {
                    synchronized (getInstalledHashMap()) {
                        HashMapBucket bucket = (HashMapBucket) getInstalledHashMap().get(bucketId);
                        return temOwner.equals(bucket.getTempAddress());
                    }

                } finally {
                    getSync().ReleaseReaderLock();
                }
            }
        } catch (NullPointerException e) {
            getCacheLog().Error("DistributionMgr.VerifyTemporaryOwnership", e.toString());
        } catch (Exception e) {
            getCacheLog().Error("DistributionMgr.VerifyTemporaryOwnership", e.toString());
        }
        return false;
    }

    /**
     * Verifies whether a given node is the permanent owner of the bucket or not
     *
     * @param bucketId
     * @param perOwner
     * @return
     */
    public final boolean VerifyPermanentOwnership(int bucketId, Address perOwner) {
        try {
            if (perOwner != null) {
                getSync().AcquireReaderLock();
                try {
                    synchronized (getInstalledHashMap()) {
                        HashMapBucket bucket = (HashMapBucket) getInstalledHashMap().get(bucketId);
                        return perOwner.equals(bucket.getTempAddress());
                    }

                } finally {
                    getSync().ReleaseReaderLock();
                }
            }
        } catch (Exception e) {
            getCacheLog().Error("DistributionMgr.VerifyPermanentOwnership", e.toString());
        }
        return false;
    }

    /**
     *
     *
     * @param newMap
     * @param newBucketsOwnershipMap
     * @param leftMbrs
     */
    public final void InstallHashMap(DistributionMaps distributionMaps, java.util.List leftMbrs) {
        java.util.ArrayList newMap = null;
        java.util.HashMap newBucketsOwnershipMap = null;

        _sync.AcquireWriterLock();
        try {
            if (distributionMaps == null) {
                return;
            }

            newMap = distributionMaps.getHashmap();
            newBucketsOwnershipMap = distributionMaps.getBucketsOwnershipMap();

            if (newMap == null || newBucketsOwnershipMap == null) {
                return;
            }

            if (_installedHashMap != null) {
                for (int i = 0; i < newMap.size(); i++) {
                    HashMapBucket newBucket = (HashMapBucket) newMap.get(i);
                    int index = _installedHashMap.indexOf(newBucket);
                    HashMapBucket oldBucket = (HashMapBucket) _installedHashMap.get(index);

                    if (!oldBucket.getPermanentAddress().equals(newBucket.getPermanentAddress()) && oldBucket.getTempAddress().equals(newBucket.getTempAddress())) {
                        getCacheLog().Error("Install Hasmap", "BucketID: " + (new Integer(index)).toString() + "\toldBucket: " + oldBucket.getPermanentAddress().toString()
                                + "\toldBucket.Temp: " + oldBucket.getTempAddress().toString() + "\tnewBucket: " + newBucket.getPermanentAddress().toString() + "\tnewBucekt.Temp: "
                                + newBucket.getTempAddress().toString());
                    } else {
                        //:
                        oldBucket.setPermanentAddress(newBucket.getPermanentAddress());
                        oldBucket.setTempAddress(newBucket.getTempAddress());
                        oldBucket.setStatus(newBucket.getStatus());
                        //oldBucket.NotifyBucketUpdate();
                    }

                }
            } else {
                _installedHashMap = newMap;
            }
            _bucketsOwnershipMap = newBucketsOwnershipMap;

            NotifyBucketUpdate();

            if (getCacheLog().getIsInfoEnabled()) {
                try {

                    if (newMap != null) {
                        StringBuilder sb = new StringBuilder();
                        getCacheLog().Info("DisMgr.Install", "---------------- HashMap (Begin)---------------------");
                        for (int i = 0; i < newMap.size(); i++) {
                            sb.append(" " + newMap.get(i).toString());
                            if ((i + 1) % 100 == 0) {
                                getCacheLog().Info("DisMgr.newMap", sb.toString());
                                sb.delete(0, sb.length());
                            }
                        }
                        getCacheLog().Info("DisMgr.Install", "---------------- HashMap (End)---------------------");
                    }
	            else
                    {
                         getCacheLog().Info("DistMgr.Install", "hash map is null");
                    }
                    HashMapBucket bkt;
                    if (newBucketsOwnershipMap != null) {
                        getCacheLog().Info("DisMgr.Install", "---------------- BucketOwnerShipMap (Begin)---------------------");
                        Iterator ide = newBucketsOwnershipMap.entrySet().iterator();
                        StringBuilder sb = new StringBuilder();
                        Map.Entry KeyValue;
                        while (ide.hasNext()) {
                            KeyValue = (Map.Entry) ide.next();
                            Object Key = KeyValue.getKey();
                            Object Value = KeyValue.getValue();
                            Address owner = (Address) ((Key instanceof Address) ? Key : null);
                            getCacheLog().Info("DisMgr.Install", "--- owner : " + owner + " ----");
                            java.util.ArrayList myMap = (java.util.ArrayList) ((Value instanceof java.util.ArrayList) ? Value : null);
                            int functionBkts = 0, bktsUnderTxfr = 0, bktsNeedTxfr = 0;

                            for (int i = 0; i < myMap.size(); i++) {
                                bkt = (HashMapBucket) ((myMap.get(i) instanceof HashMapBucket) ? myMap.get(i) : null);
                                switch (bkt.getStatus()) {
                                    case BucketStatus.Functional:
                                        functionBkts++;
                                        break;

                                    case BucketStatus.UnderStateTxfr:
                                        bktsUnderTxfr++;
                                        break;

                                    case BucketStatus.NeedTransfer:
                                        bktsNeedTxfr++;
                                        break;
                                }
                                sb.append("  ").append(bkt.toString());
                                if ((i + 1) % 100 == 0) {
                                    getCacheLog().Info("DisMgr.newBucketsOwnershipMap", sb.toString());
                                    sb.delete(0, sb.length());
                                }
                            }
                            if (sb.length() > 0) {
                                getCacheLog().Info("DisMgr.newBucketsOwnershipMap", sb.toString());
                                sb.delete(0, sb.length());
                            }
                           getCacheLog().Info("DisMgr.newBucketsOwnershipMap", "[" + owner + "]->" + " buckets owned :" + myMap.size() + "[ functional : " + functionBkts + " ; underStateTxfr : " + bktsUnderTxfr + " ; needTxfr :" + bktsNeedTxfr + " ]");
                        }
                        getCacheLog().Info("DisMgr.Install", "---------------- BucketOwnerShipMap (End)---------------------");
                    }
                    else{
                         getCacheLog().Info("DisMgr.newBucketsOwnershipMap", "bucket ownership map is null");
                    }
                } catch (Exception ex) {
                    getCacheLog().Error("print map", ex.toString());
                }
            }

        } finally {
            _sync.ReleaseWriterLock();
        }
    }

    public void Wait(Object key, String group) {
        if (key != null) {
            if (_installedHashMap != null) {
                int bucketId = GetBucketId(key);
                HashMapBucket bucket = (HashMapBucket) _installedHashMap.get(bucketId);
                bucket.getStateTxfrLatch().WaitForAny((byte) (BucketStatus.Functional | BucketStatus.NeedTransfer));
            } else {
                getCacheLog().Error("DistributionManager.Wait", "_installedHashMap == null");
            }
        }
    }

    public final void Set(int bucketId, String group) {
        if (_installedHashMap != null) {
            HashMapBucket bucket = (HashMapBucket) _installedHashMap.get(bucketId);
            bucket.getStateTxfrLatch().SetStatusBit(BucketStatus.Functional, BucketStatus.UnderStateTxfr);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (_installedHashMap != null) {
            for (int i = 0; i < _installedHashMap.size(); i++) {
                if (_installedHashMap.get(i) != null) {
                    sb.append("Key: " + (new Integer(i)).toString() + "\tValue: " + _installedHashMap.get(i).toString() + "\n");
                } else {
                    sb.append("Key: " + (new Integer(i)).toString() + "\tValue: NULL\n");
                }
            }
        }
        return sb.toString();
    }

    public final java.util.ArrayList GetPermanentAddress(java.util.List activePartitions) {
        java.util.ArrayList list = new java.util.ArrayList();
        _sync.AcquireReaderLock();
        try {
            for (Iterator it = activePartitions.iterator(); it.hasNext();) {
                Address server = (Address) it.next();
                java.util.ArrayList tempList = (java.util.ArrayList) ((_bucketsOwnershipMap.get(server) instanceof java.util.ArrayList) ? _bucketsOwnershipMap.get(server) : null);
                for (Iterator subit = tempList.iterator(); subit.hasNext();) {
                    HashMapBucket bucket = (HashMapBucket) subit.next();
                    Address address = bucket.getPermanentAddress();
                    if (!list.contains(address)) {
                        list.add(address.clone());
                    }
                }
                if (!list.contains(server)) {
                    list.add(server.clone());
                }
            }
        } finally {
            _sync.ReleaseReaderLock();
        }
        return list;
    }

    public final boolean InStateTransfer() {
        _sync.AcquireReaderLock();
        try {
            for (Iterator it = _installedHashMap.iterator(); it.hasNext();) {
                HashMapBucket bucket = (HashMapBucket) it.next();
                if (bucket.getStatus() == BucketStatus.UnderStateTxfr || bucket.getStatus() == BucketStatus.NeedTransfer) {
                    return true;
                }
            }
        } finally {
            _sync.ReleaseReaderLock();
        }
        return false;
    }
}
