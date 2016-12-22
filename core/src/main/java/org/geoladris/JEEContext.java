package org.geoladris;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import javax.servlet.ServletContext;

import org.geoladris.Context;
import org.geoladris.JEEContextAnalyzer;

public class JEEContext implements Context {
  private ServletContext servletContext;
  private File noJavaRoot;

  public JEEContext(ServletContext servletContext, File noJavaRoot) {
    this.servletContext = servletContext;
    this.noJavaRoot = noJavaRoot;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<String> getLibPaths() {
    return servletContext.getResourcePaths("/WEB-INF/lib");
  }

  @Override
  public InputStream getLibAsStream(String jarFileName) {
    return servletContext.getResourceAsStream(jarFileName);
  }

  @Override
  public File[] getDirs() {
    return new File[] {
        new File(servletContext
            .getRealPath("/WEB-INF/classes/" + JEEContextAnalyzer.CLIENT_RESOURCES_DIR)),
        noJavaRoot};
  }
}
