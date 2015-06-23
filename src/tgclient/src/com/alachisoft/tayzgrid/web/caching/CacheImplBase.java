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

package com.alachisoft.tayzgrid.web.caching;

import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.CacheConfigParams;
import com.alachisoft.tayzgrid.common.DeleteParams;
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.common.InsertResult;

import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.event.CacheListener;
import com.alachisoft.tayzgrid.event.CacheNotificationType;
import com.alachisoft.tayzgrid.event.CacheStatusEventListener;
import com.alachisoft.tayzgrid.event.CacheStatusNotificationType;
import com.alachisoft.tayzgrid.event.CustomListener;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.caching.CacheItemAttributes;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.events.EventType;
import com.alachisoft.tayzgrid.runtime.exceptions.AggregateException;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskManagement;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessorResult;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import com.alachisoft.tayzgrid.web.mapreduce.MROutputOption;
import com.alachisoft.tayzgrid.web.mapreduce.TaskEnumerator;
import com.alachisoft.tayzgrid.web.mapreduce.TaskEnumeratorCache;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import tangible.RefObject;
public class CacheImplBase implements TaskEnumeratorCache, TaskManagement
{

    private String _clientID;
    private boolean _encryptionEnabled;

    public CacheImplBase()
    {
        try
        {
            _clientID = UUID.randomUUID().toString() + ":" + InetAddress.getLocalHost().getHostName().toString() + ":" + ManagementFactory.getRuntimeMXBean().getName().toString();
        }
        catch (Exception e)
        {
        }
    }

    public boolean getEncryptionEnabled()
    {
        return _encryptionEnabled;
    }
    public void setEncryptionEnabled(boolean value)
    {
        _encryptionEnabled = value;
    }
 
    protected void registerCustomEventListener(CustomListener listener) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
    }

    protected void registerCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> registerAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException
    {
    }

    protected void registerCacheStatusEventlistener(CacheStatusEventListener listener, EnumSet<CacheStatusNotificationType> registerAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException
    {
    }

    protected void unregisterCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException
    {
    }

    protected void unregisterCacheStatusEventlistener(CacheStatusEventListener listener, EnumSet<CacheStatusNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException
    {
    }

    protected void unregisterCustomEventListener(CustomListener listener) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
    }

    protected boolean getSerializationEnabled()
    {
        return true;
    }

    protected TypeInfoMap getTypeMap()
    {
        return null;
    }

    protected void setTypeMap(TypeInfoMap value)
    {
    }

    /**
     * Occurs in response to a <see cref="Cache.RaiseCustomEvent"/> method call.
     *
     *
     * You can use this event to handle custom application defined event notifications. <p>Doing a lot of processing inside the handler might have an impact on the performance of
     * the cache and cluster. It is therefore advisable to do minimal processing inside the handler. </p> For more information on how to use this callback see the documentation for
     * <see cref="CustomEventCallback"/>.
     *
     */
    public long getCount() throws Exception
    {
        return 0;
    }

    public final String getClientID()
    {
        return _clientID;
    }

    public void unregisterHashmapChangedNotification() throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
    }



    public String getName()
    {
        return null;
    }



    public void dispose(boolean disposing) throws GeneralFailureException, OperationFailedException, ConfigurationException
    {
    }

    //property instead of cache index
    public Object getItem(Object key) throws Exception
    {
        return null;
    }

    Vector getCallbackQueue()
    {
        return null;
    }

    public void setItem(Object key, Object value)
    {
    }

    public Object add(Object key, Object value,  java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onDsItemAddedCallback, short asyncItemAddedCallback, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, java.util.HashMap queryInfo, BitSet flagMap, String providerName, String resyncProviderName, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, long size) throws Exception
    {
        return null;
    }

    /**
     * Add array of <see cref="CacheItem"/> to the cache.
     *
     * @param keys The cache keys used to reference the items.
     * @param items The items that are to be stored
     * @param group The data group of the item
     * @param subGroup Sub group of the group
     * @return keys that are added or that alredy exists in the cache and their status. If CacheItem contains invalid values the related exception is thrown. See <see
     * cref="CacheItem"/> for invalid property values and related exceptions <example>The following example demonstrates how to add items to the cache with a sliding expiration of
     * 5 minutes, a priority of high, and that notifies the application when the item is removed from the cache.
     *
     * First create a CacheItems.      <code>
     * string keys = {"ORD_23", "ORD_67"};
     * CacheItem items = new CacheItem[2]
     * items[0] = new CacheItem(new Order());
     * items[0].SlidingExpiration = new TimeSpan(0,5,0);
     * items[0].Priority = CacheItemPriority.High;
     * items[0].ItemRemoveCallback = onRemove;
     *
     * items[1] = new CacheItem(new Order());
     * items[1].SlidingExpiration = new TimeSpan(0,5,0);
     * items[1].Priority = CacheItemPriority.Low;
     * items[1].ItemRemoveCallback = onRemove;
     * </code>
     *
     * Then add CacheItem to the cache      <code>
     *
     * NCache.Cache.Add(keys, items, "Customer", "Orders");
     *
     * Cache.Add(keys, items, "Customer", "Orders");
     *
     * </code> </example>
     */
    public java.util.HashMap add(Object[] keys, CacheItem[] items, int[] removeCallbackIds, int[] updateCallbackIds, short onDataSourceItemsAdded, String providerName, long[] sizes) throws Exception
    {
        return null;
    }

    /**
     * Function that choose the appropriate function of TGCache's Cache, that need to be called according to the data provided to it.
     * @param key
     * @param value
     * @param syncDependency
     * @param absoluteExpiration
     * @param slidingExpiration
     * @param priority
     * @return 
     */
    public Object addAsync(Object key, Object value,  java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onAsyncItemAddCallback, short dsItemAddedCallback, boolean isResyncExpiredItems, String group, String subGroup, java.util.HashMap queryInfo, BitSet flagMap, String providerName, String resyncProviderName, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, long size) throws Exception
    {
        return null;
    }




    public void clear(BitSet flagMap, short onDsClearedCallback, boolean isAsync, String providerName) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
    }

    public void clearAsync(BitSet flagMap, short onDsClearedCallback, boolean isAsync, String providerName) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
    }

    public void clearAsync(BitSet flagMap, short onAsyncCacheClearCallback, short onDsClearedCallback, boolean isAsync, String providerName) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
    }

    public boolean contains(Object key) throws GeneralFailureException, OperationFailedException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConfigurationException, ConnectionException
    {
        return false;
    }
    
    public CacheConfigParams getCacheConfiguration() throws GeneralFailureException, OperationFailedException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConfigurationException, ConnectionException
    {
        return null;
    }

    public com.alachisoft.tayzgrid.caching.CompressedValueEntry get(Object key, BitSet flagMap, String group, String subGroup, CacheItemVersion version, LockHandle lockHandle, TimeSpan lockTimeout, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
        lockHandle.setLockId(null);
        lockHandle.setLockDate(new Date());
        return null;
    }

    public java.util.HashMap getByTag(Tag[] tags, com.alachisoft.tayzgrid.caching.TagComparisonType comaprisonType) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ArgumentNullException, ConnectionException, Exception
    {
        return null;
    }

    public java.util.Collection getKeysByTag(Tag[] tags, com.alachisoft.tayzgrid.caching.TagComparisonType comaprisonType) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ArgumentNullException, ConnectionException, Exception
    {
        return null;
    }

    public void removeByTag(Tag[] tags, com.alachisoft.tayzgrid.caching.TagComparisonType comaprisonType) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, ArgumentNullException, Exception
    {
    }

    public Collection getGroupKeys(String group, String subGroup) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
        return null;
    }

    public java.util.HashMap getGroupData(String group, String subGroup) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
        return null;
    }

    public void raiseCustomEvent(Object notifId, Object data) throws Exception
    {
    }

    public java.util.Map get(Object[] keys, BitSet flagMap, String providerName, short jCacheCompletionListener, boolean replaceExistingValues, boolean isAsync) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
        return null;
    }

    public Object getCacheItem(Object key, BitSet flagMap, String group, String subGroup, DSReadOption dsReadOption, CacheItemVersion version, LockHandle lockHandle, TimeSpan lockTimeout, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
        return null;
    }

    public InsertResult insert(Object key, Object value, java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onDsItemAddedCallback, short asyncItemAddedCallback, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, java.util.HashMap queryInfo, BitSet flagMap, String lockId, CacheItemVersion version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName, String resyncProviderName, EventDataFilter itemAddedDataFilter, EventDataFilter itemRemovedDataFilter, long size, InsertParams options) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions.SecurityException, ConnectionException, Exception
    {
        return null;
    }

    public java.util.HashMap insert(Object[] keys, CacheItem[] items, int[] removeCallbackIds, int[] updateCallbackIds, short onDsItemsUpdatedCallback, String providerName, long[] sizes) throws Exception
    {
        return null;
    }

    public void insertAsync(Object key, Object value,  java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onAsyncItemUpdateCallback, short onDsItemUpdatedCallback, boolean isResyncExpiredItems, String group, String subGroup, java.util.HashMap queryInfo, BitSet flagMap, String providerName, String resyncProviderName, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemoveDataFilter, long size) throws Exception
    {
    }

    public com.alachisoft.tayzgrid.caching.CompressedValueEntry remove(Object key, BitSet flagMap, short asyncItemRemovedCallback, short dsItemRemovedCallbackId, boolean isAsync, String lockId, CacheItemVersion version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String ProviderName) throws GeneralFailureException, OperationFailedException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConfigurationException, ConnectionException, Exception
    {
        return null;
    }

    public boolean delete(Object key, BitSet flagMap, short asyncItemRemovedCallback, short dsItemRemovedCallbackId, boolean isAsync, String lockId, CacheItemVersion version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String ProviderName, DeleteParams deleteParams) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
        return false;
    }

    public java.util.HashMap remove(Object[] keys, BitSet flagMap, String providerName, short onDsItemsRemovedCallback) throws GeneralFailureException, OperationFailedException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConfigurationException, ConnectionException, Exception
    {
        return null;
    }

    public void delete(Object[] keys, BitSet flagMap, String providerName, short onDsItemsRemovedCallback) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
    }

    public void removeAsync(Object key, BitSet flagMap, short onDsItemRemovedCallback)
    {
    }

    public void removeAsync(Object key, BitSet flagMap, short onAsyncItemRemoveCallback, short onDsItemRemovedCallback, String providerName)
    {
    }

    public void remove(String group, String subGroup) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException
    {
    }

    public QueryResultSet search(String query, java.util.HashMap values) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
        return null;
    }

    public QueryResultSet searchEntries(String query, java.util.HashMap values) throws OperationFailedException, GeneralFailureException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConnectionException, Exception
    {
        return null;
    }

    public int executeNonQuery(String query, java.util.HashMap values)
            throws OperationFailedException, Exception
    {
        return 0;
    }

    public Object SafeSerialize(Object serializableObject, String serializationContext, BitSet flag, CacheImplBase cacheImpl, RefObject<Long> size) throws GeneralFailureException
    {
//        return null;
        long _size;
        if (serializableObject != null)
        {
            java.lang.Class type = serializableObject.getClass();

            if (byte[].class.equals(type) && flag != null)
            {
                flag.SetBit((byte) BitSetConstants.BinaryData);
                _size = serializableObject instanceof byte[] ? ((byte[]) serializableObject).length : 0;
                size.argvalue = _size;
                return serializableObject;
            }

            try
            {
                serializableObject = CompactBinaryFormatter.toByteBuffer(serializableObject, serializationContext);
                flag.SetBit((byte)BitSetConstants.Flattened);
                _size = serializableObject instanceof byte[] ? ((byte[]) serializableObject).length : 0;
                size.argvalue = _size;
            } catch (IOException ex)
            {
                Logger.getLogger(RemoteCache.class.getName()).log(Level.SEVERE, null, ex);
                throw new GeneralFailureException(ex.getMessage());
            }
        }
        return serializableObject;
    }

//    public Object SafeDeserialize(Object serializedObject, String serializationContext, BitSet flag, CacheImplBase cacheImpl)
//    {
//        return null;
//    }
        
    public Object SafeDeserialize(Object serializedObject, String serializationContext, BitSet flag, CacheImplBase cacheImpl) {
        Object deserialized = serializedObject;
        try {
            if (serializedObject != null) {
                if (flag != null && flag.IsBitSet((byte) BitSetConstants.BinaryData)) {
                    return serializedObject;
                }
                deserialized = CompactBinaryFormatter.fromByteBuffer((byte[]) serializedObject, serializationContext);
            }

        } catch (IOException ex) {
            Logger.getLogger(RemoteCache.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(RemoteCache.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RuntimeException exception) {

            deserialized = serializedObject;
        }
        return deserialized;

    }
    public Enumeration getEnumerator()
    {
        return null;
    }

    public EnumerationDataChunk getNextChunk(EnumerationPointer pointer) throws OperationFailedException
    {
        return null;
    }

    public java.util.HashMap GetEncryptionInfo()
    {
        return null;
    }

    public java.util.ArrayList<EnumerationDataChunk> getNextChunk(java.util.ArrayList<EnumerationPointer> pointers) throws  GeneralFailureException, OperationFailedException, AggregateException, ConnectionException,java.net.UnknownHostException
    {
        return null;
    }

    public void unlock(Object key) throws Exception
    {
    }

    public void unlock(Object key, String lockId) throws Exception
    {
    }

    public boolean lock(Object key, TimeSpan lockTimeout, LockHandle lockHandle) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException, Exception
    {
        lockHandle = null;
        return false;
    }

    public boolean isLocked(Object key, LockHandle lockHandle) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException, Exception
    {
        return false;
    }

    public void registerKeyNotificationCallback(Object key, short updateCallbackid, short removeCallbackid) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException, Exception
    {
    }

    public void unRegisterKeyNotificationCallback(Object key, short updateCallbackid, short removeCallbackid) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException, Exception
    {
    }

    public void registerKeyNotificationCallback(Object[] keys, short updateCallbackid, short removeCallbackid) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException, Exception
    {
    }

    public void unRegisterKeyNotificationCallback(Object key, short update, short remove, EventType eventType) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException{}
    
    public void unRegisterKeyNotificationCallback(Object[] key, short update, short remove, EventType eventType) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {}
    
    public void unRegisterKeyNotificationCallback(Object[] key, short update, short remove) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {}
    
    public void registerGeneralNotification(EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter, short sequenceNumber) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException
    { }


    public boolean setAttributes(Object key, CacheItemAttributes attribute) throws Exception
    {
        return false;
    }
    
    public void registerKeyNotificationCallback(Object key, short update, short remove, EventDataFilter datafilter, boolean notifyOnItemExpiration) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException ,Exception
    { } 
    
    public void registerKeyNotificationCallback(Object[] key, short update, short remove, EventDataFilter datafilter, boolean notifyOnItemExpiration) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException{}
        
    public void registerKeyNotificationCallback(Object key, short updateCallbackid, short removeCallbackid, boolean notifyOnItemExpiration) throws ConnectionException, OperationFailedException, GeneralFailureException, AggregateException {}    
    
    public void unRegisterGeneralNotification(EnumSet<EventType> eventEnumSet, short sequenceNumber) throws GeneralFailureException,  OperationFailedException, ConnectionException, AggregateException
    {}
    
    public java.util.Map<Object,EntryProcessorResult> invokeEntryProcessor(Object[] keys,
           com.alachisoft.tayzgrid.runtime.processor.EntryProcessor entryProcessor,String defaultReadThru,String defaultWriteThru,
           Object... arguments) throws GeneralFailureException,OperationFailedException,ConnectionException,AggregateException
    {
        return null;
    }
  
    // <editor-fold desc=" ------------- MapReduce ------------ ">
    
    public void executeMapReduceTask(MapReduceTask task, String taskId, MROutputOption outputOption, short callbackId) throws GeneralFailureException,  OperationFailedException, ConnectionException, AggregateException
    {}
    
    public void registerMapReduceTaskCallback(short callbackId, String taskId) throws GeneralFailureException,  OperationFailedException, ConnectionException, AggregateException
    {}
    
    public void cancelAllTasks() throws GeneralFailureException, OperationFailedException,  ConnectionException, AggregateException
    {}
    
    public List getRunningTasks() throws GeneralFailureException, OperationFailedException,  ConnectionException, AggregateException
    { return null; }
    
    // </editor-fold>

    @Override
    public TaskEnumeratorResult getNextRecord(String serverAddress,TaskEnumeratorPointer pointer) throws OperationFailedException {

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void dispose(String serverAddress) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void cancelTask(String taskId) throws OperationFailedException
    {}

    @Override
    public TaskStatus getTaskProgress(String taskId) throws GeneralFailureException,  OperationFailedException, ConnectionException, AggregateException
    { return null; }

    @Override
    public TaskEnumerator getTaskEnumerator(String taskId, short callbackId) throws OperationFailedException {
        return null;
    }

}
