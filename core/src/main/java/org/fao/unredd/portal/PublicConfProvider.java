package org.fao.unredd.portal;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;

import de.csgis.commons.JSONContentProvider;
import net.sf.json.JSONObject;

/**
 * <p>
 * {@link ModuleConfigurationProvider} that returns the configuration from
 * <code>&lt;config_dir&gt;/public-conf.json</code>.
 * </p>
 * 
 * @author victorzinho
 */
public class PublicConfProvider implements ModuleConfigurationProvider {
	public static final String FILE_BASE = "public-conf";
	public static final String FILE = FILE_BASE + ".json";

	public static final String ROLE_DIR = "role_conf";

	private ConfigurationProviderHelper helper;
	private File file;

	public PublicConfProvider(File configDir,
			Map<String, PluginDescriptor> plugins) {
		JSONContentProvider contents = new JSONContentProvider(
				configDir.getAbsolutePath());
		this.helper = new ConfigurationProviderHelper(contents, plugins);
		this.file = new File(configDir, FILE);
	}

	@Override
	public Map<PluginDescriptor, JSONObject> getPluginConfig(
			PortalRequestConfiguration configurationContext,
			HttpServletRequest request) throws IOException {
		return file.exists() ? helper.getPluginConfig(FILE_BASE) : null;
	}

	@Override
	public boolean canBeCached() {
		return true;
	}
}
