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

package com.alachisoft.tayzgrid.caching.alertspropagators;

import com.alachisoft.tayzgrid.config.newdom.EmailNotifications;
import com.alachisoft.tayzgrid.config.newdom.NotificationRecipient;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.Common;
import java.util.Iterator;

public class EmailNotifierArgs
{
    private EmailNotifications _emailNotification;
    private CacheRuntimeContext _context;

    public EmailNotifications getEmailNotifications()
    {
        return _emailNotification;
    }

    public final CacheRuntimeContext getCacheRuntimeContext()
    {
        return _context;
    }

    public EmailNotifierArgs(java.util.Map properties, CacheRuntimeContext context)
    {
        _context = context;
        _emailNotification = new EmailNotifications();

        if (properties.containsKey("email-notification-enabled"))
        {
            _emailNotification.setEmailNotificationEnabled(Boolean.parseBoolean((String) properties.get("email-notification-enabled")));
        }

        if (properties.containsKey("sender"))
        {
            _emailNotification.setSender((String) ((properties.get("sender") instanceof String) ? properties.get("sender") : null));
        }

        if (properties.containsKey("smtp-server"))
        {
            _emailNotification.setSmtpServer((String) ((properties.get("smtp-server") instanceof String) ? properties.get("smtp-server") : null));
        }

        if (properties.containsKey("smtp-port"))
        {
            _emailNotification.setSmtpPort(Integer.parseInt(((String) properties.get("smtp-port"))));
        }

        if (properties.containsKey("ssl"))
        {
            _emailNotification.setSSL(Boolean.parseBoolean((String) properties.get("ssl")));
        }

        if (properties.containsKey("authentication"))
        {
            _emailNotification.setAuthentication(Boolean.parseBoolean((String) properties.get("authentication")));
        }

        if (properties.containsKey("sender-login"))
        {
            _emailNotification.setLogin((String) ((properties.get("sender-login") instanceof String) ? properties.get("sender-login") : null));
        }

        if (properties.containsKey("sender-password"))
        {
            _emailNotification.setPassword((String) ((properties.get("sender-password") instanceof String) ? properties.get("sender-password") : null));
        }

        if (properties.containsKey("recipients"))
        {
            java.util.HashMap recipients = Common.as(properties.get("recipients"), java.util.HashMap.class);
            _emailNotification.setRecipients(GetRecipients(recipients));
        }
    }

    private NotificationRecipient[] GetRecipients(java.util.HashMap settings)
    {
        NotificationRecipient[] recipients = null;

        if (settings.size() != 0)
        {
            recipients = new NotificationRecipient[settings.size()];
            int index = 0;

            Iterator it = settings.entrySet().iterator();
            while (it.hasNext())
            {
                java.util.Map.Entry entry = (java.util.Map.Entry) it.next();
                recipients[index] = new NotificationRecipient();
                recipients[index].setID((String) entry.getValue());
                index++;
            }
        }
        return recipients;
    }
}