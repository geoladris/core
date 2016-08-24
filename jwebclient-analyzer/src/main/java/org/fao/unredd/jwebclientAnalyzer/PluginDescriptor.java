package org.fao.unredd.jwebclientAnalyzer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * @author vicgonco
 *
 */
public class PluginDescriptor {
	public static final String PROP_DEFAULT_CONF = "default-conf";
	public static final String PROP_REQUIREJS = "requirejs";

	private JSONObject requireJS;
	private JSONObject configuration;
	private Set<String> modules, stylesheets;
	private String name;

	public PluginDescriptor() {
		this.modules = new HashSet<>();
		this.stylesheets = new HashSet<>();
	}

	public PluginDescriptor(String content) {
		this.modules = new HashSet<>();
		this.stylesheets = new HashSet<>();
		setConfiguration(content);
	}

	public void setConfiguration(String content) {
		JSONObject jsonRoot = (JSONObject) JSONSerializer.toJSON(content);

		if (jsonRoot.has(PROP_REQUIREJS)) {
			requireJS = jsonRoot.getJSONObject(PROP_REQUIREJS);
		}
		if (jsonRoot.has(PROP_DEFAULT_CONF)) {
			configuration = jsonRoot.getJSONObject(PROP_DEFAULT_CONF);
		}
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

	public JSONObject getDefaultConf() {
		return configuration;
	}

	public Set<String> getModules() {
		return modules;
	}

	public Set<String> getStylesheets() {
		return stylesheets;
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
