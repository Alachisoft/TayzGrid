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

package com.alachisoft.tayzgrid.caching.util;
import com.alachisoft.tayzgrid.caching.queries.NCQLParser;
import com.alachisoft.tayzgrid.common.logger.ILogger;
import com.alachisoft.tayzgrid.parser.ParseMessage;
import com.alachisoft.tayzgrid.parser.Reduction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class ParserHelper
{

    private static final String ResourceName = "/com/alachisoft/tayzgrid/caching/queries/NCQL.cgt";
    private Reduction _currentReduction;
    private ILogger _ncacheLog;

    private ILogger getCacheLog()
    {
        return _ncacheLog;
    }

    public ParserHelper(ILogger NCacheLog)
    {
        this._ncacheLog = NCacheLog;

    }

    public final ParseMessage Parse(String query)
    {

        try
        {
            NCQLParser parser = new NCQLParser(ResourceName, _ncacheLog);
            BufferedReader tr = new BufferedReader(new StringReader(query));
            ParseMessage message = null;
            message = parser.Parse(tr, true);
            _currentReduction = parser.getCurrentReduction();
            return message;
        }
        catch (IOException iOException)
        {
        }

        return null;
    }

    public final Reduction getCurrentReduction()
    {
        return _currentReduction;
    }
}
