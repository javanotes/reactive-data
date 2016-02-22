/* ============================================================================
*
* FILE: StackingWithBuiltClassifiers.java
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

import java.util.Random;

import weka.classifiers.meta.Stacking;
import weka.core.Instances;

class StackingWithBuiltClassifiers extends Stacking {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   * Buildclassifier selects a classifier from the set of classifiers
   * by minimising error on the training data.
   *
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

    if (m_MetaClassifier == null) {
      throw new IllegalArgumentException("No meta classifier has been set");
    }

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    Instances newData = new Instances(data);
    m_BaseFormat = new Instances(data, 0);
    newData.deleteWithMissingClass();

    Random random = new Random(m_Seed);
    newData.randomize(random);
    if (newData.classAttribute().isNominal()) {
      newData.stratify(m_NumFolds);
    }

    // Create meta level
    generateMetaLevel(newData, random);

    /** Changed here */
    // DO NOT Rebuilt all the base classifiers on the full training data
    /*for (int i = 0; i < m_Classifiers.length; i++) {
      getClassifier(i).buildClassifier(newData);
    }*/
    /** End change */
  }

  /**
   * Generates the meta data
   *
   * @param newData the data to work on
   * @param random the random number generator to use for cross-validation
   * @throws Exception if generation fails
   */
  @Override
  protected void generateMetaLevel(Instances newData, Random random)
    throws Exception {

    Instances metaData = metaFormat(newData);
    m_MetaFormat = new Instances(metaData, 0);
    for (int j = 0; j < m_NumFolds; j++) {
      

      /** Changed here */
      //Instances train = newData.trainCV(m_NumFolds, j, random);
      // DO NOT Build base classifiers
      /*for (int i = 0; i < m_Classifiers.length; i++) {
          getClassifier(i).buildClassifier(train);
      }*/
      /** End change */

      // Classify test instances and add to meta data
      Instances test = newData.testCV(m_NumFolds, j);
      for (int i = 0; i < test.numInstances(); i++) {
  metaData.add(metaInstance(test.instance(i)));
      }
    }

    m_MetaClassifier.buildClassifier(metaData);
  }

}
