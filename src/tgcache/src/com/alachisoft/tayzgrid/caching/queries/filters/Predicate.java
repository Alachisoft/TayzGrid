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

import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.caching.queries.AttributeIndex;
import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.caching.topologies.local.LocalCacheBase;
import com.alachisoft.tayzgrid.common.datastructures.SortedMap;

public abstract class Predicate implements IPredicate {

    private boolean inverse;

    public final boolean getInverse() {
        return inverse;
    }

    public void Invert() {
        inverse = !inverse;
    }

    @Override
    public final boolean Evaluate(Object o) {
        return !inverse == ApplyPredicate(o);
    }

    public java.util.ArrayList ReEvaluate(AttributeIndex index, LocalCacheBase cache, java.util.Map attributeValues, String cacheContext) throws StateTransferException, Exception {
        QueryContext context = new QueryContext(cache);
        context.setAttributeValues(attributeValues);
        context.setIndex(index);
        context.setCacheContext(cacheContext);

        Execute(context, null);

        context.getTree().Reduce();
        return context.getTree().getLeftList();
    }

    public void Execute(QueryContext queryContext, Predicate nextPredicate) throws StateTransferException, java.lang.Exception {
    }

    public void ExecuteInternal(QueryContext queryContext, tangible.RefObject<SortedMap> list) throws java.lang.Exception {
    }

    public abstract boolean ApplyPredicate(Object o);
}
