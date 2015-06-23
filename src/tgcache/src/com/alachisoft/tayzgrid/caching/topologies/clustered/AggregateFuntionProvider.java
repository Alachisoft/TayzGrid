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

package com.alachisoft.tayzgrid.caching.topologies.clustered;

import com.alachisoft.tayzgrid.common.net.ObjectProvider;
import com.alachisoft.tayzgrid.common.net.IRentableObject;

public class AggregateFuntionProvider extends ObjectProvider {

    public AggregateFuntionProvider() {
    }

    public AggregateFuntionProvider(int initialsize) {
        super(initialsize);
    }

    @Override
    protected IRentableObject CreateObject() {
        return new AggregateFunction();
    }

    @Override
    public String getName() {
        return "AggregateFuntionProvider";
    }

    @Override
    protected void ResetObject(Object obj) {
        AggregateFunction af = (AggregateFunction) ((obj instanceof AggregateFunction) ? obj : null);
        if (af != null) {
            af.setFunctions(null);
        }
    }

    @Override
    public java.lang.Class getObjectType() {
        if (_objectType == null) {
            _objectType = AggregateFunction.class;
        }
        return _objectType;
    }
}
