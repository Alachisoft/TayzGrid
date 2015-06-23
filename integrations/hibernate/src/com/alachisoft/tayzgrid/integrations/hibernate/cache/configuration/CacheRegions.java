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
