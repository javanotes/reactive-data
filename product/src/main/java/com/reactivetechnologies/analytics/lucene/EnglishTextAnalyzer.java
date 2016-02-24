/* ============================================================================
*
* FILE: LAnalyzer.java
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
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.tartarus.snowball.ext.EnglishStemmer;

class EnglishTextAnalyzer extends Analyzer {
  
  private final static Set<String> characters = new HashSet<>();
  static{
    for(char i=32; i<=126; i++)
    {
      characters.add(Character.toString(i));
    }
  }
  private EnglishTextAnalyzer(){}
  /**
   * 
   * @param analyzer
   * @param string
   * @return
   * @throws IOException
   */
  static Set<String> getTokens(String string) throws IOException {
    Set<String> result = new HashSet<String>();
    TokenStream stream = null;
    Analyzer analyzer = new EnglishTextAnalyzer();
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
      analyzer.close();
    }
    return result;
  }
    
  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    Tokenizer source = new ClassicTokenizer();
    CharArraySet stopWords = CharArraySet.copy(characters);
    stopWords.addAll(StopAnalyzer.ENGLISH_STOP_WORDS_SET);
    
    TokenFilter filter = new ClassicFilter(source);
    filter = new LowerCaseFilter(filter);
    filter = new SnowballFilter(filter, new EnglishStemmer());
    filter = new ShingleFilter(filter, 2, 2);
    filter = new StopFilter(filter, stopWords);
    
     
    return new TokenStreamComponents(source, filter);
   
  }

}
