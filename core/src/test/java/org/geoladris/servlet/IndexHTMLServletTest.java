package org.geoladris.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoladris.Environment;
import org.geoladris.Geoladris;
import org.geoladris.PluginDescriptor;
import org.geoladris.config.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexHTMLServletTest {
  private IndexHTMLServlet servlet;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private ByteArrayOutputStream bos;
  private ServletContext servletContext;

  @Before
  public void setup() throws Exception {
    this.servlet = new IndexHTMLServlet();
    this.request = mock(HttpServletRequest.class);
    this.response = mock(HttpServletResponse.class);

    this.bos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(this.bos);
    when(this.response.getWriter()).thenReturn(writer);

    this.servletContext = mock(ServletContext.class);

    ServletConfig servletConfig = mock(ServletConfig.class);
    when(servletConfig.getServletContext()).thenReturn(this.servletContext);
    this.servlet.init(servletConfig);
  }

  @After
  public void teardown() {
    System.getProperties().remove(Environment.MINIFIED);
  }

  @Test
  public void cssOrder() throws Exception {
    PluginDescriptor plugin = new PluginDescriptor("myplugin", true);
    plugin.addStylesheet("theme/a.css");
    plugin.addStylesheet("styles/a.css");
    plugin.addStylesheet("theme/b.css");
    PluginDescriptor[] plugins = new PluginDescriptor[] {plugin};

    Config config = mock(Config.class);
    when(config.getPluginConfig(any(Locale.class))).thenReturn(plugins);
    when(this.request.getAttribute(Geoladris.ATTR_CONFIG)).thenReturn(config);

    this.servlet.doGet(this.request, this.response);
    this.response.getWriter().flush();
    this.bos.flush();

    String content = this.bos.toString();
    int i1 = content.indexOf("<link rel=\"stylesheet\" href=\"styles/a.css\">");
    int i2 = content.indexOf("<link rel=\"stylesheet\" href=\"theme/a.css\">");
    int i3 = content.indexOf("<link rel=\"stylesheet\" href=\"theme/b.css\">");
    assertTrue(i1 < i2);
    assertTrue(i1 < i3);
  }

  @Test
  public void notMinifiedIfDebugParamTrue() throws Exception {
    mockDebugParam("true");

    this.servlet.doGet(this.request, this.response);

    String css = IndexHTMLServlet.OPTIMIZED_FOLDER + "/portal-style.css";
    assertFalse(responseContent().contains(css));
  }

  @Test
  public void minifiedIfDebugParamInvalid() throws Exception {
    mockDebugParam("invalid_value");

    this.servlet.doGet(this.request, this.response);

    String css = IndexHTMLServlet.OPTIMIZED_FOLDER + "/portal-style.css";
    assertTrue(responseContent().contains(css));
  }

  @Test
  public void minifiedIfDebugParamMissing() throws Exception {
    mockDebugParam(null);

    this.servlet.doGet(this.request, this.response);

    String css = IndexHTMLServlet.OPTIMIZED_FOLDER + "/portal-style.css";
    assertTrue(responseContent().contains(css));
  }

  /**
   * Minimum ServletContext attributes, {@link Environment#MINIFIED} set to true and debug param in
   * {@link #request} as given.
   * 
   * @param debugParam
   */
  private void mockDebugParam(String debugParam) {
    Config config = mock(Config.class);
    when(config.getPluginConfig(any(Locale.class))).thenReturn(new PluginDescriptor[0]);
    when(this.request.getAttribute(Geoladris.ATTR_CONFIG)).thenReturn(config);

    System.setProperty(Environment.MINIFIED, "true");
    when(this.request.getParameter(IndexHTMLServlet.HTTP_PARAM_DEBUG)).thenReturn(debugParam);
  }

  private String responseContent() throws IOException {
    this.response.getWriter().flush();
    this.bos.flush();
    return this.bos.toString();
  }
}
