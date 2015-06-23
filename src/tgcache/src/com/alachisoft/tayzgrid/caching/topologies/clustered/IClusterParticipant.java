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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;

public interface IClusterParticipant extends IClusterEventSink {

    /**
     * Authenticate the client and see if it is allowed to join the list of
     * valid members.
     *
     * @param address
     * @param identity
     * @return true if the node is valid and belongs to the scheme's cluster
     */
    boolean AuthenticateNode(Address address, NodeIdentity identity);

    /**
     * Called when a new member joins the group.
     *
     * @param address address of the joining member
     * @param identity additional identity information
     * @return true if the node joined successfuly
     */
    boolean OnMemberJoined(Address address, NodeIdentity identity) throws java.net.UnknownHostException;

    /**
     * Called when an existing member leaves the group.
     *
     * @param address address of the joining member
     * @return true if the node left successfuly
     */
    boolean OnMemberLeft(Address address, NodeIdentity identity);

    /**
     * Called after the membership has been changed. Lets the members do some
     * member oriented tasks.
     */
    void OnAfterMembershipChange() throws InterruptedException, OperationFailedException;

    /**
     * Called after the membership has been changed. Lets the members do some
     * member oriented tasks.
     */
    boolean IsInStateTransfer();
}