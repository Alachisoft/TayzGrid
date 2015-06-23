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
public class ConfigureBackingSourceParam extends com.alachisoft.tayzgrid.tools.common.CommandLineParamsBase {

    private static String _asmPath = "";
    private static String _class = "";
    private static String _parameters;
    private static String _cacheId = "";
    private static boolean _readthru = false;
    private static boolean _writethru = false;
    private static String _server = "";
    private static int _port = -1;
    private static String _providorName = "";
    private static boolean _async = false;
    private static boolean _isdefault = false;
    private static boolean _nodeploy = false;
    private static String _depAsmPath = "";
    private static boolean _isBatching = false;
    private static int _operationDelay = 500;
    private static int _batchInterval = 5;
    private static int _operationPerSecond = 1;
    private static int _operationQueueLimit = 5000;
    private static int _operationEvictionRatio = 5;
    
    private static boolean  _loaderOnly = false;

    /**
     * @return the _isBatching
     */
     @ArgumentAttributeAnnontation(shortNotation = "-b", fullNotation = "--IsBatching", appendText = "", defaultValue = "")
    public static boolean isIsBatching() {
        return _isBatching;
    }

    /**
     * @param aIsBatching the _isBatching to set
     */
     
     @ArgumentAttributeAnnontation(shortNotation = "-b", fullNotation = "--IsBatching ", appendText = "", defaultValue = "")
    public static void setIsBatching(boolean aIsBatching) {
        _isBatching = aIsBatching;
    }

    /**
     * @return the _operationDelay
     */
     
    @ArgumentAttributeAnnontation(shortNotation = "-od", fullNotation = "--Operation-delay", appendText = "", defaultValue = "")
    public static int getOperationDelay() {
        return _operationDelay;
    }

    /**
     * @param aOperationDelay the _operationDelay to set
     */
     
     @ArgumentAttributeAnnontation(shortNotation = "-od", fullNotation = "--Operation-delay", appendText = "", defaultValue = "")
    public static void setOperationDelay(int aOperationDelay) {
        _operationDelay = aOperationDelay;
    }

    /**
     * @return the _batchInterval
     */
     
     @ArgumentAttributeAnnontation(shortNotation = "-bi", fullNotation = "--Batch-interval", appendText = "", defaultValue = "")
    public static int getBatchInterval() {
        return _batchInterval;
    }

    /**
     * @param aBatchInterval the _batchInterval to set
     */
     
     @ArgumentAttributeAnnontation(shortNotation = "-bi", fullNotation = "--Batch-interval", appendText = "", defaultValue = "")
    public static void setBatchInterval(int aBatchInterval) {
        _batchInterval = aBatchInterval;
    }

    /**
     * @return the _operationPerSecond
     */
     
     @ArgumentAttributeAnnontation(shortNotation = "-ops", fullNotation = "--Operation-per-second", appendText = "", defaultValue = "")
    public static int getOperationPerSecond() {
        return _operationPerSecond;
    }

    /**
     * @param aOperationPerSecond the _operationPerSecond to set
     */
     
     @ArgumentAttributeAnnontation(shortNotation = "-ops", fullNotation = "--Operation-per-second", appendText = "", defaultValue = "")
    public static void setOperationPerSecond(int aOperationPerSecond) {
        _operationPerSecond = aOperationPerSecond;
    }

    /**
     * @return the _operationQueueLimit
     */
     
     @ArgumentAttributeAnnontation(shortNotation = "-oql", fullNotation = "--Operation-Queue-Limit", appendText = "", defaultValue = "")
    public static int getOperationQueueLimit() {
        return _operationQueueLimit;
    }

    /**
     * @param aOperationQueueLimit the _operationQueueLimit to set
     */
     
     @ArgumentAttributeAnnontation(shortNotation = "-oql", fullNotation = "--Operation-Queue-Limit", appendText = "", defaultValue = "")
    public static void setOperationQueueLimit(int aOperationQueueLimit) {
        _operationQueueLimit = aOperationQueueLimit;
    }

    /**
     * @return the _operationEvictionRatio
     */
     
     @ArgumentAttributeAnnontation(shortNotation = "-oer", fullNotation = "--Operation-Eviction-Ratio", appendText = "", defaultValue = "")
    public static int getOperationEvictionRatio() {
        return _operationEvictionRatio;
    }

    /**
     * @param aOperationEvictionRatio the _operationEvictionRatio to set
     */
     
     @ArgumentAttributeAnnontation(shortNotation = "-oer", fullNotation = "--Operation-Eviction-Ratio", appendText = "", defaultValue = "")
    public static void setOperationEvictionRatio(int aOperationEvictionRatio) {
        _operationEvictionRatio = aOperationEvictionRatio;
    }

    public ConfigureBackingSourceParam() {
    }

    @ArgumentAttributeAnnontation(shortNotation = "-a", fullNotation = "--assembly-path", appendText = "", defaultValue = "")
    public final String getAsmPath() {
        return _asmPath;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-a", fullNotation = "--assembly-path", appendText = "", defaultValue = "")
    public final void setAsmPath(String value) {
        _asmPath = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final String getCacheId() {
        return _cacheId;
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final void setCacheId(String value) {
        _cacheId = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-c", fullNotation = "--class", appendText = "", defaultValue = "")
    public final String GetClass() {
        return _class;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-c", fullNotation = "--class", appendText = "", defaultValue = "")
    public final void SetClass(String value) {
        _class = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-l", fullNotation = "--parameter-list", appendText = "", defaultValue = "")
    public final String getParameters() {
        return _parameters;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-l", fullNotation = "--parameter-list", appendText = "", defaultValue = "")
    public final void setParameters(String value) {
        _parameters = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-r", fullNotation = "--readthru", appendText = "", defaultValue = "false")
    public final boolean getIsReadThru() {
        return _readthru;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-r", fullNotation = "--readthru", appendText = "", defaultValue = "false")
    public final void setIsReadThru(boolean value) {
        _readthru = value;
    }
    
    @ArgumentAttributeAnnontation(shortNotation = "-lo", fullNotation = "--loaderOnly", appendText = "", defaultValue = "false")
    public final boolean  getIsLoaderOnly() {
        return _loaderOnly;
    }
    
    @ArgumentAttributeAnnontation(shortNotation = "-lo", fullNotation = "--loaderOnly", appendText = "", defaultValue = "false")
    public final void  setIsLoaderOnly(boolean loaderOnly) {
        _loaderOnly = loaderOnly;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-w", fullNotation = "--writethru", appendText = "", defaultValue = "false")
    public final boolean getIsWriteThru() {
        return _writethru;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-w", fullNotation = "--writethru", appendText = "", defaultValue = "false")
    public final void setIsWriteThru(boolean value) {
        _writethru = value;
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

    @ArgumentAttributeAnnontation(shortNotation = "-n", fullNotation = "--provider-name", appendText = "", defaultValue = "")
    public final String getProviderName() {
        return _providorName;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-n", fullNotation = "--provider-name", appendText = "", defaultValue = "")
    public final void setProviderName(String value) {
        _providorName = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-b", fullNotation = "--write-behind", appendText = "", defaultValue = "false")
    public final boolean getAsync() {
        return _async;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-b", fullNotation = "--write-behind", appendText = "", defaultValue = "false")
    public final void setAsync(boolean value) {
        _async = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-d", fullNotation = "--no-deploy", appendText = "", defaultValue = "false")
    public final boolean getNoDeploy() {
        return _nodeploy;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-d", fullNotation = "--no-deploy", appendText = "", defaultValue = "false")
    public final void setNoDeploy(boolean value) {
        _nodeploy = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-D", fullNotation = "--dep-asm-path", appendText = "", defaultValue = "")
    public final String getDepAsmPath() {
        return _depAsmPath;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-D", fullNotation = "--dep-asm-path", appendText = "", defaultValue = "")
    public final void setDepAsmPath(String value) {
        _depAsmPath = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-t", fullNotation = "--default", appendText = "", defaultValue = "false")
    public final boolean getDefaultProvider() {
        return _isdefault;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-t", fullNotation = "--default", appendText = "", defaultValue = "false")
    public final void setDefaultProvider(boolean value) {
        _isdefault = value;
    }
}
