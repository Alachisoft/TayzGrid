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

package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.common.propagator.AlertNotificationTypes;

public class AlertTypeHelper
{

    public static AlertNotificationTypes Initialize(java.util.Map properties)
    {
        AlertNotificationTypes alertTypes = new AlertNotificationTypes();

        try
        {
            if (properties.containsKey("cache-stop"))
            {
                alertTypes.setCacheStop(Boolean.parseBoolean((String) properties.get("cache-stop")));
            }

            if (properties.containsKey("cache-start"))
            {
                alertTypes.setCacheStart(Boolean.parseBoolean((String) properties.get("cache-start")));
            }

            if (properties.containsKey("node-left"))
            {
                alertTypes.setNodeLeft(Boolean.parseBoolean((String) properties.get("node-left")));
            }

            if (properties.containsKey("node-joined"))
            {
                alertTypes.setNodeJoined(Boolean.parseBoolean((String) properties.get("node-joined")));
            }

            if (properties.containsKey("state-transfer-started"))
            {
                alertTypes.setStartTransferStarted(Boolean.parseBoolean((String) properties.get("state-transfer-started")));
            }

            if (properties.containsKey("state-transfer-stop"))
            {
                alertTypes.setStartTransferStop(Boolean.parseBoolean((String) properties.get("state-transfer-stop")));
            }

            if (properties.containsKey("state-transfer-error"))
            {
                alertTypes.setStartTransferError(Boolean.parseBoolean((String) properties.get("state-transfer-error")));
            }

            if (properties.containsKey("service-start-error"))
            {
                alertTypes.setServiceStartError(Boolean.parseBoolean((String) properties.get("service-start-error")));
            }

            if (properties.containsKey("cache-size"))
            {
                alertTypes.setCacheSize(Boolean.parseBoolean((String) properties.get("cache-size")));
            }

            if (properties.containsKey("general-error"))
            {
                alertTypes.setGeneralError(Boolean.parseBoolean((String) properties.get("general-error")));
            }

            if (properties.containsKey("licensing-error"))
            {
                alertTypes.setLicensingError(Boolean.parseBoolean((String) properties.get("licensing-error")));
            }

            if (properties.containsKey("configuration-error"))
            {
                alertTypes.setConfigurationError(Boolean.parseBoolean((String) properties.get("configuration-error")));
            }

            if (properties.containsKey("security-error"))
            {
                alertTypes.setSecurityError(Boolean.parseBoolean((String) properties.get("security-error")));
            }

            if (properties.containsKey("general-info"))
            {
                alertTypes.setGeneralInfo(Boolean.parseBoolean((String) properties.get("general-info")));
            }

            if (properties.containsKey("unhandled-exceptions"))
            {
                alertTypes.setUnHandledException(Boolean.parseBoolean((String) properties.get("unhandled-exceptions")));
            }

        }
        catch (Exception ex)
        {
        }
        return alertTypes;
    }
}
