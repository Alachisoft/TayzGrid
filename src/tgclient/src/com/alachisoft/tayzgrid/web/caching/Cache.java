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

//<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~ com.sun.* ~~~~~~~~~~~~~~~~~~~~~~~~">
import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.CompressedValueEntry;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import static com.alachisoft.tayzgrid.caching.queries.QueryType.AggregateFunction;
import static com.alachisoft.tayzgrid.caching.queries.QueryType.SearchEntries;
import static com.alachisoft.tayzgrid.caching.queries.QueryType.SearchKeys;
import com.alachisoft.tayzgrid.command.AddCommand;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.DeleteParams;
import com.alachisoft.tayzgrid.common.InsertParams;
import com.alachisoft.tayzgrid.common.InsertResult;
import com.alachisoft.tayzgrid.common.ResourcePool;
import com.alachisoft.tayzgrid.common.ServicePropValues;
import com.alachisoft.tayzgrid.common.caching.expiration.ExpirationContract;
import com.alachisoft.tayzgrid.common.caching.expiration.ExpirationType;

import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;
import com.alachisoft.tayzgrid.common.enums.AggregateFunctionType;
import com.alachisoft.tayzgrid.common.queries.AverageResult;
import com.alachisoft.tayzgrid.common.stats.UsageStats;
import com.alachisoft.tayzgrid.communication.ClientConfiguration;
import com.alachisoft.tayzgrid.event.CacheListener;
import com.alachisoft.tayzgrid.event.CacheNotificationType;
import com.alachisoft.tayzgrid.event.CacheStatusEventListener;
import com.alachisoft.tayzgrid.event.CacheStatusNotificationType;
import com.alachisoft.tayzgrid.event.CustomListener;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.caching.CacheItemAttributes;
import com.alachisoft.tayzgrid.runtime.caching.NamedTagsDictionary;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.events.EventType;
import com.alachisoft.tayzgrid.runtime.exceptions.AggregateException;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.runtime.exceptions.EntryProcessorException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.mapreduce.Filter;
import com.alachisoft.tayzgrid.runtime.mapreduce.KeyFilter;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceListener;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;
import com.alachisoft.tayzgrid.runtime.mapreduce.QueryFilter;
import com.alachisoft.tayzgrid.runtime.mapreduce.TrackableTask;
import com.alachisoft.tayzgrid.runtime.aggregation.Aggregator;
import com.alachisoft.tayzgrid.runtime.aggregation.ValueExtractor;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessorResult;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.standard.FormatterServices;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectInputStream;
import com.alachisoft.tayzgrid.serialization.util.SerializationUtil;
import com.alachisoft.tayzgrid.statistics.PerfStatsCollector;
import com.alachisoft.tayzgrid.common.CacheConfigParams;
import com.alachisoft.tayzgrid.common.util.PrimitiveDefaults;
import com.alachisoft.tayzgrid.util.DictionaryEntry;
import com.alachisoft.tayzgrid.util.HelperUtil;
import static com.alachisoft.tayzgrid.web.caching.Cache.DefaultAbsoluteExpiration;
import static com.alachisoft.tayzgrid.web.caching.Cache.DefaultSlidingExpiration;
import static com.alachisoft.tayzgrid.web.caching.Cache.NoLockingExpiration;
import static com.alachisoft.tayzgrid.web.caching.DSWriteOption.WriteBehind;
import static com.alachisoft.tayzgrid.web.caching.DSWriteOption.WriteThru;
import com.alachisoft.tayzgrid.web.caching.apilogging.DebugAPIConfigurations;
import com.alachisoft.tayzgrid.web.caching.apilogging.RuntimeAPILogItem;
import com.alachisoft.tayzgrid.web.events.CacheEventDescriptor;
import com.alachisoft.tayzgrid.web.events.EventManager;
import com.alachisoft.tayzgrid.web.mapreduce.MROutputOption;
import com.alachisoft.tayzgrid.web.mapreduce.MapReduceTaskResult;
import com.alachisoft.tayzgrid.web.aggregation.AggregatorTask;
import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.cache.CacheException;
import javax.cache.integration.CompletionListener;
import tangible.RefObject;

//</editor-fold>

/**
 * Implements the clustered cache for an application. This class cannot be inherited.
 *
 * @param <K>
 * @param <V>
 * @version 1.0
 */
public class Cache<K, V> implements Enumeration
{
    /**
     *
     * the primary instance that handles performance counters
     */
    PerfStatsCollector _perfStatsCollector;
    private HashMap<Long, RuntimeAPILogItem> _runtimeAPILogHM = new HashMap<Long, RuntimeAPILogItem>();
    
    HashMap<Long, RuntimeAPILogItem> getRuntimeAPILogHashMap()
    {
        return _runtimeAPILogHM;
    }

    private void logSizeInfo(boolean encryptionEnabled, boolean isBulk, long objectSize, long encryptedObjectSize, long compressedObjectSize, int noOfObjects)
    {
            RuntimeAPILogItem rtAPILogItem = new RuntimeAPILogItem();
            rtAPILogItem.setEncryptionEnabled(encryptionEnabled);
            rtAPILogItem.setIsBulk(isBulk);
            rtAPILogItem.setNoOfObjects(noOfObjects);
            if (noOfObjects != 0)
            {
                rtAPILogItem.setSizeOfObject(objectSize / noOfObjects);
                rtAPILogItem.setSizeOfEncryptedObject(encryptedObjectSize / noOfObjects);
                rtAPILogItem.setSizeOfCompressedObject(compressedObjectSize / noOfObjects);
            }
            _runtimeAPILogHM.put(Thread.currentThread().getId(), rtAPILogItem);
    }

     boolean isPerfStatsCollectorInitialized()
    {
        return _perfStatsCollector != null;
    }
     
             
    private ExpirationContract _defaultExpiration;
     
    //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constants & Variables ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    private String cacheId = null;
    /// <summary> Underlying implementation of TGCache. </summary>
    private CacheImplBase _cacheImpl;
    /// <summary> Reference count of the cache. </summary>
    public boolean exceptionsEnabled = true;
    private boolean cachestoppedFlag = false;
    private Level _level = Level.SEVERE;
    /* The Broker object to handle the requests. */
    /**
     * The Command Broker.
     */
    private int _compressed = BitSetConstants.Compressed;
    /*collection for the callbacks*/
    /**
     * specify no absolute expiration.
     */
    public static Date DefaultAbsoluteExpiration = null;
    /**
     * specify no sliding expiration.
     */
    public static TimeSpan DefaultSlidingExpiration = null;
    /**
     * Disable lock expiration
     */
    public static final TimeSpan NoLockingExpiration = null;
    private CacheListener[] cacheListenersList = new CacheListener[0];
    private CacheStatusEventListener[] clusterListenersList = new CacheStatusEventListener[0];
    private CustomListener[] customListenersList = new CustomListener[0];
    private com.alachisoft.tayzgrid.serialization.util.TypeInfoMap typeMap = null;
    
    private ArrayList<CacheClearedCallback> _cacheCleared = new ArrayList<CacheClearedCallback>();
    private ArrayList<CacheStoppedCallback> _cacheStoppedCallback = new ArrayList<CacheStoppedCallback>();
    private ArrayList<ICustomEvent> _customEventCallback = new ArrayList<ICustomEvent>(); 
    
    private boolean _encryptionEnabled = false;
    private boolean _customNotifRegistered;
    private int _refCounter = 0;
    private int forcedViewId = -5;
    /// <summary>Serialization context (actually name of the cache.)used for Compact Framework </summary>
    private String _serializationContext;
    private String _defaultReadThruProvider;
    private String _defaultWriteThruProvider;
    private ArrayList _secondaryInprocInstances;
    private int _refAddCount = 0;
    private int _refUpdateCount = 0;
    private int _refRemoveCount = 0;
    private int _refCustomCount = 0;
    private int _refCacheStoppedCount = 0;
    private int _refClearCount = 0;
    private int _refNodeJoinedCount = 0;
    private int _refNodeLeftCount = 0;
    private EventManager _eventManager;
    
    private ResourcePool _asyncCallbackIDsMap = new ResourcePool();
    private ResourcePool _asyncCallbacksMap = new ResourcePool();
    private short _aiacbInitialVal = 2000;
    private short _aiucbInitialVal = 3000;
    private short _aircbInitialVal = 4000;
    private short _acccbInitialVal = 5000;
    private short _dsiacbInitialVal = 6000;
    private short _dsiucbInitialVal = 7000;
    private short _dsircbInitialVal = 8000;
    private short _dsccbInitialVal = 9000;
    private short _jcclInitialVal = 10000;
    private short _mapReduceInitialVal = 6277;


    /// <summary> Contains callback ids and associated callbacks</summary>
    private ResourcePool _callbackIDsMap = new ResourcePool();

    /// <summary> Contains callbacks and associated callback ids. Actually reverse of the above pool.</summary>
    private ResourcePool _callbacksMap = new ResourcePool();
    
    private ArrayList<MemberJoinedCallback> _memberJoined = new ArrayList<MemberJoinedCallback>();
    private ArrayList<MemberLeftCallback> _memberLeft = new ArrayList<MemberLeftCallback>();
    short asyncCallbackId = -1;
    
    private ClusterEventsListener _clusterListener;
    private CacheEventsListener _cacheEventListener;
    private CacheAsyncEventsListener _cacheAsyncEventListener;
    private boolean _allowQueryTags = true;
    
    
    Cache()
    {
    }

    
    protected void setAllowQueryTags(boolean value)
    {
        this._allowQueryTags = value;
    }
    
    protected boolean getAllowQueryTags()
    {
        return this._allowQueryTags;
    }
    
    
    private short GetCallbackId(CompletionListener completionListener) {
        if (completionListener == null) {
            return -1;
        }
        
        if(_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }
        
        if(_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }
        
        while (true) {
            if(_asyncCallbacksMap.GetResource(completionListener) == null) {
                _jcclInitialVal++;
                _asyncCallbacksMap.AddResource(completionListener, _jcclInitialVal);
                _asyncCallbackIDsMap.AddResource(_jcclInitialVal, completionListener);
                return _jcclInitialVal;
            } else {
                try {
                    short clId = (Short) _asyncCallbacksMap.GetResource(completionListener);
                    _asyncCallbacksMap.AddResource(completionListener, clId);
                    _asyncCallbackIDsMap.AddResource(clId, completionListener);
                    return clId;
                }
                catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }
    //end
    private short GetCallbackId(AsyncCacheClearedCallback asyncCacheClearCallback) {
        if (asyncCacheClearCallback == null) {
            return -1;
        }


        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(asyncCacheClearCallback) == null) {
                _acccbInitialVal++;
                _asyncCallbacksMap.AddResource(asyncCacheClearCallback, _acccbInitialVal);
                _asyncCallbackIDsMap.AddResource(_acccbInitialVal, asyncCacheClearCallback);
                return _acccbInitialVal;
            } else {
                try {
                    //get existing id from the table.
                    short acccbId = (Short) _asyncCallbacksMap.GetResource(asyncCacheClearCallback);

                    //add it again for updating the ref count.
                    _asyncCallbacksMap.AddResource(asyncCacheClearCallback, acccbId);
                    _asyncCallbackIDsMap.AddResource(acccbId, asyncCacheClearCallback);
                    return acccbId;
                } catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }

    private short GetCallbackId(AsyncItemRemovedCallback asyncItemRemoveCallback) {
        if (asyncItemRemoveCallback == null) {
            return -1;
        }


        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(asyncItemRemoveCallback) == null) {
                _aircbInitialVal++;
                _asyncCallbacksMap.AddResource(asyncItemRemoveCallback, _aircbInitialVal);
                _asyncCallbackIDsMap.AddResource(_aircbInitialVal, asyncItemRemoveCallback);
                return _aircbInitialVal;
            } else {
                try {
                    //get existing id from the table.
                    short aircbId = (Short) _asyncCallbacksMap.GetResource(asyncItemRemoveCallback);

                    //add it again to update the ref count.
                    _asyncCallbacksMap.AddResource(asyncItemRemoveCallback, aircbId);
                    _asyncCallbackIDsMap.AddResource(aircbId, asyncItemRemoveCallback);
                    return aircbId;
                } catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }

    private short GetCallbackId(AsyncItemUpdatedCallback asyncItemUpdateCallback) {
        if (asyncItemUpdateCallback == null) {
            return -1;
        }


        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(asyncItemUpdateCallback) == null) {
                _aiucbInitialVal++;
                _asyncCallbacksMap.AddResource(asyncItemUpdateCallback, _aiucbInitialVal);
                _asyncCallbackIDsMap.AddResource(_aiucbInitialVal, asyncItemUpdateCallback);
                return _aiucbInitialVal;
            } else {
                try {
                    //get the existing id from the table.
                    short aiucbId = (Short)_asyncCallbacksMap.GetResource(asyncItemUpdateCallback);

                    //add it again to update the ref count.
                    _asyncCallbacksMap.AddResource(asyncItemUpdateCallback, aiucbId);
                    _asyncCallbackIDsMap.AddResource(aiucbId, asyncItemUpdateCallback);
                    return aiucbId;
                } catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }
    
    private short GetCallbackId(AsyncItemAddedCallback asyncItemAddCallback) {
        if (asyncItemAddCallback == null) {
            return -1;
        }


        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(asyncItemAddCallback) == null) {
                _aiacbInitialVal++;
                _asyncCallbacksMap.AddResource(asyncItemAddCallback, _aiacbInitialVal);
                _asyncCallbackIDsMap.AddResource(_aiacbInitialVal, asyncItemAddCallback);
                return _aiacbInitialVal;
            } else {
                try {
                    //get existing id from the table.
                    short aiacbId = (Short) _asyncCallbacksMap.GetResource(asyncItemAddCallback);

                    //add it again into the table for updating ref count.
                    _asyncCallbacksMap.AddResource(asyncItemAddCallback, aiacbId);
                    _asyncCallbackIDsMap.AddResource(aiacbId, asyncItemAddCallback);
                    return aiacbId;
                } catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }
    
    private short GetCallbackId(DataSourceItemsAddedCallback dsItemAddedCallback) {
        if (dsItemAddedCallback == null) {
            return -1;
        }

        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(dsItemAddedCallback) == null) {
                _dsiacbInitialVal++;
                _asyncCallbacksMap.AddResource(dsItemAddedCallback, _dsiacbInitialVal);
                _asyncCallbackIDsMap.AddResource(_dsiacbInitialVal, dsItemAddedCallback);
                return _dsiacbInitialVal;
            } else {
                try {
                    short dsiacb = (Short) _asyncCallbacksMap.GetResource(dsItemAddedCallback);
                    _asyncCallbacksMap.AddResource(dsItemAddedCallback, dsiacb);
                    _asyncCallbackIDsMap.AddResource(dsiacb, dsItemAddedCallback);
                    return dsiacb;
                } catch (NullPointerException ex) {
                    continue;
                }
            }
        }
    }
    
    private short GetCallbackId(DataSourceItemsAddedCallback dsItemAddedCallback, int numberOfCallbacks) {
        if (dsItemAddedCallback == null) {
            return -1;
        }

        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(dsItemAddedCallback) == null) {
                _dsiacbInitialVal++;
                _asyncCallbacksMap.AddResource(dsItemAddedCallback, _dsiacbInitialVal, numberOfCallbacks);
                _asyncCallbackIDsMap.AddResource(_dsiacbInitialVal, dsItemAddedCallback, numberOfCallbacks);
                return _dsiacbInitialVal;
            } else {
                try {
                    short dsiacb = (Short) _asyncCallbacksMap.GetResource(dsItemAddedCallback);
                    _asyncCallbacksMap.AddResource(dsItemAddedCallback, dsiacb, numberOfCallbacks);
                    _asyncCallbackIDsMap.AddResource(dsiacb, dsItemAddedCallback, numberOfCallbacks);
                    return dsiacb;
                } catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }

    private short GetCallbackId(DataSourceItemsUpdatedCallback dsItemUpdatedCallback) {
        if (dsItemUpdatedCallback == null) {
            return -1;
        }

        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(dsItemUpdatedCallback) == null) {
                _dsiucbInitialVal++;
                _asyncCallbacksMap.AddResource(dsItemUpdatedCallback, _dsiucbInitialVal);
                _asyncCallbackIDsMap.AddResource(_dsiucbInitialVal, dsItemUpdatedCallback);
                return _dsiucbInitialVal;
            } else {
                try {
                    short dsiacb = (Short) _asyncCallbacksMap.GetResource(dsItemUpdatedCallback);
                    _asyncCallbacksMap.AddResource(dsItemUpdatedCallback, dsiacb);
                    _asyncCallbackIDsMap.AddResource(dsiacb, dsItemUpdatedCallback);
                    return dsiacb;
                } catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }
    
    private short GetCallbackId(DataSourceItemsUpdatedCallback dsItemUpdatedCallback, int numberOfCallbacks) {
        if (dsItemUpdatedCallback == null) {
            return -1;
        }

        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(dsItemUpdatedCallback) == null) {
                _dsiucbInitialVal++;
                _asyncCallbacksMap.AddResource(dsItemUpdatedCallback, _dsiucbInitialVal, numberOfCallbacks);
                _asyncCallbackIDsMap.AddResource(_dsiucbInitialVal, dsItemUpdatedCallback, numberOfCallbacks);
                return _dsiucbInitialVal;
            } else {
                try {
                    short dsiacb = (Short) _asyncCallbacksMap.GetResource(dsItemUpdatedCallback);
                    _asyncCallbacksMap.AddResource(dsItemUpdatedCallback, dsiacb, numberOfCallbacks);
                    _asyncCallbackIDsMap.AddResource(dsiacb, dsItemUpdatedCallback, numberOfCallbacks);
                    return dsiacb;
                } catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }

    private short GetCallbackId(DataSourceItemsRemovedCallback dsItemRemovedCallback) {
        if (dsItemRemovedCallback == null) {
            return -1;
        }

        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(dsItemRemovedCallback) == null) {
                _dsircbInitialVal++;
                _asyncCallbacksMap.AddResource(dsItemRemovedCallback, _dsircbInitialVal);
                _asyncCallbackIDsMap.AddResource(_dsircbInitialVal, dsItemRemovedCallback);
                return _dsircbInitialVal;
            } else {
                try {
                    short dsiacb = (Short) _asyncCallbacksMap.GetResource(dsItemRemovedCallback);
                    _asyncCallbacksMap.AddResource(dsItemRemovedCallback, dsiacb);
                    _asyncCallbackIDsMap.AddResource(dsiacb, dsItemRemovedCallback);
                    return dsiacb;
                } catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }
    
    private  short GetCallbackId(DataSourceItemsRemovedCallback dsItemRemovedCallback, int numberOfCallbacks) {
        if (dsItemRemovedCallback == null) {
            return -1;
        }

        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(dsItemRemovedCallback) == null) {
                _dsircbInitialVal++;
                _asyncCallbacksMap.AddResource(dsItemRemovedCallback, _dsircbInitialVal, numberOfCallbacks);
                _asyncCallbackIDsMap.AddResource(_dsircbInitialVal, dsItemRemovedCallback, numberOfCallbacks);
                return _dsircbInitialVal;
            } else {
                try {
                    short dsiacb = (Short) _asyncCallbacksMap.GetResource(dsItemRemovedCallback);
                    _asyncCallbacksMap.AddResource(dsItemRemovedCallback, dsiacb, numberOfCallbacks);
                    _asyncCallbackIDsMap.AddResource(dsiacb, dsItemRemovedCallback, numberOfCallbacks);
                    return dsiacb;
                } catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }
    
    private short GetCallbackId(DataSourceClearedCallback dsClearedCallback) {
        if (dsClearedCallback == null) {
            return -1;
        }

        if (_asyncCallbackIDsMap == null) {
            _asyncCallbackIDsMap = new ResourcePool();
        }

        if (_asyncCallbacksMap == null) {
            _asyncCallbacksMap = new ResourcePool();
        }

        while (true) {
            if (_asyncCallbacksMap.GetResource(dsClearedCallback) == null) {
                _dsccbInitialVal++;
                _asyncCallbacksMap.AddResource(dsClearedCallback, _dsccbInitialVal);
                _asyncCallbackIDsMap.AddResource(_dsccbInitialVal, dsClearedCallback);
                return _dsccbInitialVal;
            } else {
                try {
                    short dsiacb = (Short) _asyncCallbacksMap.GetResource(dsClearedCallback);
                    _asyncCallbacksMap.AddResource(dsClearedCallback, dsiacb);
                    _asyncCallbackIDsMap.AddResource(dsiacb, dsClearedCallback);
                    return dsiacb;
                } catch (java.lang.NullPointerException ex) {
                    continue;
                }
            }
        }
    }

    protected ArrayList<ICustomEvent> getCustomEventList()
    {
        return this._customEventCallback;
    }
    
    protected void setCustomEvent(ArrayList<ICustomEvent> implClass)
    {
        this._customEventCallback = implClass;
    }
    
    protected void addToCustomEventInvocationList(ICustomEvent implClass)
    {
        if ( implClass != null)
            _customEventCallback.add(implClass);
    }
    
    protected void removeFromCustomEventInvocationList(ICustomEvent implClass)
    {
        if (implClass != null)
            _customEventCallback.remove(implClass);
    }
    
    protected CacheAsyncEventsListener getCacheAsyncEventListener()
    {
        return this._cacheAsyncEventListener;
    }
    
    protected CacheEventsListener getCacheEventListener()
    {
        return this._cacheEventListener;
    }
    
    protected ClusterEventsListener getClusterEventsListener()
    {
        return this._clusterListener;
    }
    
    protected ResourcePool getAsyncCallbackIDsMap()
    {
        return this._asyncCallbackIDsMap;
    }
    
    protected ResourcePool getAsyncCallbacksMap()
    {
        return this._asyncCallbacksMap;
    }    
    
    protected ArrayList<CacheStoppedCallback> getCacheStoppedCallbackList()
    {
        return this._cacheStoppedCallback;
    }
    
    protected void setCacheStoppedCallbackList(ArrayList<CacheStoppedCallback> callbackList)
    {
        if(callbackList != null && !callbackList.isEmpty())
            this._cacheStoppedCallback = callbackList;
    }
    
    protected void addToCacheStoppedCallbackInvocationList(CacheStoppedCallback callback)
    {
        if(callback != null)        
            this._cacheStoppedCallback.add(callback);
    }
    
    protected void removeFromCacheStoppedCallbackInvocationList(CacheStoppedCallback callback)
    {
        if(callback != null)        
            this._cacheStoppedCallback.remove(callback);
    }
    
    protected ArrayList<CacheClearedCallback> getCacheClearedCallbackList()
    {
        return this._cacheCleared;
    }
    
    protected void setCacheClearedCallbackList(ArrayList<CacheClearedCallback> callbackList)
    {
        if(callbackList != null && !callbackList.isEmpty())
            this._cacheCleared = callbackList;
    }
    
    protected void addToCacheClearedCallbackInvocationList(CacheClearedCallback callback)
    {
        if(callback != null)        
            this._cacheCleared.add(callback);
    }
    
    protected void removeFromCacheClearedCallbackInvocationList(CacheClearedCallback callback)
    {
        if(callback != null)        
            this._cacheCleared.remove(callback);
    }
    
    protected ArrayList<MemberJoinedCallback> getMemberJoinedCallbackList()
    {
        return this._memberJoined;
    }
    
    protected void setMemberJoinedCallbackList(ArrayList<MemberJoinedCallback> callbackList)
    {
        if(callbackList != null && !callbackList.isEmpty())
            this._memberJoined = callbackList;
    }
    
    protected void addToMemberJoinedCallbackInvocationList(MemberJoinedCallback callback)
    {
        if(callback != null)        
            this._memberJoined.add(callback);
    }
    
    protected void removeFromMemberJoinedInvocationList(MemberJoinedCallback callback)
    {
        if(callback != null)        
            this._memberJoined.remove(callback);
    }
        
    protected ArrayList<MemberLeftCallback> getMemberLeftCallbackList()
    {
        return this._memberLeft;
    }
    
    protected void setMemberLeftCallbackList(ArrayList<MemberLeftCallback> callbackList)
    {
        if(callbackList != null && !callbackList.isEmpty())
            this._memberLeft = callbackList;
    }
    
    protected void addToMemberLeftInvocationList(MemberLeftCallback callback)
    {
        if(callback != null)        
            this._memberLeft.add(callback);
    }    
    
    protected void removeFromMemberLeftInvocationList(MemberLeftCallback callback)
    {
        if(callback != null)        
            this._memberLeft.remove(callback);
    }
    //-Sami:[Events by Sami]
    
    String getSerializationContext()
    {
        return _serializationContext;
    }

    void setSerializationContext(String value)
    {
        _serializationContext = value;
    }
    
    EventManager getEventManager()
    {
        return this._eventManager;
    }
    
    void setEventManager(EventManager eventManager)
    {
        this._eventManager = eventManager;
    }
    
    private ResourcePool getCallbackIDsMap()
    {
        return this._callbackIDsMap;
    }
    
    private void setCallbackIDsMap(ResourcePool pool)
    {
        this._callbackIDsMap = pool;
    }
            
    private ResourcePool getCallbackMap()
    {
        return this._callbacksMap;
    }
    
    private void setCallbackMap(ResourcePool pool)
    {
        this._callbacksMap = pool;
    }


    CacheImplBase getCacheImpl()
    {
        return _cacheImpl;
    }

    void setCacheImpl(CacheImplBase value)
    {
        _cacheImpl = value;
        if (_cacheImpl != null)
        {
            _serializationContext = _cacheImpl.getName();
            cacheId = _serializationContext.toLowerCase();
        }
    }

    /**
     * Creates a new instance of Cache.
     *
     * @param objectCache Instance of cache implementation(inproc/outproc)
     * @param cacheId The cache-id to request from the server.
     */
    public Cache(CacheImplBase objectCache, String cacheId) 
    {
        _cacheImpl = objectCache;
        this.cacheId = cacheId;
        if (_cacheImpl != null) {
            _serializationContext = _cacheImpl.getName(); //Sets the serialization context.
            cacheId = _cacheImpl.getName();
        }

        _eventManager = new EventManager(cacheId, null, this);
        _cacheEventListener = new CacheEventsListener(this, _eventManager);
        _cacheAsyncEventListener = new CacheAsyncEventsListener(this);
        _clusterListener = new ClusterEventsListener(this);

        loadRWTrhuSettings();

        addRef();
    }

    /**
     * Creates a new instance of Cache.
     *
     * @param objectCache Instance of cache implementation(inproc/outproc)
     * @param cacheId The cache-id to request from the server.
     * @param perfStatsCollector Instance for publishing perfmon statistics.
     */
    public Cache(CacheImplBase objectCache, String cacheId, PerfStatsCollector perfStatsCollector) {
        _cacheImpl = objectCache;
        this.cacheId = cacheId;
        if (_cacheImpl != null) {
            _serializationContext = _cacheImpl.getName(); //Sets the serialization context.
        }

        _eventManager = new EventManager(cacheId, null, this);
        _cacheEventListener = new CacheEventsListener(this, _eventManager);
        _cacheAsyncEventListener = new CacheAsyncEventsListener(this);
        _clusterListener = new ClusterEventsListener(this);

        _perfStatsCollector = perfStatsCollector;
        loadRWTrhuSettings();
        addRef();
    }

    private void loadRWTrhuSettings()
    {
        try
        {
            ClientConfiguration config = new ClientConfiguration(this.cacheId);
            config.LoadConfiguration();
            _defaultWriteThruProvider = config.getDefaultWriteThru();
            _defaultReadThruProvider = config.getDefaultReadThru();
        }
        catch (Exception e)
        {
        }
    }
    
    Object safeSerialize(Object serializableObject, String serializationContext, BitSet flag,RefObject<Long> size) throws GeneralFailureException,OperationFailedException {
        Object serializedObject = null;

        if (_cacheImpl == null) {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (serializableObject != null) {
            UsageStats statsSerialization = new UsageStats();
            statsSerialization.BeginSample();
            serializedObject = _cacheImpl.SafeSerialize(serializableObject, serializationContext, flag, _cacheImpl,size);
            statsSerialization.EndSample();
            if (_perfStatsCollector != null) {
                _perfStatsCollector.incrementMSecPerSerializaion(statsSerialization.getCurrent());
            }
        }
        return serializedObject;
    }

    Object safeDeserialize(Object serializedObject, String serializationContext, BitSet flag) throws OperationFailedException {
        Object deSerializedObject = null;

        if (_cacheImpl == null) {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (serializedObject != null) {
            UsageStats statsSerialization = new UsageStats();
            statsSerialization.BeginSample();
            deSerializedObject = _cacheImpl.SafeDeserialize(serializedObject, serializationContext, flag, _cacheImpl);
            statsSerialization.EndSample();
            if (_perfStatsCollector != null) {
                _perfStatsCollector.incrementMSecPerSerializaion(statsSerialization.getCurrent());
            }
        }
        return deSerializedObject;
    }

    int getCompressedValue()
    {
        return _compressed;
    }
    
    void addSecondaryInprocInstance(Cache secondaryInstance)
    {
        if (_secondaryInprocInstances == null)
        {
            _secondaryInprocInstances = new java.util.ArrayList();
        }

        _secondaryInprocInstances.add(secondaryInstance);
    }
    
    
    /**
     * Keeps the number of initializations of this cache, used at dispose when all threads have called in the dispose method
     */
    synchronized void addRef()
    {
        _refCounter++;
    }

    @Override
    public String toString()
    {
        return this.cacheId;
    }

    //<editor-fold defaultstate="collapsed" desc="ExceptionEnabled">
    /**
     * If this property is set the Cache object throws exceptions from public operations. If not set no exception is thrown and the operation fails silently. Setting this
     * flag is especially helpful during development phase of application since exceptions provide more information about the specific causes of failure.
     *
     * @return true if exceptions are enabled, otherwise false.
     */
    public boolean isExceptionsEnabled()
    {
        return exceptionsEnabled;
    }

    /**
     * If this property is set the Cache object throws exceptions from public operations. If not set no exception is thrown and the operation fails silently. Setting this
     * flag is especially helpful during development phase of application since exceptions provide more information about the specific causes of failure.
     *
     * @param exceptionsEnabled boolean value to enable/disable the exceptions.
     */
    public void setExceptionsEnabled(boolean exceptionsEnabled)
    {
        this.exceptionsEnabled = exceptionsEnabled;
    }//</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Add overloads ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    /**
     * Adds an item into the Cache object with a cache key to reference its
     * location.
     *
     * @return The item version in cache
     * @param key The cache key used to reference the item.
     * @param value The item to be added to the cache.
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access
     * cache.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws ArgumentException Thrown when Operation was failed due to an
     * invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     * */
    public CacheItemVersion add(Object key, Object value)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        try
        {
            long size=0;
            RefObject<Long> tempRef_size = new RefObject<Long>(size);
            addOperation(key, value, Cache.DefaultAbsoluteExpiration, Cache.DefaultSlidingExpiration, CacheItemPriority.Default, DSWriteOption.None, null, null, null, null, false, null, null, false, null, null, null, null, EventDataFilter.None, EventDataFilter.None, tempRef_size);
            return HelperUtil.createCacheItemVersion(1);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return null;
    }

    /**
     * Add a CacheItem to the cache
     *
     * @return The item version in cache
     * @param key The cache key used to reference the item.
     * @param item CacheItem to add in the cache
     * @see CacheItem
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws SecurityException Thrown if the user is not authorized to access
     * cache.
     * @throws ArgumentException Thrown when Operation was failed due to an
     * invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during
     * configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion add(Object key, CacheItem item)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        if (item == null)
        {
            throw new IllegalArgumentException("Value cannot be null.\r\nParameter name: item");
        }

       
        boolean isResyncExpiredItems = false;
        String group = null;
        String subGroup = null;
        NamedTagsDictionary getNamedTags = null;
       
        group = item.getGroup();
        subGroup = item.getSubGroup();
        getNamedTags = item.getNamedTags();
        isResyncExpiredItems = item.getResyncExpiredItems();
        
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        addOperation(key, item.getValue(),  item.getAbsoluteExpiration(), item.getSlidingExpiration(),
                item.getPriority(), DSWriteOption.None, item.getCacheItemRemovedListener(), item.getCacheItemUpdatedListener(), null, null, 
                isResyncExpiredItems, group, subGroup, false, item.getTags(), null, item.getResyncProviderName(), getNamedTags,
                item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(), tempRef_size);
        return HelperUtil.createCacheItemVersion(1);
    }

    /**
     * Adds an item into the Cache object with a cache key to reference its location and using default values provided by the CacheItemPriority enumeration. It also enables
     * the associating tags with the object.
     *
     * @return The item version in cache
     * @param key The cache key used to reference the item.
     * @param value Value to add in the cache
     * @param tags An array of Tags to associate with the object.
     * @see Tag
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion add(Object key, Object value, Tag[] tags)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        try
        {
            long size=0;
            RefObject<Long> tempRef_size = new RefObject<Long>(size);
            addOperation(key, value, DefaultAbsoluteExpiration, DefaultSlidingExpiration, CacheItemPriority.Normal, DSWriteOption.None, null, null, null, null, false, null, null, false, tags, null, null, null, EventDataFilter.None, EventDataFilter.None,tempRef_size);
            return HelperUtil.createCacheItemVersion(1);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return null;
    }

    /**
     * Adds an item into the Cache object with a cache key to reference its location and using default values provided by the CacheItemPriority enumeration. It also enables
     * the associating tags with the object.
     *
     * @return The item version in cache
     * @param key The cache key used to reference the item.
     * @param value Value to add in the cache
     * @param namedTags to associate with object.
     * @see NamedTagsDictionary
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion add(Object key, Object value, NamedTagsDictionary namedTags)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        try
        {
            long size=0;
            RefObject<Long> tempRef_size = new RefObject<Long>(size);
            addOperation(key, value, DefaultAbsoluteExpiration, DefaultSlidingExpiration, CacheItemPriority.Normal, DSWriteOption.None, null, null, null, null, false, null, null, false, null, null, null, namedTags, EventDataFilter.None, EventDataFilter.None,tempRef_size);
            return HelperUtil.createCacheItemVersion(1);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return null;
    }

    /**
     * Adds an item in the cache.
     *
     * @return The item version in cache
     * @param key The cache key used to reference the item.
     * @param group The data group of the item
     * @param subGroup The data group of the item
     * @param value The value to be added in the cache.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion add(Object key, Object value, String group, String subGroup)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        addOperation(key, value,  Cache.DefaultAbsoluteExpiration, Cache.DefaultSlidingExpiration, CacheItemPriority.Default, DSWriteOption.None, 
                null, null, null, null, false, group, subGroup, false, null, null, null, null,
                EventDataFilter.None, EventDataFilter.None,tempRef_size);
        return HelperUtil.createCacheItemVersion(1);
    }

    /**
     * Adds the specified item to the Cache object with dependencies, expiration and priority policies, and a delegate you can use to notify your application when the
     * inserted item is removed from the Cache.
     *
     * @param key The cache key used to reference the item.
     * @param value The item to be added to the cache.
     * @param absoluteExpiration The time at which the added object expires and is removed from the cache.
     * @param slidingExpiration The interval between the time the added object was last accessed and when that object expires. If this value is the equivalent of 20 minutes, the
     * object expires and is removed from the cache 20 minutes after it is last accessed.
     * @param priority The relative cost of the object, as expressed by the CacheItemPriority enumeration. The cache uses this value when it evicts objects; objects with a lower
     * cost are removed from the cache before objects with a higher cost.
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */

    public CacheItemVersion add(Object key, Object value, Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority)
            throws GeneralFailureException, OperationFailedException, AggregateException, SecurityException, ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        addOperation(key, value, absoluteExpiration, slidingExpiration, priority, DSWriteOption.None, null, null, null, null, 
                false, null, null, false, null, null, null, null,
                EventDataFilter.None, EventDataFilter.None,tempRef_size);
        return HelperUtil.createCacheItemVersion(1);
    }

    //+ :20110330
    /**
     * Adds the specified item to the Cache, and a delegate you can use to notify your application when the item is added into cache.
     *
     * @param key The cache key used to reference the item.
     * @param item The item to be added to the cache.
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemAdded callback, if provided, is called when item is added to data source.
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion add(Object key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsAddedCallback onDataSourceItemAdded)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        if (item == null)
        {
            throw new IllegalArgumentException("value cannot be null");
        }
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        addOperation(key, item.getValue(),   item.getAbsoluteExpiration(), 
                item.getSlidingExpiration(), item.getPriority(), dsWriteOption, item.getCacheItemRemovedListener(), item.getCacheItemUpdatedListener(), 
                null, onDataSourceItemAdded, item.getResyncExpiredItems(), item.getGroup(), item.getSubGroup(), false, item.getTags(), null, 
                item.getResyncProviderName(), item.getNamedTags(), item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(), tempRef_size);
        return HelperUtil.createCacheItemVersion(1);
    }

    /**
     * Adds the specified item to the Cache, and a delegate you can use to notify your application when the item is added into cache.
     *
     * @param key The cache key used to reference the item.
     * @param item The item to be added to the cache.
     * @param dsWriteOption option regarding updating data source.
     * @param providerName unique identifier for the data source.
     * @param onDataSourceItemAdded callback, if provided, is called when item is added to data source.
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion add(Object key, CacheItem item, DSWriteOption dsWriteOption, String providerName, DataSourceItemsAddedCallback onDataSourceItemAdded)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        if (item == null)
        {
            throw new IllegalArgumentException("Value cannot be null.\r\nParameter name: item");
        }
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        addOperation(key, item.getValue(),  item.getAbsoluteExpiration(), item.getSlidingExpiration(), 
                item.getPriority(), dsWriteOption, item.getCacheItemRemovedListener(), item.getCacheItemUpdatedListener(), null, onDataSourceItemAdded, 
                item.getResyncExpiredItems(), item.getGroup(), item.getSubGroup(), false, item.getTags(), providerName,
                item.getResyncProviderName(), item.getNamedTags(), item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(),tempRef_size);
        return HelperUtil.createCacheItemVersion(1);
    }

    private BitSet setDSUpdateOptBit(BitSet flagMap, DSWriteOption dsWriteOption)
    {
        if (flagMap == null)
        {
            flagMap = new BitSet();
        }

        switch (dsWriteOption)
        {
            case WriteBehind:
            {
                flagMap.SetBit((byte) BitSetConstants.WriteBehind);
            }
            break;
            case WriteThru:
            {
                flagMap.SetBit((byte) BitSetConstants.WriteThru);
            }
            break;
            case OptionalWriteThru:
            {
                flagMap.SetBit((byte) BitSetConstants.WriteThru);
                flagMap.SetBit((byte) BitSetConstants.OptionalDSOperation);
            }
            break;    
        }

        return flagMap;
    }

    private BitSet setDSReadOptBit(BitSet flagMap, DSReadOption dsReadOption)
    {
        if (flagMap == null)
        {
            flagMap = new BitSet();
        }

        if (dsReadOption == DSReadOption.ReadThru)
        {
            flagMap.SetBit((byte) BitSetConstants.ReadThru);
        }
        
        if (dsReadOption == DSReadOption.OptionalReadThru)
        {
            flagMap.SetBit((byte) BitSetConstants.ReadThru);
            flagMap.SetBit((byte) BitSetConstants.OptionalDSOperation);
        }
        return flagMap;
    }
    

    protected Object addOperation(Object key, Object value,   Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, DSWriteOption dsWriteOption, CacheDataModificationListener cacheItemRemovedListener, CacheDataModificationListener cacheItemUpdatedListener, AsyncItemAddedCallback asyncItemAddedCallback, DataSourceItemsAddedCallback onDataSourceItemAdded, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, Tag[] tags, String providerName, String resyncProviderName, NamedTagsDictionary namedTags, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, RefObject<Long> size)
            throws GeneralFailureException, OperationFailedException, AggregateException, SecurityException, ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        //+ :20110330 previously verified in AddCommand method
        checkKeyValidity(key);
        if (value == null)
        {
            throw new IllegalArgumentException("value cannot be null");
        }

        if (key.equals("") && value.equals(""))
        {
            throw new IllegalArgumentException("key or value cannot be empty");
        }
        
        
        if (group == null)
        {
            if (subGroup != null)
            {
                throw new IllegalArgumentException("group must be specified for sub group");
            }
        }
        //- :20110330

        AddCommand command = null;

        short removeCallbackID = -1;
        short updateCallbackID = -1;
        short dsItemAddedCallbackID = -1;
        
        if(_defaultExpiration!=null){
        HashMap<ExpirationType,Object> resolutionMap = _defaultExpiration.resolveClientExpirations(absoluteExpiration, slidingExpiration);
        absoluteExpiration = (Date) resolutionMap.get(ExpirationType.FixedExpiration);
        slidingExpiration = (TimeSpan) resolutionMap.get(ExpirationType.SlidingExpiration);
        }
        
        
        if (cacheItemRemovedListener != null)
        {
            short[] callbackIds = _eventManager.registerSelectiveEvent(cacheItemRemovedListener, EnumSet.of(EventType.ItemRemoved), itemRemovedDataFilter);
            removeCallbackID = callbackIds[1];
        }
        if (cacheItemUpdatedListener != null)
        {
            short[] callabackIds = _eventManager.registerSelectiveEvent(cacheItemUpdatedListener, EnumSet.of(EventType.ItemUpdated), itemUpdateDataFilter);
                    updateCallbackID = callabackIds[0];
        }
        
        if (asyncItemAddedCallback != null)
        {                        
            asyncCallbackId = (Short)GetCallbackId(asyncItemAddedCallback);
        }

        if (onDataSourceItemAdded != null)
        {
            dsItemAddedCallbackID = GetCallbackId(onDataSourceItemAdded);
        }

        if (isPerfStatsCollectorInitialized())
        {
            _perfStatsCollector.mSecPerAddBeginSample();
        }

        HashMap queryInfo = new HashMap();

        //No need to send query tag info for client cache
        if (_allowQueryTags) {
            HashMap htQueryInfo = getQueryInfo(value);
            if (htQueryInfo != null) {
                queryInfo.put("query-info", htQueryInfo);
            }

            HashMap htNamedTagInfo = getNamedTagsInfo(value, namedTags);
            if (htNamedTagInfo != null) {
                queryInfo.put("named-tag-info", htNamedTagInfo);
            }

            ArrayList<Tag> validTags = new ArrayList<Tag>();

            HashMap htTagInfo = getTagInfo(value, tags, validTags);
            if (htTagInfo != null) {
                queryInfo.put("tag-info", htTagInfo);
            }
        }

        com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();
        //Note: size is not set for any value!! 
        value = getSerializedBytes(value,_serializationContext, flagMap, size);

        //: Variables used for size info for APILogging
        long objectSize = 0;
        long encryptedObjectSize = 0;
        long compressedObjectSize = 0;
        if (DebugAPIConfigurations.isLoggingEnabled() && value != null && _cacheImpl.getSerializationEnabled())
        {
            objectSize = ((byte[]) value).length;
        }
        
        if (isPerfStatsCollectorInitialized() && value != null && value instanceof byte[]) {
            _perfStatsCollector.incrementAvgItemSize(((byte[]) value).length);
        }
        

        if(DebugAPIConfigurations.isLoggingEnabled())
            logSizeInfo((_cacheImpl.getEncryptionEnabled()), false, objectSize, encryptedObjectSize, compressedObjectSize, 1);

        if ((dsWriteOption == DSWriteOption.WriteThru || dsWriteOption == DSWriteOption.WriteBehind) && providerName == null)
        {
            providerName = _defaultWriteThruProvider;
        }
        flagMap = setDSUpdateOptBit(flagMap, dsWriteOption);

        if (providerName != null && providerName.trim().length() > 0)
        {
            providerName = providerName.toLowerCase();
        }
        if (resyncProviderName != null && resyncProviderName.trim().length() > 0)
        {
            resyncProviderName = resyncProviderName.toLowerCase();
        }

        //- :20110330
        if (_cacheImpl instanceof InprocCache && isAsync)
        {
            _cacheImpl.addAsync(key, value,  absoluteExpiration, slidingExpiration, priority, removeCallbackID, updateCallbackID, asyncCallbackId, dsItemAddedCallbackID, isResyncExpiredItems, group, subGroup, queryInfo, flagMap, providerName, resyncProviderName, itemUpdateDataFilter , itemRemovedDataFilter, size.argvalue);
        }
        else
        {
            //Difference in order of arguments in add and addAsync function. onDataSourceItemAdded and asyncItemAddedCallback has differnet order in both function signatures.
            _cacheImpl.add(key, value,  absoluteExpiration, slidingExpiration, priority, removeCallbackID, updateCallbackID, dsItemAddedCallbackID, asyncCallbackId, isResyncExpiredItems, group, subGroup, isAsync, queryInfo, flagMap, providerName, resyncProviderName, itemUpdateDataFilter , itemRemovedDataFilter,size.argvalue);
        }

        if (isPerfStatsCollectorInitialized())
        {
            _perfStatsCollector.mSecPerAddEndSample();
            _perfStatsCollector.incrementAddPerSecStats();
        }

        return value;
    }
    
      /**
     * Add array of CacheItem to the cache.
     *
     * @return keys that are added or that already exists in the cache and their status.
     * @param keys The cache keys used to reference the items.
     * @param items The items that are to be stored
     * @see CacheItem
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap addBulk(Object[] keys, CacheItem[] items)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        return addBulk(keys, items, DSWriteOption.None, null);
    }
    
       
    /**
     * Add array of CacheItem to the cache.
     *
     * @return keys that are added or that already exists in the cache and their status.
     * @param keys The cache keys used to reference the items.
     * @param items The items that are to be stored
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemAddedCallback callback, if provided, is called when item is added to data source.
     * @see CacheItem
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap addBulk(Object[] keys, CacheItem[] items, DSWriteOption dsWriteOption, DataSourceItemsAddedCallback onDataSourceItemAddedCallback)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        return addBulk(keys, items, dsWriteOption, null, onDataSourceItemAddedCallback);
    }
    
    
    /**
     * Add array of CacheItem to the cache.
     *
     * @return keys that are added or that already exists in the cache and their status.
     * @param keys The cache keys used to reference the items.
     * @param items The items that are to be stored
     * @param dsWriteOption option regarding updating data source.
     * @param providerName unique identifier for the data source.
     * @param onDataSourceItemAddedCallback callback, if provided, is called when item is added to data source.
     * @see CacheItem
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap addBulk(Object[] keys, CacheItem[] items, DSWriteOption dsWriteOption, String providerName, DataSourceItemsAddedCallback onDataSourceItemAddedCallback)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (keys == null)
        {
            throw new IllegalArgumentException("keys");
        }
        if (items == null)
        {
            throw new IllegalArgumentException("items");
        }
        if (keys.length == 0)
        {
            throw new OperationFailedException("There is no key present in keys array");
        }

        if (items.length == 0)
        {
            throw new OperationFailedException("There is no item present in items array");
        }
        if (keys.length != items.length)
        {
            throw new IllegalArgumentException("Keys count is not equal to items count");
        }
        if (findDupliate(keys))
        {
            throw new OperationFailedException("Duplicate keys found in provided 'key' array.");
        }
//        if (keys.length == 1 && items.length == 1)
//        {
//            Map addResult = new HashMap();
//            try
//            {
//                addResult.put(keys[0], add(keys[0], items[0]));
//            }
//            catch (Exception e)
//            {
//            }
//            return (HashMap) addResult;
//        }
        Long[] sz = new Long[items.length];
        RefObject<Long[]> refSizes = new RefObject<Long[]>(sz.clone());
        return DoBulkAdd(keys, items, dsWriteOption, providerName, onDataSourceItemAddedCallback, refSizes);
    }
 
    private Object[] RemoveDuplicateKeys(Object[] keys)
    {
//        String[] keys = keyRef.argvalue;
        if (keys.length > 1)
        {
            HashMap keysAndItems = new HashMap(keys.length);
            for (int item = 0; item < keys.length; item++)
            {
                if (keys[item] != null)
                {
                    checkKeyValidity(keys[item]);
                    keysAndItems.put(keys[item], null);
                }
            }
            keys = new Object[keysAndItems.size()];
            keysAndItems.keySet().toArray(keys);
        }
        return keys;
    }

    private void RemoveDuplicateTags(tangible.RefObject<Tag[]> tagRef)
    {
        Tag[] tags = tagRef.argvalue;
        HashMap keysAndItems = new HashMap(tags.length);
        for (int index = 0; index < tags.length; index++)
        {
            if (tags[index] != null && tags[index].getTagName() != null)
            {
                keysAndItems.put(tags[index], null);
            }
            else
            {
                throw new IllegalArgumentException("Tag cannot be null.");
            }
        }
        tags = new Tag[keysAndItems.size()];
        keysAndItems.keySet().toArray(tags);
    }
    
    private void checkKeyValidity(Object key) {
       if (key == null)
            throw new IllegalArgumentException("Key cannot be null.");
       if(key.equals(""))
            throw new IllegalArgumentException("Key cannot be an empty string.");
       if(key instanceof Object[])
            throw new IllegalArgumentException("Key cannot ba an array.");

   }
 
    private Boolean findDupliate(Object[] keys)
    {
        HashMap HashMap = new HashMap(keys.length);
        boolean duplicateFound = false;
        try
        {
            for (int i = 0; i < keys.length; i++)
            {
                checkKeyValidity(keys[i]);

                if (!HashMap.containsKey(keys[i]))
                {
                    //If Count is less than the capacity of the HashMap, this method is an O(1) operation.
                    //If the capacity needs to be increased to accommodate the new element, this method becomes an O(n) operation,
                    //where n is Count.
                    HashMap.put(keys[i], "");
                }
                else
                {
                    duplicateFound = true;
                }
            }
        }
        catch (NullPointerException e)
        {
            throw new IllegalArgumentException("Value cannot be null.\r\nParameter name: key");
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e);
        }
        catch (Exception e)
        {
        }
        HashMap.clear();
        return duplicateFound;
    }

    //+ :20110330
    /**
     * Adds a key and CacheItem in the cache Asynchronously.
     *
     * @return Success or Failure
     * @param key The cache key used to reference the item.
     * @param item CacheItem to be added in the cache.
     * @param dsWriteOption option regarding updating data source.
     * @param onSourceItemAdded callback, if provided, is called when item is added to data source.
     * @see CacheItem
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object addAsync(Object key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsAddedCallback onSourceItemAdded)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
            RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return addOperation(key, item.getValue(), item.getAbsoluteExpiration(), 
                item.getSlidingExpiration(), item.getPriority(), dsWriteOption, item.getCacheItemRemovedListener(), item.getCacheItemUpdatedListener(),
                item.getAsyncItemAddedCallback(), onSourceItemAdded, item.getResyncExpiredItems(), item.getGroup(), item.getSubGroup(), true, 
                item.getTags(), null, item.getResyncProviderName(), item.getNamedTags(),
                item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(), tempRef_size);
    }

    /**
     * Adds a key and CacheItem in the cache Asynchronously.
     *
     * @return Success or Failure
     * @param key The cache key used to reference the item.
     * @param item CacheItem to be added in the cache.
     * @param dsWriteOption option regarding updating data source.
     * @param providerName unique identifier for the data source.
     * @param onSourceItemAdded callback, if provided, is called when item is added to data source.
     * @see CacheItem
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object addAsync(Object key, CacheItem item, DSWriteOption dsWriteOption, String providerName, DataSourceItemsAddedCallback onSourceItemAdded)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return addOperation(key, item.getValue(),  item.getAbsoluteExpiration(), 
                item.getSlidingExpiration(), item.getPriority(), dsWriteOption, item.getCacheItemRemovedListener(), item.getCacheItemUpdatedListener(), 
                item.getAsyncItemAddedCallback(), onSourceItemAdded, item.getResyncExpiredItems(), item.getGroup(), item.getSubGroup(), 
                true, item.getTags(), providerName, item.getResyncProviderName(), item.getNamedTags(),
                item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(),tempRef_size);
    }

//- :20110330
    /**
     * Add a key value pair to the cache asynchronously.
     *                item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(),tempRef_size);

     * @param key The cache key used to reference the item.
     * @param value The value to be added.
     * @param onAsyncItemAddCallback Callback that returns the result of the operation
     * @param group The data group of the item
     * @param subGroup The data group of the item
     * @return Success of Failure
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings. This excpetion is thrown when
     * cache is unable to read or parse configuration file.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object addAsync(Object key, Object value, AsyncItemAddedCallback onAsyncItemAddCallback, String group, String subGroup)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return addOperation(key, value,   Cache.DefaultAbsoluteExpiration, Cache.DefaultSlidingExpiration, CacheItemPriority.Default, 
                DSWriteOption.None, null, null, onAsyncItemAddCallback, null, false, group, subGroup, true, null, null, null, null,
                EventDataFilter.None, EventDataFilter.None ,tempRef_size);
    }

    /**
     * Adds a key and CacheItem in the cache Asynchronously.
     *
     * @return Success or Failure
     * @param key The cache key used to reference the item.
     * @param item CacheItem to be added in the cache.
     * @see CacheItem
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     */
    private Object addAsync(Object key, CacheItem item)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return addOperation(key, item.getValue(),  item.getAbsoluteExpiration(), 
                item.getSlidingExpiration(), item.getPriority(), DSWriteOption.None, item.getCacheItemRemovedListener(), item.getCacheItemUpdatedListener(), 
                item.getAsyncItemAddedCallback(), null, item.getResyncExpiredItems(), item.getGroup(), item.getSubGroup(), true, 
                item.getTags(), null, item.getResyncProviderName(), item.getNamedTags(),
                item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(),tempRef_size);
    }

// </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~ Insert Overloads ~~~~~~~~~~~~~~~~~~~~~~~~~ ">
    /**
     * Inserts an item into the Cache object with a cache key to reference its location and using default values provided by the CacheItemPriority enumeration.
     *
     * @param key The cache key used to reference the item.
     * @param value the item to be added.
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion insert(Object key, Object value)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return insertOperation(key, value,  Cache.DefaultAbsoluteExpiration, Cache.DefaultSlidingExpiration, CacheItemPriority.Default, 
                DSWriteOption.None, null, null, null, null, false, null, null, false, null, null, 
                com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, null, null, null, null,
                EventDataFilter.None, EventDataFilter.None, tempRef_size);
    }

    /**
     * Add a CacheItem to the cache
     *
     * @param key The cache key used to reference the item.
     * @param item The item to be added to the cache.
     * @see CacheItem
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion insert(Object key, CacheItem item)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {

        if (item == null)
        {
            throw new IllegalArgumentException("Value cannot be null.\r\nParameter name: item");
        }
      
        boolean isResyncExpiredItems = false;
        String group = null;
        String subGroup = null;
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);

        NamedTagsDictionary getNamedTags = null;
    
        group = item.getGroup();
        subGroup = item.getSubGroup();
        getNamedTags = item.getNamedTags();
        isResyncExpiredItems = item.getResyncExpiredItems();
      
        return insertOperation(key, item.getValue(),                
                item.getAbsoluteExpiration(), item.getSlidingExpiration(),
                item.getPriority(),
                DSWriteOption.None,
                null,
                item.getCacheItemRemovedListener(),
                item.getCacheItemUpdatedListener(),
                null, isResyncExpiredItems,
                group,
                subGroup,
                false, item.getVersion(), null,
                (item.getVersion() == null ? com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK : com.alachisoft.tayzgrid.caching.LockAccessType.COMPARE_VERSION),
                item.getTags(), null, item.getResyncProviderName(), getNamedTags,
                item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(), tempRef_size);
    }

    //+ :20110330
    /**
     * Add a CacheItem to the cache
     *
     * @param key The cache key used to reference the item.
     * @param item The item to be added to the cache.
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemUpdated callback; if provided, is called when item is updated in data source.
     * @see CacheItem
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion insert(Object key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        if (item == null)
        {
            throw new IllegalArgumentException("Value cannot be null.\r\nParameter name: item");
        }
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return insertOperation(key, item.getValue(), 
                 item.getAbsoluteExpiration(),
                item.getSlidingExpiration(), item.getPriority(), dsWriteOption, null,
                item.getCacheItemRemovedListener(), item.getCacheItemUpdatedListener(), onDataSourceItemUpdated,
                item.getResyncExpiredItems(), item.getGroup(), item.getSubGroup(),
                false,
                item.getVersion(),
                null,
                (item.getVersion() == null ? com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK : com.alachisoft.tayzgrid.caching.LockAccessType.COMPARE_VERSION), 
                item.getTags(), null, item.getResyncProviderName(), item.getNamedTags(),
                item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(), tempRef_size);
    }

    /**
     * Add a CacheItem to the cache
     *
     * @param key The cache key used to reference the item.
     * @param item The item to be added to the cache.
     * @param dsWriteOption option regarding updating data source.
     * @param providerName unique identifier for the data source.
     * @param onDataSourceItemUpdated callback; if provided, is called when item is updated in data source.
     * @see CacheItem
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion insert(Object key, CacheItem item, DSWriteOption dsWriteOption, String providerName, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {

        if (item == null)
        {
            throw new IllegalArgumentException("Value cannot be null.\r\nParameter name: item");
        }
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return insertOperation(key, item.getValue(), 
                item.getAbsoluteExpiration(),
                item.getSlidingExpiration(), item.getPriority(), dsWriteOption, null,
                item.getCacheItemRemovedListener(), item.getCacheItemUpdatedListener(), onDataSourceItemUpdated,
                item.getResyncExpiredItems(), item.getGroup(), item.getSubGroup(),
                false,
                item.getVersion(),
                null,
                (item.getVersion() == null ? com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK : com.alachisoft.tayzgrid.caching.LockAccessType.COMPARE_VERSION), 
                item.getTags(), providerName, item.getResyncProviderName(), item.getNamedTags(),
                item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(),
                tempRef_size);
    }

    //- :20110330
    /**
     *
     * @param key The cache key used to reference the item.
     * @param item The item to be added to the cache.
     * @param lockHandle An instance of LockHandle. If the item is locked, then it can be updated only if the correct lockHandle is specified.
     * @param releaseLock A flag to determine whether or not release lock after operation is performed.
     * @see CacheItem
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion insert(Object key, CacheItem item, LockHandle lockHandle, boolean releaseLock)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {

        if (item == null)
        {
            throw new IllegalArgumentException("Value cannot be null.\r\nParameter name: item");
        }


       
        boolean isResyncExpiredItems = false;
        String group = null;
        String subGroup = null;


        NamedTagsDictionary getNamedTags = null;
        group = item.getGroup();
        subGroup = item.getSubGroup();
        getNamedTags = item.getNamedTags();
        isResyncExpiredItems = item.getResyncExpiredItems();
       
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return insertOperation(key, item.getValue(), 
                item.getAbsoluteExpiration(),
                item.getSlidingExpiration(), item.getPriority(), DSWriteOption.None, null,
                item.getCacheItemRemovedListener(), item.getCacheItemUpdatedListener(), null,
                isResyncExpiredItems, group, subGroup,
                false,
                item.getVersion(),
                lockHandle,
                (releaseLock ? com.alachisoft.tayzgrid.caching.LockAccessType.RELEASE : com.alachisoft.tayzgrid.caching.LockAccessType.DONT_RELEASE), item.getTags(), null, item.getResyncProviderName(), getNamedTags,
                item.getItemUpdatedDataFilter(),
                item.getItemRemovedDataFilter(),
                tempRef_size);
    }

    /**
     * Inserts an Object into the Cache.
     *
     * @param key The cache key used to reference the item.
     * @param value the value to be added in the cache.
     * @param group the data group of the item.
     * @param subGroup the data group of the item.
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion insert(Object key, Object value, String group, String subGroup)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return insertOperation(key, value,  Cache.DefaultAbsoluteExpiration, Cache.DefaultSlidingExpiration, CacheItemPriority.Default, 
                DSWriteOption.None, null, null, null, null, false, group, subGroup, false, null, null, 
                com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, null, null, null, null,
                EventDataFilter.None, EventDataFilter.None, tempRef_size);
    }

    /**
     * Inserts an object into the Cache with dependencies and expiration policies.
     *
     * @param key The cache key used to reference the item.
     * @param value The item to be added to the cache.
     * @param absoluteExpiration The time at which the added object expires and is removed from the cache.
     * @param slidingExpiration The interval between the time the added object was last accessed and when that object expires. If this value is the equivalent of 20 minutes, the
     * object expires and is removed from the cache 20 minutes after it is last accessed.
     * @param priority
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion insert(Object key, Object value, Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority)
            throws GeneralFailureException, OperationFailedException, AggregateException, SecurityException, ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return insertOperation(key, value, absoluteExpiration, slidingExpiration, priority, DSWriteOption.None, 
                null, null, null, null, false, null, null, false, null, null, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, 
                null, null, null, null, EventDataFilter.None, EventDataFilter.None, tempRef_size);
    }

    /**
     * Inserts an item into the Cache with a cache key to reference its location and using default values provided by the CacheItemPriority enumeration. It also enables the
     * associating tags with the object.
     *
     * @param key The cache key used to reference the item.
     * @param value The item to be added to the cache.
     * @param tags An array of Tag to associate with the object.
     * @see Tag
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion insert(Object key, Object value, Tag[] tags)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return insertOperation(key, value, Cache.DefaultAbsoluteExpiration, Cache.DefaultSlidingExpiration, 
                CacheItemPriority.Normal, DSWriteOption.None, null, null, null, null, false, null, null, false, null, null, 
                com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, tags, null, null, null,
                EventDataFilter.None, EventDataFilter.None, tempRef_size);
    }

    /**
     * Inserts an item into the Cache with a cache key to reference its location and using default values provided by the CacheItemPriority enumeration. It also enables the
     * associating tags with the object.
     *
     * @param key The cache key used to reference the item.
     * @param value The item to be added to the cache.
     * @param namedTags to associate with object.
     * @see NamedTagsDictionary
     * @return The item version in cache
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItemVersion insert(Object key, Object value, NamedTagsDictionary namedTags)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return insertOperation(key, value,  Cache.DefaultAbsoluteExpiration, Cache.DefaultSlidingExpiration, CacheItemPriority.Normal, 
                DSWriteOption.None, null, null, null, null, false, null, null, false, null, null, 
                com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, null, null, null, namedTags,
                EventDataFilter.None, EventDataFilter.None, tempRef_size);
    }


    protected CacheItemVersion insertOperation(Object key, Object value,  Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, DSWriteOption dsWriteOption, AsyncItemUpdatedCallback asyncItemUpdatedCallback, CacheDataModificationListener cacheItemRemovedListener, CacheDataModificationListener cacheItemUdpatedListener, DataSourceItemsUpdatedCallback onDataSourceItemUpdated, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, CacheItemVersion version, LockHandle lockHandle, com.alachisoft.tayzgrid.caching.LockAccessType accessType, Tag[] tags, String providerName, String resyncProviderName, NamedTagsDictionary namedTags, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, RefObject<Long> size) 
            throws GeneralFailureException, OperationFailedException, AggregateException, SecurityException, ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        InsertResult result = insertOperationInternal(key, value,   absoluteExpiration, slidingExpiration, priority, dsWriteOption, asyncItemUpdatedCallback, cacheItemRemovedListener, cacheItemUdpatedListener, onDataSourceItemUpdated, isResyncExpiredItems, group, subGroup, isAsync, version, lockHandle, accessType, tags, providerName, resyncProviderName, namedTags, itemUpdateDataFilter, itemRemovedDataFilter, size, null);
        CacheItemVersion newVersion = new CacheItemVersion();
        if(result == null)
            newVersion = HelperUtil.createCacheItemVersion(0);
        else
            newVersion = HelperUtil.createCacheItemVersion(result.Version);
        return newVersion;
    }
    

     protected InsertResult insertOperationInternal(Object key, Object value,  Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, DSWriteOption dsWriteOption, AsyncItemUpdatedCallback asyncItemUpdatedCallback, CacheDataModificationListener cacheItemRemovedListener, CacheDataModificationListener cacheItemUdpatedListener, DataSourceItemsUpdatedCallback onDataSourceItemUpdated, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, CacheItemVersion version, LockHandle lockHandle, com.alachisoft.tayzgrid.caching.LockAccessType accessType, Tag[] tags, String providerName, String resyncProviderName, NamedTagsDictionary namedTags, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, RefObject<Long> size, InsertParams options) 
            throws GeneralFailureException, OperationFailedException, AggregateException, SecurityException, ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        checkKeyValidity(key);
        if (value == null)
        {
            throw new IllegalArgumentException("Value cannot be null");
        }
        

        if (group == null && subGroup != null)
        {
            throw new IllegalArgumentException("group must be specified for sub group");
        }

        com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();
//        InsertCommand command = null;
//        long itemVersion = version != null ? version.getVersion() : 0;

        if (value == null)
        {
            throw new IllegalArgumentException("Value cannot be null.\r\nParameter name: value");
        }
        
        String lockID = "";

        lockID = lockHandle != null ? lockHandle.getLockId() : "";

        //accessType = releaseLock ? LockAccessType.RELEASE : LockAccessType.DONT_RELEASE;

        if (lockID != null && !lockID.equals(""))
        {
            flagMap.SetBit((byte) BitSetConstants.LockItem);
        }
        
        if(_defaultExpiration!=null){
        HashMap<ExpirationType,Object> resolutionMap = _defaultExpiration.resolveClientExpirations(absoluteExpiration, slidingExpiration);
        absoluteExpiration = (Date) resolutionMap.get(ExpirationType.FixedExpiration);
        slidingExpiration = (TimeSpan) resolutionMap.get(ExpirationType.SlidingExpiration);
        }
        
        HashMap queryInfo = new HashMap();

        //No need to send query tag info for client cache
        if (_allowQueryTags) {
            HashMap htQueryInfo = getQueryInfo(value);

            if (htQueryInfo != null) {
                queryInfo.put("query-info", htQueryInfo);
            }

            HashMap htNamedTagInfo = getNamedTagsInfo(value, namedTags);
            if (htNamedTagInfo != null) {
                queryInfo.put("named-tag-info", htNamedTagInfo);
            }


            ArrayList<Tag> validTags = new ArrayList<Tag>();
            HashMap htTagInfo = getTagInfo(value, tags, validTags);
            if (htTagInfo != null) {
                queryInfo.put("tag-info", htTagInfo);
            }
        }
        
        // need to set the damn value of size
        value = getSerializedBytes(value,_serializationContext, flagMap, size);
        

        //: Variables used for size info for APILogging
        long objectSize = 0;
        long encryptedObjectSize = 0;
        long compressedObjectSize = 0;

        if (DebugAPIConfigurations.isLoggingEnabled() && value != null && _cacheImpl.getSerializationEnabled())
        {
            objectSize = ((byte[]) value).length;
        }

        if (isPerfStatsCollectorInitialized() && value != null && value instanceof byte[]) {
            _perfStatsCollector.incrementAvgItemSize(((byte[]) value).length);
        }

        if (DebugAPIConfigurations.isLoggingEnabled()) {
            logSizeInfo((_cacheImpl.getEncryptionEnabled()), false, objectSize, encryptedObjectSize, compressedObjectSize, 1);
        }

        if ((dsWriteOption == DSWriteOption.WriteThru || dsWriteOption == DSWriteOption.WriteBehind) && providerName == null)
        {
            providerName = _defaultWriteThruProvider;
        }
        flagMap = setDSUpdateOptBit(flagMap, dsWriteOption);




        if (providerName != null && providerName.trim().length() > 0)
        {
            providerName = providerName.toLowerCase();
        }

        if (resyncProviderName != null && resyncProviderName.trim().length() > 0)
        {
            resyncProviderName = resyncProviderName.toLowerCase();
        }



        short removeCallbackId = -1;
        short updateCallbackId = -1;
        short asyncCallbackId = -1;
        short dsItemUpdateCallbackId = -1;
        
        if (asyncItemUpdatedCallback != null)
        {
            asyncCallbackId = GetCallbackId(asyncItemUpdatedCallback);
//            if (_cacheImpl.getCallbackQueue().indexOf(asyncItemUpdatedCallback) == -1)
//            {
//                _cacheImpl.getCallbackQueue().add(asyncItemUpdatedCallback);
//            }
        }

        if (cacheItemRemovedListener != null)
        {
            short[] callabackIds = _eventManager.registerSelectiveEvent(cacheItemRemovedListener, EnumSet.of(EventType.ItemRemoved), itemRemovedDataFilter);
                    removeCallbackId = callabackIds[1];
            //            if (_cacheImpl.getCallbackQueue().indexOf(removeCallback) == -1)
//            {
//                _cacheImpl.getCallbackQueue().add(removeCallback);
//            }
        }
        
        if (cacheItemUdpatedListener != null)
        {
            short[] callabackIds = _eventManager.registerSelectiveEvent(cacheItemUdpatedListener, EnumSet.of(EventType.ItemUpdated), itemUpdateDataFilter);
                    updateCallbackId = callabackIds[0];
            //            if (_cacheImpl.getCallbackQueue().indexOf(updateCallback) == -1)
//            {
//                _cacheImpl.getCallbackQueue().add(updateCallback);
//            }
        }

        if (onDataSourceItemUpdated != null)
        {
            dsItemUpdateCallbackId = GetCallbackId(onDataSourceItemUpdated);
            //            if (_cacheImpl.getCallbackQueue().indexOf(onDataSourceItemUpdated) == -1)
//            {
//                _cacheImpl.getCallbackQueue().add(onDataSourceItemUpdated);
//            }
        }

        if(options != null && options.OldValue != null)
        {
            BitSet flags = new BitSet();
            Object serialized = getSerializedEntry(options.OldValue, flags);
            options.OldValue = serialized;
            options.OldValueFlag = flags;
        }
        
        if (_cacheImpl instanceof InprocCache && isAsync)
        {
            _cacheImpl.insertAsync(key, value,   absoluteExpiration, slidingExpiration, priority, 
                    removeCallbackId, updateCallbackId, asyncCallbackId, dsItemUpdateCallbackId, isResyncExpiredItems, 
                    group, subGroup, queryInfo, flagMap, providerName, resyncProviderName, itemUpdateDataFilter, itemRemovedDataFilter, size.argvalue);
			
			if (isPerfStatsCollectorInitialized()) {
                _perfStatsCollector.mSecPerUpdateEndSample();
                _perfStatsCollector.incrementUpdPerSecStats();
            }
        }
        else
        {
            InsertResult result = _cacheImpl.insert(key, value,   absoluteExpiration, slidingExpiration, priority, 
                    removeCallbackId, updateCallbackId, dsItemUpdateCallbackId, 
                    asyncCallbackId, isResyncExpiredItems, group, subGroup, isAsync, queryInfo, flagMap, lockID, version, accessType, 
                    providerName, resyncProviderName, itemUpdateDataFilter, itemRemovedDataFilter, size.argvalue, options);
			if(result != null)
				result.ExistingValue = getDeserializedEntry((CompressedValueEntry) result.ExistingValue);           

		   if (isPerfStatsCollectorInitialized()) {
                _perfStatsCollector.mSecPerUpdateEndSample();
                _perfStatsCollector.incrementUpdPerSecStats();
            }
			return result;
        }
        return null;
    }
     
     //----15/April/2015--Added specifically to support JCAche operations
     /**
     * This method is intended for internal use only.
     */
     public InsertResult insertInternal(Object key, Object value, DSWriteOption dsWriteOption, InsertParams options) throws OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
     {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        return insertOperationInternal(key, value,   Cache.DefaultAbsoluteExpiration, Cache.DefaultSlidingExpiration, CacheItemPriority.Default, 
                dsWriteOption, null, null, null, null, false, null, null, false, null, null, 
                com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, null, null, null, null,
                EventDataFilter.None, EventDataFilter.None, tempRef_size, options);
     }
     
     //+NUMAN: 21/April/2015 Entry Processor
     /**
     * 
     * @param key The cache key used to reference the item.
     * @param entryProcessor Instance of user implementation of entryProcessor.  
     * @param arguments Arguments required by the entryProcessor.
     * @return returns result after processing.
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access
     * cache.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws EntryProcessorException Any Exception that occurs while executing
     * entry processor task is wrapped in this exception.
     */
     public Object invokeEntryProcessor(Object key,
           com.alachisoft.tayzgrid.runtime.processor.EntryProcessor entryProcessor,
           Object... arguments) throws GeneralFailureException, OperationFailedException,  ConnectionException, AggregateException, EntryProcessorException
     {
         EntryProcessorResult result=null;
         if (key == null)
         {
             throw new IllegalArgumentException("Key cannot be null");
         }
         java.util.Map<Object,EntryProcessorResult> map= invokeEntryProcessorBulk(new Object[]{key},entryProcessor,arguments);
         
         if(map==null || map.isEmpty())
         {
             return null;
         }
         else
         {
             Iterator itResult = map.entrySet().iterator();
             if(itResult.hasNext())
                 result= ((Map.Entry<Object,EntryProcessorResult>)itResult.next()).getValue();
         }
         
         return result!=null?result.get():null;
     }
     
      //+NUMAN: 21/April/2015 Entry Processor
     /**
     * 
     * @param keys Array of keys the cache keys used to reference the items.
     * @param entryProcessor Instance of user implementation of entryProcessor.  
     * @param arguments Arguments required by the entryProcessor.
     * @return returns result after processing in a hashMap for each key.
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access
     * cache.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     */
     public java.util.Map<Object,EntryProcessorResult> invokeEntryProcessorBulk(Object[] keys,
           com.alachisoft.tayzgrid.runtime.processor.EntryProcessor entryProcessor,
           Object... arguments) throws GeneralFailureException, OperationFailedException,  ConnectionException, AggregateException
     {
          if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
          
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        
        if (keys ==null || keys.length == 0) 
        {
            throw new IllegalArgumentException("There is no key present in keys array");
        }
        
        if (findDupliate(keys))
        {
            throw new OperationFailedException("Duplicate keys found in provided 'key' array.");
        }
        
        if (entryProcessor == null)
        {
            throw new IllegalArgumentException("EntryProcessor cannot be null");
        }
        
        return _cacheImpl.invokeEntryProcessor(keys, entryProcessor,_defaultReadThruProvider,_defaultWriteThruProvider, arguments);
     }
     
    /**
     * Insert a CacheItem to the cache asyncronously
     *
     * @param key The cache key used to reference the item.
     * @param value The value to be added in the cache.
     * @param asyncItemUpdatedCallback
     * @param group
     * @param subGroup
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void insertAsync(Object key, Object value, AsyncItemUpdatedCallback asyncItemUpdatedCallback, String group, String subGroup)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        insertOperation(key, value,  Cache.DefaultAbsoluteExpiration, Cache.DefaultSlidingExpiration, CacheItemPriority.Default, 
                DSWriteOption.None, asyncItemUpdatedCallback, null, null, null, false, group, subGroup, true, null, null, 
                com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, null, null, null, null,
                EventDataFilter.None, EventDataFilter.None, tempRef_size);

    }

    //+ :20110330
    /**
     * Inserts a key and CacheItem in the cache.
     *
     * @param key The cache key used to reference the item.
     * @param item The item that is to be stored
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemUpdated callback; if provided, is called when item is updated in data source.
     * @see CacheItem
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void insertAsync(Object key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        insertOperation(key, item.getValue(),  item.getAbsoluteExpiration(), 
                item.getSlidingExpiration(), item.getPriority(), dsWriteOption, item.getAsyncItemUpdatedCallback(), null, null, 
                onDataSourceItemUpdated, item.getResyncExpiredItems(), item.getGroup(), item.getSubGroup(), true, null, null, 
                com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, item.getTags(), null, item.getResyncProviderName(), 
                item.getNamedTags(), item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(),tempRef_size);

    }
    
    /**
     * Inserts a key and CacheItem in the cache.
     *
     * @param key The cache key used to reference the item.
     * @param item The item that is to be stored.
     * @param providerName unique identifier for the data source.
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemUpdated callback; if provided, is called when item is updated in data source.
     * @see CacheItemGeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void insertAsync(Object key, CacheItem item, String providerName, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        long size=0;
        RefObject<Long> tempRef_size = new RefObject<Long>(size);
        insertOperation(key, item.getValue(),
                item.getAbsoluteExpiration(), item.getSlidingExpiration(), item.getPriority(), dsWriteOption, 
                item.getAsyncItemUpdatedCallback(), null, null, onDataSourceItemUpdated, item.getResyncExpiredItems(), item.getGroup(), 
                item.getSubGroup(), true, null, null, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, item.getTags(), 
                providerName, item.getResyncProviderName(), item.getNamedTags(),
                item.getItemUpdatedDataFilter(), item.getItemRemovedDataFilter(),tempRef_size);

    }
    
    
    /**
     * Insert list of CacheItem to the cache
     *
     * @return The list of items could not be added in the cache.
     * @param keys The cache keys used to reference the items.
     * @param items The items that are to be stored.
     * @throws ConnectionException
     * @see CacheItemGeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     */
    public HashMap insertBulk(Object[] keys, CacheItem[] items)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        return insertBulk(keys, items, DSWriteOption.None,  null);
    }
    
    /**
     * Insert list of CacheItem to the cache
     *
     * @return The list of items could not be added in the cache.
     * @param keys The cache keys used to reference the items.
     * @params items The items that are to be stored.
     * @param items
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemUpdated callback; if provided, is called when item is updated in data source.
     * @see CacheItemGeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap insertBulk(Object[] keys, CacheItem[] items, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        return insertBulk(keys, items, dsWriteOption, null, onDataSourceItemUpdated);
    }

    /**
     * Insert list of CacheItem to the cache
     *
     * @return The list of items could not be added in the cache.
     * @param keys The cache keys used to reference the items.
     * @params items The items that are to be stored
     * @param items
     * @param providerName unique identifier for the data source.
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemUpdated callback; if provided, is called when item is updated in data source.
     * @see CacheItemGeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap insertBulk(Object[] keys, CacheItem[] items, DSWriteOption dsWriteOption, String providerName, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception
    {
        if (keys.length == 0)
        {
            throw new OperationFailedException("There is no key present in keys array");
        }

        if (items.length == 0)
        {
            throw new OperationFailedException("There is no item present in items array");
        }
        if (keys.length != items.length)
        {
            throw new IllegalArgumentException("Keys count is not equal to items count");
        }
        if (findDupliate(keys))
        {
            throw new OperationFailedException("Duplicate keys found in provided 'key' array.");
        }
        
        Long[] sz = new Long[items.length];
        RefObject<Long[]> refSizes = new RefObject<Long[]>(sz.clone());
        return DoBulkInsert(keys, items, dsWriteOption, providerName, onDataSourceItemUpdated,refSizes);
    }
    // </editor-fold>
    
    /**
     * Removes the object from the Cache.
     *
     * @return The item removed from the Cache. If the value in the key parameter is not found, returns a null reference.
     * @param key The cache key used to reference the item.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object remove(Object key)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return remove(key, DSWriteOption.None, null);
    }

    /**
     * Removes the object from the Cache.
     *
     * @param key The cache key used to reference the item. GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void delete(Object key)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        delete(key, DSWriteOption.None, null);
    }

    /**
     * Removes an item from cache if the specified version is still the most recent version in the cache.
     *
     * @param key The cache key used to reference the item.
     * @param version The version of the item to be removed. The item is removed from the cache only if this is still the most recent version in the cache.
     * @return The item removed from the Cache. If the value in the key parameter is not found, returns a null reference GeneralFailureException Thrown when an exception
     * occurs during a clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object remove(Object key, CacheItemVersion version)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return remove(key, null, DSWriteOption.None, null, false, null, version, com.alachisoft.tayzgrid.caching.LockAccessType.COMPARE_VERSION, null);
    }

    /**
     * Removes an item from cache if the specified version is still the most recent version in the cache.
     *
     * @param key The cache key used to reference the item.
     * @param version The version of the item to be removed. The item is removed from the cache only if this is still the most recent version in the cache. GeneralFailureException
     * Thrown when an exception occurs during a clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void delete(Object key, CacheItemVersion version)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        delete(key, null, DSWriteOption.None, null, false, null, version, com.alachisoft.tayzgrid.caching.LockAccessType.COMPARE_VERSION, null, null);
    }

    /**
     * Removes an item from cache if it is not already locked or if the correct lock-id is specified.
     *
     * @param key The cache key used to reference the item.
     * @param lockHandle If the item is locked then, it can be removed only if the correct lockHandle is specified.
     * @return The item removed from the Cache. If the value in the key parameter is not found, returns a null reference GeneralFailureException Thrown when an exception
     * occurs during a clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object remove(Object key, LockHandle lockHandle)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return remove(key, null, DSWriteOption.None, null, false, lockHandle, null, com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT, null);
    }

    /**
     * Removes an item from cache if it is not already locked or if the correct lock-id is specified.
     *
     * @param key The cache key used to reference the item.
     * @param lockHandle If the item is locked then, it can be removed only if the correct lockHandle is specified. GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void delete(Object key, LockHandle lockHandle)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        delete(key, null, DSWriteOption.None, null, false, lockHandle, null, com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT, null, null);
    }

    /**
     * Removes the object from the Cache.
     *
     * @return The item removed from the Cache. If the value in the key parameter is not found, returns a null reference.
     * @param key The cache key used to reference the item.
     * @param dsWriteOption option regarding updating data source.
     * @param providerName unique identifier for the data source.
     * @param onDataSourceItemRemoved callback; if provided, is called when item is removed from data source. GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object remove(Object key, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return remove(key, null, dsWriteOption, onDataSourceItemRemoved, false, null, null, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, providerName);
    }

    /**
     * Removes the object from the Cache.
     *
     * @param key The cache key used to reference the item.
     * @param dsWriteOption option regarding updating data source.
     * @param providerName unique identifier for the data source.
     * @param onDataSourceItemRemoved callback; if provided, is called when item is removed from data source. GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void delete(Object key, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        delete(key, null, dsWriteOption, onDataSourceItemRemoved, false, null, null, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, providerName, null);
    }

    /**
     * Removes the object from the Cache.
     *
     * @param key The cache key used to reference the item.
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemRemoved callback; if provided, is called when item is removed from data source. GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void delete(Object key, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        delete(key, null, dsWriteOption, onDataSourceItemRemoved, false, null, null, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, null, null);
    }
    
    ///Added for jcache
    public boolean deleteInternal(Object key, DSWriteOption dsWriteOption, DeleteParams deleteParams) 
            throws OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return delete(key, null, dsWriteOption, null, false, null, null, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, null, deleteParams);
    }

    /**
     * Removes the object from the Cache.
     *
     * @return The item removed from the Cache. If the value in the key parameter is not found, returns a null reference.
     * @param key The cache key used to reference the item.
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemRemoved callback; if provided, is called when item is removed from data source. GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object remove(Object key, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return remove(key, null, dsWriteOption, onDataSourceItemRemoved, false, null, null, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, null);
    }

    private Object remove(Object key, AsyncItemRemovedCallback asyncItemRemovedCallback, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved, boolean isAsync, LockHandle lockHandle, CacheItemVersion itemVersion, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        checkKeyValidity(key);

        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        

        com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();

        String lockId = null;
        long version = 0;


        if (lockHandle != null)
        {
            lockId = lockHandle.getLockId();
        }

        if (itemVersion != null)
        {
            version = itemVersion.getVersion();
        }
        if (providerName != null && providerName.trim().length() > 0)
        {
            providerName = providerName.toLowerCase();
        }


        flagMap = this.setDSUpdateOptBit(flagMap, dsWriteOption);


        short asyncItemRemovedCallbackId = -1;
        short onDSItemRemovedCallbackId = -1;
        
        if (asyncItemRemovedCallback != null)
        {
            asyncItemRemovedCallbackId = GetCallbackId(asyncItemRemovedCallback);
        }

        if (onDataSourceItemRemoved != null)
        {
            onDSItemRemovedCallbackId = GetCallbackId(onDataSourceItemRemoved);
        }

        if ((dsWriteOption == DSWriteOption.WriteThru || dsWriteOption == DSWriteOption.WriteBehind) && providerName == null)
        {
            providerName = _defaultWriteThruProvider;
        }

        long objectSize = 0;
        long encryptedObjectSize = 0;
        long compressedObjectSize = 0;

        Object obj = null;
        try
        {
            if (isPerfStatsCollectorInitialized())
            {
                _perfStatsCollector.mSecPerDelBeginSample();
            }
            com.alachisoft.tayzgrid.caching.CompressedValueEntry result = _cacheImpl.remove(key, flagMap, asyncItemRemovedCallbackId, onDSItemRemovedCallbackId, isAsync, lockId, HelperUtil.createCacheItemVersion(version), accessType, providerName);
            if (result != null && result.Value != null)
            {

                if (result.Value instanceof byte[])
                {
                    if(isPerfStatsCollectorInitialized() && result.Value !=null && result.Value instanceof byte[])
                        _perfStatsCollector.incrementAvgItemSize(((byte[]) result.Value).length);
                    

                    if (DebugAPIConfigurations.isLoggingEnabled() && result.Value instanceof byte[]) {
                        objectSize = ((byte[]) result.Value).length;
                    }
                    obj = Cache.this.getDeserializedObject(result.Value,_serializationContext, result.Flag); // we need flag of result entry for deserialization

                }
                else
                {
                    obj = result.Value;
                }
            }
            if (isPerfStatsCollectorInitialized())
            {
                _perfStatsCollector.mSecPerDelEndSample();
                _perfStatsCollector.incrementDelPerSecStats();
            }
            if (DebugAPIConfigurations.isLoggingEnabled()) {
                logSizeInfo((_cacheImpl.getEncryptionEnabled()), false, objectSize, encryptedObjectSize, compressedObjectSize, 1);
            }
            return obj;
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        if (DebugAPIConfigurations.isLoggingEnabled()) {
            logSizeInfo((_cacheImpl.getEncryptionEnabled()), false, objectSize, encryptedObjectSize, compressedObjectSize, 1);
        }
        return null;

    }

    private boolean delete(Object key, AsyncItemRemovedCallback asyncItemRemovedCallback, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved, boolean isAsync, LockHandle lockHandle, CacheItemVersion itemVersion, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName, DeleteParams deleteParams)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {


        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        checkKeyValidity(key);
        
        com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();

        String lockId = null;
        long version = 0;


        if (lockHandle != null)
        {
            lockId = lockHandle.getLockId();
        }

        if (itemVersion != null)
        {
            version = itemVersion.getVersion();
        }
        if (providerName != null && providerName.trim().length() > 0)
        {
            providerName = providerName.toLowerCase();
        }


        flagMap = this.setDSUpdateOptBit(flagMap, dsWriteOption);

        short asyncItemRemovedCallbackId = -1;
        short onDSItemRemovedCallbackId = -1;
        
        if (asyncItemRemovedCallback != null)
        {
            asyncItemRemovedCallbackId = GetCallbackId(asyncItemRemovedCallback);
        }

        if (onDataSourceItemRemoved != null)
        {
            onDSItemRemovedCallbackId = GetCallbackId(onDataSourceItemRemoved);
        }

        if ((dsWriteOption == DSWriteOption.WriteThru || dsWriteOption == DSWriteOption.WriteBehind) && providerName == null)
        {
            providerName = _defaultWriteThruProvider;
        }
        
        if(deleteParams != null && deleteParams.OldValue != null)
        {
            BitSet flags = new BitSet();
            Object serialized = getSerializedEntry(deleteParams.OldValue, flags);
            deleteParams.OldValue = serialized;
            deleteParams.OldValueFlag = flags;
        }

        try
        {
            return _cacheImpl.delete(key, flagMap, asyncItemRemovedCallbackId, onDSItemRemovedCallbackId, isAsync, lockId, HelperUtil.createCacheItemVersion(version), accessType, providerName, deleteParams);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return false;
    }

    /**
     * Removes the object from the Cache. This is similar to Remove except that the operation is performed asynchronously. A ItemRemoved event is fired upon successful
     * completion of this method.It is not possible to determine if the actual operation has failed, therefore use this operation for the cases when it does not matter much.
     *
     * @param key The cache key used to reference the item.
     * @param asyncItemRemovedCallback callback can be used by the client application to get the result of the Asynchronous Remove operation
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemRemoved callback; if provided, is called when item is removed from data source. GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void removeAsync(Object key, AsyncItemRemovedCallback asyncItemRemovedCallback, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        removeAsync(key, asyncItemRemovedCallback, dsWriteOption, null, onDataSourceItemRemoved);
    }

    /**
     * Removes the object from the Cache. This is similar to Remove except that the operation is performed asynchronously. A ItemRemoved event is fired upon successful
     * completion of this method.It is not possible to determine if the actual operation has failed, therefore use this operation for the cases when it does not matter much.
     *
     * @param key The cache key used to reference the item.
     * @param asyncItemRemovedCallback callback can be used by the client application to get the result of the Asynchronous Remove operation
     * @param dsWriteOption option regarding updating data source.
     * @param providerName unique identifier for the data source.
     * @param onDataSourceItemRemoved callback; if provided, is called when item is removed from data source. GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void removeAsync(Object key, AsyncItemRemovedCallback asyncItemRemovedCallback, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        remove(key, asyncItemRemovedCallback, dsWriteOption, onDataSourceItemRemoved, true, null, null, com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT, providerName);
    }

    
    /**
     * Removes the objects from the Cache.
     *
     * @return The items removed from the Cache. If the value in the keys parameter is not found, returns a null reference
     * @param keys The cache keys used to reference the item.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap removeBulk(Object[] keys)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return removeBulk(keys, DSWriteOption.None,null);
    }
    
    /**
     * Removes the objects from the Cache.
     *
     * @return The items removed from the Cache. If the value in the keys parameter is not found, returns a null reference
     * @param keys The cache keys used to reference the item.
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemRemoved callback; if provided, is called when item is removed from data source. GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap removeBulk(Object[] keys, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        return removeBulk(keys, dsWriteOption, null, onDataSourceItemRemoved);
    }

    /**
     * Removes the objects from the Cache.
     *
     * @return The items removed from the Cache. If the value in the keys parameter is not found, returns a null reference
     * @param keys The cache keys used to reference the item.
     * @param dsWriteOption option regarding updating data source.
     * @param providerName unique identifier for the data source.
     * @param onDataSourceItemRemoved callback; if provided, is called when item is removed from data source.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap removeBulk(Object[] keys, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();
        //to find null keys
        findDupliate(keys);
        
        short onDSItemRemoved = -1;
        
        if (onDataSourceItemRemoved != null)
        {
            onDSItemRemoved = GetCallbackId(onDataSourceItemRemoved,keys.length);
        }

        if ((dsWriteOption == DSWriteOption.WriteThru || dsWriteOption == DSWriteOption.WriteBehind) && providerName == null)
        {
            providerName = _defaultWriteThruProvider;
        }

        if (providerName != null && providerName.trim().length() > 0)
        {
            providerName = providerName.toLowerCase();
        }
        flagMap = this.setDSUpdateOptBit(flagMap, dsWriteOption);

        long sumObjectSize = 0;
        long sumEncryptedObjectSize = 0;
        long sumCompressedObjecSize = 0;

        try
        {
            HashMap resultMap = _cacheImpl.remove(keys, flagMap, providerName, onDSItemRemoved);

            if (resultMap == null)
            {
                if (DebugAPIConfigurations.isLoggingEnabled()) {
                    logSizeInfo((_cacheImpl.getEncryptionEnabled()), true, sumObjectSize, sumEncryptedObjectSize, sumCompressedObjecSize, 0);
                }
                return null;
            }

            HashMap deflatMap = new HashMap(resultMap.size());
            ///Do decompression (if necessary) and deserialization of data
            ///and add to a new table

            for (Object entry : resultMap.entrySet())
            {
                if (Common.is(((Map.Entry) entry).getValue(), CompressedValueEntry.class))
                {
                    Map.Entry<String, CompressedValueEntry> pair =
                            (Map.Entry<String, CompressedValueEntry>) entry;

                    CompressedValueEntry result = pair.getValue();
                    if (result.Value instanceof CallbackEntry)
                    {
                        CallbackEntry e = (CallbackEntry) (result.Value instanceof CallbackEntry ? result.Value : null);
                        result.Value = e.getValue();
                    }
                    if (result.Value == null)
                    {
                        deflatMap.put(pair.getKey(), null);
                    }
                    else
                    {
                        if (result.Value instanceof byte[])
                        {
                            if(isPerfStatsCollectorInitialized() && result.Value !=null)
                                _perfStatsCollector.incrementAvgItemSize(((byte[]) result.Value).length);
                            
                            if (DebugAPIConfigurations.isLoggingEnabled() && result.Value instanceof byte[]) {
                                sumObjectSize += ((byte[]) result.Value).length;
                            }
                            deflatMap.put(pair.getKey(), Cache.this.getDeserializedObject(result.Value,_serializationContext, result.Flag));
                        }
                        else
                        {
                            deflatMap.put(pair.getKey(), Cache.this.getDeserializedObject(result.Value,_serializationContext, result.Flag));
                        }
                    }
                }
                else
                {
                    deflatMap.put(((Map.Entry) entry).getKey(), ((Map.Entry) entry).getValue());
                }
            }
            if (DebugAPIConfigurations.isLoggingEnabled()) {
                logSizeInfo((_cacheImpl.getEncryptionEnabled()), true, sumObjectSize, sumEncryptedObjectSize, sumCompressedObjecSize, resultMap.size());
            }
            return deflatMap;
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        if (DebugAPIConfigurations.isLoggingEnabled()) {
            logSizeInfo((_cacheImpl.getEncryptionEnabled()), true, sumObjectSize, sumEncryptedObjectSize, sumCompressedObjecSize, 0);
        }
        return null;
    }
    
    
     /**
     * Removes the objects from the Cache.
     *
     * @param keys The cache keys used to reference the item.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void deleteBulk(Object[] keys)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        deleteBulk(keys, DSWriteOption.None, null);
    }
    
    
    /**
     * Removes the objects from the Cache.
     *
     * @param keys The cache keys used to reference the item.
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceItemRemoved callback; if provided, is called when item is removed from data source. GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void deleteBulk(Object[] keys, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        deleteBulk(keys, dsWriteOption, null, onDataSourceItemRemoved);
    }
    
    
    /**
     * Removes the objects from the Cache.
     *
     * @param keys The cache keys used to reference the item.
     * @param dsWriteOption option regarding updating data source.
     * @param providerName unique identifier for the data source.
     * @param onDataSourceItemRemoved callback; if provided, is called when item is removed from data source.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void deleteBulk(Object[] keys, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (keys == null)
        {
            throw new IllegalArgumentException("keys");
        }
        if (keys.length == 0)
        {
            throw new IllegalArgumentException("There is no key present in keys array");
        }
        com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();
        //for null entries in keys.
        findDupliate(keys);
        
        short onDSItemRemoved = -1;
        
        if (onDataSourceItemRemoved != null)
        {
            onDSItemRemoved = GetCallbackId(onDataSourceItemRemoved,keys.length);
        }

        if ((dsWriteOption == DSWriteOption.WriteThru || dsWriteOption == DSWriteOption.WriteBehind) && providerName == null)
        {
            providerName = _defaultWriteThruProvider;
        }

        if (providerName != null && providerName.trim().length() > 0)
        {
            providerName = providerName.toLowerCase();
        }

        flagMap = this.setDSUpdateOptBit(flagMap, dsWriteOption);
        try
        {
            _cacheImpl.delete(keys, flagMap, providerName, onDSItemRemoved);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
    }

    /**
     * Remove the group from cache.
     *
     * @param group group to be removed.
     * @param subGroup subGroup to be removed.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void removeGroupData(String group, String subGroup)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
       if (group == null)
        {
            throw new IllegalArgumentException("Group cannot be null");
        }
        if (group == null && subGroup != null)
        {
            throw new IllegalArgumentException("Group must be specified for sub group");
        }
        try
        {
            _cacheImpl.remove(group, subGroup);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return;
    }

    /**
     * Removes all elements from the Cache.
     *
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void clear()
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        clear(DSWriteOption.None, null);
    }

    /**
     * Removes all elements from the Cache.
     *
     * @param dsWriteOption option regarding updating data source.
     * @param onDataSourceCleared callback; if provided, is called when data source is cleared.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    protected void clear(DSWriteOption dsWriteOption, DataSourceClearedCallback onDataSourceCleared)
    throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (_cacheImpl == null)
        {
            return;
        }
        try
        {
            String providerName = null;

            if (cacheId == null)
            {
                throw new OperationFailedException("Cache is not initialized");
            }

            if (onDataSourceCleared != null)
            {
                if (_cacheImpl.getCallbackQueue().indexOf(onDataSourceCleared) == -1)
                {
                    _cacheImpl.getCallbackQueue().add(onDataSourceCleared);
                }
            }
            //clear api not supported in DS providers
            dsWriteOption = DSWriteOption.None;
            com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();
            flagMap = this.setDSUpdateOptBit(flagMap, dsWriteOption);


            _cacheImpl.clear(flagMap, (short) _cacheImpl.getCallbackQueue().indexOf(onDataSourceCleared), false, providerName);


        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
    }

    /**
     * Removes all elements from the Cache asynchronously.
     *
     * @param onAsyncCacheCleared Callback that returns the result of the operation
     * @throws GeneralFailureException
     * @throws OperationFailedException
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void clearAsync(AsyncCacheClearedCallback onAsyncCacheCleared)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        clear(DSWriteOption.None, onAsyncCacheCleared, null);
    }
    
    private void clear(DSWriteOption dsWriteOption, AsyncCacheClearedCallback onAsyncCacheCleared, DataSourceClearedCallback onDataSourceCleared)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (_cacheImpl == null)
        {
            return;
        }
        try
        {
            String providerName = null;

            if (cacheId == null)
            {
                throw new OperationFailedException("Cache is not initialized");
            }
            if (onAsyncCacheCleared != null)
            {
                if (_cacheImpl.getCallbackQueue().indexOf(onAsyncCacheCleared) == -1)
                {
                    _cacheImpl.getCallbackQueue().add(onAsyncCacheCleared);
                }
            }

            if (onDataSourceCleared != null)
            {
                if (_cacheImpl.getCallbackQueue().indexOf(onDataSourceCleared) == -1)
                {
                    _cacheImpl.getCallbackQueue().add(onDataSourceCleared);
                }
            }
            //clear api not supported in DS providers
            dsWriteOption = DSWriteOption.None;
            com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();
            flagMap = this.setDSUpdateOptBit(flagMap, dsWriteOption);

            _cacheImpl.clearAsync(flagMap, (short) _cacheImpl.getCallbackQueue().indexOf(onAsyncCacheCleared), (short) _cacheImpl.getCallbackQueue().indexOf(onDataSourceCleared), true, providerName);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~ Search & SearchEntries ~~~~~~~~~~~~~~~~~~~~~~~~">
    /**
     * Retrieves the keys for the specified query.
     *
     * @return The list of keys.
     * @param query The query to execute on the cache.
     * @param values The HashMap of attribute names and values.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.register
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Collection search(String query, HashMap values)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(query))
        {
            throw new IllegalArgumentException("query");
        }
        if (values == null)
        {
            throw new IllegalArgumentException("values");
        }

        com.alachisoft.tayzgrid.caching.queries.QueryResultSet resultSet = null;
        java.util.ArrayList collection = new java.util.ArrayList();
        try
        {
            resultSet = _cacheImpl.search(query, values);

            switch (resultSet.getType())
            {
                case AggregateFunction:
                {
                    Object result = resultSet.getAggregateFunctionResult().getValue();
                    if (result != null)
                    {
                        if (resultSet.getAggregateFunctionResult().getKey().toString().toUpperCase()
                                .equals(AggregateFunctionType.AVG.toString().toUpperCase()))
                        {
                            resultSet.setAggregateFunctionResult(new DictionaryEntry<Object, Object>(AggregateFunctionType.AVG, ((AverageResult) result).getAverage()));
                        }
                        collection.add(result);
                    }
                }
                break;
                case SearchKeys:
                    collection = resultSet.getSearchKeysResult();
                    break;
            }
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }

        return collection;
    }
 /**
     * Executes delete statements on cache
     *
     * @return The number of rows affected.
     * @param query The delete query to execute on the cache.
     * @param values The HashMap of attribute names and values.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     */
    public int executeNonQuery(String query, HashMap values)
            throws OperationFailedException, Exception
    {
        int effectedKeys = 0;
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (query == null || query.equals(""))
        {
            throw new IllegalArgumentException("query");
        }
        if (values == null)
        {
            throw new IllegalArgumentException("values");
        }

        try
        {
            effectedKeys = _cacheImpl.executeNonQuery(query, values);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return effectedKeys;
    }

    /**
     * Retrieves the key and value pairs for the specified query.
     *
     * @return The list of key and value pairs for the specified query.
     * @param query The query to execute on the cache.
     * @param values The HashMap of attribute names and values.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap searchEntries(String query, HashMap values)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (query == null || query.equals(""))
        {
            throw new OperationFailedException("query");
        }
        if (values == null)
        {
            throw new OperationFailedException("values");
        }
        if(query.toLowerCase().contains("group by"))
        {
            throw new OperationFailedException("Invalid query. Queries with GROUP BY clause can not be executed using SearchEntries API.");
        }
        com.alachisoft.tayzgrid.caching.queries.QueryResultSet resultSet = null;
        HashMap deflatMap = new HashMap();

        try
        {
            resultSet = _cacheImpl.searchEntries(query, values);
            switch (resultSet.getType())
            {
                case AggregateFunction:
                {
                    Object result = resultSet.getAggregateFunctionResult().getValue();
                    if (result != null)
                    {
                        if (resultSet.getAggregateFunctionResult().getKey().toString().toUpperCase()
                                .equals(AggregateFunctionType.AVG.toString().toUpperCase()))
                        {
                            deflatMap.put(resultSet.getAggregateFunctionResult().getKey(), ((AverageResult) result).getAverage());
                        }
                        else
                        {
                            deflatMap.put(resultSet.getAggregateFunctionResult().getKey(), result);
                        }
                    }

                }
                break;
                case SearchEntries:
                {
                    HashMap resultMap = (HashMap) resultSet.getSearchEntriesResult();
                    if (resultMap == null)
                    {
                        return null;
                    }

                    ///Do decompression (if necessary) and deserialization of data
                    ///and add to a new table

                    long sumObjectSize = 0;
                    long sumEncryptedObjectSize = 0;
                    long sumCompressedObjecSize = 0;

                    for (Object entry : resultMap.entrySet())
                    {
                        Map.Entry<String, CompressedValueEntry> pair =
                                (Map.Entry<String, CompressedValueEntry>) entry;

                        CompressedValueEntry valEntry = pair.getValue();
                        if (valEntry.Value instanceof byte[])
                        {
                            byte[] deflatValue = (byte[]) valEntry.Value;
                            if (DebugAPIConfigurations.isLoggingEnabled()) {
                                sumCompressedObjecSize += deflatValue.length;
                            }
                            
                            if (isPerfStatsCollectorInitialized() && deflatValue != null) {
                                _perfStatsCollector.incrementAvgItemSize(((byte[]) deflatValue).length);
                            }

                            if (DebugAPIConfigurations.isLoggingEnabled()) {
                                sumObjectSize += deflatValue.length;
                            }
                            deflatMap.put(pair.getKey(), Cache.this.getDeserializedObject(deflatValue,_serializationContext, valEntry.Flag));
                        }
                        else
                        {
                            deflatMap.put(pair.getKey(), Cache.this.getDeserializedObject(valEntry.Value, _serializationContext,valEntry.Flag));
                        }
                    }
                    if (DebugAPIConfigurations.isLoggingEnabled()) {
                        logSizeInfo((_cacheImpl.getEncryptionEnabled()), true, sumObjectSize, sumEncryptedObjectSize, sumCompressedObjecSize, resultMap.size());
                    }
                }
                break;
            }
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return deflatMap;
    }
    
    
    /**
     * Retrieves ICacheReader to read results for the specified query.
     * @return The list of key and value pairs for the specified query.
     * @param query The query to execute on the cache.
     * @param values The HashMap of attribute names and values.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
 the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public ICacheReader executeReader(String query, HashMap values)
        throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (query == null || query.equals(""))
        {
            throw new OperationFailedException("query");
        }
        if (values == null)
        {
            throw new OperationFailedException("values");
        }
        
        if(!query.toLowerCase().contains("group by"))
        {
            throw new OperationFailedException("Invalid query. Only queries with GROUP BY clause can be executed using ExecuteReader API.");
        }
        
        com.alachisoft.tayzgrid.caching.queries.QueryResultSet resultSet = null;
        ICacheReader reader=null;
        try
        {
            resultSet=_cacheImpl.searchEntries(query, values);
            switch(resultSet.getType())
            {
                case GroupByAggregateFunction:
                    reader = new QueryCacheReader(resultSet.getGroupByResult());
            }
                    
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return reader;
    } 
    
    //</editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~ Contains ~~~~~~~~~~~~~~~~~~~~~~~~">
    /**
     * Determines whether the cache contains a specific key.
     *
     * @return true if the Cache contains an element with the specified key; otherwise, false.
     * @param key The key to locate in the Cache.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public boolean contains(Object key)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        checkKeyValidity(key);
        
        if (_cacheImpl == null)
        {
            return false;
        }
        try
        {
            return _cacheImpl.contains(key);
        }
        catch (Exception e4)
	{
	   throw e4;				
	}
        

    }
    //</editor-fold>

    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~ Get Overloads GetCacheItem GetGroupData GetGroupKeys ~~~~~~~ ">
    /**
     * Retrieves the specified item from the Cache object.
     *
     * @return The retrieved cache item, or a null reference (Nothing in Visual Basic) if the key is not found.
     * @param key The identifier for the cache item to retrieve.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object get(Object key)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {


        return get(key, null, null, DSReadOption.None, null, Cache.NoLockingExpiration, null, com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT, null);

    }

    /**
     * Retrieves the specified item from the Cache object. It accepts the CacheItemVersion. If item version is 0, object would be retrieved from cache with version. If any
     * value greater the 0 is specified,then object is returned from the cache only if that is the current version of the object in the cache.
     *
     * @param key The identifier for the cache item to retrieve.
     * @param version the version of the object.
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object get(Object key, CacheItemVersion version)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        com.alachisoft.tayzgrid.caching.LockAccessType accessType = com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT;
        if (version != null)
        {
            accessType = version.getVersion() == 0 ? com.alachisoft.tayzgrid.caching.LockAccessType.GET_VERSION : com.alachisoft.tayzgrid.caching.LockAccessType.MATCH_VERSION;
        }
        return get(key, DSReadOption.None, version);

    }

    /**
     * Retrieves the specified item from the Cache object if it is not already locked. Otherwise returns null. This is different from the normal Get operation where an item
     * is returned ignoring the lock altogether.
     *
     * @param key The identifier for the cache item to retrieve
     * @param lockTimeout The time span after which the lock is automatically released
     * @param lockHandle An instance of LockHandle to hold the lock information. lockHandle will be populated by cache.
     * @param acquireLock A flag to determine whether to acquire a lock or not
     * @return The retrieved cache item, or a null reference if the key is not found
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache. {
     * @codeThe following example demonstrates how to retrieve the cached value and acquire a lock at the same time Cache theCache =
     * Cache.initializeCache("myreplicatedcache"); theCache.Add("cachedItemKey", "cachedItemValue");
     *
     * LockHandle lockHandle = new LockHandle();
     *
     * object cachedItem = theCache.get("cachedItemKey", Cache.NoLockingExpiration, lockHandle, true);}
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object get(Object key, TimeSpan lockTimeout, LockHandle lockHandle, boolean acquireLock)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {


        com.alachisoft.tayzgrid.caching.LockAccessType accessType = (acquireLock ? com.alachisoft.tayzgrid.caching.LockAccessType.ACQUIRE : com.alachisoft.tayzgrid.caching.LockAccessType.DONT_ACQUIRE);

        return get(key, null, null, DSReadOption.None, null, lockTimeout, lockHandle, accessType, null);
    }

    /**
     * Retrieves the specified item from the Cache object.
     *
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @param key The identifier for the cache item to retrieve.
     * @param dsReadOption Options regarding reading from data source
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object get(Object key, DSReadOption dsReadOption)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return get(key, null, null, dsReadOption, null, Cache.NoLockingExpiration, null, com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT, null);
    }

    /**
     * Retrieves the specified item from the Cache object.
     *
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @param key The identifier for the cache item to retrieve.
     * @param dsReadOption Options regarding reading from data source
     * @param version he version of the object.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object get(Object key, DSReadOption dsReadOption, CacheItemVersion version)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        com.alachisoft.tayzgrid.caching.LockAccessType accessType = com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT;
        if (version != null)
        {
            accessType = version.getVersion() == 0 ? com.alachisoft.tayzgrid.caching.LockAccessType.GET_VERSION : com.alachisoft.tayzgrid.caching.LockAccessType.MATCH_VERSION;
        }

        return get(key, null, null, dsReadOption, version, Cache.NoLockingExpiration, null, accessType, null);
    }

    /**
     * Retrieves the specified item from the Cache object.
     *
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @param key The identifier for the cache item to retrieve.
     * @param group Group of the object.
     * @param subGroup SubGroup of the object.
     * @param dsReadOption Options regarding reading from data source
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object get(Object key, String group, String subGroup, DSReadOption dsReadOption)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return get(key, group, subGroup, dsReadOption, null, Cache.NoLockingExpiration, null, com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT, null);
    }

    /**
     * Retrieves the specified item from the Cache object.
     *
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @param key The identifier for the cache item to retrieve.
     * @param providerName unique identifier for the data source.
     * @param dsReadOption Options regarding reading from data source
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object get(Object key, String providerName, DSReadOption dsReadOption)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return get(key, null, null, dsReadOption, null, Cache.NoLockingExpiration, null, com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT, providerName);
    }

    /**
     * Retrieves the specified item from the Cache object.
     *
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @param key The identifier for the cache item to retrieve.
     * @param providerName unique identifier for the data source.
     * @param dsReadOption Options regarding reading from data source
     * @param version he version of the object.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object get(Object key, String providerName, DSReadOption dsReadOption, CacheItemVersion version)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        com.alachisoft.tayzgrid.caching.LockAccessType accessType = com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT;
        if (version != null)
        {
            accessType = version.getVersion() == 0 ? com.alachisoft.tayzgrid.caching.LockAccessType.GET_VERSION : com.alachisoft.tayzgrid.caching.LockAccessType.MATCH_VERSION;
        }

        return get(key, null, null, dsReadOption, version, Cache.NoLockingExpiration, null, accessType, providerName);
    }
    
    //ammama:
    // To get Cache Config from the server - 
    /*
    * This method is intended for internal use only.
    */
    public CacheConfigParams getCacheConfigurationInternal() throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        CacheConfigParams params = new CacheConfigParams();
        if(_cacheImpl == null)
            return null;
        try
        {
            params = _cacheImpl.getCacheConfiguration();
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return params;
    }

    /**
     *
     * @param key in the cache.
     * @param group
     * @param subGroup
     * @param version
     * @param lockTimeout
     * @param lockHandle
     * @param acquireLock
    * @return
     * @throws GeneralFailureException
     * @throws OperationFailedException
     * @throws AggregateException
     * @throws SecurityException
     */
    Object get(Object key, String group, String subGroup, DSReadOption dsReadOption, CacheItemVersion version, TimeSpan lockTimeout, LockHandle lockHandle, com.alachisoft.tayzgrid.caching.LockAccessType accessType, String providerName)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (version == null && accessType == com.alachisoft.tayzgrid.caching.LockAccessType.COMPARE_VERSION)
        {
            throw new IllegalArgumentException("Value cannot be null.\r\nParameter name: version");
        }
        checkKeyValidity(key);
        com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();

        if (lockHandle == null)
        {
            lockHandle = new LockHandle();
        }

        if (version == null)
        {
            version = HelperUtil.createCacheItemVersion(0);
        }

        if (dsReadOption == DSReadOption.ReadThru && providerName == null)
        {
            providerName = _defaultReadThruProvider;
        }
        flagMap = this.setDSReadOptBit(flagMap, dsReadOption);

        long objectSize = 0;
        long encryptedObjectSize = 0;
        long compressedObjectSize = 0;

        Object obj = null;
        com.alachisoft.tayzgrid.caching.CompressedValueEntry result = null;
        try
        {
            if (_perfStatsCollector != null)
            {
                _perfStatsCollector.mSecPerGetBeginSample();
                _perfStatsCollector.mSecPerDecryptionBeginSample();
            }
            result = _cacheImpl.get(key, flagMap, group, subGroup, version, lockHandle, lockTimeout, accessType, providerName);
            if (_perfStatsCollector != null)
            {
                _perfStatsCollector.mSecPerGetEndSample();
                _perfStatsCollector.mSecPerDecryptionEndSample();
                _perfStatsCollector.incrementGetPerSecStats();
            }
            if (result != null && result.Value != null)
            {

                if (result.Value instanceof CallbackEntry)
                {
                    CallbackEntry e = result.Value instanceof CallbackEntry ? (CallbackEntry) result.Value : null;
                    result.Value = e.getValue();
                }
                if (result.Value instanceof byte[])
                {
                    if (DebugAPIConfigurations.isLoggingEnabled()) {
                        compressedObjectSize = ((byte[]) result.Value).length;
                    }
                    
                    if(isPerfStatsCollectorInitialized() && result.Value !=null)
                        _perfStatsCollector.incrementAvgItemSize(((byte[]) result.Value).length);
                    
                    if (DebugAPIConfigurations.isLoggingEnabled() && result.Value instanceof byte[]) {
                        objectSize = ((byte[]) result.Value).length;
                    }
                }
                obj = Cache.this.getDeserializedObject(result.Value,_serializationContext, result.Flag);
            }
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        if (DebugAPIConfigurations.isLoggingEnabled()) {
            logSizeInfo(_encryptionEnabled, false, objectSize, encryptedObjectSize, compressedObjectSize, 1);
        }
        return obj;
    }    

    /*
    * This method is intended for internal use only.
    */
    public void getBulkInternal(Set<? extends K> set, boolean bln, CompletionListener cl) throws NullPointerException, IllegalStateException,
            CacheException, ClassCastException
    {
        Object[] keys = set.toArray(new Object[set.size()]);
        JCacheLoadAllItem jCacheItems = new JCacheLoadAllItem();
        jCacheItems.setReplaceExistingValues(bln);
        jCacheItems.setCompletionListener(cl);
        try {
            getBulk(keys, DSReadOption.OptionalReadThru, null, jCacheItems, true);
        }
        catch (Exception e) {
        }
    }
    
     /**
     * Retrieves the object from the cache for the given keys as key value pairs
     *
     * @return The retrieved cache items.
     * @param keys The keys against which items are to be fetched.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap getBulk(Object[] keys)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return getBulk(keys, DSReadOption.None);
    }
    
    /**
     * Retrieves the object from the cache for the given keys as key value pairs
     *
     * @return The retrieved cache items.
     * @param keys The keys against which items are to be fetched.
     * @param dsReadOption Options regarding reading from data source
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap getBulk(Object[] keys, DSReadOption dsReadOption)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return getBulk(keys, dsReadOption, null, null, false);
    }

    /**
     * Retrieves the object from the cache for the given keys as key value pairs
     *
     * @return The retrieved cache items.
     * @param keys The keys against which items are to be fetched.
     * @param provideName unique identifier for the data source.
     * @param dsReadOption Options regarding reading from data source
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap getBulk(Object[] keys, String provideName, DSReadOption dsReadOption)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return getBulk(keys, dsReadOption, provideName, null,false);
    }

//    private HashMap getBulk(Object[] keys, DSReadOption dsReadOption, String providerName) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    private HashMap getBulk(Object[] keys, DSReadOption dsReadOption, String providerName, JCacheLoadAllItem jCacheItem, boolean isAsync) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (keys == null)
        {
            throw new IllegalArgumentException("keys");
        }
        if (keys.length == 0)
        {
            throw new IllegalArgumentException("There is no key present in keys array");
        }
        keys = RemoveDuplicateKeys(keys);
        com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();

        if (dsReadOption == DSReadOption.ReadThru && providerName == null)
        {
            providerName = _defaultReadThruProvider;
        }

        if(jCacheItem == null) jCacheItem = new JCacheLoadAllItem();
        if(jCacheItem.getCompletionListener() != null) {
            asyncCallbackId = (Short)GetCallbackId(jCacheItem.getCompletionListener());
            jCacheItem.setCompletionListenerId(asyncCallbackId);
        }
 
        HashMap resultMap = null;
        flagMap = this.setDSReadOptBit(flagMap, dsReadOption);

        try
        {

            resultMap = (HashMap) _cacheImpl.get(keys, flagMap, providerName, jCacheItem.getCompletionListenerId(), jCacheItem.getReplaceExistingValues(), isAsync);

            if (resultMap == null)
            {
                if (DebugAPIConfigurations.isLoggingEnabled()) {
                    logSizeInfo(_encryptionEnabled, true, 0, 0, 0, 0);
                }
                return null;
            }
            long sumObjectSize = 0;
            long sumCompressedObjectSize = 0;
            long sumEncryptedObjectSize = 0;

            HashMap deflatMap = new HashMap(resultMap.size());
            ///Do decompression (if necessary) and deserialization of data
            ///and add to a new table

            for (Object entry : resultMap.entrySet())
            {
                if (Common.is(((Map.Entry) entry).getValue(), CompressedValueEntry.class))
                {
                    Map.Entry<String, CompressedValueEntry> pair =
                            (Map.Entry<String, CompressedValueEntry>) entry;
                    CompressedValueEntry result = pair.getValue();
                    if (result!= null)
                    {
                        if (result.Value instanceof CallbackEntry)
                        {
                            CallbackEntry e = (CallbackEntry) (result.Value instanceof CallbackEntry ? result.Value : null);
                            result.Value = e.getValue();
                        }
                    }
                    if (result.Value instanceof byte[])
                    {
                        if (DebugAPIConfigurations.isLoggingEnabled()) {
                            sumCompressedObjectSize += ((byte[]) result.Value).length;
                        }
                        
                        if(isPerfStatsCollectorInitialized() && result.Value !=null)
                            _perfStatsCollector.incrementAvgItemSize(((byte[]) result.Value).length);
                    
                        if (DebugAPIConfigurations.isLoggingEnabled() && result.Value instanceof byte[]) {
                            sumObjectSize += ((byte[]) result.Value).length;
                        }
                        deflatMap.put(pair.getKey(), Cache.this.getDeserializedObject(result.Value,_serializationContext, result.Flag));
                    }
                    else
                    {
                        deflatMap.put(pair.getKey(), Cache.this.getDeserializedObject(result.Value,_serializationContext, result.Flag));
                    }
                }
                else
                {
                    deflatMap.put(((Map.Entry) entry).getKey(), ((Map.Entry) entry).getValue());
                }
            }
            if (DebugAPIConfigurations.isLoggingEnabled()) {
                logSizeInfo(_encryptionEnabled, true, sumObjectSize, sumEncryptedObjectSize, sumCompressedObjectSize, deflatMap.size());
            }
            return deflatMap;
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return null;
    }

    /**
     * Retrieves the key and value pairs in a group or sub group. If only group is specified, data for the group and all the sub groups of the group are returned. If both the group
     * and sub group are specified. Only the data related to the sub group are returned.
     *
     * @return The list of keys of a group or a sub group.
     * @param group The group whose keys are to be returned.
     * @param subGroup The sub group of the group foe which keys are to be returned.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Collection getGroupKeys(String group, String subGroup)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (group == null)
        {
            throw new IllegalArgumentException("Group cannot be null");
        }
        try
        {
            return _cacheImpl.getGroupKeys(group, subGroup);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return null;
    }

    /**
     * Retrieves the key and value pairs in a group or sub group. If only group is specified, data for the group and all the sub groups of the group are returned. If both the group
     * and sub group are specified. Only the data related to the sub group are returned.
     *
     * @return The list of key and value pairs of a group or a sub group.
     * @param group The group whose keys are to be returned.
     * @param subGroup The sub group of the group foe which keys are to be returned.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public HashMap getGroupData(String group, String subGroup)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (group == null)
        {
            throw new IllegalArgumentException("Group cannot be null");
        }
        if (group == null && subGroup != null)
        {
            throw new IllegalArgumentException("Group must be specified for sub group");
        }
        try
        {
            HashMap resultMap = _cacheImpl.getGroupData(group, subGroup);
            if (resultMap == null)
            {
                if (DebugAPIConfigurations.isLoggingEnabled()) {
                    logSizeInfo(_encryptionEnabled, true, 0, 0, 0, 0);
                }
                return null;
            }

            long sumObjectSize = 0;
            long sumCompressedObjectSize = 0;
            long sumEncryptedObjectSize = 0;

            HashMap deflatMap = new HashMap(resultMap.size());
            ///Do decompression (if necessary) and deserialization of data
            ///and add to a new table

            for (Object entry : resultMap.entrySet())
            {
                Map.Entry<String, CompressedValueEntry> pair =
                        (Map.Entry<String, CompressedValueEntry>) entry;

                CompressedValueEntry result = pair.getValue();
                if (result.Value instanceof CallbackEntry)
                {
                    CallbackEntry e = result.Value instanceof CallbackEntry ? (CallbackEntry) result.Value : null;
                    result.Value = e.getValue();
                }
                if (DebugAPIConfigurations.isLoggingEnabled()) {
                    sumCompressedObjectSize += ((byte[]) result.Value).length;
                }
                
                if (isPerfStatsCollectorInitialized() && result.Value != null && result.Value instanceof byte[]) {
                    _perfStatsCollector.incrementAvgItemSize(((byte[]) result.Value).length);
                }
                                    
                if (DebugAPIConfigurations.isLoggingEnabled() && result.Value instanceof byte[]) {
                    sumObjectSize += ((byte[]) result.Value).length;
                }

                deflatMap.put(pair.getKey(), Cache.this.getDeserializedObject(result.Value,_serializationContext, result.Flag));

            }
            if (DebugAPIConfigurations.isLoggingEnabled()) {
                logSizeInfo(_encryptionEnabled, true, sumObjectSize, sumEncryptedObjectSize, sumCompressedObjectSize, deflatMap.size());
            }
            return deflatMap;
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return null;
    }

    /**
     * Get the cache item stored in cache.
     *
     * @return The Cache item in the cache against the specified key.
     * @param key The cache key used to reference the item.
     * @see CacheItem
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItem getCacheItem(Object key) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
         return (CacheItem) this.getCacheItemInternal(key, null, null, DSReadOption.None, null, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, NoLockingExpiration, null, "", false);
    }

     /**
     * Get the cache item stored in cache.
     *
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @param key The cache key used to reference the item.
     * @param lockTimeout The TimeSpan after which the lock is automatically released.
     * @param lockHandle An instance of "LockHandle" to hold the lock information.
     * @param acquireLock A flag to determine whether to acquire a lock or not.
     * @see CacheItem
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItem getCacheItem(Object key, TimeSpan lockTimeout, LockHandle lockHandle, boolean acquireLock) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return this.getCacheItemInternal(key, null, null, DSReadOption.None, null, acquireLock ? com.alachisoft.tayzgrid.caching.LockAccessType.ACQUIRE : com.alachisoft.tayzgrid.caching.LockAccessType.DONT_ACQUIRE, lockTimeout, lockHandle, "", acquireLock);
    }

    /**
     * Retrieves the specified item from the Cache object. It accepts the by reference. See @link CacheItemVersion by reference
     * If null is passed for CacheItemVersion, then the version of the object from the cache is returned. If non-null
     * CacheItemVersion is passed, then object is returned from the cache only if that is the current version of the object in the cache.
     *
     * @param key The identifier for the cache item to retrieve.
     * @param version The version of the object.
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItem getCacheItem(Object key, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return this.getCacheItem(key, DSReadOption.None, version);
    }

    /**
     * Retrieves the specified item from the Cache object.
     *
     * @param key The identifier for the cache item to retrieve.
     * @param dsReadOption Options regarding reading from data source
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItem getCacheItem(Object key, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return this.getCacheItem(key, null, dsReadOption);
    }

    /**
     * Retrieves the specified item from the Cache object.
     *
     * @param key The identifier for the cache item to retrieve.
     * @param providerName A specific name for the data source
     * @param dsReadOption Options regarding reading from data source
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItem getCacheItem(Object key, String providerName, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return this.getCacheItem(key, providerName, dsReadOption, null);
    }

    /**
     * Retrieves the specified item from the Cache object.
     *
     * @param key key The identifier for the cache item to retrieve.
     * @param dsReadOption Options regarding reading from data source
     * @param version
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItem getCacheItem(Object key, DSReadOption dsReadOption, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        com.alachisoft.tayzgrid.caching.LockAccessType accessType = com.alachisoft.tayzgrid.caching.LockAccessType.DEFAULT;
        if (version != null)
        {
            accessType = version.getVersion() == 0 ? com.alachisoft.tayzgrid.caching.LockAccessType.GET_VERSION : com.alachisoft.tayzgrid.caching.LockAccessType.MATCH_VERSION;
        }
        return this.getCacheItemInternal(key, null, null, dsReadOption, version, accessType, NoLockingExpiration, null, "", false);
    }

    /**
     * Retrieves the specified item from the Cache object. If the object is read through the data source, put it in the cache. It accepts the @link CacheItemVersion by reference.
     * If null is passed for CacheItemVersion, then the version of the object from the cache is returned. If non-null
     * CacheItemVersion is passed, then object is returned from the cache only if that is the current version of the object in the cache.
     *
     * @param key The identifier for the cache item to retrieve.
     * @param providerName A specific name for the data source
     * @param dsReadOption Options regarding reading from data source.
     * @param version The version of the object.
     * @return The retrieved cache item, or a null reference if the key is not found
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItem getCacheItem(Object key, String providerName, DSReadOption dsReadOption, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        return this.getCacheItemInternal(key, null, null, dsReadOption, version, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, NoLockingExpiration, null, providerName, false);
    }

    /**
     * Retrieves the specified item from the Cache object. If the object is read through the data source, put it against the given group and sub group.
     *
     * @param key The identifier for the cache item to retrieve.
     * @param group The name of the group which the item belongs to.
     * @param subGroup The name of the subGroup within a group.
     * @param dsReadOption Options regarding reading from data source.
     * @return The retrieved cache item, or a null reference if the key is not found.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public CacheItem getCacheItem(Object key, String group, String subGroup, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (group == null)
        {
            throw new ArgumentNullException("group can not be null");
        }
        return (CacheItem) this.getCacheItemInternal(key, group, subGroup, dsReadOption, null, com.alachisoft.tayzgrid.caching.LockAccessType.IGNORE_LOCK, NoLockingExpiration, null, null, false);
    }

    CacheItem getCacheItemInternal(Object key, String group, String subGroup, DSReadOption dsReadOption, CacheItemVersion version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, TimeSpan lockTimeout, LockHandle lockHandle, String providerName, boolean acquireLock) throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception
    {
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (lockHandle == null)
        {
            lockHandle = new LockHandle();
        }
        if (key == null)
        {
            throw new IllegalArgumentException("keys");
        }
        if (key.equals(""))
        {
            throw new IllegalArgumentException("key cannot be empty string");
        }
        if (group == null && subGroup != null)
        {
            throw new IllegalArgumentException("group must be specified for sub group");
        }
        if (dsReadOption == null)
        {
            throw new OperationFailedException("DSReadOption can not be Null, use DSReadOption.None instead");
        }

        if (dsReadOption == DSReadOption.ReadThru && providerName == null)
        {
            providerName = _defaultReadThruProvider;
        }

        CacheItem item = new CacheItem();
        com.alachisoft.tayzgrid.caching.CacheEntry entry = null;
        com.alachisoft.tayzgrid.common.BitSet flagMap = new com.alachisoft.tayzgrid.common.BitSet();
        String lockId = lockHandle.getLockId();
        try
        {

            flagMap = this.setDSReadOptBit(flagMap, dsReadOption);

            Object value = _cacheImpl.getCacheItem(key, flagMap, group, subGroup, dsReadOption, version, lockHandle, lockTimeout, accessType, providerName);
            if (value == null)
            {
                return null;
            }

            long objectSize = 0;
            long encryptedObjectSize = 0;
            long compressedObjectSize = 0;

            if (value instanceof CacheItem)
            {
                item = (CacheItem) value;
                
                if (isPerfStatsCollectorInitialized() && ((CacheItem) value).getValue() != null && ((CacheItem) value).getValue() instanceof byte[]) {
                    _perfStatsCollector.incrementAvgItemSize(((byte[]) ((CacheItem) value).getValue()).length);
                }
                
                if (DebugAPIConfigurations.isLoggingEnabled() && item.getValue() instanceof byte[] && _cacheImpl.getSerializationEnabled())
                {
                    objectSize = ((byte[]) item.getValue()).length;
                }

                item.setValue(Cache.this.getDeserializedObject(item.getValue(),_serializationContext, item.getFlag()));
                if (item.getAbsoluteExpiration() != Cache.DefaultAbsoluteExpiration)
                {
                    item.setAbsoluteExpiration(item.getAbsoluteExpiration());
                }
                if (DebugAPIConfigurations.isLoggingEnabled()) {
                    logSizeInfo(_encryptionEnabled, false, objectSize, encryptedObjectSize, compressedObjectSize, 1);
                }
                return item;

            }
            entry = (com.alachisoft.tayzgrid.caching.CacheEntry) value;
            item.setFlag(entry.getFlag());

            if (entry.getValue() instanceof CallbackEntry)
            {
                CallbackEntry cb = (CallbackEntry) entry.getValue();
                item.setValue(cb.getValue());

                if (DebugAPIConfigurations.isLoggingEnabled() && item.getValue() instanceof byte[] && _cacheImpl.getSerializationEnabled())
                {
                    objectSize = ((byte[]) item.getValue()).length;
                }
                
                if (isPerfStatsCollectorInitialized() && cb.getValue() != null && cb.getValue() instanceof byte[]) {
                    _perfStatsCollector.incrementAvgItemSize(((byte[]) cb.getValue()).length);
                }

                item.setValue(Cache.this.getDeserializedObject(item.getValue(),_serializationContext, item.getFlag()));
            }
            else
            {
                item.setValue(entry.getValue());

                if (DebugAPIConfigurations.isLoggingEnabled() && item.getValue() instanceof byte[] && _cacheImpl.getSerializationEnabled())
                {
                    objectSize = ((byte[]) item.getValue()).length;
                }

                if (isPerfStatsCollectorInitialized() && entry.getValue() != null && entry.getValue() instanceof byte[]) {
                    _perfStatsCollector.incrementAvgItemSize(((byte[]) entry.getValue()).length);
                }
                item.setValue(Cache.this.getDeserializedObject(item.getValue(),_serializationContext, item.getFlag()));
            }
            if (DebugAPIConfigurations.isLoggingEnabled()) {
                logSizeInfo(_encryptionEnabled, false, objectSize, encryptedObjectSize, compressedObjectSize, 1);
            }

            if (entry != null)
            {
                item.setPriority((CacheItemPriority) entry.getPriority());
            }

            ExpirationHint hint = entry.getExpirationHint();

            if (hint != null)
            {
                item.setResyncExpiredItems(hint.getNeedsReSync());

            }

            Date absoluteExpiration = null;
            TimeSpan slidingExpiration = TimeSpan.ZERO;
            tangible.RefObject<Date> tempRef_absExp = new tangible.RefObject<Date>(absoluteExpiration);
            tangible.RefObject<TimeSpan> tempRef_sldExp = new tangible.RefObject<TimeSpan>(slidingExpiration);
            com.alachisoft.tayzgrid.caching.autoexpiration.DependencyHelper.GetCacheDependency(hint, tempRef_absExp, tempRef_sldExp);

            absoluteExpiration = tempRef_absExp.argvalue;
            slidingExpiration = tempRef_sldExp.argvalue;
            if (absoluteExpiration != Cache.DefaultAbsoluteExpiration)
            {
                item.setAbsoluteExpiration(absoluteExpiration);
            }
            if (slidingExpiration != TimeSpan.ZERO)
            {
                item.setSlidingExpiration(slidingExpiration);
            }
            else
            {
                item.setSlidingExpiration(Cache.DefaultSlidingExpiration);
            }

            if (entry.getGroupInfo() != null)
            {
                item.setGroup(entry.getGroupInfo().getGroup());
                item.setSubGroup(entry.getGroupInfo().getSubGroup());
            }
            item.setVersion(HelperUtil.createCacheItemVersion(entry.getVersion()));
            item._creationTime = entry.getCreationTime();
            item._lastModifiedTime = entry.getLastModifiedTime();

            if (entry.getQueryInfo() != null)
            {
                if (entry.getQueryInfo().get("tag-info") != null)
                {
                    HashMap tagInfo = entry.getQueryInfo().get("tag-info") instanceof HashMap ? (HashMap) entry.getQueryInfo().get("tag-info") : null;
                    ArrayList tagsList = tagInfo.get("tags-list") instanceof ArrayList ? (ArrayList) tagInfo.get("tags-list") : null;
                    Tag[] tags = new Tag[tagsList.size()];
                    int i = 0;
                    for (Object tag : tagsList)
                    {
                        tags[i++] = new Tag(tag.toString());
                    }

                    item.setTags(tags);
                }
                if (entry.getQueryInfo().get("named-tag-info") != null)
                {
                    HashMap tagInfo = entry.getQueryInfo().get("named-tag-info") instanceof HashMap ? (HashMap) entry.getQueryInfo().get("named-tag-info") : null;
                    HashMap tagsList = tagInfo.get("named-tags-list") instanceof HashMap ? (HashMap) tagInfo.get("named-tags-list") : null;
                    NamedTagsDictionary namedTags = new NamedTagsDictionary();

                    for (Object tagObj : tagsList.entrySet())
                    {
                        if (tagObj instanceof Map.Entry)
                        {
                            Map.Entry tag = (Map.Entry) tagObj;
                            Class tagType = tag.getValue().getClass();
                            String tagKey = tag.getKey().toString();

                            if (tag.getValue() instanceof java.lang.Integer)
                            {
                                namedTags.add(tagKey, (Integer) tag.getValue());
                            }
                            else if (tag.getValue() instanceof java.lang.Long)
                            {
                                namedTags.add(tagKey, (Long) tag.getValue());
                            }
                            else if (tag.getValue() instanceof java.lang.Float)
                            {
                                namedTags.add(tagKey, (Float) tag.getValue());
                            }
                            else if (tag.getValue() instanceof java.lang.Double)
                            {
                                namedTags.add(tagKey, (Double) tag.getValue());
                            }
                            else if (tag.getValue() instanceof java.lang.Boolean)
                            {
                                namedTags.add(tagKey, (Boolean) tag.getValue());
                            }
                            else if (tag.getValue() instanceof java.lang.Character)
                            {
                                namedTags.add(tagKey, (Character) tag.getValue());
                            }
                            else if (tag.getValue() instanceof java.lang.String)
                            {
                                namedTags.add(tagKey, (String) tag.getValue());
                            }
                            else if (tag.getValue() instanceof java.util.Date)
                            {
                                namedTags.add(tagKey, (Date) tag.getValue());
                            }
                        }
                    }
                    item.setNamedTags(namedTags);
                }
            }
            return item;
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return null;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Tags Methods ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    /**
     * Returns the cached objects that have all the same tags in common. (Returns the Intersection set.)
     *
     * @param tags An array of Tag to search with.
     * @return A HashMap containing cache keys and associated objects.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws Exception Thrown when an exception occurs while performing
     * operation.
     */
    public HashMap getByAllTags(Tag[] tags)
            throws GeneralFailureException, OperationFailedException, AggregateException,  Exception
    {
        return getByTag(tags, com.alachisoft.tayzgrid.caching.TagComparisonType.ALL_MATCHING_TAGS);
    }

    /**
     * Returns the cached objects that have all the same tags in common. (Returns the Intersection set.)
     *
     * @param tags An array of Tag to search with.
     * @return A Collection containing cache keys.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws Exception Thrown when an exception occurs while performing
     * operation.
     */
    public Collection getKeysByAllTags(Tag[] tags)
            throws GeneralFailureException, OperationFailedException, AggregateException,  Exception
    {
        return getKeysByTag(tags, com.alachisoft.tayzgrid.caching.TagComparisonType.ALL_MATCHING_TAGS);
    }

    /**
     * Returns the cached objects that have any of the same tags in common. (Returns the Union set.)
     *
     * @param tags An array of Tag to search with.
     * @return A HashMap containing cache keys and associated objects.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception Thrown when an exception occurs while performing
     * operation.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     */
    public HashMap getByAnyTag(Tag[] tags)
            throws GeneralFailureException, OperationFailedException, AggregateException,  Exception
    {
        return getByTag(tags, com.alachisoft.tayzgrid.caching.TagComparisonType.ANY_MATCHING_TAG);
    }

    /**
     * Returns the cached objects that have any of the same tags in common. (Returns the Union set.)
     *
     * @param tags An array of Tag to search with.
     * @return A Collection containing cache keys.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws Exception Thrown when an exception occurs while performing
     * operation.
     */
    public Collection getKeysByAnyTag(Tag[] tags)
            throws GeneralFailureException, OperationFailedException, AggregateException,  Exception
    {
        return getKeysByTag(tags, com.alachisoft.tayzgrid.caching.TagComparisonType.ANY_MATCHING_TAG);
    }

    /**
     * Gets all the cached objects with the specified tag.
     *
     * @param tag The tag to search with.
     * @return A HashMap containing cache keys and associated objects.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws Exception Thrown when an exception occurs while performing
     * operation.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     */
    public HashMap getByTag(Tag tag)
            throws GeneralFailureException, OperationFailedException, AggregateException,  Exception
    {
        return getByTag(new Tag[]
                {
                    tag
                }, com.alachisoft.tayzgrid.caching.TagComparisonType.BY_TAG);
    }

    /**
     * Gets all the cached objects with the specified tag.
     *
     * @param tag The tag to search with.
     * @return A Collection containing cache keys.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws Exception Thrown when an exception occurs while performing
     * operation.
     */
    public Collection getKeysByTag(Tag tag)
            throws GeneralFailureException, OperationFailedException, AggregateException,  Exception
    {
        return getKeysByTag(new Tag[]
                {
                    tag
                }, com.alachisoft.tayzgrid.caching.TagComparisonType.BY_TAG);
    }

    private HashMap getByTag(Tag[] tags, com.alachisoft.tayzgrid.caching.TagComparisonType comparisonType)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ArgumentNullException, ConnectionException, Exception
    {

        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (tags == null)
        {
            throw new OperationFailedException("Tag[] cannot be null.");
        }
        tangible.RefObject<Tag[]> tagRef = new tangible.RefObject<Tag[]>(tags);
        RemoveDuplicateTags(tagRef);
        try
        {
            HashMap resultMap = _cacheImpl.getByTag(tags, comparisonType);
            if (resultMap == null)
            {
                if (DebugAPIConfigurations.isLoggingEnabled()) {
                    logSizeInfo(_encryptionEnabled, true, 0, 0, 0, 0);
                }
                return null;
            }

            long sumObjectSize = 0;
            long sumCompressedObjectSize = 0;
            long sumEncryptedObjectSize = 0;

            HashMap deflatMap = new HashMap(resultMap.size());
            ///Do decompression (if necessary) and deserialization of data
            ///and add to a new table

            for (Object entry : resultMap.entrySet())
            {
                Map.Entry<String, CompressedValueEntry> pair =
                        (Map.Entry<String, CompressedValueEntry>) entry;

                CompressedValueEntry valEntry = pair.getValue();
                if (valEntry.Value instanceof byte[])
                {
                    byte[] deflatValue = (byte[]) valEntry.Value;

                    if (DebugAPIConfigurations.isLoggingEnabled()) {
                        sumCompressedObjectSize += deflatValue.length;
                    }

                    if (isPerfStatsCollectorInitialized() && deflatValue != null) {
                        _perfStatsCollector.incrementAvgItemSize(((byte[]) deflatValue).length);
                    }
                    
                    if (DebugAPIConfigurations.isLoggingEnabled()) {
                        sumObjectSize += deflatValue.length;
                    }

                    Object obj = null;
                    try
                    {
                        ByteArrayInputStream val = new ByteArrayInputStream((byte[]) deflatValue);
                        ObjectInput ow = new ObjectInputStream(val, this.cacheId);
                        obj = ow.readObject();
                    }
                    catch (IOException iOException)
                    {
                        throw new GeneralFailureException(iOException.getMessage());
                    }
                    catch (ClassNotFoundException classNotFoundException)
                    {
                        throw new GeneralFailureException(classNotFoundException.getMessage());
                    }
                    deflatMap.put(pair.getKey(), obj);

                }
                else
                {
                    deflatMap.put(pair.getKey(), valEntry.Value);
                }
            }
            if (DebugAPIConfigurations.isLoggingEnabled()) {
                logSizeInfo(_encryptionEnabled, true, sumObjectSize, sumEncryptedObjectSize, sumCompressedObjectSize, deflatMap.size());
            }
            return deflatMap;
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }

        return null;
    }

    private Collection getKeysByTag(Tag[] tags, com.alachisoft.tayzgrid.caching.TagComparisonType comparisonType)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ArgumentNullException, ConnectionException, Exception
    {

        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (tags == null)
        {
            throw new OperationFailedException("Tag[] cannot be null.");
        }

        tangible.RefObject<Tag[]> tagRef = new tangible.RefObject<Tag[]>(tags);
        RemoveDuplicateTags(tagRef);
        try
        {
            return _cacheImpl.getKeysByTag(tags, comparisonType);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }

        return null;
    }

    /**
     * Removes the cached objects that have the specified tag.
     *
     * @param tag - A Tag to search with.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access
     * cache.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws ArgumentNullException Thrown when Operation was failed due to a
     * null argument.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void removeByTag(Tag tag) throws GeneralFailureException, OperationFailedException, ArgumentNullException, AggregateException,  ConnectionException, Exception
    {
        if (tag == null || tag.getTagName() == null)
        {
            throw new OperationFailedException("Tag cannot be null.");
        }

        _cacheImpl.removeByTag(new Tag[]
                {
                    tag
                }, com.alachisoft.tayzgrid.caching.TagComparisonType.BY_TAG);

    }

    /**
     * Removes the cached objects that have any of the same tags in common. (Returns the Union set.)
     *
     * @param tags - An array of Tag to search with.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access
     * cache.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws ArgumentNullException Thrown when Operation was failed due to a
     * null argument.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void removeByAnyTag(Tag[] tags) throws GeneralFailureException, OperationFailedException, ArgumentNullException, AggregateException,  ConnectionException, Exception
    {
        if (tags == null)
        {
            throw new OperationFailedException("Tag[] cannot be null.");
        }
        if (tags.length == 0)
        {
            throw new OperationFailedException("Atleast one Tag required.");
        }
        tangible.RefObject<Tag[]> tagRef = new tangible.RefObject<Tag[]>(tags);
        RemoveDuplicateTags(tagRef);
        _cacheImpl.removeByTag(tags, com.alachisoft.tayzgrid.caching.TagComparisonType.ANY_MATCHING_TAG);
    }

    /**
     * Removes the cached objects that have all of the same tags in common. (Returns the Intersection set.)
     *
     * @param tags - An array of Tag to search with.
     * @throws GeneralFailureException
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown if the user is not authorized to access
     * cache.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws ArgumentNullException Thrown when Operation was failed due to a
     * null argument.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void removeByAllTags(Tag[] tags) throws GeneralFailureException, OperationFailedException, ArgumentNullException, AggregateException,  ConnectionException, Exception
    {
        if (tags == null)
        {
            throw new OperationFailedException("Tag[] cannot be null.");
        }
        if (tags.length == 0)
        {
            throw new OperationFailedException("Atleast one Tag required.");
        }
        tangible.RefObject<Tag[]> tagRef = new tangible.RefObject<Tag[]>(tags);
        RemoveDuplicateTags(tagRef);
        _cacheImpl.removeByTag(tags, com.alachisoft.tayzgrid.caching.TagComparisonType.ALL_MATCHING_TAGS);
    }

    // </editor-fold>

    /*
    * this method is used from the sync cache to synchronize the L1 cache with L2.
    * getting a serialized object reduces the cost as we need to put the same object again in the cache.
    */
    Object getSerializedObject(Object key, DSReadOption dsReadOption, tangible.RefObject<Long> v, tangible.RefObject<BitSet> flag) throws Exception
    {
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (key == null)
        {
            throw new ArgumentNullException("key");
        }

        com.alachisoft.tayzgrid.caching.CompressedValueEntry result = null;
        try
        {
            BitSet flagMap = new BitSet();

            flagMap = this.setDSReadOptBit(flagMap, dsReadOption);

            LockHandle lockHandle = new LockHandle();
            CacheItemVersion version = HelperUtil.createCacheItemVersion(0);
            result = _cacheImpl.get(key, flagMap, null, null, version, lockHandle, TimeSpan.ZERO, com.alachisoft.tayzgrid.caching.LockAccessType.GET_VERSION, null);


            //set the version...
            if (version != null)
            {
                v.argvalue = version.getVersion();
            }
            flag.argvalue.setData(result.Flag.getData());

            if (result != null && result.Value != null)
            {
                if (result.Value instanceof CallbackEntry)
                {
                    CallbackEntry e = (result.Value instanceof CallbackEntry) ? (CallbackEntry) result.Value : null;
                    result.setValue((byte[]) e.getValue());
                }
                
                if (isPerfStatsCollectorInitialized() && result.Value != null && result.Value instanceof byte[]) {
                    _perfStatsCollector.incrementAvgItemSize(((byte[]) result.Value).length);
                }
                return result.Value;

            }
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
            else
            {
                return null;
            }
        }
        return null;
    }

    /**
     * *
     * Gets an object from the cache only if a newer version of the object exists in cache.
     *
     * @param key key used to reference the desired object
     * @param version The version of the desired object passed by reference.
     * @return The version of the desired object passed by reference.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object getIfNewer(Object key, CacheItemVersion version)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return this.getIfNewer(key, null, null, version);
    }

    /**
     * *
     * Gets an object from the cache only if a newer version of the object exists in cache.
     *
     * @param key key used to reference the desired object
     * @param group key used to reference the desired object
     * @param subGroup The group of the cached object
     * @param version The subGroup of the cached object
     * @return The version of the desired object passed by reference.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public Object getIfNewer(Object key, String group, String subGroup, CacheItemVersion version)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        return this.get(key, group, subGroup, DSReadOption.None, version, NoLockingExpiration, null, com.alachisoft.tayzgrid.caching.LockAccessType.COMPARE_VERSION, null);
    }

    // <editor-fold defaultstate="collapsed" desc="Locking Methods">
    /**
     * *
     * Acquire a lock on an item in cache.
     *
     * @param key key of cached item to be locked.
     * @param lockTimeout TimeSpan after which the lock is automatically released.
     * @param lockHandle An instance of LockHandle that will be filled in with the lock information if lock is acquired successfully.
     * @return True if the lock was acquired successfully, false otherwise
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public boolean lock(Object key, TimeSpan lockTimeout, LockHandle lockHandle)
            throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception
    {
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (key == null)
        {
            throw new IllegalArgumentException("key is null.");
        }

        boolean lockAcquired = false;
        try
        {
            lockAcquired = _cacheImpl.lock(key, lockTimeout, lockHandle);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return lockAcquired;
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
    boolean isLocked(Object key, LockHandle lockHandle)
            throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception
    {
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (key == null)
        {
            throw new IllegalArgumentException("key is null.");
        }

        try
        {
            return _cacheImpl.isLocked(key, lockHandle);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return false;
    }

    /**
     * *
     * Forcefully unlocks a locked cached item.
     *
     * @param key key of a cached item to be unlocked
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void unlock(Object key)
            throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception
    {
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (key == null)
        {
            throw new IllegalArgumentException("key is null.");
        }
        try
        {
            _cacheImpl.unlock(key);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
    }

    /**
     * Unlocks a locked cached item if the correct lock-id is specified.
     *
     * @param key key of a cached item to be unlocked
     * @param lockHandle An instance of LockHandle that was generated when lock was acquired
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void unlock(Object key, LockHandle lockHandle)
            throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception
    {
        unlockOperation(key, lockHandle);
    }

    private void unlockOperation(Object key, LockHandle lockHandle)
            throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception
    {

        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (key == null)
        {
            throw new IllegalArgumentException("key is null.");
        }
        String lockId = (lockHandle == null) ? null : lockHandle.getLockId();
        try
        {
            _cacheImpl.unlock(key, lockId);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~DoBulkAddInsert~~~~~~~~~~~~~~~~~">
    /**
     * @return @param keys The cache key used to reference the item.
     * @param items
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     */
    private HashMap DoBulkAdd(Object[] keys, CacheItem[] items, DSWriteOption dsWriteOption, String providerName, 
            DataSourceItemsAddedCallback onDataSourceItemAdded, RefObject<Long[]> refSize)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, ConnectionException, Exception
    {
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if ((dsWriteOption == DSWriteOption.WriteThru || dsWriteOption == DSWriteOption.WriteBehind) && providerName == null)
        {
            providerName = _defaultWriteThruProvider;
        }

        if (providerName != null && !providerName.trim().equals(""))
        {
            providerName = providerName.toLowerCase();
        }

        if (keys.length != items.length)
        {
            throw new OperationFailedException("Keys count is not equal to items count");
        }

        HashMap queryInfo = null;
        com.alachisoft.tayzgrid.common.BitSet flagMap;
        CacheItem[] clonedItems = new CacheItem[items.length];

        int[] updateCallbackIds = new int[items.length];
        int[] removeCallbackIds = new int[items.length];

        long sumCompressedObjectSize = 0;
        long sumEncryptedObjectSize = 0;
        long sumObjectSize = 0;

        for (int i = 0; i < items.length; i++)
        {
            if (keys[i] == null)
            {
                throw new IllegalArgumentException("Key cannot be null");
            }
            if (items[i] == null)
            {
                throw new IllegalArgumentException("CacheItem cannot be null");
            }
            if (items[i].getSubGroup() != null && items[i].getGroup() == null)
            {
                throw new IllegalArgumentException("Group cannot be null when sub-group is provided");
            }
            
            flagMap = new com.alachisoft.tayzgrid.common.BitSet();
            CacheItem cloned = (CacheItem) items[i].Clone();
            if(_defaultExpiration!=null){
            HashMap<ExpirationType,Object> resolutionMap = _defaultExpiration.resolveClientExpirations(cloned.getAbsoluteExpiration(), cloned.getSlidingExpiration());
            cloned.setAbsoluteExpiration((Date) resolutionMap.get(ExpirationType.FixedExpiration));
            cloned.setSlidingExpiration((TimeSpan) resolutionMap.get(ExpirationType.SlidingExpiration));
            queryInfo = new HashMap();
            }

            //No need to send query tag info for client cache
            if (_allowQueryTags) {
                HashMap htQueryInfo = getQueryInfo(cloned.getValue());
                if (htQueryInfo != null) {
                    queryInfo.put("query-info", htQueryInfo);
                }

                HashMap htNamedTagInfo = getNamedTagsInfo(cloned.getValue(), cloned.getNamedTags());
                if (htNamedTagInfo != null) {
                    queryInfo.put("named-tag-info", htNamedTagInfo);
                }

                ArrayList<Tag> validTags = new ArrayList<Tag>();
                HashMap htTagInfo = getTagInfo(cloned.getValue(), cloned.getTags(), validTags);
                if (htTagInfo != null) {
                    queryInfo.put("tag-info", htTagInfo);
                }
            }

            cloned.setQueryInfo(queryInfo);

            RefObject<Long> refValue = new RefObject<Long>(refSize.argvalue[i]);
            cloned.setValue(getSerializedBytes(cloned.getValue(),_serializationContext ,flagMap, refValue));
            refSize.argvalue[i]=refValue.argvalue;
            if (DebugAPIConfigurations.isLoggingEnabled() && cloned.getValue() != null && _cacheImpl.getSerializationEnabled())
            {
                sumObjectSize += ((byte[]) cloned.getValue()).length;
            }
            
            if (isPerfStatsCollectorInitialized())
            {
                if (cloned.getValue() != null && cloned.getValue() instanceof byte[])
                {
                    _perfStatsCollector.incrementAvgItemSize(((byte[]) cloned.getValue()).length);
                }
            }

            flagMap = this.setDSUpdateOptBit(flagMap, dsWriteOption);
            cloned.setFlag(flagMap);
            cloned.setResyncProviderName(items[i].getResyncProviderName());
            clonedItems[i] = cloned;

            updateCallbackIds[i] = -1;
            removeCallbackIds[i] = -1;

            
            CacheDataModificationListener cacheItemUpdateCallback = items[i].getCacheItemUpdatedListener();
            CacheDataModificationListener cacheItemRemoveCallback = items[i].getCacheItemRemovedListener();

            if(cacheItemUpdateCallback != null)
            {
                short[] callabackIds = _eventManager.registerSelectiveEvent(cacheItemUpdateCallback, EnumSet.of(EventType.ItemUpdated), EventDataFilter.None);
                    updateCallbackIds[i] = callabackIds[0];
            }
            
            if(cacheItemRemoveCallback != null)
            {
                short[] callabackIds = _eventManager.registerSelectiveEvent(cacheItemRemoveCallback, EnumSet.of(EventType.ItemRemoved), EventDataFilter.None);
                    removeCallbackIds[i] = callabackIds[1];
            }
        }
        
        if (DebugAPIConfigurations.isLoggingEnabled()) {
            logSizeInfo(_encryptionEnabled, true, sumObjectSize, sumEncryptedObjectSize, sumCompressedObjectSize, items.length);
        }

        short dsItemUpdateCallbackId = -1;
        if (onDataSourceItemAdded != null)
        {
            dsItemUpdateCallbackId = GetCallbackId(onDataSourceItemAdded,keys.length);
        }

        try
        {
           
            Long[] ref_sz=refSize.argvalue;
            long[] sizes= new long[ref_sz.length];
            for (int i1 = 0; i1 < ref_sz.length; i1++) {
                sizes[i1] = ref_sz[i1];
            }
            return _cacheImpl.add(keys, clonedItems, removeCallbackIds, updateCallbackIds, /*(short) _cacheImpl.getCallbackQueue().indexOf(onDataSourceItemAdded)*/
                    dsItemUpdateCallbackId, providerName, sizes);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw new OperationFailedException(e.getMessage());
            }
        }
        return null;
    }

    /**
     * @return @param keys The cache key used to reference the item.
     * @param items
     * @param update
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     */
    private HashMap DoBulkInsert(Object[] keys, CacheItem[] items, DSWriteOption dsWriteOption, String providerName, DataSourceItemsUpdatedCallback onDataSourceItemUpdated,RefObject<Long[]> refSize)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, ConnectionException, ConnectionException, Exception
    {

        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if ((dsWriteOption == DSWriteOption.WriteThru || dsWriteOption == DSWriteOption.WriteBehind) && providerName == null)
        {
            providerName = _defaultWriteThruProvider;
        }

        if (providerName != null && !providerName.trim().equals(""))
        {
            providerName = providerName.toLowerCase();
        }

        if (keys.length != items.length)
        {
            throw new OperationFailedException("Keys count is not equal to items count");
        }

        HashMap queryInfo = null;
        com.alachisoft.tayzgrid.common.BitSet flagMap;
        CacheItem[] clonedItems = new CacheItem[items.length];

        int[] updateCallbackIds = new int[items.length];
        int[] removeCallbackIds = new int[items.length];

        long sumObjectSize = 0;
        long sumEncryptedObjectSize = 0;
        long sumCompressedObjectSize = 0;

        for (int i = 0; i < items.length; i++)
        {
            if (keys[i] == null)
            {
                throw new IllegalArgumentException("Key cannot be null");
            }
            if (items[i] == null)
            {
                throw new IllegalArgumentException("CacheItem cannot be null");
            }
            if (items[i].getSubGroup() != null && items[i].getGroup() == null)
            {
                throw new IllegalArgumentException("Group cannot be null when sub-group is provided");
            }
            
            flagMap = new com.alachisoft.tayzgrid.common.BitSet();
            CacheItem cloned = (CacheItem) items[i].Clone();
            
            if(_defaultExpiration!=null){
                HashMap<ExpirationType,Object> resolutionMap = _defaultExpiration.resolveClientExpirations(cloned.getAbsoluteExpiration(), cloned.getSlidingExpiration());
                cloned.setAbsoluteExpiration((Date) resolutionMap.get(ExpirationType.FixedExpiration));
                cloned.setSlidingExpiration((TimeSpan) resolutionMap.get(ExpirationType.SlidingExpiration));
        }
            queryInfo = new HashMap();
            
            //No need to send query tag info for client cache
            if (_allowQueryTags) {
                HashMap htQueryInfo = getQueryInfo(cloned.getValue());
                if (htQueryInfo != null) {
                    queryInfo.put("query-info", htQueryInfo);
                }

                HashMap htNamedTagInfo = getNamedTagsInfo(cloned.getValue(), cloned.getNamedTags());
                if (htNamedTagInfo != null) {
                    queryInfo.put("named-tag-info", htNamedTagInfo);
                }

                ArrayList<Tag> validTags = new ArrayList<Tag>();
                HashMap htTagInfo = getTagInfo(cloned.getValue(), cloned.getTags(), validTags);
                if (htTagInfo != null) {
                    queryInfo.put("tag-info", htTagInfo);
                }
            }

            cloned.setQueryInfo(queryInfo);

            RefObject<Long> refValue = new RefObject<Long>(refSize.argvalue[i]);
            cloned.setValue(getSerializedBytes(cloned.getValue(), _serializationContext,flagMap,refValue));
            refSize.argvalue[i]=refValue.argvalue;
            if (DebugAPIConfigurations.isLoggingEnabled() && cloned.getValue() != null && _cacheImpl.getSerializationEnabled())
            {
                sumObjectSize += ((byte[]) cloned.getValue()).length;
            }
            
            if (_perfStatsCollector != null)
            {
                if (cloned.getValue() != null && cloned.getValue() instanceof byte[])
                {
                    _perfStatsCollector.incrementAvgItemSize(((byte[]) cloned.getValue()).length);
                }
            }

            flagMap = this.setDSUpdateOptBit(flagMap, dsWriteOption);
            cloned.setFlag(flagMap);

            clonedItems[i] = cloned;

            updateCallbackIds[i] = -1;
            removeCallbackIds[i] = -1;

            
            CacheDataModificationListener cacheItemUpdatedCallback = items[i].getCacheItemUpdatedListener();
            CacheDataModificationListener cacheItemRemovedCallback = items[i].getCacheItemRemovedListener();

            if (cacheItemUpdatedCallback != null)
            {
                short[] callabackIds = _eventManager.registerSelectiveEvent(cacheItemUpdatedCallback , EnumSet.of(EventType.ItemUpdated), EventDataFilter.None);
                    updateCallbackIds[i] = callabackIds[0];
                
            }

            if (cacheItemRemovedCallback != null)
            {
                short[] callabackIds = _eventManager.registerSelectiveEvent(cacheItemRemovedCallback, EnumSet.of(EventType.ItemRemoved), EventDataFilter.None);
                    removeCallbackIds[i] = callabackIds[1];
            }
        }
        if (DebugAPIConfigurations.isLoggingEnabled()) {
            logSizeInfo(_encryptionEnabled, true, sumObjectSize, sumEncryptedObjectSize, sumCompressedObjectSize, items.length);
        }


        short dsItemUpdateCallbackId = -1;
        if (onDataSourceItemUpdated != null)
        {
            dsItemUpdateCallbackId = GetCallbackId(onDataSourceItemUpdated,keys.length);
        }
        try
        {
            Long[] ref_sz=refSize.argvalue;
            long[] sizes= new long[ref_sz.length];
            for (int i1 = 0; i1 < ref_sz.length; i1++) {
                sizes[i1] = ref_sz[i1];
            }
            return _cacheImpl.insert(keys, clonedItems, removeCallbackIds, updateCallbackIds, /*(short) _cacheImpl.getCallbackQueue().indexOf(onDataSourceItemUpdated) */
                    dsItemUpdateCallbackId, providerName,sizes);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return null;
    }
// </editor-fold>

    //<editor-fold defaultstate="collapsed" desc="/                --- AddDependency ---       /">



    /**
     * Add Attribute existing cache item
     *
     * @param key Key used to reference the required object
     * @param attributes Set of attributes to be added
     * @return True of the operation succeeds otherwise false
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple
     * exceptions occur from multiple nodes. It combines all the exceptions as
     * inner exceptions and throw it to the client application.
     * @throws GeneralFailureException Thrown when an exception occurs during a
     * clustered operation.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public boolean setAttributes(Object key, CacheItemAttributes attributes) throws OperationFailedException, GeneralFailureException,  AggregateException, ConnectionException, Exception
    {
        if (key == null)
        {
            throw new OperationFailedException("Key cannot be null");
        }

        if (attributes == null)
        {
            throw new OperationFailedException("Attributes cannot be null");
        }
        if(_defaultExpiration!=null){
            HashMap resolutionMap = _defaultExpiration.resolveClientExpirations(attributes.getAbsoluteExpiration(), DefaultSlidingExpiration);
            attributes.setAbsoluteExpiration((Date)resolutionMap.get(ExpirationType.FixedExpiration));
        }
        
        return _cacheImpl.setAttributes(key, attributes);
    }

    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="/                --- Key based notifications registration ---       /">
    /**
     * Register a listener to receive cache event when the specified key is modified.
     
     * @param keys array used to reference the cache item.
     * @param selectiveCacheDataNotificationCallback it is invoked when an item is added, updated or removed from the cache.
     * @param eventEnumSet Tells whether the event is to be raised on item updated or removed.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ArgumentException Thrown when Operation was failed due to an
     * invalid argument.
     * @throws ArgumentNullException Thrown when Operation was failed due to an
     * null argument.
     */
        public void addBulkCacheDataModificationListener(Object[] keys,
            CacheDataModificationListener selectiveCacheDataNotificationCallback,
            EnumSet<EventType> eventEnumSet) throws ArgumentNullException, ArgumentException, OperationFailedException, Exception {
            addBulkCacheDataModificationListener(keys, selectiveCacheDataNotificationCallback, eventEnumSet, EventDataFilter.None);
    }
     /**
     * Register a listener to receive cache event when the specified key is modified.
     
     * @param keys array used to reference the cache item.
     * @param selectiveCacheDataNotificationCallback it is invoked when an item is added, updated or removed from the cache.
     * @param eventEnumSet Tells whether the event is to be raised on item updated or removed.
     * @param dataFilter This filter is to describe when registering an event, upon raise how much data is retrieved from cache when the event is raised.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ArgumentException Thrown when Operation was failed due to an
     * invalid argument.
     * @throws ArgumentNullException Thrown when Operation was failed due to an
     * null argument.
      
     */
    public void addBulkCacheDataModificationListener(Object[] keys, 
            CacheDataModificationListener selectiveCacheDataNotificationCallback,
            EnumSet<EventType> eventEnumSet,
            EventDataFilter dataFilter) throws ArgumentNullException, ArgumentException, OperationFailedException, Exception
    {
        if(keys == null || keys.length == 0)
            throw new ArgumentNullException("key");
        
        if(selectiveCacheDataNotificationCallback == null)
            throw new ArgumentException("selectiveCacheDataNotificationCallback"); 
        
        if(eventEnumSet == null)
            throw new ArgumentException("eventEnumSet"); 
        addCacheDataModificationListener(keys, selectiveCacheDataNotificationCallback, eventEnumSet, dataFilter, true);
    }
    
    /**
     * Registers the CacheItemUpdatedCallback and/or CacheItemRemovedCallback for the specified key.
    
     * @param keys array used to reference the cache item.
     * @param listener it is invoked when an item is added, updated or removed from the cache.
     * @param eventEnumSet Tells whether the event is to be raised on item updated or removed.
     * @param dataFilter This filter is to describe when registering an event, upon raise how much data is retrieved from cache when the event is raised.
     * @param notifyOnItemExpiration Flag to determine whether to notify on item expiration or not.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     
     */
    protected void addCacheDataModificationListener(Object[] keys, CacheDataModificationListener listener, EnumSet<EventType> enumTypeSet, EventDataFilter dataFilter, boolean notifyOnItemExpiration) throws OperationFailedException, Exception
    {
        if(_cacheImpl == null) throw new OperationFailedException("Cache is not initialized");
        
        try
        {
            if(keys != null)
            {
                short[] callbackRefs = _eventManager.registerSelectiveEvent(listener, enumTypeSet, dataFilter);
                 _cacheImpl.registerKeyNotificationCallback(keys, callbackRefs[0], callbackRefs[1], dataFilter, notifyOnItemExpiration);
                
            }
        }
        catch(Exception e)
        {
            if( exceptionsEnabled )
                throw new Exception(e.getMessage(), e.getCause());
        }
    }
     /**
     * Register a listener to receive cache event when the specified key is modified.
    
     * @param cacheDataNotificationListener it is invoked when an item is added, updated or removed from the cache.
     * @param eventEnumSet Tells whether the event is to be raised on item updated or removed.
     * @param dataFilter This filter is to describe when registering an event, upon raise how much data is retrieved from cache when the event is raised.
     * @return returns a unique event descriptor used to unregister listener.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ArgumentException Thrown when Operation was failed due to an
     * invalid argument.
     */
    public CacheEventDescriptor addCacheDataModificationListener(CacheDataModificationListener cacheDataNotificationListener, EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter) throws ArgumentException, OperationFailedException, Exception
    {
        if (cacheDataNotificationListener == null)
                throw new ArgumentException("cacheDataNotificationCallback");
        
        if(eventEnumSet == null)
            throw new ArgumentException("eventEnumSet");

        Object key = null;//to get rid of method ambiguity error
        return addCacheDataModificationListener(key, cacheDataNotificationListener, eventEnumSet, dataFilter, true);
    } 
    
     /**
     * Registers the CacheItemUpdatedCallback and/or CacheItemRemovedCallback for the list of specified keys.
     *
     * CacheItemUpdatedCallback and/or CacheItemRemovedCallback provided this way are very useful because a client application can show interest in any item already present in the
     * cache. As soon as the item is updated or removed from the cache, the client application is notified and actions can be taken accordingly.
     *
     * @param key cache key used to reference the cache items.
     * @param updateCallback The CacheItemUpdatedCallback that is invoked if the item with the specified key is updated in the cache.
     * @param removeCallback The CacheItemRemovedCallback is invoked when the item with the specified key is removed from the cache.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @see CacheItemUpdatedCallback
     * @see CacheItemRemovedCallback
     */
    void registerKeyNotificationCallback(Object key, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback)
    throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
            registerKeyNotificationCallback(key, updateCallback, removeCallback, true);
    }
            
     /**
     * Registers the CacheItemUpdatedCallback and/or CacheItemRemovedCallback for the list of specified keys.
     *
     * CacheItemUpdatedCallback and/or CacheItemRemovedCallback provided this way are very useful because a client application can show interest in any item already present in the
     * cache. As soon as the item is updated or removed from the cache, the client application is notified and actions can be taken accordingly.
     *
     * @param key cache key used to reference the cache items.
     * @param updateCallback The CacheItemUpdatedCallback that is invoked if the item with the specified key is updated in the cache.
     * @param removeCallback The CacheItemRemovedCallback is invoked when the item with the specified key is removed from the cache.
     * @param notifyOnItemExpiration boolean flag to specify whether to notify on item expiration or not.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @see CacheItemUpdatedCallback
     * @see CacheItemRemovedCallback
     */
            void registerKeyNotificationCallback(Object key, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback, boolean notifyOnItemExpiration )
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (key == null)
        {
            throw new IllegalArgumentException("key");
        }
        if (updateCallback == null && removeCallback == null)
        {
            throw new IllegalArgumentException();
        }

        if (removeCallback != null)
        {
            if (_cacheImpl.getCallbackQueue().indexOf(removeCallback) == -1)
            {
                _cacheImpl.getCallbackQueue().add(removeCallback);
            }
        }
        if (updateCallback != null)
        {
            if (_cacheImpl.getCallbackQueue().indexOf(updateCallback) == -1)
            {
                _cacheImpl.getCallbackQueue().add(updateCallback);
            }
        }

        try
        {
            _cacheImpl.registerKeyNotificationCallback(key, (short) _cacheImpl.getCallbackQueue().indexOf(updateCallback), (short) _cacheImpl.getCallbackQueue().indexOf(removeCallback), notifyOnItemExpiration);
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
    }

    /**
     * Registers the CacheItemUpdatedCallback and/or CacheItemRemovedCallback for the list of specified keys.
     *
     * CacheItemUpdatedCallback and/or CacheItemRemovedCallback provided this way are very useful because a client application can show interest in any item already present in the
     * cache. As soon as the item is updated or removed from the cache, the client application is notified and actions can be taken accordingly.
     *
     * @param keys The list of the cache keys used to reference the cache items.
     * @param updateCallback The CacheItemUpdatedCallback that is invoked if the item with the specified key is updated in the cache.
     * @param removeCallback The CacheItemRemovedCallback is invoked when the item with the specified key is removed from the cache.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @see CacheItemUpdatedCallback
     * @see CacheItemRemovedCallback
     */
    void registerKeyNotificationCallback(Object[] keys, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (keys == null)
        {
            throw new IllegalArgumentException("Keys array can not be null");
        }
        //for null values in keys
        findDupliate(keys);
        if (keys.length == 0)
        {
            throw new IllegalArgumentException("Keys count can not be zero");
        }

        if (updateCallback == null && removeCallback == null)
        {
            throw new IllegalArgumentException();
        }

        if (removeCallback != null)
        {
            if (_cacheImpl.getCallbackQueue().indexOf(removeCallback) == -1)
            {
                _cacheImpl.getCallbackQueue().add(removeCallback);
            }
        }
        if (updateCallback != null)
        {
            if (_cacheImpl.getCallbackQueue().indexOf(updateCallback) == -1)
            {
                _cacheImpl.getCallbackQueue().add(updateCallback);
            }
        }
        try
        {
            _cacheImpl.registerKeyNotificationCallback(keys, (short) _cacheImpl.getCallbackQueue().indexOf(updateCallback), (short) _cacheImpl.getCallbackQueue().indexOf(removeCallback));
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
    }

    /**
     * Unregisters the CacheItemUpdatedCallback and/or CacheItemRemovedCallback already registered for the specified key.
     *
     * @param key The cache key used to reference the cache item.
     * @param updateCallback CacheItemUpdatedCallback that is invoked when the item with the specified key is updated in the cache.
     * @param removeCallback CacheItemRemovedCallback that is invoked when the item with the key is removed from the cache.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @see CacheItemUpdatedCallback
     * @see CacheItemRemovedCallback
     */
    void unRegisterKeyNotificationCallback(Object key, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (key == null)
        {
            throw new IllegalArgumentException("key");
        }
        if (updateCallback == null && removeCallback == null)
        {
            throw new IllegalArgumentException();
        }

        if (removeCallback != null)
        {
            if (_cacheImpl.getCallbackQueue().indexOf(removeCallback) == -1)
            {
                _cacheImpl.getCallbackQueue().add(removeCallback);
            }
        }
        if (updateCallback != null)
        {
            if (_cacheImpl.getCallbackQueue().indexOf(updateCallback) == -1)
            {
                _cacheImpl.getCallbackQueue().add(updateCallback);
            }
        }
        try
        {
            _cacheImpl.unRegisterKeyNotificationCallback(key, (short) _cacheImpl.getCallbackQueue().indexOf(updateCallback), (short) _cacheImpl.getCallbackQueue().indexOf(removeCallback));
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
    }

    /**
     * Unregisters the CacheItemUpdatedCallback and/or CacheItemRemovedCallback already registered for the specified list of keys.
     *
     * @param keys Keys to unregister.
     * @param updateCallback CacheItemUpdatedCallback that is invoked when the item with the specified key is updated in the cache.
     * @param removeCallback acheItemRemovedCallback that is invoked when the item with the key is removed from the cache.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @see CacheItemUpdatedCallback
     * @see CacheItemRemovedCallback
     */
    void unRegisterKeyNotificationCallback(Object[] keys, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }

        if (keys == null)
        {
            throw new IllegalArgumentException("key");
        }
        if (keys.length == 0)
        {
            throw new IllegalArgumentException("Keys count can not be zero");
        }
        if (updateCallback == null && removeCallback == null)
        {
            throw new IllegalArgumentException();
        }

        if (removeCallback != null)
        {
            if (_cacheImpl.getCallbackQueue().indexOf(removeCallback) == -1)
            {
                _cacheImpl.getCallbackQueue().add(removeCallback);
            }
        }
        if (updateCallback != null)
        {
            if (_cacheImpl.getCallbackQueue().indexOf(updateCallback) == -1)
            {
                _cacheImpl.getCallbackQueue().add(updateCallback);
            }
        }
        try
        {
            _cacheImpl.unRegisterKeyNotificationCallback(keys, (short) _cacheImpl.getCallbackQueue().indexOf(updateCallback), (short) _cacheImpl.getCallbackQueue().indexOf(removeCallback));
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
    }

    //</editor-fold>
    
    /**
     * Raises a custom event.
     *
     * @param key The key of the event.
     * @param value The value.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void raiseCustomEvent(Object key, Object value)
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        if (_cacheImpl == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        try
        {
            long size = 0;
            RefObject<Long> refSize = new RefObject<Long>(size);
            
            value = this.getSerializedBytes(value,_serializationContext ,new com.alachisoft.tayzgrid.common.BitSet(), refSize);
            key = this.getSerializedBytes(key, _serializationContext,new com.alachisoft.tayzgrid.common.BitSet(), refSize);
            _cacheImpl.raiseCustomEvent(key, value);
        }
        catch (Exception e5)
        {
            if (isExceptionsEnabled())
            {
                throw e5;
            }
        }
    }

    /**
     * Returns the cache count. Note that this count is the total item count in the whole cluster.
     *
     * @return The count of the cache elements.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public long getCount()
            throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {

        if (cacheId == null)
        {
            throw new OperationFailedException("Cache is not initialized");
        }
        try
        {
            if (_cacheImpl != null)
            {
                return _cacheImpl.getCount();
            }
        }
        catch (Exception e)
        {
            if (isExceptionsEnabled())
            {
                throw e;
            }
        }
        return 0;
    }

    /**
     * Disposes this cache instance.
     *
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     */
    public void dispose() throws GeneralFailureException, OperationFailedException, ConfigurationException
    {
        dispose(true);
    }

    private void dispose(boolean disposing)
            throws GeneralFailureException, OperationFailedException, ConfigurationException
    {
        synchronized (this)
        {
            _refCounter--;
            if (_refCounter > 0)
            {
                return;
            }
            else if (_refCounter < 0)
            {
                _refCounter = 0;
            }
        }
        synchronized (TayzGrid.getCaches())
        {
            if (cacheId != null)
            {
                TayzGrid.getCaches().removeCache(cacheId);
                SerializationUtil.unRegisterCache(cacheId);
                cacheId = null; //bug 962 Set the cache id to null; this means that cache is disposed.
            }
        }
        if (_cacheImpl != null)
        {
            _cacheImpl.dispose(disposing);
        }

        if (_secondaryInprocInstances != null)
        {
            for (Object cache : _secondaryInprocInstances)
            {
                ((Cache) cache).dispose();
            }
        }

        _cacheImpl = null;

    }
    
    //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Serialize/Deserialize ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    private Object getSerializedBytes(Object value,String serializationContext ,com.alachisoft.tayzgrid.common.BitSet flag, RefObject<Long> size) throws GeneralFailureException, OperationFailedException
    {
        if (value != null ) {
            if ((value instanceof byte[]) && flag != null) {
                flag.SetBit((byte) BitSetConstants.BinaryData);
                size.argvalue=(long)((byte[])value).length;
                return value;
            }

            if (_cacheImpl == null) {
                throw new OperationFailedException("Cache is not initialized");
            }

            if (value != null) {
                UsageStats statsSerialization = new UsageStats();
                statsSerialization.BeginSample();
                value = _cacheImpl.SafeSerialize(value, serializationContext.toLowerCase(), flag, _cacheImpl, size);

                statsSerialization.EndSample();
                if (_perfStatsCollector != null) {
                    _perfStatsCollector.incrementMSecPerSerializaion(statsSerialization.getCurrent());
                }
            }
            return value;
        } else {
            return value;
        }
    }
    
    Object getDeserializedEntry(com.alachisoft.tayzgrid.caching.CompressedValueEntry result) throws GeneralFailureException
    {
        Object obj = null;
        if (result != null && result.Value != null)
            {

                if (result.Value instanceof byte[])
                {
                    
                    if(isPerfStatsCollectorInitialized() && result.Value !=null && result.Value instanceof byte[])
                        _perfStatsCollector.incrementAvgItemSize(((byte[]) result.Value).length);

//                    if (DebugAPIConfigurations.isLoggingEnabled() && result.Value instanceof byte[]) {
//                        objectSize = ((byte[]) result.Value).length;
//                    }
                    obj = Cache.this.getDeserializedObject(result.Value,_serializationContext, result.Flag); // we need flag of result entry for deserialization

                }
                else
                {
                    obj = result.Value;
                }
            }
        return obj;
    }
    
    /*
    Performs all bunary operations including serialization, encryption and compression
    */
    Object getSerializedEntry(Object value, BitSet flagMap) throws GeneralFailureException, OperationFailedException
    {
        long sz = 0;
        RefObject<Long> size = new RefObject<Long>(sz);
        value = getSerializedBytes(value,_serializationContext, flagMap, size);
        
        if (isPerfStatsCollectorInitialized() && value != null && value instanceof byte[]) {
            _perfStatsCollector.incrementAvgItemSize(((byte[]) value).length);
        }
        return value;
    }

    //Can be accesed within package and in by the class itself. Not available to 'world' and 'subclasses'
    Object getDeserializedObject(Object value,String serializationContext, com.alachisoft.tayzgrid.common.BitSet flag) throws GeneralFailureException
    {
        if (value instanceof byte[])
        {
            try
            {
                if (flag != null && flag.IsBitSet((byte) BitSetConstants.BinaryData))
                {
                    return value;
                }
                
                UsageStats statsSerialization=new UsageStats();
                statsSerialization.BeginSample();
//                 // <editor-fold defaultstate="collapsed" desc="Previous code">
//                ByteArrayInputStream val = new ByteArrayInputStream((byte[]) value);
//                ObjectInput ow = new ObjectInputStream(val, getSerializationContext());
//                Object result = ow.readObject();
//                // </editor-fold>
                value= _cacheImpl.SafeDeserialize(value, serializationContext.toLowerCase()
                        , flag,_cacheImpl);
                statsSerialization.EndSample();
                if(_perfStatsCollector!=null)
                    _perfStatsCollector.incrementMSecPerSerializaion(statsSerialization.getCurrent());
                return value;
            }
            
            catch (Exception iOException)
            {
                return value;
            }
//            catch (IOException iOException)
//            {
//                return value;
//            }
//            catch (ClassNotFoundException classNotFoundException)
//            {
//                throw new GeneralFailureException(classNotFoundException.getMessage());
//            }
        }
        else
        {
            return value;
        }
    }
    
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~ Register Notifications ~~~~~~~~~~~~~~~~~~~~~~~~~~~">
    /**
     * Registers the specified listener that with the specified notification type.
     *
     * @return void
     * @param listener A listener to be registered.
     * @param registerAgainst Notification type against which specified listener has to register.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     */
    void registerCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> registerAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentNullException, ConnectionException
    {

        if (registerAgainst != null)
        {
            _cacheImpl.registerCacheEventlistener(listener, registerAgainst);
        }
        else
        {
            throw new ArgumentNullException("registerAgainst");
        }
    }

    /**
     * Unregisters the specified listener that was registered with the specified notification type.
     *
     * @return void
     * @param listener Registered cache listener.
     * @param registerAgainst Notification type against which specified listener is registered.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     */
    void unregisterCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, ArgumentException
    {
        if (unregisterAgainst != null)
        {
            _cacheImpl.unregisterCacheEventlistener(listener, unregisterAgainst);
        }
        else
        {
            throw new ArgumentException("unregisterAgainst");
        }
    }

    /**
     * Registers the specified listener that with the specified cache status notification type.
     *
     * @param listener A listener to be registered.
     * @param registerAgainst Cache status notification type against which specified listener has to register.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ArgumentNullException Thrown when Operation was failed due to an
     * null argument.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void addCacheStatusEventlistener(CacheStatusEventListener listener, EnumSet<CacheStatusNotificationType> registerAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, ArgumentNullException
    {
        if (registerAgainst != null)
        {
            _cacheImpl.registerCacheStatusEventlistener(listener, registerAgainst);
        }
        else
        {
            throw new ArgumentNullException("registerAgainst");
        }
    }
    
    /**
     * Unregisters the specified listener that was registered with the specified cache status notification type.
     *
     * @param listener Registered cache listener
     * @param unregisterAgainst Cache status notification type against which specified listener is registered.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws SecurityException Thrown when current user is not allowed to perform this operation on this cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ArgumentNullException Thrown when Operation was failed due to an
     * null argument.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void removeCacheStatusEventlistener(CacheStatusEventListener listener, EnumSet<CacheStatusNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, ArgumentNullException
    {

        if (unregisterAgainst != null)
        {
            _cacheImpl.unregisterCacheStatusEventlistener(listener, unregisterAgainst);
        }
        else
        {
            throw new ArgumentNullException("unregisterAgainst");
        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~ C u s t o m E v e n t   L i s t e n e r ~~~~~~~~~~~~~~~~~~~~~~~~~">
    /**
     * Adds the specified custom listener to receive custom cache events from this cache. If CustomListener listener is null, no exception is thrown or no action is performed.
     * @param listener custom listener to add.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown whenever connection with cache server is lost while performing an operation on an outproc cache.
     */
    public synchronized void addCustomEventListener(CustomListener listener) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        _cacheImpl.registerCustomEventListener(listener);
    }
    /**
     * Removes the specified custom listener that was registered to receive custom cache events from this cache.
     * @param listener custom listener to remove.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown whenever connection with cache server is lost while performing an operation on an outproc cache.
     */
    public synchronized void removeCustomEventListener(CustomListener listener) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
    {
        _cacheImpl.unregisterCustomEventListener(listener);
    }
    //</editor-fold>
    
    // <editor-fold desc=" ------- Aggregator Methods ----------">
    /**
     * Executes the provided aggregate function on all keys present in the cache.
     * 
     * @param extractor instance of value extractor.
     * @param aggregator instance of aggregator.
     * @param timeOut Time after which task is considered as failed.
     * @return returns the result of the aggregate function.
     * @throws ArgumentNullException Thrown when Operation was failed due to a
     * null argument.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public Object aggregate(ValueExtractor extractor, Aggregator aggregator, long timeOut) throws ArgumentNullException, OperationFailedException, Exception {
        return aggregate(extractor, aggregator, null, null, null, timeOut);
    }
    
    /**
     * Executes the provided aggregate function on all keys present in the cache.
     * 
     * @param extractor instance of value extractor.
     * @param aggregator instance of aggregator.
     * @return returns the result of the aggregate function.
     * @throws ArgumentNullException Thrown when Operation was failed due to a
     * null argument.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public Object aggregate(ValueExtractor extractor, Aggregator aggregator) throws ArgumentNullException, OperationFailedException, Exception {
        return aggregate(extractor, aggregator, null, null, null, null);
    }

    /**
     * Executes the provided aggregate function on keys that match the given 
     * criteria.
     * 
     * @param extractor instance of value extractor.
     * @param aggregator instance of aggregator.
     * @param query Query to filter out the required keys.
     * @param parameters Input parameters for the query.
     * @return returns the result of the aggregate function.
     * @throws ArgumentNullException Thrown when Operation was failed due to a
     * null argument.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public Object aggregate(ValueExtractor extractor, Aggregator aggregator, String query, java.util.HashMap parameters) throws ArgumentNullException, OperationFailedException, Exception {
        return aggregate(extractor, aggregator, query, parameters, null, null);
    }

    /**
     * Executes the provided aggregate function on keys that match the given 
     * criteria.
     * 
     * @param extractor instance of value extractor.
     * @param aggregator instance of aggregator.
     * @param keyFilter instance of custom implementation for filtering of keys.
     * @return returns the result of the aggregate function.
     * @throws ArgumentNullException Thrown when Operation was failed due to a
     * null argument.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public Object aggregate(ValueExtractor extractor, Aggregator aggregator, KeyFilter keyFilter) throws ArgumentNullException, OperationFailedException, Exception {
        return aggregate(extractor, aggregator, null, null, keyFilter, null);
    }
    
    /**
     * Executes the provided aggregate function on keys that match the given 
     * criteria.
     * 
     * @param extractor instance of value extractor.
     * @param aggregator instance of aggregator.
     * @param query Query to filter out the required keys.
     * @param parameters Input parameters for the query.
     * @param timeOut Time after which task is considered as failed.
     * @return returns the result of the aggregate function.
     * @throws ArgumentNullException Thrown when Operation was failed due to a
     * null argument.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public Object aggregate(ValueExtractor extractor, Aggregator aggregator, String query, java.util.HashMap parameters, long timeOut) throws ArgumentNullException, OperationFailedException, Exception {
        return aggregate(extractor, aggregator, query, parameters, null, timeOut);
    }

    /**
     * Executes the provided aggregate function on keys that match the given 
     * criteria.
     * 
     * @param extractor instance of value extractor.
     * @param aggregator instance of aggregator.
     * @param keyFilter instance of custom implementation for filtering of keys.
     * @param timeOut Time after which task is considered as failed.
     * @return returns the result of the aggregate function.
     * @throws ArgumentNullException Thrown when Operation was failed due to a
     * null argument.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public Object aggregate(ValueExtractor extractor, Aggregator aggregator, KeyFilter keyFilter, long timeOut ) throws ArgumentNullException, OperationFailedException, Exception {
        return aggregate(extractor, aggregator, null, null, keyFilter, timeOut);
    }

    private Object aggregate(ValueExtractor extractor, Aggregator aggregator, String query, java.util.HashMap parameters, KeyFilter keyFilter, Long timeOut) throws ArgumentNullException, OperationFailedException, Exception {
        AggregatorTask aggregatorTask = new AggregatorTask(extractor, aggregator);
        Object value = null;
        boolean isResultfatch = false;
        TrackableTask reduceTaskResult = null;
        Enumeration enumeration = null;
        MapReduceTask mapReduceTask = aggregatorTask.createMapReduceTask();
        if (!Common.isNullorEmpty(query) && parameters != null) {
            reduceTaskResult = executeTask(mapReduceTask, query, parameters);

        } else if (keyFilter != null) {
            reduceTaskResult = executeTask(mapReduceTask, keyFilter);
        } else {
            reduceTaskResult = executeTask(mapReduceTask);
        }
        if (reduceTaskResult != null) {
            try {
                if (timeOut != null) {
                    enumeration = reduceTaskResult.getResult(timeOut);
                } else {
                    enumeration = reduceTaskResult.getResult();
                }
            } catch (OperationFailedException op) {

                if (op.getMessage().equals("getResult request timed out.")) {
                    try {
                        reduceTaskResult.cancelTask();
                    } catch (OperationFailedException ope) {
                    }
                    throw op;
                }

            }
            if (enumeration != null) {
                if (enumeration.hasMoreElements()) {
                    Map.Entry entry = (Map.Entry) enumeration.nextElement();
                    value = entry.getValue();
                    isResultfatch = true;
                }
            }
        }
        if (!isResultfatch) {
            value = PrimitiveDefaults.getDefault(aggregatorTask.getBuiltInAggregatorType());
        }
        return value;
    }
    // </editor-fold>    

    // <editor-fold desc=" ------- MapReduce Methods ----------">
    /**
     * Executes the provided task on keys that match the given criteria.
     * 
     * @param task Task to be executed on the cache.
     * @return Returns the TrackableTask object to monitor or get the result of 
     * the Task.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public TrackableTask executeTask(MapReduceTask task) throws OperationFailedException, Exception
    {
        if(task == null)
            throw new IllegalArgumentException("task cannot be null");
        
        if(task.getMapper() == null)
            throw new IllegalArgumentException("Mapper cannot be null");
        
        if(cacheId == null || _cacheImpl == null)
            throw new OperationFailedException("Cache is not initialized");
        
        UUID taskId = UUID.randomUUID();
                
        MapReduceTaskResult result = new MapReduceTaskResult(this._cacheImpl, taskId.toString());
        try
        {
            short callbackId = _eventManager.registerMapReduceListener(result, taskId.toString());
            if(_cacheImpl != null)
                _cacheImpl.executeMapReduceTask(task, taskId.toString(), MROutputOption.IN_MEMORY, callbackId);
            
        }
        catch(Exception ex)
        {
            if(isExceptionsEnabled())
                throw ex;
        }
        
        return result;
    }
    
    /**
     * Executes the provided task on keys that match the given criteria.
     * 
     * @param task Task to be executed on the cache.
     * @param query Query to filter out the required keys.
     * @param parameters Input parameters for the query.
     * @return Returns the TrackableTask object to monitor or get the result of 
     * the Task.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public TrackableTask executeTask(MapReduceTask task, String query, java.util.HashMap parameters) throws OperationFailedException, Exception
    {
        if(task!=null)
        {
            QueryFilter qf = new QueryFilter(query, parameters);            
            task.setFilter(new Filter(qf));
        }
        
        return executeTask(task);
    }
    
    /**
     * Executes the provided task on all the keys present in cache.
     * 
     * @param task Task to be executed on the cache.
     * @param keyFilter instance of custom implementation for filtering of keys.
     * @return Returns the TrackableTask object to monitor or get the result of 
     * the Task.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public TrackableTask executeTask(MapReduceTask task, KeyFilter keyFilter) throws OperationFailedException, Exception
    {
        if(keyFilter != null && task!=null)
            task.setFilter(new Filter(keyFilter));
        
        return executeTask(task);
    }
    
    /**
     * Gets the TrackableTask object against the given taskId. Any client having
     * the taskId can get the object and call getResult() on this object.
     * 
     * @param taskId The unique Id of a task.
     * @return Returns the TrackableTask object to get the result and status of 
     * the Task.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public TrackableTask getTaskResult(String taskId) throws OperationFailedException, Exception
    {
        MapReduceTaskResult result = new MapReduceTaskResult(this._cacheImpl, taskId);
        registerMapReduceCallback(result, taskId);
        return result;
    }
    
    private void registerMapReduceCallback(MapReduceListener listener, String taskId) throws OperationFailedException, Exception
    {
        if (_cacheImpl == null)
            throw new OperationFailedException("Cache is not initialized");
        
        // register on client side and get id
        short callbackId = _eventManager.registerMapReduceListener(listener, taskId);
        
        //send to server side
        try
        {
            if(_cacheImpl != null)
                _cacheImpl.registerMapReduceTaskCallback(callbackId, taskId);
            
        }
        catch(Exception ex)
        {
            if(isExceptionsEnabled())
                throw ex;
        }
    }
    
    /**
     * Returns the List of running task(s).
     * 
     * @return Returns List of tasksId.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws Exception  Thrown when an exception occurs while performing the
     * operation.
     */
    public List getRunningTasks() throws OperationFailedException, Exception
    {
        if(_cacheImpl == null || cacheId == null)
            throw new OperationFailedException("Cache is not initialized");
        
        try {
            if(_cacheImpl != null)
               return _cacheImpl.getRunningTasks();
        } catch(Exception ex)
        {
            if(isExceptionsEnabled())
                throw ex;
        }
        return null;
    }  
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Enumerator Methods & Classes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ">
    /**
     *
     * @return
     */
    // Types of Enumerations/Iterations
    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;

    @Override
    public boolean hasMoreElements()
    {
        WebCacheEnumerator enumerator = new WebCacheEnumerator(cacheId, null, null, this);
        return enumerator.hasMoreElements();
    }

    @Override
    public Object nextElement()
    {
        WebCacheEnumerator enumerator = new WebCacheEnumerator(cacheId, null, null, this);
        return enumerator.nextElement();
    }
    
    /**
     * Register the cache general notifications of specified type and filter 
     * This API is for internal use only
     * @return void
     * @param eventEnumSet specify the event type (add,update,remove).
     * @param dataFilter specify the data filter of event(none, metadata, DataWithMetaData).
     * @param registrationSequenceId. specify the sequence of registration.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ArgumentException Thrown when Operation was failed due to an invalid argument.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown whenever connection with cache server is lost while performing an operation on an outproc cache.
     */
    public void registerCacheNotificationDataFilterInternal(EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter, short registrationSequenceId) throws GeneralFailureException, GeneralFailureException, OperationFailedException, OperationFailedException, AggregateException,   ConfigurationException, ConnectionException {
        if(_cacheImpl != null)
        {
            _cacheImpl.registerGeneralNotification(eventEnumSet, dataFilter, registrationSequenceId);
        }
    }
    

    /**
     * Unregister the cache general notifications of specified type
     *
     * @param eventEnumSet specify the event type (add,update,remove). .
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConnectionException Thrown when an exception occurs when 
     * connection with server is lost.
     */
    public void unregisterGeneralCacheNotificationInternal(EnumSet<EventType> eventEnumSet) throws GeneralFailureException, GeneralFailureException,   OperationFailedException, ConnectionException, AggregateException {
        if(_cacheImpl != null)
        {
            _cacheImpl.unRegisterGeneralNotification(eventEnumSet, (short)-1);
        }
    }
    
    /**
     * Unregister a listener that was registered to receive cache event on data modification.
     *
     * @param discriptor holds the link to the registered delegate.
     * @throws GeneralFailureException Thrown when an exception occurs during a clustered operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws AggregateException This exception is thrown when multiple exceptions occur from multiple nodes. It combines all the exceptions as inner exceptions and throw it to
     * the client application.
     * @throws SecurityException Thrown if the user is not authorized to access cache.
     * @throws ConfigurationException Thrown when an exception occurs during configuration. Likely causes are badly specified configuration strings.
     * @throws ConnectionException Thrown whenever connection with cache server is lost while performing an operation on an outproc cache.
     */
    public void removeCacheDataModificationListener(CacheEventDescriptor discriptor) throws OperationFailedException, GeneralFailureException,   ConnectionException, ConnectionException, AggregateException, AggregateException, ConfigurationException {
        if (_cacheImpl == null) {
            if (_cacheImpl == null) {
                throw new OperationFailedException("Cache is not initialized");
            }
        }
        if (discriptor == null) {
            throw new IllegalArgumentException("CacheEventDiscriptor");
        }

        if (!discriptor.getIsRegistered()) {
            return;
        }

        _eventManager.unRegisterDiscriptor(discriptor);
    }
    
    /**
     * Unregister a listener that was registered to receive cache event when the specified key is modified.
     *
     * @param key The cache key used to reference the cache item
     * @param listener Registered listener
     * @param eventEnumSet specify the event type (add,update,remove).
     * @throws Exception Thrown when an exception occurs during an operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ArgumentNullException Thrown when passed argument is null.
     */
    public void removeCacheDataModificationListener(Object key, CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet) throws OperationFailedException, ArgumentNullException, ArgumentNullException, Exception {
        if (_cacheImpl == null) {
            if (_cacheImpl == null) {
                throw new OperationFailedException("Cache is not initialized");
            }
        }
        if (key == null) {
            throw new ArgumentNullException("key");
        }
        if (listener == null) {
            throw new ArgumentNullException("CacheDataNotificationCallback");
        }

        try {
            short[] value = this._eventManager.unRegisterSelectiveNotification(listener, eventEnumSet);

            short update = value[0];
            short remove = value[1];

            _cacheImpl.unRegisterKeyNotificationCallback(key, update, remove);
        } catch (Exception e) {
            if (exceptionsEnabled) {
                throw e;
            }
        }
    }

    /**
     *  Unregister a listener that was registered to receive cache event when the specified key is modified.
     *
     * @param key[] An array of cache keys used to reference the cache items
     * @param listener Registered listener
     * @param eventEnumSet specify the event type (add,update,remove).
     * @throws Exception Thrown when an exception occurs during an operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ArgumentNullException Thrown when passed argument is null.
     */
    public void removeBulkCacheDataModificationListener(Object[] key, CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet) throws OperationFailedException, ArgumentNullException, Exception {
        if (_cacheImpl == null) {
            if (_cacheImpl == null) {
                throw new OperationFailedException("Cache is not initialized");
            }
        }
        if (key == null) {
            throw new ArgumentNullException("key");
        }
        if (listener == null) {
            throw new ArgumentNullException("CacheDataNotificationCallback");
        }

        try {
            short[] value = this._eventManager.unRegisterSelectiveNotification(listener, eventEnumSet);

            short update = value[0];
            short remove = value[1];

            _cacheImpl.unRegisterKeyNotificationCallback(key, update, remove);
        } catch (Exception e) {
            if (exceptionsEnabled) {
                throw e;
            }
        }
    }
        
    protected CacheEventDescriptor addCacheDataModificationListener(Object key, CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter, boolean notifyOnItemExpiration) throws OperationFailedException, OperationFailedException, Exception 
    {
        if (_cacheImpl == null) throw new OperationFailedException("Cache is not initialized");
            CacheEventDescriptor discriptor = null;
        
            try
            {
                if (key != null)
                {
                    short[] callbackRefs = _eventManager.registerSelectiveEvent(listener, eventEnumSet, dataFilter);
                    _cacheImpl.registerKeyNotificationCallback(key, callbackRefs[0], callbackRefs[1], dataFilter, notifyOnItemExpiration);
                }
                else
                {
                    discriptor = _eventManager.registerGeneralEvents(listener, eventEnumSet, dataFilter);               
                }
            }
            catch (Exception e)
            {
                if (exceptionsEnabled) 
                    throw new Exception(e.getMessage(), e.getCause());
            }
            return discriptor;
    }
    
    /**
     * Register a listener to receive cache event when the specified key is modified.
     *
     * @return void
     * @param key The cache key used to reference the cache item
     * @param listener listener that acts when data associated with the specified key is modified
     * @param eventEnumSet specify the event type (add,update,remove).
     * @throws Exception Thrown when an exception occurs during an operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ArgumentNullException Thrown when passed argument is null.
     */
    public void addCacheDataModificationListener(Object key, CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet) throws ArgumentNullException, OperationFailedException, Exception {
    
         addCacheDataModificationListener(key, listener, eventEnumSet, EventDataFilter.None);

    }
    
    /**
     * Register a listener to receive cache event when the specified key is modified.
     *
     * @param key The cache key used to reference the cache item
     * @param listener listener that acts when data associated with the specified key is modified
     * @param eventEnumSet specify the event type (add,update,remove).
     * @param dataFilter dataFilter Filters the response according to the filter.
     * @throws Exception Thrown when an exception occurs during an operation.
     * @throws OperationFailedException Thrown whenever an API fails.
     * @throws ArgumentNullException Thrown when passed argument is null.
     */
    public void addCacheDataModificationListener(Object key, CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter) throws ArgumentNullException, OperationFailedException, Exception
    {
        if (key == null)
            throw new ArgumentNullException("key");
        
        if(listener == null)
            throw new ArgumentNullException("selectiveCacheDataNotificationCallback");
        
        if(eventEnumSet == null)
            throw new ArgumentNullException("eventEnumSet");
        addCacheDataModificationListener(key, listener, eventEnumSet, dataFilter, true);
    }            

    /**
     * @return the _defaultExpiration
     */
    protected ExpirationContract getDefaultExpiration() {
        return _defaultExpiration;
    }

    /**
     * @param _defaultExpiration the _defaultExpiration to set
     */
    protected void setDefaultExpiration(ExpirationContract _defaultExpiration) {
        this._defaultExpiration = _defaultExpiration;
    }
    
    
    /**
     * 
     * @param <K>
     * @param <V> 
     */
    public class Entry<K, V> implements Map.Entry<K, V>
    {
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
        protected Entry(K key, V value)
        {
            this.key = key;
            this.value = value;
        }

        /**
         *
         */
        protected Entry()
        {
        }

        /**
         *
         * @return
         */
        @Override
        protected Object clone()
        {
            return new Entry<K, V>(key, value);
        }

        /**
         *
         * @return
         */
        public K getKey()
        {
            return key;
        }

        /**
         *
         * @return
         */
        public V getValue()
        {
            return value;
        }

        /**
         *
         * @param value
         * @return
         */
        public V setValue(V value)
        {

            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        /**
         *
         * @param key
         */
        public void setKey(K key)
        {

            if (key == null)
            {
                throw new IllegalArgumentException();
            }

            this.key = key;
        }

        /**
         *
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof Map.Entry))
            {
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
        public String toString()
        {
            return key.toString() + "=" + (value != null ? value.toString() : "null");
        }
    }

    public Enumeration getEnumerator()
    {
        WebCacheEnumerator enumerator = new WebCacheEnumerator(cacheId, null, null, this);
        return enumerator;
    }

    ArrayList<EnumerationDataChunk> getNextChunk(ArrayList<EnumerationPointer> pointer)
    {
        ArrayList<EnumerationDataChunk> chunks = null;

        try
        {
            chunks = _cacheImpl.getNextChunk(pointer);
        }
        catch (Exception ex)
        {
            //this is a empty call just to dispose the enumeration pointers for this particular enumerator
            //on all the nodes.
            for (int i = 0; i < pointer.size(); i++)
            {
                pointer.get(i).setDisposable(true);
            }
            try
            {
                _cacheImpl.getNextChunk(pointer);
            }
            catch (Exception exc)
            {
            }

            if (exceptionsEnabled)
            {
                throw new ConcurrentModificationException(ex.getMessage());
            }
        }
        return chunks;
    }
    private HashMap getQueryInfo(Object value) throws GeneralFailureException
    {

        HashMap queryInfo = null;

        if (_cacheImpl.getTypeMap() == null)
        {
            return null;
        }
        else
        {
            this.typeMap = _cacheImpl.getTypeMap();
        }

        try
        {
            int handleId = typeMap.getHandleId(value.getClass().getCanonicalName());
            if (handleId != -1)
            {
                queryInfo = new HashMap();
                ArrayList attribValues = new ArrayList();
                ArrayList attributes = typeMap.getAttribList(handleId);

                for (int i = 0; i < attributes.size(); i++)
                {
                    Field fieldAttrib = value.getClass().getField((String) attributes.get(i));
                    if (fieldAttrib != null)
                    {
                        Object attribValue = fieldAttrib.get(value);


                        attribValues.add(attribValue);
                    }
                    else
                    {
                        throw new Exception("Unable to extract query information from user object.");
                    }

                }
                queryInfo.put(handleId, attribValues);
            }
        }
        catch (Exception e)
        {
            throw new GeneralFailureException("Unable to extract query information from user object.");
        }
        return queryInfo;
    }

    private HashMap getTagInfo(Object value, Tag[] tags, ArrayList<Tag> validTags) throws ArgumentNullException
    {
        if (tags == null)
        {
            return null;
        }

        HashMap tagInfo = new HashMap();
        ArrayList tagsList = new ArrayList();
        for (Tag tag : tags)
        {
            if (tag == null || tag.getTagName() == null)
            {
                throw new ArgumentNullException("Value cannot be null.\r\nParameter name: Tag");
            }
            else if (!tagsList.contains(tag.getTagName()))
            {
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

    private HashMap getNamedTagsInfo(Object value, NamedTagsDictionary namedTags) throws NullPointerException, ArgumentException
    {
        if (value == null || namedTags == null || namedTags.getCount() == 0)
        {
            return null;
        }

        CheckDuplicateIndexName(value, namedTags);

        HashMap tagInfo = new HashMap();
        HashMap tagsList = new HashMap();

        Iterator iterator = namedTags.getKeysIterator();

        while (iterator.hasNext())
        {
            String key = iterator.next().toString();
            Object val = namedTags.getValue(key);

            if (val == null)
            {
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

    private void CheckDuplicateIndexName(Object value, NamedTagsDictionary namedTags) throws ArgumentException
    {
        if (typeMap == null)
        {
            return;
        }

        int handleId = typeMap.getHandleId(value.getClass().getCanonicalName());
        if (handleId != -1)
        {
            ArrayList attributes = this.typeMap.getAttribList(handleId);
            for (int i = 0; i < attributes.size(); i++)
            {
                if (namedTags.contains(attributes.get(i).toString())) //@UH whether this should be case insensitive
                {
                    throw new ArgumentException("Key in named tags conflicts with the indexed attribute name of the specified object.");
                }
            }
        }
    }

    private void InitializeLogging(String fileName, Level logLevel) throws GeneralFailureException
    {
        if (logLevel == Level.OFF)
        {
            Logger logger = Logger.getLogger("com.alachisoft");
            logger.setLevel(logLevel);
            return;
        }

        try
        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            // Create a file handler that uses 3 logfiles, each with a limit of 1Mbyte
            String pattern = fileName + "." + sdf.format(new java.util.Date()) + ".log-%g.htm";
            String path = null;
            String separator = System.getProperty("file.separator");
            int limit = 1000000; // 1 Mb
            int numLogFiles = 3;
            if (System.getProperty("os.name").toLowerCase().startsWith("win"))
            {
                path = System.getenv("NCHome");
                if (path != null)
                {
                    if (!path.endsWith(separator))
                    {
                        path = path.concat(separator);
                    }
                }
                //Should not be used, not a good of getting final path
                // USE the following instead:

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
            }
            else
            {
                path = System.getenv("NCACHE_ROOT");
                if (path != null && path.equalsIgnoreCase("") == false)
                {
                    path = path.concat(separator + pattern);
                }
                else
                {
                    path = System.getenv("NCACHE_MEMCACHED_ROOT");
                    if (path != null && path.equalsIgnoreCase("") == false)
                    {
                        path = path.concat(separator + pattern);
                    }

                }
                if (path == null)
                {
                    path = "/usr/local/ncache/";
                }
                if (!path.endsWith(separator))
                {
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
        }
        catch (IOException e)
        {
            throw new GeneralFailureException(e.getMessage());
        }
        catch (Exception e)
        {
        }
    }

}
