/* ============================================================================
*
* FILE: Server.java
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

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.jar.JarOutputStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server implements Closeable{

  private final int port;
  public int getPort() {
    return port;
  }

  public Server(int port) {
    this.port = port;
  }

  private static final Logger log = LoggerFactory.getLogger(Server.class);
  private ServerBootstrap bootstrap;
  private Channel channel;
  
  @PostConstruct
  private void init()
  {
    bootstrap = new ServerBootstrap();
    bootstrap.setFactory(new NioServerSocketChannelFactory(Executors.newSingleThreadExecutor(new ThreadFactory() {
      
      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "Stream.Server.Boss");
        t.setDaemon(true);
        return t;
      }
    }), Executors.newSingleThreadExecutor(new ThreadFactory() {
      
      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "Stream.Server.Worker");
        t.setDaemon(true);
        return t;
      }
    }), 1));
    
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      @Override
      public ChannelPipeline getPipeline() throws Exception {
          
          ChannelPipeline pipeline = pipeline();
          pipeline.addLast("handler", new ChunkedWriteHandler());
          pipeline.addLast("writer", new SimpleChannelUpstreamHandler(){
            @Override
            public void messageReceived(final ChannelHandlerContext ctx, MessageEvent messageEvent) throws Exception {
                if (messageEvent.getMessage() instanceof ChunkedFile) {
                    try {
                      handleChunkedFile(ctx, (ChunkedFile) messageEvent.getMessage());
                    } catch (Exception e) {
                      log.error("Unable to handle jar stream download", e);
                    }
                } else {
                    super.messageReceived(ctx, messageEvent);
                }
            }

            private void handleChunkedFile(ChannelHandlerContext ctx, ChunkedFile message) throws Exception {
              JarOutputStream jar = new JarOutputStream(new FileOutputStream("some.jar"));
              while(message.hasNextChunk())
              {
                ChannelBuffer buff = (ChannelBuffer) message.nextChunk();
                byte[] bytes = new byte[buff.readableBytes()];
                buff.readBytes(bytes);
                jar.write(bytes);
              }
              jar.flush();
              jar.close();
            }
          });
          return pipeline;
      }
    });
    
    channel = bootstrap.bind(new InetSocketAddress(port));
    log.info("[Stream Server] Listening on port- "+port);
  }

  @Override@PreDestroy
  public void close() throws IOException {
    if (channel != null) {
      channel.close();
    }
    if (bootstrap != null) {
      bootstrap.releaseExternalResources();
    }
  }
}
