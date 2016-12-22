package org.geoladris;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.geoladris.config.Config;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.verification.VerificationMode;

public class PluginUpdaterTest {
  private JEEContextAnalyzer analyzer;
  private Config config;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Before
  public void setup() throws IOException {
    this.analyzer = mock(JEEContextAnalyzer.class);
    this.config = mock(Config.class);
  }

  @Test
  public void invalidDir() {
    try {
      // Check that it doesn't throw an exception, just ignores the dir
      new PluginUpdater(this.analyzer, this.config, new File("invalid_directory"));
    } catch (IOException e) {
      fail();
    }
  }

  @Test
  public void updatesPluginsOnAddition() throws Exception {
    mockPlugins();
    runUpdater();
    checkUpdate(never());

    tmp.newFolder("plugin");
    // Wait a bit until the updater does its job
    Thread.sleep(100);

    checkUpdate(times(1));
  }

  @Test
  public void updatesPluginsOnDeletion() throws Exception {
    File plugin = tmp.newFolder("plugin");

    mockPlugins();
    runUpdater();
    checkUpdate(never());

    plugin.delete();
    // Wait a bit until the updater does its job
    Thread.sleep(100);

    checkUpdate(times(1));
  }

  @Test
  public void doesNotUpdateTwiceForAMovedFile() throws Exception {
    File plugin = tmp.newFolder("plugin");

    mockPlugins();
    runUpdater();
    checkUpdate(never());

    FileUtils.moveDirectory(plugin, new File(tmp.getRoot(), "another_plugin"));
    // Wait a bit until the updater does its job
    Thread.sleep(100);

    checkUpdate(times(1));
  }

  @Test
  public void updatesWhenSubdirectoryChanges() throws Exception {
    mockPlugins();
    runUpdater();
    checkUpdate(never());

    File plugin = tmp.newFolder("plugin");
    // Wait more than the updater threshold
    Thread.sleep(300);
    checkUpdate(times(1));

    new File(plugin, "modules").mkdir();

    // Wait a bit until the updater does its job
    Thread.sleep(100);
    checkUpdate(times(2));
  }

  private void mockPlugins() {
    Set<PluginDescriptor> plugins = new HashSet<>();
    plugins.add(new PluginDescriptor("p1", true));
    plugins.add(new PluginDescriptor("p2", true));
    when(this.analyzer.getPluginDescriptors()).thenReturn(plugins);
  }

  private void runUpdater() throws IOException {
    PluginUpdater updater = new PluginUpdater(this.analyzer, this.config, tmp.getRoot());
    new Thread(updater).start();
  }

  private void checkUpdate(VerificationMode mode) {
    verify(this.analyzer, mode).reload();
    verify(this.config, mode).updatePlugins(this.analyzer.getPluginDescriptors());
  }
}
