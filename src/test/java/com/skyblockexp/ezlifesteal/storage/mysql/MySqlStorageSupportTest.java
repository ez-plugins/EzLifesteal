package com.skyblockexp.ezlifesteal.storage.mysql;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

class MySqlStorageSupportTest {

    @Test
    void constructorBuildsJdbcUrlUsingHostPortAndDatabaseExactly() throws Exception {
        MySqlStorageSupport support = new MySqlStorageSupport("db.example.net", 3307, "lifesteal_db", "user", "secret",
                true);

        String jdbcUrl = (String) readPrivateField(support, "jdbcUrl");

        assertEquals("jdbc:mysql://db.example.net:3307/lifesteal_db", jdbcUrl);
    }

    @Test
    void constructorBuildsPropertiesWithRequiredEntries() throws Exception {
        MySqlStorageSupport support = new MySqlStorageSupport("localhost", 3306, "ezlifesteal", "admin", "p@ss", false);

        Properties properties = (Properties) readPrivateField(support, "properties");

        assertEquals("admin", properties.getProperty("user"));
        assertEquals("p@ss", properties.getProperty("password"));
        assertEquals("false", properties.getProperty("useSSL"));
        assertEquals("true", properties.getProperty("autoReconnect"));
        assertTrue(properties.containsKey("user"));
        assertTrue(properties.containsKey("password"));
        assertTrue(properties.containsKey("useSSL"));
        assertTrue(properties.containsKey("autoReconnect"));
    }

    @Test
    void connectionDelegatesToDriverManagerWithJdbcUrlAndProperties() throws Exception {
        MySqlStorageSupport support = new MySqlStorageSupport("mysql.internal", 3308, "players", "root", "pw", true);
        Connection expectedConnection = mock(Connection.class);

        String expectedJdbcUrl = (String) readPrivateField(support, "jdbcUrl");
        Properties expectedProperties = (Properties) readPrivateField(support, "properties");

        try (MockedStatic<DriverManager> driverManager = Mockito.mockStatic(DriverManager.class)) {
            driverManager.when(() -> DriverManager.getConnection(anyString(), any(Properties.class)))
                    .thenReturn(expectedConnection);

            Connection actualConnection = support.connection();

            assertSame(expectedConnection, actualConnection);
            driverManager.verify(() -> DriverManager.getConnection(eq(expectedJdbcUrl), eq(expectedProperties)));
        }
    }

    private static Object readPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
