/*
 * @(#)StackTraceElementSerializationSurrogate.java	1.0
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
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogateBase;
import java.io.IOException;

/**
 * StackTraceElementSerializationSurrogate is responsible for writing and reading
 * instances of StackTraceElement class.
 *
 * @version 1.0, September 18, 2008
 */
public class StackTraceElementSerializationSurrogate
        extends SerializationSurrogateBase
        implements SerializationSurrogate, BuiltinSerializationSurrogate
{

    /** Creates a new instance of NxBooleanSerializationSurrogate */
    public StackTraceElementSerializationSurrogate()
    {
        super(StackTraceElement.class);
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
            String className = input.readUTF();
            String mtdName = input.readUTF();
            String fileName = input.readUTF();
            int lineNum = input.readInt();
            return new StackTraceElement(className, mtdName, fileName, lineNum);
        }
        catch (IOException ex)
        {
            throw new CacheInstantiationException(ex);
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
        StackTraceElement elem = (StackTraceElement) graph;
        try
        {
            output.writeUTF(elem.getClassName());
            output.writeUTF(elem.getMethodName());
            output.writeUTF(elem.getFileName());
            output.writeInt(elem.getLineNumber());
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
            input.skipUTF();
            input.skipUTF();
            input.skipUTF();
            input.skipInt();
        }
        catch (IOException ex)
        {
            throw new CacheInstantiationException(ex);
        }
    }
}
