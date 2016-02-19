/* ============================================================================
*
* FILE: WekaMessageChannel.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hazelcast.core.Message;
import com.reactivetechnologies.analytics.EngineException;
import com.reactivetechnologies.analytics.EvaluationDatasetGenerator;
import com.reactivetechnologies.analytics.RegressionModelEngine;
import com.reactivetechnologies.analytics.core.CombinerType;
import com.reactivetechnologies.analytics.core.RegressionModel;
import com.reactivetechnologies.analytics.utils.ConfigUtil;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.MessagingChannel;
/**
 * A component class that performs the scheduled task of stacking classifiers,
 * as well as serve as a cluster communication channel
 */
@Component
@ConfigurationProperties
public class WekaMessagingChannel implements MessagingChannel<Byte> {

  private static final Logger log = LoggerFactory.getLogger(WekaMessagingChannel.class);
  static final byte DUMP_MODEL_REQ = 0b00000001;
  static final byte DUMP_MODEL_RES = 0b00000011;
  @Value("${weka.scheduler.combiner}")
  private String combiner;
  @Autowired
  private RegressionModelEngine classifierBean;
  
  /**
   * Task for scheduling ensemble dumps.
   */
  @Scheduled(fixedDelay = 5000, initialDelay = 5000)
  public void ensembleModelTask()
  {
    log.info("[ensembleModelTask] task starting..");
    try {
      boolean done = tryMemberSnapshot(10, TimeUnit.MINUTES);
      if(done)
      {
        ensembleModels();
      }
      else
      {
        log.info("[ensembleModelTask] task ignored.. ");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.debug("", e);
    } catch (TimeoutException e) {
      log.warn("[ensembleModelTask] task failed. Generated model may be inconsistent", e);
    }
  }
  @Autowired
  private EvaluationDatasetGenerator dataGen;
  private void ensembleModels() {
    List<RegressionModel> models = new ArrayList<>();
    for(Iterator<RegressionModel> iterModel = hzService.getSetIterator(ConfigUtil.WEKA_MODEL_SNAPSHOT_SET); iterModel.hasNext();)
    {
      RegressionModel model = iterModel.next();
      models.add(model);
    }
    if(!models.isEmpty())
    {
      log.info("[ensembleModelTask] Saving model generated.. Combiner- "+combiner);
      RegressionModel ensemble;
      try 
      {
        ensemble = classifierBean.findBestFitModel(models, CombinerType.valueOf(combiner), dataGen.generate());
        log.debug(ensemble.getTrainedClassifier()+"");
        ensemble.generateId();
        hzService.persistItem(ConfigUtil.WEKA_MODEL_PERSIST_MAP, ensemble, ensemble.getLongId());
      } catch (EngineException e) {
        log.warn("[ensembleModelTask] task failed", e);
      }
      
      
    }
    
    
  }

  @Autowired
  private HazelcastClusterServiceBean hzService;
  
  @PostConstruct
  void init()
  {
    hzService.addMessageChannel(this);
    log.debug("Message channel created");
  }
  @Override
  public void onMessage(Message<Byte> message) {
    log.debug("Message received from Member:: ["+message.getPublishingMember()+"] "+message.getMessageObject());
    switch (message.getMessageObject()) 
    {
      case DUMP_MODEL_REQ:
        dumpClassifierSnapshot();
        break;
      case DUMP_MODEL_RES:
        notifyIfProcessing();
        break;  
      default:
        break;
    }
  }

  private void notifyIfProcessing() {
    if(processing && mCount.decrementAndGet() == 0)
    {
      //this instance is aggregating
      synchronized (this) {
        notify();
      }
    }
    
  }

  private void dumpClassifierSnapshot() 
  {
    RegressionModel model = classifierBean.generateModelSnapshot();
    log.debug("Dumping model:: "+model.getTrainedClassifier());
    //TODO: model.serializeClassifierAsJson();
    hzService.addToSet(ConfigUtil.WEKA_MODEL_SNAPSHOT_SET, model);
    sendMessage(DUMP_MODEL_RES);
  }
  @Override
  public String topic() {
    return ConfigUtil.WEKA_COMMUNICATION_TOPIC;
  }

  @Override
  public void sendMessage(Byte message) {
    hzService.publish(message, topic());
    log.debug("Message published:: ["+message+"] ");

  }
  /**
   * Request a dump of classifier models from all cluster members
   * @param duration
   * @param unit
   * @return
   * @throws InterruptedException
   * @throws TimeoutException 
   */
  public boolean tryMemberSnapshot(long duration, TimeUnit unit) throws InterruptedException, TimeoutException
  {
    boolean snapshotDone = false;
    boolean locked = hzService.acquireLock(TimeUnit.SECONDS, 10);
    try
    {
      if (locked) {
        snapshotDone = signalAndAwait(duration, unit);
        if(!snapshotDone)
          throw new TimeoutException("Operation timed out in ["+duration+" "+unit+"] before getting response from all members");
      }
    }
    finally
    {
      hzService.releaseLock(true);
    }
    
    return snapshotDone;
  }

  private final AtomicInteger mCount = new AtomicInteger(0);
  private volatile boolean processing;
  /**
   * 
   * @param duration
   * @param unit
   * @return
   * @throws InterruptedException
   */
  private boolean signalAndAwait(long duration, TimeUnit unit) throws InterruptedException{
    synchronized (this) {
      mCount.set(hzService.size());
      processing = true;
      try 
      {
        sendMessage(DUMP_MODEL_REQ);
        log.debug("Waiting for dump response..");
        wait(unit.toMillis(duration));
        
      } 
      finally {
        processing = false;
      }
    }
    return mCount.get() == 0;
    
  }

  /**
   * Request a dump of classifier models from all cluster members
   * @return
   * @throws InterruptedException
   * @throws TimeoutException 
   */
  public boolean tryMemberSnapshot() throws InterruptedException, TimeoutException {
    return tryMemberSnapshot(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    
  }

}
