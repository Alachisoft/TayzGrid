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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.alachisoft.tayzgrid.utility.common;

import java.util.Date;

/**
 *
 * 
 */
public class DataTypeUtil {

public static int getType(Object obj) {
        if(obj==null)
        return 0;
        if (obj instanceof Integer) {
            return 1;
        } else if (obj instanceof Long) {
            return 2;
        } else if (obj instanceof Float) {
            return 3;
        } else if (obj instanceof Double) {
            return 4;
        } else if (obj instanceof String) {
            return 5;
        } else if (obj instanceof Character) {
            return 6;
        } else if (obj instanceof Boolean) {
            return 7;
        } else if (obj instanceof Date) {
            return 8;
        } else {
            return 0;
        }
    }
}
