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

package com.alachisoft.tayzgrid.runtime.caching;

import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.util.Date;

public class ProviderCacheItem {
    private Object value;
    private Date absoluteExpiration;
    private String group;
    private CacheItemPriority itemPriority=CacheItemPriority.Default;
    private boolean resyncItemOnExpiration;
    private String resyncProviderName;
    private TimeSpan slidingExpiration;
    private String subGroup;
    private Tag[] tag;
    private NamedTagsDictionary namedTagsDictionary;



    public ProviderCacheItem()
    {
    }

    public ProviderCacheItem(Object value)
    {
        this.value=value;
    }

    public Date getAbsoluteExpiration() {
        return absoluteExpiration;
    }

    public void setAbsoluteExpiration(Date absoluteExpiration) {
        this.absoluteExpiration = absoluteExpiration;
    }

   

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public CacheItemPriority getItemPriority() {
        return itemPriority;
    }

    public void setItemPriority(CacheItemPriority itemPriority) {
        this.itemPriority = itemPriority;
    }

    public boolean isResyncItemOnExpiration() {
        return resyncItemOnExpiration;
    }

    public void setResyncItemOnExpiration(boolean resyncItemOnExpiration) {
        this.resyncItemOnExpiration = resyncItemOnExpiration;
    }

    public String getResyncProviderName() {
        return resyncProviderName;
    }

    public void setResyncProviderName(String resyncProviderName) {
        this.resyncProviderName = resyncProviderName;
    }

    public TimeSpan getSlidingExpiration() {
        return slidingExpiration;
    }

    public void setSlidingExpiration(TimeSpan slidingExpiration) {
        this.slidingExpiration = slidingExpiration;
    }

    public String getSubGroup() {
        return subGroup;
    }

    public void setSubGroup(String subGroup) {
        this.subGroup = subGroup;
    }

    public Tag[] getTags() {
        return tag;
    }

    public void setTags(Tag[] tag) {
        this.tag = tag;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
           this.value = value;
    }
    public NamedTagsDictionary getNamedTags() {
        return namedTagsDictionary;
    }

    public void setNamedTags(NamedTagsDictionary namedTagsDictionary) {
        this.namedTagsDictionary = namedTagsDictionary;
    }
}
