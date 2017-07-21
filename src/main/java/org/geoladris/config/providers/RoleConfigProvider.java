package org.geoladris.config.providers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.geoladris.Geoladris;
import org.geoladris.config.Config;
import org.geoladris.config.PluginConfigProvider;

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
 * The active role is taken from the {@link Geoladris#ATTR_ROLE} session attribute.
 * </p>
 * 
 * @author victorzinho
 */
public class RoleConfigProvider implements PluginConfigProvider {
  public static final String ROLE_DIR = "role_conf";

  private Map<File, JSONContentProvider> contents = new HashMap<>();

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, JSONObject> getPluginConfig(Config config,
      Map<String, JSONObject> currentConfig, HttpServletRequest request) throws IOException {
    File dir = new File(config.getDir(), ROLE_DIR);
    JSONContentProvider jsonContent = contents.get(dir);
    if (jsonContent == null) {
      jsonContent = new JSONContentProvider(dir.getAbsolutePath());
      contents.put(dir, jsonContent);
    }

    String role = getRole(request);
    return role != null ? jsonContent.get().get(role) : null;
  }

  private String getRole(HttpServletRequest request) {
    HttpSession session = request.getSession();
    if (session == null) {
      return null;
    }
    Object attr = session.getAttribute(Geoladris.ATTR_ROLE);
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
