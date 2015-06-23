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
package com.alachisoft.tayzgrid.communication;

import com.alachisoft.tayzgrid.common.datastructures.EnumerationDataChunk;


import java.util.HashMap;
import java.util.Map;
import com.alachisoft.tayzgrid.command.Command;
import com.alachisoft.tayzgrid.command.CommandResponse;
import com.alachisoft.tayzgrid.common.protobuf.ExceptionProtocol;
import com.alachisoft.tayzgrid.runtime.exceptions.ConnectionException;
import com.alachisoft.tayzgrid.util.DictionaryEntry;
import com.alachisoft.tayzgrid.common.enums.AggregateFunctionType;
import com.alachisoft.tayzgrid.common.net.Address;
import com.alachisoft.tayzgrid.command.RequestType;
import com.alachisoft.tayzgrid.common.mapreduce.TaskEnumeratorResult;
import java.util.*;

public class Request
{

    private long _requestId = -1;
    private boolean _isAsync = false;
    private boolean _isBulk = false;
    private boolean _isAsyncCallbackSpecified = false;
    private String _name;
    private CommandResponse _finalResponse = null;
    private final Object _responseMutex = new Object();
    private com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol.Response.Type _type;
    private HashMap<Address, ResponseList> _responses = new HashMap<Address, ResponseList>();
    private HashMap<Address, Command> _commands = new HashMap<Address, Command>();
    private HashMap<Address, EnumerationDataChunk> _chunks = new HashMap<Address, EnumerationDataChunk>();
    private long _timeout = 90000;
    private boolean _isrequestTimeoutReset = false;
    private boolean _resend = false;
    private Address _reRoutedAddress = null;

    public Request(boolean isBulk, long timeout)
    {
        _isBulk = isBulk;
        _timeout = timeout;
    }
    
       public final boolean getIsRequestTimeoutReset()
   {
	   return _isrequestTimeoutReset;
   }
   public final void setIsRequestTimeoutReset(boolean value)
   {
	   _isrequestTimeoutReset = value;
   }

    public final boolean getIsResendReuest()
    {
            return _resend;
    }
    public final void setIsResendReuest(boolean value)
    {
            _resend = value;
    }

    public final long getRequestTimeout()
    {
            return _timeout;
    }
    public final void setRequestTimeout(long value)
    {
            _timeout = value;
    }
    
   public final Address getReRoutedAddress()
   {
         return _reRoutedAddress;
   }
   public final void setReRoutedAddress(Address value)
   {
         _reRoutedAddress = value;
   }
   
    public final RequestType getCommandRequestType()
    {
	 for (Command command : _commands.values())
	 {
                       
		 return command.getCommandRequestType();
	 }

	 return RequestType.InternalCommand;
    }

    public final boolean IsDedicated(long viewId)
    {
        for (Command command : _commands.values())
        {
                return command.getClientLastViewId() == viewId;
        }

        return false;
    }

    public String getName()
    {
        return _name;
    }

    public void setName(String _name)
    {
        this._name = _name;
    }

    public boolean isAsyncCallbackSpecified()
    {
        return _isAsyncCallbackSpecified;
    }

    public void setIsAsyncCallbackSpecified(boolean _isAsyncCallbackSpecified)
    {
        this._isAsyncCallbackSpecified = _isAsyncCallbackSpecified;
    }

    public boolean isAsync()
    {
        return _isAsync;
    }

    public void setIsAsync(boolean _isAsync)
    {
        this._isAsync = _isAsync;
    }

    public boolean IsBulk()
    {
        return _commands.size() > 1 || _isBulk;
    }

    public long getRequestId()
    {
        return _requestId;
    }

    public void setRequestId(long id)
    {
        _requestId = id;
        for (Command command : _commands.values())
        {
            command.setRequestId(id);
        }
    }

    public HashMap<Address, Command> getCommands()
    {
        return _commands;
    }

    public int getNumberOfCompleteResponses()
    {
        int count = 0;
        synchronized (_responseMutex)
        {
            for (ResponseList responseList : _responses.values())
            {
                if (responseList.IsComplete())
                {
                    count++;
                }
            }
        }
        return count;
    }

    public String getTimeoutMessage()
    {
        StringBuilder sb = new StringBuilder("Operation timed out. No response from the server(s).");
        sb.append(" [");
        synchronized (_responseMutex)
        {
            for (Address ip : _commands.keySet())
            {
                if (!_responses.containsKey(ip))
                {
                    sb.append(ip + ", ");
                }
                else
                {
                    ResponseList response = _responses.get(ip);
                    if (!response.IsComplete())
                    {
                        sb.append(ip + ", ");
                    }
                }
            }
        }
        int length = sb.length();
        sb.delete(length - 2, length);
        sb.append("]");
        return sb.toString();
    }

    public CommandResponse getResponse()
    {
        synchronized (_responseMutex)
        {
            for (Map.Entry<Address, ResponseList> responses : _responses.entrySet())
            {
                for (CommandResponse rsp : responses.getValue().getResponses().values())
                {
                    //in case exception is not thrown from 1st server.
                    if (rsp.getExpValue() != null && rsp.getExpValue() != ExceptionProtocol.Exception.Type.STATE_TRANSFER_EXCEPTION && rsp.getExpValue()
                            != ExceptionProtocol.Exception.Type.ATTRIBUTE_INDEX_NOT_FOUND && rsp.getExpValue() != ExceptionProtocol.Exception.Type.TYPE_INDEX_NOT_FOUND)
                    {
                        _finalResponse = rsp;
                        return _finalResponse;
                    }
                    MergeResponse(responses.getKey(), rsp);
                }
            }
        }
        return _finalResponse;
    }

    public boolean allResponsesRecieved()
    {
        return getNumberOfCompleteResponses() == _commands.size();
    }

    public void addCommand(Address address, Command command)
    {
        _name = command.getCommandName();
        command.setParent(this);

        if (!_commands.containsKey(address))
        {
            _commands.put(address, command);
        }
    }

    public void addResponse(Address address, CommandResponse response)
    {
        _type = response.getType();
        synchronized (_responseMutex)
        {
            if (_responses.containsKey(address))
            {
                ResponseList responseList = _responses.get(address);
                if (!responseList.IsComplete())
                {
                    responseList.AddResponse(response);
                }
                else
                {
                    if (_reRoutedAddress != null && !_reRoutedAddress.equals(address))
                    {
                            if (!_responses.containsKey(_reRoutedAddress))
                            {
                                    ResponseList rspList = new ResponseList();
                                    if (!rspList.IsComplete())
                                    {
                                            rspList.AddResponse(response);
                                    }

                                    _responses.put(_reRoutedAddress, rspList);
                            }
                            else
                            {
                                    responseList = _responses.get(_reRoutedAddress);
                                    if (!responseList.IsComplete())
                                    {
                                            responseList.AddResponse(response);
                                    }
                            }
                    }
               }
            }
        }
    }

    public void initializeResponse(Address address)
    {
        synchronized (_responseMutex)
        {
            if (!_responses.containsKey(address))
            {
                _responses.put(address, new ResponseList());
            }
        }
    }

    public boolean removeResponse(Address address)
    {
        synchronized (_responseMutex)
        {
            _responses.remove(address);
            boolean removeRequestFromTable = _responses.size() == 0;
            return removeRequestFromTable;
        }
    }

    public boolean expectingResponseFrom(Address address)
    {
        synchronized (_responseMutex)
        {
            boolean result = _responses.containsKey(address);
            return result;
        }
    }

    public void reset(Address ip)
    {
        synchronized (_responseMutex)
        {
            if (_responses.containsKey(ip))
            {
                ResponseList responseList = _responses.get(ip);
                responseList.Clear();
                responseList.AddResponse(new CommandResponse(true, ip));
                _responses.put(ip, responseList);
            }
        }
    }

    public void ResetFailedResponse(Address ip)
    {
        synchronized (_responseMutex)
        {
            if (_responses.containsKey(ip))
            {
                ResponseList responseList = _responses.get(ip);
                responseList.Clear();
                responseList.AddResponse(new CommandResponse(true, ip));
            }
        }
    }

    public void SetAggregateFunctionResult()
    {
        switch (_finalResponse.getQueryResultSet().getAggregateFunctionType())
        {
            case MAX:
                _finalResponse.getQueryResultSet().setAggregateFunctionResult(new DictionaryEntry<Object, Object>(AggregateFunctionType.MAX, _finalResponse.getQueryResultSet().getAggregateFunctionResult().getValue()));
                break;
            case MIN:
                _finalResponse.getQueryResultSet().setAggregateFunctionResult(new DictionaryEntry<Object, Object>(AggregateFunctionType.MIN, _finalResponse.getQueryResultSet().getAggregateFunctionResult().getValue()));
                break;
            case COUNT:
                _finalResponse.getQueryResultSet().setAggregateFunctionResult(new DictionaryEntry<Object, Object>(AggregateFunctionType.COUNT, _finalResponse.getQueryResultSet().getAggregateFunctionResult().getValue()));
                break;
            case SUM:
                _finalResponse.getQueryResultSet().setAggregateFunctionResult(new DictionaryEntry<Object, Object>(AggregateFunctionType.SUM, _finalResponse.getQueryResultSet().getAggregateFunctionResult().getValue()));
                break;
            case AVG:
                _finalResponse.getQueryResultSet().setAggregateFunctionResult(new DictionaryEntry<Object, Object>(AggregateFunctionType.AVG, _finalResponse.getQueryResultSet().getAggregateFunctionResult().getValue()));
                break;
        }
    }

    public void MergeResponse(Address address, CommandResponse response)
    {
        if (_finalResponse == null && response.getType() != com.alachisoft.tayzgrid.common.protobuf.ResponseProtocol.Response.Type.GET_NEXT_CHUNK)
        {
            _finalResponse = response;
            if (response.isBrokerReset())
            {
                MergeFailedResponse(response);
            }
        }
        else
        {
            if (response.isBrokerReset())
            {
                MergeFailedResponse(response);
            }
            else
            {
                //In .net switch statement accepts null values and execute default case.
                //In java switch statement throws null pointer exception when null value is given.
                if (response.getType() != null)
                {
                    switch (response.getType())
                    {
                        case ADD_BULK:
                        case INSERT_BULK:
                        case GET_BULK:
                        case REMOVE_BULK:
                        case GET_GROUP_DATA:
                        case GET_TAG:
                        case INVOKE_ENTRYPROCESSOR:
                            HashMap map = (HashMap) response.getResultMap();
                            HashMap finalMap = (HashMap) _finalResponse.getResultMap();
                            if (finalMap != null && map != null)
                            {
                                finalMap.putAll(map);
                            }
                            break;

                        case GET_GROUP_KEYS:
                        case GET_KEYS_TAG:
                            ArrayList<String> list = (ArrayList<String>) response.getResultMap();
                            ArrayList<String> finalList = (ArrayList<String>) _finalResponse.getResultMap();
                            if (finalList != null && list != null)
                            {
                                finalList.addAll(list);
                            }
                            break;

                        case SEARCH:
                            if ((_finalResponse.getExpValue() == ExceptionProtocol.Exception.Type.TYPE_INDEX_NOT_FOUND) || (_finalResponse.getExpValue()
                                    == ExceptionProtocol.Exception.Type.ATTRIBUTE_INDEX_NOT_FOUND))
                            {
                                _finalResponse = response;
                                break;
                            }

                            switch (response.getQueryResultSet().getAggregateFunctionType())
                            {
                                case NOTAPPLICABLE:
                                    list = (ArrayList<String>) response.getResultMap();
                                    finalList = (ArrayList<String>) _finalResponse.getResultMap();
                                    if (finalList != null && list != null)
                                    {
                                        finalList.addAll(list);
                                    }
                                    break;

                                default:
                                    if (!_finalResponse.getQueryResultSet().getIsInitialized())
                                    {
                                        SetAggregateFunctionResult();
                                        _finalResponse.getQueryResultSet().Initialize(_finalResponse.getQueryResultSet());
                                    }
                                    _finalResponse.getQueryResultSet().Compile(response.getQueryResultSet());
                                    break;
                            }
                            break;
                        case SEARCH_ENTRIES:
                            if ((_finalResponse.getExpValue() == ExceptionProtocol.Exception.Type.TYPE_INDEX_NOT_FOUND) || (_finalResponse.getExpValue()
                                    == ExceptionProtocol.Exception.Type.ATTRIBUTE_INDEX_NOT_FOUND))
                            {
                                _finalResponse = response;
                                break;
                            }
                            
                            switch (response.getQueryResultSet().getType()) 
                            {
                                case GroupByAggregateFunction:
                                    _finalResponse.getQueryResultSet().getGroupByResult().Union(response.getQueryResultSet().getGroupByResult());
                                    break;
                                default:
                                    switch (response.getQueryResultSet().getAggregateFunctionType()) 
                                    {
                                        case NOTAPPLICABLE:
                                            HashMap map1 = (HashMap) response.getResultMap();
                                            HashMap finalMap1 = (HashMap) _finalResponse.getResultMap();
                                            if (finalMap1 != null && map1 != null) {
                                                finalMap1.putAll(map1);
                                            }
                                            break;

                                        default:
                                            if (!_finalResponse.getQueryResultSet().getIsInitialized()) {
                                                SetAggregateFunctionResult();
                                                _finalResponse.getQueryResultSet().Initialize(_finalResponse.getQueryResultSet());
                                            }
                                            _finalResponse.getQueryResultSet().Compile(response.getQueryResultSet());
                                            break;
                                    }
                                    break;
                            }
                            break;
                        case DELETE_QUERY:
                        case REMOVE_QUERY:
                             if ((_finalResponse.getExpValue() == ExceptionProtocol.Exception.Type.TYPE_INDEX_NOT_FOUND) || (_finalResponse.getExpValue()
                                    == ExceptionProtocol.Exception.Type.ATTRIBUTE_INDEX_NOT_FOUND))
                             {
                                if ((response.getExpValue() != ExceptionProtocol.Exception.Type.ATTRIBUTE_INDEX_NOT_FOUND) || (response.getExpValue() != ExceptionProtocol.Exception.Type.TYPE_INDEX_NOT_FOUND))
                                {
                                    _finalResponse = response;
                                }
                            }
                            else if (_finalResponse != null && (response.getExpValue() != ExceptionProtocol.Exception.Type.ATTRIBUTE_INDEX_NOT_FOUND) || (response.getExpValue() != ExceptionProtocol.Exception.Type.TYPE_INDEX_NOT_FOUND))
                            {
                                _finalResponse.setRemovedKeyCount(response.getRemovedKeyCount());
                            }                            
                            break;
                        case GET_NEXT_CHUNK:
                            if (_finalResponse == null)
                            {
                                _finalResponse = response;
                            }

                            EnumerationDataChunk chunk = null;
                            if (_chunks.containsKey(address))
                            {
                                chunk = _chunks.get(address);
                            }
                            else
                            {
                                chunk = new EnumerationDataChunk();
                                chunk.setData(new ArrayList());
                                _chunks.put(address, chunk);
                            }

                            for (int i = 0; i < response.getNextChunk().size(); i++)
                            {
                                chunk.getData().addAll(response.getNextChunk().get(i).getData());
                                chunk.setPointer(response.getNextChunk().get(i).getPointer());
                                if (chunk.getPointer().getNodeIpAddress() == null)
                                {
                                    chunk.getPointer().setNodeIpAddress(address);
                                }
                            }

                            _finalResponse.setNextChunk(new ArrayList<EnumerationDataChunk>(_chunks.values()));

                            break;
                        case GET_TASK_ENUMERATOR:                            
                            _finalResponse.getTaskEnumerator().addAll(response.getTaskEnumerator());
                            break;
                        case EXCEPTION:
                            if (response.getExpValue() == ExceptionProtocol.Exception.Type.STATE_TRANSFER_EXCEPTION)
                            {
                                _finalResponse = response;
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void MergeFailedResponse(CommandResponse response)
    {
        Command command = _commands.get(response.getResetConnectionIP());
        if (_type != null)
        {
            switch (_type)
            {
                case ADD_BULK:
                case INSERT_BULK:
                case GET_BULK:
                case REMOVE_BULK:
                case INVOKE_ENTRYPROCESSOR:


                    Object key;
                    HashMap map = (HashMap) _finalResponse.getResultMap();
                    if (map == null)
                    {
                        _finalResponse.setResultMap(new HashMap());
                        map = (HashMap) _finalResponse.getResultMap();
                    }

                    for (int index = 0; index < command.getBulkKeys().length; index++)
                    {
                        key = command.getBulkKeys()[index];

                        if (map != null)
                        {
                            if (!map.containsKey(key))
                            {
                                map.put(key, new ConnectionException("Connection with server lost [" + response.getResetConnectionIP() + "]"));
                            }
                        }
                    }

                    _finalResponse.setBrokerRequested(false);
                    break;
               
                case GET_GROUP_DATA:
                case GET_TAG:
                case GET_GROUP_KEYS:
                case GET_KEYS_TAG:
                case SEARCH:
                case SEARCH_ENTRIES:
                    _finalResponse.setBrokerRequested(true);
                    _finalResponse.setResetConnectionIP(response.getResetConnectionIP());
                    break;
            }
        }
    }
}
