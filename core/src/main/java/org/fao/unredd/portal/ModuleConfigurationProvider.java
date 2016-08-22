package org.fao.unredd.portal;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;

import net.sf.json.JSONObject;

public interface ModuleConfigurationProvider {

	/**
	 * Returns a map where the keys are the names of the modules and the
	 * JSONObjects are the configuration of each module
	 *
	 * @param configurationContext
	 * @param request
	 *            Request that loads the application
	 * @deprecated Use
	 *             {@link #getConfigMap(PortalRequestConfiguration, HttpServletRequest)}.
	 *
	 * @return
	 * @throws IOException
	 */
	Map<String, JSONObject> getConfigurationMap(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException;

	/**
	 * Returns a map where the keys are the configured plugins and the
	 * JSONObjects are the configuration for the modules contained in the
	 * plugin.
	 * 
	 * @param configurationContext
	 * @param request
	 *            Request that loads the application
	 * 
	 * @return
	 * @throws IOException
	 */
	Map<PluginDescriptor, JSONObject> getPluginConfig(
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
