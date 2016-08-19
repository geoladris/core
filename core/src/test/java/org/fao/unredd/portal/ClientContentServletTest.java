package org.fao.unredd.portal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fao.unredd.AppContextListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

public class ClientContentServletTest {
	@Captor
	private ArgumentCaptor<Config> configCaptor;
	@Captor
	private ArgumentCaptor<List<String>> jsPathsCaptor;
	@Captor
	private ArgumentCaptor<List<String>> cssPathsCaptor;
	@Captor
	private ArgumentCaptor<Map<String, String>> nonRequirePaths;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	public void setupConfigurationFolder(String folder) {

		AppContextListener listener = new AppContextListener();
		ServletContextEvent servletContextEvent = mock(ServletContextEvent.class);
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getInitParameter("PORTAL_CONFIG_DIR")).thenReturn(
				folder);
		when(servletContext.getResourcePaths("/WEB-INF/lib")).thenReturn(
				new HashSet<String>());
		when(servletContext.getRealPath("/WEB-INF/classes/")).thenReturn(
				folder + "/WEB-INF/classes");
		when(servletContextEvent.getServletContext())
				.thenReturn(servletContext);
		listener.contextInitialized(servletContextEvent);

		verify(servletContext).setAttribute(eq("config"),
				configCaptor.capture());
		verify(servletContext).setAttribute(eq("js-paths"),
				jsPathsCaptor.capture());
		verify(servletContext).setAttribute(eq("css-paths"),
				cssPathsCaptor.capture());
		verify(servletContext).setAttribute(eq("requirejs-paths"),
				nonRequirePaths.capture());
	}

	@Test
	public void scanNoJavaPlugins() throws ServletException, IOException {
		setupConfigurationFolder("src/test/resources/testNoJavaPlugins");
		requirePaths("/testNoJavaPlugins");
	}

	@Test
	public void scanJavaRootModules() throws ServletException, IOException {
		setupConfigurationFolder("src/test/resources/testJavaNonRootModules");
		requirePaths("/testJavaNonRootModules");
	}

	@Test
	public void scanJavaRootSubfolders() throws ServletException, IOException {
		setupConfigurationFolder("src/test/resources/testJavaRootSubfolders");
		requirePaths("/testJavaRootSubfolders");
	}

	private void requirePaths(String classpathPrefix) throws ServletException,
			IOException {
		Config config = configCaptor.getValue();
		List<String> paths = jsPathsCaptor.getValue();
		for (int i = 0; i < paths.size(); i++) {
			paths.set(i, "/modules/" + paths.get(i) + ".js");
		}
		testRequest(config, paths, classpathPrefix);

		paths = new ArrayList<String>(nonRequirePaths.getValue().values());
		for (int i = 0; i < paths.size(); i++) {
			paths.set(i, paths.get(i).substring(2) + ".js");
		}
		testRequest(config, paths, classpathPrefix);

		paths = new ArrayList<String>(cssPathsCaptor.getValue());
		for (int i = 0; i < paths.size(); i++) {
			paths.set(i, "/" + paths.get(i));
		}
		testRequest(config, paths, classpathPrefix);
	}

	private void testRequest(Config config, Collection<String> collection,
			String classpathPrefix) throws ServletException, IOException {

		ClientContentServlet servlet = new ClientContentServlet();
		servlet.setTestingClasspathRoot(classpathPrefix + "/WEB-INF/classes/");
		ServletConfig servletConfig = mock(ServletConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getAttribute("config")).thenReturn(config);
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		servlet.init(servletConfig);

		assertTrue(collection.size() > 0);
		for (String path : collection) {
			int secondSlashIndex = path.indexOf("/", 1);
			String servletPath = path.substring(0, secondSlashIndex);
			String pathInfo = path.substring(secondSlashIndex);
			HttpServletResponse response = mock(HttpServletResponse.class);
			HttpServletRequest request = mock(HttpServletRequest.class);
			when(request.getServletPath()).thenReturn(servletPath);
			when(request.getPathInfo()).thenReturn(pathInfo);
			servlet.doGet(request, response);

			verify(response).setStatus(AdditionalMatchers.not(eq(404)));
		}
	}

	@Test
	public void test404() throws ServletException, IOException {
		setupConfigurationFolder("src/test/resources/testNoJavaPlugins");

		ClientContentServlet servlet = new ClientContentServlet();
		servlet.setTestingClasspathRoot("/testNoJavaPlugins/WEB-INF/classes/");
		ServletConfig servletConfig = mock(ServletConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getAttribute("config")).thenReturn(
				configCaptor.getValue());
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		servlet.init(servletConfig);

		check404(servlet, "/static/", "/parabailarlabamba");
		check404(servlet, "/modules/", "j/module-notexists");
		check404(servlet, "/modules/", "plugin1/module-notexists");
	}

	private void check404(ClientContentServlet servlet, String servletPath,
			String pathInfo) throws ServletException, IOException {
		HttpServletResponse response = mock(HttpServletResponse.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getServletPath()).thenReturn(servletPath);
		when(request.getPathInfo()).thenReturn(pathInfo);

		try {
			servlet.doGet(request, response);
			fail();
		} catch (StatusServletException e) {
			assertEquals(404, e.getStatus());
		}
	}

}
