package org.geoladris;

import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.geoladris.config.Config;
import org.geoladris.config.ModuleConfigurationProvider;

public interface Geoladris {
  // Servlet context attributes
  /**
   * {@link List}&lt;{@link ModuleConfigurationProvider}&gt;. Obtain with
   * {@link ServletContext#getAttribute(String)}.
   */
  String ATTR_CONFIG_PROVIDERS = "org.geoladris.request.config_providers";

  // HTTP request attributes
  /**
   * {@link Config}. Obtain with {@link HttpServletRequest#getAttribute(String)}.
   */
  String ATTR_CONFIG = "org.geoladris.request.config";
  /**
   * {@link String}. Obtain with {@link HttpServletRequest#getAttribute(String)}.
   */
  String ATTR_APP = "org.geoladris.request.app";
  /**
   * {@link Locale}. Obtain with {@link HttpServletRequest#getAttribute(String)}.
   */
  String ATTR_LOCALE = "org.geoladris.request.locale";

  // Session attributes
  /**
   * String. Obtain with {@link HttpSession#getAttribute(String)}.
   */
  String ATTR_ROLE = "org.geoladris.session.role";
}
