package org.geoladris.config;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.geoladris.Plugin;

public interface Config {
  String PROPERTY_CLIENT_MODULES = "client.modules";
  String PROPERTY_MAP_CENTER = "map.centerLonLat";
  String PROPERTY_LANGUAGES = "languages";

  String DIR_STATIC = "static";
  String DIR_PLUGINS = "plugins";

  File getDir();

  Properties getProperties();

  /**
   * Returns an array of <code>Map&lt;String, String&gt;</code>. For each element of the array, a
   * {@link Map} is returned containing two keys/values: <code>code</code> (for language code) and
   * <code>name</code> (for language name).
   * 
   * @return The array of languages or null if no language configuration is found
   */
  Map<String, String>[] getLanguages();

  ResourceBundle getMessages(Locale locale) throws ConfigException;

  String localize(String template);

  /**
   * @param property
   * @return the property as an array or null if the property does not exist.
   */
  String[] getPropertyAsArray(String property);

  /**
   * @return The language defined as default in the configuration or null if no language is defined
   *         in the configuration
   */
  String getDefaultLang();

  /**
   * @param locale
   * @param request
   * @return plugin configuration provided by the list of {@link PluginConfigProvider}. Only
   *         configuration for the active plugins are provided. Any disabled plugins won't be
   *         contained in the array.
   */
  Plugin[] getPluginConfig(Locale locale, HttpServletRequest request);

  /**
   * @return the folder in the configuration directory where client plugins are to be found.
   */
  File getNoJavaPluginRoot();

  void setPlugins(Set<Plugin> plugins);

  void addPluginConfigProvider(PluginConfigProvider provider);

  List<PluginConfigProvider> getPluginConfigProviders();
}
