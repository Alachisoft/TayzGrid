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

package com.alachisoft.tayzgrid.command;

import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.RemoveCommandProtocol;
import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;

import java.io.IOException;


public final class RemoveCommand extends Command {

	private int flagMap;
	private int dsItemRemoveCallbackId;
        private String _lockHandle;
        private com.alachisoft.tayzgrid.caching.LockAccessType _accessType;
        private long _version;
        
        private String providerName;
        

	/**
	 * Creates a new instance of RemoveCommand
	 *
	 * @param key
	 * @param isAsync
	 */
	public RemoveCommand(Object key, BitSet flagMap, String lockHandle, int asyncOpCompleted, int dsItemRemoveCallbackId, long version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, boolean isAsync, String providerName) {

            super.asyncCallbackId = asyncOpCompleted;
            this.key = key;
		this.isAsync = isAsync;
                this.asyncCallbackSpecified = this.isAsync && super.asyncCallbackId != -1 ? true : false;
		this.flagMap = BitSetConstants.getBitSetData(flagMap);
                this._lockHandle = lockHandle;
                this._accessType = accessType;
		this.dsItemRemoveCallbackId = dsItemRemoveCallbackId;
                this._version = version;
                this.providerName = providerName;
	}

	protected void createCommand() throws CommandException {
		if (key == null)
			throw new NullPointerException("Key");
		if (key.equals(""))
			throw new IllegalArgumentException("key");
                try
                {
                    RemoveCommandProtocol.RemoveCommand.Builder builder
                            = RemoveCommandProtocol.RemoveCommand.newBuilder();

                    builder = builder.setIsAsync(isAsync)
                            .setRequestId(this.getRequestId())
                            .setKey(CacheKeyUtil.toByteString(key, this.getCacheId()))
                            .setFlag(flagMap)
                            .setLockAccessType(this._accessType.getValue())
                            .setDatasourceItemRemovedCallbackId(dsItemRemoveCallbackId)
                            .setVersion(this._version);

                    if(providerName != null)
                    {
                        builder = builder.setProviderName(this.providerName);
                    }
                    if (this._lockHandle != null) {
                        builder = builder.setLockId(this._lockHandle);
                    }

		
                    CommandProtocol.Command.Builder commandBuilder =
                        CommandProtocol.Command.newBuilder();

                    commandBuilder = commandBuilder.setRemoveCommand(builder)
                        .setRequestID(this.getRequestId())
                        .setType(CommandProtocol.Command.Type.REMOVE);

                    super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());
		}
                catch (IOException ex)
                {
			throw new CommandException(ex.getMessage());
		}
	}

	/**
	 *
	 * @return
	 */
	protected boolean parseCommand() {
		return true;
	}

        public CommandType getCommandType() {
            return CommandType.REMOVE;
        }
        
        public int AsycItemRemovedOpComplete()
        {
            return super.asyncCallbackId;
        }
        
        @Override
        public RequestType getCommandRequestType()
        {
                return RequestType.AtomicWrite;
        }
}
