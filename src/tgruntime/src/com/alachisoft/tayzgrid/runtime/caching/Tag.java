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

package com.alachisoft.tayzgrid.runtime.caching;

/**
 * Represents an string based identifier that can be associated with the cache items so that they are logically grouped together and can be retrieved efficiently. One or more tags
 * can be associated with each cache item. To create an instance of Tag class you can use code as follows: Tag tag = new Tag("Alpha");
 */
public class Tag
{

    private String _tag;

    /// <summary>
    /// Initializes a new instance of Tag class.
    /// </summary>
    /// <param name="tag">@param
    public Tag(String tag)
    {
        _tag = tag;
    }

    /// <summary>
    /// Gets the string based tag name.
    /// </summary>
    public String getTagName()
    {
        return this._tag;
    }

    public void setTagName(String tag)
    {
        this._tag = tag;
    }

    /// <summary>
    /// String representation of the tag class.
    /// </summary>
    /// <returns></returns>
    public String ToString()
    {
        return _tag;
    }

    public boolean equals(Object obj)
    {
        if (obj.getClass() != this.getClass())
        {
            throw new IllegalArgumentException("Type mismatch");
        }
        return this._tag.equals(((Tag) obj)._tag);
    }

    public int getHashCode()
    {
        return _tag.hashCode();
    }
}

