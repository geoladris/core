package org.geoladris;

import java.io.File;

public class ClassesPluginLayout implements PluginLayout {

	private File rootFolder;

	public ClassesPluginLayout(File rootFolder) {
		super();
		this.rootFolder = rootFolder;
	}

	@Override
	public File getWebResourcesRoot() {
		return new File(rootFolder, JEEContextAnalyzer.CLIENT_RESOURCES_DIR);
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
