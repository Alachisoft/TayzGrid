/*
 * @(#)BlockDataConstants.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.core.io;

/**
 * BlockDataConstants class.
 *
 * @version 1.0, September 18, 2008
 */
final class BlockDataConstants {
    /** maximum data block length */
    public static final int MAX_BLOCK_SIZE = 2048;
    /** maximum data block header length */
    public static final int MAX_HEADER_SIZE = 4;
    /** (tunable) length of char buffer (for reading strings) */
    public static final int CHAR_BUF_SIZE = 256;
}
