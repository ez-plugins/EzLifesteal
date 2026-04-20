package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.util.WorldNameUtil;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

public final class GameplayParsingService {

    private final DefaultPluginRuntimeServices runtimeServices;

    public GameplayParsingService(DefaultPluginRuntimeServices runtimeServices) {
        this.runtimeServices = runtimeServices;
    }

    public WorldOverrides parseWorldOverrides() {
        final ConfigurationSection overridesSection =
                runtimeServices.getLifestealConfigAdapter().getSection("world-overrides");
        if (overridesSection == null) {
            return new WorldOverrides(Map.of(), Map.of(), Map.of());
        }

        final Map<String, Double> killOverrides = new HashMap<>();
        final Map<String, Double> deathOverrides = new HashMap<>();
        final Map<String, Boolean> banOverrides = new HashMap<>();
        for (String worldKey : overridesSection.getKeys(false)) {
            if (worldKey == null || worldKey.isBlank()) {
                continue;
            }
            final ConfigurationSection worldSection = overridesSection.getConfigurationSection(worldKey);
            if (worldSection == null) {
                runtimeServices.getLogger().warning(
                        "Skipping lifesteal.world-overrides entry '" + worldKey + "' because it is not a section."
                );
                continue;
            }
            final String normalized = runtimeServices.normalizeWorldName(worldKey);
            if (worldSection.contains("hearts-per-kill")) {
                killOverrides.put(normalized, worldSection.getDouble("hearts-per-kill"));
            }
            if (worldSection.contains("hearts-lost-on-death")) {
                deathOverrides.put(normalized, worldSection.getDouble("hearts-lost-on-death"));
            }
            if (worldSection.contains("ban-when-zero")) {
                banOverrides.put(normalized, worldSection.getBoolean("ban-when-zero"));
            }
        }
        return new WorldOverrides(
                killOverrides.isEmpty() ? Map.of() : Map.copyOf(killOverrides),
                deathOverrides.isEmpty() ? Map.of() : Map.copyOf(deathOverrides),
                banOverrides.isEmpty() ? Map.of() : Map.copyOf(banOverrides)
        );
    }

    public Map<EntityType, MobReward> parseMobRewards() {
        final ConfigurationSection rewardsSection =
                runtimeServices.getLifestealConfigAdapter().getSection("mob-rewards");
        if (rewardsSection == null) {
            return Map.of();
        }
        final Map<EntityType, MobReward> parsed = new EnumMap<>(EntityType.class);
        for (String entityKey : rewardsSection.getKeys(false)) {
            if (entityKey == null || entityKey.isBlank()) {
                continue;
            }
            final ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(entityKey);
            if (rewardSection == null) {
                runtimeServices.getLogger().warning(
                        "Skipping lifesteal.mob-rewards entry '" + entityKey + "' because it is not a section."
                );
                continue;
            }
            final EntityType entityType;
            try {
                entityType = EntityType.valueOf(entityKey.trim().toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException exception) {
                runtimeServices.getLogger().warning("Unknown entity type in lifesteal.mob-rewards: " + entityKey);
                continue;
            }
            final double heartsDelta = rewardSection.getDouble("hearts", 0.0);
            final Set<String> allowedWorlds =
                    new HashSet<>(WorldNameUtil.parseWorldList(rewardSection.getStringList("worlds")));
            final Set<String> blockedWorlds =
                    new HashSet<>(WorldNameUtil.parseWorldList(rewardSection.getStringList("blocked-worlds")));
            String permission = rewardSection.getString("permission");
            if (permission != null && permission.isBlank()) {
                permission = null;
            }
            parsed.put(entityType, new MobReward(entityType, heartsDelta, allowedWorlds, blockedWorlds, permission));
        }
        return parsed.isEmpty() ? Map.of() : Collections.unmodifiableMap(parsed);
    }

    public record WorldOverrides(
            Map<String, Double> heartsPerKillOverrides,
            Map<String, Double> heartsLostOnDeathOverrides,
            Map<String, Boolean> banWhenZeroOverrides
    ) {
    }
}
