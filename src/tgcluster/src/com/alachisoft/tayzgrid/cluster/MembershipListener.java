/*
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

package com.alachisoft.tayzgrid.cluster;

import com.alachisoft.tayzgrid.common.net.Address;

public interface MembershipListener
{

    /**
     * Called by JGroups to notify the target object of a change of membership. <b>No long running actions should be done in this callback in the case of Ensemble, as this would
     * block Ensemble.</b> If some long running action needs to be performed, it should be done in a separate thread (cf.
     * <code>../Tests/QuoteServer.java</code>).
     * @param new_view
     * @throws java.lang.InterruptedException
     */
    void viewAccepted(View new_view) throws InterruptedException, Exception;

    /**
     * Called when a member is suspected
     */
    void suspect(Address suspected_mbr);

    /**
     * Block sending and receiving of messages until viewAccepted() is called
     */
    void block();

    /**
     * Whether to allow the joining of new node
     */
    boolean AllowJoin();
}
