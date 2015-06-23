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

package com.alachisoft.tayzgrid.caching.cacheloader;

import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.caching.NamedTagsDictionary;
import com.alachisoft.tayzgrid.runtime.caching.Tag;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.serialization.util.TypeInfoMap;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Calendar;

public class CacheLoaderUtil
{

    public static int EvaluateExpirationParameters(java.util.Date absoluteExpiration, TimeSpan slidingExpiration)throws ArgumentException
    {
         if ((absoluteExpiration==null) &&(slidingExpiration==null))
        {
            return 2;
        }
        if (absoluteExpiration==null)
        {
            if (((java.lang.Comparable)slidingExpiration).compareTo(TimeSpan.ZERO) < 0)
            {
                throw new IllegalArgumentException("slidingExpiration");
            }
            Calendar date = Calendar.getInstance();
            date.add( Calendar.YEAR, 1 );
            if (((java.lang.Comparable)slidingExpiration).compareTo(TimeSpan.Subtract(date.getTime(), Calendar.getInstance().getTime()))>=0)
            {
                throw new IllegalArgumentException("slidingExpiration");

            }

            return 0;
        }

        if (slidingExpiration==null)
        {
            return 1;
        }

        throw new IllegalArgumentException("You cannot set both sliding and absolute expirations on the same cache item.");
    }

    public static void EvaluateTagsParameters(java.util.HashMap queryInfo, String group)
    {
        if (queryInfo != null)
        {
            if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(group) && queryInfo.get("tag-info") != null)
            {
                throw new IllegalArgumentException("You cannot set both groups and tags on the same cache item.");
            }
        }
    }

    public static java.util.HashMap GetJavaTagInfo(String fullName, Tag[] tags)
    {
        if (tags == null)
        {
            return null;
        }

        java.util.HashMap tagInfo = new java.util.HashMap();
        java.util.ArrayList tagsList = new java.util.ArrayList();
        for (Tag tag : tags)
        {
            if (tag == null)
            {
                throw new IllegalArgumentException("Tag");
            }
            else if (tag.getTagName() != null)
            {
                tagsList.add(tag.getTagName());
            }

        }

        tagInfo.put("type", fullName);
        tagInfo.put("tags-list", tagsList);

        return tagInfo;
    }

    public static java.util.HashMap GetTagInfo(Object value, Tag[] tags)
    {
        if (tags == null)
        {
            return null;
        }

        java.util.HashMap tagInfo = new java.util.HashMap();
        java.util.ArrayList tagsList = new java.util.ArrayList();
        for (Tag tag : tags)
        {
            if (tag == null)
            {
                throw new IllegalArgumentException("Tag cannot be null");
            }
            else if (tag.getTagName() != null)
            {
                tagsList.add(tag.getTagName());
            }

        }
        tagInfo.put("type", value.getClass().getName());
        tagInfo.put("tags-list", tagsList);

        return tagInfo;
    }

    public static java.util.HashMap GetJavaNamedTagsInfo(String fullName, NamedTagsDictionary namedTags, TypeInfoMap typeMap)throws Exception
    {
        CheckJavaDuplicateIndexName(fullName, namedTags, typeMap);

        if (namedTags == null || namedTags.getCount() == 0)
        {
            return null;
        }

        java.util.HashMap tagInfo = new java.util.HashMap();
        java.util.HashMap tagsList = new java.util.HashMap();

        Iterator ide = namedTags.getIterator();
        while(ide.hasNext())
        {
            Map.Entry nameValue = (Map.Entry) ide.next();
            if (nameValue.getValue() == null)
            {
                throw new IllegalArgumentException("Named Tag value cannot be null");
            }

            tagsList.put(nameValue.getKey(), nameValue.getValue());
        }

        String typeName = fullName;
        typeName = typeName.replace("+", ".");

        tagInfo.put("type", typeName);
        tagInfo.put("named-tags-list", tagsList);

        return tagInfo;
    }

    private static void CheckJavaDuplicateIndexName(String fullName, NamedTagsDictionary namedTags, TypeInfoMap typeMap) throws Exception
    {
        if (namedTags == null || typeMap == null)
        {
            return;
        }

        String typeName = fullName;
        typeName = typeName.replace("+", ".");

        int handleId = typeMap.getHandleId(typeName);
        if (handleId != -1)
        {
            java.util.ArrayList attributes = typeMap.getAttribList(handleId);
            for (Object name : attributes)
            {
                if (namedTags.contains(name.toString()))
                { 
                    //@UH whether this should be case insensitive
                    throw new Exception("Key in named tags conflicts with the indexed attribute name of the specified object.");
                }
            }
        }
    }

    public static java.util.HashMap GetNamedTagsInfo(Object value, NamedTagsDictionary namedTags, TypeInfoMap typeMap)throws Exception
    {
        CheckDuplicateIndexName(value, namedTags, typeMap);

        if (namedTags == null || namedTags.getCount() == 0)
        {
            return null;
        }

        java.util.HashMap tagInfo = new java.util.HashMap();
        java.util.HashMap tagsList = new java.util.HashMap();
        Iterator ide = namedTags.getIterator();
        while(ide.hasNext())
        {
            Map.Entry nameValue = (Map.Entry) ide.next();
            if (nameValue.getValue() == null)
            {
                throw new IllegalArgumentException("Named Tag value cannot be null");
            }

            tagsList.put(nameValue.getKey(), nameValue.getValue());
        }
        String typeName = value.getClass().getName();
        typeName = typeName.replace("+", ".");

        tagInfo.put("type", typeName);
        tagInfo.put("named-tags-list", tagsList);

        return tagInfo;
    }

    private static void CheckDuplicateIndexName(Object value, NamedTagsDictionary namedTags, TypeInfoMap typeMap)throws Exception
    {
        if (namedTags == null || value == null || typeMap == null)
        {
            return;
        }
        String typeName = value.getClass().getName();
        typeName = typeName.replace("+", ".");

        int handleId = typeMap.getHandleId(typeName);
        if (handleId != -1)
        {
            java.util.ArrayList attributes = typeMap.getAttribList(handleId);
            for (Object name : attributes)
            {
                if (namedTags.contains(name.toString()))
                { 
                    //@UH whether this should be case insensitive
                    throw new Exception("Key in named tags conflicts with the indexed attribute name of the specified object.");
                }
            }
        }
    }

    public static java.util.HashMap GetQueryInfo(Object value, TypeInfoMap typeMap)
    {
        java.util.HashMap queryInfo = null;

        if (typeMap == null)
        {
            return null;
        }

        try
        {
            int handleId = typeMap.getHandleId(value.getClass().getName());
            if (handleId != -1)
            {
                queryInfo = new java.util.HashMap();
                java.lang.Class valType = null; //Asif Imam (Cattering Case-InSensetive string comparisons.
                java.util.ArrayList attribValues = new java.util.ArrayList();
                java.util.ArrayList attributes = typeMap.getAttribList(handleId);
                for (int i = 0; i < attributes.size(); i++)
                {
                    Object propertyAttrib = null;
                    if (propertyAttrib != null)
                    {
                    }
                    else
                    {
                        java.lang.reflect.Field fieldAttrib = value.getClass().getField((String) attributes.get(i));
                        if (fieldAttrib != null)
                        {
                            Object attribValue = fieldAttrib.get(value);

                            if (attribValue instanceof String)
                            { //add all strings as lower case in index tree
                                attribValue = (Object) (attribValue.toString());//.toLowerCase();
                            }
                            if (attribValue instanceof Date)
                            { //add all DateTime as ticks
                                NCDateTime ncd = new NCDateTime((Date)attribValue);
                                attribValue = Long.toString(ncd.getTicks());
                            }
                            attribValues.add(attribValue);
                        }
                        else
                        {
                            throw new Exception("Unable extracting query information from user object.");
                        }
                    }
                }
                queryInfo.put(handleId, attribValues);
            }
        }
        catch (Exception e)
        {
        }
        return queryInfo;
    }
}
