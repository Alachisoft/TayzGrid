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

package com.alachisoft.tayzgrid.socketserver.command;

import com.alachisoft.tayzgrid.socketserver.ExceptionType;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.AggregateException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationNotSupportedException;
import com.alachisoft.tayzgrid.socketserver.command.OperationResult;
import com.alachisoft.tayzgrid.runtime.exceptions.MaxClientReachedException;
import java.io.IOException;
public abstract class CommandBase
{
    protected String immatureId = "-2";


    protected Object _userData;

    protected long forcedViewId = -5;

    public OperationResult getOperationResult()
    {
        return OperationResult.Failure;
    }
    public int getOperations()
    {
        return 1;
    }

    protected java.util.List<byte[]> _serializedResponsePackets = new java.util.ArrayList<byte[]>();

    public java.util.List<byte[]> getSerializedResponsePackets()
    {
        return _serializedResponsePackets;
    }


    public boolean getCanHaveLargedata()
    {
        return false;
    }
    public boolean getIsBulkOperation()
    {
        return false;
    }

    public Object getUserData()
    {
        return _userData;
    }
    public void setUserData(Object value)
    {
        _userData = value;
    }

    //PROTOBUF
    public abstract void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command)throws Exception;

    protected final void UpdateDelimIndexes(tangible.RefObject<String> command, char delim, tangible.RefObject<Integer> beginQuoteIndex, tangible.RefObject<Integer> endQuoteIndex)
    {
        beginQuoteIndex.argvalue = endQuoteIndex.argvalue;
        endQuoteIndex.argvalue = command.argvalue.indexOf(delim, beginQuoteIndex.argvalue + 1);
    }

    protected final String ExceptionPacket(Exception exc, String requestId)
    {
        byte exceptionId = 0;

        if (exc instanceof OperationFailedException)
        {
            exceptionId = (byte)ExceptionType.OPERATIONFAILED.getValue();
        }
        else if (exc instanceof AggregateException)
        {
            exceptionId = (byte)ExceptionType.AGGREGATE.getValue();
        }
        else if (exc instanceof ConfigurationException)
        {
            exceptionId = (byte)ExceptionType.CONFIGURATION.getValue();
        }
        else if (exc instanceof OperationNotSupportedException)
        {
            exceptionId =(byte) ExceptionType.NOTSUPPORTED.getValue();
        }
        else if (exc instanceof MaxClientReachedException)
        {
            exceptionId =(byte) ExceptionType.MAX_CLIENTS_REACHED.getValue();

        }
        else
        {
            exceptionId = (byte)ExceptionType.GENERALFAILURE.getValue();
        }

        return "EXCEPTION \"" + requestId + "\"" + exceptionId + "\"";
    }

    protected final byte[] ExceptionMessage(Exception exc)
    {
        if (exc instanceof AggregateException)
        {
            Exception[] innerExceptions = ((AggregateException)exc).getExceptions();
            if (innerExceptions[0] != null)
            {
                return com.alachisoft.tayzgrid.socketserver.util.HelperFxn.ToBytes(innerExceptions[0].toString());
            }
        }
        return com.alachisoft.tayzgrid.socketserver.util.HelperFxn.ToBytes(exc.toString());
    }

    protected final byte[] ParsingExceptionMessage(Exception exc)
    {
        return com.alachisoft.tayzgrid.socketserver.util.HelperFxn.ToBytes("ParsingException: " + exc.toString());
    }



}
