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


public class JLinuxLogger implements IEventLogger
{
    private static JLogger _logger = null;
    
    public void initialize()
    {
        
    }
     public void setLogger(JLogger fileLogger)
     {
         _logger= fileLogger;
     }
    
    @Override
    public void LogEvent(String message,EventType msgType)
    {
        try
        {
            if(msgType==EventType.ERROR)
            {
                _logger.Error(message);
            }
            else if(msgType==EventType.INFORMATION)
            {
                _logger.Info(message);
            }
            else if(msgType==EventType.WARNING)
            {  
                _logger.Warn(message);
            }
        }
        catch(Exception ex)
        {
        }
    }
    
    
}
