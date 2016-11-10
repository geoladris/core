package org.geoladris.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.geoladris.Environment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ConfigFolderTest {
  private Properties systemProperties;
  private File configDir;

  @Before
  public void setup() throws IOException {
    this.systemProperties = System.getProperties();
    Properties properties = new Properties();
    properties.putAll(this.systemProperties);
    System.setProperties(properties);

    this.configDir = File.createTempFile("geoladris", "");
    this.configDir.delete();
    this.configDir.mkdir();

  }

  @After
  public void teardown() throws IOException {
    System.setProperties(this.systemProperties);
    FileUtils.deleteDirectory(this.configDir);
  }

  @Test
  public void noPortalConfigDirProperty() throws Exception {
    String appName = "core";
    String root = "/var/lib/tomcat/webapps/" + appName;
    ServletContext context = mockContext(appName, root);
    ConfigFolder folder = new ConfigFolder(context, new Environment());
    assertEquals(new File(root, "WEB-INF/default_config"), folder.getFilePath());
  }

  @Test
  public void nonExistingPortalConfigDir() throws Exception {
    String appName = "core";
    String root = "/var/lib/tomcat/webapps/" + appName;
    ServletContext context = mockContext(appName, root);

    System.setProperty(Environment.CONFIG_DIR, "/var/geoladris");
    ConfigFolder folder = new ConfigFolder(context, new Environment());
    assertEquals(new File(root, "WEB-INF/default_config"), folder.getFilePath());
  }

  @Test
  public void validConfigDirUsesSubdirectory() throws Exception {
    String appName = "core";
    String root = "/var/lib/tomcat/webapps/" + appName;
    ServletContext context = mockContext(appName, root);

    File appConfigDir = new File(this.configDir, appName);
    appConfigDir.mkdirs();

    System.setProperty(Environment.CONFIG_DIR, this.configDir.getAbsolutePath());
    ConfigFolder folder = new ConfigFolder(context, new Environment());

    assertEquals(appConfigDir, folder.getFilePath());
  }

  @Test
  public void usesPortalDirIfGeoladrisMissing() throws Exception {
    String appName = "core";
    String root = "/var/lib/tomcat/webapps/" + appName;
    ServletContext context = mockContext(appName, root);

    System.setProperty(Environment.PORTAL_CONFIG_DIR, this.configDir.getAbsolutePath());
    ConfigFolder folder = new ConfigFolder(context, new Environment());

    assertEquals(this.configDir, folder.getFilePath());
  }

  @Test
  public void geoladrisDirTakesPrecedenceOverPortalDir() throws Exception {
    String appName = "core";
    String root = "/var/lib/tomcat/webapps/" + appName;
    ServletContext context = mockContext(appName, root);

    File appConfigDir = new File(this.configDir, appName);
    appConfigDir.mkdirs();

    System.setProperty(Environment.PORTAL_CONFIG_DIR, this.configDir.getAbsolutePath());
    System.setProperty(Environment.CONFIG_DIR, this.configDir.getAbsolutePath());
    ConfigFolder folder = new ConfigFolder(context, new Environment());

    assertEquals(appConfigDir, folder.getFilePath());
  }

  private ServletContext mockContext(String contextPath, final String rootPath) {
    ServletContext context = mock(ServletContext.class);
    when(context.getContextPath()).thenReturn(contextPath);
    when(context.getRealPath(anyString())).thenAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return rootPath + "/" + invocation.getArguments()[0].toString();
      }
    });

    return context;
  }
}
