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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alachisoft.tayzgrid.common.caching.statistics.customcounters;

/**
 *
 * @author  
 */
public interface PerformanceCounter
{

    /**
     * Gets the name of the counter.
     *
     * @return
     */
    public String getName();

    /**
     * Gets the name of the instance to which this counter belongs.
     *
     * @return
     */
    public String getInstanceName();

    /**
     * Gets the category of the counter.
     *
     * @return
     */
    public String getCategory();

    /**
     * Increments the counter value by one.
     */
    public void increment();

    /**
     *
     * Decrements the counter value by given value
     *
     * @param value
     */
    public void incrementBy(double value);

    /**
     * Increments the counter value by one.
     */
    public void decrement();

    /**
     *
     * Decrements the counter value by given value.
     *
     * @param value
     */
    public void decrementBy(double value);

    /**
     *
     * Gets the counter value
     *
     * @return
     */
    public double getValue();

    /**
     *
     * Sets the counter value
     *
     * @param _value
     */
    public void setValue(double _value);

    public void reset();
    
    
  

}
