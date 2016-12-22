package org.geoladris;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.log4j.Logger;
import org.geoladris.config.Config;

public class PluginUpdater implements Runnable {
  private static final Logger logger = Logger.getLogger(PluginUpdater.class);

  private WatchService watcher;
  private SimpleFileVisitor<Path> registerDir;

  private JEEContextAnalyzer analyzer;
  private Config config;

  public PluginUpdater(JEEContextAnalyzer analyzer, Config config, File... dirs)
      throws IOException {
    this.analyzer = analyzer;
    this.config = config;
    this.watcher = FileSystems.getDefault().newWatchService();
    this.registerDir = new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
        return FileVisitResult.CONTINUE;
      }
    };

    for (File dir : dirs) {
      if (dir.exists() && dir.isDirectory()) {
        Files.walkFileTree(Paths.get(dir.toURI()), this.registerDir);
      }
    }
  }

  @Override
  public void run() {
    long lastUpdate = -1;
    while (true) {
      // wait for key to be signalled
      WatchKey key;
      try {
        key = this.watcher.take();
      } catch (InterruptedException x) {
        return;
      }

      // Do not update too often; at least 5 seconds between updates
      if (lastUpdate + 100 > System.currentTimeMillis()) {
        continue;
      }

      Path dir;
      try {
        dir = (Path) key.watchable();
      } catch (ClassCastException e) {
        logger.error("bug! Watching something that is not a Path?");
        continue;
      }

      logger.debug(
          "Updating plugin descriptors because of a change in " + dir.toFile().getAbsolutePath());
      this.analyzer.reload();
      this.config.updatePlugins(this.analyzer.getPluginDescriptors());
      lastUpdate = System.currentTimeMillis();

      logger.debug("Checking for directory creation");
      for (WatchEvent<?> event : key.pollEvents()) {
        if (!event.kind().equals(ENTRY_CREATE)) {
          continue;
        }

        @SuppressWarnings("unchecked")
        Path child = dir.resolve(((WatchEvent<Path>) event).context());
        try {
          if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
            logger.debug("Watching " + child.toFile().getAbsolutePath());
            Files.walkFileTree(child, this.registerDir);
          }
        } catch (IOException e) {
          logger.warn("Cannot walk new directory: " + child.toFile().getAbsolutePath(), e);
        }
      }

      key.reset();
    }
  }
}
