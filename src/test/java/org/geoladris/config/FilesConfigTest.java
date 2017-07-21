package org.geoladris.config;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.io.IOUtils;
import org.geoladris.Plugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FilesConfigTest {
  private FilesConfig config;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setup() {
    config = new FilesConfig(folder.getRoot(), new ArrayList<PluginConfigProvider>(),
        new HashSet<Plugin>(), false, -1);
  }

  @Test
  public void missingPropertiesFile() {
    Properties properties = config.readProperties();
    assertEquals(0, properties.size());
  }

  @Test
  public void validPropertiesFile() throws Exception {
    File file = new File(config.getDir(), "portal.properties");
    IOUtils.write("a=1", new FileOutputStream(file));

    Properties properties = config.readProperties();
    assertEquals(1, properties.size());
    assertEquals("1", properties.getProperty("a"));
  }

  @Test
  public void missingMessagesFile() throws Exception {
    folder.newFolder("messages");
    ResourceBundle bundle = config.getResourceBundle(Locale.ENGLISH);
    assertEquals(0, bundle.keySet().size());
  }

  @Test
  public void validMessagesFile() throws Exception {
    File file = new File(folder.newFolder("messages"), "messages_en.properties");
    IOUtils.write("a=1\n", new FileOutputStream(file));
    ResourceBundle bundle = config.getResourceBundle(Locale.ENGLISH);
    assertEquals(1, bundle.keySet().size());
    assertEquals("1", bundle.getString("a"));
  }
}
