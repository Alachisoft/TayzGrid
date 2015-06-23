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

package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.cluster.util.RspList;
import com.alachisoft.tayzgrid.cluster.util.Rsp;
import com.alachisoft.tayzgrid.cluster.OperationResponse;
import com.alachisoft.tayzgrid.caching.topologies.clustered.results.ClusterOperationResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.caching.statistics.NodeStatus;
import com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.LockOptions;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.RemoteException;
import com.alachisoft.tayzgrid.runtime.exceptions.ParserException;
import com.alachisoft.tayzgrid.runtime.exceptions.BadResponseException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import java.util.ArrayList;

/**
 * Deals in tasks specific to Cluster cache implementations.
 */
public class ClusterHelper {

    public static void ValidateResponses(RspList results, java.lang.Class type, String serializationContext) throws Exception {
        if (results == null) {
            return;
        }

        java.util.ArrayList parserExceptions = new java.util.ArrayList();
        java.util.ArrayList exceptions = new java.util.ArrayList(11);
        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);

            if (rsp.wasSuspected()) {
                continue;
            }

            if (!rsp.wasReceived()) {
                continue;
            }

            if (rsp.getValue() != null) {
                Object rspValue = rsp.getValue();

                if (rspValue instanceof ParserException) {
                    parserExceptions.add((Exception) rspValue);
                    continue;
                }
                if ((((Exception) ((rspValue instanceof Exception) ? rspValue : null)) instanceof com.alachisoft.tayzgrid.parser.AttributeIndexNotDefined)
                        || (((Exception) ((rspValue instanceof Exception) ? rspValue : null)) instanceof com.alachisoft.tayzgrid.parser.TypeIndexNotDefined)) {
                    parserExceptions.add((Exception) rspValue);
                    continue;
                }
                if (rspValue instanceof Exception) {
                    Exception exception = (Exception) rspValue;

                    if (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("index is not defined for")) {
                        parserExceptions.add((Exception) rspValue);
                        continue;
                    }

                    exceptions.add((Exception) rspValue);
                    continue;
                }

                if (type != null && !rspValue.getClass().equals(type)) {
                    exceptions.add(new BadResponseException("bad response returned by group member " + rsp.getSender()));
                    continue;
                }
            }
        }
        //in case or partitioned caches search requests are broadcasted.
        //it is possible that tag index are defined on one node but not defined on some other node.
        //we will throw the exception back only if we receive exception from every node.
        if (parserExceptions.size() == results.size()) {
            Exception e = (Exception) ((parserExceptions.get(0) instanceof Exception) ? parserExceptions.get(0) : null);
            throw e;
        }

        if (exceptions.size() == 1) {
            Exception e = (Exception) ((exceptions.get(0) instanceof Exception) ? exceptions.get(0) : null);
            if (e instanceof CacheException) { 
                throw e;
            } else {
                throw new RemoteException((Exception) exceptions.get(0));
            }
        } else if (exceptions.size() > 0) {
            for (int i = 0; i < exceptions.size(); i++) {
                Exception e = (Exception) ((exceptions.get(i) instanceof Exception) ? exceptions.get(i) : null);
                if (e instanceof LockingException) { 
                    throw e;
                } else if (e instanceof CacheException) {
                    continue;
                } else {
                    exceptions.set(i, new RemoteException(e));
                }
            }
            throw new com.alachisoft.tayzgrid.runtime.exceptions.AggregateException(exceptions);
        }
    }

    /**
     * Returns the set of nodes where the addition was performed as an atomic
     * operation.
     *
     * @param results responses collected from all members of cluster.
     * @return list of nodes where the operation succeeded
     */
    public static CacheAddResult FindAtomicAddStatusReplicated(RspList results) {
        CacheAddResult res = CacheAddResult.Failure;
        if (results == null) {
            return res;
        }
        int timeoutCount = 0;
        int suspectedCount = 0;
        int successCount = 0;

        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);

            if (rsp.wasSuspected()) {
                suspectedCount++;
                continue;
            }
            if (!rsp.wasReceived() && !rsp.wasSuspected()) {
                timeoutCount++;
                continue;
            }
            res = (CacheAddResult) rsp.getValue();
            if (res == CacheAddResult.Success) {
                successCount++;
            }
            if (res != CacheAddResult.Success && res != CacheAddResult.KeyExists) {
                return res;
            }
        }
        if (suspectedCount > 0 && successCount > 0 && (suspectedCount + successCount == results.size())) {
            //as operation is successfull on all other nodes other than suspected node(s).
            return CacheAddResult.Success;
        }
        if (timeoutCount > 0 && (timeoutCount + successCount == results.size())) {
            if (successCount > 0) {
                //operation is not succeeded on some of the nodes; therefore we throw timeout exception.
                return CacheAddResult.PartialTimeout;
            } else {
                //operation timed out on all of the node; no need to rollback.
                return CacheAddResult.FullTimeout;
            }
        }
        if (timeoutCount > 0 && suspectedCount > 0) {
            if (successCount > 0) {
                return CacheAddResult.PartialTimeout;
            } else {
                return CacheAddResult.FullTimeout;
            }
        }

        return res;
    }

    public static Rsp FindAtomicRemoveStatusReplicated(RspList results) throws TimeoutException {
        return FindAtomicRemoveStatusReplicated(results, null);
    }

    /**
     * Returns the set of nodes where the addition was performed as an atomic
     * operation.
     *
     * @param results responses collected from all members of cluster.
     * @return list of nodes where the operation succeeded
     */
    public static Rsp FindAtomicRemoveStatusReplicated(RspList results, ILogger NCacheLog) throws TimeoutException {
        Rsp retRsp = null;
        if (results == null) {
            return retRsp;
        }
        int timeoutCount = 0;
        int suspectedCount = 0;
        int successCount = 0;
        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);

            if (!rsp.wasReceived() && !rsp.wasSuspected()) {
                timeoutCount++;
                continue;
            }
            if (rsp.wasSuspected()) {
                suspectedCount++;
                continue;
            }
            if (rsp.getValue() != null) {
                retRsp = rsp;
            }
            successCount++;

        }
        if (suspectedCount > 0 && successCount > 0 && (suspectedCount + successCount == results.size())) {
            //as operation is successfull on all other nodes other than suspected node(s).
            return retRsp;
        }
        if (timeoutCount > 0 && (timeoutCount + successCount == results.size())) {
            throw new TimeoutException("Operation TimeOut");
        }
        if (timeoutCount > 0 && suspectedCount > 0) {
            throw new TimeoutException("Operation TimeOut");
        }

        return retRsp;
    }

    public static LockOptions FindAtomicIsLockedStatusReplicated(RspList results, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate) {
        LockOptions lockInfo = null;
        if (results == null) {
            return lockInfo;
        }

        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);

            if (rsp.wasSuspected()) {
                continue;
            }
            if (!rsp.wasReceived()) {
                continue;
            }

            lockInfo = (LockOptions) rsp.getValue();
            if (lockInfo != null) {
                if (lockInfo.getLockId() != null) {
                    return lockInfo;
                }
            }
        }
        return lockInfo;
    }

    public static boolean FindAtomicLockStatusReplicated(RspList results, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate) {
        boolean res = true;
        LockOptions lockInfo = null;
        if (results == null) {
            return res;
        }
        int lockAcquired = 0;
        int itemNotFound = 0;
        int rspReceived = results.size();

        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);

            if (rsp.wasSuspected()) {
                rspReceived--;
                continue;
            }
            if (!rsp.wasReceived()) {
                rspReceived--;
                continue;
            }

            lockInfo = (LockOptions) rsp.getValue();
            Object lock = lockInfo.getLockId() == null ? "" : lockInfo.getLockId();
            if (lock.equals(lockId.argvalue)) {
                lockDate.argvalue = lockInfo.getLockDate();
                lockAcquired++;
            } else {
                if (lockInfo.getLockId() == null) {
                    //item was not present on the node.
                    lockId.argvalue = null;
                    try {
                        NCDateTime time = new NCDateTime(1970, 1, 1, 0, 0, 0, 0);
                        lockDate.argvalue = time.getDate();
                    } catch (Exception exc) {
                    }
                    itemNotFound++;
                } else {
                    res = false;
                    lockId.argvalue = lockInfo.getLockId();
                    lockDate.argvalue = lockInfo.getLockDate();
                    break;
                }
            }

        }
        if (lockAcquired > 0 && (lockAcquired + itemNotFound == rspReceived)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the next enumeration data chunk from the set of chunks returned
     * by multiple nodes
     *
     * @param results
     * @return
     */
    public static EnumerationDataChunk FindAtomicEnumerationDataChunkReplicated(RspList results) {
        EnumerationDataChunk nextChunk = null;
        if (results == null) {
            return nextChunk;
        }

        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);

            if (!rsp.wasReceived() || rsp.wasSuspected()) {
                continue;
            }

            nextChunk = (EnumerationDataChunk) rsp.getValue();

            if (nextChunk != null) {
                return nextChunk;
            }
        }
        return nextChunk;
    }

    /**
     * Returns the set of nodes where the addition was performed as an atomic
     * operation.
     *
     * @param results responses collected from all members of cluster.
     * @return list of nodes where the operation succeeded
     */
    public static boolean FindAtomicAddHintReplicated(RspList results) {
        boolean res = false;
        if (results == null) {
            return res;
        }

        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);

            if (!rsp.wasReceived() || rsp.wasSuspected()) {
                continue;
            }

            res = (Boolean) rsp.getValue();
            if (res == false) {
                return res;
            }
        }
        return res;
    }

    /**
     * Returns the array of keys for which Bulk operation failed.
     *
     * @param results responses collected from all members of cluster.
     * @return list of nodes where the operation succeeded
     */
    public static Object[] FindAtomicBulkOpStatusReplicated(RspList results, Address local) {
        java.util.HashMap failedKeys = new java.util.HashMap();

        Object[] result = null;
        if (results == null) {
            return null;
        }

        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);

            if (rsp.wasSuspected()) {
                continue;
            }
            if (!rsp.wasReceived()) {
                continue;
            }

            result = (Object[]) rsp.getValue();

            for (int j = 0; j < result.length; j++) {
                if (failedKeys.containsKey(result[j]) == false) {
                    failedKeys.put(result[j], result[j]);
                }
            }
        }

        Object[] failed = new Object[failedKeys.size()];
        System.arraycopy(failedKeys, 0, failed, 0, failedKeys.size());

        return failed;
    }

    /**
     * Returns the set of nodes where the insertion was performed as an atomic
     * operation.
     *
     * @param results responses collected from all members of cluster.
     * @return list of nodes where the operation succeeded
     */
    public static CacheInsResultWithEntry FindAtomicInsertStatusReplicated(RspList results) {
        int needEvictCount = 0;
        int timeoutCount = 0;
        int suspectedCount = 0;
        int successCount = 0;

        CacheInsResultWithEntry res = new CacheInsResultWithEntry();
        if (results == null) {
            return res;
        }

        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);
            if (!rsp.wasReceived() && !rsp.wasSuspected()) {
                timeoutCount++;
                continue;
            }

            if (rsp.wasSuspected()) {
                suspectedCount++;
                continue;
            }

            res = (CacheInsResultWithEntry) ((OperationResponse) rsp.getValue()).SerializablePayload;
            if (res.getResult() == CacheInsResult.Success || res.getResult() == CacheInsResult.SuccessOverwrite) {
                successCount++;
            }
            if (res.getResult() != CacheInsResult.Success && res.getResult() != CacheInsResult.SuccessOverwrite && res.getResult() != CacheInsResult.NeedsEviction) {
            }

            /*
             * If all the nodes in the Cluster return NeedsEviction response then we do not need to remove
             */
            if (res.getResult() == CacheInsResult.NeedsEviction) {
                needEvictCount++;
            }
        }

        if (needEvictCount == results.size()) {
            //every node returned the NeedEviction; so we need not remove the item
            //as data is not corrupted.
            res.setResult(CacheInsResult.NeedsEvictionNotRemove);
        }
        if (suspectedCount > 0 && successCount > 0 && (suspectedCount + successCount == results.size())) {
            //as operation is successfull on all other nodes other than suspected node(s).
        }
        if (timeoutCount > 0 && (timeoutCount + successCount == results.size())) {
            if (successCount > 0) {
                //operation is not succeeded on some of the nodes; therefore we throw timeout exception.
                res.setResult(CacheInsResult.PartialTimeout);
            } else {
                //operation timed out on all of the node; no need to rollback.
                res.setResult(CacheInsResult.FullTimeout);
            }
        }
        if (timeoutCount > 0 && suspectedCount > 0) {
            if (successCount > 0) {
                //operation is not succeeded on some of the nodes; therefore we throw timeout exception.
                res.setResult(CacheInsResult.PartialTimeout);
            } else {
                //operation timed out on all of the node; no need to rollback.
                res.setResult(CacheInsResult.FullTimeout);
            }
        }

        return res;
    }

    /**
     * Returns the set of nodes where the insertion was performed as an atomic
     * operation.
     *
     * @param results responses collected from all members of cluster.
     * @return key and value pairs for inserted items
     */
    public static java.util.HashMap FindAtomicBulkInsertStatusReplicated(RspList results) {
        java.util.HashMap insertedKeys = new java.util.HashMap();
        java.util.HashMap result = null;
        java.util.HashMap prvResult = null;

        if (results == null) {
            return insertedKeys;
        }

        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);
            if (rsp.wasSuspected()) {
                continue;
            }
            if (!rsp.wasReceived()) {
                continue;
            }

            result = (java.util.HashMap) rsp.getValue();

            if (prvResult == null) {
                insertedKeys = result;
                prvResult = result;
            } else {
                for (Object key : prvResult.keySet()) {
                    if (result.containsKey(key) == false) {
                        if (insertedKeys.containsKey(key)) {
                            insertedKeys.remove(key);
                        }
                    }
                }

                prvResult = result;
            }
        }
        return insertedKeys;
    }

    /**
     * Find first entry in the response list that is not null and didnt timeout.
     *
     * @param results response list
     * @return found entry
     */
    public static Rsp GetFirstNonNullRsp(RspList results) {
        if (results == null) {
            return null;
        }

        Rsp rsp = null;
        ArrayList responseToRemove = null;
        for (int i = 0; i < results.size(); i++) {
            rsp = (Rsp) results.elementAt(i);

            if (rsp.wasSuspected()) {
                if (responseToRemove == null) {
                    responseToRemove = new ArrayList(3);
                }
                responseToRemove.add(i);
                continue;
            }
            if (!rsp.wasReceived()) {
                if (responseToRemove == null) {
                    responseToRemove = new ArrayList(3);
                }
                responseToRemove.add(i);
                continue;
            }

            if (rsp.getValue() != null) {
                if (responseToRemove != null) {
                    for (int j = 0; j < responseToRemove.size(); j++) {
                        results.removeElementAt((Integer) responseToRemove.get(j));
                    }
                }
                return rsp;
            }
        }

        if (responseToRemove != null) {
            for (int j = 0; j < responseToRemove.size(); j++) {
                results.removeElementAt((Integer) responseToRemove.get(j));
            }
        }
        return null;
    }

    /**
     * Find first entry in the response list that is not null and didnt timeout.
     *
     * @param results response list
     * @param type type of response to fetch
     * @return found entry
     */
    public static Rsp GetFirstNonNullRsp(RspList results, java.lang.Class type) {
        if (results == null) {
            return null;
        }

        Rsp rsp = null;
        ArrayList responseToRemove = null;
        for (int i = 0; i < results.size(); i++) {
            rsp = (Rsp) results.elementAt(i);

            if (rsp.wasSuspected()) {
                if (responseToRemove == null) {
                    responseToRemove = new ArrayList(3);
                }
                responseToRemove.add(i);
                continue;
            }
            if (!rsp.wasReceived()) {
                if (responseToRemove == null) {
                    responseToRemove = new ArrayList(3);
                }
                responseToRemove.add(i);
                continue;
            }

            if (rsp.getValue() != null && rsp.getValue().getClass().equals(type)) {
                if (responseToRemove != null) {
                    for (int j = 0; j < responseToRemove.size(); j++) {
                        results.removeElementAt((Integer) responseToRemove.get(j));
                    }
                }
                return rsp;
            }
        }
        if (responseToRemove != null) {
            for (int j = 0; j < responseToRemove.size(); j++) {
                results.removeElementAt((Integer) responseToRemove.get(j));
            }
        }
        return null;
    }

    /**
     * Find all entries in the response list that are not null and didnt
     * timeout.
     *
     * @param results response list
     * @param type type of response to fetch
     * @return List of entries found
     */
    public static java.util.ArrayList GetAllNonNullRsp(RspList results, java.lang.Class type) {
        java.util.ArrayList list = new java.util.ArrayList();
        if (results == null) {
            return null;
        }

        Rsp rsp = null;
        ArrayList responseToRemove = null;
        for (int i = 0; i < results.size(); i++) {
            rsp = (Rsp) results.elementAt(i);

            if (rsp.wasSuspected()) {
                if (responseToRemove == null) {
                    responseToRemove = new ArrayList(3);
                }
                responseToRemove.add(i);
                continue;
            }
            if (!rsp.wasReceived()) {
                if (responseToRemove == null) {
                    responseToRemove = new ArrayList(3);
                }
                responseToRemove.add(i);
                continue;
            }

            if (rsp.getValue() != null && rsp.getValue().getClass().equals(type)) {
                list.add(rsp);
            }
        }

        if (responseToRemove != null) {
            for (int i = 0; i < responseToRemove.size(); i++) {
                results.removeElementAt((Integer) responseToRemove.get(i));
            }
        }
        return list;
    }

    /**
     * Returns the array of keys for which Bulk operation failed.
     *
     * @param results responses collected from all members of cluster.
     * @return list of nodes where the operation succeeded
     */
    public static java.util.HashMap FindAtomicBulkRemoveStatusReplicated(RspList results, Address local) {
        java.util.HashMap result = null;

        if (results == null) {
            return new java.util.HashMap();
        }

        for (int i = 0; i < results.size(); i++) {
            Rsp rsp = (Rsp) results.elementAt(i);

            if (rsp.wasSuspected()) {
                continue;
            }
            if (!rsp.wasReceived()) {
                continue;
            }

            result = (java.util.HashMap) rsp.getValue();

            if (result != null) {
                return result;
            }
        }

        return result;
    }

    /**
     * Combines the collected statistics of the nodes, in a partitioned
     * environment.
     *
     * @return
     */
    public static CacheStatistics CombinePartitionReplicasStatistics(ClusterCacheStatistics s) {
        CacheStatistics stats = new CacheStatistics();
        if (s.getNodes() == null) {
            return stats;
        }

        boolean zeroSeen = false;
        for (int i = 0; i < s.getNodes().size(); i++) {
            Object tempVar = s.getNodes().get(i);
            NodeInfo info = (NodeInfo) ((tempVar instanceof NodeInfo) ? tempVar : null);
            if (info == null || info.getStatistics() == null || !info.getStatus().IsAnyBitSet((byte) (NodeStatus.Coordinator | NodeStatus.SubCoordinator))) {
                continue;
            }

            stats.setHitCount(stats.getHitCount() + info.getStatistics().getHitCount());
            stats.setMissCount(stats.getMissCount() + info.getStatistics().getMissCount());
            stats.UpdateCount(stats.getCount() + info.getStatistics().getCount());
            stats.setMaxCount(stats.getMaxCount() + info.getStatistics().getMaxCount());
            if (info.getStatistics().getMaxCount() == 0) {
                zeroSeen = true;
            }
        }

        stats.setMaxSize(s.getLocalNode().getStatistics().getMaxSize());

        if (zeroSeen) {
            stats.setMaxCount(0);
        }
        return stats;
    }

    /**
     * Combines the collected statistics of the nodes, in a partitioned
     * environment.
     *
     * @return
     */
    public static CacheStatistics CombinePartitionStatistics(ClusterCacheStatistics s) {
        CacheStatistics stats = new CacheStatistics();
        if (s.getNodes() == null) {
            return stats;
        }

        boolean zeroSeen = false;
        for (int i = 0; i < s.getNodes().size(); i++) {
            Object tempVar = s.getNodes().get(i);
            NodeInfo info = (NodeInfo) ((tempVar instanceof NodeInfo) ? tempVar : null);
            if (info == null || info.getStatistics() == null) {
                continue;
            }

            stats.setHitCount(stats.getHitCount() + info.getStatistics().getHitCount());
            stats.setMissCount(stats.getMissCount() + info.getStatistics().getMissCount());
            stats.UpdateCount(stats.getCount() + info.getStatistics().getCount());
            stats.setMaxCount(stats.getMaxCount() + info.getStatistics().getMaxCount());
            if (info.getStatistics().getMaxCount() == 0) {
                zeroSeen = true;
            }
        }

        stats.setMaxSize(s.getLocalNode().getStatistics().getMaxSize());

        if (zeroSeen) {
            stats.setMaxCount(0);
        }
        return stats;
    }

    /**
     * Combines the collected statistics of the nodes, in a replicated
     * environment.
     *
     * @return
     */
    public static CacheStatistics CombineReplicatedStatistics(ClusterCacheStatistics s) {
        CacheStatistics stats = new CacheStatistics();
        if (s.getNodes() == null) {
            return stats;
        }

        for (int i = 0; i < s.getNodes().size(); i++) {
            Object tempVar = s.getNodes().get(i);
            NodeInfo info = (NodeInfo) ((tempVar instanceof NodeInfo) ? tempVar : null);
            if (info == null || info.getStatistics() == null) {
                continue;
            }

            stats.setHitCount(stats.getHitCount() + info.getStatistics().getHitCount());
            stats.setMissCount(stats.getMissCount() + info.getStatistics().getMissCount());
        }

        stats.UpdateCount(s.getLocalNode().getStatistics().getCount());
        stats.setMaxCount(s.getLocalNode().getStatistics().getMaxCount());
        stats.setMaxSize(s.getLocalNode().getStatistics().getMaxSize());

        stats.setSessionCount(s.getLocalNode().getStatistics().getSessionCount());
        return stats;
    }

    public static java.util.HashMap VerifyAllTrueResopnses(RspList results) throws SuspectedException, TimeoutException {
        java.util.HashMap res = new java.util.HashMap();
        if (results == null) {
            return res;
        }
        Rsp rsp = null;
        for (int i = 0; i < results.size(); i++) {
            rsp = (Rsp) results.elementAt(i);

            if (rsp.wasSuspected()) {
                throw new SuspectedException(rsp.getSender());
            }

            if (!rsp.wasReceived()) {
                throw new TimeoutException("Operation timeout");
            }
        }
        if (rsp != null) {
            return (java.util.HashMap) rsp.getValue();
        } else {
            return null;
        }
    }
}
