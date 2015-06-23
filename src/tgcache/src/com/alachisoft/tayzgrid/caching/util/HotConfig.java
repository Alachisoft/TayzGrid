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
package com.alachisoft.tayzgrid.caching.util;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;
import java.util.HashMap;

public class HotConfig implements InternalCompactSerializable {

    private boolean _isErrorLogsEnabled;
    private boolean _isDetailedLogsEnabled;
    private long _cacheMaxSize;
    private long _cleanInterval;
    private float _evictRatio;
    private boolean _securityEnabled;
    private String _securityDomainController;
    private String _securityPort;
    private java.util.HashMap _securityUsers;
    private java.util.HashMap _alertNotifier;

    private boolean _isTargetCache;

    /**
     * Regisered Backing Source.
     */
    private java.util.HashMap _backingSource;
    /**
     * Regisered CompactSerialization
     */
    private java.util.HashMap _compactSerialization;



    /**
     * Registered Backing Source.
     * @return 
     */
    public final java.util.HashMap getBackingSource() {
        return _backingSource;
    }

    public final void setBackingSource(java.util.HashMap value) {
        _backingSource = value;
    }

    public final java.util.HashMap getCompactSerialization() {
        return _compactSerialization;
    }

    public final void setCompactSerialization(java.util.HashMap value) {
        _compactSerialization = value;
    }

    public final boolean getSecurityEnabled() {
        return _securityEnabled;
    }

    public final void setSecurityEnabled(boolean value) {
        _securityEnabled = value;
    }

    public final String getSecurityDomainController() {
        return _securityDomainController;
    }

    public final void setSecurityDomainController(String value) {
        _securityDomainController = value;
    }

    public final java.util.HashMap getSecurityUsers() {
        return _securityUsers;
    }

    public final void setSecurityUsers(java.util.HashMap value) {
        _securityUsers = value;
    }

    public String getSecurityPort() {
        return _securityPort;
    }

    public void setSecurityPort(String _securityPort) {
        this._securityPort = _securityPort;
    }

    public final java.util.HashMap getAlertNotifier() {
        return _alertNotifier;
    }

    public final void setAlertNotifier(java.util.HashMap value) {
        _alertNotifier = value;
    }

    public final float getEvictRatio() {
        return _evictRatio;
    }

    public final void setEvictRatio(float value) {
        _evictRatio = value;
    }

    public final long getCacheMaxSize() {
        return _cacheMaxSize;
    }

    public final void setCacheMaxSize(long value) {
        _cacheMaxSize = value;
    }

    /**
     * Fatal anmd error logs.
     */
    public final boolean getIsErrorLogsEnabled() {
        return _isErrorLogsEnabled;
    }

    public final void setIsErrorLogsEnabled(boolean value) {
        _isErrorLogsEnabled = value;
    }

    /**
     * Info, warning, debug logs.
     */
    public final boolean getIsDetailedLogsEnabled() {
        return _isDetailedLogsEnabled;
    }

    public final void setIsDetailedLogsEnabled(boolean value) {
        _isDetailedLogsEnabled = value;
    }

    public final long getCleanInterval() {
        return _cleanInterval;
    }

    public final void setCleanInterval(long value) {
        _cleanInterval = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_isErrorLogsEnabled).append("\"");
        sb.append(_isDetailedLogsEnabled).append("\"");
        sb.append(_cacheMaxSize).append("\"");
        sb.append(_cleanInterval).append("\"");
        sb.append(_evictRatio).append("\"");
      
        return sb.toString();
    }

    public static HotConfig FromString(String attributes) {
        if (Common.isNullorEmpty(attributes)) {
            return null;
        }

        HotConfig config = new HotConfig();

        int beginQuoteIndex = 0;
        int endQuoteIndex = 0;

        tangible.RefObject<String> tempRef_attributes = new tangible.RefObject<String>(attributes);
        tangible.RefObject<Integer> tempRef_beginQuoteIndex = new tangible.RefObject<Integer>(beginQuoteIndex);
        tangible.RefObject<Integer> tempRef_endQuoteIndex = new tangible.RefObject<Integer>(endQuoteIndex);
        UpdateDelimIndexes(tempRef_attributes, '"', tempRef_beginQuoteIndex, tempRef_endQuoteIndex);
        attributes = tempRef_attributes.argvalue;
        beginQuoteIndex = tempRef_beginQuoteIndex.argvalue;
        endQuoteIndex = tempRef_endQuoteIndex.argvalue;
        String errorLogs = attributes.substring(beginQuoteIndex, endQuoteIndex);
        if (errorLogs != null && !errorLogs.equals("")) {
            config._isErrorLogsEnabled = Boolean.parseBoolean(errorLogs);
        }

        tangible.RefObject<String> tempRef_attributes2 = new tangible.RefObject<String>(attributes);
        tangible.RefObject<Integer> tempRef_beginQuoteIndex2 = new tangible.RefObject<Integer>(beginQuoteIndex);
        tangible.RefObject<Integer> tempRef_endQuoteIndex2 = new tangible.RefObject<Integer>(endQuoteIndex);
        UpdateDelimIndexes(tempRef_attributes2, '"', tempRef_beginQuoteIndex2, tempRef_endQuoteIndex2);
        attributes = tempRef_attributes2.argvalue;
        beginQuoteIndex = tempRef_beginQuoteIndex2.argvalue;
        endQuoteIndex = tempRef_endQuoteIndex2.argvalue;
        String detailedLogs = attributes.substring(beginQuoteIndex + 1, beginQuoteIndex + 1 + endQuoteIndex - beginQuoteIndex - 1);
        if (detailedLogs != null && !detailedLogs.equals("")) {
            config._isDetailedLogsEnabled = Boolean.parseBoolean(detailedLogs);
        }

        tangible.RefObject<String> tempRef_attributes3 = new tangible.RefObject<String>(attributes);
        tangible.RefObject<Integer> tempRef_beginQuoteIndex3 = new tangible.RefObject<Integer>(beginQuoteIndex);
        tangible.RefObject<Integer> tempRef_endQuoteIndex3 = new tangible.RefObject<Integer>(endQuoteIndex);
        UpdateDelimIndexes(tempRef_attributes3, '"', tempRef_beginQuoteIndex3, tempRef_endQuoteIndex3);
        attributes = tempRef_attributes3.argvalue;
        beginQuoteIndex = tempRef_beginQuoteIndex3.argvalue;
        endQuoteIndex = tempRef_endQuoteIndex3.argvalue;

        tangible.RefObject<String> tempRef_attributes4 = new tangible.RefObject<String>(attributes);
        tangible.RefObject<Integer> tempRef_beginQuoteIndex4 = new tangible.RefObject<Integer>(beginQuoteIndex);
        tangible.RefObject<Integer> tempRef_endQuoteIndex4 = new tangible.RefObject<Integer>(endQuoteIndex);
        UpdateDelimIndexes(tempRef_attributes4, '"', tempRef_beginQuoteIndex4, tempRef_endQuoteIndex4);
        attributes = tempRef_attributes4.argvalue;
        beginQuoteIndex = tempRef_beginQuoteIndex4.argvalue;
        endQuoteIndex = tempRef_endQuoteIndex4.argvalue;
        String threshold = attributes.substring(beginQuoteIndex + 1, beginQuoteIndex + 1 + endQuoteIndex - beginQuoteIndex - 1);


        tangible.RefObject<String> tempRef_attributes5 = new tangible.RefObject<String>(attributes);
        tangible.RefObject<Integer> tempRef_beginQuoteIndex5 = new tangible.RefObject<Integer>(beginQuoteIndex);
        tangible.RefObject<Integer> tempRef_endQuoteIndex5 = new tangible.RefObject<Integer>(endQuoteIndex);
        UpdateDelimIndexes(tempRef_attributes5, '"', tempRef_beginQuoteIndex5, tempRef_endQuoteIndex5);
        attributes = tempRef_attributes5.argvalue;
        beginQuoteIndex = tempRef_beginQuoteIndex5.argvalue;
        endQuoteIndex = tempRef_endQuoteIndex5.argvalue;
        String size = attributes.substring(beginQuoteIndex + 1, beginQuoteIndex + 1 + endQuoteIndex - beginQuoteIndex - 1);
        if (size != null && !size.equals("")) {
            config._cacheMaxSize = Long.parseLong(size);
        }

        tangible.RefObject<String> tempRef_attributes6 = new tangible.RefObject<String>(attributes);
        tangible.RefObject<Integer> tempRef_beginQuoteIndex6 = new tangible.RefObject<Integer>(beginQuoteIndex);
        tangible.RefObject<Integer> tempRef_endQuoteIndex6 = new tangible.RefObject<Integer>(endQuoteIndex);
        UpdateDelimIndexes(tempRef_attributes6, '"', tempRef_beginQuoteIndex6, tempRef_endQuoteIndex6);
        attributes = tempRef_attributes6.argvalue;
        beginQuoteIndex = tempRef_beginQuoteIndex6.argvalue;
        endQuoteIndex = tempRef_endQuoteIndex6.argvalue;
        String interval = attributes.substring(beginQuoteIndex + 1, beginQuoteIndex + 1 + endQuoteIndex - beginQuoteIndex - 1);
        if (interval != null && !interval.equals("")) {
            config._cleanInterval = Long.parseLong(interval);
        }

        tangible.RefObject<String> tempRef_attributes7 = new tangible.RefObject<String>(attributes);
        tangible.RefObject<Integer> tempRef_beginQuoteIndex7 = new tangible.RefObject<Integer>(beginQuoteIndex);
        tangible.RefObject<Integer> tempRef_endQuoteIndex7 = new tangible.RefObject<Integer>(endQuoteIndex);
        UpdateDelimIndexes(tempRef_attributes7, '"', tempRef_beginQuoteIndex7, tempRef_endQuoteIndex7);
        attributes = tempRef_attributes7.argvalue;
        beginQuoteIndex = tempRef_beginQuoteIndex7.argvalue;
        endQuoteIndex = tempRef_endQuoteIndex7.argvalue;
        String evict = attributes.substring(beginQuoteIndex + 1, beginQuoteIndex + 1 + endQuoteIndex - beginQuoteIndex - 1);
        if (evict != null && !evict.equals("")) {
            config._evictRatio = Float.parseFloat(evict);
        }

        tangible.RefObject<String> tempRef_attributes8 = new tangible.RefObject<String>(attributes);
        tangible.RefObject<Integer> tempRef_beginQuoteIndex8 = new tangible.RefObject<Integer>(beginQuoteIndex);
        tangible.RefObject<Integer> tempRef_endQuoteIndex8 = new tangible.RefObject<Integer>(endQuoteIndex);
        UpdateDelimIndexes(tempRef_attributes8, '"', tempRef_beginQuoteIndex8, tempRef_endQuoteIndex8);
        attributes = tempRef_attributes8.argvalue;
        beginQuoteIndex = tempRef_beginQuoteIndex8.argvalue;
        endQuoteIndex = tempRef_endQuoteIndex8.argvalue;
       
        return config;
    }

    private static void UpdateDelimIndexes(tangible.RefObject<String> attributes, char delim, tangible.RefObject<Integer> beginQuoteIndex, tangible.RefObject<Integer> endQuoteIndex) {
        beginQuoteIndex.argvalue = endQuoteIndex.argvalue;
        endQuoteIndex.argvalue = attributes.argvalue.indexOf(delim, beginQuoteIndex.argvalue + 1);
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        setIsErrorLogsEnabled(reader.ReadBoolean());
        setIsDetailedLogsEnabled(reader.ReadBoolean());
        setCacheMaxSize(reader.ReadInt64());
        setCleanInterval(reader.ReadInt64());
        setEvictRatio(reader.ReadSingle());
        setSecurityEnabled(reader.ReadBoolean());
        setSecurityDomainController((String)Common.as(reader.ReadObject(), String.class));
        setSecurityPort((String)Common.as(reader.ReadObject(), String.class));
        setSecurityUsers((HashMap)Common.as(reader.ReadObject(), java.util.HashMap.class));
        setAlertNotifier((HashMap)Common.as(reader.ReadObject(), java.util.HashMap.class));

    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.Write(getIsErrorLogsEnabled());
        writer.Write(getIsDetailedLogsEnabled());
        writer.Write(getCacheMaxSize());
        writer.Write(getCleanInterval());
        writer.Write(getEvictRatio());
        writer.Write(getSecurityEnabled());
        writer.WriteObject(getSecurityDomainController());
        writer.WriteObject(getSecurityPort());
        writer.WriteObject(getSecurityUsers());
        writer.WriteObject(getAlertNotifier());

    }
}
