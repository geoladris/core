package org.geoladris.config;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.geoladris.Environment;
import org.geoladris.Geoladris;
import org.postgresql.Driver;

import net.sf.json.JSONObject;

public class DBConfigurationProvider implements ModuleConfigurationProvider {
  private static final Logger logger = Logger.getLogger(DBConfigurationProvider.class);

  static final String SQL = "SELECT conf FROM %sapps WHERE app = ? AND role LIKE ?";

  static final String DEFAULT_ROLE = "default";

  static {
    try {
      Class.forName(Driver.class.getCanonicalName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private DBDataSource dataSource;
  private String contextPath, schema;

  public DBConfigurationProvider(DBDataSource dataSource, String contextPath) {
    this.dataSource = dataSource;
    this.contextPath = contextPath;
    this.schema = Environment.getInstance().get(Environment.JDBC_SCHEMA);
    if (this.schema == null) {
      this.schema = "";
    }
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
      conn = this.dataSource.getConnection();
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

  public boolean hasApp(String app) {
    try {
      return getConfig(app, DEFAULT_ROLE) != null;
    } catch (IOException e) {
      logger.error("Cannot obtain configuration from database for app: " + app, e);
      return false;
    }
  }
}
