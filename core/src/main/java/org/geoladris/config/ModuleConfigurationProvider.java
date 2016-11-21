package org.geoladris.config;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

public interface ModuleConfigurationProvider {
  /**
   * Returns a map where the keys are the configured plugins and the JSONObjects are the
   * configuration for the modules contained in the plugin.
   * 
   * @param config
   * @param request Request that loads the application
   * 
   * @return
   * @throws IOException
   */
  Map<String, JSONObject> getPluginConfig(Config config, HttpServletRequest request)
      throws IOException;

  /**
   * True if the value returned by {@link #getPluginConfig(Config, HttpServletRequest)} can be
   * cached so that the method is not called in every request.
   * 
   * @return
   */
  boolean canBeCached();
}
