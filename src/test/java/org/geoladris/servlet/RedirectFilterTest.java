package org.geoladris.servlet;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoladris.Geoladris;
import org.geoladris.Plugin;
import org.geoladris.config.Config;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RedirectFilterTest {
  private static final String CONTEXT_PATH = "/test";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private RedirectFilter filter;
  private ServletContext context;
  private Config config;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain chain;
  private RequestDispatcher dispatcher;

  @Before
  public void setup() throws ServletException {
    this.filter = new RedirectFilter();

    config = mock(Config.class);
    when(config.getDir()).thenReturn(tmp.getRoot());

    context = mock(ServletContext.class);
    when(context.getAttribute(Geoladris.ATTR_CONFIG)).thenReturn(config);

    FilterConfig filterConfig = mock(FilterConfig.class);
    when(filterConfig.getServletContext()).thenReturn(context);
    this.filter.init(filterConfig);

    this.dispatcher = mock(RequestDispatcher.class);
    this.request = mock(HttpServletRequest.class);
    this.response = mock(HttpServletResponse.class);
    this.chain = mock(FilterChain.class);

    when(this.request.getContextPath()).thenReturn(CONTEXT_PATH);
  }

  @Test
  public void resourceExistsInContext() throws Exception {

    when(context.getResource("index.html")).thenReturn(mockUrl());
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/index.html");
    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  public void nonJsInRoot() throws Exception {
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/app.min.css");
    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  @Test
  public void jsInWarRootPlugin() throws Exception {
    mockPlugin("core", true);
    mockWarResource("/core/css/test.css");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/css/test.css");

    verifyDispatcher();
  }

  @Test
  public void cssInWarRootPlugin() throws Exception {
    mockPlugin("core", true);
    mockWarResource("/core/src/message-bus.js");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/message-bus.js");

    verifyDispatcher();
  }

  @Test
  public void jslibInWarRootPlugin() throws Exception {
    mockPlugin("core", true);
    mockWarResource("/core/jslib/lib.js");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/jslib/lib.js");

    verifyDispatcher();
  }

  @Test
  public void jsInConfigRootPlugin() throws Exception {
    mockPlugin("p", true);
    mockConfigResource("/p/src/module.js");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/module.js");

    verifyDispatcher();
  }

  @Test
  public void cssInConfigRootPlugin() throws Exception {
    mockPlugin("p", true);
    mockConfigResource("/p/css/test.css");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/css/test.css");

    verifyDispatcher();
  }

  @Test
  public void jslibInConfigRootPlugin() throws Exception {
    mockPlugin("core", true);
    mockConfigResource("/core/jslib/lib.js");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/jslib/lib.js");

    verifyDispatcher();
  }

  @Test
  public void nodeModulesInConfigRootPlugin() throws Exception {
    mockPlugin("core", true);
    mockConfigResource("/core/node_modules/lib.js");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/node_modules/lib.js");

    verifyDispatcher();
  }

  @Test
  public void jsInWarNonRootPlugin() throws Exception {
    mockPlugin("p", false);
    mockWarResource("/p/src/module.js");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/p/module.js");

    verifyDispatcher();
  }

  @Test
  public void cssInWarNonRootPlugin() throws Exception {
    mockPlugin("p", false);
    mockWarResource("/p/css/test.css");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/css/p/test.css");

    verifyDispatcher();
  }

  @Test
  public void jslibInWarNonRootPlugin() throws Exception {
    mockPlugin("p", false);
    mockWarResource("/p/jslib/lib.js");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/jslib/lib.js");

    verifyDispatcher();
  }

  @Test
  public void jsInConfigNonRootPlugin() throws Exception {
    mockPlugin("p", false);
    mockConfigResource("/p/src/module.js");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/p/module.js");

    verifyDispatcher();
  }

  @Test
  public void cssInConfigNonRootPlugin() throws Exception {
    mockPlugin("p", false);
    mockConfigResource("/p/css/test.css");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/css/p/test.css");

    verifyDispatcher();
  }

  @Test
  public void jslibInConfigNonRootPlugin() throws Exception {
    mockPlugin("p", false);
    mockConfigResource("/p/jslib/lib.js");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/jslib/lib.js");

    verifyDispatcher();
  }

  @Test
  public void nodeModulesInConfigNonRootPlugin() throws Exception {
    mockPlugin("p", false);
    mockConfigResource("/p/node_modules/lib.js");
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/node_modules/lib.js");

    verifyDispatcher();
  }

  @Test
  public void resourceNotInPlugins() throws Exception {
    mockPlugin("core", true);
    when(request.getRequestURI()).thenReturn(CONTEXT_PATH + "/static/header.png");

    filter.doFilter(request, response, chain);

    verify(chain).doFilter(request, response);
  }

  private void mockPlugin(String name, boolean installInRoot) {
    when(config.getPluginConfig(any(Locale.class), any(HttpServletRequest.class)))
        .thenReturn(new Plugin[] {new Plugin(name, installInRoot)});
  }

  private URL mockUrl() throws IOException {
    return new File("foo").toURI().toURL();
  }

  private void mockWarResource(String resource) throws Exception {
    String path = "/" + Geoladris.PATH_PLUGINS_FROM_WAR + resource;
    when(context.getResource(path)).thenReturn(mockUrl());
    when(request.getRequestDispatcher(path)).thenReturn(dispatcher);
  }

  private void mockConfigResource(String resource) throws Exception {
    File f = new File(this.config.getDir(), Config.DIR_PLUGINS + resource);
    f.getParentFile().mkdirs();
    f.createNewFile();
    when(request.getRequestDispatcher("/" + Geoladris.PATH_PLUGINS_FROM_CONFIG + resource))
        .thenReturn(dispatcher);
  }

  private void verifyDispatcher() throws Exception {
    filter.doFilter(request, response, chain);

    verify(chain, never()).doFilter(request, response);
    verify(dispatcher).forward(request, response);
  }
}
