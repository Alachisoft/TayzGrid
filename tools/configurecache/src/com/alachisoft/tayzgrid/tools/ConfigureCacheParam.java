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
 * Summary description for ConfigureCacheTool.
 *
 *
 */
public class ConfigureCacheParam extends com.alachisoft.tayzgrid.tools.common.CommandLineParamsBase {

    private static String _cacheId = "";
    private static String _path = "";
    private static String _file = "";
    private static String _server = "";
    private static int _port = -1;
    private static int _socketPort = -1;

    private static int _range = 1; 
    private static String _mirrorNode = "";
    private static long _cacheSize = -1;
    private static String _evictionPolicy = "";
    private static java.math.BigDecimal _ratio = new java.math.BigDecimal(-1);
    private static int _cleanupInterval = -1;
    private static String _topology = "local-cache";
    private static String _repStrategy = "async";
 
    private static String _defPriority = "";
    private static boolean _isInProc  = false;

    /**
     * @return the _isInProc
     */
    @ArgumentAttributeAnnontation(shortNotation = "-I", fullNotation = "--inproc", appendText = "", defaultValue = "false")
    public  boolean getIsInProc() {
        return _isInProc;
    }

    /**
     * @param aIsInProc the _isInProc to set
     */
    @ArgumentAttributeAnnontation(shortNotation = "-I", fullNotation = "--inproc", appendText = "", defaultValue = "false")
    public  void setIsInProc(boolean aIsInProc) {
        _isInProc = aIsInProc;
    }
   
    
    
    
    /**
     * @return the _range
     */
    @ArgumentAttributeAnnontation(shortNotation = "-r", fullNotation = "--range", appendText = "", defaultValue = "")
    public final int getRange() {
        return _range;
    }

    /**
     * @param aRange the _range to set
     */
    @ArgumentAttributeAnnontation(shortNotation = "-r", fullNotation = "--range", appendText = "", defaultValue = "")
    public final void setRange(int aRange) {
        _range = aRange;
    }

    

    public ConfigureCacheParam() {
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final String getCacheId() {
        return _cacheId;
    }

    @ArgumentAttributeAnnontation(shortNotation = "", fullNotation = "", appendText = "", defaultValue = "")
    public final void setCacheId(String value) {
        _cacheId = value;
    }

    /**
     * @return the _socketPort
     */
    @ArgumentAttributeAnnontation(shortNotation = "-c", fullNotation = "--clientport", appendText = "", defaultValue = "")
    public final int getSocketPort() {
        return _socketPort;
    }

    /**
     * @param aSocketPort the _socketPort to set
     */
    @ArgumentAttributeAnnontation(shortNotation = "-c", fullNotation = "--clientport", appendText = "", defaultValue = "")
    public final void setSocketPort(int aSocketPort) {
        _socketPort = aSocketPort;
    }

    /**
     * @return the _managementPort
     */
    //@ArgumentAttributeAnnontation(shortNotation = "-m", fullNotation = "--managementport", appendText = "", defaultValue = "")
//    public final int getManagementPort() {
//        return _managementPort;
//    }

    /**
     * @param aManagementPort the _managementPort to set
     */
    //@ArgumentAttributeAnnontation(shortNotation = "-m", fullNotation = "--managementport", appendText = "", defaultValue = "")
//    public final void setManagementPort(int aManagementPort) {
//        _managementPort = aManagementPort;
//    }
    
    
    @ArgumentAttributeAnnontation(shortNotation = "-T", fullNotation = "--path", appendText = "", defaultValue = "")
    public final String getPath() {
        return _path;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-T", fullNotation = "--path", appendText = "", defaultValue = "")
    public final void setPath(String value) {
        _path = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-f", fullNotation = "--file", appendText = "", defaultValue = "")
    public final String getFileName() {
        return _file;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-f", fullNotation = "--file", appendText = "", defaultValue = "")
    public final void setFileName(String value) {
        _file = value;
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

    @ArgumentAttributeAnnontation(shortNotation = "-m", fullNotation = "--mirror-node", appendText = "", defaultValue = "")
    public final String getActiveMirrorNode() {
        return _mirrorNode;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-m", fullNotation = "--mirror-node", appendText = "", defaultValue = "")
    public final void setActiveMirrorNode(String value) {
        _mirrorNode = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-S", fullNotation = "--cache-size", appendText = "", defaultValue = "")
    public final long getCacheSize() {
        return _cacheSize;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-S", fullNotation = "--cache-size", appendText = "", defaultValue = "")
    public final void setCacheSize(long value) {
        _cacheSize = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-y", fullNotation = "--evit-policy", appendText = "", defaultValue = "")
    public final String getEvictionPolicy() {
        return _evictionPolicy;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-y", fullNotation = "--evit-policy", appendText = "", defaultValue = "")
    public final void setEvictionPolicy(String value) {
        _evictionPolicy = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-o", fullNotation = "--ratio", appendText = "", defaultValue = "")
    public final java.math.BigDecimal getRatio() {
        return _ratio;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-o", fullNotation = "--ratio", appendText = "", defaultValue = "")
    public final void setRatio(java.math.BigDecimal value) {
        _ratio = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-i", fullNotation = "--interval", appendText = "", defaultValue = "")
    public final int getCleanupInterval() {
        return _cleanupInterval;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-i", fullNotation = "--interval", appendText = "", defaultValue = "")
    public final void setCleanupInterval(int value) {
        _cleanupInterval = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-t", fullNotation = "--topology", appendText = "", defaultValue = "")
    public final String getTopology() {
        return _topology;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-t", fullNotation = "--topology", appendText = "", defaultValue = "")
    public final void setTopology(String value) {
        _topology = value;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-d", fullNotation = "--def-priority", appendText = "", defaultValue = "")
    public final String getDefaultPriority() {
        return _defPriority;
    }

    @ArgumentAttributeAnnontation(shortNotation = "-d", fullNotation = "--def-priority", appendText = "", defaultValue = "")
    public final void setDefaultPriority(String value) {
        _defPriority = value;
    }
}
