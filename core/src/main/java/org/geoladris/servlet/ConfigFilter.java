package org.geoladris.servlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  @Override
  @SuppressWarnings("unchecked")
  public void init(FilterConfig filterConfig) throws ServletException {
    this.servletContext = filterConfig.getServletContext();
    this.rootConfigDir = getDir(this.servletContext);

    List<ModuleConfigurationProvider> providers =
        (List<ModuleConfigurationProvider>) this.servletContext
            .getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS);
    for (ModuleConfigurationProvider provider : providers) {
      if (provider instanceof DBConfigurationProvider) {
        this.dbProvider = (DBConfigurationProvider) provider;
      }
    }
  }

  public File getDir(ServletContext context) {
    Environment env = Environment.getInstance();

    File dir = null;
    String configDir = env.getConfigDir(context);
    if (configDir != null) {
      // Directory provided by getConfigDir uses subdirectories for contexts
      dir = new File(configDir, context.getContextPath());
    } else {
      // Directory provided by getPortalConfigDir does not use subdirectories
      configDir = env.getPortalConfigDir(context);
      if (configDir != null) {
        dir = new File(configDir);
      }
    }

    File defaultDir = new File(context.getRealPath("WEB-INF/default_config"));
    if (dir == null) {
      dir = defaultDir;
      logger.warn("GEOLADRIS_CONFIG_DIR and PORTAL_CONFIG_DIR properties " + "not found. Using "
          + dir.getAbsolutePath() + " as configuration directory.");
    } else if (!dir.exists()) {
      logger.warn("Configuration directory is set to " + dir.getAbsolutePath()
          + ", but it doesn't exist. Using " + defaultDir.getAbsolutePath() + ".");
      dir = defaultDir;
    }

    logger.info("============================================================================");
    logger.info("Configuration directory: " + dir.getAbsolutePath());
    logger.info("============================================================================");

    return dir;
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
        config = initConfig(request, new File(this.rootConfigDir, app));
        this.appConfigs.put(app, config);
      }
    } else {
      if (this.defaultConfig == null) {
        this.defaultConfig = initConfig(request, this.rootConfigDir);
      }
      config = this.defaultConfig;
    }

    request.setAttribute(Geoladris.ATTR_CONFIG, config);
    request.setAttribute(Geoladris.ATTR_APP, app);

    chain.doFilter(req, resp);
  }

  private String getApp(HttpServletRequest request) {
    String root = request.getContextPath();
    String path = request.getRequestURI().substring(root.length() + 1);
    if (path.length() == 0) {
      return null;
    }
    String app = path.split("/")[0];

    if (dbProvider != null && dbProvider.isEnabled()) {
      return dbProvider.hasApp(root + "/" + app) ? app : null;
    } else {
      File appConfigDir = new File(this.rootConfigDir, app);
      File publicConf = new File(appConfigDir, PublicConfProvider.FILE);
      File pluginJson = new File(appConfigDir, "plugin-conf.json");
      return publicConf.isFile() || pluginJson.isFile() ? app : null;
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
