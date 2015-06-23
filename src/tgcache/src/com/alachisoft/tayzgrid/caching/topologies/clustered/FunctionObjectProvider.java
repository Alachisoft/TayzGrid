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

public class FunctionObjectProvider extends ObjectProvider {

    public FunctionObjectProvider() {
    }

    public FunctionObjectProvider(int initialsize) {
        super(initialsize);
    }

    @Override
    protected IRentableObject CreateObject() {
        return new Function();
    }

    @Override
    public String getName() {
        return "FunctionObjectProvider";
    }

    @Override
    protected void ResetObject(Object obj) {
        Function f = (Function) ((obj instanceof Function) ? obj : null);
        if (f != null) {
            f.setOperand(0);
            f.setOperand(null);
            f.setExcludeSelf(true);
        }
    }

    @Override
    public java.lang.Class getObjectType() {
        if (_objectType == null) {
            _objectType = Function.class;
        }
        return _objectType;
    }
}