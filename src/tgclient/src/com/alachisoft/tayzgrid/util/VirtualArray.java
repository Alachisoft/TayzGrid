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

package com.alachisoft.tayzgrid.util;

import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.google.protobuf.ByteString;
import java.nio.Buffer;
import java.util.List;

public class VirtualArray {

    private List<ByteString> _baseArray;
    private long _size;

    public VirtualArray(List<ByteString> array)
    {
        _baseArray = array;
        for (int i = 0; i < array.size(); i++)
        {
            byte[] tmp = array.get(i).toByteArray();
            if (tmp != null) _size += tmp.length;
        }
    }


    public int CopyData(byte[] buffer, int offset, int length)
            throws IllegalArgumentException
    {
        if (offset + length > buffer.length)
            throw new IllegalArgumentException("Length plus offset is greater than buffer size");

        int dataToCopy = (int)(length >= _size ? _size : length);
        int dataCopied = dataToCopy;
        int i = 0;

        while (dataToCopy > 0)
        {
            byte[] binarChunk = (byte[])_baseArray.get(i).toByteArray();
            if (binarChunk != null)
            {
                int copyCount = Math.min(binarChunk.length,dataToCopy);

                HelperFxn.BlockCopy(binarChunk, 0, buffer, offset, copyCount);
                offset += copyCount;
                dataToCopy -= copyCount;
            }
            i++;
        }
        return dataCopied;
    }


}
