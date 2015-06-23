/*
 * @(#)ObjectArraySerializationSurrogate.java	1.0
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
import com.alachisoft.tayzgrid.serialization.core.io.TypeSurrogateConstants;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.BuiltinSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheIOException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheInstantiationException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogateImpl;
import com.alachisoft.tayzgrid.serialization.standard.io.TypeSurrogateSelectorImpl;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

/**
 * ObjectArraySerializationSurrogate is responsible for writing and reading instances of Object[] classe.
 *
 * @version 1.0, September 18, 2008
 */
public class ObjectArraySerializationSurrogate
        extends SerializationSurrogateImpl
        implements BuiltinSerializationSurrogate
{

    /**
     * Specify a hard handle for this surrogate
     */
    public static final short HARD_HANDLE = TypeSurrogateConstants.FirstTypeHandle + 0x03;

    /**
     * Creates a new instance of ObjectArraySerializationSurrogate
     */
    public ObjectArraySerializationSurrogate()
    {
        super(Object[].class);
    }

    /**
     * Creates instance of type returned by getRealClass(). This is different from NxSerializationSurrogateBase.createTypeInstance() in the sense that an NCacheObjectInput object
     * is passed as parameter that can be used to read creation specific information from the stream. For example it can be used to read the length of the array before actually
     * reading the values.
     *
     * The default implementation simply delegates to super.createTypeInstance().
     *
     * @param input stream reader
     * @throws CacheInstantiationException Object creation related exceptions.
     * @return Object that this surrogate must deserialize
     */
    @Override
    public Object instantiate(CacheObjectInput input)
            throws CacheInstantiationException
    {
        try
        {
            int length = input.readInt();
            return new Object[length];
        }
        catch (IOException ex)
        {
            throw new CacheInstantiationException(ex);
        }
    }

    /**
     * Read an object of type returned by getRealClass() from the stream reader. A fresh instance of the object is passed as parameter. The surrogate should populate fields in the
     * object from data on the stream
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
        try
        {
            Object[] array = (Object[]) graph;

            TypeSurrogateSelectorImpl mSelector = TypeSurrogateSelectorImpl.getDefault();
            short typeHandle = input.readShort();
            SerializationSurrogate surrogate = mSelector.getSurrogateForTypeHandle(typeHandle, input.getCacheContext());


            if (surrogate == null)
            {
                surrogate = mSelector.GetSurrogateForSubTypeHandle(typeHandle, input.readShort(), input.getCacheContext());
            }

            Class className = surrogate.getRealClass();

            Object obj = Array.newInstance(className, array.length);


            int len = array.length;
            for (int i = 0; i < len; i++)
            {
                Array.set(obj, i, input.readObject());
            }

            return obj;
        }
        catch (ClassNotFoundException ex)
        {
            throw new CacheInstantiationException(ex);
        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
    }

    /**
     * Write an object of type returned by getRealClass() to the stream writer
     *
     * @param output stream writer
     * @param graph object to be written to the stream reader
     * @throws CacheIOException Any of the usual Input/Output related exceptions.
     */
    public void writeDirect(CacheObjectOutput output, Object graph)
            throws CacheIOException
    {
        try
        {


            Object[] array = (Object[]) graph;
            int len = array.length;
            //length is read in SerializationSurrogateImpl ... which is wrong ... needs to be fixed for understanding and flow
            output.writeInt(len);


            TypeSurrogateSelectorImpl mSelector = TypeSurrogateSelectorImpl.getDefault();

            Object[] o = (Object[]) graph;
            Object[] tempObject = new Object[2];


            if (graph.getClass() != tempObject.getClass())
            {
                Object obj = null;
                for (int i = 0; i < o.length; i++)
                {
                    if (o[i] != null)
                    {
                        obj = o[i];
                        break;
                    }
                }
                SerializationSurrogate surrogateTemp = mSelector.getSurrogateForObject(obj, output.getCacheContext());
                surrogateTemp.writeHandle(output, obj);
                if (surrogateTemp.getSubHandle() > 0)
                {
                    surrogateTemp.writeSubHandle(output, obj);
                }
            }
            else
            {
                SerializationSurrogate surrogateTemp = mSelector.getDefaultSurrogate();
                surrogateTemp.writeHandle(output, new Object());
            }


            for (int i = 0; i < len; i++)
            {
                try
                {
                    output.writeObject(array[i]);

                }catch (IOException ex)
                {
                    throw new CacheIOException(ex);
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
            Object[] array = (Object[]) graph;

            TypeSurrogateSelectorImpl mSelector = TypeSurrogateSelectorImpl.getDefault();
            short typeHandle = input.readShort();
            SerializationSurrogate surrogate = mSelector.getSurrogateForTypeHandle(typeHandle, input.getCacheContext());

            if (surrogate == null)
            {
                surrogate = mSelector.GetSurrogateForSubTypeHandle(typeHandle, input.readShort(), input.getCacheContext());
            }

            Class className = surrogate.getRealClass();


            int len = array.length;
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
