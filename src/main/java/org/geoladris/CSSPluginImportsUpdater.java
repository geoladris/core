package org.geoladris;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;
import org.geoladris.config.Config;

public class CSSPluginImportsUpdater implements Runnable {
  private static final Logger logger = Logger.getLogger(CSSPluginImportsUpdater.class);

  static final String FILE = "plugin_imports.css";

  private Config config;

  public CSSPluginImportsUpdater(Config config) {
    this.config = config;
  }

  @Override
  public void run() {
    File staticDir = new File(this.config.getDir(), Config.DIR_STATIC);
    File pluginsDir = new File(this.config.getDir(), Config.DIR_PLUGINS);

    String content = getCssFromDir(pluginsDir, "../" + Geoladris.PATH_PLUGINS_FROM_CONFIG + "/");

    try {
      FileWriter writer = new FileWriter(new File(staticDir, FILE));
      IOUtils.write(content, writer);
      writer.close();
    } catch (IOException e) {
      logger.error("Cannot update " + FILE, e);
    }
  }

  private String getCssFromDir(File dir, String prefix) {
    String content = "";
    final File staticDir = new File(this.config.getDir(), Config.DIR_STATIC);
    Collection<File> cssFiles = FileUtils.listFiles(dir, new AbstractFileFilter() {
      @Override
      public boolean accept(File dir, String name) {
        String lower = name.toLowerCase();
        return !dir.getPath().contains("node_modules") && lower.endsWith(".css")
            && (!lower.equals(FILE) || !dir.equals(staticDir));
      }
    }, TrueFileFilter.TRUE);

    for (File css : cssFiles) {
      String relative = dir.toURI().relativize(css.toURI()).getPath();
      content += "@import url(\"" + prefix + relative + "\");\n";
    }

    return content;
  }
}
