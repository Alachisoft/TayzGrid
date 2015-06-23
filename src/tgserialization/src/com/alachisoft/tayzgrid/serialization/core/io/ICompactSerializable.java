/*
 * @(#)Externalizable.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.core.io;

import java.io.IOException;

/**
 * Externalizable interface.
 *
 * @version 1.0, September 18, 2008
 */
public interface ICompactSerializable  {
    /**
     * The object implements the serialize method to save its contents
     * by calling the methods of DataOutput for its primitive values or
     * calling the writeObject method of ObjectOutput for objects, strings,
     * and arrays.
     *
     * @param out the stream to write the object to
     * @exception IOException Includes any I/O exceptions that may occur
     */
    void serialize(CacheObjectOutput out) throws IOException;

    /**
     * The object implements the deserialize method to restore its
     * contents by calling the methods of DataInput for primitive
     * types and readObject for objects, strings and arrays.  The
     * deserialize method must read the values in the same sequence
     * and with the same types as were written by serialize.
     *
     * @param in the stream to read data from in order to restore the object
     * @exception IOException if I/O errors occur
     * @exception ClassNotFoundException If the class for an object being
     *              restored cannot be found.
     */
    void deserialize(CacheObjectInput in) throws IOException, ClassNotFoundException;
}
