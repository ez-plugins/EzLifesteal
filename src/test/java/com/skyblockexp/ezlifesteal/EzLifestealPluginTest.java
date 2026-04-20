package com.skyblockexp.ezlifesteal;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EzLifestealPluginTest {

    @Test
    void sanitizeHeartBoundsKeepsValidValues() {
        com.skyblockexp.ezlifesteal.EzLifestealPlugin.SanitizedHeartBounds bounds =
                com.skyblockexp.ezlifesteal.EzLifestealPlugin.sanitizeHeartBounds(5.0,
                7.0, 15.0);

        assertEquals(5.0, bounds.minHearts());
        assertEquals(7.0, bounds.defaultHearts());
        assertEquals(15.0, bounds.maxHearts());
        assertFalse(bounds.adjusted());
    }

    @Test
    void sanitizeHeartBoundsClampsAndOrdersValues() {
        com.skyblockexp.ezlifesteal.EzLifestealPlugin.SanitizedHeartBounds bounds =
                com.skyblockexp.ezlifesteal.EzLifestealPlugin.sanitizeHeartBounds(0.0,
                50.0, -5.0);

        assertEquals(0.0, bounds.minHearts());
        assertEquals(0.0, bounds.defaultHearts());
        assertEquals(0.0, bounds.maxHearts());
        assertTrue(bounds.adjusted());
    }

    @Test
    void sanitizeHeartBoundsClampsDefaultWithinBounds() {
        com.skyblockexp.ezlifesteal.EzLifestealPlugin.SanitizedHeartBounds bounds =
                com.skyblockexp.ezlifesteal.EzLifestealPlugin.sanitizeHeartBounds(3.0,
                1.5, 4.0);

        assertEquals(3.0, bounds.minHearts());
        assertEquals(3.0, bounds.defaultHearts());
        assertEquals(4.0, bounds.maxHearts());
        assertTrue(bounds.adjusted());
    }
}
