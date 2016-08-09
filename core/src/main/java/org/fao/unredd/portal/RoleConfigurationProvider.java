package org.fao.unredd.portal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import de.csgis.commons.JSONContentProvider;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

public class RoleConfigurationProvider implements ModuleConfigurationProvider {
	public static final String ROLE_DIR = "role_conf";

	private JSONContentProvider contents;

	public RoleConfigurationProvider(File configDir) {
		this.contents = new JSONContentProvider(
				new File(configDir, ROLE_DIR).getAbsolutePath());
	}

	@Override
	public Map<String, JSONObject> getConfigurationMap(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException {
		return doGetConfigMap(request);
	}

	@Override
	public Map<String, JSON> getConfigMap(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException {
		return doGetConfigMap(request);
	}

	@SuppressWarnings("unchecked")
	private <T extends JSON> Map<String, T> doGetConfigMap(
			HttpServletRequest request) {
		Object attr = request.getSession()
				.getAttribute(Constants.SESSION_ATTR_ROLE);
		if (attr == null) {
			return null;
		}

		String role = attr.toString();
		JSONObject roleSpecificConf = this.contents.get().get(role);
		if (roleSpecificConf == null) {
			return null;
		}

		Map<String, T> ret = new HashMap<String, T>();
		for (Object key : roleSpecificConf.keySet()) {
			String plugin = key.toString();
			ret.put(plugin, (T) roleSpecificConf.get(plugin));
		}
		return ret;
	}

	@Override
	public boolean canBeCached() {
		return false;
	}
}
