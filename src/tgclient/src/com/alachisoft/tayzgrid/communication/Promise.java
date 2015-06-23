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

package com.alachisoft.tayzgrid.communication;



import com.alachisoft.tayzgrid.common.threading.Monitor;

public class Promise {
    /// <summary>The result of the request</summary>
    Object _result=null;
    /// <summary>Used to wait on the result</summary>
    Object _mutex=new Object();
    
    /// <summary>
    /// If result was already submitted, returns it immediately, else blocks until
    /// results becomes available.
    /// </summary>
    /// <param name="timeout">Maximum time to wait for result.</param>
    /// <returns>Promise result</returns>
    public Object waitResult(long timeout) {
        Object ret=null;
        
        synchronized(_mutex) {
            if(_result != null) {
                ret=_result;
                _result=null;
                return ret;
            }
            if(timeout <= 0) {
                try {
                    Monitor.wait(_mutex);
                } catch(Exception ex) {
                }
            } else {
                try {
                     Monitor.wait(_mutex,timeout); 
                }catch(Exception ex) {
                    }
            }
            
            // SAL: Cosider Trace
            if(_result != null) {
                ret=_result;
                _result=null;
                return ret;
            }
            return null;
        }
    }
    
    /// <summary>
    /// Checks whether result is available. Does not block.
    /// </summary>
    /// <returns>Result if available</returns>
    public Object isResultAvailable() {
            synchronized(_mutex) {
                return _result;
            }
    }
    
    /// <summary>
    /// Sets the result and notifies any threads waiting for it
    /// </summary>
    /// <param name="obj">Result of request</param>
    public void setResult(Object obj) {
        synchronized(_mutex) {
            _result=obj;
            Monitor.pulse(_mutex);
        }
    }
    
    
    /// <summary>
    /// Clears the result and causes all waiting threads to return
    /// </summary>
    public void reset() {
        synchronized(_mutex) {
            _result=null;
            Monitor.pulse(_mutex);
        }
    }
    
    /// <summary>
    /// String representation of the result
    /// </summary>
    /// <returns>String representation of the result</returns>
    public String toString() {
        return "result=" + _result;
    }
    
}
