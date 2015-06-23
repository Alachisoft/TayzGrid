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

package com.alachisoft.tayzgrid.runtime.mapreduce;

import java.io.Serializable;
import java.util.HashMap;

public class OutputMap<Key extends Object, Value extends Object> implements Serializable{

    private HashMap<Key, Value> outputMap;
    
    public OutputMap()
    {
        outputMap = new HashMap<Key, Value>();
    }
    /**
     * Collects the output
     *
     * @param key
     * @param value
     */
    public void emit(Key key, Value value)
    {
        getOutputMap().put(key, value);
    }

    /**
     * @return the outputMap
     */
    public HashMap<Key, Value> getOutputMap() {
        return outputMap;
    }
}
