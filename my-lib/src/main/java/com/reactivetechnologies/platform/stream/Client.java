/* ============================================================================
*
* FILE: Client.java
*
The MIT License (MIT)

Copyright (c) 2016 Sutanu Dalui

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*
* ============================================================================
*/
package com.reactivetechnologies.platform.stream;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.stream.ChunkedFile;

public class Client implements Closeable, IClient{
 
  private final ClientBootstrap bootstrap;
  private volatile  boolean connected;
  private Channel channel;


  /**
   * Initialize the socket details
   * 
   */
  Client(ClientBootstrap bootstrap) {
      this.bootstrap = bootstrap;
      connected = false;
  }

  
  private void connect(int port, String hostName) throws Exception {
    ChannelFuture futureChannel = bootstrap.connect(new InetSocketAddress(hostName, port));
    channel = futureChannel.awaitUninterruptibly().getChannel();
    
  }

  public boolean isConnected() {
      return connected;
  }
  @Override
  public void close() {
    channel.close();
      
  }
  
  private void send(File file) throws IOException {
    channel.write(new ChunkedFile(file)).awaitUninterruptibly();
  }

  @Override
  public void connectAndSend(int port, String hostName, File file)
      throws Exception {
    synchronized (this) {
      if (!connected)
        connect(port, hostName);
      send(file);
    }
    
  }

}
