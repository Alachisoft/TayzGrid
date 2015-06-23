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

package com.alachisoft.tayzgrid.config;

import com.alachisoft.tayzgrid.runtime.exceptions.ConfigurationException;
import java.util.HashMap;

public class PropsConfigReader extends ConfigReader
{

    private static class Tokenizer
    {
        private String _text;
        private String _token;
        private int _index = 0;
        public static final int EOF = -1;
        public static final int ASSIGN = 0;
        public static final int NEST = 1;
        public static final int UNNEST = 2;
        public static final int CONTINUE = 3;
        public static final int ID = 4;

        public Tokenizer(String text)
        {
            _text = text;
        }

        public final String getTokenValue()
        {
            return _token;
        }

        private String getIdentifier()
        {
            final String offendingStr = "=();";
            StringBuilder returnVal = new StringBuilder();

            if (_text.charAt(_index) == '\'')
            {
                _index++;
                while (_index < _text.length())
                {
                    if (_text.charAt(_index) == '\'')
                    {
                        _index++;
                        return returnVal.toString();
                    }
                    if (_text.charAt(_index) == '\r' || _text.charAt(_index) == '\n' || _text.charAt(_index) == '\t')
                    {
                        _index++;
                        continue;
                    }
                    returnVal.append(_text.charAt(_index++));
                }
                return null;
            }

            while (_index < _text.length())
            {
                if (_text.charAt(_index) == '\r' || _text.charAt(_index) == '\n' || _text.charAt(_index) == '\t')
                {
                    _index++;
                    continue;
                }
                if (offendingStr.indexOf(_text.charAt(_index)) != -1)
                {
                    return returnVal.toString();
                }
                returnVal.append(_text.charAt(_index++));
            }
            return null;
        }

        public final int getNextToken()
        {
            String trimStr = "=();";
            while (_index < _text.length())
            {
                if (trimStr.indexOf(_text.charAt(_index)) != -1)
                {
                    _token = ((Character)_text.charAt(_index)).toString();
                    return trimStr.indexOf(_text.charAt(_index++));
                }
                if (_text.charAt(_index) == '\r' || _text.charAt(_index) == '\n' || _text.charAt(_index) == '\t' || _text.charAt(_index) == ' ')
                {
                    _index++;
                    continue;
                }
                _token = getIdentifier();
                if (_token != null)
                {
                    return ID;
                }
            }
            return EOF;
        }
    }
    
    private String _propString;

    public PropsConfigReader(String propString)
    {
        _propString = propString;
    }

    public final String getPropertyString()
    {
        return _propString;
    }

    @Override
    public java.util.HashMap getProperties()
    {
        try
        {
            return GetProperties(_propString);
        }
        catch (ConfigurationException configurationException)
        {
            return null;
        }
    }

    public final String ToPropertiesXml() throws ConfigurationException
    {
        return ConfigReader.ToPropertiesXml(GetProperties(_propString));
    }

    private static enum State
    {
        keyNeeded,
        valNeeded;

        public int getValue()
        {
            return this.ordinal();
        }

        public static State forValue(int value)
        {
            return values()[value];
        }
    }

    private java.util.HashMap GetProperties(String propString) throws ConfigurationException
    {
        boolean uppercaseFlag = false;
        java.util.HashMap properties = new java.util.HashMap();
        Tokenizer tokenizer = new Tokenizer(propString);

        String key = "";
        int nestingLevel = 0;
        State state = State.keyNeeded;
        java.util.Stack stack = new java.util.Stack();

        int i =0;
        do
        {
            int token = tokenizer.getNextToken();
            switch (token)
            {
                case Tokenizer.EOF:
                    if (state != State.keyNeeded)
                    {
                        throw new ConfigurationException("Invalid EOF");
                    }
                    if (nestingLevel > 0)
                    {
                        throw new ConfigurationException("Invalid property string, un-matched paranthesis");
                    }
                    return properties;

                case Tokenizer.UNNEST:
                    if (state != State.keyNeeded)
                    {
                        throw new ConfigurationException("Invalid property string, ) misplaced");
                    }
                    if (nestingLevel < 1)
                    {
                        throw new ConfigurationException("Invalid property string, ) unexpected");
                    }
                    if (uppercaseFlag)
                    {
                        uppercaseFlag = false;
                    }
                    Object tempVar = stack.pop();
                    properties = (java.util.HashMap) ((tempVar instanceof java.util.HashMap) ? tempVar : null);
                    nestingLevel--;
                    break;

                case Tokenizer.ID:
                    switch (state)
                    {
                        case keyNeeded:
                            if (key.equals("parameters"))
                            {
                                uppercaseFlag = true;
                            }
                            key = tokenizer.getTokenValue();
                            token = tokenizer.getNextToken();

                            if (token == Tokenizer.CONTINUE || token == Tokenizer.UNNEST || token == Tokenizer.ID || token == Tokenizer.EOF)
                            {
                                throw new ConfigurationException("Invalid property string, key following a bad token");
                            }

                            if (token == Tokenizer.ASSIGN)
                            {
                                state = State.valNeeded;
                            }
                            else if (token == Tokenizer.NEST)
                            {
                                stack.push(properties);
                                properties.put(key.toLowerCase(), new HashMap());
                                properties = (java.util.HashMap) ((properties.get(key.toLowerCase()) instanceof java.util.HashMap) ? properties.get(key.toLowerCase()) : null);

                                state = State.keyNeeded;
                                nestingLevel++;
                            }
                            break;

                        case valNeeded:
                            String val = tokenizer.getTokenValue();
                            token = tokenizer.getNextToken();
                            state = State.keyNeeded;

                            if (token == Tokenizer.ASSIGN || token == Tokenizer.ID || token == Tokenizer.EOF)
                            {
                                throw new ConfigurationException("Invalid property string, value following a bad token");
                            }

                            if (uppercaseFlag)
                            {
                                properties.put(key, val);
                            }
                            else
                            {
                                properties.put(key.toLowerCase(), val);
                            }

                            if (token == Tokenizer.NEST)
                            {
                                stack.push(properties);
                                properties.put(key.toLowerCase(), new java.util.HashMap());
                                properties = (java.util.HashMap) ((properties.get(key.toLowerCase()) instanceof java.util.HashMap) ? properties.get(key.toLowerCase()) : null);

                                properties.put("id", key);
                                properties.put("type", val);

                                state = State.keyNeeded;
                                nestingLevel++;
                            }
                            else if (token == Tokenizer.UNNEST)
                            {
                                if (nestingLevel < 1)
                                {
                                    throw new ConfigurationException("Invalid property string, ) unexpected");
                                }
                                if (uppercaseFlag)
                                {
                                    uppercaseFlag = false;
                                }
                                Object tempVar2 = stack.pop();
                                properties = (java.util.HashMap) ((tempVar2 instanceof java.util.HashMap) ? tempVar2 : null);
                                nestingLevel--;
                                state = State.keyNeeded;
                            }
                            break;
                    }
                    break;

                default:
                    throw new ConfigurationException("Invalid property string");
            }
        }
        while (true);
    }
}
