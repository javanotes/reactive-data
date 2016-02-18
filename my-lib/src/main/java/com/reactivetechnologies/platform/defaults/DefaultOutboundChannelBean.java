/* ============================================================================
*
* FILE: DefaultOutboundChannel.java
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
package com.reactivetechnologies.platform.defaults;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reactivetechnologies.platform.interceptor.AbstractOutboundChannel;
import com.reactivetechnologies.platform.interceptor.OutboundInterceptor;
/**
 * No operation implementation
 */
public class DefaultOutboundChannelBean extends AbstractOutboundChannel {

  
  private static final Logger log = LoggerFactory.getLogger(DefaultOutboundChannelBean.class);
  
  @Override
  public void onFeedException(Serializable item, OutboundInterceptor<Serializable> feeder,
      Throwable exception) {
    
    log.error("Item ["+item+"] had exception on feeding channel ["+feeder+"]. Error:: "+exception.getMessage());
    log.debug("", exception);
  }

  @Override
  public void onFeedTimeout(Serializable item, OutboundInterceptor<Serializable> feeder) {
    
    log.warn("Item ["+item+"] faced timeout on feeding channel ["+feeder+"]. ");
  }
  @Override
  public String name() {
    return "default";
  }
  @Override
  public Class<Serializable> type() {
    return Serializable.class;
  }

}
