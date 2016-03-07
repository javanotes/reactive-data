/* ============================================================================
*
* FILE: AsyncClientHandler.java
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
package com.reactivetechnologies.platform.rest.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
/**
 * Asynchronous client handler for fetching response in a asynchronous manner, given the 'Location' URL.
 * Use {@link #setUrl(String)} and then {@link #get()} or {@link #get(long, TimeUnit)} for doing a blocking call
 * to get the response, similar to a {@linkplain Future}
 */
public class AsyncClientHandler implements Runnable
{
  AsyncClientHandler()
  {
    
  }
  /**
   * Package private
   * @param url
   */
  AsyncClientHandler(String url) {
    super();
    this.url = url;
    
  }
  private String url = null;
  public String getUrl() {
    return url;
  }
  /**
   * Sets the url to check for response
   * @param url
   */
  public void setUrl(String url) {
    this.url = url;
    synchronized (this) {
      notify();
    }
  }
  private final SynchronousQueue<String> queue = new SynchronousQueue<>();
  private volatile boolean cancelled;
  /**
   * 
   * @param url
   * @return
   * @throws IOException
   */
  private String sendGet(String url) throws IOException {

    StringBuilder response = new StringBuilder();
    HttpURLConnection con = null;
    BufferedReader in = null;
    int responseCode = HttpURLConnection.HTTP_NO_CONTENT;
    String responseString = null;
    URL obj = new URL(url);
    do 
    {
      try 
      {
        if(responseCode != HttpURLConnection.HTTP_NO_CONTENT)
          throw new IOException("HTTP "+responseCode+": "+HttpResponseStatus.valueOf(responseCode).getReasonPhrase());
        
        if(cancelled)
          throw new InterruptedIOException();
          
        con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");
        con.setRequestProperty("Connection", "keep-alive");
        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) 
        {
          in = new BufferedReader(new InputStreamReader(con.getInputStream()));
          String inputLine;
          while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
          }
          
          responseString = response.toString();
          
        } 
                
      } 
      finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {

          }
        }
        if (con != null) {
          con.disconnect();
        }
      } 
    } while (responseCode != HttpURLConnection.HTTP_OK);
    
    return responseString;

  }
  
  @Override
  public void run() {
    pollForResponse();
    
  }
  private volatile boolean done;
  private void pollForResponse() {
    try 
    {
      if(url == null)
      {
        synchronized(this)
        {
          if(url == null)
            wait();
        }
      }
      String response = sendGet(url);
      done = true;
      queue.put(response);
    } catch (IOException e) {
      if(e instanceof InterruptedIOException)
      {
        queue.offer("cancel");
      }
      else
      {
        error = e;
        queue.offer("error");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    catch(Exception e)
    {
      error = e;
      queue.offer("error");
    }
    
  }
  private Exception error;
  public boolean cancel() {
    if(!done){
      cancelled = true;
      done = true;
    }
    return cancelled;
  }
  /**
   * 
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  public String get() throws InterruptedException, ExecutionException {
    String returned = queue.take();
    if("cancel".equals(returned))
      return "";
    if("error".equals(returned))
      throw new ExecutionException(error);
    return returned;
  }
  /**
   * 
   * @param timeout
   * @param unit
   * @return
   * @throws InterruptedException
   * @throws TimeoutException
   * @throws ExecutionException
   */
  public String get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
    String returned = queue.poll(timeout, unit);
    if(returned == null)
      throw new TimeoutException();
    if("cancel".equals(returned))
      return "";
    if("error".equals(returned))
      throw new ExecutionException(error);
    return returned;
  }
  
  public boolean isCancelled() {
    return cancelled;
  }
  
  public boolean isDone() {
    return done;
  }
}