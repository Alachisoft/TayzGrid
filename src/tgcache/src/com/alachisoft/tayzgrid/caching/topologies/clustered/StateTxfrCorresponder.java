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

import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.OperationContextOperationType;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.common.util.MemoryUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class StateTxfrCorresponder 
{
    /**
     * 200K is the threshold data size. Above this threshold value, data will be
     * transfered in chunks.
     */
    protected long _threshold = 50 * 1024; //200 * 1000;
    public ClusterCacheBase _parent;
    protected int _currentBucket = -1;
    protected java.util.ArrayList _keyList;
    protected java.util.HashMap _keyUpdateLogTbl = new java.util.HashMap();
    protected int _keyCount;
    protected boolean _sendLogData = false;
    private int _lastTxfrId = 0;
    private DistributionManager _distMgr;
    private Address _clientNode;
    private java.util.ArrayList _logableBuckets = new java.util.ArrayList();
    private byte _transferType;
    private boolean _isBalanceDataLoad = false;


    /**
     * Gets or sets a value indicating whether this StateTransfer Corresponder
     * is in Data balancing mode or not.
     */
    public final boolean getIsBalanceDataLoad() {
        return _isBalanceDataLoad;
    }

    public final void setIsBalanceDataLoad(boolean value) {
        _isBalanceDataLoad = value;
    }

    public StateTxfrCorresponder(ClusterCacheBase parent, DistributionManager distMgr, Address requestingNode, byte transferType) {
        _parent = parent;
        _distMgr = distMgr;
        _clientNode = requestingNode;
        _transferType = transferType;
    }

    public final StateTxfrInfo TransferBucket(java.util.ArrayList bucketIds, boolean sparsedBuckets, int expectedTxfrId) throws StateTransferException, OperationFailedException, CacheException, LockingException {

        if (bucketIds != null) {
            for (int i = bucketIds.size() - 1; i >= 0; i--) {
                int bkId = (Integer) bucketIds.get(i);
                if (_transferType == StateTransferType.MOVE_DATA && !_distMgr.VerifyTemporaryOwnership(bkId, _clientNode)) {
                    if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.TransferBucket", bkId + " ownership changed");
                    }
                }
            }
        }

        if (sparsedBuckets) {
            return GetData(bucketIds);
        } else {
            if (bucketIds != null && bucketIds.size() > 0) {
                for (Iterator it = bucketIds.iterator(); it.hasNext();) {
                    Object bucketId = it.next();
                    synchronized (_parent._bucketStateTxfrStatus) {
                        _parent._bucketStateTxfrStatus.put(bucketId, true);
                    }
                }

                if (_currentBucket != (Integer) bucketIds.get(0)) {
                    if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.TxfrBucket", "bucketid : " + bucketIds.get(0) + " exptxfrId : " + expectedTxfrId);
                    }

                    _lastTxfrId = expectedTxfrId;
                    //request for a new bucket.
                    //get its key list from parent.
                    _currentBucket = (Integer) bucketIds.get(0);
                    boolean enableLogs = _transferType == StateTransferType.MOVE_DATA ? true : false;
                    java.util.ArrayList keyList = _parent.getInternalCache().GetKeyList(_currentBucket, enableLogs);
                    _logableBuckets.add(_currentBucket);

                    if (keyList != null) {
                        Object tempVar = keyList.clone();
                        {
                            _keyList = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
                        }
                    }

                    //reset the _lastLogTblCount
                    _sendLogData = false;
                } else {
                    if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.TxfrBucket", "bucketid : " + bucketIds.get(0) + " exptxfrId : " + expectedTxfrId);
                    }
                    //remove all the last sent keys from keylist that has not been
                    //modified during this time.
                    if (_keyList != null && expectedTxfrId > _lastTxfrId) {
                        synchronized (_keyList) {

                            _keyList.subList(0, _keyCount).clear();
                            _keyCount = 0;
                        }
                        _lastTxfrId = expectedTxfrId;
                    }
                }
            } else {
                return new StateTxfrInfo(new java.util.HashMap(), null, null, true);
            }

            //take care that we need to send data in chunks if
            //bucket is too large.
            return GetData(_currentBucket);
        }
    }

    protected final StateTxfrInfo GetData(java.util.ArrayList bucketIds) throws StateTransferException {
        try {
            Object[] keys = null;
            java.util.HashMap data = null;
            java.util.HashMap result = new java.util.HashMap();
            java.util.ArrayList payLoad = new java.util.ArrayList();
            java.util.ArrayList payLoadCompilationInfo = new java.util.ArrayList();

            if (!_sendLogData) {
                java.util.Iterator ie = bucketIds.iterator();
                while (ie.hasNext()) {
                    int bucketId = (Integer) ie.next();
                    if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.GetData(1)", "transfering data for bucket : " + bucketId);
                    }
                    boolean enableLogs = _transferType == StateTransferType.MOVE_DATA ? true : false;
                    java.util.ArrayList keyList = _parent.getInternalCache().GetKeyList(bucketId, enableLogs);
                    _logableBuckets.add(bucketId);

                    data = null;
                    if (keyList != null) {
                        if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                            _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.GetData(1)", "bucket : " + bucketId + " [" + keyList.size() + " ]");
                        }

                        keys = keyList.toArray(new Object[0]);
                        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                        operationContext.Add(OperationContextFieldName.GenerateQueryInfo, true);
                        data = _parent.getInternalCache().Get(keys, operationContext);
                    }

                    if (data != null && data.size() > 0) {
                        if (result.isEmpty()) {
                            Object tempVar = data.clone();
                            result = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
                        } else {
                            Iterator ide = data.entrySet().iterator();
                            while (ide.hasNext()) {
                                Map.Entry pair = (Map.Entry) ide.next();
                                CacheEntry entry = (CacheEntry) ((pair.getValue() instanceof CacheEntry) ? pair.getValue() : null);
                                UserBinaryObject ubObject = null;
                                if (entry.getValue() instanceof CallbackEntry) {
                                    Object tempVar2 = ((CallbackEntry) entry.getValue()).getValue();
                                    ubObject = (UserBinaryObject) ((tempVar2 instanceof UserBinaryObject) ? tempVar2 : null);
                                } else {
                                    Object tempVar3 = entry.getValue();
                                    {
                                        ubObject = (UserBinaryObject) ((tempVar3 instanceof UserBinaryObject) ? tempVar3 : null);
                                    }
                                }

                                payLoad.addAll(Arrays.asList(ubObject.getData()));
                                long size = entry.getDataSize();
                                payLoadCompilationInfo.add(size);
                                int index = payLoadCompilationInfo.size() - 1;
                                PayloadInfo payLoadInfo = new PayloadInfo(entry.CloneWithoutValue(), index);
                                result.put(pair.getKey(), payLoadInfo);
                            }

                        }
                    }
                }
                _sendLogData = true;
                if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                    _parent.getContext().getCacheLog().Info("State Transfer Corresponder", "BalanceDataLoad = " + (new Boolean(_isBalanceDataLoad)).toString());
                }
                if (_isBalanceDataLoad) {
                    _parent.getContext().PerfStatsColl.incrementDataBalPerSecStatsBy(result.size());
                } else {
                    _parent.getContext().PerfStatsColl.incrementStateTxfrPerSecStatsBy(result.size());
                }
                return new StateTxfrInfo(result, payLoad, payLoadCompilationInfo, false);
            } else {
                return GetLoggedData(bucketIds);
            }
        } catch (Exception ex) {
            _parent.getContext().getCacheLog().Error("StateTxfrCorresponder.GetData(1)", ex.toString());
            return null;
        }
    }

    protected final StateTxfrInfo GetData(int bucketId) throws OperationFailedException, CacheException, LockingException {
        java.util.HashMap result = new java.util.HashMap();
        java.util.ArrayList payLoad = new java.util.ArrayList();
        java.util.ArrayList payLoadCompilationInfo = new java.util.ArrayList();

        long sizeToSend = 0;

        synchronized (_parent._bucketStateTxfrStatus) {
            _parent._bucketStateTxfrStatus.put(bucketId, true);
        }

        if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
            _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.GetData(2)", "state txfr request for :" + bucketId + " txfrid :" + _lastTxfrId);
        }

        if (_keyList != null && _keyList.size() > 0) {
            if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.GetData(2)", "bucket size :" + _keyList.size());
            }

            for (_keyCount = 0; _keyCount < _keyList.size(); _keyCount++) {
                Object key = _keyList.get(_keyCount);

                OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                operationContext.Add(OperationContextFieldName.GenerateQueryInfo, true);
                CacheEntry entry = _parent.getInternalCache().getInternalCache().Get(key, false, operationContext);

                if (entry != null) {

                    //@24march2015 --TODO-- key must b sizeable
                    long size = entry.getInMemorySize();// + MemoryUtil.getStringSize(key)); entry size alos includes key size;

                    if (sizeToSend > _threshold) {
                        break;
                    }

                    UserBinaryObject ubObject = null;
                    if (entry.getValue() instanceof CallbackEntry) {
                        Object tempVar = ((CallbackEntry) entry.getValue()).getValue();
                        ubObject = (UserBinaryObject) ((tempVar instanceof UserBinaryObject) ? tempVar : null);
                    } else {
                        Object tempVar2 = entry.getValue();
                        {
                            ubObject = (UserBinaryObject) ((tempVar2 instanceof UserBinaryObject) ? tempVar2 : null);
                        }
                    }

                    payLoad.addAll(Arrays.asList(ubObject.getData()));
                    long entrySize = entry.getDataSize();
                    payLoadCompilationInfo.add(entrySize);
                    int index = payLoadCompilationInfo.size() - 1;
                    PayloadInfo payLoadInfo = new PayloadInfo(entry.CloneWithoutValue(), index);

                    result.put(key, payLoadInfo);
                    sizeToSend += size;
                }
            }
            if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.GetData(2)", "items sent :" + _keyCount);
            }

            if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.GetData(2)", "BalanceDataLoad = " + (new Boolean(_isBalanceDataLoad)).toString());
            }

            if (_isBalanceDataLoad) {
                _parent.getContext().PerfStatsColl.incrementDataBalPerSecStatsBy(result.size());
            } else {
                _parent.getContext().PerfStatsColl.incrementStateTxfrPerSecStatsBy(result.size());
            }
            return new StateTxfrInfo(result, payLoad, payLoadCompilationInfo, false, sizeToSend);
        } else if (_transferType == StateTransferType.MOVE_DATA) {
            //We need to transfer the logs.
            if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.GetData(2)", "sending log data for bucket: " + bucketId);
            }

            java.util.ArrayList list = new java.util.ArrayList(1);
            list.add(bucketId);
            return GetLoggedData(list);
        } else {
            //As transfer mode is not MOVE_DATA, therefore no logs are maintained
            //and hence are not transferred.
            return new StateTxfrInfo(null, null, null, true);
        }
    }

    protected StateTxfrInfo GetLoggedData(java.util.ArrayList bucketIds) throws OperationFailedException {
        java.util.ArrayList updatedKeys = null;
        java.util.ArrayList removedKeys = null;
        java.util.HashMap logTbl = null;
        StateTxfrInfo info = null;
        java.util.HashMap result = new java.util.HashMap();
        java.util.ArrayList payLoad = new java.util.ArrayList();
        java.util.ArrayList payLoadCompilationInfo = new java.util.ArrayList();

        boolean isLoggingStopped = false;
        try {
            tangible.RefObject<Boolean> tempRef_isLoggingStopped = new tangible.RefObject<Boolean>(isLoggingStopped);
            logTbl = _parent.getInternalCache().GetLogTable(bucketIds, tempRef_isLoggingStopped);
            isLoggingStopped = tempRef_isLoggingStopped.argvalue;

            if (logTbl != null) {

                updatedKeys = (java.util.ArrayList) ((logTbl.get("updated") instanceof java.util.ArrayList) ? logTbl.get("updated") : null);
                removedKeys = (java.util.ArrayList) ((logTbl.get("removed") instanceof java.util.ArrayList) ? logTbl.get("removed") : null);

                if (updatedKeys != null && updatedKeys.size() > 0) {
                    for (int i = 0; i < updatedKeys.size(); i++) {
                        Object key = updatedKeys.get(i);

                        OperationContext operationContext = new OperationContext(OperationContextFieldName.OperationType, OperationContextOperationType.CacheOperation);
                        operationContext.Add(OperationContextFieldName.GenerateQueryInfo, true);
                        CacheEntry entry = _parent.getInternalCache().Get(key, false, operationContext);

                        UserBinaryObject ubObject = null;
                        if (entry.getValue() instanceof CallbackEntry) {
                            Object tempVar = ((CallbackEntry) entry.getValue()).getValue();
                            ubObject = (UserBinaryObject) ((tempVar instanceof UserBinaryObject) ? tempVar : null);
                        } else {
                            Object tempVar2 = entry.getValue();
                            {
                                ubObject = (UserBinaryObject) ((tempVar2 instanceof UserBinaryObject) ? tempVar2 : null);
                            }
                        }

                        payLoad.addAll(Arrays.asList(ubObject.getData()));
                        long size = entry.getDataSize();
                        payLoadCompilationInfo.add(size);
                        int index = payLoadCompilationInfo.size() - 1;
                        PayloadInfo payLoadInfo = new PayloadInfo(entry.CloneWithoutValue(), index);

                        result.put(key, payLoadInfo);
                    }
                }
                if (removedKeys != null && removedKeys.size() > 0) {
                    for (int i = 0; i < removedKeys.size(); i++) {
                        Object key = removedKeys.get(i);
                        result.put(key, null);
                    }
                }

                if (!isLoggingStopped) {
                    info = new StateTxfrInfo(result, payLoad, payLoadCompilationInfo, false);
                } else {
                    info = new StateTxfrInfo(result, payLoad, payLoadCompilationInfo, true);
                }

                _parent.getContext().getCacheLog().Debug("StateTxfrCorresponder.GetLoggedData()", info == null ? "returning null state-txfr-info" : "returning "
                        + (Integer) info.data.size() + " items in state-txfr-info");
                return info;
            } else {
                if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                    _parent.getContext().getCacheLog().Info("StateTxfrCorresoponder.GetLoggedData", "no logged data found");
                }
            }
        } catch (Exception e) {
            _parent.getContext().getCacheLog().Error("StateTxfrCorresoponder.GetLoggedData", e.toString());
            throw new OperationFailedException(e);
        } finally {
        }
        //no operation has been logged during state transfer.
        //so announce completion of state transfer for this bucket.
        return new StateTxfrInfo(result, payLoad, payLoadCompilationInfo, true);
    }

    /**
     * Disposes the state txfr corresponder. On dispose corresponder should stop
     * logger in the hashed cache if it has turned on any one.
     */
    public final void dispose() {
        if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
            _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.Dispose", _clientNode.toString() + " corresponder disposed");
        }
        if (_keyList != null) {
            _keyList.clear();
        }
        if (_keyUpdateLogTbl != null) {
            _keyUpdateLogTbl.clear();
        }

        if (_transferType == StateTransferType.MOVE_DATA) {
            if (_parent != null && _logableBuckets != null) {
                for (int i = 0; i < _logableBuckets.size(); i++) {
                    if (_parent.getContext().getCacheLog().getIsInfoEnabled()) {
                        _parent.getContext().getCacheLog().Info("StateTxfrCorresponder.Dispose", " removing logs for bucketid " + _logableBuckets.get(i));
                    }
                    _parent.RemoveFromLogTbl((Integer) _logableBuckets.get(i));
                }
            }
        }
    }
}
