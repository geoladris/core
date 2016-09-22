package org.geoladris;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;
import de.csgis.commons.JSONUtils;

/**
 * Contains all information about the plugin that may be necessary to make a geoladris application
 * work: modules, requirejs paths and shims, configuration, etc.
 * 
 * @author fergonco
 */
public class PluginDescriptor {
  private HashMap<String, String> requireJSPathsMap = new HashMap<String, String>();
  private HashMap<String, String> requireJSShims = new HashMap<String, String>();
  private JSONObject configuration = new JSONObject();
  private HashSet<String> modules = new HashSet<String>();
  private HashSet<String> stylesheets = new HashSet<String>();
  private String name;
  private boolean installInRoot;

  public void setInstallInRoot(boolean installInRoot) {
    this.installInRoot = installInRoot;
  }

  /**
   * Returns the requireJS maps
   * 
   * @return
   */
  public Map<String, String> getRequireJSPathsMap() {
    return requireJSPathsMap;
  }

  /**
   * Returns the requirejs shim map for RequireJS config.js.
   * 
   * @return
   */
  public Map<String, String> getRequireJSShims() {
    return requireJSShims;
  }

  public JSONObject getConfiguration() {
    return configuration;
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
    if (!installInRoot && this.name != null) {
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

  /**
   * <p>
   * Adds a new stylesheet to the plugin.
   * </p>
   * 
   * @param stylesheet The stylesheet to add to the plugin. It is simply the path for the CSS within
   *        the plugin. This class takes care of qualifying the path with the plugin name.
   */
  public void addStylesheet(String stylesheet) {
    if (!installInRoot && this.name != null) {
      String dir = new File(stylesheet).getParentFile().getName();
      stylesheet = stylesheet.replace(dir + "/", dir + "/" + this.name + "/");
    }
    this.stylesheets.add(stylesheet);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public void mergeConfiguration(JSONObject configuration) {
    this.configuration = JSONUtils.merge(this.configuration, configuration);
  }

  public void mergeRequireJSPaths(Map<String, String> requireJSPathsMap) {
    this.requireJSPathsMap.putAll(requireJSPathsMap);
  }

  public void mergeRequireJSShims(Map<String, String> requireJSShims) {
    this.requireJSShims.putAll(requireJSShims);
  }

  public void setConfiguration(JSONObject configuration) {
    this.configuration = configuration;
  }

  @SuppressWarnings("unchecked")
  public PluginDescriptor cloneDescriptor() {
    PluginDescriptor ret = new PluginDescriptor();
    ret.configuration = JSONObject.fromObject(this.configuration);
    ret.installInRoot = this.installInRoot;
    ret.modules = (HashSet<String>) this.modules.clone();
    ret.stylesheets = (HashSet<String>) this.stylesheets.clone();
    ret.name = this.name;
    ret.requireJSPathsMap = (HashMap<String, String>) this.requireJSPathsMap.clone();
    ret.requireJSShims = (HashMap<String, String>) this.requireJSShims.clone();
    return ret;
  }

  public boolean isInstallInRoot() {
    return installInRoot;
  }
}
