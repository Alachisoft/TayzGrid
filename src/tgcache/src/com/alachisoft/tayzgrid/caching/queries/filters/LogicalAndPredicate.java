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
import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.common.datastructures.SortedMap;

public class LogicalAndPredicate extends Predicate implements java.lang.Comparable {

    private java.util.ArrayList members;

    public LogicalAndPredicate() {
        members = new java.util.ArrayList();
    }

    public final java.util.ArrayList getChildren() {
        return members;
    }

    public void Invert() {
        super.Invert();
        for (int i = 0; i < members.size(); i++) {
            ((Predicate) members.get(i)).Invert();
        }
    }

    @Override
    public boolean ApplyPredicate(Object o) {
        for (int i = 0; i < members.size(); i++) {
            if (((Predicate) members.get(i)).Evaluate(o) == getInverse()) {
                return getInverse();
            }
        }
        return !getInverse();
    }

    @Override
    public void ExecuteInternal(QueryContext queryContext, tangible.RefObject<SortedMap> list) throws Exception {
        boolean sortAscending = true;

        java.util.ArrayList keys = new java.util.ArrayList();

        if (getInverse()) {
            sortAscending = false;
        }
        SortedMap tmpList = new SortedMap(new QueryResultComparer(sortAscending));

        for (int i = 0; i < members.size(); i++) {
            Predicate predicate = (Predicate) members.get(i);
            tangible.RefObject<SortedMap> tempRef_tmpList = new tangible.RefObject<SortedMap>(tmpList);
            predicate.ExecuteInternal(queryContext, tempRef_tmpList);
            tmpList = tempRef_tmpList.argvalue;
        }

        if (getInverse()) {
            keys = GetUnion(tmpList);
        } else {
            keys = GetIntersection(tmpList);
        }

        if (keys != null) {
            list.argvalue.putValue(keys.size(), keys);
        }
    }

    @Override
    public void Execute(QueryContext queryContext, Predicate nextPredicate) throws StateTransferException, Exception {
        boolean sortAscending = true;
        boolean normalizePredicates = true;

        if (getInverse()) {
            sortAscending = false;
        }
        SortedMap list = new SortedMap(new QueryResultComparer(sortAscending));

        for (int i = 0; i < members.size(); i++) {
            Predicate predicate = (Predicate) members.get(i);
            boolean isOfTypePredicate = predicate instanceof IsOfTypePredicate;

            if (isOfTypePredicate) {
                predicate.Execute(queryContext, (Predicate) members.get(++i));
                normalizePredicates = false;
            } else {
                //: IN-1009	:	Changed SortedList to ArrayList
                tangible.RefObject<SortedMap> tempRef_list = new tangible.RefObject<SortedMap>(list);
                predicate.ExecuteInternal(queryContext, tempRef_list);
                list = tempRef_list.argvalue;
            }
        }

        if (normalizePredicates) {
            if (getInverse()) {
                queryContext.getTree().setRightList(GetUnion(list));
            } else {
                queryContext.getTree().setRightList(GetIntersection(list));
            }
        }
    }

    /**
     * handles case for 'OR' condition [Inverse == true]
     *
     * @param list
     * @return
     */
    private java.util.ArrayList GetUnion(SortedMap list) {
        java.util.HashMap finalTable = new java.util.HashMap();

        if (list.size() > 0) {
            Object tempVar = list.getByIndex(0);
            java.util.ArrayList finalKeys = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
            for (int i = 0; i < finalKeys.size(); i++) {
                finalTable.put(finalKeys.get(i), null);
            }

            for (int i = 1; i < list.size(); i++) {
                Object tempVar2 = list.getByIndex(i);
                java.util.ArrayList keys = (java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null);

                if (keys != null && keys.size() > 0) {
                    for (int j = 0; j < keys.size(); j++) {
                        finalTable.put(keys.get(j), null);
                    }
                }
            }
        }

        return new java.util.ArrayList(finalTable.keySet());
    }

    /**
     * handles the case for 'AND' condition
     *
     * @param list
     * @return
     */
    private java.util.ArrayList GetIntersection(SortedMap list) {
        java.util.HashMap finalTable = new java.util.HashMap();

        if (list.size() > 0) {
            Object tempVar = list.getByIndex(0);
            java.util.ArrayList keys = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);

            for (int i = 0; i < keys.size(); i++) {
                finalTable.put(keys.get(i), null);
            }

            for (int i = 1; i < list.size(); i++) {
                java.util.HashMap shiftTable = new java.util.HashMap();
                Object tempVar2 = list.getByIndex(i);
                keys = (java.util.ArrayList) ((tempVar2 instanceof java.util.ArrayList) ? tempVar2 : null);

                if (keys != null) {
                    for (int j = 0; j < keys.size(); j++) {
                        Object key = keys.get(j);

                        if (finalTable.containsKey(key)) {
                            shiftTable.put(key, null);
                        }
                    }
                }
                finalTable = shiftTable;
            }
        }

        return new java.util.ArrayList(finalTable.keySet());
    }

    @Override
    public String toString() {
        String text = getInverse() ? "(" : "(";
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) {
                text += getInverse() ? " or " : " and ";
            }
            text += members.get(i).toString();
        }
        text += ")";
        return text;
    }

    @Override
    public final int compareTo(Object obj) {
        if (obj instanceof LogicalAndPredicate) {
            LogicalAndPredicate other = (LogicalAndPredicate) obj;
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