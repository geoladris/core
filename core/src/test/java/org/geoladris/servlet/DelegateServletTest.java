package org.geoladris.servlet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoladris.Geoladris;
import org.geoladris.TestingServletContext;
import org.junit.Before;
import org.junit.Test;

public class DelegateServletTest {
  private static final String CONTEXT_PATH = "test";

  private DelegateServlet servlet;

  private HttpServletRequest request;
  private HttpServletResponse response;

  private ClientContentServlet clientContent;
  private IndexHTMLServlet index;
  private MainJSServlet main;
  private ConfigServlet config;

  @Before
  public void setup() throws Exception {
    clientContent = mock(ClientContentServlet.class);
    index = mock(IndexHTMLServlet.class);
    main = mock(MainJSServlet.class);
    config = mock(ConfigServlet.class);

    TestingServletContext context = new TestingServletContext();
    this.request = context.request;
    this.response = context.response;
    context.setContextPath("/" + CONTEXT_PATH);

    servlet = new DelegateServlet();
    servlet.setDelegates(clientContent, index, main, config);
    servlet.init(context.servletConfig);
  }

  @Test
  public void rootIndex() throws Exception {
    request.setAttribute(Geoladris.ATTR_APP, null);
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/");
    servlet.doGet(request, response);
    verify(index).doGet(request, response);
  }

  @Test
  public void subappIndex() throws Exception {
    request.setAttribute(Geoladris.ATTR_APP, "myapp");
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/myapp/");
    servlet.doGet(request, response);
    verify(index).doGet(request, response);
  }

  @Test
  public void rootConfig() throws Exception {
    request.setAttribute(Geoladris.ATTR_APP, null);
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/config.js");
    servlet.doGet(request, response);
    verify(config).doGet(request, response);
  }

  @Test
  public void subappConfig() throws Exception {
    request.setAttribute(Geoladris.ATTR_APP, "myapp");
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/myapp/config.js");
    servlet.doGet(request, response);
    verify(config).doGet(request, response);
  }

  @Test
  public void rootMain() throws Exception {
    request.setAttribute(Geoladris.ATTR_APP, null);
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/modules/main.js");
    servlet.doGet(request, response);
    verify(main).doGet(request, response);
  }

  @Test
  public void subappMain() throws Exception {
    request.setAttribute(Geoladris.ATTR_APP, "myapp");
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/myapp/modules/main.js");
    servlet.doGet(request, response);
    verify(main).doGet(request, response);
  }

  @Test
  public void rootClient() throws Exception {
    request.setAttribute(Geoladris.ATTR_APP, null);
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/jslib/a.js");
    servlet.doGet(request, response);
    verify(clientContent).doGet(request, response);
  }

  @Test
  public void subappClient() throws Exception {
    request.setAttribute(Geoladris.ATTR_APP, "myapp");
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/myapp/jslib/a.js");
    servlet.doGet(request, response);
    verify(clientContent).doGet(request, response);
  }

  @Test
  public void invalidRequest() throws Exception {
    request.setAttribute(Geoladris.ATTR_APP, "myapp");
    when(request.getRequestURI()).thenReturn("/" + CONTEXT_PATH + "/myapp2/jslib/a.js");
    servlet.doGet(request, response);
    verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
  }
}
