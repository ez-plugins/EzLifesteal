package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * Fires the "availability event" when a spawned beacon transitions to the AVAILABLE state.
 *
 * <p>Events include server broadcast, title overlay, particles, and fireworks — each
 * individually configurable via {@link BeaconSpawnSettings.AvailabilityEventSettings}.</p>
 */
public final class BeaconAvailabilityService {

    private static final Random RANDOM = new Random();

    private final Logger logger;

    public BeaconAvailabilityService(Logger logger) {
        this.logger = logger;
    }

    /**
     * Fires all enabled availability events for the given beacon.
     *
     * @param beacon   the beacon that just became available
     * @param accessor plugin accessor (for message service and plugin instance)
     * @param settings availability event settings from config
     */
    public void fireAvailabilityEvent(
            SpawnedBeacon beacon,
            PluginAccessor accessor,
            BeaconSpawnSettings.AvailabilityEventSettings settings
    ) {
        final Location location = beacon.getLocation();
        if (location.getWorld() == null) {
            logger.warning("Beacon " + beacon.shortId() + " has no world; cannot fire availability event.");
            return;
        }

        if (settings.broadcastEnabled()) {
            fireBroadcast(beacon, accessor, settings.broadcastMessageKey());
        }
        if (settings.titleEnabled()) {
            fireTitle(beacon, accessor, settings.titleKey(), settings.subtitleKey());
        }
        if (settings.particlesEnabled()) {
            fireParticles(location);
        }
        if (settings.fireworksEnabled()) {
            fireFireworks(accessor, location);
        }
    }

    private void fireBroadcast(
            SpawnedBeacon beacon,
            PluginAccessor accessor,
            String messageKey
    ) {
        final MessageService messageService = accessor.getMessageService();
        if (messageService == null) {
            return;
        }
        final Location loc = beacon.getLocation();
        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("world", loc.getWorld() != null ? loc.getWorld().getName() : "?");
        placeholders.put("x", String.valueOf(loc.getBlockX()));
        placeholders.put("y", String.valueOf(loc.getBlockY()));
        placeholders.put("z", String.valueOf(loc.getBlockZ()));

        final String message = messageService.format(messageKey, placeholders);
        if (!message.isBlank()) {
            Bukkit.broadcastMessage(message);
        }
    }

    private void fireTitle(
            SpawnedBeacon beacon,
            PluginAccessor accessor,
            String titleKey,
            String subtitleKey
    ) {
        final MessageService messageService = accessor.getMessageService();
        if (messageService == null) {
            return;
        }
        final Location loc = beacon.getLocation();
        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("world", loc.getWorld() != null ? loc.getWorld().getName() : "?");
        placeholders.put("x", String.valueOf(loc.getBlockX()));
        placeholders.put("y", String.valueOf(loc.getBlockY()));
        placeholders.put("z", String.valueOf(loc.getBlockZ()));

        final String title = messageService.render(titleKey, placeholders);
        final String subtitle = messageService.render(subtitleKey, placeholders);
        final String resolvedTitle = title != null ? title : "";
        final String resolvedSubtitle = subtitle != null ? subtitle : "";

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(resolvedTitle, resolvedSubtitle, 10, 70, 20);
        }
    }

    private void fireParticles(Location location) {
        final World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.spawnParticle(Particle.END_ROD, location.clone().add(0.5, 1.0, 0.5), 80, 1.5, 1.5, 1.5, 0.05);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, location.clone().add(0.5, 0.5, 0.5), 50, 0.5, 0.5, 0.5, 0.1);
    }

    private void fireFireworks(PluginAccessor accessor, Location location) {
        final World world = location.getWorld();
        if (world == null) {
            return;
        }
        final Location spawnLoc = location.clone().add(0.5, 1.0, 0.5);
        for (int i = 0; i < 3; i++) {
            final int fireworkIndex = i;
            if (i == 0) {
                spawnFirework(accessor, spawnLoc, Color.YELLOW, Color.WHITE);
            } else {
                final Location offset = spawnLoc.clone().add(
                        (RANDOM.nextDouble() - 0.5) * 2,
                        0,
                        (RANDOM.nextDouble() - 0.5) * 2
                );
                // Stagger the remaining fireworks slightly
                com.skyblockexp.ezlifesteal.util.SchedulerAdapter.runLater(
                        accessor.getPlugin(),
                        () -> spawnFirework(accessor, offset,
                                fireworkIndex == 1 ? Color.RED : Color.AQUA,
                                Color.WHITE),
                        fireworkIndex * 5L
                );
            }
        }
    }

    private void spawnFirework(PluginAccessor accessor, Location location, Color primary, Color fade) {
        final World world = location.getWorld();
        if (world == null) {
            return;
        }
        try {
            final Firework firework = (Firework) world.spawnEntity(location, EntityType.FIREWORK_ROCKET);
            final FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.STAR)
                    .withColor(primary)
                    .withFade(fade)
                    .withTrail()
                    .build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        } catch (Exception exception) {
            logger.warning("Failed to spawn firework for beacon availability event: " + exception.getMessage());
        }
    }
}
