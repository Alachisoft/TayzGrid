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
import com.alachisoft.tayzgrid.common.protobuf.BulkRemoveCommandProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.ArrayList;


/**
 *
 * 
 */
public final class BulkRemoveCommand extends Command {

	private Object[] keys;
	private BitSet flagMap;
	private int onDsItemsRemovedId;

    
        private String providerName;
  

	/** Creates a new instance of BulkGetCommand */
	public BulkRemoveCommand(Object[] keys, BitSet flagMap, String providerName, int onDsItemRemovedId) {
		this.name = "REMOVEBULK";
                super.setBulkKeys(keys);
		this.keys = keys;
		this.flagMap = flagMap;
		this.onDsItemsRemovedId = onDsItemRemovedId;
                this.providerName = providerName;
	}

	protected void createCommand() throws CommandException {
		if (keys == null)
			throw new NullPointerException("keys");
		if (keys.length == 0)
			throw new IllegalArgumentException(
					"There is no key present in keys array");
                try {
                    BulkRemoveCommandProtocol.BulkRemoveCommand.Builder builder =
                            BulkRemoveCommandProtocol.BulkRemoveCommand.newBuilder();

                    ArrayList<ByteString> list = new ArrayList<ByteString>();

                    for (int i = 0; i < keys.length; i++)
                    {
                        if(keys[i] != null)
                            list.add(CacheKeyUtil.toByteString(keys[i], this.getCacheId()));

                    }


                    builder = builder.addAllKeys(list)
                            .setDatasourceItemRemovedCallbackId(onDsItemsRemovedId)
                            .setRequestId(this.getRequestId())
                            .setFlag(BitSetConstants.getBitSetData(this.flagMap));


                     if(providerName != null) builder = builder.setProviderName(providerName);




                    CommandProtocol.Command.Builder commandBuilder =
                        CommandProtocol.Command.newBuilder();

                    commandBuilder = commandBuilder.setBulkRemoveCommand(builder)
                        .setRequestID(this.getRequestId())
                        .setType(CommandProtocol.Command.Type.REMOVE_BULK)
                        .setIntendedRecipient(this.getIntendedRecipient())
                        .setClientLastViewId(this.getClientLastViewId())
                        .setCommandVersion(1);

                    super.commandBytes = this.constructCommand(commandBuilder.build().toByteArray());

		} catch (IOException ex) {
			throw new CommandException(ex.getMessage());
		}
	}

	protected boolean parseCommand() {
		return false;
	}

        public CommandType getCommandType() {
         return CommandType.REMOVE_BULK;
        }
        
        @Override
        public RequestType getCommandRequestType()
        {
                return RequestType.BulkWrite;
        }
}
