package org.fao.unredd.portal;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;

import de.csgis.commons.JSONContentProvider;
import net.sf.json.JSONObject;

/**
 * <p>
 * Helper for {@link ModuleConfigurationProvider} classes.
 * </p>
 * 
 * @author victorzinho
 */
public class ConfigurationProviderHelper {
	private static final Logger logger = Logger
			.getLogger(ConfigurationProviderHelper.class);

	private JSONContentProvider contents;
	private Map<String, PluginDescriptor> plugins;

	public ConfigurationProviderHelper(Map<String, PluginDescriptor> plugins) {
		this(null, plugins);
	}

	public ConfigurationProviderHelper(JSONContentProvider provider,
			Map<String, PluginDescriptor> plugins) {
		this.contents = provider;
		this.plugins = plugins;
	}

	/**
	 * Get the plugin configurations from the requested <code>.json</code> file.
	 * <code>.json </code> files have the following format:
	 * 
	 * <ul>
	 * <li>Always contains a valid JSON object.
	 * <li>The keys of the JSON objects match plugin names.
	 * <li>The value for each key/plugin is another JSON object whose keys are
	 * module names and values are module configurations.
	 * </ul>
	 * 
	 * 
	 * @param file
	 *            The file to obtain the configuration. Contents are provided by
	 *            the {@link JSONContentProvider} used in
	 *            {@link #PublicConfHelper(JSONContentProvider, Map)}.
	 * @return A map with the plugin configuration. Keys are plugin names;
	 *         values are JSON objects with module configurations (JSON object
	 *         keys are module names; JSON object values are module
	 *         configurations).
	 * @throws NullPointerException
	 *             if this instance has been created with a constructor that
	 *             does not receive a {@link JSONContentProvider}, such as
	 *             {@link #ConfigurationProviderHelper(JSONContentProvider, Map)}.
	 */
	public Map<PluginDescriptor, JSONObject> getPluginConfig(String file)
			throws NullPointerException {
		JSONObject conf = this.contents.get().get(file);
		if (conf == null) {
			return null;
		}

		Map<PluginDescriptor, JSONObject> ret = new HashMap<PluginDescriptor, JSONObject>();
		for (Object plugin : conf.keySet()) {
			PluginDescriptor descriptor = this.plugins.get(plugin);
			if (descriptor == null) {
				logger.warn("Cannot find descriptor for plugin: " + plugin);
			} else {
				ret.put(descriptor, conf.getJSONObject(plugin.toString()));
			}
		}

		return ret;
	}
}
