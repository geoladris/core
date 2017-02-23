package org.geoladris.servlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.geoladris.Context;
import org.geoladris.Environment;
import org.geoladris.Geoladris;
import org.geoladris.JEEContext;
import org.geoladris.JEEContextAnalyzer;
import org.geoladris.PluginDescriptor;
import org.geoladris.PluginUpdater;
import org.geoladris.config.Config;
import org.geoladris.config.DBConfig;
import org.geoladris.config.DBConfigurationProvider;
import org.geoladris.config.DBDataSource;
import org.geoladris.config.FilesConfig;
import org.geoladris.config.ModuleConfigurationProvider;
import org.geoladris.config.PublicConfProvider;

public class ConfigFilter implements Filter {
  private static final Logger logger = Logger.getLogger(ConfigFilter.class);

  // Protected for testing purposes
  protected Map<String, Config> appConfigs = new HashMap<>();
  protected Map<Config, Thread> configUpdaters = new HashMap<>();
  protected int schedulerRate = 600;

  private Config defaultConfig;
  private File rootConfigDir;
  private DBConfigurationProvider dbProvider;
  private ServletContext servletContext;

  @Override
  @SuppressWarnings("unchecked")
  public void init(FilterConfig filterConfig) throws ServletException {
    this.servletContext = filterConfig.getServletContext();

    List<ModuleConfigurationProvider> providers =
        (List<ModuleConfigurationProvider>) this.servletContext
            .getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS);
    for (ModuleConfigurationProvider provider : providers) {
      if (provider instanceof DBConfigurationProvider) {
        this.dbProvider = (DBConfigurationProvider) provider;
      }
    }

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
        for (String app : appConfigs.keySet().toArray(new String[0])) {
          if (!isAppEnabled(app)) {
            removeApp(app);
          }
        }
      }
    }, this.schedulerRate, this.schedulerRate, TimeUnit.SECONDS);

  }

  private File getRootConfigDir() {
    if (this.rootConfigDir == null) {
      Environment env = Environment.getInstance();

      String configDir = env.getConfigDir(servletContext);
      if (configDir != null) {
        // Directory provided by getConfigDir uses subdirectories for contexts
        this.rootConfigDir = new File(configDir, servletContext.getContextPath());
      } else {
        // Directory provided by getPortalConfigDir does not use subdirectories
        configDir = env.getPortalConfigDir(servletContext);
        if (configDir != null) {
          this.rootConfigDir = new File(configDir);
        }
      }

      File defaultDir = new File(servletContext.getRealPath("WEB-INF/default_config"));
      if (this.rootConfigDir == null) {
        this.rootConfigDir = defaultDir;
        logger.warn("GEOLADRIS_CONFIG_DIR and PORTAL_CONFIG_DIR properties not found. Using "
            + this.rootConfigDir.getAbsolutePath() + " as configuration directory.");
      } else if (!this.rootConfigDir.exists()) {
        logger.warn("Configuration directory is set to " + this.rootConfigDir.getAbsolutePath()
            + ", but it doesn't exist. Using " + defaultDir.getAbsolutePath() + ".");
        this.rootConfigDir = defaultDir;
      }

      logger.info("============================================================================");
      logger.info("Configuration directory: " + this.rootConfigDir.getAbsolutePath());
      logger.info("============================================================================");
    }
    return rootConfigDir;
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;

    Config config;
    String app = getApp(request);
    if (app != null) {
      config = this.appConfigs.get(app);
      if (config == null) {
        config = initConfig(app, request);
        this.appConfigs.put(app, config);
      }
    } else {
      if (this.defaultConfig == null) {
        this.defaultConfig = initConfig(app, request);
      }
      config = this.defaultConfig;
    }

    config.setRequest(request);
    request.setAttribute(Geoladris.ATTR_CONFIG, config);
    request.setAttribute(Geoladris.ATTR_APP, app);

    chain.doFilter(req, resp);
  }

  /**
   * Determines whether the provided application is enabled or not.
   * 
   * @param app Application to check.
   * @return <code>true</code> if the application is enabled, <code>false</code> otherwise.
   */
  private boolean isAppEnabled(String app) {
    if (dbProvider != null) {
      String root = this.servletContext.getContextPath().substring(1);
      return dbProvider.hasApp(root + "/" + app);
    } else {
      File appConfigDir = new File(getRootConfigDir(), app);
      File publicConf = new File(appConfigDir, PublicConfProvider.FILE);
      File pluginJson = new File(appConfigDir, "plugin-conf.json");
      return publicConf.isFile() || pluginJson.isFile();
    }
  }

  /**
   * Removes the application from {@link #appConfigs} (if contained).
   * 
   * @param app The application to remove.
   */
  private void removeApp(String app) {
    if (this.appConfigs.containsKey(app)) {
      Config config = this.appConfigs.remove(app);
      Thread thread = this.configUpdaters.remove(config);
      thread.interrupt();
    }
  }

  /**
   * Returns the name of the requested application. It also removes the application from
   * {@link #appConfigs} if it no longer exists.
   * 
   * @param request The request to obtain the name of the application.
   * @return the name of the application or <code>null</code> if it's the root application.
   */
  private String getApp(HttpServletRequest request) {
    String root = request.getContextPath().substring(1);
    // length + 2 to remove leading and trailing slashes
    String path = request.getRequestURI();
    path = path.substring(Math.min(path.length(), root.length() + 2));
    if (path.length() == 0) {
      return null;
    }

    String app = path.split("/")[0];
    if (isAppEnabled(app)) {
      return app;
    } else {
      removeApp(app);
      return null;
    }
  }

  /**
   * Creates a new {@link Config}. Analyzes the set of {@link PluginDescriptor} and runs a new
   * {@link PluginUpdater} for this config.
   * 
   * @param app The name of the application within this container or <code>null</code> if it's the
   *        root application.
   * @param request Request that origined the creation of this config.
   * @return the new {@link Config}.
   */
  private Config initConfig(String app, HttpServletRequest request) {
    File root = getRootConfigDir();
    File configDir = app != null ? new File(root, app) : root;
    JEEContext context = new JEEContext(this.servletContext, new File(configDir, "plugins"));
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

    Config config = createConfig(app, configDir, request, plugins, useCache, cacheTimeout);

    try {
      PluginUpdater updater = new PluginUpdater(analyzer, config, context.getDirs());
      Thread t = new Thread(updater);
      t.start();
      this.configUpdaters.put(config, t);
    } catch (IOException e) {
      logger.warn("Cannot start plugin updater. Plugins descriptor won't be updated");
    }

    return config;
  }

  /**
   * For testing purposes
   */
  Config createConfig(String app, File configDir, HttpServletRequest request,
      Set<PluginDescriptor> plugins, boolean useCache, int cacheTimeout) {
    if (DBDataSource.getInstance().isEnabled()) {
      String qualifiedApp = this.servletContext.getContextPath().substring(1);
      qualifiedApp += app != null ? "/" + app : "";
      return new DBConfig(qualifiedApp, configDir, this.servletContext, request, plugins, useCache,
          cacheTimeout);
    } else {
      return new FilesConfig(configDir, this.servletContext, request, plugins, useCache,
          cacheTimeout);
    }
  }

  /**
   * For testing purposes
   */
  JEEContextAnalyzer getAnalyzer(Context context) {
    return new JEEContextAnalyzer(context);
  }

  @Override
  public void destroy() {
    // do nothing
  }
}
