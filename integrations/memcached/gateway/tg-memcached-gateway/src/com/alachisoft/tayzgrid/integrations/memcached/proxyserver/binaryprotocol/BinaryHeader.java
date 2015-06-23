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

import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.memcachedencoding.BinaryConverter;

public abstract class BinaryHeader
{
	protected Magic _magic;
	public final Magic getMagicByte()
	{
		return _magic;
	}
	public final void setMagicByte(Magic value)
	{
		_magic = value;
	}

	protected Opcode _opcode;
        
	public final Opcode getOpcode()
	{
		return _opcode;
	}
	public final void setOpcode(Opcode value)
	{
		_opcode = value;
	}

	protected int _keyLenght = 0;
	public final int getKeyLength()
	{
		return _keyLenght;
	}
	public final void setKeyLength(int value)
	{
		_keyLenght = value;
	}

	protected int _extraLenght = 0;
	public final int getExtraLength()
	{
		return _extraLenght;
	}
	public final void setExtraLength(int value)
	{
		_extraLenght = value;
	}
 
	protected byte _dataType;
 
	public final byte getDataType()
	{
		return _dataType;
	}
 
	public final void setDataType(byte value)
	{
		_dataType = value;
	}

	protected int _totalBodyLength = 0;
	public final int getTotalBodyLenght()
	{
		return _totalBodyLength;
	}
	public final void setTotalBodyLenght(int value)
	{
		_totalBodyLength = value;
	}

	protected int _opaque;
	public final int getOpaque()
	{
		return _opaque;
	}
	public final void setOpaque(int value)
	{
		_opaque = value;
	}

 
	protected long _cas;
	public final long getCAS()
	{
		return _cas;
	}
 
	public final void setCAS(long value)
	{
		_cas = value;
	}

	public final int getValueLength()
	{
		return this._totalBodyLength - this._keyLenght - this._extraLenght;
	}

	public BinaryHeader()
	{
	}

 
	public BinaryHeader(byte[] header)
	{
		if (header.length < 24)
		{
			throw new IllegalArgumentException("Length of header should be 24 bytes.");
		}

		_magic = Magic.forValue(header[0]);
		_opcode = Opcode.forValue(header[1]);
                
		_keyLenght = (int)BinaryConverter.ToUInt16(header, 2);
		_extraLenght = (int)header[4];

		_dataType = header[5];
		//6,7 reserved
		_totalBodyLength = BinaryConverter.ToInt32(header, 8);
		_opaque = BinaryConverter.ToInt32(header, 12);
		_cas = BinaryConverter.ToInt64(header, 16);
	}
}