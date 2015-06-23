/*
 * @(#)CacheInstantiationException.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.core.io.surrogates;

/**
 * CacheInstantiationException class.
 *
 * @version 1.0, September 18, 2008
 */
public class CacheInstantiationException extends Exception {
    
    /** Constructs an NCacheInstantiationException without a detail message. */
    public CacheInstantiationException() {
    }

    /** Constructs an NCacheInstantiationException with a detail message. */
    public CacheInstantiationException(String message) {
        super(message);
    }

    /** Constructs a new exception with the specified cause. */
    public CacheInstantiationException(Throwable cause) {
        super(cause);
    }

    /**  Constructs a new exception with the specified detail message and cause. */
    public CacheInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }
}
