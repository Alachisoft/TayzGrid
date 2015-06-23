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

package com.alachisoft.tayzgrid.caching.emailalertpropagator;

import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class JVCSmtpClient
{
    private String _smtpServer;
    private int _port;
    private boolean _enableSSL;
    private Authenticator _authenticator;
    private CacheRuntimeContext _context;
    public int Port;
    public boolean EnableSsl;
    
    public Authenticator getCrentialAuthenticator()
    {
        return _authenticator;
    }

    public void setCredentialAuthenticator(Authenticator value)
    {
        _authenticator = value;
    }

    public JVCSmtpClient(String smtpServer, CacheRuntimeContext context)
    {
       this._smtpServer = smtpServer;
       this._context = context;
    }

    public void Send(InternetAddress from, InternetAddress[] recipients, String subject, String message) throws UnsupportedEncodingException
    {
        Properties props = new Properties();
        props.put("mail.smtp.host", _smtpServer);
        props.put("mail.smtp.port", Port);

        Session session;
        Authenticator credentials = getCrentialAuthenticator();
        if(credentials != null)
        {
            props.put("mail.smtp.auth", "true");
            session = Session.getInstance(props, credentials);
        }
        else
            session = Session.getInstance(props);

        try {
            Message msg;
            msg = new MimeMessage(session);
            msg.setFrom(from);
            msg.setRecipients(Message.RecipientType.TO, recipients);
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            msg.setText(message);

            javax.mail.Transport.send(msg);
        }
        catch (MessagingException e)
        {
            this._context.getCacheLog().Error(e.getMessage());
        }
    }
}
