package org.geoladris.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;
import org.geoladris.Environment;
import org.postgresql.Driver;

public class DBDataSource {
  private static final Logger logger = Logger.getLogger(DBConfig.class);

  public static final String CONTEXT_RESOURCE_NAME = "geoladris";

  private static final DBDataSource instance = new DBDataSource();

  static {
    try {
      Class.forName(Driver.class.getCanonicalName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static DBDataSource getInstance() {
    return instance;
  }

  private DBDataSource() {}

  private InitialContext context;
  private DataSource dataSource;

  public Connection getConnection() throws SQLException {
    if (this.dataSource == null) {
      try {
        // First check context.xml
        this.dataSource =
            (DataSource) getContext().lookup("java:/comp/env/jdbc/" + CONTEXT_RESOURCE_NAME);
      } catch (NamingException | ClassCastException e) {
        // If not, get from environment variables
        Environment env = Environment.getInstance();
        String url = env.get(Environment.JDBC_URL);
        String user = env.get(Environment.JDBC_USER);
        String pass = env.get(Environment.JDBC_PASS);

        if (url != null && user != null && pass != null) {
          this.dataSource = createDataSource(url, user, pass);
        } else {
          throw new SQLException("Cannot obtain default configuration from database");
        }
      }
    }

    return this.dataSource.getConnection();
  }

  /**
   * Just for testing purposes
   */
  BasicDataSource createDataSource(String url, String user, String pass) {
    BasicDataSource ds = new BasicDataSource();
    ds.setUrl(url);
    ds.setUsername(user);
    ds.setPassword(pass);
    return ds;
  }

  /**
   * Package visibility for testing purposes
   */
  InitialContext getContext() throws NamingException {
    if (this.context == null) {
      this.context = new InitialContext();
    }
    return this.context;
  }

  /**
   * Just for testing purposes.
   */
  void setInitialContext(InitialContext context) {
    this.context = context;
  }

  public boolean isEnabled() {
    try {
      return getConnection() != null;
    } catch (SQLException e) {
      logger.info(e);
      return false;
    }
  }
}
