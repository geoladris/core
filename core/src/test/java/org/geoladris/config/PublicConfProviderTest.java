package org.geoladris.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.sf.json.JSONObject;

public class PublicConfProviderTest {
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private PublicConfProvider provider;
  private PortalRequestConfiguration requestConfig;

  @Before
  public void setup() throws IOException {
    this.provider = new PublicConfProvider();
    this.requestConfig = mock(PortalRequestConfiguration.class);
    when(this.requestConfig.getConfigDir()).thenReturn(this.folder.getRoot());
  }

  @Test
  public void missingPublicConfFile() throws Exception {
    Map<String, JSONObject> conf =
        this.provider.getPluginConfig(this.requestConfig, mock(HttpServletRequest.class));
    assertNull(conf);
  }

  @Test
  public void validPublicConfFile() throws Exception {
    File tmp = new File(folder.getRoot(), PublicConfProvider.FILE);
    FileWriter writer = new FileWriter(tmp);
    String pluginName = "p1";
    IOUtils.write("{ '" + pluginName + "' : { mymodule : {'a' : true }}}", writer);
    writer.close();

    Map<String, JSONObject> conf =
        provider.getPluginConfig(this.requestConfig, mock(HttpServletRequest.class));
    assertEquals(1, conf.size());
    assertTrue(conf.containsKey(pluginName));
    JSONObject pluginConf = conf.get(pluginName);
    assertTrue(pluginConf.getJSONObject("mymodule").getBoolean("a"));

    tmp.delete();
  }

  @Test
  public void canBeCached() {
    assertTrue(this.provider.canBeCached());
  }
}
