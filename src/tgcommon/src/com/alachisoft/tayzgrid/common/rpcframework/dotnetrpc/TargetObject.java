
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


import com.alachisoft.tayzgrid.common.rpcframework.TargetMethodAttribute;
import com.alachisoft.tayzgrid.common.rpcframework.IRPCTargetObject;
import com.alachisoft.tayzgrid.common.rpcframework.ITargetMethod;
import java.lang.annotation.Annotation;

public class TargetObject<Target> implements IRPCTargetObject<Target>
{

    private Target _target;

    public TargetObject(Target targetObject)
    {
        _target = targetObject;
    }

    public final Target GetTargetObject()
    {
        return _target;
    }

    public final ITargetMethod GetMethod(String methodName, int overload)
    {
        ITargetMethod targetMethod = null;
        if (_target != null)
        {
            java.lang.reflect.Method[] members = _target.getClass().getMethods();//GetMethods(BindingFlags.Instance | BindingFlags.Public);

            for (int i = 0; i < members.length; i++)
            {
                java.lang.reflect.Method member = members[i];

                Annotation[] customAttributes = member.getDeclaredAnnotations();;//member.GetCustomAttributes(TargetMethodAttribute.class, true);

                if (customAttributes != null && customAttributes.length > 0)
                {
                    TargetMethodAttribute targetMethodAttribute = (TargetMethodAttribute) ((customAttributes[0] instanceof TargetMethodAttribute) ? customAttributes[0] : null);

                    if (targetMethodAttribute.privateMethod().equals(methodName) && targetMethodAttribute.privateOverload() == overload)
                    {
                        targetMethod = new TargetMethod(_target, member, targetMethodAttribute.privateMethod(), targetMethodAttribute.privateOverload());
                    }
                }
            }
        }
        return targetMethod;
    }

    public final ITargetMethod[] GetAllMethods()
    {
        java.util.ArrayList<ITargetMethod> methods = new java.util.ArrayList<ITargetMethod>();
        if (_target != null)
        {
            java.lang.reflect.Method[] members = _target.getClass().getMethods(/*BindingFlags.Instance | BindingFlags.Public*/);

            for (int i = 0; i < members.length; i++)
            {
                java.lang.reflect.Method member = members[i];

                Annotation[] customAttributes = member.getDeclaredAnnotations();//member.GetCustomAttributes(TargetMethodAttribute.class, true);

                if (customAttributes != null && customAttributes.length > 0)
                {
                    TargetMethodAttribute targetMethodAttribute = (TargetMethodAttribute) ((customAttributes[0] instanceof TargetMethodAttribute) ? customAttributes[0] : null);
                    TargetMethod targetMethod = new TargetMethod(_target, member, targetMethodAttribute.privateMethod(), targetMethodAttribute.privateOverload());
                    methods.add(targetMethod);
                }
            }
            
            members = _target.getClass().getSuperclass().getMethods(/*BindingFlags.Instance | BindingFlags.Public*/);

            for (int i = 0; i < members.length; i++)
            {
                java.lang.reflect.Method member = members[i];

                Annotation[] customAttributes = member.getDeclaredAnnotations();//member.GetCustomAttributes(TargetMethodAttribute.class, true);

                if (customAttributes != null && customAttributes.length > 0)
                {
                    TargetMethodAttribute targetMethodAttribute = (TargetMethodAttribute) ((customAttributes[0] instanceof TargetMethodAttribute) ? customAttributes[0] : null);
                    TargetMethod targetMethod = new TargetMethod(_target, member, targetMethodAttribute.privateMethod(), targetMethodAttribute.privateOverload());
                    methods.add(targetMethod);
                }
            }
            
        }
        return methods.toArray(new ITargetMethod[0]);
    }
}
