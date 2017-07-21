package org.geoladris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import org.junit.Test;

import net.sf.json.JSONObject;

public class PluginTest {
  public void cannotHaveNullName() {
    try {
      new Plugin(null, false);
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  @Test
  public void modulesInstalledInRoot() throws Exception {
    Plugin descriptor = new Plugin("p1", true);

    descriptor.addModule("m1");

    Set<String> modules = descriptor.getModules();
    assertEquals(1, modules.size());
    assertTrue(modules.contains("m1"));
  }

  @Test
  public void modulesInstalledOutsideRoot() throws Exception {
    String name = "myplugin";
    Plugin descriptor = new Plugin(name, false);

    descriptor.addModule("m1");

    Set<String> modules = descriptor.getModules();
    assertEquals(1, modules.size());
    assertTrue(modules.contains(name + "/m1"));
  }

  @Test
  public void qualifiesModuleNames() {
    Plugin plugin = new Plugin("p", false);
    plugin.addModule("m1");
    Set<String> modules = plugin.getModules();
    assertTrue(modules.contains("p/m1"));
    assertEquals(1, modules.size());
  }

  @Test
  public void enabledByDefault() {
    Plugin plugin = new Plugin("p", false);
    assertTrue(plugin.isEnabled());
  }

  @Test
  public void disablePluginWithConfig() {
    Plugin plugin = new Plugin("p", false);
    plugin.setConfiguration(JSONObject.fromObject("{'" + Plugin.CONF_ENABLED + "' : false}"));
    assertFalse(plugin.isEnabled());
  }

  @Test
  public void enablePluginWithConfig() {
    Plugin plugin = new Plugin("p", false);
    plugin.setConfiguration(JSONObject.fromObject("{'" + Plugin.CONF_ENABLED + "' : false}"));
    assertFalse(plugin.isEnabled());
    plugin.setConfiguration(JSONObject.fromObject("{'" + Plugin.CONF_ENABLED + "' : true}"));
    assertTrue(plugin.isEnabled());
  }

  @Test
  public void overrideConfig() {
    Plugin plugin = new Plugin("p", false);

    JSONObject config = new JSONObject();
    config.element("m1", JSONObject.fromObject("{a : 1}"));
    config.element("m2", true);

    plugin.setConfiguration(config);

    config = new JSONObject();
    config.element("m2", false);
    config.element(Plugin.CONF_OVERRIDE, true);
    plugin.setConfiguration(config);

    JSONObject pluginConfig = plugin.getConfiguration();
    assertFalse(pluginConfig.has("p/m1"));
    assertFalse(pluginConfig.getBoolean("p/m2"));
  }

  @Test
  public void mergeConfigIfSpecified() {
    Plugin plugin = new Plugin("p", false);

    JSONObject config = new JSONObject();
    JSONObject m1Config = JSONObject.fromObject("{a : 1}");
    config.element("m1", m1Config);
    plugin.setConfiguration(config);

    config = new JSONObject();
    config.element("m2", false);
    config.element(Plugin.CONF_OVERRIDE, false);
    plugin.setConfiguration(config);

    JSONObject pluginConfig = plugin.getConfiguration();
    assertEquals(m1Config, pluginConfig.getJSONObject("p/m1"));
    assertFalse(pluginConfig.getBoolean("p/m2"));
  }

  @Test
  public void mergeConfigByDefault() {
    Plugin plugin = new Plugin("p", false);

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
    Plugin plugin = new Plugin("p", false);

    JSONObject config = new JSONObject();
    config.element(Plugin.CONF_ENABLED, true);
    config.element(Plugin.CONF_OVERRIDE, false);
    config.element("m1", true);

    plugin.setConfiguration(config);

    JSONObject pluginConfig = plugin.getConfiguration();
    assertEquals(1, pluginConfig.size());
    assertTrue(pluginConfig.getBoolean("p/m1"));
    assertFalse(pluginConfig.has(Plugin.CONF_ENABLED));
    assertFalse(pluginConfig.has(Plugin.CONF_OVERRIDE));
  }

  @Test
  public void keepEnabledStateIfNotSpecified() {
    Plugin plugin = new Plugin("p", false);
    plugin.setConfiguration(JSONObject.fromObject("{'" + Plugin.CONF_ENABLED + "' : false}"));
    assertFalse(plugin.isEnabled());
    plugin.setConfiguration(new JSONObject());
    assertFalse(plugin.isEnabled());
  }

  @Test
  public void installInRoot() {
    Plugin plugin = new Plugin("p", false);
    assertFalse(plugin.isInstallInRoot());

    plugin = new Plugin("p", true);
    assertTrue(plugin.isInstallInRoot());

    plugin = new Plugin("p", JSONObject.fromObject("{ installInRoot : true}"));
    assertTrue(plugin.isInstallInRoot());

    plugin = new Plugin("p", JSONObject.fromObject("{ installInRoot : false}"));
    assertFalse(plugin.isInstallInRoot());
  }

  @Test
  public void toStringReturnsName() {
    String name = "p";
    assertEquals(name, new Plugin(name, false).toString());
  }

  @Test
  public void nameCannotBeNull() {
    try {
      new Plugin(null, false);
      fail();
    } catch (IllegalArgumentException e) {
    }

    try {
      new Plugin("", false);
      fail();
    } catch (IllegalArgumentException e) {
    }
  }
}
