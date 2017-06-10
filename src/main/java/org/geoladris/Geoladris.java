package org.geoladris;

import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.geoladris.config.Config;

public interface Geoladris {
  // Servlet context attributes
  /**
   * {@link Config}. Obtain with {@link ServletContext#getAttribute(String)}.
   */
  String ATTR_CONFIG = "org.geoladris.config";

  /**
   * {@link Locale}. Obtain with {@link HttpServletRequest#getAttribute(String)}.
   */
  String ATTR_LOCALE = "org.geoladris.request.locale";

  // Session attributes
  /**
   * String. Obtain with {@link HttpSession#getAttribute(String)}.
   */
  String ATTR_ROLE = "org.geoladris.session.role";

  String PATH_PLUGINS_FROM_CONFIG = "plugins";
  String PATH_PLUGINS_FROM_WAR = "geoladris";
  String PATH_STATIC = "static";
}
