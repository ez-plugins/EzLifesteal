package com.skyblockexp.ezlifesteal.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsParsedYamlWhenFileExists() throws IOException {
        Path dataFolder = tempDir.resolve("plugin-data");
        Files.createDirectories(dataFolder);
        Path configFile = dataFolder.resolve("existing.yml");
        Files.writeString(configFile, "section:\n  enabled: true\nname: test\n");

        Logger logger = mock(Logger.class);
        JavaPlugin plugin = mockPlugin(dataFolder.toFile(), logger);
        ConfigLoader loader = new ConfigLoader(plugin);

        YamlConfiguration configuration = loader.load("existing.yml");

        assertTrue(configuration.getBoolean("section.enabled"));
        assertEquals("test", configuration.getString("name"));
        verify(plugin, never()).saveResource(any(), eq(false));
    }

    @Test
    void loadAttemptsSaveResourceAndReturnsEmptyConfigurationWhenFileIsStillMissing() {
        Path dataFolder = tempDir.resolve("missing-load");
        Logger logger = mock(Logger.class);
        JavaPlugin plugin = mockPlugin(dataFolder.toFile(), logger);
        ConfigLoader loader = new ConfigLoader(plugin);

        YamlConfiguration configuration = loader.load("missing.yml");

        assertTrue(configuration.getKeys(true).isEmpty());
        verify(plugin).saveResource("missing.yml", false);
        verify(logger).warning(contains("Failed to load missing.yml because the file could not be created."));
    }

    @Test
    void ensureResourcesCallsCreationForEachEntryWithoutThrowing() {
        Path dataFolder = tempDir.resolve("ensure-resources");
        Logger logger = mock(Logger.class);
        JavaPlugin plugin = mockPlugin(dataFolder.toFile(), logger);
        ConfigLoader loader = new ConfigLoader(plugin);

        assertDoesNotThrow(() -> loader.ensureResources(List.of("a.yml", "nested/b.yml", "nested/c.yml")));

        verify(plugin).saveResource("a.yml", false);
        verify(plugin).saveResource("nested/b.yml", false);
        verify(plugin).saveResource("nested/c.yml", false);
    }

    @Test
    void saveIsNoOpForNullConfiguration() {
        Path dataFolder = tempDir.resolve("save-null");
        Logger logger = mock(Logger.class);
        JavaPlugin plugin = mockPlugin(dataFolder.toFile(), logger);
        ConfigLoader loader = new ConfigLoader(plugin);

        assertDoesNotThrow(() -> loader.save(null, "ignored.yml"));

        verify(logger, never()).severe(any(String.class));
    }

    @Test
    void saveLogsSevereWhenIOExceptionOccurs() throws IOException {
        Path dataFolder = tempDir.resolve("save-io");
        Logger logger = mock(Logger.class);
        JavaPlugin plugin = mockPlugin(dataFolder.toFile(), logger);
        ConfigLoader loader = new ConfigLoader(plugin);
        YamlConfiguration configuration = mock(YamlConfiguration.class);
        doThrow(new IOException("disk full")).when(configuration).save(any(File.class));

        loader.save(configuration, "output.yml");

        verify(logger).severe(contains("Failed to save output.yml: disk full"));
    }

    @Test
    void ensureResourceIfMissingLogsWarningWhenDataFolderCreationFails() throws Exception {
        Path blockerFile = tempDir.resolve("blocker-file");
        Files.writeString(blockerFile, "not-a-directory");
        File impossibleDataFolder = blockerFile.resolve("plugin-data").toFile();

        Logger logger = mock(Logger.class);
        JavaPlugin plugin = mockPlugin(impossibleDataFolder, logger);
        ConfigLoader loader = new ConfigLoader(plugin);

        invokeEnsureResourceIfMissing(loader, "config.yml");

        verify(logger).warning(contains("Unable to create plugin data directory to save config.yml"));
        verify(plugin, never()).saveResource(any(), eq(false));
    }

    @Test
    void ensureResourceIfMissingLogsWarningWhenParentFolderCreationFails() throws Exception {
        Path dataFolder = tempDir.resolve("data");
        Files.createDirectories(dataFolder);
        Path blockingFile = dataFolder.resolve("blocked");
        Files.writeString(blockingFile, "not-a-directory");

        Logger logger = mock(Logger.class);
        JavaPlugin plugin = mockPlugin(dataFolder.toFile(), logger);
        ConfigLoader loader = new ConfigLoader(plugin);

        invokeEnsureResourceIfMissing(loader, "blocked/nested/config.yml");

        verify(logger).warning(contains("Unable to create directory"));
        verify(logger).warning(contains("to save blocked/nested/config.yml"));
        verify(plugin, never()).saveResource(any(), eq(false));
    }

    @Test
    void ensureResourceIfMissingLogsWarningWhenJarResourceIsMissing() throws Exception {
        Path dataFolder = tempDir.resolve("jar-missing");
        Logger logger = mock(Logger.class);
        JavaPlugin plugin = mockPlugin(dataFolder.toFile(), logger);
        doThrow(new IllegalArgumentException("missing")).when(plugin).saveResource("unknown.yml", false);
        ConfigLoader loader = new ConfigLoader(plugin);

        invokeEnsureResourceIfMissing(loader, "unknown.yml");

        verify(logger).warning(contains("Default resource 'unknown.yml' is missing from the plugin jar."));
    }

    private JavaPlugin mockPlugin(File dataFolder, Logger logger) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(logger);
        doNothing().when(plugin).saveResource(any(), eq(false));
        return plugin;
    }

    private void invokeEnsureResourceIfMissing(ConfigLoader loader, String resource) throws Exception {
        Method method = ConfigLoader.class.getDeclaredMethod("ensureResourceIfMissing", String.class);
        method.setAccessible(true);
        method.invoke(loader, resource);
    }
}
