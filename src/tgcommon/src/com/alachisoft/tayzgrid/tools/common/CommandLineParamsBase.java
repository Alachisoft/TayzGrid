
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

package com.alachisoft.tayzgrid.tools.common;

public class CommandLineParamsBase {

    private static String _userId = "";
    private static String _userPwd = "";
    private static boolean _printLogo = true;
    private static boolean _hotApply = false;
    private static boolean _overwrite = false;
    private static boolean _usage = false;

    public CommandLineParamsBase() {
    }

    /**
     * @return
     */
    @ArgumentAttributeAnnontation(shortNotation = "-H", fullNotation = "--hotapply", appendText = "", defaultValue = "false")
    public final boolean getIsHotApply() {
        return _hotApply;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-H", fullNotation = "--hotapply", appendText = "", defaultValue = "false")
    public final void setIsHotApply(boolean value) {
        _hotApply = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-O", fullNotation = "--overwrite", appendText = "", defaultValue = "false")
    public final boolean getIsOverWrite() {
        return _overwrite;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-O", fullNotation = "--overwrite", appendText = "", defaultValue = "false")
    public final void setIsOverWrite(boolean value) {
        _overwrite = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-h", fullNotation = "--help", appendText = "", defaultValue = "false")
    public final boolean getIsUsage() {
        return _usage;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-h", fullNotation = "--help", appendText = "", defaultValue = "false")
    public final void setIsUsage(boolean value) {
        _usage = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-G", fullNotation = "--nologo", appendText = "", defaultValue = "true")
    public final boolean getIsLogo() {
        return _printLogo;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-G", fullNotation = "--nologo", appendText = "", defaultValue = "true")
    public final void setIsLogo(boolean value) {
        _printLogo = value;
    }
}
