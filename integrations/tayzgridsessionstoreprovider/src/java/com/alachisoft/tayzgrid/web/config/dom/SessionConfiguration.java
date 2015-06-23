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

package com.alachisoft.tayzgrid.web.config.dom;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationRootAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import java.io.Serializable;

@ConfigurationRootAnnotation(value = "servlet-session-config")
public class SessionConfiguration implements Serializable {

    private Log _log;
    private Locking _locking;
    private Cache _cache;
    
    @ConfigurationSectionAnnotation(value = "log")
    public final Log getLogFile() {
        return _log;
    }

    @ConfigurationSectionAnnotation(value = "log")
    public final void setLogFile(Log value) {
        _log = value;
    }

    @ConfigurationSectionAnnotation(value = "cache")
    public final Cache getCache() {
        return _cache; 
    }

    @ConfigurationSectionAnnotation(value = "cache")
    public final void setCache(Cache value) {
        _cache = value;
    }

    @ConfigurationSectionAnnotation(value = "locking")
    public final Locking getLocking() {
        return _locking;
    }

    @ConfigurationSectionAnnotation(value = "locking")
    public final void setLocking(Locking value) {
        _locking = value;
    }
}
