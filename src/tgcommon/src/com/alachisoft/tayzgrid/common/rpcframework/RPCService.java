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

package com.alachisoft.tayzgrid.common.rpcframework;

import java.lang.reflect.InvocationTargetException;

public class RPCService<TargetObject> {
	private RPCMethodCache<TargetObject> _rpcCache;

	public RPCService(IRPCTargetObject<TargetObject> targetObject) {
		_rpcCache = new RPCMethodCache<TargetObject>(targetObject);
	}

	public final Object InvokeMethodOnTarget(String methodName, int overload, Object[] arguments) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		ITargetMethod targetMethod = _rpcCache.GetTargetMethod(methodName, overload);

		if (targetMethod == null) {
                    
			throw new InstantiationException("Target method not found (Method: " + methodName + " , overload : " + overload + ")");
		}

		if (targetMethod.GetNumberOfArguments() != arguments.length) {
			throw new IllegalAccessException();
		}

		Object returnVal = targetMethod.Invoke(arguments);

		return returnVal;
	}

}
