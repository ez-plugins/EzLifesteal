package com.skyblockexp.ezlifesteal.service;

/**
 * Sanitizes configured or persisted heart values to a finite value within expected bounds.
 */
public final class HeartValueSanitizer {

    private HeartValueSanitizer() {
    }

    public static double sanitize(double rawHearts, double minHearts, double defaultHearts, double maxHearts) {
        final double effectiveMin = Double.isFinite(minHearts) ? minHearts : 0.0D;
        double effectiveMax = Double.isFinite(maxHearts) ? maxHearts : effectiveMin;
        if (effectiveMax < effectiveMin) {
            effectiveMax = effectiveMin;
        }

        double effectiveDefault = Double.isFinite(defaultHearts) ? defaultHearts : effectiveMin;
        effectiveDefault = clamp(effectiveDefault, effectiveMin, effectiveMax);

        if (!Double.isFinite(rawHearts)) {
            return effectiveDefault;
        }
        return clamp(rawHearts, effectiveMin, effectiveMax);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
