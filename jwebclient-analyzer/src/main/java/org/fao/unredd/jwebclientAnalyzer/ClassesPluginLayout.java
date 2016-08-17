package org.fao.unredd.jwebclientAnalyzer;

import java.io.File;

public class ClassesPluginLayout implements PluginLayout {

	private File rootFolder;
	private String configurationFolder;
	private String webResourcesFolder;

	public ClassesPluginLayout(File rootFolder, String configurationFolder,
			String webResourcesFolder) {
		super();
		this.rootFolder = rootFolder;
		this.configurationFolder = configurationFolder;
		this.webResourcesFolder = webResourcesFolder;
	}

	@Override
	public File getConfigurationRoot() {
		return new File(rootFolder, configurationFolder);
	}

	@Override
	public File getWebResourcesRoot() {
		return new File(rootFolder, webResourcesFolder);
	}

	@Override
	public boolean rootFolderExists() {
		return rootFolder.exists();
	}

	@Override
	public File getReferenceFolder() {
		return rootFolder;
	}

}
