package org.geoladris.maven;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Collection;
import java.util.Map;

import org.geoladris.JEEContextAnalyzer;
import org.geoladris.PluginDescriptor;
import org.geoladris.maven.ExpandedClientContext;
import org.junit.Test;

public class ExpandedClientContextTest {
  @Test
  public void checkExpandedClient() {
    JEEContextAnalyzer context =
        new JEEContextAnalyzer(new ExpandedClientContext("src/test/resources/test2"));

    PluginDescriptor plugin = context.getPluginDescriptors().iterator().next();
    checkList(plugin.getModules(), "module3");
    checkList(plugin.getStylesheets(), "modules/module3.css", "styles/general2.css");
    checkMapKeys(plugin.getRequireJSPathsMap(), "mustache");
    checkMapKeys(plugin.getRequireJSShims(), "mustache");
  }

  private void checkList(Collection<String> result, String... testEntries) {
    for (String entry : testEntries) {
      assertTrue(entry + " not in " + result, result.contains(entry));
    }

    assertEquals(result.size(), testEntries.length);
  }

  private void checkMapKeys(Map<String, ?> result, String... testKeys) {
    for (String entry : testKeys) {
      assertTrue(result.remove(entry) != null);
    }

    assertTrue(result.size() == 0);
  }
}
