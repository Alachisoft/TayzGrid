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

package com.alachisoft.tayzgrid.web.caching.apilogging;

import com.alachisoft.tayzgrid.event.CacheNotificationType;
import com.alachisoft.tayzgrid.event.CacheStatusNotificationType;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.caching.NamedTagsDictionary;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.web.caching.CacheItem;
import com.alachisoft.tayzgrid.web.caching.CacheItemVersion;

import com.alachisoft.tayzgrid.web.caching.DSReadOption;
import com.alachisoft.tayzgrid.web.caching.DSWriteOption;
import java.util.Date;
import java.util.EnumSet;

public class APILogItem {

    private String _signature = null;
    private Object _key = null;
    /**
     * Absolute expiration for the object.
     */
    private java.util.Date _abs = null;
    /**
     * Sliding expiration for the object.
     */
    private TimeSpan _sld = null;
    private CacheItemPriority _p = null;
    private String _group = null;
    private String _subGroup = null;
    private Tag[] _tags = null;
    private NamedTagsDictionary _namedTags = null;
    private int _noOfKeys = -1;
    private String _providerName = null;
    private String _resyncProviderName = null;
    private Boolean _isResyncRequired = null;
    private DSWriteOption _dsWriteOption = null;
    private DSReadOption _dsReadOption = null;
    private String _query = null;
    private java.util.Map _queryValues = null;
    private CacheItemVersion _version = null;
    private TimeSpan _lockTimeout = null;
    private Boolean _acquireLock = null;
    private Boolean _releaseLock = null;
    private String _exceptionMessage = null;
    private RuntimeAPILogItem _rtAPILogItem = null;

    private EnumSet<CacheNotificationType> _cacheNotificationTypes = null;
    private EnumSet<CacheStatusNotificationType> _cacheStatusNotificationTypes = null;
    
    private Date _loggingTime;
    
    public final Date getLoggingTime()
    {
        return _loggingTime;
    }
    
    public final void setLoggingTime(Date value)
    {
        _loggingTime=value;
    }
    
    public final RuntimeAPILogItem getRuntimeAPILogItem() {
        return _rtAPILogItem;
    }

    public final void setRuntimeAPILogItem(RuntimeAPILogItem value) {
        _rtAPILogItem = value;
    }

    public APILogItem() {
    }

    public APILogItem(Object key, String exceptionMessage) {
        _key = key;
        _exceptionMessage = exceptionMessage;
    }

    public APILogItem(Object key, CacheItem item, String exceptionMessage) {
        _key = key;
         
        _group = item.getGroup();
        _subGroup = item.getSubGroup();
        _isResyncRequired = item.getResyncExpiredItems();
        _tags = item.getTags();
        _namedTags = item.getNamedTags();      
        _abs = item.getAbsoluteExpiration();
        _sld = item.getSlidingExpiration();
        _p = item.getPriority();
        
       
        _resyncProviderName = item.getResyncProviderName();
        _version = item.getVersion();

        _exceptionMessage = exceptionMessage;
    }

    /**
     * Get or set the signature of API call
     */
    public final String getSignature() {
        return _signature;
    }

    public final void setSignature(String value) {
        _signature = value;
    }

    /**
     * Get or set the key
     */
    public final Object getKey() {
        return _key;
    }

    public final void setKey(Object value) {
        _key = value;
    }

    /**
     * Get or set the number of keys
     */
    public final int getNoOfKeys() {
        return _noOfKeys;
    }

    public final void setNoOfKeys(int value) {
        _noOfKeys = value;
    }

    /**
     * Get or set the name of group
     */
    public final String getGroup() {
        return _group;
    }

    public final void setGroup(String value) {
        _group = value;
    }

    /**
     * Get or set the name of subgroup
     */
    public final String getSubGroup() {
        return _subGroup;
    }

    public final void setSubGroup(String value) {
        _subGroup = value;
    }

    /**
     * Get or set the absolute expiraion date and time
     */
    public final java.util.Date getAbsolueExpiration() {
        return _abs;
    }

    public final void setAbsolueExpiration(java.util.Date value) {
        _abs = value;
    }

    /**
     * Get or set the sliding expiration timespan
     */
    public final TimeSpan getSlidingExpiration() {
        return _sld;
    }

    public final void setSlidingExpiration(TimeSpan value) {
        _sld = value;
    }

    /**
     * Get or set the tags
     */
    public final Tag[] getTags() {
        return _tags;
    }

    public final void setTags(Tag[] value) {
        _tags = value;
    }

    /**
     * Get or set the named tags
     */
    public final NamedTagsDictionary getNamedTags() {
        return _namedTags;
    }

    public final void setNamedTags(NamedTagsDictionary value) {
        _namedTags = value;
    }

    /**
     * Get or set the priority
     */
    public final CacheItemPriority getPriority() {
        return _p;
    }

    public final void setPriority(CacheItemPriority value) {
        _p = value;
    }

    public final String getProviderName() {
        return _providerName;
    }

    public final void setProviderName(String value) {
        _providerName = value;
    }

    public final String getResyncProviderName() {
        return _resyncProviderName;
    }

    public final void setResyncProviderName(String value) {
        _resyncProviderName = value;
    }

    public final DSWriteOption getDSWriteOption() {
        return _dsWriteOption;
    }

    public final void setDSWriteOption(DSWriteOption value) {
        _dsWriteOption = value;
    }

    public final DSReadOption getDSReadOption() {
        return _dsReadOption;
    }

    public final void setDSReadOption(DSReadOption value) {
        _dsReadOption = value;
    }

    public final String getQuery() {
        return _query;
    }

    public final void setQuery(String value) {
        _query = value;
    }

    public final java.util.Map getQueryValues() {
        return _queryValues;
    }

    public final void setQueryValues(java.util.Map value) {
        _queryValues = value;
    }

    public final CacheItemVersion getCacheItemVersion() {
        return _version;
    }

    public final void setCacheItemVersion(CacheItemVersion value) {
        _version = value;
    }

    public final TimeSpan getLockTimeout() {
        return _lockTimeout;
    }

    public final void setLockTimeout(TimeSpan value) {
        _lockTimeout = value;
    }

    public final Boolean getAcquireLock() {
        return _acquireLock;
    }

    public final void setAcquireLock(Boolean value) {
        _acquireLock = value;
    }

    public final Boolean getReleaseLock() {
        return _releaseLock;
    }

    public final void setReleaseLock(Boolean value) {
        _releaseLock = value;
    }

    public final Boolean getIsResyncRequired() {
        return _isResyncRequired;
    }

    public final void setIsResyncRequired(Boolean value) {
        _isResyncRequired = value;
    }

    public final String getExceptionMessage() {
        return _exceptionMessage;
    }

    public final void setExceptionMessage(String value) {
        _exceptionMessage = value;
    }
    
    public final EnumSet<CacheNotificationType> getCacheNotificationTypes()
    {
        return _cacheNotificationTypes;
    }
    
    public final void setCacheNotificationTypes(EnumSet<CacheNotificationType> value)
    {
        _cacheNotificationTypes=value;
    }
    
    public final EnumSet<CacheStatusNotificationType> getCacheStatusNotificationTypes()
    {
        return _cacheStatusNotificationTypes;
    }
    
    public final void setCacheStatusNotificationTypes(EnumSet<CacheStatusNotificationType> value)
    {
        _cacheStatusNotificationTypes=value;
    }
}
