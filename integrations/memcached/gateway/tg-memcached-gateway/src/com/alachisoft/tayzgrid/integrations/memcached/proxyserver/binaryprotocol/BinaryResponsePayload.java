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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.binaryprotocol;

public class BinaryResponsePayload
{
 
	private byte[] _extra;
 
	private byte[] _key;
 
	private byte[] _value;
 
	public final byte[] getExtra()
	{
		return _extra == null ? new byte[] { } : _extra;
	}
 
	public final void setExtra(byte[] value)
	{
		_extra = value;
	}

 
	public final byte[] getKey()
	{
		return _key == null ? new byte[] { } : _key;
	}
 
	public final void setKey(byte[] value)
	{
		_key = value;
	}

 
	public final byte[] getValue()
	{
		return _value == null ? new byte[] { } : _value;
	}
 
	public final void setValue(byte[] value)
	{
		_value = value;
	}
}