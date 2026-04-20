package com.skyblockexp.ezlifesteal.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberFormatUtilTest {

    @Test
    void formatDoubleOmitsDecimalPlacesForWholeNumbers() {
        assertEquals("10", NumberFormatUtil.formatDouble(10.0));
        assertEquals("0", NumberFormatUtil.formatDouble(0.0));
    }

    @Test
    void formatDoubleUsesTwoDecimalPlacesForFractionalNumbers() {
        assertEquals("10.50", NumberFormatUtil.formatDouble(10.5));
        assertEquals("3.14", NumberFormatUtil.formatDouble(3.14159));
    }

    @Test
    void formatCompactUsesExpectedSuffixes() {
        assertEquals("950", NumberFormatUtil.formatCompact(950));
        assertEquals("1.3k", NumberFormatUtil.formatCompact(1250));
        assertEquals("12.6m", NumberFormatUtil.formatCompact(12_550_000));
        assertEquals("2b", NumberFormatUtil.formatCompact(2_000_000_000D));
        assertEquals("1.5t", NumberFormatUtil.formatCompact(1_500_000_000_000D));
    }
}
