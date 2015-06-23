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

package com.alachisoft.tayzgrid.storage;


import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.*;



public class StoreItem implements ICompactSerializable, Serializable {
	public Object Key;
	public Object Value;

	public StoreItem() {
	}
	public StoreItem(Object key, Object val) {
		Key = key;
		Value = val;
	}

	/** 
	 Convert a key-value pair to binary form.
	*/
	public static byte[] ToBinary(Object key, Object val, String cacheContext) throws IOException {
		StoreItem item = new StoreItem(key, val);
                ByteArrayOutputStream byteArrayOutPutStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutPutStream = new ObjectOutputStream(byteArrayOutPutStream);
                objectOutPutStream.writeObject(item);
                return byteArrayOutPutStream.toByteArray();
	}

	/** 
	 Convert a binary form of key-value pair to StoreItem
	*/
	public static StoreItem FromBinary(byte[] buffer, String cacheContext) throws IOException, ClassNotFoundException {
             ByteArrayInputStream byteArrayInPutStream = new ByteArrayInputStream(buffer);
                ObjectInputStream objectOutPutStream = new ObjectInputStream(byteArrayInPutStream);
                return (StoreItem)objectOutPutStream.readObject();
	}


    @Override
	public void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException {
		Key = reader.readObject();
		Value = reader.readObject();
	}

    @Override
	public void serialize(CacheObjectOutput writer)throws IOException  {
		writer.writeObject(Key);
		writer.writeObject(Value);
	}

}
