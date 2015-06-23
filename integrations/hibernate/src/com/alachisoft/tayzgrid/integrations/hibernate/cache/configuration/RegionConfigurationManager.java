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

package com.alachisoft.tayzgrid.integrations.hibernate.cache.configuration;

import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;

public class RegionConfigurationManager {
    RegionConfiguraton[] _regions=null;
    
    public RegionConfigurationManager(CacheRegions regions) throws ConfigurationException
    {
        if(regions==null || regions.getRegions()==null)
        {
            throw new ConfigurationException("cache-regions not specified in application config.");
        }
        _regions=regions.getRegions();
        for(int i=0;i<_regions.length;i++)
            validateRegionConfig(_regions[i]);
    }
    
    public RegionConfiguraton getRegionConfig(String regionName)
    {
        for(int i=0;i<_regions.length;i++)
        {
            if(_regions[i].getRegionName().equals(regionName))
                return _regions[i];
        }
        return null;
    }
    
    private void validateRegionConfig(RegionConfiguraton region) throws ConfigurationException
    {

        if(region.getRegionName()==null || region.getRegionName().isEmpty())
            throw new ConfigurationException("region-name cannot be null or empty.");
        if (region.getCacheName()==null || region.getRegionName().isEmpty())
                throw new ConfigurationException("cache-name cannot be null in region = " + region.getRegionName());
            if((!region.getExpirationType().equalsIgnoreCase("absolute")) && (!region.getExpirationType().equalsIgnoreCase("sliding")) && (!region.getExpirationType().equalsIgnoreCase("none")))
                throw new ConfigurationException("Invalid value for expiraion-type in region = " + region.getRegionName());
            if (!"none".equals(region.getExpirationType().toLowerCase()))
            {
                if(region.getExpirationPeriod()<=0)
                    throw new ConfigurationException("Invalid value for expiraion-period in region = " + region.getRegionName() + ". Expiraion period must be greater than zero.");
            }

            if (region.getPriority().equalsIgnoreCase("abovenormal"))
                region.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.AboveNormal);
            else if (region.getPriority().equalsIgnoreCase("belownormal"))
                region.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.BelowNormal);
            else if (region.getPriority().equalsIgnoreCase("default"))
                region.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.Default);
            else if (region.getPriority().equalsIgnoreCase("high"))
                region.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.High);
            else if (region.getPriority().equalsIgnoreCase("low"))
                region.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.Low);
            else if (region.getPriority().equalsIgnoreCase("normal"))
                region.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.Normal);
            else if (region.getPriority().equalsIgnoreCase("notremovable"))
                region.setCacheItemPriority(com.alachisoft.tayzgrid.runtime.CacheItemPriority.NotRemovable);
            else
                throw new ConfigurationException("Invalid value for priority in region = " + region.getRegionName());
    }
    
    public boolean contains(String regionName)
    {
        return this.getRegionConfig(regionName)!=null;
    }   
}
