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

package com.alachisoft.tayzgrid.common.util;

import com.alachisoft.tayzgrid.common.enums.EventType;
import com.alachisoft.tayzgrid.common.logger.EventLogger;

public class BufferPool {
	private static final int DEFAULT_BUFFER_SIZE = 512 * 1024;
	private static final int DEFAULT_POOL_SIZE = 40;

	private static int _bufferSize = DEFAULT_BUFFER_SIZE;
	private static int _poolSize = DEFAULT_POOL_SIZE;
	private static java.util.LinkedList _pool = new java.util.LinkedList();
	private static Object _sync_lock = new Object();

	static {
		String temp = "";

		if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(temp)) {
			int bufferSize = DEFAULT_BUFFER_SIZE;
			tangible.RefObject<Integer> tempRef_bufferSize = new tangible.RefObject<Integer>(bufferSize);
                        boolean tempVar =  false;
                        try{
                            Integer.parseInt(temp);
                            tempVar = true;
                        }catch(NumberFormatException ex){}
				bufferSize = tempRef_bufferSize.argvalue;
			if (tempVar) {
				_bufferSize = bufferSize;
			}
		}
		temp = "";
		if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(temp)) {
			int poolSize = DEFAULT_POOL_SIZE;
			tangible.RefObject<Integer> tempRef_poolSize = new tangible.RefObject<Integer>(poolSize);
                        boolean tempVar2 =  false;
                        try{
                            Integer.parseInt(temp);
                            tempVar2 = true;
                        }catch(NumberFormatException ex){}
				poolSize = tempRef_poolSize.argvalue;
			if (tempVar2) {
				_poolSize = poolSize;
			}
		}
		AllocateNewBuffers();
	}

	private static void AllocateNewBuffers() {
		synchronized (_sync_lock) {
			java.util.ArrayList newBuffers = new java.util.ArrayList();
			for (int i = 1; i <= _poolSize; i++) {
				try {
					byte[] buffer = new byte[_bufferSize];
					_pool.offer(buffer);
				} catch (OutOfMemoryError e) {
					EventLogger.LogEvent("BufferPool can't allocate new buffer", EventType.ERROR);
				} catch (RuntimeException e2) {

				}
			}
		}
	}

	/** 
	 Gets a larg buffer from the pool.
	 
	 @return 
	*/
	public static byte[] CheckoutBuffer(int size) {
		if (size != -1 && size > _bufferSize) {
		   return new byte[size];
		}

		synchronized (_sync_lock) {
			if (_pool.isEmpty()) {
				AllocateNewBuffers();
			}

			if (_pool.size() > 0) {
				Object tempVar = _pool.poll();
				return (byte[])((tempVar instanceof byte[]) ? tempVar : null);
			} else {
				return new byte[_bufferSize];
			}
		}
	}

	/** 
	 Frees a buffer allocated from the pool.
	 
	 @param buffer
	*/
	public static void CheckinBuffer(byte[] buffer) {
		if (buffer == null) {
			return;
		}
		if (buffer.length > _bufferSize) { //This is not a pool buffer.
			return;
		}
		synchronized (_sync_lock) {
			_pool.offer(buffer);
		}
	}
	/** 
	 Releases all the buffers.
	*/
	public static void Clear() {
		synchronized (_sync_lock) {
			_pool.clear();
		}
	}
}
