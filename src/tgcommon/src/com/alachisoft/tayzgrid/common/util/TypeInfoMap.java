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

package com.alachisoft.tayzgrid.common.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TypeInfoMap
{

    private int _typeHandle = 0;
    private java.util.HashMap _map;
    private java.util.HashMap _typeToHandleMap;
    private StringBuilder _protocolString = null;

    public TypeInfoMap(java.util.HashMap indexClasses)
    {
        CreateMap(indexClasses);
    }

    public TypeInfoMap(String protocolString)
    {
        CreateMap(protocolString);
    }

    private void CreateMap(java.util.HashMap indexClasses)
    {
        _map = new java.util.HashMap();
        _typeToHandleMap = new java.util.HashMap();

        Iterator ie = indexClasses.entrySet().iterator();
        while (ie.hasNext())
        {
            Map.Entry pair = (Map.Entry)ie.next();
            java.util.HashMap innerProps = (java.util.HashMap) ((pair.getValue() instanceof java.util.HashMap) ? pair.getValue() : null);
            if (innerProps != null)
            {
                java.util.HashMap type = new java.util.HashMap();
                java.util.HashMap attributes = new java.util.HashMap();
                java.util.ArrayList attribList = new java.util.ArrayList();
                Iterator en = innerProps.entrySet().iterator();
                while (en.hasNext())
                {
                    Map.Entry pair1 = (Map.Entry)en.next();
                    java.util.HashMap attribs = (java.util.HashMap) ((pair1.getValue() instanceof java.util.HashMap) ? pair1.getValue() : null);
                    if (attribs != null)
                    {
                        Iterator ide = attribs.entrySet().iterator();
                        while (ide.hasNext())
                        {
                            Map.Entry pair2 = (Map.Entry)ide.next();
                            java.util.HashMap attrib = (java.util.HashMap) ((pair2.getValue() instanceof java.util.HashMap) ? pair2.getValue() : null);
                            if (attrib != null)
                            {
                                attribList.add((String) ((attrib.get("id") instanceof String) ? attrib.get("id") : null));
                                attributes.put((String) ((attrib.get("id") instanceof String) ? attrib.get("id") : null), (String) ((attrib.get("data-type") instanceof String) ? attrib.get("data-type") : null));
                            }
                        }
                    }
                }
                type.put("name", (String) ((innerProps.get("name") instanceof String) ? innerProps.get("name") : null));
                type.put("attributes", attributes);
                type.put("sequence", attribList);
                _map.put(_typeHandle, type);
                _typeToHandleMap.put((String) ((type.get("name") instanceof String) ? type.get("name") : null), _typeHandle);
                _typeHandle++;
            }
        }
    }

    private void CreateMap(String value)
    {
        int startIndex = 0;
        int endIndex = value.indexOf('"', startIndex + 1);

        int typeCount = Integer.parseInt(value.substring(startIndex, startIndex + (endIndex) - (startIndex)));
        _map = new java.util.HashMap(typeCount);
        _typeToHandleMap = new java.util.HashMap(typeCount);

        int typeHandle;
        String typeName;

        for (int i = 0; i < typeCount; i++)
        {
            startIndex = endIndex + 1;
            endIndex = value.indexOf('"', endIndex + 1);
            typeHandle = Integer.parseInt(value.substring(startIndex, startIndex + (endIndex) - (startIndex)));

            startIndex = endIndex + 1;
            endIndex = value.indexOf('"', endIndex + 1);
            typeName = value.substring(startIndex, startIndex + (endIndex) - (startIndex));

            java.util.HashMap typeMap = new java.util.HashMap();
            typeMap.put("name", typeName);

            startIndex = endIndex + 1;
            endIndex = value.indexOf('"', endIndex + 1);
            int attributesCount = Integer.parseInt(value.substring(startIndex, startIndex + (endIndex) - (startIndex)));

            java.util.ArrayList attributes = new java.util.ArrayList(attributesCount);
            String attributeName;

            for (int j = 0; j < attributesCount; j++)
            {
                startIndex = endIndex + 1;
                endIndex = value.indexOf('"', endIndex + 1);
                attributeName = value.substring(startIndex, startIndex + (endIndex) - (startIndex));

                attributes.add(attributeName);
            }

            typeMap.put("sequence", attributes);
            _map.put(typeHandle, typeMap);
            _typeToHandleMap.put((String) ((typeMap.get("name") instanceof String) ? typeMap.get("name") : null), typeHandle);
        }
    }

    /**
     *
     *
     * @param handle Handle-id for the Type
     * @return The complete name of the Type for the given handle-id.
     */
    public final String getTypeName(int handle)
    {
        return (String) ((java.util.HashMap) _map.get(handle)).get("name");
    }

    public final java.util.ArrayList getAttribList(int handleId)
    {
        return (java.util.ArrayList) ((java.util.HashMap) _map.get(handleId)).get("sequence");
    }

    /**
     *
     *
     * @param handle Handle-id of the Type
     * @return Hastable contaning the attribut list of the Type.
     */
    public final java.util.HashMap getAttributes(int handle)
    {
        return (java.util.HashMap) ((java.util.HashMap) _map.get(handle)).get("attributes");
    }

    /**
     *
     *
     * @param typeName Name of the Type
     * @return Hastable contaning the attribut list of the Type.
     */
    public final java.util.ArrayList GetAttributes(String typeName)
    {
        int handle = getHandleId(typeName);
        if (handle != -1 && _map.containsKey(handle))
        {
            return (java.util.ArrayList) (((java.util.HashMap) _map.get(handle)).get("attributes"));
        }

        return null;
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

    public final int getHandleId(String typeName)
    {
        if (_typeToHandleMap.containsKey(typeName))
        {
            return (Integer)_typeToHandleMap.get(typeName);
        }

        return -1;
    }
    
    public String GetAttributeType(String typeName, String attributeName)
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
