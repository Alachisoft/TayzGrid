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


package com.alachisoft.tayzgrid.serialization.standard;

import com.alachisoft.tayzgrid.serialization.standard.io.ObjectInputStream;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectOutputStream;
import java.io.*;

public class CompactBinaryFormatter
{

    /**
     *
     * @param graph
     * @param cacheContext
     * @return
     * @throws IOException
     */
    public static byte[] toByteBuffer(Object graph, String cacheContext) throws IOException
    {
        try
        {
            ByteArrayOutputStream val = new ByteArrayOutputStream();
            ObjectOutput ow = new ObjectOutputStream(val, cacheContext);
            ow.writeObject(graph);
            ow.flush();
            return val.toByteArray();
        }
        catch (IOException iOException)
        {
            try
            {
                ByteArrayOutputStream val = new ByteArrayOutputStream();
                ObjectOutput ow = new ObjectOutputStream(val, cacheContext);
                ow.writeObject(graph);
                ow.flush();
                return val.toByteArray();
            }
            catch (IOException iOException2)
            {

                throw iOException2;
            }
        }
    }

    /**
     *
     * @param graph
     * @param cacheContext
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object fromByteBuffer(byte[] graph, String cacheContext) throws IOException, ClassNotFoundException
    {
        try
        {
            ByteArrayInputStream val = new ByteArrayInputStream(graph);
            ObjectInput ow = new ObjectInputStream(val, cacheContext);
            return ow.readObject();
        }
        catch (IOException iOException)
        {

            try
            {
                ByteArrayInputStream val = new ByteArrayInputStream(graph);
                ObjectInput ow = new ObjectInputStream(val, cacheContext);
                return ow.readObject();
            }
            catch (IOException iOException1)
            {
                throw iOException;
            }
            catch (ClassNotFoundException classNotFoundException)
            {
                throw classNotFoundException;
            }
        }
        catch (ClassNotFoundException classNotFoundException)
        {
            throw classNotFoundException;
        }
    }

    public static void writeToStream(Object graph, OutputStream out, String cacheContext) throws IOException
    {
        try
        {
            ObjectOutput ow = new ObjectOutputStream(out, cacheContext);
            ow.writeObject(graph);
            ow.flush();
        }
        catch (IOException iOException)
        {
            try
            {
                ObjectOutput ow = new ObjectOutputStream(out, cacheContext);
                ow.writeObject(graph);
                ow.flush();
            }
            catch (IOException iOException2)
            {
                throw iOException2;
            }
        }
    }
}
