package org.geoladris.config;

import java.io.File;
import java.io.IOException;
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

  private JSONContentProvider contents;
  private File file;

  public PublicConfProvider(File configDir) {
    this.contents = new JSONContentProvider(configDir.getAbsolutePath());
    this.file = new File(configDir, FILE);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, JSONObject> getPluginConfig(PortalRequestConfiguration requestConfig,
      HttpServletRequest request) throws IOException {
    return file.exists() ? this.contents.get().get(FILE_BASE) : null;
  }

  @Override
  public boolean canBeCached() {
    return true;
  }
}
