/* ============================================================================
*
* FILE: ClientFactoryBean.java
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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.springframework.beans.factory.FactoryBean;

public class ClientFactoryBean implements FactoryBean<Client>, IClient {

  public ClientFactoryBean() {
    
  }
  private final Map<String, Client> clientPool = new HashMap<>();
  @PostConstruct
  private void init()
  {
    bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
        Executors.newCachedThreadPool(),
        Executors.newCachedThreadPool()));
    
    bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
      public ChannelPipeline getPipeline() throws Exception {
          ChannelPipeline pipeline = pipeline();
          pipeline.addLast("encoder", new ChunkedWriteHandler());
          return pipeline;
      }
    });
  }
  @Override
  public Client getObject() throws Exception {
    return new Client(bootstrap);
  }
  @PreDestroy
  private void destroy()
  {
    for(Client c : clientPool.values())
    {
      c.close();
    }
    bootstrap.releaseExternalResources();
  }
  private ClientBootstrap bootstrap;
  
  @Override
  public Class<?> getObjectType() {
    return Client.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  private static String key(int port, String hostName)
  {
    return hostName + ":" + port;
  }
  
  @Override
  public void connectAndSend(int port, String hostName, File file) throws Exception {
    String key = key(port, hostName);
    if(!clientPool.containsKey(key))
    {
      synchronized (this) {
        if(!clientPool.containsKey(key))
        {
          clientPool.put(key, getObject());
        }
      }
    }
    
    clientPool.get(key).connectAndSend(port, hostName, file);
  }

}
