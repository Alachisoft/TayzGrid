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

import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectInput;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.core.io.TypeSurrogateSelector;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheIOException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheInstantiationException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogate;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.SerializationSurrogateImpl;
import java.util.HashMap;

public class DynamicSurrogateBuilder
{

    public DynamicSurrogateBuilder()
    {
        
    }

    static SerializationSurrogate createTypeSurrogate(TypeSurrogateSelector mSurrogateTypeSelector, Class cls, HashMap attributeOrder, String cacheContext, boolean portable, short subHandle,HashMap nonCompactFieldsMap) throws Exception
    {
        Class surrogate;
        return (SerializationSurrogate)SurrogateHelper.CreateGenericTypeInstance(mSurrogateTypeSelector, cls, attributeOrder, cacheContext, portable, subHandle,nonCompactFieldsMap);
    }

    /// <summary>
        /// Generates a <see cref="WriteObjectDelegate"/> method for serializing an object of
        /// given <paramref name="type"/>.
        /// </summary>
        /// <param name="type">The type of the object to be serialized</param>
        /// <returns>A dynamically generated delegate that serializes <paramref name="type"/> object</returns>
        static Object CreateWriterDelegate(Class type)
        {
            return null;
        }

        public static Object GetAllFields(Class type, Object list)
        {
            return null;
        }

        static void EmitWriterMethod(Class type, Object il)
        {
        }

        private static void EmitWriteInstruction(Object field, Object il)
        {
        }

        static Object CreateReaderDelegate(Class type)
        {
            return null;
        }

        static void EmitReaderMethod(Class type, Object il)
        {
        }

        private static void EmitReadInstruction(Object field, Object il)
        {
        }

}

        
