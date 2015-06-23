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
import com.alachisoft.tayzgrid.common.datastructures.BucketStatus;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;

public class StateTrxfrForReplicaTask extends StateTransferTask {

    protected DistributionManager _distMgr;

    public StateTrxfrForReplicaTask(ClusterCacheBase parent, DistributionManager distMgr, Address localAdd) {
        super(parent, localAdd);
        _distMgr = distMgr;
        _allowBulkInSparsedBuckets = false;
        _trasferType = StateTransferType.REPLICATE_DATA;
    }

    @Override
    protected String getName() {
        return "(" + _localAddress.toString() + ")StateTrxfrForReplicaTask";
    }

    @Override
    protected void PrepareBucketsForStateTxfr(java.util.ArrayList buckets) throws LockingException, StateTransferException, OperationFailedException, CacheException {
        if (_parent != null) {
            _parent.StartLogging(buckets);
            _parent.RemoveBucketData(buckets);
        }
    }

    @Override
    protected boolean getIsSyncReplica()
    {
        return true;
    }
    
    @Override
    protected void EndBucketsStateTxfr(java.util.ArrayList buckets) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        //in case, we need to transfer locally logged data
        //after all data has been fetched from other node...
        
    }

    @Override
    public BucketTxfrInfo GetBucketsForTxfr() {
        BucketTxfrInfo txfrInfo = super.GetBucketsForTxfr().clone();
        if (_distMgr != null && !txfrInfo.end) {
            if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                _parent.getContext().getCacheLog().Info(getName() + ".GetBucketsForTxfr", " Waiting for " + (Integer) txfrInfo.bucketIds.get(0) + "; Owner; "
                        + txfrInfo.owner.toString());
            }
            try {
                _distMgr.WaitForBucketStatus((Integer) txfrInfo.bucketIds.get(0), BucketStatus.Functional, txfrInfo.owner);
            } catch (InterruptedException interruptedException) {
            }

            if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                _parent.getContext().getCacheLog().Info(getName() + ".GetBucketsForTxfr", " State Changed of " + (Integer) txfrInfo.bucketIds.get(0) + "; Owner; "
                        + txfrInfo.owner.toString());
            }
        }
        return txfrInfo;
    }

    @Override
    protected java.util.HashMap AcquireLockOnBuckets(java.util.ArrayList buckets) {
        //In case of a replica node replicate a bucket from the source,it is not required
        //to get a proper lock. we simply simulate
        java.util.HashMap result = new java.util.HashMap();
        result.put(BucketLockResult.OwnerChanged, null);
        result.put(BucketLockResult.LockAcquired, buckets);
        return result;
    }

    @Override
    public void AcknowledgeStateTransferCompleted(Address owner, java.util.ArrayList buckets) {
        //no need to send an acknowlegement to the owner.
    }
}