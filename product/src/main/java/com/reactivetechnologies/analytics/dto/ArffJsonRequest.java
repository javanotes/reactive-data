/* ============================================================================
*
* FILE: ArffJsonRequest.java
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
package com.reactivetechnologies.analytics.dto;

import org.springframework.util.StringUtils;

public class ArffJsonRequest extends JsonRequest {

  public String getType() {
    return "ARFF";
  }
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("@RELATION ");
    s.append(StringUtils.hasText(getRelation()) ? getRelation() : "stream").append("\n\n");
    
    for(Attribute a : getAttributes())
    {
      s.append("@ATTRIBUTE ").append(a.getName()).append(" ");
      if(StringUtils.hasText(a.getType()))
      {
        if(a.getType().equalsIgnoreCase("string") || a.getType().equalsIgnoreCase("numeric"))
        {
          s.append(a.getType()).append("\n");
        }
        else if(a.getType().equalsIgnoreCase("date"))
        {
          s.append(a.getType()).append("\"").append(a.getSelector()[0]).append("\"").append("\n");
        }
      }
      else
      {
        s.append("{").append(StringUtils.arrayToCommaDelimitedString(a.getSelector())).append("}").append("\n");
      }
    }
    
    s.append("\n@DATA\n");
    for(Text d : getData())
    {
      s.append(d.getText()).append("\n");
    }
    
    return s.toString();
  }
  public int getClassIndex() {
    return classIndex;
  }
  public void setClassIndex(int classIndex) {
    this.classIndex = classIndex;
  }
  public String getRelation() {
    return relation;
  }
  public void setRelation(String relation) {
    this.relation = relation;
  }
  
  public Attribute[] getAttributes() {
    return attributes;
  }
  public void setAttributes(Attribute[] attributes) {
    this.attributes = attributes;
  }
  int classIndex = -1;
  String relation;
  Attribute[] attributes;
    
}
