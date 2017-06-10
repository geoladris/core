package org.geoladris;

import java.io.IOException;

import org.geoladris.config.Config;

public class PluginUpdater implements Runnable {
  private PluginDirsAnalyzer analyzer;
  private Config config;

  public PluginUpdater(PluginDirsAnalyzer analyzer, Config config) throws IOException {
    this.analyzer = analyzer;
    this.config = config;
  }

  @Override
  public void run() {
    this.analyzer.reload();
    this.config.setPlugins(this.analyzer.getPluginDescriptors());
  }
}
