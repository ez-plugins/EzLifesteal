package com.skyblockexp.ezlifesteal.service;

import java.util.List;
import java.util.Map;
import org.bukkit.Material;

public record ShapedHeartRecipeSpec(
        String heartId,
        int amount,
        List<String> pattern,
        Map<Character, Material> ingredients
) implements HeartRecipeSpec {

    @Override
    public String type() {
        return "shaped";
    }
}
