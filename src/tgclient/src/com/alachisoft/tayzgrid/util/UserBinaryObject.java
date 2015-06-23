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

import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserBinaryObject {

    private static final int LARGE_OBJECT_SIZE = 79 * 1024;
    private int noOfChunks;
    private int index;
    private List<byte[]> data;

    public UserBinaryObject(int noOfChunks) {
        this.noOfChunks = noOfChunks;
        this.data = new ArrayList<byte[]>(this.noOfChunks);
    }

    public UserBinaryObject(List<byte[]> data) {
        int dataSize = data.size();

        this.noOfChunks = dataSize;
        this.data = new ArrayList<byte[]>(this.noOfChunks);
        for (int i = 0; i < dataSize; i++) {
            this.data.add(data.get(i));
        }
    }

    public void addDataChunk(byte[] dataChunk) {
        if (this.data != null && this.index < this.noOfChunks) {
            this.data.add(this.index, dataChunk);
            this.index++;
        }
    }

    public List<byte[]> getDataList() {
        return this.data;
    }

    public byte[] getFullObject() throws IOException {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            int chunkSize = this.data.size();
            for (int i = 0; i < chunkSize; i++) {
                stream.write(this.data.get(i));
            }
            return stream.toByteArray();
        } finally {
            stream.close();
        }
    }

    public int size() {

        int dataSize = 0;
        for (int i = 0; i < this.noOfChunks; i++) {
            dataSize += this.data.get(i).length;
        }
        return dataSize;
    }

    public static UserBinaryObject createUserBinaryObject(byte[] largbyteArray) {
        if (largbyteArray != null) {
            int noOfChunks = largbyteArray.length / LARGE_OBJECT_SIZE;
            noOfChunks += (largbyteArray.length - (noOfChunks * LARGE_OBJECT_SIZE)) != 0 ? 1 : 0;

            UserBinaryObject binaryObject = new UserBinaryObject(noOfChunks);

            int nextChunk = 0;
            int nextChunkSize = 0;

            for (int i = 1; i <= noOfChunks; i++) {
                nextChunkSize = largbyteArray.length - nextChunk;
                if (nextChunkSize > LARGE_OBJECT_SIZE) {
                    nextChunkSize = LARGE_OBJECT_SIZE;
                }

                byte[] binaryChunk = new byte[nextChunkSize];

                System.arraycopy(largbyteArray, nextChunk, binaryChunk, 0, nextChunkSize);
                nextChunk += nextChunkSize;
                binaryObject.addDataChunk(binaryChunk);
            }

            return binaryObject;
        }
        return null;
    }

    public static UserBinaryObject createUserBinaryObject(List<ByteString> list) {
        if (list != null) {
            int size = list.size();
            if (size > 0) {
                List<byte[]> userData = new ArrayList<byte[]>(size);
                for (int i = 0; i < size; i++) {
                    userData.add(list.get(i).toByteArray());
                }
                return new UserBinaryObject(userData);
            }
        }
        return null;
    }
}
