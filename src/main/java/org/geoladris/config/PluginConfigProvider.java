package org.geoladris.config;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

public interface PluginConfigProvider {
  /**
   * @param config
   * @param currentConfig
   * @param request Request that loads the application
   *
   * @return a map where the keys are the configured plugin names and the JSONObjects are the
   *         configuration for the modules contained in the plugin.
   * @throws IOException
   */
  Map<String, JSONObject> getPluginConfig(Config config, Map<String, JSONObject> currentConfig,
      HttpServletRequest request) throws IOException;

  /**
   * @return <code>true</code> if the value returned by
   *         {@link #getPluginConfig(Config config, Map currentConfig, HttpServletRequest request)}
   *         can be cached so that the method is not called in every request.
   */
  boolean canBeCached();
}
