package org.geoladris.config.providers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.geoladris.config.Config;
import org.geoladris.config.PluginConfigProvider;

import de.csgis.commons.JSONContentProvider;
import net.sf.json.JSONObject;

/**
 * <p>
 * {@link PluginConfigProvider} that returns the configuration from
 * <code>&lt;config_dir&gt;/public-conf.json</code>.
 * </p>
 * 
 * @author victorzinho
 */
public class PublicConfProvider implements PluginConfigProvider {
  public static final String FILE_BASE = "public-conf";
  public static final String FILE = FILE_BASE + ".json";

  private Map<File, JSONContentProvider> contents = new HashMap<>();

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, JSONObject> getPluginConfig(Config config,
      Map<String, JSONObject> currentConfig, HttpServletRequest request) throws IOException {
    File dir = config.getDir();
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
