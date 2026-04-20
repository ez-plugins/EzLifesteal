package com.skyblockexp.ezlifesteal.model;

/**
 * Minimal holder for sanitized heart bounds.
 * @param adjusted the adjusted
 * @param defaultHearts the defaultHearts
 * @param maxHearts the maxHearts
 * @param minHearts the minHearts
 */
public record SanitizedHeartBounds(double minHearts, double defaultHearts, double maxHearts, boolean adjusted) {
}
