package org.geoladris.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.dbcp2.BasicDataSource;
import org.geoladris.Environment;
import org.junit.Before;
import org.junit.Test;

public class DBDataSourceTest {
  private static final String RESOURCE_NAME =
      "java:/comp/env/jdbc/" + DBDataSource.CONTEXT_RESOURCE_NAME;

  private DBDataSource ds;
  private InitialContext context;

  @Before
  public void setup() throws Exception {
    this.ds = spy(DBDataSource.getInstance());
    this.context = mock(InitialContext.class);
    doReturn(context).when(ds).getContext();
  }

  @Test
  public void noInitialContextNorEnv() throws Exception {
    when(context.lookup(RESOURCE_NAME)).thenThrow(mock(NamingException.class));
    assertFalse(ds.isEnabled());
  }

  @Test
  public void getFromInitialContext() throws Exception {
    doReturn(mock(Connection.class)).when(ds).getConnection();
    when(context.lookup(RESOURCE_NAME)).thenReturn(ds);
    assertTrue(ds.isEnabled());
  }

  @Test
  public void getFromEnv() throws Exception {
    when(context.lookup(anyString())).thenThrow(mock(NamingException.class));

    Properties previousProps = System.getProperties();
    Properties newProps = new Properties();
    newProps.putAll(previousProps);
    newProps.put(Environment.JDBC_URL, "jdbc:postgresql://localhost/db");
    newProps.put(Environment.JDBC_USER, "user");
    newProps.put(Environment.JDBC_PASS, "pass");
    System.setProperties(newProps);

    BasicDataSource basicDataSource = mock(BasicDataSource.class);
    when(basicDataSource.getConnection()).thenReturn(mock(Connection.class));
    doReturn(basicDataSource).when(ds).createDataSource(anyString(), anyString(), anyString());
    assertTrue(ds.isEnabled());
    System.setProperties(previousProps);
  }
}
