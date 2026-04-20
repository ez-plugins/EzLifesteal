package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.overlay.HeartOverlayManager;
import com.skyblockexp.ezlifesteal.placeholder.PlaceholderHook;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.storage.provider.StorageProvider;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DefaultPluginRuntimeServicesLifecycleTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void initializeCoreStateSetsHeartKeyConfigLoaderAndPlayerLookupService() {
        EzLifestealPlugin plugin = MockBukkit.load(EzLifestealPlugin.class);
        Registry registry = new Registry();
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);

        services.initializeCoreState();

        assertNotNull(EzLifestealPlugin.HEART_KEY);
        assertEquals(new NamespacedKey(plugin, "heart_id"), EzLifestealPlugin.HEART_KEY);
        assertNotNull(registry.getConfigState().getConfigLoader());
        assertNotNull(registry.getManagerState().getPlayerLookupService());
        assertSame(registry.getManagerState().getPlayerLookupService(), registry.getPlayerLookupService());
    }

    @Test
    void initializeStorageExecutorReplacesPreviousExecutorAndUsesDaemonNamedThread() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        ExecutorService previousExecutor = mock(ExecutorService.class);
        setField(services, "storageService", null);
        setField(services, "storageExecutor", previousExecutor);

        services.initializeStorageExecutor();

        verify(previousExecutor).shutdownNow();
        ExecutorService newExecutor = (ExecutorService) getField(services, "storageExecutor");
        assertNotNull(newExecutor);

        AtomicReference<Thread> threadRef = new AtomicReference<>();
        newExecutor.submit(() -> threadRef.set(Thread.currentThread())).get();
        Thread createdThread = threadRef.get();
        assertNotNull(createdThread);
        assertEquals("EzLifesteal-Storage", createdThread.getName());
        assertTrue(createdThread.isDaemon());
        newExecutor.shutdownNow();
    }

    @Test
    void closeStorageClosesAndClearsStorageReferences() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();
        com.skyblockexp.ezlifesteal.storage.Storage storage = mock(com.skyblockexp.ezlifesteal.storage.Storage.class);
        StorageProvider provider = mock(StorageProvider.class);

        setField(services, "storageService", null);
        setField(services, "storage", storage);
        setField(services, "storageProvider", provider);
        setField(services, "profileRepository", provider.profiles());
        setField(services, "banRepository", provider.bans());

        services.closeStorage();

        verify(storage).close();
        assertNull(getField(services, "storage"));
        assertNull(getField(services, "storageProvider"));
        assertNull(getField(services, "profileRepository"));
        assertNull(getField(services, "banRepository"));
    }

    @Test
    void shutdownManagersHandlesNullAndNonNullComponents() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();

        services.shutdownManagers();

        LifestealManager lifestealManager = mock(LifestealManager.class);
        HeartOverlayManager overlayManager = mock(HeartOverlayManager.class);
        TopHologramManager hologramManager = mock(TopHologramManager.class);
        PlaceholderHook placeholderHook = mock(PlaceholderHook.class);
        com.skyblockexp.ezlifesteal.killstreak.KillStreakManager killStreakManager =
                mock(com.skyblockexp.ezlifesteal.killstreak.KillStreakManager.class);

        services.getRegistry().getManagerState().setLifestealManager(lifestealManager);
        services.getRegistry().getManagerState().setHeartOverlayManager(overlayManager);
        services.getRegistry().getManagerState().setTopHologramManager(hologramManager);
        services.getRegistry().getManagerState().setPlaceholderExpansion(placeholderHook);
        services.getRegistry().getManagerState().setKillStreakManager(killStreakManager);
        setField(services, "placeholderExpansion", placeholderHook);

        services.shutdownManagers();

        verify(lifestealManager).saveAllSync();
        verify(overlayManager).shutdown();
        verify(hologramManager).shutdown();
        verify(placeholderHook).unregisterExpansion();
        verify(killStreakManager).clear();
        assertNull(getField(services, "placeholderExpansion"));
    }

    @Test
    void reloadManagerStateSavesStateRebuildsManagersAndClearsPlaceholderCache() throws Exception {
        DefaultPluginRuntimeServices services = runtimeWithBasicPlugin();

        LifestealManager oldManager = mock(LifestealManager.class);
        PlaceholderHook placeholderHook = mock(PlaceholderHook.class);
        HeartOverlayManager overlayManager = mock(HeartOverlayManager.class);
        TopHologramManager hologramManager = mock(TopHologramManager.class);
        com.skyblockexp.ezlifesteal.killstreak.KillStreakManager killStreakManager =
                mock(com.skyblockexp.ezlifesteal.killstreak.KillStreakManager.class);

        services.getRegistry().getManagerState().setLifestealManager(oldManager);
        setField(services, "lifestealManager", oldManager);
        services.getRegistry().getManagerState().setPlaceholderExpansion(placeholderHook);
        services.getRegistry().getManagerState().setHeartOverlayManager(overlayManager);
        services.getRegistry().getManagerState().setTopHologramManager(hologramManager);
        services.getRegistry().getManagerState().setKillStreakManager(killStreakManager);
        setField(services, "placeholderExpansion", placeholderHook);
        setField(services, "heartsConfig", new YamlConfiguration());

        setField(services, "adminConfigAdapter",
                new com.skyblockexp.ezlifesteal.config.AdminConfigAdapter(new YamlConfiguration(),
                        new YamlConfiguration()));
        setField(services, "lifestealConfigAdapter",
                new com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter(new YamlConfiguration(),
                        new YamlConfiguration()));
        setField(services, "smurfConfigAdapter",
                new com.skyblockexp.ezlifesteal.config.SmurfConfigAdapter(new YamlConfiguration(),
                        new YamlConfiguration()));
        setField(services, "messageService", mock(com.skyblockexp.ezlifesteal.config.MessageService.class));
        setField(services, "profileRepository",
                mock(com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository.class));

        services.reloadManagerState();

        verify(oldManager).saveAllSync();
        verify(placeholderHook).clearCache();

        assertNotNull(getField(services, "heartRegistry"));
        assertNotNull(getField(services, "lifestealManager"));
        assertNotNull(services.getRegistry().getManagerState().getHeartOverlayManager());
        assertNotNull(services.getRegistry().getManagerState().getTopHologramManager());
    }

    private DefaultPluginRuntimeServices runtimeWithBasicPlugin() {
        EzLifestealPlugin plugin = MockBukkit.load(EzLifestealPlugin.class);
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());
        services.initializeCoreState();
        return services;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
