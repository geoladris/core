
package org.fao.unredd.portal;

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

import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;
import org.junit.Test;

import net.sf.json.JSONObject;

public class DefaultConfigTest {

	@Test
	public void testConfigurationProvidersMerge() throws Exception {
		Set<PluginDescriptor> plugins = new HashSet<>();
		PluginDescriptor plugin1 = new PluginDescriptor();
		plugin1.setName("1");
		plugins.add(plugin1);

		JSONObject conf1 = JSONObject
				.fromObject("{ module : { a : 1, b : 2 }}");
		JSONObject conf2 = JSONObject
				.fromObject("{ module : { a : 10, c : 3 }}");

		ModuleConfigurationProvider provider1 = mock(
				ModuleConfigurationProvider.class);
		when(provider1.getPluginConfig(any(PortalRequestConfiguration.class),
				any(HttpServletRequest.class)))
						.thenReturn(Collections.singletonMap(plugin1, conf1));
		ModuleConfigurationProvider provider2 = mock(
				ModuleConfigurationProvider.class);
		when(provider2.getPluginConfig(any(PortalRequestConfiguration.class),
				any(HttpServletRequest.class)))
						.thenReturn(Collections.singletonMap(plugin1, conf2));

		Config config = new DefaultConfig(mock(ConfigFolder.class), plugins,
				null, false, true);

		config.addModuleConfigurationProvider(provider1);
		config.addModuleConfigurationProvider(provider2);

		Map<PluginDescriptor, JSONObject> c = config.getPluginConfig(
				Locale.getDefault(), mock(HttpServletRequest.class));
		JSONObject pluginConf = c.get(plugin1).getJSONObject("module");

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
		Config config = new DefaultConfig(folder, null, null, useCache, false);

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
		ModuleConfigurationProvider configurationProvider = mock(
				ModuleConfigurationProvider.class);
		when(configurationProvider.canBeCached()).thenReturn(canBeCached);

		Config config = new DefaultConfig(mock(ConfigFolder.class), null, null,
				useCache, false);
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
		Config config = new DefaultConfig(
				new ConfigFolder("doesnotexist", "doesnotexist"), null, null,
				false, false);
		assertNotNull(config.getDir());
		assertNotNull(config.getPluginConfig(Locale.getDefault(),
				mock(HttpServletRequest.class)));
		assertNotNull(config.getProperties());
		assertNotNull(config.getMessages(Locale.getDefault()));
		assertNotNull(config.getDefaultLang());
	}

	@Test
	public void testFailingConfigurationProvider() throws Exception {
		Config config = new DefaultConfig(
				new ConfigFolder("doesnotexist", "doesnotexist"), null, null,
				false, false);
		ModuleConfigurationProvider provider = mock(
				ModuleConfigurationProvider.class);
		when(provider.getPluginConfig(any(PortalRequestConfiguration.class),
				any(HttpServletRequest.class)))
						.thenThrow(new IOException("mock"));
		config.addModuleConfigurationProvider(provider);
		assertNotNull(config.getPluginConfig(Locale.getDefault(),
				mock(HttpServletRequest.class)));
	}

	@Test
	public void doesNotReturnDisabledPlugins() throws Exception {
		PluginDescriptor p1 = new PluginDescriptor();
		PluginDescriptor p2 = new PluginDescriptor();
		p1.setName("p1");
		p1.setName("p2");

		Config config = new DefaultConfig(
				new ConfigFolder("doesnotexist", "doesnotexist"), null, null,
				false, false);

		Map<PluginDescriptor, JSONObject> conf = new HashMap<>();
		conf.put(p1, JSONObject.fromObject("{_enabled : false, "
				+ "m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}"));
		conf.put(p2, JSONObject
				.fromObject("{_enabled : true, m3 : { a : 1, b : 2}}"));

		ModuleConfigurationProvider p = mock(ModuleConfigurationProvider.class);
		when(p.getPluginConfig(any(PortalRequestConfiguration.class),
				any(HttpServletRequest.class))).thenReturn(conf);
		config.addModuleConfigurationProvider(p);

		Map<PluginDescriptor, JSONObject> pluginConfs = config
				.getPluginConfig(Locale.ROOT, mock(HttpServletRequest.class));

		assertEquals(1, pluginConfs.size());
		JSONObject pluginConf = pluginConfs.get(p2);
		assertEquals(1, pluginConf.keySet().size());
		assertEquals("m3", pluginConf.keySet().iterator().next());
	}

	@Test
	public void pluginsDisabledByDefault() throws Exception {
		PluginDescriptor p1 = new PluginDescriptor();

		ModuleConfigurationProvider p = mockConfProvider(p1,
				"{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}");
		Config config = new DefaultConfig(
				new ConfigFolder("doesnotexist", "doesnotexist"), null, null,
				false, false);
		config.addModuleConfigurationProvider(p);

		Map<PluginDescriptor, JSONObject> pluginConf = config
				.getPluginConfig(Locale.ROOT, mock(HttpServletRequest.class));
		assertEquals(0, pluginConf.size());
	}

	@Test
	public void ignoresEnabledPseudomoduleIfAllEnabled() throws Exception {
		PluginDescriptor p1 = new PluginDescriptor();

		ModuleConfigurationProvider p = mockConfProvider(p1,
				"{ _enabled: false, m1 : { a : 1, b : 2}}");
		Config config = new DefaultConfig(
				new ConfigFolder("doesnotexist", "doesnotexist"), null, null,
				false, true);
		config.addModuleConfigurationProvider(p);

		Map<PluginDescriptor, JSONObject> pluginConf = config
				.getPluginConfig(Locale.ROOT, mock(HttpServletRequest.class));
		assertEquals(1, pluginConf.size());
		assertEquals(1, pluginConf.get(p1).getJSONObject("m1").get("a"));
		assertEquals(2, pluginConf.get(p1).getJSONObject("m1").get("b"));
	}

	@Test
	public void mergesDefaultConfIfSpecified() throws Exception {
		PluginDescriptor plugin = new PluginDescriptor();

		DefaultConfProvider defaultConfProvider = mockDefaultConfProvider(
				plugin, "{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}");
		ModuleConfigurationProvider provider = mockConfProvider(plugin,
				"{ _override : false, _enabled : true, m1 : { a : 10}}");

		Config config = new DefaultConfig(
				new ConfigFolder("doesnotexist", "doesnotexist"), null,
				defaultConfProvider, false, false);
		config.addModuleConfigurationProvider(provider);

		Map<PluginDescriptor, JSONObject> pluginConfs = config
				.getPluginConfig(Locale.ROOT, mock(HttpServletRequest.class));

		assertEquals(1, pluginConfs.size());
		JSONObject pluginConf = pluginConfs.get(plugin);
		assertEquals(2, pluginConf.keySet().size());
		assertEquals(10, pluginConf.getJSONObject("m1").get("a"));
		assertEquals(2, pluginConf.getJSONObject("m1").get("b"));
		assertEquals(1, pluginConf.getJSONObject("m2").get("c"));
		assertEquals(2, pluginConf.getJSONObject("m2").get("d"));
	}

	@Test
	public void overridesDefaultConfIfSpecified() throws Exception {
		PluginDescriptor plugin = new PluginDescriptor();

		DefaultConfProvider defaultConfProvider = mockDefaultConfProvider(
				plugin, "{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}");
		ModuleConfigurationProvider provider = mockConfProvider(plugin,
				"{ _override : true, _enabled : true, m1 : { a : 10}}");

		Config config = new DefaultConfig(
				new ConfigFolder("doesnotexist", "doesnotexist"), null,
				defaultConfProvider, false, false);
		config.addModuleConfigurationProvider(provider);

		Map<PluginDescriptor, JSONObject> pluginConfs = config
				.getPluginConfig(Locale.ROOT, mock(HttpServletRequest.class));

		assertEquals(1, pluginConfs.size());
		JSONObject pluginConf = pluginConfs.get(plugin);
		assertEquals(1, pluginConf.keySet().size());
		assertEquals(10, pluginConf.getJSONObject("m1").get("a"));
	}

	@Test
	public void defaultConfigurationOverridenByDefault() throws Exception {
		PluginDescriptor plugin = new PluginDescriptor();

		DefaultConfProvider defaultConfProvider = mockDefaultConfProvider(
				plugin, "{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}");
		ModuleConfigurationProvider provider = mockConfProvider(plugin,
				"{ _enabled : true, m1 : { a : 10}}");

		Config config = new DefaultConfig(
				new ConfigFolder("doesnotexist", "doesnotexist"), null,
				defaultConfProvider, false, false);
		config.addModuleConfigurationProvider(provider);

		Map<PluginDescriptor, JSONObject> pluginConfs = config
				.getPluginConfig(Locale.ROOT, mock(HttpServletRequest.class));

		assertEquals(1, pluginConfs.size());
		JSONObject pluginConf = pluginConfs.get(plugin);
		assertEquals(1, pluginConf.keySet().size());
		assertEquals(10, pluginConf.getJSONObject("m1").get("a"));
	}

	@Test
	public void doesNotReturnEnabledOverridePseudomodules() throws Exception {
		PluginDescriptor plugin = new PluginDescriptor();

		ModuleConfigurationProvider provider = mockConfProvider(plugin,
				"{ _override : false, _enabled : true, m1 : { a : 10 }}");

		Config config = new DefaultConfig(
				new ConfigFolder("doesnotexist", "doesnotexist"), null, null,
				false, false);
		config.addModuleConfigurationProvider(provider);

		Map<PluginDescriptor, JSONObject> pluginConfs = config
				.getPluginConfig(Locale.ROOT, mock(HttpServletRequest.class));

		assertEquals(1, pluginConfs.size());
		JSONObject pluginConf = pluginConfs.get(plugin);
		assertFalse(pluginConf.has("_enabled"));
		assertFalse(pluginConf.has("_override"));
	}

	@Test
	public void includesPluginsFromDefaultConfIfAllEnabled() throws Exception {
		PluginDescriptor p1 = new PluginDescriptor();
		p1.setName("plugin1");
		PluginDescriptor p2 = new PluginDescriptor();
		p2.setName("plugin2");

		DefaultConfProvider defaultConfProvider = mockDefaultConfProvider(p2,
				"{ m2 : { b : 20 }}");
		ModuleConfigurationProvider provider = mockConfProvider(p1,
				"{ m1 : { a : 10 }}");

		Config config = new DefaultConfig(
				new ConfigFolder("doesnotexist", "doesnotexist"), null,
				defaultConfProvider, false, true);
		config.addModuleConfigurationProvider(provider);

		Map<PluginDescriptor, JSONObject> pluginConfs = config
				.getPluginConfig(Locale.ROOT, mock(HttpServletRequest.class));

		assertEquals(2, pluginConfs.size());
		assertEquals(10, pluginConfs.get(p1).getJSONObject("m1").get("a"));
		assertEquals(20, pluginConfs.get(p2).getJSONObject("m2").get("b"));
	}

	private DefaultConfProvider mockDefaultConfProvider(PluginDescriptor plugin,
			String conf) throws IOException {
		return mockConfProvider(plugin, conf, DefaultConfProvider.class);
	}

	private ModuleConfigurationProvider mockConfProvider(
			PluginDescriptor plugin, String conf) throws IOException {
		return mockConfProvider(plugin, conf,
				ModuleConfigurationProvider.class);
	}

	private <T extends ModuleConfigurationProvider> T mockConfProvider(
			PluginDescriptor plugin, String conf, Class<T> clazz)
			throws IOException {
		T p = mock(clazz);
		Map<PluginDescriptor, JSONObject> defaultConf = new HashMap<>();
		defaultConf.put(plugin, JSONObject.fromObject(conf));
		when(p.getPluginConfig(any(PortalRequestConfiguration.class),
				any(HttpServletRequest.class))).thenReturn(defaultConf);
		return p;
	}

}
