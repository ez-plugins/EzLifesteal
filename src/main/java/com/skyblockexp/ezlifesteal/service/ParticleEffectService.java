package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Spawns heart-consumption particles with modern and legacy config fallbacks.
 */
public class ParticleEffectService {
    private static final org.bukkit.Color DEFAULT_DARK_RED = org.bukkit.Color.fromRGB(139, 0, 0);

    private final PluginAccessor plugin;

    public ParticleEffectService(PluginAccessor plugin) {
        this.plugin = plugin;
    }

    public void spawnHeartConsumptionEffects(Player player, Location location) {
        if (!plugin.getLifestealConfigAdapter().getBoolean("heart-consumption-effects.enabled", true)) {
            return;
        }

        final boolean stacking =
                plugin.getLifestealConfigAdapter().getBoolean("heart-consumption-effects.stacking-particles",
                true);
        final ConfigurationSection particlesSection =
                plugin.getLifestealConfigAdapter().getSection("heart-consumption-effects.particles");
        if (stacking) {
            final int durationTicks =
                    plugin.getLifestealConfigAdapter().getInt("heart-consumption-effects.stacking-duration-ticks",
                    40);
            if (particlesSection != null) {
                for (String key : particlesSection.getKeys(false)) {
                    final ConfigurationSection particleConfig = particlesSection.getConfigurationSection(key);
                    if (particleConfig != null) {
                        spawnParticleStack(player, location, particleConfig, durationTicks);
                    }
                }
            }
            else {
                spawnLegacyParticleStack(player, location, durationTicks);
            }
            return;
        }

        if (particlesSection != null) {
            for (String key : particlesSection.getKeys(false)) {
                final ConfigurationSection particleConfig = particlesSection.getConfigurationSection(key);
                if (particleConfig != null) {
                    spawnParticleBurst(player, location, particleConfig);
                }
            }
        }
        else {
            spawnLegacyParticleBurst(player, location);
        }
    }

    void spawnParticleStack(Player player, Location location, ConfigurationSection particleConfig, int durationTicks) {
        final ParticleSettings settings = loadParticleSettings(particleConfig);
        for (int tick = 0; tick < durationTicks; tick += 2) {
            final int finalTick = tick;
            SchedulerAdapter.runLater(plugin.getPlugin(), () -> spawnParticle(player, location, settings.particleType,
                    settings.particleCount / 10, settings.particleSpeed, settings.color), finalTick);
        }
    }

    void spawnParticleBurst(Player player, Location location, ConfigurationSection particleConfig) {
        final ParticleSettings settings = loadParticleSettings(particleConfig);
        SchedulerAdapter.run(plugin.getPlugin(), () ->
                spawnParticle(player, location, settings.particleType, settings.particleCount, settings.particleSpeed,
                        settings.color));
    }

    void spawnLegacyParticleStack(Player player, Location location, int durationTicks) {
        final ParticleSettings settings = loadLegacyParticleSettings();
        for (int tick = 0; tick < durationTicks; tick += 2) {
            final int finalTick = tick;
            SchedulerAdapter.runLater(plugin.getPlugin(), () -> spawnParticle(player, location, settings.particleType,
                    settings.particleCount / 10, settings.particleSpeed, settings.color), finalTick);
        }
    }

    void spawnLegacyParticleBurst(Player player, Location location) {
        final ParticleSettings settings = loadLegacyParticleSettings();
        SchedulerAdapter.run(plugin.getPlugin(), () ->
                spawnParticle(player, location, settings.particleType, settings.particleCount, settings.particleSpeed,
                        settings.color));
    }

    private ParticleSettings loadParticleSettings(ConfigurationSection particleConfig) {
        return new ParticleSettings(
                getParticleType(particleConfig.getString("type", "DUST")),
                particleConfig.getInt("count", 50),
                particleConfig.getDouble("speed", 0.1),
                parseColor(particleConfig.getString("color", "#8B0000"))
        );
    }

    private ParticleSettings loadLegacyParticleSettings() {
        return new ParticleSettings(
                getParticleType(plugin.getLifestealConfigAdapter()
                        .getString("heart-consumption-effects.particle-type", "DUST")),
                plugin.getLifestealConfigAdapter().getInt("heart-consumption-effects.particle-count", 50),
                plugin.getLifestealConfigAdapter().getDouble("heart-consumption-effects.particle-speed", 0.1),
                DEFAULT_DARK_RED
        );
    }

    void spawnParticle(Player player,
                       Location location,
                       Particle particleType,
                       int particleCount,
                       double particleSpeed,
                       org.bukkit.Color color) {
        try {
            if (particleType == Particle.DUST) {
                player.getWorld().spawnParticle(particleType, location, particleCount,
                        0.5, 0.5, 0.5, particleSpeed, new Particle.DustOptions(color, 1.0f));
            }
            else {
                player.getWorld().spawnParticle(particleType, location, particleCount,
                        0.5, 0.5, 0.5, particleSpeed);
            }
        }
        catch (Exception ignored) {
        }
    }

    org.bukkit.Color parseColor(String hexColor) {
        try {
            String normalized = hexColor;
            if (normalized.startsWith("#")) {
                normalized = normalized.substring(1);
            }
            if (normalized.length() == 6) {
                final int r = Integer.parseInt(normalized.substring(0, 2), 16);
                final int g = Integer.parseInt(normalized.substring(2, 4), 16);
                final int b = Integer.parseInt(normalized.substring(4, 6), 16);
                return org.bukkit.Color.fromRGB(r, g, b);
            }
        }
        catch (Exception ignored) {
        }
        return DEFAULT_DARK_RED;
    }

    private Particle getParticleType(String typeName) {
        try {
            return Particle.valueOf(typeName);
        }
        catch (IllegalArgumentException exception) {
            return Particle.DUST;
        }
    }

    private record ParticleSettings(Particle particleType, int particleCount, double particleSpeed,
            org.bukkit.Color color) {
    }
}
