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

package com.alachisoft.tayzgrid.common.datastructures;

/**
 * This class is converted to use List instead of .net Array
 * Buffer.BlockCopy is replaced by System.arrayCopy of JAVA High level testing
 * is required to make sure if required functionality is achieved.
 * Serialization is now complied to NCacheSerializtion(JAVA)
 *
 */
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VirtualArray implements ICompactSerializable {

    private java.util.List _baseArray;
    private long _size;
    private static final int maxSize = 79 * 1024;
  
   
    @Deprecated
    // Only used for ICompactSerilization 
    public VirtualArray(){
       
   }
    
    public VirtualArray(int size) {
        _size = size;
        int largeObjectSize = 79 * 1024;
        int noOfChunks = (int) (size / largeObjectSize);
        noOfChunks += (size - (noOfChunks * largeObjectSize)) != 0 ? 1 : 0;
        _baseArray = new ArrayList<Object>(noOfChunks);

        for (int i = 0; i < noOfChunks; i++) {
            byte[] buffer = null;
            if (size >= maxSize) {
                buffer = new byte[maxSize];
                size -= maxSize;
            } else {
                buffer = new byte[size];
            }
            _baseArray.add(i, buffer);

        }

    }

    public VirtualArray(java.util.List array) {
        _baseArray = array;

        //Size calc only
        for (int i = 0; i < array.size(); i++) {
            byte[] tmp = (byte[]) ((array.get(i) instanceof byte[]) ? array.get(i) : null);
            if (tmp != null) {
                _size += tmp.length;
            }
        }
    }


    public final byte GetValueAt(VirtualIndex vIndex) {
        byte[] arr = (byte[]) ((_baseArray.get(vIndex.getYIndex()) instanceof byte[]) ? _baseArray.get(vIndex.getYIndex()) : null);
        return (byte) arr[vIndex.getXIndex()];
    }

    public final void SetValueAt(VirtualIndex vIndex, byte value) {
        byte[] arr = (byte[]) ((_baseArray.get(vIndex.getYIndex()) instanceof byte[]) ? _baseArray.get(vIndex.getYIndex()) : null);
        arr[vIndex.getXIndex()] = value;
    }

    public static void CopyData(VirtualArray src, VirtualIndex srcIndex, VirtualArray dst, VirtualIndex dstIndex, int count) {
        CopyData(src, srcIndex, dst, dstIndex, count, false);
    }

    public static void CopyData(VirtualArray src, VirtualIndex srcIndex, VirtualArray dst, VirtualIndex dstIndex, int count, boolean allowExpantion)
    {
        if (src == null || dst == null || srcIndex == null || dstIndex == null)
        {
            return;
        }

        if (src.getSize() < srcIndex.getIndexValue())
        {
            throw new IndexOutOfBoundsException();
        }


        srcIndex = srcIndex.clone();
        dstIndex = dstIndex.clone();

        while (count > 0)
        {

            byte[] arr = ((src._baseArray.get(srcIndex.getYIndex()).getClass().isArray()) ? (byte[])src._baseArray.get(srcIndex.getYIndex()) : null);
            int copyCount = maxSize - srcIndex.getXIndex();
            if (copyCount > count)
            {
                copyCount = count;
            }

            byte[] dstArr = null;
            if (dst._baseArray.size() > dstIndex.getYIndex())
            {
                dstArr = ((dst._baseArray.get(dstIndex.getYIndex()).getClass().isArray()) ? (byte[])dst._baseArray.get(dstIndex.getYIndex()) : null);
            }

            int accomdateble = (maxSize - dstIndex.getXIndex());

            if (accomdateble > copyCount)
            {
                accomdateble = copyCount;
            }
            if ((dstArr == null || accomdateble > (dstArr.length - dstIndex.getXIndex())) && allowExpantion)
            {
                if (dstArr == null)
                {
                    dstArr = new byte[accomdateble];

                    dst._baseArray.add(dstArr);
                }
                else
                {
                    /**
                     * Buffer.BlockCopy replaced by System.arrayCopy
                     */
                    byte[] tmpArray = new byte[accomdateble + dstArr.length - (dstArr.length - dstIndex.getXIndex())];

                    System.arraycopy(dstArr, 0, tmpArray, 0, dstArr.length);


                    //nothing
                    dstArr=tmpArray;
                    dst._baseArray.set(dstIndex.getYIndex(), dstArr);
                }

            }
            /**
             * Buffer.BlockCopy replaced by System.arrayCopy
             */
            System.arraycopy(arr, srcIndex.getXIndex(), dstArr, dstIndex.getXIndex(), accomdateble);
            count -= accomdateble;
            srcIndex.IncrementBy(accomdateble);
            dstIndex.IncrementBy(accomdateble);
        }
    }
    public final int CopyData(byte[] buffer, int offset, int length) {
        if (offset + length > buffer.length) {
            throw new IllegalArgumentException("Length plus offset is greater than buffer size");
        }

        int dataToCopy = (int) (length >= getSize() ? getSize() : length);
        int dataCopied = dataToCopy;
        int i = 0;
        while (dataToCopy > 0) {

            byte[] binarChunk = (byte[]) _baseArray.get(i);
            if (binarChunk != null) {
                /**
                * Buffer.BlockCopy replaced by System.arrayCopy
                */
                int copyCount = Math.min(binarChunk.length, dataToCopy);
                System.arraycopy(binarChunk, 0, buffer, offset, copyCount);
                offset += copyCount;
                dataToCopy -= copyCount;
            }
            i++;
        }
        return dataCopied;
    }

    public final java.util.List getBaseArray() {
        return _baseArray;
    }

    public final long getSize() {
        return _size;
    }

    @Override
    public final void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException {
        _size = reader.readLong();
        Object tempVar = reader.readObject();
        _baseArray = (java.util.List) ((tempVar instanceof java.util.List) ? tempVar : null);
    }

    @Override
    public final void serialize(CacheObjectOutput writer) throws IOException {
        writer.writeLong(_size);
        writer.writeObject(_baseArray);
    }

}
