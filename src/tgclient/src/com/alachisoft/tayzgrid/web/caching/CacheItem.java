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

//~--- JDK imports ------------------------------------------------------------
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.caching.NamedTagsDictionary;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.runtime.events.EventType;
import java.util.*;
//~--- classes ----------------------------------------------------------------

/**
 * Class that represents a cached item including its dependencies, expiration
 * and eviction information
 */
public class CacheItem {

    /**
     * ItemRemoveCallback function pointer supplied by client application to be
     * fired on item removal.
     */
    // private CacheItemRemovedCallback _rc;
    /**
     * ItemRemoveCallback function pointer supplied by client application to be
     * fired on item update.
     */
    // private CacheItemUpdatedCallback _uc;
    /**
     * A values that identifies that "Resync Expired Items" is to be provided
     * for the object.\n if true "Resync Expired Items" is enabled otherwise
     * not.
     */
    private boolean _r = false;
    /**
     * Absolute expiration for the object.
     */
    private Date _abs;
 

    /**
     * Priority for the object.
     */
    private CacheItemPriority _p;
    /**
     * Sliding expiration for the object.
     */
    private TimeSpan _sld;
    /**
     * The actual object provided by the client application
     */
    private Object _v;
    private com.alachisoft.tayzgrid.common.BitSet _flagMap;
    private HashMap _queryInfo;
    private AsyncItemAddedCallback asyncItemAddedCallback = null;
    private AsyncItemUpdatedCallback asyncItemUpdatedCallback = null;
    private String _group;
    private String _subGroup;
    private String _resyncProviderName = null;
    private CacheItemVersion _version;
    private Tag[] _tags;
    private NamedTagsDictionary _namedTags;

    private CacheDataModificationListener _cacheItemUpdatedCallback;
    private CacheDataModificationListener _cacheItemRemovedListener;
    private EventDataFilter _itemRemovedDataFilter = EventDataFilter.None;
    private EventDataFilter _itemUpdatedDataFilter = EventDataFilter.None;

    Date _creationTime = new Date();
    Date _lastModifiedTime = new Date();

    //~--- constructors -------------------------------------------------------
//    /** Creates a new instance of CacheItem */
    @Deprecated
    CacheItem() {
        _abs = Cache.DefaultAbsoluteExpiration;
        _sld = Cache.DefaultSlidingExpiration;
        _p = CacheItemPriority.Default;
    }

    /**
     * Constructor
     *
     * @param value Actual object to be stored in cache
     */
    public CacheItem(Object value) {
        _v = value;
        _abs = Cache.DefaultAbsoluteExpiration;
        _sld = Cache.DefaultSlidingExpiration;
        _p = CacheItemPriority.Default;
    }

    //~--- get methods --------------------------------------------------------
    /**
     * The time at which the added object expires and is removed from the cache.
     *
     * @return The time at which the added object expires and is removed from
     * the cache.
     */
    public Date getAbsoluteExpiration() {
        return _abs;
    }




    /**
     * The relative cost of the object, as expressed by the enumeration. The
     * cache uses this value when it evicts objects; objects with a lower cost
     * are removed from the cache before objects with a higher cost.
     *
     * @return CacheItemPriority object of this item.
     * @see CacheItemPriority
     */
    public CacheItemPriority getPriority() {
        return _p;
    }

    /**
     * Gets a value indicating whether the object when expired will cause a
     * re-fetch of the object from the master datasource. (Resync Expired Items)
     *
     * @return true/false
     */
    public boolean getResyncExpiredItems() {
        return _r;
    }

    /**
     * The interval between the time the added object was last accessed and when
     * that object expires. If this value is the equivalent of 20 minutes, the
     * object expires and is removed from the cache 20 minutes after it is last
     * accessed.
     *
     * @return Date object specifying the sliding period for this item.
     */
    public TimeSpan getSlidingExpiration() {
        return _sld;
    }

    /**
     * The payload for this item.
     *
     * @return The value of the item.
     */
    public Object getValue() {
        return _v;
    }

    /**
     * Internal use only
     *
     * @return
     * @deprecated
     */
    public com.alachisoft.tayzgrid.common.BitSet getFlag() {
        return _flagMap;
    }

    /**
     * Group name, this caheItem is to be included in
     *
     * @return Group Name
     */
    public String getGroup() {
        return _group;
    }

    /**
     * SubGroup name, this caheItem is to be included in
     *
     * @return SubGroup name
     */
    public String getSubGroup() {
        return _subGroup;
    }

    /**
     * Item version of the object, used when getCachItem is used on Cache
     *
     * @return
     * @see Cache
     */
    public CacheItemVersion getVersion() {
        return this._version;
    }

    /**
     * Internal use only
     *
     * @deprecated
     * @return
     */
    public HashMap getQueryInfo() {
        return _queryInfo;
    }

    /**
     * gets all tags this CacheItem is attached to
     *
     * @return Array of tags
     */
    public Tag[] getTags() {
        return _tags;
    }

    /**
     * If dependency or ItemUpdateCallbacks are used in this cacheItem then it
     * is a shallow copy, else this clone method observes a deep clone
     *
     * @return CacheItem as an Object
     */
    public Object Clone() {
        CacheItem newItem = new CacheItem(this._v);

        newItem._abs = this._abs;
        newItem._sld = this._sld;
        newItem._p = this._p;
        newItem._r = this._r;
        newItem._version = this._version;
        newItem._group = this._group;
        newItem._subGroup = this._subGroup;
        newItem._flagMap = this._flagMap;
        newItem.asyncItemAddedCallback = this.asyncItemAddedCallback;
        newItem.asyncItemUpdatedCallback = this.asyncItemUpdatedCallback;
        newItem._tags = this._tags != null ? this._tags : null;
        newItem._namedTags = this._namedTags != null ? this._namedTags : null;
        newItem._queryInfo = this._queryInfo != null ? ((HashMap) this._queryInfo.clone()) : null;

        return newItem;
    }

    //~--- set methods --------------------------------------------------------
    /**
     * Sets the time at which the added object expires and is removed from the
     * cache.
     *
     * @param absoluteExpiration Sets the time at which the added object expires
     * and is removed from the cache.
     */
    public void setAbsoluteExpiration(Date absoluteExpiration) {
        _abs = absoluteExpiration;
    }

    /**
     * Internal use only
     *
     * @deprecated
     * @param queryInfo
     */
    public void setQueryInfo(HashMap queryInfo) {
        this._queryInfo = queryInfo;
    }

    /**
     * sets all tags this CacheItem is to be attached to
     *
     * @param tags Array of tag names, cannot contain null
     */
    public void setTags(Tag[] tags) {
        this._tags = tags;
    }



  

    /**
     *
     * @param priority The relative cost of the object, as expressed by the
     * enumeration. The cache uses this value when it evicts objects; objects
     * with a lower cost are removed from the cache before objects with a higher
     * cost.
     *
     * @see CacheItemPriority
     */
    public void setPriority(CacheItemPriority priority) {
        _p = priority;
    }

    /**
     * Set priority using integer value
     *
     * @see CacheItemPriority
     * @param p
     */
    public void setPriority(int p) {
        switch (p) {
            case 1:
                _p = CacheItemPriority.Low;
                break;
            case 2:
                _p = CacheItemPriority.BelowNormal;
            case 0:
                _p = CacheItemPriority.Normal;
                break;
            case 3:
                _p = CacheItemPriority.AboveNormal;
                break;
            case 4:
                _p = CacheItemPriority.High;
                break;
            case 5:
                _p = CacheItemPriority.NotRemovable;
                break;
            case 6:
                _p = CacheItemPriority.Default;
                break;
            default:
                _p = CacheItemPriority.Default;
                break;

        }
    }

    /**
     *
     * @param resync Sets a value indicating whether the object when expired
     * will cause a re-fetch of the object from the master datasource. (Resync
     * Expired Items).
     */
    public void setResyncExpiredItems(boolean resync) {
        _r = resync;
    }

    /**
     *
     * @param slidingExpiration The interval between the time the added object
     * was last accessed and when that object expires. If this value is the
     * equivalent of 20 minutes, the object expires and is removed from the
     * cache 20 minutes after it is last accessed.
     */
    public void setSlidingExpiration(TimeSpan slidingExpiration) {
        _sld = slidingExpiration;
    }

    /**
     *
     * @param value Sets the value of the CacheItem.
     */
    public void setValue(Object value) {
        _v = value;
    }

    /**
     * Internal use only
     *
     * @param Flag
     * @deprecated
     */
    public void setFlag(com.alachisoft.tayzgrid.common.BitSet Flag) {
        _flagMap = Flag;
    }

    /**
     * An instance that, if provided, is called when an Asynchronous request for
     * Add Operation completes. You can use this to obtain the results of an
     * 'AddAsync' operation.
     *
     * @return instance of the callback
     * @see AsyncItemAddedCallback
     */
    public AsyncItemAddedCallback getAsyncItemAddedCallback() {
        return asyncItemAddedCallback;
    }

    /**
     * An instance that, if provided, is called when an Asynchronous request for
     * Add Operation completes. You can use this to obtain the results of an
     * 'AddAsync' operation.
     *
     * @param itemAddedCallback
     * @see AsyncItemAddedCallback
     */
    public void setAsyncItemAddedCallback(AsyncItemAddedCallback itemAddedCallback) {
        this.asyncItemAddedCallback = itemAddedCallback;
    }

    /**
     * An instance that, if provided, is called when an Asynchronous request for
     * Update Operation completes. You can use this to obtain the results of an
     * 'InsertAsync' operation.
     *
     * @return instance of the callback
     * @see AsyncItemUpdatedCallback
     */
    public AsyncItemUpdatedCallback getAsyncItemUpdatedCallback() {
        return asyncItemUpdatedCallback;
    }

    /**
     * An instance that, if provided, is called when an Asynchronous request for
     * Update Operation completes. You can use this to obtain the results of an
     * 'InsertAsync' operation.
     *
     * @param itemUpdatedCallback
     * @see AsyncItemUpdatedCallback
     */
    public void setAsyncItemUpdatedCallback(AsyncItemUpdatedCallback itemUpdatedCallback) {
        this.asyncItemUpdatedCallback = itemUpdatedCallback;
    }

    /**
     * The name of the group to associate with the cache item. All cache items
     * with the same group name are logically grouped together.
     *
     * @param group string, cannot be null of subgroup is existent
     */
    public void setGroup(String group) {
        this._group = group;
    }

    /**
     * The name of the sub-group within a group. This hierarchical grouping
     * gives more control over the cache items.
     *
     * @param subGroup string, cannot be null if group is existent
     */
    public void setSubGroup(String subGroup) {
        this._subGroup = subGroup;
    }

    /**
     * Sets Item Version
     *
     * @param version
     */
    public void setVersion(CacheItemVersion version) {
        this._version = version;
    }

    /**
     *
     * @param namedTags Sets the value of the named tags
     */
    public void setNamedTags(NamedTagsDictionary namedTags) {
        this._namedTags = namedTags;
    }

    /**
     *
     * @return NamedTagsDictionary associated with CacheItem
     */
    public NamedTagsDictionary getNamedTags() {
        return this._namedTags;
    }

    /**
     * @param resyncProviderName provider used for re-synchronization of item
     */
    public void setResyncProviderName(String resyncProviderName) {
        this._resyncProviderName = resyncProviderName;
    }

    /**
     *
     * @return provider used for re-synchronization of item
     */
    public String getResyncProviderName() {

        return _resyncProviderName;
    }

    /**
     * Internal use only
     *
     * @deprecated
     * @return
     */
    public Date getCreationTime() {
        return _creationTime;
    }

    /**
     * internal use only
     *
     * @deprecated
     * @return
     */
    public Date getLastModifiedTime() {
        return _lastModifiedTime;
    }

    /**
     * Internal Use only
     *
     * @deprecated
     * @param _creationTime
     */
    public void setCreationTime(Date _creationTime) {
        this._creationTime = _creationTime;
    }

    /**
     * Internal use only
     *
     * @param _lastModifiedTime
     * @deprecated
     */
    public void setLastModifiedTime(Date _lastModifiedTime) {
        this._lastModifiedTime = _lastModifiedTime;
    }

    public void addCacheDataNotificationListener(CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet, EventDataFilter dataFilter) {
        if (listener == null) {
            return;
        }

        if (eventEnumSet == null) {
            return;
        }

        if (eventEnumSet.contains(EventType.ItemRemoved)) {
            _cacheItemRemovedListener = listener;
            _itemRemovedDataFilter = dataFilter;
        }

        if (eventEnumSet.contains(EventType.ItemUpdated)) {
            _cacheItemUpdatedCallback = listener;
            _itemUpdatedDataFilter = dataFilter;
        }
    }

    public void removeCacheDataNotificationListener(CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet) {
        if (listener == null) {
            return;
        }

        if (eventEnumSet.contains(EventType.ItemRemoved)) {
            if (_cacheItemRemovedListener != null && _cacheItemRemovedListener == listener) {
                _cacheItemRemovedListener = null;
                _itemRemovedDataFilter = EventDataFilter.None;
            }
        }

        if (eventEnumSet.contains(EventType.ItemUpdated)) {
            if (_cacheItemUpdatedCallback != null && _cacheItemUpdatedCallback == listener) {
                _cacheItemUpdatedCallback = null;
                _itemUpdatedDataFilter = EventDataFilter.None;
            }
        }
    }

    public void addCacheDataNotificationListener(CacheDataModificationListener listener, EnumSet<EventType> eventEnumSet) {
        this.addCacheDataNotificationListener(listener, eventEnumSet, EventDataFilter.None);
    }

    EventDataFilter getItemUpdatedDataFilter() {
        return this._itemUpdatedDataFilter;
    }

    EventDataFilter getItemRemovedDataFilter() {
        return this._itemRemovedDataFilter;
    }

    CacheDataModificationListener getCacheItemUpdatedListener() {
        return this._cacheItemUpdatedCallback;
    }

    CacheDataModificationListener getCacheItemRemovedListener() {
        return this._cacheItemRemovedListener;
    }
}
