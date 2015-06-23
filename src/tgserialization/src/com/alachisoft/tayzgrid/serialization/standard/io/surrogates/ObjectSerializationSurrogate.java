/*
 * @(#)ObjectSerializationSurrogate.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.standard.io.surrogates;

import com.alachisoft.tayzgrid.serialization.core.io.ExtendedObjectInputStream;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.core.io.SerializationContext;
import com.alachisoft.tayzgrid.serialization.core.io.TypeSurrogateConstants;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.BuiltinSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheIOException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheInstantiationException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogateBase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * ObjectSerializationSurrogate is responsible for writing and reading
 * instances of unknown classes.
 *
 * @version 1.0, September 18, 2008
 */
public class ObjectSerializationSurrogate
        extends SerializationSurrogateBase
        implements SerializationSurrogate, BuiltinSerializationSurrogate
{
    /** Specify a hard handle for this surrogate */
    public static final short HARD_HANDLE = TypeSurrogateConstants.FirstTypeHandle + 0x00;

    /**
     * Creates a new instance of NxForbiddenSerializationSurrogate
     */
    public ObjectSerializationSurrogate(Class type)
    {
        super(type);
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
        try {
            Integer cookie = input.readInt();
            Object custom = input.getContext().getObject(cookie);
            if (custom == null)
            {
                int length = input.readInt();
                byte check = input.readByte();
                
                if (check == (byte)1 || (check == (byte)0 && length == 0))
                {
                    ObjectInput nativeReader = new ExtendedObjectInputStream(input.getBaseStream());
                    custom = nativeReader.readObject();
                    input.getContext().rememberForRead(custom);
                }
                else
                {
                    input.skipBytes(length);
                }
            }
            return custom;
        } catch (IOException ex) {
            throw new CacheIOException(ex);
        } catch (ClassNotFoundException ex) {
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
        try {
            Integer cookie = output.getContext().getCookie(graph);
            if (cookie != SerializationContext.InvalidCookie)
            {
                output.writeInt(cookie);
                return;
            }

            cookie = output.getContext().rememberForWrite(graph);
            output.writeInt(cookie);

            ByteArrayOutputStream val = new ByteArrayOutputStream();

            ObjectOutput nativeWriter = new ObjectOutputStream(val);
            nativeWriter.writeObject(graph);
            nativeWriter.flush();

            output.writeInt(val.toByteArray().length);
            output.writeByte((byte)1);

            output.write(val.toByteArray());
            output.flush();


        } catch (IOException ex) {
            throw new CacheIOException(ex);
        }
    }

    public void skipObject(CacheObjectInput input) throws CacheInstantiationException, CacheIOException
    {
        try {
            Integer cookie = input.readInt();
            Object custom = input.getContext().getObject(cookie);
            if (custom == null)
            {
                int length = input.readInt();
                byte check = input.readByte();
                
//                ByteArrayInputStream val = new ByteArrayInputStream(;
                //If an object have length zero in it's byte then deserialize the object using native serialization
                if (check == (byte)1 || (check == (byte)0 && length == 0))
                {
                    ObjectInput nativeReader = new ObjectInputStream(input.getBaseStream());
                    custom = nativeReader.readObject();
                    input.getContext().rememberForRead(custom);
                }
                else
                {
                    input.skipBytes(length);
                }
            }
        } catch (IOException ex) {
            throw new CacheIOException(ex);
        } catch (ClassNotFoundException ex) {
            throw new CacheInstantiationException(ex);
        }
    }
}
