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

import com.alachisoft.tayzgrid.caching.autoexpiration.FixedExpiration;
import com.alachisoft.tayzgrid.common.protobuf.AddAttributeProtocol;
import com.alachisoft.tayzgrid.common.protobuf.CommandProtocol;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.runtime.exceptions.CommandException;
import com.alachisoft.tayzgrid.web.caching.Cache;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import java.io.IOException;
import java.util.Date;

public class AddAttributeCommand extends Command
{

    Date _date;

    public AddAttributeCommand(Object key, Date date)
    {
        this.name = "AddAttribute";
        this.key = key;

        if (date == null)
        {
            this._date = Cache.DefaultAbsoluteExpiration;
        }
        else
        {
            this._date = date;
        }

    }

    @Override
    public CommandType getCommandType()
    {
        return CommandType.ADDATTRIBUTES;
    }
    
    @Override
public RequestType getCommandRequestType()
{
	return RequestType.AtomicWrite;
}

    @Override
    protected void createCommand() throws CommandException
    {
        if (key == null)
        {
            throw new NullPointerException("Key");
        }

        AddAttributeProtocol.AddAttributeCommand.Builder build = AddAttributeProtocol.AddAttributeCommand.newBuilder();
        try
        {
            build.setKey(CacheKeyUtil.toByteString(key, this.getCacheId())).setRequestId(this.getRequestId());

       

            if (_date != null)
            {
                build.setAbsExpiration(HelperFxn.getUTCTicks(_date));

            }
        }
        catch (Exception exception)
        {
            throw new CommandException(exception.toString());
        }

        try
        {
            commandBytes = super.constructCommand(CommandProtocol.Command.newBuilder()
                    .setAddAttributeCommand(build)
                    .setRequestID(this.getRequestId())
                    .setType(CommandProtocol.Command.Type.ADD_ATTRIBUTE).build().toByteArray());
        }
        catch (IOException e)
        {
            throw new CommandException(e.getMessage());
        }
        catch (Exception ex)
        {
            throw new CommandException(ex.getMessage());
        }

    }

    @Override
    protected boolean parseCommand()
    {
        return true;
    }
}
