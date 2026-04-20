package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.placeholder.PlaceholderHook;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceholderServiceTest {

    @Test
    void setupPlaceholderExpansionUnregistersExistingExpansion() {
        DefaultPluginRuntimeServices runtime = baseRuntime();
        PlaceholderHook existing = mock(PlaceholderHook.class);
        when(runtime.getPlaceholderExpansion()).thenReturn(existing);

        PlaceholderService service = new PlaceholderService(runtime, mock(EzLifestealPlugin.class));

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            PluginManager pluginManager = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(false);

            service.setupPlaceholderExpansion(mock(PluginAccessor.class));

            verify(existing).unregisterExpansion();
            verify(runtime).setPlaceholderExpansion(null);
        }
    }

    @Test
    void setupPlaceholderExpansionReturnsWhenPlaceholderApiDisabled() {
        DefaultPluginRuntimeServices runtime = baseRuntime();
        PlaceholderService service = new PlaceholderService(runtime, mock(EzLifestealPlugin.class));

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            PluginManager pluginManager = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(false);

            service.setupPlaceholderExpansion(mock(PluginAccessor.class));

            verify(runtime).getPlaceholderExpansion();
            verify(runtime, never()).setPlaceholderExpansion(any());
        }
    }

    @Test
    void setupPlaceholderExpansionStoresExpansionWhenRegistered() throws Exception {
        DefaultPluginRuntimeServices runtime = baseRuntime();
        Registry registry = mock(Registry.class);
        Registry.ManagerState managerState = mock(Registry.ManagerState.class);
        when(runtime.getRegistry()).thenReturn(registry);
        when(registry.getManagerState()).thenReturn(managerState);

        PlaceholderHook expansion = mock(PlaceholderHook.class);
        when(expansion.register()).thenReturn(true);

        PlaceholderService service = new PlaceholderService(runtime, mock(EzLifestealPlugin.class),
                accessor -> expansion);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            PluginManager pluginManager = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(true);

            service.setupPlaceholderExpansion(mock(PluginAccessor.class));

            verify(runtime).setPlaceholderExpansion(expansion);
            verify(managerState).setPlaceholderExpansion(expansion);
            verify(runtime.getLogger()).info("Registered PlaceholderAPI placeholders.");
        }
    }

    @Test
    void setupPlaceholderExpansionWarnsWhenRegisterReturnsFalse() throws Exception {
        DefaultPluginRuntimeServices runtime = baseRuntime();
        PlaceholderHook expansion = mock(PlaceholderHook.class);
        when(expansion.register()).thenReturn(false);

        PlaceholderService service = new PlaceholderService(runtime, mock(EzLifestealPlugin.class),
                accessor -> expansion);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            PluginManager pluginManager = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(true);

            service.setupPlaceholderExpansion(mock(PluginAccessor.class));

            verify(runtime.getLogger()).warning("Failed to register PlaceholderAPI placeholders.");
        }
    }

    @Test
    void setupPlaceholderExpansionWarnsWhenClassIsMissing() throws Exception {
        DefaultPluginRuntimeServices runtime = baseRuntime();
        PlaceholderService service = new PlaceholderService(runtime, mock(EzLifestealPlugin.class),
                accessor -> {
                    throw new ClassNotFoundException("missing");
                });

        runWithPlaceholderApiEnabled(() -> service.setupPlaceholderExpansion(mock(PluginAccessor.class)));

        verify(runtime.getLogger()).warning("PlaceholderAPI support is not available.");
    }

    @Test
    void setupPlaceholderExpansionWarnsWhenDependencyIsMissing() throws Exception {
        DefaultPluginRuntimeServices runtime = baseRuntime();
        PlaceholderService service = new PlaceholderService(runtime, mock(EzLifestealPlugin.class),
                accessor -> {
                    throw new NoClassDefFoundError("missing");
                });

        runWithPlaceholderApiEnabled(() -> service.setupPlaceholderExpansion(mock(PluginAccessor.class)));

        verify(runtime.getLogger()).warning("PlaceholderAPI was not found; placeholders will remain disabled.");
    }

    @Test
    void setupPlaceholderExpansionLogsReflectiveOperationFailure() throws Exception {
        DefaultPluginRuntimeServices runtime = baseRuntime();
        ReflectiveOperationException cause = new InvocationTargetException(new RuntimeException("boom"));
        PlaceholderService service = new PlaceholderService(runtime, mock(EzLifestealPlugin.class),
                accessor -> {
                    throw cause;
                });

        runWithPlaceholderApiEnabled(() -> service.setupPlaceholderExpansion(mock(PluginAccessor.class)));

        verify(runtime.getLogger()).log(Level.WARNING, "Failed to register PlaceholderAPI placeholders.", cause);
    }

    @Test
    void setupPlaceholderExpansionLogsRuntimeFailure() throws Exception {
        DefaultPluginRuntimeServices runtime = baseRuntime();
        RuntimeException cause = new RuntimeException("boom");
        PlaceholderService service = new PlaceholderService(runtime, mock(EzLifestealPlugin.class),
                accessor -> {
                    throw cause;
                });

        runWithPlaceholderApiEnabled(() -> service.setupPlaceholderExpansion(mock(PluginAccessor.class)));

        verify(runtime.getLogger()).log(Level.WARNING, "Failed to register PlaceholderAPI placeholders.", cause);
    }

    @Test
    void setupPlaceholderExpansionWarnsWhenExpansionTypeIsIncompatible() throws Exception {
        DefaultPluginRuntimeServices runtime = baseRuntime();
        PlaceholderService service = new PlaceholderService(runtime, mock(EzLifestealPlugin.class),
                accessor -> PlaceholderService.createPlaceholderHookFromClass(String.class, accessor));

        runWithPlaceholderApiEnabled(() -> service.setupPlaceholderExpansion(mock(PluginAccessor.class)));

        verify(runtime.getLogger())
                .warning("PlaceholderAPI support is not compatible with this version of EzLifesteal.");
    }

    @Test
    void createPlaceholderHookFromClassCreatesCompatibleHook() throws Exception {
        PlaceholderHook hook = PlaceholderService.createPlaceholderHookFromClass(TestPlaceholderHook.class,
                mock(PluginAccessor.class));

        assertTrue(hook instanceof TestPlaceholderHook);
    }

    @Test
    void createPlaceholderHookFromClassRejectsIncompatibleType() {
        assertThrows(ReflectiveOperationException.class,
                () -> PlaceholderService.createPlaceholderHookFromClass(String.class, mock(PluginAccessor.class)));
    }

    @Test
    void createPlaceholderHookFromClassThrowsWhenConstructorFails() {
        assertThrows(InvocationTargetException.class,
                () -> PlaceholderService.createPlaceholderHookFromClass(FailingPlaceholderHook.class,
                        mock(PluginAccessor.class)));
    }

    @Test
    void defaultFactoryMethodSurfacesClassNotFound() throws Exception {
        Method method = PlaceholderService.class.getDeclaredMethod("createPlaceholderHook", PluginAccessor.class);
        method.setAccessible(true);

        try {
            method.invoke(null, mock(PluginAccessor.class));
        }
        catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            assertTrue(cause instanceof ClassNotFoundException || cause instanceof NoClassDefFoundError);
        }
    }

    private static DefaultPluginRuntimeServices baseRuntime() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        when(runtime.getLogger()).thenReturn(mock(Logger.class));
        return runtime;
    }

    private static void runWithPlaceholderApiEnabled(Runnable action) {
        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            PluginManager pluginManager = mock(PluginManager.class);
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            when(pluginManager.isPluginEnabled("PlaceholderAPI")).thenReturn(true);
            action.run();
        }
    }

    public static final class TestPlaceholderHook implements PlaceholderHook {
        public TestPlaceholderHook(PluginAccessor pluginAccessor) {
        }

        @Override
        public boolean register() {
            return true;
        }

        @Override
        public boolean unregisterExpansion() {
            return true;
        }

        @Override
        public void clearCache() {
        }
    }

    public static final class FailingPlaceholderHook implements PlaceholderHook {
        public FailingPlaceholderHook(PluginAccessor pluginAccessor) {
            throw new IllegalStateException("boom");
        }

        @Override
        public boolean register() {
            return false;
        }

        @Override
        public boolean unregisterExpansion() {
            return false;
        }

        @Override
        public void clearCache() {
        }
    }
}
