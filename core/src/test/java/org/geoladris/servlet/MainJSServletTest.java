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

import org.geoladris.PluginDescriptor;
import org.geoladris.config.Config;
import org.geoladris.config.PluginDescriptors;
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
    PluginDescriptor p1 = new PluginDescriptor();
    p1.getRequireJSPathsMap().put("ol", "jslib/ol");
    PluginDescriptor p2 = new PluginDescriptor();
    p2.getRequireJSPathsMap().put("jquery-ui", "jslib/jquery-ui");

    PluginDescriptors descriptors = mock(PluginDescriptors.class);
    Config config = mock(Config.class);
    when(config.getPluginConfig(any(Locale.class), any(HttpServletRequest.class)))
        .thenReturn(descriptors);
    when(servletContext.getAttribute(AppContextListener.ATTR_CONFIG)).thenReturn(config);

    HttpServletResponse response = mockResponse();
    when(descriptors.getEnabled()).thenReturn(new PluginDescriptor[] {p1, p2});
    this.servlet.doGet(this.request, response, false);
    assertTrue(content(response).contains("jslib/jquery-ui"));

    response = mockResponse();
    when(descriptors.getEnabled()).thenReturn(new PluginDescriptor[] {p1});
    this.servlet.doGet(this.request, response, false);
    assertFalse(content(response).contains("jslib/jquery-ui"));
  }

  @Test
  public void useCache() throws Exception {
    PluginDescriptor p1 = new PluginDescriptor();
    p1.getRequireJSPathsMap().put("ol", "jslib/ol");
    PluginDescriptor p2 = new PluginDescriptor();
    p2.getRequireJSPathsMap().put("jquery-ui", "jslib/jquery-ui");

    PluginDescriptors descriptors = mock(PluginDescriptors.class);
    Config config = mock(Config.class);
    when(config.getPluginConfig(any(Locale.class), any(HttpServletRequest.class)))
        .thenReturn(descriptors);
    when(servletContext.getAttribute(AppContextListener.ATTR_CONFIG)).thenReturn(config);

    HttpServletResponse response = mockResponse();
    when(descriptors.getEnabled()).thenReturn(new PluginDescriptor[] {p1, p2});
    this.servlet.doGet(this.request, response, true);
    assertTrue(content(response).contains("jslib/jquery-ui"));

    response = mockResponse();
    when(descriptors.getEnabled()).thenReturn(new PluginDescriptor[] {p1});
    this.servlet.doGet(this.request, response, true);
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
