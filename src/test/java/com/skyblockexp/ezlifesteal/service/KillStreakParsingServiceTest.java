package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakSettings;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KillStreakParsingServiceTest {

    @BeforeAll
    public static void beforeAll() {
        MockBukkitTestHelper.startServer();
    }

    @AfterAll
    public static void afterAll() {
        MockBukkitTestHelper.stopServer();
    }

    @Test
    public void parseRewardItemString_parsesMaterialAndAmount() {
        DefaultPluginRuntimeServices services = Mockito.mock(DefaultPluginRuntimeServices.class);
        KillStreakParsingService parser = new KillStreakParsingService(services);

        ItemStack stack = parser.parseRewardItemString("DIAMOND:3", "key");
        assertNotNull(stack);
        assertEquals(Material.DIAMOND, stack.getType());
        assertEquals(3, stack.getAmount());
    }

    @Test
    public void parseKillStreakSettings_parsesRewardsSection() {
        DefaultPluginRuntimeServices services = Mockito.mock(DefaultPluginRuntimeServices.class);
        LifestealConfigAdapter adapter = Mockito.mock(LifestealConfigAdapter.class);
        Mockito.when(services.getLifestealConfigAdapter()).thenReturn(adapter);

        YamlConfiguration root = new YamlConfiguration();
        ConfigurationSection ks = root.createSection("kill-streaks");
        ks.set("enabled", true);
        ks.set("reset-on-death", false);
        ConfigurationSection rewards = ks.createSection("rewards");
        ConfigurationSection r5 = rewards.createSection("5");
        r5.set("streak", 5);
        r5.set("money", 10.0);
        r5.set("items", List.of("DIAMOND:2", "STONE 4"));

        Mockito.when(adapter.getSection("kill-streaks")).thenReturn(ks);

        KillStreakParsingService parser = new KillStreakParsingService(services);
        KillStreakSettings settings = parser.parseKillStreakSettings();
        assertTrue(settings.enabled());
        assertFalse(settings.resetOnDeath());
        assertTrue(settings.configuredStreaks().contains(5));
        List<?> rewardsList = settings.rewardsFor(5);
        assertEquals(1, rewardsList.size());
        Object first = rewardsList.get(0);
        assertNotNull(first);
        // ensure the parsed object is a KillStreakReward
        assertTrue(first instanceof com.skyblockexp.ezlifesteal.killstreak.KillStreakReward);
    }
}
