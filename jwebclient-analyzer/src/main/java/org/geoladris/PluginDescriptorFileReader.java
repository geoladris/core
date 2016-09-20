package org.geoladris;

import java.util.HashMap;
import java.util.Map;

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

	private HashMap<String, String> requireJSPathsMap = new HashMap<>();
	private HashMap<String, String> requireJSShims = new HashMap<>();
	private boolean installInRoot;
	private String pluginName;
	private JSONObject configuration;

	public PluginDescriptorFileReader(String content,
			boolean defaultInstallInRoot, String pluginName) {
		JSONObject jsonRoot = (JSONObject) JSONSerializer.toJSON(content);

		this.installInRoot = jsonRoot.has(PROP_INSTALL_IN_ROOT) ? jsonRoot
				.getBoolean(PROP_INSTALL_IN_ROOT) : defaultInstallInRoot;
		this.pluginName = pluginName;

		if (jsonRoot.has(PROP_REQUIREJS)) {
			JSONObject requireJS = jsonRoot.getJSONObject(PROP_REQUIREJS);
			if (requireJS != null) {
				requireJSPathsMap = new HashMap<String, String>();
				requireJSShims = new HashMap<String, String>();
				fill(requireJSPathsMap, (JSONObject) requireJS.get("paths"));
				fill(requireJSShims, (JSONObject) requireJS.get("shim"));
			}
		}
		if (jsonRoot.has(PROP_DEFAULT_CONF)) {
			configuration = jsonRoot.getJSONObject(PROP_DEFAULT_CONF);
		}
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
		return this.installInRoot ? jsLibPath : jsLibPath.replace("jslib/",
				"jslib/" + this.pluginName + "/");
	}

	private JSONObject getConfiguration() {
		return configuration;
	}

	private HashMap<String, String> getRequireJSPathsMap() {
		return requireJSPathsMap;
	}

	private HashMap<String, String> getRequireJSShims() {
		return requireJSShims;
	}

	private boolean isInstallInRoot() {
		return installInRoot;
	}

	public void fillPluginDescriptor(PluginDescriptor plugin) {
		plugin.setInstallInRoot(isInstallInRoot());
		plugin.mergeConfiguration(getConfiguration());
		plugin.mergeRequireJSPaths(getRequireJSPathsMap());
		plugin.mergeRequireJSShims(getRequireJSShims());
		plugin.setName(pluginName);
	}
}
