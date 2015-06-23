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



package com.alachisoft.tayzgrid.web.caching.queries;

import com.alachisoft.tayzgrid.util.DictionaryEntry;
import com.alachisoft.tayzgrid.runtime.queries.AverageResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;


public class QueryResultSet {

     private Collection searchKeysResult = new ArrayList();
     private HashMap searchEntriesResult;
     private DictionaryEntry<Object, Object> aggregateFunctionResult;
     private QueryType queryType = QueryType.SearchKeys;
     private AggregateFunctionType aggregateFunctionType = AggregateFunctionType.NOTAPPLICABLE;
     private String CQUniqueId;
     private boolean isInitialized;

    public boolean isIsInitialized() {
        return isInitialized;
    }

    public String getCQUniqueId() {
        return CQUniqueId;
    }

    public void setCQUniqueId(String CQUniqueId) {
        this.CQUniqueId = CQUniqueId;
    }

    public QueryType getQueryType()
    {
        return queryType;
    }

    public void setQueryType(QueryType queryType)
    {
        this.queryType = queryType;
    }

    public void setQueryType(int queryType)
    {
        switch(queryType)
        {
            case 0:
                this.queryType = QueryType.SearchKeys;
            break;
            case 1:
                this.queryType = QueryType.SearchEntries;
            break;
            case 2:
                this.queryType = QueryType.AggregateFunction;
            break;
        }
    }

    public AggregateFunctionType getAggregateFunctionType()
    {
        return aggregateFunctionType;
    }

    public void setAggregateFunctionType(AggregateFunctionType aggregateFunctionType)
    {
        this.aggregateFunctionType = aggregateFunctionType;
    }

    public void setAggregateFunctionType(int aggregateFunctionType)
    {
         switch(aggregateFunctionType)
        {
            case 0:
                this.aggregateFunctionType = AggregateFunctionType.SUM;
            break;
            case 1:
                this.aggregateFunctionType = AggregateFunctionType.COUNT;
            break;
            case 2:
                this.aggregateFunctionType = AggregateFunctionType.MIN;
            break;
            case 3:
                this.aggregateFunctionType = AggregateFunctionType.MAX;
            break;
            case 4:
                this.aggregateFunctionType = AggregateFunctionType.AVG;
            break;
            case 5:
                this.aggregateFunctionType = AggregateFunctionType.NOTAPPLICABLE;
            break;
        }

    }

    public DictionaryEntry<Object, Object> getAggregateFunctionResult()
    {
        return aggregateFunctionResult;
    }

    public void setAggregateFunctionResult(DictionaryEntry<Object, Object> aggregateFunctionResult)
    {
        this.aggregateFunctionResult = aggregateFunctionResult;
    }

    public Collection getSearchKeysResult()
    {
        return searchKeysResult;
    }

    public void setSearchKeysResult(Collection searchKeysResult)
    {
        this.searchKeysResult = searchKeysResult;
    }

    public HashMap getSearchEntriesResult()
    {
        return searchEntriesResult;
    }

    public void setSearchEntriesResult(HashMap searchEnteriesResult)
    {
        this.searchEntriesResult = searchEnteriesResult;
    }

    public void Compile(QueryResultSet resultSet)
    {
        if (!this.isInitialized)
        {
            Initialize(resultSet);
            return;
        }

        switch (this.getQueryType())
        {
            case AggregateFunction:

                AggregateFunctionType type = AggregateFunctionType.valueOf(this.getAggregateFunctionResult().getKey().toString());
                switch (type)
                {
                    case SUM:
                        BigDecimal a;
                        BigDecimal b;

                        Object thisVal = this.getAggregateFunctionResult().getValue();
                        Object otherVal = resultSet.getAggregateFunctionResult().getValue();

                        BigDecimal sum = null;

                        if (thisVal == null && otherVal != null)
                        {
                            sum = (BigDecimal)otherVal;
                        }
                        else if (thisVal != null && otherVal == null)
                        {
                            sum = (BigDecimal)thisVal;
                        }
                        else if (thisVal != null && otherVal != null)
                        {
                            a = (BigDecimal)thisVal;
                            b = (BigDecimal)otherVal;
                            sum = a.add(b);
                        }

                        if (sum != null)
                        {
                            this.setAggregateFunctionResult(new DictionaryEntry(AggregateFunctionType.SUM, sum));
                        }
                        else
                        {
                            this.setAggregateFunctionResult(new DictionaryEntry(AggregateFunctionType.SUM, null));
                        }
                        break;

                    case COUNT:
                        a = (BigDecimal)this.getAggregateFunctionResult().getValue();
                        b = (BigDecimal)resultSet.getAggregateFunctionResult().getValue();
                        BigDecimal count = a.add(b);

                        this.setAggregateFunctionResult(new DictionaryEntry(AggregateFunctionType.COUNT, count));
                        break;

                    case MIN:
                        Comparable thisValue = (Comparable)this.getAggregateFunctionResult().getValue();
                        Comparable otherValue = (Comparable)resultSet.getAggregateFunctionResult().getValue();
                        Comparable min = thisValue;

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

                        this.setAggregateFunctionResult(new DictionaryEntry(AggregateFunctionType.MIN, min));
                        break;

                    case MAX:
                        thisValue = (Comparable)this.getAggregateFunctionResult().getValue();
                        otherValue = (Comparable)resultSet.getAggregateFunctionResult().getValue();
                        Comparable max = thisValue;

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

                        this.setAggregateFunctionResult(new DictionaryEntry(AggregateFunctionType.MAX, max));
                        break;

                    case AVG:
                        thisVal = this.getAggregateFunctionResult().getValue();
                        otherVal = resultSet.getAggregateFunctionResult().getValue();

                        AverageResult avg = null;
                        if (thisVal == null && otherVal != null)
                        {
                            avg = (AverageResult)otherVal;
                        }
                        else if (thisVal != null && otherVal == null)
                        {
                            avg = (AverageResult)thisVal;
                        }
                        else if (thisVal != null && otherVal != null)
                        {
                            AverageResult thisResult = (AverageResult)thisVal;
                            AverageResult otherResult = (AverageResult)otherVal;

                            avg = new AverageResult();
                            avg.setSum(thisResult.getSum().add(otherResult.getSum()));
                            avg.setCount(thisResult.getCount().add(otherResult.getCount()));
                        }

                        if (avg != null)
                        {
                            this.setAggregateFunctionResult(new DictionaryEntry(AggregateFunctionType.AVG, avg));
                        }
                        else
                        {
                            this.setAggregateFunctionResult(new DictionaryEntry(AggregateFunctionType.AVG, null));
                        }
                        break;
                }

                break;

            case SearchKeys:
                if (this.getSearchKeysResult() == null)
                    this.setSearchKeysResult(resultSet.getSearchKeysResult());
                else
                    this.getSearchKeysResult().addAll(resultSet.getSearchKeysResult());

                break;

            case SearchEntries:
                if (this.getSearchEntriesResult() == null)
                    this.setSearchEntriesResult(resultSet.getSearchEntriesResult());
                else
                {
                   this.getSearchEntriesResult().putAll(resultSet.getSearchEntriesResult());
                }

                break;
        }
    }

    public void Initialize(QueryResultSet resultSet)
    {
        if (!isInitialized)
        {
            this.setQueryType(resultSet.getQueryType());
            this.setAggregateFunctionType(resultSet.getAggregateFunctionType());
            this.setAggregateFunctionResult(resultSet.getAggregateFunctionResult());
            this.setSearchKeysResult(resultSet.getSearchKeysResult());
            this.setSearchEntriesResult(resultSet.getSearchEntriesResult());
            this.isInitialized = true;
        }
    }


}
