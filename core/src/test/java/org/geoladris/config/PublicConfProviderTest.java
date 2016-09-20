package org.geoladris.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoladris.PortalRequestConfiguration;
import org.geoladris.config.PublicConfProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PublicConfProviderTest {
	private File configDir;
	private PublicConfProvider provider;

	@Before
	public void setup() throws IOException {
		configDir = File.createTempFile("geoladris", "");
		configDir.delete();
		configDir.mkdir();

		provider = new PublicConfProvider(configDir);
	}

	@After
	public void teardown() throws IOException {
		FileUtils.deleteDirectory(configDir);
	}

	@Test
	public void missingPublicConfFile() throws Exception {
		Map<String, JSONObject> conf = provider.getPluginConfig(
				mock(PortalRequestConfiguration.class),
				mock(HttpServletRequest.class));
		assertNull(conf);
	}

	@Test
	public void validPublicConfFile() throws Exception {
		File tmp = new File(configDir, PublicConfProvider.FILE);
		FileWriter writer = new FileWriter(tmp);
		String pluginName = "p1";
		IOUtils.write("{ '" + pluginName + "' : { mymodule : {'a' : true }}}",
				writer);
		writer.close();

		Map<String, JSONObject> conf = provider.getPluginConfig(
				mock(PortalRequestConfiguration.class),
				mock(HttpServletRequest.class));
		assertEquals(1, conf.size());
		assertTrue(conf.containsKey(pluginName));
		JSONObject pluginConf = conf.get(pluginName);
		assertTrue(pluginConf.getJSONObject("mymodule").getBoolean("a"));

		tmp.delete();
	}

	@Test
	public void canBeCached() {
		assertTrue(this.provider.canBeCached());
	}
}
