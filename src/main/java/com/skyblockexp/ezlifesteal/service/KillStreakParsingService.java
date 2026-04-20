package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.killstreak.KillStreakReward;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakSettings;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Helper service to parse kill-streak and reward configuration into domain objects.
 */
public class KillStreakParsingService {

    private final DefaultPluginRuntimeServices services;

    public KillStreakParsingService(DefaultPluginRuntimeServices services) {
        this.services = services;
    }

    public KillStreakSettings parseKillStreakSettings() {
        final ConfigurationSection section = services.getLifestealConfigAdapter().getSection("kill-streaks");
        if (section == null) {
            return KillStreakSettings.disabled();
        }
        final boolean enabled = section.getBoolean("enabled", false);
        final boolean resetOnDeath = section.getBoolean("reset-on-death", true);
        final ConfigurationSection rewardsSection = section.getConfigurationSection("rewards");
        if (rewardsSection == null || rewardsSection.getKeys(false).isEmpty()) {
            return new KillStreakSettings(enabled, resetOnDeath, Map.of());
        }
        final Map<Integer, List<KillStreakReward>> rewardMap = new HashMap<>();
        for (String key : rewardsSection.getKeys(false)) {
            if (key == null || key.isBlank()) {
                continue;
            }
            ConfigurationSection rewardSection = rewardsSection.getConfigurationSection(key);
            if (rewardSection == null) {
                final Object raw = rewardsSection.get(key);
                if (raw instanceof ConfigurationSection nested) {
                    rewardSection = nested;
                }
            }
            if (rewardSection == null) {
                services.getLogger()
                        .warning("Skipping lifesteal.kill-streaks.rewards entry '" + key + "' because it is not a"
                                + " section.");
                continue;
            }
            final KillStreakReward reward = parseKillStreakReward(key, rewardSection);
            if (reward == null) {
                continue;
            }
            rewardMap.computeIfAbsent(reward.getStreak(), ignored -> new ArrayList<>()).add(reward);
        }
        if (rewardMap.isEmpty()) {
            return new KillStreakSettings(enabled, resetOnDeath, Map.of());
        }
        final Map<Integer, List<KillStreakReward>> immutable = new HashMap<>();
        for (Map.Entry<Integer, List<KillStreakReward>> entry : rewardMap.entrySet()) {
            immutable.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new KillStreakSettings(enabled, resetOnDeath, immutable);
    }

    public KillStreakReward parseKillStreakReward(String key, ConfigurationSection rewardSection) {
        int streak = rewardSection.getInt("streak", -1);
        if (streak <= 0) {
            try {
                streak = Integer.parseInt(key.trim());
            }
            catch (NumberFormatException ignored) {
            }
        }
        if (streak <= 0) {
            services.getLogger().warning("Kill streak reward '" + key + "' is missing a valid streak value; skipping.");
            return null;
        }
        final double money = rewardSection.getDouble("money", 0.0);
        List<String> commands = new ArrayList<>(rewardSection.getStringList("commands"));
        if (commands.isEmpty() && rewardSection.isString("command")) {
            commands.add(rewardSection.getString("command"));
        }
        commands = commands.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
        List<String> messages = new ArrayList<>(rewardSection.getStringList("messages"));
        if (rewardSection.isString("message")) {
            final String message = rewardSection.getString("message");
            if (message != null) {
                messages.add(message);
            }
        }
        messages = messages.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isEmpty())
                .collect(Collectors.toList());
        String broadcastMessage = rewardSection.getString("broadcast-message");
        if (broadcastMessage != null && broadcastMessage.isBlank()) {
            broadcastMessage = null;
        }
        final List<ItemStack> items = parseRewardItems(rewardSection, key);
        return new KillStreakReward(streak, money, commands, messages, broadcastMessage, items);
    }

    public List<ItemStack> parseRewardItems(ConfigurationSection rewardSection, String key) {
        if (!rewardSection.contains("items")) {
            return List.of();
        }
        final List<?> rawItems = rewardSection.getList("items");
        if (rawItems == null || rawItems.isEmpty()) {
            return List.of();
        }
        final List<ItemStack> items = new ArrayList<>();
        for (Object raw : rawItems) {
            final ItemStack parsed = parseRewardItem(raw, key);
            if (parsed != null) {
                items.add(parsed);
            }
        }
        return items.isEmpty() ? List.of() : items;
    }

    public ItemStack parseRewardItem(Object raw, String key) {
        if (raw instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            final Map<?, ?> m = (Map<?, ?>) map;
            return parseRewardItemMap(m, key);
        }
        if (raw instanceof String s) {
            return parseRewardItemString(s, key);
        }
        services.getLogger().warning("Unsupported item entry in kill streak reward '" + key + "': " + raw);
        return null;
    }

    public ItemStack parseRewardItemMap(Map<?, ?> map, String key) {
        Object materialValue = map.get("material");
        if (materialValue == null) {
            materialValue = map.get("type");
        }
        if (materialValue == null) {
            materialValue = map.get("item");
        }
        if (!(materialValue instanceof String materialName) || materialName.isBlank()) {
            services.getLogger()
                    .warning("Kill streak reward '" + key + "' is missing an item material; skipping entry.");
            return null;
        }
        final Material material = Material.matchMaterial(materialName.trim());
        if (material == null) {
            services.getLogger()
                    .warning("Unknown material '" + materialName + "' in kill streak reward '" + key + "'; skipping"
                            + " entry.");
            return null;
        }
        int amount = 1;
        final Object amountValue = map.get("amount");
        if (amountValue instanceof Number number) {
            amount = Math.max(1, number.intValue());
        }
        else if (amountValue instanceof String amountString && !amountString.isBlank()) {
            try {
                amount = Math.max(1, Integer.parseInt(amountString.trim()));
            }
            catch (NumberFormatException exception) {
                services.getLogger()
                        .warning("Invalid amount '" + amountString + "' in kill streak reward '" + key + "'; defaulting"
                                + " to 1.");
            }
        }
        final ItemStack stack = new ItemStack(material, amount);
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            final Object nameValue = map.get("name");
            if (nameValue instanceof String name && !name.isBlank()) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            }
            final Object loreValue = map.get("lore");
            if (loreValue instanceof List<?> loreList && !loreList.isEmpty()) {
                final List<String> lore = new ArrayList<>();
                for (Object line : loreList) {
                    if (line instanceof String text && !text.isBlank()) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', text));
                    }
                }
                if (!lore.isEmpty()) {
                    meta.setLore(lore);
                }
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public ItemStack parseRewardItemString(String value, String key) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String materialToken = trimmed;
        int amount = 1;
        final String[] colonSplit = trimmed.split(":", 2);
        if (colonSplit.length == 2) {
            materialToken = colonSplit[0];
            final String amountToken = colonSplit[1].trim();
            if (!amountToken.isEmpty()) {
                try {
                    amount = Math.max(1, Integer.parseInt(amountToken));
                }
                catch (NumberFormatException exception) {
                    services.getLogger()
                            .warning("Invalid item amount '" + amountToken + "' in kill streak reward '" + key + "';"
                                    + " defaulting to 1.");
                }
            }
        }
        else {
            final String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2) {
                materialToken = parts[0];
                try {
                    amount = Math.max(1, Integer.parseInt(parts[1]));
                }
                catch (NumberFormatException exception) {
                    services.getLogger()
                            .warning("Invalid item amount '" + parts[1] + "' in kill streak reward '" + key + "';"
                                    + " defaulting to 1.");
                }
            }
        }
        final Material material = Material.matchMaterial(materialToken.trim());
        if (material == null) {
            services.getLogger()
                    .warning("Unknown material '" + materialToken + "' in kill streak reward '" + key + "'; skipping"
                            + " entry.");
            return null;
        }
        return new ItemStack(material, amount);
    }
}
