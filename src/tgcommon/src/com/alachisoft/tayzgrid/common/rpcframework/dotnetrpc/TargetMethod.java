
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


package com.alachisoft.tayzgrid.common.rpcframework.dotnetrpc;


import com.alachisoft.tayzgrid.common.rpcframework.ITargetMethod;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TargetMethod implements ITargetMethod {
	private java.lang.reflect.Method _method;
	private String _specifiedMethodName;
	private int _overlaod;
	private Object _target;

	public TargetMethod(Object targetObject, java.lang.reflect.Method methodInfo, String methodName, int overload) {
		_target = targetObject;
		_method = methodInfo;
		_specifiedMethodName = methodName;
		_overlaod = overload;
	}

	public final String GetMethodName() {
		return _specifiedMethodName;
	}

	public final int GetOverlaod() {
		return _overlaod;
	}

	public final Object GetMethodReflectionInfo() {
		return _method;
	}

	public final int GetNumberOfArguments() {
		return _method.getParameterTypes().length;
	}

        @Override
	public final Object Invoke(Object[] arguments) throws IllegalAccessException,IllegalArgumentException,InvocationTargetException {
            Object val = null;
            val = _method.invoke(_target, arguments);
        
            return val;
    }
}
