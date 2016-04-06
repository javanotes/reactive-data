/* ============================================================================
*
* FILE: AbstractMessageChannel.java
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
package com.reactivetechnologies.platform.datagrid.handlers;

import java.io.Closeable;

import org.springframework.util.Assert;

import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
/**
 * A base class with utility.
 *
 * @param <E>
 */
public abstract class AbstractMessageChannel<E> implements MessageChannel<E>, Closeable {

  /**
   * Get the Hazelcast service bean
   * @return
   */
  protected HazelcastClusterServiceBean hazelcastService;
  protected String registrationId;
  public String getRegistrationId() {
    return registrationId;
  }

  /**
   * 
   * @param hazelcastService
   */
  public AbstractMessageChannel(HazelcastClusterServiceBean hazelcastService, boolean orderedSubcribe) {
    Assert.notNull(hazelcastService);
    this.hazelcastService = hazelcastService;
    this.hazelcastService.addMessageChannel(this, orderedSubcribe);
  }
    
  @Override
  public void sendMessage(E message) {
    hazelcastService.publish(message, topic());

  }
  
  public void setRegistrationId(String regID) {
    registrationId = regID;
    
  }

  @Override
  public void close() {
    hazelcastService.removeMessageChannel(this);
  }

}
