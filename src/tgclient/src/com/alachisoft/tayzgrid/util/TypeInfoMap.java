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

import java.util.ArrayList;
import java.util.HashMap;

public class TypeInfoMap {
    private int _typeHandle = 0;
    private HashMap _map;
    private HashMap _typeToHandleMap;
    StringBuilder _protocolString = null;

    public TypeInfoMap(String protocolString) {
        createMap(protocolString);
    }

    private void createMap(String value) {
        int startIndex = 0;
        int endIndex = value.indexOf('"', startIndex + 1);

        int typeCount = Integer.parseInt(value.substring(startIndex, endIndex));
        _map = new HashMap(typeCount);
        _typeToHandleMap = new HashMap(typeCount);

        int typeHandle;
        String typeName;

        for (int i = 0; i < typeCount; i++) {
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

            for (int j = 0; j < attributesCount; j++) {
                startIndex = endIndex + 1;
                endIndex = value.indexOf('"', endIndex + 1);
                attributeName = value.substring(startIndex, endIndex);

                attributes.add(attributeName);
            }

            typeMap.put("sequence", attributes);
            _map.put(typeHandle, typeMap);
            _typeToHandleMap.put((String)typeMap.get("name"), typeHandle);
        }
    }

    public String getTypeName(int handle) {
        return (String)(((HashMap)_map.get(handle)).get("name"));
    }

    public HashMap getAttributes(int handle) {
        return (HashMap)(((HashMap)_map.get(handle)).get("attributes"));
    }

    public HashMap getAttributes(String typeName) {
        int handle = getHandleId(typeName);
        if (handle != -1 && _map.containsKey(handle))
            return (HashMap)(((HashMap)_map.get(handle)).get("attributes"));

        return null;
    }

    public ArrayList getAttribList(int handle) {
        return (ArrayList)(((HashMap)_map.get(handle)).get("sequence"));
    }

    public int getHandleId(String typeName) {
        if (_typeToHandleMap.containsKey(typeName))
            return (Integer) _typeToHandleMap.get(typeName);

        return -1;
    }
}
