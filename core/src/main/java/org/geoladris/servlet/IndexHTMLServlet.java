package org.geoladris.servlet;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.geoladris.PluginDescriptor;
import org.geoladris.config.Config;
import org.geoladris.config.PluginDescriptors;

public class IndexHTMLServlet extends HttpServlet {
	public static final String HTTP_PARAM_DEBUG = "debug";

	// System properties
	public static final String PROP_MINIFIED_JS = "MINIFIED_JS";

	// portal.properties
	public static final String PROP_TITLE = "title";

	public static final String OPTIMIZED_FOLDER = "optimized";

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty("resource.loader", "string");
		engine.setProperty("string.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.StringResourceLoader");
		engine.setProperty("string.resource.loader.repository.class",
				"org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl");
		engine.setProperty("runtime.log.logsystem.class",
				"org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
		engine.setProperty("runtime.log.logsystem.log4j.category", "velocity");
		engine.setProperty("runtime.log.logsystem.log4j.logger", "velocity");
		engine.init();
		VelocityContext context = new VelocityContext();

		String debug = req.getParameter(HTTP_PARAM_DEBUG);
		boolean minifiedjs;

		if (debug != null && Boolean.parseBoolean(debug)) {
			minifiedjs = false;
		} else {
			minifiedjs = Boolean.parseBoolean(System
					.getProperty(PROP_MINIFIED_JS));
		}

		Config config = (Config) getServletContext().getAttribute("config");
		ArrayList<String> styleSheets = new ArrayList<String>();
		if (minifiedjs) {
			styleSheets.add(OPTIMIZED_FOLDER + "/portal-style.css");
		} else {
			Locale locale = (Locale) req.getAttribute(LangFilter.ATTR_LOCALE);
			PluginDescriptors pluginDescriptors = config.getPluginConfig(
					locale, req);
			List<String> classPathStylesheets = new ArrayList<>();
			for (PluginDescriptor plugin : pluginDescriptors.getEnabled()) {
				classPathStylesheets.addAll(plugin.getStylesheets());
			}

			Collections.sort(classPathStylesheets, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					if (o1.startsWith("theme") || o2.startsWith("styles")) {
						return 1;
					} else if (o1.startsWith("styles")
							|| o2.startsWith("theme")) {
						return -1;
					} else {
						return 0;
					}
				}
			});
			styleSheets.addAll(classPathStylesheets);
			styleSheets.addAll(getStyleSheets(config, "modules"));
		}
		context.put("styleSheets", styleSheets);

		String queryString = req.getQueryString();
		String url = "config.js";
		if (queryString != null) {
			url += "?" + queryString;
		}
		context.put("configUrl", url);

		if (minifiedjs) {
			context.put("mainModulePath", OPTIMIZED_FOLDER + "/portal");
		} else {
			context.put("mainModulePath", "modules/main");
		}

		Properties props = config.getProperties();
		String title = "";
		if (props != null && props.containsKey(PROP_TITLE)) {
			title = "<title>" + props.getProperty(PROP_TITLE) + "</title>";
		}
		context.put("title", title);

		StringResourceRepository repo = StringResourceLoader.getRepository();
		String templateName = "/index.html";
		BufferedInputStream bis = new BufferedInputStream(this.getClass()
				.getResourceAsStream("/index.html"));
		String indexContent = IOUtils.toString(bis);
		bis.close();
		repo.putStringResource(templateName, indexContent);

		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");
		Template t = engine.getTemplate("/index.html");
		t.merge(context, resp.getWriter());
	}

	private ArrayList<String> getStyleSheets(Config config, String path) {
		File styleFolder = new File(config.getDir(), path);
		return getStyleSheets(styleFolder, path);
	}

	private ArrayList<String> getStyleSheets(File styleFolder, String path) {
		ArrayList<String> styleSheets = new ArrayList<String>();
		File[] styleSheetFiles = styleFolder.listFiles();
		if (styleSheetFiles != null) {
			for (File file : styleSheetFiles) {
				String fileName = file.getName();
				if (fileName.toLowerCase().endsWith(".css")) {
					styleSheets.add(path + "/" + fileName);
				}
			}
		}
		return styleSheets;
	}
}
