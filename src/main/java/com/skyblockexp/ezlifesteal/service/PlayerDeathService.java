package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Resolves heart loss/gain behavior for player death events.
 */
public class PlayerDeathService {
    private final PluginAccessor plugin;

    private final BanEnforcementService banEnforcementService;

    private final DeathOutcomePolicy deathOutcomePolicy;

    private final BanMessageFormatter banMessageFormatter;

    private final KillerRewardService killerRewardService;


    public PlayerDeathService(PluginAccessor plugin, BanEnforcementService banEnforcementService) {
        this(plugin, banEnforcementService, new DeathOutcomePolicy(), new BanMessageFormatter(),
                new KillerRewardService(plugin));
    }

    public PlayerDeathService(PluginAccessor plugin,
                              BanEnforcementService banEnforcementService,
                              DeathOutcomePolicy deathOutcomePolicy,
                              BanMessageFormatter banMessageFormatter,
                              KillerRewardService killerRewardService) {
        this.plugin = plugin;
        this.banEnforcementService = banEnforcementService;
        this.deathOutcomePolicy = deathOutcomePolicy;
        this.banMessageFormatter = banMessageFormatter;
        this.killerRewardService = killerRewardService;
    }

    public void handlePlayerDeath(Player victim, Player killer, boolean mobDeath, String banMessage,
            String kickMessage) {
        if (!plugin.isGlobalLifestealEnabled()) {
            return;
        }
        if (!plugin.isLifestealEnabledInWorld(victim.getWorld().getName())) {
            plugin.getLogger().info("Skipping lifesteal death handling for " + victim.getName()
                    + " in disabled world " + victim.getWorld().getName());
            return;
        }

        final UUID victimId = victim.getUniqueId();
        final LifestealManager manager = plugin.getLifestealManager();
        final LifestealProfile victimProfile = manager.getOrCreateProfile(victimId);
        final AdminDetector adminDetector = plugin.getAdminDetector();
        final boolean victimIsAdmin = adminDetector != null && adminDetector.isAdmin(victim);

        final String victimWorld = victim.getWorld().getName();
        final DeathOutcome outcome = deathOutcomePolicy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                victimIsAdmin,
                plugin.isAdminBypassHeartLoss(),
                killer != null,
                mobDeath,
                plugin.isDontRemoveHeartsFromMobs(),
                plugin.getMobRemoveHeartsGreaterThan(),
                victimProfile.getHearts(),
                plugin.getHeartsLostOnDeath(victimWorld),
                manager.getMinHearts(),
                plugin.isBanWhenZeroHearts(victimWorld)
        ));

        if (outcome.applyHeartLoss()) {
            victimProfile.removeHearts(-outcome.heartDelta(), manager.getMinHearts());
            manager.saveProfileAsync(victimProfile).whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().severe("Failed to save victim profile for "
                            + victim.getName() + ": " + throwable.getMessage());
                }
            });

            if (outcome.shouldBan()) {
                final BanMessageFormatter.BanMessages messages =
                        banMessageFormatter.formatMessages(banMessage, kickMessage, victim, killer,
                                victimProfile.getHearts());
                banEnforcementService.applyBanWithStorage(victim, messages.banMessage(), messages.kickMessage());
            }
            else {
                if (outcome.shouldExecuteZeroHeartCommands()) {
                    plugin.executeZeroHeartCommands(victim, killer, victimProfile.getHearts());
                }
                scheduleVictimUiSync(victim, victimProfile, manager);
            }
        }
        else {
            scheduleVictimUiSync(victim, victimProfile, manager);
        }

        final KillStreakManager killStreakManager = plugin.getKillStreakManager();
        if (killStreakManager != null) {
            killStreakManager.handleDeath(victim);
        }

        if (killer != null && !killer.getUniqueId().equals(victimId)) {
            rewardKiller(victim, killer, manager, adminDetector, killStreakManager);
        }

        plugin.requestTopHologramUpdate();
    }

    private void rewardKiller(Player victim,
                              Player killer,
                              LifestealManager manager,
                              AdminDetector adminDetector,
                              KillStreakManager killStreakManager) {
        if (!plugin.isLifestealEnabledInWorld(killer.getWorld().getName())) {
            plugin.getLogger().info("Skipping lifesteal rewards for killer " + killer.getName()
                    + " in disabled world " + killer.getWorld().getName());
            plugin.requestTopHologramUpdate();
            return;
        }

        final LifestealProfile killerProfile = manager.getOrCreateProfile(killer.getUniqueId());
        final boolean killerIsAdmin = adminDetector != null && adminDetector.isAdmin(killer);
        final KillerOutcome killerOutcome = deathOutcomePolicy.computeKillerOutcome(new DeathOutcomePolicy.KillerInput(
                killerIsAdmin,
                plugin.isAdminBypassHeartGain(),
                plugin.isDropHeartOnDeath(),
                plugin.getHeartsPerKill(killer.getWorld().getName())
        ));

        if (killerOutcome.applyHeartGain()) {
            killerRewardService.applyKillerReward(victim, killer, killerProfile, manager, killerOutcome);
        }
        else {
            SchedulerAdapter.run(plugin.getPlugin(), () -> {
                manager.applyHearts(killer, killerProfile);
                plugin.sendHeartStatus(killer, killerProfile.getHearts());
            });
        }

        final var smurfDetector = plugin.getSmurfDetector();
        if (smurfDetector != null) {
            smurfDetector.recordKill(killer, victim);
        }
        if (killStreakManager != null) {
            killStreakManager.handleKill(killer);
        }
    }

    private void scheduleVictimUiSync(Player victim, LifestealProfile victimProfile, LifestealManager manager) {
        SchedulerAdapter.run(plugin.getPlugin(), () -> {
            manager.applyHearts(victim, victimProfile);
            plugin.sendHeartStatus(victim, victimProfile.getHearts());
        });
    }
}
