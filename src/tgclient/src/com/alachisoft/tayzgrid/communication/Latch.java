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

package com.alachisoft.tayzgrid.communication;

import com.alachisoft.tayzgrid.communication.Promise;
import java.util.BitSet;



///
/// </summary>
public class Latch {
    /// <summary> A watchdog that prevents pre-init use of cache. </summary>
    private Promise _initWatch = new Promise();
    /// <summary> The runtime status of this node. </summary>
    private byte _status = 0x00;

    public Latch() { }
    public Latch(byte initialStatus) { _status = initialStatus; }

    public byte getStatus() {
        return _status;
    }


    /// <summary>
    /// Check is aall of the bits given in the bitset is set.
    /// </summary>
    /// <param name="status"></param>
    /// <returns></returns>
    public boolean areAllBitsSet(byte status) {
        return (_status & status ) == status;
    }

    /// <summary>
    /// Check is any of the bits given in the bitset is set.
    /// </summary>
    /// <param name="status"></param>
    /// <returns></returns>
    public boolean isAnyBitsSet(byte status) {
        return (_status & status ) != 0;
    }

    /// <summary> The runtime status of this node. </summary>
    public synchronized void clear() {
            _status = 0x00;
            _initWatch.setResult(_status);
    }

    /// <summary> The runtime status of this node. </summary>
    public synchronized void setStatusBit(byte bitsToSet, byte bitsToUnset) {
            _status |= bitsToSet;
            _status &= (byte)(~bitsToUnset & 0xff);
            _initWatch.setResult(_status);
    }

    /// <summary>
    /// Blocks the thread until any of the two statii is reached.
    /// </summary>
    /// <param name="status"></param>
    public void waitForAny(byte status) {
        while (!isAnyBitsSet(status)) {
            Object result = _initWatch.waitResult(0);
          
            if (result == null) {
                return;
            }
        }
    }

    /// <summary>
    /// Blocks the thread until any of the two statii is reached.
    /// </summary>
    /// <param name="status"></param>
    public void waitForAll(byte status) {
        while (!areAllBitsSet(status)) {
            Object result = _initWatch.waitResult(0);
           
            if (result == null) {
                return;
            }
        }
    }
}

