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

package com.alachisoft.tayzgrid.common.configuration;

import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import java.beans.beancontext.BeanContext;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class ConfigurationBuilder
{

    private java.util.HashMap _baseConfigurationMap = new HashMap();
    private static String[] _excludedText = new String[]
    {
        "sec", "%", "mb"
    };
    private java.util.HashMap<String, DynamicConfigType> _dynamicSectionTypeMap = new java.util.HashMap<String, DynamicConfigType>();
    private java.util.ArrayList _lastLoadedConfiugration = new java.util.ArrayList();
    private String _file;
    private String _path = null;
    private static final String DYNAMIC_CONFIG_SECTION = "dynamic-config-object";

    public ConfigurationBuilder(Object[] configuration) throws Exception
    {
        setConfiguration(configuration);

    }

    public ConfigurationBuilder(String file)
    {
        _file = file;
    }

    /**
     * In this case user will provide the xml data at the time of Reading
     */
    public ConfigurationBuilder()
    {
    }

    public ConfigurationBuilder(String file, String path)
    {
        _file = file;
        _path = path;

    }

    public final Object[] getConfiguration()
    {
        return _lastLoadedConfiugration.toArray();
    }

    public final void setConfiguration(Object[] value) throws Exception
    {


        synchronized (_lastLoadedConfiugration)
        {
            _lastLoadedConfiugration.clear();
            for (int i = 0; i < value.length; i++)
            {
                String rootAttrib = ValidateForRootConfiguration(value[i].getClass());
                if (rootAttrib != null)
                {
                    _lastLoadedConfiugration.add(value[i]);
                }
                else
                {
                    throw new Exception(value[i].getClass() + " is not marked as RootConfiguration");
                }
            }
        }


    }

    /**
     * Registers a type to be matched for root configuration. ConfigurationBuilder map an XML config to a .Net class if it is registered with the framework.
     *
     * @param type type of the object which is to be mapped to a XML section. Registering object should have a ConfigurationRootAttribute.
     */
    public final void RegisterRootConfigurationObject(java.lang.Class type) throws Exception
    {
        String rootConfiguratinAttrib = ValidateForRootConfiguration(type);
        if (rootConfiguratinAttrib == null)
        {
            throw new Exception(type.toString() + " is not marked as RootConfiguration");
        }
        else
        {
            _baseConfigurationMap.put(rootConfiguratinAttrib.toLowerCase(), type);
        }
    }

    private static String ValidateForRootConfiguration(java.lang.Class type)
    {
        String rootAttrib = null;
        Annotation[] customAttributes = type.getAnnotations();

        if (customAttributes != null)
        {
            for (Annotation attrib : customAttributes)
            {
                if (attrib instanceof ConfigurationRootAnnotation)
                {
                    rootAttrib = ((ConfigurationRootAnnotation) attrib).value();
                    break;
                }
            }
        }
        return rootAttrib;
    }

    public final void ReadConfiguration() throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, Exception
    {
        ReadConfiguration(_file, _path);
    }

    public final void ReadConfiguration(String xml) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, Exception
    {
        ReadConfiguration(_file, _path);
    }

    public final void ReadConfiguration(Document bridges) throws InstantiationException, IllegalAccessException, Exception
    {
        NodeList nodeList = bridges.getChildNodes();
        if ((nodeList != null) && (nodeList.getLength() > 0))
        {
            for (int i = 0; i < nodeList.getLength(); i++)
            {
                Node node = nodeList.item(i);
                if (((node.getNodeType() != Node.CDATA_SECTION_NODE) && (node.getNodeType() != Node.COMMENT_NODE))) //&& (node.getNodeType() != Node. .XmlDeclaration))
                {
                    this.ReadConfigurationForNode(node);
                }
            }
        }
    }

    private void ReadConfiguration(String file, String path) throws ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, Exception
    {
        if (file == null)
        {
            throw new Exception("File name can not be null");
        }

        path = path == null ? "" : path;

        _lastLoadedConfiugration = new java.util.ArrayList();
        String fileName = path + file;
        if (!(new java.io.File(fileName)).isFile())
        {
            throw new Exception("File " + fileName + " not found");
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        Document document = null;
        try
        {
            DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.parse(new File(fileName));
        }
        catch (Exception e)
        {
            throw new Exception("Can not open " + fileName + " Error:" + e.toString());
        }

        ReadConfiguration(document);

    }

    /*
     * public final void ReadConfiguration(XmlDocument xmlDocument) { XmlNodeList nodeList = xmlDocument.ChildNodes; if (nodeList != null && nodeList.size() > 0) { for (int i = 0;
     * i < nodeList.size(); i++) { XmlNode node = nodeList[i];
     *
     * if (node.NodeType == XmlNodeType.CDATA || node.NodeType == XmlNodeType.Comment || node.NodeType == XmlNodeType.XmlDeclaration) { continue; } ReadConfigurationForNode(node);
     * } } }
     */
    private Object GetConfigurationObject(String cofingStr) throws InstantiationException, IllegalAccessException
    {
        Object cfgObject = null;
        if (_baseConfigurationMap.containsKey(cofingStr.toLowerCase()))
        {
            java.lang.Class type = (java.lang.Class) ((_baseConfigurationMap.get(cofingStr.toLowerCase()) instanceof java.lang.Class) ? _baseConfigurationMap.get(cofingStr.toLowerCase()) : null);
            cfgObject = Activator.createInstance(type);

            _lastLoadedConfiugration.add(cfgObject);

        }
        return cfgObject;
    }

    private void ReadConfigurationForNode(Node node) throws InstantiationException, IllegalAccessException, Exception
    {
        Object cfgObject = GetConfigurationObject(node.getNodeName().toLowerCase());
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node node1 = nodeList.item(i);
            if (node1.getNodeType() == Node.ELEMENT_NODE)
            {
                Element element = (Element) node1;
                if (node1.getNodeName().toLowerCase().equals(DYNAMIC_CONFIG_SECTION))
                {
                    ExtractDyanamicConfigSectionObjectType(node1);
                }
            }
        }
        if (cfgObject != null)
        {
            PopulateConfiugrationObject(cfgObject, node);
        }
        else
        {
            for (int i = 0; i < nodeList.getLength(); i++)
            {
                node = nodeList.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE)
                {
                    continue;
                }
                ReadConfigurationForNode(node);
            }
        }
    }

    private void PopulateConfiugrationObject(Object config, Node node) throws Exception
    {
        if (node == null || config == null)
        {
            return;
        }

        Element element = (Element) node;
        NamedNodeMap attribs = element.getAttributes();

        for (int i = 0; i < attribs.getLength(); i++)
        {
            FillConfigWithAttribValue(config, attribs.item(i));
        }

        NodeList nodeList = node.getChildNodes();
        HashMap sameSections = new HashMap();

        for (int i = 0; i < nodeList.getLength(); i++)
        {
            if (nodeList.item(i).getNodeType() == Node.ELEMENT_NODE)
            {
                Element childElements = (Element) nodeList.item(i);
                Class sectionType = null;

                if (childElements.getNodeName().toLowerCase() == DYNAMIC_CONFIG_SECTION && childElements.hasAttributes())
                {
                    //Not called as yet
                    ExtractDyanamicConfigSectionObjectType(childElements);
                }
            }
        }


        for (int i = 0; i < nodeList.getLength(); i++)
        {
            if (nodeList.item(i).getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element sectionNode = (Element) nodeList.item(i);
            Class sectionType = null;
            if (sectionNode.getNodeName().toLowerCase().equals(DYNAMIC_CONFIG_SECTION))
            {
                continue;
            }

            sectionType = GetConfigSectionObjectType(config, sectionNode.getNodeName());

            if (sectionType != null)
            {
                if (sectionType.isArray())
                {
                    String nonArrayType = sectionType.getCanonicalName().replace("[]", "");
                    ArrayList sameSessionList = null;
                    HashMap tmp = null;
                    if (!sameSections.containsKey(sectionType))
                    {
                        tmp = new HashMap();
                        tmp.put("section-name", sectionNode.getNodeName());

                        sameSessionList = new ArrayList();
                        tmp.put("section-list", sameSessionList);
                        sameSections.put(sectionType, tmp);
                    }
                    else
                    {
                        tmp = sameSections.get(sectionType) instanceof HashMap ? (HashMap) sameSections.get(sectionType) : null;
                        sameSessionList = tmp.get("section-list") instanceof ArrayList ? (ArrayList) tmp.get("section-list") : null;
                    }

                    Class clas = Class.forName(nonArrayType);
                    Object singleSessionObject = Activator.createInstance(clas);
                    PopulateConfiugrationObject(singleSessionObject, sectionNode);
                    sameSessionList.add(singleSessionObject);
                }
                else
                {
                    Object objHandle = Activator.createInstance(sectionType);
                    Object sectionConfig = objHandle;
                    PopulateConfiugrationObject(sectionConfig, sectionNode);
                    SetConfigSectionObject(config, sectionConfig, sectionNode.getNodeName());
                }
            }

            if (sameSections.size() > 0)
            {
                HashMap tmp;
                Iterator ide = sameSections.entrySet().iterator();
                while (ide.hasNext())
                {
                    Map.Entry pair = (Map.Entry) ide.next();
                    Class classtype = pair.getKey() instanceof Class ? ((Class) pair.getKey()) : null;
                    String type = classtype.getCanonicalName().replace("[]", "");
                    tmp = pair.getValue() instanceof HashMap ? (HashMap) pair.getValue() : null;
                    ArrayList sameSessionList = tmp.get("section-list") instanceof ArrayList ? (ArrayList) tmp.get("section-list") : null;
                    String sectionName = tmp.get("section-name") instanceof String ? (String) tmp.get("section-name") : null;
                    Object[] sessionArrayObj = new Object[sameSessionList.size()];
                    for (int j = 0; j < sameSessionList.size(); j++)
                    {
                        sessionArrayObj[j] = Activator.createInstance(Class.forName(classtype.getCanonicalName().replace("[]", "")));
                    }

                    if (sessionArrayObj != null)
                    {
                        System.arraycopy(sameSessionList.toArray(), 0, sessionArrayObj, 0, sameSessionList.size());
                        Object obj = sessionArrayObj;
                        SetConfigSectionObject(config, obj, sectionName);
                    }
                }
            }
        }
    }

    private Object GetConfigSectionObject(Object config, String sectionName)
    {
        return null;
    }

    /**
     * Gets the type of the section object.
     *
     * @param config
     * @param sectionName
     * @return
     */
    private java.lang.Class GetConfigSectionObjectType(Object config, String sectionName)
    {
        java.lang.Class sectionType = null;
        Class type = config.getClass();
        Method[] objProps = type.getMethods();

        if (objProps != null)
        {
            for (int i = 0; i < objProps.length; i++)
            {
                Method fieldInfo = objProps[i];
                Object[] customAttribs = fieldInfo.getAnnotations();
                if (customAttribs != null && customAttribs.length > 0)
                {
                    ConfigurationSectionAnnotation configAttrib = (ConfigurationSectionAnnotation) ((customAttribs[0] instanceof ConfigurationSectionAnnotation) ? customAttribs[0] : null);
                    if (configAttrib != null && configAttrib.value().toLowerCase().equals(sectionName.toLowerCase()))
                    {
                        //for simplicity get return type of the getter method, skip for the setter
                        if (fieldInfo.getParameterTypes().length > 0)
                        {
                            continue;
                        }
                        sectionType = fieldInfo.getReturnType();
                        break;
}
                }
            }
        }

        if (sectionType == null)
        {
            if (_dynamicSectionTypeMap.containsKey(sectionName.toLowerCase()))
            {
                sectionType = _dynamicSectionTypeMap.get(sectionName.toLowerCase()).getType();
            }
        }

        return sectionType;
    }

    /**
     * Gets the type of the section object.
     *
     * @param config
     * @param sectionName
     * @return
     */
    private void ExtractDyanamicConfigSectionObjectType(Node node)
    {
        java.lang.Class sectionType = null;
        if (node != null)
        {
            String assemblyName = null;
            String className = null;
            boolean isArray = false;
            String sectionid = null;
            NamedNodeMap map = node.getAttributes();

            for (int i = 0; i < map.getLength(); i++)
            {
                Node attribute = map.item(i);
                if (attribute.getNodeName().toLowerCase().equals("assembly"))
                {
                    assemblyName = attribute.getNodeValue();
                }

                if (attribute.getNodeName().toLowerCase().equals("class"))
                {
                    className = attribute.getNodeValue();
                }

                if (attribute.getNodeName().toLowerCase().equals("section-id"))
                {
                    sectionid = attribute.getNodeValue();
                }

                if (attribute.getNodeName().toLowerCase().equals("is-array"))
                {
                    isArray = Boolean.parseBoolean(attribute.getNodeValue());
                }
            }

            if (className == null || sectionid == null)
            {
                return;
            }
            //Assembly qualified name ; for ref: http://msdn.microsoft.com/en-us/library/system.type.assemblyqualifiedname.aspx


            String assebmlyQualifiedName = null;
            if (assemblyName != null)
            {
                assebmlyQualifiedName = className + "," + assemblyName;
            }
            else
            {
                assebmlyQualifiedName = className;
            }

            try
            {
                sectionType = java.lang.Class.forName(assebmlyQualifiedName);
            }
            catch (ClassNotFoundException clex)
            {
            }

            if (sectionType != null && !tangible.DotNetToJavaStringHelper.isNullOrEmpty(sectionid))
            {
                _dynamicSectionTypeMap.put(sectionid, new DynamicConfigType(sectionType, isArray));
            }
        }
    }

    private void SetConfigSectionObject(Object config, Object sectionConfig, String sectionName) throws ConfigurationException
    {
        java.lang.Class type = config.getClass();
        Method[] objProps = type.getMethods();

        if (objProps != null)
        {
            for (int i = 0; i < objProps.length; i++)
            {
                Method fieldInfo = objProps[i];
                Annotation[] customAttribs = fieldInfo.getAnnotations();
                if (customAttribs != null && customAttribs.length > 0)
                {
                    ConfigurationSectionAnnotation configSection = (ConfigurationSectionAnnotation) ((customAttribs[0].annotationType().equals(ConfigurationSectionAnnotation.class) ? customAttribs[0] : null));
                    try
                    {
                        if (configSection != null && configSection.value().equals(sectionName.toLowerCase()))
                        {
                            if (fieldInfo.getParameterTypes().length == 0)
                            {
                                continue;
                            }
                            fieldInfo.invoke(config, sectionConfig);
                            break;
                        }
                    }
                    catch (Exception e)
                    {
                        throw new ConfigurationException(e.getMessage());
                    }

                }
            }
        }
    }

    public Object ConvertToPrimitive(java.lang.Class targetType, String value, String appendedText)
    {
        Object primitiveValue = null;

        if (appendedText != null && !appendedText.equals(""))
        {
            value = value.toLowerCase().replace(appendedText.toLowerCase(), "");
        }


        if (targetType.getCanonicalName().equals("byte"))
        {
            primitiveValue = Byte.parseByte(value);

        }
        else if (targetType.getCanonicalName().equals("short"))
        {
            primitiveValue = Short.parseShort(value);

        }
        else if (targetType.getCanonicalName().equals("double"))
        {
            primitiveValue = Double.parseDouble(value);

        }
        else if (targetType.getCanonicalName().equals("boolean"))
        {
            primitiveValue = Boolean.parseBoolean(value.toLowerCase());

        }
        else if (targetType.getCanonicalName().equals("char"))
        {
            primitiveValue = value;
        }
        else if (targetType.getCanonicalName().equals("int"))
        {
            primitiveValue = Integer.parseInt(value);
        }
        else if (targetType.getCanonicalName().equals("long"))
        {
            primitiveValue = Long.decode(value);
        }
        else if (targetType.getCanonicalName().equals("float"))
        {
            primitiveValue = Float.parseFloat(value);
        }
        else if (targetType.getCanonicalName().equals("java.lang.Byte"))
        {
            primitiveValue = Byte.parseByte(value);
        }
        else if (targetType.getCanonicalName().equals("java.lang.Double"))
        {
            primitiveValue = Double.parseDouble(value);
        }
        else if (targetType.getCanonicalName().equals("java.lang.Integer"))
        {
            primitiveValue = Integer.parseInt(value);
        }
        if (targetType.getCanonicalName().equals("java.lang.Short"))
        {
            primitiveValue = Short.parseShort(value);

        }
        else if (targetType.getCanonicalName().equals("java.lang.Boolean"))
        {
            primitiveValue = Boolean.parseBoolean(value.toLowerCase());

        }
        else if (targetType.getCanonicalName().equals("java.lang.Character"))
        {

            if (value.toCharArray().length == 1)
            {
                primitiveValue = value.toCharArray()[0];
            }
            else
            {
            }
        }
        else if (targetType.getCanonicalName().equals("java.math.BigDecimal"))
        {
            primitiveValue = BigDecimal.valueOf(Double.parseDouble(value));
        }
        else if (targetType.getCanonicalName().equals("java.lang.Long"))
        {
            primitiveValue = Long.decode(value);
        }
        if (targetType.getCanonicalName().equals("java.lang.String"))
        {
            primitiveValue = value;
        }
        else if (targetType.getCanonicalName().equals("java.lang.Float"))
        {
            primitiveValue = Float.parseFloat(value);
        }
        else if(targetType.getCanonicalName().equals("java.lang.Object[]"))
        {
            primitiveValue = new Object[] { value };
        }


        return primitiveValue;
    }

    private String ExcludeExtraText(String input)
    {
        String output = input;
        if (input != null)
        {
            input = input.toLowerCase();
            for (int i = 0; i < _excludedText.length; i++)
            {
                if (input.indexOf(_excludedText[i]) >= 0)
                {
                    output = input.replace(_excludedText[i], "");
                    break;
                }
            }
        }
        return output;
    }

    private void FillConfigWithAttribValue(Object config, Node xmlAttrib) throws Exception
    {
        java.lang.Class type = config.getClass();
        Method[] objProps = type.getMethods();

        if (objProps != null)
        {
            for (int i = 0; i < objProps.length; i++)
            {
                Method fieldInfo = objProps[i];
                Annotation[] customAttribs = fieldInfo.getAnnotations();
                if (customAttribs != null && customAttribs.length > 0)
                {
                    ConfigurationAttributeAnnotation configAttrib = (ConfigurationAttributeAnnotation) ((customAttribs[0].annotationType().equals(ConfigurationAttributeAnnotation.class) ? customAttribs[0] : null));
                    try
                    {
                        if (configAttrib != null && xmlAttrib.getNodeName().toLowerCase().equals(configAttrib.value()))
                        {
                            if (fieldInfo.getParameterTypes().length == 0)
                            {
                                continue;
                            }
                            Class[] typs = fieldInfo.getParameterTypes();
                            fieldInfo.invoke(config, ConvertToPrimitive(typs[0], xmlAttrib.getNodeValue(), configAttrib.appendText()));
                            break;
                        }
                    }
                    catch (Exception e)
                    {
                        throw new Exception("Can not set the value for attribute " + configAttrib.value() + " Errror :" + e.toString());
                    }

                }
            }
        }
    }

    public final String GetXmlString() throws IllegalArgumentException, IllegalAccessException
    {
        StringBuilder sb = new StringBuilder();
        if (_lastLoadedConfiugration != null)
        {
            for (Object cfgObject : _lastLoadedConfiugration)
            {
                sb.append(GetXmlString(cfgObject));
            }
        }
        return sb.toString();
    }

    public final String GetXmlString(Object cfgObject) throws IllegalArgumentException, IllegalAccessException
    {
        StringBuilder sb = new StringBuilder();
        String rootXmlStr = null;
        java.lang.Class type = cfgObject.getClass();
        Annotation[] cfgObjCustomAttribs = type.getAnnotations();

        if (cfgObjCustomAttribs != null && cfgObjCustomAttribs.length > 0)
        {
            for (int i = 0; i < cfgObjCustomAttribs.length; i++)
            {
                if (cfgObjCustomAttribs[i].annotationType() == ConfigurationRootAnnotation.class)
                {
                    ConfigurationRootAnnotation rootAttrib = (ConfigurationRootAnnotation) cfgObjCustomAttribs[i];
                    if (rootAttrib != null)
                    {
                        rootXmlStr = rootAttrib.value();
                    }
                }
            }
        }
        return GetSectionXml(cfgObject, rootXmlStr, 1);

    }

    private String getRightPadString(int padCount){
        
        String padString = "";
        
        for(int i = 0; i< padCount; i++){
            padString += " ";
        }
        return padString;
    }
    private String GetSectionXml(Object configSection, String sectionName, int indent) throws IllegalArgumentException, IllegalAccessException
    {
        String endStr = "\r\n";
        String preStr = getRightPadString(indent*2);
 
        StringBuilder sb = new StringBuilder(preStr + "<" + sectionName);
        java.lang.Class type = configSection.getClass();

        Method[] propertiesInfo = type.getMethods();
        Arrays.sort(propertiesInfo, new Comparator<Method>(){
         @Override
            public int compare(Method o1, Method o2) {
                ConfigurationAttributeAnnotation first = o1.getAnnotation(ConfigurationAttributeAnnotation.class);
                ConfigurationAttributeAnnotation second = o2.getAnnotation(ConfigurationAttributeAnnotation.class);
                // nulls last
                if (first != null && second != null) {
                    return first.order()- second.order();
                } else
                if (first != null && second == null) {
                    return -1;
                } else
                if (first == null && second != null) {
                    return 1;
                }
                return o1.getName().compareTo(o2.getName());
            }
        });
        

        if (propertiesInfo != null && propertiesInfo.length > 0)
        {
            for (int i = 0; i < propertiesInfo.length; i++)
            {
                Method property = propertiesInfo[i];
                if (property.getReturnType().equals(void.class))
                {
                    continue;
                }

                Annotation[] customAttribs = property.getAnnotations();

                if (customAttribs != null && customAttribs.length > 0)
                {
                    for (int j = 0; j < customAttribs.length; j++)
                    {
                        ConfigurationAttributeAnnotation attrib = (ConfigurationAttributeAnnotation) (customAttribs[j].annotationType() == ConfigurationAttributeAnnotation.class ? customAttribs[j] : null);
                        if (attrib != null)
                        {
                            Object propertyValue = null;
                               
                        
                            try
                            {
                                propertyValue = property.invoke(configSection, new Object[0]);
                            }
                            catch (InvocationTargetException ex)
                            {
                                // Logger.getLogger(ConfigurationBuilder.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            String appendedText = attrib.appendText() != null ? attrib.appendText() : "";
                            if (propertyValue != null)
                            {
                                sb.append(" " + attrib.value() + "=\"" + propertyValue.toString() + appendedText + "\"");
                            }
                            else
                            {
                                sb.append(" " + attrib.value() + "=\"\"");
                            }
                        }
                    }
                }
            }
        }
        boolean subsectionsFound = false;
        boolean firstSubSection = true;
        StringBuilder comments = null;

        //get xml string for sub-sections if exists
        if (propertiesInfo != null && propertiesInfo.length > 0)
        {
            for (int i = 0; i < propertiesInfo.length; i++)
            {
                Method property = propertiesInfo[i];
                if (property.getReturnType().equals(void.class))
                {
                    continue;
                }

                Annotation[] customAttribs = property.getAnnotations();

                if (customAttribs != null && customAttribs.length > 0)
                {
                    for (int j = 0; j < customAttribs.length; j++)
                    {

                        if (property.isAnnotationPresent(ConfigurationCommentAnnotation.class))
                        {
                            Object propertyValue = null;
                            try
                            {
                                propertyValue = property.invoke(configSection, new Object[0]);
                            }
                            catch (InvocationTargetException ex)
                            {
                            }
                            if (propertyValue != null)
                            {
                                String propStr = (String) ((propertyValue instanceof String) ? propertyValue : null);
                                if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(propStr))
                                {
                                    if (comments == null)
                                    {
                                        comments = new StringBuilder();
                                    }
                                    comments.append(String.format("%1$s<!--%2$s-->%3$s", preStr, propStr, endStr));
                                }
                            }
                        }

                        if (property.isAnnotationPresent(ConfigurationSectionAnnotation.class))
                        {
                            Object propertyValue = null;
                            try
                            {
                                propertyValue = property.invoke(configSection, new Object[0]);
                            }
                            catch (InvocationTargetException ex)
                            {
                            }
                            if (propertyValue != null)
                            {
                                subsectionsFound = true;
                                if (firstSubSection)
                                {
                                    sb.append(">" + endStr);
                                    firstSubSection = false;
                                }
                                if (propertyValue.getClass().isArray())
                                {
                                    Object[] array = (Object[]) ((propertyValue instanceof Object[]) ? propertyValue : null);
                                    Object actualSectionObj;
                                    for (int k = 0; k < array.length; k++)
                                    {
                                        actualSectionObj = array[k];
                                        if (actualSectionObj != null)
                                        {
                                            sb.append(GetSectionXml(actualSectionObj, property.getAnnotation(ConfigurationSectionAnnotation.class).value(), indent + 1));
                                        }
                                    }
                                }
                                else
                                {
                                    sb.append(GetSectionXml(propertyValue, property.getAnnotation(ConfigurationSectionAnnotation.class).value(), indent + 1));
                                }
                            }
                        }
                    }
                }
            }
        }

        if (subsectionsFound)
        {
            sb.append(preStr + "</" + sectionName + ">" + endStr);
        }
        else
        {
            sb.append("/>" + endStr);
        }

        String xml = "";
        if (comments != null)
        {
            xml = comments.toString() + sb.toString();
        }
        else
        {
            xml = sb.toString();
        }

        return xml;

    }
}
