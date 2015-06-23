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

import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.*;




/** 
 Contains new hashmap and related information for client
*/
public final class NewHashmap implements ICompactSerializable{ // : ICompactSerializable
	private long _lastViewId;
	private java.util.HashMap _map;
	private java.util.ArrayList<String> _members;
	private byte[] _buffer;
	private boolean _updateMap = false;

	/** 
	 Default constructor
	*/
	public NewHashmap() {
	}

	public NewHashmap(long lastViewid, java.util.HashMap map, java.util.ArrayList<Address> members) {
		this._lastViewId = lastViewid;
		this._map = map;
		this._members = new java.util.ArrayList<String>(members.size());

		for (Address address : members) {
			this._members.add(address.getIpAddress().getHostAddress());
		}
	}

	/** 
	 Last view id that was published
	*/
	public long getLastViewId() {
		return this._lastViewId;
	}

	/** 
	 New hash map
	*/
	public java.util.HashMap getMap() {
		return this._map;
	}

	/** 
	 Just change the view id
	*/
	public boolean getUpdateMap() {
		return _updateMap;
	}
	public void setUpdateMap(boolean value) {
		_updateMap = value;
	}

	/** 
	 List of server members (string representation of IP addresses)
	*/
	public java.util.ArrayList getMembers() {
		return this._members;
	}

	/** 
	 Returned the serialized object of NewHashmap
	*/
	public byte[] getBuffer() {
		return this._buffer;
	}

	/** 
	 Serialize NewHashmap
	 
	 @param instance
	 @param serializationContext Serialization context used to serialize the object
	*/
	public static void Serialize(NewHashmap instance, String serializationContext, boolean updateClientMap) {
		java.util.HashMap mapInfo = null;
		if (instance != null) {
			mapInfo = new java.util.HashMap();
			mapInfo.put("ViewId", instance._lastViewId);
			mapInfo.put("Members", instance._members);
			mapInfo.put("Map", instance._map);
			mapInfo.put("UpdateMap", updateClientMap);

                        ObjectOutputStream objectOutPutStream = null;
                        try{
                            ByteArrayOutputStream byteArrayOutPutStream = new ByteArrayOutputStream();
                            objectOutPutStream = new ObjectOutputStream(byteArrayOutPutStream);
                            objectOutPutStream.writeObject(mapInfo);
                            instance._buffer = byteArrayOutPutStream.toByteArray();
                            objectOutPutStream.close();
                            
                        }catch(IOException e){
                        }finally{
                            if(objectOutPutStream != null){  
                                try{
                                    objectOutPutStream.close();
                                }catch(IOException ex){
                                }
                                    
                            }
                        }
		}
	}

	/** 
	 Deserialize NewHashmap
	 
	 @param serializationContext
	 @return 
	*/
	public static NewHashmap Deserialize(byte[] buffer, String serializationContext) {
		NewHashmap hashmap = null;

		if (buffer != null && buffer.length > 0) {
                        Object tempVar = null;
                        ObjectInputStream objectInputStream = null;
                        try{
                            objectInputStream = new ObjectInputStream(new ByteArrayInputStream(buffer));
                            tempVar = objectInputStream.readObject();
                            objectInputStream.close();
                            
                        }catch(ClassNotFoundException cls){
                        }catch(IOException e){
                        }finally{
                            if(objectInputStream != null){  
                                try{
                                    objectInputStream.close();
                                }catch(IOException ex){
                                }
                                    
                            }
                        }
                        
			java.util.HashMap map = (java.util.HashMap)((tempVar instanceof java.util.HashMap) ? tempVar : null);
			if (map != null) {
				hashmap = new NewHashmap();
				hashmap._lastViewId = (Long)map.get("ViewId");
				hashmap._members = (java.util.ArrayList)map.get("Members");
				hashmap._map = (java.util.HashMap)map.get("Map");
				hashmap._updateMap = (map.get("UpdateMap") != null) ? (Boolean)map.get("UpdateMap") : false;
			}
		}
		return hashmap;
	}



	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{ (");
		builder.append(this._lastViewId);
		builder.append(") ");
		builder.append("[");
		for (int i = 0; i < this.getMembers().size(); i++) {
			builder.append(this.getMembers().get(i));
			if (i < (this.getMembers().size() - 1)) {
				builder.append(",");
			}
		}
		builder.append("] }");
		return builder.toString();
	}

    @Override
    public void serialize(CacheObjectOutput writer) throws IOException {
       writer.writeLong(this._lastViewId);
       writer.writeObject(this._members);
       writer.writeObject(this._map);
       writer.writeBoolean(this._updateMap);
    }

    @Override
    public void deserialize(CacheObjectInput reader) throws IOException, ClassNotFoundException {
           this._lastViewId=reader.readLong();
           this._members=(java.util.ArrayList)reader.readObject();
           this._map=(java.util.HashMap)reader.readObject();
           this._updateMap=reader.readBoolean();
    }
}
