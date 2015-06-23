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
package com.alachisoft.tayzgrid.common;

public class CacheConfigParams {
    
    
/**
 * Custom class for populating the Cache Config Params
 * 
 */
    private boolean _isReadThru = false;
    private boolean  _isWriteThru = false;
    private boolean  _isStasticsEnabled = false;
    
    public CacheConfigParams () {
        
    }
    public boolean getIsReadThru() {
        return _isReadThru;
    }
    public void setIsReadThru(boolean isReadThru) {
        _isReadThru = isReadThru;
    }
    public boolean getIsWriteThru() {
        return _isWriteThru;
    }
    public void setIsWriteThru(boolean isWriteThru) {
        _isWriteThru = isWriteThru;
    }
    
    public boolean getIsStatisticsEnabled() {
        return _isStasticsEnabled;
    }
    public void  setIsStatisticsEnabled(boolean  isStatisticsEnabled) {
        _isStasticsEnabled = isStatisticsEnabled;
    }
    
}
