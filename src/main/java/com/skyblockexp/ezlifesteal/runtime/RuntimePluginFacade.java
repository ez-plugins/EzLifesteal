package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.storage.Storage;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RuntimePluginFacade implements PluginAccessor {
    private final EzLifestealPlugin plugin;

    private final DefaultPluginRuntimeServices runtime;

    private final Registry registry;

    public RuntimePluginFacade(EzLifestealPlugin plugin, DefaultPluginRuntimeServices runtime) {

        this.plugin = plugin;

        this.runtime = runtime;
        this.registry = runtime.getRegistry();
    }

    @Override public JavaPlugin getPlugin() {
        return plugin;
    }

    @Override public Logger getLogger() {
        return plugin.getLogger();
    }

    @Override public String getPluginName() {
        return plugin.getName();
    }

    @Override public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override public String getPluginAuthors() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override public HeartRegistry getHeartRegistry() {
        return runtime.getHeartRegistry();
    }

    @Override public LifestealManager getLifestealManager() {
        return registry.getManagerState().getLifestealManager();
    }

    @Override public MessageService getMessageService() {
        return runtime.getMessageService();
    }

    @Override public PlayerLookupService getPlayerLookupService() {
        return registry.getManagerState().getPlayerLookupService();
    }

    @Override public TopHologramManager getTopHologramManager() {

        final TopHologramManager mgr = runtime.getTopHologramManager();

        return mgr != null ? mgr : registry.getManagerState().getTopHologramManager();

    }

    @Override public LifestealConfigAdapter getLifestealConfigAdapter() {
        return registry.getConfigState().getLifestealConfigAdapter();
    }

    @Override public AdminDetector getAdminDetector() {
        return runtime.getAdminDetector();
    }

    @Override public SmurfDetector getSmurfDetector() {
        return runtime.getSmurfDetector();
    }

    @Override public KillStreakManager getKillStreakManager() {
        return registry.getManagerState().getKillStreakManager();
    }

    @Override public Storage getStorage() {
        return runtime.getStorage();
    }

    @Override public MobReward getMobReward(EntityType entityType) {
        return registry.getGameplayState().getMobRulesState().getMobRewards().get(entityType);
    }

    @Override public boolean isGlobalLifestealEnabled() {
        return registry.getGameplayState().getHeartRulesState().isGlobalLifestealEnabled();
    }

    @Override public boolean isLifestealEnabledInWorld(String worldName) {
        return runtime.isLifestealEnabledInWorld(worldName);
    }

    @Override public boolean isAdminBypassHeartLoss() {
        return registry.getGameplayState().getHeartRulesState().isAdminBypassHeartLoss();
    }

    @Override public boolean isAdminBypassHeartGain() {
        return registry.getGameplayState().getHeartRulesState().isAdminBypassHeartGain();
    }

    @Override public boolean isDontRemoveHeartsFromMobs() {
        return registry.getGameplayState().getMobRulesState().isDontRemoveHeartsFromMobs();
    }

    @Override public double getMobRemoveHeartsGreaterThan() {
        return registry.getGameplayState().getMobRulesState().getMobRemoveHeartsGreaterThan();
    }

    @Override public double getHeartsLostOnDeath(String worldName) {
        return runtime.getHeartsLostOnDeath(worldName);
    }

    @Override public boolean isBanWhenZeroHearts(String worldName) {
        return runtime.isBanWhenZeroHearts(worldName);
    }

    @Override public boolean isDropHeartOnDeath() {
        return registry.getGameplayState().getDropRulesState().isDropHeartOnDeath();
    }

    @Override public boolean isDropHeartOnlyWhenKilledByPlayer() {
        return registry.getGameplayState().getDropRulesState().isDropHeartOnlyWhenKilledByPlayer();
    }

    @Override public String getDropHeartId() {
        return registry.getGameplayState().getDropRulesState().getDropHeartId();
    }

    @Override public int getDropHeartAmount() {
        return registry.getGameplayState().getDropRulesState().getDropHeartAmount();
    }

    @Override public boolean isReviveBeaconEnabled() {
        return registry.getGameplayState().getReviveBeaconState().isReviveBeaconEnabled();
    }

    @Override public String getReviveBeaconVoucherHeartId() {
        return registry.getGameplayState().getReviveBeaconState().getReviveBeaconVoucherHeartId();
    }

    @Override public boolean isReviveBeaconRequireSneak() {
        return registry.getGameplayState().getReviveBeaconState().isReviveBeaconRequireSneak();
    }

    @Override public double getReviveBeaconMaxDistance() {
        return registry.getGameplayState().getReviveBeaconState().getReviveBeaconMaxDistance();
    }

    @Override public boolean isReviveBeaconConsumeOnFail() {
        return registry.getGameplayState().getReviveBeaconState().isReviveBeaconConsumeOnFail();
    }

    @Override public boolean isReviveBeaconRequireVoucherInBeacon() {

        return registry.getGameplayState().getReviveBeaconState().isReviveBeaconRequireVoucherInBeacon();

    }

    @Override public double getReviveBeaconVoucherHoldSeconds() {

        return registry.getGameplayState().getReviveBeaconState().getReviveBeaconVoucherHoldSeconds();
    }

    @Override public boolean isReviveBeaconWhitelistEnabled() {

        return registry.getGameplayState().getReviveBeaconState().isReviveBeaconWhitelistEnabled();
    }

    @Override public java.util.List<String> getReviveBeaconWhitelistedBeacons() {

        return registry.getGameplayState().getReviveBeaconState().getReviveBeaconWhitelistedBeacons();
    }

    @Override public com.skyblockexp.ezlifesteal.config.ReviveAnimationSettings getReviveAnimationSettings() {

        return registry.getGameplayState().getReviveBeaconState().getReviveAnimationSettings();
    }

    @Override public boolean isReviveBeaconBroadcastEnabled() {

        return registry.getGameplayState().getReviveBeaconState().isReviveBeaconBroadcastEnabled();
    }

    @Override public String getReviveBeaconBroadcastHoldStartMessageKey() {

        return registry.getGameplayState().getReviveBeaconState().getReviveBeaconBroadcastHoldStartMessageKey();
    }

    @Override public String getReviveBeaconBroadcastCompleteMessageKey() {

        return registry.getGameplayState().getReviveBeaconState().getReviveBeaconBroadcastCompleteMessageKey();
    }

    @Override public double getHeartsPerKill(String worldName) {
        return runtime.getHeartsPerKill(worldName);
    }

    @Override public void clearHeartStatus(UUID playerId) {
        runtime.clearHeartStatus(playerId);
    }

    @Override public void sendHeartStatus(Player player, double hearts) {
        runtime.sendHeartStatus(player, hearts);
    }

    @Override public void executeZeroHeartCommands(Player victim, Player killer, double remainingHearts) {
        runtime.executeZeroHeartCommands(victim, killer, remainingHearts);
    }

    @Override public void requestTopHologramUpdate() {
        runtime.requestTopHologramUpdate();
    }

    @Override public void reloadPlugin(CommandSender initiator) {
        plugin.reloadPlugin(initiator);
    }

    @Override public void simulatePlayerDeath(Player victim, Player killer) {
        runtime.simulatePlayerDeath(victim, killer);
    }

    @Override public void simulatePlayerKill(Player killer) {
        runtime.simulatePlayerKill(killer);
    }

}
