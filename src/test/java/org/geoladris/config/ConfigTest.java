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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.geoladris.Plugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.sf.json.JSONObject;

public class ConfigTest {
  @Mock
  private HttpServletRequest request;
  @Mock
  private ServletContext context;
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConfigurationProvidersMerge() throws Exception {
    Set<Plugin> plugins = new HashSet<>();
    Plugin plugin1 = new Plugin("1", true);
    plugins.add(plugin1);

    JSONObject conf1 = JSONObject.fromObject("{ module : { a : 1, b : 2 }}");
    JSONObject conf2 = JSONObject.fromObject("{ module : { a : 10, c : 3 }}");

    PluginConfigProvider p1 = mock(PluginConfigProvider.class);
    when(p1.getPluginConfig(any(Config.class), any(Map.class), any(HttpServletRequest.class)))
        .thenReturn(Collections.singletonMap("1", conf1));
    PluginConfigProvider p2 = mock(PluginConfigProvider.class);
    when(p2.getPluginConfig(any(Config.class), any(Map.class), any(HttpServletRequest.class)))
        .thenReturn(Collections.singletonMap("1", conf2));

    Config config = new ConfigImpl(folder.getRoot(), Arrays.asList(p1, p2), plugins, false, -1);

    Plugin[] c = config.getPluginConfig(Locale.getDefault(), request);

    JSONObject pluginConf = c[0].getConfiguration().getJSONObject("module");

    assertTrue(pluginConf.has("a") && pluginConf.has("b") && pluginConf.has("c"));
    assertEquals(3, pluginConf.get("c"));
    assertEquals(2, pluginConf.get("b"));
    // Providers should be applied in order
    assertEquals(10, pluginConf.get("a"));
  }

  @Test
  public void testCache() throws Exception {
    String defaultLang = "es";
    Locale locale = new Locale(defaultLang);
    Properties firstProperties = new Properties();
    firstProperties.put("languages", "{\"es\": \"Espa\u00f1ol\"}");
    firstProperties.put("languages.default", defaultLang);
    Config config =
        buildConfigReadOnceAndChangeFolderConfig(true, -1, defaultLang, locale, firstProperties);

    // Check we still have the same values
    assertTrue(config.getDefaultLang().equals(defaultLang));
    assertTrue(config.getLanguages()[0].get("code").equals("es"));
    assertTrue(config.getProperties().equals(firstProperties));
  }

  @Test
  public void testNoCache() throws Exception {
    String defaultLang = "es";
    Locale locale = new Locale(defaultLang);
    Properties firstProperties = new Properties();
    firstProperties.put("languages", "{\"es\": \"Espa\u00f1ol\"}");
    firstProperties.put("languages.default", defaultLang);
    Config config =
        buildConfigReadOnceAndChangeFolderConfig(false, -1, defaultLang, locale, firstProperties);

    assertFalse(config.getDefaultLang().equals(defaultLang));
    assertFalse(config.getLanguages()[0].get("code").equals("es"));
    assertFalse(config.getProperties() == firstProperties);
  }

  @Test
  public void testCacheTimeout() throws Exception {
    String defaultLang = "es";

    Properties firstProperties = new Properties();
    firstProperties.put("languages", "{\"es\": \"Espa\u00f1ol\"}");
    firstProperties.put("languages.default", defaultLang);

    int cacheTimeout = 1;
    Config config = buildConfigReadOnceAndChangeFolderConfig(true, cacheTimeout, defaultLang,
        new Locale(defaultLang), firstProperties);

    // Check we still have the same values
    assertTrue(config.getDefaultLang().equals(defaultLang));
    assertTrue(config.getLanguages()[0].get("code").equals("es"));
    assertTrue(config.getProperties().equals(firstProperties));

    Thread.sleep(cacheTimeout * 1500);

    // Check values changed
    assertFalse(config.getDefaultLang().equals(defaultLang));
    assertFalse(config.getLanguages()[0].get("code").equals("es"));
    assertFalse(config.getProperties() == firstProperties);
  }

  private Config buildConfigReadOnceAndChangeFolderConfig(boolean useCache, int cacheTimeout,
      String defaultLang, Locale locale, Properties firstProperties) throws IOException {
    File portalProperties = new File(folder.getRoot(), "portal.properties");
    firstProperties.store(new FileOutputStream(portalProperties), "");

    Config config = new ConfigImpl(folder.getRoot(), new ArrayList<PluginConfigProvider>(), null,
        useCache, cacheTimeout);

    assertTrue(config.getDefaultLang().equals(defaultLang));
    assertTrue(config.getLanguages()[0].get("code").equals("es"));
    assertTrue(config.getProperties().equals(firstProperties));

    Properties secondProperties = new Properties();
    secondProperties.put("languages", "{\"fr\": \"Frances\"}");
    secondProperties.put("languages.default", "fr");
    secondProperties.store(new FileOutputStream(portalProperties), "");
    return config;
  }

  @Test
  public void testPluginConfigurationCached() throws Exception {
    readPluginConfigurationTwice(true, true, 1);
  }

  @Test
  public void testPluginConfigurationCacheIgnoredIfProviderCannotBeCached() throws Exception {
    readPluginConfigurationTwice(true, false, 2);
  }

  @Test
  public void testPluginConfigurationCacheIgnoredIfCacheDisabled() throws Exception {
    readPluginConfigurationTwice(false, true, 2);
  }

  @SuppressWarnings("unchecked")
  private void readPluginConfigurationTwice(boolean useCache, boolean canBeCached, int numCalls)
      throws IOException {
    // Install configuration provider
    PluginConfigProvider provider = mock(PluginConfigProvider.class);
    when(provider.canBeCached()).thenReturn(canBeCached);

    Config config =
        new ConfigImpl(null, Arrays.asList(provider), Collections.<Plugin>emptySet(), useCache, -1);

    // Call twice
    config.getPluginConfig(Locale.getDefault(), request);
    config.getPluginConfig(Locale.getDefault(), request);

    // Check num calls
    verify(provider, times(numCalls)).getPluginConfig(any(Config.class), any(Map.class),
        any(HttpServletRequest.class));
  }

  @Test
  public void testNoConfigurationFolder() throws Exception {
    File portalProperties = new File(folder.getRoot(), "portal.properties");
    new Properties().store(new FileOutputStream(portalProperties), "");

    Config config = new ConfigImpl(folder.getRoot(), new ArrayList<PluginConfigProvider>(),
        Collections.<Plugin>emptySet(), false, -1);
    assertNotNull(config.getDir());
    assertNotNull(config.getPluginConfig(Locale.getDefault(), request));
    assertNotNull(config.getProperties());
    assertNotNull(config.getMessages(Locale.getDefault()));
    assertNotNull(config.getDefaultLang());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFailingConfigurationProvider() throws Exception {
    PluginConfigProvider provider = mock(PluginConfigProvider.class);
    when(provider.getPluginConfig(any(Config.class), any(Map.class), any(HttpServletRequest.class)))
        .thenThrow(new IOException("mock"));
    Config config = new ConfigImpl(mock(File.class), Arrays.asList(provider),
        Collections.<Plugin>emptySet(), false, -1);
    assertNotNull(config.getPluginConfig(Locale.getDefault(), request));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testMergeDoesNotAffectDefaultPluginConfiguration() throws IOException {
    Set<Plugin> plugins = new HashSet<Plugin>();
    Plugin pluginDescriptor = new Plugin("p1", JSONObject.fromObject("{default-conf:{m1:true}}"));
    plugins.add(pluginDescriptor);

    Map<String, JSONObject> mergingConfiguration1 = new HashMap<String, JSONObject>();
    mergingConfiguration1.put("p1", JSONObject.fromObject("{m2:true}"));
    Map<String, JSONObject> mergingConfiguration2 = new HashMap<String, JSONObject>();
    mergingConfiguration2.put("p1", JSONObject.fromObject("{}"));
    PluginConfigProvider provider = mock(PluginConfigProvider.class);
    when(provider.getPluginConfig(any(Config.class), any(Map.class), any(HttpServletRequest.class)))
        .thenReturn(mergingConfiguration1).thenReturn(mergingConfiguration2);
    Config config = new ConfigImpl(mock(File.class), Arrays.asList(provider), plugins, false, -1);

    JSONObject configuration = config.getPluginConfig(Locale.ROOT, request)[0].getConfiguration();
    assertEquals(2, configuration.keySet().size());

    configuration = config.getPluginConfig(Locale.ROOT, request)[0].getConfiguration();
    assertEquals(1, configuration.keySet().size());
  }

  @Test
  public void updatesPlugins() {
    Set<Plugin> plugins = new HashSet<Plugin>();
    Plugin plugin = new Plugin("p1", true);
    plugin.addModule("m1");
    plugins.add(plugin);

    Config config =
        new ConfigImpl(mock(File.class), new ArrayList<PluginConfigProvider>(), plugins, false, -1);
    Plugin[] pluginConfig = config.getPluginConfig(Locale.getDefault(), request);
    assertEquals(1, pluginConfig.length);
    assertEquals("p1", pluginConfig[0].getName());
    assertEquals(1, pluginConfig[0].getModules().size());
    assertEquals("m1", pluginConfig[0].getModules().iterator().next());

    plugins = new HashSet<Plugin>();
    plugin = new Plugin("p2", false);
    plugin.addModule("m2");
    plugins.add(plugin);

    config.setPlugins(plugins);
    pluginConfig = config.getPluginConfig(Locale.getDefault(), request);
    assertEquals(1, pluginConfig.length);
    assertEquals("p2", pluginConfig[0].getName());
    assertEquals(1, pluginConfig[0].getModules().size());
    assertEquals("p2/m2", pluginConfig[0].getModules().iterator().next());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void ignoresConfigurationForNonExistingPlugins() throws IOException {
    Set<Plugin> plugins = Collections.singleton(new Plugin("p1", true));


    PluginConfigProvider provider = mock(PluginConfigProvider.class);
    Map<String, JSONObject> providerConf =
        Collections.singletonMap("another_plugin", new JSONObject());
    when(provider.getPluginConfig(any(Config.class), any(Map.class), any(HttpServletRequest.class)))
        .thenReturn(providerConf);
    Config config = new ConfigImpl(mock(File.class), Arrays.asList(provider), plugins, false, -1);

    Plugin[] pluginConfig = config.getPluginConfig(Locale.getDefault(), request);
    assertEquals(1, pluginConfig.length);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  public void sendsCurrentConfigurationForGetPluginConfig() throws IOException {

    Set<Plugin> plugins = new HashSet<Plugin>();
    Plugin plugin = new Plugin("p1", JSONObject.fromObject("{default-conf:{m1:true}}"));
    plugins.add(plugin);

    PluginConfigProvider p1 = mock(PluginConfigProvider.class);
    PluginConfigProvider p2 = mock(PluginConfigProvider.class);

    JSONObject pluginConfig = JSONObject.fromObject("{m1 : { a : true, b : 2}}");
    when(p1.getPluginConfig(any(Config.class), any(Map.class), any(HttpServletRequest.class)))
        .thenReturn(Collections.singletonMap(plugin.getName(), pluginConfig));
    Config config = new ConfigImpl(mock(File.class), Arrays.asList(p1, p2), plugins, false, -1);

    config.getPluginConfig(Locale.ROOT, request);

    ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
    verify(p2).getPluginConfig(any(Config.class), captor.capture(), any(HttpServletRequest.class));

    Map<String, JSONObject> currentConfiguration = captor.getValue();
    assertEquals(1, currentConfiguration.size());
    assertEquals(pluginConfig, currentConfiguration.get(plugin.getName()));
  }

  @Test
  public void missingPropertiesFile() {
    Config config = new ConfigImpl(folder.getRoot(), new ArrayList<PluginConfigProvider>(),
        new HashSet<Plugin>(), false, -1);
    Properties properties = config.getProperties();
    assertEquals(0, properties.size());
  }

  @Test
  public void validPropertiesFile() throws Exception {
    Config config = new ConfigImpl(folder.getRoot(), new ArrayList<PluginConfigProvider>(),
        new HashSet<Plugin>(), false, -1);
    File file = new File(config.getDir(), "portal.properties");
    IOUtils.write("a=1", new FileOutputStream(file));

    Properties properties = config.getProperties();
    assertEquals(1, properties.size());
    assertEquals("1", properties.getProperty("a"));
  }

  @Test
  public void missingMessagesFile() throws Exception {
    Config config = new ConfigImpl(folder.getRoot(), new ArrayList<PluginConfigProvider>(),
        new HashSet<Plugin>(), false, -1);
    folder.newFolder("messages");
    ResourceBundle bundle = config.getMessages(Locale.ENGLISH);
    assertEquals(0, bundle.keySet().size());
  }

  @Test
  public void validMessagesFile() throws Exception {
    Config config = new ConfigImpl(folder.getRoot(), new ArrayList<PluginConfigProvider>(),
        new HashSet<Plugin>(), false, -1);
    File file = new File(folder.newFolder("messages"), "messages_en.properties");
    IOUtils.write("a=1\n", new FileOutputStream(file));
    ResourceBundle bundle = config.getMessages(Locale.ENGLISH);
    assertEquals(1, bundle.keySet().size());
    assertEquals("1", bundle.getString("a"));
  }
}
