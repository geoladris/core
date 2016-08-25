package org.fao.unredd.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;
import org.junit.Before;
import org.junit.Test;

import de.csgis.commons.JSONContentProvider;
import net.sf.json.JSONObject;

public class ConfigurationProviderHelperTest {
	private ConfigurationProviderHelper helper;
	private JSONContentProvider contents;
	private Map<String, PluginDescriptor> plugins;

	@Before
	public void setup() {
		contents = mock(JSONContentProvider.class);

		plugins = new HashMap<>();

		helper = new ConfigurationProviderHelper(contents, plugins);
	}

	@Test
	public void pluginConfigMissingFile() throws IOException {
		when(contents.get()).thenReturn(new HashMap<String, JSONObject>());
		assertNull(helper.getPluginConfig("missing"));
	}

	@Test
	public void pluginConfigValidJSON() throws IOException {
		String file = "myfile";

		PluginDescriptor p1 = new PluginDescriptor(true);
		p1.setName("plugin1");
		PluginDescriptor p2 = new PluginDescriptor(true);
		p2.setName("plugin2");
		plugins.put("plugin1", p1);
		plugins.put("plugin2", p2);

		String pluginConf1 = "{module1 : {prop1 : 42, prop2 : true}}";
		String pluginConf2 = "{module2 : {prop3 : 'test'},"
				+ "module3 : [4, 2, 9]}";
		JSONObject json = JSONObject.fromObject("{ plugin1 : " + pluginConf1
				+ ", plugin2 : " + pluginConf2 + "}");
		HashMap<String, JSONObject> files = new HashMap<String, JSONObject>();
		files.put(file, json);

		when(contents.get()).thenReturn(files);
		Map<PluginDescriptor, JSONObject> config = helper.getPluginConfig(file);
		assertEquals(2, config.size());
		JSONObject conf1 = config.get(p1);
		JSONObject conf2 = config.get(p2);
		assertEquals(42, conf1.getJSONObject("module1").getInt("prop1"));
		assertTrue(conf1.getJSONObject("module1").getBoolean("prop2"));
		assertEquals("test", conf2.getJSONObject("module2").getString("prop3"));
		assertEquals(4, conf2.getJSONArray("module3").get(0));
		assertEquals(2, conf2.getJSONArray("module3").get(1));
		assertEquals(9, conf2.getJSONArray("module3").get(2));
	}
}
