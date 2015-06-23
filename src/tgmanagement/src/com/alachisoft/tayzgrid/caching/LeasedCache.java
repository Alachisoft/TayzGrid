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

package com.alachisoft.tayzgrid.caching;

import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;

/**
 * Summary description for LeasedCache.
 */
public class LeasedCache extends Cache
{

    /**
     * Sponsor used to extend lifetime of cache.
     */


    /**
     * Default constructor.
     */
    public LeasedCache()
    {
    }

    /**
     * Overloaded constructor.
     *
     * @param configString
     */
    public LeasedCache(String configString) throws ConfigurationException
    {
        super(configString);
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     *
     * @param disposing
     *
     *
     */
    private void dispose(boolean disposing)
    {
        if (disposing)
        {
            System.gc();
        }
    }

    /**
     * Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
     */
    @Override
    public void dispose()
    {
        try
        {
            dispose(true);
        }
        finally
        {
            super.dispose();
        }
    }

    /**
     * Obtains a lifetime service object to control the lifetime policy for this instance.
     *
     * @return An object of type ILease used to control the lifetime policy for this instance.
     */
    public Object InitializeLifetimeService()
    {



        return new Object();
    }

    /**
     * Start the cache functionality.
     */
    @Override
    protected void Start(CacheRenderer renderer, String userId, String password, boolean twoPhaseInitialization) throws Exception
    {
        super.Start(renderer, userId, password, twoPhaseInitialization);
    }

    /**
     * Stop the internal working of the cache.
     */
    @Override
    public void Stop(boolean isGracefulShutdown) throws Exception
    {
        super.Stop(isGracefulShutdown);
    }
    
    @Override
    public  boolean VerifyNodeShutDown(boolean isGraceful)
    {
        return super.VerifyNodeShutDown(isGraceful);
    }

    /**
     * Start the cache functionality.
     */
    public final void StartInstance(CacheRenderer renderer, String userId, String password, boolean twoPhaseInitialization) throws Exception
    {
        Start(renderer, userId, password, twoPhaseInitialization);
    }

    public final void StartInstancePhase2()
    {
        try
        {
            super.StartPhase2();
        }
        catch (Exception e)
        {

        }
    }

    /**
     * Stop the internal working of the cache.
     */
    public final void StopInstance(Boolean isGracefulShutdown) throws Exception
    {
        Stop(isGracefulShutdown);
    }
    
    public Boolean VerifyNodeShutdownInProgress(Boolean isGracefulShutdown)
    {
        return VerifyNodeShutDown(isGracefulShutdown);
    }

}
