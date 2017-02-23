package org.geoladris.servlet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import javax.naming.NamingException;

import org.geoladris.Geoladris;
import org.geoladris.TestingServletContext;
import org.geoladris.config.DBConfigurationProvider;
import org.geoladris.config.ModuleConfigurationProvider;
import org.geoladris.config.PluginJSONConfigurationProvider;
import org.geoladris.config.PublicConfProvider;
import org.junit.Before;
import org.junit.Test;

public class AppContextListenerTest {
  private AppContextListener listener;
  private TestingServletContext context;

  @Before
  public void setup() throws IOException {
    this.listener = spy(new AppContextListener());
    this.context = new TestingServletContext();
  }

  @Test
  public void exceptionObtainingDBProvider() throws Exception {
    this.context.setContextPath("test");
    doThrow(NamingException.class).when(this.listener).getDBProvider(anyString());

    this.listener.contextInitialized(this.context.event);

    assertTrue(hasProvider(PublicConfProvider.class));
    assertTrue(hasProvider(PluginJSONConfigurationProvider.class));
    assertFalse(hasProvider(DBConfigurationProvider.class));
  }

  @Test
  public void disabledDBProvider() throws Exception {
    this.context.setContextPath("test");
    DBConfigurationProvider dbProvider = mock(DBConfigurationProvider.class);
    when(this.listener.isDBEnabled()).thenReturn(false);
    doReturn(dbProvider).when(this.listener).getDBProvider(anyString());

    this.listener.contextInitialized(this.context.event);

    assertTrue(hasProvider(PublicConfProvider.class));
    assertTrue(hasProvider(PluginJSONConfigurationProvider.class));
    assertFalse(hasProvider(DBConfigurationProvider.class));
  }

  @Test
  public void enabledDBProvider() throws Exception {
    this.context.setContextPath("test");
    DBConfigurationProvider dbProvider = mock(DBConfigurationProvider.class);
    when(this.listener.isDBEnabled()).thenReturn(true);
    doReturn(dbProvider).when(this.listener).getDBProvider(anyString());

    this.listener.contextInitialized(this.context.event);

    assertFalse(hasProvider(PublicConfProvider.class));
    assertFalse(hasProvider(PluginJSONConfigurationProvider.class));
    assertTrue(hasProvider(DBConfigurationProvider.class));
  }

  @Test
  public void trailingSlashInContextPath() throws Exception {
    this.context.setContextPath("/test");
    DBConfigurationProvider dbProvider = mock(DBConfigurationProvider.class);
    when(this.listener.isDBEnabled()).thenReturn(true);
    doReturn(dbProvider).when(this.listener).getDBProvider(anyString());

    this.listener.contextInitialized(this.context.event);

    verify(this.listener).getDBProvider("test");
  }

  @SuppressWarnings("unchecked")
  private boolean hasProvider(Class<? extends ModuleConfigurationProvider> c) {
    List<ModuleConfigurationProvider> providers =
        (List<ModuleConfigurationProvider>) this.context.servletContext
            .getAttribute(Geoladris.ATTR_CONFIG_PROVIDERS);
    for (ModuleConfigurationProvider provider : providers) {
      if (c.isInstance(provider)) {
        return true;
      }
    }

    return false;
  }

}
