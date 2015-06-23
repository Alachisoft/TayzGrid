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

package com.alachisoft.tayzgrid.common.util;

import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;

/**
 * This class is used to authenticate feature available in installed editions For example; If only .net edition is installed then only java supported readThru, writeThru and
 * cacheloader won't work similarly, if only java edition is installed then only .net based readThru, writeThru and cacheloader will not work If both edition's are installed then
 * .net and java based readThru, writeThru and cacheloader will work.
 */
public final class AuthenticateFeature
{

    private static InstallModes _dotNetInstallMode = InstallModes.None;
    private static InstallModes _javaInstallMode = InstallModes.None;
    private static final String DOTNET_INSTALL_MODE = "DotNetInstallMode";
    private static final String JAVA_INSTALL_MODE = "JavaInstallMode";

    /**
     * Set the java and .net editions are installed mode
     */
    static
    {
        try
        {
            _dotNetInstallMode = InstallModes.Server;
            _javaInstallMode = InstallModes.Server;

        }
        catch (RuntimeException exception)
        {
        }

    }

    /**
     * Verify whether java edition is installed or not
     *
     * @return true if java edition is installed otherwise; false
     *
     */
    public static boolean getIsJavaEnabled()
    {
        if (_javaInstallMode == InstallModes.Client)
        {
            return true;
        }
        if (_javaInstallMode == InstallModes.Developer)
        {
            return true;
        }
        if (_javaInstallMode == InstallModes.Server)
        {
            return true;
        }

        return false;
    }

    /**
     * Verify whether .net edition is installed or not
     *
     * @return true if .net edition is installed otherwise; false
     *
     */
    public static boolean getIsDotNetEnabled()
    {
        if (_dotNetInstallMode == InstallModes.Client)
        {
            return true;
        }
        if (_dotNetInstallMode == InstallModes.Developer)
        {
            return true;
        }
        if (_dotNetInstallMode == InstallModes.Server)
        {
            return true;
        }

        return false;
    }

    /**
     * Get Java Install Mode
     *
     * @return java install mode
     *
     */
    public static InstallModes getJavaInstallMode()
    {
        return _javaInstallMode;
    }

    /**
     * Get .net Install Mode
     *
     * @return .net install mode
     *
     */
    public static InstallModes getDotNetInstallMode()
    {
        return _dotNetInstallMode;
    }

    public static void Authenticate(LanguageContext languageContext) throws ConfigurationException
    {
        if (languageContext == LanguageContext.DOTNET && !getIsDotNetEnabled())
        {
            throw new ConfigurationException(".net based readThru provider's are not supported in current installed NCache edition");
        }
        else if (languageContext == LanguageContext.JAVA && !getIsJavaEnabled())
        {
            throw new ConfigurationException("java based readThru provider's are not supported in current installed NCache edition");
        }

    }
}
