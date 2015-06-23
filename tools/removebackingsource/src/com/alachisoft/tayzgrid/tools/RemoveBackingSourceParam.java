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
 * Summary description for ConfigureBackingSource.
 *
 *
 */
public class RemoveBackingSourceParam extends com.alachisoft.tayzgrid.tools.common.CommandLineParamsBase {

    private static String _asmPath = "";
    private static String _class = "";
    private static String _parameters;
    private static String _cacheId = "";
    private static boolean _readthru = false;
    private static boolean _writethru = false;
    private static String _server = "";
    private static int _port = -1;
    private static String _providorName = "";

    public RemoveBackingSourceParam() {
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final String getCacheId() {
        return _cacheId;
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final void setCacheId(String value) {
        _cacheId = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-R", fullNotation = "--readthru", appendText = "", defaultValue = "false")
    public final boolean getIsReadThru() {
        return _readthru;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-R", fullNotation = "--readthru", appendText = "", defaultValue = "false")
    public final void setIsReadThru(boolean value) {
        _readthru = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-W", fullNotation = "--writethru", appendText = "", defaultValue = "false")
    public final boolean getIsWriteThru() {
        return _writethru;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-W", fullNotation = "--writethru", appendText = "", defaultValue = "false")
    public final void setIsWriteThru(boolean value) {
        _writethru = value;
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

    @ArgumentAttributeAnnontation(shortNotation = "-n", fullNotation = "--provider-name", appendText = "", defaultValue = "")
    public final String getProviderName() {
        return _providorName;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-n", fullNotation = "--provider-name", appendText = "", defaultValue = "")
    public final void setProviderName(String value) {
        _providorName = value;
    }
}
