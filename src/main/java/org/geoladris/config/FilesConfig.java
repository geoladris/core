package org.geoladris.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geoladris.Plugin;

public class FilesConfig extends AbstractConfig {
  private static Logger logger = Logger.getLogger(FilesConfig.class);

  public FilesConfig(File configDir, List<PluginConfigProvider> configProviders,
      Set<Plugin> plugins, boolean useCache, int cacheTimeout) throws ConfigException {
    super(configDir, configProviders, plugins, useCache, cacheTimeout);
  }

  @Override
  protected Properties readProperties() {
    File file = new File(this.getDir(), "portal.properties");
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

  @Override
  protected ResourceBundle getResourceBundle(Locale locale) {
    try {
      URL messagesDir = new File(getDir(), "messages").toURI().toURL();
      URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {messagesDir});
      return ResourceBundle.getBundle("messages", locale, urlClassLoader);
    } catch (MalformedURLException e) {
      logger.error("Something is wrong with the configuration directory", e);
      throw new ConfigException(e);
    } catch (MissingResourceException e) {
      logger.info("Missing locale bundle: " + locale);
      try {
        return new PropertyResourceBundle(new ByteArrayInputStream(new byte[0]));
      } catch (IOException e1) {
        // ignore, not an actual IO operation
        return null;
      }
    }
  }
}
