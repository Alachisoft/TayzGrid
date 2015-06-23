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
package com.alachisoft.tayzgrid.web.aggregation;

import com.alachisoft.tayzgrid.runtime.aggregation.Aggregator;
import com.alachisoft.tayzgrid.runtime.mapreduce.KeyValuePair;
import com.alachisoft.tayzgrid.runtime.mapreduce.Reducer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author
 */
class AggregatorReducer implements Reducer<Object, Object, Object> {

    private final Aggregator aggregator;
    private final List reducerList;
    private final Object aggkey;
    private final Class classType;

    public AggregatorReducer(Object key, Aggregator a , Class ctype) {
        this.aggregator = a;
        this.reducerList = new ArrayList();
        this.aggkey = key;
        classType = ctype;
    }

    @Override
    public void reduce(Object value) {
        if(value!=null)
        {
            reducerList.add(classType.cast(value));
        }
    }

    @Override
    public void finishReduce(KeyValuePair<Object, Object> context) {
        context.setKey(aggkey);
        context.setValue(aggregator.aggragateAll(reducerList));
    }

}
