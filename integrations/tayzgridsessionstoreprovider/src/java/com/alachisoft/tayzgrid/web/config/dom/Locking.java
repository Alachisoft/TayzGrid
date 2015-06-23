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

package com.alachisoft.tayzgrid.web.config.dom;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import java.io.Serializable;

public class Locking implements Serializable {

    private boolean _enableLocking;
    private int _retryCount;
    private int _retryInterval;
    private int _lockTimeout;

    @ConfigurationAttributeAnnotation(value = "enable-session-locking", appendText = "")
    public final boolean getEnableLocking() {
        return _enableLocking;
    }

    @ConfigurationAttributeAnnotation(value = "enable-session-locking", appendText = "")
    public final void setEnableLocking(boolean value) {
        _enableLocking = value;
    }

    @ConfigurationAttributeAnnotation(value = "retries-count", appendText = "")
    public final int getRetriesCount() {
        return _retryCount;
    }

    @ConfigurationAttributeAnnotation(value = "retries-count", appendText = "")
    public final void setRetriesCount(int value) {
        _retryCount = value;
    }

    @ConfigurationAttributeAnnotation(value = "retry-interval", appendText = "ms")
    public final int getRetryInterval() {
        return _retryInterval;
    }

    @ConfigurationAttributeAnnotation(value = "retry-interval", appendText = "ms")
    public final void setRetryInterval(int value) {
        _retryInterval = value;
    }

    @ConfigurationAttributeAnnotation(value = "lock-timeout", appendText = "ms")
    public final int getLockTimeout() {
        return _lockTimeout;
    }

    @ConfigurationAttributeAnnotation(value = "lock-timeout", appendText = "ms")
    public final void setLockTimeout(int value) {
        _lockTimeout = value;
    }

    @ConfigurationAttributeAnnotation(value = "empty-session-when-locked", appendText = "")
    public final boolean getEmptySessions() {
        return _enableLocking;
    }

    @ConfigurationAttributeAnnotation(value = "empty-session-when-locked", appendText = "")
    public final void setEmptySessions(boolean value) {
        _enableLocking = value;
    }
}
