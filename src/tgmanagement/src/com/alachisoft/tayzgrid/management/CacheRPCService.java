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

package com.alachisoft.tayzgrid.management;

import com.alachisoft.tayzgrid.servicecontrol.CacheService;
import com.alachisoft.tayzgrid.management.rpc.RemoteCacheServer;
import com.alachisoft.tayzgrid.management.ICacheServer;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.net.UnknownHostException;

public class CacheRPCService extends CacheService
{

    public CacheRPCService(String address) throws UnknownHostException
    {
        super(address, true);
    }

    public CacheRPCService(String address, int port) throws UnknownHostException
    {
        super(address, port, true);
    }

    @Override
    public ICacheServer GetCacheServer(TimeSpan timeout) throws java.lang.Exception
    {
        RemoteCacheServer cacheServer = new RemoteCacheServer(getServerName(), (int) getPort());
        cacheServer.Initialize(null);
        return cacheServer;
    }
}
