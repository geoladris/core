package org.fao.unredd.portal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;

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

	private ConfigFolder folder;
	private boolean useCache;
	private HashMap<Locale, ResourceBundle> localeBundles = new HashMap<Locale, ResourceBundle>();
	private Map<ModuleConfigurationProvider, Map<String, JSONObject>> cachedConfigurations = new HashMap<ModuleConfigurationProvider, Map<String, JSONObject>>();

	private ArrayList<ModuleConfigurationProvider> moduleConfigurationProviders = new ArrayList<ModuleConfigurationProvider>();
	private DefaultConfProvider defaultConfProvider;
	private Set<PluginDescriptor> plugins;

	public DefaultConfig(ConfigFolder folder, Set<PluginDescriptor> plugins,
			DefaultConfProvider defaultConfProvider, boolean useCache) {
		this.folder = folder;
		this.plugins = plugins;
		this.defaultConfProvider = defaultConfProvider;
		this.useCache = useCache;
	}

	@Override
	public File getDir() {
		return folder.getFilePath();
	}

	@Override
	public synchronized Properties getProperties() {
		if (properties == null || !useCache) {
			properties = folder.getProperties();
		}
		return properties;
	}

	/**
	 * Returns an array of <code>Map&lt;String, String&gt;</code>. For each
	 * element of the array, a {@link Map} is returned containing two
	 * keys/values: <code>code</code> (for language code) and <code>name</code>
	 * (for language name).
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
		} catch (ConfigurationException e) {
			return null;
		}
	}

	@Override
	public ResourceBundle getMessages(Locale locale)
			throws ConfigurationException {
		ResourceBundle bundle = localeBundles.get(locale);
		if (bundle == null || !useCache) {
			try {
				bundle = folder.getMessages(locale);
				localeBundles.put(locale, bundle);
			} catch (MissingResourceException e) {
				logger.info("Missing locale bundle: " + locale);
				try {
					bundle = new PropertyResourceBundle(
							new ByteArrayInputStream(new byte[0]));
				} catch (IOException e1) {
					// ignore, not an actual IO operation
				}
			}
		}
		return bundle;
	}

	private String localize(String template, Locale locale)
			throws ConfigurationException {
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
		} catch (ConfigurationException e) {
			return null;
		}
	}

	@Override
	public String getDefaultLang() {
		try {
			return getProperty(PROPERTY_DEFAULT_LANG);
		} catch (ConfigurationException e) {
			Map<String, String>[] langs = getLanguages();
			if (langs != null && langs.length > 0) {
				return langs[0].get("code");
			} else {
				return "en";
			}
		}
	}

	private String getProperty(String propertyName)
			throws ConfigurationException {
		Properties props = getProperties();
		String value = props.getProperty(propertyName);
		if (value != null) {
			return value;
		} else {
			throw new ConfigurationException("No \"" + propertyName
					+ "\" property in configuration. Conf folder: "
					+ folder.getFilePath().getAbsolutePath() + ". Contents: "
					+ props.keySet().size());
		}
	}

	@Override
	public PluginDescriptors getPluginConfig(Locale locale,
			HttpServletRequest request) {
		PluginDescriptors ret = new PluginDescriptors(plugins);

		/*
		 * Get the providers configuration and merge it, it includes public and
		 * role configuration
		 */
		for (ModuleConfigurationProvider provider : moduleConfigurationProviders) {
			Map<String, JSONObject> providerConfiguration = cachedConfigurations
					.get(provider);
			if (providerConfiguration == null || !useCache
					|| !provider.canBeCached()) {
				try {
					providerConfiguration = provider
							.getPluginConfig(
									new PortalConfigurationContextImpl(locale),
									request);
					cachedConfigurations.put(provider, providerConfiguration);
				} catch (IOException e) {
					logger.info("Provider failed to contribute configuration: "
							+ provider.getClass());
				}
			}

			if (providerConfiguration == null) {
				continue;
			}

			// Merge the configuration in the result
			for (String pluginName : providerConfiguration.keySet()) {
				JSONObject pluginConf = providerConfiguration.get(pluginName);
				ret.merge(pluginName, pluginConf);
			}

		}

		return ret;
	}

	@Override
	public void addModuleConfigurationProvider(
			ModuleConfigurationProvider provider) {
		moduleConfigurationProviders.add(provider);
	}

	@Override
	public PluginDescriptor getPlugin(String name) {
		for (PluginDescriptor plugin : this.plugins) {
			if (name.equals(plugin.getName())) {
				return plugin;
			}
		}
		return null;
	}

	public boolean hasModuleConfigurationProvider(
			Class<? extends ModuleConfigurationProvider> clazz) {
		for (ModuleConfigurationProvider provider : this.moduleConfigurationProviders) {
			if (clazz.isInstance(provider)) {
				return true;
			}
		}

		return clazz.isInstance(this.defaultConfProvider);
	}

	private class PortalConfigurationContextImpl implements
			PortalRequestConfiguration {

		private Locale locale;

		public PortalConfigurationContextImpl(Locale locale) {
			this.locale = locale;
		}

		@Override
		public String localize(String template) {
			return DefaultConfig.this.localize(template, locale);
		}

		@Override
		public File getConfigurationDirectory() {
			return getDir();
		}

		@Override
		public boolean usingCache() {
			return useCache;
		}
	}

	@Override
	public File getNoJavaPluginRoot() {
		return new File(getDir(), "plugins");
	}
}