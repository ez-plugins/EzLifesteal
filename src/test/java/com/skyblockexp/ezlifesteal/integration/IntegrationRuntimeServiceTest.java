package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.listener.SeasonsServiceListener;
import com.skyblockexp.ezlifesteal.listener.VaultServiceListener;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import com.skyblockexp.ezlifesteal.util.PluginLifecycleSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.bytebuddy.ByteBuddy;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegrationRuntimeServiceTest {

    private EzLifestealPlugin plugin;

    private PluginAccessor pluginAccessor;

    private DefaultPluginRuntimeServices runtime;

    private Registry registry;

    private IntegrationRuntimeService service;

    private Logger logger;


    private PluginManager pluginManager;

    private ServicesManager servicesManager;

    @BeforeEach

    void setUp() {
        plugin = mock(EzLifestealPlugin.class);
        pluginAccessor = mock(PluginAccessor.class);
        runtime = mock(DefaultPluginRuntimeServices.class);
        registry = new Registry();
        service = new IntegrationRuntimeService(plugin, pluginAccessor, runtime, registry);
        logger = mock(Logger.class);

        pluginManager = mock(PluginManager.class);
        servicesManager = mock(ServicesManager.class);

        when(plugin.getLogger()).thenReturn(logger);
    }

    @Test
    void startRegistersListenersRefreshesVaultAttemptsSeasonResetThenHooks() {
        var integrationState = registry.getSeasonsIntegrationState();
        integrationState.setApiClass(SuccessfulSeasonsApi.class);

        when(runtime.ensureSeasonsClasses()).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
            when(pluginManager.getPlugin("EzSeasons")).thenReturn(null);

            service.start();

            InOrder order = inOrder(pluginManager, runtime);
            order.verify(pluginManager, times(2)).registerEvents(any(Listener.class), eq(plugin));
            order.verify(runtime).setupVault();
            order.verify(pluginManager).getPlugin("EzSeasons");
            order.verify(runtime).ensureSeasonsClasses();

            assertNotNull(registry.getVaultIntegrationState().getServiceListener());
            assertNotNull(registry.getSeasonsIntegrationState().getServiceListener());
            assertNull(registry.getSeasonsIntegrationState().getSeasonResetListener());
        }
    }

    @Test
    void reloadRefreshesVaultAndRehooksWithoutReRegisteringListeners() {
        registry.getSeasonsIntegrationState().setApiClass(SuccessfulSeasonsApi.class);
        when(runtime.ensureSeasonsClasses()).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);

            service.reload();

            verify(runtime).setupVault();
            verify(runtime).ensureSeasonsClasses();
            verify(pluginManager, never()).registerEvents(any(Listener.class), eq(plugin));
        }
    }

    @Test
    void stopUnhooksAndUnregistersListenersClearingReferences() {
        var seasonsState = registry.getSeasonsIntegrationState();
        var vaultState = registry.getVaultIntegrationState();

        VaultServiceListener vaultListener = new VaultServiceListener(() -> true, () -> { });
        SeasonsServiceListener seasonsListener = new SeasonsServiceListener(plugin, () -> { }, () -> { });
        SeasonResetListener resetListener = new SeasonResetListener(pluginAccessor);

        vaultState.setServiceListener(vaultListener);
        seasonsState.setServiceListener(seasonsListener);
        registry.setVaultServiceListener(vaultListener);
        registry.setSeasonsServiceListener(seasonsListener);

        seasonsState.setApiClass(SuccessfulSeasonsApi.class);
        seasonsState.setIntegrationClass(FakeIntegration.class);
        seasonsState.setApiInstance(new SuccessfulSeasonsApi());
        seasonsState.setIntegrationProxy(mock(FakeIntegration.class));
        seasonsState.setSeasonResetListener(resetListener);

        try (MockedStatic<PluginLifecycleSupport> lifecycle = mockStatic(PluginLifecycleSupport.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            service.stop();

            lifecycle.verify(() -> PluginLifecycleSupport.unregisterListener(vaultListener));
            lifecycle.verify(() -> PluginLifecycleSupport.unregisterListener(seasonsListener));

            assertNull(vaultState.getServiceListener());
            assertNull(registry.getVaultServiceListener());
            assertNull(seasonsState.getServiceListener());
            assertNull(registry.getSeasonsServiceListener());
            assertNull(seasonsState.getSeasonResetListener());
            assertNull(seasonsState.getApiInstance());
            assertNull(seasonsState.getIntegrationProxy());
        }
    }

    @Test
    void hookIntoEzSeasonsReturnsEarlyWhenIntegrationProxyAlreadySet() {
        var state = registry.getSeasonsIntegrationState();
        state.setIntegrationProxy(new Object());

        service.hookIntoEzSeasons();

        verify(runtime, never()).ensureSeasonsClasses();
    }

    @Test
    void hookIntoEzSeasonsReturnsEarlyWhenApiNotFound() {
        var state = registry.getSeasonsIntegrationState();
        state.setApiClass(SuccessfulSeasonsApi.class);
        when(runtime.ensureSeasonsClasses()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
            when(servicesManager.getRegistration(SuccessfulSeasonsApi.class)).thenReturn(null);

            service.hookIntoEzSeasons();

            assertNull(state.getApiInstance());
            assertNull(state.getIntegrationProxy());
            verify(logger, never()).warning(anyString());
        }
    }

    @Test
    void hookIntoEzSeasonsReturnsEarlyWhenEnsureSeasonsClassesIsFalse() {
        var state = registry.getSeasonsIntegrationState();
        state.setApiClass(SuccessfulSeasonsApi.class);
        when(runtime.ensureSeasonsClasses()).thenReturn(false);

        service.hookIntoEzSeasons();

        verify(runtime).ensureSeasonsClasses();
        assertNull(state.getApiInstance());
        assertNull(state.getIntegrationProxy());
    }

    @Test
    void hookIntoEzSeasonsLogsWarningWhenProxyCreationFails() {
        var state = registry.getSeasonsIntegrationState();
        state.setApiClass(SuccessfulSeasonsApi.class);
        state.setIntegrationClass(null);
        state.setProfileClass(null);

        var provider = mock(RegisteredServiceProvider.class);
        SuccessfulSeasonsApi api = new SuccessfulSeasonsApi();

        when(runtime.ensureSeasonsClasses()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
            when(servicesManager.getRegistration(SuccessfulSeasonsApi.class)).thenReturn(provider);
            when(provider.getProvider()).thenReturn(api);

            service.hookIntoEzSeasons();

            verify(logger).warning("Failed to create EzSeasons integration proxy.");
            assertSame(api, state.getApiInstance());
            assertNull(state.getIntegrationProxy());
        }
    }

    @Test
    void unhookFromEzSeasonsClearsStateEvenWhenReflectiveUnregisterFails() {
        var state = registry.getSeasonsIntegrationState();
        state.setApiClass(NoUnregisterApi.class);
        state.setIntegrationClass(FakeIntegration.class);
        state.setApiInstance(new NoUnregisterApi());
        state.setIntegrationProxy(mock(FakeIntegration.class));

        service.unhookFromEzSeasons();

        verify(logger).log(eq(Level.WARNING), eq("Failed to unregister EzSeasons integration cleanly."),
                any(Throwable.class));
        assertNull(state.getApiInstance());
        assertNull(state.getIntegrationProxy());
    }

    @Test
    void registerSeasonResetListenerSkipsWhenResetEventClassUnavailable() throws Exception {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.getPlugin("EzSeasons")).thenReturn(null);

            invokeRegisterSeasonResetListener();

            assertNull(registry.getSeasonsIntegrationState().getSeasonResetListener());
            verify(pluginManager, never()).registerEvents(any(Listener.class), eq(plugin));
        }
    }

    @Test
    void registerSeasonResetListenerRegistersOnceWhenResetEventClassExists() throws Exception {
        Plugin seasonsPlugin = createPluginLoadedByClassLoaderContainingSeasonResetEvent();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.getPlugin("EzSeasons")).thenReturn(seasonsPlugin);

            invokeRegisterSeasonResetListener();
            invokeRegisterSeasonResetListener();

            assertNotNull(registry.getSeasonsIntegrationState().getSeasonResetListener());
            verify(pluginManager, times(1)).registerEvents(any(SeasonResetListener.class), eq(plugin));
            verify(logger, times(1)).info("Registered EzSeasons season reset listener.");
        }
    }

    private void invokeRegisterSeasonResetListener()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = IntegrationRuntimeService.class.getDeclaredMethod("registerSeasonResetListener");
        method.setAccessible(true);
        method.invoke(service);
    }

    private Plugin createPluginLoadedByClassLoaderContainingSeasonResetEvent() {
        DefiningClassLoader childLoader = new DefiningClassLoader(getClass().getClassLoader());
        byte[] bytes = new ByteBuddy()
                .subclass(Object.class)
                .name("com.skyblockexp.lifesteal.seasons.api.events.SeasonResetEvent")
                .make()
                .getBytes();
        Class<?> resetEventClass = childLoader.define("com.skyblockexp.lifesteal.seasons.api.events.SeasonResetEvent",
                bytes);
        assertTrue(resetEventClass.getName().endsWith("SeasonResetEvent"));

        return (Plugin) Proxy.newProxyInstance(
                childLoader,
                new Class[]{Plugin.class},
                (proxy, method, args) -> {
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getReturnType().equals(int.class)) {
                        return 0;
                    }
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    return null;
                }
        );
    }


    static final class DefiningClassLoader extends ClassLoader {
        DefiningClassLoader(ClassLoader parent) {
            super(parent);
        }

        Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length, (ProtectionDomain) null);
        }
    }

    interface FakeIntegration {
    }

    static class FakeProfile {
    }

    static class SuccessfulSeasonsApi {
        public boolean registerIntegration(FakeIntegration integration) {
            return true;
        }

        public void unregisterIntegration(FakeIntegration integration) {
            // no-op
        }
    }

    static class NoUnregisterApi {
        public boolean registerIntegration(FakeIntegration integration) {
            return true;
        }
    }
}
