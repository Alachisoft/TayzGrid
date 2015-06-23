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

package com.alachisoft.tayzgrid.integrations.memcached.proxyserver.executionmanagement;

import com.alachisoft.tayzgrid.integrations.memcached.provider.CacheFactory;
import com.alachisoft.tayzgrid.integrations.memcached.provider.exceptions.CacheRuntimeException;
import com.alachisoft.tayzgrid.integrations.memcached.provider.exceptions.InvalidArgumentsException;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.commands.AbstractCommand;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.ModuleStatus;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.LogManager;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.MemConfiguration;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.common.Opcode;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.networkgateway.TcpNetworkGateway;
import com.alachisoft.tayzgrid.integrations.memcached.proxyserver.threading.ThreadPool;


public class SequentialExecutionManager extends ExecutionManager {

    private java.util.LinkedList<AbstractCommand> _commandsQueue = new java.util.LinkedList<AbstractCommand>();
    private boolean _alive = false;

    @Override
    public ModuleStatus RegisterCommand(AbstractCommand command) {
        if (command == null) {
            return ModuleStatus.Running;
        }
        synchronized (this) {
            _commandsQueue.offer(command);
            if (_alive) {
                return ModuleStatus.Running;
            }
            return ModuleStatus.Idle;
        }
    }

    @Override
    public void Start() {
        try {
            synchronized (this) {
                if (_alive || _commandsQueue.isEmpty()) {
                    return;
                }
                _alive = true;
            }
            StartSequentialExecution();
        } catch (RuntimeException e) {
            LogManager.getLogger().Error("SequentialExecutionManager", " Failed to start sequential execution manager. Exception: " + e.getMessage());
            return;
        }
    }

    @Override
    public void run() {
        try {
            StartSequentialExecution();
        } catch (RuntimeException e) {
            LogManager.getLogger().Error("SequentialExecutionManager", " Failed to start sequential execution manager. Exception: " + e.getMessage());
            return;
        }
    }
    
    private boolean ProviderRequired(Opcode opcode)
    {
        switch(opcode)
        {
            case No_op:
            case Invalid_Command:
            case unknown_command:
            case Quit:
            case QuitQ:
                return false;
            default:
                return true;
        }
    }

    public final void StartSequentialExecution() {
        boolean go = false;

        do {
            AbstractCommand command;
            synchronized (this) {
                command = _commandsQueue.poll();
            }
            try {
                if(!ProviderRequired(command.getOpcode()) || (_cacheProvider = CacheFactory.createCacheProvider(MemConfiguration.getCacheName()))!=null)
                {
                    command.Execute(_cacheProvider);
                }
                else
                {
                    LogManager.getLogger().Fatal("Configured cache is not responding. Please verify that cache id is correct and cache is running.");
                    command.setExceptionOccured(true);
                    command.setErrorMessage("Configured cache is not responding. Please verify that cache id is correct and cache is running.");
                    command.setDisposeClient(true);
                }
            } catch (InvalidArgumentsException e) {
                LogManager.getLogger().Error("SequentialExecutionManager", "\tError while executing command. CommandType = " + command.getOpcode().toString() + "  " + e);
                command.setExceptionOccured(true);
                command.setErrorMessage(e.getMessage());
            } catch (CacheRuntimeException e) {
                LogManager.getLogger().Error("SequentialExecutionManager", "\tError while executing command. CommandType = " + command.getOpcode().toString() + "  " + e);
                command.setExceptionOccured(true);
                command.setErrorMessage(e.getMessage());
            }

            TcpNetworkGateway.s_executionTimeStats.EndSample();
            TcpNetworkGateway.s_responseTimeStats.BeginSample();
            ModuleStatus responseMgrStatus = _commandConsumer.RegisterCommand(command);

            synchronized (this) {
                go = (responseMgrStatus == ModuleStatus.Running) && _commandsQueue.size() > 0;
            }
        } while (go);


        synchronized (this) {
            if (_commandsQueue.size() > 0) {
                ThreadPool.ExecuteTask(this);
            } else {
                _alive = false;
            }
        }

        if (_commandConsumer != null) {
            _commandConsumer.Start();
        }
    }

    private void StartSequentialExecution(Object obj) {
        this.StartSequentialExecution();
    }
}