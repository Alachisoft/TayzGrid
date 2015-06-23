/*
 * @(#)BlockDataInputStream.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.core.io;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.io.UTFDataFormatException;
import java.math.BigInteger;

/**
 * Input stream with two modes: in default mode, inputs data written in the same format as DataOutputStream; in "block data" mode, inputs data bracketed by block data markers (see
 * object serialization specification for details). Buffering depends on block data mode: when in default mode, no data is buffered in advance; when in block data mode, all data
 * for the current data block is read in at once (and buffered).
 *
 * @version 1.0, September 18, 2008
 */
public class BlockDataInputStream
        extends InputStream implements DataInput
{

    /**
     * maximum data block length
     */
    private static final int MAX_BLOCK_SIZE = BlockDataConstants.MAX_BLOCK_SIZE;
    /**
     * maximum data block header length
     */
    private static final int MAX_HEADER_SIZE = BlockDataConstants.MAX_HEADER_SIZE;
    /**
     * (tunable) length of char buffer (for reading strings)
     */
    private static final int CHAR_BUF_SIZE = BlockDataConstants.CHAR_BUF_SIZE;
    /**
     * readBlockHeader() return value indicating header read may block
     */
    private static final int HEADER_BLOCKED = -2;
    /**
     * buffer for reading general/block data
     */
    private final byte[] buf = new byte[MAX_BLOCK_SIZE];
    /**
     * buffer for reading block data headers
     */
    private final byte[] hbuf = new byte[MAX_HEADER_SIZE];
    /**
     * char buffer for fast string reads
     */
    private final char[] cbuf = new char[CHAR_BUF_SIZE];
    // block data state fields; values meaningful only when blkmode true
    /**
     * current offset into buf
     */
    private int pos = 0;
    /**
     * end offset of valid data in buf, or -1 if no more block data
     */
    private int end = -1;
    /**
     * number of bytes in current block yet to be read from stream
     */
    private int unread = 0;
    /**
     * flag set when at end of field value block with no TC_ENDBLOCKDATA
     */
    private boolean defaultDataEnd = false;
    /**
     * underlying stream (wrapped in peekable filter stream)
     */
    private final DataInputStream in;
    /**
     * loopback stream (for data reads that span data blocks)
     */
    private final DataInputStream din;
    /**
     * bit encoder/decoder
     */
    private final EndianBits bitfmt;

    /**
     * Creates new BlockDataInputStream on top of given underlying stream. Block data mode is turned off by default.
     */
    public BlockDataInputStream(InputStream in)
    {
        this.in = new DataInputStream(in);
        din = new DataInputStream(this);
        bitfmt = new LittleEndianBits();
        pos = 0;
        end = 0;
        unread = 0;

        try
        {
            refill();
        }
        catch (IOException iOException)
        {
        }
    }

    /**
     * Converts specified span of bytes into float values.
     */
    // REMIND: remove once hotspot inlines Float.intBitsToFloat
    private static native void bytesToFloats(byte[] src, int srcpos,
            float[] dst, int dstpos,
            int nfloats);

    /**
     * Converts specified span of bytes into double values.
     */
    // REMIND: remove once hotspot inlines Double.longBitsToDouble
    private static native void bytesToDoubles(byte[] src, int srcpos,
            double[] dst, int dstpos,
            int ndoubles);

    /**
     * Attempts to read in the next block data header (if any). If canBlock is false and a full header cannot be read without possibly blocking, returns HEADER_BLOCKED, else if the
     * next element in the stream is a block data header, returns the block data length specified by the header, else returns -1.
     */
    private int readLen(boolean canBlock) throws IOException
    {
        if (defaultDataEnd)
        {
            /*
             * Fix for 4360508: stream is currently at the end of a field
             * value block written via default serialization; since there
             * is no terminating TC_ENDBLOCKDATA tag, simulate
             * end-of-custom-data behavior explicitly.
             */
            return -1;
        }
        try
        {
            for (;;)
            {
                int avail = canBlock ? Integer.MAX_VALUE : in.available();
                if (avail < 4)
                {
                    return HEADER_BLOCKED;
                }

                in.readFully(hbuf, 0, 4);
                int len = bitfmt.getInt(hbuf, 0);
                if (len < 0)
                {
                    throw new StreamCorruptedException(
                            "illegal block data header length: " + len);
                }
                return len;
            }
        }
        catch (EOFException ex)
        {
            throw new StreamCorruptedException(
                    "unexpected EOF while reading block data header");
        }
    }

    /**
     * Refills internal buffer buf with block data. Any data in buf at the time of the call is considered consumed. Sets the pos, end, and unread fields to reflect the new amount
     * of available block data; if the next element in the stream is not a data block, sets pos and unread to 0 and end to -1.
     */
    private void refill() throws IOException
    {
        try
        {
            do
            {
                pos = 0;
                if (unread > 0)
                {
                    int n =
                            in.read(buf, 0, Math.min(unread, MAX_BLOCK_SIZE));
                    if (n >= 0)
                    {
                        end = n;
                        unread = n;
                    }
                    else
                    {
                        throw new StreamCorruptedException("unexpected EOF in middle of data block");
                    }
                }
                else
                {
                    unread = MAX_BLOCK_SIZE;
                    end = 0;
                }
            }
            while (pos == end);
        }
        catch (IOException ex)
        {
            pos = 0;
            end = -1;
            unread = 0;
            throw ex;
        }
    }

    /* ----------------- generic input stream methods ------------------ */
    /*
     * The following methods are equivalent to their counterparts in
     * InputStream, except that they interpret data block boundaries and
     * read the requested data from within data blocks when in block data
     * mode.
     */
    public int read() throws IOException
    {
        if (pos == end)
        {
            refill();
        }
        return (end >= 0) ? (buf[pos++] & 0xFF) : -1;
    }

    public int read(byte[] b, int off, int len) throws IOException
    {
        return read(b, off, len, false);
    }

    public long skip(long len) throws IOException
    {
        long remain = len;
        while (remain > 0)
        {
            if (pos == end)
            {
                refill();
            }
            if (end < 0)
            {
                break;
            }
            int nread = (int) Math.min(remain, end - pos);
            remain -= nread;
            pos += nread;
        }
        return len - remain;
    }

    public int available() throws IOException
    {
        if ((pos == end) && (unread == 0))
        {
            int n;
            while ((n = readLen(false)) == 0);
            switch (n)
            {
                case HEADER_BLOCKED:
                    break;

                case -1:
                    pos = 0;
                    end = -1;
                    break;

                default:
                    pos = 0;
                    end = 0;
                    unread = n;
                    break;
            }
        }
        // avoid unnecessary call to in.available() if possible
        int unreadAvail = (unread > 0)
                ? Math.min(in.available(), unread) : 0;
        return (end >= 0) ? (end - pos) + unreadAvail : 0;
    }

    public void close() throws IOException
    {
        pos = 0;
        end = -1;
        unread = 0;
        in.close();
    }

    /**
     * Attempts to read len bytes into byte array b at offset off. Returns the number of bytes read, or -1 if the end of stream/block data has been reached. If copy is true, reads
     * values into an intermediate buffer before copying them to b (to avoid exposing a reference to b).
     */
    int read(byte[] b, int off, int len, boolean copy) throws IOException
    {
        if (len == 0)
        {
            return 0;
        }
        else if (!copy)
        {
            if (pos == end)
            {
                refill();
            }
            if (end < 0)
            {
                return -1;
            }
            int nread = Math.min(len, end - pos);
            System.arraycopy(buf, pos, b, off, nread);
            pos += nread;
            return nread;
        }
        else
        {
            int nread = in.read(buf, 0, Math.min(len, MAX_BLOCK_SIZE));
            if (nread > 0)
            {
                System.arraycopy(buf, 0, b, off, nread);
            }
            return nread;
        }
    }

    //<editor-fold defaultstate="collapsed" desc="generic input stream methods">
    /* ----------------- primitive data input methods ------------------ */
    /*
     * The following methods are equivalent to their counterparts in
     * DataInputStream, except that they interpret data block boundaries
     * and read the requested data from within data blocks when in block
     * data mode.
     */
    public void readFully(byte[] b) throws IOException
    {
        readFully(b, 0, b.length, false);
    }

    public void readFully(byte[] b, int off, int len) throws IOException
    {
        readFully(b, off, len, false);
    }

    public void readFully(byte[] b, int off, int len, boolean copy)
            throws IOException
    {
        while (len > 0)
        {
            int n = read(b, off, len, copy);
            if (n < 0)
            {
                throw new EOFException();
            }
            off += n;
            len -= n;
        }
    }

    public int skipBytes(int n) throws IOException
    {
        return din.skipBytes(n);
    }

    public boolean readBoolean() throws IOException
    {
        int v = read();
        if (v < 0)
        {
            throw new EOFException();
        }
        return (v != 0);
    }

    public byte readByte() throws IOException
    {
        int v = read();
        if (v < 0)
        {
            throw new EOFException();
        }
        return (byte) v;
    }

    public int readUnsignedByte() throws IOException
    {
        int v = read();
        if (v < 0)
        {
            throw new EOFException();
        }
        return v;
    }

    public char readChar() throws IOException
    {
        if (end - pos < 2)
        {
            if (end - pos != 0)
            {
                byte[] biteBuff = new byte[2];
                biteBuff[0] = buf[pos];
                refill();
                biteBuff[1] = this.readByte();
                char v = bitfmt.getChar(biteBuff, 0);
                return v;
            }
            //end - pos if Zero then simply refill
            refill();
            char v = bitfmt.getChar(buf, pos);
            pos += 2;
            return v;
//            return din.readChar();
        }

        char v = bitfmt.getChar(buf, pos);
        pos += 2;
        return v;
    }

    public short readShort() throws IOException
    {
        if (end - pos < 2)
        {
            if (end - pos != 0)
            {
                byte[] biteBuff = new byte[2];
                biteBuff[0] = buf[pos];
                refill();
                biteBuff[1] = this.readByte();
                short v = bitfmt.getShort(biteBuff, 0);
                return v;
            }


            refill();
            short v = bitfmt.getShort(buf, pos);
            pos += 2;
            return v;
//            return din.readShort();
        }

        short v = bitfmt.getShort(buf, pos);
        pos += 2;
        return v;
    }

    public int readUnsignedShort() throws IOException
    {
        if (end - pos < 2)
        {

            if (end - pos != 0)
            {
                byte[] biteBuff = new byte[2];
                biteBuff[0] = buf[pos];
                refill();
                biteBuff[1] = this.readByte();
                int v = bitfmt.getShort(biteBuff, 0) & 0xFFFF;
                return v;
            }

            refill();
            int v = bitfmt.getShort(buf, pos) & 0xFFFF;
            pos += 2;
            return v;
//            return din.readUnsignedShort();
        }

        int v = bitfmt.getShort(buf, pos) & 0xFFFF;
        pos += 2;
        return v;
    }

    public int readInt() throws IOException
    {
        if (end - pos < 4)
        {

            if (end - pos != 0)
            {
                byte[] biteBuff = new byte[4];
                int toBePurged = end - pos;
                for (int i = 0; i < toBePurged; i++)
                {
                    biteBuff[i] = buf[pos + i];
                }
                refill();
                for (int i = toBePurged; i < 4; i++)
                {
                    biteBuff[i] = this.readByte();
                }

                int v = bitfmt.getInt(biteBuff, 0);
                return v;
            }

            refill();
            int v = bitfmt.getInt(buf, pos);
            pos += 4;
            return v;
//            return din.readInt();
        }

        int v = bitfmt.getInt(buf, pos);
        pos += 4;
        return v;
    }

    public float readFloat() throws IOException
    {
        if (end - pos < 4)
        {
            if (end - pos != 0)
            {
                byte[] biteBuff = new byte[4];
                int toBePurged = end - pos;
                for (int i = 0; i < toBePurged; i++)
                {
                    biteBuff[i] = buf[pos + i];
                }
                refill();
                for (int i = toBePurged; i < 4; i++)
                {
                    biteBuff[i] = this.readByte();
                }

                float v = bitfmt.getFloat(biteBuff, 0);
                return v;
            }

            refill();
            float v = bitfmt.getFloat(buf, pos);
            pos += 4;
            return v;
//            return din.readFloat();
        }

        float v = bitfmt.getFloat(buf, pos);
        pos += 4;
        return v;
    }

    public long readLong() throws IOException
    {
        if (end - pos < 8)
        {

            if (end - pos != 0)
            {
                byte[] biteBuff = new byte[8];
                int toBePurged = end - pos;
                for (int i = 0; i < toBePurged; i++)
                {
                    biteBuff[i] = buf[pos + i];
                }
                refill();
                for (int i = toBePurged; i < 8; i++)
                {
                    biteBuff[i] = this.readByte();
                }

                long v = bitfmt.getLong(biteBuff, 0);
                return v;
            }

            refill();
            long v = bitfmt.getLong(buf, pos);
            pos += 8;
            return v;
//            return din.readLong();
        }

        long v = bitfmt.getLong(buf, pos);
        pos += 8;
        return v;
    }

    public double readDouble() throws IOException
    {
        if (end - pos < 8)
        {

            if (end - pos != 0)
            {
                byte[] biteBuff = new byte[8];
                int toBePurged = end - pos;
                for (int i = 0; i < toBePurged; i++)
                {
                    biteBuff[i] = buf[pos + i];
                }
                refill();
                for (int i = toBePurged; i < 8; i++)
                {
                    biteBuff[i] = this.readByte();
                }

                double v = bitfmt.getDouble(biteBuff, 0);
                return v;
            }

            refill();
            double v = bitfmt.getDouble(buf, pos);
            pos += 8;
            return v;
//            return din.readDouble();
        }

        double v = bitfmt.getDouble(buf, pos);
        pos += 8;
        return v;
    }

    public String readUTF() throws IOException
    {
        return readUTFBody(readInt());
    }

    public int readUInt16() throws IOException
    {
        byte[] bytes = new byte[2];
        this.read(bytes);
        int value = 0;
        value = (((bytes[0] & 0xFF) << (8 * 0))
                | ((bytes[1] & 0xFF) << (8 * 1)) & 0xFFFF);
        return value;
    }

    public long readUInt32() throws IOException
    {
        byte[] bytes = new byte[4];
        this.read(bytes);
        long value = 0;
        value = (((bytes[0] & 0xFF) << (8 * 0))
                | ((bytes[1] & 0xFF) << (8 * 1))
                | ((bytes[2] & 0xFF) << (8 * 2))
                | ((bytes[3] & 0xFF) << (8 * 3))) & 0xFFFFFFFFL;
        return value;
    }

    public BigInteger readUInt64() throws IOException
    {
        byte[] bytes = new byte[8];
        this.read(bytes);
        byte tmp;
        int j = bytes.length - 1, i = 0;
        while (j > i)
        {
            tmp = bytes[j];
            bytes[j] = bytes[i];
            bytes[i] = tmp;
            j--;
            i++;
        }
        return new BigInteger(1, bytes);
    }

    public String readLine() throws IOException
    {
        return din.readLine();	// deprecated, not worth optimizing
    }

    public short readUByte() throws IOException
    {
        return (short) (0x000000FF & ((int) this.readUnsignedByte()));
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="primitive data array input methods">
    /* -------------- primitive data array input methods --------------- */
    /*
     * The following methods read in spans of primitive data values.
     * Though equivalent to calling the corresponding primitive read
     * methods repeatedly, these methods are optimized for reading groups
     * of primitive data values more efficiently.
     */
    public void readBooleans(boolean[] v, int off, int len) throws IOException
    {
        int stop, endoff = off + len;
        while (off < endoff)
        {
            if (end - pos < 1)
            {
                //no need to read ToBePurged bytes since pos == end
                refill();
                v[off++] = bitfmt.getBoolean(buf, pos++);
//                v[off++] = din.readBoolean();
                continue;
            }
            else
            {
                stop = Math.min(endoff, off + end - pos);
            }

            while (off < stop)
            {
                v[off++] = bitfmt.getBoolean(buf, pos++);
            }
        }
    }

    public void readChars(char[] v, int off, int len) throws IOException
    {
        int stop, endoff = off + len;
        while (off < endoff)
        {
            if (end - pos < 2)
            {
                if (end - pos != 0)
                {
                    byte[] biteBuff = new byte[2];
                    biteBuff[0] = buf[pos];
                    refill();
                    biteBuff[1] = this.readByte();
                    v[off++] = bitfmt.getChar(biteBuff, 0);
                    continue;
                }
                //end - pos if Zero then simply refill
                refill();
                v[off++] = bitfmt.getChar(buf, pos);
                pos += 2;
                continue;
//            return din.readChar();
            }
            else
            {
                stop = Math.min(endoff, off + ((end - pos)));
                //stop = Math.min(endoff, off + ((end - pos) >> 1));
            }

            while (off < stop)
            {
                v[off++] = bitfmt.getChar(buf, pos);
                pos += 2;
            }
        }
    }

    public void readShorts(short[] v, int off, int len) throws IOException
    {
        int stop, endoff = off + len;
        while (off < endoff)
        {
            if (end - pos < 2)
            {
                if (end - pos != 0)
                {
                    byte[] biteBuff = new byte[2];
                    biteBuff[0] = buf[pos];
                    refill();
                    biteBuff[1] = this.readByte();
                    v[off++] = bitfmt.getShort(biteBuff, 0);
                    continue;
                }

                refill();
                v[off++] = bitfmt.getShort(buf, pos);
                pos += 2;
//                v[off++] = din.readShort();
                continue;
            }
            else
            {
                stop = Math.min(endoff, off + ((end - pos) >> 1));
            }

            while (off < stop)
            {
                v[off++] = bitfmt.getShort(buf, pos);
                pos += 2;
            }
        }
    }

    public void readInts(int[] v, int off, int len) throws IOException
    {
        int stop, endoff = off + len;
        while (off < endoff)
        {
            if (end - pos < 4)
            {

                if (end - pos != 0)
                {
                    byte[] biteBuff = new byte[4];
                    int toBePurged = end - pos;
                    for (int i = 0; i < toBePurged; i++)
                    {
                        biteBuff[i] = buf[pos + i];
                    }
                    refill();
                    for (int i = toBePurged; i < 4; i++)
                    {
                        biteBuff[i] = this.readByte();
                    }

                    v[off++] = bitfmt.getInt(biteBuff, 0);
                    continue;
                }

                refill();
                v[off++] = bitfmt.getInt(buf, pos);
                pos += 4;
//                v[off++] = din.readInt();
                continue;
            }
            else
            {
                stop = Math.min(endoff, off + ((end - pos) >> 2));
            }

            while (off < stop)
            {
                v[off++] = bitfmt.getInt(buf, pos);
                pos += 4;
            }
        }
    }

    public void readFloats(float[] v, int off, int len) throws IOException
    {
        int span, endoff = off + len;
        int stop;
        while (off < endoff)
        {
            if (end - pos < 4)
            {

                if (end - pos != 0)
                {
                    byte[] biteBuff = new byte[4];
                    int toBePurged = end - pos;
                    for (int i = 0; i < toBePurged; i++)
                    {
                        biteBuff[i] = buf[pos + i];
                    }
                    refill();
                    for (int i = toBePurged; i < 4; i++)
                    {
                        biteBuff[i] = this.readByte();
                    }

                    v[off++] = bitfmt.getFloat(biteBuff, 0);
                    continue;
                }

                refill();
                v[off++] = bitfmt.getFloat(buf, pos);
                pos += 4;
//                v[off++] = din.readFloat();
                continue;
            }
            else
            {
                stop = Math.min(endoff, off + ((end - pos) >> 2));
                //                span = Math.min(endoff - off, ((end - pos) >> 2));
            }

            while (off < stop)
            {
                v[off++] = bitfmt.getFloat(buf, pos);
                pos += 4;
            }
            //            bytesToFloats(buf, pos, v, off, span);
            //            off += span;
            //            pos += span << 2;
        }
    }

    public void readLongs(long[] v, int off, int len) throws IOException
    {
        int stop, endoff = off + len;
        while (off < endoff)
        {
            if (end - pos < 8)
            {

                if (end - pos != 0)
                {
                    byte[] biteBuff = new byte[8];
                    int toBePurged = end - pos;
                    for (int i = 0; i < toBePurged; i++)
                    {
                        biteBuff[i] = buf[pos + i];
                    }
                    refill();
                    for (int i = toBePurged; i < 8; i++)
                    {
                        biteBuff[i] = this.readByte();
                    }

                    v[off++] = bitfmt.getLong(biteBuff, 0);
                    continue;
                }

                refill();
                v[off++] = bitfmt.getLong(buf, pos);
                pos += 8;
//                v[off++] = din.readLong();
                continue;
            }
            else
            {
                stop = Math.min(endoff, off + ((end - pos) >> 3));
            }

            while (off < stop)
            {
                v[off++] = bitfmt.getLong(buf, pos);
                pos += 8;
            }
        }
    }

    public void readDoubles(double[] v, int off, int len) throws IOException
    {
        int span, endoff = off + len;
        int stop;
        while (off < endoff)
        {
            if (end - pos < 8)
            {
                if (end - pos != 0)
                {
                    byte[] biteBuff = new byte[8];
                    int toBePurged = end - pos;
                    for (int i = 0; i < toBePurged; i++)
                    {
                        biteBuff[i] = buf[pos + i];
                    }
                    refill();
                    for (int i = toBePurged; i < 8; i++)
                    {
                        biteBuff[i] = this.readByte();
                    }

                    v[off++] = bitfmt.getDouble(biteBuff, 0);
                    continue;
                }

                refill();
                v[off++] = bitfmt.getDouble(buf, pos);
                pos += 8;
//                v[off++] = din.readDouble();
                continue;
            }
            else
            {
                stop = Math.min(endoff, off + ((end - pos) >> 3));
                //                span = Math.min(endoff - off, ((end - pos) >> 3));
            }

            while (off < stop)
            {
                v[off++] = bitfmt.getDouble(buf, pos);
                pos += 8;
            }

            //            bytesToDoubles(buf, pos, v, off, span);
            //            off += span;
            //            pos += span << 3;
        }
    }

    /**
     * Reads in string written in "long" UTF format. "Long" UTF format is identical to standard UTF, except that it uses an 8 byte header (instead of the standard 2 bytes) to
     * convey the UTF encoding length.
     */
    public String readLongUTF() throws IOException
    {
        return readUTFBody((long) readInt());
    }

    /**
     * Reads in the "body" (i.e., the UTF representation minus the 2-byte or 8-byte length header) of a UTF encoding, which occupies the next utflen bytes.
     */
    private String readUTFBody(long utflen) throws IOException
    {
        StringBuilder sbuf = new StringBuilder();
        if (utflen == 0)
        {
            return "";
        }
        while (utflen > 0)
        {
            int avail = end - pos;
            if (avail >= 3 || (long) avail == utflen)
            {
                utflen -= readUTFSpan(sbuf, utflen);
            }
            else
            {
                // near block boundary, read one byte at a time
                utflen -= readUTFChar(sbuf, utflen);
            }
        }

        return sbuf.toString();
    }

    /**
     * Reads span of UTF-encoded characters out of internal buffer (starting at offset pos and ending at or before offset end), consuming no more than utflen bytes. Appends read
     * characters to sbuf. Returns the number of bytes consumed.
     */
    private long readUTFSpan(StringBuilder sbuf, long utflen)
            throws IOException
    {
        int cpos = 0;
        int start = pos;
        int avail = Math.min(end - pos, CHAR_BUF_SIZE);
        // stop short of last char unless all of utf bytes in buffer
        int stop = pos + ((utflen > avail) ? avail - 2 : (int) utflen);
        boolean outOfBounds = false;

        try
        {
            while (pos < stop)
            {
                int b1, b2, b3;
                b1 = buf[pos++] & 0xFF;
                switch (b1 >> 4)
                {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:	  // 1 byte format: 0xxxxxxx
                        cbuf[cpos++] = (char) b1;
                        break;

                    case 12:
                    case 13:  // 2 byte format: 110xxxxx 10xxxxxx
                        b2 = buf[pos++];
                        if ((b2 & 0xC0) != 0x80)
                        {
                            throw new UTFDataFormatException();
                        }
                        cbuf[cpos++] = (char) (((b1 & 0x1F) << 6) | ((b2 & 0x3F) << 0));
                        break;

                    case 14:  // 3 byte format: 1110xxxx 10xxxxxx 10xxxxxx
                        b3 = buf[pos + 1];
                        b2 = buf[pos + 0];
                        pos += 2;
                        if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80)
                        {
                            throw new UTFDataFormatException();
                        }
                        cbuf[cpos++] = (char) (((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | ((b3 & 0x3F) << 0));
                        break;

                    default:  // 10xx xxxx, 1111 xxxx
                        throw new UTFDataFormatException();
                }
            }
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {
            outOfBounds = true;
        }
        finally
        {
            if (outOfBounds || (pos - start) > utflen)
            {
                /*
                 * Fix for 4450867: if a malformed utf char causes the
                 * conversion loop to scan past the expected end of the utf
                 * string, only consume the expected number of utf bytes.
                 */
                pos = start + (int) utflen;
                throw new UTFDataFormatException();
            }
        }

        sbuf.append(cbuf, 0, cpos);
        return pos - start;
    }

    /**
     * Reads in single UTF-encoded character one byte at a time, appends the character to sbuf, and returns the number of bytes consumed. This method is used when reading in UTF
     * strings written in block data mode to handle UTF-encoded characters which (potentially) straddle block-data boundaries.
     */
    private int readUTFChar(StringBuilder sbuf, long utflen)
            throws IOException
    {
        int b1, b2, b3;
        b1 = readByte() & 0xFF;
        switch (b1 >> 4)
        {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:     // 1 byte format: 0xxxxxxx
                sbuf.append((char) b1);
                return 1;

            case 12:
            case 13:    // 2 byte format: 110xxxxx 10xxxxxx
                if (utflen < 2)
                {
                    throw new UTFDataFormatException();
                }
                b2 = readByte();
                if ((b2 & 0xC0) != 0x80)
                {
                    throw new UTFDataFormatException();
                }
                sbuf.append((char) (((b1 & 0x1F) << 6) | ((b2 & 0x3F) << 0)));
                return 2;

            case 14:    // 3 byte format: 1110xxxx 10xxxxxx 10xxxxxx
                if (utflen < 3)
                {
                    if (utflen == 2)
                    {
                        readByte();		// consume remaining byte
                    }
                    throw new UTFDataFormatException();
                }
                b2 = readByte();
                b3 = readByte();
                if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80)
                {
                    throw new UTFDataFormatException();
                }
                sbuf.append((char) (((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | ((b3 & 0x3F) << 0)));
                return 3;

            default:   // 10xx xxxx, 1111 xxxx
                throw new UTFDataFormatException();
        }
    }
    //</editor-fold>
}
