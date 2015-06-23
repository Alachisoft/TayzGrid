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

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class Writethru implements InternalCompactSerializable, Cloneable
{

    private boolean enabled;
    private Provider[] provider;
    private WriteBehind writeBehind;
    public Writethru() {
    }

    @ConfigurationSectionAnnotation(value = "provider")
    public final Provider[] getProviders() {
        return provider;
    }

    @ConfigurationSectionAnnotation(value = "provider")
    public final void setProviders(Object value) {
        
        if (value != null) 
        {
            Object[] obj = (Object[]) value;
            
            provider = new Provider[obj.length];
            for (int i = 0; i < obj.length; i++) {
                provider[i] = (Provider) obj[i];
            }
        }
    }
    @ConfigurationSectionAnnotation(value = "write-behind")
    public final WriteBehind getWriteBehind()
    {
        return writeBehind;
    }

    @ConfigurationSectionAnnotation(value = "write-behind")
    public final void setWriteBehind(WriteBehind value)
    {
        writeBehind = value;
    }

    @ConfigurationAttributeAnnotation(value = "enable-write-thru", appendText = "")//Changes for Dom enabled
    public final boolean getEnabled() {
        return enabled;
    }

    @ConfigurationAttributeAnnotation(value = "enable-write-thru", appendText = "")//Changes for Dom enabled
    public final void setEnabled(boolean value) {
        enabled = value;
    }

    @Override
    public final Object clone() {
        Writethru writethru = new Writethru();
        writethru.setEnabled(enabled);

        writethru.setProviders(getProviders() != null ? (Provider[]) ((getProviders().clone() instanceof Provider[]) ? getProviders().clone() : null) : null);
        return writethru;
    }
    
    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        provider = (Provider[]) Common.readAs(reader.ReadObject(), Provider[].class);
        enabled = reader.ReadBoolean();
        writeBehind = (WriteBehind)Common.readAs(reader.ReadObject(), WriteBehind.class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(provider);
        writer.Write(enabled);
        writer.WriteObject(writeBehind);
    }
}
