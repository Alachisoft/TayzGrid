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
import com.alachisoft.tayzgrid.caching.queries.ComparisonType;
import com.alachisoft.tayzgrid.caching.queries.IIndexStore;
import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.common.datastructures.SortedMap;
import com.alachisoft.tayzgrid.parser.AttributeIndexNotDefined;
import com.alachisoft.tayzgrid.runtime.exceptions.CacheException;
import com.alachisoft.tayzgrid.runtime.exceptions.GeneralFailureException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;

public class FunctorGreaterGeneratorPredicate extends Predicate implements java.lang.Comparable {

    private IFunctor functor;
    private IGenerator generator;

    public FunctorGreaterGeneratorPredicate(IFunctor lhs, IGenerator rhs) {
        functor = lhs;
        generator = rhs;
    }

    @Override
    public boolean ApplyPredicate(Object o) {
        Comparable lhs = (Comparable) functor.Evaluate(o);
        if (getInverse()) {
            return lhs.compareTo(generator.Evaluate()) <= 0;
        }
        return lhs.compareTo(generator.Evaluate()) > 0;
    }

    @Override
    public void ExecuteInternal(QueryContext queryContext, tangible.RefObject<SortedMap> list) throws GeneralFailureException, OperationFailedException, CacheException {
        AttributeIndex index = queryContext.getIndex();
        IIndexStore store = ((MemberFunction) functor).GetStore(index);

        if (store != null) {
            java.util.ArrayList keyList = null;

            if (getInverse()) {
                keyList = store.GetData(generator.Evaluate(((MemberFunction) functor).getMemberName(), queryContext.getAttributeValues()), ComparisonType.LESS_THAN_EQUALS);
            } else {
                keyList = store.GetData(generator.Evaluate(((MemberFunction) functor).getMemberName(), queryContext.getAttributeValues()), ComparisonType.GREATER_THAN);
            }

            if (keyList != null) {
                list.argvalue.putValue(keyList.size(), keyList);
            }
        } else {
            {
                throw new AttributeIndexNotDefined("Index is not defined for attribute '" + ((MemberFunction) functor).getMemberName() + "'");
            }
        }
    }

    @Override
    public void Execute(QueryContext queryContext, Predicate nextPredicate) throws GeneralFailureException, OperationFailedException, CacheException {
        AttributeIndex index = queryContext.getIndex();
        IIndexStore store = ((MemberFunction) functor).GetStore(index);

        if (store != null) {
            java.util.ArrayList keyList = null;

            if (getInverse()) {
                keyList = store.GetData(generator.Evaluate(((MemberFunction) functor).getMemberName(), queryContext.getAttributeValues()), ComparisonType.LESS_THAN_EQUALS);
            } else {
                keyList = store.GetData(generator.Evaluate(((MemberFunction) functor).getMemberName(), queryContext.getAttributeValues()), ComparisonType.GREATER_THAN);
            }

            if (keyList != null && keyList.size() > 0) {
                java.util.Iterator keyListEnum = keyList.iterator();

                if (queryContext.getPopulateTree()) {
                    queryContext.getTree().setRightList(keyList);

                    queryContext.setPopulateTree(false);
                } else {
                    while (keyListEnum.hasNext()) {
                        if (queryContext.getTree().getLeftList().contains(keyListEnum.next())) {
                            queryContext.getTree().Shift(keyListEnum.next());
                        }
                    }
                }
            }
        } else { //straight-forward. no index.
                throw new AttributeIndexNotDefined("Index is not defined for attribute '" + ((MemberFunction) functor).getMemberName() + "'");
        }
    }

    @Override
    public String toString() {
        return functor + (getInverse() ? " <= " : " > ") + generator;
    }

    @Override
    public final int compareTo(Object obj) {
        if (obj instanceof FunctorGreaterGeneratorPredicate) {
            FunctorGreaterGeneratorPredicate other = (FunctorGreaterGeneratorPredicate) obj;
            if (getInverse() == other.getInverse()) {
                return ((java.lang.Comparable) functor).compareTo(other.functor) == 0 && ((java.lang.Comparable) generator).compareTo(other.generator) == 0 ? 0 : -1;
            }
        }
        return -1;
    }
}
