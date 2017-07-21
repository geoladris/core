package org.geoladris.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoladris.Geoladris;
import org.geoladris.Plugin;
import org.geoladris.config.Config;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class ConfigServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    Config config = (Config) getServletContext().getAttribute(Geoladris.ATTR_CONFIG);
    Locale locale = (Locale) req.getAttribute(Geoladris.ATTR_LOCALE);

    ResourceBundle bundle = config.getMessages(locale);

    String title;
    try {
      title = bundle.getString("title");
    } catch (MissingResourceException e) {
      title = "Untitled";
    }

    JSONObject moduleConfig = new JSONObject();
    // Fixed elements
    Plugin[] enabledPluginDescriptors = config.getPluginConfig(locale, req);
    moduleConfig.element("customization", buildCustomizationObject(getServletContext(), config,
        locale, title, enabledPluginDescriptors));
    moduleConfig.element("i18n", buildI18NObject(bundle));
    moduleConfig.element("url-parameters", JSONSerializer.toJSON(req.getParameterMap()));

    JSONObject paths = new JSONObject();
    JSONObject shim = new JSONObject();

    for (Plugin pluginDescriptor : enabledPluginDescriptors) {
      JSONObject configuration = pluginDescriptor.getConfiguration();
      if (configuration != null) {
        moduleConfig.putAll(configuration);
      }

      JSONObject requirejs = pluginDescriptor.getRequireJS();
      if (requirejs != null && !requirejs.isNullObject()) {
        if (requirejs.has("paths")) {
          paths.putAll(requirejs.getJSONObject("paths"));
        }
        if (requirejs.has("shim")) {
          shim.putAll(requirejs.getJSONObject("shim"));
        }
      }
    }

    JSONObject ret = new JSONObject();
    ret.element("config", moduleConfig);
    ret.element("paths", paths);
    ret.element("shim", shim);
    String json = ret.toString();

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

  private JSONObject buildCustomizationObject(ServletContext servletContext, Config config,
      Locale locale, String title, Plugin[] plugins) {
    JSONObject obj = new JSONObject();
    obj.element("title", title);
    obj.element(Config.PROPERTY_LANGUAGES, config.getLanguages());
    obj.element("languageCode", locale.getLanguage());
    obj.element(Config.PROPERTY_MAP_CENTER, config.getPropertyAsArray(Config.PROPERTY_MAP_CENTER));
    obj.element("map.initialZoomLevel", config.getProperties().get("map.initialZoomLevel"));

    ArrayList<String> modules = new ArrayList<String>();
    String[] extraModules = config.getPropertyAsArray(Config.PROPERTY_CLIENT_MODULES);
    if (extraModules != null) {
      Collections.addAll(modules, extraModules);
    }
    for (Plugin plugin : plugins) {
      modules.addAll(plugin.getModules());
    }
    obj.element("modules", modules);

    return obj;
  }
}
