package org.fao.unredd.portal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;

import net.sf.json.JSONObject;

public class DefaultConfProvider implements ModuleConfigurationProvider {
	private Set<PluginDescriptor> plugins;

	public DefaultConfProvider(Set<PluginDescriptor> plugins) {
		this.plugins = plugins;
	}

	@Override
	public Map<PluginDescriptor, JSONObject> getPluginConfig(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException {
		Map<PluginDescriptor, JSONObject> ret = new HashMap<>();
		for (PluginDescriptor plugin : this.plugins) {
			ret.put(plugin, plugin.getDefaultConf());
		}
		return ret;
	}

	@Override
	public boolean canBeCached() {
		return true;
	}
}
