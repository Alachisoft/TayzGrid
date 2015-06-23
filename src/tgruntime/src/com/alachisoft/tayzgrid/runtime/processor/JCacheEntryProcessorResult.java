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

package com.alachisoft.tayzgrid.runtime.processor;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

/**
 *
 * @author 
 */
public class JCacheEntryProcessorResult<T> implements EntryProcessorResult<T>
{
    private T value;
    private EntryProcessorException exception;
     
    public JCacheEntryProcessorResult(T v)
    {
        value=v;
    }
    public JCacheEntryProcessorResult()
    {
        
    }
    public JCacheEntryProcessorResult(EntryProcessorException ex)
    {
        exception=ex;
    }
    
    @Override
    public T get() throws EntryProcessorException 
    {
        if(exception!=null) throw exception;
        
        return value;
    } 

    /**
     * @param value the value to set
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * @param exception the exception to set
     */
    public void setException(EntryProcessorException exception) {
        this.exception = exception;
    }
}
