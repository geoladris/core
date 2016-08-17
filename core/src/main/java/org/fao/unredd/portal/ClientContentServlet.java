package org.fao.unredd.portal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fao.unredd.jwebclientAnalyzer.JEEContextAnalyzer;

public class ClientContentServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(ClientContentServlet.class);

	private String testingClasspathRoot = "";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Config config = (Config) getServletContext().getAttribute("config");

		String pathInfo = req.getServletPath() + req.getPathInfo();
		File file = null;
		InputStream stream = null;

		// Is this just a file in static folder?
		File confStaticFile = new File(config.getDir(), pathInfo);
		if (confStaticFile.isFile()) {
			file = confStaticFile;
		} else {
			// Is it a java client resource?
			String[] parts = pathInfo.substring(1).split(Pattern.quote("/"));
			if (parts.length >= 3) {
				String modulesOrStylesOrJsLib = parts[0];
				String pluginName = parts[1];
				String path = StringUtils.join(parts, File.separator, 2,
						parts.length);
				if (parts[1]
						.equals(JEEContextAnalyzer.JAVA_CLASSPATH_PLUGIN_NAME)) {
					String classpathResource = testingClasspathRoot + "/nfms"
							+ File.separator + modulesOrStylesOrJsLib
							+ File.separator + path;
					InputStream classPathResource = this.getClass()
							.getResourceAsStream(classpathResource);
					if (classPathResource != null) {
						stream = new BufferedInputStream(classPathResource);
					}
				} else {
					// Is it a no-java plugin resource?
					File noJavaPluginFile = new File(
							config.getNoJavaPluginRoot(), pluginName
									+ File.separator + modulesOrStylesOrJsLib
									+ File.separator + path);
					if (noJavaPluginFile.isFile()) {
						file = noJavaPluginFile;
					}
				}
			}

		}

		if (file != null) { // it was a file
			// Manage cache headers: Last-Modified and If-Modified-Since
			long ifModifiedSince = req.getDateHeader("If-Modified-Since");
			long lastModified = confStaticFile.lastModified();
			if (ifModifiedSince >= (lastModified / 1000 * 1000)) {
				resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
				return;
			}
			resp.setDateHeader("Last-Modified", lastModified);
			stream = new BufferedInputStream(
					new FileInputStream(confStaticFile));
		}

		// Set content type
		String type;
		if (pathInfo.endsWith("css")) {
			type = "text/css";
		} else if (pathInfo.endsWith("js")) {
			type = "application/javascript";
		} else {
			FileNameMap fileNameMap = URLConnection.getFileNameMap();
			type = fileNameMap.getContentTypeFor(pathInfo);
		}
		resp.setContentType(type);

		if (stream == null) {
			throw new StatusServletException(404,
					"The file could not be found: " + pathInfo);
		} else {
			// Send contents
			try {
				IOUtils.copy(stream, resp.getOutputStream());
				resp.setStatus(HttpServletResponse.SC_OK);
			} catch (IOException e) {
				logger.error("Error reading file", e);
				throw new StatusServletException(500,
						"Could transfer the resource");
			}
		}
	}

	public void setTestingClasspathRoot(String testingClasspathRoot) {
		this.testingClasspathRoot = testingClasspathRoot;
	}
}
