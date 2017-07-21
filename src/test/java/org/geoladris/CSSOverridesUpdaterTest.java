package org.geoladris;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.geoladris.config.Config;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CSSOverridesUpdaterTest {
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private CSSOverridesUpdater updater;
  private File pluginsDir, staticDir, overridesCss;

  @Before
  public void setup() {
    Config config = mock(Config.class);
    when(config.getDir()).thenReturn(tmp.getRoot());
    this.updater = new CSSOverridesUpdater(config);

    this.pluginsDir = tmp.newFolder(Config.DIR_PLUGINS);
    this.staticDir = tmp.newFolder(Config.DIR_STATIC);
    this.overridesCss = new File(staticDir, "overrides.css");
  }

  @Test
  public void updatesOverridesCss() throws IOException {
    new File(pluginsDir, "test.css").createNewFile();

    updater.run();

    String contents = IOUtils.toString(new FileReader(overridesCss));
    assertEquals("@import url(\"../plugins/test.css\");", contents.trim());
  }

  @Test
  public void unreadableOverridesCss() throws IOException {
    String contents = "body{ color : white; }";
    FileWriter output = new FileWriter(overridesCss);
    IOUtils.write(contents, output);
    output.close();
    overridesCss.setWritable(false);

    updater.run();

    assertEquals(contents, IOUtils.toString(new FileReader(overridesCss)));
  }
}
