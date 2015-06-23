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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands;

import com.alachisoft.tayzgrid.integrations.memcached.provider.exceptions.CacheRuntimeException;
import com.alachisoft.tayzgrid.integrations.memcached.provider.exceptions.InvalidArgumentsException;
import com.alachisoft.tayzgrid.integrations.memcached.provider.IMemcachedProvider;
import com.alachisoft.tayzgrid.integrations.memcached.provider.OperationResult;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;

public abstract class AbstractCommand {

    protected Opcode _opcode = Opcode.Invalid_Command;
    protected String _errorMessage;
    protected boolean _noReply;
    protected int _opaque;
    protected OperationResult _result;
    
    private boolean _needDisposeCient=false;
    public final boolean disposeClient()
    {
        return _needDisposeCient;
    }
    
    public final void setDisposeClient(boolean dispose)
    {
        _needDisposeCient=dispose;
    }
 

    public final int getOpaque() {
        return _opaque;
    }

    public final void setOpaque(int value) {
        _opaque = value;
    }

    public final Opcode getOpcode() {
        return _opcode;
    }

    public final void setOpcode(Opcode value) {
        _opcode = value;
    }
    private boolean privateExceptionOccured;

    public final boolean getExceptionOccured() {
        return privateExceptionOccured;
    }

    public final void setExceptionOccured(boolean value) {
        privateExceptionOccured = value;
    }

    public final String getErrorMessage() {
        return _errorMessage;
    }

    public final void setErrorMessage(String value) {
        _errorMessage = value;
    }

    public final boolean getNoReply() {
        return _noReply;
    }

    public final void setNoReply(boolean value) {
        _noReply = value;
    }

    public final OperationResult getOperationResult() {
        return _result;
    }

    public final void setOperationResult(OperationResult value) {
        _result = value;
    }

    public AbstractCommand(Opcode type) {
        _opcode = type;
    }

    public abstract void Execute(IMemcachedProvider cacheProvider) throws CacheRuntimeException, InvalidArgumentsException;
}