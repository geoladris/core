package org.geoladris.servlet;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.geoladris.Context;
import org.geoladris.Environment;
import org.geoladris.JEEContext;
import org.geoladris.JEEContextAnalyzer;
import org.geoladris.PluginDescriptor;
import org.geoladris.PluginUpdater;
import org.geoladris.config.ConfigFolder;
import org.geoladris.config.DBConfigurationProvider;
import org.geoladris.config.DefaultConfig;
import org.geoladris.config.ModuleConfigurationProvider;
import org.geoladris.config.PluginJSONConfigurationProvider;
import org.geoladris.config.PublicConfProvider;
import org.geoladris.config.RoleConfigurationProvider;

public class AppContextListener implements ServletContextListener {
  private static final Logger logger = Logger.getLogger(AppContextListener.class);

  public static final String ATTR_CONFIG = "config";
  public static final String ATTR_ENV = "environment";

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ServletContext servletContext = sce.getServletContext();

    Environment env = new Environment();
    servletContext.setAttribute(ATTR_ENV, env);
    ConfigFolder folder = new ConfigFolder(servletContext, env);

    JEEContext context = new JEEContext(servletContext, new File(folder.getFilePath(), "plugins"));
    JEEContextAnalyzer analyzer = getAnalyzer(context);

    ModuleConfigurationProvider publicConfigurationProvider = null;

    try {
      String app = servletContext.getContextPath();
      if (app.startsWith("/")) {
        app = app.substring(1);
      }

      DBConfigurationProvider dbConfigProvider =
          new DBConfigurationProvider(app, new InitialContext());
      if (dbConfigProvider.isEnabled()) {
        publicConfigurationProvider = dbConfigProvider;
      }
    } catch (NamingException e) {
      // Cannot obtain config from database. Ignore.
    }

    if (publicConfigurationProvider == null) {
      File publicConf = new File(folder.getFilePath(), PublicConfProvider.FILE);
      if (publicConf.exists() && publicConf.isFile()) {
        publicConfigurationProvider = new PublicConfProvider(folder.getFilePath());
      } else {
        publicConfigurationProvider = new PluginJSONConfigurationProvider();
        logger.warn("plugin-conf.json file for configuration has been "
            + "deprecated. Use public-conf.json instead.");

      }
    }

    Set<PluginDescriptor> plugins = analyzer.getPluginDescriptors();
    DefaultConfig config = new DefaultConfig(folder, plugins, env.getConfigCache());
    config.addModuleConfigurationProvider(publicConfigurationProvider);
    config.addModuleConfigurationProvider(new RoleConfigurationProvider(folder.getFilePath()));

    servletContext.setAttribute(ATTR_CONFIG, config);

    try {
      PluginUpdater updater = new PluginUpdater(analyzer, config, context.getDirs());
      new Thread(updater).start();
    } catch (IOException e) {
      logger.warn("Cannot start plugin updater. Plugins descriptor won't be updated");
    }
  }

  /**
   * For testing purposes.
   */
  JEEContextAnalyzer getAnalyzer(Context context) {
    return new JEEContextAnalyzer(context);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {}
}
