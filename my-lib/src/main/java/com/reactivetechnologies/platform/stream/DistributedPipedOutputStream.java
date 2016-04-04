/* ============================================================================
*
* FILE: DistributedPipedOutputStream.java
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
package com.reactivetechnologies.platform.stream;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.hazelcast.core.Message;
import com.reactivetechnologies.platform.Configurator;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.MessageChannel;
/**
 * A single threaded piped output stream. Will be distributed in nature.
 * The 'connect' to the input stream is via a Hazelcast topic.
 * 
 */
public class DistributedPipedOutputStream extends OutputStream implements MessageChannel<byte[]>, Buffered {

  protected static final Logger log = LoggerFactory.getLogger(DistributedPipedOutputStream.class);
  
  
  private final HazelcastClusterServiceBean hzService;
  
  /**
   * 
   */
  public DistributedPipedOutputStream(HazelcastClusterServiceBean hzService) {
    this(Configurator.DEFAULT_CHUNK_SIZE_BYTES, hzService);
  }

  private byte[] circularBuffer;
  private int bufferSize = Configurator.DEFAULT_CHUNK_SIZE_BYTES;
  private int position = 0;
  private int flushCount = 0;
  private boolean connected;
 
  public long getPosition() {
      return position;
  }
  public long getBytesWritten() {
    return flushCount*bufferSize + position;
  }
    
  /**
   *
   * @param sink
   * @param size
   * @throws IOException
   */
  public DistributedPipedOutputStream(int size, HazelcastClusterServiceBean hzService)
  {
    this.hzService = hzService;
    bufferSize = size;
    this.hzService.addMessageChannel(this);
    connect();
  }
  
  
  private void connect() {
      circularBuffer = new byte[bufferSize];
      clear();
      setConnected(true);
  }
  @Override
  public void write(int b) throws IOException {
    if(!connected)
      throw new IOException("Stream not connected to any sink");
    if(closed)
      throw new IOException("Stream closed");
    
    writeByte((byte) b);
  }
  @Override
  public void write(byte[] b) throws IOException {
    if(!connected)
      throw new IOException("Stream not connected to any sink");
    if(closed)
      throw new IOException("Stream closed");
    
    Assert.notNull(b);
    for(byte _b : b)
    {
      writeByte(_b);
    }
  }
  private void writeByte(byte b) throws IOException
  {
      try {
          writeToChannel(b);
      } catch (InterruptedException e) {
          throw new InterruptedIOException();
      }
  }
  
  
  /**
   * blocking call
   * @throws InterruptedException
   */
  private void writeToChannel(byte b) throws InterruptedException {
      try 
      {         
          if(position == bufferSize)
          {
            flushBuffer();
            flushCount++;
            clear();
          }
          circularBuffer[position++] = b;
          //log.debug("bytes written -> ["+getBytesWritten()+"]");
      } finally {
          
      }
     
  }
  @Override
  public void flush()
  {
    flushBuffer();
  }
  private void flushBuffer() {
    sendMessage(Arrays.copyOf(circularBuffer, position));
    
  }
  /**
   * 
   */
  public void reset()
  {
    clear();
    flushCount = 0;
  }
  /* (non-Javadoc)
   * @see com.reactivetechnologies.platform.stream.IStream#clear()
   */
  @Override
  public void clear()
  {
    Arrays.fill(circularBuffer, (byte)-1);
    position = 0;
  }
  public int getBufferSize() {
      return bufferSize;
  }
  public void setBufferSize(int bufferSize) {
      this.bufferSize = bufferSize;
  }
  @Override
  public void close() 
  {
    flush();
    reset();
  }
  private volatile boolean closed = false;
  
  public boolean isOpen() {
      return !closed;
  }
  
  @Override
  public void onMessage(Message<byte[]> message) {
    //ignore
    
  }

  @Override
  public void sendMessage(byte[] message) {
    hzService.publish(message, topic());
    log.debug("Flushed bytes- "+message.length);
  }
  public boolean isConnected() {
    return connected;
  }
  private void setConnected(boolean connected) {
    this.connected = connected;
  }
  @Override
  public String topic() {
    return Configurator.PIPED_TOPIC_FILE;
  }
  @Override
  public void disconnect() {
    if (!closed) {
      close();
      circularBuffer = null;
      closed = true;
    }
    
  }

}
