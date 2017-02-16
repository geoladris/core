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
import org.geoladris.config.Config;
import org.geoladris.config.DBConfigurationProvider;
import org.geoladris.config.DefaultConfig;
import org.geoladris.config.ModuleConfigurationProvider;
import org.geoladris.config.PublicConfProvider;

public class ConfigFilter implements Filter {
  private static final Logger logger = Logger.getLogger(ConfigFilter.class);

  private Map<String, Config> appConfigs = new HashMap<>();
  private Config defaultConfig;
  private File rootConfigDir;
  private DBConfigurationProvider dbProvider;
  private ServletContext servletContext;

  private int schedulerRate = 600;

  /**
   * For testing purposes
   */
  protected void setSchedulerRate(int schedulerRate) {
    this.schedulerRate = schedulerRate;
  }

  /**
   * For testing purposes
   */
  protected Map<String, Config> getAppConfigs() {
    return appConfigs;
  }

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
        String root = servletContext.getContextPath().substring(1);
        for (String app : appConfigs.keySet().toArray(new String[0])) {
          if (!isAppEnabled(root, app)) {
            appConfigs.remove(app);
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
        config = initConfig(request, new File(getRootConfigDir(), app));
        this.appConfigs.put(app, config);
      }
    } else {
      if (this.defaultConfig == null) {
        this.defaultConfig = initConfig(request, getRootConfigDir());
      }
      config = this.defaultConfig;
    }

    request.setAttribute(Geoladris.ATTR_CONFIG, config);
    request.setAttribute(Geoladris.ATTR_APP, app);

    chain.doFilter(req, resp);
  }

  private boolean isAppEnabled(String root, String app) {
    if (dbProvider != null && dbProvider.isEnabled()) {
      return dbProvider.hasApp(root + "/" + app);
    } else {
      File appConfigDir = new File(getRootConfigDir(), app);
      File publicConf = new File(appConfigDir, PublicConfProvider.FILE);
      File pluginJson = new File(appConfigDir, "plugin-conf.json");
      return publicConf.isFile() || pluginJson.isFile();
    }
  }

  private String getApp(HttpServletRequest request) {
    String root = request.getContextPath().substring(1);
    // length + 2 to remove leading and trailing slashes
    String path = request.getRequestURI();
    path = path.substring(Math.min(path.length(), root.length() + 2));
    if (path.length() == 0) {
      return null;
    }

    String app = path.split("/")[0];
    if (isAppEnabled(root, app)) {
      return app;
    } else {
      this.appConfigs.remove(app);
      return null;
    }
  }

  private Config initConfig(HttpServletRequest request, File configDir) {
    JEEContext context = new JEEContext(this.servletContext, new File(configDir, "plugins"));
    JEEContextAnalyzer analyzer = getAnalyzer(context);
    Set<PluginDescriptor> plugins = analyzer.getPluginDescriptors();
    boolean useCache = Environment.getInstance().getConfigCache();
    DefaultConfig config =
        new DefaultConfig(configDir, this.servletContext, request, plugins, useCache);
    return config;
  }

  JEEContextAnalyzer getAnalyzer(Context context) {
    return new JEEContextAnalyzer(context);
  }

  @Override
  public void destroy() {
    // do nothing
  }
}
