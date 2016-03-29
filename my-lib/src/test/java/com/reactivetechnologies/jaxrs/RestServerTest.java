/* ============================================================================
*
* FILE: RestServerTest.java
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
package com.reactivetechnologies.jaxrs;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.reactivetechnologies.platform.Configurator;
import com.reactivetechnologies.platform.rest.WebbitRestServerBean;
import com.reactivetechnologies.platform.rest.rt.Serveable;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Configurator.class})
@Configuration
public class RestServerTest {

  static int PORT = 8992;
  static String CTX_URL = "http://localhost:"+PORT;
  @Bean
  public Serveable restServer()
  {
    Serveable rb = new WebbitRestServerBean(PORT, 1, "com.reactivetechnologies.jaxrs");
    return rb;
  }
  
  static String sendGet(String url) throws IOException {

    StringBuilder response = new StringBuilder();
    HttpURLConnection con = null;
    BufferedReader in = null;
    try 
    {
      URL obj = new URL(url);
      con = (HttpURLConnection) obj.openConnection();

      // optional default is GET
      con.setRequestMethod("GET");

      //add request header
      con.setRequestProperty("User-Agent", "Mozilla/5.0");

      int responseCode = con.getResponseCode();
      if(responseCode == HttpURLConnection.HTTP_OK)
      {
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
      }
      else
      {
        throw new IOException("Response Code: "+responseCode);
      }
      
      return response.toString();
    } catch (IOException e) {
      throw e;
    }
    finally
    {
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

  }
  
  static String sendPost(String url, String content) throws IOException {

    StringBuilder response = new StringBuilder();
    HttpURLConnection con = null;
    BufferedReader in = null;
    try 
    {
      URL obj = new URL(url);
      con = (HttpURLConnection) obj.openConnection();

      // optional default is GET
      con.setRequestMethod("POST");

      //add request header
      con.setRequestProperty("User-Agent", "Mozilla/5.0");
      con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
      
      // Send post request
      con.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(con.getOutputStream());
      wr.writeUTF(content);
      wr.flush();
      wr.close();

      int responseCode = con.getResponseCode();
      if(responseCode == HttpURLConnection.HTTP_OK)
      {
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
      }
      else
      {
        throw new IOException("Response Code: "+responseCode);
      }
      
      return response.toString();
    } catch (IOException e) {
      throw e;
    }
    finally
    {
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

  }
  
  static String sendDelete(String url, String content) throws IOException {

    StringBuilder response = new StringBuilder();
    HttpURLConnection con = null;
    BufferedReader in = null;
    try 
    {
      URL obj = new URL(url);
      con = (HttpURLConnection) obj.openConnection();

      // optional default is GET
      con.setRequestMethod("DELETE");

      //add request header
      con.setRequestProperty("User-Agent", "Mozilla/5.0");
      con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
      
      // Send post request
      con.setDoOutput(true);
      DataOutputStream wr = new DataOutputStream(con.getOutputStream());
      wr.writeUTF(content);
      wr.flush();
      wr.close();

      int responseCode = con.getResponseCode();
      if(responseCode == HttpURLConnection.HTTP_OK)
      {
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
          response.append(inputLine);
        }
      }
      else
      {
        throw new IOException("Response Code: "+responseCode);
      }
      
      return response.toString();
    } catch (IOException e) {
      throw e;
    }
    finally
    {
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

  }
  @Test
  public void testPlainGet()
  {
    try {
      String resp = sendGet(CTX_URL+"/hello");
      Assert.assertTrue(resp.contains("hello"));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }
  
}
