package org.geoladris.servlet;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.geoladris.Context;
import org.geoladris.Environment;
import org.geoladris.JEEContextAnalyzer;
import org.geoladris.PluginDescriptor;
import org.geoladris.config.ConfigFolder;
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

    Set<PluginDescriptor> plugins = analyzer.getPluginDescriptors();
    File publicConf = new File(folder.getFilePath(), PublicConfProvider.FILE);
    boolean hasPublicConf = publicConf.exists() && publicConf.isFile();
    ModuleConfigurationProvider publicConfigurationProvider;
    if (hasPublicConf) {
      publicConfigurationProvider = new PublicConfProvider(folder.getFilePath());
    } else {
      publicConfigurationProvider = new PluginJSONConfigurationProvider();
      logger.warn("plugin-conf.json file for configuration has been "
          + "deprecated. Use public-conf.json instead.");

    }

    DefaultConfig config = new DefaultConfig(folder, plugins, env.getConfigCache());
    config.addModuleConfigurationProvider(publicConfigurationProvider);
    config.addModuleConfigurationProvider(new RoleConfigurationProvider(folder.getFilePath()));

    servletContext.setAttribute(ATTR_CONFIG, config);
  }

  /**
   * For testing purposes.
   */
  JEEContextAnalyzer getAnalyzer(Context context) {
    return new JEEContextAnalyzer(context);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {}

  private class JEEContext implements Context {

    private ServletContext servletContext;
    private File noJavaRoot;

    public JEEContext(ServletContext servletContext, File noJavaRoot) {
      this.servletContext = servletContext;
      this.noJavaRoot = noJavaRoot;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getLibPaths() {
      return servletContext.getResourcePaths("/WEB-INF/lib");
    }

    @Override
    public InputStream getLibAsStream(String jarFileName) {
      return servletContext.getResourceAsStream(jarFileName);
    }

    @Override
    public File[] getDirs() {
      return new File[] {
          new File(servletContext
              .getRealPath("/WEB-INF/classes/" + JEEContextAnalyzer.CLIENT_RESOURCES_DIR)),
          noJavaRoot};
    }
  }
}
