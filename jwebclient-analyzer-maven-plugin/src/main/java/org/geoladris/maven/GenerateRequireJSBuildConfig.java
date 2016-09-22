package org.geoladris.maven;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.geoladris.JEEContextAnalyzer;
import org.geoladris.PluginDescriptor;
import org.geoladris.RequireTemplate;

/**
 * Generates a RequireJS main.js module and configuration file for the requirejs minification
 * process.
 * 
 * The plugin operates on a folder that contains all the client resources, expects to find the
 * RequireJS modules in a "modules" folder and CSS stylesheets in the "styles" folder
 * 
 * @author fergonco
 */
@Mojo(name = "generate-buildconfig")
public class GenerateRequireJSBuildConfig extends AbstractMojo {

  /**
   * Root of the client resources
   */
  @Parameter
  protected String webClientFolder;

  /**
   * Path where the buildconfig file will be generated
   */
  @Parameter
  protected String buildconfigOutputPath;

  /**
   * Path where the main.js file will be generated
   */
  @Parameter
  protected String mainOutputPath;

  /**
   * Path to the main.js template to use.
   */
  @Parameter
  protected String mainTemplate;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    JEEContextAnalyzer analyzer =
        new JEEContextAnalyzer(new ExpandedClientContext(webClientFolder));

    try {
      InputStream mainStream = new FileInputStream(mainTemplate);
      processTemplate(analyzer, mainStream, mainOutputPath);
    } catch (IOException e) {
      throw new MojoExecutionException("Cannot access main template", e);
    }

    InputStream buildStream = getClass().getResourceAsStream("/buildconfig.js");
    processTemplate(analyzer, buildStream, buildconfigOutputPath);
  }

  private void processTemplate(JEEContextAnalyzer analyzer, InputStream templateStream,
      String outputPath) throws MojoExecutionException {

    Map<String, String> paths = new HashMap<String, String>();
    Map<String, String> shims = new HashMap<String, String>();
    List<String> moduleNames = new ArrayList<String>();
    for (PluginDescriptor plugin : analyzer.getPluginDescriptors()) {
      paths.putAll(plugin.getRequireJSPathsMap());
      shims.putAll(plugin.getRequireJSShims());
      moduleNames.addAll(plugin.getModules());
    }
    RequireTemplate template = new RequireTemplate(templateStream, paths, shims, moduleNames);
    try {
      String content = template.generate();
      templateStream.close();

      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputPath));
      IOUtils.write(content, outputStream);
      outputStream.close();
    } catch (IOException e) {
    }
  }

}
