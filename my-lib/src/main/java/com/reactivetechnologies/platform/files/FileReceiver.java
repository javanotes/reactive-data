/* ============================================================================
*
* FILE: FileChunkReceiver.java
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
package com.reactivetechnologies.platform.files;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Message;
import com.reactivetechnologies.platform.Configurator;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.AbstractMessageChannel;
/**
 * An abstract single threaded piped output stream. Will be distributed in nature.
 * The 'connect' to the input stream is via a Hazelcast topic.
 */
public class FileReceiver extends AbstractMessageChannel<FileChunk> {

  private static final Logger log = LoggerFactory.getLogger(FileReceiver.class);
  /**
   * 
   * @param hzService
   */
  public FileReceiver(HazelcastClusterServiceBean hzService) {
    super(hzService, true);
    
  }
  
  
  @Override
  public String topic() {
    return Configurator.PIPED_TOPIC_FILE;
  }
  private SynchronousQueue<FileChunk> queue = new SynchronousQueue<>(true);
  
  /**
   * Blocking get.
   * @return
   * @throws InterruptedException
   */
  public FileChunk get() throws InterruptedException
  {
    return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
  }
  /**
   * Blocking get
   * @param duration
   * @param unit
   * @return
   * @throws InterruptedException
   */
  public FileChunk get(long duration, TimeUnit unit) throws InterruptedException
  {
    return queue.poll(duration, unit);
  }
  private volatile boolean markDiscard;
  @Override
  public void onMessage(final Message<FileChunk> message) {
    if(message.getPublishingMember().localMember())
    {
      return;
    }
    if(isMarkDiscard()){
      //log.debug(message.getMessageObject()+"");
      discardChunks++;
      return;
    }
    try 
    {
      boolean offered = queue.offer(message.getMessageObject(), 10, TimeUnit.SECONDS);
      if(!offered)
        log.error("Chunk lost. Unable to queue even after waiting 10 secs!");
      
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

    }
    
  }

  private volatile int discardChunks;

  public boolean isMarkDiscard() {
    return markDiscard;
  }

  /**
   * If the receiver should ignore messages
   */
  public void markDiscard() {
    this.markDiscard = true;
    log.debug("* Chunks discarded previously * "+discardChunks);
    discardChunks = 0;
  }
  /**
   * Unmark discarded
   */
  public void unmarkDiscard() {
    this.markDiscard = false;
  }
  
}
