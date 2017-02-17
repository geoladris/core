package org.geoladris.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.geoladris.Geoladris;
import org.geoladris.PluginDescriptor;

import net.sf.json.JSONObject;

/**
 * Utility class to access the custom resources placed in PORTAL_CONFIG_DIR.
 * 
 * @author Oscar Fonts
 * @author Fernando Gonzalez
 */
public class DefaultConfig implements Config {

  private static Logger logger = Logger.getLogger(DefaultConfig.class);
  private static final String PROPERTY_DEFAULT_LANG = "languages.default";

  private Properties properties;

  private File configDir;
  private boolean useCache;
  private HashMap<Locale, ResourceBundle> localeBundles = new HashMap<Locale, ResourceBundle>();
  private Map<ModuleConfigurationProvider, Map<String, JSONObject>> cachedConfigurations =
      new HashMap<ModuleConfigurationProvider, Map<String, JSONObject>>();

  private Set<PluginDescriptor> plugins;
  private HttpServletRequest request;
  private ServletContext context;

  public DefaultConfig(File configDir, ServletContext context, HttpServletRequest request,
      Set<PluginDescriptor> plugins, boolean useCache) {
    this.configDir = configDir;
    this.plugins = plugins;
    this.useCache = useCache;
    this.request = request;
    this.context = context;
  }

  public void setRequest(HttpServletRequest request) {
    this.request = request;
  }

  @Override
  public File getDir() {
    return configDir;
  }

  @Override
  public synchronized Properties getProperties() {
    if (properties == null || !useCache) {
      File file = new File(this.configDir, "portal.properties");
      logger.debug("Reading portal properties file " + file);
      properties = new Properties();
      try {
        properties.load(new FileInputStream(file));
      } catch (FileNotFoundException e) {
        logger.warn("Missing portal.properties file");
      } catch (IOException e) {
        logger.error("Error reading portal.properties file", e);
      }
    }
    return properties;
  }

  @Override
  public void setPlugins(Set<PluginDescriptor> plugins) {
    this.plugins = plugins;
  }

  /**
   * Returns an array of <code>Map&lt;String, String&gt;</code>. For each element of the array, a
   * {@link Map} is returned containing two keys/values: <code>code</code> (for language code) and
   * <code>name</code> (for language name).
   * 
   * @return
   */
  @Override
  @SuppressWarnings("unchecked")
  public Map<String, String>[] getLanguages() {

    try {
      List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
      JSONObject json = JSONObject.fromObject(getProperty("languages"));
      for (Object langCode : json.keySet()) {
        Map<String, String> langObject = new HashMap<String, String>();
        langObject.put("code", langCode.toString());
        langObject.put("name", json.getString(langCode.toString()));

        ret.add(langObject);
      }
      return ret.toArray(new Map[ret.size()]);
    } catch (ConfigException e) {
      return null;
    }
  }

  @Override
  public ResourceBundle getMessages(Locale locale) throws ConfigException {
    ResourceBundle bundle = localeBundles.get(locale);
    if (bundle == null || !useCache) {
      try {
        URL messagesDir = new File(this.configDir, "messages").toURI().toURL();
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {messagesDir});
        bundle = ResourceBundle.getBundle("messages", locale, urlClassLoader);
        localeBundles.put(locale, bundle);
      } catch (MalformedURLException e) {
        logger.error("Something is wrong with the configuration directory", e);
        throw new ConfigException(e);
      } catch (MissingResourceException e) {
        logger.info("Missing locale bundle: " + locale);
        try {
          bundle = new PropertyResourceBundle(new ByteArrayInputStream(new byte[0]));
        } catch (IOException e1) {
          // ignore, not an actual IO operation
        }
      }
    }
    return bundle;
  }

  private String localize(String template, Locale locale) throws ConfigException {
    Pattern patt = Pattern.compile("\\$\\{([\\w.]*)\\}");
    Matcher m = patt.matcher(template);
    StringBuffer sb = new StringBuffer(template.length());
    ResourceBundle messages = getMessages(locale);
    while (m.find()) {
      String text;
      try {
        text = messages.getString(m.group(1));
        m.appendReplacement(sb, text);
      } catch (MissingResourceException e) {
        // do not replace
      }
    }
    m.appendTail(sb);
    return sb.toString();
  }

  @Override
  public String[] getPropertyAsArray(String property) {
    try {
      return getProperty(property).split(",");
    } catch (ConfigException e) {
      return null;
    }
  }

  @Override
  public String getDefaultLang() {
    try {
      return getProperty(PROPERTY_DEFAULT_LANG);
    } catch (ConfigException e) {
      Map<String, String>[] langs = getLanguages();
      if (langs != null && langs.length > 0) {
        return langs[0].get("code");
      } else {
        return "en";
      }
    }
  }

  private String getProperty(String propertyName) throws ConfigException {
    Properties props = getProperties();
    String value = props.getProperty(propertyName);
    if (value != null) {
      return value;
    } else {
      throw new ConfigException(
          "No \"" + propertyName + "\" property in configuration. Conf folder: "
              + configDir.getAbsolutePath() + ". Contents: " + props.keySet().size());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public PluginDescriptor[] getPluginConfig(Locale locale) {
    // Get a map: name -> cloned descriptor
    Map<String, PluginDescriptor> namePluginDescriptor = new HashMap<String, PluginDescriptor>();
    for (PluginDescriptor pluginDescriptor : this.plugins) {
      PluginDescriptor clonedDescriptor = pluginDescriptor.cloneDescriptor();
      namePluginDescriptor.put(clonedDescriptor.getName(), clonedDescriptor);
    }

    PortalRequestConfiguration requestConfig = new PortalRequestConfigurationImpl(locale);

    // Get the providers configuration and merge it
    List<ModuleConfigurationProvider> providers = (List<ModuleConfigurationProvider>) this.context
        .getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS);
    for (ModuleConfigurationProvider provider : providers) {

      Map<String, JSONObject> providerConfiguration = cachedConfigurations.get(provider);
      if (providerConfiguration == null || !useCache || !provider.canBeCached()) {
        try {
          providerConfiguration = provider.getPluginConfig(requestConfig, request);
          cachedConfigurations.put(provider, providerConfiguration);
        } catch (IOException e) {
          logger.info("Provider failed to contribute configuration: " + provider.getClass());
        }
      }

      if (providerConfiguration == null) {
        continue;
      }

      // Merge the configuration in the result
      for (String pluginName : providerConfiguration.keySet()) {
        JSONObject pluginConf = providerConfiguration.get(pluginName);
        PluginDescriptor pluginDescriptor = namePluginDescriptor.get(pluginName);
        if (pluginDescriptor == null) {
          logger.warn("Configuration has been defined for a non-existing plugin: " + pluginName);
        } else {
          pluginDescriptor.setConfiguration(pluginConf);
        }
      }
    }

    // Get only enabled plugins
    List<PluginDescriptor> enabled = new ArrayList<>();
    for (PluginDescriptor plugin : namePluginDescriptor.values()) {
      if (plugin.isEnabled()) {
        enabled.add(plugin);
      }
    }

    return enabled.toArray(new PluginDescriptor[enabled.size()]);
  }

  @Override
  public File getNoJavaPluginRoot() {
    return new File(getDir(), "plugins");
  }

  private class PortalRequestConfigurationImpl implements PortalRequestConfiguration {
    private Locale locale;

    public PortalRequestConfigurationImpl(Locale locale) {
      this.locale = locale;
    }

    @Override
    public String localize(String template) {
      return DefaultConfig.this.localize(template, locale);
    }

    @Override
    public File getConfigDir() {
      return DefaultConfig.this.getDir();
    }

    @Override
    public boolean usingCache() {
      return DefaultConfig.this.useCache;
    }
  }
}
