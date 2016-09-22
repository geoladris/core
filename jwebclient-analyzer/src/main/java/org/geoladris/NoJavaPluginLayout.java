package org.geoladris;

import java.io.File;

public class NoJavaPluginLayout implements PluginLayout {

  private File rootFolder;

  public NoJavaPluginLayout(File rootFolder) {
    super();
    this.rootFolder = rootFolder;
  }

  @Override
  public File getWebResourcesRoot() {
    return rootFolder;
  }

  @Override
  public boolean rootFolderExists() {
    return rootFolder.exists();
  }

  @Override
  public File getReferenceFolder() {
    return rootFolder;
  }

}
