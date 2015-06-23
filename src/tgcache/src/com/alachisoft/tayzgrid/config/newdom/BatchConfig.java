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
import com.alachisoft.tayzgrid.serialization.core.io.InternalCompactSerializable;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactReader;
import com.alachisoft.tayzgrid.serialization.standard.io.CompactWriter;
import java.io.IOException;

public class BatchConfig implements Cloneable, InternalCompactSerializable
{

    private String batchInterval, operationDelay;

    public BatchConfig() {
    }

    @ConfigurationAttributeAnnotation(value = "batch-interval", appendText = "s")
    public final String getBatchInterval() {
        return batchInterval;
    }

    @ConfigurationAttributeAnnotation(value = "batch-interval", appendText = "s")
    public final void setBatchInterval(String value) {
        batchInterval = value;
    }

    @ConfigurationAttributeAnnotation(value = "operation-delay", appendText = "ms")
    public final String getOperationDelay() {
        return operationDelay;
    }

    @ConfigurationAttributeAnnotation(value = "operation-delay", appendText = "ms")
    public final void setOperationDelay(String value) {
        operationDelay = value;
    }

    @Override
    public final Object clone() {
        BatchConfig config = new BatchConfig();
        config.setBatchInterval(getBatchInterval() != null ? new String(getBatchInterval()) : null);
        config.setOperationDelay(getOperationDelay() != null ? new String(getOperationDelay()) : null);
        return config;
    }

    @Override
    public final void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        Object tempVar = reader.ReadObject();
        batchInterval = (String) ((tempVar instanceof String) ? tempVar : null);
        Object tempVar2 = reader.ReadObject();
        operationDelay = (String) ((tempVar2 instanceof String) ? tempVar2 : null);
    }

    @Override
    public final void Serialize(CompactWriter writer) throws IOException {
        writer.WriteObject(batchInterval);
        writer.WriteObject(operationDelay);
    }

}
