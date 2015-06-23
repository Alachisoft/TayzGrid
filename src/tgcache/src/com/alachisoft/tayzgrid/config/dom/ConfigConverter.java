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

package com.alachisoft.tayzgrid.config.dom;

import com.alachisoft.tayzgrid.common.CaseInsensitiveMap;
import com.alachisoft.tayzgrid.common.enums.DataFormat;
import com.alachisoft.tayzgrid.common.util.PortCalculator;
import com.alachisoft.tayzgrid.config.newdom.Attrib;
import com.alachisoft.tayzgrid.config.newdom.BatchConfig;
import com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy;
import com.alachisoft.tayzgrid.config.newdom.TaskConfiguration;
import com.alachisoft.tayzgrid.config.newdom.WriteBehind;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ConfigConverter
{

    public static java.util.HashMap ToHashMap(CacheServerConfig config)
    {
        return DomToHashMap.GetConfig(config);
    }

    public static java.util.HashMap ToHashMap(CacheServerConfig[] configs)
    {
        return DomToHashMap.GetConfig(configs);
    }

    public static CacheServerConfig[] ToDom(java.util.HashMap config)
    {
        return HashMapToDom.GetConfig(config);
    }

    private static class HashMapToDom
    {

        public static CacheServerConfig[] GetConfig(java.util.HashMap config)
        {
            CacheServerConfig[] caches = new CacheServerConfig[config.size()];
            int i = 0;
            for (Iterator it = config.values().iterator(); it.hasNext();)
            {
                java.util.HashMap cache = (java.util.HashMap) it.next();

                caches[i++] = GetCacheConfiguration(new HashMap(new CaseInsensitiveMap((Map<String,Object>)(cache))));
            }
            return caches;
        }

        private static CacheServerConfig GetCacheConfiguration(java.util.HashMap settings)
        {
            CacheServerConfig cache = new CacheServerConfig();
            cache.setName(settings.get("id").toString());
            if (settings.containsKey("web-cache"))
            {
                GetWebCache(cache, (java.util.HashMap) settings.get("web-cache"));
            }
            if (settings.containsKey("cache"))
            {
                GetCache(cache, (java.util.HashMap) settings.get("cache"));
            }            
            return cache;
        }

        private static void GetCache(CacheServerConfig cache, java.util.HashMap settings)
        {
            if (settings.containsKey("config-id"))
            {
                cache.setConfigID((Double) (settings.get("config-id")));
            }
            if (settings.containsKey("last-modified"))
            {
                cache.setLastModified(settings.get("last-modified").toString());
            }
            if (settings.containsKey("auto-start"))
            {
                cache.setAutoStartCacheOnServiceStartup((Boolean) (settings.get("auto-start")));
            }
            if(settings.containsKey("data-format"))
            {                
                cache.setDataFormat(settings.get("data-format").toString());
            }
            if (settings.containsKey("backing-source"))
            {
                cache.setBackingSource(GetBackingSource((java.util.HashMap) settings.get("backing-source")));
            }
            if (settings.containsKey("cache-loader"))
            {
                cache.setCacheLoader(GetCacheLoader((java.util.HashMap) settings.get("cache-loader")));
            }

            if (settings.containsKey("log"))
            {
                cache.setLog(GetLog((java.util.HashMap) settings.get("log")));
            }
            if (settings.containsKey("cache-classes"))
            {
                GetCacheClasses(cache, (java.util.HashMap) settings.get("cache-classes"));
            }
            if (settings.containsKey("perf-counters"))
            {
                cache.setPerfCounters(GetPerfCounters(settings));
            }

            if (settings.containsKey("sql-dependency"))
            {
                cache.setSQLDependencyConfig(GetSQLDependency((java.util.HashMap) settings.get("sql-dependency")));
            }
            if (cache.getAlertsNotifications() != null)
            {
                cache.setAlertsNotifications(GetAlerts((java.util.HashMap) settings.get("alerts")));
            }
         
            if(settings.containsKey("tasks-config"))
            {
                cache.setTaskConfiguration(GetTaskConfiguration((java.util.HashMap) ((settings.get("task-config") instanceof java.util.HashMap) ? settings.get("task-config") : null)));
            }
        }

        private static com.alachisoft.tayzgrid.config.newdom.PerfCounters GetPerfCounters(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.PerfCounters perCounters = new com.alachisoft.tayzgrid.config.newdom.PerfCounters();
            perCounters.setEnabled((Boolean) (settings.get("perf-counters")));
            return perCounters;
        }



        private static com.alachisoft.tayzgrid.config.newdom.SQLDependencyConfig GetSQLDependency(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.SQLDependencyConfig sqlDependencyConfig = new com.alachisoft.tayzgrid.config.newdom.SQLDependencyConfig();
            if (settings.containsKey("use-default"))
            {
                sqlDependencyConfig.setUseDefault((Boolean) (settings.get("use-default")));
            }
            return sqlDependencyConfig;
        }

        private static com.alachisoft.tayzgrid.config.newdom.AlertsNotifications GetAlerts(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.AlertsNotifications alertNotification = new com.alachisoft.tayzgrid.config.newdom.AlertsNotifications();
            if (settings.containsKey("email-notification"))
            {
                java.util.HashMap emailNotification = (java.util.HashMap) ((settings.get("email-notification") instanceof java.util.HashMap) ? settings.get("email-notification") : null);
                alertNotification.setEMailNotifications(GetEmailNotifications(emailNotification));
            }

            if (settings.containsKey("alerts-types"))
            {
                java.util.HashMap alertType = (java.util.HashMap) ((settings.get("alerts-types") instanceof java.util.HashMap) ? settings.get("alerts-types") : null);
                alertNotification.setAlertsTypes(GetAlertsType(alertType));
            }
            return alertNotification;
        }

        private static com.alachisoft.tayzgrid.config.newdom.EmailNotifications GetEmailNotifications(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.EmailNotifications emailNotifications = new com.alachisoft.tayzgrid.config.newdom.EmailNotifications();

            if (settings.containsKey("email-notification-enabled"))
            {
                emailNotifications.setEmailNotificationEnabled((Boolean) (settings.get("email-notification-enabled")));
            }

            if (settings.containsKey("sender"))
            {
                emailNotifications.setSender((String) ((settings.get("sender") instanceof String) ? settings.get("sender") : null));
            }

            if (settings.containsKey("smtp-server"))
            {
                emailNotifications.setSmtpServer((String) ((settings.get("smtp-server") instanceof String) ? settings.get("smtp-server") : null));
            }

            if (settings.containsKey("smtp-port"))
            {
                emailNotifications.setSmtpPort((Integer) (settings.get("smtp-port")));
            }

            if (settings.containsKey("ssl"))
            {
                emailNotifications.setSSL((Boolean) (settings.get("ssl")));
            }

            if (settings.containsKey("authentication"))
            {
                emailNotifications.setAuthentication((Boolean) (settings.get("authentication")));
            }

            if (settings.containsKey("sender-login"))
            {
                emailNotifications.setLogin((String) ((settings.get("sender-login") instanceof String) ? settings.get("sender-login") : null));
            }

            if (settings.containsKey("sender-password"))
            {
                emailNotifications.setPassword((String) ((settings.get("sender-password") instanceof String) ? settings.get("sender-password") : null));
            }

            if (settings.containsKey("recipients"))
            {
                java.util.HashMap recipients = (java.util.HashMap) ((settings.get("recipients") instanceof java.util.HashMap) ? settings.get("recipients") : null);
                emailNotifications.setRecipients(GetRecipients(recipients));
            }

            return emailNotifications;
        }

        private static com.alachisoft.tayzgrid.config.newdom.NotificationRecipient[] GetRecipients(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.NotificationRecipient[] recipients = null;

            if (settings.size() != 0)
            {
                recipients = new com.alachisoft.tayzgrid.config.newdom.NotificationRecipient[settings.size()];
                int index = 0;
                Iterator ide = settings.entrySet().iterator();
                while (ide.hasNext())
                {
                    Map.Entry pair = (Map.Entry) ide.next();
                    recipients[index] = new com.alachisoft.tayzgrid.config.newdom.NotificationRecipient();
                    recipients[index].setID(pair.getKey().toString());
                    index++;
                }
            }
            return recipients;
        }

        private static com.alachisoft.tayzgrid.config.newdom.AlertsTypes GetAlertsType(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.AlertsTypes alertTypes = new com.alachisoft.tayzgrid.config.newdom.AlertsTypes();

            if (settings.containsKey("cache-stop"))
            {
                alertTypes.setCacheStop((Boolean) (settings.get("cache-stop")));
            }

            if (settings.containsKey("cache-start"))
            {
                alertTypes.setCacheStart((Boolean) (settings.get("cache-start")));
            }

            if (settings.containsKey("node-left"))
            {
                alertTypes.setNodeLeft((Boolean) (settings.get("node-left")));
            }

            if (settings.containsKey("node-joined"))
            {
                alertTypes.setNodeJoined((Boolean) (settings.get("node-joined")));
            }

            if (settings.containsKey("state-transfer-started"))
            {
                alertTypes.setStartTransferStarted((Boolean) (settings.get("state-transfer-started")));
            }

            if (settings.containsKey("state-transfer-stop"))
            {
                alertTypes.setStartTransferStop((Boolean) (settings.get("state-transfer-stop")));
            }

            if (settings.containsKey("state-transfer-error"))
            {
                alertTypes.setStartTransferError((Boolean) (settings.get("state-transfer-error")));
            }

            if (settings.containsKey("service-start-error"))
            {
                alertTypes.setServiceStartError((Boolean) (settings.get("service-start-error")));
            }

            if (settings.containsKey("cache-size"))
            {
                alertTypes.setCacheSize((Boolean) (settings.get("cache-size")));
            }

            if (settings.containsKey("general-error"))
            {
                alertTypes.setGeneralError((Boolean) (settings.get("general-error")));
            }

            if (settings.containsKey("licensing-error"))
            {
                alertTypes.setLicensingError((Boolean) (settings.get("licensing-error")));
            }

            if (settings.containsKey("configuration-error"))
            {
                alertTypes.setConfigurationError((Boolean) (settings.get("configuration-error")));
            }

            if (settings.containsKey("general-info"))
            {
                alertTypes.setGeneralInfo((Boolean) (settings.get("general-info")));
            }

            if (settings.containsKey("unhandled-exceptions"))
            {
                alertTypes.setUnHandledException((Boolean) (settings.get("unhandled-exceptions")));
            }

            return alertTypes;
        }

        private static void GetCacheClasses(CacheServerConfig cache, java.util.HashMap settings)
        {
            if (settings.containsKey(cache.getName()))
            {
                GetClassifiedCache(cache, (java.util.HashMap) settings.get(cache.getName()));
            }
        }

        private static void GetClassifiedCache(CacheServerConfig cache, java.util.HashMap settings)
        {
            if (settings.containsKey("cluster"))
            {
                cache.setCluster(GetCluster(settings));
            }
            if (settings.containsKey("internal-cache"))
            {
                GetInternalCache(cache, (java.util.HashMap) settings.get("internal-cache"));
            }
            else
            {
                GetInternalCache(cache, settings);
            }

            if (settings.containsKey("notifications"))
            {
                cache.setNotifications(GetNotifications((java.util.HashMap) settings.get("notifications")));
            }
        }

        private static com.alachisoft.tayzgrid.config.newdom.Notifications GetNotifications(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.Notifications notifications = new com.alachisoft.tayzgrid.config.newdom.Notifications();
            if (settings.containsKey("item-remove"))
            {
                notifications.setItemRemove((Boolean) (settings.get("item-remove")));
            }
            if (settings.containsKey("item-add"))
            {
                notifications.setItemAdd((Boolean) (settings.get("item-add")));
            }
            if (settings.containsKey("item-update"))
            {
                notifications.setItemUpdate((Boolean) (settings.get("item-update")));
            }
            if (settings.containsKey("cache-clear"))
            {
                notifications.setCacheClear((Boolean) (settings.get("cache-clear")));
            }
            return notifications;
        }

        private static void GetInternalCache(CacheServerConfig cache, java.util.HashMap settings)
        {
            if (settings.containsKey("indexes"))
            {
                cache.setQueryIndices(GetIndexes((java.util.HashMap) settings.get("indexes")));
            }
            if (settings.containsKey("storage"))
            {
                cache.setStorage(GetStorage((java.util.HashMap) settings.get("storage")));
            }
            if (settings.containsKey("scavenging-policy"))
            {
                cache.setEvictionPolicy(GetEvictionPolicy((java.util.HashMap) settings.get("scavenging-policy")));
            }
            if(settings.containsKey("expiration-policy"))
            {
                cache.setExpirationPolicy(GetExpirationPolicy((java.util.HashMap)settings.get("expiration-policy")));
            }
            if (settings.containsKey("clean-interval"))
            {
                cache.setCleanup(GetCleanup(settings));
            }
        }

        private static com.alachisoft.tayzgrid.config.newdom.Cleanup GetCleanup(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.Cleanup cleanup = new com.alachisoft.tayzgrid.config.newdom.Cleanup();
            cleanup.setInterval((Integer) (settings.get("clean-interval")));
            return cleanup;
        }

        private static com.alachisoft.tayzgrid.config.newdom.EvictionPolicy GetEvictionPolicy(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.EvictionPolicy evictionPolicy = new com.alachisoft.tayzgrid.config.newdom.EvictionPolicy();
            if (settings.containsKey("eviction-enabled"))
            {
                evictionPolicy.setEnabled((Boolean) (settings.get("eviction-enabled")));
            }
            if (settings.containsKey("priority"))
            {
                evictionPolicy.setDefaultPriority(((java.util.HashMap) settings.get("priority")).get("default-value").toString());
            }  
            if (settings.containsKey("class"))
            {
                evictionPolicy.setPolicy((String) ((settings.get("class") instanceof String) ? settings.get("class") : null));
            }
            if (settings.containsKey("evict-ratio"))
            {
                evictionPolicy.setEvictionRatio((java.math.BigDecimal) (settings.get("evict-ratio")));
            }
            return evictionPolicy;
        }
        
        private static com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy GetExpirationPolicy(java.util.HashMap map)
        {
            com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy policy = new ExpirationPolicy();
            if(map.containsKey("default-policy"))
                policy.setPolicyType((String)map.get("default-policy"));
            if(map.containsKey("duration"))
                policy.setDuration(Long.parseLong(map.get("duration").toString()));
            if(map.containsKey("unit"))
                policy.setUnit((String) map.get("unit"));
            return policy;
        }

        private static com.alachisoft.tayzgrid.config.newdom.QueryIndex GetIndexes(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.QueryIndex indexes = new com.alachisoft.tayzgrid.config.newdom.QueryIndex();
            if (settings.containsKey("index-classes"))
            {
                indexes.setClasses(GetIndexClasses((java.util.HashMap) settings.get("index-classes")));
            }
            return indexes;
        }

        private static com.alachisoft.tayzgrid.config.newdom.Class[] GetIndexClasses(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.Class[] classes = new com.alachisoft.tayzgrid.config.newdom.Class[settings.size()];
            int i = 0;
            for (Iterator it = settings.values().iterator(); it.hasNext();)
            {
                java.util.HashMap cls = (java.util.HashMap) it.next();
                classes[i++] = GetIndexClass(cls);
            }
            return classes;
        }

        private static com.alachisoft.tayzgrid.config.newdom.Class GetIndexClass(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.Class cls = new com.alachisoft.tayzgrid.config.newdom.Class();
            if (settings.containsKey("id"))
            {
                cls.setID(settings.get("id").toString());
            }
            if (settings.containsKey("name"))
            {
                cls.setName(settings.get("name").toString());
            }
            if (settings.containsKey("attributes"))
            {
                cls.setAttributes(GetIndexAttributes((java.util.HashMap) settings.get("attributes")));
            }
            return cls;
        }

        private static com.alachisoft.tayzgrid.config.newdom.Attrib[] GetIndexAttributes(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.Attrib[] attributes = new com.alachisoft.tayzgrid.config.newdom.Attrib[settings.size()];
            int i = 0;
            for (Iterator it = settings.values().iterator(); it.hasNext();)
            {
                java.util.HashMap attrib = (java.util.HashMap) it.next();
                attributes[i++] = GetIndexAttribute(attrib);
            }
            return attributes;
        }

        private static com.alachisoft.tayzgrid.config.newdom.Attrib GetIndexAttribute(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.Attrib attrib = new com.alachisoft.tayzgrid.config.newdom.Attrib();
            if (settings.containsKey("id"))
            {
                attrib.setID(settings.get("id").toString());
            }
            if (settings.containsKey("data-type"))
            {
                attrib.setType(settings.get("data-type").toString());
            }
            if (settings.containsKey("name"))
            {
                attrib.setName(settings.get("name").toString());
            }
            return attrib;
        }

        private static com.alachisoft.tayzgrid.config.newdom.Storage GetStorage(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.Storage storage = new com.alachisoft.tayzgrid.config.newdom.Storage();
            if (settings.containsKey("class"))
            {
                storage.setType(settings.get("class").toString());
            }
            if (settings.containsKey("heap"))
            {
                storage.setSize((Long) (((java.util.HashMap) settings.get("heap")).get("max-size")));
            }
            return storage;
        }
        
        private static com.alachisoft.tayzgrid.config.newdom.User[] GetSecurityUser(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.User[] users = null;

            if (settings.size() != 0)
            {
                users = new com.alachisoft.tayzgrid.config.newdom.User[settings.size()];
                int index = 0;
                Iterator ide = settings.entrySet().iterator();
                while (ide.hasNext())
                {
                    Map.Entry ent = (Map.Entry) ide.next();
                    users[index] = new com.alachisoft.tayzgrid.config.newdom.User((String) ent.getKey().toString());
                    index++;
                }
            }
            return users;
        }
        private static Cluster GetCluster(java.util.HashMap settings)
        {
            Cluster cluster = new Cluster();
            if (settings.containsKey("type"))
            {
                cluster.setTopology(settings.get("type").toString());
            }
            if (settings.containsKey("stats-repl-interval"))
            {
                cluster.setStatsRepInterval((Integer) (settings.get("stats-repl-interval")));
            }
            if (settings.containsKey("op-timeout"))
            {
                cluster.setOpTimeout((Integer) (settings.get("op-timeout")));
            }
            if (settings.containsKey("use-heart-beat"))
            {
                cluster.setUseHeartbeat((Boolean) (settings.get("use-heart-beat")));
            }

            settings = (java.util.HashMap) settings.get("cluster");
            if (settings.containsKey("channel"))
            {
                cluster.setChannel(GetChannel((java.util.HashMap) settings.get("channel"), 1));
            }

            return cluster;
        }

        private static Channel GetChannel(java.util.HashMap settings, int defaultPortRange)
        {
            Channel channel = new Channel(defaultPortRange);
            if (settings.containsKey("tcp"))
            {
                GetTcp(channel, (java.util.HashMap) settings.get("tcp"));
            }
            if (settings.containsKey("tcpping"))
            {
                GetTcpPing(channel, (java.util.HashMap) settings.get("tcpping"));
            }
            if (settings.containsKey("pbcast.gms"))
            {
                GetGMS(channel, (java.util.HashMap) settings.get("pbcast.gms"));
            }
            return channel;
        }

      

        private static void GetTcpPing(Channel channel, java.util.HashMap settings)
        {
            if (settings.containsKey("initial_hosts"))
            {
                channel.setInitialHosts(settings.get("initial_hosts").toString());
            }
            if (settings.containsKey("num_initial_members"))
            {
                channel.setNumInitHosts((Integer) (settings.get("num_initial_members")));
            }
            if (settings.containsKey("port_range"))
            {
                channel.setPortRange((Integer) (settings.get("port_range")));
            }
        }

        private static void GetTcp(Channel channel, java.util.HashMap settings)
        {
            if (settings.containsKey("start_port"))
            {
                channel.setTcpPort((Integer) (settings.get("start_port")));
            }
            if (settings.containsKey("port_range"))
            {
                channel.setPortRange((Integer) (settings.get("port_range")));
            }
            if (settings.containsKey("connection_retries"))
            {
                channel.setConnectionRetries((Integer) (settings.get("connection_retries")));
            }
            if (settings.containsKey("connection_retry_interval"))
            {
                channel.setConnectionRetryInterval((Integer) (settings.get("connection_retry_interval")));
            }

        }

        private static void GetGMS(Channel channel, java.util.HashMap settings)
        {
            if (settings.containsKey("join_retry_count"))
            {
                channel.setJoinRetries(Integer.parseInt(settings.get("join_retry_count").toString()));
            }
            if (settings.containsKey("join_retry_timeout"))
            {
                channel.setJoinRetryInterval((Integer) (settings.get("join_retry_timeout")));
            }
        }

        private static com.alachisoft.tayzgrid.config.newdom.Log GetLog(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.Log log = new com.alachisoft.tayzgrid.config.newdom.Log();
            if (settings.containsKey("enabled"))
            {
                log.setEnabled((Boolean) (settings.get("enabled")));
            }
            if (settings.containsKey("trace-errors"))
            {
                log.setTraceErrors((Boolean) (settings.get("trace-errors")));
            }
            if (settings.containsKey("trace-notices"))
            {
                log.setTraceNotices((Boolean) (settings.get("trace-notices")));
            }
            if (settings.containsKey("trace-debug"))
            {
                log.setTraceDebug((Boolean) (settings.get("trace-debug")));
            }
            if (settings.containsKey("trace-warnings"))
            {
                log.setTraceWarnings((Boolean) (settings.get("trace-warnings")));
            }

            if (settings.containsKey("log-path"))
            {
                log.setLocation((String) (settings.get("log-path")));
            }
            return log;
        }

        private static com.alachisoft.tayzgrid.config.newdom.TaskConfiguration GetTaskConfiguration(java.util.HashMap settings)
        {
            TaskConfiguration taskConfig = new TaskConfiguration();
            if(settings.containsKey("max-tasks"))
            {
                taskConfig.setMaxTasks((Integer) settings.get("max-tasks"));
            }
            if(settings.containsKey("chunk-size"))
            {
                taskConfig.setChunkSize((Integer) settings.get("chunk-size"));
            }
            if(settings.containsKey("communicate-stats"))
            {
                taskConfig.setCommunicateStats((Boolean) settings.get("communicate-stats"));
            }
            if(settings.containsKey("queue-size"))
            {
                taskConfig.setQueueSize((Integer) settings.get("queue-size"));
            }
            if(settings.containsKey("max-avoidable-exceptions"))
            {
                taskConfig.setMaxExceptions((Integer) settings.get("max-avoidable-exceptions"));
            }
            return taskConfig;
        }
        
        private static com.alachisoft.tayzgrid.config.newdom.BackingSource GetBackingSource(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.BackingSource backingSource = new com.alachisoft.tayzgrid.config.newdom.BackingSource();
            if (settings.containsKey("read-thru"))
            {
                backingSource.setReadthru(GetReadThru((java.util.HashMap) settings.get("read-thru")));
            }
            if (settings.containsKey("write-thru"))
            {
                backingSource.setWritethru(GetWriteThru((java.util.HashMap) settings.get("write-thru")));
            }
//            if(settings.containsKey("is-loader-enabled"))
//                backingSource.setIsLoader((Boolean)settings.get("is-loader-enabled"));
            return backingSource;
        }

        private static com.alachisoft.tayzgrid.config.newdom.CacheLoader GetCacheLoader(java.util.HashMap settings)
        {

            com.alachisoft.tayzgrid.config.newdom.CacheLoader CL = new com.alachisoft.tayzgrid.config.newdom.CacheLoader();
                CL.setProvider(new com.alachisoft.tayzgrid.config.newdom.ProviderAssembly());

                if (settings.containsKey("cache-load"))
                {
                    java.util.HashMap cacheload = (java.util.HashMap) settings.get("cache-load");
                    CL.getProvider().setAssemblyName(cacheload.get("assembly").toString());
                    CL.getProvider().setClassName(cacheload.get("classname").toString());
                    CL.getProvider().setFullProviderName(settings.get("full-name").toString());
                    CL.setRetries((Integer) (cacheload.get("retries")));
                    CL.setRetryInterval((Integer) (cacheload.get("retry-interval")));
                    if (cacheload.containsKey("parameters"))
                    {
                        CL.setParameters(GetParameters((java.util.HashMap) ((cacheload.get("parameters") instanceof java.util.HashMap) ? cacheload.get("parameters") : null)));
                    }
                }

            return CL;
        }

        private static com.alachisoft.tayzgrid.config.newdom.Writethru GetWriteThru(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.Writethru writeThru = new com.alachisoft.tayzgrid.config.newdom.Writethru();
            if (settings.containsKey("write-thru-providers"))
            {
                writeThru.setProviders(GetProviders((java.util.HashMap) ((settings.get("write-thru-providers") instanceof java.util.HashMap) ? settings.get("write-thru-providers") : null)));
            }
            return writeThru;
        }
        private static WriteBehind GetWriteBehind(java.util.HashMap settings) {
            if (settings == null) {
                return null;
            }

            WriteBehind writeBehind = new WriteBehind();

            if (settings.containsKey("mode")) {
                writeBehind.setMode(settings.get("mode").toString());
            }
            if (settings.containsKey("throttling-rate-per-sec")) {
                writeBehind.setThrottling(settings.get("throttling-rate-per-sec").toString());
            }
            if (settings.containsKey("failed-operations-queue-limit")) {
                writeBehind.setRequeueLimit(settings.get("failed-operations-queue-limit").toString());
            }
            if (settings.containsKey("failed-operations-eviction-ratio")) {
                writeBehind.setEviction(settings.get("failed-operations-eviction-ratio").toString());
            }
            if (settings.containsKey("batch-mode-config")) {
                writeBehind.setBatchConfig(GetBatchConfig((java.util.HashMap) ((settings.get("batch-mode-config") instanceof java.util.HashMap) ? settings.get("batch-mode-config") : null)));
            }

            return writeBehind;
        }

        private static BatchConfig GetBatchConfig(java.util.HashMap settings) {
            if (settings == null) {
                return null;
            }

            BatchConfig batchConfig = new BatchConfig();
            if (settings.containsKey("batch-interval")) {
                batchConfig.setBatchInterval(settings.get("batch-interval").toString());
            }
            if (settings.containsKey("operation-delay")) {
                batchConfig.setOperationDelay(settings.get("operation-delay").toString());
            }

            return batchConfig;
        }
        private static com.alachisoft.tayzgrid.config.newdom.Readthru GetReadThru(java.util.HashMap settings)
        {
            com.alachisoft.tayzgrid.config.newdom.Readthru readThru = new com.alachisoft.tayzgrid.config.newdom.Readthru();
            if (settings.containsKey("read-thru-providers"))
            {
                readThru.setProviders(GetProviders((java.util.HashMap) ((settings.get("read-thru-providers") instanceof java.util.HashMap) ? settings.get("read-thru-providers") : null)));
                
            }
            return readThru;
        }

        private static com.alachisoft.tayzgrid.config.newdom.Provider[] GetProviders(java.util.HashMap settings)
        {
            if (settings == null)
            {
                return null;
            }
            com.alachisoft.tayzgrid.config.newdom.Provider[] providers = new com.alachisoft.tayzgrid.config.newdom.Provider[settings.size()];
            int i = 0;
            Iterator ide = settings.entrySet().iterator();
            while (ide.hasNext())
            {
                Map.Entry ent = (Map.Entry) ide.next();
                if (ent.getValue() instanceof java.util.HashMap)
                {
                    com.alachisoft.tayzgrid.config.newdom.Provider provider = new com.alachisoft.tayzgrid.config.newdom.Provider();
                    java.util.HashMap properties = (java.util.HashMap) ent.getValue();
                    Iterator de = properties.entrySet().iterator();
                    while (de.hasNext())
                    {
                        Map.Entry pairDE = (Map.Entry) de.next();
                        if (pairDE.getKey().equals("assembly-name"))
                        {
                            provider.setAssemblyName((String) pairDE.getValue());
                        }
                        if (pairDE.getKey().equals("class-name"))
                        {
                            provider.setClassName((String) pairDE.getValue());
                        }
                        if (pairDE.getKey().equals("provider-name"))
                        {
                            provider.setProviderName((String) pairDE.getValue());
                        }

                        if (pairDE.getKey().equals("full-name"))
                        {
                            provider.setFullProviderName((String) pairDE.getValue());
                        }

                        if (pairDE.getKey().equals("default-provider"))
                        {
                            provider.setIsDefaultProvider((Boolean) pairDE.getValue());
                        }
                        if (pairDE.getKey().equals("parameters"))
                        {
                            provider.setParameters(GetParameters((java.util.HashMap) ((pairDE.getValue() instanceof java.util.HashMap) ? pairDE.getValue() : null)));
                        }
                        if(pairDE.getKey().equals("loader-only")) {
                            provider.setIsLoaderOnly((Boolean)pairDE.getValue());
                        }
                    }
                    providers[i] = provider;
                    i++;
                }
            }
            return providers;
        }

        private static com.alachisoft.tayzgrid.config.newdom.Parameter[] GetParameters(java.util.HashMap settings)
        {
            if (settings == null)
            {
                return null;
            }
            com.alachisoft.tayzgrid.config.newdom.Parameter[] parameters = new com.alachisoft.tayzgrid.config.newdom.Parameter[settings.size()];

            int i = 0;
            Iterator ide = settings.entrySet().iterator();
            while (ide.hasNext())
            {
                Map.Entry ent = (Map.Entry) ide.next();
                com.alachisoft.tayzgrid.config.newdom.Parameter parameter = new com.alachisoft.tayzgrid.config.newdom.Parameter();
                parameter.setName((String) ((ent.getKey() instanceof String) ? ent.getKey() : null));
                parameter.setParamValue((String) ((ent.getValue() instanceof String) ? ent.getValue() : null));
                parameters[i] = parameter;
                i++;
            }
            return parameters;
        }

        private static void GetWebCache(CacheServerConfig cache, java.util.HashMap settings)
        {
            if (settings.containsKey("shared"))
            {
                cache.setInProc(!(Boolean) (settings.get("shared")));
            }
        }
    
        private static void GetDataFormat(CacheServerConfig cache, java.util.HashMap settings) 
        {
            if (settings.containsKey("data-format")) 
                cache.setDataFormat((String) settings.get("data-format"));                
        }
    }
    private static class DomToHashMap
    {

        public static java.util.HashMap GetCacheConfiguration(CacheServerConfig cache)
        {
            java.util.HashMap config = new java.util.HashMap();
            config.put("type", "cache-configuration");
            config.put("id", cache.getName());
            config.put("cache", GetCache(cache));
            config.put("web-cache", GetWebCache(cache));            
            return config;
        }

        public static java.util.HashMap GetWebCache(CacheServerConfig cache)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("shared", (new Boolean((!cache.getInProc()))).toString().toLowerCase());
            settings.put("cache-id", cache.getName());
            return settings;
        }
        public static java.util.HashMap GetDataFormat(CacheServerConfig cache) {
        java.util.HashMap settings = new java.util.HashMap();
        settings.put("data-format", cache.getDataFormat());
        //settings.put("cache-id", cache.getName());
        return settings;
    }
        public static java.util.HashMap GetCache(CacheServerConfig cache)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("name", cache.getName());
            if (cache.getLog() != null)
            {
                settings.put("log", GetLog(cache.getLog()));
            }
            settings.put("config-id", cache.getConfigID());
            settings.put("data-format", cache.getDataFormat());
            if (cache.getLastModified() != null)
            {
                settings.put("last-modified", cache.getLastModified());
            }

            settings.put("auto-start", cache.getAutoStartCacheOnServiceStartup());
            settings.put("cache-classes", GetCacheClasses(cache));
            settings.put("class", cache.getName());
            //settings.put("management-port", cache.getManagementPort()); Waleed
            settings.put("management-port", PortCalculator.getManagementPort(cache.getClientPort()));
            settings.put("client-port", cache.getClientPort());

            if (cache.getPerfCounters() != null)
            {
                settings.put("perf-counters", cache.getPerfCounters().getEnabled());
            }

            if (cache.getSQLDependencyConfig() != null)
            {
                settings.put("sql-dependency", GetSQLDependency(cache.getSQLDependencyConfig()));
            }

            if (cache.getAlertsNotifications() != null)
            {
                settings.put("alerts", GetAlerts(cache.getAlertsNotifications()));
            }

            if (cache.getBackingSource() != null)
            {
                settings.put("backing-source", GetBackingSource(cache.getBackingSource()));
            }
            if (cache.getCacheLoader() != null)
            {
                settings.put("cache-loader", GetCacheLoader(cache.getCacheLoader()));
            }
           
            
            if(cache.getTaskConfiguration() != null)
            {
                settings.put("tasks-config", GetTaskConfiguration(cache.getTaskConfiguration()));
            }
            return settings;
        }

        private static java.util.HashMap GetBackingSource(com.alachisoft.tayzgrid.config.newdom.BackingSource backingSource)
        {

                    java.util.HashMap settings =new java.util.HashMap();
                    if (backingSource.getReadthru() != null)
                    {
                        settings.put("read-thru", GetReadThru(backingSource.getReadthru()));
                    }
                    if (backingSource.getWritethru() != null)
                    {
                        settings.put("write-thru", GetWriteThru(backingSource.getWritethru()));
                    }
            return settings;
        }

        private static java.util.HashMap GetCacheLoader(com.alachisoft.tayzgrid.config.newdom.CacheLoader CL)
        {
                    java.util.HashMap settings = new java.util.HashMap();
                    if (CL.getProvider() != null)
                    {
                        settings.put("assembly", CL.getProvider().getAssemblyName());
                        settings.put("classname", CL.getProvider().getClassName());
                        settings.put("full-name", CL.getProvider().getFullProviderName());
                        settings.put("retries", CL.getRetries());
                        settings.put("retry-interval", CL.getRetryInterval());
                        settings.put("enabled", CL.getEnabled());
                        if (CL.getParameters() != null)
                        {
                            settings.put("parameters", GetParameters(CL.getParameters()));
                        }
                    }
            return settings;
        }

        private static java.util.HashMap GetWriteThru(com.alachisoft.tayzgrid.config.newdom.Writethru writethru)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("enabled", (new Boolean(writethru.getEnabled())).toString());

            if (writethru.getProviders() != null)
            {
                settings.put("write-thru-providers", GetProviders(writethru.getProviders()));
            }
            if (writethru.getWriteBehind() != null)
            {
                settings.put("write-behind", GetWriteBehind(writethru.getWriteBehind()));
            }
            return settings;
        }
        private static java.util.HashMap GetWriteBehind(WriteBehind writeBehind) {
            java.util.HashMap settings = new java.util.HashMap();

            if (writeBehind != null) {
                settings.put("mode", writeBehind.getMode());
                settings.put("throttling-rate-per-sec", writeBehind.getThrottling());
                settings.put("failed-operations-queue-limit", writeBehind.getRequeueLimit());
                settings.put("failed-operations-eviction-ratio", writeBehind.getEviction());
                if (writeBehind.getBatchConfig() != null) {
                    settings.put("batch-mode-config", GetBatchConfig(writeBehind.getBatchConfig()));
                }
            }

            return settings;
        }

        private static java.util.HashMap GetBatchConfig(BatchConfig batchConfig) {
            java.util.HashMap settings = new java.util.HashMap();
            if (batchConfig != null) {
                settings.put("batch-interval", batchConfig.getBatchInterval());
                settings.put("operation-delay", batchConfig.getOperationDelay());
            }
            return settings;
        }
        private static java.util.HashMap GetReadThru(com.alachisoft.tayzgrid.config.newdom.Readthru readthru)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("enabled", (new Boolean(readthru.getEnabled())).toString());

            if (readthru.getProviders() != null)
            {
                settings.put("read-thru-providers", GetProviders(readthru.getProviders()));
            }

            return settings;
        }

        private static java.util.HashMap GetProviders(com.alachisoft.tayzgrid.config.newdom.Provider[] providers)
        {
            java.util.HashMap settings = new java.util.HashMap();

            if (providers != null && providers.length > 0)
            {
                for (int i = 0; i < providers.length; i++)
                {
                    settings.put(providers[i].getProviderName(), GetProvider(providers[i]));
                }
            }

            return settings;
        }

        private static java.util.HashMap GetProvider(com.alachisoft.tayzgrid.config.newdom.Provider provider)
        {
            java.util.HashMap settings = new java.util.HashMap();

            if (provider != null)
            {
                settings.put("provider-name", provider.getProviderName());
                settings.put("assembly-name", provider.getAssemblyName());
                settings.put("class-name", provider.getClassName());
                settings.put("full-name", provider.getFullProviderName());
                settings.put("default-provider", (new Boolean(provider.getIsDefaultProvider())).toString());
                settings.put("loader-only", provider.getIsLoaderOnly());
                java.util.HashMap paramss = GetParameters(provider.getParameters());
                if (paramss != null)
                {
                    settings.put("parameters", paramss);
                }
            }

            return settings;
        }

        private static java.util.HashMap GetParameters(com.alachisoft.tayzgrid.config.newdom.Parameter[] parameters)
        {
            if (parameters == null)
            {
                return null;
            }

            java.util.HashMap settings = new java.util.HashMap();
            for (int i = 0; i < parameters.length; i++)
            {
                settings.put(parameters[i].getName(), parameters[i].getParamValue());
            }


            return settings;
        }

        private static java.util.HashMap GetTaskConfiguration(com.alachisoft.tayzgrid.config.newdom.TaskConfiguration taskConfig)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("max-tasks", new Integer(taskConfig.getMaxTasks()));
            settings.put("chunk-size", new Integer(taskConfig.getChunkSize()));
            settings.put("communicate-stats", new Boolean(taskConfig.isCommunicateStats()));
            settings.put("queue-size", new Integer(taskConfig.getQueueSize()));
            settings.put("max-avoidable-exceptions", new Integer(taskConfig.getMaxExceptions()));
            return settings;
        }
        private static java.util.HashMap GetCacheClasses(CacheServerConfig cache)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put(cache.getName(), GetClassifiedCache(cache));
            return settings;
        }

        private static java.util.HashMap GetClassifiedCache(CacheServerConfig cache)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("id", cache.getName());
            if (cache.getCluster() == null)
            {
                settings.put("type", "local-cache");
                if(cache.getCacheType().equals("client-cache"))
                {
                  settings.put("isclientcache", true);
                }else{
                    settings.put("isclientcache", false);
                }
                GetInternalCache(settings, cache, true);
            }
            else
            {
                GetCluster(settings, cache);
            }
            if (cache.getNotifications() != null)
            {
                settings.put("notifications", GetNotifications(cache.getNotifications()));
            }
            return settings;
        }

        private static void GetInternalCache(java.util.HashMap source, CacheServerConfig cache, boolean localCache)
        {
            if (cache.getQueryIndices() != null)
            {
                source.put("indexes", GetIndexes(cache.getQueryIndices()));
            }
            if (cache.getStorage() != null)
            {
                source.put("storage", GetStorage(cache.getStorage()));
            }
            if (!localCache)
            {
                source.put("type", "local-cache");
                source.put("id", "internal-cache");
            }
            if (cache.getEvictionPolicy() != null)
            {
                source.put("scavenging-policy", GetEvictionPolicy(cache.getEvictionPolicy()));
            }
            if(cache.getExpirationPolicy()!=null)
            {
                source.put("expiration-policy", GetExpirationPolicy(cache.getExpirationPolicy()));
            }
            
            if (cache.getCleanup() != null)
            {
                source.put("clean-interval", (new Integer(cache.getCleanup().getInterval())).toString());
            }
            if(cache.getTaskConfiguration() != null)
            {
                source.put("tasks-config", GetTaskConfiguration(cache.getTaskConfiguration()));
            }
        }

        private static java.util.HashMap GetEvictionPolicy(com.alachisoft.tayzgrid.config.newdom.EvictionPolicy evictionPolicy)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("class", evictionPolicy.getPolicy());
            settings.put("eviction-enabled", evictionPolicy.getEnabled());
            settings.put("priority", GetEvictionPriority(evictionPolicy));
            settings.put("evict-ratio", evictionPolicy.getEvictionRatio());
            return settings;
        }
        
        private static java.util.HashMap GetExpirationPolicy(com.alachisoft.tayzgrid.config.newdom.ExpirationPolicy expirationPolicy)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("default-policy", expirationPolicy.getPolicyType());
            settings.put("duration", expirationPolicy.getDuration());
            settings.put("unit", expirationPolicy.getUnit());
            return settings;
        }
        
        private static java.util.HashMap GetEvictionPriority(com.alachisoft.tayzgrid.config.newdom.EvictionPolicy evictionPolicy)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("default-value", evictionPolicy.getDefaultPriority());
            return settings;
        }

        private static java.util.HashMap GetStorage(com.alachisoft.tayzgrid.config.newdom.Storage storage)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("class", storage.getType());
            if (storage.getType().equals("heap"))
            {
                settings.put("heap", GetHeap(storage));
            }
            settings.put("heap", GetHeap(storage));
            return settings;
        }
        private static java.util.HashMap GetHeap(com.alachisoft.tayzgrid.config.newdom.Storage storage)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("max-size", (new Long(storage.getSize())).toString());
            return settings;
        }

        private static java.util.HashMap GetIndexes(com.alachisoft.tayzgrid.config.newdom.QueryIndex indexes)
        {
            java.util.HashMap settings = new java.util.HashMap();
            if (indexes.getClasses() != null)
            {
                settings.put("index-classes", GetIndexClasses(indexes.getClasses()));
            }
            return settings;
        }

        private static java.util.HashMap GetIndexClasses(com.alachisoft.tayzgrid.config.newdom.Class[] classes)
        {
            java.util.HashMap settings = new java.util.HashMap();
            for (com.alachisoft.tayzgrid.config.newdom.Class cls : classes)
            {
                settings.put(cls.getID(), GetIndexClass(cls));
            }
            return settings;
        }

        private static java.util.HashMap GetIndexClass(com.alachisoft.tayzgrid.config.newdom.Class cls)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("name", cls.getName());
            settings.put("type", "class");
            settings.put("id", cls.getID());
            if (cls.getAttributes() != null)
            {
                settings.put("attributes", GetIndexAttributes(cls.getAttributes()));
            }
            return settings;
        }

        private static java.util.HashMap GetIndexAttributes(com.alachisoft.tayzgrid.config.newdom.Attrib[] attributes)
        {
            java.util.HashMap settings = new java.util.HashMap();
            for (com.alachisoft.tayzgrid.config.newdom.Attrib attrib : attributes)
            {
                settings.put(attrib.getID(), GetIndexAttribute(attrib));
            }
            return settings;
        }

        private static java.util.HashMap GetIndexAttribute(com.alachisoft.tayzgrid.config.newdom.Attrib attrib)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("name", attrib.getName());
            settings.put("data-type", attrib.getType());
            settings.put("type", "attrib");
            settings.put("id", attrib.getID());
            return settings;
        }

        private static void GetCluster(java.util.HashMap settings, CacheServerConfig cache)
        {
            settings.put("type", cache.getCluster().getTopology());
            settings.put("stats-repl-interval", (new Integer(cache.getCluster().getStatsRepInterval())).toString());
            settings.put("op-timeout", (new Integer(cache.getCluster().getOpTimeout())).toString());
            settings.put("use-heart-beat", (new Boolean(cache.getCluster().getUseHeartbeat())).toString().toLowerCase());

            if (cache.getCleanup() != null)
            {
                settings.put("clean-interval", (new Integer(cache.getCleanup().getInterval())).toString());
            }
            settings.put("internal-cache", new java.util.HashMap());
            GetInternalCache((java.util.HashMap) settings.get("internal-cache"), cache, false);

            java.util.HashMap cluster = new java.util.HashMap();
            cluster.put("group-id", cache.getName());
            cluster.put("class", "tcp");
            cluster.put("channel", GetChannel(cache.getCluster().getChannel(), cache.getCluster().getUseHeartbeat(), cache.getClientPort()));

            settings.put("cluster", cluster);
        }

      

        private static java.util.HashMap GetSQLDependency(com.alachisoft.tayzgrid.config.newdom.SQLDependencyConfig sqlDependencyConfig)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("use-default", (new Boolean(sqlDependencyConfig.getUseDefault())).toString().toLowerCase());
            return settings;
        }

        private static java.util.HashMap GetAlerts(com.alachisoft.tayzgrid.config.newdom.AlertsNotifications alertsNotifications)
        {
            java.util.HashMap settings = new java.util.HashMap();
            if (alertsNotifications.getEMailNotifications() != null)
            {
                settings.put("email-notification", GetEmailNotifications(alertsNotifications.getEMailNotifications()));
            }
            if (alertsNotifications.getAlertsTypes() != null)
            {
                settings.put("alerts-types", GetAlertsType(alertsNotifications.getAlertsTypes()));
            }
            return settings;
        }

        private static java.util.HashMap GetEmailNotifications(com.alachisoft.tayzgrid.config.newdom.EmailNotifications emailNotifications)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("email-notification-enabled", (new Boolean(emailNotifications.getEmailNotificationEnabled())).toString());
            settings.put("sender", emailNotifications.getSender());
            settings.put("smtp-server", emailNotifications.getSmtpServer());
            settings.put("smtp-port", (new Integer(emailNotifications.getSmtpPort())).toString());
            settings.put("ssl", (new Boolean(emailNotifications.getSSL())).toString());
            settings.put("authentication", (new Boolean(emailNotifications.getAuthentication())).toString());
            settings.put("sender-login", emailNotifications.getLogin());
            settings.put("sender-password", emailNotifications.getPassword());

            if (emailNotifications.getRecipients() != null)
            {
                settings.put("recipients", GetRecipients(emailNotifications.getRecipients()));
            }
            return settings;
        }

        private static java.util.HashMap GetRecipients(com.alachisoft.tayzgrid.config.newdom.NotificationRecipient[] recipients)
        {
            java.util.HashMap settings = new java.util.HashMap();

            for (int i = 0; i < recipients.length; i++)
            {
                settings.put(recipients[i].getID(), recipients[i].getID());
            }

            return settings;
        }

        private static java.util.HashMap GetAlertsType(com.alachisoft.tayzgrid.config.newdom.AlertsTypes alertTypes)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("cache-stop", (new Boolean(alertTypes.getCacheStop())).toString());
            settings.put("cache-start", (new Boolean(alertTypes.getCacheStart())).toString());
            settings.put("node-left", (new Boolean(alertTypes.getNodeLeft())).toString());
            settings.put("node-joined", (new Boolean(alertTypes.getNodeJoined())).toString());
            settings.put("state-transfer-started", (new Boolean(alertTypes.getStartTransferStarted())).toString());
            settings.put("state-transfer-stop", (new Boolean(alertTypes.getStartTransferStop())).toString());
            settings.put("state-transfer-error", (new Boolean(alertTypes.getStartTransferError())).toString());
            settings.put("service-start-error", (new Boolean(alertTypes.getServiceStartError())).toString());
            settings.put("cache-size", (new Boolean(alertTypes.getCacheSize())).toString());
            settings.put("general-error", (new Boolean(alertTypes.getGeneralError())).toString());
            settings.put("licensing-error", (new Boolean(alertTypes.getLicensingError())).toString());
            settings.put("configuration-error", (new Boolean(alertTypes.getConfigurationError())).toString());
            settings.put("general-info", (new Boolean(alertTypes.getGeneralInfo())).toString());
            settings.put("unhandled-exceptions", (new Boolean(alertTypes.getUnHandledException())).toString());
            return settings;
        }

        private static java.util.HashMap GetChannel(Channel channel, boolean useHeartBeat, int clientport)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("tcp", GetTcp(channel, useHeartBeat, clientport));
            settings.put("tcpping", GetTcpPing(channel));
            settings.put("pbcast.gms", GetGMS(channel));
            return settings;
        }

        private static java.util.HashMap GetTcpPing(Channel channel)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("initial_hosts", channel.getInitialHosts().toString());
            settings.put("num_initial_members", (new Integer(channel.getNumInitHosts())).toString());
            settings.put("port_range", (new Integer(channel.getPortRange())).toString());
            return settings;
        }

        private static java.util.HashMap GetTcp(Channel channel, boolean useHeartBeat, int clientport)
        {
            java.util.HashMap settings = new java.util.HashMap();
            //settings.put("start_port", (new Integer(channel.getTcpPort())).toString());
            settings.put("start_port", (PortCalculator.getClusterPort(clientport)));
            settings.put("port_range", (new Integer(channel.getPortRange())).toString());
            settings.put("connection_retries", channel.getConnectionRetries());
            settings.put("connection_retry_interval", channel.getConnectionRetryInterval());
            settings.put("use_heart_beat", useHeartBeat);
            return settings;
        }

        private static java.util.HashMap GetGMS(Channel channel)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("join_retry_count", (new Integer(channel.getJoinRetries())).toString());
            settings.put("join_retry_timeout", (new Integer(channel.getJoinRetryInterval())).toString());
            return settings;
        }

        private static java.util.HashMap GetNotifications(com.alachisoft.tayzgrid.config.newdom.Notifications notifications)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("item-add", (new Boolean(notifications.getItemAdd())).toString().toLowerCase());
            settings.put("item-remove", (new Boolean(notifications.getItemRemove())).toString().toLowerCase());
            settings.put("item-update", (new Boolean(notifications.getItemUpdate())).toString().toLowerCase());
            settings.put("cache-clear", (new Boolean(notifications.getCacheClear())).toString().toLowerCase());
            return settings;
        }

        private static java.util.HashMap GetLog(com.alachisoft.tayzgrid.config.newdom.Log log)
        {
            java.util.HashMap settings = new java.util.HashMap();
            settings.put("enabled", (new Boolean(log.getEnabled())).toString().toLowerCase());
            settings.put("trace-errors", (new Boolean(log.getTraceErrors())).toString().toLowerCase());
            settings.put("trace-notices", (new Boolean(log.getTraceNotices())).toString().toLowerCase());
            settings.put("trace-debug", (new Boolean(log.getTraceDebug())).toString().toLowerCase());
            settings.put("trace-warnings", (new Boolean(log.getTraceWarnings())).toString().toLowerCase());
            settings.put("log-path",log.getLocation().toString().toLowerCase());
            return settings;
        }

        public static java.util.HashMap GetConfig(CacheServerConfig cache)
        {
            return GetCacheConfiguration(cache);
        }

        public static java.util.HashMap GetConfig(CacheServerConfig[] caches)
        {
            java.util.HashMap settings = new java.util.HashMap();
            for (CacheServerConfig cache : caches)
            {
                settings.put(cache.getName(), GetCacheConfiguration(cache));
            }
            return settings;
        }
    }
}
