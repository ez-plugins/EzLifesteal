package com.skyblockexp.ezlifesteal.storage.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySqlStorageSupport {

    private final String jdbcUrl;

    private final Properties properties;

    public MySqlStorageSupport(String host, int port, String database, String username, String password,
            boolean useSsl) {
        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
        this.properties = new Properties();
        this.properties.setProperty("user", username);
        this.properties.setProperty("password", password);
        this.properties.setProperty("useSSL", Boolean.toString(useSsl));
        this.properties.setProperty("autoReconnect", "true");
    }

    public Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, properties);
    }
}
