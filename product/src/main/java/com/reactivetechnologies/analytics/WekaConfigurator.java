/* ============================================================================
*
* FILE: ClassifierConfigurator.java
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
package com.reactivetechnologies.analytics;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.reactivetechnologies.analytics.core.CachedIncrementalClassifierBean;
import com.reactivetechnologies.analytics.core.IncrementalClassifierBean;
import com.reactivetechnologies.analytics.core.RegressionModel;
import com.reactivetechnologies.analytics.handlers.WekaInboundInterceptorBean;
import com.reactivetechnologies.analytics.handlers.WekaOutboundInterceptorBean;
import com.reactivetechnologies.analytics.mapper.DataMapperFactoryBean;
import com.reactivetechnologies.analytics.mapper.DataMappers;
import com.reactivetechnologies.analytics.store.WekaModelJdbcRepository;
import com.reactivetechnologies.analytics.store.WekaModelPersistenceStore;
import com.reactivetechnologies.analytics.utils.ConfigUtil;
import com.reactivetechnologies.platform.ChannelMultiplexerBean;
import com.reactivetechnologies.platform.ChannelMultiplexerFactoryBean;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.defaults.DefaultOutboundChannelBean;
import com.reactivetechnologies.platform.interceptor.AbstractInboundInterceptor;
import com.reactivetechnologies.platform.message.Event;

import weka.classifiers.Classifier;
import weka.core.Utils;

@Configuration
public class WekaConfigurator {

  @Value("${weka.classifier}")
  private String wekaClassifier;
  
  @Value("${weka.classifier.options}")
  private String options;
  /**
   * The core class that implements Weka functions.
   * @return
   * @throws Exception
   */
  @Bean
  public IncrementalClassifierBean regressionBean() throws Exception
  {
    Classifier c = Classifier.forName(wekaClassifier, null);
    c.setOptions(Utils.splitOptions(options));
    //return new IncrementalClassifierBean(c, 1000);
    return new CachedIncrementalClassifierBean(c, 1000);
  }
  
  /**
   * Factory bean to create instances of message flow, via a {@linkplain ChannelMultiplexerBean}.
   * @return
   */
  @Bean
  public ChannelMultiplexerFactoryBean channelMultiplexerFactoryBean()
  {
    ChannelMultiplexerFactoryBean bean = new ChannelMultiplexerFactoryBean();
    return bean;
  }
  
  /**
   * This is the main class that creates a "flow" of an inbound channel to feed through an outbound channel.
   * Here we create a flow for Weka messages, by setting the channel to a {@linkplain WekaInboundInterceptorBean}.
   * @return
   * @throws Exception
   */
  @Bean
  public ChannelMultiplexerBean wekaChannelMultiplexerBean() throws Exception
  {
    ChannelMultiplexerBean bean = channelMultiplexerFactoryBean().getObject();
    bean.setChannel(inboundWeka());
    return bean;
  }
  
  @Autowired
  HazelcastClusterServiceBean hzService;
  
  @PostConstruct
  void onLoad()
  {
    hzService.setMapStoreImplementation(ConfigUtil.WEKA_MODEL_PERSIST_MAP, mapStore());
    log.debug("Map store impl set.. ");
  }
  
  private static final Logger log = LoggerFactory.getLogger(WekaConfigurator.class);
  
    
  /**
   * Factory bean for a composite of data mappers {@linkplain DataMappers}. This composite
   * class is basically a factory for creating the mappers.
   * @return
   */
  @Bean
  public DataMapperFactoryBean mapperFactoryBean()
  {
    return new DataMapperFactoryBean();
  }
  
  /**
   * Weka incoming channel. Clients should submit via an {@linkplain Event} wrapping the payload JSON
   * 
   */
  @Bean
  public AbstractInboundInterceptor<?, ? extends Serializable> inboundWeka()
  {
    WekaInboundInterceptorBean in = new WekaInboundInterceptorBean();
    //link the outbound channel
    in.setOutChannel(outboundWeka());
    return in;
  }
  /**
   * Weka outgoing channel.
   * @return
   */
  @Bean
  public DefaultOutboundChannelBean outboundWeka()
  {
    DefaultOutboundChannelBean out = new DefaultOutboundChannelBean();
    out.addFeeder(outboundWekaInterceptor());
    return out;
  }
  /**
   * Weka outgoing channel feeder. Should invoke Weka functions
   * @return
   */
  @Bean
  public WekaOutboundInterceptorBean outboundWekaInterceptor()
  {
    return new WekaOutboundInterceptorBean();
  }
  
//JDBC configurations
  @ConfigurationProperties(prefix = "spring.datasource")
  @Bean
  public DataSource unpooled()
  {
    return new DriverManagerDataSource();
  }
  @Bean
  public JdbcTemplate jdbcTemplateUnpooled()
  {
    JdbcTemplate tmpl = new JdbcTemplate(unpooled());
    return tmpl;
  }
  
  /**
   * The {@linkplain JdbcTemplate} is injected into this repository.
   * Extension point by implementing a CRUD repository
   * @return
   */
  @Bean
  public CrudRepository<RegressionModel, Long> repository()
  {
    return new WekaModelJdbcRepository();
  }
  
  /**
   * The backing map store implementation. The {@linkplain CrudRepository} will be injected into this
   * @return
   */
  @Bean
  @Primary
  public WekaModelPersistenceStore mapStore()
  {
    return new WekaModelPersistenceStore();
  }
}
