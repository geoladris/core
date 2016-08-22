package org.fao.unredd.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.fao.unredd.AppContextListener;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import net.sf.json.JSONArray;
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
	public void usesModulesFromPluginsInConfiguration() throws Exception {
		Locale locale = Locale.ROOT;

		PluginDescriptor plugin1 = new PluginDescriptor();
		plugin1.setName("plugin1");
		plugin1.getModules().add("module1");
		String pluginConf1 = "{module1 : {prop1 : 42, prop2 : true}}";

		Map<PluginDescriptor, JSONObject> pluginConf = new HashMap<PluginDescriptor, JSONObject>();
		pluginConf.put(plugin1, JSONObject.fromObject(pluginConf1));

		mockEmptyConfig();
		when(request.getAttribute(LangFilter.ATTR_LOCALE)).thenReturn(locale);
		when(config.getPluginConfig(locale, request)).thenReturn(pluginConf);

		servlet.doGet(request, response, context);

		String content = response();
		// It starts with var require = {...
		JSONObject json = JSONObject
				.fromObject(response().substring(content.indexOf('{')));
		JSONArray modules = json.getJSONObject("config")
				.getJSONObject("customization").getJSONArray("modules");
		assertEquals(1, modules.size());
		assertEquals("module1", modules.get(0));
	}

	@Test
	public void writesRequireJSConfigurationAsReturnedByConfig()
			throws Exception {
		Locale locale = Locale.ROOT;

		PluginDescriptor plugin1 = new PluginDescriptor();
		plugin1.setName("plugin1");
		plugin1.getModules().add("module1");
		PluginDescriptor plugin2 = new PluginDescriptor();
		plugin2.setName("plugin2");
		plugin2.getModules().add("module2");
		plugin2.getModules().add("module3");
		String pluginConf1 = "{module1 : {prop1 : 42, prop2 : true}}";
		String pluginConf2 = "{module2 : {prop3 : 'test'},"
				+ "module3 : [4, 2, 9]}";

		Map<PluginDescriptor, JSONObject> pluginConf = new HashMap<PluginDescriptor, JSONObject>();
		pluginConf.put(plugin1, JSONObject.fromObject(pluginConf1));
		pluginConf.put(plugin2, JSONObject.fromObject(pluginConf2));

		mockEmptyConfig();
		when(request.getAttribute(LangFilter.ATTR_LOCALE)).thenReturn(locale);
		when(config.getPluginConfig(locale, request)).thenReturn(pluginConf);

		servlet.doGet(request, response, context);

		String content = response();
		// It starts with var require = {...
		JSONObject json = JSONObject
				.fromObject(response().substring(content.indexOf('{')));
		JSONObject cfg = json.getJSONObject("config");
		JSONObject module1 = cfg.getJSONObject("module1");
		JSONObject module2 = cfg.getJSONObject("module2");
		JSONArray module3 = cfg.getJSONArray("module3");

		assertEquals(42, module1.getInt("prop1"));
		assertTrue(module1.getBoolean("prop2"));
		assertEquals("test", module2.getString("prop3"));
		assertEquals(4, module3.get(0));
		assertEquals(2, module3.get(1));
		assertEquals(9, module3.get(2));
	}

	@Test
	public void ignoresNullConfigurations() throws Exception {
		mockEmptyConfig();
		when(request.getAttribute(LangFilter.ATTR_LOCALE))
				.thenReturn(Locale.ROOT);
		when(config.getPluginConfig(Locale.ROOT, request)).thenReturn(
				Collections.<PluginDescriptor, JSONObject>singletonMap(
						new PluginDescriptor(), null));

		servlet.doGet(request, response, context);

		// It starts with var require = {...
		JSONObject json = JSONObject
				.fromObject(response().substring(response().indexOf('{')));
		JSONObject cfg = json.getJSONObject("config");
		// customization, i18n and url-parameters are always there
		assertEquals(3, cfg.keySet().size());
	}

	private void mockEmptyConfig() {
		ResourceBundle bundle = ResourceBundle.getBundle("messages");
		when(this.config.getMessages(any(Locale.class))).thenReturn(bundle);
		when(this.config.getProperties()).thenReturn(new Properties());
	}

	private String response() throws IOException {
		this.response.getWriter().flush();
		this.response.getWriter().close();
		this.stream.flush();
		this.stream.close();
		return this.stream.toString();
	}
}
