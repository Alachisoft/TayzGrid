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

package com.alachisoft.tayzgrid.caching.queries;

import com.alachisoft.tayzgrid.caching.CacheEntry;
import com.alachisoft.tayzgrid.common.datastructures.RecordSet;
import com.alachisoft.tayzgrid.common.queries.AverageResult;
import com.alachisoft.tayzgrid.common.enums.AggregateFunctionType;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class QueryResultSet implements ICompactSerializable
{

    private java.util.ArrayList _searchKeysResult;
    private java.util.HashMap _searchEntriesResult;
    private Map.Entry _aggregateFunctionResult;
    private boolean _isInitialized = false;
    private QueryType _queryType = QueryType.SearchKeys;
    private AggregateFunctionType _aggregateFunctionType = AggregateFunctionType.NOTAPPLICABLE;
    private String _cqId;
    private ArrayList _keysForUpdateIndices;
    private RecordSet _groupByResult;
    
    public RecordSet getGroupByResult()
    {
        return _groupByResult;
    }
    
    public void setGroupByResult(RecordSet value)
    {
        _groupByResult=value;
    }
    
    public ArrayList getUpdateIndicesKeys()
    {
        return _keysForUpdateIndices;
    }

    public void setUpdateIndicesKeys(ArrayList value)
    {
        _keysForUpdateIndices = value;
    }

    public final String getCQUniqueId()
    {
        return _cqId;
    }

    public final void setCQUniqueId(String value)
    {
        _cqId = value;
    }

    public final boolean getIsInitialized()
    {
        return _isInitialized;
    }

    public final QueryType getType()
    {
        return _queryType;
    }

    public final void setType(QueryType value)
    {
        _queryType = value;
    }

    public final AggregateFunctionType getAggregateFunctionType()
    {
        return _aggregateFunctionType;
    }

    public final void setAggregateFunctionType(AggregateFunctionType value)
    {
        _aggregateFunctionType = value;
    }

    public final java.util.ArrayList getSearchKeysResult()
    {
        return _searchKeysResult;
    }

    public final void setSearchKeysResult(java.util.ArrayList value)
    {
        _searchKeysResult = value;
    }

    public final java.util.HashMap getSearchEntriesResult()
    {
        return _searchEntriesResult;
    }

    public final void setSearchEntriesResult(java.util.HashMap value)
    {
        _searchEntriesResult = value;
    }

    public final Map.Entry getAggregateFunctionResult()
    {
        return _aggregateFunctionResult;
    }

    public final void setAggregateFunctionResult(Map.Entry value)
    {
        _aggregateFunctionResult = value;
    }

    public final void Initialize(QueryResultSet resultSet)
    {
        if (!_isInitialized)
        {
            this.setType(resultSet.getType());
            this.setAggregateFunctionType(resultSet.getAggregateFunctionType());
            this.setAggregateFunctionResult(resultSet.getAggregateFunctionResult());
            this.setSearchKeysResult(resultSet.getSearchKeysResult());
            this.setSearchEntriesResult(resultSet.getSearchEntriesResult());
            this._isInitialized = true;
        }
    }

    public final void Compile(QueryResultSet resultSet)
    {
        if (!this._isInitialized)
        {
            Initialize(resultSet);
            return;
        }

        switch (this.getType())
        {
            case AggregateFunction:

                switch ((AggregateFunctionType) this.getAggregateFunctionResult().getKey())
                {
                    case SUM:
                        java.math.BigDecimal a = new java.math.BigDecimal(0);
                        java.math.BigDecimal b = new java.math.BigDecimal(0);

                        Object thisVal = this.getAggregateFunctionResult().getValue();
                        Object otherVal = resultSet.getAggregateFunctionResult().getValue();

                        java.math.BigDecimal sum = null;

                        if (thisVal == null && otherVal != null)
                        {
                            sum = (java.math.BigDecimal) otherVal;
                        }
                        else if (thisVal != null && otherVal == null)
                        {
                            sum = (java.math.BigDecimal) thisVal;
                        }
                        else if (thisVal != null && otherVal != null)
                        {
                            a = (java.math.BigDecimal) thisVal;
                            b = (java.math.BigDecimal) otherVal;
                            sum = a.add(b);
                        }

                        if (sum != null)
                        {
                            this.setAggregateFunctionResult(new AbstractMap.SimpleEntry(AggregateFunctionType.SUM, sum));
                        }
                        else
                        {
                            this.setAggregateFunctionResult(new AbstractMap.SimpleEntry(AggregateFunctionType.SUM, null));
                        }
                        break;

                    case COUNT:
                        a = (java.math.BigDecimal) this.getAggregateFunctionResult().getValue();
                        b = (java.math.BigDecimal) resultSet.getAggregateFunctionResult().getValue();
                        java.math.BigDecimal count = a.add(b);

                        this.setAggregateFunctionResult(new AbstractMap.SimpleEntry(AggregateFunctionType.COUNT, count));
                        break;

                    case MIN:
                        java.lang.Comparable thisValue = (java.lang.Comparable) this.getAggregateFunctionResult().getValue();
                        java.lang.Comparable otherValue = (java.lang.Comparable) resultSet.getAggregateFunctionResult().getValue();
                        java.lang.Comparable min = thisValue;

                        if (thisValue == null && otherValue != null)
                        {
                            min = otherValue;
                        }
                        else if (thisValue != null && otherValue == null)
                        {
                            min = thisValue;
                        }
                        else if (thisValue == null && otherValue == null)
                        {
                            min = null;
                        }
                        else if (otherValue.compareTo(thisValue) < 0)
                        {
                            min = otherValue;
                        }

                        this.setAggregateFunctionResult(new AbstractMap.SimpleEntry(AggregateFunctionType.MIN, min));
                        break;

                    case MAX:
                        thisValue = (java.lang.Comparable) this.getAggregateFunctionResult().getValue();
                        otherValue = (java.lang.Comparable) resultSet.getAggregateFunctionResult().getValue();
                        java.lang.Comparable max = thisValue;

                        if (thisValue == null && otherValue != null)
                        {
                            max = otherValue;
                        }
                        else if (thisValue != null && otherValue == null)
                        {
                            max = thisValue;
                        }
                        else if (thisValue == null && otherValue == null)
                        {
                            max = null;
                        }
                        else if (otherValue.compareTo(thisValue) > 0)
                        {
                            max = otherValue;
                        }

                        this.setAggregateFunctionResult(new AbstractMap.SimpleEntry(AggregateFunctionType.MAX, max));
                        break;

                    case AVG:
                        thisVal = this.getAggregateFunctionResult().getValue();
                        otherVal = resultSet.getAggregateFunctionResult().getValue();

                        AverageResult avg = null;
                        if (thisVal == null && otherVal != null)
                        {
                            avg = (AverageResult) otherVal;
                        }
                        else if (thisVal != null && otherVal == null)
                        {
                            avg = (AverageResult) thisVal;
                        }
                        else if (thisVal != null && otherVal != null)
                        {
                            AverageResult thisResult = (AverageResult) thisVal;
                            AverageResult otherResult = (AverageResult) otherVal;

                            avg = new AverageResult();
                            avg.setSum(thisResult.getSum().add(otherResult.getSum()));
                            avg.setCount(thisResult.getCount().add(otherResult.getCount()));
                        }

                        if (avg != null)
                        {
                            this.setAggregateFunctionResult(new AbstractMap.SimpleEntry(AggregateFunctionType.AVG, avg));
                        }
                        else
                        {
                            this.setAggregateFunctionResult(new AbstractMap.SimpleEntry(AggregateFunctionType.AVG, null));
                        }
                        break;
                }

                break;

            case SearchKeys:
                if (this.getSearchKeysResult() == null)
                {
                    this.setSearchKeysResult(resultSet.getSearchKeysResult());
                }
                else
                {
                    if(resultSet.getSearchKeysResult() !=null)
                        this.getSearchKeysResult().addAll(resultSet.getSearchKeysResult());
                }

                break;

            case SearchEntries:
                if (this.getSearchEntriesResult() == null)
                {
                    this.setSearchEntriesResult(resultSet.getSearchEntriesResult());
                }
                else
                {
                    Iterator ide = resultSet.getSearchEntriesResult().entrySet().iterator();
                    while (ide.hasNext())
                    {
                        Map.Entry current = (Map.Entry) ide.next();
                        try
                        {
                            this.getSearchEntriesResult().put(current.getKey(), current.getValue());
                        }
                        catch (IllegalArgumentException ex) //Overwrite entry with an updated one
                        {

                            CacheEntry entry = (CacheEntry) ((current.getValue() instanceof CacheEntry) ? current.getValue() : null);
                            Object tempVar = this.getSearchEntriesResult().get(current.getKey());
                            CacheEntry existingEntry = (CacheEntry) ((tempVar instanceof CacheEntry) ? tempVar : null);
                            if (entry != null && existingEntry != null)
                            {
                                if (entry.getVersion() > existingEntry.getVersion())
                                {
                                    this.getSearchEntriesResult().put(current.getKey(), entry);
                                }
                            }
                        }
                    }
                }

                break;
        }
    }

    public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        _aggregateFunctionResult = (Map.Entry) reader.readObject();
        Object tempVar = reader.readObject();
        _searchKeysResult = (java.util.ArrayList) ((tempVar instanceof java.util.ArrayList) ? tempVar : null);
        Object tempVar2 = reader.readObject();
        _searchEntriesResult = (java.util.HashMap) ((tempVar2 instanceof java.util.HashMap) ? tempVar2 : null);
        _queryType = QueryType.forValue(reader.readInt());
        _aggregateFunctionType = AggregateFunctionType.forValue(reader.readInt());
        _cqId = reader.readUTF();
        _groupByResult=(RecordSet)reader.readObject();
    }

    public void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeObject(_aggregateFunctionResult);
        writer.writeObject(_searchKeysResult);
        writer.writeObject(_searchEntriesResult);
        writer.writeInt(_queryType.getValue());
        writer.writeInt(_aggregateFunctionType.getValue());
        writer.writeUTF(getCQUniqueId());
        writer.writeObject(_groupByResult);
    }
}
