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

package com.alachisoft.tayzgrid.config.newdom;

public class Assembly implements Cloneable, java.io.Serializable
{

    private String id, name;
    private java.util.HashMap<String, Type> typesMap;

    public Assembly()
    {
    }

    public final String getID()
    {
        return id;
    }

    public final void setID(String value)
    {
        id = value;
    }

    public final String getName()
    {
        return name;
    }

    public final void setName(String value)
    {
        name = value;
    }

    public final Type[] getTypes()
    {
        if (typesMap == null)
        {
            return null;
        }

        Type[] types = new Type[typesMap.size()];
        typesMap.values().toArray(types);
        return types;
    }

    public final void setTypes(Type[] value)
    {
        if (typesMap == null)
        {
            typesMap = new java.util.HashMap<String, Type>();
        }

        typesMap.clear();
        for (Type type : value)
        {
            typesMap.put(type.getName(), type);
        }
    }

    public final java.util.HashMap<String, Type> getTypesMap()
    {
        return typesMap;
    }

    public final void setTypesMap(java.util.HashMap<String, Type> value)
    {
        typesMap = value;
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Assembly)
        {
            return ((Assembly) obj).name.toLowerCase().equals(name.toLowerCase());
        }

        return false;
    }

    public final Object clone() throws CloneNotSupportedException
    {
        Assembly assembly = new Assembly();
        assembly.setID(getID() != null ? new String(getID()) : null);
        assembly.setName(getName() != null ? new String(getName()) : null);
        assembly.setTypes(getTypes() != null ? (Type[]) getTypes().clone() : null);
        return assembly;
    }

    public static Assembly GetExecutingAssembly() throws Exception
    {
        throw new Exception("The method or operation is not implemented.");
    }

    public final Object GetManifestResourceStream(String p)throws Exception
    {
        throw new Exception("The method or operation is not implemented.");
    }
}
