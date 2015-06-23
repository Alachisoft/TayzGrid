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

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.caching.exceptions.StateTransferException;
import com.alachisoft.tayzgrid.common.enums.AggregateFunctionType;
import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.common.datastructures.SortedMap;
import java.math.BigDecimal;
import java.util.Iterator;

public class MaxPredicate extends AggregateFunctionPredicate {

    @Override
    public boolean ApplyPredicate(Object o) {
        return false;
    }

    @Override
    public void Execute(QueryContext queryContext, Predicate nextPredicate) throws StateTransferException, Exception {
        if (getChildPredicate() != null) {
            getChildPredicate().Execute(queryContext, nextPredicate);
        }

        queryContext.getTree().Reduce();
        CacheEntry entry = null;

        java.lang.Comparable max = null;
        boolean initialized = false;
        java.lang.Class type = null;
        for (Iterator it = queryContext.getTree().getLeftList().iterator(); it.hasNext();) {
            Object key = it.next();
            java.lang.Comparable current = (java.lang.Comparable) queryContext.getIndex().GetAttributeValue(key, getAttributeName());

            if (current != null) {
                type = current.getClass();
                if (type == Boolean.class) {
                    throw new Exception("MAX cannot be applied to Boolean data type.");
                }
                if (!initialized) {
                    max = current;
                    initialized = true;
                }
                if (current.compareTo(max) > 0) {
                    max = current;
                }
            }
        }

        if (type != null) {
            if ((type != java.util.Date.class) && (type != String.class) && (type != Character.class)) {
                if (max != null) {
                    super.SetResult(queryContext, AggregateFunctionType.MAX, new BigDecimal(((Number) max).doubleValue()));
                    return;
                }
            }
        }
        super.SetResult(queryContext, AggregateFunctionType.MAX, max);
    }

    @Override
    public void ExecuteInternal(QueryContext queryContext, tangible.RefObject<SortedMap> list) {
    }

    @Override
    public final AggregateFunctionType getFunctionType() {
        return AggregateFunctionType.MAX;
    }
}
