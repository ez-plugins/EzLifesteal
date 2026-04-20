package com.skyblockexp.ezlifesteal.service;

import java.util.List;
import org.bukkit.Material;

public record ShapelessHeartRecipeSpec(
        String heartId,
        int amount,
        List<Material> ingredients
) implements HeartRecipeSpec {

    @Override
    public String type() {
        return "shapeless";
    }
}
