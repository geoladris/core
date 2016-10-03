package org.geoladris;

import javax.servlet.ServletContext;

public class Environment {
  public static final String CONFIG_CACHE = "GEOLADRIS_CONFIG_CACHE";
  public static final String CONFIG_DIR = "GEOLADRIS_CONFIG_DIR";

  public String get(String propertyName) {
    String property = System.getProperty(propertyName);
    return property != null ? property : System.getenv(propertyName);
  }

  public String get(String propertyName, ServletContext context) {
    String property = context.getInitParameter(propertyName);
    return property != null ? property : get(propertyName);
  }

  public boolean getConfigCache() {
    return Boolean.parseBoolean(get(CONFIG_CACHE));
  }

  public String getConfigDir(ServletContext context) {
    return get(CONFIG_DIR, context);
  }
}
