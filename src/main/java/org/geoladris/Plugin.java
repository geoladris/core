package org.geoladris;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import de.csgis.commons.JSONUtils;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Contains all information about the plugin that may be necessary to make a geoladris application
 * work: modules, requirejs paths and shims, configuration, etc.
 *
 * @author fergonco
 */
public class Plugin {
  private static final String PROP_REQUIREJS = "requirejs";
  private static final String PROP_DEFAULT_CONF = "default-conf";
  private static final String PROP_INSTALL_IN_ROOT = "installInRoot";

  public static final String CONF_ENABLED = "_enabled";
  public static final String CONF_OVERRIDE = "_override";

  private JSONObject configuration = new JSONObject();
  private HashSet<String> modules = new HashSet<String>();
  private String name;
  private boolean installInRoot, enabled;
  private final JSONObject descriptor;

  public Plugin(String name, File configFile) throws IOException {
    this(name, (JSONObject) JSONSerializer.toJSON(IOUtils.toString(configFile.toURI())));
  }

  public Plugin(String name, JSONObject descriptor) {
    this.installInRoot = descriptor.optBoolean(PROP_INSTALL_IN_ROOT, false);
    this.name = name;
    this.enabled = true;
    this.descriptor = descriptor;
    if (descriptor.has(PROP_DEFAULT_CONF)) {
      setConfiguration(descriptor.getJSONObject(PROP_DEFAULT_CONF));
    }
  }

  /**
   * Creates a new plugin descriptor with the given name.
   *
   * @param name The name of the plugin. It cannot be null or empty.
   */
  public Plugin(String name, boolean installInRoot) {
    this(name, installInRoot, new JSONObject());
  }


  public Plugin(String name, boolean installInRoot, JSONObject descriptor) {
    if (name == null || name.length() == 0) {
      throw new IllegalArgumentException("Plugin name cannot be null or empty");
    }
    this.name = name;
    this.installInRoot = installInRoot;
    this.enabled = true;
    this.descriptor = descriptor;
  }

  public String getName() {
    return name;
  }

  /**
   * Get the modules. It returns a new copied set each time; if you want to add a new module use
   * {@link #addModule(String)}.
   *
   * @return the modules.
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

  public boolean isEnabled() {
    return this.enabled;
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

  public boolean isInstallInRoot() {
    return installInRoot;
  }

  @SuppressWarnings("unchecked")
  public Plugin clonePlugin() {
    Plugin ret = new Plugin(this.name, this.installInRoot, JSONObject.fromObject(this.descriptor));
    ret.configuration = JSONObject.fromObject(this.configuration);
    ret.modules = (HashSet<String>) this.modules.clone();
    return ret;
  }

  @Override
  public String toString() {
    return name;
  }

  public JSONObject getRequireJS() {
    return this.descriptor.getJSONObject(PROP_REQUIREJS);
  }
}
