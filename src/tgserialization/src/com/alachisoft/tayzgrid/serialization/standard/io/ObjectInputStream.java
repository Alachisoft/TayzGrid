/*
 * @(#)ObjectInputStream.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */

package com.alachisoft.tayzgrid.serialization.standard.io;

import com.alachisoft.tayzgrid.serialization.core.io.BlockDataInputStream;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.SerializationContext;
import com.alachisoft.tayzgrid.serialization.core.io.TypeSurrogateSelector;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.EOFDotNetSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.EOFJavaSerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ObjectArraySerializationSurrogate;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ObjectInputStream class.
 *
 * @version 1.0, September 18, 2008
 */
public class ObjectInputStream
        extends InputStream implements CacheObjectInput
{
    /**
     * working arrays initialized on demand by readUTF
     */
    private BlockDataInputStream mInput;
    private SerializationContext mContext;
    private TypeSurrogateSelector mSelector;
    public static byte[] buf;
    private String _cacheContext;

    /**
     * Creates an ObjectInputStream that reads from the specified InputStream.
     * A serialization stream header is read from the stream and verified.
     *
     * <p>If a security manager is installed, this constructor will check for
     * the "enableSubclassImplementation" SerializablePermission when invoked
     * directly or indirectly by the constructor of a subclass which overrides
     * the ObjectInputStream.readFields or ObjectInputStream.readUnshared
     * methods.
     *
     * @param	in input stream to read from
     * @throws	StreamCorruptedException if the stream header is incorrect
     * @throws	IOException if an I/O error occurs while reading stream header
     * @throws	SecurityException if untrusted subclass illegally overrides
     * 		security-sensitive methods
     * @throws	NullPointerException if <code>in</code> is <code>null</code>
     */
    public ObjectInputStream(InputStream in, String cacheContext) throws IOException {
//        JarFileLoader jfl=JarLoaderFactory.getLoader(cacheContext);
//        if(jfl!=null)
//        {    
//            Thread.currentThread().setContextClassLoader(jfl);
//        }
        this.mInput = new BlockDataInputStream(in);
        this.mContext = new SerializationContext(TypeSurrogateSelectorImpl.getDefault());
        this.mSelector = this.mContext.getSurrogateSelector();
        this.mContext.putUserItem("__bin", this.mInput);
        this._cacheContext = cacheContext;
   }

    /**
     * Creates an ObjectInputStream that reads from the specified InputStream.
     * A serialization stream header is read from the stream and verified.
     * This constructor will block until the corresponding ObjectOutputStream
     * has written and flushed the header.
     *
     * <p>If a security manager is installed, this constructor will check for
     * the "enableSubclassImplementation" SerializablePermission when invoked
     * directly or indirectly by the constructor of a subclass which overrides
     * the ObjectInputStream.readFields or ObjectInputStream.readUnshared
     * methods.
     *
     * @param	in input stream to read from
     * @param	selector type surrogate selector to use
     * @throws	StreamCorruptedException if the stream header is incorrect
     * @throws	IOException if an I/O error occurs while reading stream header
     * @throws	SecurityException if untrusted subclass illegally overrides
     * 		security-sensitive methods
     * @throws	NullPointerException if <code>this.mInput</code> is <code>null</code>
     */
    public ObjectInputStream(InputStream in, TypeSurrogateSelector selector)
        throws IOException
    {
        this.mInput = new BlockDataInputStream(in);
        this.mSelector = TypeSurrogateSelectorImpl.getDefault();
        this.mContext = new SerializationContext(this.mSelector);
        this.mContext.putUserItem("__bin", this.mInput);
    }

    /**
     * Returns the current <see cref="SerializationContext"/> object.
     *
     * @return the current serialzation context
     */
    public SerializationContext getContext() {
        return this.mContext;
    }

    /**
     * Returns the underlying InputStream object.
     */
    public InputStream getBaseStream() {
        return this.mInput;
    }

   /**
     * Reads some bytes from an input
     * stream and stores them into the buffer
     * array <code>b</code>. The number of bytes
     * read is equal
     * to the length of <code>b</code>.
     * <p>
     * This method blocks until one of the
     * following conditions occurs:<p>
     * <ul>
     * <li><code>b.length</code>
     * bytes of input data are available, this.mInput which
     * case a normal return is made.
     *
     * <li>End of
     * file is detected, this.mInput which case an <code>EOFException</code>
     * is thrown.
     *
     * <li>An I/O error occurs, this.mInput
     * which case an <code>IOException</code> other
     * than <code>EOFException</code> is thrown.
     * </ul>
     * <p>
     * If <code>b</code> is <code>null</code>,
     * a <code>NullPointerException</code> is thrown.
     * If <code>b.length</code> is zero, then
     * no bytes are read. Otherwise, the first
     * byte read is stored into element <code>b[0]</code>,
     * the next one into <code>b[1]</code>, and
     * so on.
     * If an exception is thrown from
     * this method, then it may be that some but
     * not all bytes of <code>b</code> have been
     * updated with data from the input stream.
     *
     * @param     b   the buffer into which the data is read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final void readFully(byte b[]) throws IOException {
        this.mInput.readFully(b);
    }

    /**
     *
     * Reads <code>len</code>
     * bytes from
     * an input stream.
     * <p>
     * This method
     * blocks until one of the following conditions
     * occurs:<p>
     * <ul>
     * <li><code>len</code> bytes
     * of input data are available, this.mInput which case
     * a normal return is made.
     *
     * <li>End of file
     * is detected, this.mInput which case an <code>EOFException</code>
     * is thrown.
     *
     * <li>An I/O error occurs, this.mInput
     * which case an <code>IOException</code> other
     * than <code>EOFException</code> is thrown.
     * </ul>
     * <p>
     * If <code>b</code> is <code>null</code>,
     * a <code>NullPointerException</code> is thrown.
     * If <code>off</code> is negative, or <code>len</code>
     * is negative, or <code>off+len</code> is
     * greater than the length of the array <code>b</code>,
     * then an <code>IndexOutOfBoundsException</code>
     * is thrown.
     * If <code>len</code> is zero,
     * then no bytes are read. Otherwise, the first
     * byte read is stored into element <code>b[off]</code>,
     * the next one into <code>b[off+1]</code>,
     * and so on. The number of bytes read is,
     * at most, equal to <code>len</code>.
     *
     * @param     b   the buffer into which the data is read.
     * @param off  an int specifying the offset into the data.
     * @param len  an int specifying the number of bytes to read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final void readFully(byte b[], int off, int len) throws IOException {
        this.mInput.readFully(b, off, len);
    }

    /**
     * Makes an attempt to skip over
     * <code>n</code> bytes
     * of data from the input
     * stream, discarding the skipped bytes. However,
     * it may skip
     * over some smaller number of
     * bytes, possibly zero. This may result from
     * any of a
     * number of conditions; reaching
     * end of file before <code>n</code> bytes
     * have been skipped is
     * only one possibility.
     * This method never throws an <code>EOFException</code>.
     * The actual
     * number of bytes skipped is returned.
     *
     * @param      n   the number of bytes to be skipped.
     * @return     the number of bytes actually skipped.
     * @exception  IOException   if an I/O error occurs.
     */
    public final int skipBytes(int n) throws IOException {
        return this.mInput.skipBytes(n);
    }

    /**
     * Reads one input byte and returns
     * <code>true</code> if that byte is nonzero,
     * <code>false</code> if that byte is zero.
     * This method is suitable for reading
     * the byte written by the <code>writeBoolean</code>
     * method of interface <code>DataOutput</code>.
     *
     * @return     the <code>boolean</code> value read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final boolean readBoolean() throws IOException {
        return this.mInput.readBoolean();
    }

    /**
     * Reads and returns one input byte.
     * The byte is treated as a signed value this.mInput
     * the range <code>-128</code> through <code>127</code>,
     * inclusive.
     * This method is suitable for
     * reading the byte written by the <code>writeByte</code>
     * method of interface <code>DataOutput</code>.
     *
     * @return     the 8-bit value read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final byte readByte() throws IOException {
        return this.mInput.readByte();
    }

    /**
     * Reads one input byte, zero-extends
     * it to type <code>int</code>, and returns
     * the result, which is therefore this.mInput the range
     * <code>0</code>
     * through <code>255</code>.
     * This method is suitable for reading
     * the byte written by the <code>writeByte</code>
     * method of interface <code>DataOutput</code>
     * if the argument to <code>writeByte</code>
     * was intended to be a value this.mInput the range
     * <code>0</code> through <code>255</code>.
     *
     * @return     the unsigned 8-bit value read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final int readUnsignedByte() throws IOException {
        return this.mInput.readUnsignedByte();
    }

    /**
     * Reads two input bytes and returns
     * a <code>short</code> value. Let <code>a</code>
     * be the first byte read and <code>b</code>
     * be the second byte. The value
     * returned
     * is:
     * <p><pre><code>(short)((a &lt;&lt; 8) | (b &amp; 0xff))
     * </code></pre>
     * This method
     * is suitable for reading the bytes written
     * by the <code>writeShort</code> method of
     * interface <code>DataOutput</code>.
     *
     * @return     the 16-bit value read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final short readShort() throws IOException {
        return this.mInput.readShort();
    }

    /**
     * Reads two input bytes and returns
     * an <code>int</code> value this.mInput the range <code>0</code>
     * through <code>65535</code>. Let <code>a</code>
     * be the first byte read and
     * <code>b</code>
     * be the second byte. The value returned is:
     * <p><pre><code>(((a &amp; 0xff) &lt;&lt; 8) | (b &amp; 0xff))
     * </code></pre>
     * This method is suitable for reading the bytes
     * written by the <code>writeShort</code> method
     * of interface <code>DataOutput</code>  if
     * the argument to <code>writeShort</code>
     * was intended to be a value this.mInput the range
     * <code>0</code> through <code>65535</code>.
     *
     * @return     the unsigned 16-bit value read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final int readUnsignedShort() throws IOException {
        return this.mInput.readUnsignedShort();
    }

    /**
     * Reads two input bytes and returns a <code>char</code> value.
     * Let <code>a</code>
     * be the first byte read and <code>b</code>
     * be the second byte. The value
     * returned is:
     * <p><pre><code>(char)((a &lt;&lt; 8) | (b &amp; 0xff))
     * </code></pre>
     * This method
     * is suitable for reading bytes written by
     * the <code>writeChar</code> method of interface
     * <code>DataOutput</code>.
     *
     * @return     the <code>char</code> value read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final char readChar() throws IOException {
        return this.mInput.readChar();
    }

    /**
     * Reads four input bytes and returns an
     * <code>int</code> value. Let <code>a-d</code>
     * be the first through fourth bytes read. The value returned is:
     * <p><pre>
     * <code>
     * (((a &amp; 0xff) &lt;&lt; 24) | ((b &amp; 0xff) &lt;&lt; 16) |
     * &#32;((c &amp; 0xff) &lt;&lt; 8) | (d &amp; 0xff))
     * </code></pre>
     * This method is suitable
     * for reading bytes written by the <code>writeInt</code>
     * method of interface <code>DataOutput</code>.
     *
     * @return     the <code>int</code> value read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final int readInt() throws IOException {
        return this.mInput.readInt();
    }

    /**
     * Reads eight input bytes and returns
     * a <code>long</code> value. Let <code>a-h</code>
     * be the first through eighth bytes read.
     * The value returned is:
     * <p><pre> <code>
     * (((long)(a &amp; 0xff) &lt;&lt; 56) |
     *  ((long)(b &amp; 0xff) &lt;&lt; 48) |
     *  ((long)(c &amp; 0xff) &lt;&lt; 40) |
     *  ((long)(d &amp; 0xff) &lt;&lt; 32) |
     *  ((long)(e &amp; 0xff) &lt;&lt; 24) |
     *  ((long)(f &amp; 0xff) &lt;&lt; 16) |
     *  ((long)(g &amp; 0xff) &lt;&lt;  8) |
     *  ((long)(h &amp; 0xff)))
     * </code></pre>
     * <p>
     * This method is suitable
     * for reading bytes written by the <code>writeLong</code>
     * method of interface <code>DataOutput</code>.
     *
     * @return     the <code>long</code> value read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final long readLong() throws IOException {
        return this.mInput.readLong();
    }

    /**
     * Reads four input bytes and returns
     * a <code>float</code> value. It does this
     * by first constructing an <code>int</code>
     * value this.mInput exactly the manner
     * of the <code>readInt</code>
     * method, then converting this <code>int</code>
     * value to a <code>float</code> this.mInput
     * exactly the manner of the method <code>Float.intBitsToFloat</code>.
     * This method is suitable for reading
     * bytes written by the <code>writeFloat</code>
     * method of interface <code>DataOutput</code>.
     *
     * @return     the <code>float</code> value read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final float readFloat() throws IOException {
        return this.mInput.readFloat();
    }

    /**
     * Reads eight input bytes and returns
     * a <code>double</code> value. It does this
     * by first constructing a <code>long</code>
     * value this.mInput exactly the manner
     * of the <code>readlong</code>
     * method, then converting this <code>long</code>
     * value to a <code>double</code> this.mInput exactly
     * the manner of the method <code>Double.longBitsToDouble</code>.
     * This method is suitable for reading
     * bytes written by the <code>writeDouble</code>
     * method of interface <code>DataOutput</code>.
     *
     * @return     the <code>double</code> value read.
     * @exception  EOFException  if this stream reaches the end before reading
     *               all the bytes.
     * @exception  IOException   if an I/O error occurs.
     */
    public final double readDouble() throws IOException {
        return this.mInput.readDouble();
    }

    /**
     * Reads the next line of text from the input stream.
     * It reads successive bytes, converting
     * each byte separately into a character,
     * until it encounters a line terminator or
     * end of
     * file; the characters read are then
     * returned as a <code>String</code>. Note
     * that because this
     * method processes bytes,
     * it does not support input of the full Unicode
     * character set.
     * <p>
     * If end of file is encountered
     * before even one byte can be read, then <code>null</code>
     * is returned. Otherwise, each byte that is
     * read is converted to type <code>char</code>
     * by zero-extension. If the character <code>'\n'</code>
     * is encountered, it is discarded and reading
     * ceases. If the character <code>'\r'</code>
     * is encountered, it is discarded and, if
     * the following byte converts &#32;to the
     * character <code>'\n'</code>, then that is
     * discarded also; reading then ceases. If
     * end of file is encountered before either
     * of the characters <code>'\n'</code> and
     * <code>'\r'</code> is encountered, reading
     * ceases. Once reading has ceased, a <code>String</code>
     * is returned that contains all the characters
     * read and not discarded, taken this.mInput order.
     * Note that every character this.mInput this string
     * will have a value less than <code>&#92;u0100</code>,
     * that is, <code>(char)256</code>.
     *
     * @return the next line of text from the input stream,
     *         or <CODE>null</CODE> if the end of file is
     *         encountered before a byte can be read.
     * @exception  IOException  if an I/O error occurs.
     */
    public String readLine() throws IOException {
        return this.mInput.readLine();
    }

    /**
     * Reads this.mInput a string that has been encoded using a
     * <a href="#modified-utf-8">modified UTF-8</a>
     * format.
     * The general contract of <code>readUTF</code>
     * is that it reads a representation of a Unicode
     * character string encoded this.mInput modified
     * UTF-8 format; this string of characters
     * is then returned as a <code>String</code>.
     * <p>
     * First, two bytes are read and used to
     * construct an unsigned 16-bit integer this.mInput
     * exactly the manner of the <code>readUnsignedShort</code>
     * method . This integer value is called the
     * <i>UTF length</i> and specifies the number
     * of additional bytes to be read. These bytes
     * are then converted to characters by considering
     * them this.mInput groups. The length of each group
     * is computed from the value of the first
     * byte of the group. The byte following a
     * group, if any, is the first byte of the
     * next group.
     * <p>
     * If the first byte of a group
     * matches the bit pattern <code>0xxxxxxx</code>
     * (where <code>x</code> means "may be <code>0</code>
     * or <code>1</code>"), then the group consists
     * of just that byte. The byte is zero-extended
     * to form a character.
     * <p>
     * If the first byte
     * of a group matches the bit pattern <code>110xxxxx</code>,
     * then the group consists of that byte <code>a</code>
     * and a second byte <code>b</code>. If there
     * is no byte <code>b</code> (because byte
     * <code>a</code> was the last of the bytes
     * to be read), or if byte <code>b</code> does
     * not match the bit pattern <code>10xxxxxx</code>,
     * then a <code>UTFDataFormatException</code>
     * is thrown. Otherwise, the group is converted
     * to the character:<p>
     * <pre><code>(char)(((a&amp; 0x1F) &lt;&lt; 6) | (b &amp; 0x3F))
     * </code></pre>
     * If the first byte of a group
     * matches the bit pattern <code>1110xxxx</code>,
     * then the group consists of that byte <code>a</code>
     * and two more bytes <code>b</code> and <code>c</code>.
     * If there is no byte <code>c</code> (because
     * byte <code>a</code> was one of the last
     * two of the bytes to be read), or either
     * byte <code>b</code> or byte <code>c</code>
     * does not match the bit pattern <code>10xxxxxx</code>,
     * then a <code>UTFDataFormatException</code>
     * is thrown. Otherwise, the group is converted
     * to the character:<p>
     * <pre><code>
     * (char)(((a &amp; 0x0F) &lt;&lt; 12) | ((b &amp; 0x3F) &lt;&lt; 6) | (c &amp; 0x3F))
     * </code></pre>
     * If the first byte of a group matches the
     * pattern <code>1111xxxx</code> or the pattern
     * <code>10xxxxxx</code>, then a <code>UTFDataFormatException</code>
     * is thrown.
     * <p>
     * If end of file is encountered
     * at any time during this entire process,
     * then an <code>EOFException</code> is thrown.
     * <p>
     * After every group has been converted to
     * a character by this process, the characters
     * are gathered, this.mInput the same order this.mInput which
     * their corresponding groups were read from
     * the input stream, to form a <code>String</code>,
     * which is returned.
     * <p>
     * The <code>writeUTF</code>
     * method of interface <code>DataOutput</code>
     * may be used to write data that is suitable
     * for reading by this method.
     * @return     a Unicode string.
     * @exception  EOFException            if this stream reaches the end
     *               before reading all the bytes.
     * @exception  IOException             if an I/O error occurs.
     * @exception  UTFDataFormatException  if the bytes do not represent a
     *               valid modified UTF-8 encoding of a string.
     */
    public String readUTF() throws IOException {
        return this.mInput.readUTF();
    }

    public int readUInt16() throws IOException
    {
        return this.mInput.readUInt16();
    }

    public long readUInt32() throws IOException
    {
        return this.mInput.readUInt32();
    }

    public BigInteger readUInt64() throws IOException
    {
        return this.mInput.readUInt64();
    }

    /**
     * Read and return an object. The class that implements this interface
     * defines where the object is "read" from.
     *
     * @return the object read from the stream
     * @exception java.lang.ClassNotFoundException If the class of a serialized
     *      object cannot be found.
     * @exception IOException If any of the usual Input/Output
     * related exceptions occur.
     */
    public Object readObject()
        throws ClassNotFoundException, IOException
    {
        try
        {
            // read type handle
            short handle = this.readShort();

            // Find an appropriate surrogate by handle
            SerializationSurrogate surrogate =
                    this.mSelector.getSurrogateForTypeHandle(handle, _cacheContext);

            if(surrogate == null)
                surrogate = this.mSelector.GetSurrogateForSubTypeHandle(handle, this.readShort(), _cacheContext);


            return surrogate.readObject(this);
        }
        catch (Exception ex)
        {
            throw new IOException(ex.toString());
        }
    }

    /**
     * Read and return an object. The class that implements this interface
     * defines where the object is "read" from.
     *
     * @param objClass the class surrogate to use for reading the object.
     * @return the object read from the stream
     * @exception java.lang.ClassNotFoundException If the class of a serialized
     *      object cannot be found.
     * @exception IOException If any of the usual Input/Output
     * related exceptions occur.
     */
    public Object readObjectAs(Class objClass)
	throws ClassNotFoundException, IOException
    {
        try
        {
            // Find an appropriate surrogate by handle
            SerializationSurrogate surrogate =
                    this.mSelector.getSurrogateForType(objClass, _cacheContext);
            return surrogate.readObject(this);
        }
        catch (Exception ex)
        {
            throw new IOException(ex.toString());
        }
    }

    /**
     * Reads a byte of data. This method will block if no input is
     * available.
     * @return 	the byte read, or -1 if the end of the
     *		stream is reached.
     * @exception IOException If an I/O error has occurred.
     */
    public final int read() throws IOException {
        return this.mInput.read();
    }

    /**
     * Reads into an array of bytes.  This method will
     * block until some input is available.
     * @param b	the buffer into which the data is read
     * @return  the actual number of bytes read, -1 is
     * 		returned when the end of the stream is reached.
     * @exception IOException If an I/O error has occurred.
     */
    @Override
    public final int read(byte b[]) throws IOException {
        return this.mInput.read(b);
    }

    /**
     * Reads into an array of bytes.  This method will
     * block until some input is available.
     * @param b	the buffer into which the data is read
     * @param off the start offset of the data
     * @param len the maximum number of bytes read
     * @return  the actual number of bytes read, -1 is
     * 		returned when the end of the stream is reached.
     * @exception IOException If an I/O error has occurred.
     */
    @Override
    public final int read(byte b[], int off, int len) throws IOException {
        return this.mInput.read(b, off, len);
    }

    /**
     * Skips n bytes of input.
     * @param n the number of bytes to be skipped
     * @return	the actual number of bytes skipped.
     * @exception IOException If an I/O error has occurred.
     */
    @Override
    public final long skip(long n) throws IOException {
        return this.mInput.skip(n);
    }

    /**
     * Returns the number of bytes that can be read
     * without blocking.
     * @return the number of available bytes.
     * @exception IOException If an I/O error has occurred.
     */
    @Override
    public final int available() throws IOException {
        return this.mInput.available();
    }

    /**
     * Closes the input stream. Must be called
     * to release any resources associated with
     * the stream.
     * @exception IOException If an I/O error has occurred.
     */
    @Override
    public final void close() throws IOException {
        this.mInput.close();
    }




    public String getCacheContext()
    {
        return this._cacheContext;
    }

    public void skipObject() throws IOException
    {
        try
        {
            // read type handle
            short handle = this.readShort();

            // Find an appropriate surrogate by handle
            SerializationSurrogate surrogate =
                    this.mSelector.getSurrogateForTypeHandle(handle, _cacheContext);

            if(surrogate == null)
                surrogate = this.mSelector.GetSurrogateForSubTypeHandle(handle, this.readShort(), _cacheContext);


            surrogate.skipObject(this);
        }
        catch (Exception ex)
        {
            throw new IOException(ex.toString());
        }
    }

    //<editor-fold defaultstate="collapsed" desc="SKIP">
    public void skipFully(byte[] b) throws IOException
    {
        this.mInput.skipBytes(b.length);
    }

    public void skipFully(byte[] b, int off, int len) throws IOException
    {
        this.mInput.readFully(b, off, len);
    }

    public void skipBoolean() throws IOException
    {
        this.mInput.skipBytes(1);
    }

    public void skipByte() throws IOException
    {
        this.mInput.skipBytes(1);
    }

    public void skipUnsignedByte() throws IOException
    {
        this.mInput.skipBytes(1);
    }

    public void skipShort() throws IOException
    {
        this.mInput.skipBytes(2);
    }

    public void skipUnsignedShort() throws IOException
    {
        this.mInput.skipBytes(2);
    }

    public void skipChar() throws IOException
    {
        this.mInput.skipBytes(1);
    }

    public void skipInt() throws IOException
    {
        this.mInput.skipBytes(4);
    }

    public void skipLong() throws IOException
    {
        this.mInput.skipBytes(8);
    }

    public void skipFloat() throws IOException
    {
        this.mInput.skipBytes(4);
    }

    public void skipDouble() throws IOException
    {
        this.mInput.skipBytes(8);
    }

    public void skipLine() throws IOException
    {
        this.mInput.readLine();
    }

    public void skipUTF() throws IOException
    {
        this.mInput.readUTF();
    }

    public void skipUInt16() throws IOException
    {
        this.mInput.skipBytes(2);
    }

    public void skipUInt32() throws IOException
    {
        this.mInput.skipBytes(4);
    }

    public void skipUInt64() throws IOException
    {
        this.mInput.skipBytes(8);
    }

    public void skipUByte() throws IOException
    {
        this.mInput.skipBytes(2);
    }
    //</editor-fold>

    public short readUByte() throws IOException
    {
        return this.mInput.readUByte();
    }
}
