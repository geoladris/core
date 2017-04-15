package org.geoladris.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.geoladris.JEEContext;
import org.geoladris.Environment;
import org.geoladris.Geoladris;
import org.geoladris.JEEContextAnalyzer;
import org.geoladris.PluginDescriptor;
import org.geoladris.PluginUpdater;
import org.geoladris.config.Config;
import org.geoladris.config.DBConfig;
import org.geoladris.config.DBConfigurationProvider;
import org.geoladris.config.DBDataSource;
import org.geoladris.config.FilesConfig;
import org.geoladris.config.ModuleConfigurationProvider;
import org.geoladris.config.PluginJSONConfigurationProvider;
import org.geoladris.config.PublicConfProvider;
import org.geoladris.config.RoleConfigurationProvider;

public class AppContextListener implements ServletContextListener {
  private static final Logger logger = Logger.getLogger(AppContextListener.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ServletContext servletContext = sce.getServletContext();

    String root = servletContext.getContextPath();
    if (root.startsWith("/")) {
      root = root.substring(1);
    }

    File configDir = getConfigDir(servletContext);
    JEEContext context = new JEEContext(servletContext, new File(configDir, "plugins"));
    JEEContextAnalyzer analyzer = getAnalyzer(context);
    Set<PluginDescriptor> plugins = analyzer.getPluginDescriptors();
    boolean useCache = Environment.getInstance().getConfigCache();

    String timeoutProp = Environment.getInstance().get(Environment.CACHE_TIMEOUT);
    int cacheTimeout = -1;
    if (timeoutProp != null) {
      try {
        cacheTimeout = Integer.parseInt(timeoutProp);
      } catch (NumberFormatException e) {
        logger.info("Invalid integer value for '" + Environment.CACHE_TIMEOUT
            + "'. Cache timeout disabled");
      }
    }

    List<ModuleConfigurationProvider> providers = new ArrayList<>();
    Config config = null;
    if (isDBEnabled()) {
      try {
        providers.add(getDBProvider(root));
        config = new DBConfig(root, configDir, providers, plugins, useCache, cacheTimeout);
      } catch (IOException e) {
        // Cannot obtain config from database. Ignore and use files.
      }
    }

    if (config == null) {
      providers.add(new PublicConfProvider());
      providers.add(new PluginJSONConfigurationProvider());
      providers.add(new RoleConfigurationProvider());
      config = new FilesConfig(configDir, providers, plugins, useCache, cacheTimeout);
    }

    servletContext.setAttribute(Geoladris.ATTR_CONFIG, config);

    try {
      PluginUpdater updater = new PluginUpdater(analyzer, config, context.getDirs());
      Thread t = new Thread(updater);
      t.start();
    } catch (IOException e) {
      logger.warn("Cannot start plugin updater. Plugins descriptor won't be updated");
    }
  }

  private File getConfigDir(ServletContext servletContext) {
    Environment env = Environment.getInstance();

    String envConfigDir = env.getConfigDir(servletContext);
    File configDir = null;
    if (envConfigDir != null) {
      // Directory provided by getConfigDir uses subdirectories for contexts
      configDir = new File(envConfigDir, servletContext.getContextPath());
    } else {
      // Directory provided by getPortalConfigDir does not use subdirectories
      envConfigDir = env.getPortalConfigDir(servletContext);
      if (envConfigDir != null) {
        configDir = new File(envConfigDir);
      }
    }

    File defaultDir = new File(servletContext.getRealPath("WEB-INF/default_config"));
    if (configDir == null) {
      configDir = defaultDir;
      logger.warn("GEOLADRIS_CONFIG_DIR and PORTAL_CONFIG_DIR properties not found. Using "
          + configDir.getAbsolutePath() + " as configuration directory.");
    } else if (!configDir.exists()) {
      logger.warn("Configuration directory is set to " + configDir.getAbsolutePath()
          + ", but it doesn't exist. Using " + defaultDir.getAbsolutePath() + ".");
      configDir = defaultDir;
    }

    logger.info("============================================================================");
    logger.info("Configuration directory: " + configDir.getAbsolutePath());
    logger.info("============================================================================");

    return configDir;
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

  /**
   * For testing purposes
   */
  JEEContextAnalyzer getAnalyzer(JEEContext context) {
    return new JEEContextAnalyzer(context);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {}
}
