package org.fao.unredd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.commons.io.FileUtils;
import org.fao.unredd.jwebclientAnalyzer.Context;
import org.fao.unredd.jwebclientAnalyzer.JEEContextAnalyzer;
import org.fao.unredd.portal.Config;
import org.fao.unredd.portal.DefaultConfProvider;
import org.fao.unredd.portal.DefaultConfig;
import org.fao.unredd.portal.PluginJSONConfigurationProvider;
import org.fao.unredd.portal.PublicConfProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class AppContextListenerTest {
	private AppContextListener listener;
	private File confDir;
	private ServletContext context;

	@Before
	public void setup() throws IOException {
		this.listener = spy(new AppContextListener());
		File root = File.createTempFile("geoladris_conf_dir", "");
		root.delete();
		this.confDir = new File(root, "WEB-INF/default_config");
		this.confDir.mkdirs();
		this.context = mock(ServletContext.class);
		when(this.context.getRealPath("/")).thenReturn(root.getAbsolutePath());

		doReturn(mock(JEEContextAnalyzer.class)).when(this.listener)
				.getAnalyzer(any(Context.class));
	}

	@After
	public void teardown() throws IOException {
		FileUtils.deleteDirectory(this.confDir);
	}

	@Test
	public void addsConfig() {
		this.listener.contextInitialized(context);
		verify(context).setAttribute(eq(AppContextListener.ATTR_CONFIG),
				any(Config.class));
	}

	@Test
	public void testPublicConf() throws IOException {
		File publicConf = new File(this.confDir, PublicConfProvider.FILE);
		publicConf.createNewFile();

		ArgumentCaptor<Config> captor = ArgumentCaptor.forClass(Config.class);

		this.listener.contextInitialized(context);
		verify(context).setAttribute(eq(AppContextListener.ATTR_CONFIG),
				captor.capture());

		DefaultConfig config = (DefaultConfig) captor.getValue();
		assertTrue(config
				.hasModuleConfigurationProvider(PublicConfProvider.class));
		assertTrue(config
				.hasModuleConfigurationProvider(DefaultConfProvider.class));
		assertFalse(config.hasModuleConfigurationProvider(
				PluginJSONConfigurationProvider.class));
	}

	@Test
	public void testPluginConf() {
		ArgumentCaptor<Config> captor = ArgumentCaptor.forClass(Config.class);

		this.listener.contextInitialized(context);
		verify(context).setAttribute(eq(AppContextListener.ATTR_CONFIG),
				captor.capture());

		DefaultConfig config = (DefaultConfig) captor.getValue();
		assertFalse(config
				.hasModuleConfigurationProvider(PublicConfProvider.class));
		assertTrue(config
				.hasModuleConfigurationProvider(DefaultConfProvider.class));
		assertTrue(config.hasModuleConfigurationProvider(
				PluginJSONConfigurationProvider.class));
	}
}
