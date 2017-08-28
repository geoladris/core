package org.geoladris.servlet;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import net.sf.json.JSONObject;

public class ErrorFilterTest {
  private ErrorFilter filter;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private FilterChain chain;
  private ByteArrayOutputStream out;

  @Before
  public void setup() throws Exception {
    this.filter = new ErrorFilter();

    this.filter.init(mock(FilterConfig.class));

    this.request = mock(HttpServletRequest.class);
    this.response = mock(HttpServletResponse.class);
    this.chain = mock(FilterChain.class);
    this.out = new ByteArrayOutputStream();
    when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
      @Override
      public void write(int b) throws IOException {
        out.write(b);
      }
      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setWriteListener(WriteListener arg0) {
      }
    });
  }

  @Test
  public void unknownException() throws Exception {
    doThrow(new IOException()).when(chain).doFilter(request, response);
    filter.doFilter(request, response, chain);

    verify(response).setStatus(500);
    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("utf8");

    assertEquals(JSONObject.fromObject("{ message : 'Server error: null. '}"),
        JSONObject.fromObject(this.out.toString()));
  }

  @Test
  public void statusServletException() throws Exception {
    doThrow(new StatusServletException(401, "Failed")).when(chain).doFilter(request, response);
    filter.doFilter(request, response, chain);

    verify(response).setStatus(401);
    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("utf8");

    assertEquals(JSONObject.fromObject("{ message : 'Server error: Failed. '}"),
        JSONObject.fromObject(this.out.toString()));
  }
}
