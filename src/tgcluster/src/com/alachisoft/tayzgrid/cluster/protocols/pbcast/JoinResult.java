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

package com.alachisoft.tayzgrid.cluster.protocols.pbcast;

// $Id: JoinRsp.java,v 1.2 2004/03/30 06:47:18 belaban Exp $
 
public enum JoinResult
{

    Success,
    MaxMbrLimitReached,  
    Rejected,
    HandleLeaveInProgress,
    HandleJoinInProgress,
    MembershipChangeAlreadyInProgress;

    public int getValue()
    {
        return this.ordinal();
    }

    public static JoinResult forValue(int value)
    {
        return values()[value];
    }
}
