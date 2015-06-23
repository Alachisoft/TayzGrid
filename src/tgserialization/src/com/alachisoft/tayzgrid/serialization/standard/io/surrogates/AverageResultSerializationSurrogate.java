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

import com.alachisoft.tayzgrid.runtime.queries.AverageResult;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.BuiltinSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheIOException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheInstantiationException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogateBase;
import com.alachisoft.tayzgrid.serialization.standard.io.TypeSurrogateSelectorImpl;
import java.io.IOException;
import java.math.BigDecimal;

public class AverageResultSerializationSurrogate extends SerializationSurrogateBase
        implements SerializationSurrogate, BuiltinSerializationSurrogate
{

    /** Creates a new instance of LongSerializationSurrogate */
    public AverageResultSerializationSurrogate()
    {
        super(AverageResult.class);
    }

    /**
     * Read and return an object. The class that implements this interface
     * defines where the object is "read" from.
     *
     * @param input the stream to read data from in order to restore the object.
     * @return the object read from the stream
     * @exception java.lang.ClassNotFoundException If the class of a serialized
     *      object cannot be found.
     * @throws CacheIOException If any of the usual Input/Output
 related exceptions occur.
     */
    public Object readObject(CacheObjectInput input)
            throws CacheInstantiationException, CacheIOException
    {
        try
        {
            AverageResult result = new AverageResult();
            TypeSurrogateSelectorImpl impl = TypeSurrogateSelectorImpl.getDefault();
            SerializationSurrogate decimalSurrogate = impl.getSurrogateForType(BigDecimal.class, null);
            result.setSum((BigDecimal)decimalSurrogate.readObject(input));
            result.setCount((BigDecimal)decimalSurrogate.readObject(input));
            return result;
        }
        catch (Exception ex)
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
            AverageResult result = (AverageResult)graph;
            TypeSurrogateSelectorImpl impl = TypeSurrogateSelectorImpl.getDefault();
            SerializationSurrogate decimalSurrogate = impl.getSurrogateForType(BigDecimal.class, null);
            decimalSurrogate.writeObject(output, result.getSum());
            decimalSurrogate.writeObject(output, result.getCount());
        }
        catch (Exception ex)
        {
            throw new CacheIOException(ex);
        }
    }

    public void skipObject(CacheObjectInput input) throws CacheInstantiationException, CacheIOException
    {
        try
        {
            TypeSurrogateSelectorImpl impl = TypeSurrogateSelectorImpl.getDefault();
            SerializationSurrogate decimalSurrogate = impl.getSurrogateForType(BigDecimal.class, null);
            decimalSurrogate.skipObject(input);
            decimalSurrogate.skipObject(input);
        }
        catch (Exception ex)
        {
            throw new CacheIOException(ex);
        }
    }
}
