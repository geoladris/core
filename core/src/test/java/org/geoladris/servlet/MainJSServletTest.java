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
import org.junit.Before;
import org.junit.Test;

public class MainJSServletTest {
  private MainJSServlet servlet;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private TestingServletContext context;

  @Before
  public void setup() throws Exception {
    this.servlet = new MainJSServlet();

    this.context = new TestingServletContext();

    this.request = this.context.request;
    this.response = this.context.response;

    this.servlet.init(this.context.servletConfig);
  }

  @Test
  public void dontCacheIfDisabledGlobally() throws Exception {
    PluginDescriptor p1 = new PluginDescriptor("p1", true);
    p1.addRequireJSPath("ol", "jslib/ol");
    PluginDescriptor p2 = new PluginDescriptor("p2", true);
    p2.addRequireJSPath("jquery-ui", "jslib/jquery-ui");

    Config config = mock(Config.class);
    System.setProperty(Environment.CONFIG_CACHE, "false");
    context.servletContext.setAttribute(Geoladris.ATTR_CONFIG, config);

    when(config.getPluginConfig(any(Locale.class), eq(request)))
        .thenReturn(new PluginDescriptor[] {p1, p2});
    this.servlet.doGet(this.request, response);
    assertTrue(context.getResponse().contains("jslib/jquery-ui"));

    when(config.getPluginConfig(any(Locale.class), eq(request)))
        .thenReturn(new PluginDescriptor[] {p1});
    this.servlet.doGet(this.request, response);
    assertFalse(context.getResponse().contains("jslib/jquery-ui"));
  }

  @Test
  public void useCache() throws Exception {
    PluginDescriptor p1 = new PluginDescriptor("p1", true);
    p1.addRequireJSPath("ol", "jslib/ol");
    PluginDescriptor p2 = new PluginDescriptor("p2", true);
    p2.addRequireJSPath("jquery-ui", "jslib/jquery-ui");

    Config config = mock(Config.class);
    System.setProperty(Environment.CONFIG_CACHE, "true");
    context.servletContext.setAttribute(Geoladris.ATTR_CONFIG, config);

    when(config.getPluginConfig(any(Locale.class), eq(request)))
        .thenReturn(new PluginDescriptor[] {p1, p2});
    this.servlet.doGet(this.request, response);
    assertTrue(context.getResponse().contains("jslib/jquery-ui"));

    when(config.getPluginConfig(any(Locale.class), eq(request)))
        .thenReturn(new PluginDescriptor[] {p1, p2});
    this.servlet.doGet(this.request, response);
    assertTrue(context.getResponse().contains("jslib/jquery-ui"));
  }
}
