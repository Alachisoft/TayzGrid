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

package com.alachisoft.tayzgrid.web.caching.apilogging;

import com.alachisoft.tayzgrid.util.ClientConfigurations;
import java.util.Date;

public final class DebugAPIConfigurations {
    
    private static int s_timeBeforeLoggingStart = 0;
    private static int s_numberOfIterations = 0;
    private static int s_durationOfEachIteration = 0;
    private static int s_intervalBetweenIterations = 0;
    private static boolean s_loggingEnabled = false;
    private static int s_loggerThreadLoggingInterval = 5;
    /**
     * Indicates that all logging intervals has been passed.
     */
    private boolean _loggingExpired = false;
    private  java.util.Date _loggingStartTime;

    static {
        loadConfiguration();
    }
    
    public DebugAPIConfigurations()
    {
        _loggingStartTime=new Date();
        _loggingStartTime.setTime(_loggingStartTime.getTime()+s_timeBeforeLoggingStart*1000);
    }

    public boolean isLoggingExpired()
    {
        return _loggingExpired;
    }
    public static int getTimeBeforeLoggingStart()
    {
        return s_timeBeforeLoggingStart;
    }
    
    public static void setTimeBeforeLoggingStart(int value)
    {
        s_timeBeforeLoggingStart=value;
    }
    
    public static int getLoggerThreadLoggingInterval()
    {
        return s_loggerThreadLoggingInterval;
    }

    public static int getNumberOfIterations() {
        return s_numberOfIterations;
    }

    public static void setNumberOfIterations(int value) {
        s_numberOfIterations = value;
    }

    public static int getDurationOfEachIteration() {
        return s_durationOfEachIteration;
    }

    public static void setDurationOfEachIteration(int value) {
        s_durationOfEachIteration = value;
    }

    public static int getIntervalBetweenIterations() {
        return s_intervalBetweenIterations;
    }

    public static void setIntervalBetweenIterations(int value) {
        s_intervalBetweenIterations = value;
    }

    public static boolean isLoggingEnabled() {
        return s_loggingEnabled;
    }

    public static void setLoggingEnabled(boolean value) {
        s_loggingEnabled = value;
    }

    private static void loadConfiguration() {
        s_loggingEnabled = ClientConfigurations.getDebugApiLoggingEnabled();
        s_timeBeforeLoggingStart=ClientConfigurations.getDebugApiTimeBeforeLoggingStart();
        s_numberOfIterations = ClientConfigurations.getDebugApiNumberOfIterations();
        s_durationOfEachIteration = ClientConfigurations.getDebugApiDurationOfEachIteration();
        s_intervalBetweenIterations = ClientConfigurations.getDebugApiIntervalBetweenIterations();
        s_loggerThreadLoggingInterval = ClientConfigurations.getDebugApiLoggerThreadLoggingInterval();
    }

    public boolean isInLoggingInterval() {
        if ((!s_loggingEnabled) || _loggingExpired) {
            return false;
        }
        long normalizedSpan = (new java.util.Date()).getTime() - _loggingStartTime.getTime();
        double normalizedSeconds = ((double) normalizedSpan) / 1000;
        if (normalizedSeconds < 0) {
            return false;
        }

        int completeIntervalLength = s_durationOfEachIteration + s_intervalBetweenIterations;
        if (completeIntervalLength == 0 || ((normalizedSeconds / (s_numberOfIterations * completeIntervalLength)) >= 1)) {
            _loggingExpired = true;
            return false;
        }

        double fraction = normalizedSeconds - (int) Math.floor(normalizedSeconds);
        int normalizedToInterval = (int) Math.floor(normalizedSeconds) % completeIntervalLength;
        double timePassedInCurrentIteration = (double) normalizedToInterval + fraction;
        if (timePassedInCurrentIteration >= s_durationOfEachIteration) {
            return false;
        }
        return true;
    }

    public int GetIterationNumber(Date loggingTime) {
        long normalizedSpan = loggingTime.getTime() - _loggingStartTime.getTime();
        double normalizedSeconds = ((double) normalizedSpan) / 1000;
        double loggingInterval = s_durationOfEachIteration + s_intervalBetweenIterations;
        int iterationNumber=1+(int) Math.floor(normalizedSeconds / loggingInterval);
        if(iterationNumber>s_numberOfIterations)
            iterationNumber=s_numberOfIterations;
        return iterationNumber;
    }
}
