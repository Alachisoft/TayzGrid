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

package com.alachisoft.tayzgrid.web.events;

import com.alachisoft.tayzgrid.caching.EventCacheEntry;
import com.alachisoft.tayzgrid.caching.ItemRemoveReason;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.events.EventType;
import com.alachisoft.tayzgrid.web.caching.CacheItemRemovedReason;
import com.alachisoft.tayzgrid.web.caching.CacheItemVersion;
 
import com.alachisoft.tayzgrid.common.protobuf.EventIdProtocol.EventId;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class EventUtil {

    public static com.alachisoft.tayzgrid.caching.EventId ConvertToEventID(com.alachisoft.tayzgrid.common.protobuf.BulkEventItemResponseProtocol.BulkEventItemResponse eventItem, com.alachisoft.tayzgrid.persistence.EventType eventType) {
        com.alachisoft.tayzgrid.caching.EventId eventId;
        eventId = eventId = new com.alachisoft.tayzgrid.caching.EventId();
        EventId varEventId;

        switch (eventType) {
            case ITEM_ADDED_EVENT:
                varEventId = eventItem.getItemAddedEvent().getEventId();
                eventId.setEventUniqueID(varEventId.getEventUniqueId());
                eventId.setEventCounter(varEventId.getEventCounter());
                eventId.setOperationCounter(varEventId.getOperationCounter());
                eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_ADDED_EVENT);
                break;

            case ITEM_UPDATED_EVENT:
                varEventId = eventItem.getItemUpdatedEvent().getEventId();
                eventId.setEventUniqueID(varEventId.getEventUniqueId());
                eventId.setEventCounter(varEventId.getEventCounter());
                eventId.setOperationCounter(varEventId.getOperationCounter());
                eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_EVENT);
                break;

            case ITEM_UPDATED_CALLBACK:
                varEventId = eventItem.getItemUpdatedCallback().getEventId();
                eventId.setEventUniqueID(varEventId.getEventUniqueId());
                eventId.setEventCounter(varEventId.getEventCounter());
                eventId.setOperationCounter(varEventId.getOperationCounter());
                eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_UPDATED_CALLBACK);
                break;
 
            case ITEM_REMOVED_CALLBACK:
                varEventId = eventItem.getItemRemoveCallback().getEventId();
                eventId.setEventUniqueID(varEventId.getEventUniqueId());
                eventId.setEventCounter(varEventId.getEventCounter());
                eventId.setOperationCounter(varEventId.getOperationCounter());
                eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_CALLBACK);
                break;

            case ITEM_REMOVED_EVENT:
                varEventId = eventItem.getItemRemovedEvent().getEventId();
                eventId.setEventUniqueID(varEventId.getEventUniqueId());
                eventId.setEventCounter(varEventId.getEventCounter());
                eventId.setOperationCounter(varEventId.getOperationCounter());
                eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.ITEM_REMOVED_EVENT);
                break;

            case CACHE_CLEARED_EVENT:
                varEventId = eventItem.getCacheClearedEvent().getEventId();
                eventId.setEventUniqueID(varEventId.getEventUniqueId());
                eventId.setEventCounter(varEventId.getEventCounter());
                eventId.setOperationCounter(varEventId.getOperationCounter());
                eventId.setEventType(com.alachisoft.tayzgrid.persistence.EventType.CACHE_CLEARED_EVENT);
                break;
        }
        return eventId;
    }

    public static EventCacheItem ConvertToEventEntry(com.alachisoft.tayzgrid.common.protobuf.EventCacheItemProtocol.EventCacheItem cacheItem) {
 
        byte[] objectValue = null;

        if (cacheItem == null) {
            return null;
        }

        EventCacheItem item = new EventCacheItem();
        item.setCacheItemPriority(CacheItemPriority.forValue(cacheItem.getPriority()));
        
        CacheItemVersion cacheItemVersion = new CacheItemVersion();
        cacheItemVersion.setVersion(cacheItem.getItemVersion());
        item.setCacheItemVersion(cacheItemVersion);
        
        item.setGroup(tangible.DotNetToJavaStringHelper.isNullOrEmpty(cacheItem.getGroup()) ? null : cacheItem.getGroup());
        item.setReSyncExpiredItems(cacheItem.getResyncExpiredItems());
        
        item.setReSyncProviderName(tangible.DotNetToJavaStringHelper.isNullOrEmpty(cacheItem.getResyncProviderName()) ? null : cacheItem.getResyncProviderName());
        item.setSubGroup(tangible.DotNetToJavaStringHelper.isNullOrEmpty(cacheItem.getSubGroup()) ? null : cacheItem.getSubGroup());
        List<ByteString> cacheItemValueList = cacheItem.getValueList();
        
        List<byte[]> bite = new ArrayList<byte[]>();
                                    
        for(ByteString byteString : cacheItemValueList)
        {
            bite.add(byteString.toByteArray());
        }
                    
        if (cacheItemValueList != null && !cacheItemValueList.isEmpty()) {
             
            UserBinaryObject ubObject = new UserBinaryObject((Object[]) bite.toArray());
            objectValue = ubObject.GetFullObject();
            item.setValue(objectValue);
        }

        return item;

    }
    
    public static EventType getEventType(EnumSet<EventType> eventEnumSet) {
        
        if (eventEnumSet.contains(EventType.ItemAdded)) {
            return EventType.ItemAdded;
        } else if (eventEnumSet.contains(EventType.ItemRemoved)) {
            return EventType.ItemRemoved;
        } else if (eventEnumSet.contains(EventType.ItemUpdated)) {
            return EventType.ItemUpdated;
        }else if (eventEnumSet.contains(EventType.CacheCleared)) {
            return EventType.CacheCleared;
        }

        return EventType.ItemAdded;
    }

    /**
     * For Inproc only
     *
     * @param entry
     * @return
     */
    public static EventCacheItem ConvertToItem(EventCacheEntry entry) {
 
        byte[] objectValue = null;

        if(entry == null)
            return null;
        
        EventCacheItem item = new EventCacheItem();

        item.setCacheItemPriority(CacheItemPriority.forValue(entry.getPriority()));

        CacheItemVersion version = new CacheItemVersion();
        version.setVersion(entry.getVersion());

        item.setCacheItemVersion(version);
        item.setGroup(entry.getGroup());
        item.setReSyncExpiredItems(entry.getReSyncExpiredItems());
        item.setReSyncProviderName(entry.getReSyncProviderCacheItem());
        item.setSubGroup(entry.getSubGroup());
        if (entry.getValue() != null) {
            UserBinaryObject ubObject = (UserBinaryObject) ((entry.getValue() instanceof UserBinaryObject) ? entry.getValue() : null);
            if (ubObject != null) {
                objectValue = ubObject.GetFullObject();
                item.setValue(objectValue);
            } else {
                item.setValue(entry.getValue());
            }
        }
        return item;
    }

    public static CacheItemRemovedReason ConvertToCIRemoveReason(ItemRemoveReason reason) {
        switch (reason) {
           
            case Expired:
                return CacheItemRemovedReason.Expired;
            case Removed:
                return CacheItemRemovedReason.Removed;
            case Underused:
                return CacheItemRemovedReason.Underused;
            default:
                return CacheItemRemovedReason.Underused;
        }
    }
}
