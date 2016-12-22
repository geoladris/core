package org.geoladris.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.geoladris.Environment;
import org.geoladris.Geoladris;
import org.junit.Test;

import net.sf.json.JSONObject;

public class DBConfigurationProviderTest {
  private PreparedStatement st;

  @Test
  public void noInitialContextNorEnv() throws Exception {
    InitialContext context = mock(InitialContext.class);
    when(context.lookup("java:/comp/env/jdbc/" + DBConfigurationProvider.CONTEXT_RESOURCE_NAME))
        .thenThrow(mock(NamingException.class));

    DBConfigurationProvider provider = new DBConfigurationProvider("test", context);

    assertFalse(provider.isEnabled());
  }

  @Test
  public void getFromInitialContext() throws Exception {
    DataSource ds = mockDataSource(true, "{}");

    InitialContext context = mock(InitialContext.class);
    when(context.lookup("java:/comp/env/jdbc/" + DBConfigurationProvider.CONTEXT_RESOURCE_NAME))
        .thenReturn(ds);

    DBConfigurationProvider provider = new DBConfigurationProvider("test", context);

    assertTrue(provider.isEnabled());
  }

  @Test
  public void getFromEnv() throws Exception {
    InitialContext context = mock(InitialContext.class);
    when(context.lookup(anyString())).thenThrow(mock(NamingException.class));

    Properties previousProps = System.getProperties();
    Properties newProps = new Properties();
    newProps.putAll(previousProps);
    newProps.put(Environment.JDBC_URL, "jdbc:postgresql://localhost/db");
    newProps.put(Environment.JDBC_USER, "user");
    newProps.put(Environment.JDBC_PASS, "pass");
    System.setProperties(newProps);

    DBConfigurationProvider provider = spy(new DBConfigurationProvider("test", context));

    DataSource ds = mockDataSource(true, "{}");
    doReturn(ds).when(provider).createDataSource(anyString(), anyString(), anyString());

    assertTrue(provider.isEnabled());

    System.setProperties(previousProps);
  }

  @Test
  public void validConnectionWithMissingDefaultConf() throws Exception {
    DataSource ds = mockDataSource(false, null);
    InitialContext context = mock(InitialContext.class);
    when(context.lookup("java:/comp/env/jdbc/" + DBConfigurationProvider.CONTEXT_RESOURCE_NAME))
        .thenReturn(ds);

    DBConfigurationProvider provider = new DBConfigurationProvider("test", context);

    assertFalse(provider.isEnabled());
  }

  @Test
  public void getConfigDefault() throws Exception {
    String config = "{'a' : { 'm1' : true}}";
    String app = "test";

    DataSource ds = mockDataSource(true, config);

    InitialContext context = mock(InitialContext.class);
    when(context.lookup("java:/comp/env/jdbc/" + DBConfigurationProvider.CONTEXT_RESOURCE_NAME))
        .thenReturn(ds);

    DBConfigurationProvider provider = new DBConfigurationProvider(app, context);

    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(Geoladris.SESSION_ATTR_ROLE)).thenReturn(null);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession()).thenReturn(session);

    Map<String, JSONObject> pluginConfig =
        provider.getPluginConfig(mock(PortalRequestConfiguration.class), request);
    assertTrue(pluginConfig.get("a").getBoolean("m1"));
    verify(this.st, atLeastOnce()).setString(1, app);
    verify(this.st, atLeastOnce()).setString(2, DBConfigurationProvider.DEFAULT_ROLE);
  }

  @Test
  public void getConfigSpecificRole() throws Exception {
    String config = "{'a' : { 'm1' : true}}";
    String app = "test";
    String role = "role1";

    DataSource ds = mockDataSource(true, config);

    InitialContext context = mock(InitialContext.class);
    when(context.lookup("java:/comp/env/jdbc/" + DBConfigurationProvider.CONTEXT_RESOURCE_NAME))
        .thenReturn(ds);

    DBConfigurationProvider provider = new DBConfigurationProvider(app, context);

    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(Geoladris.SESSION_ATTR_ROLE)).thenReturn(role);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession()).thenReturn(session);

    Map<String, JSONObject> pluginConfig =
        provider.getPluginConfig(mock(PortalRequestConfiguration.class), request);
    assertTrue(pluginConfig.get("a").getBoolean("m1"));
    verify(this.st, atLeastOnce()).setString(1, app);
    verify(this.st).setString(2, role);
  }

  @Test
  public void getConfigInvalidRole() throws Exception {
    String config = "{'a' : { 'm1' : true}}";
    String app = "test";
    String role = "role1";

    ResultSet result = mock(ResultSet.class);
    when(result.next()).thenReturn(true).thenReturn(false);
    when(result.getString(1)).thenReturn(config).thenReturn(null);

    this.st = mock(PreparedStatement.class);
    when(st.executeQuery()).thenReturn(result);

    Connection conn = mock(Connection.class);
    when(conn.prepareStatement(String.format(DBConfigurationProvider.SQL, ""))).thenReturn(st);

    BasicDataSource ds = mock(BasicDataSource.class);
    when(ds.getConnection()).thenReturn(conn);

    InitialContext context = mock(InitialContext.class);
    when(context.lookup("java:/comp/env/jdbc/" + DBConfigurationProvider.CONTEXT_RESOURCE_NAME))
        .thenReturn(ds);

    DBConfigurationProvider provider = new DBConfigurationProvider(app, context);

    HttpSession session = mock(HttpSession.class);
    when(session.getAttribute(Geoladris.SESSION_ATTR_ROLE)).thenReturn(role);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession()).thenReturn(session);

    Map<String, JSONObject> pluginConfig =
        provider.getPluginConfig(mock(PortalRequestConfiguration.class), request);
    assertNull(pluginConfig);
  }

  private DataSource mockDataSource(boolean hasNext, String conf) throws SQLException {
    ResultSet result = mock(ResultSet.class);
    when(result.next()).thenReturn(hasNext);
    when(result.getString(1)).thenReturn(conf);

    this.st = mock(PreparedStatement.class);
    when(st.executeQuery()).thenReturn(result);

    Connection conn = mock(Connection.class);
    when(conn.prepareStatement(String.format(DBConfigurationProvider.SQL, ""))).thenReturn(st);

    BasicDataSource ds = mock(BasicDataSource.class);
    when(ds.getConnection()).thenReturn(conn);
    return ds;
  }
}
