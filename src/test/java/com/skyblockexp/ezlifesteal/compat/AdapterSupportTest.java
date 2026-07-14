package com.skyblockexp.ezlifesteal.compat;

import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdapterSupportTest {

    public interface HasScheduler {
        Object getScheduler();
    }

    public static final class PlayerScheduler {
        boolean invoked;

        public void run(Plugin plugin, Consumer<Object> action, Runnable retired) {
            invoked = true;
            action.accept(new Object());
        }
    }

    @Test
    void resolveRuntimeAdapterIdHandlesKnownAndUnknownVersions() {
        org.bukkit.Server server = mock(org.bukkit.Server.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);

            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.21.4-R0.1-SNAPSHOT");
            when(server.getName()).thenReturn("Paper");
            scheduler.when(SchedulerAdapter::detectFolia).thenReturn(false);
            assertEquals("paper.1.21.x", AdapterSupport.resolveRuntimeAdapterId());

            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.26.2-R0.1-SNAPSHOT");
            when(server.getName()).thenReturn("Folia");
            scheduler.when(SchedulerAdapter::detectFolia).thenReturn(true);
            assertEquals("folia.26.2.x", AdapterSupport.resolveRuntimeAdapterId());

            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.21.11-R0.1-SNAPSHOT");
            when(server.getName()).thenReturn("Spigot");
            scheduler.when(SchedulerAdapter::detectFolia).thenReturn(false);
            assertEquals("spigot.1.21.x", AdapterSupport.resolveRuntimeAdapterId());

            bukkit.when(Bukkit::getBukkitVersion).thenReturn("1.26.2-R0.1-SNAPSHOT");
            when(server.getName()).thenReturn("Spigot");
            scheduler.when(SchedulerAdapter::detectFolia).thenReturn(false);
            assertEquals("spigot.26.2.x", AdapterSupport.resolveRuntimeAdapterId());

            bukkit.when(Bukkit::getBukkitVersion).thenReturn("git-Paper-unknown");
            when(server.getName()).thenReturn("Paper");
            scheduler.when(SchedulerAdapter::detectFolia).thenReturn(false);
            assertEquals("paper.unknown", AdapterSupport.resolveRuntimeAdapterId());
        }
    }

    @Test
    void runOnMainAndRunAsyncHandleNullPluginAndScheduling() {
        AtomicInteger counter = new AtomicInteger();
        AdapterSupport.runOnMain(null, counter::incrementAndGet);
        AdapterSupport.runAsync(null, counter::incrementAndGet);
        assertEquals(2, counter.get());

        Plugin plugin = mock(Plugin.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            scheduler.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });
            scheduler.when(() -> SchedulerAdapter.runAsync(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });

            AdapterSupport.runOnMain(plugin, counter::incrementAndGet);
            AdapterSupport.runAsync(plugin, counter::incrementAndGet);
        }
        assertEquals(4, counter.get());
    }

    @Test
    void runForPlayerUsesSchedulerAndFoliaPlayerSchedulerPaths() {
        Plugin plugin = mock(Plugin.class);
        AtomicInteger counter = new AtomicInteger();

        Player nonFoliaPlayer = mock(Player.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            scheduler.when(SchedulerAdapter::detectFolia).thenReturn(false);
            scheduler.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });

            AdapterSupport.runForPlayer(plugin, nonFoliaPlayer, counter::incrementAndGet);
        }
        assertEquals(1, counter.get());

        PlayerScheduler foliaScheduler = new PlayerScheduler();
        Object proxy = Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Player.class, HasScheduler.class},
                (obj, method, args) -> {
                    if ("getScheduler".equals(method.getName())) {
                        return foliaScheduler;
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(obj);
                    }
                    if ("equals".equals(method.getName())) {
                        return obj == args[0];
                    }
                    return null;
                });
        Player foliaPlayer = (Player) proxy;

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            scheduler.when(SchedulerAdapter::detectFolia).thenReturn(true);
            scheduler.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });

            AdapterSupport.runForPlayer(plugin, foliaPlayer, counter::incrementAndGet);
        }

        assertEquals(2, counter.get());
    }

    @Test
    void runForPlayerFallsBackWhenFoliaReflectionFails() {
        Plugin plugin = mock(Plugin.class);
        Player player = mock(Player.class);
        AtomicInteger counter = new AtomicInteger();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            scheduler.when(SchedulerAdapter::detectFolia).thenReturn(true);
            scheduler.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });

            AdapterSupport.runForPlayer(plugin, player, counter::incrementAndGet);
        }

        assertEquals(1, counter.get());
    }

    @Test
    void runAtLocationUsesSchedulerAndInlinePaths() {
        Plugin plugin = mock(Plugin.class);
        World world = mock(World.class);
        Location location = new Location(world, 1.0, 2.0, 3.0);
        AtomicInteger counter = new AtomicInteger();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            scheduler.when(SchedulerAdapter::detectFolia).thenReturn(false);
            scheduler.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return null;
            });

            AdapterSupport.runAtLocation(plugin, location, counter::incrementAndGet);
        }

        assertEquals(1, counter.get());

        AdapterSupport.runAtLocation(null, location, counter::incrementAndGet);
        assertEquals(2, counter.get());
    }

    @Test
    void dropItemHelpersHandleNullAndValidInputs() {
        Plugin plugin = mock(Plugin.class);
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 0.0, 64.0, 0.0);
        ItemStack item = mock(ItemStack.class);

        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);

        AdapterSupport.dropItemLeftoversAtPlayer(plugin, player, null);
        AdapterSupport.dropItemLeftoversAtPlayer(plugin, player, Map.of());
        AdapterSupport.dropItemAtPlayer(plugin, player, null);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);

            Map<Integer, ItemStack> leftovers = new java.util.HashMap<>();
            leftovers.put(0, item);
            leftovers.put(1, null);

            AdapterSupport.dropItemLeftoversAtPlayer(plugin, player, leftovers);
            AdapterSupport.dropItemAtPlayer(plugin, player, item);
        }

        verify(world, org.mockito.Mockito.times(2)).dropItemNaturally(location, item);
    }

    @Test
    void runForPlayerSkipsNullInputsAndUnusablePlayer() {
        Plugin plugin = mock(Plugin.class);
        AtomicInteger counter = new AtomicInteger();

        AdapterSupport.runForPlayer(plugin, null, counter::incrementAndGet);
        AdapterSupport.runForPlayer(plugin, mock(Player.class), null);

        verify(plugin, never()).getName();
        assertEquals(0, counter.get());
    }
}