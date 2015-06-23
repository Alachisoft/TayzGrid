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

package com.alachisoft.tayzgrid.serialization.standard.io.surrogates;

import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.BuiltinSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheIOException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheInstantiationException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogateBase;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogateImpl;
import java.io.IOException;

public class UInt16ArraySerializationSurrogate extends SerializationSurrogateImpl implements BuiltinSerializationSurrogate
{

    /** Creates a new instance of DoubleSerializationSurrogate */
    public UInt16ArraySerializationSurrogate()
    {
        super(UInt16ArraySerializationSurrogate.class);
    }
    
    @Override
    public Object instantiate(CacheObjectInput input)
            throws CacheInstantiationException
    {
        try
        {
            int length = input.readInt();
            return new int[length];
        }
        catch (IOException ex)
        {
            throw new CacheInstantiationException(ex);
        }
    }
    
    
    @Override
    public Object readDirect(CacheObjectInput input, Object graph) throws CacheInstantiationException, CacheIOException
    {
        try
        {
            int[] integer = (int[])graph;
            for (int i = 0; i < integer.length; i++)
            {
                integer[i] = input.readUInt16();
            }
            return integer;

        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
    }

    /**
     * Write an object to the underlying storage or stream.  The
     * class that implements this interface defines how the object is
     * written.
     *
     * @param output the stream to write the object to.
     * @param graph the object to write .
     * @throws CacheIOException Any of the usual Input/Output related exceptions.
     */
    public void writeDirect(CacheObjectOutput output, Object graph)
            throws CacheIOException
    {
        try
        {
            int[] integer = ((int[])graph);
            output.writeInt(integer.length);
            for (int i = 0; i < integer.length; i++)
            {
                output.writeUInt16(integer[i]);
            }
        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
    }

    @Override
    public void skipDirect(CacheObjectInput input, Object graph) throws CacheInstantiationException, CacheIOException
    {
        try
        {
            int[] integer = (int[])graph;
            for (int i = 0; i < integer.length; i++)
            {
                input.skipUInt16();
            }

        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
    }
}
