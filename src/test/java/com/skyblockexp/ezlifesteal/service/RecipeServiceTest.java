package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private JavaPlugin plugin;


    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin();

        services = mock(DefaultPluginRuntimeServices.class);
        heartRegistry = mock(HeartRegistry.class);
        registry = new Registry();
        logger = mock(Logger.class);

        when(services.getRegistry()).thenReturn(registry);
        when(services.getLogger()).thenReturn(logger);
        when(services.getHeartRegistry()).thenReturn(heartRegistry);
        when(services.createNamespacedKey(anyString()))
                .thenAnswer(invocation -> new NamespacedKey("ezlifestealtest", invocation.getArgument(0)));
        when(services.getPlugin()).thenReturn(plugin);

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

        recipeService.registerHeartRecipes();

        verify(services, times(1)).addRegisteredHeartRecipe(any(NamespacedKey.class));
    }

    @Test
    void registerHeartRecipesUsesSchedulerAdapterRunForFoliaCompatibility() {
        YamlConfiguration heartsConfig = new YamlConfiguration();
        heartsConfig.set("recipes.basic.type", "shapeless");
        heartsConfig.set("recipes.basic.amount", 1);
        heartsConfig.set("recipes.basic.ingredients", java.util.List.of("DIAMOND"));
        registry.getConfigState().setHeartsConfig(heartsConfig);

        try (MockedStatic<SchedulerAdapter> schedulerAdapter = mockStatic(SchedulerAdapter.class)) {
            schedulerAdapter.when(() -> SchedulerAdapter.run(any(), any())).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });

            recipeService.registerHeartRecipes();

            schedulerAdapter.verify(() -> SchedulerAdapter.run(any(), any()), times(1));
        }
    }
}
