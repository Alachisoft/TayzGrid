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


import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.ServicePropValues;

/**
 *
 */
public class ChannelConfigBuilder
{

    /**
     *
     *
     * @param properties
     * @return
     */
    public static String BuildTCPConfiguration(java.util.Map properties, long opTimeout)
    {
        StringBuilder b = new StringBuilder(2048);
        b.append(BuildTCP((java.util.Map) ((properties.get("tcp") instanceof java.util.Map) ? properties.get("tcp") : null))).append(":");
        b.append(BuildTCPPING((java.util.Map) ((properties.get("tcpping") instanceof java.util.Map) ? properties.get("tcpping") : null))).append(":");
        b.append(BuildQueue((java.util.Map) ((properties.get("queue") instanceof java.util.Map) ? properties.get("queue") : null))).append(":");
        b.append(Buildpbcast_GMS((java.util.Map) ((properties.get("pbcast.gms") instanceof java.util.Map) ? properties.get("pbcast.gms") : null), false)).append(":");
        b.append(BuildTOTAL((java.util.Map) ((properties.get("total") instanceof java.util.Map) ? properties.get("total") : null), opTimeout)).append(":");
        b.append(BuildVIEW_ENFORCER((java.util.Map) ((properties.get("view-enforcer") instanceof java.util.Map) ? properties.get("view-enforcer") : null)));
        return b.toString();
    }

    public static String BuildTCPConfiguration(java.util.Map properties, String userId, String password, long opTimeout, boolean isReplica)
    {
        StringBuilder b = new StringBuilder(2048);
        b.append(BuildTCP((java.util.Map) ((properties.get("tcp") instanceof java.util.Map) ? properties.get("tcp") : null))).append(":");
        b.append(BuildTCPPING((java.util.Map) ((properties.get("tcpping") instanceof java.util.Map) ? properties.get("tcpping") : null), userId, password)).append(":");
        b.append(BuildQueue((java.util.Map) ((properties.get("queue") instanceof java.util.Map) ? properties.get("queue") : null))).append(":");
        b.append(Buildpbcast_GMS((java.util.Map) ((properties.get("pbcast.gms") instanceof java.util.Map) ? properties.get("pbcast.gms") : null), isReplica)).append(":");
        b.append(BuildTOTAL((java.util.Map) ((properties.get("total") instanceof java.util.Map) ? properties.get("total") : null), opTimeout)).append(":");
        b.append(BuildVIEW_ENFORCER((java.util.Map) ((properties.get("view-enforcer") instanceof java.util.Map) ? properties.get("view-enforcer") : null)));

        return b.toString();
    }

	   /**
     *
     *
     * @param properties
     * @return
     */
    public static String BuildUDPConfiguration(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(2048);
		b.append(BuildUDP((java.util.Map)((properties.get("udp") instanceof java.util.Map) ? properties.get("udp") : null))).append(":");
		b.append(BuildPING((java.util.Map)((properties.get("ping") instanceof java.util.Map) ? properties.get("ping") : null))).append(":");
		b.append(BuildMERGEFAST((java.util.Map)((properties.get("mergefast") instanceof java.util.Map) ? properties.get("mergefast") : null))).append(":");
		b.append(BuildFD_SOCK((java.util.Map)((properties.get("fd-sock") instanceof java.util.Map) ? properties.get("fd-sock") : null))).append(":");
		b.append(BuildVERIFY_SUSPECT((java.util.Map)((properties.get("verify-suspect") instanceof java.util.Map) ? properties.get("verify-suspect") : null))).append(":");
		b.append(BuildFRAG((java.util.Map)((properties.get("frag") instanceof java.util.Map) ? properties.get("frag") : null))).append(":");
		b.append(BuildUNICAST((java.util.Map)((properties.get("unicast") instanceof java.util.Map) ? properties.get("unicast") : null))).append(":");
		b.append(BuildQueue((java.util.Map)((properties.get("queue") instanceof java.util.Map) ? properties.get("queue") : null))).append(":");
		b.append(Buildpbcast_NAKACK((java.util.Map)((properties.get("pbcast.nakack") instanceof java.util.Map) ? properties.get("pbcast.nakack") : null))).append(":");
		b.append(Buildpbcast_STABLE((java.util.Map)((properties.get("pbcast.stable") instanceof java.util.Map) ? properties.get("pbcast.stable") : null))).append(":");
		b.append(Buildpbcast_GMS((java.util.Map)((properties.get("pbcast.gms") instanceof java.util.Map) ? properties.get("pbcast.gms") : null), false)).append(":");
		b.append(BuildTOTAL((java.util.Map)((properties.get("total") instanceof java.util.Map) ? properties.get("total") : null), 5000)).append(":");
		b.append(BuildVIEW_ENFORCER((java.util.Map)((properties.get("view-enforcer") instanceof java.util.Map) ? properties.get("view-enforcer") : null)));
        return b.toString();
    }

    private static String BuildUDP(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(256);
        b.append("UDP(").append(ConfigHelper.SafeGetPair(properties, "mcast_addr", "239.0.1.10")).append(ConfigHelper.SafeGetPair(properties, "mcast_port", 10001)).append(ConfigHelper.SafeGetPair(properties, "bind_addr", null)).append(ConfigHelper.SafeGetPair(properties, "bind_port", null)).append(ConfigHelper.SafeGetPair(properties, "port_range", 256)).append(ConfigHelper.SafeGetPair(properties, "ip_mcast", null)).append(ConfigHelper.SafeGetPair(properties, "mcast_send_buf_size", null)).append(ConfigHelper.SafeGetPair(properties, "mcast_recv_buf_size", null)).append(ConfigHelper.SafeGetPair(properties, "ucast_send_buf_size", null)).append(ConfigHelper.SafeGetPair(properties, "ucast_recv_buf_size", null)).append(ConfigHelper.SafeGetPair(properties, "max_bundle_size", null)).append(ConfigHelper.SafeGetPair(properties, "max_bundle_timeout", null)).append(ConfigHelper.SafeGetPair(properties, "enable_bundling", null)).append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(ConfigHelper.SafeGetPair(properties, "use_incoming_packet_handler", null)).append(ConfigHelper.SafeGetPair(properties, "use_outgoing_packet_handler", null)).append("ip_ttl=32;").append(")"); //"false" - 20 - 32000 - 64000 - 32000 - 64000 - 32000 - "false"
        return b.toString();
    }

    private static String BuildPING(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(256);
        b.append("PING(").append(ConfigHelper.SafeGetPair(properties, "timeout", null)).append(ConfigHelper.SafeGetPair(properties, "num_initial_members", null)).append(ConfigHelper.SafeGetPair(properties, "port_range", null)).append(ConfigHelper.SafeGetPair(properties, "initial_hosts", null)).append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(")"); //1 - 2 - 2000
        return b.toString();
    }

    private static String BuildMERGEFAST(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(16);
        b.append("MERGEFAST(").append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(")");
        return b.toString();
    }

    private static String BuildFD_SOCK(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(64);
        b.append("FD_SOCK(").append(ConfigHelper.SafeGetPair(properties, "get_cache_timeout", null)).append(ConfigHelper.SafeGetPair(properties, "start_port", null)).append(ConfigHelper.SafeGetPair(properties, "num_tries", null)).append(ConfigHelper.SafeGetPair(properties, "suspect_msg_interval", null)).append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(")"); //5000 - 3 - 49152 - 3000
        return b.toString();
    }

    private static String BuildVERIFY_SUSPECT(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(32);
        b.append("VERIFY_SUSPECT(").append(ConfigHelper.SafeGetPair(properties, "timeout", 1500)).append(ConfigHelper.SafeGetPair(properties, "num_msgs", null)).append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(")"); // null
        return b.toString();
    }

    private static String BuildQueue(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(32);
        b.append("QUEUE(").append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(")");
        return b.toString();
    }

    private static String Buildpbcast_NAKACK(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(128);
        b.append("pbcast.NAKACK(").append(ConfigHelper.SafeGetPair(properties, "retransmit_timeout", null)).append(ConfigHelper.SafeGetPair(properties, "gc_lag", 40)).append(ConfigHelper.SafeGetPair(properties, "max_xmit_size", null)).append(ConfigHelper.SafeGetPair(properties, "use_mcast_xmit", null)).append(ConfigHelper.SafeGetPair(properties, "discard_delivered_msgs", null)).append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(")"); // "true" -  "false" -  8192 - "600,1200,2400,4800"
        return b.toString();
    }

    private static String BuildUNICAST(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(64);
        b.append("UNICAST(").append(ConfigHelper.SafeGetPair(properties, "timeout", null)).append(ConfigHelper.SafeGetPair(properties, "window_size", null)).append(ConfigHelper.SafeGetPair(properties, "min_threshold", null)).append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(")"); // -1 -  -1 -  "800,1600,3200,6400"
        return b.toString();
    }

    private static String Buildpbcast_STABLE(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(256);
        b.append("pbcast.STABLE(").append(ConfigHelper.SafeGetPair(properties, "digest_timeout", null)).append(ConfigHelper.SafeGetPair(properties, "desired_avg_gossip", null)).append(ConfigHelper.SafeGetPair(properties, "stability_delay", null)).append(ConfigHelper.SafeGetPair(properties, "max_gossip_runs", null)).append(ConfigHelper.SafeGetPair(properties, "max_bytes", null)).append(ConfigHelper.SafeGetPair(properties, "max_suspend_time", null)).append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(")"); // 600000 -  0 -  3 -  6000 -  20000 -  60000
        return b.toString();
    }

    private static String BuildFRAG(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(32);
        b.append("FRAG(").append(ConfigHelper.SafeGetPair(properties, "frag_size", null)).append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(")");
        return b.toString();
    }

    private static String Buildpbcast_GMS(java.util.Map properties, boolean isReplica)
    {
        StringBuilder b = new StringBuilder(256);
        if (isReplica)
        {
            if (properties == null)
            {
                properties = new java.util.HashMap();
            }
            properties.put("is_part_replica", "true");
        }
        b.append("pbcast.GMS(").append(ConfigHelper.SafeGetPair(properties, "shun", null)).append(ConfigHelper.SafeGetPair(properties, "join_timeout", null)).append(ConfigHelper.SafeGetPair(properties, "join_retry_timeout", null)).append(ConfigHelper.SafeGetPair(properties, "join_retry_count", null)).append(ConfigHelper.SafeGetPair(properties, "leave_timeout", null)).append(ConfigHelper.SafeGetPair(properties, "merge_timeout", null)).append(ConfigHelper.SafeGetPair(properties, "digest_timeout", null)).append(ConfigHelper.SafeGetPair(properties, "disable_initial_coord", null)).append(ConfigHelper.SafeGetPair(properties, "num_prev_mbrs", null)).append(ConfigHelper.SafeGetPair(properties, "print_local_addr", null)).append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(ConfigHelper.SafeGetPair(properties, "is_part_replica", null)).append(")"); // false -  50 -  false -  5000 -  10000 -  5000 -  3 -  2000 -  5000 -  "true"
        return b.toString();
    }

    private static String BuildTOTAL(java.util.Map properties, long opTimeout)
    {
        StringBuilder b = new StringBuilder(8);
        b.append("TOTAL(").append(ConfigHelper.SafeGetPair(properties, "timeout", null)).append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(ConfigHelper.SafeGetPair(properties, "op_timeout", opTimeout)).append(")"); //"600,1200,2400,4800"
        return b.toString();
    }

    private static String BuildVIEW_ENFORCER(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(16);
        b.append("VIEW_ENFORCER(").append(ConfigHelper.SafeGetPair(properties, "down_thread", null)).append(ConfigHelper.SafeGetPair(properties, "up_thread", null)).append(")");
        return b.toString();
    }

    private static String BuildTCPPING(java.util.Map properties)
    {
        StringBuilder b = new StringBuilder(256);
       
		b.append("TCPPING(").append(")")
                        .append(ConfigHelper.SafeGetPair(properties,"timeout", null))				//3000
                        .append(ConfigHelper.SafeGetPair(properties,"port_range", null))			//5
                        .append(ConfigHelper.SafeGetPair(properties,"static", null))				//false
                        .append(ConfigHelper.SafeGetPair(properties,"num_initial_members", null))	//2
                        .append(ConfigHelper.SafeGetPair(properties,"initial_hosts", null))
                        .append(ConfigHelper.SafeGetPair(properties,"discovery_addr", "228.8.8.8"))		//228.8.8.8
                        .append(ConfigHelper.SafeGetPair(properties,"discovery_port", 7700))			//7700
                        .append(ConfigHelper.SafeGetPair(properties,"down_thread", null))
                        .append(ConfigHelper.SafeGetPair(properties,"up_thread", null))
                        .append(")");
        return b.toString();
    }

    private static String BuildTCPPING(java.util.Map properties, String userId, String password)
    {
        StringBuilder b = new StringBuilder(256);
        
        b.append("TCPPING(")
                        .append(ConfigHelper.SafeGetPair(properties, "timeout", null))
                        .append(ConfigHelper.SafeGetPair(properties, "port_range", null))
                        .append(ConfigHelper.SafeGetPair(properties, "static", null))				//false
                        .append(ConfigHelper.SafeGetPair(properties, "num_initial_members", null))	//2
                        .append(ConfigHelper.SafeGetPair(properties, "initial_hosts", null))
                        .append(ConfigHelper.SafeGetPair(properties, "discovery_addr", "228.8.8.8"))		//228.8.8.8
                        .append(ConfigHelper.SafeGetPair(properties, "discovery_port", 7700))			//7700
                        .append(ConfigHelper.SafeGetPair(properties, "down_thread", null))
                        .append(ConfigHelper.SafeGetPair(properties, "up_thread", null))
                        .append(ConfigHelper.SafeGetPair(properties, "is_por", null))
                        .append(ConfigHelper.SafeGetPair(properties, "start_port", null))
                        .append(ConfigHelper.SafeGetPair(properties, "user-id", userId))
                        .append(ConfigHelper.SafeGetPair(properties, "password", password))
                        .append(")");
		      return b.toString();
    }

    private static String BuildTCP(java.util.Map properties)
    {
        String bindIP = ServicePropValues.CacheServer_BindToIP;
       

        StringBuilder b = new StringBuilder(256);
        b.append("TCP(")
                .append(ConfigHelper.SafeGetPair(properties, "connection_retries", 0))
                .append(ConfigHelper.SafeGetPair(properties, "connection_retry_interval", 0))
                .append(ConfigHelper.SafeGetPair(properties, "bind_addr", bindIP))
                .append(ConfigHelper.SafeGetPair(properties, "start_port", null))
                .append(ConfigHelper.SafeGetPair(properties, "port_range", null))
                .append(ConfigHelper.SafeGetPair(properties, "send_buf_size", null))				//32000
                .append(ConfigHelper.SafeGetPair(properties, "recv_buf_size", null))				//64000
                .append(ConfigHelper.SafeGetPair(properties, "reaper_interval", null))			//0
                .append(ConfigHelper.SafeGetPair(properties, "conn_expire_time", null))			//0
                .append(ConfigHelper.SafeGetPair(properties, "skip_suspected_members", null))	//true
                .append(ConfigHelper.SafeGetPair(properties, "down_thread", true))
                .append(ConfigHelper.SafeGetPair(properties, "up_thread", true))
                .append(ConfigHelper.SafeGetPair(properties, "use_heart_beat", true))
                .append(ConfigHelper.SafeGetPair(properties, "heart_beat_interval", null))
                .append(ConfigHelper.SafeGetPair(properties, "is_inproc", null))
                .append(")");
        
        return b.toString();
    }
}
