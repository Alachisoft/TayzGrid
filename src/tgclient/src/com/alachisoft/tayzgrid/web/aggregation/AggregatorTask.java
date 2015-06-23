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

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.runtime.aggregation.Aggregator;
import com.alachisoft.tayzgrid.runtime.aggregation.ValueExtractor;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *
 * @author tayzgrid_dev
 */
public final class AggregatorTask {

    private final com.alachisoft.tayzgrid.runtime.mapreduce.MapReduceTask<Object, Object, Object, Object> aggregatorTask;
    private final ValueExtractor extractor;
    private final Aggregator aggregator;
    private AggregatorCombinerFactory aggregatorCombinerFactory = null;
    private AggregatorMapper aggregatorMapper = null;
    private AggregatorReducerFactory aggregatorReducerFactory = null;
    private java.lang.Class aggregatorInputType = Object.class;

    public Class getBuiltInAggregatorType() {
        return getAggregatorTypeClass();
    }

    public AggregatorTask(ValueExtractor valueExtractor, Aggregator userAggregator) {
        aggregatorTask = new MapReduceTask();
        extractor = valueExtractor;
        aggregator = userAggregator;
    }

    public MapReduceTask createMapReduceTask() throws ArgumentNullException {

        for (Type iface : extractor.getClass().getGenericInterfaces()) {
            ParameterizedType type = null;
            try {
                type = (ParameterizedType) iface;
            } catch (Exception ex) {
            }
            if (type != null && type.getRawType().equals(com.alachisoft.tayzgrid.runtime.aggregation.ValueExtractor.class)) {
                if (((ParameterizedType) (iface)).getActualTypeArguments().length > 1) {
                    this.aggregatorInputType = (Class) ((ParameterizedType) (iface)).getActualTypeArguments()[1];
                }
            }
            break;
        }

        aggregatorMapper = new AggregatorMapper(extractor);
        aggregatorCombinerFactory = new AggregatorCombinerFactory(aggregator, aggregatorInputType);
        aggregatorReducerFactory = new AggregatorReducerFactory(aggregator, aggregatorInputType);
        aggregatorTask.setMapper(aggregatorMapper);
        aggregatorTask.setCombiner(aggregatorCombinerFactory);
        aggregatorTask.setReducer(aggregatorReducerFactory);
        return aggregatorTask;
    }

    private Class getAggregatorTypeClass() {
        Class typeclass = Object.class;
        if (isTypeOf(aggregator.getClass())) {

            for (Type iface : aggregator.getClass().getGenericInterfaces()) {
                ParameterizedType type = null;
                try {
                    type = (ParameterizedType) iface;
                } catch (Exception ex) {
                }
                if (type != null && type.getRawType().equals(com.alachisoft.tayzgrid.runtime.aggregation.Aggregator.class)) {
                    if (((ParameterizedType) (iface)).getActualTypeArguments().length > 1) {
                        typeclass = (Class) ((ParameterizedType) (iface)).getActualTypeArguments()[1];
                    }
                }
                break;
            }
        }

        return typeclass;
    }

    private boolean isTypeOf(Class classType) {
        return classType.equals(BuiltinAggregator.IntegerAggregator.class)
                || classType.equals(BuiltinAggregator.DoubleAggregator.class)
                || classType.equals(BuiltinAggregator.LongAggregator.class)
                || classType.equals(BuiltinAggregator.ShortAggregator.class)
                || classType.equals(BuiltinAggregator.BigDecimalAggregator.class)
                || classType.equals(BuiltinAggregator.BigIntegerAggregator.class)
                || classType.equals(BuiltinAggregator.FloatAggregator.class)
                || classType.equals(BuiltinAggregator.StringAggregator.class)
                || classType.equals(BuiltinAggregator.DateAggregator.class)
                || classType.equals(BuiltinAggregator.CountAggregator.class)
                || classType.equals(BuiltinAggregator.DistinctAggregator.class);
    }

}
