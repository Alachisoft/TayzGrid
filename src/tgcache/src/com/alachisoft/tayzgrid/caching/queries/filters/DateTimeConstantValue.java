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

package com.alachisoft.tayzgrid.caching.queries.filters;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class DateTimeConstantValue extends ConstantValue {

    public DateTimeConstantValue() {
        super.constant = new java.util.Date();
    }

    public DateTimeConstantValue(String lexeme) {
        /* 
         * It has been decided that we will accept date time in the following format
         * DD-MM-YYYY HH:MM:SS:MS; if date is given in any other format; exception will be thrown
         */

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            super.constant = format.parse(lexeme);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Can not parse the given date. Provide the date in YYYY-MM-dd hh:mm:ss format");
        }
    }
}
