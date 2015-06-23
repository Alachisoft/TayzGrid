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

// <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~ com.alachisoft.tgcache.* ~~~~~~~~~~~~~~~~~~~~~~~~">
import com.alachisoft.tayzgrid.caching.AsyncOpResult;
import com.alachisoft.tayzgrid.caching.TagComparisonType;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;

import com.alachisoft.tayzgrid.caching.queries.QueryResultSet.*;
import com.alachisoft.tayzgrid.command.AddAttributeCommand;
import com.alachisoft.tayzgrid.command.AddCommand;

import com.alachisoft.tayzgrid.command.BulkAddCommand;
import com.alachisoft.tayzgrid.command.BulkDeleteCommand;
import com.alachisoft.tayzgrid.command.BulkGetCommand;
import com.alachisoft.tayzgrid.command.BulkInsertCommand;
import com.alachisoft.tayzgrid.command.BulkRemoveCommand;
import com.alachisoft.tayzgrid.command.ClearCommand;
import com.alachisoft.tayzgrid.command.Command;
import com.alachisoft.tayzgrid.command.CommandResponse;
import com.alachisoft.tayzgrid.command.CommandType;
import com.alachisoft.tayzgrid.command.ContainsCommand;
import com.alachisoft.tayzgrid.command.DeleteCommand;
import com.alachisoft.tayzgrid.command.DeleteQueryCommand;
import com.alachisoft.tayzgrid.command.DisposeCommand;
import com.alachisoft.tayzgrid.command.GetCacheBinding;
import com.alachisoft.tayzgrid.command.GetCacheConfigurationCommand;
import com.alachisoft.tayzgrid.command.GetCacheItemCommand;
import com.alachisoft.tayzgrid.command.GetCommand;
import com.alachisoft.tayzgrid.command.GetCountCommand;

import com.alachisoft.tayzgrid.command.GetGroupCommand;
import com.alachisoft.tayzgrid.command.GetGroupNextChunkCommand;
import com.alachisoft.tayzgrid.command.GetHashmapCommand;
import com.alachisoft.tayzgrid.command.GetKeysByTagCommand;
import com.alachisoft.tayzgrid.command.GetNextChunkCommand;
import com.alachisoft.tayzgrid.command.GetNextRecordCommand;
import com.alachisoft.tayzgrid.command.GetOptimalServer;
import com.alachisoft.tayzgrid.command.GetRunningServersCommand;
import com.alachisoft.tayzgrid.command.GetRunningTasksCommand;
import com.alachisoft.tayzgrid.command.GetTagCommand;
import com.alachisoft.tayzgrid.command.GetTaskEnumeratorCommand;
import com.alachisoft.tayzgrid.command.GetTypeInfoMap;

import com.alachisoft.tayzgrid.command.InitCommand;
import com.alachisoft.tayzgrid.command.InsertCommand;
import com.alachisoft.tayzgrid.command.InvokeEntryProcessorCommand;
import com.alachisoft.tayzgrid.command.IsLockedCommand;
import com.alachisoft.tayzgrid.command.LockCommand;
import com.alachisoft.tayzgrid.command.MapReduceTaskCallbackCommand;
import com.alachisoft.tayzgrid.command.MapReduceTaskCommand;
import com.alachisoft.tayzgrid.command.RaiseCustomEventCommand;
import com.alachisoft.tayzgrid.command.RegisterBulkKeyNotification;
import com.alachisoft.tayzgrid.command.RegisterKeyNotification;
import com.alachisoft.tayzgrid.command.RegisterNotification;
import com.alachisoft.tayzgrid.command.RemoveByTagCommand;
import com.alachisoft.tayzgrid.command.RemoveCommand;
import com.alachisoft.tayzgrid.command.RemoveGroupCommand;
import com.alachisoft.tayzgrid.command.RequestType;
import com.alachisoft.tayzgrid.command.SearchCommand;
import com.alachisoft.tayzgrid.command.TaskCancelCommand;
import com.alachisoft.tayzgrid.command.TaskProgressCommand;
import com.alachisoft.tayzgrid.command.UnRegisterBulkKeyNotification;
import com.alachisoft.tayzgrid.command.UnRegisterKeyNotification;
import com.alachisoft.tayzgrid.command.UnlockCommand;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.CacheConfigParams;
import com.alachisoft.tayzgrid.common.DeleteParams;
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.common.InsertResult;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.caching.expiration.ExpirationContract;

import com.alachisoft.tayzgrid.common.caching.expiration.ExpirationType;


import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.datastructures.GroupEnumerationPointer;
import com.alachisoft.tayzgrid.common.enums.NotificationsType;
import com.alachisoft.tayzgrid.common.logger.JLogger;
import com.alachisoft.tayzgrid.common.logger.LoggerNames;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorPointer;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse;
import static com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse.EventType.ITEM_ADDED_EVENT;
import com.alachisoft.tayzgrid.common.protobuf.DSUpdatedCallbackResponseProtocol.DSUpdatedCallbackResponse;
import com.alachisoft.tayzgrid.common.protobuf.DSUpdatedCallbackResultProtocol.DSUpdatedCallbackResult;
import com.alachisoft.tayzgrid.common.protobuf.ItemRemoveCallbackResponseProtocol.ItemRemoveCallbackResponse;
import com.alachisoft.tayzgrid.common.protobuf.ItemRemovedEventResponseProtocol.ItemRemovedEventResponse;
import com.alachisoft.tayzgrid.common.protobuf.TaskCallbackResponseProtocol;
import com.alachisoft.tayzgrid.common.threading.AsyncProcessor;
import com.alachisoft.tayzgrid.common.threading.Monitor;
import com.alachisoft.tayzgrid.common.threading.ThreadPool;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.communication.ClientConfiguration;
import com.alachisoft.tayzgrid.communication.ConnectionManager;
import com.alachisoft.tayzgrid.communication.ConnectionPool;
import com.alachisoft.tayzgrid.communication.ConnectionStatus;
import com.alachisoft.tayzgrid.communication.RemoteServer;
import com.alachisoft.tayzgrid.communication.Request;
import com.alachisoft.tayzgrid.event.CacheEvent;
import com.alachisoft.tayzgrid.event.CacheListener;
import com.alachisoft.tayzgrid.event.CacheNotificationType;
import com.alachisoft.tayzgrid.event.CacheStatusEventListener;
import com.alachisoft.tayzgrid.event.CacheStatusNotificationType;
import com.alachisoft.tayzgrid.event.ClusterEvent;
import com.alachisoft.tayzgrid.event.CommandEvent;
import com.alachisoft.tayzgrid.event.CustomEvent;
import com.alachisoft.tayzgrid.event.CustomListener;
import com.alachisoft.tayzgrid.event.Notifications;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.caching.CacheItemAttributes;
import com.alachisoft.tayzgrid.runtime.caching.NamedTagsDictionary;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.events.EventType;
import com.alachisoft.tayzgrid.runtime.exceptions.AggregateException;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.StateTransferInProgressException;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;
import com.alachisoft.tayzgrid.runtime.mapreduce.TaskStatus;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessorResult;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.serialization.standard.FormatterServices;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectInputStream;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import com.alachisoft.tayzgrid.statistics.PerfStatsCollector;
import com.alachisoft.tayzgrid.util.GUID;
import com.alachisoft.tayzgrid.util.Logs;
import com.alachisoft.tayzgrid.util.NewHashMap;
import com.alachisoft.tayzgrid.util.NotificationType;
import com.alachisoft.tayzgrid.util.VirtualArray;
import com.alachisoft.tayzgrid.web.asynctasks.AsyncOperationCompletedEventTask;
import com.alachisoft.tayzgrid.web.asynctasks.CacheClearEventTask;
import com.alachisoft.tayzgrid.web.asynctasks.CustomEventTask;
import com.alachisoft.tayzgrid.web.asynctasks.DSUpdateEventTask;
import com.alachisoft.tayzgrid.web.asynctasks.ItemAddedEventTask;
import com.alachisoft.tayzgrid.web.asynctasks.ItemRemovedCallbackTask;
import com.alachisoft.tayzgrid.web.asynctasks.ItemRemovedEventTask;
import com.alachisoft.tayzgrid.web.asynctasks.ItemUpdatedCallbackTask;
import com.alachisoft.tayzgrid.web.asynctasks.ItemUpdatedEventTask;
import com.alachisoft.tayzgrid.web.events.EventCacheItem;
import com.alachisoft.tayzgrid.web.events.EventManager.EventRegistrationInfo;
import com.alachisoft.tayzgrid.web.events.EventUtil;
import com.alachisoft.tayzgrid.web.mapreduce.MROutputOption;
import com.alachisoft.tayzgrid.web.mapreduce.TaskEnumerator;
import com.alachisoft.tayzgrid.web.persistence.PersistenceManager;
import com.google.protobuf.ByteString;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import tangible.RefObject;

// </editor-fold>
/**
 * Implements the clustered cache for an application. This class cannot be
 * inherited.
 *
 * @param <K>
 * @param <V>
 * 
 * @version 1.0
 */
public final class RemoteCache extends CacheImplBase {

    PerfStatsCollector _perfStatsCollector;
    //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constants & Variables ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    private String cacheId = null;
    private boolean exceptionsEnabled = true;
    private boolean memberjoinedFlag = false;
    private boolean memberleftFlag = false;
    private boolean cachestoppedFlag = false;
    private Level _level = Level.SEVERE;
    /* The Broker object to handle the requests. */
    /**
     * The Command Broker.
     */
    private Broker Broker = null;
    private int _compressed = BitSetConstants.Compressed;
    /*collection for the callbacks*/
    private Vector callbackQue = new Vector();
    /**
     * specify no absolute expiration.
     */
    public static Date NoAbsoluteExpiration = null;
    /**
     * specify no sliding expiration.
     */
    public static TimeSpan NoSlidingExpiration = null;
    /**
     * Disable lock expiration
     */
    public static final TimeSpan NoLockingExpiration = null;
    //<editor-fold desc=" L I S T E N E R S   L I S T">
    List<CacheListenerWrapper> _cacheCleared = Collections.synchronizedList(new ArrayList<CacheListenerWrapper>());
    List<CacheListenerWrapper> _cacheItemAdded = Collections.synchronizedList(new ArrayList<CacheListenerWrapper>());
    List<CacheListenerWrapper> _cacheItemUpdated = Collections.synchronizedList(new ArrayList<CacheListenerWrapper>());
    List<CacheListenerWrapper> _cacheItemRemoved = Collections.synchronizedList(new ArrayList<CacheListenerWrapper>());
    List<CacheStatusListenerWrapper> _memberJoined = Collections.synchronizedList(new ArrayList<CacheStatusListenerWrapper>());
    List<CacheStatusListenerWrapper> _memberLeft = Collections.synchronizedList(new ArrayList<CacheStatusListenerWrapper>());
    List<CacheStatusListenerWrapper> _cacheStopped = Collections.synchronizedList(new ArrayList<CacheStatusListenerWrapper>());
    List<CustomListenerWrapper> _customListener = Collections.synchronizedList(new ArrayList<CustomListenerWrapper>());

    private com.alachisoft.tayzgrid.serialization.util.TypeInfoMap typeMap = null;

    private boolean _addNotifRegistered;
    private boolean _updateNotifRegistered;
    private boolean _removeNotifRegistered;
    private boolean _clearNotifRegistered;
    private boolean _customNotifRegistered;
    private int _refCounter = 0;
    private int forcedViewId = -5;

    /// <summary>Serialization context (actually name of the cache.)used for Compact Framework </summary>
    private String _serializationContext;
    private ThreadPoolExecutor tExec = new ThreadPoolExecutor(1, 25, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(25));
    private Cache _parent;

    private RemoteCacheAsyncEventsListener _asycEventsListener;
    private RemoteCacheCacheEventsListener _EventsListener;
    private RemoteCacheClusterEventsListener _clusterEventsListener;

    public RemoteCacheAsyncEventsListener getAsyncEventsListener() {
        return this._asycEventsListener;
    }

    public RemoteCacheCacheEventsListener getEventsListener() {
        return this._EventsListener;
    }

    public RemoteCacheClusterEventsListener getClusterEventsListener() {
        return this._clusterEventsListener;
    }

    protected List<CustomListenerWrapper> getCustomEventListenerList() {
        return this._customListener;
    }

    /**
     * Maximum threads allowed at a time is 25, but if they ever exceed the old
     * threads if inactive will be given a grace period of 60 seconds before
     * terminating them
     * <p>
     * <b><u>NOTE:</u></b> Avoid using wait or sleep in a thread assigned to
     * this pool. This pool is used for 'Do things and get out ' stuff</p>
     */
//    private ExecutorService pool = Executors.newFixedThreadPool(25, tExec.getThreadFactory());
    //</editor-fold>
    //</editor-fold>
    private RemoteCache() {

    }

    String getSerializationContext() {
        return _serializationContext;
    }

    void setSerializationContext(String value) {
        _serializationContext = value;
    }

 

    void setParentCache(Cache parentCache) {
        _parent = parentCache;
    }

    public String getName() {
        return cacheId;
    }

    @Override
    Vector getCallbackQueue() {
        return callbackQue;
    }

    @Override
    protected TypeInfoMap getTypeMap() {
        return this.typeMap;
    }

    RemoteCache(String cacheId, Cache parent, String server, int port, CacheInitParams initParams, PerfStatsCollector perfStatsCollector)
            throws GeneralFailureException, ConfigurationException,  Exception {

        this(cacheId, server, port, initParams, perfStatsCollector, parent);
        _parent = parent;

        _asycEventsListener = new RemoteCacheAsyncEventsListener(_parent.getCacheAsyncEventListener());
        _EventsListener = new RemoteCacheCacheEventsListener(_parent.getCacheEventListener());
        _clusterEventsListener = new RemoteCacheClusterEventsListener(_parent.getClusterEventsListener());
    }

    /**
     * Creates a new instance of Cache.
     *
     * @param server Name of the server to connect. This will override
     * configuration. if the client is already connected an Exception is thrown.
     * @param port the port of the server.
     * @param cacheId The cache-id to request from the server.
     * @throws ConfigurationException Thrown
     * when an exception occurs during configuration. Likely causes are badly
     * specified configuration strings.
     * @throws GeneralFailureException Thrown
     * when an exception occurs during a clustered operation.
     */
    RemoteCache(String cacheId, String server, int port, CacheInitParams initParams, PerfStatsCollector perfStatsCollector, Cache parent)
            throws GeneralFailureException, ConfigurationException,  Exception {
        _perfStatsCollector = perfStatsCollector;
        _parent = parent;
        try {
            if (System.getenv("ENABLE_JAVA_LOGS") != null) {
                if (Integer.parseInt(System.getenv("ENABLE_JAVA_LOGS")) == new Integer(1)) {
                    _level = Level.SEVERE;
                    InitializeLogging(cacheId, Level.OFF);
                }
            } else {
                _level = Level.parse(System.getProperty("enableJvCLogs", "OFF"));
                InitializeLogging(cacheId, Level.OFF);
            }
        } catch (Exception ex) {
        }
        Logger logger = Logger.getLogger("com.alachisoft");
        if (logger.isLoggable(Level.FINEST)) {
            logger.entering("com.alachisoft.tgcache.web.caching.cache", "Constructor", new Object[]{
                cacheId, server, port, initParams
            });
        }

        if (cacheId == null) {
            throw new NullPointerException("cacheId");
        }

        this.cacheId = cacheId.toLowerCase();

        Broker = new Broker(this, cacheId, initParams);

        // changed this for init param task
        RemoteServer rServer = Broker.GetInitialServer();

        Broker.SnmpAddress = perfStatsCollector.getSnmpAddress();
        Broker.startServices(cacheId, rServer.getName(), rServer.getPort(), initParams);
        if (!Broker.isConnected()) {
            throw new GeneralFailureException("No server is available to process the request.");
        }

        if (logger.isLoggable(Level.FINEST)) {
            logger.exiting("com.alachisoft.tgcache.web.caching.cache", "Constructor", this);
        }

    }

    /**
     * Keeps the number of initializations of this cache, used at dispose when
     * all threads have called in the dispose method
     */
    synchronized void addRef() {
        _refCounter++;
    }

    @Override
    public String toString() {
        return cacheId;
    }

    //<editor-fold defaultstate="collapsed" desc="ExceptionEnabled">
    /**
     * If this property is set the Cache object throws exceptions from public
     * operations. If not set no exception is thrown and the operation fails
     * silently. Setting this flag is especially helpful during development
     * phase of application since exceptions provide more information about the
     * specific causes of failure.
     *
     * @return true if exceptions are enabled, otherwise false.
     */
    public boolean isExceptionsEnabled() {
        return exceptionsEnabled;
    }

    /**
     * If this property is set the Cache object throws exceptions from public
     * operations. If not set no exception is thrown and the operation fails
     * silently. Setting this flag is especially helpful during development
     * phase of application since exceptions provide more information about the
     * specific causes of failure.
     *
     * @param exceptionsEnabled boolean value to enable/disable the exceptions.
     */
    public void setExceptionsEnabled(boolean exceptionsEnabled) {
        this.exceptionsEnabled = exceptionsEnabled;
    }//</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Add overloads ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    //- :20110330
    @Override
    public Object add(Object key, Object value,  java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onDsItemAddedCallback, short asyncItemAddedCallback, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, java.util.HashMap queryInfo, BitSet flagMap, String providerName, String resyncProviderName, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, long size)
            throws GeneralFailureException, OperationFailedException, AggregateException, SecurityException, ConfigurationException, ArgumentException, ConnectionException, Exception {

        AddCommand command = new AddCommand(key, (byte[]) value,   absoluteExpiration, slidingExpiration, priority, onRemoveCallback, onUpdateCallback, asyncItemAddedCallback, onDsItemAddedCallback, isResyncExpiredItems, group, subGroup, isAsync, queryInfo, flagMap, providerName, resyncProviderName, this.cacheId, itemUpdateDataFilter, itemRemovedDataFilter);

        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        if (!isAsync) {
            CommandResponse response = request.getResponse();
            exceptionOccured(response);
        }

        return value;
    }

    @Override
    public java.util.HashMap add(Object[] keys, CacheItem[] items, int[] removeCallbackIds, int[] updateCallbackIds, short onDataSourceItemsAdded, String providerName, long[] sizes) throws Exception {
        HashMap<Address, Entry<Object[], CacheItem[]>> keysDistributionMap = new HashMap<Address, Entry<Object[], CacheItem[]>>();

        Request request;
        if (Broker._importHashmap) {
            if (Broker.poolFullyDisConnected()) {
                throw new OperationFailedException("No server is available to process the request");
            }

            if (!Broker.poolFullyConnected()) {

                BulkAddCommand command = new BulkAddCommand(keys, items, updateCallbackIds, removeCallbackIds, onDataSourceItemsAdded, providerName, cacheId);
                request = Broker.createDedicatedRequest(command);
            } else {
                request = new Request(true, Broker._commandTimeout);
                Broker.getKeysDistributionMap(keys, items, keysDistributionMap);
                for (Address serverAddress : keysDistributionMap.keySet()) {
                    Entry<Object[], CacheItem[]> keysAndItems = keysDistributionMap.get(serverAddress);
                    BulkAddCommand command = new BulkAddCommand(keysAndItems.getKey(), keysAndItems.getValue(), updateCallbackIds, removeCallbackIds, onDataSourceItemsAdded, providerName, cacheId);
                    command.setClientLastViewId(Broker.getClientLastViewId());
                    request.addCommand(serverAddress, command);
                }
            }
        } else {
            BulkAddCommand command = new BulkAddCommand(keys, items, updateCallbackIds, removeCallbackIds, onDataSourceItemsAdded, providerName, cacheId);
            request = Broker.createRequest(command);
        }

        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
        return (HashMap) response.getResultMap();
    }

// </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~ Insert Overloads ~~~~~~~~~~~~~~~~~~~~~~~~~ ">
    @Override
    public InsertResult insert(Object key, Object value,   java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onDsItemAddedCallback, short asyncItemAddedCallback, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, java.util.HashMap queryInfo, BitSet flagMap, String lockId, CacheItemVersion version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName, String resyncProviderName, EventDataFilter itemAddedDataFilter, EventDataFilter itemRemovedDataFilter, long size, InsertParams options) throws OperationFailedException, GeneralFailureException, AggregateException, SecurityException, ConnectionException {


        InsertCommand command = new InsertCommand(key, (byte[]) value,  absoluteExpiration, slidingExpiration, priority, onRemoveCallback, onUpdateCallback, asyncItemAddedCallback, onDsItemAddedCallback, isResyncExpiredItems, group, subGroup, isAsync, queryInfo, (version
                == null) ? 0 : version.getVersion(), flagMap, lockId, accessType, providerName, resyncProviderName, cacheId, itemAddedDataFilter, itemRemovedDataFilter, options);

        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        InsertResult result = null;

        if (!isAsync) {
            CommandResponse response = request.getResponse();

            if (this.isExceptionsEnabled()) {
                exceptionOccured(response);
            }
            result = new InsertResult();
            result.Success = response.success();
            result.Version = response.getVersion();
            response.getFlagValueEntry().Value = response.getValue();
            result.ExistingValue = response.getFlagValueEntry();
        }
        return result;
    }

    @Override
    public java.util.HashMap insert(Object[] keys, CacheItem[] items, int[] removeCallbackIds, int[] updateCallbackIds, short onDsItemsUpdatedCallback, String providerName, long[] sizes) throws Exception {
        HashMap<Address, Entry<Object[], CacheItem[]>> keysDistributionMap = new HashMap<Address, Entry<Object[], CacheItem[]>>();

        Request request;
        if (Broker._importHashmap) {
            if (Broker.poolFullyDisConnected()) {
                throw new OperationFailedException("No server is available to process the request");
            }

            if (!Broker.poolFullyConnected()) {
                BulkInsertCommand command = new BulkInsertCommand(keys, items, updateCallbackIds, removeCallbackIds, onDsItemsUpdatedCallback, providerName, cacheId);
                request = Broker.createDedicatedRequest(command);
            } else {
                request = new Request(true, Broker._commandTimeout);
                Broker.getKeysDistributionMap(keys, items, keysDistributionMap);
                for (Address serverAddress : keysDistributionMap.keySet()) {
                    Entry<Object[], CacheItem[]> keysAndItems = keysDistributionMap.get(serverAddress);
                    BulkInsertCommand command = new BulkInsertCommand(keysAndItems.getKey(), keysAndItems.getValue(), updateCallbackIds, removeCallbackIds, onDsItemsUpdatedCallback, providerName, cacheId);
                    command.setClientLastViewId(Broker.getClientLastViewId());
                    request.addCommand(serverAddress, command);
                }
            }
        } else {
            BulkInsertCommand command = new BulkInsertCommand(keys, items, updateCallbackIds, removeCallbackIds, onDsItemsUpdatedCallback, providerName, cacheId);
            request = Broker.createRequest(command);
        }

        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
        return (HashMap) response.getResultMap();
    }

    @Override
    public void insertAsync(Object key, Object value, java.util.Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, short onRemoveCallback, short onUpdateCallback, short onAsyncItemUpdateCallback, short onDsItemUpdatedCallback, boolean isResyncExpiredItems, String group, String subGroup, java.util.HashMap queryInfo, BitSet flagMap, String providerName, String resyncProviderName, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemoveDataFilter, long size) throws Exception {

    }
    //- :20110330
// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Remove Overloads ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    //+ :20110330
    @Override
    public com.alachisoft.tayzgrid.caching.CompressedValueEntry remove(Object key, BitSet flagMap, short asyncItemRemovedCallback, short dsItemRemovedCallbackId, boolean isAsync, String lockId, CacheItemVersion version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String ProviderName) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Command command = new RemoveCommand(key, flagMap, lockId, asyncItemRemovedCallback, dsItemRemovedCallbackId, version != null ? version.getVersion() : 0, accessType, isAsync, ProviderName);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        com.alachisoft.tayzgrid.caching.CompressedValueEntry result = null;
        if (!isAsync) {
            CommandResponse response = request.getResponse();

            exceptionOccured(response);
            response.getFlagValueEntry().Value = response.getValue();
            result = response.getFlagValueEntry();
        }
        return result;
    }

    @Override
    public java.util.HashMap remove(Object[] keys, BitSet flagMap, String providerName, short onDsItemsRemovedCallback) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        HashMap<Address, Entry<Object[], CacheItem[]>> keysDistributionMap = new HashMap<Address, Entry<Object[], CacheItem[]>>();
        Request request;

        if (Broker._importHashmap) {
            if (Broker.poolFullyDisConnected()) {
                throw new OperationFailedException("No server is available to process the request");
            }

            if (!Broker.poolFullyConnected()) {
                BulkRemoveCommand command = new BulkRemoveCommand(keys, flagMap, providerName, onDsItemsRemovedCallback);
                request = Broker.createDedicatedRequest(command);
            } else {
                request = new Request(true, Broker._commandTimeout);
                Broker.getKeysDistributionMap(keys, null, keysDistributionMap);
                for (Address serverAddress : keysDistributionMap.keySet()) {
                    Entry<Object[], CacheItem[]> keysAndItems = keysDistributionMap.get(serverAddress);
                    BulkRemoveCommand command = new BulkRemoveCommand(keysAndItems.getKey(), flagMap, providerName, onDsItemsRemovedCallback);
                    command.setClientLastViewId(Broker.getClientLastViewId());
                    request.addCommand(serverAddress, command);
                }
            }
        } else {
            BulkRemoveCommand command = new BulkRemoveCommand(keys, flagMap, providerName, onDsItemsRemovedCallback);
            request = Broker.createRequest(command);
        }

        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();
        exceptionOccured(response);
        return (HashMap) response.getResultMap();

    }

    @Override
    public boolean delete(Object key, BitSet flagMap, short asyncItemRemovedCallback, short dsItemRemovedCallbackId, boolean isAsync, String lockId, CacheItemVersion version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String ProviderName, DeleteParams deleteParams) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new DeleteCommand(key, flagMap, lockId, asyncItemRemovedCallback, dsItemRemovedCallbackId, version.getVersion(), accessType, isAsync, ProviderName, deleteParams);

        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        if (!isAsync) {
            CommandResponse response = request.getResponse();

            exceptionOccured(response);
            return response.success();
        }
        return false;
    }

    @Override
    public void delete(Object[] keys, BitSet flagMap, String providerName, short onDsItemsRemovedCallback) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        HashMap<Address, Entry<Object[], CacheItem[]>> keysDistributionMap = new HashMap<Address, Entry<Object[], CacheItem[]>>();
        Request request;

        if (Broker._importHashmap) {
            if (Broker.poolFullyDisConnected()) {
                throw new OperationFailedException("No server is available to process the request");
            }

            if (!Broker.poolFullyConnected()) {
                BulkDeleteCommand command = new BulkDeleteCommand(keys, flagMap, providerName, onDsItemsRemovedCallback);
                request = Broker.createDedicatedRequest(command);
            } else {
                request = new Request(true, Broker._commandTimeout);
                Broker.getKeysDistributionMap(keys, null, keysDistributionMap);
                for (Address serverAddress : keysDistributionMap.keySet()) {
                    Entry<Object[], CacheItem[]> keysAndItems = keysDistributionMap.get(serverAddress);
                    BulkDeleteCommand command = new BulkDeleteCommand(keysAndItems.getKey(), flagMap, providerName, onDsItemsRemovedCallback);
                    command.setClientLastViewId(Broker.getClientLastViewId());
                    request.addCommand(serverAddress, command);
                }
            }
        } else {
            BulkDeleteCommand command = new BulkDeleteCommand(keys, flagMap, providerName, onDsItemsRemovedCallback);
            request = Broker.createRequest(command);
        }

        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        exceptionOccured(response);
    }

    //- :20110330
    /**
     * Remove the group from cache.
     *
     * @param group group to be removed.
     * @param subGroup subGroup to be removed.
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to
     * perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    @Override
    public void remove(String group, String subGroup)
            throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new RemoveGroupCommand(group, subGroup, false);
        Request request = Broker.createRequest(command);
        CommandResponse response = null;
        try {
            Broker.executeRequest(request);
            response = request.getResponse();

            response.parseResponse(cacheId);

            exceptionOccured(response);

        } catch (StateTransferInProgressException ex) {

            command = new RemoveGroupCommand(group, subGroup, false);
            request = Broker.createDedicatedRequest(command);

            try {

                Broker.executeRequest(request);
                response = request.getResponse();
                response.parseResponse(cacheId);
                exceptionOccured(response);
            } catch (Exception exception) {
                throw new OperationFailedException(exception.getMessage());
            }
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

    }// </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Clear Overloads ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    /**
     * Removes all elements from the Cache.
     *
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to
     * perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    @Override
    public void clear(BitSet flagMap, short onDsClearedCallback, boolean isAsync, String providerName) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new ClearCommand(isAsync, flagMap, -1, onDsClearedCallback, providerName);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        if (!isAsync) {
            CommandResponse response = request.getResponse();
            if (this.isExceptionsEnabled()) {
                exceptionOccured(response);
            }
        }
    }

    /**
     * Removes all elements from the Cache asynchronously.
     *
     * @param dsWriteOption option regarding updating data source.
     * @param onAsyncCacheCleared Callback that returns the result of the
     * operation
     * @param onDataSourceCleared callback; if provided, is called when data
     * source is cleared.
     * @throws GeneralFailureException
     * @throws OperationFailedException
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws SecurityException
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    @Override
    public void clearAsync(BitSet flagMap, short onAsyncCacheClearCallback, short onDsClearedCallback, boolean isAsync, String providerName) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new ClearCommand(isAsync, flagMap, onAsyncCacheClearCallback, onDsClearedCallback, providerName);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        if (!isAsync) {
            CommandResponse response = request.getResponse();
            if (this.isExceptionsEnabled()) {
                exceptionOccured(response);
            }
        }
    }

//</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~ Search & SearchEntries ~~~~~~~~~~~~~~~~~~~~~~~~">
    @Override
    public com.alachisoft.tayzgrid.caching.queries.QueryResultSet search(String query, java.util.HashMap values) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new SearchCommand(query, values, false, false);
        Request request = Broker.createRequest(command);

        CommandResponse response = null;
        OperationFailedException exception = null;
        try {
            Broker.executeRequest(request);
            response = request.getResponse();

        } catch (ActivityBlockedException ex) {
            command = new SearchCommand(query, values, false, false);
            try {
                response = ExecuteCacheRequest(command, true);
            } catch (Exception ex1) {
                exception = new OperationFailedException(ex1.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }
        try {
            exceptionOccured(response);
        } catch (StateTransferInProgressException ex) {

            try {
                command = new SearchCommand(query, values, false, false);
                request = Broker.createDedicatedRequest(command);
                Broker.executeRequest(request);
                response = request.getResponse();
                exceptionOccured(response);
            } catch (Exception e) {
                exception = new OperationFailedException(e.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }

        if (this.isExceptionsEnabled() && exception != null) {
            throw exception;
        }

        com.alachisoft.tayzgrid.caching.queries.QueryResultSet queryResultSet = response.getQueryResultSet();
        return queryResultSet;

    }

    @Override
    public com.alachisoft.tayzgrid.caching.queries.QueryResultSet searchEntries(String query, java.util.HashMap values) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new SearchCommand(query, values, true, false);
        Request request = Broker.createRequest(command);

        CommandResponse response = null;
        OperationFailedException exception = null;
        try {
            Broker.executeRequest(request);
            response = request.getResponse();
        } catch (ActivityBlockedException ex) {
            command = new SearchCommand(query, values, true, false);
            try {
                response = ExecuteCacheRequest(command, true);
            } catch (Exception ex1) {
                exception = new OperationFailedException(ex1.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }
        try {
            exceptionOccured(response);
        } catch (StateTransferInProgressException ex) {

            try {
                command = new SearchCommand(query, values, true, false);
                request = Broker.createDedicatedRequest(command);
                Broker.executeRequest(request);
                response = request.getResponse();
                exceptionOccured(response);
            } catch (Exception e) {
                exception = new OperationFailedException(e.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }

        if (this.isExceptionsEnabled() && exception != null) {
            throw exception;
        }
        return response.getQueryResultSet();
    }

    /**
     * Remove on the <see cref="Cache"/> based on the query specified.
     *
     * @param query simple SQL like query syntax t oquery objects from cache
     * @return Returns a dictionary containing removed cache keys and associated
     * objects
     * <example>
     * These operators are supported by NCache Queries. 1. Comparison Operators
     * = , == , != , <> , < , > , <=, >=, IN 2. Logical Operators AND , OR , NOT
     * 3. Miscellaneous () , DateTime.Now , DateTime("any date time compatible
     * string")
     *
     * <code>
     *
     * "delete Test.Application.Employee where this.Name = 'Paul Jones'"
     * "delete Test.Application.Employee where this.Salary > 2000"
     * "delete Test.Application.Employee where this.Name = 'Paul Jones' and this.Salary > 2000"
     * "delete Test.Application.Employee where Not(this.Name = 'Paul Jones' and this.Salary > 2000)"
     *
     * </code>
     * </example>
     */
    @Override
    public int executeNonQuery(String query, HashMap values)
            throws OperationFailedException, Exception {
        Request request;
        CommandResponse res = null;
        DeleteQueryCommand command = new DeleteQueryCommand(query, values, true);

        OperationFailedException exception = null;
        try {
            request = Broker.createRequest(command);
            Broker.executeRequest(request);
            res = request.getResponse();
            exceptionOccured(res);
        } catch (StateTransferInProgressException ex) {
            try {
                command = new DeleteQueryCommand(query, values, true);
                request = Broker.createDedicatedRequest(command);
                Broker.executeRequest(request);
                res = request.getResponse();
                exceptionOccured(res);
            } catch (Exception e) {
                exception = new OperationFailedException(e.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }

        if (this.isExceptionsEnabled() && exception != null) {
            throw exception;
        }

        return res.getRemovedKeyCount();

    }

    @Override
    public void unRegisterGeneralNotification(EnumSet<EventType> eventEnumSet, short sequenceNumber) throws GeneralFailureException,  OperationFailedException, ConnectionException, AggregateException {
        NotificationsType notificationsType;

        if (eventEnumSet.contains(EventType.ItemAdded)) {
            notificationsType = NotificationsType.UnregAddNotif;
        } else if (eventEnumSet.contains(EventType.ItemRemoved)) {
            notificationsType = NotificationsType.UnregRemoveNotif;
        } else if (eventEnumSet.contains(EventType.ItemUpdated)) {
            notificationsType = NotificationsType.UnregUpdateNotif;
        } else if (eventEnumSet.contains(EventType.CacheCleared)) {
            notificationsType = NotificationsType.UnregClearNotif;
        } else {
            return;
        }

        RegisterNotification command = new RegisterNotification(notificationsType.getValue(), sequenceNumber);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();
        exceptionOccured(response);

    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~ Contains ~~~~~~~~~~~~~~~~~~~~~~~~">
    /**
     * Determines whether the cache contains a specific key.
     *
     * @return true if the Cache contains an element with the specified key;
     * otherwise, false.
     * @param key The key to locate in the Cache.
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws SecurityException Thrown when current user is not allowed to
     * perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    @Override
    public boolean contains(Object key)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {

        Command command = new ContainsCommand(key);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();
        exceptionOccured(response);
        return response.exists();
        //return new String(res.getValue()).equals("1") ? true : false;
    }
    //</editor-fold>

    @Override
    public CacheConfigParams getCacheConfiguration() throws GeneralFailureException, OperationFailedException, AggregateException, com.alachisoft.tayzgrid.runtime.exceptions. ConfigurationException, ConnectionException {
        Command command = new GetCacheConfigurationCommand();
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        exceptionOccured(response);
        return response.cacheConfigParams();
    }


    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~ Get Overloads GetCacheItem GetGroupData GetGroupKeys ~~~~~~~ ">
    @Override

    public com.alachisoft.tayzgrid.caching.CompressedValueEntry get(Object key, BitSet flagMap, String group, String subGroup, CacheItemVersion version, LockHandle lockHandle, TimeSpan lockTimeout, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        GetCommand command = new GetCommand(key, flagMap, group, subGroup, accessType, lockHandle.getLockId(), lockTimeout, version.getVersion(), false, providerName);

        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }

        if (response.getValue() == null) {
            lockHandle.setLockId(response.getLockHandle().getLockId());
            lockHandle.setLockDate(response.getLockHandle().getLockDate());
            version.setVersion(response.getVersion());
            return null;
        } else {
            lockHandle.setLockId(response.getLockHandle().getLockId());
            lockHandle.setLockDate(response.getLockHandle().getLockDate());
            version.setVersion(response.getVersion());
            response.getFlagValueEntry().Value = response.getValue();
            return response.getFlagValueEntry();

        }

    }

    public final CommandResponse ExecuteCacheRequest(Command command, boolean isDedicated) throws Exception {
        Request request = null;

        if (isDedicated) {
            request = Broker.createDedicatedRequest(command);
        } else {
            request = Broker.createRequest(command);
        }

        Broker.executeRequest(request);

        if (!request.isAsync()) {
            return request.getResponse();
        }
        return null;

    }

    @Override
    public java.util.Map get(Object[] keys, BitSet flagMap, String providerName, short jCacheCompletionListener, boolean replaceExistingValues, boolean isAsync) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {

        HashMap<Address, Entry<Object[], CacheItem[]>> keysDistributionMap = new HashMap<Address, Entry<Object[], CacheItem[]>>();

        Request request;

        if (Broker.importHashmap() && Broker.poolFullyConnected() && !isAsync) {
            request = new Request(true, Broker._commandTimeout);
            Broker.getKeysDistributionMap(keys, null, keysDistributionMap);
            for (Address serverAddress : keysDistributionMap.keySet()) {
                Entry<Object[], CacheItem[]> keysAndItems = keysDistributionMap.get(serverAddress);
//                BulkGetCommand command = new BulkGetCommand(keysAndItems.getKey(), flagMap, providerName);
                BulkGetCommand command = new BulkGetCommand(keysAndItems.getKey(), flagMap, providerName, jCacheCompletionListener, replaceExistingValues, isAsync);
                command.setClientLastViewId(Broker.getClientLastViewId());
                //command.setIntendedRecipient(serverAddress);
                request.addCommand(serverAddress, command);
            }
        } else {
//            BulkGetCommand command = new BulkGetCommand(keys, flagMap, providerName);
            BulkGetCommand command = new BulkGetCommand(keys, flagMap, providerName, jCacheCompletionListener, replaceExistingValues, isAsync);
            request = Broker.createRequest(command);
        }
        CommandResponse response = null;
        try {

            Broker.executeRequest(request);
            if (!request.isAsync()) {
                response = request.getResponse();
            } else {
                response = null;
            }
        } catch (ActivityBlockedException ex) {
            if (request.getCommandRequestType() == RequestType.BulkRead) {
                BulkGetCommand command = new BulkGetCommand(keys, flagMap, providerName, jCacheCompletionListener, replaceExistingValues, isAsync);

                try {
                    response = ExecuteCacheRequest(command, true);
                } catch (Exception ex1) {
                    throw new OperationFailedException(ex1.getMessage());
                }

            }
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }

        return (HashMap) response.getResultMap();
    }

    //- :20110330
    /**
     * Retrieves the key and value pairs in a group or sub group. If only group
     * is specified, data for the group and all the sub groups of the group are
     * returned. If both the group and sub group are specified. Only the data
     * related to the sub group are returned.
     *
     * @return The list of keys of a group or a sub group.
     * @param group The group whose keys are to be returned.
     * @param subGroup The sub group of the group foe which keys are to be
     * returned.
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws SecurityException Thrown when current user is not allowed to
     * perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    @Override
    public Collection getGroupKeys(String group, String subGroup)
            throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new GetGroupCommand(group, subGroup, true);
        Request request = Broker.createRequest(command);

        CommandResponse response = null;
        OperationFailedException exception = null;
        try {
            Broker.executeRequest(request);
            response = request.getResponse();
            exceptionOccured(response);
        } catch (ActivityBlockedException ex) {
            command = new GetGroupCommand(group, subGroup, true);
            try {
                response = ExecuteCacheRequest(command, true);
            } catch (Exception ex1) {
                exception = new OperationFailedException(ex1.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }

        try {
            exceptionOccured(response);
        } catch (StateTransferInProgressException ex) {

            try {
                command = new GetGroupCommand(group, subGroup, true);
                request = Broker.createDedicatedRequest(command);
                Broker.executeRequest(request);
                response = request.getResponse();
                exceptionOccured(response);
            } catch (Exception e) {
                exception = new OperationFailedException(e.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }

        if (this.isExceptionsEnabled() && exception != null) {
            throw exception;
        }

        return (List<String>) response.getResultMap();
    }

    @Override
    public java.util.HashMap getGroupData(String group, String subGroup) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new GetGroupCommand(group, subGroup, false);
        Request request = Broker.createRequest(command);

        CommandResponse response = null;
        OperationFailedException exception = null;
        try {
            Broker.executeRequest(request);
            response = request.getResponse();
        } catch (ActivityBlockedException ex) {
            command = new GetGroupCommand(group, subGroup, false);
            try {
                response = ExecuteCacheRequest(command, true);
            } catch (Exception ex1) {
                exception = new OperationFailedException(ex1.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }

        try {
            exceptionOccured(response);
        } catch (StateTransferInProgressException ex) {

            try {
                command = new GetGroupCommand(group, subGroup, false);
                request = Broker.createDedicatedRequest(command);
                Broker.executeRequest(request);
                response = request.getResponse();
                exceptionOccured(response);
            } catch (Exception e) {
                exception = new OperationFailedException(e.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }

        if (this.isExceptionsEnabled() && exception != null) {
            throw exception;
        }
        return (HashMap) response.getResultMap();
    }

    @Override
    public Object getCacheItem(Object key, BitSet flagMap, String group, String subGroup, DSReadOption dsReadOption, CacheItemVersion version, LockHandle lockHandle, TimeSpan lockTimeout, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {

        Command command = new GetCacheItemCommand(key, group, subGroup, accessType, lockHandle, lockTimeout, version, providerName, flagMap);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();
        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }

        lockHandle.setLockId(response.getLockHandle().getLockId());
        lockHandle.setLockDate(response.getLockHandle().getLockDate());

        if (response.getItem() == null) {
            return null;
        }

        return response.getItem();
    }

    @Override
    public java.util.HashMap getByTag(Tag[] tags, TagComparisonType comparisonType) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException, ArgumentNullException {
        String[] sTags = tagsToStringArray(tags);
        GetTagCommand command = new GetTagCommand(sTags, comparisonType);
        Request request = Broker.createRequest(command);

        CommandResponse response = null;
        OperationFailedException exception = null;
        try {
            Broker.executeRequest(request);
            response = request.getResponse();
        } catch (ActivityBlockedException ex) {
            command = new GetTagCommand(sTags, comparisonType);
            try {
                response = ExecuteCacheRequest(command, true);
            } catch (Exception ex1) {
                exception = new OperationFailedException(ex1.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }
        try {
            exceptionOccured(response);
        } catch (StateTransferInProgressException ex) {

            try {
                command = new GetTagCommand(sTags, comparisonType);
                request = Broker.createDedicatedRequest(command);
                Broker.executeRequest(request);
                response = request.getResponse();
                exceptionOccured(response);
            } catch (Exception e) {
                exception = new OperationFailedException(e.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }

        if (this.isExceptionsEnabled() && exception != null) {
            throw exception;
        }

        if (response.getResultMap() == null) {
            return null;
        }

        return (HashMap) response.getResultMap();
    }

    @Override
    public java.util.Collection getKeysByTag(Tag[] tags, com.alachisoft.tayzgrid.caching.TagComparisonType comparisonType) throws OperationFailedException, GeneralFailureException, AggregateException,  ArgumentNullException, ConnectionException {
        String[] sTags = tagsToStringArray(tags);

        GetKeysByTagCommand command = new GetKeysByTagCommand(sTags, comparisonType);
        Request request = Broker.createRequest(command);

        CommandResponse response = null;
        OperationFailedException exception = null;
        try {
            Broker.executeRequest(request);
            response = request.getResponse();
        } catch (ActivityBlockedException ex) {
            command = new GetKeysByTagCommand(sTags, comparisonType);
            try {
                response = ExecuteCacheRequest(command, true);
            } catch (Exception ex1) {
                exception = new OperationFailedException(ex1.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }
        try {
            exceptionOccured(response);
        } catch (StateTransferInProgressException ex) {

            try {
                command = new GetKeysByTagCommand(sTags, comparisonType);
                request = Broker.createDedicatedRequest(command);
                Broker.executeRequest(request);
                response = request.getResponse();
                exceptionOccured(response);
            } catch (Exception e) {
                exception = new OperationFailedException(e.getMessage());
            }
        } catch (Exception ex) {
            exception = new OperationFailedException(ex.getMessage());
        }

        if (this.isExceptionsEnabled() && exception != null) {
            throw exception;
        }

        if (response.getResultMap() == null) {
            return null;
        }

        return (Collection) response.getResultMap();
    }

    @Override
    public void removeByTag(Tag[] tags, TagComparisonType comaprisonType) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException, ArgumentNullException {
        String[] sTags = tagsToStringArray(tags);
        Command command = new RemoveByTagCommand(sTags, comaprisonType);
        Request request = Broker.createRequest(command);
        CommandResponse response = null;
        try {
            Broker.executeRequest(request);
            response = request.getResponse();

            response.parseResponse(cacheId);

            exceptionOccured(response);

        } catch (StateTransferInProgressException ex) {

            command = new RemoveByTagCommand(sTags, TagComparisonType.BY_TAG);
            request = Broker.createDedicatedRequest(command);

            try {

                Broker.executeRequest(request);
                response = request.getResponse();
                response.parseResponse(cacheId);
                exceptionOccured(response);
            } catch (Exception exception) {
                throw new OperationFailedException(exception.getMessage());
            }
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
    }

    private String[] tagsToStringArray(Tag[] tags) throws ArgumentNullException {
        if (tags == null) {
            throw new ArgumentNullException("Tag");
        }

        String[] sTags = new String[tags.length];

        for (int i = 0; i < tags.length; i++) {
            if (sTags == null) {
                throw new ArgumentNullException("Tag is null");
            }

            sTags[i] = tags[i].getTagName();
        }
        return sTags;
    }

    private HashMap getDeserializedValueMap(HashMap map)
            throws GeneralFailureException {
        if (map == null) {
            return null;
        }
        HashMap result = new HashMap(map.size());
        for (Object entry : map.entrySet()) {
            Map.Entry<String, com.alachisoft.tayzgrid.caching.CompressedValueEntry> pair
                    = (Map.Entry<String, com.alachisoft.tayzgrid.caching.CompressedValueEntry>) entry;

            com.alachisoft.tayzgrid.caching.CompressedValueEntry valEntry = pair.getValue();
            byte[] deflatValue = (byte[]) valEntry.Value;

            result.put(pair.getKey(), getDeserializedObject(deflatValue, valEntry.Flag));

        }
        return result;
    }

    // <editor-fold defaultstate="collapsed" desc="Locking Methods">
    /**
     * *
     * Acquire a lock on an item in cache.
     *
     * @param key key of cached item to be locked.
     * @param lockTimeout TimeSpan after which the lock is automatically
     * released.
     * @param lockHandle An instance of LockHandle that will be filled in with
     * the lock information if lock is acquired successfully.
     * @return True if the lock was acquired successfully, false otherwise
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to
     * perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    @Override

    public boolean lock(Object key, TimeSpan lockTimeout, LockHandle lockHandle) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {

        Command command = new LockCommand(key, lockTimeout);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }

        if (lockHandle == null) {
            lockHandle = new LockHandle();
        }

        LockHandle respHandle = response.getLockHandle();
        if (respHandle != null) {
            lockHandle.setLockDate(respHandle.getLockDate());
            lockHandle.setLockId(respHandle.getLockId());
        }

        return response.getLockAcquired();
    }

    /**
     *
     * @param key
     * @param lockHandle
     * @return
     * @throws OperationFailedException
     * @throws SecurityException
     * @throws GeneralFailureException
     * @throws AggregateException
     * @throws ConfigurationException
     */
    @Override

    public boolean isLocked(Object key, LockHandle lockHandle) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {

        Command command = new IsLockedCommand(key, lockHandle.getLockId());
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }

        LockHandle respHandle = response.getLockHandle();
        if (respHandle != null) {
            lockHandle.setLockDate(respHandle.getLockDate());
            //deal with empty string instead of null coz protobuf dont accept null
            lockHandle.setLockId(respHandle.getLockId().equals("") ? null : respHandle.getLockId());
        }

        return response.getLockAcquired();
    }

    @Override
    public void unlock(Object key) throws Exception {
        Command command = new UnlockCommand(key);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
    }


    @Override
    public void unlock(Object key, String lockId) throws Exception {
        Command command = new UnlockCommand(key, lockId);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
    }
    // </editor-fold>

    //<editor-fold defaultstate="collapsed" desc="/                --- AddDependency ---       /">

    /**
     * Add Attribute existing cache item
     *
     * @param key Key used to reference the required object
     * @param attributes Set of attributes to be added
     * @return True of the operation succeeds otherwise false
     * @throws OperationFailedException
     */

    public boolean setAttributes(Object key, CacheItemAttributes attribute) throws OperationFailedException, GeneralFailureException, SecurityException, AggregateException, ConnectionException, Exception {
        AddAttributeCommand command = new AddAttributeCommand(key, attribute.getAbsoluteExpiration());

        Request request = Broker.createRequest(command);

        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
        return response.success();
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="/                --- Key based notifications registration ---       /">
    @Override

    public void registerKeyNotificationCallback(Object key, short update, short remove, EventDataFilter datafilter, boolean notifyOnItemExpiration) throws ConnectionException, OperationFailedException, GeneralFailureException, AggregateException {
        Command command = new RegisterKeyNotification(key, update, remove, datafilter, notifyOnItemExpiration);
        Request request;
        request = Broker.createRequest(command);

        try {
            Broker.executeRequest(request);
        } catch (ConnectionException e) {
            throw new ConnectionException(e.getMessage());
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
    }

    @Override
    public void registerKeyNotificationCallback(Object key, short updateCallbackid, short removeCallbackid, boolean notifyOnItemExpiration) throws ConnectionException, OperationFailedException, GeneralFailureException, AggregateException {
        Command command = new RegisterKeyNotification(key, updateCallbackid, removeCallbackid, notifyOnItemExpiration);
        Request request;
        request = Broker.createRequest(command);

        try {
            Broker.executeRequest(request);
        } catch (ConnectionException e) {
            throw new ConnectionException(e.getMessage());
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
    }

    @Override

    public void unRegisterKeyNotificationCallback(Object key, short updateCallbackid, short removeCallbackid) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new UnRegisterKeyNotification(key, updateCallbackid, removeCallbackid);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
    }

    @Override
    public void registerKeyNotificationCallback(Object[] keys, short updateCallbackid, short removeCallbackid) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new RegisterBulkKeyNotification(keys, updateCallbackid, removeCallbackid);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
    }

    @Override

    public void registerKeyNotificationCallback(Object[] key, short update, short remove, EventDataFilter datafilter, boolean notifyOnItemExpiration) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new RegisterBulkKeyNotification(key, update, remove, datafilter, notifyOnItemExpiration);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
    }

    @Override
    public void unRegisterKeyNotificationCallback(Object key, short update, short remove, EventType eventType) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new UnRegisterKeyNotification(key, update, remove);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
    }

    @Override
    public void unRegisterKeyNotificationCallback(Object[] key, short update, short remove, EventType eventType) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new UnRegisterBulkKeyNotification(key, update, remove);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
    }

    @Override
    public void unRegisterKeyNotificationCallback(Object[] key, short update, short remove) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        Command command = new UnRegisterBulkKeyNotification(key, update, remove);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }
    }
    //</editor-fold>



    /**
     * Raises a custom event.
     *
     * @param key The key of the event.
     * @param value The value.
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to
     * perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    public void raiseCustomEvent(Object key, Object value)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        Command command = new RaiseCustomEventCommand(key, value, false);

        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();
        exceptionOccured(response);
    }

    /**
     * Returns the cache count. Note that this count is the total item count in
     * the whole cluster.
     *
     * @return The count of the cache elements.
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to
     * perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    public long getCount()
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        Command command = new GetCountCommand();
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();
        if (response != null) {
            if (this.isExceptionsEnabled()) {
                exceptionOccured(response);
            }
            return response.getCount();

        } else {
            return 0L;
        }
    }

    /**
     * Disposes this cache instance.
     *
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    public void dispose(boolean disposing)
            throws GeneralFailureException, OperationFailedException, ConfigurationException {
        this.Broker.dispose();
        this.Broker = null;

        try {
            tExec.shutdownNow();
        } catch (Exception exp) {
        }
    }

    /**
     *
     *
     * @param res
     * @throws ConfigurationException
     * @throws OperationFailedException Thrown
     * whenever an API fails.
     * @throws GeneralFailureException Thrown
     * when an exception occurs during a clustered operation.
     * @throws AggregateException This
     * exception is thrown when multiple exceptions occur from multiple nodes.
     * It combines all the exceptions as inner exceptions and throw it to the
     * client application.
     */
    private void exceptionOccured(CommandResponse res)
            throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException {
        boolean _isStreamException = false;
        if (res != null && res.getExpMessage() != null) {
            switch (res.getExpValue()) {
                case OPERATIONFAILED:
                    throw new OperationFailedException(new String(res.getExpMessage()));
                case AGGREGATE:
                    throw new AggregateException(new String(res.getExpMessage()), null);
                case CONFIGURATION:
                    throw new GeneralFailureException(new String(res.getExpMessage()));
                case GENERALFAILURE:
                    throw new GeneralFailureException(new String(res.getExpMessage()));
                case TYPE_INDEX_NOT_FOUND:
                    throw new OperationFailedException(res.getExpMessage());
                case ATTRIBUTE_INDEX_NOT_FOUND:
                    throw new OperationFailedException(res.getExpMessage());
                case STATE_TRANSFER_EXCEPTION:
                    throw new StateTransferInProgressException(res.getExpMessage());
                case MAX_CLIENTS_REACHED:
                    throw new GeneralFailureException(res.getExpMessage());
                default:
                    throw new GeneralFailureException(new String(res.getExpMessage()));
            }
        }
        if (res != null && res.isBrokerReset()) {
            throw new ConnectionException("Connection with server lost [" + res.getResetConnectionIP() + "]");
        }
    }

    //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Serialize/Deserialize ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    @Override
    public Object SafeSerialize(Object serializableObject, String serializationContext, BitSet flag, CacheImplBase cacheImpl, RefObject<Long> size) throws GeneralFailureException {
        return super.SafeSerialize(serializableObject, serializationContext, flag, cacheImpl, size);
    }

//    @Override
//    public Object SafeDeserialize(Object serializedObject, String serializationContext, BitSet flag, CacheImplBase cacheImpl) {
//        Object deserialized = serializedObject;
//        try {
//            if (serializedObject != null && cacheImpl.getSerializationEnabled()) {
//                if (flag != null && flag.IsBitSet((byte) BitSetConstants.BinaryData)) {
//                    return serializedObject;
//                }
//                deserialized = CompactBinaryFormatter.fromByteBuffer((byte[]) serializedObject, serializationContext);
//            }
//
//        } catch (IOException ex) {
//            Logger.getLogger(RemoteCache.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (ClassNotFoundException ex) {
//            Logger.getLogger(RemoteCache.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (RuntimeException exception) {
//
//            deserialized = serializedObject;
//        }
//        return deserialized;
//
//    }
    private Object getDeserializedObject(byte[] value, com.alachisoft.tayzgrid.common.BitSet flag) throws GeneralFailureException {

        try {
            ByteArrayInputStream val = new ByteArrayInputStream(value);
            ObjectInput ow = new ObjectInputStream(val, this.cacheId);
            return ow.readObject();
        } catch (IOException iOException) {
            return value;
        } catch (ClassNotFoundException classNotFoundException) {
            throw new GeneralFailureException(classNotFoundException.getMessage());

        }
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Register Notifications ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    //+Sami:20143001
    @Override
    public void registerGeneralNotification(EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter, short sequenceNumber) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        NotificationsType notificationType;
        if (eventEnumSet.contains(EventType.ItemAdded)) {
            notificationType = NotificationsType.RegAddNotif;
        } else if (eventEnumSet.contains(EventType.ItemRemoved)) {
            notificationType = NotificationsType.RegRemoveNotif;
        } else if (eventEnumSet.contains(EventType.ItemUpdated)) {
            notificationType = NotificationsType.RegUpdateNotif;
        } else if (eventEnumSet.contains(EventType.CacheCleared)) {
            notificationType = NotificationsType.RegClearNotif;
        } else {
            return;
        }

        RegisterNotification notif = new RegisterNotification(notificationType.getValue(), dataFilter.getValue(), sequenceNumber);
        Request request = Broker.createRequest(notif);

//        response.parseResponse();
        try {
            Broker.executeRequest(request);
        } catch (ConnectionException e) {
            throw new ConnectionException(e.getMessage());
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }

    }

    //-Sami:20143001
    @Override
    protected void registerCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> registerAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        boolean cleared = false, added = false, removed = false, updated = false;

        CacheListenerWrapper wrapper = new CacheListenerWrapper(listener);
        if (registerAgainst.contains(CacheNotificationType.ALL)) {
            if (!_cacheCleared.contains(wrapper)) {
                _cacheCleared.add(wrapper);
                cleared = true;
            }

            if (!_cacheItemAdded.contains(wrapper)) {
                _cacheItemAdded.add(wrapper);
                added = true;
            }

            if (!_cacheItemRemoved.contains(wrapper)) {
                _cacheItemRemoved.add(wrapper);
                removed = true;
            }

            if (!_cacheItemUpdated.contains(wrapper)) {
                _cacheItemUpdated.add(wrapper);
                updated = true;
            }

        } else {
            if (registerAgainst.contains(CacheNotificationType.CacheCleared) && !_cacheCleared.contains(wrapper)) {
                _cacheCleared.add(wrapper);
                cleared = true;
            }

            if (registerAgainst.contains(CacheNotificationType.ItemAdded) && !_cacheItemAdded.contains(wrapper)) {
                _cacheItemAdded.add(wrapper);
                added = true;
            }

            if (registerAgainst.contains(CacheNotificationType.ItemRemoved) && !_cacheItemRemoved.contains(wrapper)) {
                _cacheItemRemoved.add(wrapper);
                removed = true;
            }

            if (registerAgainst.contains(CacheNotificationType.ItemUpdated) && !_cacheItemUpdated.contains(wrapper)) {
                _cacheItemUpdated.add(wrapper);
                updated = true;
            }
        }

        if (_cacheCleared.size() == 1 && cleared && (registerAgainst.contains(CacheNotificationType.CacheCleared)
                || registerAgainst.contains(CacheNotificationType.ALL))) {
            registerNotification(NotificationType.REGISTER_CLEAR, true);
            this._clearNotifRegistered = true;
        }

        if (_cacheItemAdded.size() == 1 && added && registerAgainst.contains(CacheNotificationType.ItemAdded)
                || registerAgainst.contains(CacheNotificationType.ALL)) {
            registerNotification(NotificationType.REGISTER_ADD, true);
            this._addNotifRegistered = true;
        }

        if (_cacheItemRemoved.size() == 1 && removed && registerAgainst.contains(CacheNotificationType.ItemRemoved)
                || registerAgainst.contains(CacheNotificationType.ALL)) {
            registerNotification(NotificationType.REGISTER_REMOVE, true);
            this._removeNotifRegistered = true;
        }

        if (_cacheItemUpdated.size() == 1 && updated && registerAgainst.contains(CacheNotificationType.ItemUpdated)
                || registerAgainst.contains(CacheNotificationType.ALL)) {
            registerNotification(NotificationType.REGISTER_INSERT, true);
            this._updateNotifRegistered = true;
        }
    }

    @Override
    protected void unregisterCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        if (unregisterAgainst.contains(CacheNotificationType.ALL)) {
            removeIfCacheListenerExists(_cacheCleared, listener);
            removeIfCacheListenerExists(_cacheItemAdded, listener);
            removeIfCacheListenerExists(_cacheItemRemoved, listener);
            removeIfCacheListenerExists(_cacheItemUpdated, listener);
        } else {
            if (unregisterAgainst.contains(CacheNotificationType.CacheCleared)) {
                removeIfCacheListenerExists(_cacheCleared, listener);
            }

            if (unregisterAgainst.contains(CacheNotificationType.ItemAdded)) {
                removeIfCacheListenerExists(_cacheItemAdded, listener);
            }

            if (unregisterAgainst.contains(CacheNotificationType.ItemRemoved)) {
                removeIfCacheListenerExists(_cacheItemRemoved, listener);
            }

            if (unregisterAgainst.contains(CacheNotificationType.ItemUpdated)) {
                removeIfCacheListenerExists(_cacheItemUpdated, listener);
            }
        }

        if (_cacheCleared.size() == 0 && unregisterAgainst.contains(CacheNotificationType.CacheCleared)) {
            unregisterNotifications(NotificationType.UNREGISTER_CLEAR);
            this._clearNotifRegistered = false;
        }

        if (_cacheItemAdded.size() == 0 && unregisterAgainst.contains(CacheNotificationType.ItemAdded)) {
            unregisterNotifications(NotificationType.UNREGISTER_ADD);
            this._addNotifRegistered = false;
        }

        if (_cacheItemRemoved.size() == 0 && unregisterAgainst.contains(CacheNotificationType.ItemRemoved)) {
            unregisterNotifications(NotificationType.UNREGISTER_REMOVE);
            this._removeNotifRegistered = false;
        }

        if (_cacheItemUpdated.size() == 0 && unregisterAgainst.contains(CacheNotificationType.ItemUpdated)) {
            unregisterNotifications(NotificationType.UNREGISTER_INSERT);
            this._updateNotifRegistered = false;
        }
    }

    private boolean removeIfCacheListenerExists(List list, java.util.EventListener listener) {
        Iterator ite = list.iterator();
        int indexOf = -1;

        if (listener instanceof CacheListener) {
            while (ite.hasNext()) {
                CacheListenerWrapper wrapper = (CacheListenerWrapper) ite.next();
                if (wrapper.verifyListenerInstance((CacheListener) listener)) {
                    indexOf = list.indexOf(wrapper);
                    break;
                }
            }
        }

        if (indexOf > -1) {
            list.remove(indexOf);
            return true;
        } else {
            return false;
        }
    }

    private boolean removeIfClusterListenerExists(List list, java.util.EventListener listener) {
        Iterator ite = list.iterator();
        int indexOf = -1;

        if (listener instanceof CacheStatusEventListener) {
            while (ite.hasNext()) {
                CacheStatusListenerWrapper wrapper = (CacheStatusListenerWrapper) ite.next();
                if (wrapper.verifyListenerInstance((CacheStatusEventListener) listener)) {
                    indexOf = list.indexOf(wrapper);
                    break;
                }
            }
        }

        if (indexOf > -1) {
            list.remove(indexOf);
            return true;
        } else {
            return false;
        }
    }

    private boolean removeIfCustomListenerExists(List list, java.util.EventListener listener) {
        Iterator ite = list.iterator();
        int indexOf = -1;

        if (listener instanceof CustomListener) {
            while (ite.hasNext()) {
                CustomListenerWrapper wrapper = (CustomListenerWrapper) ite.next();
                if (wrapper.verifyListenerInstance((CustomListener) listener)) {
                    indexOf = list.indexOf(wrapper);
                    break;
                }
            }
        }

        if (indexOf > -1) {
            list.remove(indexOf);
            return true;
        } else {
            return false;
        }
    }

    //<editor-fold defaultstate="collapsed" desc="register commands">
    /**
     * @param modifier
     * @throws OperationFailedException Thrown
     * whenever an API fails.
     * @throws GeneralFailureException Thrown
     * when an exception occurs during a clustered operation.
     * @throws AggregateException This
     * exception is thrown when multiple exceptions occur from multiple nodes.
     * It combines all the exceptions as inner exceptions and throw it to the
     * client application.
     */
    private void registerNotification(int modifier, boolean checkConnected) throws GeneralFailureException, OperationFailedException, AggregateException,  ConnectionException {

        Command command = new RegisterNotification(modifier, (short) -1);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();
        exceptionOccured(response);
    }

    private void registerNotification(ConnectionManager connection, int modifier, boolean checkConnected)
            throws OperationFailedException,  GeneralFailureException, AggregateException, ConnectionException {
        Command command = new RegisterNotification(modifier, (short) -1);

        Request request = new Request(false, Broker._commandTimeout);
        Address ipAddress = connection.getServerAddress();
        request.addCommand(ipAddress, command);

        try {
            Broker.executeRequest(request, connection, checkConnected, true);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();
        exceptionOccured(response);
    }
    //</editor-fold>

    @Override
    protected void registerCacheStatusEventlistener(CacheStatusEventListener listener, EnumSet<CacheStatusNotificationType> registerAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        boolean joined = false, left = false, stopped = false;

        CacheStatusListenerWrapper wrapper = new CacheStatusListenerWrapper(listener);
        if (registerAgainst.contains(CacheStatusNotificationType.ALL)) {
            if (!_memberJoined.contains(wrapper)) {
                _memberJoined.add(wrapper);
                joined = true;
            }

            if (_memberLeft.contains(wrapper)) {
                _memberLeft.add(wrapper);
                left = true;
            }

            if (!_cacheStopped.contains(wrapper)) {
                _cacheStopped.add(wrapper);
                stopped = true;
            }
        } else {
            if (registerAgainst.contains(CacheStatusNotificationType.MemberJoined) && !_memberJoined.contains(wrapper)) {
                _memberJoined.add(wrapper);
                joined = true;
            }

            if (registerAgainst.contains(CacheStatusNotificationType.MemberLeft) && !_memberLeft.contains(wrapper)) {
                _memberLeft.add(wrapper);
                left = true;
            }

            if (registerAgainst.contains(CacheStatusNotificationType.CacheStopped) && !_cacheStopped.contains(wrapper)) {
                _cacheStopped.add(wrapper);
                stopped = true;
            }
        }

        if (_memberJoined.size() == 1 && joined && registerAgainst.contains(CacheStatusNotificationType.MemberJoined)
                || registerAgainst.contains(CacheStatusNotificationType.ALL)) {
            registerNotification(NotificationType.REGISTER_MEMBER_JOINED, true);
            this.memberjoinedFlag = true;
        }

        if (_memberLeft.size() == 1 && left && registerAgainst.contains(CacheStatusNotificationType.MemberLeft)
                || registerAgainst.contains(CacheStatusNotificationType.ALL)) {
            registerNotification(NotificationType.REGISTER_MEMBER_LEFT, true);
            this.memberleftFlag = true;
        }

        if (_cacheStopped.size() == 1 && stopped && registerAgainst.contains(CacheStatusNotificationType.CacheStopped)
                || registerAgainst.contains(CacheStatusNotificationType.ALL)) {
            registerNotification(NotificationType.REGISTER_CACHE_STOPPED, true);
            this.cachestoppedFlag = true;
        }

    }

    @Override
    protected void unregisterCacheStatusEventlistener(CacheStatusEventListener listener, EnumSet<CacheStatusNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        if (unregisterAgainst.contains(CacheStatusNotificationType.ALL)) {
            removeIfCacheListenerExists(_memberJoined, listener);
            removeIfCacheListenerExists(_memberLeft, listener);
            removeIfCacheListenerExists(_cacheStopped, listener);
        } else {
            if (unregisterAgainst.contains(CacheStatusNotificationType.MemberJoined)) {
                removeIfCacheListenerExists(_memberJoined, listener);
            }

            if (unregisterAgainst.contains(CacheStatusNotificationType.MemberLeft)) {
                removeIfCacheListenerExists(_memberLeft, listener);
            }

            if (unregisterAgainst.contains(CacheStatusNotificationType.CacheStopped)) {
                removeIfCacheListenerExists(_cacheStopped, listener);
            }
        }

        if (_memberJoined.size() == 1 && unregisterAgainst.contains(CacheStatusNotificationType.MemberJoined)) {
            unregisterNotifications(NotificationType.REGISTER_MEMBER_JOINED);
            this.memberjoinedFlag = false;
        }

        if (_memberLeft.size() == 1 && unregisterAgainst.contains(CacheStatusNotificationType.MemberLeft)) {
            unregisterNotifications(NotificationType.REGISTER_MEMBER_LEFT);
            this.memberleftFlag = false;
        }

        if (_cacheStopped.size() == 1 && unregisterAgainst.contains(CacheStatusNotificationType.CacheStopped)) {
            unregisterNotifications(NotificationType.REGISTER_CACHE_STOPPED);
            this.cachestoppedFlag = false;
        }
    }

    @Override
    protected void registerCustomEventListener(CustomListener listener) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        if (listener != null) {
            CustomListenerWrapper wrapper = new CustomListenerWrapper(listener);
            if (!_customListener.contains(wrapper)) {
                _customListener.add(wrapper);

                if (_customListener.size() == 1) {
                    registerCustomNotification();
                }
            }
        }
    }

    @Override
    protected void unregisterCustomEventListener(CustomListener listener) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        if (listener != null) {
            removeIfCustomListenerExists(_customListener, listener);

            if (_customListener.size() == 0) {
                this.unregisterCustomNotification();
            }
        }
    }

    /**
     * Registers the Clear operation notification with the server.
     *
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws SecurityException Thrown when current user is not allowed to
     * perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    private void registerCustomNotification() throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        registerNotification(NotificationType.REGISTER_CUSTOM, true);
        this._customNotifRegistered = true;
    }

    /**
     * Registers the Clear operation notification with the server.
     *
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws SecurityException Thrown when current user is not allowed to
     * perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     */
    private void unregisterCustomNotification() throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {
        unregisterNotifications(NotificationType.UNREGISTER_CUSTOM);
        this._customNotifRegistered = false;
    }

    /**
     *
     * @param modifier
     */
    private void unregisterNotifications(int modifier)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException {

        Command command = new RegisterNotification(modifier, (short) -1);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();
        exceptionOccured(response);
    }

    /**
     * Re register notification to a new connection
     *
     * @param connection
     */
    private void reregisterGeneralNotifications(ConnectionManager connection) throws OperationFailedException, ConfigurationException, AggregateException,  GeneralFailureException, ConnectionException {

        if (_parent != null && _parent.getEventManager() != null) {
            EventRegistrationInfo[] generalEventRegistrations = _parent.getEventManager().getEventRegistrationInfo();
            //Re-registers general events with new server
            if (generalEventRegistrations != null) {
                for (int i = 0; i < generalEventRegistrations.length; i++) {
                    EventRegistrationInfo eventRegistration = generalEventRegistrations[i];
                    try {
                        registerNotifications(EnumSet.of(eventRegistration.getEventTYpe()), eventRegistration.getDataFilter(), eventRegistration.getRegistrationSequence(), connection);
                    } catch (Exception ex) {
                        Logger.getLogger(RemoteCache.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        if (this._customNotifRegistered) {
            registerNotification(connection, NotificationType.REGISTER_CUSTOM, true);
            this._customNotifRegistered = true;
        }

        registerNotification(connection, NotificationType.REGISTER_MEMBER_JOINED, true);
        registerNotification(connection, NotificationType.REGISTER_MEMBER_LEFT, true);
    }

    private void registerNotifications(EnumSet<EventType> eventType, EventDataFilter dataFilter, short sequenceNumber, ConnectionManager connection) throws Exception {
        NotificationsType notificationType;

        //System.Threading.Thread.MemoryBarrier();
        if (eventType.contains(EventType.ItemAdded)) {
            notificationType = NotificationsType.RegAddNotif;
        } else if (eventType.contains(EventType.ItemRemoved)) {
            notificationType = NotificationsType.RegRemoveNotif;
        } else if (eventType.contains(EventType.ItemUpdated)) {
            notificationType = NotificationsType.RegUpdateNotif;
        } else if (eventType.contains(EventType.CacheCleared)) {
            notificationType = NotificationsType.RegClearNotif;
        } else {
            return;
        }

        RegisterNotification notif = new RegisterNotification(notificationType.getValue(), dataFilter.getValue(), sequenceNumber);
        Request request = new Request(false, Broker._commandTimeout);
        Address ipAddress = connection.getServerAddress();
        request.addCommand(ipAddress, notif);

        try {
            Broker.executeRequest(request, connection, true, true);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }

    }

    /**
     *
     * @param connection
     */
    private void registerHashmapRecievedEvent(ConnectionManager connection) throws OperationFailedException,  ConfigurationException, GeneralFailureException, AggregateException, ConnectionException {
        registerNotification(connection, NotificationType.REGISTER_HASHMAP_RECIEVED, true);
    }

    //<editor-fold defaultstate="collapsed" desc=" ---- MapReduce ---- ">
    @Override
    public void executeMapReduceTask(MapReduceTask task, String taskId, MROutputOption outputOption, short callbackId) throws GeneralFailureException,  OperationFailedException, ConnectionException, AggregateException {
        long size = 0;
        RefObject<Long> tempSize = new RefObject<Long>(size);

        Object serializedTask = null;
        if (task.getMapper() != null) {
            serializedTask = SafeSerialize(task, this.getName(), new BitSet(), this, tempSize);
        }
        Command command = new MapReduceTaskCommand(serializedTask, taskId, outputOption, callbackId);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();
        if (response != null) {
            if (this.isExceptionsEnabled()) {
                exceptionOccured(response);
            }
        }
    }

    @Override
    public void registerMapReduceTaskCallback(short callbackId, String taskId) throws GeneralFailureException,  OperationFailedException, ConnectionException, AggregateException {
        Command command = new MapReduceTaskCallbackCommand(callbackId, taskId);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();
        if (response != null) {
            if (this.isExceptionsEnabled()) {
                exceptionOccured(response);
            }
        }
    }

    /**
     * Cancels a Running Task
     *
     * @param taskId
     */
    @Override
    public void cancelTask(String taskId) throws OperationFailedException {
        Command command = new TaskCancelCommand(taskId, false);
        try {
            cancelTasks(command);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
    }

    private void cancelTasks(Command command) throws GeneralFailureException,  OperationFailedException, ConnectionException, AggregateException {
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();
        if (response != null) {
            if (this.isExceptionsEnabled()) {
                exceptionOccured(response);
            }
        }
    }

    /**
     *
     * Flags that cancel All Tasks
     *
     * @throws GeneralFailureException
     * @throws SecurityException
     * @throws OperationFailedException
     * @throws ConnectionException
     * @throws AggregateException
     */
    @Override
    public void cancelAllTasks() throws GeneralFailureException,  OperationFailedException, ConnectionException, AggregateException {
        Command command = new TaskCancelCommand(null, true);
        cancelTasks(command);
    }

    /**
     *
     * Get all the Running Tasks.
     *
     * @throws GeneralFailureException
     * @throws OperationFailedException
     * @throws SecurityException
     * @throws ConnectionException
     * @throws AggregateException
     */
    @Override
    public java.util.ArrayList getRunningTasks() throws GeneralFailureException, OperationFailedException,  ConnectionException, AggregateException {
        java.util.ArrayList runningTasks = null;
        Command command = new GetRunningTasksCommand();
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();
        if (response != null) {
            runningTasks = new ArrayList(response.getRunningTasks());
            if (this.isExceptionsEnabled()) {
                exceptionOccured(response);
            }
        }
        return runningTasks;
    }

    /**
     *
     * @param taskId
     * @return 
     * @throws GeneralFailureException
     * @throws OperationFailedException
     * @throws SecurityException
     * @throws ConnectionException
     * @throws AggregateException
     */
    @Override
    public TaskStatus getTaskProgress(String taskId) throws GeneralFailureException, OperationFailedException,  ConnectionException, AggregateException {
        TaskStatus progress = null;
        Command command = new TaskProgressCommand(taskId);
        Request request = Broker.createRequest(command);
        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        CommandResponse response = request.getResponse();
        if (response != null) {
            progress = response.getTaskProgress();
            if (this.isExceptionsEnabled()) {
                exceptionOccured(response);
            }
        }
        return progress;
    }

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~ C u s t o m E v e n t   L i s t e n e r ~~~~~~~~~~~~~~~~~~~~~~~~~">
    /**
     * Notifies the Member Joined Event to all the listeners registered with the
     * cache.
     *
     * @param id
     * @see CacheEvent
     * @see CacheListener
     * @see CacheAdapter
     */
    private void notifyMemberJoinedEvent(String ip, int port) {
        // Makeup a EventObject
        if (!memberjoinedFlag) {
            return;
        }
        final ClusterEvent event = new ClusterEvent(this, ClusterEvent.EventType.JOINED, ip, port, this.cacheId);

        int length = _memberJoined.size();
        for (int i = 0; i < length; i++) {
            final CacheStatusEventListener listener = _memberJoined.get(i).getClusterEvent();
            com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                public void run() {
                    listener.memberJoined(event);
                }
            });
        }
    }

    /**
     * Notifies the Member Left Event to all the listeners registered with the
     * cache.
     *
     * @param id
     * @see CacheEvent
     * @see CacheListener
     * @see CacheAdapter
     */
    private void notifyMemberLeftEvent(String ip, int port) {
        // Makeup a EventObject
        if (!memberleftFlag) {
            return;
        }
        final ClusterEvent event = new ClusterEvent(this, ClusterEvent.EventType.LEFT, ip, port, this.cacheId);

        int length = _memberLeft.size();
        for (int i = 0; i < length; i++) {
            final CacheStatusEventListener listener = _memberLeft.get(i).getClusterEvent();
            com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                public void run() {
                    listener.memberLeft(event);
                }
            });

        }
    }

    /**
     * Notifies the Cache Stopped Event to all the listeners registered with the
     * cache.
     *
     * @param id
     * @see CacheEvent
     * @see CacheListener
     * @see CacheAdapter
     */
    private void notifyCacheStoppedEvent(String cacheId) {
        // Makeup a EventObject
        if (!cachestoppedFlag) {
            return;
        }
        final ClusterEvent event = new ClusterEvent(this, ClusterEvent.EventType.STOPPED, cacheId);

        int length = _cacheStopped.size();
        for (int i = 0; i < length; i++) {
            final CacheStatusEventListener listener = _cacheStopped.get(i).getClusterEvent();
            com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                public void run() {
                    listener.cacheStopped(event);
                }
            });

        }
    }

    /**
     * Notifies the Add Event to all the listeners registered with the cache.
     *
     * @param id
     * @param key The cache key used to reference the item.
     *
     * @see CacheEvent
     * @see CacheListener
     * @see CacheAdapter
     */
    private void notifyAsyncAddEvent(int id, final Object key, final Object value) {
        if (id > -1) {
            final Object callback = callbackQue.get(id);
            if (callback != null) {
                final AsyncItemAddedCallback item = ((AsyncItemAddedCallback) callback);
                com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                    public void run() {
                        item.asyncItemAdded(key, value);
                    }
                });

            }
        }
    }

    /**
     * Notifies the Update Event to all the listeners registered with the cache.
     *
     * @param id
     * @param key The cache key used to reference the item.
     *
     * @see CacheEvent
     * @see CacheListener
     * @see CacheAdapter
     */
    private void notifyAsyncUpdateEvent(int id, final Object key, final Object value) {
        if (id > -1) {

            final Object callback = callbackQue.get(id);
            if (callback != null) {
                final AsyncItemUpdatedCallback item = ((AsyncItemUpdatedCallback) callback);
                com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                    public void run() {
                        item.asyncItemUpdated(key, value);
                    }
                });

            }
        }
    }

    /**
     * Notifies the Remove Event to all the listeners registered with the cache.
     *
     * @param id
     * @param key The cache key used to reference the item.
     *
     * @param value
     * @param reason
     * @see CacheEvent
     * @see CacheListener
     * @see CacheAdapter
     */
    private void notifyAsyncRemoveEvent(int id, final Object key, final Object value) {
        if (id > -1) {
            Object callback = callbackQue.get(id);
            if (callback != null) {
                final AsyncItemRemovedCallback item = ((AsyncItemRemovedCallback) callback);
                com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                    public void run() {
                        item.asyncItemRemoved(key, value);
                    }
                });

            }
        }
    }

    /**
     * Notifies the Clear Event to all the listeners registered with the cache.
     *
     * @param id
     * @see CacheEvent
     * @see CacheListener
     * @see CacheAdapter
     */
    private void notifyAsyncClearEvent(int id, final Object value) {

        if (id > -1) {
            Object callback = callbackQue.get(id);
            if (callback != null) {
                final AsyncCacheClearedCallback item = ((AsyncCacheClearedCallback) callback);
                com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                    public void run() {
                        item.asyncCacheCleared(value);
                    }
                });

            }
        }
    }

    private void notifyDSUpdated(int id, final Map value, OpCode operationCode) {

        if (id > -1 && callbackQue.size() > 0) {
            Object callback = callbackQue.get(id);
            if (callback != null) {
                switch (operationCode) {
                    case Add: {
                        final DataSourceItemsAddedCallback item = ((DataSourceItemsAddedCallback) callback);
                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                            public void run() {
                                item.dataSourceItemAdded(value);
                            }
                        });

                    }
                    break;
                    case Update: {
                        final DataSourceItemsUpdatedCallback item = ((DataSourceItemsUpdatedCallback) callback);
                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                            public void run() {
                                item.dataSourceItemUpdated(value);
                            }
                        });

                    }
                    break;
                    case Remove: {
                        final DataSourceItemsRemovedCallback item = ((DataSourceItemsRemovedCallback) callback);

                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                            public void run() {
                                item.dataSourceItemRemoved(value);
                            }
                        });

                    }
                    break;
                    case Clear: {
                        Iterator enumeration = value.values().iterator();
                        while (enumeration.hasNext()) {

                            final Object param = enumeration.next();
                            final DataSourceClearedCallback item = ((DataSourceClearedCallback) callback);
                            com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                                public void run() {
                                    item.dataSourceCleared(param);
                                }
                            });
                        }
                    }
                    break;
                }

            }
        }
    }

    private void notifyCustomEvent(byte[] notifId, byte[] value) {
        //<editor-fold defaultstate="collapsed" desc="notifId deserialization">

        byte[] notifIdBytes = notifId;
        Object key = null;

        try {
            key = this.getDeserializedObject(notifId, null);
        } catch (Exception e) {
            key = notifId;
        }

        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="value deserialization">
        byte[] valueBytes = value;
        Object obj = null;
        try {
            obj = this.getDeserializedObject(value, null);
        } catch (Exception e) {
            obj = value;
        }
        //</editor-fold>

        final CustomEvent event = new CustomEvent(this, CustomEvent.EventType.CUSTOM, key, obj);

        int length = _customListener.size();
        for (int i = 0; i < length; i++) {
            final CustomListener listener = _customListener.get(i).getCustomEvent();
            com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(new Runnable() {
                public void run() {
                    listener.customEventOccured(event);
                }
            });
        }
    }
    //</editor-fold>
    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Enumerator Methods & Classes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ">
    /**
     *
     * @return
     */
    // Types of Enumerations/Iterations
    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;

    /**
     *
     * @param <K>
     * @param <V>
     */
    public class Entry<K, V> implements Map.Entry<K, V> {

        /**
         *
         */
        K key;
        /**
         *
         */
        V value;

        /**
         *
         * @param key
         * @param value
         */
        protected Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        /**
         *
         */
        protected Entry() {
        }

        /**
         *
         * @return
         */
        @Override
        protected Object clone() {
            return new Entry<K, V>(/*hash,*/key, value/*,
             (next==null ? null : (Entry<K,V>) next.clone())*/);
        }

        /**
         *
         * @return
         */
        public K getKey() {
            return key;
        }

        /**
         *
         * @return
         */
        public V getValue() {
            return value;
        }

        /**
         *
         * @param value
         * @return
         */
        public V setValue(V value) {
//            if(value == null) {
//                throw new NullPointerException();
//            }

            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        /**
         *
         * @param key
         */
        public void setKey(K key) {

            if (key == null) {
                throw new NullPointerException();
            }

            this.key = key;
        }

        /**
         *
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;

            return (key == null ? e.getKey() == null : key.equals(e.getKey()))
                    && (value == null ? e.getValue() == null : value.equals(e.getValue()));
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return key.toString() + "=" + (value != null ? value.toString() : "null");
        }
    }

    /**
     *
     * @param pointers
     * @return
     * @throws SecurityException
     * @throws GeneralFailureException
     */
    @Override
    public java.util.ArrayList<com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk> getNextChunk(java.util.ArrayList<com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer> pointers) throws  GeneralFailureException, OperationFailedException, AggregateException, ConnectionException, UnknownHostException {
        ArrayList<com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk> nextChunk = null;
        Request request;
        Command command = null;
        if (pointers.size() > 0 && pointers.get(0).getNodeIpAddress() != null)//&& !pointers.get(0).getNodeIpAddress().equals("")) 
        {
            if (Broker.importHashmap()) {
                request = new Request(true, Broker._commandTimeout);
            } else {
                request = new Request(false, Broker._commandTimeout);
            }

            for (int i = 0; i < pointers.size(); i++) {
                EnumerationPointer pointer = pointers.get(i);

                if (pointer.isGroupPointer()) {
                    GroupEnumerationPointer groupPointer = (GroupEnumerationPointer) pointer;
                    command = new GetGroupNextChunkCommand(groupPointer.getId(), groupPointer.getChunkId(), groupPointer.getGroup(), groupPointer.getSubGroup());
                } else {
                    command = new GetNextChunkCommand(pointer.getId(), pointer.getChunkId(), pointer.isDisposable());
                }

                request.addCommand(pointer.getNodeIpAddress(), command);
            }
        } else {
            if (!Broker.poolHasAllServers() || !Broker.importHashmap()) {
                EnumerationPointer pointer = pointers.get(0);
                if (pointer.isGroupPointer()) {
                    GroupEnumerationPointer groupPointer = (GroupEnumerationPointer) pointer;
                    command = new GetGroupNextChunkCommand(groupPointer.getId(), groupPointer.getChunkId(), groupPointer.getGroup(), groupPointer.getSubGroup());
                } else {
                    command = new GetNextChunkCommand(pointer.getId(), pointer.getChunkId(), pointer.isDisposable());
                }

                request = Broker.createRequest(command);
            } else {
                request = new Request(true, Broker._commandTimeout);
                EnumerationPointer pointer = pointers.get(0);
                for (int i = 0; i < Broker.getClientServerList().size(); i++) {
                    command = new GetNextChunkCommand(pointer.getId(), pointer.getChunkId(), pointer.isDisposable());
                    command.setClientLastViewId(-1);
                    RemoteServer server = (RemoteServer) Broker.getClientServerList().get(i);
                    command.setIntendedRecipient(server.getName());
                    request.addCommand(new Address(server.getName().toString(), server.getPort()), command);

                }
            }
        }

        try {
            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        CommandResponse response = request.getResponse();
        exceptionOccured(response);
        nextChunk = response.getNextChunk();

        return nextChunk;
    }

    /**
     *
     * @return
     */
    @Override
    public Enumeration getEnumerator() {

        return null;

    }

    // </editor-fold>
    //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Broker Class. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ">
    /**
     *
     * @version 1.0
     * 
     */
    //private [It was private previously. +Sami:20140203]
    public//</editor-fold> //end of B R O K E R
    //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ License Verification ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ">
    final class Broker implements CommandEvent {

        public RemoteCache _cache;

        private ConnectionManager _connection = null;
        private String _clientId = null;
        private long req_id = 0;
        private Object _requestIdMutex = new Object();
        private final Map responseQue = Collections.synchronizedMap(new HashMap(10000, 0.75f));
        private String nodeIp = "";
        private int newServerPort = 0;

        private String _cacheId = "";
        private String _serverId = "";
        private String _licenseCode = "";
        private String serverName = "";
        private int initialPort = 0;
        private int currentPort = 0;
        private int portRange = 0;
        private int priority = 0;
        private int _port = 0;

        private Object _connectionMutex = new Object();
        private final Object _hashmapUpdateMutex = new Object();
        private Date _retryConnectionStartTime = new Date();
        private boolean _retryConnection = true;
        private boolean _loggingEnabled = false;
        private ClientConfiguration _clientConfig = null;
        private int _connectionTimeout = 5;
        private int _connectionMutexTimeout = -1;
        private int _retryInterval = 1;
        private int _connectionRetries = 1;
        private int _commandTimeout = 90000;
        private int _retryConnectionDelay = 600000;
        private long _retryConnectionDelayInMinutes = 10;
        private ReentrantReadWriteLock _readWriteLock = new ReentrantReadWriteLock();
        private ReentrantReadWriteLock.WriteLock _lock = null;
        final int forcedViewId = -5;
        //_____________________________________\\
        private Map serverMap = null;
        private ConnectionPool _pool;
        private boolean _importHashmap = true;
        private Address _serverIP;//private String _serverIP;
        private boolean _balanceNodes;
        private ArrayBlockingQueue<Runnable> _notifQue = null;

        private Thread _worker = null;
        private NewHashmapRecivedTask _newMapTask = null;
        private HashMap requestTable = null;
        private final Object requestMutex = new Object();
        boolean _encryptionEnabled = false;
        private Logs _logger;
        public String SnmpAddress;
        AsyncProcessor _asyncProcessor;
        AsyncProcessor _eventProcessor = null;

        private int _asyncProcessorThreadCount = 1;

        private boolean _connectingFirstTime = true;
        private PersistenceManager _persistenceManager = null;
        private boolean _isPersistEnabled = false;
        private int _persistenceInterval;
      
        private boolean isCacheBind = false;
        private boolean _notifyAsync = true;
        private byte[] _value;

        private Hashtable<Address, ShutDownServerInfo> _shutdownServers = new Hashtable<Address, ShutDownServerInfo>();
        private long _shutdownTimeout = 180;

        public byte[] getValue() {
            return _value;
        }

        public void setValue(byte[] value) {
            if (value != null) {
                _value = value.clone();
            }
        }

        public final boolean getIsPersistenceEnabled() {
            return _isPersistEnabled;
        }

        public final int getPersistInterval() {
            return _persistenceInterval;
        }

        public Logs getLogger() {
            return _logger;
        }

        public final int getOperationTimeOut() {
            return _commandTimeout;
        }

        private void initializeLogs(boolean enableLogs, boolean enableDetailedLogs) throws Exception {
            Logs localLogger = new Logs();

            localLogger.setIsDetailedLogsEnabled(false);
            localLogger.setIsErrorLogsEnabled(false);
            try {
                if (enableLogs || enableDetailedLogs) {

                    localLogger.setIsDetailedLogsEnabled(enableDetailedLogs);
                    localLogger.setIsErrorLogsEnabled(enableLogs);

                    String pid = ManagementFactory.getRuntimeMXBean().getName();
                    localLogger.setNCacheLog(new JLogger());
                    localLogger.getCacheLog().Initialize(LoggerNames.ClientLogs, cacheId);
                    if (enableDetailedLogs) {
                        localLogger.getCacheLog().SetLevel("ALL");
                    } else {
                        localLogger.getCacheLog().SetLevel("INFO");
                    }

                } else {
                    if (_logger != null && _logger.getCacheLog() != null) {
                        _logger.getCacheLog().Flush();
                        _logger.getCacheLog().SetLevel("OFF");
                    }
                }
            } catch (Exception e) {
                localLogger.setIsDetailedLogsEnabled(false);
                localLogger.setIsErrorLogsEnabled(false);
            } finally {
                _logger = localLogger;
            }
        }
        int serverMissingEventCount;
        int missingEventCount;
        int duplicates;
        int peristentDuplicates;
        int eventsAlreadyArrived;
        private java.util.Hashtable eventsHistory = new java.util.Hashtable();
        private java.util.ArrayList<com.alachisoft.tayzgrid.caching.EventId> missingEvents = new java.util.ArrayList<com.alachisoft.tayzgrid.caching.EventId>();

        public final void SynchronizeEvents(ConnectionManager connection) throws OperationFailedException, Exception {
            try {
                com.alachisoft.tayzgrid.command.SyncEventsCommand command;

                java.util.ArrayList<com.alachisoft.tayzgrid.caching.EventId> evtids = _persistenceManager.GetPersistedEventsList();

                command = new com.alachisoft.tayzgrid.command.SyncEventsCommand(evtids);
                Request request = Broker.createRequest(command);
                try {
                    Broker.executeRequest(request);
                } catch (Exception ex) {
                    throw new OperationFailedException(ex.getMessage());
                }
                CommandResponse res = request.getResponse();
                if (res != null) {
                    res.parseResponse(cacheId);
                }

                com.alachisoft.tayzgrid.caching.EventId evtId;
                byte[] objectValue = null;
                CacheItemRemovedReason reason;
                serverMissingEventCount = res.getProtobufResponse().getSyncEventsResponse().getEventInfoCount();

                for (int i = 0; i <= serverMissingEventCount; i++) {
                    try {
                        com.alachisoft.tayzgrid.common.protobuf.EventInfoProtocol.EventInfo evtInfo = res.getProtobufResponse().getSyncEventsResponse().getEventInfo(i);
                        Object key = CacheKeyUtil.SafeDeserialize(evtInfo.getKey(), cacheId);
                        evtId = new com.alachisoft.tayzgrid.caching.EventId(evtInfo.getEventId().getEventUniqueId(), evtInfo.getEventId().getOperationCounter(), evtInfo.getEventId().getEventCounter());
                        evtId.setEventType(ConvertEventType(evtInfo.getEventType()));
                        evtId.setQueryChangeType(com.alachisoft.tayzgrid.caching.queries.QueryChangeType.forValue(evtInfo.getChangeType()));
                        evtId.setQueryId(evtInfo.getQueryId());

                        if (_persistenceManager.PersistEvent(evtId)) //if not in event store
                        {
                            missingEvents.add(evtId);
                            missingEventCount++;

                            switch (evtInfo.getEventType()) {
                                case ITEM_REMOVED_CALLBACK:
                                    Object objRemoveCallback = callbackQue.get(evtInfo.getCallbackId());
                                    switch (evtInfo.getItemRemoveReason()) {
                                      
                                        case 1:
                                            reason = CacheItemRemovedReason.Expired;
                                            break;

                                        case 2:
                                            reason = CacheItemRemovedReason.Removed;
                                            break;

                                        default:
                                            reason = CacheItemRemovedReason.Underused;
                                            break;

                                    }
                                    if (objRemoveCallback instanceof Notifications) {
                                        Notifications notifiRemove = (Notifications) callbackQue.get(evtInfo.getCallbackId());
                                        ExecutorClass execRemove = new ExecutorClass(key, notifiRemove, 2);
                                        execRemove.setCacheEvent(new CacheEvent(this, CacheEvent.EventType.REMOVED, key, evtInfo, reason));
                                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(execRemove);
                                    } else {

                                    }
                                    break;

                                case ITEM_UPDATED_CALLBACK:
                                    Object objUpdateCallBack = callbackQue.get(evtInfo.getCallbackId());

                                    if (objUpdateCallBack instanceof Notifications) {
                                        Notifications notifi = (Notifications) callbackQue.get(evtInfo.getCallbackId());
                                        ExecutorClass exec = new ExecutorClass(key, notifi, 1);
                                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(exec);
                                    }
                                    break;

                                case CACHE_CLEARED_EVENT:
                                    for (int j = 0; j < _cacheCleared.size(); j++) {
                                        CacheListener notifiClear = (CacheListener) _cacheCleared.get(i).getCacheEvent();
                                        ExecutorClass execRemove = new ExecutorClass(key, notifiClear, 3);
                                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(execRemove);
                                    }
                                    break;

                                case ITEM_ADDED_EVENT:

                                    switch (evtInfo.getItemRemoveReason()) {
                                      
                                        case 1:
                                            reason = CacheItemRemovedReason.Expired;
                                            break;

                                        case 2:
                                            reason = CacheItemRemovedReason.Removed;
                                            break;

                                        default:
                                            reason = CacheItemRemovedReason.Underused;
                                            break;

                                    }

                                    for (int k = 0; k < _cacheItemAdded.size(); k++) {
                                        CacheListener notifiClear = (CacheListener) _cacheItemAdded.get(i).getCacheEvent();
                                        ExecutorClass execRemove = new ExecutorClass(key, notifiClear, 4);
                                        execRemove.setCacheEvent(new CacheEvent(this, CacheEvent.EventType.ADDED, key, null, reason));
                                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(execRemove);
                                    }

                                    break;

                                case ITEM_UPDATED_EVENT:
                                    for (int l = 0; l < _cacheItemUpdated.size(); l++) {
                                        CacheListener notifiUpdate = (CacheListener) _cacheItemUpdated.get(i).getCacheEvent();
                                        ExecutorClass execRemove = new ExecutorClass(key, notifiUpdate, 6);
                                        execRemove.setCacheEvent(new CacheEvent(this, CacheEvent.EventType.UPDATED, key, null));
                                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(execRemove);
                                    }
                                    break;

                                case ITEM_REMOVED_EVENT:
                                    switch (evtInfo.getItemRemoveReason()) {
                                       
                                        case 1:
                                            reason = CacheItemRemovedReason.Expired;
                                            break;

                                        case 2:
                                            reason = CacheItemRemovedReason.Removed;
                                            break;

                                        default:
                                            reason = CacheItemRemovedReason.Underused;
                                            break;

                                    }

                                    for (int m = 0; m < _cacheItemRemoved.size(); m++) {
                                        CacheListener notifi = (CacheListener) _cacheItemRemoved.get(i).getCacheEvent();
                                        ExecutorClass execRemove = new ExecutorClass(key, notifi, 5);
                                        execRemove.setCacheEvent(new CacheEvent(this, CacheEvent.EventType.REMOVED, key, null, reason));
                                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(execRemove);
                                    }
                                    break;

                            }
                        }

                    } catch (RuntimeException ex) {
                        if (_logger.getIsErrorLogsEnabled()) {
                            _logger.getCacheLog().Error("Broker.SynchronizeEvents", ex.getMessage());
                        }

                    }
                }
            } catch (RuntimeException ex) {
                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.SynchronizeEvents", ex.getMessage());
                }
            } finally {

            }

        }

        private com.alachisoft.tayzgrid.persistence.EventType ConvertEventType(com.alachisoft.tayzgrid.common.protobuf.EventInfoProtocol.EventInfo.EventType protoEventType) {
            switch (protoEventType) {
                case CACHE_CLEARED_EVENT:
                    return com.alachisoft.tayzgrid.persistence.EventType.CACHE_CLEARED_EVENT;

                case ITEM_ADDED_EVENT:
                    return com.alachisoft.tayzgrid.persistence.EventType.ITEM_ADDED_EVENT;

                case ITEM_REMOVED_CALLBACK:
                    return com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_CALLBACK;

                case ITEM_REMOVED_EVENT:
                    return com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_EVENT;

                case ITEM_UPDATED_CALLBACK:
                    return com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_CALLBACK;

                case ITEM_UPDATED_EVENT:
                    return com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_EVENT;

            }
            return com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_EVENT;
        }

        // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~NewHashmapRecivedTask~~~~~~~~~~~~~~~~~~~~">
        private final class NewHashmapRecivedTask implements Runnable {

            Broker _parent = null;
            private final ArrayList<NewHashMap> _queue = new ArrayList<NewHashMap>(2);
            private boolean _exiting = false;

            public NewHashmapRecivedTask(Broker parent) {
                this._parent = parent;
            }

            public void queue(NewHashMap map) {
                synchronized (this._queue) {
                    this._queue.add(map);
                    this._queue.notify();
                }
            }

            @Override
            protected void finalize() throws Throwable {
                this.destroy();
            }

            public void destroy() {
                this._exiting = true;
                synchronized (this._queue) {
                    this._queue.clear();
                    this._queue.notify();
                }
            }

            public void run() {

                while (true) {
                    synchronized (this._queue) {
                        try {
                            this._queue.wait();
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }

                    if (this._exiting) {
                        break;
                    }

                    synchronized (this._queue) {
                        if (!this._queue.isEmpty()) {
                            NewHashMap map = this._queue.get(this._queue.size() - 1);
                            this._parent.newHashmapRecieved(map);

                            this._queue.clear();
                        }
                    }
                }
            }
        }
        // </editor-fold>

        Broker(RemoteCache cache, String cacheId, CacheInitParams initParams) throws ConfigurationException, GeneralFailureException {

            _cache = cache;

            if (initParams != null) {
                this._clientConfig = new ClientConfiguration(cacheId, initParams);
            } else {
                this._clientConfig = new ClientConfiguration(cacheId);
            }

            this._cacheId = cacheId;
            this._licenseCode = "LicenceCode";
            this._balanceNodes = _clientConfig.getBalanceNodes();
            this._importHashmap = _clientConfig.getImportHashmap();
            this._commandTimeout = _clientConfig.getTimeout() * 1000;
            this._connectionTimeout = _clientConfig.getConnectionTimeout();
            this._connectionRetries = _clientConfig.getConnectionRetries();
            this._retryInterval = _clientConfig.getRetryInterval();
            this._retryConnectionDelay = _clientConfig.getRetryConnectionDelay();
            this._retryConnectionDelayInMinutes = _retryConnectionDelay / 60000; //Converting to minutes from milliseconds;
          

            _lock = _readWriteLock.writeLock();

            this._notifQue = new ArrayBlockingQueue<Runnable>(100);

            this._pool = new ConnectionPool();

            this.requestTable = new HashMap(10000, 0.75f);
        }

        //function to return the initial server and port to connect with
        public final RemoteServer GetInitialServer() {
            RemoteServer serverInfo = new RemoteServer();
            if (_clientConfig.initParam.getServerListSize() > 0) {

                CacheServerInfo[] info = _clientConfig.initParam.getServerList();
                serverInfo = info[0].getServerInfo();
                return serverInfo;

            } else if (_clientConfig.initParam.getServer() != null) {
                serverInfo = new RemoteServer(_clientConfig.initParam.getServer(), _clientConfig.initParam.getPort());
                return serverInfo;
            }
            return serverInfo;

        }

        //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Broker Service Operations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ">
        private void startServices(String cacheId, String server, int port, CacheInitParams initParams) throws CacheException, Exception {
            String EnableLogs = System.getProperty("ENABLE_CLIENT_LOGS");
            String EnableDetailedLogs = System.getProperty("ENABLE_DETAILED_CLIENT_LOGS");
            String BufferSize = System.getProperty("BufferSize");
            boolean enableLogs = false;
            boolean enableDetailedLogs = false;

            if (EnableLogs != null && !EnableLogs.trim().isEmpty()) {
                enableLogs = Boolean.parseBoolean(EnableLogs);
            }

            if (EnableDetailedLogs != null && !EnableDetailedLogs.trim().isEmpty()) {
                enableDetailedLogs = Boolean.parseBoolean(EnableDetailedLogs);
            }

            try {
                initializeLogs(enableLogs, enableDetailedLogs);
            } catch (Exception exception) {
                System.out.println("Unable to initilization logger due to :" + exception.getMessage());
            }

            this._cacheId = cacheId;
            this._serverId = server;
            this._port = port;

            this._newMapTask = new NewHashmapRecivedTask(this);
            this._worker = new Thread(this._newMapTask);
            this._worker.setName("Broker worker thread");
            try {
                this._worker.setPriority(3);
            } catch (IllegalArgumentException ex) {
            }
            try {
                this._worker.start();
            } catch (IllegalThreadStateException ex) {
            }

            int conTimeout = _connectionRetries * (_connectionTimeout + _retryInterval);
            if (conTimeout > 0) {
                _connectionMutexTimeout = conTimeout;
            }
            if (_commandTimeout < 60000) {
                _commandTimeout = 60000; //minimum timeout is 60 seconds.
            }
            _connection = new ConnectionManager(this, _logger, _clientConfig.getBindIP());
            RemoteServer remoteServer = new RemoteServer(server, port);
            if (remoteServer.getName() != null) {
                remoteServer.setUserProvided(true);
                _clientConfig.addServer(remoteServer);
                try {
                    this.connectRemoteServer(this._connection, remoteServer, true);
                } catch (Exception se) {

                    if (_logger.getIsErrorLogsEnabled()) {
                        _logger.getCacheLog().Error("Broker.StartServices", se.toString());
                    }
                }
            }
            if (initParams.getLoadBalance()) {
                this._balanceNodes = initParams.getLoadBalance();
            } else {
                this._balanceNodes = remoteServer.getNodeBalance();
            }

            if (!isConnected()) {
                try {
                    tryNextServer();
                } catch (Exception se) {

                    if (_logger.getIsErrorLogsEnabled()) {
                        _logger.getCacheLog().Error("Broker.StartServices", se.toString());
                    }
                    throw se;
                }
            }

            if (!_notifyAsync) {
                /*
                 * This segment will specify _asyncProcessor setting from user configuration file
                 */

                if (_asyncProcessorThreadCount <= 0) {
                    _asyncProcessorThreadCount = 1;
                }

                if (_asyncProcessorThreadCount > 5) {
                    _asyncProcessorThreadCount = 5;
                }

                _eventProcessor = new AsyncProcessor(_asyncProcessorThreadCount);
                _eventProcessor.Start();

            }

            _asyncProcessor = new AsyncProcessor(_asyncProcessorThreadCount);
            _asyncProcessor.Start();

        }

        public void stopServices() {

            if (_logger.getIsErrorLogsEnabled()) {
                _logger.getCacheLog().Error("Broker.StopService", "stopping services...");
            }
            HashMap connections = this._pool.cloneConnectionPool();
            Iterator<ConnectionManager> enu = connections.values().iterator();
            while (enu.hasNext()) {
                ConnectionManager conn = enu.next();
                this._pool.remove(conn.getServerAddress());
                conn.disconnect();
            }

            _connection.disconnect();

            this._newMapTask.destroy();
            try {
                if (this._worker != null && !this._worker.isInterrupted() && this._worker.isAlive()) {
                    this._worker.join();
                }
            } catch (InterruptedException ex) {
            }
        }

        private void resetBroker(Address ip) {
            Iterator iter = null;

            synchronized (requestMutex) {
                iter = requestTable.values().iterator();
                while (iter.hasNext()) {
                    Request request = (Request) iter.next();
                    if (request.expectingResponseFrom(ip)) {
                        synchronized (request) {
                            request.reset(ip);
//                        pulsableRequests.add(request);
                            Monitor.pulse(request);
                            //request.notify();
                        }
                    }
                }

            }
        }
        //</editor-fold>

        public void Write(String str) {

        }

        //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Responce Received and Processing ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ">
        /**
         * event handler for the Receive CommandBase Event. gets the command
         * from the _requestTable and if this command was async then this thread
         * goes on to notify the initiator of this request. if the command was
         * synchronous then the thread which is waiting on this command object
         * is notified and command object is Remove from the _requestTable.
         *
         * @param result CommandResponse indicating the response/command
         * received from the server.
         */
        public void commandReceived(CommandResponse result, Address serverAddress) {
            try {
                processResponse(result, serverAddress);
            } catch (OperationFailedException ex) {
                if (_logger!=null&&_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.commandReceived", ex.toString());
                }

            } catch (IOException ex) {
                if (_logger!=null&&_logger.getIsErrorLogsEnabled()) {
                _logger.getCacheLog().Error("Broker.commandReceived", ex.toString());
                }
            } catch (ClassNotFoundException ex) {
                if (_logger!=null&&_logger.getIsErrorLogsEnabled()) {
                _logger.getCacheLog().Error("Broker.commandReceived", ex.toString());
                }
            }
        }

        private void processResponse(CommandResponse response, Address remoteServerAddress) throws OperationFailedException, IOException, ClassNotFoundException {
            response.parseResponse(cacheId);

            Command command = null;

            Request request = null;

            synchronized (requestMutex) {
                request = (Request) requestTable.get(response.getRequestId());
                if (request != null) {
                    if (request.getCommands().size() > 0) {
                        command = (Command) request.getCommands().get(remoteServerAddress);
                    }
                }
            }

            response.setCacheId(_cacheId);

            switch (response.getType()) {
                case SEARCH:
                case SEARCH_ENTRIES:
                case ADD:
                case REMOVE_BULK:
                case GET_BULK:
                case INSERT_BULK:
                case ADD_BULK:
                case CONTAINS:
                case COUNT:
                case CLEAR:
                case EXCEPTION:
                case GET:
                case GET_CACHE_ITEM:
                case GET_GROUP_DATA:
                case GET_GROUP_KEYS:
                case GET_ENUMERATOR:
                case GET_TYPEINFO_MAP:
                case GET_TAG:
                case INSERT:
                case INIT:
                case RAISE_CUSTOM_EVENT:
                case REMOVE:
                case REMOVE_GROUP:
                case REGISTER_NOTIF:
                case REGISTER_KEY_NOTIF:
                case REGISTER_BULK_KEY_NOTIF:
                case UNREGISTER_BULK_KEY_NOTIF:
                case UNREGISTER_KEY_NOTIF:
                case GET_OPTIMAL_SERVER:
                case UNLOCK:
                case LOCK:
                case ISLOCKED:
                case GET_HASHMAP:
                case REMOVE_TAG:
                case DELETE:
                case DELETE_BULK:
                case GET_KEYS_TAG:
                case GET_GROUP_NEXT_CHUNK:
                case GET_NEXT_CHUNK:
                case ADD_ATTRIBUTE:
                 case SYNC_EVENTS:
                case DELETE_QUERY:
                case REMOVE_QUERY:
                case MAP_REDUCE_TASK:
                case MAP_REDUCE_TASK_CANCEL:
                case GET_RUNNING_TASKS:
                case TASK_PROGRESS:
                case GET_TASK_ENUMERATOR:
                case GET_NEXT_RECORD:
                case INVOKE_ENTRYPROCESSOR:
                case GET_CACHE_CONFIG:
                case MAP_REDUCE_TASK_CALLBACK:
                case DISPOSE:
                    if (request == null) {
                        return;
                    }
                    synchronized (request) {
                        request.addResponse(remoteServerAddress, response);
                        Monitor.pulse(request);
                        //request.notify();
                    }
                    if (_logger.getIsDetailedLogsEnabled()) {
                        _logger.getCacheLog().Debug("Broker.SendCommand:" + "RequestID :" + request.getRequestId() + " " + request.getName()
                                + " received respose from server. Seq # "
                                + response.getSequenceId());
                    }
                    break;
                case ITEM_UPDATED_CALLBACK:

                    if (_persistenceManager != null && !_persistenceManager.PersistEvent(response.getEventId())) {
                        return;
                    }

                    if (_cache != null && _cache.getAsyncEventsListener() != null) {
                        _cache.getEventsListener().OnCustomUpdateCallback(response.getCallbackId(), response.getKey(), true, null, null, null, response.getDataFilter());
                    }
                    break;

                case ITEM_REMOVED_CALLBACK:
                    if (_persistenceManager != null && !_persistenceManager.PersistEvent(response.getEventId())) {
                        return;
                    }

                    if (_cache != null && _cache.getAsyncEventsListener() != null) {
                        _cache.getEventsListener().OnCustomRemoveCallback(response.getCallbackId(), response.getKey(), response.getValue(), response.getReason(), response.getFlagValueEntry().Flag, true, null, response.getDataFilter());
                    }

                    break;
                case ASYNC_OP_COMPLETED_CALLBACK:

                    Object asyncOpResult = null;
                    if (response.getAsyncOpResult() instanceof Integer) {
                        asyncOpResult = AsyncOpResult.forValue((Integer) response.getAsyncOpResult() - 1);
                    } else {
                        asyncOpResult = response.getAsyncOpResult();
                    }

                    if (command instanceof AddCommand) {
                        notifyAsyncAddEvent(command.getAsyncCallbackId(), command.getKey(), asyncOpResult);
                    } else if (command instanceof InsertCommand) {
                        notifyAsyncUpdateEvent(command.getAsyncCallbackId(), command.getKey(), asyncOpResult);
                    } else if (command instanceof RemoveCommand) {
                        notifyAsyncRemoveEvent(command.getAsyncCallbackId(), command.getKey(), asyncOpResult);
                    } else if (command instanceof ClearCommand) {
                        notifyAsyncClearEvent(command.getAsyncCallbackId(), asyncOpResult);
                    }
//                    } else if (command instanceof BulkGetCommand) {
//                        notifyJCacheLoaderEvent(command.getAsyncCallbackId(), asyncOpResult);
//                    }
                    if (request != null) {
                        synchronized (requestMutex) {
                            requestTable.remove(request.getRequestId());
                            if (_perfStatsCollector != null) {
                                _perfStatsCollector.decrementRequestQueueSize();
                            }
                        }
                    }
                    break;
                case ITEM_ADDED_EVENT:
                    if (_persistenceManager != null && !_persistenceManager.PersistEvent(response.getEventId())) {
                        peristentDuplicates++;
                        return;
                    }

                    _cache.getEventsListener().OnItemAdded(response.getKey(), true, null, null);

                    break;
                case ITEM_UPDATED_EVENT:
                    if (_persistenceManager != null && !_persistenceManager.PersistEvent(response.getEventId())) {
                        peristentDuplicates++;
                        return;

                    }

                    _cache.getEventsListener().OnItemUpdated(response.getKey(), true, null, null, null);

                    break;
                case ITEM_REMOVED_EVENT:
                    if (_persistenceManager != null && !_persistenceManager.PersistEvent(response.getEventId())) {
                        peristentDuplicates++;
                        return;
                    }

                    _cache.getEventsListener().OnItemRemoved(response.getKey(), response.getValue(), response.getReason(), response.getFlagValueEntry().Flag, true, null);

                    break;
                case CACHE_CLEARED_EVENT:
                    if (_persistenceManager != null && !_persistenceManager.PersistEvent(response.getEventId())) {
                        return;

                    }
                    for (int i = 0; i < _cacheCleared.size(); i++) {
                        CacheListener notifiClear = (CacheListener) _cacheCleared.get(i).getCacheEvent();
                        ExecutorClass execRemove = new ExecutorClass(response.getKey(), notifiClear, 3);
                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(execRemove);
                    }
                    break;
                case CUSTOM_EVENT:

                    _cache.getEventsListener().OnCustomNotification(CompactBinaryFormatter.fromByteBuffer(response.getNotifId(), cacheId),
                            CompactBinaryFormatter.fromByteBuffer(response.getValue(), cacheId), true);
                    break;
                case NODE_LEFT_EVENT:
                    RemoteServer serverLeft = new RemoteServer();
                    if (response.getPort() > 0) {
                        serverLeft = _clientConfig.getMappedServer(response.getIp().toString(), response.getPort());// new RemoteServer(response.getIp(), response.getPort());
                        _clientConfig.RemoveServer(serverLeft);
                    }
                    notifyMemberLeftEvent(serverLeft.getName(), serverLeft.getPort());

                    break;
                case NODE_JOINED_EVENT:
                    RemoteServer newServerJoined = new RemoteServer();
                    if (response.getPort() > 0) {
                        newServerJoined = _clientConfig.getMappedServer(response.getIp(), response.getPort());//new RemoteServer(response.getIp(), response.getPort());
                        _clientConfig.addServer(newServerJoined);
                    }
                    nodeIp = newServerJoined.getName();//response.getIp();
                    newServerPort = newServerJoined.getPort();//response.getPort();
                    notifyMemberJoinedEvent(newServerJoined.getName(), newServerJoined.getPort());

                    if (response.reconnectClients() && _clientConfig.getBalanceNodes()) {
                        BalanceNodeExecutor balanceNodeExec = new BalanceNodeExecutor();
                        com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(balanceNodeExec);
                    }
                    break;
                case CACHE_STOPPED_EVENT:
                    notifyCacheStoppedEvent(response.getCacheId());
                    break;
                case HASHMAP_CHANGED_EVENT:
                    UpdatePoolExecutor updatePoolExec = new UpdatePoolExecutor(response, true);
                    com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(updatePoolExec);
                    break;

                case DS_UPDATE_CALLBACK:
                    notifyDSUpdated((int) response.getCallbackId(), response.getHashMap(), response.getDSOperationCode());
                    break;

                case BULK_EVENT:
                    List<BulkEventItemResponse> responseList = response.getEventList();
                    if (responseList != null && !responseList.isEmpty()) {

                        BulkEventStructure bulkEventStructure = new BulkEventStructure();
                        bulkEventStructure._perfStatsCollector = _perfStatsCollector;
                        bulkEventStructure._persistenceManager = _persistenceManager;
                        bulkEventStructure.bulkEventList = responseList;
                        bulkEventStructure.parent = Broker;
                        bulkEventStructure.remoteServerAddress = remoteServerAddress;

                        ThreadPool.executeTask(bulkEventStructure);
                    }

                    break;

                case BLOCK_ACTIVITY:

                    ShutDownServerInfo ssinfo = new ShutDownServerInfo();
                    ssinfo.setUniqueBlockingId(response.getProtobufResponse().getBlockActivityEvent().getUniqueKey());
                    ssinfo.setBlockServerAddress(new Address(response.getProtobufResponse().getBlockActivityEvent().getServerIP(), response.getProtobufResponse().getBlockActivityEvent().getPort()));
                    ssinfo.setBlockInterval(response.getProtobufResponse().getBlockActivityEvent().getTimeoutInterval());

                    ssinfo.setStartBlockingTime((Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l);

                    if (!_shutdownServers.contains(ssinfo.getBlockServerAddress())) {
                        _shutdownServers.put(ssinfo.getBlockServerAddress(), ssinfo);

                        long maxTimeout = 0;

                        for (ShutDownServerInfo sInfo : _shutdownServers.values()) {
                            if (maxTimeout == 0) {
                                maxTimeout = sInfo.getBlockInterval();
                            }
                            if (maxTimeout < sInfo.getBlockInterval()) {
                                maxTimeout = sInfo.getBlockInterval();
                            }
                        }

                        double additionaltime = maxTimeout * 0.05f;
                        maxTimeout = (maxTimeout + (int) additionaltime) * 1000;
                        _shutdownTimeout = maxTimeout;
                        try {
                            java.util.Iterator iter = null;
                            synchronized (requestMutex) {
                                iter = requestTable.values().iterator();
                                while (iter.hasNext()) {
                                    Request req = (Request) iter.next();
                                    synchronized (req) {
                                        req.setRequestTimeout(req.getRequestTimeout() + maxTimeout);
                                        req.setIsRequestTimeoutReset(true);

                                    }
                                }
                            }
                        } catch (Exception ex) {
                            if(_logger.getIsErrorLogsEnabled())
                            {
                                _logger.getCacheLog().Error("Broker.ProcessResponse", ex.toString());
                            }
                        }
                    } else {
                        ShutDownServerInfo oldInfo = (ShutDownServerInfo) _shutdownServers.get(ssinfo.getBlockServerAddress());
                        if (!oldInfo.getUniqueBlockingId().equals(ssinfo.getUniqueBlockingId())) {

                            long startTime = oldInfo.getStartBlockingTime();
                            int timeout = (int) (oldInfo.getBlockInterval() * 1000) - (int) (((Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l - startTime) * 1000);
                            timeout = timeout / 1000;
                            if (timeout <= 0) {
                                _shutdownServers.put(oldInfo.getBlockServerAddress(), ssinfo);
                            }
                        }
                    }

                    break;

                case UNBLOCK_ACTIVITY:
                    Address blockServer = new Address(response.getProtobufResponse().getUnblockActivityEvent().getServerIP(), response.getProtobufResponse().getUnblockActivityEvent().getPort());
                    if (_shutdownServers.containsKey(blockServer)) {
                        ShutDownServerInfo ssInfo = (ShutDownServerInfo) _shutdownServers.get(blockServer);
                        if (ssInfo != null) {
                            if (ssInfo.getUniqueBlockingId().equals(response.getProtobufResponse().getUnblockActivityEvent().getUniqueKey())) {
                                if (_pool.contains(blockServer)) {
                                    _pool.getConnection(blockServer).disconnect();
                                    _pool.remove(blockServer);
                                }
                                if (_connection.getServerAddress().equals(blockServer)) {
                                    ConnectionManager con = _pool.GetAnyConnection(); //TryPool or GetAnyConnection here?
                                    if (con != null) {
                                        _connection = con;
                                    }
                                }

                                if (_connection != null && !_connection.isReconnecting()) {
                                    if (_asyncProcessor != null) {
                                        _asyncProcessor.Enqueue(new ReconnectTask(this, _connection));
                                    }
                                }

                                synchronized (ssInfo.getWaitForBlockedActivity()) {
                                    _shutdownServers.remove(blockServer);

                                    Monitor.pulse(ssInfo.getWaitForBlockedActivity());

                                }

                            }
                        }
                    }
            }

        }

        private Object getDeserializedObject(byte[] value) throws GeneralFailureException {
            try {
                ByteArrayInputStream val = new ByteArrayInputStream(value);
                ObjectInput ow = new ObjectInputStream(val, this._cacheId);
                return ow.readObject();
            } catch (IOException iOException) {
                return value;
            } catch (ClassNotFoundException classNotFoundException) {
                throw new GeneralFailureException(classNotFoundException.getMessage());

            }
        }

        public String getDefaultReadThruProvider() throws ConfigurationException {
            if (_clientConfig == null) {
                throw new ConfigurationException("client configuration not initialized");
            }

            return _clientConfig.getDefaultReadThru();
        }

        public String getDefaultWriteThruProvider() throws ConfigurationException {
            if (_clientConfig == null) {
                throw new ConfigurationException("client configuration not initialized");
            }

            return _clientConfig.getDefaultWriteThru();
        }

        public final class BulkEventStructure implements Runnable {

            public com.alachisoft.tayzgrid.caching.EventId eventId;
            public Address remoteServerAddress = null;
            public List<com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse> bulkEventList;
            public Broker parent;
            public PerfStatsCollector _perfStatsCollector;
            public PersistenceManager _persistenceManager;

            public BulkEventStructure() {
            }

            @Override
            public void run() {
                try {
                    if (!bulkEventList.isEmpty()) {
                        com.alachisoft.tayzgrid.caching.EventId eventId = null;

                        for (BulkEventItemResponse eventItem : bulkEventList) {
                            try {
                                if (_perfStatsCollector != null) {
                                    eventId = new com.alachisoft.tayzgrid.caching.EventId();
                                }

                                switch (eventItem.getEventType()) {
                                    case ITEM_ADDED_EVENT: {
                                        com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId eventItemEventId = eventItem.getItemAddedEvent().getEventId();
                                        eventId.setEventUniqueID(eventItemEventId.getEventUniqueId());
                                        eventId.setEventCounter(eventItemEventId.getEventCounter());
                                        eventId.setOperationCounter(eventItemEventId.getOperationCounter());
                                        eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_ADDED_EVENT);

                                        BitSet flag = new BitSet((byte) eventItem.getItemAddedEvent().getFlag());

                                        EventCacheItem item = EventUtil.ConvertToEventEntry(eventItemEventId.getItem());

                                        if (_persistenceManager != null && !_persistenceManager.PersistEvent(eventId)) {
                                            peristentDuplicates++;
                                            continue;
                                        }
                                        Object key = CacheKeyUtil.SafeDeserialize(eventItem.getItemAddedEvent().getKey(), cacheId);
                                        if (_notifyAsync) {
                                            _cache._EventsListener.OnItemAdded(key, _notifyAsync, item, flag);
                                        } else {
                                            _eventProcessor.Enqueue(new ItemAddedEventTask(parent, key, _notifyAsync, item, flag));
                                        }
                                    }
                                    break;
                                    case ITEM_UPDATED_EVENT: {
                                        com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId eventItemEventId = eventItem.getItemUpdatedEvent().getEventId();
                                        eventId.setEventUniqueID(eventItemEventId.getEventUniqueId());
                                        eventId.setEventCounter(eventItemEventId.getEventCounter());
                                        eventId.setOperationCounter(eventItemEventId.getOperationCounter());
                                        eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_EVENT);

                                        BitSet flag = new BitSet((byte) eventItem.getItemUpdatedEvent().getFlag());

                                        EventCacheItem item = EventUtil.ConvertToEventEntry(eventItemEventId.getItem());
                                        EventCacheItem oldItem = EventUtil.ConvertToEventEntry(eventItem.getItemUpdatedEvent().getEventId().getOldItem());

                                        if (_persistenceManager != null && !_persistenceManager.PersistEvent(eventId)) {
                                            peristentDuplicates++;
                                            continue;
                                        }
                                        Object key = CacheKeyUtil.SafeDeserialize(eventItem.getItemUpdatedEvent().getKey(), cacheId);
                                        if (_notifyAsync) {
                                            _cache._EventsListener.OnItemUpdated(key, _notifyAsync, item, oldItem, flag);
                                        } else {
                                            _eventProcessor.Enqueue(new ItemUpdatedEventTask(parent, key, _notifyAsync, item, oldItem, flag));
                                        }
                                    }
                                    break;
                                    case ITEM_UPDATED_CALLBACK: {
                                        com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId eventItemEventId = eventItem.getItemUpdatedCallback().getEventId();
                                        eventId.setEventUniqueID(eventItemEventId.getEventUniqueId());
                                        eventId.setEventCounter(eventItemEventId.getEventCounter());
                                        eventId.setOperationCounter(eventItemEventId.getOperationCounter());
                                        eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_CALLBACK);

                                        BitSet flag = new BitSet((byte) eventItem.getItemUpdatedCallback().getFlag());

                                        EventCacheItem item = EventUtil.ConvertToEventEntry(eventItemEventId.getItem());
                                        EventCacheItem oldItem = EventUtil.ConvertToEventEntry(eventItem.getItemUpdatedCallback().getEventId().getOldItem());

                                        if (_persistenceManager != null && !_persistenceManager.PersistEvent(eventId)) {
                                            continue;
                                        }

                                        Object key = CacheKeyUtil.SafeDeserialize(eventItem.getItemUpdatedCallback().getKey(), cacheId);
                                        if (_notifyAsync) {
                                            if (_cache != null && _cache.getAsyncEventsListener() != null) {
                                                _cache.getEventsListener().OnCustomUpdateCallback((short) eventItem.getItemUpdatedCallback().getCallbackId(),
                                                        key, _notifyAsync, item, oldItem, flag,
                                                        EventDataFilter.forValue(eventItem.getItemUpdatedCallback().getDataFilter()));
                                            }
                                        } else {
                                            _eventProcessor.Enqueue(new ItemUpdatedCallbackTask(parent, key, (short) eventItem.getItemUpdatedCallback().getCallbackId(), _notifyAsync, item, oldItem, flag, EventDataFilter.forValue(eventItem.getItemUpdatedCallback().getDataFilter())));
                                        }
                                    }
                                    break;
                                    case ITEM_REMOVED_CALLBACK: {
                                        BitSet flag = new BitSet((byte) eventItem.getItemRemoveCallback().getFlag());

                                        ItemRemoveCallbackResponse cb = eventItem.getItemRemoveCallback();
                                        EventCacheItem item = EventUtil.ConvertToEventEntry(eventItem.getItemRemoveCallback().getEventId().getItem());
                                        byte[] value = null;
                                        if (item != null && cb != null && cb.getValueList() != null
                                                && !cb.getValueList().isEmpty() && cb.getValueList().size() > 0) {

                                            List<byte[]> bite = new ArrayList<byte[]>();
                                            for (ByteString byteString : cb.getValueList()) {
                                                bite.add(byteString.toByteArray());
                                            }

                                            UserBinaryObject ubObject = new UserBinaryObject(bite.toArray());
                                            value = ubObject.GetFullObject();
                                            item.setValue(value);
                                            //parent.setValue(ubObject.GetFullObject());
                                            //item.setValue(parent.getValue());
                                        }

                                        com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId eventItemEventId = cb.getEventId();
                                        eventId.setEventUniqueID(eventItemEventId.getEventUniqueId());
                                        eventId.setEventCounter(eventItemEventId.getEventCounter());
                                        eventId.setOperationCounter(eventItemEventId.getOperationCounter());
                                        eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_CALLBACK);

                                        if (_persistenceManager != null && !_persistenceManager.PersistEvent(eventId)) {
                                            continue;
                                        }

                                        if (_notifyAsync) {
                                            Object key = CacheKeyUtil.SafeDeserialize(cb.getKey(), cacheId);
                                            if (_cache != null && _cache.getAsyncEventsListener() != null) {
                                                _cache.getEventsListener().OnCustomRemoveCallback((short) cb.getCallbackId(), key, (Object) value, CacheItemRemovedReason.forValue(cb.getItemRemoveReason()), flag, _notifyAsync, item, EventDataFilter.forValue(cb.getDataFilter()));
                                            } else {
                                                _eventProcessor.Enqueue(new ItemRemovedCallbackTask(parent, key, (short) cb.getCallbackId(), value, CacheItemRemovedReason.forValue(cb.getItemRemoveReason()), flag, _notifyAsync, item, EventDataFilter.forValue(cb.getDataFilter())));
                                            }
                                        }
                                    }
                                    break;
                                    case ITEM_REMOVED_EVENT: {
                                        BitSet flag = new BitSet((byte) eventItem.getItemRemovedEvent().getFlag());

                                        ItemRemovedEventResponse cb = eventItem.getItemRemovedEvent();
                                        EventCacheItem item = EventUtil.ConvertToEventEntry(eventItem.getItemRemovedEvent().getEventId().getItem());
                                        byte[] value = null;
                                        if (item != null && cb != null && cb.getValueList() != null
                                                && !cb.getValueList().isEmpty() && cb.getValueList().size() > 0) {

                                            List<byte[]> bite = new ArrayList<byte[]>();
                                            for (ByteString byteString : cb.getValueList()) {
                                                bite.add(byteString.toByteArray());
                                            }
                                            UserBinaryObject ubObject = new UserBinaryObject(bite.toArray());
                                            value = ubObject.GetFullObject();
                                            item.setValue(value);

                                        }

                                        com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId eventItemEventId = cb.getEventId();
                                        eventId.setEventUniqueID(eventItemEventId.getEventUniqueId());
                                        eventId.setEventCounter(eventItemEventId.getEventCounter());
                                        eventId.setOperationCounter(eventItemEventId.getOperationCounter());
                                        eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_EVENT);

                                        if (_persistenceManager != null && !_persistenceManager.PersistEvent(eventId)) {
                                            peristentDuplicates++;
                                            continue;
                                        }

                                        Object key = CacheKeyUtil.SafeDeserialize(cb.getKey(), cacheId);
                                        if (_notifyAsync) {
                                            _cache.getEventsListener().OnItemRemoved(key, (Object) value, CacheItemRemovedReason.forValue(cb.getItemRemoveReason()), flag, _notifyAsync, item);
                                        } else {
                                            _eventProcessor.Enqueue(new ItemRemovedEventTask(parent, key, (Object) value, CacheItemRemovedReason.forValue(cb.getItemRemoveReason()), flag, _notifyAsync, item));
                                        }
                                    }
                                    break;
                                    case CACHE_CLEARED_EVENT: {
                                        com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId eventItemEventId = eventItem.getCacheClearedEvent().getEventId();
                                        eventId.setEventUniqueID(eventItemEventId.getEventUniqueId());
                                        eventId.setEventCounter(eventItemEventId.getEventCounter());
                                        eventId.setOperationCounter(eventItemEventId.getOperationCounter());
                                        eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.CACHE_CLEARED_EVENT);

                                        if (_notifyAsync) {
                                            _cache.getEventsListener().OnCacheCleared(_notifyAsync);
                                        } else {
                                            _eventProcessor.Enqueue(new CacheClearEventTask(parent, _notifyAsync));
                                        }
                                    }
                                    break;
                                    case RAISE_CUSTOM_EVENT: {
                                        if (_notifyAsync) {
                                            try {
                                                List<CustomListenerWrapper> customEventList = _cache.getCustomEventListenerList();
                                                for (CustomListenerWrapper listener : customEventList) {
                                                    CustomEvent event = new CustomEvent(_cacheId, CustomEvent.EventType.CUSTOM,
                                                            CompactBinaryFormatter.fromByteBuffer(eventItem.getCustomEvent().getKey().toByteArray(), _cacheId),
                                                            CompactBinaryFormatter.fromByteBuffer(eventItem.getCustomEvent().getValue().toByteArray(), _cacheId));
                                                    listener.getCustomEvent().customEventOccured(event);
                                                }

                                            } catch (IOException ex) {
                                                if (parent != null && parent.getLogger()!=null && parent.getLogger().getIsErrorLogsEnabled()) {
                                                    parent.getLogger().getCacheLog().Error("RIASE_CUSTOM_EVENT", ex.getMessage());
                                                }
                                            } catch (ClassNotFoundException ex) {
                                                if (parent != null && parent.getLogger()!=null && parent.getLogger().getIsErrorLogsEnabled()) {
                                                    parent.getLogger().getCacheLog().Error("RIASE_CUSTOM_EVENT", ex.getMessage());
                                                }
                                            }
                                        } else {
                                            _eventProcessor.Enqueue(new CustomEventTask(parent, eventItem.getCustomEvent().getKey().toByteArray(), cacheId, eventItem.getCustomEvent().getValue().toByteArray(), _notifyAsync));
                                        }
                                    }
                                    case ASYNC_OP_COMPLETED_EVENT: {
                                        Object asyncOpResult;
                                        com.alachisoft.tayzgrid.command.Command command = null;

                                        if (eventItem.getAsyncOperationCompletedCallback().getSuccess()) {
                                            asyncOpResult = AsyncOpResult.Success;
                                        } else {
                                            asyncOpResult = new Exception(eventItem.getAsyncOperationCompletedCallback().getExc().getMessage());
                                        }

                                        Request request = (Request) requestTable.get(eventItem.getAsyncOperationCompletedCallback().getRequestId());

                                        if (request != null) {
                                            synchronized (requestTable) {
                                                requestTable.remove(eventItem.getAsyncOperationCompletedCallback().getRequestId());
                                            }
                                            if (request.getCommands().size() > 0) {
                                                command = (Command) request.getCommands().get(remoteServerAddress);
                                            }
                                        }
                                        Object key = CacheKeyUtil.SafeDeserialize(eventItem.getAsyncOperationCompletedCallback().getKey(), cacheId);
                                        if (_notifyAsync) {
                                            if (_cache != null && _cache.getAsyncEventsListener() != null) {
                                                if (command instanceof AddCommand) {
                                                    _cache.getAsyncEventsListener().OnAsyncAddCompleted(key, (short) ((AddCommand) command).AsycItemAddedOpComplete(), asyncOpResult, _notifyAsync);
                                                } else if (command instanceof InsertCommand) {
                                                    _cache.getAsyncEventsListener().OnAsyncInsertCompleted(key, (short) ((InsertCommand) command).AsycItemUpdatedOpComplete(), asyncOpResult, _notifyAsync);
                                                } else if (command instanceof RemoveCommand) {
                                                    _cache.getAsyncEventsListener().OnAsyncRemoveCompleted(key, (short) ((RemoveCommand) command).AsycItemRemovedOpComplete(), asyncOpResult, _notifyAsync);
                                                } else if (command instanceof ClearCommand) {
                                                    _cache.getAsyncEventsListener().OnAsyncClearCompleted((short) ((ClearCommand) command).AsyncCacheClearedOpComplete(), asyncOpResult, _notifyAsync);
                                                } else if (command instanceof BulkGetCommand) {
                                                    _cache.getAsyncEventsListener().OnJCacheLoadingCompletion((short) ((BulkGetCommand) command).JCacheLoaderOpComplete(), asyncOpResult, _notifyAsync);
                                                }
                                            }
                                        } else {
                                            _eventProcessor.Enqueue(new AsyncOperationCompletedEventTask(parent, command, key, asyncOpResult, _notifyAsync));
                                        }
                                    }
                                    break;
                                    case DS_UPDATED_CALLBACK: {
                                        DSUpdatedCallbackResponse cb = eventItem.getDSUpdatedCallback();
                                        OpCode operationCode = OpCode.getOpCode(eventItem.getDSUpdatedCallback().getOpCode());
                                        java.util.Hashtable resultDic = new java.util.Hashtable();

                                        for (DSUpdatedCallbackResult response : cb.getResultList()) {
                                            if (response.getSuccess()) {
                                                resultDic.put(response.getKey(), DataSourceOpResult.Success);
                                            } else if (response != null && response.getException() != null) {
                                                resultDic.put(response.getKey(), new OperationFailedException(response.getException().getMessage()));
                                            } else {
                                                resultDic.put(response.getKey(), DataSourceOpResult.Failure);
                                            }
                                        }

                                        if (_notifyAsync) {
                                            if (_cache != null && _cache.getAsyncEventsListener() != null) {
                                                _cache.getAsyncEventsListener().OnDataSourceUpdated((short) eventItem.getDSUpdatedCallback().getCallbackId(), resultDic, operationCode, _notifyAsync);
                                            }
                                        } else {
                                            _eventProcessor.Enqueue(new DSUpdateEventTask(parent, (short) eventItem.getDSUpdatedCallback().getCallbackId(), resultDic, operationCode, _notifyAsync));
                                        }

                                    }
                                    case MAP_REDUCE_TASK_CALLBACK: {
                                       if (parent != null && parent.getLogger()!=null && parent.getLogger().getIsErrorLogsEnabled()) {
                                            parent.getLogger().getCacheLog().Info("RemoteCache.TaskCallback", "MapReduce task callback have been received.");
                                       }
                                       
                                        com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId mrEventId = eventItem.getTaskCallback().getEventId();
                                        eventId.setEventUniqueID(mrEventId.getEventUniqueId());
                                        eventId.setEventCounter(mrEventId.getEventCounter());
                                        eventId.setOperationCounter(mrEventId.getOperationCounter());
                                        eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.TASK_CALLBACK);

                                        TaskCallbackResponseProtocol.TaskCallbackResponse cbRes = eventItem.getTaskCallback();
                                        if (_notifyAsync && cbRes != null) {
                                            _cache.getEventsListener().OnMapReduceCompleteCallback(cbRes.getTaskId(), (int) cbRes.getTaskStatus(), (short) cbRes.getCallbackId());
                                        } else {
                                        }
                                        break;
                                    }

                                }
                            } catch (Exception ex) {
                                if (parent != null && parent.getLogger()!=null && parent.getLogger().getIsErrorLogsEnabled()) {
                                    parent.getLogger().getCacheLog().Error("Broker.BulkEventStructure", "An error occured while raising bulk event of type : " + eventItem.getEventType() + ". Error :" + ex.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                }
            }
        }

        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Send Command ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ">
        private void sendCommand(Command command) throws ConfigurationException, ConnectionException, OperationFailedException,  GeneralFailureException, java.net.UnknownHostException, ActivityBlockedException {
            ConnectionManager connection = this._connection;
            if (command.getKey() != null && this._importHashmap) {
                Address ip;// = "";
                synchronized (_hashmapUpdateMutex) {
                    ip = this._pool.getIp(command.getKey());
                }
                if (ip != null) {

                    try {
                        connection = getConnection(ip, true);// this._pool.getConnection(ip);
                    } catch (ConnectionException ex) {
                        Logger log = Logger.getLogger("com.alachisoft");
                        if (log.isLoggable(Level.SEVERE)) {
                            log.log(Level.SEVERE, ex.toString());
                        }
                    } catch (ConfigurationException ex) {
                        throw new GeneralFailureException(ex.getMessage());
                    }
                    if (connection == null || (connection != null && !connection.isConnected())) {
                        try {
                            connection = this.tryPool();
                        } catch (ConnectionException ex) {
                            Logger log = Logger.getLogger("com.alachisoft");
                            if (log.isLoggable(Level.SEVERE)) {
                                log.log(Level.SEVERE, ex.toString());
                            }
                        } catch (ConfigurationException ex) {
                            throw new GeneralFailureException(ex.getMessage());
                        }
                    }
                }
            } else if (this._importHashmap && !connection.isConnected()) {
                connection = _connection = tryPool();

            }
            sendCommand(connection, command, true);
        }

        private void sendCommand(ConnectionManager connection, Command command, boolean checkConnected) throws OperationFailedException,  GeneralFailureException, java.net.UnknownHostException, ActivityBlockedException {

            Address ip = connection.getServerAddress();
            if (checkConnected) {
                connection.getStatusLatch().waitForAny((byte) (ConnectionStatus.CONNECTED | ConnectionStatus.DISCONNECTED | ConnectionStatus.LOADBALANCE));
            }
            try {
                doSendCommand(connection, command, checkConnected);
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("Broker.SendCommand", "RequestID : " + command.getRequestId() + " " + command.getCommandName() + " sent to server "
                            + connection.getIp());
                }
            } catch (ActivityBlockedException aex) {
                throw aex;
            } catch (ConnectionException ex) {

                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.SendCommand", "RequestID :" + command.getRequestId() + " " + command.getCommandName() + " can not sent to server "
                            + connection.getIp());
                }

                synchronized (requestMutex) {
                    Request request = command.getParent();
                    if (!this._importHashmap) {
                        if (request.removeResponse(ip)) {
                            requestTable.remove(request.getRequestId());
                            if (_perfStatsCollector != null) {
                                _perfStatsCollector.decrementRequestQueueSize();
                            }
                        }
                    } else {
                        request.ResetFailedResponse(connection.getServerAddress());
                    }
                }

                if (!this._importHashmap) {
                    tryNextServer();
                }
                connection = this._connection;
                if (this._importHashmap && !connection.isConnected()) {
                    try {
                        connection = this.getConnection(connection.getServerAddress(), false);
                        if (_logger.getIsDetailedLogsEnabled()) {
                            _logger.getCacheLog().Debug("Broker.sendCommand", "getConnection called and returned a connection");
                        }

                    } catch (ConnectionException ex1) {
                        if (_logger.getIsDetailedLogsEnabled()) {
                            _logger.getCacheLog().Debug("Broker.sendCommand", "Connection exception thrown by getConnection");
                        }

                    } catch (ConfigurationException ex1) {
                        throw new GeneralFailureException(ex1.getMessage());
                    }
                }
                try {
                    command.resetBytes();
                    addRequestToRequestTable(command.getParent());
                    doSendCommand(connection, command, checkConnected);
                } catch (ActivityBlockedException aex) {
                    throw aex;
                } catch (ConnectionException ex1) {
                    if (_logger.getIsErrorLogsEnabled()) {
                        _logger.getCacheLog().Error("Broker.SendCommand", "RequestID :" + command.getRequestId() + " " + command.getCommandName() + " can not sent to server "
                                + connection.getIp());
                    }
                    throw new OperationFailedException("No server is available to process the request");
                } catch (Exception ex2) {
                    if (_logger.getIsErrorLogsEnabled()) {
                        _logger.getCacheLog().Error("Broker.SendCommand", "RequestID :" + command.getRequestId() + " " + command.getCommandName() + " can not sent to server "
                                + connection.getIp() + " " + ex2.toString());
                    }
                    throw new OperationFailedException(ex2.getMessage());
                }
            } catch (Exception ex2) {
                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.SendCommand", "RequestID :" + command.getRequestId() + " " + command.getCommandName() + " can not sent to server "
                            + connection.getIp());
                }
                throw new OperationFailedException(ex2.toString());
            }
        }

        private void doSendCommand(ConnectionManager connection, Command command, boolean checkConnected) throws ConnectionException, CommandException, ActivityBlockedException {

            if (_shutdownServers.size() > 0) {
                boolean reacquiredLock = true;

                if (command.getCommandRequestType() != RequestType.InternalCommand) {
                    ShutDownServerInfo ssInfo = (ShutDownServerInfo) _shutdownServers.get(connection.getServerAddress());
                    if (ssInfo != null) {
                        synchronized (ssInfo.getWaitForBlockedActivity()) {
                            if (_shutdownServers.containsKey(connection.getServerAddress())) {

                                long startTime = ssInfo.getStartBlockingTime();
                                int timeout = (int) (ssInfo.getBlockInterval() * 1000) - (int) (((Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l - startTime) * 1000);
                                if (timeout > 0) {
                                    try {
                                        reacquiredLock = Monitor.wait(ssInfo.getWaitForBlockedActivity(), timeout);
                                    } catch (Exception ex) {
                                    }
                                }
                                throw new ActivityBlockedException("Request timeout due to node down", ssInfo.getBlockServerAddress());
                            }

                        }
                    }
                }
            }

            initializeResponse(connection, command);
            command.setCacheId(_cacheId);

            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("doSendCommand:\tServer name: " + connection.getServer() + " command type = " + command.getCommandName());
            }

            connection.sendCommand(command.toByteArray(), checkConnected);
        }
        //</editor-fold>

        ConnectionManager verifyServerConnectivity(Address serverIP, boolean optimizeConnection) {
            ConnectionManager con = null;
            try {
                if (optimizeConnection) {
                    con = getConnection(serverIP, false);
                    if (!con.isConnected()) {
                        con = tryPool();
                    }
                } else {
                    con = getConnection(serverIP, true);
                }
            } catch (Exception ex) {
            }
            return con;
        }

        private ConnectionManager getConnection(Address ip, boolean strictMatch) throws ConfigurationException,  ConnectionException, UnknownHostException {
            ConnectionManager connection = this._pool.getConnection(ip);
            if (connection != null) {
                if (!connection.isConnected()) {
                    if (connection.notifRegistered) {
                        reregisterNotification(connection);
                    }

                    if (!connection.isReconnecting()) {
                        _asyncProcessor.Enqueue(new ReconnectTask(this, connection));
                    }
                } else {
                    return connection;
                }
            }
            if (this._importHashmap && !strictMatch) {
                connection = tryPool();
            }

            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("getConnetion:\t ip = " + ip + " ,strictMatch = " + strictMatch);
            }
            if (connection == null) {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("getConnection:\t connection is null");
                }
            } else {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("getConnection:\t connected? = " + connection.isConnected() + ", ip = " + connection.getServer());
                }
            }

            return connection;
        }

        //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Failover Section ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ">
        //public void serverLost(String ip, int port, boolean forcedDisconnect) {
        public void serverLost(Address ip, boolean forcedDisconnect) {

            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().CriticalInfo("ServerLost", "Server lost " + ip + "; forcedDisconnected = " + forcedDisconnect);
            }
            if (this._importHashmap) {
                if (!forcedDisconnect) {
                    try {
                        reregisterNotification(ip);

                        ConnectionManager connection = this._pool.getConnection(ip);
                        if (connection != null && !connection.isReconnecting()) {
                            _asyncProcessor.Enqueue(new ReconnectTask(this, connection));
                        }
                    } catch (Exception e) {
                        if (_logger.getIsErrorLogsEnabled()) {
                            _logger.getCacheLog().Debug("Broker.ServerLost", e.getMessage());
                        }
                    }
                }
            } else {

                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("Broker.NewHashmapRecieved", "Disconnected from " + ip + ", and removed from pool");
                }
                this._pool.remove(ip);
                if (_shutdownServers.containsKey(ip)) {
                    ShutDownServerInfo ssInfo = (ShutDownServerInfo) _shutdownServers.get(ip);

                    synchronized (ssInfo.getWaitForBlockedActivity()) {

                        _shutdownServers.remove(ip);
                        Monitor.pulse(ssInfo.getWaitForBlockedActivity());

                    }

                }
            }
            resetBroker(ip);
            this._clientConfig.RemoveServer(new RemoteServer(ip.getIpAddress().toString(), ip.getPort()));

            if (forcedDisconnect || this._connection.getStatusLatch().isAnyBitsSet(ConnectionStatus.BUSY)) {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("ServerLost", "Connection is busy");
                }

                return;
            }

            try {
                if (!this._connection.isConnected() && !this._importHashmap) {
                    if (_logger.getIsDetailedLogsEnabled()) {
                        _logger.getCacheLog().Debug("ServerLost", "trying next server");
                    }

                    tryNextServer();
                }

            } catch (CacheException ex) {

                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().CriticalInfo("ServerLost", "failed to connect to any server in the list." + ex.toString());
                }
            } finally {
                try {
                    ConnectionManager pooledConn = tryPool();

                    if (_logger.getIsErrorLogsEnabled()) {
                        _logger.getCacheLog().CriticalInfo("ServerLost", "pooledConn == null ? " + (pooledConn == null));
                    }
                    if (!this._connection.isConnected() && (pooledConn != null && !pooledConn.isConnected())) {
                        if (_logger.getIsErrorLogsEnabled()) {
                            _logger.getCacheLog().CriticalInfo("ServerLost", "notfiying cache stopped...");
                        }

                        notifyCacheStoppedEvent(cacheId);
                    }
                } catch (Exception ex) {
                    if (_logger.getIsErrorLogsEnabled()) {
                        _logger.getCacheLog().CriticalInfo("ServerLost", "Exception on server lost: " + ex.toString());
                    }
                }
            }
        }

        private void sendUnregisterEventCommand(int modifier) throws OperationFailedException, GeneralFailureException, AggregateException,  ConnectionException, ConfigurationException, java.net.UnknownHostException {
            Command cm = new RegisterNotification(modifier, (short) -1);

            if (!this._importHashmap) {
                try {
                    sendCommand(cm);
                } catch (Exception ex) {
                }
            } else {
                HashMap pool = this._pool.cloneConnectionPool();
                ConnectionManager connection = null;
                Iterator<ConnectionManager> enu = pool.values().iterator();
                while (enu.hasNext()) {
                    connection = enu.next();
                    if (connection != null && connection.isConnected() && connection.notifRegistered) {
                        try {
                            sendCommand(connection, cm, true);
                        } catch (Exception ex) {
                        }
                        break;
                    }
                }
            }

            exceptionOccured(cm.getResponse());
        }

        private void reregisterNotification(Address ip) {
            this.reregisterNotification(this._pool.getConnection(ip));
        }

        private void reregisterNotification(ConnectionManager connection) {
            if (connection != null && connection.notifRegistered) {
                connection.notifRegistered = false;
                ConnectionManager selected = this._connection;

                if (!selected.isConnected()) {
                    try {
                        selected = tryPool();
                    } catch (Exception ex) {
                        if (_logger.getIsErrorLogsEnabled()) {
                            _logger.getCacheLog().Error("Remote.reregisterNotification: " + ex.getMessage());

                        }

                    }
                }

                if (selected != null && selected.isConnected()) {
                    try {
                        reregisterGeneralNotifications(selected);

                    } catch (Exception ex) {
                        Logger.getLogger(Cache.class
                                .getName()).log(Level.SEVERE, null, ex);
                    }

                    selected.notifRegistered = true;
                }
            }
        }

        @SuppressWarnings("static-access")
        private void tryNextServer() throws OperationFailedException{
            boolean connected = false;
            RemoteServer startingServer = null;
            
            int retries = _connectionRetries;

            try {
                if (!_lock.tryLock(_connectionMutexTimeout, TimeUnit.SECONDS)) {
                    return;
                }
            } catch (Exception e) {
                return; //lock could not be granted before the timeout expires.
            }

            try {
                checkRetryConnectionDelay(); //[KS: Checking if retry connection Interval is over or not]
                if (!_retryConnection) {
                    return;
                }

                while (retries-- > 0) {
                    try {
                        if (!_connection.isConnected()) {
                            _connection.getStatusLatch().setStatusBit(ConnectionStatus.BUSY, (byte) (ConnectionStatus.CONNECTED | ConnectionStatus.DISCONNECTED));

                            if (_clientConfig == null) {
                                _clientConfig = new ClientConfiguration(cacheId);
                            }
                            _clientConfig.LoadConfiguration();

                            if (_clientConfig.getServerCount() > 0) {
                                RemoteServer nextServer = _clientConfig.getNextServer();
                                startingServer = nextServer;
                                while (!connected) {
                                    if (nextServer == null) {
                                        break;
                                    }
                                    for (int i = 0; i < nextServer.getPortRange(); i++) {
                                        try {
                                            if (_logger.getIsDetailedLogsEnabled()) {
                                                _logger.getCacheLog().Debug("tryNextServer:\tTrying to connect with " + nextServer.toString());
                                            }
                                            connected = connectRemoteServer(this._connection, nextServer, true);
                                            if (connected) {
                                                //save the successful port
                                                this._port = _connection.getPort();
                                                break;
                                            }
                                        } catch (Exception ex) {
                                            if (_logger.getIsErrorLogsEnabled()) {
                                                _logger.getCacheLog().Error("Broker.tryNextServer: " + ex.getMessage());

                                            }

                                      
                                        }
                                    }
                                    if (!connected) {
                                        nextServer = _clientConfig.getNextServer();
                                        if (startingServer == nextServer) {
                                            break;
                                        }
                                    }
                                }

                                //if the connection is established, exit the outer loop.
                                //otherwise sleep for the sleep interval and retry.
                                if (connected) {
                                    break;
                                }
                                Thread.sleep(_retryInterval * 1000);
                                continue;
                            }
                        } else {
                            connected = true;
                        }
                    } catch (Exception e) {
                        throw e;
                    }
                }
            } catch (Exception e) {
                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.tryNextServer: " + e.getMessage());

                }
                throw new OperationFailedException(e.getMessage());
            } finally {
                byte setStatus = connected ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED;
                byte unsetStatus = (byte) ((!connected ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED) | ConnectionStatus.BUSY);
                _connection.getStatusLatch().setStatusBit(setStatus, unsetStatus);

                _retryConnection = connected;

                _lock.unlock();

            }

            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("tryNextServer:\tconnected ? " + connected);
            }

        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ConnectRemoteServer ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ">
        private boolean connectRemoteServer(ConnectionManager connection, RemoteServer server, boolean registerNotif) throws ConnectionException,  UnknownHostException,   Exception {
            boolean connected = false;

            if (server != null && !server.getName().equals("")) {
                connected = connectRemoteServer(connection, server.getName(), server.getPort(), this._balanceNodes, this._importHashmap, registerNotif);
            }
            return connected;
        }

        private boolean connectRemoteServer(ConnectionManager connection, String addr, int port, boolean nodeBalance, boolean importHashmap, boolean registerNotif) throws ConnectionException,  java.net.UnknownHostException,   Exception {
            if (addr == null || port == 0) {
                return false;
            }

            boolean connected = false;
            connected = connection.connect(addr, port);

            if (connected) {
                if (!isCacheBind) {

                    CommandResponse cacheBinding = getCacheBinding(connection);
                    addr = cacheBinding.getIp();
                    port = cacheBinding.getPort();
                    //if (cacheBinding.getIsRunning()) {
                    if (port != _port) {
                        connected = connection.connect(addr, port);
                    }
                    //} else {
                    // connected = false;
                    //}
                    isCacheBind = connected;
                }
            }

            Write("connectRemoveServer:\t connected ? " + connected);
            if (_logger == null) {
                throw new Exception("Logger is null");
            }
            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Info("Broker.ConnectRemoteServer", "[Local : (" + connection.getCurrentIP() + ") Server : (" + connection.getServer() + ":" + port
                        + ")] connected successfully");
            }

            if (connected && nodeBalance ) {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("connectRemoveServer:\t node balancing");
                }
                CommandResponse optimalResponse = getOptimalServer(connection);

                if (optimalResponse != null) {
                    RemoteServer rm = _clientConfig.getMappedServer(optimalResponse.getIp(), optimalResponse.getPort());

                    if (rm.getName() != null && !rm.getName().isEmpty() && !rm.getName().equalsIgnoreCase(addr)) {
                        connection.disconnect();
                        connected = connection.connect(rm.getName(), rm.getPort());
                    }
                }

            }

            if (connected) {
                try {
//                    Write("connectRemoveServer:\t initializing cache");
                    initializeCache(connection);
                    if (_logger.getIsDetailedLogsEnabled()) {
                        _logger.getCacheLog().Info("Broker.ConnectRemoteServer", "[Local : (" + connection.getCurrentIP() + ") Server : (" + connection.getServer() + ":" + port
                                + ")] initialized cache successfully");
                    }

                    java.util.HashMap<String, Integer> runningServers = getRunningServers(connection, addr, port);

                    if (runningServers != null) {
                        int outPort;

                        Iterator it = runningServers.entrySet().iterator();
                        while (it.hasNext()) {
                            RemoteServer rServer = new RemoteServer();
                            Map.Entry pairs = (Map.Entry) it.next();
                            rServer.setName(pairs.getKey().toString());
                            rServer.setPort(Integer.parseInt(pairs.getValue().toString()));
                            _clientConfig.addServer(rServer);
                        }
                    }

                } catch (Exception e) {

                    if (_logger.getIsErrorLogsEnabled()) {
                        _logger.getCacheLog().Error("Broker.ConnectRemoteServer", e.toString());
                    }
                    connection.disconnect();
                    connected = false;
                }
            } else if (_logger.getIsErrorLogsEnabled()) {
                _logger.getCacheLog().Error("Broker.ConnectRemoteServer", "Could not connect to server (" + connection.getServer() + ":" + port + ")");
            }

            if (connected) {
                connection.startReceiver();
                connection.getStatusLatch().setStatusBit(ConnectionStatus.CONNECTED, (byte) (ConnectionStatus.DISCONNECTED | ConnectionStatus.BUSY));

                this._serverIP = connection.getServerAddress();

                this._port = connection.getPort();

                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("connectRemoteServer:\tadding to pool");
                }
                this._pool.add(connection.getServerAddress(), connection);

                if (importHashmap) {
                    this.getHashmap();
                }

                try {
                    if (registerNotif) {
                        reregisterGeneralNotifications(connection);

                        connection.notifRegistered = true;
                    }

                    if (this._importHashmap) {
                        registerHashmapRecievedEvent(connection);
                    }
                    getTypeInfoMap(connection, false);

                   

                } catch (Exception ex) {
                    if (_logger.getIsErrorLogsEnabled()) {
                        _logger.getCacheLog().Error("Broker.connectRemoteServer: " + ex.getMessage());

                    }
                }

                if (_persistenceManager != null && !_connectingFirstTime) {
                    this._asyncProcessor.Enqueue(new SynchronizeEventsTask(this, connection));
                }
                _connectingFirstTime = false;

            }

            return connected;
        }

        private java.util.HashMap<String, Integer> getRunningServers(ConnectionManager conn, String coonectedServerAddress, int port) {
            GetRunningServersCommand command = new GetRunningServersCommand(_cacheId);
            Request request = new Request(false, Broker._commandTimeout);
            request.addCommand(conn.getServerAddress(), command);
            CommandResponse runningServers;
            try {
                Broker.executeRequest(request, conn, false, false);
                runningServers = conn.AssureRecieve();

                synchronized (requestTable) {
                    requestTable.remove(request.getRequestId());
                    if (_perfStatsCollector != null) {
                        _perfStatsCollector.decrementRequestQueueSize();
                    }
                }

                if (runningServers != null) {
                    runningServers.parseResponse(cacheId);
                    return runningServers.getRunningServers();
                }
            } catch (Exception ex) {
                // 
            }

            return null;

        }
        //</editor-fold>

//         /// <summary>
//        /// Checks if the retry connection interval is over and sets retry connection flag to true.
//        /// </summary>
        private void checkRetryConnectionDelay() {
            Date currentTime = new Date();
            long span = currentTime.getTime() - _retryConnectionStartTime.getTime();
            span = span / 1000;
            if (span >= _retryConnectionDelayInMinutes) {
                _retryConnectionStartTime = currentTime;
                _retryConnection = true;
            }
        }

        /**
         *
         */
        void getHashmap() {
            this.getHashmap(null);
        }

        public boolean importHashmap() {
            return _importHashmap;
        }

        /**
         *
         * @param connection
         */
        private void getHashmap(ConnectionManager connection) {
            GetHashmapCommand command = new GetHashmapCommand();

            Request request = new Request(false, Broker._commandTimeout);
            Address ipAddress = connection == null ? _connection.getServerAddress() : connection.getServerAddress();

            request.addCommand(ipAddress, command);

            CommandResponse response = null;
            try {
                if (connection != null) {
                    executeRequest(request, connection, true, true);
                } else {
                    executeRequest(request);
                }

                response = request.getResponse();
                exceptionOccured(response);
            } catch (Exception ex) {
                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.getHashMap: " + ex.getMessage());

                }
                return;
            }
            UpdatePoolExecutor updatePoolExec = new UpdatePoolExecutor(response, false);
            com.alachisoft.tayzgrid.common.threading.ThreadPool.executeTask(updatePoolExec);
        }

        /**
         * Update HashMap of Nodes connected of cluster this broker is connected
         * to
         *
         * @param response
         */
        private void updatePool(CommandResponse response, boolean queue) {

            NewHashMap map = null;

            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("udpatePool:\tUpdate pool called.");
            }
            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("Broker.UpdatePool", "Update pool called.");
            }
            try {

                if (queue == false) {

                    map = (NewHashMap) response.getResultMap();
                } else if (response.getValue() != null) {
                    com.alachisoft.tayzgrid.common.datastructures.NewHashmap newHashMap = (com.alachisoft.tayzgrid.common.datastructures.NewHashmap) CompactBinaryFormatter.fromByteBuffer(response.getValue(), cacheId);
                    map = new NewHashMap(newHashMap.getLastViewId(), newHashMap.getMap(), newHashMap.getMembers(), 0, newHashMap.getUpdateMap());
                }
            } catch (UnsupportedEncodingException ex) {
                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.UpdatePool", ex.toString());
                }
            } catch (IOException ex) {
                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.UpdatePool", ex.toString());
                }
            } catch (ClassNotFoundException ex) {
                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.UpdatePool", ex.toString());
                }
            }
            if (map == null || map.getMap() == null) {
                this._importHashmap = false;
                return;
            }

            if (!queue) {
                this._pool.setBucketSize(map.getBucketSize());
                this.newHashmapRecieved(map);
            } else {
                this._newMapTask.queue(map);
            }

        }

        private void startBalancingClients() {
            boolean isReleaseLock = false;
            try {
                this._lock.lock();

                isReleaseLock = true;

                this._connection.getStatusLatch().setStatusBit((byte) (ConnectionStatus.BUSY | ConnectionStatus.LOADBALANCE), (byte) (ConnectionStatus.CONNECTED
                        | ConnectionStatus.DISCONNECTED));
                int totalTimeToWait = this._commandTimeout;
                int timeSlice = 2000;

                if (totalTimeToWait == 0) {
                    timeSlice = 0;
                } else if (timeSlice > totalTimeToWait) {
                    timeSlice = totalTimeToWait;
                    totalTimeToWait = 0;
                } else {
                    totalTimeToWait -= timeSlice;
                }

                int nextInterval = timeSlice;

                do {
                    if (_logger.getIsDetailedLogsEnabled()) {
                        _logger.getCacheLog().Info("Broker.StartBalancingClients : total wait time remaining: " + totalTimeToWait);
                        _logger.getCacheLog().Info("Broker.StartBalancingClients : current wait interval: " + nextInterval);
                    }
                    Thread.sleep(nextInterval);

                    synchronized (requestMutex) {
                        if (this.requestTable.size() == 0) {
                            break;
                        }
                        if (_logger.getIsDetailedLogsEnabled()) {
                            _logger.getCacheLog().Info("Broker.StartBalancingClients : Responses remaining: " + this.requestTable.size());
                        }
                    }

                    if (totalTimeToWait == 0) {
                        timeSlice = 0;
                    } else if (timeSlice > totalTimeToWait) {
                        timeSlice = totalTimeToWait;
                        totalTimeToWait = 0;
                    } else {
                        totalTimeToWait -= timeSlice;
                    }

                    nextInterval = timeSlice;

                } while (nextInterval > 0);

                resetBroker(this._serverIP);
                _connection.getStatusLatch().setStatusBit(ConnectionStatus.BUSY, ConnectionStatus.LOADBALANCE);
                _connection.disconnect();

                Thread.sleep(5000);

                if (!connectRemoteServer(this._connection, nodeIp, newServerPort, false, false, true)) {
                    this._lock.unlock();
                    isReleaseLock = false;
                    tryNextServer();
                } else {
                    this._connection.getStatusLatch().setStatusBit(ConnectionStatus.CONNECTED, ConnectionStatus.BUSY);
                }
            } catch (Exception ex) {
            } finally {
                if (isReleaseLock) {
                    this._lock.unlock();
                }
            }
        }

        /**
         *
         * @param rep
         * @return
         */
        private NewHashMap fromString(String rep) {
            if (rep != null && !rep.equals("")) {
                String[] newMap = rep.split("\t");
                if (newMap.length == 4) {
                    HashMap<Integer, String> map = new HashMap<Integer, String>();
                    String[] entries = newMap[0].split("\r\n");
                    String delimiter = "\\$";
                    for (String entry : entries) {
                        String[] keyVal = entry.split(delimiter);
                        map.put(Integer.parseInt(keyVal[0]), keyVal[1]);
                    }

                    ArrayList<String> members = new ArrayList<String>();
                    entries = newMap[1].split("\r\n");
                    for (int i = 0; i < entries.length; i++) {
                        members.add(entries[i]);
                    }

                    long lastViewId = Long.valueOf(newMap[2]);

                    boolean updateMap = Boolean.valueOf(newMap[3]);

                    NewHashMap newHashMap = new NewHashMap(lastViewId, map, members, 0, updateMap);

                    return newHashMap;
                }
            }
            return null;
        }

        //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Logging ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ">
        public boolean isLoggingEnabled() {
            return _loggingEnabled;
        }

        public void setLoggingEnabled(boolean loggingEnabled) {
            this._loggingEnabled = loggingEnabled;
        }
        //</editor-fold>

        private void initializeCache(ConnectionManager connection) throws Exception {
            if (_clientId == null) {
                _clientId = (new GUID()).toString() + ":" + connection.getCurrentIP() + ":" + this.getProcessID() + ":" + Broker.SnmpAddress;
            }

            InitCommand command = new InitCommand(_cacheId, _clientId, this._licenseCode);

            Request request = new Request(false, _commandTimeout);
            request.addCommand(connection.getServerAddress(), command);

            Broker.executeRequest(request, connection, false, false);

            CommandResponse result = connection.AssureRecieve();// request.getResponse();
            result.parseResponse(cacheId);

          
            if (result != null) {
                exceptionOccured(result);
            }

            String cacheType = result.getCacheType();

            if (cacheType != null) {
                if (cacheType.equalsIgnoreCase("partitioned-server")) {
                    this._balanceNodes = false;
                } else if (cacheType.equalsIgnoreCase("local-cache")) {
                    this._balanceNodes = false;
                    this._importHashmap = false;
                } else if (cacheType.equalsIgnoreCase("replicated-server")) {
                    this._importHashmap = false;
                }
            }

//            _defaultExpiration            
            this._isPersistEnabled = result.getIsPersistenceEnabled();
            this._persistenceInterval = result.getPersistInterval();
            if (_isPersistEnabled && _persistenceManager == null) {
                _persistenceManager = new PersistenceManager(_persistenceInterval + 10);
            } else if (!_isPersistEnabled && _persistenceManager != null) {
                _persistenceManager.dispose();
                _persistenceManager = null;
            } else if (_isPersistEnabled && _persistenceManager != null) {
                _persistenceManager.StartEventDuplicationCheck();
            }

            ExpirationContract contract = new ExpirationContract();
            contract.setTypeOrdinal(result.getExpirationOrdinal());
            contract.setTimeUnitOrdinal(result.getExpirationUnitOrdinal());
            contract.setDuration(result.getExpirationDuration());
            _cache._parent.setDefaultExpiration(contract);

            if (result.getProtobufResponse().getInitCache().getIsShutDownProcessEnabled()) {
                for (com.alachisoft.tayzgrid.common.protobuf.ShutDownServerInfoProtocol.ShutDownServerInfo sInfo : result.getProtobufResponse().getInitCache().getShutDownServerInfoList()) {
                    Address blockedServer = new Address(sInfo.getServerIP(), sInfo.getPort());
                    if (!_shutdownServers.containsKey(blockedServer)) {
                        ShutDownServerInfo shutdownServer = new ShutDownServerInfo();
                        shutdownServer.setBlockInterval(sInfo.getTimeoutInterval());
                        shutdownServer.setBlockServerAddress(blockedServer);
                        shutdownServer.setUniqueBlockingId(sInfo.getUniqueKey());
                        _shutdownServers.put(blockedServer, shutdownServer);
                    }

                }
            }
        }
        //}

        private String getProcessID() {
            String processId = ManagementFactory.getRuntimeMXBean().getName();
            int index = processId.indexOf("@");
            if (index > 0) {
                return processId.substring(0, index);
            }
            return processId;
        }

        private String getComputerName() {
            try {
                return InetAddress.getLocalHost().getHostName();

            } catch (Exception e) {

            }
            return "";
        }

        public long getClientLastViewId() {
            return this._pool.getLastViewId();
        }

        public ArrayList getClientServerList() {
            return _clientConfig.getServerList();
        }

        private void initializeResponse(ConnectionManager connection, Command command) {
            synchronized (requestMutex) {
                command.getParent().initializeResponse(connection.getServerAddress());
            }
        }

   
       

        public boolean getKeysDistributionMap(Object[] keys, CacheItem[] items, HashMap<Address, Entry<Object[], CacheItem[]>> keysDistributionMap) {
            boolean result = _importHashmap;
            boolean itemsAvailable = items != null;

            if (result) {
                HashMap<Address, HashMap> keysDistributionList = new HashMap<Address, HashMap>();
                HashMap keysAndItems = null;
                Object key = "";
                CacheItem item = null;

                for (int i = 0; i < keys.length; i++) {
                    key = keys[i];
                    if (itemsAvailable) {
                        item = items[i];
                    }

                    Address address;

                    synchronized (_hashmapUpdateMutex) {
                        address = _pool.getIp(key);
                    }
                    if (keysDistributionList.containsKey(address)) {
                        keysAndItems = keysDistributionList.get(address);
                        keysAndItems.put(key, item);
                    } else {
                        keysAndItems = new HashMap();
                        keysAndItems.put(key, item);
                        keysDistributionList.put(address, keysAndItems);
                    }
                }

                Entry<Object[], CacheItem[]> tmp;

                Address serverAddress;

                for (Map.Entry<Address, HashMap> pair : keysDistributionList.entrySet()) {
                    int index = 0;
                    serverAddress = pair.getKey();
                    keysAndItems = pair.getValue();

                    Object[] distributedKeys = new Object[keysAndItems.size()];
                    CacheItem[] distributedItems = null;
                    if (itemsAvailable) {
                        distributedItems = new CacheItem[keysAndItems.size()];
                    }

                    Iterator ide = keysAndItems.entrySet().iterator();
                    while (ide.hasNext()) {
                        Map.Entry entry = (Map.Entry) ide.next();
                        distributedKeys[index] = entry.getKey();
                        if (itemsAvailable) {
                            distributedItems[index] = (CacheItem) entry.getValue();
                        }
                        index++;
                    }

                    tmp = new Entry<Object[], CacheItem[]>(distributedKeys, distributedItems);
                    keysDistributionMap.put(serverAddress, tmp);
                }
            }

            return result;
        }

        //Convert return type To Address
        Address getConnectionIP(Command command) throws GeneralFailureException,  UnknownHostException {
            ConnectionManager connection = this._connection;
            if (command.getKey() != null && this._importHashmap) {
                Address ip;
                synchronized (_hashmapUpdateMutex) {
                    ip = this._pool.getIp(command.getKey());
                }

                if (ip != null) {

                    try {
                        connection = getConnection(ip, true);
                    } catch (ConnectionException ex) {
                        Logger log = Logger.getLogger("com.alachisoft");
                        if (log.isLoggable(Level.SEVERE)) {
                            log.log(Level.SEVERE, ex.toString());
                        }
                    } catch (ConfigurationException ex) {
                        throw new GeneralFailureException(ex.getMessage());
                    }

                    if (connection == null || (connection != null && !connection.isConnected())) {
                        try {
                            connection = this.tryPool();
                        } catch (ConnectionException ex) {
                            Logger log = Logger.getLogger("com.alachisoft");
                            if (log.isLoggable(Level.SEVERE)) {
                                log.log(Level.SEVERE, ex.toString());
                            }
                        } catch (ConfigurationException ex) {
                            throw new GeneralFailureException(ex.getMessage());
                        }
                    }
                }
            }
            return connection.getServerAddress();
        }

        public Request createRequest(Command command) throws GeneralFailureException,  OperationFailedException, ConnectionException {
            Request request = null;

            switch (command.getCommandType()) {
                case GET_GROUP:
                case GET_TAG:
                case REMOVE_GROUP:
                case REMOVE_BY_TAG:
                case SEARCH:
                case GET_KEYS_TAG:
                case GET_NEXT_CHUNK:
                case GETGROUP_NEXT_CHUNK:
                case DELETEQUERY:
                case GET_TASK_ENUMERATOR:
                    if (_importHashmap) {
                        if (poolFullyDisConnected()) {
                            throw new OperationFailedException("No server is available to process the request");
                        }

                        if (poolFullyConnected()) {
                            request = new Request(true, Broker._commandTimeout);
                            Address[] servers = _pool.getServers();
                            for (Address server : servers) {
                                request.addCommand(server, command);
                            }
                            command.setClientLastViewId(this.getClientLastViewId());
                        } else {
                            request = createDedicatedRequest(command);
                        }
                    } else {
                        request = new Request(false, Broker._commandTimeout);
                        request.addCommand(_connection.getServerAddress(), command);
                    }
                    request.setIsAsync(command.isAsync);
                    request.setIsAsyncCallbackSpecified(command.asyncCallbackSpecified);

                    break;

                default:
                    request = new Request(false, Broker._commandTimeout);
                    request.setIsAsync(command.isAsync);
                    request.setIsAsyncCallbackSpecified(command.asyncCallbackSpecified);
                    Address ipAddress = new Address();
                    try {
                        ipAddress = getConnectionIP(command); //Convert ipAddress To Address
                    } catch (java.net.UnknownHostException ex) {
                        throw new ConnectionException(ex.getMessage());
                    }
                    request.addCommand(ipAddress, command);
                    break;
            }

            return request;
        }

        public boolean poolFullyDisConnected() {
            synchronized (_hashmapUpdateMutex) {
                return _pool.isFullyDisConnected();
            }
        }

        public boolean poolFullyConnected() {
            synchronized (_hashmapUpdateMutex) {
                if (_shutdownServers.size() > 1) {
                    return false;
                }
                return _pool.isFullyConnected();
            }
        }

        public Request createRequestOnServer(String nodeAddress, Command command) throws OperationFailedException {
            Request request = null;
            request = new Request(true, Broker._commandTimeout);
            ConnectionManager conn = null;
            synchronized (_hashmapUpdateMutex) {
                conn = _pool.getConnectionForKey(nodeAddress);
            }
            if (conn != null) {
                request.addCommand(conn.getServerAddress(), command);
            } else {
                return createDedicatedRequest(command);
            }
            return request;
        }

        public Request createDedicatedRequest(Command command) throws OperationFailedException {
            Request request = null;
            if (_importHashmap) {
                request = new Request(true, Broker._commandTimeout);
                command.setClientLastViewId(forcedViewId);
                ConnectionManager conn = null;
                synchronized (_hashmapUpdateMutex) {
                    conn = _pool.GetAnyConnection();
                }
                if (conn != null) {
                    request.addCommand(conn.getServerAddress(), command);
                } else {
                    throw new OperationFailedException("No server is available to process the request");
                }
            } else {
                request = new Request(false, Broker._commandTimeout);
                command.setClientLastViewId(forcedViewId);
                request.addCommand(_connection.getServerAddress(), command);
            }
            request.setIsAsync(command.isAsync);
            request.setIsAsyncCallbackSpecified(command.asyncCallbackSpecified);

            return request;
        }

        private void addRequestToRequestTable(Request request) {
            if (!(request.isAsync() && !request.isAsyncCallbackSpecified())) {
                request.setRequestId(this.getRequestId());
                synchronized (requestMutex) {
                    requestTable.put(request.getRequestId(), request);
                    if (_perfStatsCollector != null) {
                        _perfStatsCollector.incrementRequestQueueSize();
                    }
                }
            }
        }

        public void executeRequest(Request request, ConnectionManager connection, boolean checkConnected, boolean waitForResponse) throws Exception {
            //1. Add request to request table

            if (_shutdownServers.size() > 0) {
                if (request.getRequestTimeout() == _commandTimeout) {
                    request.setRequestTimeout(request.getRequestTimeout() + _shutdownTimeout);
                }
            }

            if (waitForResponse) {
                addRequestToRequestTable(request);
            }

            //2. send each command. This method not only takes care of the specific connection for
            //sending command, it also intializes the response for the connection.
            if (!request.IsBulk()) {
                for (Command command : request.getCommands().values()) {

                    try {
                        sendCommand(connection, command, checkConnected);
                    } catch (ActivityBlockedException ex) {
                        //should check for request type or not?
                        //if (command.CommandRequestType == RequestType.AtomicRead || command.CommandRequestType == RequestType.AtomicWrite)
                        request.setReRoutedAddress(ex.getBlockedServerIp());
                        sendCommand(command);
                    }
                }
            }

            if (waitForResponse) {
                boolean reacquiredLock = true;
                int timeout = (int) request.getRequestTimeout();//this._commandTimeout;
                long startTime = (Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l;
                try {
                    synchronized (request) {
                        int completedResponses = 0;
                        while (timeout > 0) {
                            if (request.isAsync()) {
                                break;
                            }

                            if (request.allResponsesRecieved()) {
//                            synchronized (requestMutex) {
//                                requestTable.remove(request.getRequestId());
//                                if (_perfStatsCollector != null) {
//                                    _perfStatsCollector.decrementRequestQueueSize();
//                                }
//                            }
                                break;
                            }

                            timeout = /*this._commandTimeout*/ (int) request.getRequestTimeout() - (int) ((Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l - startTime);
                            reacquiredLock = Monitor.wait(request, timeout);

                            if (!reacquiredLock) {
                                if (request.getIsRequestTimeoutReset()) {
                                    timeout = (int) request.getRequestTimeout() - (int) ((Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l - startTime);
                                    if (timeout > 0) {
                                        reacquiredLock = true;
                                        reacquiredLock = Monitor.wait(request, timeout);
                                    }
                                }
                            }

                            if (!reacquiredLock && !(completedResponses < request.getNumberOfCompleteResponses()) && !request.allResponsesRecieved()) {
//                            synchronized (requestMutex) {
//                                requestTable.remove(request.getRequestId());
//                                if (_perfStatsCollector != null) {
//                                    _perfStatsCollector.decrementRequestQueueSize();
//                                }
//                            }
                                //Write("executeRequest:" + request.getTimeoutMessage());
                                if (_logger.getIsErrorLogsEnabled()) {
                                    _logger.getCacheLog().Error("Broker.SendCommand", request.getTimeoutMessage());
                                    _logger.getCacheLog().Error("Broker.ExecuteRequest->timeout", Long.toString(request.getRequestId()));
                                }
                                throw new OperationFailedException(request.getTimeoutMessage());
                            }

                            completedResponses = request.getNumberOfCompleteResponses();
                        }
                    }
                } finally {
                    synchronized (requestMutex) {
                        if (!request.isAsync() && requestTable.containsKey(request.getRequestId())) {
                            requestTable.remove(request.getRequestId());
                            if (_perfStatsCollector != null) {
                                _perfStatsCollector.decrementRequestQueueSize();
                            }
                        }
                    }
                }
            }
        }

        public void executeRequest(Request request) throws Exception {
            //1. Add request to request table

            if (_shutdownServers.size() > 0) {
                if (request.getRequestTimeout() == _commandTimeout) {
                    request.setRequestTimeout(request.getRequestTimeout() + _shutdownTimeout);
                }
            }

            addRequestToRequestTable(request);

            //2. send each command. This method not only takes care of the specific connection for
            //sending command, it also intializes the response for the connection.
            if (!request.IsBulk()) {
                for (Command command : request.getCommands().values()) {

                    try {
                        sendCommand(command);
                    } catch (ActivityBlockedException ex) {

                        request.setReRoutedAddress(ex.getBlockedServerIp());
                        sendCommand(command);

                    }
                }
            } else {
                for (Map.Entry<Address, Command> pair : request.getCommands().entrySet()) {
                    Address ip = pair.getKey();

                    Command command = pair.getValue();
                    boolean optimizeConnection = (command.getCommandType() != CommandType.GET_NEXT_CHUNK);
                    ConnectionManager connection = verifyServerConnectivity(ip, optimizeConnection);
                    if (connection == null) {
                        if (command.getCommandType() == CommandType.GET_NEXT_CHUNK) {
                            throw new OperationFailedException("Enumeration has been modified");
                        }
                    }
                    try {
                        sendCommand(connection, command, true);
                    } catch (ActivityBlockedException ex) {
                        if (command.getCommandRequestType() == RequestType.ChunkRead) {
                            throw new OperationFailedException("Enumeration has been modified");
                        } else if (command.getCommandRequestType() == RequestType.BulkWrite || request.IsDedicated(forcedViewId)) {
                            request.setReRoutedAddress(ex.getBlockedServerIp());
                            sendCommand(command);

                        } else if (command.getCommandRequestType() == RequestType.BulkRead) {

                            throw ex;
                        }
                    }
                }
            }
            boolean reacquiredLock = true;
            int timeout = (int) request.getRequestTimeout();
            long startTime = (Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l;
            try {
                synchronized (request) {
                    int completedResponses = 0;
                    while (timeout > 0) {
                        if (request.isAsync()) {
                            break;
                        }

                        if (request.allResponsesRecieved()) {
//                        synchronized (requestMutex) {
//                            requestTable.remove(request.getRequestId());
//                            if (_perfStatsCollector != null) {
//                                _perfStatsCollector.decrementRequestQueueSize();
//                            }
//                        }
                            break;
                        }

                        timeout = (int) request.getRequestTimeout() - (int) ((Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l - startTime);
                        reacquiredLock = Monitor.wait(request, timeout);
                        //request.wait(timeout);

                        if (!reacquiredLock) {
                            if (request.getIsRequestTimeoutReset()) {
                                timeout = (int) request.getRequestTimeout() - (int) ((Calendar.getInstance().getTime().getTime() - 621355968000000000l) / 10000l - startTime);
                                if (timeout > 0) {
                                    reacquiredLock = true;
                                    reacquiredLock = Monitor.wait(request, timeout);
                                }
                            }
                        }

                        if (!reacquiredLock && !(completedResponses < request.getNumberOfCompleteResponses()) && !request.allResponsesRecieved()) {
//                        synchronized (requestMutex) {
//                            requestTable.remove(request.getRequestId());
//                            if (_perfStatsCollector != null) {
//                                _perfStatsCollector.decrementRequestQueueSize();
//                            }
//                        }

                            if (_logger.getIsErrorLogsEnabled()) {
                                _logger.getCacheLog().Error("Broker.SendCommand", request.getTimeoutMessage());
                            }
                            throw new OperationFailedException(request.getTimeoutMessage());
                        }

                        completedResponses = request.getNumberOfCompleteResponses();
                    }
                }
            } finally {
                synchronized (requestMutex) {
                    if (!request.isAsync() && requestTable.containsKey(request.getRequestId())) {
                        requestTable.remove(request.getRequestId());
                        if (_perfStatsCollector != null) {
                            _perfStatsCollector.decrementRequestQueueSize();
                        }
                    }

                }
            }
        }

        public void dispose() {
            if (_logger.getIsErrorLogsEnabled()) {
                _logger.getCacheLog().Error("Broker.Dispose", "disposing...");
            }

            HashMap<Address, ConnectionManager> connections = this._pool.cloneConnectionPool();

            for (Map.Entry<Address, ConnectionManager> entry : connections.entrySet()) {
                ConnectionManager connection = entry.getValue();
                if (connection != null) {
                    if (connection.isConnected()) {
                        DisposeCommand command = new DisposeCommand(false);
                        try {
                            Request request = this.createRequest(command);
                            this.executeRequest(request, connection, true, false);

                        } catch (Exception ex) {
                            if (_logger.getIsDetailedLogsEnabled()) {
                                _logger.getCacheLog().Debug("Broker.Dispose:" + ex.toString());
                            }
                        }
                    }

                    connection.disconnect();

                    if (_logger.getIsDetailedLogsEnabled()) {
                        _logger.getCacheLog().Debug("Broker.Dispose:" + connection.getIp() + " disconnected");
                    }
                }

                this._pool.remove(connection.getServerAddress());

                _perfStatsCollector.dispose();
                if (_asyncProcessor != null) {
                    _asyncProcessor.Stop();
                    _asyncProcessor = null;
                }

                if (_eventProcessor != null) {
                    _eventProcessor.Stop();
                    _eventProcessor = null;
                }

                try {
                    if (this._newMapTask != null) {
                        this._newMapTask.destroy();
                    }
                    if (this._worker != null && !this._worker.isInterrupted() && this._worker.isAlive()) {
                        _worker.interrupt();
                        this._worker.join();
                    }
                    _newMapTask = null;
                } catch (InterruptedException ex) {
                }

                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.Dispose", connection.getIp() + " disconnected");
                }
                               
                if(_logger.getCacheLog()!=null)
                {
                    _logger.getCacheLog().Flush();
                    _logger.getCacheLog().Close();
                }
            }

        }

        /**
         *
         * @param hashMap
         */
        private void newHashmapRecieved(NewHashMap newMap) {
            if (newMap == null || newMap.getMap() == null) {
                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.NewHashmapReceived", "Hashmap is null... returning");
                }
                return;
            }

            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("newHashMapRecieved:\tnew hashmap recieved...");
            }

            try {
                synchronized (_hashmapUpdateMutex) {

                    if (_logger.getIsDetailedLogsEnabled()) {
                        _logger.getCacheLog().Debug("Broker.NewHashmapReceived", "Hashmap " + newMap.toString());
                    }
                    if (newMap.getLastViewId() == this._pool.getLastViewId()) {
                        if (_logger.getIsErrorLogsEnabled()) {
                        
                            _logger.getCacheLog().Error("Broker.NewHashmapReceived", "Hashmap is same as current pool. Pool " + this._pool.toString() + " New Hashmap "
                                + newMap.toString() + " ... returning");
                        }
                        return;
                    }
                    if (newMap.getLastViewId() < this._pool.getLastViewId()) {
                        
                        if (_logger.getIsErrorLogsEnabled()) {
                        _logger.getCacheLog().Error("Broker.NewHashmapReceived", "Hashmap is older than current pool. Pool " + this._pool.toString() + " New Hashmap "
                                + newMap.toString() + " ... returning");
                        }
                        return;
                    }

                    HashMap<Address, Address> newConnection = new HashMap<Address, Address>();
                    List<String> hashMap = newMap.getMembers();

                    for (int i = 0; i < hashMap.size(); i++) {
                        String ip = hashMap.get(i);
                        
                        int serverPort = _pool.getPort();
                        if(serverPort <0){
                            serverPort = this._port;
                        }
                        RemoteServer server = _clientConfig.getMappedServer(ip, serverPort);
                        ip = server.getName();
                        serverPort = server.getPort();

                        Address addr = new Address(ip, serverPort);

                        if (!newConnection.containsKey(addr)) {
                            newConnection.put(addr, addr);
                        }
                        if (!this._pool.contains(addr)) {
                            if (_logger.getIsDetailedLogsEnabled()) {
                                _logger.getCacheLog().Debug("newHashMapRecieved:\tConnection not in pool. ip = " + ip);
                            }

                            ConnectionManager connection = new ConnectionManager(this, _logger, _clientConfig.getBindIP());

                            Exception exception = null;
                            try {
                                isCacheBind = false;
                                boolean connected = this.connectRemoteServer(connection, server.getName(), server.getPort(), false, false, false);
                                if (connected) {
                                    if (_logger.getIsDetailedLogsEnabled()) {
                                        _logger.getCacheLog().Debug("newHashmapReceived:\tConnection established, adding to pool");
                                    }
                                    
                                    if(addr.getPort() != connection.getPort()){
                                        if(newConnection.containsKey(addr)){
                                            newConnection.remove(addr);
                                            addr = connection.getServerAddress();
                                            newConnection.put(addr, addr);
                                        }
                                    }
                                    
                                    this._pool.add(connection.getServerAddress(), connection);
                                    this._clientConfig.addServer(server);
                                    if (_logger.getIsDetailedLogsEnabled()) {
                                        _logger.getCacheLog().Debug("Broker.NewHashmapRecieved", "Connection made to " + ip + " and added to pool");
                                    }
                                }
                            } catch (ConnectionException ex) {

                                if (_logger.getIsErrorLogsEnabled()) {
                                    _logger.getCacheLog().Error("Broker.NewHashmapRecieved", ex.toString());
                                }
                                exception = ex;

                            } catch (Exception ex) {

                                if (_logger.getIsErrorLogsEnabled()) {
                                    _logger.getCacheLog().Error("Broker.NewHashmapRecieved", "You do not have permissions to perform the operation on " + ip + ". " + ex.toString());
                                    _logger.getCacheLog().Error("Broker.NewHashmapRecieved", ex.toString());
                                }
                                exception = ex;
                            }

                            if (exception != null && _logger.getIsErrorLogsEnabled()) {
                                _logger.getCacheLog().Error("Broker.NewHashmapRecieved", "Could not connect to " + ip + ". " + exception.toString());
                            }
                        } else {
                            ConnectionManager connection = this._pool.getConnection(addr);
                            if (connection != null && !connection.isConnected()) {

                                if (_logger.getIsDetailedLogsEnabled()) {
                                    _logger.getCacheLog().Debug("Broker.NewHashmapRecieved", "Not connected to " + ip + " in the pool");
                                }
                                tryConnecting(connection);
                            } else {
                                if (_logger.getIsDetailedLogsEnabled()) {
                                    _logger.getCacheLog().Debug("Broker.NewHashmapRecieved", "Already connected to " + ip + " in the pool");
                                }
                            }
                        }
                    }

                    this._pool.cleanUpDisconnected(newConnection, _shutdownServers);
                    RemoteServer srvr = new RemoteServer();
                    String add = null;
                    for (int key = 0; key < newMap.getMap().size(); key++) {
                        add = (String) newMap.getMap().get(key);
                        srvr = _clientConfig.getMappedServer(add, this._port);
                        newMap.getMap().put(key, new Address(srvr.getName(), srvr.getPort()));
                    }

                    this._pool.setHashMap(newMap);
                    if (_logger.getIsDetailedLogsEnabled()) {
                        _logger.getCacheLog().Debug("Broker.NewHashmapReceived", "Hashmap applied " + newMap.toString() + " Pool " + this._pool.toString());
                    }
                }
            } catch (Exception ex) {

                if (_logger.getIsErrorLogsEnabled()) {
                    _logger.getCacheLog().Error("Broker.NewHashmapRecieved", ex.toString());
                }
            }
        }

        /**
         *
         * @param connection
         * @return
         */
        private boolean tryConnecting(ConnectionManager connection) {

            boolean connected = false;

            try {
                if (_lock.tryLock(_connectionTimeout, TimeUnit.SECONDS)) {
                    try {

                        connected = (connection != null && connection.isConnected());
                        if (!connected) {
                            checkRetryConnectionDelay(); //[KS: Checking if retry connection Interval is over or not]
                            if (!_retryConnection) {
                                return false;
                            }

                            connection.getStatusLatch().setStatusBit(ConnectionStatus.BUSY, (byte) (ConnectionStatus.CONNECTED | ConnectionStatus.DISCONNECTED));
                            connected = connectRemoteServer(connection, connection.getServer(), this._port, false, false, needsNotifRegistration());
                            if (connected) {
                                if (_logger.getIsDetailedLogsEnabled()) {
                                    _logger.getCacheLog().Info("Broker.TryConnecting", "Connection established with " + connection.getIp());
                                }
                            }
                        }
                    } catch (Exception sec) {
                        if (_logger.getIsErrorLogsEnabled()) {
                            _logger.getCacheLog().Error("Broker.TryConnecting", "You do not have permissions to perform the operation on : " + connection.getIp());
                        }
                    } finally {
                        byte toSet = connected ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED;
                        byte toUnset = (byte) ((!connected ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED) | ConnectionStatus.BUSY);
                        connection.getStatusLatch().setStatusBit(toSet, toUnset);

                        _retryConnection = connected;

                        if (_logger.getIsDetailedLogsEnabled()) {
                            _logger.getCacheLog().Debug("tryConnecting:\tUnlocking...");
                        }
                        _lock.unlock();
                    }
                }
            } catch (Exception e) {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("tryConnecting:\t" + e.getMessage());
                }
            }

            return connected;
        }

        private boolean needsNotifRegistration() {
            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("needsNotificationRegistration:");
            }

            HashMap connections = this._pool.getPool();

            Iterator<ConnectionManager> enu = connections.values().iterator();

            while (enu.hasNext()) {
                ConnectionManager connection = enu.next();
                if (connection.isConnected()) {
                    if (_logger.getIsDetailedLogsEnabled()) {
                        _logger.getCacheLog().Debug("needsNotificationRegistration:\t return false");
                    }
                    return false;
                }
            }

            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("needsNotificationRegistration:\t return true");
            }
            return true;
        }

        /**
         *
         * @return
         */
        @SuppressWarnings("static-access")
        private ConnectionManager tryPool() throws ConfigurationException,  ConnectionException, UnknownHostException {
            ConnectionManager connection = null;
            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("tryePool:");
            }
            if (this._clientConfig == null) {
                try {
                    this._clientConfig = new ClientConfiguration(this._cacheId);
                } catch (ConfigurationException ex) {
                    Logger.getLogger("com.alachisoft").log(Level.SEVERE, ex.toString());
                }
            }
            int retries = 3;
            while (true) {
                try {
                    this._clientConfig.LoadConfiguration();
                    break;
                } catch (ConfigurationException ex) {
                    if (--retries == 0) {
                        throw ex;
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex1) {

                    }
                }
            }

            RemoteServer nextServer = this._clientConfig.getNextServer();
            RemoteServer startingServer = nextServer;

            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("tryPool:\t Checking servers");
            }
            while (true) {
                if (this._clientConfig.getServerCount() > 0) {
                    connection = this._pool.getConnection(new Address(nextServer.getName(), nextServer.getPort()));
                    if (connection != null && connection.isConnected()) {
                        if (_logger.getIsDetailedLogsEnabled()) {
                            _logger.getCacheLog().Debug("tryPool:\t A connections is connected");
                        }
                        break;
                    } else {
                        nextServer = this._clientConfig.getNextServer();
                        if (startingServer.equals(nextServer)) {
                            break;
                        }
                    }
                } else {
                    break;
                }
            }

            if (connection == null || (connection != null && !connection.isConnected())) {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("tryPool:\tNo connection connected, trying pool");
                }
                while (true) {
                    if (this._clientConfig.getServerCount() > 0) {
                        if (this._pool.contains(new Address(nextServer.getName(), nextServer.getPort()))) {
                            connection = this._pool.getConnection(new Address(nextServer.getName(), nextServer.getPort()));
                        } else {
                            connection = new ConnectionManager(this, _logger, _clientConfig.getBindIP());
                        }

                        if (!connection.isConnected()) {
                            connection = reconnectServer(connection, nextServer);

                            if (connection.isConnected()) {
                                if (!this._pool.contains(new Address(nextServer.getName(), nextServer.getPort()))) {
                                    this._pool.add(connection.getServerAddress(), connection);
                                }
                                break;
                            } else {
                                nextServer = this._clientConfig.getNextServer();
                                if (startingServer.equals(nextServer)) {
                                    break;
                                }
                            }
                        }
                    } else {
                        break;
                    }
                }
            }
            if (connection == null) {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("tryPool:\tconnection is null");
                }
            } else {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("tryPool:\tconnection connected? " + connection.isConnected());
                }
            }

            return connection;
        }

        /**
         *
         * @param connection
         * @param server
         * @return
         */
        @SuppressWarnings("static-access")
        private ConnectionManager reconnectServer(ConnectionManager connection, RemoteServer server)
                throws ConnectionException,  java.net.UnknownHostException {

            if (_logger.getIsDetailedLogsEnabled()) {
                _logger.getCacheLog().Debug("reconnectServer:\t");
            }
            boolean connected = false;
            int retries = this._connectionRetries;
            ArrayList<String> deniedServers = new ArrayList<String>();
            ConnectionException exc = null;

            try {
                _lock.tryLock(_connectionMutexTimeout, TimeUnit.SECONDS);
            } catch (Exception e) {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("reconnectServer:\tFailed to acquire lock");
                }
                return connection; //lock could not be granted before the timeout expires.
            }

            try {
                checkRetryConnectionDelay(); //[KS: Checking if retry connection Interval is over or not]
                if (!_retryConnection) {
                    return connection;
                }

                while (retries-- > 0) {
                    connection.getStatusLatch().setStatusBit(ConnectionStatus.BUSY, (byte) (ConnectionStatus.CONNECTED | ConnectionStatus.DISCONNECTED));

                    if (server == null) {
                        break;
                    }
                    if (server.getName() != null) {
                        for (int i = 0; i < server.getPortRange(); i++) {
                            try {
                                if (!connected) {
                                    connected = this.connectRemoteServer(connection, server.getName(), server.getPort() + i, this._balanceNodes, this._importHashmap, true);
                                }
                                if (connected) {
                                    if (_logger.getIsDetailedLogsEnabled()) {
                                        _logger.getCacheLog().Debug("reconnectServer:\tconnection connected");
                                    }
                                    break;
                                }
                            } catch (ConnectionException ex) {
                                exc = ex;
                            }
                            catch (Exception ex) {
                                if (!deniedServers.contains(server.getName())) {
                                    deniedServers.add(server.getName());
                                }
                            }
                        }
                        try {
                            Thread.sleep(this._retryInterval);
                        } catch (InterruptedException ex) {

                        }
                    }
                    if (connection.isConnected()) {
                        connected = true;
                        break;
                    }
                }
            } finally {
                this._lock.unlock();
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("reconnectServer:\tunlocked");
                }

                byte setStatus = connected ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED;
                byte unsetStatus = (byte) ((!connected ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED) | ConnectionStatus.BUSY);
                connection.getStatusLatch().setStatusBit(setStatus, unsetStatus);

                _retryConnection = connected; //[KS : Connection is up again so we can retry ]

                if (!connected) {
                    if (deniedServers.size() > 0) {
                        StringBuilder str = new StringBuilder("You do not have permissions to perform the operation on : ");
                        Iterator<String> iter = deniedServers.iterator();
                        while (true) {
                            str.append("\'" + iter.next() + "\'");
                            if (iter.hasNext()) {
                                str.append(", ");
                            } else {
                                break;
                            }
                        }
                        throw new ConnectionException(str.toString());
                    }

                    if (exc != null) {
                        throw exc;
                    }
                }
            }
            if (connection == null) {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("reconnectServer:\tconnection is null");
                }
            } else {
                if (_logger.getIsDetailedLogsEnabled()) {
                    _logger.getCacheLog().Debug("reconnectServer:\tconnection connected? " + connection.isConnected());
                }
            }
            return connection;
        }

        private void registerClusterNotifs() {
        }

        public boolean isConnected() {
            return _connection != null ? _connection.isConnected() : false;
        }

        private long getRequestId() {
            synchronized (_requestIdMutex) {
                return req_id++;
            }
        }

        private CommandResponse getOptimalServer(ConnectionManager connection) throws Exception {
            CommandResponse nodeBalanceResult = null;

            GetOptimalServer command = new GetOptimalServer(_cacheId);

            Request request = new Request(false, Broker._commandTimeout);
            request.addCommand(connection.getServerAddress(), command);

            try {
                Broker.executeRequest(request, connection, false, false);
                nodeBalanceResult = connection.AssureRecieve();//request.getResponse();
                nodeBalanceResult.parseResponse(cacheId);

                if (nodeBalanceResult != null) {
                    exceptionOccured(nodeBalanceResult);
                }

            }catch (Exception ex) {

            }
            return nodeBalanceResult;
        }

        private CommandResponse getCacheBinding(ConnectionManager connection){
            CommandResponse cacheBindingResult = null;

            GetCacheBinding command = new GetCacheBinding(_cacheId);

            Request request = new Request(false, Broker._commandTimeout);
            request.addCommand(connection.getServerAddress(), command);

            try {
                Broker.executeRequest(request, connection, false, false);
                cacheBindingResult = connection.AssureRecieve();//request.getResponse();
                cacheBindingResult.parseResponse(_cacheId);

                if (cacheBindingResult != null) {
                    exceptionOccured(cacheBindingResult);
                }

            } catch (Exception ex) {

            }
            return cacheBindingResult;
        }

        private void getTypeInfoMap(ConnectionManager connection, boolean b) throws CacheException, java.net.UnknownHostException {
            GetTypeInfoMap command = new GetTypeInfoMap(b);
            Request request = Broker.createRequest(command);
            try {
                Broker.executeRequest(request);
            } catch (Exception ex) {
                throw new OperationFailedException(ex.getMessage());
            }

            CommandResponse response = request.getResponse();
            exceptionOccured(response);
            typeMap = response.getTypeMap();
        }

        public boolean poolHasAllServers() {
            return _clientConfig.getServerCount() == _pool.getServers().length;
        }

       

        private int getTimeOut() {
            try {
                String timeout = System.getProperty("jvcclienttimeout", "20");
                return Integer.parseInt(timeout);
            } catch (Exception e) {
            }
            return 20;
        }

        private int hashCode(String string) {

            return -1;

        }

        private class UpdatePoolExecutor implements Runnable {

            private CommandResponse res;
            private boolean queue;

            public UpdatePoolExecutor(CommandResponse res, boolean queue) {
                this.res = res;
                this.queue = queue;
            }

            public void run() {
                updatePool(res, queue);
            }
        }

        private class BalanceNodeExecutor implements Runnable {

            public void run() {
                startBalancingClients();
            }
        }

        private class RaiseCustomEvent implements Runnable {

            private byte[] notifID;
            private byte[] val;

            public RaiseCustomEvent(byte[] notifID, byte[] val) {
                this.notifID = notifID;
                this.val = val;
            }

            public void run() {
                notifyCustomEvent(notifID, val);
            }
        }

    }

    @Override
    protected void finalize() throws Throwable {

        this.Broker = null;
        _cacheCleared.clear();
        _customListener.clear();
        super.finalize();
    }

    private HashMap getQueryInfo(Object value) throws GeneralFailureException {

        HashMap queryInfo = null;

        if (this.typeMap == null) {
            return null;
        }

        try {
            int handleId = typeMap.getHandleId(value.getClass().getCanonicalName());
            if (handleId != -1) {
                queryInfo = new HashMap();
                ArrayList attribValues = new ArrayList();
                ArrayList attributes = typeMap.getAttribList(handleId);

                for (int i = 0; i < attributes.size(); i++) {
                    Field fieldAttrib = value.getClass().getField((String) attributes.get(i));
                    if (fieldAttrib != null) {
                        Object attribValue = fieldAttrib.get(value);

                        attribValues.add(attribValue);
                    } else {
                        throw new Exception("Unable to extract query information from user object.");
                    }

                }
                queryInfo.put(handleId, attribValues);
            }

        } catch (Exception e) {
            throw new GeneralFailureException("Unable to extract query information from user object.");
        }

        return queryInfo;
    }

    private HashMap getTagInfo(Object value, Tag[] tags, ArrayList<Tag> validTags) throws ArgumentNullException {
        if (tags == null) {
            return null;
        }

        HashMap tagInfo = new HashMap();
        ArrayList tagsList = new ArrayList();
        for (Tag tag : tags) {
            if (tag == null) {
                throw new ArgumentNullException("Value cannot be null.\r\nParameter name: Tag");
            } else if (tag.getTagName() != null) {
                tagsList.add(tag.getTagName());
                validTags.add(tag);
            }
        }

        String typeName = value.getClass().getName();
        typeName = typeName.replace("+", ".");

        tagInfo.put("type", typeName);
        tagInfo.put("tags-list", tagsList);

        return tagInfo;
    }

    private HashMap getNamedTagsInfo(Object value, NamedTagsDictionary namedTags) throws NullPointerException, ArgumentException {
        if (value == null || namedTags == null || namedTags.getCount() == 0) {
            return null;
        }

        CheckDuplicateIndexName(value, namedTags);

        HashMap tagInfo = new HashMap();
        HashMap tagsList = new HashMap();

        Iterator iterator = namedTags.getKeysIterator();

        while (iterator.hasNext()) {
            String key = iterator.next().toString();
            Object val = namedTags.getValue(key);

            if (val == null) {
                throw new ArgumentNullException("Named Tag value cannot be null");
            }

            tagsList.put(key, val);
        }

        String typeName = value.getClass().getCanonicalName();
        typeName = typeName.replace("+", ".");

        tagInfo.put("type", typeName);
        tagInfo.put("named-tags-list", tagsList);

        return tagInfo;
    }

    private void CheckDuplicateIndexName(Object value, NamedTagsDictionary namedTags) throws ArgumentException {
        if (typeMap == null) {
            return;
        }

        int handleId = typeMap.getHandleId(value.getClass().getCanonicalName());
        if (handleId != -1) {
            ArrayList attributes = this.typeMap.getAttribList(handleId);
            for (int i = 0; i < attributes.size(); i++) {
                if (namedTags.contains(attributes.get(i).toString())) //@UH whether this should be case insensitive
                {
                    throw new ArgumentException("Key in named tags conflicts with the indexed attribute name of the specified object.");
                }
            }
        }
    }

    private void InitializeLogging(String fileName, Level logLevel) throws GeneralFailureException {
        if (logLevel == Level.OFF) {
            Logger logger = Logger.getLogger("com.alachisoft");
            logger.setLevel(logLevel);
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            // Create a file handler that uses 3 logfiles, each with a limit of 1Mbyte
            String pattern = fileName + "." + sdf.format(new java.util.Date()) + ".log-%g.htm";
            String path = null;
            String separator = System.getProperty("file.separator");
            int limit = 1000000; // 1 Mb
            int numLogFiles = 3;
            if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
                //<editor-fold defaultstate="collapsed" desc=" Library Path for Windows ">
                path = System.getenv("NCHome");
                if (path != null) {
                    if (!path.endsWith(separator)) {
                        path = path.concat(separator);
                    }
                }
                pattern = path.concat(ServicePropValues.LOGS_FOLDER + separator + "Java" + separator + pattern);
                FileHandler fh = new FileHandler(pattern, limit, numLogFiles);

                // Add to logger
                Logger logger = Logger.getLogger("com.alachisoft");
                fh.setLevel(logLevel);
                logger.addHandler(fh);
                // Root logger has the console out put Hanler attached by default
                // to disable we can either remove Console handler of the root logger by getting all root loggers
                // or simply setUseParentHandlers to false to disable passing of logs to the root logger
                logger.setUseParentHandlers(false);
                logger.setLevel(Level.OFF);
            } else {
                path = System.getenv("NCACHE_ROOT");
                if (path != null && path.equalsIgnoreCase("") == false) {
                    path = path.concat(separator + pattern);
                } else {
                    path = System.getenv("NCACHE_MEMCACHED_ROOT");
                    if (path != null && path.equalsIgnoreCase("") == false) {
                        path = path.concat(separator + pattern);
                    }

                }
                if (path == null) {
                    path = "/usr/local/ncache/";
                }
                //<editor-fold defaultstate="collapsed" desc=" Library Path for linux ">
                if (!path.endsWith(separator)) {
                    path = path.concat(separator);
                }
                pattern = path.concat("logs" + separator + pattern);
                FileHandler fh = new FileHandler(pattern, limit, numLogFiles);

                // Add to logger
                Logger logger = Logger.getLogger("com.alachisoft");
                fh.setLevel(logLevel);
                logger.addHandler(fh);
                logger.setLevel(logLevel);
                // Root logger has the console out put Hanler attached by default
                // to disable we can either remove Console handler of the root logger by getting all root loggers
                // or simply setUseParentHandlers to false to disable passing of logs to the root logger
                logger.setUseParentHandlers(false);
                logger.setLevel(Level.OFF);
            }
        } catch (IOException e) {
            throw new GeneralFailureException(e.getMessage());
        } catch (Exception e) {
        }

    }

    // <editor-fold defaultstate="collapsed" desc="Serialization Compact Framework">
    /**
     *
     */
    FormatterServices impl;

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Entry Processor Code">  
    /**
     *
     * @param keys
     * @param entryProcessor
     * @param defaultReadThru
     * @param defaultWriteThru
     * @param arguments
     * @return
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException
     * @throws
     * com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.SecurityException
     * @throws com.alachisoft.tayzgrid.runtime.exceptions.AggregateException
     */
    @Override
    public java.util.Map<Object, EntryProcessorResult> invokeEntryProcessor(Object[] keys,
            com.alachisoft.tayzgrid.runtime.processor.EntryProcessor entryProcessor, String defaultReadThru, String defaultWriteThru,
            Object... arguments) throws GeneralFailureException, OperationFailedException,  ConnectionException, AggregateException {
        List<byte[]> argumentList = null;
        byte[] ep = null;

        Long size = Long.MIN_VALUE;
        RefObject<Long> refSize = new RefObject<Long>(size);
        BitSet flag = new BitSet();

        if (entryProcessor != null) {
            ep = (byte[]) SafeSerialize(entryProcessor, this.getName(), flag, this, refSize);
        }

        if (arguments != null && arguments.length > 0) {
            argumentList = new ArrayList<byte[]>();
            for (Object obj : arguments) {
                argumentList.add((byte[]) SafeSerialize(obj, this.getName(), flag, this, refSize));
            }
        }

        HashMap<Address, Entry<Object[], CacheItem[]>> keysDistributionMap = new HashMap<Address, Entry<Object[], CacheItem[]>>();
        Request request;

        if (Broker.importHashmap() && Broker.poolFullyConnected()) {
            request = new Request(true, Broker._commandTimeout);
            Broker.getKeysDistributionMap(keys, null, keysDistributionMap);
            for (Address serverAddress : keysDistributionMap.keySet()) {
                Entry<Object[], CacheItem[]> keysAndItems = keysDistributionMap.get(serverAddress);
                InvokeEntryProcessorCommand command = new InvokeEntryProcessorCommand(keysAndItems.getKey(), ep, argumentList, defaultReadThru, defaultWriteThru);
                command.setClientLastViewId(Broker.getClientLastViewId());
                //command.setIntendedRecipient(serverAddress.);
                request.addCommand(serverAddress, command);
            }
        } else {
            InvokeEntryProcessorCommand command = new InvokeEntryProcessorCommand(keys, ep, argumentList, defaultReadThru, defaultWriteThru);
            request = Broker.createRequest(command);
        }
        CommandResponse response = null;
        try {

            Broker.executeRequest(request);
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        response = request.getResponse();

        if (this.isExceptionsEnabled()) {
            exceptionOccured(response);
        }

        return (HashMap) response.getResultMap();
    }

    // </editor-fold>
    private final class ReconnectTask implements AsyncProcessor.IAsyncTask {

        private short _retries = 3;
        private ConnectionManager _connection;
        private Broker _parent;

        public ReconnectTask(Broker parent, ConnectionManager connection) {
            //System.out.println("Reconnecting : " + connection.getServer());
            connection.setReconnecting(true);
            this._parent = parent;
            this._connection = connection;
        }

        public void Process() {
            try {
                //return;
                if (this._connection == null) {
                    return;
                }
                if (this._connection.isConnected()) {
                    return;
                }

                while (_retries-- > 0) {

                    Thread.sleep(2000);//waite for 2 seconds before retrying

                    try {
                        Exception exception = null;

                        if (this._parent.tryConnecting(this._connection)) {
                            break;
                        }

                        if (exception != null) {
                            if (exception.getMessage().startsWith("System.Exception: Cache is not registered")) {
                                ConnectionManager connection = this._parent.tryPool();
                                if (connection != null && connection.isConnected()) {
                                    this._parent.getHashmap(connection);
                                    if (this._parent.getLogger().getIsErrorLogsEnabled()) {
                                        this._parent.getLogger().getCacheLog().Error("ReconnectTask.Process", "Connection [" + this._connection.getIp() + "] Exception->"
                                                + exception.toString());
                                    }
                                }
                                break;
                            }
                            //Cache might be restarted
                            if (exception.getMessage().startsWith("System.Exception: Cache is not running") && _retries == 0) // then wait till the retries
                            {
                                ConnectionManager connection = this._parent.tryPool();
                                if (connection != null && connection.isConnected()) {
                                    this._parent.getHashmap(connection);
                                    if (this._parent.getLogger().getIsErrorLogsEnabled()) {
                                        this._parent.getLogger().getCacheLog().Error("ReconnectTask.Process", "Connection [" + this._connection.getIp() + "] Exception->"
                                                + exception.toString());
                                    }
                                }
                                break;
                            }

                            if (this._parent.getLogger().getIsErrorLogsEnabled()) {
                                this._parent.getLogger().getCacheLog().Error("ReconnectTask.Process", "Connection [" + this._connection.getIp() + "] Exception "
                                        + exception.toString());
                            }
                        }
                    } catch (Exception e) {
                        if (_parent.getLogger().getIsErrorLogsEnabled()) {
                            _parent.getLogger().getCacheLog().Error("ReconnectTask.Process", e.toString());
                        }
                        break;
                    }
                }
            } catch (InterruptedException inter) {
                if (_parent.getLogger().getIsErrorLogsEnabled()) {
                    _parent.getLogger().getCacheLog().Error("ReconnectTask.Process", inter.toString());
                }
            } finally {
                if (this._connection != null) {
                    this._connection.setReconnecting(false);
                }
            }

        }
    }

    private final class SynchronizeEventsTask implements AsyncProcessor.IAsyncTask {

        private ConnectionManager _connection;
        private Broker _parent;

        public SynchronizeEventsTask(Broker parent, ConnectionManager connection) {
            this._parent = parent;
            this._connection = connection;
        }

        public void Process() {
            try {
                _parent.SynchronizeEvents(_connection);
            } catch (Exception e) {
                if (_parent.getLogger().getIsErrorLogsEnabled()) {
                    _parent.getLogger().getCacheLog().Error("SynchronizeEventsTask.Process", e.toString());
                }
            }
        }
    }

    @Override
    public TaskEnumeratorResult getNextRecord(String serverAddress, TaskEnumeratorPointer pointer) throws OperationFailedException {
        CommandResponse res = null;
        GetNextRecordCommand getNextRecordCommand = new GetNextRecordCommand(pointer.getClientId(), pointer.getTaskID(), pointer.getCallbackID(), pointer.getClientAddress(), pointer.getClusterAddress());
        try {
            getNextRecordCommand.setClientLastViewId(Broker.getClientLastViewId());
            Request request = Broker.createRequestOnServer(serverAddress, getNextRecordCommand);
            Broker.executeRequest(request);
            res = request.getResponse();
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
        try {
            res.parseResponse(cacheId);
            exceptionOccured(res);
            return res.getEnumeratorResultSet();
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }
    }

    @Override
    public void dispose(String serverAddress) {
        throw new UnsupportedOperationException("Operation not supported yet.");
    }

    @Override
    public TaskEnumerator getTaskEnumerator(String taskId, short callbackId) throws OperationFailedException {
        CommandResponse res = null;
        try {
            Command getTaskEnumeratorCommand = new GetTaskEnumeratorCommand(taskId, callbackId);
            Request request = Broker.createRequest(getTaskEnumeratorCommand);
            Broker.executeRequest(request);
            res = request.getResponse();
        } catch (Exception ex) {
            throw new OperationFailedException(ex.getMessage());
        }

        if (res != null) {
            if (this.isExceptionsEnabled()) {
                try {
                    exceptionOccured(res);
                } catch (Exception ex) {
                    throw new OperationFailedException(ex.getMessage());
                }
            }
        }
        ArrayList<TaskEnumeratorResult> enumeratorResultSet = res.getTaskEnumerator();
        TaskEnumerator mrResultEnumerator = new TaskEnumerator(enumeratorResultSet, this);
        return mrResultEnumerator; //to be implemented
    }
}
