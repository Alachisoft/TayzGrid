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

public class CompositeFunction 
{
    private IFunctor func;
    private IFunctor func2;

    public CompositeFunction(IFunctor func, IFunctor func2) {
        this.func = func;
        this.func2 = func2;
    }

    public final Object Evaluate(Object o) {
        return func.Evaluate(func2.Evaluate(o));
    }

    public String toString() {
        return func + "." + func2;
    }

    public final int compareTo(Object obj) {
        if (obj instanceof CompositeFunction) {
            CompositeFunction other = (CompositeFunction) obj;
            return (((java.lang.Comparable) func).compareTo(other.func) == 0) && ((java.lang.Comparable) func2).compareTo(other.func2) == 0 ? 0 : -1;
        }
        return -1;
    }
}
