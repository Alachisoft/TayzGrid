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

import com.alachisoft.tayzgrid.tools.common.ArgumentAttributeAnnontation;

public class StressTestToolParam extends com.alachisoft.tayzgrid.tools.common.CommandLineParamsBase {

    private String _cacheId = null;
    private int _totalLoopCount = 0;
    private int _testCaseIterations = 20;
    private int _testCaseIterationDelay = 0;
    private int _getsPerIteration = 1;
    private int _updatesPerIteration = 1;
    private int _dataSize = 1024;
    private int _expiration = 60;
    private int _threadCount = 1;
    private int _reportingInterval = 5000;

    public StressTestToolParam() {
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final void setCacheId(String value) {
        this._cacheId = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final String getCacheId() {
        return this._cacheId;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-n", fullNotation = "--item-count", appendText = "", defaultValue = "")
    public final void setTotalLoopCount(int value) {
        this._totalLoopCount = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-n", fullNotation = "--item-count", appendText = "", defaultValue = "")
    public final int getTotalLoopCount() {
        return this._totalLoopCount;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-i", fullNotation = "--test-case-iterations", appendText = "", defaultValue = "")
    public final void setTestCaseIterations(int value) {
        this._testCaseIterations = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-i", fullNotation = "--test-case-iterations", appendText = "", defaultValue = "")
    public final int getTestCaseIterations() {
        return this._testCaseIterations;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-d", fullNotation = "--test-case-iteration-delay", appendText = "", defaultValue = "")
    public final void setTestCaseIterationDelay(int value) {
        this._testCaseIterationDelay = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-d", fullNotation = "--test-case-iteration-delay", appendText = "", defaultValue = "")
    public final int getTestCaseIterationDelay() {
        return this._testCaseIterationDelay;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-g", fullNotation = "--gets-per-iteration", appendText = "", defaultValue = "")
    public final void setGetsPerIteration(int value) {
        this._getsPerIteration = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-g", fullNotation = "--gets-per-iteration", appendText = "", defaultValue = "")
    public final int getGetsPerIteration() {
        return this._getsPerIteration;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-u", fullNotation = "--updates-per-iteration", appendText = "", defaultValue = "")
    public final void setUpdatesPerIteration(int value) {
        this._updatesPerIteration = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-u", fullNotation = "--updates-per-iteration", appendText = "", defaultValue = "")
    public final int getUpdatesPerIteration() {
        return this._updatesPerIteration;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-m", fullNotation = "--item-size", appendText = "", defaultValue = "")
    public final void setDataSize(int value) {
        this._dataSize = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-m", fullNotation = "--item-size", appendText = "", defaultValue = "")
    public final int getDataSize() {
        return this._dataSize;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-e", fullNotation = "--sliding-expiration", appendText = "", defaultValue = "")
    public final void setExpiration(int value) {
        this._expiration = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-e", fullNotation = "--sliding-expiration", appendText = "", defaultValue = "")
    public final int getExpiration() {
        return this._expiration;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-t", fullNotation = "--thread-count", appendText = "", defaultValue = "")
    public final void setThreadCount(int value) {
        this._threadCount = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-t", fullNotation = "--thread-count", appendText = "", defaultValue = "")
    public final int getThreadCount() {
        return this._threadCount;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-R", fullNotation = "--reporting-interval", appendText = "", defaultValue = "")
    public final void setReportingInterval(int value) {
        this._reportingInterval = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-R", fullNotation = "--reporting-interval", appendText = "", defaultValue = "")
    public final int getReportingInterval() {
        return this._reportingInterval;
    }
}
