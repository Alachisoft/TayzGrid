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



public class AlertsNotifications implements Cloneable, InternalCompactSerializable
{

    private EmailNotifications _emailNotifications;
    private AlertsTypes _alertTypes;
    private boolean _emailNotification;

    @ConfigurationSectionAnnotation(value="email-notification")
    public final EmailNotifications getEMailNotifications()
    {
        return _emailNotifications;
    }

    @ConfigurationSectionAnnotation(value="email-notification")
    public final void setEMailNotifications(EmailNotifications value)
    {
        _emailNotifications = value;
    }

    @ConfigurationSectionAnnotation(value="alerts-types")
    public final AlertsTypes getAlertsTypes()
    {
        return _alertTypes;
    }

    @ConfigurationSectionAnnotation(value="alerts-types")
    public final void setAlertsTypes(AlertsTypes value)
    {
        _alertTypes = value;
    }

    public final Object clone()
    {
        AlertsNotifications alertNotification = new AlertsNotifications();
        alertNotification.setEMailNotifications(this.getEMailNotifications() != null ? (EmailNotifications) this.getEMailNotifications().clone() : null);
        alertNotification.setAlertsTypes(this.getAlertsTypes() != null ? (AlertsTypes) this.getAlertsTypes().clone() : null);
        return alertNotification;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _emailNotifications = (EmailNotifications)Common.readAs(reader.ReadObject(),EmailNotifications.class);
        _alertTypes = (AlertsTypes) Common.readAs(reader.ReadObject(),AlertsTypes.class );
        _emailNotification = reader.ReadBoolean();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.WriteObject(_emailNotifications);
        writer.WriteObject(_alertTypes);
        writer.Write(_emailNotification);
    }

}
