package org.fao.unredd.jwebclientAnalyzer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.input.BoundedInputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JEEContextAnalyzerTest {
	private static File test1LibFolder = new File(
			"src/test/resources/test1/WEB-INF/lib");
	private static File testOnlyLibFolder = new File(
			"src/test/resources/testOnlyLib/WEB-INF/lib");

	@BeforeClass
	public static void packAsJar() throws IOException {
		packageAsJar("test2", ".", test1LibFolder);

		packageAsJar("test2", ".", testOnlyLibFolder);
		packageAsJar("testJavaNonRootModules", "WEB-INF/classes",
				testOnlyLibFolder);
	}

	private static void packageAsJar(String testCaseToPack,
			String pluginContentsRoot, File jslib)
			throws FileNotFoundException, IOException {
		assertTrue(jslib.exists() || jslib.mkdirs());

		File jarFile = new File(jslib, testCaseToPack + ".jar");
		assertTrue(!jarFile.exists() || jarFile.delete());

		FileOutputStream stream = new FileOutputStream(jarFile);
		File jarContentRoot = new File("src/test/resources/", testCaseToPack
				+ File.separator + pluginContentsRoot);
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
		FileUtils.deleteDirectory(test1LibFolder);
		FileUtils.deleteDirectory(testOnlyLibFolder.getParentFile());
	}

	@Test
	public void checkTest1() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/test1"));

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
			defaultConf.putAll(plugin.getConfiguration());
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
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/test3"), "conf", "webapp");

		PluginDescriptor plugin = context.getPluginDescriptors().iterator()
				.next();
		checkMapKeys(plugin.getRequireJSPathsMap(), "jquery-ui", "fancy-box",
				"openlayers");
		checkMapKeys(plugin.getRequireJSShims(), "fancy-box");
	}

	@Test
	public void checkCustomWebResourcesDir() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/test3"), "conf", "webapp");

		PluginDescriptor plugin = context.getPluginDescriptors().iterator()
				.next();
		checkList(plugin.getModules(), "module1", "module2");
		checkList(plugin.getStylesheets(), "styles/general.css",
				"modules/module2.css");
	}

	@Test
	public void scanNoJavaPlugins() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/testNoJava"), "nfms", "nfms");
		Set<PluginDescriptor> plugins = context.getPluginDescriptors();
		assertEquals(3, plugins.size());
		for (PluginDescriptor plugin : plugins) {
			if (plugin.getName().equals("plugin1")) {
				checkList(plugin.getModules(), //
						"plugin1/a", //
						"plugin1/b");
				checkList(plugin.getStylesheets(), //
						"styles/plugin1/a.css", //
						"modules/plugin1/d.css");
				Map<String, String> path = plugin.getRequireJSPathsMap();
				assertEquals("../jslib/plugin1/lib-a", path.get("lib-a"));
				assertEquals("../jslib/plugin1/lib-b", path.get("lib-b"));
				assertEquals(2, path.size());
				Map<String, String> shims = plugin.getRequireJSShims();
				assertEquals("[\"lib-a\"]", shims.get("lib-b"));
				assertEquals(1, shims.size());
			} else if (plugin.getName().equals("plugin2")) {
				checkList(plugin.getModules(), "plugin2/c");
				checkList(plugin.getStylesheets(), //
						"styles/plugin2/b.css", //
						"styles/plugin2/c.css");
				Map<String, String> paths = plugin.getRequireJSPathsMap();
				assertEquals("../jslib/plugin2/lib-c", paths.get("lib-c"));
				assertEquals(1, paths.size());
				Map<String, String> shim = plugin.getRequireJSShims();
				assertEquals("[\"lib-a\",\"lib-b\"]", shim.get("lib-c"));
				assertEquals(1, shim.size());
			} else if (plugin.getName().equals("javaplugin")) {
				checkList(plugin.getModules(), "javaplugin/module-java");
				checkList(plugin.getStylesheets(), //
						"styles/javaplugin/style-java.css", //
						"modules/javaplugin/module-style-java.css");
				Map<String, String> paths = plugin.getRequireJSPathsMap();
				assertEquals("../jslib/javaplugin/lib-java1",
						paths.get("lib-java1"));
				assertEquals("../jslib/javaplugin/lib-java2",
						paths.get("lib-java2"));
				assertEquals(2, paths.size());
				Map<String, String> shim = plugin.getRequireJSShims();
				assertEquals("[\"lib-a\",\"lib-b\",\"lib-c\"]",
						shim.get("lib-java"));
				assertEquals(1, shim.size());
			}
		}
	}

	@Test
	public void scanJavaNonRootModules() {
		String name = "testJavaNonRootModules";
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/" + name), "nfms", "nfms");
		PluginDescriptor plugin = context.getPluginDescriptors().iterator()
				.next();

		checkList(plugin.getModules(), name + "/module1");
		checkList(plugin.getStylesheets(), "styles/" + name + "/style1.css",
				"modules/" + name + "/module1.css");
		Map<String, String> nonRequirePaths = plugin.getRequireJSPathsMap();
		assertEquals("../jslib/" + name + "/lib", nonRequirePaths.get("lib"));
		assertEquals(1, nonRequirePaths.size());
	}

	@Test
	public void scanJavaNonRootModulesAsJar() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/testOnlyLib"), "nfms", "nfms");

		Set<PluginDescriptor> plugins = context.getPluginDescriptors();
		assertEquals(2, plugins.size());

		List<String> modules = new ArrayList<>();
		List<String> styles = new ArrayList<>();
		Map<String, String> paths = new HashMap<>();
		for (PluginDescriptor plugin : plugins) {
			modules.addAll(plugin.getModules());
			styles.addAll(plugin.getStylesheets());
			paths.putAll(plugin.getRequireJSPathsMap());
		}

		checkList(modules, //
				"testJavaNonRootModules/module1", //
				"module3"//
		);
		checkList(styles, //
				"styles/testJavaNonRootModules/style1.css", //
				"modules/testJavaNonRootModules/module1.css", //
				"styles/general2.css", //
				"modules/module3.css"//
		);
		assertEquals("../jslib/testJavaNonRootModules/lib", paths.get("lib"));
		assertEquals("../jslib/jquery.mustache", paths.get("mustache"));
		assertEquals(2, paths.size());
	}

	public void checkThemePath() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/test_theme"));

		PluginDescriptor plugin = context.getPluginDescriptors().iterator()
				.next();
		checkList(plugin.getStylesheets(), "styles/general.css",
				"theme/theme.css");
	}

	@Test
	public void checkPluginDescriptors() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/test1"));

		Set<PluginDescriptor> plugins = context.getPluginDescriptors();
		assertEquals(2, plugins.size());

		for (PluginDescriptor plugin : plugins) {
			Set<String> modules = plugin.getModules();
			Set<String> styles = plugin.getStylesheets();
			JSONObject defaultConf = plugin.getConfiguration();
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
			assertTrue(entry + " not in " + result, result.contains(entry));
		}

		assertEquals(result.size(), testEntries.length);
	}

	private void checkMapKeys(Map<String, ?> result, String... testKeys) {
		for (String entry : testKeys) {
			assertTrue(result.remove(entry) != null);
		}

		assertTrue(result.size() == 0);
	}
}
