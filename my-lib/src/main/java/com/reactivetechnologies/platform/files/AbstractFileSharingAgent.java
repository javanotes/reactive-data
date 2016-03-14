/* ============================================================================
*
* FILE: AbstractFileSharingAgent.java
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

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import com.reactivetechnologies.platform.Configurator;
import com.reactivetechnologies.platform.OperationsException;
import com.reactivetechnologies.platform.datagrid.core.HazelcastClusterServiceBean;
import com.reactivetechnologies.platform.datagrid.handlers.MessageChannel;
/**
 * Implementation of file sharing agents can extend this class and simply provide a read {@linkplain FileChunkHandler}
 * and write {@linkplain FileChunkHandler}.
 * @see  {@linkplain BufferedStreamChunkHandler}, {@linkplain MemoryMappedChunkHandler}
 */
public abstract class AbstractFileSharingAgent implements MessageChannel<Byte>, FileSharingAgent{

  static final byte SEND_FILE       = 0b00000001;
  static final byte SEND_FILE_ACK   = 0b00000011;
  static final byte RECV_FILE_ERR       = 0b00000110;
  static final byte RECV_FILE_ACK   = 0b00000111;
  
  @Autowired
  protected HazelcastClusterServiceBean hzService;
  private ExecutorService threads;
  
  /**
   * 
   */
  public AbstractFileSharingAgent()
  {
    
  }
  private static final Logger log = LoggerFactory.getLogger(AbstractFileSharingAgent.class);
  protected FileReceiver receiver;
  protected FileSender sender;
  
  private void registerSelf()
  {
    hzService.addMessageChannel(this);
    receiver = new FileReceiver(hzService);
    sender = new FileSender(hzService);
  }
  @PreDestroy
  protected void onunload()
  {
    threads.shutdown();
    try {
      threads.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      
    }
  }
  private void startHandlers()
  {
    threads = Executors.newSingleThreadExecutor(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "FileSharingAgent.Worker");
        return t;
      }
    });
    
  }
  @PostConstruct
  protected void onload()
  {
    registerSelf();
    startHandlers();
    log.debug(getClass().getSimpleName()+" initiated..");
  }
  @Value("${keyval.files.receive.targetDir}")
  private String fileWriteDir;
  @Value("${keyval.files.send.requestAck.secs:30}")
  private long sendAwaitSecs;
  @Value("${keyval.files.send.synchronize.secs:10}")
  private long lockAwaitSecs;
  @Value("${keyval.files.send.receiptAck.secs:600}")
  private long receiptAwaitSecs;
  /**
   * 
   */
  private SecureRandom rand = new SecureRandom();
  private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
  /**
   * File consuming task handler. This task is executed on the receiving node
   */
  private class FileConsumingTask implements Runnable
  {
    
    private AtomicInteger retryCount = new AtomicInteger();
    @SuppressWarnings("unused")
    boolean retry()
    {
      return retryCount.getAndIncrement() < 3;
    }
    private final long id;
    public FileConsumingTask() {
      super();
      this.id = rand.nextLong();
    }
    
    private FileChunk firstChunk = null;
    private long bytesRead = 0;
    @Override
    public void run() 
    {
      FileChunk chunk = null;
      boolean isLastChunk = false;
      
      String dirPath = fileWriteDir + File.separator + df.format(new Date());
      try (FileChunkHandler writer = newWriteHandler(dirPath)) {
          
        while (!isLastChunk) 
        {
          chunk = receiver.get();
          log.debug("Writing chunk=> "+chunk);
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
   * Create a new instance of {@linkplain AbstractFileChunkHandler} for writing the consumed
   * file bytes.
   * @param dirPath
   * @return
   * @throws IOException 
   */
  protected abstract FileChunkHandler newWriteHandler(String dirPath) throws IOException;
  /**
   * Create a new instance of {@linkplain AbstractFileChunkHandler} for reading the file to be shared.
   * @param f
   * @return
   * @throws IOException 
   */
  protected abstract FileChunkHandler newReadHandler(File f) throws IOException;
  /* (non-Javadoc)
   * @see com.reactivetechnologies.platform.files.FileSharingAgent#distribute(java.io.File)
   */
  @Override
  public Future<FileShareResponse> distribute(File f) throws IOException, OperationsException
  {
    try 
    {
      if(tryLock(lockAwaitSecs, TimeUnit.SECONDS))
      {
        if(!initSendFileRequest())
          throw new OperationsException("Unable to initiate a cluster wide send file request in "+lockAwaitSecs+" secs");
        
        Future<FileShareResponse> response = waitForFileReceiptAckAsync();
        
        try(FileChunkHandler reader = newReadHandler(f))
        {
          FileChunk fc = null;
          while((fc = reader.readNext()) != null)
          {
            sender.sendMessage(fc);
          }
        }
        
        return response;
      }
      else
      {
        throw new OperationsException("Operation not allowed at this time. Probably some other sharing is running.");
      }
    } 
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new OperationsException("Interrupted while trying to acquire cluster lock.", e);
    }
    finally
    {
      unlock();
    }
    
  }
  private final AtomicInteger sharingErrorCount = new AtomicInteger();
  private volatile boolean awaitingFileReceiptAck, awaitingFileSendAck;
  /**
   * 
   * @return
   */
  public boolean isAwaitingFileReceiptAck() {
    return awaitingFileReceiptAck;
  }
  /**
   * 
   * @return
   */
  public boolean isAwaitingFileSendAck() {
    return awaitingFileSendAck;
  }
  private Set<Member> fileReceiptErrored;
  /**
   * This task is executed on the node which shares the file
   */
  private class FileShareResponseTask implements Callable<FileShareResponse>
  {

    @Override
    public FileShareResponse call() throws Exception {

      FileShareResponse r = null;
      try 
      {
        awaitingFileReceiptAck = true;
        fileReceiptErrored = new HashSet<>();
        boolean latched = latch.await(receiptAwaitSecs, TimeUnit.SECONDS);
        if (latched) {
          log.info(">> File sharing process returned successfully. Error count: "+sharingErrorCount);
          r = FileShareResponse.FINISH;
          r.setErrorCount(sharingErrorCount.get());
          for(Member m : fileReceiptErrored)
          {
            r.getErrorNodes().add(m.getStringAttribute(Configurator.NODE_INSTANCE_ID));
          }
          
        }
        else
        {
          log.error(">> File sharing process timed out after waiting "+receiptAwaitSecs+" seconds <<");
          r = FileShareResponse.TIMEOUT;
          r.setErrorCount(sharingErrorCount.get());
          for(Member m : fileReceiptErrored)
          {
            r.getErrorNodes().add(m.getStringAttribute(Configurator.NODE_INSTANCE_ID));
          }
          
        }
      } catch (InterruptedException e) {
        log.error("", e);
      }
      finally
      {
        latch = null;
        awaitingFileReceiptAck = false;
        fileReceiptErrored = null;
      }
      return r;
      
    
    }
    
  }
  /**
   * Awaits file receipt acknowledgement from members, for a maximum of 600 seconds.
   * @return 
   */
  private Future<FileShareResponse> waitForFileReceiptAckAsync() 
  {
    latch = new CountDownLatch(hzService.size() - 1);
    sharingErrorCount.getAndSet(0);
        
    return threads.submit(new FileShareResponseTask());
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
  /**
   * Sends a signal to cluster that a new file sharing will start.
   * Will wait for 30 seconds to receive acknowledgement from members.
   * @return if success
   */
  private boolean initSendFileRequest() 
  {
    try 
    {
      latch = new CountDownLatch(hzService.size() - 1);
      sendMessage(SEND_FILE);
      awaitingFileSendAck = true;
      return latch.await(sendAwaitSecs, TimeUnit.SECONDS);
        
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.debug("", e);
    }
    finally
    {
      latch = null;
      awaitingFileSendAck = false;
    }
    return false;
  }
  @Override
  public void onMessage(Message<Byte> message) 
  {
    if(message.getPublishingMember().localMember())
      return;
    
    switch(message.getMessageObject())
    {
      case SEND_FILE_ACK:
      if (awaitingFileSendAck) {
        latch.countDown();
      }
      break;
      case SEND_FILE:
        startFileConsumingTask();
        break;
      case RECV_FILE_ACK:
        if (awaitingFileReceiptAck) {
          latch.countDown();
        }
        break;
      case RECV_FILE_ERR:
        if (awaitingFileReceiptAck) {
          latch.countDown();
          sharingErrorCount.incrementAndGet();
          fileReceiptErrored.add(message.getPublishingMember());
          log.error("*** RECV_FILE_ERR ***");
        }
        
        break;
        default: break;
    }
    
  }
  /**
   * Acknowledge a new file to be received, and starts a separate thread for consuming.
   * the data
   * @return 
   */
  private long startFileConsumingTask() {
    FileConsumingTask ah = new FileConsumingTask();
    threads.submit(ah);
    sendMessage(SEND_FILE_ACK);
    log.info("New file consuming task submitted- "+ah.id);
    return ah.id;
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
