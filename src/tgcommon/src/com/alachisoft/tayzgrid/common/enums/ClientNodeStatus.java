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

package com.alachisoft.tayzgrid.common.enums;


/**
 * Client cache information on client nodes in NCache Explorer tree. 1. Unavailable means, client node is no more the part of this cluster. i.e. Cluster entry removed from client
 * config on this node 2. ClientCacheUnavailable means, client cache is not created on the client node. 3. ClientCacheEnabled means, client cache is created and is started. 4.
 * ClientCacheDisabled means, client cache is created but is stopped. 5. ClientCacheNotRegistered is same as ClientCacheUnavailable.. but it is used to change the state of client
 * cache instead of client node.
 */
public enum ClientNodeStatus
{

    Unavailable,
    ClientCacheUnavailable,
    ClientCacheEnabled,
    ClientCacheDisabled,
    ClientCacheNotRegistered,
    UpdatingStatus;

    public int getValue()
    {
        return this.ordinal();
    }

    public static ClientNodeStatus forValue(int value)
    {
        return values()[value];
    }
}
