package org.fao.unredd.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fao.unredd.AppContextListener;
import org.junit.Before;
import org.junit.Test;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

public class ConfigServletTest {
	private ConfigServlet servlet;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private ServletContext context;
	private Config config;
	private ByteArrayOutputStream stream;

	@Before
	public void setup() throws IOException {
		this.servlet = new ConfigServlet();
		this.request = mock(HttpServletRequest.class);
		this.response = mock(HttpServletResponse.class);
		this.config = mock(Config.class);
		this.context = mock(ServletContext.class);
		this.stream = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(this.stream);
		when(this.context.getAttribute(AppContextListener.ATTR_CONFIG))
				.thenReturn(this.config);
		when(this.response.getWriter()).thenReturn(writer);
	}

	@Test
	public void mergesConfIfSpecified() throws Exception {
		Locale locale = Locale.ROOT;

		HashMap<String, JSON> defaultConf = new HashMap<String, JSON>();
		defaultConf.put("module1", JSONObject.fromObject("{'a' : 1, 'b' : 2}"));
		defaultConf.put("module2", JSONObject.fromObject("{'c' : 3, 'd' : 4}"));

		HashMap<String, JSON> pluginConf = new HashMap<String, JSON>();
		pluginConf.put("module1", JSONObject.fromObject("{'a' : 10, 'x' : 9}"));
		pluginConf.put("module2", JSONObject.fromObject("{'c' : 30}"));

		Set<String> mergeConfModules = new HashSet<String>();
		mergeConfModules.add("module1");

		mockEmptyConfig();
		when(request.getAttribute(LangFilter.ATTR_LOCALE)).thenReturn(locale);
		when(context.getAttribute(AppContextListener.ATTR_JS_PATHS))
				.thenReturn(new ArrayList<String>());
		when(context.getAttribute(AppContextListener.ATTR_MERGE_CONF_MODULES))
				.thenReturn(mergeConfModules);
		when(context.getAttribute(AppContextListener.ATTR_PLUGIN_CONFIGURATION))
				.thenReturn(defaultConf);
		when(config.getPluginConfig(locale, request)).thenReturn(pluginConf);

		servlet.doGet(request, response, context);

		String content = content();
		// It starts with var require = {...
		JSONObject json = JSONObject
				.fromObject(content().substring(content.indexOf('{')));
		JSONObject module1 = json.getJSONObject("config")
				.getJSONObject("module1");
		System.out.println(module1);
		assertEquals(10, module1.getInt("a"));
		assertEquals(2, module1.getInt("b"));
		assertEquals(9, module1.getInt("x"));

		JSONObject module2 = json.getJSONObject("config")
				.getJSONObject("module2");
		assertEquals(30, module2.getInt("c"));
		assertFalse(module2.has("d"));
	}

	private void mockEmptyConfig() {
		ResourceBundle bundle = ResourceBundle.getBundle("messages");
		when(this.config.getMessages(any(Locale.class))).thenReturn(bundle);
		when(this.config.getProperties()).thenReturn(new Properties());
	}

	private String content() throws IOException {
		this.response.getWriter().flush();
		this.response.getWriter().close();
		this.stream.flush();
		this.stream.close();
		return this.stream.toString();
	}
}
