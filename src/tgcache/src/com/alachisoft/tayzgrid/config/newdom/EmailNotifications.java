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

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

/**
 * This Class is responsible for holding email-notification related fields
 */
public class EmailNotifications implements Cloneable, InternalCompactSerializable
{

    private boolean _emailNotification;
    private String _sender = "";
    private String _smtpServer = "";
    private String _senderLogin = "";
    private String _senderPassword = "";
    private int _smtpPort;
    private boolean _ssl;
    private boolean _userAuthentication;
    private NotificationRecipient[] _notificationRecipients;

    @ConfigurationAttributeAnnotation(value = "email-notification", appendText = "")//Changes for Dom email-notification-enabled
    public final boolean getEmailNotificationEnabled()
    {
        return _emailNotification;
    }

    @ConfigurationAttributeAnnotation(value = "email-notification", appendText = "")//Changes for Dom email-notification-enabled
    public final void setEmailNotificationEnabled(boolean value)
    {
        _emailNotification = value;
    }

    @ConfigurationAttributeAnnotation(value = "sender", appendText = "")
    public final String getSender()
    {
        return _sender;
    }

    @ConfigurationAttributeAnnotation(value = "sender", appendText = "")
    public final void setSender(String value)
    {
        _sender = value;
    }

    @ConfigurationAttributeAnnotation(value = "smtp-server", appendText = "")
    public final String getSmtpServer()
    {
        return _smtpServer;
    }

    @ConfigurationAttributeAnnotation(value = "smtp-server", appendText = "")
    public final void setSmtpServer(String value)
    {
        _smtpServer = value;
    }

    @ConfigurationAttributeAnnotation(value = "smtp-port", appendText = "")
    public final int getSmtpPort()
    {
        return _smtpPort;
    }

    @ConfigurationAttributeAnnotation(value = "smtp-port", appendText = "")
    public final void setSmtpPort(int value)
    {
        _smtpPort = value;
    }

    @ConfigurationAttributeAnnotation(value = "ssl", appendText = "")
    public final boolean getSSL()
    {
        return _ssl;
    }

    @ConfigurationAttributeAnnotation(value = "ssl", appendText = "")
    public final void setSSL(boolean value)
    {
        _ssl = value;
    }

    @ConfigurationAttributeAnnotation(value = "authentication", appendText = "")
    public final boolean getAuthentication()
    {
        return _userAuthentication;
    }

    @ConfigurationAttributeAnnotation(value = "authentication", appendText = "")
    public final void setAuthentication(boolean value)
    {
        _userAuthentication = value;
    }

    @ConfigurationAttributeAnnotation(value = "sender-login", appendText = "")
    public final String getLogin()
    {
        return _senderLogin;
    }

    @ConfigurationAttributeAnnotation(value = "sender-login", appendText = "")
    public final void setLogin(String value)
    {
        _senderLogin = value;
    }

    @ConfigurationAttributeAnnotation(value = "sender-password", appendText = "")
    public final String getPassword()
    {
        return _senderPassword;
    }

    @ConfigurationAttributeAnnotation(value = "sender-password", appendText = "")
    public final void setPassword(String value)
    {
        _senderPassword = value;
    }

    @ConfigurationSectionAnnotation(value = "recipient")
    public final NotificationRecipient[] getRecipients()
    {
        return _notificationRecipients;
    }

    @ConfigurationSectionAnnotation(value = "recipient")
    public final void setRecipients(Object[] value)
    {
        Object[] objs = (Object[]) value;
        _notificationRecipients = new NotificationRecipient[objs.length];
        for (int i = 0; i < objs.length; i++)
        {
            _notificationRecipients[i] = (NotificationRecipient) objs[i];
        }
    }

    @Override
    public final Object clone()
    {
        EmailNotifications emailNotification = new EmailNotifications();
        emailNotification.setSender(this.getSender());
        emailNotification.setSmtpServer(this.getSmtpServer());
        emailNotification.setSmtpPort(this.getSmtpPort());
        emailNotification.setSSL(this.getSSL());
        emailNotification.setAuthentication(this.getAuthentication());
        emailNotification.setLogin(this.getLogin());
        emailNotification.setPassword(this.getPassword());
        Object tempVar = this.getRecipients().clone();
        emailNotification.setRecipients(this.getRecipients() != null ? (NotificationRecipient[]) ((tempVar instanceof NotificationRecipient[]) ? tempVar : null) : null);
        emailNotification.setEmailNotificationEnabled(this.getEmailNotificationEnabled());
        return emailNotification;
    }

    /**
     *
     * @param reader
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException
    {
        _emailNotification = reader.ReadBoolean();
        _sender = Common.as(reader.ReadObject(), String.class);
        _smtpServer = Common.as(reader.ReadObject(), String.class);
        _senderLogin = Common.as(reader.ReadObject(), String.class);
        _senderPassword = Common.as(reader.ReadObject(), String.class);
        _smtpPort = reader.ReadInt32();
        _ssl = reader.ReadBoolean();
        _userAuthentication = reader.ReadBoolean();
        _notificationRecipients = Common.as(reader.ReadObject(), NotificationRecipient[].class);
    }

    /**
     *
     * @param writer
     * @throws IOException
     */
    @Override
    public void Serialize(CompactWriter writer) throws IOException
    {
        writer.Write(_emailNotification);
        writer.WriteObject(_sender);
        writer.WriteObject(_smtpServer);
        writer.WriteObject(_senderLogin);
        writer.WriteObject(_senderPassword);
        writer.Write(_smtpPort);
        writer.Write(_ssl);
        writer.Write(_userAuthentication);
        writer.WriteObject(_notificationRecipients);
    }
}
