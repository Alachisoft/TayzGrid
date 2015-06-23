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

import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.memcachedencoding.BinaryConverter;

public class BinaryResponseHeader extends BinaryHeader
{
	private BinaryResponseStatus _status = BinaryResponseStatus.values()[0];
	public final BinaryResponseStatus getStatus()
	{
		return _status;
	}
	public final void setStatus(BinaryResponseStatus value)
	{
		_status = value;
	}

	public BinaryResponseHeader()
	{
		_magic = Magic.Response;
	}

 
	public final byte[] ToByteArray()
	{
 
		byte[] header = new byte[24];

 
		header[0] = _magic.getValue();
 
		header[1] = _opcode.getValue();

 
		System.arraycopy(BinaryConverter.GetBytes((short)_keyLenght), 0, header, 2,2);

 
		header[4] = (byte)(_extraLenght & 255);
		header[5] = _dataType;
                
                System.arraycopy(BinaryConverter.GetBytes((short)_status.getValue()), 0, header, 6, 2);
		System.arraycopy(BinaryConverter.GetBytes(_totalBodyLength), 0, header, 8, 4);
		System.arraycopy(BinaryConverter.GetBytes(_opaque), 0, header, 12, 4);
		System.arraycopy(BinaryConverter.GetBytes(_cas), 0, header, 16, 8);

		return header;
	}
}