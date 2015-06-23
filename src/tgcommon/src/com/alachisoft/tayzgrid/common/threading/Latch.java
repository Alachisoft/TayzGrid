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

package com.alachisoft.tayzgrid.common.threading;

import com.alachisoft.tayzgrid.common.BitSet;

public class Latch {
	/**  A watchdog that prevents pre-init use of cache. 
	*/
	private Promise _initWatch = new Promise();
	/**  The runtime status of this node. 
	*/
	private BitSet _status = new BitSet();

	public Latch() {
	}
	public Latch(byte initialStatus) {
		_status.setData(initialStatus);
	}

	public final BitSet getStatus() {
		return _status;
	}


	/** 
	 Check is aall of the bits given in the bitset is set.
	 
	 @param status
	 @return 
	*/
	public final boolean AreAllBitsSet(byte status) {
		return _status.IsBitSet(status);
	}

	/** 
	 Check is any of the bits given in the bitset is set.
	 
	 @param status
	 @return 
	*/
	public final boolean IsAnyBitsSet(byte status) {
		return _status.IsAnyBitSet(status);
	}

	/**  The runtime status of this node. 
	*/
	public final void Clear() {
		synchronized (this) {
			_status.setData((byte) 0);
			_initWatch.SetResult(_status);
		}
	}

	/**  The runtime status of this node. 
	*/
	public final void SetStatusBit(byte bitsToSet, byte bitsToUnset) {
		synchronized (this) {
			_status.Set(bitsToSet, bitsToUnset);
			_initWatch.SetResult(_status);
		}
	}

	/** 
	 Blocks the thread until any of the two statii is reached.
	 
	 @param status
	*/
	public final void WaitForAny(byte status) {
		while (!IsAnyBitsSet(status)) {
			Object result = _initWatch.WaitResult(Long.MAX_VALUE);
			/** Result of a reset on the watch dog, done from dispose
			*/
			if (result == null) {
				return;
			}
		}
	}
        
        
        public final void WaitForAny(byte status, long timeout) {
            
            if (timeout > 0)
            {
		while (!IsAnyBitsSet(status)) {
			Object result = _initWatch.WaitResult(timeout);
			/** Result of a reset on the watch dog, done from dispose
			*/
			if (result == null) {
				return;
			}
		}
            }
	}

	/** 
	 Blocks the thread until any of the two statii is reached.
	 
	 @param status
	*/
	public final void WaitForAll(byte status) {
		while (!AreAllBitsSet(status)) {
			Object result = _initWatch.WaitResult(Long.MAX_VALUE);
			/** Result of a reset on the watch dog, done from dispose
			*/
			if (result == null) {
				return;
			}
		}
	}
}
