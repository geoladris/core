package org.geoladris.config;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.geoladris.PortalRequestConfiguration;

import net.sf.json.JSONObject;

public interface ModuleConfigurationProvider {
	/**
	 * Returns a map where the keys are the configured plugins and the
	 * JSONObjects are the configuration for the modules contained in the
	 * plugin.
	 * 
	 * It is possible to use the anonymous plugin
	 * {@link PluginDescriptors#UNNAMED_GEOLADRIS_CORE_PLUGIN} as plugin name
	 * but in that case the configuration will not be associated to a plugin and
	 * therefore it will not be possible to enable/disable it
	 * 
	 * @param configurationContext
	 * @param request
	 *            Request that loads the application
	 * 
	 * @return
	 * @throws IOException
	 */
	Map<String, JSONObject> getPluginConfig(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException;

	/**
	 * True if the value returned by
	 * {@link #getConfigurationMap(PortalRequestConfiguration, HttpServletRequest)}
	 * can be cached so that the method is not called in every request.
	 * 
	 * @return
	 */
	boolean canBeCached();
}
