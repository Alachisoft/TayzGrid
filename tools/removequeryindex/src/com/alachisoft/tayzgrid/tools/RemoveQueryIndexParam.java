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

package com.alachisoft.tayzgrid.tools;

import com.alachisoft.tayzgrid.tools.common.ArgumentAttributeAnnontation;

/**
 * Summary description for ConfigureQueryIndexTool.
 *
 */
public class RemoveQueryIndexParam extends com.alachisoft.tayzgrid.tools.common.CommandLineParamsBase {

    private static String _asmPath = "";
    private static String _class = "";
    private static String _attributes = "";
    private static String _cacheId = "";
    private static String _server = "";
    private static int _port = -1;

    public RemoveQueryIndexParam() {
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final String getCacheId() {
        return _cacheId;
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final void setCacheId(String value) {
        _cacheId = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-c", fullNotation = "--class", appendText = "", defaultValue = "")
    public final String GetClass() {
        return _class;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-c", fullNotation = "--class", appendText = "", defaultValue = "")
    public final void setClass(String value) {
        _class = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-L", fullNotation = "--attrib-list", appendText = "", defaultValue = "")
    public final String getAttributes() {
        return _attributes;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-L", fullNotation = "--attrib-list", appendText = "", defaultValue = "")
    public final void setAttributes(String value) {
        _attributes = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-s", fullNotation = "--server", appendText = "", defaultValue = "")
    public final String getServer() {
        return _server;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-s", fullNotation = "--server", appendText = "", defaultValue = "")
    public final void setServer(String value) {
        _server = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-p", fullNotation = "--port", appendText = "", defaultValue = "")
    public final int getPort() {
        return _port;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-p", fullNotation = "--port", appendText = "", defaultValue = "")
    public final void setPort(int value) {
        _port = value;
    }
}
