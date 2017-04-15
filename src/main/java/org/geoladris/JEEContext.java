package org.geoladris;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import javax.servlet.ServletContext;

public class JEEContext {
  private ServletContext servletContext;
  private File noJavaRoot;

  public JEEContext(ServletContext servletContext, File noJavaRoot) {
    this.servletContext = servletContext;
    this.noJavaRoot = noJavaRoot;
  }

  @SuppressWarnings("unchecked")
  public Set<String> getLibPaths() {
    return servletContext.getResourcePaths("/WEB-INF/lib");
  }

  public InputStream getLibAsStream(String jarFileName) {
    return servletContext.getResourceAsStream(jarFileName);
  }

  public File[] getDirs() {
    return new File[] {new File(servletContext.getRealPath("/WEB-INF/classes")), noJavaRoot};
  }
}
