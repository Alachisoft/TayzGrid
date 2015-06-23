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
import com.alachisoft.tayzgrid.runtime.processor.EntryProcessorResult;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

/**
 *
 * @author 
 * @param <T>
 */
public class TayzGridEntryProcessorResult<T> implements EntryProcessorResult<T>,InternalCompactSerializable
{
    private T value;
    private EntryProcessorException exception;
    
    public TayzGridEntryProcessorResult()
    {        
    }
     
    public TayzGridEntryProcessorResult(T v)
    {
        value=v;
    }
    
    public TayzGridEntryProcessorResult(EntryProcessorException ex)
    {
        exception=ex;
    }
    
    @Override
    public T get() throws EntryProcessorException 
    {
        if(exception!=null) throw exception;
        
        return value;
    }   

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException 
    {
        value=(T) reader.ReadObject();
        exception=(EntryProcessorException) reader.ReadObject();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException 
    {
        writer.WriteObject(value);
        writer.WriteObject(exception);
    }
}
