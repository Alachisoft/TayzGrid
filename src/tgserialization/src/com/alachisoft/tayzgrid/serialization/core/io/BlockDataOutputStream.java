/*
 * @(#)BlockDataOutputStream.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.core.io;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.math.BigInteger;

/**
 * Buffered output stream with two modes: in default mode, outputs data in
 * same format as DataOutputStream; in "block data" mode, outputs data
 * bracketed by block data markers (see object serialization specification
 * for details).
 *
 * @version 1.0, September 18, 2008
 */
public class BlockDataOutputStream
        extends OutputStream implements DataOutput
{

    /** maximum data block length */
    private static final int MAX_BLOCK_SIZE = BlockDataConstants.MAX_BLOCK_SIZE;
    /** maximum data block header length */
    private static final int MAX_HEADER_SIZE = BlockDataConstants.MAX_HEADER_SIZE;
    /** (tunable) length of char buffer (for writing strings) */
    private static final int CHAR_BUF_SIZE = BlockDataConstants.CHAR_BUF_SIZE;
    /** buffer for writing general/block data */
    private final byte[] buf = new byte[MAX_BLOCK_SIZE];
    /** buffer for writing block data headers */
    private final byte[] hbuf = new byte[MAX_HEADER_SIZE];
    /** char buffer for fast string writes */
    private final char[] cbuf = new char[CHAR_BUF_SIZE];
    /** current offset into buf */
    private int pos = 0;
    /** underlying output stream */
    private final OutputStream out;
    /** loopback stream (for data writes that span data blocks) */
    private final DataOutputStream dout;
    /** bit encoder/decoder */
    private final EndianBits bitfmt;

    /**
     * Creates new BlockDataOutputStream on top of given underlying stream.
     * Block data mode is turned off by default.
     */
    public BlockDataOutputStream(OutputStream out)
    {
        this.out = out;
        dout = new DataOutputStream(this);
        bitfmt = new LittleEndianBits();
    }

    /**
     * Converts specified span of float values into byte values.
     */
    // REMIND: remove once hotspot inlines Float.floatToIntBits
    private static native void floatsToBytes(float[] src, int srcpos,
            byte[] dst, int dstpos,
            int nfloats);

    /**
     * Converts specified span of double values into byte values.
     */
    // REMIND: remove once hotspot inlines Double.doubleToLongBits
    private static native void doublesToBytes(double[] src, int srcpos,
            byte[] dst, int dstpos,
            int ndoubles);

    /**
     * Returns the current value of the counter <code>written</code>,
     * the number of bytes written to this data output stream so far.
     * If the counter overflows, it will be wrapped to Integer.MAX_VALUE.
     *
     * @return  the value of the <code>written</code> field.
     * @see     java.io.DataOutputStream#written
     */
    public final int size()
    {
        return this.dout.size();
    }

    /* ----------------- generic output stream methods ----------------- */
    /*
     * The following methods are equivalent to their counterparts in
     * OutputStream, except that they partition written data into data
     * blocks when in block data mode.
     */
    public void write(int b) throws IOException
    {
        if (pos >= MAX_BLOCK_SIZE)
        {
            drain();
        }
        buf[pos++] = (byte) b;
    }

    public void write(byte[] b) throws IOException
    {
        write(b, 0, b.length);
    }

    /**
     * Writes specified span of byte values from given array.  If copy is
     * true, copies the values to an intermediate buffer before writing
     * them to underlying stream (to avoid exposing a reference to the
     * original byte array).
     */
    public void write(byte[] b, int off, int len) throws IOException
    {
        //writeLen(len);
        while (len > 0)
        {
            if (pos >= MAX_BLOCK_SIZE)
            {
                drain();
            }
            if (len >= MAX_BLOCK_SIZE && pos == 0)
            {
                // avoid unnecessary copy
                out.write(b, off, MAX_BLOCK_SIZE);
                off += MAX_BLOCK_SIZE;
                len -= MAX_BLOCK_SIZE;
            }
            else
            {
                int wlen = Math.min(len, MAX_BLOCK_SIZE - pos);
                System.arraycopy(b, off, buf, pos, wlen);
                pos += wlen;
                off += wlen;
                len -= wlen;
            }
        }
    }

    public void flush() throws IOException
    {
        drain();
        out.flush();
    }

    public void close() throws IOException
    {
        flush();
        out.close();
    }

    /**
     * Writes all buffered data from this stream to the underlying stream,
     * but does not flush underlying stream.
     */
    void drain() throws IOException
    {
        if (pos == 0)
        {
            return;
        }
        out.write(buf, 0, pos);
//        if(out instanceof ByteArrayOutputStream)
//            if(((ByteArrayOutputStream)out).size() == MAX_BLOCK_SIZE)
//                ((ByteArrayOutputStream)out).flush();
        pos = 0;
    }

    /**
     * Writes block data header.  Data blocks shorter than 256 bytes are
     * prefixed with a 2-byte header; all others start with a 5-byte
     * header.
     */
    private void writeLen(int len) throws IOException
    {
        bitfmt.putInt(hbuf, 0, len);
        out.write(hbuf, 0, 4);
    }


    /* ----------------- primitive data output methods ----------------- */
    /*
     * The following methods are equivalent to their counterparts in
     * DataOutputStream, except that they partition written data into data
     * blocks when in block data mode.
     */
    public void writeBoolean(boolean v) throws IOException
    {
        if (pos >= MAX_BLOCK_SIZE)
        {
            drain();
        }
        bitfmt.putBoolean(buf, pos++, v);
    }

    public void writeByte(int v) throws IOException
    {
        if (pos >= MAX_BLOCK_SIZE)
        {
            drain();
        }
        buf[pos++] = (byte) v;
    }

    public void writeChar(int v) throws IOException
    {
        if (pos + 2 <= MAX_BLOCK_SIZE)
        {
            bitfmt.putChar(buf, pos, (char) v);
            pos += 2;
        }
        else
        {
            drain();
            this.writeChar(v);
//            dout.writeChar(v);
        }
    }

    public void writeShort(int v) throws IOException
    {
        if (pos + 2 <= MAX_BLOCK_SIZE)
        {
            bitfmt.putShort(buf, pos, (short) v);
            pos += 2;
        }
        else
        {
            drain();
            this.writeShort(v);
//            dout.writeShort(v);
        }
    }

    public void writeInt(int v) throws IOException
    {
        if (pos + 4 <= MAX_BLOCK_SIZE)
        {
            bitfmt.putInt(buf, pos, v);
            pos += 4;
        }
        else
        {
            drain();
            this.writeInt(v);
//            dout.writeInt(v);
        }
    }

    public void writeFloat(float v) throws IOException
    {
        if (pos + 4 <= MAX_BLOCK_SIZE)
        {
            bitfmt.putFloat(buf, pos, v);
            pos += 4;
        }
        else
        {
            drain();
            this.writeFloat(v);
//            dout.writeFloat(v);
        }
    }

    public void writeLong(long v) throws IOException
    {
        if (pos + 8 <= MAX_BLOCK_SIZE)
        {
            bitfmt.putLong(buf, pos, v);
            pos += 8;
        }
        else
        {
            drain();
            this.writeLong(v);
//            dout.writeLong(v);
        }
    }

    public void writeDouble(double v) throws IOException
    {
        if (pos + 8 <= MAX_BLOCK_SIZE)
        {
            bitfmt.putDouble(buf, pos, v);
            pos += 8;
        }
        else
        {
            drain();
            this.writeDouble(v);
//            dout.writeDouble(v);
        }
    }

    public void writeBytes(String s) throws IOException
    {
        int endoff = s.length();
        int cpos = 0;
        int csize = 0;
        for (int off = 0; off < endoff;)
        {
            if (cpos >= csize)
            {
                cpos = 0;
                csize = Math.min(endoff - off, CHAR_BUF_SIZE);
                s.getChars(off, off + csize, cbuf, 0);
            }
            if (pos >= MAX_BLOCK_SIZE)
            {
                drain();
            }
            int n = Math.min(csize - cpos, MAX_BLOCK_SIZE - pos);
            int stop = pos + n;
            while (pos < stop)
            {
                buf[pos++] = (byte) cbuf[cpos++];
            }
            off += n;
        }
    }

    public void writeChars(String s) throws IOException
    {
        int endoff = s.length();
        for (int off = 0; off < endoff;)
        {
            int csize = Math.min(endoff - off, CHAR_BUF_SIZE);
            s.getChars(off, off + csize, cbuf, 0);
            writeChars(cbuf, 0, csize);
            off += csize;
        }
    }

    public void writeUTF(String s) throws IOException
    {
        if(s == null)
        {
            writeInt(0);
            return;
        }
        writeUTF(s, getUTFLength(s));
    }


    /* -------------- primitive data array output methods -------------- */
    /*
     * The following methods write out spans of primitive data values.
     * Though equivalent to calling the corresponding primitive write
     * methods repeatedly, these methods are optimized for writing groups
     * of primitive data values more efficiently.
     */
    public void writeBooleans(boolean[] v, int off, int len) throws IOException
    {
        int endoff = off + len;
        while (off < endoff)
        {
            if (pos >= MAX_BLOCK_SIZE)
            {
                drain();
            }
            int stop = Math.min(endoff, off + (MAX_BLOCK_SIZE - pos));
            while (off < stop)
            {
                bitfmt.putBoolean(buf, pos++, v[off++]);
            }
        }
    }

    public void writeChars(char[] v, int off, int len) throws IOException
    {
        int limit = MAX_BLOCK_SIZE - 2;
        int endoff = off + len;
        while (off < endoff)
        {
            if (pos <= limit)
            {
                int avail = (MAX_BLOCK_SIZE - pos) >> 1;
                int stop = Math.min(endoff, off + avail);
                while (off < stop)
                {
                    bitfmt.putChar(buf, pos, v[off++]);
                    pos += 2;
                }
            }
            else
            {
                drain();
//                dout.writeChar(v[off++]);
            }
        }
    }

    public void writeShorts(short[] v, int off, int len) throws IOException
    {
        int limit = MAX_BLOCK_SIZE - 2;
        int endoff = off + len;
        while (off < endoff)
        {
            if (pos <= limit)
            {
                int avail = (MAX_BLOCK_SIZE - pos) >> 1;
                int stop = Math.min(endoff, off + avail);
                while (off < stop)
                {
                    bitfmt.putShort(buf, pos, v[off++]);
                    pos += 2;
                }
            }
            else
            {
                drain();
//                this.writeShort(v[off++]);
//                dout.writeShort(v[off++]);
            }
        }
    }

    public void writeInts(int[] v, int off, int len) throws IOException
    {
        int limit = MAX_BLOCK_SIZE - 4;
        int endoff = off + len;
        while (off < endoff)
        {
            if (pos <= limit)
            {
                int avail = (MAX_BLOCK_SIZE - pos) >> 2;
                int stop = Math.min(endoff, off + avail);
                while (off < stop)
                {
                    bitfmt.putInt(buf, pos, v[off++]);
                    pos += 4;
                }
            }
            else
            {
                drain();
//                this.writeInt(v[off++]);
//                dout.writeInt(v[off++]);
            }
        }
    }

    public void writeFloats(float[] v, int off, int len) throws IOException
    {
        int limit = MAX_BLOCK_SIZE - 4;
        int endoff = off + len;
        while (off < endoff)
        {
            if (pos <= limit)
            {
                int avail = (MAX_BLOCK_SIZE - pos) >> 2;
                int stop = Math.min(endoff, off + avail);
                bitfmt.putFloat(buf, pos, v[off++]);
                pos += 4;
//                int chunklen = Math.min(endoff - off, avail);
//                floatsToBytes(v, off, buf, pos, chunklen);
//                off += chunklen;
//                pos += chunklen << 2;
            }
            else
            {
                drain();
//                this.writeFloat(v[off++]);
//                dout.writeFloat(v[off++]);
            }
        }
    }

    public void writeLongs(long[] v, int off, int len) throws IOException
    {
        int limit = MAX_BLOCK_SIZE - 8;
        int endoff = off + len;
        while (off < endoff)
        {
            if (pos <= limit)
            {
                int avail = (MAX_BLOCK_SIZE - pos) >> 3;
                int stop = Math.min(endoff, off + avail);
                while (off < stop)
                {
                    bitfmt.putLong(buf, pos, v[off++]);
                    pos += 8;
                }
            }
            else
            {
                drain();
//                this.writeLong(v[off++]);
//                dout.writeLong(v[off++]);
            }
        }
    }

    public void writeDoubles(double[] v, int off, int len) throws IOException
    {
        int limit = MAX_BLOCK_SIZE - 8;
        int endoff = off + len;
        while (off < endoff)
        {
            if (pos <= limit)
            {
                int avail = (MAX_BLOCK_SIZE - pos) >> 3;
                int stop = Math.min(endoff, off + avail);
                while (off < stop)
                {
                    bitfmt.putDouble(buf, pos, v[off++]);
                    pos += 8;
                }
//                int chunklen = Math.min(endoff - off, avail);
//                doublesToBytes(v, off, buf, pos, chunklen);
//                off += chunklen;
//                pos += chunklen << 3;
            }
            else
            {
                drain();
//                this.writeDouble(v[off++]);
//                dout.writeDouble(v[off++]);
            }
        }
    }

    /**
     * Returns the length in bytes of the UTF encoding of the given string.
     */
    int getUTFLength(String s)
    {
        int len = s.length();
        int utflen = 0;
        for (int off = 0; off < len;)
        {
            int csize = Math.min(len - off, CHAR_BUF_SIZE);
            s.getChars(off, off + csize, cbuf, 0);
            for (int cpos = 0; cpos < csize; cpos++)
            {
                char c = cbuf[cpos];
                if (c >= 0x0001 && c <= 0x007F)
                {
                    utflen++;
                }
                else if (c > 0x07FF)
                {
                    utflen += 3;
                }
                else
                {
                    utflen += 2;
                }
            }
            off += csize;
        }
        return utflen;
    }

    /**
     * Writes the given string in UTF format.  This method is used in
     * situations where the UTF encoding length of the string is already
     * known; specifying it explicitly avoids a prescan of the string to
     * determine its UTF length.
     */
    void writeUTF(String s, int utflen) throws IOException
    {
        if (utflen > 0xFFFFL)
        {
            throw new UTFDataFormatException();
        }

        writeInt(utflen);
//        write7BitEncodedInt(utflen);
        //writeShort((int) utflen);
        if (utflen == (long) s.length())
        {
            writeBytes(s);
        }
        else
        {
            writeUTFBody(s);
        }
    }

    private void write7BitEncodedInt(int value) throws IOException
    {
        while (value >= 0x80)
        {
            this.writeByte((byte) value | 0x80);
            value = value >> 7;
        }
        this.writeByte((byte) value);
    }




    /**
     * Writes given string in "long" UTF format.  "Long" UTF format is
     * identical to standard UTF, except that it uses an 8 byte header
     * (instead of the standard 2 bytes) to convey the UTF encoding length.
     */
    public void writeLongUTF(String s) throws IOException
    {
        writeLongUTF(s, getUTFLength(s));
    }

    /**
     * Writes given string in "long" UTF format, where the UTF encoding
     * length of the string is already known.
     */
    void writeLongUTF(String s, long utflen) throws IOException
    {
        writeLong(utflen);
        if (utflen == (long) s.length())
        {
            writeBytes(s);
        }
        else
        {
            writeUTFBody(s);
        }
    }

    /**
     * Writes the "body" (i.e., the UTF representation minus the 2-byte or
     * 8-byte length header) of the UTF encoding for the given string.
     */
    private void writeUTFBody(String s) throws IOException
    {
        int limit = MAX_BLOCK_SIZE - 3;
        int len = s.length();
        for (int off = 0; off < len;)
        {
            int csize = Math.min(len - off, CHAR_BUF_SIZE);
            s.getChars(off, off + csize, cbuf, 0);
            for (int cpos = 0; cpos < csize; cpos++)
            {
                char c = cbuf[cpos];
                if (pos <= limit)
                {
                    if (c <= 0x007F && c != 0)
                    {
                        buf[pos++] = (byte) c;
                    }
                    else if (c > 0x07FF)
                    {
                        buf[pos + 2] = (byte) (0x80 | ((c >> 0) & 0x3F));
                        buf[pos + 1] = (byte) (0x80 | ((c >> 6) & 0x3F));
                        buf[pos + 0] = (byte) (0xE0 | ((c >> 12) & 0x0F));
                        pos += 3;
                    }
                    else
                    {
                        buf[pos + 1] = (byte) (0x80 | ((c >> 0) & 0x3F));
                        buf[pos + 0] = (byte) (0xC0 | ((c >> 6) & 0x1F));
                        pos += 2;
                    }
                }
                else
                { 	// write one byte at a time to normalize block
                    if (c <= 0x007F && c != 0)
                    {
                        write(c);
                    }
                    else if (c > 0x07FF)
                    {
                        write(0xE0 | ((c >> 12) & 0x0F));
                        write(0x80 | ((c >> 6) & 0x3F));
                        write(0x80 | ((c >> 0) & 0x3F));
                    }
                    else
                    {
                        write(0xC0 | ((c >> 6) & 0x1F));
                        write(0x80 | ((c >> 0) & 0x3F));
                    }
                }
            }
            off += csize;
        }
    }

    public final void writeUInt16(int i) throws IOException
    {
        byte[] ret = new byte[2];
        ret[0] = (byte) (i & 0xff);
        ret[1] = (byte) ((i >> 8) & 0xff);
        this.write(ret);
    }

    public final void writeUInt32(long i) throws IOException
    {
        byte[] ret = new byte[4];
        ret[0] = (byte) (i & 0xff);
        ret[1] = (byte) ((i >> 8) & 0xff);
        ret[2] = (byte) ((i >> 16) & 0xff);
        ret[3] = (byte) ((i >> 24) & 0xff);
        this.write(ret);
    }

    public final void writeUInt64(BigInteger bigInteger) throws IOException
    {
        byte[] bytes = bigInteger.toByteArray();
        byte[] tmp = new byte[1];
        int j = bytes.length-1,i = 0;
        while (j > i)
        {
            tmp[0] = bytes[j];
            bytes[j] = bytes[i];
            bytes[i] = tmp[0];
            j--;
            i++;
        }

        tmp = new byte[8];
        int count = bytes.length;
        count = count > 8 ? 8 : count;
        for (int k = 0; k < count; k++)
        {
            tmp[k] = (byte) (bytes[k] & 0xff);
        }

        this.write(tmp);
    }

    public final void writeUByte(short sh) throws IOException
    {
        this.writeByte((int)(0x000000FF & sh));
    }
}
