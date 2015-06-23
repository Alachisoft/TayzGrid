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

import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SelectableChannel;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.util.Iterator;

public class SelectSockets {
    public static int PORT_NUMBER = 1234;


    /**
     *
     * @param argv
     * @throws java.lang.Exception
     */
    public void go(String [] argv)
    throws Exception {
        int port = PORT_NUMBER;

        if (argv.length > 0) {	
            port = Integer.parseInt(argv [0]);
        }


        // allocate an unbound server socket channel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        // Get the associated ServerSocket to bind it with
        ServerSocket serverSocket = serverChannel.socket();
        // create a new Selector for use below
        Selector selector = Selector.open();

        // set the port the server channel will listen to
        serverSocket.bind(new InetSocketAddress(port));

        // set non-blocking mode for the listening socket
        serverChannel.configureBlocking(false);

        // register the ServerSocketChannel with the Selector
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // this may block for a long time, upon return the
            // selected set contains keys of the ready channels
            int n = selector.select();

            if (n == 0) {
                continue;	// nothing to do
            }

            // get an iterator over the set of selected keys
            Iterator it = selector.selectedKeys().iterator();

            // look at each key in the selected set
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();

                // Is a new connection coming in?
                if (key.isAcceptable()) {
                    ServerSocketChannel server =
                            (ServerSocketChannel) key.channel();
                    SocketChannel channel = server.accept();

                    registerChannel(selector, channel,
                            SelectionKey.OP_READ);

                    sayHello(channel);
                }

                // is there data to read on this channel?
                if (key.isReadable()) {
                    readDataFromSocket(key);
                }

                // remove key from selected set, it's been handled
                it.remove();
            }
        }
    }

    // ----------------------------------------------------------

    /**
     * Register the given channel with the given selector for
     * the given operations of interest
     * @param selector
     * @param channel
     * @param ops
     * @throws java.lang.Exception
     */
    protected void registerChannel(Selector selector,
            SelectableChannel channel, int ops)
            throws Exception {
        if (channel == null) {
            return;		
        }

        // set the new channel non-blocking
        channel.configureBlocking(false);

        // register it with the selector
        channel.register(selector, ops);
    }

    // ----------------------------------------------------------

    // Use the same byte buffer for all channels.  A single thread is
    // servicing all the channels, so no danger of concurrent acccess.
    private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

    /**
     * Sample data handler method for a channel with data ready to read.
     *
     * @param key A SelectionKey object associated with a channel
     *  determined by the selector to be ready for reading.  If the
     *  channel returns an EOF condition, it is closed here, which
     *  automatically invalidates the associated key.  The selector
     *  will then de-register the channel on the next select call.
     * @throws java.lang.Exception
     */
    protected void readDataFromSocket(SelectionKey key)
    throws Exception {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        int count;

        buffer.clear();			// make buffer empty

        // loop while data available, channel is non-blocking
        while ((count = socketChannel.read(buffer)) > 0) {
            buffer.flip();		// make buffer readable

            // send the data, don't assume it goes all at once
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
           

            buffer.clear();	
        }

        if (count < 0) {
            // close channel on EOF, invalidates the key
            socketChannel.close();
        }
    }

    // ----------------------------------------------------------

    /**
     * Spew a greeting to the incoming client connection.
     * @param channel The newly connected SocketChannel to say hello to.
     * @throws java.lang.Exception
     */
    private void sayHello(SocketChannel channel)
    throws Exception {
        buffer.clear();
        buffer.put("Hi there!\r\n".getBytes());
        buffer.flip();

        channel.write(buffer);
    }

}


