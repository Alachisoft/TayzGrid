/*
 * @(#)ExternalizableSerializationSurrogate.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.standard.io.surrogates;

import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.BuiltinSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheIOException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheInstantiationException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogateImpl;
import java.io.Externalizable;
import java.io.IOException;

/**
 * ExternalizableSerializationSurrogate is responsible for writing and reading
 * instances of class that implement the Externalizable interface.
 *
 * @version 1.0, September 18, 2008
 */
public class ExternalizableSerializationSurrogate
        extends SerializationSurrogateImpl
        implements BuiltinSerializationSurrogate
{

    /** Creates a new instance of ExternalizableSerializationSurrogate */
    public ExternalizableSerializationSurrogate(Class type)
    {
        super(type);
    }

    public Object readDirect(CacheObjectInput input, Object graph)
            throws CacheInstantiationException, CacheIOException
    {
        try
        {
            ((Externalizable) graph).readExternal(input);
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
            ((Externalizable) graph).writeExternal(output);
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
            ((Externalizable) graph).readExternal(input);
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
