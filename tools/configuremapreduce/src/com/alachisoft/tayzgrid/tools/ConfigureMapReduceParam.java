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

/**
 * Summary description for ConfigureBackingSource.
 *
 *
 */
public class ConfigureMapReduceParam extends com.alachisoft.tayzgrid.tools.common.CommandLineParamsBase {

    private static String _cacheId = "";
    private static int _maxTasks = 0;
    private static int _chunkSize = 0;
    private static int _queueSize = 0;
    private static int _maxExceptions = 0;
    private static String _server = "";
    private static int _port = -1;
    
    public ConfigureMapReduceParam()
    {}
    
    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final String getCacheId() {
        return _cacheId;
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final void setCacheId(String value) {
        _cacheId = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-s", fullNotation = "--server", appendText = "", defaultValue = "")
    public final String getServer() {
        return _server;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-s", fullNotation = "--server", appendText = "", defaultValue = "")
    public final void setServer(String value) {
        _server = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-p", fullNotation = "--port", appendText = "", defaultValue = "")
    public final int getPort() {
        return _port;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-p", fullNotation = "--port", appendText = "", defaultValue = "")
    public final void setPort(int value) {
        _port = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-m", fullNotation = "--maxTasks", appendText = "", defaultValue = "")
    public final int getMaxTasks() {
        return _maxTasks;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-m", fullNotation = "--maxTasks", appendText = "", defaultValue = "")
    public final void setMaxTasks(int value) {
        _maxTasks = value;
    }
        @ArgumentAttributeAnnontation(shortNotation = "-C", fullNotation = "--chunk", appendText = "", defaultValue = "")
    public final int getChunkSize() {
        return _chunkSize;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-C", fullNotation = "--chunk", appendText = "", defaultValue = "")
    public final void setChunkSize(int value) {
        _chunkSize = value;
    }
        @ArgumentAttributeAnnontation(shortNotation = "-q", fullNotation = "--queue", appendText = "", defaultValue = "")
    public final int getQueueSize() {
        return _queueSize;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-q", fullNotation = "--queue", appendText = "", defaultValue = "")
    public final void setQueueSize(int value) {
        _queueSize = value;
    }
        @ArgumentAttributeAnnontation(shortNotation = "-E", fullNotation = "--exception", appendText = "", defaultValue = "")
    public final int getMaxExceptions() {
        return _maxExceptions;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-E", fullNotation = "--exception", appendText = "", defaultValue = "")
    public final void setMaxExceptions(int value) {
        _maxExceptions = value;
    }
}
