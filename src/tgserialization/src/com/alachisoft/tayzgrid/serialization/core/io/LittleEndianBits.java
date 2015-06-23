/*
 * @(#)LittleEndianBits.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.core.io;

/**
 * Utility methods for packing/unpacking primitive values in/out of byte arrays using little-endian byte ordering.
 *
 * @version 1.0, September 18, 2008
 */
final public class LittleEndianBits implements EndianBits
{

    /*
     * Methods for unpacking primitive values from byte arrays starting at
     * given offsets.
     */
    public boolean getBoolean(byte[] b, int off)
    {
        return b[off] != 0;
    }

    public char getChar(byte[] b, int off)
    {
        return (char) (((b[off + 0] & 0xFF) << 0) + ((b[off + 1]) << 8));
    }

    public short getShort(byte[] b, int off)
    {
        return (short) (((b[off + 0] & 0xFF) << 0) + ((b[off + 1]) << 8));
    }

    public int getInt(byte[] b, int off)
    {
        return ((b[off + 0] & 0xFF) << 0) + ((b[off + 1] & 0xFF) << 8) + ((b[off + 2] & 0xFF) << 16) + ((b[off + 3]) << 24);
    }

    public float getFloat(byte[] b, int off)
    {
        int i = ((b[off + 0] & 0xFF) << 0) + ((b[off + 1] & 0xFF) << 8) + ((b[off + 2] & 0xFF) << 16) + ((b[off + 3]) << 24);
        return Float.intBitsToFloat(i);
    }

    public long getLong(byte[] b, int off)
    {
        return ((b[off + 0] & 0xFFL) << 0) + ((b[off + 1] & 0xFFL) << 8) + ((b[off + 2] & 0xFFL) << 16) + ((b[off + 3] & 0xFFL) << 24) + ((b[off + 4] & 0xFFL) << 32) + ((b[off + 5]
                & 0xFFL) << 40) + ((b[off + 6] & 0xFFL) << 48) + (((long) b[off + 7]) << 56);
    }

    public double getDouble(byte[] b, int off)
    {
        long j = ((b[off + 0] & 0xFFL) << 0) + ((b[off + 1] & 0xFFL) << 8) + ((b[off + 2] & 0xFFL) << 16) + ((b[off + 3] & 0xFFL) << 24) + ((b[off + 4] & 0xFFL) << 32) + ((b[off
                + 5] & 0xFFL) << 40) + ((b[off + 6] & 0xFFL) << 48) + (((long) b[off + 7]) << 56);
        return Double.longBitsToDouble(j);
    }

    /*
     * Methods for packing primitive values into byte arrays starting at given
     * offsets.
     */
    public void putBoolean(byte[] b, int off, boolean val)
    {
        b[off] = (byte) (val ? 1 : 0);
    }

    public void putChar(byte[] b, int off, char val)
    {
	b[off + 0] = (byte) (val >>> 0);
        b[off + 1] = (byte) (val >>> 8);
    }

    public void putShort(byte[] b, int off, short val)
    {
        b[off + 0] = (byte) (val >>> 0);
        b[off + 1] = (byte) (val >>> 8);
    }

    public void putInt(byte[] b, int off, int val)
    {
        b[off + 0] = (byte) (val >>> 0);
        b[off + 1] = (byte) (val >>> 8);
        b[off + 2] = (byte) (val >>> 16);
        b[off + 3] = (byte) (val >>> 24);
    }

    public void putFloat(byte[] b, int off, float val)
    {
        int i = Float.floatToIntBits(val);
        b[off + 0] = (byte) (i >>> 0);
        b[off + 1] = (byte) (i >>> 8);
        b[off + 2] = (byte) (i >>> 16);
        b[off + 3] = (byte) (i >>> 24);
    }

    public void putLong(byte[] b, int off, long val)
    {
        b[off + 0] = (byte) (val >>> 0);
        b[off + 1] = (byte) (val >>> 8);
        b[off + 2] = (byte) (val >>> 16);
        b[off + 3] = (byte) (val >>> 24);
        b[off + 4] = (byte) (val >>> 32);
        b[off + 5] = (byte) (val >>> 40);
        b[off + 6] = (byte) (val >>> 48);
        b[off + 7] = (byte) (val >>> 56);
    }

    public void putDouble(byte[] b, int off, double val)
    {
        long j = Double.doubleToLongBits(val);
        b[off + 0] = (byte) (j >>> 0);
        b[off + 1] = (byte) (j >>> 8);
        b[off + 2] = (byte) (j >>> 16);
        b[off + 3] = (byte) (j >>> 24);
        b[off + 4] = (byte) (j >>> 32);
        b[off + 5] = (byte) (j >>> 40);
        b[off + 6] = (byte) (j >>> 48);
        b[off + 7] = (byte) (j >>> 56);
    }
}
