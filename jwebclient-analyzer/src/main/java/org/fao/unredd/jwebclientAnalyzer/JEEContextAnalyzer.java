package org.fao.unredd.jwebclientAnalyzer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

import net.sf.json.JSONObject;

public class JEEContextAnalyzer {
	private static Logger logger = Logger.getLogger(JEEContextAnalyzer.class);

	/**
	 * @deprecated Use {@link #pluginDescriptors} instead.
	 */
	private ArrayList<String> js = new ArrayList<String>();

	/**
	 * @deprecated Use {@link #pluginDescriptors} instead.
	 */
	private ArrayList<String> css = new ArrayList<String>();
	/**
	 * @deprecated Use {@link #pluginDescriptors} instead.
	 */
	private Map<String, String> requirejsPaths = new HashMap<String, String>();
	/**
	 * @deprecated Use {@link #pluginDescriptors} instead.
	 */
	private Map<String, String> requirejsShims = new HashMap<String, String>();

	private Set<PluginDescriptor> pluginDescriptors = new HashSet<>();

	@Deprecated
	private Map<String, JSONObject> configurationMapDeprecated = new HashMap<String, JSONObject>();

	public JEEContextAnalyzer(Context context) {
		this(context, "nfms", "nfms");
	}

	public JEEContextAnalyzer(Context context, String pluginConfDir,
			String webResourcesDir) {
		PluginConfigEntryListener pluginConfListener = new PluginConfigEntryListener(
				pluginConfDir);
		WebResourcesEntryListener webResourcesListener = new WebResourcesEntryListener(
				webResourcesDir);

		scanClasses(context, pluginConfListener, webResourcesListener);
		scanJars(context, pluginConfListener, webResourcesListener);
		scanWithPluginDescriptorListener(
				new PluginDescriptorListener(pluginConfDir, webResourcesDir),
				context);
	}

	private void scanClasses(Context context,
			PluginConfigEntryListener pluginConfListener,
			WebResourcesEntryListener webResourcesListener) {
		File rootFolder = context.getClientRoot();
		if (rootFolder.exists()) {
			scanDir(context, new File(rootFolder, pluginConfListener.dir),
					pluginConfListener);
			scanDir(context, new File(rootFolder, webResourcesListener.dir),
					webResourcesListener);
		}
	}

	private void scanWithPluginDescriptorListener(
			PluginDescriptorListener pluginDescriptorListener,
			Context context) {
		// Scan directory
		File rootFolder = context.getClientRoot();
		if (rootFolder.exists()) {
			pluginDescriptorListener.reset();
			scanDir(context,
					new File(rootFolder, pluginDescriptorListener.confDir),
					pluginDescriptorListener);
			scanDir(context,
					new File(rootFolder, pluginDescriptorListener.resourcesDir),
					pluginDescriptorListener);
			this.pluginDescriptors.add(pluginDescriptorListener.descriptor);
		}

		// Scan jars
		Set<String> libJars = context.getLibPaths();
		for (Object jar : libJars) {
			pluginDescriptorListener.reset();
			scanJar(jar.toString(), context, pluginDescriptorListener);
			this.pluginDescriptors.add(pluginDescriptorListener.descriptor);
		}
	}

	private void scanDir(Context context, final File dir,
			ContextEntryListener listener) {
		if (dir.isDirectory()) {
			Iterator<File> allFiles = FileUtils.iterateFiles(dir,
					relevantExtensions, TrueFileFilter.INSTANCE);

			final File referenceFolder = dir.getParentFile();
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

	private void scanJars(Context context,
			ContextEntryListener... contextEntryListeners) {
		Set<String> libJars = context.getLibPaths();
		for (Object jar : libJars) {
			scanJar(jar.toString(), context, contextEntryListeners);
		}
	}

	private void scanJar(String jar, Context context,
			ContextEntryListener... contextEntryListeners) {
		InputStream jarStream = context.getLibAsStream(jar.toString());
		final ZipInputStream zis = new ZipInputStream(
				new BufferedInputStream(jarStream));
		ZipEntry entry;
		try {
			while ((entry = zis.getNextEntry()) != null) {
				String entryPath = entry.getName();
				if (relevantExtensions.accept(new File(entryPath))) {
					ContextEntryReader contentReader = new ContextEntryReader() {
						private String content;
						@Override
						public String getContent() throws IOException {
							if (content == null) {
								content = IOUtils.toString(zis);
							}
							return content;
						}
					};

					for (ContextEntryListener listener : contextEntryListeners) {
						listener.accept(entryPath, contentReader);
					}
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

	/**
	 * @deprecated Use {@link #getPluginDescriptors()} instead.
	 */
	public List<String> getRequireJSModuleNames() {
		return js;
	}

	/**
	 * @deprecated Use {@link #getPluginDescriptors()} instead.
	 */
	public List<String> getCSSRelativePaths() {
		return css;
	}

	/**
	 * @deprecated Use {@link #getPluginDescriptors()} instead.
	 */
	public Map<String, String> getNonRequirePathMap() {
		return requirejsPaths;
	}

	/**
	 * @deprecated Use {@link #getPluginDescriptors()} instead.
	 */
	public Map<String, String> getNonRequireShimMap() {
		return requirejsShims;
	}

	/**
	 * @deprecated Use {@link #getPluginDescriptors()} instead.
	 */
	public Map<String, JSONObject> getConfigurationElements() {
		return configurationMapDeprecated;
	}

	public Set<PluginDescriptor> getPluginDescriptors() {
		return pluginDescriptors;
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

	/**
	 * @deprecated Use {@link PluginDescriptorListener} instead.
	 */
	private class PluginConfigEntryListener implements ContextEntryListener {
		private String dir;

		public PluginConfigEntryListener(String dir) {
			this.dir = dir;
		}

		@Override
		public void accept(String path, ContextEntryReader contentReader)
				throws IOException {
			if (path.matches("\\Q" + dir + File.separator
					+ "\\E[\\w-]+\\Q-conf.json\\E")) {
				PluginDescriptor pluginDescriptor = new PluginDescriptor(
						contentReader.getContent());
				requirejsPaths.putAll(pluginDescriptor.getRequireJSPathsMap());
				requirejsShims.putAll(pluginDescriptor.getRequireJSShims());
				configurationMapDeprecated
						.putAll(pluginDescriptor.getConfigurationMap());
			}
		}
	}

	/**
	 * @deprecated Use {@link PluginDescriptorListener} instead.
	 */
	private class WebResourcesEntryListener implements ContextEntryListener {

		private String dir;

		public WebResourcesEntryListener(String dir) {
			this.dir = dir;
		}

		@Override
		public void accept(String path, ContextEntryReader contentReader)
				throws IOException {
			String stylesPrefix = dir + File.separator + "styles";
			String modulesPrefix = dir + File.separator + "modules";
			String themePrefix = dir + File.separator + "theme";
			File pathFile = new File(path);
			if (path.startsWith(modulesPrefix)) {
				if (path.endsWith(".css")) {
					String output = path.substring(dir.length() + 1);
					css.add(output);
				}
				if (path.endsWith(".js")) {
					String name = pathFile.getName();
					name = name.substring(0, name.length() - 3);
					js.add(name);
				}
			} else if ((path.startsWith(stylesPrefix)
					|| path.startsWith(themePrefix)) && path.endsWith(".css")) {
				String output = path.substring(dir.length() + 1);
				css.add(output);
			}
		}
	}

	private class PluginDescriptorListener implements ContextEntryListener {
		private PluginDescriptor descriptor;
		private String confDir, resourcesDir;
		private String modulesPrefix, stylesPrefix, themePrefix;

		public PluginDescriptorListener(String confDir, String resourcesDir) {
			this.confDir = confDir;
			this.resourcesDir = resourcesDir;
			this.stylesPrefix = resourcesDir + File.separator + "styles";
			this.modulesPrefix = resourcesDir + File.separator + "modules";
			this.themePrefix = resourcesDir + File.separator + "theme";
		}

		public void reset() {
			this.descriptor = new PluginDescriptor();
		}

		@Override
		public void accept(String path, ContextEntryReader contentReader)
				throws IOException {
			if (path.matches("\\Q" + confDir + File.separator
					+ "\\E[\\w-]+\\Q-conf.json\\E")) {
				this.descriptor.setConfiguration(contentReader.getContent());
				String name = new File(path).getName();
				name = name.substring(0, name.length() - "-conf.json".length());
				this.descriptor.setName(name);
			}

			File pathFile = new File(path);
			if (path.startsWith(modulesPrefix)) {
				if (path.endsWith(".css")) {
					String output = path.substring(resourcesDir.length() + 1);
					descriptor.getStylesheets().add(output);
				}
				if (path.endsWith(".js")) {
					String name = pathFile.getName();
					name = name.substring(0, name.length() - 3);
					descriptor.getModules().add(name);
				}
			} else if ((path.startsWith(stylesPrefix)
					|| path.startsWith(themePrefix)) && path.endsWith(".css")) {
				String output = path.substring(resourcesDir.length() + 1);
				descriptor.getStylesheets().add(output);
			}
		}
	}
}
