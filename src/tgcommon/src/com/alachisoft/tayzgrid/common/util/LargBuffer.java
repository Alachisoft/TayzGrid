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

public class LargBuffer {
	private byte[] _buffer;
	private BufferStatus _status = BufferStatus.Free;
	private int _id;

	public LargBuffer(byte[] buffer, int id) {
		_buffer = buffer;
		_id = id;
	}

	public LargBuffer(byte[] buffer, int id, BufferStatus status) {
		this(buffer, id);
		_status = status;
	}

	public final byte[] getBuffer() {
		return _buffer;
	}

	public final BufferStatus getStatus() {
		return _status;
	}
	public final void setStatus(BufferStatus value) {
		_status = value;
	}

	public final int getID() {
		return _id;
	}

	public final boolean getIsFree() {
		return _status == BufferStatus.Free;
	}
}
