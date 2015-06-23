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

import javax.cache.processor.MutableEntry;

/**
 *
 * @author 
 * @param <K>
 * @param <V>
 */
public class JCacheMutableEntryWrapper<K,V> implements MutableEntry<K,V>
{
    com.alachisoft.tayzgrid.runtime.processor.MutableEntry<K,V> tayzGridEntry=null;
    
    /**
     *
     * @param entry
     */
    public JCacheMutableEntryWrapper(com.alachisoft.tayzgrid.runtime.processor.MutableEntry<K,V> entry)
    {
        tayzGridEntry=entry;
    }

    @Override
    public boolean exists() 
    {
        if(tayzGridEntry==null)return false;
        
        return tayzGridEntry.exists();
    }

    @Override
    public void remove() 
    {
        if(tayzGridEntry==null)return;
   
        tayzGridEntry.remove();
    }

    @Override
    public void setValue(V v) 
    {
        if(tayzGridEntry==null)return;
        
        tayzGridEntry.setValue(v);
    }

    @Override
    public K getKey() 
    {
        if(tayzGridEntry==null)return null;
        
        return (K)tayzGridEntry.getKey();
    }

    @Override
    public V getValue() 
    {
        if(tayzGridEntry==null)return null;
        
        return (V)tayzGridEntry.getValue();
    }

    @Override
    public <T> T unwrap(Class<T> type) 
    {
        if(tayzGridEntry==null)return null;
        
        return (T)tayzGridEntry;
    }    
}
