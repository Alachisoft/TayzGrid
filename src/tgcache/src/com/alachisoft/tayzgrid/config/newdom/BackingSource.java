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
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class BackingSource implements Cloneable, InternalCompactSerializable {

    private Readthru readthru;
    private Writethru writehtru;
    
    //For JCache Inproc: If the user configures the loader.
    private boolean isLoader = false;

    public BackingSource() {
    }
    
//    @ConfigurationSectionAnnotation(value = "enable-loader")
//    public final boolean getIsLoader() {
//        return isLoader;
//    }
//    @ConfigurationSectionAnnotation(value = "enable-loader")
//    public final void setIsLoader(boolean isLoader) {
//        this.isLoader = isLoader;
//    }

    @ConfigurationSectionAnnotation(value = "read-thru")
    public final Readthru getReadthru() {
        return readthru;
    }

    @ConfigurationSectionAnnotation(value = "read-thru")
    public final void setReadthru(Readthru value) {
        readthru = value;
    }

    @ConfigurationSectionAnnotation(value = "write-thru")
    public final Writethru getWritethru() {
        return writehtru;
    }

    @ConfigurationSectionAnnotation(value = "write-thru")
    public final void setWritethru(Writethru value) {
        writehtru = value;
    }

    @Override
    public final Object clone() {
        BackingSource store = new BackingSource();
        store.setReadthru(getReadthru() != null ? (Readthru) getReadthru().clone() : null);
        store.setWritethru(getWritethru() != null ? (Writethru) getWritethru().clone() : null);
        return store;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        readthru = (Readthru) Common.readAs(reader.ReadObject(), Readthru.class);
        writehtru = (Writethru) Common.readAs(reader.ReadObject(), Writethru.class);
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(readthru);
        writer.WriteObject(writehtru);
    }
}
