/*
 * @(#)SerializationSurrogate.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.core.io.surrogates;

import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;

/**
 * SerializationSurrogate class.
 *
 * @version 1.0, September 18, 2008
 */
public interface SerializationSurrogate
{

    /**
     * Return the Class of object for which this object is a surrogate.
     *
     * @return the Class of the surrogate Class type.
     */
    public Class getRealClass();

    /**
     * Return the handle associated with the Class surrogate.
     *
     * @return the handle associated with the surrogate Class type.
     */
    public short getClassHandle();

    /**
     * Sub handle associated with Surrogate
     * @return returns associated SubHandle, will return 0 when not portable
     */
    public short getSubHandle();

    /**
     * Set the handle for this instance of the Class surrogate.
     *
     * @param handle the handle associated with the surrogate Class type.
     */
    public void setClassHandle(short handle);

    /**
     * Sub handle associated with Surrogate
     * @param subHandle assigned when portable
     */
    public void setSubTypeHandle(short subHandle);

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
            throws CacheInstantiationException, CacheIOException;

    /**
     * Writes object meta information, i.e. handle to the specified <paramref name="writer"/> object.
     *
     * @param output the stream to write the object to.
     * @param graph the object to write .
     * @throws CacheIOException Any of the usual Input/Output related exceptions.
     */
    public void writeHandle(CacheObjectOutput output, Object graph)
            throws CacheIOException;
    
    public void writeSubHandle(CacheObjectOutput output, Object graph) throws CacheIOException;

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
            throws CacheIOException;
    
    public void skipObject(CacheObjectInput input) throws CacheInstantiationException, CacheIOException;
}
