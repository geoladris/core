package org.fao.unredd.portal;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;
import de.csgis.commons.JSONContentProvider;

/**
 * <p>
 * ModuleConfigurationProvider that returns the configuration that is specific
 * to a role. The role configurations are taken from
 * <code>&lt;config_dir&gt;/</code>{@value #ROLE_DIR}
 * <code>/&lt;role&gt;.json</code> files.
 * </p>
 * 
 * <p>
 * The active role is taken from the {@link Constants#SESSION_ATTR_ROLE} session
 * attribute.
 * </p>
 * 
 * @author victorzinho
 */
public class RoleConfigurationProvider implements ModuleConfigurationProvider {
	public static final String ROLE_DIR = "role_conf";

	private ConfigurationProviderHelper helper;

	public RoleConfigurationProvider(File configDir) {
		String roleDir = new File(configDir, ROLE_DIR).getAbsolutePath();
		this.helper = new ConfigurationProviderHelper(new JSONContentProvider(
				roleDir));
	}

	@Override
	public Map<String, JSONObject> getPluginConfig(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException {
		String role = getRole(request);
		return role != null ? this.helper.getPluginConfig(role) : null;
	}

	private String getRole(HttpServletRequest request) {
		Object attr = request.getSession().getAttribute(
				Constants.SESSION_ATTR_ROLE);
		if (attr == null) {
			return null;
		}

		return attr.toString();
	}

	@Override
	public boolean canBeCached() {
		return false;
	}
}
