/* ============================================================================
*
* FILE: LogTextAnalyzer.java
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

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.standard.ClassicTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.tartarus.snowball.ext.EnglishStemmer;

class TextAnalyzer extends Analyzer {
  
  private final static Set<String> characters = new HashSet<>();
  static{
    for(char i=32; i<=126; i++)
    {
      characters.add(Character.toString(i));
    }
  }
    
  @Override
  protected TokenStreamComponents createComponents(String fieldName) {
    final Tokenizer source = new ClassicTokenizer();
    CharArraySet stopWords = CharArraySet.copy(characters);
    stopWords.addAll(StopAnalyzer.ENGLISH_STOP_WORDS_SET);
    
    TokenFilter filter = new ClassicFilter(source);
    filter = new LowerCaseFilter(filter);
    filter = new StopFilter(filter, stopWords);
    filter = new SnowballFilter(filter, new EnglishStemmer());
     
    return new TokenStreamComponents(source, filter);
   
  }

}
