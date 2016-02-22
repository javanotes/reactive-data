/* ============================================================================
*
* FILE: AdaBoostM1WithBuiltClassifiers.java
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
package com.reactivetechnologies.analytics.core.eval;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.AdaBoostM1;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

class AdaBoostM1WithBuiltClassifiers extends AdaBoostM1 {
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  AdaBoostM1WithBuiltClassifiers(Classifier[] classifiers) {
    this.m_Classifiers = classifiers;
  }

  @Override
  public void buildClassifier(Instances data) throws Exception 
  {
    /** Changed here: Using the provided classifiers */
    /** End */

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();
    
    // only class? -> build ZeroR model
    if (data.numAttributes() == 1) {
      System.err.println(
    "Cannot build model (only class attribute present in data!), "
    + "using ZeroR model instead!");
      m_ZeroR = new weka.classifiers.rules.ZeroR();
      m_ZeroR.buildClassifier(data);
      return;
    }
    else {
      m_ZeroR = null;
    }
    
    m_NumClasses = data.numClasses();
    if ((!m_UseResampling) && 
  (m_Classifier instanceof WeightedInstancesHandler)) {
      buildClassifierWithWeights(data);
    } else {
      buildClassifierUsingResampling(data);
    }
  }

  @Override
  protected void buildClassifierWithWeights(Instances data) 
      throws Exception {

      Instances training;
      double epsilon, reweight;
      Evaluation evaluation;
      int numInstances = data.numInstances();
      
      // Initialize data
      m_Betas = new double [m_Classifiers.length];
      m_NumIterationsPerformed = 0;

      // Create a copy of the data so that when the weights are diddled
      // with it doesn't mess up the weights for anyone else
      training = new Instances(data, 0, numInstances);
      
      // Do boostrap iterations
      for (m_NumIterationsPerformed = 0; m_NumIterationsPerformed < m_Classifiers.length; 
     m_NumIterationsPerformed++) {
        if (m_Debug) {
    System.err.println("Training classifier " + (m_NumIterationsPerformed + 1));
        }
        // Select instances to train the classifier on
        if (m_WeightThreshold < 100) {
    selectWeightQuantile(training, 
             (double)m_WeightThreshold / 100);
        } else {
    new Instances(training, 0, numInstances);
        }

        /** Changed here: DO NOT Build the classifier! */
        /*if (m_Classifiers[m_NumIterationsPerformed] instanceof Randomizable)
          ((Randomizable) m_Classifiers[m_NumIterationsPerformed]).setSeed(randomInstance.nextInt());
        
        m_Classifiers[m_NumIterationsPerformed].buildClassifier(trainData);*/
        /** End change */
        
        
        // Evaluate the classifier
        evaluation = new Evaluation(data);
        evaluation.evaluateModel(m_Classifiers[m_NumIterationsPerformed], training);
        epsilon = evaluation.errorRate();

        // Stop if error too small or error too big and ignore this model
        if (Utils.grOrEq(epsilon, 0.5) || Utils.eq(epsilon, 0)) {
    if (m_NumIterationsPerformed == 0) {
      m_NumIterationsPerformed = 1; // If we're the first we have to to use it
    }
    break;
        }
        // Determine the weight to assign to this model
        m_Betas[m_NumIterationsPerformed] = Math.log((1 - epsilon) / epsilon);
        reweight = (1 - epsilon) / epsilon;
        if (m_Debug) {
    System.err.println("\terror rate = " + epsilon
           +"  beta = " + m_Betas[m_NumIterationsPerformed]);
        }
   
        // Update instance weights
        setWeights(training, reweight);
      }
    }

  @Override
  protected void buildClassifierUsingResampling(Instances data) 
      throws Exception {

      Instances trainData, training;
      double epsilon, reweight, sumProbs;
      Evaluation evaluation;
      int numInstances = data.numInstances();
      int resamplingIterations = 0;

      // Initialize data
      m_Betas = new double [m_Classifiers.length];
      m_NumIterationsPerformed = 0;
      // Create a copy of the data so that when the weights are diddled
      // with it doesn't mess up the weights for anyone else
      training = new Instances(data, 0, numInstances);
      sumProbs = training.sumOfWeights();
      for (int i = 0; i < training.numInstances(); i++) {
        training.instance(i).setWeight(training.instance(i).
                weight() / sumProbs);
      }
      
      // Do boostrap iterations
      for (m_NumIterationsPerformed = 0; m_NumIterationsPerformed < m_Classifiers.length; 
     m_NumIterationsPerformed++) {
        if (m_Debug) {
    System.err.println("Training classifier " + (m_NumIterationsPerformed + 1));
        }

        // Select instances to train the classifier on
        if (m_WeightThreshold < 100) {
    trainData = selectWeightQuantile(training, 
             (double)m_WeightThreshold / 100);
        } else {
    trainData = new Instances(training);
        }
        
        // Resample
        resamplingIterations = 0;
        double[] weights = new double[trainData.numInstances()];
        for (int i = 0; i < weights.length; i++) {
    weights[i] = trainData.instance(i).weight();
        }
        do {
    

    /** Changed here: DO NOT build classifier*/
    // Build and evaluate classifier
    //m_Classifiers[m_NumIterationsPerformed].buildClassifier(sample);
    /** End change */
    
    
    evaluation = new Evaluation(data);
    evaluation.evaluateModel(m_Classifiers[m_NumIterationsPerformed], 
           training);
    epsilon = evaluation.errorRate();
    resamplingIterations++;
        } while (Utils.eq(epsilon, 0) && 
          (resamplingIterations < 10));
          
        // Stop if error too big or 0
        if (Utils.grOrEq(epsilon, 0.5) || Utils.eq(epsilon, 0)) {
    if (m_NumIterationsPerformed == 0) {
      m_NumIterationsPerformed = 1; // If we're the first we have to to use it
    }
    break;
        }
        
        // Determine the weight to assign to this model
        m_Betas[m_NumIterationsPerformed] = Math.log((1 - epsilon) / epsilon);
        reweight = (1 - epsilon) / epsilon;
        if (m_Debug) {
    System.err.println("\terror rate = " + epsilon
           +"  beta = " + m_Betas[m_NumIterationsPerformed]);
        }
   
        // Update instance weights
        setWeights(training, reweight);
      }
    }
}