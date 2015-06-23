
package com.alachisoft.tayzgrid.common;


import static com.alachisoft.tayzgrid.common.ServicePropValues.getMapping_Properties;
import static com.alachisoft.tayzgrid.common.ServicePropValues.getTGHome;
import com.alachisoft.tayzgrid.runtime.util.RuntimeUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author 
 */


public class EnvPropValue {
    
    public static String DEBUG_PORT_OFFSET="";
    public static String JMX_PORT_OFFSET="";

    public static String TGHome = null;
    public static String JAVAHome = null;
    private static String Mapping_Properties;
    public static ArrayList<String> param = new ArrayList<String>();;
    private static HashMap<String,String> propertiesHashmap = new HashMap<String, String>();
    
    private static String getMapping_Properties()
    {
        if (Mapping_Properties == null) {
            RuntimeUtil.OS currentOS = RuntimeUtil.getCurrentOS();
            File file;
            if (currentOS == RuntimeUtil.OS.Linux) {
                file = new File("./env.properties");
            } else {
                file = new File(".\\env.properties");

            }
            if (file.exists()) {
                Mapping_Properties = file.getPath();
            } else {
                Mapping_Properties = Common.combinePath(getTGHome(), "config", "env.properties");
            }
        }
        return Mapping_Properties;
    }
     
    public static boolean loadEnvPropFromTGHOME(){
//         if (getTGHome() != null && !TGHome.trim().isEmpty()) {
//            if (!TGHome.endsWith("/") && !TGHome.endsWith("\\")) {
//                TGHome = TGHome.concat("/");
//            }
            
            String filePath = getMapping_Properties();
            //filePath = filePath.concat("config/env.properties");
            Properties props = new Properties();

            try {
                props.load(new FileInputStream(filePath));
            } catch (IOException iOException) {
            }
            
            Enumeration enu = props.keys();
            if(enu != null && enu.hasMoreElements()){
                while (enu.hasMoreElements()) {
                    String key = (String) enu.nextElement();
                    propertiesHashmap.put(key, props.getProperty(key).trim());               
                    //System.setProperty(key, props.getProperty(key).trim());
                }
                EnvPropValue.initialize();
                return true;
            }
            

        //}
        return false;
    }
    
    public static void initialize() {
                  
        JAVAHome = propertiesHashmap.get("JAVA_HOME");
        String value = propertiesHashmap.get("TG_VMARGS");
        
        if(value != null){
            
            String []properties = value.split(" ");            
            if(properties.length > 0)
                extractProperties(properties);
        }
            
    }
    
    public static void extractProperties(String[] properties){
        
        param.clear();
        for(String str:properties){
            
            str = str.replaceAll("\\s+","");
            str = str.replace("$", "");
            str = str.replace("\"", "");
            String value =  propertiesHashmap.get(str);
           
            if(value != null){
                
                if(value.contains("-Xrunjdwp")){               
                    DEBUG_PORT_OFFSET = value.replaceFirst(".*?(\\d+).*", "$1");

                    DEBUG_PORT_OFFSET = DEBUG_PORT_OFFSET.matches("-?\\d+") ? DEBUG_PORT_OFFSET : "4000";
                }

                else if(value.contains("sun.management.jmxremote.port")){
                    String [] ports = value.split("=");

                    if(ports.length >= 2)
                       JMX_PORT_OFFSET = ports[1].matches("-?\\d+") ?  ports[1] :"1501";
                }            

                else{
                       param.add(value);
                }
            }
            
            
        }
    }
    
    
    public static String getTGHome() {
        if (TGHome == null) {
            TGHome = Common.getTGHome();
        }
        return TGHome;
    }
    
    
}
