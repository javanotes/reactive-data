/* ============================================================================
*
* FILE: WekaInterceptorBean.java
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

import com.hazelcast.internal.ascii.rest.RestValue;
import com.hazelcast.util.StringUtil;
import com.reactivetechnologies.analytics.core.TrainModel;
import com.reactivetechnologies.analytics.dto.ArffJsonRequest;
import com.reactivetechnologies.analytics.dto.Attribute;
import com.reactivetechnologies.analytics.mapper.DataMapper;
import com.reactivetechnologies.analytics.utils.ConfigUtil;
import com.reactivetechnologies.analytics.utils.GsonWrapper;
import com.reactivetechnologies.platform.interceptor.AbstractInboundInterceptor;
import com.reactivetechnologies.platform.interceptor.ChannelException;

public class WekaInboundInterceptorBean extends AbstractInboundInterceptor<RestValue, TrainModel> {

  private static final Logger log = LoggerFactory.getLogger(WekaInboundInterceptorBean.class);
  
  @PostConstruct
  void created() throws Exception
  {
    if (log.isDebugEnabled()) {
      log.debug(
          "Ready to intercept for WEKA. Listening on IMAP::" + keyspace());
      log.debug("Downstream connector [" + getOutChannel() + "] ");
      
      ArffJsonRequest event = new ArffJsonRequest();
      event.setRelation("schema");
      event.setType("JSON");
      event.setAttributes(new Attribute[]{
          new Attribute("sepallength", "numeric", null), 
          new Attribute("sepalwidth", "numeric", null),
          new Attribute("petallength", "numeric", null),
          new Attribute("petalwidth", "numeric", null),
          new Attribute("class", null, new String[]{"Iris-setosa", "Iris-versicolor", "Iris-virginica"})
          });
      
      event.setData(new String[]{
          "5.1,3.5,1.4,0.2,Iris-setosa",
          "4.9,3.0,1.4,0.2,Iris-setosa",
          "7.0,3.2,4.7,1.4,Iris-versicolor",
          "5.8,2.8,5.1,2.4,Iris-virginica"});
      
      log.debug("[" + name() + "] consuming Hazelcast event of type:\n"
          + GsonWrapper.get().toJson(event));
      log.debug("Corresponding ARFF generated:: \n"+event.toString());
    }
    
  }
  
  @Override
  public String keyspace() {
    return ConfigUtil.WEKA_IN_MAP;
  }

  
  @Override
  public String name() {
    return ConfigUtil.WEKA_IN_BEAN_NAME;
  }
 
  @Override
  public Class<TrainModel> outType() {
    return TrainModel.class;
  }
   
  @Autowired
  private DataMapper mapper;
  
  @Override
  public TrainModel intercept(Serializable key, RestValue _new,
      RestValue _old) throws ChannelException {
    if (log.isDebugEnabled()) {
      log.debug("-- Begin message --");
      log.debug(StringUtil.bytesToString(_new.getValue()));
      log.debug("-- End message --");
    }
    try {
      ArffJsonRequest jr = GsonWrapper.get().fromJson(StringUtil.bytesToString(_new.getValue()), ArffJsonRequest.class);
      return mapper.mapStringToModel(jr);
    } catch (Exception e) {
      throw new ChannelException("Inbound interceptor ["+name()+"] ", e);
    }
  }
  @Override
  public Class<RestValue> inType() {
    return RestValue.class;
  }

  

  

  
}
