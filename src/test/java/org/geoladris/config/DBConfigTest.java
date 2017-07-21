package org.geoladris.config;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.geoladris.Plugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.stubbing.OngoingStubbing;

public class DBConfigTest {
  private static final String APP = "app";

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private DBConfig config;
  private Connection conn;
  private PreparedStatement st;
  private ResultSet result;

  @Before
  public void setup() throws Exception {
    config = spy(new DBConfig(APP, folder.getRoot(), new ArrayList<PluginConfigProvider>(),
        new HashSet<Plugin>(), false, -1));

    this.result = mock(ResultSet.class);

    this.st = mock(PreparedStatement.class);
    when(this.st.executeQuery()).thenReturn(this.result);

    this.conn = mock(Connection.class);
    when(this.conn.prepareStatement(anyString())).thenReturn(this.st);
    doReturn(this.conn).when(this.config).getConnection();
  }

  @Test
  public void sqlExceptionOnReadProperties() throws Exception {
    when(this.result.next()).thenThrow(new SQLException());
    Properties props = config.getProperties();
    assertEquals(0, props.size());
  }

  @Test
  public void readPropertiesSuccess() throws Exception {
    mockResult(new String[] {"a", "b"}, new String[] {"1", "2"});
    Properties props = config.getProperties();
    assertEquals(2, props.size());
    assertEquals("1", props.getProperty("a"));
    assertEquals("2", props.getProperty("b"));
  }

  @Test
  public void sqlExceptionOnGetResourceBundle() throws Exception {
    when(this.result.next()).thenThrow(new SQLException());
    try {
      config.getResourceBundle(Locale.getDefault());
      fail();
    } catch (ConfigException e) {
    }
  }

  @Test
  public void getResourceBundleSuccess() throws Exception {
    mockResult(new String[] {"a", "b"}, new String[] {"1", "2"});
    ResourceBundle bundle = config.getMessages(Locale.getDefault());
    assertEquals(2, bundle.keySet().size());
    assertEquals("1", bundle.getString("a"));
    assertEquals("2", bundle.getString("b"));
  }

  @Test
  public void fallbackToDefaultLangOnEmptyResourceBundle() throws Exception {
    when(result.next()).thenReturn(false).thenReturn(true).thenReturn(false);
    when(result.getString(1)).thenReturn("a");
    when(result.getString(2)).thenReturn("1");

    ResourceBundle bundle = config.getMessages(Locale.getDefault());

    assertEquals(1, bundle.keySet().size());
    assertEquals("1", bundle.getString("a"));
    verify(this.st, times(2)).setString(1, APP);
    verify(this.st).setString(2, Locale.getDefault().getLanguage());
    verify(this.st).setString(2, "");
  }

  @Test
  public void closesConnection() throws Exception {
    mockResult(new String[] {"a", "b"}, new String[] {"1", "2"});
    config.getMessages(Locale.getDefault());
    verify(this.conn).close();
  }

  private void mockResult(String[] keys, String values[]) throws SQLException {
    if (keys.length == 0) {
      return;
    }

    OngoingStubbing<Boolean> next = when(result.next()).thenReturn(true);
    for (int i = 1; i < keys.length; i++) {
      next = next.thenReturn(true);
    }
    next.thenReturn(false);

    OngoingStubbing<String> f1 = when(result.getString(1)).thenReturn(keys[0]);
    for (int i = 1; i < keys.length; i++) {
      f1 = f1.thenReturn(keys[i]);
    }
    OngoingStubbing<String> f2 = when(result.getString(2)).thenReturn(values[0]);
    for (int i = 1; i < values.length; i++) {
      f2 = f2.thenReturn(values[i]);
    }
  }
}
