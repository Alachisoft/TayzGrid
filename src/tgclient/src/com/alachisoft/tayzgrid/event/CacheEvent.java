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

package com.alachisoft.tayzgrid.event;

//~--- non-JDK imports --------------------------------------------------------

import com.alachisoft.tayzgrid.web.caching.CacheItemRemovedReason;
import java.util.EventObject;

//~--- classes ----------------------------------------------------------------

/**
 * CacheEvent is used to notify interested parties that
 * something has happened with respect to a cache.
 *
 * @version 0.1
 */
public class CacheEvent extends EventObject {

    /**
     * Creates a new object representing a cache event.
     *
     * @param source the cache object responsible for the event
     * @param type the event type
     * @param key the key of the item added.
     */
    public CacheEvent(Object source, EventType type, Object key) {
        super(source);
        this.type = type;
        this.key  = key;
    }

    /**
     * Creates a new object representing a cache event.
     *
     * @param source the cache object responsible for the event
     * @param type the event type
     * @param key the key of the item added.
     * @param value the value of the item added.
     */
    public CacheEvent(Object source, EventType type, Object key, Object value) {
        super(source);
        this.type = type;
        this.key   = key;
        this.value = value;
    }

    /**
     * Creates a new object representing a cache event.
     *
     * @param source the cache object responsible for the event
     * @param type the event type
     * @param key the key of the item added.
     * @param value the value of the item added.
     * @param reason the reason for the removal of the item from
     *  the cache.
     * @see CacheItemRemovedReason
     */
    public CacheEvent(Object source,  EventType type, Object key, Object value,
            CacheItemRemovedReason reason) {
        super(source);
        this.type = type;
        this.key    = key;
        this.value  = value;
        this.reason = reason;
    }

    /**
     * Gets the type of event.
     *
     * @return the type
     */
    public EventType getEventType() {
        return type;
    }

    /**
     * Returns the Key of the item for which this event occured.
     * @return Returns the Key of the item for which this event occured.
     */
    public Object getKey() {
        return key;
    }

    /**
     * Returns the Value of the item for which this event occured.
     * @return Returns the Value of the item for which this event occured.
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns the remove reason for the Reamve Event.
     * @return Returns the remove reason for the Reamve Event.
     */
    public CacheItemRemovedReason getRemoveReason() {
        return reason;
    }

    private EventType type;
    private Object key;
    private Object value;
    private CacheItemRemovedReason reason;

    /**
     * Defines the ADDED, UPDATED, REMOVED and CLEARED event types, along
     * with their string representations, returned by toString().
     */
    public static final class EventType {

        /**
         *
         * @param s
         */
        private EventType(String s) {
            typeString = s;
        }

        /**
         * Added type.
         */
        public static final EventType ADDED = new EventType("ADDED");

        /**
         * Updated type.
         */
        public static final EventType UPDATED = new EventType("UPDATED");

        /**
         * Removed type.
         */
        public static final EventType REMOVED = new EventType("REMOVED");

        /**
         * Cleared type.
         */
        public static final EventType CLEARED = new EventType("CLEARED");

        /**
         * Converts the type to a string.
         *
         * @return the string
         */
        public String toString() {
            return typeString;
        }

        private String typeString;
    }
}

