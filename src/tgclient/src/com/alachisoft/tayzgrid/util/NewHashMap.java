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

package com.alachisoft.tayzgrid.util;

import java.util.HashMap;
import java.util.List;


/// <summary>
    /// Contains new hashmap and related information for client
    /// </summary>
    public class NewHashMap
    {
        private long _lastViewId;
        private HashMap _map;
        private List<String> _members;
        private int _bucketSize;
        private boolean _updateMap;

        /// <summary>
        /// Default constructor
        /// </summary>
        public NewHashMap()
        {
        }

        public NewHashMap(long lastViewid, HashMap<Integer, String> map, List<String> members, int bucketSize, boolean updateMap)
        {
            this._lastViewId = lastViewid;
            this._map = map;
            this._members = members;
            this._bucketSize = bucketSize;
            this._updateMap = updateMap;
        }

        /// <summary>
        /// Last view id that was published
        /// </summary>
        public long getLastViewId()
        {
            return this._lastViewId;
        }

        /// <summary>
        /// New hash map
        /// </summary>
        public HashMap getMap()
        {
            return this._map;
        }

        /// <summary>
        /// List of server members (string representation of IP addresses)
        /// </summary>
        public List<String> getMembers()
        {
            return this._members;
        }

        public int getBucketSize() {
            return _bucketSize;
        }

        public boolean getUpdateMap() {
            return _updateMap;
        }
    }
