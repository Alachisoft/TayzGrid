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

// ===============================================================================
// Alachisoft (R) TayzGrid Sample Code
// TayzGrid Product Class used by samples
// ===============================================================================
// Copyright © Alachisoft.  All rights reserved.
// THIS CODE AND INFORMATION IS PROVIDED "AS IS" WITHOUT WARRANTY
// OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT
// LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
// FITNESS FOR A PARTICULAR PURPOSE.
// ===============================================================================
package cachekeygenerator;

import java.lang.reflect.Method;
import org.springframework.cache.interceptor.KeyGenerator;

public class TayzGridEntityKeyGenerator implements KeyGenerator{

    public Object generate(Object target, Method method, Object... params) {
        String key="Entity";
        for(Object param: params)
        {
            key=key+":"+param.hashCode();
        }
        return key;        
    }
    
}
