package org.geoladris.config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.log4j.Logger;
import org.geoladris.Environment;
import org.geoladris.Geoladris;
import org.postgresql.Driver;

import net.sf.json.JSONObject;

public class DBConfigurationProvider implements ModuleConfigurationProvider {
  private static final Logger logger = Logger.getLogger(DBConfigurationProvider.class);

  public static final String CONTEXT_RESOURCE_NAME = "geoladris";

  static final String SQL = "SELECT conf FROM %sapps WHERE app = ? AND role LIKE ?";

  static final String DEFAULT_ROLE = "default";

  static {
    try {
      Class.forName(Driver.class.getCanonicalName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private DataSource dataSource;
  private InitialContext context;
  private String contextPath, schema;

  public DBConfigurationProvider(String contextPath, InitialContext context) {
    this.contextPath = contextPath;
    this.context = context;
  }

  private DataSource getDataSource() throws IOException {
    if (this.dataSource == null) {
      try {
        // First check context.xml
        this.dataSource =
            (DataSource) context.lookup("java:/comp/env/jdbc/" + CONTEXT_RESOURCE_NAME);
      } catch (NamingException | ClassCastException e) {
        // If not, get from environment variables
        Environment env = Environment.getInstance();
        String url = env.get(Environment.JDBC_URL);
        String user = env.get(Environment.JDBC_USER);
        String pass = env.get(Environment.JDBC_PASS);
        this.schema = env.get(Environment.JDBC_SCHEMA);

        if (url != null && user != null && pass != null) {
          this.dataSource = createDataSource(url, user, pass);
        } else {
          throw new IOException("Cannot obtain default configuration for context '"
              + this.contextPath + "' from database");
        }
      }

      if (this.schema == null) {
        this.schema = "";
      }

      String conf = getConfig(this.contextPath, DEFAULT_ROLE);
      if (conf == null) {
        throw new IOException(
            "Cannot obtain default configuration for app '" + this.contextPath + "' from database");
      }

    }

    return this.dataSource;
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

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, JSONObject> getPluginConfig(PortalRequestConfiguration configurationContext,
      HttpServletRequest request) throws IOException {
    HttpSession session = request.getSession();
    String role;
    if (session == null) {
      role = DEFAULT_ROLE;
    } else {
      Object roleAttr = session.getAttribute(Geoladris.ATTR_ROLE);
      role = roleAttr != null ? roleAttr.toString() : DEFAULT_ROLE;
    }

    Object appAttr = request.getAttribute(Geoladris.ATTR_APP);
    String app = appAttr != null ? appAttr.toString() : null;

    String qualifiedApp = this.contextPath;
    if (app != null && app.length() > 0) {
      qualifiedApp += "/" + app;
    }

    String config = getConfig(qualifiedApp, role);
    return config != null ? JSONObject.fromObject(config) : null;
  }

  private String getConfig(String app, String role) throws IOException {
    Connection conn = null;
    try {
      conn = getDataSource().getConnection();
      PreparedStatement st = conn.prepareStatement(String.format(SQL, this.schema));
      st.setString(1, app);
      st.setString(2, role);

      ResultSet result = st.executeQuery();
      if (result.next()) {
        return result.getString(1);
      } else {
        logger.error("Cannot find configuration for app '" + app + "' and role '" + role
            + "' in the database");
        return null;
      }
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException e) {
          logger.error("Cannot close connection", e);
        }
      }
    }
  }

  @Override
  public boolean canBeCached() {
    return false;
  }

  public boolean isEnabled() {
    try {
      return getDataSource() != null;
    } catch (IOException e) {
      logger.info(e);
      return false;
    }
  }

  public boolean hasApp(String app) {
    try {
      return getConfig(app, DEFAULT_ROLE) != null;
    } catch (IOException e) {
      logger.error("Cannot obtain configuration from database for app: " + app, e);
      return false;
    }
  }
}
