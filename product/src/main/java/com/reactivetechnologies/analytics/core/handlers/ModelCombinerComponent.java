/* ============================================================================
*
* FILE: ModelCombinerComponent.java
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
package com.reactivetechnologies.analytics.core.handlers;

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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import com.hazelcast.core.Message;
import com.reactivetechnologies.analytics.EngineException;
import com.reactivetechnologies.analytics.RegressionModelEngine;
import com.reactivetechnologies.analytics.core.Dataset;
import com.reactivetechnologies.analytics.core.dto.CombinerResult;
import com.reactivetechnologies.analytics.core.dto.RegressionModel;
import com.reactivetechnologies.analytics.core.eval.CombinerDatasetGenerator;
import com.reactivetechnologies.analytics.core.eval.CombinerType;
import com.reactivetechnologies.analytics.utils.ConfigUtil;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.MessageChannel;
/**
 * A component class that performs the scheduled task of stacking classifiers,
 * as well as serve as a cluster communication channel
 */
@Component
@ConfigurationProperties
public class ModelCombinerComponent implements MessageChannel<Byte> {

  private static final Logger log = LoggerFactory.getLogger(ModelCombinerComponent.class);
  static final byte DUMP_MODEL_REQ = 0b00000001;
  static final byte DUMP_MODEL_RES = 0b00000011;
  @Value("${weka.scheduler.combiner}")
  private String combiner;
  @Value("${weka.scheduler.combiner.options}")
  private String combinerOpts;
  @Autowired
  private RegressionModelEngine classifierBean;
  
  /**
   * Runs a cluster wide model collection, and generates a combined (ensembled/voted/evaluated) classifier model.
   * The generated model is persisted in database, only if it is different than the ones already present.
   * @return Persisted model Id, or "" if not persisted in this run
   * @throws EngineException
   */
  public CombinerResult runTask() throws EngineException
  {
    log.info("[ensembleModelTask] task starting..");
    String modelId = "";
    CombinerResult result = CombinerResult.IGNORED;
    try 
    {
      boolean done = tryMemberSnapshot(10, TimeUnit.MINUTES);
      if(done)
      {
        modelId = ensembleModels();
        if(modelId != null)
        {
          result = CombinerResult.MODEL_CREATED;
          result.setModelId(modelId);
        }
         
      }
      else
      {
        log.info("[ensembleModelTask] task ignored.. ");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.debug("", e);
    } catch (TimeoutException e) {
      log.warn("[ensembleModelTask] task timed out. Generated model may be inconsistent", e);
      result = CombinerResult.MODEL_CREATED;
    } catch (EngineException e) {
      if(e.getCause() instanceof DuplicateKeyException)
      {
        log.warn(e.getMessage());
        //log.debug(e.getMessage(), e.getCause());
        result = CombinerResult.MODEL_EXISTS;
        result.setModelId(e.getCause().getMessage());
      }
      else
        throw e;
    }
    return result;
  }
  @Autowired
  private CombinerDatasetGenerator dataGen;
  
  private String ensembleModels() throws EngineException 
  {
    List<RegressionModel> models = new ArrayList<>();
    for(Iterator<RegressionModel> iterModel = hzService.getSetIterator(ConfigUtil.WEKA_MODEL_SNAPSHOT_SET); iterModel.hasNext();)
    {
      RegressionModel model = iterModel.next();
      models.add(model);
    }
    if(!models.isEmpty())
    {
      
      RegressionModel ensemble;
      try 
      {
        Dataset dset = dataGen.generate();
        dset.setOptions(combinerOpts);
        ensemble = classifierBean.findBestFitModel(models, CombinerType.valueOf(combiner), dset);
        log.debug(ensemble.getTrainedClassifier()+"");
        ensemble.generateId();
        boolean saved = hzService.persistItem(ConfigUtil.WEKA_MODEL_PERSIST_MAP, ensemble, ensemble.getStringId());
        if(!saved)
          throw new DuplicateKeyException(ensemble.getStringId());
        
        log.info("[ensembleModelTask] Saved model generated.. Combiner used- "+combiner);
        return ensemble.getStringId();
      } catch (EngineException e) {
        throw e;
      }
      catch (DuplicateKeyException e) {
        throw new EngineException("Ignoring model already present in database", e);
      }      
    }
    return null;
    
    
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
  boolean tryMemberSnapshot(long duration, TimeUnit unit) throws InterruptedException, TimeoutException
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
  boolean tryMemberSnapshot() throws InterruptedException, TimeoutException {
    return tryMemberSnapshot(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    
  }
  
}
