package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import java.lang.reflect.Proxy;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.bytebuddy.ByteBuddy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultPluginRuntimeServicesSeasonsLoadingTest {

    @Test
    void ensureSeasonsClassesPrefersModernApiAndIntegrationWhenBothAreAvailable() {
        RuntimeHarness harness = runtimeHarness();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(harness.pluginManager);
            when(harness.pluginManager.getPlugin("EzSeasons"))
                    .thenReturn(seasonsPluginWithLoader(getClass().getClassLoader()));

            boolean loaded = harness.services.ensureSeasonsClasses();

            assertTrue(loaded);
            assertEquals("com.skyblockexp.lifesteal.seasons.api.SeasonsApi",
                    harness.registry.getSeasonsIntegrationState().getApiClass().getName());
            assertEquals("com.skyblockexp.lifesteal.seasons.integration.LifestealIntegration",
                    harness.registry.getSeasonsIntegrationState().getIntegrationClass().getName());
            assertEquals("Profile",
                    harness.registry.getSeasonsIntegrationState().getProfileClass().getSimpleName());
        }
    }

    @Test
    void ensureSeasonsClassesFallsBackToLegacyWhenModernClassesAreUnavailable() {
        RuntimeHarness harness = runtimeHarness();
        // Build an isolated classloader that does NOT see any modern EzSeasons API classes
        // but DOES have legacy-style classes, simulating an old EzSeasons plugin installation.
        ClassLoader loader = buildLegacyEzSeasonsClassLoader();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(harness.pluginManager);
            when(harness.pluginManager.getPlugin("EzSeasons"))
                    .thenReturn(seasonsPluginWithLoader(loader));

            boolean loaded = harness.services.ensureSeasonsClasses();

            assertTrue(loaded);
            assertEquals("com.ezlifesteal.seasons.api.SeasonsApi",
                    harness.registry.getSeasonsIntegrationState().getApiClass().getName());
            assertEquals("com.skyblockexp.lifesteal.seasons.api.SeasonsIntegration",
                    harness.registry.getSeasonsIntegrationState().getIntegrationClass().getName());
            String profileName = harness.registry.getSeasonsIntegrationState().getProfileClass().getName();
            assertTrue(profileName.endsWith("$Profile"),
                    "Expected profile class name to end with $Profile, was: " + profileName);
        }
    }

    @Test
    void ensureSeasonsClassesLeavesIntegrationDisabledWhenNoCandidatesExist() {
        RuntimeHarness harness = runtimeHarness();
        ClassLoader loader = new FilteringClassLoader(getClass().getClassLoader(), Set.of(
                "com.skyblockexp.lifesteal.seasons.api.SeasonsApi",
                "com.skyblockexp.lifesteal.seasons.integration.LifestealIntegration",
                "com.ezlifesteal.seasons.api.SeasonsApi",
                "com.skyblockexp.lifesteal.seasons.api.SeasonsIntegration"
        ));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(harness.pluginManager);
            when(harness.pluginManager.getPlugin("EzSeasons"))
                    .thenReturn(seasonsPluginWithLoader(loader));

            assertFalse(harness.services.ensureSeasonsClasses());
            assertNull(harness.registry.getSeasonsIntegrationState().getApiClass());
            assertNull(harness.registry.getSeasonsIntegrationState().getIntegrationClass());
            assertNull(harness.registry.getSeasonsIntegrationState().getProfileClass());
            verifyNoInteractions(harness.logger);
        }
    }

    /**
     * Builds an isolated classloader that simulates an old EzSeasons installation. The loader uses the
     * test classloader as parent (so Bukkit API is accessible) but blocks modern EzSeasons classes and
     * overrides specific class names with ByteBuddy-generated legacy equivalents that include a
     * {@code $Profile} class — matching the expected shape of the old EzSeasons API.
     */
    private ClassLoader buildLegacyEzSeasonsClassLoader() {
        Set<String> blockedModernClasses = Set.of(
                "com.skyblockexp.lifesteal.seasons.api.SeasonsApi",
                "com.skyblockexp.lifesteal.seasons.integration.LifestealIntegration"
        );
        LegacySimulatingClassLoader loader = new LegacySimulatingClassLoader(
                getClass().getClassLoader(), blockedModernClasses);

        byte[] legacyApiBytes = new ByteBuddy()
                .makeInterface()
                .name("com.ezlifesteal.seasons.api.SeasonsApi")
                .make()
                .getBytes();
        loader.define("com.ezlifesteal.seasons.api.SeasonsApi", legacyApiBytes);

        byte[] integrationBytes = new ByteBuddy()
                .subclass(Object.class)
                .name("com.skyblockexp.lifesteal.seasons.api.SeasonsIntegration")
                .make()
                .getBytes();
        loader.define("com.skyblockexp.lifesteal.seasons.api.SeasonsIntegration", integrationBytes);

        // Define $Profile as a separate class; resolveSeasonsProfileClass falls back to
        // Class.forName(integration.getName() + "$Profile") after getDeclaredClasses() returns empty.
        byte[] profileBytes = new ByteBuddy()
                .subclass(Object.class)
                .name("com.skyblockexp.lifesteal.seasons.api.SeasonsIntegration$Profile")
                .make()
                .getBytes();
        loader.define("com.skyblockexp.lifesteal.seasons.api.SeasonsIntegration$Profile", profileBytes);

        return loader;
    }

    private static RuntimeHarness runtimeHarness() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Logger logger = mock(Logger.class);
        Registry registry = new Registry();
        PluginManager pluginManager = mock(PluginManager.class);

        when(plugin.getLogger()).thenReturn(logger);

        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);
        return new RuntimeHarness(services, registry, logger, pluginManager);
    }

    private static Plugin seasonsPluginWithLoader(ClassLoader loader) {
        return (Plugin) Proxy.newProxyInstance(
                loader,
                new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
    }

    private record RuntimeHarness(
            DefaultPluginRuntimeServices services,
            Registry registry,
            Logger logger,
            PluginManager pluginManager
    ) {
    }

    private static final class FilteringClassLoader extends ClassLoader {
        private final Set<String> blockedClassNames;

        private FilteringClassLoader(ClassLoader parent, Set<String> blockedClassNames) {
            super(parent);
            this.blockedClassNames = blockedClassNames;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (blockedClassNames.contains(name)) {
                throw new ClassNotFoundException(name + " is intentionally hidden for this test");
            }
            return super.loadClass(name, resolve);
        }
    }

    /**
     * A classloader that simulates an old-style EzSeasons plugin classloader. Its parent is the test
     * classloader so Bukkit API classes remain accessible, but specific modern EzSeasons class names are
     * blocked and legacy class definitions can be injected via {@link #define(String, byte[])}.
     */
    private static final class LegacySimulatingClassLoader extends ClassLoader {

        private final Map<String, Class<?>> defined = new HashMap<>();

        private final Set<String> blocked;

        private LegacySimulatingClassLoader(ClassLoader parent, Set<String> blocked) {
            super(parent);
            this.blocked = blocked;
        }

        void define(String name, byte[] bytes) {
            defined.put(name, defineClass(name, bytes, 0, bytes.length, (ProtectionDomain) null));
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (defined.containsKey(name)) {
                return defined.get(name);
            }
            if (blocked.contains(name)) {
                throw new ClassNotFoundException("Blocked for legacy simulation: " + name);
            }
            return super.loadClass(name, resolve);
        }
    }
}
