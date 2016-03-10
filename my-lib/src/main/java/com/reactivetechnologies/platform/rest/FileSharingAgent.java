/* ============================================================================
*
* FILE: FileSharingAgent.java
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
package com.reactivetechnologies.platform.rest;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hazelcast.core.Message;
import com.reactivetechnologies.platform.Configurator;
import com.reactivetechnologies.platform.OperationsException;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.MessageChannel;
import com.reactivetechnologies.platform.files.BasicFileChunkHandler;
import com.reactivetechnologies.platform.files.FileChunk;
import com.reactivetechnologies.platform.files.FileChunkHandler;
import com.reactivetechnologies.platform.files.FileReceiver;
import com.reactivetechnologies.platform.files.FileSender;
@Component
public class FileSharingAgent implements MessageChannel<Byte>{

  static final byte SEND_FILE       = 0b00000001;
  static final byte SEND_FILE_ACK   = 0b00000011;
  static final byte RECV_FILE_ERR       = 0b00000110;
  static final byte RECV_FILE_ACK   = 0b00000111;
  
  @Autowired
  private HazelcastClusterServiceBean hzService;
  private ThreadPoolExecutor threads;
  
  /**
   * 
   */
  public FileSharingAgent()
  {
    
  }
  private static final Logger log = LoggerFactory.getLogger(FileSharingAgent.class);
  private FileReceiver receiver;
  private FileSender sender;
  
  private void registerSelf()
  {
    hzService.addMessageChannel(this);
    receiver = new FileReceiver(hzService);
    sender = new FileSender(hzService);
  }
  @PreDestroy
  void onunload()
  {
    threads.shutdown();
    try {
      threads.awaitTermination(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      
    }
  }
  private void startHandlers()
  {
    threads = (ThreadPoolExecutor) Executors.newFixedThreadPool(3, new ThreadFactory() {
      int n = 0;
      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "FileSharingAgent.Worker.Thread-"+(n++));
        return t;
      }
    });
    threads.setRejectedExecutionHandler(new RejectedExecutionHandler() {
      
      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        
        if(r instanceof AgentHandler)
        {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            
          }
          AgentHandler a = (AgentHandler) r;
          if(a.retry())
          {
            log.warn("Retrying execution for runnable: "+r);
            executor.submit(a);
          }
          else
          {
            log.error("Task rejected permanently after 3 retries. "+r);
          }
        }
      }
    });
  }
  @PostConstruct
  void onload()
  {
    registerSelf();
    startHandlers();
  }
  @Value("${}")
  private String fileWriteDir;
  /**
   * 
   */
  private SecureRandom rand = new SecureRandom();
  /**
   * 
   */
  private class AgentHandler implements Runnable
  {
    
    private AtomicInteger retryCount = new AtomicInteger();
    boolean retry()
    {
      return retryCount.getAndIncrement() < 3;
    }
    private final long id;
    public AgentHandler() {
      super();
      this.id = rand.nextLong();
    }
    
    private FileChunk firstChunk = null;
    private long bytesRead = 0;
    @Override
    public void run() {
      FileChunk chunk = null;
      boolean isLastChunk = false;

      try (BasicFileChunkHandler writer = new BasicFileChunkHandler(fileWriteDir)) {
          
        while (!isLastChunk) 
        {
          chunk = receiver.get();
          writer.writeNext(chunk);
          if (firstChunk == null) {
            firstChunk = chunk;
          }
          bytesRead += chunk.getChunk().length;
          isLastChunk = firstChunk.getFileSize() == bytesRead;
        }
        sendMessage(RECV_FILE_ACK);
        
      } catch (InterruptedException e) {
        log.error("Unable to fetch file bytes", e);
        sendMessage(RECV_FILE_ERR);
      } catch (IOException e1) {
        log.error("Unable to write file", e1);
        sendMessage(RECV_FILE_ERR);
      } 
    
      
    }
    
  }
  /**
   * Shares a file across the cluster. File sharing is an exclusive process.
   * So at a time only 1 sharing can be processed.
   * @param f
   * @throws IOException
   * @throws OperationsException recoverable exception, can be tried later probably
   */
  public void shareFile(File f) throws IOException, OperationsException
  {
    try 
    {
      if(tryLock(30, TimeUnit.SECONDS))
      {
        if(!initSendFileRequest())
          throw new OperationsException("Unable to initiate a cluster wide send file request in 30 secs");
        
        waitFileReceiptAckAsync();
        
        try(FileChunkHandler reader = new BasicFileChunkHandler(f, Configurator.DEFAULT_CHUNK_SIZE_BYTES))
        {
          FileChunk fc = null;
          while((fc = reader.readNext()) != null)
          {
            sender.sendMessage(fc);
          }
        }
        
        
      }
      else
      {
        throw new OperationsException("Operation not allowed at this time. Probably some other sharing is running.");
      }
    } 
    catch (InterruptedException e) {
      log.error("", e);
    }
    finally
    {
      unlock();
    }
    
  }
  private final AtomicInteger sharingErrorCount = new AtomicInteger();
  /**
   * 
   */
  private void waitFileReceiptAckAsync() 
  {
    latch = new CountDownLatch(hzService.size() - 1);
    sharingErrorCount.getAndSet(0);
    threads.submit(new Runnable() {
      
      @Override
      public void run() {
        try 
        {
          boolean latched = latch.await(600, TimeUnit.SECONDS);
          if (latched) {
            log.info(">> File sharing process returned successfully. Error count: "+sharingErrorCount);
          }
          else
            log.error(">> File sharing process failed after waiting 10 minutes <<");
        } catch (InterruptedException e) {
          log.error("", e);
        }
        
      }
    });
  }
  private CountDownLatch latch;
  /**
   * Distributed lock
   * @param duration
   * @param unit
   * @return
   * @throws InterruptedException
   */
  private boolean tryLock(long duration, TimeUnit unit) throws InterruptedException
  {
    Lock lock = hzService.getClusterLock("FileSharingAgent");
    return lock.tryLock(duration, unit);
  }
  /**
   * Distributed unlock
   */
  private void unlock()
  {
    Lock lock = hzService.getClusterLock("FileSharingAgent");
    lock.unlock();
  }
  private boolean initSendFileRequest() 
  {
    try 
    {
      latch = new CountDownLatch(hzService.size() - 1);
      sendMessage(SEND_FILE);
      return latch.await(30, TimeUnit.SECONDS);
        
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.debug("", e);
    }
    finally
    {
      latch = null;
    }
    return false;
  }
  @Override
  public void onMessage(Message<Byte> message) {
    if(message.getPublishingMember().localMember())
      return;
    
    switch(message.getMessageObject())
    {
      case SEND_FILE_ACK:
      if (latch != null) {
        latch.countDown();
      }
      break;
      case SEND_FILE:
        acknowledgeFileReceipt();
        break;
      case RECV_FILE_ACK:
        if (latch != null) {
          latch.countDown();
        }
        break;
      case RECV_FILE_ERR:
        if (latch != null) {
          latch.countDown();
          sharingErrorCount.incrementAndGet();
        }
        log.error("*** RECV_FILE_ERR ***");//TODO: handle?
        break;
        default: break;
    }
    
  }
  private void acknowledgeFileReceipt() {
    AgentHandler ah = new AgentHandler();
    threads.submit(ah);
    sendMessage(SEND_FILE_ACK);
  }
  @Override
  public String topic() {
    return "FileSharingAgent-Topic";
  }
  @Override
  public void sendMessage(Byte message) {
    hzService.publish(message, topic());   
  }
}
