/*
 * @(#)TypeSurrogateConstants.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.core.io;

/**
 * Provides constant definitions for various classes
 *
 * @version 1.0, September 18, 2008
 */
public final class TypeSurrogateConstants
{
    /**
     * Absolute minimum id that can be assigned to a user defined type. 
     */
    public static final short FirstTypeHandle = Short.MIN_VALUE;
    
    /**
     * Minimum id that can be assigned to a user defined type.
     */
    public static final short   MinTypeHandle = Short.MIN_VALUE + 4096;

    /**
     * Maximum id that can be assigned to a user defined type. 
     */
    public static final short   MaxTypeHandle = Short.MAX_VALUE;
}
