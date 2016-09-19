package org.fao.unredd.jwebclientAnalyzer;

import java.io.File;

public class ClassesPluginLayout implements PluginLayout {

	private File rootFolder;

	public ClassesPluginLayout(File rootFolder) {
		super();
		this.rootFolder = rootFolder;
	}

	@Override
	public File getWebResourcesRoot() {
		return new File(rootFolder, Constants.CLIENT_RESOURCES_DIR);
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
