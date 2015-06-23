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

package com.alachisoft.tayzgrid.samples;

import com.alachisoft.tayzgrid.runtime.caching.ProviderCacheItem;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.ReadThruProvider;
import com.alachisoft.tayzgrid.samples.data.Customer;
import com.alachisoft.tayzgrid.samples.data.Product;
import java.io.File;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ReadThru implements ReadThruProvider
{

    private static DocumentBuilderFactory docFactory;
    private static DocumentBuilder docBuilder;
    private static Document document;
    private static Node _nodeFound;
    private static String _filename;
    
    @Override
    public void init(HashMap hm, String string) throws Exception {
        //Perform start implementation here
        docFactory = DocumentBuilderFactory.newInstance();

        docBuilder = docFactory.newDocumentBuilder();
        docFactory.setNamespaceAware(true); // never forget this!

        String filename = System.getenv("TG_HOME") + "/samples/dataproviders/dist/XMLBackingSource.xml";
        _filename = filename;

        File file = new File(filename);

        if (file.exists())
        {
            document = docBuilder.parse(filename);
        }
    }

    @Override
    public void dispose() throws Exception {
    }
    @Override
    public void loadFromSource(Object key, ProviderCacheItem pci) throws Exception
    {

        document = docBuilder.parse(_filename);

        if (IsNodePresent(key))
        {
            Node item = _nodeFound;
            String nodeName = item.toString();
            if (nodeName.contains("Product"))
            {
                Product product = new Product();
                NodeList childNodeList = item.getChildNodes();

                product.setId(Integer.parseInt(childNodeList.item(1).getChildNodes().item(0).getNodeValue()));
                product.setName(childNodeList.item(2).getChildNodes().item(0).getNodeValue());
                product.setClassName(childNodeList.item(3).getChildNodes().item(0).getNodeValue());
                product.setCategory(childNodeList.item(4).getChildNodes().item(0).getNodeValue());
                if (pci == null)
                {
                    pci = new ProviderCacheItem(product);
                }
                else
                {
                    pci.setValue(product);
                }
            }
            else if (nodeName.contains("Customer"))
            {
                Customer customer = new Customer();
                NodeList childNodeList = item.getChildNodes();
                customer.setName(childNodeList.item(1).getChildNodes().item(0).getNodeValue());
                customer.setAge(Integer.parseInt(childNodeList.item(2).getChildNodes().item(0).getNodeValue()));
                customer.setContactNo(childNodeList.item(3).getChildNodes().item(0).getNodeValue());
                customer.setAddress(childNodeList.item(4).getChildNodes().item(0).getNodeValue());
                customer.setGender(childNodeList.item(5).getChildNodes().item(0).getNodeValue());
                if (pci == null)
                {
                    pci = new ProviderCacheItem(customer);
                }
                else
                {
                    pci.setValue(customer);
                }
            }
        }
    }

    @Override
    public HashMap<Object, ProviderCacheItem> loadFromSource(Object[] keys) throws Exception
    {

        HashMap result = new HashMap();
        ProviderCacheItem[] items = new ProviderCacheItem[keys.length];
        int pointer = 0;

        for (Object key : keys)
        {
            items[pointer] = new ProviderCacheItem();
            loadFromSource(key, items[pointer]);
            result.put(key, items[pointer]);
        }
        return result;
    }

    //---
    private Node getNode(NodeList nodeList, Object value)
    {
        if (nodeList.getLength() != 0)
        {
            int nodeListLength = nodeList.getLength();
            for (int i = 0; i < nodeListLength; i++)
            {
                Node _node = nodeList.item(i);
                Node childNode = _node.getFirstChild();
                if (childNode.getTextContent().equals(value))
                {
                    _nodeFound = _node;
                    return _node;
                }
            }
            _nodeFound = null;
            return null;
        }
        else
        {
            _nodeFound = null;
            return null;
        }
    }

    private boolean IsNodePresent(Object key)
    {
        NodeList nodeList = document.getElementsByTagName("Customer");
        NodeList nodeList2 = document.getElementsByTagName("Product");
        if (getNode(nodeList, key) != null || getNode(nodeList2, key) != null)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

  

}
