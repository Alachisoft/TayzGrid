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
package com.alachisoft.tayzgrid.common.commandline;

public abstract class CommandLineArgs
{

    private Boolean usage = false;
    private Boolean logo = true;

    @ArgumentAttributeAnnotation(defaultValue = "false", shortNotation = "-h", fullNotation = "--help", appendText = "")
    public Boolean getUsage()
    {
        return usage;
    }

    @ArgumentAttributeAnnotation(defaultValue = "false", shortNotation = "-h", fullNotation = "--help", appendText = "")
    public void setUsage(Boolean usage)
    {
        this.usage = usage;
    }

    @ArgumentAttributeAnnotation(defaultValue = "true", shortNotation = "-G", fullNotation = "--nologo", appendText = "")
    public Boolean getLogo()
    {
        return logo;
    }

    @ArgumentAttributeAnnotation(defaultValue = "true", shortNotation = "-G", fullNotation = "--nologo", appendText = "")
    public void setLogo(Boolean logo)
    {
        this.logo = logo;
    }

    public void printUsage()
    {
        StringBuilder usageBuilder = new StringBuilder();
        usageBuilder.append("  ").append("-h --help [help].").append("\n");
        usageBuilder.append(" \t").append("Displays a detailed help screen.").append("\n").append("\n");
        usageBuilder.append("  ").append("-G --nologo [nologo]").append("\n");
        usageBuilder.append(" \t").append("Suppresses the display of the logo banner").append("\n").append("\n");
        System.out.print(usageBuilder.toString());
    }

    public void printLogo()
    {
        System.out.println("Alachisoft (R) TayzGrid Utility - License Tool. Version 4.6.0.0 " + "\nCopyright (C) Alachisoft 2015. All rights reserved.\n");
    }
}
