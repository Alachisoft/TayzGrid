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

public class StorageCommand extends AbstractCommand {

    private String _key;
 
    private int _flags;
    private long _expirationTimeInSeconds;
    private Object _dataBlock;
    private int _dataSize;
 
    private long _casUnique;

    public final String getKey() {
        return _key;
    }

    public final void setKey(String value) {
        _key = value;
    }

    public final int getFlags() {
        return _flags;
    }
    
    public final int getDataSize()
    {
        return _dataSize;
    }
 

    public final void setFlags(int value) {
        _flags = value;
    }

    public final long getExpirationTimeInSeconds() {
        return _expirationTimeInSeconds;
    }

    public final void setExpirationTimeInSeconds(long value) {
        _expirationTimeInSeconds = value;
    }

    public final long getCASUnique() {
        return _casUnique;
    }
 
    public final void setCASUnique(long value) {
        _casUnique = value;
    }

    public final Object getDataBlock() {
        return _dataBlock;
    }

    public final void setDataBlock(Object value) {
        _dataBlock = value;
    }

     public StorageCommand(Opcode cmdType, String key, int flags, long expirationTimeInSeconds, int dataSize) {
        super(cmdType);
        _key = key;
        _flags = flags;
        _expirationTimeInSeconds = expirationTimeInSeconds;
        _dataSize = dataSize;
    }

     public StorageCommand(Opcode cmdType, String key, int flags, long expirationTimeInSeconds, long casUnique, int dataSize) {
        super(cmdType);
        _key = key;
        _flags = flags;
        _expirationTimeInSeconds = expirationTimeInSeconds;
        _dataSize = dataSize;
        _casUnique = casUnique;
    }

    @Override
    public void Execute(IMemcachedProvider cacheProvider) throws CacheRuntimeException, InvalidArgumentsException {
        if (this._errorMessage != null) {
            return;
        }

        if (_casUnique > 0) {
            _result = cacheProvider.checkAndSet(_key, _flags, _expirationTimeInSeconds, _casUnique, _dataBlock);
        } else {
            switch (this.getOpcode()) {
                case Set:
                case SetQ:
                    _result = cacheProvider.set(_key, _flags, _expirationTimeInSeconds, _dataBlock);
                     
                    break;
                case Add:
                case AddQ:
                    _result = cacheProvider.add(_key, _flags, _expirationTimeInSeconds, _dataBlock);
                    break;
                case Replace:
                case ReplaceQ:
                    _result = cacheProvider.replace(_key, _flags, _expirationTimeInSeconds, _dataBlock);
                    break;
                case Append:
                case AppendQ:
                    _result = cacheProvider.append(_key, (byte[]) _dataBlock, _casUnique);
                    break;
                case Prepend:
                case PrependQ:
                    _result = cacheProvider.prepend(_key, (byte[]) _dataBlock, _casUnique);
                    break;
                case CAS:
                    _result = cacheProvider.checkAndSet(_key, _flags, _expirationTimeInSeconds, _casUnique, _dataBlock);
                    break;
            }
        }
    }
}