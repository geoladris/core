package org.geoladris.config;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class ConfigFolderTest {
  @Test
  public void noPortalConfigDirProperty() throws Exception {
    String appName = "core";
    String root = "/var/lib/tomcat/webapps/" + appName;
    ConfigFolder folder = new ConfigFolder(appName, root, null);
    assertEquals(new File(root, "WEB-INF/default_config"), folder.getFilePath());
  }

  @Test
  public void nonExistingPortalConfigDir() throws Exception {
    String appName = "core";
    String root = "/var/lib/tomcat/webapps/" + appName;

    File confDir = new File("/var/geoladris");

    ConfigFolder folder = new ConfigFolder(appName, root, confDir.getAbsolutePath());
    assertEquals(new File(root, "WEB-INF/default_config"), folder.getFilePath());
  }

  @Test
  public void validConfigDirUsesSubdirectory() throws Exception {
    String appName = "core";
    String root = "/var/lib/tomcat/webapps/" + appName;

    File file = File.createTempFile("geoladris", "");
    file.delete();
    File appConfigDir = new File(file, appName);
    appConfigDir.mkdirs();

    ConfigFolder folder = new ConfigFolder(appName, root, file.getAbsolutePath());
    assertEquals(appConfigDir, folder.getFilePath());

    FileUtils.deleteDirectory(file);
  }
}
