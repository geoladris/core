package org.geoladris.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.geoladris.Geoladris;
import org.geoladris.JEEContextAnalyzer;
import org.geoladris.PluginDescriptor;
import org.geoladris.config.Config;

public class ClientContentServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static Logger logger = Logger.getLogger(ClientContentServlet.class);

  private String testingClasspathRoot = "";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    Config config = (Config) req.getAttribute(Geoladris.ATTR_CONFIG);

    String pathInfo = req.getRequestURI();
    // Remove context path and app path
    pathInfo = pathInfo.substring(req.getContextPath().length() + 1);
    Object app = req.getAttribute(Geoladris.ATTR_APP);
    if (app != null && app.toString().length() > 0) {
      pathInfo = pathInfo.substring(app.toString().length() + 1);
    }

    // Is this just a file in static folder?
    File confStaticFile = new File(config.getDir(), pathInfo);
    if (confStaticFile.isFile()) {
      sendFile(confStaticFile, pathInfo, req, resp);
      return;
    }

    String[] parts = pathInfo.split(Pattern.quote("/"));
    Locale locale = (Locale) req.getAttribute(Geoladris.ATTR_LOCALE);
    PluginDescriptor[] enabled = config.getPluginConfig(locale);

    // is it in the root plugin?
    for (PluginDescriptor plugin : enabled) {
      if (!plugin.isInstallInRoot()) {
        continue;
      }

      String resourcePath = testingClasspathRoot + File.separator
          + JEEContextAnalyzer.CLIENT_RESOURCES_DIR + File.separator + plugin.getName()
          + File.separator + StringUtils.join(parts, File.separator);
      InputStream classPathResource = this.getClass().getResourceAsStream(resourcePath);
      if (classPathResource != null) {
        sendStream(classPathResource, pathInfo, resp);
        return;
      }
    }

    if (parts.length >= 3) {
      String modulesOrStylesOrJsLib = parts[0];
      String pluginName = parts[1];
      String path = StringUtils.join(parts, File.separator, 2, parts.length);

      for (PluginDescriptor plugin : enabled) {
        if (plugin.isInstallInRoot()) {
          continue;
        }

        if (plugin.getName().equals(pluginName)) {
          File noJavaPluginFile = new File(config.getNoJavaPluginRoot(),
              pluginName + File.separator + modulesOrStylesOrJsLib + File.separator + path);
          if (noJavaPluginFile.isFile()) {
            sendFile(noJavaPluginFile, pathInfo, req, resp);
            return;
          } else {
            // It is a Java named plugin
            String resourcePath = testingClasspathRoot + File.separator
                + JEEContextAnalyzer.CLIENT_RESOURCES_DIR + File.separator + pluginName
                + File.separator + modulesOrStylesOrJsLib + File.separator + path;
            InputStream classPathResource = this.getClass().getResourceAsStream(resourcePath);
            if (classPathResource != null) {
              sendStream(classPathResource, pathInfo, resp);
              return;
            }
          }
        }
      }
    } else {
      InputStream stream = getServletContext().getResourceAsStream(pathInfo);
      if (stream != null) {
        sendStream(stream, pathInfo, resp);
        return;
      }
    }

    throw new StatusServletException(404, "The file could not be found: " + pathInfo);
  }

  void sendFile(File file, String resource, HttpServletRequest request,
      HttpServletResponse response) throws StatusServletException, IOException {
    // Manage cache headers: Last-Modified and If-Modified-Since
    long ifModifiedSince = request.getDateHeader("If-Modified-Since");
    long lastModified = file.lastModified();
    if (ifModifiedSince >= (lastModified / 1000 * 1000)) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }
    response.setDateHeader("Last-Modified", lastModified);
    sendStream(new FileInputStream(file), resource, response);
  }

  void sendStream(InputStream stream, String resource, HttpServletResponse response)
      throws StatusServletException {
    setContentType(resource, response);

    if (stream == null) {
      throw new StatusServletException(404, "The file could not be found: " + resource);
    } else {
      // Send contents
      try {
        IOUtils.copy(stream, response.getOutputStream());
        response.setStatus(HttpServletResponse.SC_OK);
      } catch (IOException e) {
        logger.error("Error reading file", e);
        throw new StatusServletException(500, "Could transfer the resource");
      }
    }
  }

  void setContentType(String resource, HttpServletResponse response) {
    String type;
    if (resource.endsWith("css")) {
      type = "text/css";
    } else if (resource.endsWith("js")) {
      type = "application/javascript";
    } else if (resource.endsWith("svg")) {
      type = "image/svg+xml";
    } else {
      FileNameMap fileNameMap = URLConnection.getFileNameMap();
      type = fileNameMap.getContentTypeFor(resource);
    }
    response.setContentType(type);
  }

  public void setTestingClasspathRoot(String testingClasspathRoot) {
    this.testingClasspathRoot = testingClasspathRoot;
  }
}
