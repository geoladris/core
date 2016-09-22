package org.geoladris;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

public interface Context {

  Set<String> getLibPaths();

  InputStream getLibAsStream(String jarFileName);

  File getClientRoot();

  File getNoJavaRoot();
}
