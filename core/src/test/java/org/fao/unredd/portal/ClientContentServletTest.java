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
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.fao.unredd.AppContextListener;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

public class ClientContentServletTest {
	@Captor
	private ArgumentCaptor<Config> configCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	/**
	 * Mock ServletContext and initialize {@link AppContextListener} with it.
	 * Capture {@link Config} instance.
	 * 
	 * @param folder
	 */
	public void setupConfigurationFolder(String folder) {
		AppContextListener listener = new AppContextListener();
		ServletContextEvent servletContextEvent = mock(
				ServletContextEvent.class);
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getInitParameter("PORTAL_CONFIG_DIR"))
				.thenReturn(folder);
		when(servletContext.getResourcePaths("/WEB-INF/lib"))
				.thenReturn(new HashSet<String>());
		when(servletContext.getRealPath("/WEB-INF/classes/"))
				.thenReturn(folder + "/WEB-INF/classes");
		when(servletContextEvent.getServletContext())
				.thenReturn(servletContext);
		listener.contextInitialized(servletContextEvent);

		verify(servletContext).setAttribute(eq(AppContextListener.ATTR_CONFIG),
				configCaptor.capture());
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

	/**
	 * Get descriptors from config instance and query all modules, styles and
	 * libs to the {@link ClientContentServlet}
	 * 
	 * @param classpathPrefix
	 * @throws ServletException
	 * @throws IOException
	 */
	private void requirePaths(String classpathPrefix)
			throws ServletException, IOException {
		Config config = configCaptor.getValue();

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getSession()).thenReturn(mock(HttpSession.class));

		PluginDescriptors pluginDescriptors = config
				.getPluginConfig(Locale.ROOT, request);

		List<String> modules = new ArrayList<>();
		List<String> styles = new ArrayList<>();
		List<String> nonRequirePaths = new ArrayList<>();
		PluginDescriptor[] enabled = pluginDescriptors.getEnabled();

		for (PluginDescriptor plugin : enabled) {
			modules.addAll(plugin.getModules());
			styles.addAll(plugin.getStylesheets());
			nonRequirePaths.addAll(plugin.getRequireJSPathsMap().values());
		}

		for (int i = 0; i < modules.size(); i++) {
			modules.set(i, "/modules/" + modules.get(i) + ".js");
		}
		testRequest(config, modules, classpathPrefix);

		for (int i = 0; i < nonRequirePaths.size(); i++) {
			nonRequirePaths.set(i, nonRequirePaths.get(i).substring(2) + ".js");
		}
		testRequest(config, nonRequirePaths, classpathPrefix);

		for (int i = 0; i < styles.size(); i++) {
			styles.set(i, "/" + styles.get(i));
		}
		testRequest(config, styles, classpathPrefix);
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

			verify(response).setStatus(HttpServletResponse.SC_OK);
		}
	}

	@Test
	public void test404() throws ServletException, IOException {
		setupConfigurationFolder("src/test/resources/testNoJavaPlugins");

		ClientContentServlet servlet = new ClientContentServlet();
		servlet.setTestingClasspathRoot("/testNoJavaPlugins/WEB-INF/classes/");
		ServletConfig servletConfig = mock(ServletConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getAttribute("config"))
				.thenReturn(configCaptor.getValue());
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

	@Test
	public void test304NotModified() throws ServletException, IOException {
		setupConfigurationFolder("src/test/resources/testNoJavaPlugins");

		ClientContentServlet servlet = new ClientContentServlet();
		servlet.setTestingClasspathRoot("/testNoJavaPlugins/WEB-INF/classes/");
		ServletConfig servletConfig = mock(ServletConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getAttribute("config"))
				.thenReturn(configCaptor.getValue());
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		servlet.init(servletConfig);

		HttpServletResponse response = mock(HttpServletResponse.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getServletPath()).thenReturn("/modules/plugin1/");
		when(request.getPathInfo()).thenReturn("a.js");
		when(request.getDateHeader("If-Modified-Since"))
				.thenReturn(System.currentTimeMillis());

		servlet.doGet(request, response);

		verify(response).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
	}

}
