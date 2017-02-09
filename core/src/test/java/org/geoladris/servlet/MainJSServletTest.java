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
import org.junit.Before;
import org.junit.Test;

public class MainJSServletTest {
  private MainJSServlet servlet;
  private HttpServletRequest request;
  private ByteArrayOutputStream bos;
  private ServletContext servletContext;

  @Before
  public void setup() throws Exception {
    this.servlet = new MainJSServlet();
    this.request = mock(HttpServletRequest.class);

    this.servletContext = mock(ServletContext.class);

    ServletConfig servletConfig = mock(ServletConfig.class);
    when(servletConfig.getServletContext()).thenReturn(this.servletContext);
    this.servlet.init(servletConfig);
  }

  @Test
  public void dontCacheIfDisabledGlobally() throws Exception {
    PluginDescriptor p1 = new PluginDescriptor("p1", true);
    p1.addRequireJSPath("ol", "jslib/ol");
    PluginDescriptor p2 = new PluginDescriptor("p2", true);
    p2.addRequireJSPath("jquery-ui", "jslib/jquery-ui");

    Config config = mock(Config.class);
    System.setProperty(Environment.CONFIG_CACHE, "false");
    when(request.getAttribute(Geoladris.ATTR_CONFIG)).thenReturn(config);

    HttpServletResponse response = mockResponse();
    when(config.getPluginConfig(any(Locale.class))).thenReturn(new PluginDescriptor[] {p1, p2});
    this.servlet.doGet(this.request, response);
    assertTrue(content(response).contains("jslib/jquery-ui"));

    response = mockResponse();
    when(config.getPluginConfig(any(Locale.class))).thenReturn(new PluginDescriptor[] {p1});
    this.servlet.doGet(this.request, response);
    assertFalse(content(response).contains("jslib/jquery-ui"));
  }

  @Test
  public void useCache() throws Exception {
    PluginDescriptor p1 = new PluginDescriptor("p1", true);
    p1.addRequireJSPath("ol", "jslib/ol");
    PluginDescriptor p2 = new PluginDescriptor("p2", true);
    p2.addRequireJSPath("jquery-ui", "jslib/jquery-ui");

    Config config = mock(Config.class);
    System.setProperty(Environment.CONFIG_CACHE, "true");
    when(request.getAttribute(Geoladris.ATTR_CONFIG)).thenReturn(config);

    HttpServletResponse response = mockResponse();
    when(config.getPluginConfig(any(Locale.class))).thenReturn(new PluginDescriptor[] {p1, p2});
    this.servlet.doGet(this.request, response);
    assertTrue(content(response).contains("jslib/jquery-ui"));

    response = mockResponse();
    when(config.getPluginConfig(any(Locale.class))).thenReturn(new PluginDescriptor[] {p1, p2});
    this.servlet.doGet(this.request, response);
    assertTrue(content(response).contains("jslib/jquery-ui"));
  }

  private String content(HttpServletResponse response) throws IOException {
    response.getWriter().flush();
    this.bos.flush();
    this.bos.close();
    return this.bos.toString();
  }

  private HttpServletResponse mockResponse() throws IOException {
    this.bos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(this.bos);
    HttpServletResponse response = mock(HttpServletResponse.class);
    when(response.getWriter()).thenReturn(writer);

    return response;
  }
}
