package org.fao.unredd.jwebclientAnalyzer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class PluginDescriptor {

	private JSONObject requireJS;
	private JSONObject configuration;
	private Boolean installInRoot = null;

	public PluginDescriptor(String content) {
		JSONObject jsonRoot = (JSONObject) JSONSerializer.toJSON(content);

		if (jsonRoot.has("requirejs")) {
			requireJS = jsonRoot.getJSONObject("requirejs");
		}
		if (jsonRoot.has("default-conf")) {
			configuration = jsonRoot.getJSONObject("default-conf");
		}
		if (jsonRoot.has("installInRoot")) {
			installInRoot = jsonRoot.getBoolean("installInRoot");
		}
	}

	public Map<String, String> getRequireJSPathsMap(String pluginName) {
		Map<String, String> ret = new HashMap<String, String>();

		if (requireJS != null) {
			fill(ret, pluginName, (JSONObject) requireJS.get("paths"));
		}
		return ret;
	}

	private void fill(Map<String, String> map, String pluginName,
			JSONObject jsonMap) {
		if (jsonMap == null) {
			return;
		}

		for (Object key : jsonMap.keySet()) {
			Object value = jsonMap.get(key.toString());
			map.put(key.toString(), buildJSLibURL(pluginName, value.toString()));
		}
	}

	private String buildJSLibURL(String pluginName, String jsLibPath) {
		if (pluginName == null) {
			return jsLibPath;
		} else {
			return jsLibPath.replace("jslib/", "jslib/" + pluginName + "/");
		}
	}

	public Map<String, String> getRequireJSShims(String pluginName) {
		Map<String, String> ret = new HashMap<String, String>();

		if (requireJS != null) {
			fill(ret, pluginName, (JSONObject) requireJS.get("shim"));
		}
		return ret;
	}

	public Map<String, JSONObject> getConfigurationMap() {
		Map<String, JSONObject> configurationMap = new HashMap<String, JSONObject>();
		if (configuration != null) {
			@SuppressWarnings("rawtypes")
			Iterator iterator = configuration.keys();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				configurationMap.put(key, configuration.getJSONObject(key));
			}
		}
		return configurationMap;
	}

	public Boolean isInstallInRoot() {
		return installInRoot;
	}
}
