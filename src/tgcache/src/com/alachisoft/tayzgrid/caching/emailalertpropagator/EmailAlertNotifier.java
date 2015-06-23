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

import com.alachisoft.tayzgrid.config.newdom.EmailNotifications;
import com.alachisoft.tayzgrid.config.newdom.NotificationRecipient;
import com.alachisoft.tayzgrid.caching.alertspropagators.EmailNotifierArgs;
import com.alachisoft.tayzgrid.caching.CacheRuntimeContext;
import com.alachisoft.tayzgrid.common.propagator.IAlertPropagator;
import com.alachisoft.tayzgrid.common.propagator.AlertNotificationTypes;
import com.alachisoft.tayzgrid.common.Base64;
import com.alachisoft.tayzgrid.common.EventID;
import java.util.ArrayList;
import javax.mail.internet.InternetAddress;
import com.alachisoft.tayzgrid.common.EncryptionUtil;

/**
 * This class is responsible of sending email notification if configured. Dated: 20100414
 */
public class EmailAlertNotifier implements IAlertPropagator
{

    public static class SendMailArgs
    {

        private StringBuilder _mailMessage;
        private JVCSmtpClient _smtpClient;
        private InternetAddress[] _to;
        private InternetAddress _from;
        private String _subject;

        public final InternetAddress[] getTo()
        {
            return _to;
        }

        public final InternetAddress getFrom()
        {
            return _from;
        }

        public final String getSubject()
        {
            return _subject;
        }

        public final StringBuilder getMailMessage()
        {
            return _mailMessage;
        }

        public final JVCSmtpClient getSMTPClient()
        {
            return _smtpClient;
        }

        public SendMailArgs(InternetAddress from, InternetAddress[] to, String subject, StringBuilder mailMessage, JVCSmtpClient smtpClient)
        {
            _from = from;
            _to = to;
            _subject = subject;
            _mailMessage = mailMessage;
            _smtpClient = smtpClient;

        }
    }

    private java.util.ArrayList<Integer> _eventList = new java.util.ArrayList<Integer>(); //List of event to against whom email notification to be generated
    private java.util.ArrayList<String> _recipients = new java.util.ArrayList<String>(); //List of recipient of email alerts
    private EmailNotifications _emailNotification; //Holds email related fields
    private JVCSmtpClient _smtpClient; //SMTP client which actually sends the email
    private java.util.ArrayList<InternetAddress> _multipleRecipient = new ArrayList<InternetAddress>();
    private CacheRuntimeContext _context; //Runtime context is holded to know about the coordinator node and cache details
    private static final String _emailSignature = "\n\n\nTayzGrid Alerts"; //Defined signature of email
    private static final String _emailPostScript = "\nPS: This is TayzGrid auto generated email."; //Post script to be appended at email end after signature
    private static final String _emailClosing = _emailSignature + _emailPostScript;
    private String _emailSubject; //holds email subject
    private String _cacheName;
    private Object _syncLock = new Object(); //required for synchroization


    public EmailAlertNotifier()
    {
        int x = 0;
    }



    public final void Initialize(Object obj, AlertNotificationTypes alertTypes)
    {
        synchronized (_syncLock)
        {
            try
            {
                EmailNotifierArgs args = (EmailNotifierArgs) obj;
                _emailNotification = args.getEmailNotifications();
                _context = args.getCacheRuntimeContext(); 
                
                _smtpClient = new JVCSmtpClient(_emailNotification.getSmtpServer(), this._context);
                _smtpClient.Port = _emailNotification.getSmtpPort() == 0 ? 25 : _emailNotification.getSmtpPort();
                _smtpClient.EnableSsl = _emailNotification.getSSL();

                if (_emailNotification.getAuthentication())
                {
                    String decryptedPassword = _emailNotification.getPassword();
                    if (_emailNotification.getPassword().length() > 0) {
                            byte[] password = Base64.decode(_emailNotification.getPassword());
                            decryptedPassword = EncryptionUtil.Decrypt(password);
                    }

                    _smtpClient.setCredentialAuthenticator(new CredentialAuthenticator(_emailNotification.getLogin(), decryptedPassword));
                }

                _cacheName = _context.getCacheRoot().getName();
                _emailSubject = "[" + _cacheName + "] [Event ID: ";
                if (_emailNotification.getRecipients() != null && _emailNotification.getRecipients().length > 0)
                {
                    for (NotificationRecipient recipient : _emailNotification.getRecipients())
                    {
                        if (!_recipients.contains(recipient.getID()))
                        {
                            _recipients.add(recipient.getID());
                            _multipleRecipient.add(new InternetAddress(recipient.getID()));
                        }
                    }
                }

                if (alertTypes.getCacheStop())
                {
                    _eventList.add(EventID.CacheStop);
                }

                if (alertTypes.getCacheStart())
                {
                    _eventList.add(EventID.CacheStart);
                }

                if (alertTypes.getNodeLeft())
                {
                    _eventList.add(EventID.NodeLeft);
                }

                if (alertTypes.getNodeJoined())
                {
                    _eventList.add(EventID.NodeJoined);
                }

                if (alertTypes.getStartTransferStarted())
                {
                    _eventList.add(EventID.StateTransferStart);
                }

                if (alertTypes.getStartTransferStop())
                {
                    _eventList.add(EventID.StateTransferStop);
                }

                if (alertTypes.getStartTransferError())
                {
                    _eventList.add(EventID.StateTransferError);
                }

                if (alertTypes.getServiceStartError())
                {
                    _eventList.add(EventID.ServiceStartFailure);
                }

                if (alertTypes.getCacheSize())
                {
                    _eventList.add(EventID.CacheSizeWarning);
                }

                if (alertTypes.getGeneralError())
                {
                    _eventList.add(EventID.GeneralError);
                }

                if (alertTypes.getLicensingError())
                {
                    _eventList.add(EventID.LicensingError);
                }

                if (alertTypes.getConfigurationError())
                {
                    _eventList.add(EventID.ConfigurationError);
                }

                if (alertTypes.getSecurityError())
                {
                    _eventList.add(EventID.SecurityError);
                }

                if (alertTypes.getGeneralInfo())
                {
                    _eventList.add(EventID.GeneralInformation);
                }

                if (alertTypes.getUnHandledException())
                {
                    _eventList.add(EventID.UnhandledException);
                }
            }
            catch (Exception exception)
            {
                if (_context != null)
                {
                    _context.getCacheLog().Error("Email Notifier", exception.getMessage()); //Write in error log if any exception occurs regaring email notification
                }
            }
        }
    }

    public final void Unintialize()
    {
        synchronized (_syncLock)
        {
            _eventList.clear();
            _recipients.clear();
            _emailNotification = null;
        }
    }

    public final void RaiseAlert(int eventId, String source, String message)
    {
        synchronized (_syncLock)
        {
            try
            {
                if (_emailNotification == null || !_emailNotification.getEmailNotificationEnabled())
                {
                    return;
                }

                if (_context == null)
                {
                    return;
                }

                if (_context.getIsStartedAsMirror() == true)
                {
                    return;
                }

                if (_multipleRecipient.size() == 0)
                {
                    return;
                }

                if (!_eventList.contains(eventId))
                {
                    return;
                }

                if (eventId != EventID.CacheStop && eventId != EventID.CacheStart && eventId != EventID.CacheSizeWarning)
                {
                        if (!_context.getCacheRoot().getIsCoordinator())
                            return;
                 
                }

                StringBuilder messageBody = new StringBuilder();
                messageBody.append("Machine: " + java.net.InetAddress.getLocalHost().getHostName());//Environment.MachineName);
                messageBody.append("\nCache Name: " + _cacheName);
                messageBody.append("\nEvent Type: " + EventID.EventText(eventId));
                messageBody.append("\nEvent ID: " + (new Integer(eventId)).toString());
                messageBody.append("\nDate Time: " + new java.util.Date());
                messageBody.append("\n\nMessage: " + message);
                messageBody.append(_emailClosing);

                InternetAddress[] recipients = java.util.Arrays.copyOf(_multipleRecipient.toArray(), _multipleRecipient.size(), InternetAddress[].class);
                InternetAddress from = new InternetAddress(_emailNotification.getSender(), "TayzGrid Alerts");
                String subject = _emailSubject + (new Integer(eventId)).toString() + "] " + EventID.EventText(eventId);
                final SendMailArgs mailArgs = new SendMailArgs(from, recipients, subject, messageBody, _smtpClient);

                Thread workerThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        SendMail(mailArgs);
                    }
                });
                workerThread.start();
            }
            catch (Exception exception)
            {
                if (_context != null)
                {                    
                    _context.getCacheLog().Error("Email Notifier", exception.getMessage()); //Write in error log if any exception occurs regaring email notification
                }
            }
        }
    }

    private void SendMail(Object state)
    {
       
        synchronized (_syncLock)
        {
            try
            {
                SendMailArgs mailArgs = (SendMailArgs) state;
                mailArgs.getSMTPClient().Send(mailArgs.getFrom(), mailArgs.getTo(), mailArgs.getSubject(), mailArgs.getMailMessage().toString());
            }
            catch (Exception exception)
            {
                if (_context != null)
                {
                    _context.getCacheLog().Error("Email Notifier", exception.getMessage());                    
                }
            }
        }
    }
}
