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

package com.alachisoft.tayzgrid.web.session;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationBuilder;
import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import com.alachisoft.tayzgrid.web.CacheSession;
import com.alachisoft.tayzgrid.web.LockException;
import com.alachisoft.tayzgrid.web.ProxyRequest;
import com.alachisoft.tayzgrid.web.ProxyResponse;
import com.alachisoft.tayzgrid.web.config.dom.SessionConfiguration;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class TayzGridSessionProvider implements Filter {

    TayzGridManager _cache = null;
    String cache = null;
    Logger logger = Logger.getLogger(TayzGridSessionProvider.class);
    private boolean lockRemoteSession = true;
    int lockTimeOut = 1;
    int numberOfRetries = 5;
    int retryInterval = 500;
    boolean emptySessions = false;
    String configPath = null;
    String sessionConfigPath = null;
    public static final String INITPARAM_LOCK_REMOTE_SESSION = "enable-session-locking";
    public static final String INITPARAM_LOCK_TIMEOUT = "lock-timeout";
    public static final String EMPTY_SESSIONS = "empty-session-when-locked";
    public static final String INITPARAM_NUMBER_OF_RETRIES = "retries-count";
    public static final String INITPARAM_RETRY_INTERVAL = "retry-interval";
    public static final String INITPARAM_SCONFIG_PATH = "configPath";
    public static final String INITPARAM_SCONFIG_FILE = "session.xml";
    private boolean _isMultiSite = false;
    private static SessionConfiguration[] _sessionConfigurations;
    private static SessionConfiguration _sessionConfiguration;

    public void init(FilterConfig filterConfig) throws ServletException {
        //path of session config file
        sessionConfigPath = filterConfig.getInitParameter(INITPARAM_SCONFIG_PATH);
        try {
            if (sessionConfigPath != null) {
                String path = Common.combinePath(sessionConfigPath, INITPARAM_SCONFIG_FILE);
                File f = new File(path);
                if (f.exists() && f.canRead()) {
                    loadConfig(INITPARAM_SCONFIG_FILE, path);
                } else {
                    logger.error("Unable to load TayzGrid session configuration file");
                }
            } else {
                throw new ServletException("Please make sure that web.xml have ‘configPath’ init-param for TayzGrid filter.");
            }
        } catch (Exception e) {
            //log exception here
            logger.error("Unable to configure TayzGrid session provider: " + e.getMessage());
        }
        try {
            if (_sessionConfiguration != null) {
             
                
                    cache = _sessionConfiguration.getCache() != null ? _sessionConfiguration.getCache().getId() : null;
                

                if (cache == null) {
                    throw new ServletException("cacheName cannot be null");
                }
                Object tmp = _sessionConfiguration.getLocking().getEnableLocking();
                if (tmp != null) {
                    lockRemoteSession = convertToBoolean(TayzGridSessionProvider.INITPARAM_LOCK_REMOTE_SESSION, tmp.toString());
                }
                if (lockRemoteSession) {
                    tmp = _sessionConfiguration.getLocking().getLockTimeout();
                    if (tmp != null) {
                        lockTimeOut = convertToInt(TayzGridSessionProvider.INITPARAM_LOCK_TIMEOUT, tmp.toString());
                    }
                    tmp = _sessionConfiguration.getLocking().getRetriesCount();
                    if (tmp != null) {
                        numberOfRetries = convertToInt(TayzGridSessionProvider.INITPARAM_NUMBER_OF_RETRIES, tmp.toString());
                    }
                    tmp = _sessionConfiguration.getLocking().getRetryInterval();
                    if (tmp != null) {
                        retryInterval = convertToInt(TayzGridSessionProvider.INITPARAM_RETRY_INTERVAL, tmp.toString());
                    }
                    tmp = _sessionConfiguration.getLocking().getEmptySessions();
                    if (tmp != null) {
                        emptySessions = convertToBoolean(TayzGridSessionProvider.EMPTY_SESSIONS, tmp.toString());
                    }
                }
                configPath = sessionConfigPath;
                initLogging(filterConfig);

                logger.debug("Filter configuration:");
                logger.debug("\tcacheId: " + _cache);
                logger.debug("\tlockRemoteSession: " + lockRemoteSession);
                logger.debug("\tlockTimeout: " + lockTimeOut);
                logger.debug("\tnumberOfRetries: " + numberOfRetries);
                logger.debug("\tretryInterval: " + retryInterval);
                logger.debug("\tconfigPath: " + configPath);

               _cache = new TayzGridManager(cache.toString(), lockRemoteSession, lockTimeOut, numberOfRetries, retryInterval, emptySessions, configPath);
            }
        } catch (Exception e) {
            //log exception here
            throw new ServletException(e);
        }
    }

    private void initLogging(FilterConfig filterConfig) {
        String logconfig = _sessionConfiguration.getLogFile().getPropFile();
        boolean logconfigured = false;
        if (logconfig != null) {
            String realpath = filterConfig.getServletContext().getRealPath(logconfig);
            if (realpath != null) {
                File f = new File(realpath);
                if (f.exists() && f.canRead()) {
                    PropertyConfigurator.configure(realpath);
                    logconfigured = true;
                }
            }
        }
        if (!logconfigured && logconfig != null) {
            // this might be due to getRealPath() returning null as in weblogic
            // try loading from classes
            String tmp = logconfig;
            if (!tmp.startsWith("/")) {
                tmp = "/" + logconfig;
                try {
                    URL url = this.getClass().getResource(tmp);
                    if (url != null) {
                        PropertyConfigurator.configure(url);
                        logconfigured = true;
                    }
                } catch (Exception exp) {
                }

            }
        }
        if (!logconfigured) {
            logconfig = null;
            PropertyConfigurator.configure(this.getClass().getResource("/com/alachisoft/tayzgrid/web/log4j.properties"));
            logconfigured = true;
        }
        logger = Logger.getLogger(TayzGridSessionProvider.class);
        if (logconfigured) {
            logger.info("Logger initialized from " + (logconfig == null ? "defaults" : logconfig));
        } else {
            logger.info("TayzGrid is not able to initialize logging. Will use the global logger.");
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(_cache==null) return;
        ServletRequest req = request;
        ServletResponse resp = response;
        logger.info("processing request");
        logger.debug("In doFilter ...");
        if (!_cache.isConnected()) {
            logger.info("Cache [" + cache + "] is not initialized. retrying ...");
            try {
                _cache.initializeCache(cache, configPath); //--- cacheManager cannot be null
            } catch (Exception exp) {
                throw new ServletException(exp);
            }
        }
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            if (_cache.isConnected()) {
                logger.debug("Generating proxy request and response");
                resp = new ProxyResponse((HttpServletResponse) response);
                try {
                    req = new ProxyRequest((HttpServletRequest) request, (ProxyResponse) resp, _cache);
                } catch (LockException exp) {
                    throw new RuntimeException(exp);
                }
            }
        }

        chain.doFilter(req, resp);

        logger.debug("Request completed.");
        //--- request completed.
        if (req instanceof ProxyRequest) {
            if (((ProxyRequest) req).hasFatalError()) {
                //--- request had fetal error because session was locked. should be reported to the user.
                try {
                    ((HttpServletResponse) response).sendError(500, "Session is locked by another request. Please try after few seconds.");
                } catch (Exception ex) {
                } //--- ignore exception - we tried to do something good, but its not worth
            }

            CacheSession csession = ((ProxyRequest) req).getCacheSession();
            if (csession != null) {
                logger.debug("Saving session state.");
                javax.servlet.http.HttpSession tmpSession = ((HttpServletRequest) request).getSession(false);
                Object lock = tmpSession;
                if (lock == null) {
                    lock = new Object(); //--- create new lock. Will avoid Null lock but have no effect
                }
                //--- save the session state to the cache ...
                try {
                    synchronized (lock) {
                        _cache.finalizeCacheSession(tmpSession, csession); //--- don't create session if invalidated
                    }
                } catch (Exception exp) {
                    throw new RuntimeException(exp);
                }
                //--- csession is not null which simply means request is instanceof HttpServletRequest
                //--- add a cookie

                ((HttpServletResponse) response).addCookie(new Cookie(ProxyRequest.TAYZGRID_SESSION_ID, csession.getSessionId()));
            } else {
                logger.debug("No valid session to store in cache.");
            }
        }

    }

    public void destroy() {
        if(_cache==null) return;
        logger.info("Closing filter. Application going down.");
        if (_cache.isConnected()) {
            logger.info("Disconnecting from cache: " + cache);
            _cache.disconnect();
        }
    }

    private int convertToInt(String paramName, String value) {
        try {
            //if (value == null)
            if (value == null || value.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(value);
        } catch (Exception exp) {
            throw new IllegalArgumentException("Invalid value for parameter: " + paramName);
        }
    }

    private boolean convertToBoolean(String paramName, String value) {
        //--- we will not use Boolean.valueOf as it returns false for anything other than true
        // however, we need to throw an exception so that user is aware of what he is doing
        if (value == null) {
            return false;
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        } else {
            throw new IllegalArgumentException("Invalid value for parameter: " + paramName);
        }
    }

    private void loadConfig(String file, String path) throws Exception {
        ConfigurationBuilder builder = new ConfigurationBuilder(path);
        try {
            builder.RegisterRootConfigurationObject(SessionConfiguration.class);
            builder.ReadConfiguration();
        } catch (Exception exc) {
            throw new ConfigurationException(exc.getMessage(), exc.getCause());
        }
        _sessionConfigurations = new SessionConfiguration[builder.getConfiguration().length];
        System.arraycopy(builder.getConfiguration(), 0, _sessionConfigurations, 0, builder.getConfiguration().length);
        if (_sessionConfigurations.length == 1) {
            _sessionConfiguration = _sessionConfigurations[0];
        } else {
            throw new ConfigurationException("Please provide config for sessions or multi site sessions");
        }

    }
}
