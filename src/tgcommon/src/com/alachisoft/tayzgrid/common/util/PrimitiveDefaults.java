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

package com.alachisoft.tayzgrid.common.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


/**
 *
 * @author 
 */
public class PrimitiveDefaults {
   
   private static final Map<Class<?>, Object> Values;

   static {
    Map<Class<?>, Object> map = new HashMap<Class<?>, Object>();
    map.put( Boolean.class, false);
    map.put( Byte.class, (byte) 0);
    map.put( Short.class, (short) 0);
    map.put( Integer.class, 0);
    map.put( Long.class, 0L);
    map.put( Float.class, 0f);
    map.put( Double.class, 0d);
    map.put( BigInteger.class, BigInteger.ZERO);
    map.put( BigDecimal.class, BigDecimal.ZERO);
    map.put( String.class, "");
    map.put( Date.class, new Date());
    map.put( HashSet.class, new HashSet());
    Values = Collections.unmodifiableMap(map);
   }
    
    public static <T> T getDefault(Class<T> type) {
        return (T)Values.get(type);
    }
   
   
}
