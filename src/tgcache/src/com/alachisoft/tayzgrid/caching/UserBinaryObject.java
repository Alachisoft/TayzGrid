/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.common.ISizable;
import com.alachisoft.tayzgrid.common.datastructures.IStreamItem;
import com.alachisoft.tayzgrid.common.datastructures.VirtualArray;
import com.alachisoft.tayzgrid.common.datastructures.VirtualIndex;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Encapsulates the actual user payload in byte array form. This class is designed to keep user payload in chunks of size not greater than 80 KB. It is designed to handle the large
 * objects.
 */
public class UserBinaryObject implements ICompactSerializable, IStreamItem, java.io.Serializable, ISizable
{

    private static final int LARGE_OBJECT_SIZE = 79 * 1024;
    private static final int BYTE_ARRAY_MEMORY_OVERHEAD = 24;
    private java.util.ArrayList _data = new java.util.ArrayList();
    private int _noOfChunks;
    private int _index;

    public UserBinaryObject(int noOfChunks)
    {
        _noOfChunks = noOfChunks;
    }

    public UserBinaryObject(Object[] data)
    {
        _noOfChunks = data.length;
        for (Object buffer : data)
        {
            _data.add((byte[])buffer);
        }
    }

    public UserBinaryObject()
    {
        _data = new java.util.ArrayList();
    }

    public final void AddDataChunk(byte[] dataChunk)
    {
        if (_data != null && _index < _noOfChunks)
        {
            _data.add(_index, dataChunk);
            _index++;
        }
    }

    /**
     * Creates a UserBinaryObject from a byte array, which may be a large object.
     *
     * @param largbyteArray
     * @return
     */
    public static UserBinaryObject CreateUserBinaryObject(byte[] largbyteArray)
    {
        UserBinaryObject binaryObject = null;
        if (largbyteArray != null)
        {
            int noOfChunks = largbyteArray.length / LARGE_OBJECT_SIZE;
            noOfChunks += (largbyteArray.length - (noOfChunks * LARGE_OBJECT_SIZE)) != 0 ? 1 : 0;

            binaryObject = new UserBinaryObject(noOfChunks);

            int nextChunk = 0;
            int nextChunkSize = 0;
            for (int i = 1; i <= noOfChunks; i++)
            {
                nextChunkSize = largbyteArray.length - nextChunk;
                if (nextChunkSize > LARGE_OBJECT_SIZE)
                {
                    nextChunkSize = LARGE_OBJECT_SIZE;
                }

                byte[] binaryChunk = new byte[nextChunkSize];

                System.arraycopy(largbyteArray, nextChunk, binaryChunk, 0, nextChunkSize);

                nextChunk += nextChunkSize;
                binaryObject.AddDataChunk(binaryChunk);
            }
        }

        return binaryObject;
    }

    /**
     * Creates a UserBinaryObject from a byte array, which may be a large object.
     *
     * @param largbyteArray
     * @return
     */
    public static UserBinaryObject CreateUserBinaryObject(byte[] largbyteArray, int startIndex, int count)
    {
        UserBinaryObject binaryObject = null;
        if (largbyteArray != null)
        {
            int noOfChunks = count / LARGE_OBJECT_SIZE;
            noOfChunks += (count - (noOfChunks * LARGE_OBJECT_SIZE)) != 0 ? 1 : 0;

            binaryObject = new UserBinaryObject(noOfChunks);

            int nextChunk = 0;
            int nextChunkSize = 0;

            for (int i = 1; i <= noOfChunks; i++)
            {
                nextChunkSize = count - nextChunk;
                if (nextChunkSize > LARGE_OBJECT_SIZE)
                {
                    nextChunkSize = LARGE_OBJECT_SIZE;
                }

                byte[] binaryChunk = new byte[nextChunkSize];

                System.arraycopy(largbyteArray, startIndex, binaryChunk, 0, nextChunkSize);

                nextChunk += nextChunkSize;
                startIndex += nextChunkSize;

                binaryObject.AddDataChunk(binaryChunk);
            }
        }
        return binaryObject;
    }

    public final Object[] getData()
    {
        return _data.toArray(new Object[0]);
    }

    public final java.util.ArrayList<byte[]> getDataList()
    {
        java.util.ArrayList<byte[]> byteList = new java.util.ArrayList<byte[]>();
        for (Iterator it = _data.iterator(); it.hasNext();)
        {
            byte[] buffer = (byte[]) it.next();
            byteList.add(buffer);
        }

        return byteList;
    }
    

    /**
     * Re-assemle the individual binary chunks into a byte array. This method should not be called unless very necessary.
     *
     * @return
     */
    public final byte[] GetFullObject()
    {
        byte[] fullByteArray = null;

        if (getSize() > 0)
        {
            fullByteArray = new byte[getSize()];
            int nextIndex = 0;
            byte[] binarChunk = null;
            for (int i = 0; i < _data.size(); i++)
            {
                binarChunk = (byte[]) _data.get(i);
                if (binarChunk != null)
                {
                    System.arraycopy(binarChunk, 0, fullByteArray, nextIndex, binarChunk.length);
                    nextIndex += binarChunk.length;
                }
            }
        }
        return fullByteArray;
    }

    /**
     * Gets the size of the user binary object.
     * @return 
     */
    @Override
    public final int getSize()
    {
        int dataSize = 0;
        for (int i = 0; i < _noOfChunks; i++)
        {
            dataSize += ((byte[]) _data.get(i)).length;
        }
        return dataSize;
    }
    
    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        _noOfChunks = reader.readInt();
        _index = reader.readInt();

        if (_noOfChunks > 0)
        {
            _data = new java.util.ArrayList(_noOfChunks);
            for (int i = 0; i < _noOfChunks; i++)
            {
                Object tempVar = reader.readObject();
                _data.add(i, (byte[]) ((tempVar instanceof byte[]) ? tempVar : null));
            }
        }
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeInt(_noOfChunks);
        writer.writeInt(_index);
        for (int i = 0; i < _noOfChunks; i++)
        {
            writer.writeObject(_data.get(i));
        }
    }

    public final VirtualArray Read(int offset, int length)
    {
        VirtualArray vBuffer = null;
        int streamLength = getLength();

        if (offset >= streamLength)
        {
            return new VirtualArray(0);
        }
        if (offset + length > streamLength)
        {
            length -= (offset + length - streamLength);
        }

        VirtualArray vSrc = new VirtualArray(_data);
        vBuffer = new VirtualArray(length);
        VirtualIndex vSrcIndex = new VirtualIndex(offset);
        VirtualIndex vDstIndex = new VirtualIndex();
        VirtualArray.CopyData(vSrc, vSrcIndex, vBuffer, vDstIndex, length);

        return vBuffer;
    }

    public final void Write(VirtualArray vBuffer, int srcOffset, int dstOffset, int length)
    {
        if (vBuffer == null)
        {
            return;
        }
        {
            VirtualArray vDstArray = new VirtualArray(_data);
            VirtualArray.CopyData(vBuffer, new VirtualIndex(srcOffset), vDstArray, new VirtualIndex(dstOffset), length, true);
            _noOfChunks = _data.size();
        }
    }

    @Override
    public final int getLength(){
        return getSize();
    }

    @Override
    public final void setLength(int value){
    }  
    
    public int getInMemorySize(){
        return getLength() + BYTE_ARRAY_MEMORY_OVERHEAD;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
	    return true;
        byte[] arr2 = null;
        //byte[] is converted to UserBinaryObject in cacheEntry
	if (obj instanceof UserBinaryObject)
        {
            UserBinaryObject ubo = (UserBinaryObject) obj;
            if(ubo._data.size() != this._data.size())
                return false;
            arr2 = ubo.GetFullObject();
        }
        else if((obj instanceof byte[]))
            arr2 = (byte[]) obj;
        else
            return false;
        
        byte[] arr1 = this.GetFullObject();
        
        return Arrays.equals(arr1, arr2);
    }

}
