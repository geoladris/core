package org.fao.unredd.portal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fao.unredd.AppContextListener;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class ConfigServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp, getServletContext());
	}

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

		Map<PluginDescriptor, JSONObject> pluginConfs = config
				.getPluginConfig(locale, req);

		JSONObject moduleConfig = new JSONObject();
		// Fixed elements
		moduleConfig.element("customization", buildCustomizationObject(context,
				config, locale, title, pluginConfs.keySet()));
		moduleConfig.element("i18n", buildI18NObject(bundle));
		moduleConfig.element("url-parameters",
				JSONSerializer.toJSON(req.getParameterMap()));

		for (JSONObject conf : pluginConfs.values()) {
			if (conf != null) {
				moduleConfig.putAll(conf);
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
			Config config, Locale locale, String title,
			Set<PluginDescriptor> plugins) {
		JSONObject obj = new JSONObject();
		obj.element("title", title);
		obj.element(Config.PROPERTY_LANGUAGES, config.getLanguages());
		obj.element("languageCode", locale.getLanguage());
		obj.element(Config.PROPERTY_MAP_CENTER,
				config.getPropertyAsArray(Config.PROPERTY_MAP_CENTER));
		obj.element("map.initialZoomLevel",
				config.getProperties().get("map.initialZoomLevel"));

		ArrayList<String> modules = new ArrayList<String>();
		String[] extraModules = config
				.getPropertyAsArray(Config.PROPERTY_CLIENT_MODULES);
		if (extraModules != null) {
			Collections.addAll(modules, extraModules);
		}
		for (PluginDescriptor plugin : plugins) {
			modules.addAll(plugin.getModules());
		}
		obj.element("modules", modules);

		return obj;
	}
}