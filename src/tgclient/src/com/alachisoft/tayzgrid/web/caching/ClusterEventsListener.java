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

import com.alachisoft.tayzgrid.caching.CallbackEntry;
import com.alachisoft.tayzgrid.caching.UserBinaryObject;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.IDisposable;
import com.alachisoft.tayzgrid.common.stats.UsageStats;
import com.alachisoft.tayzgrid.runtime.events.EventDataFilter;
import com.alachisoft.tayzgrid.web.events.EventCacheItem;
import com.alachisoft.tayzgrid.web.net.NodeInfo;
import java.util.ArrayList;


public class ClusterEventsListener implements IDisposable {


    Cache _parent;

    public ClusterEventsListener(Cache parent) {
        _parent = parent;

    }


    ///#region    /                 --- IDisposable ---           /
    /**
     * Performs application-defined tasks associated with freeing, releasing, or
     * resetting unmanaged resources.
     *
     *
     *
     */
    public void dispose() {
        try {
        } catch (java.lang.Exception e) {
        }
    }

    public void OnMemberJoined(com.alachisoft.tayzgrid.common.net.Address clusterAddress, com.alachisoft.tayzgrid.common.net.Address serverAddress) {
        try {
            ArrayList<MemberJoinedCallback> callbackList = _parent.getMemberJoinedCallbackList();

            if (callbackList != null && !callbackList.isEmpty()) {

                for (MemberJoinedCallback callback : callbackList) {
                    try {
                        callback.MemberJoinedCallback(new NodeInfo(serverAddress.getIpAddress(), serverAddress.getPort()));
                    } catch (RuntimeException e) {
                    }
                }
            }
        } catch (java.lang.Exception e) {
        }
    }

    public void OnMemberLeft(com.alachisoft.tayzgrid.common.net.Address clusterAddress, com.alachisoft.tayzgrid.common.net.Address serverAddress) {
        try {
            ArrayList<MemberLeftCallback> callbackList = _parent.getMemberLeftCallbackList();

            if (callbackList != null && !callbackList.isEmpty()) {
                for (MemberLeftCallback callback : callbackList) {
                    try {
                        callback.MemberLeftCallback(new NodeInfo(serverAddress.getIpAddress(), serverAddress.getPort()));
                    } catch (RuntimeException e) {
                    }
                }
            }
        } catch (java.lang.Exception e) {
        }
    }


    public void onActiveQueryChanged(String queryId, QueryChangeType changeType, Object key, boolean notifyAsync, EventCacheItem item, EventCacheItem oldItem, BitSet flag, EventDataFilter datafilter) throws Exception {
        if (item != null && item.getValue() != null) {
            item.setValue(GetObject(item.getValue(), flag));
        }
        if (oldItem != null && oldItem.getValue() != null) {
            oldItem.setValue(GetObject(oldItem.getValue(), flag));
        }

        _parent._perfStatsCollector.incrementEventProcessedPerSec();
    }

    private Object GetObject(Object value, BitSet Flag) throws Exception {
        try {
            if (value instanceof CallbackEntry) {
                value = ((CallbackEntry) value).getValue();
            }

            if (value instanceof UserBinaryObject) {
                value = ((UserBinaryObject) value).GetFullObject();
            }

            return _parent.safeDeserialize(value, _parent.getSerializationContext(), Flag);
        } catch (RuntimeException ex) {
            return value;
        }
    }
}
