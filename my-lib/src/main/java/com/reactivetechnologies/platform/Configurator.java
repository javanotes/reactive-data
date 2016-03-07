/* ============================================================================
*
* FILE: HazelcastConfiguration.java
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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.keyvalue.core.KeyValueTemplate;

import com.reactivetechnologies.platform.datagrid.HazelcastKeyValueAdapterBean;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceFactoryBean;
import com.reactivetechnologies.platform.rest.netty.AsyncEventReceiver;

@Configuration
public class Configurator {

  @Value("${keyval.hazelcast.cfg: }")
  private String configXml;
  @Value("${keyval.entity.base:com.uthtechnologies.springdata.hz}")
  private String entityPkg;
  @Value("${keyval.hazelcast.id:node-1}")
  private String instanceId;
  @Value("${keyval.hazelcast.group: }")
  private String groupId;
  
  @Value("${restserver.jaxrs.basePkg: }")
  private String basePkg;
  
  @Bean
  @ConfigurationProperties(prefix = "keyval")
  public HazelcastProperties hzProps()
  {
    return new HazelcastProperties();
  }
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigIn() 
  {
    return new PropertySourcesPlaceholderConfigurer();
  }
  /**
   * For static access to Spring context
   * @return
   */
  @Bean
  public ContextAwareComponent contextAware()
  {
    return new ContextAwareComponent();
  }
  /**
   * Factory bean for obtaining singleton instance of Hazelcast instance wrapper.
   * @return
   */
  @Bean
  public HazelcastClusterServiceFactoryBean hzServiceFactoryBean()
  {
    HazelcastClusterServiceFactoryBean hazelcastFactory = new HazelcastClusterServiceFactoryBean();
    hazelcastFactory.setConfigXml(configXml);
    hazelcastFactory.setEntityBasePkg(entityPkg);
    hazelcastFactory.setInstanceId(instanceId);
    hazelcastFactory.setGroup(groupId);
    return hazelcastFactory;
    
  }
  
  /*
   * This is for starting NanoHTTPD server
   * @deprecated
  @Bean
  public RequestDispatcherFactoryBean restletFactory()
  {
    return new RequestDispatcherFactoryBean(basePkg); 
  }*/
  
  /**
   * Asynchronous REST processor
   * @return
   */
  @Bean
  public AsyncEventReceiver asyncRestProcessor()
  {
    return new AsyncEventReceiver();
  }
  
  /**
   * Singleton instance of spring data key value adaptor over Hazelcast.
   * Does not start Hazelcast instance listeners (lifecycle / partition migration).
   * @return
   * @throws Exception
   */
  @Bean
  @Primary
  public HazelcastKeyValueAdapterBean hzKeyValueAdaptor() throws Exception
  {
    HazelcastKeyValueAdapterBean bean = new HazelcastKeyValueAdapterBean(hzServiceFactoryBean().getObject());
    return bean;
    
  }
  /**
   * Singleton instance of spring data key value adaptor over Hazelcast.
   * Force start Hazelcast instance listeners (lifecycle / partition migration).
   * @return
   * @throws Exception
   */
  @Bean
  @Qualifier("hzKeyValueAdaptorJoinImmediate")
  public HazelcastKeyValueAdapterBean hzKeyValueAdaptorJoinImmediate() throws Exception
  {
    HazelcastKeyValueAdapterBean bean = new HazelcastKeyValueAdapterBean(true, hzServiceFactoryBean().getObject());
    return bean;
    
  }
  
  /**
   * Spring data key value operations over Hazelcast. This is the instance to use Hazelcast as a generic key value store.
   * Does not start Hazelcast instance listeners (lifecycle / partition migration)
   * @return
   * @throws Exception
   */
  @Bean
  @Primary
  public KeyValueTemplate kvtemplate() throws Exception
  {
    KeyValueTemplate kv = new KeyValueTemplate(hzKeyValueAdaptor());
    return kv;
    
  }
  /**
   * Spring data key value operations over Hazelcast. This is the instance to use Hazelcast as a generic key value store.
   * Force start Hazelcast instance listeners (lifecycle / partition migration)
   * @return
   * @throws Exception
   */
  @Bean
  @Qualifier("kvTemplateJoinImmediate")
  public KeyValueTemplate kvtemplateAndJoin() throws Exception
  {
    KeyValueTemplate kv = new KeyValueTemplate(hzKeyValueAdaptorJoinImmediate());
    return kv;
    
  }
  
}
