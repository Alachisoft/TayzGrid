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

package com.alachisoft.tayzgrid.storage;

import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.common.propagator.IAlertPropagator;

public class CacheStorageFactory {

    public static ICacheStorage CreateStorageProvider(java.util.Map properties, String cacheContext, boolean evictionEnabled, ILogger NCacheLog, IAlertPropagator alertPropagator) throws Exception 
    {
        /**
         * Facotry object resonsible for creating mulitple types of stores. Used
         * by the LocalCache. sample property string for creation is.
         *
         * storage ( scheme=file; file ( max-objects=100;
         * root-dir="c:\temp\test"; ) )
         *
         */
        /**
         * Internal method that creates a cache store. A HashMap containing the
         * config parameters is passed to this method.
         */
        if (properties == null) {
            throw new IllegalArgumentException("properties");
        }

        StorageProviderBase cacheStorage = null;
        try {
            if (!properties.containsKey("class")) {
                throw new Exception("Missing cache store class.");
            }

            String scheme = String.valueOf(properties.get("class")).toLowerCase();
            java.util.Map schemeProps = (java.util.Map) properties.get(scheme);

            if (scheme.compareTo("heap") == 0) {
                cacheStorage = new ClrHeapStorageProvider(schemeProps, evictionEnabled, NCacheLog, alertPropagator);
            }

            if (cacheStorage != null) {
                cacheStorage.setCacheContext(cacheContext);
            }
        } catch (Exception e) {
            throw e;
        }

        return cacheStorage;
    }
}
