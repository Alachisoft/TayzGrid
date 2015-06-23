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

package com.alachisoft.tayzgrid.caching.queries.filters;

import com.alachisoft.tayzgrid.caching.queries.AttributeIndex;
import com.alachisoft.tayzgrid.caching.queries.IIndexStore;

public class MemberFunction implements IFunctor, java.lang.Comparable {

    private String name;

    public MemberFunction(String memname) {
        name = memname;
    }

    public final String getMemberName() {
        return name;
    }

    @Override
    public final Object Evaluate(Object o) {
        try {
            Object obj = o;
            java.lang.reflect.Field field = obj.getClass().getField(name);
            if (field != null) {
                return field.get(obj);
            } else {
                return null;
            }
        } catch (Exception noSuchFieldException) {
            return null;
        }
    }

    public final IIndexStore GetStore(AttributeIndex index) {
        if (index != null) {
            return index.GetStore(name);
        }
        return null;
    }

    @Override
    public String toString() {
        return "GetMember(" + name + ")";
    }

    @Override
    public final int compareTo(Object obj) {
        if (obj instanceof MemberFunction) {
            MemberFunction other = (MemberFunction) obj;
            return name.compareTo(other.name);
        }
        return -1;
    }
}
