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

package com.alachisoft.tayzgrid.processor;

/**
 *
 * @author 
 * @param <K>
 * @param <V>
 */
public class TayzGridMutableEntry<K,V> implements com.alachisoft.tayzgrid.runtime.processor.MutableEntry<K,V>
{
    private K key=null;
    private V value=null;    
    
    private Boolean isAvailableInCache=false;
    private Boolean isUpdated=false;
    private Boolean isRemoved=false;
    
    public TayzGridMutableEntry(K key,V value)
    {
        this.key=key;
        
        if(value!=null)
        {
            this.value=value;            
            isAvailableInCache=true;
        }
        
    }
    
    @Override
    public K getKey() 
    {
        return (K)key;
        
    }

    @Override
    public V getValue() {
        return (V)value;
    }

    @Override
    public <T> T unwrap(Class<T> type) 
    {
        return (T)value;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean exists() 
    {
        return value!=null;
    }

    @Override
    public void remove() 
    {
        value=null;
        isRemoved=true;        
        isUpdated=false;
    }

    /**
     *
     * @param v
     */
    @Override
    public void setValue(V v) 
    {
        if(v==null)
            throw new IllegalArgumentException("value can not be null.");
        value=v;
        
        isUpdated=true;      
        isRemoved=false;
    }
    
    /**
     * @return the isUpdated
     */
    public Boolean isUpdated() 
    {
        return isUpdated;
    }

    /**
     * @return the isRemoved
     */
    public Boolean isRemoved() {
        return isAvailableInCache && isRemoved;
    }
}
