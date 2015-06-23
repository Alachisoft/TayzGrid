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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Represents the version of each cache item. An instance of this class is used
 * in the optimistic concurrency model to ensure the data integrity.
 */
public final class CacheItemVersion implements Comparable<CacheItemVersion> {


    private long _version;


    /**
     * Get item version
     * @return Item's version
     */
    public long getVersion(){
        return this._version;
    }

    /**
     * Set item version
     * @param version Item's version
     */
    public void setVersion(long version){
        this._version = version;
    }

    /**
     * The string representation of this class.
     * @return a string representation of the object.
     */
    @Override
    public String toString(){
        return Long.toString(this._version);
    }

    /**
     * Compare CacheItemVersion with current instance of item version
     * @param itemVersion item version to be compared
     * @return 0 if two instance are equal. An integer greater then 0 if
     * this instance is greater. An integer less than 0 if this instance is smaller
     */
    public int compareTo(CacheItemVersion itemVersion) {

        if (itemVersion == null) {
            return -1;
        }
        long version = itemVersion._version;
        if (version == this._version) {
            return 0;
        }
        if (version < this._version) {
            return 1;
        }
        if (version > this._version) {
            return -1;
        }

        return -1;
    }
}

