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

import java.util.Date;

public class JvDateFormatter extends java.text.SimpleDateFormat {

    public JvDateFormatter(String pattern) {
        super(pattern);
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo,
            java.text.FieldPosition pos) {
        StringBuffer str = super.format(date, toAppendTo, pos);
        str.insert(str.length() - 2, ':');
        return str;
    }

    @Override
    public Date parse(String text, java.text.ParsePosition pos) {
        StringBuffer str = new StringBuffer(text);
        if (str.charAt(str.length() - 3) == ':') {
            str.deleteCharAt(str.length() - 3);
        }
        return super.parse(str.toString(), pos);
    }
}
