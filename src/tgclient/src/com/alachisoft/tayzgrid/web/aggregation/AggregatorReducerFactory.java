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
import com.alachisoft.tayzgrid.runtime.mapreduce.Reducer;
import com.alachisoft.tayzgrid.runtime.mapreduce.ReducerFactory;

/**
 *
 * @author
 */
class AggregatorReducerFactory implements ReducerFactory<Object, Object, Object, Object> {

    private final Aggregator aggregator;
    private final Class classType;

    public AggregatorReducerFactory(Aggregator a, Class cType) {
        this.aggregator = a;
        this.classType=cType;
    }

    @Override
    public Reducer<Object, Object, Object> getReducer(Object key) {
       return  new AggregatorReducer(key, aggregator, classType);
    }
}
