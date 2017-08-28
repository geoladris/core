package org.geoladris;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Context;
import org.apache.catalina.Globals;
import org.apache.catalina.webresources.StandardRoot;
import org.geoladris.config.Config;
import org.geoladris.config.ConfigImpl;
import org.geoladris.config.PluginConfigProvider;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public class TestingServletContext {
  public ServletContext servletContext;
  public ServletConfig servletConfig;
  public FilterConfig filterConfig;
  public ServletContextEvent event;
  public HttpSession session;
  public HttpServletRequest request;
  public HttpServletResponse response;

  private ByteArrayOutputStream out;
  private PrintWriter writer;

  private Map<String, Object> servletAttributes;
  private Map<String, Object> requestAttributes;

  public TestingServletContext() throws IOException {
    this.event = mock(ServletContextEvent.class);
    this.servletConfig = mock(ServletConfig.class);
    this.filterConfig = mock(FilterConfig.class);
    this.servletContext = mock(ServletContext.class);
    this.session = mock(HttpSession.class);
    this.response = mock(HttpServletResponse.class);

    this.servletAttributes = new HashMap<>();

    when(this.event.getServletContext()).thenReturn(this.servletContext);

    when(this.servletConfig.getServletContext()).thenReturn(this.servletContext);
    when(this.filterConfig.getServletContext()).thenReturn(this.servletContext);

    when(this.response.getWriter()).then(new Answer<PrintWriter>() {
      @Override
      public PrintWriter answer(InvocationOnMock invocation) throws Throwable {
        out = new ByteArrayOutputStream();
        writer = new PrintWriter(out);
        return writer;
      }
    });
    when(this.response.getOutputStream()).then(new Answer<ServletOutputStream>() {
      @Override
      public ServletOutputStream answer(InvocationOnMock invocation) throws Throwable {
        out = new ByteArrayOutputStream();
        return new ServletOutputStream() {
          @Override
          public void write(int b) throws IOException {
            out.write(b);
          }

          @Override
          public boolean isReady() {
            return true;
          }

          @Override
          public void setWriteListener(WriteListener arg0) {}
        };
      }
    });

    when(this.session.getServletContext()).thenReturn(this.servletContext);

    when(this.servletContext.getAttribute(anyString())).then(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        return servletAttributes.get(invocation.getArguments()[0].toString());
      }
    });
    StandardRoot root = new StandardRoot();
    root.setContext(mock(Context.class));
    when(this.servletContext.getAttribute(Globals.RESOURCES_ATTR)).thenReturn(root);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        servletAttributes.put(args[0].toString(), args[1]);
        return null;
      }
    }).when(this.servletContext).setAttribute(anyString(), any());

    resetRequest();
  }

  public Config mockConfig(File configDir) {
    File pluginsDir = new File(configDir, "plugins");
    PluginDirsAnalyzer analyzer =
        new PluginDirsAnalyzer(new File(servletContext.getRealPath("/plugins")), pluginsDir);
    Config config = new ConfigImpl(configDir, new ArrayList<PluginConfigProvider>(),
        analyzer.getPlugins(), false, -1);
    when(this.servletContext.getAttribute(Geoladris.ATTR_CONFIG)).thenReturn(config);

    return config;
  }

  public void setContextPath(String contextPath) {
    when(this.servletContext.getContextPath()).thenReturn(contextPath);
    when(this.request.getContextPath()).thenReturn(contextPath);
  }

  public void resetRequest() {
    this.request = mock(HttpServletRequest.class);
    this.requestAttributes = new HashMap<>();

    when(this.request.getSession()).thenReturn(this.session);
    when(this.request.getAttribute(anyString())).then(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        return requestAttributes.get(invocation.getArguments()[0].toString());
      }
    });
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        requestAttributes.put(args[0].toString(), args[1]);
        return null;
      }
    }).when(this.request).setAttribute(anyString(), any());

    String path = this.servletContext.getContextPath();
    when(this.request.getContextPath()).thenReturn(path);
  }

  public String getResponse() {
    this.writer.flush();
    this.writer.close();
    return this.out.toString();
  }
}
