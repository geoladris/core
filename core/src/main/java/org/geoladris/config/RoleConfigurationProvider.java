package org.geoladris.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.geoladris.Geoladris;

import de.csgis.commons.JSONContentProvider;
import net.sf.json.JSONObject;

/**
 * <p>
 * ModuleConfigurationProvider that returns the configuration that is specific to a role. The role
 * configurations are taken from <code>&lt;config_dir&gt;/</code>{@value #ROLE_DIR}
 * <code>/&lt;role&gt;.json</code> files.
 * </p>
 * 
 * <p>
 * The active role is taken from the {@link Geoladris#SESSION_ATTR_ROLE} session attribute.
 * </p>
 * 
 * @author victorzinho
 */
public class RoleConfigurationProvider implements ModuleConfigurationProvider {
  public static final String ROLE_DIR = "role_conf";

  private JSONContentProvider contents;

  public RoleConfigurationProvider(File configDir) {
    String roleDir = new File(configDir, ROLE_DIR).getAbsolutePath();
    this.contents = new JSONContentProvider(roleDir);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, JSONObject> getPluginConfig(Config config, HttpServletRequest request)
      throws IOException {
    String role = getRole(request);
    return role != null ? this.contents.get().get(role) : null;
  }

  private String getRole(HttpServletRequest request) {
    Object attr = request.getSession().getAttribute(Geoladris.SESSION_ATTR_ROLE);
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
