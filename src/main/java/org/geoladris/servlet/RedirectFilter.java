package org.geoladris.servlet;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.geoladris.Geoladris;
import org.geoladris.Plugin;
import org.geoladris.PluginDirsAnalyzer;
import org.geoladris.config.Config;

public class RedirectFilter implements Filter {
  private Config config;
  private ServletContext context;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.context = filterConfig.getServletContext();
    this.config = (Config) this.context.getAttribute(Geoladris.ATTR_CONFIG);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;

    String path = req.getRequestURI().substring(req.getContextPath().length() + 1);

    try {
      if (this.context.getResource(path) != null) {
        chain.doFilter(request, response);
        return;
      }
    } catch (MalformedURLException e) {
      chain.doFilter(request, response);
      return;
    }

    int index = path.indexOf('/');
    if (index < 0 && !path.endsWith(".js")) {
      chain.doFilter(request, response);
      return;
    }

    String subdir;
    if (path.startsWith("css") || path.startsWith("jslib") || path.startsWith("node_modules")) {
      subdir = path.substring(0, index);
      path = path.substring(index + 1);
    } else {
      subdir = PluginDirsAnalyzer.MODULES;
    }

    Locale locale = (Locale) req.getAttribute(Geoladris.ATTR_LOCALE);
    Plugin[] plugins = this.config.getPluginConfig(locale, req);
    for (Plugin plugin : plugins) {
      String qualifiedPath;
      if (plugin.isInstallInRoot() || subdir.equals("jslib") || subdir.equals("node_modules")) {
        qualifiedPath = plugin.getName() + "/" + subdir + "/" + path;
      } else {
        index = Math.max(0, path.indexOf('/'));
        String pluginName = path.substring(0, index);
        qualifiedPath = pluginName + "/" + subdir + "/" + path.substring(index + 1);
      }

      String warPath = "/" + Geoladris.PATH_PLUGINS_FROM_WAR + "/" + qualifiedPath;
      File configFile = new File(this.config.getDir(), Config.DIR_PLUGINS + "/" + qualifiedPath);
      if (configFile.exists()) {
        String configPath = "/" + Geoladris.PATH_PLUGINS_FROM_CONFIG + "/" + qualifiedPath;
        request.getRequestDispatcher(configPath).forward(request, response);
        return;
      } else if (this.context.getResource(warPath) != null) {
        request.getRequestDispatcher(warPath).forward(request, response);
        return;
      }
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {}
}
