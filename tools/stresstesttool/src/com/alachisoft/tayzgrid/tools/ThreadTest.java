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

package com.alachisoft.tayzgrid.tools;

import com.alachisoft.tayzgrid.web.caching.Cache;
import com.alachisoft.tayzgrid.web.caching.CacheInitParams;
import com.alachisoft.tayzgrid.web.caching.CacheMode;
import com.alachisoft.tayzgrid.web.caching.TayzGrid;

public class ThreadTest {

        String _cacheId = "";
        int _totalLoopCount = 0;
        int _testCaseIterations = 10;
        int _testCaseIterationDelay = 0;
        int _getsPerIteration = 1;
        int _updatesPerIteration = 1;
        int _dataSize = 1024;
        int _expiration = 300;
        int _threadCount = 1;
        int _reportingInterval = 5000;

        public ThreadTest(String cacheId, int totalLoopCount, int testCaseIterations, int testCaseIterationDelay, int getsPerIteration, int updatesPerIteration, int dataSize, int expiration, int threadCount, int reportingInterval, boolean noLogo)
        {
            _cacheId = cacheId;
            _totalLoopCount = totalLoopCount;
            _testCaseIterations = testCaseIterations;
            _testCaseIterationDelay = testCaseIterationDelay;
            _getsPerIteration = getsPerIteration;
            _updatesPerIteration = updatesPerIteration;
            _dataSize = dataSize;
            _expiration = expiration;
            _threadCount = threadCount;
            _reportingInterval = reportingInterval;
        }

        public void Test()
        {
            try
            {
                Thread[] threads = new Thread[_threadCount];
                CacheInitParams ciParams = new CacheInitParams();
                ciParams.setMode(CacheMode.OutProc);
                Cache cache = com.alachisoft.tayzgrid.web.caching.TayzGrid.initializeCache(_cacheId, ciParams);
            
                cache.setExceptionsEnabled(true);

                for (int threadIndex = 0; threadIndex < _threadCount; threadIndex++)
                {
                    ThreadContainer tc = new ThreadContainer(cache, _totalLoopCount, _testCaseIterations, _testCaseIterationDelay, _getsPerIteration, _updatesPerIteration, _dataSize, _expiration, _threadCount, _reportingInterval, threadIndex);
                    threads[threadIndex] = new Thread(tc);
                    threads[threadIndex].setName("ThreadIndex: " + threadIndex);
                    threads[threadIndex].start();
                }

                for (int threadIndex = 0; threadIndex < threads.length; threadIndex++)
                {
                    threads[threadIndex].join();
                }

                cache.dispose();
            }
            catch (Exception e)
            {
                 System.err.println("Error :- " + e.getMessage());
                 e.printStackTrace();
            }
        }
}
