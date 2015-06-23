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

import com.alachisoft.tayzgrid.cluster.util.RspList;
import com.alachisoft.tayzgrid.cluster.util.Rsp;
import com.alachisoft.tayzgrid.cluster.blocks.GroupRequest;
import com.alachisoft.tayzgrid.cluster.OperationResponse;
import com.alachisoft.tayzgrid.caching.util.CacheHelper;
import com.alachisoft.tayzgrid.caching.util.LazyKeysetEnumerator;
import com.alachisoft.tayzgrid.caching.util.ClusterHelper;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResult;
import com.alachisoft.tayzgrid.caching.topologies.IClusterEventsListener;
import com.alachisoft.tayzgrid.caching.statistics.NodeStatus;
import com.alachisoft.tayzgrid.caching.queries.DeleteQueryResultSet;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.common.ResetableIterator;
import com.alachisoft.tayzgrid.common.LockOptions;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DSWriteBehindOperation;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import tangible.RefObject;

public class ReplicatedCacheBase extends ClusterCacheBase {

    /**
     * An info object that is passed as identity of the members, i.e.,
     * additional data with the Address object. This will help the partition
     * determine legitimate members as well as gather useful information about
     * member configuration. Load balancer might be a good consumer of this
     * information.
     */
    public static class Identity extends NodeIdentity implements ICompactSerializable {

        /**
         * @deprecated Only to be used for CompactSerialization
         */
        public Identity() {
            super();
        }

        public Identity(boolean hasStorage, int renderPort, InetAddress renderAddress) {
            super(hasStorage, renderPort, renderAddress);
        }

        @Override
        public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
            super.deserialize(reader);
        }

        @Override
        public void serialize(CacheObjectOutput writer) throws IOException {
            super.serialize(writer);
        }
    }
    /**
     * string suffix used to differentiate group name.
     */
    protected static final String MCAST_DOMAIN = ".r20";

    /**
     * Overloaded constructor. Takes the listener as parameter.
     *
     * @param listener listener of Cache events.
     */
    public ReplicatedCacheBase(java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context) {
        super(properties, listener, context);
    }

    /**
     * Overloaded constructor. Takes the listener as parameter.
     *
     * @param listener listener of Cache events.
     */
    public ReplicatedCacheBase(java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context, IClusterEventsListener clusterListener) {
        super(properties, listener, context, clusterListener);
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     */
    @Override
    public void dispose() {
        super.dispose();
    }

    /**
     * Authenticate the client and see if it is allowed to join the list of
     * valid members.
     *
     * @param address
     * @param identity
     * @return true if the node is valid and belongs to the scheme's cluster
     */
    @Override
    public boolean AuthenticateNode(Address address, NodeIdentity identity) {
        try {
            if (identity == null || !(identity instanceof Identity)) {
                getContext().getCacheLog().Warn("ReplicatedCacheBase.AuthenticateNode()", "A non-recognized node attempted to join cluster -> " + address);
                return false;
            }
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Returns the count of clustered cache items, from a functional node.
     */
    protected final long Clustered_SessionCount(Address targetNode) throws CacheException {
        try {
            Function func = new Function(OpCodes.GetSessionCount.getValue(), null);
            Object result = getCluster().SendMessage(targetNode, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return 0;
            }

            return (Long) result;
        } catch (CacheException e) {
            getCacheLog().Error("ReplicatedCacheBase.Clustered_SessionCount()", e.toString());
            throw e;
        } catch (Exception e) {
            getCacheLog().Error("ReplicatedCacheBase.Clustered_SessionCount()", e.toString());
            throw new GeneralFailureException("Clustered_SessionCount failed, Error: " + e.getMessage(), e);
        }
    }

    @Override
    protected DeleteQueryResultSet Clustered_DeleteQuery(java.util.ArrayList dests, String query, java.util.Map values, boolean notify, boolean isUserOperation, ItemRemoveReason ir, OperationContext operationContext)
            throws Exception {
        DeleteQueryResultSet res = new DeleteQueryResultSet();
        try {
            Function func = new Function(OpCodes.DeleteQuery.getValue(), new Object[]{query, values, notify, isUserOperation, ir, operationContext}, false);
            RspList results = getCluster().Broadcast(func, GroupRequest.GET_ALL, true, com.alachisoft.tayzgrid.common.enums.Priority.Normal);
            if (results == null) {
                return res;
            }
            ClusterHelper.ValidateResponses(results, DeleteQueryResultSet.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, DeleteQueryResultSet.class);
            if (rspList.size() <= 0) {
                return res;
            } else {
                Rsp rsp = (Rsp) rspList.get(0);
                DeleteQueryResultSet result = (DeleteQueryResultSet) rsp.getValue();
                return result;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Removes all entries from the cluster.
     *
     *
     * This method invokes <see cref="handleClear"/> on every server node in the
     * cluster.
     *
     */
    protected final void Clustered_Clear(CallbackEntry cbEntry, String taskId, boolean excludeSelf, OperationContext operationContext) throws GeneralFailureException {
        try {
            Function func = new Function(OpCodes.Clear.getValue(), new Object[]{
                cbEntry, taskId, operationContext
            }, excludeSelf);
            getCluster().BroadcastToServers(func, GroupRequest.GET_ALL);
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the object from the cluster.
     *
     * @param key key of the entry.
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return cache entry.
     */
    protected final CacheEntry Clustered_Get(Address address, Object key, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, LockAccessType accessType, OperationContext operationContext) throws CacheException {
        CacheEntry retVal = null;
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Get", "enter");
            }
            Function func = new Function(OpCodes.Get.getValue(), new Object[]{
                key, operationContext
            });
            Object result = getCluster().SendMessage(address, func, GroupRequest.GET_FIRST, false);
            if (result == null) {
                return retVal;
            }
            retVal = (CacheEntry) ((OperationResponse) result).SerializablePayload;
            if (retVal != null) {
                retVal.setValue(((OperationResponse) result).UserPayload);
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Get", "exit");
            }
        }
        return retVal;
    }

    /**
     * Retrieve the objects from the cluster.
     *
     * @param group group for which keys are needed
     * @param subGroup sub group of the group
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return list of keys
     */
    protected final java.util.ArrayList Clustered_GetKeys(Address address, String group, String subGroup, OperationContext operationContext) throws CacheException {
        java.util.ArrayList retVal = null;
        try {
            Function func = new Function(OpCodes.GetKeys.getValue(), new Object[]{
                group, subGroup, operationContext
            });
            Object result = getCluster().SendMessage(address, func, GroupRequest.GET_FIRST, _asyncOperation);
            if (result == null) {
                return null;
            }
            retVal = (java.util.ArrayList) result;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    /**
     * Retrieve the objects from the cluster.
     *
     * @param group group for which keys are needed
     * @param subGroup sub group of the group
     * @param excludeSelf Set false to do a complete cluster lookup.
     * @return list of keys
     */
    protected final CacheEntry Clustered_GetGroup(Address address, Object key, String group, String subGroup, tangible.RefObject<Long> version, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, TimeSpan lockTimeout, LockAccessType accessType, OperationContext operationContext) throws CacheException {
        CacheEntry retVal = null;
        try {
            Function func = new Function(OpCodes.GetGroup.getValue(), new Object[]{
                key, group, subGroup, operationContext
            });
            Object result = getCluster().SendMessage(address, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return null;
            }
            retVal = (CacheEntry) result;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    /**
     * Gets the data group info of the item. Node containing the item will
     * return the data group information.
     *
     * @param key
     * @return Result of the operation On the other ndoe handleGetGroupInfo is
     * called
     */
    public final ClusteredOperationResult Clustered_GetGroupInfo(Object key, OperationContext operationContext) throws GeneralFailureException, CacheException {
        return Clustered_GetGroupInfo(getCluster().getServers(), key, true, operationContext);
    }

    /**
     * Gets the data group info the items. Node containing items will return a
     * table of Data grop information.
     *
     * @param keys
     * @return /// On the other ndoe handleGetGroupInfo is called
     */
    public final java.util.Collection Clustered_GetGroupInfoBulk(Object[] keys, OperationContext operationContext) throws GeneralFailureException, CacheException {
        return Clustered_GetGroupInfoBulk(getCluster().getServers(), keys, true, operationContext);
    }

    /**
     * Gets data group info the items
     *
     * @param keys Keys of the items
     * @return IDictionary of the data grup info the items
     */
    public final java.util.HashMap Clustered_GetGroupInfoBulkResult(Object[] keys, OperationContext operationContext) throws GeneralFailureException, CacheException {

        java.util.Collection result = Clustered_GetGroupInfoBulk(keys, operationContext);
        ClusteredOperationResult opRes;
        java.util.HashMap infos;
        java.util.HashMap max = null;
        java.util.HashMap infoTable;
        if (result != null) {
            java.util.Iterator ie = result.iterator();
            while (ie.hasNext()) {
                opRes = (ClusteredOperationResult) ie.next();
                if (opRes != null) {
                    infos = (java.util.HashMap) opRes.getResult();
                    if (max == null) {
                        max = infos;
                    } else if (infos.size() > max.size()) {
                        max = infos;
                    }

                }
            }
        }
        infoTable = max;
        return infoTable;
    }

    /**
     * Adds key and value pairs to the cache. If any of the specified key
     * already exists in the cache; it is updated, otherwise a new item is added
     * to the cache.
     *
     * @param keys keys of the entry.
     * @param cacheEntries the cache entries.
     * @return list of keys that failed to be added
     *
     * This method either invokes <see cref="handleInsert"/> on any cluster node
     * or invokes <see cref="Local_Insert"/> locally. The choice of the server
     * node is determined by the
     * <see cref="LoadBalancer"/>. <see cref="Local_Insert"/> triggers either
     * <see cref="OnItemAdded"/> or <see cref="OnItemUpdated"/>, which in turn
     * trigger either an item-added or item-updated cluster-wide notification.
     *
     */
    public final java.util.HashMap Clustered_Insert(Object[] keys, CacheEntry[] cacheEntries, String taskId, boolean notify, OperationContext operationContext) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        /**
         * Wait until the object enters any running status
         */
        java.util.HashMap pEntries = null;

        pEntries = Get(keys, operationContext); //dont remove

        java.util.HashMap existingItems;
        java.util.HashMap jointTable = new java.util.HashMap();
        java.util.HashMap failedTable = new java.util.HashMap();
        java.util.HashMap insertable = new java.util.HashMap();
        java.util.HashMap insertResults = null;
        java.util.ArrayList inserted = new java.util.ArrayList();
        java.util.ArrayList added = new java.util.ArrayList();
        Object[] validKeys;
        CacheEntry[] validEnteries;
        Object[] failedKeys;
        CacheEntry[] failedEnteries;
        int index = 0;
        Object key;

        for (int i = 0; i < keys.length; i++) {
            jointTable.put(keys[i], cacheEntries[i]);
        }

        Object tempVar = jointTable.clone();
        java.util.HashMap keyValTable = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);

        if (jointTable.size() > 0) {
            index = 0;
            validKeys = new Object[jointTable.size()];
            validEnteries = new CacheEntry[jointTable.size()];

            Iterator ide = jointTable.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                key = pair.getKey();
                validKeys[index] = key;
                index += 1;
            }

            java.util.HashMap groups = Clustered_GetGroupInfoBulkResult(validKeys, operationContext);
            if (groups != null) {
                existingItems = groups;
                if (existingItems != null && existingItems.size() > 0) {
                    insertable = CacheHelper.GetInsertableItems(existingItems, jointTable);
                    if (insertable != null) {
                        index = 0;
                        validKeys = new Object[insertable.size()];
                        validEnteries = new CacheEntry[insertable.size()];

                        ide = insertable.entrySet().iterator();
                        CacheEntry entry;
                        while (ide.hasNext()) {
                            Map.Entry pair = (Map.Entry) ide.next();
                            key = pair.getKey();
                            entry = (CacheEntry) pair.getValue();
                            validKeys[index] = key;
                            validEnteries[index] = entry;
                            inserted.add(key);
                            index += 1;
                            jointTable.remove(key);
                        }
                        try {
                            insertResults = Clustered_Insert(getCluster().getServers(), validKeys, validEnteries, taskId, operationContext);
                        } catch (Exception e) {
                            getContext().getCacheLog().Error("ReplicatedServerCacheBase.Insert(Keys)", e.toString());
                            for (int i = 0; i < validKeys.length; i++) {
                                failedTable.put(validKeys[i], e);
                                inserted.remove(validKeys[i]);
                            }
                            Clustered_Remove(validKeys, ItemRemoveReason.Removed, null, null, null, false, operationContext);
                        }

                        ide = existingItems.entrySet().iterator();
                        while (ide.hasNext()) {
                            Map.Entry pair = (Map.Entry) ide.next();
                            key = pair.getKey();
                            if (jointTable.containsKey(key)) {
                                failedTable.put(key, new OperationFailedException("Data group of the inserted item does not match the existing item's data group", false));
                                jointTable.remove(key);
                            }
                        }
                    }

                }
            }
        }

        if (jointTable.size() > 0) {
            index = 0;
            validKeys = new Object[jointTable.size()];
            validEnteries = new CacheEntry[jointTable.size()];
            Iterator ide = jointTable.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry pair = (Map.Entry) ide.next();
                key = pair.getKey();
                validKeys[index] = key;
                validEnteries[index] = (CacheEntry) pair.getValue();
                added.add(key);
                index += 1;
            }
            for (int i = 0; i < validKeys.length; i++) {
                key = validKeys[i];
                if (jointTable.containsKey(key)) {
                    jointTable.remove(key);
                }
            }
            try {
                insertResults = null;
                insertResults = Clustered_Insert(getCluster().getServers(), validKeys, validEnteries, taskId, operationContext);
            } catch (Exception e) {
                getContext().getCacheLog().Error("ReplicatedServerCacheBase.Insert(Keys)", e.toString());
                for (int i = 0; i < validKeys.length; i++) {
                    failedTable.put(validKeys[i], e);
                    added.remove(validKeys[i]);
                }

                Clustered_Remove(validKeys, ItemRemoveReason.Removed, null, null, null, false, operationContext);
            }

        }
        if (insertResults != null) {
            java.util.HashMap failed = CacheHelper.CompileInsertResult(insertResults);
            Iterator Ie = failed.entrySet().iterator();
            while (Ie.hasNext()) {
                Map.Entry pair = (Map.Entry) Ie.next();
                failedTable.put(pair.getKey(), pair.getValue());
            }
        }

        return failedTable;
    }

    @Override
    protected void DequeueWriteBehindTask(String[] taskId, String providerName, OperationContext operationContext) {
        if (taskId == null) {
            return;
        }

        Function func = new Function(OpCodes.WBTCompleted.getValue(), new Object[]{
            taskId, providerName, operationContext
        }, true);
        try {
            getCluster().BroadcastToServers(func, GroupRequest.GET_NONE, true);
        } catch (IOException iOException) {
            this.getCacheLog().Error("DequeueWriteBehindTask", iOException.getMessage());
        } catch (ClassNotFoundException classNotFoundException) {
            this.getCacheLog().Error("DequeueWriteBehindTask", classNotFoundException.getMessage());
        }
    }

    //for atomic
    @Override
    protected void EnqueueWriteBehindOperation(DSWriteBehindOperation operation) {
        if (operation.getTaskId() == null) {
            return;
        }

        Function func = new Function(OpCodes.EnqueueWBOp.getValue(), new Object[]{operation}, true);
        try {
            getCluster().BroadcastToServers(func, GroupRequest.GET_NONE, true);
        } catch (IOException iOException) {
            this.getCacheLog().Error("DequeueWriteBehindTask", iOException.getMessage());
        } catch (ClassNotFoundException classNotFoundException) {
            this.getCacheLog().Error("DequeueWriteBehindTask", classNotFoundException.getMessage());
        }
    }
    //for bulk

    @Override
    protected void EnqueueWriteBehindOperation(java.util.ArrayList operations) {
        if (operations == null) {
            return;
        }
        Function func = new Function(OpCodes.EnqueueWBOp.getValue(), new Object[]{operations}, true);
        try {
            getCluster().BroadcastToServers(func, GroupRequest.GET_NONE, true);
        } catch (IOException iOException) {
            this.getCacheLog().Error("DequeueWriteBehindTask", iOException.getMessage());
        } catch (ClassNotFoundException classNotFoundException) {
            this.getCacheLog().Error("DequeueWriteBehindTask", classNotFoundException.getMessage());
        }
    }

    /**
     * Hanlder for clustered item updated notification.
     *
     * @param info packaged information
     * @return null
     */
    protected Object handleNotifyUpdate(Object info) {
        OperationContext operationContext = null;
        if (info instanceof Object[]) {
            Object[] objs = (Object[]) ((info instanceof Object[]) ? info : null);
            if (objs.length > 1) {
                operationContext = (OperationContext) ((objs[1] instanceof OperationContext) ? objs[1] : null);
            }

            NotifyItemUpdated(objs[0], true, operationContext, null);
        } else {
            NotifyItemUpdated(info, true, operationContext, null);
        }
        return null;
    }

    /**
     * Hanlder for clustered item added notification.
     *
     * @param info packaged information
     * @return null
     */
    protected Object handleNotifyAdd(Object info) {
        NotifyItemAdded(info, true, null, null);
        return null;
    }

    /**
     * Retrieve the objects from the cluster.
     *
     * @param group group for which keys are needed
     * @param subGroup sub group of the group
     * @return key and entry pairs
     */
    protected final java.util.HashMap Clustered_GetData(Address address, String group, String subGroup, OperationContext operationContext) throws CacheException {
        java.util.HashMap retVal = null;
        try {
            Function func = new Function(OpCodes.GetData.getValue(), new Object[]{
                group, subGroup, operationContext
            });
            Object result = getCluster().SendMessage(address, func, GroupRequest.GET_FIRST, _asyncOperation);
            if (result == null) {
                return new java.util.HashMap();
            }
            retVal = (java.util.HashMap) result;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    protected final void Clustered_UnLock(Object key, Object lockId, boolean isPreemptive, OperationContext operationContext) throws CacheException {
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Unlock", "enter");
            }
            Function func = new Function(OpCodes.UnLockKey.getValue(), new Object[]{
                key, lockId, isPreemptive, operationContext
            }, false);
            getCluster().BroadcastToMultiple(getCluster().getServers(), func, GroupRequest.GET_ALL);
        } catch (Exception e) {
            throw new CacheException(e);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Unlock", "exit");
            }
        }
    }

    protected final boolean Clustered_Lock(Object key, LockExpiration lockExpiration, tangible.RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws CacheException {
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Lock", "enter");
            }
            Function func = new Function(OpCodes.LockKey.getValue(), new Object[]{
                key, lockId.argvalue, lockDate.argvalue, lockExpiration, operationContext
            }, false);

            RspList results = getCluster().BroadcastToMultiple(getCluster().getServers(), func, GroupRequest.GET_ALL);

            try {
                ClusterHelper.ValidateResponses(results, LockOptions.class, getName());
            } catch (LockingException le) {
                //release the lock preemptively...
                Clustered_UnLock(key, null, true, operationContext);
                return false;
            }

            return ClusterHelper.FindAtomicLockStatusReplicated(results, lockId, lockDate);
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Lock", "exit");
            }
        }
    }

    protected final LockOptions Clustered_IsLocked(Object key, RefObject<Object> lockId, tangible.RefObject<java.util.Date> lockDate, OperationContext operationContext) throws CacheException {
        try {
            Function func = new Function(OpCodes.IsLocked.getValue(), new Object[]{
                key, lockId.argvalue, lockDate.argvalue, operationContext
            }, false);

            RspList results = getCluster().BroadcastToMultiple(getCluster().getServers(), func, GroupRequest.GET_ALL);

            try {
                ClusterHelper.ValidateResponses(results, LockOptions.class, getName());
            } catch (LockingException le) {
                //release the lock preemptively...
                Clustered_UnLock(key, null, true, operationContext);
                return null;
            }

            return ClusterHelper.FindAtomicIsLockedStatusReplicated(results, lockId, lockDate);
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }
    /**
     * Add the object to the cluster. Does load balancing as well.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method either invokes <see cref="handleAdd"/> on one of the server
     * nodes in the cluster, or invokes <see cref="Local_Add"/> locally.
     *
     */
    protected final CacheAddResult Clustered_Add(java.util.List dests, Object key, CacheEntry cacheEntry, String taskId, OperationContext operationContext) throws CacheException {
        CacheAddResult result = CacheAddResult.Failure;
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Add", "enter");
            }
            boolean writeThruEnable = _context.getDsMgr() != null;
            CacheEntry entryToBeSent = cacheEntry;
            if (writeThruEnable) {
                entryToBeSent = cacheEntry.CloneWithoutValue();
            }

            /**
             * Ask every server to add the object, except myself.
             */
            Function func = new Function(OpCodes.Add.getValue(), new Object[]{
                key, entryToBeSent, taskId, operationContext
            }, false, key);
            Object[] userPayLoad = null;
            if (cacheEntry.getValue() instanceof CallbackEntry) {
                CallbackEntry cbEntry = ((CallbackEntry) cacheEntry.getValue());
                userPayLoad = cbEntry.getUserData();
                if (!writeThruEnable) {
                    cbEntry.setValue(null);
                }
            } else {
                userPayLoad = cacheEntry.getUserData();
                if (!writeThruEnable) {
                    cacheEntry.setValue(null);
                }
            }

            func.setUserPayload(userPayLoad);
            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL, _asyncOperation);

            ClusterHelper.ValidateResponses(results, CacheAddResult.class, getName());

            /**
             * Check if the operation failed on any node.
             */
            result = ClusterHelper.FindAtomicAddStatusReplicated(results);
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Add", "exit");
            }
        }
        return result;
    }

    /**
     * Add the object to the cluster. Does load balancing as well.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method either invokes <see cref="handleAdd"/> on one of the server
     * nodes in the cluster, or invokes <see cref="Local_Add"/> locally.
     *
     */
    protected final boolean Clustered_Add(java.util.List dests, Object key, ExpirationHint eh, OperationContext operationContext) throws CacheException {
        boolean result = false;
        try {
            /**
             * Ask every server to add the object, except myself.
             */
            Function func = new Function(OpCodes.AddHint.getValue(), new Object[]{
                key, eh, operationContext
            }, false, key);
            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL, _asyncOperation);

            ClusterHelper.ValidateResponses(results, Boolean.class, getName());

            /**
             * Check if the operation failed on any node.
             */
            result = ClusterHelper.FindAtomicAddHintReplicated(results);
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return result;
    }

   

    /**
     * Add the object to the cluster.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method invokes <see cref="handleAdd"/> on every server-node in the
     * cluster. If the operation fails on any one node the whole operation is
     * considered to have failed and is rolled-back.
     *
     */
    protected final java.util.HashMap Clustered_Add(java.util.List dests, Object[] keys, CacheEntry[] cacheEntries, String taskId, OperationContext operationContext) throws CacheException {
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.AddBlk", "enter");
            }
            /**
             * Ask every server to add the object, except myself.
             */
            Function func = new Function(OpCodes.Add.getValue(), new Object[]{
                keys, cacheEntries, taskId, operationContext
            }, false);
            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL);

            ClusterHelper.ValidateResponses(results, java.util.HashMap.class, getName());

            /**
             * Check if the operation failed on any node.
             */
            return ClusterHelper.FindAtomicBulkInsertStatusReplicated(results);
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.AddBlk", "exit");
            }
        }
    }

    /**
     * Updates or Adds the object to the cluster.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method invokes <see cref="handleInsert"/> on the specified node.
     *
     */
    protected final CacheInsResult Clustered_Insert(Address dest, Object key, CacheEntry cacheEntry, String taskId, OperationContext operationContext) throws CacheException {
        CacheInsResult retVal = CacheInsResult.Failure;
        try {
            boolean writeThruEnable = _context.getDsMgr() != null;
            CacheEntry entryToBeSent = cacheEntry;
            if (writeThruEnable) {
                entryToBeSent = cacheEntry.CloneWithoutValue();
            }

            Function func = new Function(OpCodes.Insert.getValue(), new Object[]{
                key, entryToBeSent, operationContext
            }, false, key);
            Object[] userPayLoad = null;
            if (cacheEntry.getValue() instanceof CallbackEntry) {
                CallbackEntry cbEntry = ((CallbackEntry) cacheEntry.getValue());
                userPayLoad = cbEntry.getUserData();
                if (!writeThruEnable) {
                    cbEntry.setValue(null);
                }
            } else {
                userPayLoad = cacheEntry.getUserData();
                if (!writeThruEnable) {
                    cacheEntry.setValue(null);
                }
            }

            func.setUserPayload(userPayLoad);

            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST, _asyncOperation);
            if (result == null) {
                return retVal;
            }

            retVal = (CacheInsResult) result;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    /**
     * Updates or Adds the objects to the cluster.
     *
     * @param keys keys of the entries.
     * @param cacheEntries cache entries.
     * @return failed keys
     *
     * This method invokes <see cref="handleInsert"/> on the specified node.
     *
     */
    protected final java.util.HashMap Clustered_Insert(Address dest, Object[] keys, CacheEntry[] cacheEntries, String taskId, OperationContext operationContext) throws CacheException {
        java.util.HashMap inserted = null;
        try {
            Function func = new Function(OpCodes.Insert.getValue(), new Object[]{
                keys, cacheEntries, taskId, operationContext
            });
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return new java.util.HashMap();
            }
            inserted = (java.util.HashMap) result;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return inserted;
    }

    protected final CacheInsResultWithEntry Clustered_Insert(java.util.List dests, Object key, CacheEntry cacheEntry, String taskId, Object lockId, LockAccessType accessType, OperationContext operationContext) throws CacheException {
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Insert", "enter");
            }
            boolean writeThruEnable = _context.getDsMgr() != null;
            CacheEntry entryToBeSent = cacheEntry;
            if (writeThruEnable) {
                entryToBeSent = cacheEntry.CloneWithoutValue();
            }

            /**
             * Ask every server to update the object, except myself.
             */
            Function func = new Function(OpCodes.Insert.getValue(), new Object[]{
                key, entryToBeSent, taskId, _statusLatch.IsAnyBitsSet(NodeStatus.Initializing), lockId, accessType, operationContext
            }, false, key);
            Object[] userPayLoad = null;
            if (cacheEntry.getValue() instanceof CallbackEntry) {
                CallbackEntry cbEntry = ((CallbackEntry) cacheEntry.getValue());
                userPayLoad = cbEntry.getUserData();
                if (!writeThruEnable) {
                    cbEntry.setValue(null);
                }
            } else {
                userPayLoad = cacheEntry.getUserData();
                if (!writeThruEnable) {
                    cacheEntry.setValue(null);
                }
            }

            func.setUserPayload(userPayLoad);
            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL, _asyncOperation);

            ClusterHelper.ValidateResponses(results, OperationResponse.class, getName());

            // Check if the operation failed on any node.
            // return ClusterHelper.FindAtomicInsertStatusReplicated(results);
            //Bug Fixed, during state transfer (one node up with the exisiting one) of replicated cache,
            //while client doing insert operaion continously, which incrementing the add/sec counter while the client only performing insert
            //means no need to incrment add/sec counter, need only updat/sec to be incremented
            //so after discussing with butt sahib and sajid, we modify the code here.
            CacheInsResultWithEntry retVal = ClusterHelper.FindAtomicInsertStatusReplicated(results);
            if (retVal != null && retVal.getResult() == CacheInsResult.Success && results != null) {
                for (int i = 0; i < results.getResults().size(); i++) {
                    if (((CacheInsResultWithEntry) ((OperationResponse) results.getResults().get(i)).SerializablePayload).getResult() == CacheInsResult.SuccessOverwrite) {
                        retVal.setResult(CacheInsResult.SuccessOverwrite);
                        break;
                    }
                }
            }
            return retVal;

        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Insert", "exit");
            }
        }
    }

    protected final java.util.HashMap Clustered_Insert(java.util.List dests, Object[] keys, CacheEntry[] cacheEntries, String taskId, OperationContext operationContext) throws CacheException {
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.InsertBlk", "enter");
            }
            /**
             * Ask every server to update the object, except myself.
             */
            Function func = new Function(OpCodes.Insert.getValue(), new Object[]{
                keys, cacheEntries, taskId, operationContext
            }, false);
            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL);

            ClusterHelper.ValidateResponses(results, java.util.HashMap.class, getName());

            /**
             * Check if the operation failed on any node.
             */
            return ClusterHelper.FindAtomicBulkInsertStatusReplicated(results);
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.InsertBlk", "exit");
            }
        }
    }

    /**
     * Remove the object from the cluster.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method invokes <see cref="handleRemove"/> on every server node in
     * the cluster.
     *
     */
    protected final CacheEntry Clustered_Remove(Object key, ItemRemoveReason ir, CallbackEntry cbEntry, String taskId, String providerName, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws CacheException {
        CacheEntry retVal = null;
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Remove", "enter");
            }
            Function func = new Function(OpCodes.Remove.getValue(), new Object[]{
                key, ir, notify, cbEntry, taskId, lockId, accessType, version, providerName, operationContext
            }, false, key);
            RspList results = getCluster().BroadcastToServers(func, GroupRequest.GET_ALL, _asyncOperation);
            if (results == null) {
                return retVal;
            }

            ClusterHelper.ValidateResponses(results, OperationResponse.class, getName());

            Rsp rsp = ClusterHelper.FindAtomicRemoveStatusReplicated(results);
            if (rsp == null) {
                return retVal;
            }

            Object tempVar = rsp.getValue();
            OperationResponse opRes = (OperationResponse) ((tempVar instanceof OperationResponse) ? tempVar : null);
            if (opRes != null) {
                CacheEntry entry = (CacheEntry) ((opRes.SerializablePayload instanceof CacheEntry) ? opRes.SerializablePayload : null);
                if (entry != null) {
                    entry.setValue(opRes.UserPayload);
                }
                return entry;
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.Remove", "exit");
            }
        }
        return retVal;
    }

    /**
     * Remove the objects from the cluster.
     *
     * @param keys keys of the entries.
     * @return list of failed keys
     *
     * This method invokes <see cref="handleRemove"/> on every server node in
     * the cluster.
     *
     */
    protected final java.util.HashMap Clustered_Remove(Object[] keys, ItemRemoveReason ir, CallbackEntry cbEntry, String taskId, String providerName, boolean notify, OperationContext operationContext) throws CacheException {
        java.util.HashMap removedEntries = new java.util.HashMap();
        try {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.RemoveBlk", "enter");
            }
            Function func = new Function(OpCodes.Remove.getValue(), new Object[]{
                keys, ir, notify, cbEntry, taskId, providerName, operationContext
            }, false);
            RspList results = getCluster().BroadcastToServers(func, GroupRequest.GET_ALL);

            if (results == null) {
                return removedEntries;
            }

            ClusterHelper.ValidateResponses(results, java.util.HashMap.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.HashMap.class);

            if (rspList.size() <= 0) {
                return removedEntries;
            }

            java.util.Iterator ia = rspList.iterator();
            while (ia.hasNext()) {
                Rsp rsp = (Rsp) ia.next();
                java.util.HashMap removed = (java.util.HashMap) rsp.getValue();

                Iterator ide = removed.entrySet().iterator();
                while (ide.hasNext()) {
                    Map.Entry pair = (Map.Entry) ide.next();
                    removedEntries.put(pair.getKey(), pair.getValue());
                }
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        } finally {
            if (ServerMonitor.getMonitorActivity()) {
                ServerMonitor.LogClientActivity("RepCacheBase.RemoveBlk", "exit");
            }
        }
        return removedEntries;
    }

    /**
     * Remove the objects from the cluster. For efficiency multiple objects are
     * sent as one.
     *
     * @param keys list of keys to remove.
     * @return true if succeded, false otherwise.
     *
     * This method invokes <see cref="handleRemoveRange"/> on every server node
     * in the cluster.
     *
     */
    protected final boolean Clustered_Remove(Object[] keys, ItemRemoveReason reason, OperationContext operationContext) throws GeneralFailureException, TimeoutException {
        try {
            Function func = new Function(OpCodes.RemoveRange.getValue(), new Object[]{
                keys, reason, operationContext
            }, false);

            RspList results = getCluster().BroadcastToServers(func, GroupRequest.GET_ALL, true);

            if (results != null) {
                for (int i = 0; i < results.size(); i++) {
                    Rsp rsp1 = (Rsp) results.elementAt(i);

                    if (!rsp1.wasReceived()) {
                        getContext().getCacheLog().Error("ReplicatedBase.Remove[]", "timeout_failure :" + rsp1.getSender() + " Keys :" + keys.length);
                        continue;
                    }
                }
            }
            Rsp rsp = ClusterHelper.FindAtomicRemoveStatusReplicated(results, getContext().getCacheLog());

            return true;
        } catch (TimeoutException e2) {
            throw e2;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * provides Enumerator over replicated client cache
     */
    public static class ClusteredEnumerator extends LazyKeysetEnumerator {

        private Address _targetNode;

        /**
         * Constructor
         *
         * @param cache
         * @param keyList
         */
        public ClusteredEnumerator(ReplicatedCacheBase cache, Address address, Object[] keyList) {
            super(cache, keyList, true);
            _targetNode = address;
        }

        /**
         * Does the lazy loading of object. This method is virtual so containers
         * can customize object fetching logic.
         *
         * @param key
         * @return
         */
        @Override
        protected Object FetchObject(Object key, OperationContext operationContext) throws OperationFailedException {
            ReplicatedCacheBase cache = (ReplicatedCacheBase) ((_cache instanceof ReplicatedCacheBase) ? _cache : null);

            Object obj = null;
            boolean doAgain = false;
            do {
                doAgain = false;
                Address targetNode = cache.getCluster().getCoordinator();
                if (targetNode == null) {
                    return null;
                }

                if (cache.getCluster().getIsCoordinator()) {
                    //coordinator has left and i am the new coordinator so need not to do
                    //state transfer.
                    _bvalid = false;
                    return obj;
                }
                try {
                    operationContext.Add(OperationContextFieldName.GenerateQueryInfo, true);
                    obj = cache.Clustered_Get(targetNode, key, operationContext);
                } catch (SuspectedException se) 
                {
                    //coordinator has left; so need to synchronize with the new coordinator.
                    doAgain = true;
                } catch (Exception ex) {
                    throw new OperationFailedException(ex);
                }
            } while (doAgain);

            return obj;
        }

    }

    /**
     * Returns a .NET IEnumerator interface so that a client should be able to
     * iterate over the elements of the cache store.
     *
     * @return IDictionaryEnumerator enumerator.
     */
    protected final ResetableIterator Clustered_GetEnumerator(Address targetNode) throws CacheException {
        try {
            Function func = new Function(OpCodes.KeyList.getValue(), null);
            Object result = getCluster().SendMessage((Address) targetNode.clone(), func, GroupRequest.GET_FIRST, getCluster().getTimeout() * 10);
            if ((result == null) || !(result instanceof Object[])) {
                return null;
            }

            return new ClusteredEnumerator(this, (Address) targetNode.clone(), (Object[]) ((result instanceof Object[]) ? result : null));
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    protected final QueryResultSet Clustered_Search(Address dest, String queryText, java.util.Map values, boolean excludeSelf, OperationContext operationContext) throws CacheException {
        try {
            Function func = new Function(OpCodes.Search.getValue(), new Object[]{
                queryText, values, operationContext
            }, excludeSelf);
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_ALL, false);

            if (result == null) {
                return null;
            }

            return (QueryResultSet) result;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationFailedException("Clustered_Search failed, Error: " + e.getMessage(), e);
        }
    }

    protected final QueryResultSet Clustered_SearchEntries(Address dest, String queryText, java.util.Map values, boolean excludeSelf, OperationContext operationContext) throws CacheException {
        try {
            Function func = new Function(OpCodes.SearchEntries.getValue(), new Object[]{
                queryText, values, operationContext
            }, excludeSelf);
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST, false);

            if (result == null) {
                return null;
            }

            return (QueryResultSet) result;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationFailedException("Clustered_SearchEntries failed, Error: " + e.getMessage(), e);
        }
    }
}
