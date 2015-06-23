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
import com.alachisoft.tayzgrid.common.configuration.ConfigurationRootAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

@ConfigurationRootAnnotation(value = "data-sharing")
public class DataSharing implements Cloneable, java.io.Serializable, InternalCompactSerializable {

    private java.util.ArrayList<Type> typesList;

    @ConfigurationSectionAnnotation(value = "sharing-type")//Changes for Dom type
    public final Type[] getTypes() {
        if (typesList != null) {
            return typesList.toArray(new Type[0]);
        }
        return null;
    }

    @ConfigurationSectionAnnotation(value = "sharing-type")//Changes for Dom type
    public final void setTypes(Object[] types) {
        if (typesList == null) {
            typesList = new java.util.ArrayList<Type>();
        }

        typesList.clear();
        if (types != null) {
            for (Object type : types) {
                typesList.add((Type) type);
            }
        }
    }

    public final java.util.ArrayList<Type> getTypesList() {
        return typesList;
    }

    public final void setTypesList(java.util.ArrayList<Type> value) {
        typesList = value;
    }

    @Override
    public final Object clone() {
        DataSharing dataSharing = new DataSharing();
        dataSharing.setTypes(getTypes() != null ? (Type[]) getTypes().clone() : null);
        return dataSharing;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        setTypes(Common.as(reader.ReadObject(), Type[].class));
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(getTypes());
    }
}
