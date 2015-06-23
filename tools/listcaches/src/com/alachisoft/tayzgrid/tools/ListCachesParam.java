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

public class ListCachesParam extends com.alachisoft.tayzgrid.tools.common.CommandLineParamsBase {

    private String server="";
    private int port=-1;
    boolean detailed;//, printConf, xmlSyntax;

    public ListCachesParam() {
    }

    @ArgumentAttributeAnnontation(shortNotation = "-s", fullNotation = "--server", appendText = "", defaultValue = "")
    public final void setServer(String value) {
        this.server = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-s", fullNotation = "--server", appendText = "", defaultValue = "")
    public final String getServer() {
        return this.server;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-p", fullNotation = "--port", appendText = "", defaultValue = "")
    public final void setPort(int value) {
        this.port = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-p", fullNotation = "--port", appendText = "", defaultValue = "")
    public final int getPort() {
        return this.port;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-a", fullNotation = "--detail", appendText = "", defaultValue = "")
    public final void setDetail(boolean value) {
        this.detailed = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-a", fullNotation = "--detail", appendText = "", defaultValue = "")
    public final boolean getDetail() {
        return this.detailed;
    }

//    @ArgumentAttributeAnnontation(shortNotation = "-x", fullNotation = "--dp-xml", appendText = "", defaultValue = "false")
//    public final void setEnableXml(boolean value) {
//        this.printConf = true;
//        this.xmlSyntax = true;
//    }
//
//    public final boolean getXmlSyntax() {
//        return this.xmlSyntax;
//    }
//
//    public final boolean getPrintConf() {
//        return this.printConf;
//    }
//
//    @ArgumentAttributeAnnontation(shortNotation = "-z", fullNotation = "--dp-string", appendText = "", defaultValue = "false")
//    public final void setDisableXml(boolean value) {
//
//        this.printConf = true;
//        this.xmlSyntax = false;
//    }
}
