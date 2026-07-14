package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.Plugin;

public final class HeartRecipeRegistrar {

    public void register(HeartRecipeSpec spec, NamespacedKey key, ItemStack result, Plugin plugin) {
        SchedulerAdapter.run(plugin, () -> {
            try {
                if (spec instanceof ShapedHeartRecipeSpec shapedSpec) {
                    final ShapedRecipe recipe = new ShapedRecipe(key, result);
                    recipe.shape(shapedSpec.pattern().toArray(new String[0]));
                    shapedSpec.ingredients().forEach(recipe::setIngredient);
                    Bukkit.addRecipe(recipe);
                }
                else if (spec instanceof ShapelessHeartRecipeSpec shapelessSpec) {
                    final ShapelessRecipe recipe = new ShapelessRecipe(key, result);
                    shapelessSpec.ingredients().forEach(recipe::addIngredient);
                    Bukkit.addRecipe(recipe);
                }
            }
            catch (Throwable t) {
                plugin.getLogger().warning("Failed to register heart recipe: " + t.getMessage());
            }
        });
    }

    public void cancelRegistration(NamespacedKey key, Plugin plugin) {
        SchedulerAdapter.run(plugin, () -> Bukkit.removeRecipe(key));
    }
}