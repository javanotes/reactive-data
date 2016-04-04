/* ============================================================================
*
* FILE: JarFileSharingAgent.java
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
package com.reactivetechnologies.platform.rest.dll;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.reactivetechnologies.platform.Configurator;
import com.reactivetechnologies.platform.files.dist.AbstractFileSharingAgent;
import com.reactivetechnologies.platform.files.io.BufferedStreamChunkHandler;
import com.reactivetechnologies.platform.files.io.FileChunkHandler;
import com.reactivetechnologies.platform.files.io.MemoryMappedChunkHandler;
/**
 * Jar file sharing agent
 */
public class JarFileSharingAgent extends AbstractFileSharingAgent {

  private static final Logger log = LoggerFactory.getLogger(JarFileSharingAgent.class);
  /**
   * 
   */
  public JarFileSharingAgent() {
    super();
  }
  
  @Value("{restserver.jaxrs.extDir}")
  private String extLib;
  
  
  @Override
  protected FileChunkHandler newWriteHandler(String dirPath) throws IOException {
    return new BufferedStreamChunkHandler(dirPath){
      /**
       * Override the default behaviour to keep a backup
       * @throws IOException 
       */
      @Override
      protected void moveExistingFile() throws IOException 
      {
        //super.moveExistingFile();
        
        try {
          if (file.exists()) {
            Path fp = file.toPath();
            Path backupFile = Files
                .move(fp,
                    fp.resolveSibling(file.getName() + ".bkp."
                        + new SimpleDateFormat("yyyyMMddHHmmss")
                            .format(new Date())),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
            
            log.info("Backup file created- "+backupFile);
          }
          
          file.createNewFile();
        } catch (IOException e) {
          log.warn("Backup not taken. Going with default replace and copy. error => "+e.getMessage());
          log.debug("", e);
          super.moveExistingFile();
        }
                
      }
    };
  }

  @Override
  protected FileChunkHandler newReadHandler(File f) throws IOException {
    return new MemoryMappedChunkHandler(f, Configurator.DEFAULT_CHUNK_SIZE_BYTES);
  }
  @Override
  protected void onFileReceiptSuccess(File file) {
    try {
      moveToExtLibDir(file);
    } catch (IOException e) {
      log.error("Failed to move received jar to ext lib directory", e);
    }
    
  }

  private void moveToExtLibDir(File file) throws IOException {
    Files.move(file.toPath(), Paths.get(extLib).resolve(file.getName()),
        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES,
        StandardCopyOption.ATOMIC_MOVE);

  }

  @Override
  protected void onFileReceiptFailure(ExecutionException cause) {
    log.error("Exception on file receipt", cause);
    
  }

}
