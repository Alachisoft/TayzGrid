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

package com.alachisoft.tayzgrid.config;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class Mapping implements Cloneable, InternalCompactSerializable, java.io.Serializable {

    private String _privateIp;
    private int _privatePort;
    private String _publicIP;
    private int _publicPort;

    @ConfigurationAttributeAnnotation(value = "public-ip", appendText = "")
    public final String getPublicIP() {
        return _publicIP;
    }

    @ConfigurationAttributeAnnotation(value = "public-ip", appendText = "")
    public final void setPublicIP(String value) {
        _publicIP = value;
    }

    @ConfigurationAttributeAnnotation(value = "public-port", appendText = "")
    public final int getPublicPort() {
        return _publicPort;
    }

    @ConfigurationAttributeAnnotation(value = "public-port", appendText = "")
    public final void setPublicPort(int value) {
        _publicPort = value;
    }

    @ConfigurationAttributeAnnotation(value = "private-ip", appendText = "")
    public final String getPrivateIP() {
        return _privateIp;
    }

    @ConfigurationAttributeAnnotation(value = "private-ip", appendText = "")
    public final void setPrivateIP(String value) {
        _privateIp = value;
    }

    @ConfigurationAttributeAnnotation(value = "private-port", appendText = "")
    public final int getPrivatePort() {
        return _privatePort;
    }

    @ConfigurationAttributeAnnotation(value = "private-port", appendText = "")
    public final void setPrivatePort(int value) {
        _privatePort = value;
    }

    @Override
    public final Object clone() {
        Mapping map = new Mapping();
        map.setPrivateIP(_privateIp);
        map.setPrivatePort(_privatePort);
        map.setPublicIP(_publicIP);
        map.setPublicPort(_publicPort);

        return map;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {

        _privateIp = (String) Common.as(reader.ReadObject(), String.class);
        _privatePort = reader.ReadInt32();
        _publicIP = (String) Common.as(reader.ReadObject(), String.class);
        _publicPort = reader.ReadInt32();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(this._privateIp);
        writer.Write(this._privatePort);
        writer.WriteObject(this._publicIP);
        writer.Write(this._publicPort);
    }
}
