package org.geoladris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

import net.sf.json.JSONObject;

public class PluginDescriptorTest {
  private PluginDescriptorFileReader reader = new PluginDescriptorFileReader();

  public void cannotHaveNullName() {
    try {
      new PluginDescriptor(null, false);
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void modulesAndStylesInstalledInRoot() throws Exception {
    PluginDescriptor descriptor = new PluginDescriptor("p1", true);

    descriptor.addModule("m1");
    descriptor.addStylesheet("modules/s1.css");
    descriptor.addStylesheet("styles/s1.css");
    descriptor.addStylesheet("theme/s1.css");

    Set<String> modules = descriptor.getModules();
    assertEquals(1, modules.size());
    assertTrue(modules.contains("m1"));
    Set<String> styles = descriptor.getStylesheets();
    assertEquals(3, styles.size());
    assertTrue(styles.contains("modules/s1.css"));
    assertTrue(styles.contains("styles/s1.css"));
    assertTrue(styles.contains("theme/s1.css"));
  }

  @Test
  public void requirePathsAndShimsInstalledInRoot() throws Exception {
    String paths = "jquery : '../jslib/jquery/jquery', "
        + "openlayers : '../jslib/OpenLayers/OpenLayers.unredd'";
    String shim = "'fancy-box' : ['jquery']";
    String config =
        "{installInRoot:true, requirejs : { paths : {" + paths + "}, shim : {" + shim + "}}}";

    PluginDescriptor descriptor = reader.read(config, "p1");

    Map<String, String> pathsMap = descriptor.getRequireJSPathsMap();
    assertEquals(2, pathsMap.size());
    assertTrue(pathsMap.containsKey("openlayers"));
    assertTrue(pathsMap.containsKey("jquery"));
    assertEquals("../jslib/OpenLayers/OpenLayers.unredd", pathsMap.get("openlayers"));
    assertEquals("../jslib/jquery/jquery", pathsMap.get("jquery"));
    Map<String, String> shimMap = descriptor.getRequireJSShims();
    assertEquals(1, shimMap.size());
    assertTrue(shimMap.containsKey("fancy-box"));
    assertEquals("[\"jquery\"]", shimMap.get("fancy-box"));
  }

  @Test
  public void modulesAndStylesInstalledOutsideRoot() throws Exception {
    String name = "myplugin";
    PluginDescriptor descriptor = new PluginDescriptor(name, false);

    descriptor.addModule("m1");
    descriptor.addStylesheet("modules/s1.css");
    descriptor.addStylesheet("styles/s1.css");
    descriptor.addStylesheet("theme/s1.css");

    Set<String> modules = descriptor.getModules();
    assertEquals(1, modules.size());
    assertTrue(modules.contains(name + "/m1"));
    Set<String> styles = descriptor.getStylesheets();
    assertEquals(3, styles.size());
    assertTrue(styles.contains("modules/" + name + "/s1.css"));
    assertTrue(styles.contains("styles/" + name + "/s1.css"));
    assertTrue(styles.contains("theme/" + name + "/s1.css"));
  }

  @Test
  public void requirePathsAndShimsInstalledOutsideRoot() throws Exception {
    String name = "myplugin";

    String paths = "jquery : '../jslib/jquery/jquery', "
        + "openlayers : '../jslib/OpenLayers/OpenLayers.unredd'";
    String shim = "'fancy-box' : ['jquery']";
    String config =
        "{installInRoot:false, requirejs : { paths : {" + paths + "}, shim : {" + shim + "}}}";

    PluginDescriptor descriptor = reader.read(config, name);

    Map<String, String> pathsMap = descriptor.getRequireJSPathsMap();
    assertEquals(2, pathsMap.size());
    assertTrue(pathsMap.containsKey("openlayers"));
    assertTrue(pathsMap.containsKey("jquery"));
    assertEquals("../jslib/" + name + "/OpenLayers/OpenLayers.unredd", pathsMap.get("openlayers"));
    assertEquals("../jslib/" + name + "/jquery/jquery", pathsMap.get("jquery"));
    Map<String, String> shimMap = descriptor.getRequireJSShims();
    assertEquals(1, shimMap.size());
    assertTrue(shimMap.containsKey("fancy-box"));
    assertEquals("[\"jquery\"]", shimMap.get("fancy-box"));
  }

  @Test
  public void qualifiesModuleNames() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);
    plugin.addModule("m1");
    Set<String> modules = plugin.getModules();
    assertTrue(modules.contains("p/m1"));
    assertEquals(1, modules.size());
  }

  @Test
  public void qualifiesStylesheets() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);
    plugin.addStylesheet("theme/style.css");
    Set<String> stylesheets = plugin.getStylesheets();
    assertTrue(stylesheets.contains("theme/p/style.css"));
    assertEquals(1, stylesheets.size());
  }

  @Test
  public void qualifiesRequireJSPaths() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);
    plugin.addRequireJSPath("jquery", "jslib/jquery-2.10");
    Map<String, String> paths = plugin.getRequireJSPathsMap();
    assertEquals(1, paths.size());
    assertEquals("jslib/p/jquery-2.10", paths.get("jquery"));
  }

  @Test
  public void qualifiesRequireJSShim() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);
    plugin.addRequireJSShim("mustache", "jslib/jquery-2.10");
    Map<String, String> shim = plugin.getRequireJSShims();
    assertEquals(1, shim.size());
    assertEquals("jslib/p/jquery-2.10", shim.get("mustache"));
  }

  @Test
  public void enabledByDefault() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);
    assertTrue(plugin.isEnabled());
  }

  @Test
  public void disablePluginWithConfig() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);
    plugin.setConfiguration(
        JSONObject.fromObject("{'" + PluginDescriptor.CONF_ENABLED + "' : false}"));
    assertFalse(plugin.isEnabled());
  }

  @Test
  public void enablePluginWithConfig() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);
    plugin.setConfiguration(
        JSONObject.fromObject("{'" + PluginDescriptor.CONF_ENABLED + "' : false}"));
    assertFalse(plugin.isEnabled());
    plugin.setConfiguration(
        JSONObject.fromObject("{'" + PluginDescriptor.CONF_ENABLED + "' : true}"));
    assertTrue(plugin.isEnabled());
  }

  @Test
  public void overrideConfig() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);

    JSONObject config = new JSONObject();
    config.element("m1", JSONObject.fromObject("{a : 1}"));
    config.element("m2", true);

    plugin.setConfiguration(config);

    config = new JSONObject();
    config.element("m2", false);
    config.element(PluginDescriptor.CONF_OVERRIDE, true);
    plugin.setConfiguration(config);

    JSONObject pluginConfig = plugin.getConfiguration();
    assertFalse(pluginConfig.has("p/m1"));
    assertFalse(pluginConfig.getBoolean("p/m2"));
  }

  @Test
  public void mergeConfigIfSpecified() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);

    JSONObject config = new JSONObject();
    JSONObject m1Config = JSONObject.fromObject("{a : 1}");
    config.element("m1", m1Config);
    plugin.setConfiguration(config);

    config = new JSONObject();
    config.element("m2", false);
    config.element(PluginDescriptor.CONF_OVERRIDE, false);
    plugin.setConfiguration(config);

    JSONObject pluginConfig = plugin.getConfiguration();
    assertEquals(m1Config, pluginConfig.getJSONObject("p/m1"));
    assertFalse(pluginConfig.getBoolean("p/m2"));
  }

  @Test
  public void mergeConfigByDefault() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);

    JSONObject config = new JSONObject();
    JSONObject m1Config = JSONObject.fromObject("{a : 1}");
    config.element("m1", m1Config);
    plugin.setConfiguration(config);

    config = new JSONObject();
    config.element("m2", false);
    plugin.setConfiguration(config);

    JSONObject pluginConfig = plugin.getConfiguration();
    assertEquals(m1Config, pluginConfig.getJSONObject("p/m1"));
    assertFalse(pluginConfig.getBoolean("p/m2"));
  }

  @Test
  public void configDoesNotContainEnabledOrOverride() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);

    JSONObject config = new JSONObject();
    config.element(PluginDescriptor.CONF_ENABLED, true);
    config.element(PluginDescriptor.CONF_OVERRIDE, false);
    config.element("m1", true);

    plugin.setConfiguration(config);

    JSONObject pluginConfig = plugin.getConfiguration();
    assertEquals(1, pluginConfig.size());
    assertTrue(pluginConfig.getBoolean("p/m1"));
    assertFalse(pluginConfig.has(PluginDescriptor.CONF_ENABLED));
    assertFalse(pluginConfig.has(PluginDescriptor.CONF_OVERRIDE));
  }
}
