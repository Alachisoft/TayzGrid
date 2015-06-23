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

package com.alachisoft.tayzgrid.config;

import com.alachisoft.tayzgrid.caching.util.CacheInfo;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import java.util.Iterator;
import java.util.Map;

/**
 *
 *
 * @param clustername
 * @param cluster
 */
/**
 * Deals in tasks specific to configuration.
 */
public class ConfigHelper
{

    public static OnClusterConfigUpdate OnConfigUpdated;

    /**
     * Returns name of Cache from the property string.
     *
     * @param properties properties map
     * @return cache name.
     */
    public static CacheInfo GetCacheInfo(String propstring) throws ConfigurationException
    {
        PropsConfigReader pr = new PropsConfigReader(propstring);
        CacheInfo inf = GetCacheInfo(pr.getProperties());
        inf.setConfigString(propstring);
        return inf;
    }

    /**
     * Returns name of Cache from the property map.
     *
     * @param properties properties map
     * @return cache name.
     */
    private static CacheInfo GetCacheInfo(java.util.Map properties) throws ConfigurationException
    {
        if (!properties.containsKey("cache"))
        {
            throw new ConfigurationException("Missing configuration attribute 'cache'");
        }

        CacheInfo inf = new CacheInfo();
        java.util.Map cacheConfig = (java.util.Map) properties.get("cache");

        String schemeName = "";
        if (cacheConfig.containsKey("name"))
        {
            inf.setName(String.valueOf(cacheConfig.get("name")).trim());
        }

        if (!cacheConfig.containsKey("class"))
        {
            throw new ConfigurationException("Missing configuration attribute 'class'");
        }

        schemeName = String.valueOf(cacheConfig.get("class"));
        if (inf.getName().length() < 1)
        {
            inf.setName(schemeName);
        }

        if (!cacheConfig.containsKey("cache-classes"))
        {
            throw new ConfigurationException("Missing configuration section 'cache-classes'");
        }
        java.util.Map cacheClasses = (java.util.Map) cacheConfig.get("cache-classes");

        if (!cacheClasses.containsKey(schemeName.toLowerCase()))
        {
            throw new ConfigurationException("Cannot find cache class '" + schemeName + "'");
        }
        java.util.Map schemeProps = (java.util.Map) cacheClasses.get(schemeName.toLowerCase());

        if (!schemeProps.containsKey("type"))
        {
            throw new ConfigurationException("Cannot find the type of cache, invalid configuration for cache class '" + schemeName + "'");
        }

        inf.setClassName(String.valueOf(schemeProps.get("type")));
        return inf;
    }

    /**
     * Returns an xml config given a properties map.
     *
     * @param properties properties map
     * @return xml config.
     */
    public static String CreatePropertiesXml(java.util.Map properties, int indent, boolean format)
    {
        Iterator it = properties.entrySet().iterator();
        StringBuilder returnStr = new StringBuilder(8096);
        StringBuilder nestedStr = new StringBuilder(8096);

        String preStr = format ? ConfigHelper.padRight("",indent * 2) : "";
        String endStr = format ? "\n" : "";

        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry) it.next();

            String keyName = (String) ((pair.getKey() instanceof String) ? pair.getKey() : null);
            String attributes = "";

            if (pair.getValue() instanceof java.util.HashMap)
            {
                java.util.HashMap subproperties = (java.util.HashMap) pair.getValue();

                if (keyName.equals("cluster") && OnConfigUpdated != null)
                {
                    OnConfigUpdated.invoke(subproperties.get("group-id").toString(), pair);
                }
                if (subproperties.containsKey("type") && subproperties.containsKey("id"))
                {
                    keyName = (String) subproperties.get("type");

                    if (subproperties.containsKey("partitionId"))
                    {
                        attributes = " id='" + subproperties.get("id") + "'" + " partitionId='" + subproperties.get("partitionId") + "'";
                    }
                    else
                    {
                        attributes = " id='" + subproperties.get("id") + "'";
                    }

                    subproperties = (java.util.HashMap) subproperties.clone();
                    subproperties.remove("id");
                    subproperties.remove("type");
                }
                nestedStr.append(preStr).append("<" + keyName + attributes + ">").append(endStr);
                nestedStr.append(CreatePropertiesXml(subproperties, indent + 1, format)).append(preStr).append("</" + keyName + ">").append(endStr);
            }
            else
            {
                returnStr.append(preStr).append("<" + keyName + ">").append(pair.getValue()).append("</" + keyName + ">").append(endStr);
            }
        }
        returnStr.append(nestedStr.toString());

        return returnStr.toString();
    }

    public static String CreatePropertiesXml2(java.util.Map properties, int indent, boolean format)
    {
        Iterator it = properties.entrySet().iterator();
        StringBuilder returnStr = new StringBuilder(8096);
        StringBuilder nestedStr = new StringBuilder(8096);

        String preStr = format ? ConfigHelper.padRight("",indent * 2) : "";
        String endStr = format ? "\n" : "";

        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry) it.next();

            String keyName = (String) ((pair.getKey() instanceof String) ? pair.getKey() : null);
            String attributes = "";
            String cacheName = "";

            if (pair.getValue() instanceof java.util.HashMap)
            {
                java.util.HashMap subproperties = (java.util.HashMap) pair.getValue();

                if (subproperties.containsKey("type") && subproperties.containsKey("name"))
                {
                    cacheName = (String) ((subproperties.get("name") instanceof String) ? subproperties.get("name") : null);
                    keyName = (String) subproperties.get("type");

                    subproperties = (java.util.HashMap) subproperties.clone();
                    subproperties.remove("type");
                }

                nestedStr.append(preStr).append("<" + keyName + BuildAttributes(subproperties));
                if (subproperties.isEmpty())
                {
                    nestedStr.append("/>").append(endStr);
                }
                else
                {
                    if (subproperties.size() == 1)
                    {
                        Iterator ide = subproperties.entrySet().iterator();
                        while (ide.hasNext())
                        {
                            Map.Entry pair1 = (Map.Entry) ide.next();
                            if (((String) pair1.getKey()).toLowerCase().compareTo(cacheName) == 0 && pair1.getValue() instanceof java.util.Map)
                            {
                                subproperties = (java.util.HashMap) ((pair1.getValue() instanceof java.util.HashMap) ? pair.getValue() : null);
                            }
                        }
                    }
                    nestedStr.append(">").append(endStr);
                    nestedStr.append(CreatePropertiesXml2(subproperties, indent + 1, format)).append(preStr).append("</" + keyName + ">").append(endStr);
                }
            }
        }
        returnStr.append(nestedStr.toString());

        return returnStr.toString();
    }

    private static String BuildAttributes(java.util.HashMap subProps)
    {
        StringBuilder attributes = new StringBuilder();
        String preString = " ";

        Object tempVar = subProps.clone();
        java.util.HashMap tmp = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);

        Iterator ide = tmp.entrySet().iterator();
        while (ide.hasNext())
        {
            Map.Entry pair = (Map.Entry) ide.next();
            String key = (String) ((pair.getKey() instanceof String) ? pair.getKey() : null);
            if (!(pair.getValue() instanceof java.util.HashMap))
            {
                attributes.append(preString).append(key).append("=").append("\"").append(pair.getValue()).append("\"");
                subProps.remove(key);
            }
        }
        return attributes.toString();
    }

    /**
     * Returns an xml config given a properties map.
     *
     * @param properties properties map
     * @return xml config.
     */
    public static String CreatePropertiesXml(java.util.Map properties)
    {
        return CreatePropertiesXml(properties, 0, false);
    }

    public static String CreatePropertiesXml(java.util.Map properties, String configid)
    {
        StringBuilder returnStr = new StringBuilder("<cache-configuration id='");
        returnStr.append(configid).append("'>");
        returnStr.append(CreatePropertiesXml(properties, 0, false)).append("</cache-configuration>");
        return returnStr.toString();
    }

    /**
     * Returns a property string given a properties map.
     *
     * @param properties properties map
     * @return property string
     */
    public static String CreatePropertyString(java.util.Map properties)
    {
        return CreatePropertyString(properties, 0, false);
    }

    /**
     * Returns a property string given a properties map.
     *
     * @param properties properties map
     * @return property string
     */
    public static String CreatePropertyString(java.util.Map properties, int indent, boolean format)
    {
        Iterator it = properties.entrySet().iterator();
        StringBuilder returnStr = new StringBuilder(8096);
        StringBuilder nestedStr = new StringBuilder(8096);

        String preStr = format ? ConfigHelper.padRight("",indent * 2) : "";
        String endStr = format ? "\n" : "";

        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry) it.next();
            if (pair.getValue() instanceof java.util.HashMap)
            {
                java.util.HashMap subproperties = (java.util.HashMap) pair.getValue();

                if (pair.getKey().equals("cluster") && OnConfigUpdated != null)
                {
                    OnConfigUpdated.invoke(subproperties.get("group-id").toString(), pair);
                }

                if (subproperties.containsKey("type") && subproperties.containsKey("id"))
                {
                    nestedStr.append(preStr).append(subproperties.get("id"));
                    nestedStr.append("=").append(subproperties.get("type")).append(endStr);

                    subproperties = (java.util.HashMap) subproperties.clone();
                    subproperties.remove("id");
                    subproperties.remove("type");
                }
                else
                {
                    nestedStr.append(preStr).append(pair.getKey().toString()).append(endStr);
                }

                nestedStr.append(preStr).append("(").append(endStr).append(CreatePropertyString(subproperties, indent + 1, format)).append(preStr).append(")").append(endStr);
            }
            else
            {
                returnStr.append(preStr).append(pair.getKey().toString());
                if (pair.getValue() instanceof String)
                {
                    returnStr.append("='").append(pair.getValue()).append("';").append(endStr);
                }
                else
                {
                    returnStr.append("=").append(pair.getValue()).append(";").append(endStr);
                }
            }
        }
        returnStr.append(nestedStr.toString());
        return returnStr.toString();
    }

    /**
     * Returns a property string given a properties map.
     *
     * @param properties properties map
     * @return property string
     */
    public static String CreatePropertyString2(java.util.Map properties, int indent, boolean format)
    {
        Iterator it = properties.entrySet().iterator();
        StringBuilder returnStr = new StringBuilder(8096);
        StringBuilder nestedStr = new StringBuilder(8096);

        String preStr = format ? ConfigHelper.padRight("",indent * 2) : "";
        String endStr = format ? "\n" : "";

        while (it.hasNext())
        {
            Map.Entry pair = (Map.Entry) it.next();
            if (pair.getValue() instanceof java.util.HashMap)
            {
                java.util.HashMap subproperties = (java.util.HashMap) pair.getValue();
                if (subproperties.containsKey("type") && subproperties.containsKey("id"))
                {
                    nestedStr.append(preStr).append(subproperties.get("id"));
                    nestedStr.append("=").append(subproperties.get("type")).append(endStr);

                    subproperties = (java.util.HashMap) subproperties.clone();
                    subproperties.remove("id");
                    subproperties.remove("type");
                }
                else
                {
                    nestedStr.append(preStr).append(pair.getKey().toString()).append(endStr);
                }

                nestedStr.append(preStr).append("(").append(endStr).append(CreatePropertyString(subproperties, indent + 1, format)).append(preStr).append(")").append(endStr);
            }
            else
            {
                returnStr.append(preStr).append(pair.getKey().toString());
                if (pair.getValue() instanceof String)
                {
                    returnStr.append("='").append(pair.getValue()).append("';").append(endStr);
                }
                else
                {
                    returnStr.append("=").append(pair.getValue()).append(";").append(endStr);
                }
            }
        }
        returnStr.append(nestedStr.toString());
        return returnStr.toString();
    }

    /**
     * Finds and returns a cache scheme specified as attribute of other caches schemes. Handles the case of a ref to a scheme as well as inline cache definitions. the returned
     * propmap contains the props for the intended scheme.
     *
     * @param cacheSchemes
     * @param properties
     * @param caheName
     * @return
     */
    public static java.util.Map GetCacheScheme(java.util.Map cacheClasses, java.util.Map properties, String cacheName) throws ConfigurationException
    {
        java.util.Map cacheProps = null;
        // check if a reference to some scheme is specified
        if (properties.containsKey(cacheName + "-ref"))
        {
            String cacheScheme = String.valueOf(properties.get(cacheName + "-ref")).toLowerCase();
            if (!cacheClasses.containsKey(cacheScheme))
            {
                throw new ConfigurationException("Cannot find cache class '" + cacheScheme + "'");
            }
            // get the properties from the scheme
            cacheProps = (java.util.Map) cacheClasses.get(cacheScheme);
        }
        else if (properties.containsKey(cacheName))
        {
            // no reference specified, i.e., inline definition is specified.
            cacheProps = (java.util.Map) properties.get(cacheName);
        }
        if ((cacheProps == null) || !cacheProps.containsKey("type"))
        {
            throw new ConfigurationException("Cannot find the type of cache, invalid configuration for cache class");
        }

        return cacheProps;
    }

    public static String SafeGet(java.util.Map h, String key)
    {
        return SafeGet(h, key, null);
    }

    public static String SafeGet(java.util.Map h, String key, Object def)
    {
        Object res = null;
        if (h != null)
        {
            res = h.get(key);
        }
        if (res == null)
        {
            res = def;
        }
        if (res == null)
        {
            return "";
        }
        return res.toString();
    }

    public static String SafeGetPair(java.util.Map h, String key, Object def)
    {
        String res = SafeGet(h, key, def);
        if (res.equals(""))
        {
            return res;
        }

        StringBuilder b = new StringBuilder(64);
        b.append(key).append("=").append(res.toString()).append(";");
        return b.toString();
    }

    /**
     * Builds and returns a property string understood by the lower layer, i.e., Cluster Uses the properties specified in the configuration, and defaults for others.
     *
     * @param properties cluster properties
     * @return property string used by Cluster
     */
    public static String GetClusterPropertyString(java.util.Map properties, long opTimeout) throws ConfigurationException
    {
        boolean udpCluster = true;

        // check if a reference to some scheme is specified
        String cacheScheme = SafeGet(properties, "class").toLowerCase();
        if (cacheScheme.equals("tcp"))
        {
            udpCluster = false;
        }

        if (!properties.containsKey("channel"))
        {
            throw new ConfigurationException("Cannot find channel properties");
        }

        java.util.Map channelprops = (java.util.Map) ((properties.get("channel") instanceof java.util.Map) ? properties.get("channel") : null);
        if (udpCluster)
        {
            return ChannelConfigBuilder.BuildUDPConfiguration(channelprops);
        }
        return ChannelConfigBuilder.BuildTCPConfiguration(channelprops, opTimeout);
    }

    public static String GetClusterPropertyString(java.util.Map properties, String userId, String password, long opTimeout, boolean isReplica) throws ConfigurationException
    {
        boolean udpCluster = true;

        // check if a reference to some scheme is specified

        String cacheScheme = SafeGet(properties, "class").toLowerCase();
        if (cacheScheme.equals("tcp"))
        {
            udpCluster = false;
        }
        if (!properties.containsKey("channel"))
        {
            throw new ConfigurationException("Cannot find channel properties");
        }

        java.util.Map channelprops = (java.util.Map) ((properties.get("channel") instanceof java.util.Map) ? properties.get("channel") : null);
        if (udpCluster)
        {
            return ChannelConfigBuilder.BuildUDPConfiguration(channelprops);
        }
        return ChannelConfigBuilder.BuildTCPConfiguration(channelprops, userId, password, opTimeout, isReplica);
    }

    public static String padRight(String s, int n)
    {
        return String.format("%1$-" + n + "s", s);
    }

}
