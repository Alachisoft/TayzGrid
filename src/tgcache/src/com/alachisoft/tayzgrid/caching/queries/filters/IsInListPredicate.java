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
import java.util.Collections;

public class IsInListPredicate extends Predicate implements java.lang.Comparable {

    private IFunctor functor;
    private java.util.ArrayList members;

    public IsInListPredicate() {
        members = new java.util.ArrayList();
    }

    public final void setFunctor(IFunctor value) {
        functor = value;
    }

    public final java.util.ArrayList getValues() {
        return members;
    }

    public final void Append(Object item) {
        Object obj = ((IGenerator) item).Evaluate();
        if (members.contains(obj)) {
            return;
        }
        members.add(obj);
        Collections.sort(members);
    }

    @Override
    public boolean ApplyPredicate(Object o) {
        Object lhs = functor.Evaluate(o);
        if (getInverse()) {
            return !members.contains(lhs);
        }
        return members.contains(lhs);
    }

    @Override
    public void ExecuteInternal(QueryContext queryContext, tangible.RefObject<SortedMap> list) throws java.lang.Exception {
        AttributeIndex index = queryContext.getIndex();
        IIndexStore store = ((MemberFunction) functor).GetStore(index);

        java.util.ArrayList keyList = new java.util.ArrayList();

        if (store != null) {
            Object tempVar = queryContext.getAttributeValues().get(((MemberFunction) functor).getMemberName());
            members = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);

            if (members == null) {
                if (queryContext.getAttributeValues().size() > 0) {
                    members = new java.util.ArrayList();
                    members.add(queryContext.getAttributeValues().get(((MemberFunction) functor).getMemberName()));
                } else {
                    throw new Exception("Value(s) not specified for indexed attribute " + ((MemberFunction) functor).getMemberName() + ".");
                }
            }

            if (!getInverse()) {
                for (int i = 0; i < members.size(); i++) {
                    java.util.ArrayList temp = store.GetData(members.get(i), ComparisonType.EQUALS);
                    if (temp != null) {
                        if (temp.size() > 0) {
                            keyList.addAll(temp);
                        }
                    }
                }
            } else {
                java.util.ArrayList temp = store.GetData(members.get(0), ComparisonType.NOT_EQUALS);
                if (temp != null) {
                    if (temp.size() > 0) {
                        for (int i = 1; i < members.size(); i++) {
                            java.util.ArrayList extras = store.GetData(members.get(i), ComparisonType.EQUALS);
                            if (extras != null) {
                                java.util.Iterator ie = extras.iterator();
                                if (ie != null) {
                                    while (ie.hasNext()) {
                                        if (temp.contains(ie.next())) {
                                            temp.remove(ie.next());
                                        }
                                    }
                                }
                            }
                        }
                        keyList.addAll(temp);
                    }
                }
            }

            if (keyList != null) {
                list.argvalue.putValue(keyList.size(), keyList);
            }
        } else {
            throw new AttributeIndexNotDefined("Index is not defined for attribute '" + ((MemberFunction) functor).getMemberName() + "'");
        }
    }

    @Override
    public void Execute(QueryContext queryContext, Predicate nextPredicate) throws Exception {
        AttributeIndex index = queryContext.getIndex();
        IIndexStore store = ((MemberFunction) functor).GetStore(index);

        if (store != null) {
            Object tempVar = queryContext.getAttributeValues().get(((MemberFunction) functor).getMemberName());
            members = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);

            if (members == null) {
                if (queryContext.getAttributeValues().size() > 0) {
                    members = new java.util.ArrayList();
                    members.add(queryContext.getAttributeValues().get(((MemberFunction) functor).getMemberName()));
                } else {
                    throw new Exception("Value(s) not specified for indexed attribute " + ((MemberFunction) functor).getMemberName() + ".");
                }
            }

            java.util.ArrayList keyList = new java.util.ArrayList();

            if (!getInverse()) {
                for (int i = 0; i < members.size(); i++) {
                    java.util.ArrayList temp = store.GetData(members.get(i), ComparisonType.EQUALS);
                    if (temp != null) {
                        if (temp.size() > 0) {
                            keyList.addAll(temp);
                        }
                    }
                }
            } else {
                java.util.ArrayList temp = store.GetData(members.get(0), ComparisonType.NOT_EQUALS);
                if (temp != null) {
                    if (temp.size() > 0) {
                        for (int i = 1; i < members.size(); i++) {
                            java.util.ArrayList extras = store.GetData(members.get(i), ComparisonType.EQUALS);
                            if (extras != null) {
                                java.util.Iterator ie = extras.iterator();
                                if (ie != null) {
                                    while (ie.hasNext()) {
                                        if (temp.contains(ie.next())) {
                                            temp.remove(ie.next());
                                        }
                                    }
                                }
                            }
                        }

                        keyList.addAll(temp);
                    }
                }
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
        } else {
            throw new AttributeIndexNotDefined("Index is not defined for attribute '" + ((MemberFunction) functor).getMemberName() + "'");
        }
    }

    @Override
    public String toString() {
        String text = getInverse() ? "is not in (" : "is in (";
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) {
                text += ", ";
            }
            text += members.get(i).toString();
        }
        text += ")";
        return text;
    }

    @Override
    public final int compareTo(Object obj) {
        if (obj instanceof IsInListPredicate) {
            IsInListPredicate other = (IsInListPredicate) obj;
            if (getInverse() == other.getInverse()) {
                if (members.size() == other.members.size()) {
                    for (int i = 0; i < members.size(); i++) {
                        if (members.get(i) != other.members.get(i)) {
                            return -1;
                        }
                    }
                    return 0; 
                }
            }
        }
        return -1;
    }
}
