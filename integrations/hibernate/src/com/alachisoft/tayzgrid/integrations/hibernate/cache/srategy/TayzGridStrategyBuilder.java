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

package com.alachisoft.tayzgrid.integrations.hibernate.cache.srategy;

import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridCollectionRegion;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridEntityRegion;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridNaturalIdRegion;
import org.hibernate.cache.spi.access.AccessType;
import static org.hibernate.cache.spi.access.AccessType.*;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

public class TayzGridStrategyBuilder {

    private TayzGridStrategyBuilder()
    {
    }
    
    public static CollectionRegionAccessStrategy buildCollectionRegionAccessStrategy(TayzGridCollectionRegion region, AccessType accessType) {
        switch (accessType) {
            case READ_ONLY:
                return new ReadOnlyTayzGridCollectionRegionAccessStrategy(region);
            case NONSTRICT_READ_WRITE:
                return new NonStrictReadWriteTayzGridCollectionRegionAccessStrategy(region);
            case READ_WRITE:
                return new ReadWriteTayzGridCollectionRegionAccessStrategy(region);
            case TRANSACTIONAL:
                throw new IllegalArgumentException(accessType + " access strategy not supported.");
            default:
                throw new IllegalArgumentException(accessType + " is not recognized as valid access type.");
        }
    }

    public static EntityRegionAccessStrategy buildEntityRegionAccessStrategy(TayzGridEntityRegion region, AccessType accessType) {
        switch (accessType) {
            case READ_ONLY:
                return new ReadOnlyTayzGridEntityRegionAccessStrategy(region);
            case NONSTRICT_READ_WRITE:
                return new NonStrictReadWriteTayzGridEntityRegionAccessStrategy(region);
            case READ_WRITE:
                return new ReadWriteTayzGridEntityRegionAccessStrategy(region);
            case TRANSACTIONAL:
                throw new IllegalArgumentException(accessType + " access strategy not supported.");
            default:
                throw new IllegalArgumentException(accessType + " is not recognized as valid access type.");
        }
    }

    public static NaturalIdRegionAccessStrategy buildNaturalIdRegionAccessStrategy(TayzGridNaturalIdRegion region, AccessType accessType) {
        switch (accessType) {
            case READ_ONLY:
                return new ReadOnlyTayzGridNaturalIdRegionAccessStrategy(region);
            case NONSTRICT_READ_WRITE:
                return  new NonStrictReadWriteTayzGridNaturalIdRegionAccessStrategy(region);
            case READ_WRITE:
                return new ReadWriteTayzGridNaturalIdRegionAccessStraegy(region);
            case TRANSACTIONAL:
                throw new IllegalArgumentException(accessType + " access strategy not supported.");
            default:
                throw new IllegalArgumentException(accessType + " is not recognized as valid access type.");
        }
    }
}
