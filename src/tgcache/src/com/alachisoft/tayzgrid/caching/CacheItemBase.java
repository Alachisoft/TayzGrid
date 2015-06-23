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

package com.alachisoft.tayzgrid.caching;

public class CacheItemBase implements java.io.Serializable {

    /**
     * The actual object provided by the client application
     */
    private Object _v = null;

    protected CacheItemBase() {
    }

    /**
     * Default constructor. No call back.
     * @param obj
     */
    public CacheItemBase(Object obj) {
        if (obj instanceof byte[]) {
            obj = UserBinaryObject.CreateUserBinaryObject((byte[]) obj);
        }
        _v = obj;
    }

    /**
     * The actual object provided by the client application
     * @return 
     */
    public Object getValue() {
        return _v;
    }

    public void setValue(Object value) {
        _v = value;
    }
}
