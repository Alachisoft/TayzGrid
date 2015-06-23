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
package com.alachisoft.tayzgrid.samples;

import javax.cache.integration.CompletionListener;

public class CompletionEvent implements CompletionListener
{

    public int _eventCount = 0;
    public int _completeCount = 0;
    public int _expCount = 0;

    public boolean _isCompleted = false;
    public Exception _exp = null;

    @Override
    public void onCompletion()
    {
        this._eventCount++;
        this._completeCount++;
        this._isCompleted = true;
        PrintEventInfo();
    }

    @Override
    public void onException(Exception excptn)
    {
        this._eventCount++;
        this._expCount++;
        this._isCompleted = false;
        this._exp = excptn;
        PrintEventInfo();
    }

    public void VerifyCompletionEvent(int eventCount, int compeleteCount, int expCount, boolean isCompleted) throws Exception
    {
        if (this._eventCount != eventCount)
        {
            throw new Exception(eventCount + " event(s) expected instead of " + this._eventCount);
        }
        if (this._completeCount != compeleteCount)
        {
            throw new Exception(compeleteCount + " event(s) expected instead of " + this._completeCount);
        }
        if (this._expCount != expCount)
        {
            throw new Exception(expCount + " event(s) expected instead of " + this._expCount);
        }
        if (this._isCompleted != isCompleted)
        {
            throw new Exception("Completed is " + this._isCompleted + " instead of " + isCompleted);
        }
        
        if (isCompleted == true)
        {
            if (this._exp != null)
            {
                throw new Exception("Completed is TURE but exp is not NULL");
            }
        }
        else
        {
            if (this._exp == null)
            {
                throw new Exception("Completed is false but exp is NULL");
            }
            if(this._exp instanceof UnsupportedOperationException)
            {
                throw new Exception(this._exp.getClass().getSimpleName() + " is thrown instead of UnsupportedOperationException");
            }
            if(this._exp.getMessage().contains("Loading Key from loader failed.") == false)
                throw new Exception("Exp message is miss match.");
        }
    }

    public void Reset()
    {
        _eventCount = 0;
        _completeCount = 0;
        _expCount = 0;
        _isCompleted = false;
        _exp = null;
    }

    protected void PrintEventInfo()
    {
        System.out.println("EVENT is called by cache loader.");
    }
}
