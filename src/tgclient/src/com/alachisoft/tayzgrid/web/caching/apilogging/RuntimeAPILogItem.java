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

package com.alachisoft.tayzgrid.web.caching.apilogging;

public class RuntimeAPILogItem
{
	private boolean _isForSentObject = true;
	public final boolean getIsForSentObject()
	{
		return _isForSentObject;
	}
	public final void setIsForSentObject(boolean value)
	{
		_isForSentObject = value;
	}

	private boolean _isBulk = false;
	public final boolean getIsBulk()
	{
		return _isBulk;
	}
	public final void setIsBulk(boolean value)
	{
		_isBulk = value;
	}

	private int _noOfObjects = 1;
	public final int getNoOfObjects()
	{
		return _noOfObjects;
	}
	public final void setNoOfObjects(int value)
	{
		_noOfObjects = value;
	}

	private long _sizeOfObject = 0;
	public final long getSizeOfObject()
	{
		return _sizeOfObject;
	}
	public final void setSizeOfObject(long value)
	{
		_sizeOfObject = value;
	}

	private boolean _encryptionEnabled = false;
	public final boolean getEncryptionEnabled()
	{
		return _encryptionEnabled;
	}
	public final void setEncryptionEnabled(boolean value)
	{
		_encryptionEnabled = value;
	}

	private long _sizeOfEncryptedObject = 0;
	public final long getSizeOfEncryptedObject()
	{
		return _sizeOfEncryptedObject;
	}
	public final void setSizeOfEncryptedObject(long value)
	{
		_sizeOfEncryptedObject = value;
	}

	private long _sizeOfCompressedObject = 0;
	public final long getSizeOfCompressedObject()
	{
		return _sizeOfCompressedObject;
	}
	public final void setSizeOfCompressedObject(long value)
	{
		_sizeOfCompressedObject = value;
	}
}
