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
package com.alachisoft.tayzgrid.caching.topologies.clustered.mirroring;

import com.alachisoft.tayzgrid.common.mirroring.CacheNode;
import com.alachisoft.tayzgrid.common.EventArgs;

public class BackupMovedEventArgs extends EventArgs {

    private CacheNode node;
    private String lastBackup;

    /**
     * This nodes backup has changed or moved.
     */
    public final CacheNode getAffectedNode() {
        return node;
    }

    /**
     * The Last backupNodeId where this nodes mirror existed and is now changed.
     */
    public final String getLastBackup() {
        return lastBackup;
    }

    public BackupMovedEventArgs(CacheNode affectedNode, String lastBackup) {
        node = affectedNode;
        this.lastBackup = lastBackup;
    }
}
