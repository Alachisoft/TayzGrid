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

import com.alachisoft.tayzgrid.caching.topologies.clustered.results.ClusterOperationResult;
import com.alachisoft.tayzgrid.caching.topologies.clustered.StateTxfrInfo;
import com.alachisoft.tayzgrid.caching.topologies.clustered.PartitionedCacheBase;
import com.alachisoft.tayzgrid.caching.topologies.clustered.PayloadInfo;
import com.alachisoft.tayzgrid.caching.topologies.clustered.ReplicatedCacheBase;
import com.alachisoft.tayzgrid.caching.topologies.CacheInsResultWithEntry;
import com.alachisoft.tayzgrid.caching.topologies.CacheBase;
import com.alachisoft.tayzgrid.caching.statistics.CacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.ClusterCacheStatistics;
import com.alachisoft.tayzgrid.caching.statistics.NodeInfo;
import com.alachisoft.tayzgrid.caching.statistics.BucketStatistics;
import com.alachisoft.tayzgrid.caching.evictionpolicies.TimestampHint;
import com.alachisoft.tayzgrid.caching.evictionpolicies.CounterHint;
import com.alachisoft.tayzgrid.caching.evictionpolicies.PriorityEvictionHint;
import com.alachisoft.tayzgrid.caching.datasourceproviders.WriteBehindQueueRequest;
import com.alachisoft.tayzgrid.caching.datasourceproviders.WriteBehindQueueResponse;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DSWriteOperation;
import com.alachisoft.tayzgrid.caching.datasourceproviders.DSWriteBehindOperation;
import com.alachisoft.tayzgrid.caching.datagrouping.DataAffinity;
import com.alachisoft.tayzgrid.caching.autoexpiration.DependencyHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.IdleExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.AggregateExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.FixedExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.NodeExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.LockExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.FixedIdleExpiration;
import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.AsyncCallbackInfo;
import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.CallbackInfo;
import com.alachisoft.tayzgrid.caching.CompactCacheEntry;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;
import com.alachisoft.tayzgrid.common.ICloneable;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.LockingException;
import com.alachisoft.tayzgrid.caching.topologies.clustered.Function;
import com.alachisoft.tayzgrid.caching.topologies.clustered.AggregateFunction;
import com.alachisoft.tayzgrid.common.StatusInfo;
import com.alachisoft.tayzgrid.common.enums.CacheStatusOnServerContainer;
import com.alachisoft.tayzgrid.common.monitoring.CacheNodeStatistics;
import com.alachisoft.tayzgrid.common.monitoring.ClientProcessStats;
import com.alachisoft.tayzgrid.common.monitoring.ConfiguredCacheInfo;
import com.alachisoft.tayzgrid.common.monitoring.EventViewerEvent;
import com.alachisoft.tayzgrid.common.monitoring.Node;
import com.alachisoft.tayzgrid.common.monitoring.ServerNode;
import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.dom.Cluster;

import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.standard.FormatterServices;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class MiscUtil {



 

    /**
     * Registers types with the Compact Serializatin Framework. Range of
     * reserved typeHandle is (61 - 1000).
     */
    public static void RegisterCompactTypes() throws CacheArgumentException {

        FormatterServices impl = FormatterServices.getDefault();

        impl.registerKnownTypes(CacheEntry.class, (short) 61);
        impl.registerKnownTypes(CounterHint.class, (short) 62);
        impl.registerKnownTypes(TimestampHint.class, (short) 63);
        impl.registerKnownTypes(PriorityEvictionHint.class, (short) 64);
        impl.registerKnownTypes(CacheStatistics.class, (short) 65);
        impl.registerKnownTypes(ClusterCacheStatistics.class, (short) 66);
        impl.registerKnownTypes(NodeInfo.class, (short) 67);
        impl.registerKnownTypes(AggregateExpirationHint.class, (short) 68);
        impl.registerKnownTypes(IdleExpiration.class, (short) 69);
        impl.registerKnownTypes(LockExpiration.class, (short) 135);
        impl.registerKnownTypes(FixedExpiration.class, (short) 70);
        impl.registerKnownTypes(FixedIdleExpiration.class, (short) 72);
        impl.registerKnownTypes(DependencyHint.class, (short) 73);
        impl.registerKnownTypes(CompactCacheEntry.class, (short) 105);
        impl.registerKnownTypes(CallbackEntry.class, (short) 107);
        impl.registerKnownTypes(CallbackInfo.class, (short) 111);
        impl.registerKnownTypes(AsyncCallbackInfo.class, (short) 112);
        impl.registerKnownTypes(BucketStatistics.class, (short) 117);
        impl.registerKnownTypes(CacheInsResultWithEntry.class, (short) 118);
        impl.registerKnownTypes(DSWriteOperation.class, (short) 120);
        impl.registerKnownTypes(DSWriteBehindOperation.class, (short) 121);
        impl.registerKnownTypes(UserBinaryObject.class, (short) 125);
        impl.registerKnownTypes(ClusterOperationResult.class, (short) 141);
        impl.registerKnownTypes(VirtualArray.class, (short) 149);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.locking.LockManager.class, (short) 150);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.datastructures.DistributionMaps.class, (short) 160);
        impl.registerKnownTypes(WriteBehindQueueRequest.class, (short) 122);
        impl.registerKnownTypes(WriteBehindQueueResponse.class, (short) 123);
        impl.registerKnownTypes(StateTxfrInfo.class, (short) 116);
        impl.registerKnownTypes(NodeExpiration.class, (short) 74);
        impl.registerKnownTypes(PartitionedCacheBase.Identity.class, (short) 77);
        impl.registerKnownTypes(PayloadInfo.class, (short) 136);
        impl.registerKnownTypes(DataAffinity.class, (short) 106);

        impl.registerKnownTypes(CompressedValueEntry.class, (short) 133);
        impl.registerKnownTypes(Function.class, (short) 75);
        impl.registerKnownTypes(AggregateFunction.class, (short) 76);
        impl.registerKnownTypes(ReplicatedCacheBase.Identity.class, (short) 78);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.caching.queries.QueryResultSet.class, (short) 151);
        
        impl.registerKnownTypes(com.alachisoft.tayzgrid.caching.OperationContext[].class, (short) 345);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.caching.OperationContext.class, (short) 153);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.caching.OperationID.class, (short) 163);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.LockOptions.class, (short) 261);       
        impl.registerKnownTypes(com.alachisoft.tayzgrid.persistence.Event.class, (short) 272);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.persistence.EventInfo.class, (short) 273);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.datastructures.RecordColumn.class, (short) 274);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.datastructures.RecordSet.class, (short) 275);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.caching.EventCacheEntry.class, (short) 310);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.caching.EventContext.class, (short) 311);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.caching.EventContext[].class, (short) 312);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.datastructures.NewHashmap.class, (short) 346);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.caching.autoexpiration.LockMetaInfo.class,(short) 347);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy.class, (short)348);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.InsertParams.class, (short) 350);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.DeleteParams.class, (short) 351);
        
        //MapReduce.
        impl.registerKnownTypes(com.alachisoft.tayzgrid.mapreduce.MapReduceOperation.class, (short) 393);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.mapreduce.ReducerInput.class, (short) 395);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer.class, (short) 396);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult.class, (short) 397);
		impl.registerKnownTypes(com.alachisoft.tayzgrid.common.mapreduce.TaskOutputPair.class, (short) 398);
        impl.registerKnownTypes(CacheEntry[].class, (short) 400);       
        impl.registerKnownTypes(com.alachisoft.tayzgrid.processor.TayzGridEntryProcessorResult.class, (short) 399);
    }

    public static void RegisterCompact() throws CacheArgumentException {
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.rpcframework.TargetMethodParameter.class, ((Integer) 165).shortValue());

        //<editor-fold defaultstate="collapsed" desc="[Register Cache Server Assemblies]">
        FormatterServices.getDefault().registerKnownTypes(CacheServerConfig.class, ((Integer) 177).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Log.class, ((Integer) 178).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.PerfCounters.class, ((Integer) 179).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.QueryIndex.class, ((Integer) 181).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Class.class, ((Integer) 182).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Type.class, ((Integer) 184).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CompactClass.class, ((Integer) 186).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.BackingSource.class, ((Integer) 187).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Readthru.class, ((Integer) 188).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Writethru.class, ((Integer) 189).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Provider.class, ((Integer) 190).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CacheLoader.class, ((Integer) 191).shortValue());
        

        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Notifications.class, ((Integer) 192).shortValue());
        
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Cleanup.class, ((Integer) 194).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Storage.class, ((Integer) 195).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.EvictionPolicy.class, ((Integer) 196).shortValue());
        FormatterServices.getDefault().registerKnownTypes(Cluster.class, ((Integer) 197).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.dom.Channel.class, ((Integer) 198).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.NodeIdentity.class, ((Integer) 199).shortValue());
        FormatterServices.getDefault().registerKnownTypes(StatusInfo.class, ((Integer) 200).shortValue());

        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.AutoLoadBalancing.class, ((Integer) 203).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ClientNodes.class, ((Integer) 204).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ClientNode.class, ((Integer) 205).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.AlertsNotifications.class, ((Integer) 206).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.EmailNotifications.class, ((Integer) 207).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.AlertsTypes.class, ((Integer) 208).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.NotificationRecipient.class, ((Integer) 209).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.SQLDependencyConfig.class, ((Integer) 210).shortValue());
        
        FormatterServices.getDefault().registerKnownTypes(CacheStatusOnServerContainer.class, ((Integer) 213).shortValue());
        FormatterServices.getDefault().registerKnownTypes(CacheStatistics.class, ((Integer) 65).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ClusterCacheStatistics.class, ((Integer) 66).shortValue());
        FormatterServices.getDefault().registerKnownTypes(NodeInfo.class, ((Integer) 67).shortValue());
        
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Class[].class, ((Integer) 249).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ClientNodeStatusWrapper.class, ((Integer) 250).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Attrib.class, ((Integer) 251).shortValue());

        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Type[].class, ((Integer) 252).shortValue());

        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.PortableClass.class, ((Integer) 253).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.PortableClass[].class, ((Integer) 254).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.AttributeListUnion.class, ((Integer) 255).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.PortableAttribute.class, ((Integer) 256).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.PortableAttribute[].class, ((Integer) 257).shortValue());
        
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.enums.RtContextValue.class, ((Integer) 300).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CacheDeployment.class, ((Integer) 264).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig.class, ((Integer) 265).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Cluster.class, ((Integer) 266).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CacheServerConfigSetting.class, ((Integer) 267).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CacheTopology.class, ((Integer) 268).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Channel.class, ((Integer) 269).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ServerNode.class, ((Integer) 270).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ServersNodes.class, ((Integer) 271).shortValue());
        
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.WriteBehind.class, ((Integer) 276).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.BatchConfig.class, ((Integer) 277).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.datastructures.NewHashmap.class, ((Integer) 346).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.caching.util.HotConfig.class, ((Integer) 349).shortValue());

        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="[Monitor Server Assemblies]">
        FormatterServices.getDefault().registerKnownTypes(CacheNodeStatistics.class, ((Integer) 221).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ServerNode.class, ((Integer) 222).shortValue());

        FormatterServices.getDefault().registerKnownTypes(EventViewerEvent.class, ((Integer) 223).shortValue());

        FormatterServices.getDefault().registerKnownTypes(Node.class, ((Integer) 224).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.net.Address.class, ((Integer) 110).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.monitoring.ClientNode.class, ((Integer) 226).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ConfiguredCacheInfo.class, ((Integer) 227).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ClientProcessStats.class, ((Integer) 228).shortValue());

        FormatterServices.getDefault().registerKnownTypes(CacheNodeStatistics[].class, ((Integer) 229).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ServerNode[].class, ((Integer) 230).shortValue());
        FormatterServices.getDefault().registerKnownTypes(EventViewerEvent[].class, ((Integer) 231).shortValue());

        FormatterServices.getDefault().registerKnownTypes(Node[].class, ((Integer) 232).shortValue());
        FormatterServices.getDefault().registerKnownTypes(Address[].class, ((Integer) 233).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.common.monitoring.ClientNode[].class, ((Integer) 234).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ConfiguredCacheInfo[].class, ((Integer) 235).shortValue());
        FormatterServices.getDefault().registerKnownTypes(ClientProcessStats[].class, ((Integer) 236).shortValue());
       
       
       
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.User.class, ((Integer) 240).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.User[].class, ((Integer) 241).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.NotificationRecipient[].class, ((Integer) 242).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.CompactClass[].class, ((Integer) 243).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Parameter.class, ((Integer) 244).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Parameter[].class, ((Integer) 245).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ProviderAssembly.class, ((Integer) 246).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.ClientNode[].class, ((Integer) 247).shortValue());
        FormatterServices.getDefault().registerKnownTypes(com.alachisoft.tayzgrid.config.newdom.Provider[].class, ((Integer) 248).shortValue());

        //</editor-fold>
    }
    
    
    /**
     * Converts the Address into a System.Net.IPEndPoint.
     *
     * @param address Address
     * @return System.Net.IPEndPoint
     */
    public static InetSocketAddress AddressToEndPoint(Address address) {
        Address ipAddr = (Address) ((address instanceof Address) ? address : null);
        if (ipAddr == null) {
            return null;
        }
        return new InetSocketAddress(ipAddr.getIpAddress(), ipAddr.getPort());
    }

    /**
     * Returns an array containing list of keys contained in the cache. Null if
     * there are no keys or if timeout occurs.
     *
     * @param cache
     * @param timeout
     * @return
     */
    public static Object[] GetKeyset(CacheBase cache, int timeout) throws OperationFailedException, LockingException, GeneralFailureException, CacheException {
        long index = 0;
        Object[] objects = null;
        cache.getSync().AcquireWriterLock();
        try {
            if (!cache.getSync().IsWriterLockHeld() || cache.getCount() < 1) {
                return objects;
            }

            objects = new Object[(int) (long) (new Long(cache.getCount()))];
            for (java.util.Iterator i = cache.GetEnumerator(); i.hasNext();) {
                objects[(int) (long) (new Long(index++))] = ((Map.Entry) i.next()).getKey();
            }
        } finally {
            cache.getSync().ReleaseWriterLock();
        }
        return objects;
    }

    /**
     * Get the contents of list as array
     *
     * @param list
     * @return
     */
    public static Object[] GetArrayFromCollection(java.util.Collection col) {
        if (col == null) {
            return null;
        }
        Object[] arr = new Object[col.size()];

        System.arraycopy(col.toArray(), 0, arr, 0, col.size());
        return arr;
    }

    /**
     * Get the keys that are not in the list
     *
     * @param keys
     * @param list
     * @return
     */
    public static Object[] GetNotAvailableKeys(Object[] keys, java.util.ArrayList list) {
        java.util.HashMap table = new java.util.HashMap();
        for (Object key : list) {
            table.put(key, "");
        }

        return GetNotAvailableKeys(keys, table);
    }

    /**
     * Converts bytes into mega bytes (MB).
     *
     * @param bytes
     * @return MB
     */
    public static double ConvertToMegaBytes(long bytes) {
        return (double) bytes / (1024 * 1024);
    }

    /**
     * Get the keys that are not in the HashMap
     *
     * @param keys
     * @param list
     * @return
     */
    public static Object[] GetNotAvailableKeys(Object[] keys, java.util.HashMap table) {
        Object[] unAvailable = new Object[keys.length - table.size()];

        int i = 0;
        for (Object key : keys) {
            if (table.containsKey(key) == false) {
                unAvailable[i] = key;
                i++;
            }
        }

        return unAvailable;
    }

    /**
     * Fill unavailable keys, available keys and their relative data
     *
     * @param keys
     * @param entries
     * @param unAvailable
     * @param available
     * @param data
     * @param list
     */
    public static void FillArrays(Object[] keys, CacheEntry[] entries, Object[] unAvailable, Object[] available, CacheEntry[] data, java.util.ArrayList list) {
        java.util.HashMap table = new java.util.HashMap();
        for (Object key : list) {
            table.put(key, "");
        }
        FillArrays(keys, entries, unAvailable, available, data, table);
    }

    /**
     * Fill unavailable keys, available keys and their relative data
     *
     * @param keys
     * @param entries
     * @param unAvailable
     * @param available
     * @param data
     * @param table
     */
    public static void FillArrays(Object[] keys, CacheEntry[] entries, Object[] unAvailable, Object[] available, CacheEntry[] data, java.util.HashMap table) {
        int a = 0, u = 0, i = 0;
        for (Object key : keys) {
            if (table.containsKey(key) == false) {
                available[a] = key;
                data[a] = entries[i];
                a++;
            } else {
                unAvailable[u] = key;
                u++;
            }
            i++;
        }
    }

    /**
     * Fill available keys and their relative data
     *
     * @param keys
     * @param entries
     * @param available
     * @param data
     * @param list
     */
    public static void FillArrays(Object[] keys, CacheEntry[] entries, Object[] available, CacheEntry[] data, java.util.ArrayList list) {
        java.util.HashMap table = new java.util.HashMap();
        for (Object key : list) {
            table.put(key, "");
        }
        FillArrays(keys, entries, available, data, table);
    }

    /**
     * Fill available keys and their relative data
     *
     * @param keys
     * @param entries
     * @param available
     * @param data
     * @param table
     */
    public static void FillArrays(Object[] keys, CacheEntry[] entries, Object[] available, CacheEntry[] data, java.util.HashMap table) {
        int a = 0, i = 0;
        for (Object key : keys) {
            if (table.containsKey(key) == false) {
                available[a] = key;
                data[a] = entries[i];
                a++;
            }
            i++;
        }
    }

    public static java.util.Map DeepClone(java.util.Map dic) {
        java.util.HashMap table = new java.util.HashMap();
        ArrayList alist;
        for (Iterator it = dic.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            ICloneable list = (ICloneable) ((entry.getValue() instanceof ICloneable) ? entry.getValue() : null);
            if (list != null) {
                table.put(entry.getKey(), list.clone());
            } else if (entry.getValue() instanceof java.util.ArrayList) {
                alist = new ArrayList((ArrayList) entry.getValue());
                table.put(entry.getKey(), alist);
            } else {
                table.put(entry.getKey(), entry.getValue());
            }
        }
        return table;
    }
}
