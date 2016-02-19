/* ============================================================================
*
* FILE: BaggingWithBuiltClassifiers.java
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

import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.meta.Bagging;
import weka.core.Instances;
import weka.core.Utils;

class BaggingWithBuiltClassifiers extends Bagging {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  public BaggingWithBuiltClassifiers(Classifier[] classifiers) {
    m_Classifiers = classifiers;
  }
  public BaggingWithBuiltClassifiers(Classifier classifier) throws Exception {
    m_Classifier = classifier;
    makeCopies();
  }
  void makeCopies() throws Exception
  {
    m_Classifiers = Classifier.makeCopies(m_Classifier, getNumIterations());
  }
  @Override
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    /** Changed here: Use supplied classifier */
    //super.buildClassifier(data);
    /** End change */

    if (m_CalcOutOfBag && (m_BagSizePercent != 100)) {
      throw new IllegalArgumentException("Bag size needs to be 100% if "
          + "out-of-bag error is to be calculated!");
    }

    int bagSize = (int) (data.numInstances() * (m_BagSizePercent / 100.0));
    Random random = new Random(m_Seed);

    boolean[][] inBag = null;
    if (m_CalcOutOfBag)
      inBag = new boolean[m_Classifiers.length][];

    for (int j = 0; j < m_Classifiers.length; j++) {
      Instances bagData = null;

      // create the in-bag dataset
      if (m_CalcOutOfBag) {
        inBag[j] = new boolean[data.numInstances()];
        bagData = data.resampleWithWeights(random, inBag[j]);
      } else {
        bagData = data.resampleWithWeights(random);
        if (bagSize < data.numInstances()) {
          bagData.randomize(random);
          Instances newBagData = new Instances(bagData, 0, bagSize);
          bagData = newBagData;
        }
      }

      /** Changed here: Use supplied classifier */
      /*if (m_Classifier instanceof Randomizable) {
        ((Randomizable) m_Classifiers[j]).setSeed(random.nextInt());
      }

      // build the classifier
      m_Classifiers[j].buildClassifier(bagData);*/
      /** End change */
    }

    // calc OOB error?
    if (getCalcOutOfBag()) {
      double outOfBagCount = 0.0;
      double errorSum = 0.0;
      boolean numeric = data.classAttribute().isNumeric();

      for (int i = 0; i < data.numInstances(); i++) {
        double vote;
        double[] votes;
        if (numeric)
          votes = new double[1];
        else
          votes = new double[data.numClasses()];

        // determine predictions for instance
        int voteCount = 0;
        for (int j = 0; j < m_Classifiers.length; j++) {
          if (inBag[j][i])
            continue;

          voteCount++;
          // double pred = m_Classifiers[j].classifyInstance(data.instance(i));
          if (numeric) {
            // votes[0] += pred;
            votes[0] += m_Classifiers[j].classifyInstance(data.instance(i));
          } else {
            // votes[(int) pred]++;
            double[] newProbs = m_Classifiers[j].distributionForInstance(data
                .instance(i));
            // average the probability estimates
            for (int k = 0; k < newProbs.length; k++) {
              votes[k] += newProbs[k];
            }
          }
        }

        // "vote"
        if (numeric) {
          vote = votes[0];
          if (voteCount > 0) {
            vote /= voteCount; // average
          }
        } else {
          if (Utils.eq(Utils.sum(votes), 0)) {
          } else {
            Utils.normalize(votes);
          }
          vote = Utils.maxIndex(votes); // predicted class
        }

        // error for instance
        outOfBagCount += data.instance(i).weight();
        if (numeric) {
          errorSum += StrictMath.abs(vote - data.instance(i).classValue())
              * data.instance(i).weight();
        } else {
          if (vote != data.instance(i).classValue())
            errorSum += data.instance(i).weight();
        }
      }

      m_OutOfBagError = errorSum / outOfBagCount;
    } else {
      m_OutOfBagError = 0;
    }
  }

}
