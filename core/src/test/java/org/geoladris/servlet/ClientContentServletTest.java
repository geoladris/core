package org.geoladris.servlet;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoladris.Environment;
import org.geoladris.PluginDescriptor;
import org.geoladris.TestingServletContext;
import org.geoladris.config.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ClientContentServletTest {
  private Config config;
  private ClientContentServlet servlet;

  private HttpServletResponse response;
  private HttpServletRequest request;

  private Properties systemProperties;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    systemProperties = System.getProperties();
    Properties props = new Properties();
    props.putAll(systemProperties);
    System.setProperties(props);
  }

  @After
  public void teardown() {
    System.setProperties(systemProperties);
  }

  /**
   * Mock ServletContext and initialize {@link AppContextListener} with it. Capture {@link Config}
   * instance.
   * 
   * @param folder
   * @throws ServletException
   * @throws IOException
   */
  public void setupConfigurationFolder(final String folder) throws ServletException, IOException {
    String confDir = folder.substring(0, folder.lastIndexOf('/'));
    String contextPath = folder.substring(folder.lastIndexOf('/'));

    TestingServletContext context = new TestingServletContext();
    context.setContextPath(contextPath);

    ServletContext servletContext = context.servletContext;
    when(servletContext.getResourcePaths("/WEB-INF/lib")).thenReturn(new HashSet<String>());
    Answer<String> answer = new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return folder + invocation.getArguments()[0];
      }
    };
    when(servletContext.getRealPath(startsWith("/WEB-INF/"))).then(answer);

    config = context.mockConfig(new File(folder));
    System.setProperty(Environment.CONFIG_DIR, confDir);

    servlet = new ClientContentServlet();
    servlet.setTestingClasspathRoot(contextPath + "/WEB-INF/classes");
    servlet.init(context.servletConfig);

    this.request = context.request;
    this.response = context.response;
  }

  @Test
  public void scanNoJavaPlugins() throws ServletException, IOException {
    setupConfigurationFolder("src/test/resources/testNoJavaPlugins");
    requirePaths("/testNoJavaPlugins");
  }

  @Test
  public void scanJavaRootModules() throws ServletException, IOException {
    setupConfigurationFolder("src/test/resources/testJavaNonRootModules");
    requirePaths("/testJavaNonRootModules");
  }

  @Test
  public void scanJavaRootSubfolders() throws ServletException, IOException {
    setupConfigurationFolder("src/test/resources/testJavaRootSubfolders");
    requirePaths("/testJavaRootSubfolders");
  }

  /**
   * Get descriptors from config instance and query all modules, styles and libs to the
   * {@link ClientContentServlet}
   * 
   * @param classpathPrefix
   * @throws ServletException
   * @throws IOException
   */
  private void requirePaths(String classpathPrefix) throws ServletException, IOException {
    List<String> modules = new ArrayList<>();
    List<String> styles = new ArrayList<>();
    List<String> nonRequirePaths = new ArrayList<>();
    PluginDescriptor[] enabled = config.getPluginConfig(Locale.ROOT, request);

    for (PluginDescriptor plugin : enabled) {
      modules.addAll(plugin.getModules());
      styles.addAll(plugin.getStylesheets());
      nonRequirePaths.addAll(plugin.getRequireJSPathsMap().values());
    }

    for (int i = 0; i < modules.size(); i++) {
      modules.set(i, "/modules/" + modules.get(i) + ".js");
    }
    testRequest(modules, classpathPrefix);

    for (int i = 0; i < nonRequirePaths.size(); i++) {
      nonRequirePaths.set(i, nonRequirePaths.get(i).substring(2) + ".js");
    }
    testRequest(nonRequirePaths, classpathPrefix);

    for (int i = 0; i < styles.size(); i++) {
      styles.set(i, "/" + styles.get(i));
    }
    testRequest(styles, classpathPrefix);
  }

  private void testRequest(Collection<String> collection, String classpathPrefix)
      throws ServletException, IOException {
    assertTrue(collection.size() > 0);
    for (String path : collection) {
      int secondSlashIndex = path.indexOf("/", 1);
      String servletPath = path.substring(0, secondSlashIndex);
      String pathInfo = path.substring(secondSlashIndex);
      String contextPath = this.servlet.getServletContext().getContextPath();
      when(request.getRequestURI()).thenReturn(contextPath + servletPath + pathInfo);
      reset(response);
      servlet.doGet(request, response);

      verify(response).setStatus(HttpServletResponse.SC_OK);
    }
  }

  @Test
  public void test404() throws ServletException, IOException {
    setupConfigurationFolder("src/test/resources/testNoJavaPlugins");
    check404(servlet, "/static/", "/parabailarlabamba");
    check404(servlet, "/modules/", "j/module-notexists");
    check404(servlet, "/modules/", "plugin1/module-notexists");
  }

  private void check404(ClientContentServlet servlet, String servletPath, String pathInfo)
      throws ServletException, IOException {
    String contextPath = this.servlet.getServletContext().getContextPath();
    when(request.getRequestURI()).thenReturn(contextPath + servletPath + pathInfo);

    try {
      servlet.doGet(request, response);
      fail();
    } catch (StatusServletException e) {
      assertEquals(404, e.getStatus());
    }
  }

  @Test
  public void test304NotModified() throws ServletException, IOException {
    setupConfigurationFolder("src/test/resources/testNoJavaPlugins");

    String contextPath = this.servlet.getServletContext().getContextPath();
    when(request.getRequestURI()).thenReturn(contextPath + "/modules/plugin1/a.js");
    when(request.getDateHeader("If-Modified-Since")).thenReturn(System.currentTimeMillis());

    servlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
  }

  @Test
  public void test404ForDisabledPlugins() throws ServletException, IOException {
    setupConfigurationFolder("src/test/resources/testNoJavaPlugins");
    config.setPlugins(new HashSet<PluginDescriptor>());
    check404(servlet, "/modules/", "plugin1/a.js");
  }

  @Test
  public void svgContentType() throws Exception {
    setupConfigurationFolder("src/test/resources/testNoJavaPlugins");
    String contextPath = this.servlet.getServletContext().getContextPath();
    when(request.getRequestURI()).thenReturn(contextPath + "/modules/plugin1/images/image.svg");

    servlet.doGet(request, response);

    verify(response).setContentType("image/svg+xml");
  }

  @Test
  public void doesNotReturnUnqualifiedResourcesAsQualified() throws Exception {
    setupConfigurationFolder("src/test/resources/testJavaNonRootModules");
    check404(servlet, "/modules/", "testJavaRootSubfolders/module.js");
  }

  @Test
  public void doesNotReturnQualifiedResourcesAsUnqualified() throws Exception {
    setupConfigurationFolder("src/test/resources/testJavaNonRootModules");
    check404(servlet, "/modules/", "module1.js");
  }

  @Test
  public <T> void servesFromContextResources() throws Exception {
    setupConfigurationFolder("src/test/resources/testNoJavaPlugins");

    when(servlet.getServletContext().getResourceAsStream(anyString()))
        .then(new Answer<InputStream>() {
          @Override
          public InputStream answer(InvocationOnMock invocation) throws Throwable {
            return getClass().getResourceAsStream("/" + invocation.getArguments()[0].toString());
          }
        });

    String contextPath = this.servlet.getServletContext().getContextPath();
    when(request.getRequestURI()).thenReturn(contextPath + "/log4j.properties");
    servlet.doGet(request, response);

    verify(response).setStatus(HttpServletResponse.SC_OK);
  }
}
