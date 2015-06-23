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

package com.alachisoft.tayzgrid.web;

import com.alachisoft.tayzgrid.web.session.SessionMonitor;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import org.apache.log4j.Logger;

public class ProxySession implements HttpSession
{
    private HttpSession session = null;
    private boolean invalidated = false;
    private CacheSession csession = null;
    private ProxyRequest request = null;
    private Logger logger = Logger.getLogger(ProxySession.class);

    public ProxySession(HttpSession session, ProxyRequest request, CacheSession csession)
    {
        this.session = session;
        this.csession = csession;
        this.request = request;
        if (csession != null && session != null)
        {
            //--- this should never happen
            logger.debug("Initializing proxy session with attributes from remote session.");
            fillLocalSession(csession, session);
        }
    }
    private void fillLocalSession(CacheSession csession, HttpSession session)
    {
        Set keys = csession.getKeys();
        Iterator it = keys.iterator();
        while (it.hasNext())
        {
            String key = (String) it.next();
            logger.debug("Adding attribute [" + key + "] to local session.");
            session.setAttribute(key, csession.getAttribute(key));
        }
        /**
         * Clear the CacheSession attributes locally. We'll again fill it
         * with the attributes of the local session before saving it to JvCache
         * once we are done with execution of the request.
         */
        csession.clearAttributes();
    }
    public long getCreationTime()
    {
        return session.getCreationTime();
    }
    public String getId()
    {
        return csession.getSessionId();
    }
    public long getLastAccessedTime()
    {
        return session.getLastAccessedTime();
    }
    public ServletContext getServletContext()
    {
        return session.getServletContext();
    }
    public void setMaxInactiveInterval(int interval)
    {
        session.setMaxInactiveInterval(interval);
    }
    public int getMaxInactiveInterval()
    {
        return session.getMaxInactiveInterval();
    }
    public HttpSessionContext getSessionContext()
    {
        return session.getSessionContext();
    }
    public Object getAttribute(String name)
    {
        return session.getAttribute(name);
    }
    /**
     * @deprecated @param name
     * @return
     */
    public Object getValue(String name)
    {
        return getAttribute(name);
    }
    public Enumeration getAttributeNames()
    {
        return session.getAttributeNames();
    }
    public String[] getValueNames()
    {
        return session.getValueNames();
    }
    public void setAttribute(String name, Object value)
    {
        ensureSerializable(value);
        session.setAttribute(name, value);
    }
    public void putValue(String name, Object value)
    {
        ensureSerializable(value);
        setAttribute(name, value);
    }
    public void removeAttribute(String name)
    {
        session.removeAttribute(name);
    }
    public void removeValue(String name)
    {
        removeAttribute(name);
    }
    public void invalidate()
    {
        logger.debug("Session [" + getId() + "] is invalidated.");
        this.invalidated = true;
        SessionMonitor.resetRequestCount(getId());
        session.invalidate();
        request.sessionInvalidated(); //--- notify request to take appropriate measures
    }
    public boolean isNew()
    {
        return csession.isIsNew();
    }
    public boolean isInvalid()
    {
        return invalidated;
    }
    private void ensureSerializable(Object value)
    {
        if (!(value instanceof Serializable))
        {
            throw new IllegalArgumentException("non-serializable attributes can not be added/saved in session.");
        }
    }
}
