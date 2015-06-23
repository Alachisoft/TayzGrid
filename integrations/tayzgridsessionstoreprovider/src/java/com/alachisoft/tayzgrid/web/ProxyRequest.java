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

import com.alachisoft.tayzgrid.web.session.TayzGridManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;

public class ProxyRequest extends javax.servlet.http.HttpServletRequestWrapper {

    private HttpServletRequest request = null; //--- Original request object
    private HttpSession session = null;
    private CacheSession csession = null;
    private TayzGridManager cacheManager = null;
    private boolean hasFatal = false;
    public static String TAYZGRID_SESSION_ID = "tayzgridsessionid";
    private String validSessionId = null;
    private Logger logger = Logger.getLogger(ProxyRequest.class);
    private ProxyResponse response = null;

    //--------------------------------------------------------------------------------------------------------
    public ProxyRequest(HttpServletRequest request, ProxyResponse response, TayzGridManager cacheManager) throws LockException {
        super(request);
        logger.debug("Initializing request...");
        this.response = response;
        this.request = request;
        this.cacheManager = cacheManager;
        // try to find valid remote session id for this request
        String idFromCookie = null;
        String idFromUrl = null;
        // find cookie
        Cookie[] cookies = request.getCookies();
        for (int i = 0; cookies != null && i < cookies.length; i++) {
            if (cookies[i] != null && cookies[i].getName().equalsIgnoreCase(TAYZGRID_SESSION_ID)) {
                idFromCookie = cookies[i].getValue();
                break;
            }
        }
        // find URL token
        String queryString = request.getRequestURI();
        if (queryString != null) {
            logger.debug("Query String:" + queryString);
            Pattern p = Pattern.compile("[^;]*(;(([^=]+)=([^;]+)))+");
            // WE expect the last token
            Matcher m = p.matcher(queryString);
            if (m.matches()) {
                String key = m.group(3);
                if (key != null && key.equalsIgnoreCase(TAYZGRID_SESSION_ID)) {
                    idFromUrl = m.group(4);
                }
            }
        }
        csession = cacheManager.findValidRemoteSession(request, new String[]{
                    idFromCookie, idFromUrl, request.getRequestedSessionId()
                });

        if (csession != null) {
            validSessionId = csession.getSessionId();
        }
        logger.debug("Valid Session ID=" + validSessionId);
        if (validSessionId != null && validSessionId.length() > 0) {
            response.setRemoteSessionId(validSessionId);
        }
    }

    public HttpSession getSession(boolean create) {

        logger.debug("Request for session, create= " + create);
        if (!create && request.getSession(false) == null) {
            logger.debug("No valid session, returning null");
            return null;
        }
        if (request.getSession(false) != null) {
            if (csession == null) {
                logger.debug("Initializing cache session");
                try {
                    synchronized (request.getSession(false)) {
                        csession = cacheManager.initializeCacheSession(request, null);
                    }
                } catch (LockException ex) {
                    hasFatal = true;
                    throw new RuntimeException(ex.getMessage());
                }
            }
            if (session == null) {
                logger.debug("Creating session proxy");
                session = new ProxySession(request.getSession(false), this, csession);
            }
        } else if (create) {
            logger.debug("No valid local session, creating new ...");
            try {
                synchronized (request.getSession()) {
                    csession = cacheManager.initializeCacheSession(request, csession);
                }
            } catch (LockException ex) {
                hasFatal = true;
                throw new RuntimeException(ex.getMessage());
            }
            logger.debug("Creating session proxy");
            session = new ProxySession(request.getSession(true), this, csession);
            validSessionId = csession.getSessionId();
            response.setRemoteSessionId(validSessionId);
        }
        return session;
    }

    public HttpSession getSession() {
        return getSession(true);
    }

    public CacheSession getCacheSession() {
        return csession;
    }

    public void sessionInvalidated() {
        //--- session being invalidated. handle here
        session = null;
        csession.reset();
    }

    public boolean hasFatalError() {
        return hasFatal;
    }
}
