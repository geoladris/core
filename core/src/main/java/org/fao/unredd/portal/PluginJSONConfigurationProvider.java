package org.fao.unredd.portal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

public class PluginJSONConfigurationProvider
		implements
			ModuleConfigurationProvider {

	@Override
	public Map<String, JSONObject> getConfigurationMap(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException {
		PluginDescriptor pluginDescriptor = getPluginDescriptor(
				configurationContext, request);
		return pluginDescriptor != null
				? pluginDescriptor.getConfigurationMap()
				: new HashMap<String, JSONObject>();
	}

	@Override
	public Map<String, JSON> getConfigMap(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException {
		PluginDescriptor pluginDescriptor = getPluginDescriptor(
				configurationContext, request);

		return pluginDescriptor != null
				? pluginDescriptor.getConfigMap()
				: new HashMap<String, JSON>();
	}

	private PluginDescriptor getPluginDescriptor(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException {
		File configProperties = new File(
				configurationContext.getConfigurationDirectory(),
				"plugin-conf.json");
		BufferedInputStream stream;
		try {
			stream = new BufferedInputStream(
					new FileInputStream(configProperties));
		} catch (FileNotFoundException e) {
			return null;
		}
		String content = IOUtils.toString(stream);
		stream.close();
		return new PluginDescriptor(content);
	}

	@Override
	public boolean canBeCached() {
		return true;
	}

}