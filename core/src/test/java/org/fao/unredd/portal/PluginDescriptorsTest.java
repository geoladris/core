package org.fao.unredd.portal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.sf.json.JSONObject;

import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptorFileReader;
import org.junit.Test;

public class PluginDescriptorsTest {

	@Test
	public void doesNotReturnDisabledPlugins() throws Exception {
		PluginDescriptors pluginDescriptors = new PluginDescriptors(
				Collections.<PluginDescriptor> emptySet());
		pluginDescriptors.merge("p1", JSONObject
				.fromObject("{_enabled : false, "
						+ "m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}"));
		pluginDescriptors.merge("p2", JSONObject
				.fromObject("{_enabled : true, m3 : { a : 1, b : 2}}"));

		PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();

		assertEquals(1, descriptors.length);
		JSONObject pluginConf = descriptors[0].getConfiguration();
		assertEquals(1, pluginConf.keySet().size());
		assertEquals("m3", pluginConf.keySet().iterator().next());
	}

	@Test
	public void pluginsEnabledByDefault() throws Exception {
		PluginDescriptors pluginDescriptors = new PluginDescriptors(
				Collections.<PluginDescriptor> emptySet());

		pluginDescriptors.merge("p1", JSONObject
				.fromObject("{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}"));

		PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
		assertEquals(1, descriptors.length);
	}

	@Test
	public void defaultConfigurationMergedByDefault() throws Exception {
		checkConfigurationMerge("{ _enabled : true, m1 : { a : 10}}");
	}

	@Test
	public void mergesDefaultConfIfSpecified() throws Exception {
		checkConfigurationMerge("{ _override : false,"
				+ " _enabled : true, m1 : { a : 10}}");
	}

	private void checkConfigurationMerge(String pluginConfiguration) {
		Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
		PluginDescriptor pluginDescriptor = new PluginDescriptor();
		new PluginDescriptorFileReader(
				"{default-conf:{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}}",
				true, "p1").fillPluginDescriptor(pluginDescriptor);
		plugins.add(pluginDescriptor);
		PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
		pluginDescriptors.merge("p1",
				JSONObject.fromObject(pluginConfiguration));

		JSONObject resultConfiguration = pluginDescriptors.getEnabled()[0]
				.getConfiguration();
		assertEquals(2, resultConfiguration.keySet().size());
		assertEquals(10, resultConfiguration.getJSONObject("m1").get("a"));
		assertEquals(2, resultConfiguration.getJSONObject("m1").get("b"));
		assertEquals(1, resultConfiguration.getJSONObject("m2").get("c"));
		assertEquals(2, resultConfiguration.getJSONObject("m2").get("d"));
	}

	@Test
	public void overridesDefaultConfIfSpecified() throws Exception {
		Set<PluginDescriptor> plugins = createPluginSet(
				"{default-conf:{ m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}}",
				"p1");
		PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);
		pluginDescriptors.merge("p1", JSONObject
				.fromObject("{ _override : true,"
						+ " _enabled : true, m1 : { a : 10}}"));

		JSONObject resultConfiguration = pluginDescriptors.getEnabled()[0]
				.getConfiguration();
		assertEquals(1, resultConfiguration.keySet().size());
		JSONObject m1Configuration = resultConfiguration.getJSONObject("m1");
		assertEquals(1, m1Configuration.keySet().size());
		assertEquals(10, m1Configuration.get("a"));
	}

	private Set<PluginDescriptor> createPluginSet(
			String pluginDefaultConfiguration, String pluginName) {
		Set<PluginDescriptor> plugins = new HashSet<PluginDescriptor>();
		PluginDescriptor pluginDescriptor = new PluginDescriptor();
		new PluginDescriptorFileReader(pluginDefaultConfiguration, true,
				pluginName).fillPluginDescriptor(pluginDescriptor);
		plugins.add(pluginDescriptor);
		return plugins;
	}

	@Test
	public void doesNotReturnPseudomodules() throws Exception {
		Set<PluginDescriptor> plugins = createPluginSet(
				"{ installInRoot: false, default-conf:{m1 : { a : 1, b : 2}, m2 : { c : 1, d : 2}}}",
				"p1");
		PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);

		pluginDescriptors.merge("p1", JSONObject
				.fromObject("{ _override : false, "//
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
		Set<PluginDescriptor> plugins = createPluginSet(
				"{default-conf:{ m2 : { b : 20 }}}", "p2");
		PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);

		pluginDescriptors.merge("p1",
				JSONObject.fromObject("{ m1 : { a : 10 }}"));

		PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
		assertEquals(2, descriptors.length);
		int p1Index = descriptors[0].getName().equals("p1") ? 0 : 1;
		int p2Index = 1 - p1Index;
		assertEquals(10,
				descriptors[p1Index].getConfiguration().getJSONObject("m1")
						.get("a"));
		assertEquals(20,
				descriptors[p2Index].getConfiguration().getJSONObject("m2")
						.get("b"));
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
				+ "}", true, null).fillPluginDescriptor(pluginDescriptor1);
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
				+ "}", true, null).fillPluginDescriptor(pluginDescriptor2);
		pluginDescriptor2.addModule("m2");
		pluginDescriptor2.addStylesheet("m2.css");
		plugins.add(pluginDescriptor1);
		plugins.add(pluginDescriptor2);
		PluginDescriptors pluginDescriptors = new PluginDescriptors(plugins);

		PluginDescriptor[] descriptors = pluginDescriptors.getEnabled();
		assertEquals(1, descriptors.length);
		PluginDescriptor pluginDescriptor = descriptors[0];
		// all in unnamed plugin
		assertEquals(PluginDescriptors.UNNAMED_GEOLADRIS_CORE_PLUGIN,
				pluginDescriptor.getName());
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

}
