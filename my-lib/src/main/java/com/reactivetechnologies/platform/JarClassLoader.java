/* ============================================================================
*
* FILE: JarClassLoader.java
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
package com.reactivetechnologies.platform;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public class JarClassLoader extends URLClassLoader {
  /**
   * 
   * @param urls
   * @param delegation
   */
  JarClassLoader(URL[] urls, ClassLoader delegation) {
    super(urls, delegation);

  }
  private static TreeSet<String> scanForPackages(String path) throws IOException
  {
    try(JarFile file = new JarFile(path))
    {
      TreeSet<String> packages = new TreeSet<>(new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
          if(o2.length() > o1.length() && o2.contains(o1))
            return -1;
          else if(o2.length() < o1.length() && o1.contains(o2))
            return 1;
          else
            return o1.compareTo(o2);
        }
      });
      for(Enumeration<JarEntry> entries = file.entries(); entries.hasMoreElements();)
      {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
                
        if(name.endsWith(".class"))
        {
          String fqcn = ClassUtils.convertResourcePathToClassName(name);
          fqcn = StringUtils.delete(fqcn, ".class");
          packages.add(ClassUtils.getPackageName(fqcn));
        }
      }
      
      return packages;
    }
  }
  /**
   * 
   * @param delegation
   */
  JarClassLoader(ClassLoader delegation) {
    this(new URL[] {}, delegation);
  }
  /**
   * 
   */
  JarClassLoader() {
    super(new URL[] {});
  }

  /**
   * Adds a jar to the class path. If convention is followed, 
   * the first element of the returned set will be the base package
   * 
   * @param path
   * @return The packages present in this jar.
   * @throws IOException 
   */
  public Set<String> addJar(String path) throws IOException {
    String urlPath = "jar:file://" + path + "!/";
    URL url = new URL(urlPath);
    addURL(url);
    return scanForPackages(path);
  }
  /**
   * Adds a jar to the class path. If convention is followed, 
   * the first element of the returned set will be the base package
   * @param file
   * @return
   * @throws IOException
   */
  public Set<String> addJar(File file) throws IOException {
    return addJar(file.getAbsolutePath());
  }
  
  /*public static void main(String[] args) {
    JarClassLoader jcl = new JarClassLoader();
    try {
      jcl.addJar("C:\\Users\\esutdal\\Downloads\\spark-1.5.2-bin-without-hadoop\\spark-1.5.2-bin-without-hadoop\\lib\\spark-1.5.2-yarn-shuffle.jar");
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }*/
}
