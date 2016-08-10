package org.fao.unredd.portal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fao.unredd.AppContextListener;

import de.csgis.commons.JSONUtils;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class ConfigServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp, getServletContext());
	}

	@SuppressWarnings("unchecked")
	void doGet(HttpServletRequest req, HttpServletResponse resp,
			ServletContext context) throws IOException {
		Config config = (Config) context
				.getAttribute(AppContextListener.ATTR_CONFIG);
		Locale locale = (Locale) req.getAttribute(LangFilter.ATTR_LOCALE);

		ResourceBundle bundle = config.getMessages(locale);

		String title;
		try {
			title = bundle.getString("title");
		} catch (MissingResourceException e) {
			title = "Untitled";
		}

		JSONObject moduleConfig = new JSONObject();
		// Fixed elements
		moduleConfig.element("customization",
				buildCustomizationObject(context, config, locale, title));
		moduleConfig.element("i18n", buildI18NObject(bundle));
		moduleConfig.element("url-parameters",
				JSONSerializer.toJSON(req.getParameterMap()));
		/*
		 * Plugin configuration. default plugin conf and overridden by multiple
		 * potential means (by default, by portal plugin-conf.json)
		 */
		Map<String, JSON> pluginConfiguration = (Map<String, JSON>) context
				.getAttribute(AppContextListener.ATTR_PLUGIN_CONFIGURATION);
		Set<String> mergeConfModules = (Set<String>) context
				.getAttribute(AppContextListener.ATTR_MERGE_CONF_MODULES);
		Map<String, JSON> pluginConfigurationOverride = config
				.getPluginConfig(locale, req);
		for (String configurationItem : pluginConfigurationOverride.keySet()) {
			JSON cfg = pluginConfigurationOverride.get(configurationItem);
			Object defaultCfg = pluginConfiguration.get(configurationItem);
			if (mergeConfModules.contains(configurationItem)
					&& cfg instanceof JSONObject
					&& defaultCfg instanceof JSONObject) {
				cfg = JSONUtils.merge((JSONObject) defaultCfg,
						(JSONObject) cfg);
			}
			moduleConfig.element(configurationItem, cfg);
		}
		for (String configurationItem : pluginConfiguration.keySet()) {
			JSON defaultConfiguration = pluginConfiguration
					.get(configurationItem);
			if (!pluginConfigurationOverride.containsKey(configurationItem)) {
				moduleConfig.element(configurationItem, defaultConfiguration);
			}
		}

		String json = new JSONObject().element("config", moduleConfig)
				.toString();

		resp.setContentType("application/javascript");
		resp.setCharacterEncoding("utf8");
		PrintWriter writer = resp.getWriter();
		writer.write("var require = " + json);
	}

	private HashMap<String, String> buildI18NObject(ResourceBundle bundle) {
		HashMap<String, String> messages = new HashMap<String, String>();
		for (String key : bundle.keySet()) {
			messages.put(key, bundle.getString(key));
		}

		return messages;
	}

	private JSONObject buildCustomizationObject(ServletContext servletContext,
			Config config, Locale locale, String title) {
		// These properties will be handled manually at the end of the method
		HashSet<String> manuallyHandled = new HashSet<String>();
		Collections.addAll(manuallyHandled, Config.PROPERTY_CLIENT_MODULES,
				Config.PROPERTY_MAP_CENTER, Config.PROPERTY_LANGUAGES);

		// We put here all the properties except for the manually handled
		Properties properties = config.getProperties();
		JSONObject obj = new JSONObject();
		for (Object keyObj : properties.keySet()) {
			String key = keyObj.toString();
			if (!manuallyHandled.contains(key)) {
				obj.element(key, properties.getProperty(key));
			}
		}

		// We put the manually handled properties
		obj.element("title", title);
		obj.element(Config.PROPERTY_LANGUAGES, config.getLanguages());
		obj.element("languageCode", locale.getLanguage());
		obj.element(Config.PROPERTY_MAP_CENTER,
				config.getPropertyAsArray(Config.PROPERTY_MAP_CENTER));

		ArrayList<String> modules = new ArrayList<String>();
		String[] extraModules = config
				.getPropertyAsArray(Config.PROPERTY_CLIENT_MODULES);
		if (extraModules != null) {
			Collections.addAll(modules, extraModules);
		}
		@SuppressWarnings("unchecked")
		ArrayList<String> classPathModules = (ArrayList<String>) servletContext
				.getAttribute(AppContextListener.ATTR_JS_PATHS);
		modules.addAll(classPathModules);
		obj.element("modules", modules);

		return obj;
	}
}
