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

package com.alachisoft.tayzgrid.integrations.hibernate.cache;

import com.alachisoft.tayzgrid.integrations.hibernate.cache.srategy.util.Timestamper;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridCollectionRegion;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridNaturalIdRegion;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridQueryResultRegion;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridTimestampsRegion;
import com.alachisoft.tayzgrid.integrations.hibernate.cache.regions.TayzGridEntityRegion;
import java.util.Properties;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Settings;

public class TayzGridRegionFactory implements RegionFactory {

    /**
     * Life cycle callback to perform any necessary initialization of the
     * underlying cache implementation(s). Called exactly once during the
     * construction of a {@link org.hibernate.internal.SessionFactoryImpl}.
     *
     * @param settings The settings in effect.
     * @param properties The defined cfg properties
     *
     * @throws org.hibernate.cache.CacheException Indicates problems starting
     * the L2 cache impl; considered as a sign to stop
     * {@link org.hibernate.SessionFactory} building.
     */
    @Override
    public void start(Settings settings, Properties properties) throws CacheException {
    }

    /**
     * Life cycle callback to perform any necessary cleanup of the underlying
     * cache implementation(s). Called exactly once during
     * {@link org.hibernate.SessionFactory#close}.
     */
    @Override
    public void stop() {
    }

    /**
     * By default should we perform "minimal puts" when using this second level
     * cache implementation?
     *
     * @return True if "minimal puts" should be performed by default; false
     * otherwise.
     */
    @Override
    public boolean isMinimalPutsEnabledByDefault() {
        return false;
    }

    /**
     * Get the default access type for {@link EntityRegion entity} and
     * {@link CollectionRegion collection} regions.
     *
     * @return This factory's default access type.
     */
    @Override
    public AccessType getDefaultAccessType() {
        return  AccessType.READ_WRITE;
    }

    /**
     * Generate a timestamp.
     * <p/>
     * This is generally used for cache content locking/unlocking purposes
     * depending upon the access-strategy being used.
     *
     * @return The generated timestamp.
     */
    @Override
    public long nextTimestamp() {
        return Timestamper.next();
    }

    /**
     * Build a cache region specialized for storing entity data.
     *
     * @param regionName The name of the region.
     * @param properties Configuration properties.
     * @param metadata Information regarding the type of data to be cached
     *
     * @return The built region
     *
     * @throws CacheException Indicates problems building the region.
     */
    @Override
    public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
        return new TayzGridEntityRegion(regionName, properties, metadata);
    }

    /**
     * Build a cache region specialized for storing NaturalId to Primary Key
     * mappings.
     *
     * @param regionName The name of the region.
     * @param properties Configuration properties.
     * @param metadata Information regarding the type of data to be cached
     *
     * @return The built region
     *
     * @throws CacheException Indicates problems building the region.
     */
    @Override
    public NaturalIdRegion buildNaturalIdRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
        return new TayzGridNaturalIdRegion(regionName, properties, metadata);
    }

    /**
     * Build a cache region specialized for storing collection data.
     *
     * @param regionName The name of the region.
     * @param properties Configuration properties.
     * @param metadata Information regarding the type of data to be cached
     *
     * @return The built region
     *
     * @throws CacheException Indicates problems building the region.
     */
    @Override
    public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
        return new TayzGridCollectionRegion(regionName, properties, metadata);
    }

    /**
     * Build a cache region specialized for storing query results
     *
     * @param regionName The name of the region.
     * @param properties Configuration properties.
     *
     * @return The built region
     *
     * @throws CacheException Indicates problems building the region.
     */
    @Override
    public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
        return new TayzGridQueryResultRegion(regionName, properties);
    }

    /**
     * Build a cache region specialized for storing update-timestamps data.
     *
     * @param regionName The name of the region.
     * @param properties Configuration properties.
     *
     * @return The built region
     *
     * @throws CacheException Indicates problems building the region.
     */
    @Override
    public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
        return new TayzGridTimestampsRegion(regionName, properties);
    }
}
