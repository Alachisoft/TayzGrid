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

import com.alachisoft.tayzgrid.config.dom.CacheServerConfig;
import com.alachisoft.tayzgrid.config.dom.ConfigConverter;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlConfigReader extends ConfigReader
{
    private String _cacheId = "";
    private String _configFileName;
    private String _configSection;

    private static final char[] _trimChars = new char[] { ' ', '\t', '\r', '\n', '\'', '"' };

    public XmlConfigReader(String configFileName, String configSection)
    {
        _configFileName = configFileName;
        _configSection = configSection;
    }

    public final String getConfigFileName()
    {
        return _configFileName;
    }

    public final String getConfigSection()
    {
        return _configSection;
    }
    
    @Override
    public java.util.HashMap getProperties()
    {
        try
        {
            return GetProperties(_configFileName, _configSection);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public final java.util.ArrayList getPropertiesList() throws ConfigurationException
    {
        return GetAllProperties(_configFileName, _configSection);
    }
    
    public final String ToPropertiesString()
    {
        return ConfigReader.ToPropertiesString(getProperties());
    }


    protected final java.util.Hashtable GetAttributesOfNode(Node navigator)
    {
        java.util.Hashtable attributes = new java.util.Hashtable();
        for (int i = 0; i< navigator.getAttributes().getLength(); i++){
            attributes.put(navigator.getAttributes().item(i).getNodeName(), navigator.getAttributes().item(i).getNodeValue());
        }
        return attributes;
    }

    protected java.util.Hashtable BuildHashtable(java.util.Hashtable properties, Node navigator)
    {
         String name = navigator.getNodeName().toLowerCase();
        NodeList childNavigator = navigator.getChildNodes();
        if (!(childNavigator.getLength() > 0)) {
            if (navigator.getNodeType() == Node.ELEMENT_NODE) {
                properties.put(name, navigator.getNodeValue().trim());
            }
        }

        for (int i = 0; i < childNavigator.getLength(); i++) {
            java.util.Hashtable attributes = GetAttributesOfNode(childNavigator.item(i));

            java.util.Hashtable subproperties = BuildHashtable(new java.util.Hashtable(), childNavigator.item(0));
            if (attributes.containsKey("id")) {
                String id = attributes.get("id").toString();
                subproperties.put("id", id);
                subproperties.put("type", name);
                name = id.toLowerCase();
            }
            properties.put(name, subproperties);
        }
        return properties;
    }

    protected java.util.Hashtable BuildHashtable2(java.util.Hashtable properties, Node navigator)
    {

        int count = 0;
        do
        {
            String name = navigator.getNodeName().toLowerCase();
            NodeList  childNavigator = navigator.getChildNodes();
            java.util.Hashtable attributes = GetAttributesOfNode(childNavigator.item(0));//childNavigator.Current

            java.util.Hashtable subprops = new java.util.Hashtable();
            Iterator attribEnum = attributes.entrySet().iterator();
            while (attribEnum.hasNext())
            {
                Map.Entry pair = (Map.Entry)attribEnum.next();
                subprops.put(pair.getKey(), pair.getValue());
            }

            if (attributes.containsKey("id"))
            {
                String id = attributes.get("id").toString();
                subprops.put("type", name);

                if ( ! id.equals("internal-cache"))
                {
                    _cacheId = id;
                }
                name = id.toLowerCase();
            }


            if (childNavigator.getLength() > count)
            {
                count ++;
                java.util.Hashtable subproperties = BuildHashtable2(subprops, childNavigator.item(count));
                if (name.toLowerCase().equals("cache"))
                {
                    subproperties.put("class", _cacheId);
                    subproperties.put("name", _cacheId);
                }
                if ( ! name.equals(""))
                {
                    properties.put(name, subproperties);
                }
            }
            else
            {
                if ( ! name.equals(""))
                {
                    properties.put(name, subprops);
                }
            }
        }
        while (navigator.hasAttributes());
        navigator = navigator.getParentNode();
        return properties;
    }

    public final java.util.HashMap GetProperties(String fileName, String configSection) throws ConfigurationException
    {
        java.util.HashMap properties = new java.util.HashMap();
        try
        {
            tangible.RefObject<java.util.HashMap> tempRef_properties = new tangible.RefObject<java.util.HashMap>(properties);
            LoadConfig(fileName, tempRef_properties);
            properties = tempRef_properties.argvalue;
            return (java.util.HashMap)properties.get(configSection);
        }
        catch(Exception e)
        {
            throw new ConfigurationException("Specified config section '" + configSection + "' not found in file '" + fileName + "'. If it is a cache, it must be registered properly on this machine. --- " +  e.toString());
        }
    }

    private static com.alachisoft.tayzgrid.config.dom.CacheServerConfig[] LoadConfig(String fileName) throws ConfigurationException, Exception
    {
        ConfigurationBuilder builder = new ConfigurationBuilder(fileName);
        try
        {
            builder.RegisterRootConfigurationObject(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig.class);
        }
        catch (Exception exception)
        {
            throw new ConfigurationException(exception.getMessage(), exception);
        }


        try
        {
            builder.ReadConfiguration();
        }
        catch (Exception exc)
        {
            throw new ConfigurationException(exc.getMessage(), exc.getCause());
        }
        com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[] caches = new com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[builder.getConfiguration().length];
        System.arraycopy(builder.getConfiguration(), 0, caches, 0, builder.getConfiguration().length);

        return convertToOldDom(caches);
    }
    
    //Changes for New Dom compatibility with Old Dom [Numan Hanif]
    private static com.alachisoft.tayzgrid.config.dom.CacheServerConfig[] convertToOldDom(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[] newCacheConfigsList) throws Exception {
        com.alachisoft.tayzgrid.config.dom.CacheServerConfig[] oldCacheConfigsList = new CacheServerConfig[newCacheConfigsList.length];

        for (int index = 0; index < newCacheConfigsList.length; index++)
        {
            oldCacheConfigsList[index]=com.alachisoft.tayzgrid.config.newdom.DomHelper.convertToOldDom(newCacheConfigsList[index]);
        }
        return oldCacheConfigsList;


    }

    private static void LoadConfig(String fileName, tangible.RefObject<java.util.HashMap> properties) throws ConfigurationException, Exception
    {
        ConfigurationBuilder builder = new ConfigurationBuilder(fileName);
        try
        {
            builder.RegisterRootConfigurationObject(com.alachisoft.tayzgrid.config.newdom.CacheServerConfig.class);
        }
        catch (Exception exception)
        {
            throw new ConfigurationException(exception.getMessage(), exception);
        }
        try
        {
            builder.ReadConfiguration();
        }
        catch (Exception exc)
        {
            throw new ConfigurationException(exc.getMessage(), exc.getCause());
        }
        com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[] caches = new com.alachisoft.tayzgrid.config.newdom.CacheServerConfig[builder.getConfiguration().length];
        System.arraycopy(builder.getConfiguration(), 0, caches, 0, builder.getConfiguration().length);
        //Changes for New Dom compatibility with Old Dom [Numan Hanif]
        properties.argvalue = ConfigConverter.ToHashMap(convertToOldDom(caches));
    }

    public final java.util.Hashtable GetProperties(String fileName, String configSection, String partId) throws ConfigurationException
    {
        java.util.HashMap properties = new java.util.HashMap();
        try
        {
            tangible.RefObject<java.util.HashMap> tempRef_properties = new tangible.RefObject<java.util.HashMap>(properties);
            LoadConfig(fileName, tempRef_properties);
            properties = tempRef_properties.argvalue;
            return (java.util.Hashtable)properties.get(configSection);
        }
        catch (Exception e)
        {
            throw new ConfigurationException("Specified config section '" + configSection + "' not found in file '" + fileName + "'. If it is a cache, it must be registered properly on this machine. --- " +  e.toString());
        }
    }

    public final java.util.Hashtable GetProperties2(String fileName, String configSection, String partId) throws ConfigurationException
    {
        java.util.Hashtable properties = new java.util.Hashtable();
        try
        {

            File fXmlFile = new File("c:\\file.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            Node i = (Node) doc.getElementsByTagName("/configuration/cache-config");
            while (i.hasChildNodes())
            {
                Element element;
                if (i.getNodeType() == Node.ELEMENT_NODE)
                {
                    element = (Element) i;

                    String name = getTagValue("name", element).toLowerCase();
                    if (name.compareTo(configSection.toLowerCase()) != 0)
                    {
                        continue;
                    }

                    if (i.hasChildNodes())
                    {
                        java.util.Hashtable section = new java.util.Hashtable();

                        BuildHashtable2(section, i.getNextSibling());

                        section.put("name", getTagValue("name", element));
                        section.put("inproc", getTagValue("inproc", element));

                        properties.put(getTagValue("name", element).toLowerCase(), section);
                    }
                }
            }
            return properties;
        }
        catch (Exception e)
        {
            throw new ConfigurationException("Error occured while reading configuration", e);
        }
    }

    private static String getTagValue(String sTag, Element eElement) {
	NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();

        Node nValue = (Node) nlList.item(0);

	return nValue.getNodeValue();
  }

    public final CacheServerConfig GetConfigDom() throws ConfigurationException
    {
        try
        {
            CacheServerConfig[] configs = LoadConfig(this._configFileName);
            CacheServerConfig configDom = null;
            if (configs != null)
            {
                for (CacheServerConfig config : configs)
                {
                    if (config.getName() != null && config.getName().toLowerCase().equals(this._configSection.toLowerCase()))
                    {
                        configDom = config;
                        break;
                    }
                }
            }

            if (configDom != null)
            {
                return configDom;
            }
        }
        catch (Exception e)
        {
            throw new ConfigurationException("Error occured while reading configuration", e);
        }
        throw new ConfigurationException("Specified config section '" + this._configSection + "' not found in file '" + this._configFileName + "'. If it is a cache, it must be registered properly on this machine.");
    }

    public final java.util.ArrayList GetAllProperties(String fileName, String configSection) throws ConfigurationException
    {
        java.util.ArrayList propsList = new java.util.ArrayList();
        java.util.HashMap properties = new java.util.HashMap();

        try
        {
            tangible.RefObject<java.util.HashMap> tempRef_properties = new tangible.RefObject<java.util.HashMap>(properties);
            LoadConfig(fileName, tempRef_properties);
            properties = tempRef_properties.argvalue;
            if (properties.containsKey(configSection.toLowerCase()))
            {
                propsList.add(properties.get(configSection.toLowerCase()));
            }
            return propsList;
        }
        catch (Exception e)
        {
            throw new ConfigurationException("Specified config section '" + configSection + "' not found in file '" + fileName + "'. If it is a cache, it must be registered properly on this machine. --- " +  e.toString());
        }
    }

    public final java.util.ArrayList GetAllProperties2(String fileName, String configSection) throws ConfigurationException
    {
        java.util.ArrayList propsList = new java.util.ArrayList();
        java.util.Hashtable properties = new java.util.Hashtable();

        try
        {

            File fXmlFile = new File("c:\\file.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            Node i = (Node) doc.getElementsByTagName("/configuration/cache-config");
            while (i.hasAttributes())
            {
                if (i.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) i;
                    if (getTagValue("name", element) == null)
                    {
                        continue;
                    }

                    String name = getTagValue("name",element).toLowerCase();
                    if (name.compareTo(configSection.toLowerCase()) != 0)
                    {
                        continue;
                    }

                    if (i.hasChildNodes())
                    {
                        java.util.Hashtable section = new java.util.Hashtable();

                        BuildHashtable2(section, i.getNextSibling());

                        section.put("name", getTagValue("name", element));
                        section.put("inproc", getTagValue("inproc", element));

                        Object tempVar = section.clone();
                        propsList.add((java.util.Hashtable) ((tempVar instanceof java.util.Hashtable) ? tempVar : null));
                    }
                }
            }
            return propsList;
        }
        catch (Exception e)
        {
            throw new ConfigurationException("Specified config section '" + configSection + "' not found in file '" + fileName + "'. If it is a cache, it must be registered properly on this machine. --- " +  e.toString());
        }
    }

    public final java.util.Map GetProperties(String fileName) throws ConfigurationException
    {
        java.util.HashMap properties = new java.util.HashMap();
        try
        {
            tangible.RefObject<java.util.HashMap> tempRef_properties = new tangible.RefObject<java.util.HashMap>(properties);
            LoadConfig(fileName, tempRef_properties);
            properties = tempRef_properties.argvalue;
            return properties;
        }
        catch(Exception e)
        {
            throw new ConfigurationException("Error occured while reading configuration", e);
        }
    }

    public final java.util.Map GetProperties2(String fileName)throws ConfigurationException
    {
        java.util.Hashtable properties = new java.util.Hashtable();
        try
        {

            File fXmlFile = new File("c:\\file.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            Node i = (Node) doc.getElementsByTagName("/configuration/cache-config");

            while(i.hasChildNodes())
            {
                if (i.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) i;
                    if (i.hasChildNodes())
                    {
                        java.util.Hashtable section = new java.util.Hashtable();

                        BuildHashtable2(section, i.getNextSibling());

                        section.put("name", getTagValue("name", element));
                        section.put("inproc", getTagValue("inproc", element));

                        properties.put(getTagValue("name", element).toLowerCase(), section);
                    }
                }
            }
            return properties;
        }
        catch (Exception e)
        {
            throw new ConfigurationException("Error occured while reading configuration", e);
        }
    }
}
