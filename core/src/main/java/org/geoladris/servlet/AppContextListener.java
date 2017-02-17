package org.geoladris.servlet;

import java.util.ArrayList;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.geoladris.Geoladris;
import org.geoladris.config.DBConfigurationProvider;
import org.geoladris.config.ModuleConfigurationProvider;
import org.geoladris.config.PluginJSONConfigurationProvider;
import org.geoladris.config.PublicConfProvider;

public class AppContextListener implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ServletContext context = sce.getServletContext();
    String root = context.getContextPath();
    DBConfigurationProvider dbProvider = null;
    try {
      if (root.startsWith("/")) {
        root = root.substring(1);
      }

      dbProvider = getDBProvider(root);
    } catch (NamingException e) {
      // Cannot obtain config from database. Ignore.
    }

    ArrayList<ModuleConfigurationProvider> providers = new ArrayList<ModuleConfigurationProvider>();
    if (dbProvider != null && dbProvider.isEnabled()) {
      providers.add(dbProvider);
    } else {
      providers.add(new PublicConfProvider());
      providers.add(new PluginJSONConfigurationProvider());
    }

    context.setAttribute(Geoladris.ATTR_CONFIG_PROVIDERS, providers);
  }

  /**
   * For testing purposes
   */
  DBConfigurationProvider getDBProvider(String contextPath) throws NamingException {
    return new DBConfigurationProvider(contextPath, new InitialContext());
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {}
}
