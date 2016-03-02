/* ============================================================================
*
* FILE: SimpleJsonTypeAdapter.java
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
package com.reactivetechnologies.platform.rest.json;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public abstract class SimpleTypeAdapter<T> implements JsonSerializer<T>
{
  private final Class<T> classType;
  
  /**
   * 
   * @param classType
   */
  public SimpleTypeAdapter(Class<T> classType) {
    super();
    this.classType = classType;
    
  }
  /**
   * Use the protected field {@link #json} to convert
   * @param object
   * @param json 
   */
  protected abstract void serializeToJson(T object, JsonObject json);
  protected void addAsBoolean(String key, Boolean value, JsonObject json)
  {
    json.add(key, new JsonPrimitive(value));
  }
  protected void addAsString(String key, String value, JsonObject json)
  {
    json.add(key, new JsonPrimitive(value));
  }
  protected void addAsNumber(String key, Number value, JsonObject json)
  {
    json.add(key, new JsonPrimitive(value));
  }
  public Class<T> getClassType() {
    return classType;
  }
  @Override
  public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
    final JsonObject json = new JsonObject();
    serializeToJson(src, json);
    return json;
  }
  
  
}