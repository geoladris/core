package org.geoladris.servlet;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.geoladris.Geoladris;
import org.geoladris.config.Config;

public class LangFilter implements Filter {
  public static final String PATH_SETLANG = "/setlang";
  private Config config;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.config = (Config) filterConfig.getServletContext().getAttribute(Geoladris.ATTR_CONFIG);
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;

    HttpSession session = request.getSession();
    if (PATH_SETLANG.equals(request.getServletPath())) {
      String lang = request.getQueryString();
      if (lang == null) {
        lang = config.getDefaultLang();
      }

      session.setAttribute(Geoladris.ATTR_LOCALE, get(lang, request));
      response.sendRedirect(request.getContextPath());
    } else if (session.getAttribute(Geoladris.ATTR_LOCALE) == null) {
      session.setAttribute(Geoladris.ATTR_LOCALE, get(config.getDefaultLang(), request));
      chain.doFilter(req, resp);
    } else {
      chain.doFilter(req, resp);
    }
  }

  private Locale get(String lang, ServletRequest request) {
    return (lang != null && lang.trim().length() > 0) ? new Locale(lang) : request.getLocale();
  }

  @Override
  public void destroy() {}

}
