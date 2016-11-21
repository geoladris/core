package org.geoladris.maven;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import org.geoladris.Context;
import org.geoladris.JEEContextAnalyzer;

public class ExpandedClientContext implements Context {

  private String jeeContextFolder;

  public ExpandedClientContext(String jeeContextFolder) {
    this.jeeContextFolder = jeeContextFolder;
  }

  @Override
  public Set<String> getLibPaths() {
    return Collections.emptySet();
  }

  @Override
  public InputStream getLibAsStream(String jarFileName) {
    throw new UnsupportedOperationException("Internal error");
  }

  @Override
  public File[] getDirs() {
    return new File[] {
        new File(jeeContextFolder + File.separator + JEEContextAnalyzer.CLIENT_RESOURCES_DIR)};
  }
}
