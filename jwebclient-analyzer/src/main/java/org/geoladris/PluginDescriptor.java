package org.geoladris;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.csgis.commons.JSONUtils;
import net.sf.json.JSONObject;

/**
 * Contains all information about the plugin that may be necessary to make a geoladris application
 * work: modules, requirejs paths and shims, configuration, etc.
 * 
 * @author fergonco
 */
public class PluginDescriptor {
  public static final String CONF_ENABLED = "_enabled";
  public static final String CONF_OVERRIDE = "_override";

  private HashMap<String, String> requireJSPathsMap = new HashMap<String, String>();
  private HashMap<String, String> requireJSShims = new HashMap<String, String>();
  private JSONObject configuration = new JSONObject();
  private HashSet<String> modules = new HashSet<String>();
  private HashSet<String> stylesheets = new HashSet<String>();
  private String name;
  private boolean installInRoot, enabled;

  /**
   * Creates a new plugin descriptor with the given name.
   * 
   * @param name The name of the plugin. It cannot be null or empty.
   */
  public PluginDescriptor(String name, boolean installInRoot) {
    if (name == null || name.length() == 0) {
      throw new IllegalArgumentException("Plugin name cannot be null or empty");
    }
    this.name = name;
    this.installInRoot = installInRoot;
    this.enabled = true;
  }

  public String getName() {
    return name;
  }

  /**
   * Get the RequireJS paths. It returns a new copied map each time; if you want to add a new module
   * use {@link #addRequireJSPath(String, String)}.
   * 
   * @return
   */
  public Map<String, String> getRequireJSPathsMap() {
    Map<String, String> ret = new HashMap<>();
    ret.putAll(this.requireJSPathsMap);
    return ret;
  }

  public void addRequireJSPath(String key, String value) {
    addToRequireJSMap(key, value, this.requireJSPathsMap);
  }

  /**
   * Get the RequireJS shims. It returns a new copied map each time; if you want to add a new module
   * use {@link #addRequireJSShim(String, String)}.
   * 
   * @return
   */
  public Map<String, String> getRequireJSShims() {
    Map<String, String> ret = new HashMap<>();
    ret.putAll(this.requireJSShims);
    return ret;
  }

  public void addRequireJSShim(String key, String value) {
    addToRequireJSMap(key, value, this.requireJSShims);
  }

  private void addToRequireJSMap(String key, String value, Map<String, String> map) {
    if (!this.installInRoot) {
      value = value.replace("jslib/", "jslib/" + this.name + "/");
    }
    map.put(key, value);
  }

  /**
   * Get the modules. It returns a new copied set each time; if you want to add a new module use
   * {@link #addModule(String)}.
   * 
   * @return
   */
  public Set<String> getModules() {
    Set<String> ret = new HashSet<>();
    ret.addAll(this.modules);
    return ret;
  }

  /**
   * <p>
   * Adds a new module to the plugin.
   * </p>
   * 
   * @param module The module to add to the plugin. It is simply the path for the JS within the
   *        plugin. This class takes care of qualifying the path with the plugin name.
   */
  public void addModule(String module) {
    if (!this.installInRoot) {
      module = this.name + "/" + module;
    }
    this.modules.add(module);
  }

  /**
   * Get the stylesheets. It returns a new copied set each time; if you want to add a new stylesheet
   * use {@link #addStylesheet(String)}.
   * 
   * @return
   */
  public Set<String> getStylesheets() {
    Set<String> ret = new HashSet<>();
    ret.addAll(this.stylesheets);
    return ret;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  /**
   * <p>
   * Adds a new stylesheet to the plugin.
   * </p>
   * 
   * @param stylesheet The stylesheet to add to the plugin. It is simply the path for the CSS within
   *        the plugin. This class takes care of qualifying the path with the plugin name.
   */
  public void addStylesheet(String stylesheet) {
    if (!this.installInRoot) {
      // Get root (styles, modules or theme)
      File root = new File(stylesheet).getParentFile();
      while (root.getParentFile() != null) {
        root = root.getParentFile();
      }

      stylesheet = stylesheet.replace(root.getName(), root.getName() + "/" + this.name);
    }
    this.stylesheets.add(stylesheet);
  }

  public JSONObject getConfiguration() {
    return configuration;
  }

  public void setConfiguration(JSONObject configuration) {
    boolean override = configuration.optBoolean(CONF_OVERRIDE, false);
    this.enabled = configuration.optBoolean(CONF_ENABLED, this.enabled);

    JSONObject newConfig = JSONObject.fromObject(configuration.toString());
    newConfig.remove(CONF_ENABLED);
    newConfig.remove(CONF_OVERRIDE);

    if (!this.installInRoot) {
      // prefix all keys with plugin name
      JSONObject qualified = new JSONObject();
      for (Object moduleName : newConfig.keySet()) {
        qualified.put(this.name + "/" + moduleName, newConfig.get(moduleName));
      }

      newConfig = qualified;
    }

    this.configuration = override ? newConfig : JSONUtils.merge(this.configuration, newConfig);
  }

  @SuppressWarnings("unchecked")
  public PluginDescriptor cloneDescriptor() {
    PluginDescriptor ret = new PluginDescriptor(this.name, this.installInRoot);
    ret.configuration = JSONObject.fromObject(this.configuration);
    ret.modules = (HashSet<String>) this.modules.clone();
    ret.stylesheets = (HashSet<String>) this.stylesheets.clone();
    ret.requireJSPathsMap = (HashMap<String, String>) this.requireJSPathsMap.clone();
    ret.requireJSShims = (HashMap<String, String>) this.requireJSShims.clone();
    return ret;
  }

  public boolean isInstallInRoot() {
    return installInRoot;
  }

  @Override
  public String toString() {
    return name;
  }
}
