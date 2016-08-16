package org.fao.unredd.portal;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.junit.Test;

public class ConfigServletTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testCustomizationModule() throws ServletException, IOException {
		Config config = mock(Config.class);
		when(config.getMessages(any(Locale.class))).thenReturn(
				new PropertyResourceBundle(
						new ByteArrayInputStream(new byte[0])));
		Properties portalProperties = new Properties();
		portalProperties.put("languages", "{\"es\": \"Espa\u00f1ol\"}");
		portalProperties.put("languages.default", "es");
		portalProperties.put("client.modules", "");
		portalProperties.put("map.initialZoomLevel", "5");
		portalProperties.put("moreproperties", "should not appear");
		when(config.getProperties()).thenReturn(portalProperties);
		List<Map<String, String>> languages = new ArrayList<Map<String, String>>();
		HashMap<String, String> spanish = new HashMap<String, String>();
		spanish.put("code", "es");
		spanish.put("name", "Español");
		languages.add(spanish);
		when(config.getLanguages()).thenReturn(languages.toArray(new Map[0]));
		when(config.getPropertyAsArray(Config.PROPERTY_MAP_CENTER)).thenReturn(
				new String[] { "0", "0" });
		when(config.getPropertyAsArray(Config.PROPERTY_CLIENT_MODULES))
				.thenReturn(new String[0]);

		HttpServletRequest req = mock(HttpServletRequest.class);
		when(req.getAttribute("locale")).thenReturn(new Locale("es"));
		HttpServletResponse resp = mock(HttpServletResponse.class);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(baos);
		when(resp.getWriter()).thenReturn(writer);

		ConfigServlet servlet = getInitializedServlet(config);
		servlet.doGet(req, resp);
		writer.close();

		String response = new String(baos.toByteArray());
		System.out.println(response);

		assertTrue(response.contains("languages"));
		assertTrue(response.contains("languageCode"));
		assertTrue(response.contains("title"));
		assertTrue(response.contains(Config.PROPERTY_MAP_CENTER));
		assertTrue(response.contains("map.initialZoomLevel"));
		assertTrue(response.contains("modules"));
		assertFalse(response.contains("moreproperties"));
	}

	private ConfigServlet getInitializedServlet(Config config)
			throws ServletException {
		ConfigServlet servlet = new ConfigServlet();
		ServletConfig servletConfig = mock(ServletConfig.class);
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getAttribute("config")).thenReturn(config);
		when(servletContext.getAttribute("js-paths")).thenReturn(
				new ArrayList<String>());
		when(servletContext.getAttribute("plugin-configuration")).thenReturn(
				new HashMap<String, JSONObject>());
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		servlet.init(servletConfig);
		return servlet;
	}

}
