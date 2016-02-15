/* ============================================================================
*
* FILE: ChannelMultiplexerBean.java
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
package com.reactivetechnologies.platform;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reactivetechnologies.platform.datagrid.HazelcastKeyValueAdapterBean;
import com.reactivetechnologies.platform.interceptor.AbstractInboundInterceptor;
/**
 * The main class to be used as a multiplexer for message channel creation. This creates a <i>flow</i> of message from incoming to outgoing (one-to-many). So there should
 * be 'n' number of instances to create 'n' number of flows.<p>
 * <b>Note:</b> Consequently this should ideally be a prototype class. Use the factory bean {@linkplain ChannelMultiplexerFactoryBean} to create instances
 * The Hazelcast singletons are the {@linkplain HazelcastKeyValueAdapterBean} and {@linkplain HazelcastClusterServiceBean}
 * @see WekaConfigurator
 */
public class ChannelMultiplexerBean {
   
  private final static Logger log = LoggerFactory.getLogger(ChannelMultiplexerBean.class);
  private final HazelcastKeyValueAdapterBean hzAdaptor;
  ChannelMultiplexerBean(HazelcastKeyValueAdapterBean hzAdaptor)
  {
    this.hzAdaptor = hzAdaptor;
  }
  
  private AbstractInboundInterceptor<?, ? extends Serializable> channel;
  public AbstractInboundInterceptor<?, ? extends Serializable> getChannel() {
    return channel;
  }
  public void setChannel(AbstractInboundInterceptor<?, ? extends Serializable> channel) {
    this.channel = channel;
  }
  /**
   * Creates a new channel with an inbound interceptor implementation.
   * @param channel
   */
  @PostConstruct
  public <V> void createChannel()
  {
    hzAdaptor.addLocalKeyspaceListener(channel);
    log.info("New channel flow created with inbound interceptor ["+getChannel().name()+"].");
  }
}
