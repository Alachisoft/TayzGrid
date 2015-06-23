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

import com.alachisoft.tayzgrid.common.util.ReaderWriterLock;
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import com.alachisoft.tayzgrid.web.CacheSession;
import com.alachisoft.tayzgrid.web.EmptySession;
import com.alachisoft.tayzgrid.web.LockException;
import com.alachisoft.tayzgrid.web.caching.Cache;
import com.alachisoft.tayzgrid.web.caching.CacheItem;
import com.alachisoft.tayzgrid.web.caching.CacheItemVersion;
import com.alachisoft.tayzgrid.web.caching.LockHandle;
import com.alachisoft.tayzgrid.web.caching.TayzGrid;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;

public class TayzGridManager {

    protected Cache cache = null;
    protected Logger logger = Logger.getLogger(TayzGridManager.class);
    protected boolean lockRemoteSession = false;
    protected int lockTimeOut = 0;
    protected HashMap locks = new HashMap();
    protected int numberOfRetries = 0;
    protected int retryInterval = 0; // in milliseconds
    protected String _cacheId = null; 
    protected ReaderWriterLock _sync = new ReaderWriterLock();//synchronizes the cache operations and cache connection recycling.
    boolean _emptySessionWhenLocked = false;
    protected String _primaryCache="";
    protected String _currentSessionCache="";
    private String SESSION_TAG = "TayzGrid_session_data";
    //--------------------------------------------------------------------------------------------------------
    public TayzGridManager(String cacheId, boolean lockRemoteSession, int lockTimeOut, int numberOfRetries, int retryInterval, boolean emptySessions, String configPath) throws Exception {
        this.lockRemoteSession = lockRemoteSession;
        this.lockTimeOut = lockTimeOut;
        this.numberOfRetries = numberOfRetries;
        this.retryInterval = retryInterval;
        this._emptySessionWhenLocked = emptySessions;
        if (lockRemoteSession) {
            if (lockTimeOut < 0) {
                throw new IllegalArgumentException(TayzGridSessionProvider.INITPARAM_LOCK_TIMEOUT + " must be a non-negative value: " + lockTimeOut);
            }
            if (numberOfRetries < 0) {
                throw new IllegalArgumentException(TayzGridSessionProvider.INITPARAM_NUMBER_OF_RETRIES + " must be a non-negative value: " + numberOfRetries);
            }
            if (retryInterval < 0) {
                throw new IllegalArgumentException(TayzGridSessionProvider.INITPARAM_RETRY_INTERVAL + " must be a non-negative value: " + retryInterval);
            }
        }
        this._cacheId = cacheId;
        this.initializeCache(cacheId, configPath);
    }

    public boolean initializeCache(String cacheId, String configPath) throws Exception {
        try {
            logger.debug("Initializing cache: " + cacheId);
            if (configPath != null) {
                logger.debug("Using cache config path: " + configPath);
                TayzGrid.setConfigPath(configPath);
            }
            cache = TayzGrid.initializeCache(cacheId);
            logger.debug("Cache [" + cacheId + "] is initialized successfully.");
            return true;
        } catch (Exception ex) {
            logger.error("Unable to initialize the cache [" + cacheId + "]. See log for more details.", ex);
            throw ex;
        }
    }

    public void disconnect() {
        if (cache == null) {
            logger.debug("No valid cache instance to disconnect with.");
            //--- no need to stop. Cache not initialized
            return;
        }
        try {
            logger.debug("Disconnecting with cache: " + this._cacheId);
            cache.dispose();
            logger.debug("Successfully disconnected with cache: " + this._cacheId);
        } catch (Exception ex) {
            logger.error("Could not disconnect with cache [" + this._cacheId + "]. See log for more details.", ex);
        }
    }

    public boolean isConnected() {
        boolean connected = cache != null;
        logger.debug("Is Connected with cache [" + this._cacheId + "]: " + connected);

        return connected;
    }

    public void updateCacheItem(String key, Serializable item, int slicedExpiry, Cache cache) throws Exception {
        logger.debug("Updating cache item: " + key + " , with sliding expiration: " + slicedExpiry);

        CacheItem sessionItem = new CacheItem(item);
        com.alachisoft.tayzgrid.runtime.caching.Tag[] tag= new com.alachisoft.tayzgrid.runtime.caching.Tag[1];
        tag[0]=new com.alachisoft.tayzgrid.runtime.caching.Tag(SESSION_TAG);
        sessionItem.setTags(tag);
        
        if (cache != null) {
            try {
                if (slicedExpiry < 0) {
                    //--- if it is negative - never expire
                    logger.debug("Item will never expire.");
                    if (locks.get(key) != null) {
                        logger.debug("Will release the lock.");
                        sessionItem.setPriority(CacheItemPriority.Default);
                        cache.insert(key, sessionItem, (LockHandle) locks.get(key), true);
                        cache.insert(key, item);
                        locks.remove(key);
                    } else {
                        cache.insert(key, item);
                    }
                } else {
                    if (locks.get(key) != null) {
                        logger.debug("Will release the lock.");
                        sessionItem.setPriority(CacheItemPriority.Default);
                        sessionItem.setAbsoluteExpiration(Cache.DefaultAbsoluteExpiration);
                        sessionItem.setSlidingExpiration(new TimeSpan(slicedExpiry * 1000));
                        CacheItemVersion version = cache.insert(key, sessionItem, (LockHandle) locks.get(key), true);
                        cache.insert(key, item);
                        locks.remove(key);
                    } else {
                        sessionItem.setPriority(CacheItemPriority.Default);
                        sessionItem.setAbsoluteExpiration(Cache.DefaultAbsoluteExpiration);
                        sessionItem.setSlidingExpiration(new TimeSpan(slicedExpiry * 1000));
                        CacheItemVersion version = cache.insert(key, sessionItem, new LockHandle(), true);
                        cache.insert(key, item);
                    }
                }
            } catch (Exception ex) {
                logger.error("Unable to update session [" + key + "] in cache [" + this._cacheId + "]. See logs for more details.", ex); // - sajid
                throw ex;
            }
        } else {
            logger.debug("Cache [" + cache + "] is not initialized.");
            throw new Exception("Cache [" + cache + "] is not initialized.");
        }
    }

    public void removeCacheItem(String key, Cache cache) throws Exception {
        logger.debug("Removing session: " + key);
        if (cache != null) {
            try {
                if (locks.get(key) != null) {
                    cache.remove(key, (LockHandle) locks.get(key));
                    locks.remove(key);
                } else {
                    cache.remove(key);
                }
                logger.debug("Session is removed.");
            } catch (Exception ex) {
                logger.error("Could not remove session. See log for more detail.", ex);
            }
        } else {
            logger.debug("Cache " + this._cacheId + " is not initialized.");
        }
    }

    public Object getCacheItem(String key, Cache cache, int count) throws LockException, Exception {
        logger.debug("Getting session: " + key);
        if (cache != null) {
            try {
                Object obj = null;
                if (lockRemoteSession && _primaryCache.equalsIgnoreCase(_currentSessionCache)) {
                    LockHandle handle = new LockHandle();
                    obj = cache.get(key, new TimeSpan(lockTimeOut), handle, true);
                    obj= cache.get(key);
                    logger.debug("object is: " + obj + ", lockid is: " + handle.getLockId());
                    if (obj == null && (handle.getLockId() != null && handle.getLockId().length() > 0)) {
                        try {
                            count++;
                            if (count < this.numberOfRetries) {
                                logger.debug("Waiting ... for " + retryInterval + " ms");
                                Thread.sleep(this.retryInterval);
                                logger.debug("Retrying ... retry count: " + count);
                                if (SessionMonitor.getRequestCount(key) == 0) {
                                    logger.debug("Local Request Count is 0");
                                    return getCacheItem(key, cache, count);
                                } else {
                                    //--- somehow session has already been loaded by another request
                                    return null; //--- session is already locally new. No need to synch it
                                }
                            } else {
                                if (isEmptySession()) {
                                    return new EmptySession();
                                } else {
                                    logger.debug("Terminating ...");
                                    throw new LockException("Session is locked. please retry after few seconds.");
                                }
                            }
                        } catch (LockException le) {
                            throw le;
                        } catch (Exception exp) {
                            logger.debug("Could not get session: " + this._cacheId + ". See log for more details.", exp);
                        }
                    }
                    if (obj != null) {
                        locks.put(key, handle);
                    }
                } else {
                    obj = cache.get(key);
                }
                return obj;
            } catch (LockException le) {
                throw le;
            } catch (Exception ex) {
                logger.debug("Could not get session: " + this._cacheId + ". See log for more details.", ex); // - sajid
                throw ex;
            }
        } else {
            logger.debug("Cache " + this._cacheId + " is not initialized.");
            throw new Exception(); 
        }
    }

    public CacheSession findSessionById(String sessionId) throws LockException, Exception {
        logger.debug("Trying to find session by id: " + sessionId);
        Object obj = getCacheItem(sessionId, cache, 0);
        if (obj != null && obj instanceof CacheSession) {
            logger.debug("Found valid session object.");
            return (CacheSession) obj;
        }
        logger.debug("Could not find session [" + sessionId + "] in cache [" + this._cacheId + "]. Returning null."); // - sajid

        return null;
    }

    /**
     * This method tracks Session stored on JvCache server using different
     * techniques. If finds an already stored session, fills the local session
     * with attributes from JvCache copy. Otherwise creates a new session on
     * JvCache server. Here is the algorithm it uses to initialize the session:
     * ------------------------------------------------------------------------------------------
     * | If local session is new try to find a CacheSession. (This may be
     * because | | loadbalancer moved the request to a new node) | | if no
     * CacheSession exists by local session id, try to find a requested session.
     * | | (May be the new node created a different session object) | | if still
     * no CacheSession found, create a new one and add to cache. | | | | if
     * local session is not new, simply try to find a CacheSession by local
     * session ID. | | If no session found, create a new one and add it to
     * cache. | | | | Finally, copy all the attributes from CacheSession to the
     * local session. |
     * ------------------------------------------------------------------------------------------
     *
     * Initialize the session to the JvCache
     *
     * @param request instance of the HttpServletRequest that CONTAINER passed
     * to the filter
     * @return An instance of CacheSessiion saved to JvCache that can be later
     * on updated once the chain has finished execution
     */
    public CacheSession initializeCacheSession(HttpServletRequest request, CacheSession csession) throws LockException {
        logger.debug("Initializing sessioin for request -> " + request.getRemoteAddr() + ":" + request.getRemotePort());

        HttpSession session = request.getSession(false);
        if (session == null) {
            return csession;
        }

        String validSessionId = (csession != null ? csession.getSessionId() : null);
        if (validSessionId == null) {
            validSessionId = session.getId();
        }

        if (SessionMonitor.getRequestCount(validSessionId) == 0) {
            if (csession != null) {
                logger.debug("Maximum inactive interval is: " + csession.getMaxInactiveInterval());
                session.setMaxInactiveInterval((int) csession.getMaxInactiveInterval());
            }
        }

        //--- still null? Create new one and add to the
        if (csession == null) {
            logger.debug("Creating new session with id: " + getPrimaryPrefix() + session.getId());
            csession = new CacheSession(getPrimaryPrefix());
            csession.setSessionId(validSessionId);
            csession.setMaxInactiveInterval(session.getMaxInactiveInterval());
        }
        SessionMonitor.addRequest(csession.getSessionId());

        return csession;
    }

    /**
     * This method updates the state of the session in cache.Here is the
     * algorithm that is used to update the state:
     *
     * --------------------------------------------------------------------------------------
     * | If local session is null (because it has been invalidated) | | remove
     * the session from cache. | | | | Otherwise, if local session id does not
     * match with the one we have in cache | | (session invalidated and created
     * a new one) | | | | Remove the old session and add a new CacheSesion in
     * cache (In fact, just | | change the session id of the cache entry so that
     * it now can be tracked using | | new session id, rest of the state will be
     * updated later | | | | Finally, copy all the attributes from local session
     * to the CacheSession and | | update it on the JvCache server. Also clear
     * the attribute list of the local session.|
     * ---------------------------------------------------------------------------------------
     *
     * @param session
     * @param csession
     */
    public void finalizeCacheSession(HttpSession session, CacheSession csession) throws Exception {
        logger.debug("Finalizing session ..");
        if (session == null) {
            Cache cache = getCache();
            //--- csession is not valid. We should remove the csession from cache.
            this.removeCacheItem(csession.getSessionId(), cache);
            logger.debug("Session invalidated. Removing from cache: " + this._cacheId);
            return;
        }
        //--- remove the requestCount from session monitor
        SessionMonitor.removeRequest(csession.getSessionId());
        //--- check if another request is using the same session? If yes, do not save the session state for now
        if (SessionMonitor.getRequestCount(csession.getSessionId()) == 0 && csession.updateToCache()) {
            //--- get attributes list
            logger.debug("Filling the session attributes...");
            Enumeration names = session.getAttributeNames();
            while (names.hasMoreElements()) {
                String key = (String) names.nextElement();
                logger.debug("Updating key: " + key);
                csession.setAttribute(key, session.getAttribute(key));
                logger.debug("Removing key [" + key + "] from local session");
                session.removeAttribute(key);
            }
            //--- session is no longer new
            csession.setIsNew(false);
            csession.setMaxInactiveInterval(session.getMaxInactiveInterval());
            //--- now update the cache
            logger.debug("Saving session state...");
            //getting current primary cache instance for multi site cache
            this.updateCacheItem(csession.getSessionId(), csession, session.getMaxInactiveInterval(), cache);
        }
    }

    public CacheSession findValidRemoteSession(HttpServletRequest request, String[] expectedKeys) throws LockException {
        if (expectedKeys == null) {
            return null;
        }
        CacheSession cs = null;
        for (int i = 0; i < expectedKeys.length; i++) {
            if (expectedKeys[i] == null || expectedKeys[i].trim().length() == 0) {
                continue;
            }
            try {
                cs = findSessionById(expectedKeys[i].trim());
                if (cs != null) {
                    //Updating session id according to current primary cache prefix
                    cs.setPrefix(getPrimaryPrefix());
                    cs = initializeCacheSession(request, cs);
                    break;
                }
            } catch (Exception exp) {
            }
        }

        if (cs == null) {
            //--- cs still null? could be either non-existant or locally available.
            for (int i = 0; i < expectedKeys.length; i++) {
                if (expectedKeys[i] == null || expectedKeys[i].trim().length() == 0) {
                    continue;
                }
                if (SessionMonitor.getRequestCount(expectedKeys[i].trim()) > 0) {
                    cs = new CacheSession(getPrimaryPrefix());
                    cs.setSessionId(expectedKeys[i].trim());
                    break;
                }
            }
        }

        return cs;
    }

    public String getPrimaryPrefix() {
        return "";
    }

    public Cache getCache() {
        return cache;
    }

    public boolean isEmptySession() {
        return false;
    }
}
