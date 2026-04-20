package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeServiceTest {

    private DefaultPluginRuntimeServices services;

    private HeartRegistry heartRegistry;

    private Registry registry;

    private RecipeService recipeService;

    private ServerMock server;

    private Logger logger;


    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();

        services = mock(DefaultPluginRuntimeServices.class);
        heartRegistry = mock(HeartRegistry.class);
        registry = new Registry();
        logger = mock(Logger.class);

        when(services.getRegistry()).thenReturn(registry);
        when(services.getLogger()).thenReturn(logger);
        when(services.getHeartRegistry()).thenReturn(heartRegistry);
        when(services.createNamespacedKey(anyString()))
                .thenAnswer(invocation -> new NamespacedKey("ezlifestealtest", invocation.getArgument(0)));

        Heart heart = mock(Heart.class);
        when(heart.createItemStack()).thenReturn(new org.bukkit.inventory.ItemStack(Material.DIAMOND));
        when(heartRegistry.getById(anyString())).thenReturn(heart);

        recipeService = new RecipeService(services);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void registerHeartRecipesReturnsEarlyWhenConfigOrRecipesSectionMissing() {
        registry.getConfigState().setHeartsConfig(null);
        recipeService.registerHeartRecipes();

        YamlConfiguration configWithoutRecipes = new YamlConfiguration();
        registry.getConfigState().setHeartsConfig(configWithoutRecipes);
        recipeService.registerHeartRecipes();

        verify(services, times(2)).clearRegisteredHeartRecipesInternal();
        verify(heartRegistry, never()).getById(anyString());
    }

    @Test
    void registerHeartRecipesCompletesSuccessfulRegistrationFlow() {
        YamlConfiguration heartsConfig = new YamlConfiguration();
        heartsConfig.set("recipes.basic.type", "shaped");
        heartsConfig.set("recipes.basic.amount", 2);
        heartsConfig.set("recipes.basic.pattern", java.util.List.of("AB"));
        heartsConfig.set("recipes.basic.ingredients.A", "DIAMOND");
        heartsConfig.set("recipes.basic.ingredients.B", "EMERALD");
        registry.getConfigState().setHeartsConfig(heartsConfig);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.addRecipe(any(Recipe.class))).thenReturn(true);

            recipeService.registerHeartRecipes();

            ArgumentCaptor<Recipe> recipeCaptor = ArgumentCaptor.forClass(Recipe.class);
            bukkit.verify(() -> Bukkit.addRecipe(recipeCaptor.capture()), times(1));
            ShapedRecipe shapedRecipe = assertInstanceOf(ShapedRecipe.class, recipeCaptor.getValue());
            assertEquals(java.util.List.of("AB"), java.util.List.of(shapedRecipe.getShape()));
            assertEquals(Material.DIAMOND, shapedRecipe.getIngredientMap().get('A').getType());
            assertEquals(Material.EMERALD, shapedRecipe.getIngredientMap().get('B').getType());
            assertEquals(2, shapedRecipe.getResult().getAmount());
        }

        verify(services, times(1)).addRegisteredHeartRecipe(any(NamespacedKey.class));
    }
}
