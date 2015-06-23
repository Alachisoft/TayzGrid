/*
 * @(#)SerializationSurrogateBase.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.core.io.surrogates;

import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * SerializationSurrogateBase class.
 *
 * @version 1.0, September 18, 2008
 */
public class SerializationSurrogateBase
{

    private short mTypeHandle;
    private short mSubTypeHandle = 0;
    private Class mActualType;
    private Constructor mConstructor;
    static private Object[] mEmptyArgs = new Object[0];

    /**
     * Constructor.
     *
     * @param type The type for which it is a surrogate.
     */
    public SerializationSurrogateBase(Class type)
    {
        this.mActualType = type;
        try
        {
            this.mConstructor = type.getDeclaredConstructor(new Class[0]);
            this.mConstructor.setAccessible(true);
        }
        catch (Exception ex)
        {
        }
    }

    /**
     * Return the type of object for which this object is a surrogate.
     *
     * @return the type of object for which this object is a surrogate
     */
    public Class getRealClass()
    {
        return mActualType;
    }

    /**
     * Gets the magic ID associated with the type provided by the
     * NxTypeSurrogateSelector.
     *
     * @return Magic ID associated with the type
     */
    public short getClassHandle()
    {
        return mTypeHandle;
    }

    /**
     * Sets the magic ID associated with the type provided by the
     * NxTypeSurrogateSelector.
     *
     * @param value Magic ID for the type.
     */
    public void setClassHandle(short value)
    {

        mTypeHandle = value;
    }

    public short getSubHandle()
    {
        return mSubTypeHandle;
    }

    public void setSubTypeHandle(short subHandle)
    {
        mSubTypeHandle = subHandle;
    }
    
    public boolean isVersionCompatible()
    {
        return false;
    }

    /**
     * Creates instance of type returned by getRealClass(). Calls the default
     * constructor and returns the object. There must be a default constructor
     * even though it is private.
     *
     * @return Object that this surrogate must deserialize
     */
    public Object createInstance() throws CacheInstantiationException
    {
        try
        {
            if (mActualType.isInterface())
            {
                return null;
            }
            if (this.mConstructor != null)
            {
                return this.mConstructor.newInstance(mEmptyArgs);
            }
            return newInstanceUsingEnclosingClasses(mActualType);
        }
        catch (Exception ex)
        {
            throw new CacheInstantiationException(ex);
        }
    }

    /**
     * Instantiates a class with its empty constructor.
     * An enclosed class does not contain an empty constructor even if explicitly declared, not until if it is static.
     * It requires a new instance of its enclosing class. Therefore all its enclosing classes should have an empty constructor.
     * This method is called recursively until there is no enclosing type or an empty constructor can be found
     * @param type Class to initialize
     * @return New instance
     * @throws NoSuchMethodException If Constructors are not found of either the class itself or its enclosing classes
     * @throws IllegalAccessException Access modifers
     * @throws InstantiationException Error in instantiation
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws CacheInstantiationException
     */
    private Object newInstanceUsingEnclosingClasses(Class type) throws NoSuchMethodException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException, CacheInstantiationException
    {
        try
        {
            if (type.getConstructor() != null)
            {
                return type.newInstance();
            }
        }
        catch (NoSuchMethodException noSuchMethodException)
        {
        }

        //Throws exception if not found
        if (type.getConstructor(type.getEnclosingClass()) != null)
        {
            return type.getConstructor(type.getEnclosingClass()).newInstance(newInstanceUsingEnclosingClasses(type.getEnclosingClass()));
        }
        throw new CacheInstantiationException(type.getCanonicalName() + " and/or its enclosing classes do not have an empty constructor");
    }

    /**
     * Writes object meta information, i.e. handle to the specified <paramref name="writer"/> object.
     *
     * @param output the stream to write the object to.
     * @param graph the object to write .
     * @exception IOException Any of the usual Input/Output related exceptions.
     */
    public void writeHandle(CacheObjectOutput output, Object graph)
            throws CacheIOException
    {
        try
        {
            output.writeShort(this.mTypeHandle);
        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
    }

    public void writeSubHandle(CacheObjectOutput output, Object graph)
            throws CacheIOException
    {
        try
        {
            output.writeShort(this.mSubTypeHandle);
        }
        catch (IOException ex)
        {
            throw new CacheIOException(ex);
        }
    }
}
