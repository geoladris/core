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

  Map<String, JSONObject> getCurrentConfiguration();
}
