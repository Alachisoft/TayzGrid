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
import com.alachisoft.tayzgrid.caching.queries.QueryIndexManager;
import com.alachisoft.tayzgrid.parser.ParserException;
import com.alachisoft.tayzgrid.parser.TypeIndexNotDefined;

public class IsOfTypePredicate extends Predicate implements java.lang.Comparable {

    private String typename;

    public IsOfTypePredicate(String name) {
        typename = name;
    }

    public final String getTypeName() {
        return typename;
    }

    public final void setTypeName(String value) {
        typename = value;
    }

    @Override
    public boolean ApplyPredicate(Object o) {
        if (typename.equals("*")) {
            throw new ParserException("Incorrect query format \'*\'.");
        }
        return typename.equals(o.getClass().getName());
    }

    @Override
    public void Execute(QueryContext queryContext, Predicate nextPredicate) throws StateTransferException, Exception {
        if (typename.equals("*")) {
            throw new ParserException("Incorrect query format. \'*\' is not supported.");
        }

        if (queryContext.getIndexManager() == null) {
            throw new TypeIndexNotDefined("Index is not defined for '" + typename.toString() + "'");
        }

        queryContext.setTypeName(typename);

        if (queryContext.getIndex() == null) { //try to get virtual index
            //we are not allowing queries
            //with out attribute-level indexes right now.

            if (queryContext.getAttributeValues() != null && queryContext.getAttributeValues().size() == 1) {
                //In case, user is querying for tag only, then we should not throw
                //exception.
                if (queryContext.getAttributeValues().containsKey("$Tag$")) {
                    queryContext.setIndex(new AttributeIndex(null, queryContext.getCache().getContext().getCacheRoot().getName(), null));
                    return;
                }
            }
            //in case of DisableException is true, exception will not be thrown, and return new attribute index.
            if (QueryIndexManager.getDisableException()) {
                queryContext.setIndex(new AttributeIndex(null, queryContext.getCache().getContext().getCacheRoot().getName(), null));
                return;
            }
            throw new TypeIndexNotDefined("Index is not defined for '" + typename.toString() + "'");
        } else {
            //populate the tree for normal queries and for tag queries...
            if (nextPredicate == null && queryContext.getPopulateTree()) {
                queryContext.getTree().Populate(queryContext.getIndex().GetEnumerator(typename, false));
                queryContext.getTree().Populate(queryContext.getIndex().GetEnumerator(typename, true));
            } else {
                nextPredicate.Execute(queryContext, null);
            }
        }
    }

    @Override
    public String toString() {
        return "typeof(Value)" + (getInverse() ? " != " : " == ") + typename;
    }

    @Override
    public final int compareTo(Object obj) {
        if (obj instanceof IsOfTypePredicate) {
            IsOfTypePredicate other = (IsOfTypePredicate) obj;
            if (getInverse() == other.getInverse()) {
                return typename.compareTo(other.typename);
            }
        }
        return -1;
    }
}
