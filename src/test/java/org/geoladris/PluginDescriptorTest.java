package org.geoladris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import org.junit.Test;

import net.sf.json.JSONObject;

public class PluginDescriptorTest {
  public void cannotHaveNullName() {
    try {
      new PluginDescriptor(null, false);
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void modulesInstalledInRoot() throws Exception {
    PluginDescriptor descriptor = new PluginDescriptor("p1", true);

    descriptor.addModule("m1");

    Set<String> modules = descriptor.getModules();
    assertEquals(1, modules.size());
    assertTrue(modules.contains("m1"));
  }

  @Test
  public void modulesInstalledOutsideRoot() throws Exception {
    String name = "myplugin";
    PluginDescriptor descriptor = new PluginDescriptor(name, false);

    descriptor.addModule("m1");

    Set<String> modules = descriptor.getModules();
    assertEquals(1, modules.size());
    assertTrue(modules.contains(name + "/m1"));
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

  @Test
  public void keepEnabledStateIfNotSpecified() {
    PluginDescriptor plugin = new PluginDescriptor("p", false);
    plugin.setConfiguration(
        JSONObject.fromObject("{'" + PluginDescriptor.CONF_ENABLED + "' : false}"));
    assertFalse(plugin.isEnabled());
    plugin.setConfiguration(new JSONObject());
    assertFalse(plugin.isEnabled());
  }
}
