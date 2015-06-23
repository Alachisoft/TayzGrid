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
import com.alachisoft.tayzgrid.common.enums.AggregateFunctionType;
import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.common.datastructures.SortedMap;

public class CountPredicate extends AggregateFunctionPredicate {

    public boolean ApplyPredicate(Object o) {
        return false;
    }

    public void Execute(QueryContext queryContext, Predicate nextPredicate) throws StateTransferException, Exception {
        if (getChildPredicate() != null) {
            getChildPredicate().Execute(queryContext, nextPredicate);
        }

        queryContext.getTree().Reduce();
        java.math.BigDecimal count = new java.math.BigDecimal(queryContext.getTree().getLeftList().size());
        super.SetResult(queryContext, AggregateFunctionType.COUNT, count);
    }

    public void ExecuteInternal(QueryContext queryContext, tangible.RefObject<SortedMap> list) {
    }

    @Override
    public final AggregateFunctionType getFunctionType() {
        return AggregateFunctionType.COUNT;
    }
}
