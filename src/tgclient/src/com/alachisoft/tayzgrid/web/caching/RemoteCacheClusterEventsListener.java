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

import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.net.Address;


public class RemoteCacheClusterEventsListener implements IDisposable {


    private ClusterEventsListener _listener;
    /**
     * Constructor.
     *
     * @param parent
     */
    public RemoteCacheClusterEventsListener(ClusterEventsListener parent) {
        _listener = parent;
    }


    ///#region    /                 --- IDisposable ---           /
    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     *
     *
     */
    public final void dispose() {
    }

    public final void OnMemberJoined(Address clusterAddress, Address serverAddress) {
        try {
            if (_listener != null) {
                
                _listener.OnMemberJoined(clusterAddress, serverAddress);
            }
        } catch (java.lang.Exception e) {
        }
    }

    public final void OnMemberLeft(Address clusterAddress, Address serverAddress) {
        try {
            if (_listener != null) {
                
                _listener.OnMemberLeft(clusterAddress, serverAddress);
            }
        } catch (java.lang.Exception e) {
        }
    }

}
