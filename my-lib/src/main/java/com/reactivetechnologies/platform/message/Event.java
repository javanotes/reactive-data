/* ============================================================================
*
* FILE: Event.java
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
package com.reactivetechnologies.platform.message;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
/**
 * The data structure for submitting generic messages to the Hazelcast cluster
 *
 * @param <T>
 */
public class Event<T> implements DataSerializable, Serializable{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private long genTimestamp = -1;
  private long correlationId = 0;
  private String header = "";
  
  private byte[] bytes;
  private static KryoPool pool;
  static
  {
    pool = new KryoPool.Builder(new KryoFactory() {
      
      @Override
      public Kryo create() {
        Kryo kryo = new Kryo();
        return kryo;
      }
    }).softReferences().build();
  }
  
  /**
   * Gets the payload after deserializing
   * @return
   */
  @SuppressWarnings("unchecked")
  public T getPayload() {
    Kryo k = pool.borrow();
    try {
      Input in = new Input(bytes);
      return (T) k.readClassAndObject(in);
    } finally {
      pool.release(k);
    }
  }
  /**
   * Timestamp and corrId will be set in this case
   * @param item
   */
  public Event(T item)
  {
    setPayload(item);
    setGenTimestamp(System.currentTimeMillis());
    setCorrelationId(UUID.randomUUID().getMostSignificantBits());
    
  }
  /**
   * Timestamp and corrId needs to be set explicitly
   */
  public Event()
  {
    
  }
  /**
   * Sets the payload in serialized form
   * @param item
   */
  public void setPayload(T item) {
    Kryo k = pool.borrow();
    try {
      Output out = new Output(1024, -1);
      k.writeClassAndObject(out, item);
      bytes = out.getBuffer();
    } finally {
      pool.release(k);
    }
    
  }

  @Override
  public final void writeData(ObjectDataOutput out) throws IOException {
    out.writeLong(getCorrelationId());
    out.writeLong(getGenTimestamp());
    out.writeUTF(getHeader());
    out.writeByteArray(bytes);
  }

  @Override
  public final void readData(ObjectDataInput in) throws IOException {
    setCorrelationId(in.readLong());
    setGenTimestamp(in.readLong());
    setHeader(in.readUTF());
    bytes = in.readByteArray();
    
  }
  public long getGenTimestamp() {
    return genTimestamp;
  }
  public void setGenTimestamp(long genTimestamp) {
    this.genTimestamp = genTimestamp;
  }
  public long getCorrelationId() {
    return correlationId;
  }
  public void setCorrelationId(long correlationId) {
    this.correlationId = correlationId;
  }
  public String getHeader() {
    return header;
  }
  public void setHeader(String header) {
    this.header = header;
  }

  

}
