package org.geoladris.servlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.geoladris.Geoladris;
import org.geoladris.PluginDescriptor;
import org.geoladris.PluginDirsAnalyzer;
import org.geoladris.config.Config;

public class RedirectFilter implements Filter {
  private Config config;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.config = (Config) filterConfig.getServletContext().getAttribute(Geoladris.ATTR_CONFIG);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    Locale locale = (Locale) req.getAttribute(Geoladris.ATTR_LOCALE);

    String path = req.getRequestURI().substring(req.getContextPath().length() + 1);
    String[] parts = path.split("/");

    String pluginName = null;
    if (parts.length == 1) {
      // installInRoot
      PluginDescriptor[] plugins = this.config.getPluginConfig(locale, req);
      Map<String, String> moduleRootPluginMap = new HashMap<>();
      for (PluginDescriptor plugin : plugins) {
        if (plugin.isInstallInRoot()) {
          for (String module : plugin.getModules()) {
            moduleRootPluginMap.put(module + ".js", plugin.getName());
          }
        }
      }

      pluginName = moduleRootPluginMap.get(path);
    } else {
      pluginName = parts[0];
      path = path.substring(path.indexOf('/') + 1);
    }

    String configPath = pluginName + "/" + PluginDirsAnalyzer.MODULES + "/" + path;
    File configFile = new File(this.config.getDir(), Config.DIR_PLUGINS + "/" + configPath);
    if (configFile.exists()) {
      request.getRequestDispatcher("/" + Geoladris.PATH_PLUGINS_FROM_CONFIG + configPath)
          .forward(request, response);
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public void destroy() {}
}
