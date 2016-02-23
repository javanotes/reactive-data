/* ============================================================================
*
* FILE: TEXTDataMapper.java
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

import java.text.ParseException;

import com.reactivetechnologies.analytics.core.Dataset;
import com.reactivetechnologies.analytics.dto.JsonRequest;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class TEXTDataMapper implements DataMapper {

  @Override
  public String type() {
    return "TEXT";
  }

  @Override
  public Dataset mapStringToModel(JsonRequest request) throws ParseException {
    if(request != null && request.getData() != null && request.getData().length > 0)
    {
      FastVector fvWekaAttributes = new FastVector(2);
      FastVector nil = null;
      Attribute attr0 = new Attribute("text",nil, 0);
      FastVector fv = new FastVector();
      for(String nominal : request.getClassVars())
      {
        fv.addElement(nominal);
      }
      Attribute attr1 = new Attribute("class", fv,1);
      
      fvWekaAttributes.addElement(attr0);
      fvWekaAttributes.addElement(attr1); 
      
      Instances ins = new Instances("attr-reln", fvWekaAttributes, request.getData().length);
      ins.setClassIndex(1);
      for(String s : request.getData())
      {
        String [] ss = s.split(",");
        if(ss.length >= 2)
        {
          Instance i = new Instance(2);
          i.setValue(attr0, ss[0]);
          i.setValue(attr1, ss[1]);
          ins.add(i);
        }
        
      }
      
      return new Dataset(ins);
    }
    return null;
  }

}
