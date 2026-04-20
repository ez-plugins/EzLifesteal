package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.lang.reflect.Field;
import java.util.logging.Logger;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPluginRuntimeServicesStorageTest {

    @Test
    void setupStorageDefaultsToYamlWhenTypeMissingAndPersistsConfiguration() throws Exception {
        Logger logger = mock(Logger.class);
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getDataFolder()).thenReturn(new java.io.File("build/tmp/runtime-storage-tests/default-type"));

        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());
        YamlConfiguration storageConfig = new YamlConfiguration();
        setField(services, "storageConfig", storageConfig);

        try (MockedStatic<Bukkit> bukkit = mockBukkitBanApi()) {
            services.setupStorage();
        }

        assertEquals("YAML", storageConfig.getString("type"));
        verify(plugin).saveConfig();
        verify(logger).warning(contains("No storage type configured; defaulting to YAML storage."));
        String summary = (String) getField(services, "storageSummary");
        assertTrue(summary.startsWith("YAML storage ("));
    }

    @Test
    void setupStorageFallsBackToYamlForUnknownTypeAndLogsWarning() throws Exception {
        Logger logger = mock(Logger.class);
        DefaultPluginRuntimeServices services = runtimeWithStorageConfig(logger, "YAML");

        setField(services, "storageConfig", yamlWithType("NotAType"));
        try (MockedStatic<Bukkit> bukkit = mockBukkitBanApi()) {
            services.setupStorage();
        }

        String fallbackSummary = (String) getField(services, "storageSummary");
        assertEquals("YAML storage (fallback for unknown type 'NotAType')", fallbackSummary);
        verify(logger).warning(contains("Unknown storage type 'NotAType'"));
    }

    @Test
    void setupStorageBuildsMysqlSummaryAndLogsWhenInitializationFails() throws Exception {
        Logger logger = mock(Logger.class);
        DefaultPluginRuntimeServices services = runtimeWithStorageConfig(logger, "MYSQL");
        YamlConfiguration storageConfig = yamlWithType("MYSQL");
        storageConfig.set("mysql.host", "db.example");
        storageConfig.set("mysql.port", 3307);
        storageConfig.set("mysql.database", "lifesteal");
        storageConfig.set("mysql.username", "rooter");
        storageConfig.set("mysql.password", "secret");
        storageConfig.set("mysql.table", "profiles");
        storageConfig.set("mysql.use-ssl", true);
        setField(services, "storageConfig", storageConfig);

        try (MockedStatic<Bukkit> bukkit = mockBukkitBanApi()) {
            services.setupStorage();
        }

        String summary = (String) getField(services, "storageSummary");
        assertTrue(summary.contains("MySQL storage (db.example:3307/lifesteal, user=rooter, table=profiles, SSL=on)"));
        verify(logger).severe(contains("Failed to initialise storage"));
    }

    @Test
    void setupStorageLogsWhenClosingExistingStorageFailsAndDoesNotLeavePartialStateOnProviderFailure()
            throws Exception {
        Logger logger = mock(Logger.class);
        DefaultPluginRuntimeServices services = runtimeWithStorageConfig(logger, "MYSQL");

        com.skyblockexp.ezlifesteal.storage.Storage existing = mock(com.skyblockexp.ezlifesteal.storage.Storage.class);
        doThrow(new StorageException("boom-close")).when(existing).close();
        setField(services, "storage", existing);

        YamlConfiguration mysqlConfig = yamlWithType("MYSQL");
        mysqlConfig.set("mysql.host", "localhost");
        setField(services, "storageConfig", mysqlConfig);

        try (MockedStatic<Bukkit> bukkit = mockBukkitBanApi()) {
            services.setupStorage();
        }

        verify(logger).warning(contains("Failed to close existing storage: boom-close"));
        verify(logger).severe(contains("Failed to initialise storage"));
        assertNull(getField(services, "profileRepository"));
        assertNull(getField(services, "banRepository"));
    }

    private DefaultPluginRuntimeServices runtimeWithStorageConfig(Logger logger, String type) throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        when(plugin.getDataFolder()).thenReturn(new java.io.File("build/tmp/runtime-storage-tests"));

        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());
        setField(services, "storageConfig", yamlWithType(type));
        return services;
    }

    private static YamlConfiguration yamlWithType(String type) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("type", type);
        return config;
    }

    private static MockedStatic<Bukkit> mockBukkitBanApi() {
        BanList banList = mock(BanList.class);
        when(banList.getBanEntries()).thenReturn(java.util.Set.of());
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS);
        bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);
        return bukkit;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
