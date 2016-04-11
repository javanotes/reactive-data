/* ============================================================================
*
* FILE: Configurator.java
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

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.util.StringUtils;

import com.reactivetechnologies.platform.datagrid.HazelcastKeyValueAdapterBean;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceFactoryBean;
import com.reactivetechnologies.platform.hzdfs.AsciiFileDistributor;
import com.reactivetechnologies.platform.rest.AsyncEventReceiverBean;
import com.reactivetechnologies.platform.rest.WebbitRestServerBean;
import com.reactivetechnologies.platform.rest.dll.JarFileSharingAgent;
import com.reactivetechnologies.platform.rest.dll.JarModuleLoader;
import com.reactivetechnologies.platform.rest.rt.Serveable;
import com.reactivetechnologies.platform.utils.ResourceLoaderHelper;
/**
 * Primary configuration class to autowire the core platform class instances and Hazelcast.
 */
@Configuration
public class Configurator {

  private static final Logger log = LoggerFactory.getLogger(Configurator.class);
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
  
  @Value("${restserver.jaxrs.extDir}")
  private String dllRoot;
  
  @Value("${restserver.maxConnection:10}")
  private int nThreads;
  @Value("${restserver.port:8991}")
  private int port;
  @Value("${restserver.enabled:true}")
  private boolean restEnabled;
  
  public boolean isRestEnabled() {
    return restEnabled;
  }
  public static final String PIPED_INSTREAM_FILE = "PIPED_INSTREAM_FILE";
  public static final String PIPED_OUTSTREAM_FILE = "PIPED_OUTSTREAM_FILE";
  public static final String PIPED_TOPIC_FILE = "PIPED_TOPIC_FILE";
  public static final int DEFAULT_CHUNK_SIZE_BYTES = 8192;
  public static final String NODE_INSTANCE_ID = "keyval.hazelcast.id";
      
  private static class RestEnabledCondition implements Condition
  {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      String enabled = context.getEnvironment().getProperty("restserver.enabled");
      boolean b = "true".equalsIgnoreCase(enabled);
      if(!b)
        log.warn("** REST not enabled **");
      return b;
      
    }
    
  }
  /**
   * REST server for listening to POST/GET requests
   * @return
   */
  @Bean
  @Conditional(RestEnabledCondition.class)
  public Serveable restServer()
  {
    return new WebbitRestServerBean(port, nThreads, basePkg);
    
  }
  /**
   * Asynchronous REST processor
   * @return
   */
  @Bean
  @Conditional(RestEnabledCondition.class)
  public AsyncEventReceiverBean asyncRestProcessor()
  {
    return new AsyncEventReceiverBean();
  }
  
  /**
   * Jar module loader.
   * @return
   */
  @Bean
  @Conditional(RestEnabledCondition.class)
  public JarModuleLoader dllLoader()
  {
    try 
    {
      if(StringUtils.isEmpty(dllRoot))
        throw new BeanCreationException("'restserver.jaxrs.extDir' path not found");
      File f = ResourceLoaderHelper.loadFromFileOrClassPath(dllRoot, false);
      return new JarModuleLoader(f);
    } catch (IOException e) {
      throw new BeanCreationException("'restserver.jaxrs.extDir' path not found", e);
    }
  }
  /**
   * Jar distribution agent.
   * @return
   */
  @Bean
  @Conditional(RestEnabledCondition.class)
  public JarFileSharingAgent jarSharingAgent()
  {
    return new JarFileSharingAgent();
  }
  
  
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
  public SpringContext contextAware()
  {
    return new SpringContext();
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
   * Spring data key value operations over Hazelcast. This is the instance to use Hazelcast as a generic key value store using Spring data.
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
   * Spring data key value operations over Hazelcast. This is the instance to use Hazelcast as a generic key value store using Spring data.
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
  @Bean
  public AsciiFileDistributor hdfs() throws Exception
  {
    return new AsciiFileDistributor(hzServiceFactoryBean().getObject());
  }
  
  
}
