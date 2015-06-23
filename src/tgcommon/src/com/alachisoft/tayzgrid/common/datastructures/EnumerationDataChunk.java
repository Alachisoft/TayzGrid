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


import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class EnumerationDataChunk implements ICompactSerializable {
	private java.util.ArrayList _data;
	private EnumerationPointer _pointer;

	public final java.util.ArrayList getData() {
		return _data;
	}
	public final void setData(java.util.ArrayList value) {
		_data = value;
	}

	public final EnumerationPointer getPointer() {
		return _pointer;
	}
	public final void setPointer(EnumerationPointer value) {
		_pointer = value;
	}

	public final boolean isLastChunk() {
		return _pointer.getHasFinished();
	}


	public  void deserialize(CacheObjectInput reader)throws IOException,ClassNotFoundException {
		_data = (java.util.ArrayList)reader.readObject();
		_pointer = (EnumerationPointer)reader.readObject();
	}

	public  void serialize(CacheObjectOutput writer) throws IOException {
		writer.writeObject(_data);
		writer.writeObject(_pointer);
	}





}
