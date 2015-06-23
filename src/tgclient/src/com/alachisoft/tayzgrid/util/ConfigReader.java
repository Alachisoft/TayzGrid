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

package com.alachisoft.tayzgrid.util;

import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 *
 */
public class ConfigReader
{

    String fileName;
    private static String _bindIP = null;

    /**
     * Creates a new instance of ConfigReader
     */
    public ConfigReader()
    {
    }

    public static Map ReadParams(String fileName, String cache) throws SAXException, IOException
    {

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;

        try
        {
            builder = builderFactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException ex)
        {
        }

        Document response = null;

        File f = new File(fileName);
        response = builder.parse(f);
        f = null;

        NodeList cacheNL = response.getElementsByTagName("cache");
        NodeList serversNL = null;
        Node cacheN = null;

        for (int i = 0; i < cacheNL.getLength(); i++)
        {
            cacheN = cacheNL.item(i);
            if (cacheN.hasAttributes())
            {
                if (cacheN.getAttributes().getNamedItem("id").getNodeValue().equalsIgnoreCase(cache))
                {
                    serversNL = cacheN.getChildNodes();
                    break;
                }
            }
        }

        if (serversNL == null)
        {
            return null;
        }

        int numServers = serversNL.getLength();
        Map serverMap = new HashMap();

        for (int i = 0; i < numServers; i++)
        {
            Node serverNode = serversNL.item(i);

            if (serverNode.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }

            NodeList data = serverNode.getChildNodes();

            Map attribMap = new HashMap();

            for (int j = 0; j < data.getLength(); j++)
            {
                Node n = data.item(j);
                if (n.getNodeType() != Node.ELEMENT_NODE)
                {
                    continue;
                }

                String key = n.getNodeName();
                String value = n.getTextContent();

                attribMap.put(key, value);
            }

            serverMap.put((String) attribMap.get("priority"), attribMap);
        }
        return serverMap;
    }

    public static String BindIP() throws Exception
    {
        File file = null;
        if (_bindIP != null)
        {
            return _bindIP;
        }
        try
        {
            String path = DirectoryUtil.getConfigPath("client.conf");
            file = new File(path);
            if (!file.exists())
            {
                return "";
            }
            Document configuration = null;

            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = null;

            try
            {
                builder = builderFactory.newDocumentBuilder();
            }
            catch (ParserConfigurationException ex)
            {
            }

            configuration = builder.parse(file);
            NodeList cacheList = configuration.getElementsByTagName("jvcache-client");

            for (int i = 0; i < cacheList.getLength(); i++)
            {
                Node cache = cacheList.item(i);
                if (cache.hasAttributes())
                {
                    _bindIP = cache.getAttributes().getNamedItem("bind-ip-address").getNodeValue();
                    break;
                }
            }
        }
        catch (ConfigurationException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ConfigurationException("An error occured while reading client.conf. " + e.getMessage());
        }
        finally
        {
        }
        if (_bindIP != null)
        {
            return _bindIP;
        }
        return "";
    }
    public static java.util.HashMap ReadSecurityParams(String fileName, String cacheId) throws ConfigurationException
    {
        java.util.HashMap securityParams = null;
        try
        {
            String filePath = DirectoryUtil.getConfigPath(fileName);
            if (filePath == null)
            {
                return null;
            }

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new File(filePath));
            Element docEle = document.getDocumentElement();

            NodeList cacheList = docEle.getElementsByTagName("cache");
            NodeList cacheConfig = null;
            if (cacheList != null && cacheList.getLength() > 0)
            {
                for (int nodeItem = 0; nodeItem < cacheList.getLength(); nodeItem++)
                {
                    Node cacheNode = cacheList.item(nodeItem);
                    if (cacheNode.getNodeType() == Node.ELEMENT_NODE)
                    {
                        Element cacheElement = (Element) cacheNode;
                        if (cacheId.equals(cacheElement.getAttribute("id")))
                        {
                            cacheConfig = cacheNode.getChildNodes();
                            break;
                        }
                    }
                }
            }

            if (cacheConfig == null)
            {
                return null;
            }
            if (cacheConfig != null && cacheConfig.getLength() > 0)
            {

                for (int nodeItem = 0; nodeItem < cacheConfig.getLength(); nodeItem++)
                {
                    Node currentConfig = cacheConfig.item(nodeItem);
                    if (currentConfig.getNodeType() == Node.ELEMENT_NODE)
                    {
                        Element cacheElement = (Element) currentConfig;
                        if (cacheElement.getTagName().equals("security"))
                        {
                            NodeList securityData = currentConfig.getChildNodes();
                            if (securityData != null)
                            {
                                securityParams = new java.util.HashMap();
                                java.util.HashMap tmp = null;

                                for (int j = 0; j < securityData.getLength(); j++)
                                {
                                    Node securitUser = securityData.item(j);
                                    if (securitUser.getNodeType() == Node.ELEMENT_NODE)
                                    {
                                    Element primaryElement = (Element) securitUser;
                                    String tempVar = primaryElement.getTagName();
                                    if (tempVar.equals("primary"))
                                    {
                                        tmp = new java.util.HashMap();
                                        securityParams.put("pri-user", tmp);
                                        tmp.put("user-id", primaryElement.getAttribute("user-id"));
                                        tmp.put("password", primaryElement.getAttribute("password"));

                                    }
                                    else if (tempVar.equals("secondary"))
                                    {
                                        tmp = new java.util.HashMap();
                                        securityParams.put("sec-user", tmp);
                                        tmp.put("user-id", primaryElement.getAttribute("user-id"));
                                        tmp.put("password", primaryElement.getAttribute("password"));
                                    }
                                    else
                                    {
                                        throw new com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException("Invalid XmlNode \'" + primaryElement.getTagName()
                                                + "\' found in security section");
                                    }
                                    }
                                }
                                return securityParams;
                            }
                            return null;
                        }
                    }
                }
            }
        }
        catch (com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException ex)
        {
            throw ex;
        }
        catch (RuntimeException e)
        {
            throw new ConfigurationException("An error occured while reading client.conf. " + e.getMessage());
        }
        catch (SAXException se)
        {
            return null;
        }
        catch (ParserConfigurationException pce)
        {
            return null;
        }
        catch (IOException ioe)
        {
            return null;
        }
        catch (Exception e)
        {
            throw new ConfigurationException(e.getMessage());
        }

        return securityParams;
    }

    private static String GetClientConfigurationPath(String fileName)
    {
        return DirectoryUtil.GetBaseFilePath(fileName);
    }
}
