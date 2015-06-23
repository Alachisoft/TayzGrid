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



public class AutoLoadBalancing implements Cloneable, InternalCompactSerializable
{

    private boolean enabled = true;
    private int autoBalancingThreshold = 60;
    private int autoBalancingInterval = 30;

    public AutoLoadBalancing()
    {
    }

    @ConfigurationAttributeAnnotation(value="enabled",appendText="")
    public final boolean getEnabled()
    {
        return enabled;
    }

    @ConfigurationAttributeAnnotation(value="enabled",appendText="")
    public final void setEnabled(boolean value)
    {
        enabled = value;
    }

    @ConfigurationAttributeAnnotation(value="auto-balancing-threshold", appendText="%")
    public final int getThreshold()
    {
        return autoBalancingThreshold;
    }

    @ConfigurationAttributeAnnotation(value="auto-balancing-threshold",appendText="%")
    public final void setThreshold(int value)
    {
        autoBalancingThreshold = value;
    }

    @ConfigurationAttributeAnnotation(value="auto-balancing-interval",appendText="sec")
    public final int getInterval()
    {
        return autoBalancingInterval;
    }

    @ConfigurationAttributeAnnotation(value="auto-balancing-interval",appendText="sec")
    public final void setInterval(int value)
    {
        autoBalancingInterval = value;
    }


    @Override
    public final Object clone()
    {
        AutoLoadBalancing autoBalancing = new AutoLoadBalancing();
        autoBalancing.enabled = enabled;
        autoBalancing.autoBalancingThreshold = autoBalancingThreshold;
        autoBalancing.autoBalancingInterval = autoBalancingInterval;
        return autoBalancing;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException
    {
        enabled = reader.ReadBoolean();
        autoBalancingThreshold = reader.ReadInt32();
        autoBalancingInterval = reader.ReadInt32();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(enabled);
        writer.Write(autoBalancingThreshold);
        writer.Write(autoBalancingInterval);
    }

}
