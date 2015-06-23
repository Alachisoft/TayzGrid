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

public class FunctorEqualsFunctorPredicate extends Predicate implements java.lang.Comparable {

    private IFunctor functor;
    private IFunctor generator;

    public FunctorEqualsFunctorPredicate(IFunctor lhs, IFunctor rhs) {
        functor = lhs;
        generator = rhs;
    }

    @Override
    public boolean ApplyPredicate(Object o) {
        Object lhs = functor.Evaluate(o);
        Object rhs = generator.Evaluate(o);
        return lhs.equals(rhs);
    }

    @Override
    public String toString() {
        return functor + (getInverse() ? " != " : " == ") + generator;
    }

    @Override
    public final int compareTo(Object obj) {
        if (obj instanceof FunctorEqualsFunctorPredicate) {
            FunctorEqualsFunctorPredicate other = (FunctorEqualsFunctorPredicate) obj;
            if (getInverse() == other.getInverse()) {
                return ((java.lang.Comparable) functor).compareTo(other.functor) == 0 && ((java.lang.Comparable) generator).compareTo(other.generator) == 0 ? 0 : -1;
            }
        }
        return -1;
    }
}
