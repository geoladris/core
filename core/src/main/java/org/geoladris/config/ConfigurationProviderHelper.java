package org.geoladris.config;

import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;
import de.csgis.commons.JSONContentProvider;

/**
 * <p>
 * Helper for {@link ModuleConfigurationProvider} classes.
 * </p>
 * 
 * @author victorzinho
 */
public class ConfigurationProviderHelper {

  private JSONContentProvider contents;

  public ConfigurationProviderHelper(JSONContentProvider provider) {
    this.contents = provider;
  }

  /**
   * Get the plugin configurations from the requested <code>.json</code> file. <code>.json </code>
   * files have the following format:
   * 
   * <ul>
   * <li>Always contains a valid JSON object.
   * <li>The keys of the JSON objects match plugin names.
   * <li>The value for each key/plugin is another JSON object whose keys are module names and values
   * are module configurations.
   * </ul>
   * 
   * 
   * @param file The file to obtain the configuration. Contents are provided by the
   *        {@link JSONContentProvider} used in {@link #PublicConfHelper(JSONContentProvider, Map)}.
   * @return A map with the plugin configuration. Keys are plugin names; values are JSON objects
   *         with module configurations (JSON object keys are module names; JSON object values are
   *         module configurations).
   * @throws NullPointerException if this instance has been created with a constructor that does not
   *         receive a {@link JSONContentProvider}, such as
   *         {@link #ConfigurationProviderHelper(JSONContentProvider, Map)} .
   */
  public Map<String, JSONObject> getPluginConfig(String file) throws NullPointerException {
    JSONObject conf = this.contents.get().get(file);
    if (conf == null) {
      return null;
    }

    Map<String, JSONObject> ret = new HashMap<String, JSONObject>();
    for (Object plugin : conf.keySet()) {
      String pluginName = plugin.toString();
      ret.put(pluginName, conf.getJSONObject(pluginName));
    }

    return ret;
  }
}
