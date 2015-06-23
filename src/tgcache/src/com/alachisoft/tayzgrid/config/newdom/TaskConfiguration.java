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


public class TaskConfiguration implements Cloneable, InternalCompactSerializable {

    private int maxTasks = 10;
    private int chunkSize = 100;
    private boolean communicateStats = false;
    private int queueSize = 10;
    private int maxExceptions = 10;

    @ConfigurationAttributeAnnotation(value = "max-tasks", appendText = "")
    public int getMaxTasks() {
        return maxTasks;
    }
    
    @ConfigurationAttributeAnnotation(value = "max-tasks", appendText = "")
    public void setMaxTasks(int maxTasks) {
        this.maxTasks = maxTasks;
    }

    @ConfigurationAttributeAnnotation(value = "chunk-size", appendText = "")
    public int getChunkSize() {
        return chunkSize;
    }

    @ConfigurationAttributeAnnotation(value = "chunk-size", appendText = "")
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @ConfigurationAttributeAnnotation(value = "communicate-stats", appendText = "")
    public boolean isCommunicateStats() {
        return communicateStats;
    }

    @ConfigurationAttributeAnnotation(value = "communicate-stats", appendText = "")
    public void setCommunicateStats(boolean communicateStats) {
        this.communicateStats = communicateStats;
    }

    @ConfigurationAttributeAnnotation(value = "queue-size", appendText = "")
    public int getQueueSize() {
        return queueSize;
    }

    @ConfigurationAttributeAnnotation(value = "queue-size", appendText = "")
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
    
    @ConfigurationAttributeAnnotation(value = "max-avoidable-exceptions", appendText = "")
    public int getMaxExceptions() {
        return maxExceptions;
    }

    @ConfigurationAttributeAnnotation(value = "max-avoidable-exceptions", appendText = "")
    public void setMaxExceptions(int maxExceptions) {
        this.maxExceptions = maxExceptions;
    }
    
    @Override
    public TaskConfiguration clone()
    {
        TaskConfiguration conf = new TaskConfiguration();
        conf.setChunkSize(getChunkSize());
        conf.setMaxTasks(getMaxTasks());
        conf.setCommunicateStats(isCommunicateStats());
        conf.setQueueSize(getQueueSize());
        conf.setMaxExceptions(getMaxExceptions());
        return conf;
    }

    @Override
    public void Deserialize(CompactReader reader) throws IOException, ClassNotFoundException {
        this.maxTasks = reader.ReadInt32();
        this.chunkSize = reader.ReadInt32();
        this.communicateStats = reader.ReadBoolean();
        this.queueSize = reader.ReadInt32();
        this.maxExceptions = reader.ReadInt32();
    }

    @Override
    public void Serialize(CompactWriter writer) throws IOException {
        writer.Write(maxTasks);
        writer.Write(chunkSize);
        writer.Write(communicateStats);
        writer.Write(queueSize);
        writer.Write(maxExceptions);
    }
}
