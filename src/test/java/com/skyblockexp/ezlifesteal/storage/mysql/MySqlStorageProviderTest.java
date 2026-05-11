package com.skyblockexp.ezlifesteal.storage.mysql;

import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlStorageProviderTest {

    @Test
    void createWithValidConfigInitializesAndExposesRepositories() throws SQLException {
        MySqlStorageProvider provider = createFromConfig(validConfig());

        assertNotNull(provider.profiles());
        assertNotNull(provider.bans());
        assertNotNull(provider.teamBanks());
        assertInstanceOf(MySqlProfileRepository.class, provider.profiles());
        assertInstanceOf(MySqlBanRepository.class, provider.bans());

        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);

        try (MockedStatic<DriverManager> driverManager = Mockito.mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(anyString(), any())).thenReturn(connection);

            assertDoesNotThrow(provider::init);

            verify(statement, times(4)).executeUpdate(anyString());
        }
    }

    @Test
    void missingKeysFallBackToDefaultsAndInvalidTableThrowsPredictableException() {
        YamlConfiguration missing = new YamlConfiguration();
        missing.set("host", "db.internal");
        MySqlStorageProvider withFallbacks = createFromConfig(missing);
        assertNotNull(withFallbacks);

        YamlConfiguration invalid = new YamlConfiguration();
        invalid.set("table", "  ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> createFromConfig(invalid));
        assertTrue(exception.getMessage().contains("table"));
    }

    @Test
    void repeatedInitAndCloseCallsAreSafe() throws SQLException {
        MySqlStorageProvider provider = createFromConfig(validConfig());
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);

        try (MockedStatic<DriverManager> driverManager = Mockito.mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(anyString(), any())).thenReturn(connection);

            assertDoesNotThrow(provider::init);
            assertDoesNotThrow(provider::init);
            assertDoesNotThrow(provider::close);
            assertDoesNotThrow(provider::close);
        }
    }

    @Test
    void initWrapsSqlExceptionAsStorageException() {
        MySqlStorageProvider provider = createFromConfig(validConfig());

        SQLException sqlException = new SQLException("connection refused");
        try (MockedStatic<DriverManager> driverManager = Mockito.mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(anyString(), any()))
                    .thenThrow(sqlException);

            StorageException exception = assertThrows(StorageException.class, provider::init);
            assertTrue(exception.getMessage().contains("Unable to initialise MySQL"));
        }
    }

    private static YamlConfiguration validConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("host", "localhost");
        config.set("port", 3306);
        config.set("database", "lifesteal");
        config.set("username", "root");
        config.set("password", "password");
        config.set("use-ssl", false);
        config.set("table", "lifesteal_players");
        return config;
    }

    private static MySqlStorageProvider createFromConfig(YamlConfiguration config) {
        String host = config.getString("host", "localhost");
        int port = config.getInt("port", 3306);
        String database = config.getString("database", "lifesteal");
        String username = config.getString("username", "root");
        String password = config.getString("password", "password");
        boolean useSsl = config.getBoolean("use-ssl", false);
        String table = config.getString("table", "lifesteal_players");
        if (table == null || table.isBlank()) {
            throw new IllegalArgumentException("table must not be blank");
        }
        return new MySqlStorageProvider(host, port, database, username, password, useSsl, table);
    }
}
