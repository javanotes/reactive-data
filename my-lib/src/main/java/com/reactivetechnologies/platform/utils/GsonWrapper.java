/* ============================================================================
*
* FILE: GsonWrapper.java
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
package com.reactivetechnologies.platform.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component
public class GsonWrapper {

  private Gson gsonInstance = null;

  /**
   * 
   */
  public GsonWrapper() {
    super();
        
  }
  private List<AbstractJsonSerializer<?>> typeAdapters = new ArrayList<>();
  /**
   * With simple adaptors
   * @param typeAdapters
   */
  public GsonWrapper(List<AbstractJsonSerializer<?>> typeAdapters) {
    this();
    this.typeAdapters.addAll(typeAdapters);    
  }
  
  private <T> void build()
  {
    GsonBuilder builder = new GsonBuilder()
        .setPrettyPrinting().disableHtmlEscaping();
        
    for(AbstractJsonSerializer<?> adaptor: typeAdapters)
    {
      builder.registerTypeAdapter(adaptor.getClassType(), adaptor);
    }
    gsonInstance = builder.create();
    alreadyBuilt = true;
  }
  /**
   * Register another type adaptor for Json serialization
   * @param adaptor
   * @throws IllegalAccessException if already initialized
   */
  public void registerTypeAdapter(AbstractJsonSerializer<?> adaptor) throws IllegalAccessException
  {
    if(alreadyBuilt)
      throw new IllegalAccessException("Gson already initialized");
    typeAdapters.add(adaptor);
  }
  private volatile boolean alreadyBuilt;
  /**
   * 
   * @return
   */
  public Gson get()
  {
    if(!alreadyBuilt)
    {
      synchronized (this) {
        if(!alreadyBuilt)
          build();
      }
    }
      
    return gsonInstance;
  }
  
}
