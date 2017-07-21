package org.geoladris.config.providers;

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
import org.geoladris.config.Config;
import org.geoladris.config.DBDataSource;
import org.geoladris.config.PluginConfigProvider;

import net.sf.json.JSONObject;

public class DBConfigProvider implements PluginConfigProvider {
  private static final Logger logger = Logger.getLogger(DBConfigProvider.class);

  static final String SQL = "SELECT conf FROM %sapps WHERE app = ? AND role LIKE ?";

  static final String DEFAULT_ROLE = "default";

  private DBDataSource dataSource;
  private String contextPath, schema;

  public DBConfigProvider(DBDataSource dataSource, String contextPath) {
    this.dataSource = dataSource;
    this.contextPath = contextPath;
    this.schema = Environment.getInstance().get(Environment.JDBC_SCHEMA);
    if (this.schema == null) {
      this.schema = "";
    } else {
      this.schema += ".";
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, JSONObject> getPluginConfig(Config config,
      Map<String, JSONObject> currentConfig, HttpServletRequest request) throws IOException {
    HttpSession session = request.getSession();
    String role;
    if (session == null) {
      role = DEFAULT_ROLE;
    } else {
      Object roleAttr = session.getAttribute(Geoladris.ATTR_ROLE);
      role = roleAttr != null ? roleAttr.toString() : DEFAULT_ROLE;
    }

    String ret = getConfig(this.contextPath, role);
    return ret != null ? JSONObject.fromObject(ret) : null;
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
