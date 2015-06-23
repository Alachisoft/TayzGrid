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

package com.alachisoft.tayzgrid.integrations.hibernate.cache.regions;

import com.alachisoft.tayzgrid.integrations.hibernate.cache.srategy.TayzGridStrategyBuilder;
import java.util.Properties;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

public class TayzGridNaturalIdRegion extends TayzGridTransactionalDataRegion implements NaturalIdRegion {

    public TayzGridNaturalIdRegion(String name, Properties properties, CacheDataDescription metaData) {
        super(name, properties, metaData);
    }

    /**
     * Build an access strategy for the requested access type.
     *
     * @param accessType The type of access strategy to build; never null.
     * @return The appropriate strategy contract for accessing this region for
     * the requested type of access.
     * @throws org.hibernate.cache.CacheException Usually indicates
     * mis-configuration.
     */
    @Override
    public NaturalIdRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
        return TayzGridStrategyBuilder.buildNaturalIdRegionAccessStrategy(this, accessType);
    }
}
