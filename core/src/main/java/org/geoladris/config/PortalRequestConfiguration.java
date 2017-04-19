package org.geoladris.config;

import java.io.File;
import java.util.Map;

import net.sf.json.JSONObject;

/**
 * Instance that provides services using the current request and portal configuration
 * 
 * @author fergonco
 */
public interface PortalRequestConfiguration {

  String localize(String template);

  File getConfigDir();

  /**
   * Gets the configuration that has been created so far by the previous
   * {@link ModuleConfigurationProvider} for the request.
   * 
   * 
   * @return The configuration following the same rules as the return value of
   *         {@link ModuleConfigurationProvider#getPluginConfig(PortalRequestConfiguration, javax.servlet.http.HttpServletRequest)}.
   */
  Map<String, JSONObject> getCurrentConfiguration();
}
