package org.fao.unredd.portal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import net.sf.json.JSONObject;

import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;

public class PluginDescriptors {
	private static final String CONF_ENABLED = "_enabled";
	private static final String CONF_OVERRIDE = "_override";
	public static final String UNNAMED_GEOLADRIS_CORE_PLUGIN = "unnamed_geoladris_core_plugin";

	private HashMap<String, PluginDescriptor> namePluginDescriptor = new HashMap<String, PluginDescriptor>();
	private PluginDescriptor[] enabled = null;

	public PluginDescriptors(Set<PluginDescriptor> plugins) {
		for (PluginDescriptor pluginDescriptor : plugins) {
			String pluginName = pluginDescriptor.getName();
			if (pluginName == null) {
				pluginName = UNNAMED_GEOLADRIS_CORE_PLUGIN;
			}
			namePluginDescriptor.put(pluginName,
					pluginDescriptor.cloneDescriptor());
		}
	}

	public void merge(String pluginName,
			JSONObject overridingPluginConfiguration) {
		PluginDescriptor pluginDescriptor = namePluginDescriptor
				.get(pluginName);
		if (pluginDescriptor == null) {
			pluginDescriptor = new PluginDescriptor();
			pluginDescriptor.mergeConfiguration(overridingPluginConfiguration);
			namePluginDescriptor.put(pluginName, pluginDescriptor);
		} else {
			if (!overridingPluginConfiguration.has(CONF_OVERRIDE)
					|| !overridingPluginConfiguration.getBoolean(CONF_OVERRIDE)) {
				pluginDescriptor
						.mergeConfiguration(overridingPluginConfiguration);
			} else {
				pluginDescriptor
						.setConfiguration(overridingPluginConfiguration);
			}
		}
	}

	public PluginDescriptor[] getEnabled() {
		if (enabled == null) {
			ArrayList<PluginDescriptor> ret = new ArrayList<PluginDescriptor>();
			for (PluginDescriptor pluginDescriptor : namePluginDescriptor
					.values()) {
				JSONObject pluginConfiguration = pluginDescriptor
						.getConfiguration();

				if (!pluginConfiguration.has(CONF_ENABLED)
						|| pluginConfiguration.getBoolean(CONF_ENABLED) == true) {
					pluginConfiguration.remove(CONF_ENABLED);
					pluginConfiguration.remove(CONF_OVERRIDE);

					ret.add(pluginDescriptor);
				}
			}

			enabled = ret.toArray(new PluginDescriptor[ret.size()]);
		}
		return enabled;
	}

	public PluginDescriptor get(String pluginName) {
		return namePluginDescriptor.get(pluginName);
	}

}
