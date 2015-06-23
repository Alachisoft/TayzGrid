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

import com.alachisoft.tayzgrid.caching.LockAccessType;
import com.alachisoft.tayzgrid.common.BitSet;

import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;
import com.alachisoft.tayzgrid.common.datastructures.EnumerationPointer;

import com.alachisoft.tayzgrid.event.CacheListener;
import com.alachisoft.tayzgrid.event.CacheNotificationType;
import com.alachisoft.tayzgrid.event.CacheStatusNotificationType;
import com.alachisoft.tayzgrid.event.CacheStatusEventListener;
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
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.web.caching.apilogging.APILogItem;
import com.alachisoft.tayzgrid.web.caching.apilogging.APILogger;
import com.alachisoft.tayzgrid.web.caching.apilogging.DebugAPIConfigurations;
import com.alachisoft.tayzgrid.web.caching.apilogging.RuntimeAPILogItem;
import com.alachisoft.tayzgrid.web.events.CacheEventDescriptor;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import tangible.RefObject;

public class WrapperCache<K, V> extends Cache<K, V>
{
    private Cache _webCache = null;
    private APILogger _apiLogger;
    private DebugAPIConfigurations _debugAPIConfigurations;

    public WrapperCache(Cache cache) {
        _webCache = cache;
        _debugAPIConfigurations=new DebugAPIConfigurations();
        _apiLogger = new APILogger(cache.toString(), _debugAPIConfigurations);
    }

    @Override
    public boolean isPerfStatsCollectorInitialized() {
        return _webCache.isPerfStatsCollectorInitialized();
    }

    @Override
    String getSerializationContext() {
        return _webCache.getSerializationContext();
    }

    @Override
    void setSerializationContext(String value) {
        _webCache.setSerializationContext(value);
    }

   

    @Override
    CacheImplBase getCacheImpl() {
        return _webCache.getCacheImpl();
    }

    @Override
    void setCacheImpl(CacheImplBase value) {
        _webCache.setCacheImpl(value);
    }



    @Override
    void addSecondaryInprocInstance(Cache secondaryInstance) {
        _webCache.addSecondaryInprocInstance(secondaryInstance);
    }

    @Override
    void addRef() {
        _webCache.addRef();
    }

    @Override
    public boolean isExceptionsEnabled() {
        return _webCache.isExceptionsEnabled();
    }

    @Override
    public void setExceptionsEnabled(boolean exceptionsEnabled) {
        _webCache.setExceptionsEnabled(exceptionsEnabled);
    }

    @Override
    protected Object addOperation(Object key, Object value, Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, DSWriteOption dsWriteOption, CacheDataModificationListener cacheItemRemovedListener, CacheDataModificationListener cacheItemUpdatedListener, AsyncItemAddedCallback asyncItemAddedCallback, DataSourceItemsAddedCallback onDataSourceItemAdded, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, Tag[] tags, String providerName, String resyncProviderName, NamedTagsDictionary namedTags, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, RefObject<Long> size) throws GeneralFailureException, OperationFailedException, AggregateException, SecurityException, ConfigurationException, ArgumentException, ConnectionException, Exception {


        return _webCache.addOperation(key, value, absoluteExpiration, slidingExpiration, priority, dsWriteOption, cacheItemRemovedListener, cacheItemUpdatedListener, asyncItemAddedCallback, onDataSourceItemAdded, isResyncExpiredItems, group, subGroup, isAsync, tags, providerName, resyncProviderName, namedTags, itemUpdateDataFilter ,itemRemovedDataFilter, size);
    }

    @Override
    public HashMap addBulk(Object[] keys, CacheItem[] items, DSWriteOption dsWriteOption, String providerName, DataSourceItemsAddedCallback onDataSourceItemAddedCallback) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        HashMap result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.addBulk(keys, items, dsWriteOption, providerName, onDataSourceItemAddedCallback);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("addBulk(String[] keys, CacheItem[] items, DSWriteOption dsWriteOption, String providerName, DataSourceItemsAddedCallback onDataSourceItemAddedCallback)");
                    
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setProviderName(providerName);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    protected CacheItemVersion insertOperation(Object key, Object value,  Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority, DSWriteOption dsWriteOption, AsyncItemUpdatedCallback asyncItemUpdatedCallback, CacheDataModificationListener cacheItemRemovedListener, CacheDataModificationListener cacheItemUdpatedListener, DataSourceItemsUpdatedCallback onDataSourceItemUpdated, boolean isResyncExpiredItems, String group, String subGroup, boolean isAsync, CacheItemVersion version, LockHandle lockHandle, LockAccessType accessType, Tag[] tags, String providerName, String resyncProviderName, NamedTagsDictionary namedTags, EventDataFilter itemUpdateDataFilter, EventDataFilter itemRemovedDataFilter, RefObject<Long> size) throws GeneralFailureException, OperationFailedException, AggregateException, SecurityException, ConfigurationException, ArgumentException, ConnectionException, Exception {
        return _webCache.insertOperation(key, value,  absoluteExpiration, slidingExpiration, priority, dsWriteOption, asyncItemUpdatedCallback, cacheItemRemovedListener, cacheItemUdpatedListener, onDataSourceItemUpdated, isResyncExpiredItems, group, subGroup, isAsync, version, lockHandle, accessType, tags, providerName, resyncProviderName, namedTags, itemUpdateDataFilter, itemRemovedDataFilter ,size);

    }

    @Override
    public void insertAsync(Object key, Object value, AsyncItemUpdatedCallback asyncItemUpdatedCallback, String group, String subGroup) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.insertAsync(key, value, asyncItemUpdatedCallback, group, subGroup);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setGroup(group);
                    logItem.setSubGroup(subGroup);
                    logItem.setSignature("insertAsync(Object key, Object value, AsyncItemUpdatedCallback asyncItemUpdatedCallback, String group, String subGroup)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
    }

    @Override
    public void insertAsync(Object key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.insertAsync(key, item, dsWriteOption, onDataSourceItemUpdated);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setSignature("insertAsync(Object key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
    }

    @Override
    public void insertAsync(Object key, CacheItem item, String providerName, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.insertAsync(key, item, providerName, dsWriteOption, onDataSourceItemUpdated);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setSignature("insertAsync(Object key, CacheItem item, String providerName, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
    }

    @Override
    public HashMap insertBulk(Object[] keys, CacheItem[] items, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        HashMap result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.insertBulk(keys, items, dsWriteOption, onDataSourceItemUpdated);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("insertBulk(String[] keys, CacheItem[] items, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public HashMap insertBulk(Object[] keys, CacheItem[] items, DSWriteOption dsWriteOption, String providerName, DataSourceItemsUpdatedCallback onDataSourceItemUpdated) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        HashMap result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.insertBulk(keys, items, dsWriteOption, providerName, onDataSourceItemUpdated);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("insertBulk(String[] keys, CacheItem[] items, DSWriteOption dsWriteOption, String providerName, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setProviderName(providerName);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Object remove(Object key, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.remove(key, version);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setCacheItemVersion(version);
                    logItem.setSignature("remove(Object key, CacheItemVersion version)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public void delete(Object key, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.delete(key, version);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setCacheItemVersion(version);
                    logItem.setSignature("delete(Object key, CacheItemVersion version)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public Object remove(Object key, LockHandle lockHandle) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.remove(key, lockHandle);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("remove(Object key, LockHandle lockHandle)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public void delete(Object key, LockHandle lockHandle) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.delete(key, lockHandle);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("delete(Object key, LockHandle lockHandle)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public Object remove(Object key, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.remove(key, dsWriteOption, providerName, onDataSourceItemRemoved);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setProviderName(providerName);
                    logItem.setSignature("remove(Object key, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public void delete(Object key, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.delete(key, dsWriteOption, providerName, onDataSourceItemRemoved);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setProviderName(providerName);
                    logItem.setSignature("delete(Object key, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void delete(Object key, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.delete(key, dsWriteOption, onDataSourceItemRemoved);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setSignature("delete(Object key, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public Object remove(Object key, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.remove(key, dsWriteOption, onDataSourceItemRemoved);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setSignature("remove(Object key, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public void removeAsync(Object key, AsyncItemRemovedCallback asyncItemRemovedCallback, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.removeAsync(key, asyncItemRemovedCallback, dsWriteOption, onDataSourceItemRemoved);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setSignature("removeAsync(Object key, AsyncItemRemovedCallback asyncItemRemovedCallback, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
    }

    @Override
    public void removeAsync(Object key, AsyncItemRemovedCallback asyncItemRemovedCallback, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.removeAsync(key, asyncItemRemovedCallback, dsWriteOption, providerName, onDataSourceItemRemoved);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setProviderName(providerName);
                    logItem.setSignature("removeAsync(Object key, AsyncItemRemovedCallback asyncItemRemovedCallback, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
    }

    @Override
    public HashMap removeBulk(Object[] keys, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        HashMap result;
        String exceptionMessage = null;
        try {
            result = _webCache.removeBulk(keys, dsWriteOption, providerName, onDataSourceItemRemoved);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setNoOfKeys(keys.length);
                    logItem.setProviderName(providerName);
                    logItem.setSignature("removeBulk(String[] keys, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public void deleteBulk(Object[] keys, DSWriteOption dsWriteOption, String providerName, DataSourceItemsRemovedCallback onDataSourceItemRemoved) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.deleteBulk(keys, dsWriteOption, providerName, onDataSourceItemRemoved);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setNoOfKeys(keys.length);
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setProviderName(providerName);
                    logItem.setSignature("delete(Object key, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void removeGroupData(String group, String subGroup) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.removeGroupData(group, subGroup);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setGroup(group);
                    logItem.setSubGroup(subGroup);
                    logItem.setSignature("removeGroupData(String group, String subGroup)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
    }



    @Override
    public void clear() throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception
        {
            String exceptionMessage = null;
        try {
            _webCache.clear();
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("clear()");
                    logItem.setExceptionMessage(exceptionMessage);
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
        }



    @Override
    public void clearAsync(AsyncCacheClearedCallback onAsyncCacheCleared) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.clearAsync(onAsyncCacheCleared);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("clearAsync(AsyncCacheClearedCallback onAsyncCacheCleared)");
                    logItem.setDSWriteOption(DSWriteOption.None);
                    logItem.setExceptionMessage(exceptionMessage);
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }
	
    @Override
    public Collection search(String query, HashMap values) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Collection result;
        String exceptionMessage = null;
        try {
            result = _webCache.search(query, values);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setQuery(query);
                    logItem.setSignature("search(String query, HashMap values)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public int executeNonQuery(String query, HashMap values)
            throws OperationFailedException, Exception
    {
        int result;
        String exceptionMessage = null;

        try
        {
            result = _webCache.executeNonQuery(query, values);
        }
        catch (Exception ex)
        {
            exceptionMessage = ex.getMessage();
            throw ex;
        }
        finally
        {
            try
            {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setQuery(query);
                    logItem.setSignature("executeNonQuery(String query, HashMap values)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            }
            catch (Exception ex)
            {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public HashMap searchEntries(String query, HashMap values) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        HashMap result;
        String exceptionMessage = null;
        try {
            result = _webCache.searchEntries(query, values);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setQuery(query);
                    logItem.setSignature("searchEntries(String query, HashMap values)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public ICacheReader executeReader(String query, HashMap values) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        ICacheReader result;
        String exceptionMessage = null;
        try {
            result = _webCache.executeReader(query, values);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setQuery(query);
                    logItem.setSignature("executeReader(String query, HashMap values)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public boolean contains(Object key) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        boolean result;
        String exceptionMessage = null;
        try {
            result = _webCache.contains(key);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("contains(Object key)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
        return result;
    }

    @Override

    public Object get(Object key, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {

        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.get(key, dsReadOption);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("get(Object key, DSReadOption dsReadOption)");
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Object get(Object key, DSReadOption dsReadOption, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.get(key, dsReadOption, version);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("get(Object key, DSReadOption dsReadOption, CacheItemVersion version)");
                    logItem.setCacheItemVersion(version);
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Object get(Object key, String providerName, DSReadOption dsReadOption, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.get(key, providerName, dsReadOption, version);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("get(Object key, String providerName, DSReadOption dsReadOption, CacheItemVersion version)");
                    logItem.setProviderName(providerName);
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setCacheItemVersion(version);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    Object get(Object key, String group, String subGroup, DSReadOption dsReadOption, CacheItemVersion version, TimeSpan lockTimeout, LockHandle lockHandle, LockAccessType accessType, String providerName) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        return _webCache.get(key, group, subGroup, dsReadOption, version, lockTimeout, lockHandle, accessType, providerName);
    }



  
    @Override
    public HashMap getBulk(Object[] keys, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        HashMap result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getBulk(keys, dsReadOption);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("getBulk(String[] keys, DSReadOption dsReadOption)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public HashMap getBulk(Object[] keys, String provideName, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        HashMap result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getBulk(keys, provideName, dsReadOption);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("getBulk(String[] keys, String providerName, DSReadOption dsReadOption)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setProviderName(provideName);
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Collection getGroupKeys(String group, String subGroup) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Collection result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getGroupKeys(group, subGroup);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("getGroupKeys(String group, String subGroup)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setGroup(group);
                    logItem.setSubGroup(subGroup);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public HashMap getGroupData(String group, String subGroup) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        HashMap result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getGroupData(group, subGroup);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("getGroupData(String group, String subGroup)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setGroup(group);
                    logItem.setSubGroup(subGroup);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public CacheItem getCacheItem(Object key) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        CacheItem result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getCacheItem(key);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("getCacheItem(Object key)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public CacheItem getCacheItem(Object key, TimeSpan lockTimeout, LockHandle lockHandle, boolean acquireLock) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        CacheItem result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getCacheItem(key, lockTimeout, lockHandle, acquireLock);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("getCacheItem(Object key, TimeSpan lockTimeout, LockHandle lockHandle, boolean acquireLock)");
                    logItem.setLockTimeout(lockTimeout);
                    logItem.setAcquireLock(acquireLock);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public CacheItem getCacheItem(Object key, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        CacheItem result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getCacheItem(key, dsReadOption);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("getCacheItem(Object key, DSReadOption dsReadOption)");
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public CacheItem getCacheItem(Object key, String providerName, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        CacheItem result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getCacheItem(key, providerName, dsReadOption);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("getCacheItem(Object key, String providerName, DSReadOption dsReadOption)");
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setProviderName(providerName);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public CacheItem getCacheItem(Object key, DSReadOption dsReadOption, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        CacheItem result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getCacheItem(key, dsReadOption, version);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("getCacheItem(Object key, DSReadOption dsReadOption, CacheItemVersion version)");
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setCacheItemVersion(version);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public CacheItem getCacheItem(Object key, String providerName, DSReadOption dsReadOption, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        CacheItem result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getCacheItem(key, providerName, dsReadOption, version);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("getCacheItem(Object key, String providerName, DSReadOption dsReadOption, CacheItemVersion version)");
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setProviderName(providerName);
                    logItem.setCacheItemVersion(version);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    CacheItem getCacheItemInternal(Object key, String group, String subGroup, DSReadOption dsReadOption, CacheItemVersion version, LockAccessType accessType, TimeSpan lockTimeout, LockHandle lockHandle, String providerName, boolean acquireLock) throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception {
        return _webCache.getCacheItemInternal(key, group, subGroup, dsReadOption, version, accessType, lockTimeout, lockHandle, providerName, acquireLock);
    }

    @Override
    public HashMap getByAllTags(Tag[] tags) throws GeneralFailureException, OperationFailedException, AggregateException,  Exception {
        HashMap result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getByAllTags(tags);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("getByAllTags(Tag[] tags)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setTags(tags);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Collection getKeysByAllTags(Tag[] tags) throws GeneralFailureException, OperationFailedException, AggregateException,  Exception {
        Collection result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getKeysByAllTags(tags);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("getKeysByAllTags(Tag[] tags)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setTags(tags);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public HashMap getByAnyTag(Tag[] tags) throws GeneralFailureException, OperationFailedException, AggregateException,  Exception {
        HashMap result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getByAnyTag(tags);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("getByAnyTag(Tag[] tags)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setTags(tags);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Collection getKeysByAnyTag(Tag[] tags) throws GeneralFailureException, OperationFailedException, AggregateException,  Exception {
        Collection result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getKeysByAnyTag(tags);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("getKeysByAnyTag(Tag[] tags)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setTags(tags);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public HashMap getByTag(Tag tag) throws GeneralFailureException, OperationFailedException, AggregateException,  Exception {
        HashMap result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getByTag(tag);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("getByTag(Tag tag)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setTags(new Tag[]{tag});
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Collection getKeysByTag(Tag tag) throws GeneralFailureException, OperationFailedException, AggregateException,  Exception {
        Collection result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getKeysByTag(tag);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("getKeysByTag(Tag tag)");
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setTags(new Tag[]{tag});
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public void removeByTag(Tag tag) throws GeneralFailureException, OperationFailedException, ArgumentNullException, AggregateException,  ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.removeByTag(tag);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setTags(new Tag[]{tag});
                    logItem.setSignature("removeByTag(Tag tag)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
    }

    @Override
    public void removeByAnyTag(Tag[] tags) throws GeneralFailureException, OperationFailedException, ArgumentNullException, AggregateException,  ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.removeByAnyTag(tags);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setTags(tags);
                    logItem.setSignature("removeByAnyTag(Tag[] tags)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
    }

    @Override
    public void removeByAllTags(Tag[] tags) throws GeneralFailureException, OperationFailedException, ArgumentNullException, AggregateException,  ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.removeByAllTags(tags);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setTags(tags);
                    logItem.setSignature("removeByAllTags(Tag[] tags)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
    }

    @Override
    Object getSerializedObject(Object key, DSReadOption dsReadOption, RefObject<Long> v, RefObject<BitSet> flag) throws Exception {
        return _webCache.getSerializedObject(key, dsReadOption, v, flag);
    }

    @Override
    public Object getIfNewer(Object key, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getIfNewer(key, version);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("getGroupKeys(String group, String subGroup)");
                    logItem.setCacheItemVersion(version);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Object getIfNewer(Object key, String group, String subGroup, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getIfNewer(key, group, subGroup, version);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("getGroupKeys(String group, String subGroup)");
                    logItem.setCacheItemVersion(version);
                    logItem.setGroup(group);
                    logItem.setSubGroup(subGroup);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public boolean lock(Object key, TimeSpan lockTimeout, LockHandle lockHandle) throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception {
        boolean result;
        String exceptionMessage = null;
        try {
            result = _webCache.lock(key, lockTimeout, lockHandle);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setLockTimeout(lockTimeout);
                    logItem.setSignature("lock(Object key, TimeSpan lockTimeout, LockHandle lockHandle)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
        return result;
    }

    @Override
    boolean isLocked(Object key, LockHandle lockHandle) throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception {
        return _webCache.isLocked(key, lockHandle);
    }

    @Override
    public void unlock(Object key) throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.unlock(key);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("unlock(Object key)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void unlock(Object key, LockHandle lockHandle) throws OperationFailedException,  GeneralFailureException, AggregateException, ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.unlock(key, lockHandle);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("unlock(Object key, LockHandle lockHandle)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }

        }
    }


    @Override
    public boolean setAttributes(Object key, CacheItemAttributes attributes) throws OperationFailedException, GeneralFailureException,  AggregateException, ConnectionException, Exception {
        boolean result;
        String exceptionMessage = null;
        try {
            result = _webCache.setAttributes(key, attributes);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("setAttributes(Object key, CacheItemAttributes attributes)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
        return result;
    }

    @Override
    public void registerKeyNotificationCallback(Object key, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.registerKeyNotificationCallback(key, updateCallback, removeCallback);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("registerKeyNotificationCallback(String[] key, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void registerKeyNotificationCallback(Object[] keys, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.registerKeyNotificationCallback(keys, updateCallback, removeCallback);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setNoOfKeys(keys.length);
                    logItem.setSignature("registerKeyNotificationCallback(String[] keys, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
     void unRegisterKeyNotificationCallback(Object key, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.unRegisterKeyNotificationCallback(key, updateCallback, removeCallback);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("unRegisterKeyNotificationCallback(Object key, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
     void unRegisterKeyNotificationCallback(Object[] keys, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.unRegisterKeyNotificationCallback(keys, updateCallback, removeCallback);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setNoOfKeys(keys.length);
                    logItem.setSignature("unRegisterKeyNotificationCallback(String[] keys, CacheItemUpdatedCallback updateCallback, CacheItemRemovedCallback removeCallback)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void raiseCustomEvent(Object key, Object value) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.raiseCustomEvent(key, value);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setSignature("raiseCustomEvent(Object key, Object value)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public long getCount() throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        long result;
        String exceptionMessage = null;
        try {
            result = _webCache.getCount();
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setSignature("getCount()");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
        return result;
    }

    @Override
    public void dispose() throws GeneralFailureException, OperationFailedException, ConfigurationException {
        String exceptionMessage = null;
        try {
            _webCache.dispose();
        } catch (GeneralFailureException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (OperationFailedException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ConfigurationException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setSignature("dispose()");
                    _apiLogger.Log(logItem);
                }
                _apiLogger.Dispose();
            } catch (Exception ex) {
            }
        }
    }

    @Override
    Object getDeserializedObject(Object value,String serializationContext, BitSet flag) throws GeneralFailureException {
        return _webCache.getDeserializedObject(value,serializationContext, flag);
    }

    @Override
    public void registerCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> registerAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentNullException, ConnectionException {
        String exceptionMessage = null;
        try {
            _webCache.registerCacheEventlistener(listener, registerAgainst);
        } catch (GeneralFailureException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (OperationFailedException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (AggregateException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ConfigurationException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ArgumentNullException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ConnectionException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setCacheNotificationTypes(registerAgainst);
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setSignature("registerCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> registerAgainst)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void unregisterCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, ArgumentException {
        String exceptionMessage = null;
        try {
            _webCache.unregisterCacheEventlistener(listener, unregisterAgainst);
        } catch (GeneralFailureException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (OperationFailedException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (AggregateException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ConfigurationException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ArgumentNullException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ConnectionException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setCacheNotificationTypes(unregisterAgainst);
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setSignature("unregisterCacheEventlistener(CacheListener listener, EnumSet<CacheNotificationType> unregisterAgainst)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }


    @Override
    public CacheEventDescriptor addCacheDataModificationListener(CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter) throws ArgumentNullException, OperationFailedException, Exception
    {
        CacheEventDescriptor result = null;
        String exceptionMessage = null;
        try
        {
                result = _webCache.addCacheDataModificationListener(listener, eventEnumSet, dataFilter);
        }
        catch (RuntimeException e)
        {
                exceptionMessage = e.getMessage();
                throw e;
        }
        finally
        {
                try
                {
                     
                    if (_debugAPIConfigurations.isInLoggingInterval())
                        {
                                APILogItem logItem = new APILogItem();
                                logItem.setSignature("addCacheDataModificationListener(string key, CacheDataModificationListener listener, EnumSet<EventType> eventType, Runtime.Events.EventDataFilter datafilter");
                                logItem.setExceptionMessage(exceptionMessage);
                                _apiLogger.Log(logItem);
                        }
                }
                catch (RuntimeException e)
                {
                }
        }
        return result;
    }    
    
    @Override
    public void addCacheDataModificationListener(Object key, CacheDataModificationListener listener, EnumSet<EventType> eventType, EventDataFilter datafilter) throws ArgumentNullException, ArgumentNullException, OperationFailedException, Exception

    {
     
        String exceptionMessage = null;
        try
        {
                _webCache.addCacheDataModificationListener(key, listener, eventType, datafilter);
        }
        catch (RuntimeException e)
        {
                exceptionMessage = e.getMessage();
                throw e;
        }
        finally
        {
                try
                {
                        if (_debugAPIConfigurations.isInLoggingInterval())
                        {
                                APILogItem logItem = new APILogItem();
                                logItem.setSignature("addCacheDataModificationListener(string key, CacheDataModificationListener listener, EnumSet<EventType> eventType, Runtime.Events.EventDataFilter datafilter");
                                logItem.setExceptionMessage(exceptionMessage);
                                logItem.setKey(key);
                                _apiLogger.Log(logItem);
                        }
                }
                catch (RuntimeException e)
                {
                }
        }                    
    }

    @Override
    public void addCacheDataModificationListener(Object key, CacheDataModificationListener listener, EnumSet<EventType> eventType) throws ArgumentNullException, ArgumentNullException, OperationFailedException, Exception

    {
     
        String exceptionMessage = null;
        try
        {
                _webCache.addCacheDataModificationListener(key, listener, eventType);
        }
        catch (RuntimeException e)
        {
                exceptionMessage = e.getMessage();
                throw e;
        }
        finally
        {
                try
                {
                        if (_debugAPIConfigurations.isInLoggingInterval())
                        {
                                APILogItem logItem = new APILogItem();
                                logItem.setSignature("addCacheDataModificationListener(string key, CacheDataModificationListener listener, EnumSet<EventType> eventType");
                                logItem.setExceptionMessage(exceptionMessage);
                                logItem.setKey(key);
                                _apiLogger.Log(logItem);
                        }
                }
                catch (RuntimeException e)
                {
                }
        }                    
    }
    @Override
    public void addBulkCacheDataModificationListener(Object[] keys, CacheDataModificationListener listener, EnumSet<EventType> eventType) throws IOException, ArgumentNullException, ArgumentNullException, ArgumentException, ArgumentException, OperationFailedException, OperationFailedException, Exception

    {    
            String exceptionMessage = null;
            try
            {
                    _webCache.addBulkCacheDataModificationListener(keys, listener, eventType);
            }
            catch (RuntimeException e)
            {
                    exceptionMessage = e.getMessage();
                    throw e;
            }
            finally
            {
                    try
                    {
                            if (_debugAPIConfigurations.isInLoggingInterval())
                            {
                                    APILogItem logItem = new APILogItem();
                                    logItem.setSignature("addCacheDataModificationListener(string[] keys, CacheDataModificationListener listener, EnumSet<EventType> eventType");
                                    logItem.setExceptionMessage(exceptionMessage);
                                    logItem.setNoOfKeys(keys.length);
                                    _apiLogger.Log(logItem);
                            }
                    }
                    catch (RuntimeException e)
                    {
                    }
            }
    }

    @Override
    public void addBulkCacheDataModificationListener(Object[] keys, CacheDataModificationListener listener, EnumSet<EventType> eventType, EventDataFilter datafilter) throws IOException, ArgumentNullException, ArgumentNullException, ArgumentException, ArgumentException, OperationFailedException, OperationFailedException, Exception

    {    
            String exceptionMessage = null;
            try
            {
                    _webCache.addBulkCacheDataModificationListener(keys, listener, eventType, datafilter);
            }
            catch (RuntimeException e)
            {
                    exceptionMessage = e.getMessage();
                    throw e;
            }
            finally
            {
                    try
                    {
                            if (_debugAPIConfigurations.isInLoggingInterval())
                            {
                                    APILogItem logItem = new APILogItem();
                                    logItem.setSignature("addCacheDataModificationListener(string[] keys, CacheDataModificationListener listener, EnumSet<EventType> eventType, Runtime.Events.EventDataFilter datafilter");
                                    logItem.setExceptionMessage(exceptionMessage);
                                    logItem.setNoOfKeys(keys.length);
                                    _apiLogger.Log(logItem);
                            }
                    }
                    catch (RuntimeException e)
                    {
                    }
            }
    }
    @Override
    protected CacheEventDescriptor addCacheDataModificationListener(Object key, CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter, boolean notifyOnItemExpiration) throws OperationFailedException, OperationFailedException, Exception 
    {
        return _webCache.addCacheDataModificationListener(key, listener, eventEnumSet, dataFilter, notifyOnItemExpiration);
    }
    
    @Override 
    protected void addCacheDataModificationListener(Object[] keys, CacheDataModificationListener listener, EnumSet<EventType> enumTypeSet, EventDataFilter dataFilter, boolean notifyOnItemExpiration) throws OperationFailedException, Exception
    {
        _webCache.addCacheDataModificationListener(keys, listener, enumTypeSet, dataFilter, notifyOnItemExpiration);
    }    
    
    @Override
    public void removeCacheDataModificationListener(CacheEventDescriptor discriptor) throws OperationFailedException, GeneralFailureException,   ConnectionException, ConnectionException, AggregateException, AggregateException, ConfigurationException {
        String exceptionMessage = null;
        try
        {
                _webCache.removeCacheDataModificationListener(discriptor);
        }
        catch (RuntimeException e)
        {
                exceptionMessage = e.getMessage();
                throw e;
        }
        finally
        {
                try
                {
                        if (_debugAPIConfigurations.isInLoggingInterval())
                        {
                                APILogItem logItem = new APILogItem();
                                logItem.setSignature("removeCacheDataModificationListener(CacheEventDescriptor discriptor)");
                                logItem.setExceptionMessage(exceptionMessage);
                                _apiLogger.Log(logItem);                                
                        }
                }
                catch (RuntimeException e)
                {
                } 
                catch (IOException ex) 
                {                    
                }
        }
    }
    
    @Override
    public void removeBulkCacheDataModificationListener(Object[] key, CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet) throws OperationFailedException, ArgumentNullException, Exception 
    {
        String exceptionMessage = null;

        try
        {
                _webCache.removeBulkCacheDataModificationListener(key, listener, eventEnumSet);
        }
        catch (RuntimeException e)
        {
                exceptionMessage = e.getMessage();
                throw e;
        }
        finally
        {
                try
                {
                        if (_debugAPIConfigurations.isInLoggingInterval())
                        {
                                APILogItem logItem = new APILogItem();
                                logItem.setSignature("removeCacheDataModificationListener(string[] key, CacheDataModificationListener listener, Runtime.Events.EventType eventType)");
                                logItem.setNoOfKeys(key.length);
                                logItem.setExceptionMessage(exceptionMessage);
                                _apiLogger.Log(logItem);
                        }
                }
                catch (RuntimeException e)
                {
                }
        }
    }
    
    @Override
    public void removeCacheDataModificationListener(Object key, CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet) throws OperationFailedException, ArgumentNullException, ArgumentNullException, Exception 
    {
        String exceptionMessage = null;

        try
        {
                _webCache.removeCacheDataModificationListener(key, listener, eventEnumSet);
        }
        catch (RuntimeException e)
        {
                exceptionMessage = e.getMessage();
                throw e;
        }
        finally
        {
                try
                {
                        if (_debugAPIConfigurations.isInLoggingInterval())
                        {
                                APILogItem logItem = new APILogItem();
                                logItem.setSignature("removeCacheDataModificationListener(string key, CacheDataModificationListener listener, Runtime.Events.EventType eventType)");
                                logItem.setKey(key);
                                logItem.setExceptionMessage(exceptionMessage);
                                _apiLogger.Log(logItem);
                        }
                }
                catch (RuntimeException e)
                {
                }
        }
    }
    
    
 
    
    @Override
    public void addCacheStatusEventlistener(CacheStatusEventListener listener, EnumSet<CacheStatusNotificationType> registerAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, ArgumentNullException {
        String exceptionMessage = null;
        try {
            _webCache.addCacheStatusEventlistener(listener, registerAgainst);
        } catch (GeneralFailureException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (OperationFailedException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (AggregateException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ConfigurationException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ArgumentNullException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ConnectionException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setCacheStatusNotificationTypes(registerAgainst);
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setSignature("registerCacheStatusEventlistener(ClusterListener listener, EnumSet<CacheStatusNotificationType> registerAgainst)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void removeCacheStatusEventlistener(CacheStatusEventListener listener, EnumSet<CacheStatusNotificationType> unregisterAgainst) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, ArgumentNullException {
        String exceptionMessage = null;
        try {
            _webCache.removeCacheStatusEventlistener(listener, unregisterAgainst);
        } catch (GeneralFailureException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (OperationFailedException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (AggregateException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ConfigurationException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ArgumentNullException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } catch (ConnectionException ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setCacheStatusNotificationTypes(unregisterAgainst);
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setSignature("unregisterCacheStatusEventlistener(ClusterListener listener, EnumSet<CacheStatusNotificationType> unregisterAgainst)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void addCustomEventListener(CustomListener listener) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.addCustomEventListener(listener);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setSignature("registerCustomEventListener(CustomListener listener)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void removeCustomEventListener(CustomListener listener) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.removeCustomEventListener(listener);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setSignature("unregisterCustomEventListener(CustomListener listener)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public boolean hasMoreElements() {
        return _webCache.hasMoreElements();
    }

    @Override
    public Object nextElement() {
        return _webCache.nextElement();
    }

    @Override
    public Enumeration getEnumerator() {
        return _webCache.getEnumerator();
    }

    @Override
    ArrayList<EnumerationDataChunk> getNextChunk(ArrayList<EnumerationPointer> pointer) {
        return _webCache.getNextChunk(pointer);
    }

    @Override
    public CacheItemVersion add(Object key, Object value) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.add(key, value);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("add(Object key, Object value)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion add(Object key, CacheItem item) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.add(key, item);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setSignature("add(Object key, CacheItem item)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion add(Object key, Object value, Tag[] tags) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.add(key, value, tags);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("add(Object key, Object value, Tag[] tags)");
                    logItem.setTags(tags);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion add(Object key, Object value, NamedTagsDictionary namedTags) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.add(key, value, namedTags);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("add(Object key, Object value, NamedTagsDictionary namedTags)");
                    logItem.setNamedTags(namedTags);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion add(Object key, Object value, String group, String subGroup) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.add(key, value, group, subGroup);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("add(Object key, Object value, String group, String subGroup)");
                    logItem.setGroup(group);
                    logItem.setSubGroup(subGroup);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override

    public CacheItemVersion add(Object key, Object value,  Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority) throws GeneralFailureException, OperationFailedException, AggregateException, SecurityException, ConfigurationException, ArgumentException, ConnectionException, Exception {

        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.add(key, value,  absoluteExpiration, slidingExpiration, priority);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("add(Object key, Object value, Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority)");
                    logItem.setAbsolueExpiration(absoluteExpiration);
                    logItem.setSlidingExpiration(slidingExpiration);
                    logItem.setPriority(priority);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion add(Object key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsAddedCallback onDataSourceItemAdded) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.add(key, item, dsWriteOption, onDataSourceItemAdded);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setSignature("add(string key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsAddedCallback onDataSourceItemAdded)");
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion add(Object key, CacheItem item, DSWriteOption dsWriteOption, String providerName, DataSourceItemsAddedCallback onDataSourceItemAdded) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.add(key, item, dsWriteOption, providerName, onDataSourceItemAdded);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setSignature("add(Object key, CacheItem item, DSWriteOption dsWriteOption, String providerName, DataSourceItemsAddedCallback onDataSourceItemAdded)");
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setProviderName(providerName);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public HashMap addBulk(Object[] keys, CacheItem[] items, DSWriteOption dsWriteOption, DataSourceItemsAddedCallback onDataSourceItemAddedCallback) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        HashMap result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.addBulk(keys, items, dsWriteOption, onDataSourceItemAddedCallback);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setSignature("addBulk(String[] keys, CacheItem[] items, DSWriteOption dsWriteOption, DataSourceItemsAddedCallback onDataSourceItemAddedCallback)");
                    //logItem.setNoOfKeys(keys.length);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }

        return result;
    }

    @Override
    public Object addAsync(Object key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsAddedCallback onSourceItemAdded) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        Object obj = null;
        String exceptionMessage = null;
        try {
            obj = _webCache.addAsync(key, item, dsWriteOption, onSourceItemAdded);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setSignature("addAsync(Object key, Object value, AsyncItemAddedCallback onAsyncItemAddCallback, String group, String subGroup)");
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return obj;
    }

    @Override
    public Object addAsync(Object key, CacheItem item, DSWriteOption dsWriteOption, String providerName, DataSourceItemsAddedCallback onSourceItemAdded) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        Object obj = null;
        String exceptionMessage = null;
        try {
            obj = _webCache.addAsync(key, item, dsWriteOption, providerName, onSourceItemAdded);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setSignature("addAsync(Object key, CacheItem item, DSWriteOption dsWriteOption, String providerName, DataSourceItemsAddedCallback onSourceItemAdded)");
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setProviderName(providerName);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return obj;
    }

    @Override
    public Object addAsync(Object key, Object value, AsyncItemAddedCallback onAsyncItemAddCallback, String group, String subGroup) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        Object obj = null;
        String exceptionMessage = null;
        try {
            obj = _webCache.addAsync(key, value, onAsyncItemAddCallback, group, subGroup);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("addAsync(Object key, Object value, AsyncItemAddedCallback onAsyncItemAddCallback, String group, String subGroup)");
                    logItem.setGroup(group);
                    logItem.setSubGroup(subGroup);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return obj;
    }

    @Override
    public CacheItemVersion insert(Object key, Object value) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.insert(key, value);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("insert(Object key, Object value)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion insert(Object key, CacheItem item) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.insert(key, item);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setSignature("insert(Object key, CacheItem item)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion insert(Object key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.insert(key, item, dsWriteOption, onDataSourceItemUpdated);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setSignature("insert(Object key, CacheItem item, DSWriteOption dsWriteOption, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion insert(Object key, CacheItem item, DSWriteOption dsWriteOption, String providerName, DataSourceItemsUpdatedCallback onDataSourceItemUpdated) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.insert(key, item, dsWriteOption, providerName, onDataSourceItemUpdated);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setProviderName(providerName);
                    logItem.setSignature("insert(Object key, CacheItem item, DSWriteOption dsWriteOption, String providerName, DataSourceItemsUpdatedCallback onDataSourceItemUpdated)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion insert(Object key, CacheItem item, LockHandle lockHandle, boolean releaseLock) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.insert(key, item, lockHandle, releaseLock);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, item, exceptionMessage);
                    logItem.setReleaseLock(releaseLock);
                    logItem.setSignature("insert(Object key, CacheItem item, LockHandle lockHandle, boolean releaseLock)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion insert(Object key, Object value, String group, String subGroup) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.insert(key, value, group, subGroup);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setGroup(group);
                    logItem.setSubGroup(subGroup);
                    logItem.setSignature("insert(Object key, Object value, String group, String subGroup)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion insert(Object key, Object value, Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority) throws GeneralFailureException, OperationFailedException, AggregateException, SecurityException, ConfigurationException, ArgumentException, ConnectionException, Exception {

        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.insert(key, value, absoluteExpiration, slidingExpiration, priority);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setAbsolueExpiration(absoluteExpiration);
                    logItem.setSlidingExpiration(slidingExpiration);
                    logItem.setPriority(priority);
                    logItem.setSignature("insert(Object key, Object value, Date absoluteExpiration, TimeSpan slidingExpiration, CacheItemPriority priority)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion insert(Object key, Object value, Tag[] tags) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.insert(key, value, tags);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setTags(tags);
                    logItem.setSignature("insert(Object key, Object value, Tag[] tags)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public CacheItemVersion insert(Object key, Object value, NamedTagsDictionary namedTags) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ArgumentException, ConnectionException, Exception {
        CacheItemVersion version;
        String exceptionMessage = null;
        try {
            version = _webCache.insert(key, value, namedTags);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setNamedTags(namedTags);
                    logItem.setSignature("insert(Object key, Object value, NamedTagsDictionary namedTags)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return version;
    }

    @Override
    public HashMap removeBulk(Object[] keys, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        HashMap result;
        String exceptionMessage = null;
        try {
            result = _webCache.removeBulk(keys, dsWriteOption, onDataSourceItemRemoved);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setNoOfKeys(keys.length);
                    logItem.setSignature("removeBulk(String[] keys, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public void deleteBulk(Object[] keys, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        String exceptionMessage = null;
        try {
            _webCache.deleteBulk(keys, dsWriteOption, onDataSourceItemRemoved);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem();
                    logItem.setDSWriteOption(dsWriteOption);
                    logItem.setNoOfKeys(keys.length);
                    logItem.setExceptionMessage(exceptionMessage);
                    logItem.setSignature("deleteBulk(String[] keys, DSWriteOption dsWriteOption, DataSourceItemsRemovedCallback onDataSourceItemRemoved)");
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public Object get(Object key) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {

        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.get(key);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("get(Object key)");
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Object get(Object key, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.get(key, version);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("get(Object key, CacheItemVersion version)");
                    logItem.setCacheItemVersion(version);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Object get(Object key, TimeSpan lockTimeout, LockHandle lockHandle, boolean acquireLock) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.get(key, lockTimeout, lockHandle, acquireLock);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("get(Object key, TimeSpan lockTimeout, LockHandle lockHandle, boolean acquireLock)");
                    logItem.setLockTimeout(lockTimeout);
                    logItem.setAcquireLock(acquireLock);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Object get(Object key, String group, String subGroup, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.get(key, group, subGroup, dsReadOption);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("get(Object key, String group, String subGroup, DSReadOption dsReadOption)");
                    logItem.setGroup(group);
                    logItem.setSubGroup(subGroup);
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public Object get(Object key, String providerName, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        Object result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.get(key, providerName, dsReadOption);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("get(Object key, String providerName, DSReadOption dsReadOption)");
                    logItem.setProviderName(providerName);
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public CacheItem getCacheItem(Object key, CacheItemVersion version) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        CacheItem result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getCacheItem(key, version);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("getCacheItem(Object key, CacheItemVersion version)");
                    logItem.setCacheItemVersion(version);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }

    @Override
    public CacheItem getCacheItem(Object key, String group, String subGroup, DSReadOption dsReadOption) throws GeneralFailureException, OperationFailedException, AggregateException,  ConfigurationException, ConnectionException, Exception {
        CacheItem result = null;
        String exceptionMessage = null;
        try {
            result = _webCache.getCacheItem(key, group, subGroup, dsReadOption);
        } catch (Exception ex) {
            exceptionMessage = ex.getMessage();
            throw ex;
        } finally {
            try {
                if (_debugAPIConfigurations.isInLoggingInterval()) {
                    APILogItem logItem = new APILogItem(key, exceptionMessage);
                    logItem.setSignature("getCacheItem(Object key, String group, String subGroup, DSReadOption dsReadOption)");
                    logItem.setDSReadOption(dsReadOption);
                    logItem.setGroup(group);
                    logItem.setSubGroup(subGroup);
                    logItem.setRuntimeAPILogItem((RuntimeAPILogItem) _webCache.getRuntimeAPILogHashMap().get(Thread.currentThread().getId()));
                    _apiLogger.Log(logItem);
                }
            } catch (Exception ex) {
            }
            _webCache.getRuntimeAPILogHashMap().remove(Thread.currentThread().getId());
        }
        return result;
    }


    @Override
    public void registerCacheNotificationDataFilterInternal(EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter, short registrationSequenceId) throws GeneralFailureException, GeneralFailureException, OperationFailedException, OperationFailedException, AggregateException,   ConfigurationException, ConnectionException {
        _webCache.registerCacheNotificationDataFilterInternal(eventEnumSet, dataFilter, registrationSequenceId);
    }

    @Override
    public String toString()
    {
      return _webCache.toString();
    }
}
