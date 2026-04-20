package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.runtime.state.GameplayState;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPluginRuntimeServicesConfigTest {

    @Test
    void worldOverrideSummaryReportsEmptyWhenNoOverrides() {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();

        assertEquals("No world-specific lifesteal overrides are configured.", services.getWorldOverrideSummary());
    }

    @Test
    void worldOverrideSummaryIncludesNonEmptyOverrides() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        setField(services, "heartsPerKillOverrides", Map.of("world", 1.5));
        setField(services, "heartsLostOnDeathOverrides", Map.of("world_nether", 2.0));
        setField(services, "banWhenZeroOverrides", Map.of("world", true));

        String summary = services.getWorldOverrideSummary();

        assertTrue(summary.startsWith("World overrides loaded (2): "));
        assertTrue(summary.contains("world: kill=1.50, ban=on"));
        assertTrue(summary.contains("world_nether: death=2"));
    }

    @Test
    void worldOverrideSummaryIgnoresNullAndBlankWorldNames() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();

        Map<String, Double> killOverrides = new HashMap<>();
        killOverrides.put("world", 1.25);
        killOverrides.put("", 3.0);
        killOverrides.put(null, 9.0);
        setField(services, "heartsPerKillOverrides", killOverrides);
        setField(services, "heartsLostOnDeathOverrides", Map.of());
        setField(services, "banWhenZeroOverrides", Map.of("world", true));

        String summary = services.getWorldOverrideSummary();

        assertTrue(summary.contains("world: kill=1.25, ban=on"));
        assertFalse(summary.contains("null"));
        assertFalse(summary.contains(": kill=3"));
    }

    @Test
    void parseWorldOverridesSkipsBlankKeysAndNormalizesMixedCaseWorldNames() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        YamlConfiguration lifesteal = new YamlConfiguration();
        lifesteal.set("world-overrides.World.hearts-per-kill", 1.75);
        lifesteal.set("world-overrides.World.ban-when-zero", true);
        lifesteal.createSection("world-overrides.   ");
        setField(services, "lifestealConfigAdapter", new LifestealConfigAdapter(lifesteal, new YamlConfiguration()));

        invokePrivateMethod(services, "parseWorldOverrides");

        assertEquals(1.75, services.getHeartsPerKill("world"));
        assertTrue(services.isBanWhenZeroHearts("WORLD"));
        assertFalse(services.getWorldOverrideSummary().contains("   "));
    }

    @Test
    void worldSpecificHeartsValuesFallbackFromOverrideToGlobalDefaults() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        GameplayState gameplayState = (GameplayState) getField(services, "gameplayState");
        gameplayState.getHeartRulesState().setHeartsPerKill(0.5);
        gameplayState.getHeartRulesState().setHeartsLostOnDeath(2.5);

        YamlConfiguration lifesteal = new YamlConfiguration();
        lifesteal.set("world-overrides.World.hearts-per-kill", 1.75);
        lifesteal.set("world-overrides.World.hearts-lost-on-death", 3.0);
        setField(services, "lifestealConfigAdapter", new LifestealConfigAdapter(lifesteal, new YamlConfiguration()));

        invokePrivateMethod(services, "parseWorldOverrides");

        assertEquals(1.75, services.getHeartsPerKill("world"));
        assertEquals(1.75, services.getHeartsPerKill("WORLD"));
        assertEquals(0.5, services.getHeartsPerKill("world_nether"));
        assertEquals(0.5, services.getHeartsPerKill(null));
        assertEquals(0.5, services.getHeartsPerKill("   "));

        assertEquals(3.0, services.getHeartsLostOnDeath("world"));
        assertEquals(3.0, services.getHeartsLostOnDeath("WoRlD"));
        assertEquals(2.5, services.getHeartsLostOnDeath("world_the_end"));
        assertEquals(2.5, services.getHeartsLostOnDeath(null));
        assertEquals(2.5, services.getHeartsLostOnDeath(" "));
    }

    @Test
    void worldSpecificBanSettingFallsBackToGlobalDefaultForUnknownNullAndBlankWorlds() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        GameplayState gameplayState = (GameplayState) getField(services, "gameplayState");
        gameplayState.getHeartRulesState().setBanWhenZeroHearts(false);

        YamlConfiguration lifesteal = new YamlConfiguration();
        lifesteal.set("world-overrides.World.ban-when-zero", true);
        setField(services, "lifestealConfigAdapter", new LifestealConfigAdapter(lifesteal, new YamlConfiguration()));

        invokePrivateMethod(services, "parseWorldOverrides");

        assertTrue(services.isBanWhenZeroHearts("world"));
        assertTrue(services.isBanWhenZeroHearts("WORLD"));
        assertFalse(services.isBanWhenZeroHearts("world_nether"));
        assertFalse(services.isBanWhenZeroHearts(null));
        assertFalse(services.isBanWhenZeroHearts("   "));
    }

    @Test
    void parseMobRewardsHandlesInvalidEntityBlankPermissionAndMalformedHeartValues() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        YamlConfiguration lifesteal = new YamlConfiguration();
        lifesteal.set("mob-rewards.ZOMBIE.hearts", "not-a-number");
        lifesteal.set("mob-rewards.ZOMBIE.permission", "   ");
        lifesteal.set("mob-rewards.INVALID_ENTITY.hearts", 2.0);
        setField(services, "lifestealConfigAdapter", new LifestealConfigAdapter(lifesteal, new YamlConfiguration()));

        invokePrivateMethod(services, "parseMobRewards");

        MobReward zombieReward = services.getMobReward(EntityType.ZOMBIE);
        assertNotNull(zombieReward);
        assertEquals(0.0, zombieReward.getHearts());
        assertNull(zombieReward.getPermission());
        assertNull(services.getMobReward(EntityType.CREEPER));
    }

    @Test
    void parseRewardItemMapHandlesInvalidMaterialAmountAndBlankMetadata() throws Exception {
        MockBukkit.mock();
        try {
            DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
            Method parseRewardItemMap = DefaultPluginRuntimeServices.class.getDeclaredMethod("parseRewardItemMap",
                    Map.class, String.class);
            parseRewardItemMap.setAccessible(true);

            Object invalidMaterialResult = parseRewardItemMap.invoke(services, Map.of("material", "NOT_A_REAL_ITEM"),
                    "10");
            assertNull(invalidMaterialResult);

            Map<String, Object> itemConfig = new HashMap<>();
            itemConfig.put("material", "DIAMOND");
            itemConfig.put("amount", "abc");
            itemConfig.put("name", "   ");
            itemConfig.put("lore", List.of("   ", "", " \t"));

            ItemStack parsed = (ItemStack) parseRewardItemMap.invoke(services, itemConfig, "10");

            assertNotNull(parsed);
            assertEquals(Material.DIAMOND, parsed.getType());
            assertEquals(1, parsed.getAmount());
            ItemMeta meta = parsed.getItemMeta();
            assertNotNull(meta);
            assertTrue(meta.getDisplayName() == null || meta.getDisplayName().isBlank());
            assertTrue(meta.getLore() == null || meta.getLore().isEmpty());
        }
        finally {
            MockBukkit.unmock();
        }
    }

    @Test
    void worldRestrictionHelperHandlesEnabledDisabledAndEmptySets() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        Method method = DefaultPluginRuntimeServices.class.getDeclaredMethod("describeWorldRestrictions");
        method.setAccessible(true);

        assertEquals(
                "World restrictions: none (all worlds inherit the global setting).",
                method.invoke(services)
        );

        setField(services, "enabledWorlds", Set.of("world", "world_nether"));
        assertEquals(
                "World restrictions: enabled only in world, world_nether.",
                method.invoke(services)
        );

        setField(services, "enabledWorlds", Set.of());
        setField(services, "disabledWorlds", Set.of("world_the_end"));
        assertEquals(
                "World restrictions: disabled in world_the_end.",
                method.invoke(services)
        );
    }

    private DefaultPluginRuntimeServices runtimeWithBasicPlugin() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());
        try {
            setField(services, "gameplayParsingService",
                    new com.skyblockexp.ezlifesteal.service.GameplayParsingService(services));
            setField(services, "killStreakParsingService",
                    new com.skyblockexp.ezlifesteal.service.KillStreakParsingService(services));
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to prepare test runtime services", exception);
        }
        return services;
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

    private static Object invokePrivateMethod(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
