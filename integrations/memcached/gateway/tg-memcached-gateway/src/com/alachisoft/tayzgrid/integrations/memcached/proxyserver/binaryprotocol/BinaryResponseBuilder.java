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

import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.DeleteCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.StatsCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.GetCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.InvalidCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.StorageCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.VersionCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.NoOperationCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.FlushCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.MutateCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.QuitCommand;
import com.alachisoft.tayzgrid.integrations.memcached.provider.Result;
import com.alachisoft.tayzgrid.integrations.memcached.provider.MutateOpResult;
import com.alachisoft.tayzgrid.integrations.memcached.provider.OperationResult;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.DataStream;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.memcachedencoding.BinaryConverter;

public class BinaryResponseBuilder
{
 	private static byte[] BuildResposne(Opcode opcode, BinaryResponseStatus status, int opaque, long cas, String key, byte[] value, byte[] extra)
	{
		BinaryResponse response = new BinaryResponse();
		response.getHeader().setOpcode(opcode);
		response.getHeader().setStatus(status);
		response.getHeader().setOpaque(opaque);
		response.getHeader().setCAS(cas);
		response.getPayLoad().setKey(BinaryConverter.GetBytes(key));
		response.getPayLoad().setValue(value);
		response.getPayLoad().setExtra(extra);
		return response.BuildResponse();
	}

 	public static byte[] BuildQuitResponse(QuitCommand command)
	{
		if (command.getNoReply() == true)
		{
			return null;
		}

		return BuildResposne(command.getOpcode(), BinaryResponseStatus.no_error, command.getOpaque(), 0, null, null, null);
	}

 	public static byte[] BuildInvalidResponse(InvalidCommand command)
	{
		BinaryResponseStatus status;
		if (command != null && command.getOpcode() == Opcode.unknown_command)
		{
			status = BinaryResponseStatus.unknown_commnad;
		}
		else
		{
			status = BinaryResponseStatus.invalid_arguments;
		}

		return BuildResposne(command.getOpcode(), status, command.getOpaque(), 0, null, null, null);
	}

 	public static byte[] BuildVersionResponse(VersionCommand command)
	{
 		byte[] value = null;

		if (command.getOperationResult().getReturnResult() == Result.SUCCESS)
		{
			Object tempVar = command.getOperationResult().getReturnValue();
			String version = (String)((tempVar instanceof String) ? tempVar : null);
			value = BinaryConverter.GetBytes(version);
		}
		return BuildResposne(command.getOpcode(), BinaryResponseStatus.no_error, command.getOpaque(), 0, null, value, null);
	}

 	public static byte[] BuildCounterResponse(MutateCommand command)
	{
 		byte[] value = null;
		BinaryResponseStatus status = BinaryResponseStatus.no_error;
 		long cas = 0;

		if (command.getExceptionOccured())
		{
			status = BinaryResponseStatus.item_not_stored;
		}
                else if(command.getErrorMessage()!=null)
                {
                    status=BinaryResponseStatus.invalid_arguments;
                    value = BinaryConverter.GetBytes(command.getErrorMessage());
                }
		else
		{
			switch (command.getOperationResult().getReturnResult())
			{
				case ITEM_TYPE_MISMATCHED:
					status = BinaryResponseStatus.incr_decr_on_nonnumeric_value;
					value = BinaryConverter.GetBytes("Increment or decrement on non-numeric value");
					break;
				case ITEM_NOT_FOUND:
					status = BinaryResponseStatus.key_not_found;
					value = BinaryConverter.GetBytes("NOT_FOUND");
					break;
				case SUCCESS:
					if (command.getNoReply() == true)
					{
						return null;
					}
 					cas = (Long)command.getOperationResult().getReturnValue();
					OperationResult tempVar = command.getOperationResult();
 					long response = ((MutateOpResult)((tempVar instanceof MutateOpResult) ? tempVar : null)).getMutateResult();
					value = BinaryConverter.GetBytes(response);
					break;
				case ITEM_MODIFIED:
					status = BinaryResponseStatus.key_exists;
					break;
			}
		}
		return BuildResposne(command.getOpcode(), status, command.getOpaque(), cas, null, value, null);
	}

 	public static byte[] BuildGetResponse(GetCommand command)
	{
		String key = "";
 		byte[] value = new byte[0];
 		int flag = 0;
 		long cas = 0;
		BinaryResponseStatus status = BinaryResponseStatus.no_error;
		if (command.getExceptionOccured())
		{
			status = BinaryResponseStatus.key_not_found;
		}
		else if (command.getResults().size() > 0)
		{
			cas = command.getResults().get(0).getVersion();
			Object tempVar = command.getResults().get(0).getReturnValue();
 			value = (byte[])((tempVar instanceof byte[]) ? tempVar : null);
			flag = command.getResults().get(0).getFlag();

			if (command.getOpcode() == Opcode.GetK)
			{
				key = command.getResults().get(0).getKey();
			}
		}
		else
		{
			if (command.getNoReply() == true)
			{
				return null;
			}
			status = BinaryResponseStatus.key_not_found;
		}

 		byte[] flagBytes = BinaryConverter.GetBytes(flag);
		return BuildResposne(command.getOpcode(), status, command.getOpaque(), cas, key, value, flagBytes);
	}

 	public static byte[] BuildNoOpResponse(NoOperationCommand command)
	{
		return BuildResposne(command.getOpcode(), BinaryResponseStatus.no_error, command.getOpaque(), 0, null, null, null);
	}

 	public static byte[] BuildDeleteResponse(DeleteCommand command)
	{
		BinaryResponseStatus status = BinaryResponseStatus.no_error;
		if (command.getOperationResult().getReturnResult() != Result.SUCCESS)
		{
			status = BinaryResponseStatus.key_not_found;
		}
		else
		{
			if (command.getNoReply() == true)
			{
				return null;
			}
		}
		return BuildResposne(command.getOpcode(), status, command.getOpaque(), 0, null, null, null);
	}

 	public static byte[] BuildStorageResponse(StorageCommand command)
	{
		BinaryResponseStatus status = BinaryResponseStatus.no_error;
 		long cas = 0;
		if (command.getExceptionOccured())
		{
			status = BinaryResponseStatus.item_not_stored;
		}
		else
		{
			switch (command.getOperationResult().getReturnResult())
			{
				case SUCCESS:
					if (command.getNoReply() == true)
					{
						return null;
					}
 					cas = (Long)command.getOperationResult().getReturnValue();
					break;
				case ITEM_EXISTS:
					status = BinaryResponseStatus.key_exists;
					break;
				case ITEM_NOT_FOUND:
					status = BinaryResponseStatus.key_not_found;
					break;
				case ITEM_MODIFIED:
					status = BinaryResponseStatus.key_exists;
					break;
				default:
					status = BinaryResponseStatus.item_not_stored;
					break;
			}
		}
		return BuildResposne(command.getOpcode(), status, command.getOpaque(), cas, null, null, null);
	}

 	public static byte[] BuildFlushResponse(FlushCommand command)
	{
		BinaryResponseStatus status = BinaryResponseStatus.no_error;
		if (command.getOperationResult().getReturnResult() != Result.SUCCESS)
		{
			status = BinaryResponseStatus.invalid_arguments;
		}
		else
		{
			if (command.getNoReply() == true)
			{
				return null;
			}
		}
		return BuildResposne(command.getOpcode(), status, command.getOpaque(), 0, null, null, null);
	}

 	public static byte[] BuildStatsResponse(StatsCommand command)
	{
		DataStream stream = new DataStream();
		Object tempVar = command.getOperationResult().getReturnValue();
		java.util.Hashtable stats = (java.util.Hashtable)((tempVar instanceof java.util.Hashtable) ? tempVar : null);

    
		stream.Write(BuildResposne(command.getOpcode(), BinaryResponseStatus.no_error, command.getOpaque(), 0, null, null, null));
		return stream.ReadAll();
	}
}