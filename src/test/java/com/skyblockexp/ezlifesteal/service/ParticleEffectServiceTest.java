package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParticleEffectServiceTest {

    @Test
    void parseColorFallsBackToDefaultForInvalidHex() {
        ParticleEffectService service = new ParticleEffectService(mock(PluginAccessor.class));

        Color color = service.parseColor("not-a-color");

        assertEquals(Color.fromRGB(139, 0, 0), color);
    }

    @Test
    void parseColorParsesRgbHexWithOrWithoutHash() {
        ParticleEffectService service = new ParticleEffectService(mock(PluginAccessor.class));

        assertEquals(Color.fromRGB(255, 170, 17), service.parseColor("#FFAA11"));
        assertEquals(Color.fromRGB(255, 170, 17), service.parseColor("FFAA11"));
    }

    @Test
    void spawnHeartConsumptionEffectsReturnsWhenDisabled() {
        PluginAccessor accessor = mock(PluginAccessor.class);
        LifestealConfigAdapter config = mock(LifestealConfigAdapter.class);
        when(accessor.getLifestealConfigAdapter()).thenReturn(config);
        when(config.getBoolean("heart-consumption-effects.enabled", true)).thenReturn(false);

        ParticleEffectService service = new ParticleEffectService(accessor);

        try (MockedStatic<SchedulerAdapter> schedulerAdapter = Mockito.mockStatic(SchedulerAdapter.class)) {
            service.spawnHeartConsumptionEffects(mock(Player.class), mock(Location.class));
            schedulerAdapter.verifyNoInteractions();
        }
    }

    @Test
    void spawnHeartConsumptionEffectsUsesModernStackingSection() {
        PluginAccessor accessor = buildAccessorWithPlugin();
        LifestealConfigAdapter config = mock(LifestealConfigAdapter.class);
        when(accessor.getLifestealConfigAdapter()).thenReturn(config);

        YamlConfiguration particleConfig = new YamlConfiguration();
        particleConfig.set("heart-consumption-effects.particles.red.type", "DUST");
        particleConfig.set("heart-consumption-effects.particles.red.count", 20);
        particleConfig.set("heart-consumption-effects.particles.red.speed", 0.2);
        particleConfig.set("heart-consumption-effects.particles.red.color", "#112233");

        when(config.getBoolean("heart-consumption-effects.enabled", true)).thenReturn(true);
        when(config.getBoolean("heart-consumption-effects.stacking-particles", true)).thenReturn(true);
        when(config.getInt("heart-consumption-effects.stacking-duration-ticks", 40)).thenReturn(6);
        when(config.getSection("heart-consumption-effects.particles"))
                .thenReturn(particleConfig.getConfigurationSection("heart-consumption-effects.particles"));

        ParticleEffectService service = new ParticleEffectService(accessor);
        Player player = mock(Player.class);
        Location location = mock(Location.class);

        try (MockedStatic<SchedulerAdapter> schedulerAdapter = Mockito.mockStatic(SchedulerAdapter.class)) {
            schedulerAdapter.when(() -> SchedulerAdapter.runLater(any(), any(), anyLong())).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return mock(SchedulerAdapter.TaskHandle.class);
            });

            World world = mock(World.class);
            when(player.getWorld()).thenReturn(world);

            service.spawnHeartConsumptionEffects(player, location);

            schedulerAdapter.verify(() -> SchedulerAdapter.runLater(any(), any(), eq(0L)), times(1));
            schedulerAdapter.verify(() -> SchedulerAdapter.runLater(any(), any(), eq(2L)), times(1));
            schedulerAdapter.verify(() -> SchedulerAdapter.runLater(any(), any(), eq(4L)), times(1));
            verify(world, times(3)).spawnParticle(eq(Particle.DUST), eq(location), eq(2),
                    eq(0.5), eq(0.5), eq(0.5), eq(0.2), any(Particle.DustOptions.class));
        }
    }

    @Test
    void spawnHeartConsumptionEffectsUsesLegacyBurstWhenSectionMissing() {
        PluginAccessor accessor = buildAccessorWithPlugin();
        LifestealConfigAdapter config = mock(LifestealConfigAdapter.class);
        when(accessor.getLifestealConfigAdapter()).thenReturn(config);

        when(config.getBoolean("heart-consumption-effects.enabled", true)).thenReturn(true);
        when(config.getBoolean("heart-consumption-effects.stacking-particles", true)).thenReturn(false);
        when(config.getSection("heart-consumption-effects.particles")).thenReturn(null);
        when(config.getString("heart-consumption-effects.particle-type", "DUST")).thenReturn("INVALID_PARTICLE");
        when(config.getInt("heart-consumption-effects.particle-count", 50)).thenReturn(12);
        when(config.getDouble("heart-consumption-effects.particle-speed", 0.1)).thenReturn(0.3);

        ParticleEffectService service = new ParticleEffectService(accessor);
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        Location location = mock(Location.class);

        try (MockedStatic<SchedulerAdapter> schedulerAdapter = Mockito.mockStatic(SchedulerAdapter.class)) {
            schedulerAdapter.when(() -> SchedulerAdapter.run(any(), any())).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });

            service.spawnHeartConsumptionEffects(player, location);

            schedulerAdapter.verify(() -> SchedulerAdapter.run(any(), any()), times(1));
            verify(world, times(1)).spawnParticle(eq(Particle.DUST), eq(location), eq(12),
                    eq(0.5), eq(0.5), eq(0.5), eq(0.3), any(Particle.DustOptions.class));
        }
    }

    @Test
    void spawnParticleHandlesNonDustAndSwallowsRuntimeFailures() {
        ParticleEffectService service = new ParticleEffectService(mock(PluginAccessor.class));
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        Location location = mock(Location.class);

        service.spawnParticle(player, location, Particle.FLAME, 5, 0.1, Color.RED);
        verify(world, times(1)).spawnParticle(Particle.FLAME, location, 5, 0.5, 0.5, 0.5, 0.1);

        Player throwingPlayer = mock(Player.class);
        when(throwingPlayer.getWorld()).thenThrow(new RuntimeException("boom"));
        service.spawnParticle(throwingPlayer, location, Particle.DUST, 1, 0.1, Color.RED);
        verify(throwingPlayer, times(1)).getWorld();
    }

    private PluginAccessor buildAccessorWithPlugin() {
        PluginAccessor accessor = mock(PluginAccessor.class);
        when(accessor.getPlugin()).thenReturn(mock(JavaPlugin.class));
        return accessor;
    }
}
