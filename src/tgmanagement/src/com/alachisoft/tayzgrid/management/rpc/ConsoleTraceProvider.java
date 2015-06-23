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


package com.alachisoft.tayzgrid.management.rpc;

import com.alachisoft.tayzgrid.common.communication.ITraceProvider;
import java.io.Console;

public class ConsoleTraceProvider implements ITraceProvider
    {

        public void TraceCritical(String module, String message)
        {
            WriteTraceToConsole("Critical", module, message);
        }

        public void TraceError(String module, String errorMessage)
        {
            WriteTraceToConsole("Error", module, errorMessage);
        }

        public void TraceWarning(String module, String warningMessage)
        {
            WriteTraceToConsole("Warning", module, warningMessage);
        }

        public void TraceDebug(String module, String debug)
        {
            WriteTraceToConsole("Debug", module, debug);
        }

        private void WriteTraceToConsole(String traceLevel, String module, String message)
        {
           String finalStr = "[ConsoleTrace] " + new java.util.Date().toString() + "      [" + traceLevel + "]       [" + module + "]      " + message;
            synchronized (this)
            {
            }
			
        }
}
