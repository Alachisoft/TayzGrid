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

/*
 * EndianBits.java
 *
 * Created on September 10, 2008, 10:28 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.alachisoft.tayzgrid.serialization.core.io;

/**
 *
 * @author 
 */
public interface EndianBits {
    boolean getBoolean(byte[] b, int off);

    char getChar(byte[] b, int off);

    double getDouble(byte[] b, int off);

    float getFloat(byte[] b, int off);

    int getInt(byte[] b, int off);

    long getLong(byte[] b, int off);

    short getShort(byte[] b, int off);

    void putBoolean(byte[] b, int off, boolean val);

    void putChar(byte[] b, int off, char val);

    void putDouble(byte[] b, int off, double val);

    void putFloat(byte[] b, int off, float val);

    void putInt(byte[] b, int off, int val);

    void putLong(byte[] b, int off, long val);

    void putShort(byte[] b, int off, short val);
    
}
