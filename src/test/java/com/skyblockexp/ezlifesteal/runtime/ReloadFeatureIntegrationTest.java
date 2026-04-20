package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.command.MessageCapturingSender;
import java.io.File;
import java.io.IOException;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadFeatureIntegrationTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void lifestealReloadCommandConsumesUpdatedLanguageMessages() throws IOException {
        EzLifestealPlugin plugin = MockBukkit.load(EzLifestealPlugin.class);
        File languageFile = new File(plugin.getDataFolder(), "languages/en.yml");
        YamlConfiguration languageConfig = YamlConfiguration.loadConfiguration(languageFile);
        languageConfig.set("prefix", "");
        languageConfig.set("reloaded", "Reloaded from feature test.");
        languageConfig.save(languageFile);

        PluginCommand lifestealCommand = plugin.getCommand("lifesteal");
        assertNotNull(lifestealCommand);

        MessageCapturingSender sender = new MessageCapturingSender();
        boolean executed = lifestealCommand.execute(sender.getProxy(), "lifesteal", new String[]{"reload"});

        assertTrue(executed);
        assertTrue(sender.getMessages().stream().anyMatch(message -> message.contains("Reloaded from feature test.")));
    }

    @Test
    void runtimeReloadConsumesUpdatedGameplayValuesForLiveAccessors() throws IOException {
        EzLifestealPlugin plugin = MockBukkit.load(EzLifestealPlugin.class);
        PluginAccessor accessor = plugin.getPluginAccessor();
        assertNotNull(accessor);

        File lifestealCoreFile = new File(plugin.getDataFolder(), "lifesteal-core.yml");
        YamlConfiguration lifestealCore = YamlConfiguration.loadConfiguration(lifestealCoreFile);
        lifestealCore.set("hearts-per-kill", 3.25);
        lifestealCore.save(lifestealCoreFile);

        plugin.reloadPlugin(null);

        assertEquals(3.25, accessor.getHeartsPerKill("world"));
    }
}
