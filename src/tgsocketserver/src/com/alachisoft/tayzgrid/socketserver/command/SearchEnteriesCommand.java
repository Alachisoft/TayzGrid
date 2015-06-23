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

import com.alachisoft.tayzgrid.socketserver.command.responsebuilders.SearchEnteriesResponseBuilder;
import com.alachisoft.tayzgrid.socketserver.ICommandExecuter;
import com.alachisoft.tayzgrid.socketserver.ClientManager;
import com.alachisoft.tayzgrid.socketserver.TayzGrid;
import com.alachisoft.tayzgrid.caching.queries.QueryResultSet;
import com.alachisoft.tayzgrid.common.protobuf.KeyValueProtocol;
import com.alachisoft.tayzgrid.common.protobuf.SearchCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.ValueWithTypeProtocol;
import com.alachisoft.tayzgrid.common.util.JavaClrTypeMapping;
import com.alachisoft.tayzgrid.caching.OperationContextFieldName;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.common.util.ResponseHelper;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
public class SearchEnteriesCommand extends CommandBase {
	private final static class CommandInfo {
		public String RequestId;
		public String Query;
		public java.util.Map Values;
		public int CommandVersion;
		public long ClientLastViewId;

		public CommandInfo clone() {
			CommandInfo varCopy = new CommandInfo();

			varCopy.RequestId = this.RequestId;
			varCopy.Query = this.Query;
			varCopy.Values = this.Values;
			varCopy.CommandVersion = this.CommandVersion;
			varCopy.ClientLastViewId = this.ClientLastViewId;

			return varCopy;
		}
	}

	private static char Delimitor = '|'; 
	//PROTOBUF
	@Override
	public void ExecuteCommand(ClientManager clientManager, com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command) {
		CommandInfo cmdInfo = new CommandInfo();

		try {
			cmdInfo = ParseCommand(command, clientManager).clone();
		} catch (RuntimeException exc) {
			if (!super.immatureId.equals("-2")) {
				//PROTOBUF:RESPONSE
				_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
			}
			return;
		}

		byte[] data = null;

		try {
			ICommandExecuter tempVar = clientManager.getCmdExecuter();
			TayzGrid nCache = (TayzGrid)((tempVar instanceof TayzGrid) ? tempVar : null);
			QueryResultSet resultSet = null;
			com.alachisoft.tayzgrid.caching.OperationContext operationContext = new com.alachisoft.tayzgrid.caching.OperationContext(com.alachisoft.tayzgrid.caching.OperationContextFieldName.OperationType, com.alachisoft.tayzgrid.caching.OperationContextOperationType.CacheOperation);
			if (cmdInfo.CommandVersion <= 1) { //NCache 3.8 SP4 and previous
				operationContext.Add(OperationContextFieldName.ClientLastViewId, forcedViewId);
			}
			else { 
				operationContext.Add(OperationContextFieldName.ClientLastViewId, cmdInfo.ClientLastViewId);
			}
			resultSet = nCache.getCache().SearchEntries(cmdInfo.Query, cmdInfo.Values, operationContext);

			SearchEnteriesResponseBuilder.BuildResponse(resultSet, cmdInfo.CommandVersion, cmdInfo.RequestId, _serializedResponsePackets, nCache.getCacheId());

		} catch (Exception exc) {
			//PROTOBUF:RESPONSE
			_serializedResponsePackets.add(ResponseHelper.SerializeExceptionResponse(exc, command.getRequestID()));
		}
	}

	//PROTOBUF : SearchCommand is used for enteries
	private CommandInfo ParseCommand(com.alachisoft.tayzgrid.common.protobuf.CommandProtocol.Command command, ClientManager clientManager) {
		CommandInfo cmdInfo = new CommandInfo();

		SearchCommandProtocol.SearchCommand searchCommand = command.getSearchCommand();
		cmdInfo.Query = searchCommand.getQuery();
		if (clientManager.IsDotNetClient) {
			int index = cmdInfo.Query.indexOf("$Text$");
			if (index != -1) {
				cmdInfo.Query = cmdInfo.Query.replace("$Text$", "System.String");
			} else {
				index = cmdInfo.Query.indexOf("$TEXT$");
				if (index != -1) {
					cmdInfo.Query = cmdInfo.Query.replace("$TEXT$", "System.String");
				} else {
					index = cmdInfo.Query.indexOf("$text$");
					if (index != -1) {
						cmdInfo.Query = cmdInfo.Query.replace("$text$", "System.String");
					}
				}
			}
		} else {
			int index = cmdInfo.Query.indexOf("$Text$");
			if (index != -1) {
				cmdInfo.Query = cmdInfo.Query.replace("$Text$", "java.lang.String");
			} else {
				index = cmdInfo.Query.indexOf("$TEXT$");
				if (index != -1) {
					cmdInfo.Query = cmdInfo.Query.replace("$TEXT$", "java.lang.String");
				} else {
					index = cmdInfo.Query.indexOf("$text$");
					if (index != -1) {
						cmdInfo.Query = cmdInfo.Query.replace("$text$", "java.lang.String");
					}
				}
			}
		}
		cmdInfo.RequestId = (new Long(command.getRequestID())).toString();
		cmdInfo.CommandVersion = command.getCommandVersion();
		cmdInfo.ClientLastViewId = command.getClientLastViewId();

		if (searchCommand.getSearchEntries() == true) {
			cmdInfo.Values = new java.util.HashMap();
			for (KeyValueProtocol.KeyValue searchValue : searchCommand.getValuesList()) {
				String key = searchValue.getKey();
				java.lang.Class type = null;
				Object value = null;

				for (ValueWithTypeProtocol.ValueWithType valueWithType : searchValue.getValueList()) {
					String typeStr = valueWithType.getType();
					if (clientManager.IsDotNetClient) {
						typeStr = JavaClrTypeMapping.ClrToJava(valueWithType.getType());
					}
                                       try
                                    {
                                        type = java.lang.Class.forName(typeStr);

                                        if (valueWithType.getValue() != null)
                                        {

                                            if (type == java.util.Date.class)
                                            {
                                                /**
                                                 * For client we would be sending ticks instead of string representation of Date.
                                                 */
                                                value =HelperFxn.getDateFromTicks(Long.parseLong(valueWithType.getValue()));
                                            }
                                            else
                                            {
                                                value=new ConfigurationBuilder().ConvertToPrimitive(type, valueWithType.getValue(), "");
                                            }

                                        }
                                    }
                                    catch (Exception e)
                                    {
                                        throw new java.lang.IllegalArgumentException("Cannot convert '" + valueWithType.getValue() + "' to " + type.toString());
                                    }

					if (!cmdInfo.Values.containsKey(key)) {
						cmdInfo.Values.put(key, value);
					} else {
						java.util.ArrayList list = (java.util.ArrayList)((cmdInfo.Values.get(key) instanceof java.util.ArrayList) ? cmdInfo.Values.get(key) : null); // the value is not array list
						if (list == null) {
							list = new java.util.ArrayList();
							list.add(cmdInfo.Values.get(key)); // add the already present value in the list
							cmdInfo.Values.remove(key); // remove the key from hashtable to avoid key already exists exception
							list.add(value); // add the new value in the list
							cmdInfo.Values.put(key, list);
						} else {
							list.add(value);
						}
					}
				}
			}
		}

		return cmdInfo;
	}


	private Object GetValueObject(String value, boolean dotNetClient) throws Exception{
		Object retVal = null;

		try {
			String[] vals = value.split(java.util.regex.Pattern.quote((new Character(Delimitor)).toString()), -1);
			Object valObj = (Object)vals[0];
			String typeStr = vals[1];

			if (!dotNetClient) {
				String type = JavaClrTypeMapping.JavaToClr(typeStr);
				if (type != null) { 
					typeStr = type;
				}
			}

			java.lang.Class objType = java.lang.Class.forName(typeStr);
                        retVal=objType.cast(valObj);
		} catch (Exception ex) {
			throw ex;
		}
		return retVal;
	}
}
