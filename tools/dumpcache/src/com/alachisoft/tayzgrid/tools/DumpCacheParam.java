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

package com.alachisoft.tayzgrid.tools;

import com.alachisoft.tayzgrid.tools.common.ArgumentAttributeAnnontation;

public class DumpCacheParam extends com.alachisoft.tayzgrid.tools.common.CommandLineParamsBase {

    private String cacheId = "";
    private long keyCount = 1000;
    private String keyFilter = "";

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final void setCacheId(String value) {
        this.cacheId = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final String getCacheId() {
        return this.cacheId;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-k", fullNotation = "--key-count", appendText = "", defaultValue = "")
    public final void setKeyCount(long value) {
        this.keyCount = value;

    }

    @ArgumentAttributeAnnontation(shortNotation = "-k", fullNotation = "--key-count", appendText = "", defaultValue = "")
    public final long getKeyCount() {
        return this.keyCount;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-F", fullNotation = "--key-filter", appendText = "", defaultValue = "")
    public final void setKeyFilter(String value) {
        this.keyFilter = value;
    }
    
    @ArgumentAttributeAnnontation(shortNotation = "-F", fullNotation = "-key-filter", appendText = "", defaultValue = "")
    public final String getKeyFilter() {
        return this.keyFilter;
    }
}
