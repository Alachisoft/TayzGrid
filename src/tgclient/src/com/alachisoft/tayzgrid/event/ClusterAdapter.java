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

/**
 * An abstract adapter class for receiving cluster events.
 * The methods in this class are empty. This class exists as
 * convenience for creating listener objects.
 * <P>
 * Clustre events let you track when a member joins, leaves the cluster or
 * cache stops on a node.
 * <P>
 * Extend this class to create a <code>ClusterEvent</code> listener
 * and override the methods for the events of interest. (If you implement the
 * <code>ClusterListener</code> interface, you have to define all of
 * the methods in it. This abstract class defines null methods for them
 * all, so you can only have to define methods for events you care about.)
 * <P>
 * Create a listener object using the extended class and then register it with
 * a component using the component's <code>addClusterListener</code>
 * method. When a member joins, leaves the cluster or when the cache is stopped
 * the relevant method in the listener object is invoked and the
 * <code>ClustereEvent</code> is passed to it.
 *
 * @author 
 * @version 1.0
 *
 * @see ClusterEvent
 * @see ClusterListener
 *
 */
public abstract class ClusterAdapter implements CacheStatusEventListener {

    /**
     * Invoked when a member joins the cluster.
     * @param e Contains the event details.
     */
    public void memberJoined(ClusterEvent e) {}

    /**
     * Invoked when a member leaves the cluster.
     * @param e Contains the event details.
     */
    public void memberLeft(ClusterEvent e) {}

    /**
     * Invoked when cache on a member node stops.
     * @param e Contains the event details.
     */
    public void cacheStopped(ClusterEvent e) {}
}
