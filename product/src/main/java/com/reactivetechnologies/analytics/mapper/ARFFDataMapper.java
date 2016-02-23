/* ============================================================================
*
* FILE: ARFFDataMapper.java
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
package com.reactivetechnologies.analytics.mapper;

import java.io.StringReader;
import java.text.ParseException;

import com.reactivetechnologies.analytics.core.Dataset;
import com.reactivetechnologies.analytics.dto.ArffJsonRequest;
import com.reactivetechnologies.analytics.dto.JsonRequest;

import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

public class ARFFDataMapper implements DataMapper {

  @Override
  public String type() {
    return "ARFF";
  }

  @Override
  public Dataset mapStringToModel(JsonRequest request) throws ParseException {
    if(!(request instanceof ArffJsonRequest))
    {
      throw new ParseException("Not an instance of "+ArffJsonRequest.class, -1);
    }
    try 
    {
      ArffJsonRequest arff = (ArffJsonRequest) request;
      ArffReader ar = new ArffReader(new StringReader(request.toString()));
      Instances ins = ar.getData();
      ins.setClassIndex(arff.getClassIndex() >= 0 ? arff.getClassIndex() : ins.numAttributes()-1);
      return new Dataset(ins);
    } catch (Exception e) {
      ParseException pe = new ParseException("Cannot convert JSON stream to ARFF", -1);
      pe.initCause(e);
      throw pe;
    }
  }

}
