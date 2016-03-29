/* ============================================================================
*
* FILE: URIDetail.java
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
package com.reactivetechnologies.platform.rest.rt;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.util.Assert;
import org.webbitserver.rest.deps.org.weborganic.furi.URIPattern;
import org.webbitserver.rest.deps.org.weborganic.furi.URIResolveResult;
import org.webbitserver.rest.deps.org.weborganic.furi.URIResolver;

public final class URIDetail {

  private final Map<String, Boolean> templates = new LinkedHashMap<>();
  private List<String> tokens;
  public List<String> getTemplateTokens()
  {
    if(tokens == null)
    {
      tokens = new ArrayList<>();
      for(Entry<String, Boolean> entry : templates.entrySet())
      {
        if(entry.getValue())
          tokens.add(entry.getKey());
      }
    }
    
    return new ArrayList<>(tokens);
    
  }
  public URIDetail() {
    this("");
  }
  private final String rawUri;
  private final URIPattern pattern;
  public URIDetail(String uri) {
    
    Assert.notNull(uri);
    rawUri = uri;
    pattern = new URIPattern(rawUri);
    String[] splits = uri.split("/");
    if(splits.length == 0)
      throw new IllegalArgumentException("Invalid URI: "+uri);
    
    for(String split : splits)
    {
      if(split.startsWith("{") && split.endsWith("}"))
      {
        templates.put(split.substring(1, split.length()-1), true);
      }
      else if(split.startsWith("{") || split.endsWith("}"))
        throw new IllegalArgumentException("Invalid URI: "+uri);
      else
        templates.put(split, false);
    }
    
  }
  /**
   * @deprecated Use {@link #matchesTemplate(String)} instead
   * @param uri
   * @return
   */
  boolean matches(String uri)
  {
    String[] part = uri.split("/");
    int i =0;
    for(Entry<String, Boolean> entry : templates.entrySet())
    {
      if(i < part.length)
      {
        if(!entry.getValue())
        {
          //not a template
          if(!part[i].equals(entry.getKey()))
          {
            return false;
          }
        }
        i++;
      }
      else
      {
        return false;
      }
    }
    return true;
            
  }
  /**
   * Using template resolver
   * @param uri
   * @return
   */
  public boolean matchesTemplate(String uri)
  {
    String path = URI.create(uri).getPath();
    URIResolver uriResolver = new URIResolver(path);
    URIResolveResult resolveResult = uriResolver.resolve(pattern);
    return resolveResult.getStatus() == URIResolveResult.Status.RESOLVED;
  }
  /*public static void main(String[] args) {
    // /users/{username}/{userid}
    URIDetail u = new URIDetail("/users/{username}/{userid}/set/{id}");
    u.setBase("users");
    u.templates.add("username");
    u.templates.add("userid");
    
    System.out.println(u.matches("/users/sutanu/30")+" "+ u.matchesTemplate("/users/sutanu/30"));
    System.out.println(u.matches("/users/sutanu/30/set/4")+" "+ u.matchesTemplate("/users/sutanu/30/set/4"));
    System.out.println(u.matches("/users/sutanu/30/set/4/")+" "+ u.matchesTemplate("/users/sutanu/30/set/4/"));
    System.out.println(u.matches("/users/sutanu/")+" "+ u.matchesTemplate("/users/sutanu/"));
    System.out.println(u.matches("/users/sutanu")+" "+ u.matchesTemplate("/users/sutanu"));
    System.out.println(u.matches("/users/")+" "+ u.matchesTemplate("/users/"));
    System.out.println(u.matches("/users")+" "+ u.matchesTemplate("/users"));
    System.out.println(u.matches("/sutanu")+" "+ u.matchesTemplate("/sutanu"));
  }*/

  public String getRawUri() {
    return rawUri;
  }

}
