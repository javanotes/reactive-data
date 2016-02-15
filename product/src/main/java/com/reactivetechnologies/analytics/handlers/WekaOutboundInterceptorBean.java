/* ============================================================================
*
* FILE: WekaOutboundInterceptorBean.java
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
package com.reactivetechnologies.analytics.handlers;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.reactivetechnologies.analytics.core.IncrementalClassifierBean;
import com.reactivetechnologies.analytics.core.TrainModel;
import com.reactivetechnologies.platform.interceptor.ChannelException;
import com.reactivetechnologies.platform.interceptor.OutboundInterceptor;

public class WekaOutboundInterceptorBean
    implements OutboundInterceptor<Serializable> {

  private static final Logger log = LoggerFactory.getLogger(WekaOutboundInterceptorBean.class);
  @Autowired
  private IncrementalClassifierBean classifierBean;
  @PostConstruct
  void init()
  {
    log.debug("Weka outbound interceptor created with wrapper ["+classifierBean+"]. ");
    
  }
  
  @Override
  public void feed(Serializable item) throws ChannelException {
    try 
    {
      log.debug("Feeding training model:: "+item);
      classifierBean.incrementModel((TrainModel) item);
    } catch (Exception e) {
      throw new ChannelException(e);
    }
    
  }
  @Override
  public String name() {
    return "Weka-Submitter";
  }
  @Override
  public Class<?> type() {
    return TrainModel.class;
  }

}
