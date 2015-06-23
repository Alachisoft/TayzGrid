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

import com.alachisoft.tayzgrid.integrations.memcached.provider.IMemcachedProvider;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;

public class InvalidCommand extends AbstractCommand {

    public InvalidCommand() {
        super(Opcode.Invalid_Command);
        _errorMessage = "ERROR";
    }

    public InvalidCommand(String error) {
        super(Opcode.Invalid_Command);
        if (error != null) {
            _errorMessage = error;
        } else {
            _errorMessage = "ERROR";
        }
    }

    @Override
    public void Execute(IMemcachedProvider cacheProvider) {
    }
}