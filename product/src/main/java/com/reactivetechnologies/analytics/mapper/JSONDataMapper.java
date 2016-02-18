/* ============================================================================
*
* FILE: JSONDataMapper.java
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

import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

public class JSONDataMapper implements DataMapper {

  public JSONDataMapper() {
  }

  @Override
  public String type() {
    return "JSON";
  }

  @Override
  public Dataset mapStringToModel(ArffJsonRequest request)
      throws ParseException {
    try {
      ArffReader ar = new ArffReader(new StringReader(request.toString()));
      Instances ins = ar.getData();
      ins.setClassIndex(request.getClassIndex() >= 0 ? request.getClassIndex() : ins.numAttributes()-1);
      return new Dataset(ins);
    } catch (Exception e) {
      ParseException pe = new ParseException("Cannot convert JSON stream to ARFF", -1);
      pe.initCause(e);
      throw pe;
    }
  }

}
