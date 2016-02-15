/* ============================================================================
 *
 * FILE: ResourceLoaderHelper.java
 *
 * MODULE DESCRIPTION:
 * See class description
 *
 * Copyright (C) 2015 by
 * ERICSSON
 *
 * The program may be used and/or copied only with the written
 * permission from Ericsson Inc, or in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program has been supplied.
 *
 * All rights reserved
 *
 * ============================================================================
 */

package com.reactivetechnologies.platform.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.springframework.util.ResourceUtils;


public class ResourceLoaderHelper {

  
  /**
   * Loads a resource as a file system resource or as a classpath resource  
   * @param resource
   * @return
   * @throws IOException
   */
  public static File loadFromFileOrClassPath(String resource) throws IOException
  {
    File f;
    ;
    try {
      f = ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX+resource);
      if (f.exists() && f.isFile()) {
        return f;
      }
    } catch (IOException e) {
      try {
        f = ResourceUtils.getFile(ResourceUtils.FILE_URL_PREFIX+resource);
        if (f.exists() && f.isFile()) {
          return f;
        }
      } catch (IOException e1) {
        FileNotFoundException ffe = new FileNotFoundException("Resource ["+resource+"] not found.");
        ffe.initCause(e1);
        throw ffe;
      }
    }
    throw new FileNotFoundException("Resource ["+resource+"] does not exist/not a valid file.");
    
  }
  
}
