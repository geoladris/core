package org.geoladris.servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.geoladris.Geoladris;
import org.geoladris.config.DBConfigurationProvider;
import org.geoladris.config.DBDataSource;
import org.geoladris.config.ModuleConfigurationProvider;
import org.geoladris.config.PluginJSONConfigurationProvider;
import org.geoladris.config.PublicConfProvider;
import org.geoladris.config.RoleConfigurationProvider;

public class AppContextListener implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ServletContext context = sce.getServletContext();

    DBConfigurationProvider dbProvider = null;
    if (isDBEnabled()) {
      String root = context.getContextPath();
      try {
        if (root.startsWith("/")) {
          root = root.substring(1);
        }

        dbProvider = getDBProvider(root);
      } catch (IOException e) {
        // Cannot obtain config from database. Ignore and use files.
      }
    }

    ArrayList<ModuleConfigurationProvider> providers = new ArrayList<ModuleConfigurationProvider>();
    if (dbProvider != null) {
      providers.add(dbProvider);
    } else {
      providers.add(new PublicConfProvider());
      providers.add(new PluginJSONConfigurationProvider());
      providers.add(new RoleConfigurationProvider());
    }
    context.setAttribute(Geoladris.ATTR_CONFIG_PROVIDERS, providers);
  }

  /**
   * For testing purposes
   */
  DBConfigurationProvider getDBProvider(String contextPath) throws IOException {
    return new DBConfigurationProvider(DBDataSource.getInstance(), contextPath);
  }

  /**
   * For testing purposes
   */
  boolean isDBEnabled() {
    return DBDataSource.getInstance().isEnabled();
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {}
}
