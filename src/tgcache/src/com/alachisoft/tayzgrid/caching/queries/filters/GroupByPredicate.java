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
import com.alachisoft.tayzgrid.caching.queries.GroupByValueList;
import com.alachisoft.tayzgrid.caching.queries.QueryContext;
import com.alachisoft.tayzgrid.caching.queries.QueryType;
import com.alachisoft.tayzgrid.common.datastructures.ColumnDataType;
import com.alachisoft.tayzgrid.common.datastructures.ColumnType;
import com.alachisoft.tayzgrid.common.datastructures.KeyValuesContainer;
import com.alachisoft.tayzgrid.common.datastructures.MultiRootTree;
import com.alachisoft.tayzgrid.common.datastructures.RecordSet;
import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import java.util.ArrayList;

public class GroupByPredicate extends Predicate implements java.lang.Comparable {

    private ArrayList<String> _attributeNames;
    private Predicate _childPredicate;

    private GroupByValueList _groupByValueList;

    public GroupByPredicate() {
        _attributeNames = new ArrayList<String>();
    }

    public Predicate getChildPredicate() {
        return _childPredicate;
    }

    public void setChildPredicate(Predicate value) {
        _childPredicate = value;
    }

    public GroupByValueList getGroupByValueList() {
        return _groupByValueList;
    }

    public void setGroupByValueList(GroupByValueList value) {
        _groupByValueList = value;
    }

    @Override
    public boolean ApplyPredicate(Object o) {
        return false;
    }

    public ArrayList<String> getAttributeNamesList() {
        return _attributeNames;
    }

    @Override
    public void Execute(QueryContext queryContext, Predicate nextPredicate) throws StateTransferException, Exception {
        getChildPredicate().Execute(queryContext, nextPredicate);
        queryContext.getTree().Reduce();

        MultiRootTree groupTree = new MultiRootTree(_attributeNames.size(), _attributeNames);
        KeyValuesContainer keyValues = new KeyValuesContainer();
        if (!queryContext.getTree().getLeftList().isEmpty()) {
            for (Object key : queryContext.getTree().getLeftList()) {
                keyValues.setKey(key);
                boolean invalidGroupKey = false;
                for (int i = 0; i < _attributeNames.size(); i++) {
                    Object attributeValue = queryContext.getIndex().GetAttributeValue(key, _attributeNames.get(i));
                    if (attributeValue == null) {
                        invalidGroupKey = true;
                        break;
                    }
                    keyValues.getValues().put(_attributeNames.get(i), attributeValue);
                }
                if (!invalidGroupKey) {
                    groupTree.Add(keyValues);
                }
            }
        }

        RecordSet resultRecordSet = new RecordSet();
        for (String columnName : _groupByValueList.getObjectAttributesList()) {
            resultRecordSet.AddColumn(columnName, false);
        }

        for (AggregateFunctionPredicate afp : _groupByValueList.getAggregateFunctionsList()) {
            String columnName = null;
            if (afp.getAttributeName() != null) {
                columnName = afp.getFunctionType().toString() + "(" + afp.getAttributeName() + ")";
            } else {
                columnName = afp.getFunctionType().toString() + "()";
            }

            if (!resultRecordSet.AddColumn(columnName, false, ColumnType.AggregateResultColumn, afp.getFunctionType())) {
                throw new ArgumentException("Invalid query. Same value cannot be selected twice.");
            }
            afp.setChildPredicate(null);
        }

        //add remaining attributes in Group By clause as hidden columns
        for (String attribute : _attributeNames) {
            resultRecordSet.AddColumn(attribute, true);
        }

        //generates RecordSet from tree. Last columns (extra added by this function) of RecordSet contains keys in group as ArrayList
        groupTree.ToRecordSet(resultRecordSet);

        for (int rowID = 0; rowID < resultRecordSet.getRowCount(); rowID++) {
            ArrayList keysList = (ArrayList) resultRecordSet.GetObject(rowID, resultRecordSet.getColumnCount() - 1);
            int j = 0;
            for (AggregateFunctionPredicate afp : this._groupByValueList.getAggregateFunctionsList()) {
                queryContext.getTree().getRightList().addAll(keysList);
                afp.Execute(queryContext, null);
                int columnId = _groupByValueList.getObjectAttributesList().size() + j++;
                if (resultRecordSet.GetColumnDataType(columnId) == ColumnDataType.Object) {
                    resultRecordSet.SetColumnDataType(columnId, RecordSet.ToColumnDataType(queryContext.getResultSet().getAggregateFunctionResult().getValue()));
                }

                resultRecordSet.Add(queryContext.getResultSet().getAggregateFunctionResult().getValue(), rowID, columnId);
            }
        }
        resultRecordSet.RemoveLastColumn();

        queryContext.getResultSet().setGroupByResult(resultRecordSet);
        queryContext.getResultSet().setType(QueryType.GroupByAggregateFunction);
    }

    @Override
    public int compareTo(Object o) {
        GroupByPredicate other = (GroupByPredicate) (o instanceof GroupByPredicate ? o : null);
        if (other != null) {
            return ((java.lang.Comparable) this.getChildPredicate()).compareTo(o);
        }
        return -1;
    }
}