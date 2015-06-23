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

import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.common.datastructures.SortedMap;
import com.alachisoft.tayzgrid.parser.ParserException;

public class IsNullPredicate extends Predicate implements java.lang.Comparable {

    private IFunctor functor;

    public IsNullPredicate(IFunctor f) {
        functor = f;
    }

    @Override
    public boolean ApplyPredicate(Object o) {
        return o == null;
    }

    @Override
    public String toString() {
        return getInverse() ? "is not null" : "is null";
    }

    @Override
    public void ExecuteInternal(QueryContext queryContext, tangible.RefObject<SortedMap> list) {
        throw new ParserException("Incorrect query format. \'" + this.toString() + "\' not supported.");
    }

    @Override
    public void Execute(QueryContext queryContext, Predicate nextPredicate) {
        throw new ParserException("Incorrect query format. \'" + this.toString() + "\' not supported.");
    }

    @Override
    public final int compareTo(Object obj) {
        if (obj instanceof IsNullPredicate) {
            return (new Boolean(getInverse())).compareTo(((Predicate) obj).getInverse());
        }
        return -1;
    }
}
