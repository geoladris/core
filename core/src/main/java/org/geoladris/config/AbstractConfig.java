package org.geoladris.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.geoladris.PluginDescriptor;

import net.sf.json.JSONObject;

public abstract class AbstractConfig implements Config {
  private static final Logger logger = Logger.getLogger(AbstractConfig.class);

  private static final String PROPERTY_DEFAULT_LANG = "languages.default";

  private File configDir;
  private Set<PluginDescriptor> plugins;
  private boolean useCache;
  private List<ModuleConfigurationProvider> configProviders;

  private Map<ModuleConfigurationProvider, Map<String, JSONObject>> cachedConfigurations =
      new HashMap<ModuleConfigurationProvider, Map<String, JSONObject>>();
  private Map<Locale, ResourceBundle> localeBundles = new HashMap<Locale, ResourceBundle>();
  private Properties properties;


  public AbstractConfig(File configDir, List<ModuleConfigurationProvider> configProviders,
      Set<PluginDescriptor> plugins, boolean useCache, int cacheTimeout) {
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

  protected abstract ResourceBundle getResourceBundle(Locale locale) throws ConfigException;

  @Override
  public Properties getProperties() {
    if (this.properties == null || !this.useCache) {
      this.properties = readProperties();
    }
    return properties;
  }

  protected abstract Properties readProperties();

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
  public PluginDescriptor[] getPluginConfig(Locale locale, HttpServletRequest request) {
    // Get a map: name -> cloned descriptor
    Map<String, PluginDescriptor> namePluginDescriptor = new HashMap<String, PluginDescriptor>();
    for (PluginDescriptor pluginDescriptor : this.plugins) {
      PluginDescriptor clonedDescriptor = pluginDescriptor.cloneDescriptor();
      namePluginDescriptor.put(clonedDescriptor.getName(), clonedDescriptor);
    }

    PortalRequestConfigurationImpl requestConfig = new PortalRequestConfigurationImpl(locale);

    // Get the providers configuration and merge it
    List<ModuleConfigurationProvider> providers =
        (List<ModuleConfigurationProvider>) this.configProviders;
    for (ModuleConfigurationProvider provider : providers) {
      requestConfig.currentConfig.clear();
      for (PluginDescriptor p : namePluginDescriptor.values()) {
        requestConfig.currentConfig.put(p.getName(),
            JSONObject.fromObject(p.getConfiguration().toString()));
      }

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

  @Override
  public void setPlugins(Set<PluginDescriptor> plugins) {
    this.plugins = plugins;
  }

  @Override
  public void addModuleConfigurationProvider(ModuleConfigurationProvider provider) {
    this.configProviders.add(provider);
  }

  @Override
  public List<ModuleConfigurationProvider> getModuleConfigurationProviders() {
    return this.configProviders;
  }

  private class PortalRequestConfigurationImpl implements PortalRequestConfiguration {
    private Locale locale;
    private Map<String, JSONObject> currentConfig;

    public PortalRequestConfigurationImpl(Locale locale) {
      this.locale = locale;
      this.currentConfig = new HashMap<>();
    }

    @Override
    public String localize(String template) {
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
    public File getConfigDir() {
      return AbstractConfig.this.configDir;
    }

    @Override
    public Map<String, JSONObject> getCurrentConfiguration() {
      return this.currentConfig;
    }
  }
}
