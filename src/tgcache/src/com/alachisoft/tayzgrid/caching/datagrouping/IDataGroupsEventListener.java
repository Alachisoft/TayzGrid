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

package com.alachisoft.tayzgrid.caching.datagrouping;

import com.alachisoft.tayzgrid.caching.ItemRemoveReason;

/**
 * An interface for the data group event listener.
 */
public interface IDataGroupsEventListener {

    /**
     * Fired when a new data group is added in the cache.
     *
     * @param group Newly added group
     */
    void OnDataGroupAdded(String group);

    /**
     * Fired when an existing data group is removed from the cache.
     *
     * @param group Removed data group
     * @param lastItemRemovedReason Reason for the removal of last group item.
     */
    void OnDataGroupRemoved(String group, ItemRemoveReason lastItemRemovedReason);
}
