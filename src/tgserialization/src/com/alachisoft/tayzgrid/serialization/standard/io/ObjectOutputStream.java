/*
 * @(#)ObjectOutputStream.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.standard.io;

import com.alachisoft.tayzgrid.serialization.core.io.BlockDataOutputStream;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.core.io.SerializationContext;
import com.alachisoft.tayzgrid.serialization.core.io.TypeSurrogateSelector;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheIOException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ObjectArraySerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.surrogates.ObjectSerializationSurrogate;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

/**
 * ObjectOutputStream class.
 *
 * @version 1.0, September 18, 2008
 */
public class ObjectOutputStream
        extends OutputStream implements CacheObjectOutput
{

    /**
     * working arrays initialized on demand by readUTF
     */
    private BlockDataOutputStream mOutput;
    private SerializationContext mContext;
    private TypeSurrogateSelector mSelector;
    private String _cacheContext;

    /**
     * Creates an ObjectOutputStream that writes to the specified OutputStream.
     * This constructor writes the serialization stream header to the
     * underlying stream; callers may wish to flush the stream immediately to
     * ensure that constructors for receiving ObjectInputStreams will not block
     * when reading the header.
     *
     * <p>If a security manager is installed, this constructor will check for
     * the "enableSubclassImplementation" SerializablePermission when invoked
     * directly or indirectly by the constructor of a subclass which overrides
     * the ObjectOutputStream.putFields or ObjectOutputStream.writeUnshared
     * methods.
     *
     * @param	out output stream to write to
     * @throws	IOException if an I/O error occurs while writing stream header
     * @throws	SecurityException if untrusted subclass illegally overrides
     * 		security-sensitive methods
     * @throws	NullPointerException if <code>out</code> is <code>null</code>
     */
    public ObjectOutputStream(OutputStream out, String cacheContext) throws IOException
    {
//        JarFileLoader jfl=JarLoaderFactory.getLoader(cacheContext);
//        if(jfl!=null)
//        {    
//            Thread.currentThread().setContextClassLoader(jfl);
//        }
        this.mOutput = new BlockDataOutputStream(out);
        this.mSelector = TypeSurrogateSelectorImpl.getDefault();
        this.mContext = new SerializationContext(this.mSelector);
        this.mContext.putUserItem("__bout", this.mOutput);
        this._cacheContext = cacheContext;
    }

    /**
     * Creates an ObjectOutputStream that writes to the specified OutputStream.
     * This constructor writes the serialization stream header to the
     * underlying stream; callers may wish to flush the stream immediately to
     * ensure that constructors for receiving ObjectInputStreams will not block
     * when reading the header.
     *
     * <p>If a security manager is installed, this constructor will check for
     * the "enableSubclassImplementation" SerializablePermission when invoked
     * directly or indirectly by the constructor of a subclass which overrides
     * the ObjectOutputStream.putFields or ObjectOutputStream.writeUnshared
     * methods.
     *
     * @param	out output stream to write to
     * @param	selector type surrogate selector to use
     * @throws	IOException if an I/O error occurs while writing stream header
     * @throws	SecurityException if untrusted subclass illegally overrides
     * 		security-sensitive methods
     * @throws	NullPointerException if <code>out</code> is <code>null</code>
     */
    public ObjectOutputStream(OutputStream out, TypeSurrogateSelector selector) throws IOException
    {
        this.mOutput = new BlockDataOutputStream(out);
        this.mContext = new SerializationContext(selector);
        this.mSelector = this.mContext.getSurrogateSelector();
        this.mContext.putUserItem("__bout", this.mOutput);
    }

    /**
     * Returns the current <see cref="SerializationContext"/> object.
     *
     * @return the current serialzation context
     */
    public SerializationContext getContext()
    {
        return this.mContext;
    }

    /**
     * Returns the underlying InputStream object.
     */
    public OutputStream getBaseStream()
    {
        return this.mOutput;
    }

    /**
     * Writes the specified byte (the low eight bits of the argument
     * <code>b</code>) to the underlying output stream. If no exception
     * is thrown, the counter <code>written</code> is incremented by
     * <code>1</code>.
     * <p>
     * Implements the <code>write</code> method of <code>OutputStream</code>.
     *
     * @param      b   the <code>byte</code> to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public synchronized void write(int b) throws IOException
    {
        this.mOutput.write(b);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to the underlying output stream.
     * If no exception is thrown, the counter <code>written</code> is
     * incremented by <code>len</code>.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    @Override
    public synchronized void write(byte b[], int off, int len) throws IOException
    {
        this.mOutput.write(b, off, len);
    }

    /**
     * Flushes this data output stream. This forces any buffered output
     * bytes to be written out to the stream.
     * <p>
     * The <code>flush</code> method of <code>DataOutputStream</code>
     * calls the <code>flush</code> method of its underlying output stream.
     *
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     * @see        java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException
    {
        this.mOutput.flush();
    }

    /**
     * Writes a <code>boolean</code> to the underlying output stream as
     * a 1-byte value. The value <code>true</code> is written out as the
     * value <code>(byte)1</code>; the value <code>false</code> is
     * written out as the value <code>(byte)0</code>. If no exception is
     * thrown, the counter <code>written</code> is incremented by
     * <code>1</code>.
     *
     * @param      v   a <code>boolean</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeBoolean(boolean v) throws IOException
    {
        this.mOutput.writeBoolean(v);
    }

    /**
     * Writes out a <code>byte</code> to the underlying output stream as
     * a 1-byte value. If no exception is thrown, the counter
     * <code>written</code> is incremented by <code>1</code>.
     *
     * @param      v   a <code>byte</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeByte(int v) throws IOException
    {
        this.mOutput.writeByte(v);
    }

    /**
     * Writes a <code>short</code> to the underlying output stream as two
     * bytes, high byte first. If no exception is thrown, the counter
     * <code>written</code> is incremented by <code>2</code>.
     *
     * @param      v   a <code>short</code> to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeShort(int v) throws IOException
    {
        this.mOutput.writeShort(v);
    }

    /**
     * Writes a <code>char</code> to the underlying output stream as a
     * 2-byte value, high byte first. If no exception is thrown, the
     * counter <code>written</code> is incremented by <code>2</code>.
     *
     * @param      v   a <code>char</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeChar(int v) throws IOException
    {
        this.mOutput.writeChar(v);
    }

    /**
     * Writes an <code>int</code> to the underlying output stream as four
     * bytes, high byte first. If no exception is thrown, the counter
     * <code>written</code> is incremented by <code>4</code>.
     *
     * @param      v   an <code>int</code> to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeInt(int v) throws IOException
    {
        this.mOutput.writeInt(v);
    }

    /**
     * Writes a <code>long</code> to the underlying output stream as eight
     * bytes, high byte first. In no exception is thrown, the counter
     * <code>written</code> is incremented by <code>8</code>.
     *
     * @param      v   a <code>long</code> to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeLong(long v) throws IOException
    {
        this.mOutput.writeLong(v);
    }

    /**
     * Converts the float argument to an <code>int</code> using the
     * <code>floatToIntBits</code> method in class <code>Float</code>,
     * and then writes that <code>int</code> value to the underlying
     * output stream as a 4-byte quantity, high byte first. If no
     * exception is thrown, the counter <code>written</code> is
     * incremented by <code>4</code>.
     *
     * @param      v   a <code>float</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     * @see        java.lang.Float#floatToIntBits(float)
     */
    public final void writeFloat(float v) throws IOException
    {
        this.mOutput.writeFloat(v);
    }

    /**
     * Converts the double argument to a <code>long</code> using the
     * <code>doubleToLongBits</code> method in class <code>Double</code>,
     * and then writes that <code>long</code> value to the underlying
     * output stream as an 8-byte quantity, high byte first. If no
     * exception is thrown, the counter <code>written</code> is
     * incremented by <code>8</code>.
     *
     * @param      v   a <code>double</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     * @see        java.lang.Double#doubleToLongBits(double)
     */
    public final void writeDouble(double v) throws IOException
    {
        this.mOutput.writeDouble(v);
    }

    /**
     * Writes out the string to the underlying output stream as a
     * sequence of bytes. Each character in the string is written out, in
     * sequence, by discarding its high eight bits. If no exception is
     * thrown, the counter <code>written</code> is incremented by the
     * length of <code>s</code>.
     *
     * @param      s   a string of bytes to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeBytes(String s) throws IOException
    {
        this.mOutput.writeBytes(s);
    }

    /**
     * Writes a string to the underlying output stream as a sequence of
     * characters. Each character is written to the data output stream as
     * if by the <code>writeChar</code> method. If no exception is
     * thrown, the counter <code>written</code> is incremented by twice
     * the length of <code>s</code>.
     *
     * @param      s   a <code>String</code> value to be written.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.DataOutputStream#writeChar(int)
     * @see        java.io.FilterOutputStream#out
     */
    public final void writeChars(String s) throws IOException
    {
        this.mOutput.writeChars(s);
    }

    /**
     * Writes a string to the underlying output stream using
     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
     * encoding in a machine-independent manner.
     * <p>
     * First, two bytes are written to the output stream as if by the
     * <code>writeShort</code> method giving the number of bytes to
     * follow. This value is the number of bytes actually written out,
     * not the length of the string. Following the length, each character
     * of the string is output, in sequence, using the modified UTF-8 encoding
     * for the character. If no exception is thrown, the counter
     * <code>written</code> is incremented by the total number of
     * bytes written to the output stream. This will be at least two
     * plus the length of <code>str</code>, and at most two plus
     * thrice the length of <code>str</code>.
     *
     * @param      str   a string to be written.
     * @exception  IOException  if an I/O error occurs.
     */
    public final void writeUTF(String str) throws IOException
    {
        this.mOutput.writeUTF(str);
    }

    /**
     * Writes a 'UInt16' vale portable with 'ushort' for .Net to the underlying output stream as four
     * bytes. If no exception is thrown, the counter
     * <code>written</code> is incremented by <code>4</code>.
     * @param i value to write
     * @throws IOException either when EOF is reached or value is out of Range of 'ushort'
     */
    public final void writeUInt16(int i) throws IOException
    {
        if(i > 65535)
            throw new IOException("Unable to serialize: Provided integer value [" + Integer.toString(i) + "] is greater than 65,535 and is out of Range of 'UInt16'");
        else if(i < 0)
        {
            throw new IOException("Unable to serialize: Provided integer value [" + Integer.toString(i) + "] is less than '0' and is out of Range of 'UInt16'");
        }

        this.mOutput.writeUInt16(i);
    }

    /**
     * Writes a 'UInt32' vale portable with 'uInt' for .Net to the underlying output stream as four
     * bytes. If no exception is thrown, the counter is incremented with 4 bytes
     * @param i value to be written
     * @throws IOException either when EOF is reached or value is out of Range of 'uInt'
     */
    public final void writeUInt32(long i) throws IOException
    {
        if (i > 4294967295L)
        {
            throw new IOException("Unable to serialize: Provided long value [" + Long.toString(i) + "] is greater than 4,29,49,67,295 and is out of Range of 'UInt32'");
        }
        else if(i < 0)
        {
            throw new IOException("Unable to serialize: Provided long value [" + Long.toString(i) + "] is less than '0' and is out of Range of 'UInt32'");
        }

        this.mOutput.writeUInt32(i);
    }

    /**
     * Writes a 'UInt64' vale portable with 'uLong' for .Net to the underlying output stream as four
     * bytes. If no exception is thrown, the counter is incremented with 8 bytes
     * @param bigInteger bigInteger value to write
     * @throws IOException either when EOF is reached or value is out of Range of 'uLong'
     */
    public final void writeUInt64(BigInteger bigInteger) throws IOException
    {
        if(bigInteger == null)
            throw new IOException("Unable to serialize: Provided BigInteger value is null which is incompatible with  'UInt64'");

        if (bigInteger.compareTo(new BigInteger("18446744073709551615")) == 1)
        {
            throw new IOException("Unable to serialize: Provided BigInteger value [" + bigInteger.toString() + "] is greater than 18,446,744,073,709,551,615 and is out of Range of 'UInt64'");
        }
        else if(bigInteger.compareTo(new BigInteger("0")) == -1)
        {
            throw new IOException("Unable to serialize: Provided BigInteger value [" + bigInteger.toString() + "] is less than 0 and is out of Range of 'UInt64'");
        }

        this.mOutput.writeUInt64(bigInteger);
    }

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
        return this.mOutput.size();
    }

    /**
     * Write the specified object to the ObjectOutputStream. Objects referenced by this
     * object are written transitively so that a complete equivalent graph of
     * objects can be reconstructed by an ObjectInputStream.
     *
     * <p>Exceptions are thrown for problems with the OutputStream and for
     * classes that should not be serialized.  All exceptions are fatal to the
     * OutputStream, which is left in an indeterminate state, and it is up to
     * the caller to ignore or recover the stream state.
     *
     * @throws	IOException Any exception thrown by the underlying
     * 		OutputStream.
     */
    public final void writeObject(Object obj)
            throws IOException
    {
        try
        {
            // Find an appropriate surrogate for the object
            SerializationSurrogate surrogate = this.mSelector.getSurrogateForObject(obj, _cacheContext);
            // write type handle

            Class[] type;
            if (surrogate instanceof ObjectSerializationSurrogate)
            {
                SerializationSurrogate testSurrogate = null;

                testSurrogate = getType(surrogate, obj.getClass());

                if (testSurrogate != null)
                {
                    surrogate = testSurrogate;
                }

            }

            surrogate.writeHandle(this, obj);
            if(surrogate.getSubHandle() > 0)
                surrogate.writeSubHandle(this, obj);
            surrogate.writeObject(this, obj);
        }
        catch (CacheIOException nCacheIOException)
        {
            //this.writeObject(obj);
            throw new IOException(nCacheIOException.toString() + " is not marked as serializable.");
        }
        catch (Exception ex)
        {
            throw new IOException(ex.toString());
        }
    }

    SerializationSurrogate getType(SerializationSurrogate surrogate, Class type)
    {
        Class[] typeClasses = type.getInterfaces();
        for (int i = 0; i < typeClasses.length; i++)
        {
            Class class1 = typeClasses[i];

            surrogate = this.mSelector.getSurrogateForType(class1, true, _cacheContext);
            if (surrogate != null)
            {
                return surrogate;
            }
            else
            {
                surrogate = getType(surrogate, class1);
                if (surrogate != null)
                {
                    return surrogate;
                }
            }
        }
        return null;
    }

    /**
     * Write the specified object to the ObjectOutputStream. Objects referenced by this
     * object are written transitively so that a complete equivalent graph of
     * objects can be reconstructed by an ObjectInputStream.
     *
     * <p>Exceptions are thrown for problems with the OutputStream and for
     * classes that should not be serialized.  All exceptions are fatal to the
     * OutputStream, which is left in an indeterminate state, and it is up to
     * the caller to ignore or recover the stream state.
     *
     * @param obj the object to be written
     * @param objClass the class surrogate to use for writing the object.
     * @exception IOException Any of the usual Input/Output related exceptions.
     * @throws	IOException Any exception thrown by the underlying
     * 		OutputStream.
     */
    public void writeObject(Object obj, Class objClass)
            throws IOException
    {
        if (obj == null)
        {
            throw new NullPointerException("obj");
        }
        if (objClass == null)
        {
            throw new NullPointerException("objClass");
        }

        try
        {
            SerializationSurrogate surrogate = this.mSelector.getSurrogateForType(objClass, _cacheContext);
            surrogate.writeObject(this, obj);
        }
        catch (Exception ex)
        {
            throw new IOException(ex.toString());
        }
    }

    public void writeObject(Object obj, Object objClass) throws IOException
    {
        this.writeObject(obj, objClass.getClass());
    }

    // <editor-fold defaultstate="collapsed" desc="Overloads">
    public void write(boolean v, String type) throws IOException
    {
        writeBoolean(v);
    }

    public void write(int v, String type) throws IOException
    {
        writeInt(v);
    }

    public void write(String v, String type) throws IOException
    {
        writeUTF(v);
    }

    public String getCacheContext()
    {
        return this._cacheContext;
    }
    // </editor-fold>

    public void writeUByte(short sh) throws IOException
    {
        if (sh > 255)
        {
            throw new IOException("Unable to serialize: Provided short value [" + Short.toString(sh) + "] is greater than 255 and is out of Range of 'UnsignedByte'");
        }
        else if (sh < 0)
        {
            throw new IOException("Unable to serialize: Provided integer value [" + Short.toString(sh) + "] is less than '0' and is out of Range of 'UnsignedByte'");
        }

        this.mOutput.writeUByte(sh);
    }
}
