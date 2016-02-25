/* ============================================================================
*
* FILE: AsyncRunnerExecutor.java
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
package com.reactivetechnologies.analytics;

import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.iki.elonen.NanoHTTPD.AsyncRunner;
import fi.iki.elonen.NanoHTTPD.ClientHandler;

class AsyncRunnerExecutor extends ThreadPoolExecutor implements AsyncRunner {
  
  private static final Logger log = LoggerFactory.getLogger(AsyncRunnerExecutor.class);
  private final ConcurrentSkipListSet<ClientHandler> handlers = new ConcurrentSkipListSet<>(new Comparator<ClientHandler>() {

    @Override
    public int compare(ClientHandler o1, ClientHandler o2) {
      return Integer.compare(o1.hashCode(), o2.hashCode());
    }
  });
  
  private static class ListeningFutureTask<V> extends FutureTask<V> {

    private final Runnable myTask;

    public ListeningFutureTask(Runnable runnable, V result) {

      super(runnable, result);
      this.myTask = runnable;

    }

  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  protected  <T> RunnableFuture newTaskFor(Runnable runnable, T value) {

      return new ListeningFutureTask(runnable, value);            

  }
  public AsyncRunnerExecutor(int nThreads) {
    super(nThreads, nThreads, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(nThreads));
    setThreadFactory(new ThreadFactory() {
      private int n=1;
      @Override
      public Thread newThread(Runnable arg0) {
        Thread t = new Thread(arg0, "HTTP-Threadpool-Executor-"+(n++));
        return t;
      }
    });
    
    setRejectedExecutionHandler(new RejectedExecutionHandler() {
      
      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if(r instanceof ListeningFutureTask)
        {
          @SuppressWarnings("rawtypes")
          ClientHandler ch = (ClientHandler) ((ListeningFutureTask) r).myTask;
          log.warn("**Rejecting client connection**"+ch);
          ch.close();
        }
        else
        {
          log.error("RejectedExecutionHandler not returning ListeningFutureTask!");
        }
        
      }
    });
  }
  @Override
  public void exec(ClientHandler code) {
    handlers.add(code);
    execute(code);
    
  }

  @Override
  public void closed(ClientHandler clientHandler) {
    handlers.remove(clientHandler);
  }

  @Override
  public void closeAll() {
    log.info("Shutting down async runner executor..");
    for(ClientHandler ch : handlers)
    {
      ch.close();
    }
    shutdown();
    try {
      awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      
    }
    
  }
}