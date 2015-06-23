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

package com.alachisoft.tayzgrid.config.newdom;

import com.alachisoft.tayzgrid.common.configuration.ConfigurationAttributeAnnotation;
import com.alachisoft.tayzgrid.common.configuration.ConfigurationSectionAnnotation;
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class WriteBehind implements Cloneable, InternalCompactSerializable {

    private String mode, throttling, requeueLimit, eviction;
    private BatchConfig batchConfig;

    public WriteBehind() {
    }

    @ConfigurationAttributeAnnotation(value = "mode", appendText = "")
    public final String getMode() {
        return mode;
    }
    @ConfigurationAttributeAnnotation(value = "mode", appendText = "")
    public final void setMode(String value) {
        mode = value;
    }

    @ConfigurationAttributeAnnotation(value = "failed-operations-queue-limit", appendText = "")
    public final String getRequeueLimit() {
        return requeueLimit;
    }
    @ConfigurationAttributeAnnotation(value = "failed-operations-queue-limit", appendText = "")
    public final void setRequeueLimit(String value) {
        requeueLimit = value;
    }

    @ConfigurationAttributeAnnotation(value = "failed-operations-eviction-ratio", appendText = "%")
    public final String getEviction() {
        return eviction;
    }
    @ConfigurationAttributeAnnotation(value = "failed-operations-eviction-ratio", appendText = "%")
    public final void setEviction(String value) {
        eviction = value;
    }
    @ConfigurationAttributeAnnotation(value = "throttling-rate-per-sec", appendText = "")
    public final String getThrottling() {
        return throttling;
    }
    @ConfigurationAttributeAnnotation(value = "throttling-rate-per-sec", appendText = "")
    public final void setThrottling(String value) {
        throttling = value;
    }

    @ConfigurationSectionAnnotation(value = "batch-mode-config")
    public final BatchConfig getBatchConfig() {
        return batchConfig;
    }
    @ConfigurationSectionAnnotation(value = "batch-mode-config")
    public final void setBatchConfig(BatchConfig value) {
        batchConfig = value;
    }

    @Override
    public final Object clone() {
        WriteBehind writeBehind = new WriteBehind();
        writeBehind.setMode(getMode() != null ? new String(getMode()) : null);
        writeBehind.setThrottling(getThrottling() != null ? new String(getThrottling()) : null);
        writeBehind.setEviction(getEviction() != null ? new String(getEviction()) : null);
        writeBehind.setRequeueLimit(getRequeueLimit() != null ? new String(getRequeueLimit()) : null);
        Object tempVar = getBatchConfig().clone();
        writeBehind.setBatchConfig(getBatchConfig() != null ? (BatchConfig) ((tempVar instanceof BatchConfig) ? tempVar : null) : null);
        return writeBehind;
    }

    @Override
    public final void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        Object tempVar = reader.ReadObject();
        mode = (String) ((tempVar instanceof String) ? tempVar : null);
        Object tempVar2 = reader.ReadObject();
        throttling = (String) ((tempVar2 instanceof String) ? tempVar2 : null);
        Object tempVar3 = reader.ReadObject();
        batchConfig = (BatchConfig) ((tempVar3 instanceof BatchConfig) ? tempVar3 : null);
        Object tempVar4 = reader.ReadObject();
        eviction = (String) ((tempVar4 instanceof String) ? tempVar4 : null);
        Object tempVar5 = reader.ReadObject();
        requeueLimit = (String) ((tempVar5 instanceof String) ? tempVar5 : null);
    }

    @Override
    public final void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(mode);
        writer.WriteObject(throttling);
        writer.WriteObject(batchConfig);
        writer.WriteObject(eviction);
        writer.WriteObject(requeueLimit);
    }
}
