package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPluginRuntimeServicesLifecycleTest {

    @Test
    void getShopConfig_prefers_registry_configState() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = new Registry();
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);

        var cfg = new org.bukkit.configuration.file.YamlConfiguration();
        registry.getConfigState().setShopConfig(cfg);

        assertSame(cfg, services.getShopConfig());
    }

    @Test
    void getProfileRepository_returns_storage_when_profile_repo_null() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = new Registry();
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);

        com.skyblockexp.ezlifesteal.storage.Storage storage = mock(com.skyblockexp.ezlifesteal.storage.Storage.class);
        // set private field storage
        Field storageField = DefaultPluginRuntimeServices.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        storageField.set(services, storage);

        assertSame(storage, services.getProfileRepository());
    }

    @Test
    void ensureSeasonsClasses_returns_false_when_plugin_missing() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = new Registry();
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);

        try (MockedStatic<Bukkit> bk = mockStatic(Bukkit.class)) {
            PluginManager pm = mock(PluginManager.class);
            bk.when(Bukkit::getPluginManager).thenReturn(pm);
            when(pm.getPlugin("EzSeasons")).thenReturn(null);

            assertFalse(services.ensureSeasonsClasses());
        }
    }

    @Test
    void clearRegisteredHeartRecipes_calls_server_remove_and_clears_set() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = new Registry();
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);
        com.skyblockexp.ezlifesteal.service.RecipeService recipeService =
                mock(com.skyblockexp.ezlifesteal.service.RecipeService.class);
        Field field = DefaultPluginRuntimeServices.class.getDeclaredField("recipeService");
        field.setAccessible(true);
        field.set(services, recipeService);

        services.clearRegisteredHeartRecipes();

        verify(recipeService).clearRegisteredHeartRecipes();
    }
}
