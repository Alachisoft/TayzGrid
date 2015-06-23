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
import com.alachisoft.tayzgrid.runtime.CacheItemPriority;
import com.alachisoft.tayzgrid.runtime.util.TimeSpan;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public final class ThreadContainer implements Runnable{

        Cache _cache = null;
        int _totalLoopCount = 0;
        int _testCaseIterations = 10;
        int _testCaseIterationDelay = 0;
        int _getsPerIteration = 1;
        int _updatesPerIteration = 1;
        int _dataSize = 1024;
        int _expiration = 300;
        int _threadCount = 1;
        int _reportingInterval = 5000;
        int _threadIndex = 0;

        int numErrors = 0;
        int maxErrors = 1000;

        public ThreadContainer(Cache cache, int totalLoopCount, int testCaseIterations, int testCaseIterationDelay, int getsPerIteration, int updatesPerIteration, int dataSize, int expiration, int threadCount, int reportingInterval, int threadIndex)
        {
            _cache = cache;
            _totalLoopCount = totalLoopCount;
            _testCaseIterations = testCaseIterations;
            _testCaseIterationDelay = testCaseIterationDelay;
            _getsPerIteration = getsPerIteration;
            _updatesPerIteration = updatesPerIteration;
            _dataSize = dataSize;
            _expiration = expiration;
            _threadCount = threadCount;
            _reportingInterval = reportingInterval;
            _threadIndex = threadIndex;
        }

        public void run() {
            try
            {
                DoGetInsert();
            }
            catch (Exception exp)
            {

            }

         }

        private String getCurrentDataTime()
        {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
          return dateFormat.format(date).toString();
        }
        private void DoGetInsert() throws Exception
        {
            byte[] data = new byte[_dataSize];

            if (_totalLoopCount <= 0)
            {
                for (long totalIndex = 0; ; totalIndex++)
                {
                    ProcessGetInsertIteration(data);
                    if (totalIndex > 1 && (totalIndex % _reportingInterval) == 0)
                    {
                        long count = _cache.getCount();
                        System.out.println(getCurrentDataTime() + ": Cache count: " + count);
                        totalIndex = 1;
                    }
                }
            }
            else
            {
                for (long totalIndex = 0; totalIndex < _totalLoopCount; totalIndex++)
                {
                    ProcessGetInsertIteration(data);
                    if (totalIndex > 1 && (totalIndex % _reportingInterval) == 0)
                    {
                        long count = _cache.getCount();
                        System.out.println(getCurrentDataTime() + ": Cache count: " + count);
                    }
                }
            }
        }

        private void ProcessGetInsertIteration(byte[] data) throws Exception
        {
            String guid = UUID.randomUUID().toString(); //create a unique key to be inserted in store.

            for (long testCaseIndex = 0; testCaseIndex < _testCaseIterations; testCaseIndex++)
            {
                String key = guid;

                for (int getsIndex = 0; getsIndex < _getsPerIteration; getsIndex++)
                {
                    try
                    {
                        Object obj = _cache.get(key);
                    }
                    catch (Exception e)
                    {
                        System.err.println("GET Error: Key: " + key + ", Exception: " + e.toString());
                        numErrors++;
                        if (this.numErrors > this.maxErrors)
                        {
                            System.err.println("Too many errors. Exiting...");
                            throw e;
                        }
                    }
                }

                for (int updatesIndex = 0; updatesIndex < _updatesPerIteration; updatesIndex++)
                {
                    try
                    {
                        _cache.insert(key, data,
                                                Cache.DefaultAbsoluteExpiration,
                                                new TimeSpan(0, 0, _expiration),
                                                CacheItemPriority.Default);
                    }
                    catch (Exception e)
                    {
                        System.err.println("INSERT Error: Key: " + key + ", Exception: " + e.toString());
                        numErrors++;
                        if (numErrors > 1000)
                        {
                            System.err.println("Too many errors. Exiting...");
                            throw e;
                        }
                    }
                }

                if (_testCaseIterationDelay > 0)
                {
                    // Sleep for this many seconds
                    Thread.sleep(_testCaseIterationDelay * 1000);
                }

            }
        }
}
