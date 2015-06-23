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

import com.alachisoft.tayzgrid.common.enums.EventType;
import org.apache.log4j.*;
import org.apache.log4j.helpers.DateLayout;
import org.apache.log4jna.nt.*;

public class JWindowsLogger implements IEventLogger {

    private static JLogger _logger = null;
    private static Logger _eventLogger = null;

    public void initialize() {
        try {
            
                BasicConfigurator.configure(new Win32EventLogAppender("TayzGridSvc", new PatternLayout()));
                _eventLogger = LogManager.getLogger(JWindowsLogger.class);
           
            
        } catch (Exception t) {
        }
    }

    public void setLogger(JLogger fileLogger) {
        _logger = fileLogger;
    }

    @Override
    public void LogEvent(String message, EventType msgType) {
        try {

            if (msgType == EventType.ERROR) {
                _logger.Error(message);
                _eventLogger.error(message);
            } else if (msgType == EventType.INFORMATION) {
                _logger.Info(message);
                _eventLogger.info(message);
            } else if (msgType == EventType.WARNING) {
                _logger.Warn(message);
                _eventLogger.warn(message);
            }

        } catch (Exception ex) {
        }

    }

}
