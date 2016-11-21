package org.geoladris;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Reads the contents of the plugin descriptor file
 * 
 * @author fergonco
 */
public class PluginDescriptorFileReader {
  private static final String PROP_DEFAULT_CONF = "default-conf";
  private static final String PROP_REQUIREJS = "requirejs";
  private static final String PROP_INSTALL_IN_ROOT = "installInRoot";

  private PluginDescriptor plugin;

  public PluginDescriptorFileReader() {}

  /**
   * Obtains a {@link PluginDescriptor} with the specified name by parsing the given content.
   * 
   * @param content The contents of the &lt;plugin&gt;-conf.json descriptor.
   * @param pluginName The name of the plugin. It cannot be null or empty.
   * @return The plugin descriptor.
   */
  public PluginDescriptor read(String content, String pluginName) {
    JSONObject jsonRoot = (JSONObject) JSONSerializer.toJSON(content);

    boolean installInRoot = jsonRoot.optBoolean(PROP_INSTALL_IN_ROOT, false);
    this.plugin = new PluginDescriptor(pluginName, installInRoot);

    if (jsonRoot.has(PROP_REQUIREJS)) {
      JSONObject requireJS = jsonRoot.getJSONObject(PROP_REQUIREJS);
      JSONObject paths = requireJS.getJSONObject("paths");
      JSONObject shim = requireJS.getJSONObject("shim");
      // if (requireJS != null) {

      if (paths != null) {
        for (Object keyObj : paths.keySet()) {
          String key = keyObj.toString();
          this.plugin.addRequireJSPath(key, paths.get(key).toString());
        }
      }

      if (shim != null) {
        for (Object keyObj : shim.keySet()) {
          String key = keyObj.toString();
          this.plugin.addRequireJSShim(key, shim.get(key).toString());
        }
      }
    }
    // }

    if (jsonRoot.has(PROP_DEFAULT_CONF)) {
      plugin.setConfiguration(jsonRoot.getJSONObject(PROP_DEFAULT_CONF));
    }

    return plugin;
  }
}
