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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common;

 
import com.alachisoft.tayzgrid.common.logger.*;
import com.alachisoft.tayzgrid.integrations.memcached.provider.CacheFactory;

public final class LogManager {

    private static final ILogger _logger;

    static {
        _logger = new JLogger();
        try {
            _logger.Initialize(LoggerNames.MemcacheGateway);
            CacheFactory.setLogger(_logger);
        } catch (Exception ex) {
        }
    }

    public static ILogger getLogger() {
        return _logger;
    }
 
}