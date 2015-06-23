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

package com.alachisoft.tayzgrid.config.newdom;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class Log implements Cloneable, InternalCompactSerializable
{

    private boolean enabled = true;
    private boolean traceErrors = true;
    private boolean traceWarnings, traceNotices, traceDebug;
    private String location="";
    public Log()
    {
    }
    //[ConfigurationAttribute("enabled")]
    @ConfigurationAttributeAnnotation(value="enable-logs",appendText="" )//Changes for Dom enabled
    public final boolean getEnabled()
    {
        return enabled;
    }

    @ConfigurationAttributeAnnotation(value="enable-logs",appendText="")//Changes for Dom enabled
    public final void setEnabled(boolean value)
    {
        enabled = value;
    }

    @ConfigurationAttributeAnnotation(value="trace-errors",appendText="")
    public final boolean getTraceErrors()
    {
        return traceErrors;
    }

    @ConfigurationAttributeAnnotation(value="trace-errors",appendText="")
    public final void setTraceErrors(boolean value)
    {
        traceErrors = value;
    }

    @ConfigurationAttributeAnnotation(value="trace-notices",appendText="")
    public final boolean getTraceNotices()
    {
        return traceNotices;
    }

    @ConfigurationAttributeAnnotation(value="trace-notices",appendText="")
    public final void setTraceNotices(boolean value)
    {
        traceNotices = value;
    }

    @ConfigurationAttributeAnnotation(value="trace-warnings",appendText="")
    public final boolean getTraceWarnings()
    {
        return traceWarnings;
    }

    @ConfigurationAttributeAnnotation(value="trace-warnings",appendText="")
    public final void setTraceWarnings(boolean value)
    {
        traceWarnings = value;
    }

    @ConfigurationAttributeAnnotation(value="trace-debug",appendText="")
    public final boolean getTraceDebug()
    {
        return traceDebug;
    }

    @ConfigurationAttributeAnnotation(value="trace-debug",appendText="")
    public final void setTraceDebug(boolean value)
    {
        traceDebug = value;
    }
    
   
    @ConfigurationAttributeAnnotation(value="log-path",appendText="" )//Changes for Dom enabled
    public final String getLocation()
    {
        if(location == null)
            return "";
        else 
            return location;
    }

    @ConfigurationAttributeAnnotation(value="log-path",appendText="")//Changes for Dom enabled
    public final void setLocation(String value)
    {
        location = value;
    }
    @Override
    public final Object clone()
    {
        Log log = new Log();
        log.setEnabled(getEnabled());
        log.setTraceDebug(getTraceDebug());
        log.setTraceErrors(getTraceErrors());
        log.setTraceNotices(getTraceNotices());
        log.setTraceWarnings(getTraceWarnings());
        log.setLocation(getLocation());
        return log;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        enabled = reader.ReadBoolean();
        traceErrors = reader.ReadBoolean();
        traceWarnings = reader.ReadBoolean();
        traceNotices = reader.ReadBoolean();
        traceDebug = reader.ReadBoolean();
        location=(String) reader.ReadObject();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(enabled);
        writer.Write(traceErrors);
        writer.Write(traceWarnings);
        writer.Write(traceNotices);
        writer.Write(traceDebug);
        writer.WriteObject(location);
    }
}
