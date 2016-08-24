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

	private Set<PluginDescriptor> pluginDescriptors = new HashSet<>();

	public JEEContextAnalyzer(Context context) {
		this(context, "nfms", "nfms");
	}

	public JEEContextAnalyzer(Context context, String pluginConfDir,
			String webResourcesDir) {
		scanWithPluginDescriptorListener(
				new PluginDescriptorListener(pluginConfDir, webResourcesDir),
				context);
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
