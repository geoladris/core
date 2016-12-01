package org.geoladris.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoladris.JEEContextAnalyzer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class GenerateMinifiedResourcesTest {
  private GenerateMinifiedResources mojo;
  private File testDir, buildConfig, main, wroXML;

  @Before
  public void setup() throws IOException {
    this.testDir = File.createTempFile("geoladris", "");
    this.testDir.delete();
    this.testDir.mkdir();

    this.buildConfig = new File(testDir, "buildconfig.js");
    this.wroXML = new File(testDir, "wro.xml");

    this.mojo = new GenerateMinifiedResources();
    this.mojo.buildConfigPath = this.buildConfig.getAbsolutePath();
    this.mojo.wroXmlPath = this.wroXML.getAbsolutePath();
    this.mojo.webResourcesDir = getClass().getResource("/").getPath();

    this.main = new File(this.mojo.webResourcesDir + File.separator
        + JEEContextAnalyzer.CLIENT_RESOURCES_DIR + File.separator + "main.js");
  }

  @After
  public void teardown() throws IOException {
    if (testDir != null && testDir.exists()) {
      FileUtils.deleteDirectory(testDir);
    }
  }

  @Test
  public void generatesBuildConfigJS() throws Exception {
    this.mojo.execute();

    assertTrue(this.buildConfig.exists());
    String content = IOUtils.toString(new FileInputStream(this.buildConfig));
    // The JSON object is surrounded by brackets
    content = content.substring(1, content.length() - 2);
    JSONObject config = JSONObject.fromObject(content);

    JSONObject paths = config.getJSONObject("paths");
    assertEquals(2, paths.size());
    assertEquals("plugin/modules/module", paths.getString("plugin/module"));
    assertEquals("plugin/modules/../jslib/jquery.mustache", paths.getString("mustache"));

    JSONArray deps = config.getJSONArray("deps");
    assertEquals(1, deps.size());
    assertEquals("plugin/module", deps.get(0));

    assertEquals(
        this.mojo.webResourcesDir + File.separator + JEEContextAnalyzer.CLIENT_RESOURCES_DIR,
        config.get("baseUrl"));
  }

  @Test
  public void generatesMainJS() throws Exception {
    this.mojo.execute();

    assertTrue(this.main.exists());
    String content = IOUtils.toString(new FileInputStream(this.main));

    // Get configuration from require.config({...}) without comments (//)
    Pattern pattern =
        Pattern.compile("\\Qrequire.config({\\E(.+)\\Q});\\E", Pattern.MULTILINE | Pattern.DOTALL);
    Matcher matcher = pattern.matcher(content);
    matcher.find();
    String configStr = matcher.group(1);
    configStr = configStr.replaceAll("//.*", "");
    JSONObject config = JSONObject.fromObject("{" + configStr + "}");

    JSONObject paths = config.getJSONObject("paths");
    assertEquals(2, paths.size());
    assertEquals("plugin/modules/module", paths.getString("plugin/module"));
    assertEquals("plugin/modules/../jslib/jquery.mustache", paths.getString("mustache"));

    assertEquals("modules", config.getString("baseUrl"));
  }

  @Test
  public void generatesWroXML() throws Exception {
    this.mojo.execute();

    assertTrue(this.wroXML.exists());
    String content = IOUtils.toString(new FileInputStream(this.wroXML));

    assertTrue(content.contains("<css>/plugin/styles/**.css</css>"));
    assertTrue(content.contains("<css>/plugin/modules/**.css</css>"));
    assertTrue(content.contains("<css>/plugin/theme/**.css</css>"));
  }
}
