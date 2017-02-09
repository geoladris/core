package org.geoladris.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DelegateServlet extends HttpServlet {
  private static final String SUBAPP_REGEX = "^(/.+)?";

  private ClientContentServlet clientContent = new ClientContentServlet();
  private IndexHTMLServlet index = new IndexHTMLServlet();
  private MainJSServlet main = new MainJSServlet();
  private ConfigServlet config = new ConfigServlet();

  @Override
  public void init(ServletConfig servletConfig) throws ServletException {
    super.init(servletConfig);

    clientContent.init(servletConfig);
    index.init(servletConfig);
    main.init(servletConfig);
    config.init(servletConfig);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String uri = req.getRequestURI();
    if (uri.matches(SUBAPP_REGEX + "/$")) {
      index.doGet(req, resp);
    } else if (uri.matches(SUBAPP_REGEX + "/config.js$")) {
      config.doGet(req, resp);
    } else if (uri.matches(SUBAPP_REGEX + "/modules/main.js$")) {
      main.doGet(req, resp);
    } else {
      clientContent.doGet(req, resp);
    }
  }
}
