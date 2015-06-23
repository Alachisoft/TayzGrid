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

import com.alachisoft.tayzgrid.runtime.datasourceprovider.OperationResult;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteOperation;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteOperationType;
import com.alachisoft.tayzgrid.runtime.datasourceprovider.WriteThruProvider;
import com.alachisoft.tayzgrid.samples.data.Customer;
import com.alachisoft.tayzgrid.samples.data.Product;
import java.io.File;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WriteThru implements WriteThruProvider
{

    private static File _file;
    private static String _filename;
    private static TransformerFactory transformerFactory;
    private static Transformer transformer;
    private static DOMSource source;
    private static DocumentBuilderFactory docFactory;
    private static DocumentBuilder docBuilder;
    private static Document document;
    
    @Override
    public void init(HashMap hm, String string) throws Exception
    {
        //Perform start implementation here
        docFactory = DocumentBuilderFactory.newInstance();


        docBuilder = docFactory.newDocumentBuilder();
        docFactory.setNamespaceAware(true); // never forget this!

        String filename = System.getenv("TG_HOME") + "/samples/dataproviders/dist/XMLBackingSource.xml";
        _filename = filename;

        File file = new File(filename);
        boolean fileCreated = file.createNewFile();

        if (fileCreated)
        {
            document = docBuilder.newDocument();
            Element rootElement = document.createElement("Items");
            rootElement.normalize();
            document.appendChild(rootElement);
        }
        else
        {
            document = docBuilder.parse(filename);
        }

        _file = file;

        // write the content into xml file
        transformerFactory = TransformerFactory.newInstance();
        transformer = transformerFactory.newTransformer();
        source = new DOMSource(document);
        StreamResult result = new StreamResult(filename);
        transformer.transform(source, result);
    }

    @Override
    public OperationResult writeToDataSource(WriteOperation wo) throws Exception
    {
        WriteOperationType writeOperationType = wo.getOperationType();
        
        Object value =  wo.getProviderCacheItem().getValue();
        Object key = wo.getKey();
        
        OperationResult operationResult = new OperationResult(wo, OperationResult.Status.Failure);
        operationResult.setError("Key already exist");
        
        if( value == null )
        {
            return operationResult;
        }
        //if add operation is received 
        if( writeOperationType == WriteOperationType.Add )
        {
            if(addToSource(key, value) == true )
            {
                operationResult.setDSOperationStatus(OperationResult.Status.Success);
                return operationResult;
            }
            else
            {
                return operationResult;
            }
        }
        //if Update Operation is received 
        if( writeOperationType == WriteOperationType.Update )
        {
            if(updateToSource(key, value) == true )
            {
                operationResult.setDSOperationStatus(OperationResult.Status.Success);
                return operationResult;
            }
            else
            {
                return operationResult;
            }
        }
        if( writeOperationType == WriteOperationType.Delete )
        {
            if(removeFromSource(key) == true )
            {
                operationResult.setDSOperationStatus(OperationResult.Status.Success);
                return operationResult;
            }
            else
            {
                return operationResult;
            }
        }
    return operationResult;
}

    @Override
    public OperationResult[] writeToDataSource(WriteOperation[] wos) throws Exception
    {
        OperationResult [] result=new OperationResult[wos.length];
        int counter = 0;
        boolean dsFlag=false;
        for(WriteOperation Wo : wos)
        {
            if(Wo.getOperationType()== WriteOperationType.Add)
                dsFlag=addToSource(Wo.getKey(), Wo.getProviderCacheItem().getValue());
            
            if(Wo.getOperationType()== WriteOperationType.Delete)
                dsFlag = removeFromSource(Wo.getKey());
            
            if(Wo.getOperationType()== WriteOperationType.Update)
                dsFlag=updateToSource(Wo.getKey(), Wo.getProviderCacheItem().getValue());
            
            
            result[counter]=new OperationResult(Wo, OperationResult.Status.Failure);
            
            if(dsFlag)
               result[counter].setDSOperationStatus(OperationResult.Status.Success);
                       
            counter++;
        }
        return result;
    }

    @Override
    public void dispose() throws Exception
    {
    }
    
    //---
    private synchronized void WriteToXML()
    {
        try
        {
            // write the content into xml file
            source = new DOMSource(document);
            StreamResult result = new StreamResult(_file);
            transformer.transform(source, result);
        }
        catch (Exception ex)
        {
            //Write to error logs here...
        }
    }

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
                    return _node;
                }
            }
            return null;
        }
        else
        {
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
    
    private boolean addToSource(Object key, Object value)
    {
        try
        {
            document = docBuilder.parse(_filename);

            //check if node with key already exists                   
            if (IsNodePresent(key))
            {
                return false;
            }

            //Check if root of the document exists
            Element rootElement = document.getDocumentElement();
            if (rootElement == null)
            {
                rootElement = document.createElement("Items");
                rootElement.normalize();
                document.appendChild(rootElement);
            }

            //infer the typeof object
            if (value instanceof Product)
            {
                Element element = document.createElement("Product");

                //Key element
                Element keyElement = document.createElement("Key");
                keyElement.appendChild(document.createTextNode(key.toString()));
                element.appendChild(keyElement);

                //Id element
                Element id = document.createElement("Id");
                id.appendChild(document.createTextNode(String.valueOf(((Product) value).getId())));
                element.appendChild(id);

                //Name element
                Element name = document.createElement("Name");
                name.appendChild(document.createTextNode(((Product) value).getName()));
                element.appendChild(name);

                //Class element
                Element _class = document.createElement("Class");
                _class.appendChild(document.createTextNode(((Product) value).getClassName()));
                element.appendChild(_class);

                //Category element
                Element category = document.createElement("Category");
                category.appendChild(document.createTextNode(((Product) value).getCategory()));
                element.appendChild(category);

                element.normalize();

                rootElement.appendChild(element);

            }
            else if (value instanceof Customer)
            {
                Element element = document.createElement("Customer");

                //Key element
                Element keyElement = document.createElement("Key");
                keyElement.appendChild(document.createTextNode(key.toString()));
                element.appendChild(keyElement);

                Element name = document.createElement("Name");
                name.appendChild(document.createTextNode(((Customer) value).getName()));
                element.appendChild(name);

                Element age = document.createElement("Age");
                age.appendChild(document.createTextNode(String.valueOf(((Customer) value).getAge())));
                element.appendChild(age);

                Element contactNo = document.createElement("ContactNo");
                contactNo.appendChild(document.createTextNode(((Customer) value).getContactNo()));
                element.appendChild(contactNo);

                Element address = document.createElement("Address");
                address.appendChild(document.createTextNode(((Customer) value).getAddress()));
                element.appendChild(address);

                Element gender = document.createElement("Gender");
                gender.appendChild(document.createTextNode(((Customer) value).getGender()));
                element.appendChild(gender);

                element.normalize();
                rootElement.appendChild(element);
            }

            WriteToXML();
            return true;
        }
        catch (Throwable ex)
        {
            // Log exception here     
        }
        return false;
    }
    
    private boolean updateToSource(Object key,Object value)
    {
        //How data is added to the file ... Define logic here
        try
        {
            document = docBuilder.parse(_filename);

            //check if node with key already exists    
            Node previousNode = null;

            if (IsNodePresent(key))
            {
                NodeList nodeList = document.getElementsByTagName("Product");
                previousNode = getNode(nodeList, key);
                if (previousNode == null)
                {
                    nodeList = document.getElementsByTagName("Customer");
                    previousNode = getNode(nodeList, key);
                }
            }

            //Check if root of the document exists
            Element rootElement = document.getDocumentElement();
            if (rootElement == null)
            {
                rootElement = document.createElement("Items");
                rootElement.normalize();
                document.appendChild(rootElement);
            }

            if (previousNode == null)
            {
                return addToSource(key, value);
            }
            else
            {
                //infer the typeof object
                if (value instanceof Product)
                {

                    Element element = document.createElement("Product");

                    //Key element
                    Element keyElement = document.createElement("Key");
                    keyElement.appendChild(document.createTextNode(key.toString()));
                    element.appendChild(keyElement);

                    //Id element
                    Element id = document.createElement("Id");
                    id.appendChild(document.createTextNode(String.valueOf(((Product) value).getId())));
                    element.appendChild(id);

                    //Name element
                    Element name = document.createElement("Name");
                    name.appendChild(document.createTextNode(((Product) value).getName()));
                    element.appendChild(name);

                    //Class element
                    Element _class = document.createElement("Class");
                    _class.appendChild(document.createTextNode(((Product) value).getClassName()));
                    element.appendChild(_class);

                    //Category element
                    Element category = document.createElement("Category");
                    category.appendChild(document.createTextNode(((Product) value).getCategory()));
                    element.appendChild(category);

                    element.normalize();

                    rootElement.removeChild(previousNode);
                    rootElement.appendChild(element);

                }
                else if (value instanceof Customer)
                {

                    Element element = document.createElement("Customer");

                    //Key element
                    Element keyElement = document.createElement("Key");
                    keyElement.appendChild(document.createTextNode(key.toString()));
                    element.appendChild(keyElement);

                    Element name = document.createElement("Name");
                    name.appendChild(document.createTextNode(((Customer) value).getName()));
                    element.appendChild(name);

                    Element age = document.createElement("Age");
                    age.appendChild(document.createTextNode(String.valueOf(((Customer) value).getAge())));
                    element.appendChild(age);

                    Element contactNo = document.createElement("ContactNo");
                    contactNo.appendChild(document.createTextNode(((Customer) value).getContactNo()));
                    element.appendChild(contactNo);

                    Element address = document.createElement("Address");
                    address.appendChild(document.createTextNode(((Customer) value).getAddress()));
                    element.appendChild(address);

                    Element gender = document.createElement("Gender");
                    gender.appendChild(document.createTextNode(((Customer) value).getGender()));
                    element.appendChild(gender);

                    element.normalize();

                    rootElement.removeChild(previousNode);
                    rootElement.appendChild(element);
                }
            }

            // write the content into xml file
            WriteToXML();
            return true;
        }
        catch (Throwable ex)
        {
                // Log exception here            
        }
        return false;
    }
    
    private boolean removeFromSource(Object key)
    {
        try
        {
            document = docBuilder.parse(_file);

            Node node = document.getElementById(key.toString());
            document.removeChild(node);
            return true;
        }
        catch (Exception ex)
        {
            // Log exception here            
        }
        return false;
    }
}
