package org.geoladris.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoladris.Geoladris;
import org.geoladris.PluginDescriptor;
import org.geoladris.TestingServletContext;
import org.geoladris.config.Config;
import org.junit.Before;
import org.junit.Test;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ConfigServletTest {
  private Config config;
  private ConfigServlet servlet;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private TestingServletContext context;

  @Before
  public void setup() throws Exception {
    this.config = mock(Config.class);
    this.servlet = new ConfigServlet();

    this.context = new TestingServletContext();
    this.response = context.response;
    this.request = context.request;

    this.servlet.init(context.servletConfig);
    context.servletContext.setAttribute(Geoladris.ATTR_CONFIG, this.config);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCustomizationModule() throws ServletException, IOException {
    when(config.getMessages(any(Locale.class)))
        .thenReturn(new PropertyResourceBundle(new ByteArrayInputStream(new byte[0])));
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
    when(config.getPropertyAsArray(Config.PROPERTY_MAP_CENTER)).thenReturn(new String[] {"0", "0"});
    when(config.getPropertyAsArray(Config.PROPERTY_CLIENT_MODULES)).thenReturn(new String[0]);

    when(config.getPluginConfig(any(Locale.class), eq(request)))
        .thenReturn(new PluginDescriptor[0]);

    request.setAttribute(Geoladris.ATTR_LOCALE, new Locale("es"));

    servlet.doGet(request, this.response);

    String response = context.getResponse();
    assertTrue(response.contains("languages"));
    assertTrue(response.contains("languageCode"));
    assertTrue(response.contains("title"));
    assertTrue(response.contains(Config.PROPERTY_MAP_CENTER));
    assertTrue(response.contains("map.initialZoomLevel"));
    assertTrue(response.contains("modules"));
    assertFalse(response.contains("moreproperties"));
  }

  @Test
  public void usesModulesFromPluginsInConfiguration() throws Exception {
    String defaultConf = "{default-conf:{module1 : {prop1 : 42, prop2 : true}}}";
    PluginDescriptor plugin1 = new PluginDescriptor("plugin1", JSONObject.fromObject(defaultConf));
    plugin1.addModule("module1");

    PluginDescriptor[] plugin = new PluginDescriptor[] {plugin1};
    mockEmptyConfig();
    this.request.setAttribute(Geoladris.ATTR_LOCALE, Locale.ROOT);
    when(config.getPluginConfig(Locale.ROOT, request)).thenReturn(plugin);

    servlet.doGet(this.request, response);

    String content = context.getResponse();
    // It starts with var require = {...
    JSONObject json = JSONObject.fromObject(content.substring(content.indexOf('{')));
    JSONArray modules =
        json.getJSONObject("config").getJSONObject("customization").getJSONArray("modules");
    assertEquals(1, modules.size());
    assertEquals("plugin1/module1", modules.get(0));
  }

  @Test
  public void writesRequireJSConfigurationAsReturnedByConfig() throws Exception {
    PluginDescriptor plugin1 = new PluginDescriptor("plugin1",
        JSONObject.fromObject("{default-conf:{module1 : {prop1 : 42, prop2 : true}}}"));
    plugin1.getModules().add("module1");
    PluginDescriptor plugin2 = new PluginDescriptor("plugin2", JSONObject
        .fromObject("{default-conf:{module2 : {prop3 : 'test'}," + "module3 : [4, 2, 9]}}"));
    plugin2.getModules().add("module2");
    plugin2.getModules().add("module3");

    PluginDescriptor[] plugins = new PluginDescriptor[] {plugin1, plugin2};

    mockEmptyConfig();
    request.setAttribute(Geoladris.ATTR_LOCALE, Locale.ROOT);
    when(config.getPluginConfig(Locale.ROOT, request)).thenReturn(plugins);

    servlet.doGet(request, response);

    String content = context.getResponse();
    // It starts with var require = {...
    JSONObject json = JSONObject.fromObject(content.substring(content.indexOf('{')));
    JSONObject cfg = json.getJSONObject("config");
    JSONObject module1 = cfg.getJSONObject("plugin1/module1");
    JSONObject module2 = cfg.getJSONObject("plugin2/module2");
    JSONArray module3 = cfg.getJSONArray("plugin2/module3");

    assertEquals(42, module1.getInt("prop1"));
    assertTrue(module1.getBoolean("prop2"));
    assertEquals("test", module2.getString("prop3"));
    assertEquals(4, module3.get(0));
    assertEquals(2, module3.get(1));
    assertEquals(9, module3.get(2));
  }

  private void mockEmptyConfig() {
    ResourceBundle bundle = ResourceBundle.getBundle("messages");
    when(this.config.getMessages(any(Locale.class))).thenReturn(bundle);
    when(this.config.getProperties()).thenReturn(new Properties());
  }
}
