/*
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

package com.alachisoft.tayzgrid.cluster;

import com.alachisoft.tayzgrid.cluster.blocks.ConnectInfo;
import com.alachisoft.tayzgrid.cluster.blocks.ConnectionTable;
import com.alachisoft.tayzgrid.cluster.blocks.RequestCorrelatorHDR;
import com.alachisoft.tayzgrid.cluster.MergeView;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.cluster.MessageTrace;
import com.alachisoft.tayzgrid.cluster.protocols.PingHeader;
import com.alachisoft.tayzgrid.cluster.protocols.PingRsp;
import com.alachisoft.tayzgrid.cluster.protocols.TOTAL;
import com.alachisoft.tayzgrid.cluster.protocols.TcpHeader;
import com.alachisoft.tayzgrid.cluster.protocols.pbcast.MergeData;
import com.alachisoft.tayzgrid.cluster.util.List;
import com.alachisoft.tayzgrid.cluster.View;
import com.alachisoft.tayzgrid.cluster.ViewId;
import com.alachisoft.tayzgrid.common.datastructures.HashMapBucket;
import com.alachisoft.tayzgrid.runtime.exceptions.NotSupportedException;
import com.alachisoft.tayzgrid.serialization.core.io.surrogates.CacheArgumentException;
import com.alachisoft.tayzgrid.serialization.standard.FormatterServices;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;


/**
 * Contains conversion support elements such as classes, interfaces and static methods.
 */
public class Global
{

    /**
     *
     * @throws CacheArgumentException
     */
    public static void registerCompactType() throws CacheArgumentException
    {
        FormatterServices impl = FormatterServices.getDefault();
        impl.registerKnownTypes(List.class, (short) 81);
        impl.registerKnownTypes(ViewId.class, (short) 82);
        impl.registerKnownTypes(View.class, (short) 83);
       
        impl.registerKnownTypes(PingRsp.class, (short) 85);
       
        impl.registerKnownTypes(com.alachisoft.tayzgrid.cluster.protocols.pbcast.Digest.class, (short) 87);
   
        impl.registerKnownTypes(Message.class, (short) 89);
        impl.registerKnownTypes(MergeView.class, (short) 90);
        impl.registerKnownTypes(MergeData.class, (short) 91);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.cluster.protocols.pbcast.JoinRsp.class, (short) 92);
        impl.registerKnownTypes(RequestCorrelatorHDR.class, (short) 93);
        impl.registerKnownTypes(TOTAL.HDR.class, (short) 94);
   
        impl.registerKnownTypes(com.alachisoft.tayzgrid.cluster.protocols.pbcast.GMS.HDR.class, (short) 98);
    
        impl.registerKnownTypes(PingHeader.class, (short) 103);
        impl.registerKnownTypes(TcpHeader.class, (short) 104);
        impl.registerKnownTypes(ConnectionTable.Connection.ConnectionHeader.class, (short) 108);

        impl.registerKnownTypes(HashMapBucket.class, (short) 114);


        impl.registerKnownTypes(com.alachisoft.tayzgrid.cluster.protocols.TCP.HearBeat.class, (short) 115);

        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.stats.HPTimeStats.class, (short) 126);
        impl.registerKnownTypes(com.alachisoft.tayzgrid.common.stats.HPTime.class, (short) 127);
        impl.registerKnownTypes(MessageTrace.class, (short) 128);
        impl.registerKnownTypes(ConnectInfo.class, (short) 137);
    }
    /**
     * ****************************
     */
    //Provides access to a static System.Random class instance
    public static java.util.Random Random = new java.util.Random();

    /**
     * ****************************
     */
    /**
     * This class provides functionality not found in .NET collection-related interfaces.
     */
    public static class ICollectionSupport
    {

        /**
         * Removes all the elements from the specified collection that are contained in the target collection.
         *
         * @param target Collection where the elements will be removed.
         * @param c Elements to remove from the target collection.
         * @return true
         */
        public static boolean RemoveAll(java.util.ArrayList target, java.util.ArrayList c) throws Exception
        {
            try
            {
                for (int i = 0; i < c.size(); i++)
                {
                    target.remove(c.get(i));
                }
            }
            catch (Exception ex)
            {
                throw ex;
            }
            return true;
        }

        /**
         * Retains the elements in the target collection that are contained in the specified collection
         *
         * @param target Collection where the elements will be removed.
         * @param c Elements to be retained in the target collection.
         * @return true
         */
        public static boolean RetainAll(java.util.ArrayList target, java.util.ArrayList c) throws Exception
        {
            try
            {
                for (int i = target.size() - 1; i >= 0; i--)
                {
                    if (!c.contains(target.get(i)))
                    {
                        target.remove(i);
                    }
                }
            }
            catch (Exception ex)
            {
                throw ex;
            }
            return true;
        }
    }

    /**
     * ****************************
     */
    /**
     * The class performs token processing in strings
     *
     *
     * This class breaks a string into set of tokens and returns them one by one
     *
     * Hasan Khan: Originally this class was written by someone else which highly relied upon use of exceptions for its functionality and since it is used in many places in the
     * code it could affect the performance of NCache. I have been asked to fix this performance bottleneck so I will rewrite this class.
     *
     * Design of this class is totally useless but I'm going to follow the old design for the sake of compatibility of rest of the code.
     *
     * Design flaws: ------------- 1) HasMoreTokens() works same as MoveNext 2) MoveNext() internally calls HasMoreTokens 3) Current calls NextToken 4) NextToken() gives the
     * current token 5) Count gives the number of remaining tokens
     */
    /*
     * : Implementation was changed to support the current functionality of the Tokenizer code wether the working of the class is absurd or not
     * Iterator implementation made are hasNext() -> HasMoreTokens() ### next() -> MoveNext() ### remove() added
     */
    public static class Tokenizer implements java.util.Iterator
    {

        private String text;
        private char[] delims;
        private String[] tokens;
        private int index;

        /**
         *
         * @param text
         * @param delimiters SEND ONE CHAR ONLY - haven't tested for multiple delimiters
         */
        public Tokenizer(String text, String delimiters)
        {
            this.text = text;
            delims = delimiters.toCharArray();

            /**
             * We do not need this function in 1x so conditional compiling it reason: StringSplitOptions.RemoveEmptyEntries is not defined in system assembly of .net 1x
             */


                tokens = text.split(delimiters);

                if(tokens.length == 1 && tokens[0].isEmpty())
                    tokens = new String[0];

            index = -1; // First call of MoveNext will put the pointer on right position.
        }

        @Override
        public final String next()
        {
            return tokens[index]; //Hasan: this is absurd
        }

        /**
         * Remaining tokens count
         * @return
         */
        public final int getCount()
        {
            if (index < tokens.length)
            {
                return tokens.length - index - 1;
            }
            else
            {
                return 0;
            }
        }

        /**
         * Determines if there are more tokens to return from text. Also moves the pointer to next token
         *
         * @return True if there are more tokens otherwise, false
         */
        //public final boolean HasMoreTokens()
        @Override
        public final boolean hasNext()
        { //Hasan: bad design
            if (index < tokens.length - 1)
            {
                index++;
                return true;
            }
            else
            {
                return false;
            }
        }


        /**
         * Performs the same action as NextToken
         * @return
         */
        public final Object getCurrent()
        {
            return next();
        }

        /**
         * Performs the same function as HasMoreTokens
         *
         * @return True if there are more tokens otherwise, false
         */
        public final boolean MoveNext()
        {
            return hasNext(); //Hasan: this is absurd
        }

        public final void Reset()
        {
            index = -1;
        }

        @Override
        @Deprecated
        public final void remove()
        {
            
        }

    }

    /**
     * Converts an array of bytes to an array of chars
     *
     * @param byteArray The array of bytes to convert
     * @return The new array of chars
     */
    public static char[] ToCharArray(byte[] byteArray)
    {
        //return System.Text.UTF8Encoding.UTF8.GetChars(byteArray);
        //Its UTF-8 not UTF8
        return new String(byteArray, 0, byteArray.length, Charset.forName("UTF-8")).toCharArray();
    }

    /**
     * ****************************
     */
    /**
     * Converts the specified collection to its string representation.
     *
     * @param c The collection to convert to string.
     * @return A string representation of the specified collection.
     */
    public static String CollectionToString(java.util.Collection c)
    {
        StringBuilder s = new StringBuilder();

        if (c != null)
        {

            java.util.ArrayList l = new java.util.ArrayList(c);

            boolean isDictionary = (c instanceof java.util.BitSet || c instanceof java.util.HashMap || c instanceof java.util.Map || (l.size()
                    > 0 && l.get(0) instanceof Map.Entry));
            for (int index = 0; index < l.size(); index++)
            {
                if (l.get(index) == null)
                {
                    s.append("null");
                }
                else if (!isDictionary)
                {
                    s.append(l.get(index));
                }
                else
                {
                    isDictionary = true;
                    if (c instanceof Map)
                    {
                        s.append(((Map) c).get(index));
                    }
                    else
                    {
                        s.append(((Map.Entry) l.get(index)).getKey());
                    }
                    s.append("=");
                    if (c instanceof Map)
                    {
                        s.append(((Map) c).values().toArray()[0]);
                    }
                    else
                    {
                        s.append(((Entry) l.get(index)).getValue());
                    }

                }
                if (index < l.size() - 1)
                {
                    s.append(", ");
                }
            }

            if (isDictionary)
            {
                if (c instanceof java.util.ArrayList)
                {
                    isDictionary = false;
                }
            }
            if (isDictionary)
            {
                s.insert(0, "{");
                s.append("}");
            }
            else
            {
                s.insert(0, "[");
                s.append("]");
            }
        }
        else
        {
            s.insert(0, "null");
        }
        return s.toString();
    }

    /**
     * Tests if the specified object is a collection and converts it to its string representation.
     *
     * @param obj The object to convert to string
     * @return A string representation of the specified object.
     */
    public static String CollectionToString(Object obj)
    {
        String result = "";

        if (obj != null)
        {
            if (obj instanceof java.util.Collection)
            {
                result = CollectionToString((java.util.Collection) obj);
            }
            else
            {
                result = obj.toString();
            }
        }
        else
        {
            result = "null";
        }

        return result;
    }

    public static String ArrayListToString(java.util.List list)
    {
        StringBuilder s = new StringBuilder();

        if (list != null)
        {
            s.append("[ ");

            for (Object item : list)
            {
                s.append(item.toString() + ",");
            }
            s.deleteCharAt(s.length());
            s.append(" ]");
        }
        else
        {
            s.append("<null>");
        }
        return s.toString();
    }
}
