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

package com.alachisoft.tayzgrid.common.communication;

import com.alachisoft.tayzgrid.common.communication.exceptions.ChannelException;

public class RequestResponsePair
{

    private boolean lockAccquired = false;
    public boolean getLockAccquired()
    {
        return this.lockAccquired;
    }
    public void setLockAccquired(boolean lockAccquired)
    {
        this.lockAccquired = lockAccquired;
    }
    private Object privateRequest;

    public final Object getRequest()
    {
        return privateRequest;
    }

    public final void setRequest(Object value)
    {
        privateRequest = value;
    }
    private Object privateResponse;

    public final Object getResponse()
    {
        return privateResponse;
    }

    public final void setResponse(Object value)
    {
        privateResponse = value;
    }
    private ChannelException privateChannelException;

    public final ChannelException getChannelException()
    {
        return privateChannelException;
    }

    public final void setChannelException(ChannelException value)
    {
        privateChannelException = value;
    }
    private boolean privateRequestSentOverChannel;

    public final boolean getRequestSentOverChannel()
    {
        return privateRequestSentOverChannel;
    }

    public final void setRequestSentOverChannel(boolean value)
    {
        privateRequestSentOverChannel = value;
    }
}
