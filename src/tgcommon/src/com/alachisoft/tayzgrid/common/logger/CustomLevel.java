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
public class CustomLevel extends org.apache.log4j.Level
{

    /**
     * Creates a custom level to be used be the logger of log4j
     * @param level 50000 is for FATAL, for increased level use a higher value<br/>
     * <b>OFF_INT</b> = Integer.MAX_VALUE;<br/>
     * <b>FATAL_INT</b> = 50000;<br/>
     * <b>ERROR_INT</b> = 40000;<br/>
     * <b>WARN_INT</b>  = 30000;<br/>
     * <b>INFO_INT</b>  = 20000;<br/>
     * <b>DEBUG_INT</b> = 10000;
     * @param levelStr Name of the trace
     * @param syslogEquivalent 0, 3, 4, 6, 7 the lower the value the higher the priority
     */
    public CustomLevel(int level, String levelStr, int syslogEquivalent)
    {
        super(level, levelStr, syslogEquivalent);
    }

}
