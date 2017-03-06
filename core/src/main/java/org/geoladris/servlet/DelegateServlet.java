package org.geoladris.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoladris.Geoladris;

public class DelegateServlet extends HttpServlet {
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
    Object app = req.getAttribute(Geoladris.ATTR_APP);
    String regexPrefix = "^" + getServletContext().getContextPath();
    if (app != null) {
      regexPrefix += "/" + app;
    }

    String uri = req.getRequestURI();
    if (uri.matches(regexPrefix + "/$")) {
      index.doGet(req, resp);
    } else if (uri.matches(regexPrefix + "/config.js$")) {
      config.doGet(req, resp);
    } else if (uri.matches(regexPrefix + "/modules/main.js$")) {
      main.doGet(req, resp);
    } else if (uri.matches(regexPrefix + "/(optimized|static|jslib|modules|styles|theme)/.*")) {
      clientContent.doGet(req, resp);
    } else {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  /**
   * For testing purposes
   */
  void setDelegates(ClientContentServlet clientContent, IndexHTMLServlet index, MainJSServlet main,
      ConfigServlet config) {
    this.clientContent = clientContent;
    this.index = index;
    this.main = main;
    this.config = config;
  }
}
