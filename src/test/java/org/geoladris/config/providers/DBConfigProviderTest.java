package org.geoladris.config.providers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.geoladris.Geoladris;
import org.geoladris.config.Config;
import org.geoladris.config.DBDataSource;
import org.junit.Test;

import net.sf.json.JSONObject;

public class DBConfigProviderTest {
  private PreparedStatement st;

  @Test
  public void getConfigDefault() throws Exception {
    String config = "{'a' : { 'm1' : true}}";
    String app = "test";

    DBDataSource ds = mockDataSource(true, config);
    DBConfigProvider provider = new DBConfigProvider(ds, app);

    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(Geoladris.ATTR_ROLE)).thenReturn(null);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession()).thenReturn(session);

    Map<String, JSONObject> pluginConfig =
        provider.getPluginConfig(mock(Config.class), new HashMap<String, JSONObject>(), request);
    assertTrue(pluginConfig.get("a").getBoolean("m1"));
    verify(this.st, atLeastOnce()).setString(1, app);
    verify(this.st, atLeastOnce()).setString(2, DBConfigProvider.DEFAULT_ROLE);
  }

  @Test
  public void getConfigSpecificRole() throws Exception {
    String config = "{'a' : { 'm1' : true}}";
    String app = "test";
    String role = "role1";

    DBConfigProvider provider = new DBConfigProvider(mockDataSource(true, config), app);

    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(Geoladris.ATTR_ROLE)).thenReturn(role);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession()).thenReturn(session);

    Map<String, JSONObject> pluginConfig =
        provider.getPluginConfig(mock(Config.class), new HashMap<String, JSONObject>(), request);
    assertTrue(pluginConfig.get("a").getBoolean("m1"));
    verify(this.st, atLeastOnce()).setString(1, app);
    verify(this.st).setString(2, role);
  }

  @Test
  public void getConfigInvalidRole() throws Exception {
    String app = "test";
    String role = "role1";

    ResultSet result = mock(ResultSet.class);
    when(result.next()).thenReturn(false);
    when(result.getString(1)).thenReturn(null);

    this.st = mock(PreparedStatement.class);
    when(st.executeQuery()).thenReturn(result);

    Connection conn = mock(Connection.class);
    when(conn.prepareStatement(String.format(DBConfigProvider.SQL, ""))).thenReturn(st);

    DBDataSource ds = mock(DBDataSource.class);
    when(ds.getConnection()).thenReturn(conn);

    DBConfigProvider provider = new DBConfigProvider(ds, app);

    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(Geoladris.ATTR_ROLE)).thenReturn(role);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession()).thenReturn(session);

    Map<String, JSONObject> pluginConfig =
        provider.getPluginConfig(mock(Config.class), new HashMap<String, JSONObject>(), request);
    assertNull(pluginConfig);
  }

  @Test
  public void hasApp() throws Exception {
    DBDataSource ds = mockDataSource(true, "{}");
    DBConfigProvider provider = new DBConfigProvider(ds, "test");
    assertTrue(provider.hasApp("test"));
  }

  @Test
  public void doesNotHaveApp() throws Exception {
    DBDataSource ds = mockDataSource(false, "{}");
    DBConfigProvider provider = new DBConfigProvider(ds, "test");
    assertFalse(provider.hasApp("test"));
  }

  @Test
  public void providesConfig() throws Exception {
    DBDataSource ds = mockDataSource(true, "{}");
    DBConfigProvider provider = new DBConfigProvider(ds, "test");

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession()).thenReturn(mock(HttpSession.class));

    provider.getPluginConfig(mock(Config.class), new HashMap<String, JSONObject>(), request);
    verify(this.st, atLeastOnce()).setString(1, "test");
  }

  @Test
  public void nullSession() throws Exception {
    DBDataSource ds = mockDataSource(true, "{'a' : { 'm1' : true}}");
    DBConfigProvider provider = new DBConfigProvider(ds, "test");

    Map<String, JSONObject> pluginConfig = provider.getPluginConfig(mock(Config.class),
        new HashMap<String, JSONObject>(), mock(HttpServletRequest.class));
    assertTrue(pluginConfig.get("a").getBoolean("m1"));
  }

  private DBDataSource mockDataSource(boolean hasNext, String conf) throws SQLException {
    ResultSet result = mock(ResultSet.class);
    when(result.next()).thenReturn(hasNext);
    when(result.getString(1)).thenReturn(conf);

    this.st = mock(PreparedStatement.class);
    when(st.executeQuery()).thenReturn(result);

    Connection conn = mock(Connection.class);
    when(conn.prepareStatement(String.format(DBConfigProvider.SQL, ""))).thenReturn(st);

    DBDataSource ds = mock(DBDataSource.class);
    when(ds.getConnection()).thenReturn(conn);
    return ds;
  }
}
