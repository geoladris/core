package org.fao.unredd.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.json.JSONObject;

public class PublicConfProviderTest {
	private File configDir;
	private PublicConfProvider provider;
	private PluginDescriptor plugin;

	@Before
	public void setup() throws IOException {
		configDir = File.createTempFile("geoladris", "");
		configDir.delete();
		configDir.mkdir();

		plugin = new PluginDescriptor();
		plugin.setName("myplugin");

		Map<String, PluginDescriptor> plugins = new HashMap<>();
		plugins.put(plugin.getName(), plugin);

		provider = new PublicConfProvider(configDir, plugins);
	}

	@After
	public void teardown() throws IOException {
		FileUtils.deleteDirectory(configDir);
	}

	@Test
	public void missingPublicConfFile() throws Exception {
		Map<PluginDescriptor, JSONObject> conf = provider.getPluginConfig(
				mock(PortalRequestConfiguration.class),
				mock(HttpServletRequest.class));
		assertNull(conf);
	}

	@Test
	public void validPublicConfFile() throws Exception {
		File tmp = new File(configDir, PublicConfProvider.FILE);
		FileWriter writer = new FileWriter(tmp);
		IOUtils.write(
				"{ '" + plugin.getName() + "' : { mymodule : {'a' : true }}}",
				writer);
		writer.close();

		Map<PluginDescriptor, JSONObject> conf = provider.getPluginConfig(
				mock(PortalRequestConfiguration.class),
				mock(HttpServletRequest.class));
		assertEquals(1, conf.size());
		assertTrue(conf.containsKey(plugin));
		JSONObject pluginConf = conf.get(plugin);
		assertTrue(pluginConf.getJSONObject("mymodule").getBoolean("a"));

		tmp.delete();
	}

	@Test
	public void canBeCached() {
		assertTrue(this.provider.canBeCached());
	}
}
