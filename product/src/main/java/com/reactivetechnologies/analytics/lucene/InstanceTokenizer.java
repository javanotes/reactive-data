/* ============================================================================
*
* FILE: LuceneTokenizerFilter.java
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
package com.reactivetechnologies.analytics.lucene;

import java.util.Iterator;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import weka.core.Instances;
import weka.core.Utils;
import weka.core.stemmers.NullStemmer;
import weka.core.tokenizers.WordTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

class InstanceTokenizer extends WordTokenizer {

  /**
   * Converts String attributes into a set of attributes representing word occurrence information from the text contained in the strings. 
   * The set of words (attributes) is determined by the first batch filtered (typically training data). Uses a Lucene analyzer to tokenize
   * the string. NOTE: The text string should either be the first or last attribute
   * @param dataRaw
   * @param opts
   * @param isLast - whether last attribute is the text to be filtered, else first
   * @return
   * @throws Exception
   * @see {@linkplain StringToWordVector}
   */
  public static Instances filter(Instances dataRaw, String opts, boolean isLast) throws Exception
  {
    StringToWordVector filter = new StringToWordVector();
    if(StringUtils.hasText(opts))
    {
      filter.setOptions(Utils.splitOptions(opts));
    }
    filter.setTokenizer(new InstanceTokenizer());
    filter.setUseStoplist(false);//ignore any other stop list
    filter.setStemmer(new NullStemmer());//ignore any other stemmer
    filter.setInputFormat(dataRaw);
    filter.setAttributeIndices(isLast ? "last" : "first");
    return Filter.useFilter(dataRaw, filter);
  }
  InstanceTokenizer()
  {
    
  }
  /**
   * 
   */
  private static final long serialVersionUID = 7965082235263621687L;
  
  /**
   * Returns a string describing the stemmer
   * 
   * @return    a description suitable for displaying in the 
   *      explorer/experimenter gui
   */
  public String globalInfo() {
    return 
        "Using Lucene analyzer to tokenize the strings.";
  }

  /**
   * Tests if this enumeration contains more elements.
   * 
   * @return    true if and only if this enumeration object contains 
   *      at least one more element to provide; false otherwise.
   */
  public boolean hasMoreElements() {
    Assert.notNull(tokenized);
    return tokenized.hasNext();
  }

  /**
   * Returns the next element of this enumeration if this enumeration object 
   * has at least one more element to provide.
   * 
   * @return    the next element of this enumeration.
   */
  public Object nextElement() {
    Assert.notNull(tokenized);
    return tokenized.next();
  }
  private Iterator<String> tokenized;
  /**
   * Sets the string to tokenize. Tokenization happens immediately.
   * 
   * @param s   the string to tokenize
   */
  public void tokenize(String s) {
    try {
      tokenized = EnglishTextAnalyzer.getTokens(s).iterator();
    } catch (Exception e) {
      throw new UnsupportedOperationException("Failed to tokenize stream using Lucene", e);
    }
    
  }

}
