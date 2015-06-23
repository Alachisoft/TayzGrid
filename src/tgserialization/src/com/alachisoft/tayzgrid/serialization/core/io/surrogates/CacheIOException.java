/*
 * @(#)CacheIOException.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.core.io.surrogates;

/**
 * CacheIOException class.
 *
 * @version 1.0, September 18, 2008
 */
public class CacheIOException extends Exception {
    
    /** Constructs an NxInstantiationException without a detail message. */
    public CacheIOException() {
    }

    /** Constructs an NxInstantiationException with a detail message. */
    public CacheIOException(String message) {
        super(message);
    }

    /** Constructs a new exception with the specified cause. */
    public CacheIOException(Throwable cause) {
        super(cause);
    }

    /**  Constructs a new exception with the specified detail message and cause. */
    public CacheIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
