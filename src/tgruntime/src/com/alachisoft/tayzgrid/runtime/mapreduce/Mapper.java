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

public interface Mapper<InputKey extends Object, InputValue extends Object, OutputKey extends Object, OutputValue extends Object>
        extends Serializable {

    /**
     * For every key-value pair input, map method is executed
     *
     * @param key
     * @param value
     * @param context
     */
    public void map(InputKey key, InputValue value, OutputMap<OutputKey, OutputValue> context);

}
