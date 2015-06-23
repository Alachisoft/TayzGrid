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

import com.alachisoft.tayzgrid.common.protobuf.ManagementResponseProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.AggregateException;
import com.alachisoft.tayzgrid.runtime.exceptions.NotSupportedException;
import com.alachisoft.tayzgrid.runtime.exceptions.AttributeIndexNotDefined;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.MaxClientReachedException;
import com.alachisoft.tayzgrid.runtime.exceptions.StateTransferInProgressException;
import com.alachisoft.tayzgrid.runtime.exceptions.TypeIndexNotDefined;
import com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception;
import java.io.UnsupportedEncodingException;
public class ResponseHelper
{

    public static byte[] SerializeResponse(ResponseProtocol.Response command)
    {
        byte[] temp = command.toByteArray();
        byte[] bite = new byte[10 + temp.length];
        byte[] temp2 = null;
        try
        {
            temp2 = new Integer(temp.length).toString().getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException unsupportedEncodingException)
        {
        }
        System.arraycopy(temp2, 0, bite, 0, temp2.length);
        System.arraycopy(temp, 0, bite, 10, temp.length);
        return bite;
    }

     public static byte[] SerializeResponse(ManagementResponseProtocol.ManagementResponse.Builder command)
    {
        byte[] temp = command.build().toByteArray();
        byte[] bite = new byte[10 + temp.length];
        byte[] temp2 = null;
        try
        {
            temp2 = new Integer(temp.length).toString().getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException unsupportedEncodingException)
        {
        }
        System.arraycopy(temp2, 0, bite, 0, temp2.length);
        System.arraycopy(temp, 0, bite, 10, temp.length);
        return bite;
    }
    public static byte[] SerializeExceptionResponse(java.lang.Throwable exc, long requestId)
    {
        com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Builder ex = Exception.newBuilder();
        if (exc.getCause() != null)
        {
            StringBuilder builder = new StringBuilder(ex.getMessage());
            builder.append(builder.toString() == null || builder.toString().isEmpty() ? "" : " ");
            builder.append(exc.getCause().getMessage());
            ex.setMessage(builder.toString());
            ex.setException(exc.toString() + " " + exc.getCause().getMessage());
        }
        else
        {
            String message = exc.getMessage();
            if(message == null || message.isEmpty()){
                message = exc.toString();
            }

            ex.setMessage(message);
            ex.setException(exc.toString());
        }
        if (exc instanceof java.lang.reflect.InvocationTargetException && exc.getCause() != null)
        {
            exc = (java.lang.Exception) exc.getCause();
        }
        if (exc instanceof OperationFailedException)
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.OPERATIONFAILED);
        }
        else if (exc instanceof AggregateException)
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.AGGREGATE);
        }
        else if (exc instanceof ConfigurationException)
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.CONFIGURATION);
        }
        else if (exc instanceof NotSupportedException)
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.NOTSUPPORTED);
        }
        else if (exc instanceof TypeIndexNotDefined)
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.TYPE_INDEX_NOT_FOUND);
        }
        else if (exc instanceof AttributeIndexNotDefined)
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.ATTRIBUTE_INDEX_NOT_FOUND);
        }
        else if (exc instanceof com.alachisoft.tayzgrid.parser.TypeIndexNotDefined)
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.TYPE_INDEX_NOT_FOUND);
        }
        else if (exc instanceof com.alachisoft.tayzgrid.parser.AttributeIndexNotDefined)
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.ATTRIBUTE_INDEX_NOT_FOUND);
        }
        else if (exc instanceof StateTransferInProgressException)
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.STATE_TRANSFER_EXCEPTION);
        }
        else if (exc instanceof MaxClientReachedException)
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.MAX_CLIENTS_REACHED);
        }
        else
        {
            ex.setType(com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol.Exception.Type.GENERALFAILURE);
        }
//
        com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol.Response.Builder response = com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol.Response.newBuilder();
        response.setRequestId(requestId);
        response.setException(ex);
        response.setResponseType(ResponseProtocol.Response.Type.EXCEPTION);

        return SerializeResponse(response.build());
    }
}
