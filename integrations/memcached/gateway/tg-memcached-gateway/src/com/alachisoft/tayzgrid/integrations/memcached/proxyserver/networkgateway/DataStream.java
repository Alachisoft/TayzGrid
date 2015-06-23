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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway;

public class DataStream {

    private byte[] _buffer;
    private int _readIndex = 0;
    private int _writeIndex = 0;

    public DataStream() {
        _buffer = new byte[10240];
    }

    public DataStream(int capacity) {
        _buffer = new byte[capacity];
    }

    public final int Read(byte[] buffer, int offset, int count) {
        int bytesToRead = 0;
        synchronized (this) {
            int streamLength = _writeIndex - _readIndex;
            bytesToRead = count < streamLength ? count : streamLength;

            if (bytesToRead > 0) {
                System.arraycopy(_buffer, _readIndex, buffer, offset, bytesToRead);
                _readIndex += bytesToRead;
                if (_readIndex == _writeIndex) {
                    _readIndex = _writeIndex = 0;
                }
            }
        }
        return bytesToRead;
    }

    public final byte[] ReadAll() {

        byte[] data;
        synchronized (this) {
            int totalBytes = _writeIndex - _readIndex;
            data = new byte[totalBytes];
            System.arraycopy(_buffer, _readIndex, data, 0, totalBytes);
            _readIndex = _writeIndex = 0;
        }
        return data;
    }

    public final void Write(byte[] buffer) {
        if (buffer != null) {
            this.Write(buffer, 0, buffer.length);
        }
    }

    public final void Write(byte[] buffer, int offset, int count) {
        synchronized (this) {
            int remainingCapacity = _buffer.length - _writeIndex;
            if (remainingCapacity < count) {
                byte[] newBuffer = new byte[_buffer.length * 2];
                System.arraycopy(_buffer, _readIndex, newBuffer, 0, _buffer.length - _readIndex);
                _writeIndex = _buffer.length - _readIndex;
                _readIndex = 0;
                _buffer = newBuffer;
            }
            System.arraycopy(buffer, offset, _buffer, _writeIndex, count);
            _writeIndex += count;
        }
    }

    public final long getLenght() {
        synchronized (this) {
            return _writeIndex - _readIndex;
        }
    }

    public final void dispose() {
        
    }
}