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
package com.alachisoft.tayzgrid.common.caching.statistics.customcounters;

/**
 *
 * AverageCounter is an instantaneous counter used for calculating average operations performed in one second. For e.g Average data sent
 */
public class AverageCounter extends InstantaneousCounter
{
    private double _sum = 0;
    private long _totalCount=0;

    public AverageCounter(String name, String instance)
    {
        super(name, instance);
    }

    public AverageCounter(String category, String name, String instance)
    {
        super(category, name, instance);
    }

    @Override
    protected void calculate(double value)
    {
        _sum+=value;
    }
    
    public void incrementBy(double value, long count)
    {
        synchronized(this)
        {
            super.incrementBy(value);
            _totalCount+=count;
            if(_totalCount!=0)
                _value=_sum/_totalCount;
            else
                _value=0;
        }
    }
    
    public void incrementBase(long count)
    {
        synchronized(this)
        {
            _totalCount+=count;
        }
    }
    
    @Override
    public void incrementBy(double value)
    {
        this.incrementBy(value,1);
    }

    @Override
    protected void flipChanged()
    {
        _sum=0;
        _totalCount=0;
    }

    @Override
    public void decrement()
    {
    }

    @Override
    public void decrementBy(double value)
    {
    }

    @Override
    public void setValue(double _value)
    {
        _lastValue=this._value;
        this._value = _value;
    }
}
