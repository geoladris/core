package org.geoladris.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.geoladris.Environment;

public class ConfigFolder {
  private static final Logger logger = Logger.getLogger(ConfigFolder.class);
  private File dir = null;
  private Environment env;
  private ServletContext context;

  public ConfigFolder(ServletContext context, Environment env) {
    this.context = context;
    this.env = env;
  }

  public File getFilePath() {
    if (dir == null) {
      File defaultDir = new File(this.context.getRealPath("WEB-INF/default_config"));

      String configDir = env.getConfigDir(context);
      if (configDir != null) {
        // Directory provided by getConfigDir uses subdirectories for apps
        dir = new File(configDir, context.getContextPath());
      } else {
        // Directory provided by getPortalConfigDir does not use subdirectories
        configDir = env.getPortalConfigDir(context);
        if (configDir != null) {
          dir = new File(configDir);
        }
      }

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
    }

    return dir;
  }

  private File getPortalPropertiesFile() {
    return new File(getFilePath() + "/portal.properties");
  }

  private File getTranslationFolder() {
    return new File(getFilePath(), "messages");
  }

  public Properties getProperties() {
    File file = getPortalPropertiesFile();
    logger.debug("Reading portal properties file " + file);
    Properties properties = new Properties();
    try {
      properties.load(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      logger.warn("Missing portal.properties file");
    } catch (IOException e) {
      logger.error("Error reading portal.properties file", e);
    }

    return properties;
  }

  public ResourceBundle getMessages(Locale locale) {
    URLClassLoader urlClassLoader;
    try {
      urlClassLoader = new URLClassLoader(new URL[] {getTranslationFolder().toURI().toURL()});
    } catch (MalformedURLException e) {
      logger.error("Something is wrong with the configuration directory", e);
      throw new ConfigException(e);
    }
    return ResourceBundle.getBundle("messages", locale, urlClassLoader);
  }

}
