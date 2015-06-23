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
 * Instantaneous counter is the base class for all types of instantaneous counters. An instantaneous counters values must change after each second i.e. on per second basis. All
 * values calculated in a time interval of one seconds becomes invalid for the next interval and it is re-initialized.
 */
public abstract class InstantaneousCounter extends PerformanceCounterBase
{

    InstantaneousFlip currentFlip;

    public InstantaneousCounter(String name, String instance)
    {
        super(name, instance);
        currentFlip = FlipManager.flip.clone();
    }

    public InstantaneousCounter(String category, String name, String instance)
    {
        super(category, name, instance);
        currentFlip = FlipManager.flip.clone();
    }

    @Override
    public void increment()
    {
        incrementBy(1);
    }

    @Override
    public void incrementBy(double value)
    {
        synchronized (this)
        {
            if (!currentFlip.equals(FlipManager.flip))
            {
                if (FlipManager.flip.getFlip() == currentFlip.getFlip() + 1)
                {
                    _lastValue = _value;
                }
                else
                {
                    _lastValue = 0;
                }
                _value = 0;
                currentFlip = FlipManager.flip.clone();
                flipChanged();
            }
            calculate(value);
        }
    }

    protected abstract void calculate(double value);

    protected abstract void flipChanged();
    
    @Override
    public double getValue()
    {
        updateIfFlipChanged();
        return _lastValue;
    }

    protected void updateIfFlipChanged()
    {
        synchronized(this)
        {
            if (!currentFlip.equals(FlipManager.flip))
            {
                if (FlipManager.flip.getFlip() == currentFlip.getFlip() + 1)
                {
                    _lastValue = _value;
                }
                else
                {
                    _lastValue = 0;
                }
                _value = 0;
                currentFlip = FlipManager.flip.clone();
                flipChanged();
            }
        }
    }
}
