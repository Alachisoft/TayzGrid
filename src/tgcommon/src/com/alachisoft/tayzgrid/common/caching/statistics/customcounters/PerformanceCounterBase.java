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

public abstract class PerformanceCounterBase implements PerformanceCounter
{

    private String _name;
    protected String _instanceName;
    private String _category;
    protected double _value;
    protected double _lastValue;

    public PerformanceCounterBase(String name, String instance)
    {
        _name = null;
        _instanceName = instance;
        _category = null;
        _value = 0;
        _lastValue = 0;
    }

    public PerformanceCounterBase(String category, String name, String instance)
    {
        _name = null;
        _instanceName = instance;
        _category = category;
        _value = 0;
        _lastValue = 0;
    }

    /**
     * Gets the name of the counter.
     *
     * @return
     */
    @Override
    public String getName()
    {
        return _name;
    }

    /**
     * Gets the name of the instance to which this counter belongs.
     *
     * @return
     */
    @Override
    public String getInstanceName()
    {
        return _instanceName;
    }

    /**
     * Gets the category of the counter.
     *
     * @return
     */
    @Override
    public String getCategory()
    {
        return _category;
    }

    /**
     * Increments the counter value by one.
     */
    @Override
    public abstract void increment();

    /**
     *
     * Decrements the counter value by given value
     *
     * @param value
     */
    @Override
    public abstract void incrementBy(double value);

    /**
     * Increments the counter value by one.
     */
    @Override
    public abstract void decrement();

    /**
     *
     * Decrements the counter value by given value.
     *
     * @param value
     */
    @Override
    public abstract void decrementBy(double value);

    /**
     *
     * Gets the counter value
     *
     * @return
     */
    @Override
    public abstract double getValue();

    /**
     *
     * Sets the counter value
     *
     * @param _value
     */
    @Override
    public abstract void setValue(double _value);

    /**
     *
     */
    public void reset()
    {
        synchronized (this)
        {
            _value = _lastValue = 0;
        }
    }

    @Override
    public String toString()
    {
        String toStr = "[";
        toStr += _name != null ? "Name :" + _name : "";
        toStr += _instanceName != null ? "; Instance :" + _instanceName : "";
        toStr += _category != null ? "; Category :" + _category + "]" : "";
        return toStr;
    }
}
