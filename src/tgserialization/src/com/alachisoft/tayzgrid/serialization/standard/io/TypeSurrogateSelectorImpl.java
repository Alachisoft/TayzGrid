/*
 * @(#)TypeSurrogateSelectorImpl.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.standard.io;

import com.alachisoft.tayzgrid.serialization.core.io.TypeSurrogateConstants;
import com.alachisoft.tayzgrid.serialization.core.io.TypeSurrogateSelectorBase;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.AverageResultSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.BigDecimalSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.BigIntegerSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.BooleanArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.BooleanSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ByteArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ByteSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.CharArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.CharSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.CollectionSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.DateArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.DateSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.DoubleArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.DoubleSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.EOFDotNetSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.EOFJavaSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.FloatArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.FloatSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.GenericArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.IntegerArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.IntegerSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.LongArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.LongSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.MapSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.NullSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ObjectArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ObjectSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ShortArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ShortSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.SkipSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.SqlDateSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.SqlTimeSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.SqlTimestampSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.StackTraceElementSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.StringArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.StringSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ThrowableSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt16ArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt16SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt32ArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt32SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt64ArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UInt64SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UnsignedByteArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.UnsignedByteSerializationSurrogate;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * TypeSurrogateSelectorImpl class.
 *
 * @version 1.0, September 18, 2008
 */
public class TypeSurrogateSelectorImpl extends TypeSurrogateSelectorBase
{
    /**
     * Minimum id that can be assigned to a user defined type.
     */
    private static TypeSurrogateSelectorImpl msDefault;

    /**
     * Creates a new instance of TypeSurrogateSelectorBase
     */
    public TypeSurrogateSelectorImpl() {
        construct(true);
    }

    /**
     * Creates a new instance of TypeSurrogateSelectorBase
     */
    public TypeSurrogateSelectorImpl(boolean useBuiltinSurrogates) {
        construct(useBuiltinSurrogates);
    }

    /***
     */
    private void construct(boolean useBuiltinSurrogates) {

        mNullSurrogate = new NullSerializationSurrogate();
        mDefaultSurrogate = new ObjectSerializationSurrogate(Object.class);
        mDefaultArraySurrogate = new ObjectArraySerializationSurrogate();
        try {

            this.register(mNullSurrogate, NullSerializationSurrogate.HARD_HANDLE);
            this.register(mDefaultSurrogate, ObjectSerializationSurrogate.HARD_HANDLE);
            this.register(mDefaultArraySurrogate, ObjectArraySerializationSurrogate.HARD_HANDLE);
            if (useBuiltinSurrogates)
            {
                registerBuiltinSurrogates();
            }
        } catch (CacheArgumentException ex) {
        }
    }

    /*
     * Returns the default type surrogate selector Object
    */
    public static TypeSurrogateSelectorImpl getDefault()
    {
        if(TypeSurrogateSelectorImpl.msDefault == null)
            TypeSurrogateSelectorImpl.msDefault = new TypeSurrogateSelectorImpl();
        return TypeSurrogateSelectorImpl.msDefault;
    }

    /*
     * Returns the default type surrogate selector Object
    */
    public static void setDefault(TypeSurrogateSelectorImpl value)
    {
        TypeSurrogateSelectorImpl.msDefault = value;
    }

    /**
     * Unregisters all surrogates, except null and default ones.
     */
    public void clear()
    {
        super.clear();
        try {
            this.register(mNullSurrogate, NullSerializationSurrogate.HARD_HANDLE);
            this.register(mDefaultSurrogate, ObjectSerializationSurrogate.HARD_HANDLE);
            this.register(mDefaultArraySurrogate, ObjectSerializationSurrogate.HARD_HANDLE);
        } catch (CacheArgumentException ex) {
        }
    }

    /**
     * Registers built-in surrogates with the system.
     */
    public synchronized void registerBuiltinSurrogates() throws CacheArgumentException
    {
        short typeHandle = TypeSurrogateConstants.FirstTypeHandle + 100;

        typeHandle = TypeSurrogateConstants.FirstTypeHandle + 4;
        register(new BooleanSerializationSurrogate(), typeHandle++);
        register(new UnsignedByteSerializationSurrogate(), typeHandle++);
        register(new CharSerializationSurrogate(), typeHandle++);
        register(new FloatSerializationSurrogate(), typeHandle++);
        register(new DoubleSerializationSurrogate(), typeHandle++);
        register(new BigDecimalSerializationSurrogate(), typeHandle++);
        register(new ShortSerializationSurrogate(), typeHandle++);
        register(new IntegerSerializationSurrogate(), typeHandle++);
        register(new LongSerializationSurrogate(), typeHandle++);
        register(new StringSerializationSurrogate(), typeHandle++ );
        register(new DateSerializationSurrogate(), typeHandle++);
        register(new NullSerializationSurrogate(), typeHandle++);
        register(new BooleanArraySerializationSurrogate(), typeHandle++);
        register(new UnsignedByteArraySerializationSurrogate(), typeHandle++);
        register(new CharArraySerializationSurrogate(), typeHandle++);
        register(new FloatArraySerializationSurrogate(), typeHandle++);
        register(new DoubleArraySerializationSurrogate(), typeHandle++);
        register(new ShortArraySerializationSurrogate(), typeHandle++);
        register(new IntegerArraySerializationSurrogate(), typeHandle++);
        register(new LongArraySerializationSurrogate(), typeHandle++);
        register(new StringArraySerializationSurrogate(), typeHandle++);

        register(new AverageResultSerializationSurrogate(), typeHandle++);

        //End of File for Java: To be provided by .Net Client
        register(new EOFJavaSerializationSurrogate(), typeHandle++);
        //End of File for Java: To be provided by Java Client
        register(new EOFDotNetSerializationSurrogate(), typeHandle++);
        //Skip this value Surrogate
        register(new SkipSerializationSurrogate(), typeHandle++);

        register(new NullSerializationSurrogate(), typeHandle++);
        register(new DateArraySerializationSurrogate(), typeHandle++);
        register(new NullSerializationSurrogate(), typeHandle++);
        register(new ByteArraySerializationSurrogate(), typeHandle++);
        register(new UInt16ArraySerializationSurrogate(), typeHandle++);
        register(new UInt32ArraySerializationSurrogate(), typeHandle++);
        register(new UInt64ArraySerializationSurrogate(), typeHandle++);

        register(new NullSerializationSurrogate(), typeHandle++);
        register(new ByteSerializationSurrogate(), typeHandle++);
        register(new UInt16SerializationSurrogate(), typeHandle++);
        register(new UInt32SerializationSurrogate(), typeHandle++);
        register(new UInt64SerializationSurrogate(), typeHandle++);


        register(new NullSerializationSurrogate(), typeHandle++);



        register(new CollectionSerializationSurrogate(ArrayList.class) { }, typeHandle++);
        register(new NullSerializationSurrogate(), typeHandle++);
        register(new MapSerializationSurrogate(HashMap.class) { }, typeHandle++);




        register(new GenericArraySerializationSurrogate<BigInteger>(BigInteger[].class), typeHandle++);

        register(new CollectionSerializationSurrogate(LinkedList.class) { }, typeHandle++);


        typeHandle = TypeSurrogateConstants.FirstTypeHandle + 400;
        register(new ThrowableSerializationSurrogate(Exception.class), typeHandle++);
        register(new ThrowableSerializationSurrogate(Error.class), typeHandle++);
        register(new StackTraceElementSerializationSurrogate(), typeHandle++);
        register(new GenericArraySerializationSurrogate<StackTraceElement>(StackTraceElement[].class), typeHandle++);

        typeHandle = TypeSurrogateConstants.FirstTypeHandle + 600;
        register(new GenericArraySerializationSurrogate<Boolean>(Boolean[].class), typeHandle++);
        register(new GenericArraySerializationSurrogate<Byte>(Byte[].class), typeHandle++);
        register(new GenericArraySerializationSurrogate<Character>(Character[].class), typeHandle++);
        register(new GenericArraySerializationSurrogate<Float>(Float[].class), typeHandle++);
        register(new GenericArraySerializationSurrogate<Double>(Double[].class), typeHandle++);
        register(new GenericArraySerializationSurrogate<Short>(Short[].class), typeHandle++);
        register(new GenericArraySerializationSurrogate<Integer>(Integer[].class), typeHandle++);
        register(new GenericArraySerializationSurrogate<Long>(Long[].class), typeHandle++);

        typeHandle = TypeSurrogateConstants.FirstTypeHandle + 900;
        register(new SqlDateSerializationSurrogate(), typeHandle++);
        register(new SqlTimeSerializationSurrogate(), typeHandle++);
        register(new SqlTimestampSerializationSurrogate(), typeHandle++);

        register(new BigIntegerSerializationSurrogate(), typeHandle++);
        register(new MapSerializationSurrogate(Hashtable.class) { },(short) 2000);


    }
}
