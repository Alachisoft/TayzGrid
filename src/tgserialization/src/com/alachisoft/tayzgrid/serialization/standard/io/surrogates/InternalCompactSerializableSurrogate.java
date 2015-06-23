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

import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.BuiltinSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheIOException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheInstantiationException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogateImpl;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectInputStream;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectOutputStream;
import java.io.Externalizable;
import java.io.IOException;

public class InternalCompactSerializableSurrogate extends SerializationSurrogateImpl
        implements BuiltinSerializationSurrogate
{

    /** Creates a new instance of ExternalizableSerializationSurrogate */
    public InternalCompactSerializableSurrogate(Class type)
    {
        super(type);
    }

    public Object readDirect(CacheObjectInput input, Object graph)
            throws CacheInstantiationException, CacheIOException
    {
        try
        {
            CompactReader reader = new CompactReader((ObjectInputStream)input);
            ((InternalCompactSerializable) graph).Deserialize(reader);
            return graph;
        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
        catch (ClassNotFoundException ex)
        {
            throw new CacheInstantiationException(ex);
        }
    }

    public void writeDirect(CacheObjectOutput output, Object graph)
            throws CacheIOException
    {
        try
        {
            CompactWriter writer = new CompactWriter((ObjectOutputStream)output);
            ((InternalCompactSerializable) graph).Serialize(writer);
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
            CompactReader reader = new CompactReader((ObjectInputStream)input);
            ((InternalCompactSerializable) graph).Deserialize(reader);
        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
        catch (ClassNotFoundException ex)
        {
            throw new CacheInstantiationException(ex);
        }
    }
}
