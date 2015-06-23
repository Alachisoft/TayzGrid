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

public class BinaryResponse
{
	private BinaryResponseHeader _header;
	public final BinaryResponseHeader getHeader()
	{
		return _header;
	}
	public final void setHeader(BinaryResponseHeader value)
	{
		_header = value;
	}

	private BinaryResponsePayload _payload;
	public final BinaryResponsePayload getPayLoad()
	{
		return _payload;
	}
	public final void setPayLoad(BinaryResponsePayload value)
	{
		_payload = value;
	}

	public BinaryResponse()
	{
		_header = new BinaryResponseHeader();
		_payload = new BinaryResponsePayload();
	}

	public final byte[] BuildResponse()
	{
		_header.setMagicByte(Magic.Response);
		_header.setKeyLength(getPayLoad().getKey().length);
		_header.setExtraLength(getPayLoad().getExtra().length);
		_header.setTotalBodyLenght(_header.getKeyLength() + _header.getExtraLength() + getPayLoad().getValue().length);

		byte[] response = new byte[24 + _header.getTotalBodyLenght()];
 
                System.arraycopy(_header.ToByteArray(), 0, response, 0, 24);
		System.arraycopy(getPayLoad().getExtra(), 0, response, 24, getPayLoad().getExtra().length);
		System.arraycopy(getPayLoad().getKey(), 0, response, 24 + getPayLoad().getExtra().length, getPayLoad().getKey().length);
		System.arraycopy(getPayLoad().getValue(), 0, response, 24 + getPayLoad().getExtra().length + getPayLoad().getKey().length, getPayLoad().getValue().length);
                return response;
	}
}