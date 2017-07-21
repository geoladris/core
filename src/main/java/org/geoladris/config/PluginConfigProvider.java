package org.geoladris.config;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

public interface PluginConfigProvider {
  /**
   * Returns a map where the keys are the configured plugin names and the JSONObjects are the
   * configuration for the modules contained in the plugin.
   * 
   * @param requestConfig
   * @param request Request that loads the application
   * 
   * @return
   * @throws IOException
   */
  Map<String, JSONObject> getPluginConfig(Config config, Map<String, JSONObject> currentConfig,
      HttpServletRequest request) throws IOException;

  /**
   * True if the value returned by
   * {@link #getPluginConfig(PortalRequestConfiguration, HttpServletRequest)} can be cached so that
   * the method is not called in every request.
   * 
   * @return
   */
  boolean canBeCached();
}
