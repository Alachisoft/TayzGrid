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

package com.alachisoft.tayzgrid.jsr107.configuration;

import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import javax.cache.configuration.MutableConfiguration;


public class CacheConfiguration<K, V> extends MutableConfiguration<K, V>{
    protected JSR107InitParams initParams = new JSR107InitParams();
    
    
    public long getCacheSize()
    {
        return initParams.getCacheServerConfig().getCacheSettings().getStorage().getSize();
    }
        
    public CacheConfiguration<K, V> setCacheSize(long size)
    {
        initParams.getCacheServerConfig().getCacheSettings().getStorage().setSize(size);
        return this;
    }
    
    public String getLogPath()
    {
        return initParams.getCacheServerConfig().getCacheSettings().getLog().getLocation();
    }
    
    public CacheConfiguration<K, V> setLogPath(String path)
    {
        initParams.getCacheServerConfig().getCacheSettings().getLog().setLocation(path);
        return this;
    }
    
    public int getCleanInterval()
    {
        return initParams.getCacheServerConfig().getCacheSettings().getCleanup().getInterval();
    }
    
    public CacheConfiguration<K, V> setCleanInterval(int cleanInterval)
    {
        initParams.getCacheServerConfig().getCacheSettings().getCleanup().setInterval(cleanInterval);
        return this;
    }
    
    
    public boolean isEvictionEnabled()
    {
        return initParams.getCacheServerConfig().getCacheSettings().getEvictionPolicy().getEnabled();
    }
    
    public CacheConfiguration<K, V> setEvictionEnabled(boolean isEvictionEnabled)
    {
        initParams.getCacheServerConfig().getCacheSettings().getEvictionPolicy().setEnabled(isEvictionEnabled);
        return this;
    }
    
    public java.math.BigDecimal getEvictionRatio()
    {
        return initParams.getCacheServerConfig().getCacheSettings().getEvictionPolicy().getEvictionRatio();
    }
    
    public CacheConfiguration<K, V> setEvictionRatio(java.math.BigDecimal evictionRatio)
    {
        initParams.getCacheServerConfig().getCacheSettings().getEvictionPolicy().setEvictionRatio(evictionRatio);
        return this;
    }
    
    public CacheEvictionPolicy getEvicionPolicy()
    {
        return CacheEvictionPolicy.forValue(initParams.getCacheServerConfig().getCacheSettings().getEvictionPolicy().getPolicy());
    }
    
    public CacheConfiguration<K, V> setEvictionPolicy(CacheEvictionPolicy evictionPolicy)
    {
        initParams.getCacheServerConfig().getCacheSettings().getEvictionPolicy().setPolicy(evictionPolicy.getValue());
        return this;
    }
    
    public CacheItemPriority getDefaultCacheItemPriority()
    {
        return GetPriorityValue(initParams.getCacheServerConfig().getCacheSettings().getEvictionPolicy().getDefaultPriority());
    }
    
    public CacheConfiguration<K, V> setDefaultCacheItemPriority(CacheItemPriority defaultCacheItemPriority)
    {
        initParams.getCacheServerConfig().getCacheSettings().getEvictionPolicy().setDefaultPriority(GetPriorityString(defaultCacheItemPriority));
        return this;
    }
    
        /**
     * Convert the string representation of Priority to PriorityVale enumeration
     *
     * @param priority
     * @return
     */
    private CacheItemPriority GetPriorityValue(String priority)
    {
        priority = priority.toLowerCase();
        if (priority.equals("notremovable"))
        {
            return CacheItemPriority.NotRemovable;
        }
        else if (priority.equals("high"))
        {
            return CacheItemPriority.High;
        }
        else if (priority.equals("above-normal"))
        {
            return CacheItemPriority.AboveNormal;
        }
        else if (priority.equals("below-normal"))
        {
            return CacheItemPriority.BelowNormal;
        }
        else if (priority.equals("low"))
        {
            return CacheItemPriority.Low;
        }
        return CacheItemPriority.Default;
    }
    
    private String GetPriorityString(CacheItemPriority priority)
    {
        if (priority == CacheItemPriority.NotRemovable)
        {
            return "notremovable";
        }
        else if (priority == CacheItemPriority.High)
        {
            return "high";
        }
        else if (priority == CacheItemPriority.AboveNormal)
        {
            return "above-normal";
        }
        else if (priority == CacheItemPriority.BelowNormal)
        {
            return "below-normal";
        }
        else if (priority == CacheItemPriority.Low)
        {
            return "low";
        }
        return "default";
    }
    
    public JSR107InitParams getInitParamsInternal()
    {
        return initParams;
    }
}
