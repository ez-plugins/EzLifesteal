package com.skyblockexp.ezlifesteal.service;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

public final class HeartRecipeRegistrar {

    public boolean register(HeartRecipeSpec spec, NamespacedKey key, ItemStack result) {
        if (spec instanceof ShapedHeartRecipeSpec shapedSpec) {
            final ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(shapedSpec.pattern().toArray(new String[0]));
            shapedSpec.ingredients().forEach(recipe::setIngredient);
            return Bukkit.addRecipe(recipe);
        }

        if (spec instanceof ShapelessHeartRecipeSpec shapelessSpec) {
            final ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            shapelessSpec.ingredients().forEach(recipe::addIngredient);
            return Bukkit.addRecipe(recipe);
        }

        throw new IllegalArgumentException("Unsupported recipe spec type: " + spec.getClass().getName());
    }
}
