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
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.geoladris.Plugin;

import net.sf.json.JSONObject;

public class ConfigImpl implements Config {
  private static final Logger logger = Logger.getLogger(ConfigImpl.class);

  private static final String PROPERTY_DEFAULT_LANG = "languages.default";

  private File configDir;
  private Set<Plugin> plugins;
  private boolean useCache;
  private List<PluginConfigProvider> configProviders;
  private Locale currentLocale;

  private Map<PluginConfigProvider, Map<String, JSONObject>> cachedConfigurations =
      new HashMap<PluginConfigProvider, Map<String, JSONObject>>();
  private Map<Locale, ResourceBundle> localeBundles = new HashMap<Locale, ResourceBundle>();
  private Properties properties;


  public ConfigImpl(File configDir, List<PluginConfigProvider> configProviders,
      Set<Plugin> plugins, boolean useCache, int cacheTimeout) {
    this.configDir = configDir;
    this.plugins = plugins;
    this.useCache = useCache;
    this.configProviders = configProviders;

    if (cacheTimeout > 0) {
      int timeoutMillis = cacheTimeout * 1000;
      new Timer(true).schedule(new TimerTask() {
        @Override
        public void run() {
          cachedConfigurations.clear();
          localeBundles.clear();
          properties = null;
        }
      }, timeoutMillis, timeoutMillis);
    }
  }

  @Override
  public File getDir() {
    return this.configDir;
  }

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
      bundle = getResourceBundle(locale);
    }
    return bundle;
  }

  private ResourceBundle getResourceBundle(Locale locale) {
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

  @Override
  public Properties getProperties() {
    if (this.properties == null || !this.useCache) {
      this.properties = readProperties();
    }
    return properties;
  }

  private Properties readProperties() {
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
  public String[] getPropertyAsArray(String property) {
    try {
      return getProperty(property).split(",");
    } catch (ConfigException e) {
      return null;
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

  @Override
  public Plugin[] getPluginConfig(Locale locale, HttpServletRequest request) {
    this.currentLocale = locale;

    // Get a map: name -> cloned plugin
    Map<String, Plugin> namePluginMap = new HashMap<String, Plugin>();
    for (Plugin plugin : this.plugins) {
      Plugin clone = plugin.clonePlugin();
      namePluginMap.put(clone.getName(), clone);
    }

    // PortalRequestConfigurationImpl requestConfig = new PortalRequestConfigurationImpl(locale);
    Map<String, JSONObject> pluginConfig = new HashMap<>();

    // Get the providers configuration and merge it
    for (PluginConfigProvider provider : this.configProviders) {
      pluginConfig.clear();
      for (Plugin plugin : namePluginMap.values()) {
        JSONObject config = new JSONObject();
        JSONObject c = plugin.getConfiguration();
        for (Object key : c.keySet()) {
          String unqualified = key.toString().substring(plugin.getName().length() + 1);
          config.element(unqualified, c.get(key));
        }
        pluginConfig.put(plugin.getName(), config);
      }

      Map<String, JSONObject> providerConfig = cachedConfigurations.get(provider);
      if (providerConfig == null || !useCache || !provider.canBeCached()) {
        try {
          providerConfig = provider.getPluginConfig(this, pluginConfig, request);
          cachedConfigurations.put(provider, providerConfig);
        } catch (IOException e) {
          logger.info("Provider failed to contribute configuration: " + provider.getClass());
        }
      }

      if (providerConfig == null) {
        continue;
      }

      // Merge the configuration in the result
      for (String pluginName : providerConfig.keySet()) {
        JSONObject pluginConf = providerConfig.get(pluginName);
        Plugin plugin = namePluginMap.get(pluginName);
        if (plugin == null) {
          logger.warn("Configuration has been defined for a non-existing plugin: " + pluginName);
        } else {
          plugin.setConfiguration(pluginConf);
        }
      }
    }

    // Get only enabled plugins
    List<Plugin> enabled = new ArrayList<>();
    for (Plugin plugin : namePluginMap.values()) {
      if (plugin.isEnabled()) {
        enabled.add(plugin);
      }
    }

    return enabled.toArray(new Plugin[enabled.size()]);
  }

  @Override
  public File getNoJavaPluginRoot() {
    return new File(getDir(), "plugins");
  }

  @Override
  public void setPlugins(Set<Plugin> plugins) {
    this.plugins = plugins;
  }

  @Override
  public void addPluginConfigProvider(PluginConfigProvider provider) {
    this.configProviders.add(provider);
  }

  @Override
  public List<PluginConfigProvider> getPluginConfigProviders() {
    return this.configProviders;
  }

  @Override
  public String localize(String template) {
    Pattern patt = Pattern.compile("\\$\\{([\\w.]*)\\}");
    Matcher m = patt.matcher(template);
    StringBuffer sb = new StringBuffer(template.length());
    ResourceBundle messages = getMessages(this.currentLocale);
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
}
