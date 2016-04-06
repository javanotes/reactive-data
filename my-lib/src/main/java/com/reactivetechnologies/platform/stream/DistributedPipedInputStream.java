/* ============================================================================
*
* FILE: DistributedPipedInputStream.java
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
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.InterruptibleChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Message;
import com.reactivetechnologies.platform.Configurator;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.MessageChannel;
/**
 * A single threaded piped input stream. Will be distributed in nature.
 * The 'connect' to the output stream is via a Hazelcast topic.
 * 
 */
public class DistributedPipedInputStream extends InputStream implements InterruptibleChannel, MessageChannel<byte[]>, Buffered{

  protected static final Logger log = LoggerFactory.getLogger(DistributedPipedInputStream.class);
  private ByteBuffer writeBuffer;
  private boolean connected = false;
  
  private final HazelcastClusterServiceBean hzService;
  private void setConnected(boolean connected) {
      this.connected = connected;
  }
  /**
   * 
   * @param hzService
   */
  public DistributedPipedInputStream(HazelcastClusterServiceBean hzService)
  {
     this(Configurator.DEFAULT_CHUNK_SIZE_BYTES, hzService);
  }
  private String topicRegId;
  /**
   * 
   * @param bufferSize
   * @param hzService
   */
  public DistributedPipedInputStream(int bufferSize, HazelcastClusterServiceBean hzService)
  {
      this.hzService = hzService;
      writeBuffer = ByteBuffer.allocate(bufferSize);
      topicRegId = this.hzService.addMessageChannel(this, true);
      setConnected(true);
  }
  @Override
  public int read() throws IOException {
      if(!connected)
          throw new IOException("Stream not connected to any source");
      if(closed)
        throw new IOException("Stream closed");
      return readByte();
  }
  
  private byte readByte() throws InterruptedIOException
  {
      try {
          return readByteFromChannel();
      } catch (InterruptedException e) {
          throw new InterruptedIOException();
      }
  }
  public boolean isBlocked()
  {
      while(!b1.compareAndSet(false, true));
      try {
          return reader != null && reader.isAlive();
      } finally {
          b1.compareAndSet(true, false);
      }
     
  }
  private final AtomicBoolean b1 = new AtomicBoolean();
  private Thread reader;
  /**
   *   
   * @return
   * @throws InterruptedException
   */
  private Byte readByteFromChannel() throws InterruptedException
  {
      Byte _byte = null;
      lock.lock();
      try 
      {
          reader = Thread.currentThread();
          if(readBuffer != null && readBuffer.hasRemaining()){
            _byte = readBuffer.get();
          }
          else
          {
            spaceAvailable.signalAll();
            bytesAvailable.await();
            
          }
            
    } finally {
      lock.unlock();
      while (!b1.compareAndSet(false, true))
        ;
      reader = null;
      b1.compareAndSet(true, false);

    }
      return _byte;
  }
  private volatile boolean closed = false;
  /* (non-Javadoc)
   * @see com.reactivetechnologies.platform.stream.IStream#clear()
   */
  @Override
  public void clear()
  {
    if (readBuffer != null) {
      readBuffer.clear();
    }
    writeBuffer.clear();
  }
  @Override
  public void close() throws IOException {
    clear();
    super.close();  
  }
  @Override
  public void disconnect()
  {
    if (!closed) {
      while (!b1.compareAndSet(false, true))
        ;
      if (reader != null) {
        reader.interrupt();
      }
      b1.compareAndSet(true, false);
      clear();
      hzService.removeMessageChannel(topic(), topicRegId);
      closed = true;
    }
  }
  @Override
  public boolean isOpen() {
      return !closed;
  }
  private final Lock lock = new ReentrantLock();
  private final Condition bytesAvailable = lock.newCondition();
  private final Condition spaceAvailable = lock.newCondition();
  
  private byte[] nextBytes;
  private ByteBuffer readBuffer;
  
  @Override
  public void onMessage(Message<byte[]> message) {
    if(closed)
      return;
    
    if(message.getPublishingMember().localMember())
    {
      log.debug("Ignoring bytes received from self..");
      return;
    }
    byte[] bytesRecvd = message.getMessageObject();
    log.debug("Got bytes of length- "+bytesRecvd.length);
    try
    {
      handleBytesReceived(bytesRecvd);
    }
    finally
    {
      
    }
    
    
  }
  /**
   * Awaits for a specified duration if available = 0. Can be used for polling.
   * @param duration
   * @param unit
   * @return
   * @throws InterruptedException
   */
  public int awaitAvailable(long duration, TimeUnit unit) throws InterruptedException
  {
    if(available() == 0) {
      synchronized (this) {
        if(available() == 0){
          try {
            reader = Thread.currentThread();
            wait(unit.toMillis(duration));
          } finally {
            reader = null;
          }
        }
      }
    }
    
    return available();
  }
  /**
   * Awaits for a specified duration until available != 0. Can be used for blocking.
   * @param duration
   * @param unit
   * @return
   * @throws InterruptedException
   */
  public int awaitUntilAvailable(long duration, TimeUnit unit) throws InterruptedException
  {
    lock.lock();
    try
    {
      if(_available(false) == 0)
      {
        reader = Thread.currentThread();
        bytesAvailable.await(duration, unit);
      }
    }
    finally{
      reader = null;
      lock.unlock();
    }
    
    return available();
  }
  
  private int _available(boolean flush)
  {
    if(readBuffer != null && readBuffer.remaining() != 0)
    {
      return readBuffer.remaining();
    }
    else
    {
      int available = bufferedCount;
      if (flush && available > 0) {
        lock.lock();
        try 
        {
          copyAndFlush(true);
          log.debug("available => " + readBuffer.remaining());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("", e);
        } finally {
          lock.unlock();
        } 
      }
      return available;
    }
  }
  @Override
  public int available()
  {
    return _available(true);
    
  }
  private void handleBytesReceived(byte[] receivedBytes)
  {
    lock.lock();
    try 
    {
      try 
      {
        doBufferedCopy(receivedBytes);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        e.printStackTrace();
      }      
      
    } finally {
      lock.unlock();
    }
  }
  
  private void appendToPrevBytes(byte[] thisBytes)
  {
    byte[] addedBytes = new byte[nextBytes.length + thisBytes.length];
    
    System.arraycopy(nextBytes, 0, addedBytes, 0, nextBytes.length);
    System.arraycopy(thisBytes, 0, addedBytes, nextBytes.length, thisBytes.length);
    
    nextBytes = addedBytes;
  }
  private volatile int bufferedCount = 0;
  /**
   * Copy from the write buffer to read buffer.
   * @param forceFlush
   * @throws InterruptedException
   */
  private void copyAndFlush(boolean forceFlush) throws InterruptedException
  {
    if(nextBytes == null || nextBytes.length == 0)
      return;
    log.debug("copyAndFlush() writeBuffer.remaining => "+writeBuffer.remaining());
    if(writeBuffer.remaining() >= nextBytes.length)
    {
      writeBuffer.put(nextBytes);
      nextBytes = null;
      bufferedCount = writeBuffer.position();
      
      if(forceFlush){
        flushBuffers();
        bufferedCount = 0;
      }
      log.debug("copyAndFlush(non overflow) bufferedCount => "+bufferedCount);
    }
    else
    {
      int copyLen = 0;
      if (writeBuffer.hasRemaining()) 
      {
        copyLen = writeBuffer.remaining();
        writeBuffer.put(nextBytes, 0, copyLen);
        
      }
      
      byte[] remBytes = new byte[nextBytes.length - copyLen];
      System.arraycopy(nextBytes, copyLen, remBytes, 0, remBytes.length);
      nextBytes = remBytes;
      bufferedCount = nextBytes.length;
      
      log.debug("copyAndFlush(overflow) bufferedCount => " + bufferedCount);
      
      flushBuffers();
            
    }
  }
  private void doBufferedCopy(final byte[] thisBytes) throws InterruptedException
  {
    if(nextBytes != null)
    {
      appendToPrevBytes(thisBytes);
    }
    else
      nextBytes = thisBytes;
    
    log.debug("doBufferedCopy() => "+nextBytes.length);
    
    copyAndFlush(false);
  }
  
  private byte[] fetchReadBytes()
  {
    writeBuffer.flip();
    byte[] readableBytes = new byte[writeBuffer.limit()];
    writeBuffer.get(readableBytes);
    log.debug("fetchReadBytes() => "+readableBytes.length);
    return readableBytes;
  }
  /**
   * 
   * @param readableBytes 
   * @throws InterruptedException
   */
  private void copyReadBytes(byte[] readableBytes) throws InterruptedException
  {
    if(readBuffer != null && readBuffer.hasRemaining())
    {
      try 
      {
        log.debug("---------- copyReadBytes waiting --------");
        boolean b = spaceAvailable.await(300, TimeUnit.SECONDS);
        if(!b)
          throw new IllegalStateException("No consumer seems to be available for ready bytes even after waiting 300 secs. "
              + "Is there a corresponding DistributedPipedInputStream configured on the same Topic?");
        log.debug("---------- copyReadBytes released --------");
      } catch (InterruptedException e) {
        throw e;
      }
    }
      
    if(readBuffer == null)
    {
      readBuffer = ByteBuffer.wrap(readableBytes);
    }
    else
    {
      readBuffer.clear();
      readBuffer.put(readableBytes);
      readBuffer.flip();
    }
    log.debug("copyReadBytes() => "+readableBytes.length);
  }
  /**
   * 
   * @throws InterruptedException
   */
  private void flushBuffers() throws InterruptedException 
  {
    byte[] readableBytes = fetchReadBytes();
    copyReadBytes(readableBytes);
    
    bytesAvailable.signalAll();
    writeBuffer.clear();
  }
  
  @Override
  public void sendMessage(byte[] message) {
    throw new UnsupportedOperationException("Out of scope");
  }
  @Override
  public String topic() {
    return Configurator.PIPED_TOPIC_FILE;
  }
}
