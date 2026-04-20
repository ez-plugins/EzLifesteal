package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.BanEnforcementService;
import com.skyblockexp.ezlifesteal.service.BeaconReviveService;
import com.skyblockexp.ezlifesteal.service.CombatTagService;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.service.ParticleEffectService;
import com.skyblockexp.ezlifesteal.service.PlayerDeathService;
import com.skyblockexp.ezlifesteal.service.ReviveAnimationService;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class PlayerListener implements Listener {
    private final PluginAccessor plugin;

    private final CombatTagService combatTagService;

    private final PlayerDeathService playerDeathService;

    private final ParticleEffectService particleEffectService;

    private final BeaconReviveService beaconReviveService;


    private String banMessage;

    private String kickMessage;

    @SuppressWarnings("unused")

    private final Map<UUID, Long> combatTags;

    @SuppressWarnings("unused")

    private final Map<UUID, Entity> lastDamagers;

    public PlayerListener(PluginAccessor plugin,
                          String banMessage,
                          String kickMessage,
                          boolean combatLogoutProtectionEnabled,
                          long combatLogoutTagDurationMillis) {
        this(plugin,
                new BanEnforcementService(plugin),
                null,
                null,
                null,
                null,
                banMessage,
                kickMessage,
                combatLogoutProtectionEnabled,
                combatLogoutTagDurationMillis);
    }

    public PlayerListener(PluginAccessor plugin,
                          BanEnforcementService banEnforcementService,
                          CombatTagService combatTagService,
                          PlayerDeathService playerDeathService,
                          ParticleEffectService particleEffectService,
                          BeaconReviveService beaconReviveService,
                          String banMessage,
                          String kickMessage,
                          boolean combatLogoutProtectionEnabled,
                          long combatLogoutTagDurationMillis) {
        this.plugin = plugin;
        final BanEnforcementService enforcementService = banEnforcementService == null
                ? new BanEnforcementService(plugin) : banEnforcementService;
        this.combatTagService = combatTagService == null
                ? new CombatTagService(plugin, enforcementService, banMessage, kickMessage,
                combatLogoutProtectionEnabled, combatLogoutTagDurationMillis)
                : combatTagService;
        this.playerDeathService = playerDeathService == null
                ? new PlayerDeathService(plugin, enforcementService)
                : playerDeathService;
        this.particleEffectService = particleEffectService == null
                ? new ParticleEffectService(plugin)
                : particleEffectService;
        this.beaconReviveService = beaconReviveService == null
                ? new BeaconReviveService(plugin, new ReviveAnimationService(plugin))
                : beaconReviveService;
        this.combatTags = this.combatTagService.getCombatTags();
        this.lastDamagers = this.combatTagService.getLastDamagers();
        this.banMessage = banMessage;
        this.kickMessage = kickMessage;
    }

    public void updateSettings(String banMessage,
                               String kickMessage,
                               boolean combatLogoutProtectionEnabled,
                               long combatLogoutTagDurationMillis) {
        this.banMessage = banMessage;
        this.kickMessage = kickMessage;
        combatTagService.updateSettings(banMessage, kickMessage,
                combatLogoutProtectionEnabled, combatLogoutTagDurationMillis);
    }

    public BeaconReviveService getBeaconReviveService() {
        return beaconReviveService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        combatTagService.clearPlayerState(player.getUniqueId());
        if (!plugin.isGlobalLifestealEnabled()) {
            plugin.clearHeartStatus(player.getUniqueId());
            return;
        }
        final LifestealManager manager = plugin.getLifestealManager();
        final MessageService messageService = plugin.getMessageService();
        manager.loadProfileAsync(player.getUniqueId()).whenComplete((profile, throwable) -> {
            if (throwable != null) {
                plugin.getLogger()
                        .severe("Failed to load profile for " + player.getName() + ": " + throwable.getMessage());
                SchedulerAdapter.run(plugin.getPlugin(), () -> {
                    if (!player.isOnline()) {
                        plugin.clearHeartStatus(player.getUniqueId());
                        return;
                    }
                    messageService.sendMessage(player, "storage-error");
                });
                return;
            }
            SchedulerAdapter.run(plugin.getPlugin(), () -> {
                if (!player.isOnline()) {
                    plugin.clearHeartStatus(player.getUniqueId());
                    return;
                }
                if (!plugin.isLifestealEnabledInWorld(player.getWorld().getName())) {
                    plugin.getLogger().info("Skipping lifesteal join handling for " + player.getName()
                            + " in disabled world " + player.getWorld().getName());
                    plugin.clearHeartStatus(player.getUniqueId());
                    return;
                }
                manager.applyHearts(player, profile);
                plugin.sendHeartStatus(player, profile.getHearts());
                sendInstallationMessageToAdmin(player);
            });
        });
    }


    private void sendInstallationMessageToAdmin(Player player) {
        final AdminDetector adminDetector = plugin.getAdminDetector();
        if (adminDetector == null || !adminDetector.isAdmin(player)) {
            return;
        }
        if (plugin.getPlugin() == null) {
            return;
        }

        final NamespacedKey key = new NamespacedKey(plugin.getPlugin(), "admin_install_info_sent");
        if (player.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            return;
        }

        final MessageService messageService = plugin.getMessageService();
        sendInstallationLine(player, messageService, "admin-install-header",
                "&c[EzLifesteal] &7Thanks for installing EzLifesteal!");
        sendInstallationLine(player, messageService, "admin-install-line-1",
                "&7Use &f/lifesteal help &7to view available subcommands.");
        sendInstallationLine(player, messageService, "admin-install-line-2",
                "&7Use &f/lifesteal about &7to view plugin metadata.");
        sendInstallationLine(player, messageService, "admin-install-line-3",
                "&7Configure files in your &fplugins/EzLifesteal &7folder.");
        sendInstallationLine(player, messageService, "admin-install-line-4",
                "&7Run &f/lifesteal reload &7after changing configuration.");
        player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    private void sendInstallationLine(Player player,
                                      MessageService messageService,
                                      String key,
                                      String fallbackMessage) {
        if (messageService != null && !messageService.getMessage(key).isBlank()) {
            messageService.sendMessage(player, key);
            return;
        }
        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', fallbackMessage));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (combatTagService.isInCombat(player.getUniqueId())
                && plugin.isGlobalLifestealEnabled()
                && plugin.isLifestealEnabledInWorld(player.getWorld().getName())) {
            combatTagService.handleCombatLogout(player);
        }

        final LifestealManager manager = plugin.getLifestealManager();
        manager.getLoadedProfile(player.getUniqueId()).ifPresent(profile ->
                manager.saveProfileAsync(profile).whenComplete((unused, throwable) -> {
                    if (throwable != null) {
                        plugin.getLogger()
                                .severe("Failed to save profile for " + player.getName()
                                        + ": " + throwable.getMessage());
                    }
                    manager.unload(player.getUniqueId());
                })
        );

        plugin.clearHeartStatus(player.getUniqueId());
        combatTagService.clearPlayerState(player.getUniqueId());
        final KillStreakManager killStreakManager = plugin.getKillStreakManager();
        if (killStreakManager != null) {
            killStreakManager.handleQuit(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        final Entity damager = event.getDamager();
        combatTagService.recordDamager(victim.getUniqueId(), damager);
        combatTagService.tryTagPlayers(victim, resolveAttacker(damager));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        handlePlayerDeath(event.getEntity(), event.getEntity().getKiller());
    }

    public void handlePlayerDeath(Player victim, Player killer) {
        final boolean mobDeath = combatTagService.isMobDeath(victim.getUniqueId());
        combatTagService.clearPlayerState(victim.getUniqueId());
        if (killer != null) {
            combatTagService.clearCombatTag(killer.getUniqueId());
        }
        playerDeathService.handlePlayerDeath(victim, killer,
                mobDeath, banMessage, kickMessage);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null
                && beaconReviveService.tryHandleBeaconInteract(player, item, event.getClickedBlock())) {
            event.setCancelled(true);
            return;
        }

        try {
            final var container = item.getItemMeta() == null ? null : item.getItemMeta().getPersistentDataContainer();
            if (container == null) {
                return;
            }
            final String heartId = plugin.getHeartIdFrom(container);
            if (heartId == null) {
                return;
            }

            event.setCancelled(true);
            final var registry = plugin.getHeartRegistry();
            if (registry == null) {
                return;
            }
            final var heart = registry.getById(heartId);
            if (heart == null) {
                return;
            }

            final LifestealManager manager = plugin.getLifestealManager();
            final var optional = manager.getLoadedProfile(player.getUniqueId());
            if (optional.isEmpty()) {
                return;
            }
            final LifestealProfile profile = optional.get();
            final double current = profile.getHearts();
            final double max = manager.getMaxHearts();
            if (current >= max) {
                SchedulerAdapter.run(plugin.getPlugin(), () -> {
                    final var ms = plugin.getMessageService();
                    final String configured = ms.getMessage("already-at-max");
                    if (configured == null || configured.isBlank()) {
                        player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                                "You Already have reached the max heart capacity"));
                    }
                    else {
                        ms.sendMessage(player, "already-at-max");
                    }
                });
                return;
            }

            profile.addHearts(heart.getHearts(), max);
            manager.saveProfileAsync(profile);
            SchedulerAdapter.run(plugin.getPlugin(), () -> {
                final var ms = plugin.getMessageService();
                ms.sendMessage(player, "heart-redeemed", Map.of(
                        "heart", heart.getId(),
                        "amount", Double.toString(heart.getHearts()),
                        "total", Double.toString(profile.getHearts())
                ));
                manager.applyHearts(player, profile);
                plugin.sendHeartStatus(player, profile.getHearts());
            });
            item.setAmount(Math.max(0, item.getAmount() - 1));
            final Location location = player.getLocation().add(0, 1, 0);
            particleEffectService.spawnHeartConsumptionEffects(player, location);
        }
        catch (Exception ignored) {
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        if (damager instanceof TNTPrimed primed && primed.getSource() instanceof Player source) {
            return source;
        }
        if (damager instanceof Tameable tameable && tameable.getOwner() instanceof Player owner) {
            return owner;
        }
        return null;
    }

    @SuppressWarnings("unused")
    private void handleCombatLogout(Player player) {
        combatTagService.handleCombatLogout(player);
    }

    @SuppressWarnings("unused")
    private boolean isMobDeath(UUID victimId) {
        return combatTagService.isMobDeath(victimId);
    }
}
