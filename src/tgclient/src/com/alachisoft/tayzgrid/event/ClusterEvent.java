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

//~--- JDK imports ------------------------------------------------------------

import java.util.EventObject;
import javax.swing.event.EventListenerList;

//~--- classes ----------------------------------------------------------------

/**
 * ClusterEvent is used to notify interested parties that
 * something has happened with respect to the cluster.
 *
 * @version 0.1
 */
public class ClusterEvent extends EventObject {

    //~--- fields -------------------------------------------------------------

    //~--- constructors -------------------------------------------------------

     /**
     * Creates a new object representing a cluster event.
     *
     * @param source the object responsible for the event
     * @param type the event type
     * @param ip the ip of the memeber.
     * @param port the port of the memeber.
     */
    public ClusterEvent(Object source, EventType type, String ip, int port,String cacheId) {
        super(source);
        this.type = type;
        this.cacheId=cacheId;
        this.ip   = ip;
        this.port = port;
    }

    /**
     * Creates a new object representing a cluster event.
     * @param type The type of cluster event.
     * @param source the object responsible for the event
     * @param cacheId the cache-id of the stopped cache.
     */
    public ClusterEvent(Object source, EventType type, String cacheId) {
        super(source);
        this.type = type;
        this.cacheId = cacheId;
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
     * Returns the NCache Socket Server port.
     * @return Returns the NCache Socket Server port.
     */
    public long getPort() {
        return port;
    }

    /**
     * Returns IP of the member node.
     * @return  Returns IP of the member node.
     */
    public String getIp() {
        return ip.replace("/", "");
    }

    /**
     * Returns cache-id of the stopped cache.
     * @return  Returns cache-id of the stopped cache.
     */
    public String getCacheId() {
        return cacheId;
    }

    private EventType type;
    private String ip="";
    private int port;
    private String cacheId;

    /**
     * Defines the JOINED and LEFT event types, along
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
         * Joined type.
         */
        public static final EventType JOINED = new EventType("JOINED");

        /**
         * Left type.
         */
        public static final EventType LEFT = new EventType("LEFT");


        /**
         * Left type.
         */
        public static final EventType STOPPED = new EventType("STOPPED");

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

