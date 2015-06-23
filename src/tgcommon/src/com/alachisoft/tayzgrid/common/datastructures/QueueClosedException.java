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

import com.alachisoft.tayzgrid.common.exceptions.TimeoutException;







/**  Elements are added at the tail and removed from the head. Class is thread-safe in that
 1 producer and 1 consumer may add/remove elements concurrently. The class is not
 explicitely designed for multiple producers or consumers. Implemented as a linked
 list, so that removal of an element at the head does not cause a right-shift of the
 remaining elements (as in a Vector-based implementation).
 
*/

public class QueueClosedException extends RuntimeException {
	/** 
	 Basic Exception
	*/
	public QueueClosedException() {
	}
	/** 
	 Exception with custom message
	 
	 @param msg Message to display when exception is thrown
	*/
	public QueueClosedException(String msg) {
		super(msg);
	}

	/** 
	 Creates a String representation of the Exception
	 
	 @return A String representation of the Exception
	*/
	public final String toString() {
		if (this.getMessage() != null) {
			return "QueueClosedException:" + this.getMessage();
		} else {
			return "QueueClosedException";
		}
	}
}
