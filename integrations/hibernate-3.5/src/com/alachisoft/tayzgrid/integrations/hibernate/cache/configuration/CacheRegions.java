/*
* ===============================================================================
* Alachisoft (R) TayzGrid Integrations
* TayzGrid Provider for Hibernate
* ===============================================================================
* Copyright © Alachisoft.  All rights reserved.
* THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY
* OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT
* LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
* FITNESS FOR A PARTICULAR PURPOSE.
* ===============================================================================
*/
package com.alachisoft.tayzgrid.integrations.hibernate.cache.configuration;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;

public class CacheRegions {
    private RegionConfiguraton [] _regions;
    
    @ConfigurationSectionAnnotation(value = "region")
    public RegionConfiguraton[] getRegions()
    {
        return _regions;
    }
    
    @ConfigurationSectionAnnotation(value = "region")
    public void setRegions(Object [] value)
    {
        _regions=new RegionConfiguraton[value.length];
        for(int i=0;i<value.length;i++)
        _regions[i]=(RegionConfiguraton) value[i];
    }
    
}
