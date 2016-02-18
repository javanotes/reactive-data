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

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reactivetechnologies.analytics.EngineException;
import com.reactivetechnologies.analytics.RegressionModelEngine;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.meta.Stacking;
import weka.classifiers.meta.Vote;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;

public class IncrementalClassifierBean extends Classifier implements UpdateableClassifier, RegressionModelEngine {

  private static final Logger log = LoggerFactory.getLogger(IncrementalClassifierBean.class);
  
  protected Classifier clazzifier;
  private ExecutorService thread;
  @PostConstruct
  void init()
  {
    log.info("** Weka Classifier loaded ["+clazzifier+"] **");
    initialized = loadAndInitializeModel();
    //TODO: initialize evaluation data fetch
  }
  
  private volatile boolean initialized;
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
    if(!(c instanceof UpdateableClassifier))
      throw new IllegalArgumentException("Not an instance of UpdateableClassifier");
    clazzifier = c;
    queue = new ArrayBlockingQueue<>(size);
    thread = Executors.newSingleThreadExecutor(new ThreadFactory() {
      
      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "RegressionBean.Worker.Thread");
        return t;
      }
    });
    thread.submit(new Runnable() {
      
      @SuppressWarnings("unchecked")
      @Override
      public void run() {
        while(true)
        {
          try 
          {
            Dataset i = queue.take();
            
            if(i.isStop())
              break;
           
            if(!initialized)
            {
              buildClassifier(i.getAsInstance());
              initialized = true;
            }
            else
            {
              Enumeration<Instance> e_ins = i.getAsInstance().enumerateInstances();
              while(e_ins.hasMoreElements())
              {
                updateClassifier(e_ins.nextElement());
              }
            }
            
            
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("", e);
          } catch (Exception e) {
            log.error("Unable to update classifier", e);
          }
        }
        log.debug("Stopped Weka submitter");
      }
    });
  }
  @PreDestroy
  void stopWorker()
  {
    try {
      incrementModel(new Dataset(true));
    } catch (Exception e) {
      // ignored
    }
  }
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private final ArrayBlockingQueue<Dataset> queue;
  
  @Override
  public void updateClassifier(Instance instance) throws Exception {
    ((UpdateableClassifier) clazzifier).updateClassifier(instance);
    
  }

  @Override
  public void buildClassifier(Instances data) throws Exception {
    clazzifier.buildClassifier(data);
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
  public RegressionModel combineModels(List<RegressionModel> models) throws EngineException {
    
    Classifier[] classifiers = new Classifier[models.size()];
    int i=0;
    for(RegressionModel model : models)
    {
      classifiers[i++] = model.getTrainedClassifier();
    }
    
    Classifier bestFit = getBestFitByEvaluation(classifiers);
    RegressionModel m = new RegressionModel();
    m.setTrainedClassifier(bestFit);
    return m;
  }
  
  private Classifier getBestFitByVoting(Classifier[] classifiers)
  {
    Vote v = new Vote();
    v.setClassifiers(classifiers);
    v.setCombinationRule(new SelectedTag(Vote.AVERAGE_RULE, Vote.TAGS_RULES));
    return v;
  }
  
  private Classifier getBestFitByStacking(Classifier[] classifiers)
  {
    Stacking s = new Stacking();
    s.setClassifiers(classifiers);
    return s;
  }
  
  /**
   * 
   * @param classifiers
   * @return
   * @throws EngineException
   */
  private Classifier getBestFitByEvaluation(Classifier[] classifiers) throws EngineException
  {
    Classifier bestFit = null;
    double rms = Double.MAX_VALUE;
    for(Classifier cl : classifiers)
    {
      try 
      {
        Evaluation eval = evaluateClassifier(cl);
        double d = eval.rootMeanSquaredError();
        if(d < rms)
        {
          rms = d;
          bestFit = cl;
        }
      } catch (Exception e) {
        throw new EngineException("Exception while evaluating model", e);
      }
    }
    return bestFit;
  }
  /**
   * 
   * @param cl
   * @return
   * @throws IOException
   * @throws Exception
   */
  private Evaluation evaluateClassifier(Classifier cl) throws IOException, Exception {
    Instances ins = fetchModelEvaluationData().getAsInstance();
    Evaluation eval = new Evaluation(ins);
    eval.useNoPriors();
    eval.evaluateModel(cl, ins);
    return eval;
  }
  
  
  @Override
  public ClassifiedModel classify(Dataset unclassified) {
    // TODO: classify(Dataset unclassified)
    return null;
  }
  
  protected Dataset fetchModelEvaluationData() throws IOException {
    // TODO fetchModelEvaluationData
    return null;
  }


}
