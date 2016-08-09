package org.fao.unredd.portal;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

public class RoleConfigurationProviderTest {
	private File configDir, roleDir;
	private RoleConfigurationProvider provider;

	@Before
	public void setup() throws IOException {
		configDir = File.createTempFile("geoladris", "");
		configDir.delete();
		configDir.mkdir();

		roleDir = new File(configDir, RoleConfigurationProvider.ROLE_DIR);
		roleDir.mkdir();

		provider = new RoleConfigurationProvider(configDir);
	}

	@After
	public void teardown() throws IOException {
		FileUtils.deleteDirectory(configDir);
	}

	@Test
	public void noRoleOnRequest() throws Exception {
		HttpServletRequest request = mockRequest(null);
		Map<String, JSON> conf = provider
				.getConfigMap(mock(PortalRequestConfiguration.class), request);
		assertNull(conf);
	}

	@Test
	public void roleWithoutSpecificConf() throws Exception {
		HttpServletRequest request = mockRequest("role1");
		Map<String, JSON> conf = provider
				.getConfigMap(mock(PortalRequestConfiguration.class), request);
		assertNull(conf);
	}

	@Test
	public void addsPlugin() throws Exception {
		String role = "role1";

		File tmp = new File(roleDir, role + ".json");
		FileWriter writer = new FileWriter(tmp);
		IOUtils.write("{ module : {'a' : true }}", writer);
		writer.close();

		HttpServletRequest request = mockRequest(role);
		Map<String, JSON> conf = provider
				.getConfigMap(mock(PortalRequestConfiguration.class), request);
		assertTrue(conf.containsKey("module"));
		JSONObject pluginConf = (JSONObject) conf.get("module");
		assertTrue(pluginConf.getBoolean("a"));

		tmp.delete();
	}

	private HttpServletRequest mockRequest(String role) {
		HttpServletRequest request = mock(HttpServletRequest.class);

		HttpSession session = mock(HttpSession.class);
		when(request.getSession()).thenReturn(session);
		when(session.getAttribute(Constants.SESSION_ATTR_ROLE))
				.thenReturn(role);

		return request;
	}
}
