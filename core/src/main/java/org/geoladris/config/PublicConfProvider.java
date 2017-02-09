package org.geoladris.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import de.csgis.commons.JSONContentProvider;
import net.sf.json.JSONObject;

/**
 * <p>
 * {@link ModuleConfigurationProvider} that returns the configuration from
 * <code>&lt;config_dir&gt;/public-conf.json</code>.
 * </p>
 * 
 * @author victorzinho
 */
public class PublicConfProvider implements ModuleConfigurationProvider {
  public static final String FILE_BASE = "public-conf";
  public static final String FILE = FILE_BASE + ".json";

  public static final String ROLE_DIR = "role_conf";

  private Map<File, JSONContentProvider> contents = new HashMap<>();

  public PublicConfProvider() {}

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, JSONObject> getPluginConfig(PortalRequestConfiguration requestConfig,
      HttpServletRequest request) throws IOException {
    File dir = requestConfig.getConfigDir();
    JSONContentProvider jsonContent = contents.get(dir);
    if (jsonContent == null) {
      jsonContent = new JSONContentProvider(dir.getAbsolutePath());
      contents.put(dir, jsonContent);
    }
    File file = new File(dir, FILE);
    return file.exists() ? jsonContent.get().get(FILE_BASE) : null;
  }

  @Override
  public boolean canBeCached() {
    return true;
  }
}
