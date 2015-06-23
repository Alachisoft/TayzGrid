/*
 * @(#)MapSerializationSurrogate.java	1.0
 *
 * Created on September 18, 2008, 12:36 PM
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * MapSerializationSurrogate class.
 *
 * @version 1.0, September 18, 2008
 */
public abstract class MapSerializationSurrogate<K, V>
        extends SerializationSurrogateImpl
        implements BuiltinSerializationSurrogate
{

    /** Creates a new instance of NxBooleanSerializationSurrogate */
    public MapSerializationSurrogate(Class<? extends Map<K, V>> cls)
    {
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
        Map<K, V> map = (Map<K, V>) graph;
        try
        {
            int len = input.readInt();
            for (int i = 0; i < len; i++)
            {
                K key = (K) input.readObject();
                V value = (V) input.readObject();
                map.put(key, value);
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
        Map<K, V> map = (Map<K, V>) graph;
        Set<Map.Entry<K, V>> entries = map.entrySet();
        int len = map.size();

        try
        {
            output.writeInt(len);
            if (len > 0)
            {
                Object[] obj = entries.toArray();
                for (int i = 0; i < len; i++)
                {
                    Map.Entry<K,V> entry = (Map.Entry<K,V>) obj[i];
                    output.writeObject(entry.getKey());
                    output.writeObject(entry.getValue());
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
                input.skipObject();
            }
        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
    }
}
