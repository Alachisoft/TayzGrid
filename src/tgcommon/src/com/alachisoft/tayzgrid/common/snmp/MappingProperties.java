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
package com.alachisoft.tayzgrid.common.snmp;

public class MappingProperties
{

    private final static String[] Oids =
    {
        /*
         * These Oids are redundant such as NodeName is mapped to "1.3.6.1.4.1.12.1.0.0" and "1.3.6.1.4.1.12.1.1.0"
         *              Such redundancies should be removed in future.
         */
        //<editor-fold defaultstate="collapsed" desc="Parent Oids">
        "1.3.6.1.4.1.12 = <oid>",
        "1.3.6.1.4.1.12.1 = tayzgrid",
        "1.3.6.1.4.1.12.1.0 = server",
        "1.3.6.1.4.1.12.1.1 = cache",
        "1.3.6.1.4.1.12.1.2 = client",
       
        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="Server Oids">
        "1.3.6.1.4.1.12.1.0.0 = Node Name",
        "1.3.6.1.4.1.12.1.0.1 = Logged Event",
        "1.3.6.1.4.1.12.1.0.7 = Cache Ports",
        "1.3.6.1.4.1.12.1.0.8 = Client Port",
        "1.3.6.1.4.1.12.1.0.37 = Bytes sent/sec",
        "1.3.6.1.4.1.12.1.0.38 = Bytes received/sec",
        "1.3.6.1.4.1.12.1.0.39 = Event Queue Count",
        "1.3.6.1.4.1.12.1.0.50 = Requests/sec",
        "1.3.6.1.4.1.12.1.0.51 = Responses/sec",
        "1.3.6.1.4.1.12.1.0.52 = Client Bytes Sent/sec",
        "1.3.6.1.4.1.12.1.0.53 = Client Bytes Received/sec",
        "1.3.6.1.4.1.12.1.0.54 = Average µs/cache operations",
//        "1.3.6.1.4.1.12.1.0.55 = mSecPerOperationBase",
        "1.3.6.1.4.1.12.1.0.56 = Total CPU Usage",
        "1.3.6.1.4.1.12.1.0.57 = Total Free Physical Memory",
        "1.3.6.1.4.1.12.1.0.58 = Total Memory Usage",
        "1.3.6.1.4.1.12.1.0.59 = TayzGrid CPU Usage",
        "1.3.6.1.4.1.12.1.0.60 = TayzGrid Available Memory",
        "1.3.6.1.4.1.12.1.0.61 = TayzGrid Max Memory",
        "1.3.6.1.4.1.12.1.0.62 = TayzGrid Memory Usage",
        "1.3.6.1.4.1.12.1.0.63 = TayzGrid Network Usage",
        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="Cache Oids">
        "1.3.6.1.4.1.12.1.1.0 = Node Name",
        "1.3.6.1.4.1.12.1.1.1 = Count",
        "1.3.6.1.4.1.12.1.1.2 = CacheLastAccessCount",
        "1.3.6.1.4.1.12.1.1.3 = Additions/sec",
        "1.3.6.1.4.1.12.1.1.4 = Hits/sec",
        "1.3.6.1.4.1.12.1.1.5 = Updates/sec",
        "1.3.6.1.4.1.12.1.1.6 = Misses/sec",
        "1.3.6.1.4.1.12.1.1.7 = Fetches/sec",
        "1.3.6.1.4.1.12.1.1.8 = Deletes/sec",
        "1.3.6.1.4.1.12.1.1.9 = Average µs/add",
        "1.3.6.1.4.1.12.1.1.10 = Average µs/insert",
        "1.3.6.1.4.1.12.1.1.11 = Avearge µs/fetch",
        "1.3.6.1.4.1.12.1.1.12 = Average µs/remove",
        "1.3.6.1.4.1.12.1.1.13 = Hit ratio/sec (%)",
        "1.3.6.1.4.1.12.1.1.14 = Expirations/sec",
        "1.3.6.1.4.1.12.1.1.15 = Evictions/sec",
        "1.3.6.1.4.1.12.1.1.16 = State transfer/sec",
        "1.3.6.1.4.1.12.1.1.17 = Data balance/sec",
//        "1.3.6.1.4.1.12.1.1.18 = mSecPerAddBase",
//        "1.3.6.1.4.1.12.1.1.19 = mSecPerInsertBase",
//        "1.3.6.1.4.1.12.1.1.20 = mSecPerGetBase",
//        "1.3.6.1.4.1.12.1.1.21 = mSecPerDelBase",
//        "1.3.6.1.4.1.12.1.1.22 = Hit ratio/secbase(%)",
        "1.3.6.1.4.1.12.1.1.23 = Mirror queue size",
        "1.3.6.1.4.1.12.1.1.24 = Readthru/sec",
        "1.3.6.1.4.1.12.1.1.25 = Writethru/sec",
        "1.3.6.1.4.1.12.1.1.26 = Cache Size",
        "1.3.6.1.4.1.12.1.1.27 = Cluster ops/sec",
        
        "1.3.6.1.4.1.12.1.1.28 = Write-behind queue count",
        "1.3.6.1.4.1.12.1.1.29 = Write-behind/sec",
        "1.3.6.1.4.1.12.1.1.30 = Average µs/datasource write",
        "1.3.6.1.4.1.12.1.1.31 = Write-behind failure retry count", 
        "1.3.6.1.4.1.12.1.1.32 = Write-behind evictions/sec",
        "1.3.6.1.4.1.12.1.1.33 = Datasource updates/sec",
        "1.3.6.1.4.1.12.1.1.34 = Average µs/datasource update",
        "1.3.6.1.4.1.12.1.1.35 = Datasource failed operations/sec",
        "1.3.6.1.4.1.12.1.1.36 = Current batch operations count",
        
        
        
        
        "1.3.6.1.4.1.12.1.1.50 = Requests/sec",
        "1.3.6.1.4.1.12.1.1.51 = Responses/sec",
        "1.3.6.1.4.1.12.1.1.52 = Client Bytes Sent/sec",
        "1.3.6.1.4.1.12.1.1.53 = Client Bytes Received/sec",
        "1.3.6.1.4.1.12.1.1.54 = Average µs/cache operations",
//        "1.3.6.1.4.1.12.1.1.55 = mSecPerOperationBase",
        "1.3.6.1.4.1.12.1.1.56 = Total CPU Usage",
        "1.3.6.1.4.1.12.1.1.57 = Total Free Physical Memory",
        "1.3.6.1.4.1.12.1.1.58 = Total Memory Usage",
        "1.3.6.1.4.1.12.1.1.59 = TayzGrid CPU Usage",
        "1.3.6.1.4.1.12.1.1.60 = TayzGrid Available Memory",
        "1.3.6.1.4.1.12.1.1.61 = TayzGrid Max Memory",
        "1.3.6.1.4.1.12.1.1.62 = TayzGrid Memory Usage",
        "1.3.6.1.4.1.12.1.1.63 = TayzGrid Network Usage",
        "1.3.6.1.4.1.12.1.1.64 = Query Index Size",
        "1.3.6.1.4.1.12.1.1.65 = Expiration Index Size",
        "1.3.6.1.4.1.12.1.1.66 = Eviction Index Size",
        "1.3.6.1.4.1.12.1.1.67 = Queries/sec",
        "1.3.6.1.4.1.12.1.1.68 = Average µs/Query Execution",
        "1.3.6.1.4.1.12.1.1.69 = Average Query Size",
        "1.3.6.1.4.1.12.1.1.70 = Cache Max Size",
        "1.3.6.1.4.1.12.1.1.71 = Client Connected",
        "1.3.6.1.4.1.12.1.1.72 = Mirror Started",
        "1.3.6.1.4.1.12.1.1.73 = INProc Cache",
        "1.3.6.1.4.1.12.1.1.74 = Node Name",
        "1.3.6.1.4.1.12.1.1.75 = Node Status",
	"1.3.6.1.4.1.12.1.1.76 = Running Cache Servers",
        "1.3.6.1.4.1.12.1.1.77 = Cache Servers",
        "1.3.6.1.4.1.12.1.1.78 = M/R pending tasks",
        "1.3.6.1.4.1.12.1.1.79 = M/R running tasks",
        "1.3.6.1.4.1.12.1.1.80 = M/R no. of records mapped/sec",
        "1.3.6.1.4.1.12.1.1.81 = M/R no. of records combined/sec",
        "1.3.6.1.4.1.12.1.1.82 = M/R no. of records reduced/sec",
        "1.3.6.1.4.1.12.1.1.83 = PID",
        "1.3.6.1.4.1.12.1.1.84 = LocalAddress",
        
        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="Client Oids">
        "1.3.6.1.4.1.12.1.2.0 = Client CPU Usage",
        "1.3.6.1.4.1.12.1.2.1 = Memory Usage", 
        "1.3.6.1.4.1.12.1.2.2 = Network Usage",
        "1.3.6.1.4.1.12.1.2.3 = Requests/sec",
        "1.3.6.1.4.1.12.1.2.4 = Additions/sec",
        "1.3.6.1.4.1.12.1.2.5 = Fetches/sec",
        "1.3.6.1.4.1.12.1.2.6 = Updates/sec",
        "1.3.6.1.4.1.12.1.2.7 = Deletes/sec",
        "1.3.6.1.4.1.12.1.2.8 = Read Operations/sec",
        "1.3.6.1.4.1.12.1.2.9 = Write Operations/sec",
        "1.3.6.1.4.1.12.1.2.10 = Request queue size",
        "1.3.6.1.4.1.12.1.2.11 = Average Item Size",
        "1.3.6.1.4.1.12.1.2.12 = Average µs/Event",
        "1.3.6.1.4.1.12.1.2.14 = Average µs/serialization",
        "1.3.6.1.4.1.12.1.2.16 = Event Processed/sec",
        "1.3.6.1.4.1.12.1.2.17 = Event Triggered/sec",
        "1.3.6.1.4.1.12.1.2.18 = Average µs/encryption",
        "1.3.6.1.4.1.12.1.2.19 = Average µs/decryption",
        
        //</editor-fold>

      
    
    };

    public static String[] getOids()
    {
        return Oids;
    }
}
