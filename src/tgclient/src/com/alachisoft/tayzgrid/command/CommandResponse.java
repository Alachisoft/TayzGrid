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
package com.alachisoft.tayzgrid.command;

import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.queries.QueryChangeType;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.CacheConfigParams;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;


import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse;
import com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol;
import com.alachisoft.tayzgrid.common.protobuf.GetTaskEnumeratorResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.InvokeEPKeyValuePackageResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyExceptionPackageResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyValuePackageResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.KeyValuePairProtocol.KeyValuePair;
import com.alachisoft.tayzgrid.common.protobuf.NamedTagInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.QueryResultSetProtocol.QueryResultSet;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol.Response;
import com.alachisoft.tayzgrid.common.protobuf.SearchEntriesResponseProtocol.SearchEntriesResponse;
import com.alachisoft.tayzgrid.common.protobuf.SearchResponseProtocol.SearchResponse;
import com.alachisoft.tayzgrid.common.protobuf.TagInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TaskCallbackResponseProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.persistence.EventType;
import com.alachisoft.tayzgrid.processor.TayzGridEntryProcessorResult;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.caching.NamedTagsDictionary;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import com.alachisoft.tayzgrid.util.DictionaryEntry;
import com.alachisoft.tayzgrid.util.HelperUtil;
import com.alachisoft.tayzgrid.util.NewHashMap;
import com.alachisoft.tayzgrid.util.UserBinaryObject;
import com.alachisoft.tayzgrid.web.caching.Cache;
import com.alachisoft.tayzgrid.web.caching.CacheItem;
import com.alachisoft.tayzgrid.web.caching.CacheItemRemovedReason;
import com.alachisoft.tayzgrid.web.caching.DataSourceOpResult;
import com.alachisoft.tayzgrid.web.caching.LockHandle;
import com.alachisoft.tayzgrid.web.caching.OpCode;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores the result send by the NCache Server
 *
 * @version 1.0
 * 
 */
public final class CommandResponse {

    /**
     *
     */
    private Object key;
    /**
     * Represents the type of the response.
     */
    private String type;
    /**
     * Requested id of the command.
     */
    private long req_id = 0;
    /**
     * Which callback is it.
     */
    private int registeredCallback = 0;
    /**
     * Remove reason if item is removed.
     */
    private int reason;
    private Object result = null;
    private ExceptionProtocol.Exception.Type expType;
    private String expMessage = null;
    private byte[] rawResult = null;
    private String _cacheType = "";
    /**
     *
     */
    private byte[] value;
    private CacheItem item;
    private TypeInfoMap _typeMap;
    private com.alachisoft.tayzgrid.caching.CompressedValueEntry _flagValueEntry = new com.alachisoft.tayzgrid.caching.CompressedValueEntry();
    // <editor-fold defaultstate="collapsed" desc="Cluster Event Data">
    private String ip = "";
    private boolean isRunning = false;
    private int port = 0;
    private String clusterIp = "";
    private int clusterPort = 0;
    private boolean reconnectClients = false;
    private LockHandle _lockHandle = new LockHandle();
    private String cacheId = "";
    
    // </editor-fold>
    private boolean _brokerReset = false;
    private long _version;
    /**
     * *
     * Whether lock is acquired or not
     */
    private boolean _lockAcquired;
    //private int _bucketSize = 0;
    private HashMap _hashmap = null;
    private ResponseProtocol.Response.Type _type;
    private long _count = 0;
    private boolean _exists = false;
    private short _callbackId = -1;
    private Object _asyncOpResult = null;
    private byte[] _notifId = null;
    private boolean _success = false;
    private int _bytesRead;
    private long _streamLength;
    private List<ByteString> _dataList;
    //+ :20110330
    private OpCode _operationCode;
 
    //- :20110330
    //+ 20110208 Aggregate function support
    private com.alachisoft.tayzgrid.caching.queries.QueryResultSet resultSet = null;
    private String _queryId;
    private QueryChangeType _changeType;
    private ArrayList<EnumerationDataChunk> _enumerationDataChunk = new ArrayList<EnumerationDataChunk>();
    private ArrayList<TaskEnumeratorResult> _taskEnumerator = new ArrayList<TaskEnumeratorResult>();
    private TaskEnumeratorResult _enumeratorResultSet=null;
    private java.util.HashMap<String, Integer> runningServers = new HashMap<String, Integer>();
    private Response _response;
    private com.alachisoft.tayzgrid.caching.EventId _eventId;
    private boolean _isPersistEnabled = false;
    private int _persistenceInterval;
    private int _removedKeyCount;
    private int _expirationOrdinal;
    private int _expirationUnitOrdinal;
    private long _expirationDuration;
	private List<BulkEventItemResponse> _eventList;
    private EventDataFilter _dataFilter = EventDataFilter.None;

    // Map Reduce things
    private int _taskStatus;
    private String _taskId;
    private List runningTasks;
    private TaskStatus taskStatus;
    
    private CacheConfigParams _cacheConfigParams = new CacheConfigParams(); 

    public int getTaskStatus() {
        return _taskStatus;
    }

    public void setTaskStatus(int _taskStatus) {
        this._taskStatus = _taskStatus;
    }

    public String getTaskId() {
        return _taskId;
    }

    public void setTaskId(String _taskId) {
        this._taskId = _taskId;
    }
    
    public EventDataFilter getDataFilter() {
        return this._dataFilter;
    }

    public final List<BulkEventItemResponse> getEventList() {
        if (_eventList != null) {
            return _eventList;
        } else {
            return new ArrayList<BulkEventItemResponse>();
        }
    }
    

    public final boolean getIsPersistenceEnabled() {
        return _isPersistEnabled;
    }

    public final int getPersistInterval() {
        return _persistenceInterval;
    }

    public final com.alachisoft.tayzgrid.caching.EventId getEventId() {
        return _eventId;
    }

    public final Response getProtobufResponse() {
        return _response;
    }

    public final int getRemovedKeyCount() {
        return _removedKeyCount;
    }

    public final void setRemovedKeyCount(int value) {
        _removedKeyCount = _removedKeyCount + value;
    }

    public ArrayList<EnumerationDataChunk> getNextChunk() {
        return _enumerationDataChunk;
    }

    public void setNextChunk(ArrayList<EnumerationDataChunk> chunks) {
        this._enumerationDataChunk = chunks;
    }
    /// <summary>Tells with which ip connection is broken</summary>
    private Address _resetConnectionIP;//private String _resetConnectionIP = "";
    private String _intendedRecipient = "";

    public Address getResetConnectionIP() {
        return _resetConnectionIP;
    }

    public void setResetConnectionIP(Address ip) {
        _resetConnectionIP = ip;
    }

    public String getIntendedRecipient() {
        return _intendedRecipient;
    }

    public boolean reconnectClients() {
        return reconnectClients;
    }

    public String getClusterIp() {
        return clusterIp;
    }

    public int getClusterPort() {
        return clusterPort;
    }
    /// <summary>
    /// by default one response is sent back for each request. If required, a single response
    /// can be segmented into smaller chunks. In that case, these properties must be properly set.
    /// </summary>
    private int _sequenceId = 1;

    public int getSequenceId() {
        return _sequenceId;
    }
    private int _numberOfChunks = 1;

    public int getNumberOfChunks() {
        return _numberOfChunks;
    }

    public java.util.HashMap<String, Integer> getRunningServers() {
        if (runningServers != null) {
            return runningServers;
        } else {
            return new java.util.HashMap<String, Integer>();
        }
    }

    /**
     * Creates a new instance of CacheResultItem
     */
    public CommandResponse(boolean brokerReset)
    {
        type = "";
        value = null;
        _brokerReset = brokerReset;
    }

    /// <summary>
    /// Creates a new instance of CacheResultItem
    /// <param name="brokerReset">True if broker is reset due to lost connection, false otherwise</param>
    /// </summary>
    public CommandResponse(boolean brokerReset, Address resetConnectionIP)
    {
        type = "";
        value = null;
        _brokerReset = brokerReset;
        _resetConnectionIP = resetConnectionIP;
    }

    /**
     * Get response type
     *
     * @return
     */
    public ResponseProtocol.Response.Type getType() {
        return this._type;
    }

    public long getVersion() {
        return this._version;
    }

    public HashMap getHashMap() {
        return _hashmap;
    }

    public OpCode getDSOperationCode() {
        return _operationCode;
    }

    public int BytesRead() {
        return _bytesRead;
    }

    public long StreamLegnth() {
        return _streamLength;
    }

    public List<ByteString> DataList() {
        return _dataList;
    }

    /**
     * Get cache count
     *
     * @return
     */
    public long getCount() {
        return this._count;
    }

    public QueryChangeType getChangeType() {
        return _changeType;
    }

    /**
     * Determine whether an item exists in cache or not
     *
     * @return
     */
    public boolean exists() {
        return this._exists;
    }
    
    public CacheConfigParams cacheConfigParams() {
        return this._cacheConfigParams;
    }

    /**
     * Get callback id
     *
     * @return
     */
    public short getCallbackId() {
        return this._callbackId;
    }

    /**
     * Get notif id for custom events
     *
     * @return
     */
    public byte[] getNotifId() {
        return this._notifId;
    }

    /**
     * Determine whether operation was successfully performed or not
     *
     * @return
     */
    public boolean success() {
        return this._success;
    }

    /**
     *
     * @return
     */
    public Object getAsyncOpResult() {
        return this._asyncOpResult;
    }

    public String getQueryId() {
        return _queryId;
    }

    /*
     * @return the populated QueryResult received from the NCache Server based on query executed.
     */
    public com.alachisoft.tayzgrid.caching.queries.QueryResultSet getQueryResultSet() {
        return this.resultSet;
    }

    /**
     * Set value from protobuf response
     *
     * @param list
     * @throws OperationFailedException
     */
    private void setValue(List<ByteString> list) throws OperationFailedException {
        if (list == null) {
            this.value = null;
            return;
        }
        if (list.size() == 0) {
            this.value = null;
            return;
        }

        UserBinaryObject userObj
                = UserBinaryObject.createUserBinaryObject(list);
        try {
            this.value = userObj.getFullObject();
        } catch (IOException ex) {
            throw new OperationFailedException(ex.getMessage());
        }
    }

    /**
     *
     * @param val
     */

    public void parseResponse(String serializationContext) throws OperationFailedException, IOException, ClassNotFoundException
    {
        String val = null;
        byte[] buffer = this.getRawResult();
        ResponseProtocol.Response response = null;
        try {
            ///Parse response
            response = ResponseProtocol.Response.parseFrom(buffer);
        } catch (InvalidProtocolBufferException ex) {
            Logger logger = Logger.getLogger("com.alachisoft");
            if (logger.isLoggable(Level.SEVERE)) {
                logger.logp(Level.SEVERE, this.getClass().getName(), "parseResponse", "Unable to parse the response received from server.", ex);
            }

            throw new OperationFailedException(ex.getMessage());
        }

        com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer enumPointer = null;
        com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk chunk = null;

        this._sequenceId = response.getSequenceId();
        this._numberOfChunks = response.getNumberOfChuncks();

        this._type = response.getResponseType();

        switch (response.getResponseType()) {
            case ADD:
            case REMOVE_GROUP:
            case REGISTER_NOTIF:
            case CLEAR:
            case RAISE_CUSTOM_EVENT:
            case REGISTER_KEY_NOTIF:
            case REGISTER_BULK_KEY_NOTIF:
            case UNREGISTER_BULK_KEY_NOTIF:
            case UNREGISTER_KEY_NOTIF:
            case UNLOCK:
            case REMOVE_TAG:
            case DELETE_BULK:
            case DISPOSE:
            
            case MAP_REDUCE_TASK:
            case MAP_REDUCE_TASK_CANCEL:
                this.req_id = response.getRequestId();
                break;
            case DELETE:
                this.req_id = response.getRequestId();
                this._success = response.getDeleteResponse().getSuccess();
                break;
            case TASK_PROGRESS:
                try {
                    this.taskStatus = (TaskStatus) CompactBinaryFormatter.fromByteBuffer(response.getTaskProgressResponse().getProgresses().toByteArray(), cacheId);
                } catch (Exception ex) 
                { }
                this.req_id = response.getRequestId();
                break;


            case COUNT:
                this.req_id = response.getRequestId();
                this._count = response.getCount().getCount();
                break;

            case CONTAINS:
                this.req_id = response.getRequestId();
                this._exists = response.getContain().getExists();
                break;
                
            case GET_CACHE_CONFIG:
                this.req_id = response.getRequestId();
                this._cacheConfigParams.setIsReadThru(response.getGetCacheConfigurationResponse().getIsReadThru());
                this._cacheConfigParams.setIsWriteThru(response.getGetCacheConfigurationResponse().getIsWriteThru());
                this._cacheConfigParams.setIsStatisticsEnabled(response.getGetCacheConfigurationResponse().getIsStasticsEnabled());
                break;

            case ITEM_UPDATED_CALLBACK:
                this.req_id = response.getRequestId();
                this._callbackId = (short) response.getItemUpdatedCallback().getCallbackId();
                this.key = CacheKeyUtil.Deserialize(response.getItemUpdatedCallback().getKey(), serializationContext);
                this._dataFilter = EventDataFilter.forValue(response.getItemUpdatedCallback().getDataFilter());
                _eventId = new com.alachisoft.tayzgrid.caching.EventId();
                _eventId.setEventUniqueID(response.getItemUpdatedCallback().getEventId().getEventUniqueId());
                _eventId.setEventCounter(response.getItemUpdatedCallback().getEventId().getEventCounter());
                _eventId.setOperationCounter(response.getItemUpdatedCallback().getEventId().getOperationCounter());
                _eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_CALLBACK);

                break;
            case CACHE_CLEARED_EVENT:
                this.req_id = response.getRequestId();
                this._callbackId = (short) response.getItemUpdatedCallback().getCallbackId();
                _eventId = new com.alachisoft.tayzgrid.caching.EventId();
                _eventId.setEventUniqueID(response.getCacheCleared().getEventId().getEventUniqueId());
                _eventId.setEventUniqueID(Integer.toString(response.getCacheCleared().getEventId().getEventCounter()));
                _eventId.setOperationCounter(response.getCacheCleared().getEventId().getOperationCounter());
                _eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.CACHE_CLEARED_EVENT);

                break;
            case ITEM_REMOVED_CALLBACK:
                this.req_id = response.getRequestId();
                this._callbackId = (short) response.getItemRemovedCallback().getCallbackId();
                this.key = CacheKeyUtil.Deserialize(response.getItemRemovedCallback().getKey(), serializationContext);
                this.reason = response.getItemRemovedCallback().getItemRemoveReason();
                this._flagValueEntry.Flag = new com.alachisoft.tayzgrid.common.BitSet((byte) response.getItemRemovedCallback().getFlag());
                this._dataFilter = EventDataFilter.forValue(response.getItemRemovedCallback().getDataFilter());
                if (response.getItemRemovedCallback().getValueCount() > 0) {
                    this.setValue(response.getItemRemovedCallback().getValueList());
                }
                _eventId = new com.alachisoft.tayzgrid.caching.EventId();
                _eventId.setEventUniqueID(response.getItemRemovedCallback().getEventId().getEventUniqueId());
                _eventId.setEventUniqueID(Integer.toString(response.getItemRemovedCallback().getEventId().getEventCounter()));
                _eventId.setOperationCounter(response.getItemRemovedCallback().getEventId().getOperationCounter());
                _eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_CALLBACK);
                break;

            case ASYNC_OP_COMPLETED_CALLBACK:
                this.req_id = response.getRequestId();
                this.key = CacheKeyUtil.Deserialize(response.getAsyncOpCompletedCallback().getKey(), serializationContext);
                if (response.getAsyncOpCompletedCallback().getSuccess())
                {
                    this._asyncOpResult = 1;
                } else {
                    this._asyncOpResult = new Exception(response.getAsyncOpCompletedCallback().getExc().getException());
                }
                break;

            case DS_UPDATE_CALLBACK: {
                this.req_id = response.getRequestId();
                this._callbackId = (short) response.getDsUpdateCallbackRespose().getCallbackId();
                this._operationCode = _operationCode.getOpCode(response.getDsUpdateCallbackRespose().getOpCode());
                int length = response.getDsUpdateCallbackRespose().getResultList().size();
                this._hashmap = new HashMap();
                for (int index = 0; index < length; index++) {
                    com.alachisoft.tayzgrid.common.protobuf.DSUpdatedCallbackResultProtocol.DSUpdatedCallbackResult result = response.getDsUpdateCallbackRespose().getResult(index);

                    if (result.getSuccess()) {
                        _hashmap.put(result.getKey(), DataSourceOpResult.Success);
                    } else if (result.hasException()) {
                        Exception ex = new OperationFailedException(result.getException().getException());
                        _hashmap.put(result.getKey(), ex);
                    } else {
                        _hashmap.put(result.getKey(), DataSourceOpResult.Failure);
                    }
                }

                this.result = _hashmap;

            }
            break;

            case ITEM_ADDED_EVENT:
                this.req_id = response.getRequestId();
                this.key = CacheKeyUtil.Deserialize(response.getItemAdded().getKey(), serializationContext);
                _eventId = new com.alachisoft.tayzgrid.caching.EventId();
                _eventId.setEventUniqueID(response.getItemAdded().getEventId().getEventUniqueId());
                _eventId.setEventCounter(response.getItemAdded().getEventId().getEventCounter());
                _eventId.setOperationCounter(response.getItemAdded().getEventId().getOperationCounter());
                _eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_ADDED_EVENT);
                break;

            case ITEM_UPDATED_EVENT:
                this.req_id = response.getRequestId();
                this.key = CacheKeyUtil.Deserialize(response.getItemUpdated().getKey(), serializationContext);
                _eventId = new com.alachisoft.tayzgrid.caching.EventId();
                _eventId.setEventUniqueID(response.getItemUpdated().getEventId().getEventUniqueId());
                _eventId.setEventCounter(response.getItemUpdated().getEventId().getEventCounter());
                _eventId.setOperationCounter(response.getItemUpdated().getEventId().getOperationCounter());
                _eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_EVENT);
                break;

            case CACHE_STOPPED_EVENT:
                this.req_id = response.getRequestId();
                this.cacheId = response.getCacheStopped().getCacheId();
                break;

            case ITEM_REMOVED_EVENT:
                this.req_id = response.getRequestId();
                this.key = CacheKeyUtil.Deserialize(response.getItemRemoved().getKey(), serializationContext);
                this._flagValueEntry.Flag = new com.alachisoft.tayzgrid.common.BitSet((byte) response.getItemRemoved().getFlag());
                this.reason = response.getItemRemoved().getItemRemoveReason();
                if (response.getItemRemoved().getValueCount() > 0) {
                    this.setValue(response.getItemRemoved().getValueList());
                }
                _eventId = new com.alachisoft.tayzgrid.caching.EventId();
                _eventId.setEventUniqueID(response.getItemRemoved().getEventId().getEventUniqueId());
                _eventId.setEventCounter(response.getItemRemoved().getEventId().getEventCounter());
                _eventId.setOperationCounter(response.getItemRemoved().getEventId().getOperationCounter());
                _eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_EVENT);

                break;

            case MAP_REDUCE_TASK_CALLBACK:
                this.req_id = response.getRequestId();
                TaskCallbackResponseProtocol.TaskCallbackResponse tcr = response.getTaskCallbackResponse();
                this._callbackId = (short) tcr.getCallbackId();
                this._taskId = tcr.getTaskId();
                this._taskStatus = (int) tcr.getTaskStatus();
                _eventId = new com.alachisoft.tayzgrid.caching.EventId();
                _eventId.setEventUniqueID(response.getTaskCallbackResponse().getEventId().getEventUniqueId());
                _eventId.setEventCounter(response.getTaskCallbackResponse().getEventId().getEventCounter());
                _eventId.setOperationCounter(response.getTaskCallbackResponse().getEventId().getOperationCounter());
                _eventId.setEventType(EventType.TASK_CALLBACK);
                break;
            case GET_RUNNING_TASKS:
                this.req_id = response.getRequestId();
                this.runningTasks = (List) response.getGetRunningTasksResponse().getRunningTasksList();
                break;
            case CUSTOM_EVENT:
                this.req_id = response.getRequestId();
                if (response.getCustomEvent().hasKey()) {
                    this._notifId = response.getCustomEvent().getKey().toByteArray();
                }
                if (response.getCustomEvent().hasValue()) {
                    this.value = response.getCustomEvent().getValue().toByteArray();
                }
                break;

            case GET_OPTIMAL_SERVER:
                this.req_id = response.getRequestId();
                this.ip = response.getGetOptimalServer().getServer();
                this.port = response.getGetOptimalServer().getPort();                
                break;
             
            case CACHE_BINDING:
                this.req_id = response.getRequestId();
                this.ip = response.getGetCacheBindingResponse().getServer();
                this.port = response.getGetCacheBindingResponse().getPort();
                this.isRunning = response.getGetCacheBindingResponse().getIsRunning();
                break;

            case NODE_JOINED_EVENT:
                this.req_id = response.getRequestId();
                this.ip = response.getNodeJoined().getServerIp();
                this.port = Integer.parseInt(response.getNodeJoined().getServerPort());
                this.clusterIp = response.getNodeJoined().getClusterIp();
                this.clusterPort = Integer.parseInt(response.getNodeJoined().getClusterPort());
                this.reconnectClients = response.getNodeJoined().getReconnect();
                break;

            case NODE_LEFT_EVENT:
                this.req_id = response.getRequestId();
                this.ip = response.getNodeLeft().getServerIp();
                this.port = Integer.parseInt(response.getNodeLeft().getServerPort());
                this.clusterIp = response.getNodeLeft().getClusterIp();
                this.clusterPort = Integer.parseInt(response.getNodeLeft().getClusterPort());
                break;

            case INSERT:
                this.req_id = response.getRequestId();
                this._version = response.getInsert().getVersion();
                this._success = response.getInsert().getSuccess();
                if (response.getInsert().getExistingValueCount()> 0)
                {
                    this.setValue(response.getInsert().getExistingValueList());
                }
                this._flagValueEntry.Flag = new com.alachisoft.tayzgrid.common.BitSet((byte) response.getGet().getFlag());
                this._flagValueEntry.Flag.SetBit((byte) response.getInsert().getFlag());
                break;

            case INIT:
                this.req_id = response.getRequestId();
                this._cacheType = response.getInitCache().getCacheType();
             
                _isPersistEnabled = response.getInitCache().getIsPersistenceEnabled();
                _persistenceInterval = response.getInitCache().getPersistenceInterval();
                _expirationOrdinal = response.getInitCache().getExpirationType();
                _expirationUnitOrdinal = response.getInitCache().getExpirationUnit();
                _expirationDuration = response.getInitCache().getDuration();
                _response = response;
                break;

            case GET:
                this.req_id = response.getRequestId();
                this._flagValueEntry.Flag = new com.alachisoft.tayzgrid.common.BitSet((byte) response.getGet().getFlag());
                this._flagValueEntry.Flag.SetBit((byte) response.getGet().getFlag());
                String lockId = response.getGet().getLockId();
                if (lockId == null ? "" == null : lockId.equals("")) {
                    lockId = null;
                }
                this._lockHandle.setLockId(lockId);
                long ticks = response.getGet().getLockTime();
                if (ticks > 0) {

                    this._lockHandle.setLockDate(HelperFxn.getDateFromTicks(ticks));
                } else {
                    try {
                        this._lockHandle.setLockDate(new NCDateTime(1970, 1, 1, 0, 0, 0, 0).getDate());
                    } catch (Exception e) {
                    }
                }
                this._version = response.getGet().getVersion();
                if (response.getGet().getDataCount() > 0) {
                    this.setValue(response.getGet().getDataList());
                }
                break;

            case REMOVE:
                this.req_id = response.getRequestId();
                this._flagValueEntry.Flag = new com.alachisoft.tayzgrid.common.BitSet((byte) response.getRemove().getFlag());
                if (response.getRemove().getValueCount() > 0) {
                    this.setValue(response.getRemove().getValueList());
                }
                break;

            case LOCK:
                this.req_id = response.getRequestId();
                this._lockAcquired = response.getLockResponse().getLocked();
                lockId = response.getLockResponse().getLockId();
                if (lockId == null ? "" == null : lockId.equals("")) {
                    lockId = null;
                }
                this._lockHandle.setLockId(lockId);
                ticks = response.getLockResponse().getLockTime();
                if (ticks > 0) {

                    this._lockHandle.setLockDate(HelperFxn.getDateFromTicks(ticks));

                } else {
                    try {
                        this._lockHandle.setLockDate(new NCDateTime(1970, 1, 1, 0, 0, 0, 0).getDate());
                    } catch (Exception e) {
                    }
                }
                break;

            case ISLOCKED:
                this.req_id = response.getRequestId();
                this._lockAcquired = response.getIsLockedResponse().getIsLocked();
                this._lockHandle.setLockId(response.getIsLockedResponse().getLockId());
                ticks = response.getIsLockedResponse().getLockTime();
                if (ticks > 0) {

                    this._lockHandle.setLockDate(HelperFxn.getDateFromTicks(ticks));

                } else {
                    try {
                        this._lockHandle.setLockDate(new NCDateTime(1970, 1, 1, 0, 0, 0, 0).getDate());
                    } catch (Exception e) {
                    }
                }
                break;
                
            case INVOKE_ENTRYPROCESSOR:
                this.req_id = response.getRequestId();
                this._intendedRecipient = response.getIntendedRecipient();
                
                InvokeEPKeyValuePackageResponseProtocol.InvokeEPKeyValuePackageResponse invokeEPKeyValPack
                        = response.getInvokeEntryProcessorResponse().getKeyValuePackage();
                HashMap invokeEPResult = new HashMap();
                if (invokeEPKeyValPack != null)
                {
                    for (int i = 0; i < invokeEPKeyValPack.getKeysCount(); i++)
                    {
                        Object fetchedKey = CacheKeyUtil.Deserialize(invokeEPKeyValPack.getKeys(i), serializationContext);
                        //this.setValue(invokeEPKeyValPack.getValues(i));                        
                        
                        TayzGridEntryProcessorResult entry = (TayzGridEntryProcessorResult)CacheKeyUtil.Deserialize(invokeEPKeyValPack.getValues(i), serializationContext);

                        invokeEPResult.put(fetchedKey, entry);

                        ///reset value
                        this.value = null;
                    }
                }

                this.result = invokeEPResult;
                break;

            case GET_BULK:
                this.req_id = response.getRequestId();
                this._intendedRecipient = response.getIntendedRecipient();
                
                KeyValuePackageResponseProtocol.KeyValuePackageResponse keyValPack
                        = response.getBulkGet().getKeyValuePackage();
                HashMap hashmap = new HashMap();
                if (keyValPack != null)
                {
                    for (int i = 0; i < keyValPack.getKeysCount(); i++)
                    {
                        Object fetchedKey = CacheKeyUtil.Deserialize(keyValPack.getKeys(i), serializationContext);
                        this.setValue(keyValPack.getValues(i).getDataList());
                        CompressedValueEntry entry = new CompressedValueEntry(this.getValue(),
                                new com.alachisoft.tayzgrid.common.BitSet((byte) keyValPack.getFlag(i)));

                        hashmap.put(fetchedKey, entry);

                        ///reset value
                        this.value = null;
                    }
                }

                this.result = hashmap;
                break;

            case REMOVE_BULK:
                this.req_id = response.getRequestId();
                this._intendedRecipient = response.getIntendedRecipient();
                keyValPack
                        = response.getBulkRemove().getKeyValuePackage();
                hashmap = new HashMap();
                if (keyValPack != null)
                {
                    for (int i = 0; i < keyValPack.getKeysCount(); i++)
                    {
                        Object key = CacheKeyUtil.Deserialize(keyValPack.getKeys(i), serializationContext);
                        CompressedValueEntry entry = new CompressedValueEntry();

                        this.setValue(keyValPack.getValues(i).getDataList());
                        entry.setValue(this.getValue());

                        entry.Flag = new com.alachisoft.tayzgrid.common.BitSet((byte) keyValPack.getFlag(i));
                        hashmap.put(key, entry);

                        ///reset value
                        this.value = null;
                    }
                }

                this.result = hashmap;
                break;

            case GET_GROUP_DATA:
                this.req_id = response.getRequestId();
                keyValPack
                        = response.getGetGroupData().getKeyValuePackage();
                hashmap = new HashMap();
                if (keyValPack != null)
                {
                    for (int i = 0; i < keyValPack.getKeysCount(); i++)
                    {
                        Object groupKey = CacheKeyUtil.Deserialize(keyValPack.getKeys(i), serializationContext);
                        CompressedValueEntry entry = new CompressedValueEntry();

                        this.setValue(keyValPack.getValues(i).getDataList());
                        entry.setValue(this.getValue());

                        entry.Flag = new com.alachisoft.tayzgrid.common.BitSet((byte) keyValPack.getFlag(i));
                        hashmap.put(groupKey, entry);

                        ///reset value
                        this.value = null;
                    }
                }

                this.result = hashmap;
                break;

            case GET_TAG:
                this.req_id = response.getRequestId();
                keyValPack
                        = response.getGetTag().getKeyValuePackage();
                hashmap = new HashMap();
                if (keyValPack != null)
                {
                    for (int i = 0; i < keyValPack.getKeysCount(); i++)
                    {
                        Object itemKey = CacheKeyUtil.Deserialize(keyValPack.getKeys(i), serializationContext);
                        CompressedValueEntry entry = new CompressedValueEntry();

                        this.setValue(keyValPack.getValues(i).getDataList());
                        entry.setValue(this.getValue());

                        entry.Flag = new com.alachisoft.tayzgrid.common.BitSet((byte) keyValPack.getFlag(i));
                        hashmap.put(itemKey, entry);

                        ///reset value
                        this.value = null;
                    }
                }

                this.result = hashmap;
                break;

            case GET_KEYS_TAG:
                this.req_id = response.getRequestId();
                ArrayList keys = new ArrayList();
                for(ByteString key : response.getGetKeysByTagResponse().getKeysList())
                {
                    keys.add(CacheKeyUtil.Deserialize(key, serializationContext));
                    
                }
                this.result = keys;
                break;

            case SEARCH:
                this.req_id = response.getRequestId();
                SearchResponse searchResponse = response.getSearch();
                resultSet = new com.alachisoft.tayzgrid.caching.queries.QueryResultSet();
                QueryResultSet protoResultSet = searchResponse.getQueryResultSet();
                resultSet.setCQUniqueId(protoResultSet.getCQUniqueId());

                switch (protoResultSet.getQueryType()) {
                    case AGGREGATE_FUNCTIONS:
                        resultSet.setType(com.alachisoft.tayzgrid.caching.queries.QueryType.AggregateFunction);
                        resultSet.setAggregateFunctionType(com.alachisoft.tayzgrid.common.enums.AggregateFunctionType.forValue(protoResultSet.getAggregateFunctionType().getNumber()));
                        byte[] bytes = protoResultSet.getAggregateFunctionResult().getValue().toByteArray();
                        if (bytes.length > 0) {
                            resultSet.setAggregateFunctionResult(new DictionaryEntry<Object, Object>(protoResultSet.getAggregateFunctionResult().getKey(), CacheKeyUtil.Deserialize(bytes, serializationContext)));
                        } else {
                            resultSet.setAggregateFunctionResult(new DictionaryEntry<Object, Object>(protoResultSet.getAggregateFunctionResult().getKey(), null));
                        }
                        break;
                    case SEARCH_KEYS:
                        resultSet.setType(com.alachisoft.tayzgrid.caching.queries.QueryType.SearchKeys);
                        ArrayList searchKeys = new ArrayList();
                        for(ByteString key : searchResponse.getQueryResultSet().getSearchKeyResultsList())
                        {
                            searchKeys.add(CacheKeyUtil.Deserialize(key, serializationContext));
                        }
                        this.result = searchKeys;
                        this.resultSet.setSearchKeysResult((ArrayList) this.result);
                        break;
                }

                break;
            case SEARCH_ENTRIES: {
                this.req_id = response.getRequestId();
                SearchEntriesResponse searchEntriesResponse = response.getSearchEntries();
                protoResultSet = searchEntriesResponse.getQueryResultSet();
                resultSet = new com.alachisoft.tayzgrid.caching.queries.QueryResultSet();
                resultSet.setCQUniqueId(protoResultSet.getCQUniqueId());

                switch (protoResultSet.getQueryType()) {
                    case AGGREGATE_FUNCTIONS:
                        resultSet.setType(com.alachisoft.tayzgrid.caching.queries.QueryType.AggregateFunction);
                        resultSet.setAggregateFunctionType(com.alachisoft.tayzgrid.common.enums.AggregateFunctionType.forValue(protoResultSet.getAggregateFunctionType().getNumber()));
                        byte[] bytes = protoResultSet.getAggregateFunctionResult().getValue().toByteArray();
                        if (bytes.length > 0) {
                            resultSet.setAggregateFunctionResult(new DictionaryEntry<Object, Object>(protoResultSet.getAggregateFunctionResult().getKey(), CacheKeyUtil.Deserialize(bytes, serializationContext)));
                        } else {
                            resultSet.setAggregateFunctionResult(new DictionaryEntry<Object, Object>(protoResultSet.getAggregateFunctionResult().getKey(), null));
                        }
                        break;
                    case SEARCH_ENTRIES:
                        resultSet.setType(com.alachisoft.tayzgrid.caching.queries.QueryType.SearchEntries);
                        keyValPack = searchEntriesResponse.getQueryResultSet().getSearchKeyEnteriesResult();
                        HashMap HashMap = new HashMap();
                        if (keyValPack != null)
                        {
                            for (int i = 0; i < keyValPack.getKeysCount(); i++)
                            {
                                Object key = CacheKeyUtil.Deserialize(keyValPack.getKeys(i), serializationContext);
                                CompressedValueEntry entry = new CompressedValueEntry();

                                this.setValue(keyValPack.getValues(i).getDataList());
                                entry.setValue(this.getValue());

                                entry.Flag = new com.alachisoft.tayzgrid.common.BitSet((byte) keyValPack.getFlag(i));
                                HashMap.put(key, entry);

                                this.value = null;
                            }
                        }
                        this.result = HashMap;
                        resultSet.setSearchEntriesResult((HashMap) this.result);
                        break;
                }
            }
            break;
            case ADD_BULK:
                this.req_id = response.getRequestId();
                this._intendedRecipient = response.getIntendedRecipient();
                KeyExceptionPackageResponseProtocol.KeyExceptionPackageResponse excPack =
                response.getBulkAdd().getKeyExceptionPackage();
                hashmap = new HashMap();
                if (excPack != null)
                {
                    for (int i = 0; i < excPack.getKeysCount(); i++)
                    {
                        hashmap.put(CacheKeyUtil.Deserialize(excPack.getKeys(i), serializationContext),
                        new OperationFailedException(excPack.getExceptions(i).getException()));
                    }
                }

                this.result = hashmap;
                break;
            case INSERT_BULK:
                this.req_id = response.getRequestId();
                this._intendedRecipient = response.getIntendedRecipient();
                excPack = response.getBulkInsert().getKeyExceptionPackage();
                hashmap = new HashMap();
                if (excPack != null)
                {
                    for (int i = 0; i < excPack.getKeysCount(); i++)
                    {
                        hashmap.put(CacheKeyUtil.Deserialize(excPack.getKeys(i), serializationContext),
                                new OperationFailedException(excPack.getExceptions(i).getException()));
                    }
                }

                this.result = hashmap;
                break;

            case GET_GROUP_KEYS:
                this.req_id = response.getRequestId();
                ArrayList groupKeys = new ArrayList();
                for(ByteString key : response.getGetGroupKeys().getKeysList())
                {
                    groupKeys.add(CacheKeyUtil.Deserialize(key, serializationContext));
                }
                this.result = groupKeys;
                break;

            case GET_ENUMERATOR:
                this.req_id = response.getRequestId();                
                ArrayList enumKeys = new ArrayList();
                for(ByteString key : response.getGetEnum().getKeysList())
                {
                    enumKeys.add(CacheKeyUtil.Deserialize(key, serializationContext));
                }
                this.result = enumKeys;
                break;

            case GET_NEXT_CHUNK:
                this.req_id = response.getRequestId();

                enumPointer = new com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer(response.getGetNextChunkResponse().getEnumerationPointer().getId(),
                        response.getGetNextChunkResponse().getEnumerationPointer().getChunkId());

                chunk = new EnumerationDataChunk();
                ArrayList chunkKeys = new ArrayList();
                for(ByteString key : response.getGetNextChunkResponse().getKeysList())
                {
                    chunkKeys.add(CacheKeyUtil.Deserialize(key, serializationContext));
                }
                chunk.setData(chunkKeys);
                chunk.setPointer(enumPointer);
                _enumerationDataChunk.add(chunk);
                break;

            case GET_GROUP_NEXT_CHUNK:
                this.req_id = response.getRequestId();

                enumPointer = new com.alachisoft.tayzgrid.common.datastructures.GroupEnumerationPointer(response.getGetGroupNextChunkResponse().getGroupEnumerationPointer().getId(),
                        response.getGetGroupNextChunkResponse().getGroupEnumerationPointer().getChunkId(),
                        response.getGetGroupNextChunkResponse().getGroupEnumerationPointer().getGroup(),
                        response.getGetGroupNextChunkResponse().getGroupEnumerationPointer().getSubGroup());

                chunk = new EnumerationDataChunk();
                ArrayList groupChunkKeys = new ArrayList();
                for(ByteString key : response.getGetGroupNextChunkResponse().getKeysList())
                {
                    groupChunkKeys.add(CacheKeyUtil.Deserialize(key, serializationContext));
                }
                chunk.setData(groupChunkKeys);
                chunk.setPointer(enumPointer);
                _enumerationDataChunk.add(chunk);
                break;

            case GET_CACHE_ITEM:
                this.req_id = response.getRequestId();
                lockId = response.getGetItem().getLockId();
                if (lockId == null ? "" == null : lockId.equals("")) {
                    lockId = null;
                }
                this._lockHandle.setLockId(lockId);
                ticks = response.getGetItem().getLockTicks();
                if (ticks > 0) {

                    this._lockHandle.setLockDate(HelperFxn.getDateFromTicks(ticks));
                } else {
                    try {
                        this._lockHandle.setLockDate(new NCDateTime(1970, 1, 1, 0, 0, 0, 0).getDate());
                    } catch (Exception e) {
                    }
                }
                if (response.getGetItem().getValueCount() > 0) {
                    this.setValue(response.getGetItem().getValueList());

                    this.item = new CacheItem(this.getValue());
                    this.value = null;

                    long absExpiration = response.getGetItem().getAbsExp();
                    long sldExpiration = response.getGetItem().getSldExp();

                    if (absExpiration > 0) {
                        item.setAbsoluteExpiration(HelperFxn.getDateFromUTCTicks(absExpiration));
                    }
                    else
                    {
                        item.setAbsoluteExpiration(Cache.DefaultAbsoluteExpiration);
                   }

                    if (sldExpiration > 0) {
                        item.setSlidingExpiration(new TimeSpan(sldExpiration));

                    }
                    else
                    {
                        item.setSlidingExpiration(Cache.DefaultSlidingExpiration);
					}

                    item.setPriority(CacheItemPriority.forValue(response.getGetItem().getPriority()));
                    item.setResyncExpiredItems(response.getGetItem().getNeedsResync());
                    item.setGroup(response.getGetItem().getGroup());
                    item.setSubGroup(response.getGetItem().getSubGroup());
                    item.setFlag(BitSetConstants.setBitSetData(response.getGetItem().getFlag()));
                    item.setCreationTime(HelperFxn.getDateFromTicks(response.getGetItem().getCreationTime()));
                    item.setLastModifiedTime(HelperFxn.getDateFromTicks(response.getGetItem().getLastModifiedTime()));

                    item.setVersion(HelperUtil.createCacheItemVersion(response.getGetItem().getVersion()));

                    if (response.getGetItem().getTagInfo() != null) {
                        TagInfoProtocol.TagInfo info = response.getGetItem().getTagInfo();
                        int tagCount = info.getTagsCount();
                        Tag[] tags = new Tag[tagCount];
                        for (int i = 0; i < tagCount; i++) {
                            tags[i] = new Tag(info.getTags(i));
                        }
                        if (tagCount > 0) {
                            item.setTags(tags);
                        }

                    }

                    if (response.getGetItem().getNamedTagInfo() != null) {
                        NamedTagInfoProtocol.NamedTagInfo namedTags = response.getGetItem().getNamedTagInfo();
                        NamedTagsDictionary namedTagDictionary = new NamedTagsDictionary();
                        int namedTagCount = namedTags.getNamesCount();
                        for (int i = 0; i < namedTagCount; i++) {
                            try {
                                String type = namedTags.getTypes(i);
                                Class cls = Class.forName(namedTags.getTypes(i));

                                if (cls == Integer.class) {
                                    namedTagDictionary.add(namedTags.getNames(i), Integer.parseInt(namedTags.getVals(i)));
                                } else if (cls == Long.class) {
                                    namedTagDictionary.add(namedTags.getNames(i), Long.parseLong(namedTags.getVals(i)));
                                } else if (cls == Float.class) {
                                    namedTagDictionary.add(namedTags.getNames(i), Float.parseFloat(namedTags.getVals(i)));
                                } else if (cls == Double.class) {
                                    namedTagDictionary.add(namedTags.getNames(i), Double.parseDouble(namedTags.getVals(i)));
                                } else if (cls == String.class) {
                                    namedTagDictionary.add(namedTags.getNames(i), namedTags.getVals(i));
                                } else if (cls == Character.class) {
                                    namedTagDictionary.add(namedTags.getNames(i), namedTags.getVals(i).charAt(0));
                                } else if (cls == Boolean.class) {
                                    namedTagDictionary.add(namedTags.getNames(i), Boolean.getBoolean(namedTags.getVals(i)));
                                } else if (cls == Date.class) {
                                    namedTagDictionary.add(namedTags.getNames(i), HelperFxn.getDateFromTicks(Long.parseLong(namedTags.getVals(i))));
                                }

                            } catch (ClassNotFoundException clsNotFoundException) {
                            } catch (ArgumentException aE) {
                                throw new OperationFailedException(aE.getMessage());
                            }
                        }

                        if (namedTagCount > 0) {
                            item.setNamedTags(namedTagDictionary);
                        }
                    }
                
                }
                break;

            case GET_TYPEINFO_MAP:
                this.req_id = response.getRequestId();
                String map = response.getGetTypemap().getMap();
                if (map != null && !map.equals("")) {
                    this._typeMap = new TypeInfoMap(map);
                }
                break;

            case GET_HASHMAP:
                this.req_id = response.getRequestId();

                NewHashMap newHashMap = null;

                if (response.getGetHashmap().getKeyValuePairCount() > 0) {

                    HashMap<Integer, String> newMap = new HashMap<Integer, String>();
                    List<KeyValuePair> keyVal = response.getGetHashmap().getKeyValuePairList();
                    int size = keyVal.size();

                    for (int i = 0; i < size; i++) {
                        newMap.put(Integer.parseInt(keyVal.get(i).getKey()),
                                keyVal.get(i).getValue());
                    }

                    newHashMap = new NewHashMap(response.getGetHashmap().getViewId(),
                            newMap, response.getGetHashmap().getMembersList(),
                            response.getGetHashmap().getBucketSize(), false);
                }

                this.result = newHashMap;

                break;

            case HASHMAP_CHANGED_EVENT:
                this.req_id = response.getRequestId();
                this.setValue(response.getHashmapChanged().getTable().toByteArray());
                break;

            case ADD_ATTRIBUTE:
                this.req_id = response.getRequestId();
                this._success = response.getAddAttributeResponse().getSuccess();
                break;
            case GET_RUNNING_SERVERS:
                this.req_id = response.getRequestId();
                if (response.getGetRunningServer().getKeyValuePairCount() > 0) {

                    List<KeyValuePair> keyVal = response.getGetRunningServer().getKeyValuePairList();
                    int size = keyVal.size();

                    for (int i = 0; i < size; i++) {
                        runningServers.put(keyVal.get(i).getKey(),
                                Integer.parseInt(keyVal.get(i).getValue()));
                    }

                }
                break;
            case BULK_EVENT:
                List<BulkEventItemResponse> bulkEventList = response.getBulkEventResponse().getEventListList();
                if (bulkEventList != null) {
                    _eventList = bulkEventList;
                }
            case EXCEPTION:
                this.req_id = response.getRequestId();
                this.expType = response.getException().getType();
                this.expMessage = response.getException().getMessage();
                break;

            case SYNC_EVENTS:
                this.req_id = response.getRequestId();
                _response = response;
                break;
            case REMOVE_QUERY:
            case DELETE_QUERY:
                this.req_id = response.getRequestId();
                this._removedKeyCount = response.getRemoveQueryResponse().getRemovedKeyCount();
                break;
            case BLOCK_ACTIVITY:
                _response = response;
                break;
            case UNBLOCK_ACTIVITY:
                _response = response;
                break;
            case GET_NEXT_RECORD:
                this.req_id = response.getRequestId();
                com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorResultProtocol.TaskEnumeratorResult EnumeratorResult = response.getGetNextRecordResponse().getTaskEnumeratorResult();
                TaskEnumeratorResult enumeratorResult = new TaskEnumeratorResult();
                enumeratorResult.setIsLastResult(EnumeratorResult.getIsLastResult());
                enumeratorResult.setNodeAddress(EnumeratorResult.getNodeAddress());
                TaskEnumeratorPointer pointer = new TaskEnumeratorPointer(EnumeratorResult.getPointer().getClientId(), EnumeratorResult.getPointer().getTaskId(),(short) EnumeratorResult.getPointer().getCallbackId());
                pointer.setClientAddress(new Address(EnumeratorResult.getPointer().getClientIp(), EnumeratorResult.getPointer().getClientPort()));
                pointer.setClusterAddress(new Address(EnumeratorResult.getPointer().getClusterIp(), EnumeratorResult.getPointer().getClusterPort()));
                
                Object key = EnumeratorResult.getEntry().getKey();
                Object value = EnumeratorResult.getEntry().getValue();
                try {
                    if(key != null && key instanceof ByteString) {
                        key  = CompactBinaryFormatter.fromByteBuffer(((ByteString) key).toByteArray(), cacheId);
                    }
                }catch(Exception ex) {
                    key = null;
                }
                try {    
                    if(value != null && value instanceof ByteString) {
                        value = CompactBinaryFormatter.fromByteBuffer(((ByteString) value).toByteArray(), cacheId);
                    }
                }catch(Exception ex) {
                    value = null;
                }
                if(key == null && value == null) {
                    enumeratorResult.setIsLastResult(true);
                    enumeratorResult.setRecordSet(null);
                } else {
                    Map.Entry nextRecordSet = new AbstractMap.SimpleEntry(key, value);
                    enumeratorResult.setRecordSet(nextRecordSet);
                }
                _enumeratorResultSet = enumeratorResult;
                break;
            case GET_TASK_ENUMERATOR:
                this.req_id = response.getRequestId();
                GetTaskEnumeratorResponseProtocol.GetTaskEnumeratorResponse getTaskEnumeratorResponse = response.getGetTaskEnumeratorResponse();
                for(com.alachisoft.tayzgrid.common.protobuf.TaskEnumeratorResultProtocol.TaskEnumeratorResult taskEnumeratorResult : getTaskEnumeratorResponse.getTaskEnumeratorResultList())
                {
                    TaskEnumeratorResult enumeratorResultSet = new TaskEnumeratorResult();
                    enumeratorResultSet.setIsLastResult(taskEnumeratorResult.getIsLastResult());
                    enumeratorResultSet.setNodeAddress(taskEnumeratorResult.getNodeAddress());
                    TaskEnumeratorPointer pointerGTE = new TaskEnumeratorPointer(taskEnumeratorResult.getPointer().getClientId(), taskEnumeratorResult.getPointer().getTaskId(),(short) taskEnumeratorResult.getPointer().getCallbackId());
                    pointerGTE.setClientAddress(new Address(taskEnumeratorResult.getPointer().getClientIp(), taskEnumeratorResult.getPointer().getClientPort()));
                    pointerGTE.setClusterAddress(new Address(taskEnumeratorResult.getPointer().getClusterIp(), taskEnumeratorResult.getPointer().getClusterPort()));
                    enumeratorResultSet.setPointer(pointerGTE);
                    
                    Object key1 = taskEnumeratorResult.getEntry().getKey();
                    Object value1 = taskEnumeratorResult.getEntry().getValue();
                    try {
                        if(key1 != null && key1 instanceof ByteString) {
                            key1  = CompactBinaryFormatter.fromByteBuffer(((ByteString) key1).toByteArray(), cacheId);
                        }
                    }catch(Exception ex) {
                        key1 = null;
                    }
                    try {    
                        if(value1 != null && value1 instanceof ByteString) {
                            value1 = CompactBinaryFormatter.fromByteBuffer(((ByteString) value1).toByteArray(), cacheId);
                        }
                    }catch(Exception ex) {
                        value1 = null;
                    }
                    if(key1 == null && value1 == null) {
                        enumeratorResultSet.setIsLastResult(true);
                        enumeratorResultSet.setRecordSet(null);
                    } else {
                        Map.Entry recordSet = new AbstractMap.SimpleEntry(key1, value1);
                        enumeratorResultSet.setRecordSet(recordSet);
                    }
                    _taskEnumerator.add(enumeratorResultSet);
                }
                break;
        }

    }

    public CacheItem getItem() {
        return this.item;
    }

    public byte[] getValue() {

        return value;

    }

    public com.alachisoft.tayzgrid.caching.CompressedValueEntry getFlagValueEntry() {
        return _flagValueEntry;
    }

    public void setValue(byte[] val) {
        if (val != null) {
            value = new byte[val.length];
            System.arraycopy(val, 0, value, 0, val.length);
        }
    }

    public synchronized long getRequestId() {
        return req_id;
    }

    public int getRegisteredCallbacks() {
        return registeredCallback;
    }

    public CacheItemRemovedReason getReason() {
        switch (reason) {
         
            case 1:
                return CacheItemRemovedReason.Expired;
            case 2:
                return CacheItemRemovedReason.Removed;
            default:
                return CacheItemRemovedReason.Underused;
        }
    }


    public Object getKey()
    {
        return key;
    }

    /**
     *
     * @return
     */
    public String getCacheType() {
        return this._cacheType;
    }

 

    public boolean isBrokerReset() {
        return _brokerReset;
    }

    public void setBrokerRequested(boolean value) {
        _brokerReset = value;
    }

    public void resetResponse() {
        req_id = -1;
        type = "";
        reason = 0;
        key = null;
        value = null;
    }

    public Object getResultMap() {
        return result;
    }



    private Date extractKeysFiles(String interimCommand, ArrayList keyList,
            ArrayList fileList) {
        Date startAfter = new Date();
        startAfter.getTime();

        while (interimCommand.startsWith("KEYDEPENDENCY") || interimCommand.startsWith("FILEDEPENDENCY")) {
            int interimBeginIndex = 0, interimEndIndex = 0;
            String value = null;

            interimBeginIndex = interimEndIndex + 1;
            interimEndIndex = interimCommand.indexOf('"', interimEndIndex + 1);

            while (true) {
                interimBeginIndex = interimEndIndex + 1;
                interimEndIndex = interimCommand.indexOf('"',
                        interimEndIndex + 1);

                value = interimCommand.substring(interimBeginIndex,
                        interimEndIndex);

                int valueBeginIndex = 0, valueEndIndex = 0;

                if (value.equals("STARTAFTER")) {
                    interimBeginIndex = interimEndIndex + 1;
                    interimEndIndex = interimCommand.indexOf('"',
                            interimEndIndex + 1);

                    startAfter = new Date(Long.parseLong(interimCommand.substring(interimBeginIndex, interimEndIndex)));

                    interimBeginIndex += valueBeginIndex;
                    interimEndIndex += valueEndIndex;
                    break;
                } else {
                    if (interimCommand.startsWith("KEYDEPENDENCY")) {
                        keyList.add(value);
                    } else {
                        fileList.add(value);
                    }
                }
            }
            interimBeginIndex = interimEndIndex + 1;
            interimEndIndex = interimCommand.indexOf('\n', interimEndIndex + 1);
            interimCommand = interimCommand.substring(interimEndIndex + 1);
        }
        return startAfter;

    }



    public byte[] getRawResult() {
        return rawResult;
    }

    public void setRawResult(byte[] rawResult) {
        this.rawResult = rawResult;
    }

    public void setResultMap(Object result) {
        this.result = result;
    }

    public ExceptionProtocol.Exception.Type getExpValue() {
        return expType;
    }

    public String getExpMessage() {
        return this.expMessage;
    }

    private Object getObject(byte[] serializedObject) {
        Object obj = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(serializedObject);
        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();

        } catch (Exception ex) {
            return null;
        }
        return obj;
    }

    public void setIp(String address) {
        ip = address;
    }

    public String getIp() {
        return ip;
    }

    public void setPort(int p) {
        port = p;
    }

    public int getPort() {
        return port;
    }

    public String getCacheId() {
        return cacheId;
    }

    public void setCacheId(String cacheId) {
        this.cacheId = cacheId;
    }

    public TypeInfoMap getTypeMap() {
        return _typeMap;
    }


    public LockHandle getLockHandle() {
        return this._lockHandle;
    }

    public boolean getLockAcquired() {
        return this._lockAcquired;
    }

    /**
     * @return the isRunning
     */
    public boolean getIsRunning() {
        return isRunning;
    }

    /**
     * @param isRunning the isRunning to set
     */
    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }
	/**
     * @return the _expirationOrdinal
     */
    public int getExpirationOrdinal() {
        return _expirationOrdinal;
    }

    /**
     * @return the _expirationUnitOrdinal
     */
    public int getExpirationUnitOrdinal() {
        return _expirationUnitOrdinal;
    }

    /**
     * @return the _expirationDuration
     */
    public long getExpirationDuration() {
        return _expirationDuration;
    }
/**
     * @return the runningTasks
     */
    public List getRunningTasks() {
        return runningTasks;
    }
    /**
     * @param runningTasks the runningTasks to set
     */
    public void setRunningTasks(List runningTasks) {
        this.runningTasks = runningTasks;
    }

    /**
     * @return the _taskEnumerator
     */
    public ArrayList<TaskEnumeratorResult> getTaskEnumerator() {
        return _taskEnumerator;
    }

    /**
     * @param taskEnumerator the _taskEnumerator to set
     */
    public void setTaskEnumerator(ArrayList<TaskEnumeratorResult> taskEnumerator) {
        this._taskEnumerator = taskEnumerator;
    }

    /**
     * @return the _enumeratorResultSet
     */
    public TaskEnumeratorResult getEnumeratorResultSet() {
        return _enumeratorResultSet;
    }

    /**
     * @param enumeratorResultSet the _enumeratorResultSet to set
     */
    public void setEnumeratorResultSet(TaskEnumeratorResult enumeratorResultSet) {
        this._enumeratorResultSet = enumeratorResultSet;
    }

    public TaskStatus getTaskProgress() {
        return taskStatus;
    }
}
