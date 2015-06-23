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
import com.alachisoft.tayzgrid.common.queries.AverageResult;
import java.math.BigDecimal;
import java.util.Iterator;

public class AveragePredicate extends AggregateFunctionPredicate {

    public boolean ApplyPredicate(Object o) {
        return false;
    }

    public void Execute(QueryContext queryContext, Predicate nextPredicate) throws StateTransferException, java.lang.Exception {
        if (getChildPredicate() != null) {
            getChildPredicate().Execute(queryContext, nextPredicate);
        }

        queryContext.getTree().Reduce();
        CacheEntry entry = null;

        java.math.BigDecimal sum = new java.math.BigDecimal(0);

        if (queryContext.getTree().getLeftList().size() > 0) {
            for (Iterator it = queryContext.getTree().getLeftList().iterator(); it.hasNext();) {
                Object key = it.next();
                Object attribValue = queryContext.getIndex().GetAttributeValue(key, getAttributeName());
                if (attribValue != null) {
                    if ((attribValue instanceof Boolean) || (attribValue instanceof java.util.Date) || (attribValue instanceof String) || (attribValue instanceof Character)) {
                        throw new Exception("AVG can only be applied to integral data types.");
                    }

                    sum = sum.add(new BigDecimal(((Number) attribValue).doubleValue()));
                }
            }

            AverageResult avgResult = new AverageResult();
            avgResult.setSum(sum);
            avgResult.setCount(new java.math.BigDecimal(queryContext.getTree().getLeftList().size()));
            //put the count and the sum
            super.SetResult(queryContext, AggregateFunctionType.AVG, avgResult);
        } else {
            super.SetResult(queryContext, AggregateFunctionType.AVG, null);
        }
    }

    public void ExecuteInternal(QueryContext queryContext, tangible.RefObject<SortedMap> list) {
    }

    @Override
    public final AggregateFunctionType getFunctionType() {
        return AggregateFunctionType.AVG;
    }
}
