package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.config.ReviveAnimationSettings;
import com.skyblockexp.ezlifesteal.service.BeaconSpawnService;
import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.service.TeamBankAdminService;
import com.skyblockexp.ezlifesteal.service.TeamBankService;
import com.skyblockexp.ezlifesteal.storage.Storage;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import com.skyblockexp.ezlifesteal.storage.repository.TeamBankRepository;
import com.skyblockexp.ezlifesteal.util.ban.PlatformBanAdapter;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

public interface PluginAccessor {
    JavaPlugin getPlugin();

    Logger getLogger();

    String getPluginName();

    String getPluginVersion();

    String getPluginAuthors();

    HeartRegistry getHeartRegistry();

    LifestealManager getLifestealManager();

    MessageService getMessageService();

    PlayerLookupService getPlayerLookupService();

    TopHologramManager getTopHologramManager();

    LifestealConfigAdapter getLifestealConfigAdapter();

    AdminDetector getAdminDetector();

    SmurfDetector getSmurfDetector();

    KillStreakManager getKillStreakManager();

    Storage getStorage();

    default ProfileRepository getProfileRepository() {

        return getStorage();
    }

    default BanRepository getBanRepository() {

        return getStorage();
    }

    default PlatformBanAdapter getBanAdapter() {
        return null;
    }

    default TeamBankRepository getTeamBankRepository() {
        return null;
    }

    default TeamBankService getTeamBankService() {
        return null;
    }

    MobReward getMobReward(EntityType entityType);

    boolean isGlobalLifestealEnabled();

    boolean isLifestealEnabledInWorld(String worldName);

    default boolean isTeamBankEnabled() {
        return false;
    }

    default double getTeamBankMaxHearts() {
        return 0.0D;
    }

    default boolean isTeamKillBypassEnabled() {
        return false;
    }

    default boolean shouldBypassForTeamKill(org.bukkit.entity.Player killer, org.bukkit.entity.Player victim) {
        return false;
    }

    default List<String> getTeamKillBypassExemptWorlds() {
        return List.of();
    }

    default int getTeamKillBypassMinTeamSize() {
        return 1;
    }

    default double getTeamBankMaxHeartsForTeam(UUID teamId) {
        return getTeamBankMaxHearts();
    }

    default TeamBankAdminService getTeamBankAdminService() {
        return null;
    }

    boolean isAdminBypassHeartLoss();

    boolean isAdminBypassHeartGain();

    boolean isDontRemoveHeartsFromMobs();

    double getMobRemoveHeartsGreaterThan();

    double getHeartsLostOnDeath(String worldName);

    boolean isBanWhenZeroHearts(String worldName);

    boolean isDropHeartOnDeath();

    boolean isDropHeartOnlyWhenKilledByPlayer();

    String getDropHeartId();

    int getDropHeartAmount();

    boolean isReviveBeaconEnabled();

    String getReviveBeaconVoucherHeartId();

    boolean isReviveBeaconRequireSneak();

    double getReviveBeaconMaxDistance();

    boolean isReviveBeaconConsumeOnFail();

    default boolean isReviveBeaconRequireVoucherInBeacon() {
        return false;
    }

    default double getReviveBeaconVoucherHoldSeconds() {
        return 0.0D;
    }

    default boolean isReviveBeaconWhitelistEnabled() {
        return false;
    }

    default java.util.List<String> getReviveBeaconWhitelistedBeacons() {
        return java.util.List.of();
    }

    default ReviveAnimationSettings getReviveAnimationSettings() {
        return ReviveAnimationSettings.defaults();
    }

    default boolean isReviveBeaconBroadcastEnabled() {
        return false;
    }

    default String getReviveBeaconBroadcastHoldStartMessageKey() {
        return "beacon-revive-broadcast-hold-start";
    }

    default String getReviveBeaconBroadcastCompleteMessageKey() {
        return "beacon-revive-broadcast-complete";
    }

    double getHeartsPerKill(String worldName);

    void clearHeartStatus(java.util.UUID playerId);

    void sendHeartStatus(org.bukkit.entity.Player player, double hearts);

    void executeZeroHeartCommands(org.bukkit.entity.Player victim, org.bukkit.entity.Player killer,
            double remainingHearts);

    void requestTopHologramUpdate();

    void reloadPlugin(org.bukkit.command.CommandSender initiator);

    void simulatePlayerDeath(org.bukkit.entity.Player victim, org.bukkit.entity.Player killer);

    void simulatePlayerKill(org.bukkit.entity.Player killer);

    // Test-friendly helper to extract heart id from an item's PersistentDataContainer
    default String getHeartIdFrom(org.bukkit.persistence.PersistentDataContainer container) {

        if (container == null) {
            return null;
        }
        try {
            final var key = EzLifestealPlugin.HEART_KEY;
            if (key == null) {
                return null;
            }
            return container.get(key, org.bukkit.persistence.PersistentDataType.STRING);
        }
        catch (Throwable t) {
            return null;
        }
    }

    default BeaconSpawnSettings getBeaconSpawnSettings() {
        return BeaconSpawnSettings.disabled();
    }

    default BeaconSpawnService getBeaconSpawnService() {
        return null;
    }
}
