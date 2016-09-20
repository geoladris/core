package org.geoladris.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.geoladris.PluginDescriptor;
import org.geoladris.PluginDescriptorFileReader;
import org.geoladris.PortalRequestConfiguration;
import org.geoladris.config.Config;
import org.geoladris.config.ConfigFolder;
import org.geoladris.config.DefaultConfig;
import org.geoladris.config.ModuleConfigurationProvider;
import org.geoladris.config.PluginDescriptors;
import org.junit.Test;

public class DefaultConfigTest {

	@Test
	public void testConfigurationProvidersMerge() throws Exception {
		Set<PluginDescriptor> plugins = new HashSet<>();
		PluginDescriptor plugin1 = new PluginDescriptor();
		plugin1.setInstallInRoot(true);
		plugin1.setName("1");
		plugins.add(plugin1);

		JSONObject conf1 = JSONObject
				.fromObject("{ module : { a : 1, b : 2 }}");
		JSONObject conf2 = JSONObject
				.fromObject("{ module : { a : 10, c : 3 }}");

		ModuleConfigurationProvider provider1 = mock(ModuleConfigurationProvider.class);
		when(
				provider1.getPluginConfig(
						any(PortalRequestConfiguration.class),
						any(HttpServletRequest.class))).thenReturn(
				Collections.singletonMap("1", conf1));
		ModuleConfigurationProvider provider2 = mock(ModuleConfigurationProvider.class);
		when(
				provider2.getPluginConfig(
						any(PortalRequestConfiguration.class),
						any(HttpServletRequest.class))).thenReturn(
				Collections.singletonMap("1", conf2));

		Config config = new DefaultConfig(mock(ConfigFolder.class), plugins,
				null, false);

		config.addModuleConfigurationProvider(provider1);
		config.addModuleConfigurationProvider(provider2);

		PluginDescriptors c = config.getPluginConfig(Locale.getDefault(),
				mock(HttpServletRequest.class));
		JSONObject pluginConf = c.get("1").getConfiguration()
				.getJSONObject("module");

		assertTrue(pluginConf.has("a") && pluginConf.has("b")
				&& pluginConf.has("c"));
		assertEquals(3, pluginConf.get("c"));
		assertEquals(2, pluginConf.get("b"));
		// Providers should be applied in order
		assertEquals(10, pluginConf.get("a"));
	}

	@Test
	public void testCache() throws Exception {
		String defaultLang = "es";
		Locale locale = new Locale(defaultLang);
		ResourceBundle resourceBundle = mock(ResourceBundle.class);
		Properties firstProperties = new Properties();
		firstProperties.put("languages", "{\"es\": \"Espa\u00f1ol\"}");
		firstProperties.put("languages.default", defaultLang);
		Config config = buildConfigReadOnceAndChangeFolderConfig(true,
				defaultLang, locale, resourceBundle, firstProperties);

		// Check we still have the same values
		assertTrue(config.getDefaultLang().equals(defaultLang));
		assertTrue(config.getLanguages()[0].get("code").equals("es"));
		assertTrue(config.getMessages(locale) == resourceBundle);
		assertTrue(config.getProperties() == firstProperties);
	}

	@Test
	public void testNoCache() throws Exception {
		String defaultLang = "es";
		Locale locale = new Locale(defaultLang);
		ResourceBundle resourceBundle = mock(ResourceBundle.class);
		Properties firstProperties = new Properties();
		firstProperties.put("languages", "{\"es\": \"Espa\u00f1ol\"}");
		firstProperties.put("languages.default", defaultLang);
		Config config = buildConfigReadOnceAndChangeFolderConfig(false,
				defaultLang, locale, resourceBundle, firstProperties);

		// Check we still have the same values
		assertFalse(config.getDefaultLang().equals(defaultLang));
		assertFalse(config.getLanguages()[0].get("code").equals("es"));
		assertFalse(config.getMessages(locale) == resourceBundle);
		assertFalse(config.getProperties() == firstProperties);
	}

	private Config buildConfigReadOnceAndChangeFolderConfig(boolean useCache,
			String defaultLang, Locale locale, ResourceBundle resourceBundle,
			Properties firstProperties) {
		ConfigFolder folder = mock(ConfigFolder.class);
		Config config = new DefaultConfig(folder, null, null, useCache);

		when(folder.getMessages(locale)).thenReturn(resourceBundle);
		when(folder.getProperties()).thenReturn(firstProperties);

		assertTrue(config.getDefaultLang().equals(defaultLang));
		assertTrue(config.getLanguages()[0].get("code").equals("es"));
		assertTrue(config.getMessages(locale) == resourceBundle);
		assertTrue(config.getProperties() == firstProperties);

		Properties secondProperties = new Properties();
		secondProperties.put("languages", "{\"fr\": \"Frances\"}");
		secondProperties.put("languages.default", "fr");
		ResourceBundle secondResourceBundle = mock(ResourceBundle.class);
		when(folder.getMessages(locale)).thenReturn(secondResourceBundle);
		when(folder.getProperties()).thenReturn(secondProperties);
		return config;
	}

	@Test
	public void testPluginConfigurationCached() throws Exception {
		readPluginConfigurationTwice(true, true, 1);
	}

	@Test
	public void testPluginConfigurationCacheIgnoredIfProviderCannotBeCached()
			throws Exception {
		readPluginConfigurationTwice(true, false, 2);
	}

	@Test
	public void testPluginConfigurationCacheIgnoredIfCacheDisabled()
			throws Exception {
		readPluginConfigurationTwice(false, true, 2);
	}

	private void readPluginConfigurationTwice(boolean useCache,
			boolean canBeCached, int numCalls) throws IOException {
		// Install configuration provider
		ModuleConfigurationProvider configurationProvider = mock(ModuleConfigurationProvider.class);
		when(configurationProvider.canBeCached()).thenReturn(canBeCached);

		Config config = new DefaultConfig(mock(ConfigFolder.class),
				Collections.<PluginDescriptor> emptySet(), null, useCache);
		config.addModuleConfigurationProvider(configurationProvider);

		// Call twice
		config.getPluginConfig(Locale.getDefault(),
				mock(HttpServletRequest.class));
		config.getPluginConfig(Locale.getDefault(),
				mock(HttpServletRequest.class));

		// Check num calls
		verify(configurationProvider, times(numCalls)).getPluginConfig(
				any(PortalRequestConfiguration.class),
				any(HttpServletRequest.class));
	}

	@Test
	public void testNoConfigurationFolder() {
		Config config = new DefaultConfig(new ConfigFolder("doesnotexist",
				"doesnotexist"), Collections.<PluginDescriptor> emptySet(),
				null, false);
		assertNotNull(config.getDir());
		assertNotNull(config.getPluginConfig(Locale.getDefault(),
				mock(HttpServletRequest.class)));
		assertNotNull(config.getProperties());
		assertNotNull(config.getMessages(Locale.getDefault()));
		assertNotNull(config.getDefaultLang());
	}

	@Test
	public void testFailingConfigurationProvider() throws Exception {
		Config config = new DefaultConfig(new ConfigFolder("doesnotexist",
				"doesnotexist"), Collections.<PluginDescriptor> emptySet(),
				null, false);
		ModuleConfigurationProvider provider = mock(ModuleConfigurationProvider.class);
		when(
				provider.getPluginConfig(any(PortalRequestConfiguration.class),
						any(HttpServletRequest.class))).thenThrow(
				new IOException("mock"));
		config.addModuleConfigurationProvider(provider);
		assertNotNull(config.getPluginConfig(Locale.getDefault(),
				mock(HttpServletRequest.class)));
	}

	@Test
	public void testMergeDoesNotAffectDefaultPluginConfiguration()
			throws IOException {
		Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
		PluginDescriptor pluginDescriptor = new PluginDescriptor();
		new PluginDescriptorFileReader("{default-conf:{m1:true}}", true, "p1")
				.fillPluginDescriptor(pluginDescriptor);
		plugins.add(pluginDescriptor);
		Config config = new DefaultConfig(new ConfigFolder("doesnotexist",
				"doesnotexist"), plugins, null, false);

		Map<String, JSONObject> mergingConfiguration1 = new HashMap<String, JSONObject>();
		mergingConfiguration1.put("p1", JSONObject.fromObject("{m2:true}"));
		Map<String, JSONObject> mergingConfiguration2 = new HashMap<String, JSONObject>();
		mergingConfiguration2.put("p1", JSONObject.fromObject("{}"));
		ModuleConfigurationProvider provider = mock(ModuleConfigurationProvider.class);
		when(
				provider.getPluginConfig(any(PortalRequestConfiguration.class),
						any(HttpServletRequest.class))).thenReturn(
				mergingConfiguration1).thenReturn(mergingConfiguration2);
		config.addModuleConfigurationProvider(provider);

		JSONObject configuration = config.getPluginConfig(Locale.ROOT,
				mock(HttpServletRequest.class)).getEnabled()[0]
				.getConfiguration();
		assertEquals(2, configuration.keySet().size());

		configuration = config.getPluginConfig(Locale.ROOT,
				mock(HttpServletRequest.class)).getEnabled()[0]
				.getConfiguration();
		assertEquals(1, configuration.keySet().size());
	}
}
