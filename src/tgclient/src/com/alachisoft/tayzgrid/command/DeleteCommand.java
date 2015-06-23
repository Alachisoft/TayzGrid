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

import com.alachisoft.tayzgrid.common.BitSet;
import com.alachisoft.tayzgrid.common.BitSetConstants;
import com.alachisoft.tayzgrid.common.DeleteParams;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.DeleteCommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.util.UserBinaryObject;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public final class DeleteCommand extends Command {

	private int flagMap;
	private int dsItemRemoveCallbackId;
        private String _lockHandle;
        private com.alachisoft.tayzgrid.caching.LockAccessType _accessType;
        private long _version;
      
        private String providerName;
        private DeleteParams deleteParams;

	/**
	 * Creates a new instance of RemoveCommand
	 *
	 * @param key
	 * @param isAsync
	 */
	public DeleteCommand(Object key, BitSet flagMap, String lockHandle, int asyncOpCompleted, int dsItemRemoveCallbackId, long version, com.alachisoft.tayzgrid.caching.LockAccessType accessType, boolean isAsync, String providerName, DeleteParams deleteParams) {

            super.asyncCallbackId = asyncOpCompleted;
            this.key = key;
            this.isAsync = isAsync;
            this.flagMap = BitSetConstants.getBitSetData(flagMap);
            this._lockHandle = lockHandle;
            this._accessType = accessType;
            this.dsItemRemoveCallbackId = dsItemRemoveCallbackId;
            this._version = version;
            this.providerName = providerName;
            this.deleteParams = deleteParams;
	}

	protected void createCommand() throws CommandException {
		if (key == null)
			throw new NullPointerException("Key");
		if (key.equals(""))
			throw new IllegalArgumentException("key");
                try
                {
                    DeleteCommandProtocol.DeleteCommand.Builder builder
                            = DeleteCommandProtocol.DeleteCommand.newBuilder();

                    builder = builder.setIsAsync(isAsync)
                            .setRequestId(this.getRequestId())
                            .setKey(CacheKeyUtil.toByteString(key, this.getCacheId()))
                            .setFlag(flagMap)
                            .setLockAccessType(this._accessType.getValue())
                            .setDatasourceItemRemovedCallbackId(dsItemRemoveCallbackId)
                            .setVersion(this._version);
                    
                    if(deleteParams!=null)
                    {
                        builder = builder.setCompareOld(deleteParams.CompareOldValue);

                        if(deleteParams.OldValue != null)
                        {
                            ///Get user data and add to protobuf data array
                            UserBinaryObject userBin = UserBinaryObject.createUserBinaryObject((byte[])deleteParams.OldValue);
                            List<byte[]> dataList = userBin.getDataList();
                            int noOfChunks = dataList.size();

                            ///Copy the chunks to protobuf list
                            for (int i = 0; i < noOfChunks; i++)
                            {
                                builder.addOldValue(ByteString.copyFrom(dataList.get(i)));
                            }
                            
                            builder.setOldValueFlag(BitSetConstants.getBitSetData(deleteParams.OldValueFlag));
                        }
                    }

                if(providerName != null)
                {
                    builder = builder.setProviderName(this.providerName);
                }
                if (this._lockHandle != null) {
                    builder = builder.setLockId(this._lockHandle);
                }


                    CommandProtocol.Command.Builder commandBuilder =
                        CommandProtocol.Command.newBuilder();

                    commandBuilder = commandBuilder.setDeleteCommand(builder)
                        .setRequestID(this.getRequestId())
                        .setType(CommandProtocol.Command.Type.DELETE);

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
         return CommandType.DELETE;
     }
     
     @Override
    public RequestType getCommandRequestType()
    {
            return RequestType.AtomicWrite;
    }
}
