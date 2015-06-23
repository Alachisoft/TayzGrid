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

public class SkipSerializationSurrogate extends SerializationSurrogateBase implements SerializationSurrogate, BuiltinSerializationSurrogate
{
    public SkipSerializationSurrogate() {
        super(SkipSerializationSurrogate.class);
    }

    /**
     * Returns 'this'
     * @param input
     * @return
     * @throws CacheInstantiationException
     * @throws CacheIOException
     */
    public Object readObject(CacheObjectInput input) throws CacheInstantiationException, CacheIOException
    {
        return (Object)this;
    }

    /**
     * Does not perform any operation
     * @param output
     * @param graph
     * @throws CacheIOException
     */
    public void writeObject(CacheObjectOutput output, Object graph) throws CacheIOException
    {
        
    }

    /**
     * Does not perform any Operation
     * @param input 
     * @throws CacheInstantiationException
     * @throws CacheIOException
     */
    public void skipObject(CacheObjectInput input) throws CacheInstantiationException, CacheIOException
    {
        
    }
}
