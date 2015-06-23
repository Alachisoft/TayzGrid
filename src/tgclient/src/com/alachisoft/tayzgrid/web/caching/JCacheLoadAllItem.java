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
package com.alachisoft.tayzgrid.web.caching;

import javax.cache.integration.CompletionListener;



public class JCacheLoadAllItem {
    private boolean _replaceExistingValues = false;
    private CompletionListener _completionListener = null;
    private short _completionListenerId = -1;

    public JCacheLoadAllItem() {
    }
    public void setReplaceExistingValues (boolean value) {
        _replaceExistingValues = value;
    }
    public boolean getReplaceExistingValues() {
        return _replaceExistingValues;
    }
    public void setCompletionListener(CompletionListener listener) {
        _completionListener = listener;
    }
    public CompletionListener getCompletionListener() {
        return _completionListener;
    }
    
   public void setCompletionListenerId (short value) {
       _completionListenerId = value;
   }
   public short getCompletionListenerId() {
       return _completionListenerId;
   }
    
}
