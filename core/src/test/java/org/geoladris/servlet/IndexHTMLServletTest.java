package org.geoladris.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoladris.Environment;
import org.geoladris.Geoladris;
import org.geoladris.PluginDescriptor;
import org.geoladris.TestingServletContext;
import org.geoladris.config.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexHTMLServletTest {
  private IndexHTMLServlet servlet;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private TestingServletContext context;

  @Before
  public void setup() throws Exception {
    this.servlet = new IndexHTMLServlet();

    this.context = new TestingServletContext();

    this.request = this.context.request;
    this.response = this.context.response;

    this.servlet.init(this.context.servletConfig);
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
    when(config.getPluginConfig(any(Locale.class), eq(this.request))).thenReturn(plugins);
    this.context.servletContext.setAttribute(Geoladris.ATTR_CONFIG, config);

    this.servlet.doGet(this.request, this.response);

    String content = this.context.getResponse();
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
    assertFalse(this.context.getResponse().contains(css));
  }

  @Test
  public void minifiedIfDebugParamInvalid() throws Exception {
    mockDebugParam("invalid_value");

    this.servlet.doGet(this.request, this.response);

    String css = IndexHTMLServlet.OPTIMIZED_FOLDER + "/portal-style.css";
    assertTrue(this.context.getResponse().contains(css));
  }

  @Test
  public void minifiedIfDebugParamMissing() throws Exception {
    mockDebugParam(null);

    this.servlet.doGet(this.request, this.response);

    String css = IndexHTMLServlet.OPTIMIZED_FOLDER + "/portal-style.css";
    assertTrue(this.context.getResponse().contains(css));
  }

  /**
   * Minimum ServletContext attributes, {@link Environment#MINIFIED} set to true and debug param in
   * {@link #request} as given.
   * 
   * @param debugParam
   */
  private void mockDebugParam(String debugParam) {
    Config config = mock(Config.class);
    when(config.getPluginConfig(any(Locale.class), eq(this.request)))
        .thenReturn(new PluginDescriptor[0]);
    this.context.servletContext.setAttribute(Geoladris.ATTR_CONFIG, config);

    System.setProperty(Environment.MINIFIED, "true");
    when(this.request.getParameter(IndexHTMLServlet.HTTP_PARAM_DEBUG)).thenReturn(debugParam);
  }
}
