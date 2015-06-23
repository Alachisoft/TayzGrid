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

import com.alachisoft.tayzgrid.runtime.exceptions.EntryProcessorException;
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessor;
import com.alachisoft.tayzgrid.runtime.processor.MutableEntry;

/**
 *
 * @author 
 * @param <K>
 * @param <V>
 * @param <T>
 */
public class JCacheEntryProcessor<K,V,T> implements EntryProcessor<K,V,T>
{    
    private javax.cache.processor.EntryProcessor jcacheProcessor=null;    

    public JCacheEntryProcessor(javax.cache.processor.EntryProcessor processor)
    {
        jcacheProcessor=processor;
    }
    @Override
    public T processEntry(MutableEntry<K, V> entry, Object... arguments) throws EntryProcessorException 
    {
        if(jcacheProcessor!=null)
        {
            JCacheMutableEntryWrapper<K,V> jcacheEntryWrapper=new JCacheMutableEntryWrapper<K, V>(entry);
            
            try
            {
                return (T)jcacheProcessor.process(jcacheEntryWrapper, arguments);
            }
            catch(javax.cache.processor.EntryProcessorException ex)
            {
                throw new EntryProcessorException(ex);
            }
        }
        return null;
    }

    @Override
    public Boolean ignoreLock() 
    {
       return true;
    }    
}
