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

import com.alachisoft.tayzgrid.serialization.core.io.ICompactSerializable;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import java.io.IOException;

public class TargetMethodParameter implements ICompactSerializable
{

    private java.util.ArrayList _parameterList = new java.util.ArrayList();

    public final java.util.ArrayList getParameterList()
    {
        return _parameterList;
    }

    public final void TargetMethodArguments()
    {
        _parameterList = new java.util.ArrayList();
    }

    public final void TargetMethodArguments(java.util.ArrayList parameter)
    {
        _parameterList = parameter;
    }

    public final void AddParameter(Object parameter)
    {
        _parameterList.add(parameter);
    }

    //<editor-fold defaultstate="collapsed" desc="ICompactSerializable Members">
    public final void deserialize(CacheObjectInput reader) throws ClassNotFoundException, IOException
    {
        _parameterList = new java.util.ArrayList();
         int length = reader.readInt();

             boolean isByteArray = false;
            for (int i = 0; i < length; i++)
            {
                isByteArray = reader.readBoolean();
                if (isByteArray)
                {
                    int count = reader.readInt();
                    byte[] buffer = new byte[count];
                    reader.readFully(buffer);
                    _parameterList.add(buffer);
                }
                else
                    _parameterList.add(reader.readObject());
            }
        
    }

    public final void serialize(CacheObjectOutput writer) throws IOException
    {
        writer.writeInt(_parameterList.size());
        boolean isByteArray = false;
        for(int i =0; i< _parameterList.size(); i++)
        {
            isByteArray = _parameterList.get(i) != null && _parameterList.get(i).getClass() == byte[].class;
            writer.writeBoolean(isByteArray);
            if(isByteArray)
            {
                byte[] buffer = (byte[])_parameterList.get(i);
                writer.writeInt(buffer.length);
                writer.write(buffer);
            }
            else
                writer.writeObject(_parameterList.get(i));
        }
        
    }
    //</editor-fold>
}
