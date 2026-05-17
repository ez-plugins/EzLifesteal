package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.ConfigLoader;
import com.skyblockexp.ezlifesteal.service.StorageService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class StorageModeSelectionFeatureTest {

    @TempDir
    Path tempDir;

    @Test
    void yamlStorageModeWiresStorageAndRepositoryBackends() throws Exception {
        EzLifestealPlugin plugin = pluginBackedByTempFolder();
        Files.writeString(tempDir.resolve("storage.yml"), "type: YAML\n");

        StorageService service = new StorageService(plugin, new Registry(), new ConfigLoader(plugin));
        try (MockedStatic<Bukkit> bukkit = mockBukkitBanApi()) {
            service.setupStorage();
        }

        assertNotNull(service.getStorage());
        assertNotNull(service.getProfileRepository());
        assertNotNull(service.getBanRepository());
        assertTrue(service.getStorageSummary().startsWith("YAML storage ("));
    }

    @Test
    void mysqlModeWithoutMysqlSectionFallsBackToYamlRepositoryWiring() throws Exception {
        EzLifestealPlugin plugin = pluginBackedByTempFolder();
        Files.writeString(tempDir.resolve("storage.yml"), "type: MYSQL\n");

        StorageService service = new StorageService(plugin, new Registry(), new ConfigLoader(plugin));
        try (MockedStatic<Bukkit> bukkit = mockBukkitBanApi()) {
            service.setupStorage();
        }

        assertNotNull(service.getStorage());
        assertNotNull(service.getProfileRepository());
        assertNotNull(service.getBanRepository());
        assertTrue(service.getStorageSummary().startsWith("YAML storage ("));
    }

    private EzLifestealPlugin pluginBackedByTempFolder() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        return plugin;
    }

    private static MockedStatic<Bukkit> mockBukkitBanApi() {
        BanList banList = mock(BanList.class);
        when(banList.getBanEntries()).thenReturn(java.util.Set.of());
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS);
        bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(banList);
        return bukkit;
    }
}
