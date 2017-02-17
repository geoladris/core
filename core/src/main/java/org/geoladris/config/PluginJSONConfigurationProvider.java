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
import org.apache.log4j.Logger;
import org.geoladris.PluginDescriptor;
import org.geoladris.PluginDescriptorFileReader;

import net.sf.json.JSONObject;

/**
 * @deprecated Use {@link PublicConfProvider} instead.
 * @author victorzinho
 */
public class PluginJSONConfigurationProvider implements ModuleConfigurationProvider {
  private static final Logger logger = Logger.getLogger(PluginJSONConfigurationProvider.class);

  private static final String PLUGIN_NAME = "core";

  @Override
  public Map<String, JSONObject> getPluginConfig(PortalRequestConfiguration requestConfig,
      HttpServletRequest request) throws IOException {
    File publicConf = new File(requestConfig.getConfigDir(), PublicConfProvider.FILE);
    if (publicConf.isFile()) {
      return null;
    }

    logger.warn("Using deprecated plugin-conf.json; use public-conf instead");
    File pluginConf = new File(requestConfig.getConfigDir(), "plugin-conf.json");
    BufferedInputStream stream;
    try {
      stream = new BufferedInputStream(new FileInputStream(pluginConf));
    } catch (FileNotFoundException e) {
      return null;
    }
    String content = IOUtils.toString(stream);
    stream.close();

    PluginDescriptor plugin = new PluginDescriptorFileReader().read(content, PLUGIN_NAME);

    Map<String, JSONObject> ret = new HashMap<>();
    if (plugin != null) {
      ret.put(PLUGIN_NAME, plugin.getConfiguration());
    }
    return ret;
  }

  @Override
  public boolean canBeCached() {
    return true;
  }
}
