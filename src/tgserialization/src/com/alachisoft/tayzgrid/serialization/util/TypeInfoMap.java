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


package com.alachisoft.tayzgrid.serialization.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TypeInfoMap
{

    private int _typeHandle = 0;
    private HashMap _map;
    private HashMap _typeToHandleMap;
    StringBuilder _protocolString = null;

    public TypeInfoMap(String protocolString)
    {
        createMap(protocolString);
    }

    public TypeInfoMap(HashMap protocolMap)
    {
        createMap(protocolMap);
    }
     public final String ToProtocolString()
    {
        _protocolString = new StringBuilder();
        _protocolString.append(_map.size()).append("\"");

        Iterator mapDic = _map.entrySet().iterator();
        while (mapDic.hasNext())
        {
            Map.Entry pair = (Map.Entry)mapDic.next();
            _protocolString.append((Integer)(pair.getKey())).append("\"");

            java.util.HashMap type = (java.util.HashMap) ((pair.getValue() instanceof java.util.HashMap) ? pair.getValue() : null);
            _protocolString.append((String) ((type.get("name") instanceof String) ? type.get("name") : null)).append("\"");

            java.util.ArrayList attributes = (java.util.ArrayList) type.get("sequence");
            _protocolString.append(attributes.size()).append("\"");

            for (int i = 0; i < attributes.size(); i++)
            {
                _protocolString.append((String) ((attributes.get(i) instanceof String) ? attributes.get(i) : null)).append("\"");
            }
        }

        return _protocolString.toString();
    }

    private void createMap(HashMap indexClasses)
    {
        _map = new HashMap();
        _typeToHandleMap = new HashMap();

        Iterator ieI = indexClasses.entrySet().iterator();
        while (ieI.hasNext())
        {
            Map.Entry ie = (Map.Entry) ieI.next();
            HashMap innerProps = ie.getValue() instanceof HashMap ? (HashMap) ie.getValue() : null;
            if (innerProps != null)
            {
                HashMap type = new HashMap();
                HashMap attributes = new HashMap();
                ArrayList attribList = new ArrayList();
                Iterator en1 = innerProps.entrySet().iterator();
                while (en1.hasNext())
                {
                    Map.Entry en = (Map.Entry) en1.next();
                    HashMap attribs = en.getValue() instanceof HashMap ? (HashMap) en.getValue() : null;;
                    if (attribs != null)
                    {
                        Iterator ide1 = attribs.entrySet().iterator();
                        while (ide1.hasNext())
                        {
                            Map.Entry ide = (Map.Entry) ide1.next();
                            HashMap attrib = ide.getValue() instanceof HashMap ? (HashMap) ide.getValue() : null;;
                            if (attrib != null)
                            {
                                attribList.add(attrib.get("id") instanceof String ? (String) attrib.get("id") : null);
                                attributes.put(attrib.get("id") instanceof String ? (String) attrib.get("id") : null, attrib.get("data-type") instanceof String ? (String) attrib.get("data-type") : null);
                            }
                        }
                    }
                }
                type.put("name", innerProps.get("name") instanceof String ? (String) innerProps.get("name") : null);
                type.put("attributes", attributes);
                type.put("sequence", attribList);
                _map.put(_typeHandle, type);
                _typeToHandleMap.put(type.get("name") instanceof String ? (String) type.get("name") : null, _typeHandle);
                _typeHandle++;
            }
        }
    }

    private void createMap(String value)
    {
        int startIndex = 0;
        int endIndex = value.indexOf('"', startIndex + 1);

        int typeCount = Integer.parseInt(value.substring(startIndex, endIndex));
        _map = new HashMap(typeCount);
        _typeToHandleMap = new HashMap(typeCount);

        int typeHandle;
        String typeName;

        for (int i = 0; i < typeCount; i++)
        {
            startIndex = endIndex + 1;
            endIndex = value.indexOf('"', endIndex + 1);
            typeHandle = Integer.parseInt(value.substring(startIndex, endIndex));

            startIndex = endIndex + 1;
            endIndex = value.indexOf('"', endIndex + 1);
            typeName = value.substring(startIndex, endIndex);

            HashMap typeMap = new HashMap();
            typeMap.put("name", typeName);

            startIndex = endIndex + 1;
            endIndex = value.indexOf('"', endIndex + 1);
            int attributesCount = Integer.parseInt(value.substring(startIndex, endIndex));

            ArrayList attributes = new ArrayList(attributesCount);
            String attributeName;

            for (int j = 0; j < attributesCount; j++)
            {
                startIndex = endIndex + 1;
                endIndex = value.indexOf('"', endIndex + 1);
                attributeName = value.substring(startIndex, endIndex);

                attributes.add(attributeName);
            }

            typeMap.put("sequence", attributes);
            _map.put(typeHandle, typeMap);
            _typeToHandleMap.put((String) typeMap.get("name"), typeHandle);
        }
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="handle">Handle-id for the Type</param>
    /// <returns>The complete name of the Type for the given handle-id.</returns>
    public String getTypeName(int handle)
    {
        return (String) (((HashMap) _map.get(handle)).get("name"));
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="handle">Handle-id of the Type</param>
    /// <returns>Hastable contaning the attribut list of the Type.</returns>
    public HashMap getAttributes(int handle)
    {
        return (HashMap) (((HashMap) _map.get(handle)).get("attributes"));
    }

    /// <summary>
    ///
    /// </summary>
    /// <param name="typeName">Name of the Type</param>
    /// <returns>Hastable contaning the attribut list of the Type.</returns>
    public HashMap getAttributes(String typeName)
    {
        int handle = getHandleId(typeName);
        if (handle != -1 && _map.containsKey(handle))
        {
            return (HashMap) (((HashMap) _map.get(handle)).get("attributes"));
        }

        return null;
    }

    public ArrayList getAttribList(int handle)
    {
        return (ArrayList) (((HashMap) _map.get(handle)).get("sequence"));
    }

    public int getHandleId(String typeName)
    {
        if (_typeToHandleMap.containsKey(typeName))
        {
            return (Integer) _typeToHandleMap.get(typeName);
        }

        return -1;
    }
    
    public String getAttributeType(String typeName, String attributeName)
    {
        String attribteType = "";
        int handle = getHandleId(typeName);
        if(handle != -1 && _map.containsKey(handle))
        {
            HashMap map = (HashMap) ((HashMap) _map.get(handle)).get("attributes");
            attribteType = (String) map.get(attributeName);
        }
        return attribteType;
    }
}
