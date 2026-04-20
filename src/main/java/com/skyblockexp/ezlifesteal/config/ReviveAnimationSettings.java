package com.skyblockexp.ezlifesteal.config;

/**
 * Runtime-safe revive animation settings parsed from lifesteal configuration.
 * @param durationTicks the durationTicks
 * @param enabled the enabled
 * @param impactParticle the impactParticle
 * @param impactSound the impactSound
 * @param loopSound the loopSound
 * @param ringCount the ringCount
 * @param ringParticle the ringParticle
 * @param spiralParticle the spiralParticle
 * @param spiralSteps the spiralSteps
 * @param verticalLiftPerStep the verticalLiftPerStep
 */
public record ReviveAnimationSettings(
        boolean enabled,
        int durationTicks,
        int spiralSteps,
        int ringCount,
        double verticalLiftPerStep,
        ParticlePreset spiralParticle,
        ParticlePreset ringParticle,
        ParticlePreset impactParticle,
        SoundPreset loopSound,
        SoundPreset impactSound
) {

    public static ReviveAnimationSettings defaults() {
        return new ReviveAnimationSettings(
                true,
                30,
                10,
                3,
                0.08D,
                new ParticlePreset("END_ROD", 2, 0.0D, 0.0D, 0.0D, 0.0D),
                new ParticlePreset("ENCHANT", 2, 0.03D, 0.03D, 0.03D, 0.01D),
                new ParticlePreset("TOTEM_OF_UNDYING", 20, 0.4D, 0.6D, 0.4D, 0.01D),
                new SoundPreset("BLOCK_BEACON_AMBIENT", 0.30F, 1.20F),
                new SoundPreset("BLOCK_BEACON_ACTIVATE", 1.0F, 1.15F)
        );
    }

    public record ParticlePreset(
            String type,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed
    ) {
    }

    public record SoundPreset(
            String type,
            float volume,
            float pitch
    ) {
    }
}
