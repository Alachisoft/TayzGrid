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

public class RPCMethodCache<TargetObject> {
	private IRPCTargetObject<TargetObject> _targetObject;
	private java.util.Map<String, ITargetMethod> _cache = new java.util.HashMap<String, ITargetMethod>();

	public RPCMethodCache(IRPCTargetObject<TargetObject> targetObject) {
		_targetObject = targetObject;
		PopulateCache();
	}

	private void PopulateCache() {
		if (_targetObject != null) {
			ITargetMethod[] methods = _targetObject.GetAllMethods();

			for (int i = 0; i < methods.length; i++) {
				ITargetMethod method = methods[i];
				String key = GetCacheKey(method.GetMethodName(), method.GetOverlaod());
				if (!_cache.containsKey(key)) {
					_cache.put(key, method);
				} else {
					//throw new RuntimeException("Duplicate method exists in the target object. (Method :" + method.GetMethodName() + " , overlaod :" + method.GetOverlaod() + ")");
				}
			}
		}
	}



	private static String GetCacheKey(String methodName, int overload) {
		return methodName + "$" + overload;
	}

	public final ITargetMethod GetTargetMethod(String methodName, int overload) {
		String key = GetCacheKey(methodName, overload);

		if (_cache.containsKey(key)) {
			return _cache.get(key);
		} else {
			return null;
		}
	}

	public final IRPCTargetObject<TargetObject> GetTargetObject() {
		return _targetObject;
	}
}
