package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.ReviveAnimationSettings;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ReviveAnimationService {
    private final PluginAccessor plugin;

    public ReviveAnimationService(PluginAccessor plugin) {
        this.plugin = plugin;
    }

    public void playReviveAnimation(Location beaconLocation, Player activator, ReviveAnimationSettings settings) {
        if (beaconLocation == null || activator == null || settings == null || !settings.enabled()) {
            return;
        }
        final World world = beaconLocation.getWorld();
        if (world == null || !activator.isOnline()) {
            return;
        }

        final Location center = beaconLocation.clone().add(0.5D, 1.0D, 0.5D);
        final AnimationStepRunner stepRunner = new AnimationStepRunner(center, activator, settings);
        final SchedulerAdapter.TaskHandle taskHandle = SchedulerAdapter.runTimer(plugin.getPlugin(), stepRunner, 0L, 1L);
        stepRunner.setTaskHandle(taskHandle);
    }

    private final class AnimationStepRunner implements Runnable {
        private final Location center;

        private final Player activator;

        private final ReviveAnimationSettings settings;

        private int step;

        private SchedulerAdapter.TaskHandle taskHandle;

        private AnimationStepRunner(Location center, Player activator, ReviveAnimationSettings settings) {
            this.center = center;
            this.activator = activator;
            this.settings = settings;
        }

        @Override
        public void run() {
            if (!isStillValid()) {
                cancel();
                return;
            }
            if (step >= settings.durationTicks()) {
                playImpact(center.getWorld(), center, activator, settings);
                cancel();
                return;
            }

            final World world = center.getWorld();
            if (world == null) {
                cancel();
                return;
            }
            emitStep(world, center, settings, step);
            step++;
        }

        private boolean isStillValid() {
            if (!activator.isOnline()) {
                return false;
            }
            final World centerWorld = center.getWorld();
            final World playerWorld = activator.getWorld();
            return centerWorld != null && playerWorld != null
                    && centerWorld.getUID().equals(playerWorld.getUID());
        }

        private void cancel() {
            if (taskHandle != null) {
                taskHandle.cancel();
            }
        }

        private void setTaskHandle(SchedulerAdapter.TaskHandle taskHandle) {
            this.taskHandle = taskHandle;
        }

        private void emitStep(World world, Location centerPoint, ReviveAnimationSettings animationSettings,
                int currentStep) {
            final double progress = (double) currentStep / Math.max(1, animationSettings.durationTicks());
            final double verticalOffset = animationSettings.verticalLiftPerStep() * currentStep;
            final double baseRadius = 0.8D + (0.9D * progress);

            for (int ring = 0; ring < animationSettings.ringCount(); ring++) {
                final double ringRadius = baseRadius + (ring * 0.35D);
                final double angle = (currentStep * 0.35D) + ((Math.PI * 2.0D / animationSettings.ringCount()) * ring);
                final Location ringLocation = centerPoint.clone().add(
                        Math.cos(angle) * ringRadius,
                        0.4D + verticalOffset,
                        Math.sin(angle) * ringRadius
                );
                spawnParticle(world, ringLocation, animationSettings.ringParticle());
            }

            for (int index = 0; index < animationSettings.spiralSteps(); index++) {
                final double phase = ((double) index / animationSettings.spiralSteps()) * (Math.PI * 2.0D);
                final double angle = phase + (currentStep * 0.35D);
                final double radius = 0.4D + (progress * 0.7D);
                final Location spiralLocation = centerPoint.clone().add(
                        Math.cos(angle) * radius,
                        0.2D + verticalOffset + (index * 0.02D),
                        Math.sin(angle) * radius
                );
                spawnParticle(world, spiralLocation, animationSettings.spiralParticle());
            }

            playSound(world, centerPoint, animationSettings.loopSound());
        }

        private void spawnParticle(World world, Location location, ReviveAnimationSettings.ParticlePreset preset) {
            final Particle particle = parseParticle(preset.type(), Particle.END_ROD);
            world.spawnParticle(
                    particle,
                    location,
                    preset.count(),
                    preset.offsetX(),
                    preset.offsetY(),
                    preset.offsetZ(),
                    preset.speed()
            );
        }

        private void playImpact(World world, Location centerPoint, Player source,
                ReviveAnimationSettings animationSettings) {
            spawnParticle(world, centerPoint, animationSettings.impactParticle());
            playSound(world, centerPoint, animationSettings.impactSound());
            world.playSound(source.getLocation(), Sound.ITEM_TOTEM_USE, 0.8F, 1.0F);
        }

        private void playSound(World world, Location location, ReviveAnimationSettings.SoundPreset preset) {
            final Sound sound = parseSound(preset.type(), Sound.BLOCK_BEACON_AMBIENT);
            world.playSound(location, sound, preset.volume(), preset.pitch());
        }

        private Particle parseParticle(String raw, Particle fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return Particle.valueOf(raw.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException exception) {
                return fallback;
            }
        }

        private Sound parseSound(String raw, Sound fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            try {
                return Sound.valueOf(raw.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException exception) {
                return fallback;
            }
        }
    }
}