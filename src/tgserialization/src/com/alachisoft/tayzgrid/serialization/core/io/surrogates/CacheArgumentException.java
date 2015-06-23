/*
 * @(#)NxInstantiationException.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.core.io.surrogates;

/**
 * ArgumentException class.
 *
 * @version 1.0, September 18, 2008
 */
public class CacheArgumentException extends Exception {
    
    /** Constructs an ArgumentException without a detail message. */
    public CacheArgumentException() {
    }

    /** Constructs an ArgumentException with a detail message. */
    public CacheArgumentException(String message) {
        super(message);
    }

    /** Constructs a new exception with the specified cause. */
    public CacheArgumentException(Throwable cause) {
        super(cause);
    }

    /**  Constructs a new exception with the specified detail message and cause. */
    public CacheArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
