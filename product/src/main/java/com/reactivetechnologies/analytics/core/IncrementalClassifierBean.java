/* ============================================================================
*
* FILE: IncrementalClassifier.java
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
package com.reactivetechnologies.analytics.core;

import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.reactivetechnologies.analytics.EngineException;
import com.reactivetechnologies.analytics.RegressionModelEngine;
import com.reactivetechnologies.analytics.core.dto.ClassifiedModel;
import com.reactivetechnologies.analytics.core.dto.RegressionModel;
import com.reactivetechnologies.analytics.core.eval.CombinerType;
import com.reactivetechnologies.analytics.lucene.TextInstanceFilter;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.core.Instance;
import weka.core.Instances;
/**
 * A proxy over a Weka classifier.
 */
public class IncrementalClassifierBean extends Classifier implements RegressionModelEngine {

  protected boolean isUpdateable()
  {
    return clazzifier != null && (clazzifier instanceof UpdateableClassifier);
  }
  @Autowired
  private HazelcastClusterServiceBean hzService;
  
  /**
   * Builds an intermediary classifier based on training data available
   * @throws Exception
   */
  private synchronized void updateClassifier() throws Exception
  {
    Set<Entry<Integer, Dataset>> entries = hzService.instanceEntrySet();
    if (!entries.isEmpty()) {
      Instances data = null;
      for (Entry<Integer, Dataset> entry : entries) {
        data = new Instances(getAsInstances(entry.getValue()));

      }
      buildClassifier(data);
      hzService.clearInstanceMap();
      
      log.info("[updateClassifier] Incremental classifier build complete");
    }
    instanceCount.compareAndSet(instanceBatchSize, 0);
  }
  /**
   * 
   */
  private class EventTimer implements Runnable {
    
    
    @Override
    public void run() {
      try 
      {
        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastBuildAt) >= maxIdle) {
          log.debug("[EventTimer] start run..");
          updateClassifier();
          log.debug("[EventTimer] end run..");
        }
                
      }  catch (Exception e) {
        log.error("[EventTimer] Unable to update classifier!", e);
      }
    }
  }
  
  /**
   * 
   */
  private class EventConsumer implements Runnable {
    
    
    @Override
    public void run() {
      while(true)
      {
        try 
        {
          Dataset i = queue.take();
          
          if(i.isStop())
            break;
         
          hzService.setInstanceValue(instanceCount.get(), i);
          if(instanceCount.incrementAndGet() == instanceBatchSize)
          {
            log.debug("[EventConsumer] start build..");
            updateClassifier();
          }
                  
          
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.debug("", e);
        } catch (Exception e) {
          log.error("[EventConsumer] Unable to update classifier!", e);
        }
      }
      log.debug("Stopped Weka submitter");
    }
  }

  private final AtomicInteger instanceCount = new AtomicInteger(0);
  private static final Logger log = LoggerFactory.getLogger(IncrementalClassifierBean.class);
  
  @Value("${weka.classifier.tokenize}")
  private boolean filterDataset;
  @Value("${weka.classifier.tokenize.options: }")
  private String filterOpts;
  @Value("${weka.classifier.build.batchSize:1000}")
  private int instanceBatchSize;
  @Value("${weka.classifier.build.intervalSecs:3600}")
  private long delay;
  @Value("${weka.classifier.build.maxIdleSecs:3600}")
  private long maxIdle;
  
  protected Classifier clazzifier;
  private ExecutorService worker, timer;
  protected volatile long lastBuildAt = 0;
  
  @PostConstruct
  void init()
  {
    loadAndInitializeModel();
    
    log.info( (isUpdateable() ? "UPDATEABLE ":"NON-UPDATEABLE ") + "** Weka Classifier loaded ["+clazzifier+"] **");
    if(log.isDebugEnabled())
    {
      log.debug("weka.classifier.tokenize? "+filterDataset);
      log.debug("weka.classifier.tokenize.options: "+filterOpts);
      log.debug("weka.classifier.build.batchSize: "+instanceBatchSize);
      log.debug("weka.classifier.build.intervalSecs: "+delay);
      log.debug("weka.classifier.build.maxIdleSecs: "+maxIdle);
    }
    worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
      
      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "RegressionBean.Worker.Thread");
        return t;
      }
    });
    worker.submit(new EventConsumer());
    
    timer = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      
      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "RegressionBean.Timer.Thread");
        t.setDaemon(true);
        return t;
      }
    });
    ((ScheduledExecutorService)timer).scheduleWithFixedDelay(new EventTimer(), delay, delay, TimeUnit.SECONDS);
  }
  @Override
  public boolean loadAndInitializeModel() {
    return false;
        
  }
  /**
   * 
   * @param c Base classifier
   * @param size size of blocking queue
   */
  public IncrementalClassifierBean(Classifier c, int size)
  {
    if(!(c instanceof Classifier))
      throw new IllegalArgumentException("Not an instance of Classifier");
    clazzifier = c;
    queue = new ArrayBlockingQueue<>(size);
    
  }
  @PreDestroy
  void stopWorker()
  {
    
    try {
      if(timer != null){
        timer.shutdown();
        try {
          timer.awaitTermination(delay, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          
        }
      }
      incrementModel(new Dataset(true));
      worker.shutdown();
    } catch (Exception e) {
      // ignored
    }
  }
  
  protected Instances getAsInstances(Dataset i) throws Exception {
    if (filterDataset) {
      return TextInstanceFilter.filter(i.getAsInstances(), filterOpts,
          false);
    }
    return i.getAsInstances();
  }
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private final ArrayBlockingQueue<Dataset> queue;
  
  @Override
  public void buildClassifier(Instances data) throws Exception {
    try 
    {
      if(isUpdateable())
      {
        UpdateableClassifier u = (UpdateableClassifier) clazzifier;
        for(@SuppressWarnings("unchecked")
        Enumeration<Instance> e = data.enumerateInstances(); e.hasMoreElements();)
        {
          u.updateClassifier(e.nextElement());
        }
      }
      else
        clazzifier.buildClassifier(data);
      
      lastBuildAt = System.currentTimeMillis();
    } finally {
      
    }
    
    
  }


  /**
   * Classifies the given test instance. The instance has to belong to a dataset
   * when it's being classified. Note that a classifier MUST implement either
   * this or distributionForInstance().
   * 
   * @param instance the instance to be classified
   * @return the predicted most likely class for the instance or
   *         Instance.missingValue() if no prediction is made
   * @exception Exception if an error occurred during the prediction
   */
  public double classifyInstance(Instance instance) throws Exception {

    return clazzifier.classifyInstance(instance);
  }

  /**
   * Predicts the class memberships for a given instance. If an instance is
   * unclassified, the returned array elements must be all zero. If the class is
   * numeric, the array must consist of only one element, which contains the
   * predicted value. Note that a classifier MUST implement either this or
   * classifyInstance().
   * 
   * @param instance the instance to be classified
   * @return an array containing the estimated membership probabilities of the
   *         test instance in each class or the numeric prediction
   * @exception Exception if distribution could not be computed successfully
   */
  public double[] distributionForInstance(Instance instance) throws Exception {

    return clazzifier.distributionForInstance(instance);
  }
  
  /**
   * Parses a given list of options. Valid options are:
   * <p>
   * 
   * -D <br>
   * If set, classifier is run in debug mode and may output additional info to
   * the console.
   * <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    clazzifier.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    return clazzifier.getOptions();
    
  }

  @Override
  public void incrementModel(Dataset nextInstance) throws Exception {
    boolean b = queue.offer(nextInstance, 5, TimeUnit.SECONDS);
    if(!b)
      throw new TimeoutException("Unable to offer model even after waiting for 5 secs");
  }

  
  @Override
  public RegressionModel generateModelSnapshot() {
    RegressionModel m = new RegressionModel();
    m.setTrainedClassifier(clazzifier);
    return m;
  }
      
  @Override
  public ClassifiedModel classify(Dataset unclassified) throws EngineException {
    ClassifiedModel model = new ClassifiedModel();
    try {
      model.setClassified(classifyInstance(unclassified.getAsInstance()));
    } catch (Exception e) {
      throw new EngineException(e);
    }
      
    return model;
  }
  
  @Override
  public RegressionModel findBestFitModel(List<RegressionModel> models, CombinerType combiner, Dataset evaluationSet) throws EngineException {
    Classifier[] classifiers = new Classifier[models.size()];
    int i=0;
    for(RegressionModel model : models)
    {
      classifiers[i++] = model.getTrainedClassifier();
    }
    
    Classifier bestFit = null;
    bestFit = combiner.getBestFitClassifier(classifiers, evaluationSet.getAsInstances(), evaluationSet.getOptions());
    log.info("Best fit model combination generated.. ");
    RegressionModel m = new RegressionModel();
    m.setTrainedClassifier(bestFit);
    return m;
  }


}
