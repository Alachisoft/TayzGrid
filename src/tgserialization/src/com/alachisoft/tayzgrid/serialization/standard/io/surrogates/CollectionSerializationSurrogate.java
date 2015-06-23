/*
 * @(#)CollectionSerializationSurrogate.java	1.0
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
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * CollectionSerializationSurrogate<E> is responsible for writing and reading
 * instances of Collection<E> class.
 *
 * @version 1.0, September 18, 2008
 */
public abstract class CollectionSerializationSurrogate<E>
        extends SerializationSurrogateImpl
        implements BuiltinSerializationSurrogate
{
    /** Creates a new instance of NxBooleanSerializationSurrogate */
    public CollectionSerializationSurrogate(Class<? extends Collection<E>> cls) {
        super(cls);
    }

    /**
     * Read an object of type returned by getRealClass() from the stream reader.
     * A fresh instance of the object is passed as parameter.
     * The surrogate should populate fields in the object from data on the stream
     *
     * @param input stream reader
     * @param graph a fresh instance of the object that the surrogate must deserialize.
     * @throws CacheInstantiationException Object creation related exceptions.
     * @throws CacheIOException Any of the usual Input/Output related exceptions.
     * @return object read from the stream reader
     */
    public Object readDirect(CacheObjectInput input, Object graph)
	           throws CacheInstantiationException, CacheIOException
    {
        Collection<E> object = (Collection<E>) graph;
        E item = null;
        try
        {
            int len = input.readInt();
            for (int i = 0; i < len; i++)
            {
                item = (E) input.readObject();
                object.add(item);
            }
        }
        catch (ClassNotFoundException ex)
        {
            throw new CacheIOException(ex);
        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
        return graph;
    }

    /**
     * Write an object of type returned by getRealClass( to the stream writer
     *
     * @param output stream writer
     * @param graph object to be written to the stream reader
     * @throws CacheIOException Any of the usual Input/Output related exceptions.
     */
    public void writeDirect(CacheObjectOutput output, Object graph)
      throws CacheIOException
   {
        Collection<E> object = (Collection<E>)graph;
        int len = object.size();

        try {
            output.writeInt(len);
            if(len > 0)
            {
                Object[] obj = object.toArray();
                for (int i = 0; i < obj.length; i++)
                {
                    output.writeObject(obj[i]);
                }
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
            int len = input.readInt();
            for (int i = 0; i < len; i++)
            {
                input.skipObject();
            }
        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
    }
}
