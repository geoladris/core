package org.geoladris.config;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geoladris.Environment;
import org.geoladris.PluginDescriptor;

public class DBConfig extends AbstractConfig {
  private static final Logger logger = Logger.getLogger(DBConfig.class);

  static final String SQL_PROPS = "SELECT key, value FROM %sprops WHERE app = ?";
  static final String SQL_MESSAGES = "SELECT key, value FROM %smessages WHERE app = ? AND lang = ?";

  private String app;
  private String schema;

  public DBConfig(String app, File configDir, List<ModuleConfigurationProvider> configProviders,
      Set<PluginDescriptor> plugins, boolean useCache, int cacheTimeout) throws ConfigException {
    super(configDir, configProviders, plugins, useCache, cacheTimeout);

    this.app = app;
    this.schema = Environment.getInstance().get(Environment.JDBC_SCHEMA);
    if (this.schema == null) {
      this.schema = "";
    } else {
      this.schema += ".";
    }
  }

  @Override
  protected ResourceBundle getResourceBundle(Locale locale) {
    try {
      Map<String, String> values = readFromDatabase(SQL_MESSAGES, locale.getLanguage());
      if (values.size() == 0) {
        values = readFromDatabase(SQL_MESSAGES, "");
      }
      return new MapResourceBundle(values);
    } catch (SQLException e) {
      throw new ConfigException("Cannot obtain messages from database", e);
    }
  }

  @Override
  protected Properties readProperties() {
    Properties properties = new Properties();
    try {
      properties.putAll(readFromDatabase(SQL_PROPS));
    } catch (SQLException e) {
      logger.error("Cannot obtain properties from database", e);
    }
    return properties;
  }

  private Map<String, String> readFromDatabase(String sql) throws SQLException {
    return readFromDatabase(sql, null);
  }

  private Map<String, String> readFromDatabase(String sql, String param) throws SQLException {
    Connection conn = null;
    try {
      conn = getConnection();

      PreparedStatement st = conn.prepareStatement(String.format(sql, this.schema));
      st.setString(1, this.app);
      if (param != null) {
        st.setString(2, param);
      }

      Map<String, String> ret = new HashMap<>();
      ResultSet result = st.executeQuery();
      while (result.next()) {
        ret.put(result.getString(1), result.getString(2));
      }
      return ret;
    } finally {
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException e) {
          logger.fatal("Cannot close connection", e);
        }
      }
    }
  }

  /**
   * For testing purposes
   */
  Connection getConnection() throws SQLException {
    return DBDataSource.getInstance().getConnection();
  }

  private class MapResourceBundle extends ResourceBundle {
    private Map<String, String> map;

    public MapResourceBundle(Map<String, String> map) {
      this.map = map;
    }

    @Override
    protected Object handleGetObject(String key) {
      return map.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
      return Collections.enumeration(map.keySet());
    }
  }
}
