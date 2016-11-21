package org.geoladris.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.geoladris.PluginDescriptor;
import org.geoladris.PluginDescriptorFileReader;

import net.sf.json.JSONObject;

/**
 * @deprecated Use {@link PublicConfProvider} instead.
 * @author victorzinho
 */
public class PluginJSONConfigurationProvider implements ModuleConfigurationProvider {
  private static final String PSEUDO_PLUGIN_NAME = "__plugin-json__";

  @Override
  public Map<String, JSONObject> getPluginConfig(Config config, HttpServletRequest request)
      throws IOException {
    // We create return a pseudo-plugin descriptor containing all the
    // configuration to override/merge
    // The modules, stylesheets and RequireJS data is empty since it is
    // taken from all the other real plugins.
    File configProperties = new File(config.getDir(), "plugin-conf.json");
    BufferedInputStream stream;
    try {
      stream = new BufferedInputStream(new FileInputStream(configProperties));
    } catch (FileNotFoundException e) {
      return null;
    }
    String content = IOUtils.toString(stream);
    stream.close();

    PluginDescriptor plugin = new PluginDescriptorFileReader().read(content, PSEUDO_PLUGIN_NAME);

    Map<String, JSONObject> ret = new HashMap<>();
    if (plugin != null) {
      ret.put(PSEUDO_PLUGIN_NAME, plugin.getConfiguration());
    }
    return ret;
  }

  @Override
  public boolean canBeCached() {
    return true;
  }
}
