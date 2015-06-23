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

public class SQLDependencyConfig implements Cloneable, InternalCompactSerializable
{

    private boolean _useDefault = true;

    @ConfigurationAttributeAnnotation(value="use-default",appendText="")
    public final boolean getUseDefault()
    {
        return _useDefault;
    }

    @ConfigurationAttributeAnnotation(value="use-default",appendText="")
    public final void setUseDefault(boolean value)
    {
        _useDefault = value;
    }

    @Override
    public final Object clone()
    {
        SQLDependencyConfig sqlDependencyConfig = new SQLDependencyConfig();
        sqlDependencyConfig.setUseDefault(this.getUseDefault());
        return sqlDependencyConfig;
    }
    @Override
    public void Deserialize(CompactReader reader) throws IOException
    {
        _useDefault = reader.ReadBoolean();
    }

    /**
     *
     * @param writer
     * @throws IOException
     */
    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(_useDefault);
    }

}
