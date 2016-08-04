package org.fao.unredd.portal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

public class IndexHTMLServletTest {
	private IndexHTMLServlet servlet;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private ByteArrayOutputStream bos;
	private ServletContext servletContext;

	@Before
	public void setup() throws Exception {
		this.servlet = new IndexHTMLServlet();
		this.request = mock(HttpServletRequest.class);
		this.response = mock(HttpServletResponse.class);

		this.bos = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(this.bos);
		when(this.response.getWriter()).thenReturn(writer);

		this.servletContext = mock(ServletContext.class);

		ServletConfig servletConfig = mock(ServletConfig.class);
		when(servletConfig.getServletContext()).thenReturn(this.servletContext);
		this.servlet.init(servletConfig);
	}

	@Test
	public void cssOrder() throws Exception {
		List<String> css = new ArrayList<String>();
		Collections.addAll(css, "theme/a.css", "styles/a.css", "theme/b.css");

		when(this.servletContext.getAttribute("config"))
				.thenReturn(mock(Config.class));
		when(this.servletContext.getAttribute("css-paths")).thenReturn(css);

		this.servlet.doGet(this.request, this.response);
		this.response.getWriter().flush();
		this.bos.flush();

		String content = this.bos.toString();
		int i1 = content
				.indexOf("<link rel=\"stylesheet\" href=\"styles/a.css\">");
		int i2 = content
				.indexOf("<link rel=\"stylesheet\" href=\"theme/a.css\">");
		int i3 = content
				.indexOf("<link rel=\"stylesheet\" href=\"theme/b.css\">");
		assertTrue(i1 < i2);
		assertTrue(i1 < i3);
		assertTrue(i2 < i3);
	}
}
