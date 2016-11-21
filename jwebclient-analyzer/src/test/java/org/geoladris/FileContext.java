package org.geoladris;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.geoladris.Context;

public class FileContext implements Context {

  private File root;

  public FileContext(String root) {
    this.root = new File(root);
  }

  @Override
  public Set<String> getLibPaths() {
    File[] jars = new File(root, "WEB-INF/lib").listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".jar");
      }
    });

    HashSet<String> ret = new HashSet<String>();
    if (jars != null) {
      for (File file : jars) {
        ret.add("WEB-INF/lib/" + file.getName());
      }
    }
    return ret;
  }

  @Override
  public InputStream getLibAsStream(String jarFileName) {
    try {
      return new BufferedInputStream(new FileInputStream(new File(root, jarFileName)));
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  @Override
  public File[] getDirs() {
    return new File[] {new File(root, "WEB-INF/classes/" + JEEContextAnalyzer.CLIENT_RESOURCES_DIR),
        new File(root, "plugins")};
  }
}
