package org.fao.unredd;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.fao.unredd.jwebclientAnalyzer.Context;
import org.fao.unredd.jwebclientAnalyzer.JEEContextAnalyzer;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;
import org.fao.unredd.portal.ConfigFolder;
import org.fao.unredd.portal.DefaultConfig;
import org.fao.unredd.portal.PluginJSONConfigurationProvider;
import org.fao.unredd.portal.RoleConfigurationProvider;

public class AppContextListener implements ServletContextListener {
	public static final String ENV_CONFIG_CACHE = "NFMS_CONFIG_CACHE";
	public static final String INIT_PARAM_DIR = "PROTAL_CONFIG_DIR";

	public static final String ATTR_CONFIG = "config";
	public static final String ATTR_JS_PATHS = "js-paths";
	public static final String ATTR_CSS_PATHS = "css-paths";
	public static final String ATTR_REQUIREJS_PATHS = "requirejs-paths";
	public static final String ATTR_REQUIREJS_SHIMS = "requirejs-shims";
	public static final String ATTR_PLUGIN_CONFIGURATION = "plugin-configuration";

	/**
	 * @deprecated See {@link PluginDescriptor#getMergeConf()}.
	 */
	public static final String ATTR_MERGE_CONF_MODULES = "merge-conf-modules";

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		ServletContext servletContext = sce.getServletContext();
		contextInitialized(servletContext,
				new JEEContextAnalyzer(new JEEContext(servletContext)));
	}

	void contextInitialized(ServletContext servletContext,
			JEEContextAnalyzer context) {
		String rootPath = servletContext.getRealPath("/");
		String configInitParameter = servletContext
				.getInitParameter(INIT_PARAM_DIR);
		boolean configCache = Boolean
				.parseBoolean(System.getenv(ENV_CONFIG_CACHE));
		DefaultConfig config = new DefaultConfig(
				new ConfigFolder(rootPath, configInitParameter), configCache);
		config.addModuleConfigurationProvider(
				new PluginJSONConfigurationProvider());
		config.addModuleConfigurationProvider(
				new RoleConfigurationProvider(config.getDir()));
		servletContext.setAttribute(ATTR_CONFIG, config);
		servletContext.setAttribute(ATTR_JS_PATHS,
				context.getRequireJSModuleNames());
		servletContext.setAttribute(ATTR_CSS_PATHS,
				context.getCSSRelativePaths());
		servletContext.setAttribute(ATTR_REQUIREJS_PATHS,
				context.getNonRequirePathMap());
		servletContext.setAttribute(ATTR_REQUIREJS_SHIMS,
				context.getNonRequireShimMap());
		servletContext.setAttribute(ATTR_PLUGIN_CONFIGURATION,
				context.getConfigElements());
		servletContext.setAttribute(ATTR_MERGE_CONF_MODULES,
				context.getMergeConfModules());
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

	private class JEEContext implements Context {

		private ServletContext servletContext;

		public JEEContext(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<String> getLibPaths() {
			return servletContext.getResourcePaths("/WEB-INF/lib");
		}

		@Override
		public InputStream getLibAsStream(String jarFileName) {
			return servletContext.getResourceAsStream(jarFileName);
		}

		@Override
		public File getClientRoot() {
			return new File(servletContext.getRealPath("/WEB-INF/classes/"));
		}

	}

}
