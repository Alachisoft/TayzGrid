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

import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;

public class ConfigurationManager {
    private static ConfigurationManager _singleton;
    
    private ApplicationConfiguration _appConfig=null;
    private RegionConfigurationManager _regionConfigManager=null;
    
    private ConfigurationManager() throws ConfigurationException, FileNotFoundException, Exception
    {
       String appID=System.getProperty("tayzgrid.application_id");
       if(appID==null || appID.isEmpty())
           throw new ConfigurationException("tayzgrid.application-id not specified in System proprties.");
       
       String configFilePath=this.GetFilePah("TayzGridHibernate.xml");
       ConfigurationBuilder configBuilder=new ConfigurationBuilder(configFilePath);
       configBuilder.RegisterRootConfigurationObject(ApplicationConfiguration.class);
       configBuilder.ReadConfiguration();
       
       Object [] configuraion =configBuilder.getConfiguration();   
       boolean appConfigFound=false;
       if(configuraion!=null && configuraion.length>0)
       {
           for(int i=0; i<configuraion.length; i++)
           {
               _appConfig=(ApplicationConfiguration)configuraion[i];
               if(_appConfig!=null)
               {
                   if(_appConfig.getApplicationID()!= null && _appConfig.getApplicationID().equalsIgnoreCase(appID))
                   {
                       appConfigFound=true;
                       break;
                   }
               }
           }
       }
       
       if(!appConfigFound)
           throw new ConfigurationException("Invalid value of tayzgrid.application_id. Applicaion configuration not found for application-id = " + appID);
       if(_appConfig.getDefaultRegion()==null || _appConfig.getDefaultRegion().isEmpty())
           throw new ConfigurationException("default-region cannot be null for application-id = " + _appConfig.getApplicationID());
       
       _regionConfigManager=new RegionConfigurationManager(_appConfig.getCacheRegions());
       if(!_regionConfigManager.contains(_appConfig.getDefaultRegion()))
           throw new ConfigurationException("Region's configuration not specified for default-region : "+_appConfig.getDefaultRegion());
       
    }
    
    private String GetFilePah(String fileName) throws FileNotFoundException
    {
        String filePath="";
        if(new File(fileName).exists())
            filePath=fileName;
        else if(new File(".\\bin\\" + fileName).exists())
            filePath = ".\\bin\\" + fileName;
        else if (new File(System.getenv("TG_HOME") + "\\config\\" + fileName).exists())
            filePath = System.getenv("TG_HOME") + "\\config\\" + fileName;
        else
                throw new FileNotFoundException(fileName +" file not found.");
        return filePath;     
    }
    
    public static ConfigurationManager getInstance() throws ConfigurationException, FileNotFoundException, Exception
    {
        if(_singleton==null)
            _singleton=new ConfigurationManager();
        return _singleton;
    }
    
    public RegionConfiguraton getRegionConfiguration(String regionName)
    {
        RegionConfiguraton rConfig=_regionConfigManager.getRegionConfig(regionName);
        if(rConfig==null)
            rConfig=_regionConfigManager.getRegionConfig(_appConfig.getDefaultRegion());
        return rConfig;
    }
    
    public String getCacheKey(Object key)
    {
        String cacheKey="HibernateTayzGrid:"+key.toString();

        if(!_appConfig.getKeyCaseSensitivity())
            cacheKey=cacheKey.toLowerCase();
        return cacheKey;
    }
    public boolean isExceptionEnabled()
    {
        return _appConfig.getCacheExceptionEnabled();
    }
    
    private String pad(String str, int size, String padChar)
    {
        StringBuffer padded = new StringBuffer(str);
        while (padded.length() < (size + str.length()))
        {
            padded.append(padChar);
        }
        return padded.toString();
    }
}
