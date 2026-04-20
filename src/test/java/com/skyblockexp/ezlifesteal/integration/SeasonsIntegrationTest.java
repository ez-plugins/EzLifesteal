package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeasonsIntegrationTest {

    private EzLifestealPlugin plugin;

    private DefaultPluginRuntimeServices runtime;

    private Registry registry;

    private IntegrationRuntimeService service;

    private Logger logger;


    private PluginManager pluginManager;

    private ServicesManager servicesManager;

    @BeforeEach
    void setUp() {
        plugin = mock(EzLifestealPlugin.class);
        runtime = mock(DefaultPluginRuntimeServices.class);
        registry = new Registry();
        service = new IntegrationRuntimeService(plugin, mock(PluginAccessor.class), runtime, registry);
        logger = mock(Logger.class);

        pluginManager = mock(PluginManager.class);
        servicesManager = mock(ServicesManager.class);

        when(plugin.getLogger()).thenReturn(logger);

        var state = registry.getSeasonsIntegrationState();
        state.setApiClass(FakeSeasonsApi.class);
        state.setIntegrationClass(FakeIntegration.class);
        state.setProfileClass(FakeIntegration.Profile.class);
    }

    @Test
    void dependencyDetectedPathInitializesHooksSuccessfully() {
        FakeSeasonsApi api = new FakeSeasonsApi();
        @SuppressWarnings("unchecked")
        RegisteredServiceProvider<FakeSeasonsApi> provider = mock(RegisteredServiceProvider.class);
        when(provider.getProvider()).thenReturn(api);
        when(runtime.ensureSeasonsClasses()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
            when(servicesManager.getRegistration(FakeSeasonsApi.class)).thenReturn(provider);

            service.hookIntoEzSeasons();

            assertNotNull(registry.getSeasonsIntegrationState().getApiInstance());
            assertNotNull(registry.getSeasonsIntegrationState().getIntegrationProxy());
            verify(logger).info("Hooked into EzSeasons. Hearts will reset automatically at season end.");
        }
    }

    @Test
    void dependencyAbsentPathDisablesIntegrationGracefully() {
        when(runtime.ensureSeasonsClasses()).thenReturn(false);

        service.hookIntoEzSeasons();

        assertNull(registry.getSeasonsIntegrationState().getApiInstance());
        assertNull(registry.getSeasonsIntegrationState().getIntegrationProxy());
        verify(logger, never()).warning(anyString());
    }

    @Test
    void reloadRechecksPluginPresenceAndUpdatesState() {
        FakeSeasonsApi api = new FakeSeasonsApi();
        @SuppressWarnings("unchecked")
        RegisteredServiceProvider<FakeSeasonsApi> provider = mock(RegisteredServiceProvider.class);
        when(provider.getProvider()).thenReturn(api);
        when(runtime.ensureSeasonsClasses()).thenReturn(true);

        AtomicBoolean dependencyAvailable = new AtomicBoolean(false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
            when(servicesManager.getRegistration(FakeSeasonsApi.class))
                    .thenAnswer(inv -> dependencyAvailable.get() ? provider : null);

            service.reload();
            assertNull(registry.getSeasonsIntegrationState().getIntegrationProxy());

            dependencyAvailable.set(true);
            service.reload();

            assertNotNull(registry.getSeasonsIntegrationState().getIntegrationProxy());
            verify(runtime, atLeast(2)).ensureSeasonsClasses();
        }
    }

    @Test
    void failuresInHookCallsAreCaughtAndLoggedWithoutCrashingFlow() {
        ThrowingSeasonsApi api = new ThrowingSeasonsApi();
        @SuppressWarnings("unchecked")
        RegisteredServiceProvider<ThrowingSeasonsApi> provider = mock(RegisteredServiceProvider.class);
        when(provider.getProvider()).thenReturn(api);
        when(runtime.ensureSeasonsClasses()).thenReturn(true);

        var state = registry.getSeasonsIntegrationState();
        state.setApiClass(ThrowingSeasonsApi.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
            when(servicesManager.getRegistration(ThrowingSeasonsApi.class)).thenReturn(provider);

            service.hookIntoEzSeasons();

            assertNull(state.getIntegrationProxy());
            verify(logger).log(eq(Level.WARNING), eq("Failed to register EzSeasons integration."),
                    any(Throwable.class));
        }
    }

    @Test
    void hookSupportsLifecycleStyleEzSeasonsApiRegistration() {
        LifecycleSeasonsApi api = new LifecycleSeasonsApi();
        @SuppressWarnings("unchecked")
        RegisteredServiceProvider<LifecycleSeasonsApi> provider = mock(RegisteredServiceProvider.class);
        when(provider.getProvider()).thenReturn(api);
        when(runtime.ensureSeasonsClasses()).thenReturn(true);

        var state = registry.getSeasonsIntegrationState();
        state.setApiClass(LifecycleSeasonsApi.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
            when(servicesManager.getRegistration(LifecycleSeasonsApi.class)).thenReturn(provider);

            service.hookIntoEzSeasons();

            assertNotNull(state.getIntegrationProxy());
            assertInstanceOf(LifecycleIntegration.class, state.getIntegrationProxy());
            verify(logger).info("Hooked into EzSeasons. Hearts will reset automatically at season end.");
        }
    }

    @Test
    void integrationProxyObjectMethodsUseExpectedIdentityBehavior() {
        FakeIntegration integration = createIntegrationProxy();
        FakeIntegration otherIntegration = createIntegrationProxy();

        assertEquals("EzLifestealSeasonsIntegration", integration.toString());
        assertEquals(System.identityHashCode(integration), integration.hashCode());
        assertTrue(integration.equals(integration));
        assertFalse(integration.equals(otherIntegration));
        assertFalse(integration.equals(null));
    }

    @Test
    void resetAllHeartsAsyncReturnsManagerFutureOrFailedFutureWhenManagerMissing() {
        FakeIntegration integration = createIntegrationProxy();

        LifestealManager manager = mock(LifestealManager.class);
        CompletableFuture<Void> managerFuture = CompletableFuture.completedFuture(null);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.resetAllHeartsAsync()).thenReturn(managerFuture);

        assertEquals(managerFuture, integration.resetAllHeartsAsync());

        when(plugin.getLifestealManager()).thenReturn(null);
        CompletableFuture<Void> missingManagerFuture = integration.resetAllHeartsAsync();
        CompletionException exception = assertThrows(CompletionException.class, missingManagerFuture::join);
        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("Lifesteal manager is not ready", exception.getCause().getMessage());
    }

    @Test
    void getLoadedProfileReturnsEmptyWhenManagerMissingAndWrapsPresentProfile() {
        FakeIntegration integration = createIntegrationProxy();
        UUID uniqueId = UUID.randomUUID();
        LifestealManager manager = mock(LifestealManager.class);
        LifestealProfile profile = new LifestealProfile(uniqueId, 9.5);

        when(plugin.getLifestealManager()).thenReturn(null);
        assertTrue(integration.getLoadedProfile(uniqueId).isEmpty());

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getLoadedProfile(uniqueId)).thenReturn(Optional.of(profile));

        Optional<Object> wrappedProfile = integration.getLoadedProfile(uniqueId);
        assertTrue(wrappedProfile.isPresent());
        assertInstanceOf(FakeIntegration.Profile.class, wrappedProfile.get());
    }

    @Test
    void applyHeartsUsesWrappedProfileAndRejectsNonProxyValues() {
        FakeIntegration integration = createIntegrationProxy();
        UUID uniqueId = UUID.randomUUID();
        LifestealManager manager = mock(LifestealManager.class);
        LifestealProfile profile = new LifestealProfile(uniqueId, 6.0);
        Player player = mock(Player.class);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getLoadedProfile(uniqueId)).thenReturn(Optional.of(profile));
        Object wrappedProfile = integration.getLoadedProfile(uniqueId).orElseThrow();

        integration.applyHearts(player, wrappedProfile);
        verify(manager).applyHearts(player, profile);

        assertThrows(IllegalArgumentException.class, () -> integration.applyHearts(player, new Object()));
    }

    @Test
    void sendHeartStatusForwardsWithNumericConversion() {
        FakeIntegration integration = createIntegrationProxy();
        Player player = mock(Player.class);

        integration.sendHeartStatus(player, Integer.valueOf(7));

        verify(plugin).sendHeartStatus(player, 7.0D);
    }

    @Test
    void unsupportedIntegrationMethodThrowsUnsupportedOperationException() {
        FakeIntegration integration = createIntegrationProxy();

        assertThrows(UnsupportedOperationException.class, integration::unsupportedCall);
    }

    @Test
    void profileProxyMethodsExposeHeartsAndObjectSemantics() {
        FakeIntegration integration = createIntegrationProxy();
        LifestealManager manager = mock(LifestealManager.class);
        UUID sharedId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();

        LifestealProfile profile = new LifestealProfile(sharedId, 11.0);
        LifestealProfile sameIdProfile = new LifestealProfile(sharedId, 2.0);
        LifestealProfile differentProfile = new LifestealProfile(otherId, 11.0);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getLoadedProfile(sharedId)).thenReturn(Optional.of(profile), Optional.of(sameIdProfile));
        when(manager.getLoadedProfile(otherId)).thenReturn(Optional.of(differentProfile));

        FakeIntegration.Profile firstProxy =
                (FakeIntegration.Profile) integration.getLoadedProfile(sharedId).orElseThrow();
        FakeIntegration.Profile secondProxySameId =
                (FakeIntegration.Profile) integration.getLoadedProfile(sharedId).orElseThrow();
        FakeIntegration.Profile thirdProxyDifferent =
                (FakeIntegration.Profile) integration.getLoadedProfile(otherId).orElseThrow();

        assertEquals(11.0, firstProxy.getHearts());
        assertEquals("EzLifestealProfileProxy{" + sharedId + "}", firstProxy.toString());
        assertEquals(Objects.hashCode(sharedId), firstProxy.hashCode());
        assertTrue(firstProxy.equals(firstProxy));
        assertTrue(firstProxy.equals(secondProxySameId));
        assertFalse(firstProxy.equals(thirdProxyDifferent));
        assertFalse(firstProxy.equals(new Object()));
        assertFalse(firstProxy.equals(null));
    }

    private FakeIntegration createIntegrationProxy() {
        SeasonsIntegration seasonsIntegration = new SeasonsIntegration(plugin);
        return (FakeIntegration) seasonsIntegration.createProxy(FakeIntegration.class, FakeIntegration.Profile.class);
    }

    interface FakeIntegration {
        CompletableFuture<Void> resetAllHeartsAsync();

        Optional<Object> getLoadedProfile(UUID uniqueId);

        void applyHearts(Player player, Object profile);

        void sendHeartStatus(Player player, Number hearts);

        void unsupportedCall();

        interface Profile {
            double getHearts();
        }
    }

    static class FakeSeasonsApi {
        public boolean registerIntegration(FakeIntegration integration) {
            return true;
        }
    }

    static class ThrowingSeasonsApi {
        public boolean registerIntegration(FakeIntegration integration) {
            throw new IllegalStateException("kaboom");
        }
    }

    interface LifecycleIntegration {
        void onRegister(Object api);

        void onUnregister();
    }

    static class LifecycleSeasonsApi {
        public boolean registerIntegration(LifecycleIntegration integration) {
            integration.onRegister(this);
            return true;
        }
    }
}
