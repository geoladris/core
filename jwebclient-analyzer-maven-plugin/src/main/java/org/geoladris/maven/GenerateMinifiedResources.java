package org.geoladris.maven;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.geoladris.Context;
import org.geoladris.JEEContextAnalyzer;
import org.geoladris.PluginDescriptor;
import org.geoladris.RequireTemplate;

/**
 * Generates a RequireJS main.js module and configuration file for the requirejs and CSS
 * minification process.
 * 
 * @author fergonco
 */
@Mojo(name = "generate-minified")
public class GenerateMinifiedResources extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.directory}/requirejs")
  protected String webResourcesDir;

  @Parameter(defaultValue = "${project.build.directory}/buildconfig.js")
  protected String buildConfigPath;

  @Parameter(defaultValue = "${project.build.directory}/wro.xml")
  protected String wroXmlPath;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    JEEContextAnalyzer analyzer = new JEEContextAnalyzer(new ExpandedClientContext());

    // main.js
    String mainSrc = this.webResourcesDir + File.separator + "main.js";
    String mainDest = this.webResourcesDir + File.separator
        + JEEContextAnalyzer.CLIENT_RESOURCES_DIR + File.separator + "main.js";
    try {
      InputStream mainStream = new FileInputStream(mainSrc);
      processTemplate(analyzer, mainStream, mainDest);
    } catch (FileNotFoundException e) {
      throw new MojoExecutionException("Cannot access main template", e);
    }

    // buildconfig.js
    InputStream buildStream = getClass().getResourceAsStream("/buildconfig.js");
    processTemplate(analyzer, buildStream, this.buildConfigPath);

    // wro.xml
    try {
      String template = IOUtils.toString(getClass().getResourceAsStream("/wro.xml"));
      String cssResources = "";
      for (PluginDescriptor plugin : analyzer.getPluginDescriptors()) {
        cssResources += "<css>/" + plugin.getName() + "/styles/**.css</css>\n";
        cssResources += "<css>/" + plugin.getName() + "/modules/**.css</css>\n";
        cssResources += "<css>/" + plugin.getName() + "/theme/**.css</css>\n";
      }
      String output = template.replaceAll("\\Q$cssResources\\E", cssResources);
      IOUtils.write(output, new FileOutputStream(this.wroXmlPath));
    } catch (IOException e) {
      throw new MojoExecutionException("Cannot write wro.xml file", e);
    }
  }

  private void processTemplate(JEEContextAnalyzer analyzer, InputStream templateStream,
      String outputPath) throws MojoExecutionException {

    Map<String, String> paths = new HashMap<String, String>();
    Map<String, String> shims = new HashMap<String, String>();
    List<String> moduleNames = new ArrayList<String>();
    for (PluginDescriptor plugin : analyzer.getPluginDescriptors()) {
      shims.putAll(plugin.getRequireJSShims());

      Map<String, String> requireJSPaths = plugin.getRequireJSPathsMap();
      Set<String> modules = plugin.getModules();

      for (String module : modules) {
        paths.put(module, relativize(module, plugin));
      }

      for (String key : requireJSPaths.keySet()) {
        paths.put(key, relativize(requireJSPaths.get(key), plugin));
      }

      moduleNames.addAll(modules);
    }

    RequireTemplate template = new RequireTemplate(templateStream, paths, shims, moduleNames,
        this.webResourcesDir + "/" + JEEContextAnalyzer.CLIENT_RESOURCES_DIR);
    try {
      String content = template.generate();
      templateStream.close();

      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputPath));
      IOUtils.write(content, outputStream);
      outputStream.close();
    } catch (IOException e) {
    }
  }

  private String relativize(String path, PluginDescriptor plugin) {
    String name = plugin.getName();
    path = path.replace(name + "/", "");
    return name + "/modules/" + path;
  }

  private class ExpandedClientContext implements Context {
    @Override
    public Set<String> getLibPaths() {
      return Collections.emptySet();
    }

    @Override
    public InputStream getLibAsStream(String jarFileName) {
      throw new UnsupportedOperationException("Internal error");
    }

    @Override
    public File[] getDirs() {
      return new File[] {
          new File(webResourcesDir + File.separator + JEEContextAnalyzer.CLIENT_RESOURCES_DIR)};
    }
  }
}
