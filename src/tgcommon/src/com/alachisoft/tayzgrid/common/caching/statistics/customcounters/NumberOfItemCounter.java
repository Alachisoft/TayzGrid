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

public class NumberOfItemCounter extends PerformanceCounterBase
{

    public NumberOfItemCounter(String name, String instance)
    {
        super(name, instance);
    }

    public NumberOfItemCounter(String category, String name, String instance)
    {
        super(category, name, instance);
    }

    @Override
    public void increment()
    {
        synchronized (this)
        {
            _value++;
        }
    }

    @Override
    public void incrementBy(double value)
    {
        synchronized (this)
        {
            _value += value;
        }
    }

    @Override
    public void decrement()
    {
        synchronized (this)
        {
            _value--;
        }
    }

    @Override
    public void decrementBy(double value)
    {
        synchronized (this)
        {
            _value -= value;
        }
    }

    @Override
    public double getValue()
    {
        return _value;
    }

    @Override
    public void setValue(double _value)
    {
        super._value = _value;
    }
}
