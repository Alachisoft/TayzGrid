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

import com.alachisoft.tayzgrid.command.Command;
import com.alachisoft.tayzgrid.web.caching.CacheItemRemovedReason;
import java.util.EventObject;

//~--- JDK imports ------------------------------------------------------------

import javax.swing.event.EventListenerList;

//~--- classes ----------------------------------------------------------------

/**
 * CustomEvent is used to notify interested parties that
 * something has happened with respect to a custom event.
 *
 * @version 0.1
 */
public class CustomEvent extends EventObject {

    //~--- fields -------------------------------------------------------------

    //~--- constructors -------------------------------------------------------


    /**
     * Creates a new object representing a custom event.
     *
     * @param source the cache object responsible for the event
     * @param type the event type
     * @param key the key of the item.
     * @param value the value of the item.
     */
    public CustomEvent(Object source, EventType type, Object key, Object value) {
        super(source);
        this.type = type;
        this.key   = key;
        this.value = value;
    }

    //~--- methods ------------------------------------------------------------

    //~--- get methods --------------------------------------------------------

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
     * @return  Returns the Key of the item for which this event occured.
     */
    public Object getKey() {
        return key;
    }

    /**
     * Returns the Value of the item for which this event occured.
     * @return  Returns the Key of the item for which this event occured.
     */
    public Object getValue() {
        return value;
    }

    private EventType type;
    private Object key;
    private Object value;

    /**
     * Defines the CUSTOM event type, along
     * with its string representation, returned by toString().
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
         * Custom type.
         */
        public static final EventType CUSTOM = new EventType("CUSTOM");

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

