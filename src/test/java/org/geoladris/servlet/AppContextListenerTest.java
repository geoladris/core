package org.geoladris.servlet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.geoladris.Environment;
import org.geoladris.Geoladris;
import org.geoladris.Plugin;
import org.geoladris.PluginDirsAnalyzer;
import org.geoladris.TestingServletContext;
import org.geoladris.config.Config;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AppContextListenerTest {
  private static final String CONTEXT_PATH = "test";
  private static final String DEFAULT_CONFIG = "default_config";

  private AppContextListener listener;
  private TestingServletContext context;
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  private Set<Plugin> plugins;

  @Before
  public void setup() throws IOException {
    this.listener = spy(new AppContextListener());
    this.context = new TestingServletContext();

    context = new TestingServletContext();
    folder.newFolder(CONTEXT_PATH);
    final File defaultConfig = folder.newFolder(DEFAULT_CONFIG);

    context = new TestingServletContext();
    context.setContextPath("/" + CONTEXT_PATH);
    when(context.servletContext.getRealPath("WEB-INF/default_config"))
        .thenReturn(defaultConfig.getAbsolutePath());

    System.clearProperty(Environment.CONFIG_DIR);
    System.clearProperty(Environment.PORTAL_CONFIG_DIR);

    when(context.servletContext.getRealPath(anyString())).then(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        if (invocation.getArguments()[0].toString().startsWith("WEB-INF/default_config")) {
          return defaultConfig.getAbsolutePath();
        } else {
          return folder.getRoot().getAbsolutePath();
        }
      }
    });

    plugins = new HashSet<>();
    PluginDirsAnalyzer analyzer = mock(PluginDirsAnalyzer.class);
    when(analyzer.getPlugins()).thenReturn(plugins);
    doReturn(analyzer).when(listener).getAnalyzer(any(File[].class));
  }

  @Test
  public void configDir() throws Exception {
    System.setProperty(Environment.CONFIG_DIR, folder.getRoot().getAbsolutePath());
    Config config = init("/");
    assertEquals(new File(folder.getRoot(), CONTEXT_PATH), config.getDir());
  }

  @Test
  public void portalConfigDir() throws Exception {
    File dir = new File(folder.getRoot(), CONTEXT_PATH);
    System.setProperty(Environment.PORTAL_CONFIG_DIR, dir.getAbsolutePath());
    Config config = init("/");
    assertEquals(dir, config.getDir());
  }

  @Test
  public void missingConfigDir() throws Exception {
    File defaultConfig = new File(folder.getRoot(), DEFAULT_CONFIG);
    Config config = init("/");
    assertEquals(defaultConfig, config.getDir());
  }

  @Test
  public void invalidConfigDir() throws Exception {
    System.setProperty(Environment.CONFIG_DIR, "invalid_dir");
    File defaultConfig = new File(folder.getRoot(), DEFAULT_CONFIG);
    Config config = init("/");
    assertEquals(defaultConfig, config.getDir());
  }

  private Config init(String path) throws Exception {
    when(context.request.getRequestURI()).thenReturn("/" + path);
    listener.contextInitialized(context.event);
    return (Config) context.servletContext.getAttribute(Geoladris.ATTR_CONFIG);
  }
}
