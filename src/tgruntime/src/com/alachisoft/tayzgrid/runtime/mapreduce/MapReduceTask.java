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

import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentNullException;
import java.io.Serializable;

public class MapReduceTask<InputKey extends Object, InputValue extends Object, OutputKey extends Object, OutputValue extends Object> 
    implements Serializable
{

    private Mapper<InputKey, InputValue, OutputKey, OutputValue> _mapper;
    private CombinerFactory<OutputKey, OutputValue> _combiner;
    private ReducerFactory<OutputKey, OutputValue,?,?> _reducer;
    
    private MapReduceInput<InputKey, InputValue> _mrInputProvider;

    private MapReduceOutput<InputKey,InputValue> _mrOutput;
    
    private int _mrOutputOption = 0;
    
    private Filter _filter;

    public MapReduceTask() {

    }

    public void setMapper(Mapper<InputKey, InputValue, OutputKey, OutputValue> mapper)
            throws ArgumentNullException
    {
        if (mapper == null) {
            throw new ArgumentNullException("mapper");
        }
        this._mapper = mapper;

    }

    public void setCombiner(CombinerFactory<OutputKey, OutputValue> combinerFactory) {
        this._combiner = combinerFactory;
    }

    public void setReducer(ReducerFactory<OutputKey, OutputValue, ?, ?> reducerFactory) {
        this._reducer = reducerFactory;
    }
            
    public void setMapReduceInput(MapReduceInput<InputKey, InputValue> input) {
        this._mrInputProvider = input;
    }
    
    public Mapper<InputKey, InputValue, ?, ?> getMapper() {
        return _mapper;
    }

    public CombinerFactory<OutputKey, OutputValue> getCombiner() {
        return _combiner;
    }

    public ReducerFactory<OutputKey, OutputValue, ?,?> getReducer() {
        return _reducer;
    }

    public Filter getFilter() {
        return _filter;
    }

    public MapReduceInput<InputKey, InputValue> getMapReduceInputProvider() {
        return _mrInputProvider;
    }

    public MapReduceOutput<InputKey,InputValue> getMapReduceOutput() {
        return _mrOutput;
    }

    /**
     * @param filter the _filter to set
     */
    public void setFilter(Filter filter) {
        this._filter = filter;
    }
}
