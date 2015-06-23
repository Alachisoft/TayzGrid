/*
 * ===============================================================================
 * Alachisoft (R) TayzGrid Integrations
 * TayzGrid Provider for Hibernate
 * ===============================================================================
 * Copyright © Alachisoft.  All rights reserved.
 * THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY
 * OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE.
 * ===============================================================================
 */
package com.alachisoft.tayzgrid.integrations.hibernate.cache;

import com.alachisoft.tayzgrid.integrations.hibernate.cache.configuration.ConfigurationManager;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.configuration.RegionConfiguraton;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.web.caching.CacheItem;
import com.alachisoft.tayzgrid.web.caching.DSWriteOption;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.Timestamper;

public class TayzGrid implements Cache {

    private static final Logger _log = LogManager.getLogger("TayzGrid");
    private static HashMap<String, com.alachisoft.tayzgrid.web.caching.Cache> _caches = new HashMap<String, com.alachisoft.tayzgrid.web.caching.Cache>();
    private static String _tagPrefix = "TGHibernate.";
    private com.alachisoft.tayzgrid.web.caching.Cache _cache;
    private RegionConfiguraton _regionConfig = null;
    private String _regionName;
    private String _connectionString;

    public TayzGrid(String regionName, Properties properties) {
        try {
            if (_log.isDebugEnabled()) {
                _log.debug("Initializing TayzGrid with region : " + regionName);
            }
            if (properties.containsKey("tayzgrid.application_id")) {
                System.setProperty("tayzgrid.application_id", properties.getProperty("tayzgrid.application_id"));
            }

            _regionName = regionName;
            _regionConfig = ConfigurationManager.getInstance().getRegionConfiguration(regionName);

            if (_caches.containsKey(_regionConfig.getCacheName())) {
                _cache = _caches.get(_regionConfig.getCacheName());
            } else {
                _cache = com.alachisoft.tayzgrid.web.caching.TayzGrid.initializeCache(_regionConfig.getCacheName());
                _caches.put(_regionConfig.getCacheName(), _cache);
                _cache.setExceptionsEnabled(ConfigurationManager.getInstance().isExceptionEnabled());
            }

            if (properties.containsKey("hibernate.connection.url")) {
                _connectionString = properties.getProperty("hibernate.connection.url").toString();
            }

        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Failed to initialize TayzGrid. " + e.getMessage());
            }
            throw new CacheException("Failed to initialize TayzGrid. " + e.getMessage(), e);
        }

    }

    @Override
    public Object read(Object key) throws CacheException {
        return get(key);
    }

    @Override
    public Object get(Object key) throws CacheException {
        try {
            if (key == null) {
                return null;
            }

            String cacheKey = ConfigurationManager.getInstance().getCacheKey(key);
            if (_log.isDebugEnabled()) {
                _log.debug("Fetching object from cache with key = " + cacheKey);
            }
            return _cache.get(cacheKey);
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Get operation failed. " + e.getMessage());
            }
            throw new CacheException("Get operation failed. " + e.getMessage(), e);
        }
    }

    @Override
    public void put(Object key, Object value) throws CacheException {
        try {
            if (key == null) {
                throw new ArgumentException("null key not allowed.");
            }
            if (value == null) {
                throw new ArgumentException("null value not allowed.");
            }

            String cacheKey = ConfigurationManager.getInstance().getCacheKey(key);
            CacheItem cItem = new CacheItem(value);
            cItem.setPriority(_regionConfig.getCacheItemPriority());
            cItem.setTags(new Tag[]{new Tag(_tagPrefix + _regionName)});

            if (_regionConfig.getExpirationType().equalsIgnoreCase("sliding")) {
                cItem.setSlidingExpiration(new TimeSpan(0, 0, _regionConfig.getExpirationPeriod()));
            } else if (_regionConfig.getExpirationType().equalsIgnoreCase("absolute")) {
                cItem.setAbsoluteExpiration(Calendar.getInstance().getTime());
            }

            if (_log.isDebugEnabled()) {
                _log.debug(String.format("Inserting: key={0}&value={1}", key, value.toString()));
            }
            _cache.insert(cacheKey, cItem);
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Put operation failed. " + e.getMessage());
            }
            throw new CacheException("Put operation failed. " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Object key, Object value) throws CacheException {
        put(key, value);
    }

    @Override
    public void remove(Object key) throws CacheException {
        try {
            String cacheKey = ConfigurationManager.getInstance().getCacheKey(key);
            if (_log.isDebugEnabled()) {
                _log.debug("Removing item with key: " + cacheKey);
            }
            _cache.remove(cacheKey);
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Remove operation failed. " + e.getMessage());
            }
            throw new CacheException("Remove operation failed. " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() throws CacheException {
        try {
            if (_log.isDebugEnabled()) {
                _log.debug("Clearing Cache : " + _cache + "   with Region: " + _regionName);
            }
            _cache.removeByTag(new Tag(_tagPrefix + _regionName));
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Clear operation failed. " + e.getMessage());
            }
            throw new CacheException("Clear operation failed. " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() throws CacheException {
        try {
            if (_cache != null) {
                if (_log.isDebugEnabled()) {
                    _log.debug("Destroying Cache : " + _cache);
                }
                _caches.remove(_cache.toString());
                _cache.dispose();
            }
        } catch (Exception e) {
            if (_log.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                _log.error("Destroy operation failed. " + e.getMessage());
            }
            throw new CacheException("Destroy operation failed. " + e.getMessage(), e);
        }
    }

    @Override
    public void lock(Object o) throws CacheException {
    }

    @Override
    public void unlock(Object o) throws CacheException {
    }

    @Override
    public long nextTimestamp() {
        return Timestamper.next();
    }

    @Override
    public int getTimeout() {
        return Timestamper.ONE_MS * 60000;
    }

    @Override
    public String getRegionName() {
        return _regionName;
    }

    @Override
    public long getSizeInMemory() {
        return -1;
    }

    @Override
    public long getElementCountInMemory() {
        return -1;
    }

    @Override
    public long getElementCountOnDisk() {
        return 0;
    }

    @Override
    public Map toMap() {
        return null;
    }
}
