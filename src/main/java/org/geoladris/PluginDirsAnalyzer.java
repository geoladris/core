package org.geoladris;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

public class PluginDirsAnalyzer {
  private static Logger logger = Logger.getLogger(PluginDirsAnalyzer.class);

  public static final String MODULES = "src";
  public static final String CONF_FILE = "geoladris.json";

  private Set<PluginDescriptor> pluginDescriptors;
  private File[] pluginsDirs;

  public PluginDirsAnalyzer(File... pluginsDirs) {
    this.pluginsDirs = pluginsDirs;
    reload();
  }

  public void reload() {
    this.pluginDescriptors = new HashSet<>();
    for (File pluginsDir : this.pluginsDirs) {
      scanPluginsDir(pluginsDir);
    }
  }

  private void scanPluginsDir(File pluginsDir) {
    if (!pluginsDir.isDirectory()) {
      return;
    }

    File[] plugins = pluginsDir.listFiles();
    if (plugins == null) {
      logger.warn(
          "Cannot read plugins from directory: " + pluginsDir.getAbsolutePath() + ". Ignoring");
      return;
    }

    for (File pluginDir : plugins) {
      if (!pluginDir.isDirectory()) {
        continue;
      }

      String name = pluginDir.getName();
      File conf = new File(pluginDir, CONF_FILE);
      if (!conf.exists()) {
        conf = new File(pluginDir, name + "-conf.json");
      }

      PluginDescriptor plugin;
      try {
        plugin = new PluginDescriptor(name, conf);
      } catch (IOException e) {
        plugin = new PluginDescriptor(name, false);
      }
      this.pluginDescriptors.add(plugin);

      File modulesDir = new File(pluginDir, MODULES);
      if (modulesDir.isDirectory()) {
        Collection<File> modules = FileUtils.listFiles(modulesDir, new AbstractFileFilter() {
          @Override
          public boolean accept(File file, String name) {
            return name.toLowerCase().endsWith(".js");
          }
        }, TrueFileFilter.INSTANCE);
        int rootLength = modulesDir.getAbsolutePath().length() + 1;
        for (File module : modules) {
          String path = module.getAbsolutePath();
          path = path.substring(rootLength, path.length() - 3);
          plugin.addModule(path);
        }
      }
    }
  }

  public Set<PluginDescriptor> getPluginDescriptors() {
    return pluginDescriptors;
  }
}
