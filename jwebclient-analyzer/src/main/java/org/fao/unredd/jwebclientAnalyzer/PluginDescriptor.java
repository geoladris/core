package org.fao.unredd.jwebclientAnalyzer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * @author vicgonco
 *
 */
public class PluginDescriptor {
	public static final String PROP_MERGE_CONF = "merge-conf";
	public static final String PROP_DEFAULT_CONF = "default-conf";
	public static final String PROP_REQUIREJS = "requirejs";

	private JSONObject requireJS;
	private JSONObject configuration;
	/**
	 * @deprecated
	 */
	private boolean mergeConf;

	public PluginDescriptor(String content) {
		JSONObject jsonRoot = (JSONObject) JSONSerializer.toJSON(content);

		if (jsonRoot.has(PROP_REQUIREJS)) {
			requireJS = jsonRoot.getJSONObject(PROP_REQUIREJS);
		}
		if (jsonRoot.has(PROP_DEFAULT_CONF)) {
			configuration = jsonRoot.getJSONObject(PROP_DEFAULT_CONF);
		}

		this.mergeConf = jsonRoot.has(PROP_MERGE_CONF)
				&& jsonRoot.getBoolean(PROP_MERGE_CONF);
	}

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
			map.put(key.toString(), value.toString());
		}
	}

	public Map<String, String> getRequireJSShims() {
		Map<String, String> ret = new HashMap<String, String>();

		if (requireJS != null) {
			fill(ret, (JSONObject) requireJS.get("shim"));
		}
		return ret;
	}

	/**
	 * Determines whether the plugin configuration should be merged
	 * (<code>true</code>) or replaced (<code>false</code>) when applying other
	 * configuration from different sources.
	 * 
	 * @deprecated In the future this option will always be <code>true</code>.
	 * @return <code>true</code> if the plugin configuration should be merged,
	 *         <code>false</code> otherwise.
	 */
	public boolean getMergeConf() {
		return mergeConf;
	}

	/**
	 * @deprecated Use {@link #getConfigMap()}.
	 */
	public Map<String, JSONObject> getConfigurationMap() {
		Map<String, JSONObject> configurationMap = new HashMap<String, JSONObject>();
		fillConfigMap(configurationMap);
		return configurationMap;
	}

	public Map<String, JSON> getConfigMap() {
		Map<String, JSON> configurationMap = new HashMap<String, JSON>();
		fillConfigMap(configurationMap);
		return configurationMap;
	}

	@SuppressWarnings("unchecked")
	private <T extends JSON> void fillConfigMap(
			Map<String, T> configurationMap) {
		if (configuration != null) {
			@SuppressWarnings("rawtypes")
			Iterator iterator = configuration.keys();
			while (iterator.hasNext()) {
				String key = (String) iterator.next();
				configurationMap.put(key, (T) configuration.get(key));
			}
		}
	}
}
