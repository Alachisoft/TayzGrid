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
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;

public class MutateCommand extends AbstractCommand {

    private String _key;

    public final String getKey() {
        return _key;
    }

    public final void setKey(String value) {
        _key = value;
    }
 
    private long _delta;

    public final long getDelta() {
        return _delta;
    }
 

    public final void setDelta(long value) {
        _delta = value;
    }
 
    private long _initialValue;

    public final long getInitialValue() {
        return _initialValue;
    }
 

    public final void setInitialValue(long value) {
        _initialValue = value;
    }
    private long _expirationTimeInSeconds = 4294967295L;

    public final long getExpirationTimeInSeconds() {
        return _expirationTimeInSeconds;
    }

    public final void setExpirationTimeInSeconds(long value) {
        _expirationTimeInSeconds = value;
    }
 
    private long _cas = 0;

    public final long getCAS() {
        return _cas;
    }
 

    public final void setCAS(long value) {
        _cas = value;
    }

    public MutateCommand(Opcode cmdType) {
        super(cmdType);
    }

    @Override
    public void Execute(IMemcachedProvider cacheProvider) throws CacheRuntimeException, InvalidArgumentsException {
        switch (_opcode) {
            case Increment:
            case IncrementQ:
                _result = cacheProvider.increment(_key, _delta, _initialValue, _expirationTimeInSeconds, _cas);
                break;

            case Decrement:
            case DecrementQ:
                _result = cacheProvider.decrement(_key, getDelta(), _initialValue, _expirationTimeInSeconds, _cas);
                break;
        }
    }
}