package org.fao.unredd.jwebclientAnalyzer;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class PluginDescriptor {
	public static final String PROP_DEFAULT_CONF = "default-conf";
	public static final String PROP_REQUIREJS = "requirejs";
	public static final String PROP_INSTALL_IN_ROOT = "installInRoot";

	private JSONObject requireJS;
	private JSONObject configuration;
	private boolean installInRoot;
	private Set<String> modules, stylesheets;
	private String name;

	/**
	 * Creates a new plugin descriptor.
	 * 
	 * @param installInRoot
	 *            Determines whether the plugin is installed in root by default
	 *            or not. It can be overriden with
	 *            {@link #setConfiguration(String)}.
	 */
	public PluginDescriptor(boolean installInRoot) {
		this.modules = new HashSet<>();
		this.stylesheets = new HashSet<>();
		this.installInRoot = installInRoot;
	}

	/**
	 * Sets the plugin configuration. This method cannot be called after adding
	 * modules ({@link #addModule(String)}) or stylesheets
	 * ({@link #addStylesheet(String)}).
	 * 
	 * @param content
	 *            The JSON configuration for the plugin.
	 */
	public void setConfiguration(String content) {
		if (this.modules.size() > 0 || this.stylesheets.size() > 0) {
			throw new IllegalStateException(
					"Cannot configure plugin after modules and/or "
							+ "stylesheets have been added.");
		}

		JSONObject jsonRoot = (JSONObject) JSONSerializer.toJSON(content);

		if (jsonRoot.has(PROP_REQUIREJS)) {
			requireJS = jsonRoot.getJSONObject(PROP_REQUIREJS);
		}
		if (jsonRoot.has(PROP_DEFAULT_CONF)) {
			configuration = jsonRoot.getJSONObject(PROP_DEFAULT_CONF);
		}
		if (jsonRoot.has(PROP_INSTALL_IN_ROOT)) {
			installInRoot = jsonRoot.getBoolean(PROP_INSTALL_IN_ROOT);
		}
	}

	/**
	 * Returns the requirejs paths map for RequireJS config.js. The values of
	 * the map already contain the plugin name.
	 * 
	 * @return
	 */
	public Map<String, String> getRequireJSPathsMap() {
		Map<String, String> ret = new HashMap<String, String>();

		if (requireJS != null) {
			fill(ret, (JSONObject) requireJS.get("paths"));
		}
		return ret;
	}

	private void fill(Map<String, String> map, JSONObject jsonMap) {
		if (jsonMap == null) {
			return;
		}

		for (Object key : jsonMap.keySet()) {
			Object value = jsonMap.get(key.toString());
			map.put(key.toString(), buildJSLibURL(value.toString()));
		}
	}

	private String buildJSLibURL(String jsLibPath) {
		return this.installInRoot
				? jsLibPath
				: jsLibPath.replace("jslib/", "jslib/" + this.name + "/");
	}

	/**
	 * Returns the requirejs shim map for RequireJS config.js. The values of the
	 * map already contain the plugin name.
	 * 
	 * @return
	 */
	public Map<String, String> getRequireJSShims() {
		Map<String, String> ret = new HashMap<String, String>();

		if (requireJS != null) {
			fill(ret, (JSONObject) requireJS.get("shim"));
		}
		return ret;
	}

	public JSONObject getDefaultConf() {
		return configuration;
	}

	/**
	 * Get the modules. It returns a new copied set each time; if you want to
	 * add a new module use {@link #addModule(String)}.
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
	 * <p>
	 * <b>IMPORTANT</b>: {@link #setConfiguration(String)} cannot be called
	 * after modules and/or stylesheets have been added.
	 * </p>
	 * 
	 * @param module
	 *            The module to add to the plugin. It is simply the path for the
	 *            JS within the plugin. This class takes care of qualifying the
	 *            path with the plugin name.
	 */
	public void addModule(String module) {
		if (!installInRoot && this.name != null) {
			module = this.name + "/" + module;
		}
		this.modules.add(module);
	}

	/**
	 * Get the stylesheets. It returns a new copied set each time; if you want to
	 * add a new stylesheet use {@link #addStylesheet(String)}.
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
	 * <p>
	 * <b>IMPORTANT</b>: {@link #setConfiguration(String)} cannot be called
	 * after modules and/or stylesheets have been added.
	 * </p>
	 * 
	 * @param stylesheet
	 *            The stylesheet to add to the plugin. It is simply the path for
	 *            the CSS within the plugin. This class takes care of qualifying
	 *            the path with the plugin name.
	 */
	public void addStylesheet(String stylesheet) {
		if (!installInRoot && this.name != null) {
			String dir = new File(stylesheet).getParentFile().getName();
			stylesheet = stylesheet.replace(dir + "/",
					dir + "/" + this.name + "/");
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
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof PluginDescriptor)) {
			return false;
		}

		PluginDescriptor p = (PluginDescriptor) obj;

		boolean equals = this.name != null
				? this.name.equals(p.name)
				: p.name == null;
		equals &= this.configuration != null
				? this.configuration.equals(p.configuration)
				: p.configuration == null;
		equals &= this.requireJS != null
				? this.requireJS.equals(p.requireJS)
				: p.requireJS == null;
		return equals && p.modules.equals(this.modules)
				&& p.stylesheets.equals(this.stylesheets);
	}

	@Override
	public int hashCode() {
		int hash = name != null ? name.hashCode() : 0;
		hash += configuration != null ? configuration.hashCode() : 0;
		hash += requireJS != null ? requireJS.hashCode() : 0;
		return hash + modules.hashCode() + stylesheets.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}
}