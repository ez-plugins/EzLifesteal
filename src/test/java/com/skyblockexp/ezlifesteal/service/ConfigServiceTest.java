package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.ConfigLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigServiceTest {

    @Test
    void normalizeLanguageHandlesNullBlankAndRegionVariants() {
        ConfigService service = new ConfigService(mock(EzLifestealPlugin.class), mock(ConfigLoader.class));

        assertEquals("en", service.normalizeLanguage(null));
        assertEquals("en", service.normalizeLanguage("   "));
        assertEquals("nl", service.normalizeLanguage("NL_nl"));
        assertEquals("fr", service.normalizeLanguage(" fr-CA "));
    }

    @Test
    void loadLanguageFileReturnsNullWhenFileRemainsMissing(@TempDir Path tempDir) {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        ConfigLoader loader = mock(ConfigLoader.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());

        ConfigService service = new ConfigService(plugin, loader);

        YamlConfiguration loaded = service.loadLanguageFile("de");

        assertNull(loaded);
        verify(loader).ensureResources(eq(List.of("languages/de.yml")));
    }

    @Test
    void loadLanguageFileLoadsExistingYamlFile(@TempDir Path tempDir) throws IOException {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        ConfigLoader loader = mock(ConfigLoader.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());

        File languageDir = tempDir.resolve("languages").toFile();
        org.junit.jupiter.api.Assertions.assertTrue(languageDir.mkdirs());
        File languageFile = new File(languageDir, "en.yml");
        YamlConfiguration expected = new YamlConfiguration();
        expected.set("messages.prefix", "[EZ]");
        expected.save(languageFile);

        ConfigService service = new ConfigService(plugin, loader);

        YamlConfiguration loaded = service.loadLanguageFile("en");

        assertNotNull(loaded);
        assertEquals("[EZ]", loaded.getString("messages.prefix"));
    }

    @Test
    void ensureAdditionalConfigFilesRequestsExpectedResourceSets() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        ConfigLoader loader = mock(ConfigLoader.class);
        ConfigService service = new ConfigService(plugin, loader);

        service.ensureAdditionalConfigFiles();

        verify(loader).ensureResources(eq(List.of(
                "admin.yml",
                "smurf.yml",
                "storage.yml",
                "lifesteal-core.yml",
                "lifesteal-drops.yml",
                "lifesteal-worlds.yml",
                "lifesteal-mobs.yml",
                "lifesteal-killstreaks.yml",
                "hearts.yml",
                "shop.yml",
                "features.yml",
                "revive-beacon.yml",
                "revive-beacon-whitelist.yml"
        )));
        verify(loader).ensureResources(eq(List.of(
                "languages/en.yml",
                "languages/nl.yml",
                "languages/es.yml",
                "languages/fr.yml",
                "languages/de.yml",
                "languages/pt.yml"
        )));
    }
}
