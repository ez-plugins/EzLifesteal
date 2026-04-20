package com.skyblockexp.ezlifesteal.util;

public final class NumberFormatUtil {
    private static final double THOUSAND = 1_000D;

    private static final double MILLION = 1_000_000D;

    private static final double BILLION = 1_000_000_000D;

    private static final double TRILLION = 1_000_000_000_000D;

    private NumberFormatUtil() { }

    public static String formatDouble(double value) {
        return value % 1 == 0 ? Integer.toString((int) value) : String.format(java.util.Locale.US, "%.2f", value);
    }

    public static String formatCompact(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        final double absolute = Math.abs(value);
        if (absolute >= TRILLION) {
            return formatSuffix(value / TRILLION, "t");
        }
        if (absolute >= BILLION) {
            return formatSuffix(value / BILLION, "b");
        }
        if (absolute >= MILLION) {
            return formatSuffix(value / MILLION, "m");
        }
        if (absolute >= THOUSAND) {
            return formatSuffix(value / THOUSAND, "k");
        }
        return formatDouble(value);
    }

    private static String formatSuffix(double value, String suffix) {
        final double rounded = Math.round(value * 10.0D) / 10.0D;
        final String formatted = rounded % 1 == 0 ? Integer.toString((int) rounded) : String.format(java.util.Locale.US,
                "%.1f", rounded);
        return formatted + suffix;
    }
}
