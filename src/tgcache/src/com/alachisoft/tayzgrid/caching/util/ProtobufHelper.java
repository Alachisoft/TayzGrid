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

package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.caching.autoexpiration.AggregateExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.ExpirationHint;
import com.alachisoft.tayzgrid.caching.autoexpiration.FixedExpiration;
import com.alachisoft.tayzgrid.caching.autoexpiration.IdleExpiration;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.common.protobuf.NamedTagInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.NamedTagInfoProtocol.NamedTagInfo;
import com.alachisoft.tayzgrid.common.protobuf.QueryInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.QueryInfoProtocol.QueryInfo;
import com.alachisoft.tayzgrid.common.protobuf.TagInfoProtocol;
import com.alachisoft.tayzgrid.common.protobuf.TagInfoProtocol.TagInfo;
import com.alachisoft.tayzgrid.common.util.CacheKeyUtil;
import com.alachisoft.tayzgrid.common.util.JavaClrTypeMapping;

import com.alachisoft.tayzgrid.runtime.exceptions.ArgumentException;
import com.alachisoft.tayzgrid.runtime.exceptions.OperationFailedException;
import com.alachisoft.tayzgrid.runtime.util.HelperFxn;
import com.alachisoft.tayzgrid.runtime.util.NCDateTime;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

public final class ProtobufHelper
{

    public static java.util.HashMap GetHashtableFromQueryInfoObj(QueryInfo queryInfo)
    {
        java.util.HashMap queryInfoTable = new java.util.HashMap();
        if (queryInfo == null || queryInfo.getAttributesCount()<1)
        {
            return null;
        }
        java.util.ArrayList<String> queryInfoCopy = new ArrayList<String>();
        for (int i = 0; i < queryInfo.getAttributesCount(); i++)
        {
            queryInfoCopy.add(i, queryInfo.getAttributesList().get(i));
        }

        // putting back null values
        int nullIndex = 0;

        String[] queryAttribValues = new String[queryInfo.getAttributesList().size()];
        queryInfo.getAttributesList().toArray(queryAttribValues);
        for (String attrib : queryAttribValues)
        {
            if (attrib.equals("NCNULL"))
            {
                queryInfoCopy.add(nullIndex, null);
                queryInfoCopy.remove(nullIndex + 1);
            }
            nullIndex++;
        }
        java.util.ArrayList attributes = new java.util.ArrayList();
        for (String attrib : queryInfoCopy)
        {
            attributes.add(attrib);
        }

        queryInfoTable.put(queryInfo.getHandleId(), attributes);

        return queryInfoTable;
    }

    public static QueryInfo GetQueryInfoObj(java.util.HashMap queryInfoDic)
    {
        if (queryInfoDic == null)
        {
            return null;
        }
        if (queryInfoDic.isEmpty())
        {
            return null;
        }

        QueryInfoProtocol.QueryInfo.Builder queryInfo = QueryInfoProtocol.QueryInfo.newBuilder();
        Iterator queryInfoEnum = queryInfoDic.entrySet().iterator();
        while (queryInfoEnum.hasNext())
        {

            Map.Entry pair = (Map.Entry) queryInfoEnum.next();
            queryInfo.setHandleId((java.lang.Integer) pair.getKey());
            java.util.Iterator valuesEnum = ((java.util.ArrayList) pair.getValue()).iterator();

            while (valuesEnum.hasNext())
            {
                String value = null;
                if (valuesEnum.next() != null)
                {
                    if (valuesEnum.next() instanceof java.util.Date)
                    {
                        try
                        {
                            value = new Long(new NCDateTime((java.util.Date) valuesEnum.next()).getTicks()).toString();
                        }
                        catch (ArgumentException argumentException)
                        {
                        }
                    }
                    else
                    {
                        value = valuesEnum.next().toString();
                    }
                }
                else
                { // we need to send null values too as a special placeholder
                    value = "NCNULL";
                }
                queryInfo.getAttributesList().add(value);
            }
        }
        return queryInfo.build();
    }

    public static java.util.HashMap GetHashtableFromTagInfoObj(TagInfo tagInfo)
    {
        java.util.HashMap tagInfoTable = new java.util.HashMap();
        if (tagInfo == null || tagInfo.getTagsCount()<1)
        {
            return null;
        }

        tagInfoTable.put("type", tagInfo.getType());
        tagInfoTable.put("tags-list", new java.util.ArrayList(tagInfo.getTagsList()));

        return tagInfoTable;
    }

    public static TagInfo GetTagInfoObj(java.util.HashMap tagInfoDic)
    {
        if (tagInfoDic == null)
        {
            return null;
        }
        if (tagInfoDic.isEmpty())
        {
            return null;
        }

        TagInfoProtocol.TagInfo.Builder build = TagInfoProtocol.TagInfo.newBuilder();
        build.setType((String) tagInfoDic.get("type"));
        java.util.Iterator tagsEnum = ((java.util.ArrayList) tagInfoDic.get("tags-list")).iterator();
        while (tagsEnum.hasNext())
        {
            Object obj = tagsEnum.next();
            if (obj != null)
            {
                build.addTags(obj.toString());
            }
            else
            {
                build.addTags(null);
            }
        }

        return build.build();
    }

    public static NamedTagInfo GetNamedTagInfoObj(java.util.HashMap namedTagInfoDic, boolean isDotNetClient) throws ArgumentException
    {
        NamedTagInfoProtocol.NamedTagInfo.Builder build = NamedTagInfoProtocol.NamedTagInfo.newBuilder();
        build.setType((String) namedTagInfoDic.get("type"));

        HashMap hash = (java.util.HashMap) namedTagInfoDic.get("named-tags-list");
        Iterator ide = hash.entrySet().iterator();
        Map.Entry entry;
        while (ide.hasNext())
        {
            entry = (Map.Entry) ide.next();
            build.addNames(entry.getKey().toString());
            if (!isDotNetClient)
            {
                build.addTypes(entry.getValue().getClass().getName());
            }
            else
            {
                build.addTypes(JavaClrTypeMapping.JavaToClr(entry.getValue().getClass().getName()));
            }
            if (entry.getValue().getClass() == java.util.Date.class)
            {
                build.addVals(Long.toString((new NCDateTime((Date) entry.getValue())).getTicks()));
            }
            else
            {
                build.addVals(entry.getValue().toString());
            }
        }

        return build.build();
    }

    public static java.util.HashMap GetHashtableFromNamedTagInfoObjFromDotNet(NamedTagInfo tagInfo) throws ClassNotFoundException
    {
        if (tagInfo == null || tagInfo.getNamesCount()<1)
        {
            return null;
        }
        
        NamedTagInfoProtocol.NamedTagInfo.Builder tag = tagInfo.toBuilder();        
        java.util.List<String> typesList=new ArrayList<String>();
        for (int i = 0; i < tagInfo.getNamesList().size(); i++)
        {
            typesList.add(i, JavaClrTypeMapping.ClrToJava(tagInfo.getTypesList().get(i)));
            
        }
        return GetHashtableFromNamedTagInfoObjDotNet(tagInfo.getType(),tagInfo.getNamesList(),typesList,tagInfo.getValsList());

    }
    
    
    //Dirty Code Due to Some Exception 
    
        private static java.util.HashMap GetHashtableFromNamedTagInfoObjDotNet(String type,java.util.List<java.lang.String> namesList,java.util.List<java.lang.String> typesList,java.util.List<java.lang.String> valuesList) throws ClassNotFoundException
    {
        java.util.HashMap tagInfoTable = new java.util.HashMap();
        tagInfoTable.put("type", type);

        java.util.HashMap tagList = new java.util.HashMap();
        for (int i = 0; i < namesList.size(); i++)
        {
            Object obj = null;
            java.lang.Class t1 = java.lang.Class.forName(typesList.get(i));
            if (t1 == java.util.Date.class)
            {

                obj = HelperFxn.getDateFromTicks((Long.parseLong(valuesList.get(i))));
            }
            else
            {
                obj = new ConfigurationBuilder().ConvertToPrimitive(t1, valuesList.get(i), "");
            }

            tagList.put(namesList.get(i), obj);
        }

        tagInfoTable.put("named-tags-list", tagList);

        return tagInfoTable;
    }
    

    private static java.util.HashMap GetHashtableFromNamedTagInfoObj(NamedTagInfo tagInfo) throws ClassNotFoundException
    {
        java.util.HashMap tagInfoTable = new java.util.HashMap();
        tagInfoTable.put("type", tagInfo.getType());

        java.util.HashMap tagList = new java.util.HashMap();
        for (int i = 0; i < tagInfo.getNamesList().size(); i++)
        {
            Object obj = null;
            java.lang.Class t1 = java.lang.Class.forName(tagInfo.getTypesList().get(i));
            if (t1 == java.util.Date.class)
            {

                obj = HelperFxn.getDateFromTicks((Long.parseLong(tagInfo.getValsList().get(i))));
            }
            else
            {
                obj = new ConfigurationBuilder().ConvertToPrimitive(t1, tagInfo.getValsList().get(i), "");
            }

            tagList.put(tagInfo.getNamesList().get(i), obj);
        }

        tagInfoTable.put("named-tags-list", tagList);

        return tagInfoTable;
    }

    public static java.util.HashMap GetHashtableFromNamedTagInfoObjFromJava(NamedTagInfo tagInfo) throws ClassNotFoundException
    {
        if (tagInfo == null || tagInfo.getNamesCount()<1)
        {
            return null;
        }
        return GetHashtableFromNamedTagInfoObj(tagInfo);
    }

    public static ExpirationHint GetExpirationHintObj( long absoluteExpiration, long slidingExpiration, boolean resyncOnExpiration, String serializationContext) throws OperationFailedException, IOException, ClassNotFoundException
    {
        ExpirationHint hint = null;
        //We expect Web.Cache to send in UTC DateTime, AbsoluteExpiration is dealt in UTC
        if (absoluteExpiration != 0 && absoluteExpiration != Long.MAX_VALUE)
        {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.clear();
            cal.set(Calendar.MILLISECOND, 0);
            NCDateTime ncd = new NCDateTime(absoluteExpiration);
            cal.setTime(ncd.getLocalizedDate());

            hint = new FixedExpiration(cal.getTime(), absoluteExpiration);
        }
        if (slidingExpiration != 0)
        {
            hint = new IdleExpiration(new TimeSpan(slidingExpiration));
        }

       
        if (hint != null )
        {
            hint = new AggregateExpirationHint(hint);
        }
       

        if (hint != null && resyncOnExpiration)
        {
            hint.SetBit(ExpirationHint.NEEDS_RESYNC);
        }

        return hint;
    }

    public static ExpirationHint GetExpirationHintObj( boolean resyncOnExpiration, String serializationContext) throws OperationFailedException, IOException, ClassNotFoundException
    {
        AggregateExpirationHint hints = new AggregateExpirationHint();

        
        if (resyncOnExpiration)
        {
            hints.SetBit(ExpirationHint.NEEDS_RESYNC);
        }

        ExpirationHint[] expHints = hints.getHints();

        if (expHints.length == 0)
        {
            return null;
        }

        if (expHints.length == 1)
        {
            return expHints[0];
        }

        return hints;
    }

}
