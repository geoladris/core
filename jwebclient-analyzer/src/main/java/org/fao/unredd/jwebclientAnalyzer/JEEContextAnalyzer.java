package org.fao.unredd.jwebclientAnalyzer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

public class JEEContextAnalyzer {
	private static Logger logger = Logger.getLogger(JEEContextAnalyzer.class);

	private PluginDescriptor currentPlugin;
	private Set<PluginDescriptor> pluginDescriptors;

	public JEEContextAnalyzer(Context context) {
		this.pluginDescriptors = new HashSet<>();

		scanClasses(context);
		scanJars(context);
		scanNoJava(context);
	}

	private void scanClasses(Context context) {
		PluginConfigEntryListener pluginConfListener = new PluginConfigEntryListener(
				Constants.CLIENT_RESOURCES_DIR + File.separator, true);
		WebResourcesEntryListener webResourcesListener = new WebResourcesEntryListener(
				Constants.CLIENT_RESOURCES_DIR + File.separator);

		ClassesPluginLayout pluginLayout = new ClassesPluginLayout(
				context.getClientRoot());
		if (pluginLayout.rootFolderExists()) {
			this.currentPlugin = new PluginDescriptor();
			extractInfo(context, pluginLayout, pluginConfListener,
					webResourcesListener);
			this.pluginDescriptors.add(this.currentPlugin);
			this.currentPlugin = null;
		}
	}

	private void scanJars(Context context) {
		PluginConfigEntryListener pluginConfListener = new PluginConfigEntryListener(
				Constants.CLIENT_RESOURCES_DIR + File.separator, true);
		WebResourcesEntryListener webResourcesListener = new WebResourcesEntryListener(
				Constants.CLIENT_RESOURCES_DIR + File.separator);

		Set<String> libJars = context.getLibPaths();
		for (Object jar : libJars) {
			this.currentPlugin = new PluginDescriptor();
			processJar(context, jar, pluginConfListener);
			processJar(context, jar, webResourcesListener);
			this.pluginDescriptors.add(this.currentPlugin);
			this.currentPlugin = null;
		}
	}

	private void scanNoJava(Context context) {
		PluginConfigEntryListener pluginConfListener = new PluginConfigEntryListener(
				"", false);
		WebResourcesEntryListener webResourcesListener = new WebResourcesEntryListener(
				"");

		File rootFolder = context.getNoJavaRoot();
		if (rootFolder != null) {
			File[] plugins = rootFolder.listFiles();
			if (plugins != null) {
				for (File pluginRoot : plugins) {
					this.currentPlugin = new PluginDescriptor();
					this.currentPlugin.setName(pluginRoot.getName());
					extractInfo(context, new NoJavaPluginLayout(pluginRoot),
							pluginConfListener, webResourcesListener);
					this.pluginDescriptors.add(this.currentPlugin);
					this.currentPlugin = null;
				}
			}
		}
	}

	private void processJar(Context context, Object jar,
			ContextEntryListener listener) {
		InputStream jarStream = context.getLibAsStream(jar.toString());
		final ZipInputStream zis = new ZipInputStream(
				new BufferedInputStream(jarStream));
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

	private void extractInfo(Context context, PluginLayout pluginLayout,
			PluginConfigEntryListener pluginConfListener,
			WebResourcesEntryListener webResourcesListener) {
		scanDir(context, pluginLayout.getWebResourcesRoot(),
				pluginLayout.getReferenceFolder(), pluginConfListener);
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

	private class PluginConfigEntryListener implements ContextEntryListener {
		private String pathPrefix;
		private boolean defaultInstallInRoot;

		public PluginConfigEntryListener(String pathPrefix,
				boolean defaultInstallInRoot) {
			this.pathPrefix = pathPrefix;
			this.defaultInstallInRoot = defaultInstallInRoot;
		}

		@Override
		public void accept(String path, ContextEntryReader contentReader)
				throws IOException {
			if (path.matches(
					"\\Q" + pathPrefix + "\\E[\\w-]+\\Q-conf.json\\E")) {
				String name = new File(path).getName();
				name = name.substring(0, name.length() - "-conf.json".length());
				PluginDescriptorFileReader reader = new PluginDescriptorFileReader(
						contentReader.getContent(), defaultInstallInRoot, name);
				reader.fillPluginDescriptor(currentPlugin);
			}
		}
	}

	private class WebResourcesEntryListener implements ContextEntryListener {
		private String pathPrefix;
		private String modulesPrefix, stylesPrefix, themePrefix;

		public WebResourcesEntryListener(String pathPrefix) {
			this.pathPrefix = pathPrefix;
			this.stylesPrefix = pathPrefix + "styles";
			this.modulesPrefix = pathPrefix + "modules";
			this.themePrefix = pathPrefix + "theme";
		}

		@Override
		public void accept(String path, ContextEntryReader contentReader)
				throws IOException {
			File pathFile = new File(path);
			if (path.startsWith(modulesPrefix)) {
				if (path.endsWith(".css")) {
					String stylesheet = path.substring(pathPrefix.length());
					currentPlugin.addStylesheet(stylesheet);
				} else if (path.endsWith(".js")) {
					String name = pathFile.getName();
					name = name.substring(0, name.length() - 3);
					currentPlugin.addModule(name);
				}
			} else if ((path.startsWith(stylesPrefix)
					|| path.startsWith(themePrefix)) && path.endsWith(".css")) {
				String stylesheet = path.substring(pathPrefix.length());
				currentPlugin.addStylesheet(stylesheet);
			}
		}
	}
}