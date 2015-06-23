/*
 * @(#)SerializationSurrogateImpl.java	1.0
 *
 * Created on September 18, 2008, 12:59 PM
 *
 * Copyright 2008 NeXtreme Innovations, Inc. All rights reserved.
 * "NeXtreme Innovations" PROPRIETARY/CONFIDENTIAL. Use is subject
 * to license terms.
 */
package com.alachisoft.tayzgrid.serialization.core.io.surrogates;

import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.core.io.SerializationContext;

/**
 * SerializationSurrogateImpl class.
 *
 * @version 1.0, September 18, 2008
 */
public abstract class SerializationSurrogateImpl extends SerializationSurrogateBase implements SerializationSurrogate
{

    /** Creates a new instance of SerializationSurrogateImpl */
    public SerializationSurrogateImpl(Class type)
    {
        super(type);
    }

    /**
     * Creates instance of type returned by getRealClass(). This is different from SerializationSurrogateBase.createTypeInstance()
     * in the sense that an ObjectInput object is passed as parameter that can be used to read creation specific
     * information from the stream. For example it can be used to read the length of the array before actually 
     * reading the values.
     * 
     * The default implementation simply delegates to super.createTypeInstance().
     *
     * @param input stream reader
     * @throws CacheInstantiationException Object creation related exceptions.
     * @return Object that this surrogate must deserialize
     */
    public Object instantiate(CacheObjectInput input)
            throws CacheInstantiationException
    {
        return super.createInstance();
    }

    /**
     * Read an object of type returned by getRealClass() from the stream reader. 
     * A fresh instance of the object is passed as parameter.
     * The surrogate should populate fields in the object from data on the stream
     * 
     * @param input stream reader
     * @param graph a fresh instance of the object that the surrogate must deserialize.
     * @throws CacheInstantiationException Object creation related exceptions.
     * @exception IOException Any of the usual Input/Output related exceptions.
     * @return object read from the stream reader
     */
    abstract public Object readDirect(CacheObjectInput input, Object graph)
            throws CacheInstantiationException, CacheIOException;

    /**
     * Write an object of type returned by getRealClass() to the stream writer
     *
     * @param output stream writer
     * @param graph object to be written to the stream reader
     * @exception IOException Any of the usual Input/Output related exceptions.
     */
    abstract public void writeDirect(CacheObjectOutput output, Object graph)
            throws CacheIOException;
    
    abstract public void skipDirect(CacheObjectInput input, Object graph) throws CacheInstantiationException, CacheIOException;

    /**
     * Read an object of type returned by getRealClass() from the stream reader
     *
     * @param input stream reader
     * @return object read from the stream reader
     * @throws CacheInstantiationException Object creation related exceptions.
     * @exception IOException Any of the usual Input/Output related exceptions.
     */
    public Object readObject(CacheObjectInput input)
            throws CacheInstantiationException, CacheIOException
    {
        try
        {
            int cookie = input.readInt();
            Object graph = input.getContext().getObject(cookie);
            if (graph == null)
            {
                boolean bKnown = false;
                graph = instantiate(input);
                if (graph != null)
                {
                    input.getContext().rememberForRead(graph);
                    bKnown = true;
                }
                 
                if (this.isVersionCompatible()) 
                {
                    int dataLength = 0;
//                  
                    dataLength = input.readInt();
                    graph = readDirect(input, graph);
   
                    //streams in java does not provide seek options,
                    //For time being jvcache will consider all data has been read from stream
                } else {
                    graph = readDirect(input, graph);
                }
                
                if (!bKnown)
                {
                    input.getContext().rememberForRead(graph);
                }
            }
            return graph;
        }
        catch (CacheInstantiationException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new CacheIOException(ex);
        }
    }

    /**
     * Write an object to the underlying storage or stream.  The
     * class that implements this interface defines how the object is
     * written.
     *
     * @param output stream writer
     * @param graph object to be written to the stream reader
     * @exception IOException Any of the usual Input/Output related exceptions.
     */
    public void writeObject(CacheObjectOutput output, Object graph)  throws CacheIOException
    {
        try
        {
            Integer cookie = output.getContext().getCookie(graph);
            if (cookie != SerializationContext.InvalidCookie)
            {
                output.writeInt(cookie);
                return;
            }

            cookie = output.getContext().rememberForWrite(graph);
            output.writeInt(cookie);
            
            if(this.isVersionCompatible())
            {
                long startPosition = 0;
                long endPosition = 0;
                
                //No position api in java streams
                //version compatibility is not provided in jvcache
                //data length -1 will indicate non version compatible class i.e. no skip logic
                output.writeInt((int)-1);
                writeDirect(output, graph);

            }
            else
            {
                writeDirect(output, graph);
            }
        }
        catch (Exception ex)
        {
            throw new CacheIOException(ex);
        }
    }
    
    public void skipObject(CacheObjectInput input) throws CacheInstantiationException, CacheIOException
    {
        try
        {
            int cookie = input.readInt();
            Object graph = input.getContext().getObject(cookie);
            if (graph == null)
            {
                graph = instantiate(input);
                graph = readDirect(input, graph);
            }
        }
        catch (CacheInstantiationException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new CacheIOException(ex);
        }
    }
}
