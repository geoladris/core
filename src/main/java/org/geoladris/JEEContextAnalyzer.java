package org.geoladris;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

public class JEEContextAnalyzer {
  private static Logger logger = Logger.getLogger(JEEContextAnalyzer.class);

  public static final String CONF_FILE = "geoladris.json";

  private Set<PluginDescriptor> pluginDescriptors;
  private PluginDescriptorFileReader reader;
  private JEEContext context;

  public JEEContextAnalyzer(JEEContext context) {
    this.context = context;
    this.reader = new PluginDescriptorFileReader();

    reload();
  }

  public void reload() {
    this.pluginDescriptors = new HashSet<>();

    File jarsDir = unzipJars(context);
    scanPluginsDir(jarsDir);
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
    File conf = new File(pluginDir, CONF_FILE);
    if (!conf.exists()) {
      conf = new File(pluginDir, name + "-conf.json");
    }
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

  private File unzipJars(JEEContext context) {
    File dir;
    try {
      dir = File.createTempFile("geoladris", "");
      dir.delete();
      dir.mkdirs();
    } catch (IOException e) {
      throw new RuntimeException("Cannot process plugins from jar files", e);
    }

    Set<String> jars = context.getLibPaths();
    for (String jar : jars) {
      String pluginName = new File(jar).getName().replace(".jar", "");
      File jarDir = new File(dir, pluginName);
      ZipInputStream zis = new ZipInputStream(context.getLibAsStream(jar));
      try {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
          String name = entry.getName();
          File file = new File(jarDir, name);
          if (name.endsWith("/")) {
            file.mkdirs();
            continue;
          }

          File parent = file.getParentFile();
          if (parent != null) {
            parent.mkdirs();
          }

          FileOutputStream fos = new FileOutputStream(file);
          IOUtils.copy(zis, fos);
          fos.close();
        }
      } catch (IOException e) {
        logger.warn("Error reading plugin from jar file: " + jar + ". Ignoring", e);
      } finally {
        try {
          zis.close();
        } catch (IOException e) {
        }
      }
    }

    return dir;
  }
}
