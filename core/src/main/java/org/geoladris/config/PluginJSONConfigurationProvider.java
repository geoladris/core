package org.geoladris.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.geoladris.PluginDescriptor;
import org.geoladris.PluginDescriptorFileReader;
import org.geoladris.PortalRequestConfiguration;

/**
 * @deprecated Use {@link PublicConfProvider} instead.
 * @author victorzinho
 */
public class PluginJSONConfigurationProvider implements ModuleConfigurationProvider {

  @Override
  public Map<String, JSONObject> getPluginConfig(PortalRequestConfiguration configurationContext,
      HttpServletRequest request) throws IOException {
    // We create return a pseudo-plugin descriptor containing all the
    // configuration to override/merge
    // The modules, stylesheets and RequireJS data is empty since it is
    // taken from all the other real plugins.
    File configProperties =
        new File(configurationContext.getConfigurationDirectory(), "plugin-conf.json");
    BufferedInputStream stream;
    try {
      stream = new BufferedInputStream(new FileInputStream(configProperties));
    } catch (FileNotFoundException e) {
      return null;
    }
    String content = IOUtils.toString(stream);
    stream.close();

    PluginDescriptor plugin = new PluginDescriptor();
    new PluginDescriptorFileReader(content, null).fillPluginDescriptor(plugin);

    Map<String, JSONObject> ret = new HashMap<>();
    if (plugin != null) {
      ret.put(PluginDescriptors.UNNAMED_GEOLADRIS_CORE_PLUGIN, plugin.getConfiguration());
    }
    return ret;
  }

  @Override
  public boolean canBeCached() {
    return true;
  }
}
