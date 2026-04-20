package com.skyblockexp.ezlifesteal.service;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeartRecipeParserTest {

    private final HeartRecipeParser parser = new HeartRecipeParser();

    @Test
    void parseShapedRecipeFailsWhenIngredientKeyLengthIsInvalid() {
        YamlConfiguration section = new YamlConfiguration();
        section.set("type", "shaped");
        section.set("pattern", List.of("A"));
        section.set("ingredients.AA", "DIAMOND");

        HeartRecipeParseException exception = assertThrows(
                HeartRecipeParseException.class,
                () -> parser.parse("basic", section)
        );

        assertEquals(
                "Invalid ingredient key 'AA' in recipe 'basic' (must be a single character); skipping recipe.",
                exception.getMessage()
        );
    }

    @Test
    void parseShapelessRecipeFailsWhenMaterialIsUnknown() {
        YamlConfiguration section = new YamlConfiguration();
        section.set("type", "shapeless");
        section.set("ingredients", List.of("NOT_A_REAL_BLOCK"));

        HeartRecipeParseException exception = assertThrows(
                HeartRecipeParseException.class,
                () -> parser.parse("basic", section)
        );

        assertEquals(
                "Unknown material 'NOT_A_REAL_BLOCK' in shapeless recipe for 'basic'.",
                exception.getMessage()
        );
    }

    @Test
    void parseShapedRecipeFailsWhenPatternUsesMissingMapping() {
        YamlConfiguration section = new YamlConfiguration();
        section.set("type", "shaped");
        section.set("pattern", List.of("AB"));
        section.set("ingredients.A", "DIAMOND");

        HeartRecipeParseException exception = assertThrows(
                HeartRecipeParseException.class,
                () -> parser.parse("basic", section)
        );

        assertEquals(
                "Pattern for recipe 'basic' uses character 'B' which has no ingredient mapping; skipping recipe.",
                exception.getMessage()
        );
    }

    @Test
    void parseShapedRecipeReturnsTypedSpec() throws HeartRecipeParseException {
        YamlConfiguration section = new YamlConfiguration();
        section.set("type", "shaped");
        section.set("amount", 2);
        section.set("pattern", List.of("AB", "BA"));
        section.set("ingredients.A", "DIAMOND");
        section.set("ingredients.B", "EMERALD");

        HeartRecipeSpec spec = parser.parse("basic", section);

        ShapedHeartRecipeSpec shaped = assertInstanceOf(ShapedHeartRecipeSpec.class, spec);
        assertEquals(2, shaped.amount());
        assertEquals(List.of("AB", "BA"), shaped.pattern());
        assertEquals(Material.DIAMOND, shaped.ingredients().get('A'));
        assertEquals(Material.EMERALD, shaped.ingredients().get('B'));
    }
}
