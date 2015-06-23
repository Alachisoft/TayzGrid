/*
 * @(#)ObjectOutput.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.core.io;

import java.io.ObjectOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;

/**
 * ObjectOutput extends the ObjectOutput interface to include writing of
 * objects as instances of different types.
 *
 * @version 1.0, September 18, 2008
 */
public interface CacheObjectOutput extends ObjectOutput
{
    /**
     * Returns the current <see cref="SerializationContext"/> object.
     *
     * @return the current serialzation context
     */
    public SerializationContext getContext();

    /**
     * Returns the underlying OutputStream object. 
     */
    public OutputStream getBaseStream();

    /**
     * Write an object to the underlying storage or stream.  The
     * class that implements this interface defines how the object is
     * written.
     *
     * @param objClass the class surrogate to use for writing the object.
     * @param obj the object to be written
     * @exception IOException Any of the usual Input/Output related exceptions.
     */
    public void writeObject(Object obj, Class objClass) throws IOException;
    
    public void writeObject(Object obj, Object objClass) throws IOException;

    /**
     * Gets the current Cache Name
     * @return string CacheContext
     */
    public String getCacheContext();
    
    public void writeUInt16(int i) throws IOException;
    
    public void writeUInt32(long i) throws IOException;
    
    public void writeUInt64(BigInteger bigInteger) throws IOException;
    
    public void writeUByte(short sh) throws IOException;
}

