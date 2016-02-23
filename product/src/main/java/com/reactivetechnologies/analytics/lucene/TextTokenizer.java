/* ============================================================================
*
* FILE: TextTokenizer.java
*
* MODULE DESCRIPTION:
* See class description
*
* Copyright (C) 2015 by
* 
*
* The program may be used and/or copied only with the written
* permission from  or in accordance with
* the terms and conditions stipulated in the agreement/contract
* under which the program has been supplied.
*
* All rights reserved
*
* ============================================================================
*/
package com.reactivetechnologies.analytics.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;


public class TextTokenizer {

  /**
   * 
   * @param analyzer
   * @param string
   * @return
   * @throws IOException
   */
  private static Set<String> getTokens(Analyzer analyzer, String string) throws IOException {
    Set<String> result = new HashSet<String>();
    TokenStream stream = null;
    try 
    {
      stream  = analyzer.tokenStream(null, new StringReader(string));
      stream.reset();
      while (stream.incrementToken()) {
        result.add(stream.getAttribute(CharTermAttribute.class).toString());
      }
      
    } catch (IOException e) {
      throw e;
    }
    finally
    {
      if (stream != null) {
        stream.end();
        stream.close();
      }
    }
    return result;
  }
  
  /**
   * Tokenize a text to create a reverse index lookup for searching.
   * @param text
   * @return
   * @throws IOException 
   */
  public static Set<String> tokenize(String text) throws IOException
  {
    Set<String> tokens = new HashSet<>();
    TextAnalyzer az = new TextAnalyzer();
    tokens.addAll(getTokens(az, text));
    
    return tokens;
  }
   
}
