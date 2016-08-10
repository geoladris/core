package org.fao.unredd;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import javax.servlet.ServletContext;

import org.fao.unredd.jwebclientAnalyzer.JEEContextAnalyzer;
import org.junit.Before;
import org.junit.Test;

public class AppContextListenerTest {
	private AppContextListener listener;

	@Before
	public void setup() {
		this.listener = new AppContextListener();
	}

	@Test
	public void addsMergeConfModules() {
		ServletContext context = mock(ServletContext.class);
		JEEContextAnalyzer analyzer = mock(JEEContextAnalyzer.class);
		Set<String> set = Collections.singleton("module");
		when(analyzer.getMergeConfModules()).thenReturn(set);

		this.listener.contextInitialized(context, analyzer);

		verify(context).setAttribute(AppContextListener.ATTR_MERGE_CONF_MODULES,
				set);
	}
}
