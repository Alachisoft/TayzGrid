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

package com.alachisoft.tayzgrid.common.logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class JFormatter extends Formatter {

    private static final DateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss,SSS");

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append(df.format(new Date(record.getMillis()))).append("     ");


        builder.append(String.format("%-45s", record.getLoggerName()));
        builder.append(String.format("%-35s", record.getThreadID()));
        builder.append(String.format("%-20s", record.getLevel()));
        
        builder.append(formatMessage(record));
        builder.append("\r\n");
        return builder.toString();
    }
}
