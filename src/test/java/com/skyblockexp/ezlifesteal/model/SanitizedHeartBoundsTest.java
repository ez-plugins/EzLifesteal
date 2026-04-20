package com.skyblockexp.ezlifesteal.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SanitizedHeartBoundsTest {

    @Test
    void recordExposesGivenValues() {
        SanitizedHeartBounds bounds = new SanitizedHeartBounds(5.0, 10.0, 20.0, false);

        assertEquals(5.0, bounds.minHearts());
        assertEquals(10.0, bounds.defaultHearts());
        assertEquals(20.0, bounds.maxHearts());
        assertFalse(bounds.adjusted());
    }
}
