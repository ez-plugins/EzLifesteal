package com.skyblockexp.ezlifesteal.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public final class HeartRecipeParser {

    public HeartRecipeSpec parse(String heartId, ConfigurationSection recipeSection) throws HeartRecipeParseException {
        final String type = recipeSection.getString("type", "shaped").toLowerCase(Locale.ROOT).trim();
        final int amount = Math.max(1, recipeSection.getInt("amount", 1));

        if ("shaped".equals(type)) {
            return parseShaped(heartId, amount, recipeSection);
        }
        if ("shapeless".equals(type)) {
            return parseShapeless(heartId, amount, recipeSection);
        }

        throw new HeartRecipeParseException(
                "Unknown recipe type '" + type + "' for heart '" + heartId + "'; supported: shaped, shapeless."
        );
    }

    private ShapedHeartRecipeSpec parseShaped(String heartId, int amount, ConfigurationSection recipeSection)
            throws HeartRecipeParseException {
        final List<String> pattern = recipeSection.getStringList("pattern");
        if (pattern.isEmpty()) {
            throw new HeartRecipeParseException("Shaped recipe for '" + heartId + "' has no pattern; skipping.");
        }
        if (pattern.size() > 3) {
            throw new HeartRecipeParseException(
                    "Shaped recipe for '" + heartId + "' has more than 3 rows; skipping."
            );
        }

        int width = -1;
        final Set<Character> usedChars = new HashSet<>();
        final List<String> normalizedPattern = new ArrayList<>(pattern.size());
        for (String row : pattern) {
            final String normalizedRow = row == null ? "" : row;
            if (width == -1) {
                width = normalizedRow.length();
            }
            if (normalizedRow.length() != width) {
                throw new HeartRecipeParseException(
                        "Inconsistent pattern row lengths for shaped recipe '" + heartId + "'; skipping."
                );
            }
            for (char c : normalizedRow.toCharArray()) {
                if (c != ' ' && c != 0) {
                    usedChars.add(c);
                }
            }
            normalizedPattern.add(normalizedRow);
        }

        if (width > 3) {
            throw new HeartRecipeParseException(
                    "Shaped recipe for '" + heartId + "' has row width > 3; skipping."
            );
        }

        final ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
        if (ingredientsSection == null) {
            throw new HeartRecipeParseException(
                    "Shaped recipe for '" + heartId + "' is missing 'ingredients' mapping; skipping."
            );
        }

        final Map<Character, Material> mapping = new HashMap<>();
        for (String ingredientKey : ingredientsSection.getKeys(false)) {
            if (ingredientKey == null || ingredientKey.length() != 1) {
                throw new HeartRecipeParseException(
                        "Invalid ingredient key '" + ingredientKey + "' in recipe '" + heartId
                                + "' (must be a single character); skipping recipe."
                );
            }

            final String materialName = ingredientsSection.getString(ingredientKey);
            if (materialName == null) {
                throw new HeartRecipeParseException(
                        "Ingredient '" + ingredientKey + "' for recipe '" + heartId
                                + "' has no material specified; skipping recipe."
                );
            }

            final Material material = Material.matchMaterial(materialName.trim());
            if (material == null) {
                throw new HeartRecipeParseException(
                        "Unknown material '" + materialName + "' for ingredient '" + ingredientKey
                                + "' in recipe '" + heartId + "'; skipping recipe."
                );
            }
            mapping.put(ingredientKey.charAt(0), material);
        }

        for (char used : usedChars) {
            if (!mapping.containsKey(used)) {
                throw new HeartRecipeParseException(
                        "Pattern for recipe '" + heartId + "' uses character '" + used
                                + "' which has no ingredient mapping; skipping recipe."
                );
            }
        }

        return new ShapedHeartRecipeSpec(heartId, amount, normalizedPattern, Map.copyOf(mapping));
    }

    private ShapelessHeartRecipeSpec parseShapeless(String heartId, int amount, ConfigurationSection recipeSection)
            throws HeartRecipeParseException {
        final List<String> ingredients = recipeSection.getStringList("ingredients");
        if (ingredients.isEmpty()) {
            throw new HeartRecipeParseException(
                    "Shapeless recipe for '" + heartId + "' has no ingredients; skipping."
            );
        }

        final List<Material> resolvedIngredients = new ArrayList<>(ingredients.size());
        for (String materialName : ingredients) {
            if (materialName == null) {
                throw new HeartRecipeParseException(
                        "Null ingredient in shapeless recipe for '" + heartId + "'; skipping recipe."
                );
            }

            final Material material = Material.matchMaterial(materialName.trim());
            if (material == null) {
                throw new HeartRecipeParseException(
                        "Unknown material '" + materialName + "' in shapeless recipe for '" + heartId + "'."
                );
            }
            resolvedIngredients.add(material);
        }

        return new ShapelessHeartRecipeSpec(heartId, amount, List.copyOf(resolvedIngredients));
    }
}
