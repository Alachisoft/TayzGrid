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

package com.alachisoft.tayzgrid.cluster.util;

import com.alachisoft.tayzgrid.cluster.blocks.ExtSocketException;
import com.alachisoft.tayzgrid.cluster.blocks.RequestCorrelatorHDR;
import com.alachisoft.tayzgrid.cluster.Event;
import com.alachisoft.tayzgrid.cluster.Global;
import com.alachisoft.tayzgrid.cluster.HeaderType;
import com.alachisoft.tayzgrid.cluster.Message;
import com.alachisoft.tayzgrid.common.FlagsByte;
import com.alachisoft.tayzgrid.serialization.core.io.CacheObjectOutput;
import com.alachisoft.tayzgrid.serialization.standard.CompactBinaryFormatter;
import com.alachisoft.tayzgrid.serialization.standard.io.ObjectOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;

// $Id: Util.java,v 1.13 2004/07/28 08:14:14 belaban Exp $
 
public class Util
{
    // constants

    public static final int MAX_PORT = 65535; // highest port allocatable

    /**
     * Finds first available port starting at start_port and returns server socket
     */
    public static ServerSocket createServerSocket(int start_port)
    {
       ServerSocket ret = null;

        while (true)
        {
            try
            {
                ServerSocket temp_tcpListener;
                temp_tcpListener = new ServerSocket(start_port);
               
                ret = temp_tcpListener;
            }
            catch (BindException bex)
            {
                
                start_port++;
                continue;
            }
            catch (IOException io_ex)
            {
                
            }
            break;
        }
        return ret;
    }

    /**
     * Returns all members that left between 2 views. All members that are element of old_mbrs but not element of new_mbrs are returned.
     */
    public static java.util.List determineLeftMembers(java.util.ArrayList old_mbrs, java.util.ArrayList new_mbrs)
    {
        java.util.List retval = Collections.synchronizedList(new java.util.ArrayList(10));
        Object mbr;

        if (old_mbrs == null || new_mbrs == null)
        {
            return retval;
        }

        for (int i = 0; i < old_mbrs.size(); i++)
        {
            mbr = old_mbrs.get(i);
            if (!new_mbrs.contains(mbr))
            {
                retval.add(mbr);
            }
        }

        return retval;
    }

    /**
     * Sleep for timeout msecs. Returns when timeout has elapsed or thread was interrupted
     */
    public static void sleep(long timeout) throws InterruptedException
    {
        Thread.sleep(timeout);
    }

 
    public static byte[] serializeMessage(Message msg) throws IOException
    {
        int len = 0;
 
        byte[] buffie;
        FlagsByte flags = new FlagsByte();

        msg.setDest(null);
        msg.setDests(null);

        RequestCorrelatorHDR rqHeader = (RequestCorrelatorHDR) msg.getHeader(HeaderType.REQUEST_COORELATOR);

        if (rqHeader != null)
        {
            rqHeader.serializeFlag = false;
        }

        ByteArrayOutputStream stmOut = new ByteArrayOutputStream();
        stmOut.reset();
        ObjectOutput msgWriter = new ObjectOutputStream(stmOut, "");
        msgWriter.write(Util.WriteInt32(len), 0, 4);
        msgWriter.write(Util.WriteInt32(len), 0, 4);

        if (msg.getIsUserMsg())
        {
           
            flags.SetOn(FlagsByte.Flag.TRANS);
            msgWriter.write(flags.getDataByte());
            msg.SerializeLocal(msgWriter);

        }
        else
        {
            flags.SetOff(FlagsByte.Flag.TRANS);
            msgWriter.write(flags.getDataByte());

            msgWriter.write(CompactBinaryFormatter.toByteBuffer(msg, ""));
 
        }
        msgWriter.flush();


        byte[] data = stmOut.toByteArray();
    
        len = data.length - 4;

        int payloadLength = 0;
        // the user payload size. payload is passed on untill finally send on the socket.
        if (msg.getPayload() != null)
        {
            for (int i = 0; i < msg.getPayload().length; i++)
            {
                payloadLength += ((byte[]) msg.getPayload()[i]).length;
            }
            len += payloadLength;
        }

      
        byte[] val = Util.WriteInt32(len);
        byte[] val1 = Util.WriteInt32(len - 4 - payloadLength);
        for (int i = 0; i < 4; i++)
        {
            data[i] = val[i];
            data[i+4] = val1[i];
        }
   
        buffie = new byte[len + 4 - payloadLength];
        System.arraycopy(data, 0, buffie, 0, len + 4 - payloadLength);
    

        return buffie;
    }

    /**
     * On most UNIX systems, the minimum sleep time is 10-20ms. Even if we specify sleep(1), the thread will sleep for at least 10-20ms. On Windows, sleep() seems to be implemented
     * as a busy sleep, that is the thread never relinquishes control and therefore the sleep(x) is exactly x ms long.
     */
    public static void sleep(long msecs, boolean busy_sleep) throws InterruptedException
    {
        if (!busy_sleep)
        {
            sleep(msecs);
            return;
        }

        //changing to System.currentTimeinMillis
        //long start = (new java.util.Date().Ticks - 621355968000000000) / 10000;
        long start = System.currentTimeMillis();
        long stop = start + msecs;

        while (stop > start)
        {
            
            start = System.currentTimeMillis();
        }
    }

    /**
     * Returns a random value in the range [1 - range]
     */
    public static long random(long range)
    {
        return (long) ((Global.Random.nextDouble() * 100000) % range) + 1;
    }

    /**
     * E.g. 2000,4000,8000
     */
    public static long[] parseCommaDelimitedLongs(String s)
    {
        if (s == null)
        {
            return null;
        }

        String[] v = s.split("[,]", -1);
        if (v.length == 0)
        {
            return null;
        }

        long[] retval = new long[v.length];
        for (int i = 0; i < v.length; i++)
        {
            retval[i] = Long.parseLong(v[i].trim());
        }
        return retval;
    }

    /**
     * Selects a random subset of members according to subset_percentage and returns them. Picks no member twice from the same membership. If the percentage is smaller than 1 ->
     * picks 1 member.
     */
    public static java.util.List pickSubset(java.util.ArrayList members, double subset_percentage)
    {
        java.util.List ret = Collections.synchronizedList(new java.util.ArrayList(10)), tmp_mbrs;
        int num_mbrs = members.size(), subset_size, index;

        if (num_mbrs == 0)
        {
            return ret;
        }
        subset_size = (int) Math.ceil(num_mbrs * subset_percentage);

        tmp_mbrs = (java.util.ArrayList) members.clone();

        for (int i = subset_size; i > 0 && tmp_mbrs.size() > 0; i--)
        {
            index = (int) ((Global.Random.nextDouble() * num_mbrs) % tmp_mbrs.size());
            ret.add(tmp_mbrs.get(index));
            tmp_mbrs.remove(index);
        }

        return ret;
    }

 
    /**
     * Tries to read an object from the message's buffer and prints it
     */
    public static String printMessage(Message msg)
    {
        if (msg == null)
        {
            return "";
        }
        if (msg.getLength() == 0)
        {
            return null;
        }

        try
        {
            return msg.getObject().toString();
        }
        catch (Exception ex)
        {
            
            // it is not an object
            return "";
        }
    }

    public static String printEvent(Event evt)
    {
        Message msg;

        if (evt.getType() == Event.MSG)
        {
            msg = (Message) evt.getArg();
            if (msg != null)
            {
                if (msg.getLength() > 0)
                {
                    return printMessage(msg);
                }
                else
                {
                    return msg.printObjectHeaders();
                }
            }
        }
        return evt.toString();
    }
 

 
    /**
     * Fragments a byte buffer into smaller fragments of (max.) frag_size. Example: a byte buffer of 1024 bytes and a frag_size of 248 gives 4 fragments of 248 bytes each and 1
     * fragment of 32 bytes.
     *
     * @return An array of byte buffers (
     * <code>byte[]</code>).
     *
     */
 
    public static byte[][] fragmentBuffer(byte[] buf, int frag_size)
    {
 
        byte[][] retval;
        long total_size = buf.length;
        int accumulated_size = 0;
 
        byte[] fragment;
        int tmp_size = 0;
        int num_frags;
        int index = 0;

        num_frags = buf.length % frag_size == 0 ? buf.length / frag_size : buf.length / frag_size + 1;
        retval = new byte[num_frags][];

        while (accumulated_size < total_size)
        {
            if (accumulated_size + frag_size <= total_size)
            {
                tmp_size = frag_size;
            }
            else
            {
                tmp_size = (int) (total_size - accumulated_size);
            }
            fragment = new byte[tmp_size];
            System.arraycopy(buf, accumulated_size, fragment, 0, tmp_size);
            retval[index++] = fragment;
            accumulated_size += tmp_size;
        }
        return retval;
    }

    /**
     * Given a buffer and a fragmentation size, compute a list of fragmentation offset/length pairs, and return them in a list. Example:<br/> Buffer is 10 bytes, frag_size is 4
     * bytes. Return value will be ({0,4}, {4,4}, {8,2}). This is a total of 3 fragments: the first fragment starts at 0, and has a length of 4 bytes, the second fragment starts at
     * offset 4 and has a length of 4 bytes, and the last fragment starts at offset 8 and has a length of 2 bytes.
     *
     * @param frag_size
     *
     * @return List. A List<Range> of offset/length pairs
     *
     */
    public static java.util.List computeFragOffsets(int offset, int length, int frag_size)
    {
        java.util.List retval = new java.util.ArrayList();
        long total_size = length + offset;
        int index = offset;
        int tmp_size = 0;
        Range r;

        while (index < total_size)
        {
            if (index + frag_size <= total_size)
            {
                tmp_size = frag_size;
            }
            else
            {
                tmp_size = (int) (total_size - index);
            }
            r = new Range(index, tmp_size);
            retval.add(r);
            index += tmp_size;
        }
        return retval;
    }

 
    public static java.util.List computeFragOffsets(byte[] buf, int frag_size)
    {
        return computeFragOffsets(0, buf.length, frag_size);
    }

    /**
     * Concatenates smaller fragments into entire buffers.
     *
     * @param fragments An array of byte buffers (
     * <code>byte[]</code>)
     *
     * @return A byte buffer
     *
     */
 
    public static byte[] defragmentBuffer(byte[][] fragments)
    {
        int total_length = 0;
 
        byte[] ret;
        int index = 0;

        if (fragments == null)
        {
            return null;
        }
        for (int i = 0; i < fragments.length; i++)
        {
            if (fragments[i] == null)
            {
                continue;
            }
            total_length += fragments[i].length;
        }
        ret = new byte[total_length];
        for (int i = 0; i < fragments.length; i++)
        {
            if (fragments[i] == null)
            {
                continue;
            }
            System.arraycopy(fragments[i], 0, ret, index, fragments[i].length);
            index += fragments[i].length;
        }
        return ret;
    }

 
    public static void printFragments(byte[][] frags)
    {
        for (int i = 0; i < frags.length; i++)
        {
             
        }
    }
  

    public static String shortName(String hostname)
    {
        int index;
        StringBuilder sb = new StringBuilder();

        if (hostname == null)
        {
            return null;
        }

        index = hostname.indexOf((char) '.');
        if (index > 0 && !Character.isDigit(hostname.charAt(0)))
        {
            sb.append(hostname.substring(0, (index) - (0)));
        }
        else
        {
            sb.append(hostname);
        }
        return sb.toString();
    }
   

    /**
     * Reads a number of characters from the current source Stream and writes the data to the target array at the specified index.
     *
     * @param sourceStream The source Stream to read from.
     * @param target Contains the array of characteres read from the source Stream.
     * @param start The starting index of the target array.
     * @param count The maximum number of characters to read from the source Stream.
     * @return The number of characters read. The number will be less than or equal to count depending on the data available in the source Stream. Returns -1 if the end of the
     * stream is reached.
     */
    public static int ReadInput(Socket sock, byte[] target, int start, int count) throws IOException
    {
        
        if (target.length == 0)
        {
            return 0;
        }

       
        int bytesRead, totalBytesRead = 0, buffStart = start;

        while (true)
        {
            try
            {
                if (!sock.isConnected())
                {
                    throw new ExtSocketException("socket closed");
                }
                bytesRead = sock.getInputStream().read(target, start, count);
              

                if (bytesRead <= 0)
                {
                    throw new ExtSocketException("socket closed");
                }

                totalBytesRead += bytesRead;
                if (bytesRead == count)
                {
                    break;
                }
                else
                {
                    count = count - bytesRead;
                }

                start = start + bytesRead;
            }catch(IOException ioe){
                
                throw ioe;
            }
             
        }

        // Returns -1 if EOF
        if (totalBytesRead == 0)
        {
            return -1;
        }
 
        return totalBytesRead;
    }

 
    public static int ReadInput(Socket sock, byte[] target, int start, int count, int min) throws IOException
    {
        
        if (target.length == 0)
        {
            return 0;
        }

       
        int bytesRead, totalBytesRead = 0, buffStart = start;

        while (true)
        {
            try
            {
                if (!sock.isConnected())
                {
                    throw new ExtSocketException("socket closed");
                }

               bytesRead =  sock.getInputStream().read(target, start, count);
                

                if (bytesRead == 0)
                {
                    throw new ExtSocketException("socket closed");
                }

                totalBytesRead += bytesRead;
                if (bytesRead == count)
                {
                    break;
                }
                else
                {
                    count = count - bytesRead;
                }

                start = start + bytesRead;
                if (totalBytesRead > min && sock.getInputStream().available() <= 0)
                {
                    break;
                }
            }
            catch (IOException e)
            {
 
                    throw e;
 
            }

        }

       
        if (totalBytesRead == 0)
        {
            return -1;
        }

 

        return totalBytesRead;
    }

    /**
     * Compares the two IP Addresses. Returns 0 if both are equal, 1 if ip1 is greateer than ip2 otherwise -1.
     *
     * @param ip1
     * @param ip1
     * @return
     */
    public static int CompareIP(InetAddress ip1, InetAddress ip2)
    {
 
        int ipval1, ipval2;
        ipval1 = IPAddressToLong(ip1);
        ipval2 = IPAddressToLong(ip2);

        if (ipval1 == ipval2)
        {
            return 0;
        }
        if (ipval1 > ipval2)
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }

    private static int IPAddressToLong(InetAddress IPAddr)
    {
 
        byte[] byteIP = IPAddr.getAddress();

 
        int ip = (int) byteIP[0] << 24;
 
        ip += (int) byteIP[1] << 16;
 
        ip += (int) byteIP[2] << 8;
 
        ip += (int) byteIP[3];

        return ip;
    }
  

    /**
     * Reads a number of characters from the current source TextReader and writes the data to the target array at the specified index.
     *
     * @param sourceTextReader The source TextReader to read from
     * @param target Contains the array of characteres read from the source TextReader.
     * @param start The starting index of the target array.
     * @param count The maximum number of characters to read from the source TextReader.
     * @return The number of characters read. The number will be less than or equal to count depending on the data available in the source TextReader. Returns -1 if the end of the
     * stream is reached.
     */
     public static int ReadInput(BufferedReader sourceTextReader, byte[] target, int start, int count) throws IOException
    {
        // Returns 0 bytes if not enough space in target
        if (target.length == 0)
        {
            return 0;
        }

        char[] charArray = new char[target.length];
        int bytesRead = sourceTextReader.read(charArray, start, count);

        // Returns -1 if EOF
        if (bytesRead == 0)
        {
            return -1;
        }

        for (int index = start; index < start + bytesRead; index++)
        {
 
            target[index] = (byte) charArray[index];
        }

        return bytesRead;
    }

    /**
     * Creates a byte buffer representation of a <c>int32</c>
     *
     * @param value <c>int</c> to be converted
     * @return Byte Buffer representation of a <c>Int32</c>
     */
 
    public static byte[] WriteInt32(int value)
    {
 
        byte[] _byteBuffer = new byte[4];
 
        _byteBuffer[0] = (byte) (value >>> 0);
 
        _byteBuffer[1] = (byte) (value >>> 8);
 
        _byteBuffer[2] = (byte) (value >>> 16);
 
        _byteBuffer[3] = (byte) (value >>> 24);

        return _byteBuffer;
    }  

    /**
     * Creates a <c>Int32</c> from a byte buffer representation
     *
     * @param _byteBuffer Byte Buffer representation of a <c>Int32</c>
     * @return
     */
 
    public static int convertToInt32(byte[] _byteBuffer)
    {
 
        return ((_byteBuffer[0] & 0xFF) << 0) +
	       ((_byteBuffer[1] & 0xFF) << 8) +
	       ((_byteBuffer[2] & 0xFF) << 16) +
	       ((_byteBuffer[3]) << 24);
    }  

    /**
     * Creates a <c>Int32</c> from a byte buffer representation
     *
     * @param _byteBuffer Byte Buffer representation of a <c>Int32</c>
     * @return
     */
 
    public static int convertToInt32(byte[] _byteBuffer, int offset)
    {
 
        byte[] temp = new byte[4];
       System.arraycopy(_byteBuffer, offset, temp, 0, 4);
      
        return convertToInt32(temp);
    }  
 
}
