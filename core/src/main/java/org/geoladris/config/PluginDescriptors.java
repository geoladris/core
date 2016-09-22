package org.geoladris.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.geoladris.PluginDescriptor;

import de.csgis.commons.JSONUtils;
import net.sf.json.JSONObject;

public class PluginDescriptors {
  private static final String CONF_ENABLED = "_enabled";
  private static final String CONF_OVERRIDE = "_override";
  public static final String UNNAMED_GEOLADRIS_CORE_PLUGIN = "unnamed_geoladris_core_plugin";

  private HashMap<String, PluginDescriptor> namePluginDescriptor =
      new HashMap<String, PluginDescriptor>();
  private PluginDescriptor[] enabled = null;

  public PluginDescriptors(Set<PluginDescriptor> plugins) {
    for (PluginDescriptor pluginDescriptor : plugins) {
      PluginDescriptor clonedDescriptor = pluginDescriptor.cloneDescriptor();
      String pluginName = clonedDescriptor.getName();
      if (pluginName == null) {
        pluginName = UNNAMED_GEOLADRIS_CORE_PLUGIN;
      }
      if (UNNAMED_GEOLADRIS_CORE_PLUGIN.equals(pluginName)) {
        PluginDescriptor unnamedPluginDescriptor =
            namePluginDescriptor.get(UNNAMED_GEOLADRIS_CORE_PLUGIN);
        if (unnamedPluginDescriptor != null) {
          unnamedPluginDescriptor.mergeConfiguration(clonedDescriptor.getConfiguration());
          unnamedPluginDescriptor.mergeRequireJSPaths(clonedDescriptor.getRequireJSPathsMap());
          unnamedPluginDescriptor.mergeRequireJSShims(clonedDescriptor.getRequireJSShims());
          for (String module : pluginDescriptor.getModules()) {
            unnamedPluginDescriptor.addModule(module);
          }
          for (String stylesheet : pluginDescriptor.getStylesheets()) {
            unnamedPluginDescriptor.addStylesheet(stylesheet);
          }
        } else {
          clonedDescriptor.setName(UNNAMED_GEOLADRIS_CORE_PLUGIN);
          clonedDescriptor.setInstallInRoot(true);
          namePluginDescriptor.put(pluginName, clonedDescriptor);
        }
      } else {
        namePluginDescriptor.put(pluginName, clonedDescriptor);
      }
    }
  }

  public void merge(String pluginName, JSONObject overridingPluginConfiguration) {
    if (pluginName == UNNAMED_GEOLADRIS_CORE_PLUGIN) {
      @SuppressWarnings("unchecked")
      Set<String> modules = overridingPluginConfiguration.keySet();
      for (String overridingConfigurationModuleName : modules) {
        JSONObject overridingModuleConfiguration =
            overridingPluginConfiguration.getJSONObject(overridingConfigurationModuleName);

        /*
         * Search for the module on each plugin installed in root, unnamed included
         */
        boolean found = false;
        Set<String> pluginNames = namePluginDescriptor.keySet();
        for (String searchPluginName : pluginNames) {
          PluginDescriptor descriptor = namePluginDescriptor.get(searchPluginName);
          if (descriptor.isInstallInRoot()) {
            JSONObject moduleConfiguration =
                descriptor.getConfiguration().getJSONObject(overridingConfigurationModuleName);
            if (moduleConfiguration != null && !moduleConfiguration.isNullObject()) {
              JSONObject mergedModuleConfiguration =
                  JSONUtils.merge(moduleConfiguration, overridingModuleConfiguration);
              descriptor.getConfiguration().element(overridingConfigurationModuleName,
                  mergedModuleConfiguration);
              found = true;
              break;
            }
          }
        }

        /*
         * If not found on plugins installed in root, add it to the unnamed plugin
         */
        if (!found) {
          PluginDescriptor descriptor = namePluginDescriptor.get(UNNAMED_GEOLADRIS_CORE_PLUGIN);
          descriptor.getConfiguration().put(overridingConfigurationModuleName,
              overridingModuleConfiguration);
        }
      }
    } else {
      PluginDescriptor pluginDescriptor = namePluginDescriptor.get(pluginName);
      if (pluginDescriptor == null) {
        pluginDescriptor = new PluginDescriptor();
        pluginDescriptor.mergeConfiguration(overridingPluginConfiguration);
        pluginDescriptor.setName(pluginName);
        namePluginDescriptor.put(pluginName, pluginDescriptor);
      } else {
        if (!overridingPluginConfiguration.has(CONF_OVERRIDE)
            || !overridingPluginConfiguration.getBoolean(CONF_OVERRIDE)) {
          pluginDescriptor.mergeConfiguration(overridingPluginConfiguration);
        } else {
          pluginDescriptor.setConfiguration(overridingPluginConfiguration);
        }
      }
    }
  }

  public PluginDescriptor[] getEnabled() {
    if (enabled == null) {
      ArrayList<PluginDescriptor> ret = new ArrayList<PluginDescriptor>();
      for (PluginDescriptor pluginDescriptor : namePluginDescriptor.values()) {
        JSONObject pluginConfiguration = pluginDescriptor.getConfiguration();

        if (!pluginConfiguration.has(CONF_ENABLED)
            || pluginConfiguration.getBoolean(CONF_ENABLED) == true) {
          pluginConfiguration.remove(CONF_ENABLED);
          pluginConfiguration.remove(CONF_OVERRIDE);

          ret.add(pluginDescriptor);
        }
      }

      enabled = ret.toArray(new PluginDescriptor[ret.size()]);
    }
    return enabled;
  }

  public PluginDescriptor get(String pluginName) {
    return namePluginDescriptor.get(pluginName);
  }

  public JSONObject getQualifiedConfiguration(String pluginName) {
    PluginDescriptor descriptor = namePluginDescriptor.get(pluginName);
    JSONObject descriptorConfiguration = descriptor.getConfiguration();
    if (descriptor.isInstallInRoot()) {
      return descriptorConfiguration;
    } else {
      // prefix all keys with plugin name
      JSONObject qualifiedConfiguration = new JSONObject();
      @SuppressWarnings("unchecked")
      Set<String> moduleNames = descriptorConfiguration.keySet();
      for (String moduleName : moduleNames) {
        qualifiedConfiguration.put(descriptor.getName() + "/" + moduleName,
            descriptorConfiguration.get(moduleName));
      }
      return qualifiedConfiguration;
    }
  }

}
