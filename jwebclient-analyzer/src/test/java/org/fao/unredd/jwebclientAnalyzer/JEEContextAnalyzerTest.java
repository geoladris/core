package org.fao.unredd.jwebclientAnalyzer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.input.BoundedInputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import net.sf.json.JSONObject;

public class JEEContextAnalyzerTest {
	private static File libFolder = new File(
			"src/test/resources/test1/WEB-INF/lib");

	@BeforeClass
	public static void packTest2AsJar() throws IOException {
		assertTrue(libFolder.exists() || libFolder.mkdirs());

		File jarFile = new File(libFolder, "test2.jar");
		assertTrue(!jarFile.exists() || jarFile.delete());

		FileOutputStream stream = new FileOutputStream(jarFile);
		File jarContentRoot = new File("src/test/resources/test2");
		Collection<File> files = FileUtils.listFiles(jarContentRoot,
				TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		JarOutputStream out = new JarOutputStream(stream);
		for (File file : files) {
			String entryName = file.getPath();
			entryName = entryName
					.substring(jarContentRoot.getPath().length() + 1);
			out.putNextEntry(new ZipEntry(entryName));
			InputStream entryInputStream = new BoundedInputStream(
					new FileInputStream(file));
			IOUtils.copy(entryInputStream, out);
			entryInputStream.close();
		}
		out.close();
	}

	@AfterClass
	public static void removeTest2Jar() throws IOException {
		FileUtils.deleteDirectory(libFolder);
	}

	@Test
	public void checkTest1() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(
				new FileContext("src/test/resources/test1"));

		Set<PluginDescriptor> plugins = context.getPluginDescriptors();
		assertEquals(2, plugins.size());

		List<String> modules = new ArrayList<>();
		List<String> styles = new ArrayList<>();
		Map<String, String> paths = new HashMap<>();
		Map<String, String> shims = new HashMap<>();
		JSONObject defaultConf = new JSONObject();
		for (PluginDescriptor plugin : plugins) {
			modules.addAll(plugin.getModules());
			styles.addAll(plugin.getStylesheets());
			paths.putAll(plugin.getRequireJSPathsMap());
			shims.putAll(plugin.getRequireJSShims());
			defaultConf.putAll(plugin.getDefaultConf());
		}

		checkList(modules, "module1", "module2", "module3");
		checkList(styles, "styles/general.css", "modules/module2.css",
				"modules/module3.css", "styles/general2.css");
		checkMapKeys(paths, "jquery-ui", "fancy-box", "openlayers", "mustache");
		checkMapKeys(shims, "fancy-box", "mustache");
		JSONObject layout = defaultConf.getJSONObject("layout");
		JSONObject legend = defaultConf.getJSONObject("legend");
		JSONObject layerList = defaultConf.getJSONObject("layer-list");

		assertEquals("29px", layout.getString("banner-size"));
		assertEquals(true, legend.getBoolean("show-title"));
		assertEquals(14, layerList.getInt("top"));
	}

	@Test
	public void checkExpandedClient() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(
				new ExpandedClientContext("src/test/resources/test2"));

		PluginDescriptor plugin = context.getPluginDescriptors().iterator()
				.next();
		checkList(plugin.getModules(), "module3");
		checkList(plugin.getStylesheets(), "modules/module3.css",
				"styles/general2.css");
		checkMapKeys(plugin.getRequireJSPathsMap(), "mustache");
		checkMapKeys(plugin.getRequireJSShims(), "mustache");
	}

	@Test
	public void checkCustomPluginConfDir() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(
				new FileContext("src/test/resources/test3"), "conf", "webapp");

		PluginDescriptor plugin = context.getPluginDescriptors().iterator()
				.next();
		checkMapKeys(plugin.getRequireJSPathsMap(), "jquery-ui", "fancy-box",
				"openlayers");
		checkMapKeys(plugin.getRequireJSShims(), "fancy-box");
	}

	@Test
	public void checkCustomWebResourcesDir() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(
				new FileContext("src/test/resources/test3"), "conf", "webapp");

		PluginDescriptor plugin = context.getPluginDescriptors().iterator()
				.next();
		checkList(plugin.getModules(), "module1", "module2");
		checkList(plugin.getStylesheets(), "styles/general.css",
				"modules/module2.css");
	}

	@Test
	public void checkThemePath() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(
				new FileContext("src/test/resources/test_theme"));

		PluginDescriptor plugin = context.getPluginDescriptors().iterator()
				.next();
		checkList(plugin.getStylesheets(), "styles/general.css",
				"theme/theme.css");
	}

	@Test
	public void checkPluginDescriptors() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(
				new FileContext("src/test/resources/test1"));

		Set<PluginDescriptor> plugins = context.getPluginDescriptors();
		assertEquals(2, plugins.size());

		for (PluginDescriptor plugin : plugins) {
			Set<String> modules = plugin.getModules();
			Set<String> styles = plugin.getStylesheets();
			JSONObject defaultConf = plugin.getDefaultConf();
			Map<String, String> requirejsPaths = plugin.getRequireJSPathsMap();
			Map<String, String> requirejsShims = plugin.getRequireJSShims();
			if ("test1".equals(plugin.getName())) {
				assertEquals(2, modules.size());
				assertTrue(modules.contains("module1"));
				assertTrue(modules.contains("module2"));
				assertEquals(2, styles.size());
				assertTrue(styles.contains("modules/module2.css"));
				assertTrue(styles.contains("styles/general.css"));
				assertTrue(defaultConf.containsKey("layout"));
				assertTrue(defaultConf.containsKey("legend"));
				assertFalse(defaultConf.containsKey("layer-list"));
				assertTrue(requirejsPaths.containsKey("fancy-box"));
				assertTrue(requirejsPaths.containsKey("jquery-ui"));
				assertTrue(requirejsPaths.containsKey("openlayers"));
				assertFalse(requirejsPaths.containsKey("mustache"));
				assertTrue(requirejsShims.containsKey("fancy-box"));
				assertFalse(requirejsShims.containsKey("mustache"));
			} else if ("test2".equals(plugin.getName())) {
				assertEquals(1, modules.size());
				assertTrue(modules.contains("module3"));
				assertEquals(2, styles.size());
				assertTrue(styles.contains("modules/module3.css"));
				assertTrue(styles.contains("styles/general2.css"));
				assertFalse(defaultConf.containsKey("layout"));
				assertFalse(defaultConf.containsKey("legend"));
				assertTrue(defaultConf.containsKey("layer-list"));
				assertFalse(requirejsPaths.containsKey("fancy-box"));
				assertFalse(requirejsPaths.containsKey("jquery-ui"));
				assertFalse(requirejsPaths.containsKey("openlayers"));
				assertTrue(requirejsPaths.containsKey("mustache"));
				assertFalse(requirejsShims.containsKey("fancy-box"));
				assertTrue(requirejsShims.containsKey("mustache"));
			} else {
				fail();
			}
		}
	}

	private void checkList(Collection<String> result, String... testEntries) {
		for (String entry : testEntries) {
			assertTrue(entry, result.remove(entry));
		}

		assertTrue(result.size() == 0);
	}

	private void checkMapKeys(Map<String, ?> result, String... testKeys) {
		for (String entry : testKeys) {
			assertTrue(result.remove(entry) != null);
		}

		assertTrue(result.size() == 0);
	}
}
