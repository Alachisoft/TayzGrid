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

package com.alachisoft.tayzgrid.caching.queries;

import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.standard.io.TypeSurrogateSelectorImpl;
import java.util.Iterator;
import java.util.Map;

// This class holds information about the portable types and mapped attributes. When query indices are being created
// the type is looked up in the portable types, if found then the index is created based on the compact ID of the type
// rather than it's name so that both java and .net types result in the same query index. For attributes the name lookup
// results in the order of the attribute so that it can be shared among .net and java type for indexing.
public final class PortableTypes {

    private static java.util.HashMap _cmptKnownTypesforJavaAndNet = new java.util.HashMap(); // Dot Net and java compact type hashtable
    private static java.util.HashMap _cmptKnownAttributesforJavaAndNet = new java.util.HashMap(); // Dot Net and java compact type attributes hashtable
    private static java.util.HashMap _cmptTypes = new java.util.HashMap(); //HashMap of convertable types

    public static void Initialize(java.util.HashMap _cmptKnownTypes, String cacheContext) {
        java.util.HashMap knownTypes = new java.util.HashMap();
        java.util.HashMap knownAttributes = new java.util.HashMap();
        java.util.HashMap compactTypes = new java.util.HashMap();
        TypeSurrogateSelectorImpl impl = TypeSurrogateSelectorImpl.getDefault();
        //Handling of $Text$ datatype for both java and .net clients
        SerializationSurrogate surrogate = impl.getSurrogateForType(String.class, null);
        knownTypes.put("System.String", (new Short(surrogate.getClassHandle())).toString());
        knownTypes.put("java.lang.String", (new Short(surrogate.getClassHandle())).toString());

        surrogate = impl.getSurrogateForType(Character.class, null);
        knownTypes.put("System.Char", (new Short(surrogate.getClassHandle())).toString());
        knownTypes.put("java.lang.Character", (new Short(surrogate.getClassHandle())).toString());

        surrogate = impl.getSurrogateForType(Boolean.class, null);
        knownTypes.put("System.Boolean", (new Short(surrogate.getClassHandle())).toString());
        knownTypes.put("java.lang.Boolean", (new Short(surrogate.getClassHandle())).toString());

        surrogate = impl.getSurrogateForType(Byte.class, null);
        knownTypes.put("System.Byte", (new Short(surrogate.getClassHandle())).toString());
        knownTypes.put("java.lang.Byte", (new Short(surrogate.getClassHandle())).toString());

        surrogate = impl.getSurrogateForType(Short.class, null);
        knownTypes.put("System.Int16", (new Short(surrogate.getClassHandle())).toString());
        knownTypes.put("java.lang.Short", (new Short(surrogate.getClassHandle())).toString());

        surrogate = impl.getSurrogateForType(Integer.class, null);
        knownTypes.put("System.Int32", (new Short(surrogate.getClassHandle())).toString());
        knownTypes.put("java.lang.Integer", (new Short(surrogate.getClassHandle())).toString());

        surrogate = impl.getSurrogateForType(Long.class, null);
        knownTypes.put("System.Int64", (new Short(surrogate.getClassHandle())).toString());
        knownTypes.put("java.lang.Long", (new Short(surrogate.getClassHandle())).toString());

        surrogate = impl.getSurrogateForType(Float.class, null);
        knownTypes.put("System.Single", (new Short(surrogate.getClassHandle())).toString());
        knownTypes.put("java.lang.Float", (new Short(surrogate.getClassHandle())).toString());

        surrogate = impl.getSurrogateForType(Double.class, null);
        knownTypes.put("System.Double", (new Short(surrogate.getClassHandle())).toString());
        knownTypes.put("java.lang.Double", (new Short(surrogate.getClassHandle())).toString());

        if (_cmptKnownTypes != null) {
            Iterator ide = _cmptKnownTypes.entrySet().iterator();
            while (ide.hasNext()) {
                Map.Entry current = (Map.Entry) ide.next();
                java.util.HashMap compactType = (java.util.HashMap) current.getValue();
                if ((Boolean) (compactType.get("portable"))) {
                    java.util.ArrayList<String> typeNames = new java.util.ArrayList<String>();

                    java.util.HashMap classes = (java.util.HashMap) compactType.get("known-classes");
                    Iterator ide2 = classes.entrySet().iterator();
                    while (ide2.hasNext()) {
                        Map.Entry pair = (Map.Entry) ide2.next();
                        java.util.ArrayList attribs = new java.util.ArrayList();
                        java.util.HashMap typeInfo = (java.util.HashMap) pair.getValue();

                        typeNames.add(typeInfo.get("name").toString());

                        if (typeInfo.get("attribute") != null) {
                            for (Iterator it = ((java.util.HashMap) typeInfo.get("attribute")).values().iterator(); it.hasNext();) {
                                java.util.HashMap attrib = (java.util.HashMap) it.next();
                                if (!attrib.get("order").toString().equals("-1")) {
                                    attribs.add(attrib);
                                }
                            }
                        }
                        knownAttributes.put(typeInfo.get("name"), attribs);
                    }

                    compactTypes.put(typeNames.get(0), typeNames.get(1));
                }
            }
        }
        if (knownTypes.size() > 0 && !_cmptKnownTypesforJavaAndNet.containsKey(cacheContext)) {
            _cmptKnownTypesforJavaAndNet.put(cacheContext, knownTypes);
        }
        if (knownAttributes.size() > 0 && !_cmptKnownAttributesforJavaAndNet.containsKey(cacheContext)) {
            _cmptKnownAttributesforJavaAndNet.put(cacheContext, knownAttributes);
        }
        if (compactTypes.size() > 0 && !_cmptTypes.containsKey(cacheContext)) {
            _cmptTypes.put(cacheContext, compactTypes);
        }

    }

    public static String GetPortableType(String type, String cacheContext) {
        java.util.HashMap knownTypes = (java.util.HashMap) _cmptKnownTypesforJavaAndNet.get(cacheContext);
        if (knownTypes != null) {
            if (knownTypes.containsKey(type)) {
                return (String) knownTypes.get(type);
            }
        }
        return type;
    }

    public static String GetPortableAttribute(String type, String attribute, String cacheContext) {
        java.util.HashMap knownAttributes = (java.util.HashMap) _cmptKnownAttributesforJavaAndNet.get(cacheContext);
        if (knownAttributes != null) {
            if (knownAttributes.containsKey(type)) {
                java.util.ArrayList list = (java.util.ArrayList) knownAttributes.get(type);
                if (list != null && list.size() > 0) {
                    java.util.ArrayList returnList = new java.util.ArrayList();
                    for (Iterator it = list.iterator(); it.hasNext();) {
                        java.util.HashMap attrib = (java.util.HashMap) it.next();
                        if (attrib.get("name").toString().equals(attribute)) {
                            return (String) attrib.get("order");
                        }
                    }
                }
            }
        }
        return attribute;
    }

    public static String GetConvertableType(String type, String cacheContext) {
        String convertableType = null;
        java.util.HashMap compactTypes = (java.util.HashMap) _cmptTypes.get(cacheContext);
        if (compactTypes != null) {
            if (compactTypes.containsKey(type)) {
                convertableType = (String) ((compactTypes.get(type) instanceof String) ? compactTypes.get(type) : null);
            } else {
                if (compactTypes.containsValue(type)) {
                    Iterator compactTypesIterator = compactTypes.entrySet().iterator();
                    while (compactTypesIterator.hasNext()) {
                        Map.Entry current = (Map.Entry) compactTypesIterator.next();
                        if (current.getValue().equals(type)) {
                            convertableType = (String) ((current.getKey() instanceof String) ? current.getKey() : null);
                            break;
                        }
                    }
                }
            }
        }
        return convertableType;
    }

    public static void dispose(String cacheContext) {
        if (cacheContext != null) {
            if (_cmptKnownTypesforJavaAndNet != null && _cmptKnownTypesforJavaAndNet.size() > 0) {
                _cmptKnownTypesforJavaAndNet.remove(cacheContext);
            }
            if (_cmptKnownAttributesforJavaAndNet != null && _cmptKnownAttributesforJavaAndNet.size() > 0) {
                _cmptKnownAttributesforJavaAndNet.remove(cacheContext);
            }
            if (_cmptTypes != null && _cmptTypes.size() > 0) {
                _cmptTypes.remove(cacheContext);
            }
        }
    }
}