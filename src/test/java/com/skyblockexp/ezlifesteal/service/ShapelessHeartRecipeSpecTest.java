package com.skyblockexp.ezlifesteal.service;

import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ShapelessHeartRecipeSpecTest {

    @Test
    void typeReturnsShapeless() {
        ShapelessHeartRecipeSpec spec = new ShapelessHeartRecipeSpec("basic", 2, List.of(Material.REDSTONE));

        assertEquals("shapeless", spec.type());
    }
}
