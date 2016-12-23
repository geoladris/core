package org.geoladris;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

public class JEEContextAnalyzer {
  private static Logger logger = Logger.getLogger(JEEContextAnalyzer.class);

  public static final String CLIENT_RESOURCES_DIR = "geoladris";

  private Set<PluginDescriptor> pluginDescriptors;
  private PluginDescriptorFileReader reader;
  private Context context;

  public JEEContextAnalyzer(Context context) {
    this.context = context;
    this.reader = new PluginDescriptorFileReader();

    reload();
  }

  public void reload() {
    this.pluginDescriptors = new HashSet<>();

    File jarsDir = ZipUtils.unzipJars(context);

    scanPluginsDir(new File(jarsDir, CLIENT_RESOURCES_DIR));
    for (File dir : context.getDirs()) {
      scanPluginsDir(dir);
    }

    try {
      FileUtils.deleteDirectory(jarsDir);
    } catch (IOException e) {
      logger.warn("Cannot delete temporary directory: " + jarsDir.getAbsolutePath(), e);
    }
  }

  private void scanPluginsDir(File dir) {
    if (dir == null || !dir.isDirectory()) {
      return;
    }

    File[] plugins = dir.listFiles();
    if (plugins == null) {
      logger.warn("Cannot read plugins from directory: " + dir.getAbsolutePath() + ". Ignoring");
      return;
    }

    for (File pluginDir : plugins) {
      if (!pluginDir.isDirectory()) {
        continue;
      }

      PluginDescriptor plugin = createPluginDescriptor(pluginDir);
      fillPluginDescriptor(plugin, pluginDir);
      this.pluginDescriptors.add(plugin);
    }
  }

  private PluginDescriptor createPluginDescriptor(File pluginDir) {
    String name = pluginDir.getName();
    File conf = new File(pluginDir, name + "-conf.json");
    try {
      return reader.read(IOUtils.toString(conf.toURI()), name);
    } catch (IOException e) {
      return new PluginDescriptor(name, false);
    }
  }

  private void fillPluginDescriptor(PluginDescriptor plugin, File pluginDir) {
    Iterator<File> allFiles =
        FileUtils.iterateFiles(pluginDir, relevantExtensions, TrueFileFilter.INSTANCE);
    while (allFiles.hasNext()) {
      File file = allFiles.next();
      String path = file.getAbsolutePath();
      path = path.substring(pluginDir.getAbsolutePath().length() + 1);

      if (path.startsWith("modules")) {
        if (path.endsWith(".css")) {
          plugin.addStylesheet(path);
        } else if (path.endsWith(".js")) {
          String module = path.substring("modules/".length(), path.length() - 3);
          plugin.addModule(module);
        }
      } else if ((path.startsWith("styles") || path.startsWith("theme")) && path.endsWith(".css")) {
        plugin.addStylesheet(path);
      }
    }
  }

  public Set<PluginDescriptor> getPluginDescriptors() {
    return pluginDescriptors;
  }

  private static IOFileFilter relevantExtensions = new IOFileFilter() {
    @Override
    public boolean accept(File file, String name) {
      return true;
    }

    @Override
    public boolean accept(File file) {
      String lowerCase = file.getName().toLowerCase();
      return lowerCase.endsWith(".js") || lowerCase.endsWith(".css") || lowerCase.endsWith(".json");
    }
  };
}
