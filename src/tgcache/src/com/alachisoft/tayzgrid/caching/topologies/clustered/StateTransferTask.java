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

import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.caching.statistics.BucketStatistics;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DataSourceReplicationManager;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.common.threading.Promise;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.datastructures.BucketStatus;
import com.alachisoft.tayzgrid.common.datastructures.HashMapBucket;
import com.alachisoft.tayzgrid.common.EventCategories;
import com.alachisoft.tayzgrid.common.EventID;
import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.logger.EventLogger;
import com.alachisoft.tayzgrid.common.threading.ThrottlingManager;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import sun.misc.GC;

/**
 * Asynchronous state tranfer job.
 */
public class StateTransferTask implements Runnable {

    @Override
    public void run() {
        this.Process();
    }
    /**
     * The partition base class
     */
    public ClusterCacheBase _parent = null;
    /**
     * A promise object to wait on.
     */
    protected Promise _promise = null;
    /**
     * 10K is the threshold data size. Above this threshold value, data will be
     * state transfered in chunks.
     */
    //in future we may need it back.
    protected long _threshold = 0; //10 * 1000;
    /**
     * All the buckets that has less than threshold data size are sparsed. This
     * is the list of sparsed bucket ids.
     */
    protected java.util.ArrayList _sparsedBuckets = new java.util.ArrayList();
    /**
     * All the buckets that has more than threshold data size are filled. This
     * is the list of the filled buckted ids.
     */
    protected java.util.ArrayList _filledBuckets = new java.util.ArrayList();
    protected boolean _isInStateTxfr = false;
    protected Thread _worker;
    protected boolean _isRunning;
    protected Object _stateTxfrMutex = new Object();
    protected Address _localAddress;
    protected int _bktTxfrRetryCount = 3;
    protected java.util.ArrayList _correspondingNodes = new java.util.ArrayList();
    /**
     * Flag which determines that if sparsed buckets are to be transferred in
     * bulk or not.
     */
    protected boolean _allowBulkInSparsedBuckets = true;
    protected byte _trasferType = StateTransferType.MOVE_DATA;
    protected boolean _logStateTransferEvent;
    protected boolean _stateTransferEventLogged;
    /**
     * Gets or sets a value indicating whether this task is for Balancing Data
     * Load or State Transfer.
     */
    protected boolean _isBalanceDataLoad = false;

    java.util.ArrayList _failedKeysList = null;

    private static long MB = 1024 * 1024;
    protected long stateTxfrDataSizePerSecond = MB;
    TimeSpan _interval = new TimeSpan(0, 0, 1);

    private ThrottlingManager _throttlingManager;
    private boolean _enableGC;
    private long _gcThreshHold = 1024 * MB * 2;
    private long _dataTransferred;

    private Object _updateIdMutex = new Object();
    int updateCount;

    /**
     * Constructor
     */
    protected StateTransferTask() {
        _promise = new Promise();
    }

    protected String getName() {
        return "(" + _localAddress.toString() + ")StateTransferTask";
    }

    /**
     * Constructor
     *
     * @param parent
     */
    public StateTransferTask(ClusterCacheBase parent, Address localAdd) {
        _parent = parent;
        _promise = new Promise();
        _localAddress = localAdd;

        if (ServicePropValues.STATE_TRANSFER_DATA_SIZE_PER_SEC != null && !ServicePropValues.STATE_TRANSFER_DATA_SIZE_PER_SEC.isEmpty()) {
            try {
                float result = 0;
                result = Float.parseFloat(ServicePropValues.STATE_TRANSFER_DATA_SIZE_PER_SEC);
                if (result > 0) {
                    stateTxfrDataSizePerSecond = (long) (result * MB);
                }
            } catch (Exception ex) {
                _parent.getCacheLog().Error(this.getName(), "Invalid value specified for NCacheServer.StateTransferDataSizePerSecond.");
            }
        }

        if (ServicePropValues.ENABLE_GC_DURING_STATE_TRANSFER != null && !ServicePropValues.ENABLE_GC_DURING_STATE_TRANSFER.isEmpty()) {
            try {
                boolean result = false;
                result = Boolean.valueOf(ServicePropValues.ENABLE_GC_DURING_STATE_TRANSFER);
                _enableGC = result;
            } catch (Exception ex) {
                _parent.getCacheLog().Error(this.getName(), "Invalid value specified for NCacheServer.EnableGCDuringStateTransfer.");
            }
        }
        if (ServicePropValues.GC_THRESHOLD != null && !ServicePropValues.GC_THRESHOLD.isEmpty()) {
            try {
                long result = _gcThreshHold;
                result = Long.parseLong(ServicePropValues.GC_THRESHOLD);
                _gcThreshHold = result;
            } catch (Exception ex) {
                _parent.getCacheLog().Error(this.getName(), "Invalid value specified for NCacheServer.GCThreshold.");
            }
        }

        if (_parent != null && _parent.getCacheLog().getIsErrorEnabled()) {
            _parent.getCacheLog().CriticalInfo(getName(), " explicit-gc-enabled =" + _enableGC + " threshold = " + _gcThreshHold);
        }
    }

    protected boolean getIsSyncReplica() {
        return false;
    }

    public final void Start() {
        String instanceName = this.toString();

        _throttlingManager = new ThrottlingManager(stateTxfrDataSizePerSecond);
        _throttlingManager.Start();
        _worker = new Thread(this);
        _worker.setDaemon(true);
        _worker.start();
    }

    public final void Stop() {
        if (_worker != null) {
            _parent.getContext().getCacheLog().Flush();
            _worker.stop();
            _worker = null;
        }

        _sparsedBuckets.clear();
        _filledBuckets.clear();
    }

    public final void DoStateTransfer(java.util.ArrayList buckets, boolean transferQueue) {
        final Object[] args = new Object[3];
        int updateId = 0;

        synchronized (_updateIdMutex) {
            updateId = ++updateCount;
        }
        args[0] = buckets;
        args[1] = transferQueue;
        args[2]= updateId;

        Thread workerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                UpdateAsync(args);
            }
        });
        workerThread.start();
    }

    public class InternalClass implements Runnable {

        StateTransferTask state;
        Object obj;

        public InternalClass(StateTransferTask state, Object obj) {
            this.state = state;
            this.obj = obj;
        }

        @Override
        public void run() {
            if (state != null) {
                state.UpdateAsync(this.obj);
            }
        }
    }

    public final void UpdateAsync(Object state) {
        try {
            Object[] obj = (Object[]) ((state instanceof Object[]) ? state : null);
            java.util.ArrayList buckets = (java.util.ArrayList) ((obj[0] instanceof java.util.ArrayList) ? obj[0] : null);
            
            int updateId = (Integer)obj[2];

            _parent.DetermineClusterStatus();
            if(!UpdateStateTransfer(buckets, updateId)){
                return;
            }

            if (_parent.getHasDisposed()) {
                return;
            }

            if (!_isRunning) {
                _logStateTransferEvent = _stateTransferEventLogged = false;
                Start();
            }

        } catch (Exception e) {
            _parent.getContext().getCacheLog().Error(getName() + ".UpdateAsync", e.toString());
        }

    }

    /**
     * Gets or sets a value indicating whether this StateTransfer task is
     * initiated for Data balancing purposes or not.
     */
    public final boolean getIsBalanceDataLoad() {
        return _isBalanceDataLoad;
    }

    public final void setIsBalanceDataLoad(boolean value) {
        _isBalanceDataLoad = value;
    }

    public final boolean getIsRunning() {
        synchronized (_stateTxfrMutex) {
            return _isRunning;
        }
    }

    public final void setIsRunning(boolean value) {
        synchronized (_stateTxfrMutex) {
            _isRunning = value;
        }
    }

    /**
     * Do the state transfer now.
     */
    protected void Process()// throws IOException, ChannelClosedException, ChannelException
    {
        //fetch the latest stats from every node.
        _isRunning = true;

        Object result = null;

        try {
            _parent.getCluster().MarkClusterInStateTransfer();
            _parent.DetermineClusterStatus();

            _parent.getContext().getCacheLog().CriticalInfo(getName() + ".Process", " State transfer has started");
            
            BucketTxfrInfo info = new BucketTxfrInfo();
            while (true) {

                synchronized (_stateTxfrMutex) {
                    info = GetBucketsForTxfr().clone();

                    //if no more data to transfer then stop.
                    if (info.end) {
                        _isRunning = false;
                        break;
                    }
                }

                java.util.ArrayList bucketIds = info.bucketIds;
                Address owner = info.owner;
                boolean isSparsed = info.isSparsed;

                if (bucketIds != null && bucketIds.size() > 0) {
                    if (!_correspondingNodes.contains(owner)) {
                        _correspondingNodes.add(owner);
                    }
                   
                    TransferData(bucketIds, owner, isSparsed);
                }
            }
            
            result = _parent.Local_Count();
        } catch (Exception e) {
            _parent.getContext().getCacheLog().Error(getName() + ".Process", e.toString());
            result = e;
        } finally {

            //Mark state transfer completed.
            _parent.getCluster().MarkClusterStateTransferCompleted();
            try {
                if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                    _parent.getContext().getCacheLog().Info(getName() + ".Process", " Ending state transfer with result : " + result.toString());
                }

                _parent.EndStateTransfer(result);

                if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                    _parent.getContext().getCacheLog().Info(getName() + ".Process", " Total Corresponding Nodes: " + _correspondingNodes.size());
                }
                for (Iterator it = _correspondingNodes.iterator(); it.hasNext();) {
                    Address corNode = (Address) it.next();
                    if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        _parent.getContext().getCacheLog().Info(getName() + ".Process", " Corresponding Node: " + corNode.toString());
                    }
                    _parent.SignalEndOfStateTxfr(corNode);
                }
                _isInStateTxfr = false;

                if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                    _parent.getContext().getCacheLog().Info(getName() + ".Process", " Finalizing state transfer");
                }

                _parent.getContext().getCacheLog().CriticalInfo(getName() + ".Process", " State transfer has ended");
                if (_logStateTransferEvent) {
                    if (_parent.alertPropagator != null) {
                        _parent.alertPropagator.RaiseAlert(EventID.StateTransferStop, "NCache", "\"" + _parent.getContext().getSerializationContext() + "\""
                                + " has ended state transfer.");
                    }
                    //-
                    EventLogger.LogEvent("TayzGrid", "\"" + _parent.getContext().getSerializationContext() + "(" + _parent.getCluster().getLocalAddress().toString() + ")\""
                            + " has ended state transfer.", EventType.INFORMATION, EventCategories.Information, EventID.StateTransferStop);
                }

            } catch (Exception ex) {
                _parent.getContext().getCacheLog().Error(getName() + ".Process", ex.toString());
            }
        }
    }

    private void TransferData(int bucketId, Address owner, boolean sparsedBucket) throws InterruptedException, LockingException, StateTransferException, OperationFailedException, CacheException {
        java.util.ArrayList tmp = new java.util.ArrayList(1);
        tmp.add(bucketId);
        TransferData(tmp, owner, sparsedBucket);
    }

    protected void TransferData(java.util.ArrayList bucketIds, Address owner, boolean sparsedBuckets) throws InterruptedException, LockingException, StateTransferException, OperationFailedException, CacheException {
        java.util.ArrayList ownershipChanged = null;
        java.util.ArrayList lockAcquired = null;
        java.util.ArrayList alreayLocked = null;

        //ask coordinator node to lock this/these bucket(s) during the state transfer.
        java.util.HashMap lockResults = AcquireLockOnBuckets(bucketIds);

        if (lockResults != null) {
            ownershipChanged = (java.util.ArrayList) lockResults.get(BucketLockResult.OwnerChanged);
            if (ownershipChanged != null && ownershipChanged.size() > 0) {
                //remove from local buckets. remove from sparsedBuckets. remove from filledBuckets.
                //these are no more my property.
                java.util.Iterator ie = ownershipChanged.iterator();
                while (ie.hasNext()) {
                    Object obj = ie.next();
                    if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        _parent.getContext().getCacheLog().Info(getName() + ".TransferData", " " + obj.toString() + " ownership changed");
                    }

                    if (_parent.getInternalCache().getStatistics().getLocalBuckets().containsKey(obj)) {
                        synchronized (_parent.getInternalCache().getStatistics().getLocalBuckets()) {
                            _parent.getInternalCache().getStatistics().getLocalBuckets().remove(obj);
                        }
                    }
                }
            }

            lockAcquired = (java.util.ArrayList) lockResults.get(BucketLockResult.LockAcquired);
            if (lockAcquired != null && lockAcquired.size() > 0) {
                _failedKeysList = new java.util.ArrayList();
                AnnounceStateTransfer(lockAcquired);
                tangible.RefObject<Address> tempRef_owner = new tangible.RefObject<Address>(owner);
                boolean bktsTxfrd = TransferBuckets(lockAcquired, tempRef_owner, sparsedBuckets);
                owner = tempRef_owner.argvalue;

                ReleaseBuckets(lockAcquired);
                removeFailedKeysOnReplica();
            }
        } else {
            if (_parent.getContext().getCacheLog().getIsErrorEnabled()) {
                _parent.getContext().getCacheLog().Error(getName() + ".TransferData", " Lock acquisition failure");
            }
        }
    }

    private void removeFailedKeysOnReplica() {
        try {
            if (getIsSyncReplica() && _failedKeysList != null && _failedKeysList.size() > 0) {
                OperationContext operationContext = new OperationContext(OperationContextFieldName.RemoveOnReplica, true);
                operationContext.Add(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);

                Iterator iteFailedkeys = _failedKeysList.iterator();
                while (iteFailedkeys.hasNext()) {
                    Object key = iteFailedkeys.next();
                    try {
                        _parent.getContext().getCacheImpl().Remove(key, ItemRemoveReason.Removed, false, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                    } catch (Exception ex) {

                    }
                }

            }
        } finally {
            _failedKeysList = null;
        }
    }

    protected void PrepareBucketsForStateTxfr(java.util.ArrayList buckets) throws LockingException, StateTransferException, OperationFailedException, CacheException {
    }

    protected void EndBucketsStateTxfr(java.util.ArrayList buckets) throws LockingException, StateTransferException, OperationFailedException, CacheException {
    }

    protected void AnnounceStateTransfer(java.util.ArrayList buckets) throws CacheException {
        _parent.AnnounceStateTransfer(buckets);
    }

    protected void ReleaseBuckets(java.util.ArrayList lockedBuckets) throws CacheException {
        if (_parent != null) {
            _parent.ReleaseBuckets(lockedBuckets);
        }
    }

    /**
     * Transfers the buckets from a its owner. We may receive data in chunks. It
     * is a pull model, a node wanting state transfer a bucket makes request to
     * its owner.
     *
     * @param buckets
     * @param owner
     * @return
     */
    private boolean TransferBuckets(java.util.ArrayList buckets, tangible.RefObject<Address> owner, boolean sparsedBuckets) throws InterruptedException, LockingException, StateTransferException, OperationFailedException, CacheException {
        boolean transferEnd;
        boolean successfullyTxfrd = false;
        int expectedTxfrId = 1;
        boolean resync = false;
        try {
            if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                _parent.getContext().getCacheLog().Info(getName() + ".TransferBuckets", " Starting transfer. Owner : " + owner.argvalue.toString() + " , Bucket : "
                        + (new Integer((Integer) buckets.get(0))).toString());
            }

            PrepareBucketsForStateTxfr(buckets);
            long dataRecieved = 0;
            long currentIternationData = 0;

            while (true) {
                if (_enableGC && _dataTransferred >= _gcThreshHold) {
                    java.util.Date start = new Date();
                    System.gc();
                    Date end = new Date();
                    long diff = end.getTime() - start.getTime();
                    if (_parent.getCacheLog().getIsErrorEnabled()) {
                        _parent.getCacheLog().CriticalInfo(this.getName() + ".TransferBucket", "explicit GC called. time taken(ms) :" + diff + " gcThreshold :" + _gcThreshHold);
                    }

                } else {
                    _dataTransferred += currentIternationData;
                }

                boolean sleep = false;
                resync = false;
                transferEnd = true;
                StateTxfrInfo info = null;
                try {
                    currentIternationData = 0;
                    info = SafeTransferBucket(buckets, owner.argvalue, sparsedBuckets, expectedTxfrId);

                    if (info != null) {
                        currentIternationData = info.getDataSize();
                        dataRecieved += info.getDataSize();
                    }

                } catch (SuspectedException e) {
                    resync = true;
                } catch (TimeoutException e2) {
                }

                if (resync) {
                    if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        _parent.getContext().getCacheLog().Info(getName() + ".TransferBuckets", owner.argvalue + " is suspected");
                    }
                    Address changedOwner = GetChangedOwner((Integer) buckets.get(0), owner.argvalue);

                    if (changedOwner != null) {
                        if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                            _parent.getContext().getCacheLog().Info(getName() + ".TransferBuckets", changedOwner + " is new owner");
                        }

                        if (changedOwner.equals(owner.argvalue)) {
                            continue;
                        } else {
                            owner.argvalue = changedOwner;
                            expectedTxfrId = 1;
                            continue;
                        }
                    } else {
                        _parent.getContext().getCacheLog().Error(getName() + ".TransferBuckets", " Could not get new owner");
                        info = new StateTxfrInfo(true);
                    }
                }

                if (info != null) {
                    successfullyTxfrd = true;
                    transferEnd = info.transferCompleted;
                    java.util.HashMap tbl = info.data;
                    CacheEntry entry = null;

                    //next transfer
                    expectedTxfrId++;
                    //add data to local cache.

                    if (tbl != null && tbl.size() > 0) {
                        Iterator ide = tbl.entrySet().iterator();
                        while (ide.hasNext()) {
                            Map.Entry pair = (Map.Entry) ide.next();
                            if (!_stateTransferEventLogged) {
                                if (_parent.alertPropagator != null) {
                                    _parent.alertPropagator.RaiseAlert(EventID.StateTransferStart, "NCache", "\"" + _parent.getContext().getSerializationContext() + "("
                                            + _parent.getCluster().getLocalAddress().toString() + ")\"" + " has started state transfer.");
                                }
                                EventLogger.LogEvent("TayzGrid", "\"" + _parent.getContext().getSerializationContext() + "(" + _parent.getCluster().getLocalAddress().toString() + ")\"" + " has started state transfer.", EventType.INFORMATION, EventCategories.Information, EventID.StateTransferStart);
                                _stateTransferEventLogged = _logStateTransferEvent = true;
                            }
                            try {
                                OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);

                                if (pair.getValue() != null) {
                                    entry = (CacheEntry) ((pair.getValue() instanceof CacheEntry) ? pair.getValue() : null);

                                    CacheInsResultWithEntry result = _parent.getInternalCache().Insert(pair.getKey(), entry, false, false, null, entry.getVersion(), LockAccessType.PRESERVE_VERSION, operationContext);

                                    if (result != null && result.getResult() == CacheInsResult.NeedsEviction) {
                                        _failedKeysList.add(pair.getKey());
                                    }
                                } else {
                                    _parent.getInternalCache().Remove(pair.getKey(), ItemRemoveReason.Removed, false, false, null, 0, LockAccessType.IGNORE_LOCK, operationContext);
                                }
                            } catch (StateTransferException se) {
                                _parent.getContext().getCacheLog().Error(getName() + ".TransferBuckets", " Can not add/remove key = " + pair.getKey() + " : value is "
                                        + ((pair.getValue() == null) ? "null" : " not null") + " : " + se.toString());
                            } catch (Exception e) {
                                _parent.getContext().getCacheLog().Error(getName() + ".TransferBuckets", " Can not add/remove key = " + pair.getKey() + " : value is "
                                        + ((pair.getValue() == null) ? "null" : " not null") + " : " + e.toString());
                            }
                        }

                        if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                            _parent.getContext().getCacheLog().Info(getName() + ".TransferBuckets", " BalanceDataLoad = " + (new Boolean(_isBalanceDataLoad)).toString());
                        }

                        if (_isBalanceDataLoad) {
                            _parent.getContext().PerfStatsColl.incrementDataBalPerSecStatsBy(tbl.size());
                        } else {
                            _parent.getContext().PerfStatsColl.incrementStateTxfrPerSecStatsBy(tbl.size());
                        }
                    }
                } else {
                    successfullyTxfrd = false;
                }

                if (transferEnd) {
                    BucketsTransfered(owner.argvalue, buckets);
                    EndBucketsStateTxfr(buckets);
                    //send ack for the state transfer over.
                    //Ask every node to release lock on this/these bucket(s)
                    if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        _parent.getContext().getCacheLog().Info(getName() + ".TransferBuckets", "Acknowledging transfer. Owner : " + owner.argvalue.toString() + " , Bucket : "
                                + (new Integer((Integer) buckets.get(0))).toString());
                    }

                    AcknowledgeStateTransferCompleted(owner.argvalue, buckets);
                    break;
                }
                if (info != null) {
                    _throttlingManager.Throttle(info.getDataSize());
                }
            }
        } catch (InterruptedException e3) {
            EndBucketsStateTxfr(buckets);
            throw e3;
        }
        return successfullyTxfrd;

    }

    public void AcknowledgeStateTransferCompleted(Address owner, java.util.ArrayList buckets) throws OperationFailedException, GeneralFailureException {
        if (owner != null) {
            _parent.AckStateTxfrCompleted(owner, buckets);
        }
    }

    /**
     * Safely transfer a buckets from its owner. In case timeout occurs we retry
     * once again.
     *
     * @param buckets
     * @param owner
     * @return
     */
    private StateTxfrInfo SafeTransferBucket(java.util.ArrayList buckets, Address owner, boolean sparsedBuckets, int expectedTxfrId) throws SuspectedException, TimeoutException {
        StateTxfrInfo info = null;
        int retryCount = _bktTxfrRetryCount;

        while (retryCount > 0) {
            try {
                info = _parent.TransferBucket(buckets, owner, _trasferType, sparsedBuckets, expectedTxfrId, _isBalanceDataLoad);
                return info;
            } catch (SuspectedException e) {
                //Member with which we were doing state txfer has left.
                _parent.getContext().getCacheLog().Error(getName() + ".SafeTransterBucket", " " + owner + " is suspected during state txfr");
                for (Iterator it = buckets.iterator(); it.hasNext();) {
                    int bucket = (Integer) it.next();
                    try {
                        _parent.EmptyBucket(bucket);
                    } catch (Exception ex) {
                        _parent.getContext().getCacheLog().Error(getName() + ".SafeTransterBucket", ex.toString());
                    }
                }
                throw e;
            } catch (TimeoutException tout_e) {
                _parent.getContext().getCacheLog().Error(getName() + ".SafeTransterBucket", " State transfer request timed out from " + owner);
                retryCount--;
                if (retryCount <= 0) {
                    throw tout_e;
                }
            } catch (Exception e) {
                _parent.getContext().getCacheLog().Error(getName() + ".SafeTransterBucket", " An error occured during state Txfr " + e.toString());
                break;

            }
        }
        return info;
    }

    /**
     * Acquire locks on the buckets.
     *
     * @param buckets
     * @return
     */
    protected java.util.HashMap AcquireLockOnBuckets(java.util.ArrayList buckets) {
        int maxTries = 3;
        while (maxTries > 0) {
            try {
                java.util.HashMap lockResults = _parent.LockBuckets(buckets);
                return lockResults;
            } catch (Exception e) {
                _parent.getContext().getCacheLog().Error(getName() + ".AcquireLockOnBuckets", "could not acquire lock on buckets. error: " + e.toString());
                maxTries--;
            }
        }
        return null;
    }

    public BucketTxfrInfo GetBucketsForTxfr() {
        java.util.ArrayList bucketIds = null;
        Address owner = null;
        int bucketId;
        java.util.ArrayList filledBucketIds = null;

        synchronized (_stateTxfrMutex) {
            if (_sparsedBuckets != null && _sparsedBuckets.size() > 0) {
                synchronized (_sparsedBuckets) {
                    BucketsPack bPack = (BucketsPack) ((_sparsedBuckets.get(0) instanceof BucketsPack) ? _sparsedBuckets.get(0) : null);
                    owner = bPack.getOwner();
                    bucketIds = bPack.getBucketIds();
                    if (_allowBulkInSparsedBuckets) {
                        //_sparsedBuckets.Remove(bPack);
                        return new BucketTxfrInfo(bucketIds, true, owner);
                    } else {
                        java.util.ArrayList list = new java.util.ArrayList();
                        list.add(bucketIds.get(0));
                        //Although it is from the sparsed bucket but we intentionally set flag as non-sparsed.
                        return new BucketTxfrInfo(list, false, owner);
                    }
                }
            } else if (_filledBuckets != null && _filledBuckets.size() > 0) {
                synchronized (_filledBuckets) {
                    BucketsPack bPack = (BucketsPack) ((_filledBuckets.get(0) instanceof BucketsPack) ? _filledBuckets.get(0) : null);
                    owner = bPack.getOwner();
                    filledBucketIds = bPack.getBucketIds();
                    if (filledBucketIds != null && filledBucketIds.size() > 0) {
                        bucketId = (Integer) filledBucketIds.get(0);

                        bucketIds = new java.util.ArrayList(1);
                        bucketIds.add(bucketId);
                    }
                }
                return new BucketTxfrInfo(bucketIds, false, owner);
            } else {
                return new BucketTxfrInfo(true);
            }
        }
    }

    /**
     * Removes the buckets from the list of transferable buckets after we have
     * transferred them.
     *
     * @param owner
     * @param buckets
     * @param sparsed
     */
    protected final void BucketsTransfered(Address owner, java.util.ArrayList buckets) {
        BucketsPack bPack = null;
        synchronized (_stateTxfrMutex) {
            if (_sparsedBuckets != null) {
                BucketsPack dummy = new BucketsPack(null, owner);
                int index = _sparsedBuckets.indexOf(dummy);
                if (index != -1) {
                    bPack = (BucketsPack) ((_sparsedBuckets.get(index) instanceof BucketsPack) ? _sparsedBuckets.get(index) : null);
                    for (Iterator it = buckets.iterator(); it.hasNext();) {
                        int bucket = (Integer) it.next();
                        int indexOf = bPack.getBucketIds().indexOf(bucket);
                        if (indexOf >= 0) {
                            bPack.getBucketIds().remove(bucket);
                        }
                    }
                    if (bPack.getBucketIds().isEmpty()) {
                        _sparsedBuckets.remove(index);
                    }
                }
            }
            if (_filledBuckets != null) {
                BucketsPack dummy = new BucketsPack(null, owner);
                int index = _filledBuckets.indexOf(dummy);
                if (index != -1) {
                    bPack = (BucketsPack) ((_filledBuckets.get(index) instanceof BucketsPack) ? _filledBuckets.get(index) : null);
                    for (Iterator it = buckets.iterator(); it.hasNext();) {
                        int bucket = (Integer) it.next();
                        int ind = bPack.getBucketIds().indexOf(bucket);
                        if (ind >= 0) {
                            bPack.getBucketIds().remove(ind);
                        }
                    }
                    if (bPack.getBucketIds().isEmpty()) {
                        _filledBuckets.remove(index);
                    }
                }
            }
        }
    }

    private BucketStatistics GetBucketStats(int bucketId, Address owner) {
        List temp = Collections.synchronizedList(new ArrayList());
        for (int i = 0; i < _parent._stats.getNodes().size(); i++) {
            Object obj = _parent._stats.getNodes().get(i);
            temp.add(obj);
        }

        Object tempVar = temp;
        java.util.ArrayList nodeInfos = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
        if (nodeInfos != null) {
            java.util.Iterator ie = nodeInfos.iterator();
            if (ie != null) {
                while (ie.hasNext()) {
                    Object object = ie.next();
                    NodeInfo tmp = (NodeInfo) ((object instanceof NodeInfo) ? object : null);
                    if (tmp.getAddress().compareTo(owner) == 0 && tmp.getStatistics() != null) {
                        if (tmp.getStatistics().getLocalBuckets() != null) {
                            Object obj = tmp.getStatistics().getLocalBuckets().get(bucketId);
                            if (obj != null) {
                                return (BucketStatistics) ((obj instanceof BucketStatistics) ? obj : null);
                            }
                        }
                        return new BucketStatistics();
                    }
                }
            }
        }
        return null;
    }

    /**
     * This method removes all the buckets that were being transferred from
     * leaving member and are still incomplete.
     *
     *
     * @param leavingMbr
     */
    public final void ResetStateTransfer() {
        
    }

    /**
     * Updates the state transfer task in synchronus way. It adds/remove buckets
     * to be transferred by the state transfer task.
     *
     * @param myBuckets
     */
    public final boolean UpdateStateTransfer(java.util.ArrayList myBuckets, int updateId) {
        if (_parent.getHasDisposed()) {
            return false;
        }

        StringBuilder sb = new StringBuilder();
        synchronized (_updateIdMutex)
            {
                if (updateId != updateCount)
                {
                     if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                         _parent.getContext().getCacheLog().Info(getName() + "UpdateStateTxfr", " Do not need to update the task as update id does not match; provided id :" + updateId + " currentId :" + updateCount);
                     }
                    return false;
                }
            }
        
        synchronized (_stateTxfrMutex) {
            try {
                if (myBuckets != null) {
                    if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        _parent.getContext().getCacheLog().Info(getName() + ".UpdateStateTxfr", " my buckets " + myBuckets.size());
                    }
                    //we work on the copy of the map.
                    Object tempVar = myBuckets.clone();
                    java.util.ArrayList buckets = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
                    java.util.ArrayList leavingNodes = new java.util.ArrayList();

                    if (_sparsedBuckets != null && _sparsedBuckets.size() > 0) {
                        java.util.Iterator e = _sparsedBuckets.iterator();

                        synchronized (_sparsedBuckets) {
                            while (e.hasNext()) {
                                BucketsPack bPack = (BucketsPack) e.next();
                                Object tempVar2 = bPack.getBucketIds().clone();
                                java.util.ArrayList bucketIds = (java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null);
                                for (Iterator it = bucketIds.iterator(); it.hasNext();) {
                                    int bucketId = (Integer) it.next();
                                    HashMapBucket current = new HashMapBucket(null, bucketId);
                                    if (!buckets.contains(current)) {
                                        int indexOf = bPack.getBucketIds().indexOf(bucketId);
                                        if (indexOf >= 0) {
                                            bPack.getBucketIds().remove(indexOf);
                                        }
                                    } else {
                                        HashMapBucket bucket = (HashMapBucket) ((buckets.get(buckets.indexOf(current)) instanceof HashMapBucket) ? buckets.get(buckets.indexOf(current)) : null);
                                        if (!bPack.getOwner().equals(bucket.getPermanentAddress())) {
                                            //either i have become owner of the bucket or
                                            //some one else for e.g a replica node
                                            if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                                                _parent.getContext().getCacheLog().Info(getName() + ".UpdateStateTxfer", bucket.getBucketId() + "bucket owner changed old :"
                                                        + bPack.getOwner() + " new :" + bucket.getPermanentAddress());
                                            }
                                            int indexOf = bPack.getBucketIds().indexOf(bucketId);
                                            if (indexOf >= 0) {
                                                bPack.getBucketIds().remove(indexOf);
                                            }
                                        }
                                    }
                                }
                                if (bPack.getBucketIds().isEmpty()) {
                                    //This owner has left.
                                    leavingNodes.add(bPack.getOwner());
                                }

                            }

                            for (Iterator it = leavingNodes.iterator(); it.hasNext();) {
                                Address leavigNode = (Address) it.next();
                                BucketsPack bPack = new BucketsPack(null, leavigNode);
                                _sparsedBuckets.remove(bPack);
                            }
                            leavingNodes.clear();
                        }
                    }

                    if (_filledBuckets != null && _filledBuckets.size() > 0) {
                        java.util.Iterator e = _filledBuckets.iterator();
                        synchronized (_filledBuckets) {
                            while (e.hasNext()) {
                                BucketsPack bPack = (BucketsPack) e.next();
                                Object tempVar3 = bPack.getBucketIds().clone();
                                java.util.ArrayList bucketIds = (java.util.ArrayList) ((tempVar3 instanceof java.util.ArrayList) ? tempVar3 : null);
                                for (Iterator it = bucketIds.iterator(); it.hasNext();) {
                                    int bucketId = (Integer) it.next();
                                    HashMapBucket current = new HashMapBucket(null, bucketId);
                                    if (!buckets.contains(current)) {
                                        int index = bPack.getBucketIds().indexOf(bucketId);
                                        if (index >= 0) {
                                            bPack.getBucketIds().remove(index);
                                        }
                                    } else {
                                        HashMapBucket bucket = (HashMapBucket) ((buckets.get(buckets.indexOf(current)) instanceof HashMapBucket) ? buckets.get(buckets.indexOf(current)) : null);
                                        if (!bPack.getOwner().equals(bucket.getPermanentAddress())) {
                                            //either i have become owner of the bucket or
                                            //some one else for e.g a replica node
                                            int index = bPack.getBucketIds().indexOf(bucketId);
                                            if (index >= 0) {
                                                bPack.getBucketIds().remove(index);
                                            }
                                        }
                                    }
                                }

                                if (bPack.getBucketIds().isEmpty()) {
                                    //This owner has left.
                                    leavingNodes.add(bPack.getOwner());
                                }

                            }
                            for (Iterator it = leavingNodes.iterator(); it.hasNext();) {
                                Address leavigNode = (Address) it.next();
                                BucketsPack bPack = new BucketsPack(null, leavigNode);
                                _filledBuckets.remove(bPack);
                            }
                            leavingNodes.clear();
                        }
                    }

                    //Now we add those buckets which we have to be state transferred
                    //and are not currently in our list
                    java.util.Iterator ie = buckets.iterator();
                    _parent.getContext().getCacheLog().DevTrace(getName() + ".UpdateStateTxfr", Integer.toString(buckets.size()));
                    while (ie.hasNext()) {
                        Object obj = ie.next();
                        HashMapBucket bucket = (HashMapBucket) ((obj instanceof HashMapBucket) ? obj : null);
                        if (_localAddress.equals(bucket.getTempAddress()) && !_localAddress.equals(bucket.getPermanentAddress())) {
                            BucketsPack bPack = new BucketsPack(null, bucket.getPermanentAddress());

                            if (IsSparsedBucket(bucket.getBucketId(), bucket.getPermanentAddress())) {
                                int index = _sparsedBuckets.indexOf(bPack);
                                if (index != -1) {
                                    bPack = (BucketsPack) ((_sparsedBuckets.get(index) instanceof BucketsPack) ? _sparsedBuckets.get(index) : null);
                                } else {
                                    _sparsedBuckets.add(bPack);
                                }

                                if (!bPack.getBucketIds().contains(bucket.getBucketId())) {
                                    bPack.getBucketIds().add(bucket.getBucketId());
                                }

                            } else {
                                int index = _filledBuckets.indexOf(bPack);
                                if (index != -1) {
                                    bPack = (BucketsPack) ((_filledBuckets.get(index) instanceof BucketsPack) ? _filledBuckets.get(index) : null);
                                } else {
                                    _filledBuckets.add(bPack);
                                }

                                if (!bPack.getBucketIds().contains(bucket.getBucketId())) {
                                    bPack.getBucketIds().add(bucket.getBucketId());
                                }
                            }
                        }
                    }
                }
            } catch (NullPointerException nu) {
                _parent.getContext().getCacheLog().DevTrace(getName() + ".UpdateStateTxfr", nu.toString());
            } catch (Exception e) {
                _parent.getContext().getCacheLog().Error(getName() + ".UpdateStateTxfr", e.toString());
            } finally {
                if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                    _parent.getContext().getCacheLog().Info(getName() + ".UpdateStateTxfr", " Pulsing waiting thread");
                }
                Monitor.pulse(_stateTxfrMutex);
            }
        }
        return true;
    }

    protected final Address GetChangedOwner(int bucket, Address currentOwner) throws InterruptedException {
        Address newOwner = null;
        synchronized (_stateTxfrMutex) {
            while (true) {
                newOwner = GetOwnerOfBucket(bucket);
                if (newOwner == null) {
                    return null;
                }

                if (newOwner.equals(currentOwner)) {
                    Monitor.wait(_stateTxfrMutex);
                } else {
                    return newOwner;
                }
            }
        }
    }

    protected final Address GetOwnerOfBucket(int bucket) {
        synchronized (_stateTxfrMutex) {

            if (_sparsedBuckets != null) {
                for (Iterator it = _sparsedBuckets.iterator(); it.hasNext();) {
                    BucketsPack bPack = (BucketsPack) it.next();
                    if (bPack.getBucketIds().contains(bucket)) {
                        return bPack.getOwner();
                    }
                }
            }
            if (_filledBuckets != null) {
                for (Iterator it = _filledBuckets.iterator(); it.hasNext();) {
                    BucketsPack bPack = (BucketsPack) it.next();
                    if (bPack.getBucketIds().contains(bucket)) {
                        return bPack.getOwner();
                    }
                }
            }

        }
        return null;
    }

    /**
     * Determines whether a given bucket is sparsed one or not. A bucket is
     * considered sparsed if its size is less than the threshhold value.
     *
     * @param bucketId
     * @param owner
     * @return True, if bucket is sparsed.
     */
    public final boolean IsSparsedBucket(int bucketId, Address owner) {
        boolean isSparsed = false;
        BucketStatistics stats = GetBucketStats((int) bucketId, owner);
        isSparsed = stats != null ? stats.getDataSize() < _threshold : false;
        return isSparsed;

    }

    public final void UpdateBuckets() {
        synchronized (_parent._internalCache.getStatistics().getLocalBuckets()) {
            Iterator ide = _parent._internalCache.getStatistics().getLocalBuckets().entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                if (_isInStateTxfr) {
                    if (_sparsedBuckets != null && _sparsedBuckets.size() > 0) {
                        Object tempVar = _sparsedBuckets.clone();
                        java.util.ArrayList tmp = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
                        java.util.Iterator e = tmp.iterator();
                        synchronized (_sparsedBuckets) {
                            while (e.hasNext()) {
                                BucketsPack bPack = (BucketsPack) e.next();
                                Object tempVar2 = bPack.getBucketIds().clone();
                                java.util.ArrayList bucketIds = (java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null);
                                for (Iterator it = bucketIds.iterator(); it.hasNext();) {
                                    int bucketId = (Integer) it.next();
                                    if (!_parent._internalCache.getStatistics().getLocalBuckets().containsKey(bucketId)) {
                                        if (((HashMapBucket) _parent.getHashMap().get(bucketId)).getStatus() != BucketStatus.UnderStateTxfr) {
                                            int indexOf = bPack.getBucketIds().indexOf(bucketId);
                                            if (indexOf >= 0) {
                                                bPack.getBucketIds().remove(indexOf);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (_filledBuckets != null && _filledBuckets.size() > 0) {
                        Object tempVar3 = _filledBuckets.clone();
                        java.util.ArrayList tmp = (java.util.ArrayList) ((tempVar3 instanceof java.util.ArrayList) ? tempVar3 : null);
                        java.util.Iterator e = tmp.iterator();
                        synchronized (_filledBuckets) {
                            while (e.hasNext()) {
                                BucketsPack bPack = (BucketsPack) e.next();
                                Object tempVar4 = bPack.getBucketIds().clone();
                                java.util.ArrayList bucketIds = (java.util.ArrayList) ((tempVar4 instanceof java.util.ArrayList) ? tempVar4 : null);
                                for (Iterator it = bucketIds.iterator(); it.hasNext();) {
                                    int bucketId = (Integer) it.next();
                                    if (!_parent._internalCache.getStatistics().getLocalBuckets().containsKey(bucketId)) {
                                        if (((HashMapBucket) _parent.getHashMap().get(bucketId)).getStatus() != BucketStatus.UnderStateTxfr) {
                                            int indexOf = bPack.getBucketIds().indexOf(bucketId);
                                            if (indexOf >= 0) {
                                                bPack.getBucketIds().remove(indexOf);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Address owner = ((HashMapBucket) _parent.getHashMap().get((Integer) pair.getKey())).getPermanentAddress();
                    BucketsPack bPack = new BucketsPack(null, owner);

                    BucketStatistics bucketStats = GetBucketStats((Integer) pair.getKey(), owner);

                    if (bucketStats.getCount() > 0) {
                        if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                            _parent.getContext().getCacheLog().Info(getName() + ".UpdateBuckets()", " Bucket : " + pair.getKey() + " has " + bucketStats.getCount() + " items");
                        }
                    }

                    if (bucketStats.getDataSize() < _threshold) {
                        int index = _sparsedBuckets.indexOf(bPack);
                        if (index != -1) {
                            bPack = (BucketsPack) ((_sparsedBuckets.get(index) instanceof BucketsPack) ? _sparsedBuckets.get(index) : null);
                        }

                        bPack.getBucketIds().add(pair.getKey());

                        if (!_sparsedBuckets.contains(bPack)) {
                            _sparsedBuckets.add(bPack);
                        }
                    } else {
                        int index = _filledBuckets.indexOf(bPack);
                        if (index != -1) {
                            bPack = (BucketsPack) ((_filledBuckets.get(index) instanceof BucketsPack) ? _filledBuckets.get(index) : null);
                        }

                        bPack.getBucketIds().add(pair.getKey());

                        if (!_filledBuckets.contains(owner)) {
                            _filledBuckets.add(bPack);
                        }
                    }
                }
            }
        }
    }
}
