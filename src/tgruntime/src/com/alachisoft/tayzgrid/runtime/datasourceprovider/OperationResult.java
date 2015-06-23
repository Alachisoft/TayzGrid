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

package com.alachisoft.tayzgrid.runtime.datasourceprovider;

public class OperationResult
{
	public enum Status
	{
		Success,
		Failure,
		FailureRetry,
		FailureDontRemove;

		public int getValue()
		{
			return this.ordinal();
		}

		public static Status forValue(int value)
		{
			return values()[value];
		}
	}
	private WriteOperation _writeOperation;
	private boolean _updateInCache = false;
	private OperationResult.Status _status = OperationResult.Status.Success;
	private String _errorMessage;
	private Exception _exception;

	public OperationResult(WriteOperation writeOperation, Status operationStatus)
	{
		this._writeOperation = writeOperation;
		this._status = operationStatus;
	}
	public OperationResult(WriteOperation writeOperation, Status operationStatus, String errorMessage)
	{
		this._writeOperation = writeOperation;
		this._status = operationStatus;
		this._errorMessage = errorMessage;
	}
	public OperationResult(WriteOperation writeOperation, Status operationStatus, Exception exception)
	{
		this._writeOperation = writeOperation;
		this._status = operationStatus;
		this._exception = exception;
	}
	/** 
	 Specify if item will be updated in cache store after write operation.
	*/
	public final boolean getUpdateInCache()
	{
		return _updateInCache;
	}
        /** 
	 Specify if item will be updated in cache store after write operation.
	*/
	public final void setUpdateInCache(boolean value)
	{
		_updateInCache = value;
	}
	/** 
	 Status of write operation.
	*/
	public final Status getDSOperationStatus()
	{
		return _status;
	}
        /** 
	 Status of write operation.
	*/
	public final void setDSOperationStatus(Status value)
	{
		_status = value;
	}
	/** 
	 Gets the Write operation.
	*/
	public final WriteOperation getOperation()
	{
		return _writeOperation;
	}
        /** 
	 Sets the Write operation.
	*/
	public final void setOperation(WriteOperation value)
	{
		_writeOperation = value;
	}
	/** 
	 Gets the exception associated with write operation.
	*/
	public final Exception getException()
	{
		return _exception;
	}
        /** 
	 Sets the exception associated with write operation.
	*/
	public final void setException(Exception value)
	{
		_exception = value;
	}
	/** 
	 Gets the error message associated with write operation.
	*/
	public final String getError()
	{
		return _errorMessage;
	}
        /** 
	 Sets the error message associated with write operation.
	*/
	public final void setError(String value)
	{
		_errorMessage = value;
	}
}