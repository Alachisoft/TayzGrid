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
import java.io.IOException;

public class UInt16SerializationSurrogate extends SerializationSurrogateBase implements SerializationSurrogate, BuiltinSerializationSurrogate
{

    /** Creates a new instance of DoubleSerializationSurrogate */
    public UInt16SerializationSurrogate()
    {
        super(UInt16SerializationSurrogate.class);
    }

    public Object readObject(CacheObjectInput input)
            throws CacheInstantiationException, CacheIOException
    {
        try
        {
            return input.readUInt16();

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
    public void writeObject(CacheObjectOutput output, Object graph)
            throws CacheIOException
    {
        try
        {
            output.writeUInt16((Integer)graph);
        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
    }

    public void skipObject(CacheObjectInput input) throws CacheInstantiationException, CacheIOException
    {
        try
        {
            input.skipUInt16();

        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
    }
}
