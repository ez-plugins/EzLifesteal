package com.skyblockexp.ezlifesteal.storage.yaml;

import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlStorageProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void createWithValidConfigInitializesAndExposesRepositories() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("data-root", tempDir.toFile().getAbsolutePath());
        config.set("legacy-file", "legacy-storage.yml");

        YamlStorageProvider provider = createFromConfig(config, tempDir.toFile());

        assertNotNull(provider.profiles());
        assertNotNull(provider.bans());
        assertInstanceOf(YamlProfileRepository.class, provider.profiles());
        assertInstanceOf(YamlBanRepository.class, provider.bans());

        assertDoesNotThrow(provider::init);
        assertTrue(tempDir.resolve("players").toFile().isDirectory());
        assertTrue(tempDir.resolve("bans").toFile().isDirectory());
    }

    @Test
    void missingKeysFallBackToDefaultsAndInvalidRootThrowsPredictableException() throws IOException {
        YamlConfiguration missingKeys = new YamlConfiguration();

        YamlStorageProvider withFallbacks = createFromConfig(missingKeys, tempDir.toFile());
        assertDoesNotThrow(withFallbacks::init);
        assertTrue(tempDir.resolve("players").toFile().isDirectory());

        File notADirectory = tempDir.resolve("storage-root-file").toFile();
        assertTrue(notADirectory.createNewFile());
        YamlConfiguration invalid = new YamlConfiguration();
        invalid.set("data-root", notADirectory.getAbsolutePath());

        YamlStorageProvider invalidProvider = createFromConfig(invalid, tempDir.toFile());
        StorageException exception = assertThrows(StorageException.class, invalidProvider::init);
        assertTrue(exception.getMessage().contains("Unable to create players directory"));
    }

    @Test
    void repeatedInitAndCloseCallsAreSafe() {
        YamlStorageProvider provider = createFromConfig(new YamlConfiguration(), tempDir.toFile());

        assertDoesNotThrow(provider::init);
        assertDoesNotThrow(provider::init);
        assertDoesNotThrow(provider::close);
        assertDoesNotThrow(provider::close);
    }

    private static YamlStorageProvider createFromConfig(YamlConfiguration config, File defaultDataRoot) {
        String configuredRoot = config.getString("data-root");
        File dataRoot = (configuredRoot == null || configuredRoot.isBlank())
                ? defaultDataRoot
                : new File(configuredRoot);
        String legacyFile = config.getString("legacy-file", "lifesteal-storage.yml");
        return new YamlStorageProvider(dataRoot, legacyFile);
    }
}
