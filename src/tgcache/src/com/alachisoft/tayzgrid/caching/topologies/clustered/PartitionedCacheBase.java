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
import com.alachisoft.tayzgrid.caching.util.LazyKeysetEnumerator;
import com.alachisoft.tayzgrid.caching.util.AggregateEnumerator;
import com.alachisoft.tayzgrid.caching.util.ClusterHelper;
import com.alachisoft.tayzgrid.caching.topologies.ICacheEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.CacheAddResult;
import com.alachisoft.tayzgrid.caching.topologies.IClusterEventsListener;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.datagrouping.DataAffinity;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.OperationContext;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.monitoring.ServerMonitor;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.common.exceptions.SuspectedException;
import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;
import com.alachisoft.tayzgrid.common.ResetableIterator;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;

/**
 * A class to serve as the base for partitioned clustered cache implementations.
 */
public class PartitionedCacheBase extends ClusterCacheBase {

    /**
     * An info object that is passed as identity of the members, i.e.,
     * additional data with the Address object. This will help the partition
     * determine legitimate members as well as gather useful information about
     * member configuration. Load balancer might be a good consumer of this
     * information.
     */
    public static class Identity extends NodeIdentity implements ICompactSerializable {

        /**
         * @deprecated Only used for Serialization
         */
        @Deprecated
        public Identity() {
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
    protected static final String MCAST_DOMAIN = ".p20";

    /**
     * Overloaded constructor. Takes the listener as parameter.
     *
     * @param listener listener of Cache events.
     */
    public PartitionedCacheBase(java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context) {
        super(properties, listener, context);
    }

    /**
     * Overloaded constructor. Takes the listener as parameter.
     *
     * @param listener listener of Cache events.
     */
    public PartitionedCacheBase(java.util.Map properties, ICacheEventsListener listener, CacheRuntimeContext context, IClusterEventsListener clusterListener) {
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
                getContext().getCacheLog().Warn("PartitionedCacheBase.AuthenticateNode()", "A non-recognized node attempted to join cluster -> " + address);
                return false;
            }
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    protected final java.util.HashMap Clustered_LockBuckets(java.util.ArrayList bucketIds, Address owner, Address targetNode) throws OperationFailedException {
        Object result = null;
        try {
            Function function = new Function(OpCodes.LockBuckets.getValue(), new Object[]{
                bucketIds, owner
            }, false);
            result = getCluster().SendMessage(targetNode, function, GroupRequest.GET_FIRST, false);

            if (result == null) {
                return null;
            }

            return (java.util.HashMap) ((result instanceof java.util.HashMap) ? result : null);
        } catch (Exception ex) {
            throw new OperationFailedException(ex);
        }
    }

    protected final void Clustered_AckStateTxfrCompleted(Address targetNode, java.util.ArrayList bucketIds) throws OperationFailedException, GeneralFailureException {
        try {
            Function func = new Function(OpCodes.AckStateTxfr.getValue(), bucketIds, true);
            getCluster().SendMessage(targetNode, func, GroupRequest.GET_NONE);
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the list of keys fron the cache for the given group or sub
     * group.
     */
    protected final java.util.ArrayList Clustered_GetKeys(java.util.ArrayList dests, String group, String subGroup) throws GeneralFailureException, CacheException {
        java.util.ArrayList list = new java.util.ArrayList();
        try {
            Function func = new Function(OpCodes.GetKeys.getValue(), new Object[]{
                group, subGroup
            }, true);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return null;
            }

            ClusterHelper.ValidateResponses(results, java.util.ArrayList.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.ArrayList.class);

            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    java.util.ArrayList cList = (java.util.ArrayList) rsp.getValue();
                    if (cList != null) {
                        list.addAll(cList);
                    }
                }
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retrieve the list of keys fron the cache for the given group or sub
     * group.
     */
    protected final java.util.HashMap Clustered_GetData(String group, String subGroup, OperationContext operationContext) throws CacheException {
        java.util.HashMap table = new java.util.HashMap();
        try {
            Function func = new Function(OpCodes.GetData.getValue(), new Object[]{
                group, subGroup, operationContext
            }, true);
            RspList results = getCluster().BroadcastToServers(func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return null;
            }

            ClusterHelper.ValidateResponses(results, java.util.HashMap.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.HashMap.class);

            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    java.util.HashMap cTable = (java.util.HashMap) rsp.getValue();
                    if (cTable != null) {
                        Iterator ide = cTable.entrySet().iterator();
                        Map.Entry KeyValue;
                        while (ide.hasNext()) {
                            KeyValue = (Map.Entry) ide.next();
                            Object Key = KeyValue.getKey();
                            Object Value = KeyValue.getValue();
                            table.put(Key, Value);
                        }
                    }
                }
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }

        return table;
    }

    protected final java.util.HashMap Clustered_GetTag(java.util.ArrayList dests, String[] tags, TagComparisonType comparisonType, boolean excludeSelf, OperationContext operationContext) throws CacheException {
        java.util.HashMap keyValues = new java.util.HashMap();

        try {
            Function func = new Function(OpCodes.GetTag.getValue(), new Object[]{
                tags, comparisonType, operationContext
            }, excludeSelf);
            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return null;
            }

            ClusterHelper.ValidateResponses(results, java.util.HashMap.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.HashMap.class);

            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    java.util.Map entries = (java.util.Map) rsp.getValue();
                    if (entries != null) {
                        Iterator ide = entries.entrySet().iterator();
                        Map.Entry KeyValue;
                        while (ide.hasNext()) {
                            KeyValue = (Map.Entry) ide.next();
                            Object Key = KeyValue.getKey();
                            Object Value = KeyValue.getValue();
                            keyValues.put(Key, Value);
                        }
                    }
                }
            }

            return keyValues;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    protected final java.util.ArrayList Clustered_GetKeysByTag(java.util.ArrayList dests, String[] tags, TagComparisonType comparisonType, boolean excludeSelf, OperationContext operationContext) throws CacheException {
        java.util.ArrayList keys = new java.util.ArrayList();

        try {
            Function func = new Function(OpCodes.GetKeysByTag.getValue(), new Object[]{
                tags, comparisonType, operationContext
            }, excludeSelf);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false, getCluster().getTimeout() * 10);
            if (results == null) {
                return null;
            }

            ClusterHelper.ValidateResponses(results, java.util.ArrayList.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.ArrayList.class);

            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    java.util.Collection entries = (java.util.Collection) rsp.getValue();
                    if (entries != null) {
                        java.util.Iterator ide = entries.iterator();
                        while (ide.hasNext()) {
                            keys.add(ide.next());
                        }
                    }
                }
            }

            return keys;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Retrieve the list of keys fron the cache for the given group or sub
     * group.
     */
    protected final java.util.HashMap Clustered_GetData(java.util.ArrayList dests, String group, String subGroup, OperationContext operationContext) throws CacheException {
        java.util.HashMap table = new java.util.HashMap();
        try {
            Function func = new Function(OpCodes.GetData.getValue(), new Object[]{
                group, subGroup, operationContext
            }, true);
            RspList results = getCluster().Multicast(dests, func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return null;
            }

            ClusterHelper.ValidateResponses(results, java.util.HashMap.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, java.util.HashMap.class);

            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    java.util.HashMap cTable = (java.util.HashMap) rsp.getValue();
                    if (cTable != null) {
                        Iterator ide = cTable.entrySet().iterator();
                        Map.Entry KeyValue;
                        while (ide.hasNext()) {
                            KeyValue = (Map.Entry) ide.next();
                            Object Key = KeyValue.getKey();
                            Object Value = KeyValue.getValue();
                            table.put(Key, Value);
                        }
                    }
                }
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }

        return table;
    }

    /**
     * Gets the data group info of the item. Node containing the item will
     * return the data group information.
     *
     * @param key
     * @return Result of the operation On the other ndoe handleGetGroupInfo is
     * called
     */
    public final ClusteredOperationResult Clustered_GetGroupInfo(Object key, OperationContext operationContext) throws GeneralFailureException {
        try {
            return Clustered_GetGroupInfo(getCluster().getServers(), key, true, operationContext);
        } catch (CacheException cacheException) {
            throw new GeneralFailureException(cacheException.getMessage(), cacheException);
        }
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
            getCluster().BroadcastToServers(func, GroupRequest.GET_ALL, false);
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    /**
     * Add the object to specfied node in the cluster.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method either invokes <see cref="handleAdd"/> on every server-node
     * in the cluster.
     *
     */
    protected final CacheAddResult Clustered_Add(Address dest, Object key, CacheEntry cacheEntry, String taskId, OperationContext operationContext) throws SuspectedException, TimeoutException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PartCacheBase.Add_1", "");
        }
        CacheAddResult retVal = CacheAddResult.Success;
        try {
            Function func = new Function(OpCodes.Add.getValue(), new Object[]{
                key, cacheEntry.CloneWithoutValue(), taskId, operationContext
            });
            Object[] userPayLoad = null;
            if (cacheEntry.getValue() instanceof CallbackEntry) {
                CallbackEntry cbEntry = ((CallbackEntry) cacheEntry.getValue());
                userPayLoad = cbEntry.getUserData();
            } else {
                userPayLoad = cacheEntry.getUserData();
            }

            func.setUserPayload(userPayLoad);
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return retVal;
            }
            if (result instanceof CacheAddResult) {
                retVal = (CacheAddResult) result; 
            } else if (result instanceof Exception) {
                throw (Exception) result;
            }
        } catch (SuspectedException se) {
            throw se;
        } catch (TimeoutException te) {
            throw te;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    /**
     * Add the ExpirationHint to a specfied node in the cluster.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method either invokes <see cref="handleAdd"/> on every server-node
     * in the cluster.
     *
     */
    protected final boolean Clustered_Add(Address dest, Object key, ExpirationHint eh, OperationContext operationContext) throws CacheException, SuspectedException {
        boolean retVal = false;
        try {
            Function func = new Function(OpCodes.AddHint.getValue(), new Object[]{
                key, eh, operationContext
            });
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return retVal;
            }
            retVal = (Boolean) result; 
        } catch (SuspectedException sus) {
            throw sus;
        } catch (CacheException e) {
            if (e.getMessage().contains("I am no more the owner of this bucket")) {
                throw new StateTransferException(e.getMessage(), e);
            }
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

  

    /**
     * Add the object to specfied node in the cluster.
     *
     * @param key key of the entry.
     * @return cache entry.
     *
     * This method either invokes <see cref="handleAdd"/> on every server-node
     * in the cluster.
     *
     */
    protected final java.util.HashMap Clustered_Add(Address dest, Object[] keys, CacheEntry[] cacheEntries, String taskId, OperationContext operationContext) throws TimeoutException, SuspectedException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PartCacheBase.AddBlk", "");
        }

        java.util.HashMap retVal = null;
        try {
            Function func = new Function(OpCodes.Add.getValue(), new Object[]{
                keys, cacheEntries, taskId, operationContext
            });
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST);
            if (result == null) {
                return retVal;
            }
            retVal = (java.util.HashMap) result; 
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }

    /**
     * Verifies that joining node has no data integrity conflicts with other
     * nodes of the cluster.
     *
     * @return True, if no data integrity conflicts found, other wise false Each
     * partitioned node can have his data affinity. Data groups other than the
     * strongly affiliated groups can be loadbalanced to any of the existing
     * node. In such a situaltion if a new node joins and it has strong affinity
     * with the groups whose data was previously distributed evenly, then a data
     * integrity conflict arises. To avoid such conflicts each joining node
     * first ves that no other node on the cluster has data of his groups.
     * If it is so, then he has to leave the cluster.
     */
    public final boolean VerifyDataIntegrity() {
        boolean integrityVied = true;
        boolean integrityIssue = false;

        try {
            if (getCluster().getServers().size() > 1) {
                if (_stats != null && _stats.getLocalNode().getDataAffinity() != null) {
                    DataAffinity affinity = _stats.getLocalNode().getDataAffinity();

                    if (affinity.getGroups() != null && affinity.getGroups().size() > 0) {
                        Function fun = new Function(OpCodes.VerifyDataIntegrity.getValue(), (Object) affinity.getGroups(), false);
                        RspList results = getCluster().BroadcastToServers(fun, GroupRequest.GET_ALL, false);

                        if (results != null) {
                            ClusterHelper.ValidateResponses(results, Boolean.class, getName());
                            Rsp response;
                            for (int i = 0; i < results.size(); i++) {
                                response = (Rsp) results.elementAt(i);
                                if (response.wasReceived()) {
                                    integrityIssue = (Boolean) response.getValue();
                                    if (integrityIssue) {
                                        getContext().getCacheLog().Error("PartitionedCacheBase.Verifydataintegrity()", "data integrity issue from "
                                                + response.getSender().toString());
                                        integrityVied = false;
                                    }
                                } else {
                                    getContext().getCacheLog().Error("PartitionedCacheBase.Verifydataintegrity()", "data integrity vication not received from "
                                            + response.getSender().toString());
                                    integrityVied = false;
                                    break;
                                }
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            if (getContext() != null) {
                getContext().getCacheLog().Error("PartitionedCacheBase.Verifydataintegrity()", e.toString());
            }
            integrityVied = false;
        }

        return integrityVied;
    }

    protected final QueryResultSet Clustered_Search(java.util.ArrayList dests, String queryText, java.util.Map values, boolean excludeSelf, OperationContext operationContext) throws CacheException {
        QueryResultSet resultSet = new QueryResultSet();

        try {
            Function func = new Function(OpCodes.Search.getValue(), new Object[]{
                queryText, values, operationContext
            }, excludeSelf);
            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL, false);

            if (results == null) {
                return null;
            }

            ClusterHelper.ValidateResponses(results, QueryResultSet.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, QueryResultSet.class);

            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    QueryResultSet cRestultSet = (QueryResultSet) rsp.getValue();
                    resultSet.Compile(cRestultSet);
                }
            }

            return resultSet;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
    }

    protected final QueryResultSet Clustered_SearchEntries(java.util.ArrayList dests, String queryText, java.util.Map values, boolean excludeSelf, OperationContext operationContext) throws GeneralFailureException, CacheException {
        QueryResultSet resultSet = new QueryResultSet();

        try {
            Function func = new Function(OpCodes.SearchEntries.getValue(), new Object[]{
                queryText, values, operationContext
            }, excludeSelf);
            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL, false);
            if (results == null) {
                return null;
            }

            ClusterHelper.ValidateResponses(results, QueryResultSet.class, getName());
            java.util.ArrayList rspList = ClusterHelper.GetAllNonNullRsp(results, QueryResultSet.class);

            if (rspList.size() <= 0) {
                return null;
            } else {
                java.util.Iterator im = rspList.iterator();
                while (im.hasNext()) {
                    Rsp rsp = (Rsp) im.next();
                    QueryResultSet cResultSet = (QueryResultSet) rsp.getValue();
                    resultSet.Compile(cResultSet);
                }
            }

            return resultSet;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
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
    protected final CacheInsResultWithEntry Clustered_Insert(Address dest, Object key, CacheEntry cacheEntry, String taskId, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws GeneralFailureException, CacheException, TimeoutException, SuspectedException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PartCacheBase.Insert", "");
        }

        CacheInsResultWithEntry retVal = new CacheInsResultWithEntry();
        try {
            Function func = new Function(OpCodes.Insert.getValue(), new Object[]{
                key, cacheEntry.CloneWithoutValue(), taskId, lockId, accessType, version, operationContext
            });
            Object[] userPayLoad = null;
            if (cacheEntry.getValue() instanceof CallbackEntry) {
                CallbackEntry cbEntry = ((CallbackEntry) cacheEntry.getValue());
                userPayLoad = cbEntry.getUserData();
            } else {
                userPayLoad = cacheEntry.getUserData();
            }

            func.setUserPayload(userPayLoad);
            func.setResponseExpected(true);
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST, false);
            if (result == null) {
                return retVal;
            }

            retVal = (CacheInsResultWithEntry) ((OperationResponse) result).SerializablePayload;
            if (retVal.getEntry() != null) {
                retVal.getEntry().setValue(((OperationResponse) result).UserPayload);
            }
        } catch (SuspectedException se) {
            throw se;
        } catch (TimeoutException te) {
            throw te;
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
    protected final java.util.HashMap Clustered_Insert(Address dest, Object[] keys, CacheEntry[] cacheEntries, String taskId, OperationContext operationContext) throws TimeoutException, SuspectedException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PartCacheBase.InsertBlk", "");
        }

        java.util.HashMap inserted = null;
        try {
            Function func = new Function(OpCodes.Insert.getValue(), new Object[]{
                keys, cacheEntries, taskId, operationContext
            });
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST, false);
            if (result == null) {
                return new java.util.HashMap();
            }
            inserted = (java.util.HashMap) result;
        } catch (TimeoutException te) {
            throw te;
        } catch (SuspectedException se) {
            throw se;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return inserted;
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
    protected final CacheEntry Clustered_Remove(Address dest, Object key, ItemRemoveReason ir, CallbackEntry cbEntry, String taskId, String providerName, boolean notify, Object lockId, long version, LockAccessType accessType, OperationContext operationContext) throws SuspectedException, TimeoutException, CacheException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PartCacheBase.Remove", "");
        }

        CacheEntry retVal = null;
        try {
            Function func = new Function(OpCodes.Remove.getValue(), new Object[]{
                key, ir, notify, cbEntry, taskId, lockId, accessType, version, providerName, operationContext
            }, false);
            func.setResponseExpected(true);
            Object result = getCluster().SendMessage(dest, func, GroupRequest.GET_FIRST, false);
            if (result != null) {
                retVal = (CacheEntry) ((((OperationResponse) result).SerializablePayload instanceof CacheEntry) ? ((OperationResponse) result).SerializablePayload : null);
                if (retVal != null) {
                    retVal.setValue(((OperationResponse) result).UserPayload);
                }
            }
        } catch (SuspectedException se) {
            throw se;
        } catch (TimeoutException te) {
            throw te;
        } catch (CacheException e) {
            if (e.getMessage().contains("I am no more the owner of this bucket")) {
                throw new StateTransferException(e.getMessage(), e);
            }

            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
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
    protected final java.util.HashMap Clustered_Remove(Address dest, Object[] keys, ItemRemoveReason ir, CallbackEntry cbEntry, String taskId, String providerName, boolean notify, OperationContext operationContext) throws GeneralFailureException, CacheException, SuspectedException {
        if (ServerMonitor.getMonitorActivity()) {
            ServerMonitor.LogClientActivity("PartCacheBase.RemoveBlk", "");
        }

        java.util.HashMap removedEntries = new java.util.HashMap();
        java.util.ArrayList dests = new java.util.ArrayList();
        dests.add(dest);
        try {
            Function func = new Function(OpCodes.Remove.getValue(), new Object[]{
                keys, ir, notify, cbEntry, taskId, providerName, operationContext
            }, false);
            RspList results = getCluster().Multicast(dests, func, getGetFirstResponse(), false);

            if (results == null) {
                return removedEntries;
            }

            if (results.getSuspectedMembers().size() == dests.size()) {
                //All the members of this group has gone down.
                //we must try this operation on some other group.
                throw new SuspectedException("operation failed because the group member was suspected");
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
                Map.Entry KeyValue;
                while (ide.hasNext()) {
                    KeyValue = (Map.Entry) ide.next();
                    Object Key = KeyValue.getKey();
                    Object Value = KeyValue.getValue();
                    removedEntries.put(Key, Value);
                }
            }
        } catch (SuspectedException sus) {
            throw sus;
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return removedEntries;
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
    protected final java.util.HashMap Clustered_RemoveGroup(String group, String subGroup, boolean notify, OperationContext operationContext) throws CacheException {
        java.util.HashMap removedEntries = new java.util.HashMap();
        try {
            Function func = new Function(OpCodes.RemoveGroup.getValue(), new Object[]{
                group, subGroup, notify, operationContext
            }, false);
            RspList results = getCluster().BroadcastToServers(func, GroupRequest.GET_ALL, false);

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
                Map.Entry KeyValue;
                while (ide.hasNext()) {
                    KeyValue = (Map.Entry) ide.next();
                    Object Key = KeyValue.getKey();
                    Object Value = KeyValue.getValue();
                    removedEntries.put(Key, Value);
                }
            }
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }

        return removedEntries;
    }

    protected final java.util.HashMap Clustered_RemoveByTag(java.util.ArrayList dests, String[] tags, TagComparisonType comparisonType, boolean notify, boolean excludeSelf, OperationContext operationContext) throws CacheException {
        java.util.HashMap removedEntries = new java.util.HashMap();
        try {
            Function func = new Function(OpCodes.RemoveByTag.getValue(), new Object[]{
                tags, comparisonType, notify, operationContext
            }, excludeSelf);
            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL, false);

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
        }
        return removedEntries;
    }
   



    /**
     * provides Enumerator over cache partitions
     */
    public static class LazyPartitionedKeysetEnumerator extends LazyKeysetEnumerator {

        /**
         * Holder for current dictionary entry.
         */
        private Address _address;
        private boolean _isLocalEnumerator;

        /**
         * Constructor
         *
         * @param cache
         * @param keyList
         */
        public LazyPartitionedKeysetEnumerator(PartitionedCacheBase cache, Object[] keyList, Address address, boolean isLocalEnumerator) {
            super(cache, keyList, false);
            _address = address;
            _isLocalEnumerator = isLocalEnumerator;
        }

        /**
         * Does the lazy loading of object. This method is virtual so containers
         * can customize object fetching logic.
         *
         * @param key
         * @return
         */
        @Override
        protected Object FetchObject(Object key, OperationContext operationContext) throws TimeoutException, CacheException, SuspectedException, OperationFailedException, LockingException {
            PartitionedServerCache ps = (PartitionedServerCache) ((_cache instanceof PartitionedServerCache) ? _cache : null);

            if (_isLocalEnumerator) {
                return ps.Local_Get(key, operationContext);
            }

            return ps.Clustered_Get(_address, key, operationContext);
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
        public ClusteredEnumerator(PartitionedCacheBase cache, Address address, Object[] keyList) {
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
        protected Object FetchObject(Object key, OperationContext operationContext) throws TimeoutException, CacheException, SuspectedException {
            PartitionedCacheBase cache = (PartitionedCacheBase) ((_cache instanceof PartitionedCacheBase) ? _cache : null);
            return cache.Clustered_Get(_targetNode, key, operationContext);
        }
    }

    /**
     * Returns a .NET IEnumerator interface so that a client should be able to
     * iterate over the elements of the cache store.
     *
     * @return Iterator enumerator.
     */
    protected final Iterator Clustered_GetEnumerator(Address targetNode) throws GeneralFailureException, CacheException {
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

    /**
     * Returns a .NET IEnumerator interface so that a client should be able to
     * iterate over the elements of the cache store.
     *
     * @return Iterator enumerator.
     */
    protected final ResetableIterator Clustered_GetEnumerator(java.util.List dests, ResetableIterator local) throws GeneralFailureException, CacheException {
        ResetableIterator retVal = null;
        try {
            Function func = new Function(OpCodes.KeyList.getValue(), null);
            RspList results = getCluster().BroadcastToMultiple(dests, func, GroupRequest.GET_ALL, getCluster().getTimeout() * 10, false);
            if (results == null) {
                return retVal;
            }

            ClusterHelper.ValidateResponses(results, Object[].class, getName());

            Rsp rsp = null;
            java.util.ArrayList validRsps = new java.util.ArrayList();
            for (int i = 0; i < results.size(); i++) {
                rsp = (Rsp) results.elementAt(i);

                if (rsp.getValue() != null) {
                    validRsps.add(rsp);
                }
            }

            int index = (local == null ? 0 : 1);
            int totalEnums = validRsps.size() + index;
            ResetableIterator[] enums = new ResetableIterator[totalEnums];
            if (local != null) {
                enums[0] = local;
            }
            for (int i = 0; i < validRsps.size(); i++) {
                rsp = (Rsp) validRsps.get(i);
                Object tempVar = rsp.getValue();
                Object tempVar2 = rsp.getSender();
                enums[index++] = new LazyPartitionedKeysetEnumerator(this, (Object[]) ((tempVar instanceof Object[]) ? tempVar : null), (Address) ((tempVar2 instanceof Address) ? tempVar2 : null), false);
            }
            retVal = new AggregateEnumerator(enums);
        } catch (CacheException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralFailureException(e.getMessage(), e);
        }
        return retVal;
    }
}
