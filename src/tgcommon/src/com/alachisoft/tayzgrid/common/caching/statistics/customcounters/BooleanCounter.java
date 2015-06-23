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



public class BooleanCounter extends PerformanceCounterBase
{
    boolean flag = false;

    public BooleanCounter(String name, String instance) {
        super(name, instance);
    }

    @Override
    public void increment() {
        
    }

    @Override
    public void incrementBy(double value) {
        
    }

    @Override
    public void decrement() {
        
    }

    @Override
    public void decrementBy(double value) {
        
    }

    @Override
    public double getValue() {
        return 0;
        
    }

    @Override
    public void setValue(double _value) {
        
    }
    
    public void setInstanceName(String name){
        this._instanceName = name;
    }
    
    public boolean getBooleanValue()
    {
        return flag;
    }
    
    public void setBooleanValue(boolean value)
    {
        flag = value;
    }
    
    @Override
    public String toString()
    {
        return Boolean.toString((flag));
    }
}
