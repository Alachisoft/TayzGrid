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
 To be implemented by binary data structures.
*/
public interface IStreamItem {
	/** 
	 Copies the data from stream item into the Virtual buffer.
	 
	 @param offset offset in the stream item.
	 @param length length of the data to be read.
	 @return 
	*/
	VirtualArray Read(int offset, int length);

	/** 
	 Copies data from the virutal buffer into the stream item.
	 
	 @param vBuffer Data to be written to the stream item.
	 @param srcOffset Offset in the source buffer.
	 @param dstOffset Offset in the stream item.
	 @param length Length of data to be copied.
	*/
	void Write(VirtualArray vBuffer, int srcOffset, int dstOffset, int length);

	/** 
	 Gets/Sets the length of stram item.
	*/
	int getLength();
	void setLength(int value);
}
