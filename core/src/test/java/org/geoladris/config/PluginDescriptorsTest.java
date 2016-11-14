package org.geoladris.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.geoladris.PluginDescriptor;
import org.geoladris.PluginDescriptorFileReader;
import org.geoladris.config.PluginDescriptors;
import org.junit.Test;

import net.sf.json.JSONObject;

public class PluginDescriptorsTest {

  @Test
  public void doesNotReturnDisabledPlugins() throws Exception {
    PluginDescriptors pluginDescriptors =
        new PluginDescriptors(Collections.<PluginDescriptor>emptySet());
    pluginDescriptors.merge("p1", JSONObject
        .fromObject("{_enabled : false, " + "m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}"));
    pluginDescriptors.merge("p2", JSONObject.fromObject("{_enabled : true, m3 : { a : 1, b : 2}}"));

    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();

    assertEquals(1, descriptors.length);
    JSONObject pluginConf = descriptors[0].getConfiguration();
    assertEquals(1, pluginConf.keySet().size());
    assertEquals("m3", pluginConf.keySet().iterator().next());
  }

  @Test
  public void pluginsEnabledByDefault() throws Exception {
    PluginDescriptors pluginDescriptors =
        new PluginDescriptors(Collections.<PluginDescriptor>emptySet());

    pluginDescriptors.merge("p1",
        JSONObject.fromObject("{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}"));

    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
    assertEquals(1, descriptors.length);
  }

  @Test
  public void defaultConfigurationMergedByDefault() throws Exception {
    checkConfigurationMerge("{ _enabled : true, m1 : { a : 10}}");
  }

  @Test
  public void mergesDefaultConfIfSpecified() throws Exception {
    checkConfigurationMerge("{ _override : false," + " _enabled : true, m1 : { a : 10}}");
  }

  private void checkConfigurationMerge(String pluginConfiguration) {
    Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
    PluginDescriptor pluginDescriptor = new PluginDescriptor();
    new PluginDescriptorFileReader("{default-conf:{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}}",
        "p1").fillPluginDescriptor(pluginDescriptor);
    plugins.add(pluginDescriptor);
    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
    pluginDescriptors.merge("p1", JSONObject.fromObject(pluginConfiguration));

    JSONObject resultConfiguration = pluginDescriptors.getEnabled()[0].getConfiguration();
    assertEquals(2, resultConfiguration.keySet().size());
    assertEquals(10, resultConfiguration.getJSONObject("m1").get("a"));
    assertEquals(2, resultConfiguration.getJSONObject("m1").get("b"));
    assertEquals(1, resultConfiguration.getJSONObject("m2").get("c"));
    assertEquals(2, resultConfiguration.getJSONObject("m2").get("d"));
  }

  @Test
  public void overridesDefaultConfIfSpecified() throws Exception {
    Set<PluginDescriptor> plugins =
        createPluginSet("{default-conf:{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}}", "p1");
    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
    pluginDescriptors.merge("p1",
        JSONObject.fromObject("{ _override : true," + " _enabled : true, m1 : { a : 10}}"));

    JSONObject resultConfiguration = pluginDescriptors.getEnabled()[0].getConfiguration();
    assertEquals(1, resultConfiguration.keySet().size());
    JSONObject m1Configuration = resultConfiguration.getJSONObject("m1");
    assertEquals(1, m1Configuration.keySet().size());
    assertEquals(10, m1Configuration.get("a"));
  }

  private Set<PluginDescriptor> createPluginSet(String pluginDefaultConfiguration,
      String pluginName) {
    Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
    PluginDescriptor pluginDescriptor = new PluginDescriptor();
    new PluginDescriptorFileReader(pluginDefaultConfiguration, pluginName)
        .fillPluginDescriptor(pluginDescriptor);
    plugins.add(pluginDescriptor);
    return plugins;
  }

  @Test
  public void doesNotReturnPseudomodules() throws Exception {
    Set<PluginDescriptor> plugins = createPluginSet(
        "{ installInRoot: false, default-conf:{m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}}", "p1");
    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);

    pluginDescriptors.merge("p1",
        JSONObject.fromObject("{ _override : false, "//
            + "_enabled : true, "//
            + "m1 : { a : 1, b : 2}, "//
            + "m2 : { c : 1, d : 2}}"));

    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
    assertEquals(1, descriptors.length);
    Set<?> moduleNames = descriptors[0].getConfiguration().keySet();
    assertEquals(2, moduleNames.size());
    assertTrue(moduleNames.contains("m1"));
    assertTrue(moduleNames.contains("m2"));
  }

  @Test
  public void includesPluginsFromDefaultConfIfAllEnabled() throws Exception {
    Set<PluginDescriptor> plugins = createPluginSet("{default-conf:{ m2 : { b : 20 }}}", "p2");
    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);

    pluginDescriptors.merge("p1", JSONObject.fromObject("{ m1 : { a : 10 }}"));

    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
    assertEquals(2, descriptors.length);
    int p1Index = descriptors[0].getName().equals("p1") ? 0 : 1;
    int p2Index = 1 - p1Index;
    assertEquals(10, descriptors[p1Index].getConfiguration().getJSONObject("m1").get("a"));
    assertEquals(20, descriptors[p2Index].getConfiguration().getJSONObject("m2").get("b"));
  }

  @Test
  public void twoAnonymousPlugins() throws Exception {
    Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
    PluginDescriptor pluginDescriptor1 = new PluginDescriptor();
    new PluginDescriptorFileReader("{"//
        + "default-conf:{ m1 : 1}, "//
        + " requirejs:{"//
        + "  paths:{"//
        + "   l1:\"../jslib/l1.js\","//
        + "   l2:\"../jslib/l2.js\""//
        + "  },"//
        + "  shim:{ l2:[\"l1\"] }"//
        + " }"//
        + "}", null).fillPluginDescriptor(pluginDescriptor1);
    pluginDescriptor1.addModule("m1");
    pluginDescriptor1.addStylesheet("m1.css");
    plugins.add(pluginDescriptor1);
    PluginDescriptor pluginDescriptor2 = new PluginDescriptor();
    new PluginDescriptorFileReader("{"//
        + "default-conf:{ m2 : 2}, "//
        + " requirejs:{"//
        + "  paths:{"//
        + "   l3:\"../jslib/l3.js\""//
        + "  },"//
        + "  shim:{ l3:[\"l1\"] }"//
        + " }"//
        + "}", null).fillPluginDescriptor(pluginDescriptor2);
    pluginDescriptor2.addModule("m2");
    pluginDescriptor2.addStylesheet("m2.css");
    plugins.add(pluginDescriptor2);
    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);

    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
    assertEquals(1, descriptors.length);
    PluginDescriptor pluginDescriptor = descriptors[0];
    // all in unnamed plugin
    assertEquals(PluginDescriptors.UNNAMED_GEOLADRIS_CORE_PLUGIN, pluginDescriptor.getName());
    // configuration merged
    assertEquals(1, pluginDescriptor.getConfiguration().getInt("m1"));
    assertEquals(2, pluginDescriptor.getConfiguration().getInt("m2"));
    // modules and stylesheets
    assertEquals(2, pluginDescriptor.getModules().size());
    assertEquals(2, pluginDescriptor.getStylesheets().size());
    // requirejs paths and shims
    assertEquals(3, pluginDescriptor.getRequireJSPathsMap().size());
    assertEquals(2, pluginDescriptor.getRequireJSShims().size());
  }

  @Test
  public void unnamedMergeSearchesModulesInPluginsThatInstallInRoot() throws Exception {

    Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
    PluginDescriptor rootNamedPluginDescriptor = new PluginDescriptor();
    String rootNamedPluginName = "rootNamedPlugin";
    new PluginDescriptorFileReader("{"//
        + "installInRoot:true,"//
        + "default-conf:{ m1 : {value:1} }, "//
        + "}", rootNamedPluginName).fillPluginDescriptor(rootNamedPluginDescriptor);
    plugins.add(rootNamedPluginDescriptor);
    PluginDescriptor qualifiedNamedPluginDescriptor = new PluginDescriptor();
    String qualifiedNamedPluginName = "qualifiedNamedPlugin";
    new PluginDescriptorFileReader("{"//
        + "installInRoot:false,"//
        + "default-conf:{ m2 : {value:2} }, "//
        + "}", qualifiedNamedPluginName).fillPluginDescriptor(qualifiedNamedPluginDescriptor);
    plugins.add(qualifiedNamedPluginDescriptor);
    PluginDescriptor unnamedPluginDescriptor = new PluginDescriptor();
    new PluginDescriptorFileReader("{"//
        + "default-conf:{ m3 : {value:3} }, "//
        + "}", null).fillPluginDescriptor(unnamedPluginDescriptor);
    plugins.add(unnamedPluginDescriptor);
    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);

    pluginDescriptors.merge(PluginDescriptors.UNNAMED_GEOLADRIS_CORE_PLUGIN,
        JSONObject.fromObject("{"//
            + " m1 : {value:11},"//
            + " m2 : {value:12},"//
            + " m3 : {value:13},"//
            + "}"));

    PluginDescriptor[] enabled = pluginDescriptors.getEnabled();
    assertEquals(3, enabled.length);
    int unnamedIndex = -1;
    int qualifiedNamedIndex = -1;
    int rootNamedIndex = -1;
    for (int i = 0; i < enabled.length; i++) {
      if (enabled[i].getName().equals(PluginDescriptors.UNNAMED_GEOLADRIS_CORE_PLUGIN)) {
        unnamedIndex = i;
      }
      if (enabled[i].getName().equals(rootNamedPluginName)) {
        rootNamedIndex = i;
      }
      if (enabled[i].getName().equals(qualifiedNamedPluginName)) {
        qualifiedNamedIndex = i;
      }
    }
    JSONObject rootConfiguration = enabled[unnamedIndex].getConfiguration();
    assertEquals(2, rootConfiguration.keySet().size());
    assertEquals(13, rootConfiguration.getJSONObject("m3").getInt("value"));
    assertEquals(12, rootConfiguration.getJSONObject("m2").getInt("value"));
    JSONObject namedrootConfiguration = enabled[rootNamedIndex].getConfiguration();
    assertEquals(1, namedrootConfiguration.keySet().size());
    assertEquals(11, namedrootConfiguration.getJSONObject("m1").getInt("value"));
    JSONObject namedQualifiedConfiguration = enabled[qualifiedNamedIndex].getConfiguration();
    assertEquals(1, namedQualifiedConfiguration.keySet().size());
    assertEquals(2, namedQualifiedConfiguration.getJSONObject("m2").getInt("value"));
  }

  @Test
  public void qualifiedPluginModuleConfiguration() throws Exception {
    Set<PluginDescriptor> plugins = createPluginSet("{"//
        + " installInRoot:false,"//
        + " default-conf:{ m2 : 0 }, "//
        + "}", "plugin1");
    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);

    assertTrue(pluginDescriptors.getQualifiedConfiguration("plugin1").has("plugin1/m2"));
  }

  @Test
  public void unqualifiedPluginModuleConfiguration() throws Exception {
    Set<PluginDescriptor> plugins = createPluginSet("{"//
        + " installInRoot:true,"//
        + " default-conf:{ m2 : 0 }, "//
        + "}", "plugin1");
    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);

    assertTrue(pluginDescriptors.getQualifiedConfiguration("plugin1").has("m2"));
  }

  @Test
  public void qualifiedAnonymousPluginModuleConfiguration() throws Exception {
    Set<PluginDescriptor> plugins = createPluginSet("{"//
        + " installInRoot:false,"//
        + " default-conf:{ m2 : 0 }, "//
        + "}", null);
    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);

    assertTrue(pluginDescriptors
        .getQualifiedConfiguration(PluginDescriptors.UNNAMED_GEOLADRIS_CORE_PLUGIN).has("m2"));
  }

  @Test
  public void firstUnnamedQualifiedButQualifiedConfigurationIsNotQualified() throws Exception {
    Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
    PluginDescriptor pluginDescriptor = new PluginDescriptor();
    new PluginDescriptorFileReader("{"//
        + " default-conf:{ m2 : {value:0} }, "//
        + "}", null).fillPluginDescriptor(pluginDescriptor);
    plugins.add(pluginDescriptor);
    PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
    pluginDescriptors.merge(PluginDescriptors.UNNAMED_GEOLADRIS_CORE_PLUGIN,
        JSONObject.fromObject("{ m1 : {value:0} }"));

    JSONObject unnamedPluginConfiguration = pluginDescriptors
        .getQualifiedConfiguration(PluginDescriptors.UNNAMED_GEOLADRIS_CORE_PLUGIN);
    assertTrue(unnamedPluginConfiguration.has("m1"));
    assertTrue(unnamedPluginConfiguration.has("m2"));
  }

  @Test
  public void mergingUnnamedModuleConfigurationThatDoesNotExistInitiallyOnThePlugins()
      throws Exception {
    PluginDescriptors pluginDescriptors = new PluginDescriptors(createPluginSet("{"//
        + " default-conf:{ m2 : 0 }, "//
        + "}", null));

    pluginDescriptors.merge(PluginDescriptors.UNNAMED_GEOLADRIS_CORE_PLUGIN,
        JSONObject.fromObject("{"//
            + " m1 : {value:11}"//
            + "}"));

    PluginDescriptor[] enabled = pluginDescriptors.getEnabled();
    assertEquals(1, enabled.length);
    assertEquals(PluginDescriptors.UNNAMED_GEOLADRIS_CORE_PLUGIN, enabled[0].getName());
    JSONObject configuration = enabled[0].getConfiguration();
    assertEquals(2, configuration.keySet().size());
    assertTrue(configuration.has("m1"));
    assertTrue(configuration.has("m2"));
  }

  /**
   * Bug fix: a call to merge with a pluginName not found in the descriptors used to construct the
   * PluginDescriptors instance causes the PluginDescriptor to have null name
   */
  @Test
  public void nullNameWhenNewPluginConfigurationIsMerged() {
    PluginDescriptors pluginDescriptors = new PluginDescriptors(createPluginSet("{"//
        + " default-conf:{ m2 : 0 }, "//
        + "}", "plugin1"));

    pluginDescriptors.merge("plugin2", JSONObject.fromObject("{ m2 : 0 }"));

    PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
    assertEquals(2, descriptors.length);
    ArrayList<String> pluginNames = new ArrayList<>();
    Collections.addAll(pluginNames, "plugin1", "plugin2");
    for (PluginDescriptor pluginDescriptor : descriptors) {
      assertNotNull(pluginDescriptor.getName());
      pluginNames.remove(pluginDescriptor.getName());
    }
    assertEquals(0, pluginNames.size());
  }

  @Test
  public void qualifiedStyleWithSubdirectories() {
    PluginDescriptor descriptor = new PluginDescriptor();
    descriptor.setInstallInRoot(false);
    descriptor.setName("myplugin");
    descriptor.addStylesheet("styles/subdirectory/style.css");
    String style = descriptor.getStylesheets().iterator().next();
    assertEquals("styles/myplugin/subdirectory/style.css", style);
  }
}
