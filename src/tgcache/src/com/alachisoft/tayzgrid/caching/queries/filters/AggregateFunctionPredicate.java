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

import com.alachisoft.tayzgrid.common.enums.AggregateFunctionType;
import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.caching.queries.QueryType;
import java.util.AbstractMap;

public class AggregateFunctionPredicate extends Predicate implements java.lang.Comparable {

    private String _attributeName;
    private Predicate _childPredicate;

    public final Predicate getChildPredicate() {
        return _childPredicate;
    }

    public final void setChildPredicate(Predicate value) {
        _childPredicate = value;
    }

    public final String getAttributeName() {
        return _attributeName;
    }

    public final void setAttributeName(String value) {
        _attributeName = value;
    }

    public boolean ApplyPredicate(Object o) {
        return false;
    }

    public final void SetResult(QueryContext queryContext, AggregateFunctionType functionType, Object result) {
        QueryResultSet resultSet = new QueryResultSet();
        resultSet.setType(QueryType.AggregateFunction);
        resultSet.setAggregateFunctionType(functionType);
        resultSet.setAggregateFunctionResult(new AbstractMap.SimpleEntry(functionType, result));
        queryContext.setResultSet(resultSet);
    }

    public final int compareTo(Object obj) {
        AggregateFunctionPredicate other = (AggregateFunctionPredicate) ((obj instanceof AggregateFunctionPredicate) ? obj : null);

        if (other != null) {
            return ((java.lang.Comparable) getChildPredicate()).compareTo(other.getChildPredicate());
        }

        return -1;
    }

    public AggregateFunctionType getFunctionType() {
        return AggregateFunctionType.NOTAPPLICABLE;
    }
}
