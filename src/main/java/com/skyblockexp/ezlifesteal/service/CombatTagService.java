package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Tracks combat tags and applies combat-logout penalties.
 */
public class CombatTagService {
    private final PluginAccessor plugin;

    private final BanEnforcementService banEnforcementService;

    private final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();

    private final Map<UUID, Entity> lastDamagers = new ConcurrentHashMap<>();


    private String banMessage;

    private String kickMessage;

    private boolean combatLogoutProtectionEnabled;

    private long combatLogoutTagDurationMillis;


    public CombatTagService(PluginAccessor plugin,
                            BanEnforcementService banEnforcementService,
                            String banMessage,
                            String kickMessage,
                            boolean combatLogoutProtectionEnabled,
                            long combatLogoutTagDurationMillis) {
        this.plugin = plugin;
        this.banEnforcementService = banEnforcementService;
        this.banMessage = banMessage;
        this.kickMessage = kickMessage;
        this.combatLogoutProtectionEnabled = combatLogoutProtectionEnabled;
        this.combatLogoutTagDurationMillis = combatLogoutTagDurationMillis;
    }

    public void updateSettings(String banMessage,
                               String kickMessage,
                               boolean combatLogoutProtectionEnabled,
                               long combatLogoutTagDurationMillis) {
        this.banMessage = banMessage;
        this.kickMessage = kickMessage;
        this.combatLogoutProtectionEnabled = combatLogoutProtectionEnabled;
        this.combatLogoutTagDurationMillis = combatLogoutTagDurationMillis;
        if (!combatLogoutProtectionEnabled) {
            combatTags.clear();
        }
    }

    public void recordDamager(UUID victimId, Entity damager) {
        lastDamagers.put(victimId, damager);
    }

    public void tryTagPlayers(Player victim, Player attacker) {
        if (!combatLogoutProtectionEnabled || attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        if (!plugin.isGlobalLifestealEnabled() || !plugin.isLifestealEnabledInWorld(victim.getWorld().getName())) {
            return;
        }
        final long expiry = System.currentTimeMillis() + combatLogoutTagDurationMillis;
        combatTags.put(victim.getUniqueId(), expiry);
        combatTags.put(attacker.getUniqueId(), expiry);
    }

    public boolean isInCombat(UUID uniqueId) {
        final Long expiresAt = combatTags.get(uniqueId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt < System.currentTimeMillis()) {
            combatTags.remove(uniqueId);
            return false;
        }
        return true;
    }

    public void clearCombatTag(UUID uniqueId) {
        if (uniqueId == null) {
            return;
        }
        combatTags.remove(uniqueId);
    }

    public void clearPlayerState(UUID uniqueId) {
        if (uniqueId == null) {
            return;
        }
        clearCombatTag(uniqueId);
        lastDamagers.remove(uniqueId);
    }

    public boolean isMobDeath(UUID victimId) {
        if (victimId == null) {
            return false;
        }
        final Entity lastDamager = lastDamagers.get(victimId);
        return lastDamager instanceof LivingEntity;
    }

    public Map<UUID, Long> getCombatTags() {
        return combatTags;
    }

    public Map<UUID, Entity> getLastDamagers() {
        return lastDamagers;
    }

    public void handleCombatLogout(Player player) {
        final LifestealManager manager = plugin.getLifestealManager();
        final var optionalProfile = manager.getLoadedProfile(player.getUniqueId());
        if (optionalProfile.isEmpty()) {
            clearCombatTag(player.getUniqueId());
            return;
        }

        final LifestealProfile profile = optionalProfile.get();
        final String playerName = player.getName() != null ? player.getName() : player.getUniqueId().toString();
        final double previousHearts = profile.getHearts();
        final double heartsLostOnDeath = plugin.getHeartsLostOnDeath(player.getWorld().getName());
        profile.removeHearts(heartsLostOnDeath, manager.getMinHearts());
        final double remainingHearts = profile.getHearts();
        final double heartsLost = Math.max(0.0, previousHearts - remainingHearts);

        if (heartsLost > 0.0) {
            plugin.getLogger()
                    .info(playerName + " logged out during combat and lost " + formatHearts(heartsLost) + " hearts.");
        }
        else {
            plugin.getLogger().info(playerName + " logged out during combat.");
        }

        plugin.getMessageService().sendMessage(player, "combat-logout-penalty",
                Map.of("hearts_lost", formatHearts(heartsLost),
                        "remaining_hearts", formatHearts(remainingHearts)));

        manager.saveProfileAsync(profile).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().severe("Failed to save profile for combat logger "
                        + player.getName() + ": " + throwable.getMessage());
            }
        });

        final boolean shouldBan = plugin.isBanWhenZeroHearts(player.getWorld().getName()) && remainingHearts <= 0.0;
        if (shouldBan) {
            String formattedBanMessage = formatTemplate(banMessage, player, null, remainingHearts);
            String formattedKickMessage = formatTemplate(kickMessage, player, null, remainingHearts);
            if (formattedBanMessage.isEmpty() && formattedKickMessage.isEmpty()) {
                formattedBanMessage = ChatColor.translateAlternateColorCodes('&', "You have run out of hearts.");
                formattedKickMessage = formattedBanMessage;
            }
            else if (formattedBanMessage.isEmpty()) {
                formattedBanMessage = formattedKickMessage;
            }
            else if (formattedKickMessage.isEmpty()) {
                formattedKickMessage = formattedBanMessage;
            }
            banEnforcementService.applyBanWithStorage(player, formattedBanMessage, formattedKickMessage);
        }
        else if (remainingHearts <= 0.0) {
            plugin.executeZeroHeartCommands(player, null, remainingHearts);
        }

        final KillStreakManager killStreakManager = plugin.getKillStreakManager();
        if (killStreakManager != null) {
            killStreakManager.handleDeath(player);
        }
        plugin.requestTopHologramUpdate();
        clearCombatTag(player.getUniqueId());
    }

    private String formatTemplate(String template, Player victim, Player killer, double remainingHearts) {
        if (template == null || template.isBlank()) {
            return "";
        }
        String result = template;
        final String victimName = victim.getName() == null ? victim.getUniqueId().toString() : victim.getName();
        result = result.replace("%victim%", victimName);
        result = result.replace("%player%", victimName);
        String killerName = "";
        if (killer != null) {
            killerName = killer.getName() == null ? killer.getUniqueId().toString() : killer.getName();
        }
        result = result.replace("%killer%", killerName);
        final String formattedHearts = formatHearts(remainingHearts);
        result = result.replace("%remaining_hearts%", formattedHearts);
        result = result.replace("%hearts%", formattedHearts);
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String formatHearts(double value) {
        return value % 1 == 0 ? Integer.toString((int) value) : String.format(Locale.US, "%.1f", value);
    }
}
