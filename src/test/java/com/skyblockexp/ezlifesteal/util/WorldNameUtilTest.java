package com.skyblockexp.ezlifesteal.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldNameUtilTest {

    @Test
    void normalizeWorldNameHandlesNullAndCaseInsensitiveConversion() {
        assertEquals("", WorldNameUtil.normalizeWorldName(null));
        assertEquals("world_nether", WorldNameUtil.normalizeWorldName("WoRLD_NETHER"));
    }

    @Test
    void parseWorldListFiltersBlankNamesAndReturnsNormalizedUniqueValues() {
        Set<String> worlds = WorldNameUtil
                .parseWorldList(Arrays.asList(" world ", "world", "", "WORLD", "world_nether", "   ", null));

        assertEquals(Set.of(" world ", "world", "world_nether"), worlds);
        assertTrue(WorldNameUtil.parseWorldList(null).isEmpty());
        assertTrue(WorldNameUtil.parseWorldList(List.of()).isEmpty());
    }
}
