package org.geoladris.servlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.catalina.Globals;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.log4j.Logger;
import org.geoladris.CSSOverridesUpdater;
import org.geoladris.DirectoryWatcher;
import org.geoladris.Environment;
import org.geoladris.Geoladris;
import org.geoladris.Plugin;
import org.geoladris.PluginDirsAnalyzer;
import org.geoladris.PluginUpdater;
import org.geoladris.config.Config;
import org.geoladris.config.DBConfig;
import org.geoladris.config.DBDataSource;
import org.geoladris.config.FilesConfig;
import org.geoladris.config.PluginConfigProvider;
import org.geoladris.config.providers.DBConfigProvider;
import org.geoladris.config.providers.PluginJSONConfigProvider;
import org.geoladris.config.providers.PublicConfProvider;
import org.geoladris.config.providers.RoleConfigProvider;

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
    File pluginsFromConfig = new File(configDir, Config.DIR_PLUGINS);
    String pluginsFromWar = servletContext.getRealPath("/" + Geoladris.PATH_PLUGINS_FROM_WAR);
    File[] pluginsDirs = new File[] {new File(pluginsFromWar), pluginsFromConfig};
    PluginDirsAnalyzer analyzer = getAnalyzer(pluginsDirs);
    Set<Plugin> plugins = analyzer.getPlugins();
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

    List<PluginConfigProvider> providers = new ArrayList<>();
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
      providers.add(new PluginJSONConfigProvider());
      providers.add(new RoleConfigProvider());
      config = new FilesConfig(configDir, providers, plugins, useCache, cacheTimeout);
    }

    servletContext.setAttribute(Geoladris.ATTR_CONFIG, config);

    File staticDir = new File(configDir, Config.DIR_STATIC);
    addDirectoryWatcher(new PluginUpdater(analyzer, config), pluginsDirs);
    addDirectoryWatcher(new CSSOverridesUpdater(config), staticDir, pluginsFromConfig);

    WebResourceRoot resourcesRoot =
        (WebResourceRoot) servletContext.getAttribute(Globals.RESOURCES_ATTR);
    addStaticResources(resourcesRoot, "/" + Geoladris.PATH_STATIC, staticDir);
    addStaticResources(resourcesRoot, "/" + Geoladris.PATH_PLUGINS_FROM_CONFIG, pluginsFromConfig);
  }

  private void addDirectoryWatcher(Runnable action, File... dirs) {
    try {
      DirectoryWatcher.watch(action, dirs);
    } catch (IOException e) {
      logger.warn("Cannot start updater: " + action.getClass().getCanonicalName()
          + ". It won't be updated");
    }
  }

  private void addStaticResources(WebResourceRoot resourcesRoot, String urlPath, File dir) {
    try {
      DirResourceSet resourceSet =
          new DirResourceSet(resourcesRoot, urlPath, dir.getAbsolutePath(), "/");
      resourcesRoot.addPreResources(resourceSet);
    } catch (Throwable e) {
      logger.error("Cannot add static resources", e);
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
  DBConfigProvider getDBProvider(String contextPath) throws IOException {
    return new DBConfigProvider(DBDataSource.getInstance(), contextPath);
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
  PluginDirsAnalyzer getAnalyzer(File[] pluginsDirs) {
    return new PluginDirsAnalyzer(pluginsDirs);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {}
}
