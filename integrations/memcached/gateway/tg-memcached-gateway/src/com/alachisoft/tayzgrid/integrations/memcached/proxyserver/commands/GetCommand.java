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
import com.alachisoft.tayzgrid.integrations.memcached.provider.Result;
import com.alachisoft.tayzgrid.integrations.memcached.provider.IMemcachedProvider;
import com.alachisoft.tayzgrid.integrations.memcached.provider.GetOpResult;
import com.alachisoft.tayzgrid.integrations.memcached.provider.OperationResult;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;
import java.util.ArrayList;

public class GetCommand extends AbstractCommand {

    private String[] _keys;
    private java.util.List<GetOpResult> _getResult;

    public final String[] getKeys() {
        return _keys;
    }

    public final void setKeys(String[] value) {
        _keys = value;
    }

    public final java.util.List<GetOpResult> getResults() {
        return _getResult;
    }

    public final void setResults(java.util.ArrayList<GetOpResult> value) {
        _getResult = value;
    }

    public GetCommand(Opcode type) {
        super(type);

    }

    @Override
    public void Execute(IMemcachedProvider cacheProvider) throws CacheRuntimeException, InvalidArgumentsException {
        if (this._errorMessage != null) {
            return;
        }
     
        _getResult = cacheProvider.get(_keys);
        this.setOperationResult(new OperationResult());
        this.getOperationResult().setReturnResult(Result.SUCCESS);
    }
}