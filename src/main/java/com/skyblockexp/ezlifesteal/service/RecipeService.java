package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.util.logging.Level;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class RecipeService {

    private final DefaultPluginRuntimeServices services;

    private final HeartRecipeParser parser;

    private final HeartRecipeRegistrar registrar;


    public RecipeService(DefaultPluginRuntimeServices services) {
        this(services, new HeartRecipeParser(), new HeartRecipeRegistrar());
    }

    RecipeService(DefaultPluginRuntimeServices services, HeartRecipeParser parser, HeartRecipeRegistrar registrar) {
        this.services = services;
        this.parser = parser;
        this.registrar = registrar;
    }

    public void registerHeartRecipes() {
        services.clearRegisteredHeartRecipesInternal();

        final YamlConfiguration heartsConfig = services.getRegistry().getConfigState().getHeartsConfig();
        if (heartsConfig == null) {
            return;
        }

        final ConfigurationSection recipesSection = heartsConfig.getConfigurationSection("recipes");
        if (recipesSection == null) {
            return;
        }

        for (String heartId : recipesSection.getKeys(false)) {
            final ConfigurationSection recipeSection = recipesSection.getConfigurationSection(heartId);
            if (recipeSection == null) {
                continue;
            }

            final Heart heart = services.getHeartRegistry().getById(heartId);
            if (heart == null) {
                services.getLogger().warning("Skipping recipe for unknown heart id '" + heartId + "'.");
                continue;
            }

            try {
                final HeartRecipeSpec spec = parser.parse(heartId, recipeSection);
                final ItemStack result = heart.createItemStack();
                result.setAmount(spec.amount());
                final NamespacedKey key = services.createNamespacedKey("heart_" + heartId);

                if (registrar.register(spec, key, result)) {
                    services.addRegisteredHeartRecipe(key);
                    services.getLogger().info("Registered " + spec.type() + " heart recipe for '" + heartId + "'.");
                }
                else {
                    services.getLogger().warning("Bukkit rejected " + spec.type() + " recipe for '" + heartId + "'.");
                }
            }
            catch (HeartRecipeParseException exception) {
                services.getLogger().warning(exception.getMessage());
            }
            catch (Throwable throwable) {
                services.getLogger().log(Level.WARNING, "Failed to register heart recipe '" + heartId + "'.",
                        throwable);
            }
        }
    }

    public void clearRegisteredHeartRecipes() {
        services.clearRegisteredHeartRecipesInternal();
    }

    public boolean isHeartRecipesEnabled() {
        return services.isHeartRecipesEnabledInternal();
    }
}
