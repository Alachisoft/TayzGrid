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

import java.lang.management.ManagementFactory;


public class StringCounter extends PerformanceCounterBase{

    double value = 0;
    StringBuilder stringBuilder= new StringBuilder("");
    private String counterValue;
    
    public StringCounter(String name, String instance)
    {
        super(name,instance);
        stringBuilder = new StringBuilder("");
        counterValue = "";

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
        return value;
    }

    @Override
    public void setValue(double _value) {
    }
    
    public void appendValue(String cacheID,String IP, String port)
    {
            
        stringBuilder.append("[cacheID:").append(cacheID).append(",Address:").append(IP) .append(",Port:").append(port).append("]");
 
     }
    public String getStringValue()
    {
        //stringBuilder.setLength(0);
        return stringBuilder.toString();
    }
    
    public void setValue(String value)
    {
        stringBuilder.setLength(0);
        stringBuilder.append(value);
     }
    
    
    
    
}
