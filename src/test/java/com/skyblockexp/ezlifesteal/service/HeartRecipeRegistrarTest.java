package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeartRecipeRegistrarTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void registerAddsShapedRecipe() {
        HeartRecipeRegistrar registrar = new HeartRecipeRegistrar();
        Plugin plugin = mock(Plugin.class);
        Logger logger = mock(Logger.class);
        NamespacedKey key = NamespacedKey.minecraft("heart_recipe_shaped_test");
        ItemStack result = new ItemStack(Material.STICK);
        ShapedHeartRecipeSpec spec = new ShapedHeartRecipeSpec(
                "revive",
                1,
                List.of("ABC", "DEF", "GHI"),
                Map.of('A', Material.STICK, 'B', Material.APPLE)
        );

        when(plugin.getLogger()).thenReturn(logger);

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            scheduler.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });

            registrar.register(spec, key, result, plugin);
        }

        verify(logger, never()).warning(contains("Failed to register heart recipe"));
    }

    @Test
    void registerAddsShapelessRecipe() {
        HeartRecipeRegistrar registrar = new HeartRecipeRegistrar();
        Plugin plugin = mock(Plugin.class);
        Logger logger = mock(Logger.class);
        NamespacedKey key = NamespacedKey.minecraft("heart_recipe_shapeless_test");
        ItemStack result = new ItemStack(Material.STICK);
        ShapelessHeartRecipeSpec spec = new ShapelessHeartRecipeSpec(
                "revive",
                1,
            List.of(Material.STICK, Material.APPLE)
        );

        when(plugin.getLogger()).thenReturn(logger);

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            scheduler.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });

            registrar.register(spec, key, result, plugin);
        }

        verify(logger, never()).warning(contains("Failed to register heart recipe"));
    }

    @Test
    void registerLogsWarningWhenRecipeRegistrationFails() {
        HeartRecipeRegistrar registrar = new HeartRecipeRegistrar();
        Plugin plugin = mock(Plugin.class);
        Logger logger = mock(Logger.class);
        NamespacedKey key = NamespacedKey.minecraft("heart_recipe_failure_test");
        ItemStack result = new ItemStack(Material.STICK);
        ShapelessHeartRecipeSpec spec = new ShapelessHeartRecipeSpec(
                "revive",
                1,
            List.of(Material.STICK)
        );

        when(plugin.getLogger()).thenReturn(logger);

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            scheduler.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });
            bukkit.when(() -> Bukkit.addRecipe(any(org.bukkit.inventory.ShapelessRecipe.class)))
                    .thenThrow(new RuntimeException("boom"));

            registrar.register(spec, key, result, plugin);
        }

        verify(logger).warning(org.mockito.Mockito.contains("Failed to register heart recipe"));
    }

    @Test
    void cancelRegistrationRemovesRecipe() {
        HeartRecipeRegistrar registrar = new HeartRecipeRegistrar();
        Plugin plugin = mock(Plugin.class);
        NamespacedKey key = NamespacedKey.minecraft("heart_recipe_cancel_test");

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            scheduler.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });

            registrar.cancelRegistration(key, plugin);

            bukkit.verify(() -> Bukkit.removeRecipe(key));
        }
    }
}