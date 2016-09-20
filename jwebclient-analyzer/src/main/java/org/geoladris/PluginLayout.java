package org.geoladris;

import java.io.File;

public interface PluginLayout {
	/**
	 * Gets the folder inside the plugin root where web resources are to be
	 * found (directly or in one subdirectory)
	 * 
	 * @return
	 */
	File getWebResourcesRoot();

	/**
	 * @return If the root folder of the plugin exists
	 */
	boolean rootFolderExists();

	/**
	 * @return The folder to be used as reference to obtain relative paths
	 */
	File getReferenceFolder();

}
