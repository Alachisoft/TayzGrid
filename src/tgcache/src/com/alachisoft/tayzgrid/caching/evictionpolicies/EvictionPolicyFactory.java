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

package com.alachisoft.tayzgrid.caching.evictionpolicies;

import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;

public class EvictionPolicyFactory
{
    public static IEvictionPolicy CreateDefaultEvictionPolicy()
    {
        return null;
    }

    /**
     * Internal method that creates a cache policy. A HashMap containing the config parameters is passed to this method.
     */
    public static IEvictionPolicy CreateEvictionPolicy(java.util.Map properties) throws ConfigurationException
    {
        if (properties == null)
        {
            throw new IllegalArgumentException("properties");
        }

        try
        {
            float evictRatio = 0;
            if (properties.containsKey("evict-ratio"))
            {
                evictRatio = Float.parseFloat((String) properties.get("evict-ratio"));
            }
            IEvictionPolicy evictionPolicy = null;

            String scheme = "";
            
            scheme = String.valueOf(properties.get("class"));
            scheme = scheme.toLowerCase();
           
            java.util.Map schemeProps = (java.util.Map) properties.get(scheme);
            if (scheme.equals("lru"))
            {
                evictionPolicy = new LRUEvictionPolicy(schemeProps, evictRatio);
            }
            else if (scheme.equals("lfu"))
            {
                evictionPolicy = new LFUEvictionPolicy(schemeProps, evictRatio);
            }
            else if (scheme.equals("priority"))
            {
                evictionPolicy = new PriorityEvictionPolicy(schemeProps, evictRatio);
            }
            if (evictionPolicy == null)
            {
                throw new ConfigurationException("Invalid Eviction Policy: " + scheme);
            }
            //return a thread safe eviction policy.
            return evictionPolicy;
        }
        catch (ConfigurationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ConfigurationException("EvictionPolicyFactory.CreateEvictionPolicy(): " + e.toString());
        }
    }
}
