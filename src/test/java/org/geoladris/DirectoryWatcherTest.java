package org.geoladris;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DirectoryWatcherTest {
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private int nActions;

  @Before
  public void setup() throws IOException {
    this.nActions = 0;
  }

  @Test
  public void invalidDir() {
    try {
      // Check that it doesn't throw an exception, just ignores the dir
      new DirectoryWatcher(new TestAction(), new File("invalid_directory"));
    } catch (IOException e) {
      fail();
    }
  }

  @Test
  public void callsActionOnAddition() throws Exception {
    runUpdater();
    assertEquals(0, nActions);

    tmp.newFolder("plugin");
    // Wait a bit until the updater does its job
    Thread.sleep(100);

    assertEquals(1, nActions);
  }

  @Test
  public void updatesPluginsOnDeletion() throws Exception {
    File plugin = tmp.newFolder("plugin");

    runUpdater();
    assertEquals(0, nActions);

    plugin.delete();
    // Wait a bit until the updater does its job
    Thread.sleep(100);

    assertEquals(1, nActions);
  }

  @Test
  public void doesNotUpdateTwiceForAMovedFile() throws Exception {
    File plugin = tmp.newFolder("plugin");

    runUpdater();
    assertEquals(0, nActions);

    FileUtils.moveDirectory(plugin, new File(tmp.getRoot(), "another_plugin"));
    // Wait a bit until the updater does its job
    Thread.sleep(100);

    assertEquals(1, nActions);
  }

  @Test
  public void updatesWhenSubdirectoryChanges() throws Exception {
    runUpdater();
    assertEquals(0, nActions);

    File plugin = tmp.newFolder("plugin");
    // Wait more than the updater threshold
    Thread.sleep(300);
    assertEquals(1, nActions);

    new File(plugin, "modules").mkdir();

    // Wait a bit until the updater does its job
    Thread.sleep(100);
    assertEquals(2, nActions);
  }

  private void runUpdater() throws IOException {
    DirectoryWatcher watcher = new DirectoryWatcher(new TestAction(), tmp.getRoot());
    new Thread(watcher).start();
  }

  private class TestAction implements Runnable {
    @Override
    public void run() {
      nActions++;
    }
  }
}
