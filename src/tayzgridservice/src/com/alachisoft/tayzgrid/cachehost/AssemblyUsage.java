

package com.alachisoft.tayzgrid.cachehost;


public class AssemblyUsage {
    
    public static void printUsage()
    {
         String usage =  "Usage: tayzgridd \r\n-i --cache-id \r\n Specifies the id/name of cache." + "\r\n" 
                + "\r\n" + "\r\n" + "-f --configfile \r\n Specifies the config file path" + "\r\n"
                + "\r\n" + "\r\n" + "-F --propertyfile \r\n Specifies the property file path" + "\r\n"
                + "\r\n" + "\r\n" + "-p --clientport \r\n Specifies the client port" + "\r\n";
         
         System.out.println(usage);
               
    }
    
    
}
