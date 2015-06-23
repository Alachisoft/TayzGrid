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
 * RateOfCounter is an instantaneous counter used for calculating average operations performed in one second.
 * For e.g Average data sent
 */
public class RateOfCounter extends InstantaneousCounter
{

    public RateOfCounter(String name, String instance)
    {
        super(name, instance);
    }

    public RateOfCounter(String category, String name, String instance)
    {
        super(category, name, instance);
    }

    @Override
    protected void calculate(double value)
    {
        _value += value;
    }

    @Override
    protected void flipChanged()
    {
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
    public double getValue()
    {
        super.updateIfFlipChanged();
        return _lastValue;
    }

    @Override
    public void setValue(double _value)
    {
        this._value = _value;
    }
}
