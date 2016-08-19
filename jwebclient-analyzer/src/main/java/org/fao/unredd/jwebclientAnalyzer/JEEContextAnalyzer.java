package org.fao.unredd.jwebclientAnalyzer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

public class JEEContextAnalyzer {
	public static final String JAVA_CLASSPATH_PLUGIN_NAME = "j";

	private static Logger logger = Logger.getLogger(JEEContextAnalyzer.class);

	private ArrayList<String> js = new ArrayList<String>();
	private ArrayList<String> css = new ArrayList<String>();
	private Map<String, String> requirejsPaths = new HashMap<String, String>();
	private Map<String, String> requirejsShims = new HashMap<String, String>();
	private Map<String, JSONObject> configurationMap = new HashMap<String, JSONObject>();

	public JEEContextAnalyzer(Context context) {
		this(context, "nfms", "nfms");
	}

	public JEEContextAnalyzer(Context context, String pluginConfDir,
			String webResourcesDir) {
		scanClasses(context, pluginConfDir, webResourcesDir, true);
		scanJars(context, pluginConfDir, webResourcesDir, true);
		scanNoJava(context, false);
	}

	private void scanNoJava(Context context, boolean installInRoot) {
		File rootFolder = context.getNoJavaRoot();
		if (rootFolder != null) {
			File[] plugins = rootFolder.listFiles();
			if (plugins != null) {
				for (File pluginRoot : plugins) {
					PluginConfigEntryListener noJavaPluginConfListener = new PluginConfigEntryListener(
							pluginRoot.getName(), "", installInRoot);
					WebResourcesEntryListener noJavaWebResourcesListener = new WebResourcesEntryListener(
							pluginRoot.getName(), "");
					extractInfo(context, noJavaPluginConfListener,
							noJavaWebResourcesListener, new NoJavaPluginLayout(
									pluginRoot));
				}
			}
		}
	}

	private void scanClasses(Context context, String pluginConfDir,
			String webResourcesDir, boolean installInRoot) {
		PluginConfigEntryListener pluginConfListener = new PluginConfigEntryListener(
				JAVA_CLASSPATH_PLUGIN_NAME, pluginConfDir + File.separator,
				installInRoot);
		WebResourcesEntryListener webResourcesListener = new WebResourcesEntryListener(
				JAVA_CLASSPATH_PLUGIN_NAME, webResourcesDir + File.separator);
		ClassesPluginLayout pluginLayout = new ClassesPluginLayout(
				context.getClientRoot(), pluginConfDir, webResourcesDir);
		if (pluginLayout.rootFolderExists()) {
			extractInfo(context, pluginConfListener, webResourcesListener,
					pluginLayout);
		}
	}

	private void extractInfo(Context context,
			PluginConfigEntryListener pluginConfListener,
			WebResourcesEntryListener webResourcesListener,
			PluginLayout pluginLayout) {
		scanDir(context, pluginLayout.getConfigurationRoot(),
				pluginLayout.getReferenceFolder(), pluginConfListener);
		webResourcesListener.setInstallInRoot(pluginConfListener.installInRoot);
		scanDir(context, pluginLayout.getWebResourcesRoot(),
				pluginLayout.getReferenceFolder(), webResourcesListener);
	}

	private void scanDir(Context context, final File dir,
			final File referenceFolder, ContextEntryListener listener) {
		if (dir.isDirectory()) {
			Iterator<File> allFiles = FileUtils.iterateFiles(dir,
					relevantExtensions, TrueFileFilter.INSTANCE);

			int rootPathLength = referenceFolder.getAbsolutePath().length() + 1;
			while (allFiles.hasNext()) {
				File file = allFiles.next();
				String name = file.getAbsolutePath();
				final String relativePath = name.substring(rootPathLength);
				try {
					ContextEntryReader contentReader = new ContextEntryReader() {

						@Override
						public String getContent() throws IOException {
							InputStream input = new BufferedInputStream(
									new FileInputStream(new File(
											referenceFolder, relativePath)));
							String content = IOUtils.toString(input);
							input.close();

							return content;
						}
					};
					listener.accept(relativePath, contentReader);
				} catch (IOException e) {
					logger.info("Cannot analyze file:" + relativePath);
				}
			}
		}
	}

	private void scanJars(Context context, String pluginConfDir,
			String webResourcesDir, boolean installInRoot) {
		Set<String> libJars = context.getLibPaths();
		for (Object jar : libJars) {
			PluginConfigEntryListener pluginConfListener = new PluginConfigEntryListener(
					JAVA_CLASSPATH_PLUGIN_NAME, pluginConfDir + File.separator,
					installInRoot);
			WebResourcesEntryListener webResourcesListener = new WebResourcesEntryListener(
					JAVA_CLASSPATH_PLUGIN_NAME, webResourcesDir
							+ File.separator);

			processJar(context, jar, pluginConfListener);
			webResourcesListener
					.setInstallInRoot(pluginConfListener.installInRoot);
			processJar(context, jar, webResourcesListener);
		}
	}

	private void processJar(Context context, Object jar,
			ContextEntryListener listener) {
		InputStream jarStream = context.getLibAsStream(jar.toString());
		final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
				jarStream));
		ZipEntry entry;
		try {
			while ((entry = zis.getNextEntry()) != null) {
				String entryPath = entry.getName();
				if (relevantExtensions.accept(new File(entryPath))) {
					ContextEntryReader contentReader = new ContextEntryReader() {

						@Override
						public String getContent() throws IOException {
							return IOUtils.toString(zis);
						}
					};

					listener.accept(entryPath, contentReader);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Cannot start the application", e);
		} finally {
			try {
				zis.close();
			} catch (IOException e) {
			}
		}
	}

	public List<String> getRequireJSModuleNames() {
		return js;
	}

	public List<String> getCSSRelativePaths() {
		return css;
	}

	public Map<String, String> getNonRequirePathMap() {
		return requirejsPaths;
	}

	public Map<String, String> getNonRequireShimMap() {
		return requirejsShims;
	}

	public Map<String, JSONObject> getConfigurationElements() {
		return configurationMap;
	}

	private interface ContextEntryListener {

		void accept(String path, ContextEntryReader contentReader)
				throws IOException;

	}

	private interface ContextEntryReader {
		String getContent() throws IOException;
	}

	private static IOFileFilter relevantExtensions = new IOFileFilter() {

		@Override
		public boolean accept(File file, String name) {
			return true;
		}

		@Override
		public boolean accept(File file) {
			String lowerCase = file.getName().toLowerCase();
			return lowerCase.endsWith(".js") || lowerCase.endsWith(".css")
					|| lowerCase.endsWith(".json");
		}
	};

	private class PluginConfigEntryListener implements ContextEntryListener {
		private String pathPrefix;
		private String pluginName;
		private boolean installInRoot;

		public PluginConfigEntryListener(String pluginName, String entryRoot,
				boolean installInRoot) {
			this.pluginName = pluginName;
			this.pathPrefix = entryRoot;
			this.installInRoot = installInRoot;
		}

		@Override
		public void accept(String path, ContextEntryReader contentReader)
				throws IOException {
			if (path.matches("\\Q" + pathPrefix + "\\E[\\w-]+\\Q-conf.json\\E")) {
				PluginDescriptor pluginDescriptor = new PluginDescriptor(
						contentReader.getContent());
				Boolean installInRoot = pluginDescriptor.isInstallInRoot();
				if (installInRoot != null) {
					this.installInRoot = installInRoot;
				}
				String pluginNamePrefix;
				if (this.installInRoot) {
					pluginNamePrefix = null;
				} else {
					pluginNamePrefix = pluginName;
				}
				requirejsPaths.putAll(pluginDescriptor
						.getRequireJSPathsMap(pluginNamePrefix));
				requirejsShims.putAll(pluginDescriptor
						.getRequireJSShims(pluginNamePrefix));
				configurationMap.putAll(pluginDescriptor.getConfigurationMap());
			}
		}

	}

	private class WebResourcesEntryListener implements ContextEntryListener {
		private String pathPrefix;
		private String pluginName;
		private boolean installInRoot = false;

		public WebResourcesEntryListener(String pluginName, String pathPrefix) {
			this.pathPrefix = pathPrefix;
			this.pluginName = pluginName;
		}

		public void setInstallInRoot(boolean installInRoot) {
			this.installInRoot = installInRoot;
		}

		@Override
		public void accept(String path, ContextEntryReader contentReader)
				throws IOException {
			String styles = "styles";
			String stylesPrefix = pathPrefix + styles;
			String modules = "modules";
			String modulesPrefix = pathPrefix + modules;
			if (path.startsWith(modulesPrefix)) {
				if (path.endsWith(".css")) {
					String output = buildCSSURL(path, modules);
					css.add(output);
				}
				if (path.endsWith(".js")) {
					File pathFile = new File(path);
					String name = pathFile.getName();
					name = name.substring(0, name.length() - 3);
					if (!installInRoot) {
						name = pluginName != null ? pluginName + "/" + name
								: name;
					}
					js.add(name);
				}
			} else {
				if (path.startsWith(stylesPrefix) && path.endsWith(".css")) {
					String output = buildCSSURL(path, styles);
					css.add(output);
				}
			}
		}

		private String buildCSSURL(String path, String folderName) {
			String urlPath = path.substring(pathPrefix.length());
			if (!installInRoot) {
				return urlPath.replace(folderName + "/", folderName + "/"
						+ pluginName + "/");
			} else {
				return urlPath;
			}
		}

	}
}
