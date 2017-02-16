package org.geoladris.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.geoladris.Context;
import org.geoladris.Environment;
import org.geoladris.Geoladris;
import org.geoladris.JEEContextAnalyzer;
import org.geoladris.PluginDescriptor;
import org.geoladris.TestingServletContext;
import org.geoladris.config.Config;
import org.geoladris.config.DBConfigurationProvider;
import org.geoladris.config.ModuleConfigurationProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConfigFilterTest {
  private static final String CONTEXT_PATH = "test";
  private static final String DEFAULT_CONFIG = "default_config";

  private static final String PROP_APP = "app";

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private ConfigFilter filter;
  private TestingServletContext context;

  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain chain;

  private DBConfigurationProvider dbProvider;
  private Set<PluginDescriptor> plugins;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    folder.newFolder(CONTEXT_PATH);
    File defaultConfig = folder.newFolder(DEFAULT_CONFIG);

    context = new TestingServletContext();
    context.setContextPath("/" + CONTEXT_PATH);
    when(context.servletContext.getRealPath("WEB-INF/default_config"))
        .thenReturn(defaultConfig.getAbsolutePath());

    System.clearProperty(Environment.CONFIG_DIR);
    System.clearProperty(Environment.PORTAL_CONFIG_DIR);

    this.request = context.request;
    this.response = context.response;
    this.chain = mock(FilterChain.class);

    dbProvider = mock(DBConfigurationProvider.class);
    List<ModuleConfigurationProvider> providers =
        (List<ModuleConfigurationProvider>) context.servletContext
            .getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS);
    providers.add(dbProvider);

    when(context.servletContext.getRealPath("WEB-INF/default_config"))
        .thenReturn(defaultConfig.getAbsolutePath());
    filter = spy(new ConfigFilter());
    filter.init(context.filterConfig);

    plugins = new HashSet<>();
    JEEContextAnalyzer analyzer = mock(JEEContextAnalyzer.class);
    when(analyzer.getPluginDescriptors()).thenReturn(plugins);
    doReturn(analyzer).when(filter).getAnalyzer(any(Context.class));
  }

  @Test
  public void configDir() throws Exception {
    System.setProperty(Environment.CONFIG_DIR, folder.getRoot().getAbsolutePath());
    Config config = filter("/");
    assertEquals(new File(folder.getRoot(), CONTEXT_PATH), config.getDir());
  }

  @Test
  public void portalConfigDir() throws Exception {
    File dir = new File(folder.getRoot(), CONTEXT_PATH);
    System.setProperty(Environment.PORTAL_CONFIG_DIR, dir.getAbsolutePath());
    Config config = filter("/");
    assertEquals(dir, config.getDir());
  }

  @Test
  public void missingConfigDir() throws Exception {
    File defaultConfig = new File(folder.getRoot(), DEFAULT_CONFIG);
    Config config = filter("/");
    assertEquals(defaultConfig, config.getDir());
  }

  @Test
  public void invalidConfigDir() throws Exception {
    System.setProperty(Environment.CONFIG_DIR, "invalid_dir");
    File defaultConfig = new File(folder.getRoot(), DEFAULT_CONFIG);
    Config config = filter("/");
    assertEquals(defaultConfig, config.getDir());
  }

  @Test
  public void rootConfig() throws Exception {
    File rootConfigDir = folder.getRoot();
    File contextConfigDir = new File(rootConfigDir, CONTEXT_PATH);

    System.setProperty(Environment.CONFIG_DIR, rootConfigDir.getAbsolutePath());
    writePortalProperties("root", contextConfigDir);

    Config config = filter("/");

    assertEquals(contextConfigDir, config.getDir());
    assertEquals("root", config.getProperties().getProperty(PROP_APP));
  }

  @Test
  public void sameRootConfigMultipleCalls() throws Exception {
    File rootConfigDir = folder.getRoot();
    File contextConfigDir = new File(rootConfigDir, CONTEXT_PATH);

    System.setProperty(Environment.CONFIG_DIR, rootConfigDir.getAbsolutePath());
    writePortalProperties("root", contextConfigDir);

    Config cfg1 = filter("/");
    Config cfg2 = filter("/");

    assertEquals(cfg1, cfg2);
  }

  @Test
  public void subappConfig() throws Exception {
    String subapp = "mysubapp";
    File subappConfigDir = setupSubapp(subapp);

    when(dbProvider.isEnabled()).thenReturn(true);
    when(dbProvider.hasApp(CONTEXT_PATH + "/" + subapp)).thenReturn(true);

    Config config = filter("/" + subapp + "/");
    assertEquals(subappConfigDir, config.getDir());
    assertEquals(subapp, config.getProperties().getProperty(PROP_APP));
  }

  @Test
  public void sameSubappConfigMultipleCalls() throws Exception {
    String subapp = "mysubapp";
    setupSubapp(subapp);
    when(dbProvider.isEnabled()).thenReturn(true);
    when(dbProvider.hasApp(CONTEXT_PATH + "/" + subapp)).thenReturn(true);

    Config cfg1 = filter("/" + subapp + "/");
    Config cfg2 = filter("/" + subapp + "/");

    assertEquals(subapp, cfg1.getProperties().getProperty(PROP_APP));
    assertEquals(cfg1, cfg2);
  }

  @Test
  public void subappPublicConf() throws Exception {
    String subapp = "mysubapp";
    File subappConfigDir = setupSubapp(subapp);
    new File(subappConfigDir, "public-conf.json").createNewFile();

    Config config = filter("/" + subapp + "/");

    assertEquals(subapp, config.getProperties().getProperty(PROP_APP));
  }

  @Test
  public void subappPluginConf() throws Exception {
    String subapp = "mysubapp";
    File subappConfigDir = setupSubapp(subapp);
    new File(subappConfigDir, "plugin-conf.json").createNewFile();

    Config config = filter("/" + subapp + "/");

    assertEquals(subapp, config.getProperties().getProperty(PROP_APP));
  }

  @Test
  public void removesNonExistingConfigsPeriodically() throws Exception {
    File dir1 = setupSubapp("subapp1");
    File dir2 = setupSubapp("subapp2");
    new File(dir1, "public-conf.json").createNewFile();
    new File(dir2, "public-conf.json").createNewFile();

    filter.setSchedulerRate(1);
    filter.init(context.filterConfig);

    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/subapp1");
    filter.doFilter(request, response, chain);
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/subapp2");
    filter.doFilter(request, response, chain);

    Map<String, Config> configs = filter.getAppConfigs();
    assertEquals(2, configs.size());
    assertTrue(configs.containsKey("subapp1"));
    assertTrue(configs.containsKey("subapp2"));

    FileUtils.deleteDirectory(dir1);
    FileUtils.deleteDirectory(dir2);

    Thread.sleep(1100);
    assertEquals(0, configs.size());
  }

  @Test
  public void removesNonExistingConfigsIfRequested() throws Exception {
    File dir1 = setupSubapp("subapp1");
    File dir2 = setupSubapp("subapp2");
    new File(dir1, "public-conf.json").createNewFile();
    new File(dir2, "public-conf.json").createNewFile();

    filter.setSchedulerRate(1);
    filter.init(context.filterConfig);

    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/subapp1");
    filter.doFilter(request, response, chain);
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/subapp2");
    filter.doFilter(request, response, chain);

    Map<String, Config> configs = filter.getAppConfigs();
    assertEquals(2, configs.size());
    assertTrue(configs.containsKey("subapp1"));
    assertTrue(configs.containsKey("subapp2"));

    FileUtils.deleteDirectory(dir1);

    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/subapp1");
    filter.doFilter(request, response, chain);
    assertEquals(1, configs.size());
    assertTrue(configs.containsKey("subapp2"));
  }

  private Config filter(String path) throws Exception {
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + path);
    filter.doFilter(request, response, chain);
    return (Config) request.getAttribute(Geoladris.ATTR_CONFIG);
  }

  private File setupSubapp(String subapp) throws Exception {
    File rootConfigDir = folder.getRoot();
    System.setProperty(Environment.CONFIG_DIR, rootConfigDir.getAbsolutePath());
    File contextConfigDir = new File(rootConfigDir, CONTEXT_PATH);
    File subappConfigDir = new File(contextConfigDir, subapp);
    subappConfigDir.mkdir();

    writePortalProperties(subapp, subappConfigDir);
    return subappConfigDir;
  }

  private void writePortalProperties(String app, File configDir) throws Exception {
    Properties props = new Properties();
    props.setProperty(PROP_APP, app);
    props.store(new FileOutputStream(new File(configDir, "portal.properties")), "");
  }

}
